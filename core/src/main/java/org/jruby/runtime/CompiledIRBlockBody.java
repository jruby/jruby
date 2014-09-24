package org.jruby.runtime;

import org.jruby.EvalType;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block.Type;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.invoke.MethodHandle;

public class CompiledIRBlockBody extends IRBlockBody {
    protected final MethodHandle handle;
    private final boolean sharedScope;

    public CompiledIRBlockBody(StaticScope staticScope, String parameterList, String fileName, int lineNumber, boolean sharedScope, MethodHandle handle, int arity) {
        this(staticScope, parameterList.split(","), fileName, lineNumber, sharedScope, handle, Arity.createArity(arity));
    }

    public CompiledIRBlockBody(StaticScope staticScope, String[] parameterList, String fileName, int lineNumber, boolean sharedScope, MethodHandle handle, Arity arity) {
        super(staticScope, parameterList, fileName, lineNumber, arity);
        this.handle = handle;
        this.sharedScope = sharedScope;
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

        DynamicScope newScope = null;
        DynamicScope prevScope = binding.getDynamicScope();

        // CON FIXME: This is copied from InterpretedIRBlockBody, and obviously means all blocks allocate a scope; we must fix that
        // SSS FIXME: Maybe, we should allocate a NoVarsScope/DummyScope for for-loop bodies because the static-scope here
        // probably points to the parent scope? To be verified and fixed if necessary. There is no harm as it is now. It
        // is just wasteful allocation since the scope is not used at all.

        // Pass on eval state info to the dynamic scope and clear it on the block-body
        newScope  = DynamicScope.newDynamicScope(getStaticScope(), prevScope, this.evalType.get());
        this.evalType.set(EvalType.NONE);
        context.pushScope(newScope);

        try {
            return (IRubyObject)handle.invokeExact(context, getStaticScope(), self, args, block, binding.getMethod(), type);
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        } finally {
            binding.getFrame().setVisibility(oldVis);
            context.postYield(binding, prevFrame);
        }
    }
}
