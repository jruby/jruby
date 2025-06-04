package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.SmartBinder;
import com.headius.invokebinder.SmartHandle;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.ir.targets.simple.NormalInvokeSite;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.SwitchPoint;

import static org.jruby.api.Access.hashClass;
import static org.jruby.api.Create.dupString;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

/**
* Created by headius on 10/23/14.
*/
public class ArrayDerefInvokeSite extends NormalInvokeSite {
    public ArrayDerefInvokeSite(MethodType type, String file, int line) {
        super(type, "[]", false, 0, file, line);
    }

    public static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(ArrayDerefInvokeSite.class),
            "bootstrap",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, int.class),
            false);

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String file, int line) {
        InvokeSite site = new ArrayDerefInvokeSite(type, file, line);

        return InvokeSite.bootstrap(site, lookup);
    }

    public IRubyObject invoke(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);

        MethodHandle mh;

        SwitchPoint switchPoint = (SwitchPoint) selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (method.isBuiltin() &&
                testOptimizedHash(hashClass(context), self) &&
                !((RubyHash) self).isComparedByIdentity()) {
            // fast path since we know we're working with a normal hash and have a pre-frozen string
            mh = SmartBinder.from(signature)
                    .permute("self", "context", "arg0")
                    .cast(IRubyObject.class, RubyHash.class, ThreadContext.class, IRubyObject.class)
                    .invokeVirtual(MethodHandles.publicLookup(), "op_aref")
                    .handle();

            updateInvocationTarget(mh, self, selfClass, method, switchPoint);

            return ((RubyHash) self).op_aref(context, args[0]);
        } else {
            // slow path follows normal invoke logic with a strDup for the key

            // strdup for this call
            args[0] = dupString(context, (RubyString) args[0]);

            if (methodMissing(method, methodName, callType, caller)) {
                return callMethodMissing(entry, callType, context, self, selfClass, methodName, args, block);
            }

            mh = getHandle(context, self, entry);
            // strdup for future calls
            mh = MethodHandles.filterArguments(mh, 3, STRDUP_FILTER);

            updateInvocationTarget(mh, self, selfClass, entry.method, switchPoint);

            return method.call(context, self, entry.sourceModule, methodName, args, block);
        }
    }

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    @Override
    protected SmartHandle testTarget(IRubyObject self, RubyModule testClass) {
        if (self instanceof RubyHash && testOptimizedHash((RubyClass) testClass, self)) {
            return SmartBinder
                    .from(signature.changeReturn(boolean.class))
                    .permute("self")
                    .insert(0, "selfClass", RubyClass.class, testClass)
                    .invokeStaticQuiet(LOOKUP, ArrayDerefInvokeSite.class, "testOptimizedHash");
        }

        return super.testTarget(self, testClass);
    }

    public static boolean testOptimizedHash(RubyClass testClass, IRubyObject self) {
        return testClass == RubyBasicObject.getMetaClass(self) &&
                !((RubyHash) self).isComparedByIdentity();
    }

    /**
     * Failover version uses a monomorphic cache and DynamicMethod.call, as in non-indy.
     *
     * This assumes all ArrayDeref will be arity=1, which correlates to the code in IRBuilder.
     */
    public IRubyObject fail(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String name = methodName;
        CacheEntry entry = cache;

        // strdup for all calls
        arg0 = dupString(context, (RubyString) arg0);

        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, entry.sourceModule, name, arg0, block);
        }

        entry = selfClass.searchWithCache(name);

        if (methodMissing(entry.method, methodName, callType, caller)) {
            return callMethodMissing(entry, callType, context, self, selfClass, name, arg0, block);
        }

        cache = entry;

        return entry.method.call(context, self, entry.sourceModule, name, arg0, block);
    }

    private static final MethodHandle STRDUP_FILTER = Binder.from(IRubyObject.class, IRubyObject.class)
            .cast(RubyString.class, RubyString.class)
            .invokeVirtualQuiet(MethodHandles.publicLookup(), "strDup");
}