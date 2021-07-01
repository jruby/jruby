package org.jruby.ir.instructions;

import org.jruby.RubySymbol;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.EnumSet;

// FIXME: When presistence is revisited this should strip these out of code streams on save and add them in if
// tracing is on for load.
/**
 * Instrumented trace.
 */
public class TraceInstr extends NoOperandInstr {
    private final RubyEvent event;
    private final RubySymbol name;
    private final String filename;
    private final int linenumber;

    public TraceInstr(RubyEvent event, RubySymbol name, String filename, int linenumber) {
        super(Operation.TRACE);

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
        return name == null ? null : name.idString();
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
        e.encode(getEvent());
        e.encode(name);
        e.encode(getFilename());
        e.encode(getLinenumber());
    }

    public static TraceInstr decode(IRReaderDecoder d) {
        return new TraceInstr(d.decodeRubyEvent(), d.decodeSymbol(), d.decodeString(), d.decodeInt());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRRuntimeHelpers.callTrace(context, getEvent(), getName(), getFilename(), getLinenumber());

        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.TraceInstr(this);
    }

    @Override
    public boolean computeScopeFlags(IRScope scope, EnumSet<IRFlags> flags) {
        flags.addAll(IRFlags.REQUIRE_ALL_FRAME_FIELDS);
        return true;
    }
}
