package org.jruby.ir.operands;

import org.jruby.RubyRational;
import org.jruby.ir.IRVisitor;
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
        return RubyRational.newRationalRaw(context.runtime,
                (IRubyObject) numerator.cachedObject(context), (IRubyObject) denominator.cachedObject(context));
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
}
