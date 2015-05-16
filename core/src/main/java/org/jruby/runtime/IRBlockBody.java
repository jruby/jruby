package org.jruby.runtime;

import org.jruby.EvalType;
import org.jruby.RubyArray;
import org.jruby.ir.IRScope;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block.Type;
import org.jruby.runtime.builtin.IRubyObject;

public abstract class IRBlockBody extends ContextAwareBlockBody {
    protected final String fileName;
    protected final int lineNumber;
    protected final IRScope closure;
    protected ThreadLocal<EvalType> evalType;

    public IRBlockBody(IRScope closure, Signature signature) {
        super(closure.getStaticScope(), signature);
        this.closure = closure;
        this.fileName = closure.getFileName();
        this.lineNumber = closure.getLineNumber();
        this.evalType = new ThreadLocal();
        this.evalType.set(EvalType.NONE);
    }

    public void setEvalType(EvalType evalType) {
        this.evalType.set(evalType);
    }

    @Override
    public IRubyObject call(ThreadContext context, Binding binding, Type type) {
        return call(context, IRubyObject.NULL_ARRAY, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject arg0, Binding binding, Type type) {
        return call(context, new IRubyObject[] {arg0}, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Binding binding, Type type) {
        return call(context, new IRubyObject[] {arg0, arg1}, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Binding binding, Type type) {
        return call(context, new IRubyObject[] {arg0, arg1, arg2}, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding, Type type) {
        return call(context, args, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding, Type type, Block block) {
        if (type == Type.LAMBDA) signature.checkArity(context.runtime, args);

        return commonYieldPath(context, prepareArgumentsForCall(context, args, type), null, binding, type, block);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Binding binding, Type type) {
        IRubyObject[] args = IRubyObject.NULL_ARRAY;
        if (type == Type.LAMBDA) signature.checkArity(context.runtime, args);

        return commonYieldPath(context, args, null, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, Binding binding, Type type) {
        if (arg0 instanceof RubyArray) {
		    // Unwrap the array arg
            IRubyObject[] args = IRRuntimeHelpers.convertValueIntoArgArray(context, arg0, signature.arityValue(), true);

            // FIXME: arity error is aginst new args but actual error shows arity of original args.
            if (type == Type.LAMBDA) signature.checkArity(context.runtime, args);

            return commonYieldPath(context, args, null, binding, type, Block.NULL_BLOCK);
        } else {
            return yield(context, arg0, binding, type);
        }
    }

    private IRubyObject yieldSpecificMultiArgsCommon(ThreadContext context, IRubyObject[] args, Binding binding, Type type) {
        int blockArity = getSignature().arityValue();
        if (blockArity == 0) {
            args = IRubyObject.NULL_ARRAY; // discard args
        } else if (blockArity == 1) {
            args = new IRubyObject[] { RubyArray.newArrayNoCopy(context.runtime, args) };
        }

        if (type == Type.LAMBDA) signature.checkArity(context.runtime, args);

        return commonYieldPath(context, args, null, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Binding binding, Type type) {
        return yieldSpecificMultiArgsCommon(context, new IRubyObject[] { arg0, arg1 }, binding, type);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Binding binding, Type type) {
        return yieldSpecificMultiArgsCommon(context, new IRubyObject[] { arg0, arg1, arg2 }, binding, type);
    }

    private IRubyObject[] toAry(ThreadContext context, IRubyObject value) {
        IRubyObject val0 = Helpers.aryToAry(value);

        if (!(val0 instanceof RubyArray)) {
            throw context.runtime.newTypeError(value.getType().getName() + "#to_ary should return Array");
        }

        return ((RubyArray)val0).toJavaArray();
    }

    protected IRubyObject doYieldLambda(ThreadContext context, IRubyObject value, Binding binding, Type type) {
        // Lambda does not splat arrays even if a rest arg is present when it wants a single parameter filled.
        IRubyObject[] args = signature.required() == 1 ? new IRubyObject[] { value } : toAry(context, value);

        signature.checkArity(context.runtime, args);

        return commonYieldPath(context, args, null, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject doYield(ThreadContext context, IRubyObject value, Binding binding, Type type) {
        if (type == Type.LAMBDA) return doYieldLambda(context, value, binding, type);

        int blockArity = getSignature().arityValue();

        IRubyObject[] args;
        if (blockArity >= -1 && blockArity <= 1) {
            args = new IRubyObject[] { value };
        } else {
            args = toAry(context, value);
        }

        return commonYieldPath(context, args, null, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject doYield(ThreadContext context, IRubyObject[] args, IRubyObject self, Binding binding, Type type) {
        if (type == Type.LAMBDA) signature.checkArity(context.runtime, args);

        return commonYieldPath(context, args, self, binding, type, Block.NULL_BLOCK);
    }

    protected IRubyObject useBindingSelf(Binding binding) {
        IRubyObject self = binding.getSelf();
        binding.getFrame().setSelf(self);

        return self;
    }

    protected abstract IRubyObject commonYieldPath(ThreadContext context, IRubyObject[] args, IRubyObject self, Binding binding, Type type, Block block);

    @Override
    public String getFile() {
        return fileName;
    }

    @Override
    public int getLine() {
        return lineNumber;
    }
}
