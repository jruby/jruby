package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.IRException;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubyKernel;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;

// Right now, this is primarily used for JRuby implementation.  Ruby exceptions go through
// RubyKernel.raise (or RubyThread.raise).
public class THROW_EXCEPTION_Instr extends OneOperandInstr {
    public THROW_EXCEPTION_Instr(Operand exc) {
        super(Operation.THROW, null, exc);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new THROW_EXCEPTION_Instr(getArg().cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        if (getArg() instanceof IRException) {
            throw ((IRException)getArg()).getException(context.getRuntime());
        }
        else {
            Object excObj = getArg().retrieve(interp, context, self);
            if (excObj instanceof IRubyObject)
                RubyKernel.raise(context, context.getRuntime().getKernel(), new IRubyObject[] {(IRubyObject)excObj}, Block.NULL_BLOCK);
            else if (excObj instanceof Error)  // from regular ensures
               throw (Error)excObj;
            else // from breaks running ensures
               throw (RuntimeException)excObj;
        }

        // Control will never reach here but the Java compiler doesn't know that
        // since RubyKernel.raise doesn't declare that it throws an exception.
        return null;
    }
}
