package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class ClosureReturnInstr extends ReturnBase {
    public ClosureReturnInstr(Operand rv) {
        super(Operation.CLOSURE_RETURN, rv);
    }

    @Override
    public Instr cloneForBlockCloning(InlinerInfo ii) {
        return new ClosureReturnInstr(returnValue.cloneForInlining(ii));
    }

    @Override
    public Instr cloneForInlinedClosure(InlinerInfo ii) {
        return new CopyInstr(ii.getYieldResult(), returnValue.cloneForInlining(ii));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ClosureReturnInstr(this);
    }
}
