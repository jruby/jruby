package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.objectweb.asm.Handle;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;
import static org.jruby.util.CodegenUtils.sig;

/**
 * Perform an optimized block_given? check without accessing a caller's frame.
 *
 * This logic checks if the target method is our built-in block_given? and uses fast logic in that case. All other calls
 * fall back on normal indy call logic.
 *
 * Note that if the target method is initially not built-in, or becomes a not built-in version later, we permanently
 * fall back on the invocation logic. No further checking is done and the site is optimized as a normal call from there.
 */
public class BlockGivenSite extends MutableCallSite {
    private final String file;
    private final int line;

    public BlockGivenSite(MethodType type, String file, int line) {
        super(type);

        this.file = file;
        this.line = line;
    }

    public static final Handle BLOCK_GIVEN_BOOTSTRAP = Bootstrap.getBootstrapHandle("blockGivenBootstrap", BlockGivenSite.class, sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, int.class));

    public static CallSite blockGivenBootstrap(MethodHandles.Lookup lookup, String name, MethodType methodType, String file, int line) {
        BlockGivenSite blockGivenSite = new BlockGivenSite(methodType, file, line);

        blockGivenSite.setTarget(Binder.from(methodType).prepend(blockGivenSite, name).invokeVirtualQuiet("blockGivenFallback"));

        return blockGivenSite;
    }

    public IRubyObject blockGivenFallback(String name, ThreadContext context, IRubyObject self, Block block) throws Throwable {
        String methodName = name.split(":")[1];

        CacheEntry entry = self.getMetaClass().searchWithCache(methodName);
        MethodHandle target;

        if (entry.method.isBuiltin()) {
            target = Binder.from(type())
                    .permute(0, 2)
                    .invokeStaticQuiet(BlockGivenSite.class, "blockGiven");
        } else {
            target = Binder.from(type())
                    .permute(0, 1)
                    .invoke(SelfInvokeSite.bootstrap(lookup(), name, methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class), 0, 0, file, line).dynamicInvoker());
        }

        setTarget(target);

        return (IRubyObject) target.invokeExact(context, self, block);
    }

    public static IRubyObject blockGiven(ThreadContext context, Block block) {
        return block.isGiven() ? context.tru : context.fals;
    }
}
