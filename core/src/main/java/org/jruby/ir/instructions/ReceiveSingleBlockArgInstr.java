package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ReceiveSingleBlockArgInstr extends ReceiveArgBase implements FixedArityInstr {
    public ReceiveSingleBlockArgInstr(Variable result) {
        super(Operation.RECV_SINGLE_BLOCK_ARG, result, -1);
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {};
    }

    @Override
    public Instr clone(CloneInfo info) {
        return this;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
    }

    @Override
    public Operand[] getOperands() {
        return Operand.EMPTY_ARRAY;
    }

    @Override
    public void setOperand(int i, Operand operand) {
    }

    public static ReceiveSingleBlockArgInstr decode(IRReaderDecoder d) {
        return new ReceiveSingleBlockArgInstr(d.decodeVariable());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveSingleBlockArgInstr(this);
    }

    @Override
    public IRubyObject receiveArg(ThreadContext context, IRubyObject[] args,  boolean acceptsKeywordArguments) {
        return IRRuntimeHelpers.receiveSingleBlockArg(context, args);
    }
}
