package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.DynamicScopeGenerator;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static java.lang.invoke.MethodType.methodType;
import static org.jruby.runtime.Helpers.arrayOf;
import static org.jruby.util.CodegenUtils.p;

public class HeapVariableBootstrap {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    public static final Handle GET_HEAP_LOCAL_OR_NIL_BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(HeapVariableBootstrap.class),
            "getHeapLocalOrNilBootstrap",
            Bootstrap.BOOTSTRAP_INT_INT_SIG,
            false);
    public static final Handle GET_HEAP_LOCAL_BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(HeapVariableBootstrap.class),
            "getHeapLocalBootstrap",
            Bootstrap.BOOTSTRAP_INT_INT_SIG,
            false);

    public static CallSite getHeapLocalBootstrap(MethodHandles.Lookup lookup, String name, MethodType type, int depth, int location) throws Throwable {
        // no null checking needed for method bodies
        MethodHandle getter;
        Binder binder = Binder
                .from(type);

        if (depth == 0) {
            if (location < DynamicScopeGenerator.SPECIALIZED_GETS.size()) {
                getter = binder.invokeVirtualQuiet(LOOKUP, DynamicScopeGenerator.SPECIALIZED_GETS.get(location));
            } else {
                getter = binder
                        .insert(1, location)
                        .invokeVirtualQuiet(LOOKUP, "getValueDepthZero");
            }
        } else {
            getter = binder
                    .insert(1, arrayOf(int.class, int.class), location, depth)
                    .invokeVirtualQuiet(LOOKUP, "getValue");
        }

        ConstantCallSite site = new ConstantCallSite(getter);

        return site;
    }

    public static CallSite getHeapLocalOrNilBootstrap(MethodHandles.Lookup lookup, String name, MethodType type, int depth, int location) throws Throwable {
        MethodHandle getter;
        Binder binder = Binder
                .from(type)
                .filter(1, LiteralValueBootstrap.contextValue(lookup, "nil", methodType(IRubyObject.class, ThreadContext.class)).dynamicInvoker());

        if (depth == 0) {
            if (location < DynamicScopeGenerator.SPECIALIZED_GETS_OR_NIL.size()) {
                getter = binder.invokeVirtualQuiet(LOOKUP, DynamicScopeGenerator.SPECIALIZED_GETS_OR_NIL.get(location));
            } else {
                getter = binder
                        .insert(1, location)
                        .invokeVirtualQuiet(LOOKUP, "getValueDepthZeroOrNil");
            }
        } else {
            getter = binder
                    .insert(1, arrayOf(int.class, int.class), location, depth)
                    .invokeVirtualQuiet(LOOKUP, "getValueOrNil");
        }

        ConstantCallSite site = new ConstantCallSite(getter);

        return site;
    }
}
