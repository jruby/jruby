package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;

import java.util.Map;

public abstract class ReturnBase extends Instr {
    protected Operand returnValue;

    public ReturnBase(Operation op, Operand returnValue) {
        super(op);
        this.returnValue = returnValue;
        assert returnValue != null : "RETURN must have returnValue operand";
    }

    public Operand getReturnValue() {
        return this.returnValue;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { returnValue };
    }

    @Override
    public String toString() {
        return super.toString() + "(" + returnValue + ")";
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        returnValue = returnValue.getSimplifiedOperand(valueMap, force);
    }
}
