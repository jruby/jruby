package org.jruby.ir.operands;

import org.jruby.RubySymbol;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

import java.util.List;
import java.util.Map;

public abstract class Variable extends Operand implements Comparable {
    public Variable() {
        super();
    }

    public abstract String getId();

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap, boolean force) {
        Operand v = valueMap.get(this);
        // You can only value-replace atomic values
        return (v != null) && (force || v.canCopyPropagate()) ? v : this;
    }

    public boolean isSelf() {
        return false;
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

    public abstract Variable clone(SimpleCloneInfo ii);

    @Override
    public Operand cloneForInlining(CloneInfo ii) {
        return ii.getRenamedVariable(this);
    }
}
