package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
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
public class Fixnum extends ImmutableLiteral {
    final public long value;

    public Fixnum(long val) {
        super(OperandType.FIXNUM);
        value = val;
    }

    public Fixnum(BigInteger val) {
        this(val.longValue());
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
        return other instanceof Fixnum && value == ((Fixnum) other).value;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Fixnum(this);
    }

    public long getValue() {
        return value;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(value);
    }

    public static Fixnum decode(IRReaderDecoder d) {
        return new Fixnum(d.decodeLong());
    }

    @Override
    public String toString() {
        return "Fixnum:" + value;
    }
}
