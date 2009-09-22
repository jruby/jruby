package org.jruby.compiler.ir.operands;

import java.util.List;
import java.util.Map;

// Represents a regexp from ruby
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, this regexp operand could get converted to calls
// that actually build the Regexp object
public class Regexp extends Operand
{
    final public int _opts;
    Operand _re;

    public Regexp(Operand re, int opts) { _re = re; _opts = opts; }

    public boolean isConstant() { return _re.isConstant(); }

    public String toString() { return "RE:|" + _re + "|" + _opts; }

    public boolean isNonAtomicValue() { return true; }

    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap)
    {
        _re = _re.getSimplifiedOperand(valueMap);
        return this;
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l)
    {
        _re.addUsedVariables(l);
    }
}
