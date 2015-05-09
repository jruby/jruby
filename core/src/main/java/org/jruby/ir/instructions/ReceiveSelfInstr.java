package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

public class ReceiveSelfInstr extends ResultBaseInstr implements FixedArityInstr {
    // SSS FIXME: destination always has to be a local variable '%self'.  So, is this a redundant arg?
    public ReceiveSelfInstr(Variable result) {
        super(Operation.RECV_SELF, result, EMPTY_OPERANDS);

        assert result != null: "ReceiveSelfInstr result is null";
    }

    @Override
    public Instr clone(CloneInfo info) {
        if (info instanceof SimpleCloneInfo) return this;

        // receive-self will disappear after inlining and all uses of %self will be replaced by the call receiver
        // FIXME: What about 'self' in closures??
        return null;
    }

    public static ReceiveSelfInstr decode(IRReaderDecoder d) {
        return new ReceiveSelfInstr(d.decodeVariable());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveSelfInstr(this);
    }
}
