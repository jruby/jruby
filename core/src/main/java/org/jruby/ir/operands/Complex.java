package org.jruby.ir.operands;

import org.jruby.RubyComplex;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Represents a Complex literal.
 */
public class Complex extends ImmutableLiteral {
    private final ImmutableLiteral number;

    public Complex(ImmutableLiteral number) {
        super();

        this.number = number;
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.COMPLEX;
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return IRRuntimeHelpers.newComplexRaw(context,
                (IRubyObject) number.cachedObject(context));
    }

    @Override
    public String toString() {
        return "Complex:" + number + "i";
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Complex(this);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(number);
    }

    public static Complex decode(IRReaderDecoder d) {
        return new Complex((ImmutableLiteral) d.decodeOperand());
    }

    public Operand getNumber() {
        return number;
    }

    @Override
    public boolean isTruthyImmediate() {
        return true;
    }
}
