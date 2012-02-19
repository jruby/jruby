package org.jruby.compiler.ir.operands;

import java.util.List;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class CurrentModule extends Operand {
    public CurrentModule() { }

    @Override
    public String toString() {
        return "<current-module>";
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    @Override
    public void addUsedVariables(List<Variable> l) { 
        /* Nothing to do */
    }

    @Override
    public Operand cloneForInlining(InlinerInfo ii) {
        return this;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        return currDynScope.getStaticScope().getModule();
    }
}
