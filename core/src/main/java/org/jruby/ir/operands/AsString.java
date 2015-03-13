package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.List;
import java.util.Map;

public class AsString extends Operand {
    final private Operand source;

    public AsString(Operand source) {
        super(OperandType.AS_STRING);

        this.source = source == null ? StringLiteral.EMPTY_STRING : source;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        return ((IRubyObject) source.retrieve(context, self, currScope, currDynScope, temp)).asString();
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap, boolean force) {
        Operand newSource = source.getSimplifiedOperand(valueMap, force);
        return newSource == source ? this : new AsString(newSource);
    }

    @Override
    public Operand cloneForInlining(CloneInfo ii) {
        return new AsString(source.cloneForInlining(ii));
    }

    @Override
    public void addUsedVariables(List<Variable> l) {
        source.addUsedVariables(l);
    }

    public Operand getSource() {
        return source;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.AsString(this);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getSource());
    }

    @Override
    public String toString() {
        return "#{" + source + "}";
    }
}
