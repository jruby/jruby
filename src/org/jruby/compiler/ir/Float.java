package org.jruby.compiler.ir;

public class Float extends Constant
{
    final public Double _value;

    public Float(Double val) { _value = val; }

    public Operand fetchCompileTimeArrayElement(int argIndex)
    {
        return (argIndex == 0) ? this : Nil.NIL;
    }

    public String toString() { return _value + ":float"; }
}
