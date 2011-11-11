package org.jruby.compiler.ir.operands;

import java.util.List;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ArgIndex extends Operand {
    final public int index;

    public ArgIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public void addUsedVariables(List<Variable> l) { 
        /* Nothing to do */
    }

    @Override
    public String toString() {
        return Integer.toString(index);
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, Object[] temp) {
        assert false : "Should not retreive ArgIndex as operand";
        return null;
    }
}
