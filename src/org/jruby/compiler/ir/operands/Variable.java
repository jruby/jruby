package org.jruby.compiler.ir.operands;

import java.util.Map;

public class Variable extends Operand implements Comparable
{
    final public String _name;

    public Variable(String n) { _name = n; }

    public String toString() { return _name; }

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

    public Operand getSimplifiedValue(Map<Operand, Operand> valueMap)
    {
        Operand v = valueMap.get(this);
        return (v == null) ? this : v;
/**
        Operand v = valueMap.get(this);
        if (v == null) {
            return this;
        }
        // SSS FIXME: You can only value-replace non-compound values
        // Otherwise, you might end up constructing the compound value over and over!
        else if (v.isCompoundOperand()) { 
            valueMap.put(this, v);
            return this;
        }
        else {
            return v;
        }
**/
    }
}
