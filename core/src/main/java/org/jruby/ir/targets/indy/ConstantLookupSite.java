package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.targets.SiteTracker;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.SwitchPoint;

import static java.lang.invoke.MethodHandles.guardWithTest;
import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Convert.castAsModule;
import static org.jruby.api.Error.typeError;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

/**
 * Created by headius on 1/31/16.
 */
public class ConstantLookupSite extends MutableCallSite {
    private static final Logger LOG = LoggerFactory.getLogger(ConstantLookupSite.class);
    private final String name;
    private final boolean publicOnly;
    private final boolean callConstMissing;

    private volatile RubySymbol symbolicName;

    private final SiteTracker tracker = new SiteTracker();

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(ConstantLookupSite.class),
            "constLookup",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, int.class, int.class),
            false);

    public ConstantLookupSite(MethodType type, String name, boolean publicOnly, boolean callConstMissing) {
        super(type);

        this.name = name;
        this.publicOnly = publicOnly;
        this.callConstMissing = callConstMissing;
    }

    public static CallSite constLookup(MethodHandles.Lookup lookup, String searchType, MethodType type, String constName, int publicOnly, int callConstMissing) {
        ConstantLookupSite site = new ConstantLookupSite(type, constName, publicOnly == 0 ? false : true, callConstMissing == 0 ? false : true);

        MethodHandle handle = Binder
                .from(lookup, type)
                .insert(0, site)
                .invokeVirtualQuiet(lookup, searchType);

        site.setTarget(handle);

        return site;
    }

    private RubySymbol getSymbolicName(ThreadContext context) {
        RubySymbol symbolicName = this.symbolicName;
        if (symbolicName != null) return symbolicName;
        return this.symbolicName = context.runtime.fastNewSymbol(name);
    }

    public IRubyObject searchConst(ThreadContext context, StaticScope staticScope) {
        // Lexical lookup
        var object = objectClass(context);

        // get switchpoint before value
        SwitchPoint switchPoint = getSwitchPointForConstant(context.runtime);
        IRubyObject constant = staticScope == null ?
                object.getConstant(context, name) : staticScope.getScopedConstant(context, name);

        // Inheritance lookup
        RubyModule module = null;
        if (constant == null) {
            // SSS FIXME: Is this null check case correct?
            module = staticScope == null ? object : staticScope.getModule();
            constant = publicOnly ?
                    module.getConstantFromNoConstMissing(name, false) :
                    module.getConstantNoConstMissing(context, name);
        }

        // Call const_missing or cache
        if (constant == null) {
            if (callConstMissing) {
                return module.callMethod(context, "const_missing", getSymbolicName(context));
            } else {
                return UndefinedValue.UNDEFINED;
            }
        }

        // bind constant until invalidated
        MethodHandle target = Binder.from(type())
                .drop(0, 2)
                .constant(constant);
        MethodHandle fallback = Binder.from(type())
                .insert(0, this)
                .invokeVirtualQuiet(LOOKUP, "searchConst");

        setTarget(switchPoint.guardWithTest(target, fallback));

        if (Options.INVOKEDYNAMIC_LOG_CONSTANTS.load()) {
            LOG.info(name + "\tretrieved and cached from scope (searchConst) " + staticScope.getIRScope());
        }

        return constant;
    }

    public IRubyObject searchModuleForConst(ThreadContext context, IRubyObject cmVal) throws Throwable {
        if (!(cmVal instanceof RubyModule)) throw typeError(context, "", cmVal, " is not a class/module");
        RubyModule module = (RubyModule) cmVal;

        if (checkForBailout(module)) {
            return bail(context, cmVal, noCacheSMFC());
        }

        // Inheritance lookup
        Ruby runtime = context.getRuntime();

        // get switchpoint before value
        SwitchPoint switchPoint = getSwitchPointForConstant(runtime);
        IRubyObject constant = publicOnly ?
                module.getConstantFromNoConstMissing(name, false) :
                module.getConstantNoConstMissing(context, name);

        // Call const_missing or cache
        if (constant == null) {
            if (callConstMissing) {
                return module.callMethod(context, "const_missing", getSymbolicName(context));
            } else {
                return UndefinedValue.UNDEFINED;
            }
        }

        // bind constant until invalidated
        bind(runtime, module, switchPoint, constant, SMFC());

        if (Options.INVOKEDYNAMIC_LOG_CONSTANTS.load()) {
            LOG.info(name + "\tretrieved and cached from module (searchModuleForConst) " + cmVal.getMetaClass());// + " added to PIC" + extractSourceInfo(site));
        }

        return constant;
    }

    private SwitchPoint getSwitchPointForConstant(Ruby runtime) {
        return (SwitchPoint) runtime.getConstantInvalidator(name).getData();
    }

    public IRubyObject noCacheSearchModuleForConst(ThreadContext context, IRubyObject cmVal) {
        if (!(cmVal instanceof RubyModule module)) throw typeError(context, cmVal + " is not a type/class");
        // Inheritance lookup
        IRubyObject constant = publicOnly ?
                module.getConstantFromNoConstMissing(name, false) :
                module.getConstantNoConstMissing(context, name);

        // Call const_missing or cache
        return constant == null ?
                module.callMethod(context, "const_missing", getSymbolicName(context)) : constant;
    }

    public IRubyObject inheritanceSearchConst(ThreadContext context, IRubyObject cmVal) throws Throwable {
        if (!(cmVal instanceof RubyModule)) throw typeError(context, cmVal, cmVal + " is not a class/module");
        RubyModule module = (RubyModule) cmVal;

        if (checkForBailout(module)) {
            return bail(context, cmVal, noCacheISC());
        }

        Ruby runtime = context.runtime;

        // get switchpoint before value
        SwitchPoint switchPoint = getSwitchPointForConstant(runtime);

        // Inheritance lookup
        IRubyObject constant = module.getConstantNoConstMissingSkipAutoload(context, name);
        if (constant == null) constant = UndefinedValue.UNDEFINED;

        // bind constant until invalidated
        bind(runtime, module, switchPoint, constant, ISC());

        tracker.addType(module.id);

        if (Options.INVOKEDYNAMIC_LOG_CONSTANTS.load()) {
            LOG.info(name + "\tconstant cached from type (inheritanceSearchConst) " + cmVal.getMetaClass());
        }

        return constant;
    }

    public IRubyObject noCacheInheritanceSearchConst(ThreadContext context, IRubyObject cmVal) {
        if (!(cmVal instanceof RubyModule)) throw typeError(context, "", cmVal, " is not a type/class");
        RubyModule module = (RubyModule) cmVal;

        // Inheritance lookup
        IRubyObject constant = module.getConstantNoConstMissingSkipAutoload(context, name);

        return constant == null ? UndefinedValue.UNDEFINED : constant;
    }

    private MethodHandle getFallback(RubyModule module, MethodHandle cachingFallback) {
        MethodHandle fallback;// if we've cached any types and we haven't seen this new type...
        if (tracker.seenTypesCount() > 0 && !tracker.hasSeenType(module.id)) {

            // stack it up into a PIC
            if (Options.INVOKEDYNAMIC_LOG_CONSTANTS.load()) LOG.info(name + "\tconstant added to PIC");

            fallback = getTarget();

        } else {

            // wipe out site with this new type
            if (Options.INVOKEDYNAMIC_LOG_CONSTANTS.load()) LOG.info(name + "\tconstant " + (tracker.seenTypesCount() > 0 ? "rebound" : "bound"));

            fallback = cachingFallback;
            tracker.clearTypes();
        }
        return fallback;
    }

    private boolean checkForBailout(RubyModule module) {
        // Invalidated too many times
        if (tracker.clearCount() > Options.INVOKEDYNAMIC_MAXFAIL.load()) {
            if (Options.INVOKEDYNAMIC_LOG_CONSTANTS.load()) LOG.info(name + "\tinvalidated more than " + Options.INVOKEDYNAMIC_MAXFAIL.load() + " times ");
            return true;
        }

        // Too many types encountered
        if ((!tracker.hasSeenType(module.id) && tracker.seenTypesCount() + 1 > Options.INVOKEDYNAMIC_MAXPOLY.load())) {
            if (Options.INVOKEDYNAMIC_LOG_CONSTANTS.load()) LOG.info(name + "\tencountered more than " + Options.INVOKEDYNAMIC_MAXPOLY.load() + " types ");
            return true;
        }

        return false;
    }

    private IRubyObject bail(ThreadContext context, IRubyObject cmVal, MethodHandle noncachingFallback) throws Throwable {
        setTarget(noncachingFallback);
        return (IRubyObject) noncachingFallback.invokeExact(context, cmVal);
    }

    private void bind(Ruby runtime, RubyModule module, SwitchPoint switchPoint, IRubyObject constant, MethodHandle cachingFallback) {
        MethodHandle target = Binder.from(type())
                .drop(0, 2)
                .constant(constant);

        // Get appropriate fallback given state of site
        MethodHandle fallback = getFallback(module, cachingFallback);

        // Test that module is same as before
        target = guardWithTest(module.getIdTest(), target, fallback);

        target = switchPoint.guardWithTest(target, fallback);

        setTarget(target);
    }

    public IRubyObject lexicalSearchConst(ThreadContext context, StaticScope scope) {
        Ruby runtime = context.runtime;

        // get switchpoint before value
        SwitchPoint switchPoint = getSwitchPointForConstant(runtime);
        IRubyObject constant = scope.getConstantDefined(context, name);

        if (constant == null) {
            constant = UndefinedValue.UNDEFINED;
        }

        // bind constant until invalidated
        MethodHandle target = Binder.from(type())
                .drop(0, 2)
                .constant(constant);

        MethodHandle fallback = Binder.from(type())
                .insert(0, this)
                .invokeVirtualQuiet(LOOKUP, "lexicalSearchConst");

        setTarget(switchPoint.guardWithTest(target, fallback));

        if (Options.INVOKEDYNAMIC_LOG_CONSTANTS.load()) {
            LOG.info(name + "\tretrieved and cached from scope (lexicalSearchConst) " + scope.getIRScope());// + " added to PIC" + extractSourceInfo(site));
        }

        return constant;
    }

    private MethodHandle _SMFC;
    private MethodHandle SMFC() {
        if (_SMFC != null) return _SMFC;
        return _SMFC = Binder.from(type())
                .insert(0, this)
                .invokeVirtualQuiet(LOOKUP, "searchModuleForConst");
    }

    private MethodHandle _noCacheSMFC;
    private MethodHandle noCacheSMFC() {
        if (_noCacheSMFC != null) return _noCacheSMFC;
        return _noCacheSMFC = Binder.from(type())
                .insert(0, this)
                .invokeVirtualQuiet(LOOKUP, "noCacheSearchModuleForConst");
    }

    private MethodHandle _ISC;
    private MethodHandle ISC() {
        if (_ISC != null) return _ISC;
        return _ISC = Binder.from(type())
                .insert(0, this)
                .invokeVirtualQuiet(LOOKUP, "inheritanceSearchConst");
    }

    private MethodHandle _noCacheISC;
    private MethodHandle noCacheISC() {
        if (_noCacheISC != null) return _noCacheISC;
        return _noCacheISC = Binder.from(type())
                .insert(0, this)
                .invokeVirtualQuiet(LOOKUP, "noCacheInheritanceSearchConst");
    }
}
