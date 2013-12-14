package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.IRMethod;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.ScopeModule;
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
    
    public String getMethodToReturnFrom() {
        return methodToReturnFrom.getName();
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { returnValue, new ScopeModule(methodToReturnFrom) };
    }

    @Override
    public String toString() {
        return getOperation() + "(" + returnValue + ", <" + (methodToReturnFrom == null ? "-NULL-" : methodToReturnFrom.getName()) + ">" + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        switch (ii.getCloneMode()) {
            case CLOSURE_INLINE:
                if (ii.getInlineHostScope() == methodToReturnFrom) {
                    // Treat like inlining of a regular method-return
                    Variable v = ii.getCallResultVariable();
                    return v == null ? null : new CopyInstr(v, returnValue.cloneForInlining(ii));
                }
                // fall through
            case NORMAL_CLONE:
                return new NonlocalReturnInstr(returnValue.cloneForInlining(ii), methodToReturnFrom);
            default:
                return super.cloneForInlining(ii);
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.NonlocalReturnInstr(this);
    }
}
