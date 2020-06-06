package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

public class PrepareNoBlockArgsInstr extends PrepareBlockArgsInstr  {
    public static final PrepareNoBlockArgsInstr INSTANCE = new PrepareNoBlockArgsInstr();

    private PrepareNoBlockArgsInstr() {
        super(Operation.PREPARE_NO_BLOCK_ARGS);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return ii instanceof SimpleCloneInfo ? new PrepareNoBlockArgsInstr() : NopInstr.NOP;  // FIXME: Is this correct
    }

    public static PrepareNoBlockArgsInstr decode(IRReaderDecoder d) {
        return INSTANCE;
    }
    
    @Override
    public void visit(IRVisitor visitor) {
        visitor.PrepareNoBlockArgsInstr(this);
    }
}
