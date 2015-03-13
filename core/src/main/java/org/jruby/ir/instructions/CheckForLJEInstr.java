package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;

/**
  */
public class CheckForLJEInstr extends Instr {
    private boolean maybeLambda;

    public CheckForLJEInstr(boolean maybeLambda) {
        super(Operation.CHECK_FOR_LJE, EMPTY_OPERANDS);

        this.maybeLambda = maybeLambda;
    }

    public boolean maybeLambda() {
        return maybeLambda;
    }

    @Override
    public Instr clone(CloneInfo info) {
        return new CheckForLJEInstr(maybeLambda);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(maybeLambda());
    }

    public void visit(IRVisitor visitor) {
        visitor.CheckForLJEInstr(this);
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "maybe_lambda: " + maybeLambda};
    }

    public void check(ThreadContext context, DynamicScope dynamicScope, Block.Type blockType) {
        IRRuntimeHelpers.checkForLJE(context, dynamicScope, maybeLambda, blockType);
    }
}
