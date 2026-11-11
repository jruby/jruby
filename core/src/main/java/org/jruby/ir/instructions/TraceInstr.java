package org.jruby.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Nil;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
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
public class TraceInstr extends TwoOperandInstr {
    private final RubyEvent event;
    private final RubySymbol name;
    private final String filename;
    private final int linenumber;

    public TraceInstr(RubyEvent event, Operand module, RubySymbol name, String filename, int linenumber) {
        this(event, module, name, filename, linenumber, new Nil());
    }

    /**
     * @param value the value reported to hooks as the event's value (e.g. TracePoint#return_value for
     *              :return and :b_return events); nil for events that carry no value.
     */
    public TraceInstr(RubyEvent event, Operand module, RubySymbol name, String filename, int linenumber, Operand value) {
        super(Operation.TRACE, module, value);

        this.event = event;
        this.name = name;
        this.filename = filename;
        this.linenumber = linenumber;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new TraceInstr(event, getModule().cloneForInlining(ii), name, filename, linenumber, getValue().cloneForInlining(ii));
    }

    public Operand getModule() {
        return getOperand1();
    }

    public Operand getValue() {
        return getOperand2();
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
        e.encode(getModule());
        e.encode(name);
        e.encode(getFilename());
        e.encode(getLinenumber());
        e.encode(getValue());
    }

    public static TraceInstr decode(IRReaderDecoder d) {
        return new TraceInstr(d.decodeRubyEvent(), d.decodeOperand(), d.decodeSymbol(), d.decodeString(), d.decodeInt(), d.decodeOperand());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Object moduleValue = getModule().retrieve(context, self, currScope, currDynScope, temp);
        IRubyObject value = (IRubyObject) getValue().retrieve(context, self, currScope, currDynScope, temp);
        if (moduleValue instanceof Block block) {
            IRRuntimeHelpers.callTrace(context, block, value, getEvent(), getName(), getFilename(), getLinenumber());
        } else {
            IRubyObject selfClass = (IRubyObject) moduleValue;
            IRRuntimeHelpers.callTrace(context, selfClass, value, getEvent(), getName(), getFilename(), getLinenumber());
        }

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
