package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import org.jruby.RubySymbol;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.objectweb.asm.Handle;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;
import static org.jruby.util.CodegenUtils.sig;

/**
 * Perform an optimized __method__ or __callee__ invocation without accessing a caller's frame.
 *
 * This logic checks if the target method is our built-in version and uses fast logic in that case. All other calls
 * fall back on normal indy call logic. Only the built-in versions can actually access the caller's frame, so we can
 * omit the frame altogether if we only use the specialized site.
 *
 * Note that if the target method is initially not built-in, or becomes a not built-in version later, we permanently
 * fall back on the invocation logic. No further checking is done and the site is optimized as a normal call from there.
 */
public class FrameNameSite extends MutableCallSite {
    private final String file;
    private final int line;
    private RubySymbol methodCached;

    public FrameNameSite(MethodType type, String file, int line) {
        super(type);

        this.file = file;
        this.line = line;
    }

    public static final Handle FRAME_NAME_BOOTSTRAP = Bootstrap.getBootstrapHandle("frameNameBootstrap", FrameNameSite.class, sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, int.class));

    public static CallSite frameNameBootstrap(MethodHandles.Lookup lookup, String name, MethodType methodType, String file, int line) {
        FrameNameSite frameNameSite = new FrameNameSite(methodType, file, line);

        frameNameSite.setTarget(Binder.from(methodType).prepend(frameNameSite, name).invokeVirtualQuiet("frameNameFallback"));

        return frameNameSite;
    }

    public IRubyObject frameNameFallback(String name, ThreadContext context, IRubyObject self, String frameName) throws Throwable {
        String methodName = name.split(":")[1];

        CacheEntry entry = self.getMetaClass().searchWithCache(methodName);
        MethodHandle target;

        if (entry.method.isBuiltin()) {
            target = Binder.from(type())
                    .permute(2)
                    .append(context.runtime.getSymbolTable())
                    .prepend(this)
                    .invokeVirtualQuiet(methodName);
        } else {
            target = Binder.from(type())
                    .permute(0, 1)
                    .invoke(SelfInvokeSite.bootstrap(lookup(), name, methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class), 0, 0, file, line).dynamicInvoker());
        }

        setTarget(target);

        return (IRubyObject) target.invokeExact(context, self, frameName);
    }

    public IRubyObject __callee__(String frameName, RubySymbol.SymbolTable symbolTable) {
        if (frameName.charAt(0) != '\0') {
            return getSimpleName(frameName, symbolTable);
        }
        return symbolTable.getCalleeSymbolFromCompound(frameName);
    }

    public IRubyObject __method__(String frameName, RubySymbol.SymbolTable symbolTable) {
        if (frameName.charAt(0) != '\0') {
            return getSimpleName(frameName, symbolTable);
        }
        return symbolTable.getMethodSymbolFromCompound(frameName);
    }

    private RubySymbol getSimpleName(String frameName, RubySymbol.SymbolTable symbolTable) {
        // simple name, use cached version
        RubySymbol simpleName = methodCached;
        if (simpleName == null || !frameName.equals(simpleName.idString())) {
            // cache the name symbol
            return methodCached = symbolTable.getSymbol(frameName, true);
        }

        return simpleName;
    }
}
