package org.jruby.runtime;

import org.jruby.EvalType;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block.Type;
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

    protected IRubyObject commonYieldPath(ThreadContext context, IRubyObject[] args, IRubyObject self, Binding binding, Type type, Block block) {

        // SSS: Important!  Use getStaticScope() to use a copy of the static-scope stored in the block-body.
        // Do not use 'closure.getStaticScope()' -- that returns the original copy of the static scope.
        // This matters because blocks created for Thread bodies modify the static-scope field of the block-body
        // that records additional state about the block body.
        //
        // FIXME: Rather than modify static-scope, it seems we ought to set a field in block-body which is then
        // used to tell dynamic-scope that it is a dynamic scope for a thread body.  Anyway, to be revisited later!
        Visibility oldVis = binding.getFrame().getVisibility();
        Frame prevFrame = context.preYieldNoScope(binding);

        // SSS FIXME: Why is self null in non-binding-eval contexts?
        if (self == null || this.evalType.get() == EvalType.BINDING_EVAL) {
            self = useBindingSelf(binding);
        }

        // SSS FIXME: Maybe, we should allocate a NoVarsScope/DummyScope for for-loop bodies because the static-scope here
        // probably points to the parent scope? To be verified and fixed if necessary. There is no harm as it is now. It
        // is just wasteful allocation since the scope is not used at all.

        // Pass on eval state info to the dynamic scope and clear it on the block-body
        DynamicScope prevScope = binding.getDynamicScope();
        if (this.pushScope) {
            context.pushScope(DynamicScope.newDynamicScope(getStaticScope(), prevScope, this.evalType.get()));
        } else if (this.reuseParentScope) {
            // Reuse!
            // We can avoid the push only if surrounding vars aren't referenced!
            context.pushScope(prevScope);
        }
        this.evalType.set(EvalType.NONE);

        if (usesKwargs) IRRuntimeHelpers.frobnicateKwargsArgument(context, getSignature().required(), args);

        try {
            return (IRubyObject)handle.invokeExact(context, getStaticScope(), self, args, block, binding.getMethod(), type);
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
