package org.jruby.parser;

public interface ParserState {
    Object execute(RubyParser support, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yyvalue);
}
