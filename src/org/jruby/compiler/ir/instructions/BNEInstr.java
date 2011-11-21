package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.BooleanLiteral;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class BNEInstr extends BranchInstr {
    public static BranchInstr create(Operand v1, Operand v2, Label jmpTarget) {
        if (v2 == BooleanLiteral.TRUE) return new BFalseInstr(v1, jmpTarget);
        if (v2 == BooleanLiteral.FALSE) return new BTrueInstr(v1, jmpTarget);
        return new BNEInstr(v1, v2, jmpTarget);
    }

    public BNEInstr(Operand v1, Operand v2, Label jmpTarget) {
        super(Operation.BNE, v1, v2, jmpTarget);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new BNEInstr(getArg1().cloneForInlining(ii), 
                getArg2().cloneForInlining(ii), ii.getRenamedLabel(getJumpTarget()));
    }
}
