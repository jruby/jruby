package org.jruby.ir.targets;

import com.headius.invokebinder.Binder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.CompiledIRBlockBody;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
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

    private static final Logger LOG = LoggerFactory.getLogger(YieldSite.class);

    public YieldSite(MethodType type, boolean unwrap) {
        super(type);

        this.unwrap = unwrap;
    }

    public static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(YieldSite.class),
            "bootstrap",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class),
            false);

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, int unwrap) throws Throwable {
        YieldSite site = new YieldSite(type, unwrap == 1 ? true : false);

        MethodHandle handle;
        switch (name) {
            case "yield":
            case "yieldSpecific":
                handle = Binder.from(type)
                        .prepend(YieldSite.class, site)
                        .invokeVirtual(lookup, name);
                break;
            case "yieldValues":
                handle = Binder.from(type)
                        .collect(2, IRubyObject[].class)
                        .prepend(YieldSite.class, site)
                        .invokeVirtual(lookup, name);
                break;
            default:
                throw new RuntimeException("invalid yield type: " + name);
        }

        site.setTarget(handle);

        return site;
    }

    public IRubyObject yield(ThreadContext context, Block block, IRubyObject arg) throws Throwable {
        if (Options.INVOKEDYNAMIC_YIELD.load()) {
            BlockBody body = block.getBody();
            MethodHandle target;

            if (block.getBody() instanceof CompiledIRBlockBody) {
                CompiledIRBlockBody compiledBody = (CompiledIRBlockBody) block.getBody();

                if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                    LOG.info("yield \tbound directly as yield:" + Bootstrap.logBlock(block));
                }

                target = unwrap ? compiledBody.getNormalYieldUnwrapHandle() : compiledBody.getNormalYieldHandle();
            } else {
                if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                    LOG.info("yield \tbound indirectly as yield:" + Bootstrap.logBlock(block));
                }

                target = Binder.from(type())
                        .append(unwrap)
                        .invokeStaticQuiet(MethodHandles.lookup(), IRRuntimeHelpers.class, "yield");
            }

            MethodHandle fallback = getTarget();
            MethodHandle test = body.getTestBlockBody();

            MethodHandle guard = MethodHandles.guardWithTest(test, target, fallback);

            setTarget(guard);

            return (IRubyObject) target.invokeExact(context, block, arg);
        }

        return IRRuntimeHelpers.yield(context, block, arg, unwrap);
    }

    public IRubyObject yieldSpecific(ThreadContext context, Block block) throws Throwable {
        if (Options.INVOKEDYNAMIC_YIELD.load()) {
            BlockBody body = block.getBody();
            MethodHandle target;

            if (block.getBody() instanceof CompiledIRBlockBody) {
                CompiledIRBlockBody compiledBody = (CompiledIRBlockBody) block.getBody();

                if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                    LOG.info("yield \tbound directly as yieldSpecific:" + Bootstrap.logBlock(block));
                }

                target = compiledBody.getNormalYieldSpecificHandle();
            } else {
                if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                    LOG.info("yield \tbound indirectly as yieldSpecific:" + Bootstrap.logBlock(block));
                }

                target = Binder.from(type())
                        .permute(1, 0)
                        .invokeVirtualQuiet(MethodHandles.lookup(), "yieldSpecific");
            }

            MethodHandle fallback = getTarget();
            MethodHandle test = body.getTestBlockBody();

            MethodHandle guard = MethodHandles.guardWithTest(test, target, fallback);

            setTarget(guard);

            return (IRubyObject) target.invokeExact(context, block);
        }

        return block.yieldSpecific(context);
    }

    public IRubyObject yieldValues(ThreadContext context, Block block, IRubyObject[] args) {
        if (Options.INVOKEDYNAMIC_YIELD.load()) {
            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                LOG.info("yield \tbound indirectly as yieldValues:" + Bootstrap.logBlock(block));
            }
        }

        return block.yieldValues(context, args);
    }
}
