package org.jruby.compiler.ir.operands;

import org.jruby.interpreter.InterpreterContext;

public class ArgIndex extends Operand {
    final public int index;

    public ArgIndex(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return Integer.toString(index);
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
        return new Integer(index); // Enebo: Silly
    }
}
