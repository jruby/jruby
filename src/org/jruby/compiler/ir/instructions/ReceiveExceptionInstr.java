package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ReceiveExceptionInstr extends NoOperandInstr {
    public ReceiveExceptionInstr(Variable dest) {
        super(Operation.RECV_EXCEPTION, dest);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new ReceiveExceptionInstr(ii.getRenamedVariable(getResult()));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        getResult().store(interp, context, self, interp.getException());
        
        return null;
    }
}
