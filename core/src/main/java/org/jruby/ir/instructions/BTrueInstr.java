package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class BTrueInstr extends BranchInstr {
    protected BTrueInstr(Operand v, Label jmpTarget) {
        super(Operation.B_TRUE, v, null, jmpTarget);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new BTrueInstr(getArg1().cloneForInlining(ii), ii.getRenamedLabel(getJumpTarget()));
    }

    public void visit(IRVisitor visitor) {
        visitor.BTrueInstr(this);
    }

    @Override
    public int interpretAndGetNewIPC(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, int ipc) {
        Object value1 = getArg1().retrieve(context, self, currDynScope, temp);
        return ((IRubyObject)value1).isTrue() ? getJumpTarget().getTargetPC() : ipc;
    }
}
