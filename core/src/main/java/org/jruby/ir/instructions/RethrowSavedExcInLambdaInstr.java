package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;

public class RethrowSavedExcInLambdaInstr extends NoOperandInstr implements FixedArityInstr {
    public RethrowSavedExcInLambdaInstr() {
        super(Operation.RETHROW_SAVED_EXC_IN_LAMBDA);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return this; // FIXME: Needs update
    }

    public static RethrowSavedExcInLambdaInstr decode(IRReaderDecoder d) {
        return new RethrowSavedExcInLambdaInstr();
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.RethrowSavedExcInLambdaInstr(this);
    }
}
