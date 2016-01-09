package org.jruby.ir.targets;

import com.headius.invokebinder.Binder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.CompiledIRBlockBody;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

/**
 * Created by headius on 1/8/16.
 */
public class YieldSite extends MutableCallSite {
    private final boolean unwrap;

    public YieldSite(MethodType type, boolean unwrap) {
        super(type);

        this.unwrap = unwrap;
    }

    public static final Handle BOOTSTRAP = new Handle(Opcodes.H_INVOKESTATIC, p(YieldSite.class), "bootstrap", sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class));

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, int unwrap) throws Throwable {
        YieldSite site = new YieldSite(type, unwrap == 1 ? true : false);

        MethodHandle handle = Binder.from(type)
                .prepend(YieldSite.class, site)
                .invokeVirtual(lookup, name);

        site.setTarget(handle);

        return site;
    }

    public IRubyObject yield(ThreadContext context, Block block, IRubyObject arg) throws Throwable {
        MethodHandle handle = getHandleForBlock(block);

        if (!unwrap && handle != null) {
            MethodHandle test, target, fallback;

            fallback = getTarget();

            target = Binder.from(IRubyObject.class, ThreadContext.class, Block.class, IRubyObject.class)
                    .foldVoid(SET_NORMAL)
                    .filter(2, VALUE_TO_ARRAY)
                    .insert(2, block.getBody().getStaticScope())
                    .insert(3, IRubyObject.class, null)
                    .append(Block.class, Block.NULL_BLOCK)
                    .append(block.getBinding().getMethod())
                    .append(block.type)
                    .invoke(handle);

            test = Binder.from(boolean.class, ThreadContext.class, Block.class, IRubyObject.class).permute(1).append(handle).invoke(TEST_BLOCK);

            MethodHandle guard = MethodHandles.guardWithTest(test, target, fallback);

            setTarget(guard);

            return (IRubyObject)target.invokeExact(context, block, arg);
        }

        context.setCurrentBlockType(Block.Type.NORMAL);

        return IRRuntimeHelpers.yield(context, block, arg, unwrap);
    }

    public IRubyObject yieldSpecific(ThreadContext context, Block block) throws Throwable {
        MethodHandle handle = getHandleForBlock(block);
        if (handle != null) {
            MethodHandle test, target, fallback;

            fallback = getTarget();

            target = Binder.from(IRubyObject.class, ThreadContext.class, Block.class)
                    .foldVoid(SET_NORMAL)
                    .append(block.getBody().getStaticScope())
                    .append(IRubyObject.class, null)
                    .append(IRubyObject[].class, null)
                    .append(Block.class, Block.NULL_BLOCK)
                    .append(block.getBinding().getMethod())
                    .append(block.type)
                    .invoke(handle);

            test = Binder.from(boolean.class, ThreadContext.class, Block.class).drop(0).append(handle).invoke(TEST_BLOCK);

            MethodHandle guard = MethodHandles.guardWithTest(test, target, fallback);

            setTarget(guard);

            return (IRubyObject)target.invokeExact(context, block);
        }

        context.setCurrentBlockType(Block.Type.NORMAL);

        return IRRuntimeHelpers.yieldSpecific(context, block);
    }

    public static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();
    private static final MethodHandle SET_NORMAL = Binder.from(void.class, ThreadContext.class, Block.class).drop(1).append(Block.Type.NORMAL).invokeVirtualQuiet(LOOKUP, "setCurrentBlockType");
//    private static final MethodHandle YIELD_SPECIFIC = Binder.from(IRubyObject.class, ThreadContext.class, Block.class).invokeStaticQuiet(LOOKUP, IRRuntimeHelpers.class, "yieldSpecific");
//    private static final MethodHandle YIELD = Binder.from(IRubyObject.class, ThreadContext.class, Block.class, IRubyObject.class, boolean.class).invokeStaticQuiet(LOOKUP, IRRuntimeHelpers.class, "yield");
    private static final MethodHandle YIELD_SPECIFIC_FALLBACK = Binder.from(IRubyObject.class, YieldSite.class, ThreadContext.class, Block.class).invokeVirtualQuiet(LOOKUP, "yieldSpecific");
    private static final MethodHandle YIELD_FALLBACK = Binder.from(IRubyObject.class, YieldSite.class, ThreadContext.class, Block.class, IRubyObject.class).invokeVirtualQuiet(LOOKUP, "yield");
    private static final MethodHandle TEST_BLOCK = Binder.from(boolean.class, Block.class, MethodHandle.class).invokeStaticQuiet(LOOKUP, YieldSite.class, "testBlock");
    private static final MethodHandle VALUE_TO_ARRAY = Binder.from(IRubyObject[].class, IRubyObject.class).invokeStaticQuiet(LOOKUP, IRRuntimeHelpers.class, "singleBlockArgToArray");

    public static boolean testBlock(Block block, MethodHandle handle) {
        return getHandleForBlock(block) == handle;
    }

    private static MethodHandle getHandleForBlock(Block block) {
        BlockBody body = block.getBody();
        if (block.getBody() instanceof CompiledIRBlockBody) {
            CompiledIRBlockBody compiledBody = (CompiledIRBlockBody) body;
            return compiledBody.getHandle();
        }
        return null;
    }
}
