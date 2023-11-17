package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import org.jruby.RubyArray;
import org.jruby.ir.JIT;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static java.lang.invoke.MethodHandles.insertArguments;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class CheckArityBootstrap {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static final Handle CHECK_ARITY_SPECIFIC_ARGS = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(CheckArityBootstrap.class),
            "checkAritySpecificArgs",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class, int.class, int.class, int.class),
            false);
    public static final Handle CHECK_ARITY = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(CheckArityBootstrap.class),
            "checkArity",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class, int.class, int.class, int.class),
            false);
    public static final Handle CHECK_ARRAY_ARITY_BOOTSTRAP = new Handle(Opcodes.H_INVOKESTATIC, p(Helpers.class), "checkArrayArity", sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class, int.class, int.class), false);
    private static final MethodHandle CHECK_ARITY_HANDLE =
            Binder
                    .from(void.class, ThreadContext.class, StaticScope.class, Object[].class, Object.class, Block.class, int.class, int.class, boolean.class, int.class)
                    .invokeStaticQuiet(LOOKUP, CheckArityBootstrap.class, "checkArity");
    private static final MethodHandle CHECK_ARITY_SPECIFIC_ARGS_HANDLE =
            Binder
                    .from(void.class, ThreadContext.class, StaticScope.class, Object[].class, Block.class, int.class, int.class, boolean.class, int.class)
                    .invokeStaticQuiet(LOOKUP, CheckArityBootstrap.class, "checkAritySpecificArgs");
    private static final MethodHandle CHECK_ARRAY_ARITY =
            Binder
                    .from(void.class, ThreadContext.class, RubyArray.class, int.class, int.class, boolean.class)
                    .invokeStaticQuiet(LOOKUP, Helpers.class, "irCheckArgsArrayArity");

    @JIT
    public static CallSite checkArity(MethodHandles.Lookup lookup, String name, MethodType type, int req, int opt, int rest, int keyrest) {
        return new ConstantCallSite(insertArguments(CHECK_ARITY_HANDLE, 5, req, opt, rest == 0 ? false : true, keyrest));
    }

    @JIT
    public static CallSite checkAritySpecificArgs(MethodHandles.Lookup lookup, String name, MethodType type, int req, int opt, int rest, int keyrest) {
        return new ConstantCallSite(insertArguments(CHECK_ARITY_SPECIFIC_ARGS_HANDLE, 4, req, opt, rest == 0 ? false : true, keyrest));
    }

    @JIT
    public static void checkArity(ThreadContext context, StaticScope scope, Object[] args, Object keywords, Block block, int req, int opt, boolean rest, int keyrest) {
        IRRuntimeHelpers.checkArity(context, scope, args, keywords, req, opt, rest, keyrest, block);
    }

    @JIT
    public static void checkAritySpecificArgs(ThreadContext context, StaticScope scope, Object[] args, Block block, int req, int opt, boolean rest, int keyrest) {
        IRRuntimeHelpers.checkAritySpecificArgs(context, scope, args, req, opt, rest, keyrest, block);
    }

    public static CallSite checkArrayArity(MethodHandles.Lookup lookup, String name, MethodType methodType, int required, int opt, int rest) {
        return new ConstantCallSite(MethodHandles.insertArguments(CHECK_ARRAY_ARITY, 2, required, opt, (rest == 0 ? false : true)));
    }
}
