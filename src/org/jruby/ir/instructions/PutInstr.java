package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;

import java.util.Map;

// Represents target.ref = value or target = value where target is not a stack variable
public abstract class PutInstr extends Instr {
    public final int VALUE = 0;
    public final int TARGET = 1;

    protected Operand[] operands;
    protected String  ref;

    public PutInstr(Operation op, Operand target, String ref, Operand value) {
        super(op);

        operands = new Operand[] { value, target };
        this.ref = ref;
    }

    public Operand[] getOperands() {
        return operands;
    }

    public String getRef() {
        return ref;
    }

    public Operand getTarget() {
        return operands[TARGET];
    }

    public Operand getValue() {
        return operands[VALUE];
    }

    @Override
    public String toString() {
        return super.toString() + "(" + operands[TARGET] +
                (ref == null ? "" : ", " + ref) + ") = " + operands[VALUE];
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        operands[VALUE] = operands[VALUE].getSimplifiedOperand(valueMap, force);
        operands[TARGET] = operands[TARGET].getSimplifiedOperand(valueMap, force);
    }
}
