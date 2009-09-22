package org.jruby.compiler.ir.operands;

import java.util.List;
import java.util.Map;

public class Variable extends Operand implements Comparable
{
    final public String _name;

    public Variable(String n) { _name = n; }

    public String toString() { return _name; }

    public boolean isSelf() { return _name.equals("self"); }

    @Override
    public int hashCode() {
        return _name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return this._name.equals(((Variable)obj)._name);
    }

    public int compareTo(Object arg0) {
        if (arg0 instanceof Variable) {
            return _name.compareTo(((Variable)arg0)._name);
        }
        return 0;
    }

    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap)
    {
        // You can only value-replace atomic values
        Operand v = valueMap.get(this);
        return ((v != null) && !v.isNonAtomicValue()) ? v : this;
    }

    public Operand getValue(Map<Operand, Operand> valueMap)
    {
        Operand v = valueMap.get(this);
        return (v == null) ? this : v;
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l) { l.add(this); }
}
