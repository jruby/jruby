package org.jruby.ext.ripper;

import org.jruby.RubySymbol;
import org.jruby.lexer.yacc.LexContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Hold information during def_name, defn_head, and defs_head.
 */
public class Holder {
    public Object context;
    public RubySymbol name;
    public IRubyObject value;

    public Holder(Object context, RubySymbol name, IRubyObject value) {
        this.context = context;
        this.name = name;
        this.value = value;
    }
}
