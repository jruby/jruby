package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.IRMethod;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class ReturnInstr extends ReturnBase {
    public ReturnInstr(Operand returnValue) {
        super(Operation.RETURN, returnValue);
    }

    @Override
    public String toString() {
        return getOperation() + "(" + returnValue + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new ReturnInstr(returnValue.cloneForInlining(ii));
    }

    @Override
    public Instr cloneForInlinedScope(InlinerInfo ii) {
        Variable v = ii.getCallResultVariable();
        return v == null ? null : new CopyInstr(v, returnValue.cloneForInlining(ii));
    }

    @Override
    public Instr cloneForInlinedClosure(InlinerInfo ii) {
        return new CopyInstr(ii.getYieldResult(), returnValue.cloneForInlining(ii));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReturnInstr(this);
    }
}
