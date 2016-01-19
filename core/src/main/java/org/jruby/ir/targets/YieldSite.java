package org.jruby.ir.targets;

import com.headius.invokebinder.Binder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block;
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
        if (block.getBody() instanceof CompiledIRBlockBody) {
            CompiledIRBlockBody compiledBody = (CompiledIRBlockBody) block.getBody();

            MethodHandle target = unwrap ? compiledBody.getNormalYieldUnwrapHandle() : compiledBody.getNormalYieldHandle();
            MethodHandle fallback = getTarget();
            MethodHandle test = compiledBody.getTestBlockBody();

            MethodHandle guard = MethodHandles.guardWithTest(test, target, fallback);

            setTarget(guard);

            return (IRubyObject)target.invokeExact(context, block, arg);
        }

        context.setCurrentBlockType(Block.Type.NORMAL);

        return IRRuntimeHelpers.yield(context, block, arg, unwrap);
    }

    public IRubyObject yieldSpecific(ThreadContext context, Block block) throws Throwable {
        if (block.getBody() instanceof CompiledIRBlockBody) {
            CompiledIRBlockBody compiledBody = (CompiledIRBlockBody) block.getBody();

            MethodHandle target = compiledBody.getNormalYieldSpecificHandle();
            MethodHandle fallback = getTarget();
            MethodHandle test = compiledBody.getTestBlockBody();

            MethodHandle guard = MethodHandles.guardWithTest(test, target, fallback);

            setTarget(guard);

            return (IRubyObject)target.invokeExact(context, block);
        }

        context.setCurrentBlockType(Block.Type.NORMAL);

        return IRRuntimeHelpers.yieldSpecific(context, block);
    }
}
