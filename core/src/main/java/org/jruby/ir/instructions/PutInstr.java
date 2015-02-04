package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;

// Represents target.ref = value or target = value where target is not a stack variable
public abstract class PutInstr extends Instr implements FixedArityInstr {
    protected String  ref;

    public PutInstr(Operation op, Operand target, String ref, Operand value) {
        super(op, new Operand[] { target, value });

        this.ref = ref;
    }

    public String getRef() {
        return ref;
    }

    public Operand getTarget() {
        return operands[0];
    }

    public Operand getValue() {
        return operands[1];
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"name: " + ref};
    }
}
