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
import org.jruby.ast.java_signature.CharacterLiteral;
import org.jruby.ast.java_signature.ConstructorSignatureNode;
import org.jruby.ast.java_signature.DefaultAnnotationParameter;
import org.jruby.ast.java_signature.MethodSignatureNode;
import org.jruby.ast.java_signature.Literal;
import org.jruby.ast.java_signature.Modifier;
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
					// line 39 "-"
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
  public static final int yyErrorCode = 256;

  /** number of final state.
    */
  protected static final int yyFinal = 13;

  /** parser tables.
      Order is mandated by <i>jay</i>.
    */
  protected static final short[] yyLhs = {
//yyLhs 145
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
    14,    12,    12,    61,    61,
    }, yyLen = {
//yyLen 145
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
     0,     1,     1,     1,     1,
    }, yyDefRed = {
//yyDefRed 218
     0,    64,    65,    66,    67,    68,    69,    70,    71,    72,
    73,    74,     0,     0,     1,     2,     0,     0,    60,     0,
    61,    75,    76,     0,    10,     5,     6,     7,     8,     9,
    11,    12,     0,     0,     0,     0,     0,     0,    13,    14,
     4,     0,    62,     0,     0,     0,     0,    15,     0,   100,
   101,     0,     0,     0,   119,     0,     0,     0,     0,     0,
     0,     0,     0,   144,   143,     0,     0,   142,   134,     0,
     0,   133,   131,   130,   135,    77,     0,   124,   104,     0,
   105,   103,   108,   109,     0,     0,     0,     0,     0,     0,
    20,    82,     0,    78,     0,   122,     0,     0,     0,     0,
    52,    43,     0,    45,    54,    53,     0,     0,     0,     0,
    87,     0,   137,   138,     0,   127,     0,     0,     0,     0,
   106,    99,   102,     0,   120,     0,     0,     0,    79,    29,
     0,     0,     0,    38,     0,     0,     0,     0,   121,     0,
    97,     0,     0,     0,   136,   132,   129,    84,     0,     0,
   107,   111,   110,   112,   125,   123,    83,     0,     0,    30,
     0,    31,     0,     0,     0,     0,    47,    56,    55,    39,
    44,    46,     0,     0,     0,    88,    93,     0,   139,     0,
   117,     0,   116,   115,   113,     0,     0,     0,    32,     0,
    40,     0,     0,    95,    98,     0,     0,    33,     0,    34,
     0,     0,     0,    49,    58,    57,    41,    48,     0,     0,
    35,    42,     0,     0,    36,     0,    37,    50,
    }, yyDgoto = {
//yyDgoto 62
    13,    46,    14,    34,    15,   108,   109,    16,    17,    18,
    54,    88,    65,    66,    67,   110,    97,    68,    37,    38,
    39,   180,    91,    40,    69,    48,   142,    80,   149,   150,
   100,   101,   102,   103,   166,   203,   104,   167,   204,   105,
   168,   205,   106,   169,   206,    49,    50,    51,    52,    81,
    82,   151,   152,    19,    20,    56,    83,    71,    72,    73,
   114,    74,
    }, yySindex = {
//yySindex 218
   300,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  -255,     0,     0,     0,   107,   300,     0,  -261,
     0,     0,     0,  -240,     0,     0,     0,     0,     0,     0,
     0,     0,  -222,  -195,  -184,  -166,  -222,  -167,     0,     0,
     0,  -262,     0,   -49,  -143,  -123,  -184,     0,  -169,     0,
     0,  -149,     9,  -255,     0,  -125,  -122,  -184,   128,   196,
  -122,     0,   -78,     0,     0,  -100,  -107,     0,     0,  -236,
   -94,     0,     0,     0,     0,     0,   196,     0,     0,   228,
     0,     0,     0,     0,  -195,  -222,  -184,  -222,   -90,   -86,
     0,     0,  -240,     0,   -92,     0,  -119,  -166,   -74,   -88,
     0,     0,   -60,     0,     0,     0,  -182,   228,   -63,   -55,
     0,  -105,     0,     0,  -219,     0,   -49,   -21,   -62,  -258,
     0,     0,     0,  -184,     0,  -184,  -255,   128,     0,     0,
   228,   228,   162,     0,   128,  -255,  -122,   -89,     0,   196,
     0,   -50,   -59,   -21,     0,     0,     0,     0,   228,   -44,
     0,     0,     0,     0,     0,     0,     0,   -48,   -88,     0,
   -88,     0,   -76,   -58,  -145,   -37,     0,     0,     0,     0,
     0,     0,  -236,   -30,   -59,     0,     0,   -35,     0,     0,
     0,   -88,     0,     0,     0,  -255,   228,   228,     0,   172,
     0,   162,  -122,     0,     0,  -240,  -145,     0,  -145,     0,
  -138,  -197,   -33,     0,     0,     0,     0,     0,   228,   228,
     0,     0,   172,  -197,     0,  -197,     0,     0,
    }, yyRindex = {
//yyRindex 218
   116,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   151,     0,    72,
     0,     0,     0,    37,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   231,  -217,     0,   -87,     0,     0,
     0,  -244,     0,   -26,     0,     0,   231,     0,   -19,     0,
     0,     0,     0,     0,     0,     0,   216,   231,     0,   -12,
   229,  -170,     0,     0,     0,     0,   -11,     0,     0,   -32,
     0,     0,     0,     0,     0,     0,   -12,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   231,     0,   257,     3,
     0,     0,     1,     0,     0,     0,    -4,     0,  -251,     6,
     0,     0,     0,     0,     0,     0,   242,     0,     0,    28,
     0,  -179,     0,     0,     0,     0,     0,     0,     0,   -19,
     0,     0,     0,   231,     0,   231,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   255,  -161,     0,     0,
     0,  -154,  -146,     0,     0,     0,     0,     0,     0,     7,
     0,     0,     0,     0,     0,     0,     0,     4,     8,     0,
    10,     0,    -4,  -238,     6,     0,     0,     0,     0,     0,
     0,     0,   187,  -144,  -132,     0,     0,     0,     0,  -239,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   268,     0,     0,     2,     8,     0,    10,     0,
    -4,     6,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     8,     0,    10,     0,     0,
    }, yyGindex = {
//yyGindex 62
     0,   -24,     0,   226,     0,   205,     0,     0,     0,     0,
    -9,     0,     0,     0,     0,   178,    53,    -3,   -47,   -45,
     0,     0,   164,   -41,    -7,   -22,   182,     0,     0,   171,
     0,  -134,  -113,  -124,  -176,   109,     0,     0,     0,   -56,
  -158,    -6,   198,     0,     0,   243,   266,     0,     0,     0,
     0,   177,     0,     0,   335,   -34,   235,    14,   239,   -46,
     0,     0,
    };
  protected static final short[] yyTable = {
//yyTable 580
   170,    16,    17,    18,    19,    23,    89,    60,    90,    41,
   171,    98,    57,    36,    21,   207,   113,    99,    44,   165,
   148,    70,    22,   120,    59,    43,    55,    18,   197,   199,
    18,    21,    98,    16,   133,    60,   207,    77,   119,    21,
    44,    18,    21,    18,    44,    41,    92,    16,    95,    87,
   197,   199,    55,    13,    18,    45,   111,   170,    18,    18,
     3,   123,   143,   125,     3,     3,   144,   171,     3,    35,
     3,   146,   136,   111,   159,   161,   202,   124,   170,    89,
    98,    90,    47,    98,    98,   163,    99,    98,   171,   158,
   160,   164,   182,    99,    70,   133,    35,   178,   135,   190,
   211,    98,    90,   179,   137,    35,    55,   181,    90,    53,
    76,    76,    35,    15,   154,    35,   155,    76,    76,    92,
    92,    76,    55,    78,    58,    79,    92,    94,   172,    35,
   159,   161,    84,    94,    75,    89,   111,    96,   192,   163,
   163,    89,   163,    96,   163,   196,   198,   133,   201,    91,
   164,   190,   159,   161,   129,    91,   208,   209,   188,   210,
    35,   163,   163,    76,    93,   163,    94,   213,   215,    35,
    35,   201,   140,   129,   116,   130,   131,   141,   195,    24,
    25,    26,    27,    28,    29,    30,    31,   115,   140,   117,
    18,   126,    35,   173,    18,    18,    35,   128,    18,    22,
    18,    12,   214,   216,   133,   127,    62,   112,    24,    25,
    26,    27,    28,    29,    30,    31,   129,   132,   186,   187,
   188,   134,    63,    64,   138,   147,   139,   176,    61,   177,
    12,    81,   185,   189,   148,    62,    24,    25,    26,    27,
    28,    29,    30,    31,   191,    16,    16,   193,   212,    16,
    16,    63,    64,    16,   194,    16,    22,    80,    12,    16,
    16,   140,   118,    62,    16,    16,    24,    25,    26,    27,
    28,    29,    30,    31,    85,    86,   141,    26,    86,    63,
    64,   118,    16,    17,    18,    19,    22,    51,   114,    27,
   156,    28,    16,    17,   128,   128,   128,   128,   128,   128,
   128,   128,   128,   128,   128,   128,   128,   128,   128,   128,
   128,   128,   128,   128,   128,    85,   128,   175,   128,   174,
   183,   217,   128,   128,   128,   157,   184,   121,   128,   126,
   126,   126,   126,   126,   126,   126,   126,   126,   126,   126,
   126,   126,   126,   126,   126,   126,   126,   126,   126,   126,
   122,   126,    42,   126,   153,   145,     0,   126,     0,   126,
     0,     0,     0,   126,    24,    25,    26,    27,    28,    29,
    30,    31,    32,    63,    63,    63,    63,    63,    63,    63,
    63,    63,     0,     0,    22,    24,    25,    26,    27,    28,
    29,    30,    31,    63,     0,     0,     0,     0,    33,     0,
     0,     0,     0,     0,     0,    22,     0,    63,    59,    59,
    59,    59,    59,    59,    59,    59,    59,     0,    96,    24,
    25,    26,    27,    28,    29,    30,    31,     0,    59,    24,
    25,    26,    27,    28,    29,    30,    31,     0,     0,    22,
     0,     0,    59,     0,     0,     0,     0,     0,     0,    22,
     0,     0,   162,    24,    25,    26,    27,    28,    29,    30,
    31,     0,   200,     0,    17,    17,     0,   107,    17,    17,
     0,     0,    17,    22,    17,     0,     0,     0,    17,    17,
     0,     0,     0,    17,    17,    24,    25,    26,    27,    28,
    29,    30,    31,    22,    22,     0,     0,    22,    22,     0,
     0,    22,     0,    22,     0,    22,    23,    23,    22,     0,
    23,    23,    22,    22,    23,     0,    23,     0,     0,    19,
    19,    23,     0,    19,    19,    23,    23,    19,     0,    19,
     0,     0,    25,    25,    19,     0,    25,    25,    19,    19,
    25,     0,    25,     0,     0,    24,    24,    25,     0,    24,
    24,    25,    25,    24,     0,    24,     0,     0,     0,     0,
    24,     0,     0,     0,    24,    24,     1,     2,     3,     4,
     5,     6,     7,     8,     9,    10,    11,     0,     0,    12,
    };
  protected static final short[] yyCheck = {
//yyCheck 580
   134,     0,     0,     0,     0,    12,    53,    41,    53,    16,
   134,    58,    36,    16,     0,   191,    62,    58,   280,   132,
   278,    43,   277,    79,   286,   286,   288,   278,   186,   187,
   281,    17,    79,   277,   292,    69,   212,    46,    79,   278,
   280,   292,   281,   281,   280,    52,    53,   291,    57,    52,
   208,   209,   288,   292,   292,   277,    59,   191,   296,   297,
   277,    85,   281,    87,   281,   282,   285,   191,   285,    16,
   287,   117,   106,    76,   130,   131,   189,    86,   212,   126,
   127,   126,   277,   130,   131,   132,   127,   134,   212,   130,
   131,   132,   148,   134,   116,   292,    43,   143,   280,   296,
   297,   148,   281,   148,   107,    52,   288,   148,   287,   293,
   280,   281,    59,   283,   123,    62,   125,   287,   288,   126,
   281,   291,   288,   292,   291,   294,   287,   281,   135,    76,
   186,   187,   281,   287,   277,   281,   139,   281,   172,   186,
   187,   287,   189,   287,   191,   186,   187,   292,   189,   281,
   191,   296,   208,   209,   292,   287,   294,   295,   296,   297,
   107,   208,   209,   286,   289,   212,   288,   208,   209,   116,
   117,   212,   277,   292,   281,   294,   295,   282,   185,   257,
   258,   259,   260,   261,   262,   263,   264,   287,   277,   283,
   277,   281,   139,   282,   281,   282,   143,   289,   285,   277,
   287,   279,   208,   209,   292,   291,   284,   285,   257,   258,
   259,   260,   261,   262,   263,   264,   292,   291,   294,   295,
   296,   281,   300,   301,   287,   287,   281,   277,   277,   288,
   279,     0,   280,   291,   278,   284,   257,   258,   259,   260,
   261,   262,   263,   264,   281,   277,   278,   277,   281,   281,
   282,   300,   301,   285,   289,   287,   277,     0,   279,   291,
   292,   287,   281,   284,   296,   297,   257,   258,   259,   260,
   261,   262,   263,   264,   265,   287,   287,   281,    52,   300,
   301,    76,   281,   281,   281,   281,   277,   281,   281,   281,
   126,   281,   291,   291,   257,   258,   259,   260,   261,   262,
   263,   264,   265,   266,   267,   268,   269,   270,   271,   272,
   273,   274,   275,   276,   277,   287,   279,   139,   281,   137,
   149,   212,   285,   286,   287,   127,   149,    84,   291,   257,
   258,   259,   260,   261,   262,   263,   264,   265,   266,   267,
   268,   269,   270,   271,   272,   273,   274,   275,   276,   277,
    84,   279,    17,   281,   119,   116,    -1,   285,    -1,   287,
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
    -1,    -1,    -1,   296,   297,   257,   258,   259,   260,   261,
   262,   263,   264,   277,   278,    -1,    -1,   281,   282,    -1,
    -1,   285,    -1,   287,    -1,   277,   277,   278,   292,    -1,
   281,   282,   296,   297,   285,    -1,   287,    -1,    -1,   277,
   278,   292,    -1,   281,   282,   296,   297,   285,    -1,   287,
    -1,    -1,   277,   278,   292,    -1,   281,   282,   296,   297,
   285,    -1,   287,    -1,    -1,   277,   278,   292,    -1,   281,
   282,   296,   297,   285,    -1,   287,    -1,    -1,    -1,    -1,
   292,    -1,    -1,    -1,   296,   297,   266,   267,   268,   269,
   270,   271,   272,   273,   274,   275,   276,    -1,    -1,   279,
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
					// line 135 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((MethodSignatureNode)yyVals[0+yyTop]);
 }
  break;
case 2:
					// line 137 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ConstructorSignatureNode)yyVals[0+yyTop]);
 }
  break;
case 4:
					// line 141 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((TypeNode)yyVals[0+yyTop]); }
  break;
case 5:
					// line 144 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.BYTE;
 }
  break;
case 6:
					// line 147 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.SHORT;
 }
  break;
case 7:
					// line 150 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.INT;
 }
  break;
case 8:
					// line 153 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.LONG;
 }
  break;
case 9:
					// line 156 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.CHAR;
 }
  break;
case 10:
					// line 159 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.BOOLEAN;
 }
  break;
case 11:
					// line 162 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.FLOAT;
 }
  break;
case 12:
					// line 165 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.DOUBLE;
 }
  break;
case 13:
					// line 170 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[0+yyTop]);
 }
  break;
case 14:
					// line 173 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[0+yyTop]);
 }
  break;
case 15:
					// line 178 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((String)yyVals[0+yyTop]); 
 }
  break;
case 16:
					// line 183 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = new ReferenceTypeNode(((String)yyVals[0+yyTop]));
 }
  break;
case 17:
					// line 186 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     String genericTyping = "<" + ((String)yyVals[-2+yyTop]) + "." + ((String)yyVals[0+yyTop]);
     yyVal = ((ReferenceTypeNode)yyVals[-4+yyTop]);
     ((ReferenceTypeNode)yyVals[-4+yyTop]).setGenericsTyping(genericTyping);
 }
  break;
case 19:
					// line 194 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     String genericTyping = "<" + ((String)yyVals[0+yyTop]);
     yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]);
     ((ReferenceTypeNode)yyVals[-2+yyTop]).setGenericsTyping(genericTyping);
 }
  break;
case 22:
					// line 207 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(((TypeNode)yyVals[-1+yyTop]));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 23:
					// line 211 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(new ReferenceTypeNode(((String)yyVals[-1+yyTop])));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 24:
					// line 215 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ReferenceTypeNode)yyVals[-5+yyTop]).setGenericsTyping("<" + ((String)yyVals[-3+yyTop]) + "." + ((String)yyVals[-1+yyTop]));
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(((ReferenceTypeNode)yyVals[-5+yyTop]));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 25:
					// line 220 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ReferenceTypeNode)yyVals[-3+yyTop]).setGenericsTyping("<" + ((String)yyVals[-1+yyTop]));
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(((ReferenceTypeNode)yyVals[-3+yyTop]));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 26:
					// line 227 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "?";
 }
  break;
case 27:
					// line 229 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? extends " + ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
 }
  break;
case 28:
					// line 231 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? super " + ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
 }
  break;
case 29:
					// line 236 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "?>"; 
 }
  break;
case 30:
					// line 238 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? extends " + ((String)yyVals[0+yyTop]);
 }
  break;
case 31:
					// line 240 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? super " + ((String)yyVals[0+yyTop]);
 }
  break;
case 32:
					// line 245 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "?>>"; 
 }
  break;
case 33:
					// line 247 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? extends " + ((String)yyVals[0+yyTop]);
 }
  break;
case 34:
					// line 249 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? super " + ((String)yyVals[0+yyTop]);
 }
  break;
case 35:
					// line 254 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "?>>";
 }
  break;
case 36:
					// line 256 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? extends " + ((String)yyVals[0+yyTop]);
 }
  break;
case 37:
					// line 258 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? super " + ((String)yyVals[0+yyTop]);
 }
  break;
case 38:
					// line 263 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ">";
 }
  break;
case 39:
					// line 265 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]).getFullyTypedName() + "<" + ((String)yyVals[0+yyTop]);
 }
  break;
case 40:
					// line 270 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ">>";
 }
  break;
case 41:
					// line 272 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]).getFullyTypedName() + "<" + ((String)yyVals[0+yyTop]);
 }
  break;
case 42:
					// line 277 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ">>>";
 }
  break;
case 43:
					// line 282 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[0+yyTop]);
 }
  break;
case 44:
					// line 285 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 46:
					// line 291 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 48:
					// line 297 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 50:
					// line 303 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 51:
					// line 308 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
 }
  break;
case 61:
					// line 326 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
    yyVal = new ArrayList<Object>();
    ((List)yyVal).add(yyVals[0+yyTop]);
 }
  break;
case 62:
					// line 330 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
    ((List)yyVals[-1+yyTop]).add(yyVals[0+yyTop]);
 }
  break;
case 63:
					// line 335 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<Object>(); }
  break;
case 64:
					// line 338 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PUBLIC; }
  break;
case 65:
					// line 339 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PROTECTED; }
  break;
case 66:
					// line 340 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PRIVATE; }
  break;
case 67:
					// line 341 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.STATIC; }
  break;
case 68:
					// line 342 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.ABSTRACT; }
  break;
case 69:
					// line 343 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.FINAL; }
  break;
case 70:
					// line 344 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.NATIVE; }
  break;
case 71:
					// line 345 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.SYNCHRONIZED; }
  break;
case 72:
					// line 346 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.TRANSIENT; }
  break;
case 73:
					// line 347 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.VOLATILE; }
  break;
case 74:
					// line 348 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.STRICTFP; }
  break;
case 75:
					// line 349 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((Annotation)yyVals[0+yyTop]); }
  break;
case 76:
					// line 352 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[0+yyTop]); }
  break;
case 77:
					// line 353 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-2+yyTop]) + "." + ((String)yyVals[0+yyTop]); }
  break;
case 78:
					// line 356 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = new ArrayTypeNode(); 
 }
  break;
case 79:
					// line 358 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = new ArrayTypeNode(((ArrayTypeNode)yyVals[-2+yyTop]));
 }
  break;
case 80:
					// line 363 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((List)yyVals[0+yyTop]); }
  break;
case 81:
					// line 364 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<TypeNode>(); }
  break;
case 82:
					// line 367 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
    yyVal = new ArrayList<TypeNode>();
    ((List)yyVal).add(((ReferenceTypeNode)yyVals[0+yyTop]));
 }
  break;
case 83:
					// line 371 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
    ((List)yyVals[-2+yyTop]).add(((ReferenceTypeNode)yyVals[0+yyTop]));
 }
  break;
case 84:
					// line 376 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                      yyVal = new MethodSignatureNode(((String)yyVals[-3+yyTop]), ((List)yyVals[-1+yyTop]));
                  }
  break;
case 86:
					// line 382 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<ParameterNode>(); }
  break;
case 87:
					// line 385 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                          List<ParameterNode> list = new ArrayList<ParameterNode>();
                          list.add(((ParameterNode)yyVals[0+yyTop]));
                          yyVal = list;
                      }
  break;
case 88:
					// line 390 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                          ((List)yyVals[-2+yyTop]).add(((ParameterNode)yyVals[0+yyTop]));
                      }
  break;
case 89:
					// line 395 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]));
                 }
  break;
case 90:
					// line 398 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[0+yyTop]), null);
                 }
  break;
case 91:
					// line 401 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]), true);
                 }
  break;
case 92:
					// line 404 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[0+yyTop]), null, true);
                 }
  break;
case 93:
					// line 407 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]), false, true);
                 }
  break;
case 94:
					// line 410 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), null, false, true);
                 }
  break;
case 95:
					// line 413 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]), true, true);
                 }
  break;
case 96:
					// line 416 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), null, true, true);
                 }
  break;
case 97:
					// line 421 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[0+yyTop]);
 }
  break;
case 98:
					// line 423 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     /* We know this is always preceeded by 'type' production.*/
     yyVals[-3+yyTop] = new ArrayTypeNode(((TypeNode)yyVals[-3+yyTop])); 
     yyVal = ((String)yyVals[-2+yyTop]);
 }
  break;
case 99:
					// line 430 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 102:
					// line 436 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 103:
					// line 441 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 104:
					// line 446 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((String)yyVals[-1+yyTop]) + ">"; 
 }
  break;
case 105:
					// line 449 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 106:
					// line 454 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " extends " + ((String)yyVals[-1+yyTop]);
 }
  break;
case 107:
					// line 457 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " extends " + ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ((String)yyVals[0+yyTop]);
 }
  break;
case 109:
					// line 463 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "";
 }
  break;
case 110:
					// line 468 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "extends " + ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ((String)yyVals[0+yyTop]);
 }
  break;
case 112:
					// line 474 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "";
 }
  break;
case 113:
					// line 479 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 115:
					// line 484 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 116:
					// line 487 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " & " + ((String)yyVals[-1+yyTop]);
 }
  break;
case 117:
					// line 492 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " & " + ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
}
  break;
case 118:
					// line 496 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = null; }
  break;
case 119:
					// line 498 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ConstructorSignatureNode)yyVals[-1+yyTop]);
     ((ConstructorSignatureNode)yyVal).setModifiers(((List)yyVals[-2+yyTop]));
     ((ConstructorSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
 }
  break;
case 120:
					// line 502 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ConstructorSignatureNode)yyVals[-1+yyTop]);
     ((ConstructorSignatureNode)yyVal).setModifiers(((List)yyVals[-4+yyTop]));
     ((ConstructorSignatureNode)yyVal).setExtraTypeInfo("<" + ((String)yyVals[-2+yyTop]));
     ((ConstructorSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
 }
  break;
case 121:
					// line 509 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = new ConstructorSignatureNode(((String)yyVals[-3+yyTop]), ((List)yyVals[-1+yyTop]));
 }
  break;
case 122:
					// line 513 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(((TypeNode)yyVals[-2+yyTop]));
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 123:
					// line 519 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-5+yyTop]));
                  ((MethodSignatureNode)yyVal).setExtraTypeInfo("<" + ((String)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(((TypeNode)yyVals[-2+yyTop]));
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 124:
					// line 526 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(PrimitiveTypeNode.VOID);
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 125:
					// line 532 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-5+yyTop]));
                  ((MethodSignatureNode)yyVal).setExtraTypeInfo("<" + ((String)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(PrimitiveTypeNode.VOID);
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 126:
					// line 541 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
               yyVal = new Annotation(((String)yyVals[0+yyTop]), new ArrayList<AnnotationParameter>());
           }
  break;
case 127:
					// line 544 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
               yyVal = new Annotation(((String)yyVals[-3+yyTop]), ((List)yyVals[-1+yyTop]));
           }
  break;
case 128:
					// line 549 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]); }
  break;
case 129:
					// line 552 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new AnnotationParameter(((String)yyVals[-2+yyTop]), ((AnnotationExpression)yyVals[0+yyTop]));
                 }
  break;
case 130:
					// line 555 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new DefaultAnnotationParameter(((AnnotationExpression)yyVals[0+yyTop]));
                 }
  break;
case 131:
					// line 560 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                      yyVal = new ArrayList<AnnotationParameter>();
                      ((List)yyVal).add(((AnnotationParameter)yyVals[0+yyTop]));
                  }
  break;
case 132:
					// line 564 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                      ((List)yyVals[-2+yyTop]).add(((AnnotationParameter)yyVals[0+yyTop]));
                  }
  break;
case 133:
					// line 569 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = ((AnnotationExpression)yyVals[0+yyTop]);
                 }
  break;
case 134:
					// line 572 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = ((AnnotationExpression)yyVals[0+yyTop]);
                 }
  break;
case 135:
					// line 575 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = ((AnnotationExpression)yyVals[0+yyTop]);
                 }
  break;
case 136:
					// line 578 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ArrayAnnotationExpression(((List)yyVals[-1+yyTop]));
                 }
  break;
case 137:
					// line 581 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ArrayAnnotationExpression(new ArrayList<AnnotationExpression>());
                 }
  break;
case 138:
					// line 586 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                            yyVal = new ArrayList<AnnotationExpression>();
                            ((List)yyVal).add(((AnnotationExpression)yyVals[0+yyTop]));
                        }
  break;
case 139:
					// line 590 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
                            ((List)yyVals[-2+yyTop]).add(((AnnotationExpression)yyVals[0+yyTop]));
                        }
  break;
case 140:
					// line 595 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<AnnotationParameter>(); }
  break;
case 143:
					// line 600 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
           yyVal = new StringLiteral(((String)yyVals[0+yyTop]));
        }
  break;
case 144:
					// line 603 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"
  {
           yyVal = new CharacterLiteral(((String)yyVals[0+yyTop]));
        }
  break;
					// line 1417 "-"
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
					// line 608 "core/src/main/java/org/jruby/parser/JavaSignatureParser.y"

}
					// line 1453 "-"
