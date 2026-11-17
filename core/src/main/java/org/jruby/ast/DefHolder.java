package org.jruby.ast;

import org.jruby.RubySymbol;
import org.jruby.lexer.yacc.LexContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

// Note: jay grammar does not support generics so we there is a special
// method getRipperSingleton() to work around this.
public class DefHolder {
    public final RubySymbol name;
    public final LexContext ctxt;
    public int maxNumParam; // the enclosing scope's, restored once the method is defined
    public IRubyObject value;

    public int line;
    public int column = -1;
    public Object singleton = null;
    public ByteList dotOrColon;

    public DefHolder(RubySymbol name, IRubyObject value, LexContext ctxt) {
        this.name = name;
        this.value = value;
        this.ctxt = ctxt;
    }

    public DefHolder(RubySymbol name, LexContext ctxt) {
        this.name = name;
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
