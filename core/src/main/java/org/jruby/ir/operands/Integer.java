package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.runtime.ThreadContext;

import java.math.BigInteger;

// FIXME: Can be cached like Fixnum if we have many of these.
public class Integer extends ImmutableLiteral {
    final public int value;

    public Integer(int val) {
        super();
        value = val;
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.INTEGER;
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return value;
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
        visitor.Integer(this);
    }

    public long getValue() {
        return value;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(value);
    }

    public static Integer decode(IRReaderDecoder d) {
        return new Integer(d.decodeInt());
    }

    @Override
    public String toString() {
        return "Integer:" + value;
    }

    @Override
    public boolean isTruthyImmediate() {
        return true;
    }
}
