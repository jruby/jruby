package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.interpreter.FullInterpreterContext;
import org.jruby.ir.operands.Boolean;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class BNEInstr extends TwoOperandBranchInstr implements FixedArityInstr {
    public static BranchInstr create(Label jmpTarget, Operand v1, Operand v2) {
        if (v2 instanceof Boolean) {
            return ((Boolean) v2).isFalse() ? new BTrueInstr(jmpTarget, v1) : new BFalseInstr(jmpTarget, v1);
        }
        return new BNEInstr(jmpTarget, v1, v2);
    }

    public BNEInstr(Label jumpTarget, Operand v1, Operand v2) {
        super(Operation.BNE, jumpTarget, v1, v2);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BNEInstr(ii.getRenamedLabel(getJumpTarget()), getArg1().cloneForInlining(ii), getArg2().cloneForInlining(ii));
    }

    // FIXME: Add !op_equal logic here for various immutable literal types
    @Override
    public Instr simplifyBranch(FullInterpreterContext fic) {
        if (getArg1().equals(getArg2())) {
            return NopInstr.NOP;
        } else {
            return super.simplifyBranch(fic);
        }
    }

    public static BNEInstr decode(IRReaderDecoder d) {
        return new BNEInstr(d.decodeLabel(), d.decodeOperand(), d.decodeOperand());
    }

    @Override
    public int interpretAndGetNewIPC(ThreadContext context, DynamicScope currDynScope, StaticScope currScope, IRubyObject self, Object[] temp, int ipc) {
        Object value1 = getArg1().retrieve(context, self, currScope, currDynScope, temp);
        Object value2 = getArg2().retrieve(context, self, currScope, currDynScope, temp);
        boolean eql = getArg2() == context.getRuntime().getIRManager().getNil() || getArg2() == UndefinedValue.UNDEFINED ?
                value1 == value2 : ((IRubyObject) value1).op_equal(context, (IRubyObject)value2).isTrue();
        return !eql ? getJumpTarget().getTargetPC() : ipc;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BNEInstr(this);
    }
}
