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
public class LoadImplicitClosureInstr extends NoOperandResultBaseInstr implements FixedArityInstr {
    public LoadImplicitClosureInstr(Variable result) {
        super(Operation.LOAD_IMPLICIT_CLOSURE, result);

        assert result != null : "LoadImplicitClosureInstr result is null";
    }

    @Override
    public Instr clone(CloneInfo info) {
        if (info instanceof SimpleCloneInfo) return new LoadImplicitClosureInstr(info.getRenamedVariable(result));

        InlineCloneInfo ii = (InlineCloneInfo) info;

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
