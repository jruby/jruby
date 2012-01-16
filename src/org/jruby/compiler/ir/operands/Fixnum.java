package org.jruby.compiler.ir.operands;

import java.math.BigInteger;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class Fixnum extends Constant {
    final public Long value;
    private Object rubyFixnum;

    public Fixnum(Long val) {
        value = val;
        rubyFixnum = null;
    }

    public Fixnum(BigInteger val) { 
        value = val.longValue();
        rubyFixnum = null;
    }

    @Override
    public String toString() { 
        return value + ":fixnum";
    }

// ---------- These methods below are used during compile-time optimizations ------- 

    public Constant computeValue(String methodName, Constant arg) {
        if (arg instanceof Fixnum) {
            if (methodName.equals("+"))
                return new Fixnum(value + ((Fixnum)arg).value);
            else if (methodName.equals("-"))
                return new Fixnum(value - ((Fixnum)arg).value);
            else if (methodName.equals("*"))
                return new Fixnum(value * ((Fixnum)arg).value);
            else if (methodName.equals("/")) {
                Long divisor = ((Fixnum)arg).value;
                return divisor == 0L ? null : new Fixnum(value / divisor); // If divisor is zero, don't simplify!
            }
        } else if (arg instanceof Float) {
            if (methodName.equals("+"))
                return new Float(value + ((Float)arg).value);
            else if (methodName.equals("-"))
                return new Float(value - ((Float)arg).value);
            else if (methodName.equals("*"))
                return new Float(value * ((Float)arg).value);
            else if (methodName.equals("/")) {
                Double divisor = ((Float)arg).value;
                return divisor == 0.0 ? null : new Float(value / divisor); // If divisor is zero, don't simplify!
            }
        }

        return null;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        // Cache value so that when the same Fixnum Operand is copy-propagated across multiple instructions,
        // the same RubyFixnum object is created.  In addition, the same constant across loops should be
        // the same object.
        //
        // So, in this example, the output should be false, true, true
        //
        //    n = 0
        //    olda = nil
        //    while (n < 3)
        //      a = 34853
        //      p a.equal?(olda)
        //      olda = a
        //      n += 1
        //    end
        //
        if (rubyFixnum == null) rubyFixnum = context.getRuntime().newFixnum(value);
        return rubyFixnum;
    }
}
