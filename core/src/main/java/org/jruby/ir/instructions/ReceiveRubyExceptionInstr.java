package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;

public class ReceiveRubyExceptionInstr extends ReceiveExceptionBase {
    public ReceiveRubyExceptionInstr(Variable result) {
        super(Operation.RECV_RUBY_EXC, result);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new ReceiveRubyExceptionInstr(ii.getRenamedVariable(result));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveRubyExceptionInstr(this);
    }

    public static ReceiveRubyExceptionInstr decode(IRReaderDecoder d) {
        return new ReceiveRubyExceptionInstr(d.decodeVariable());
    }
}
