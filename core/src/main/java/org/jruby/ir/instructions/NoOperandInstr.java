package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;

/**
 * An instr with no Operands
 */
public abstract class NoOperandInstr extends Instr {
    public NoOperandInstr(Operation operation) {
        super(operation);
    }

    public Operand[] getOperands() {
        return Instr.EMPTY_OPERANDS;
    }

    public void setOperand(int i, Operand operand) {
        throw new IllegalArgumentException("Setting operand on no-operand instr");
    }
}
