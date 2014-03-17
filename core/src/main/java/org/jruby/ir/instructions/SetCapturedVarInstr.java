package org.jruby.ir.instructions;

import java.util.Map;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Interp;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.RubyMatchData;
import org.jruby.RubyRegexp;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class SetCapturedVarInstr extends Instr implements ResultInstr, FixedArityInstr {
    private Variable result;
    private Operand match2Result;
    private final String varName;

    public SetCapturedVarInstr(Variable result, Operand match2Result, String varName) {
        super(Operation.SET_CAPTURED_VAR);

        assert result != null: "SetCapturedVarInstr result is null";

        this.result = result;
        this.match2Result = match2Result;
        this.varName = varName;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { match2Result, new StringLiteral(varName) };
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        match2Result = match2Result.getSimplifiedOperand(valueMap, force);
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
    public String toString() {
        return result + " = set_captured_var(" + match2Result + ", '" + varName + "')";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new SetCapturedVarInstr(ii.getRenamedVariable(result), match2Result.cloneForInlining(ii), varName);
    }

    @Interp
    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject matchRes = (IRubyObject)match2Result.retrieve(context, self, currDynScope, temp);
        Object val;
        if (matchRes.isNil()) {
            val = context.nil;
        } else {
            IRubyObject backref = context.getBackRef();
            int n = ((RubyMatchData)backref).getNameToBackrefNumber(varName);
            val = RubyRegexp.nth_match(n, backref);
        }

        return val;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.SetCapturedVarInstr(this);
    }
}
