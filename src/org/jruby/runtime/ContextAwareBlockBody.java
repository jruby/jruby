package org.jruby.runtime;

import org.jruby.RubyModule;
import org.jruby.parser.StaticScope;

/**
 * Common type for all block types which share similar values that ThreadContext uses.
 */
public abstract class ContextAwareBlockBody extends BlockBody {
    /** The static scope for the block body */
    protected StaticScope scope;

    /** The 'Arity' of the block */
    protected final Arity arity;

    public ContextAwareBlockBody(StaticScope scope, Arity arity, int argumentType) {
        super(argumentType);
        
        this.scope = scope;
        this.arity = arity;
    }

    protected Frame pre(ThreadContext context, RubyModule klass, Binding binding) {
        return context.preYieldSpecificBlock(binding, scope, klass);
    }

    protected void post(ThreadContext context, Binding binding, Visibility vis, Frame lastFrame) {
        binding.getFrame().setVisibility(vis);
        context.postYield(binding, lastFrame);
    }

    public StaticScope getStaticScope() {
        return scope;
    }

    public void setStaticScope(StaticScope newScope) {
        this.scope = newScope;
    }

    public Arity arity() {
        return arity;
    }

    public Block cloneBlock(Binding binding) {
        // We clone dynamic scope because this will be a new instance of a block.  Any previously
        // captured instances of this block may still be around and we do not want to start
        // overwriting those values when we create a new one.
        // ENEBO: Once we make self, lastClass, and lastMethod immutable we can remove duplicate
        return new Block(this, binding.clone());
    }
}
