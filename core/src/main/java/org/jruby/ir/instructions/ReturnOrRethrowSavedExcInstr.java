package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;

public class ReturnOrRethrowSavedExcInstr extends ReturnInstr {
    public ReturnOrRethrowSavedExcInstr(Operand returnValue) {
        super(Operation.RETURN_OR_RETHROW_SAVED_EXC, returnValue);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return this; // FIXME: Needs update
    }

    public static ReturnOrRethrowSavedExcInstr decode(IRReaderDecoder d) {
        return new ReturnOrRethrowSavedExcInstr(d.decodeOperand());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReturnOrRethrowSavedExcInstr(this);
    }
}
