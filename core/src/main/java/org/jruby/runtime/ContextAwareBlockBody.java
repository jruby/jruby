package org.jruby.runtime;

import org.jruby.parser.StaticScope;

/**
 * Common type for all block types which share similar values that ThreadContext uses.
 */
public abstract class ContextAwareBlockBody extends BlockBody {
    /** The static scope for the block body */
    protected StaticScope scope;

    /** The 'Arity' of the block */
    protected final Signature signature;

    public ContextAwareBlockBody(StaticScope scope, Signature signature, int argumentType) {
        super(argumentType);

        this.scope = scope;
        this.signature = signature;
    }

    public ContextAwareBlockBody(StaticScope scope, Arity arity, int argumentType) {
        this(scope, Signature.from(arity), argumentType);
    }

    protected Frame pre(ThreadContext context, Binding binding) {
        return context.preYieldSpecificBlock(binding, scope);
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

    public Signature getSignature() {
        return signature;
    }

    public Arity arity() {
        return signature.arity();
    }
}
