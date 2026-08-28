package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.util.StringSupport.EMPTY_STRING_ARRAY;

public class IntegerMathInstr extends TwoOperandResultBaseInstr {
    public enum Op {
        ADD, SUBTRACT, MULTIPLY, DIVIDE;

        public static Op fromOrdinal(int value) {
            return value < 0 || value >= values().length ? null : values()[value];
        }
    }

    private final Op op;

    public IntegerMathInstr(Op op, Variable result, Operand operand1, Operand operand2) {
        super(Operation.INT_MATH, result, operand1, operand2);

        this.op = op;
    }

    @Override
    public Instr clone(CloneInfo info) {
        return new IntegerMathInstr(op, getResult(), getOperand1(), getOperand2());
    }

    public static IntegerMathInstr decode(IRReaderDecoder d) {
        Variable result = d.decodeVariable();
        Operand arg1 = d.decodeOperand();
        Operand arg2 = d.decodeOperand();

        return new IntegerMathInstr(Op.fromOrdinal(d.decodeInt()), result, arg1, arg2);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(op.ordinal());
    }

    public String[] toStringNonOperandArgs() {
        return new String[] { "op:", op.name()};
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.IntegerMathInstr(this);
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        int value1 = (int) getOperand1().retrieve(context, self, currScope, currDynScope, temp);
        int value2 = (int) getOperand2().retrieve(context, self, currScope, currDynScope, temp);

        switch(op) {
            case ADD:
                return value1 + value2;
            case SUBTRACT:
                return value1 - value2;
            case MULTIPLY:
                return value1 * value2;
            case DIVIDE:
                return value1 / value2;
            default:
                throw new RuntimeException("IntegerMathInstr has unknown op");
        }
    }

    public Op getOp() {
        return op;
    }
}
