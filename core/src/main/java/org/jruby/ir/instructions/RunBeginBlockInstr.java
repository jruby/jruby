package org.jruby.ir.instructions;

import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RunBeginBlockInstr extends OneOperandInstr implements FixedArityInstr {
    private final IRScope declaringScope;

    public RunBeginBlockInstr(IRScope declaringScope, WrappedIRClosure endBlockClosure) {
        super(Operation.RECORD_END_BLOCK, endBlockClosure);

        this.declaringScope = declaringScope;
    }

    public IRScope getDeclaringScope() {
        return declaringScope;
    }

    public WrappedIRClosure getBeginBlockClosure() {
        return (WrappedIRClosure) getOperand1();
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        scope.getFlags().add(IRFlags.HAS_END_BLOCKS);
        return true;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        // SSS FIXME: Correct in all situations??
        return new RunBeginBlockInstr(declaringScope, (WrappedIRClosure) getBeginBlockClosure().cloneForInlining(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getDeclaringScope());
        e.encode(getBeginBlockClosure());
    }

    public static RunBeginBlockInstr decode(IRReaderDecoder d) {
        return new RunBeginBlockInstr(d.decodeScope(), (WrappedIRClosure) d.decodeOperand());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Block blk = (Block) getBeginBlockClosure().retrieve(context, self, currScope, context.getCurrentScope(), temp);
        blk.yield(context, null);
        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.RunBeginBlock(this);
    }
}
