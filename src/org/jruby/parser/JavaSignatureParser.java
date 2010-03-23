// created by jay 1.0.2 (c) 2002-2004 ats@cs.rit.edu
// skeleton Java 1.0 (c) 2002 ats@cs.rit.edu

					// line 17 "src/org/jruby/parser/JavaSignatureParser.y"
 
package org.jruby.parser;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jruby.ast.java_signature.ArrayTypeNode;
import org.jruby.ast.java_signature.MethodSignatureNode;
import org.jruby.ast.java_signature.Modifier;
import org.jruby.ast.java_signature.ParameterNode;
import org.jruby.ast.java_signature.PrimitiveTypeNode;
import org.jruby.ast.java_signature.ReferenceTypeNode;
import org.jruby.ast.java_signature.TypeNode;
import org.jruby.lexer.JavaSignatureLexer;

public class JavaSignatureParser {
    private static JavaSignatureParser parser = new JavaSignatureParser();

    public static MethodSignatureNode parse(InputStream in) throws IOException, ParserSyntaxException {
        return (MethodSignatureNode) parser.yyparse(JavaSignatureLexer.create(in));
    }
					// line 29 "-"
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
//yyLhs 121
    -1,     0,    12,    12,    11,    11,    11,    11,    11,    11,
    11,    11,    18,    18,    20,    13,    13,    14,    14,    17,
    16,    15,    15,    15,    15,    25,    25,    25,    31,    31,
    31,    32,    32,    32,    33,    33,    33,    34,    34,    35,
    35,    36,    27,    27,    37,    37,    38,    38,    39,    39,
    26,    26,    28,    28,    29,    29,    30,    30,     5,     5,
     6,     6,     7,    48,    48,    48,    48,    48,    48,    48,
    48,    48,    48,    48,    19,    19,    49,    49,     8,     8,
     9,     9,     1,     3,     3,     4,     4,    10,    10,    10,
    10,    10,    10,    10,    10,    21,    21,    42,    42,    43,
    43,    40,    41,    41,    22,    22,    44,    44,    45,    47,
    47,    46,    46,    24,    24,    23,    50,     2,     2,     2,
     2,
    }, yyLen = {
//yyLen 121
     2,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     5,     1,     3,     1,
     1,     2,     2,     6,     4,     1,     3,     3,     2,     3,
     3,     2,     3,     3,     2,     3,     3,     2,     3,     2,
     3,     2,     1,     3,     1,     3,     1,     3,     1,     3,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     2,     0,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     3,     2,     3,     2,     0,
     1,     3,     4,     1,     0,     1,     3,     2,     1,     3,
     2,     3,     2,     4,     3,     1,     3,     3,     1,     1,
     3,     2,     2,     2,     2,     3,     1,     1,     3,     1,
     1,     2,     1,     2,     2,     2,     0,     4,     6,     4,
     6,
    }, yyDefRed = {
//yyDefRed 180
     0,    63,    64,    65,    66,    67,    68,    69,    70,    71,
    72,    73,     0,     1,     0,     0,    59,    60,     9,     4,
     5,     6,     7,     8,    10,    11,     0,    74,     0,     0,
     0,     0,    12,    13,     3,     0,    61,     0,     0,    14,
     0,    98,    99,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   119,   102,     0,   103,   101,   106,   107,
     0,     0,     0,    76,     0,   117,     0,     0,     0,     0,
    51,    42,     0,    44,    53,    52,     0,    75,     0,     0,
     0,    85,     0,     0,     0,    19,    80,     0,     0,   104,
    97,   100,     0,     0,    77,    28,     0,     0,     0,    37,
     0,     0,     0,     0,    82,     0,    95,     0,     0,     0,
     0,     0,     0,   105,   109,   108,   110,   120,   118,     0,
    29,     0,    30,     0,     0,     0,     0,    46,    55,    54,
    38,    43,    45,     0,     0,     0,    86,    91,     0,    81,
     0,     0,   115,     0,   114,   113,   111,     0,     0,    31,
     0,    39,     0,     0,    93,    96,     0,     0,    32,     0,
    33,     0,     0,     0,    48,    57,    56,    40,    47,     0,
     0,     0,    34,    41,     0,     0,    35,     0,    36,    49,
    }, yyDgoto = {
//yyDgoto 51
    12,    38,    13,    79,    80,    14,    15,    16,    53,    83,
    81,    67,    82,   124,    32,    33,   142,    86,    34,    35,
    40,   108,    56,   112,   113,    70,    71,    72,    73,   127,
   164,    74,   128,   165,    75,   129,   166,    76,   130,   167,
    41,    42,    43,    44,    57,    58,   114,   115,    17,    46,
    59,
    }, yySindex = {
//yySindex 180
   109,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,  -117,   109,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,  -256,     0,  -200,  -181,
  -256,  -202,     0,     0,     0,  -241,     0,  -166,  -172,     0,
  -251,     0,     0,  -146,   -26,  -150,  -148,  -172,  -168,  -119,
  -148,   -16,  -111,     0,     0,     8,     0,     0,     0,     0,
  -200,  -256,  -256,     0,  -112,     0,  -232,  -181,  -113,  -120,
     0,     0,   -82,     0,     0,     0,  -210,     0,     8,   -84,
   -69,     0,  -176,   -65,   -90,     0,     0,   -67,  -253,     0,
     0,     0,  -172,  -172,     0,     0,     8,     8,   -68,     0,
  -168,  -111,  -148,  -162,     0,   -16,     0,   -57,   -59,  -111,
  -168,     8,   -14,     0,     0,     0,     0,     0,     0,  -120,
     0,  -120,     0,   -34,   -47,  -129,    -7,     0,     0,     0,
     0,     0,     0,  -241,    -3,   -59,     0,     0,   -10,     0,
    -2,     0,     0,  -120,     0,     0,     0,     8,     8,     0,
   -56,     0,   -68,  -148,     0,     0,  -111,  -129,     0,  -129,
     0,  -161,   -66,    -4,     0,     0,     0,     0,     0,   -67,
     8,     8,     0,     0,   -56,   -66,     0,   -66,     0,     0,
    }, yyRindex = {
//yyRindex 180
  -108,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   -77,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  -199,
     0,  -170,     0,     0,     0,  -116,     0,     0,   278,     0,
    -1,     0,     0,     0,     0,     0,    13,   278,     0,     0,
    30,    12,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     6,     0,  -258,     7,
     0,     0,     0,     0,     0,     0,    47,     0,     0,     0,
    14,     0,  -212,   280,     3,     0,     0,     1,    -1,     0,
     0,     0,   278,   278,     0,     0,     0,     0,     0,     0,
     0,     0,    64,  -105,     0,     0,     0,   -31,   -30,     0,
     0,     0,    18,     0,     0,     0,     0,     0,     0,    19,
     0,    20,     0,     6,  -261,     7,     0,     0,     0,     0,
     0,     0,     0,   -64,   -21,   -20,     0,     0,     0,     0,
     4,  -254,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,    81,     0,     0,     0,    19,     0,    20,
     0,     6,     7,     0,     0,     0,     0,     0,     0,     2,
     0,     0,     0,     0,     0,    19,     0,    20,     0,     0,
    }, yyGindex = {
//yyGindex 51
     0,   -17,     0,     0,     0,     0,     0,     0,   -38,     0,
   187,     9,    -8,    28,   -36,     0,     0,   193,   -48,   -44,
     0,   200,     0,     0,   192,     0,   -88,   -83,   -86,  -141,
   135,     0,     0,     0,   -50,  -130,  -142,   202,     0,     0,
   254,   255,     0,     0,     0,     0,   204,     0,   302,   -25,
   231,
    };
  protected static final short[] yyTable = {
//yyTable 386
    69,    15,    16,    17,    18,    89,    30,    88,    87,    65,
    50,   168,   131,    47,   132,   126,    85,   158,   160,    17,
    17,    37,    17,    29,    20,   111,    20,    17,   176,   178,
    17,    17,    17,   168,    12,    99,    62,    54,    49,    55,
   158,   160,    31,    45,    92,    93,   120,   122,   119,   121,
   125,   102,    69,    29,   117,   118,    95,   133,    96,    97,
    29,   144,    69,   143,   131,    87,   132,   163,    88,   101,
   103,    88,    31,    85,    45,   141,    68,    39,     2,    31,
    84,     2,     2,    68,     2,    48,   131,    29,   132,    18,
    19,    20,    21,    22,    23,    24,    25,   120,   122,   157,
   159,   106,   162,    45,   125,   107,    31,    17,   153,    27,
    17,    17,   169,    17,    29,   106,    51,    52,    66,   134,
   120,   122,   175,   177,    68,    68,   162,    95,    68,   170,
   171,   149,   172,    31,    60,    63,    64,    84,    68,    68,
    18,    19,    20,    21,    22,    23,    24,    25,    26,    62,
    62,    62,    62,    62,    62,    62,    62,    62,    77,    99,
    27,    15,    15,   151,    15,    15,    27,    15,    99,    62,
    28,    15,    15,    94,    98,    90,    15,    15,    90,    62,
    58,    58,    58,    58,    58,    58,    58,    58,    58,    18,
    19,    20,    21,    22,    23,    24,    25,   110,   100,   104,
    58,    18,    19,    20,    21,    22,    23,    24,    25,    27,
    58,   105,    49,    16,    16,   109,    16,    16,   123,    16,
   137,    27,    99,    16,    16,   138,   151,   173,    16,    16,
   161,    18,    19,    20,    21,    22,    23,    24,    25,    61,
   150,    18,    19,    20,    21,    22,    23,    24,    25,    92,
    87,    27,    92,    87,    95,    78,   147,   148,   149,    94,
    89,    27,    94,    89,   111,    18,    19,    20,    21,    22,
    23,    24,    25,   152,   154,   155,   174,   156,    79,   116,
    78,    15,    16,    17,    18,    27,    25,    50,    15,    16,
    21,    21,   136,    21,    21,    84,    21,    83,   112,    26,
    27,    21,   139,   135,   145,    21,    21,    22,    22,   179,
    22,    22,   140,    22,    90,    91,   146,    36,    22,   116,
     0,     0,    22,    22,    18,    18,     0,    18,    18,     0,
    18,     0,     0,     0,     0,    18,     0,     0,     0,    18,
    18,    24,    24,     0,    24,    24,     0,    24,     0,     0,
     0,     0,    24,     0,     0,     0,    24,    24,    23,    23,
     0,    23,    23,     0,    23,     0,     0,     0,     0,    23,
     0,     0,     0,    23,    23,     1,     2,     3,     4,     5,
     6,     7,     8,     9,    10,    11,
    };
  protected static final short[] yyCheck = {
//yyCheck 386
    48,     0,     0,     0,     0,    55,    14,    55,    52,    47,
    35,   152,   100,    30,   100,    98,    52,   147,   148,   280,
   278,   277,   280,    14,   278,   278,   280,   288,   170,   171,
   288,   292,   293,   174,   288,   288,    44,   288,   279,   290,
   170,   171,    14,   284,    61,    62,    96,    97,    96,    97,
    98,    76,   100,    44,    92,    93,   288,   101,   290,   291,
    51,   111,   110,   111,   152,   109,   152,   150,   280,   279,
    78,   283,    44,   109,   284,   111,    48,   277,   277,    51,
    52,   280,   281,    55,   283,   287,   174,    78,   174,   257,
   258,   259,   260,   261,   262,   263,   264,   147,   148,   147,
   148,   277,   150,   284,   152,   281,    78,   277,   133,   277,
   280,   281,   156,   283,   105,   277,   282,   289,   286,   281,
   170,   171,   170,   171,    96,    97,   174,   288,   100,   290,
   291,   292,   293,   105,   280,   285,   284,   109,   110,   111,
   257,   258,   259,   260,   261,   262,   263,   264,   265,   257,
   258,   259,   260,   261,   262,   263,   264,   265,   277,   288,
   277,   277,   278,   292,   280,   281,   277,   283,   288,   277,
   287,   287,   288,   285,   287,   280,   292,   293,   283,   287,
   257,   258,   259,   260,   261,   262,   263,   264,   265,   257,
   258,   259,   260,   261,   262,   263,   264,   287,   280,   283,
   277,   257,   258,   259,   260,   261,   262,   263,   264,   277,
   287,   280,   279,   277,   278,   280,   280,   281,   286,   283,
   277,   277,   288,   287,   288,   284,   292,   293,   292,   293,
   286,   257,   258,   259,   260,   261,   262,   263,   264,   265,
   287,   257,   258,   259,   260,   261,   262,   263,   264,   280,
   280,   277,   283,   283,   288,   271,   290,   291,   292,   280,
   280,   277,   283,   283,   278,   257,   258,   259,   260,   261,
   262,   263,   264,   280,   277,   285,   280,   279,     0,   280,
     0,   280,   280,   280,   280,   277,   280,   280,   287,   287,
   277,   278,   105,   280,   281,   283,   283,   283,   280,   280,
   280,   288,   109,   103,   112,   292,   293,   277,   278,   174,
   280,   281,   110,   283,    60,    60,   112,    15,   288,    88,
    -1,    -1,   292,   293,   277,   278,    -1,   280,   281,    -1,
   283,    -1,    -1,    -1,    -1,   288,    -1,    -1,    -1,   292,
   293,   277,   278,    -1,   280,   281,    -1,   283,    -1,    -1,
    -1,    -1,   288,    -1,    -1,    -1,   292,   293,   277,   278,
    -1,   280,   281,    -1,   283,    -1,    -1,    -1,    -1,   288,
    -1,    -1,    -1,   292,   293,   266,   267,   268,   269,   270,
   271,   272,   273,   274,   275,   276,
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
case 3:
					// line 110 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((TypeNode)yyVals[0+yyTop]); }
  break;
case 4:
					// line 113 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.BOOLEAN;
 }
  break;
case 5:
					// line 116 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.SHORT;
 }
  break;
case 6:
					// line 119 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.INT;
 }
  break;
case 7:
					// line 122 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.LONG;
 }
  break;
case 8:
					// line 125 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.CHAR;
 }
  break;
case 9:
					// line 128 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.BOOLEAN;
 }
  break;
case 10:
					// line 131 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.FLOAT;
 }
  break;
case 11:
					// line 134 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.DOUBLE;
 }
  break;
case 12:
					// line 139 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[0+yyTop]);
 }
  break;
case 13:
					// line 142 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[0+yyTop]);
 }
  break;
case 14:
					// line 147 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((String)yyVals[0+yyTop]); 
 }
  break;
case 15:
					// line 152 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = new ReferenceTypeNode(((String)yyVals[0+yyTop]));
 }
  break;
case 16:
					// line 155 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     String genericTyping = "<" + ((String)yyVals[-2+yyTop]) + "." + ((String)yyVals[0+yyTop]);
     yyVal = ((ReferenceTypeNode)yyVals[-4+yyTop]);
     ((ReferenceTypeNode)yyVals[-4+yyTop]).setGenericsTyping(genericTyping);
 }
  break;
case 18:
					// line 163 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     String genericTyping = "<" + ((String)yyVals[0+yyTop]);
     yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]);
     ((ReferenceTypeNode)yyVals[-2+yyTop]).setGenericsTyping(genericTyping);
 }
  break;
case 21:
					// line 176 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(((TypeNode)yyVals[-1+yyTop]));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 22:
					// line 180 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(new ReferenceTypeNode(((String)yyVals[-1+yyTop])));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 23:
					// line 184 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ReferenceTypeNode)yyVals[-5+yyTop]).setGenericsTyping("<" + ((String)yyVals[-3+yyTop]) + "." + ((String)yyVals[-1+yyTop]));
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(((ReferenceTypeNode)yyVals[-5+yyTop]));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 24:
					// line 189 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ReferenceTypeNode)yyVals[-3+yyTop]).setGenericsTyping("<" + ((String)yyVals[-1+yyTop]));
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(((ReferenceTypeNode)yyVals[-3+yyTop]));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 25:
					// line 196 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "?";
 }
  break;
case 26:
					// line 198 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? extends " + ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
 }
  break;
case 27:
					// line 200 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? super " + ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
 }
  break;
case 28:
					// line 205 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "?>"; 
 }
  break;
case 29:
					// line 207 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? extends " + ((String)yyVals[0+yyTop]);
 }
  break;
case 30:
					// line 209 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? super " + ((String)yyVals[0+yyTop]);
 }
  break;
case 31:
					// line 214 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "?>>"; 
 }
  break;
case 32:
					// line 216 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? extends " + ((String)yyVals[0+yyTop]);
 }
  break;
case 33:
					// line 218 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? super " + ((String)yyVals[0+yyTop]);
 }
  break;
case 34:
					// line 223 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "?>>";
 }
  break;
case 35:
					// line 225 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? extends " + ((String)yyVals[0+yyTop]);
 }
  break;
case 36:
					// line 227 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? super " + ((String)yyVals[0+yyTop]);
 }
  break;
case 37:
					// line 232 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ">";
 }
  break;
case 38:
					// line 234 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]).getFullyTypedName() + "<" + ((String)yyVals[0+yyTop]);
 }
  break;
case 39:
					// line 239 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ">>";
 }
  break;
case 40:
					// line 241 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]).getFullyTypedName() + "<" + ((String)yyVals[0+yyTop]);
 }
  break;
case 41:
					// line 246 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ">>>";
 }
  break;
case 42:
					// line 251 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[0+yyTop]);
 }
  break;
case 43:
					// line 254 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 45:
					// line 260 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 47:
					// line 266 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 49:
					// line 272 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 50:
					// line 277 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
 }
  break;
case 60:
					// line 295 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    yyVal = new ArrayList<Modifier>();
    ((List)yyVal).add(((Modifier)yyVals[0+yyTop]));
 }
  break;
case 61:
					// line 299 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    ((List)yyVals[-1+yyTop]).add(((Modifier)yyVals[0+yyTop]));
 }
  break;
case 62:
					// line 304 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<Modifier>(); }
  break;
case 63:
					// line 307 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PUBLIC; }
  break;
case 64:
					// line 308 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PROTECTED; }
  break;
case 65:
					// line 309 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PRIVATE; }
  break;
case 66:
					// line 310 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.STATIC; }
  break;
case 67:
					// line 311 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.ABSTRACT; }
  break;
case 68:
					// line 312 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.FINAL; }
  break;
case 69:
					// line 313 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.NATIVE; }
  break;
case 70:
					// line 314 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.SYNCHRONIZED; }
  break;
case 71:
					// line 315 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.TRANSIENT; }
  break;
case 72:
					// line 316 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.VOLATILE; }
  break;
case 73:
					// line 317 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.STRICTFP; }
  break;
case 74:
					// line 320 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[0+yyTop]); }
  break;
case 75:
					// line 321 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-2+yyTop]) + "." + ((String)yyVals[0+yyTop]); }
  break;
case 76:
					// line 324 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = new ArrayTypeNode(); 
 }
  break;
case 77:
					// line 326 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = new ArrayTypeNode(((ArrayTypeNode)yyVals[-2+yyTop]));
 }
  break;
case 78:
					// line 331 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((List)yyVals[0+yyTop]); }
  break;
case 79:
					// line 332 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<TypeNode>(); }
  break;
case 80:
					// line 335 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    yyVal = new ArrayList<TypeNode>();
    ((List)yyVal).add(((ReferenceTypeNode)yyVals[0+yyTop]));
 }
  break;
case 81:
					// line 339 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    ((List)yyVals[-2+yyTop]).add(((ReferenceTypeNode)yyVals[0+yyTop]));
 }
  break;
case 82:
					// line 344 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                      yyVal = new MethodSignatureNode(((String)yyVals[-3+yyTop]), ((List)yyVals[-1+yyTop]));
                  }
  break;
case 84:
					// line 350 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<ParameterNode>(); }
  break;
case 85:
					// line 353 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                          List<ParameterNode> list = new ArrayList<ParameterNode>();
                          list.add(((ParameterNode)yyVals[0+yyTop]));
                          yyVal = list;
                      }
  break;
case 86:
					// line 358 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                          ((List)yyVals[-2+yyTop]).add(((ParameterNode)yyVals[0+yyTop]));
                      }
  break;
case 87:
					// line 363 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]));
                 }
  break;
case 88:
					// line 366 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[0+yyTop]), null);
                 }
  break;
case 89:
					// line 369 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]), true);
                 }
  break;
case 90:
					// line 372 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[0+yyTop]), null, true);
                 }
  break;
case 91:
					// line 375 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]), false, true);
                 }
  break;
case 92:
					// line 378 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), null, false, true);
                 }
  break;
case 93:
					// line 381 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]), true, true);
                 }
  break;
case 94:
					// line 384 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), null, true, true);
                 }
  break;
case 95:
					// line 389 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[0+yyTop]);
 }
  break;
case 96:
					// line 391 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     /* We know this is always preceeded by 'type' production.*/
     yyVals[-3+yyTop] = new ArrayTypeNode(((TypeNode)yyVals[-3+yyTop])); 
     yyVal = ((String)yyVals[-2+yyTop]);
 }
  break;
case 97:
					// line 398 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 100:
					// line 404 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 101:
					// line 409 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 102:
					// line 414 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((String)yyVals[-1+yyTop]) + ">"; 
 }
  break;
case 103:
					// line 417 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 104:
					// line 422 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " extends " + ((String)yyVals[-1+yyTop]);
 }
  break;
case 105:
					// line 425 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " extends " + ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ((String)yyVals[0+yyTop]);
 }
  break;
case 107:
					// line 431 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "";
 }
  break;
case 108:
					// line 436 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "extends " + ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ((String)yyVals[0+yyTop]);
 }
  break;
case 110:
					// line 442 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "";
 }
  break;
case 111:
					// line 447 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 113:
					// line 452 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 114:
					// line 455 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " & " + ((String)yyVals[-1+yyTop]);
 }
  break;
case 115:
					// line 460 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " & " + ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
}
  break;
case 116:
					// line 464 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = null; }
  break;
case 117:
					// line 466 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(((TypeNode)yyVals[-2+yyTop]));
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 118:
					// line 472 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-5+yyTop]));
                  ((MethodSignatureNode)yyVal).setExtraTypeInfo("<" + ((String)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(((TypeNode)yyVals[-2+yyTop]));
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 119:
					// line 479 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(PrimitiveTypeNode.VOID);
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 120:
					// line 485 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-5+yyTop]));
                  ((MethodSignatureNode)yyVal).setExtraTypeInfo("<" + ((String)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(PrimitiveTypeNode.VOID);
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
					// line 1182 "-"
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
					// line 494 "src/org/jruby/parser/JavaSignatureParser.y"

}
					// line 1218 "-"
