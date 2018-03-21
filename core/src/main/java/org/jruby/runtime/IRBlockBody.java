package org.jruby.runtime;

import org.jruby.EvalType;
import org.jruby.RubyArray;
import org.jruby.RubyProc;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.RubyArray.newArray;
import static org.jruby.runtime.Helpers.arrayOf;

public abstract class IRBlockBody extends ContextAwareBlockBody {
    protected final String fileName;
    protected final int lineNumber;
    protected final IRClosure closure;
    ThreadLocal<EvalType> evalType;

    public IRBlockBody(IRScope closure, Signature signature) {
        // ThreadLocal not set by default to avoid having many thread-local values initialized
        // servers such as Tomcat tend to do thread-local checks when un-deploying apps,
        // for JRuby leads to 100s of SEVERE warnings for a mid-size (booted) Rails app
        this(closure, signature, new ThreadLocal());
    }

    /* internal */ IRBlockBody(IRScope closure, Signature signature, ThreadLocal evalType) {
        super(closure.getStaticScope(), signature);
        this.closure = (IRClosure) closure;
        this.fileName = closure.getFileName();
        this.lineNumber = closure.getLineNumber();
        this.evalType = evalType;
    }

    public final EvalType getEvalType() {
        final EvalType type = this.evalType.get();
        return type == null ? EvalType.NONE : type;
    }

    public void setEvalType(final EvalType type) {
        if (type == null || type == EvalType.NONE) {
            this.evalType.remove();
        } else {
            this.evalType.set(type);
        }
    }

    public IRubyObject yield(ThreadContext context, Block block, IRubyObject value, IRubyObject self, Block blockArg) {
        if (canInvokeDirect()) {
            return invokeYieldDirect(context, block, arrayOf(value), blockArg, self);
        } else {
            return invokeYield(context, block, value, blockArg, self);
        }
    }

    public IRubyObject yield(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self, Block blockArg) {
        if (canInvokeDirect()) {
            return invokeYieldDirect(context, block, args, blockArg, self);
        } else {
            IRubyObject[] preppedValue = RubyProc.prepareArgs(context, block.type, this, args);
            if (block.isLambda()) return invokeLambda(context, block, preppedValue, blockArg, self);

            return invoke(context, block, preppedValue, blockArg, self);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0, Block blockArg) {
        return invokeCall(context, block, new IRubyObject[] {arg0}, blockArg, null);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, Block blockArg) {
        return invokeCall(context, block, new IRubyObject[] {arg0, arg1}, blockArg, null);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block blockArg) {
        return invokeCall(context, block, new IRubyObject[]{arg0, arg1, arg2}, blockArg, null);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject[] args, Block blockArg) {
        return invokeCall(context, block, args, blockArg, null);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block) {
        return invokeSpecific(context, block, Block.NULL_BLOCK, null);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0) {
        return invokeSpecific(context, block, arg0, Block.NULL_BLOCK, null);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1) {
        return invokeSpecific(context, block, arg0, arg1, Block.NULL_BLOCK, null);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return invokeSpecific(context, block, arg0, arg1, arg2, Block.NULL_BLOCK, null);
    }

    protected abstract boolean canInvokeDirect();

    /**
     * This is the only method that *must* be implemented in blocks, and represents the bulk of logic.
     *
     * Other forms empty into this one and can be overridden to optimize specific paths.
     *
     * @param context
     * @param block
     * @param args
     * @param blockArg
     * @param self
     * @return
     */
    protected abstract IRubyObject invoke(ThreadContext context, Block block, IRubyObject[] args, Block blockArg, IRubyObject self);

    protected abstract IRubyObject invokeCallDirect(ThreadContext context, Block block, IRubyObject[] args, Block blockArg, IRubyObject self);

    protected abstract IRubyObject invokeYieldDirect(ThreadContext context, Block block, IRubyObject[] args, Block blockArg, IRubyObject self);

    IRubyObject invokeYield(ThreadContext context, Block block, IRubyObject value, Block blockArg, IRubyObject self) {
        if (block.isLambda()) return invokeLambda(context, block, value, blockArg, self);

        int blockArity = signature.arityValue();

        if (value == null) { // no args case from BlockBody.yieldSpecific
            return invoke(context, block, IRubyObject.NULL_ARRAY, blockArg, self);
        } else if (!signature.hasKwargs() && blockArity >= -1 && blockArity <= 1) {
            return invoke(context, block, arrayOf(value), blockArg, self);
        } else {
            return invoke(context, block, toAry(context, value), blockArg, self);
        }
    }

    IRubyObject invokeLambda(ThreadContext context, Block block, IRubyObject value, Block blockArg, IRubyObject self) {
        // Lambda does not splat arrays even if a rest arg is present when it wants a single parameter filled.
        if (value == null) { // no args case from BlockBody.yieldSpecific
            return invokeLambda(context, block, IRubyObject.NULL_ARRAY, blockArg, self);
        } else if (signature.required() == 1 || signature.arityValue() == -1) {
            return invokeLambda(context, block, arrayOf(value), blockArg, self);
        } else {
            return invokeLambda(context, block, toAry(context, value), blockArg, self);
        }
    }

    IRubyObject invokeLambda(ThreadContext context, Block block, IRubyObject[] args, Block blockArg, IRubyObject self) {
        signature.checkArity(context.runtime, args);

        return invoke(context, block, args, blockArg, self);
    }

    IRubyObject invokeSpecific(ThreadContext context, Block block, Block blockArg, IRubyObject self) {
        if (canInvokeDirect()) {
            return invokeYieldDirect(context, block, null, blockArg, self);
        } else {
            IRubyObject[] args = IRubyObject.NULL_ARRAY;
            if (block.isLambda()) signature.checkArity(context.runtime, args);
            return invoke(context, block, args, blockArg, self);
        }
    }

    IRubyObject invokeSpecific(ThreadContext context, Block block, IRubyObject arg0, Block blockArg, IRubyObject self) {
        if (canInvokeDirect()) return invokeSpecificDirect(context, block, arg0, blockArg, self);

        if (arg0 instanceof RubyArray) {
            // Unwrap the array arg
            IRubyObject[] args = IRRuntimeHelpers.convertValueIntoArgArray(context, arg0, signature, true);

            // FIXME: arity error is against new args but actual error shows arity of original args.
            if (block.isLambda()) signature.checkArity(context.runtime, args);

            return invoke(context, block, args, blockArg, self);
        }

        return invokeYield(context, block, arg0, blockArg, self);
    }

    IRubyObject invokeSpecificDirect(ThreadContext context, Block block, IRubyObject arg0, Block blockArg, IRubyObject self) {
        IRubyObject[] args;
        if (arg0 instanceof RubyArray) {
            // Unwrap the array arg
            args = IRRuntimeHelpers.convertValueIntoArgArray(context, arg0, signature, true);
        } else {
            args = arrayOf(arg0);
        }
        return invokeYieldDirect(context, block, args, blockArg, self);
    }

    IRubyObject invokeSpecific(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, Block blockArg, IRubyObject self) {
        if (canInvokeDirect()) return invokeSpecificDirect(context, block, arg0, arg1, blockArg, self);

        switch (signature.arityValue()) {
            case 0:
                return invoke(context, block, IRubyObject.NULL_ARRAY, blockArg, self);
            case 1:
                return invoke(context, block, arrayOf(newArray(context.runtime, arg0, arg1)), blockArg, self);
            default:
                IRubyObject[] args = arrayOf(arg0, arg1);
                if (block.isLambda()) signature.checkArity(context.runtime, args);
                return invoke(context, block, args, blockArg, self);
        }
    }

    IRubyObject invokeSpecific(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block blockArg, IRubyObject self) {
        if (canInvokeDirect()) return invokeSpecificDirect(context, block, arg0, arg1, arg2, blockArg, self);

        switch (signature.arityValue()) {
            case 0:
                return invoke(context, block, IRubyObject.NULL_ARRAY, blockArg, self);
            case 1:
                return invoke(context, block, arrayOf(newArray(context.runtime, arg0, arg1, arg2)), blockArg, self);
            default:
                IRubyObject[] args = arrayOf(arg0, arg1, arg2);
                if (block.isLambda()) signature.checkArity(context.runtime, args);
                return invoke(context, block, args, blockArg, self);
        }
    }

    IRubyObject invokeSpecificDirect(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, Block blockArg, IRubyObject self) {
        switch (signature.arityValue()) {
            case 0:
                return invokeYieldDirect(context, block, arrayOf(arg0, arg1), blockArg, self);
            case 1:
                return invokeYieldDirect(context, block, arrayOf(newArray(context.runtime, arg0, arg1)), blockArg, self);
            default:
                return invokeYieldDirect(context, block, arrayOf(arg0, arg1), blockArg, self);
        }
    }

    IRubyObject invokeSpecificDirect(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block blockArg, IRubyObject self) {
        switch (signature.arityValue()) {
            case 0:
                return invokeYieldDirect(context, block, arrayOf(arg0, arg1, arg2), blockArg, self);
            case 1:
                return invokeYieldDirect(context, block, arrayOf(newArray(context.runtime, arg0, arg1, arg2)), blockArg, self);
            default:
                return invokeYieldDirect(context, block, arrayOf(arg0, arg1, arg2), blockArg, self);
        }
    }

    IRubyObject invokeCall(ThreadContext context, Block block, IRubyObject[] args, Block blockArg, IRubyObject self) {
        if (canInvokeDirect()) {
            return invokeCallDirect(context, block, args, blockArg, self);
        }
        return invoke(context, block, prepareArgumentsForCall(context, args, block.type), blockArg, self);
    }

    static void postYield(ThreadContext context, InterpreterContext ic, Binding binding, Visibility oldVis, Frame prevFrame) {
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

    public IRClosure getScope() {
        return closure;
    }

    @Override
    public String getFile() {
        return fileName;
    }

    @Override
    public int getLine() {
        return lineNumber;
    }

    private IRubyObject[] toAry(ThreadContext context, IRubyObject value) {
        final IRubyObject ary = Helpers.aryToAry(context, value);

        if (ary == context.nil) return arrayOf(value);

        if (ary instanceof RubyArray) return ((RubyArray) ary).toJavaArray();

        throw context.runtime.newTypeError(value.getType().getName() + "#to_ary should return Array");
    }
}
