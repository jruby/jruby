package org.jruby.ir.targets;

import com.headius.invokebinder.Binder;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.SwitchPoint;

import static java.lang.invoke.MethodHandles.guardWithTest;

/**
 * Created by headius on 1/31/16.
 */
public class ConstantLookupSite extends MutableCallSite {
    private static final Logger LOG = LoggerFactory.getLogger("ConstantLookupSite");
    private final boolean publicOnly;

    public ConstantLookupSite(MethodType type, boolean publicOnly) {
        super(type);
        this.publicOnly = publicOnly;

    }

    public IRubyObject searchConst(ThreadContext context, StaticScope staticScope, RubySymbol name) {
        // Lexical lookup
        Ruby runtime = context.getRuntime();
        RubyModule object = runtime.getObject();
        IRubyObject constant = (staticScope == null) ? object.getConstant(name.toID()) : staticScope.getConstantInner(name.toID());

        // Inheritance lookup
        RubyModule module = null;
        if (constant == null) {
            // SSS FIXME: Is this null check case correct?
            module = staticScope == null ? object : staticScope.getModule();
            constant = publicOnly ? module.getConstantFromNoConstMissing(name.toID(), false) : module.getConstantNoConstMissing(name.toID());
        }

        // Call const_missing or cache
        if (constant == null) {
            return module.callMethod(context, "const_missing", name);
        }

        SwitchPoint switchPoint = (SwitchPoint) runtime.getConstantInvalidator(name.toID()).getData();

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

    public IRubyObject inheritanceSearchConst(ThreadContext context, IRubyObject cmVal, RubySymbol name) {
        Ruby runtime = context.runtime;
        RubyModule module;

        if (cmVal instanceof RubyModule) {
            module = (RubyModule) cmVal;
        } else {
            throw runtime.newTypeError(cmVal + " is not a type/class");
        }

        IRubyObject constant = publicOnly ? module.getConstantFromNoConstMissing(name.toID(), false) : module.getConstantNoConstMissing(name.toID());

        if (constant == null) {
            constant = UndefinedValue.UNDEFINED;
        }

        SwitchPoint switchPoint = (SwitchPoint) runtime.getConstantInvalidator(name.toID()).getData();

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

    public IRubyObject lexicalSearchConst(ThreadContext context, StaticScope scope, RubySymbol name) {
        Ruby runtime = context.runtime;

        IRubyObject constant = scope.getConstantInner(name.toID());

        if (constant == null) {
            constant = UndefinedValue.UNDEFINED;
        }

        SwitchPoint switchPoint = (SwitchPoint) runtime.getConstantInvalidator(name.toID()).getData();

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
