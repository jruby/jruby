package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Self;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;


public class ReceiveSelfInstr extends NoOperandResultBaseInstr implements FixedArityInstr {
    public ReceiveSelfInstr() {
        super(Operation.RECV_SELF, Self.SELF);

        assert result != null: "ReceiveSelfInstr result is null";
    }

    @Override
    public Instr clone(CloneInfo info) {
        if (info instanceof SimpleCloneInfo) return this;

        // inlining - we convert to renamed variable which replaces %self and give it the calls receiver.
        return new CopyInstr(info.getRenamedVariable(result), info.getRenamedSelfVariable(null));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        e.encode(getOperation());
    }

    public static ReceiveSelfInstr decode(IRReaderDecoder d) {
        return d.getCurrentScope().getManager().getReceiveSelfInstr();
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveSelfInstr(this);
    }
}
