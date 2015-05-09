package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.InlineCloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

public class ReturnInstr extends ReturnBase implements FixedArityInstr {
    public ReturnInstr(Operand returnValue) {
        super(Operation.RETURN, returnValue);
    }

    @Override
    public Instr clone(CloneInfo info) {
        if (info instanceof SimpleCloneInfo) return new ReturnInstr(getReturnValue().cloneForInlining(info));

        InlineCloneInfo ii = (InlineCloneInfo) info;

        if (ii.isClosure()) return new CopyInstr(ii.getYieldResult(), getReturnValue().cloneForInlining(ii));

        Variable v = ii.getCallResultVariable();
        return v == null ? null : new CopyInstr(v, getReturnValue().cloneForInlining(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getReturnValue());
    }

    public static ReturnInstr decode(IRReaderDecoder d) {
        return new ReturnInstr(d.decodeOperand());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReturnInstr(this);
    }
}
