package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.targets.JVM;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

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
    public void compile(JVM jvm) {
        jvm.method().loadLocal(0);
        jvm.emit(getReceiver());
        for (Operand operand : getCallArgs()) {
            jvm.emit(operand);
        }

        switch (getCallType()) {
            case FUNCTIONAL:
            case VARIABLE:
                jvm.method().invokeSelf(getMethodAddr().getName(), getCallArgs().length);
                break;
            case NORMAL:
                jvm.method().invokeOther(getMethodAddr().getName(), getCallArgs().length);
                break;
            case SUPER:
                jvm.method().invokeSuper(getMethodAddr().getName(), getCallArgs().length);
                break;
        }

        jvm.method().adapter.pop();
    }
}
