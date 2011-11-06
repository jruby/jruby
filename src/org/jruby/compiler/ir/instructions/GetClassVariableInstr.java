package org.jruby.compiler.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
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
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        getResult().store(interp, context, self, 
                ((RubyModule) getSource().retrieve(interp, context, self)).getClassVar(getRef()));
        return null;
    }
}
