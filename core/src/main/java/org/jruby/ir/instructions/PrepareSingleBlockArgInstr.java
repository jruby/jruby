package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

public class PrepareSingleBlockArgInstr extends PrepareBlockArgsInstr  {
    public static final PrepareSingleBlockArgInstr INSTANCE = new PrepareSingleBlockArgInstr();

    private PrepareSingleBlockArgInstr() {
        super(Operation.PREPARE_SINGLE_BLOCK_ARG);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return ii instanceof SimpleCloneInfo ? new PrepareSingleBlockArgInstr() : NopInstr.NOP;  // FIXME: Is this correct
    }

    public static PrepareSingleBlockArgInstr decode(IRReaderDecoder d) {
        return INSTANCE;
    }
    
    @Override
    public void visit(IRVisitor visitor) {
        visitor.PrepareSingleBlockArgInstr(this);
    }
}
