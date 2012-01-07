package org.jruby.compiler.ir.operands;

import java.util.List;

import org.jruby.compiler.ir.operands.Variable;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class CurrentModule extends Operand {
    public CurrentModule() { }

    @Override
    public String toString() {
        return "<current-module>";
    }

    @Override
    public void addUsedVariables(List<Variable> l) { 
        /* Nothing to do */
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        return currDynScope.getStaticScope().getModule();
    }
}
