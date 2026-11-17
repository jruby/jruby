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

    protected Frame pre(ThreadContext context, Block block) {
        return context.preYieldSpecificBlock(block.getBinding(), scope);
    }

    protected void post(ThreadContext context, Block block, Visibility vis, Frame lastFrame) {
        block.getBinding().getFrame().setVisibility(vis);
        context.postYield(block.getBinding(), lastFrame);
    }

    public StaticScope getStaticScope() {
        return scope;
    }

    public void setStaticScope(StaticScope newScope) {
        this.scope = newScope;
    }
}
