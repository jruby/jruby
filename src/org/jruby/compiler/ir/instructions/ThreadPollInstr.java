package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ThreadPollInstr extends NoOperandInstr {
    public ThreadPollInstr() {
        super(Operation.THREAD_POLL, null);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return this;
    }

    // Can this instruction raise exceptions?
    @Override
    public boolean canRaiseException() { return false; }
    
    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        interp.getContext().callThreadPoll();
        return null;
    }
    
}
