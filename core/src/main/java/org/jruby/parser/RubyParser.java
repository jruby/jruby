// created by jay 1.0.2 (c) 2002-2004 ats@cs.rit.edu
// skeleton Java 1.0 (c) 2002 ats@cs.rit.edu

					// line 2 "parse.y"
// We ERB for ripper grammar and we need an alternative substitution value.
/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
  * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2008-2017 Thomas E Enebo <enebo@acm.org>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.parser;

import java.io.IOException;
import java.util.Set;

import org.jruby.Ruby;
import org.jruby.RubySymbol;
import org.jruby.ast.*;
import org.jruby.common.IRubyWarnings;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.lexer.LexerSource;
import org.jruby.lexer.LexingCommon;
import org.jruby.runtime.DynamicScope;
import org.jruby.lexer.yacc.StrTerm;

import org.jruby.util.ByteList;
import org.jruby.util.CommonByteLists;
import org.jruby.util.KeyValuePair;
import org.jruby.util.StringSupport;
import org.jruby.lexer.yacc.LexContext;
import org.jruby.lexer.yacc.LexContext.InRescue.*;
import org.jruby.lexer.yacc.RubyLexer;
import org.jruby.lexer.yacc.StackState;
import org.jruby.parser.NodeExits;
import org.jruby.parser.ProductionState;
import org.jruby.parser.ParserState;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.lexer.yacc.RubyLexer.*;
import static org.jruby.lexer.LexingCommon.AMPERSAND;
import static org.jruby.lexer.LexingCommon.AMPERSAND_AMPERSAND;
import static org.jruby.lexer.LexingCommon.AMPERSAND_DOT;
import static org.jruby.lexer.LexingCommon.AND_KEYWORD;
import static org.jruby.lexer.LexingCommon.BACKTICK;
import static org.jruby.lexer.LexingCommon.BANG;
import static org.jruby.lexer.LexingCommon.CARET;
import static org.jruby.lexer.LexingCommon.COLON_COLON;
import static org.jruby.lexer.LexingCommon.DOLLAR_BANG;
import static org.jruby.lexer.LexingCommon.DOT;
import static org.jruby.lexer.LexingCommon.GT;
import static org.jruby.lexer.LexingCommon.GT_EQ;
import static org.jruby.lexer.LexingCommon.LBRACKET_RBRACKET;
import static org.jruby.lexer.LexingCommon.LCURLY;
import static org.jruby.lexer.LexingCommon.LT;
import static org.jruby.lexer.LexingCommon.LT_EQ;
import static org.jruby.lexer.LexingCommon.LT_LT;
import static org.jruby.lexer.LexingCommon.MINUS;
import static org.jruby.lexer.LexingCommon.MINUS_AT;
import static org.jruby.lexer.LexingCommon.PERCENT;
import static org.jruby.lexer.LexingCommon.OR;
import static org.jruby.lexer.LexingCommon.OR_KEYWORD; 
import static org.jruby.lexer.LexingCommon.OR_OR;
import static org.jruby.lexer.LexingCommon.PLUS;
import static org.jruby.lexer.LexingCommon.RBRACKET;
import static org.jruby.lexer.LexingCommon.RCURLY;
import static org.jruby.lexer.LexingCommon.RPAREN;
import static org.jruby.lexer.LexingCommon.SLASH;
import static org.jruby.lexer.LexingCommon.STAR;
import static org.jruby.lexer.LexingCommon.STAR_STAR;
import static org.jruby.lexer.LexingCommon.TILDE;
import static org.jruby.lexer.LexingCommon.EXPR_BEG;
import static org.jruby.lexer.LexingCommon.EXPR_FITEM;
import static org.jruby.lexer.LexingCommon.EXPR_FNAME;
import static org.jruby.lexer.LexingCommon.EXPR_ENDFN;
import static org.jruby.lexer.LexingCommon.EXPR_ENDARG;
import static org.jruby.lexer.LexingCommon.EXPR_END;
import static org.jruby.lexer.LexingCommon.EXPR_LABEL;
import static org.jruby.util.CommonByteLists.ANON_BLOCK;
import static org.jruby.util.CommonByteLists.FWD_ALL;
import static org.jruby.util.CommonByteLists.FWD_BLOCK;
import static org.jruby.util.CommonByteLists.FWD_REST;
import static org.jruby.util.CommonByteLists.FWD_KWREST;
 
 public class RubyParser extends RubyParserBase {
    public RubyParser(Ruby runtime, LexerSource source, DynamicScope scope, org.jruby.parser.ParserType type) {
        super(runtime, source, scope, type);
    }
					// line 112 "-"
  // %token constants
  public static final int keyword_class = 257;
  public static final int keyword_module = 258;
  public static final int keyword_def = 259;
  public static final int keyword_undef = 260;
  public static final int keyword_begin = 261;
  public static final int keyword_rescue = 262;
  public static final int keyword_ensure = 263;
  public static final int keyword_end = 264;
  public static final int keyword_if = 265;
  public static final int keyword_unless = 266;
  public static final int keyword_then = 267;
  public static final int keyword_elsif = 268;
  public static final int keyword_else = 269;
  public static final int keyword_case = 270;
  public static final int keyword_when = 271;
  public static final int keyword_while = 272;
  public static final int keyword_until = 273;
  public static final int keyword_for = 274;
  public static final int keyword_break = 275;
  public static final int keyword_next = 276;
  public static final int keyword_redo = 277;
  public static final int keyword_retry = 278;
  public static final int keyword_in = 279;
  public static final int keyword_do = 280;
  public static final int keyword_do_cond = 281;
  public static final int keyword_do_block = 282;
  public static final int keyword_do_LAMBDA = 283;
  public static final int keyword_return = 284;
  public static final int keyword_yield = 285;
  public static final int keyword_super = 286;
  public static final int keyword_self = 287;
  public static final int keyword_nil = 288;
  public static final int keyword_true = 289;
  public static final int keyword_false = 290;
  public static final int keyword_and = 291;
  public static final int keyword_or = 292;
  public static final int keyword_not = 293;
  public static final int modifier_if = 294;
  public static final int modifier_unless = 295;
  public static final int modifier_while = 296;
  public static final int modifier_until = 297;
  public static final int modifier_rescue = 298;
  public static final int keyword_alias = 299;
  public static final int keyword_defined = 300;
  public static final int keyword_BEGIN = 301;
  public static final int keyword_END = 302;
  public static final int keyword__LINE__ = 303;
  public static final int keyword__FILE__ = 304;
  public static final int keyword__ENCODING__ = 305;
  public static final int tIDENTIFIER = 306;
  public static final int tFID = 307;
  public static final int tGVAR = 308;
  public static final int tIVAR = 309;
  public static final int tCONSTANT = 310;
  public static final int tCVAR = 311;
  public static final int tLABEL = 312;
  public static final int tINTEGER = 313;
  public static final int tFLOAT = 314;
  public static final int tRATIONAL = 315;
  public static final int tIMAGINARY = 316;
  public static final int tCHAR = 317;
  public static final int tNTH_REF = 318;
  public static final int tBACK_REF = 319;
  public static final int tSTRING_CONTENT = 320;
  public static final int tREGEXP_END = 321;
  public static final int tDUMNY_END = 322;
  public static final int tUMINUS_NUM = 323;
  public static final int END_OF_INPUT = 324;
  public static final int tSP = 325;
  public static final int tUPLUS = 326;
  public static final int tUMINUS = 327;
  public static final int tPOW = 328;
  public static final int tCMP = 329;
  public static final int tEQ = 330;
  public static final int tEQQ = 331;
  public static final int tNEQ = 332;
  public static final int tGEQ = 333;
  public static final int tLEQ = 334;
  public static final int tANDOP = 335;
  public static final int tOROP = 336;
  public static final int tMATCH = 337;
  public static final int tNMATCH = 338;
  public static final int tDOT2 = 339;
  public static final int tDOT3 = 340;
  public static final int tBDOT2 = 341;
  public static final int tBDOT3 = 342;
  public static final int tAREF = 343;
  public static final int tASET = 344;
  public static final int tLSHFT = 345;
  public static final int tRSHFT = 346;
  public static final int tANDDOT = 347;
  public static final int tCOLON2 = 348;
  public static final int tCOLON3 = 349;
  public static final int tOP_ASGN = 350;
  public static final int tASSOC = 351;
  public static final int tLPAREN = 352;
  public static final int tLPAREN_ARG = 353;
  public static final int tLBRACK = 354;
  public static final int tLBRACE = 355;
  public static final int tLBRACE_ARG = 356;
  public static final int tSTAR = 357;
  public static final int tDSTAR = 358;
  public static final int tAMPER = 359;
  public static final int tLAMBDA = 360;
  public static final int tSYMBEG = 361;
  public static final int tSTRING_BEG = 362;
  public static final int tXSTRING_BEG = 363;
  public static final int tREGEXP_BEG = 364;
  public static final int tWORDS_BEG = 365;
  public static final int tQWORDS_BEG = 366;
  public static final int tSTRING_END = 367;
  public static final int tSYMBOLS_BEG = 368;
  public static final int tQSYMBOLS_BEG = 369;
  public static final int tSTRING_DEND = 370;
  public static final int tSTRING_DBEG = 371;
  public static final int tSTRING_DVAR = 372;
  public static final int tLAMBEG = 373;
  public static final int tLABEL_END = 374;
  public static final int tIGNORED_NL = 375;
  public static final int tCOMMENT = 376;
  public static final int tEMBDOC_BEG = 377;
  public static final int tEMBDOC = 378;
  public static final int tEMBDOC_END = 379;
  public static final int tHEREDOC_BEG = 380;
  public static final int tHEREDOC_END = 381;
  public static final int k__END__ = 382;
  public static final int tLOWEST = 383;
  public static final int yyErrorCode = 256;

  /** number of final state.
    */
  protected static final int yyFinal = 1;

  /** parser tables.
      Order is mandated by <i>jay</i>.
    */
  protected static final int[] yyLhs = {
//yyLhs 829
    -1,   229,     0,    35,    36,    36,    36,    37,    37,    30,
    38,   232,   233,    41,   234,    41,    42,    43,    43,    43,
    44,   235,    44,    34,   217,   236,    45,    45,    45,    45,
    45,    45,    45,    45,    45,    45,    45,    45,    45,    45,
    45,    45,    45,    45,    86,    86,    86,    86,    86,    86,
    86,    86,    86,    86,    86,    40,    40,    40,    84,    84,
    84,    46,    46,    46,    46,    46,   238,    46,   239,    46,
    46,    27,    28,   240,    29,    53,    53,   241,   243,    54,
    50,    50,    51,    91,    91,   129,    58,    49,    49,    49,
    49,    49,    49,    49,    49,    49,    49,    49,    49,   134,
   134,   140,   140,   136,   136,   136,   136,   136,   136,   136,
   136,   136,   136,   137,   137,   135,   135,   139,   139,   138,
   138,   138,   138,   138,   138,   138,   138,   138,   138,   138,
   138,   138,   138,   138,   138,   138,   138,   138,   131,   131,
   131,   131,   131,   131,   131,   131,   131,   131,   131,   131,
   131,   131,   131,   131,   131,   131,   131,   168,   168,    26,
    26,    26,   170,   170,   170,   170,   170,   133,   133,   108,
   245,   108,   169,   169,   169,   169,   169,   169,   169,   169,
   169,   169,   169,   169,   169,   169,   169,   169,   169,   169,
   169,   169,   169,   169,   169,   169,   169,   169,   169,   169,
   169,   169,   181,   181,   181,   181,   181,   181,   181,   181,
   181,   181,   181,   181,   181,   181,   181,   181,   181,   181,
   181,   181,   181,   181,   181,   181,   181,   181,   181,   181,
   181,   181,   181,   181,   181,   181,   181,   181,   181,   181,
   181,   181,   181,    47,    47,    47,    47,    47,    47,    47,
    47,    47,    47,    47,    47,    47,    47,    47,    47,    47,
    47,    47,    47,    47,    47,    47,    47,    47,    47,    47,
    47,    47,    47,    47,    47,    47,    47,    47,    47,    47,
    47,    47,    47,    47,    47,    47,    47,    47,    39,    39,
    39,   182,   182,   182,   182,    57,    57,   194,   214,   220,
    55,    79,    79,    79,    79,    85,    85,    72,    72,    72,
    73,    73,    71,    71,    71,    71,    71,    70,    70,    70,
    70,    70,    70,    70,   247,    78,    81,    81,    80,    80,
    68,    68,    68,    68,    69,    69,    88,    88,    87,    87,
    87,    48,    48,    48,    48,    48,    48,    48,    48,    48,
    48,    48,   248,    48,   249,    48,    48,    48,    48,    48,
    48,    48,    48,    48,    48,    48,    48,    48,    48,    48,
    48,    48,    48,    48,    48,    48,   251,    48,   252,    48,
    48,    48,   253,    48,   255,    48,   256,    48,   257,    48,
   258,    48,    48,    48,    48,    48,    56,   204,   205,   213,
    31,    32,   212,    33,   215,   216,   210,   206,   207,   218,
   219,   203,   202,   208,   211,   211,   201,   244,   250,   250,
   250,   242,   242,    59,    59,    60,    60,   111,   111,   101,
   101,   102,   102,   104,   104,   104,   104,   104,   103,   103,
   190,   190,   260,   259,    76,    76,    76,    76,    77,    77,
   192,   112,   112,   112,   112,   112,   112,   112,   112,   112,
   112,   112,   112,   112,   112,   112,   113,   113,   114,   114,
   121,   121,   120,   120,   122,   122,   226,   227,   228,   261,
   262,   123,   127,   127,   124,   263,   124,   130,    90,    90,
    90,    90,    90,    52,    52,    52,    52,    52,    52,    52,
    52,    52,   128,   128,   264,   125,   265,   126,    62,    62,
    62,    62,    61,    63,    63,   225,   224,   221,   266,   141,
   142,   142,   143,   143,   143,   144,   144,   144,   144,   144,
   144,   145,   146,   146,   267,   147,   147,   222,   223,   148,
   148,   148,   148,   148,   148,   148,   148,   148,   148,   148,
   148,   148,   268,   148,   148,   148,   150,   150,   150,   150,
   150,   150,   151,   151,   152,   152,   149,   184,   184,   153,
   153,   154,   161,   161,   161,   161,   162,   162,   163,   163,
   188,   188,   185,   185,   186,   187,   187,   155,   155,   155,
   155,   155,   155,   155,   155,   155,   155,   156,   156,   156,
   156,   156,   156,   156,   156,   156,   156,   156,   156,   156,
   156,   156,   156,   157,   158,   158,   159,   160,   160,   160,
    64,    64,    65,    65,    65,    66,    66,    67,    67,    20,
    20,     2,     3,     3,     3,     4,     5,     6,   269,   269,
    11,    16,    16,    19,    19,    12,    13,    13,    14,    15,
    17,    17,    18,    18,     7,     7,     8,     8,     9,     9,
    10,   270,    10,   271,   272,   273,   274,    10,   275,   275,
   110,   110,    25,    25,    23,   164,   164,    24,    21,    21,
    22,    22,    22,    22,   193,   193,   193,    82,    82,    82,
    82,    82,    82,    82,    82,    82,    82,    82,    82,    83,
    83,    83,    83,    83,    83,    83,    83,    83,    83,    83,
    83,   109,   109,   276,    89,    89,    95,    95,    96,    94,
   277,    94,    74,    74,    74,    74,    74,    75,    75,    97,
    97,    97,    97,    97,    97,    97,    97,    97,    97,    97,
    97,    97,    97,    97,   191,   175,   175,   175,   175,   174,
   174,   178,    99,    99,    98,    98,   177,   117,   117,   119,
   119,   118,   118,   116,   116,   197,   197,   189,   176,   176,
   115,    93,    92,    92,   100,   100,   196,   196,   171,   171,
   195,   195,   172,   172,   173,   173,     1,   278,     1,   105,
   105,   106,   106,   107,   107,   107,   107,   107,   107,   165,
   165,   165,   166,   166,   167,   167,   167,   183,   183,   179,
   179,   180,   180,   230,   230,   237,   237,   198,   199,   209,
   246,   246,   246,   254,   254,   231,   231,   132,   200,
    }, yyLen = {
//yyLen 829
     2,     0,     2,     2,     1,     1,     3,     1,     2,     1,
     3,     0,     0,     8,     0,     5,     2,     1,     1,     3,
     1,     0,     3,     0,     2,     0,     4,     3,     3,     3,
     2,     3,     3,     3,     3,     4,     5,     1,     4,     4,
     7,     4,     1,     1,     4,     4,     7,     6,     6,     6,
     6,     5,     4,     4,     4,     1,     4,     3,     1,     4,
     1,     1,     3,     3,     3,     2,     0,     7,     0,     7,
     1,     1,     2,     0,     5,     1,     1,     0,     0,     4,
     1,     1,     1,     1,     4,     3,     1,     2,     3,     4,
     5,     4,     5,     6,     2,     2,     2,     2,     2,     1,
     3,     1,     3,     1,     2,     3,     5,     2,     4,     2,
     4,     1,     3,     1,     3,     2,     3,     1,     3,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     4,     3,     3,     3,     3,     2,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     4,     3,     3,     3,     3,     2,     1,     1,     1,     2,
     1,     3,     1,     1,     1,     1,     1,     1,     1,     1,
     0,     4,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     4,     4,     7,     6,     6,     6,     6,
     5,     4,     3,     3,     2,     2,     2,     2,     3,     3,
     3,     3,     3,     3,     4,     2,     2,     3,     3,     3,
     3,     1,     3,     3,     3,     3,     3,     2,     2,     3,
     3,     3,     3,     4,     4,     4,     6,     1,     1,     4,
     3,     1,     1,     1,     1,     3,     3,     1,     1,     1,
     1,     1,     2,     4,     2,     1,     4,     3,     5,     3,
     1,     1,     1,     1,     2,     4,     2,     1,     4,     4,
     2,     2,     4,     1,     0,     2,     2,     1,     2,     1,
     1,     1,     3,     3,     2,     1,     1,     1,     3,     4,
     2,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     0,     4,     0,     4,     3,     3,     2,     3,
     3,     1,     4,     3,     1,     6,     4,     3,     2,     1,
     2,     1,     6,     6,     4,     4,     0,     6,     0,     5,
     5,     6,     0,     6,     0,     7,     0,     5,     0,     5,
     0,     5,     1,     1,     1,     1,     1,     1,     1,     1,
     2,     2,     1,     2,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     2,     1,     1,     1,     5,     1,     2,     1,     1,     1,
     3,     1,     3,     1,     3,     5,     1,     3,     2,     1,
     1,     1,     0,     2,     4,     2,     2,     1,     2,     0,
     1,     6,     8,     4,     6,     4,     2,     6,     2,     4,
     6,     2,     4,     2,     4,     1,     1,     1,     3,     4,
     1,     4,     1,     3,     1,     1,     0,     0,     0,     0,
     0,     9,     4,     1,     3,     0,     4,     3,     2,     4,
     5,     5,     3,     2,     4,     4,     3,     3,     3,     2,
     1,     4,     3,     3,     0,     7,     0,     7,     1,     2,
     3,     4,     5,     1,     1,     0,     0,     0,     0,     9,
     1,     1,     1,     3,     3,     1,     2,     3,     1,     1,
     1,     1,     3,     1,     0,     4,     1,     2,     2,     1,
     1,     4,     4,     4,     3,     4,     4,     4,     3,     3,
     3,     2,     0,     6,     2,     4,     1,     1,     2,     2,
     4,     1,     2,     3,     1,     3,     5,     2,     1,     1,
     3,     1,     3,     1,     2,     1,     1,     3,     2,     1,
     1,     3,     2,     1,     2,     1,     1,     1,     3,     3,
     2,     2,     1,     1,     1,     2,     2,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     2,     2,     4,     2,     3,     1,
     6,     1,     1,     1,     1,     2,     1,     2,     1,     1,
     1,     1,     1,     1,     2,     3,     3,     3,     1,     2,
     4,     0,     3,     1,     2,     4,     0,     3,     4,     4,
     0,     3,     0,     3,     0,     2,     0,     2,     0,     2,
     1,     0,     3,     0,     0,     0,     0,     7,     1,     1,
     1,     1,     1,     1,     2,     1,     1,     3,     1,     2,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     0,     4,     0,     1,     1,     3,     1,
     0,     3,     4,     2,     2,     1,     1,     2,     0,     6,
     8,     4,     6,     4,     6,     2,     4,     6,     2,     4,
     2,     4,     1,     0,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     3,     1,     3,     1,     2,     1,     2,
     1,     1,     3,     1,     3,     1,     1,     1,     2,     1,
     3,     3,     1,     3,     1,     3,     1,     1,     2,     1,
     1,     1,     2,     1,     2,     0,     1,     0,     4,     1,
     2,     1,     3,     3,     2,     1,     4,     2,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     0,     1,     0,     1,     2,     2,     2,
     0,     1,     1,     1,     1,     1,     2,     0,     0,
    }, yyDefRed = {
//yyDefRed 1415
     1,     0,     0,    43,   404,   405,   406,     0,   397,   398,
   399,   402,    23,    23,    23,     0,     0,   394,   395,   416,
   417,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   827,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   680,   681,   682,   683,   632,   711,   712,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   479,     0,
   654,   656,   658,     0,     0,     0,     0,     0,     0,   342,
     0,   633,   343,   344,   345,   347,   346,   348,   341,   629,
   678,   672,   673,   630,     0,     0,    77,    77,     0,     2,
     0,     5,     0,     0,     0,     0,     0,    61,     0,     0,
     0,     0,   349,     0,    37,     0,    81,     0,   371,     0,
     4,     0,     0,    99,     0,   113,    86,     0,   352,     0,
     0,     0,     0,     0,     0,    23,     0,   212,   223,   213,
   236,   209,   229,   219,   218,   239,   240,   234,   217,   216,
   211,   237,   241,   242,   221,   210,   224,   228,   230,   222,
   215,   231,   238,   233,   232,   225,   235,   220,   208,   227,
   226,   207,   214,   205,   206,   202,   203,   204,   162,   164,
   163,   197,   198,   193,   175,   176,   177,   184,   181,   183,
   178,   179,   199,   200,   185,   186,   190,   194,   180,   182,
   172,   173,   174,   187,   188,   189,   191,   192,   195,   196,
   201,   168,     0,   169,   165,   167,   166,   400,   401,   403,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   654,
     0,     0,     0,     0,   317,     0,     0,     0,   331,    97,
   323,     0,     0,   791,     0,     0,    98,     0,   499,    94,
     0,     0,   816,     0,     0,    25,     0,     9,     0,     8,
   297,    24,     0,   392,   393,     0,     0,     0,   265,     0,
     0,   361,     0,     0,     0,     0,     0,    21,     0,     0,
     0,    18,     0,    17,     0,     0,   354,     0,     0,     0,
   301,     0,     0,     0,   789,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   396,     0,     0,     0,   476,   685,   684,
   686,     0,   674,   675,   676,     0,     0,     0,   638,     0,
     0,     0,     0,   277,    65,   278,   634,     0,   388,     0,
     0,   717,     0,   390,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   427,   428,   823,   824,     3,     0,
   825,     0,     0,     0,     0,   827,     0,     0,    68,     0,
     0,     0,     0,     0,   293,   294,     0,     0,     0,     0,
     0,     0,     0,     0,    66,     0,   291,   292,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   408,   488,   506,
   407,   504,   370,   506,   810,     0,     0,   809,     0,     0,
   493,     0,   368,   827,   812,   811,     0,   827,   827,   827,
     0,     0,     0,   115,    96,     0,    76,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   689,   688,     0,
   691,   787,     0,    72,   786,    71,     0,   378,     0,     0,
   693,   692,   694,   695,   697,   696,   698,     0,     0,     0,
     0,     0,     0,   350,   160,   386,     0,     0,    95,   170,
   794,     0,   334,   797,   326,     0,     0,     0,     0,     0,
     0,     0,     0,   320,   329,   827,     0,   321,   827,   827,
     0,     0,   313,     0,     0,   312,     0,   325,     0,   367,
     0,    64,    27,    29,    28,     0,   827,   298,     0,     0,
     0,     0,     0,     0,     0,   827,     0,     0,   356,    16,
     0,     0,     0,     0,   821,   302,   359,     0,   304,   360,
   790,     0,   679,     0,   117,     0,   719,     0,     0,     0,
     0,   477,   660,   677,   663,   661,   655,   635,   636,   657,
   637,   659,   639,     0,     0,     0,     0,   750,   747,   746,
   745,   748,   756,   765,   744,     0,   777,   766,   781,   780,
   776,   742,     0,     0,   754,     0,   774,     0,   763,     0,
   725,   751,   749,   440,     0,     0,   767,   441,     0,   726,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,    77,   826,     6,    31,    32,
    33,    34,   299,     0,    62,    63,   517,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   517,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   476,     0,   476,     0,     0,     0,     0,   498,
   802,     0,   496,     0,     0,     0,     0,   801,     0,   497,
     0,   803,     0,   504,    88,     0,   799,   800,   492,     0,
     0,     0,     0,     0,     0,     0,   116,     0,   827,   419,
     0,     0,     0,   808,   807,    73,     0,     0,     0,   384,
   157,     0,   159,   713,   382,     0,     0,     0,     0,     0,
     0,   363,     0,   827,     0,     0,     0,   793,     0,     0,
     0,     0,     0,     0,   333,   328,     0,     0,   792,     0,
     0,     0,   307,     0,   309,   366,   817,    26,     0,     0,
    10,     0,     0,     0,     0,     0,     0,     0,    22,     0,
    19,   355,     0,     0,     0,     0,     0,     0,     0,     0,
   478,   664,     0,   640,   643,     0,     0,   648,   645,     0,
     0,   649,     0,     0,   431,     0,     0,     0,   429,   718,
     0,   735,     0,   738,     0,   723,     0,   740,   757,     0,
     0,     0,   724,   782,   778,   584,   768,     0,     0,     0,
     0,     0,    55,   721,     0,     0,     0,   414,   415,   374,
   422,    78,   421,   375,     0,     0,     0,     0,     0,     0,
    35,   515,   515,     0,   487,   477,   502,   477,   503,   827,
   827,   504,   495,     0,     0,     0,     0,   827,   827,   311,
   494,     0,   310,     0,     0,     0,    82,     0,     0,    45,
   244,    60,     0,     0,     0,     0,    52,   251,     0,     0,
   330,     0,    44,   243,    39,    38,     0,   336,     0,   114,
     0,     0,   353,     0,     0,   420,     0,     0,   517,     0,
     0,   411,     0,     0,     0,     0,     0,     0,     0,     0,
   161,     0,     0,     0,   362,   171,     0,   796,     0,     0,
   827,   827,     0,   827,   827,   322,     0,     0,     0,     0,
    51,   250,   303,   118,     0,    23,   665,   671,   662,   670,
   644,     0,     0,     0,     0,     0,   438,     0,     0,   753,
   727,   755,     0,     0,     0,   775,     0,   764,   784,     0,
     0,     0,   752,   770,   443,   389,     0,   827,   827,   391,
    79,     0,     0,   516,   516,     0,   478,   478,     0,     0,
     0,    92,   827,   818,     0,     0,    90,    85,     0,   827,
   827,     0,     0,     0,   827,   490,   491,     0,     0,   827,
     0,   409,     0,   621,     0,   413,   412,     0,   423,   425,
     0,     0,   788,    74,   515,   380,     0,   379,     0,   508,
     0,     0,     0,     0,     0,   501,   387,    36,     0,     0,
   827,     0,     0,     0,   308,   365,     0,     0,   666,   430,
   432,     0,     0,     0,   731,     0,   733,     0,   739,     0,
   736,   722,   741,     0,     0,     0,     0,   381,     0,     0,
     0,    23,    23,    49,   248,    50,   249,    93,     0,    47,
   246,    48,   247,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,    11,     0,     0,     0,   623,   624,
   372,   426,     0,   516,   377,   509,     0,     0,   373,     0,
   714,   383,     0,     0,   483,   480,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   606,   605,   607,   608,   610,
   609,   611,   613,   619,   580,     0,     0,     0,   516,     0,
     0,     0,   654,     0,   598,   599,   600,   601,   603,   602,
   604,   597,   612,    69,     0,   531,     0,   536,   528,   529,
   539,     0,   540,   592,   593,     0,   530,     0,   576,     0,
   585,   586,   575,     0,     0,    67,     0,     0,    46,   245,
     0,    59,     0,     0,    40,     0,   410,    15,   628,     0,
     0,     0,   626,     0,     0,     0,   510,     0,   385,     0,
     0,     0,     0,   732,     0,   729,   734,   737,   595,   596,
   158,   617,     0,     0,     0,     0,     0,   561,     0,   551,
   554,   827,     0,   567,     0,   614,     0,   615,     0,     0,
   534,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   582,     0,     0,   467,   466,     0,    12,   627,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   625,     0,     0,     0,     0,   511,
   513,   514,   512,     0,     0,   485,     0,   481,   669,   668,
   667,     0,     0,   550,   549,     0,     0,     0,   562,   552,
   819,   581,     0,   532,   527,     0,     0,   588,   589,   618,
   548,   538,   544,   537,     0,     0,     0,     0,     0,     0,
   654,   577,   572,     0,   569,   465,     0,   772,     0,     0,
     0,   761,     0,     0,   447,     0,     0,     0,   507,   505,
     0,     0,     0,     0,     0,     0,   424,   518,     0,     0,
   482,     0,     0,     0,   730,   555,   563,     0,     0,   616,
     0,   535,   542,   541,   543,   546,   545,   547,     0,     0,
     0,   461,     0,   458,   456,     0,     0,   445,   468,     0,
   463,     0,     0,     0,     0,     0,   446,    13,     0,     0,
     0,     0,     0,   620,     0,   523,   524,   474,     0,   472,
   475,     0,   484,     0,     0,     0,   570,   566,   448,   773,
     0,     0,     0,     0,   469,   762,     0,     0,   358,     0,
     0,     0,     0,     0,   471,   486,     0,   553,     0,   462,
     0,   459,     0,   453,     0,   455,   444,   464,     0,     0,
   520,   521,   519,   473,     0,     0,     0,     0,   460,   454,
     0,   451,   457,     0,   452,
    }, yyDgoto = {
//yyDgoto 279
     1,   452,    69,    70,    71,    72,    73,   321,   326,   327,
   556,    74,    75,   565,    76,    77,   563,   564,   566,   765,
    78,    79,    80,    81,    82,    83,   471,   453,   266,   267,
   258,    86,    87,    88,   207,    89,    90,    91,   259,   799,
   800,   687,   688,   280,   281,   282,    93,    94,    95,    96,
    97,   847,    98,   438,   345,   235,   269,   100,   270,   987,
   988,   882,  1000,  1242,   982,  1067,  1161,  1157,   664,   238,
   502,   503,   659,   840,   930,   781,  1368,  1331,   249,   288,
   493,   240,   102,   241,   862,   863,   104,   864,   868,   704,
   105,   106,  1286,  1287,   338,   339,   340,   582,   583,   584,
   585,   774,   775,   776,   777,   292,   504,   243,   202,   244,
   918,   363,  1289,  1213,  1214,   586,   587,   588,  1290,  1291,
  1358,  1243,  1359,   108,  1247,   653,   651,  1085,   422,   674,
   408,   245,   260,   203,   111,   112,   113,   114,   115,   545,
   285,   879,  1402,  1237,  1123,  1255,  1125,  1126,  1127,  1128,
  1185,  1186,  1187,  1283,  1188,  1130,  1131,  1132,  1133,  1134,
  1135,  1136,  1137,  1138,   322,   116,   745,   662,   474,   663,
   205,   589,   590,   785,   591,   592,   593,   594,   942,   707,
   426,   206,   406,   695,  1139,  1140,   596,  1142,  1143,   597,
   598,   599,  1334,   324,   622,   600,   601,   602,   509,   835,
   494,   271,   990,   883,   118,   119,   413,   409,   991,  1190,
   120,   809,   121,   122,   518,   123,   124,   125,   984,  1159,
   623,   821,  1206,  1207,  1038,   953,   551,   760,   915,     2,
   368,   458,  1155,  1300,  1065,   526,   515,   510,   640,   626,
   877,   346,   811,   950,   272,   712,   535,   250,   435,   532,
   690,   880,   697,   887,   691,   885,   708,   603,   606,   789,
   790,   317,  1170,  1312,   654,   652,  1354,  1266,  1318,   329,
   762,   761,   916,  1018,  1086,  1250,   886,   342,   692,
    }, yySindex = {
//yySindex 1415
     0,     0, 26829,     0,     0,     0,     0, 30536,     0,     0,
     0,     0,     0,     0,     0, 27321, 27321,     0,     0,     0,
     0,    80,     0,     0,     0,     0,   505, 30431,   158,    58,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  1233, 29290, 29290,
 29290, 29290,  -109, 26951, 27075, 27935, 28181, 31109,     0, 30960,
     0,     0,     0,   290,   290,   290,   290, 29407, 29290,     0,
   -80,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   317,   317,     0,     0, 32647,     0,
   295,     0,  1135,   747, 25031,     0,   114,     0,    51,   149,
   143,   201,     0,   111,     0,    44,     0,   191,     0,   608,
     0,   629, 32939,     0,   562,     0,     0, 27321,     0, 28305,
 30858, 26024, 28305, 33052, 33165,     0,   644,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   650,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   725,     0,     0,     0,     0,     0,
     0,     0,     0, 29290,   565, 27075, 29290, 29290, 29290,     0,
 29290,   317,   317, 25756,     0,   420,   153,   847,     0,     0,
     0,   546,   882,     0,   606,   878,     0, 27443,     0,     0,
 27321, 26139,     0, 29520,  1053,     0,   965,     0, 26829,     0,
     0,     0,   688,     0,     0,    80,   317,   317,     0,   260,
   201,     0,   644,   691, 10574, 10574,   679,     0, 26951,  1048,
   295,     0,  1135,     0,     0,   158,     0,   589,  1007,   766,
     0,   420,   993,   766,     0,     0,     0,     0,     0,   158,
     0,     0,     0,     0,     0,     0,     0,     0,  1233,   850,
 33278,   317,   317,     0,   277,     0,  1093,     0,     0,     0,
     0,   895,     0,     0,     0,  1108,  1316,   936,     0,  1146,
  1146,  1146,  1146,     0,     0,     0,     0,  4439,     0,  1089,
     0,     0,  4439,     0,  1119, 27075, 28305, 27075,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   876,   282,     0,   921,     0,     0,     0,     0,     0, 26583,
     0, 28305, 28305, 28305, 28305,     0, 29520, 29520,     0, 29290,
 29290, 29290, 29290, 29290,     0,     0, 29290, 29290, 29290, 29290,
 29290, 29290, 29290, 29290,     0, 29290,     0,     0, 29290, 29290,
 29290, 29290, 29290, 29290, 29290, 29290, 29290,     0,     0,     0,
     0,     0,     0,     0,     0, 14638, 27321,     0, 31464, 29290,
     0,   873,     0,     0,     0,     0, 31655,     0,     0,     0,
 26951, 31367,  1159,     0,     0, 27075,     0,   747,   168,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,    53,     0,     0,     0,   295,     0,  1173,   168,
     0,     0,     0,     0,     0,     0,     0,     0,     0, 28305,
   442,  1176,   306,     0,     0,     0,  1112, 25909,     0,     0,
     0,   887,     0,     0,     0,  1015,  1182,  1187, 29290, 31720,
 27321, 31787, 27567,     0,     0,     0, 28059,     0,     0,     0,
 29290,  1205,     0,   158,  1207,     0,   158,     0,   242,     0,
  1213,     0,     0,     0,     0, 30536,     0,     0, 29290,  1138,
 29290,  1210,  1218, 31828, 31787,     0,    58,   158,     0,     0,
 26705,     0,  1240, 27935,     0,     0,     0, 28181,     0,     0,
     0,   965,     0,     0,     0,  1239,     0, 31883, 27321, 31977,
 33278,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,  1323,   299,  1416,   419,     0,     0,     0,
     0,     0,     0,     0,     0,  1842,     0,     0,     0,     0,
     0,     0,   158,  1241,     0,  1244,     0,  1245,     0,  1248,
     0,     0,     0,     0, 29290,     0,     0,     0,  1256,     0,
  1005,  1008,   172, 27075, 29637,   295, 27075, 29637,  -138,   229,
  -138,     0, 32205, 27321, 32299,     0,     0,     0,     0,     0,
     0,     0,     0, 27197,     0,     0,     0,   691,  4373,  4373,
  4373,  4373, 11588, 11081,  4373,  4373, 10574, 10574,   812,   812,
     0, 24175,  1893,  1893,  1056,   300,   300,   691,   691,   691,
  2583,  -138,     0,  1188,     0,  -138,   972,     0,    25,     0,
     0,    80,     0,     0,  1271,   158,   978,     0,   982,     0,
    80,     0,  2583,     0,     0, 29750,     0,     0,     0,    80,
 29750, 28427, 28551,   158, 33278,  1290,     0,  -138,     0,     0,
 27075,  1072, 29520,     0,     0,     0,  1061,  1070, 27075,     0,
     0,     0,     0,     0,     0, 32340, 27321, 31655, 27075, 27075,
   158,     0, 30536,     0, 29290, 29637, 29637,     0,   994,    39,
   158,   999,  1006,   420,     0,     0,   882, 29290,     0, 29290,
 29290, 27689,     0, 28059,     0,     0,     0,     0, 29520, 25756,
     0,   691, 29867, 29867,  1013,    80,    80, 29750,     0,     0,
     0,     0,   766, 33278,     0,     0,   158,     0,     0,  1239,
     0,     0,  1012,     0,     0,    96,   290,     0,     0,    96,
   290,     0,  1842,  1175,     0,  1311,  1325,   158,     0,     0,
  4439,     0,  4439,     0,   170,     0,  1295,     0,     0, 29290,
  1314,    40,     0,     0,     0,     0,     0,  -138,   505,  1094,
  1098, 25756,     0,     0,  -138,  1094,  1098,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   158,     0,     0, 27075,
     0,     0,     0,  1333,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   873, 27567,  1062,  1305,     0,     0,     0,
     0,   873,     0,  1286,  1103, 25450,     0,  1123,   643,     0,
     0,     0,   191,  1356,    51,   114,     0,     0, 29290, 25450,
     0,  1380,     0,     0,     0,     0,     0,     0,  1137,     0,
  1239, 33278,     0,  1174,   454,     0,   242, 31277,     0,  -138,
  1070,     0,  -138, 28673,  1172,   295, 28305, 27075,     0,     0,
     0,   158,  -138,  1317,     0,     0, 29290,     0,  1098,  1098,
     0,     0,  1096,     0,     0,     0,  1403,   158,   242,   505,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  1146,  1146,  1146,  1146,   158,     0,  1842,  1200,     0,
     0,     0,  1404,  1406,  1409,     0,  1413,     0,     0,  1256,
  1155,  1409,     0,     0,     0,     0, 29637,     0,     0,     0,
     0,     0,  -138,     0,     0, 29290,     0,     0, 29750, 29750,
  1343,     0,     0,     0, 29750, 29750,     0,     0,   679,     0,
     0, 32384, 27321, 32430,     0,     0,     0,     0, 28797,     0,
  1239,     0,  1172,     0, 28919,     0,     0,  -138,     0,     0,
 27075, 28305,     0,     0,     0,     0,  -138,     0, 29290,     0,
   250,  -138, 27075,   295,  -138,     0,     0,     0, 29290, 29290,
     0, 29290, 29290, 28059,     0,     0, 29867,  3872,     0,     0,
     0,  1428,  1433,  4439,     0,  1295,     0,  1295,     0,  1295,
     0,     0,     0,  1094,  1098, 29290, 29290,     0, 24263, 24263,
 25756,     0,     0,     0,     0,     0,     0,     0, 29750,     0,
     0,     0,     0, 29290, 27197,   972,    25,   158,   978,   982,
 29750, 29290,     0, 27197,     0,  1225,     0,  1127,     0,     0,
     0,     0,   168,     0,     0,     0, 29043, 27075,     0,  -138,
     0,     0, 29290,  4439,     0,     0, 27075,  1200,  1200,  1409,
  1452,  1409,  1409, 25756, 25756,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  4852,  4852,   746,     0,  3588,
   158,  1191,     0,  1662,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,    37,     0,  1374,     0,     0,     0,
     0,   738,     0,     0,     0,    52,     0,  1456,     0,  1461,
     0,     0,     0, 14748,   439,     0,  1383,  1383,     0,     0,
 25756,     0,  1062,     0,     0, 27075,     0,     0,     0, 27075,
 33391,   168,     0, 27075, 24263, 29290,     0,   648,     0,   158,
  -111,   464,  1433,     0,  1295,     0,     0,     0,     0,     0,
     0,     0, 14748,  1165,   158,   158, 12767,     0,  1479,     0,
     0,     0,  1401,     0,  1095,     0, 28305,     0,  1235, 12767,
     0,  4852,  4852,   746,   158,   158, 24263, 24263,   804, 14748,
  1165,     0,  2026, 27075,     0,     0, 27075,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  1249,   664,     0,     0, 27075,   454,   168,   827,     0,
     0,     0,     0,  1517,  1501,     0, 27075,     0,     0,     0,
     0,  1409,    36,     0,     0,  1165,  1520,  1522,     0,     0,
     0,     0,   158,     0,     0,  1523, 14748,     0,     0,     0,
     0,     0,     0,     0,   158,   158,   158,   158,   158,   158,
     0,     0,     0,  1526,     0,     0,  1530,     0,  1534,   158,
  1538,     0,  1437,  1540,     0, 33504,     0,  1256,     0,     0,
  1225,     0, 32527, 27321, 32752,  1174,     0,     0, 28305, 28305,
     0,  1694, 27075,  1464,     0,     0,     0, 14748,   804,     0,
 14748,     0,     0,     0,     0,     0,     0,     0,   541, 12767,
  4297,     0,  4297,     0,     0,  1466,   170,     0,     0,  1723,
     0,     0,     0,  1282,   739, 33504,     0,     0,     0,     0,
   158,     0,     0,     0, 27075,     0,     0,     0,   822,     0,
     0,  -138,     0,  1551,   158,  1551,     0,     0,     0,     0,
  1552,  1553,  1554,  1555,     0,     0,  1256,  1552,     0, 32849,
   739,     0,   263,  1694,     0,     0, 14748,     0,  1723,     0,
  1723,     0,  4297,     0,  1723,     0,     0,     0,     0,     0,
     0,     0,     0,     0,  1552,  1552,  1564,  1552,     0,     0,
  1723,     0,     0,  1552,     0,
    }, yyRindex = {
//yyRindex 1415
     0,     0,   922,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0, 18590, 18680,     0,     0,     0,
     0, 14868, 19282, 19597, 19687, 19779, 29980,     0, 29177,     0,
     0, 20094, 20184, 20276, 15719,  5936, 20591, 20681, 15836, 20773,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   764,   764,  1516,  1485,   308,     0,  1436,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  9393,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,  4008,  4008,     0,     0,     0,     0,
   204,     0,   719, 23587, 23432,  9488, 23011,     0,  9586,     0,
 12025, 27813,     0,     0,     0, 23076,     0, 21088,     0,     0,
     0,     0,   384,     0,     0,     0,     0, 18784,     0,     0,
     0,  1348,     0,     0,     0,     0, 16203,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0, 23666,     0,     0,     0,     0,     0,     0,     0,
  6848,  6943,  7041,  7357,     0,  7452,  7550,  7866,  4601,  7961,
  8059,  4968,  8375,  2208,     0,   764, 21444, 21678,  1730,     0,
     0,  4008,  4008, 14750,     0, 21717,     0,  7260,     0,     0,
     0,     0,  7260,     0,  8979,     0,     0,   431,     0,     0,
     0,  1573,     0,     0,     0,     0, 30097,     0,   600,     0,
     0,     0,  9997,     0,     0,  8470,  4008,  4008,     0,     0,
     0,     0,  9902, 10504, 13865, 13960, 21178,     0,   764,     0,
  3242,     0,  1349,     0,   611,  1573,     0,  1529,     0,  1529,
     0,     0,     0,  1504,     0,  1817,  2510,  3001,  3109,  1590,
  3171,  3204,  3220,  1635,  3340,  3348,  2048,  3451,     0,     0,
     0,  3229,  3229,     0,     0,  3517,   556,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  1472,
   473,  1539,   531,     0,     0,     0,     0,   632,     0,     0,
 25276,     0,   543,     0,     0,   294,     0,   294,   339,   545,
   872,  1292,  1974,  2296,  2314,  2748,  2594,  2636,  2778,  2911,
     0,     0,  3129,     0,     0,     0,     0,     0,     0,   426,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
 10093, 10409,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   105,     0,     0,     0,
     0,  8278,     0,     0,     0,     0,     0,     0,     0,     0,
   764,   753,   806,     0,     0,   759,     0, 12272,     0,  1541,
  2713,  2826,  4762,  6606, 10244, 10751, 11258,     0,     0, 11765,
     0,     0,     0,     0,     0,     0,    32,     0,   783,     0,
     0,     0,     0,     0,     0,     0,     0, 23987, 24102,     0,
     0, 25390,     0,     0,     0,     0,     0,  1573,     0,     0,
     0,  9077,     0,     0,     0,     0,     0,     0,     0,     0,
   105,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   774,   842,     0,  1573,   497,     0,  1573,     0,  1573,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  1573,     0,     0,
  3846,   125,     0,  1549,     0,     0,     0,   196,     0,     0,
     0,     0,     0,  3629,     0,   869,     0,     0,   105,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  1573,   179,     0,   179,     0,   192,     0,   179,
     0,     0,     0,     0,   175,   135,     0,     0,   192,     0,
    93,   388,   840,   759,     0,     0,   759,     0,     0,     0,
     0,  3361,     0,   105,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0, 10600,  2612,  4061,
 13218, 13462, 13562,  3760, 13658, 13779, 14079, 14174, 12424, 12519,
     0,  1576, 12915, 13001, 12615,  4244, 12121, 10916, 11011, 11107,
 13096,     0,     0,     0,     0,     0, 16320,  6053, 17772,     0,
     0, 27813,     0,  6420,   239,  1556, 16687,     0, 16804,     0,
 15235,     0, 13345,     0,     0,     0,     0,     0,     0, 18256,
     0,     0,     0,  1573,     0,   870,     0,     0,     0,     0,
   221, 24989,     0,     0,     0,     0,  1377,     0,   511,     0,
     0, 24875,     0,     0,     0,     0,   105,     0,   759,   600,
  1573,     0,     0,     0,     0,     0,     0,     0,  5085, 15352,
  1556,  5452,  5569, 21764,     0,     0,  7260,     0,     0,     0,
     0,   926,     0,   520,     0,     0,     0,     0,     0, 14260,
     0, 11423,     0,     0,  8568,     0,  8884,     0,     0,  1073,
     0,     0,  1529,     0,  2083,  1494,  1556,  2233,  2256,   966,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   973,     0,   963,   974,  1573,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0, 29980, 11518,
 23710, 14355,     0,     0,     0, 11614, 23747,     0,     0,     0,
     0,     0,     0,     0,  2925,   529,  1556,  3667,  3926,   294,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,  8787,   481, 19100,     0,     0,     0,     0,
     0,  9296,     0,     0,     0,  1816,     0, 23787,     0,     0,
     0,     0, 21270,     0,  1693, 23115,     0,     0,  1607,  3046,
     0,     0,     0,     0,     0,     0,  2156,     0, 23827,     0,
   985,     0,     0,   605,   313,     0,  1573,     0,     0,     0,
     0,     0,     0,     0,   313,     0,     0,   759, 24436, 24550,
     0,  1556,     0,     0,     0,     0,     0,     0,  9804, 10215,
     0,     0,  6532,     0,     0,     0,   497,  1573,  1573, 29980,
     0,     0,     0,     0,  2450,     0,     0,     0,     0,     0,
     0,  1546,   566,  1577,   574,  1573,     0,     0,     0,     0,
     0,     0,   179,   179,   179,     0,   179,     0,     0,   192,
   840,   179,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  3718,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0, 21585,     0,
     0,     0,   105,     0,     0,     0,     0,  4197,     0,     0,
  1030,     0,   504,     0,    35,     0,     0,     0,     0,     0,
   294,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   759,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   796,     0,     0,     0,   -58,     0,     0,
     0,  1035,  1118,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0, 11930, 10722,     0,     0,     0,     0,     0,
 14474,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0, 17171, 18139,  1556, 17288, 17655,
     0,  1607,  9754,     0,     0,   313,    83,   325,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   411,     0,     0,
     0,     0,     0,  1128,     0,     0,    85,     0,     0,   179,
   179,   179,   179, 14569,  7769,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  1556,
   864, 22138,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0, 23198,     0, 22100,     0,     0,     0,
     0,  3354,     0,     0,     0, 21894,     0, 11229,     0, 11736,
     0,     0,     0, 22247, 22556,     0, 25599, 25713,     0,     0,
 14655,     0, 19192, 23470,     0,   639,     0,     0,     0,   294,
     0,     0,     0,   221,     0,     0,     0,   313,     0,   989,
     0,     0,  1132,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0, 22408,  1556,  1556, 22598,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0, 23283,
     0, 22006, 22053,     0, 25102, 24666,     0,     0, 22707,     0,
 22447,     0,   112,   759,     0,     0,   600,     0,     0,   331,
  1206,  1430,  1453,  1786,  1875,  1878,  1364,  1917,  2191,  1659,
  2195,     0,     0,  2266,     0,   759,   313,     0,   383,     0,
     0,     0,     0,     0,   581,     0,   600,     0,     0,     0,
     0,   179,  1573,     0,     0, 22517, 22751, 22816,     0,     0,
     0,     0,  1573,     0,     0, 23346,     0,     0,     0,     0,
     0,     0,     0,     0,  1573,  1573,  1573,  1556,  1556,  1556,
     0,     0,     0, 22898,     0,     0,   461,     0,   461,   112,
   885,     0,     0,   461,     0,  1000,  1395,   885,     0,     0,
   313,  2285,     0,   105,     0,   605,     0,     0,     0,     0,
     0,     0,   759,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   991,     0,     0,     0,     0,     0,     0,     0,
     0,  2270,  2423,     0,  1158,     0,     0,     0,  2653,  1215,
  1556,  2658,  2682,     0,   477,     0,     0,     0,   623,     0,
     0,     0,     0, 22967,  1527, 23393,     0,     0,     0,     0,
   461,   461,   461,   461,     0,     0,   885,   461,     0,     0,
  1180,  2687,   313,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  2485,   894,
     0,     0,     0,     0,   461,   461,   461,   461,     0,     0,
     0,     0,     0,   461,     0,
    }, yyGindex = {
//yyGindex 279
     0,     0, 13000,     0,  1595, 13395, 18064,   -59,     0,     0,
  -286, 25331, 25363,     0, 25437, 25448,     0,     0,     0,  1101,
 25509,     0,    59,     0,     0,    41,  1543,   791,  2139,  2543,
     0,     0,     0,     0,    -7,  1412,     0,  1303,  1151,  -580,
  -523,  -548,   -28,     0,  1148,     3,  -104,  3757,    17,    -4,
   115,   998,     0,   -13,   -66,   637,    34,     0,   578,   448,
  -860,  -811,     0,     0,   380,     0,     0,   391,    49,  -460,
    70,  -413,    13,  1010,  -300,  -132,   480,  2554,    -3,     0,
  -203,  -417,  1581,  3035,  -521,   698,   389,  -571,     0,     0,
     0,     0,   376, -1157,   424,   829,   884,  -332,   183,  -737,
   934,  -802,  -821,   795,   951,     0,     7,  -435,     0,  2369,
     0,     0,     0,   580,     0,  -685,     0,   944,     0,   393,
     0,  -710,   349, 25588,     0,  -573,  1322,     0,   -94,   323,
   883,  2773,    -2,    45,  1654,     0,   -15,   -98,     9,  -493,
  -117,   365,     0,     0,  -824,   518,     0,     0,   482,  -993,
   -52,     0,  -828,  -472,  -420,     0,  -199,   551,     0,     0,
     0,  -844,     0,   547,     0,  -359,  -194,     0,  -468,     1,
   -50,  -749,    28,  -569,  -267, -1136,  -764,  2275,  -225,   -86,
     0,     0,  1651,     0, -1019,     0,   452,   549,     0,     0,
  1769,  -217,     0,  -718,   614,     0,     0,  -713,   469,   215,
     0,  1156,   782,     0,     0,     0,     0,     0,     0,   401,
     0,  -433,     0,     0,  1250,     0,     0,     0,     0,     0,
  1091,  -544,     0,     0,    -6,  -703,   489,  -694,   271,     0,
   -32,    20,     0,     0,     0,     0,     0,   181,     0,     0,
     0,     0,     0,     0,  1700,     0,  -213,     0,     0,     0,
  -424,     0,     0,     0,   -74,     0,     0,     0,     0,   472,
     0,     0,     0,     0,     0,     0,     0,     0,     0,    73,
     0,     0,     0,     0,     0,     0,     0,     0,     0,
    };
    protected static final int[] yyTable = YyTables.yyTable();
    protected static final int[] yyCheck = YyTables.yyCheck();

  /** maps symbol value to printable name.
      @see #yyExpecting
    */
  protected static final String[] yyNames = {
    "end-of-file",null,null,null,null,null,null,null,null,"escaped horizontal tab","'\\n'",
"escaped vertical tab","escaped form feed","escaped carriage return",null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,"' '","'!'",null,null,
    null,"'%'","'&'",null,"'('","')'","'*'","'+'","','","'-'","'.'","'/'",
    null,null,null,null,null,null,null,null,null,null,"':'","';'","'<'",
    "'='","'>'","'?'",null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,"'['","backslash","']'","'^'",null,"'`'",null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,"'{'","'|'","'}'","'~'",
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,"'class''","'module'","'def'",
"'undef'","'begin'","'rescue'","'ensure'",
"'end'","'if'","'unless'","'then'",
"'elsif'","'else'","'case'","'when'",
"'while'","'until'","'for'","'break'",
"'next'","'redo'","'retry'","'in'",
"'do'","'do' for condition","'do' for block","'do' for lambda",
"'return'","'yield'","'super'","'self'",
"'nil'","'true'","'false'","'and'",
"'or'","'not'","'if' modifier","'unless' modifier",
"'while' modifier","'until' modifier","'rescue' modifier","'alias'",
"'defined'","'BEGIN'","'END'","'__LINE__'",
"'__FILE__'","'__ENCODING__'","local variable or method","method","global variable",
"instance variable","constant","class variable","label","integer literal","float literal","rational literal",
"imaginary literal","char literal","numbered reference","back reference","literal content",
    "tREGEXP_END","dummy end","tUMINUS_NUM","end-of-input","escaped space",
"unary+","unary-","**","<=>","==","===","!=",">=","<=",
"&&","||","=~","!~","..","...","(..","(...",
"[]","[]=","<<",">>","&.","::",":: at EXPR_BEG",
"operator assignment","=>","'('","( arg","'['","'{'",
"{ arg","'*'","**arg","'&'","->","symbol literal",
"string literal","backtick literal","regexp literal","word list","verbatim work list",
"terminator","symbol list","verbatim symbol list","'}'",
    "tSTRING_DBEG","tSTRING_DVAR","tLAMBEG","tLABEL_END","tIGNORED_NL",
    "tCOMMENT","tEMBDOC_BEG","tEMBDOC","tEMBDOC_END","tHEREDOC_BEG",
    "tHEREDOC_END","k__END__","tLOWEST",
    };

  /** printable rules for debugging.
    */
  protected static final String[] yyRule = {
    "$accept : program",
    "$$1 :",
    "program : $$1 top_compstmt",
    "top_compstmt : top_stmts opt_terms",
    "top_stmts : none",
    "top_stmts : top_stmt",
    "top_stmts : top_stmts terms top_stmt",
    "top_stmt : stmt",
    "top_stmt : keyword_BEGIN begin_block",
    "block_open : '{'",
    "begin_block : block_open top_compstmt '}'",
    "$$2 :",
    "$$3 :",
    "bodystmt : compstmt lex_ctxt opt_rescue k_else $$2 compstmt $$3 opt_ensure",
    "$$4 :",
    "bodystmt : compstmt lex_ctxt opt_rescue $$4 opt_ensure",
    "compstmt : stmts opt_terms",
    "stmts : none",
    "stmts : stmt_or_begin",
    "stmts : stmts terms stmt_or_begin",
    "stmt_or_begin : stmt",
    "$$5 :",
    "stmt_or_begin : keyword_BEGIN $$5 begin_block",
    "allow_exits :",
    "k_END : keyword_END lex_ctxt",
    "$$6 :",
    "stmt : keyword_alias fitem $$6 fitem",
    "stmt : keyword_alias tGVAR tGVAR",
    "stmt : keyword_alias tGVAR tBACK_REF",
    "stmt : keyword_alias tGVAR tNTH_REF",
    "stmt : keyword_undef undef_list",
    "stmt : stmt modifier_if expr_value",
    "stmt : stmt modifier_unless expr_value",
    "stmt : stmt modifier_while expr_value",
    "stmt : stmt modifier_until expr_value",
    "stmt : stmt modifier_rescue after_rescue stmt",
    "stmt : k_END allow_exits '{' compstmt '}'",
    "stmt : command_asgn",
    "stmt : mlhs '=' lex_ctxt command_call_value",
    "stmt : lhs '=' lex_ctxt mrhs",
    "stmt : mlhs '=' lex_ctxt mrhs_arg modifier_rescue after_rescue stmt",
    "stmt : mlhs '=' lex_ctxt mrhs_arg",
    "stmt : expr",
    "stmt : error",
    "command_asgn : lhs '=' lex_ctxt command_rhs",
    "command_asgn : var_lhs tOP_ASGN lex_ctxt command_rhs",
    "command_asgn : primary_value '[' opt_call_args rbracket tOP_ASGN lex_ctxt command_rhs",
    "command_asgn : primary_value call_op tIDENTIFIER tOP_ASGN lex_ctxt command_rhs",
    "command_asgn : primary_value call_op tCONSTANT tOP_ASGN lex_ctxt command_rhs",
    "command_asgn : primary_value tCOLON2 tIDENTIFIER tOP_ASGN lex_ctxt command_rhs",
    "command_asgn : primary_value tCOLON2 tCONSTANT tOP_ASGN lex_ctxt command_rhs",
    "command_asgn : tCOLON3 tCONSTANT tOP_ASGN lex_ctxt command_rhs",
    "command_asgn : backref tOP_ASGN lex_ctxt command_rhs",
    "command_asgn : defn_head f_opt_paren_args '=' endless_command",
    "command_asgn : defs_head f_opt_paren_args '=' endless_command",
    "endless_command : command",
    "endless_command : endless_command modifier_rescue after_rescue arg",
    "endless_command : keyword_not opt_nl endless_command",
    "command_rhs : command_call_value",
    "command_rhs : command_call_value modifier_rescue after_rescue stmt",
    "command_rhs : command_asgn",
    "expr : command_call",
    "expr : expr keyword_and expr",
    "expr : expr keyword_or expr",
    "expr : keyword_not opt_nl expr",
    "expr : '!' command_call",
    "$$7 :",
    "expr : arg tASSOC $$7 p_in_kwarg p_pvtbl p_pktbl p_top_expr_body",
    "$$8 :",
    "expr : arg keyword_in $$8 p_in_kwarg p_pvtbl p_pktbl p_top_expr_body",
    "expr : arg",
    "def_name : fname",
    "defn_head : k_def def_name",
    "$$9 :",
    "defs_head : k_def singleton dot_or_colon $$9 def_name",
    "expr_value : expr",
    "expr_value : error",
    "$$10 :",
    "$$11 :",
    "expr_value_do : $$10 expr_value do $$11",
    "command_call : command",
    "command_call : block_command",
    "command_call_value : command_call",
    "block_command : block_call",
    "block_command : block_call call_op2 operation2 command_args",
    "cmd_brace_block : tLBRACE_ARG brace_body '}'",
    "fcall : operation",
    "command : fcall command_args",
    "command : fcall command_args cmd_brace_block",
    "command : primary_value call_op operation2 command_args",
    "command : primary_value call_op operation2 command_args cmd_brace_block",
    "command : primary_value tCOLON2 operation2 command_args",
    "command : primary_value tCOLON2 operation2 command_args cmd_brace_block",
    "command : primary_value tCOLON2 tCONSTANT '{' brace_body '}'",
    "command : keyword_super command_args",
    "command : k_yield command_args",
    "command : k_return call_args",
    "command : keyword_break call_args",
    "command : keyword_next call_args",
    "mlhs : mlhs_basic",
    "mlhs : tLPAREN mlhs_inner rparen",
    "mlhs_inner : mlhs_basic",
    "mlhs_inner : tLPAREN mlhs_inner rparen",
    "mlhs_basic : mlhs_head",
    "mlhs_basic : mlhs_head mlhs_item",
    "mlhs_basic : mlhs_head tSTAR mlhs_node",
    "mlhs_basic : mlhs_head tSTAR mlhs_node ',' mlhs_post",
    "mlhs_basic : mlhs_head tSTAR",
    "mlhs_basic : mlhs_head tSTAR ',' mlhs_post",
    "mlhs_basic : tSTAR mlhs_node",
    "mlhs_basic : tSTAR mlhs_node ',' mlhs_post",
    "mlhs_basic : tSTAR",
    "mlhs_basic : tSTAR ',' mlhs_post",
    "mlhs_item : mlhs_node",
    "mlhs_item : tLPAREN mlhs_inner rparen",
    "mlhs_head : mlhs_item ','",
    "mlhs_head : mlhs_head mlhs_item ','",
    "mlhs_post : mlhs_item",
    "mlhs_post : mlhs_post ',' mlhs_item",
    "mlhs_node : tIDENTIFIER",
    "mlhs_node : tIVAR",
    "mlhs_node : tGVAR",
    "mlhs_node : tCONSTANT",
    "mlhs_node : tCVAR",
    "mlhs_node : keyword_nil",
    "mlhs_node : keyword_self",
    "mlhs_node : keyword_true",
    "mlhs_node : keyword_false",
    "mlhs_node : keyword__FILE__",
    "mlhs_node : keyword__LINE__",
    "mlhs_node : keyword__ENCODING__",
    "mlhs_node : primary_value '[' opt_call_args rbracket",
    "mlhs_node : primary_value call_op tIDENTIFIER",
    "mlhs_node : primary_value tCOLON2 tIDENTIFIER",
    "mlhs_node : primary_value call_op tCONSTANT",
    "mlhs_node : primary_value tCOLON2 tCONSTANT",
    "mlhs_node : tCOLON3 tCONSTANT",
    "mlhs_node : backref",
    "lhs : tIDENTIFIER",
    "lhs : tIVAR",
    "lhs : tGVAR",
    "lhs : tCONSTANT",
    "lhs : tCVAR",
    "lhs : keyword_nil",
    "lhs : keyword_self",
    "lhs : keyword_true",
    "lhs : keyword_false",
    "lhs : keyword__FILE__",
    "lhs : keyword__LINE__",
    "lhs : keyword__ENCODING__",
    "lhs : primary_value '[' opt_call_args rbracket",
    "lhs : primary_value call_op tIDENTIFIER",
    "lhs : primary_value tCOLON2 tIDENTIFIER",
    "lhs : primary_value call_op tCONSTANT",
    "lhs : primary_value tCOLON2 tCONSTANT",
    "lhs : tCOLON3 tCONSTANT",
    "lhs : backref",
    "cname : tIDENTIFIER",
    "cname : tCONSTANT",
    "cpath : tCOLON3 cname",
    "cpath : cname",
    "cpath : primary_value tCOLON2 cname",
    "fname : tIDENTIFIER",
    "fname : tCONSTANT",
    "fname : tFID",
    "fname : op",
    "fname : reswords",
    "fitem : fname",
    "fitem : symbol",
    "undef_list : fitem",
    "$$12 :",
    "undef_list : undef_list ',' $$12 fitem",
    "op : '|'",
    "op : '^'",
    "op : '&'",
    "op : tCMP",
    "op : tEQ",
    "op : tEQQ",
    "op : tMATCH",
    "op : tNMATCH",
    "op : '>'",
    "op : tGEQ",
    "op : '<'",
    "op : tLEQ",
    "op : tNEQ",
    "op : tLSHFT",
    "op : tRSHFT",
    "op : '+'",
    "op : '-'",
    "op : '*'",
    "op : tSTAR",
    "op : '/'",
    "op : '%'",
    "op : tPOW",
    "op : tDSTAR",
    "op : '!'",
    "op : '~'",
    "op : tUPLUS",
    "op : tUMINUS",
    "op : tAREF",
    "op : tASET",
    "op : '`'",
    "reswords : keyword__LINE__",
    "reswords : keyword__FILE__",
    "reswords : keyword__ENCODING__",
    "reswords : keyword_BEGIN",
    "reswords : keyword_END",
    "reswords : keyword_alias",
    "reswords : keyword_and",
    "reswords : keyword_begin",
    "reswords : keyword_break",
    "reswords : keyword_case",
    "reswords : keyword_class",
    "reswords : keyword_def",
    "reswords : keyword_defined",
    "reswords : keyword_do",
    "reswords : keyword_else",
    "reswords : keyword_elsif",
    "reswords : keyword_end",
    "reswords : keyword_ensure",
    "reswords : keyword_false",
    "reswords : keyword_for",
    "reswords : keyword_in",
    "reswords : keyword_module",
    "reswords : keyword_next",
    "reswords : keyword_nil",
    "reswords : keyword_not",
    "reswords : keyword_or",
    "reswords : keyword_redo",
    "reswords : keyword_rescue",
    "reswords : keyword_retry",
    "reswords : keyword_return",
    "reswords : keyword_self",
    "reswords : keyword_super",
    "reswords : keyword_then",
    "reswords : keyword_true",
    "reswords : keyword_undef",
    "reswords : keyword_when",
    "reswords : keyword_yield",
    "reswords : keyword_if",
    "reswords : keyword_unless",
    "reswords : keyword_while",
    "reswords : keyword_until",
    "arg : lhs '=' lex_ctxt arg_rhs",
    "arg : var_lhs tOP_ASGN lex_ctxt arg_rhs",
    "arg : primary_value '[' opt_call_args rbracket tOP_ASGN lex_ctxt arg_rhs",
    "arg : primary_value call_op tIDENTIFIER tOP_ASGN lex_ctxt arg_rhs",
    "arg : primary_value call_op tCONSTANT tOP_ASGN lex_ctxt arg_rhs",
    "arg : primary_value tCOLON2 tIDENTIFIER tOP_ASGN lex_ctxt arg_rhs",
    "arg : primary_value tCOLON2 tCONSTANT tOP_ASGN lex_ctxt arg_rhs",
    "arg : tCOLON3 tCONSTANT tOP_ASGN lex_ctxt arg_rhs",
    "arg : backref tOP_ASGN lex_ctxt arg_rhs",
    "arg : arg tDOT2 arg",
    "arg : arg tDOT3 arg",
    "arg : arg tDOT2",
    "arg : arg tDOT3",
    "arg : tBDOT2 arg",
    "arg : tBDOT3 arg",
    "arg : arg '+' arg",
    "arg : arg '-' arg",
    "arg : arg '*' arg",
    "arg : arg '/' arg",
    "arg : arg '%' arg",
    "arg : arg tPOW arg",
    "arg : tUMINUS_NUM simple_numeric tPOW arg",
    "arg : tUPLUS arg",
    "arg : tUMINUS arg",
    "arg : arg '|' arg",
    "arg : arg '^' arg",
    "arg : arg '&' arg",
    "arg : arg tCMP arg",
    "arg : rel_expr",
    "arg : arg tEQ arg",
    "arg : arg tEQQ arg",
    "arg : arg tNEQ arg",
    "arg : arg tMATCH arg",
    "arg : arg tNMATCH arg",
    "arg : '!' arg",
    "arg : '~' arg",
    "arg : arg tLSHFT arg",
    "arg : arg tRSHFT arg",
    "arg : arg tANDOP arg",
    "arg : arg tOROP arg",
    "arg : keyword_defined opt_nl begin_defined arg",
    "arg : defn_head f_opt_paren_args '=' endless_arg",
    "arg : defs_head f_opt_paren_args '=' endless_arg",
    "arg : arg '?' arg opt_nl ':' arg",
    "arg : primary",
    "endless_arg : arg",
    "endless_arg : endless_arg modifier_rescue after_rescue arg",
    "endless_arg : keyword_not opt_nl endless_arg",
    "relop : '>'",
    "relop : '<'",
    "relop : tGEQ",
    "relop : tLEQ",
    "rel_expr : arg relop arg",
    "rel_expr : rel_expr relop arg",
    "lex_ctxt : none",
    "begin_defined : lex_ctxt",
    "after_rescue : lex_ctxt",
    "arg_value : arg",
    "aref_args : none",
    "aref_args : args trailer",
    "aref_args : args ',' assocs trailer",
    "aref_args : assocs trailer",
    "arg_rhs : arg",
    "arg_rhs : arg modifier_rescue after_rescue arg",
    "paren_args : '(' opt_call_args rparen",
    "paren_args : '(' args ',' args_forward rparen",
    "paren_args : '(' args_forward rparen",
    "opt_paren_args : none",
    "opt_paren_args : paren_args",
    "opt_call_args : none",
    "opt_call_args : call_args",
    "opt_call_args : args ','",
    "opt_call_args : args ',' assocs ','",
    "opt_call_args : assocs ','",
    "call_args : command",
    "call_args : defn_head f_opt_paren_args '=' endless_command",
    "call_args : defs_head f_opt_paren_args '=' endless_command",
    "call_args : args opt_block_arg",
    "call_args : assocs opt_block_arg",
    "call_args : args ',' assocs opt_block_arg",
    "call_args : block_arg",
    "$$13 :",
    "command_args : $$13 call_args",
    "block_arg : tAMPER arg_value",
    "block_arg : tAMPER",
    "opt_block_arg : ',' block_arg",
    "opt_block_arg : none_block_pass",
    "args : arg_value",
    "args : arg_splat",
    "args : args ',' arg_value",
    "args : args ',' arg_splat",
    "arg_splat : tSTAR arg_value",
    "arg_splat : tSTAR",
    "mrhs_arg : mrhs",
    "mrhs_arg : arg_value",
    "mrhs : args ',' arg_value",
    "mrhs : args ',' tSTAR arg_value",
    "mrhs : tSTAR arg_value",
    "primary : literal",
    "primary : strings",
    "primary : xstring",
    "primary : regexp",
    "primary : words",
    "primary : qwords",
    "primary : symbols",
    "primary : qsymbols",
    "primary : var_ref",
    "primary : backref",
    "primary : tFID",
    "$$14 :",
    "primary : k_begin $$14 bodystmt k_end",
    "$$15 :",
    "primary : tLPAREN_ARG compstmt $$15 ')'",
    "primary : tLPAREN compstmt ')'",
    "primary : primary_value tCOLON2 tCONSTANT",
    "primary : tCOLON3 tCONSTANT",
    "primary : tLBRACK aref_args ']'",
    "primary : tLBRACE assoc_list '}'",
    "primary : k_return",
    "primary : k_yield '(' call_args rparen",
    "primary : k_yield '(' rparen",
    "primary : k_yield",
    "primary : keyword_defined opt_nl '(' begin_defined expr rparen",
    "primary : keyword_not '(' expr rparen",
    "primary : keyword_not '(' rparen",
    "primary : fcall brace_block",
    "primary : method_call",
    "primary : method_call brace_block",
    "primary : lambda",
    "primary : k_if expr_value then compstmt if_tail k_end",
    "primary : k_unless expr_value then compstmt opt_else k_end",
    "primary : k_while expr_value_do compstmt k_end",
    "primary : k_until expr_value_do compstmt k_end",
    "$$16 :",
    "primary : k_case expr_value opt_terms $$16 case_body k_end",
    "$$17 :",
    "primary : k_case opt_terms $$17 case_body k_end",
    "primary : k_case expr_value opt_terms p_case_body k_end",
    "primary : k_for for_var keyword_in expr_value_do compstmt k_end",
    "$$18 :",
    "primary : k_class cpath superclass $$18 bodystmt k_end",
    "$$19 :",
    "primary : k_class tLSHFT expr_value $$19 term bodystmt k_end",
    "$$20 :",
    "primary : k_module cpath $$20 bodystmt k_end",
    "$$21 :",
    "primary : defn_head f_arglist $$21 bodystmt k_end",
    "$$22 :",
    "primary : defs_head f_arglist $$22 bodystmt k_end",
    "primary : keyword_break",
    "primary : keyword_next",
    "primary : keyword_redo",
    "primary : keyword_retry",
    "primary_value : primary",
    "k_begin : keyword_begin",
    "k_if : keyword_if",
    "k_unless : keyword_unless",
    "k_while : keyword_while allow_exits",
    "k_until : keyword_until allow_exits",
    "k_case : keyword_case",
    "k_for : keyword_for allow_exits",
    "k_class : keyword_class",
    "k_module : keyword_module",
    "k_def : keyword_def",
    "k_do : keyword_do",
    "k_do_block : keyword_do_block",
    "k_rescue : keyword_rescue",
    "k_ensure : keyword_ensure",
    "k_when : keyword_when",
    "k_else : keyword_else",
    "k_elsif : keyword_elsif",
    "k_end : keyword_end",
    "k_end : tDUMNY_END",
    "k_return : keyword_return",
    "k_yield : keyword_yield",
    "then : term",
    "then : keyword_then",
    "then : term keyword_then",
    "do : term",
    "do : keyword_do_cond",
    "if_tail : opt_else",
    "if_tail : k_elsif expr_value then compstmt if_tail",
    "opt_else : none",
    "opt_else : k_else compstmt",
    "for_var : lhs",
    "for_var : mlhs",
    "f_marg : f_norm_arg",
    "f_marg : tLPAREN f_margs rparen",
    "f_marg_list : f_marg",
    "f_marg_list : f_marg_list ',' f_marg",
    "f_margs : f_marg_list",
    "f_margs : f_marg_list ',' f_rest_marg",
    "f_margs : f_marg_list ',' f_rest_marg ',' f_marg_list",
    "f_margs : f_rest_marg",
    "f_margs : f_rest_marg ',' f_marg_list",
    "f_rest_marg : tSTAR f_norm_arg",
    "f_rest_marg : tSTAR",
    "f_any_kwrest : f_kwrest",
    "f_any_kwrest : f_no_kwarg",
    "$$23 :",
    "f_eq : $$23 '='",
    "block_args_tail : f_block_kwarg ',' f_kwrest opt_f_block_arg",
    "block_args_tail : f_block_kwarg opt_f_block_arg",
    "block_args_tail : f_any_kwrest opt_f_block_arg",
    "block_args_tail : f_block_arg",
    "opt_block_args_tail : ',' block_args_tail",
    "opt_block_args_tail :",
    "excessed_comma : ','",
    "block_param : f_arg ',' f_block_optarg ',' f_rest_arg opt_block_args_tail",
    "block_param : f_arg ',' f_block_optarg ',' f_rest_arg ',' f_arg opt_block_args_tail",
    "block_param : f_arg ',' f_block_optarg opt_block_args_tail",
    "block_param : f_arg ',' f_block_optarg ',' f_arg opt_block_args_tail",
    "block_param : f_arg ',' f_rest_arg opt_block_args_tail",
    "block_param : f_arg excessed_comma",
    "block_param : f_arg ',' f_rest_arg ',' f_arg opt_block_args_tail",
    "block_param : f_arg opt_block_args_tail",
    "block_param : f_block_optarg ',' f_rest_arg opt_block_args_tail",
    "block_param : f_block_optarg ',' f_rest_arg ',' f_arg opt_block_args_tail",
    "block_param : f_block_optarg opt_block_args_tail",
    "block_param : f_block_optarg ',' f_arg opt_block_args_tail",
    "block_param : f_rest_arg opt_block_args_tail",
    "block_param : f_rest_arg ',' f_arg opt_block_args_tail",
    "block_param : block_args_tail",
    "opt_block_param : none",
    "opt_block_param : block_param_def",
    "block_param_def : '|' opt_bv_decl '|'",
    "block_param_def : '|' block_param opt_bv_decl '|'",
    "opt_bv_decl : opt_nl",
    "opt_bv_decl : opt_nl ';' bv_decls opt_nl",
    "bv_decls : bvar",
    "bv_decls : bv_decls ',' bvar",
    "bvar : tIDENTIFIER",
    "bvar : f_bad_arg",
    "max_numparam :",
    "numparam :",
    "it_id :",
    "$$24 :",
    "$$25 :",
    "lambda : tLAMBDA $$24 max_numparam numparam it_id allow_exits f_larglist $$25 lambda_body",
    "f_larglist : '(' f_args opt_bv_decl ')'",
    "f_larglist : f_args",
    "lambda_body : tLAMBEG compstmt '}'",
    "$$26 :",
    "lambda_body : keyword_do_LAMBDA $$26 bodystmt k_end",
    "do_block : k_do_block do_body k_end",
    "block_call : command do_block",
    "block_call : block_call call_op2 operation2 opt_paren_args",
    "block_call : block_call call_op2 operation2 opt_paren_args brace_block",
    "block_call : block_call call_op2 operation2 command_args do_block",
    "block_call : block_call call_op2 paren_args",
    "method_call : fcall paren_args",
    "method_call : primary_value call_op operation2 opt_paren_args",
    "method_call : primary_value tCOLON2 operation2 paren_args",
    "method_call : primary_value tCOLON2 operation3",
    "method_call : primary_value call_op paren_args",
    "method_call : primary_value tCOLON2 paren_args",
    "method_call : keyword_super paren_args",
    "method_call : keyword_super",
    "method_call : primary_value '[' opt_call_args rbracket",
    "brace_block : '{' brace_body '}'",
    "brace_block : k_do do_body k_end",
    "$$27 :",
    "brace_body : $$27 max_numparam numparam it_id allow_exits opt_block_param compstmt",
    "$$28 :",
    "do_body : $$28 max_numparam numparam it_id allow_exits opt_block_param bodystmt",
    "case_args : arg_value",
    "case_args : tSTAR arg_value",
    "case_args : case_args ',' arg_value",
    "case_args : case_args ',' tSTAR arg_value",
    "case_body : k_when case_args then compstmt cases",
    "cases : opt_else",
    "cases : case_body",
    "p_pvtbl :",
    "p_pktbl :",
    "p_in_kwarg :",
    "$$29 :",
    "p_case_body : keyword_in p_in_kwarg p_pvtbl p_pktbl p_top_expr then $$29 compstmt p_cases",
    "p_cases : opt_else",
    "p_cases : p_case_body",
    "p_top_expr : p_top_expr_body",
    "p_top_expr : p_top_expr_body modifier_if expr_value",
    "p_top_expr : p_top_expr_body modifier_unless expr_value",
    "p_top_expr_body : p_expr",
    "p_top_expr_body : p_expr ','",
    "p_top_expr_body : p_expr ',' p_args",
    "p_top_expr_body : p_find",
    "p_top_expr_body : p_args_tail",
    "p_top_expr_body : p_kwargs",
    "p_expr : p_as",
    "p_as : p_expr tASSOC p_variable",
    "p_as : p_alt",
    "$$30 :",
    "p_alt : p_alt '|' $$30 p_expr_basic",
    "p_alt : p_expr_basic",
    "p_lparen : '(' p_pktbl",
    "p_lbracket : '[' p_pktbl",
    "p_expr_basic : p_value",
    "p_expr_basic : p_variable",
    "p_expr_basic : p_const p_lparen p_args rparen",
    "p_expr_basic : p_const p_lparen p_find rparen",
    "p_expr_basic : p_const p_lparen p_kwargs rparen",
    "p_expr_basic : p_const '(' rparen",
    "p_expr_basic : p_const p_lbracket p_args rbracket",
    "p_expr_basic : p_const p_lbracket p_find rbracket",
    "p_expr_basic : p_const p_lbracket p_kwargs rbracket",
    "p_expr_basic : p_const '[' rbracket",
    "p_expr_basic : tLBRACK p_args rbracket",
    "p_expr_basic : tLBRACK p_find rbracket",
    "p_expr_basic : tLBRACK rbracket",
    "$$31 :",
    "p_expr_basic : tLBRACE p_pktbl lex_ctxt $$31 p_kwargs rbrace",
    "p_expr_basic : tLBRACE rbrace",
    "p_expr_basic : tLPAREN p_pktbl p_expr rparen",
    "p_args : p_expr",
    "p_args : p_args_head",
    "p_args : p_args_head p_arg",
    "p_args : p_args_head p_rest",
    "p_args : p_args_head p_rest ',' p_args_post",
    "p_args : p_args_tail",
    "p_args_head : p_arg ','",
    "p_args_head : p_args_head p_arg ','",
    "p_args_tail : p_rest",
    "p_args_tail : p_rest ',' p_args_post",
    "p_find : p_rest ',' p_args_post ',' p_rest",
    "p_rest : tSTAR tIDENTIFIER",
    "p_rest : tSTAR",
    "p_args_post : p_arg",
    "p_args_post : p_args_post ',' p_arg",
    "p_arg : p_expr",
    "p_kwargs : p_kwarg ',' p_any_kwrest",
    "p_kwargs : p_kwarg",
    "p_kwargs : p_kwarg ','",
    "p_kwargs : p_any_kwrest",
    "p_kwarg : p_kw",
    "p_kwarg : p_kwarg ',' p_kw",
    "p_kw : p_kw_label p_expr",
    "p_kw : p_kw_label",
    "p_kw_label : tLABEL",
    "p_kw_label : tSTRING_BEG string_contents tLABEL_END",
    "p_kwrest : kwrest_mark tIDENTIFIER",
    "p_kwrest : kwrest_mark",
    "p_kwnorest : kwrest_mark keyword_nil",
    "p_any_kwrest : p_kwrest",
    "p_any_kwrest : p_kwnorest",
    "p_value : p_primitive",
    "p_value : p_primitive tDOT2 p_primitive",
    "p_value : p_primitive tDOT3 p_primitive",
    "p_value : p_primitive tDOT2",
    "p_value : p_primitive tDOT3",
    "p_value : p_var_ref",
    "p_value : p_expr_ref",
    "p_value : p_const",
    "p_value : tBDOT2 p_primitive",
    "p_value : tBDOT3 p_primitive",
    "p_primitive : literal",
    "p_primitive : strings",
    "p_primitive : xstring",
    "p_primitive : regexp",
    "p_primitive : words",
    "p_primitive : qwords",
    "p_primitive : symbols",
    "p_primitive : qsymbols",
    "p_primitive : keyword_nil",
    "p_primitive : keyword_self",
    "p_primitive : keyword_true",
    "p_primitive : keyword_false",
    "p_primitive : keyword__FILE__",
    "p_primitive : keyword__LINE__",
    "p_primitive : keyword__ENCODING__",
    "p_primitive : lambda",
    "p_variable : tIDENTIFIER",
    "p_var_ref : '^' tIDENTIFIER",
    "p_var_ref : '^' nonlocal_var",
    "p_expr_ref : '^' tLPAREN expr_value rparen",
    "p_const : tCOLON3 cname",
    "p_const : p_const tCOLON2 cname",
    "p_const : tCONSTANT",
    "opt_rescue : k_rescue exc_list exc_var then compstmt opt_rescue",
    "opt_rescue : none",
    "exc_list : arg_value",
    "exc_list : mrhs",
    "exc_list : none",
    "exc_var : tASSOC lhs",
    "exc_var : none",
    "opt_ensure : k_ensure compstmt",
    "opt_ensure : none",
    "literal : numeric",
    "literal : symbol",
    "strings : string",
    "string : tCHAR",
    "string : string1",
    "string : string string1",
    "string1 : tSTRING_BEG string_contents tSTRING_END",
    "xstring : tXSTRING_BEG xstring_contents tSTRING_END",
    "regexp : tREGEXP_BEG regexp_contents tREGEXP_END",
    "words_sep : ' '",
    "words_sep : words_sep ' '",
    "words : tWORDS_BEG words_sep word_list tSTRING_END",
    "word_list :",
    "word_list : word_list word words_sep",
    "word : string_content",
    "word : word string_content",
    "symbols : tSYMBOLS_BEG words_sep symbol_list tSTRING_END",
    "symbol_list :",
    "symbol_list : symbol_list word words_sep",
    "qwords : tQWORDS_BEG words_sep qword_list tSTRING_END",
    "qsymbols : tQSYMBOLS_BEG words_sep qsym_list tSTRING_END",
    "qword_list :",
    "qword_list : qword_list tSTRING_CONTENT words_sep",
    "qsym_list :",
    "qsym_list : qsym_list tSTRING_CONTENT words_sep",
    "string_contents :",
    "string_contents : string_contents string_content",
    "xstring_contents :",
    "xstring_contents : xstring_contents string_content",
    "regexp_contents :",
    "regexp_contents : regexp_contents string_content",
    "string_content : tSTRING_CONTENT",
    "$$32 :",
    "string_content : tSTRING_DVAR $$32 string_dvar",
    "$$33 :",
    "$$34 :",
    "$$35 :",
    "$$36 :",
    "string_content : tSTRING_DBEG $$33 $$34 $$35 $$36 compstmt string_dend",
    "string_dend : tSTRING_DEND",
    "string_dend : END_OF_INPUT",
    "string_dvar : nonlocal_var",
    "string_dvar : backref",
    "symbol : ssym",
    "symbol : dsym",
    "ssym : tSYMBEG sym",
    "sym : fname",
    "sym : nonlocal_var",
    "dsym : tSYMBEG string_contents tSTRING_END",
    "numeric : simple_numeric",
    "numeric : tUMINUS_NUM simple_numeric",
    "simple_numeric : tINTEGER",
    "simple_numeric : tFLOAT",
    "simple_numeric : tRATIONAL",
    "simple_numeric : tIMAGINARY",
    "nonlocal_var : tIVAR",
    "nonlocal_var : tGVAR",
    "nonlocal_var : tCVAR",
    "var_ref : tIDENTIFIER",
    "var_ref : tIVAR",
    "var_ref : tGVAR",
    "var_ref : tCONSTANT",
    "var_ref : tCVAR",
    "var_ref : keyword_nil",
    "var_ref : keyword_self",
    "var_ref : keyword_true",
    "var_ref : keyword_false",
    "var_ref : keyword__FILE__",
    "var_ref : keyword__LINE__",
    "var_ref : keyword__ENCODING__",
    "var_lhs : tIDENTIFIER",
    "var_lhs : tIVAR",
    "var_lhs : tGVAR",
    "var_lhs : tCONSTANT",
    "var_lhs : tCVAR",
    "var_lhs : keyword_nil",
    "var_lhs : keyword_self",
    "var_lhs : keyword_true",
    "var_lhs : keyword_false",
    "var_lhs : keyword__FILE__",
    "var_lhs : keyword__LINE__",
    "var_lhs : keyword__ENCODING__",
    "backref : tNTH_REF",
    "backref : tBACK_REF",
    "$$37 :",
    "superclass : '<' $$37 expr_value term",
    "superclass :",
    "f_opt_paren_args : f_paren_args",
    "f_opt_paren_args : none",
    "f_paren_args : '(' f_args rparen",
    "f_arglist : f_paren_args",
    "$$38 :",
    "f_arglist : $$38 f_args term",
    "args_tail : f_kwarg ',' f_kwrest opt_f_block_arg",
    "args_tail : f_kwarg opt_f_block_arg",
    "args_tail : f_any_kwrest opt_f_block_arg",
    "args_tail : f_block_arg",
    "args_tail : args_forward",
    "opt_args_tail : ',' args_tail",
    "opt_args_tail :",
    "f_args : f_arg ',' f_optarg ',' f_rest_arg opt_args_tail",
    "f_args : f_arg ',' f_optarg ',' f_rest_arg ',' f_arg opt_args_tail",
    "f_args : f_arg ',' f_optarg opt_args_tail",
    "f_args : f_arg ',' f_optarg ',' f_arg opt_args_tail",
    "f_args : f_arg ',' f_rest_arg opt_args_tail",
    "f_args : f_arg ',' f_rest_arg ',' f_arg opt_args_tail",
    "f_args : f_arg opt_args_tail",
    "f_args : f_optarg ',' f_rest_arg opt_args_tail",
    "f_args : f_optarg ',' f_rest_arg ',' f_arg opt_args_tail",
    "f_args : f_optarg opt_args_tail",
    "f_args : f_optarg ',' f_arg opt_args_tail",
    "f_args : f_rest_arg opt_args_tail",
    "f_args : f_rest_arg ',' f_arg opt_args_tail",
    "f_args : args_tail",
    "f_args :",
    "args_forward : tBDOT3",
    "f_bad_arg : tCONSTANT",
    "f_bad_arg : tIVAR",
    "f_bad_arg : tGVAR",
    "f_bad_arg : tCVAR",
    "f_norm_arg : f_bad_arg",
    "f_norm_arg : tIDENTIFIER",
    "f_arg_asgn : f_norm_arg",
    "f_arg_item : f_arg_asgn",
    "f_arg_item : tLPAREN f_margs rparen",
    "f_arg : f_arg_item",
    "f_arg : f_arg ',' f_arg_item",
    "f_label : tLABEL",
    "f_kw : f_label arg_value",
    "f_kw : f_label",
    "f_block_kw : f_label primary_value",
    "f_block_kw : f_label",
    "f_block_kwarg : f_block_kw",
    "f_block_kwarg : f_block_kwarg ',' f_block_kw",
    "f_kwarg : f_kw",
    "f_kwarg : f_kwarg ',' f_kw",
    "kwrest_mark : tPOW",
    "kwrest_mark : tDSTAR",
    "f_no_kwarg : p_kwnorest",
    "f_kwrest : kwrest_mark tIDENTIFIER",
    "f_kwrest : kwrest_mark",
    "f_opt : f_arg_asgn f_eq arg_value",
    "f_block_opt : f_arg_asgn f_eq primary_value",
    "f_block_optarg : f_block_opt",
    "f_block_optarg : f_block_optarg ',' f_block_opt",
    "f_optarg : f_opt",
    "f_optarg : f_optarg ',' f_opt",
    "restarg_mark : '*'",
    "restarg_mark : tSTAR",
    "f_rest_arg : restarg_mark tIDENTIFIER",
    "f_rest_arg : restarg_mark",
    "blkarg_mark : '&'",
    "blkarg_mark : tAMPER",
    "f_block_arg : blkarg_mark tIDENTIFIER",
    "f_block_arg : blkarg_mark",
    "opt_f_block_arg : ',' f_block_arg",
    "opt_f_block_arg :",
    "singleton : var_ref",
    "$$39 :",
    "singleton : '(' $$39 expr rparen",
    "assoc_list : none",
    "assoc_list : assocs trailer",
    "assocs : assoc",
    "assocs : assocs ',' assoc",
    "assoc : arg_value tASSOC arg_value",
    "assoc : tLABEL arg_value",
    "assoc : tLABEL",
    "assoc : tSTRING_BEG string_contents tLABEL_END arg_value",
    "assoc : tDSTAR arg_value",
    "assoc : tDSTAR",
    "operation : tIDENTIFIER",
    "operation : tCONSTANT",
    "operation : tFID",
    "operation2 : operation",
    "operation2 : op",
    "operation3 : tIDENTIFIER",
    "operation3 : tFID",
    "operation3 : op",
    "dot_or_colon : '.'",
    "dot_or_colon : tCOLON2",
    "call_op : '.'",
    "call_op : tANDDOT",
    "call_op2 : call_op",
    "call_op2 : tCOLON2",
    "opt_terms :",
    "opt_terms : terms",
    "opt_nl :",
    "opt_nl : '\\n'",
    "rparen : opt_nl ')'",
    "rbracket : opt_nl ']'",
    "rbrace : opt_nl '}'",
    "trailer :",
    "trailer : '\\n'",
    "trailer : ','",
    "term : ';'",
    "term : '\\n'",
    "terms : term",
    "terms : terms ';'",
    "none :",
    "none_block_pass :",
    };

  protected org.jruby.parser.YYDebug yydebug;

  /** index-checked interface to {@link #yyNames}.
      @param token single character or <code>%token</code> value.
      @return token name or <code>[illegal]</code> or <code>[unknown]</code>.
    */
  public static String yyName (int token) {
    if (token < 0 || token > yyNames.length) return "[illegal]";
    String name;
    if ((name = yyNames[token]) != null) return name;
    return "[unknown]";
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
      @param ayydebug debug message writer implementing <code>yyDebug</code>, or <code>null</code>.
      @return result of the last reduction, if any.
    */
  public Object yyparse (RubyLexer yyLex, Object ayydebug)
				throws java.io.IOException {
    this.yydebug = (org.jruby.parser.YYDebug) ayydebug;
    return yyparse(yyLex);
  }

  private static void initializeStates(ProductionState[] states, int start, int length) {
      for (int i = 0; i < length; i++) {
          states[start + i] = new ProductionState();
      }
  }

  private static void printstates(int yytop, ProductionState[] yystates) {
     for (int i = 0; i <= yytop; i++) {
         System.out.println("yytop: " + i + ", S/E: " +
             ProductionState.column(yystates[i].start) + "/" +
             ProductionState.column(yystates[i].end) +
             yystates[i].value);
     }
  }

  private static final int NEEDS_TOKEN = -1;
  private static final int DEFAULT = 0;
  private static final int YYMAX = 256;

  /** the generated parser.
      Maintains a dynamic state and value stack.
      @param yyLex scanner.
      @return result of the last reduction, if any.
    */
  public Object yyparse (RubyLexer yyLex) throws java.io.IOException {
    int yystate = 0;
    Object yyVal = null;
    ByteList id = null;
    ProductionState[] yystates = new ProductionState[YYMAX];        // stack of states and values.
    initializeStates(yystates, 0, yystates.length);
    int yytoken = NEEDS_TOKEN;     // current token
    int yyErrorFlag = 0;           // #tokens to shift
    long start = 0;
    long end = 0;

    yyLoop: for (int yytop = 0;; yytop++) {
      if (yytop + 1 >= yystates.length) {			// dynamically increase
          ProductionState[] newStates = new ProductionState[yystates.length+YYMAX];
          System.arraycopy(yystates, 0, newStates, 0, yystates.length);
          initializeStates(newStates, yystates.length, newStates.length - yystates.length);
          yystates = newStates;
      }

      yystates[yytop].state = yystate;
      yystates[yytop].value = yyVal;
      yystates[yytop].id = id;
      yystates[yytop].start = start;
      yystates[yytop].end = end;
   //         printstates(yytop, yystates);

      if (yydebug != null) yydebug.push(yystate, yyVal);

      yyDiscarded: for (;;) {	// discarding a token does not change stack
        int yyn = yyDefRed[yystate];
        if (yyn == DEFAULT) {	//ja else [default] reduce (yyn)
            if (yytoken == NEEDS_TOKEN) {
                yytoken = yyLex.nextToken();
                if (yydebug != null) yydebug.lex(yystate, yytoken, yyName(yytoken), yyLex.value());
            }

            yyn = yySindex[yystate];
            if (yyn != 0 &&
                (yyn += yytoken) >= 0 &&
                yyn < yyTable.length &&
                yyCheck[yyn] == yytoken) {
                if (yydebug != null) yydebug.shift(yystate, yyTable[yyn], yyErrorFlag-1);
                yystate = yyTable[yyn];		// shift to yyn
                yyVal = yyLex.value();
                id = yyLex.id();
                start = yyLex.start;
                end = yyLex.end;
                yytoken = NEEDS_TOKEN;
                if (yyErrorFlag > 0) --yyErrorFlag;
                continue yyLoop;
            }

            yyn = yyRindex[yystate];
            if (yyn != 0 &&
                (yyn += yytoken) >= 0 &&
                 yyn < yyTable.length &&
                 yyCheck[yyn] == yytoken) {
                yyn = yyTable[yyn];			// reduce (yyn)
            } else {
                switch (yyErrorFlag) {
  
                case 0:
                    yyerror("syntax error", yyExpecting(yystate), yyNames[yytoken]);
                    if (yydebug != null) yydebug.error("syntax error");
                    // falls through...
                case 1: case 2:
                    yyErrorFlag = 3;
                    do {
                        yyn = yySindex[yystates[yytop].state];
                        if (yyn != 0 &&
                            (yyn += yyErrorCode) >= 0 &&
                            yyn < yyTable.length &&
                            yyCheck[yyn] == yyErrorCode) {
                            if (yydebug != null) yydebug.shift(yystates[yytop].state, yyTable[yyn], 3);
                            yystate = yyTable[yyn];
                            yyVal = yyLex.value();
                            id = yyLex.id();
                            continue yyLoop;
                        }
                        if (yydebug != null) yydebug.pop(yystates[yytop].state);
                    } while (--yytop >= 0);
                    if (yydebug != null) yydebug.reject();
                    yyerror("irrecoverable syntax error"); // throws
                case 3:
                    if (yytoken == 0) {
                        if (yydebug != null) yydebug.reject();
                        yyerror("irrecoverable syntax error at end-of-file");
                    }
                    if (yydebug != null) yydebug.discard(yystate, yytoken, yyName(yytoken), yyLex.value());
                    yytoken = NEEDS_TOKEN;
                    continue yyDiscarded; // leave stack alone
                }
            }
        }

        if (yydebug != null) yydebug.reduce(yystate, yystates[yytop-yyLen[yyn]].state, yyn, yyRule[yyn], yyLen[yyn]);

        ParserState parserState = yyn >= states.length ? null : states[yyn];
//        ParserState parserState = states[yyn];
        if (parserState == null) {
            yyVal = yyLen[yyn] > 0 ? yystates[yytop - yyLen[yyn] + 1].value : null;
        } else {
            int count = yyLen[yyn];
            // an empty production is located, empty, right after the previous symbol (as bison does)
            start = count > 0 ? yystates[yytop - count + 1].start : yystates[yytop].end;
            end = yystates[yytop].end;
            yyVal = parserState.execute(this, yyVal, yystates, yytop, count, yytoken);
            // record the production's source span on the node it produced (see Node#setAutoSourceSpan: each
            // enclosing production handing the node along widens it until an explicit span locks it; a node
            // made by an empty production has no source of its own), when the parse has a use for it
            if (count > 0 && recordsPositions() && yyVal instanceof org.jruby.ast.Node node) {
                node.setAutoSourceSpan(start, end);
            }
        }
// ACTIONS_END (line used by optimize_parser)
        yytop -= yyLen[yyn];
        yystate = yystates[yytop].state;
        int yyM = yyLhs[yyn];
        if (yystate == 0 && yyM == 0) {
            if (yydebug != null) yydebug.shift(0, yyFinal);
            yystate = yyFinal;
            if (yytoken == NEEDS_TOKEN) {
                yytoken = yyLex.nextToken();
                if (yydebug != null) yydebug.lex(yystate, yytoken,yyName(yytoken), yyLex.value());
            }
            if (yytoken == 0) {
                if (yydebug != null) yydebug.accept(yyVal);
                return yyVal;
            }
            continue yyLoop;
        }
        yyn = yyGindex[yyM];
        if (yyn != 0 &&
            (yyn += yystate) >= 0 &&
            yyn < yyTable.length &&
            yyCheck[yyn] == yystate) {
            yystate = yyTable[yyn];
        } else {
            yystate = yyDgoto[yyM];
        }

        if (yydebug != null) yydebug.shift(yystates[yytop].state, yystate);
        continue yyLoop;
      }
    }
  }

static ParserState<RubyParser>[] states = new ParserState[829];
static {
states[1] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  p.setState(EXPR_BEG);
                  p.initTopLocalVariables();
  return yyVal;
};
states[2] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  /*%%%*/
                  Node expr = ((Node)yyVals[0+yyTop].value);
                  if (expr != null && !p.isEval()) {
                      /* last expression should not be void */
                      if (((Node)yyVals[0+yyTop].value) instanceof BlockNode) {
                        expr = ((BlockNode)yyVals[0+yyTop].value).getLast();
                      } else {
                        expr = ((Node)yyVals[0+yyTop].value);
                      }
                      expr = p.remove_begin(expr);
                      p.void_expr(expr);
                  }
                  p.finalizeDynamicScope();
                  p.getResult().setAST(p.addRootNode(((Node)yyVals[0+yyTop].value)));
                  /*% %*/
                  /*% ripper[final]: program!($2) %*/
  return yyVal;
};
states[3] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  yyVal = p.void_stmts(((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[4] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  /*%%%*/
                  yyVal = null;
                  /*% %*/
                  /*% ripper: stmts_add!(stmts_new!, void_stmt!) %*/
  return yyVal;
};
states[5] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  /*%%%*/
                  yyVal = p.newline_node(((Node)yyVals[0+yyTop].value), yyVals[yyTop - count + 1].start());
                  /*% %*/
                  /*% ripper: stmts_add!(stmts_new!, $1) %*/
  return yyVal;
};
states[6] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  /*%%%*/
                  yyVal = p.appendToBlock(((Node)yyVals[-2+yyTop].value), p.newline_node(((Node)yyVals[0+yyTop].value), yyVals[yyTop - count + 3].start()));
                  /*% %*/
                  /*% ripper: stmts_add!($1, $3) %*/
  return yyVal;
};
states[7] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  p.clear_block_exit(true);
                  yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[8] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[9] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  yyVal = p.init_block_exit();
  return yyVal;
};
states[10] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  p.restore_block_exit(((NodeExits)yyVals[-2+yyTop].value));
                  /*%%%*/
                  p.getResult().addBeginNode(new PreExe19Node(yyVals[yyTop - count + 1].start(), p.getCurrentScope(), ((Node)yyVals[-1+yyTop].value), p.src_line()));
                  /*                  $$ = new BeginNode(yyVals[yyTop - count + 1].start(), p.makeNullNil($2));*/
                  yyVal = null;
                  /*% %*/
                  /*% ripper: BEGIN!($2) %*/
  return yyVal;
};
states[11] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  /*%%%*/
                  if (((RescueBodyNode)yyVals[-1+yyTop].value) == null) p.yyerror("else without rescue is useless");
                  p.next_rescue_context(((LexContext)yyVals[-2+yyTop].value), LexContext.InRescue.AFTER_ELSE);
                  /*% %*/
  return yyVal;
};
states[12] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  p.next_rescue_context(((LexContext)yyVals[-4+yyTop].value), LexContext.InRescue.AFTER_ENSURE);
  return yyVal;
};
states[13] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  /*%%%*/
                   yyVal = p.new_bodystmt(((Node)yyVals[-7+yyTop].value), ((RescueBodyNode)yyVals[-5+yyTop].value), ((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                  /*% %*/
                  /*% ripper: bodystmt!(escape_Qundef($1), escape_Qundef($3), escape_Qundef($6), escape_Qundef($8)) %*/
  return yyVal;
};
states[14] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  p.next_rescue_context(((LexContext)yyVals[-1+yyTop].value), LexContext.InRescue.AFTER_ENSURE);
  return yyVal;
};
states[15] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  /*%%%*/
                   yyVal = p.new_bodystmt(((Node)yyVals[-4+yyTop].value), ((RescueBodyNode)yyVals[-2+yyTop].value), null, ((Node)yyVals[0+yyTop].value));
                  /*% %*/
                  /*% ripper: bodystmt!(escape_Qundef($1), escape_Qundef($3), Qnil, escape_Qundef($5)) %*/
  return yyVal;
};
states[16] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  yyVal = p.void_stmts(((Node)yyVals[-1+yyTop].value));
                  /*%%%*/
                  p.lock_span(((Node)yyVal)); /* the statements' own span, not the terminators after them*/
                  /*% %*/
  return yyVal;
};
states[17] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   /*%%%*/
                   yyVal = null;
                   /*% %*/
                   /*% ripper: stmts_add!(stmts_new!, void_stmt!) %*/
  return yyVal;
};
states[18] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   /*%%%*/
                    yyVal = p.newline_node(((Node)yyVals[0+yyTop].value), yyVals[yyTop - count + 1].start());
                   /*% %*/
                   /*% ripper: stmts_add!(stmts_new!, $1) %*/
  return yyVal;
};
states[19] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   /*%%%*/
                    yyVal = p.appendToBlock(((Node)yyVals[-2+yyTop].value), p.newline_node(((Node)yyVals[0+yyTop].value), yyVals[yyTop - count + 3].start()));
                    p.span(((Node)yyVal), ((Node)yyVals[-2+yyTop].value) == null ? yyVals[yyTop - count + 3].start : yyVals[yyTop - count + 1].start, yyVals[yyTop - count + 3].end);
                   /*% %*/
                   /*% ripper: stmts_add!($1, $3) %*/
  return yyVal;
};
states[20] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[21] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   p.yyerror("BEGIN is permitted only at toplevel");
  return yyVal;
};
states[22] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[23] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = p.allow_block_exit();
  return yyVal;
};
states[24] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((LexContext)yyVals[0+yyTop].value);
                   p.getLexContext().in_rescue = LexContext.InRescue.BEFORE_RESCUE;
  return yyVal;
};
states[25] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_FNAME|EXPR_FITEM);
  return yyVal;
};
states[26] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = newAlias(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: alias!($2, $4) %*/
  return yyVal;
};
states[27] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new VAliasNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[-1+yyTop].value)), p.symbolID(((ByteList)yyVals[0+yyTop].value)));
                    /*% %*/
                    /*% ripper: var_alias!($2, $3) %*/
  return yyVal;
};
states[28] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new VAliasNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[-1+yyTop].value)), p.symbolID(((BackRefNode)yyVals[0+yyTop].value).getByteName()));
                    /*% %*/
                    /*% ripper: var_alias!($2, $3) %*/
  return yyVal;
};
states[29] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "can't make alias for the number variables";
                    /*%%%*/
                    p.yyerror(message);
                    /*% %*/
                    /*% ripper[error]: alias_error!(ERR_MESG(), $3) %*/
  return yyVal;
};
states[30] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.nd_set_first_loc(((Node)yyVals[0+yyTop].value), yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: undef!($2) %*/
  return yyVal;
};
states[31] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_if(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value), p.remove_begin(((Node)yyVals[-2+yyTop].value)), null);
                    p.fixpos(((Node)yyVal), ((Node)yyVals[0+yyTop].value));
                    p.branch_modifier(((Node)yyVal), ((Node)yyVals[-2+yyTop].value), false, yyVals[yyTop - count + 3].end);
                    /*% %*/
                    /*% ripper: if_mod!($3, $1) %*/
  return yyVal;
};
states[32] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_if(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value), null, p.remove_begin(((Node)yyVals[-2+yyTop].value)));
                    p.fixpos(((Node)yyVal), ((Node)yyVals[0+yyTop].value));
                    p.branch_modifier(((Node)yyVal), ((Node)yyVals[-2+yyTop].value), true, yyVals[yyTop - count + 3].end);
                    /*% %*/
                    /*% ripper: unless_mod!($3, $1) %*/
  return yyVal;
};
states[33] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (((Node)yyVals[-2+yyTop].value) != null && ((Node)yyVals[-2+yyTop].value) instanceof BeginNode) {
                        yyVal = new WhileNode(yyVals[yyTop - count + 1].start(), p.cond(((Node)yyVals[0+yyTop].value)), ((BeginNode)yyVals[-2+yyTop].value).getBodyNode(), false);
                    } else {
                        yyVal = new WhileNode(yyVals[yyTop - count + 1].start(), p.cond(((Node)yyVals[0+yyTop].value)), ((Node)yyVals[-2+yyTop].value), true);
                    }
                    p.loop_body(((Node)yyVal), yyVals[yyTop - count + 1].start, yyVals[yyTop - count + 1].end);
                    /*% %*/
                    /*% ripper: while_mod!($3, $1) %*/
  return yyVal;
};
states[34] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (((Node)yyVals[-2+yyTop].value) != null && ((Node)yyVals[-2+yyTop].value) instanceof BeginNode) {
                        yyVal = new UntilNode(yyVals[yyTop - count + 1].start(), p.cond(((Node)yyVals[0+yyTop].value)), ((BeginNode)yyVals[-2+yyTop].value).getBodyNode(), false);
                    } else {
                        yyVal = new UntilNode(yyVals[yyTop - count + 1].start(), p.cond(((Node)yyVals[0+yyTop].value)), ((Node)yyVals[-2+yyTop].value), true);
                    }
                    p.loop_body(((Node)yyVal), yyVals[yyTop - count + 1].start, yyVals[yyTop - count + 1].end);
                    /*% %*/
                    /*% ripper: until_mod!($3, $1) %*/
  return yyVal;
};
states[35] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_rescue = ((LexContext)yyVals[-1+yyTop].value).in_rescue;
                    /*%%%*/
                    yyVal = p.newRescueModNode(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rescue_mod!($1, $4) %*/
  return yyVal;
};
states[36] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   if (p.getLexContext().in_def) {
                       p.warn("END in method; use at_exit");
                    }
                    p.restore_block_exit(((NodeExits)yyVals[-3+yyTop].value));
                    p.setLexContext(((LexContext)yyVals[-4+yyTop].value));
                    /*%%%*/
                   yyVal = new PostExeNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-1+yyTop].value), p.src_line());
                   p.span(((Node)yyVal), yyVals[yyTop - count + 1].start, yyVals[yyTop - count + 5].end);
                    /*% %*/
                    /*% ripper: END!($4) %*/
  return yyVal;
};
states[38] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = node_assign(((MultipleAsgnNode)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value), ((LexContext)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: massign!($1, $4) %*/
  return yyVal;
};
states[39] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = node_assign(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value), ((LexContext)yyVals[-1+yyTop].value));
                    p.span(((Node)yyVal), yyVals[yyTop - count + 1].start, yyVals[yyTop - count + 4].end);
                    /*% %*/
                    /*% ripper: assign!($1, $4) %*/
  return yyVal;
};
states[40] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.getLexContext().in_rescue = ((LexContext)yyVals[-4+yyTop].value).in_rescue;
                    yyVal = node_assign(((MultipleAsgnNode)yyVals[-6+yyTop].value), p.newRescueModNode(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value)), ((LexContext)yyVals[-4+yyTop].value));
                    /*% %*/
                    /*% ripper: massign!($1, rescue_mod!($4, $7)) %*/
  return yyVal;
};
states[41] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = node_assign(((MultipleAsgnNode)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value), ((LexContext)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: massign!($1, $4) %*/
  return yyVal;
};
states[42] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  return yyVal;
};
states[43] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.NEW_ERROR(yyVals[yyTop - count + 1]);
  return yyVal;
};
states[44] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = node_assign(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value), ((LexContext)yyVals[-1+yyTop].value));
                    p.span(((Node)yyVal), yyVals[yyTop - count + 1].start, yyVals[yyTop - count + 4].end);
                    /*% %*/
                    /*% ripper: assign!($1, $4) %*/
  return yyVal;
};
states[45] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_op_assign(((AssignableNode)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value), ((LexContext)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!($1, $2, $4) %*/
  return yyVal;
};
states[46] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = p.new_ary_op_assign(((Node)yyVals[-6+yyTop].value), ((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!(aref_field!($1, escape_Qundef($3)), $5, $7) %*/
  return yyVal;
};
states[47] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!(field!($1, $2, $3), $4, $6) %*/
  return yyVal;
};
states[48] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!(field!($1, $2, $3), $4, $6) %*/
  return yyVal;
};
states[49] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = p.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!(field!($1, ID2VAL(idCOLON2), $3), $4, $6) %*/
  return yyVal;
};
states[50] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    int line = yyVals[yyTop - count + 1].start();
                    yyVal = p.new_const_op_assign(line, p.new_colon2(line, ((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-3+yyTop].value)), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value), ((LexContext)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!(const_path_field!($1, $3), $4, $6) %*/
  return yyVal;
};
states[51] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    Integer pos = p.src_line();
                    yyVal = p.new_const_op_assign(pos, new Colon3Node(pos, p.symbolID(((ByteList)yyVals[-3+yyTop].value))), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value), ((LexContext)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!(top_const_field!($2), $3, $5) %*/
  return yyVal;
};
states[52] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.backref_error(((Node)yyVals[-3+yyTop].value));
                    /*% %*/
                    /*% ripper[error]: backref_error(p, RNODE($1), assign!(var_field(p, $1), $4)) %*/
  return yyVal;
};
states[53] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((DefHolder)yyVals[-3+yyTop].value), yyVals[yyTop - count + 1]);
                    p.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
                    /*%%%*/
                    yyVal = new DefnNode(((DefHolder)yyVals[-3+yyTop].value).line, ((DefHolder)yyVals[-3+yyTop].value).column, ((DefHolder)yyVals[-3+yyTop].value).name, ((ArgsNode)yyVals[-2+yyTop].value), p.getCurrentScope(), p.reduce_nodes(p.remove_begin(((Node)yyVals[0+yyTop].value))), ProductionState.line(yyVals[yyTop - count + 4].end), p.column(yyVals[yyTop - count + 4].end));
                    /* Changed from MRI*/
                    /*% %*/
                    /*% ripper: def!(get_value($1), $2, bodystmt!($4, Qnil, Qnil, Qnil)) %*/
                    p.popCurrentScope();
  return yyVal;
};
states[54] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((DefHolder)yyVals[-3+yyTop].value), yyVals[yyTop - count + 1]);
                    p.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
                    /*%%%*/
                    yyVal = new DefsNode(((DefHolder)yyVals[-3+yyTop].value).line, ((DefHolder)yyVals[-3+yyTop].value).column, (Node) ((DefHolder)yyVals[-3+yyTop].value).singleton, ((DefHolder)yyVals[-3+yyTop].value).name, ((ArgsNode)yyVals[-2+yyTop].value), p.getCurrentScope(), p.reduce_nodes(p.remove_begin(((Node)yyVals[0+yyTop].value))), ProductionState.line(yyVals[yyTop - count + 4].end), p.column(yyVals[yyTop - count + 4].end));
                    /*% %*/                    
                    /*% ripper: defs!(AREF($1, 0), AREF($1, 1), AREF($1, 2), $2, bodystmt!($4, Qnil, Qnil, Qnil)) %*/
                    p.popCurrentScope();
  return yyVal;
};
states[56] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_rescue = ((LexContext)yyVals[-1+yyTop].value).in_rescue;
                    /*%%%*/
                    yyVal = p.rescued_expr(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rescue_mod!($:1, $:4) %*/
  return yyVal;
};
states[57] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%/
                    $$ = p.call_uni_op(p.method_cond($3), NOT);
                    /*% ripper: unary!(ID2VAL(idNOT), $:3) %*/
  return yyVal;
};
states[58] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[59] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.getLexContext().in_rescue = ((LexContext)yyVals[-1+yyTop].value).in_rescue;
                    p.value_expr(((Node)yyVals[-3+yyTop].value));
                    yyVal = p.newRescueModNode(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rescue_mod!($1, $4) %*/
  return yyVal;
};
states[62] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.logop(((Node)yyVals[-2+yyTop].value), AND_KEYWORD, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[63] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.logop(((Node)yyVals[-2+yyTop].value), OR_KEYWORD, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[64] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((Node)yyVals[0+yyTop].value)), NOT);
  return yyVal;
};
states[65] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((Node)yyVals[0+yyTop].value)), BANG);
  return yyVal;
};
states[66] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[67] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pvtbl(((Set)yyVals[-2+yyTop].value));
                    p.pop_pktbl(((Set)yyVals[-1+yyTop].value));
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_kwarg = ((LexContext)yyVals[-3+yyTop].value).in_kwarg;
                    ctxt.in_alt_pattern = ((LexContext)yyVals[-3+yyTop].value).in_alt_pattern;
                    ctxt.capture_in_pattern = ((LexContext)yyVals[-3+yyTop].value).capture_in_pattern;
                    /*%%%*/
                    yyVal = p.one_line_pattern(p.newPatternCaseNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-6+yyTop].value), p.newIn(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value), null, null)));
                    /*% %*/
                    /*% ripper: case!($1, in!($7, Qnil, Qnil)) %*/
  return yyVal;
};
states[68] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[69] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pvtbl(((Set)yyVals[-2+yyTop].value));
                    p.pop_pktbl(((Set)yyVals[-1+yyTop].value));
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_kwarg = ((LexContext)yyVals[-3+yyTop].value).in_kwarg;
                    ctxt.in_alt_pattern = ((LexContext)yyVals[-3+yyTop].value).in_alt_pattern;
                    ctxt.capture_in_pattern = ((LexContext)yyVals[-3+yyTop].value).capture_in_pattern;
                    /*%%%*/
                    yyVal = p.one_line_pattern(p.newPatternCaseNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-6+yyTop].value), p.newIn(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value), new TrueNode(yyVals[yyTop - count + 1].start()), new FalseNode(yyVals[yyTop - count + 1].start()))));
                    /*% %*/
                    /*% ripper: case!($1, in!($7, Qnil, Qnil)) %*/
  return yyVal;
};
states[71] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pushLocalScope();
                    LexContext ctxt = p.getLexContext();
                    RubySymbol name = p.get_id(((ByteList)yyVals[0+yyTop].value));
                    /*%%%*/
                    p.numparam_name(((ByteList)yyVals[0+yyTop].value));
                    yyVal = new DefHolder(name, (LexContext) ctxt.clone());
                    /* a method's parameters must not change what an enclosing block allows, as in {def foo(_);end;it}*/
                    ((DefHolder) yyVal).maxNumParam = p.resetMaxNumParam();
                    /* Changed from MRI*/
                    /*%
                        p.numparam_name(yyVals[yyTop - count + 1].id);
                        $$ = new DefHolder(p.get_id(yyVals[yyTop - count + 1].id), p.get_value($1), (LexContext) ctxt.clone());
                    %*/
                    ctxt.in_def = true;
                    ctxt.in_rescue = LexContext.InRescue.BEFORE_RESCUE;
                    ctxt.cant_return = false;
  return yyVal;
};
states[72] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    ((DefHolder)yyVals[0+yyTop].value).line = yyVals[yyTop - count + 1].start();
                    ((DefHolder)yyVals[0+yyTop].value).column = p.column(yyVals[yyTop - count + 1].start);
                    yyVal = ((DefHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[73] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_FNAME); 
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_argdef = true;
  return yyVal;
};
states[74] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_ENDFN|EXPR_LABEL);
                    ((DefHolder)yyVals[0+yyTop].value).line = yyVals[yyTop - count + 1].start();
                    ((DefHolder)yyVals[0+yyTop].value).column = p.column(yyVals[yyTop - count + 1].start);
                    yyVal = ((DefHolder)yyVals[0+yyTop].value);
                    /*%%%*/
                    ((DefHolder)yyVals[0+yyTop].value).setSingleton(((Node)yyVals[-3+yyTop].value));
                    ((DefHolder)yyVals[0+yyTop].value).setDotOrColon(p.extractByteList(((ByteList)yyVals[-2+yyTop].value)));
                    /* Changed from MRI*/
                    /*%
                       $<DefHolder>$.value = p.new_array($2, $3, $<DefHolder>$.value);
                    %*/
  return yyVal;
};
states[75] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[76] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.NEW_ERROR(yyVals[yyTop - count + 1]);
  return yyVal;
};
states[77] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getConditionState().push1();
  return yyVal;
};
states[78] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getConditionState().pop();
  return yyVal;
};
states[79] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-2+yyTop].value);
                    /*% ripper: get_value($:2); %*/
  return yyVal;
};
states[82] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                       p.value_expr(((Node)yyVals[0+yyTop].value));
                       yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[84] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].start());
                    p.safe_navigation_end(((Node)yyVal), yyVals[yyTop - count + 3].end, yyVals[yyTop - count + 4].end, false);
                    /*% %*/
                    /*% ripper: method_add_arg!(call!($1, $2, $3), $4) %*/
  return yyVal;
};
states[85] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IterNode)yyVals[-1+yyTop].value);
                    /*%%%*/
                    ((IterNode)yyVals[-1+yyTop].value).setLine(yyVals[yyTop - count + 1].end());
                    p.block_span(((IterNode)yyVals[-1+yyTop].value), yyVals[yyTop - count + 1].start, yyVals[yyTop - count + 3].end);
                    /*% %*/
  return yyVal;
};
states[86] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_fcall(((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: $1 %*/
  return yyVal;
};
states[87] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.frobnicate_fcall_args(((FCallNode)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
                    yyVal = ((FCallNode)yyVals[-1+yyTop].value);
                    /*% %*/
                    /*% ripper: command!($1, $2) %*/
  return yyVal;
};
states[88] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.frobnicate_fcall_args(((FCallNode)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value));
                    yyVal = ((FCallNode)yyVals[-2+yyTop].value);
                    /*% %*/
                    /*% ripper: method_add_block!(command!($1, $2), $3) %*/
  return yyVal;
};
states[89] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].start());
                    p.safe_navigation_end(((Node)yyVal), yyVals[yyTop - count + 3].end, yyVals[yyTop - count + 4].end, false);
                    /*% %*/
                    /*% ripper: command_call!($1, $2, $3, $4) %*/
  return yyVal;
};
states[90] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value), yyVals[yyTop - count + 3].start());
                    p.span(((Node)yyVal), yyVals[yyTop - count + 1].start, yyVals[yyTop - count + 4].end);
                    p.safe_navigation_end(((Node)yyVal), yyVals[yyTop - count + 3].end, yyVals[yyTop - count + 4].end, false);
                    /*% %*/
                    /*% ripper: method_add_block!(command_call!($1, $2, $3, $4), $5) %*/
  return yyVal;
};
states[91] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
                    /*% %*/
                    /*% ripper: command_call!($1, ID2VAL(idCOLON2), $3, $4) %*/
  return yyVal;
};
states[92] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: method_add_block!(command_call!($1, ID2VAL(idCOLON2), $3, $4), $5) %*/
  return yyVal;
};
states[93] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    ((IterNode)yyVals[-1+yyTop].value).setLine(yyVals[yyTop - count + 4].end());
                    p.block_span(((IterNode)yyVals[-1+yyTop].value), yyVals[yyTop - count + 4].start, yyVals[yyTop - count + 6].end);
                    yyVal = p.new_call(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), null, ((IterNode)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: method_add_block!(command_call!($:1, $:2, $:3, Qnil), $:5) %*/
  return yyVal;
};
states[94] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_super(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: super!($2) %*/
  return yyVal;
};
states[95] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_yield(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: yield!($2) %*/
  return yyVal;
};
states[96] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ReturnNode(yyVals[yyTop - count + 1].start(), p.ret_args(((Node)yyVals[0+yyTop].value), yyVals[yyTop - count + 1].start()));
                    /*% %*/
                    /*% ripper: return!($2) %*/
  return yyVal;
};
states[97] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new BreakNode(yyVals[yyTop - count + 1].start(), p.ret_args(((Node)yyVals[0+yyTop].value), yyVals[yyTop - count + 1].start()));
                    /*% %*/
                    /*% ripper: break!($2) %*/
  return yyVal;
};
states[98] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new NextNode(yyVals[yyTop - count + 1].start(), p.ret_args(((Node)yyVals[0+yyTop].value), yyVals[yyTop - count + 1].start()));
                    /*% %*/
                    /*% ripper: next!($2) %*/
  return yyVal;
};
states[100] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((Node)yyVals[-1+yyTop].value);
                    /*% %*/
                    /*% ripper: mlhs_paren!($2) %*/
  return yyVal;
};
states[101] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((MultipleAsgnNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[102] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(((Integer)yyVals[-2+yyTop].value), p.newArrayNode(((Integer)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value)), null, null);
                    /*% %*/
                    /*% ripper: mlhs_paren!($2) %*/
  return yyVal;
};
states[103] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[0+yyTop].value), null, null);
                    /*% %*/
                    /*% ripper: $1 %*/
  return yyVal;
};
states[104] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-1+yyTop].value).add(((Node)yyVals[0+yyTop].value)), null, null);
                    /*% %*/
                    /*% ripper: mlhs_add!($1, $2) %*/
  return yyVal;
};
states[105] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value), (ListNode) null);
                    /*% %*/
                    /*% ripper: mlhs_add_star!($1, $3) %*/
  return yyVal;
};
states[106] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-4+yyTop].value), ((Node)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add_post!(mlhs_add_star!($1, $3), $5) %*/
  return yyVal;
};
states[107] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-1+yyTop].value), new StarNode(p.src_line()), null);
                    /*% %*/
                    /*% ripper: mlhs_add_star!($1, Qnil) %*/
  return yyVal;
};
states[108] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-3+yyTop].value), new StarNode(p.src_line()), ((ListNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add_post!(mlhs_add_star!($1, Qnil), $4) %*/
  return yyVal;
};
states[109] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(yyVals[yyTop - count + 2].start(), null, ((Node)yyVals[0+yyTop].value), null);
                    /*% %*/
                    /*% ripper: mlhs_add_star!(mlhs_new!, $2) %*/
  return yyVal;
};
states[110] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(yyVals[yyTop - count + 2].start(), null, ((Node)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add_post!(mlhs_add_star!(mlhs_new!, $2), $4) %*/
  return yyVal;
};
states[111] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(p.src_line(), null, new StarNode(p.src_line()), null);
                    /*% %*/
                    /*% ripper: mlhs_add_star!(mlhs_new!, Qnil) %*/
  return yyVal;
};
states[112] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(p.src_line(), null, new StarNode(p.src_line()), ((ListNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add_post!(mlhs_add_star!(mlhs_new!, Qnil), $3) %*/
  return yyVal;
};
states[114] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((Node)yyVals[-1+yyTop].value);
                    /*% %*/
                    /*% ripper: mlhs_paren!($2) %*/
  return yyVal;
};
states[115] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newArrayNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add!(mlhs_new!, $1) %*/
  return yyVal;
};
states[116] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add!($1, $2) %*/
  return yyVal;
};
states[117] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newArrayNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add!(mlhs_new!, $1) %*/
  return yyVal;
};
states[118] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add!($1, $3) %*/
  return yyVal;
};
states[119] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                   yyVal = p.assignableLabelOrIdentifier(((ByteList)yyVals[0+yyTop].value), null);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[120] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                   yyVal = new InstAsgnNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[121] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                   yyVal = new GlobalAsgnNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[122] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (p.getLexContext().in_def) p.compile_error("dynamic constant assignment");
                    yyVal = new ConstDeclNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), null, NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[123] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ClassVarAsgnNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[124] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to nil");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[125] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't change the value of self");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[126] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to true");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[127] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to false");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[128] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to __FILE__");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[129] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to __LINE__");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[130] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to __ENCODING__");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[131] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.aryset(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: aref_field!($1, escape_Qundef($3)) %*/
  return yyVal;
};
states[132] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (((ByteList)yyVals[-1+yyTop].value) == AMPERSAND_DOT) {
                        p.compile_error("&. inside multiple assignment destination");
                    }
                    /*%%%*/
                    yyVal = p.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: field!($1, $2, $3) %*/
  return yyVal;
};
states[133] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: const_path_field!($1, $3) %*/
  return yyVal;
};
states[134] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (((ByteList)yyVals[-1+yyTop].value) == AMPERSAND_DOT) {
                        p.compile_error("&. inside multiple assignment destination");
                    }
                    /*%%%*/
                    yyVal = p.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: field!($1, $2, $3) %*/
  return yyVal;
};
states[135] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (p.getLexContext().in_def) p.yyerror("dynamic constant assignment");

                    Integer position = yyVals[yyTop - count + 1].start();

                    yyVal = new ConstDeclNode(position, (RubySymbol) null, p.new_colon2(position, ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*% %*/
                    /*% ripper: const_decl(p, const_path_field!($1, $3)) %*/
  return yyVal;
};
states[136] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (p.getLexContext().in_def) {
                        p.yyerror("dynamic constant assignment");
                    }

                    Integer position = yyVals[yyTop - count + 1].start();

                    yyVal = new ConstDeclNode(position, (RubySymbol) null, p.new_colon3(position, ((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*% %*/
                    /*% ripper: const_decl(p, top_const_field!($2)) %*/
  return yyVal;
};
states[137] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.backref_error(((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper[error]: backref_error(p, RNODE($1), var_field(p, $1)) %*/
  return yyVal;
};
states[138] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.assignableLabelOrIdentifier(((ByteList)yyVals[0+yyTop].value), null);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
                    
  return yyVal;
};
states[139] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new InstAsgnNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[140] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new GlobalAsgnNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[141] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (p.getLexContext().in_def) p.compile_error("dynamic constant assignment");

                    yyVal = new ConstDeclNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), null, NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[142] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ClassVarAsgnNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[143] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to nil");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[144] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't change the value of self");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[145] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to true");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[146] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to false");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[147] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to __FILE__");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[148] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to __LINE__");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[149] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to __ENCODING__");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[150] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.aryset(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: aref_field!($1, escape_Qundef($3)) %*/
  return yyVal;
};
states[151] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: field!($1, $2, $3) %*/
  return yyVal;
};
states[152] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: field!($1, ID2VAL(idCOLON2), $3) %*/
  return yyVal;
};
states[153] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: field!($1, $2, $3) %*/
  return yyVal;
};
states[154] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ConstDeclNode(yyVals[yyTop - count + 1].start(), (RubySymbol) null, p.new_colon2(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*% %*/
                    /*% ripper: const_decl(p, const_path_field!($1, $3)) %*/
  return yyVal;
};
states[155] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ConstDeclNode(yyVals[yyTop - count + 1].start(), (RubySymbol) null, p.new_colon3(yyVals[yyTop - count + 1].start(), ((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*% %*/
                    /*% ripper: const_decl(p, top_const_field!($2)) %*/
  return yyVal;
};
states[156] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.backref_error(((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper[error]: backref_error(p, RNODE($1), var_field(p, $1)) %*/
  return yyVal;
};
states[157] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "class/module name must be CONSTANT";
                    /*%%%*/
                    p.yyerror(message, yyVals[yyTop - count + 1]);
                    /*% %*/
                    /*% ripper[error]: class_name_error!(ERR_MESG(), $1) %*/
  return yyVal;
};
states[158] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[159] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_colon3(yyVals[yyTop - count + 1].start(), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: top_const_ref!($2) %*/
  return yyVal;
};
states[160] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_colon2(yyVals[yyTop - count + 1].start(), null, ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: const_ref!($1) %*/
  return yyVal;
};
states[161] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_colon2(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: const_path_ref!($1, $3) %*/
  return yyVal;
};
states[162] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[163] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[164] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[165] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   p.setState(EXPR_ENDFN);
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[166] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[167] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal =  new LiteralNode(p.src_line(), p.symbolID(((ByteList)yyVals[0+yyTop].value)));
                    /*% %*/
                    /*% ripper: symbol_literal!($1) %*/

  return yyVal;
};
states[168] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[169] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = newUndef(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_new3(1, get_value($1)) %*/
  return yyVal;
};
states[170] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_FNAME|EXPR_FITEM);
  return yyVal;
};
states[171] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.appendToBlock(((Node)yyVals[-3+yyTop].value), newUndef(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value)));
                    /*% %*/
                    /*% ripper: rb_ary_push($1, get_value($4)) %*/
  return yyVal;
};
states[172] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[173] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[174] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[175] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[176] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[177] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[178] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[179] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[180] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[181] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[182] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[183] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[184] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[185] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[186] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[187] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[188] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[189] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[190] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[191] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[192] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[193] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[194] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[195] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[196] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[197] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[198] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[199] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[200] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[201] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[202] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[203] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[204] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[205] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[206] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[207] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[208] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[209] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[210] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[211] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[212] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[213] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[214] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[215] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[216] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[217] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[218] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[219] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[220] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[221] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[222] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = yyVals[0+yyTop].value;
  return yyVal;
};
states[223] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[224] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[225] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[226] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[227] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[228] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[229] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[230] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[231] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[232] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[233] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[234] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[235] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[236] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[237] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[238] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[239] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[240] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[241] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[242] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[243] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = node_assign(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value), ((LexContext)yyVals[-1+yyTop].value));
                    p.span(((Node)yyVal), yyVals[yyTop - count + 1].start, yyVals[yyTop - count + 4].end);
                    /*% %*/
                    /*% ripper: assign!($1, $4) %*/
  return yyVal;
};
states[244] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_op_assign(((AssignableNode)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value), ((LexContext)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!($1, $2, $4) %*/
  return yyVal;
};
states[245] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_ary_op_assign(((Node)yyVals[-6+yyTop].value), ((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!(aref_field!($1, escape_Qundef($3)), $5, $7) %*/
  return yyVal;
};
states[246] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/

                    yyVal = p.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!(field!($1, $2, $3), $4, $6) %*/
  return yyVal;
};
states[247] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = p.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!(field!($1, $2, $3), $4, $6) %*/
  return yyVal;
};
states[248] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!(field!($1, ID2VAL(idCOLON2), $3), $4, $6) %*/
  return yyVal;
};
states[249] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    Integer pos = yyVals[yyTop - count + 1].start();
                    yyVal = p.new_const_op_assign(pos, p.new_colon2(pos, ((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-3+yyTop].value)), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value), ((LexContext)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!(const_path_field!($1, $3), $4, $6) %*/
  return yyVal;
};
states[250] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    Integer pos = p.src_line();
                    yyVal = p.new_const_op_assign(pos, new Colon3Node(pos, p.symbolID(((ByteList)yyVals[-3+yyTop].value))), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value), ((LexContext)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!(top_const_field!($2), $3, $5) %*/
  return yyVal;
};
states[251] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.backref_error(((Node)yyVals[-3+yyTop].value));
                    /*% %*/
                    /*% ripper[error]: backref_error(p, RNODE($1), opassign!(var_field(p, $1), $2, $4)) %*/
  return yyVal;
};
states[252] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[-2+yyTop].value));
                    p.value_expr(((Node)yyVals[0+yyTop].value));
    
                    boolean isLiteral = ((Node)yyVals[-2+yyTop].value) instanceof FixnumNode && ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), p.makeNullNil(((Node)yyVals[-2+yyTop].value)), p.makeNullNil(((Node)yyVals[0+yyTop].value)), false, isLiteral);
                    /*% %*/
                    /*% ripper: dot2!($1, $3) %*/
  return yyVal;
};
states[253] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[-2+yyTop].value));
                    p.value_expr(((Node)yyVals[0+yyTop].value));

                    boolean isLiteral = ((Node)yyVals[-2+yyTop].value) instanceof FixnumNode && ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), p.makeNullNil(((Node)yyVals[-2+yyTop].value)), p.makeNullNil(((Node)yyVals[0+yyTop].value)), true, isLiteral);
                    /*% %*/
                    /*% ripper: dot3!($1, $3) %*/
  return yyVal;
};
states[254] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[-1+yyTop].value));

                    boolean isLiteral = ((Node)yyVals[-1+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), p.makeNullNil(((Node)yyVals[-1+yyTop].value)), NilImplicitNode.NIL, false, isLiteral);
                    /*% %*/
                    /*% ripper: dot2!($1, Qnil) %*/
  return yyVal;
};
states[255] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[-1+yyTop].value));

                    boolean isLiteral = ((Node)yyVals[-1+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), p.makeNullNil(((Node)yyVals[-1+yyTop].value)), NilImplicitNode.NIL, true, isLiteral);
                    /*% %*/
                    /*% ripper: dot3!($1, Qnil) %*/
  return yyVal;
};
states[256] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), NilImplicitNode.NIL, p.makeNullNil(((Node)yyVals[0+yyTop].value)), false, isLiteral);
                    /*% %*/
                    /*% ripper: dot2!(Qnil, $2) %*/
  return yyVal;
};
states[257] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), NilImplicitNode.NIL, p.makeNullNil(((Node)yyVals[0+yyTop].value)), true, isLiteral);
                    /*% %*/
                    /*% ripper: dot3!(Qnil, $2) %*/
  return yyVal;
};
states[258] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), PLUS, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[259] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), MINUS, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[260] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), STAR, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[261] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), SLASH, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[262] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), PERCENT, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[263] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), STAR_STAR, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[264] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.call_bin_op(((NumericNode)yyVals[-2+yyTop].value), STAR_STAR, ((Node)yyVals[0+yyTop].value), p.src_line()), MINUS_AT);
  return yyVal;
};
states[265] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(((Node)yyVals[0+yyTop].value), PLUS_AT);
  return yyVal;
};
states[266] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(((Node)yyVals[0+yyTop].value), MINUS_AT);
  return yyVal;
};
states[267] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), OR, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[268] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), CARET, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[269] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), AMPERSAND, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[270] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), LT_EQ_RT, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[271] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[272] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), EQ_EQ, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[273] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), EQ_EQ_EQ, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[274] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), BANG_EQ, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[275] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.match_op(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[276] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), BANG_TILDE, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[277] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((Node)yyVals[0+yyTop].value)), BANG);
  return yyVal;
};
states[278] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(((Node)yyVals[0+yyTop].value), TILDE);
  return yyVal;
};
states[279] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), LT_LT, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[280] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), GT_GT, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[281] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.logop(((Node)yyVals[-2+yyTop].value), AMPERSAND_AMPERSAND, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[282] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.logop(((Node)yyVals[-2+yyTop].value), OR_OR, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[283] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_defined = false;                    
                    yyVal = p.new_defined(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[284] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((DefHolder)yyVals[-3+yyTop].value), yyVals[yyTop - count + 1]);
                    p.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
                    /*%%%*/
                    yyVal = new DefnNode(((DefHolder)yyVals[-3+yyTop].value).line, ((DefHolder)yyVals[-3+yyTop].value).column, ((DefHolder)yyVals[-3+yyTop].value).name, ((ArgsNode)yyVals[-2+yyTop].value), p.getCurrentScope(), p.reduce_nodes(p.remove_begin(((Node)yyVals[0+yyTop].value))), ProductionState.line(yyVals[yyTop - count + 4].end), p.column(yyVals[yyTop - count + 4].end));
                    if (p.isNextBreak) ((DefnNode)yyVal).setContainsNextBreak();
                    /* Changed from MRI (combined two stmts)*/
                    /*% %*/
                    /*% ripper: def!(get_value($1), $2, bodystmt!($4, Qnil, Qnil, Qnil)) %*/
                    p.popCurrentScope();
  return yyVal;
};
states[285] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((DefHolder)yyVals[-3+yyTop].value), yyVals[yyTop - count + 1]);
                    p.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
                    /*%%%*/
                    yyVal = new DefsNode(((DefHolder)yyVals[-3+yyTop].value).line, ((DefHolder)yyVals[-3+yyTop].value).column, (Node) ((DefHolder)yyVals[-3+yyTop].value).singleton, ((DefHolder)yyVals[-3+yyTop].value).name, ((ArgsNode)yyVals[-2+yyTop].value), p.getCurrentScope(), p.reduce_nodes(p.remove_begin(((Node)yyVals[0+yyTop].value))), ProductionState.line(yyVals[yyTop - count + 4].end), p.column(yyVals[yyTop - count + 4].end));
                    if (p.isNextBreak) ((DefsNode)yyVal).setContainsNextBreak();
                    /*% %*/
                    /*% ripper: defs!(AREF($1, 0), AREF($1, 1), AREF($1, 2), $2, bodystmt!($4, Qnil, Qnil, Qnil)) %*/
                    p.popCurrentScope();
  return yyVal;
};
states[286] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[-5+yyTop].value));
                    /* Each arm of a ternary is a statement of its own for line events, as in MRI.*/
                    yyVal = p.new_if(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-5+yyTop].value), p.newline_node(((Node)yyVals[-3+yyTop].value), yyVals[yyTop - count + 3].start()), p.newline_node(((Node)yyVals[0+yyTop].value), yyVals[yyTop - count + 6].start()));
                    p.branch_ternary(((Node)yyVal), yyVals[yyTop - count + 1].end);
                    /*% %*/
                    /*% ripper: ifop!($1, $3, $6) %*/
  return yyVal;
};
states[287] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[289] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_rescue = ((LexContext)yyVals[-1+yyTop].value).in_rescue;
                    /*%%%*/
                    yyVal = p.rescued_expr(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rescue_mod!($:1, $:4) %*/
  return yyVal;
};
states[290] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((Node)yyVals[0+yyTop].value)), NOT);
                    /*% ripper: unary!(ID2VAL(idNOT), $:3) %*/
  return yyVal;
};
states[291] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = GT;
  return yyVal;
};
states[292] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = LT;
  return yyVal;
};
states[293] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = GT_EQ;
  return yyVal;
};
states[294] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = LT_EQ;
  return yyVal;
};
states[295] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[296] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.warning(p.src_line(), "comparison '" + ((ByteList)yyVals[-1+yyTop].value) + "' after comparison");
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[297] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = (LexContext) p.getLexContext().clone();
  return yyVal;
};
states[298] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_defined = true;
                    yyVal = ((LexContext)yyVals[0+yyTop].value);
  return yyVal;
};
states[299] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_rescue = LexContext.InRescue.AFTER_RESCUE;
                    yyVal = ((LexContext)yyVals[0+yyTop].value);
  return yyVal;
};
states[300] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = p.makeNullNil(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[302] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[303] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.arg_append(((Node)yyVals[-3+yyTop].value), p.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
                    /*% %*/
                    /*% ripper: args_add!($1, bare_assoc_hash!($3)) %*/
  return yyVal;
};
states[304] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newArrayNode(yyVals[yyTop - count + 1].start(), p.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
                    /*% %*/
                    /*% ripper: args_add!(args_new!, bare_assoc_hash!($1)) %*/
  return yyVal;
};
states[305] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[306] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_rescue = ((LexContext)yyVals[-1+yyTop].value).in_rescue;
                    /*%%%*/
                    p.value_expr(((Node)yyVals[-3+yyTop].value));
                    yyVal = p.newRescueModNode(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rescue_mod!($1, $4) %*/
  return yyVal;
};
states[307] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((Node)yyVals[-1+yyTop].value);
                    /*% %*/
                    /*% ripper: arg_paren!(escape_Qundef($2)) %*/
  return yyVal;
};
states[308] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (!p.check_forwarding_args()) {
                        yyVal = null;
                    } else {
                    /*%%%*/
                        yyVal = p.new_args_forward_call(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-3+yyTop].value));
                    /*% %*/
                    /*% ripper: arg_paren!(args_add!($2, $4)) %*/
                    }
  return yyVal;
};
states[309] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (!p.check_forwarding_args()) {
                        yyVal = null;
                    } else {
                    /*%%%*/
                        yyVal = p.new_args_forward_call(yyVals[yyTop - count + 1].start(), null);
                    /*% %*/
                    /*% ripper: arg_paren!($2) %*/
                    }
  return yyVal;
};
states[314] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[315] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.arg_append(((Node)yyVals[-3+yyTop].value), p.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
                    /*% %*/
                    /*% ripper: args_add!($1, bare_assoc_hash!($3)) %*/
  return yyVal;
};
states[316] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newArrayNode(yyVals[yyTop - count + 1].start(), p.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
                    /*% %*/
                    /*% ripper: args_add!(args_new!, bare_assoc_hash!($1)) %*/
  return yyVal;
};
states[317] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = p.newArrayNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: args_add!(args_new!, $1) %*/
  return yyVal;
};
states[318] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((DefHolder)yyVals[-3+yyTop].value), yyVals[yyTop - count + 1]);
                    p.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
                    /*%%%*/
                    yyVal = new DefnNode(((DefHolder)yyVals[-3+yyTop].value).line, ((DefHolder)yyVals[-3+yyTop].value).column, ((DefHolder)yyVals[-3+yyTop].value).name, ((ArgsNode)yyVals[-2+yyTop].value), p.getCurrentScope(), p.reduce_nodes(p.remove_begin(((Node)yyVals[0+yyTop].value))), ProductionState.line(yyVals[yyTop - count + 4].end), p.column(yyVals[yyTop - count + 4].end));
                    if (p.isNextBreak) ((DefnNode)yyVal).setContainsNextBreak();
                    /* Changed from MRI (combined two stmts)*/
                    /*% %*/
                    /*% ripper: def!(get_value($1), $2, bodystmt!($4, Qnil, Qnil, Qnil)) %*/
                    p.popCurrentScope();
  return yyVal;
};
states[319] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((DefHolder)yyVals[-3+yyTop].value), yyVals[yyTop - count + 1]);
                    p.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
                    /*%%%*/
                    yyVal = new DefsNode(((DefHolder)yyVals[-3+yyTop].value).line, ((DefHolder)yyVals[-3+yyTop].value).column, (Node) ((DefHolder)yyVals[-3+yyTop].value).singleton, ((DefHolder)yyVals[-3+yyTop].value).name, ((ArgsNode)yyVals[-2+yyTop].value), p.getCurrentScope(), p.reduce_nodes(p.remove_begin(((Node)yyVals[0+yyTop].value))), ProductionState.line(yyVals[yyTop - count + 4].end), p.column(yyVals[yyTop - count + 4].end));
                    if (p.isNextBreak) ((DefsNode)yyVal).setContainsNextBreak();
                    /*% %*/
                    /*% ripper: defs!(AREF($1, 0), AREF($1, 1), AREF($1, 2), $2, bodystmt!($4, Qnil, Qnil, Qnil)) %*/
                    p.popCurrentScope();
  return yyVal;
};
states[320] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = arg_blk_pass(((Node)yyVals[-1+yyTop].value), ((BlockPassNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: args_add_block!($1, $2) %*/
  return yyVal;
};
states[321] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newArrayNode(yyVals[yyTop - count + 1].start(), p.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
                    yyVal = arg_blk_pass(((Node)yyVal), ((BlockPassNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: args_add_block!(args_add!(args_new!, bare_assoc_hash!($1)), $2) %*/
  return yyVal;
};
states[322] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.arg_append(((Node)yyVals[-3+yyTop].value), p.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
                    yyVal = arg_blk_pass(((Node)yyVal), ((BlockPassNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: args_add_block!(args_add!($1, bare_assoc_hash!($3)), $4) %*/
  return yyVal;
};
states[323] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*% ripper[brace]: args_add_block!(args_new!, $1) %*/
  return yyVal;
};
states[324] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    boolean lookahead = false;
                    switch (yychar) {
                    case '(': case tLPAREN: case tLPAREN_ARG: case '[': case tLBRACK:
                       lookahead = true;
                    }
                    StackState cmdarg = p.getCmdArgumentState();
                    if (lookahead) cmdarg.pop();
                    cmdarg.push1();
                    if (lookahead) cmdarg.push0();
  return yyVal;
};
states[325] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    StackState cmdarg = p.getCmdArgumentState();
                    boolean lookahead = false;
                    switch (yychar) {
                    case tLBRACE_ARG:
                       lookahead = true;
                    }
                      
                    if (lookahead) cmdarg.pop();
                    cmdarg.pop();
                    if (lookahead) cmdarg.push0();
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[326] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new BlockPassNode(yyVals[yyTop - count + 2].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: $2 %*/
  return yyVal;
};
states[327] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.forwarding_arg_check(FWD_BLOCK, FWD_ALL, "block");
                    /*%%%*/
                    yyVal = new BlockPassNode(yyVals[yyTop - count + 1].start(), p.arg_var(ANON_BLOCK));
                    /* Changed from MRI*/
                    /*%
                    $$ = p.nil();
                    %*/
  return yyVal;
};
states[328] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((BlockPassNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[329] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = null;
                    /* Changed from MRI*/
                    /*%
                    $$ = p.fals();
                    %*/
  return yyVal;
};
states[330] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    int line = ((Node)yyVals[0+yyTop].value) instanceof NilImplicitNode ? p.src_line() : yyVals[yyTop - count + 1].start();
                    yyVal = p.newArrayNode(line, ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: args_add!(args_new!, $1) %*/
  return yyVal;
};
states[331] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newSplatNode(((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: args_add_star!(args_new!, $1) %*/
  return yyVal;
};
states[332] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    Node node = p.splat_array(((Node)yyVals[-2+yyTop].value));

                    if (node != null) {
                        yyVal = p.list_append(node, ((Node)yyVals[0+yyTop].value));
                    } else {
                        yyVal = p.arg_append(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    }
                    /*% %*/
                    /*% ripper: args_add!($1, $3) %*/
  return yyVal;
};
states[333] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.rest_arg_append(((Node)yyVals[-2+yyTop].value), p.newSplatNode(((Node)yyVals[0+yyTop].value)));
                    /*% %*/
                    /*% ripper: args_add_star!($1, $3) %*/
  return yyVal;
};
states[334] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
                    /*% ripper: get_value($2); %*/
  return yyVal;
};
states[335] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.forwarding_arg_check(FWD_REST, FWD_ALL, "rest");
                    yyVal = p.declareIdentifier(FWD_REST);
                    /*% %*/
                    /*% ripper: Qnil %*/
  return yyVal;
};
states[336] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[337] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[338] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/

                    Node node = p.splat_array(((Node)yyVals[-2+yyTop].value));

                    if (node != null) {
                        yyVal = p.list_append(node, ((Node)yyVals[0+yyTop].value));
                    } else {
                        yyVal = p.arg_append(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    }
                    /*% %*/
                    /*% ripper: mrhs_add!(mrhs_new_from_args!($1), $3) %*/
  return yyVal;
};
states[339] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    Node node = null;

                    if (((Node)yyVals[0+yyTop].value) instanceof ArrayNode &&
                        (node = p.splat_array(((Node)yyVals[-3+yyTop].value))) != null) {
                        yyVal = p.list_concat(node, ((Node)yyVals[0+yyTop].value));
                    } else {
                        yyVal = arg_concat(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    }
                    /*% %*/
                    /*% ripper: mrhs_add_star!(mrhs_new_from_args!($1), $4) %*/
  return yyVal;
};
states[340] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newSplatNode(((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: mrhs_add_star!(mrhs_new!, $2) %*/
  return yyVal;
};
states[345] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ListNode)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[346] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ListNode)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[347] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ListNode)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[348] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ListNode)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[351] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_fcall(((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: method_add_arg!(fcall!($1), args_new!) %*/
  return yyVal;
};
states[352] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getCmdArgumentState().push0();
  return yyVal;
};
states[353] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getCmdArgumentState().pop();
                    /*%%%*/
                    yyVal = new BeginNode(yyVals[yyTop - count + 1].start(), p.makeNullNil(((Node)yyVals[-1+yyTop].value)));
                    /*% %*/
                    /*% ripper: begin!($3) %*/
  return yyVal;
};
states[354] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_ENDARG); 
  return yyVal;
};
states[355] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((Node)yyVals[-2+yyTop].value);
                    p.paren_span(((Node)yyVal), yyVals[yyTop - count + 1].start, yyVals[yyTop - count + 4].end);
                    /*% %*/
                    /*% ripper: paren!($2) %*/
  return yyVal;
};
states[356] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (((Node)yyVals[-1+yyTop].value) != null) {
                        /* compstmt position includes both parens around it*/
                        ((Node)yyVals[-1+yyTop].value).setLine(((Integer)yyVals[-2+yyTop].value));
                        yyVal = ((Node)yyVals[-1+yyTop].value);
                    } else {
                        yyVal = new NilNode(((Integer)yyVals[-2+yyTop].value));
                    }
                    p.paren_span(((Node)yyVal), yyVals[yyTop - count + 1].start, yyVals[yyTop - count + 3].end);
                    /*% %*/
                    /*% ripper: paren!($2) %*/
  return yyVal;
};
states[357] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_colon2(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: const_path_ref!($1, $3) %*/
  return yyVal;
};
states[358] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_colon3(yyVals[yyTop - count + 1].start(), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: top_const_ref!($2) %*/
  return yyVal;
};
states[359] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.make_list(((Node)yyVals[-1+yyTop].value), yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: array!(escape_Qundef($2)) %*/
  return yyVal;
};
states[360] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.nd_set_loc(((HashNode)yyVals[-1+yyTop].value), yyVals[yyTop - count + 1].start());
                    ((HashNode)yyVal).setIsLiteral();
                    /*% %*/
                    /*% ripper: hash!(escape_Qundef($2)) %*/
  return yyVal;
};
states[361] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ReturnNode(yyVals[yyTop - count + 1].start(), NilImplicitNode.NIL);
                    /*% %*/
                    /*% ripper: return0! %*/
  return yyVal;
};
states[362] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_yield(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: yield!(paren!($3)) %*/
  return yyVal;
};
states[363] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new YieldNode(yyVals[yyTop - count + 1].start(), null);
                    /*% %*/
                    /*% ripper: yield!(paren!(args_new!)) %*/
  return yyVal;
};
states[364] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new YieldNode(yyVals[yyTop - count + 1].start(), null);
                    /*% %*/
                    /*% ripper: yield0! %*/
  return yyVal;
};
states[365] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_defined = false;
                    yyVal = p.new_defined(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[366] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((Node)yyVals[-1+yyTop].value)), NOT);
  return yyVal;
};
states[367] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(p.nil()), NOT);
  return yyVal;
};
states[368] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.frobnicate_fcall_args(((FCallNode)yyVals[-1+yyTop].value), null, ((IterNode)yyVals[0+yyTop].value));
                    yyVal = ((FCallNode)yyVals[-1+yyTop].value);                    
                    /*% %*/
                    /*% ripper: method_add_block!(method_add_arg!(fcall!($1), args_new!), $2) %*/
  return yyVal;
};
states[370] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (((Node)yyVals[-1+yyTop].value) != null && 
                          ((BlockAcceptingNode)yyVals[-1+yyTop].value).getIterNode() instanceof BlockPassNode) {
                          p.compile_error("both block arg and actual block given.");
                    }
                    yyVal = ((BlockAcceptingNode)yyVals[-1+yyTop].value).setIterNode(((IterNode)yyVals[0+yyTop].value));
                    ((Node)yyVal).setLine(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: method_add_block!($1, $2) %*/
  return yyVal;
};
states[371] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((LambdaNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[372] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_if(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-4+yyTop].value), ((Node)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value));
                    p.branch_if(((Node)yyVal), yyVals[yyTop - count + 2].end, ((Node)yyVals[-1+yyTop].value) == null ? -1 : yyVals[yyTop - count + 5].start, yyVals[yyTop - count + 6].end);
                    /*% %*/
                    /*% ripper: if!($2, $4, escape_Qundef($5)) %*/
  return yyVal;
};
states[373] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_if(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-4+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[-2+yyTop].value));
                    p.branch_unless(((Node)yyVal), yyVals[yyTop - count + 2].end);
                    /*% %*/
                    /*% ripper: unless!($2, $4, escape_Qundef($5)) %*/
  return yyVal;
};
states[374] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new WhileNode(yyVals[yyTop - count + 1].start(), p.cond(((Node)yyVals[-2+yyTop].value)), p.makeNullNil(((Node)yyVals[-1+yyTop].value)));
                    /*% %*/
                    /*% ripper: while!($2, $3) %*/
  return yyVal;
};
states[375] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new UntilNode(yyVals[yyTop - count + 1].start(), p.cond(((Node)yyVals[-2+yyTop].value)), p.makeNullNil(((Node)yyVals[-1+yyTop].value)));
                    /*% %*/
                    /*% ripper: until!($2, $3) %*/
  return yyVal;
};
states[376] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.case_labels;
                    p.case_labels = p.getRuntime().getNil();
  return yyVal;
};
states[377] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newCaseNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-4+yyTop].value), ((Node)yyVals[-1+yyTop].value));
                    p.fixpos(((Node)yyVal), ((Node)yyVals[-4+yyTop].value));
                    /*% %*/
                    /*% ripper: case!($2, $5) %*/
  return yyVal;
};
states[378] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.case_labels;
                    p.case_labels = null;
  return yyVal;
};
states[379] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newCaseNode(yyVals[yyTop - count + 1].start(), null, ((Node)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: case!(Qnil, $4) %*/
  return yyVal;
};
states[380] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newPatternCaseNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-3+yyTop].value), ((InNode)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: case!($2, $4) %*/
  return yyVal;
};
states[381] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ForNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-4+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[-2+yyTop].value), p.getCurrentScope(), 111);
                    /*% %*/
                    /*% ripper: for!($2, $4, $5) %*/
  return yyVal;
};
states[382] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.begin_definition("class");
  return yyVal;
};
states[383] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    Node body = p.makeNullNil(((Node)yyVals[-1+yyTop].value));

                    yyVal = new ClassNode(yyVals[yyTop - count + 1].start(), ((Colon3Node)yyVals[-4+yyTop].value), p.getCurrentScope(), body, ((Node)yyVals[-3+yyTop].value), p.src_line());
                    /*% %*/
                    /*% ripper: class!($2, $3, $5) %*/
                    LexContext ctxt = p.getLexContext();
                    p.popCurrentScope();
                    ctxt.in_class = ((LexContext)yyVals[-5+yyTop].value).in_class;
                    ctxt.cant_return = ((LexContext)yyVals[-5+yyTop].value).cant_return;
                    ctxt.shareable_constant_value = ((LexContext)yyVals[-5+yyTop].value).shareable_constant_value;
  return yyVal;
};
states[384] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.begin_definition(null);
  return yyVal;
};
states[385] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    Node body = p.makeNullNil(((Node)yyVals[-1+yyTop].value));

                    yyVal = new SClassNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-4+yyTop].value), p.getCurrentScope(), body, p.src_line());
                    /*% %*/
                    /*% ripper: sclass!($3, $6) %*/
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_def = ((LexContext)yyVals[-6+yyTop].value).in_def;
                    ctxt.in_class = ((LexContext)yyVals[-6+yyTop].value).in_class;
                    ctxt.cant_return = ((LexContext)yyVals[-6+yyTop].value).cant_return;
                    ctxt.shareable_constant_value = ((LexContext)yyVals[-6+yyTop].value).shareable_constant_value;
                    p.popCurrentScope();
  return yyVal;
};
states[386] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.begin_definition("module");
  return yyVal;
};
states[387] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    Node body = p.makeNullNil(((Node)yyVals[-1+yyTop].value));

                    yyVal = new ModuleNode(yyVals[yyTop - count + 1].start(), ((Colon3Node)yyVals[-3+yyTop].value), p.getCurrentScope(), body, p.src_line());
                    /*% %*/
                    /*% ripper: module!($2, $4) %*/
                    p.popCurrentScope();
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_class = ((LexContext)yyVals[-4+yyTop].value).in_class;
                    ctxt.cant_return = ((LexContext)yyVals[-4+yyTop].value).cant_return;
                    ctxt.shareable_constant_value = ((LexContext)yyVals[-4+yyTop].value).shareable_constant_value;
  return yyVal;
};
states[388] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.push_end_expect_token_locations(yyVals[yyTop - count + 1].start());
  return yyVal;
};
states[389] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.restore_defun(((DefHolder)yyVals[-4+yyTop].value));
                    /*%%%*/
                    Node body = p.reduce_nodes(p.remove_begin(p.makeNullNil(((Node)yyVals[-1+yyTop].value))));
                    yyVal = new DefnNode(((DefHolder)yyVals[-4+yyTop].value).line, ((DefHolder)yyVals[-4+yyTop].value).column, ((DefHolder)yyVals[-4+yyTop].value).name, ((ArgsNode)yyVals[-3+yyTop].value), p.getCurrentScope(), body, ProductionState.line(yyVals[yyTop - count + 5].end), p.column(yyVals[yyTop - count + 5].end));
                    if (p.isNextBreak) ((DefnNode)yyVal).setContainsNextBreak();
                    /*% %*/
                    /*% ripper: def!(get_value($1), $2, $4) %*/
                    p.popCurrentScope();
  return yyVal;
};
states[390] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.push_end_expect_token_locations(yyVals[yyTop - count + 1].start());
  return yyVal;
};
states[391] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.restore_defun(((DefHolder)yyVals[-4+yyTop].value));
                    /*%%%*/
                    Node body = p.reduce_nodes(p.remove_begin(p.makeNullNil(((Node)yyVals[-1+yyTop].value))));
                    yyVal = new DefsNode(((DefHolder)yyVals[-4+yyTop].value).line, ((DefHolder)yyVals[-4+yyTop].value).column, (Node) ((DefHolder)yyVals[-4+yyTop].value).singleton, ((DefHolder)yyVals[-4+yyTop].value).name, ((ArgsNode)yyVals[-3+yyTop].value), p.getCurrentScope(), body, ProductionState.line(yyVals[yyTop - count + 5].end), p.column(yyVals[yyTop - count + 5].end));
                    if (p.isNextBreak) ((DefsNode)yyVal).setContainsNextBreak();
                    /* Changed from MRI (no more get_value)*/
                    /*% %*/                    
                    /*% ripper: defs!(AREF($1, 0), AREF($1, 1), AREF($1, 2), $2, $4) %*/
                    p.popCurrentScope();
  return yyVal;
};
states[392] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.isNextBreak = true;
                    yyVal = new BreakNode(yyVals[yyTop - count + 1].start(), NilImplicitNode.NIL);
                    /*% %*/
                    /*% ripper: break!(args_new!) %*/
  return yyVal;
};
states[393] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.isNextBreak = true;
                    yyVal = new NextNode(yyVals[yyTop - count + 1].start(), NilImplicitNode.NIL);
                    /*% %*/
                    /*% ripper: next!(args_new!) %*/
  return yyVal;
};
states[394] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new RedoNode(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: redo! %*/
  return yyVal;
};
states[395] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (!p.getLexContext().in_defined) {
                        LexContext.InRescue rescue = p.getLexContext().in_rescue;
                        if (rescue != LexContext.InRescue.NONE) {
                            switch (rescue) {
                              case BEFORE_RESCUE: p.yyerror("Invalid retry without rescue"); break;
                              case AFTER_RESCUE: /* ok */ break;
                              case AFTER_ELSE: p.yyerror("Invalid retry after else"); break;
                              case AFTER_ENSURE: p.yyerror("Invalid retry after ensure"); break;
                            }
                        }
                    }
                    yyVal = new RetryNode(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: retry! %*/
  return yyVal;
};
states[396] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = ((Node)yyVals[0+yyTop].value) == null ? p.nil() : ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[397] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.token_info_push("begin", yyVals[yyTop - count + 1]);
                    p.push_end_expect_token_locations(yyVals[yyTop - count + 1].start());
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[398] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
                    p.WARN_EOL("if");
                    p.token_info_push("if", yyVals[yyTop - count + 1]);
                    /*
                    TokenInfo tokenInfo = p.getTokenInfo();
                    if (tokenInfo.nonspc &&
                        tokenInfo.next != null && tokenInfo.next.name.equals("else")) {
                      throw new RuntimeException("IMPL ME");
                      }*/
  return yyVal;
};
states[399] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[400] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((NodeExits)yyVals[0+yyTop].value);
  return yyVal;
};
states[401] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[-1+yyTop].value);
  return yyVal;
};
states[402] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[403] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((NodeExits)yyVals[0+yyTop].value);
  return yyVal;
};
states[404] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = (LexContext) p.getLexContext().clone();
                    p.getLexContext().in_rescue = LexContext.InRescue.BEFORE_RESCUE;
                    p.push_end_expect_token_locations(yyVals[yyTop - count + 1].start());
  return yyVal;
};
states[405] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = (LexContext) p.getLexContext().clone();  
                    p.getLexContext().in_rescue = LexContext.InRescue.BEFORE_RESCUE;
  return yyVal;
};
states[406] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
                    p.getLexContext().in_argdef = true;
  return yyVal;
};
states[407] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[408] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[409] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.getLexContext().clone();
                    p.getLexContext().in_rescue = LexContext.InRescue.AFTER_RESCUE;
  return yyVal;
};
states[410] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.getLexContext().clone();
  return yyVal;
};
states[411] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[412] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[413] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[414] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[415] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.compile_error("syntax error, unexpected end-of-input");
  return yyVal;
};
states[416] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = p.getLexContext();
                    if (ctxt.cant_return && !p.dyna_in_block()) {
                        p.compile_error("Invalid return in class/module body");
                    }
  return yyVal;
};
states[417] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = p.getLexContext();
                    if (ctxt.in_defined && !ctxt.in_def && !p.isEval()) {
                        p.compile_error("Invalid yield");
                    }
  return yyVal;
};
states[418] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = null;
                    /*% %*/
  return yyVal;
};
states[419] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = yyVals[yyTop - count + 1].end;
                    /*% %*/
  return yyVal;
};
states[420] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = yyVals[yyTop - count + 2].end;
                    /*% %*/
  return yyVal;
};
states[424] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_if(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    p.branch_elsif(((Node)yyVal), yyVals[yyTop - count + 2].end, ((Node)yyVals[0+yyTop].value) == null ? -1 : yyVals[yyTop - count + 5].start);
                    /*% %*/
                    /*% ripper: elsif!($2, $4, escape_Qundef($5)) %*/
  return yyVal;
};
states[426] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((Node)yyVals[0+yyTop].value) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop].value);
                    /*% %*/
                    /*% ripper: else!($2) %*/
  return yyVal;
};
states[428] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  return yyVal;
};
states[429] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.assignableInCurr(((ByteList)yyVals[0+yyTop].value), NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, $1);
                    %*/
  return yyVal;
};
states[430] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((Node)yyVals[-1+yyTop].value);
                    /*% %*/
                    /*% ripper: mlhs_paren!($2) %*/
  return yyVal;
};
states[431] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newArrayNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add!(mlhs_new!, $1) %*/
  return yyVal;
};
states[432] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add!($1, $3) %*/
  return yyVal;
};
states[433] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[0+yyTop].value), null, null);
                    /*% %*/
                    /*% ripper: $1 %*/
  return yyVal;
};
states[434] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
                    /*% %*/
                    /*% ripper: mlhs_add_star!($1, $3) %*/
  return yyVal;
};
states[435] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-4+yyTop].value), ((Node)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add_post!(mlhs_add_star!($1, $3), $5) %*/
  return yyVal;
};
states[436] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(p.src_line(), null, ((Node)yyVals[0+yyTop].value), null);
                    /*% %*/
                    /*% ripper: mlhs_add_star!(mlhs_new!, $1) %*/
  return yyVal;
};
states[437] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(p.src_line(), null, ((Node)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add_post!(mlhs_add_star!(mlhs_new!, $1), $3) %*/
  return yyVal;
};
states[438] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    ByteList id = yyVals[yyTop - count + 2].id;
                    /*%%%*/
                    yyVal = p.assignableInCurr(id, null);
                    /*%
                      $$ = p.assignable(id, $2);
                    %*/
  return yyVal;
};
states[439] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new StarNode(p.src_line());
                    /*% %*/
                    /*% ripper: Qnil %*/
  return yyVal;
};
states[441] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
                    /*%
                      $$ = p.symbolID(LexingCommon.NIL);
                      %*/
  return yyVal;
};
states[442] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = false;
  return yyVal;
};
states[444] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-3+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[445] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-1+yyTop].value), (ByteList) null, ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[446] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(p.src_line(), null, ((ByteList)yyVals[-1+yyTop].value), ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[447] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), null, (ByteList) null, ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[448] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[449] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(p.src_line(), null, (ByteList) null, (ByteList) null);
  return yyVal;
};
states[450] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new UnnamedRestArgNode(yyVals[yyTop - count + 1].start(), null, p.getCurrentScope().addVariable("*"));
                    /*% %*/
                    /*% ripper: excessed_comma! %*/
  return yyVal;
};
states[451] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-5+yyTop].value), ((ListNode)yyVals[-3+yyTop].value), ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[452] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-7+yyTop].value), ((ListNode)yyVals[-5+yyTop].value), ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[453] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[454] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-5+yyTop].value), ((ListNode)yyVals[-3+yyTop].value), null, ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[455] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-3+yyTop].value), null, ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[456] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-1+yyTop].value), null, ((RestArgNode)yyVals[0+yyTop].value), null, (ArgsTailHolder) null);
  return yyVal;
};
states[457] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-5+yyTop].value), null, ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[458] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-1+yyTop].value), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[459] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((ListNode)yyVals[-3+yyTop].value), ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[460] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((ListNode)yyVals[-5+yyTop].value), ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[461] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((ListNode)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[462] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((ListNode)yyVals[-3+yyTop].value), null, ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[463] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[464] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[465] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[466] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_args(p.src_line(), null, null, null, null, (ArgsTailHolder) null);
                    /*%
                      $$ = null;
                      %*/
  return yyVal;
};
states[467] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setCommandStart(true);
                    yyVal = ((ArgsNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[468] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.ordinalMaxNumParam();
                    p.getLexContext().in_argdef = false;
                    /*%%%*/
                    yyVal = p.new_args(p.src_line(), null, null, null, null, (ArgsTailHolder) null);
                    /*% %*/
                    /*% ripper: block_var!(params!(Qnil,Qnil,Qnil,Qnil,Qnil,Qnil,Qnil), escape_Qundef($2)) %*/
  return yyVal;
};
states[469] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.ordinalMaxNumParam();
                    p.getLexContext().in_argdef = false;
                    /*%%%*/
                    yyVal = ((ArgsNode)yyVals[-2+yyTop].value);
                    /*% %*/
                    /*% ripper: block_var!(escape_Qundef($2), escape_Qundef($3)) %*/
  return yyVal;
};
states[470] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[471] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = null;
                    /*% %*/
                    /*% ripper: $3 %*/
  return yyVal;
};
states[472] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
                    /*% ripper[brace]: rb_ary_new3(1, get_value($1)) %*/
  return yyVal;
};
states[473] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
                    /*% ripper[brace]: rb_ary_push($1, get_value($3)) %*/
  return yyVal;
};
states[474] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.new_bv(yyVals[yyTop - count + 1].id);
                    /*% ripper: get_value($1) %*/
  return yyVal;
};
states[475] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[476] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.resetMaxNumParam();
  return yyVal;
};
states[477] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.numparam_push();
  return yyVal;
};
states[478] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.it_id();
                    p.set_it_id(null);
  return yyVal;
};
states[479] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pushBlockScope();
                    yyVal = p.getLeftParenBegin();
                    p.setLeftParenBegin(p.getParenNest());
  return yyVal;
};
states[480] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getCmdArgumentState().push0();
                    /*%%%*/
                    yyVal = p.takeLambdaArgsStart();
                    /*% %*/
  return yyVal;
};
states[481] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node it_id = p.it_id();
                    int max_numparam = p.restoreMaxNumParam(((Integer)yyVals[-6+yyTop].value));
                    p.set_it_id(((Node)yyVals[-4+yyTop].value));
                    p.getCmdArgumentState().pop();
                    /* Changed from MRI args_with_numbered put into parser codepath and not used by ripper (since it is just a passthrough method and types do not match).*/
                    /*%%%*/
                    ArgsNode args = p.args_with_numbered(((ArgsNode)yyVals[-2+yyTop].value), max_numparam, it_id);
                    yyVal = new LambdaNode(yyVals[yyTop - count + 1].start(), args, ((Node)yyVals[0+yyTop].value), p.getCurrentScope(), p.src_line());
                    p.block_span(((LambdaNode)yyVal), ((Long)yyVals[-1+yyTop].value), yyVals[yyTop - count + 9].end);
                    p.lambda_operator(((LambdaNode)yyVal), yyVals[yyTop - count + 1].start);
                    /*% %*/
                    /*% ripper: lambda!($5, $7) %*/
                    p.setLeftParenBegin(((Integer)yyVals[-7+yyTop].value));
                    p.numparam_pop(((Node)yyVals[-5+yyTop].value));
                    p.popCurrentScope();
  return yyVal;
};
states[482] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = false;
                    /*%%%*/
                    yyVal = ((ArgsNode)yyVals[-2+yyTop].value);
                    p.ordinalMaxNumParam();
                    p.setLambdaArgsStart(yyVals[yyTop - count + 1].start);
                    /*% %*/
                    /*% ripper: paren!($2) %*/
  return yyVal;
};
states[483] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = false;
                    /*%%%*/
                    if (!p.isArgsInfoEmpty(((ArgsNode)yyVals[0+yyTop].value))) {
                        p.ordinalMaxNumParam();
                        p.setLambdaArgsStart(yyVals[yyTop - count + 1].start);
                    } else {
                        /* No parameter list: the lambda's source starts just after '->', as in MRI. That is the*/
                        /* end of the previous token, which is where an empty production's location ends.*/
                        p.setLambdaArgsStart(yyVals[yyTop - count + 1].end);
                    }
                    /*% %*/
                    yyVal = ((ArgsNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[484] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.token_info_pop("}", yyVals[yyTop - count + 1]);
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[485] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.push_end_expect_token_locations(yyVals[yyTop - count + 1].start());
  return yyVal;
};
states[486] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[487] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IterNode)yyVals[-1+yyTop].value);
                    /*%%%*/
                    ((IterNode)yyVals[-1+yyTop].value).setLine(yyVals[yyTop - count + 1].end());
                    p.block_span(((IterNode)yyVals[-1+yyTop].value), yyVals[yyTop - count + 1].start, yyVals[yyTop - count + 3].end);
                    /*% %*/
  return yyVal;
};
states[488] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /* Workaround for JRUBY-2326 (MRI does not enter this production for some reason)*/
                    /*%%%*/
                    if (((Node)yyVals[-1+yyTop].value) instanceof YieldNode) {
                        p.compile_error("block given to yield");
                    }
                    if (((Node)yyVals[-1+yyTop].value) instanceof BlockAcceptingNode && ((BlockAcceptingNode)yyVals[-1+yyTop].value).getIterNode() instanceof BlockPassNode) {
                        p.compile_error("both block arg and actual block given.");
                    }
                    if (((Node)yyVals[-1+yyTop].value) instanceof NonLocalControlFlowNode) {
                        ((BlockAcceptingNode) ((NonLocalControlFlowNode)yyVals[-1+yyTop].value).getValueNode()).setIterNode(((IterNode)yyVals[0+yyTop].value));
                    } else {
                        ((BlockAcceptingNode)yyVals[-1+yyTop].value).setIterNode(((IterNode)yyVals[0+yyTop].value));
                    }
                    yyVal = ((Node)yyVals[-1+yyTop].value);
                    ((Node)yyVal).setLine(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: method_add_block!($1, $2) %*/
  return yyVal;
};
states[489] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].start());
                    p.safe_navigation_end(((Node)yyVal), yyVals[yyTop - count + 3].end, yyVals[yyTop - count + 4].end, true);
                    /*% %*/
                    /*% ripper: opt_event(:method_add_arg!, call!($1, $2, $3), $4) %*/
  return yyVal;
};
states[490] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value), yyVals[yyTop - count + 3].start());
                    p.span(((Node)yyVal), yyVals[yyTop - count + 1].start, yyVals[yyTop - count + 4].end);
                    p.safe_navigation_end(((Node)yyVal), yyVals[yyTop - count + 3].end, yyVals[yyTop - count + 4].end, true);
                    /*% %*/
                    /*% ripper: opt_event(:method_add_block!, command_call!($1, $2, $3, $4), $5) %*/
  return yyVal;
};
states[491] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value), yyVals[yyTop - count + 3].start());
                    p.span(((Node)yyVal), yyVals[yyTop - count + 1].start, yyVals[yyTop - count + 4].end);
                    p.safe_navigation_end(((Node)yyVal), yyVals[yyTop - count + 3].end, yyVals[yyTop - count + 4].end, false);
                    /*% %*/
                    /*% ripper: method_add_block!(command_call!($1, $2, $3, $4), $5) %*/
  return yyVal;
};
states[492] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), LexingCommon.CALL, ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].start());
                    p.safe_navigation_end(((Node)yyVal), yyVals[yyTop - count + 3].end, yyVals[yyTop - count + 3].end, true);
                    /*% %*/
                    /*% ripper: method_add_arg!(call!($1, $2, ID2VAL(idCall)), $3) %*/
  return yyVal;
};
states[493] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.frobnicate_fcall_args(((FCallNode)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
                    yyVal = ((FCallNode)yyVals[-1+yyTop].value);
                    /*% %*/
                    /*% ripper: method_add_arg!(fcall!($1), $2) %*/
  return yyVal;
};
states[494] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].start());
                    p.safe_navigation_end(((Node)yyVal), yyVals[yyTop - count + 3].end, yyVals[yyTop - count + 4].end, true);
                    /*% %*/
                    /*% ripper: opt_event(:method_add_arg!, call!($1, $2, $3), $4) %*/
  return yyVal;
};
states[495] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
                    /*% %*/
                    /*% ripper: method_add_arg!(call!($1, ID2VAL(idCOLON2), $3), $4) %*/
  return yyVal;
};
states[496] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value), null, null);
                    /*% %*/
                    /*% ripper: call!($1, ID2VAL(idCOLON2), $3) %*/
  return yyVal;
};
states[497] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), LexingCommon.CALL, ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].start());
                    p.safe_navigation_end(((Node)yyVal), yyVals[yyTop - count + 3].end, yyVals[yyTop - count + 3].end, true); /* o&.(): the parentheses are the message*/
                    /*% %*/
                    /*% ripper: method_add_arg!(call!($1, $2, ID2VAL(idCall)), $3) %*/
  return yyVal;
};
states[498] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-2+yyTop].value), LexingCommon.CALL, ((Node)yyVals[0+yyTop].value), null);
                    /*% %*/
                    /*% ripper: method_add_arg!(call!($1, ID2VAL(idCOLON2), ID2VAL(idCall)), $3) %*/
  return yyVal;
};
states[499] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_super(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: super!($2) %*/
  return yyVal;
};
states[500] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ZSuperNode(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: zsuper! %*/
  return yyVal;
};
states[501] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (((Node)yyVals[-3+yyTop].value) instanceof SelfNode) {
                        yyVal = p.new_fcall(LBRACKET_RBRACKET);
                        p.frobnicate_fcall_args(((FCallNode)yyVal), ((Node)yyVals[-1+yyTop].value), null);
                    } else {
                        yyVal = p.new_call(((Node)yyVals[-3+yyTop].value), LBRACKET_RBRACKET, ((Node)yyVals[-1+yyTop].value), null);
                    }
                    /*% %*/
                    /*% ripper: aref!($1, escape_Qundef($3)) %*/
  return yyVal;
};
states[502] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IterNode)yyVals[-1+yyTop].value);
                    /*%%%*/
                    ((IterNode)yyVals[-1+yyTop].value).setLine(yyVals[yyTop - count + 1].end());
                    p.block_span(((IterNode)yyVals[-1+yyTop].value), yyVals[yyTop - count + 1].start, yyVals[yyTop - count + 3].end);
                    /*% %*/
  return yyVal;
};
states[503] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IterNode)yyVals[-1+yyTop].value);
                    /*%%%*/
                    ((IterNode)yyVals[-1+yyTop].value).setLine(yyVals[yyTop - count + 1].end());
                    p.block_span(((IterNode)yyVals[-1+yyTop].value), yyVals[yyTop - count + 1].start, yyVals[yyTop - count + 3].end);
                    /*% %*/
  return yyVal;
};
states[504] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pushBlockScope();
  return yyVal;
};
states[505] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node it_id = p.it_id();
                    int max_numparam = p.restoreMaxNumParam(((Integer)yyVals[-5+yyTop].value));
                    p.set_it_id(((Node)yyVals[-3+yyTop].value));
                    /* Changed from MRI args_with_numbered put into parser codepath and not used by ripper (since it is just a passthrough method and types do not match).*/
                    /*%%%*/
                    ArgsNode args = p.args_with_numbered(((ArgsNode)yyVals[-1+yyTop].value), max_numparam, it_id);
                    yyVal = new IterNode(yyVals[yyTop - count + 1].start(), args, ((Node)yyVals[0+yyTop].value), p.getCurrentScope(), p.src_line());
                    /*% %*/
                    /*% ripper: brace_block!(escape_Qundef($6), $7) %*/
                    p.numparam_pop(((Node)yyVals[-4+yyTop].value));
                    p.popCurrentScope();                    
  return yyVal;
};
states[506] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pushBlockScope();
                    p.getCmdArgumentState().push0();
  return yyVal;
};
states[507] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node it_id = p.it_id();
                    int max_numparam = p.restoreMaxNumParam(((Integer)yyVals[-5+yyTop].value));
                    p.set_it_id(((Node)yyVals[-3+yyTop].value));
                    /* Changed from MRI args_with_numbered put into parser codepath and not used by ripper (since it is just a passthrough method and types do not match).*/
                    /*%%%*/
                    ArgsNode args = p.args_with_numbered(((ArgsNode)yyVals[-1+yyTop].value), max_numparam, it_id);
                    yyVal = new IterNode(yyVals[yyTop - count + 1].start(), args, ((Node)yyVals[0+yyTop].value), p.getCurrentScope(), p.src_line());
                    /*% %*/
                    /*% ripper: do_block!(escape_Qundef($6), $7) %*/
                    p.getCmdArgumentState().pop();
                    p.numparam_pop(((Node)yyVals[-4+yyTop].value));
                    p.popCurrentScope();
  return yyVal;
};
states[508] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.check_literal_when(((Node)yyVals[0+yyTop].value));
                    yyVal = p.newArrayNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: args_add!(args_new!, $1) %*/
  return yyVal;
};
states[509] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newSplatNode(((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: args_add_star!(args_new!, $2) %*/
  return yyVal;
};
states[510] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.check_literal_when(((Node)yyVals[0+yyTop].value));
                    yyVal = p.last_arg_append(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: args_add!($1, $3) %*/
  return yyVal;
};
states[511] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.rest_arg_append(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: args_add_star!($1, $4) %*/
  return yyVal;
};
states[512] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newWhenNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    p.branch_when(((Node)yyVal), yyVals[yyTop - count + 1].start, ((Long)yyVals[-2+yyTop].value), yyVals[yyTop - count + 2].end, ((Node)yyVals[0+yyTop].value) == null || ((Node)yyVals[0+yyTop].value) instanceof WhenNode ? -1 : yyVals[yyTop - count + 5].start);
                    /*% %*/
                    /*% ripper: when!($2, $4, escape_Qundef($5)) %*/
  return yyVal;
};
states[515] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.push_pvtbl();
  return yyVal;
};
states[516] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.push_pktbl();
  return yyVal;
};
states[517] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.getLexContext().clone();
                    p.setState(EXPR_BEG|EXPR_LABEL);
                    p.setCommandStart(false);
                    p.getLexContext().in_kwarg = true;
                    p.getLexContext().in_alt_pattern = false;
                    p.getLexContext().capture_in_pattern = false;
  return yyVal;
};
states[518] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pvtbl(((Set)yyVals[-3+yyTop].value));
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    p.getLexContext().in_kwarg = ((LexContext)yyVals[-4+yyTop].value).in_kwarg;
                    p.getLexContext().in_alt_pattern = ((LexContext)yyVals[-4+yyTop].value).in_alt_pattern;
                    p.getLexContext().capture_in_pattern = ((LexContext)yyVals[-4+yyTop].value).capture_in_pattern;
  return yyVal;
};
states[519] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newIn(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-4+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    p.branch_in(((Node)yyVal), yyVals[yyTop - count + 1].start, ((Long)yyVals[-3+yyTop].value), yyVals[yyTop - count + 5].end, ((Node)yyVals[0+yyTop].value) == null || ((Node)yyVals[0+yyTop].value) instanceof InNode ? -1 : yyVals[yyTop - count + 9].start);
                    /*% %*/
                    /*% ripper: in!($5, $8, escape_Qundef($9)) %*/
  return yyVal;
};
states[521] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((InNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[523] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_if(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value), ((Node)yyVals[-2+yyTop].value), null);
                    p.fixpos(((Node)yyVal), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: if_mod!($3, $1) %*/
  return yyVal;
};
states[524] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_if(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value), null, ((Node)yyVals[-2+yyTop].value));
                    p.fixpos(((Node)yyVal), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: unless_mod!($3, $1) %*/
  return yyVal;
};
states[526] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, ((Node)yyVals[-1+yyTop].value),
                                                   p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, true, null, null));
  return yyVal;
};
states[527] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, ((Node)yyVals[-2+yyTop].value), ((ArrayPatternNode)yyVals[0+yyTop].value));
                    /*%%%*/
                    p.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].start());
                    /*% %*/
  return yyVal;
};
states[528] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_find_pattern(null, ((FindPatternNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[529] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, null, ((ArrayPatternNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[530] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern(null, ((HashPatternNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[532] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new HashNode(yyVals[yyTop - count + 1].start(), new KeyValuePair(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value)));
                    /*% %*/
                    /*% ripper: binary!($1, STATIC_ID2SYM((id_assoc)), $3) %*/
  return yyVal;
};
states[534] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_alt_pattern = true;
  return yyVal;
};
states[535] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (p.getLexContext().capture_in_pattern) {
                        p.yyerror("alternative pattern after variable capture");
                    }
                    p.getLexContext().in_alt_pattern = false;
                    /*%%%*/
                    yyVal = p.logop(((Node)yyVals[-3+yyTop].value), OR, ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: binary!($1, STATIC_ID2SYM(idOr), $4) %*/
  return yyVal;
};
states[537] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Set)yyVals[0+yyTop].value);
                    /*% ripper: get_value($:2); %*/
  return yyVal;
};
states[538] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Set)yyVals[0+yyTop].value);
                    /*% ripper: get_value($:2); %*/
  return yyVal;
};
states[541] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-3+yyTop].value), null, ((ArrayPatternNode)yyVals[-1+yyTop].value));
                    /*%%%*/
                    p.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].start());
                    /*% %*/
  return yyVal;
};
states[542] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_find_pattern(((Node)yyVals[-3+yyTop].value), ((FindPatternNode)yyVals[-1+yyTop].value));
                    /*%%%*/
                    p.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].start());
                    /*% %*/
  return yyVal;
};
states[543] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_hash_pattern(((Node)yyVals[-3+yyTop].value), ((HashPatternNode)yyVals[-1+yyTop].value));
                    /*%%%*/
                    p.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].start());
                    /*% %*/
  return yyVal;
};
states[544] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-2+yyTop].value), null,
                                                    p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, false, null, null));
  return yyVal;
};
states[545] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-3+yyTop].value), null, ((ArrayPatternNode)yyVals[-1+yyTop].value));
                    /*%%%*/
                    p.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].start());
                    /*% %*/
  return yyVal;
};
states[546] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_find_pattern(((Node)yyVals[-3+yyTop].value), ((FindPatternNode)yyVals[-1+yyTop].value));
                    /*%%%*/
                    p.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].start());
                    /*% %*/
  return yyVal;
};
states[547] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_hash_pattern(((Node)yyVals[-3+yyTop].value), ((HashPatternNode)yyVals[-1+yyTop].value));
                    /*%%%*/
                    p.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].start());
                    /*% %*/
  return yyVal;
};
states[548] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-2+yyTop].value), null,
                            p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, false, null, null));
  return yyVal;
};
states[549] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, null, ((ArrayPatternNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[550] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_find_pattern(null, ((FindPatternNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[551] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, null,
                            p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, false, null, null));
  return yyVal;
};
states[552] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_kwarg = false;
  return yyVal;
};
states[553] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-4+yyTop].value));
                    p.getLexContext().in_kwarg = ((LexContext)yyVals[-3+yyTop].value).in_kwarg;
                    yyVal = p.new_hash_pattern(null, ((HashPatternNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[554] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern(null, p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), p.none(), null));
  return yyVal;
};
states[555] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[556] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    ListNode preArgs = p.newArrayNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), preArgs, false, null, null);
                    /* JRuby Changed*/
                    /*% 
                        $$ = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), p.new_array($1), false, null, null);
                    %*/
  return yyVal;
};
states[557] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[0+yyTop].value), true, null, null);
  return yyVal;
};
states[558] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), p.list_concat(((ListNode)yyVals[-1+yyTop].value), ((ListNode)yyVals[0+yyTop].value)), false, null, null);
                    /* JRuby Changed*/
                    /*%
			RubyArray pre_args = $1.concat($2);
			$$ = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), pre_args, false, null, null);
                    %*/
  return yyVal;
};
states[559] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-1+yyTop].value), true, ((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[560] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-3+yyTop].value), true, ((ByteList)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[561] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ArrayPatternNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[562] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ListNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[563] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.list_concat(((ListNode)yyVals[-2+yyTop].value), ((ListNode)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_concat($1, get_value($2)) %*/
  return yyVal;
};
states[564] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, true, ((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[565] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, true, ((ByteList)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[566] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_find_pattern_tail(yyVals[yyTop - count + 1].start(), ((ByteList)yyVals[-4+yyTop].value), ((ListNode)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[567] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.error_duplicate_pattern_variable(yyVals[yyTop - count + 2].id);
                    /*%%%*/
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
                    /*% %*/
                    /*% p.assignable(yyVals[yyTop - count + 2].id, p.var_field(p.get_value($2))); %*/
  return yyVal;
};
states[568] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
                    /*% ripper: var_field(p, Qnil) %*/
  return yyVal;
};
states[570] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.list_concat(((ListNode)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_concat($1, get_value($3)) %*/
  return yyVal;
};
states[571] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newArrayNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_new_from_args(1, get_value($1)) %*/
  return yyVal;
};
states[572] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), ((HashNode)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[573] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), ((HashNode)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[574] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), ((HashNode)yyVals[-1+yyTop].value), null);
  return yyVal;
};
states[575] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), null, ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[576] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new HashNode(yyVals[yyTop - count + 1].start(), ((KeyValuePair)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper[brace]: rb_ary_new_from_args(1, $1) %*/
  return yyVal;
};
states[577] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    ((HashNode)yyVals[-2+yyTop].value).add(((KeyValuePair)yyVals[0+yyTop].value));
                    yyVal = ((HashNode)yyVals[-2+yyTop].value);
                    /*% %*/
                    /*% ripper: rb_ary_push($1, $3) %*/
  return yyVal;
};
states[578] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.error_duplicate_pattern_key(yyVals[yyTop - count + 1].id);
                    /*%%%*/
                    Node label = p.asSymbol(yyVals[yyTop - count + 1].start(), ((ByteList)yyVals[-1+yyTop].value));

                    yyVal = new KeyValuePair(label, ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_new_from_args(2, get_value($1), get_value($2)) %*/
  return yyVal;
};
states[579] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.error_duplicate_pattern_key(yyVals[yyTop - count + 1].id);
                    if (yyVals[yyTop - count + 1].id != null && !p.is_local_id(yyVals[yyTop - count + 1].id)) {
                        p.yyerror("key must be valid as local variables");
                    }
                    p.error_duplicate_pattern_variable(yyVals[yyTop - count + 1].id);
                    /*%%%*/

                    Node label = p.asSymbol(yyVals[yyTop - count + 1].start(), ((ByteList)yyVals[0+yyTop].value));
                    yyVal = new KeyValuePair(label, p.assignableLabelOrIdentifier(((ByteList)yyVals[0+yyTop].value), null));
                    /*% %*/
                    /*% ripper: rb_ary_new_from_args(2, get_value($1), Qnil) %*/
  return yyVal;
};
states[581] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
		    /*%%%*/
                    if (((Node)yyVals[-1+yyTop].value) == null || ((Node)yyVals[-1+yyTop].value) instanceof StrNode) {
                        yyVal = ((StrNode)yyVals[-1+yyTop].value).getValue();
                    }
		    /*%
                      if (true) {
                        $$ = $2;
                      }
		    %*/
                    else {
                        p.yyerror("symbol literal with interpolation is not allowed");
                        yyVal = null;
                    }
  return yyVal;
};
states[582] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[583] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = STAR_STAR;
                    /*%
                       $$ = null;
                    %*/
  return yyVal;
};
states[584] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                       yyVal = KWNOREST;
                    /*%
                       $$ = p.symbolID(KWNOREST);
                    %*/
  return yyVal;
};
states[585] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[586] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[588] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[-2+yyTop].value));
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[-2+yyTop].value) instanceof FixnumNode && ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), p.makeNullNil(((Node)yyVals[-2+yyTop].value)), p.makeNullNil(((Node)yyVals[0+yyTop].value)), false, isLiteral);
                    /*% %*/
                    /*% ripper: dot2!($1, $3) %*/
  return yyVal;
};
states[589] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[-2+yyTop].value));
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[-2+yyTop].value) instanceof FixnumNode && ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), p.makeNullNil(((Node)yyVals[-2+yyTop].value)), p.makeNullNil(((Node)yyVals[0+yyTop].value)), true, isLiteral);
                    /*% %*/
                    /*% ripper: dot3!($1, $3) %*/
  return yyVal;
};
states[590] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[-1+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[-1+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), p.makeNullNil(((Node)yyVals[-1+yyTop].value)), NilImplicitNode.NIL, false, isLiteral);
                    /*% %*/
                    /*% ripper: dot2!($1, Qnil) %*/
  return yyVal;
};
states[591] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[-1+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[-1+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), p.makeNullNil(((Node)yyVals[-1+yyTop].value)), NilImplicitNode.NIL, true, isLiteral);
                    /*% %*/
                    /*% ripper: dot3!($1, Qnil) %*/
  return yyVal;
};
states[595] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), NilImplicitNode.NIL, p.makeNullNil(((Node)yyVals[0+yyTop].value)), false, isLiteral);
                    /*% %*/
                    /*% ripper: dot2!(Qnil, $2) %*/
  return yyVal;
};
states[596] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), NilImplicitNode.NIL, p.makeNullNil(((Node)yyVals[0+yyTop].value)), true, isLiteral);
                    /*% %*/
                    /*% ripper: dot3!(Qnil, $2) %*/
  return yyVal;
};
states[601] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[602] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[603] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[604] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[605] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new NilNode(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[606] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new SelfNode(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[607] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new TrueNode(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[608] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new FalseNode(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[609] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new FileNode(yyVals[yyTop - count + 1].start(), new ByteList(p.getFile().getBytes(),
                    p.getRuntime().getEncodingService().getLocaleEncoding()));
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[610] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new FixnumNode(yyVals[yyTop - count + 1].start(), yyVals[yyTop - count + 1].start()+1);
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[611] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new EncodingNode(yyVals[yyTop - count + 1].start(), p.getEncoding());
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[612] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((LambdaNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[613] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.error_duplicate_pattern_variable(((ByteList)yyVals[0+yyTop].value));
                    yyVal = p.assignableLabelOrIdentifier(((ByteList)yyVals[0+yyTop].value), null);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[614] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    Node n = p.gettable(((ByteList)yyVals[0+yyTop].value));
                    if (!(n instanceof LocalVarNode || n instanceof DVarNode)) {
                        p.compile_error("" + ((ByteList)yyVals[0+yyTop].value) + ": no such local variable");
                    }
                    yyVal = n;
                    /*% %*/
                    /*% ripper: var_ref!($2) %*/
  return yyVal;
};
states[615] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.gettable(((ByteList)yyVals[0+yyTop].value));
                    if (yyVal == null) yyVal = new BeginNode(yyVals[yyTop - count + 1].start(), NilImplicitNode.NIL);
                    /*% %*/
                    /*% ripper: var_ref!($2) %*/
  return yyVal;
};
states[616] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new BeginNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: begin!($3) %*/
  return yyVal;
};
states[617] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_colon3(yyVals[yyTop - count + 1].start(), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: top_const_ref!($2) %*/
  return yyVal;
};
states[618] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_colon2(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: const_path_ref!($1, $3) %*/
  return yyVal;
};
states[619] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ConstNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)));
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[620] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    Node node;
                    if (((Node)yyVals[-3+yyTop].value) != null) {
                        node = p.appendToBlock(node_assign(((Node)yyVals[-3+yyTop].value), new GlobalVarNode(yyVals[yyTop - count + 1].start(), p.symbolID(DOLLAR_BANG)), null), p.makeNullNil(((Node)yyVals[-1+yyTop].value)));
                        if (((Node)yyVals[-1+yyTop].value) != null) {
                            node.setLine(yyVals[yyTop - count + 1].start());
                        }
                    } else {
                        node = ((Node)yyVals[-1+yyTop].value);
                    }
                    Node body = p.makeNullNil(node);
                    yyVal = new RescueBodyNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-4+yyTop].value), body, ((RescueBodyNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rescue!(escape_Qundef($2), escape_Qundef($3), escape_Qundef($5), escape_Qundef($6)) %*/
  return yyVal;
};
states[621] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null; 
  return yyVal;
};
states[622] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newArrayNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_new3(1, get_value($1)) %*/
  return yyVal;
};
states[623] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.splat_array(((Node)yyVals[0+yyTop].value));
                    if (yyVal == null) yyVal = ((Node)yyVals[0+yyTop].value); /* ArgsCat or ArgsPush*/
                    /*% %*/
                    /*% ripper: $1 %*/
  return yyVal;
};
states[625] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[627] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.getLexContext().in_rescue = ((LexContext)yyVals[-1+yyTop].value).in_rescue;
                    yyVal = ((Node)yyVals[0+yyTop].value);
                    /*% %*/
                    /*% ripper: ensure!($2) %*/
  return yyVal;
};
states[629] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((NumericNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[630] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[631] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((Node)yyVals[0+yyTop].value) instanceof EvStrNode ? new DStrNode(yyVals[yyTop - count + 1].start(), p.getEncoding()).add(((Node)yyVals[0+yyTop].value)) : ((Node)yyVals[0+yyTop].value);
                    /*% %*/
                    /*% ripper: $1 %*/
  return yyVal;
};
states[632] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((StrNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[633] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[634] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: string_concat!($1, $2) %*/
  return yyVal;
};
states[635] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.heredoc_dedent(((Node)yyVals[-1+yyTop].value));
                    if (((Node)yyVals[-1+yyTop].value) != null) p.nd_set_loc(((Node)yyVals[-1+yyTop].value), yyVals[yyTop - count + 1].start());
                    yyVal = ((Node)yyVals[-1+yyTop].value);
                    /*% %*/
                    /*% ripper: string_literal!(heredoc_dedent(p, $2)) %*/
  return yyVal;
};
states[636] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    int line = yyVals[yyTop - count + 1].start();

                    p.heredoc_dedent(((Node)yyVals[-1+yyTop].value));

                    if (((Node)yyVals[-1+yyTop].value) == null) {
                        yyVal = new XStrNode(line, null, StringSupport.CR_7BIT);
                    } else if (((Node)yyVals[-1+yyTop].value) instanceof StrNode) {
                        yyVal = new XStrNode(line, (ByteList) ((StrNode)yyVals[-1+yyTop].value).getValue().clone(), ((StrNode)yyVals[-1+yyTop].value).getCodeRange());
                    } else if (((Node)yyVals[-1+yyTop].value) instanceof DStrNode) {
                        yyVal = new DXStrNode(line, ((DStrNode)yyVals[-1+yyTop].value));

                        ((Node)yyVal).setLine(line);
                    } else {
                        yyVal = new DXStrNode(line).add(((Node)yyVals[-1+yyTop].value));
                    }
                    /*% %*/
                    /*% ripper: xstring_literal!(heredoc_dedent(p, $2)) %*/
  return yyVal;
};
states[637] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_regexp(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-1+yyTop].value), ((RegexpNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[638] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  return yyVal;
};
states[640] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.make_list(((ListNode)yyVals[-1+yyTop].value), yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: array!($3) %*/
  return yyVal;
};
states[641] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                     yyVal = new ArrayNode(p.src_line());
                    /*% %*/
                    /*% ripper: words_new! %*/
  return yyVal;
};
states[642] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                     yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[-1+yyTop].value) instanceof EvStrNode ? new DStrNode(yyVals[yyTop - count + 2].start(), p.getEncoding()).add(((Node)yyVals[-1+yyTop].value)) : ((Node)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: words_add!($1, $2) %*/
  return yyVal;
};
states[643] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((Node)yyVals[0+yyTop].value);
                     /*% ripper[brace]: word_add!(word_new!, $1) %*/
  return yyVal;
};
states[644] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: word_add!($1, $2) %*/
  return yyVal;
};
states[645] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.make_list(((ListNode)yyVals[-1+yyTop].value), yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: array!($3) %*/
  return yyVal;
};
states[646] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ArrayNode(p.src_line());
                    /*% %*/
                    /*% ripper: symbols_new! %*/
  return yyVal;
};
states[647] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[-1+yyTop].value) instanceof EvStrNode ? new DSymbolNode(yyVals[yyTop - count + 2].start()).add(((Node)yyVals[-1+yyTop].value)) : p.asSymbol(yyVals[yyTop - count + 2].start(), ((Node)yyVals[-1+yyTop].value)));
                    /*% %*/
                    /*% ripper: symbols_add!($1, $2) %*/
  return yyVal;
};
states[648] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.make_list(((ListNode)yyVals[-1+yyTop].value), yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: array!($3) %*/
  return yyVal;
};
states[649] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.make_list(((ListNode)yyVals[-1+yyTop].value), yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: array!($3) %*/
  return yyVal;
};
states[650] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ArrayNode(p.src_line());
                    /*% %*/
                    /*% ripper: qwords_new! %*/
  return yyVal;
};
states[651] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: qwords_add!($1, $2) %*/
  return yyVal;
};
states[652] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ArrayNode(p.src_line());
                    /*% %*/
                    /*% ripper: qsymbols_new! %*/
  return yyVal;
};
states[653] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(p.asSymbol(yyVals[yyTop - count + 2].start(), ((Node)yyVals[-1+yyTop].value)));
                    /*% %*/
                    /*% ripper: qsymbols_add!($1, $2) %*/
  return yyVal;
};
states[654] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    ByteList aChar = ByteList.create("");
                    aChar.setEncoding(p.getEncoding());
                    yyVal = p.createStr(aChar, 0);
                    /*% ripper: string_content! %*/
  return yyVal;
};
states[655] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: string_add!($1, $2) %*/
                    /* JRuby changed (removed)*/
  return yyVal;
};
states[656] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    ByteList aChar = ByteList.create("");
                    aChar.setEncoding(p.getEncoding());
                    yyVal = p.createStr(aChar, 0);
                    /*% %*/
                    /*% ripper: xstring_new! %*/
  return yyVal;
};
states[657] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: xstring_add!($1, $2) %*/
  return yyVal;
};
states[658] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = null;
                    /*% %*/
                    /*% ripper: regexp_new! %*/
  return yyVal;
};
states[659] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /* FIXME: mri is different here.*/
                    /*%%%*/
                    yyVal = p.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /* JRuby changed*/
                    /*% 
			$$ = p.dispatch("on_regexp_add", $1, $2);
                    %*/
  return yyVal;
};
states[660] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
		    /*% ripper[brace]: ripper_new_yylval(p, 0, get_value($1), $1) %*/
  return yyVal;
};
states[661] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.getStrTerm();
                    p.setStrTerm(null);
                    p.setState(EXPR_BEG);
  return yyVal;
};
states[662] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setStrTerm(((StrTerm)yyVals[-1+yyTop].value));
                   /*%%%*/
                    yyVal = new EvStrNode(yyVals[yyTop - count + 3].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: string_dvar!($3) %*/
  return yyVal;
};
states[663] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = p.getStrTerm();
                   p.setStrTerm(null);
                   p.getConditionState().push0();
                   p.getCmdArgumentState().push0();
  return yyVal;
};
states[664] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = p.getState();
                   p.setState(EXPR_BEG);
  return yyVal;
};
states[665] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = p.getBraceNest();
                   p.setBraceNest(0);
  return yyVal;
};
states[666] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = p.getHeredocIndent();
                   p.setHeredocIndent(0);
  return yyVal;
};
states[667] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   p.getConditionState().pop();
                   p.getCmdArgumentState().pop();
                   p.setStrTerm(((StrTerm)yyVals[-5+yyTop].value));
                   p.setState(((Integer)yyVals[-4+yyTop].value));
                   p.setBraceNest(((Integer)yyVals[-3+yyTop].value));
                   p.setHeredocIndent(((Integer)yyVals[-2+yyTop].value));
                   p.setHeredocLineIndent(-1);

                   /*%%%*/
                   if (((Node)yyVals[-1+yyTop].value) != null) p.nd_unset_fl_newline(((Node)yyVals[-1+yyTop].value));
                   yyVal = p.newEvStrNode(yyVals[yyTop - count + 6].start(), ((Node)yyVals[-1+yyTop].value));
                   /*% %*/
                   /*% ripper: string_embexpr!($6) %*/
  return yyVal;
};
states[670] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.gettable(((ByteList)yyVals[0+yyTop].value));
                    if (yyVal == null) yyVal = p.NEW_ERROR(yyVals[yyTop - count + 1]);
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[674] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_END);
                    /*%%%*/
                    yyVal = p.asSymbol(p.src_line(), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: symbol_literal!(symbol!($2)) %*/
  return yyVal;
};
states[677] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_END);

                    /*%%%*/
                    /* DStrNode: :"some text #{some expression}"*/
                    /* StrNode: :"some text"*/
                    /* EvStrNode :"#{some expression}"*/
                    /* Ruby 1.9 allows empty strings as symbols*/
                    if (((Node)yyVals[-1+yyTop].value) == null) {
                        yyVal = p.asSymbol(p.src_line(), new ByteList(new byte[] {}));
                    } else if (((Node)yyVals[-1+yyTop].value) instanceof DStrNode) {
                        yyVal = new DSymbolNode(yyVals[yyTop - count + 1].start(), ((DStrNode)yyVals[-1+yyTop].value));
                    } else if (((Node)yyVals[-1+yyTop].value) instanceof StrNode) {
                        yyVal = p.asSymbol(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-1+yyTop].value));
                    } else {
                        yyVal = new DSymbolNode(yyVals[yyTop - count + 1].start());
                        ((DSymbolNode)yyVal).add(((Node)yyVals[-1+yyTop].value));
                    }
                    /*% %*/
                    /*% ripper: dyna_symbol!($2) %*/
  return yyVal;
};
states[678] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((NumericNode)yyVals[0+yyTop].value);  
  return yyVal;
};
states[679] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.negateNumeric(((NumericNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: unary!(ID2VAL(idUMinus), $2) %*/
  return yyVal;
};
states[680] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[681] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((FloatNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[682] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((RationalNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[683] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ComplexNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[687] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.declareIdentifier(((ByteList)yyVals[0+yyTop].value));
                    /*%  %*/
                    /*%
                    if (p.id_is_var(yyVals[yyTop - count + 1].id)) {
                        $$ = p.dispatch("on_var_ref", $1);
                    } else {
                        $$ = p.dispatch("on_vcall", $1);
                    }
                    %*/
  return yyVal;
};
states[688] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new InstVarNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)));
                    /*%  %*/
                    /*%
                    if (p.id_is_var(yyVals[yyTop - count + 1].id)) {
                        $$ = p.dispatch("on_var_ref", $1);
                    } else {
                        $$ = p.dispatch("on_vcall", $1);
                    }
                    %*/
  return yyVal;
};
states[689] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new GlobalVarNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)));
                    /*%  %*/
                    /*%
                    if (p.id_is_var(yyVals[yyTop - count + 1].id)) {
                        $$ = p.dispatch("on_var_ref", $1);
                    } else {
                        $$ = p.dispatch("on_vcall", $1);
                    }
                    %*/
  return yyVal;
};
states[690] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ConstNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)));
                    /*%  %*/
                    /*%
                    if (p.id_is_var(yyVals[yyTop - count + 1].id)) {
                        $$ = p.dispatch("on_var_ref", $1);
                    } else {
                        $$ = p.dispatch("on_vcall", $1);
                    }
                    %*/
  return yyVal;
};
states[691] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ClassVarNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)));
                    /*%  %*/
                    /*%
                    if (p.id_is_var(yyVals[yyTop - count + 1].id)) {
                        $$ = p.dispatch("on_var_ref", $1);
                    } else {
                        $$ = p.dispatch("on_vcall", $1);
                    }
                    %*/
  return yyVal;
};
states[692] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new NilNode(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[693] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new SelfNode(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[694] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new TrueNode(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[695] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new FalseNode(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[696] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new FileNode(yyVals[yyTop - count + 1].start(), new ByteList(p.getFile().getBytes(),
                    p.getRuntime().getEncodingService().getLocaleEncoding()));
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[697] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new FixnumNode(yyVals[yyTop - count + 1].start(), yyVals[yyTop - count + 1].start()+1);
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[698] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new EncodingNode(yyVals[yyTop - count + 1].start(), p.getEncoding());
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
                    /* mri:keyword_variable*/
  return yyVal;
};
states[699] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.assignableLabelOrIdentifier(((ByteList)yyVals[0+yyTop].value), null);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
                    
  return yyVal;
};
states[700] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new InstAsgnNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[701] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new GlobalAsgnNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[702] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (p.getLexContext().in_def) p.compile_error("dynamic constant assignment");

                    yyVal = new ConstDeclNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), null, NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[703] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ClassVarAsgnNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[704] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to nil");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[705] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't change the value of self");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[706] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to true");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[707] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to false");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[708] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to __FILE__");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[709] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to __LINE__");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[710] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to __ENCODING__");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
                    /* mri:keyword_variable*/
  return yyVal;
};
states[711] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[712] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[713] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   p.setState(EXPR_BEG);
                   p.setCommandStart(true);
  return yyVal;
};
states[714] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[715] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = null;
                    /*% %*/
                    /*% ripper: Qnil %*/
  return yyVal;
};
states[717] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = false;
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, null, null, 
                                    p.new_args_tail(p.src_line(), null, (ByteList) null, (ByteList) null));
  return yyVal;
};
states[718] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ArgsNode)yyVals[-1+yyTop].value);
                    /*% %*/
                    /*% ripper: paren!($2) %*/
                    p.setState(EXPR_BEG);
                    p.getLexContext().in_argdef = false;
                    p.setCommandStart(true);
  return yyVal;
};
states[719] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((ArgsNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[720] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.getLexContext().clone();
                    p.getLexContext().in_kwarg = true;
                    p.getLexContext().in_argdef = true;
                    p.setState(p.getState() | EXPR_LABEL);
  return yyVal;
};
states[721] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_kwarg = ((LexContext)yyVals[-2+yyTop].value).in_kwarg;
                    ctxt.in_argdef = false;
                    yyVal = ((ArgsNode)yyVals[-1+yyTop].value);
                    p.setState(EXPR_BEG);
                    p.setCommandStart(true);
  return yyVal;
};
states[722] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-3+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[723] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-1+yyTop].value), (ByteList) null, ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[724] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(p.src_line(), null, ((ByteList)yyVals[-1+yyTop].value), ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[725] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), null, (ByteList) null, ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[726] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.add_forwarding_args();
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), null, ((ByteList)yyVals[0+yyTop].value), FWD_BLOCK);
  return yyVal;
};
states[727] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[728] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(p.src_line(), null, (ByteList) null, (ByteList) null);
  return yyVal;
};
states[729] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-5+yyTop].value), ((ListNode)yyVals[-3+yyTop].value), ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[730] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-7+yyTop].value), ((ListNode)yyVals[-5+yyTop].value), ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[731] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[732] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-5+yyTop].value), ((ListNode)yyVals[-3+yyTop].value), null, ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[733] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-3+yyTop].value), null, ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[734] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-5+yyTop].value), null, ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[735] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-1+yyTop].value), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[736] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((ListNode)yyVals[-3+yyTop].value), ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[737] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((ListNode)yyVals[-5+yyTop].value), ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[738] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((ListNode)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[739] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((ListNode)yyVals[-3+yyTop].value), null, ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[740] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[741] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[742] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[743] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(p.src_line(), null, null, null, null, (ArgsTailHolder) null);
  return yyVal;
};
states[744] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = FWD_KWREST;
                    /*% %*/
                    /*% ripper: args_forward! %*/
  return yyVal;
};
states[745] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "formal argument cannot be a constant";
                    /*%%%*/
                    p.yyerror(message);
                    /*% %*/
                    /*% ripper[error]: param_error!(ERR_MESG(), $1) %*/
  return yyVal;
};
states[746] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "formal argument cannot be an instance variable";
                    /*%%%*/
                    p.yyerror(message);
                    /*% %*/
                    /*% ripper[error]: param_error!(ERR_MESG(), $1) %*/
  return yyVal;
};
states[747] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "formal argument cannot be a global variable";
                    /*%%%*/
                    p.yyerror(message);
                    /*% %*/
                    /*% ripper[error]: param_error!(ERR_MESG(), $1) %*/
  return yyVal;
};
states[748] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "formal argument cannot be a class variable";
                    /*%%%*/
                    p.yyerror(message);
                    /*% %*/
                    /*% ripper[error]: param_error!(ERR_MESG(), $1) %*/
  return yyVal;
};
states[749] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value); /* Not really reached*/
  return yyVal;
};
states[750] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.formal_argument(yyVals[yyTop - count + 1].id, ((ByteList)yyVals[0+yyTop].value));
                    p.ordinalMaxNumParam();
  return yyVal;
};
states[751] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.arg_var(yyVals[yyTop - count + 1].id);
                    /*%
                      p.arg_var(yyVals[yyTop - count + 1].id);
                      $$ = $1;
                      %*/

  return yyVal;
};
states[752] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ArgumentNode)yyVals[0+yyTop].value);
                    /*% %*/
                    /*% ripper: get_value($1) %*/
  return yyVal;
};
states[753] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((Node)yyVals[-1+yyTop].value);
                    /*% %*/
                    /*% ripper: mlhs_paren!($2) %*/
  return yyVal;
};
states[754] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ArrayNode(p.src_line(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper[brace]: rb_ary_new3(1, get_value($1)) %*/
  return yyVal;
};
states[755] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[0+yyTop].value));
                    yyVal = ((ListNode)yyVals[-2+yyTop].value);
                    /*% %*/
                    /*% ripper: rb_ary_push($1, get_value($3)) %*/
  return yyVal;
};
states[756] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.formal_argument(yyVals[yyTop - count + 1].id, ((ByteList)yyVals[0+yyTop].value));
                    p.arg_var(yyVals[yyTop - count + 1].id);
                    p.ordinalMaxNumParam();
                    p.getLexContext().in_argdef = false;
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[757] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = true;
                    /*%%%*/
                    yyVal = new KeywordArgNode(yyVals[yyTop - count + 2].start(), p.assignableKeyword(((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value)));
                    /*%
                      $$ = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, $1), $2);
                    %*/
  return yyVal;
};
states[758] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = true;
                    /*%%%*/
                    yyVal = new KeywordArgNode(p.src_line(), p.assignableKeyword(((ByteList)yyVals[0+yyTop].value), new RequiredKeywordArgumentValueNode()));
                    /*%
                      $$ = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, $1), p.nil());
                    %*/
  return yyVal;
};
states[759] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = true;
                    /*%%%*/
                    yyVal = new KeywordArgNode(yyVals[yyTop - count + 2].start(), p.assignableKeyword(((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value)));
                    /*%
                      $$ = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, $1), $2);
                    %*/
  return yyVal;
};
states[760] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = true;
                    /*%%%*/
                    yyVal = new KeywordArgNode(p.src_line(), p.assignableKeyword(((ByteList)yyVals[0+yyTop].value), new RequiredKeywordArgumentValueNode()));
                    /*%
                      $$ = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, $1), p.fals());
                    %*/
  return yyVal;
};
states[761] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ArrayNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_new3(1, get_value($1)) %*/
  return yyVal;
};
states[762] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_push($1, get_value($3)) %*/
  return yyVal;
};
states[763] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ArrayNode(yyVals[yyTop - count + 1].start(), ((KeywordArgNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_new3(1, get_value($1)) %*/
  return yyVal;
};
states[764] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((KeywordArgNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_push($1, get_value($3)) %*/
  return yyVal;
};
states[765] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[766] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[767] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*% ripper: nokw_param!(Qnil) %*/
  return yyVal;
};
states[768] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.shadowing_lvar(yyVals[yyTop - count + 2].id);
                    /*%%%*/
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
                    /*% %*/
                    /*% ripper: kwrest_param!($2) %*/
  return yyVal;
};
states[769] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = FWD_KWREST;
                    /*% %*/
                    /*% ripper: kwrest_param!(Qnil) %*/
  return yyVal;
};
states[770] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.getLexContext().in_argdef = true;
                    yyVal = new OptArgNode(yyVals[yyTop - count + 3].start(), p.assignableLabelOrIdentifier(((ArgumentNode)yyVals[-2+yyTop].value).getName().getBytes(), ((Node)yyVals[0+yyTop].value)));
                    /*%
                      $$ = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, $1), $3);
                    %*/
  return yyVal;
};
states[771] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = true;
                    /*%%%*/
                    yyVal = new OptArgNode(yyVals[yyTop - count + 3].start(), p.assignableLabelOrIdentifier(((ArgumentNode)yyVals[-2+yyTop].value).getName().getBytes(), ((Node)yyVals[0+yyTop].value)));
                    /*%
                      $$ = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, $1), $3);
                    %*/
  return yyVal;
};
states[772] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new BlockNode(yyVals[yyTop - count + 1].start()).add(((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_new3(1, get_value($1)) %*/
  return yyVal;
};
states[773] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.appendToBlock(((ListNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_push($1, get_value($3)) %*/
  return yyVal;
};
states[774] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new BlockNode(yyVals[yyTop - count + 1].start()).add(((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_new3(1, get_value($1)) %*/
  return yyVal;
};
states[775] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.appendToBlock(((ListNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_push($1, get_value($3)) %*/
  return yyVal;
};
states[776] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = STAR;
  return yyVal;
};
states[777] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[778] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (!p.is_local_id(yyVals[yyTop - count + 2].id)) {
                        p.yyerror("rest argument must be local variable");
                    }
                    yyVal = p.arg_var(p.shadowing_lvar(yyVals[yyTop - count + 2].id));
                    /*%%%*/
                    yyVal = new RestArgNode(((ArgumentNode)yyVal));
                    /*% %*/
                    /*% ripper: rest_param!($2) %*/
  return yyVal;
};
states[779] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new UnnamedRestArgNode(p.src_line(), p.symbolID(CommonByteLists.EMPTY), p.getCurrentScope().addVariableThisScope("*"));
                    /*% %*/
                    /*% ripper: rest_param!(Qnil) %*/
  return yyVal;
};
states[780] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = AMPERSAND;
  return yyVal;
};
states[781] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[782] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (!p.is_local_id(yyVals[yyTop - count + 2].id)) {
                        p.yyerror("block argument must be local variable");
                    }
                    yyVal = p.arg_var(p.shadowing_lvar(yyVals[yyTop - count + 2].id));
                    /*%%%*/
                    yyVal = new BlockArgNode(((ArgumentNode)yyVal));
                    /*% %*/
                    /*% ripper: blockarg!($2) %*/
  return yyVal;
};
states[783] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.arg_var(p.shadowing_lvar(ANON_BLOCK));
                    /*%%%*/
                    yyVal = new BlockArgNode(((ArgumentNode)yyVal));
                    /* Changed from MRI*/
                    /*% 
                        $$ = p.dispatch("on_blockarg", p.nil());
                    %*/
  return yyVal;
};
states[784] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((BlockArgNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[785] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[786] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[787] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_BEG);
  return yyVal;
};
states[788] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (((Node)yyVals[-1+yyTop].value) == null) {
                        p.yyerror("can't define single method for ().");
                    } else if (((Node)yyVals[-1+yyTop].value) instanceof LiteralNode) {
                        p.yyerror("can't define single method for literals.");
                    }
                    p.value_expr(((Node)yyVals[-1+yyTop].value));
                    yyVal = ((Node)yyVals[-1+yyTop].value);
                    /*% %*/
                    /*% ripper: paren!($3) %*/
  return yyVal;
};
states[789] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new HashNode(p.src_line());
                    /*% %*/
  return yyVal;
};
states[790] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: assoclist_from_args!($1) %*/
  return yyVal;
};
states[791] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new HashNode(p.src_line(), ((KeyValuePair)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper[brace]: rb_ary_new3(1, get_value($1)) %*/
  return yyVal;
};
states[792] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((HashNode)yyVals[-2+yyTop].value).add(((KeyValuePair)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_push($1, get_value($3)) %*/
  return yyVal;
};
states[793] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.createKeyValue(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: assoc_new!($1, $3) %*/
  return yyVal;
};
states[794] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    Node label = p.asSymbol(yyVals[yyTop - count + 2].start(), ((ByteList)yyVals[-1+yyTop].value));
                    yyVal = p.createKeyValue(label, ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: assoc_new!($1, $2) %*/
  return yyVal;
};
states[795] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    Node label = p.asSymbol(yyVals[yyTop - count + 1].start(), ((ByteList)yyVals[0+yyTop].value));
                    Node var = p.gettable(((ByteList)yyVals[0+yyTop].value));
                    if (var == null) var = new BeginNode(yyVals[yyTop - count + 1].start(), NilImplicitNode.NIL);
                    yyVal = p.createKeyValue(label, var);
                    /*% %*/
                    /*% ripper: assoc_new!($1, Qnil) %*/
  return yyVal;
};
states[796] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (((Node)yyVals[-2+yyTop].value) instanceof StrNode) {
                        DStrNode dnode = new DStrNode(yyVals[yyTop - count + 1].start(), p.getEncoding());
                        dnode.add(((Node)yyVals[-2+yyTop].value));
                        yyVal = p.createKeyValue(new DSymbolNode(yyVals[yyTop - count + 1].start(), dnode), ((Node)yyVals[0+yyTop].value));
                    } else if (((Node)yyVals[-2+yyTop].value) instanceof DStrNode) {
                        yyVal = p.createKeyValue(new DSymbolNode(yyVals[yyTop - count + 1].start(), ((DStrNode)yyVals[-2+yyTop].value)), ((Node)yyVals[0+yyTop].value));
                    } else {
                        p.compile_error("Uknown type for assoc in strings: " + ((Node)yyVals[-2+yyTop].value));
                    }
                    /*% %*/
                    /*% ripper: assoc_new!(dyna_symbol!($2), $4) %*/
  return yyVal;
};
states[797] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.createKeyValue(null, ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: assoc_splat!($2) %*/
  return yyVal;
};
states[798] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.forwarding_arg_check(FWD_KWREST, FWD_ALL, "keyword rest");
                    /*%%%*/
                    yyVal = p.createKeyValue(null, p.declareIdentifier(FWD_KWREST));
                    /*% %*/
                    /*% ripper: assoc_splat!(Qnil) %*/
  return yyVal;
};
states[799] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[800] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[801] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[802] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[803] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[804] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[805] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[806] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[807] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[808] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[809] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[810] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[812] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[817] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RPAREN;
  return yyVal;
};
states[818] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RBRACKET;
  return yyVal;
};
states[819] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RCURLY;
  return yyVal;
};
states[827] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = null;
  return yyVal;
};
states[828] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  yyVal = null;
  return yyVal;
};
}
					// line 4921 "parse.y"

}
					// line 15222 "-"
