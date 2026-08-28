package org.jruby.ir.operands;

import org.jruby.RubyRange;
import org.jruby.RubyRational;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Literal Range with literal endpoints.
 */
public class Range extends ImmutableLiteral {
    private final ImmutableLiteral begin;
    private final ImmutableLiteral end;
    private final boolean exclusive;

    public Range(ImmutableLiteral begin, ImmutableLiteral end, boolean exclusive) {
        super();

        this.begin = begin;
        this.end = end;
        this.exclusive = exclusive;
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.RANGE;
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        IRubyObject begin = (IRubyObject) this.begin.cachedObject(context);
        IRubyObject end = (IRubyObject) this.end.cachedObject(context);

        if (exclusive) {
            return RubyRange.newExclusiveRange(context, begin, end);
        } else {
            return RubyRange.newInclusiveRange(context, begin, end);
        }
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        begin.encode(e);
        end.encode(e);
        e.encode(exclusive);
    }

    public static Range decode(IRReaderDecoder d) {
        return new Range((ImmutableLiteral) d.decodeOperand(), (ImmutableLiteral) d.decodeOperand(), d.decodeBoolean());
    }

    @Override
    public String toString() {
        return "Range:" + begin + (exclusive ? "..." : "..") + end;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Range(this);
    }

    public ImmutableLiteral getBegin() {
        return begin;
    }

    public ImmutableLiteral getEnd() {
        return end;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    @Override
    public boolean isTruthyImmediate() {
        return true;
    }
}
