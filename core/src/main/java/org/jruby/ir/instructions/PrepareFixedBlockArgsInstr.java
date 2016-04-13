package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

public class PrepareFixedBlockArgsInstr extends PrepareBlockArgsInstr  {
    public static final PrepareFixedBlockArgsInstr INSTANCE = new PrepareFixedBlockArgsInstr();

    private PrepareFixedBlockArgsInstr() {
        super(Operation.PREPARE_FIXED_BLOCK_ARGS);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return ii instanceof SimpleCloneInfo ? new PrepareFixedBlockArgsInstr() : NopInstr.NOP;  // FIXME: Is this correct
    }

    public static PrepareFixedBlockArgsInstr decode(IRReaderDecoder d) {
        return INSTANCE;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PrepareFixedBlockArgsInstr(this);
    }
}
