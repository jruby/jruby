package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;

/**
 * result instr with no operands
 */
public abstract class NoOperandResultBaseInstr extends ResultBaseInstr {
    public NoOperandResultBaseInstr(Operation operation, Variable result) {
        super(operation, result);
    }

    public Operand[] getOperands() {
        return Instr.EMPTY_OPERANDS;
    }

    public void setOperand(int i, Operand operand) {
        throw new IllegalArgumentException("No such operand to set at index: " + i);
    }
}
