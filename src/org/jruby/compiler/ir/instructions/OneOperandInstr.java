package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

// This is of the form:
//   v = OP(arg, attribute_array); Ex: v = NOT(v1)

public class OneOperandInstr extends Instr {
    Operand argument;

    public OneOperandInstr(Operation op, Variable dest, Operand argument) {
        super(op, dest);
        
        this.argument = argument;
    }

    @Override
    public String toString() { 
        return super.toString() + "(" + argument + ")";
    }

    public Operand getArg() {
        return argument;
    }

    public Operand[] getOperands() {
        return new Operand[] {argument};
    }

    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        argument = argument.getSimplifiedOperand(valueMap);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new OneOperandInstr(operation, ii.getRenamedVariable(result), argument.cloneForInlining(ii));
    }
}
