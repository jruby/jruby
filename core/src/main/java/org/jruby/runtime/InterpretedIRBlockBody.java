package org.jruby.runtime;

import org.jruby.RubyModule;
import org.jruby.EvalType;
import org.jruby.compiler.Compilable;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.runtime.IRRuntimeHelpers;
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
        if (closure.getManager().getInstanceConfig().getCompileMode().shouldJIT() || Options.JIT_THRESHOLD.load() == -1) {
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

    public void setInterpreterContext(InterpreterContext interpreterContext) {
        this.interpreterContext = interpreterContext;
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
        hasCallProtocolIR = closure.getFlags().contains(IRFlags.HAS_EXPLICIT_CALL_PROTOCOL);
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

    @Override
    protected IRubyObject yieldDirect(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self) {
        return Interpreter.INTERPRET_BLOCK(context, block, self, interpreterContext, args, block.getBinding().getMethod(), Block.NULL_BLOCK);
    }

    protected IRubyObject commonYieldPath(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self, Block blockArg) {
        if (callCount >= 0) promoteToFullBuild(context);

        InterpreterContext ic = ensureInstrsReady();

        Binding binding = block.getBinding();
        Visibility oldVis = binding.getFrame().getVisibility();
        Frame prevFrame = context.preYieldNoScope(binding);

        // SSS FIXME: Maybe, we should allocate a NoVarsScope/DummyScope for for-loop bodies because the static-scope here
        // probably points to the parent scope? To be verified and fixed if necessary. There is no harm as it is now. It
        // is just wasteful allocation since the scope is not used at all.
        DynamicScope actualScope = binding.getDynamicScope();
        if (ic.pushNewDynScope()) {
            context.pushScope(block.allocScope(actualScope));
        } else if (ic.reuseParentDynScope()) {
            // Reuse! We can avoid the push only if surrounding vars aren't referenced!
            context.pushScope(actualScope);
        }

        // SSS FIXME: Why is self null in non-binding-eval contexts?
        if (self == null || getEvalType() == EvalType.BINDING_EVAL) {
            self = useBindingSelf(binding);
        }

        // Clear evaltype now that it has been set on dyn-scope
        block.setEvalType(EvalType.NONE);

        try {
            return Interpreter.INTERPRET_BLOCK(context, block, self, ic, args, binding.getMethod(), blockArg);
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
