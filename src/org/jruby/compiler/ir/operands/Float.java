package org.jruby.compiler.ir.operands;

import org.jruby.compiler.ir.IR_Class;

public class Float extends Constant
{
    final public Double _value;

    public Float(Double val) { _value = val; }

    public String toString() { return _value + ":float"; }

    public Operand fetchCompileTimeArrayElement(int argIndex, boolean getSubArray) { return (argIndex == 0) ? this : Nil.NIL; }

    public IR_Class getTargetClass() { return IR_Class.getCoreClass("Float"); }

    public Constant computeValue(String methodName, Constant arg)
    {
        Double v1 = _value;
        Double v2 = (arg instanceof Fixnum) ? 1.0 * ((Fixnum)arg)._value : (Double)((Float)arg)._value;

        if (methodName.equals("+"))
            return new Float(v1 + v2);
        else if (methodName.equals("-"))
            return new Float(v1 - v2);
        else if (methodName.equals("*"))
            return new Float(v1 * v2);
        else if (methodName.equals("/")) {
            return v2 == 0.0 ? null : new Float(v1 / v2); // If divisor is zero, don't simplify!
        }

        return null;
    }
}
