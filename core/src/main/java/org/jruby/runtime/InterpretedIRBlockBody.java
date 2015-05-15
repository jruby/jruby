package org.jruby.runtime;

import org.jruby.EvalType;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.compiler.Compilable;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block.Type;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class InterpretedIRBlockBody extends IRBlockBody implements Compilable<InterpreterContext> {
    private static final Logger LOG = LoggerFactory.getLogger("InterpretedIRBlockBody");
    protected boolean pushScope;
    protected boolean reuseParentScope;
    private boolean displayedCFG = false; // FIXME: Remove when we find nicer way of logging CFG
    private int callCount = 0;
    private InterpreterContext interpreterContext;

    public InterpretedIRBlockBody(IRClosure closure, Signature signature) {
        super(closure, signature);
        this.pushScope = true;
        this.reuseParentScope = false;

        // JIT currently JITs blocks along with their method and no on-demand by themselves.  We only
        // promote to full build here if we are -X-C.
        if (closure.getManager().getInstanceConfig().getCompileMode() != RubyInstanceConfig.CompileMode.OFF) {
            callCount = -1;
        }
    }

    @Override
    public void setCallCount(int callCount) {
        this.callCount = callCount;
    }

    @Override
    public void completeBuild(InterpreterContext interpreterContext) {
        this.interpreterContext = interpreterContext;
    }

    @Override
    public IRScope getIRScope() {
        return closure;
    }

    @Override
    public ArgumentDescriptor[] getArgumentDescriptors() {
        return closure.getArgumentDescriptors();
    }

    public InterpreterContext ensureInstrsReady() {
        if (IRRuntimeHelpers.isDebug() && !displayedCFG) {
            LOG.info("Executing '" + closure + "' (pushScope=" + pushScope + ", reuseParentScope=" + reuseParentScope);
            LOG.info(closure.debugOutput());
            displayedCFG = true;
        }

        if (interpreterContext == null) {
            interpreterContext = closure.getInterpreterContext();
        }
        return interpreterContext;
    }

    @Override
    public String getClassName(ThreadContext context) {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    protected IRubyObject commonYieldPath(ThreadContext context, IRubyObject[] args, IRubyObject self, Binding binding, Type type, Block block) {
        if (callCount >= 0) promoteToFullBuild(context);

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

        InterpreterContext ic = ensureInstrsReady();

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

    // Unlike JIT in MixedMode this will always successfully build but if using executor pool it may take a while
    // and replace interpreterContext asynchronously.
    protected void promoteToFullBuild(ThreadContext context) {
        if (context.runtime.isBooting()) return; // don't Promote to full build during runtime boot

        if (callCount++ >= Options.JIT_THRESHOLD.load()) context.runtime.getJITCompiler().buildThresholdReached(context, this);
    }

    public RubyModule getImplementationClass() {
        return null;
    }

}
