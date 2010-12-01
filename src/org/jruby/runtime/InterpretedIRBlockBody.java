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

    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject value, Binding binding, Type type) {
        // FIXME: auto wrapping not quite right
        return call(context, new IRubyObject[] { value }, binding, type);
    }

    private IRubyObject prepareSelf(Binding binding) {
        IRubyObject self = binding.getSelf();
        binding.getFrame().setSelf(self);

        return self;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding, Block.Type type) {
        IRubyObject self = context.getFrameSelf(); // Should not need this and this should probably come from elsewhere
        if (scope.getNumberOfVariables() > 1 && args.length == 1 && args[0] instanceof RubyArray) {
            System.out.println("ARRAY to multi");
            RubyArray array = (RubyArray) args[0];
            int size = array.getLength();
            System.out.println("Creating n variables: " + scope.getNumberOfVariables());
            args = new IRubyObject[scope.getNumberOfVariables()];
            
            int i = 0;
            for (;i < scope.getNumberOfVariables() && i < size; i++) {
                args[i] = array.eltInternal(i);
            }
            
            for (; i < size; i++) {
                args[i] = context.getRuntime().getNil();
            }
        }
        InterpreterContext interp = new NaiveInterpreterContext(context, self, closure.getTemporaryVariableSize(), closure.getRenamedVariableSize(), args, scope, Block.NULL_BLOCK);

        return Interpreter.interpret(context, closure.getCFG(), interp);
    }

    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject value, IRubyObject self, RubyModule klass, boolean aValue, Binding binding, Type type) {
        // FIXME: null self?!?!?!
        // FIXME: null klass!?!?!?!
        // FIXME: args processing
        if (self == null) self = value;
        IRubyObject[] args = new IRubyObject[] { value };
        InterpreterContext interp = new NaiveInterpreterContext(context, self, closure.getTemporaryVariableSize(), closure.getRenamedVariableSize(), args, scope, Block.NULL_BLOCK);

        return Interpreter.interpret(context, closure.getCFG(), interp);
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
