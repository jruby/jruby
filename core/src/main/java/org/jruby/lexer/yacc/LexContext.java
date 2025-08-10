package org.jruby.lexer.yacc;

import org.jruby.ast.DefHolder;

public class LexContext {
    public enum InRescue {
        NONE,
        BEFORE_RESCUE,
        AFTER_RESCUE,
        AFTER_ELSE,
        AFTER_ENSURE
    }
    // Is the parser currently within a class body.
    public boolean in_class;

    // Is the parser currently within a method definition
    public boolean in_def;

    public boolean cant_return;

    public boolean in_defined;

    public boolean in_kwarg;

    public ShareableConstantValue shareable_constant_value;
    public boolean in_argdef;

    public InRescue in_rescue = InRescue.NONE;

    public void reset() {
        in_def = false;
    }

    public Object clone() {
        LexContext context = new LexContext();
        context.in_class = in_class;
        context.in_def = in_def;
        context.in_kwarg = in_kwarg;
        context.in_defined = in_defined;
        context.in_rescue = in_rescue;
        context.in_argdef = in_argdef;
        context.shareable_constant_value = shareable_constant_value;

        return context;
    }

    public void restore(DefHolder holder) {
        this.in_def = holder.ctxt.in_def;
        this.shareable_constant_value = holder.ctxt.shareable_constant_value;
        this.in_rescue = holder.ctxt.in_rescue;
    }
}
