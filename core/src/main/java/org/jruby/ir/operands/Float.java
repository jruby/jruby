package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.runtime.ThreadContext;

public class Float extends ImmutableLiteral {
    final public double value;

    public Float(double value) {
        super();

        this.value = value;
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.FLOAT;
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

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(value);
    }

    public static Float decode(IRReaderDecoder d) {
        return new Float(d.decodeDouble());
    }

    public double getValue() {
        return value;
    }

    @Override
    public boolean isTruthyImmediate() {
        return true;
    }
}
