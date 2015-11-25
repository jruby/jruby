package org.jruby.runtime;

import org.jruby.parser.StaticScope;

/**
 * Common type for all block types which share similar values that ThreadContext uses.
 */
public abstract class ContextAwareBlockBody extends BlockBody {
    /** The static scope for the block body */
    protected StaticScope scope;

    public ContextAwareBlockBody(StaticScope scope, Signature signature) {
        super(signature);

        this.scope = scope;
    }

    @Deprecated
    public ContextAwareBlockBody(StaticScope scope, Arity arity, int argumentType) {
        this(scope, Signature.from(arity));
    }

    protected Frame pre(ThreadContext context, Block b) {
        return context.preYieldSpecificBlock(b.getBinding(), scope);
    }

    protected void post(ThreadContext context, Block b, Visibility vis, Frame lastFrame) {
        b.getBinding().getFrame().setVisibility(vis);
        context.postYield(b.getBinding(), lastFrame);
    }

    public StaticScope getStaticScope() {
        return scope;
    }

    public void setStaticScope(StaticScope newScope) {
        this.scope = newScope;
    }
}
