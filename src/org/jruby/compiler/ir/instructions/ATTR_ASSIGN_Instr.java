package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class ATTR_ASSIGN_Instr extends MultiOperandInstr
{
    public ATTR_ASSIGN_Instr(Operand obj, Operand attr, Operand value) {
        super(Operation.ATTR_ASSIGN, null, new Operand[] { obj, attr, value });
    }

    public IR_Instr cloneForInlining(InlinerInfo ii) {
        return new ATTR_ASSIGN_Instr(_args[0].cloneForInlining(ii), _args[1].cloneForInlining(ii), _args[2].cloneForInlining(ii));
    }
}
