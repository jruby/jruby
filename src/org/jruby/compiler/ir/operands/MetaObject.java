package org.jruby.compiler.ir.operands;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRClass;
import org.jruby.compiler.ir.IRModule;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IRScope;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class MetaObject extends Operand {
    public final IRScope scope;

    public MetaObject(IRScope s) {
        scope = s;
    }

    @Override
    public String toString() {
        return scope.toString();
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    public boolean isClass() {
        return scope instanceof IRClass;
    }

    // Note: This will also return true for an IRClass
    public boolean isModule() {
        return scope instanceof IRModule;
    }

    public boolean isClosure() {
        return scope instanceof IRClosure;
    }

    public Operand getContainer() {
        return scope.getContainer();
    }

    // SSS FIXME: Incomplete!
    @Override
    public IRClass getTargetClass() {
        return (scope instanceof IRModule) ? IRClass.getCoreClass("Module") : null;
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
        if (isClosure()) {
            BlockBody body = ((IRClosure) scope).getBlockBody();
            Binding binding = interp.getContext().currentBinding((IRubyObject) interp.getSelf());

            return new Block(body, binding);
        }

        // ENEBO: This should be fine for script/module/class...what else breaks?
        RubyModule module = scope.getStaticScope().getModule();

        if (module == null) {
            // TODO: Consolidate these two since they are nearly identical
            if (isClass()) {
                module = createClass(interp, interp.getContext(), interp.getContext().getRuntime());
            } else if (isModule()) {
                module = createModule(interp, interp.getContext(), interp.getContext().getRuntime());
            }
        }
        return module;
    }

    private RubyModule createModule(InterpreterContext interp, ThreadContext context, Ruby runtime) {
        RubyModule container = getContainer(interp, runtime);

        RubyModule module = container.defineOrGetModuleUnder(scope.getName());

        scope.getStaticScope().setModule(module);
        IRMethod rootMethod = ((IRModule) scope).getRootMethod();
        DynamicMethod method = new InterpretedIRMethod(rootMethod, module.getMetaClass());

        method.call(context, module, module.getMetaClass(), "", new IRubyObject[]{});

        return module;
    }

    private RubyModule createClass(InterpreterContext interp, ThreadContext context, Ruby runtime) {
        RubyModule container = getContainer(interp, runtime);

        // TODO: Get superclass
        RubyModule clazz = container.defineOrGetClassUnder(scope.getName(), runtime.getObject());
        scope.getStaticScope().setModule(clazz);
        IRMethod rootMethod = ((IRModule) scope).getRootMethod();
        DynamicMethod method = new InterpretedIRMethod(rootMethod, clazz.getMetaClass());

        method.call(context, clazz, clazz.getMetaClass(), "", new IRubyObject[]{});

        return clazz;
    }

    private RubyModule getContainer(InterpreterContext interp, Ruby runtime) {
        return scope.getContainer() != null ?
            (RubyModule) scope.getContainer().retrieve(interp) : runtime.getObject();
    }

    @Override
    public Object store(InterpreterContext interp, Object value) {
        if (!isClosure()) {
            // Store it in live tree of modules/classes
            RubyModule container = (RubyModule) scope.getContainer().retrieve(interp);
            container.setConstant(scope.getName(), (RubyModule) value);

            // Save reference into scope for easy access
            scope.getStaticScope().setModule((RubyModule) value);
            return (RubyModule) value;
        }

        return super.store(interp, value);
    }
}
