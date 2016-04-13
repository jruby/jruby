package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

/* Generic placeholder instruction for miscellaneous stuff that
 * needs to be done before a block's coded is executed. 
 * Eventually, this should hopefully get folded away into other things. 
 */
public class UpdateBlockExecutionStateInstr extends OneOperandInstr implements FixedArityInstr {
    public UpdateBlockExecutionStateInstr(Operand self) {
        super(Operation.UPDATE_BLOCK_STATE, self);
    }

    public Operand getSelf() {
        return getOperand1();
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return ii instanceof SimpleCloneInfo ? this : NopInstr.NOP;  // FIXME: Is this correct?
    }

    public static UpdateBlockExecutionStateInstr decode(IRReaderDecoder d) {
        return new UpdateBlockExecutionStateInstr(d.decodeVariable());
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getSelf());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.UpdateBlockExecutionStateInstr(this);
    }
}
