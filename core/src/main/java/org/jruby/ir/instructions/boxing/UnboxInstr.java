package org.jruby.ir.instructions.boxing;

import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.NOperandResultBaseInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.CloneInfo;

public class UnboxInstr extends NOperandResultBaseInstr {
    public UnboxInstr(Operation op, Variable result, Operand value) {
        super(op, result, new Operand[] { value });
    }

    public Operand getValue() {
        return getOperands()[0];
    }

    @Override
    public Instr clone(CloneInfo info) {
        throw new UnsupportedOperationException();
    }
}
