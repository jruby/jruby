package org.jruby.compiler.ir.operands;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRModule;
import org.jruby.interpreter.InterpreterContext;

public class ModuleMetaObject extends MetaObject {
    public ModuleMetaObject(IRModule scope) {
        super(scope);
    }

    @Override
    public boolean isModule() {
        return true;
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
        RubyModule module = scope.getStaticScope().getModule();

        if (module != null) return module;

        Ruby runtime = interp.getRuntime();
        RubyModule container = getContainer(interp, runtime);
        module = container.defineOrGetModuleUnder(scope.getName());

        return interpretBody(interp, interp.getContext(), module);
    }

    @Override
    public Object store(InterpreterContext interp, Object value) {
        // Store it in live tree of modules/classes
        RubyModule container = (RubyModule) scope.getContainer().retrieve(interp);
        container.setConstant(scope.getName(), (RubyModule) value);

        // Save reference into scope for easy access
        scope.getStaticScope().setModule((RubyModule) value);
        return (RubyModule) value;
    }
}
