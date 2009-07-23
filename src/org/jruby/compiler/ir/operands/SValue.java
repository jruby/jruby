package org.jruby.compiler.ir.operands;

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
    final public Operand _array;
    private Operand _simplifiedValue;

    public SValue(Operand a) { _array = a; }

    public boolean isConstant() { return _array.isConstant(); }

    public String toString() { return "SValue(" + _array + ")"; }

    public Operand getSimplifiedValue()
    {
        if (!isConstant())
            return this;

        Operand so = _array.getSimplifiedValue();
        if (so instanceof Array) {
            Array a = (Array)so;
            return (a._elts.length == 1) ? a._elts[0] : a;
        }
        else {
            return this;
        }
    }

    public Operand fetchCompileTimeArrayElement(int argIndex, boolean getSubArray)
    {
        // SSS FIXME: This is not the right approach -- we'll need to reset this value on each opt. pass.
        if (_simplifiedValue == null)
            _simplifiedValue = getSimplifiedValue();

        return (_simplifiedValue == this) ? null : _simplifiedValue.fetchCompileTimeArrayElement(argIndex, getSubArray);
    }
}
