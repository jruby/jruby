package org.jruby.ir.targets;

import com.headius.invokebinder.Binder;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CodegenUtils;
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
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

/**
 * Created by headius on 1/31/16.
 */
public class ConstantLookupSite extends MutableCallSite {
    private static final Logger LOG = LoggerFactory.getLogger("ConstantLookupSite");
    private final String name;
    private final boolean publicOnly;
    private final MethodHandles.Lookup lookup;

    public static final Handle BOOTSTRAP = new Handle(Opcodes.H_INVOKESTATIC, p(ConstantLookupSite.class), "constLookup", sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, int.class));

    public ConstantLookupSite(MethodHandles.Lookup lookup, MethodType type, String name, boolean publicOnly) {
        super(type);

        this.name = name;
        this.publicOnly = publicOnly;
        this.lookup = lookup;
    }

    public static CallSite constLookup(MethodHandles.Lookup lookup, String searchType, MethodType type, String constName, int publicOnly) {
        ConstantLookupSite site = new ConstantLookupSite(lookup, type, constName, publicOnly == 0 ? false : true);

        MethodHandle handle = Binder
                .from(lookup, type)
                .insert(0, site)
                .invokeVirtualQuiet(lookup, searchType);

        site.setTarget(handle);

        return site;
    }

    public IRubyObject searchConst(ThreadContext context, StaticScope staticScope) {
        // Lexical lookup
        Ruby runtime = context.getRuntime();
        RubyModule object = runtime.getObject();
        IRubyObject constant = (staticScope == null) ? object.getConstant(name) : staticScope.getConstantInner(name);

        // Inheritance lookup
        RubyModule module = null;
        if (constant == null) {
            // SSS FIXME: Is this null check case correct?
            module = staticScope == null ? object : staticScope.getModule();
            constant = publicOnly ? module.getConstantFromNoConstMissing(name, false) : module.getConstantNoConstMissing(name);
        }

        // Call const_missing or cache
        if (constant == null) {
            return module.callMethod(context, "const_missing", context.runtime.fastNewSymbol(name));
        }

        SwitchPoint switchPoint = (SwitchPoint) runtime.getConstantInvalidator(name).getData();

        // bind constant until invalidated
        MethodHandle target = Binder.from(type())
                .drop(0, 2)
                .constant(constant);
        MethodHandle fallback = getTarget();
        if (fallback == null) {
            fallback = Binder.from(type())
                    .insert(0, this)
                    .invokeVirtualQuiet(Bootstrap.LOOKUP, "searchConst");
        }

        setTarget(switchPoint.guardWithTest(target, fallback));

        if (Options.INVOKEDYNAMIC_LOG_CONSTANTS.load()) {
            LOG.info(name + "\tretrieved and cached from scope " + staticScope.getIRScope());// + " added to PIC" + extractSourceInfo(site));
        }

        return constant;
    }

    public IRubyObject searchModuleForConst(ThreadContext context, RubyModule module) {
        // Lexical lookup
        Ruby runtime = context.getRuntime();
        IRubyObject constant = publicOnly ? module.getConstantFromNoConstMissing(name, false) : module.getConstantNoConstMissing(name);

        // Call const_missing or cache
        if (constant == null) {
            return module.callMethod(context, "const_missing", context.runtime.fastNewSymbol(name));
        }

        SwitchPoint switchPoint = (SwitchPoint) runtime.getConstantInvalidator(name).getData();

        // bind constant until invalidated
        MethodHandle target = Binder.from(type())
                .drop(0, 2)
                .constant(constant);
        MethodHandle fallback = getTarget();
        if (fallback == null) {
            fallback = Binder.from(type())
                    .insert(0, this)
                    .invokeVirtualQuiet(Bootstrap.LOOKUP, "searchConst");
        }

        setTarget(switchPoint.guardWithTest(target, fallback));

        if (Options.INVOKEDYNAMIC_LOG_CONSTANTS.load()) {
            LOG.info(name + "\tretrieved and cached from module " + module);// + " added to PIC" + extractSourceInfo(site));
        }

        return constant;
    }

    public IRubyObject inheritanceSearchConst(ThreadContext context, IRubyObject cmVal) {
        Ruby runtime = context.runtime;
        RubyModule module;

        if (cmVal instanceof RubyModule) {
            module = (RubyModule) cmVal;
        } else {
            throw runtime.newTypeError(cmVal + " is not a type/class");
        }

        IRubyObject constant = publicOnly ? module.getConstantFromNoConstMissing(name, false) : module.getConstantNoConstMissing(name);

        if (constant == null) {
            constant = UndefinedValue.UNDEFINED;
        }

        SwitchPoint switchPoint = (SwitchPoint) runtime.getConstantInvalidator(name).getData();

        // bind constant until invalidated
        MethodHandle target = Binder.from(type())
                .drop(0, 2)
                .constant(constant);

        MethodHandle fallback = getTarget();
        if (fallback == null) {
            fallback = Binder.from(type())
                    .insert(0, this)
                    .invokeVirtualQuiet(Bootstrap.LOOKUP, "inheritanceSearchConst");
        }

        // test that module is same as before
        MethodHandle test = Binder.from(type().changeReturnType(boolean.class))
                .drop(0, 1)
                .insert(1, module.id)
                .invokeStaticQuiet(Bootstrap.LOOKUP, Bootstrap.class, "testArg0ModuleMatch");

        target = guardWithTest(test, target, fallback);

        setTarget(switchPoint.guardWithTest(target, fallback));

        if (Options.INVOKEDYNAMIC_LOG_CONSTANTS.load()) {
            LOG.info(name + "\tretrieved and cached from type " + cmVal.getMetaClass());// + " added to PIC" + extractSourceInfo(site));
        }

        return constant;
    }

    public IRubyObject lexicalSearchConst(ThreadContext context, StaticScope scope) {
        Ruby runtime = context.runtime;

        IRubyObject constant = scope.getConstantInner(name);

        if (constant == null) {
            constant = UndefinedValue.UNDEFINED;
        }

        SwitchPoint switchPoint = (SwitchPoint) runtime.getConstantInvalidator(name).getData();

        // bind constant until invalidated
        MethodHandle target = Binder.from(type())
                .drop(0, 2)
                .constant(constant);

        MethodHandle fallback = getTarget();
        if (fallback == null) {
            fallback = Binder.from(type())
                    .insert(0, this)
                    .invokeVirtualQuiet(Bootstrap.LOOKUP, "lexicalSearchConst");
        }

        setTarget(switchPoint.guardWithTest(target, fallback));

        if (Options.INVOKEDYNAMIC_LOG_CONSTANTS.load()) {
            LOG.info(name + "\tretrieved and cached from scope " + scope.getIRScope());// + " added to PIC" + extractSourceInfo(site));
        }

        return constant;
    }
}
