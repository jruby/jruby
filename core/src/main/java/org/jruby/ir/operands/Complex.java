package org.jruby.ir.operands;

import org.jruby.RubyComplex;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Represents a Complex literal.
 */
public class Complex extends ImmutableLiteral {
    private final ImmutableLiteral number;

    public Complex(ImmutableLiteral number) {
        super(OperandType.COMPLEX);

        this.number = number;
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return RubyComplex.newComplexRaw(context.runtime,
                context.runtime.newFixnum(0), (IRubyObject) number.cachedObject(context));
    }

    @Override
    public String toString() {
        return number + "i";
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

    public Operand getNumber() {
        return number;
    }
}
