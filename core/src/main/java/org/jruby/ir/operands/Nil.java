package org.jruby.ir.operands;

// Records the nil object

import org.jruby.ir.IRVisitor;
import org.jruby.runtime.ThreadContext;

/**
 * Represents nil.
 *
 * Note: We used to protect the constructor, but since manager is the new
 * way I got lazy and removed protected.
 */
public class Nil extends ImmutableLiteral {
    public Nil() {
        super(OperandType.NIL);
    }

    // For UnexecutableNil
    protected Nil(OperandType type) {
        super(type);
    }

    @Override
    public String toString() {
        return "nil";
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return context.nil;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Nil(this);
    }
}
