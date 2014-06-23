package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.RubyEvent;

// FIXME: When presistence is revisited this should strip these out of code streams on save and add them in if
// tracing is on for load.
/**
 * Instrumented trace.
 */
public class TraceInstr extends Instr {
    private final RubyEvent event;
    private final String name;
    private final String filename;
    private final int linenumber;

    public TraceInstr(RubyEvent event, String name, String filename, int linenumber) {
        super(Operation.TRACE);

        this.event = event;
        this.name = name;
        this.filename = filename;
        this.linenumber = linenumber;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new TraceInstr(event, name, filename, linenumber);
    }

    public RubyEvent getEvent() {
        return event;
    }

    public String getName() {
        return name;
    }

    public String getFilename() {
        return filename;
    }

    public int getLinenumber() {
        return linenumber;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { };
    }

    public String toString() {
        return super.toString() + " " + event + ", " + name + ", " + filename + ", " + linenumber;
    }

}
