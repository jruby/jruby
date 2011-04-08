package org.jruby.compiler.ir.operands;

import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRModule;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.parser.StaticScope;

public class ModuleMetaObject extends MetaObject {
    protected ModuleMetaObject(IRModule scope) {
        super(scope);
    }

    @Override
    public boolean isModule() {
        return true;
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
		  // SSS FIXME: why would this be null? for core classes?
        StaticScope ssc =  scope.getStaticScope();
		  return ssc == null ? null : ssc.getModule();
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
