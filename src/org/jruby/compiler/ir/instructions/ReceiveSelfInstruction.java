package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// SSS FIXME: ReceiveSelf should inherit from ReceiveArg?
public class ReceiveSelfInstruction extends NoOperandInstr {
	 // SSS FIXME: destination always has to be a local variable '%variable'.  So, is this a redundant arg?
    public ReceiveSelfInstruction(Variable destination) {
        super(Operation.RECV_SELF, destination);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new CopyInstr(ii.getRenamedVariable(getResult()), ii.getCallReceiver());
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        // result is a confusing name

        // SSS FIXME: Anything else to do here?? 
        // getResult().store(interp, context, self, self);
        return null;
    }
}
