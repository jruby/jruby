package org.jruby.runtime;

import java.io.ByteArrayOutputStream;

import org.jruby.ir.IRClosure;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.persistence.IRDumper;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class InterpretedIRBlockBody extends AbstractIRBlockBody {
    private static final Logger LOG = LoggerFactory.getLogger(InterpretedIRBlockBody.class);
    protected boolean pushScope;
    protected boolean reuseParentScope;
    private InterpreterContext interpreterContext;

    public InterpretedIRBlockBody(IRClosure closure, Signature signature) {
        super(closure, signature);
    }

    @Override
    public ArgumentDescriptor[] getArgumentDescriptors() {
        return closure.getArgumentDescriptors();
    }

    public InterpreterContext ensureInstrsReady() {
        InterpreterContext interpreterContext = this.interpreterContext;

        if (interpreterContext == null) {
            if (IRRuntimeHelpers.isDebug()) {
                LOG.info("Executing '" + closure + "' (pushScope=" + pushScope + ", reuseParentScope=" + reuseParentScope);
                LOG.info(closure.debugOutput());
            }

            if (IRRuntimeHelpers.shouldPrintIR(closure.getStaticScope().getModule().getRuntime())) {
                ByteArrayOutputStream baos = IRDumper.printIR(closure, false);

                LOG.info("Printing simple IR for " + closure.getName() + ":\n" + new String(baos.toByteArray()));
            }

            interpreterContext = this.interpreterContext = closure.getInterpreterContext();
            this.pushScope = interpreterContext.pushNewDynScope();
            this.reuseParentScope = interpreterContext.reuseParentDynScope();
        }

        return interpreterContext;
    }

    @Override
    public boolean canInvokeDirect() {
        return false;
    }

    @Override
    protected IRubyObject invokeCallDirect(ThreadContext context, Block block, IRubyObject[] args, Block blockArg, IRubyObject self) {
        throw new UnsupportedOperationException("invokeCallDirect not implemented");
    }

    @Override
    protected IRubyObject invokeYieldDirect(ThreadContext context, Block block, IRubyObject[] args, Block blockArg, IRubyObject self) {
        throw new UnsupportedOperationException("invokeYieldDirect not implemented");
    }

    @Override
    protected IRubyObject invoke(ThreadContext context, Block block, IRubyObject[] args, Block blockArg, IRubyObject self) {
        InterpreterContext ic = ensureInstrsReady();

        Binding binding = block.getBinding();
        Visibility oldVis = binding.getFrame().getVisibility();
        Frame prevFrame = context.preYieldNoScope(binding);

        // SSS FIXME: Maybe, we should allocate a NoVarsScope/DummyScope for for-loop bodies because the static-scope here
        // probably points to the parent scope? To be verified and fixed if necessary. There is no harm as it is now. It
        // is just wasteful allocation since the scope is not used at all.
        DynamicScope actualScope = binding.getDynamicScope();
        if (pushScope) {
            context.pushScope(block.allocScope(actualScope));
        } else if (reuseParentScope) {
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

}
