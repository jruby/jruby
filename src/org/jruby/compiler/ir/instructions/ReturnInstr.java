package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.targets.JVM;

public class ReturnInstr extends Instr {
    public final IRMethod methodToReturnFrom;
    private Operand returnValue;

    public ReturnInstr(Operand returnValue, IRMethod methodToReturnFrom) {
        super(Operation.RETURN);
        this.methodToReturnFrom = methodToReturnFrom;
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
        return new ReturnInstr(returnValue.cloneForInlining(ii), methodToReturnFrom);
    }

    @Override
    public Instr cloneForInlinedScope(InlinerInfo ii) {
        if (methodToReturnFrom == null) {
            Variable v = ii.getCallResultVariable();
            return v == null ? null : new CopyInstr(v, returnValue.cloneForInlining(ii));
        } else if (ii.getInlineHostScope() == methodToReturnFrom) {
            // Convert to a regular return instruction
            return new ReturnInstr(returnValue.cloneForInlining(ii));
        } else {
            return cloneForInlining(ii);
        }
    }

    public void compile(JVM jvm) {
        jvm.emit(getOperands()[0]);
        jvm.method().returnValue();
    }
}
