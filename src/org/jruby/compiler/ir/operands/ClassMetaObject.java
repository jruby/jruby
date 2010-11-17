package org.jruby.compiler.ir.operands;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRClass;
import org.jruby.interpreter.InterpreterContext;


public class ClassMetaObject extends ModuleMetaObject {
    public ClassMetaObject(IRClass scope) {
        super(scope);
    }

    @Override
    public boolean isClass() {
        return true;
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
        RubyModule module = scope.getStaticScope().getModule();

        if (module != null) return module;

        Ruby runtime = interp.getRuntime();
        RubyModule container = getContainer(interp, runtime);
        // TODO: Get superclass
        module = container.defineOrGetClassUnder(scope.getName(), runtime.getObject());

        return interpretBody(interp, interp.getContext(), module);
    }
}
