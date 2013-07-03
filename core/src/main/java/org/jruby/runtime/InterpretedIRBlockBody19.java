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
        // SSS FIXME: But this should not happen -- so, some places in the runtime library are breaking this contract.
        if (argIsArray && !(value instanceof RubyArray)) argIsArray = false;

        int blockArity = arity().getValue();
        switch (blockArity) {
            case 0  : return new IRubyObject[] { value };
            case -1 : return argIsArray ? ((RubyArray)value).toJavaArray() : new IRubyObject[] { value };
            case 1  : {
               if (argIsArray) {
                   RubyArray valArray = ((RubyArray)value);
                   if (valArray.size() == 0) {
                       value = passArrayArg ? RubyArray.newEmptyArray(context.runtime) : context.nil;
                   }
                   else if (!passArrayArg) value = valArray.eltInternal(0);
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

    private IRubyObject[] getLambdaArgs(ThreadContext context, IRubyObject value, boolean passArrayArg, boolean argIsArray) {
        IRubyObject[] args = (value == null) ? IRubyObject.NULL_ARRAY : (argIsArray ? ((RubyArray)value).toJavaArray() : new IRubyObject[] { value });
        arity().checkArity(context.runtime, args);
        return args;
    }

    private IRubyObject[] getYieldArgs(ThreadContext context, IRubyObject value, boolean passArrayArg, boolean argIsArray, Type type) {
        if (type == Block.Type.LAMBDA) {
            return getLambdaArgs(context, value, passArrayArg, argIsArray);
        } else {
            return (value == null) ? IRubyObject.NULL_ARRAY : convertValueIntoArgArray(context, value, passArrayArg, argIsArray);
        }
    }

    // SSS: Looks like yieldSpecific and yieldArray need to treat array unwrapping differently.
    // This is a little baffling to me.  I think the runtime library code needs to turn off 
    // the argIsArray flag if it wants an array and not create this artificial distinction
    // between the two.  In any case, it looks like in certain contexts, argIsArray flag is being
    // passed in as true even when the argument is not an array.
    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Binding binding, Block.Type type) {
        return yieldSpecificInternal(context, null, null, null, true, binding, type);
    }
    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, Binding binding, Block.Type type) {
        return yieldSpecificInternal(context, arg0, null, null, true, binding, type);
    }
    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Binding binding, Block.Type type) {
        return yieldSpecificInternal(context, context.runtime.newArrayNoCopyLight(arg0, arg1), null, null, true, binding, type);
    }
    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Binding binding, Block.Type type) {
        return yieldSpecificInternal(context, context.runtime.newArrayNoCopyLight(arg0, arg1, arg2), null, null, true, binding, type);
    }

    private IRubyObject yieldSpecificInternal(ThreadContext context, IRubyObject value, IRubyObject self, RubyModule klass, boolean argIsArray, Binding binding, Type type) {
		  // Do not unwrap the array arg
        IRubyObject[] args = getYieldArgs(context, value, true, argIsArray, type);
        return commonYieldPath(context, args, self, klass, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject value, IRubyObject self, RubyModule klass, boolean argIsArray, Binding binding, Type type) {
		  // Unwrap the array arg
        IRubyObject[] args = getYieldArgs(context, value, false, argIsArray, type);
        return commonYieldPath(context, args, self, klass, binding, type, Block.NULL_BLOCK);
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
