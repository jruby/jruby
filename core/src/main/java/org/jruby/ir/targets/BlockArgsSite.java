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

public class BlockArgsSite extends MutableCallSite {

    public static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    public BlockArgsSite(MethodType type) {
        super(type);
    }
//
//    public static final Handle BOOTSTRAP = new Handle(
//            Opcodes.H_INVOKESTATIC, p(BlockArgsSite.class),
//            "bootstrap",
//            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class),
//            false);
//
//    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type) throws Throwable {
//        BlockArgsSite site = new BlockArgsSite(type);
//
//        MethodHandle handle;
//        switch (name) {
//            case "prepareNoBlockArgs":
//                handle = Binder.from(type)
//                        .prepend(BlockArgsSite.class, site)
//                        .invokeVirtual(lookup, name);
//                break;
//            default:
//                throw new RuntimeException("invalid block args type: " + name);
//        }
//
//        site.setTarget(handle);
//
//        return site;
//    }
//
//    public IRubyObject[] prepareNoBlockArgs(ThreadContext context, Block block, IRubyObject[] args) {
//        Binder prepBinder = Binder.from(type());
////                .filter(2, nullToArgs);
//
//        if (block.isLambda()) {
//            prepBinder = prepBinder.foldVoid(
//                    b -> b
//                            .dropFirst(2)
//                            .prepend(block.getSignature(), context.runtime)
//                            .invokeVirtualQuiet(LOOKUP, "checkArity"));
//        }
//
//        setTarget(prepBinder.dropFirst(2).identity());
//
//        return IRRuntimeHelpers.prepareNoBlockArgs(context, block, args);
//    }
//
//    private static final MethodHandle nullToArgs = Binder
//            .from(IRubyObject[].class, IRubyObject[].class)
//            .invokeStaticQuiet(LOOKUP, BlockArgsSite.class, "nullToArgs");
//
//    public static IRubyObject[] nullToArgs(IRubyObject[] args) {
//        return args == null ? IRubyObject.NULL_ARRAY : args;
//    }
}
