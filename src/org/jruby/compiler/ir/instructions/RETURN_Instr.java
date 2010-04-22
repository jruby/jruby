package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class RETURN_Instr extends OneOperandInstr
{
    public RETURN_Instr(Operand rv) {
        super(Operation.RETURN, null, rv);
    }

    public IR_Instr cloneForInlining(InlinerInfo ii) {
        return new COPY_Instr(ii.getCallResultVariable(), _arg.cloneForInlining(ii));
    }
}
