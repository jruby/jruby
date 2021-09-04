package org.jruby.ir.instructions;

import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.InlineCloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

import java.util.EnumSet;

import static org.jruby.ir.IRFlags.REQUIRES_BLOCK;

/**
 * Load the block passed to this scope via the on-heap frame (or similar cross-call structure).
 * This is typically used to access the "yieldable" target for blocks and evals. Only used
 * when within a scope that will use an on-heap frame.
 */
public class LoadBlockImplicitClosureInstr extends NoOperandResultBaseInstr implements FixedArityInstr {
    public LoadBlockImplicitClosureInstr(Variable result) {
        super(Operation.LOAD_BLOCK_IMPLICIT_CLOSURE, result);

        assert result != null : "LoadFrameClosureInstr result is null";
    }

    @Override
    public Instr clone(CloneInfo info) {
        if (info instanceof SimpleCloneInfo) return new LoadBlockImplicitClosureInstr(info.getRenamedVariable(result));

        // SSS FIXME: This code below is for inlining and is untested.

        InlineCloneInfo ii = (InlineCloneInfo) info;

        // SSS FIXME: This is not strictly correct -- we have to wrap the block into an
        // operand type that converts the static code block to a proc which is a closure.
        if (ii.getCallClosure() instanceof WrappedIRClosure) return NopInstr.NOP;

        return new CopyInstr(ii.getRenamedVariable(result), ii.getCallClosure());
    }

    // encode is from ResultBase since this has no other state.
    public static LoadBlockImplicitClosureInstr decode(IRReaderDecoder d) {
        return new LoadBlockImplicitClosureInstr(d.decodeVariable());
    }

    @Override
    public boolean computeScopeFlags(IRScope scope, EnumSet<IRFlags> flags) {
        return super.computeScopeFlags(scope, flags);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.LoadBlockImplicitClosure(this);
    }
}
