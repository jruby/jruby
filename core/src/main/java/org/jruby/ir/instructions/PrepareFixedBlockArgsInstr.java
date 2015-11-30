package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.RubyArray;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// 
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
            args = IRubyObject.NULL_ARRAY;
        } else if (args.length == 1 && args[0].respondsTo("to_ary")) {
            IRubyObject newAry = Helpers.aryToAry(args[0]);
            if (newAry.isNil()) {
                args = new IRubyObject[] { args[0] };
            } else if (newAry instanceof RubyArray) {
                args = ((RubyArray) newAry).toJavaArray();
            } else {
                throw context.runtime.newTypeError(args[0].getType().getName() + "#to_ary should return Array");
            }
        }

        // If there are insufficient args, ReceivePreReqdInstr will return nil
        return args;
    }
    
    @Override
    public void visit(IRVisitor visitor) {
        visitor.PrepareFixedBlockArgsInstr(this);
    }
}
