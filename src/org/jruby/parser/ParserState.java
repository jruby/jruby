package org.jruby.parser;

import org.jruby.lexer.yacc.RubyYaccLexer;

public interface ParserState {
    public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop);
}
