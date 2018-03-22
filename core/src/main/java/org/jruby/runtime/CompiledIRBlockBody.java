package org.jruby.runtime;

import org.jruby.ir.IRScope;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.invoke.MethodHandle;

import static org.jruby.runtime.Helpers.arrayOf;

public class CompiledIRBlockBody extends AbstractIRBlockBody {
    protected final MethodHandle handle;

    public CompiledIRBlockBody(MethodHandle handle, IRScope closure, long encodedSignature) {
        super(closure, Signature.decode(encodedSignature));
        // evalType copied (shared) on MixedModeIRBlockBody#completeBuild
        this.handle = handle;
        // Done in the interpreter (WrappedIRClosure) but we do it here
        closure.getStaticScope().determineModule();
    }

    @Override
    public ArgumentDescriptor[] getArgumentDescriptors() {
        return closure.getArgumentDescriptors();
    }

    @Override
    public boolean canInvokeDirect() {
        return true;
    }

    public MethodHandle getHandle() {
        return handle;
    }

    @Override
    public IRubyObject yield(ThreadContext context, Block block, IRubyObject value, IRubyObject self, Block blockArg) {
        return invokeYieldDirect(context, block, arrayOf(value), blockArg, self);
    }

    @Override
    public IRubyObject yield(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self, Block blockArg) {
        return invokeYieldDirect(context, block, args, blockArg, self);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0, Block blockArg) {
        return invokeCallDirect(context, block, arrayOf(arg0), blockArg, null);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, Block blockArg) {
        return invokeCallDirect(context, block, arrayOf(arg0, arg1), blockArg, null);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block blockArg) {
        return invokeCallDirect(context, block, arrayOf(arg0, arg1, arg2), blockArg, null);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject[] args, Block blockArg) {
        return invokeCallDirect(context, block, args, blockArg, null);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block) {
        return invokeYieldDirect(context, block, null, Block.NULL_BLOCK, null);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0) {
        return invokeYieldSpecificDirect(context, block, arg0, Block.NULL_BLOCK, null);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1) {
        return invokeYieldSpecificDirect(context, block, arg0, arg1, Block.NULL_BLOCK, null);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return invokeYieldSpecificDirect(context, block, arg0, arg1, arg2, Block.NULL_BLOCK, null);
    }

    @Override
    protected IRubyObject invokeCallDirect(ThreadContext context, Block block, IRubyObject[] args, Block blockArg, IRubyObject self) {
        context.setCurrentBlockType(Block.Type.PROC);
        try {
            return (IRubyObject)handle.invokeExact(context, block, getStaticScope(), (IRubyObject)null, args, blockArg, block.getBinding().getMethod());
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    @Override
    protected IRubyObject invokeYieldDirect(ThreadContext context, Block block, IRubyObject[] args, Block blockArg, IRubyObject self) {
        context.setCurrentBlockType(Block.Type.NORMAL);
        try {
            return (IRubyObject)handle.invokeExact(context, block, getStaticScope(), self, args, Block.NULL_BLOCK, block.getBinding().getMethod());
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    @Override
    protected IRubyObject invoke(ThreadContext context, Block block, IRubyObject[] args, Block blockArg, IRubyObject self) {
        throw new UnsupportedOperationException("invoke not implemented");
    }

}
