package org.jruby.compiler.ir.operands;

import java.util.List;
import java.util.Map;

public abstract class Variable extends Operand implements Comparable {
    public boolean isSelf() {
        return false;
    }

    public abstract String getName();

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap) {
        // You can only value-replace atomic values
        Operand v = valueMap.get(this);

        return ((v != null) && !v.isNonAtomicValue()) ? v : this;
    }

    @Override
    public Operand getValue(Map<Operand, Operand> valueMap) {
        Operand v = valueMap.get(this);

        return (v == null) ? this : v;
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l) {
        l.add(this);
    }
}
