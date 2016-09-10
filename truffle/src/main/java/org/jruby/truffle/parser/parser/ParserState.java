package org.jruby.truffle.parser.parser;

import org.jruby.truffle.parser.lexer.yacc.RubyLexer;

public interface ParserState {
    public Object execute(ParserSupport support, RubyLexer lexer, Object yyVal, Object[] yyVals, int yyTop);
}
