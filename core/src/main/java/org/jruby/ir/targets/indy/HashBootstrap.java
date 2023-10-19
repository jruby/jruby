package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class HashBootstrap {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final MethodHandle HASH_HANDLE =
            Binder
                    .from(RubyHash.class, ThreadContext.class, IRubyObject[].class)
                    .invokeStaticQuiet(LOOKUP, HashBootstrap.class, "hash");
    private static final MethodHandle KWARGS_HASH_HANDLE =
            Binder
                    .from(RubyHash.class, ThreadContext.class, RubyHash.class, IRubyObject[].class)
                    .invokeStaticQuiet(LOOKUP, HashBootstrap.class, "kwargsHash");
    public static final Handle HASH_H = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(HashBootstrap.class),
            "hash",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class),
            false);
    public static final Handle KWARGS_HASH_H = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(HashBootstrap.class),
            "kwargsHash",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class),
            false);
    // We use LOOKUP here to have a full-featured MethodHandles.Lookup, avoiding jruby/jruby#7911
    private static final MethodHandle RUNTIME_FROM_CONTEXT_HANDLE =
            Binder
                    .from(LOOKUP, Ruby.class, ThreadContext.class)
                    .getFieldQuiet("runtime");

    public static CallSite hash(MethodHandles.Lookup lookup, String name, MethodType type) {
        MethodHandle handle;

        int parameterCount = type.parameterCount();
        if (parameterCount == 1) {
            handle = Binder
                    .from(lookup, type)
                    .cast(type.changeReturnType(RubyHash.class))
                    .filter(0, RUNTIME_FROM_CONTEXT_HANDLE)
                    .invokeStaticQuiet(lookup, RubyHash.class, "newHash");
        } else if (!type.parameterType(parameterCount - 1).isArray()
                && (parameterCount - 1) / 2 <= Helpers.MAX_SPECIFIC_ARITY_HASH) {
            handle = Binder
                    .from(lookup, type)
                    .cast(type.changeReturnType(RubyHash.class))
                    .filter(0, RUNTIME_FROM_CONTEXT_HANDLE)
                    .invokeStaticQuiet(lookup, Helpers.class, "constructSmallHash");
        } else {
            handle = Binder
                    .from(lookup, type)
                    .collect(1, IRubyObject[].class)
                    .invoke(HASH_HANDLE);
        }

        return new ConstantCallSite(handle);
    }

    public static CallSite kwargsHash(MethodHandles.Lookup lookup, String name, MethodType type) {
        MethodHandle handle = Binder
                .from(lookup, type)
                .collect(2, IRubyObject[].class)
                .invoke(KWARGS_HASH_HANDLE);

        return new ConstantCallSite(handle);
    }

    public static RubyHash hash(ThreadContext context, IRubyObject[] pairs) {
        Ruby runtime = context.runtime;
        RubyHash hash = new RubyHash(runtime, pairs.length / 2 + 1);
        for (int i = 0; i < pairs.length;) {
            hash.fastASetCheckString(runtime, pairs[i++], pairs[i++]);
        }
        return hash;
    }

    public static RubyHash kwargsHash(ThreadContext context, RubyHash hash, IRubyObject[] pairs) {
        return IRRuntimeHelpers.dupKwargsHashAndPopulateFromArray(context, hash, pairs);
    }
}
