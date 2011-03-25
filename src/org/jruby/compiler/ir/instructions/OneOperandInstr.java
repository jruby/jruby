package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

// This is of the form:
//   v = OP(arg, attribute_array); Ex: v = NOT(v1)

public abstract class OneOperandInstr extends Instr {
    Operand argument;

    public OneOperandInstr(Operation op, Variable dest, Operand argument) {
        super(op, dest);

        assert argument != null: "One operand instructions must have a non-null argument";
        
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
}
