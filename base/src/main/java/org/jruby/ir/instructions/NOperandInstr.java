package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;

/**
 * For Instrs with an arbitrary number of Operands.
 */
public abstract class NOperandInstr extends Instr {
    protected final transient Operand[] operands;

    public NOperandInstr(Operation operation, Operand[] operands) {
        super(operation);

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
