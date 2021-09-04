package org.jruby.ir.operands;

import org.jruby.RubyBoolean;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.runtime.ThreadContext;

public class Boolean extends ImmutableLiteral {
    private final boolean truthy;

    public Boolean(boolean truthy) {
        super();

        this.truthy = truthy;
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.BOOLEAN;
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return RubyBoolean.newBoolean(context, isTrue());
    }

    public boolean isTrue()  {
        return truthy;
    }

    public boolean isFalse() {
        return !truthy;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Boolean && truthy == ((Boolean) other).truthy;
    }

    @Override
    public int hashCode() {
        return 41 * 7 + (this.truthy ? 1 : 0);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Boolean(this);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(isTrue());
    }

    public static Boolean decode(IRReaderDecoder d) {
        return d.decodeBoolean() ?
                d.getCurrentScope().getManager().getTrue() : d.getCurrentScope().getManager().getFalse();
    }

    @Override
    public String toString() {
        return isTrue() ? "true" : "false";
    }

    @Override
    public boolean isTruthyImmediate() {
        return truthy;
    }

    @Override
    public boolean isFalseyImmediate() {
        return !truthy;
    }
}
