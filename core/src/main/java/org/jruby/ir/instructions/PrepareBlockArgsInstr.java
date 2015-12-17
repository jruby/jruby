package org.jruby.ir.instructions;

import java.util.Arrays;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.RubyHash;
import org.jruby.RubyArray;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class PrepareBlockArgsInstr extends NoOperandInstr implements FixedArityInstr {
    public static final PrepareBlockArgsInstr INSTANCE = new PrepareBlockArgsInstr(Operation.PREPARE_BLOCK_ARGS);

    protected PrepareBlockArgsInstr(Operation op) {
        super(op);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return ii instanceof SimpleCloneInfo ? new PrepareBlockArgsInstr(Operation.PREPARE_BLOCK_ARGS) : NopInstr.NOP;  // FIXME: Is this correct
    }

    public static PrepareBlockArgsInstr decode(IRReaderDecoder d) {
        return INSTANCE;
    }
    
    @Override
    public void visit(IRVisitor visitor) {
        visitor.PrepareBlockArgsInstr(this);
    }
}
