package org.jruby.runtime;

import org.jruby.EvalType;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.invoke.MethodHandle;

public class CompiledIRBlockBody extends IRBlockBody {
    protected final MethodHandle handle;

    public CompiledIRBlockBody(MethodHandle handle, IRScope closure, long encodedSignature) {
        super(closure, Signature.decode(encodedSignature));
        this.handle = handle;

        // Done in the interpreter (WrappedIRClosure) but we do it here
        closure.getStaticScope().determineModule();
    }

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
}
