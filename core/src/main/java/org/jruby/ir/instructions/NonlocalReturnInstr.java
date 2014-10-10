package org.jruby.ir.instructions;

import org.jruby.ir.*;
import org.jruby.ir.operands.Boolean;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.InlineCloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

public class NonlocalReturnInstr extends ReturnBase implements FixedArityInstr {
    public final String methodName; // Primarily a debugging aid
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
    public Instr clone(CloneInfo info) {
        if (info instanceof SimpleCloneInfo) return new NonlocalReturnInstr(returnValue.cloneForInlining(info), methodName, maybeLambda);

        InlineCloneInfo ii = (InlineCloneInfo) info;
        if (ii.isClosure()) {
            if (ii.getHostScope() instanceof IRMethod) {
                // Treat like inlining of a regular method-return
                Variable v = ii.getCallResultVariable();
                return v == null ? null : new CopyInstr(v, returnValue.cloneForInlining(ii));
            }

            return new NonlocalReturnInstr(returnValue.cloneForInlining(ii), methodName, maybeLambda);
        }

        return super.clone(ii);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.NonlocalReturnInstr(this);
    }
}
