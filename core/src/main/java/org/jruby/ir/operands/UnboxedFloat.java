package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.runtime.ThreadContext;

public class UnboxedFloat extends ImmutableLiteral {
    final public double value;

    public UnboxedFloat(double value) {
        super();

        this.value = value;
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.UNBOXED_FLOAT;
    }

    @Override
    public boolean hasKnownValue() {
        return true;
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return context.runtime.newFloat(value);
    }

    @Override
    public String toString() {
        return "UnboxedFloat:" + value;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.UnboxedFloat(this);
    }

    public double getValue() {
        return value;
    }
}
