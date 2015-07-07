package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;

public abstract class ReturnBase extends Instr {
    public ReturnBase(Operation op, Operand returnValue) {
        super(op, new Operand[] { returnValue });

        assert returnValue != null : "RETURN must have returnValue operand";
    }

    public Operand getReturnValue() {
        return operands[0];
    }

    public void updateReturnValue(Operand val) {
        operands[0] = val;
    }
}
