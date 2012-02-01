package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.interpreter.Interpreter;
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

    private IRubyObject[] convertValueIntoArgArray(ThreadContext context, IRubyObject value, boolean isYieldSpecific, boolean isArray) {
        // SSS FIXME: But this should not happen -- so, some places in the runtime library are breaking this contract.
        if (isArray && !(value instanceof RubyArray)) isArray = false;

        int blockArity = arity().getValue();
        switch (blockArity) {
            case 0  : return new IRubyObject[] { value };
            case -1 : return isArray ? ((RubyArray)value).toJavaArray() : new IRubyObject[] { value };
            case 1  : {
               if (isArray) {
                   RubyArray valArray = ((RubyArray)value);
                   if (valArray.size() == 0) value = isYieldSpecific ? RubyArray.newEmptyArray(context.getRuntime()) : context.nil;
                   else if (!isYieldSpecific) value = valArray.eltInternal(0);
               }
               return new IRubyObject[] { value };
            }
            default : 
                if (isArray) {
                    RubyArray valArray = (RubyArray)value;
                    if (valArray.size() == 1) value = valArray.eltInternal(0);
                    value = RuntimeHelpers.aryToAry(value);
                    return (value instanceof RubyArray) ? ((RubyArray)value).toJavaArray() : new IRubyObject[] { value };
                } else {
                    value = RuntimeHelpers.aryToAry(value);
                    if (!(value instanceof RubyArray)) {
                        throw context.getRuntime().newTypeError(value.getType().getName() + "#to_ary should return Array");
                    }
                    return ((RubyArray)value).toJavaArray();
                }
        }
    }

    private IRubyObject[] getLambdaArgs(ThreadContext context, IRubyObject value, boolean isYieldSpecific, boolean isArray) {
        IRubyObject[] args = (value == null) ? IRubyObject.NULL_ARRAY : (isArray ? ((RubyArray)value).toJavaArray() : new IRubyObject[] { value });
        arity().checkArity(context.getRuntime(), args);
        return args;
    }

    private IRubyObject[] getYieldArgs(ThreadContext context, IRubyObject value, boolean isYieldSpecific, boolean isArray, Type type) {
        if (type == Block.Type.LAMBDA) {
            return getLambdaArgs(context, value, isYieldSpecific, isArray);
        } else {
            return (value == null) ? IRubyObject.NULL_ARRAY : convertValueIntoArgArray(context, value, isYieldSpecific, isArray);
        }
    }

    // SSS: Looks like yieldSpecific and yieldArray need to treat array unwrapping differently.
    // This is a little baffling to me.  I think the runtime library code needs to turn off 
    // the isArray flag if it wants an array and not create this artificial distinction
    // between the two.  In any case, it looks like in certain contexts, isArray flag is being
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
        return yieldSpecificInternal(context, context.getRuntime().newArrayNoCopyLight(arg0, arg1), null, null, true, binding, type);
    }
    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Binding binding, Block.Type type) {
        return yieldSpecificInternal(context, context.getRuntime().newArrayNoCopyLight(arg0, arg1, arg2), null, null, true, binding, type);
    }

    private IRubyObject yieldSpecificInternal(ThreadContext context, IRubyObject value, IRubyObject self, RubyModule klass, boolean isArray, Binding binding, Type type) {
        IRubyObject[] args = getYieldArgs(context, value, true, isArray, type);
        return commonYieldPath(context, args, self, klass, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject value, IRubyObject self, RubyModule klass, boolean isArray, Binding binding, Type type) {
        IRubyObject[] args = getYieldArgs(context, value, false, isArray, type);
        return commonYieldPath(context, args, self, klass, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject[] prepareArgumentsForCall(ThreadContext context, IRubyObject[] args, Block.Type type) {
        int blockArity = arity().getValue();

        switch (type) {
        // SSS FIXME: How is it even possible to "call" a block?  
        // I thought only procs & lambdas can be called, and blocks are yielded to.
        case NORMAL: 
        case PROC: {
            if (args.length == 1) {
                // Convert value to arg-array, unwrapping where necessary
                args = convertValueIntoArgArray(context, args[0], false, (type == Block.Type.NORMAL) && (args[0] instanceof RubyArray));
            } else if (blockArity == 1) {
                // discard excess arguments
                args = (args.length == 0) ? context.getRuntime().getSingleNilArray() : new IRubyObject[] { args[0] };
            }
            break;
        }
        case LAMBDA:
            if (blockArity == 1 && args.length != 1) {
                if (blockArity != args.length) {
                    context.getRuntime().getWarnings().warn(ID.MULTIPLE_VALUES_FOR_BLOCK, "multiple values for a block parameter (" + args.length + " for " + blockArity + ")");
                }
                args = convertToRubyArray(context, args);
            } else {
                arity().checkArity(context.getRuntime(), args);
            }
            break;
        }

        return args;
    }
}
