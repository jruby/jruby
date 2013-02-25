package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.IRMethod;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class NonlocalReturnInstr extends ReturnBase {
    public final IRMethod methodToReturnFrom;

    public NonlocalReturnInstr(Operand returnValue, IRMethod methodToReturnFrom) {
        super(Operation.NONLOCAL_RETURN, returnValue);
        this.methodToReturnFrom = methodToReturnFrom;
    }

    public NonlocalReturnInstr(Operand returnValue) {
        this(returnValue, null);
    }

    @Override
    public String toString() {
        return getOperation() + "(" + returnValue + ", <" + (methodToReturnFrom == null ? "-NULL-" : methodToReturnFrom.getName()) + ">" + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new NonlocalReturnInstr(returnValue.cloneForInlining(ii), methodToReturnFrom);
    }

    @Override
    public Instr cloneForInlinedScope(InlinerInfo ii) {
        if (ii.getInlineHostScope() == methodToReturnFrom) {
            // Convert to a regular return instruction
            return new NonlocalReturnInstr(returnValue.cloneForInlining(ii));
        } else {
            return cloneForInlining(ii);
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.NonlocalReturnInstr(this);
    }
}
