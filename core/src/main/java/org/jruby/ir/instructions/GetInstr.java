package org.jruby.ir.instructions;

import org.jruby.RubySymbol;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRWriterEncoder;

// Represents result = source.ref or result = source where source is not a stack variable
public abstract class GetInstr extends OneOperandResultBaseInstr implements FixedArityInstr {
    private final RubySymbol name;

    public GetInstr(Operation op, Variable result, Operand source, RubySymbol name) {
        super(op, result, source);

        assert result != null: getClass().getSimpleName() + " result is null";

        this.name = name;
    }

    public String getId() {
        return name.idString();
    }

    public RubySymbol getName() {
        return name;
    }

    public Operand getSource() {
        return getOperand1();
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getSource());
        e.encode(getName());
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"name: " + getName()};
    }
}