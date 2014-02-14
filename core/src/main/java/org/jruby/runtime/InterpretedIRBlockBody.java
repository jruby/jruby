package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.ir.IRClosure;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block.Type;
import org.jruby.runtime.builtin.IRubyObject;

public class InterpretedIRBlockBody extends IRBlockBody {
    protected final IRClosure closure;

    public InterpretedIRBlockBody(IRClosure closure, Arity arity, int argumentType) {
        super(closure.getStaticScope(), closure.getParameterList(), closure.getFileName(), closure.getLineNumber(), arity);
        this.closure = closure;
    }

    protected IRubyObject commonYieldPath(ThreadContext context, IRubyObject[] args, IRubyObject self, RubyModule klass, Binding binding, Type type, Block block) {
        // SSS: Important!  Use getStaticScope() to use a copy of the static-scope stored in the block-body.
        // Do not use 'closure.getStaticScope()' -- that returns the original copy of the static scope.
        // This matters because blocks created for Thread bodies modify the static-scope field of the block-body
        // that records additional state about the block body.
        //
        // FIXME: Rather than modify static-scope, it seems we ought to set a field in block-body which is then
        // used to tell dynamic-scope that it is a dynamic scope for a thread body.  Anyway, to be revisited later!
        Visibility oldVis = binding.getFrame().getVisibility();
        Frame prevFrame = context.preYieldNoScope(binding, klass);
        if (klass == null) self = prepareSelf(binding);
        try {
            DynamicScope prevScope = binding.getDynamicScope();
            // SSS FIXME: Maybe, we should allocate a NoVarsScope/DummyScope for for-loop bodies because the static-scope here
            // probably points to the parent scope? To be verified and fixed if necessary. There is no harm as it is now. It
            // is just wasteful allocation since the scope is not used at all.
            DynamicScope newScope  = closure.isBeginEndBlock() ? prevScope : DynamicScope.newDynamicScope(getStaticScope(), prevScope);
            context.pushScope(newScope);
            return Interpreter.INTERPRET_BLOCK(context, self, closure, args, binding.getMethod(), block, type);
        }
        finally {
            binding.getFrame().setVisibility(oldVis);
            context.postYield(binding, prevFrame);
        }
    }
}
