package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;

/**
 * Created by enebo on 7/28/15.
 */
public abstract class NOperandResultBaseInstr extends ResultBaseInstr {
    private final transient Operand[] operands;

    public NOperandResultBaseInstr(Operation operation, Variable result, Operand[] operands) {
        super(operation, result);

        this.operands = operands;
    }

    public Operand[] getOperands() {
        return operands;
    }

    public void setOperand(int i, Operand operand) {
        if (i < 0 || i >= operands.length) {
            throw new IllegalArgumentException("No such operand to set at index: " + i);
        }

        operands[i] = operand;
    }
}
