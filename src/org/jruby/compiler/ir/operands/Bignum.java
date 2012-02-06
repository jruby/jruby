package org.jruby.compiler.ir.operands;

import java.math.BigInteger;
import java.util.List;
import org.jruby.RubyBignum;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class Bignum extends Operand {
    final public BigInteger value;
    private Object rubyBignum;

    public Bignum(BigInteger value) {
        this.value = value;
    }

    @Override
    public boolean hasKnownValue() {
        return true;
    }
    
    @Override
    public void addUsedVariables(List<Variable> l) {
        /* do nothing */
    }

    @Override
    public String toString() { 
        return value + ":bignum";
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        // Cache value so that when the same Bignum Operand is copy-propagated across multiple instructions,
        // the same RubyBignum object is created.  In addition, the same constant across loops should be
        // the same object.
        //
        // So, in this example, the output should be false, true, true
        //
        //    n = 0
        //    olda = nil
        //    while (n < 3)
        //      a = 81402749386839761113321
        //      p a.equal?(olda)
        //      olda = a
        //      n += 1
        //    end
        //
        if (rubyBignum == null) rubyBignum = RubyBignum.newBignum(context.getRuntime(), value);
        return rubyBignum;
    }
}
