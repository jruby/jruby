package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.BooleanLiteral;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class BNEInstr extends BranchInstr {
    public static BranchInstr create(Operand v1, Operand v2, Label jmpTarget) {
        if (v2 instanceof BooleanLiteral) {
            return ((BooleanLiteral) v2).isFalse() ? new BTrueInstr(v1, jmpTarget) : new BFalseInstr(v1, jmpTarget);
        }
        return new BNEInstr(v1, v2, jmpTarget);
    }

    public BNEInstr(Operand v1, Operand v2, Label jmpTarget) {
        super(Operation.BNE, v1, v2, jmpTarget);
    }

    @Override
    public Instr cloneForInlinedScope(InlinerInfo ii) {
        return new BNEInstr(getArg1().cloneForInlining(ii),
                getArg2().cloneForInlining(ii), ii.getRenamedLabel(getJumpTarget()));
    }

    @Override
    public Instr cloneForBlockCloning(InlinerInfo ii) {
        return new BNEInstr(getArg1().cloneForInlining(ii), getArg2().cloneForInlining(ii), getJumpTarget());
    }

    @Override
    public int interpretAndGetNewIPC(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, int ipc) {
        Object value1 = getArg1().retrieve(context, self, currDynScope, temp);
        Object value2 = getArg2().retrieve(context, self, currDynScope, temp);
        boolean eql = getArg2() == context.getRuntime().getIRManager().getNil() || getArg2() == UndefinedValue.UNDEFINED ?
                value1 == value2 : ((IRubyObject) value1).op_equal(context, (IRubyObject)value2).isTrue();
        return !eql ? getJumpTarget().getTargetPC() : ipc;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BNEInstr(this);
    }
}
