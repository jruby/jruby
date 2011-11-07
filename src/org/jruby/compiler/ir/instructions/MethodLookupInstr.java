package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MethodHandle;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class MethodLookupInstr extends Instr {
    private Operand methodHandle;

    public MethodLookupInstr(Variable dest, MethodHandle mh) {
        super(Operation.METHOD_LOOKUP, dest);
        this.methodHandle = mh;
    }

    public MethodLookupInstr(Variable dest, Operand methodName, Operand receiver) {
        this(dest, new MethodHandle(methodName, receiver));
    }

    public MethodHandle getMethodHandle() {
        return (MethodHandle)methodHandle;
    }

    public Operand[] getOperands() {
        return new Operand[]{methodHandle};
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        methodHandle = methodHandle.getSimplifiedOperand(valueMap);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + methodHandle + ")";
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new MethodLookupInstr(ii.getRenamedVariable(getResult()), (MethodHandle)methodHandle.cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        getResult().store(interp, context, self, methodHandle.retrieve(interp, context, self));
        return null;
    }
}
