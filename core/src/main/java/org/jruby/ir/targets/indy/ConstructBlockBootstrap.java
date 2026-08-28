package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.CompiledIRBlockBody;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Frame;
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

public class ConstructBlockBootstrap {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final Binder BINDING_MAKER_BINDER = Binder.from(Binding.class, ThreadContext.class, IRubyObject.class, DynamicScope.class);
    private static final MethodHandle SELF_BINDING = BINDING_MAKER_BINDER.invokeStaticQuiet(LOOKUP, ConstructBlockBootstrap.class, "selfBinding");
    private static final MethodHandle SCOPE_BINDING = BINDING_MAKER_BINDER.invokeStaticQuiet(LOOKUP, ConstructBlockBootstrap.class, "scopeBinding");
    private static final MethodHandle FRAME_BINDING = BINDING_MAKER_BINDER.invokeStaticQuiet(LOOKUP, ConstructBlockBootstrap.class, "frameBinding");
    private static final MethodHandle FRAME_SCOPE_BINDING = BINDING_MAKER_BINDER.invokeStaticQuiet(LOOKUP, IRRuntimeHelpers.class, "newFrameScopeBinding");
    private static final MethodHandle CONSTRUCT_BLOCK = Binder.from(Block.class, Binding.class, CompiledIRBlockBody.class).invokeStaticQuiet(LOOKUP, ConstructBlockBootstrap.class, "constructBlock");
    public static final Handle PREPARE_BLOCK_BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(ConstructBlockBootstrap.class),
            "prepareBlock",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class,
                    MethodType.class, MethodHandle.class, MethodHandle.class, MethodHandle.class, MethodHandle.class, String.class, long.class,
                    String.class, int.class, String.class),
            false);

    public static CallSite prepareBlock(MethodHandles.Lookup lookup, String name, MethodType type,
                                        MethodHandle bodyHandle, MethodHandle scopeHandle, MethodHandle setScopeHandle, MethodHandle parentHandle, String scopeDescriptor, long encodedSignature,
                                        String file, int line, String encodedArgumentDescriptors
                                        ) throws Throwable {
        StaticScope staticScope = (StaticScope) scopeHandle.invokeExact();

        if (staticScope == null) {
            staticScope = Helpers.restoreScope(scopeDescriptor, (StaticScope) parentHandle.invokeExact());
            setScopeHandle.invokeExact(staticScope);
        }

        CompiledIRBlockBody body = new CompiledIRBlockBody(bodyHandle, staticScope, file, line, encodedArgumentDescriptors, encodedSignature);

        Binder binder = Binder.from(type);

        binder = binder.fold(FRAME_SCOPE_BINDING);

        // This optimization can't happen until we can see into the method we're calling to know if it reifies the block
        if (false) {
            /*
            if (needsBinding) {
                if (needsFrame) {
            FullInterpreterContext fic = scope.getExecutionContext();
            if (fic.needsBinding()) {
                if (fic.needsFrame()) {
                    binder = binder.fold(FRAME_SCOPE_BINDING);
                } else {
                    binder = binder.fold(SCOPE_BINDING);
                }
            } else {
                if (needsFrame) {
                    binder = binder.fold(FRAME_BINDING);
                } else {
                    binder = binder.fold(SELF_BINDING);
                }
            }*/
        }

        MethodHandle blockMaker = binder.drop(1, 3)
                .append(body)
                .invoke(CONSTRUCT_BLOCK);

        return new ConstantCallSite(blockMaker);
    }

    public static Binding frameBinding(ThreadContext context, IRubyObject self, DynamicScope scope) {
        Frame frame = context.getCurrentFrame().capture();
        return new Binding(self, frame, frame.getVisibility());
    }

    public static Binding scopeBinding(ThreadContext context, IRubyObject self, DynamicScope scope) {
        return new Binding(self, scope);
    }

    public static Binding selfBinding(ThreadContext context, IRubyObject self, DynamicScope scope) {
        return new Binding(self);
    }

    public static Block constructBlock(Binding binding, CompiledIRBlockBody body) throws Throwable {
        return new Block(body, binding);
    }
}
