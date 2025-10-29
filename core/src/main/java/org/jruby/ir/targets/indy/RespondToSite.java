package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.Signature;
import com.headius.invokebinder.SmartHandle;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
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
    private final Signature signature;

    public RespondToSite(MethodType type, String rawValue, String encoding, String file, int line) {
        super(type);

        this.rawValue = rawValue;
        this.encoding = encoding;
        this.file = file;
        this.line = line;

        signature = switch (type.parameterCount()) {
            case 2 -> Signature.from(type().returnType(), type().parameterArray(), "context", "self");
            case 3 -> Signature.from(type().returnType(), type().parameterArray(), "context", "caller", "self");
            default -> throw new IllegalArgumentException("invalid respond_to site: " + type);
        };
    }

    public static final Handle RESPOND_TO_BOOTSTRAP = Bootstrap.getBootstrapHandle("respondToBootstrap", RespondToSite.class, sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, String.class, String.class, int.class));

    public static CallSite respondToBootstrap(MethodHandles.Lookup lookup, String name, MethodType methodType, String rawValue, String encoding, String file, int line) {
        RespondToSite respondToSite = new RespondToSite(methodType, rawValue, encoding, file, line);

        respondToSite.setTarget(Binder.from(methodType).prepend(respondToSite).invokeVirtualQuiet("respondToFallback"));

        return respondToSite;
    }

    public IRubyObject respondToFallback(ThreadContext context, IRubyObject self) throws Throwable {
        RubyClass selfClass = self.getMetaClass();
        SwitchPoint switchPoint = (SwitchPoint) selfClass.getInvalidator().getData();

        MethodHandle target = respondToTarget(context, self, selfClass);

        guardAndSetTarget(self, target, selfClass, switchPoint);

        return (IRubyObject) target.invokeExact(context, self);
    }

    public IRubyObject respondToFallback(ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        RubyClass selfClass = self.getMetaClass();
        SwitchPoint switchPoint = (SwitchPoint) selfClass.getInvalidator().getData();

        MethodHandle target = respondToTarget(context, caller, selfClass);

        guardAndSetTarget(self, target, selfClass, switchPoint);

        return (IRubyObject) target.invokeExact(context, caller, self);
    }

    private void guardAndSetTarget(IRubyObject self, MethodHandle target, RubyClass selfClass, SwitchPoint switchPoint) {
        MethodHandle guardedtarget = typeCheck(target, signature, self, selfClass, getTarget());
        guardedtarget = InvokeSite.switchPoint(switchPoint, getTarget(), guardedtarget);

        setTarget(guardedtarget);
    }

    private MethodHandle respondToTarget(ThreadContext context, IRubyObject caller, RubyClass selfClass) {
        /*
        We can cache the respond_to? result iff:

        * it is a literal method name (determined at compile time)
        * AND respond_to? is defined AND builtin AND returns true for the method (true result cached)
        * OR respond_to? is undefined and respond_to_missing? is undefined (false result cached)

        We cannot cache a result if:

        * respond_to? is not the built-in version
        * respond_to? is the built-in version but returns false and respond_to_missing? is defined
        * respond_to? is undefined and respond_to_missing? is defined
         */
        CacheEntry entry = selfClass.searchWithCache("respond_to?");
        if (respondToDefined(caller, entry)) {
            if (respondToBuiltin(entry)) {
                if (respondsToMethod(selfClass)) {
                    // defined, built-in, and returns true = true result
                    return trueResult(context);
                }

                // defined, built-in, and returns false = check respond_to_missing?
                return rsmOrFalse(context, caller, selfClass);
            }

            // defined but not built-in = false result
            return defaultRespondTo(context);
        }

        // respond_to? undefined = check respond_to_missing?
        return rsmOrFalse(context, caller, selfClass);
    }

    private boolean respondsToMethod(RubyClass selfClass) {
        return selfClass.respondsToMethod(rawValue, true);
    }

    private static boolean respondToBuiltin(CacheEntry entry) {
        return entry.method.isBuiltin();
    }

    private MethodHandle rsmOrFalse(ThreadContext context, IRubyObject caller, RubyClass selfClass) {
        if (respondToMissingDefined(caller, selfClass)) {
            return defaultRespondTo(context);
        }

        return falseResult(context);

    }

    private MethodHandle falseResult(ThreadContext context) {
        Binder binder = Binder.from(type());
        MethodHandle target;
        target = binder
                .dropAll()
                .append(context.fals)
                .identity();
        return target;
    }

    private MethodHandle trueResult(ThreadContext context) {
        MethodHandle target;
        target = Binder.from(type())
                .dropAll()
                .append(context.tru)
                .identity();
        return target;
    }

    private MethodHandle defaultRespondTo(ThreadContext context) {
        Binder binder = Binder.from(type());
        MethodHandle target;
        RubySymbol id = SymbolObjectSite.constructSymbolFromRaw(context, rawValue, encoding);
        target = binder
                .append(IRubyObject.class, id)
                .invoke(NormalInvokeSite.bootstrap(lookup(), "invoke:" + JavaNameMangler.mangleMethodName("respond_to?"), type().appendParameterTypes(IRubyObject.class), 0, 0, file, line).dynamicInvoker());
        return target;
    }

    private static boolean respondToDefined(IRubyObject caller, CacheEntry entry) {
        if (InvokeSite.methodMissing(entry.method, "respond_to?", CallType.NORMAL, caller)) {
            return false;
        }

        return true;
    }

    private static boolean respondToMissingDefined(IRubyObject caller, RubyClass selfClass) {
        CacheEntry entry = selfClass.searchWithCache("respond_to_missing?");

        if (InvokeSite.methodMissing(entry.method, "respond_to_missing?", CallType.NORMAL, caller)) {
            return false;
        }

        return true;
    }

    public static MethodHandle typeCheck(MethodHandle target, Signature signature, IRubyObject self, RubyModule testClass, MethodHandle fallback) {
        SmartHandle test = InvokeSite.testTarget(signature, self, testClass);

        return MethodHandles.guardWithTest(test.handle(), target, fallback);
    }
}
