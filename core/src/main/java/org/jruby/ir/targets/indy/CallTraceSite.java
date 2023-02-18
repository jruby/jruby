package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import org.jruby.RubyModule;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class CallTraceSite extends MutableCallSite {
    private final RubyEvent event;
    private final String name;
    private final String file;
    private final int line;

    public CallTraceSite(MethodType methodType, String event, String name, String file, int line) {
        super(methodType);

        this.event = RubyEvent.fromName(event);
        this.name = name;
        this.file = file;
        this.line = line;
    }

    public static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(CallTraceSite.class),
            "bootstrap", sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, String.class, int.class),
            false);

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String event, String file, int line) {
        CallTraceSite site = new CallTraceSite(type, event, name, file, line);
        MethodHandle traceSetup = Binder
                        .from(type)
                        .prepend(site)
                        .invokeVirtualQuiet(lookup, "traceBootstrap");

        site.setTarget(traceSetup);

        return site;
    }

    public void traceBootstrap(ThreadContext context, RubyModule clazz) {
        // bind to dynamic invoker from runtime
        setTarget(Binder.from(type()).append(Helpers.arrayOf(RubyEvent.class, String.class, String.class, int.class), event, name, file, line).invoke(context.getRuntime().getCallTrace().dynamicInvoker()));

        IRRuntimeHelpers.callTrace(context, clazz, event, name, file, line);
    }

    public void traceBootstrap(ThreadContext context, Block selfBlock) {
        // bind to dynamic invoker from runtime
        setTarget(Binder.from(type()).append(Helpers.arrayOf(RubyEvent.class, String.class, String.class, int.class), event, name, file, line).invoke(context.getRuntime().getBcallTrace().dynamicInvoker()));

        IRRuntimeHelpers.callTrace(context, selfBlock.getFrameClass(), event, name, file, line);
    }
}
