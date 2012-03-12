// created by jay 1.0.2 (c) 2002-2004 ats@cs.rit.edu
// skeleton Java 1.0 (c) 2002 ats@cs.rit.edu

					// line 17 "src/org/jruby/parser/JavaSignatureParser.y"
 
package org.jruby.parser;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jruby.ast.java_signature.Annotation;
import org.jruby.ast.java_signature.AnnotationExpression;
import org.jruby.ast.java_signature.AnnotationParameter;
import org.jruby.ast.java_signature.ArrayAnnotationExpression;
import org.jruby.ast.java_signature.ArrayTypeNode;
import org.jruby.ast.java_signature.ConstructorSignatureNode;
import org.jruby.ast.java_signature.DefaultAnnotationParameter;
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
					// line 36 "-"
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
  public static final int AT = 279;
  public static final int DOT = 280;
  public static final int COMMA = 281;
  public static final int ELLIPSIS = 282;
  public static final int EQUAL = 283;
  public static final int LCURLY = 284;
  public static final int RCURLY = 285;
  public static final int LPAREN = 286;
  public static final int RPAREN = 287;
  public static final int LBRACK = 288;
  public static final int RBRACK = 289;
  public static final int QUESTION = 290;
  public static final int LT = 291;
  public static final int GT = 292;
  public static final int THROWS = 293;
  public static final int EXTENDS = 294;
  public static final int SUPER = 295;
  public static final int RSHIFT = 296;
  public static final int URSHIFT = 297;
  public static final int yyErrorCode = 256;

  /** number of final state.
    */
  protected static final int yyFinal = 13;

  /** parser tables.
      Order is mandated by <i>jay</i>.
    */
  protected static final short[] yyLhs = {
//yyLhs 141
    -1,     0,     0,    17,    17,    16,    16,    16,    16,    16,
    16,    16,    16,    23,    23,    25,    18,    18,    19,    19,
    22,    21,    20,    20,    20,    20,    30,    30,    30,    36,
    36,    36,    37,    37,    37,    38,    38,    38,    39,    39,
    40,    40,    41,    32,    32,    42,    42,    43,    43,    44,
    44,    31,    31,    33,    33,    34,    34,    35,    35,     7,
     7,     8,     8,     9,    54,    54,    54,    54,    54,    54,
    54,    54,    54,    54,    54,    54,    24,    24,    55,    55,
    10,    10,    11,    11,     1,     5,     5,     6,     6,    15,
    15,    15,    15,    15,    15,    15,    15,    26,    26,    47,
    47,    48,    48,    45,    46,    46,    27,    27,    49,    49,
    50,    52,    52,    51,    51,    29,    29,    28,    56,     4,
     4,     3,     2,     2,     2,     2,    57,    57,    53,    58,
    58,    13,    13,    59,    59,    59,    60,    60,    14,    12,
    12,
    }, yyLen = {
//yyLen 141
     2,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     5,     1,     3,
     1,     1,     2,     2,     6,     4,     1,     3,     3,     2,
     3,     3,     2,     3,     3,     2,     3,     3,     2,     3,
     2,     3,     2,     1,     3,     1,     3,     1,     3,     1,
     3,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     2,     0,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     3,     2,     3,
     2,     0,     1,     3,     4,     1,     0,     1,     3,     2,
     1,     3,     2,     3,     2,     4,     3,     1,     3,     3,
     1,     1,     3,     2,     2,     2,     2,     3,     1,     1,
     3,     1,     1,     2,     1,     2,     2,     2,     0,     3,
     5,     4,     4,     6,     4,     6,     1,     4,     2,     3,
     1,     1,     3,     1,     1,     3,     1,     3,     0,     1,
     1,
    }, yyDefRed = {
//yyDefRed 213
     0,    64,    65,    66,    67,    68,    69,    70,    71,    72,
    73,    74,     0,     0,     1,     2,     0,     0,    60,     0,
    61,    75,    76,     0,    10,     5,     6,     7,     8,     9,
    11,    12,     0,     0,     0,     0,     0,     0,    13,    14,
     4,     0,    62,     0,     0,     0,     0,    15,     0,   100,
   101,     0,     0,     0,   119,     0,     0,     0,     0,     0,
     0,     0,     0,   140,     0,   130,   131,    77,     0,   124,
   104,     0,   105,   103,   108,   109,     0,     0,     0,     0,
     0,     0,    20,    82,     0,    78,     0,   122,     0,     0,
     0,     0,     0,    52,    43,     0,    45,    54,    53,     0,
     0,     0,     0,    87,     0,   127,     0,     0,     0,     0,
   106,    99,   102,     0,   120,     0,     0,     0,    79,    29,
     0,     0,     0,    38,     0,     0,     0,     0,   121,     0,
    97,     0,     0,   132,     0,   134,   133,   129,    84,     0,
     0,   107,   111,   110,   112,   125,   123,    83,     0,     0,
    30,     0,    31,     0,     0,     0,     0,    47,    56,    55,
    39,    44,    46,     0,     0,     0,    88,    93,     0,   136,
     0,     0,   117,     0,   116,   115,   113,     0,     0,     0,
    32,     0,    40,     0,     0,    95,    98,     0,   135,     0,
     0,    33,     0,    34,     0,     0,     0,    49,    58,    57,
    41,    48,   137,     0,     0,    35,    42,     0,     0,    36,
     0,    37,    50,
    }, yyDgoto = {
//yyDgoto 61
    13,    46,    14,    34,    15,   101,   102,    16,    17,    18,
    54,    80,    61,    62,    63,   103,    89,   104,    37,    38,
    39,   172,    83,    40,    92,    48,   132,    72,   140,   141,
    93,    94,    95,    96,   157,   197,    97,   158,   198,    98,
   159,   199,    99,   160,   200,    49,    50,    51,    52,    73,
    74,   142,   143,    19,    20,    56,    75,   136,    66,   137,
   170,
    }, yySindex = {
//yySindex 213
   244,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  -265,     0,     0,     0,    -4,   244,     0,  -255,
     0,     0,     0,  -221,     0,     0,     0,     0,     0,     0,
     0,     0,  -148,  -123,  -136,  -119,  -148,  -128,     0,     0,
     0,  -230,     0,  -162,  -111,  -116,  -136,     0,  -173,     0,
     0,  -114,   130,  -265,     0,  -115,  -113,  -136,    58,   139,
  -113,   -89,  -108,     0,   -75,     0,     0,     0,   139,     0,
     0,   185,     0,     0,     0,     0,  -123,  -148,  -136,  -148,
   -72,   -81,     0,     0,  -221,     0,   -77,     0,   -56,  -119,
   -57,   -52,  -219,     0,     0,   -37,     0,     0,     0,  -166,
   185,   -42,   -29,     0,  -122,     0,  -162,   102,   -25,  -243,
     0,     0,     0,  -136,     0,  -136,  -265,    58,     0,     0,
   185,   185,    81,     0,    58,  -265,  -113,  -121,     0,   139,
     0,   -13,   -22,     0,   102,     0,     0,     0,     0,   185,
   -15,     0,     0,     0,     0,     0,     0,     0,   -38,   -52,
     0,   -52,     0,   -92,   -26,  -254,   -12,     0,     0,     0,
     0,     0,     0,  -219,   -10,   -22,     0,     0,   -21,     0,
  -215,     0,     0,   -52,     0,     0,     0,  -265,   185,   185,
     0,    92,     0,    81,  -113,     0,     0,   102,     0,  -221,
  -254,     0,  -254,     0,   -46,  -144,    -7,     0,     0,     0,
     0,     0,     0,   185,   185,     0,     0,    92,  -144,     0,
  -144,     0,     0,
    }, yyRindex = {
//yyRindex 213
    37,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,    46,     0,   -44,
     0,     0,     0,   -80,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   270,  -175,     0,  -150,     0,     0,
     0,  -240,     0,   -25,     0,     0,   270,     0,    -3,     0,
     0,     0,     0,     0,     0,     0,    -6,   270,     0,    25,
   173,     0,    26,     0,     0,     0,     0,     0,    25,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   270,     0,
   277,     3,     0,     0,     1,     0,     0,     0,    -1,     0,
  -253,     7,   127,     0,     0,     0,     0,     0,     0,   186,
     0,     0,    38,     0,  -227,     0,     0,     0,     0,    -3,
     0,     0,     0,   270,     0,   270,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   199,  -186,     0,     0,
     0,  -182,  -157,     0,     0,     0,     0,     0,     0,     0,
     8,     0,     0,     0,     0,     0,     0,     0,     4,    43,
     0,    45,     0,    -1,  -188,     7,     0,     0,     0,     0,
     0,     0,     0,   144,  -140,  -138,     0,     0,     0,     0,
     0,  -248,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   212,     0,     0,     0,     0,     2,
    43,     0,    45,     0,    -1,     7,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,    43,     0,
    45,     0,     0,
    }, yyGindex = {
//yyGindex 61
     0,   -14,     0,   275,     0,   261,     0,     0,     0,     0,
   -23,     0,     0,     0,     0,   201,   -16,   -11,   -45,   -36,
     0,     0,   215,   -39,    -5,   -19,   205,     0,     0,   193,
     0,  -110,   -93,  -109,  -167,   129,     0,     0,     0,   -53,
  -158,   -58,   217,     0,     0,   271,   281,     0,     0,     0,
     0,   206,     0,     0,   350,   -35,   259,    10,   264,  -125,
     0,
    };
  protected static final short[] yyTable = {
//yyTable 524
    35,    16,    17,    18,    19,    36,    60,    23,    81,   169,
    21,    41,    22,    90,   161,   162,   201,    82,   110,    91,
   191,   193,    57,    69,    64,    18,    90,    21,    18,   156,
    21,    43,   109,    21,    87,   139,    35,    16,   123,    18,
   201,    79,   182,    35,    13,   191,   193,    41,    84,   123,
    44,    16,    35,    65,    90,   114,    59,    60,    55,    44,
    90,    44,   202,   113,   126,   115,   187,   150,   152,    55,
   188,    81,    90,   161,   162,    90,    90,   154,    91,    90,
    82,   149,   151,   155,    35,    91,   174,    64,   196,   127,
   145,    35,   146,    18,    90,    92,   135,   161,   162,    94,
   173,    92,     3,   171,    18,    94,     3,     3,    18,    18,
     3,    84,     3,    35,   125,    47,    65,    12,    35,    70,
   163,    71,    55,   135,    89,   150,   152,    18,   184,    45,
    89,    18,    18,   154,   154,    18,   154,    18,   154,   190,
   192,    96,   195,    91,   155,   209,   211,    96,   123,    91,
   150,   152,   182,   206,    47,   130,   130,    53,   154,   154,
   131,   164,   154,    58,   208,   210,    67,    76,   195,    55,
    68,    35,   189,   106,    85,    86,   135,   128,   128,   128,
   128,   128,   128,   128,   128,   128,   128,   128,   128,   128,
   128,   128,   128,   128,   128,   128,   128,   128,   105,   128,
   119,   128,   178,   179,   180,   128,   128,   128,   107,   116,
   117,   128,   118,   126,   126,   126,   126,   126,   126,   126,
   126,   126,   126,   126,   126,   126,   126,   126,   126,   126,
   126,   126,   126,   126,   122,   126,   119,   126,   120,   121,
   123,   126,   177,   126,   124,   128,   119,   126,   203,   204,
   180,   205,   129,    24,    25,    26,    27,    28,    29,    30,
    31,    32,   138,   139,   167,   181,   168,   185,   186,   183,
    81,    22,    22,    22,   207,    22,    22,    80,   118,    22,
    26,    22,    16,    17,    18,    19,    22,    33,    51,   114,
    22,    22,    16,    17,    63,    63,    63,    63,    63,    63,
    63,    63,    63,    59,    59,    59,    59,    59,    59,    59,
    59,    59,    86,   139,    63,    24,    25,    26,    27,    28,
    29,    30,    31,    59,    27,    85,    28,    78,    63,   108,
   166,   147,   165,   175,   148,    22,   212,    59,    24,    25,
    26,    27,    28,    29,    30,    31,   176,   111,    88,    24,
    25,    26,    27,    28,    29,    30,    31,   112,    22,    24,
    25,    26,    27,    28,    29,    30,    31,    42,   144,    22,
   133,   153,     0,     0,     0,     0,     0,     0,     0,    22,
     0,    12,   194,     0,     0,     0,   134,    24,    25,    26,
    27,    28,    29,    30,    31,    77,    24,    25,    26,    27,
    28,    29,    30,    31,    16,    16,     0,    22,    16,    16,
   100,     0,    16,     0,    16,     0,    22,     0,    16,    16,
     0,    17,    17,    16,    16,    17,    17,     0,     0,    17,
     0,    17,     0,     0,     0,    17,    17,     0,     0,     0,
    17,    17,    24,    25,    26,    27,    28,    29,    30,    31,
    23,    23,     0,     0,    23,    23,     0,     0,    23,     0,
    23,     0,    22,    19,    19,    23,     0,    19,    19,    23,
    23,    19,     0,    19,     0,     0,    25,    25,    19,     0,
    25,    25,    19,    19,    25,     0,    25,     0,     0,    24,
    24,    25,     0,    24,    24,    25,    25,    24,     0,    24,
     0,     0,     0,     0,    24,     0,     0,     0,    24,    24,
     1,     2,     3,     4,     5,     6,     7,     8,     9,    10,
    11,     0,     0,    12,
    };
  protected static final short[] yyCheck = {
//yyCheck 524
    16,     0,     0,     0,     0,    16,    41,    12,    53,   134,
     0,    16,   277,    58,   124,   124,   183,    53,    71,    58,
   178,   179,    36,    46,    43,   278,    71,    17,   281,   122,
   278,   286,    71,   281,    57,   278,    52,   277,   292,   292,
   207,    52,   296,    59,   292,   203,   204,    52,    53,   292,
   280,   291,    68,    43,   281,    78,   286,    92,   288,   280,
   287,   280,   187,    77,    99,    79,   281,   120,   121,   288,
   285,   116,   117,   183,   183,   120,   121,   122,   117,   124,
   116,   120,   121,   122,   100,   124,   139,   106,   181,   100,
   113,   107,   115,   281,   139,   281,   107,   207,   207,   281,
   139,   287,   277,   139,   292,   287,   281,   282,   296,   297,
   285,   116,   287,   129,   280,   277,   106,   279,   134,   292,
   125,   294,   288,   134,   281,   178,   179,   277,   163,   277,
   287,   281,   282,   178,   179,   285,   181,   287,   183,   178,
   179,   281,   181,   281,   183,   203,   204,   287,   292,   287,
   203,   204,   296,   297,   277,   277,   277,   293,   203,   204,
   282,   282,   207,   291,   203,   204,   277,   281,   207,   288,
   286,   187,   177,   281,   289,   288,   187,   257,   258,   259,
   260,   261,   262,   263,   264,   265,   266,   267,   268,   269,
   270,   271,   272,   273,   274,   275,   276,   277,   287,   279,
   292,   281,   294,   295,   296,   285,   286,   287,   283,   281,
   291,   291,   289,   257,   258,   259,   260,   261,   262,   263,
   264,   265,   266,   267,   268,   269,   270,   271,   272,   273,
   274,   275,   276,   277,   291,   279,   292,   281,   294,   295,
   292,   285,   280,   287,   281,   287,   292,   291,   294,   295,
   296,   297,   281,   257,   258,   259,   260,   261,   262,   263,
   264,   265,   287,   278,   277,   291,   288,   277,   289,   281,
     0,   277,   278,   277,   281,   281,   282,     0,   281,   285,
   281,   287,   281,   281,   281,   281,   292,   291,   281,   281,
   296,   297,   291,   291,   257,   258,   259,   260,   261,   262,
   263,   264,   265,   257,   258,   259,   260,   261,   262,   263,
   264,   265,   287,   287,   277,   257,   258,   259,   260,   261,
   262,   263,   264,   277,   281,   287,   281,    52,   291,    68,
   129,   116,   127,   140,   117,   277,   207,   291,   257,   258,
   259,   260,   261,   262,   263,   264,   140,    76,   290,   257,
   258,   259,   260,   261,   262,   263,   264,    76,   277,   257,
   258,   259,   260,   261,   262,   263,   264,    17,   109,   277,
   106,   290,    -1,    -1,    -1,    -1,    -1,    -1,    -1,   277,
    -1,   279,   290,    -1,    -1,    -1,   284,   257,   258,   259,
   260,   261,   262,   263,   264,   265,   257,   258,   259,   260,
   261,   262,   263,   264,   277,   278,    -1,   277,   281,   282,
   271,    -1,   285,    -1,   287,    -1,   277,    -1,   291,   292,
    -1,   277,   278,   296,   297,   281,   282,    -1,    -1,   285,
    -1,   287,    -1,    -1,    -1,   291,   292,    -1,    -1,    -1,
   296,   297,   257,   258,   259,   260,   261,   262,   263,   264,
   277,   278,    -1,    -1,   281,   282,    -1,    -1,   285,    -1,
   287,    -1,   277,   277,   278,   292,    -1,   281,   282,   296,
   297,   285,    -1,   287,    -1,    -1,   277,   278,   292,    -1,
   281,   282,   296,   297,   285,    -1,   287,    -1,    -1,   277,
   278,   292,    -1,   281,   282,   296,   297,   285,    -1,   287,
    -1,    -1,    -1,    -1,   292,    -1,    -1,    -1,   296,   297,
   266,   267,   268,   269,   270,   271,   272,   273,   274,   275,
   276,    -1,    -1,   279,
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
    "VOLATILE","STRICTFP","IDENTIFIER","AND","AT","DOT","COMMA",
    "ELLIPSIS","EQUAL","LCURLY","RCURLY","LPAREN","RPAREN","LBRACK",
    "RBRACK","QUESTION","LT","GT","THROWS","EXTENDS","SUPER","RSHIFT",
    "URSHIFT",
    };

//t  /** printable rules for debugging.
//t    */
//t  protected static final String [] yyRule = {
//t    "$accept : program",
//t    "program : method_header",
//t    "program : constructor_declaration",
//t    "type : primitive_type",
//t    "type : reference_type",
//t    "primitive_type : BYTE",
//t    "primitive_type : SHORT",
//t    "primitive_type : INT",
//t    "primitive_type : LONG",
//t    "primitive_type : CHAR",
//t    "primitive_type : BOOLEAN",
//t    "primitive_type : FLOAT",
//t    "primitive_type : DOUBLE",
//t    "reference_type : class_or_interface_type",
//t    "reference_type : array_type",
//t    "type_variable : IDENTIFIER",
//t    "class_or_interface : name",
//t    "class_or_interface : class_or_interface LT type_argument_list_1 DOT name",
//t    "class_or_interface_type : class_or_interface",
//t    "class_or_interface_type : class_or_interface LT type_argument_list_1",
//t    "class_type : class_or_interface_type",
//t    "interface_type : class_or_interface_type",
//t    "array_type : primitive_type dims",
//t    "array_type : name dims",
//t    "array_type : class_or_interface LT type_argument_list_1 DOT name dims",
//t    "array_type : class_or_interface LT type_argument_list_1 dims",
//t    "wildcard : QUESTION",
//t    "wildcard : QUESTION EXTENDS reference_type",
//t    "wildcard : QUESTION SUPER reference_type",
//t    "wildcard_1 : QUESTION GT",
//t    "wildcard_1 : QUESTION EXTENDS reference_type_1",
//t    "wildcard_1 : QUESTION SUPER reference_type_1",
//t    "wildcard_2 : QUESTION RSHIFT",
//t    "wildcard_2 : QUESTION EXTENDS reference_type_2",
//t    "wildcard_2 : QUESTION SUPER reference_type_2",
//t    "wildcard_3 : QUESTION URSHIFT",
//t    "wildcard_3 : QUESTION EXTENDS reference_type_3",
//t    "wildcard_3 : QUESTION SUPER reference_type_3",
//t    "reference_type_1 : reference_type GT",
//t    "reference_type_1 : class_or_interface LT type_argument_list_2",
//t    "reference_type_2 : reference_type RSHIFT",
//t    "reference_type_2 : class_or_interface LT type_argument_list_3",
//t    "reference_type_3 : reference_type URSHIFT",
//t    "type_argument_list : type_argument",
//t    "type_argument_list : type_argument_list COMMA type_argument",
//t    "type_argument_list_1 : type_argument_1",
//t    "type_argument_list_1 : type_argument_list COMMA type_argument_1",
//t    "type_argument_list_2 : type_argument_2",
//t    "type_argument_list_2 : type_argument_list COMMA type_argument_2",
//t    "type_argument_list_3 : type_argument_3",
//t    "type_argument_list_3 : type_argument_list COMMA type_argument_3",
//t    "type_argument : reference_type",
//t    "type_argument : wildcard",
//t    "type_argument_1 : reference_type_1",
//t    "type_argument_1 : wildcard_1",
//t    "type_argument_2 : reference_type_2",
//t    "type_argument_2 : wildcard_2",
//t    "type_argument_3 : reference_type_3",
//t    "type_argument_3 : wildcard_3",
//t    "modifiers_opt : modifiers",
//t    "modifiers_opt : modifiers_none",
//t    "modifiers : modifier",
//t    "modifiers : modifiers modifier",
//t    "modifiers_none :",
//t    "modifier : PUBLIC",
//t    "modifier : PROTECTED",
//t    "modifier : PRIVATE",
//t    "modifier : STATIC",
//t    "modifier : ABSTRACT",
//t    "modifier : FINAL",
//t    "modifier : NATIVE",
//t    "modifier : SYNCHRONIZED",
//t    "modifier : TRANSIENT",
//t    "modifier : VOLATILE",
//t    "modifier : STRICTFP",
//t    "modifier : annotation",
//t    "name : IDENTIFIER",
//t    "name : name DOT IDENTIFIER",
//t    "dims : LBRACK RBRACK",
//t    "dims : dims LBRACK RBRACK",
//t    "throws : THROWS class_type_list",
//t    "throws :",
//t    "class_type_list : class_type",
//t    "class_type_list : class_type_list COMMA class_type",
//t    "method_declarator : IDENTIFIER LPAREN formal_parameter_list_opt RPAREN",
//t    "formal_parameter_list_opt : formal_parameter_list",
//t    "formal_parameter_list_opt :",
//t    "formal_parameter_list : formal_parameter",
//t    "formal_parameter_list : formal_parameter_list COMMA formal_parameter",
//t    "formal_parameter : type variable_declarator_id",
//t    "formal_parameter : type",
//t    "formal_parameter : FINAL type variable_declarator_id",
//t    "formal_parameter : FINAL type",
//t    "formal_parameter : type ELLIPSIS IDENTIFIER",
//t    "formal_parameter : type ELLIPSIS",
//t    "formal_parameter : FINAL type ELLIPSIS IDENTIFIER",
//t    "formal_parameter : FINAL type ELLIPSIS",
//t    "variable_declarator_id : IDENTIFIER",
//t    "variable_declarator_id : variable_declarator_id LBRACK RBRACK",
//t    "type_parameter_list : type_parameter_list COMMA type_parameter",
//t    "type_parameter_list : type_parameter",
//t    "type_parameter_list_1 : type_parameter_1",
//t    "type_parameter_list_1 : type_parameter_list COMMA type_parameter_1",
//t    "type_parameter : type_variable type_bound_opt",
//t    "type_parameter_1 : type_variable GT",
//t    "type_parameter_1 : type_variable type_bound_1",
//t    "type_bound_1 : EXTENDS reference_type_1",
//t    "type_bound_1 : EXTENDS reference_type additional_bound_list_1",
//t    "type_bound_opt : type_bound",
//t    "type_bound_opt : none",
//t    "type_bound : EXTENDS reference_type additional_bound_list_opt",
//t    "additional_bound_list_opt : additional_bound_list",
//t    "additional_bound_list_opt : none",
//t    "additional_bound_list : additional_bound additional_bound_list",
//t    "additional_bound_list : additional_bound",
//t    "additional_bound_list_1 : additional_bound additional_bound_list_1",
//t    "additional_bound_list_1 : AND reference_type_1",
//t    "additional_bound : AND interface_type",
//t    "none :",
//t    "constructor_declaration : modifiers_opt constructor_declarator throws",
//t    "constructor_declaration : modifiers_opt LT type_parameter_list_1 constructor_declarator throws",
//t    "constructor_declarator : name LPAREN formal_parameter_list_opt RPAREN",
//t    "method_header : modifiers_opt type method_declarator throws",
//t    "method_header : modifiers_opt LT type_parameter_list_1 type method_declarator throws",
//t    "method_header : modifiers_opt VOID method_declarator throws",
//t    "method_header : modifiers_opt LT type_parameter_list_1 VOID method_declarator throws",
//t    "annotation : annotation_name",
//t    "annotation : annotation_name LPAREN annotation_params_opt RPAREN",
//t    "annotation_name : AT name",
//t    "annotation_param : type_variable EQUAL annotation_value",
//t    "annotation_param : annotation",
//t    "annotation_params : annotation_param",
//t    "annotation_params : annotation_params COMMA annotation_param",
//t    "annotation_value : annotation",
//t    "annotation_value : type",
//t    "annotation_value : LCURLY annotation_array_values RCURLY",
//t    "annotation_array_values : annotation_value",
//t    "annotation_array_values : annotation_array_values COMMA annotation_value",
//t    "annotation_params_none :",
//t    "annotation_params_opt : annotation_params",
//t    "annotation_params_opt : annotation_params_none",
//t    };
//t
//t  /** debugging support, requires the package <tt>jay.yydebug</tt>.
//t      Set to <tt>null</tt> to suppress debugging messages.
//t    */
//t  protected jay.yydebug.yyDebug yydebug;
//t
//t  /** index-checked interface to {@link #yyNames}.
//t      @param token single character or <tt>%token</tt> value.
//t      @return token name or <tt>[illegal]</tt> or <tt>[unknown]</tt>.
//t    */
//t  public static final String yyName (int token) {
//t    if (token < 0 || token > yyNames.length) return "[illegal]";
//t    String name;
//t    if ((name = yyNames[token]) != null) return name;
//t    return "[unknown]";
//t  }
//t

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
//t    this.yydebug = (jay.yydebug.yyDebug)ayydebug;
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
//t      if (yydebug != null) yydebug.push(yyState, yyVal);

      yyDiscarded: for (;;) {	// discarding a token does not change stack
        int yyN;
        if ((yyN = yyDefRed[yyState]) == 0) {	// else [default] reduce (yyN)
          if (yyToken < 0) {
            int a1 = yyLex.yylex();
            yyToken = a1 == -1 ? 0 : a1;
//t            if (yydebug != null)
//t              yydebug.lex(yyState, yyToken, yyName(yyToken), yyLex.value());
          }
          if ((yyN = yySindex[yyState]) != 0 && (yyN += yyToken) >= 0
              && yyN < yyTable.length && yyCheck[yyN] == yyToken) {
//t            if (yydebug != null)
//t              yydebug.shift(yyState, yyTable[yyN], yyErrorFlag-1);
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
//t              if (yydebug != null) yydebug.error("syntax error");
  
            case 1: case 2:
              yyErrorFlag = 3;
              do {
                if ((yyN = yySindex[yyStates[yyTop]]) != 0
                    && (yyN += yyErrorCode) >= 0 && yyN < yyTable.length
                    && yyCheck[yyN] == yyErrorCode) {
//t                  if (yydebug != null)
//t                    yydebug.shift(yyStates[yyTop], yyTable[yyN], 3);
                  yyState = yyTable[yyN];
                  yyVal = yyLex.value();
                  continue yyLoop;
                }
//t                if (yydebug != null) yydebug.pop(yyStates[yyTop]);
              } while (-- yyTop >= 0);
//t              if (yydebug != null) yydebug.reject();
              throw new ParserSyntaxException("irrecoverable syntax error");
  
            case 3:
              if (yyToken == 0) {
//t                if (yydebug != null) yydebug.reject();
                throw new ParserSyntaxException("irrecoverable syntax error at end-of-file");
              }
//t              if (yydebug != null)
//t                yydebug.discard(yyState, yyToken, yyName(yyToken),
//t  							yyLex.value());
              yyToken = -1;
              continue yyDiscarded;		// leave stack alone
            }
        }
        int yyV = yyTop + 1-yyLen[yyN];
//t        if (yydebug != null)
//t          yydebug.reduce(yyState, yyStates[yyV-1], yyN, yyRule[yyN], yyLen[yyN]);
        yyVal = yyDefault(yyV > yyTop ? null : yyVals[yyV]);
        switch (yyN) {
// ACTIONS_BEGIN
case 1:
					// line 127 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((MethodSignatureNode)yyVals[0+yyTop]);
 }
  break;
case 2:
					// line 129 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ConstructorSignatureNode)yyVals[0+yyTop]);
 }
  break;
case 4:
					// line 133 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((TypeNode)yyVals[0+yyTop]); }
  break;
case 5:
					// line 136 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.BYTE;
 }
  break;
case 6:
					// line 139 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.SHORT;
 }
  break;
case 7:
					// line 142 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.INT;
 }
  break;
case 8:
					// line 145 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.LONG;
 }
  break;
case 9:
					// line 148 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.CHAR;
 }
  break;
case 10:
					// line 151 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.BOOLEAN;
 }
  break;
case 11:
					// line 154 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.FLOAT;
 }
  break;
case 12:
					// line 157 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.DOUBLE;
 }
  break;
case 13:
					// line 162 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[0+yyTop]);
 }
  break;
case 14:
					// line 165 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[0+yyTop]);
 }
  break;
case 15:
					// line 170 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((String)yyVals[0+yyTop]); 
 }
  break;
case 16:
					// line 175 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = new ReferenceTypeNode(((String)yyVals[0+yyTop]));
 }
  break;
case 17:
					// line 178 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     String genericTyping = "<" + ((String)yyVals[-2+yyTop]) + "." + ((String)yyVals[0+yyTop]);
     yyVal = ((ReferenceTypeNode)yyVals[-4+yyTop]);
     ((ReferenceTypeNode)yyVals[-4+yyTop]).setGenericsTyping(genericTyping);
 }
  break;
case 19:
					// line 186 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     String genericTyping = "<" + ((String)yyVals[0+yyTop]);
     yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]);
     ((ReferenceTypeNode)yyVals[-2+yyTop]).setGenericsTyping(genericTyping);
 }
  break;
case 22:
					// line 199 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(((TypeNode)yyVals[-1+yyTop]));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 23:
					// line 203 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(new ReferenceTypeNode(((String)yyVals[-1+yyTop])));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 24:
					// line 207 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ReferenceTypeNode)yyVals[-5+yyTop]).setGenericsTyping("<" + ((String)yyVals[-3+yyTop]) + "." + ((String)yyVals[-1+yyTop]));
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(((ReferenceTypeNode)yyVals[-5+yyTop]));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 25:
					// line 212 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ReferenceTypeNode)yyVals[-3+yyTop]).setGenericsTyping("<" + ((String)yyVals[-1+yyTop]));
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(((ReferenceTypeNode)yyVals[-3+yyTop]));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 26:
					// line 219 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "?";
 }
  break;
case 27:
					// line 221 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? extends " + ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
 }
  break;
case 28:
					// line 223 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? super " + ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
 }
  break;
case 29:
					// line 228 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "?>"; 
 }
  break;
case 30:
					// line 230 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? extends " + ((String)yyVals[0+yyTop]);
 }
  break;
case 31:
					// line 232 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? super " + ((String)yyVals[0+yyTop]);
 }
  break;
case 32:
					// line 237 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "?>>"; 
 }
  break;
case 33:
					// line 239 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? extends " + ((String)yyVals[0+yyTop]);
 }
  break;
case 34:
					// line 241 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? super " + ((String)yyVals[0+yyTop]);
 }
  break;
case 35:
					// line 246 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "?>>";
 }
  break;
case 36:
					// line 248 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? extends " + ((String)yyVals[0+yyTop]);
 }
  break;
case 37:
					// line 250 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? super " + ((String)yyVals[0+yyTop]);
 }
  break;
case 38:
					// line 255 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ">";
 }
  break;
case 39:
					// line 257 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]).getFullyTypedName() + "<" + ((String)yyVals[0+yyTop]);
 }
  break;
case 40:
					// line 262 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ">>";
 }
  break;
case 41:
					// line 264 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]).getFullyTypedName() + "<" + ((String)yyVals[0+yyTop]);
 }
  break;
case 42:
					// line 269 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ">>>";
 }
  break;
case 43:
					// line 274 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[0+yyTop]);
 }
  break;
case 44:
					// line 277 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 46:
					// line 283 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 48:
					// line 289 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 50:
					// line 295 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 51:
					// line 300 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
 }
  break;
case 61:
					// line 318 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    yyVal = new ArrayList<Object>();
    ((List)yyVal).add(yyVals[0+yyTop]);
 }
  break;
case 62:
					// line 322 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    ((List)yyVals[-1+yyTop]).add(yyVals[0+yyTop]);
 }
  break;
case 63:
					// line 327 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<Object>(); }
  break;
case 64:
					// line 330 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PUBLIC; }
  break;
case 65:
					// line 331 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PROTECTED; }
  break;
case 66:
					// line 332 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PRIVATE; }
  break;
case 67:
					// line 333 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.STATIC; }
  break;
case 68:
					// line 334 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.ABSTRACT; }
  break;
case 69:
					// line 335 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.FINAL; }
  break;
case 70:
					// line 336 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.NATIVE; }
  break;
case 71:
					// line 337 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.SYNCHRONIZED; }
  break;
case 72:
					// line 338 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.TRANSIENT; }
  break;
case 73:
					// line 339 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.VOLATILE; }
  break;
case 74:
					// line 340 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.STRICTFP; }
  break;
case 75:
					// line 341 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((Annotation)yyVals[0+yyTop]); }
  break;
case 76:
					// line 344 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[0+yyTop]); }
  break;
case 77:
					// line 345 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-2+yyTop]) + "." + ((String)yyVals[0+yyTop]); }
  break;
case 78:
					// line 348 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = new ArrayTypeNode(); 
 }
  break;
case 79:
					// line 350 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = new ArrayTypeNode(((ArrayTypeNode)yyVals[-2+yyTop]));
 }
  break;
case 80:
					// line 355 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((List)yyVals[0+yyTop]); }
  break;
case 81:
					// line 356 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<TypeNode>(); }
  break;
case 82:
					// line 359 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    yyVal = new ArrayList<TypeNode>();
    ((List)yyVal).add(((ReferenceTypeNode)yyVals[0+yyTop]));
 }
  break;
case 83:
					// line 363 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    ((List)yyVals[-2+yyTop]).add(((ReferenceTypeNode)yyVals[0+yyTop]));
 }
  break;
case 84:
					// line 368 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                      yyVal = new MethodSignatureNode(((String)yyVals[-3+yyTop]), ((List)yyVals[-1+yyTop]));
                  }
  break;
case 86:
					// line 374 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<ParameterNode>(); }
  break;
case 87:
					// line 377 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                          List<ParameterNode> list = new ArrayList<ParameterNode>();
                          list.add(((ParameterNode)yyVals[0+yyTop]));
                          yyVal = list;
                      }
  break;
case 88:
					// line 382 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                          ((List)yyVals[-2+yyTop]).add(((ParameterNode)yyVals[0+yyTop]));
                      }
  break;
case 89:
					// line 387 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]));
                 }
  break;
case 90:
					// line 390 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[0+yyTop]), null);
                 }
  break;
case 91:
					// line 393 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]), true);
                 }
  break;
case 92:
					// line 396 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[0+yyTop]), null, true);
                 }
  break;
case 93:
					// line 399 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]), false, true);
                 }
  break;
case 94:
					// line 402 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), null, false, true);
                 }
  break;
case 95:
					// line 405 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]), true, true);
                 }
  break;
case 96:
					// line 408 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), null, true, true);
                 }
  break;
case 97:
					// line 413 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[0+yyTop]);
 }
  break;
case 98:
					// line 415 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     /* We know this is always preceeded by 'type' production.*/
     yyVals[-3+yyTop] = new ArrayTypeNode(((TypeNode)yyVals[-3+yyTop])); 
     yyVal = ((String)yyVals[-2+yyTop]);
 }
  break;
case 99:
					// line 422 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 102:
					// line 428 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 103:
					// line 433 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 104:
					// line 438 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((String)yyVals[-1+yyTop]) + ">"; 
 }
  break;
case 105:
					// line 441 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 106:
					// line 446 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " extends " + ((String)yyVals[-1+yyTop]);
 }
  break;
case 107:
					// line 449 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " extends " + ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ((String)yyVals[0+yyTop]);
 }
  break;
case 109:
					// line 455 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "";
 }
  break;
case 110:
					// line 460 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "extends " + ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ((String)yyVals[0+yyTop]);
 }
  break;
case 112:
					// line 466 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "";
 }
  break;
case 113:
					// line 471 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 115:
					// line 476 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 116:
					// line 479 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " & " + ((String)yyVals[-1+yyTop]);
 }
  break;
case 117:
					// line 484 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " & " + ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
}
  break;
case 118:
					// line 488 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = null; }
  break;
case 119:
					// line 490 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ConstructorSignatureNode)yyVals[-1+yyTop]);
     ((ConstructorSignatureNode)yyVal).setModifiers(((List)yyVals[-2+yyTop]));
     ((ConstructorSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
 }
  break;
case 120:
					// line 494 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ConstructorSignatureNode)yyVals[-1+yyTop]);
     ((ConstructorSignatureNode)yyVal).setModifiers(((List)yyVals[-4+yyTop]));
     ((ConstructorSignatureNode)yyVal).setExtraTypeInfo("<" + ((String)yyVals[-2+yyTop]));
     ((ConstructorSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
 }
  break;
case 121:
					// line 501 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = new ConstructorSignatureNode(((String)yyVals[-3+yyTop]), ((List)yyVals[-1+yyTop]));
 }
  break;
case 122:
					// line 505 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(((TypeNode)yyVals[-2+yyTop]));
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 123:
					// line 511 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-5+yyTop]));
                  ((MethodSignatureNode)yyVal).setExtraTypeInfo("<" + ((String)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(((TypeNode)yyVals[-2+yyTop]));
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 124:
					// line 518 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(PrimitiveTypeNode.VOID);
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 125:
					// line 524 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-5+yyTop]));
                  ((MethodSignatureNode)yyVal).setExtraTypeInfo("<" + ((String)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(PrimitiveTypeNode.VOID);
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 126:
					// line 533 "src/org/jruby/parser/JavaSignatureParser.y"
  {
               yyVal = new Annotation(((String)yyVals[0+yyTop]), new ArrayList<AnnotationParameter>());
           }
  break;
case 127:
					// line 536 "src/org/jruby/parser/JavaSignatureParser.y"
  {
               yyVal = new Annotation(((String)yyVals[-3+yyTop]), ((List)yyVals[-1+yyTop]));
           }
  break;
case 128:
					// line 541 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]); }
  break;
case 129:
					// line 544 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new AnnotationParameter(((String)yyVals[-2+yyTop]), ((AnnotationExpression)yyVals[0+yyTop]));
                 }
  break;
case 130:
					// line 547 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new DefaultAnnotationParameter(((Annotation)yyVals[0+yyTop]));
                 }
  break;
case 131:
					// line 552 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                      yyVal = new ArrayList<AnnotationParameter>();
                      ((List)yyVal).add(((AnnotationParameter)yyVals[0+yyTop]));
                  }
  break;
case 132:
					// line 556 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                      ((List)yyVals[-2+yyTop]).add(((AnnotationParameter)yyVals[0+yyTop]));
                  }
  break;
case 133:
					// line 561 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = ((AnnotationExpression)yyVals[0+yyTop]);
                 }
  break;
case 134:
					// line 564 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = ((AnnotationExpression)yyVals[0+yyTop]);
                 }
  break;
case 135:
					// line 567 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ArrayAnnotationExpression(((List)yyVals[-1+yyTop]));
                 }
  break;
case 136:
					// line 572 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                            yyVal = new ArrayList<AnnotationExpression>();
                        }
  break;
case 137:
					// line 575 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                            ((List)yyVals[-2+yyTop]).add(((AnnotationExpression)yyVals[0+yyTop]));
                        }
  break;
case 138:
					// line 580 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<AnnotationParameter>(); }
  break;
					// line 1374 "-"
// ACTIONS_END
        }
        yyTop -= yyLen[yyN];
        yyState = yyStates[yyTop];
        int yyM = yyLhs[yyN];
        if (yyState == 0 && yyM == 0) {
//t          if (yydebug != null) yydebug.shift(0, yyFinal);
          yyState = yyFinal;
          if (yyToken < 0) {
            int a1 = yyLex.yylex();
            yyToken = a1 == -1 ? 0 : a1;
//t            if (yydebug != null)
//t               yydebug.lex(yyState, yyToken,yyName(yyToken), yyLex.value());
          }
          if (yyToken == 0) {
//t            if (yydebug != null) yydebug.accept(yyVal);
            return yyVal;
          }
          continue yyLoop;
        }
        if ((yyN = yyGindex[yyM]) != 0 && (yyN += yyState) >= 0
            && yyN < yyTable.length && yyCheck[yyN] == yyState)
          yyState = yyTable[yyN];
        else
          yyState = yyDgoto[yyM];
//t        if (yydebug != null) yydebug.shift(yyStates[yyTop], yyState);
        continue yyLoop;
      }
    }
  }

// ACTION_BODIES
					// line 586 "src/org/jruby/parser/JavaSignatureParser.y"

}
					// line 1410 "-"
