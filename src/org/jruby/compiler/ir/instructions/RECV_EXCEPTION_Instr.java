package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RECV_EXCEPTION_Instr extends NoOperandInstr
{
    public RECV_EXCEPTION_Instr(Variable dest) { super(Operation.RECV_EXCEPTION, dest); }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new RECV_EXCEPTION_Instr(ii.getRenamedVariable(result));
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        getResult().store(interp, interp.getException());
        return null;
    }
}
