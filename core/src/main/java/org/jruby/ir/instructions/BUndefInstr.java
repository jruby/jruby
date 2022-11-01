package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.interpreter.FullInterpreterContext;
import org.jruby.ir.operands.ImmutableLiteral;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class BUndefInstr extends OneOperandBranchInstr  implements FixedArityInstr {
    public BUndefInstr(Label jmpTarget, Operand v) {
        super(Operation.B_UNDEF, jmpTarget, v);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BUndefInstr(ii.getRenamedLabel(getJumpTarget()), getArg1().cloneForInlining(ii));
    }

    @Override
    public Instr simplifyBranch(FullInterpreterContext fic) {
        if (getArg1().equals(UndefinedValue.UNDEFINED)) {
            return new JumpInstr(getJumpTarget());
        } else if (getArg1() instanceof ImmutableLiteral) {
            return NopInstr.NOP;
        } else {
            return super.simplifyBranch(fic);
        }
    }

    public static BUndefInstr decode(IRReaderDecoder d) {
        return new BUndefInstr(d.decodeLabel(), d.decodeOperand());
    }

    @Override
    public int interpretAndGetNewIPC(ThreadContext context, DynamicScope currDynScope, StaticScope currScope, IRubyObject self, Object[] temp, int ipc) {
        Object value1 = getArg1().retrieve(context, self, currScope, currDynScope, temp);
        return value1 == UndefinedValue.UNDEFINED ? getJumpTarget().getTargetPC() : ipc;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BUndefInstr(this);
    }
}
