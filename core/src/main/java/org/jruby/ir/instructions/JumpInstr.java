package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;

public class JumpInstr extends Instr implements FixedArityInstr {
    public JumpInstr(Label target) {
        super(Operation.JUMP, new Operand[] { target });
    }

    public Label getJumpTarget() {
        return (Label) operands[0];
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new JumpInstr(ii.getRenamedLabel(getJumpTarget()));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getJumpTarget());
    }

    public static JumpInstr decode(IRReaderDecoder d) {
        return new JumpInstr(d.decodeLabel());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.JumpInstr(this);
    }
}
