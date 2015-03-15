package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.runtime.ThreadContext;

public class UnboxedBoolean extends ImmutableLiteral {
    private final boolean truthy;

    public static final UnboxedBoolean TRUE = new UnboxedBoolean(true);
    public static final UnboxedBoolean FALSE = new UnboxedBoolean(false);

    public UnboxedBoolean(boolean truthy) {
        super(OperandType.UNBOXED_BOOLEAN);

        this.truthy = truthy;
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return context.runtime.newBoolean(isTrue());
    }

    public boolean isTrue()  {
        return truthy;
    }

    public boolean isFalse() {
        return !truthy;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof UnboxedBoolean && truthy == ((UnboxedBoolean) other).truthy;
    }

    @Override
    public int hashCode() {
        return 41 * 7 + (this.truthy ? 1 : 0);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.UnboxedBoolean(this);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(truthy);
    }

    public static UnboxedBoolean decode(IRReaderDecoder d) {
        return d.decodeBoolean() ? TRUE : FALSE;
    }

    @Override
    public String toString() {
        return isTrue() ? "true" : "false";
    }
}
