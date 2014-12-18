package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

public class PushFrameInstr extends Instr implements FixedArityInstr {
    private final String frameName;
    public PushFrameInstr(String frameName) {
        super(Operation.PUSH_FRAME);

        this.frameName = frameName;
    }

    @Override
    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    public String getFrameName() {
        return frameName;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return ii instanceof SimpleCloneInfo ? this : NopInstr.NOP;  // FIXME: Is this correct?
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PushFrameInstr(this);
    }
}
