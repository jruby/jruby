package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.interpreter.FullInterpreterContext;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class BTrueInstr extends OneOperandBranchInstr implements FixedArityInstr {
    public BTrueInstr(Label jmpTarget, Operand v) {
        super(Operation.B_TRUE, jmpTarget, v);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BTrueInstr(ii.getRenamedLabel(getJumpTarget()), getArg1().cloneForInlining(ii));
    }

    @Override
    public Instr simplifyBranch(FullInterpreterContext fic) {
        if (getArg1().isTruthyImmediate()) {
            return new JumpInstr(getJumpTarget());
        } else if (getArg1().isFalseyImmediate()) {
            return NopInstr.NOP;
        } else {
            return super.simplifyBranch(fic);
        }
    }

    public static BTrueInstr decode(IRReaderDecoder d) {
        return new BTrueInstr(d.decodeLabel(), d.decodeOperand());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BTrueInstr(this);
    }

    @Override
    public int interpretAndGetNewIPC(ThreadContext context, DynamicScope currDynScope, StaticScope currScope, IRubyObject self, Object[] temp, int ipc) {
        Object value1 = getArg1().retrieve(context, self, currScope, currDynScope, temp);
        return ((IRubyObject)value1).isTrue() ? getJumpTarget().getTargetPC() : ipc;
    }
}
