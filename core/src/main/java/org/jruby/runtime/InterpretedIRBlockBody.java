package org.jruby.runtime;

import org.jruby.EvalType;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRFlags;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.representations.CFG;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block.Type;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class InterpretedIRBlockBody extends IRBlockBody {
    private static final Logger LOG = LoggerFactory.getLogger("InterpretedIRBlockBody");
    protected final IRClosure closure;
    protected boolean pushScope;
    protected boolean reuseParentScope;
    private boolean displayedCFG = false; // FIXME: Remove when we find nicer way of logging CFG

    public InterpretedIRBlockBody(IRClosure closure, Signature signature) {
        super(closure.getStaticScope(), closure.getParameterList(), closure.getFileName(), closure.getLineNumber(), signature);
        this.closure = closure;
        this.pushScope = true;
        this.reuseParentScope = false;
    }

    public InterpreterContext ensureInstrsReady() {
        if (IRRuntimeHelpers.isDebug() && !displayedCFG) {
            LOG.info("Executing '" + closure + "' (pushScope=" + pushScope + ", reuseParentScope=" + reuseParentScope);
            LOG.info(closure.debugOutput());
            displayedCFG = true;
        }

        // Try unsync access first before calling more expensive method for getting IC
        InterpreterContext ic = closure.getInterpreterContext();

        // Build/rebuild if necessary
        if (ic == null) {
            ic = closure.prepareForInterpretation();
        } else if (ic.needsRebuilding()) {
            ic = closure.prepareForInterpretation(true);
        }

        return ic;
    }

    protected IRubyObject commonYieldPath(ThreadContext context, IRubyObject[] args, IRubyObject self, Binding binding, Type type, Block block) {
        InterpreterContext ic = ensureInstrsReady();

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
        if (ic.pushNewDynScope()) {
            context.pushScope(DynamicScope.newDynamicScope(getStaticScope(), prevScope, this.evalType.get()));
        } else if (ic.reuseParentDynScope()) {
            // Reuse! We can avoid the push only if surrounding vars aren't referenced!
            context.pushScope(prevScope);
        }
        this.evalType.set(EvalType.NONE);

        try {
            return Interpreter.INTERPRET_BLOCK(context, self, ic, args, binding.getMethod(), block, type);
        }
        finally {
            // IMPORTANT: Do not clear eval-type in case this is reused in bindings!
            // Ex: eval("...", foo.instance_eval { binding })
            // The dyn-scope used for binding needs to have its eval-type set to INSTANCE_EVAL
            binding.getFrame().setVisibility(oldVis);
            if (ic.popDynScope()) {
                context.postYield(binding, prevFrame);
            } else {
                context.postYieldNoScope(prevFrame);
            }
        }
    }
}
