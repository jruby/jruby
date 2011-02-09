package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ReturnInstr extends OneOperandInstr {
    public ReturnInstr(Operand returnValue) {
        super(Operation.RETURN, null, returnValue);
        
        assert returnValue != null : "RETURN must have argument operand";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new CopyInstr(ii.getCallResultVariable(), getArg().cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        interp.setReturnValue(getArg().retrieve(interp));
        return interp.getMethodExitLabel();
    }
}
