package org.jruby.compiler.ir.operands;

import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRModule;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ModuleMetaObject extends MetaObject {
    public static final ModuleMetaObject CURRENT_MODULE = new ModuleMetaObject(null);

    protected ModuleMetaObject(IRModule scope) {
        super(scope);
    }

    @Override
    public boolean isModule() {
        return true;
    }

    @Override
    public String toString() {
        return (scope == null) ? "<current-module>" : scope.getName();
    }

    @Override
    public Object retrieve(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        IRModule module = (IRModule)scope;
        StaticScope ssc = (module == null) ? context.getCurrentScope().getStaticScope() : module.getStaticScope();
        if (ssc != null) {
            return ssc.getModule();
        } else if (module.isACoreClass()) {
            // static scope would be null for core classes
            return module.getCoreClassModule(context.getRuntime());
        } else {
            // IR/Interpretation BUG?
            return null;
        }
    }
}
