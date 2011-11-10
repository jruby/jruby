package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class NoResultCallInstr extends CallBase {
    public NoResultCallInstr(Operation op, CallType callType, MethAddr methAddr, Operand receiver, Operand[] args, Operand closure) {
        super(op, callType, methAddr, receiver, args, closure);
    }
    
    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new NoResultCallInstr(getOperation(), getCallType(), (MethAddr) getMethodAddr().cloneForInlining(ii), 
                receiver.cloneForInlining(ii), cloneCallArgs(ii), closure == null ? null : closure.cloneForInlining(ii));
    }    

    @Override
    public Label interpret(InterpreterContext interp, IRExecutionScope scope, ThreadContext context, IRubyObject self) {
        callAdapter.call(interp, context, self, (IRubyObject) getReceiver().retrieve(interp, context, self));
        return null;
    }  
}
