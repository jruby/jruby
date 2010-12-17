package org.jruby.compiler.ir.operands;

import org.jruby.interpreter.InterpreterContext;

public class ArgIndex extends Operand {
    final public int index;

    public ArgIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return Integer.toString(index);
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
        assert false : "Should not retreive ArgIndex as operand";
        return null;
    }
}
