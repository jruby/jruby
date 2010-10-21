package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

// This is of the form:
//   v = OP(arg1, arg2, attribute_array); Ex: v = ADD(v1, v2)
public abstract class TwoOperandInstr extends Instr {
    Operand operand1;
    Operand operand2;

    public TwoOperandInstr(Operation op, Variable destination, Operand a1, Operand a2) {
        super(op, destination);
        operand1 = a1;
        operand2 = a2;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + operand1 + ", " + operand2 + ")";
    }

    public Operand[] getOperands() {
        return new Operand[]{operand1, operand2};
    }

    public Operand getOperand1() {
        return operand1;
    }
    
    public Operand getOperand2() {
        return operand2;
    }

    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        operand1 = operand1.getSimplifiedOperand(valueMap);
        operand2 = operand2.getSimplifiedOperand(valueMap);
    }
}
