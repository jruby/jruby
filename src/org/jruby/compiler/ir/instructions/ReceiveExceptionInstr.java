package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ReceiveExceptionInstr extends Instr implements ResultInstr {
    private final Variable result;
    
    public ReceiveExceptionInstr(Variable result) {
        super(Operation.RECV_EXCEPTION);
        
        assert result != null : "ResultExceptionInstr result is null";
        
        this.result = result;
    }

    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }
    
    public Variable getResult() {
        return result;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new ReceiveExceptionInstr(ii.getRenamedVariable(result));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        result.store(interp, context, self, interp.getException());
        
        return null;
    }
}
