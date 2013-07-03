package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.BooleanLiteral;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Nil;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class BEQInstr extends BranchInstr {
    public static BranchInstr create(Operand v1, Operand v2, Label jmpTarget) {
        if (v2 instanceof BooleanLiteral) {
            return ((BooleanLiteral) v2).isTrue() ? new BTrueInstr(v1, jmpTarget) : new BFalseInstr(v1, jmpTarget);
        }
        if (v2 instanceof Nil) return new BNilInstr(v1, jmpTarget);
        if (v2 == UndefinedValue.UNDEFINED) return new BUndefInstr(v1, jmpTarget);
        return new BEQInstr(v1, v2, jmpTarget);
    }

    protected BEQInstr(Operand v1, Operand v2, Label jmpTarget) {
        super(Operation.BEQ, v1, v2, jmpTarget);
    }

    @Override
    public Instr cloneForInlinedScope(InlinerInfo ii) {
        return new BEQInstr(getArg1().cloneForInlining(ii), getArg2().cloneForInlining(ii), ii.getRenamedLabel(getJumpTarget()));
    }

    @Override
    public Instr cloneForBlockCloning(InlinerInfo ii) {
        return new BEQInstr(getArg1().cloneForInlining(ii), getArg2().cloneForInlining(ii), getJumpTarget());
    }

    @Override
    public int interpretAndGetNewIPC(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, int ipc) {
        Object value1 = getArg1().retrieve(context, self, currDynScope, temp);
        Object value2 = getArg2().retrieve(context, self, currDynScope, temp);
        return ((IRubyObject) value1).op_equal(context, (IRubyObject)value2).isTrue() ? getJumpTarget().getTargetPC() : ipc;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BEQInstr(this);
    }
}
