package org.jruby.scanner;

public interface TokenTypes {
    public final static int EOF = 0;
    
    public final static int LINE_COMMENT = 10;
    public final static int MULTILINE_COMMENT = 11;
    
    // Operators
    public final static int OPERATOR = 0x1000;
    
    public final static int OP_ASSIGN  = OPERATOR | 0x00; // =
    public final static int OP_ASSOC   = OPERATOR | 0x01; // =>
    public final static int OP_CMP     = OPERATOR | 0x02; // <=>
    public final static int OP_EQ 	   = OPERATOR | 0x03; // ==
    public final static int OP_EQQ 	   = OPERATOR | 0x04; // ===
    public final static int OP_GEQ 	   = OPERATOR | 0x05; // >=
    public final static int OP_GREATER = OPERATOR | 0x06; // >
    public final static int OP_LEQ 	   = OPERATOR | 0x07; // <=
    public final static int OP_LESSER  = OPERATOR | 0x08; // <
    public final static int OP_LSHFT   = OPERATOR | 0x09; // <<
    public final static int OP_MATCH   = OPERATOR | 0x0a; // =~
    public final static int OP_MUL     = OPERATOR | 0x0b; // *
    public final static int OP_NEQ     = OPERATOR | 0x0c; // !=
    public final static int OP_NMATCH  = OPERATOR | 0x0d; // !~
    public final static int OP_NOT     = OPERATOR | 0x0e; // !
    public final static int OP_POW     = OPERATOR | 0x0f; // **
    public final static int OP_RSHFT   = OPERATOR | 0x10; // >>
    public final static int OP_STAR    = OPERATOR | 0x11; // *
}