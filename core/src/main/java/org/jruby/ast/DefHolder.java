package org.jruby.ast;

import org.jruby.RubySymbol;
import org.jruby.lexer.yacc.LexContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

// Note: jay grammar does not support generics so we there is a special
// method getRipperSingleton() to work around this.
public class DefHolder {
    public final RubySymbol name;
    public final ByteList current_arg;
    public final LexContext ctxt;

    public int line;
    public Object singleton = null;
    public ByteList dotOrColon;

    public DefHolder(RubySymbol name, ByteList currentArg, LexContext ctxt) {
        this.name = name;
        this.current_arg = currentArg;
        this.ctxt = ctxt;
    }

    public IRubyObject getRipperSingleton() {
        return (IRubyObject) singleton;
    }

    // Node for parser and IRubyObject for ripper.
    public void setSingleton(Object singleton) {
        this.singleton = singleton;
    }

    public void setDotOrColon(ByteList dotOrColon) {
        this.dotOrColon = dotOrColon;
    }
}
