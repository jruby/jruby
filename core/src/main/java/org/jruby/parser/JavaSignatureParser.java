// created by jay 1.0.2 (c) 2002-2004 ats@cs.rit.edu
// skeleton Java 1.0 (c) 2002 ats@cs.rit.edu

					// line 17 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
 
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
import org.jruby.ast.java_signature.BoolLiteral;
import org.jruby.ast.java_signature.CharacterLiteral;
import org.jruby.ast.java_signature.ConstructorSignatureNode;
import org.jruby.ast.java_signature.DefaultAnnotationParameter;
import org.jruby.ast.java_signature.MethodSignatureNode;
import org.jruby.ast.java_signature.Literal;
import org.jruby.ast.java_signature.Modifier;
import org.jruby.ast.java_signature.NumberLiteral;
import org.jruby.ast.java_signature.ParameterNode;
import org.jruby.ast.java_signature.PrimitiveTypeNode;
import org.jruby.ast.java_signature.ReferenceTypeNode;
import org.jruby.ast.java_signature.SignatureNode;
import org.jruby.ast.java_signature.StringLiteral;
import org.jruby.ast.java_signature.TypeNode;
import org.jruby.lexer.JavaSignatureLexer;

public class JavaSignatureParser {
    private static JavaSignatureParser parser = new JavaSignatureParser();

    public static SignatureNode parse(InputStream in) throws IOException, ParserSyntaxException {
        return (SignatureNode) parser.yyparse(JavaSignatureLexer.create(in));
    }
					// line 41 "-"
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
  public static final int QQ = 298;
  public static final int Q = 299;
  public static final int CHARACTER_LITERAL = 300;
  public static final int STRING_LITERAL = 301;
  public static final int TRUE_LITERAL = 302;
  public static final int FALSE_LITERAL = 303;
  public static final int NUM_LITERAL = 304;
  public static final int HEXNUM_LITERAL = 305;
  public static final int yyErrorCode = 256;

  /** number of final state.
    */
  protected static final int yyFinal = 13;

  /** parser tables.
      Order is mandated by <i>jay</i>.
    */
  protected static final short[] yyLhs = {
//yyLhs 149
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
    58,    13,    13,    59,    59,    59,    59,    59,    60,    60,
    14,    12,    12,    61,    61,    61,    61,    61,    61,
    }, yyLen = {
//yyLen 149
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
     1,     1,     3,     1,     1,     1,     3,     2,     1,     3,
     0,     1,     1,     1,     1,     1,     1,     1,     1,
    }, yyDefRed = {
//yyDefRed 222
     0,    64,    65,    66,    67,    68,    69,    70,    71,    72,
    73,    74,     0,     0,     1,     2,     0,     0,    60,     0,
    61,    75,    76,     0,    10,     5,     6,     7,     8,     9,
    11,    12,     0,     0,     0,     0,     0,     0,    13,    14,
     4,     0,    62,     0,     0,     0,     0,    15,     0,   100,
   101,     0,     0,     0,   119,     0,     0,     0,     0,     0,
     0,     0,     0,   144,   143,   145,   146,   147,   148,     0,
     0,   142,   135,     0,     0,   133,   131,   130,   134,    77,
     0,   124,   104,     0,   105,   103,   108,   109,     0,     0,
     0,     0,     0,     0,    20,    82,     0,    78,     0,   122,
     0,     0,     0,     0,    52,    43,     0,    45,    54,    53,
     0,     0,     0,     0,    87,     0,   137,   138,     0,   127,
     0,     0,     0,     0,   106,    99,   102,     0,   120,     0,
     0,     0,    79,    29,     0,     0,     0,    38,     0,     0,
     0,     0,   121,     0,    97,     0,     0,     0,   136,   132,
   129,    84,     0,     0,   107,   111,   110,   112,   125,   123,
    83,     0,     0,    30,     0,    31,     0,     0,     0,     0,
    47,    56,    55,    39,    44,    46,     0,     0,     0,    88,
    93,     0,   139,     0,   117,     0,   116,   115,   113,     0,
     0,     0,    32,     0,    40,     0,     0,    95,    98,     0,
     0,    33,     0,    34,     0,     0,     0,    49,    58,    57,
    41,    48,     0,     0,    35,    42,     0,     0,    36,     0,
    37,    50,
    }, yyDgoto = {
//yyDgoto 62
    13,    46,    14,    34,    15,   112,   113,    16,    17,    18,
    54,    92,    69,    70,    71,   114,   101,    72,    37,    38,
    39,   184,    95,    40,    73,    48,   146,    84,   153,   154,
   104,   105,   106,   107,   170,   207,   108,   171,   208,   109,
   172,   209,   110,   173,   210,    49,    50,    51,    52,    85,
    86,   155,   156,    19,    20,    56,    87,    75,    76,    77,
   118,    78,
    }, yySindex = {
//yySindex 222
   317,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  -250,     0,     0,     0,   107,   317,     0,  -249,
     0,     0,     0,  -174,     0,     0,     0,     0,     0,     0,
     0,     0,  -168,  -161,  -171,  -169,  -168,  -146,     0,     0,
     0,  -218,     0,   -65,  -114,  -135,  -171,     0,  -239,     0,
     0,  -113,    -6,  -250,     0,  -122,  -115,  -171,   128,   196,
  -115,     0,   -74,     0,     0,     0,     0,     0,     0,  -107,
  -111,     0,     0,  -248,  -102,     0,     0,     0,     0,     0,
   196,     0,     0,   245,     0,     0,     0,     0,  -161,  -168,
  -171,  -168,   -90,   -91,     0,     0,  -174,     0,   -87,     0,
   -88,  -169,   -83,   -77,     0,     0,   -80,     0,     0,     0,
  -173,   245,   -78,   -61,     0,  -148,     0,     0,  -199,     0,
   -65,   -14,   -71,  -235,     0,     0,     0,  -171,     0,  -171,
  -250,   128,     0,     0,   245,   245,   162,     0,   128,  -250,
  -115,  -144,     0,   196,     0,   -55,   -63,   -14,     0,     0,
     0,     0,   245,   -46,     0,     0,     0,     0,     0,     0,
     0,   -56,   -77,     0,   -77,     0,  -139,   -58,  -132,   -40,
     0,     0,     0,     0,     0,     0,  -248,   -43,   -63,     0,
     0,   -29,     0,     0,     0,   -77,     0,     0,     0,  -250,
   245,   245,     0,   172,     0,   162,  -115,     0,     0,  -174,
  -132,     0,  -132,     0,  -118,  -166,   -39,     0,     0,     0,
     0,     0,   245,   245,     0,     0,   172,  -166,     0,  -166,
     0,     0,
    }, yyRindex = {
//yyRindex 222
   116,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   151,     0,    72,
     0,     0,     0,    37,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   261,   -64,     0,   -13,     0,     0,
     0,  -217,     0,   -25,     0,     0,   261,     0,   -15,     0,
     0,     0,     0,     0,     0,     0,   233,   261,     0,   -12,
   246,  -163,     0,     0,     0,     0,     0,     0,     0,     0,
   -11,     0,     0,   187,     0,     0,     0,     0,     0,     0,
   -12,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   261,     0,   267,     3,     0,     0,     1,     0,     0,     0,
    -4,     0,  -258,    -2,     0,     0,     0,     0,     0,     0,
   259,     0,     0,    -9,     0,  -259,     0,     0,     0,     0,
     0,     0,     0,   -15,     0,     0,     0,   261,     0,   261,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   272,  -223,     0,     0,     0,  -208,  -177,     0,     0,     0,
     0,     0,     0,    -1,     0,     0,     0,     0,     0,     0,
     0,     4,    34,     0,    36,     0,    -4,  -231,    -2,     0,
     0,     0,     0,     0,     0,     0,   204,  -160,  -140,     0,
     0,     0,     0,  -240,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   285,     0,     0,     2,
    34,     0,    36,     0,    -4,    -2,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,    34,     0,    36,
     0,     0,
    }, yyGindex = {
//yyGindex 62
     0,   -20,     0,   221,     0,   201,     0,     0,     0,     0,
   -27,     0,     0,     0,     0,   176,    -8,    -3,   -47,   -38,
     0,     0,   190,   -41,    -7,   -19,   180,     0,     0,   173,
     0,  -120,  -126,  -117,  -183,   109,     0,     0,     0,   -54,
  -165,   -51,   219,     0,     0,   239,   264,     0,     0,     0,
     0,   202,     0,     0,   337,   -34,   235,    14,   236,   -62,
     0,     0,
    };
  protected static final short[] yyTable = {
//yyTable 597
   117,    16,    17,    18,    19,    23,    93,    60,    35,    41,
   169,   102,   211,    36,    21,    94,    57,   103,   174,    81,
    18,   175,    90,    18,    74,   201,   203,    22,    90,   124,
    99,    21,    44,   211,    18,    35,   102,    43,    21,    60,
    55,    21,   123,   152,    35,    41,    96,   201,   203,    91,
    18,    35,    13,    82,    35,    83,   115,   137,    92,   150,
    16,    18,    44,   128,    92,    18,    18,   206,    59,   127,
    55,   129,    35,    94,    16,   174,   140,   115,   175,    94,
   163,   165,   147,    93,   102,   182,   148,   102,   102,   167,
   103,   102,    94,   162,   164,   168,   174,   103,   186,   175,
   158,    74,   159,    35,    89,   102,    44,   139,   141,    45,
    89,   185,    35,    35,   183,    55,    47,    76,    76,    55,
    15,    96,    53,    96,    76,    76,   137,    96,    76,   144,
   194,   215,   176,   144,   145,    35,   163,   165,   177,    35,
   115,    91,   196,   167,   167,    58,   167,    91,   167,   200,
   202,    80,   205,   133,   168,   190,   191,   192,   163,   165,
   137,   218,   220,    79,   194,   167,   167,    97,    88,   167,
   120,   217,   219,    98,   133,   205,   212,   213,   192,   214,
   119,   121,   199,    24,    25,    26,    27,    28,    29,    30,
    31,   130,    24,    25,    26,    27,    28,    29,    30,    31,
   131,   138,   132,    22,   133,    12,   134,   135,   136,   142,
    62,   116,    61,     3,    12,   137,   151,     3,     3,    62,
   143,     3,   180,     3,   189,   181,    63,    64,    65,    66,
    67,    68,   152,   193,   197,    63,    64,    65,    66,    67,
    68,   195,   216,    24,    25,    26,    27,    28,    29,    30,
    31,    24,    25,    26,    27,    28,    29,    30,    31,    89,
   198,    81,   140,    22,    18,    12,   118,    80,    18,    18,
    62,    22,    18,    90,    18,    86,   141,    26,    85,    51,
   114,   122,    16,    17,    18,    19,    63,    64,    65,    66,
    67,    68,    16,    17,   128,   128,   128,   128,   128,   128,
   128,   128,   128,   128,   128,   128,   128,   128,   128,   128,
   128,   128,   128,   128,   128,    27,   128,    28,   128,   179,
   160,   178,   128,   128,   128,   221,   187,   125,   128,   126,
   126,   126,   126,   126,   126,   126,   126,   126,   126,   126,
   126,   126,   126,   126,   126,   126,   126,   126,   126,   126,
   161,   126,   126,   126,    42,   188,   149,   126,   157,   126,
     0,     0,     0,   126,    24,    25,    26,    27,    28,    29,
    30,    31,    32,    63,    63,    63,    63,    63,    63,    63,
    63,    63,     0,     0,    22,    24,    25,    26,    27,    28,
    29,    30,    31,    63,     0,     0,     0,     0,    33,     0,
     0,     0,     0,     0,     0,    22,     0,    63,    59,    59,
    59,    59,    59,    59,    59,    59,    59,     0,   100,    24,
    25,    26,    27,    28,    29,    30,    31,     0,    59,    24,
    25,    26,    27,    28,    29,    30,    31,     0,     0,    22,
     0,     0,    59,     0,     0,     0,     0,     0,     0,    22,
     0,     0,   166,    24,    25,    26,    27,    28,    29,    30,
    31,     0,   204,     0,    16,    16,     0,   111,    16,    16,
     0,     0,    16,    22,    16,     0,     0,     0,    16,    16,
     0,    17,    17,    16,    16,    17,    17,     0,     0,    17,
     0,    17,     0,     0,     0,    17,    17,     0,     0,     0,
    17,    17,    24,    25,    26,    27,    28,    29,    30,    31,
    22,    22,     0,     0,    22,    22,     0,     0,    22,     0,
    22,     0,    22,    23,    23,    22,     0,    23,    23,    22,
    22,    23,     0,    23,     0,     0,    19,    19,    23,     0,
    19,    19,    23,    23,    19,     0,    19,     0,     0,    25,
    25,    19,     0,    25,    25,    19,    19,    25,     0,    25,
     0,     0,    24,    24,    25,     0,    24,    24,    25,    25,
    24,     0,    24,     0,     0,     0,     0,    24,     0,     0,
     0,    24,    24,     1,     2,     3,     4,     5,     6,     7,
     8,     9,    10,    11,     0,     0,    12,
    };
  protected static final short[] yyCheck = {
//yyCheck 597
    62,     0,     0,     0,     0,    12,    53,    41,    16,    16,
   136,    58,   195,    16,     0,    53,    36,    58,   138,    46,
   278,   138,   281,   281,    43,   190,   191,   277,   287,    83,
    57,    17,   280,   216,   292,    43,    83,   286,   278,    73,
   288,   281,    83,   278,    52,    52,    53,   212,   213,    52,
   281,    59,   292,   292,    62,   294,    59,   292,   281,   121,
   277,   292,   280,    90,   287,   296,   297,   193,   286,    89,
   288,    91,    80,   281,   291,   195,   110,    80,   195,   287,
   134,   135,   281,   130,   131,   147,   285,   134,   135,   136,
   131,   138,   130,   134,   135,   136,   216,   138,   152,   216,
   127,   120,   129,   111,   281,   152,   280,   280,   111,   277,
   287,   152,   120,   121,   152,   288,   277,   280,   281,   288,
   283,   281,   293,   130,   287,   288,   292,   287,   291,   277,
   296,   297,   139,   277,   282,   143,   190,   191,   282,   147,
   143,   281,   176,   190,   191,   291,   193,   287,   195,   190,
   191,   286,   193,   292,   195,   294,   295,   296,   212,   213,
   292,   212,   213,   277,   296,   212,   213,   289,   281,   216,
   281,   212,   213,   288,   292,   216,   294,   295,   296,   297,
   287,   283,   189,   257,   258,   259,   260,   261,   262,   263,
   264,   281,   257,   258,   259,   260,   261,   262,   263,   264,
   291,   281,   289,   277,   292,   279,   294,   295,   291,   287,
   284,   285,   277,   277,   279,   292,   287,   281,   282,   284,
   281,   285,   277,   287,   280,   288,   300,   301,   302,   303,
   304,   305,   278,   291,   277,   300,   301,   302,   303,   304,
   305,   281,   281,   257,   258,   259,   260,   261,   262,   263,
   264,   257,   258,   259,   260,   261,   262,   263,   264,   265,
   289,     0,   287,   277,   277,   279,   281,     0,   281,   282,
   284,   277,   285,    52,   287,   287,   287,   281,   287,   281,
   281,    80,   281,   281,   281,   281,   300,   301,   302,   303,
   304,   305,   291,   291,   257,   258,   259,   260,   261,   262,
   263,   264,   265,   266,   267,   268,   269,   270,   271,   272,
   273,   274,   275,   276,   277,   281,   279,   281,   281,   143,
   130,   141,   285,   286,   287,   216,   153,    88,   291,   257,
   258,   259,   260,   261,   262,   263,   264,   265,   266,   267,
   268,   269,   270,   271,   272,   273,   274,   275,   276,   277,
   131,   279,    88,   281,    17,   153,   120,   285,   123,   287,
    -1,    -1,    -1,   291,   257,   258,   259,   260,   261,   262,
   263,   264,   265,   257,   258,   259,   260,   261,   262,   263,
   264,   265,    -1,    -1,   277,   257,   258,   259,   260,   261,
   262,   263,   264,   277,    -1,    -1,    -1,    -1,   291,    -1,
    -1,    -1,    -1,    -1,    -1,   277,    -1,   291,   257,   258,
   259,   260,   261,   262,   263,   264,   265,    -1,   290,   257,
   258,   259,   260,   261,   262,   263,   264,    -1,   277,   257,
   258,   259,   260,   261,   262,   263,   264,    -1,    -1,   277,
    -1,    -1,   291,    -1,    -1,    -1,    -1,    -1,    -1,   277,
    -1,    -1,   290,   257,   258,   259,   260,   261,   262,   263,
   264,    -1,   290,    -1,   277,   278,    -1,   271,   281,   282,
    -1,    -1,   285,   277,   287,    -1,    -1,    -1,   291,   292,
    -1,   277,   278,   296,   297,   281,   282,    -1,    -1,   285,
    -1,   287,    -1,    -1,    -1,   291,   292,    -1,    -1,    -1,
   296,   297,   257,   258,   259,   260,   261,   262,   263,   264,
   277,   278,    -1,    -1,   281,   282,    -1,    -1,   285,    -1,
   287,    -1,   277,   277,   278,   292,    -1,   281,   282,   296,
   297,   285,    -1,   287,    -1,    -1,   277,   278,   292,    -1,
   281,   282,   296,   297,   285,    -1,   287,    -1,    -1,   277,
   278,   292,    -1,   281,   282,   296,   297,   285,    -1,   287,
    -1,    -1,   277,   278,   292,    -1,   281,   282,   296,   297,
   285,    -1,   287,    -1,    -1,    -1,    -1,   292,    -1,    -1,
    -1,   296,   297,   266,   267,   268,   269,   270,   271,   272,
   273,   274,   275,   276,    -1,    -1,   279,
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
    "URSHIFT","QQ","Q","CHARACTER_LITERAL","STRING_LITERAL",
    "TRUE_LITERAL","FALSE_LITERAL","NUM_LITERAL","HEXNUM_LITERAL",
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
					// line 141 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((MethodSignatureNode)yyVals[0+yyTop]);
 }
  break;
case 2:
					// line 143 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ConstructorSignatureNode)yyVals[0+yyTop]);
 }
  break;
case 4:
					// line 147 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((TypeNode)yyVals[0+yyTop]); }
  break;
case 5:
					// line 150 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.BYTE;
 }
  break;
case 6:
					// line 153 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.SHORT;
 }
  break;
case 7:
					// line 156 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.INT;
 }
  break;
case 8:
					// line 159 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.LONG;
 }
  break;
case 9:
					// line 162 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.CHAR;
 }
  break;
case 10:
					// line 165 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.BOOLEAN;
 }
  break;
case 11:
					// line 168 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.FLOAT;
 }
  break;
case 12:
					// line 171 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.DOUBLE;
 }
  break;
case 13:
					// line 176 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[0+yyTop]);
 }
  break;
case 14:
					// line 179 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[0+yyTop]);
 }
  break;
case 15:
					// line 184 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((String)yyVals[0+yyTop]); 
 }
  break;
case 16:
					// line 189 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = new ReferenceTypeNode(((String)yyVals[0+yyTop]));
 }
  break;
case 17:
					// line 192 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     String genericTyping = "<" + ((String)yyVals[-2+yyTop]) + "." + ((String)yyVals[0+yyTop]);
     yyVal = ((ReferenceTypeNode)yyVals[-4+yyTop]);
     ((ReferenceTypeNode)yyVals[-4+yyTop]).setGenericsTyping(genericTyping);
 }
  break;
case 19:
					// line 200 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     String genericTyping = "<" + ((String)yyVals[0+yyTop]);
     yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]);
     ((ReferenceTypeNode)yyVals[-2+yyTop]).setGenericsTyping(genericTyping);
 }
  break;
case 22:
					// line 213 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(((TypeNode)yyVals[-1+yyTop]));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 23:
					// line 217 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(new ReferenceTypeNode(((String)yyVals[-1+yyTop])));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 24:
					// line 221 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ReferenceTypeNode)yyVals[-5+yyTop]).setGenericsTyping("<" + ((String)yyVals[-3+yyTop]) + "." + ((String)yyVals[-1+yyTop]));
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(((ReferenceTypeNode)yyVals[-5+yyTop]));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 25:
					// line 226 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ReferenceTypeNode)yyVals[-3+yyTop]).setGenericsTyping("<" + ((String)yyVals[-1+yyTop]));
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(((ReferenceTypeNode)yyVals[-3+yyTop]));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 26:
					// line 233 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "?";
 }
  break;
case 27:
					// line 235 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? extends " + ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
 }
  break;
case 28:
					// line 237 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? super " + ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
 }
  break;
case 29:
					// line 242 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "?>"; 
 }
  break;
case 30:
					// line 244 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? extends " + ((String)yyVals[0+yyTop]);
 }
  break;
case 31:
					// line 246 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? super " + ((String)yyVals[0+yyTop]);
 }
  break;
case 32:
					// line 251 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "?>>"; 
 }
  break;
case 33:
					// line 253 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? extends " + ((String)yyVals[0+yyTop]);
 }
  break;
case 34:
					// line 255 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? super " + ((String)yyVals[0+yyTop]);
 }
  break;
case 35:
					// line 260 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "?>>";
 }
  break;
case 36:
					// line 262 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? extends " + ((String)yyVals[0+yyTop]);
 }
  break;
case 37:
					// line 264 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? super " + ((String)yyVals[0+yyTop]);
 }
  break;
case 38:
					// line 269 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ">";
 }
  break;
case 39:
					// line 271 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]).getFullyTypedName() + "<" + ((String)yyVals[0+yyTop]);
 }
  break;
case 40:
					// line 276 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ">>";
 }
  break;
case 41:
					// line 278 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]).getFullyTypedName() + "<" + ((String)yyVals[0+yyTop]);
 }
  break;
case 42:
					// line 283 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ">>>";
 }
  break;
case 43:
					// line 288 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[0+yyTop]);
 }
  break;
case 44:
					// line 291 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 46:
					// line 297 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 48:
					// line 303 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 50:
					// line 309 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 51:
					// line 314 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
 }
  break;
case 61:
					// line 332 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
    yyVal = new ArrayList<Object>();
    ((List)yyVal).add(yyVals[0+yyTop]);
 }
  break;
case 62:
					// line 336 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
    ((List)yyVals[-1+yyTop]).add(yyVals[0+yyTop]);
 }
  break;
case 63:
					// line 341 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<Object>(); }
  break;
case 64:
					// line 344 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PUBLIC; }
  break;
case 65:
					// line 345 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PROTECTED; }
  break;
case 66:
					// line 346 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PRIVATE; }
  break;
case 67:
					// line 347 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.STATIC; }
  break;
case 68:
					// line 348 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.ABSTRACT; }
  break;
case 69:
					// line 349 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.FINAL; }
  break;
case 70:
					// line 350 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.NATIVE; }
  break;
case 71:
					// line 351 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.SYNCHRONIZED; }
  break;
case 72:
					// line 352 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.TRANSIENT; }
  break;
case 73:
					// line 353 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.VOLATILE; }
  break;
case 74:
					// line 354 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.STRICTFP; }
  break;
case 75:
					// line 355 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((Annotation)yyVals[0+yyTop]); }
  break;
case 76:
					// line 358 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[0+yyTop]); }
  break;
case 77:
					// line 359 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-2+yyTop]) + "." + ((String)yyVals[0+yyTop]); }
  break;
case 78:
					// line 362 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = new ArrayTypeNode(); 
 }
  break;
case 79:
					// line 364 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = new ArrayTypeNode(((ArrayTypeNode)yyVals[-2+yyTop]));
 }
  break;
case 80:
					// line 369 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((List)yyVals[0+yyTop]); }
  break;
case 81:
					// line 370 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<TypeNode>(); }
  break;
case 82:
					// line 373 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
    yyVal = new ArrayList<TypeNode>();
    ((List)yyVal).add(((ReferenceTypeNode)yyVals[0+yyTop]));
 }
  break;
case 83:
					// line 377 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
    ((List)yyVals[-2+yyTop]).add(((ReferenceTypeNode)yyVals[0+yyTop]));
 }
  break;
case 84:
					// line 382 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                      yyVal = new MethodSignatureNode(((String)yyVals[-3+yyTop]), ((List)yyVals[-1+yyTop]));
                  }
  break;
case 86:
					// line 388 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<ParameterNode>(); }
  break;
case 87:
					// line 391 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                          List<ParameterNode> list = new ArrayList<ParameterNode>();
                          list.add(((ParameterNode)yyVals[0+yyTop]));
                          yyVal = list;
                      }
  break;
case 88:
					// line 396 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                          ((List)yyVals[-2+yyTop]).add(((ParameterNode)yyVals[0+yyTop]));
                      }
  break;
case 89:
					// line 401 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]));
                 }
  break;
case 90:
					// line 404 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[0+yyTop]), null);
                 }
  break;
case 91:
					// line 407 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]), true);
                 }
  break;
case 92:
					// line 410 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[0+yyTop]), null, true);
                 }
  break;
case 93:
					// line 413 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]), false, true);
                 }
  break;
case 94:
					// line 416 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), null, false, true);
                 }
  break;
case 95:
					// line 419 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]), true, true);
                 }
  break;
case 96:
					// line 422 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), null, true, true);
                 }
  break;
case 97:
					// line 427 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[0+yyTop]);
 }
  break;
case 98:
					// line 429 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     /* We know this is always preceeded by 'type' production.*/
     yyVals[-3+yyTop] = new ArrayTypeNode(((TypeNode)yyVals[-3+yyTop])); 
     yyVal = ((String)yyVals[-2+yyTop]);
 }
  break;
case 99:
					// line 436 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 102:
					// line 442 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 103:
					// line 447 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 104:
					// line 452 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((String)yyVals[-1+yyTop]) + ">"; 
 }
  break;
case 105:
					// line 455 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 106:
					// line 460 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " extends " + ((String)yyVals[-1+yyTop]);
 }
  break;
case 107:
					// line 463 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " extends " + ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ((String)yyVals[0+yyTop]);
 }
  break;
case 109:
					// line 469 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "";
 }
  break;
case 110:
					// line 474 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "extends " + ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ((String)yyVals[0+yyTop]);
 }
  break;
case 112:
					// line 480 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "";
 }
  break;
case 113:
					// line 485 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 115:
					// line 490 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 116:
					// line 493 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " & " + ((String)yyVals[-1+yyTop]);
 }
  break;
case 117:
					// line 498 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " & " + ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
}
  break;
case 118:
					// line 502 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = null; }
  break;
case 119:
					// line 504 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ConstructorSignatureNode)yyVals[-1+yyTop]);
     ((ConstructorSignatureNode)yyVal).setModifiers(((List)yyVals[-2+yyTop]));
     ((ConstructorSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
 }
  break;
case 120:
					// line 508 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ConstructorSignatureNode)yyVals[-1+yyTop]);
     ((ConstructorSignatureNode)yyVal).setModifiers(((List)yyVals[-4+yyTop]));
     ((ConstructorSignatureNode)yyVal).setExtraTypeInfo("<" + ((String)yyVals[-2+yyTop]));
     ((ConstructorSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
 }
  break;
case 121:
					// line 515 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = new ConstructorSignatureNode(((String)yyVals[-3+yyTop]), ((List)yyVals[-1+yyTop]));
 }
  break;
case 122:
					// line 519 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(((TypeNode)yyVals[-2+yyTop]));
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 123:
					// line 525 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-5+yyTop]));
                  ((MethodSignatureNode)yyVal).setExtraTypeInfo("<" + ((String)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(((TypeNode)yyVals[-2+yyTop]));
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 124:
					// line 532 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(PrimitiveTypeNode.VOID);
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 125:
					// line 538 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-5+yyTop]));
                  ((MethodSignatureNode)yyVal).setExtraTypeInfo("<" + ((String)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(PrimitiveTypeNode.VOID);
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 126:
					// line 547 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
               yyVal = new Annotation(((String)yyVals[0+yyTop]), new ArrayList<AnnotationParameter>());
           }
  break;
case 127:
					// line 550 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
               yyVal = new Annotation(((String)yyVals[-3+yyTop]), ((List)yyVals[-1+yyTop]));
           }
  break;
case 128:
					// line 555 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]); }
  break;
case 129:
					// line 558 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new AnnotationParameter(((String)yyVals[-2+yyTop]), ((AnnotationExpression)yyVals[0+yyTop]));
                 }
  break;
case 130:
					// line 561 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new DefaultAnnotationParameter(((AnnotationExpression)yyVals[0+yyTop]));
                 }
  break;
case 131:
					// line 566 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                      yyVal = new ArrayList<AnnotationParameter>();
                      ((List)yyVal).add(((AnnotationParameter)yyVals[0+yyTop]));
                  }
  break;
case 132:
					// line 570 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                      ((List)yyVals[-2+yyTop]).add(((AnnotationParameter)yyVals[0+yyTop]));
                  }
  break;
case 133:
					// line 575 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = ((AnnotationExpression)yyVals[0+yyTop]);
                 }
  break;
case 134:
					// line 578 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = ((AnnotationExpression)yyVals[0+yyTop]);
                 }
  break;
case 135:
					// line 581 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = ((AnnotationExpression)yyVals[0+yyTop]);
                 }
  break;
case 136:
					// line 584 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ArrayAnnotationExpression(((List)yyVals[-1+yyTop]));
                 }
  break;
case 137:
					// line 587 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ArrayAnnotationExpression(new ArrayList<AnnotationExpression>());
                 }
  break;
case 138:
					// line 592 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                            yyVal = new ArrayList<AnnotationExpression>();
                            ((List)yyVal).add(((AnnotationExpression)yyVals[0+yyTop]));
                        }
  break;
case 139:
					// line 596 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                            ((List)yyVals[-2+yyTop]).add(((AnnotationExpression)yyVals[0+yyTop]));
                        }
  break;
case 140:
					// line 601 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<AnnotationParameter>(); }
  break;
case 143:
					// line 606 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
           yyVal = new StringLiteral(((String)yyVals[0+yyTop]));
        }
  break;
case 144:
					// line 609 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
           yyVal = new CharacterLiteral(((String)yyVals[0+yyTop]));
        }
  break;
case 145:
					// line 612 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
           yyVal = new BoolLiteral(true);
        }
  break;
case 146:
					// line 615 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
           yyVal = new BoolLiteral(false);
        }
  break;
case 147:
					// line 618 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
           yyVal = new NumberLiteral(((String)yyVals[0+yyTop]));
        }
  break;
case 148:
					// line 621 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
           yyVal = new NumberLiteral(((String)yyVals[0+yyTop]));
        }
  break;
					// line 1459 "-"
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
					// line 626 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"

}
					// line 1495 "-"
