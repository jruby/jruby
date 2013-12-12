package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;

import java.util.Map;

// Represents result = source.ref or result = source where source is not a stack variable
public abstract class GetInstr extends Instr implements ResultInstr {
    private Operand source;
    private String  ref;
    private Variable result;

    public GetInstr(Operation op, Variable result, Operand source, String ref) {
        super(op);

        assert result != null: "" + getClass().getSimpleName() + " result is null";

        this.source = source;
        this.ref = ref;
        this.result = result;
    }

    public String getRef() {
        return ref;
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    public Operand[] getOperands() {
        return new Operand[] { source };
    }

    public Operand getSource() {
        return source;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + source + (ref == null ? "" : ", " + ref) + ")";
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        source = source.getSimplifiedOperand(valueMap, force);
    }
}
