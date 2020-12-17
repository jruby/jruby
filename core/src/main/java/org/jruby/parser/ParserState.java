package org.jruby.parser;

import org.jruby.lexer.yacc.RubyLexer;

public interface ParserState {
    public Object execute(ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count);
}
