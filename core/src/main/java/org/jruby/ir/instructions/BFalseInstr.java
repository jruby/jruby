package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class BFalseInstr extends OneOperandBranchInstr implements FixedArityInstr {
    // Public only for persistence reloading
    public BFalseInstr(Operation op, Operand v, Label jmpTarget) {
        super(op, new Operand[] {jmpTarget, v});
    }

    public BFalseInstr(Operand v, Label jmpTarget) {
        super(Operation.B_FALSE, new Operand[] {jmpTarget, v});
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BFalseInstr(getOperation(), getArg1().cloneForInlining(ii), ii.getRenamedLabel(getJumpTarget()));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BFalseInstr(this);
    }

    @Override
    public int interpretAndGetNewIPC(ThreadContext context, DynamicScope currDynScope, StaticScope currScope, IRubyObject self, Object[] temp, int ipc) {
        Object value1 = getArg1().retrieve(context, self, currScope, currDynScope, temp);
        return !((IRubyObject)value1).isTrue() ? getJumpTarget().getTargetPC() : ipc;
    }
}
