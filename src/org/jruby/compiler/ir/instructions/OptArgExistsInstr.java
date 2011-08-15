package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Instruction which can tell whether a particular Optional Argument exists
 * or not.  The result will either be a true of false store.
 */
public class OptArgExistsInstr extends NoOperandInstr {
    private int argIndex;
    
    // FIXME: NoOperandInstr? code smell...need to unify primitives/non-rubyobjects with rubyobject operands
    public OptArgExistsInstr(Variable result, int argIndex) {
        super(Operation.OPT_ARG_EXISTS, result);
        
        this.argIndex = argIndex;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new OptArgExistsInstr(result, argIndex);
    }
    
    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        boolean result = interp.getParameterCount() > argIndex;
        getResult().store(interp, context, self, context.getRuntime().newBoolean(result));
        return null;
    }    
}
