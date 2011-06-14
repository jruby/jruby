package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubyKernel;
import org.jruby.runtime.Block;
import org.jruby.exceptions.RaiseException;

// Right now, this is primarily used for JRuby implementation.  Ruby exceptions go through
// RubyKernel.raise (or RubyThread.raise).
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
    public Label interpret(InterpreterContext interp) {
        RubyKernel.raise(interp.getContext(), interp.getRuntime().getKernel(), new IRubyObject[] {(IRubyObject)getArg().retrieve(interp)}, Block.NULL_BLOCK);

        // Control will never reach here but the Java compiler doesn't know that
        // since the above method doesn't declare that it throws an exception,
        return null;
    }
}
