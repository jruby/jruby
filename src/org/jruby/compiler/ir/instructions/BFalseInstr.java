package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class BFalseInstr extends BranchInstr {
    protected BFalseInstr(Operand v, Label jmpTarget) {
        super(Operation.B_FALSE, v, null, jmpTarget);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new BFalseInstr(getArg1().cloneForInlining(ii), ii.getRenamedLabel(getJumpTarget()));
    }
}
