package org.jruby.ir.instructions;

import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class NonlocalReturnInstr extends ReturnBase implements FixedArityInstr {
    public final String methodName;
    public final int methodIdToReturnFrom;

    public NonlocalReturnInstr(Operand returnValue, String methodName, int methodIdToReturnFrom) {
        super(Operation.NONLOCAL_RETURN, returnValue);
        this.methodName = methodName;
        this.methodIdToReturnFrom = methodIdToReturnFrom;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { returnValue, new StringLiteral(methodName), new Fixnum(methodIdToReturnFrom) };
    }

    @Override
    public String toString() {
        return getOperation() + "(" + returnValue + ", <" + methodName + ":" + methodIdToReturnFrom + ">" + ")";
    }

    public boolean computeScopeFlags(IRScope scope) {
        scope.getFlags().add(IRFlags.HAS_NONLOCAL_RETURNS);
        return true;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        switch (ii.getCloneMode()) {
            case CLOSURE_INLINE:
                if (ii.getInlineHostScope().getScopeId() == methodIdToReturnFrom) {
                    // Treat like inlining of a regular method-return
                    Variable v = ii.getCallResultVariable();
                    return v == null ? null : new CopyInstr(v, returnValue.cloneForInlining(ii));
                }
                // fall through
            case ENSURE_BLOCK_CLONE:
            case NORMAL_CLONE:
                return new NonlocalReturnInstr(returnValue.cloneForInlining(ii), methodName, methodIdToReturnFrom);
            default:
                return super.cloneForInlining(ii);
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.NonlocalReturnInstr(this);
    }
}
