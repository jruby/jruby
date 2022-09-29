package org.jruby.ext.ripper;

import org.jruby.lexer.yacc.LexContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Hold information during def_name, defn_head, and defs_head.
 */
public class NameHolder {
    public LexContext context;
    public IRubyObject name;
    public IRubyObject value;

    public NameHolder(LexContext context, IRubyObject name, IRubyObject value) {
        this.context = context;
        this.name = name;
        this.value = value;
    }
}
