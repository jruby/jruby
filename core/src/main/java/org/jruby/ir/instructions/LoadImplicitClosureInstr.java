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
 * Load the "implicit" closure for this scope. Currently this is always the "block" passed
 * to a body of code on the JVM stack.
 */
public class LoadImplicitClosureInstr extends ResultBaseInstr implements FixedArityInstr {
    public LoadImplicitClosureInstr(Variable result) {
        super(Operation.LOAD_IMPLICIT_CLOSURE, result, EMPTY_OPERANDS);

        assert result != null : "LoadImplicitClosureInstr result is null";
    }

    @Override
    public Instr clone(CloneInfo info) {
        if (info instanceof SimpleCloneInfo) return new LoadImplicitClosureInstr(info.getRenamedVariable(result));

        // SSS FIXME: This code below is for inlining and is untested.

        InlineCloneInfo ii = (InlineCloneInfo) info;

        // SSS FIXME: This is not strictly correct -- we have to wrap the block into an
        // operand type that converts the static code block to a proc which is a closure.
        if (ii.getCallClosure() instanceof WrappedIRClosure) return NopInstr.NOP;

        return new CopyInstr(ii.getRenamedVariable(result), ii.getCallClosure());
    }

    public static LoadImplicitClosureInstr decode(IRReaderDecoder d) {
        return new LoadImplicitClosureInstr(d.decodeVariable());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.LoadImplicitClosure(this);
    }
}
