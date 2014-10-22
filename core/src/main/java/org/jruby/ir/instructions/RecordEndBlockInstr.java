package org.jruby.ir.instructions;

import org.jruby.ir.*;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.transformations.inlining.CloneInfo;

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

    public IRClosure getEndBlockClosure() {
        return endBlockClosure.getClosure();
    }

    @Override
    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    @Override
    public String toString() {
        return getOperation().toString() + "(" + endBlockClosure.getClosure().getName() + ")";
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

    public void interpret() {
        declaringScope.getTopLevelScope().recordEndBlock(endBlockClosure.getClosure());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.RecordEndBlockInstr(this);
    }
}
