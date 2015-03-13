package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

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
        super(Operation.TRACE, EMPTY_OPERANDS);

        this.event = event;
        this.name = name;
        this.filename = filename;
        this.linenumber = linenumber;
    }

    @Override
    public Instr clone(CloneInfo ii) {
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
    public String[] toStringNonOperandArgs() {
        return new String[] {"ev: " + event, "name: " + name, "file: " + filename, "line: " + linenumber};
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getEvent().ordinal());
        e.encode(getName());
        e.encode(getFilename());
        e.encode(getLinenumber());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        if (context.runtime.hasEventHooks()) {
            // FIXME: Try and statically generate END linenumber instead of hacking it.
            int linenumber = getLinenumber() == -1 ? context.getLine()+1 : getLinenumber();

            context.trace(getEvent(), getName(), context.getFrameKlazz(), getFilename(), linenumber);
        }

        return null;
    }
}
