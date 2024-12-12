package org.jruby.runtime;

import java.io.ByteArrayOutputStream;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.compiler.Compilable;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.persistence.IRDumper;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import static org.jruby.api.Access.instanceConfig;

public class InterpretedIRBlockBody extends IRBlockBody implements Compilable<InterpreterContext> {
    private static final Logger LOG = LoggerFactory.getLogger(InterpretedIRBlockBody.class);
    protected final boolean pushScope;
    protected final boolean reuseParentScope;
    private boolean displayedCFG = false; // FIXME: Remove when we find nicer way of logging CFG
    private int callCount = 0;
    private InterpreterContext interpreterContext;
    private InterpreterContext fullInterpreterContext;
    private final IRClosure closure;

    public InterpretedIRBlockBody(IRClosure closure, Signature signature) {
        super(closure, signature);
        this.pushScope = true;
        this.reuseParentScope = false;
        this.closure = closure;

        // -1 jit.threshold is way of having interpreter not promote full builds
        // regardless of compile mode (even when OFF full-builds are promoted)
        if (closure.getManager().getInstanceConfig().getJitThreshold() == -1) setCallCount(-1);
    }

    @Override
    public void setCallCount(int callCount) {
        this.callCount = callCount;
    }

    @Override
    public void completeBuild(InterpreterContext interpreterContext) {
        this.fullInterpreterContext = interpreterContext;
        // This enables IR & CFG to be dumped in debug mode
        // when this updated code starts executing.
        this.displayedCFG = false;
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

        InterpreterContext ic = interpreterContext;
        if (ic == null) {
            if (IRRuntimeHelpers.shouldPrintIR(closure.getStaticScope().getModule().getRuntime()) && IRRuntimeHelpers.shouldPrintScope(getIRScope())) {
                ByteArrayOutputStream baos = IRDumper.printIR(closure, false);

                LOG.info("Printing simple IR for " + closure.getId() + ":\n" + new String(baos.toByteArray()));
            }

            ic = closure.getInterpreterContext();
            interpreterContext = ic;
        }
        return ic;
    }

    @Override
    public String getOwnerName() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean canCallDirect() {
        return fullInterpreterContext != null && fullInterpreterContext.hasExplicitCallProtocol();
    }

    @Override
    protected IRubyObject callDirect(ThreadContext context, Block block, IRubyObject[] args, Block blockArg) {
        ensureInstrsReady(); // so we get debugging output
        return Interpreter.INTERPRET_BLOCK(context, block, null, fullInterpreterContext, args, block.getBinding().getMethod(), blockArg);
    }

    @Override
    protected IRubyObject yieldDirect(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self) {
        ensureInstrsReady(); // so we get debugging output
        return Interpreter.INTERPRET_BLOCK(context, block, self, fullInterpreterContext, args, block.getBinding().getMethod(), Block.NULL_BLOCK);
    }

    @Override
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

        self = IRRuntimeHelpers.updateBlockState(block, self);

        try {
            return Interpreter.INTERPRET_BLOCK(context, block, self, ic, args, binding.getMethod(), blockArg);
        }
        finally {
            postYield(context, ic, binding, oldVis, prevFrame);
        }
    }

    // Unlike JIT in MixedMode this will always successfully build but if using executor pool it may take a while
    // and replace interpreterContext asynchronously.
    private void promoteToFullBuild(ThreadContext context) {
        if (context.runtime.isBooting() && !Options.JIT_KERNEL.load()) return; // don't JIT during runtime boot

        if (this.callCount < 0) return;
        // we don't synchronize callCount++ it does not matter if count isn't accurate
        if (this.callCount++ >= instanceConfig(context).getJitThreshold()) {
            synchronized (this) { // disable same jit tasks from entering queue twice
                if (this.callCount >= 0) {
                    this.callCount = Integer.MIN_VALUE; // so that callCount++ stays < 0

                    context.runtime.getJITCompiler().buildThresholdReached(context, this);
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
