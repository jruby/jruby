package org.jruby.compiler.ir;

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

    public SValue(Operand a) { _array = a; }

    public boolean isConstant() { return _array.isConstant(); }

    public boolean isCompoundValue() { return true; }

    public String toString() { return "SValue(" + _array + ")"; }
}
