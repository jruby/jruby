// created by jay 1.0.2 (c) 2002-2004 ats@cs.rit.edu
// skeleton Java 1.0 (c) 2002 ats@cs.rit.edu

					// line 2 "ripper_RubyParser.out"
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

package org.jruby.ext.ripper;

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
import org.jruby.ext.ripper.StrTerm;
import org.jruby.util.KeyValuePair; import org.jruby.RubyArray;
import org.jruby.util.ByteList;
import org.jruby.util.CommonByteLists;
import org.jruby.util.KeyValuePair;
import org.jruby.util.StringSupport;
import org.jruby.lexer.yacc.LexContext;
import org.jruby.lexer.yacc.LexContext.InRescue.*;
import org.jruby.ext.ripper.RubyLexer;
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
 
 public class RipperParser extends RipperParserBase {
    public RipperParser(ThreadContext context, IRubyObject ripper, LexerSource source) {
        super(context, ripper, source);
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
//yyLhs 823
    -1,   228,     0,    35,    36,    36,    36,    37,    37,    30,
    38,   231,   232,    41,   233,    41,    42,    43,    43,    43,
    44,   234,    44,    34,   216,   235,    45,    45,    45,    45,
    45,    45,    45,    45,    45,    45,    45,    45,    45,    45,
    45,    45,    45,    45,    85,    85,    85,    85,    85,    85,
    85,    85,    85,    85,    40,    40,    40,    83,    83,    83,
    46,    46,    46,    46,    46,   237,    46,   238,    46,    46,
    27,    28,   239,    29,    52,    52,   240,   242,    53,    50,
    50,    90,    90,   128,    57,    49,    49,    49,    49,    49,
    49,    49,    49,    49,    49,    49,    49,   133,   133,   139,
   139,   135,   135,   135,   135,   135,   135,   135,   135,   135,
   135,   136,   136,   134,   134,   138,   138,   137,   137,   137,
   137,   137,   137,   137,   137,   137,   137,   137,   137,   137,
   137,   137,   137,   137,   137,   137,   130,   130,   130,   130,
   130,   130,   130,   130,   130,   130,   130,   130,   130,   130,
   130,   130,   130,   130,   130,   167,   167,    26,    26,    26,
   169,   169,   169,   169,   169,   132,   132,   107,   244,   107,
   168,   168,   168,   168,   168,   168,   168,   168,   168,   168,
   168,   168,   168,   168,   168,   168,   168,   168,   168,   168,
   168,   168,   168,   168,   168,   168,   168,   168,   168,   168,
   180,   180,   180,   180,   180,   180,   180,   180,   180,   180,
   180,   180,   180,   180,   180,   180,   180,   180,   180,   180,
   180,   180,   180,   180,   180,   180,   180,   180,   180,   180,
   180,   180,   180,   180,   180,   180,   180,   180,   180,   180,
   180,    47,    47,    47,    47,    47,    47,    47,    47,    47,
    47,    47,    47,    47,    47,    47,    47,    47,    47,    47,
    47,    47,    47,    47,    47,    47,    47,    47,    47,    47,
    47,    47,    47,    47,    47,    47,    47,    47,    47,    47,
    47,    47,    47,    47,    47,    47,    39,    39,    39,   181,
   181,   181,   181,    56,    56,   193,   213,   219,    54,    78,
    78,    78,    78,    84,    84,    71,    71,    71,    72,    72,
    70,    70,    70,    70,    70,    69,    69,    69,    69,    69,
   246,    77,    80,    80,    79,    79,    67,    67,    67,    67,
    68,    68,    87,    87,    86,    86,    86,    48,    48,    48,
    48,    48,    48,    48,    48,    48,    48,    48,   247,    48,
   248,    48,    48,    48,    48,    48,    48,    48,    48,    48,
    48,    48,    48,    48,    48,    48,    48,    48,    48,    48,
    48,    48,   250,    48,   251,    48,    48,    48,   252,    48,
   254,    48,   255,    48,   256,    48,   257,    48,    48,    48,
    48,    48,    55,   203,   204,   212,    31,    32,   211,    33,
   214,   215,   209,   205,   206,   217,   218,   202,   201,   207,
   210,   210,   200,   243,   249,   249,   249,   241,   241,    58,
    58,    59,    59,   110,   110,   100,   100,   101,   101,   103,
   103,   103,   103,   103,   102,   102,   189,   189,   259,   258,
    75,    75,    75,    75,    76,    76,   191,   111,   111,   111,
   111,   111,   111,   111,   111,   111,   111,   111,   111,   111,
   111,   111,   112,   112,   113,   113,   120,   120,   119,   119,
   121,   121,   225,   226,   227,   260,   261,   122,   126,   126,
   123,   262,   123,   129,    89,    89,    89,    89,    51,    51,
    51,    51,    51,    51,    51,    51,    51,   127,   127,   263,
   124,   264,   125,    61,    61,    61,    61,    60,    62,    62,
   224,   223,   220,   265,   140,   141,   141,   142,   142,   142,
   143,   143,   143,   143,   143,   143,   144,   145,   145,   146,
   146,   221,   222,   147,   147,   147,   147,   147,   147,   147,
   147,   147,   147,   147,   147,   147,   266,   147,   147,   147,
   149,   149,   149,   149,   149,   149,   150,   150,   151,   151,
   148,   183,   183,   152,   152,   153,   160,   160,   160,   160,
   161,   161,   162,   162,   187,   187,   184,   184,   185,   186,
   186,   154,   154,   154,   154,   154,   154,   154,   154,   154,
   154,   155,   155,   155,   155,   155,   155,   155,   155,   155,
   155,   155,   155,   155,   155,   155,   155,   156,   157,   157,
   158,   159,   159,   159,    63,    63,    64,    64,    64,    65,
    65,    66,    66,    20,    20,     2,     3,     3,     3,     4,
     5,     6,   267,   267,    11,    16,    16,    19,    19,    12,
    13,    13,    14,    15,    17,    17,    18,    18,     7,     7,
     8,     8,     9,     9,    10,   268,    10,   269,   270,   271,
   272,    10,   273,   273,   109,   109,    25,    25,    23,   163,
   163,    24,    21,    21,    22,    22,    22,    22,   192,   192,
   192,    81,    81,    81,    81,    81,    81,    81,    81,    81,
    81,    81,    81,    82,    82,    82,    82,    82,    82,    82,
    82,    82,    82,    82,    82,   108,   108,   274,    88,    88,
    94,    94,    95,    93,   275,    93,    73,    73,    73,    73,
    73,    74,    74,    96,    96,    96,    96,    96,    96,    96,
    96,    96,    96,    96,    96,    96,    96,    96,   190,   174,
   174,   174,   174,   173,   173,   177,    98,    98,    97,    97,
   176,   116,   116,   118,   118,   117,   117,   115,   115,   196,
   196,   188,   175,   175,   114,    92,    91,    91,    99,    99,
   195,   195,   170,   170,   194,   194,   171,   171,   172,   172,
     1,   276,     1,   104,   104,   105,   105,   106,   106,   106,
   106,   106,   106,   164,   164,   164,   165,   165,   166,   166,
   166,   182,   182,   178,   178,   179,   179,   229,   229,   236,
   236,   197,   198,   208,   245,   245,   245,   253,   253,   230,
   230,   131,   199,
    }, yyLen = {
//yyLen 823
     2,     0,     2,     2,     1,     1,     3,     1,     2,     1,
     3,     0,     0,     8,     0,     5,     2,     1,     1,     3,
     1,     0,     3,     0,     2,     0,     4,     3,     3,     3,
     2,     3,     3,     3,     3,     4,     5,     1,     4,     4,
     7,     4,     1,     1,     4,     4,     7,     6,     6,     6,
     6,     4,     4,     4,     1,     4,     3,     1,     4,     1,
     1,     3,     3,     3,     2,     0,     7,     0,     7,     1,
     1,     2,     0,     5,     1,     1,     0,     0,     4,     1,
     1,     1,     4,     3,     1,     2,     3,     4,     5,     4,
     5,     6,     2,     2,     2,     2,     2,     1,     3,     1,
     3,     1,     2,     3,     5,     2,     4,     2,     4,     1,
     3,     1,     3,     2,     3,     1,     3,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     4,
     3,     3,     3,     3,     2,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     4,     3,
     3,     3,     3,     2,     1,     1,     1,     2,     1,     3,
     1,     1,     1,     1,     1,     1,     1,     1,     0,     4,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     4,     4,     7,     6,     6,     6,     6,     5,     4,
     3,     3,     2,     2,     2,     2,     3,     3,     3,     3,
     3,     3,     4,     2,     2,     3,     3,     3,     3,     1,
     3,     3,     3,     3,     3,     2,     2,     3,     3,     3,
     3,     4,     6,     4,     4,     1,     1,     4,     3,     1,
     1,     1,     1,     3,     3,     1,     1,     1,     1,     1,
     2,     4,     2,     1,     4,     3,     5,     3,     1,     1,
     1,     1,     2,     4,     2,     1,     2,     2,     4,     1,
     0,     2,     2,     1,     2,     1,     1,     1,     3,     3,
     2,     1,     1,     1,     3,     4,     2,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     0,     4,
     0,     4,     3,     3,     2,     3,     3,     1,     4,     3,
     1,     6,     4,     3,     2,     1,     2,     1,     6,     6,
     4,     4,     0,     6,     0,     5,     5,     6,     0,     6,
     0,     7,     0,     5,     0,     5,     0,     5,     1,     1,
     1,     1,     1,     1,     1,     1,     2,     2,     1,     2,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     2,     1,     1,     1,
     5,     1,     2,     1,     1,     1,     3,     1,     3,     1,
     3,     5,     1,     3,     2,     1,     1,     1,     0,     2,
     4,     2,     2,     1,     2,     0,     1,     6,     8,     4,
     6,     4,     2,     6,     2,     4,     6,     2,     4,     2,
     4,     1,     1,     1,     3,     4,     1,     4,     1,     3,
     1,     1,     0,     0,     0,     0,     0,     9,     4,     1,
     3,     0,     4,     3,     2,     4,     5,     5,     2,     4,
     4,     3,     3,     3,     2,     1,     4,     3,     3,     0,
     7,     0,     7,     1,     2,     3,     4,     5,     1,     1,
     0,     0,     0,     0,     9,     1,     1,     1,     3,     3,
     1,     2,     3,     1,     1,     1,     1,     3,     1,     3,
     1,     2,     2,     1,     1,     4,     4,     4,     3,     4,
     4,     4,     3,     3,     3,     2,     0,     6,     2,     4,
     1,     1,     2,     2,     4,     1,     2,     3,     1,     3,
     5,     2,     1,     1,     3,     1,     3,     1,     2,     1,
     1,     3,     2,     1,     1,     3,     2,     1,     2,     1,
     1,     1,     3,     3,     2,     2,     1,     1,     1,     2,
     2,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     2,     2,
     4,     2,     3,     1,     6,     1,     1,     1,     1,     2,
     1,     2,     1,     1,     1,     1,     1,     1,     2,     3,
     3,     3,     1,     2,     4,     0,     3,     1,     2,     4,
     0,     3,     4,     4,     0,     3,     0,     3,     0,     2,
     0,     2,     0,     2,     1,     0,     3,     0,     0,     0,
     0,     7,     1,     1,     1,     1,     1,     1,     2,     1,
     1,     3,     1,     2,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     0,     4,     0,
     1,     1,     3,     1,     0,     3,     4,     2,     2,     1,
     1,     2,     0,     6,     8,     4,     6,     4,     6,     2,
     4,     6,     2,     4,     2,     4,     1,     0,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     3,     1,     3,
     1,     2,     1,     2,     1,     1,     3,     1,     3,     1,
     1,     1,     2,     1,     3,     3,     1,     3,     1,     3,
     1,     1,     2,     1,     1,     1,     2,     1,     2,     0,
     1,     0,     4,     1,     2,     1,     3,     3,     2,     1,
     4,     2,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     0,     1,     0,
     1,     2,     2,     2,     0,     1,     1,     1,     1,     1,
     2,     0,     0,
    }, yyDefRed = {
//yyDefRed 1399
     1,     0,     0,    43,   400,   401,   402,     0,   393,   394,
   395,   398,    23,    23,    23,     0,     0,   390,   391,   412,
   413,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   821,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   674,   675,   676,   677,   626,   705,   706,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   475,     0,
   648,   650,   652,     0,     0,     0,     0,     0,     0,   338,
     0,   627,   339,   340,   341,   343,   342,   344,   337,   623,
   672,   666,   667,   624,     0,     0,    76,    76,     0,     2,
     0,     5,     0,     0,     0,     0,     0,    60,     0,     0,
     0,     0,   345,     0,    37,     0,    80,     0,   367,     0,
     4,     0,     0,    97,     0,   111,    84,     0,   348,     0,
     0,     0,     0,     0,     0,    23,     0,   210,   221,   211,
   234,   207,   227,   217,   216,   237,   238,   232,   215,   214,
   209,   235,   239,   240,   219,   208,   222,   226,   228,   220,
   213,   229,   236,   231,   230,   223,   233,   218,   206,   225,
   224,   205,   212,   203,   204,   200,   201,   202,   160,   162,
   161,   195,   196,   191,   173,   174,   175,   182,   179,   181,
   176,   177,   197,   198,   183,   184,   188,   192,   178,   180,
   170,   171,   172,   185,   186,   187,   189,   190,   193,   194,
   199,   166,     0,   167,   163,   165,   164,   396,   397,   399,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   648,
     0,     0,     0,     0,   315,     0,     0,     0,   327,    95,
   319,     0,     0,   785,     0,     0,    96,     0,   494,    92,
     0,     0,   810,     0,     0,    25,     0,     9,     0,     8,
   295,    24,     0,   388,   389,     0,   263,     0,     0,   357,
     0,     0,     0,     0,     0,    21,     0,     0,     0,    18,
     0,    17,     0,     0,   350,     0,     0,     0,   299,     0,
     0,     0,   783,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   392,     0,     0,     0,   472,   679,   678,   680,     0,
   668,   669,   670,     0,     0,     0,   632,     0,     0,     0,
     0,   275,    64,   276,   628,     0,   384,     0,     0,   711,
     0,   386,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   423,   424,   817,   818,     3,     0,   819,     0,
     0,     0,     0,   821,     0,     0,    67,     0,     0,     0,
     0,     0,   291,   292,     0,     0,     0,     0,     0,     0,
     0,     0,    65,     0,   289,   290,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   404,   484,   501,   403,   499,
   366,   501,   804,     0,     0,   803,     0,     0,   488,     0,
   364,   821,   806,   805,     0,   821,   821,   821,     0,     0,
     0,   113,    94,     0,    75,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   683,   682,     0,   685,   781,
     0,    71,   780,    70,     0,   374,     0,     0,   687,   686,
   688,   689,   691,   690,   692,     0,     0,     0,     0,     0,
     0,   346,   158,   382,     0,     0,    93,   168,   788,     0,
   330,   791,   322,     0,     0,     0,     0,     0,     0,     0,
     0,   316,   325,   821,     0,   317,   821,   821,     0,     0,
   311,     0,     0,   310,     0,   321,     0,   363,     0,    63,
    27,    29,    28,     0,   821,   296,     0,     0,     0,     0,
     0,   821,     0,     0,   352,    16,     0,     0,     0,     0,
   815,   300,   355,     0,   302,   356,   784,     0,   673,     0,
   115,     0,   713,     0,     0,     0,     0,   473,   654,   671,
   657,   655,   649,   629,   630,   651,   631,   653,   633,     0,
     0,     0,     0,   744,   741,   740,   739,   742,   750,   759,
   738,     0,   771,   760,   775,   774,   770,   736,     0,     0,
   748,     0,   768,     0,   757,     0,   719,   745,   743,   436,
     0,     0,   761,   437,     0,   720,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,    76,   820,     6,    31,    32,    33,    34,   297,     0,
    61,    62,   512,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   512,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   472,     0,
   472,     0,     0,     0,     0,   493,   796,     0,   491,     0,
     0,     0,     0,   795,     0,   492,     0,   797,     0,   499,
    86,     0,   793,   794,     0,     0,     0,     0,     0,     0,
     0,   114,     0,   821,   415,     0,     0,     0,   802,   801,
    72,     0,     0,     0,   380,   155,     0,   157,   707,   378,
     0,     0,     0,     0,     0,     0,   359,     0,     0,     0,
     0,   787,     0,     0,     0,     0,     0,     0,   329,   324,
     0,     0,   786,     0,     0,     0,   305,     0,   307,   362,
   811,    26,     0,     0,    10,     0,     0,     0,     0,     0,
    22,     0,    19,   351,     0,     0,     0,     0,     0,     0,
     0,     0,   474,   658,     0,   634,   637,     0,     0,   642,
   639,     0,     0,   643,     0,     0,   427,     0,     0,     0,
   425,   712,     0,   729,     0,   732,     0,   717,     0,   734,
   751,     0,     0,     0,   718,   776,   772,   578,   762,     0,
     0,     0,     0,     0,    54,   715,     0,     0,     0,   410,
   411,   370,   418,    77,   417,   371,     0,     0,     0,     0,
     0,     0,    35,   510,   510,     0,   483,   473,   497,   473,
   498,   821,   821,   499,   490,     0,     0,     0,     0,   821,
   821,   309,   489,     0,   308,     0,     0,     0,     0,    45,
   242,    59,     0,     0,     0,     0,    53,   249,     0,     0,
   326,     0,    44,   241,    39,    38,     0,   332,     0,   112,
     0,     0,   349,     0,     0,   416,     0,     0,   512,     0,
     0,   407,     0,     0,     0,     0,     0,     0,     0,     0,
   159,     0,     0,     0,   358,   169,   790,     0,   821,   821,
     0,   821,   821,   318,     0,     0,     0,   248,   301,   116,
     0,    23,   659,   665,   656,   664,   638,     0,     0,     0,
     0,     0,   434,     0,     0,   747,   721,   749,     0,     0,
     0,   769,     0,   758,   778,     0,     0,     0,   746,   764,
   439,   385,     0,   821,   821,   387,    78,     0,     0,   511,
   511,     0,   474,   474,     0,     0,     0,    90,   821,   812,
     0,     0,    88,    83,   821,   821,     0,     0,     0,   821,
   486,   487,     0,     0,   821,     0,   405,     0,   615,     0,
   409,   408,     0,   419,   421,     0,     0,   782,    73,   510,
   376,     0,   375,     0,   503,     0,     0,     0,     0,     0,
   496,   383,    36,     0,     0,     0,   821,     0,     0,     0,
   306,   361,     0,   660,   426,   428,     0,     0,     0,   725,
     0,   727,     0,   733,     0,   730,   716,   735,     0,     0,
     0,     0,   377,     0,     0,     0,    23,    23,    50,   246,
    49,   247,    91,     0,    47,   244,    48,   245,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,    11,
     0,     0,     0,   617,   618,   368,   422,     0,   511,   373,
   504,     0,     0,   369,     0,   708,   379,     0,     0,   479,
   476,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   600,   599,   601,   602,   604,   603,   605,   607,   613,   574,
     0,     0,     0,   511,     0,     0,     0,   648,     0,   592,
   593,   594,   595,   597,   596,   598,   591,   606,    68,     0,
   526,     0,   530,   523,   524,   533,     0,   534,   586,   587,
     0,   525,     0,   570,     0,   579,   580,   569,     0,     0,
    66,     0,     0,    46,   243,     0,    58,     0,     0,    40,
     0,   406,    15,   622,     0,     0,     0,   620,     0,     0,
     0,   505,     0,   381,     0,     0,     0,     0,   726,     0,
   723,   728,   731,   589,   590,   156,   611,     0,     0,     0,
     0,     0,   555,     0,   545,   548,   821,     0,   561,     0,
   608,     0,   609,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   576,     0,     0,   463,
   462,     0,    12,   621,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   619,
     0,     0,     0,     0,   506,   508,   509,   507,     0,     0,
   481,     0,   477,   663,   662,   661,     0,     0,   544,   543,
     0,     0,     0,   556,   546,   813,   575,     0,   527,   522,
     0,   529,   582,   583,   612,   542,   532,   538,   531,     0,
     0,     0,     0,     0,     0,   648,   571,   566,     0,   563,
   461,     0,   766,     0,     0,     0,   755,     0,     0,   443,
     0,     0,     0,   502,   500,     0,     0,     0,     0,     0,
     0,   420,   513,     0,     0,   478,     0,     0,     0,   724,
   549,   557,     0,     0,   610,     0,   536,   535,   537,   540,
   539,   541,     0,     0,     0,   457,     0,   454,   452,     0,
     0,   441,   464,     0,   459,     0,     0,     0,     0,     0,
   442,    13,     0,     0,     0,     0,     0,   614,     0,   518,
   519,   470,     0,   468,   471,     0,   480,     0,     0,     0,
   564,   560,   444,   767,     0,     0,     0,     0,   465,   756,
     0,     0,   354,     0,     0,     0,     0,     0,   467,   482,
     0,   547,     0,   458,     0,   455,     0,   449,     0,   451,
   440,   460,     0,     0,   515,   516,   514,   469,     0,     0,
     0,     0,   456,   450,     0,   447,   453,     0,   448,
    }, yyDgoto = {
//yyDgoto 277
     1,   450,    69,    70,    71,    72,    73,   319,   324,   325,
   552,    74,    75,   561,    76,    77,   559,   560,   562,   757,
    78,    79,    80,    81,    82,    83,   469,   451,   231,   232,
   258,    86,    87,    88,   207,    89,    90,    91,   259,   791,
   792,   682,   683,   278,   279,   280,    93,    94,    95,    96,
    97,    98,   436,   343,   235,   267,   100,   268,   972,   973,
   872,   985,  1227,   967,  1052,  1146,  1142,   660,   238,   500,
   501,   655,   832,   916,   773,  1352,  1315,   249,   286,   491,
   240,   102,   241,   852,   853,   104,   854,   858,   699,   105,
   106,  1271,  1272,   336,   337,   338,   578,   579,   580,   581,
   766,   767,   768,   769,   290,   502,   243,   202,   244,   904,
   361,  1274,  1198,  1199,   582,   583,   584,  1275,  1276,  1342,
  1228,  1343,   108,  1232,   649,   647,  1070,   420,   670,   406,
   245,   260,   203,   111,   112,   113,   114,   115,   541,   283,
   869,  1386,  1222,  1108,  1240,  1110,  1111,  1112,  1113,  1170,
  1171,  1172,  1268,  1173,  1115,  1116,  1117,  1118,  1119,  1120,
  1121,  1122,  1123,   320,   116,   737,   658,   472,   659,   205,
   585,   586,   777,   587,   588,   589,   590,   928,   702,   424,
   206,   404,   690,  1124,  1125,   592,  1127,  1128,   593,   594,
   595,  1318,   322,   618,   596,   597,   598,   507,   827,   492,
   269,   975,   873,   118,   119,   411,   407,   976,  1175,   120,
   801,   121,   122,   516,   123,   124,   125,   969,  1144,   619,
   813,  1191,  1192,  1023,   939,   547,   752,   901,     2,   366,
   456,  1140,  1285,  1050,   522,   513,   508,   636,   622,   867,
   344,   803,   936,   270,   707,   531,   250,   433,   528,   685,
   870,   692,   877,   686,   875,   703,   599,   602,   781,   782,
   315,  1155,  1297,   650,   648,  1338,  1303,   327,   754,   753,
   902,  1003,  1071,  1235,   876,   340,   687,
    }, yySindex = {
//yySindex 1399
     0,     0, 25897,     0,     0,     0,     0, 29369,     0,     0,
     0,     0,     0,     0,     0, 26389, 26389,     0,     0,     0,
     0,   161,     0,     0,     0,     0,   313, 29264,   211,   158,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  1265, 28240, 28240,
 28240, 28240,   153, 26023, 26143, 27007, 27253, 30036,     0, 29887,
     0,     0,     0,   447,   447,   447,   447, 28357, 28240,     0,
   137,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   495,   495,     0,     0, 32130,     0,
   176,     0,  1989,   382, 29686,     0,   256,     0,    38,   179,
   270,    82,     0,   283,     0,   136,     0,   368,     0,   676,
     0,   683, 32243,     0,   562,     0,     0, 26389,     0, 27373,
 29785, 25363, 27373, 32356, 32469,     0,   692,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   707,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   765,     0,     0,     0,     0,     0,
     0,     0,     0, 28240,   517, 26143, 28240, 28240, 28240,     0,
 28240,   495,   495, 25126,     0,   482,   196,   837,     0,     0,
     0,   533,   869,     0,   588,   892,     0, 26515,     0,     0,
 26389, 25478,     0, 28470,   438,     0,   943,     0, 25897,     0,
     0,     0,   663,     0,     0,   161,     0,   213,    82,     0,
   692,   696, 10321, 10321,   666,     0, 26023,   980,   176,     0,
  1989,     0,     0,   211,     0,   330,   935,   634,     0,   482,
   915,   634,     0,     0,     0,     0,     0,   211,     0,     0,
     0,     0,     0,     0,     0,     0,  1265,   743, 32582,   495,
   495,     0,   247,     0,  1041,     0,     0,     0,     0,   880,
     0,     0,     0,  1098,  1133,  1249,     0,  1055,  1055,  1055,
  1055,     0,     0,     0,     0,  4509,     0,  1044,     0,     0,
  4509,     0,  1048, 26143, 27373, 26143,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   814,   445,
     0,   870,     0,     0,     0,     0,     0, 25651,     0, 27373,
 27373, 27373, 27373,     0, 28470, 28470,     0, 28240, 28240, 28240,
 28240, 28240,     0,     0, 28240, 28240, 28240, 28240, 28240, 28240,
 28240, 28240,     0, 28240,     0,     0, 28240, 28240, 28240, 28240,
 28240, 28240, 28240, 28240, 28240,     0,     0,     0,     0,     0,
     0,     0,     0, 30391, 26389,     0, 30582, 28240,     0,   796,
     0,     0,     0,     0, 31872,     0,     0,     0, 26023, 30294,
  1119,     0,     0, 26143,     0,   382,   149,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
    46,     0,     0,     0,   176,     0,  1108,   149,     0,     0,
     0,     0,     0,     0,     0,     0,     0, 27373,    53,  1115,
   555,     0,     0,     0,  1058, 25248,     0,     0,     0,   666,
     0,     0,     0,   996,  1125,  1130, 28240, 30647, 26389, 30714,
 26635,     0,     0,     0, 27127,     0,     0,     0, 28240,  1157,
     0,   211,  1171,     0,   211,     0,   275,     0,  1152,     0,
     0,     0,     0, 29369,     0,     0, 28240,  1092, 28240, 30755,
 30714,     0,   158,   211,     0,     0, 25776,     0,  1185, 27007,
     0,     0,     0, 27253,     0,     0,     0,   943,     0,     0,
     0,  1184,     0, 30810, 26389, 30904, 32582,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  1263,
  -118,  1460,   319,     0,     0,     0,     0,     0,     0,     0,
     0,  2594,     0,     0,     0,     0,     0,     0,   211,  1191,
     0,  1197,     0,  1201,     0,  1204,     0,     0,     0,     0,
 28240,     0,     0,     0,  1211,     0,   955,   957,   -78, 26143,
 28587,   176, 26143, 28587,   345,    92,   345,     0, 31132, 26389,
 31226,     0,     0,     0,     0,     0,     0,     0,     0, 26269,
     0,     0,     0,   696,  4479,  4479,  4479,  4479, 11335, 10828,
  4479,  4479, 10321, 10321,  1434,  1434,     0, 24803,  1343,  1343,
   897,   105,   105,   696,   696,   696,  3225,   345,     0,  1139,
     0,   345,   917,     0,   -25,     0,     0,   161,     0,     0,
  1229,   211,   926,     0,   928,     0,   161,     0,  3225,     0,
     0, 28357,     0,     0,   161, 28357, 27499, 27499,   211, 32582,
  1238,     0,   345,     0,     0, 26143,  1017, 28470,     0,     0,
     0,  1006,  1021, 26143,     0,     0,     0,     0,     0,     0,
 31267, 26389, 31311, 26143, 26143,   211,     0, 29369, 28240, 28700,
 28700,     0,   954,   -14,   211,   962,   963,   482,     0,     0,
   869, 28240,     0, 28240, 28240, 26761,     0, 27127,     0,     0,
     0,     0, 28470, 25126,     0,   696,   973,   161,   161, 28240,
     0,     0,     0,     0,   634, 32582,     0,     0,   211,     0,
     0,  1184,     0,     0,  1857,     0,     0,   200,   447,     0,
     0,   200,   447,     0,  2594,  1490,     0,  1258,  1286,   211,
     0,     0,  4509,     0,  4509,     0,   422,     0,  4609,     0,
     0, 28240,  1267,    43,     0,     0,     0,     0,     0,   345,
   313,  1037,  1039, 25126,     0,     0,   345,  1037,  1039,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   211,     0,
     0, 26143,     0,     0,     0,  1280,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   796, 26635,   989,  1248,     0,
     0,     0,     0,   796,     0,  1219, 23963,  1049,   639,     0,
     0,     0,   368,  1295,    38,   256,     0,     0, 28240, 23963,
     0,  1313,     0,     0,     0,     0,     0,     0,  1064,     0,
  1184, 32582,     0,  1107,   693,     0,   275, 30204,     0,   345,
  1021,     0,   345, 27619,  1102,   176, 27373, 26143,     0,     0,
     0,   211,   345,  1264,     0,     0,     0,   313,     0,     0,
  1033,     0,     0,     0,  1340,   211,   275,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  1055,  1055,  1055,
  1055,   211,     0,  2594,  1890,     0,     0,     0,  1350,  1353,
  1354,     0,  1359,     0,     0,  1211,  1099,  1354,     0,     0,
     0,     0, 28587,     0,     0,     0,     0,     0,   345,     0,
     0, 28240,     0,     0, 28357, 28357,  1268,     0,     0,     0,
 28357, 28357,     0,     0,     0,     0, 31357, 26389, 31454,     0,
     0,     0,     0, 27745,     0,  1184,     0,  1102,     0, 27865,
     0,     0,   345,     0,     0, 26143, 27373,     0,     0,     0,
     0,   345,     0, 28240,     0,   114,   345, 26143,   176,   345,
     0,     0,     0, 28700, 28240, 28240,     0, 28240, 28240, 27127,
     0,     0,  3109,     0,     0,     0,  1372,  1380,  4509,     0,
  4609,     0,  4609,     0,  4609,     0,     0,     0,  1037,  1039,
 28240, 28240,     0, 31932, 31932, 25126,     0,     0,     0,     0,
     0,     0,     0, 28357,     0,     0,     0,     0, 28240, 26269,
   917,   -25,   211,   926,   928, 28357, 28240,     0, 26269,     0,
  1169,     0,  1083,     0,     0,     0,     0,   149,     0,     0,
     0, 27991, 26143,     0,   345,     0,     0, 28240,  4509,     0,
     0, 26143,  1890,  1890,  1354,  1393,  1354,  1354, 25126, 25126,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  5083,  5083,   709,     0, 12514,   211,  1134,     0,  1376,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,    64,
     0,  1317,     0,     0,     0,     0,   667,     0,     0,     0,
   122,     0,  1399,     0,  1400,     0,     0,     0, 32017,   524,
     0,  1327,  1327,     0,     0, 25126,     0,   989,     0,     0,
 26143,     0,     0,     0, 26143, 32695,   149,     0, 26143, 31932,
 28240,     0,   557,     0,   211,  -129,   323,  1380,     0,  4609,
     0,     0,     0,     0,     0,     0,     0, 32017,  1110,   211,
   211, 17824,     0,  1415,     0,     0,     0,  1337,     0,  1188,
     0, 27373,     0,  1162, 17824, 32017,  5083,  5083,   709,   211,
   211, 31932, 31932,  1447, 32017,  1110,     0,  1349, 26143,     0,
     0, 26143,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,  1165,   730,     0,     0,
 26143,   693,   149,   739,     0,     0,     0,     0,  1432,  1421,
     0, 26143,     0,     0,     0,     0,  1354,    67,     0,     0,
  1110,  1438,  1442,     0,     0,     0,     0,   211,     0,     0,
  1443,     0,     0,     0,     0,     0,     0,     0,     0,   211,
   211,   211,   211,   211,   211,     0,     0,     0,  1444,     0,
     0,  1446,     0,  1452,   211,  1453,     0,  1375,  1459,     0,
 32808,     0,  1211,     0,     0,  1169,     0, 31679, 26389, 31776,
  1107,     0,     0, 27373, 27373,     0,  2240, 26143,  1384,     0,
     0,     0, 32017,  1447,     0, 32017,     0,     0,     0,     0,
     0,     0,   448, 17824,  3725,     0,  3725,     0,     0,  1408,
   422,     0,     0,  4976,     0,     0,     0,  1224,   752, 32808,
     0,     0,     0,     0,   211,     0,     0,     0, 26143,     0,
     0,     0,   671,     0,     0,   345,     0,  1493,   211,  1493,
     0,     0,     0,     0,  1496,  1499,  1505,  1506,     0,     0,
  1211,  1496,     0, 31817,   752,     0,    73,  2240,     0,     0,
 32017,     0,  4976,     0,  4976,     0,  3725,     0,  4976,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  1496,  1496,
  1509,  1496,     0,     0,  4976,     0,     0,  1496,     0,
    }, yyRindex = {
//yyRindex 1399
     0,     0,   472,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0, 18411, 18501,     0,     0,     0,
     0, 14689, 19103, 19418, 19508, 19600, 28817,     0, 28127,     0,
     0, 19915, 20005, 20097, 15540,  5683, 20412, 20502, 15657, 20594,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   454,   454,  1463,  1420,   359,     0,  1501,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  8824,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,  1970,  1970,     0,     0,     0,     0,
   167,     0,   794, 12019, 22766,  9140, 22331,     0,  9235,     0,
 11677, 26881,     0,     0,     0, 22394,     0, 20909,     0,     0,
     0,     0,   776,     0,     0,     0,     0, 18605,     0,     0,
     0,  1287,     0,     0,     0,     0, 16024,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0, 22888,     0,     0,     0,     0,     0,     0,     0,
  6279,  6595,  6690,  6788,     0,  7104,  7199,  7297,  4348,  7613,
  7708,  4715,  7806,  3207,     0,   454,  3871, 21259,  1448,     0,
     0,  1970,  1970,  2231,     0, 21357,     0,  7007,     0,     0,
     0,     0,  7007,     0,  8631,     0,     0,   467,     0,     0,
     0,  1526,     0,     0,     0,     0, 28930,     0,   546,     0,
     0,     0,  9649,     0,     0,  8122,     0,     0,     0,     0,
  9333, 10156, 13829, 13956, 20999,     0,   454,     0,   713,     0,
  1401,     0,   933,  1526,     0,  1478,     0,  1478,     0,     0,
     0,  1449,     0,  2168,  2565,  2579,  2620,  1546,  3149,  3325,
  3363,  1259,  3707,  3900,  1800,  3907,     0,     0,     0,  1284,
  1284,     0,     0,  4321,   876,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  1525,   490,  1537,
   529,     0,     0,     0,     0,   829,     0,     0, 24615,     0,
   621,     0,     0,   202,     0,   202,   302,   788,  1136,  1366,
  1508,  1656,  1674,  1285,  1744,  1816,  1967,  1904,     0,     0,
  2302,     0,     0,     0,     0,     0,     0,   169,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  9744,  9840,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   206,     0,     0,     0,     0,  7516,
     0,     0,     0,     0,     0,     0,     0,     0,   454,   887,
  1053,     0,     0,   863,     0,  3935,     0,  3035,  3480,  3764,
  6353,  9991, 10498, 11005, 13712,     0,     0, 14577,     0,     0,
     0,     0,     0,     0,   243,     0,   629,     0,     0,     0,
     0,     0,     0,     0,     0, 23315, 23441,     0,     0, 24729,
     0,     0,     0,     0,     0,  1526,     0,     0,     0,  8726,
     0,     0,     0,     0,     0,     0,     0,     0,   206,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   844,   883,
     0,  1526,   190,     0,  1526,     0,  1526,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,  1526,     0,     0,  3362,   124,     0,  1494,
     0,     0,     0,   647,     0,     0,     0,     0,     0,  4502,
     0,  1087,     0,     0,   206,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  1526,   209,
     0,   209,     0,   210,     0,   209,     0,     0,     0,     0,
   174,   134,     0,     0,   210,     0,   558,   394,   430,   863,
     0,     0,   863,     0,     0,     0,     0,  2499,     0,   206,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0, 10251,  2838, 12965, 13209, 13309, 13405, 13707,
 13526, 13612, 14072, 14119, 12171, 12266,     0,  1530, 12662, 12748,
 12362, 11772, 11868, 10347, 10663, 10758, 12843,     0,     0,     0,
     0,     0, 16141,  5800, 17593,     0,     0, 26881,     0,  6167,
   418,  1504, 16508,     0, 16625,     0, 15056,     0, 13092,     0,
     0,     0,     0,     0, 18077,     0,     0,     0,  1526,     0,
  1127,     0,     0,     0,     0,   107, 24328,     0,     0,     0,
     0,  1318,     0,   389,     0,     0, 24214,     0,     0,     0,
     0,   206,     0,   863,   546,  1526,     0,     0,     0,     0,
     0,     0,  4832, 15173,  1504,  5199,  5316, 21404,     0,     0,
  7007,     0,     0,     0,     0,  1002,     0,   226,     0,     0,
     0,     0,     0, 14205,     0, 10854,  8217,     0,  8315,     0,
     0,  1722,     0,     0,  1478,     0,  1823,  2026,  1504,  2373,
  2413,  1195,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  1270,     0,  1031,  1052,  1526,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
 28817, 11170, 22973, 14255,     0,     0,     0, 11265, 23041,     0,
     0,     0,     0,     0,     0,     0,  3180,   276,  1504,  3409,
  4050,   202,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  8025,   552, 18921,     0,     0,
     0,     0,     0,  8534,     0,     0,  4184, 23083,     0,     0,
     0,     0, 21091,     0,  2108, 22431,     0,     0,  1559,  9501,
     0,     0,     0,     0,     0,     0, 22851,     0, 23161,     0,
  1236,     0,     0,   657,   399,     0,  1526,     0,     0,     0,
     0,     0,     0,     0,   399,     0,     0,   863, 23775, 23889,
     0,  1504,     0,     0,     0,     0,     0, 28817,     0,     0,
  4004,     0,     0,     0,   190,  1526,  1526,     0,     0,     0,
  1481,     0,     0,     0,     0,     0,     0,  1545,   544,  1558,
   636,  1526,     0,     0,     0,     0,     0,     0,   209,   209,
   209,     0,   209,     0,     0,   210,   430,   209,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  1166,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   206,     0,     0,
     0,     0,  1694,     0,     0,  1368,     0,   289,     0,    68,
     0,     0,     0,     0,     0,   202,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   863,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   723,
     0,     0,   -87,     0,     0,     0,  1069,  1071,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0, 11361, 23181,
     0,     0,     0,     0,     0, 14389,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
 16992, 17960,  1504, 17109, 17476,     0,  1559,  3660,     0,     0,
   399,   101,   178,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   785,     0,     0,     0,     0,     0,   985,     0,
     0,    84,     0,     0,   209,   209,   209,   209, 14525, 22901,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,  1504,  1173, 21890,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0, 22523,
     0, 21851,     0,     0,     0,     0, 21443,     0,     0,     0,
 21552,     0,  9043,     0,  9551,     0,     0,     0, 21928,  9962,
     0, 24938, 25052,     0,     0, 14572,     0, 19013, 11525,     0,
   128,     0,     0,     0,   202,     0,     0,     0,   107,     0,
     0,     0,   399,     0,   688,     0,     0,  1146,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0, 21975,  1504,
  1504, 10469,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0, 22608,     0, 21591, 21732,     0, 29081,
 29451,     0,     0, 10976,     0, 22038,     0,   373,   863,     0,
     0,   546,     0,     0,   318,   441,  1163,  1228,  1971,  2068,
  2218,  1646,  2709,  2777,  1757,  2945,     0,     0,  3007,     0,
   863,   399,     0,   238,     0,     0,     0,     0,     0,   535,
     0,   546,     0,     0,     0,     0,   209,  1526,     0,     0,
 22085, 11483, 22124,     0,     0,     0,     0,  1526,     0,     0,
 22672,     0,     0,     0,     0,     0,     0,     0,     0,  1526,
  1526,  1526,  1504,  1504,  1504,     0,     0,     0, 22218,     0,
     0,   651,     0,   651,   373,   777,     0,     0,   651,     0,
   764,  1507,   777,     0,     0,   399,  3093,     0,   206,     0,
   657,     0,     0,     0,     0,     0,     0,   863,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   942,     0,     0,     0,
     0,     0,     0,     0,     0,  1878,  2495,     0,   825,     0,
     0,     0,  2037,   756,  1504,  2704,  2739,     0,   832,     0,
     0,     0,   545,     0,     0,     0,     0, 22281,  1477, 22724,
     0,     0,     0,     0,   651,   651,   651,   651,     0,     0,
   777,   651,     0,     0,   998,  1281,   399,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  2875,   836,     0,     0,     0,     0,   651,   651,
   651,   651,     0,     0,     0,     0,     0,   651,     0,
    }, yyGindex = {
//yyGindex 277
     0,     0,  1167,     0,  1535,  1605,  2549,   -58,     0,     0,
  -212,  3229, 22802,     0, 23240, 23252,     0,     0,     0,  1050,
 23638,     0,    59,     0,     0,    16,  1485,   750,  2213,  2337,
     0,     0,     0,     0,     4,  1367,     0,  1260,  1116,  -531,
  -534,  -577,   -42,     0,  1118,    -1,   -62,  3499,   -50,    -7,
   -35,     0,  -117,   -61,  1759,    13,     0,   746,   425,  -838,
  -715,     0,     0,   361,     0,     0,   369,    48,  -429,    89,
  -370,    69,   988,  -295,  -497,   470,  3550,    30,     0,  -226,
  -448,  1548,  2614,   -15,   469,  -555,  -558,     0,     0,     0,
     0,   350,  -942,   129,   113,   824,  -326,    78,  -697,   901,
  -798,  -766,   761,   912,     0,    40,  -436,     0,  1998,     0,
     0,     0,   547,     0,  -594,     0,   905,     0,   363,     0,
 -1033,   328, 24685,     0,  -506,  1288,     0,   -95,   126,   853,
  2635,    -2,    12,  1621,     0,    -4,   -91,   -16,  -455,  -123,
   358,     0,     0,  -825,  1900,     0,     0,   541,  -945,   -22,
     0,  -820,  -786,  -300,     0,  -240,   548,     0,     0,     0,
 -1015,     0,   543,     0,   652,  -348,     0,  -435,    20,   -49,
  -724,     1,  -570,  -438, -1089,  -745,  1457,  -305,   -86,     0,
     0,  1634,     0,  -962,     0,   952,   561,     0,     0,  1362,
  -213,     0,  -655,    77,     0,     0,  -565,   354,   266,     0,
  1149,   774,     0,     0,     0,     0,     0,     0,   402,     0,
  -141,     0,     0,  1241,     0,     0,     0,     0,     0,   685,
  -508,     0,     0,    50,  -668,  -201,   263,   281,     0,     2,
    24,     0,     0,     0,     0,     0,   164,     0,     0,     0,
     0,     0,     0,  1531,     0,  -198,     0,     0,     0,  -437,
     0,     0,     0,   -84,     0,     0,     0,     0,   476,     0,
     0,     0,     0,     0,     0,     0,     0,    21,     0,     0,
     0,     0,     0,     0,     0,     0,     0,
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
    "stmt : mlhs '=' lex_ctxt command_call",
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
    "command_asgn : primary_value tCOLON2 tCONSTANT tOP_ASGN lex_ctxt command_rhs",
    "command_asgn : primary_value tCOLON2 tIDENTIFIER tOP_ASGN lex_ctxt command_rhs",
    "command_asgn : defn_head f_opt_paren_args '=' endless_command",
    "command_asgn : defs_head f_opt_paren_args '=' endless_command",
    "command_asgn : backref tOP_ASGN lex_ctxt command_rhs",
    "endless_command : command",
    "endless_command : endless_command modifier_rescue after_rescue arg",
    "endless_command : keyword_not opt_nl endless_command",
    "command_rhs : command_call",
    "command_rhs : command_call modifier_rescue after_rescue stmt",
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
    "arg : arg '?' arg opt_nl ':' arg",
    "arg : defn_head f_opt_paren_args '=' endless_arg",
    "arg : defs_head f_opt_paren_args '=' endless_arg",
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
    "p_alt : p_alt '|' p_expr_basic",
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
    "$$30 :",
    "p_expr_basic : tLBRACE p_pktbl lex_ctxt $$30 p_kwargs rbrace",
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
    "$$31 :",
    "string_content : tSTRING_DVAR $$31 string_dvar",
    "$$32 :",
    "$$33 :",
    "$$34 :",
    "$$35 :",
    "string_content : tSTRING_DBEG $$32 $$33 $$34 $$35 compstmt string_dend",
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
    "$$36 :",
    "superclass : '<' $$36 expr_value term",
    "superclass :",
    "f_opt_paren_args : f_paren_args",
    "f_opt_paren_args : none",
    "f_paren_args : '(' f_args rparen",
    "f_arglist : f_paren_args",
    "$$37 :",
    "f_arglist : $$37 f_args term",
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
    "$$38 :",
    "singleton : '(' $$38 expr rparen",
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
            start = yystates[yytop - count + 1].start;
            end = yystates[yytop].end;
            yyVal = parserState.execute(this, yyVal, yystates, yytop, count, yytoken);
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

static ParserState<RipperParser>[] states = new ParserState[823];
static {
states[1] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  p.setState(EXPR_BEG);
                  p.initTopLocalVariables();
  return yyVal;
};
states[2] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_program", v1);
                    yyVal = v2;}
  return yyVal;
};
states[3] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  yyVal = p.void_stmts(((IRubyObject)yyVals[-1+yyTop].value));
  return yyVal;
};
states[4] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = p.dispatch("on_stmts_new");
                    v2 = p.dispatch("on_void_stmt");
                    v3 = v1;
                    v4 = v2;
                    v5 = p.dispatch("on_stmts_add", v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[5] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_stmts_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_stmts_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[6] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_stmts_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[7] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  p.clear_block_exit(true);
                  yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[8] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[9] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  yyVal = p.init_block_exit();
  return yyVal;
};
states[10] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  p.restore_block_exit(((NodeExits)yyVals[-2+yyTop].value));
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_BEGIN", v1);
                    yyVal = v2;}
  return yyVal;
};
states[11] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  return yyVal;
};
states[12] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  p.next_rescue_context(((LexContext)yyVals[-4+yyTop].value), LexContext.InRescue.AFTER_ENSURE);
  return yyVal;
};
states[13] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = p.escape(((IRubyObject)yyVals[-7+yyTop].value));
                    v2 = p.escape(((IRubyObject)yyVals[-5+yyTop].value));
                    v3 = p.escape(((IRubyObject)yyVals[-2+yyTop].value));
                    v4 = p.escape(((IRubyObject)yyVals[0+yyTop].value));
                    v5 = p.dispatch("on_bodystmt", v1, v2, v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[14] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  p.next_rescue_context(((LexContext)yyVals[-1+yyTop].value), LexContext.InRescue.AFTER_ENSURE);
  return yyVal;
};
states[15] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = p.escape(((IRubyObject)yyVals[-4+yyTop].value));
                    v2 = p.escape(((IRubyObject)yyVals[-2+yyTop].value));
                    v3 = p.nil();
                    v4 = p.escape(((IRubyObject)yyVals[0+yyTop].value));
                    v5 = p.dispatch("on_bodystmt", v1, v2, v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[16] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  yyVal = p.void_stmts(((IRubyObject)yyVals[-1+yyTop].value));
  return yyVal;
};
states[17] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = p.dispatch("on_stmts_new");
                    v2 = p.dispatch("on_void_stmt");
                    v3 = v1;
                    v4 = v2;
                    v5 = p.dispatch("on_stmts_add", v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[18] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_stmts_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_stmts_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[19] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_stmts_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[20] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[21] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   p.yyerror("BEGIN is permitted only at toplevel");
  return yyVal;
};
states[22] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[23] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = p.allow_block_exit();
  return yyVal;
};
states[24] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((LexContext)yyVals[0+yyTop].value);
                   p.getLexContext().in_rescue = LexContext.InRescue.BEFORE_RESCUE;
  return yyVal;
};
states[25] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_FNAME|EXPR_FITEM);
  return yyVal;
};
states[26] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_alias", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[27] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_var_alias", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[28] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_var_alias", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[29] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "can't make alias for the number variables";
{IRubyObject v1, v2, v3;
                    v1 = p.intern(message);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_alias_error", v1, v2);
                    yyVal = v3;
                    p.error();}
  return yyVal;
};
states[30] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((RubyArray)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_undef", v1);
                    yyVal = v2;}
  return yyVal;
};
states[31] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = p.dispatch("on_if_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[32] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = p.dispatch("on_unless_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[33] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = p.dispatch("on_while_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[34] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = p.dispatch("on_until_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[35] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_rescue = ((LexContext)yyVals[-1+yyTop].value).in_rescue;
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_rescue_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[36] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   if (p.getLexContext().in_def) {
                       p.warn("END in method; use at_exit");
                    }
                    p.restore_block_exit(((NodeExits)yyVals[-3+yyTop].value));
                    p.setLexContext(((LexContext)yyVals[-4+yyTop].value));
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_END", v1);
                    yyVal = v2;}
  return yyVal;
};
states[38] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_massign", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[39] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_assign", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[40] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_rescue_mod", v1, v2);
                    v4 = ((IRubyObject)yyVals[-6+yyTop].value);
                    v5 = v3;
                    v6 = p.dispatch("on_massign", v4, v5);
                    yyVal = v6;}
  return yyVal;
};
states[41] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_massign", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[42] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  return yyVal;
};
states[43] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.NEW_ERROR(yyVals[yyTop - count + 1]);
  return yyVal;
};
states[44] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_assign", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[45] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_opassign", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[46] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7;
                    v1 = ((IRubyObject)yyVals[-6+yyTop].value);
                    v2 = p.escape(((IRubyObject)yyVals[-4+yyTop].value));
                    v3 = p.dispatch("on_aref_field", v1, v2);
                    v4 = v3;
                    v5 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v6 = ((IRubyObject)yyVals[0+yyTop].value);
                    v7 = p.dispatch("on_opassign", v4, v5, v6);
                    yyVal = v7;}
  return yyVal;
};
states[47] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8;
                    v1 = ((IRubyObject)yyVals[-5+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v3 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v4 = p.dispatch("on_field", v1, v2, v3);
                    v5 = v4;
                    v6 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v7 = ((IRubyObject)yyVals[0+yyTop].value);
                    v8 = p.dispatch("on_opassign", v5, v6, v7);
                    yyVal = v8;}
  return yyVal;
};
states[48] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8;
                    v1 = ((IRubyObject)yyVals[-5+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v3 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v4 = p.dispatch("on_field", v1, v2, v3);
                    v5 = v4;
                    v6 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v7 = ((IRubyObject)yyVals[0+yyTop].value);
                    v8 = p.dispatch("on_opassign", v5, v6, v7);
                    yyVal = v8;}
  return yyVal;
};
states[49] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7;
                    v1 = ((IRubyObject)yyVals[-5+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v3 = p.dispatch("on_const_path_field", v1, v2);
                    v4 = v3;
                    v5 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v6 = ((IRubyObject)yyVals[0+yyTop].value);
                    v7 = p.dispatch("on_opassign", v4, v5, v6);
                    yyVal = v7;}
  return yyVal;
};
states[50] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8;
                    v1 = ((IRubyObject)yyVals[-5+yyTop].value);
                    v2 = p.intern("::");
                    v3 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v4 = p.dispatch("on_field", v1, v2, v3);
                    v5 = v4;
                    v6 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v7 = ((IRubyObject)yyVals[0+yyTop].value);
                    v8 = p.dispatch("on_opassign", v5, v6, v7);
                    yyVal = v8;}
  return yyVal;
};
states[51] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((DefHolder)yyVals[-3+yyTop].value), yyVals[yyTop - count + 1]);
                    p.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8, v9;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.nil();
                    v3 = p.nil();
                    v4 = p.nil();
                    v5 = p.dispatch("on_bodystmt", v1, v2, v3, v4);
                    v6 = p.get_value(((DefHolder)yyVals[-3+yyTop].value));
                    v7 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v8 = v5;
                    v9 = p.dispatch("on_def", v6, v7, v8);
                    yyVal = v9;}
                    p.popCurrentScope();
  return yyVal;
};
states[52] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((DefHolder)yyVals[-3+yyTop].value), yyVals[yyTop - count + 1]);
                    p.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.nil();
                    v3 = p.nil();
                    v4 = p.nil();
                    v5 = p.dispatch("on_bodystmt", v1, v2, v3, v4);
                    v6 = ((RubyArray) ((DefHolder)yyVals[-3+yyTop].value).value).eltOk(0);
                    v7 = ((RubyArray) ((DefHolder)yyVals[-3+yyTop].value).value).eltOk(1);
                    v8 = ((RubyArray) ((DefHolder)yyVals[-3+yyTop].value).value).eltOk(2);
                    v9 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v10 = v5;
                    v11 = p.dispatch("on_defs", v6, v7, v8, v9, v10);
                    yyVal = v11;}
                    p.popCurrentScope();
  return yyVal;
};
states[53] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = p.var_field(((IRubyObject)yyVals[-3+yyTop].value));
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_assign", v1, v2);
                    yyVal = p.backref_error(((IRubyObject)yyVals[-3+yyTop].value), v3);
                    p.error();}
  return yyVal;
};
states[55] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_rescue = ((LexContext)yyVals[-1+yyTop].value).in_rescue;
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_rescue_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[56] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((IRubyObject)yyVals[0+yyTop].value)), NOT);
{IRubyObject v1, v2, v3;
                    v1 = p.intern("!");
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_unary", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[57] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((IRubyObject)yyVals[0+yyTop].value));
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[58] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_rescue_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[61] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.logop(((IRubyObject)yyVals[-2+yyTop].value), AND_KEYWORD, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[62] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.logop(((IRubyObject)yyVals[-2+yyTop].value), OR_KEYWORD, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[63] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((IRubyObject)yyVals[0+yyTop].value)), NOT);
  return yyVal;
};
states[64] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((IRubyObject)yyVals[0+yyTop].value)), BANG);
  return yyVal;
};
states[65] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((IRubyObject)yyVals[-1+yyTop].value));
  return yyVal;
};
states[66] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pvtbl(((Set)yyVals[-2+yyTop].value));
                    p.pop_pktbl(((Set)yyVals[-1+yyTop].value));
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_kwarg = ((LexContext)yyVals[-3+yyTop].value).in_kwarg;
{IRubyObject v1, v2, v3, v4, v5, v6, v7;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.nil();
                    v3 = p.nil();
                    v4 = p.dispatch("on_in", v1, v2, v3);
                    v5 = ((IRubyObject)yyVals[-6+yyTop].value);
                    v6 = v4;
                    v7 = p.dispatch("on_case", v5, v6);
                    yyVal = v7;}
  return yyVal;
};
states[67] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((IRubyObject)yyVals[-1+yyTop].value));
  return yyVal;
};
states[68] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pvtbl(((Set)yyVals[-2+yyTop].value));
                    p.pop_pktbl(((Set)yyVals[-1+yyTop].value));
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_kwarg = ((LexContext)yyVals[-3+yyTop].value).in_kwarg;
{IRubyObject v1, v2, v3, v4, v5, v6, v7;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.nil();
                    v3 = p.nil();
                    v4 = p.dispatch("on_in", v1, v2, v3);
                    v5 = ((IRubyObject)yyVals[-6+yyTop].value);
                    v6 = v4;
                    v7 = p.dispatch("on_case", v5, v6);
                    yyVal = v7;}
  return yyVal;
};
states[70] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pushLocalScope();
                    ByteList currentArg = p.getCurrentArg();
                    p.setCurrentArg(null);
                    LexContext ctxt = p.getLexContext();
                    RubySymbol name = p.get_id(((IRubyObject)yyVals[0+yyTop].value));
                        p.numparam_name(yyVals[yyTop - count + 1].id);
                        yyVal = new DefHolder(p.get_id(yyVals[yyTop - count + 1].id), currentArg, p.get_value(((IRubyObject)yyVals[0+yyTop].value)), (LexContext) ctxt.clone());

                    ctxt.in_def = true;
                    ctxt.in_rescue = LexContext.InRescue.BEFORE_RESCUE;
                    p.setCurrentArg(null);
  return yyVal;
};
states[71] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    ((DefHolder)yyVals[0+yyTop].value).line = yyVals[yyTop - count + 1].start();
                    yyVal = ((DefHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[72] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_FNAME); 
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_argdef = true;
  return yyVal;
};
states[73] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_ENDFN|EXPR_LABEL);
                    ((DefHolder)yyVals[0+yyTop].value).line = yyVals[yyTop - count + 1].start();
                    yyVal = ((DefHolder)yyVals[0+yyTop].value);
                       ((DefHolder)yyVal).value = p.new_array(((IRubyObject)yyVals[-3+yyTop].value), ((IRubyObject)yyVals[-2+yyTop].value), ((DefHolder)yyVal).value);

  return yyVal;
};
states[74] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[75] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.NEW_ERROR(yyVals[yyTop - count + 1]);
  return yyVal;
};
states[76] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getConditionState().push1();
  return yyVal;
};
states[77] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getConditionState().pop();
  return yyVal;
};
states[78] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-2+yyTop].value);
{                    yyVal = p.get_value(((IRubyObject)yyVals[-2+yyTop].value));}
  return yyVal;
};
states[82] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v4 = p.dispatch("on_call", v1, v2, v3);
                    v5 = v4;
                    v6 = ((IRubyObject)yyVals[0+yyTop].value);
                    v7 = p.dispatch("on_method_add_arg", v5, v6);
                    yyVal = v7;}
  return yyVal;
};
states[83] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[84] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);}
  return yyVal;
};
states[85] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_command", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[86] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_command", v1, v2);
                    v4 = v3;
                    v5 = ((IRubyObject)yyVals[0+yyTop].value);
                    v6 = p.dispatch("on_method_add_block", v4, v5);
                    yyVal = v6;}
  return yyVal;
};
states[87] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v4 = ((IRubyObject)yyVals[0+yyTop].value);
                    v5 = p.dispatch("on_command_call", v1, v2, v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[88] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v3 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v4 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v5 = p.dispatch("on_command_call", v1, v2, v3, v4);
                    v6 = v5;
                    v7 = ((IRubyObject)yyVals[0+yyTop].value);
                    v8 = p.dispatch("on_method_add_block", v6, v7);
                    yyVal = v8;}
  return yyVal;
};
states[89] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = p.intern("::");
                    v3 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v4 = ((IRubyObject)yyVals[0+yyTop].value);
                    v5 = p.dispatch("on_command_call", v1, v2, v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[90] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = p.intern("::");
                    v3 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v4 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v5 = p.dispatch("on_command_call", v1, v2, v3, v4);
                    v6 = v5;
                    v7 = ((IRubyObject)yyVals[0+yyTop].value);
                    v8 = p.dispatch("on_method_add_block", v6, v7);
                    yyVal = v8;}
  return yyVal;
};
states[91] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8;
                    v1 = ((IRubyObject)yyVals[-5+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v3 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v4 = p.nil();
                    v5 = p.dispatch("on_command_call", v1, v2, v3, v4);
                    v6 = v5;
                    v7 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v8 = p.dispatch("on_method_add_block", v6, v7);
                    yyVal = v8;}
  return yyVal;
};
states[92] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_super", v1);
                    yyVal = v2;}
  return yyVal;
};
states[93] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_yield", v1);
                    yyVal = v2;}
  return yyVal;
};
states[94] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_return", v1);
                    yyVal = v2;}
  return yyVal;
};
states[95] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_break", v1);
                    yyVal = v2;}
  return yyVal;
};
states[96] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_next", v1);
                    yyVal = v2;}
  return yyVal;
};
states[98] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_mlhs_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[99] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[100] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_mlhs_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[101] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);}
  return yyVal;
};
states[102] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_mlhs_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[103] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_mlhs_add_star", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[104] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = p.dispatch("on_mlhs_add_star", v1, v2);
                    v4 = v3;
                    v5 = ((IRubyObject)yyVals[0+yyTop].value);
                    v6 = p.dispatch("on_mlhs_add_post", v4, v5);
                    yyVal = v6;}
  return yyVal;
};
states[105] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.nil();
                    v3 = p.dispatch("on_mlhs_add_star", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[106] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = p.nil();
                    v3 = p.dispatch("on_mlhs_add_star", v1, v2);
                    v4 = v3;
                    v5 = ((IRubyObject)yyVals[0+yyTop].value);
                    v6 = p.dispatch("on_mlhs_add_post", v4, v5);
                    yyVal = v6;}
  return yyVal;
};
states[107] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_mlhs_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_mlhs_add_star", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[108] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7;
                    v1 = p.dispatch("on_mlhs_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v4 = p.dispatch("on_mlhs_add_star", v2, v3);
                    v5 = v4;
                    v6 = ((IRubyObject)yyVals[0+yyTop].value);
                    v7 = p.dispatch("on_mlhs_add_post", v5, v6);
                    yyVal = v7;}
  return yyVal;
};
states[109] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_mlhs_new");
                    v2 = v1;
                    v3 = p.nil();
                    v4 = p.dispatch("on_mlhs_add_star", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[110] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7;
                    v1 = p.dispatch("on_mlhs_new");
                    v2 = v1;
                    v3 = p.nil();
                    v4 = p.dispatch("on_mlhs_add_star", v2, v3);
                    v5 = v4;
                    v6 = ((IRubyObject)yyVals[0+yyTop].value);
                    v7 = p.dispatch("on_mlhs_add_post", v5, v6);
                    yyVal = v7;}
  return yyVal;
};
states[112] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_mlhs_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[113] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_mlhs_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v4 = p.dispatch("on_mlhs_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[114] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_mlhs_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[115] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_mlhs_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_mlhs_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[116] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_mlhs_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[117] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[118] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[119] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[120] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[121] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[122] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[123] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[124] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[125] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[126] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[127] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[128] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[129] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v3 = p.dispatch("on_aref_field", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[130] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (((IRubyObject)yyVals[-1+yyTop].value) == AMPERSAND_DOT) {
                        p.compile_error("&. inside multiple assignment destination");
                    }
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_field", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[131] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_const_path_field", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[132] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (((IRubyObject)yyVals[-1+yyTop].value) == AMPERSAND_DOT) {
                        p.compile_error("&. inside multiple assignment destination");
                    }
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_field", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[133] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_const_path_field", v1, v2);
                    yyVal = p.const_decl(v3);}
  return yyVal;
};
states[134] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_top_const_field", v1);
                    yyVal = p.const_decl(v2);}
  return yyVal;
};
states[135] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.backref_error(((IRubyObject)yyVals[0+yyTop].value), p.var_field(((IRubyObject)yyVals[0+yyTop].value)));
                    p.error();}
  return yyVal;
};
states[136] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

                    
  return yyVal;
};
states[137] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[138] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[139] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[140] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[141] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[142] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[143] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[144] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[145] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[146] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[147] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[148] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v3 = p.dispatch("on_aref_field", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[149] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_field", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[150] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.intern("::");
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_field", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[151] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_field", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[152] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_const_path_field", v1, v2);
                    yyVal = p.const_decl(v3);}
  return yyVal;
};
states[153] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_top_const_field", v1);
                    yyVal = p.const_decl(v2);}
  return yyVal;
};
states[154] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.backref_error(((IRubyObject)yyVals[0+yyTop].value), p.var_field(((IRubyObject)yyVals[0+yyTop].value)));
                    p.error();}
  return yyVal;
};
states[155] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "class/module name must be CONSTANT";
{IRubyObject v1, v2, v3;
                    v1 = p.intern(message);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_class_name_error", v1, v2);
                    yyVal = v3;
                    p.error();}
  return yyVal;
};
states[156] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[157] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_top_const_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[158] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_const_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[159] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_const_path_ref", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[160] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[161] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[162] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[163] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   p.setState(EXPR_ENDFN);
                   yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[164] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[165] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_symbol_literal", v1);
                    yyVal = v2;}

  return yyVal;
};
states[166] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[167] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[168] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_FNAME|EXPR_FITEM);
  return yyVal;
};
states[169] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-3+yyTop].value).push(p.getContext(), p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[170] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[171] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[172] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[173] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[174] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[175] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[176] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[177] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[178] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[179] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[180] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[181] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[182] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[183] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[184] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[185] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[186] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[187] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[188] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[189] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[190] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[191] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[192] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[193] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[194] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[195] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[196] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[197] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[198] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[199] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[200] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[201] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[202] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[203] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[204] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[205] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[206] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[207] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[208] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[209] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[210] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[211] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[212] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[213] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[214] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[215] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[216] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[217] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[218] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[219] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[220] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = yyVals[0+yyTop].value;
  return yyVal;
};
states[221] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[222] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[223] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[224] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[225] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[226] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[227] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[228] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[229] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[230] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[231] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[232] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[233] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[234] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[235] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[236] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[237] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[238] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[239] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[240] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[241] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_assign", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[242] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_opassign", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[243] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7;
                    v1 = ((IRubyObject)yyVals[-6+yyTop].value);
                    v2 = p.escape(((IRubyObject)yyVals[-4+yyTop].value));
                    v3 = p.dispatch("on_aref_field", v1, v2);
                    v4 = v3;
                    v5 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v6 = ((IRubyObject)yyVals[0+yyTop].value);
                    v7 = p.dispatch("on_opassign", v4, v5, v6);
                    yyVal = v7;}
  return yyVal;
};
states[244] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8;
                    v1 = ((IRubyObject)yyVals[-5+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v3 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v4 = p.dispatch("on_field", v1, v2, v3);
                    v5 = v4;
                    v6 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v7 = ((IRubyObject)yyVals[0+yyTop].value);
                    v8 = p.dispatch("on_opassign", v5, v6, v7);
                    yyVal = v8;}
  return yyVal;
};
states[245] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8;
                    v1 = ((IRubyObject)yyVals[-5+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v3 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v4 = p.dispatch("on_field", v1, v2, v3);
                    v5 = v4;
                    v6 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v7 = ((IRubyObject)yyVals[0+yyTop].value);
                    v8 = p.dispatch("on_opassign", v5, v6, v7);
                    yyVal = v8;}
  return yyVal;
};
states[246] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8;
                    v1 = ((IRubyObject)yyVals[-5+yyTop].value);
                    v2 = p.intern("::");
                    v3 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v4 = p.dispatch("on_field", v1, v2, v3);
                    v5 = v4;
                    v6 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v7 = ((IRubyObject)yyVals[0+yyTop].value);
                    v8 = p.dispatch("on_opassign", v5, v6, v7);
                    yyVal = v8;}
  return yyVal;
};
states[247] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7;
                    v1 = ((IRubyObject)yyVals[-5+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v3 = p.dispatch("on_const_path_field", v1, v2);
                    v4 = v3;
                    v5 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v6 = ((IRubyObject)yyVals[0+yyTop].value);
                    v7 = p.dispatch("on_opassign", v4, v5, v6);
                    yyVal = v7;}
  return yyVal;
};
states[248] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = p.dispatch("on_top_const_field", v1);
                    v3 = v2;
                    v4 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v5 = ((IRubyObject)yyVals[0+yyTop].value);
                    v6 = p.dispatch("on_opassign", v3, v4, v5);
                    yyVal = v6;}
  return yyVal;
};
states[249] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.var_field(((IRubyObject)yyVals[-3+yyTop].value));
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_opassign", v1, v2, v3);
                    yyVal = p.backref_error(((IRubyObject)yyVals[-3+yyTop].value), v4);
                    p.error();}
  return yyVal;
};
states[250] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot2", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[251] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot3", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[252] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.nil();
                    v3 = p.dispatch("on_dot2", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[253] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.nil();
                    v3 = p.dispatch("on_dot3", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[254] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = p.nil();
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot2", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[255] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = p.nil();
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot3", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[256] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), PLUS, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[257] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), MINUS, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[258] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), STAR, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[259] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), SLASH, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[260] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), PERCENT, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[261] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), STAR_STAR, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[262] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), STAR_STAR, ((IRubyObject)yyVals[0+yyTop].value), p.src_line()), MINUS_AT);
  return yyVal;
};
states[263] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(((IRubyObject)yyVals[0+yyTop].value), PLUS_AT);
  return yyVal;
};
states[264] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(((IRubyObject)yyVals[0+yyTop].value), MINUS_AT);
  return yyVal;
};
states[265] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), OR, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[266] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), CARET, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[267] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), AMPERSAND, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[268] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), LT_EQ_RT, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[269] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[270] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), EQ_EQ, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[271] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), EQ_EQ_EQ, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[272] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), BANG_EQ, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[273] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.match_op(((IRubyObject)yyVals[-2+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[274] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), BANG_TILDE, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[275] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((IRubyObject)yyVals[0+yyTop].value)), BANG);
  return yyVal;
};
states[276] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(((IRubyObject)yyVals[0+yyTop].value), TILDE);
  return yyVal;
};
states[277] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), LT_LT, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[278] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), GT_GT, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[279] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.logop(((IRubyObject)yyVals[-2+yyTop].value), AMPERSAND_AMPERSAND, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[280] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.logop(((IRubyObject)yyVals[-2+yyTop].value), OR_OR, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[281] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_defined = false;                    
                    yyVal = p.new_defined(yyVals[yyTop - count + 1].start(), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[282] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-5+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_ifop", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[283] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((DefHolder)yyVals[-3+yyTop].value), yyVals[yyTop - count + 1]);
                    p.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8, v9;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.nil();
                    v3 = p.nil();
                    v4 = p.nil();
                    v5 = p.dispatch("on_bodystmt", v1, v2, v3, v4);
                    v6 = p.get_value(((DefHolder)yyVals[-3+yyTop].value));
                    v7 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v8 = v5;
                    v9 = p.dispatch("on_def", v6, v7, v8);
                    yyVal = v9;}
                    p.popCurrentScope();
  return yyVal;
};
states[284] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((DefHolder)yyVals[-3+yyTop].value), yyVals[yyTop - count + 1]);
                    p.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.nil();
                    v3 = p.nil();
                    v4 = p.nil();
                    v5 = p.dispatch("on_bodystmt", v1, v2, v3, v4);
                    v6 = ((RubyArray) ((DefHolder)yyVals[-3+yyTop].value).value).eltOk(0);
                    v7 = ((RubyArray) ((DefHolder)yyVals[-3+yyTop].value).value).eltOk(1);
                    v8 = ((RubyArray) ((DefHolder)yyVals[-3+yyTop].value).value).eltOk(2);
                    v9 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v10 = v5;
                    v11 = p.dispatch("on_defs", v6, v7, v8, v9, v10);
                    yyVal = v11;}
                    p.popCurrentScope();
  return yyVal;
};
states[285] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[287] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_rescue = ((LexContext)yyVals[-1+yyTop].value).in_rescue;
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_rescue_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[288] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((IRubyObject)yyVals[0+yyTop].value)), NOT);
{IRubyObject v1, v2, v3;
                    v1 = p.intern("!");
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_unary", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[289] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = GT;
  return yyVal;
};
states[290] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = LT;
  return yyVal;
};
states[291] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = GT_EQ;
  return yyVal;
};
states[292] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = LT_EQ;
  return yyVal;
};
states[293] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[294] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.warning(p.src_line(), "comparison '" + ((ByteList)yyVals[-1+yyTop].value) + "' after comparison");
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[295] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = (LexContext) p.getLexContext().clone();
  return yyVal;
};
states[296] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_defined = true;
                    yyVal = ((LexContext)yyVals[0+yyTop].value);
  return yyVal;
};
states[297] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_rescue = LexContext.InRescue.AFTER_RESCUE;
                    yyVal = ((LexContext)yyVals[0+yyTop].value);
  return yyVal;
};
states[298] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((IRubyObject)yyVals[0+yyTop].value));
                    yyVal = p.makeNullNil(((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[300] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[301] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((RubyArray)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_bare_assoc_hash", v1);
                    v3 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v4 = v2;
                    v5 = p.dispatch("on_args_add", v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[302] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6;
                    v1 = p.dispatch("on_args_new");
                    v2 = ((RubyArray)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_bare_assoc_hash", v2);
                    v4 = v1;
                    v5 = v3;
                    v6 = p.dispatch("on_args_add", v4, v5);
                    yyVal = v6;}
  return yyVal;
};
states[303] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((IRubyObject)yyVals[0+yyTop].value));
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[304] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_rescue = ((LexContext)yyVals[-1+yyTop].value).in_rescue;
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_rescue_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[305] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v2 = p.dispatch("on_arg_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[306] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (!p.check_forwarding_args()) {
                        yyVal = null;
                    } else {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_args_add", v1, v2);
                    v4 = v3;
                    v5 = p.dispatch("on_arg_paren", v4);
                    yyVal = v5;}
                    }
  return yyVal;
};
states[307] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (!p.check_forwarding_args()) {
                        yyVal = null;
                    } else {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_arg_paren", v1);
                    yyVal = v2;}
                    }
  return yyVal;
};
states[312] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[313] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((RubyArray)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_bare_assoc_hash", v1);
                    v3 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v4 = v2;
                    v5 = p.dispatch("on_args_add", v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[314] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6;
                    v1 = p.dispatch("on_args_new");
                    v2 = ((RubyArray)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_bare_assoc_hash", v2);
                    v4 = v1;
                    v5 = v3;
                    v6 = p.dispatch("on_args_add", v4, v5);
                    yyVal = v6;}
  return yyVal;
};
states[315] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_args_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[316] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_args_add_block", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[317] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8, v9;
                    v1 = p.dispatch("on_args_new");
                    v2 = ((RubyArray)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_bare_assoc_hash", v2);
                    v4 = v1;
                    v5 = v3;
                    v6 = p.dispatch("on_args_add", v4, v5);
                    v7 = v6;
                    v8 = ((IRubyObject)yyVals[0+yyTop].value);
                    v9 = p.dispatch("on_args_add_block", v7, v8);
                    yyVal = v9;}
  return yyVal;
};
states[318] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8;
                    v1 = ((RubyArray)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_bare_assoc_hash", v1);
                    v3 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v4 = v2;
                    v5 = p.dispatch("on_args_add", v3, v4);
                    v6 = v5;
                    v7 = ((IRubyObject)yyVals[0+yyTop].value);
                    v8 = p.dispatch("on_args_add_block", v6, v7);
                    yyVal = v8;}
  return yyVal;
};
states[319] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_args_add_block", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[320] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[321] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    StackState cmdarg = p.getCmdArgumentState();
                    boolean lookahead = false;
                    switch (yychar) {
                    case tLBRACE_ARG:
                       lookahead = true;
                    }
                      
                    if (lookahead) cmdarg.pop();
                    cmdarg.pop();
                    if (lookahead) cmdarg.push0();
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[322] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);}
  return yyVal;
};
states[323] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.nil();

  return yyVal;
};
states[324] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[325] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.fals();

  return yyVal;
};
states[326] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_args_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[327] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_args_add_star", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[328] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_args_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[329] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_args_add_star", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[330] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
{                    yyVal = p.get_value(((IRubyObject)yyVals[0+yyTop].value));}
  return yyVal;
};
states[331] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.nil();}
  return yyVal;
};
states[332] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[333] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[334] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.dispatch("on_mrhs_new_from_args", v1);
                    v3 = v2;
                    v4 = ((IRubyObject)yyVals[0+yyTop].value);
                    v5 = p.dispatch("on_mrhs_add", v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[335] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = p.dispatch("on_mrhs_new_from_args", v1);
                    v3 = v2;
                    v4 = ((IRubyObject)yyVals[0+yyTop].value);
                    v5 = p.dispatch("on_mrhs_add_star", v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[336] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_mrhs_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_mrhs_add_star", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[341] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[342] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[343] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[344] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[347] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_fcall", v1);
                    v3 = p.dispatch("on_args_new");
                    v4 = v2;
                    v5 = v3;
                    v6 = p.dispatch("on_method_add_arg", v4, v5);
                    yyVal = v6;}
  return yyVal;
};
states[348] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getCmdArgumentState().push0();
  return yyVal;
};
states[349] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getCmdArgumentState().pop();
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_begin", v1);
                    yyVal = v2;}
  return yyVal;
};
states[350] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_ENDARG); 
  return yyVal;
};
states[351] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.dispatch("on_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[352] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[353] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_const_path_ref", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[354] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_top_const_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[355] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v2 = p.dispatch("on_array", v1);
                    yyVal = v2;}
  return yyVal;
};
states[356] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v2 = p.dispatch("on_hash", v1);
                    yyVal = v2;}
  return yyVal;
};
states[357] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_return0");
                    yyVal = v1;}
  return yyVal;
};
states[358] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_paren", v1);
                    v3 = v2;
                    v4 = p.dispatch("on_yield", v3);
                    yyVal = v4;}
  return yyVal;
};
states[359] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = p.dispatch("on_paren", v2);
                    v4 = v3;
                    v5 = p.dispatch("on_yield", v4);
                    yyVal = v5;}
  return yyVal;
};
states[360] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_yield0");
                    yyVal = v1;}
  return yyVal;
};
states[361] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_defined = false;
                    yyVal = p.new_defined(yyVals[yyTop - count + 1].start(), ((IRubyObject)yyVals[-1+yyTop].value));
  return yyVal;
};
states[362] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((IRubyObject)yyVals[-1+yyTop].value)), NOT);
  return yyVal;
};
states[363] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(p.nil()), NOT);
  return yyVal;
};
states[364] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8, v9;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_fcall", v1);
                    v3 = p.dispatch("on_args_new");
                    v4 = v2;
                    v5 = v3;
                    v6 = p.dispatch("on_method_add_arg", v4, v5);
                    v7 = v6;
                    v8 = ((IRubyObject)yyVals[0+yyTop].value);
                    v9 = p.dispatch("on_method_add_block", v7, v8);
                    yyVal = v9;}
  return yyVal;
};
states[366] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_method_add_block", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[367] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[368] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v4 = p.dispatch("on_if", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[369] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v4 = p.dispatch("on_unless", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[370] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_while", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[371] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_until", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[372] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.case_labels;
                    p.case_labels = p.getRuntime().getNil();
  return yyVal;
};
states[373] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_case", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[374] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.case_labels;
                    p.case_labels = null;
  return yyVal;
};
states[375] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = p.nil();
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_case", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[376] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_case", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[377] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v4 = p.dispatch("on_for", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[378] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.begin_definition("class");
  return yyVal;
};
states[379] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v3 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v4 = p.dispatch("on_class", v1, v2, v3);
                    yyVal = v4;}
                    LexContext ctxt = p.getLexContext();
                    p.popCurrentScope();
                    ctxt.in_class = ((LexContext)yyVals[-5+yyTop].value).in_class;
                    ctxt.shareable_constant_value = ((LexContext)yyVals[-5+yyTop].value).shareable_constant_value;
  return yyVal;
};
states[380] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.begin_definition(null);
  return yyVal;
};
states[381] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_sclass", v1, v2);
                    yyVal = v3;}
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_def = ((LexContext)yyVals[-6+yyTop].value).in_def;
                    ctxt.in_class = ((LexContext)yyVals[-6+yyTop].value).in_class;
                    ctxt.shareable_constant_value = ((LexContext)yyVals[-6+yyTop].value).shareable_constant_value;
                    p.popCurrentScope();
  return yyVal;
};
states[382] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.begin_definition("module");
  return yyVal;
};
states[383] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_module", v1, v2);
                    yyVal = v3;}
                    p.popCurrentScope();
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_class = ((LexContext)yyVals[-4+yyTop].value).in_class;
                    ctxt.shareable_constant_value = ((LexContext)yyVals[-4+yyTop].value).shareable_constant_value;
  return yyVal;
};
states[384] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.push_end_expect_token_locations(yyVals[yyTop - count + 1].start());
  return yyVal;
};
states[385] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.restore_defun(((DefHolder)yyVals[-4+yyTop].value));
{IRubyObject v1, v2, v3, v4;
                    v1 = p.get_value(((DefHolder)yyVals[-4+yyTop].value));
                    v2 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v3 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v4 = p.dispatch("on_def", v1, v2, v3);
                    yyVal = v4;}
                    p.popCurrentScope();
  return yyVal;
};
states[386] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.push_end_expect_token_locations(yyVals[yyTop - count + 1].start());
  return yyVal;
};
states[387] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.restore_defun(((DefHolder)yyVals[-4+yyTop].value));
{IRubyObject v1, v2, v3, v4, v5, v6;
                    v1 = ((RubyArray) ((DefHolder)yyVals[-4+yyTop].value).value).eltOk(0);
                    v2 = ((RubyArray) ((DefHolder)yyVals[-4+yyTop].value).value).eltOk(1);
                    v3 = ((RubyArray) ((DefHolder)yyVals[-4+yyTop].value).value).eltOk(2);
                    v4 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v5 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v6 = p.dispatch("on_defs", v1, v2, v3, v4, v5);
                    yyVal = v6;}
                    p.popCurrentScope();
  return yyVal;
};
states[388] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = p.dispatch("on_break", v2);
                    yyVal = v3;}
  return yyVal;
};
states[389] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = p.dispatch("on_next", v2);
                    yyVal = v3;}
  return yyVal;
};
states[390] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_redo");
                    yyVal = v1;}
  return yyVal;
};
states[391] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_retry");
                    yyVal = v1;}
  return yyVal;
};
states[392] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((IRubyObject)yyVals[0+yyTop].value));
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value) == null ? p.nil() : ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[393] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.token_info_push("begin", yyVals[yyTop - count + 1]);
                    p.push_end_expect_token_locations(yyVals[yyTop - count + 1].start());
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[394] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
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
states[395] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[396] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((NodeExits)yyVals[0+yyTop].value);
  return yyVal;
};
states[397] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[398] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[399] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((NodeExits)yyVals[0+yyTop].value);
  return yyVal;
};
states[400] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = (LexContext) p.getLexContext().clone();
                    p.getLexContext().in_rescue = LexContext.InRescue.BEFORE_RESCUE;
                    p.push_end_expect_token_locations(yyVals[yyTop - count + 1].start());
  return yyVal;
};
states[401] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = (LexContext) p.getLexContext().clone();  
                    p.getLexContext().in_rescue = LexContext.InRescue.BEFORE_RESCUE;
  return yyVal;
};
states[402] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
                    p.getLexContext().in_argdef = true;
  return yyVal;
};
states[403] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[404] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[405] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.getLexContext().clone();
                    p.getLexContext().in_rescue = LexContext.InRescue.AFTER_RESCUE;
  return yyVal;
};
states[406] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.getLexContext().clone();
  return yyVal;
};
states[407] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[408] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[409] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[410] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[411] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.compile_error("syntax error, unexpected end-of-input");
  return yyVal;
};
states[412] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = p.getLexContext();
                    if (ctxt.in_class && !ctxt.in_def && !p.getCurrentScope().isBlockScope()) {
                        p.compile_error("Invalid return in class/module body");
                    }
  return yyVal;
};
states[413] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = p.getLexContext();
                    if (ctxt.in_defined && !ctxt.in_def && !p.isEval()) {
                        p.compile_error("Invalid yield");
                    }
  return yyVal;
};
states[420] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.escape(((IRubyObject)yyVals[0+yyTop].value));
                    v4 = p.dispatch("on_elsif", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[422] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_else", v1);
                    yyVal = v2;}
  return yyVal;
};
states[424] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  return yyVal;
};
states[425] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, ((IRubyObject)yyVals[0+yyTop].value));

  return yyVal;
};
states[426] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_mlhs_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[427] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_mlhs_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_mlhs_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[428] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_mlhs_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[429] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);}
  return yyVal;
};
states[430] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_mlhs_add_star", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[431] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = p.dispatch("on_mlhs_add_star", v1, v2);
                    v4 = v3;
                    v5 = ((IRubyObject)yyVals[0+yyTop].value);
                    v6 = p.dispatch("on_mlhs_add_post", v4, v5);
                    yyVal = v6;}
  return yyVal;
};
states[432] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_mlhs_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_mlhs_add_star", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[433] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7;
                    v1 = p.dispatch("on_mlhs_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v4 = p.dispatch("on_mlhs_add_star", v2, v3);
                    v5 = v4;
                    v6 = ((IRubyObject)yyVals[0+yyTop].value);
                    v7 = p.dispatch("on_mlhs_add_post", v5, v6);
                    yyVal = v7;}
  return yyVal;
};
states[434] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    ByteList id = yyVals[yyTop - count + 2].id;
                      yyVal = p.assignable(id, ((IRubyObject)yyVals[0+yyTop].value));

  return yyVal;
};
states[435] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.nil();}
  return yyVal;
};
states[437] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.symbolID(LexingCommon.NIL);

  return yyVal;
};
states[438] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = false;
  return yyVal;
};
states[440] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-3+yyTop].value), ((IRubyObject)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[441] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-1+yyTop].value), (ByteList) null, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[442] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(p.src_line(), null, ((IRubyObject)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[443] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), null, (ByteList) null, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[444] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[445] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(p.src_line(), null, (ByteList) null, (ByteList) null);
  return yyVal;
};
states[446] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_excessed_comma");
                    yyVal = v1;}
  return yyVal;
};
states[447] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-5+yyTop].value), ((RubyArray)yyVals[-3+yyTop].value), ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[448] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-7+yyTop].value), ((RubyArray)yyVals[-5+yyTop].value), ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[449] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[450] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-5+yyTop].value), ((RubyArray)yyVals[-3+yyTop].value), null, ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[451] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-3+yyTop].value), null, ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[452] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-1+yyTop].value), null, ((IRubyObject)yyVals[0+yyTop].value), null, (ArgsTailHolder) null);
  return yyVal;
};
states[453] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-5+yyTop].value), null, ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[454] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-1+yyTop].value), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[455] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-3+yyTop].value), ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[456] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-5+yyTop].value), ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[457] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[458] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-3+yyTop].value), null, ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[459] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[460] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[461] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[462] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = null;

  return yyVal;
};
states[463] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setCommandStart(true);
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[464] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setCurrentArg(null);
                    p.ordinalMaxNumParam();
                    p.getLexContext().in_argdef = false;
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11;
                    v1 = p.nil();
                    v2 = p.nil();
                    v3 = p.nil();
                    v4 = p.nil();
                    v5 = p.nil();
                    v6 = p.nil();
                    v7 = p.nil();
                    v8 = p.dispatch("on_params", v1, v2, v3, v4, v5, v6, v7);
                    v9 = v8;
                    v10 = p.escape(((RubyArray)yyVals[-1+yyTop].value));
                    v11 = p.dispatch("on_block_var", v9, v10);
                    yyVal = v11;}
  return yyVal;
};
states[465] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setCurrentArg(null);
                    p.ordinalMaxNumParam();
                    p.getLexContext().in_argdef = false;
{IRubyObject v1, v2, v3;
                    v1 = p.escape(((IRubyObject)yyVals[-2+yyTop].value));
                    v2 = p.escape(((RubyArray)yyVals[-1+yyTop].value));
                    v3 = p.dispatch("on_block_var", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[466] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[467] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-1+yyTop].value);}
  return yyVal;
};
states[468] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[469] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.getContext(), p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[470] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.new_bv(yyVals[yyTop - count + 1].id);
{                    yyVal = p.get_value(((IRubyObject)yyVals[0+yyTop].value));}
  return yyVal;
};
states[471] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[472] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.resetMaxNumParam();
  return yyVal;
};
states[473] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.numparam_push();
  return yyVal;
};
states[474] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.it_id();
                    p.set_it_id(null);
  return yyVal;
};
states[475] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pushBlockScope();
                    yyVal = p.getLeftParenBegin();
                    p.setLeftParenBegin(p.getParenNest());
  return yyVal;
};
states[476] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getCmdArgumentState().push0();
  return yyVal;
};
states[477] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    IRubyObject it_id = p.it_id();
                    int max_numparam = p.restoreMaxNumParam(((Integer)yyVals[-6+yyTop].value));
                    p.set_it_id(((IRubyObject)yyVals[-4+yyTop].value));
                    p.getCmdArgumentState().pop();
                    /* Changed from MRI args_with_numbered put into parser codepath and not used by ripper (since it is just a passthrough method and types do not match).*/
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = p.dispatch("on_lambda", v1, v2);
                    yyVal = v3;}
                    p.setLeftParenBegin(((Integer)yyVals[-7+yyTop].value));
                    p.numparam_pop(((Node)yyVals[-5+yyTop].value));
                    p.popCurrentScope();
  return yyVal;
};
states[478] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = false;
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.dispatch("on_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[479] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = false;
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[480] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.token_info_pop("}", yyVals[yyTop - count + 1]);
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[481] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.push_end_expect_token_locations(yyVals[yyTop - count + 1].start());
  return yyVal;
};
states[482] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[483] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[484] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /* Workaround for JRUBY-2326 (MRI does not enter this production for some reason)*/
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_method_add_block", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[485] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v4 = p.dispatch("on_call", v1, v2, v3);
                    v5 = v4;
                    v6 = ((IRubyObject)yyVals[0+yyTop].value);
                    v7 = v6 == null ? v5 : p.dispatch("on_method_add_arg", v5, v6);
                    yyVal = v7;}
  return yyVal;
};
states[486] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v3 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v4 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v5 = p.dispatch("on_command_call", v1, v2, v3, v4);
                    v6 = v5;
                    v7 = ((IRubyObject)yyVals[0+yyTop].value);
                    v8 = v7 == null ? v6 : p.dispatch("on_method_add_block", v6, v7);
                    yyVal = v8;}
  return yyVal;
};
states[487] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v3 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v4 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v5 = p.dispatch("on_command_call", v1, v2, v3, v4);
                    v6 = v5;
                    v7 = ((IRubyObject)yyVals[0+yyTop].value);
                    v8 = p.dispatch("on_method_add_block", v6, v7);
                    yyVal = v8;}
  return yyVal;
};
states[488] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_fcall", v1);
                    v3 = v2;
                    v4 = ((IRubyObject)yyVals[0+yyTop].value);
                    v5 = p.dispatch("on_method_add_arg", v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[489] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v4 = p.dispatch("on_call", v1, v2, v3);
                    v5 = v4;
                    v6 = ((IRubyObject)yyVals[0+yyTop].value);
                    v7 = v6 == null ? v5 : p.dispatch("on_method_add_arg", v5, v6);
                    yyVal = v7;}
  return yyVal;
};
states[490] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = p.intern("::");
                    v3 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v4 = p.dispatch("on_call", v1, v2, v3);
                    v5 = v4;
                    v6 = ((IRubyObject)yyVals[0+yyTop].value);
                    v7 = p.dispatch("on_method_add_arg", v5, v6);
                    yyVal = v7;}
  return yyVal;
};
states[491] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.intern("::");
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_call", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[492] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.intern("call");
                    v4 = p.dispatch("on_call", v1, v2, v3);
                    v5 = v4;
                    v6 = ((IRubyObject)yyVals[0+yyTop].value);
                    v7 = p.dispatch("on_method_add_arg", v5, v6);
                    yyVal = v7;}
  return yyVal;
};
states[493] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.intern("::");
                    v3 = p.intern("call");
                    v4 = p.dispatch("on_call", v1, v2, v3);
                    v5 = v4;
                    v6 = ((IRubyObject)yyVals[0+yyTop].value);
                    v7 = p.dispatch("on_method_add_arg", v5, v6);
                    yyVal = v7;}
  return yyVal;
};
states[494] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_super", v1);
                    yyVal = v2;}
  return yyVal;
};
states[495] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_zsuper");
                    yyVal = v1;}
  return yyVal;
};
states[496] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v3 = p.dispatch("on_aref", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[497] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[498] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[499] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pushBlockScope();
  return yyVal;
};
states[500] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    IRubyObject it_id = p.it_id();
                    int max_numparam = p.restoreMaxNumParam(((Integer)yyVals[-5+yyTop].value));
                    p.set_it_id(((IRubyObject)yyVals[-3+yyTop].value));
                    /* Changed from MRI args_with_numbered put into parser codepath and not used by ripper (since it is just a passthrough method and types do not match).*/
{IRubyObject v1, v2, v3;
                    v1 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_brace_block", v1, v2);
                    yyVal = v3;}
                    p.numparam_pop(((Node)yyVals[-4+yyTop].value));
                    p.popCurrentScope();                    
  return yyVal;
};
states[501] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pushBlockScope();
                    p.getCmdArgumentState().push0();
  return yyVal;
};
states[502] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    IRubyObject it_id = p.it_id();
                    int max_numparam = p.restoreMaxNumParam(((Integer)yyVals[-5+yyTop].value));
                    p.set_it_id(((IRubyObject)yyVals[-3+yyTop].value));
                    /* Changed from MRI args_with_numbered put into parser codepath and not used by ripper (since it is just a passthrough method and types do not match).*/
{IRubyObject v1, v2, v3;
                    v1 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_do_block", v1, v2);
                    yyVal = v3;}
                    p.getCmdArgumentState().pop();
                    p.numparam_pop(((Node)yyVals[-4+yyTop].value));
                    p.popCurrentScope();
  return yyVal;
};
states[503] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_args_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[504] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_args_add_star", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[505] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_args_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[506] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_args_add_star", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[507] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.escape(((IRubyObject)yyVals[0+yyTop].value));
                    v4 = p.dispatch("on_when", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[510] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.push_pvtbl();
  return yyVal;
};
states[511] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.push_pktbl();
  return yyVal;
};
states[512] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.getLexContext().clone();
                    p.setState(EXPR_BEG|EXPR_LABEL);
                    p.setCommandStart(false);
                    p.getLexContext().in_kwarg = true;
  return yyVal;
};
states[513] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pvtbl(((Set)yyVals[-3+yyTop].value));
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    p.getLexContext().in_kwarg = ((LexContext)yyVals[-4+yyTop].value).in_kwarg;
  return yyVal;
};
states[514] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.escape(((IRubyObject)yyVals[0+yyTop].value));
                    v4 = p.dispatch("on_in", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[516] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[518] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = p.dispatch("on_if_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[519] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = p.dispatch("on_unless_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[521] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, ((IRubyObject)yyVals[-1+yyTop].value),
                                                   p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, true, null, null));
  return yyVal;
};
states[522] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, ((IRubyObject)yyVals[-2+yyTop].value), ((RubyArray)yyVals[0+yyTop].value));
  return yyVal;
};
states[523] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_find_pattern(null, ((RubyArray)yyVals[0+yyTop].value));
  return yyVal;
};
states[524] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, null, ((RubyArray)yyVals[0+yyTop].value));
  return yyVal;
};
states[525] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern(null, ((RubyArray)yyVals[0+yyTop].value));
  return yyVal;
};
states[527] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.symbolID(EQ_GT);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_binary", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[529] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.symbolID(OR);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_binary", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[531] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Set)yyVals[0+yyTop].value);
{                    yyVal = p.get_value(((Set)yyVals[0+yyTop].value));}
  return yyVal;
};
states[532] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Set)yyVals[0+yyTop].value);
{                    yyVal = p.get_value(((Set)yyVals[0+yyTop].value));}
  return yyVal;
};
states[535] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), ((IRubyObject)yyVals[-3+yyTop].value), null, ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[536] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_find_pattern(((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[537] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_hash_pattern(((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[538] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), ((IRubyObject)yyVals[-2+yyTop].value), null,
                                                    p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, false, null, null));
  return yyVal;
};
states[539] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), ((IRubyObject)yyVals[-3+yyTop].value), null, ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[540] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_find_pattern(((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[541] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_hash_pattern(((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[542] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), ((IRubyObject)yyVals[-2+yyTop].value), null,
                            p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, false, null, null));
  return yyVal;
};
states[543] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, null, ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[544] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_find_pattern(null, ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[545] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, null,
                            p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, false, null, null));
  return yyVal;
};
states[546] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_kwarg = false;
  return yyVal;
};
states[547] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-4+yyTop].value));
                    p.getLexContext().in_kwarg = ((LexContext)yyVals[-3+yyTop].value).in_kwarg;
                    yyVal = p.new_hash_pattern(null, ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[548] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern(null, p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), p.none(), null));
  return yyVal;
};
states[549] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[550] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                        yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), p.new_array(((IRubyObject)yyVals[0+yyTop].value)), false, null, null);

  return yyVal;
};
states[551] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[0+yyTop].value), true, null, null);
  return yyVal;
};
states[552] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
			RubyArray pre_args = ((RubyArray)yyVals[-1+yyTop].value).concat(((RubyArray)yyVals[0+yyTop].value));
			yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), pre_args, false, null, null);

  return yyVal;
};
states[553] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-1+yyTop].value), true, ((IRubyObject)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[554] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-3+yyTop].value), true, ((IRubyObject)yyVals[-2+yyTop].value), ((RubyArray)yyVals[0+yyTop].value));
  return yyVal;
};
states[555] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((RubyArray)yyVals[0+yyTop].value);
  return yyVal;
};
states[556] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((RubyArray)yyVals[-1+yyTop].value);
  return yyVal;
};
states[557] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.getContext(), p.get_value(((RubyArray)yyVals[-1+yyTop].value)));;}
  return yyVal;
};
states[558] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, true, ((IRubyObject)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[559] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, true, ((IRubyObject)yyVals[-2+yyTop].value), ((RubyArray)yyVals[0+yyTop].value));
  return yyVal;
};
states[560] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_find_pattern_tail(yyVals[yyTop - count + 1].start(), ((IRubyObject)yyVals[-4+yyTop].value), ((RubyArray)yyVals[-2+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[561] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.error_duplicate_pattern_variable(yyVals[yyTop - count + 2].id);
  return yyVal;
};
states[562] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
{                    yyVal = p.var_field(p.nil());}
  return yyVal;
};
states[564] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.getContext(), p.get_value(((RubyArray)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[565] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[566] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-2+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[567] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[568] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-1+yyTop].value), null);
  return yyVal;
};
states[569] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), null, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[570] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(((RubyArray)yyVals[0+yyTop].value));}
  return yyVal;
};
states[571] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.getContext(), ((RubyArray)yyVals[0+yyTop].value));;}
  return yyVal;
};
states[572] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.error_duplicate_pattern_key(yyVals[yyTop - count + 1].id);
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[-1+yyTop].value)),  p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[573] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.error_duplicate_pattern_key(yyVals[yyTop - count + 1].id);
                    if (yyVals[yyTop - count + 1].id != null && !p.is_local_id(yyVals[yyTop - count + 1].id)) {
                        p.yyerror("key must be valid as local variables");
                    }
                    p.error_duplicate_pattern_variable(yyVals[yyTop - count + 1].id);
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)),  p.nil());}
  return yyVal;
};
states[575] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      if (true) {
                        yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
                      }

                    else {
                        p.yyerror("symbol literal with interpolation is not allowed");
                        yyVal = null;
                    }
  return yyVal;
};
states[576] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[577] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                       yyVal = null;

  return yyVal;
};
states[578] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                       yyVal = p.symbolID(KWNOREST);

  return yyVal;
};
states[579] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[580] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[582] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot2", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[583] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot3", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[584] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.nil();
                    v3 = p.dispatch("on_dot2", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[585] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.nil();
                    v3 = p.dispatch("on_dot3", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[589] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = p.nil();
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot2", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[590] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = p.nil();
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot3", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[595] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[596] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[597] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[598] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[599] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[600] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[601] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[602] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[603] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[604] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[605] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[606] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[607] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[608] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[609] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[610] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_begin", v1);
                    yyVal = v2;}
  return yyVal;
};
states[611] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_top_const_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[612] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_const_path_ref", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[613] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[614] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = p.escape(((IRubyObject)yyVals[-4+yyTop].value));
                    v2 = p.escape(((IRubyObject)yyVals[-3+yyTop].value));
                    v3 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v4 = p.escape(((IRubyObject)yyVals[0+yyTop].value));
                    v5 = p.dispatch("on_rescue", v1, v2, v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[615] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null; 
  return yyVal;
};
states[616] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[617] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);}
  return yyVal;
};
states[619] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[621] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_ensure", v1);
                    yyVal = v2;}
  return yyVal;
};
states[623] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[624] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[625] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);}
  return yyVal;
};
states[626] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[627] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[628] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_string_concat", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[629] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = p.heredoc_dedent(((IRubyObject)yyVals[-1+yyTop].value));
                    v2 = p.dispatch("on_string_literal", v1);
                    yyVal = v2;}
  return yyVal;
};
states[630] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = p.heredoc_dedent(((IRubyObject)yyVals[-1+yyTop].value));
                    v2 = p.dispatch("on_xstring_literal", v1);
                    yyVal = v2;}
  return yyVal;
};
states[631] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_regexp(yyVals[yyTop - count + 2].start(), ((IRubyObject)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[632] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  return yyVal;
};
states[634] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_array", v1);
                    yyVal = v2;}
  return yyVal;
};
states[635] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_words_new");
                    yyVal = v1;}
  return yyVal;
};
states[636] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_words_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[637] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_word_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_word_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[638] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_word_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[639] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_array", v1);
                    yyVal = v2;}
  return yyVal;
};
states[640] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_symbols_new");
                    yyVal = v1;}
  return yyVal;
};
states[641] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_symbols_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[642] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_array", v1);
                    yyVal = v2;}
  return yyVal;
};
states[643] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_array", v1);
                    yyVal = v2;}
  return yyVal;
};
states[644] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_qwords_new");
                    yyVal = v1;}
  return yyVal;
};
states[645] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_qwords_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[646] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_qsymbols_new");
                    yyVal = v1;}
  return yyVal;
};
states[647] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_qsymbols_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[648] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    ByteList aChar = ByteList.create("");
                    aChar.setEncoding(p.getEncoding());
                    yyVal = p.createStr(aChar, 0);
{IRubyObject v1;
                    v1 = p.dispatch("on_string_content");
                    yyVal = v1;}
  return yyVal;
};
states[649] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_string_add", v1, v2);
                    yyVal = v3;}
                    /* JRuby changed (removed)*/
  return yyVal;
};
states[650] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_xstring_new");
                    yyVal = v1;}
  return yyVal;
};
states[651] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_xstring_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[652] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_regexp_new");
                    yyVal = v1;}
  return yyVal;
};
states[653] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /* FIXME: mri is different here.*/
			yyVal = p.dispatch("on_regexp_add", ((IRubyObject)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));

  return yyVal;
};
states[654] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
{                    yyVal = p.ripper_new_yylval(null, p.get_value(((IRubyObject)yyVals[0+yyTop].value)), ((IRubyObject)yyVals[0+yyTop].value));}
  return yyVal;
};
states[655] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.getStrTerm();
                    p.setStrTerm(null);
                    p.setState(EXPR_BEG);
  return yyVal;
};
states[656] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setStrTerm(((StrTerm)yyVals[-1+yyTop].value));
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_string_dvar", v1);
                    yyVal = v2;}
  return yyVal;
};
states[657] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = p.getStrTerm();
                   p.setStrTerm(null);
                   p.getConditionState().push0();
                   p.getCmdArgumentState().push0();
  return yyVal;
};
states[658] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = p.getState();
                   p.setState(EXPR_BEG);
  return yyVal;
};
states[659] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = p.getBraceNest();
                   p.setBraceNest(0);
  return yyVal;
};
states[660] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = p.getHeredocIndent();
                   p.setHeredocIndent(0);
  return yyVal;
};
states[661] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   p.getConditionState().pop();
                   p.getCmdArgumentState().pop();
                   p.setStrTerm(((StrTerm)yyVals[-5+yyTop].value));
                   p.setState(((Integer)yyVals[-4+yyTop].value));
                   p.setBraceNest(((Integer)yyVals[-3+yyTop].value));
                   p.setHeredocIndent(((Integer)yyVals[-2+yyTop].value));
                   p.setHeredocLineIndent(-1);

{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_string_embexpr", v1);
                    yyVal = v2;}
  return yyVal;
};
states[664] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[668] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_END);
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_symbol", v1);
                    v3 = v2;
                    v4 = p.dispatch("on_symbol_literal", v3);
                    yyVal = v4;}
  return yyVal;
};
states[671] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_END);

{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_dyna_symbol", v1);
                    yyVal = v2;}
  return yyVal;
};
states[672] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);  
  return yyVal;
};
states[673] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = p.intern("-@");
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_unary", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[674] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[675] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[676] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[677] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[681] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (p.id_is_var(yyVals[yyTop - count + 1].id)) {
                        yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop].value));
                    } else {
                        yyVal = p.dispatch("on_vcall", ((IRubyObject)yyVals[0+yyTop].value));
                    }

  return yyVal;
};
states[682] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (p.id_is_var(yyVals[yyTop - count + 1].id)) {
                        yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop].value));
                    } else {
                        yyVal = p.dispatch("on_vcall", ((IRubyObject)yyVals[0+yyTop].value));
                    }

  return yyVal;
};
states[683] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (p.id_is_var(yyVals[yyTop - count + 1].id)) {
                        yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop].value));
                    } else {
                        yyVal = p.dispatch("on_vcall", ((IRubyObject)yyVals[0+yyTop].value));
                    }

  return yyVal;
};
states[684] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (p.id_is_var(yyVals[yyTop - count + 1].id)) {
                        yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop].value));
                    } else {
                        yyVal = p.dispatch("on_vcall", ((IRubyObject)yyVals[0+yyTop].value));
                    }

  return yyVal;
};
states[685] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (p.id_is_var(yyVals[yyTop - count + 1].id)) {
                        yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop].value));
                    } else {
                        yyVal = p.dispatch("on_vcall", ((IRubyObject)yyVals[0+yyTop].value));
                    }

  return yyVal;
};
states[686] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[687] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[688] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[689] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[690] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[691] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[692] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
                    /* mri:keyword_variable*/
  return yyVal;
};
states[693] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

                    
  return yyVal;
};
states[694] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[695] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[696] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[697] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[698] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[699] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[700] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[701] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[702] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[703] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[704] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

                    /* mri:keyword_variable*/
  return yyVal;
};
states[705] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[706] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[707] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   p.setState(EXPR_BEG);
                   p.setCommandStart(true);
  return yyVal;
};
states[708] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[709] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.nil();}
  return yyVal;
};
states[711] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = false;
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, null, null, 
                                    p.new_args_tail(p.src_line(), null, (ByteList) null, (ByteList) null));
  return yyVal;
};
states[712] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_paren", v1);
                    yyVal = v2;}
                    p.setState(EXPR_BEG);
                    p.getLexContext().in_argdef = false;
                    p.setCommandStart(true);
  return yyVal;
};
states[713] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[714] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.getLexContext().clone();
                    p.getLexContext().in_kwarg = true;
                    p.getLexContext().in_argdef = true;
                    p.setState(p.getState() | EXPR_LABEL);
  return yyVal;
};
states[715] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_kwarg = ((LexContext)yyVals[-2+yyTop].value).in_kwarg;
                    ctxt.in_argdef = false;
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
                    p.setState(EXPR_BEG);
                    p.setCommandStart(true);
  return yyVal;
};
states[716] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-3+yyTop].value), ((IRubyObject)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[717] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-1+yyTop].value), (ByteList) null, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[718] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(p.src_line(), null, ((IRubyObject)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[719] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), null, (ByteList) null, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[720] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.add_forwarding_args();
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), null, ((IRubyObject)yyVals[0+yyTop].value), FWD_BLOCK);
  return yyVal;
};
states[721] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[722] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(p.src_line(), null, (ByteList) null, (ByteList) null);
  return yyVal;
};
states[723] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-5+yyTop].value), ((RubyArray)yyVals[-3+yyTop].value), ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[724] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-7+yyTop].value), ((RubyArray)yyVals[-5+yyTop].value), ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[725] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[726] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-5+yyTop].value), ((RubyArray)yyVals[-3+yyTop].value), null, ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[727] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-3+yyTop].value), null, ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[728] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-5+yyTop].value), null, ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[729] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-1+yyTop].value), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[730] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-3+yyTop].value), ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[731] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-5+yyTop].value), ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[732] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[733] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-3+yyTop].value), null, ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[734] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[735] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[736] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[737] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(p.src_line(), null, null, null, null, (ArgsTailHolder) null);
  return yyVal;
};
states[738] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_args_forward");
                    yyVal = v1;}
  return yyVal;
};
states[739] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "formal argument cannot be a constant";
{IRubyObject v1, v2, v3;
                    v1 = p.intern(message);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_param_error", v1, v2);
                    yyVal = v3;
                    p.error();}
  return yyVal;
};
states[740] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "formal argument cannot be an instance variable";
{IRubyObject v1, v2, v3;
                    v1 = p.intern(message);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_param_error", v1, v2);
                    yyVal = v3;
                    p.error();}
  return yyVal;
};
states[741] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "formal argument cannot be a global variable";
{IRubyObject v1, v2, v3;
                    v1 = p.intern(message);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_param_error", v1, v2);
                    yyVal = v3;
                    p.error();}
  return yyVal;
};
states[742] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "formal argument cannot be a class variable";
{IRubyObject v1, v2, v3;
                    v1 = p.intern(message);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_param_error", v1, v2);
                    yyVal = v3;
                    p.error();}
  return yyVal;
};
states[743] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value); /* Not really reached*/
  return yyVal;
};
states[744] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.formal_argument(yyVals[yyTop - count + 1].id, ((IRubyObject)yyVals[0+yyTop].value));
                    p.ordinalMaxNumParam();
  return yyVal;
};
states[745] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    RubySymbol name = p.get_id(((IRubyObject)yyVals[0+yyTop].value));
                    p.setCurrentArg(name);
                      p.arg_var(yyVals[yyTop - count + 1].id);
                      yyVal = ((IRubyObject)yyVals[0+yyTop].value);


  return yyVal;
};
states[746] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setCurrentArg(null);
{                    yyVal = p.get_value(((IRubyObject)yyVals[0+yyTop].value));}
  return yyVal;
};
states[747] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_mlhs_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[748] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[749] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.getContext(), p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[750] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.formal_argument(yyVals[yyTop - count + 1].id, ((IRubyObject)yyVals[0+yyTop].value));
                    p.arg_var(yyVals[yyTop - count + 1].id);
                    p.setCurrentArg(p.get_id(((IRubyObject)yyVals[0+yyTop].value)));
                    p.ordinalMaxNumParam();
                    p.getLexContext().in_argdef = false;
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[751] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setCurrentArg(null);
                    p.getLexContext().in_argdef = true;
                      yyVal = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, ((IRubyObject)yyVals[-1+yyTop].value)), ((IRubyObject)yyVals[0+yyTop].value));

  return yyVal;
};
states[752] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setCurrentArg(null);
                    p.getLexContext().in_argdef = true;
                      yyVal = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, ((IRubyObject)yyVals[0+yyTop].value)), p.nil());

  return yyVal;
};
states[753] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = true;
                      yyVal = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, ((IRubyObject)yyVals[-1+yyTop].value)), ((IRubyObject)yyVals[0+yyTop].value));

  return yyVal;
};
states[754] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = true;
                      yyVal = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, ((IRubyObject)yyVals[0+yyTop].value)), p.fals());

  return yyVal;
};
states[755] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[756] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.getContext(), p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[757] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[758] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.getContext(), p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[759] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[760] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[761] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = p.nil();
                    v2 = p.dispatch("on_nokw_param", v1);
                    yyVal = v2;}
  return yyVal;
};
states[762] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.shadowing_lvar(yyVals[yyTop - count + 2].id);
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_kwrest_param", v1);
                    yyVal = v2;}
  return yyVal;
};
states[763] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = p.nil();
                    v2 = p.dispatch("on_kwrest_param", v1);
                    yyVal = v2;}
  return yyVal;
};
states[764] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, ((IRubyObject)yyVals[-2+yyTop].value)), ((IRubyObject)yyVals[0+yyTop].value));

  return yyVal;
};
states[765] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = true;
                    p.setCurrentArg(null);
                      yyVal = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, ((IRubyObject)yyVals[-2+yyTop].value)), ((IRubyObject)yyVals[0+yyTop].value));

  return yyVal;
};
states[766] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[767] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.getContext(), p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[768] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[769] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.getContext(), p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[770] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = STAR;
  return yyVal;
};
states[771] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[772] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (!p.is_local_id(yyVals[yyTop - count + 2].id)) {
                        p.yyerror("rest argument must be local variable");
                    }
                    yyVal = p.arg_var(p.shadowing_lvar(yyVals[yyTop - count + 2].id));
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_rest_param", v1);
                    yyVal = v2;}
  return yyVal;
};
states[773] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = p.nil();
                    v2 = p.dispatch("on_rest_param", v1);
                    yyVal = v2;}
  return yyVal;
};
states[774] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = AMPERSAND;
  return yyVal;
};
states[775] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[776] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (!p.is_local_id(yyVals[yyTop - count + 2].id)) {
                        p.yyerror("block argument must be local variable");
                    }
                    yyVal = p.arg_var(p.shadowing_lvar(yyVals[yyTop - count + 2].id));
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_blockarg", v1);
                    yyVal = v2;}
  return yyVal;
};
states[777] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.arg_var(p.shadowing_lvar(ANON_BLOCK));
                        yyVal = p.dispatch("on_blockarg", p.nil());

  return yyVal;
};
states[778] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[779] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[780] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((IRubyObject)yyVals[0+yyTop].value));
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[781] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_BEG);
  return yyVal;
};
states[782] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[783] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  return yyVal;
};
states[784] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((RubyArray)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_assoclist_from_args", v1);
                    yyVal = v2;}
  return yyVal;
};
states[785] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[786] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.getContext(), p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[787] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_assoc_new", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[788] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_assoc_new", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[789] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.nil();
                    v3 = p.dispatch("on_assoc_new", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[790] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.dispatch("on_dyna_symbol", v1);
                    v3 = v2;
                    v4 = ((IRubyObject)yyVals[0+yyTop].value);
                    v5 = p.dispatch("on_assoc_new", v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[791] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_assoc_splat", v1);
                    yyVal = v2;}
  return yyVal;
};
states[792] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.forwarding_arg_check(FWD_KWREST, FWD_ALL, "keyword rest");
{IRubyObject v1, v2;
                    v1 = p.nil();
                    v2 = p.dispatch("on_assoc_splat", v1);
                    yyVal = v2;}
  return yyVal;
};
states[793] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[794] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[795] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[796] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[797] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[798] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[799] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[800] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[801] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[802] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[803] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[804] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[806] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[811] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RPAREN;
  return yyVal;
};
states[812] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RBRACKET;
  return yyVal;
};
states[813] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RCURLY;
  return yyVal;
};
states[821] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = null;
  return yyVal;
};
states[822] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  yyVal = null;
  return yyVal;
};
}
					// line 4635 "ripper_RubyParser.out"

}
					// line 14748 "-"
