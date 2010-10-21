package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

// This instruction represents primitive arithmetic and logical operations
// and would be generated only during optimization passes that unbox ruby objects
// into native platform objects.
public class ALU_Instr extends TwoOperandInstr
{
    public ALU_Instr(Operation op, Variable dst, Operand arg1, Operand arg2) {
        super(op, dst, arg1, arg2);
    }

    public ALU_Instr(Operation op, Variable dst, Operand arg) {
        super(op, dst, arg, null);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new ALU_Instr(operation, ii.getRenamedVariable(result), operand1.cloneForInlining(ii), operand2 != null ? operand2.cloneForInlining(ii) : null);
    }
}
