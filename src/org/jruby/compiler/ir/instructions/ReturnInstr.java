package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class ReturnInstr extends Instr {
    public final IRMethod methodToReturnFrom;
    private Operand returnValue;

    public ReturnInstr(Operand returnValue, IRMethod m) {
        super(Operation.RETURN);
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

    public Operand getReturnValue() {
       return returnValue;
    }
    
    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        returnValue = returnValue.getSimplifiedOperand(valueMap, force);
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
}
