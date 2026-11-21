package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.ThreadContext;

import java.math.BigInteger;

/**
 * Literal Rational number, reduced at construction with a positive denominator.
 */
public class Rational extends ImmutableLiteral {
    private final BigInteger numerator;
    private final BigInteger denominator;

    public Rational(ImmutableLiteral numerator, ImmutableLiteral denominator) {
        this(integerValue(numerator), integerValue(denominator));
    }

    public Rational(BigInteger num, BigInteger den) {
        super();
        if (den.signum() == 0) throw new IllegalArgumentException("Rational literal denominator must not be zero");

        BigInteger gcd = num.gcd(den);
        if (den.signum() < 0) gcd = gcd.negate();
        this.numerator = num.divide(gcd);
        this.denominator = den.divide(gcd);
    }

    private static BigInteger integerValue(ImmutableLiteral value) {
        if (value instanceof Fixnum f) return BigInteger.valueOf(f.getValue());
        if (value instanceof Bignum b) return b.value;
        throw new IllegalArgumentException("Expected an integer literal: " + value);
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.RATIONAL;
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return IRRuntimeHelpers.newRationalRaw(context, numerator, denominator);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        // Preserve the integer-operand format used by persisted Rational literals.
        new Bignum(numerator).encode(e);
        new Bignum(denominator).encode(e);
    }

    public static Rational decode(IRReaderDecoder d) {
        return new Rational((ImmutableLiteral) d.decodeOperand(), (ImmutableLiteral) d.decodeOperand());
    }

    @Override
    public String toString() {
        return "Rational:" + numerator + "/" + denominator;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Rational(this);
    }

    public BigInteger getNumerator() {
        return numerator;
    }

    public BigInteger getDenominator() {
        return denominator;
    }

    @Override
    public boolean isTruthyImmediate() {
        return true;
    }
}
