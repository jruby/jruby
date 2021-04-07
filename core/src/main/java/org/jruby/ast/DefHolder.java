package org.jruby.ast;

import org.jruby.RubySymbol;
import org.jruby.lexer.yacc.LexContext;
import org.jruby.util.ByteList;

public class DefHolder {
    public final RubySymbol name;
    public final ByteList current_arg;
    public final LexContext ctxt;

    public int line;
    public Node singleton = null;

    public DefHolder(RubySymbol name, ByteList currentArg, LexContext ctxt) {
        this.name = name;
        this.current_arg = currentArg;
        this.ctxt = ctxt;
    }

    public void setSingleton(Node singleton) {
        this.singleton = singleton;
    }
}
