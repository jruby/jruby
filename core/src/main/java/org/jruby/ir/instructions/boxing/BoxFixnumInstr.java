package org.jruby.ir.instructions.boxing;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.CloneInfo;

public class BoxFixnumInstr extends BoxInstr {
    public BoxFixnumInstr(Variable dst, Operand val) {
        super(Operation.BOX_FIXNUM, dst, val);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BoxFixnumInstr(ii.getRenamedVariable(getResult()), getValue().cloneForInlining(ii));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BoxFixnumInstr(this);
    }
}
