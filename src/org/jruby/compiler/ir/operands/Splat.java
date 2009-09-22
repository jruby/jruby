package org.jruby.compiler.ir.operands;

import java.util.List;
import java.util.Map;

// Represents a splat value in Ruby code: *array
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, it could get converted to calls that implement splat semantics
public class Splat extends Operand
{
    Operand _array;

    public Splat(Operand a) { _array = a; }

    public boolean isConstant() { return _array.isConstant(); }

    public String toString() { return "*" + _array; }

    public boolean isNonAtomicValue() { return true; }

    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap)
    {
        _array = _array.getSimplifiedOperand(valueMap);
        if (_array instanceof Variable) {
            _array = ((Variable)_array).getValue(valueMap);
        }
        return this;
    }

    public Operand fetchCompileTimeArrayElement(int argIndex, boolean getSubArray)
    {
        if (_array instanceof Array) 
            return ((Array)_array).fetchCompileTimeArrayElement(argIndex, getSubArray);
        else if (_array instanceof Range)
            return ((Range)_array).fetchCompileTimeArrayElement(argIndex, getSubArray);
        else
            return null;
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l)
    {
        _array.addUsedVariables(l);
    }
}
