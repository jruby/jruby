package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ReturnInstr extends Instr {
    public final IRMethod methodToReturnFrom;
    private Operand returnValue;

    public ReturnInstr(Operand returnValue, IRMethod m) {
        super(Operation.RETURN, null);
        this.methodToReturnFrom = m;
        this.returnValue = returnValue;

        assert returnValue != null : "RETURN must have returnValue operand";
    }

    public ReturnInstr(Operand returnValue) {
        this(returnValue, null);
    }

    public Operand[] getOperands() {
        return new Operand[]{returnValue};
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        returnValue = returnValue.getSimplifiedOperand(valueMap);
    }

    @Override
    public String toString() { 
        return getOperation() + "(" + returnValue + (methodToReturnFrom == null ? "" : ", <" + methodToReturnFrom.getName() + ">") + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        // SSS FIXME: This should also look at the 'methodToReturnFrom' arg
        return new CopyInstr(ii.getCallResultVariable(), returnValue.cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        interp.setReturnValue(returnValue.retrieve(interp, context, self));
        return interp.getCurrentIRScope().getCFG().getExitBB().getLabel();
    }
}
