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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.invoke.MethodHandles.lookup;

/**
* Created by headius on 10/23/14.
*/
public abstract class InvokeSite extends MutableCallSite {
    final Signature signature;
    final Signature fullSignature;
    final int arity;
    protected final String methodName;
    final MethodHandle fallback;
    private final Set<Integer> seenTypes = new HashSet<Integer>();
    private int clearCount;
    private static final AtomicLong SITE_ID = new AtomicLong(1);
    private final long siteID = SITE_ID.getAndIncrement();
    private final int argOffset;
    protected final String file;
    protected final int line;
    private boolean boundOnce;
    CacheEntry cache = CacheEntry.NULL_CACHE;

    private static final Logger LOG = LoggerFactory.getLogger(InvokeSite.class);

    public String name() {
        return methodName;
    }

    public final CallType callType;

    public InvokeSite(MethodType type, String name, CallType callType, String file, int line) {
        super(type);
        this.methodName = name;
        this.callType = callType;
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

        this.fallback = prepareBinder().invokeVirtualQuiet(Bootstrap.LOOKUP, "invoke");
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
            return callMethodMissing(entry, callType, context, self, methodName, args, block);
        }

        MethodHandle mh = getHandle(self, selfClass, method);

        updateInvocationTarget(mh, self, selfClass, entry.method, switchPoint);

        return method.call(context, self, selfClass, methodName, args, block);
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

    public Binder prepareBinder() {
        SmartBinder binder = SmartBinder.from(signature);

        // prepare arg[]
        if (arity == -1) {
            // do nothing, already have IRubyObject[] in args
        } else if (arity == 0) {
            binder = binder.insert(argOffset, "args", IRubyObject.NULL_ARRAY);
        } else {
            binder = binder
                    .collect("args", "arg[0-9]+");
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

        MethodHandle mh = buildNewInstanceHandle(method, self, blockGiven);
        if (mh == null) mh = Bootstrap.buildNativeHandle(this, method, blockGiven);
        if (mh == null) mh = Bootstrap.buildIndyHandle(this, method, method.getImplementationClass());
        if (mh == null) mh = Bootstrap.buildJittedHandle(this, method, blockGiven);
        if (mh == null) mh = Bootstrap.buildAttrHandle(this, method, self, dispatchClass);
        if (mh == null) mh = Bootstrap.buildGenericHandle(this, method, dispatchClass);

        assert mh != null : "we should have a method handle of some sort by now";

        return mh;
    }

    MethodHandle buildNewInstanceHandle(DynamicMethod method, IRubyObject self, boolean blockGiven) {
        MethodHandle mh = null;

        if (method == self.getRuntime().getBaseNewMethod()) {
            RubyClass recvClass = (RubyClass) self;

            // Bind a second site as a dynamic invoker to guard against changes in new object's type
            CallSite initSite = SelfInvokeSite.bootstrap(lookup(), "callFunctional:initialize", type(), file, line);
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
     * guard and argument-juggling logic. Return a handle suitable for invoking
     * with the site's original method type.
     */
    MethodHandle updateInvocationTarget(MethodHandle target, IRubyObject self, RubyModule testClass, DynamicMethod method, SwitchPoint switchPoint) {
        if (target == null ||
                clearCount > Options.INVOKEDYNAMIC_MAXFAIL.load() ||
                (!hasSeenType(testClass.id)
                        && seenTypesCount() + 1 > Options.INVOKEDYNAMIC_MAXPOLY.load())) {
            setTarget(target = prepareBinder().invokeVirtualQuiet(lookup(), "fail"));
        } else {
            MethodHandle fallback;
            MethodHandle gwt;

            // if we've cached no types, and the site is bound and we haven't seen this new type...
            if (seenTypesCount() > 0 && getTarget() != null && !hasSeenType(testClass.id)) {
                // stack it up into a PIC
                if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) LOG.info(methodName + "\tadded to PIC " + logMethod(method));
                fallback = getTarget();
            } else {
                // wipe out site with this new type and method
                String bind = boundOnce ? "rebind" : "bind";
                if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) LOG.info(methodName + "\ttriggered site #" + siteID + " " + bind + " (" + file + ":" + line + ")");
                fallback = this.fallback;
                clearTypes();
            }

            addType(testClass.id);

            SmartHandle test;
            SmartBinder selfTest = SmartBinder
                    .from(signature.asFold(boolean.class))
                    .permute("self");

            if (self instanceof RubySymbol ||
                    self instanceof RubyFixnum ||
                    self instanceof RubyFloat ||
                    self instanceof RubyNil ||
                    self instanceof RubyBoolean.True ||
                    self instanceof RubyBoolean.False) {

                test = selfTest
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
        }

        return target;
    }

    public RubyClass pollAndGetClass(ThreadContext context, IRubyObject self) {
        context.callThreadPoll();
        RubyClass selfType = ((RubyBasicObject)self).getMetaClass();
        return selfType;
    }

    @Override
    public void setTarget(MethodHandle target) {
        super.setTarget(target);
        boundOnce = true;
    }

    public void setInitialTarget(MethodHandle target) {
        super.setTarget(target);
    }

    public synchronized boolean hasSeenType(int typeCode) {
        return seenTypes.contains(typeCode);
    }

    public synchronized void addType(int typeCode) {
        seenTypes.add(typeCode);
    }

    public synchronized int seenTypesCount() {
        return seenTypes.size();
    }

    public synchronized void clearTypes() {
        seenTypes.clear();
        clearCount++;
    }

    public abstract boolean methodMissing(CacheEntry entry, IRubyObject caller);

    public IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject[] args, Block block) {
        return Helpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, args, block);
    }

    private static String logMethod(DynamicMethod method) {
        return "[#" + method.getSerialNumber() + " " + method.getImplementationClass() + "]";
    }

    @JIT
    public static boolean testMetaclass(RubyClass metaclass, IRubyObject self) {
        return metaclass == ((RubyBasicObject)self).getMetaClass();
    }

    @JIT
    public static boolean testClass(Object object, Class clazz) {
        return object.getClass() == clazz;
    }

    private static final MethodHandle TEST_CLASS = Binder
            .from(boolean.class, Object.class, Class.class)
            .invokeStaticQuiet(lookup(), InvokeSite.class, "testClass");
}
