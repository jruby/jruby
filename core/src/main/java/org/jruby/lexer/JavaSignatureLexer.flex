package org.jruby.lexer;

import org.jruby.parser.JavaSignatureParser;

%%
%public
%class JavaSignatureLexer
%standalone
%unicode
%line
%int
%column
%{
  boolean stringResult = false;
  boolean characterResult = false;
  StringBuilder stringBuf = new StringBuilder();

  public Object value() {
    if (stringResult) {
        stringResult = false;
        String value = stringBuf.toString();
        stringBuf.setLength(0);
        return value;
    } else if (characterResult) {
        characterResult = false;
        String value = stringBuf.toString();
        if (stringBuf.length() != 1) throw new Error("Character not on char ("+ value +")");
        stringBuf.setLength(0);
        return value;
    }
    return yytext();
  }

  public static JavaSignatureLexer create(java.io.InputStream stream) {
    return new JavaSignatureLexer(new java.io.InputStreamReader(stream));
  }
%}

LineTerminator = \r|\n|\r\n
WhiteSpace     = {LineTerminator} | [ \t\f]
Identifier     = [:jletter:] [:jletterdigit:]*
Digit          = [0-9]
HexDigit       = {Digit} | [a-f] | [A-F]
HexNumber      = "0x" {HexDigit} ("_" ? {HexDigit})*
NumComponent   = {Digit} ("_"? {Digit})*
Number         = "-"? {NumComponent} ("." {NumComponent})?

%state CHARACTER
%state STRING
%state NUMBER

%%

<YYINITIAL> {
    
    // primitive types
    "boolean"       { return JavaSignatureParser.BOOLEAN;      }
    "byte"          { return JavaSignatureParser.BYTE;         }
    "short"         { return JavaSignatureParser.SHORT;        }
    "int"           { return JavaSignatureParser.INT;          }
    "long"          { return JavaSignatureParser.LONG;         }
    "char"          { return JavaSignatureParser.CHAR;         }
    "float"         { return JavaSignatureParser.FLOAT;        }
    "double"        { return JavaSignatureParser.DOUBLE;       }
    "void"          { return JavaSignatureParser.VOID;         }

    // modifiers
    "public"        { return JavaSignatureParser.PUBLIC;       }
    "protected"     { return JavaSignatureParser.PROTECTED;    }
    "private"       { return JavaSignatureParser.PRIVATE;      }
    "static"        { return JavaSignatureParser.STATIC;       }
    "abstract"      { return JavaSignatureParser.ABSTRACT;     }
    "final"         { return JavaSignatureParser.FINAL;        }
    "native"        { return JavaSignatureParser.NATIVE;       }
    "synchronized"  { return JavaSignatureParser.SYNCHRONIZED; }
    "transient"     { return JavaSignatureParser.TRANSIENT;    }
    "volatile"      { return JavaSignatureParser.VOLATILE;     }
    "strictfp"      { return JavaSignatureParser.STRICTFP;     }

    "@"             { return JavaSignatureParser.AT;           }
    "&"             { return JavaSignatureParser.AND;          }
    "."             { return JavaSignatureParser.DOT;          }
    ","             { return JavaSignatureParser.COMMA;        }
    "\u2026"        { return JavaSignatureParser.ELLIPSIS;     }
    "..."           { return JavaSignatureParser.ELLIPSIS;     }
    "="             { return JavaSignatureParser.EQUAL;        }
    "{"             { return JavaSignatureParser.LCURLY;       }
    "}"             { return JavaSignatureParser.RCURLY;       }
    "("             { return JavaSignatureParser.LPAREN;       }
    ")"             { return JavaSignatureParser.RPAREN;       }
    "["             { return JavaSignatureParser.LBRACK;       }
    "]"             { return JavaSignatureParser.RBRACK;       }
    "?"             { return JavaSignatureParser.QUESTION;     }
    "<"             { return JavaSignatureParser.LT;           }
    ">"             { return JavaSignatureParser.GT;           }
    "throws"        { return JavaSignatureParser.THROWS;       }
    "extends"       { return JavaSignatureParser.EXTENDS;      }
    "super"         { return JavaSignatureParser.SUPER;        }
    ">>"            { return JavaSignatureParser.RSHIFT;       }
    ">>>"           { return JavaSignatureParser.URSHIFT;      }
    
    "false"         { return JavaSignatureParser.FALSE_LITERAL;}
    "true"          { return JavaSignatureParser.TRUE_LITERAL; }
    
    {HexNumber}     { return JavaSignatureParser.HEXNUM_LITERAL; }
    {Number}        { return JavaSignatureParser.NUM_LITERAL;  }
    
    {Identifier}    { return JavaSignatureParser.IDENTIFIER;   }
    \"              { yybegin(STRING); } 
    \'              { yybegin(CHARACTER); } 
    {WhiteSpace}    { }
}

<CHARACTER> {
    \' { characterResult = true;
         yybegin(YYINITIAL);
         return JavaSignatureParser.CHARACTER_LITERAL; 
       }
  [^\n\r\'\\]+      { stringBuf.append( yytext() ); }
  \\t               { stringBuf.append('\t'); }
  \\b               { stringBuf.append('\b'); }
  \\n               { stringBuf.append('\n'); }
  \\r               { stringBuf.append('\r'); }
  \\f               { stringBuf.append('\f'); }
  \\\'              { stringBuf.append('\''); }
  \\\"              { stringBuf.append('\"'); }
  \\                { stringBuf.append('\\'); }
}

<STRING> {
  \"                {
                     stringResult = true;
                     yybegin(YYINITIAL); 
                     return JavaSignatureParser.STRING_LITERAL;
  }
  [^\n\r\"\\]+      { stringBuf.append( yytext() ); }
  \\t               { stringBuf.append('\t'); }
  \\b               { stringBuf.append('\b'); }
  \\n               { stringBuf.append('\n'); }
  \\r               { stringBuf.append('\r'); }
  \\f               { stringBuf.append('\f'); }
  \\\'              { stringBuf.append('\''); }
  \\\"              { stringBuf.append('\"'); }
  \\                { stringBuf.append('\\'); }
}

// . doesn't match all characters, because "." excludes all Unicode newline chars - use "[^]" instead
[^]  { throw new Error("Invalid character ("+yytext()+")"); }
