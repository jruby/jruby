package org.jruby.ir.instructions.boxing;

import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.ir.IRVisitor;

public class BoxFloatInstr extends BoxInstr {
    public BoxFloatInstr(Variable dst, Operand val) {
        super(Operation.BOX_FLOAT, dst, val);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new BoxFloatInstr(ii.getRenamedVariable(getResult()), getValue().cloneForInlining(ii));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BoxFloatInstr(this);
    }
}
