package org.jruby.scanner;

public interface TokenTypes {
    public final static int TOKEN_EOF = 0;
    public final static int TOKEN_WHITESPACE = 1;
    
    public final static int TOKEN_LINE_COMMENT = 10;
    public final static int TOKEN_MULTI_LINE_COMMENT = 11;
    
    public final static int TOKEN_EQQ = 100;        // ===
    public final static int TOKEN_EQ = 101;         // ==
    public final static int TOKEN_MATCH = 102;      // =~
    public final static int TOKEN_ASSOC = 103;      // =>
    public final static int TOKEN_ASSIGN = 104;     // =
    public final static int TOKEN_NEQ = 105;        // !=
    public final static int TOKEN_NMATCH = 106;     // !~
    public final static int TOKEN_NOT = 107;        // !
}