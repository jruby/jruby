package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;

import java.util.Map;

// These instructions show up in three contexts:
// - To assign args in parenthesized units: |.., (a,b,c), .. |
// - Regular multiple/parallel assignments: x,y,*z = ...
// - When blocks are inlined, all receive* instructions get
//   converted into multiple-assign instructions
public abstract class MultipleAsgnBase extends Instr implements ResultInstr {
    protected final int index;

    public MultipleAsgnBase(Operation op, Variable result, Operand array, int index) {
        super(op, result, new Operand[] { array });

        assert result != null : "MultipleAsgnBase result is null";

        this.index = index;
    }

    public Operand getArray() {
        return operands[0];
    }

    @Override
    public Variable getResult() {
        return result;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public void updateResult(Variable v) {
        this.result = v;
    }
}
