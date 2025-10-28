package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.Signature;
import com.headius.invokebinder.SmartHandle;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.api.Convert;
import org.jruby.ir.targets.simple.NormalInvokeSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.util.JavaNameMangler;
import org.objectweb.asm.Handle;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.SwitchPoint;

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
public class RespondToSite extends MutableCallSite {
    private final String rawValue;
    private final String encoding;
    private final String file;
    private final int line;
    private RubySymbol methodCached;

    public RespondToSite(MethodType type, String rawValue, String encoding, String file, int line) {
        super(type);

        this.rawValue = rawValue;
        this.encoding = encoding;
        this.file = file;
        this.line = line;
    }

    public static final Handle RESPOND_TO_BOOTSTRAP = Bootstrap.getBootstrapHandle("respondToBootstrap", RespondToSite.class, sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, String.class, String.class, int.class));

    public static CallSite respondToBootstrap(MethodHandles.Lookup lookup, String name, MethodType methodType, String rawValue, String encoding, String file, int line) {
        RespondToSite respondToSite = new RespondToSite(methodType, rawValue, encoding, file, line);

        respondToSite.setTarget(Binder.from(methodType).prepend(respondToSite).invokeVirtualQuiet("respondToFallback"));

        return respondToSite;
    }

    public IRubyObject respondToFallback(ThreadContext context, IRubyObject self) throws Throwable {
        SwitchPoint switchPoint = (SwitchPoint) self.getMetaClass().getInvalidator().getData();
        CacheEntry entry = self.getMetaClass().searchWithCache("respond_to?");
        MethodHandle target = null;

        if (!InvokeSite.methodMissing(entry.method)) {
            if (entry.method.isBuiltin()) {
                // standard respond_to, check if method is naturally defined (not via respond_to_missing?)
                boolean respondsTo = self.getMetaClass().respondsToMethod(rawValue, true);
                if (respondsTo) {
                    // cache result; method table changes will invalidate this whole thing
                    target = Binder.from(type())
                            .dropAll()
                            .append(context.tru)
                            .identity();
                }
            }
        }

        if (target == null) {
            RubySymbol id = SymbolObjectSite.constructSymbolFromRaw(context, rawValue, encoding);
            target = Binder.from(type())
                    .append(IRubyObject.class, id)
                    .invoke(SelfInvokeSite.bootstrap(lookup(), "invokeFunctional:" + JavaNameMangler.mangleMethodName("respond_to?"), type().appendParameterTypes(IRubyObject.class), 0, 0, file, line).dynamicInvoker());
        }

        MethodHandle guardedtarget = typeCheck(target, Signature.from(type().returnType(), type().parameterArray(), "context", "self"), self, self.getMetaClass(), getTarget());
        guardedtarget = InvokeSite.switchPoint(switchPoint, getTarget(), guardedtarget);

        setTarget(guardedtarget);

        return (IRubyObject) target.invokeExact(context, self);
    }

    public IRubyObject respondToFallback(ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        SwitchPoint switchPoint = (SwitchPoint) self.getMetaClass().getInvalidator().getData();
        CacheEntry entry = self.getMetaClass().searchWithCache("respond_to?");
        MethodHandle target = null;

        if (!InvokeSite.methodMissing(entry.method, "respond_to?", CallType.NORMAL, caller)) {
            if (entry.method.isBuiltin()) {
                // standard respond_to, check if method is naturally defined (not via respond_to_missing?)
                boolean respondsTo = self.getMetaClass().respondsToMethod(rawValue, true);
                if (respondsTo) {
                    // cache result; method table changes will invalidate this whole thing
                    target = Binder.from(type())
                            .dropAll()
                            .append(context.tru)
                            .identity();
                }
            }
        }

        if (target == null) {
            RubySymbol id = SymbolObjectSite.constructSymbolFromRaw(context, rawValue, encoding);
            target = Binder.from(type())
                    .append(IRubyObject.class, id)
                    .invoke(NormalInvokeSite.bootstrap(lookup(), "invoke:" + JavaNameMangler.mangleMethodName("respond_to?"), type().appendParameterTypes(IRubyObject.class), 0, 0, file, line).dynamicInvoker());
        }

        MethodHandle guardedtarget = typeCheck(target, Signature.from(type().returnType(), type().parameterArray(), "context", "self"), self, self.getMetaClass(), getTarget());
        guardedtarget = InvokeSite.switchPoint(switchPoint, getTarget(), guardedtarget);

        setTarget(guardedtarget);

        return (IRubyObject) target.invokeExact(context, caller, self);
    }

    public static MethodHandle typeCheck(MethodHandle target, Signature signature, IRubyObject self, RubyModule testClass, MethodHandle fallback) {
        MethodHandle result;
        SmartHandle test = InvokeSite.testTarget(signature, self, testClass);

        result = MethodHandles.guardWithTest(test.handle(), target, fallback);
        return result;
    }
}
