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
public class ReceivePreReqdArgInstr extends ReceiveIndexedArgBase implements FixedArityInstr {
    public ReceivePreReqdArgInstr(Variable result, Variable keywords, int argIndex) {
        super(Operation.RECV_PRE_REQD_ARG, result, keywords, argIndex);
    }

    @Override
    public Instr clone(CloneInfo info) {
        if (info instanceof SimpleCloneInfo) return new ReceivePreReqdArgInstr(info.getRenamedVariable(result), info.getRenamedVariable(getKeywords()), argIndex);

        InlineCloneInfo ii = (InlineCloneInfo) info;

        if (ii.canMapArgsStatically()) return new CopyInstr(ii.getRenamedVariable(result), ii.getArg(argIndex));

        return new ReqdArgMultipleAsgnInstr(ii.getRenamedVariable(result), ii.getArgs(), argIndex, -1, -1);
    }

    public static ReceivePreReqdArgInstr decode(IRReaderDecoder d) {
        return new ReceivePreReqdArgInstr(d.decodeVariable(), d.decodeVariable(), d.decodeInt());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceivePreReqdArgInstr(this);
    }
}
