package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.runtime.ThreadContext;

public class BooleanLiteral extends ImmutableLiteral {
    private final boolean truthy;

    public static final BooleanLiteral TRUE = new BooleanLiteral(true);
    public static final BooleanLiteral FALSE = new BooleanLiteral(false);

    public BooleanLiteral(boolean truthy) {
        super(OperandType.BOOLEAN_LITERAL);

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
        return other instanceof BooleanLiteral && truthy == ((BooleanLiteral) other).truthy;
    }

    @Override
    public int hashCode() {
        return 41 * 7 + (this.truthy ? 1 : 0);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BooleanLiteral(this);
    }

    @Override
    public String toString() {
        return isTrue() ? "true" : "false";
    }
}
