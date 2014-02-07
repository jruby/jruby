package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.runtime.ThreadContext;

import java.math.BigInteger;

/*
 * Represents a literal fixnum.
 *
 * Cache value so that when the same Fixnum Operand is copy-propagated across
 * multiple instructions, the same RubyFixnum object is created.  In addition,
 * the same constant across loops should be the same object.
 *
 * So, in this example, the output should be false, true, true
 *
 * <pre>
 *   n = 0
 *   olda = nil
 *   while (n < 3)
 *     a = 34853
 *     p a.equal?(olda)
 *     olda = a
 *     n += 1
 *   end
 * </pre>
 */
public class UnboxedFixnum extends ImmutableLiteral {
    final public long value;

    public UnboxedFixnum(long val) {
        super(OperandType.UNBOXED_FIXNUM);
        value = val;
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return context.runtime.newFixnum(value);
    }

    @Override
    public int hashCode() {
        return 47 * 7 + (int) (this.value ^ (this.value >>> 32));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof UnboxedFixnum && value == ((UnboxedFixnum) other).value;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.UnboxedFixnum(this);
    }

    public long getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "UnboxedFixnum:" + value;
    }
}
