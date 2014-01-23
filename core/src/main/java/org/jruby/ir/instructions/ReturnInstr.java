package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class ReturnInstr extends ReturnBase implements FixedArityInstr {
    public ReturnInstr(Operand returnValue) {
        super(Operation.RETURN, returnValue);
    }

    @Override
    public String toString() {
        return getOperation() + "(" + returnValue + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        switch (ii.getCloneMode()) {
            case ENSURE_BLOCK_CLONE:
            case NORMAL_CLONE:
                return new ReturnInstr(returnValue.cloneForInlining(ii));
            case CLOSURE_INLINE:
                return new CopyInstr(ii.getYieldResult(), returnValue.cloneForInlining(ii));
            case METHOD_INLINE:
                Variable v = ii.getCallResultVariable();
                return v == null ? null : new CopyInstr(v, returnValue.cloneForInlining(ii));
            default:
                // Should not get here
                return super.cloneForInlining(ii);
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReturnInstr(this);
    }
}
