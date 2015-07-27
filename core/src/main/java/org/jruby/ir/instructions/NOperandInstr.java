package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;

/**
 * For Instrs with an arbitrary number of Operands.
 */
public abstract class NOperandInstr extends Instr {
    protected Operand[] operands;

    public NOperandInstr(Operation operation, Operand[] operands) {
        super(operation);

        this.operands = operands;
    }

    public Operand[] getOperands() {
        return operands;
    }
}
