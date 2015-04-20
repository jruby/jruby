package org.jruby.runtime;

import org.jruby.EvalType;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block.Type;
import org.jruby.runtime.builtin.IRubyObject;

public abstract class IRBlockBody extends ContextAwareBlockBody {
    protected String[] parameterList;
    protected final String fileName;
    protected final int lineNumber;
    protected ThreadLocal<EvalType> evalType;

    public IRBlockBody(StaticScope staticScope, String[] parameterList, String fileName, int lineNumber, Signature signature) {
        super(staticScope, signature, -1);
        this.parameterList = parameterList;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.evalType = new ThreadLocal();
        this.evalType.set(EvalType.NONE);
    }

    public void setEvalType(EvalType evalType) {
        this.evalType.set(evalType);
    }

    @Override
    public String[] getParameterList() {
        return parameterList;
    }

    public void setParameterList(String[] parameterList) {
        this.parameterList = parameterList;
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
            IRubyObject[] args = IRRuntimeHelpers.convertValueIntoArgArray(context, arg0, signature.arity(), true);

            // FIXME: arity error is aginst new args but actual error shows arity of original args.
            if (type == Type.LAMBDA) signature.checkArity(context.runtime, args);

            return commonYieldPath(context, args, null, binding, type, Block.NULL_BLOCK);
        } else {
            return yield(context, arg0, binding, type);
        }
    }

    private IRubyObject yieldSpecificMultiArgsCommon(ThreadContext context, IRubyObject[] args, Binding binding, Type type) {
        int blockArity = arity().getValue();
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

    @Override
    public IRubyObject doYield(ThreadContext context, IRubyObject value, Binding binding, Type type) {
        IRubyObject[] args;

        int blockArity = arity().getValue();

        // For lambdas, independent of whether there is a REST arg or not, if # required args is 1,
        // the value is passed through unmodified even when it is an array!
        if ((type == Type.LAMBDA && signature.required() == 1) || (blockArity >= -1 && blockArity <= 1)) {
            args = new IRubyObject[] { value };
        } else {
            IRubyObject val0 = Helpers.aryToAry(value);
            if (!(val0 instanceof RubyArray)) {
                throw context.runtime.newTypeError(value.getType().getName() + "#to_ary should return Array");
            }
            args = ((RubyArray)val0).toJavaArray();
        }

        if (type == Type.LAMBDA) signature.checkArity(context.runtime, args);

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
    public IRubyObject[] prepareArgumentsForCall(ThreadContext context, IRubyObject[] args, Type type) {
        if (type == Type.LAMBDA) {
            signature.checkArity(context.runtime, args);
        } else {
            // SSS FIXME: How is it even possible to "call" a NORMAL block?
            // I thought only procs & lambdas can be called, and blocks are yielded to.
            if (args.length == 1) {
                // Convert value to arg-array, unwrapping where necessary
                args = IRRuntimeHelpers.convertValueIntoArgArray(context, args[0], signature.arity(), (type == Type.NORMAL) && (args[0] instanceof RubyArray));
            } else if (arity().getValue() == 1 && !getSignature().restKwargs()) {
               // discard excess arguments
                args = (args.length == 0) ? context.runtime.getSingleNilArray() : new IRubyObject[] { args[0] };
            }
        }

        return args;
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
