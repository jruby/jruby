package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;

// Represents target.ref = value or target = value where target is not a stack variable
public abstract class PutInstr extends Instr {
    public final int VALUE = 0;
    public final int TARGET = 1;

    Operand[] operands;
    String  ref;

    public PutInstr(Operation op, Operand target, String ref, Operand value) {
        super(op);

        operands = new Operand[] { value, target };
        this.ref = ref;
    }

    public Operand[] getOperands() {
        return operands;
    }

    public String getName() {
        return ref;
    }

    public Operand getTarget() {
        return operands[TARGET];
    }

    public Operand getValue() {
        return operands[VALUE];
    }

    @Override
    public String toString() {
        return super.toString() + "(" + operands[TARGET] +
                (ref == null ? "" : ", " + ref) + ") = " + operands[VALUE];
    }

    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        operands[VALUE] = operands[VALUE].getSimplifiedOperand(valueMap);
        operands[TARGET] = operands[TARGET].getSimplifiedOperand(valueMap);
    }
}
