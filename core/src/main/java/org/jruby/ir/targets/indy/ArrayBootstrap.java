package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import org.jruby.RubyArray;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.specialized.RubyArraySpecialized;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class ArrayBootstrap {
    public static final Handle ARRAY_H = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(ArrayBootstrap.class),
            "array",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class),
            false);
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final MethodHandle ARRAY_HANDLE =
            Binder
                    .from(RubyArray.class, ThreadContext.class, IRubyObject[].class)
                    .invokeStaticQuiet(LOOKUP, ArrayBootstrap.class, "array");

    public static CallSite array(MethodHandles.Lookup lookup, String name, MethodType type) {
        MethodHandle handle = Binder
                .from(type)
                .collect(1, IRubyObject[].class)
                .invoke(ARRAY_HANDLE);

        return new ConstantCallSite(handle);
    }

    public static RubyArray array(ThreadContext context, IRubyObject[] ary) {
        assert ary.length > RubyArraySpecialized.MAX_PACKED_SIZE;
        // array() only dispatches here if ^^ holds
        return RubyArray.newArrayNoCopy(context.runtime, ary);
    }
}
