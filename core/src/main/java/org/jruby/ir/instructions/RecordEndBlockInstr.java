package org.jruby.ir.instructions;

import org.jruby.ir.*;
import org.jruby.ir.interpreter.BeginEndInterpreterContext;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RecordEndBlockInstr extends Instr implements FixedArityInstr {
    private final IRScope declaringScope;
    private final WrappedIRClosure endBlockClosure;

    public RecordEndBlockInstr(IRScope declaringScope, WrappedIRClosure endBlockClosure) {
        super(Operation.RECORD_END_BLOCK);

        this.declaringScope = declaringScope;
        this.endBlockClosure = endBlockClosure;
    }

    public IRScope getDeclaringScope() {
        return declaringScope;
    }

    public WrappedIRClosure getEndBlockClosure() {
        return endBlockClosure;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { endBlockClosure };
    }

    @Override
    public String toString() {
        return getOperation().toString() + "(" + endBlockClosure + ")";
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        scope.getFlags().add(IRFlags.HAS_END_BLOCKS);
        return true;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        // SSS FIXME: Correct in all situations??
        return new RecordEndBlockInstr(declaringScope, (WrappedIRClosure) endBlockClosure.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Block blk = (Block) endBlockClosure.retrieve(context, self, currScope, context.getCurrentScope(), temp);
        context.runtime.pushExitBlock(context.runtime.newProc(Block.Type.LAMBDA, blk));
        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.RecordEndBlockInstr(this);
    }
}
