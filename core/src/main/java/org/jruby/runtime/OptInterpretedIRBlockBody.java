package org.jruby.runtime;

import org.jruby.ir.IRClosure;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class OptInterpretedIRBlockBody extends AbstractIRBlockBody {
    private static final Logger LOG = LoggerFactory.getLogger(OptInterpretedIRBlockBody.class);
    protected boolean pushScope;
    protected boolean reuseParentScope;
    private InterpreterContext fullInterpreterContext;

    public OptInterpretedIRBlockBody(IRClosure closure, Signature signature) {
        super(closure, signature);
        this.pushScope = true;
        this.reuseParentScope = false;
        this.fullInterpreterContext = closure.prepareFullBuild();
    }

    @Override
    public ArgumentDescriptor[] getArgumentDescriptors() {
        return closure.getArgumentDescriptors();
    }

    @Override
    public boolean canInvokeDirect() {
        return true;
    }

    @Override
    protected IRubyObject invokeCallDirect(ThreadContext context, Block block, IRubyObject[] args, Block blockArg, IRubyObject self) {
        context.setCurrentBlockType(Block.Type.PROC);
        InterpreterContext ic = fullInterpreterContext;
        return Interpreter.INTERPRET_BLOCK(context, block, null, ic, args, block.getBinding().getMethod(), blockArg);
    }

    @Override
    protected IRubyObject invokeYieldDirect(ThreadContext context, Block block, IRubyObject[] args, Block blockArg, IRubyObject self) {
        context.setCurrentBlockType(Block.Type.NORMAL);
        InterpreterContext ic = fullInterpreterContext;
        return Interpreter.INTERPRET_BLOCK(context, block, self, ic, args, block.getBinding().getMethod(), blockArg);
    }

    @Override
    protected IRubyObject invoke(ThreadContext context, Block block, IRubyObject[] args, Block blockArg, IRubyObject self) {
        throw new UnsupportedOperationException("invoke not implemented");
    }

}
