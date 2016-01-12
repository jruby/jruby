package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;

/**
 * Created by enebo on 7/27/15.
 */
public abstract class OneOperandInstr extends Instr {
    private transient Operand operand1;

    public OneOperandInstr(Operation operation, Operand operand1) {
        super(operation);

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
