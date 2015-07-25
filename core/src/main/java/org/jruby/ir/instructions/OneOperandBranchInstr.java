package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;

import org.jruby.ir.persistence.IRWriterEncoder;

public abstract class OneOperandBranchInstr extends BranchInstr {
    public OneOperandBranchInstr(Operation op, Operand[] operands) {
        super(op, operands);
    }

    public Operand getArg1() {
        return getOperands()[1];
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getJumpTarget());
        e.encode(getArg1());
    }
}
