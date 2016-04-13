package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;

/**
 * Result Instr with one operand.
 */
public abstract class OneOperandResultBaseInstr extends ResultBaseInstr {
    private transient Operand operand1;

    public OneOperandResultBaseInstr(Operation operation, Variable result, Operand operand1) {
        super(operation, result);

        this.operand1 = operand1;
    }

    public Operand[] getOperands() {
        return new Operand[] { operand1 };
    }

    public Operand getOperand1() {
        return operand1;
    }

    public void setOperand1(Operand operand1) {
        this.operand1 = operand1;
    }

    public void setOperand(int i, Operand operand) {
        switch (i) {
            case 0:
                operand1 = operand;
                break;
            default:
                throw new IllegalArgumentException("No such operand to set at index: " + i);
        }
    }
}
