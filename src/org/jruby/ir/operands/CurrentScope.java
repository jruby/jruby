package org.jruby.ir.operands;

import java.util.List;
import org.jruby.ir.IRScope;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class CurrentScope extends Operand {
    private IRScope current;
    
    public CurrentScope(IRScope current) {
        this.current = current;
    }

    @Override
    public String toString() {
        return "<current-scope>";
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
    public boolean canCopyPropagate() {
        return true;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        return current.getStaticScope();
    }
}
