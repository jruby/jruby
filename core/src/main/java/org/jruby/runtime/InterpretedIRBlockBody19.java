package org.jruby.runtime;

import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.ir.IRClosure;
import org.jruby.runtime.Block.Type;
import org.jruby.runtime.builtin.IRubyObject;

public class InterpretedIRBlockBody19 extends InterpretedIRBlockBody {
    public InterpretedIRBlockBody19(IRClosure closure, Arity arity, int argumentType) {
        super(closure, arity, -1);
    }

    @Override
    public String[] getParameterList() {
        return this.closure.getParameterList();
    }

    private IRubyObject[] convertValueIntoArgArray(ThreadContext context, IRubyObject value, boolean passArrayArg, boolean argIsArray) {
        // SSS FIXME: This should not really happen -- so, some places in the runtime library are breaking this contract.
        if (argIsArray && !(value instanceof RubyArray)) argIsArray = false;

        int blockArity = arity().getValue();
        switch (blockArity) {
            case -1 : return argIsArray ? ((RubyArray)value).toJavaArray() : new IRubyObject[] { value };
            case 0  : return new IRubyObject[] { value };
            case 1  : {
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
                    value = Helpers.aryToAry(value);
                    if (!(value instanceof RubyArray)) {
                        throw context.runtime.newTypeError(value.getType().getName() + "#to_ary should return Array");
                    }
                    return ((RubyArray)value).toJavaArray();
                }
        }
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
        IRubyObject[] args;
        if (type == Block.Type.LAMBDA) {
            args = arg0 instanceof RubyArray ? ((RubyArray)arg0).toJavaArray() : new IRubyObject[] { arg0 };
            arity().checkArity(context.runtime, args);
        } else if (arg0 instanceof RubyArray) {
            args = convertValueIntoArgArray(context, arg0, true, true);
        } else if (arity().getValue() <= 1) {
            args = new IRubyObject[] { arg0 };
        } else {
            IRubyObject value = Helpers.aryToAry(arg0);
            if (!(value instanceof RubyArray)) {
                throw context.runtime.newTypeError(arg0.getType().getName() + "#to_ary should return Array");
            }
            args = ((RubyArray)value).toJavaArray();
        }
        return commonYieldPath(context, args, null, null, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Binding binding, Block.Type type) {
        IRubyObject[] args = new IRubyObject[] { arg0, arg1 };
        if (type == Block.Type.LAMBDA) {
            arity().checkArity(context.runtime, args);
        } else {
            int arity = arity().getValue();
            if (arity == 0) {
                args = IRubyObject.NULL_ARRAY; // discard args
            } else if (arity == 1) {
                args = new IRubyObject[] { RubyArray.newArrayNoCopy(context.runtime, args) };
            }
        }
        return commonYieldPath(context, args, null, null, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Binding binding, Block.Type type) {
        IRubyObject[] args = new IRubyObject[] { arg0, arg1, arg2 };
        if (type == Block.Type.LAMBDA) {
            arity().checkArity(context.runtime, args);
        } else {
            int arity = arity().getValue();
            if (arity == 0) {
                args = IRubyObject.NULL_ARRAY; // discard args
            } else if (arity == 1) {
                args = new IRubyObject[] { RubyArray.newArrayNoCopy(context.runtime, args) };
            }
        }
        return commonYieldPath(context, args, null, null, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject[] args, IRubyObject self, RubyModule klass, boolean argIsArray, Binding binding, Type type) {
		// Unwrap the array arg
        IRubyObject[] newArgs;
        if (type == Block.Type.LAMBDA) {
            newArgs = (args == null) ? IRubyObject.NULL_ARRAY : args;
            arity().checkArity(context.runtime, args);
        } else {
            newArgs = (args == null) ? IRubyObject.NULL_ARRAY : args;
        }
        return commonYieldPath(context, newArgs, self, klass, binding, type, Block.NULL_BLOCK);
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
}
