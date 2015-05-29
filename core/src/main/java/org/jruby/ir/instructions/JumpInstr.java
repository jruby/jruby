package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;

public class JumpInstr extends Instr implements FixedArityInstr {
    final boolean exitsExcRegion;

    public JumpInstr(Label target) {
        this(target, false);
    }

    public JumpInstr(Label target, boolean exitsExcRegion) {
        super(Operation.JUMP, new Operand[] { target });
        this.exitsExcRegion = exitsExcRegion;
    }

    public boolean exitsExcRegion() {
        return this.exitsExcRegion;
    }

    public Label getJumpTarget() {
        return (Label) operands[0];
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new JumpInstr(ii.getRenamedLabel(getJumpTarget()), exitsExcRegion);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getJumpTarget());
        e.encode(exitsExcRegion);
    }

    public static JumpInstr decode(IRReaderDecoder d) {
        return new JumpInstr(d.decodeLabel(), d.decodeBoolean());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.JumpInstr(this);
    }
}
