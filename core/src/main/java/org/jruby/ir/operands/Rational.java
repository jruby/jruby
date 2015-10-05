package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.runtime.ThreadContext;

/**
 * Literal Rational number.
 */
public class Rational extends ImmutableLiteral {
    private final long numerator;
    private final long denominator;

    public Rational(long numerator, long denominator) {
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
        return context.runtime.newRational(numerator, denominator);
    }

    @Override
    public String toString() {
        return "Rational:" + numerator + "/1";
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Rational(this);
    }

    public long getNumerator() {
        return numerator;
    }

    public long getDenominator() {
        return denominator;
    }
}
