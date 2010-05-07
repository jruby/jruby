package org.jruby.compiler.ir.instructions;

// This is of the form:
//   d = s

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class COPY_Instr extends OneOperandInstr 
{
    public COPY_Instr(Variable d, Operand s) {
        super(Operation.COPY, d, s);
    }

    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap);
        return _arg;
    }

    public IR_Instr cloneForInlining(InlinerInfo ii) {
        return new COPY_Instr(ii.getRenamedVariable(_result), _arg.cloneForInlining(ii));
    }
}
