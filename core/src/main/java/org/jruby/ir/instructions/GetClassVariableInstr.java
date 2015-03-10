package org.jruby.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class GetClassVariableInstr extends GetInstr implements FixedArityInstr {
    public GetClassVariableInstr(Variable dest, Operand scope, String varName) {
        super(Operation.GET_CVAR, dest, scope, varName);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new GetClassVariableInstr(ii.getRenamedVariable(getResult()),
                getSource().cloneForInlining(ii), getRef());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        return ((RubyModule) getSource().retrieve(context, self, currScope, currDynScope, temp)).getClassVar(getRef());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.GetClassVariableInstr(this);
    }
}
