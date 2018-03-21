package org.jruby.runtime;

import org.jruby.ir.IRScope;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.invoke.MethodHandle;

public class CompiledIRBlockBody extends IRBlockBody {
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
