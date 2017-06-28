package org.jruby.ir.targets;

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.SmartBinder;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyString;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.util.JavaNameMangler;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.SwitchPoint;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

/**
* Created by headius on 10/23/14.
*/
public class ArrayDerefInvokeSite extends NormalInvokeSite {
    public ArrayDerefInvokeSite(MethodType type, String file, int line) {
        super(type, "[]", false, file, line);
    }

    public static final Handle BOOTSTRAP = new Handle(Opcodes.H_INVOKESTATIC, p(ArrayDerefInvokeSite.class), "bootstrap", sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, int.class));

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String file, int line) {
        InvokeSite site = new ArrayDerefInvokeSite(type, file, line);

        return InvokeSite.bootstrap(site, lookup);
    }

    public IRubyObject invoke(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);

        MethodHandle mh;

        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (method.isBuiltin() && selfClass == context.runtime.getHash()) {
            // fast path since we know we're working with a normal hash and have a pre-frozen string
            mh = SmartBinder.from(signature)
                    .permute("self", "context", "arg0")
                    .cast(IRubyObject.class, RubyHash.class, ThreadContext.class, IRubyObject.class)
                    .invokeVirtual(MethodHandles.publicLookup(), "op_aref")
                    .handle();

            SwitchPoint switchPoint = (SwitchPoint) selfClass.getInvalidator().getData();

            updateInvocationTarget(mh, self, selfClass, method, switchPoint);

            return ((RubyHash) self).op_aref(context, args[0]);
        } else {
            // slow path follows normal invoke logic with a strDup for the key
            SwitchPoint switchPoint = (SwitchPoint) selfClass.getInvalidator().getData();

            // strdup for this call
            args[0] = ((RubyString) args[0]).strDup(context.runtime);

            if (methodMissing(entry, caller)) {
                return callMethodMissing(entry, callType, context, self, methodName, args, block);
            }

            mh = getHandle(self, selfClass, method);
            // strdup for future calls
            mh = MethodHandles.filterArguments(mh, 3, STRDUP_FILTER);

            updateInvocationTarget(mh, self, selfClass, entry.method, switchPoint);

            return method.call(context, self, selfClass, methodName, args, block);
        }
    }

    /**
     * Failover version uses a monomorphic cache and DynamicMethod.call, as in non-indy.
     */
    public IRubyObject fail(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String name = methodName;
        CacheEntry entry = cache;

        // strdup for all calls
        args[0] = ((RubyString) args[0]).strDup(context.runtime);

        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name, args, block);
        }

        entry = selfClass.searchWithCache(name);

        if (methodMissing(entry, caller)) {
            return callMethodMissing(entry, callType, context, self, name, args, block);
        }

        cache = entry;

        return entry.method.call(context, self, selfClass, name, args, block);
    }

    private static final MethodHandle STRDUP_FILTER = Binder.from(IRubyObject.class, IRubyObject.class)
            .cast(RubyString.class, RubyString.class)
            .invokeVirtualQuiet(MethodHandles.publicLookup(), "strDup");
}