package org.jruby.runtime;

import org.jruby.EvalType;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.invoke.MethodHandle;

public class CompiledIRBlockBody extends IRBlockBody {
    protected final MethodHandle handle;
    protected boolean pushScope;
    protected boolean reuseParentScope;
    protected boolean usesKwargs;

    public CompiledIRBlockBody(MethodHandle handle, IRScope closure, long encodedSignature) {
        super(closure, Signature.decode(encodedSignature));
        this.handle = handle;
        // FIXME: duplicated from InterpreterContext
        this.reuseParentScope = closure.getFlags().contains(IRFlags.REUSE_PARENT_DYNSCOPE);
        this.pushScope = !closure.getFlags().contains(IRFlags.DYNSCOPE_ELIMINATED) && !this.reuseParentScope;
        this.usesKwargs = closure.receivesKeywordArgs();

        // Done in the interpreter (WrappedIRClosure) but we do it here
        closure.getStaticScope().determineModule();
    }

    @Override
    public ArgumentDescriptor[] getArgumentDescriptors() {
        return closure.getArgumentDescriptors();
    }

    @Override
    protected IRubyObject callDirect(ThreadContext context, Block block, IRubyObject[] args, Block blockArg) {
        throw new RuntimeException("callDirect not implemented in CompiledIRBlockBody. Implement me!");
    }

    @Override
    protected IRubyObject yieldDirect(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self) {
        throw new RuntimeException("yieldDirect not implemented in CompiledIRBlockBody. Implement me!");
    }

    protected IRubyObject commonYieldPath(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self, Block blockArg) {
        Binding binding = block.getBinding();
        Visibility oldVis = binding.getFrame().getVisibility();
        Frame prevFrame = context.preYieldNoScope(binding);

        // SSS FIXME: Maybe, we should allocate a NoVarsScope/DummyScope for for-loop bodies because the static-scope here
        // probably points to the parent scope? To be verified and fixed if necessary. There is no harm as it is now. It
        // is just wasteful allocation since the scope is not used at all.
        DynamicScope prevScope = binding.getDynamicScope();
        if (this.pushScope) {
            // SSS FIXME: for lambdas, this behavior is different
            // compared to what InterpretedIRBlockBody and MixedModeIRBlockBody do
            context.pushScope(DynamicScope.newDynamicScope(getStaticScope(), prevScope, this.evalType.get()));
        } else if (this.reuseParentScope) {
            // Reuse! We can avoid the push only if surrounding vars aren't referenced!
            context.pushScope(prevScope);
        }

        // SSS FIXME: Why is self null in non-binding-eval contexts?
        if (self == null || this.evalType.get() == EvalType.BINDING_EVAL) {
            self = useBindingSelf(binding);
        }

        // Clear evaltype now that it has been set on dyn-scope
        block.setEvalType(EvalType.NONE);

        if (usesKwargs) IRRuntimeHelpers.frobnicateKwargsArgument(context, getSignature().required(), args);

        try {
            return (IRubyObject)handle.invokeExact(context, getStaticScope(), self, args, blockArg, binding.getMethod(), block.type);
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        } finally {
            // IMPORTANT: Do not clear eval-type in case this is reused in bindings!
            // Ex: eval("...", foo.instance_eval { binding })
            // The dyn-scope used for binding needs to have its eval-type set to INSTANCE_EVAL
            binding.getFrame().setVisibility(oldVis);
            if (this.pushScope || this.reuseParentScope) {
                context.postYield(binding, prevFrame);
            } else {
                context.postYieldNoScope(prevFrame);
            }
        }
    }
}
