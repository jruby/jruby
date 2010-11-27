package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.MethodHandle;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class MethodLookupInstr extends OneOperandInstr {
    public MethodLookupInstr(Variable dest, MethodHandle mh) {
        super(Operation.METHOD_LOOKUP, dest, mh);
    }

    public MethodLookupInstr(Variable dest, Operand methodName, Operand receiver) {
        super(Operation.METHOD_LOOKUP, dest, new MethodHandle(methodName, receiver));
    }

    public MethodHandle getMethodHandle() {
        return (MethodHandle)getArg();
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new MethodLookupInstr(ii.getRenamedVariable(getResult()), (MethodHandle)getArg().cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        getResult().store(interp, getMethodHandle().retrieve(interp));
        return null;
    }
}
