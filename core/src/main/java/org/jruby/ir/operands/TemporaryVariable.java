package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;

public abstract class TemporaryVariable extends Variable {
    public TemporaryVariable() {
        super(OperandType.TEMPORARY_VARIABLE);
    }

    /**
     * Differentiates between different types of TemporaryVariables (useful for switch and persistence).
     */
    public abstract TemporaryVariableType getType();

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof TemporaryVariable)) return false;

        return getType() == ((TemporaryVariable) other).getType();
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public int compareTo(Object other) {
        if (!(other instanceof TemporaryVariable)) return 0;

        return getName().compareTo(((TemporaryVariable) other).getName());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.TemporaryVariable(this);
    }

    @Override
    public String toString() {
        return getName();
    }
}
