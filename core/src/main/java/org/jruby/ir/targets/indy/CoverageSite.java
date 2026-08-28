package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodType.methodType;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class CoverageSite {
    public static final Handle COVER_LINE_BOOTSTRAP = new Handle(
                Opcodes.H_INVOKESTATIC,
                p(CoverageSite.class),
                "coverLineBootstrap",
                sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, int.class, int.class),
                false);

    public static CallSite coverLineBootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String filename, int line, int oneshot) throws Throwable {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle handle = lookup.findStatic(CoverageSite.class, "coverLineFallback", methodType(void.class, MutableCallSite.class, ThreadContext.class, String.class, int.class, boolean.class));

        handle = handle.bindTo(site);
        handle = insertArguments(handle, 1, filename, line, oneshot != 0);
        site.setTarget(handle);

        return site;
    }

    public static void coverLineFallback(MutableCallSite site, ThreadContext context, String filename, int line, boolean oneshot) throws Throwable {
        IRRuntimeHelpers.updateCoverage(context, filename, line);

        if (oneshot) site.setTarget(Binder.from(void.class, ThreadContext.class).dropAll().nop());
    }
}
