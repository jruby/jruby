package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.boxing.UnboxBooleanInstr;
import org.jruby.ir.operands.*;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class BFalseInstr extends OneOperandBranchInstr implements FixedArityInstr {
    // Public only for persistence reloading
    public BFalseInstr(Operation op, Operand v, Label jmpTarget) {
        super(op, v, jmpTarget);
    }

    public BFalseInstr(Operand v, Label jmpTarget) {
        super(Operation.B_FALSE, v, jmpTarget);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new BFalseInstr(getOperation(), getArg1().cloneForInlining(ii), ii.getRenamedLabel(getJumpTarget()));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BFalseInstr(this);
    }

    @Override
    public int interpretAndGetNewIPC(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, int ipc) {
        Object value1 = getArg1().retrieve(context, self, currDynScope, temp);
        return !((IRubyObject)value1).isTrue() ? getJumpTarget().getTargetPC() : ipc;
    }
}
