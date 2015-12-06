package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class PrepareSingleBlockArgInstr extends PrepareBlockArgsInstr  {
    public PrepareSingleBlockArgInstr() {
        super(Operation.PREPARE_SINGLE_BLOCK_ARG);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return ii instanceof SimpleCloneInfo ? new PrepareSingleBlockArgInstr() : NopInstr.NOP;  // FIXME: Is this correct
    }

    public static PrepareSingleBlockArgInstr decode(IRReaderDecoder d) {
        return new PrepareSingleBlockArgInstr();
    }

    public IRubyObject[] prepareBlockArgs(ThreadContext context, Block b, IRubyObject[] args) {
        if (args == null) args = IRubyObject.NULL_ARRAY;

        // Deal with proc calls
        if (context.getCurrentBlockType() == Block.Type.PROC) {
            if (args.length == 0) {
                args = context.runtime.getSingleNilArray();
            } else if (args.length == 1) {
                args = prepareProcArgs(context, b, args);
            } else {
                args = new IRubyObject[] { args[0] };
            }
        }

        // Nothing more to do! Hurray!
        // If there are insufficient args, ReceivePreReqdInstr will return nil
        return args;
    }
    
    @Override
    public void visit(IRVisitor visitor) {
        visitor.PrepareSingleBlockArgInstr(this);
    }
}
