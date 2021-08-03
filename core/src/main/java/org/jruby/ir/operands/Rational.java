package org.jruby.ir.operands;

import org.jruby.RubyRational;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Literal Rational number.
 */
public class Rational extends ImmutableLiteral {
    private final ImmutableLiteral numerator;
    private final ImmutableLiteral denominator;

    public Rational(ImmutableLiteral numerator, ImmutableLiteral denominator) {
        super();

        this.numerator = numerator;
        this.denominator = denominator;
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.RATIONAL;
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return RubyRational.newRationalCanonicalize(context,
                (IRubyObject) numerator.cachedObject(context), (IRubyObject) denominator.cachedObject(context));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        numerator.encode(e);
        denominator.encode(e);
    }

    public static Rational decode(IRReaderDecoder d) {
        return new Rational((ImmutableLiteral) d.decodeOperand(), (ImmutableLiteral) d.decodeOperand());
    }

    @Override
    public String toString() {
        return "Rational:" + numerator + "/1";
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Rational(this);
    }

    public ImmutableLiteral getNumerator() {
        return numerator;
    }

    public ImmutableLiteral getDenominator() {
        return denominator;
    }

    @Override
    public boolean isTruthyImmediate() {
        return true;
    }
}
