package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ClosureReturnInstr extends OneOperandInstr {
    public ClosureReturnInstr(Operand rv) {
        super(Operation.CLOSURE_RETURN, null, rv);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        throw new RuntimeException("Not implemented yet!");
    }

    @Interp
    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        interp.setReturnValue(getArg().retrieve(interp));
        return interp.getMethodExitLabel();
    }
}
