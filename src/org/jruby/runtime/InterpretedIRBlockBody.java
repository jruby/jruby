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

    public IRubyObject commonCallPath(ThreadContext context, IRubyObject[] args, IRubyObject self, RubyModule klass, boolean aValue, Binding binding, Type type, Block block) {
        // FIXME: null self?!?!?!
        // FIXME: null klass!?!?!?!
        // aValue is not used??

        RubyModule currentModule = closure.getStaticScope().getModule();
        context.getCurrentScope().getStaticScope().setModule(currentModule);

        InterpreterContext interp = new NaiveInterpreterContext(context, currentModule, self, null, closure.getLocalVariablesCount(), closure.getTemporaryVariableSize(), closure.getRenamedVariableSize(), args, block);
        interp.setDynamicScope(binding.getDynamicScope());

        return Interpreter.interpret(context, closure.getCFG(), interp);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding, Block.Type type) {
        // SSS FIXME: Is this correct?
        IRubyObject self = context.getFrameSelf(); // ENEBO: Should not need this and this should probably come from elsewhere
        args = prepareArgumentsForCall(context, args, type);

        // SSS FIXME: aValue is false here -- rest of them below are true
        return commonCallPath(context, args, self, null, false, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding, Block.Type type, Block block) {
        // SSS FIXME: Is this correct? null self unlike the fixed up one above
        args = prepareArgumentsForCall(context, args, type);
        return commonCallPath(context, args, null, null, true, binding, type, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, Binding binding, Block.Type type) {
        // SSS FIXME: Is this correct? null self unlike the fixed up one above
        IRubyObject[] args = prepareArgumentsForCall(context, IRubyObject.NULL_ARRAY, type);
        return commonCallPath(context, args, null, null, true, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject arg0, Binding binding, Block.Type type) {
        // SSS FIXME: Is this correct? null self unlike the fixed up one above
        IRubyObject[] args = new IRubyObject[] {arg0};
        args = prepareArgumentsForCall(context, args, type);
        return commonCallPath(context, args, null, null, true, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Binding binding, Block.Type type) {
        // SSS FIXME: Is this correct? null self unlike the fixed up one above
        IRubyObject[] args = new IRubyObject[] {arg0, arg1};
        args = prepareArgumentsForCall(context, args, type);
        return commonCallPath(context, args, null, null, true, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Binding binding, Block.Type type) {
        // SSS FIXME: Is this correct? null self
        IRubyObject[] args = new IRubyObject[] {arg0, arg1, arg2};
        args = prepareArgumentsForCall(context, args, type);
        return commonCallPath(context, args, null, null, true, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Binding binding, Block.Type type) {
        // SSS FIXME: Is this correct? null self
        return commonCallPath(context, new IRubyObject[] {arg0, arg1}, null, null, true, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Binding binding, Block.Type type) {
        // SSS FIXME: Is this correct? null self
        return commonCallPath(context, new IRubyObject[] {arg0, arg1, arg2}, null, null, true, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject value, Binding binding, Type type) {
        // SSS FIXME: Is this correct?
        IRubyObject self = context.getFrameSelf(); // ENEBO: Should not need this and this should probably come from elsewhere
        IRubyObject[] args = prepareArgumentsForCall(context, new IRubyObject[] { value }, type);
        return commonCallPath(context, args, self, null, false, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject value, IRubyObject self, RubyModule klass, boolean aValue, Binding binding, Type type) {
        // FIXME: null self?!?!?!
        // FIXME: null klass!?!?!?!
        if (self == null) self = value; // SSS FIXME: Correct?
        IRubyObject[] args = prepareArgumentsForCall(context, new IRubyObject[] { value }, type);
        return commonCallPath(context, args, self, null, true, binding, type, Block.NULL_BLOCK);
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
