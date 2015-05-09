package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.InlineCloneInfo;

public class LineNumberInstr extends Instr implements FixedArityInstr {
    public final int lineNumber;

    public LineNumberInstr(int lineNumber) {
        super(Operation.LINE_NUM, EMPTY_OPERANDS);

        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"n: " + lineNumber};
    }

    @Override
    public Instr clone(CloneInfo ii) {
        // We record cloned scope so that debugging can remember where the linenumber original came from.
        // FIXME: Consider just saving filename and not entire scope
        if (ii instanceof InlineCloneInfo) {
            new InlinedLineNumberInstr(((InlineCloneInfo) ii).getScopeBeingInlined(), lineNumber);
        }

        // If a simple clone then we can share this instance since it cannot cause flow
        // control to change (ipc and rpc should never be accessed).
        return this;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getLineNumber());
    }

    public static LineNumberInstr decode(IRReaderDecoder d) {
        return new LineNumberInstr(d.decodeInt());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.LineNumberInstr(this);
    }
}
