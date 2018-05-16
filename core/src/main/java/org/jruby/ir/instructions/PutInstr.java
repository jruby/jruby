package org.jruby.ir.instructions;

import org.jruby.RubySymbol;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRWriterEncoder;

// Represents target.ref = value or target = value where target is not a stack variable
public abstract class PutInstr extends TwoOperandInstr implements FixedArityInstr {
    protected RubySymbol name;

    public PutInstr(Operation op, Operand target, RubySymbol name, Operand value) {
        super(op, target, value);

        this.name = name;
    }

    public String getId() {
        return name.idString();
    }

    public RubySymbol getName() {
        return name;
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
        e.encode(getName());
        e.encode(getValue());
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"name: " + name};
    }
}
