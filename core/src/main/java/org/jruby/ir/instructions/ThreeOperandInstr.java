package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;

/**
 * An Instr which takes three Operands
 */
public abstract class ThreeOperandInstr extends Instr {
    private Operand operand1;
    private Operand operand2;
    private Operand operand3;

    public ThreeOperandInstr(Operation operation, Operand operand1, Operand operand2, Operand operand3) {
        super(operation);

        this.operand1 = operand1;
        this.operand2 = operand2;
        this.operand3 = operand3;
    }

    public Operand[] getOperands() {
        return new Operand[] { operand1, operand2, operand3 };
    }

    public Operand getOperand1() {
        return operand1;
    }

    public Operand getOperand2() {
        return operand2;
    }

    public Operand getOperand3() {
        return operand3;
    }
}
