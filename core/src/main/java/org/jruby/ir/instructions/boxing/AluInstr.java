package org.jruby.ir.instructions.boxing;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.NOperandResultBaseInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;

import org.jruby.ir.transformations.inlining.CloneInfo;

public class AluInstr extends NOperandResultBaseInstr {
    public AluInstr(Operation op, Variable result, Operand a1, Operand a2) {
        super(op, result, new Operand[] { a1, a2 });
    }

    public Operand getArg1() {
        return getOperands()[0];
    }

    public Operand getArg2() {
        return getOperands()[1];
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new AluInstr(getOperation(), ii.getRenamedVariable(result), getArg1().cloneForInlining(ii),
                getArg2().cloneForInlining(ii));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.AluInstr(this);
    }
}
