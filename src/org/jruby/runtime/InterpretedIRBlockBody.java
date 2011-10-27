package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.interpreter.Interpreter;
import org.jruby.interpreter.NaiveInterpreterContext;
import org.jruby.runtime.Block.Type;
import org.jruby.runtime.builtin.IRubyObject;

public class InterpretedIRBlockBody extends ContextAwareBlockBody {
    private final IRClosure closure;

    public InterpretedIRBlockBody(IRClosure closure, Arity arity, int argumentType) {
        super(closure.getStaticScope(), arity, argumentType);
        this.closure = closure;
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
    public IRubyObject yield(ThreadContext context, IRubyObject value, Binding binding, Type type) {
        return yield(context, value, null, null, false, binding, type);
    }

    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject value, IRubyObject self, RubyModule klass, boolean isArray, Binding binding, Type type) {
        IRubyObject[] args;
        if (isArray) {
            if (arity().getValue() > 1) {
                args = value == null ? IRubyObject.NULL_ARRAY : prepareArgumentsForYield(context, ((RubyArray)value).toJavaArray(), type);
            } else {
                args = assignArrayToBlockArgs(context.getRuntime(), value);
            }
        } else {
            args = prepareArgumentsForYield(context, value == null ? IRubyObject.NULL_ARRAY : new IRubyObject[] { value }, type);
        }

        return commonYieldPath(context, args, self, klass, binding, type, Block.NULL_BLOCK);
    }

    private IRubyObject prepareSelf(Binding binding) {
        IRubyObject self = binding.getSelf();
        binding.getFrame().setSelf(self);

        return self;
    }

    private IRubyObject commonYieldPath(ThreadContext context, IRubyObject[] args, IRubyObject self, RubyModule klass, Binding binding, Type type, Block block) {
        Visibility oldVis = binding.getFrame().getVisibility();
        RubyModule currentModule = closure.getStaticScope().getModule();

        Frame prevFrame = context.preYieldNoScope(binding, klass);
        if (klass == null) self = prepareSelf(binding);
        try {
            DynamicScope prevScope = binding.getDynamicScope();
            DynamicScope newScope  = closure.isForLoopBody() ? prevScope : DynamicScope.newDynamicScope(closure.getStaticScope(), prevScope);
            context.pushScope(newScope);
            NaiveInterpreterContext interp = new NaiveInterpreterContext(context, closure, currentModule, self, null, args, block, type);
            return Interpreter.interpret(context, self, closure, interp);
        }
        finally {
            binding.getFrame().setVisibility(oldVis);
            context.postYield(binding, prevFrame);
        }
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

    private void blockArgWarning(Ruby ruby, int length) {
        ruby.getWarnings().warn(ID.MULTIPLE_VALUES_FOR_BLOCK, "multiple values for a block parameter (" +
                    length + " for 1)");
    }

    protected IRubyObject[] assignArrayToBlockArgs(Ruby ruby, IRubyObject value) {
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

    public IRubyObject[] prepareArgumentsForYield(ThreadContext context, IRubyObject[] args, Block.Type type) {
        int blockArity = arity().getValue();
        if (type != Block.Type.NORMAL) throw new RuntimeException("JRuby Internal Error.  Trying to yield to a " + type + " block.  Only NORMAL blocks can be yielded to.");

        if (args.length == 1) {
            IRubyObject soleArg = args[0];
            if (soleArg instanceof RubyArray) {
                if (argumentType == MULTIPLE_ASSIGNMENT) args = ((RubyArray) soleArg).toJavaArray();
            } else if (blockArity > 1) {
                IRubyObject toAryArg = RuntimeHelpers.aryToAry(soleArg);
                if (toAryArg instanceof RubyArray) args = ((RubyArray)toAryArg).toJavaArray();
                else throw context.getRuntime().newTypeError(soleArg.getType().getName() + "#to_ary should return Array");
            }
        } else if (argumentType == ARRAY) {
            context.getRuntime().getWarnings().warn(ID.MULTIPLE_VALUES_FOR_BLOCK, "multiple values for a block parameter (" + args.length + " for " + blockArity + ")");
            if (args.length == 0) {
                args = context.getRuntime().getSingleNilArray();
            } else {
                args = new IRubyObject[] {context.getRuntime().newArrayNoCopy(args)};
            }
        }

        return args;
    }

    public IRubyObject[] prepareArgumentsForCall(ThreadContext context, IRubyObject[] args, Block.Type type) {
        int blockArity = arity().getValue();
        switch (type) {
        // SSS FIXME: How is it even possible to "call" a block?  
        // I thought only procs & lambdas can be called, and blocks are yielded to.
        case NORMAL: 
        case PROC: {
            if (args.length == 1) {
                IRubyObject soleArg = args[0];
                if (soleArg instanceof RubyArray) {
                    if ((argumentType == MULTIPLE_ASSIGNMENT) || ((argumentType == SINGLE_RESTARG) && (type == Block.Type.NORMAL))) {
                        args = ((RubyArray) soleArg).toJavaArray();
                    }
                } else if (blockArity > 1) {
                    IRubyObject toAryArg = RuntimeHelpers.aryToAry(soleArg);
                    if (toAryArg instanceof RubyArray) args = ((RubyArray)toAryArg).toJavaArray();
                    else throw context.getRuntime().newTypeError(soleArg.getType().getName() + "#to_ary should return Array");
                }
            } else if (argumentType == ARRAY) {
                context.getRuntime().getWarnings().warn(ID.MULTIPLE_VALUES_FOR_BLOCK, "multiple values for a block parameter (" + args.length + " for " + blockArity + ")");
                if (args.length == 0) {
                    args = context.getRuntime().getSingleNilArray();
                } else {
                    args = new IRubyObject[] {context.getRuntime().newArrayNoCopy(args)};
                }
            }
            break;
        }
        case LAMBDA:
            if (argumentType == ARRAY && args.length != 1) {
                context.getRuntime().getWarnings().warn(ID.MULTIPLE_VALUES_FOR_BLOCK, "multiple values for a block parameter (" + args.length + " for " + blockArity + ")");
                if (args.length == 0) {
                    args = context.getRuntime().getSingleNilArray();
                } else {
                    args = new IRubyObject[] {context.getRuntime().newArrayNoCopy(args)};
                }
            } else {
                arity().checkArity(context.getRuntime(), args);
            }
            break;
        }

        return args;
    }

    @Override
    public String getFile() {
        // FIXME: need to get position from IR somehow?
        return "(unknown)";
    }

    @Override
    public int getLine() {
        // FIXME: need to get position from IR somehow?
        return -1;
    }
}
