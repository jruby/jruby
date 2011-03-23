package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubyException;
import org.jruby.exceptions.RaiseException;

public class THROW_EXCEPTION_Instr extends OneOperandInstr
{
    public THROW_EXCEPTION_Instr(Operand exc)
    {
        super(Operation.THROW, null, exc);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new THROW_EXCEPTION_Instr(getArg().cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        throw new RaiseException((RubyException) getArg().retrieve(interp));
    }
}
