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
        if (self == null || this.evalType == EvalType.BINDING_EVAL) {
            self = useBindingSelf(binding);
        }

        DynamicScope prevScope = binding.getDynamicScope();
        DynamicScope newScope  = sharedScope ? prevScope : DynamicScope.newDynamicScope(getStaticScope(), prevScope);
        context.pushScope(newScope);

        try {
            return (IRubyObject)handle.invokeWithArguments(context, getStaticScope(), self, args, block, binding.getMethod(), type);
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        } finally {
            binding.getFrame().setVisibility(oldVis);
            context.postYield(binding, prevFrame);
        }
    }
}
