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
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block.Type;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class InterpretedIRBlockBody extends BlockBody {
    /** The static scope for the block body */
    private StaticScope scope;

    /** The arity of the block */
    private final Arity arity;

    private final IRClosure closure;

    private final boolean hasMultipleArgsHead;

    public InterpretedIRBlockBody(IRClosure closure, Arity arity, int argumentType) {
        super(argumentType);

        this.closure = closure;
        this.arity = arity;
        this.scope = closure.getStaticScope();
        hasMultipleArgsHead = false;
    }

    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject value, Binding binding, Type type) {
        IRubyObject self = prepareSelf(binding);

        IRubyObject realArg = setupBlockArg(context.getRuntime(), value, self);
        Visibility oldVis = binding.getFrame().getVisibility();
        Frame lastFrame = pre(context, null, binding);

        try {
//            return callback.call(context, self, realArg);
            return null;
        } catch (JumpException.NextJump nj) {
            // A 'next' is like a local return from the block, ending this call or yield.
            return handleNextJump(context, nj, type);
        } finally {
            post(context, binding, oldVis, lastFrame);
        }
    }

    private IRubyObject prepareSelf(Binding binding) {
        IRubyObject self = binding.getSelf();
        binding.getFrame().setSelf(self);

        return self;
    }

    protected Frame pre(ThreadContext context, RubyModule klass, Binding binding) {
        return context.preYieldSpecificBlock(binding, scope, klass);
    }

    protected void post(ThreadContext context, Binding binding, Visibility vis, Frame lastFrame) {
        binding.getFrame().setVisibility(vis);
        context.postYield(binding, lastFrame);
    }

    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject value, IRubyObject self, RubyModule klass, boolean aValue, Binding binding, Type type) {
        throw new UnsupportedOperationException("Not supported yet2.");
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
    public StaticScope getStaticScope() {
        return scope;
    }

    @Override
    public void setStaticScope(StaticScope newScope) {
        this.scope = newScope;
    }

    @Override
    public Block cloneBlock(Binding binding) {
        binding = binding.clone();
        return new Block(this, binding);
    }

    @Override
    public Arity arity() {
        return arity;
    }
}
