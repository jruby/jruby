package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.runtime.ThreadContext;

public class Float extends ImmutableLiteral {
    final public double value;

    public Float(double value) {
        super(OperandType.FLOAT);

        this.value = value;
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
        return "Float:" + value;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Float(this);
    }

    public double getValue() {
        return value;
    }
}
