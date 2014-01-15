package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

// This instruction is similar to EQQInstr, except it also verifies that
// the type to EQQ with is actually a class or a module since rescue clauses
// have this requirement unlike case statements.
//
// If v2 is an array, compare v1 with every element of v2 and stop on first match!
public class RescueEQQInstr extends Instr implements ResultInstr, FixedArityInstr {
    private Operand arg1;
    private Operand arg2;
    private Variable result;

    public RescueEQQInstr(Variable result, Operand v1, Operand v2) {
        super(Operation.RESCUE_EQQ);

        assert result != null: "RescueEQQInstr result is null";

        this.arg1 = v1;
        this.arg2 = v2;
        this.result = result;
    }

    public Operand getArg1() {
        return arg1;
    }

    public Operand getArg2() {
        return arg2;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[]{arg1, arg2};
    }

    @Override
    public Variable getResult() {
        return result;
    }

    @Override
    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        arg1 = arg1.getSimplifiedOperand(valueMap, force);
        arg2 = arg2.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + arg1 + ", " + arg2 + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new RescueEQQInstr(ii.getRenamedVariable(result),
                arg1.cloneForInlining(ii), arg2.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        IRubyObject excType = (IRubyObject) arg1.retrieve(context, self, currDynScope, temp);
        Object excObj = arg2.retrieve(context, self, currDynScope, temp);

        return IRRuntimeHelpers.isExceptionHandled(context, excType, excObj);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.RescueEQQInstr(this);
    }
}
