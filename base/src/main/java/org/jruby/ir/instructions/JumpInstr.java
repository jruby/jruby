package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;

public class JumpInstr extends OneOperandInstr implements FixedArityInstr, JumpTargetInstr {
    public JumpInstr(Label target) {
        super(Operation.JUMP, target);
    }

    public Label getJumpTarget() {
        return (Label) getOperand1();
    }

    public void setJumpTarget(Label target) {
        setOperand1(target);
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
