package org.jruby.ir.instructions;

import org.jruby.ext.coverage.CoverageData;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.InlineCloneInfo;

public class LineNumberInstr extends NoOperandInstr implements FixedArityInstr {
    public final int lineNumber;

    /**
     * Whether to update coverage data when executing this instruction.
     *
     * Note this is non-final so that `oneshot_lines` mode can disable coverage.
     */
    public boolean coverage;

    /**
     * Whether to disable coverage after the first execution.
     *
     * This allows lightweight "covered or not" coverage that only updates once per line.
     */
    public final boolean oneshot;

    public LineNumberInstr(int lineNumber, int coverageMode) {
        super(Operation.LINE_NUM);

        this.lineNumber = lineNumber;
        this.coverage = coverageMode != CoverageData.NONE;
        this.oneshot = (coverageMode & CoverageData.ONESHOT_LINES) != 0;
    }

    public LineNumberInstr(int lineNumber) {
        super(Operation.LINE_NUM);

        this.lineNumber = lineNumber;
        this.coverage = false;
        this.oneshot = false;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public boolean isCoverage() {
        return coverage;
    }

    public boolean isOneshot() {
        return oneshot;
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
        return d.getCurrentScope().getManager().newLineNumber(d.decodeInt());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.LineNumberInstr(this);
    }
}
