package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;

/**
 * This instruction toggles a single per thread field which specifies whether an exception
 * being thrown needs to generate backtrace info. At any point after toggling this to be
 * false (no backtrace) you may encounter a nested exception which does require a backtrace.
 * This nested exception will toggle back to needing an exception.
 *
 * In theory, we could restore this field as we unwind frames but largely this optimization
 * only occurs in very simple scenarios.
 *
 * Also important to note this is only requesting for a backtrace or not.  If you request
 * no backtrace but the error is not a StandardError exception it will still be required
 * to generate a backtrace.
 */
public class ToggleBacktraceInstr extends NoOperandInstr {
    private final boolean requiresBacktrace;

    public ToggleBacktraceInstr(boolean requiresBacktrace) {
        super(Operation.TOGGLE_BACKTRACE);

        this.requiresBacktrace = requiresBacktrace;
    }

    public boolean requiresBacktrace() {
        return requiresBacktrace;
    }

    public String[] toStringNonOperandArgs() {
        return new String[] { "" + requiresBacktrace };
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(requiresBacktrace);
    }

    public static ToggleBacktraceInstr decode(IRReaderDecoder d) {
        return d.getCurrentScope().getManager().needsBacktrace(d.decodeBoolean());
    }


    @Override
    public void visit(IRVisitor visitor) {
        visitor.ToggleBacktraceInstr(this);
    }

    @Override
    public Instr clone(CloneInfo info) {
        return this;
    }
}
