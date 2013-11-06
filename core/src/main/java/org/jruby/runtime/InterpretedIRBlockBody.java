package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.ir.IRClosure;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.runtime.Block.Type;
import org.jruby.runtime.builtin.IRubyObject;

public class InterpretedIRBlockBody extends ContextAwareBlockBody {
    protected final IRClosure closure;

    public InterpretedIRBlockBody(IRClosure closure, Arity arity, int argumentType) {
        super(closure.getStaticScope(), arity, -1);
        this.closure = closure;
    }

    @Override
    public String[] getParameterList() {
        return this.closure.getParameterList();
    }

    @Override
    public IRubyObject call(ThreadContext context, Binding binding, Block.Type type) {
        return call(context, IRubyObject.NULL_ARRAY, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject arg0, Binding binding, Block.Type type) {
        return call(context, new IRubyObject[] {arg0}, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Binding binding, Block.Type type) {
        return call(context, new IRubyObject[] {arg0, arg1}, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Binding binding, Block.Type type) {
        return call(context, new IRubyObject[] {arg0, arg1, arg2}, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding, Block.Type type) {
        return call(context, args, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding, Block.Type type, Block block) {
        return commonYieldPath(context, prepareArgumentsForCall(context, args, type), null, null, binding, type, block);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Binding binding, Block.Type type) {
        IRubyObject[] args = IRubyObject.NULL_ARRAY;
        if (type == Block.Type.LAMBDA) {
            arity().checkArity(context.runtime, args);
        }
        return commonYieldPath(context, args, null, null, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, Binding binding, Block.Type type) {
        if (arg0 instanceof RubyArray) {
		    // Unwrap the array arg
            IRubyObject[] args;
            if (type == Block.Type.LAMBDA) {
                args = ((RubyArray)arg0).toJavaArray();
                arity().checkArity(context.runtime, args);
            } else {
                args = convertValueIntoArgArray(context, arg0, true, true);
            }
            return commonYieldPath(context, args, null, null, binding, type, Block.NULL_BLOCK);
        } else {
            return yield(context, arg0, binding, type);
        }
    }

    private IRubyObject[] convertValueIntoArgArray(ThreadContext context, IRubyObject value, boolean passArrayArg, boolean argIsArray) {
        // SSS FIXME: This should not really happen -- so, some places in the runtime library are breaking this contract.
        if (argIsArray && !(value instanceof RubyArray)) argIsArray = false;

        int blockArity = arity().getValue();
        switch (blockArity) {
            case -1 : return argIsArray ? ((RubyArray)value).toJavaArray() : new IRubyObject[] { value };
            case  0 : return new IRubyObject[] { value };
            case  1 : {
               if (argIsArray) {
                   RubyArray valArray = ((RubyArray)value);
                   if (valArray.size() == 0) {
                       value = passArrayArg ? RubyArray.newEmptyArray(context.runtime) : context.nil;
                   } else if (!passArrayArg) {
                       value = valArray.eltInternal(0);
                   }
               }
               return new IRubyObject[] { value };
            }
            default :
                if (argIsArray) {
                    RubyArray valArray = (RubyArray)value;
                    if (valArray.size() == 1) value = valArray.eltInternal(0);
                    value = Helpers.aryToAry(value);
                    return (value instanceof RubyArray) ? ((RubyArray)value).toJavaArray() : new IRubyObject[] { value };
                } else {
                    IRubyObject val0 = Helpers.aryToAry(value);
                    if (!(val0 instanceof RubyArray)) {
                        throw context.runtime.newTypeError(value.getType().getName() + "#to_ary should return Array");
                    }
                    return ((RubyArray)val0).toJavaArray();
                }
        }
    }

    private IRubyObject yieldSpecificMultiArgsCommon(ThreadContext context, IRubyObject[] args, Binding binding, Block.Type type) {
        if (type == Block.Type.LAMBDA) {
            arity().checkArity(context.runtime, args);
        } else {
            int blockArity = arity().getValue();
            if (blockArity == 0) {
                args = IRubyObject.NULL_ARRAY; // discard args
            } else if (blockArity == 1) {
                args = new IRubyObject[] { RubyArray.newArrayNoCopy(context.runtime, args) };
            }
        }
        return commonYieldPath(context, args, null, null, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Binding binding, Block.Type type) {
        return yieldSpecificMultiArgsCommon(context, new IRubyObject[] { arg0, arg1 }, binding, type);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Binding binding, Block.Type type) {
        return yieldSpecificMultiArgsCommon(context, new IRubyObject[] { arg0, arg1, arg2 }, binding, type);
    }

    @Override
    public IRubyObject doYield(ThreadContext context, IRubyObject value, Binding binding, Type type) {
        IRubyObject[] args;
        if (type == Block.Type.LAMBDA) {
            args = new IRubyObject[] { value };
            arity().checkArity(context.runtime, args);
        } else {
            int blockArity = arity().getValue();
            if (blockArity >= -1 && blockArity <= 1) {
                args = new IRubyObject[] { value };
            } else {
                IRubyObject val0 = Helpers.aryToAry(value);
                if (!(val0 instanceof RubyArray)) {
                    throw context.runtime.newTypeError(value.getType().getName() + "#to_ary should return Array");
                }
                args = ((RubyArray)val0).toJavaArray();
            }
        }
        return commonYieldPath(context, args, null, null, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject doYield(ThreadContext context, IRubyObject[] args, IRubyObject self, RubyModule klass, boolean argIsArray, Binding binding, Type type) {
        args = (args == null) ? IRubyObject.NULL_ARRAY : args;
        if (type == Block.Type.LAMBDA) {
            arity().checkArity(context.runtime, args);
        }
        return commonYieldPath(context, args, self, klass, binding, type, Block.NULL_BLOCK);
    }

    protected IRubyObject prepareSelf(Binding binding) {
        IRubyObject self = binding.getSelf();
        binding.getFrame().setSelf(self);

        return self;
    }

    protected IRubyObject commonYieldPath(ThreadContext context, IRubyObject[] args, IRubyObject self, RubyModule klass, Binding binding, Type type, Block block) {
        // SSS: Important!  Use getStaticScope() to use a copy of the static-scope stored in the block-body.
        // Do not use 'closure.getStaticScope()' -- that returns the original copy of the static scope.
        // This matters because blocks created for Thread bodies modify the static-scope field of the block-body
        // that records additional state about the block body.
        //
        // FIXME: Rather than modify static-scope, it seems we ought to set a field in block-body which is then
        // used to tell dynamic-scope that it is a dynamic scope for a thread body.  Anyway, to be revisited later!
        Visibility oldVis = binding.getFrame().getVisibility();
        Frame prevFrame = context.preYieldNoScope(binding, klass);
        if (klass == null) self = prepareSelf(binding);
        try {
            DynamicScope prevScope = binding.getDynamicScope();
            DynamicScope newScope  = closure.isForLoopBody() ? prevScope : DynamicScope.newDynamicScope(getStaticScope(), prevScope);
            context.pushScope(newScope);
            return Interpreter.INTERPRET_BLOCK(context, self, closure, args, binding.getMethod(), block, type);
        }
        finally {
            binding.getFrame().setVisibility(oldVis);
            context.postYield(binding, prevFrame);
        }
    }

    protected void blockArgWarning(Ruby ruby, int length) {
        ruby.getWarnings().warn(ID.MULTIPLE_VALUES_FOR_BLOCK, "multiple values for a block parameter (" +
                    length + " for 1)");
    }
    
    private IRubyObject prepareArrayArgsForCall(Ruby ruby, IRubyObject value) {
        int length = (value instanceof RubyArray) ? ((RubyArray)value).getLength() : 0;
        switch (length) {
        case 0: return ruby.getNil();
        case 1: return ((RubyArray)value).eltInternal(0);
        default: blockArgWarning(ruby, length);
        }
        return value;
    }

    private IRubyObject[] assignArrayToBlockArgs(Ruby ruby, IRubyObject value) {
        switch (argumentType) {
        case ZERO_ARGS:
            return IRubyObject.NULL_ARRAY;
        case MULTIPLE_ASSIGNMENT:
        case SINGLE_RESTARG:
            return (value == null) ? IRubyObject.NULL_ARRAY : ((value instanceof RubyArray) ? ((RubyArray)value).toJavaArrayMaybeUnsafe() : new IRubyObject[] { value } );
        default:
            return new IRubyObject[] {prepareArrayArgsForCall(ruby, value)};
        }
    }

    protected IRubyObject[] convertToRubyArray(ThreadContext context, IRubyObject[] args) {
        return (args.length == 0) ? context.runtime.getSingleNilArray()
                                  : new IRubyObject[] {context.runtime.newArrayNoCopy(args)};
    }

    protected IRubyObject[] prepareArgumentsForYield(ThreadContext context, IRubyObject[] args, Block.Type type) {
        // SSS FIXME: Hmm .. yield can yield to blocks other than NORMAL block type as well.
        int blockArity = arity().getValue();

        if (args.length == 1) {
            IRubyObject soleArg = args[0];
            if (soleArg instanceof RubyArray) {
                if (argumentType == MULTIPLE_ASSIGNMENT) args = ((RubyArray) soleArg).toJavaArray();
            } else if (blockArity > 1) {
                IRubyObject toAryArg = Helpers.aryToAry(soleArg);
                if (toAryArg instanceof RubyArray) args = ((RubyArray)toAryArg).toJavaArray();
                else {
                    throw context.runtime.newTypeError(soleArg.getType().getName() + "#to_ary should return Array");
                }
            }
        } else if (argumentType == ARRAY) {
            args = convertToRubyArray(context, args);
        }

        return args;
    }

    @Override
    public IRubyObject[] prepareArgumentsForCall(ThreadContext context, IRubyObject[] args, Block.Type type) {
        if (type == Block.Type.LAMBDA) {
            arity().checkArity(context.runtime, args);
        } else {
            // SSS FIXME: How is it even possible to "call" a NORMAL block?
            // I thought only procs & lambdas can be called, and blocks are yielded to.
            if (args.length == 1) {
                // Convert value to arg-array, unwrapping where necessary
                args = convertValueIntoArgArray(context, args[0], true, (type == Block.Type.NORMAL) && (args[0] instanceof RubyArray));
            } else if (arity().getValue() == 1) {
               // discard excess arguments
                args = (args.length == 0) ? context.runtime.getSingleNilArray() : new IRubyObject[] { args[0] };
            }
        }

        return args;
    }

    @Override
    public String getFile() {
        return closure.getFileName();
    }

    @Override
    public int getLine() {
        return closure.getLineNumber();
    }
}
