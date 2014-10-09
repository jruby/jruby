package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Boolean;
import org.jruby.ir.operands.*;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class BEQInstr extends TwoOperandBranchInstr implements FixedArityInstr {
    public static BranchInstr create(Operand v1, Operand v2, Label jmpTarget) {
        if (v2 instanceof Boolean) {
            return ((Boolean) v2).isTrue() ? new BTrueInstr(v1, jmpTarget) : new BFalseInstr(v1, jmpTarget);
        }
        if (v2 instanceof Nil) return new BNilInstr(v1, jmpTarget);
        if (v2 == UndefinedValue.UNDEFINED) return new BUndefInstr(v1, jmpTarget);
        return new BEQInstr(v1, v2, jmpTarget);
    }

    protected BEQInstr(Operand v1, Operand v2, Label jmpTarget) {
        super(Operation.BEQ, v1, v2, jmpTarget);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BEQInstr(getArg1().cloneForInlining(ii), getArg2().cloneForInlining(ii), ii.getRenamedLabel(getJumpTarget()));
    }

    @Override
    public int interpretAndGetNewIPC(ThreadContext context, DynamicScope currDynScope, StaticScope currScope, IRubyObject self, Object[] temp, int ipc) {
        Object value1 = getArg1().retrieve(context, self, currScope, currDynScope, temp);
        Object value2 = getArg2().retrieve(context, self, currScope, currDynScope, temp);
        return ((IRubyObject) value1).op_equal(context, (IRubyObject)value2).isTrue() ? getJumpTarget().getTargetPC() : ipc;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BEQInstr(this);
    }
}
