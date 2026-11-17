package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class BIntInstr extends TwoOperandBranchInstr implements FixedArityInstr {
    public enum Op {
        LT("<"), GT(">"), LTE("<="), GTE(">="), EQ("=="), NEQ("!=");

        private final String label;

        Op(String label) {
            this.label = label;
        }

        public String toString() {
            return label;
        }

        public static Op fromOrdinal(int value) {
            return value < 0 || value >= values().length ? null : values()[value];
        }
    }

    private final Op op;

    public BIntInstr(Label jumpTarget, Op op, Operand v1, Operand v2) {
        super(Operation.B_INT, jumpTarget, v1, v2);

        this.op = op;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BIntInstr(ii.getRenamedLabel(getJumpTarget()), op, getArg1().cloneForInlining(ii), getArg2().cloneForInlining(ii));
    }

    public static BIntInstr decode(IRReaderDecoder d) {
        Label label = d.decodeLabel();
        Operand arg1 = d.decodeOperand();
        Operand arg2 = d.decodeOperand();

        return new BIntInstr(label, Op.fromOrdinal(d.decodeInt()), arg1, arg2);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(op.ordinal());
    }

    @Override
    public int interpretAndGetNewIPC(ThreadContext context, DynamicScope currDynScope, StaticScope currScope, IRubyObject self, Object[] temp, int ipc) {
        int value1 = (int) getArg1().retrieve(context, self, currScope, currDynScope, temp);
        int value2 = (int) getArg2().retrieve(context, self, currScope, currDynScope, temp);

        boolean test;

        switch(op) {
            case LT:
                test = value1 < value2; break;
            case GT:
                test = value1 > value2; break;
            case LTE:
                test = value1 <= value2; break;
            case GTE:
                test = value1 >= value2; break;
            case EQ:
                test = value1 == value2; break;
            case NEQ:
                test = value1 != value2; break;
            default:
                throw new RuntimeException("BIntInstr has unknown op");
        }

        return test ? getJumpTarget().getTargetPC() : ipc;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BIntInstr(this);
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { op.toString() };
    }

    public Op getOp() {
        return op;
    }
}
