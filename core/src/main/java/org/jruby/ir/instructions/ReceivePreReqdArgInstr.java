package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.InlineCloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

/*
 * Assign Argument passed into scope/method to a result variable
 */
public class ReceivePreReqdArgInstr extends ReceiveArgBase implements FixedArityInstr {
    public ReceivePreReqdArgInstr(Variable result, int argIndex) {
        super(Operation.RECV_PRE_REQD_ARG, result, argIndex);
    }

    @Override
    public Instr clone(CloneInfo info) {
        if (info instanceof SimpleCloneInfo) return new ReceivePreReqdArgInstr(info.getRenamedVariable(result), argIndex);

        InlineCloneInfo ii = (InlineCloneInfo) info;

        if (ii.canMapArgsStatically()) return new CopyInstr(ii.getRenamedVariable(result), ii.getArg(argIndex));

        return new ReqdArgMultipleAsgnInstr(ii.getRenamedVariable(result), ii.getArgs(), -1, -1, argIndex);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getArgIndex());
    }

    public static ReceivePreReqdArgInstr decode(IRReaderDecoder d) {
        return new ReceivePreReqdArgInstr(d.decodeVariable(), d.decodeInt());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceivePreReqdArgInstr(this);
    }
}
