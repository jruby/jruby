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
    -1,     0,    11,    11,    10,    10,    10,    10,    10,    10,
    10,    10,    12,    12,    19,    14,    14,    15,    15,    17,
    16,    13,    13,    13,    13,    23,    23,    23,    24,    24,
    24,    25,    25,    25,    26,    26,    26,    27,    27,    28,
    28,    29,    30,    30,    31,    31,    32,    32,    33,    33,
    34,    34,    35,    35,    36,    36,    37,    37,     4,     4,
     5,     5,     6,    21,    21,    21,    21,    21,    21,    21,
    21,    21,    21,    21,    18,    18,    22,    22,     7,     7,
     8,     8,     1,     2,     2,     3,     3,     9,     9,     9,
     9,     9,     9,     9,     9,    20,    20,    44,    44,    38,
    38,    45,    39,    39,    40,    40,    46,    46,    47,    50,
    50,    43,    43,    41,    41,    42,    48,    49,    49,    49,
    49,
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
    72,    73,     0,     0,     0,    59,    60,     1,     9,     4,
     5,     6,     7,     8,    10,    11,     0,    74,     0,     0,
     0,     3,    13,     0,    12,     0,    61,     0,     0,    14,
     0,     0,    99,     0,    98,     0,     0,     0,     0,     0,
     0,     0,     0,   119,   102,     0,   103,   101,   106,   107,
     0,     0,     0,    76,     0,   117,     0,     0,     0,     0,
    51,    53,    52,     0,     0,    42,    44,    75,     0,     0,
     0,    85,     0,     0,     0,    19,    80,     0,     0,   104,
     0,     0,   100,    97,    77,    28,     0,     0,    37,     0,
     0,     0,     0,     0,    82,     0,    95,     0,     0,     0,
     0,     0,   105,     0,   109,   110,   108,   120,   118,     0,
    29,     0,    30,     0,     0,     0,    55,    54,     0,    38,
    46,    43,    45,     0,     0,     0,    86,    91,     0,    81,
     0,     0,     0,   115,   114,   113,   111,     0,     0,    31,
    39,     0,     0,     0,    93,    96,     0,     0,    32,     0,
    33,     0,     0,    57,    56,     0,    40,    48,    47,     0,
     0,     0,    34,    41,     0,     0,    35,     0,    36,    49,
    }, yyDgoto = {
//yyDgoto 51
    12,    38,    79,    80,    13,    14,    15,    53,    83,    81,
    67,    82,    31,    32,   125,    34,   143,    86,    35,    40,
   108,    16,    46,    70,    71,   126,   163,    72,   127,   164,
    73,    74,   129,   166,    75,    76,   130,   167,    41,    42,
    56,   112,   113,   114,    43,    44,    57,    58,    59,    17,
   116,
    }, yySindex = {
//yySindex 180
   117,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,  -117,   117,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,  -255,     0,  -241,  -240,
  -255,     0,     0,  -213,     0,  -246,     0,  -207,  -181,     0,
  -203,   -18,     0,  -194,     0,  -183,  -167,  -181,   -68,  -148,
  -167,     8,  -141,     0,     0,    33,     0,     0,     0,     0,
  -255,  -255,  -241,     0,  -166,     0,   -33,  -240,  -153,  -129,
     0,     0,     0,  -121,  -226,     0,     0,     0,    33,  -120,
  -114,     0,  -204,  -112,  -113,     0,     0,  -106,  -260,     0,
  -181,  -181,     0,     0,     0,     0,    33,    33,     0,   -56,
   -68,  -141,  -167,  -176,     0,     8,     0,   -80,   -73,  -141,
   -68,    33,     0,   -79,     0,     0,     0,     0,     0,  -153,
     0,  -153,     0,   -38,  -158,   -89,     0,     0,   -65,     0,
     0,     0,     0,  -246,   -57,   -73,     0,     0,   -59,     0,
   -67,  -153,     0,     0,     0,     0,     0,    33,    33,     0,
     0,   -26,   -56,  -167,     0,     0,  -141,  -158,     0,  -158,
     0,  -177,  -161,     0,     0,   -53,     0,     0,     0,  -106,
    33,    33,     0,     0,   -26,  -161,     0,  -161,     0,     0,
    }, yyRindex = {
//yyRindex 180
  -108,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   -77,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  -199,
     0,     0,     0,  -187,     0,  -116,     0,     0,   248,     0,
   -31,     0,     0,     0,     0,     0,    21,   248,     0,     0,
    38,   -27,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   -17,     0,    -6,  -257,
     0,     0,     0,     0,    55,     0,     0,     0,     0,     0,
   -10,     0,  -256,   262,     3,     0,     0,     1,   -31,     0,
   248,   248,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,    72,  -188,     0,     0,     0,  -173,  -105,     0,
     0,     0,     0,    -5,     0,     0,     0,     0,     0,    -4,
     0,    -3,     0,   -17,    -6,  -263,     0,     0,     0,     0,
     0,     0,     0,   -64,   -58,   -19,     0,     0,     0,     0,
     4,     0,  -243,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,    89,     0,     0,     0,    -4,     0,    -3,
     0,   -17,    -6,     0,     0,     0,     0,     0,     0,     2,
     0,     0,     0,     0,     0,    -4,     0,    -3,     0,     0,
    }, yyGindex = {
//yyGindex 51
     0,   -21,     0,     0,     0,     0,     0,   -35,     0,   173,
    13,    -7,   -48,     0,    28,   -39,     0,   171,   -44,     0,
   183,   273,   -24,     0,     0,     0,     0,   -50,  -128,  -111,
   -83,   190,     0,     0,   -86,   -85,  -142,   129,     0,   243,
     0,   193,     0,   194,     0,   246,     0,     0,   223,     0,
     0,
    };
  protected static final short[] yyTable = {
//yyTable 394
    68,    15,    16,    17,    18,    89,    30,    88,    87,    47,
   168,    50,    65,    85,   131,   132,   128,    17,   111,   158,
   160,    17,    37,    17,    88,    17,    29,    88,    98,    17,
    17,    17,   168,    49,    61,    20,    39,    20,    45,    90,
    91,    33,   158,   160,    45,    12,   120,   122,   119,   121,
   102,   124,    68,   101,    29,   117,   118,   133,    45,   176,
   178,   144,    68,   141,    29,    87,   131,   132,   165,    33,
    85,   103,   142,   106,    48,    51,    69,   107,     2,    33,
    84,     2,     2,    69,     2,    54,    62,    55,   131,   132,
    17,    29,    90,    17,    17,    90,    17,   120,   122,   157,
   159,   106,    63,   162,   124,   134,    33,    92,    52,   153,
    92,    95,   169,   170,   171,   149,   172,    64,    29,    94,
   120,   122,   175,   177,    69,    69,   162,    98,    69,    77,
    98,   150,   173,    33,   150,    98,    27,    84,    69,    69,
    18,    19,    20,    21,    22,    23,    24,    25,    26,    62,
    62,    62,    62,    62,    62,    62,    62,    62,    99,   100,
    27,    15,    15,   104,    15,    15,   105,    15,   109,    62,
    28,    15,    15,    49,   110,    87,    15,    15,    87,    62,
    58,    58,    58,    58,    58,    58,    58,    58,    58,    18,
    19,    20,    21,    22,    23,    24,    25,   137,   151,   111,
    58,    18,    19,    20,    21,    22,    23,    24,    25,    27,
    58,   138,   156,    16,    16,   152,    16,    16,    66,    16,
   154,    27,    94,    16,    16,    94,   155,   174,    16,    16,
   123,    18,    19,    20,    21,    22,    23,    24,    25,    18,
    19,    20,    21,    22,    23,    24,    25,    60,    79,   116,
    95,    27,   147,   148,   149,    95,    84,    96,    97,    27,
   161,    89,    78,    25,    89,    18,    19,    20,    21,    22,
    23,    24,    25,    83,    50,   112,    26,    27,   136,    78,
   139,    15,    16,    17,    18,    27,   135,    36,    15,    16,
    18,    19,    20,    21,    22,    23,    24,    25,    21,    21,
   140,    21,    21,   179,    21,    92,   145,   146,    93,    21,
    27,   115,     0,    21,    21,    22,    22,     0,    22,    22,
     0,    22,     0,     0,     0,     0,    22,     0,     0,     0,
    22,    22,    18,    18,     0,    18,    18,     0,    18,     0,
     0,     0,     0,    18,     0,     0,     0,    18,    18,    24,
    24,     0,    24,    24,     0,    24,     0,     0,     0,     0,
    24,     0,     0,     0,    24,    24,    23,    23,     0,    23,
    23,     0,    23,     0,     0,     0,     0,    23,     0,     0,
     0,    23,    23,     1,     2,     3,     4,     5,     6,     7,
     8,     9,    10,    11,
    };
  protected static final short[] yyCheck = {
//yyCheck 394
    48,     0,     0,     0,     0,    55,    13,    55,    52,    30,
   152,    35,    47,    52,   100,   100,    99,   280,   278,   147,
   148,   278,   277,   280,   280,   288,    13,   283,   288,   292,
   293,   288,   174,   279,    41,   278,   277,   280,   284,    60,
    61,    13,   170,   171,   284,   288,    96,    97,    96,    97,
    74,    99,   100,   279,    41,    90,    91,   101,   284,   170,
   171,   111,   110,   111,    51,   109,   152,   152,   151,    41,
   109,    78,   111,   277,   287,   282,    48,   281,   277,    51,
    52,   280,   281,    55,   283,   288,   280,   290,   174,   174,
   277,    78,   280,   280,   281,   283,   283,   147,   148,   147,
   148,   277,   285,   151,   152,   281,    78,   280,   289,   133,
   283,   288,   156,   290,   291,   292,   293,   284,   105,   285,
   170,   171,   170,   171,    96,    97,   174,   288,   100,   277,
   288,   292,   293,   105,   292,   288,   277,   109,   110,   111,
   257,   258,   259,   260,   261,   262,   263,   264,   265,   257,
   258,   259,   260,   261,   262,   263,   264,   265,   287,   280,
   277,   277,   278,   283,   280,   281,   280,   283,   280,   277,
   287,   287,   288,   279,   287,   280,   292,   293,   283,   287,
   257,   258,   259,   260,   261,   262,   263,   264,   265,   257,
   258,   259,   260,   261,   262,   263,   264,   277,   287,   278,
   277,   257,   258,   259,   260,   261,   262,   263,   264,   277,
   287,   284,   279,   277,   278,   280,   280,   281,   286,   283,
   277,   277,   280,   287,   288,   283,   285,   280,   292,   293,
   286,   257,   258,   259,   260,   261,   262,   263,   264,   257,
   258,   259,   260,   261,   262,   263,   264,   265,     0,   280,
   288,   277,   290,   291,   292,   288,   283,   290,   291,   277,
   286,   280,     0,   280,   283,   257,   258,   259,   260,   261,
   262,   263,   264,   283,   280,   280,   280,   280,   105,   271,
   109,   280,   280,   280,   280,   277,   103,    14,   287,   287,
   257,   258,   259,   260,   261,   262,   263,   264,   277,   278,
   110,   280,   281,   174,   283,    62,   113,   113,    62,   288,
   277,    88,    -1,   292,   293,   277,   278,    -1,   280,   281,
    -1,   283,    -1,    -1,    -1,    -1,   288,    -1,    -1,    -1,
   292,   293,   277,   278,    -1,   280,   281,    -1,   283,    -1,
    -1,    -1,    -1,   288,    -1,    -1,    -1,   292,   293,   277,
   278,    -1,   280,   281,    -1,   283,    -1,    -1,    -1,    -1,
   288,    -1,    -1,    -1,   292,   293,   277,   278,    -1,   280,
   281,    -1,   283,    -1,    -1,    -1,    -1,   288,    -1,    -1,
    -1,   292,   293,   266,   267,   268,   269,   270,   271,   272,
   273,   274,   275,   276,
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
case 4:
					// line 111 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.BOOLEAN;
 }
  break;
case 5:
					// line 114 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.SHORT;
 }
  break;
case 6:
					// line 117 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.INT;
 }
  break;
case 7:
					// line 120 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.LONG;
 }
  break;
case 8:
					// line 123 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.CHAR;
 }
  break;
case 9:
					// line 126 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.BOOLEAN;
 }
  break;
case 10:
					// line 129 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.FLOAT;
 }
  break;
case 11:
					// line 132 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.DOUBLE;
 }
  break;
case 12:
					// line 137 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[0+yyTop]);
 }
  break;
case 14:
					// line 143 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((String)yyVals[0+yyTop]); 
 }
  break;
case 15:
					// line 148 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = new ReferenceTypeNode(((String)yyVals[0+yyTop]));
 }
  break;
case 16:
					// line 151 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[-4+yyTop]); /* FIXME: Add generics to ref type*/
 }
  break;
case 18:
					// line 157 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]); /* FIXME: Add generics to ref type*/
 }
  break;
case 21:
					// line 168 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = new ArrayTypeNode(((TypeNode)yyVals[-1+yyTop]));
 }
  break;
case 22:
					// line 171 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = new ArrayTypeNode(new ReferenceTypeNode(((String)yyVals[-1+yyTop])));
 }
  break;
case 23:
					// line 174 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = new ArrayTypeNode(((ReferenceTypeNode)yyVals[-5+yyTop])); /* FIXME: Add generics to ref type*/
 }
  break;
case 24:
					// line 177 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = new ArrayTypeNode(((ReferenceTypeNode)yyVals[-3+yyTop])); /* FIXME: Add generics to ref type*/
 }
  break;
case 25:
					// line 181 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[0+yyTop]); }
  break;
case 26:
					// line 182 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-2+yyTop]); }
  break;
case 27:
					// line 183 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-2+yyTop]); }
  break;
case 28:
					// line 185 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-1+yyTop]); }
  break;
case 29:
					// line 186 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-2+yyTop]); }
  break;
case 30:
					// line 187 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-2+yyTop]); }
  break;
case 31:
					// line 189 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-1+yyTop]); }
  break;
case 32:
					// line 190 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-2+yyTop]); }
  break;
case 33:
					// line 191 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-2+yyTop]); }
  break;
case 34:
					// line 193 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-1+yyTop]); }
  break;
case 35:
					// line 194 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-2+yyTop]); }
  break;
case 36:
					// line 195 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-2+yyTop]); }
  break;
case 37:
					// line 197 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((TypeNode)yyVals[-1+yyTop]); }
  break;
case 38:
					// line 198 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]); }
  break;
case 39:
					// line 200 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((TypeNode)yyVals[-1+yyTop]); }
  break;
case 40:
					// line 201 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]); }
  break;
case 41:
					// line 203 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((TypeNode)yyVals[-1+yyTop]); }
  break;
case 60:
					// line 229 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    yyVal = new ArrayList<Modifier>();
    ((List)yyVal).add(((Modifier)yyVals[0+yyTop]));
 }
  break;
case 61:
					// line 233 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    ((List)yyVals[-1+yyTop]).add(((Modifier)yyVals[0+yyTop]));
 }
  break;
case 62:
					// line 238 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<Modifier>(); }
  break;
case 63:
					// line 241 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PUBLIC; }
  break;
case 64:
					// line 242 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PROTECTED; }
  break;
case 65:
					// line 243 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PRIVATE; }
  break;
case 66:
					// line 244 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.STATIC; }
  break;
case 67:
					// line 245 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.ABSTRACT; }
  break;
case 68:
					// line 246 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.FINAL; }
  break;
case 69:
					// line 247 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.NATIVE; }
  break;
case 70:
					// line 248 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.SYNCHRONIZED; }
  break;
case 71:
					// line 249 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.TRANSIENT; }
  break;
case 72:
					// line 250 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.VOLATILE; }
  break;
case 73:
					// line 251 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.STRICTFP; }
  break;
case 74:
					// line 254 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[0+yyTop]); }
  break;
case 75:
					// line 255 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-2+yyTop]) + "." + ((String)yyVals[0+yyTop]); }
  break;
case 76:
					// line 258 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = null; }
  break;
case 77:
					// line 258 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = null; }
  break;
case 78:
					// line 261 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((List)yyVals[0+yyTop]); }
  break;
case 79:
					// line 262 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<TypeNode>(); }
  break;
case 80:
					// line 265 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    yyVal = new ArrayList<TypeNode>();
    ((List)yyVal).add(((ReferenceTypeNode)yyVals[0+yyTop]));
 }
  break;
case 81:
					// line 269 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    ((List)yyVals[-2+yyTop]).add(((ReferenceTypeNode)yyVals[0+yyTop]));
 }
  break;
case 82:
					// line 274 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                      yyVal = new MethodSignatureNode(((String)yyVals[-3+yyTop]), ((List)yyVals[-1+yyTop]));
                  }
  break;
case 84:
					// line 280 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<ParameterNode>(); }
  break;
case 85:
					// line 283 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                          List<ParameterNode> list = new ArrayList<ParameterNode>();
                          list.add(((ParameterNode)yyVals[0+yyTop]));
                          yyVal = list;
                      }
  break;
case 86:
					// line 288 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                          ((List)yyVals[-2+yyTop]).add(((ParameterNode)yyVals[0+yyTop]));
                      }
  break;
case 87:
					// line 293 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]));
                 }
  break;
case 88:
					// line 296 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[0+yyTop]), null);
                 }
  break;
case 89:
					// line 299 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]), true);
                 }
  break;
case 90:
					// line 302 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[0+yyTop]), null, true);
                 }
  break;
case 91:
					// line 305 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]), false, true);
                 }
  break;
case 92:
					// line 308 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), null, false, true);
                 }
  break;
case 93:
					// line 311 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]), true, true);
                 }
  break;
case 94:
					// line 314 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), null, true, true);
                 }
  break;
case 95:
					// line 319 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                           yyVal = ((String)yyVals[0+yyTop]);
                       }
  break;
case 96:
					// line 322 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                           yyVal = ((String)yyVal) + "[]";
                       }
  break;
case 102:
					// line 334 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-1+yyTop]); }
  break;
case 104:
					// line 337 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-1+yyTop]); }
  break;
case 105:
					// line 338 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-2+yyTop]); }
  break;
case 108:
					// line 342 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-2+yyTop]); }
  break;
case 114:
					// line 350 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-1+yyTop]);}
  break;
case 115:
					// line 352 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-1+yyTop]); }
  break;
case 116:
					// line 354 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = null; }
  break;
case 117:
					// line 356 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(((TypeNode)yyVals[-2+yyTop]));
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 118:
					// line 362 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-5+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(((TypeNode)yyVals[-2+yyTop])); /* FIXME: <> part needs to be added*/
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 119:
					// line 368 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(PrimitiveTypeNode.VOID);
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 120:
					// line 374 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-5+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(PrimitiveTypeNode.VOID);
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
					// line 1026 "-"
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
					// line 382 "src/org/jruby/parser/JavaSignatureParser.y"

}
					// line 1062 "-"
