package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;

// These instructions show up in three contexts:
// - To assign args in parenthesized units: |.., (a,b,c), .. |
// - Regular multiple/parallel assignments: x,y,*z = ...
// - When blocks are inlined, all receive* instructions get
//   converted into multiple-assign instructions
public abstract class MultipleAsgnBase extends OneOperandResultBaseInstr {
    protected final int index;

    public MultipleAsgnBase(Operation op, Variable result, Operand array, int index) {
        super(op, result, array);

        assert result != null : "MultipleAsgnBase result is null";

        this.index = index;
    }

    public Operand getArray() {
        return getOperand1();
    }

    public int getIndex() {
        return index;
    }
}
