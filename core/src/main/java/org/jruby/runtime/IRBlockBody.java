package org.jruby.runtime;

import org.jruby.EvalType;
import org.jruby.RubyArray;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.builtin.IRubyObject;

public abstract class IRBlockBody extends ContextAwareBlockBody {
    protected final String fileName;
    protected final int lineNumber;
    protected final IRClosure closure;
    protected ThreadLocal<EvalType> evalType;

    public IRBlockBody(IRScope closure, Signature signature) {
        super(closure.getStaticScope(), signature);
        this.closure = (IRClosure) closure;
        this.fileName = closure.getFileName();
        this.lineNumber = closure.getLineNumber();
        this.evalType = new ThreadLocal();
        this.evalType.set(EvalType.NONE);
    }

    public void setEvalType(EvalType evalType) {
        this.evalType.set(evalType);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block) {
        return call(context, IRubyObject.NULL_ARRAY, block, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject arg0, Block block) {
        return call(context, new IRubyObject[] {arg0}, block, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return call(context, new IRubyObject[] {arg0, arg1}, block, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return call(context, new IRubyObject[]{arg0, arg1, arg2}, block, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
        return call(context, args, block, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block, Block blockArg) {
        if (block.type == Block.Type.LAMBDA) signature.checkArity(context.runtime, args);

        return commonYieldPath(context, prepareArgumentsForCall(context, args, block.type), null, block, blockArg);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block) {
        IRubyObject[] args = IRubyObject.NULL_ARRAY;
        if (block.type == Block.Type.LAMBDA) signature.checkArity(context.runtime, args);

        return commonYieldPath(context, args, null, block, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, Block block) {
        if (arg0 instanceof RubyArray) {
		    // Unwrap the array arg
            IRubyObject[] args = IRRuntimeHelpers.convertValueIntoArgArray(context, arg0, signature.arityValue(), true);

            // FIXME: arity error is aginst new args but actual error shows arity of original args.
            if (block.type == Block.Type.LAMBDA) signature.checkArity(context.runtime, args);

            return commonYieldPath(context, args, null, block, Block.NULL_BLOCK);
        } else {
            return yield(context, arg0, block);
        }
    }

    IRubyObject yieldSpecificMultiArgsCommon(ThreadContext context, IRubyObject[] args, Block block) {
        int blockArity = getSignature().arityValue();
        if (blockArity == 0) {
            args = IRubyObject.NULL_ARRAY; // discard args
        } else if (blockArity == 1) {
            args = new IRubyObject[] { RubyArray.newArrayNoCopy(context.runtime, args) };
        }

        if (block.type == Block.Type.LAMBDA) signature.checkArity(context.runtime, args);

        return commonYieldPath(context, args, null, block, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return yieldSpecificMultiArgsCommon(context, new IRubyObject[]{arg0, arg1}, block);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return yieldSpecificMultiArgsCommon(context, new IRubyObject[]{arg0, arg1, arg2}, block);
    }

    private IRubyObject[] toAry(ThreadContext context, IRubyObject value) {
        IRubyObject val0 = Helpers.aryToAry(value);

        if (val0.isNil()) return new IRubyObject[] { value };

        if (!(val0 instanceof RubyArray)) {
            throw context.runtime.newTypeError(value.getType().getName() + "#to_ary should return Array");
        }

        return ((RubyArray)val0).toJavaArray();
    }

    protected IRubyObject doYieldLambda(ThreadContext context, IRubyObject value, Block block) {
        // Lambda does not splat arrays even if a rest arg is present when it wants a single parameter filled.
        IRubyObject[] args;

        if (value == null) { // no args case from BlockBody.yieldSpecific
            args = IRubyObject.NULL_ARRAY;
        } else if (signature.required() == 1 || signature.arityValue() == -1) {
            args = new IRubyObject[] { value };
        } else {
            args = toAry(context, value);
        }

        signature.checkArity(context.runtime, args);

        return commonYieldPath(context, args, null, block, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject doYield(ThreadContext context, IRubyObject value, Block block) {
        if (block.type == Block.Type.LAMBDA) return doYieldLambda(context, value, block);

        int blockArity = getSignature().arityValue();

        IRubyObject[] args;
        if (value == null) { // no args case from BlockBody.yieldSpecific
            args = IRubyObject.NULL_ARRAY;
        } else if (blockArity >= -1 && blockArity <= 1) {
            args = new IRubyObject[] { value };
        } else {
            args = toAry(context, value);
        }

        return commonYieldPath(context, args, null, block, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject doYield(ThreadContext context, IRubyObject[] args, IRubyObject self, Block block) {
        if (block.type == Block.Type.LAMBDA) signature.checkArity(context.runtime, args);

        return commonYieldPath(context, args, self, block, Block.NULL_BLOCK);
    }

    protected IRubyObject useBindingSelf(Binding binding) {
        IRubyObject self = binding.getSelf();
        binding.getFrame().setSelf(self);

        return self;
    }

    protected abstract IRubyObject commonYieldPath(ThreadContext context, IRubyObject[] args, IRubyObject self, Block block, Block blockArg);

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
}
