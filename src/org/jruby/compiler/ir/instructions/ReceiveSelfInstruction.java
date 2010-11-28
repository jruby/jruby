package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

// SSS FIXME: ReceiveSelf should inherit from ReceiveArg?
public class ReceiveSelfInstruction extends NoOperandInstr {
	 // SSS FIXME: destination always has to be a local variable '%variable'.  So, is this a redundant arg?
    public ReceiveSelfInstruction(Variable destination) {
        super(Operation.RECV_SELF, destination);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new CopyInstr(ii.getRenamedVariable(result), ii.getCallReceiver());
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        Operand destination = getResult(); // result is a confusing name
        // SSS FIXME: Anything else to do here?? 
        destination.store(interp, self);
        return null;
    }
}
