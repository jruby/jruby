package org.jruby.ir.instructions.boxing;

import org.jruby.ir.Operation;
import org.jruby.ir.instructions.NOperandResultBaseInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;

public abstract class BoxInstr extends NOperandResultBaseInstr {
    public BoxInstr(Operation op, Variable result, Operand val) {
        super(op, result, new Operand[] { val });
    }

    public Operand getValue() {
        return getOperands()[0];
    }
}
