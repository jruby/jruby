package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.GlobalVariable;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class PutGlobalVarInstr extends PutInstr {

    public PutGlobalVarInstr(String varName, Operand value) {
        super(Operation.PUT_GLOBAL_VAR, new GlobalVariable(varName), null, value);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new PutGlobalVarInstr(((GlobalVariable) operands[TARGET]).name, operands[VALUE].cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        getTarget().store(interp, getValue().retrieve(interp));
        return null;
    }
}
