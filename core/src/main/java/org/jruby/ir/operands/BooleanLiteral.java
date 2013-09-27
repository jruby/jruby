package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.runtime.ThreadContext;

public class BooleanLiteral extends ImmutableLiteral {
    private final boolean truthy;

    public BooleanLiteral(boolean truthy) {
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
    public String toString() {
        return isTrue() ? "true" : "false";
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BooleanLiteral(this);
    }
}
