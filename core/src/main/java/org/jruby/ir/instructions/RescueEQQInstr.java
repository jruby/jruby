package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// This instruction is similar to EQQInstr, except it also verifies that
// the type to EQQ with is actually a class or a module since rescue clauses
// have this requirement unlike case statements.
//
// If v2 is an array, compare v1 with every element of v2 and stop on first match!
public class RescueEQQInstr extends TwoOperandResultBaseInstr implements FixedArityInstr {
    public RescueEQQInstr(Variable result, Operand v1, Operand v2) {
        super(Operation.RESCUE_EQQ, result, v1, v2);

        assert result != null: "RescueEQQInstr result is null";
    }

    public Operand getArg1() {
        return getOperand1();
    }

    public Operand getArg2() {
        return getOperand2();
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new RescueEQQInstr(ii.getRenamedVariable(result),
                getArg1().cloneForInlining(ii), getArg2().cloneForInlining(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getArg1());
        e.encode(getArg2());
    }

    public static RescueEQQInstr decode(IRReaderDecoder d) {
        return new RescueEQQInstr(d.decodeVariable(), d.decodeOperand(), d.decodeOperand());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject excType = (IRubyObject) getArg1().retrieve(context, self, currScope, currDynScope, temp);
        Object excObj = getArg2().retrieve(context, self, currScope, currDynScope, temp);

        return IRRuntimeHelpers.isExceptionHandled(context, excType, excObj);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.RescueEQQInstr(this);
    }
}
