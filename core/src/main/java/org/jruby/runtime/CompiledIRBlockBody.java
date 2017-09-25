package org.jruby.runtime;

import com.headius.invokebinder.Binder;
import org.jruby.ir.IRScope;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public class CompiledIRBlockBody extends IRBlockBody {
    protected final MethodHandle handle;

    public CompiledIRBlockBody(MethodHandle handle, IRScope closure, long encodedSignature) {
        super(closure, Signature.decode(encodedSignature));
        // evalType copied (shared) on MixedModeIRBlockBody#completeBuild
        this.handle = handle;
        // Done in the interpreter (WrappedIRClosure) but we do it here
        closure.getStaticScope().determineModule();
    }

    private static final MethodHandle FOLD_METHOD1 = Binder.from(String.class, ThreadContext.class, Block.class).invokeStaticQuiet(MethodHandles.lookup(), CompiledIRBlockBody.class, "foldMethod");
    private static String foldMethod(ThreadContext context, Block block) {
        return block.getBinding().getMethod();
    }

    private static final MethodHandle FOLD_TYPE1 = Binder.from(Block.Type.class, String.class, ThreadContext.class, Block.class).invokeStaticQuiet(MethodHandles.lookup(), CompiledIRBlockBody.class, "foldType");
    private static Block.Type foldType(String name, ThreadContext context, Block block) {
        return block.type;
    }

    private static final MethodHandle FOLD_METHOD2 = Binder.from(String.class, ThreadContext.class, Block.class, IRubyObject.class).invokeStaticQuiet(MethodHandles.lookup(), CompiledIRBlockBody.class, "foldMethod");
    private static String foldMethod(ThreadContext context, Block block, IRubyObject arg) {
        return block.getBinding().getMethod();
    }

    private static final MethodHandle FOLD_TYPE2 = Binder.from(Block.Type.class, String.class, ThreadContext.class, Block.class, IRubyObject.class).invokeStaticQuiet(MethodHandles.lookup(), CompiledIRBlockBody.class, "foldType");
    private static Block.Type foldType(String name, ThreadContext context, Block block, IRubyObject arg) {
        return block.type;
    }

    private static final MethodHandle SET_NORMAL = Binder.from(void.class, ThreadContext.class, Block.class).drop(1).append(Block.Type.NORMAL).invokeVirtualQuiet(MethodHandles.lookup(), "setCurrentBlockType");

    private static final MethodHandle VALUE_TO_ARRAY = Binder.from(IRubyObject[].class, IRubyObject.class).invokeStaticQuiet(MethodHandles.lookup(), IRRuntimeHelpers.class, "singleBlockArgToArray");

    private static final MethodHandle WRAP_VALUE = Binder.from(IRubyObject[].class, IRubyObject.class).invokeStaticQuiet(MethodHandles.lookup(), CompiledIRBlockBody.class, "wrapValue");

    private static IRubyObject[] wrapValue(IRubyObject value) { return new IRubyObject[] {value}; }

    @Override
    public ArgumentDescriptor[] getArgumentDescriptors() {
        return closure.getArgumentDescriptors();
    }

    @Override
    public boolean canCallDirect() {
        return true;
    }

    public MethodHandle getHandle() {
        return handle;
    }

    protected volatile MethodHandle normalYieldSpecificHandle;
    protected volatile MethodHandle normalYieldHandle;
    protected volatile MethodHandle normalYieldUnwrapHandle;
    protected volatile MethodHandle yieldTwoValuesHandle;
    protected volatile MethodHandle yieldThreeValuesHandle;

    public MethodHandle getNormalYieldSpecificHandle() {
        MethodHandle normalYieldSpecificHandle = this.normalYieldSpecificHandle;
        if (normalYieldSpecificHandle != null) return normalYieldSpecificHandle;

        return this.normalYieldSpecificHandle = Binder.from(IRubyObject.class, ThreadContext.class, Block.class)
                .foldVoid(SET_NORMAL)
                .fold(FOLD_METHOD1)
                .fold(FOLD_TYPE1)
                .append(new Class[] {StaticScope.class, IRubyObject.class, IRubyObject[].class, Block.class},
                        getStaticScope(), null, null, Block.NULL_BLOCK)
                .permute(2, 3, 4, 5, 6, 7, 1, 0)
                .invoke(handle);
    }

    public MethodHandle getNormalYieldHandle() {
        MethodHandle normalYieldHandle = this.normalYieldHandle;
        if (normalYieldHandle != null) return normalYieldHandle;

        return this.normalYieldHandle = Binder.from(IRubyObject.class, ThreadContext.class, Block.class, IRubyObject.class)
                .foldVoid(SET_NORMAL)
                .fold(FOLD_METHOD2)
                .fold(FOLD_TYPE2)
                .filter(4, WRAP_VALUE)
                .insert(4, new Class[]{StaticScope.class, IRubyObject.class}, getStaticScope(), null)
                .append(Block.class, Block.NULL_BLOCK)
                .permute(2, 3, 4, 5, 6, 7, 1, 0)
                .invoke(handle);
    }

    public MethodHandle getNormalYieldUnwrapHandle() {
        MethodHandle normalYieldUnwrapHandle = this.normalYieldUnwrapHandle;
        if (normalYieldUnwrapHandle != null) return normalYieldUnwrapHandle;

        return this.normalYieldUnwrapHandle = Binder.from(IRubyObject.class, ThreadContext.class, Block.class, IRubyObject.class)
                .foldVoid(SET_NORMAL)
                .fold(FOLD_METHOD2)
                .fold(FOLD_TYPE2)
                .filter(4, VALUE_TO_ARRAY)
                .insert(4, new Class[] {StaticScope.class, IRubyObject.class}, getStaticScope(), null)
                .append(Block.class, Block.NULL_BLOCK)
                .permute(2, 3, 4, 5, 6, 7, 1, 0)
                .invoke(handle);
    }

    public MethodHandle getYieldTwoValuesHandle() {
        MethodHandle yieldTwoValuesHandle = this.yieldTwoValuesHandle;
        if (yieldTwoValuesHandle != null) return yieldTwoValuesHandle;

        return this.yieldTwoValuesHandle = Binder.from(IRubyObject.class, ThreadContext.class, Block.class, IRubyObject.class, IRubyObject.class)
                .foldVoid(SET_NORMAL)
                .fold(FOLD_METHOD1)
                .fold(FOLD_TYPE1)
                .collect(5, IRubyObject[].class)
                .insert(5, new Class[] {StaticScope.class, IRubyObject.class},
                        getStaticScope(), null)
                .append(new Class[] {Block.class}, Block.NULL_BLOCK)
                .permute(2, 3, 4, 5, 6, 7, 1, 0)
                .invoke(handle);
    }

    public MethodHandle getYieldThreeValuesHandle() {
        MethodHandle yieldThreeValuesHandle = this.yieldThreeValuesHandle;
        if (yieldThreeValuesHandle != null) return yieldThreeValuesHandle;

        return this.yieldThreeValuesHandle = Binder.from(IRubyObject.class, ThreadContext.class, Block.class, IRubyObject.class, IRubyObject.class, IRubyObject.class)
                .foldVoid(SET_NORMAL)
                .fold(FOLD_METHOD1)
                .fold(FOLD_TYPE1)
                .collect(5, IRubyObject[].class)
                .insert(5, new Class[] {StaticScope.class, IRubyObject.class},
                        getStaticScope(), null)
                .append(new Class[] {Block.class}, Block.NULL_BLOCK)
                .permute(2, 3, 4, 5, 6, 7, 1, 0)
                .invoke(handle);
    }

    @Override
    protected IRubyObject callDirect(ThreadContext context, Block block, IRubyObject[] args, Block blockArg) {
        context.setCurrentBlockType(Block.Type.PROC);
        try {
            return (IRubyObject)handle.invokeExact(context, block, getStaticScope(), (IRubyObject)null, args, blockArg, block.getBinding().getMethod(), block.type);
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    @Override
    protected IRubyObject yieldDirect(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self) {
        context.setCurrentBlockType(Block.Type.NORMAL);
        try {
            return (IRubyObject)handle.invokeExact(context, block, getStaticScope(), self, args, Block.NULL_BLOCK, block.getBinding().getMethod(), block.type);
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    @Override
    protected IRubyObject commonYieldPath(ThreadContext context, Block block, Block.Type type, IRubyObject[] args, IRubyObject self, Block blockArg) {
        throw new UnsupportedOperationException("commonYieldPath not implemented");
    }

}
