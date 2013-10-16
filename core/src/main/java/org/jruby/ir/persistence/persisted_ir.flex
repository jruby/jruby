package org.jruby.ir.persistence;

import beaver.Symbol;
import beaver.Scanner;

import example.ExampleParser.Terminals;

/**
* Scanner for persisted IR
*/
%%

%class PersistedIRScanner
%extends Scanner

%function nextToken
%type Symbol
%yylexthrow Scanner.Exception
%eofval{
	return new Symbol(Terminals.EOF, "end-of-file");
%eofval}

%unicode

%line
%column

%{
  StringBuilder string = new StringBuilder();

  private Symbol token(short id)
  {
	return new Symbol(id, yyline + 1, yycolumn + 1, yylength(), yytext());
  }

  private Symbol token(short id, Object value)
  {
	return new Symbol(id, yyline + 1, yycolumn + 1, yylength(), value);
  }

%}

LineTerminator = \r|\n|\r\n
WhiteSpace = [ \t\f]

/* identifiers */
Identifier = [:jletter:][:jletterdigit:]*

/* Numbers */
FixnumLiteral = 0 | (-)?[1-9][0-9]*

FloatLiteral = (-)?({FLit1}|{FLit2}|{FLit3}) {Exponent}?

/* Float elements */
FLit1    = [0-9]+ \. [0-9]*
FLit2    = \. [0-9]+
FLit3    = [0-9]+
Exponent = [eE] [+-]? [0-9]+

/* String */
StringCharacter = [^\r\n\"\\]
SymbolCharacter = [^\r\n\'\\]

%state STRING, SYMBOL, REGEXP_OPTIONS, KCODE

%%

<YYINITIAL> {
  /* string literal */
  \"                             { yybegin(STRING); string.setLength(0); }
  
  /* symbol literal */
  \'                             { yybegin(SYMBOL); string.setLength(0); }
  
  /* whitespace */
  {WhiteSpace}                   { /* ignore */ }
  
  {Identifier}                   { return token(Terminals.IDENTIFIER); }
  
  {LineTerminator}               { return token(Terminals.EOLN); }

  /* instruction names */
  "alias"                        { return token(Terminals.ALIAS); }
  "block_given"                  { return token(Terminals.BLOCK_GIVEN); }
  
  "="                            { return token(Terminals.EQ); } 

  /* operand markers */
  "Array:"                       { return token(Terminals.ARRAY_MARKER); }
  ":bignum"                      { return token(Terminals.BIGNUM_MARKER); }
  "ArgsPush"                     { return token(Terminals.ARGS_PUSH_MARKER); }
  "ArgsCat"                      { return token(Terminals.ARGS_CAT_MARKER); }
  "COMPOUND_STRING"              { return token(Terminals.COMPOUND_STRING_MARKER); }
  "scope"                        { return token(Terminals.SCOPE_MARKER); }
  ":fixnum"                      { return token(Terminals.FIXNUM_MARKER); }
  ":float"                       { return token(Terminals.FLOAT_MARKER); }  
  ":Range"                       { return token(Terminals.RANGE_MARKER); }
  "RE:"                          { return token(Terminals.REGEXP_MARKER); }  
  "RegexpOptions"                { yybegin(REGEXP_OPTIONS); return token(Terminals.REGEXP_OPTIONS_MARKER); }
  "module"                       { return token(Terminals.MODULE_MARKER); }
  "SValue"                       { return token(Terminals.SVALUE_MARKER); }
  
  /* special cases */
  "-unknown-super-target-"       { return token(Terminals.UNKNOWN_SUPER_TARGET); } 
  "<Class:Object>"               { return token(Terminals.OBJECT_CLASS); }
  "%self"                        { return token(Terminals.SELF); }
  "StandardError"                { return token(Terminals.STANDARD_ERROR); }
  "%undefined"                   { return token(Terminals.UNDEFINED_VALUE); }
  "nil(unexecutable)"            { return token(Terminals.UNEXECUTABLE_NIL); }
  
  /* nil literal */
  "nil"                          { return token(Terminals.NIL); }
  
  /* boolean literals */
  "true"                         { return token(Terminals.TRUE); }
  "false"                        { return token(Terminals.FALSE); }
  
  /* range type markers */
  ".."                           { return token(Terminals.EXCLUSIVE); }
  "..."                          { return token(Terminals.INCLUSIVE); }
  
  /* separators */
  "|"                            { return token(Terminals.BAR); }
  "("                            { return token(Terminals.LPAREN); }
  ")"                            { return token(Terminals.RPAREN); }
  "{"                            { return token(Terminals.LBRACE); }
  "}"                            { return token(Terminals.RBRACE); }
  "["                            { return token(Terminals.LBRACK); }
  "]"                            { return token(Terminals.RBRACK); }
  "<"                            { return token(Terminals.LT); }
  ">"                            { return token(Terminals.GT); }
  ";"                            { return token(Terminals.SEMICOLON); }
  ","                            { return token(Terminals.COMMA); }
  "."                            { return token(Terminals.DOT); }
  
  /* special symbols */
  "*"                            { return token(Terminals.ASTERISK); }
  ":"                            { return token(Terminals.COLON); }
  "$"                            { return token(Terminals.DOLLAR); }
  "#"                            { return token(Terminals.HASH); }
  "%"                            { return token(Terminals.PERCENT);
}

<STRING> {
  \"                             { yybegin(YYINITIAL); return token(Terminals.STRING_LITERAL, string.toString()); }

  {StringCharacter}+             { string.append( yytext() ); }
}

<SYMBOL> {
  \'                             { yybegin(YYINITIAL); return token(Terminals.SYMBOL_LITERAL, string.toString()); }

  {SymbolCharacter}+             { string.append( yytext() ); }
}

<REGEXP_OPTIONS> {
  ","                            { return token(Terminals.COMMA); }
  "kcode:"                       { yybegin(KCODE); return token(Terminals.KCODE_MARKER); }
  "encodingNone"                 { return token(Terminals.ENC_NODE); }
  "extended"                     { return token(Terminals.EXPECTED); }
  "fixed"                        { return token(Terminals.FIXED); }
  "ignorecase"                   { return token(Terminals.IGNORECASE); }
  "java"                         { return token(Terminals.JAVA); }
  "kcodeDefault"                 { return token(Terminals.KCODE_DEFAULT); }
  "literal"                      { return token(Terminals.LITERAL); }
  "multiline"                    { return token(Terminals.MULTILINE); }
  "once"                         { return token(Terminals.ONCE); }
  ")"                            { yybegin(YYINITIAL); return token(Terminals.RPAREN); }
}

<KCODE> {
  "NIL"                          { yybegin(REGEXP_OPTIONS); return token(Terminals.KCODE_NIL); }
  "NONE"                         { yybegin(REGEXP_OPTIONS); return token(Terminals.KCODE_NONE); }
  "UTF8"                         { yybegin(REGEXP_OPTIONS); return token(Terminals.KCODE_UTF8); }
  "SJIS"                         { yybegin(REGEXP_OPTIONS); return token(Terminals.KCODE_SJIS); }
  "EUC"                          { yybegin(REGEXP_OPTIONS); return token(Terminals.KCODE_EUC); }
}

/* error fallback */
.|\n                             { throw new Scanner.Exception(yyline + 1, yycolumn + 1, "unrecognized character '" + yytext() + "'"); }
