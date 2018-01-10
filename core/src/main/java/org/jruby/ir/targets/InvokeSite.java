package org.jruby.ir.targets;

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.Signature;
import com.headius.invokebinder.SmartBinder;
import com.headius.invokebinder.SmartHandle;
import org.jruby.Ruby;
import org.jruby.RubyBasicObject;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyModule;
import org.jruby.RubyNil;
import org.jruby.RubySymbol;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.ir.JIT;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.invokedynamic.JRubyCallSite;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.SwitchPoint;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.invoke.MethodHandles.lookup;

/**
* Created by headius on 10/23/14.
*/
public abstract class InvokeSite extends MutableCallSite {

    private static final Logger LOG = LoggerFactory.getLogger(InvokeSite.class);
    static { // enable DEBUG output
        if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) LOG.setDebugEnable(true);
    }
    private static final boolean LOG_BINDING = LOG.isDebugEnabled();

    private static final AtomicLong SITE_ID = new AtomicLong(1);

    final Signature signature;
    final Signature fullSignature;
    final int arity;
    protected final String methodName;
    final MethodHandle fallback;
    private final SiteTracker tracker = new SiteTracker();
    private final long siteID = SITE_ID.getAndIncrement();
    private final int argOffset;
    protected final String file;
    protected final int line;
    private boolean boundOnce;
    private boolean literalClosure;
    CacheEntry cache = CacheEntry.NULL_CACHE;

    public String name() {
        return methodName;
    }

    public final CallType callType;

    public InvokeSite(MethodType type, String name, CallType callType, String file, int line) {
        this(type, name, callType, false, file, line);
    }

    public InvokeSite(MethodType type, String name, CallType callType, boolean literalClosure, String file, int line) {
        super(type);
        this.methodName = name;
        this.callType = callType;
        this.literalClosure = literalClosure;
        this.file = file;
        this.line = line;

        Signature startSig;

        if (callType == CallType.SUPER) {
            // super calls receive current class argument, so offsets and signature are different
            startSig = JRubyCallSite.STANDARD_SUPER_SIG;
            argOffset = 4;
        } else {
            startSig = JRubyCallSite.STANDARD_SITE_SIG;
            argOffset = 3;
        }

        int arity;
        if (type.parameterType(type.parameterCount() - 1) == Block.class) {
            arity = type.parameterCount() - (argOffset + 1);

            if (arity == 1 && type.parameterType(argOffset) == IRubyObject[].class) {
                arity = -1;
                startSig = startSig.appendArg("args", IRubyObject[].class);
            } else {
                for (int i = 0; i < arity; i++) {
                    startSig = startSig.appendArg("arg" + i, IRubyObject.class);
                }
            }
            startSig = startSig.appendArg("block", Block.class);
            fullSignature = signature = startSig;
        } else {
            arity = type.parameterCount() - argOffset;

            if (arity == 1 && type.parameterType(argOffset) == IRubyObject[].class) {
                arity = -1;
                startSig = startSig.appendArg("args", IRubyObject[].class);
            } else {
                for (int i = 0; i < arity; i++) {
                    startSig = startSig.appendArg("arg" + i, IRubyObject.class);
                }
            }
            signature = startSig;
            fullSignature = startSig.appendArg("block", Block.class);
        }

        this.arity = arity;

        this.fallback = prepareBinder(true).invokeVirtualQuiet(Bootstrap.LOOKUP, "invoke");
    }

    public static CallSite bootstrap(InvokeSite site, MethodHandles.Lookup lookup) {
        site.setInitialTarget(site.fallback);

        return site;
    }

    public IRubyObject invoke(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        SwitchPoint switchPoint = (SwitchPoint) selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, caller)) {
            // Test thresholds so we don't do this forever (#4596)
            if (testThresholds(selfClass) == CacheAction.FAIL) {
                logFail();
                bindToFail();
            } else {
                logMethodMissing();
            }
            return callMethodMissing(entry, callType, context, self, methodName, args, block);
        }

        MethodHandle mh = getHandle(self, selfClass, method);

        if (literalClosure) {
            mh = Binder.from(mh.type())
                    .tryFinally(getBlockEscape(signature))
                    .invoke(mh);
        }

        updateInvocationTarget(mh, self, selfClass, entry.method, switchPoint);

        if (literalClosure) {
            try {
                return method.call(context, self, selfClass, methodName, args, block);
            } finally {
                block.escape();
            }
        }

        return method.call(context, self, selfClass, methodName, args, block);
    }

    private static final MethodHandle ESCAPE_BLOCK = Binder.from(void.class, Block.class).invokeVirtualQuiet(lookup(), "escape");
    private static final Map<Signature, MethodHandle> BLOCK_ESCAPES = Collections.synchronizedMap(new HashMap<Signature, MethodHandle>());

    private static MethodHandle getBlockEscape(Signature signature) {
        Signature voidSignature = signature.changeReturn(void.class);
        MethodHandle escape = BLOCK_ESCAPES.get(voidSignature);
        if (escape == null) {
            escape = SmartBinder.from(voidSignature)
                    .permute("block")
                    .invoke(ESCAPE_BLOCK)
                    .handle();
            BLOCK_ESCAPES.put(voidSignature, escape);
        }
        return escape;
    }

    /**
     * Failover version uses a monomorphic cache and DynamicMethod.call, as in non-indy.
     */
    public IRubyObject fail(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String name = methodName;
        CacheEntry entry = cache;

        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name, args, block);
        }

        entry = selfClass.searchWithCache(name);

        if (methodMissing(entry, caller)) {
            return callMethodMissing(entry, callType, context, self, name, args, block);
        }

        cache = entry;

        return entry.method.call(context, self, selfClass, name, args, block);
    }

    /**
     * Failover version uses a monomorphic cache and DynamicMethod.call, as in non-indy.
     */
    public IRubyObject fail(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) throws Throwable {
        return fail(context, caller, self, IRubyObject.NULL_ARRAY, block);
    }

    /**
     * Failover version uses a monomorphic cache and DynamicMethod.call, as in non-indy.
     */
    public IRubyObject fail(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String name = methodName;
        CacheEntry entry = cache;

        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name, arg0, block);
        }

        entry = selfClass.searchWithCache(name);

        if (methodMissing(entry, caller)) {
            return callMethodMissing(entry, callType, context, self, name, arg0, block);
        }

        cache = entry;

        return entry.method.call(context, self, selfClass, name, arg0, block);
    }

    /**
     * Failover version uses a monomorphic cache and DynamicMethod.call, as in non-indy.
     */
    public IRubyObject fail(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String name = methodName;
        CacheEntry entry = cache;

        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name, arg0, arg1, block);
        }

        entry = selfClass.searchWithCache(name);

        if (methodMissing(entry, caller)) {
            return callMethodMissing(entry, callType, context, self, name, arg0, arg1, block);
        }

        cache = entry;

        return entry.method.call(context, self, selfClass, name, arg0, arg1, block);
    }

    /**
     * Failover version uses a monomorphic cache and DynamicMethod.call, as in non-indy.
     */
    public IRubyObject fail(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String name = methodName;
        CacheEntry entry = cache;

        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name, arg0, arg1, arg2, block);
        }

        entry = selfClass.searchWithCache(name);

        if (methodMissing(entry, caller)) {
            return callMethodMissing(entry, callType, context, self, name, arg0, arg1, arg2, block);
        }

        cache = entry;

        return entry.method.call(context, self, selfClass, name, arg0, arg1, arg2, block);
    }

    /**
     * Prepare a binder for this call site's target, forcing varargs if specified
     *
     * @param varargs whether to only call an arg-boxed variable arity path
     * @return the prepared binder
     */
    public Binder prepareBinder(boolean varargs) {
        SmartBinder binder = SmartBinder.from(signature);

        if (varargs || arity > 3) {
            // we know we want to call varargs path always, so prepare args[] here
            if (arity == -1) {
                // do nothing, already have IRubyObject[] in args
            } else if (arity == 0) {
                binder = binder.insert(argOffset, "args", IRubyObject.NULL_ARRAY);
            } else {
                binder = binder
                        .collect("args", "arg[0-9]+");
            }
        }

        // add block if needed
        if (signature.lastArgType() != Block.class) {
            binder = binder.append("block", Block.NULL_BLOCK);
        }

        // bind to site
        binder = binder.insert(0, "site", this);

        return binder.binder();
    }

    MethodHandle getHandle(IRubyObject self, RubyClass dispatchClass, DynamicMethod method) throws Throwable {
        boolean blockGiven = signature.lastArgType() == Block.class;

        MethodHandle mh = buildNewInstanceHandle(method, self);
        if (mh == null) mh = Bootstrap.buildNativeHandle(this, method, blockGiven);
        if (mh == null) mh = Bootstrap.buildIndyHandle(this, method, method.getImplementationClass());
        if (mh == null) mh = Bootstrap.buildJittedHandle(this, method, blockGiven);
        if (mh == null) mh = Bootstrap.buildAttrHandle(this, method, self, dispatchClass);
        if (mh == null) mh = Bootstrap.buildGenericHandle(this, method, dispatchClass);

        assert mh != null : "we should have a method handle of some sort by now";

        return mh;
    }

    MethodHandle buildNewInstanceHandle(DynamicMethod method, IRubyObject self) {
        MethodHandle mh = null;

        if (method == self.getRuntime().getBaseNewMethod()) {
            RubyClass recvClass = (RubyClass) self;

            // Bind a second site as a dynamic invoker to guard against changes in new object's type
            CallSite initSite = SelfInvokeSite.bootstrap(lookup(), "callFunctional:initialize", type(), literalClosure ? 1 : 0, file, line);
            MethodHandle initHandle = initSite.dynamicInvoker();

            MethodHandle allocFilter = Binder.from(IRubyObject.class, IRubyObject.class)
                    .cast(IRubyObject.class, RubyClass.class)
                    .insert(0, new Class[] {ObjectAllocator.class, Ruby.class}, recvClass.getAllocator(), self.getRuntime())
                    .invokeVirtualQuiet(lookup(), "allocate");

            mh = SmartBinder.from(lookup(), signature)
                    .filter("self", allocFilter)
                    .fold("dummy", initHandle)
                    .permute("self")
                    .identity()
                    .handle();
        }

        return mh;
    }

    /**
     * Update the given call site using the new target, wrapping with appropriate
     * bind and argument-juggling logic. Return a handle suitable for invoking
     * with the site's original method type.
     */
    MethodHandle updateInvocationTarget(MethodHandle target, IRubyObject self, RubyModule testClass, DynamicMethod method, SwitchPoint switchPoint) {
        MethodHandle fallback;
        MethodHandle gwt;

        CacheAction cacheAction = testThresholds(testClass);
        switch (cacheAction) {
            case FAIL:
                logFail();
                // bind to specific-arity fail method if available
                return bindToFail();
            case PIC:
                // stack it up into a PIC
                logPic(method);
                fallback = getTarget();
                break;
            case REBIND:
            case BIND:
                // wipe out site with this new type and method
                logBind(cacheAction);
                fallback = this.fallback;
                break;
            default:
                throw new RuntimeException("invalid cache action: " + cacheAction);
        }

        // Continue with logic for PIC, BIND, and REBIND
        tracker.addType(testClass.id);

        SmartHandle test;

        if (self instanceof RubySymbol ||
                self instanceof RubyFixnum ||
                self instanceof RubyFloat ||
                self instanceof RubyNil ||
                self instanceof RubyBoolean.True ||
                self instanceof RubyBoolean.False) {

            test = SmartBinder
                    .from(signature.asFold(boolean.class))
                    .permute("self")
                    .insert(1, "selfJavaType", self.getClass())
                    .cast(boolean.class, Object.class, Class.class)
                    .invoke(TEST_CLASS);

        } else {

            test = SmartBinder
                    .from(signature.changeReturn(boolean.class))
                    .permute("self")
                    .insert(0, "selfClass", RubyClass.class, testClass)
                    .invokeStaticQuiet(Bootstrap.LOOKUP, Bootstrap.class, "testType");
        }

        gwt = MethodHandles.guardWithTest(test.handle(), target, fallback);

        // wrap in switchpoint for mutation invalidation
        gwt = switchPoint.guardWithTest(gwt, fallback);

        setTarget(gwt);

        return target;
    }

    private void logMethodMissing() {
        if (LOG_BINDING) {
            LOG.debug(methodName + "\ttriggered site #" + siteID + " method_missing (" + file + ":" + line + ")");
        }
    }

    private void logBind(CacheAction action) {
        if (LOG_BINDING) {
            LOG.debug(methodName + "\ttriggered site #" + siteID + " " + action + " (" + file + ":" + line + ")");
        }
    }

    private void logPic(DynamicMethod method) {
        if (LOG_BINDING) {
            LOG.debug(methodName + "\tadded to PIC " + logMethod(method));
        }
    }

    private void logFail() {
        if (LOG_BINDING) {
            if (tracker.clearCount() > Options.INVOKEDYNAMIC_MAXFAIL.load()) {
                LOG.info(methodName + "\tat site #" + siteID + " failed more than " + Options.INVOKEDYNAMIC_MAXFAIL.load() + " times; bailing out (" + file + ":" + line + ")");
            } else if (tracker.seenTypesCount() + 1 > Options.INVOKEDYNAMIC_MAXPOLY.load()) {
                LOG.info(methodName + "\tat site #" + siteID + " encountered more than " + Options.INVOKEDYNAMIC_MAXPOLY.load() + " types; bailing out (" + file + ":" + line + ")");
            }
        }
    }

    private MethodHandle bindToFail() {
        MethodHandle target;
        setTarget(target = prepareBinder(false).invokeVirtualQuiet(lookup(), "fail"));
        return target;
    }

    enum CacheAction { FAIL, BIND, REBIND, PIC }

    CacheAction testThresholds(RubyModule testClass) {
        if (tracker.clearCount() > Options.INVOKEDYNAMIC_MAXFAIL.load() ||
                (!tracker.hasSeenType(testClass.id)
                        && tracker.seenTypesCount() + 1 > Options.INVOKEDYNAMIC_MAXPOLY.load())) {
            // Thresholds exceeded
            return CacheAction.FAIL;
        } else {
            // if we've cached no types, and the site is bound and we haven't seen this new type...
            if (tracker.seenTypesCount() > 0 && getTarget() != null && !tracker.hasSeenType(testClass.id)) {
                // stack it up into a PIC
                tracker.addType(testClass.id);
                return CacheAction.PIC;
            } else {
                // wipe out site with this new type and method
                tracker.clearTypes();
                tracker.addType(testClass.id);
                return boundOnce ? CacheAction.REBIND : CacheAction.BIND;
            }
        }
    }

    public RubyClass pollAndGetClass(ThreadContext context, IRubyObject self) {
        context.callThreadPoll();
        return ((RubyBasicObject) self).getMetaClass();
    }

    @Override
    public void setTarget(MethodHandle target) {
        super.setTarget(target);
        boundOnce = true;
    }

    public void setInitialTarget(MethodHandle target) {
        super.setTarget(target);
    }

    public abstract boolean methodMissing(CacheEntry entry, IRubyObject caller);

    /**
     * Variable arity method_missing invocation. Arity zero also passes through here.
     */
    public IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject[] args, Block block) {
        return Helpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, args, block);
    }

    /**
     * Arity one method_missing invocation
     */
    public IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject arg0, Block block) {
        return Helpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, arg0, block);
    }


    /**
     * Arity two method_missing invocation
     */
    public IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        return Helpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, arg0, arg1, block);
    }


    /**
     * Arity three method_missing invocation
     */
    public IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return Helpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, arg0, arg1, arg2, block);
    }

    private static String logMethod(DynamicMethod method) {
        return "[#" + method.getSerialNumber() + ' ' + method.getImplementationClass() + ']';
    }

    @JIT
    public static boolean testMetaclass(RubyClass metaclass, IRubyObject self) {
        return metaclass == ((RubyBasicObject) self).getMetaClass();
    }

    @JIT
    public static boolean testClass(Object object, Class clazz) {
        return object.getClass() == clazz;
    }

    public String toString() {
        return getClass().getName() + "[name=" + name() + ",arity=" + arity + ",type=" + type() + ",file=" + file + ",line=" + line + ']';
    }

    private static final MethodHandle TEST_CLASS = Binder
            .from(boolean.class, Object.class, Class.class)
            .invokeStaticQuiet(lookup(), InvokeSite.class, "testClass");
}
