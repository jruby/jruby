package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Boolean;
import org.jruby.ir.operands.*;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class BEQInstr extends TwoOperandBranchInstr implements FixedArityInstr {
    public static Instr create(Operand v1, Operand v2, Label jmpTarget) {
        if (v2 instanceof Boolean) {
            Boolean lhs = (Boolean) v2;

            if (lhs.isTrue()) {
                if (v1 instanceof Boolean) {
                    if (((Boolean) v1).isTrue()) { // true == true -> just jump
                        return new JumpInstr(jmpTarget);
                    } else {                       // false == true (this will never jump)
                        return NopInstr.NOP;
                    }
                } else {
                    return new BTrueInstr(jmpTarget, v1);
                }
            } else if (lhs.isFalse()) {
                if (v1 instanceof Boolean) {
                    if (((Boolean) v1).isFalse()) { // false == false -> just jump
                        return new JumpInstr(jmpTarget);
                    } else {                        // true == false (this will never jump)
                        return NopInstr.NOP;
                    }
                } else {
                    return new BFalseInstr(jmpTarget, v1);
                }
            }
        } else if (v2 instanceof Nil) {
            if (v1 instanceof Nil) { // nil == nil -> just jump
                return new JumpInstr(jmpTarget);
            } else {
                return new BNilInstr(jmpTarget, v1);
            }
        }
        if (v2 == UndefinedValue.UNDEFINED) {
            if (v1 == UndefinedValue.UNDEFINED) {
                return new JumpInstr(jmpTarget);
            } else {
                return new BUndefInstr(jmpTarget, v1);
            }
        }

        return new BEQInstr(jmpTarget, v1, v2);
    }

    protected BEQInstr(Label jumpTarget, Operand v1, Operand v2) {
        super(Operation.BEQ, jumpTarget, v1, v2);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BEQInstr(ii.getRenamedLabel(getJumpTarget()), getArg1().cloneForInlining(ii), getArg2().cloneForInlining(ii));
    }


    public static BEQInstr decode(IRReaderDecoder d) {
        return new BEQInstr(d.decodeLabel(), d.decodeOperand(), d.decodeOperand());
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
