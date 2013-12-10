package org.jruby.ir.instructions;

import org.jruby.ir.IRClosure;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class RecordEndBlockInstr extends Instr {
    private IRScope declaringScope;
    private IRClosure endBlockClosure;

    public RecordEndBlockInstr(IRScope declaringScope, IRClosure endBlockClosure) {
        super(Operation.RECORD_END_BLOCK);

        this.declaringScope = declaringScope;
        this.endBlockClosure = endBlockClosure;
    }
    
    public IRClosure getEndBlockClosure() {
        return endBlockClosure;
    }

    @Override
    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
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
