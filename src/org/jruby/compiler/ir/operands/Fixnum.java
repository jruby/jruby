package org.jruby.compiler.ir.operands;

import org.jruby.compiler.ir.IR_Class;

import java.math.BigInteger;

public class Fixnum extends Constant
{
    final public Long _value;

    public Fixnum(Long val) { _value = val; }
    public Fixnum(BigInteger val) { _value = val.longValue(); }

    public String toString() { return _value + ":fixnum"; }

// ---------- These methods below are used during compile-time optimizations ------- 
    public Operand fetchCompileTimeArrayElement(int argIndex, boolean getSubArray) { return (argIndex == 0) ? this : Nil.NIL; }

    public IR_Class getTargetClass() { return IR_Class.getCoreClass("Fixnum"); }

    public Constant computeValue(String methodName, Constant arg)
    {
        if (arg instanceof Fixnum) {
            if (methodName.equals("+"))
                return new Fixnum(_value + ((Fixnum)arg)._value);
            else if (methodName.equals("-"))
                return new Fixnum(_value - ((Fixnum)arg)._value);
            else if (methodName.equals("*"))
                return new Fixnum(_value * ((Fixnum)arg)._value);
            else if (methodName.equals("/")) {
                Long divisor = ((Fixnum)arg)._value;
                return divisor == 0L ? null : new Fixnum(_value / divisor); // If divisor is zero, don't simplify!
            }
        }
        else if (arg instanceof Float) {
            if (methodName.equals("+"))
                return new Float(_value + ((Float)arg)._value);
            else if (methodName.equals("-"))
                return new Float(_value - ((Float)arg)._value);
            else if (methodName.equals("*"))
                return new Float(_value * ((Float)arg)._value);
            else if (methodName.equals("/")) {
                Double divisor = ((Float)arg)._value;
                return divisor == 0.0 ? null : new Float(_value / divisor); // If divisor is zero, don't simplify!
            }
        }

        return null;
    }
}
