package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;

public abstract class TemporaryVariable extends Variable {
    public TemporaryVariable() {
        super();
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.TEMPORARY_VARIABLE;
    }

    /**
     * Differentiates between different types of TemporaryVariables (useful for switch and persistence).
     */
    public abstract TemporaryVariableType getType();

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof TemporaryVariable)) return false;

        return ((TemporaryVariable)other).getId().equals(getId());
    }

    @Override
    public int compareTo(Object other) {
        if (!(other instanceof TemporaryVariable)) return 0;

        return getId().compareTo(((TemporaryVariable) other).getId());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.TemporaryVariable(this);
    }

    @Override
    public String toString() {
        return getId();
    }
}
