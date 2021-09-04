package org.jruby.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class GetClassVariableInstr extends GetInstr implements FixedArityInstr {
    public GetClassVariableInstr(Variable dest, Operand scope, RubySymbol variableName) {
        super(Operation.GET_CVAR, dest, scope, variableName);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new GetClassVariableInstr(ii.getRenamedVariable(getResult()),
                getSource().cloneForInlining(ii), getName());
    }

    public static GetClassVariableInstr decode(IRReaderDecoder d) {
        return new GetClassVariableInstr(d.decodeVariable(), d.decodeOperand(), d.decodeSymbol());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        return ((RubyModule) getSource().retrieve(context, self, currScope, currDynScope, temp)).getClassVar(getId());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.GetClassVariableInstr(this);
    }
}
