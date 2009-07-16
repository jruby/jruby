package org.jruby.compiler.ir;

public class Float extends Constant
{
    final public Double _value;

    public Float(Double val) { _value = val; }

    public String toString() { return _value + ":float"; }

    public Operand fetchCompileTimeArrayElement(int argIndex, boolean getSubArray) { return (argIndex == 0) ? this : Nil.NIL; }
}
