package org.jruby.compiler.ir.operands;

import java.util.List;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class Float extends Operand {
    final public Double value;
    private Object rubyFloat;

    public Float(Double value) {
        this.value = value;
        rubyFloat = null;
    }

    // FIXME: Enebo I don't think floats are constant since they can set precision per instance.
    @Override
    public boolean hasKnownValue() {
        return true;
    }

    @Override
    public void addUsedVariables(List<Variable> l) {
        /* not used */
    }

    @Override
    public String toString() {
        return value + ":float";
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
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        if (rubyFloat == null) rubyFloat = context.getRuntime().newFloat(value);
        return rubyFloat;
    }
}
