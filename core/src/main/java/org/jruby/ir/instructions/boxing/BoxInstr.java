package org.jruby.ir.instructions.boxing;

import org.jruby.ir.Operation;
import org.jruby.ir.instructions.ResultBaseInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;

public abstract class BoxInstr extends ResultBaseInstr {
    public BoxInstr(Operation op, Variable result, Operand val) {
        super(op, result, val);
    }

    public Operand getValue() {
        return getSingleOperand();
    }
}
