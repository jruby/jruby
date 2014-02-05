package org.jruby.ir.instructions.boxing;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class UnboxFixnumInstr extends UnboxInstr {
    public UnboxFixnumInstr(Variable dst, Operand val) {
        super(Operation.UNBOX_FIXNUM, dst, val);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new UnboxFixnumInstr(ii.getRenamedVariable(getResult()), getValue().cloneForInlining(ii));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.UnboxFixnumInstr(this);
    }
}
