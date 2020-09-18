package org.jruby.ir.instructions;

import org.jruby.ir.*;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.InlineCloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

import java.util.EnumSet;

public class NonlocalReturnInstr extends ReturnBase implements FixedArityInstr {
    public final String methodId; // Primarily a debugging aid

    public NonlocalReturnInstr(Operand returnValue, String methodId) {
        super(Operation.NONLOCAL_RETURN, returnValue);
        this.methodId = methodId;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "name: " + methodId };
    }

    @Override
    public boolean computeScopeFlags(IRScope scope, EnumSet<IRFlags> flags) {
        scope.setHasNonLocalReturns();
        return true;
    }

    @Override
    public Instr clone(CloneInfo info) {
        if (info instanceof SimpleCloneInfo) return new NonlocalReturnInstr(getReturnValue().cloneForInlining(info), methodId);

        InlineCloneInfo ii = (InlineCloneInfo) info;
        if (ii.isClosure()) {
            if (ii.getHostScope() instanceof IRMethod) {
                // Lexically contained non-local returns can return directly if the live in the method they are inlining to.
                if (((InlineCloneInfo) info).getScopeBeingInlined().isScopeContainedBy(ii.getHostScope())) {
                    return new ReturnInstr(getReturnValue().cloneForInlining(ii));
                }

                // Treat like inlining of a regular method-return (note: a jump is added to exit so this copy
                // actually ends up being the methods return value).
                Variable v = ii.getCallResultVariable();
                return v == null ? null : new CopyInstr(v, getReturnValue().cloneForInlining(ii));
            }

            return new NonlocalReturnInstr(getReturnValue().cloneForInlining(ii), methodId);
        } else {
            throw new UnsupportedOperationException("Nonlocal returns shouldn't show up outside closures.");
        }
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getReturnValue());
        e.encode(methodId);
    }

    public static NonlocalReturnInstr decode(IRReaderDecoder d) {
        return new NonlocalReturnInstr(d.decodeOperand(), d.decodeString());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.NonlocalReturnInstr(this);
    }
}
