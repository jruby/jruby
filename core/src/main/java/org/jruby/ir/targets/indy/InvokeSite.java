package org.jruby.ir.targets.indy;

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
import org.jruby.RubyStruct;
import org.jruby.RubySymbol;
import org.jruby.internal.runtime.methods.AliasMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.PartialDelegatingMethod;
import org.jruby.ir.JIT;
import org.jruby.ir.targets.SiteTracker;
import org.jruby.java.invokers.InstanceFieldGetter;
import org.jruby.java.invokers.InstanceFieldSetter;
import org.jruby.javasupport.JavaUtil;
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
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.lang.invoke.MethodHandles.constant;
import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodHandles.lookup;
import static org.jruby.runtime.invokedynamic.JRubyCallSite.SITE_ID;

/**
* Created by headius on 10/23/14.
*/
public abstract class InvokeSite extends MutableCallSite {

    private static final Logger LOG = LoggerFactory.getLogger(InvokeSite.class);
    static { // enable DEBUG output
        if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) LOG.setDebugEnable(true);
    }
    private static final boolean LOG_BINDING = LOG.isDebugEnabled();
    static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public final Signature signature;
    public final Signature fullSignature;
    public final int arity;
    protected final String methodName;
    final MethodHandle fallback;
    private final SiteTracker tracker = new SiteTracker();
    private final long siteID = SITE_ID.getAndIncrement();
    private final int argOffset;
    protected final String file;
    protected final int line;
    private boolean boundOnce;
    private boolean literalClosure;
    protected CacheEntry cache = CacheEntry.NULL_CACHE;

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
        MethodHandle mh = null;
        boolean methodMissing = false;

        if (methodMissing(entry, caller)) {
            methodMissing = true;
            // Test thresholds so we don't do this forever (#4596)
            if (testThresholds(selfClass) == CacheAction.FAIL) {
                logFail();
                bindToFail();
            } else {
                logMethodMissing();
            }
            method = Helpers.selectMethodMissing(context, selfClass, entry.method.getVisibility(), methodName, callType);
            if (method instanceof Helpers.MethodMissingMethod) {
                entry = ((Helpers.MethodMissingMethod) method).entry;
                method = entry.method;
            } else {
                entry = new CacheEntry(
                        method,
                        selfClass,
                        entry.token);
            }
            try {
                mh = Bootstrap.buildMethodMissingHandle(this, entry, self);
            } catch (Throwable t) {
                t.printStackTrace();
                Helpers.throwException(t);
            }
        } else {
            mh = getHandle(self, entry);
        }

        if (literalClosure) {
            mh = Binder.from(mh.type())
                    .tryFinally(getBlockEscape(signature))
                    .invoke(mh);
        }

        updateInvocationTarget(mh, self, selfClass, entry.method, switchPoint);

        if (literalClosure) {
            try {
                if (methodMissing) {
                    return method.call(context, self, entry.sourceModule, methodName, Helpers.arrayOf(context.runtime.newSymbol(methodName), args), block);
                } else {
                    return method.call(context, self, entry.sourceModule, methodName, args, block);
                }
            } finally {
                block.escape();
            }
        }

        if (methodMissing) {
            return method.call(context, self, entry.sourceModule, methodName, Helpers.arrayOf(context.runtime.newSymbol(methodName), args), block);
        } else {
            return method.call(context, self, entry.sourceModule, methodName, args, block);
        }
    }

    private static final MethodHandle ESCAPE_BLOCK = Binder.from(void.class, Block.class).invokeVirtualQuiet(LOOKUP, "escape");
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
            return entry.method.call(context, self, entry.sourceModule, name, args, block);
        }

        entry = selfClass.searchWithCache(name);

        if (methodMissing(entry, caller)) {
            return callMethodMissing(entry, callType, context, self, selfClass, name, args, block);
        }

        cache = entry;

        return entry.method.call(context, self, entry.sourceModule, name, args, block);
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
            return entry.method.call(context, self, entry.sourceModule, name, arg0, block);
        }

        entry = selfClass.searchWithCache(name);

        if (methodMissing(entry, caller)) {
            return callMethodMissing(entry, callType, context, self, selfClass, name, arg0, block);
        }

        cache = entry;

        return entry.method.call(context, self, entry.sourceModule, name, arg0, block);
    }

    /**
     * Failover version uses a monomorphic cache and DynamicMethod.call, as in non-indy.
     */
    public IRubyObject fail(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String name = methodName;
        CacheEntry entry = cache;

        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, entry.sourceModule, name, arg0, arg1, block);
        }

        entry = selfClass.searchWithCache(name);

        if (methodMissing(entry, caller)) {
            return callMethodMissing(entry, callType, context, self, selfClass, name, arg0, arg1, block);
        }

        cache = entry;

        return entry.method.call(context, self, entry.sourceModule, name, arg0, arg1, block);
    }

    /**
     * Failover version uses a monomorphic cache and DynamicMethod.call, as in non-indy.
     */
    public IRubyObject fail(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String name = methodName;
        CacheEntry entry = cache;

        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, entry.sourceModule, name, arg0, arg1, arg2, block);
        }

        entry = selfClass.searchWithCache(name);

        if (methodMissing(entry, caller)) {
            return callMethodMissing(entry, callType, context, self, selfClass, name, arg0, arg1, arg2, block);
        }

        cache = entry;

        return entry.method.call(context, self, entry.sourceModule, name, arg0, arg1, arg2, block);
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
                        .collect("args", "arg[0-9]+", Helpers.constructObjectArrayHandle(arity));
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

    protected MethodHandle getHandle(IRubyObject self, CacheEntry entry) throws Throwable {
        boolean blockGiven = signature.lastArgType() == Block.class;

        MethodHandle mh = buildNewInstanceHandle(entry, self);
        if (mh == null) mh = buildNotEqualHandle(entry, self);
        if (mh == null) mh = Bootstrap.buildNativeHandle(this, entry, blockGiven);
        if (mh == null) mh = buildJavaFieldHandle(this, entry, self);
        if (mh == null) mh = Bootstrap.buildIndyHandle(this, entry);
        if (mh == null) mh = Bootstrap.buildJittedHandle(this, entry, blockGiven);
        if (mh == null) mh = Bootstrap.buildAttrHandle(this, entry, self);
        if (mh == null) mh = buildAliasHandle(entry, self);
        if (mh == null) mh = buildStructHandle(entry);
        if (mh == null) mh = Bootstrap.buildGenericHandle(this, entry);

        assert mh != null : "we should have a method handle of some sort by now";

        return mh;
    }

    MethodHandle buildJavaFieldHandle(InvokeSite site, CacheEntry entry, IRubyObject self) throws Throwable {
        DynamicMethod method = entry.method;

        if (method instanceof InstanceFieldGetter) {
            // only matching arity
            if (site.arity != 0 || site.signature.lastArgType() == Block.class) return null;

            Field field = ((InstanceFieldGetter) method).getField();

            // only IRubyObject subs for now
            if (!IRubyObject.class.isAssignableFrom(field.getType())) return null;

            MethodHandle fieldHandle = (MethodHandle) method.getHandle();

            if (fieldHandle != null) {
                return fieldHandle;
            }

            fieldHandle = LOOKUP.unreflectGetter(field);

            MethodHandle filter = self.getRuntime().getNullToNilHandle();

            MethodHandle receiverConverter = Binder
                    .from(field.getDeclaringClass(), IRubyObject.class)
                    .cast(Object.class, IRubyObject.class)
                    .invokeStaticQuiet(lookup(), JavaUtil.class, "objectFromJavaProxy");

            fieldHandle = Binder
                    .from(site.type())
                    .permute(2)
                    .filter(0, receiverConverter)
                    .filterReturn(filter)
                    .cast(fieldHandle.type())
                    .invoke(fieldHandle);

            method.setHandle(fieldHandle);

            return fieldHandle;
        } else if (method instanceof InstanceFieldSetter) {
            // only matching arity
            if (site.arity != 1 || site.signature.lastArgType() == Block.class) return null;

            Field field = ((InstanceFieldSetter) method).getField();

            // only IRubyObject subs for now
            if (!IRubyObject.class.isAssignableFrom(field.getType())) return null;

            MethodHandle fieldHandle = (MethodHandle) method.getHandle();

            if (fieldHandle != null) {
                return fieldHandle;
            }

            fieldHandle = LOOKUP.unreflectSetter(field);

            MethodHandle receiverConverter = Binder
                    .from(field.getDeclaringClass(), IRubyObject.class)
                    .cast(Object.class, IRubyObject.class)
                    .invokeStaticQuiet(lookup(), JavaUtil.class, "objectFromJavaProxy");

            fieldHandle = Binder
                    .from(site.type())
                    .permute(2, 3)
                    .filter(0, receiverConverter)
                    .filterReturn(constant(IRubyObject.class, self.getRuntime().getNil()))
                    .cast(fieldHandle.type())
                    .invoke(fieldHandle);

            method.setHandle(fieldHandle);

            return fieldHandle;
        }

        return null;
    }

    MethodHandle buildNewInstanceHandle(CacheEntry entry, IRubyObject self) {
        MethodHandle mh = null;
        DynamicMethod method = entry.method;

        if (method == self.getRuntime().getBaseNewMethod()) {
            RubyClass recvClass = (RubyClass) self;

            // Bind a second site as a dynamic invoker to guard against changes in new object's type
            CallSite initSite = SelfInvokeSite.bootstrap(LOOKUP, "callFunctional:initialize", type(), literalClosure ? 1 : 0, file, line);
            MethodHandle initHandle = initSite.dynamicInvoker();

            MethodHandle allocFilter = Binder.from(IRubyObject.class, IRubyObject.class)
                    .cast(IRubyObject.class, RubyClass.class)
                    .insert(0, new Class[] {ObjectAllocator.class, Ruby.class}, recvClass.getAllocator(), self.getRuntime())
                    .invokeVirtualQuiet(LOOKUP, "allocate");

            mh = SmartBinder.from(LOOKUP, signature)
                    .filter("self", allocFilter)
                    .fold("dummy", initHandle)
                    .permute("self")
                    .identity()
                    .handle();

            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                LOG.info(name() + "\tbound as new instance creation " + Bootstrap.logMethod(method));
            }
        }

        return mh;
    }

    MethodHandle buildNotEqualHandle(CacheEntry entry, IRubyObject self) {
        MethodHandle mh = null;
        DynamicMethod method = entry.method;

        Ruby runtime = self.getRuntime();

        if (method.isBuiltin()) {

            CallSite equalSite = null;

            // FIXME: poor test for built-in != and !~
            if (method.getImplementationClass() == runtime.getBasicObject() && name().equals("!=")) {
                equalSite = SelfInvokeSite.bootstrap(LOOKUP, "callFunctional:==", type(), literalClosure ? 1 : 0, file, line);
            } else if (method.getImplementationClass() == runtime.getKernel() && name().equals("!~")) {
                equalSite = SelfInvokeSite.bootstrap(LOOKUP, "callFunctional:=~", type(), literalClosure ? 1 : 0, file, line);
            }

            if (equalSite != null) {
                // Bind a second site as a dynamic invoker to guard against changes in new object's type
                MethodHandle equalHandle = equalSite.dynamicInvoker();

                MethodHandle filter = insertArguments(NEGATE, 1, runtime.getNil(), runtime.getTrue(), runtime.getFalse());
                mh = MethodHandles.filterReturnValue(equalHandle, filter);

                if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                    LOG.info(name() + "\tbound as specialized " + name() + ":" + Bootstrap.logMethod(method));
                }
            }
        }

        return mh;
    }

    public static final MethodHandle NEGATE = Binder.from(IRubyObject.class, IRubyObject.class, RubyNil.class, RubyBoolean.True.class, RubyBoolean.False.class).invokeStaticQuiet(LOOKUP, InvokeSite.class, "negate");

    public static IRubyObject negate(IRubyObject object, RubyNil nil, RubyBoolean.True tru, RubyBoolean.False fals) {
        return object == nil || object == fals ? tru : fals;
    }

    MethodHandle buildAliasHandle(CacheEntry entry, IRubyObject self) throws Throwable {
        MethodHandle mh = null;
        DynamicMethod method = entry.method;

        if (method instanceof PartialDelegatingMethod) {
            mh = getHandle(self, new CacheEntry(((PartialDelegatingMethod) method).getDelegate(), entry.sourceModule, entry.token));
        } else if (method instanceof AliasMethod) {
            AliasMethod alias = (AliasMethod) method;
            DynamicMethod innerMethod = alias.getRealMethod();
            String name = alias.getName();

            // Use a second site to mimic invocation from AliasMethod
            InvokeSite innerSite = (InvokeSite) SelfInvokeSite.bootstrap(LOOKUP, "callFunctional:" + name, type(), literalClosure ? 1 : 0, file, line);
            mh = innerSite.getHandle(self, new CacheEntry(innerMethod, entry.sourceModule, entry.token));

            alias.setHandle(mh);

            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                LOG.info(name() + "\tbound directly through alias to " + Bootstrap.logMethod(method));
            }
        }

        return mh;
    }

    MethodHandle buildStructHandle(CacheEntry entry) throws Throwable {
        MethodHandle mh = null;
        DynamicMethod method = entry.method;

        if (method instanceof RubyStruct.Accessor) {
            if (arity == 0) {
                RubyStruct.Accessor accessor = (RubyStruct.Accessor) method;
                int index = accessor.getIndex();

                mh = Binder.from(type())
                        .cast(type().changeParameterType(2, RubyStruct.class))
                        .permute(2)
                        .append(index)
                        .invokeVirtual(LOOKUP, "get");

                method.setHandle(mh);

                if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                    LOG.info(name() + "\tbound directly as Struct accessor " + Bootstrap.logMethod(method));
                }
            } else {
                if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                    LOG.info(name() + "\tcalled struct accessor with arity > 0 " + Bootstrap.logMethod(method));
                }
            }
        } else if (method instanceof RubyStruct.Mutator) {
            if (arity == 1) {
                RubyStruct.Mutator mutator = (RubyStruct.Mutator) method;
                int index = mutator.getIndex();

                mh = Binder.from(type())
                        .cast(type().changeParameterType(2, RubyStruct.class))
                        .permute(2, 3)
                        .append(index)
                        .invokeVirtual(LOOKUP, "set");

                method.setHandle(mh);

                if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                    LOG.info(name() + "\tbound directly as Struct mutator " + Bootstrap.logMethod(method));
                }
            } else {
                if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                    LOG.info(name() + "\tcalled struct mutator with arity > 1 " + Bootstrap.logMethod(method));
                }
            }
        }

        return mh;
    }

    /**
     * Update the given call site using the new target, wrapping with appropriate
     * bind and argument-juggling logic. Return a handle suitable for invoking
     * with the site's original method type.
     */
    protected MethodHandle updateInvocationTarget(MethodHandle target, IRubyObject self, RubyModule testClass, DynamicMethod method, SwitchPoint switchPoint) {
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
                    .invokeStaticQuiet(LOOKUP, Bootstrap.class, "testType");
        }

        gwt = MethodHandles.guardWithTest(test.handle(), target, fallback);

        // wrap in switchpoint for mutation invalidation
        gwt = switchPoint.guardWithTest(gwt, fallback);

        setTarget(gwt);

        tracker.addType(testClass.id);

        return target;
    }

    private void logMethodMissing() {
        if (LOG_BINDING) {
            LOG.debug(methodName + "\ttriggered site #" + siteID + " method_missing (" + file + ":" + line + ")");
        }
    }

    private void logBind(CacheAction action) {
        if (LOG_BINDING) {
            LOG.debug(methodName + "\ttriggered site #" + siteID + ' ' + action + " (" + file + ":" + line + ")");
        }
    }

    private void logPic(DynamicMethod method) {
        if (LOG_BINDING) {
            LOG.debug(methodName + "\tsite #" + siteID + " added to PIC " + logMethod(method));
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
        setTarget(target = prepareBinder(false).invokeVirtualQuiet(LOOKUP, "fail"));
        return target;
    }

    enum CacheAction { FAIL, BIND, REBIND, PIC }

    CacheAction testThresholds(RubyModule testClass) {
        if (tracker.clearCount() > Options.INVOKEDYNAMIC_MAXFAIL.load() ||
                (!tracker.hasSeenType(testClass.id) && tracker.seenTypesCount() + 1 > Options.INVOKEDYNAMIC_MAXPOLY.load())) {
            // Thresholds exceeded
            return CacheAction.FAIL;
        } else {
            // if we've cached no types, and the site is bound and we haven't seen this new type...
            if (tracker.seenTypesCount() > 0 && getTarget() != null && !tracker.hasSeenType(testClass.id)) {
                // stack it up into a PIC
                return CacheAction.PIC;
            } else {
                // wipe out site with this new type and method
                tracker.clearTypes();
                return boundOnce ? CacheAction.REBIND : CacheAction.BIND;
            }
        }
    }

    public static RubyClass pollAndGetClass(ThreadContext context, IRubyObject self) {
        context.callThreadPoll();
        return RubyBasicObject.getMetaClass(self);
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
    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self,
                                         RubyClass selfClass, String name, IRubyObject[] args, Block block) {
        return Helpers.callMethodMissing(context, self, selfClass, entry.method.getVisibility(), name, callType, args, block);
    }

    /**
     * Arity one method_missing invocation
     */
    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self,
                                         RubyClass selfClass, String name, IRubyObject arg0, Block block) {
        return Helpers.callMethodMissing(context, self, selfClass, entry.method.getVisibility(), name, callType, arg0, block);
    }


    /**
     * Arity two method_missing invocation
     */
    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self,
                                         RubyClass selfClass, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        return Helpers.callMethodMissing(context, self, selfClass, entry.method.getVisibility(), name, callType, arg0, arg1, block);
    }


    /**
     * Arity three method_missing invocation
     */
    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self,
                                         RubyClass selfClass, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return Helpers.callMethodMissing(context, self, selfClass, entry.method.getVisibility(), name, callType, arg0, arg1, arg2, block);
    }

    private static String logMethod(DynamicMethod method) {
        return "[#" + method.getSerialNumber() + ' ' + method.getImplementationClass() + ']';
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
            .invokeStaticQuiet(LOOKUP, InvokeSite.class, "testClass");
}
