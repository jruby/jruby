package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.runtime.ThreadContext;

public class Float extends ImmutableLiteral {
    final public Double value;

    public Float(Double value) {
        this.value = value;
    }

    @Override
    public boolean hasKnownValue() {
        return true;
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return context.getRuntime().newFloat(value);
    }

    @Override
    public String toString() {
        return "Float:" + value;
    }

    public Operand computeValue(String methodName, Operand arg) {
        Double v1 = value;
        Double v2 = (arg instanceof Fixnum) ? 1.0 * ((Fixnum)arg).value : (Double)((Float)arg).value;

        if (methodName.equals("+")) {
            return new Float(v1 + v2);
        } else if (methodName.equals("-")) {
            return new Float(v1 - v2);
        } else if (methodName.equals("*")) {
            return new Float(v1 * v2);
        } else if (methodName.equals("/")) {
            return v2 == 0.0 ? null : new Float(v1 / v2); // If divisor is zero, don't simplify!
        }

        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Float(this);
    }
}
