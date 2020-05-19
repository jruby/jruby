package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

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
