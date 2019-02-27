package org.jruby.ir.instructions;

import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.InlineCloneInfo;
import org.jruby.runtime.RubyEvent;

public class LineNumberInstr extends NoOperandInstr implements FixedArityInstr {
    public final RubyEvent event;
    public final String name;
    public final String filename;
    public final int lineNumber;
    public final boolean coverage;

    public LineNumberInstr(RubyEvent event, String name, String filename, int lineNumber, boolean coverage) {
        super(Operation.LINE_NUM);

        this.event = event;
        this.name = name;
        this.filename = filename;
        this.lineNumber = lineNumber;
        this.coverage = coverage;
    }

    public String getName() {
        return name;
    }

    public String getFilename() {
        return filename;
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
            new InlinedLineNumberInstr(((InlineCloneInfo) ii).getScopeBeingInlined(), event, name, filename, lineNumber, coverage);
        }

        // If a simple clone then we can share this instance since it cannot cause flow
        // control to change (ipc and rpc should never be accessed).
        return this;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(event.ordinal());
        e.encode(name);
        e.encode(filename);
        e.encode(getLineNumber());
        e.encode(coverage);
    }

    public static LineNumberInstr decode(IRReaderDecoder d) {
        return d.getCurrentScope().getManager().newLineNumber(
                RubyEvent.fromOrdinal(d.decodeInt()),
                d.decodeString(),
                d.decodeString(),
                d.decodeInt(),
                d.decodeBoolean());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.LineNumberInstr(this);
    }
}
