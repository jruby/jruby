package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Interp;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class SetCapturedVarInstr extends ResultBaseInstr implements FixedArityInstr {
    private final String varName;

    public SetCapturedVarInstr(Variable result, Operand match2Result, String varName) {
        super(Operation.SET_CAPTURED_VAR, result, new Operand[] { match2Result });

        assert result != null: "SetCapturedVarInstr result is null";

        this.varName = varName;
    }

    public Operand getMatch2Result() {
        return operands[0];
    }

    public String getVarName() {
        return varName;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "name: " + varName };
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new SetCapturedVarInstr(ii.getRenamedVariable(result), getMatch2Result().cloneForInlining(ii), varName);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getMatch2Result());
        e.encode(getVarName());
    }

    @Interp
    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject matchRes = (IRubyObject) getMatch2Result().retrieve(context, self, currScope, currDynScope, temp);
        return IRRuntimeHelpers.setCapturedVar(context, matchRes, varName);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.SetCapturedVarInstr(this);
    }
}
