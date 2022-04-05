package org.jruby.parser;

public interface ParserState<T> {
    Object execute(T parser, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yyvalue);
}
