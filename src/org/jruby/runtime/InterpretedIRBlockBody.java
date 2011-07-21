/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.exceptions.JumpException;
import org.jruby.interpreter.Interpreter;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.interpreter.NaiveInterpreterContext;
import org.jruby.runtime.Block.Type;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class InterpretedIRBlockBody extends ContextAwareBlockBody {
    private final IRClosure closure;

    private final boolean hasMultipleArgsHead;

    public InterpretedIRBlockBody(IRClosure closure, Arity arity, int argumentType) {
        super(closure.getStaticScope(), arity, argumentType);

        this.closure = closure;
        this.hasMultipleArgsHead = false;
    }

    private IRubyObject prepareSelf(Binding binding) {
        IRubyObject self = binding.getSelf();
        binding.getFrame().setSelf(self);

        return self;
    }

    public IRubyObject commonCallPath(ThreadContext context, IRubyObject[] args, IRubyObject self, RubyModule klass, boolean isArray, Binding binding, Type type, Block block) {
        // SSS FIXME: Is this correct?
        // isArray is not used??
        if (klass == null) {
            self = prepareSelf(binding);
        }

        // SSS FIXME: Can it happen that (type != block.type)?
        // if (type != block.type) System.out.println("incoming block type is different from block.type");

        RubyModule currentModule = closure.getStaticScope().getModule();
        context.getCurrentScope().getStaticScope().setModule(currentModule);

        InterpreterContext interp = new NaiveInterpreterContext(context, currentModule, self, null, closure.getLocalVariablesCount(), closure.getTemporaryVariableSize(), closure.getRenamedVariableSize(), args, block, type);
        interp.setDynamicScope(binding.getDynamicScope());

        return Interpreter.interpret(context, closure.getCFG(), interp);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding, Block.Type type) {
        // SSS FIXME: Is this correct?
        IRubyObject self = prepareSelf(binding);
        args = prepareArgumentsForCall(context, args, type);
        // SSS FIXME: isArray is false here -- rest of them below are true
        return commonCallPath(context, args, self, null, false, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding, Block.Type type, Block block) {
        args = prepareArgumentsForCall(context, args, type);
        return commonCallPath(context, args, null, null, true, binding, type, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, Binding binding, Block.Type type) {
        IRubyObject[] args = prepareArgumentsForCall(context, IRubyObject.NULL_ARRAY, type);
        return commonCallPath(context, args, null, null, true, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject arg0, Binding binding, Block.Type type) {
        IRubyObject[] args = new IRubyObject[] {arg0};
        args = prepareArgumentsForCall(context, args, type);
        return commonCallPath(context, args, null, null, true, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Binding binding, Block.Type type) {
        IRubyObject[] args = new IRubyObject[] {arg0, arg1};
        args = prepareArgumentsForCall(context, args, type);
        return commonCallPath(context, args, null, null, true, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Binding binding, Block.Type type) {
        IRubyObject[] args = new IRubyObject[] {arg0, arg1, arg2};
        args = prepareArgumentsForCall(context, args, type);
        return commonCallPath(context, args, null, null, true, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Binding binding, Block.Type type) {
        return commonCallPath(context, new IRubyObject[] {arg0, arg1}, null, null, true, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Binding binding, Block.Type type) {
        return commonCallPath(context, new IRubyObject[] {arg0, arg1, arg2}, null, null, true, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject value, Binding binding, Type type) {
        // SSS FIXME: Is this correct?
        IRubyObject self = prepareSelf(binding);
        IRubyObject[] args = prepareArgumentsForCall(context, new IRubyObject[] { value }, type);
        return commonCallPath(context, args, self, null, false, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject value, IRubyObject self, RubyModule klass, boolean isArray, Binding binding, Type type) {
        // SSS FIXME: if we already have an array, I think we dont need to call prepareArgumentsForCall
        IRubyObject[] args = prepareArgumentsForCall(context, isArray ? ((RubyArray) value).toJavaArray(): new IRubyObject[] { value }, type);
        return commonCallPath(context, args, self, klass, isArray, binding, type, Block.NULL_BLOCK);
    }

    private IRubyObject handleNextJump(ThreadContext context, JumpException.NextJump nj, Block.Type type) {
        return nj.getValue() == null ? context.getRuntime().getNil() : (IRubyObject)nj.getValue();
    }
    
    protected IRubyObject setupBlockArgs(ThreadContext context, IRubyObject value, IRubyObject self) {
        switch (argumentType) {
        case ZERO_ARGS:
            return null;
        case MULTIPLE_ASSIGNMENT:
        case SINGLE_RESTARG:
            return value;
        default:
            return defaultArgsLogic(context.getRuntime(), value);
        }
    }
    
    private IRubyObject defaultArgsLogic(Ruby ruby, IRubyObject value) {
        int length = ArgsUtil.arrayLength(value);
        switch (length) {
        case 0:
            return ruby.getNil();
        case 1:
            return ((RubyArray)value).eltInternal(0);
        default:
            blockArgWarning(ruby, length);
        }
        return value;
    }
    
    private IRubyObject warnMultiReturnNil(Ruby ruby) {
        ruby.getWarnings().warn(ID.MULTIPLE_VALUES_FOR_BLOCK, "multiple values for a block parameter (0 for 1)");
        return ruby.getNil();
    }    
    
    private void blockArgWarning(Ruby ruby, int length) {
        ruby.getWarnings().warn(ID.MULTIPLE_VALUES_FOR_BLOCK, "multiple values for a block parameter (" +
                    length + " for 1)");
    }

    protected IRubyObject setupBlockArg(Ruby ruby, IRubyObject value, IRubyObject self) {
        switch (argumentType) {
        case ZERO_ARGS:
            return null;
        case MULTIPLE_ASSIGNMENT:
        case SINGLE_RESTARG:
            return ArgsUtil.convertToRubyArray(ruby, value, hasMultipleArgsHead);
        default:
            return defaultArgLogic(ruby, value);
        }
    }

    @Override
    public IRubyObject[] prepareArgumentsForCall(ThreadContext context, IRubyObject[] args, Block.Type type) {
        switch (type) {
        // SSS FIXME: this is different from how regular interpreter does it .. it treats PROC & NORMAL blocks differently
        // But, without this fix, {:a=>:b}.each { |*x| puts a.inspect } outputs [:a,:b] instead of [[:a, :b]]
        case PROC:
        case NORMAL: {
            if (args.length == 1 && args[0] instanceof RubyArray) {
                if (argumentType == MULTIPLE_ASSIGNMENT) {
                    args = ((RubyArray) args[0]).toJavaArray();
                }
                break;
            }
        }
        case LAMBDA:
            if (argumentType == ARRAY && args.length != 1) {
                context.getRuntime().getWarnings().warn(ID.MULTIPLE_VALUES_FOR_BLOCK, "multiple values for a block parameter (" + args.length + " for " + arity().getValue() + ")");
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

    private IRubyObject defaultArgLogic(Ruby ruby, IRubyObject value) {
        if (value == null) {
            return warnMultiReturnNil(ruby);
        }
        return value;
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
