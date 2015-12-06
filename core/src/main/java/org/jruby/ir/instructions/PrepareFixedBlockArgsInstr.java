package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class PrepareFixedBlockArgsInstr extends PrepareBlockArgsInstr  {
    public PrepareFixedBlockArgsInstr() {
        super(Operation.PREPARE_FIXED_BLOCK_ARGS);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return ii instanceof SimpleCloneInfo ? new PrepareFixedBlockArgsInstr() : NopInstr.NOP;  // FIXME: Is this correct
    }

    public static PrepareFixedBlockArgsInstr decode(IRReaderDecoder d) {
        return new PrepareFixedBlockArgsInstr();
    }

    public IRubyObject[] prepareBlockArgs(ThreadContext context, Block b, IRubyObject[] args) {
        if (args == null) {
            return IRubyObject.NULL_ARRAY;
        }

        boolean isProcCall = context.getCurrentBlockType() == Block.Type.PROC;
        if (isProcCall) {
            return prepareProcArgs(context, b, args);
        }

        boolean isLambda = b.type == Block.Type.LAMBDA;
        if (isLambda && isProcCall) {
            return args;
        }

        // SSS FIXME: This check here is not required as long as
        // the single-instruction cases always uses PreapreSingleBlockArgInstr
        // But, including this here for robustness for now.
        if (b.getBody().getSignature().arityValue() == 1) {
            return args;
        }

        // Since we have more than 1 required arg,
        // convert a single value to an array if possible.
        args = toAry(context, args);

        // If there are insufficient args, ReceivePreReqdInstr will return nil
        return args;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PrepareFixedBlockArgsInstr(this);
    }
}
