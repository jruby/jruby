package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.JIT;
import org.jruby.runtime.ThreadContext;

/**
 * Used by JIT to represent a JVM null.
 */
@JIT
public class Null extends ImmutableLiteral {
    public static final Null INSTANCE = new Null();

    Null() {
        super(OperandType.NULL);
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return null;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Null(this);
    }

    @Override
    public String toString() {
        return "Null";
    }
}
