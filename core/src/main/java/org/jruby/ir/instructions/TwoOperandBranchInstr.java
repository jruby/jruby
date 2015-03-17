package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRWriterEncoder;

public abstract class TwoOperandBranchInstr extends BranchInstr {
    public TwoOperandBranchInstr(Operation op, Label jumpTarget, Operand arg1, Operand arg2) {
        super(op, new Operand[] {jumpTarget, arg1, arg2});
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getJumpTarget());
        e.encode(getArg1());
        e.encode(getArg2());
    }

    public Operand getArg1() {
        return operands[1];
    }

    public Operand getArg2() {
        return operands[2];
    }
}
