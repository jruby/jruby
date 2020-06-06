package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;

public abstract class ReturnBase extends OneOperandInstr {
    public ReturnBase(Operation op, Operand returnValue) {
        super(op, returnValue);

        assert returnValue != null : "RETURN must have returnValue operand";
    }

    public Operand getReturnValue() {
        return getOperand1();
    }

    public void updateReturnValue(Operand val) {
        setOperand1(val);
    }
}
