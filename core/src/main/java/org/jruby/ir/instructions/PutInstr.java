package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRWriterEncoder;

// Represents target.ref = value or target = value where target is not a stack variable
public abstract class PutInstr extends TwoOperandInstr implements FixedArityInstr {
    protected String  ref;

    public PutInstr(Operation op, Operand target, String ref, Operand value) {
        super(op, target, value);

        this.ref = ref;
    }

    public String getRef() {
        return ref;
    }

    public Operand getTarget() {
        return getOperand1();
    }

    public Operand getValue() {
        return getOperand2();
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getTarget());
        e.encode(getRef());
        e.encode(getValue());
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"name: " + ref};
    }
}
