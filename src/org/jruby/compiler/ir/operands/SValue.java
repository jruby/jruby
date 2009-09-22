package org.jruby.compiler.ir.operands;

import java.util.List;
import java.util.Map;

// Represents a svalue node in Ruby code
//
// According to headius, svalue evaluates its value node and returns:
//  * nil if it does not evaluate to an array or if it evaluates to an empty array
//  * the first element if it evaluates to a one-element array
//  * the array if it evaluates to a >1 element array
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, it could get converted to calls
//
public class SValue extends Operand
{
    Operand _array;

    public SValue(Operand a) { _array = a; }

    public boolean isConstant() { return _array.isConstant(); }

    public String toString() { return "SValue(" + _array + ")"; }

    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap)
    {
        _array = _array.getSimplifiedOperand(valueMap);
        if (_array instanceof Array) {
            Array a = (Array)_array;
            return (a._elts.length == 1) ? a._elts[0] : a;
        }
        else {
            return this;
        }
    }

    public Operand fetchCompileTimeArrayElement(int argIndex, boolean getSubArray)
    {
        // SSS FIXME: This should never get called for constant svalues
        return null;
    }

    public boolean isNonAtomicValue() { return true; }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l)
    {
        _array.addUsedVariables(l);
    }
}
