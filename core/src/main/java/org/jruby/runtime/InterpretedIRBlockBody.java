package org.jruby.runtime;

import java.io.ByteArrayOutputStream;
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

public class InterpretedIRBlockBody extends IRBlockBody implements Compilable<InterpreterContext> {
    private static final Logger LOG = LoggerFactory.getLogger(InterpretedIRBlockBody.class);
    protected boolean pushScope;
    protected boolean reuseParentScope;
    private boolean displayedCFG = false; // FIXME: Remove when we find nicer way of logging CFG
    private int callCount = 0;
    private InterpreterContext interpreterContext;
    private InterpreterContext fullInterpreterContext;

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

        if (interpreterContext == null) {
            if (Options.IR_PRINT.load()) {
                ByteArrayOutputStream baos = IRDumper.printIR(closure, false);

                LOG.info("Printing simple IR for " + closure.getName() + ":\n" + new String(baos.toByteArray()));
            }

            interpreterContext = closure.getInterpreterContext();
            fullInterpreterContext = interpreterContext;
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

    @Override
    public boolean canCallDirect() {
        return interpreterContext != null && interpreterContext.hasExplicitCallProtocol();
    }

    @Override
    protected IRubyObject callDirect(ThreadContext context, Block block, IRubyObject[] args, Block blockArg) {
        context.setCurrentBlockType(Block.Type.PROC);
        InterpreterContext ic = ensureInstrsReady(); // so we get debugging output
        return Interpreter.INTERPRET_BLOCK(context, block, null, ic, args, block.getBinding().getMethod(), blockArg);
    }

    @Override
    protected IRubyObject yieldDirect(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self) {
        context.setCurrentBlockType(Block.Type.NORMAL);
        InterpreterContext ic = ensureInstrsReady(); // so we get debugging output
        return Interpreter.INTERPRET_BLOCK(context, block, self, ic, args, block.getBinding().getMethod(), Block.NULL_BLOCK);
    }

    @Override
    protected IRubyObject commonYieldPath(ThreadContext context, Block block, Block.Type type, IRubyObject[] args, IRubyObject self, Block blockArg) {
        if (callCount >= 0) promoteToFullBuild(context);

        InterpreterContext ic = ensureInstrsReady();

        // Update interpreter context for next time this block is executed
        // This ensures that if we had determined canCallDirect() is false
        // based on the old IC, we continue to execute with it.
        interpreterContext = fullInterpreterContext;

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
        if (context.runtime.isBooting() && !Options.JIT_KERNEL.load()) return; // don't Promote to full build during runtime boot

        if (callCount++ >= Options.JIT_THRESHOLD.load()) {
            context.runtime.getJITCompiler().buildThresholdReached(context, this);
        }
    }

    public RubyModule getImplementationClass() {
        return null;
    }

}
