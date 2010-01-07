package org.jruby.compiler.ir.operands;

import java.util.List;
import java.util.Map;

public class Variable extends Operand implements Comparable {
    final public String name;

    public Variable(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean isSelf() {
        return name.equals("self");
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        
        return this.name.equals(((Variable) obj).name);
    }

    public int compareTo(Object arg0) {
        if (arg0 instanceof Variable) {
            return name.compareTo(((Variable) arg0).name);
        }
        return 0;
    }

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
