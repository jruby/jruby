package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;

import java.util.Map;
import org.jruby.ir.operands.StringLiteral;

// Represents target.ref = value or target = value where target is not a stack variable
public abstract class PutInstr extends Instr implements FixedArityInstr {
    private Operand target;
    private Operand value;
    protected String  ref;

    public PutInstr(Operation op, Operand target, String ref, Operand value) {
        super(op);

        this.target = target;
        this.value = value;
        this.ref = ref;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { target, new StringLiteral(ref), value };
    }

    public String getRef() {
        return ref;
    }

    public Operand getTarget() {
        return target;
    }

    public Operand getValue() {
        return value;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + target + (ref == null ? "" : ", " + ref) + ") = " + value;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        value = value.getSimplifiedOperand(valueMap, force);
        target = target.getSimplifiedOperand(valueMap, force);
    }
}
