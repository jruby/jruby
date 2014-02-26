package org.jruby.parser;

import org.jruby.lexer.yacc.RubyLexer;

public interface ParserState {
    public Object execute(ParserSupport support, RubyLexer lexer, Object yyVal, Object[] yyVals, int yyTop);
}
