package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class PrepareBlockArgsInstr extends NoOperandInstr implements FixedArityInstr {
    public PrepareBlockArgsInstr(Operation op) {
        super(op);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return ii instanceof SimpleCloneInfo ? new PrepareBlockArgsInstr(Operation.PREPARE_BLOCK_ARGS) : NopInstr.NOP;  // FIXME: Is this correct
    }

    public static PrepareBlockArgsInstr decode(IRReaderDecoder d) {
        return new PrepareBlockArgsInstr(Operation.PREPARE_BLOCK_ARGS);
    }
    
    @Override
    public void visit(IRVisitor visitor) {
        visitor.PrepareBlockArgsInstr(this);
    }

    public IRubyObject[] prepareBlockArgs(ThreadContext context, Block b, IRubyObject[] args) {
        // SSS FIXME: Incomplete: This is the placeholder for
        // scenarios not handled by specialized instructions.
        return args;
    }
}
