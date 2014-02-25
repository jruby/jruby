package org.jruby.ir.instructions;

import org.jruby.ir.IRClosure;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.ScopeModule;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class RecordEndBlockInstr extends Instr implements FixedArityInstr {
    private final IRScope declaringScope;
    private final IRClosure endBlockClosure;

    public RecordEndBlockInstr(IRScope declaringScope, IRClosure endBlockClosure) {
        super(Operation.RECORD_END_BLOCK);

        this.declaringScope = declaringScope;
        this.endBlockClosure = endBlockClosure;
    }

    public IRScope getDeclaringScope() {
        return declaringScope;
    }

    public IRClosure getEndBlockClosure() {
        return endBlockClosure;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { new ScopeModule(declaringScope), new ScopeModule(endBlockClosure) };
    }

    @Override
    public String toString() {
        return getOperation().toString() + "(" + endBlockClosure.getName() + ")";
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        scope.getFlags().add(IRFlags.HAS_END_BLOCKS);
        return true;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        // SSS FIXME: Correct in all situations??
        return new RecordEndBlockInstr(declaringScope, endBlockClosure);
    }

    public void interpret() {
        declaringScope.getTopLevelScope().recordEndBlock(endBlockClosure);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.RecordEndBlockInstr(this);
    }
}
