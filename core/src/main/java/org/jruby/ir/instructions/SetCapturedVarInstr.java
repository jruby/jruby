package org.jruby.ir.instructions;

import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Interp;
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

public class SetCapturedVarInstr extends OneOperandResultBaseInstr implements FixedArityInstr {
    private final RubySymbol variableName;

    public SetCapturedVarInstr(Variable result, Operand match2Result, RubySymbol variableName) {
        super(Operation.SET_CAPTURED_VAR, result, match2Result);

        assert result != null: "SetCapturedVarInstr result is null";

        this.variableName = variableName;
    }

    public Operand getMatch2Result() {
        return getOperand1();
    }

    public String getId() {
        return variableName.idString();
    }

    public RubySymbol getName() {
        return variableName;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "name: " + getName() };
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new SetCapturedVarInstr(ii.getRenamedVariable(result), getMatch2Result().cloneForInlining(ii), getName());
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getMatch2Result());
        e.encode(getName());
    }

    public static SetCapturedVarInstr decode(IRReaderDecoder d) {
        return new SetCapturedVarInstr(d.decodeVariable(), d.decodeOperand(), d.decodeSymbol());
    }

    @Interp
    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject matchRes = (IRubyObject) getMatch2Result().retrieve(context, self, currScope, currDynScope, temp);
        // FIXME: Add ByteList helper
        return IRRuntimeHelpers.setCapturedVar(context, matchRes, getId());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.SetCapturedVarInstr(this);
    }
}
