package org.jruby.ir.targets;

import com.headius.invokebinder.Binder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.opto.Invalidator;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.SwitchPoint;

import static java.lang.invoke.MethodType.methodType;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class TraceSite extends MutableCallSite {
    public final static String TRACE_SIG = sig(void.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class, String.class, String.class, int.class, int.class);

    public static Handle traceHandle() {
        return new Handle(
                Opcodes.H_NEWINVOKESPECIAL,
                p(TraceSite.class),
                "<init>",
                TRACE_SIG,
                false);
    }

    public TraceSite(MethodHandles.Lookup lookup, String traceCall, MethodType type, int eventNumber, String name, String fileName, int line, int coverage) {
        super(type);

        RubyEvent event = RubyEvent.fromOrdinal(eventNumber);

        this.lookup = lookup;
        this.event = event;
        this.name = name.length() == 0 ? null : name;
        this.filename = fileName;
        this.line = line;
        this.coverage = coverage == 0 ? false : true;

        this.traceSetup = Binder.from(methodType(void.class, ThreadContext.class))
                .insert(0, TraceSite.class, this)
                .invokeVirtualQuiet(lookup, "traceSetup");

        setTarget(traceSetup);
    }

    public void traceSetup(ThreadContext context) {
        Invalidator invalidator = context.runtime.getTraceInvalidator();

        MethodHandle target;

        if (context.runtime.hasEventHooks()) {
            target = Binder.from(methodType(void.class, ThreadContext.class))
                    .insert(0, TraceSite.class, this)
                    .invokeVirtualQuiet(lookup, "traceOn");
        } else {
            target = Binder
                    .from(void.class, ThreadContext.class)
                    .nop();
        }

        target = ((SwitchPoint)invalidator.getData()).guardWithTest(target, traceSetup);

        setTarget(target);
    }

    public void traceOn(ThreadContext context) {
        IRRuntimeHelpers.fireTraceEvent(context, this);
    }

    private final MethodHandles.Lookup lookup;
    public final RubyEvent event;
    public final String name;
    public final String filename;
    public final int line;
    public final boolean coverage;
    private final MethodHandle traceSetup;
}
