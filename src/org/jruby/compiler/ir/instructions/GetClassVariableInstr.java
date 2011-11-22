package org.jruby.compiler.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class GetClassVariableInstr extends GetInstr {
    public GetClassVariableInstr(Variable dest, Operand scope, String varName) {
        super(Operation.GET_CVAR, dest, scope, varName);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new GetClassVariableInstr(ii.getRenamedVariable(getResult()),
                getSource().cloneForInlining(ii), getRef());
    }

    @Override
    public Object interpret(ThreadContext context, IRubyObject self, Object[] temp, Block block) {
        return ((RubyModule) getSource().retrieve(context, self, temp)).getClassVar(getRef());
    }
}
