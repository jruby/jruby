package org.jruby.runtime;

import java.io.ByteArrayOutputStream;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.compiler.Compilable;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.FullInterpreterContext;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.persistence.IRDumper;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class MixedModeIRBlockBody extends IRBlockBody implements Compilable<CompiledIRBlockBody> {
    private static final Logger LOG = LoggerFactory.getLogger(MixedModeIRBlockBody.class);

    protected final boolean pushScope;
    protected final boolean reuseParentScope;
    private boolean displayedCFG = false; // FIXME: Remove when we find nicer way of logging CFG
    private InterpreterContext interpreterContext;
    private int callCount = 0;
    private volatile CompiledIRBlockBody jittedBody;
    private final IRClosure closure;

    public MixedModeIRBlockBody(IRClosure closure, Signature signature) {
        super(closure, signature);
        this.pushScope = true;
        this.reuseParentScope = false;
        this.closure = closure;

        // JIT currently JITs blocks along with their method and no on-demand by themselves.
        // We only promote to full build here if we are -X-C.
        if (!closure.getManager().getInstanceConfig().isJitEnabled()) setCallCount(-1);
    }

    @Override
    public boolean canCallDirect() {
        return jittedBody != null || (interpreterContext != null && interpreterContext.hasExplicitCallProtocol());
    }

    @Override
    public void setCallCount(int callCount) {
        synchronized (this) {
            this.callCount = callCount;
        }
    }

    @Override
    public void completeBuild(CompiledIRBlockBody blockBody) {
        setCallCount(-1);
        this.jittedBody = blockBody;
    }

    @Override
    public IRScope getIRScope() {
        return closure;
    }

    public BlockBody getJittedBody() {
        return jittedBody;
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
            if (IRRuntimeHelpers.shouldPrintIR(closure.getStaticScope().getModule().getRuntime())) {
                ByteArrayOutputStream baos = IRDumper.printIR(closure, false);

                LOG.info("Printing simple IR for " + closure.getId() + ":\n" + new String(baos.toByteArray()));
            }

            interpreterContext = closure.getInterpreterContext();
        }
        return interpreterContext;
    }

    @Override
    public String getName() {
        return closure.getId();
    }

    @Override
    protected IRubyObject callDirect(ThreadContext context, Block block, IRubyObject[] args, Block blockArg) {
        // We should never get here if jittedBody is null
        assert jittedBody != null : "direct call in MixedModeIRBlockBody without jitted body";

        return jittedBody.callDirect(context, block, args, blockArg);
    }

    @Override
    protected IRubyObject yieldDirect(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self) {
        // We should never get here if jittedBody is null
        assert jittedBody != null : "direct yield in MixedModeIRBlockBody without jitted body";

        return jittedBody.yieldDirect(context, block, args, self);
    }

    @Override
    protected IRubyObject commonYieldPath(ThreadContext context, Block block, Block.Type type, IRubyObject[] args, IRubyObject self, Block blockArg) {
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

        self = IRRuntimeHelpers.updateBlockState(block, self);

        try {
            return Interpreter.INTERPRET_BLOCK(context, block, self, ic, args, binding.getMethod(), blockArg);
        }
        finally {
            postYield(context, ic, binding, oldVis, prevFrame);

            // trigger JIT on the trailing edge, so we make a best effort to not interpret again after jitting
            if (callCount >= 0) promoteToFullBuild(context);
        }
    }

    private void promoteToFullBuild(ThreadContext context) {
        final Ruby runtime = context.runtime;
        if (runtime.isBooting() && !Options.JIT_KERNEL.load()) return; // don't JIT during runtime boot

        if (this.callCount < 0) return;
        // we don't synchronize callCount++ it does not matter if count isn't accurate
        if (this.callCount++ >= runtime.getInstanceConfig().getJitThreshold()) {
            synchronized (this) { // disable same jit tasks from entering queue twice
                if (this.callCount >= 0) {
                    this.callCount = Integer.MIN_VALUE; // so that callCount++ stays < 0

                    // ensure we've got code ready for JIT
                    ensureInstrsReady();
                    closure.getNearestTopLocalVariableScope().prepareForCompilation();

                    FullInterpreterContext fic = closure.getFullInterpreterContext();

                    if (fic == null || !fic.hasExplicitCallProtocol()) {
                        if (Options.JIT_LOGGING.load()) {
                            LOG.info("JIT failed; no full IR or no call protocol found in block: " + closure);
                        }
                        return; // do not JIT if we don't have an explicit protocol
                    }

                    runtime.getJITCompiler().buildThresholdReached(context, this);
                }
            }
        }
    }

    public RubyModule getImplementationClass() {
        return closure.getStaticScope().getModule();
    }

    @Override
    public IRClosure getScope() {
        return closure;
    }

}
