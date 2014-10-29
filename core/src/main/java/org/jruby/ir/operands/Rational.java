package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.runtime.ThreadContext;

/**
 * Literal Rational number.
 */
public class Rational extends ImmutableLiteral {
    private long numerator;

    public Rational(long numerator) {
        super(OperandType.RATIONAL);

        this.numerator = numerator;
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return context.runtime.newRational(numerator, 1);
    }

    @Override
    public String toString() {
        return "Rational:" + numerator + "/1";
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Rational(this);
    }

    public double getNumerator() {
        return numerator;
    }
}
