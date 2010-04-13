// created by jay 1.0.2 (c) 2002-2004 ats@cs.rit.edu
// skeleton Java 1.0 (c) 2002 ats@cs.rit.edu

					// line 17 "src/org/jruby/parser/JavaSignatureParser.y"
 
package org.jruby.parser;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jruby.ast.java_signature.ArrayTypeNode;
import org.jruby.ast.java_signature.ConstructorSignatureNode;
import org.jruby.ast.java_signature.MethodSignatureNode;
import org.jruby.ast.java_signature.Modifier;
import org.jruby.ast.java_signature.ParameterNode;
import org.jruby.ast.java_signature.PrimitiveTypeNode;
import org.jruby.ast.java_signature.ReferenceTypeNode;
import org.jruby.ast.java_signature.SignatureNode;
import org.jruby.ast.java_signature.TypeNode;
import org.jruby.lexer.JavaSignatureLexer;

public class JavaSignatureParser {
    private static JavaSignatureParser parser = new JavaSignatureParser();

    public static SignatureNode parse(InputStream in) throws IOException, ParserSyntaxException {
        return (SignatureNode) parser.yyparse(JavaSignatureLexer.create(in));
    }
					// line 31 "-"
  // %token constants
  public static final int BOOLEAN = 257;
  public static final int BYTE = 258;
  public static final int SHORT = 259;
  public static final int INT = 260;
  public static final int LONG = 261;
  public static final int CHAR = 262;
  public static final int FLOAT = 263;
  public static final int DOUBLE = 264;
  public static final int VOID = 265;
  public static final int PUBLIC = 266;
  public static final int PROTECTED = 267;
  public static final int PRIVATE = 268;
  public static final int STATIC = 269;
  public static final int ABSTRACT = 270;
  public static final int FINAL = 271;
  public static final int NATIVE = 272;
  public static final int SYNCHRONIZED = 273;
  public static final int TRANSIENT = 274;
  public static final int VOLATILE = 275;
  public static final int STRICTFP = 276;
  public static final int IDENTIFIER = 277;
  public static final int AND = 278;
  public static final int DOT = 279;
  public static final int COMMA = 280;
  public static final int ELLIPSIS = 281;
  public static final int LPAREN = 282;
  public static final int RPAREN = 283;
  public static final int LBRACK = 284;
  public static final int RBRACK = 285;
  public static final int QUESTION = 286;
  public static final int LT = 287;
  public static final int GT = 288;
  public static final int THROWS = 289;
  public static final int EXTENDS = 290;
  public static final int SUPER = 291;
  public static final int RSHIFT = 292;
  public static final int URSHIFT = 293;
  public static final int yyErrorCode = 256;

  /** number of final state.
    */
  protected static final int yyFinal = 12;

  /** parser tables.
      Order is mandated by <i>jay</i>.
    */
  protected static final short[] yyLhs = {
//yyLhs 125
    -1,     0,     0,    14,    14,    13,    13,    13,    13,    13,
    13,    13,    13,    20,    20,    22,    15,    15,    16,    16,
    19,    18,    17,    17,    17,    17,    27,    27,    27,    33,
    33,    33,    34,    34,    34,    35,    35,    35,    36,    36,
    37,    37,    38,    29,    29,    39,    39,    40,    40,    41,
    41,    28,    28,    30,    30,    31,    31,    32,    32,     7,
     7,     8,     8,     9,    50,    50,    50,    50,    50,    50,
    50,    50,    50,    50,    50,    21,    21,    51,    51,    10,
    10,    11,    11,     1,     5,     5,     6,     6,    12,    12,
    12,    12,    12,    12,    12,    12,    23,    23,    44,    44,
    45,    45,    42,    43,    43,    24,    24,    46,    46,    47,
    49,    49,    48,    48,    26,    26,    25,    52,     4,     4,
     3,     2,     2,     2,     2,
    }, yyLen = {
//yyLen 125
     2,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     5,     1,     3,
     1,     1,     2,     2,     6,     4,     1,     3,     3,     2,
     3,     3,     2,     3,     3,     2,     3,     3,     2,     3,
     2,     3,     2,     1,     3,     1,     3,     1,     3,     1,
     3,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     2,     0,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     3,     2,     3,     2,
     0,     1,     3,     4,     1,     0,     1,     3,     2,     1,
     3,     2,     3,     2,     4,     3,     1,     3,     3,     1,
     1,     3,     2,     2,     2,     2,     3,     1,     1,     3,
     1,     1,     2,     1,     2,     2,     2,     0,     3,     5,
     4,     4,     6,     4,     6,
    }, yyDefRed = {
//yyDefRed 189
     0,    64,    65,    66,    67,    68,    69,    70,    71,    72,
    73,    74,     0,     1,     2,     0,     0,    60,    61,    10,
     5,     6,     7,     8,     9,    11,    12,     0,    75,     0,
     0,     0,     0,     0,    13,    14,     4,     0,    62,     0,
     0,    15,     0,    99,   100,     0,     0,     0,   118,     0,
     0,     0,     0,     0,     0,     0,     0,   123,   103,     0,
   104,   102,   107,   108,     0,     0,     0,     0,     0,     0,
    20,    81,     0,    77,     0,   121,     0,     0,     0,     0,
     0,    52,    43,     0,    45,    54,    53,     0,    76,     0,
     0,     0,    86,     0,     0,     0,   105,    98,   101,     0,
   119,     0,     0,     0,    78,    29,     0,     0,     0,    38,
     0,     0,     0,     0,   120,     0,    96,     0,     0,    83,
     0,     0,   106,   110,   109,   111,   124,   122,    82,     0,
     0,    30,     0,    31,     0,     0,     0,     0,    47,    56,
    55,    39,    44,    46,     0,     0,     0,    87,    92,     0,
     0,   116,     0,   115,   114,   112,     0,     0,     0,    32,
     0,    40,     0,     0,    94,    97,     0,     0,    33,     0,
    34,     0,     0,     0,    49,    58,    57,    41,    48,     0,
     0,    35,    42,     0,     0,    36,     0,    37,    50,
    }, yyDgoto = {
//yyDgoto 53
    12,    40,    13,    30,    14,    90,    91,    15,    16,    17,
    48,    68,    92,    77,    93,   135,    34,    35,   151,    71,
    36,    80,    42,   118,    60,   121,   122,    81,    82,    83,
    84,   138,   174,    85,   139,   175,    86,   140,   176,    87,
   141,   177,    43,    44,    45,    46,    61,    62,   123,   124,
    18,    50,    63,
    }, yySindex = {
//yySindex 189
   134,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  -104,   134,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  -268,     0,  -255,
  -240,  -218,  -268,  -253,     0,     0,     0,  -246,     0,  -202,
  -240,     0,  -145,     0,     0,  -185,   -12,  -169,     0,  -168,
  -160,  -240,   -55,  -154,    -1,  -160,    -1,     0,     0,    33,
     0,     0,     0,     0,  -255,  -268,  -240,  -268,  -150,  -116,
     0,     0,  -107,     0,  -109,     0,  -190,  -218,  -108,  -102,
  -239,     0,     0,   -99,     0,     0,     0,  -214,     0,    33,
   -96,   -70,     0,  -256,   -72,  -260,     0,     0,     0,  -240,
     0,  -240,  -169,   -55,     0,     0,    33,    33,   -43,     0,
   -55,  -169,  -160,  -213,     0,    -1,     0,   -65,   -58,     0,
    33,   -50,     0,     0,     0,     0,     0,     0,     0,   -46,
  -102,     0,  -102,     0,   -17,   -33,  -211,   -48,     0,     0,
     0,     0,     0,     0,  -239,   -11,   -58,     0,     0,   -41,
     0,     0,  -102,     0,     0,     0,  -169,    33,    33,     0,
   -22,     0,   -43,  -160,     0,     0,  -107,  -211,     0,  -211,
     0,  -142,   -63,   -13,     0,     0,     0,     0,     0,    33,
    33,     0,     0,   -22,   -63,     0,   -63,     0,     0,
    }, yyRindex = {
//yyRindex 189
   -95,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   -64,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   268,  -161,     0,  -152,     0,     0,     0,  -250,     0,     0,
   268,     0,    -8,     0,     0,     0,     0,     0,     0,     0,
    38,   268,     0,     0,   -14,    55,   -14,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   268,     0,   277,     3,
     0,     0,     1,     0,     0,     0,    -2,     0,  -186,     5,
  -103,     0,     0,     0,     0,     0,     0,    72,     0,     0,
     0,    -4,     0,  -225,     0,    -8,     0,     0,     0,   268,
     0,   268,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,    89,  -177,     0,     0,     0,  -171,  -143,     0,
     0,     6,     0,     0,     0,     0,     0,     0,     0,     4,
     7,     0,    20,     0,    -2,  -269,     5,     0,     0,     0,
     0,     0,     0,     0,    21,   -92,   -56,     0,     0,     0,
  -181,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   106,     0,     0,     2,     7,     0,    20,
     0,    -2,     5,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     7,     0,    20,     0,     0,
    }, yyGindex = {
//yyGindex 53
     0,   -19,     0,   234,     0,   247,     0,     0,     0,     0,
   -25,     0,   190,    29,    -7,    32,   -31,     0,     0,   204,
   -47,   -15,     0,   194,     0,     0,   191,     0,   -93,   -98,
   -90,  -148,   128,     0,     0,     0,   -53,  -128,  -137,   214,
     0,     0,   256,   258,     0,     0,     0,     0,   202,     0,
   308,   -30,   230,
    };
  protected static final short[] yyTable = {
//yyTable 411
    37,    16,    17,    18,    19,    79,    96,    55,    32,    39,
   137,    18,    95,    51,   178,    57,    70,   142,   120,    18,
   143,   116,    41,    18,    18,   117,    75,    16,   109,   168,
   170,    37,    72,    53,    52,   178,    54,    16,    49,    67,
    53,   100,   185,   187,    31,    49,    99,    33,   101,    47,
    55,   168,   170,   131,   133,    89,    79,   112,    89,   130,
   132,   136,   173,    79,   116,   111,    49,   153,   145,   142,
    49,    70,   143,   152,   126,    31,   127,   109,    33,    69,
    56,   161,   113,    31,    78,    31,    33,    72,    33,   150,
   142,    78,    18,   143,    18,    64,   144,    21,   105,    21,
   106,   107,    18,    91,   131,   133,    91,    13,    28,    93,
   167,   169,    93,   172,   163,   136,     3,    73,    31,     3,
     3,    33,     3,    88,    74,    18,   131,   133,    18,    18,
   102,    18,   184,   186,    69,    78,   172,    88,    78,    78,
    88,   166,    78,    58,    31,    59,   105,    33,   179,   180,
   159,   181,    78,    19,    20,    21,    22,    23,    24,    25,
    26,    27,    63,    63,    63,    63,    63,    63,    63,    63,
    63,   103,    53,    28,    16,    16,   104,    16,    16,   108,
    16,   110,    63,    29,    16,    16,   109,   114,    95,    16,
    16,    95,    63,    59,    59,    59,    59,    59,    59,    59,
    59,    59,    19,    20,    21,    22,    23,    24,    25,    26,
   115,   119,   148,    59,    19,    20,    21,    22,    23,    24,
    25,    26,    28,    59,    90,   109,   149,    90,   120,   161,
   182,    76,   162,   156,    28,    19,    20,    21,    22,    23,
    24,    25,    26,   134,   165,    19,    20,    21,    22,    23,
    24,    25,    26,    65,   160,    28,    19,    20,    21,    22,
    23,    24,    25,    26,   171,    28,   164,   183,    80,    85,
    89,   105,   117,   157,   158,   159,    28,    79,    26,    84,
    66,    16,    17,    18,    19,    51,   113,    27,    16,    17,
    19,    20,    21,    22,    23,    24,    25,    26,    17,    17,
    28,    17,    17,    94,    17,   147,   128,   146,    17,    17,
    28,   188,   154,    17,    17,    22,    22,   129,    22,    22,
    97,    22,    98,   155,    38,   125,    22,     0,     0,     0,
    22,    22,    23,    23,     0,    23,    23,     0,    23,     0,
     0,     0,     0,    23,     0,     0,     0,    23,    23,    19,
    19,     0,    19,    19,     0,    19,     0,     0,     0,     0,
    19,     0,     0,     0,    19,    19,    25,    25,     0,    25,
    25,     0,    25,     0,     0,     0,     0,    25,     0,     0,
     0,    25,    25,    24,    24,     0,    24,    24,     0,    24,
     0,     0,     0,     0,    24,     0,     0,     0,    24,    24,
     1,     2,     3,     4,     5,     6,     7,     8,     9,    10,
    11,
    };
  protected static final short[] yyCheck = {
//yyCheck 411
    15,     0,     0,     0,     0,    52,    59,    37,    15,   277,
   108,   280,    59,    32,   162,    40,    47,   110,   278,   288,
   110,   277,   277,   292,   293,   281,    51,   277,   288,   157,
   158,    46,    47,   279,   287,   183,   282,   287,   284,    46,
   279,    66,   179,   180,    15,   284,    65,    15,    67,   289,
    80,   179,   180,   106,   107,   280,   103,    87,   283,   106,
   107,   108,   160,   110,   277,   279,   284,   120,   281,   162,
   284,   102,   162,   120,    99,    46,   101,   288,    46,    47,
   282,   292,    89,    54,    52,    56,    54,   102,    56,   120,
   183,    59,   278,   183,   280,   280,   111,   278,   288,   280,
   290,   291,   288,   280,   157,   158,   283,   288,   277,   280,
   157,   158,   283,   160,   144,   162,   277,   285,    89,   280,
   281,    89,   283,   277,   284,   277,   179,   180,   280,   281,
   280,   283,   179,   180,   102,   103,   183,   280,   106,   107,
   283,   156,   110,   288,   115,   290,   288,   115,   290,   291,
   292,   293,   120,   257,   258,   259,   260,   261,   262,   263,
   264,   265,   257,   258,   259,   260,   261,   262,   263,   264,
   265,   287,   279,   277,   277,   278,   285,   280,   281,   287,
   283,   280,   277,   287,   287,   288,   288,   283,   280,   292,
   293,   283,   287,   257,   258,   259,   260,   261,   262,   263,
   264,   265,   257,   258,   259,   260,   261,   262,   263,   264,
   280,   283,   277,   277,   257,   258,   259,   260,   261,   262,
   263,   264,   277,   287,   280,   288,   284,   283,   278,   292,
   293,   286,   280,   279,   277,   257,   258,   259,   260,   261,
   262,   263,   264,   286,   285,   257,   258,   259,   260,   261,
   262,   263,   264,   265,   287,   277,   257,   258,   259,   260,
   261,   262,   263,   264,   286,   277,   277,   280,     0,   283,
   271,   288,   280,   290,   291,   292,   277,     0,   280,   283,
    46,   280,   280,   280,   280,   280,   280,   280,   287,   287,
   257,   258,   259,   260,   261,   262,   263,   264,   277,   278,
   280,   280,   281,    56,   283,   115,   102,   113,   287,   288,
   277,   183,   121,   292,   293,   277,   278,   103,   280,   281,
    64,   283,    64,   121,    16,    95,   288,    -1,    -1,    -1,
   292,   293,   277,   278,    -1,   280,   281,    -1,   283,    -1,
    -1,    -1,    -1,   288,    -1,    -1,    -1,   292,   293,   277,
   278,    -1,   280,   281,    -1,   283,    -1,    -1,    -1,    -1,
   288,    -1,    -1,    -1,   292,   293,   277,   278,    -1,   280,
   281,    -1,   283,    -1,    -1,    -1,    -1,   288,    -1,    -1,
    -1,   292,   293,   277,   278,    -1,   280,   281,    -1,   283,
    -1,    -1,    -1,    -1,   288,    -1,    -1,    -1,   292,   293,
   266,   267,   268,   269,   270,   271,   272,   273,   274,   275,
   276,
    };

  /** maps symbol value to printable name.
      @see #yyExpecting
    */
  protected static final String[] yyNames = {
    "end-of-file",null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,"BOOLEAN","BYTE","SHORT","INT",
    "LONG","CHAR","FLOAT","DOUBLE","VOID","PUBLIC","PROTECTED","PRIVATE",
    "STATIC","ABSTRACT","FINAL","NATIVE","SYNCHRONIZED","TRANSIENT",
    "VOLATILE","STRICTFP","IDENTIFIER","AND","DOT","COMMA","ELLIPSIS",
    "LPAREN","RPAREN","LBRACK","RBRACK","QUESTION","LT","GT","THROWS",
    "EXTENDS","SUPER","RSHIFT","URSHIFT",
    };


  /** simplified error message.
      @see #yyerror(java.lang.String, java.lang.String[])
    */
  public void yyerror (String message) throws ParserSyntaxException {
    throw new ParserSyntaxException(message);
  }

  /** (syntax) error message.
      Can be overwritten to control message format.
      @param message text to be displayed.
      @param expected list of acceptable tokens, if available.
    */
  public void yyerror (String message, String[] expected, String found) throws ParserSyntaxException {
    String text = message + ", unexpected " + found + "\n";
    throw new ParserSyntaxException(text);
  }

  /** computes list of expected tokens on error by tracing the tables.
      @param state for which to compute the list.
      @return list of token names.
    */
  protected String[] yyExpecting (int state) {
    int token, n, len = 0;
    boolean[] ok = new boolean[yyNames.length];

    if ((n = yySindex[state]) != 0)
      for (token = n < 0 ? -n : 0;
           token < yyNames.length && n+token < yyTable.length; ++ token)
        if (yyCheck[n+token] == token && !ok[token] && yyNames[token] != null) {
          ++ len;
          ok[token] = true;
        }
    if ((n = yyRindex[state]) != 0)
      for (token = n < 0 ? -n : 0;
           token < yyNames.length && n+token < yyTable.length; ++ token)
        if (yyCheck[n+token] == token && !ok[token] && yyNames[token] != null) {
          ++ len;
          ok[token] = true;
        }

    String result[] = new String[len];
    for (n = token = 0; n < len;  ++ token)
      if (ok[token]) result[n++] = yyNames[token];
    return result;
  }

  /** the generated parser, with debugging messages.
      Maintains a dynamic state and value stack.
      @param yyLex scanner.
      @param yydebug debug message writer implementing <tt>yyDebug</tt>, or <tt>null</tt>.
      @return result of the last reduction, if any.
      @throws ParserSyntaxException on irrecoverable parse error.
    */
  public Object yyparse (JavaSignatureLexer yyLex, Object ayydebug)
				throws java.io.IOException, ParserSyntaxException {
    return yyparse(yyLex);
  }

  /** initial size and increment of the state/value stack [default 256].
      This is not final so that it can be overwritten outside of invocations
      of {@link #yyparse}.
    */
  protected int yyMax;

  /** executed at the beginning of a reduce action.
      Used as <tt>$$ = yyDefault($1)</tt>, prior to the user-specified action, if any.
      Can be overwritten to provide deep copy, etc.
      @param first value for <tt>$1</tt>, or <tt>null</tt>.
      @return first.
    */
  protected Object yyDefault (Object first) {
    return first;
  }

  /** the generated parser.
      Maintains a dynamic state and value stack.
      @param yyLex scanner.
      @return result of the last reduction, if any.
      @throws ParserSyntaxException on irrecoverable parse error.
    */
  public Object yyparse (JavaSignatureLexer yyLex) throws java.io.IOException, ParserSyntaxException {
    if (yyMax <= 0) yyMax = 256;			// initial size
    int yyState = 0, yyStates[] = new int[yyMax];	// state stack
    Object yyVal = null, yyVals[] = new Object[yyMax];	// value stack
    int yyToken = -1;					// current input
    int yyErrorFlag = 0;				// #tokens to shift

    yyLoop: for (int yyTop = 0;; ++ yyTop) {
      if (yyTop >= yyStates.length) {			// dynamically increase
        int[] i = new int[yyStates.length+yyMax];
        System.arraycopy(yyStates, 0, i, 0, yyStates.length);
        yyStates = i;
        Object[] o = new Object[yyVals.length+yyMax];
        System.arraycopy(yyVals, 0, o, 0, yyVals.length);
        yyVals = o;
      }
      yyStates[yyTop] = yyState;
      yyVals[yyTop] = yyVal;

      yyDiscarded: for (;;) {	// discarding a token does not change stack
        int yyN;
        if ((yyN = yyDefRed[yyState]) == 0) {	// else [default] reduce (yyN)
          if (yyToken < 0) {
            int a1 = yyLex.yylex();
            yyToken = a1 == -1 ? 0 : a1;
          }
          if ((yyN = yySindex[yyState]) != 0 && (yyN += yyToken) >= 0
              && yyN < yyTable.length && yyCheck[yyN] == yyToken) {
            yyState = yyTable[yyN];		// shift to yyN
            yyVal = yyLex.value();
            yyToken = -1;
            if (yyErrorFlag > 0) -- yyErrorFlag;
            continue yyLoop;
          }
          if ((yyN = yyRindex[yyState]) != 0 && (yyN += yyToken) >= 0
              && yyN < yyTable.length && yyCheck[yyN] == yyToken)
            yyN = yyTable[yyN];			// reduce (yyN)
          else
            switch (yyErrorFlag) {
  
            case 0:
              yyerror("syntax error", yyExpecting(yyState), yyNames[yyToken]);
  
            case 1: case 2:
              yyErrorFlag = 3;
              do {
                if ((yyN = yySindex[yyStates[yyTop]]) != 0
                    && (yyN += yyErrorCode) >= 0 && yyN < yyTable.length
                    && yyCheck[yyN] == yyErrorCode) {
                  yyState = yyTable[yyN];
                  yyVal = yyLex.value();
                  continue yyLoop;
                }
              } while (-- yyTop >= 0);
              throw new ParserSyntaxException("irrecoverable syntax error");
  
            case 3:
              if (yyToken == 0) {
                throw new ParserSyntaxException("irrecoverable syntax error at end-of-file");
              }
              yyToken = -1;
              continue yyDiscarded;		// leave stack alone
            }
        }
        int yyV = yyTop + 1-yyLen[yyN];
        yyVal = yyDefault(yyV > yyTop ? null : yyVals[yyV]);
        switch (yyN) {
// ACTIONS_BEGIN
case 1:
					// line 112 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((MethodSignatureNode)yyVals[0+yyTop]);
 }
  break;
case 2:
					// line 114 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ConstructorSignatureNode)yyVals[0+yyTop]);
 }
  break;
case 4:
					// line 118 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((TypeNode)yyVals[0+yyTop]); }
  break;
case 5:
					// line 121 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.BYTE;
 }
  break;
case 6:
					// line 124 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.SHORT;
 }
  break;
case 7:
					// line 127 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.INT;
 }
  break;
case 8:
					// line 130 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.LONG;
 }
  break;
case 9:
					// line 133 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.CHAR;
 }
  break;
case 10:
					// line 136 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.BOOLEAN;
 }
  break;
case 11:
					// line 139 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.FLOAT;
 }
  break;
case 12:
					// line 142 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.DOUBLE;
 }
  break;
case 13:
					// line 147 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[0+yyTop]);
 }
  break;
case 14:
					// line 150 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[0+yyTop]);
 }
  break;
case 15:
					// line 155 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((String)yyVals[0+yyTop]); 
 }
  break;
case 16:
					// line 160 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = new ReferenceTypeNode(((String)yyVals[0+yyTop]));
 }
  break;
case 17:
					// line 163 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     String genericTyping = "<" + ((String)yyVals[-2+yyTop]) + "." + ((String)yyVals[0+yyTop]);
     yyVal = ((ReferenceTypeNode)yyVals[-4+yyTop]);
     ((ReferenceTypeNode)yyVals[-4+yyTop]).setGenericsTyping(genericTyping);
 }
  break;
case 19:
					// line 171 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     String genericTyping = "<" + ((String)yyVals[0+yyTop]);
     yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]);
     ((ReferenceTypeNode)yyVals[-2+yyTop]).setGenericsTyping(genericTyping);
 }
  break;
case 22:
					// line 184 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(((TypeNode)yyVals[-1+yyTop]));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 23:
					// line 188 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(new ReferenceTypeNode(((String)yyVals[-1+yyTop])));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 24:
					// line 192 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ReferenceTypeNode)yyVals[-5+yyTop]).setGenericsTyping("<" + ((String)yyVals[-3+yyTop]) + "." + ((String)yyVals[-1+yyTop]));
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(((ReferenceTypeNode)yyVals[-5+yyTop]));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 25:
					// line 197 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ReferenceTypeNode)yyVals[-3+yyTop]).setGenericsTyping("<" + ((String)yyVals[-1+yyTop]));
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(((ReferenceTypeNode)yyVals[-3+yyTop]));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 26:
					// line 204 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "?";
 }
  break;
case 27:
					// line 206 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? extends " + ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
 }
  break;
case 28:
					// line 208 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? super " + ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
 }
  break;
case 29:
					// line 213 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "?>"; 
 }
  break;
case 30:
					// line 215 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? extends " + ((String)yyVals[0+yyTop]);
 }
  break;
case 31:
					// line 217 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? super " + ((String)yyVals[0+yyTop]);
 }
  break;
case 32:
					// line 222 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "?>>"; 
 }
  break;
case 33:
					// line 224 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? extends " + ((String)yyVals[0+yyTop]);
 }
  break;
case 34:
					// line 226 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? super " + ((String)yyVals[0+yyTop]);
 }
  break;
case 35:
					// line 231 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "?>>";
 }
  break;
case 36:
					// line 233 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? extends " + ((String)yyVals[0+yyTop]);
 }
  break;
case 37:
					// line 235 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? super " + ((String)yyVals[0+yyTop]);
 }
  break;
case 38:
					// line 240 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ">";
 }
  break;
case 39:
					// line 242 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]).getFullyTypedName() + "<" + ((String)yyVals[0+yyTop]);
 }
  break;
case 40:
					// line 247 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ">>";
 }
  break;
case 41:
					// line 249 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]).getFullyTypedName() + "<" + ((String)yyVals[0+yyTop]);
 }
  break;
case 42:
					// line 254 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ">>>";
 }
  break;
case 43:
					// line 259 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[0+yyTop]);
 }
  break;
case 44:
					// line 262 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 46:
					// line 268 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 48:
					// line 274 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 50:
					// line 280 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 51:
					// line 285 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
 }
  break;
case 61:
					// line 303 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    yyVal = new ArrayList<Modifier>();
    ((List)yyVal).add(((Modifier)yyVals[0+yyTop]));
 }
  break;
case 62:
					// line 307 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    ((List)yyVals[-1+yyTop]).add(((Modifier)yyVals[0+yyTop]));
 }
  break;
case 63:
					// line 312 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<Modifier>(); }
  break;
case 64:
					// line 315 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PUBLIC; }
  break;
case 65:
					// line 316 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PROTECTED; }
  break;
case 66:
					// line 317 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PRIVATE; }
  break;
case 67:
					// line 318 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.STATIC; }
  break;
case 68:
					// line 319 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.ABSTRACT; }
  break;
case 69:
					// line 320 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.FINAL; }
  break;
case 70:
					// line 321 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.NATIVE; }
  break;
case 71:
					// line 322 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.SYNCHRONIZED; }
  break;
case 72:
					// line 323 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.TRANSIENT; }
  break;
case 73:
					// line 324 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.VOLATILE; }
  break;
case 74:
					// line 325 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.STRICTFP; }
  break;
case 75:
					// line 328 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[0+yyTop]); }
  break;
case 76:
					// line 329 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-2+yyTop]) + "." + ((String)yyVals[0+yyTop]); }
  break;
case 77:
					// line 332 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = new ArrayTypeNode(); 
 }
  break;
case 78:
					// line 334 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = new ArrayTypeNode(((ArrayTypeNode)yyVals[-2+yyTop]));
 }
  break;
case 79:
					// line 339 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((List)yyVals[0+yyTop]); }
  break;
case 80:
					// line 340 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<TypeNode>(); }
  break;
case 81:
					// line 343 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    yyVal = new ArrayList<TypeNode>();
    ((List)yyVal).add(((ReferenceTypeNode)yyVals[0+yyTop]));
 }
  break;
case 82:
					// line 347 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    ((List)yyVals[-2+yyTop]).add(((ReferenceTypeNode)yyVals[0+yyTop]));
 }
  break;
case 83:
					// line 352 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                      yyVal = new MethodSignatureNode(((String)yyVals[-3+yyTop]), ((List)yyVals[-1+yyTop]));
                  }
  break;
case 85:
					// line 358 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<ParameterNode>(); }
  break;
case 86:
					// line 361 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                          List<ParameterNode> list = new ArrayList<ParameterNode>();
                          list.add(((ParameterNode)yyVals[0+yyTop]));
                          yyVal = list;
                      }
  break;
case 87:
					// line 366 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                          ((List)yyVals[-2+yyTop]).add(((ParameterNode)yyVals[0+yyTop]));
                      }
  break;
case 88:
					// line 371 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]));
                 }
  break;
case 89:
					// line 374 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[0+yyTop]), null);
                 }
  break;
case 90:
					// line 377 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]), true);
                 }
  break;
case 91:
					// line 380 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[0+yyTop]), null, true);
                 }
  break;
case 92:
					// line 383 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]), false, true);
                 }
  break;
case 93:
					// line 386 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), null, false, true);
                 }
  break;
case 94:
					// line 389 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]), true, true);
                 }
  break;
case 95:
					// line 392 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), null, true, true);
                 }
  break;
case 96:
					// line 397 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[0+yyTop]);
 }
  break;
case 97:
					// line 399 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     /* We know this is always preceeded by 'type' production.*/
     yyVals[-3+yyTop] = new ArrayTypeNode(((TypeNode)yyVals[-3+yyTop])); 
     yyVal = ((String)yyVals[-2+yyTop]);
 }
  break;
case 98:
					// line 406 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 101:
					// line 412 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 102:
					// line 417 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 103:
					// line 422 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((String)yyVals[-1+yyTop]) + ">"; 
 }
  break;
case 104:
					// line 425 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 105:
					// line 430 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " extends " + ((String)yyVals[-1+yyTop]);
 }
  break;
case 106:
					// line 433 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " extends " + ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ((String)yyVals[0+yyTop]);
 }
  break;
case 108:
					// line 439 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "";
 }
  break;
case 109:
					// line 444 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "extends " + ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ((String)yyVals[0+yyTop]);
 }
  break;
case 111:
					// line 450 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "";
 }
  break;
case 112:
					// line 455 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 114:
					// line 460 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 115:
					// line 463 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " & " + ((String)yyVals[-1+yyTop]);
 }
  break;
case 116:
					// line 468 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " & " + ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
}
  break;
case 117:
					// line 472 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = null; }
  break;
case 118:
					// line 474 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ConstructorSignatureNode)yyVals[-1+yyTop]);
     ((ConstructorSignatureNode)yyVal).setModifiers(((List)yyVals[-2+yyTop]));
     ((ConstructorSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
 }
  break;
case 119:
					// line 478 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ConstructorSignatureNode)yyVals[-1+yyTop]);
     ((ConstructorSignatureNode)yyVal).setModifiers(((List)yyVals[-4+yyTop]));
     ((ConstructorSignatureNode)yyVal).setExtraTypeInfo("<" + ((String)yyVals[-2+yyTop]));
     ((ConstructorSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
 }
  break;
case 120:
					// line 485 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = new ConstructorSignatureNode(((String)yyVals[-3+yyTop]), ((List)yyVals[-1+yyTop]));
 }
  break;
case 121:
					// line 489 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(((TypeNode)yyVals[-2+yyTop]));
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 122:
					// line 495 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-5+yyTop]));
                  ((MethodSignatureNode)yyVal).setExtraTypeInfo("<" + ((String)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(((TypeNode)yyVals[-2+yyTop]));
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 123:
					// line 502 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(PrimitiveTypeNode.VOID);
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 124:
					// line 508 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-5+yyTop]));
                  ((MethodSignatureNode)yyVal).setExtraTypeInfo("<" + ((String)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(PrimitiveTypeNode.VOID);
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
					// line 1232 "-"
// ACTIONS_END
        }
        yyTop -= yyLen[yyN];
        yyState = yyStates[yyTop];
        int yyM = yyLhs[yyN];
        if (yyState == 0 && yyM == 0) {
          yyState = yyFinal;
          if (yyToken < 0) {
            int a1 = yyLex.yylex();
            yyToken = a1 == -1 ? 0 : a1;
          }
          if (yyToken == 0) {
            return yyVal;
          }
          continue yyLoop;
        }
        if ((yyN = yyGindex[yyM]) != 0 && (yyN += yyState) >= 0
            && yyN < yyTable.length && yyCheck[yyN] == yyState)
          yyState = yyTable[yyN];
        else
          yyState = yyDgoto[yyM];
        continue yyLoop;
      }
    }
  }

// ACTION_BODIES
					// line 517 "src/org/jruby/parser/JavaSignatureParser.y"

}
					// line 1268 "-"
