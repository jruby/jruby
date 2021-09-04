package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
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
            "bootstrap", sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, String.class, String.class, int.class),
            false);

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String unused, MethodType type, String event, String name, String file, int line) {
        MutableCallSite site = new CallTraceSite(type, event, name, file, line);

        site.setTarget(
                Binder
                        .from(void.class, ThreadContext.class)
                        .insert(0, site)
                        .invokeVirtualQuiet(lookup, "trace")
        );

        return site;
    }

    public void trace(ThreadContext context) {
        IRRuntimeHelpers.callTrace(context, event, name, file, line);
    }
}
