package org.jruby.ir.operands;

import org.jruby.ir.transformations.inlining.InlinerInfo;

import java.util.List;
import java.util.Map;

public abstract class Variable extends Operand implements Comparable {
    public final static String BLOCK          = "%block";
    public final static String CURRENT_SCOPE  = "%current_scope";
    public final static String CURRENT_MODULE = "%current_module";

    public abstract String getName();

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

    public boolean isImplicitBlockArg() {
        return getName().equals(BLOCK);
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

    public abstract Variable cloneForCloningClosure(InlinerInfo ii);

    @Override
    public Operand cloneForInlining(InlinerInfo ii) {
        return ii.getRenamedVariable(this);
    }
}
