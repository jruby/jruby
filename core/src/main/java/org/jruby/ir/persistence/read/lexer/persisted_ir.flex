package org.jruby.ir.persistence.read.lexer;

import org.jruby.ir.persistence.read.parser.PersistedIRParser;
import org.jruby.parser.ParserSyntaxException;

/**
* Scanner for persisted IR
*/
%%

%public
%class PersistedIRScanner
%standalone
%unicode
%line
%column
%yylexthrow ParserSyntaxException
%eofval{
return PersistedIRParser.EOF;
%eofval}

%{

        boolean stringResult = false;
        StringBuilder string = new StringBuilder();

        public Object value() {
            if (stringResult) {
                stringResult = false;
                String value = string.toString();
                string.setLength(0);
                return value;
            }

            return yytext();
        }

        private void appendToString() {
            string.append(yytext());
        }

        public static PersistedIRScanner create(java.io.InputStream stream) {
            return new PersistedIRScanner(stream);
        }

%}

LineTerminator = \r|\n|\r\n
WhiteSpace = [ \t\f]
Identifier = [:jletter:][:jletterdigit:]*
BooleanLiteral = (true|false)
FixnumLiteral = 0 | [+-]?[1-9][0-9]*
FloatLiteral = [+-]?{FLit} {Exponent}?

/* Float elements */
FLit = [0-9]+ \. [0-9]+
Exponent = E[+-]?[0-9]+

/* String */
StringCharacter = [^\"\\]

%state STRING

%%

<YYINITIAL> {
    \"                 { stringResult = true; yybegin(STRING); }

    {WhiteSpace}       { /* ignore */ }
    {LineTerminator}   { return PersistedIRParser.EOLN; }
    {FixnumLiteral}    { return PersistedIRParser.FIXNUM; }
    {FloatLiteral}     { return PersistedIRParser.FLOAT; }
    "="                { return PersistedIRParser.EQ; }
    {BooleanLiteral}   { return PersistedIRParser.BOOLEAN; }
    "null"             { return PersistedIRParser.NULL; }

    /* Markers that are common for all instructions */
    "[DEAD]"           { return PersistedIRParser.DEAD_INSTR_MARKER; }
    "[DEAD-RESULT]"    { return PersistedIRParser.DEAD_RESULT_INSTR_MARKER; }

    {Identifier}       { return PersistedIRParser.ID; }

    /* separators */
    "["                { return PersistedIRParser.LBRACK; }
    "]"                { return PersistedIRParser.RBRACK; }
    "("                { return PersistedIRParser.LPAREN; }
    ")"                { return PersistedIRParser.RPAREN; }
    "{"                { return PersistedIRParser.LBRACE; }
    "}"                { return PersistedIRParser.RBRACE; }
    "<"                { return PersistedIRParser.LT; }
    ">"                { return PersistedIRParser.GT; }
    ","                { return PersistedIRParser.COMMA; }
}

<STRING> {
    \"                 { yybegin(YYINITIAL); return PersistedIRParser.STRING; }

    \\\"               { string.append('\"'); }
    \\                 { string.append('\\'); }

    {StringCharacter}+ { appendToString(); }
}

/* error fallback */
.|\n                   { throw new ParserSyntaxException("" + (yyline + 1) + ", " + (yycolumn + 1) + " unrecognized character '" + yytext() + "'"); }
