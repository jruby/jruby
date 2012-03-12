// created by jay 1.0.2 (c) 2002-2004 ats@cs.rit.edu
// skeleton Java 1.0 (c) 2002 ats@cs.rit.edu

					// line 17 "src/org/jruby/parser/JavaSignatureParser.y"
 
package org.jruby.parser;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jruby.ast.java_signature.Annotation;
import org.jruby.ast.java_signature.AnnotationParameter;
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
					// line 34 "-"
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
//yyLhs 136
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
    58,    13,    13,    14,    12,    12,
    }, yyLen = {
//yyLen 136
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
     5,     4,     4,     6,     4,     6,     1,     4,     2,     1,
     3,     1,     3,     0,     1,     1,
    }, yyDefRed = {
//yyDefRed 205
     0,    64,    65,    66,    67,    68,    69,    70,    71,    72,
    73,    74,     0,     0,     1,     2,     0,     0,    60,     0,
    61,    75,    76,     0,    10,     5,     6,     7,     8,     9,
    11,    12,     0,     0,     0,     0,     0,     0,    13,    14,
     4,     0,    62,     0,     0,     0,     0,    15,     0,   100,
   101,     0,     0,     0,   119,     0,     0,     0,     0,     0,
     0,     0,     0,   135,     0,   129,   131,    77,     0,   124,
   104,     0,   105,   103,   108,   109,     0,     0,     0,     0,
     0,     0,    20,    82,     0,    78,     0,   122,     0,     0,
     0,     0,     0,    52,    43,     0,    45,    54,    53,     0,
     0,     0,     0,    87,     0,   127,     0,     0,     0,     0,
   106,    99,   102,     0,   120,     0,     0,     0,    79,    29,
     0,     0,     0,    38,     0,     0,     0,     0,   121,     0,
    97,     0,     0,   132,   130,    84,     0,     0,   107,   111,
   110,   112,   125,   123,    83,     0,     0,    30,     0,    31,
     0,     0,     0,     0,    47,    56,    55,    39,    44,    46,
     0,     0,     0,    88,    93,     0,     0,   117,     0,   116,
   115,   113,     0,     0,     0,    32,     0,    40,     0,     0,
    95,    98,     0,     0,    33,     0,    34,     0,     0,     0,
    49,    58,    57,    41,    48,     0,     0,    35,    42,     0,
     0,    36,     0,    37,    50,
    }, yyDgoto = {
//yyDgoto 59
    13,    46,    14,    34,    15,   101,   102,    16,    17,    18,
    54,    80,    61,    62,    63,   103,    89,   104,   151,    38,
    39,   167,    83,    40,    92,    48,   132,    72,   137,   138,
    93,    94,    95,    96,   154,   190,    97,   155,   191,    98,
   156,   192,    99,   157,   193,    49,    50,    51,    52,    73,
    74,   139,   140,    19,    20,    56,    75,    21,    66,
    }, yySindex = {
//yySindex 205
   226,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  -266,     0,     0,     0,  -225,   226,     0,  -242,
     0,     0,     0,  -227,     0,     0,     0,     0,     0,     0,
     0,     0,  -202,  -188,  -203,  -176,  -202,  -172,     0,     0,
     0,  -262,     0,  -159,  -149,  -144,  -203,     0,  -232,     0,
     0,  -154,    87,  -266,     0,  -140,  -127,  -203,    37,   113,
  -127,   -85,  -122,     0,   -20,     0,     0,     0,   113,     0,
     0,   134,     0,     0,     0,     0,  -188,  -202,  -203,  -202,
  -121,   -88,     0,     0,  -227,     0,   -60,     0,   -30,  -176,
   -58,   -42,  -239,     0,     0,   -47,     0,     0,     0,  -229,
   134,   -36,   -14,     0,  -136,     0,  -159,  -159,   -19,  -250,
     0,     0,     0,  -203,     0,  -203,  -266,    37,     0,     0,
   134,   134,    45,     0,    37,  -266,  -127,  -129,     0,   113,
     0,    -8,   -18,     0,     0,     0,   134,    -2,     0,     0,
     0,     0,     0,     0,     0,    -6,   -42,     0,   -42,     0,
   -57,   -13,  -100,   -10,     0,     0,     0,     0,     0,     0,
  -239,    10,   -18,     0,     0,   -12,     0,     0,   -42,     0,
     0,     0,  -266,   134,   134,     0,    79,     0,    45,  -127,
     0,     0,  -227,  -100,     0,  -100,     0,  -128,   -65,    -1,
     0,     0,     0,     0,     0,   134,   134,     0,     0,    79,
   -65,     0,   -65,     0,     0,
    }, yyRindex = {
//yyRindex 205
   -16,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,    -5,     0,   -51,
     0,     0,     0,   -86,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   273,  -125,     0,   -83,     0,     0,
     0,  -230,     0,    23,     0,     0,   273,     0,     7,     0,
     0,     0,     0,     0,     0,     0,   146,   273,     0,    24,
   163,     0,    26,     0,  -181,     0,     0,     0,    24,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   273,     0,
   279,     3,     0,     0,     1,     0,     0,     0,     8,     0,
  -193,     9,   122,     0,     0,     0,     0,     0,     0,   170,
     0,     0,    28,     0,  -174,     0,     0,     0,     0,     7,
     0,     0,     0,   273,     0,   273,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   187,  -173,     0,     0,
     0,  -158,  -151,     0,     0,     0,     0,    31,     0,     0,
     0,     0,     0,     0,     0,     4,    35,     0,    36,     0,
     8,  -267,     9,     0,     0,     0,     0,     0,     0,     0,
   139,  -150,  -142,     0,     0,     0,  -177,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   194,
     0,     0,     2,    35,     0,    36,     0,     8,     9,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
    35,     0,    36,     0,     0,
    }, yyGindex = {
//yyGindex 59
     0,   -23,     0,   229,     0,   223,     0,     0,     0,     0,
    25,     0,     0,     0,     0,   189,    -4,    -9,    34,   -53,
     0,     0,   203,   -52,    -7,     0,   193,     0,     0,   184,
     0,  -102,   -95,  -101,  -168,   124,     0,     0,     0,   -63,
   -79,  -175,   207,     0,     0,   249,   250,     0,     0,     0,
     0,   191,     0,   -26,   312,   -25,   221,   -28,   225,
    };
  protected static final short[] yyTable = {
//yyTable 506
    82,    16,    17,    18,    19,    23,    91,    36,   110,    41,
   194,    22,    35,    57,    18,    65,    60,    64,    44,   109,
   201,   203,   158,   159,    59,    18,    55,   153,   136,    18,
    18,   194,    24,    25,    26,    27,    28,    29,    30,    31,
    32,    44,   123,    79,    43,    41,    84,    16,    35,    55,
    37,   125,    22,    44,   113,    35,   115,   147,   149,    55,
    70,    16,    71,    82,    35,    91,    33,    60,   146,   148,
   152,    69,    91,   169,   126,    45,   158,   159,    65,   134,
    64,   189,    87,   166,   168,    18,    37,    81,    18,    47,
    53,   127,    90,    37,   184,   186,    35,   158,   159,    18,
   126,    21,    37,   114,    21,    90,   126,    90,    92,    84,
   147,   149,    55,    90,    92,    13,   184,   186,   160,    58,
    12,   183,   185,    94,   188,    35,   152,    76,    67,    94,
    89,    96,   147,   149,    37,   179,    89,    96,   142,    91,
   143,   130,    68,   200,   202,    91,   131,   188,   130,    85,
    81,    90,     3,   161,    90,    90,     3,     3,    90,   106,
   116,    86,     3,    37,   119,   182,   195,   196,   175,   197,
    90,   128,   128,   128,   128,   128,   128,   128,   128,   128,
   128,   128,   128,   128,   128,   128,   128,   128,   128,   128,
   128,   128,   123,   128,    18,   128,   177,   128,    18,    18,
   128,   128,   105,   117,    18,   128,   126,   126,   126,   126,
   126,   126,   126,   126,   126,   126,   126,   126,   126,   126,
   126,   126,   126,   126,   126,   126,   126,   123,   126,   118,
   126,   177,   198,   122,   124,   119,   126,   173,   174,   175,
   126,    63,    63,    63,    63,    63,    63,    63,    63,    63,
   123,   128,    59,    59,    59,    59,    59,    59,    59,    59,
    59,    63,   119,   107,   120,   121,    43,   129,   135,   164,
   165,   178,    59,    81,   172,    63,   136,   181,   176,    80,
   199,    78,    16,    17,    18,    19,    59,   180,   118,    26,
    51,   108,    16,    17,    24,    25,    26,    27,    28,    29,
    30,    31,    24,    25,    26,    27,    28,    29,    30,    31,
   133,    86,   114,   134,    22,    85,    27,    28,   163,   144,
   162,   170,    22,   204,   145,   111,   112,    88,   171,    42,
   141,   133,     0,     0,     0,   150,    24,    25,    26,    27,
    28,    29,    30,    31,    24,    25,    26,    27,    28,    29,
    30,    31,    77,     0,     0,     0,    22,     0,     0,     0,
     0,     0,     0,     0,    22,     0,     0,     0,     0,   187,
    24,    25,    26,    27,    28,    29,    30,    31,     0,     0,
     0,     0,     0,     0,   100,     0,     0,     0,     0,     0,
    22,    24,    25,    26,    27,    28,    29,    30,    31,    16,
    16,     0,     0,    16,    16,     0,     0,     0,     0,    16,
     0,    22,     0,    16,    16,     0,    17,    17,    16,    16,
    17,    17,     0,    22,    22,     0,    17,    22,    22,     0,
    17,    17,     0,    22,     0,    17,    17,     0,    22,     0,
    23,    23,    22,    22,    23,    23,     0,    19,    19,     0,
    23,    19,    19,     0,     0,    23,     0,    19,     0,    23,
    23,     0,    19,     0,    25,    25,    19,    19,    25,    25,
     0,    24,    24,     0,    25,    24,    24,     0,     0,    25,
     0,    24,     0,    25,    25,     0,    24,     0,     0,     0,
    24,    24,     1,     2,     3,     4,     5,     6,     7,     8,
     9,    10,    11,     0,     0,    12,
    };
  protected static final short[] yyCheck = {
//yyCheck 506
    53,     0,     0,     0,     0,    12,    58,    16,    71,    16,
   178,   277,    16,    36,   281,    43,    41,    43,   280,    71,
   195,   196,   124,   124,   286,   292,   288,   122,   278,   296,
   297,   199,   257,   258,   259,   260,   261,   262,   263,   264,
   265,   280,   292,    52,   286,    52,    53,   277,    52,   288,
    16,   280,   277,   280,    77,    59,    79,   120,   121,   288,
   292,   291,   294,   116,    68,   117,   291,    92,   120,   121,
   122,    46,   124,   136,    99,   277,   178,   178,   106,   107,
   106,   176,    57,   136,   136,   278,    52,    53,   281,   277,
   293,   100,    58,    59,   173,   174,   100,   199,   199,   292,
   281,   278,    68,    78,   281,    71,   287,   281,   281,   116,
   173,   174,   288,   287,   287,   292,   195,   196,   125,   291,
   279,   173,   174,   281,   176,   129,   178,   281,   277,   287,
   281,   281,   195,   196,   100,   160,   287,   287,   113,   281,
   115,   277,   286,   195,   196,   287,   282,   199,   277,   289,
   116,   117,   277,   282,   120,   121,   281,   282,   124,   281,
   281,   288,   287,   129,   292,   172,   294,   295,   296,   297,
   136,   257,   258,   259,   260,   261,   262,   263,   264,   265,
   266,   267,   268,   269,   270,   271,   272,   273,   274,   275,
   276,   277,   292,   279,   277,   281,   296,   283,   281,   282,
   286,   287,   287,   291,   287,   291,   257,   258,   259,   260,
   261,   262,   263,   264,   265,   266,   267,   268,   269,   270,
   271,   272,   273,   274,   275,   276,   277,   292,   279,   289,
   281,   296,   297,   291,   281,   292,   287,   294,   295,   296,
   291,   257,   258,   259,   260,   261,   262,   263,   264,   265,
   292,   287,   257,   258,   259,   260,   261,   262,   263,   264,
   265,   277,   292,   283,   294,   295,   286,   281,   287,   277,
   288,   281,   277,     0,   280,   291,   278,   289,   291,     0,
   281,    52,   281,   281,   281,   281,   291,   277,   281,   281,
   281,    68,   291,   291,   257,   258,   259,   260,   261,   262,
   263,   264,   257,   258,   259,   260,   261,   262,   263,   264,
   287,   287,   281,   287,   277,   287,   281,   281,   129,   116,
   127,   137,   277,   199,   117,    76,    76,   290,   137,    17,
   109,   106,    -1,    -1,    -1,   290,   257,   258,   259,   260,
   261,   262,   263,   264,   257,   258,   259,   260,   261,   262,
   263,   264,   265,    -1,    -1,    -1,   277,    -1,    -1,    -1,
    -1,    -1,    -1,    -1,   277,    -1,    -1,    -1,    -1,   290,
   257,   258,   259,   260,   261,   262,   263,   264,    -1,    -1,
    -1,    -1,    -1,    -1,   271,    -1,    -1,    -1,    -1,    -1,
   277,   257,   258,   259,   260,   261,   262,   263,   264,   277,
   278,    -1,    -1,   281,   282,    -1,    -1,    -1,    -1,   287,
    -1,   277,    -1,   291,   292,    -1,   277,   278,   296,   297,
   281,   282,    -1,   277,   278,    -1,   287,   281,   282,    -1,
   291,   292,    -1,   287,    -1,   296,   297,    -1,   292,    -1,
   277,   278,   296,   297,   281,   282,    -1,   277,   278,    -1,
   287,   281,   282,    -1,    -1,   292,    -1,   287,    -1,   296,
   297,    -1,   292,    -1,   277,   278,   296,   297,   281,   282,
    -1,   277,   278,    -1,   287,   281,   282,    -1,    -1,   292,
    -1,   287,    -1,   296,   297,    -1,   292,    -1,    -1,    -1,
   296,   297,   266,   267,   268,   269,   270,   271,   272,   273,
   274,   275,   276,    -1,    -1,   279,
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
//t    "annotation_param : annotation",
//t    "annotation_param : annotation_name EQUAL annotation",
//t    "annotation_params : annotation_param",
//t    "annotation_params : annotation_params COMMA annotation_param",
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
					// line 123 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((MethodSignatureNode)yyVals[0+yyTop]);
 }
  break;
case 2:
					// line 125 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ConstructorSignatureNode)yyVals[0+yyTop]);
 }
  break;
case 4:
					// line 129 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((TypeNode)yyVals[0+yyTop]); }
  break;
case 5:
					// line 132 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.BYTE;
 }
  break;
case 6:
					// line 135 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.SHORT;
 }
  break;
case 7:
					// line 138 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.INT;
 }
  break;
case 8:
					// line 141 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.LONG;
 }
  break;
case 9:
					// line 144 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.CHAR;
 }
  break;
case 10:
					// line 147 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.BOOLEAN;
 }
  break;
case 11:
					// line 150 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.FLOAT;
 }
  break;
case 12:
					// line 153 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.DOUBLE;
 }
  break;
case 13:
					// line 158 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[0+yyTop]);
 }
  break;
case 14:
					// line 161 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[0+yyTop]);
 }
  break;
case 15:
					// line 166 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((String)yyVals[0+yyTop]); 
 }
  break;
case 16:
					// line 171 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = new ReferenceTypeNode(((String)yyVals[0+yyTop]));
 }
  break;
case 17:
					// line 174 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     String genericTyping = "<" + ((String)yyVals[-2+yyTop]) + "." + ((String)yyVals[0+yyTop]);
     yyVal = ((ReferenceTypeNode)yyVals[-4+yyTop]);
     ((ReferenceTypeNode)yyVals[-4+yyTop]).setGenericsTyping(genericTyping);
 }
  break;
case 19:
					// line 182 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     String genericTyping = "<" + ((String)yyVals[0+yyTop]);
     yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]);
     ((ReferenceTypeNode)yyVals[-2+yyTop]).setGenericsTyping(genericTyping);
 }
  break;
case 22:
					// line 195 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(((TypeNode)yyVals[-1+yyTop]));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 23:
					// line 199 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(new ReferenceTypeNode(((String)yyVals[-1+yyTop])));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 24:
					// line 203 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ReferenceTypeNode)yyVals[-5+yyTop]).setGenericsTyping("<" + ((String)yyVals[-3+yyTop]) + "." + ((String)yyVals[-1+yyTop]));
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(((ReferenceTypeNode)yyVals[-5+yyTop]));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 25:
					// line 208 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ReferenceTypeNode)yyVals[-3+yyTop]).setGenericsTyping("<" + ((String)yyVals[-1+yyTop]));
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(((ReferenceTypeNode)yyVals[-3+yyTop]));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 26:
					// line 215 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "?";
 }
  break;
case 27:
					// line 217 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? extends " + ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
 }
  break;
case 28:
					// line 219 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? super " + ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
 }
  break;
case 29:
					// line 224 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "?>"; 
 }
  break;
case 30:
					// line 226 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? extends " + ((String)yyVals[0+yyTop]);
 }
  break;
case 31:
					// line 228 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? super " + ((String)yyVals[0+yyTop]);
 }
  break;
case 32:
					// line 233 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "?>>"; 
 }
  break;
case 33:
					// line 235 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? extends " + ((String)yyVals[0+yyTop]);
 }
  break;
case 34:
					// line 237 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? super " + ((String)yyVals[0+yyTop]);
 }
  break;
case 35:
					// line 242 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "?>>";
 }
  break;
case 36:
					// line 244 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? extends " + ((String)yyVals[0+yyTop]);
 }
  break;
case 37:
					// line 246 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? super " + ((String)yyVals[0+yyTop]);
 }
  break;
case 38:
					// line 251 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ">";
 }
  break;
case 39:
					// line 253 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]).getFullyTypedName() + "<" + ((String)yyVals[0+yyTop]);
 }
  break;
case 40:
					// line 258 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ">>";
 }
  break;
case 41:
					// line 260 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]).getFullyTypedName() + "<" + ((String)yyVals[0+yyTop]);
 }
  break;
case 42:
					// line 265 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ">>>";
 }
  break;
case 43:
					// line 270 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[0+yyTop]);
 }
  break;
case 44:
					// line 273 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 46:
					// line 279 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 48:
					// line 285 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 50:
					// line 291 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 51:
					// line 296 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
 }
  break;
case 61:
					// line 314 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    yyVal = new ArrayList<Object>();
    ((List)yyVal).add(yyVals[0+yyTop]);
 }
  break;
case 62:
					// line 318 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    ((List)yyVals[-1+yyTop]).add(yyVals[0+yyTop]);
 }
  break;
case 63:
					// line 323 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<Object>(); }
  break;
case 64:
					// line 326 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PUBLIC; }
  break;
case 65:
					// line 327 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PROTECTED; }
  break;
case 66:
					// line 328 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PRIVATE; }
  break;
case 67:
					// line 329 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.STATIC; }
  break;
case 68:
					// line 330 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.ABSTRACT; }
  break;
case 69:
					// line 331 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.FINAL; }
  break;
case 70:
					// line 332 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.NATIVE; }
  break;
case 71:
					// line 333 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.SYNCHRONIZED; }
  break;
case 72:
					// line 334 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.TRANSIENT; }
  break;
case 73:
					// line 335 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.VOLATILE; }
  break;
case 74:
					// line 336 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.STRICTFP; }
  break;
case 75:
					// line 337 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((Annotation)yyVals[0+yyTop]); }
  break;
case 76:
					// line 340 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[0+yyTop]); }
  break;
case 77:
					// line 341 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-2+yyTop]) + "." + ((String)yyVals[0+yyTop]); }
  break;
case 78:
					// line 344 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = new ArrayTypeNode(); 
 }
  break;
case 79:
					// line 346 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = new ArrayTypeNode(((ArrayTypeNode)yyVals[-2+yyTop]));
 }
  break;
case 80:
					// line 351 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((List)yyVals[0+yyTop]); }
  break;
case 81:
					// line 352 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<TypeNode>(); }
  break;
case 82:
					// line 355 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    yyVal = new ArrayList<TypeNode>();
    ((List)yyVal).add(((ReferenceTypeNode)yyVals[0+yyTop]));
 }
  break;
case 83:
					// line 359 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    ((List)yyVals[-2+yyTop]).add(((ReferenceTypeNode)yyVals[0+yyTop]));
 }
  break;
case 84:
					// line 364 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                      yyVal = new MethodSignatureNode(((String)yyVals[-3+yyTop]), ((List)yyVals[-1+yyTop]));
                  }
  break;
case 86:
					// line 370 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<ParameterNode>(); }
  break;
case 87:
					// line 373 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                          List<ParameterNode> list = new ArrayList<ParameterNode>();
                          list.add(((ParameterNode)yyVals[0+yyTop]));
                          yyVal = list;
                      }
  break;
case 88:
					// line 378 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                          ((List)yyVals[-2+yyTop]).add(((ParameterNode)yyVals[0+yyTop]));
                      }
  break;
case 89:
					// line 383 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]));
                 }
  break;
case 90:
					// line 386 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[0+yyTop]), null);
                 }
  break;
case 91:
					// line 389 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]), true);
                 }
  break;
case 92:
					// line 392 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[0+yyTop]), null, true);
                 }
  break;
case 93:
					// line 395 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]), false, true);
                 }
  break;
case 94:
					// line 398 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), null, false, true);
                 }
  break;
case 95:
					// line 401 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]), true, true);
                 }
  break;
case 96:
					// line 404 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), null, true, true);
                 }
  break;
case 97:
					// line 409 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[0+yyTop]);
 }
  break;
case 98:
					// line 411 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     /* We know this is always preceeded by 'type' production.*/
     yyVals[-3+yyTop] = new ArrayTypeNode(((TypeNode)yyVals[-3+yyTop])); 
     yyVal = ((String)yyVals[-2+yyTop]);
 }
  break;
case 99:
					// line 418 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 102:
					// line 424 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 103:
					// line 429 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 104:
					// line 434 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((String)yyVals[-1+yyTop]) + ">"; 
 }
  break;
case 105:
					// line 437 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 106:
					// line 442 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " extends " + ((String)yyVals[-1+yyTop]);
 }
  break;
case 107:
					// line 445 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " extends " + ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ((String)yyVals[0+yyTop]);
 }
  break;
case 109:
					// line 451 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "";
 }
  break;
case 110:
					// line 456 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "extends " + ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ((String)yyVals[0+yyTop]);
 }
  break;
case 112:
					// line 462 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "";
 }
  break;
case 113:
					// line 467 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 115:
					// line 472 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 116:
					// line 475 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " & " + ((String)yyVals[-1+yyTop]);
 }
  break;
case 117:
					// line 480 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " & " + ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
}
  break;
case 118:
					// line 484 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = null; }
  break;
case 119:
					// line 486 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ConstructorSignatureNode)yyVals[-1+yyTop]);
     ((ConstructorSignatureNode)yyVal).setModifiers(((List)yyVals[-2+yyTop]));
     ((ConstructorSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
 }
  break;
case 120:
					// line 490 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ConstructorSignatureNode)yyVals[-1+yyTop]);
     ((ConstructorSignatureNode)yyVal).setModifiers(((List)yyVals[-4+yyTop]));
     ((ConstructorSignatureNode)yyVal).setExtraTypeInfo("<" + ((String)yyVals[-2+yyTop]));
     ((ConstructorSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
 }
  break;
case 121:
					// line 497 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = new ConstructorSignatureNode(((String)yyVals[-3+yyTop]), ((List)yyVals[-1+yyTop]));
 }
  break;
case 122:
					// line 501 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(((TypeNode)yyVals[-2+yyTop]));
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 123:
					// line 507 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-5+yyTop]));
                  ((MethodSignatureNode)yyVal).setExtraTypeInfo("<" + ((String)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(((TypeNode)yyVals[-2+yyTop]));
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 124:
					// line 514 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(PrimitiveTypeNode.VOID);
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 125:
					// line 520 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-5+yyTop]));
                  ((MethodSignatureNode)yyVal).setExtraTypeInfo("<" + ((String)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(PrimitiveTypeNode.VOID);
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 126:
					// line 529 "src/org/jruby/parser/JavaSignatureParser.y"
  {
               yyVal = new Annotation(((String)yyVals[0+yyTop]), new ArrayList<AnnotationParameter>());
           }
  break;
case 127:
					// line 532 "src/org/jruby/parser/JavaSignatureParser.y"
  {
               yyVal = new Annotation(((String)yyVals[-3+yyTop]), ((List)yyVals[-1+yyTop]));
           }
  break;
case 128:
					// line 537 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]); }
  break;
case 129:
					// line 540 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new DefaultAnnotationParameter(((Annotation)yyVals[0+yyTop]));
                 }
  break;
case 130:
					// line 543 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new AnnotationParameter(((String)yyVals[-2+yyTop]), ((Annotation)yyVals[0+yyTop]));
                 }
  break;
case 131:
					// line 548 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                      yyVal = new ArrayList<AnnotationParameter>();
                      ((List)yyVal).add(((AnnotationParameter)yyVals[0+yyTop]));
                  }
  break;
case 132:
					// line 552 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                      ((List)yyVals[-2+yyTop]).add(((AnnotationParameter)yyVals[0+yyTop]));
                  }
  break;
case 133:
					// line 557 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<AnnotationParameter>(); }
  break;
					// line 1326 "-"
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
					// line 563 "src/org/jruby/parser/JavaSignatureParser.y"

}
					// line 1362 "-"
