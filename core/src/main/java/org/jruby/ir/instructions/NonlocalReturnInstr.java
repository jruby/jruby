package org.jruby.ir.instructions;

import org.jruby.ir.IRFlags;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Boolean;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class NonlocalReturnInstr extends ReturnBase implements FixedArityInstr {
    public final String methodName;
    public final boolean maybeLambda;

    public NonlocalReturnInstr(Operand returnValue, String methodName, boolean maybeLambda) {
        super(Operation.NONLOCAL_RETURN, returnValue);
        this.methodName = methodName;
        this.maybeLambda = maybeLambda;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { returnValue, new StringLiteral(methodName), new Boolean(maybeLambda) };
    }

    @Override
    public String toString() {
        return getOperation() + "(" + returnValue + ", <" + methodName + ":" + maybeLambda + ">" + ")";
    }

    public boolean computeScopeFlags(IRScope scope) {
        scope.getFlags().add(IRFlags.HAS_NONLOCAL_RETURNS);
        return true;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        switch (ii.getCloneMode()) {
            case CLOSURE_INLINE:
                if (ii.getInlineHostScope() instanceof IRMethod) {
                    // Treat like inlining of a regular method-return
                    Variable v = ii.getCallResultVariable();
                    return v == null ? null : new CopyInstr(v, returnValue.cloneForInlining(ii));
                }
                // fall through
            case ENSURE_BLOCK_CLONE:
            case NORMAL_CLONE:
                return new NonlocalReturnInstr(returnValue.cloneForInlining(ii), methodName, maybeLambda);
            default:
                return super.cloneForInlining(ii);
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.NonlocalReturnInstr(this);
    }
}
