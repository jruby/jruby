package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.GlobalVariable;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class GetGlobalVariableInstr extends GetInstr {
    public GetGlobalVariableInstr(Variable dest, String gvarName) {
        super(Operation.GET_GLOBAL_VAR, dest, new GlobalVariable(gvarName), null);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new GetGlobalVariableInstr(ii.getRenamedVariable(result), ((GlobalVariable)getSource()).name);
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        getResult().store(interp, getSource().retrieve(interp));
        return null;
    }
}
