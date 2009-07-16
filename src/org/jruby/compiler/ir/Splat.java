package org.jruby.compiler.ir;

// Represents a splat value in Ruby code: *array
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, it could get converted to calls that implement splat semantics
public class Splat extends Operand
{
    final public Operand _array;

    public Splat(Operand a) { _array = a; }

    public boolean isConstant() { return _array.isConstant(); }

    public String toString() { return "*" + _array; }

    public Operand fetchCompileTimeArrayElement(int argIndex, boolean getSubArray)
    {
        if (_array instanceof Array) 
            return ((Array)_array).fetchCompileTimeArrayElement(argIndex, getSubArray);
        else if (_array instanceof Range)
            return ((Range)_array).fetchCompileTimeArrayElement(argIndex, getSubArray);
        else
            return null;
    }
}
