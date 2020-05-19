package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;

public class ReceiveJRubyExceptionInstr extends ReceiveExceptionBase {
    public ReceiveJRubyExceptionInstr(Variable result) {
        super(Operation.RECV_JRUBY_EXC, result);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new ReceiveJRubyExceptionInstr(ii.getRenamedVariable(result));
    }

    public static ReceiveJRubyExceptionInstr decode(IRReaderDecoder d) {
        return new ReceiveJRubyExceptionInstr(d.decodeVariable());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveJRubyExceptionInstr(this);
    }
}
