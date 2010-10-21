package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

// If v2 is an array, compare v1 with every element of v2 and stop on first match!
public class EQQ_Instr extends TwoOperandInstr {
    public EQQ_Instr(Variable result, Operand v1, Operand v2) {
        super(Operation.EQQ, result, v1, v2);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new EQQ_Instr(ii.getRenamedVariable(result), operand1.cloneForInlining(ii), operand2.cloneForInlining(ii));
    }
}
