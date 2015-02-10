package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;

// Represents result = source.ref or result = source where source is not a stack variable
public abstract class GetInstr extends ResultBaseInstr implements FixedArityInstr {
    private final String  ref;

    public GetInstr(Operation op, Variable result, Operand source, String ref) {
        super(op, result, new Operand[] { source });

        assert result != null: "" + getClass().getSimpleName() + " result is null";

        this.ref = ref;
    }

    public String getRef() {
        return ref;
    }

    public Operand getSource() {
        return operands[0];
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"name: " + ref};
    }
}