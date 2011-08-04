package org.jruby.compiler.ir.operands;

import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRModule;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ModuleMetaObject extends MetaObject {
    protected ModuleMetaObject(IRModule scope) {
        super(scope);
    }

    @Override
    public boolean isModule() {
        return true;
    }

    @Override
    public Object retrieve(InterpreterContext interp, ThreadContext context, IRubyObject self) {
		  IRModule module = (IRModule)scope;
        StaticScope ssc =  module.getStaticScope();
        if (ssc != null) {
            return ssc.getModule();
        }
        else if (module.isACoreClass()) {
            // static scope would be null for core classes
            return module.getCoreClassModule(context.getRuntime());
        }
        else {
            // IR/Interpretation BUG?
            return null;
        }
    }

    @Override
    public Object store(InterpreterContext interp, ThreadContext context, IRubyObject self, Object value) {
        // Store it in live tree of modules/classes
        RubyModule container = (RubyModule) scope.getContainer().retrieve(interp, context, self);
        container.setConstant(scope.getName(), (RubyModule) value);

        // Save reference into scope for easy access
        scope.getStaticScope().setModule((RubyModule) value);
        return (RubyModule) value;
    }
}
