package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.List;

/**
 * Represents the script's __FILE__. Isolated as its own operand because we need to be able to replace it when loading
 * persisted IR from a location different than original script.
 */
public class Filename extends Operand {
    public Filename() {
        super();
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.FILENAME;
    }

    @Override
    public boolean hasKnownValue() {
        return false;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        // we only do base encoding because filename must be provided while deserializing (#3109)
        e.encode(getOperandType().getCoded());
    }

    public static Filename decode(IRReaderDecoder d) {
        return new Filename();
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Filename(this);
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        return IRRuntimeHelpers.getFileNameStringFromScope(context, currScope);
    }

    @Override
    public Operand cloneForInlining(CloneInfo ii) {
        return this;
    }

    @Override
    public void addUsedVariables(List<Variable> l) {
        /* Do nothing */
    }
}
