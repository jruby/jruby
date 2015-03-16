package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.InlineCloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

/**
 * Load the block passed to this scope via the on-heap frame (or similar cross-call structure).
 * This is typically used to access the "yieldable" target for blocks and evals. Only used
 * when within a scope that will use an on-heap frame.
 */
public class LoadFrameClosureInstr extends ResultBaseInstr implements FixedArityInstr {
    public LoadFrameClosureInstr(Variable result) {
        super(Operation.LOAD_FRAME_CLOSURE, result, EMPTY_OPERANDS);

        assert result != null : "LoadFrameClosureInstr result is null";
    }

    @Override
    public Instr clone(CloneInfo info) {
        if (info instanceof SimpleCloneInfo) return new LoadFrameClosureInstr(info.getRenamedVariable(result));

        // SSS FIXME: This code below is for inlining and is untested.

        InlineCloneInfo ii = (InlineCloneInfo) info;

        // SSS FIXME: This is not strictly correct -- we have to wrap the block into an
        // operand type that converts the static code block to a proc which is a closure.
        if (ii.getCallClosure() instanceof WrappedIRClosure) return NopInstr.NOP;

        return new CopyInstr(ii.getRenamedVariable(result), ii.getCallClosure());
    }

    public static LoadFrameClosureInstr decode(IRReaderDecoder d) {
        return new LoadFrameClosureInstr(d.decodeVariable());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.LoadFrameClosure(this);
    }
}
