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

    public boolean isCompoundValue() { return true; }

    public String toString() { return "*" + _array; }
}
