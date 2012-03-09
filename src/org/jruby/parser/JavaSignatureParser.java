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
					// line 33 "-"
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
  public static final int LPAREN = 283;
  public static final int RPAREN = 284;
  public static final int LBRACK = 285;
  public static final int RBRACK = 286;
  public static final int QUESTION = 287;
  public static final int LT = 288;
  public static final int GT = 289;
  public static final int THROWS = 290;
  public static final int EXTENDS = 291;
  public static final int SUPER = 292;
  public static final int RSHIFT = 293;
  public static final int URSHIFT = 294;
  public static final int yyErrorCode = 256;

  /** number of final state.
    */
  protected static final int yyFinal = 13;

  /** parser tables.
      Order is mandated by <i>jay</i>.
    */
  protected static final short[] yyLhs = {
//yyLhs 135
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
    13,    13,    14,    12,    12,
    }, yyLen = {
//yyLen 135
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
     1,     2,     0,     1,     1,
    }, yyDefRed = {
//yyDefRed 201
     0,    64,    65,    66,    67,    68,    69,    70,    71,    72,
    73,    74,     0,     0,     1,     2,     0,     0,    60,     0,
    61,    75,    76,     0,    10,     5,     6,     7,     8,     9,
    11,    12,     0,     0,     0,     0,     0,     0,    13,    14,
     4,     0,    62,     0,     0,     0,     0,    15,     0,   100,
   101,     0,     0,     0,   119,     0,     0,     0,     0,     0,
     0,     0,     0,   134,   129,   130,    77,     0,   124,   104,
     0,   105,   103,   108,   109,     0,     0,     0,     0,     0,
     0,    20,    82,     0,    78,     0,   122,     0,     0,     0,
     0,     0,    52,    43,     0,    45,    54,    53,     0,     0,
     0,     0,    87,     0,   127,   131,     0,     0,   106,    99,
   102,     0,   120,     0,     0,     0,    79,    29,     0,     0,
     0,    38,     0,     0,     0,     0,   121,     0,    97,     0,
     0,    84,     0,     0,   107,   111,   110,   112,   125,   123,
    83,     0,     0,    30,     0,    31,     0,     0,     0,     0,
    47,    56,    55,    39,    44,    46,     0,     0,     0,    88,
    93,     0,     0,   117,     0,   116,   115,   113,     0,     0,
     0,    32,     0,    40,     0,     0,    95,    98,     0,     0,
    33,     0,    34,     0,     0,     0,    49,    58,    57,    41,
    48,     0,     0,    35,    42,     0,     0,    36,     0,    37,
    50,
    }, yyDgoto = {
//yyDgoto 59
    13,    46,    14,    34,    15,   100,   101,    16,    17,    18,
    54,    79,    61,    62,    63,   102,    88,   103,   147,    38,
    39,   163,    82,    40,    91,    48,   130,    71,   133,   134,
    92,    93,    94,    95,   150,   186,    96,   151,   187,    97,
   152,   188,    98,   153,   189,    49,    50,    51,    52,    72,
    73,   135,   136,    19,    20,    56,    74,    21,    65,
    }, yySindex = {
//yySindex 201
   187,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  -254,     0,     0,     0,  -226,   187,     0,  -243,
     0,     0,     0,  -239,     0,     0,     0,     0,     0,     0,
     0,     0,  -198,  -191,  -193,  -179,  -198,  -165,     0,     0,
     0,   -91,     0,  -135,  -150,  -136,  -193,     0,  -177,     0,
     0,  -121,    67,  -254,     0,   -99,  -100,  -193,    14,    77,
  -100,   -58,  -135,     0,     0,     0,     0,    77,     0,     0,
    98,     0,     0,     0,     0,  -191,  -198,  -193,  -198,   -34,
   -42,     0,     0,  -239,     0,   -23,     0,   -67,  -179,   -37,
   -27,  -232,     0,     0,   -17,     0,     0,     0,  -225,    98,
   -32,   -14,     0,  -208,     0,     0,   -15,  -263,     0,     0,
     0,  -193,     0,  -193,  -254,    14,     0,     0,    98,    98,
    35,     0,    14,  -254,  -100,   -89,     0,    77,     0,     9,
    -6,     0,    98,    10,     0,     0,     0,     0,     0,     0,
     0,   -10,   -27,     0,   -27,     0,   -72,    -1,  -228,    19,
     0,     0,     0,     0,     0,     0,  -232,    25,    -6,     0,
     0,    -5,     0,     0,   -27,     0,     0,     0,  -254,    98,
    98,     0,    46,     0,    35,  -100,     0,     0,  -239,  -228,
     0,  -228,     0,  -137,  -152,    30,     0,     0,     0,     0,
     0,    98,    98,     0,     0,    46,  -152,     0,  -152,     0,
     0,
    }, yyRindex = {
//yyRindex 201
   -29,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   -20,     0,   -61,
     0,     0,     0,   -93,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   280,  -156,     0,  -148,     0,     0,
     0,  -261,     0,    29,     0,     0,   280,     0,    33,     0,
     0,     0,     0,     0,     0,     0,   102,   280,     0,    32,
   116,     0,    34,     0,     0,     0,     0,    32,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   280,     0,   315,
     3,     0,     0,     1,     0,     0,     0,    36,     0,  -176,
    38,   -28,     0,     0,     0,     0,     0,     0,   130,     0,
     0,    37,     0,  -264,     0,     0,     0,    33,     0,     0,
     0,   280,     0,   280,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   144,  -196,     0,     0,     0,  -146,
  -133,     0,     0,   -34,     0,     0,     0,     0,     0,     0,
     0,     4,    39,     0,    61,     0,    36,  -194,    38,     0,
     0,     0,     0,     0,     0,     0,    88,  -122,   -26,     0,
     0,     0,  -170,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   158,     0,     0,     2,    39,
     0,    61,     0,    36,    38,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,    39,     0,    61,     0,
     0,
    }, yyGindex = {
//yyGindex 59
     0,   -24,     0,   291,     0,   278,     0,     0,     0,     0,
   -35,     0,     0,     0,     0,   219,    -3,    -8,    31,   -39,
     0,     0,   233,   -52,    -7,     0,   224,     0,     0,   217,
     0,  -103,   -95,  -101,  -167,   156,     0,     0,     0,   -60,
   -88,  -162,   237,     0,     0,   288,   289,     0,     0,     0,
     0,   220,     0,     0,   350,   -41,   261,   -19,   309,
    };
  protected static final short[] yyTable = {
//yyTable 467
    60,    16,    17,    18,    19,    23,    90,   190,    36,    41,
   108,    68,    57,    35,    81,   132,    16,    90,   107,   154,
    90,   155,    86,    22,    64,   149,   121,    16,   190,   197,
   199,    24,    25,    26,    27,    28,    29,    30,    31,    32,
    43,    44,   112,    64,    78,    41,    83,    37,    44,    35,
    60,    22,   111,    55,   113,   123,    35,   124,   143,   145,
    55,   121,    33,    90,    35,   173,   142,   144,   148,   128,
    90,   154,   165,   155,   129,    81,   138,   185,   139,    45,
   164,   180,   182,    37,    80,    92,    47,    18,    92,    89,
    37,   125,   154,   162,   155,    18,    35,    53,    37,    18,
    18,    89,    18,   180,   182,    18,    55,    83,    21,   143,
   145,    21,    69,    18,    70,   175,   156,   179,   181,    13,
   184,     3,   148,    58,    35,     3,     3,    66,     3,    18,
    37,   143,   145,    18,    18,    94,    18,   121,    94,   196,
   198,   173,   194,   184,    12,    80,    89,    67,    89,    89,
    89,    89,   117,    89,   191,   192,   171,   193,    37,    96,
    75,   178,    96,    89,   128,   128,   128,   128,   128,   128,
   128,   128,   128,   128,   128,   128,   128,   128,   128,   128,
   128,   128,   128,   128,   128,    85,   128,    84,   128,    44,
   128,   128,    59,   157,    55,   128,   126,   126,   126,   126,
   126,   126,   126,   126,   126,   126,   126,   126,   126,   126,
   126,   126,   126,   126,   126,   126,   126,   117,   126,   169,
   170,   171,   117,   126,   118,   119,   104,   126,    63,    63,
    63,    63,    63,    63,    63,    63,    63,    59,    59,    59,
    59,    59,    59,    59,    59,    59,   115,   114,    63,    16,
    16,   120,   126,    16,    16,    91,    16,    59,    91,    63,
    16,    16,   121,   116,   122,    16,    16,   127,    59,   131,
   168,    24,    25,    26,    27,    28,    29,    30,    31,   161,
    81,   177,    16,    17,    18,    19,   160,   172,   132,    16,
    17,    22,    24,    25,    26,    27,    28,    29,    30,    31,
   174,    87,   176,    24,    25,    26,    27,    28,    29,    30,
    31,   195,    22,   132,   118,    80,    86,    26,   133,    51,
    27,    85,   146,    22,    24,    25,    26,    27,    28,    29,
    30,    31,    76,   183,    24,    25,    26,    27,    28,    29,
    30,    31,    28,    77,    22,   106,   159,   140,    99,   158,
   166,   200,   141,   167,    22,    24,    25,    26,    27,    28,
    29,    30,    31,   109,   110,    17,    17,    42,   137,    17,
    17,   105,    17,     0,     0,    22,    17,    17,     0,    22,
    22,    17,    17,    22,    22,     0,    22,     0,     0,     0,
     0,    22,     0,    23,    23,    22,    22,    23,    23,     0,
    23,     0,     0,     0,     0,    23,     0,    19,    19,    23,
    23,    19,    19,     0,    19,     0,     0,     0,     0,    19,
     0,    25,    25,    19,    19,    25,    25,     0,    25,     0,
     0,     0,     0,    25,     0,    24,    24,    25,    25,    24,
    24,     0,    24,     0,     0,     0,     0,    24,     0,     0,
     0,    24,    24,     1,     2,     3,     4,     5,     6,     7,
     8,     9,    10,    11,     0,     0,    12,
    };
  protected static final short[] yyCheck = {
//yyCheck 467
    41,     0,     0,     0,     0,    12,    58,   174,    16,    16,
    70,    46,    36,    16,    53,   278,   277,   281,    70,   122,
   284,   122,    57,   277,    43,   120,   289,   288,   195,   191,
   192,   257,   258,   259,   260,   261,   262,   263,   264,   265,
   283,   280,    77,    62,    52,    52,    53,    16,   280,    52,
    91,   277,    76,   285,    78,   280,    59,    98,   118,   119,
   285,   289,   288,   115,    67,   293,   118,   119,   120,   277,
   122,   174,   132,   174,   282,   114,   111,   172,   113,   277,
   132,   169,   170,    52,    53,   281,   277,   281,   284,    58,
    59,    99,   195,   132,   195,   289,    99,   290,    67,   293,
   294,    70,   278,   191,   192,   281,   285,   114,   278,   169,
   170,   281,   289,   289,   291,   156,   123,   169,   170,   289,
   172,   277,   174,   288,   127,   281,   282,   277,   284,   277,
    99,   191,   192,   281,   282,   281,   284,   289,   284,   191,
   192,   293,   294,   195,   279,   114,   115,   283,   281,   118,
   119,   284,   289,   122,   291,   292,   293,   294,   127,   281,
   281,   168,   284,   132,   257,   258,   259,   260,   261,   262,
   263,   264,   265,   266,   267,   268,   269,   270,   271,   272,
   273,   274,   275,   276,   277,   285,   279,   286,   277,   280,
   283,   284,   283,   282,   285,   288,   257,   258,   259,   260,
   261,   262,   263,   264,   265,   266,   267,   268,   269,   270,
   271,   272,   273,   274,   275,   276,   277,   289,   279,   291,
   292,   293,   289,   284,   291,   292,   284,   288,   257,   258,
   259,   260,   261,   262,   263,   264,   265,   257,   258,   259,
   260,   261,   262,   263,   264,   265,   288,   281,   277,   277,
   278,   288,   284,   281,   282,   281,   284,   277,   284,   288,
   288,   289,   289,   286,   281,   293,   294,   281,   288,   284,
   280,   257,   258,   259,   260,   261,   262,   263,   264,   285,
     0,   286,   281,   281,   281,   281,   277,   288,   278,   288,
   288,   277,   257,   258,   259,   260,   261,   262,   263,   264,
   281,   287,   277,   257,   258,   259,   260,   261,   262,   263,
   264,   281,   277,   284,   281,     0,   284,   281,   284,   281,
   281,   284,   287,   277,   257,   258,   259,   260,   261,   262,
   263,   264,   265,   287,   257,   258,   259,   260,   261,   262,
   263,   264,   281,    52,   277,    67,   127,   114,   271,   125,
   133,   195,   115,   133,   277,   257,   258,   259,   260,   261,
   262,   263,   264,    75,    75,   277,   278,    17,   107,   281,
   282,    62,   284,    -1,    -1,   277,   288,   289,    -1,   277,
   278,   293,   294,   281,   282,    -1,   284,    -1,    -1,    -1,
    -1,   289,    -1,   277,   278,   293,   294,   281,   282,    -1,
   284,    -1,    -1,    -1,    -1,   289,    -1,   277,   278,   293,
   294,   281,   282,    -1,   284,    -1,    -1,    -1,    -1,   289,
    -1,   277,   278,   293,   294,   281,   282,    -1,   284,    -1,
    -1,    -1,    -1,   289,    -1,   277,   278,   293,   294,   281,
   282,    -1,   284,    -1,    -1,    -1,    -1,   289,    -1,    -1,
    -1,   293,   294,   266,   267,   268,   269,   270,   271,   272,
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
    "ELLIPSIS","LPAREN","RPAREN","LBRACK","RBRACK","QUESTION","LT","GT",
    "THROWS","EXTENDS","SUPER","RSHIFT","URSHIFT",
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
//t    "annotation_params : annotation_param",
//t    "annotation_params : annotation_params annotation_param",
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
					// line 119 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((MethodSignatureNode)yyVals[0+yyTop]);
 }
  break;
case 2:
					// line 121 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ConstructorSignatureNode)yyVals[0+yyTop]);
 }
  break;
case 4:
					// line 125 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((TypeNode)yyVals[0+yyTop]); }
  break;
case 5:
					// line 128 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.BYTE;
 }
  break;
case 6:
					// line 131 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.SHORT;
 }
  break;
case 7:
					// line 134 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.INT;
 }
  break;
case 8:
					// line 137 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.LONG;
 }
  break;
case 9:
					// line 140 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.CHAR;
 }
  break;
case 10:
					// line 143 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.BOOLEAN;
 }
  break;
case 11:
					// line 146 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.FLOAT;
 }
  break;
case 12:
					// line 149 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = PrimitiveTypeNode.DOUBLE;
 }
  break;
case 13:
					// line 154 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[0+yyTop]);
 }
  break;
case 14:
					// line 157 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[0+yyTop]);
 }
  break;
case 15:
					// line 162 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((String)yyVals[0+yyTop]); 
 }
  break;
case 16:
					// line 167 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = new ReferenceTypeNode(((String)yyVals[0+yyTop]));
 }
  break;
case 17:
					// line 170 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     String genericTyping = "<" + ((String)yyVals[-2+yyTop]) + "." + ((String)yyVals[0+yyTop]);
     yyVal = ((ReferenceTypeNode)yyVals[-4+yyTop]);
     ((ReferenceTypeNode)yyVals[-4+yyTop]).setGenericsTyping(genericTyping);
 }
  break;
case 19:
					// line 178 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     String genericTyping = "<" + ((String)yyVals[0+yyTop]);
     yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]);
     ((ReferenceTypeNode)yyVals[-2+yyTop]).setGenericsTyping(genericTyping);
 }
  break;
case 22:
					// line 191 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(((TypeNode)yyVals[-1+yyTop]));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 23:
					// line 195 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(new ReferenceTypeNode(((String)yyVals[-1+yyTop])));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 24:
					// line 199 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ReferenceTypeNode)yyVals[-5+yyTop]).setGenericsTyping("<" + ((String)yyVals[-3+yyTop]) + "." + ((String)yyVals[-1+yyTop]));
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(((ReferenceTypeNode)yyVals[-5+yyTop]));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 25:
					// line 204 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     ((ReferenceTypeNode)yyVals[-3+yyTop]).setGenericsTyping("<" + ((String)yyVals[-1+yyTop]));
     ((ArrayTypeNode)yyVals[0+yyTop]).setTypeForArray(((ReferenceTypeNode)yyVals[-3+yyTop]));
     yyVal = ((ArrayTypeNode)yyVals[0+yyTop]);
 }
  break;
case 26:
					// line 211 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "?";
 }
  break;
case 27:
					// line 213 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? extends " + ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
 }
  break;
case 28:
					// line 215 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? super " + ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
 }
  break;
case 29:
					// line 220 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "?>"; 
 }
  break;
case 30:
					// line 222 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? extends " + ((String)yyVals[0+yyTop]);
 }
  break;
case 31:
					// line 224 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? super " + ((String)yyVals[0+yyTop]);
 }
  break;
case 32:
					// line 229 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "?>>"; 
 }
  break;
case 33:
					// line 231 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? extends " + ((String)yyVals[0+yyTop]);
 }
  break;
case 34:
					// line 233 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "? super " + ((String)yyVals[0+yyTop]);
 }
  break;
case 35:
					// line 238 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "?>>";
 }
  break;
case 36:
					// line 240 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? extends " + ((String)yyVals[0+yyTop]);
 }
  break;
case 37:
					// line 242 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "? super " + ((String)yyVals[0+yyTop]);
 }
  break;
case 38:
					// line 247 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ">";
 }
  break;
case 39:
					// line 249 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]).getFullyTypedName() + "<" + ((String)yyVals[0+yyTop]);
 }
  break;
case 40:
					// line 254 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ">>";
 }
  break;
case 41:
					// line 256 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[-2+yyTop]).getFullyTypedName() + "<" + ((String)yyVals[0+yyTop]);
 }
  break;
case 42:
					// line 261 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ">>>";
 }
  break;
case 43:
					// line 266 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[0+yyTop]);
 }
  break;
case 44:
					// line 269 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 46:
					// line 275 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 48:
					// line 281 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 50:
					// line 287 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 51:
					// line 292 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
 }
  break;
case 61:
					// line 310 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    yyVal = new ArrayList<Modifier>();
    ((List)yyVal).add(((Modifier)yyVals[0+yyTop]));
 }
  break;
case 62:
					// line 314 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    ((List)yyVals[-1+yyTop]).add(((Modifier)yyVals[0+yyTop]));
 }
  break;
case 63:
					// line 319 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<Modifier>(); }
  break;
case 64:
					// line 322 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PUBLIC; }
  break;
case 65:
					// line 323 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PROTECTED; }
  break;
case 66:
					// line 324 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.PRIVATE; }
  break;
case 67:
					// line 325 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.STATIC; }
  break;
case 68:
					// line 326 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.ABSTRACT; }
  break;
case 69:
					// line 327 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.FINAL; }
  break;
case 70:
					// line 328 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.NATIVE; }
  break;
case 71:
					// line 329 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.SYNCHRONIZED; }
  break;
case 72:
					// line 330 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.TRANSIENT; }
  break;
case 73:
					// line 331 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.VOLATILE; }
  break;
case 74:
					// line 332 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = Modifier.STRICTFP; }
  break;
case 75:
					// line 333 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = null; }
  break;
case 76:
					// line 336 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[0+yyTop]); }
  break;
case 77:
					// line 337 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((String)yyVals[-2+yyTop]) + "." + ((String)yyVals[0+yyTop]); }
  break;
case 78:
					// line 340 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = new ArrayTypeNode(); 
 }
  break;
case 79:
					// line 342 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = new ArrayTypeNode(((ArrayTypeNode)yyVals[-2+yyTop]));
 }
  break;
case 80:
					// line 347 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = ((List)yyVals[0+yyTop]); }
  break;
case 81:
					// line 348 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<TypeNode>(); }
  break;
case 82:
					// line 351 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    yyVal = new ArrayList<TypeNode>();
    ((List)yyVal).add(((ReferenceTypeNode)yyVals[0+yyTop]));
 }
  break;
case 83:
					// line 355 "src/org/jruby/parser/JavaSignatureParser.y"
  {
    ((List)yyVals[-2+yyTop]).add(((ReferenceTypeNode)yyVals[0+yyTop]));
 }
  break;
case 84:
					// line 360 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                      yyVal = new MethodSignatureNode(((String)yyVals[-3+yyTop]), ((List)yyVals[-1+yyTop]));
                  }
  break;
case 86:
					// line 366 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<ParameterNode>(); }
  break;
case 87:
					// line 369 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                          List<ParameterNode> list = new ArrayList<ParameterNode>();
                          list.add(((ParameterNode)yyVals[0+yyTop]));
                          yyVal = list;
                      }
  break;
case 88:
					// line 374 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                          ((List)yyVals[-2+yyTop]).add(((ParameterNode)yyVals[0+yyTop]));
                      }
  break;
case 89:
					// line 379 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]));
                 }
  break;
case 90:
					// line 382 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[0+yyTop]), null);
                 }
  break;
case 91:
					// line 385 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]), true);
                 }
  break;
case 92:
					// line 388 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[0+yyTop]), null, true);
                 }
  break;
case 93:
					// line 391 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]), false, true);
                 }
  break;
case 94:
					// line 394 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), null, false, true);
                 }
  break;
case 95:
					// line 397 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]), true, true);
                 }
  break;
case 96:
					// line 400 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new ParameterNode(((TypeNode)yyVals[-1+yyTop]), null, true, true);
                 }
  break;
case 97:
					// line 405 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[0+yyTop]);
 }
  break;
case 98:
					// line 407 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     /* We know this is always preceeded by 'type' production.*/
     yyVals[-3+yyTop] = new ArrayTypeNode(((TypeNode)yyVals[-3+yyTop])); 
     yyVal = ((String)yyVals[-2+yyTop]);
 }
  break;
case 99:
					// line 414 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 102:
					// line 420 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-2+yyTop]) + ", " + ((String)yyVals[0+yyTop]);
 }
  break;
case 103:
					// line 425 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 104:
					// line 430 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = ((String)yyVals[-1+yyTop]) + ">"; 
 }
  break;
case 105:
					// line 433 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 106:
					// line 438 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " extends " + ((String)yyVals[-1+yyTop]);
 }
  break;
case 107:
					// line 441 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " extends " + ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ((String)yyVals[0+yyTop]);
 }
  break;
case 109:
					// line 447 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "";
 }
  break;
case 110:
					// line 452 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = "extends " + ((ReferenceTypeNode)yyVals[-1+yyTop]).getFullyTypedName() + ((String)yyVals[0+yyTop]);
 }
  break;
case 112:
					// line 458 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = "";
 }
  break;
case 113:
					// line 463 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 115:
					// line 468 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((String)yyVals[-1+yyTop]) + ((String)yyVals[0+yyTop]);
 }
  break;
case 116:
					// line 471 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " & " + ((String)yyVals[-1+yyTop]);
 }
  break;
case 117:
					// line 476 "src/org/jruby/parser/JavaSignatureParser.y"
  { 
     yyVal = " & " + ((ReferenceTypeNode)yyVals[0+yyTop]).getFullyTypedName();
}
  break;
case 118:
					// line 480 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = null; }
  break;
case 119:
					// line 482 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ConstructorSignatureNode)yyVals[-1+yyTop]);
     ((ConstructorSignatureNode)yyVal).setModifiers(((List)yyVals[-2+yyTop]));
     ((ConstructorSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
 }
  break;
case 120:
					// line 486 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = ((ConstructorSignatureNode)yyVals[-1+yyTop]);
     ((ConstructorSignatureNode)yyVal).setModifiers(((List)yyVals[-4+yyTop]));
     ((ConstructorSignatureNode)yyVal).setExtraTypeInfo("<" + ((String)yyVals[-2+yyTop]));
     ((ConstructorSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
 }
  break;
case 121:
					// line 493 "src/org/jruby/parser/JavaSignatureParser.y"
  {
     yyVal = new ConstructorSignatureNode(((String)yyVals[-3+yyTop]), ((List)yyVals[-1+yyTop]));
 }
  break;
case 122:
					// line 497 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(((TypeNode)yyVals[-2+yyTop]));
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 123:
					// line 503 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-5+yyTop]));
                  ((MethodSignatureNode)yyVal).setExtraTypeInfo("<" + ((String)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(((TypeNode)yyVals[-2+yyTop]));
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 124:
					// line 510 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(PrimitiveTypeNode.VOID);
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 125:
					// line 516 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                  yyVal = ((MethodSignatureNode)yyVals[-1+yyTop]);
                  ((MethodSignatureNode)yyVal).setModifiers(((List)yyVals[-5+yyTop]));
                  ((MethodSignatureNode)yyVal).setExtraTypeInfo("<" + ((String)yyVals[-3+yyTop]));
                  ((MethodSignatureNode)yyVal).setReturnType(PrimitiveTypeNode.VOID);
                  ((MethodSignatureNode)yyVal).setThrows(((List)yyVals[0+yyTop]));
              }
  break;
case 126:
					// line 525 "src/org/jruby/parser/JavaSignatureParser.y"
  {
               yyVal = new Annotation(((String)yyVals[0+yyTop]), null);
           }
  break;
case 127:
					// line 528 "src/org/jruby/parser/JavaSignatureParser.y"
  {
               yyVal = new Annotation(((String)yyVals[-3+yyTop]), ((List)yyVals[-1+yyTop]));
           }
  break;
case 128:
					// line 533 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                    yyVal = ((String)yyVals[-1+yyTop]);
                }
  break;
case 129:
					// line 538 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                     yyVal = new AnnotationParameter(((Annotation)yyVals[0+yyTop]));
                 }
  break;
case 130:
					// line 542 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                      yyVal = new ArrayList<AnnotationParameter>();
                      ((List)yyVal).add(((AnnotationParameter)yyVals[0+yyTop]));
                  }
  break;
case 131:
					// line 546 "src/org/jruby/parser/JavaSignatureParser.y"
  {
                      ((List)yyVals[-1+yyTop]).add(((AnnotationParameter)yyVals[0+yyTop]));
                  }
  break;
case 132:
					// line 551 "src/org/jruby/parser/JavaSignatureParser.y"
  { yyVal = new ArrayList<AnnotationParameter>(); }
  break;
					// line 1308 "-"
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
					// line 557 "src/org/jruby/parser/JavaSignatureParser.y"

}
					// line 1344 "-"
