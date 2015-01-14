package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;

import java.util.Map;

public abstract class OneOperandBranchInstr extends BranchInstr {
    public OneOperandBranchInstr(Operation op, Operand[] operands) {
        super(op, operands);
    }

    public Operand getArg1() {
        return operands[1];
    }
}
