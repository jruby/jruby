package org.jruby.runtime;

import org.jruby.EvalType;
import org.jruby.RubyModule;
import org.jruby.compiler.Compilable;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class MixedModeIRBlockBody extends AbstractIRBlockBody implements Compilable<AbstractIRBlockBody> {
    private static final Logger LOG = LoggerFactory.getLogger(MixedModeIRBlockBody.class);

    protected boolean pushScope;
    protected boolean reuseParentScope;
    private volatile int callCount = 0;
    private final InterpretedIRBlockBody baseBody;
    private AbstractIRBlockBody jittedBody;

    public MixedModeIRBlockBody(IRClosure closure, Signature signature) {
        super(closure, signature);
        this.pushScope = true;
        this.reuseParentScope = false;
        this.baseBody = new InterpretedIRBlockBody(closure, signature);

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
    public boolean canInvokeDirect() {
        return jittedBody != null;
    }

    @Override
    public void setCallCount(int callCount) {
        synchronized (this) {
            this.callCount = callCount;
        }
    }

    @Override
    public void completeBuild(AbstractIRBlockBody blockBody) {
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
        return baseBody.ensureInstrsReady();
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
    protected IRubyObject invokeCallDirect(ThreadContext context, Block block, IRubyObject[] args, Block blockArg, IRubyObject self) {
        context.setCurrentBlockType(Block.Type.PROC);
        return jittedBody.invokeCallDirect(context, block, args, blockArg, null);
    }

    @Override
    protected IRubyObject invokeYieldDirect(ThreadContext context, Block block, IRubyObject[] args, Block blockArg, IRubyObject self) {
        context.setCurrentBlockType(Block.Type.NORMAL);
        return jittedBody.invokeYieldDirect(context, block, args, blockArg, self);
    }

    @Override
    protected IRubyObject invoke(ThreadContext context, Block block, IRubyObject[] args, Block blockArg, IRubyObject self) {
        if (callCount >= 0) promoteToFullBuild(context);

        return baseBody.invoke(context, block, args, blockArg, self);
    }

    private void promoteToFullBuild(ThreadContext context) {
        if (context.runtime.isBooting() && !Options.JIT_KERNEL.load()) return; // don't JIT during runtime boot

        if (callCount >= 0) {
            synchronized (this) {
                // check call count again
                if (callCount < 0) return;

                if (callCount++ >= Options.JIT_THRESHOLD.load()) {
                    callCount = -1;

                    // ensure we've got code ready for JIT
                    ensureInstrsReady();
                    closure.getNearestTopLocalVariableScope().prepareForCompilation();

                    // if we don't have an explicit protocol, disable JIT
                    if (!closure.hasExplicitCallProtocol()) {
                        if (Options.JIT_LOGGING.load()) {
                            LOG.info("JIT failed; no protocol found in block: " + closure);
                        }
                        return;
                    }

                    context.runtime.getJITCompiler().buildThresholdReached(context, this);
                }
            }
        }
    }

    public RubyModule getImplementationClass() {
        return null;
    }

}
