package org.jruby.ir.operands;

import org.jruby.RubyBignum;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.runtime.ThreadContext;

import java.math.BigInteger;

/**
 * Represents a literal Bignum.
 *
 * We cache the value so that when the same Bignum Operand is copy-propagated
 * across multiple instructions, the same RubyBignum object is created.  In a
 * ddition, the same constant across loops should be the same object.
 *
 * So, in this example, the output should be false, true, true
 * <pre>
 *   n = 0
 *   olda = nil
 *   while (n < 3)
 *     a = 81402749386839761113321
 *     p a.equal?(olda)
 *     olda = a
 *     n += 1
 *   end
 * </pre>
 *
 */
public class Bignum extends ImmutableLiteral {
    final public BigInteger value;

    public Bignum(BigInteger value) {
        super(OperandType.BIGNUM);
        this.value = value;
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return RubyBignum.newBignum(context.runtime, value);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Bignum(this);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(value.toString());
    }

    @Override
    public String toString() {
        return "Bignum:" + value;
    }
}
