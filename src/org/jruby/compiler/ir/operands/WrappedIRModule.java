package org.jruby.compiler.ir.operands;

import org.jruby.compiler.ir.IRBody;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class WrappedIRModule extends Constant {
    private final IRBody module;

    public WrappedIRModule(IRBody scope) {
        this.module = scope;
        
        assert module != null: "We should never wrap nothing";
    }

    public IRBody getModule() {
        return module;
    }

    @Override
    public String toString() {
        return (module == null) ? "<current-module>" : module.getName();
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        StaticScope staticScope = module.getStaticScope();

        return staticScope != null ? staticScope.getModule() : context.runtime.getClass(module.getName());
    }
}
