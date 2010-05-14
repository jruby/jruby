package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class BEQ_Instr extends BRANCH_Instr
{
    public BEQ_Instr(Operand v1, Operand v2, Label jmpTarget) {
        super(Operation.BEQ, v1, v2, jmpTarget);
    }

    public IR_Instr cloneForInlining(InlinerInfo ii) { 
        return new BEQ_Instr(_arg1.cloneForInlining(ii), _arg2.cloneForInlining(ii), ii.getRenamedLabel(_target));
    }
}
