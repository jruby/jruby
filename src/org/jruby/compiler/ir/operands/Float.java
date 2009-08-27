package org.jruby.compiler.ir.operands;

import org.jruby.compiler.ir.IR_Class;

public class Float extends Constant
{
    final public Double _value;

    public Float(Double val) { _value = val; }

    public String toString() { return _value + ":float"; }

    public Operand fetchCompileTimeArrayElement(int argIndex, boolean getSubArray) { return (argIndex == 0) ? this : Nil.NIL; }

    public IR_Class getTargetClass() { return IR_Class.getCoreClass("Float"); }
}
