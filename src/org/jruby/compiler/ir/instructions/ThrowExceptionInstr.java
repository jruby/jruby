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
public class ThrowExceptionInstr extends OneOperandInstr {
    public ThrowExceptionInstr(Operand exc) {
        super(Operation.THROW, null, exc);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new ThrowExceptionInstr(getArg().cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        if (getArg() instanceof IRException) throw ((IRException) getArg()).getException(context.getRuntime());

        Object excObj = getArg().retrieve(interp, context, self);
            
        if (excObj instanceof IRubyObject) {
            RubyKernel.raise(context, context.getRuntime().getKernel(), new IRubyObject[] {(IRubyObject)excObj}, Block.NULL_BLOCK);
        } else if (excObj instanceof Error) { // from regular ensures -- these should get passed through one level.
            throw (Error) excObj;
        } 
        
        // from breaks running ensures -- these should get passed through one level.
        throw (RuntimeException) excObj;
    }
}
