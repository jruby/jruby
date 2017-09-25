package org.jruby.runtime;

import org.jruby.EvalType;
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

import java.io.ByteArrayOutputStream;

public class MixedModeIRBlockBody extends IRBlockBody implements Compilable<CompiledIRBlockBody> {
    private static final Logger LOG = LoggerFactory.getLogger(MixedModeIRBlockBody.class);

    protected boolean pushScope;
    protected boolean reuseParentScope;
    private boolean displayedCFG = false; // FIXME: Remove when we find nicer way of logging CFG
    private volatile int callCount = 0;
    private InterpreterContext interpreterContext;
    private volatile CompiledIRBlockBody jittedBody;

    public MixedModeIRBlockBody(IRClosure closure, Signature signature) {
        super(closure, signature);
        this.pushScope = true;
        this.reuseParentScope = false;

        // JIT currently JITs blocks along with their method and no on-demand by themselves.  We only
        // promote to full build here if we are -X-C.
        if (!closure.getManager().getInstanceConfig().getCompileMode().shouldJIT() ||
                Options.JIT_THRESHOLD.load() < 0) {
            callCount = -1;
        }
    }

    @Override
    public void setEvalType(EvalType evalType) {
        super.setEvalType(evalType); // so that getEvalType is correct
        if (jittedBody != null) jittedBody.setEvalType(evalType);
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
        blockBody.evalType = this.evalType; // share with parent
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
            if (Options.IR_PRINT.load()) {
                ByteArrayOutputStream baos = IRDumper.printIR(closure, false);

                LOG.info("Printing simple IR for " + closure.getName() + ":\n" + new String(baos.toByteArray()));
            }

            interpreterContext = closure.getInterpreterContext();
        }
        return interpreterContext;
    }

    @Override
    public String getClassName(ThreadContext context) {
        return closure.getName();
    }

    @Override
    public String getName() {
        return closure.getName();
    }

    @Override
    protected IRubyObject callDirect(ThreadContext context, Block block, IRubyObject[] args, Block blockArg) {
        // We should never get here if jittedBody is null
        assert jittedBody != null : "direct call in MixedModeIRBlockBody without jitted body";

        context.setCurrentBlockType(Block.Type.PROC);
        return jittedBody.callDirect(context, block, args, blockArg);
    }

    @Override
    protected IRubyObject yieldDirect(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self) {
        // We should never get here if jittedBody is null
        assert jittedBody != null : "direct yield in MixedModeIRBlockBody without jitted body";

        context.setCurrentBlockType(Block.Type.NORMAL);
        return jittedBody.yieldDirect(context, block, args, self);
    }

    @Override
    protected IRubyObject commonYieldPath(ThreadContext context, Block block, Block.Type type, IRubyObject[] args, IRubyObject self, Block blockArg) {
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

    private void promoteToFullBuild(ThreadContext context) {
        if (context.runtime.isBooting() && !Options.JIT_KERNEL.load()) return; // don't JIT during runtime boot

        if (callCount >= 0) {
            // ensure we've got code ready for JIT
            ensureInstrsReady();
            closure.getNearestTopLocalVariableScope().prepareForCompilation();

            // if we don't have an explicit protocol, disable JIT
            if (!closure.hasExplicitCallProtocol()) {
                if (Options.JIT_LOGGING.load()) {
                    LOG.info("JIT failed; no protocol found in block: " + closure);
                }
                setCallCount(-1);
                return;
            }

            synchronized (this) {
                int callCount = this.callCount;
                if (callCount >= 0 && callCount++ >= Options.JIT_THRESHOLD.load()) {
                    this.callCount = callCount;
                    context.runtime.getJITCompiler().buildThresholdReached(context, this);
                }
            }
        }
    }

    public RubyModule getImplementationClass() {
        return null;
    }

}
