package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class ReceiveJRubyExceptionInstr extends ReceiveExceptionBase {
    public ReceiveJRubyExceptionInstr(Variable result) {
        super(Operation.RECV_JRUBY_EXC, result);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new ReceiveJRubyExceptionInstr(ii.getRenamedVariable(result));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveJRubyExceptionInstr(this);
    }
}
