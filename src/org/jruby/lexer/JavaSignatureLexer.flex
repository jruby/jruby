package org.jruby.lexer;

import org.jruby.parser.JavaSignatureParser;

%%
%public
%class JavaSignatureLexer
%standalone
%unicode
%line
%column
%{
  public Object value() {
    return yytext();
  }

  public static JavaSignatureLexer create(java.io.InputStream stream) {
    return new JavaSignatureLexer(stream);
  }
%}

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
WhiteSpace     = {LineTerminator} | [ \t\f]
Identifier     = [:jletter:] [:jletterdigit:]*

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

    "&"             { return JavaSignatureParser.AND;          }
    "."             { return JavaSignatureParser.DOT;          }
    ","             { return JavaSignatureParser.COMMA;        }
    "\u2026"        { return JavaSignatureParser.ELLIPSIS;     }
    "..."           { return JavaSignatureParser.ELLIPSIS;     }
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

    {Identifier}              { return JavaSignatureParser.IDENTIFIER;   }
    {WhiteSpace}              { }
}

.|\n  { throw new Error("Invalid character ("+yytext()+")"); }
