/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.compiler.ir.instructions.jruby;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class RestoreErrorInfoInstr extends Instr {
    private final Operand arg;
    
    public RestoreErrorInfoInstr(Operand arg) {
        super(Operation.RESTORE_ERROR_INFO);
        
        this.arg = arg;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { arg };
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new RestoreErrorInfoInstr(arg.cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        context.setErrorInfo((IRubyObject) arg.retrieve(interp, context, self));
        
        return null;
    }
}
