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
    protected ThreadLocal<EvalType> evalType; // null is treated as NONE (@see getEvalType())

    public IRBlockBody(IRScope closure, Signature signature) {
        super(closure.getStaticScope(), signature);
        this.closure = (IRClosure) closure;
        this.fileName = closure.getFileName();
        this.lineNumber = closure.getLineNumber();
        // null (not set) by default to avoid having many thread-local values initialized
        // servers such as Tomcat tend to do thread-local checks when un-deploying apps,
        // for JRuby leads to 100s of SEVERE warnings for a mid-size (booted) Rails app
        this.evalType = new ThreadLocal();
    }

    public final EvalType getEvalType() {
        final EvalType type = this.evalType.get();
        return type == null ? EvalType.NONE : type;
    }

    public void setEvalType(EvalType evalType) {
        this.evalType.set(evalType);
    }

    @Override
    public abstract boolean canCallDirect();

    @Override
    public IRubyObject call(ThreadContext context, Block block) {
        return call(context, block, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0) {
        return call(context, block, new IRubyObject[] {arg0}, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1) {
        return call(context, block, new IRubyObject[] {arg0, arg1}, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return call(context, block, new IRubyObject[]{arg0, arg1, arg2}, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject[] args) {
        return call(context, block, args, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject[] args, Block blockArg) {
        if (canCallDirect()) {
            return callDirect(context, block, args, blockArg);
        } else {
            return commonYieldPath(context, block, Block.Type.PROC, prepareArgumentsForCall(context, args, block.type), null, blockArg);
        }
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block) {
        if (canCallDirect()) {
            return yieldDirect(context, block, null, null);
        } else {
            IRubyObject[] args = IRubyObject.NULL_ARRAY;
            if (block.type == Block.Type.LAMBDA) signature.checkArity(context.runtime, args);
            return commonYieldPath(context, block, Block.Type.NORMAL, args, null, Block.NULL_BLOCK);
        }
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0) {
        IRubyObject[] args;
        if (canCallDirect()) {
            if (arg0 instanceof RubyArray) {
                // Unwrap the array arg
                args = IRRuntimeHelpers.convertValueIntoArgArray(context, arg0, signature, true);
            } else {
                args = new IRubyObject[] { arg0 };
            }
            return yieldDirect(context, block, args, null);
        } else {
            if (arg0 instanceof RubyArray) {
                // Unwrap the array arg
                args = IRRuntimeHelpers.convertValueIntoArgArray(context, arg0, signature, true);

                // FIXME: arity error is aginst new args but actual error shows arity of original args.
                if (block.type == Block.Type.LAMBDA) signature.checkArity(context.runtime, args);

                return commonYieldPath(context, block, Block.Type.NORMAL, args, null, Block.NULL_BLOCK);
            } else {
                return yield(context, block, arg0);
            }
        }
    }

    IRubyObject yieldSpecificMultiArgsCommon(ThreadContext context, Block block, IRubyObject[] args) {
        int blockArity = getSignature().arityValue();
        if (blockArity == 1) {
            args = new IRubyObject[] { RubyArray.newArrayMayCopy(context.runtime, args) };
        }

        if (canCallDirect()) {
            return yieldDirect(context, block, args, null);
        } else {
            if (blockArity == 0) {
                args = IRubyObject.NULL_ARRAY; // discard args
            }
            if (block.type == Block.Type.LAMBDA) signature.checkArity(context.runtime, args);

            return commonYieldPath(context, block, Block.Type.NORMAL, args, null, Block.NULL_BLOCK);
        }
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1) {
        return yieldSpecificMultiArgsCommon(context, block, new IRubyObject[]{arg0, arg1});
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return yieldSpecificMultiArgsCommon(context, block, new IRubyObject[]{arg0, arg1, arg2});
    }

    private IRubyObject[] toAry(ThreadContext context, IRubyObject value) {
        IRubyObject val0 = Helpers.aryToAry(value);

        if (val0.isNil()) return new IRubyObject[] { value };

        if (!(val0 instanceof RubyArray)) {
            throw context.runtime.newTypeError(value.getType().getName() + "#to_ary should return Array");
        }

        return ((RubyArray)val0).toJavaArray();
    }

    protected IRubyObject doYieldLambda(ThreadContext context, Block block, IRubyObject value) {
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

        return commonYieldPath(context, block, Block.Type.NORMAL, args, null, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject doYield(ThreadContext context, Block block, IRubyObject value) {
        if (block.type == Block.Type.LAMBDA) return doYieldLambda(context, block, value);

        int blockArity = getSignature().arityValue();

        IRubyObject[] args;
        if (value == null) { // no args case from BlockBody.yieldSpecific
            args = IRubyObject.NULL_ARRAY;
        } else if (!getSignature().hasKwargs() && blockArity >= -1 && blockArity <= 1) {
            args = new IRubyObject[] { value };
        } else {
            args = toAry(context, value);
        }

        return commonYieldPath(context, block, Block.Type.NORMAL, args, null, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject doYield(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self) {
        if (block.type == Block.Type.LAMBDA) signature.checkArity(context.runtime, args);

        return commonYieldPath(context, block, Block.Type.NORMAL, args, self, Block.NULL_BLOCK);
    }

    protected IRubyObject commonYieldPath(ThreadContext context, Block block, Block.Type type, IRubyObject[] args, IRubyObject self, Block blockArg) {
        throw new RuntimeException("commonYieldPath not implemented in base class. We should never get here.");
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
}
