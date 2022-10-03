// created by jay 1.0.2 (c) 2002-2004 ats@cs.rit.edu
// skeleton Java 1.0 (c) 2002 ats@cs.rit.edu

					// line 2 "ripper_RubyParser.out"
// We use ERB for ripper grammar and we need an alternative substitution value.
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

import org.jruby.RubySymbol;
import org.jruby.ast.*;
import org.jruby.common.IRubyWarnings;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.lexer.LexerSource;
import org.jruby.lexer.LexingCommon;
import org.jruby.ext.ripper.StrTerm;
import org.jruby.util.KeyValuePair; import org.jruby.RubyArray; import org.jruby.ext.ripper.Holder;
import org.jruby.util.ByteList;
import org.jruby.util.CommonByteLists;
import org.jruby.util.KeyValuePair;
import org.jruby.util.StringSupport;
import org.jruby.lexer.yacc.LexContext;
import org.jruby.ext.ripper.RubyLexer;
import org.jruby.lexer.yacc.StackState;
import org.jruby.parser.ProductionState;
import org.jruby.parser.ParserState;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.lexer.yacc.RubyLexer.*;
import static org.jruby.lexer.LexingCommon.AMPERSAND;
import static org.jruby.lexer.LexingCommon.AMPERSAND_AMPERSAND;
import static org.jruby.lexer.LexingCommon.AMPERSAND_DOT;
import static org.jruby.lexer.LexingCommon.BACKTICK;
import static org.jruby.lexer.LexingCommon.BANG;
import static org.jruby.lexer.LexingCommon.CARET;
import static org.jruby.lexer.LexingCommon.DOLLAR_BANG;
import static org.jruby.lexer.LexingCommon.DOT;
import static org.jruby.lexer.LexingCommon.GT;
import static org.jruby.lexer.LexingCommon.LBRACKET_RBRACKET;
import static org.jruby.lexer.LexingCommon.LCURLY;
import static org.jruby.lexer.LexingCommon.LT;
import static org.jruby.lexer.LexingCommon.LT_LT;
import static org.jruby.lexer.LexingCommon.MINUS;
import static org.jruby.lexer.LexingCommon.PERCENT;
import static org.jruby.lexer.LexingCommon.OR;
import static org.jruby.lexer.LexingCommon.OR_OR;
import static org.jruby.lexer.LexingCommon.PLUS;
import static org.jruby.lexer.LexingCommon.RBRACKET;
import static org.jruby.lexer.LexingCommon.RCURLY;
import static org.jruby.lexer.LexingCommon.RPAREN;
import static org.jruby.lexer.LexingCommon.SLASH;
import static org.jruby.lexer.LexingCommon.STAR;
import static org.jruby.lexer.LexingCommon.TILDE;
import static org.jruby.lexer.LexingCommon.EXPR_BEG;
import static org.jruby.lexer.LexingCommon.EXPR_FITEM;
import static org.jruby.lexer.LexingCommon.EXPR_FNAME;
import static org.jruby.lexer.LexingCommon.EXPR_ENDFN;
import static org.jruby.lexer.LexingCommon.EXPR_ENDARG;
import static org.jruby.lexer.LexingCommon.EXPR_END;
import static org.jruby.lexer.LexingCommon.EXPR_LABEL;
import static org.jruby.util.CommonByteLists.FWD_BLOCK;
import static org.jruby.util.CommonByteLists.FWD_KWREST;
 
 public class RipperParser extends RipperParserBase {
    public RipperParser(ThreadContext context, IRubyObject ripper, LexerSource source) {
        super(context, ripper, source);
    }
					// line 98 "-"
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
  public static final int tUMINUS_NUM = 322;
  public static final int tSP = 323;
  public static final int tUPLUS = 324;
  public static final int tUMINUS = 325;
  public static final int tPOW = 326;
  public static final int tCMP = 327;
  public static final int tEQ = 328;
  public static final int tEQQ = 329;
  public static final int tNEQ = 330;
  public static final int tGEQ = 331;
  public static final int tLEQ = 332;
  public static final int tANDOP = 333;
  public static final int tOROP = 334;
  public static final int tMATCH = 335;
  public static final int tNMATCH = 336;
  public static final int tDOT2 = 337;
  public static final int tDOT3 = 338;
  public static final int tBDOT2 = 339;
  public static final int tBDOT3 = 340;
  public static final int tAREF = 341;
  public static final int tASET = 342;
  public static final int tLSHFT = 343;
  public static final int tRSHFT = 344;
  public static final int tANDDOT = 345;
  public static final int tCOLON2 = 346;
  public static final int tCOLON3 = 347;
  public static final int tOP_ASGN = 348;
  public static final int tASSOC = 349;
  public static final int tLPAREN = 350;
  public static final int tLPAREN_ARG = 351;
  public static final int tLBRACK = 352;
  public static final int tLBRACE = 353;
  public static final int tLBRACE_ARG = 354;
  public static final int tSTAR = 355;
  public static final int tDSTAR = 356;
  public static final int tAMPER = 357;
  public static final int tLAMBDA = 358;
  public static final int tSYMBEG = 359;
  public static final int tSTRING_BEG = 360;
  public static final int tXSTRING_BEG = 361;
  public static final int tREGEXP_BEG = 362;
  public static final int tWORDS_BEG = 363;
  public static final int tQWORDS_BEG = 364;
  public static final int tSTRING_END = 365;
  public static final int tSYMBOLS_BEG = 366;
  public static final int tQSYMBOLS_BEG = 367;
  public static final int tSTRING_DEND = 368;
  public static final int tSTRING_DBEG = 369;
  public static final int tSTRING_DVAR = 370;
  public static final int tLAMBEG = 371;
  public static final int tLABEL_END = 372;
  public static final int tIGNORED_NL = 373;
  public static final int tCOMMENT = 374;
  public static final int tEMBDOC_BEG = 375;
  public static final int tEMBDOC = 376;
  public static final int tEMBDOC_END = 377;
  public static final int tHEREDOC_BEG = 378;
  public static final int tHEREDOC_END = 379;
  public static final int tLOWEST = 380;
  public static final int yyErrorCode = 256;

  /** number of final state.
    */
  protected static final int yyFinal = 1;

  /** parser tables.
      Order is mandated by <i>jay</i>.
    */
  protected static final short[] yyLhs = {
//yyLhs 818
    -1,   214,     0,    30,    31,    31,    31,    31,    32,    32,
    33,   217,    34,    34,    35,    36,    36,    36,    36,    37,
   218,    37,   219,    38,    38,    38,    38,    38,    38,    38,
    38,    38,    38,    38,    38,    38,    38,    38,    38,    38,
    77,    77,    77,    77,    77,    77,    77,    77,    77,    77,
    77,    77,    75,    75,    75,    39,    39,    39,    39,    39,
   221,   222,    39,   223,   224,    39,    39,    27,    28,   225,
    29,    45,   226,    46,    43,    43,    82,    82,   120,    49,
    42,    42,    42,    42,    42,    42,    42,    42,    42,    42,
    42,   125,   125,   131,   131,   127,   127,   127,   127,   127,
   127,   127,   127,   127,   127,   128,   128,   126,   126,   130,
   130,   129,   129,   129,   129,   129,   129,   129,   129,   129,
   129,   129,   129,   129,   129,   129,   129,   129,   129,   129,
   122,   122,   122,   122,   122,   122,   122,   122,   122,   122,
   122,   122,   122,   122,   122,   122,   122,   122,   122,   159,
   159,    26,    26,    26,   161,   161,   161,   161,   161,   124,
   124,    99,   228,    99,   160,   160,   160,   160,   160,   160,
   160,   160,   160,   160,   160,   160,   160,   160,   160,   160,
   160,   160,   160,   160,   160,   160,   160,   160,   160,   160,
   160,   160,   160,   160,   172,   172,   172,   172,   172,   172,
   172,   172,   172,   172,   172,   172,   172,   172,   172,   172,
   172,   172,   172,   172,   172,   172,   172,   172,   172,   172,
   172,   172,   172,   172,   172,   172,   172,   172,   172,   172,
   172,   172,   172,   172,   172,    40,    40,    40,    40,    40,
    40,    40,    40,    40,    40,    40,    40,    40,    40,    40,
    40,    40,    40,    40,    40,    40,    40,    40,    40,    40,
    40,    40,    40,    40,    40,    40,    40,    40,    40,    40,
    40,    40,    40,    40,    40,   229,    40,    40,    40,    40,
    40,    40,    40,   173,   173,   173,   173,    50,    50,   185,
   185,    47,    70,    70,    70,    70,    76,    76,    63,    63,
    63,    64,    64,    62,    62,    62,    62,    62,    61,    61,
    61,    61,    61,   231,    69,    72,    72,    71,    71,    60,
    60,    60,    60,    79,    79,    78,    78,    78,    41,    41,
    41,    41,    41,    41,    41,    41,    41,    41,    41,   232,
    41,   233,    41,   234,    41,    41,    41,    41,    41,    41,
    41,    41,    41,    41,   235,    41,    41,    41,    41,    41,
    41,    41,    41,    41,    41,    41,   237,    41,   238,    41,
    41,    41,   239,    41,   241,    41,   242,    41,    41,    41,
    41,    41,    41,    41,    48,   197,   198,   211,   207,   208,
   210,   209,   193,   194,   205,   199,   200,   201,   202,   196,
   195,   203,   206,   192,   236,   236,   236,   227,   227,    51,
    51,    52,    52,   102,   102,    92,    92,    93,    93,    94,
    94,    94,    94,    94,    95,    95,   181,   181,   244,   243,
    67,    67,    67,    67,    68,    68,   183,   103,   103,   103,
   103,   103,   103,   103,   103,   103,   103,   103,   103,   103,
   103,   103,   104,   104,   105,   105,   112,   112,   111,   111,
   113,   113,   245,   246,   247,   248,   114,   115,   115,   116,
   116,   121,    81,    81,    81,    81,    44,    44,    44,    44,
    44,    44,    44,    44,    44,   119,   119,   249,   250,   251,
   117,   252,   253,   254,   118,    54,    54,    54,    54,    53,
    55,    55,   255,   256,   257,   132,   133,   133,   134,   134,
   134,   135,   135,   135,   135,   135,   135,   136,   137,   137,
   138,   138,   212,   213,   139,   139,   139,   139,   139,   139,
   139,   139,   139,   139,   139,   139,   139,   258,   139,   139,
   259,   139,   141,   141,   141,   141,   141,   141,   141,   141,
   142,   142,   143,   143,   140,   175,   175,   144,   144,   145,
   152,   152,   152,   152,   153,   153,   154,   154,   179,   179,
   176,   176,   177,   178,   178,   146,   146,   146,   146,   146,
   146,   146,   146,   146,   146,   147,   147,   147,   147,   147,
   147,   147,   147,   147,   147,   147,   147,   147,   147,   147,
   147,   148,   149,   149,   150,   151,   151,   151,    56,    56,
    57,    57,    57,    58,    58,    59,    59,    20,    20,     2,
     3,     3,     3,     4,     5,     6,    11,    16,    16,    19,
    19,    12,    13,    13,    14,    15,    17,    17,    18,    18,
     7,     7,     8,     8,     9,     9,    10,   260,    10,   261,
   262,   263,   264,    10,   101,   101,   101,   101,    25,    25,
    23,   155,   155,   155,   155,    24,    21,    21,   184,   184,
   184,    22,    22,    22,    22,    73,    73,    73,    73,    73,
    73,    73,    73,    73,    73,    73,    73,    74,    74,    74,
    74,    74,    74,    74,    74,    74,    74,    74,    74,   100,
   100,   265,    80,    80,    86,    86,    87,    85,   266,    85,
    65,    65,    65,    65,    65,    66,    66,    88,    88,    88,
    88,    88,    88,    88,    88,    88,    88,    88,    88,    88,
    88,    88,   182,   166,   166,   166,   166,   165,   165,   169,
    90,    90,    89,    89,   168,   108,   108,   110,   110,   109,
   109,   107,   107,   188,   188,   180,   167,   167,   106,    84,
    83,    83,    91,    91,   187,   187,   162,   162,   186,   186,
   163,   163,   164,   164,     1,   267,     1,    96,    96,    97,
    97,    98,    98,    98,    98,    98,   156,   156,   156,   157,
   157,   157,   157,   158,   158,   158,   174,   174,   170,   170,
   171,   171,   215,   215,   220,   220,   189,   190,   204,   230,
   230,   230,   240,   240,   216,   216,   123,   191,
    }, yyLen = {
//yyLen 818
     2,     0,     2,     2,     1,     1,     3,     2,     1,     2,
     3,     0,     6,     3,     2,     1,     1,     3,     2,     1,
     0,     3,     0,     4,     3,     3,     3,     2,     3,     3,
     3,     3,     3,     4,     1,     4,     4,     6,     4,     1,
     4,     4,     7,     6,     6,     6,     6,     4,     6,     4,
     6,     4,     1,     3,     1,     1,     3,     3,     3,     2,
     0,     0,     5,     0,     0,     5,     1,     1,     2,     0,
     5,     1,     0,     3,     1,     1,     1,     4,     3,     1,
     2,     3,     4,     5,     4,     5,     2,     2,     2,     2,
     2,     1,     3,     1,     3,     1,     2,     3,     5,     2,
     4,     2,     4,     1,     3,     1,     3,     2,     3,     1,
     3,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     4,     3,     3,     3,     3,     2,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     4,     3,     3,     3,     3,     2,     1,     1,
     1,     2,     1,     3,     1,     1,     1,     1,     1,     1,
     1,     1,     0,     4,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     4,     4,     7,     6,     6,
     6,     6,     5,     4,     3,     3,     2,     2,     2,     2,
     3,     3,     3,     3,     3,     3,     4,     2,     2,     3,
     3,     3,     3,     1,     3,     3,     3,     3,     3,     2,
     2,     3,     3,     3,     3,     0,     4,     6,     4,     6,
     4,     6,     1,     1,     1,     1,     1,     3,     3,     1,
     1,     1,     1,     2,     4,     2,     1,     3,     3,     5,
     3,     1,     1,     1,     1,     2,     4,     2,     1,     2,
     2,     4,     1,     0,     2,     2,     1,     2,     1,     1,
     2,     3,     4,     1,     1,     3,     4,     2,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     0,
     4,     0,     3,     0,     4,     3,     3,     2,     3,     3,
     1,     4,     3,     1,     0,     6,     4,     3,     2,     1,
     2,     1,     6,     6,     4,     4,     0,     6,     0,     5,
     5,     6,     0,     6,     0,     7,     0,     5,     4,     4,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     2,     1,     1,     1,
     5,     1,     2,     1,     1,     1,     3,     1,     3,     1,
     3,     5,     1,     3,     2,     1,     1,     1,     0,     2,
     4,     2,     2,     1,     2,     0,     1,     6,     8,     4,
     6,     4,     2,     6,     2,     4,     6,     2,     4,     2,
     4,     1,     1,     1,     3,     4,     1,     4,     1,     3,
     1,     1,     0,     0,     0,     0,     7,     4,     1,     3,
     3,     3,     2,     4,     5,     5,     2,     4,     4,     3,
     3,     3,     2,     1,     4,     3,     3,     0,     0,     0,
     5,     0,     0,     0,     5,     1,     2,     3,     4,     5,
     1,     1,     0,     0,     0,     8,     1,     1,     1,     3,
     3,     1,     2,     3,     1,     1,     1,     1,     3,     1,
     3,     1,     1,     1,     1,     1,     4,     4,     4,     3,
     4,     4,     4,     3,     3,     3,     2,     0,     4,     2,
     0,     4,     1,     1,     2,     3,     5,     2,     4,     1,
     2,     3,     1,     3,     5,     2,     1,     1,     3,     1,
     3,     1,     2,     1,     1,     3,     2,     1,     1,     3,
     2,     1,     2,     1,     1,     1,     3,     3,     2,     2,
     1,     1,     1,     2,     2,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     2,     2,     4,     2,     3,     1,     6,     1,
     1,     1,     1,     2,     1,     2,     1,     1,     1,     1,
     1,     1,     2,     3,     3,     3,     4,     0,     3,     1,
     2,     4,     0,     3,     4,     4,     0,     3,     0,     3,
     0,     2,     0,     2,     0,     2,     1,     0,     3,     0,
     0,     0,     0,     7,     1,     1,     1,     1,     1,     1,
     2,     1,     1,     1,     1,     3,     1,     2,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     0,     4,     0,     1,     1,     3,     1,     0,     3,
     4,     2,     2,     1,     1,     2,     0,     6,     8,     4,
     6,     4,     6,     2,     4,     6,     2,     4,     2,     4,
     1,     0,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     3,     1,     3,     1,     2,     1,     2,     1,     1,
     3,     1,     3,     1,     1,     2,     2,     1,     3,     3,
     1,     3,     1,     3,     1,     1,     2,     1,     1,     1,
     2,     1,     2,     0,     1,     0,     4,     1,     2,     1,
     3,     3,     2,     1,     4,     2,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     0,     1,     0,     1,     2,     2,     2,     0,
     1,     1,     1,     1,     1,     2,     0,     0,
    }, yyDefRed = {
//yyDefRed 1359
     1,     0,     0,     0,   392,   393,   394,     0,   385,   386,
   387,   390,   388,   389,   391,     0,     0,   382,   383,   403,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   671,   672,   673,   674,   620,   699,   700,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   462,     0,
   640,   642,   644,     0,     0,     0,     0,     0,     0,   329,
     0,   621,   330,   331,   332,   334,   333,   335,   328,   617,
   666,   658,   659,   618,     0,     0,     2,     0,     5,     0,
     0,     0,     0,     0,    55,     0,     0,     0,     0,   336,
     0,    34,     0,    75,     0,   361,     0,     4,     0,     0,
    91,     0,   105,    79,     0,     0,     0,   339,     0,     0,
    72,    72,     0,     0,     0,     7,   204,   215,   205,   228,
   201,   221,   211,   210,   231,   232,   226,   209,   208,   203,
   229,   233,   234,   213,   202,   216,   220,   222,   214,   207,
   223,   230,   225,   224,   217,   227,   212,   200,   219,   218,
   199,   206,   197,   198,   194,   195,   196,   154,   156,   155,
   189,   190,   185,   167,   168,   169,   176,   173,   175,   170,
   171,   191,   192,   177,   178,   182,   186,   172,   174,   164,
   165,   166,   179,   180,   181,   183,   184,   187,   188,   193,
   160,     0,   161,   157,   159,   158,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   640,     0,     0,     0,     0,
   308,     0,     0,     0,    89,   312,     0,     0,   779,     0,
     0,    90,     0,    87,     0,     0,   482,    86,     0,   805,
     0,     0,    22,     0,     0,     9,     0,     0,   380,   381,
     0,     0,   257,     0,     0,   350,     0,     0,     0,     0,
     0,    20,     0,     0,     0,    16,     0,    15,     0,     0,
     0,     0,     0,     0,     0,   292,     0,     0,     0,   777,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   384,     0,
     0,     0,   463,   663,   662,   664,     0,   660,   661,     0,
     0,     0,   627,   636,   632,   638,   269,    59,   270,   622,
     0,     0,     0,     0,   705,     0,     0,     0,   812,   813,
     3,     0,   814,     0,     0,     0,     0,     0,     0,     0,
    63,     0,     0,     0,     0,     0,   285,   286,     0,     0,
     0,     0,     0,     0,     0,     0,    60,     0,   283,   284,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   396,
   472,   491,   395,   487,   360,   491,   799,     0,     0,   798,
     0,   476,     0,   358,     0,     0,   801,   800,     0,     0,
     0,     0,     0,     0,     0,   107,    88,   681,   680,   682,
   683,   685,   684,   686,     0,   677,   676,     0,   679,     0,
     0,     0,     0,   337,   152,   376,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   775,     0,
    68,   774,    67,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   413,   414,     0,   368,     0,     0,   162,   782,
     0,   320,   785,   315,     0,     0,     0,     0,     0,     0,
     0,     0,   309,   318,     0,     0,   310,     0,     0,     0,
   352,     0,   314,     0,     0,   304,     0,     0,   303,     0,
     0,   357,    58,    24,    26,    25,     0,   354,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   345,    14,
     0,     0,     0,   342,     0,   810,   293,   348,     0,   295,
   349,   778,     0,   667,     0,   109,     0,   707,     0,     0,
     0,     0,   464,   646,   665,   649,   647,   641,   623,   624,
   643,   625,   645,     0,     0,     0,     0,   738,   735,   734,
   733,   736,   744,   753,   732,     0,   765,   754,   769,   768,
   764,   730,     0,     0,   742,     0,   762,     0,   751,     0,
   713,   739,   737,   426,     0,     0,   427,     0,   714,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   815,     6,
    28,    29,    30,    31,    32,    56,    57,    64,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,    61,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   492,     0,   488,     0,     0,     0,     0,
   481,     0,   479,     0,     0,     0,     0,   791,     0,   480,
     0,   792,   487,    81,     0,   289,   290,     0,   789,   790,
     0,     0,     0,     0,     0,     0,     0,   108,     0,   149,
     0,   151,   701,   372,     0,     0,     0,     0,     0,   405,
     0,     0,     0,   797,   796,    69,     0,     0,     0,     0,
     0,     0,     0,    72,     0,     0,     0,     0,     0,     0,
     0,   781,     0,     0,     0,     0,     0,     0,     0,   317,
     0,     0,   780,     0,     0,   351,   806,     0,   298,     0,
   300,   356,    23,     0,     0,    10,    33,     0,     0,     0,
     0,    21,     0,    17,   344,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   650,     0,   626,   629,     0,     0,
   634,   631,     0,     0,   635,     0,     0,   417,     0,     0,
     0,   415,   706,     0,   723,     0,   726,     0,   711,     0,
   728,   745,     0,     0,     0,   712,   770,   766,   755,   756,
   402,   378,   397,     0,   609,     0,     0,     0,   709,   379,
     0,     0,     0,     0,     0,   471,   493,   485,   489,   486,
     0,     0,   478,     0,     0,     0,     0,     0,     0,   302,
   477,     0,   301,     0,     0,     0,     0,    41,   236,    54,
     0,     0,     0,     0,    51,   243,     0,     0,   319,     0,
    40,   235,    36,    35,     0,   323,     0,   106,     0,     0,
     0,     0,     0,     0,     0,   153,     0,     0,   340,     0,
   406,     0,     0,   364,   408,    73,   407,   365,     0,     0,
     0,     0,     0,     0,   502,     0,     0,   399,     0,     0,
     0,   163,   784,     0,     0,     0,     0,     0,   322,   311,
     0,     0,     0,   242,   294,   110,     0,     0,   468,   465,
   651,   654,   655,   656,   657,   648,   628,   630,   637,   633,
   639,     0,   424,     0,   741,     0,   715,   743,     0,     0,
     0,   763,     0,   752,   772,     0,     0,     0,   740,   758,
   429,   398,   400,    13,   616,    11,     0,     0,     0,   611,
   612,     0,     0,     0,     0,   594,   593,   595,   596,   598,
   597,   599,   601,   607,   568,     0,     0,     0,   540,     0,
     0,     0,   640,     0,   586,   587,   588,   589,   591,   590,
   592,   585,   600,    65,     0,   517,     0,   521,   514,   515,
   524,     0,   525,   580,   581,     0,   516,     0,   564,     0,
   573,   574,   563,     0,     0,    62,     0,     0,     0,     0,
     0,    85,     0,   807,     0,     0,    83,    78,     0,     0,
     0,     0,     0,     0,   474,   475,     0,     0,     0,     0,
     0,     0,     0,   484,   377,   401,     0,   409,   411,     0,
     0,   776,    70,     0,     0,   503,   370,     0,   369,     0,
   495,     0,     0,     0,     0,     0,     0,     0,     0,   299,
   355,     0,     0,   652,   416,   418,     0,     0,     0,   719,
     0,   721,     0,   727,     0,   724,   710,   729,     0,   615,
     0,     0,   614,     0,     0,     0,     0,   583,   584,   150,
   605,     0,     0,     0,     0,     0,   549,     0,   536,   539,
     0,     0,   555,     0,   602,   669,   668,   670,     0,   603,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   572,   570,     0,     0,     0,   453,   452,
     0,    46,   240,    45,   241,     0,    43,   238,    44,   239,
     0,    53,     0,     0,     0,     0,     0,     0,     0,     0,
    37,     0,   702,   373,   362,   412,     0,   371,     0,   367,
   496,     0,     0,   363,     0,     0,     0,     0,     0,   466,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   613,     0,     0,   535,   534,     0,     0,
     0,   550,   808,   640,     0,   569,     0,   518,   513,     0,
   520,   576,   577,   606,   533,   529,     0,     0,     0,     0,
     0,     0,   565,   560,     0,   557,   451,     0,   760,     0,
     0,     0,   749,     0,     0,   433,     0,     0,     0,   494,
   490,    42,   237,     0,     0,   375,     0,     0,     0,     0,
   497,     0,   467,     0,     0,     0,     0,     0,   720,     0,
   717,   722,   725,    12,     0,     0,     0,     0,     0,   541,
     0,     0,   551,     0,   538,   604,     0,   527,   526,   528,
   531,   530,   532,     0,     0,   447,     0,   444,   442,     0,
     0,   431,   454,     0,   449,     0,     0,     0,     0,     0,
   432,     0,   504,     0,     0,   498,   500,   501,   499,   460,
     0,   458,   461,   470,   469,   653,     0,     0,     0,     0,
     0,     0,   608,     0,     0,     0,   558,   554,   434,   761,
     0,     0,     0,     0,   455,   750,     0,     0,   347,     0,
     0,   410,     0,   509,   510,     0,   457,   718,     0,     0,
     0,     0,   448,     0,   445,     0,   439,     0,   441,   430,
   450,     0,     0,     0,   459,     0,     0,     0,     0,   506,
   507,   505,   446,   440,     0,   437,   443,     0,   438,
    }, yyDgoto = {
//yyDgoto 268
     1,   439,    69,    70,    71,    72,    73,   316,   320,   321,
   547,    74,    75,   555,    76,    77,   553,   554,   556,   748,
    78,    79,    80,    81,    82,    83,   421,   440,   227,   228,
    86,    87,    88,   255,   592,   593,   274,   275,   276,    90,
    91,    92,    93,    94,    95,   428,   443,   231,   263,   264,
    98,  1016,  1017,   868,  1031,  1288,   783,   928,  1061,   923,
   644,   495,   496,   640,   810,   906,   764,  1308,  1265,   243,
   283,   482,   235,    99,   236,   830,   831,   101,   832,   836,
   673,   102,   103,  1207,  1208,   331,   332,   333,   572,   573,
   574,   575,   757,   758,   759,   760,   287,   497,   238,   201,
   239,   895,   461,  1210,  1107,  1108,   576,   577,   578,  1211,
  1212,  1290,  1145,  1291,   105,   889,  1149,   634,   632,   393,
   653,   380,   240,   277,   202,   108,   109,   110,   111,   112,
   536,   279,   865,  1351,  1227,   963,  1179,   965,   966,   967,
   968,  1074,  1075,  1076,  1204,  1077,   970,   971,   972,   973,
   974,   975,   976,   977,   978,   317,   113,   728,   642,   424,
   643,   204,   579,   580,   768,   581,   582,   583,   584,   918,
   676,   398,   205,   378,   685,   979,   980,   981,   982,   983,
   586,   587,   588,  1268,  1089,   657,   589,   590,   591,   490,
   805,   483,   265,   115,   116,  1019,   869,   117,   118,   385,
   381,   785,   926,  1020,  1079,   119,   781,   120,   121,   122,
   123,   124,  1098,  1099,     2,   340,   466,  1058,   516,   506,
   491,   621,   793,   607,   792,   852,   444,   855,   697,   508,
   526,   244,   426,   281,   522,   723,   680,   866,   695,   842,
   681,   840,   677,   772,   773,   312,   542,   743,  1042,   635,
   798,   988,   633,   796,   987,  1025,  1138,  1322,  1081,  1071,
   745,   744,   890,  1043,  1150,   841,   335,   682,
    }, yySindex = {
//yySindex 1359
     0,     0, 24101, 24727,     0,     0,     0, 27697,     0,     0,
     0,     0,     0,     0,     0, 22576, 22576,     0,     0,     0,
   125,   168,     0,     0,     0,     0,   530, 27594,   143,    63,
    96,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  1217, 26699, 26699,
 26699, 26699,   -48, 24218, 24840, 25427, 25785, 28287,     0, 28117,
     0,     0,     0,   276,   327,   362,   404, 26810, 26699,     0,
  -138,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   420,   420,     0,   214,     0,  1334,
   587, 23306,     0,   240,     0,   -36,   106,   549,   100,     0,
   201,     0,   135,     0,   220,     0,   486,     0,   536, 30277,
     0,   583,     0,     0, 22576, 30388, 30610,     0, 26922, 28017,
     0,     0, 30499, 23878, 26922,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   590,     0,     0,     0,     0,     0,     0,     0,     0,
   630,     0,     0,     0,     0,     0,     0,     0,     0, 26699,
   370, 24331, 26699, 26699, 26699,     0, 26699,   420,   420, 29372,
     0,   401,   138,   708,     0,     0,   407,   731,     0,   468,
   762,     0, 23766,     0, 22576, 24957,     0,     0, 23989,     0,
 26922,   954,     0,   800, 24101,     0, 24331,   517,     0,     0,
   125,   168,     0,   238,   549,     0,   524, 18582, 18582,   518,
 24840,     0, 24218,   816,   214,     0,  1334,     0,     0,   143,
  1334,   143,   228,   792,   287,     0,   401,   751,   287,     0,
     0,     0,     0,     0,   143,     0,     0,     0,     0,     0,
     0,     0,     0,  1217,   582, 30721,   420,   420,     0,   264,
     0,   855,     0,     0,     0,     0,   689,     0,     0,   698,
   880,   224,     0,     0,     0,     0,     0,     0,     0,     0,
  6860, 24331,   842,     0,     0,  6860, 24331,   852,     0,     0,
     0, 24497,     0, 26922, 26922, 26922, 26922, 24840, 26922, 26922,
     0, 26699, 26699, 26699, 26699, 26699,     0,     0, 26699, 26699,
 26699, 26699, 26699, 26699, 26699, 26699,     0, 26699,     0,     0,
 26699, 26699, 26699, 26699, 26699, 26699, 26699, 26699, 26699,     0,
     0,     0,     0,     0,     0,     0,     0, 28619, 22576,     0,
 28862,     0,   564,     0, 26699,   599,     0,     0, 30030,   599,
   599,   599, 24218, 28524,   883,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0, 26922,
   527,   892,   403,     0,     0,     0, 24331,   587,   140,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   109,
     0,     0,     0, 24331, 26922, 24331,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   654,   578,
     0,   694,     0,     0,   214,     0,   928,   140,     0,     0,
   518,     0,     0,     0,   788,   917,   933, 26699, 28940, 22576,
 28979, 25070,     0,     0,   599, 25539,     0,   599,   599,   143,
     0,   955,     0, 26699,   973,     0,   143,   995,     0,   143,
    69,     0,     0,     0,     0,     0, 27697,     0, 26699,   873,
   939, 26699, 28940, 28979,   599,  1334,    63,   143,     0,     0,
 24610,     0,   143,     0, 25658,     0,     0,     0, 25785,     0,
     0,     0,   800,     0,     0,     0,   999,     0, 29018, 22576,
 29057, 30721,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,  1052,   292,  1066,   491,     0,     0,     0,
     0,     0,     0,     0,     0,  1150,     0,     0,     0,     0,
     0,     0,   143,  1009,     0,  1034,     0,  1050,     0,  1057,
     0,     0,     0,     0, 26699,     0,     0,  1071,     0,   804,
   820,   -94,   853,   875, 26810,   214,   853, 26810,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   524,  7857,
  7857,  7857,  7857, 13190, 11105,  7857,  7857, 18582, 18582,   285,
   285,     0, 22470,  2236,  2236,   416,   164,   164,   524,   524,
   524,  1810,   853,     0,  1020,     0,   853,   799,     0,   803,
     0,   168,     0,     0,  1115,   143,   825,     0,   835,     0,
   168,     0,     0,     0,  1810,     0,     0, 26810,     0,     0,
   168, 26810, 25904, 25904,   143, 30721,  1145,     0,   587,     0,
     0,     0,     0,     0, 29117, 22576, 29437, 24331,   853,     0,
 24331,   931, 26922,     0,     0,     0,   853,   154,   853,     0,
 29476, 22576, 29515,     0,   943,   965, 24331, 27697, 26699, 26699,
 26699,     0,   864,   900,   143,   908,   929, 26699,   401,     0,
   731, 26699,     0, 26699, 26699,     0,     0, 25204,     0, 25539,
     0,     0,     0, 26922, 29372,     0,     0,   524,   168,   168,
 26699,     0,     0,     0,     0,   287, 30721,     0,     0,   143,
     0,     0,   999,  4028,     0,  1190,     0,     0,    19,  1255,
     0,     0,    52,  1267,     0,  1150,  1082,     0,  1263,   143,
  1264,     0,     0,  6860,     0,  6860,     0,   145,     0,  4393,
     0,     0, 26699,  1249,    14,     0,     0,     0,     0,     0,
     0,     0,     0,   477,     0, 26016, 23641,  1015,     0,     0,
 30065,  1018, 27411, 27411,  1266,     0,     0,     0,     0,     0,
   599,   599,     0,   564, 25070,   974,  1242,   599,   599,     0,
     0,   564,     0,  1219, 30106,  1040,   608,     0,     0,     0,
   220,  1284,   -36,   240,     0,     0, 26699, 30106,     0,  1307,
     0,     0,     0,     0,     0,     0,  1054,     0,   999, 30721,
   214, 26922, 24331,     0,     0,     0,   143,   853,     0,   918,
     0,    69, 28432,     0,     0,     0,     0,     0,     0,     0,
   143,     0,     0, 24331,     0,   853,   965,     0,   853, 26127,
  1085,     0,     0,   599,   599,  1008,   599,   599,     0,     0,
  1316,   143,    69,     0,     0,     0,     0,  6860,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   143,     0,  1150,     0,  1375,     0,     0,  1317,  1319,
  1322,     0,  1327,     0,     0,  1071,  1069,  1322,     0,     0,
     0,     0,     0,     0,     0,     0, 24331,     0,  1029,     0,
     0, 26699, 26699, 26699, 26699,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  4441,  4441,   588,     0, 22138,
   143,  1076,     0,  1400,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,    68,     0,  1265,     0,     0,     0,
     0,   828,     0,     0,     0,   102,     0,  1329,     0,  1353,
     0,     0,     0, 27886,    18,     0, 26699,  1281,  1281, 26810,
 26810,     0,   599,     0, 26810, 26810,     0,     0, 26699, 24840,
 29554, 22576, 29593,   599,     0,     0,     0, 26239, 24840,   999,
 24331,   214,   853,     0,     0,     0,   853,     0,     0, 24331,
 26922,     0,     0,     0,   853,     0,     0,   853,     0, 26699,
     0,   289,   853, 26699, 26699,   599, 26699, 26699, 25539,     0,
     0,   143,  -161,     0,     0,     0,  1362,  1365,  6860,     0,
  4393,     0,  4393,     0,  4393,     0,     0,     0, 24331,     0,
 30832,   140,     0, 29372, 29372, 29372, 29372,     0,     0,     0,
     0, 27886,  1077,   143,   143, 21727,     0,  1374,     0,     0,
  1314,   958,     0,   807,     0,     0,     0,     0, 26922,     0,
  1135, 30166, 27886,  4441,  4441,   588,   143,   143, 27411, 27411,
   958, 27886,  1077,     0,     0, 29372,  1680, 24331,     0,     0,
 24331,     0,     0,     0,     0, 26810,     0,     0,     0,     0,
 29372,     0,   799,   803,   143,   825,   835, 26810, 26699,     0,
     0,   853,     0,     0,     0,     0,   140,     0, 27411,     0,
     0, 26357, 24331,     0, 26699,  1399,  1385, 24331, 24331,     0,
 24331,  1375,  1375,  1322,  1404,  1322,  1322,  1186,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  1154,   635,     0,     0, 24331,    36,     0,     0,   110,  1077,
  1418,     0,     0,     0,   143,     0,  1425,     0,     0,  1426,
     0,     0,     0,     0,     0,     0,   143,   143,   143,   143,
   143,   143,     0,     0,  1427,     0,     0,  1434,     0,  1443,
   143,  1445,     0,  1349,  1447,     0, 30943,     0,  1071,     0,
     0,     0,     0,   974,     0,     0, 24331,   140,   867, 26699,
     0,  -131,     0,  1429,   853,  1370,  1128,  1365,     0,  4393,
     0,     0,     0,     0,     0, 29632, 22576, 29952,   875,     0,
  1460, 27886,     0,   760,     0,     0, 27886,     0,     0,     0,
     0,     0,     0, 30166,  7353,     0,  7353,     0,     0,  1382,
   145,     0,     0,  2544,     0,     0,     0,  1210,   682, 30943,
     0,   918,     0, 26922, 26922,     0,     0,     0,     0,     0,
   528,     0,     0,     0,     0,     0,  1322,     0,     0,   143,
     0,     0,     0, 27886,  1477,  1477,     0,     0,     0,     0,
  1482,  1492,  1493,  1494,     0,     0,  1071,  1482,     0, 29991,
   682,     0, 24331,     0,     0,  1429,     0,     0,     0,  1477,
 27886,  2544,     0,  2544,     0,  7353,     0,  2544,     0,     0,
     0,     0,     0,   357,     0,  1482,  1482,  1507,  1482,     0,
     0,     0,     0,     0,  2544,     0,     0,  1482,     0,
    }, yyRindex = {
//yyRindex 1359
     0,     0,   580,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0, 17424, 17519,     0,     0,     0,
 14595, 14118, 18113, 18426, 18516, 18608, 27039,     0, 26470,     0,
     0, 18921, 19011, 19103, 14707,  5214, 19416, 19506, 15072, 19598,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   811,   805,  1462,  1431,   199,     0,  1107,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  8608,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,  2090,  2090,     0,   126,     0,   516,
  3599, 13964,  8706, 13093,     0,  9020,     0, 25316,  3755,     0,
     0,     0, 21347,     0, 19911,     0,     0,     0,     0,   218,
     0,     0,     0,     0, 17617,     0,     0,     0,     0,     0,
     0,     0,     0,  1289,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  6894,     0,     0,     0,     0,  6171,  6485,  6580,  6678,
     0,  6992,  7087,  7185,  4132,  7499,  7594,  4250,  7692,  1716,
     0,   811,     0,     0,  3951,     0,     0,  2090,  2090, 13661,
     0,  3263,     0,  9837,     0,     0,     0,  9837,     0,  8199,
     0,     0,  1520,     0,     0,   922,     0,     0,  1520,     0,
     0,     0,     0, 27151,   618,     0,   618,  9213,     0,     0,
  9115,  8006,     0,     0,     0,     0,  9718, 13028, 13141, 20001,
     0,     0,   811,     0,  1755,     0,   824,     0,   594,  1520,
   936,  1520,  1471,     0,  1471,     0,     0,     0,  1444,     0,
   915,  2674,  2773,  2919,  1528,  3001,  3099,  3530,  1504,  3536,
  3762,  1579,  3859,     0,     0,     0,  6256,  6256,     0,     0,
  3987,   603,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   941,   843,     0, 23132,     0,   275,   843,     0,     0,     0,
     0,   180,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  9527,  9622,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   177,     0,
     0,     0, 10483,     0,     0, 27262,     0,     0,     0, 27262,
 26587, 26587,   811,   633,   649,     0,     0,     0,     0,     0,
     0,     0,     0,     0, 21867,     0,     0, 21979,     0,     0,
     0, 23244,     0,     0,     0,     0,   843,  3464,     0,  2576,
  2717,  2926,  6243, 10799, 12078, 12637, 13307, 17166,     0,     0,
     0,     0,     0,    99,     0,    99,   426,  1189,  1195,  1483,
  1566,  1636,  1760,   951,  1823,  2342,  1079,  2398,     0,     0,
  2461,     0,     0,     0,    44,     0,   458,     0,     0,     0,
  8513,     0,     0,     0,     0,     0,     0,     0,     0,   177,
     0,     0,     0,     0, 27262,     0,     0, 27262, 27262,  1520,
     0,     0,     0,   961,   964,     0,  1520,   510,     0,  1520,
  1520,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0, 27262,  1309,     0,  1520,     0,     0,
  3832,   124,  1520,     0,  1490,     0,     0,     0,   643,     0,
     0,     0,     0,     0,  4107,     0,   652,     0,     0,   177,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  1520,   190,     0,   190,     0,   205,     0,   190,
     0,     0,     0,     0,    75,    54,     0,   205,     0,   137,
    55,    66,     0,   994,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0, 10032, 12073,
 12193, 12333, 12453, 12514, 12892, 12632, 12779, 13181, 13277, 11130,
 11226,     0,  1512, 11622, 11718, 11527, 10728, 11035, 10127, 10223,
 10537, 11882,     0,     0,     0,     0,     0, 15184,  5579, 16615,
     0, 25316,     0,  5696,   211,  1497, 15549,     0, 15661,     0,
 14230,     0,     0,     0, 11978,     0,     0,     0,     0,     0,
 17092,     0,     0,     0,  1520,     0,   704,     0,   656,     0,
 22733,     0,     0,     0,     0,   177,     0,   843,     0,     0,
   871, 22845,     0,     0,     0,     0,     0,     0,     0,  2503,
     0,   177,     0,     0,  1304,     0,   744,     0,     0,     0,
     0,     0,  4615,  6061,  1497,  4732,  5097,     0,  9429,     0,
  9837,     0,     0,     0,     0,     0,     0,  1024,     0,   523,
     0,     0,     0,     0, 11346,     0,     0, 10632,     0,  8101,
     0,     0,  1123,     0,     0,  1471,     0,  2219,  1288,  1497,
  2425,  2670,   721,   -88,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   897,     0,  1191,  1520,
  1199,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,  1333,     0,   146, 13730,  8316,     0,     0,
 13773,  8823,     0,     0,     0,     0,     0,     0,     0,     0,
 27262, 27262,     0, 10977,   257, 17931,     0, 27262, 27262,     0,
     0, 14049,     0,     0, 13842,  9331,     0,     0,     0,     0,
 20093,     0, 10342, 21403,     0,     0,     0, 21620,     0,     0,
     0,     0,     0,     0,  6796,     0,  9978,     0,   746,     0,
     0,     0,   843, 22311, 22423,     0,  1497,     0,     0,  1333,
     0,  1520,     0,     0,     0,     0,     0,     0,  1556,   745,
  1497,  3162,  4225,    99,     0,     0,     0,     0,     0,     0,
  1333,     0,     0, 27262, 27262,  3645, 27262, 27262,     0,     0,
   510,  1520,  1520,     0,     0,     0,  1793,   907,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  1520,     0,     0,     0,     0,     0,     0,   190,   190,
   190,     0,   190,     0,     0,   205,    66,   190,     0,     0,
     0,     0,     0,     0,     0,     0,    99,   113,   244,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  1497,
   790, 20525,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0, 21442,     0, 20464,     0,     0,     0,
     0, 20191,     0,     0,     0, 20281,     0, 20774,     0, 20824,
     0,     0,     0, 20583, 20886,     0,     0, 23453, 23565,     0,
     0,     0, 27262,     0,     0,     0,     0,     0,     0,     0,
     0,   177,     0, 27262,     0,     0,  3399,     0,     0,   794,
   843,     0,     0,     0,     0,     0,     0,     0,     0,    99,
     0,     0,     0,  1239,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0, 27262,     0,     0,   776,     0,
     0,   487,     0,     0,     0,     0,  1201,  1213,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   783,     0,
     0,     0,     0, 13430,  7302, 13470,  7809,     0,     0,     0,
     0,     0, 20645,  1497,  1497, 20953,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 21496,     0, 20342, 20404,     0, 29889, 22931,     0,     0,
 21008,     0, 20693,     0,     0, 13566,   115,   843,     0,     0,
   618,     0,     0,     0,     0,     0,     0,     0,     0,     0,
 13611,     0, 16026, 16980,  1497, 16138, 16503,     0,     0,  9882,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   780,     0,     0,     0,   433,   843,   618,     0,
    47,     0,     0,   190,   190,   190,   190,  1333,   754,   851,
  1134,  1533,  1700,  1815,  1912,  2208,  2116,  2363,  3039,  2514,
     0,     0,  2517,     0,   843,  1520,     0,     0, 21048, 20736,
 21102,     0,     0,     0,  1461,     0,     0,     0,     0, 21533,
     0,     0,     0,     0,     0,     0,  1520,  1520,  1520,  1497,
  1497,  1497,     0,     0, 21159,     0,     0,   743,     0,   743,
   115,   766,     0,     0,   743,     0,   117,   906,   766,     0,
     0,     0,     0, 18021, 10387,     0,   871,     0,   288,     0,
     0,  1333,     0,     0,     0,     0,     0,  1214,     0,     0,
     0,     0,     0,     0,  2579,     0,   177,     0,   994,     0,
 21197,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   821,     0,     0,     0,
     0,     0,     0,     0,     0,  1296,  2049,     0,   899,     0,
     0,  1333,     0,     0,     0,     0,     0,     0,     0,     0,
   435,     0,     0,     0,     0,     0,   190,  3041,   555,  1497,
  3170,  3284,     0,     0, 21255, 21578,     0,     0,     0,     0,
   743,   743,   743,   743,     0,     0,   766,   743,     0,     0,
   960,     0,   284,     0,     0,     0,     0,     0,   768, 21308,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  2256,   123,  1333,     0,   743,   743,   743,   743,     0,
     0,     0,     0,     0,     0,     0,     0,   743,     0,
    }, yyGindex = {
//yyGindex 268
     0,     0,  1019,     0,  1523,  1152,  1535,   -59,     0,     0,
  -273,  1548,  1730,     0,  1805,  1898,     0,     0,     0,  1043,
  2266,     0,    33,     0,     0,    28,  1487,   747,  1551,  2076,
  1355,     0,    42,  1091,  -323,   365,     0,  1102,     4,   -32,
  3126,     2,   -12,   -55,     0,  -109,  -112,   919,    21,   375,
     0,   330,  -839,  -823,     0,     0,   391,     0,     0,   488,
    45,   718,  -350,   120,   983,  -313,   298,   538,  2927,    35,
     0,  -205,  -377,  1530,  2321,  -594,   905,  -459,  -558,     0,
     0,     0,     0,   384, -1115,   259,   217,   969,  -310,   509,
  -743,   888,  -831,  -777,   910,   759,     0,    34,  -451,     0,
  1211,     0,     0,     0,   675,     0,  -692,     0,   901,     0,
   399,     0,  -915,   347,  2322,     0,     0,  1023,  1291,   -87,
   181,   865,  2112,    -2,     3,  1567,     0,    48,   -98,   -24,
  -420,  -152,   335,     0,     0,  -724,   128,     0,     0,   600,
  -798,   191,     0,  -699,  -477,   194,     0,  -775,   605,     0,
     0,     0,  -162,     0,   596,     0,     0,   152,     0,  -393,
    12,   -57,  -695,   382,  -569,  -499, -1029,  -738,  2732,  -309,
   -86,     0,     0,  1600,     0, -1010,     0,     0,   607,     0,
     0,  3016,  -203,     0,     0,   893,     0,     0,  -194,     7,
   229,     0,  1067,     0,     0,   916,     0,     0,     0,     0,
     0,     0,     0,     0,   519,     0,   806,     0,     0,     0,
     0,     0,     0,     0,     0,   -26,   101,     0,     0,     0,
   147,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  -244,     0,     0,     0,     0,     0,  -439,     0,     0,     0,
   -82,     0,     0,   495,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,
    };
    protected static final short[] yyTable = YyTables.yyTable();
    protected static final short[] yyCheck = YyTables.yyCheck();

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
    null,null,null,null,"`class''","`module'","`def'",
"`undef'","`begin'","`rescue'","`ensure'",
"`end'","`if'","`unless'","`then'",
"`elsif'","`else'","`case'","`when'",
"`while'","`until'","`for'","`break'",
"`next'","`redo'","`retry'","`in'",
"`do'","`do' for condition","`do' for block","`do' for lambda",
"`return'","`yield'","`super'","`self'",
"`nil'","`true'","`false'","`and'",
"`or'","`not'","`if' modifier","`unless' modifier",
"`while' modifier","`until' modifier","`rescue' modifier","`alias'",
"`defined'","`BEGIN'","`END'","`__LINE__'",
"`__FILE__'","`__ENCODING__'","local variable or method","method","global variable",
"instance variable","constant","class variable","label","integer literal","float literal","rational literal",
"imaginary literal","char literal","numbered reference","back reference","literal content",
    "tREGEXP_END","tUMINUS_NUM","escaped space","unary+","unary-","**","<=>",
"==","===","!=",">=","<=","&&","||","=~","!~",
"..","...","(..","(...","[]","[]=","<<",">>",
"&.","::",":: at EXPR_BEG","operator assignment","=>","'('",
"( arg","'['","'{'","{ arg","'*'","**arg",
"'&'","->","symbol literal","string literal","backtick literal",
"regexp literal","word list","verbatim work list","terminator","symbol list",
"verbatim symbol list","'}'","tSTRING_DBEG","tSTRING_DVAR",
    "tLAMBEG","tLABEL_END","tIGNORED_NL","tCOMMENT","tEMBDOC_BEG",
    "tEMBDOC","tEMBDOC_END","tHEREDOC_BEG","tHEREDOC_END","tLOWEST",
    };

  /** printable rules for debugging.
    */
  protected static final String [] yyRule = {
    "$accept : program",
    "$$1 :",
    "program : $$1 top_compstmt",
    "top_compstmt : top_stmts opt_terms",
    "top_stmts : none",
    "top_stmts : top_stmt",
    "top_stmts : top_stmts terms top_stmt",
    "top_stmts : error top_stmt",
    "top_stmt : stmt",
    "top_stmt : keyword_BEGIN begin_block",
    "begin_block : '{' top_compstmt '}'",
    "$$2 :",
    "bodystmt : compstmt opt_rescue k_else $$2 compstmt opt_ensure",
    "bodystmt : compstmt opt_rescue opt_ensure",
    "compstmt : stmts opt_terms",
    "stmts : none",
    "stmts : stmt_or_begin",
    "stmts : stmts terms stmt_or_begin",
    "stmts : error stmt",
    "stmt_or_begin : stmt",
    "$$3 :",
    "stmt_or_begin : keyword_BEGIN $$3 begin_block",
    "$$4 :",
    "stmt : keyword_alias fitem $$4 fitem",
    "stmt : keyword_alias tGVAR tGVAR",
    "stmt : keyword_alias tGVAR tBACK_REF",
    "stmt : keyword_alias tGVAR tNTH_REF",
    "stmt : keyword_undef undef_list",
    "stmt : stmt modifier_if expr_value",
    "stmt : stmt modifier_unless expr_value",
    "stmt : stmt modifier_while expr_value",
    "stmt : stmt modifier_until expr_value",
    "stmt : stmt modifier_rescue stmt",
    "stmt : keyword_END '{' compstmt '}'",
    "stmt : command_asgn",
    "stmt : mlhs '=' lex_ctxt command_call",
    "stmt : lhs '=' lex_ctxt mrhs",
    "stmt : mlhs '=' lex_ctxt mrhs_arg modifier_rescue stmt",
    "stmt : mlhs '=' lex_ctxt mrhs_arg",
    "stmt : expr",
    "command_asgn : lhs '=' lex_ctxt command_rhs",
    "command_asgn : var_lhs tOP_ASGN lex_ctxt command_rhs",
    "command_asgn : primary_value '[' opt_call_args rbracket tOP_ASGN lex_ctxt command_rhs",
    "command_asgn : primary_value call_op tIDENTIFIER tOP_ASGN lex_ctxt command_rhs",
    "command_asgn : primary_value call_op tCONSTANT tOP_ASGN lex_ctxt command_rhs",
    "command_asgn : primary_value tCOLON2 tCONSTANT tOP_ASGN lex_ctxt command_rhs",
    "command_asgn : primary_value tCOLON2 tIDENTIFIER tOP_ASGN lex_ctxt command_rhs",
    "command_asgn : defn_head f_opt_paren_args '=' command",
    "command_asgn : defn_head f_opt_paren_args '=' command modifier_rescue arg",
    "command_asgn : defs_head f_opt_paren_args '=' command",
    "command_asgn : defs_head f_opt_paren_args '=' command modifier_rescue arg",
    "command_asgn : backref tOP_ASGN lex_ctxt command_rhs",
    "command_rhs : command_call",
    "command_rhs : command_call modifier_rescue stmt",
    "command_rhs : command_asgn",
    "expr : command_call",
    "expr : expr keyword_and expr",
    "expr : expr keyword_or expr",
    "expr : keyword_not opt_nl expr",
    "expr : '!' command_call",
    "$$5 :",
    "$$6 :",
    "expr : arg tASSOC $$5 $$6 p_top_expr_body",
    "$$7 :",
    "$$8 :",
    "expr : arg keyword_in $$7 $$8 p_top_expr_body",
    "expr : arg",
    "def_name : fname",
    "defn_head : k_def def_name",
    "$$9 :",
    "defs_head : k_def singleton dot_or_colon $$9 def_name",
    "expr_value : expr",
    "$$10 :",
    "expr_value_do : $$10 expr_value do",
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
    "command : keyword_super command_args",
    "command : keyword_yield command_args",
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
    "$$11 :",
    "undef_list : undef_list ',' $$11 fitem",
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
    "$$12 :",
    "arg : keyword_defined opt_nl $$12 arg",
    "arg : arg '?' arg opt_nl ':' arg",
    "arg : defn_head f_opt_paren_args '=' arg",
    "arg : defn_head f_opt_paren_args '=' arg modifier_rescue arg",
    "arg : defs_head f_opt_paren_args '=' arg",
    "arg : defs_head f_opt_paren_args '=' arg modifier_rescue arg",
    "arg : primary",
    "relop : '>'",
    "relop : '<'",
    "relop : tGEQ",
    "relop : tLEQ",
    "rel_expr : arg relop arg",
    "rel_expr : rel_expr relop arg",
    "lex_ctxt : tSP",
    "lex_ctxt : none",
    "arg_value : arg",
    "aref_args : none",
    "aref_args : args trailer",
    "aref_args : args ',' assocs trailer",
    "aref_args : assocs trailer",
    "arg_rhs : arg",
    "arg_rhs : arg modifier_rescue arg",
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
    "args : tSTAR arg_value",
    "args : args ',' arg_value",
    "args : args ',' tSTAR arg_value",
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
    "primary : tLPAREN_ARG $$15 rparen",
    "$$16 :",
    "primary : tLPAREN_ARG stmt $$16 rparen",
    "primary : tLPAREN compstmt ')'",
    "primary : primary_value tCOLON2 tCONSTANT",
    "primary : tCOLON3 tCONSTANT",
    "primary : tLBRACK aref_args ']'",
    "primary : tLBRACE assoc_list '}'",
    "primary : k_return",
    "primary : keyword_yield '(' call_args rparen",
    "primary : keyword_yield '(' rparen",
    "primary : keyword_yield",
    "$$17 :",
    "primary : keyword_defined opt_nl '(' $$17 expr rparen",
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
    "$$18 :",
    "primary : k_case expr_value opt_terms $$18 case_body k_end",
    "$$19 :",
    "primary : k_case opt_terms $$19 case_body k_end",
    "primary : k_case expr_value opt_terms p_case_body k_end",
    "primary : k_for for_var keyword_in expr_value_do compstmt k_end",
    "$$20 :",
    "primary : k_class cpath superclass $$20 bodystmt k_end",
    "$$21 :",
    "primary : k_class tLSHFT expr $$21 term bodystmt k_end",
    "$$22 :",
    "primary : k_module cpath $$22 bodystmt k_end",
    "primary : defn_head f_arglist bodystmt k_end",
    "primary : defs_head f_arglist bodystmt k_end",
    "primary : keyword_break",
    "primary : keyword_next",
    "primary : keyword_redo",
    "primary : keyword_retry",
    "primary_value : primary",
    "k_begin : keyword_begin",
    "k_if : keyword_if",
    "k_unless : keyword_unless",
    "k_while : keyword_while",
    "k_until : keyword_until",
    "k_case : keyword_case",
    "k_for : keyword_for",
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
    "k_return : keyword_return",
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
    "$$24 :",
    "$$25 :",
    "$$26 :",
    "$$27 :",
    "lambda : tLAMBDA $$24 $$25 $$26 f_larglist $$27 lambda_body",
    "f_larglist : '(' f_args opt_bv_decl ')'",
    "f_larglist : f_args",
    "lambda_body : tLAMBEG compstmt '}'",
    "lambda_body : keyword_do_LAMBDA bodystmt k_end",
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
    "$$28 :",
    "$$29 :",
    "$$30 :",
    "brace_body : $$28 $$29 $$30 opt_block_param compstmt",
    "$$31 :",
    "$$32 :",
    "$$33 :",
    "do_body : $$31 $$32 $$33 opt_block_param bodystmt",
    "case_args : arg_value",
    "case_args : tSTAR arg_value",
    "case_args : case_args ',' arg_value",
    "case_args : case_args ',' tSTAR arg_value",
    "case_body : k_when case_args then compstmt cases",
    "cases : opt_else",
    "cases : case_body",
    "$$34 :",
    "$$35 :",
    "$$36 :",
    "p_case_body : keyword_in $$34 $$35 p_top_expr then $$36 compstmt p_cases",
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
    "p_lparen : '('",
    "p_lbracket : '['",
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
    "$$37 :",
    "p_expr_basic : tLBRACE $$37 p_kwargs rbrace",
    "p_expr_basic : tLBRACE rbrace",
    "$$38 :",
    "p_expr_basic : tLPAREN $$38 p_expr rparen",
    "p_args : p_expr",
    "p_args : p_args_head",
    "p_args : p_args_head p_arg",
    "p_args : p_args_head tSTAR tIDENTIFIER",
    "p_args : p_args_head tSTAR tIDENTIFIER ',' p_args_post",
    "p_args : p_args_head tSTAR",
    "p_args : p_args_head tSTAR ',' p_args_post",
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
    "p_expr_ref : '^' tLPAREN expr_value ')'",
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
    "words : tWORDS_BEG ' ' word_list tSTRING_END",
    "word_list :",
    "word_list : word_list word ' '",
    "word : string_content",
    "word : word string_content",
    "symbols : tSYMBOLS_BEG ' ' symbol_list tSTRING_END",
    "symbol_list :",
    "symbol_list : symbol_list word ' '",
    "qwords : tQWORDS_BEG ' ' qword_list tSTRING_END",
    "qsymbols : tQSYMBOLS_BEG ' ' qsym_list tSTRING_END",
    "qword_list :",
    "qword_list : qword_list tSTRING_CONTENT ' '",
    "qsym_list :",
    "qsym_list : qsym_list tSTRING_CONTENT ' '",
    "string_contents :",
    "string_contents : string_contents string_content",
    "xstring_contents :",
    "xstring_contents : xstring_contents string_content",
    "regexp_contents :",
    "regexp_contents : regexp_contents string_content",
    "string_content : tSTRING_CONTENT",
    "$$39 :",
    "string_content : tSTRING_DVAR $$39 string_dvar",
    "$$40 :",
    "$$41 :",
    "$$42 :",
    "$$43 :",
    "string_content : tSTRING_DBEG $$40 $$41 $$42 $$43 compstmt tSTRING_DEND",
    "string_dvar : tGVAR",
    "string_dvar : tIVAR",
    "string_dvar : tCVAR",
    "string_dvar : backref",
    "symbol : ssym",
    "symbol : dsym",
    "ssym : tSYMBEG sym",
    "sym : fname",
    "sym : tIVAR",
    "sym : tGVAR",
    "sym : tCVAR",
    "dsym : tSYMBEG string_contents tSTRING_END",
    "numeric : simple_numeric",
    "numeric : tUMINUS_NUM simple_numeric",
    "nonlocal_var : tIVAR",
    "nonlocal_var : tGVAR",
    "nonlocal_var : tCVAR",
    "simple_numeric : tINTEGER",
    "simple_numeric : tFLOAT",
    "simple_numeric : tRATIONAL",
    "simple_numeric : tIMAGINARY",
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
    "$$44 :",
    "superclass : '<' $$44 expr_value term",
    "superclass :",
    "f_opt_paren_args : f_paren_args",
    "f_opt_paren_args : none",
    "f_paren_args : '(' f_args rparen",
    "f_arglist : f_paren_args",
    "$$45 :",
    "f_arglist : $$45 f_args term",
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
    "f_no_kwarg : kwrest_mark keyword_nil",
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
    "$$46 :",
    "singleton : '(' $$46 expr rparen",
    "assoc_list : none",
    "assoc_list : assocs trailer",
    "assocs : assoc",
    "assocs : assocs ',' assoc",
    "assoc : arg_value tASSOC arg_value",
    "assoc : tLABEL arg_value",
    "assoc : tLABEL",
    "assoc : tSTRING_BEG string_contents tLABEL_END arg_value",
    "assoc : tDSTAR arg_value",
    "operation : tIDENTIFIER",
    "operation : tCONSTANT",
    "operation : tFID",
    "operation2 : tIDENTIFIER",
    "operation2 : tCONSTANT",
    "operation2 : tFID",
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
      @param token single character or <tt>%token</tt> value.
      @return token name or <tt>[illegal]</tt> or <tt>[unknown]</tt>.
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
      @param ayydebug debug message writer implementing <tt>yyDebug</tt>, or <tt>null</tt>.
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

        ParserState parserState = states[yyn];
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

static ParserState<RipperParser>[] states = new ParserState[818];
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
                  yyVal = p.remove_begin(((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[9] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  yyVal = null;
  return yyVal;
};
states[10] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_BEGIN", v1);
                    yyVal = v2;}
  return yyVal;
};
states[11] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   if (((IRubyObject)yyVals[-1+yyTop].value) == null) p.yyerror("else without rescue is useless"); 
  return yyVal;
};
states[12] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = p.escape(((IRubyObject)yyVals[-5+yyTop].value));
                    v2 = p.escape(((IRubyObject)yyVals[-4+yyTop].value));
                    v3 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v4 = p.escape(((IRubyObject)yyVals[0+yyTop].value));
                    v5 = p.dispatch("on_bodystmt", v1, v2, v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[13] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = p.escape(((IRubyObject)yyVals[-2+yyTop].value));
                    v2 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v3 = null;
                    v4 = p.escape(((IRubyObject)yyVals[0+yyTop].value));
                    v5 = p.dispatch("on_bodystmt", v1, v2, v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[14] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.void_stmts(((IRubyObject)yyVals[-1+yyTop].value));
  return yyVal;
};
states[15] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = p.dispatch("on_stmts_new");
                    v2 = p.dispatch("on_void_stmt");
                    v3 = v1;
                    v4 = v2;
                    v5 = p.dispatch("on_stmts_add", v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[16] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_stmts_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_stmts_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[17] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_stmts_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[18] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[19] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[20] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   p.yyerror("BEGIN is permitted only at toplevel");
  return yyVal;
};
states[21] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[22] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_FNAME|EXPR_FITEM);
  return yyVal;
};
states[23] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_alias", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[24] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_var_alias", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[25] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_var_alias", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[26] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "can't make alias for the number variables";
{IRubyObject v1, v2, v3;
                    v1 = p.intern(message);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_alias_error", v1, v2);
                    yyVal = v3;
                    p.error();}
  return yyVal;
};
states[27] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((RubyArray)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_undef", v1);
                    yyVal = v2;}
  return yyVal;
};
states[28] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = p.dispatch("on_if_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[29] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = p.dispatch("on_unless_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[30] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = p.dispatch("on_while_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[31] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = p.dispatch("on_until_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[32] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_rescue_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[33] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   if (p.getLexContext().in_def) {
                       p.warn("END in method; use at_exit");
                    }
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_END", v1);
                    yyVal = v2;}
  return yyVal;
};
states[35] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_massign", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[36] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_assign", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[37] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_rescue_mod", v1, v2);
                    v4 = ((IRubyObject)yyVals[-5+yyTop].value);
                    v5 = v3;
                    v6 = p.dispatch("on_massign", v4, v5);
                    yyVal = v6;}
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
states[40] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_assign", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[41] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_opassign", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[42] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[43] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[44] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[45] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[46] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[47] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((Holder)yyVals[-3+yyTop].value));
                    p.restore_defun(((Holder)yyVals[-3+yyTop].value));
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8, v9;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = null;
                    v3 = null;
                    v4 = null;
                    v5 = p.dispatch("on_bodystmt", v1, v2, v3, v4);
                    v6 = p.get_value(((Holder)yyVals[-3+yyTop].value));
                    v7 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v8 = v5;
                    v9 = p.dispatch("on_def", v6, v7, v8);
                    yyVal = v9;}
                    p.popCurrentScope();
  return yyVal;
};
states[48] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((Holder)yyVals[-5+yyTop].value));
                    p.restore_defun(((Holder)yyVals[-5+yyTop].value));
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_rescue_mod", v1, v2);
                    v4 = v3;
                    v5 = null;
                    v6 = null;
                    v7 = null;
                    v8 = p.dispatch("on_bodystmt", v4, v5, v6, v7);
                    v9 = p.get_value(((Holder)yyVals[-5+yyTop].value));
                    v10 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v11 = v8;
                    v12 = p.dispatch("on_def", v9, v10, v11);
                    yyVal = v12;}

                    p.popCurrentScope();
  return yyVal;
};
states[49] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((Holder)yyVals[-3+yyTop].value));
                    p.restore_defun(((Holder)yyVals[-3+yyTop].value));
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = null;
                    v3 = null;
                    v4 = null;
                    v5 = p.dispatch("on_bodystmt", v1, v2, v3, v4);
                    v6 = ((RubyArray) ((Holder)yyVals[-3+yyTop].value).value).eltOk(0);
                    v7 = ((RubyArray) ((Holder)yyVals[-3+yyTop].value).value).eltOk(1);
                    v8 = ((RubyArray) ((Holder)yyVals[-3+yyTop].value).value).eltOk(2);
                    v9 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v10 = v5;
                    v11 = p.dispatch("on_defs", v6, v7, v8, v9, v10);
                    yyVal = v11;}
                    p.popCurrentScope();
  return yyVal;
};
states[50] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((Holder)yyVals[-5+yyTop].value));
                    p.restore_defun(((Holder)yyVals[-5+yyTop].value));
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_rescue_mod", v1, v2);
                    v4 = v3;
                    v5 = null;
                    v6 = null;
                    v7 = null;
                    v8 = p.dispatch("on_bodystmt", v4, v5, v6, v7);
                    v9 = ((RubyArray) ((Holder)yyVals[-5+yyTop].value).value).eltOk(0);
                    v10 = ((RubyArray) ((Holder)yyVals[-5+yyTop].value).value).eltOk(1);
                    v11 = ((RubyArray) ((Holder)yyVals[-5+yyTop].value).value).eltOk(2);
                    v12 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v13 = v8;
                    v14 = p.dispatch("on_defs", v9, v10, v11, v12, v13);
                    yyVal = v14;}
                    p.popCurrentScope();
  return yyVal;
};
states[51] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = p.var_field(((IRubyObject)yyVals[-3+yyTop].value));
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_assign", v1, v2);
                    yyVal = p.backref_error(((IRubyObject)yyVals[-3+yyTop].value), v3);
                    p.error();}
  return yyVal;
};
states[52] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((IRubyObject)yyVals[0+yyTop].value));
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[53] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_rescue_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[56] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.logop(((IRubyObject)yyVals[-2+yyTop].value), AMPERSAND_AMPERSAND, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[57] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.logop(((IRubyObject)yyVals[-2+yyTop].value), OR_OR, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[58] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((IRubyObject)yyVals[0+yyTop].value)), BANG);
  return yyVal;
};
states[59] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((IRubyObject)yyVals[0+yyTop].value)), BANG);
  return yyVal;
};
states[60] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((IRubyObject)yyVals[-1+yyTop].value));
                    p.setState(EXPR_BEG|EXPR_LABEL);
                    p.setCommandStart(false);
                    /* MRI 3.1 uses $2 but we want tASSOC typed?*/
                    LexContext ctxt = p.getLexContext();
                    yyVal = ctxt.in_kwarg;
                    ctxt.in_kwarg = true;
  return yyVal;
};
states[61] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.push_pvtbl();
  return yyVal;
};
states[62] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pvtbl(((Set)yyVals[-1+yyTop].value));
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_kwarg = ((Boolean)yyVals[-2+yyTop].value);
{IRubyObject v1, v2, v3, v4, v5, v6, v7;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = null;
                    v3 = null;
                    v4 = p.dispatch("on_in", v1, v2, v3);
                    v5 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v6 = v4;
                    v7 = p.dispatch("on_case", v5, v6);
                    yyVal = v7;}
  return yyVal;
};
states[63] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((IRubyObject)yyVals[-1+yyTop].value));
                    p.setState(EXPR_BEG|EXPR_LABEL);
                    p.setCommandStart(false);
                    LexContext ctxt = p.getLexContext();
                    yyVal = ctxt.in_kwarg;
                    ctxt.in_kwarg = true;
  return yyVal;
};
states[64] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.push_pvtbl();
  return yyVal;
};
states[65] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pvtbl(((Set)yyVals[-1+yyTop].value));
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_kwarg = ((Boolean)yyVals[-2+yyTop].value);
{IRubyObject v1, v2, v3, v4, v5, v6, v7;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = null;
                    v3 = null;
                    v4 = p.dispatch("on_in", v1, v2, v3);
                    v5 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v6 = v4;
                    v7 = p.dispatch("on_case", v5, v6);
                    yyVal = v7;}
  return yyVal;
};
states[67] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pushLocalScope();
                    ByteList currentArg = p.getCurrentArg();
                    p.setCurrentArg(null);
                    LexContext ctxt = p.getLexContext();
                    RubySymbol name = p.get_id(((IRubyObject)yyVals[0+yyTop].value));
                    p.numparam_name(name);
                        yyVal = new Holder(ctxt, name, p.get_value(((IRubyObject)yyVals[0+yyTop].value)));

                    ctxt.in_def = true;
                    p.setCurrentArg(null);
  return yyVal;
};
states[68] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Holder)yyVals[0+yyTop].value);
  return yyVal;
};
states[69] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_FNAME); 
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_argdef = true;
  return yyVal;
};
states[70] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_ENDFN|EXPR_LABEL);
                    yyVal = ((Holder)yyVals[0+yyTop].value);
                       ((Holder)yyVal).value = p.new_array(((IRubyObject)yyVals[-3+yyTop].value), ((IRubyObject)yyVals[-2+yyTop].value), ((Holder)yyVal).value);

  return yyVal;
};
states[71] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[72] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getConditionState().push1();
  return yyVal;
};
states[73] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getConditionState().pop();
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[77] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[78] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[79] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);}
  return yyVal;
};
states[80] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_command", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[81] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[82] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v4 = ((IRubyObject)yyVals[0+yyTop].value);
                    v5 = p.dispatch("on_command_call", v1, v2, v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[83] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[84] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = p.intern("::");
                    v3 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v4 = ((IRubyObject)yyVals[0+yyTop].value);
                    v5 = p.dispatch("on_command_call", v1, v2, v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[85] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[86] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_super", v1);
                    yyVal = v2;}
  return yyVal;
};
states[87] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_yield", v1);
                    yyVal = v2;}
  return yyVal;
};
states[88] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_return", v1);
                    yyVal = v2;}
  return yyVal;
};
states[89] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_break", v1);
                    yyVal = v2;}
  return yyVal;
};
states[90] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_next", v1);
                    yyVal = v2;}
  return yyVal;
};
states[92] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_mlhs_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[93] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[94] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_mlhs_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[95] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);}
  return yyVal;
};
states[96] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_mlhs_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[97] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_mlhs_add_star", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[98] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[99] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = null;
                    v3 = p.dispatch("on_mlhs_add_star", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[100] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = null;
                    v3 = p.dispatch("on_mlhs_add_star", v1, v2);
                    v4 = v3;
                    v5 = ((IRubyObject)yyVals[0+yyTop].value);
                    v6 = p.dispatch("on_mlhs_add_post", v4, v5);
                    yyVal = v6;}
  return yyVal;
};
states[101] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_mlhs_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_mlhs_add_star", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[102] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[103] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_mlhs_new");
                    v2 = v1;
                    v3 = null;
                    v4 = p.dispatch("on_mlhs_add_star", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[104] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7;
                    v1 = p.dispatch("on_mlhs_new");
                    v2 = v1;
                    v3 = null;
                    v4 = p.dispatch("on_mlhs_add_star", v2, v3);
                    v5 = v4;
                    v6 = ((IRubyObject)yyVals[0+yyTop].value);
                    v7 = p.dispatch("on_mlhs_add_post", v5, v6);
                    yyVal = v7;}
  return yyVal;
};
states[106] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_mlhs_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[107] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_mlhs_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v4 = p.dispatch("on_mlhs_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[108] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_mlhs_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[109] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_mlhs_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_mlhs_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[110] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_mlhs_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[111] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[112] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[113] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[114] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[115] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[116] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[117] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[118] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[119] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[120] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[121] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[122] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[123] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v3 = p.dispatch("on_aref_field", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[124] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[125] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_const_path_field", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[126] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[127] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_const_path_field", v1, v2);
                    yyVal = p.const_decl(v3);}
  return yyVal;
};
states[128] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_top_const_field", v1);
                    yyVal = p.const_decl(v2);}
  return yyVal;
};
states[129] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.backref_error(((IRubyObject)yyVals[0+yyTop].value), p.var_field(((IRubyObject)yyVals[0+yyTop].value)));
                    p.error();}
  return yyVal;
};
states[130] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[131] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[132] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[133] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[134] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[135] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[136] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[137] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[138] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[139] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[140] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[141] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[142] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v3 = p.dispatch("on_aref_field", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[143] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_field", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[144] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.intern("::");
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_field", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[145] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_field", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[146] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_const_path_field", v1, v2);
                    yyVal = p.const_decl(v3);}
  return yyVal;
};
states[147] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_top_const_field", v1);
                    yyVal = p.const_decl(v2);}
  return yyVal;
};
states[148] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.backref_error(((IRubyObject)yyVals[0+yyTop].value), p.var_field(((IRubyObject)yyVals[0+yyTop].value)));
                    p.error();}
  return yyVal;
};
states[149] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "class/module name must be CONSTANT";
{IRubyObject v1, v2, v3;
                    v1 = p.intern(message);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_class_name_error", v1, v2);
                    yyVal = v3;
                    p.error();}
  return yyVal;
};
states[150] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[151] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_top_const_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[152] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_const_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[153] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_const_path_ref", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[154] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[155] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[156] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[157] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   p.setState(EXPR_ENDFN);
                   yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[158] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[159] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_symbol_literal", v1);
                    yyVal = v2;}

  return yyVal;
};
states[160] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[161] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[162] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_FNAME|EXPR_FITEM);
  return yyVal;
};
states[163] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-3+yyTop].value).push(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[164] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.maybe_symbolize(OR);
  return yyVal;
};
states[165] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.maybe_symbolize(CARET);
  return yyVal;
};
states[166] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.maybe_symbolize(AMPERSAND);
  return yyVal;
};
states[167] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[168] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[169] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
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
                     yyVal = p.maybe_symbolize(GT);
  return yyVal;
};
states[173] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[174] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.maybe_symbolize(LT);
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
                     yyVal = p.maybe_symbolize(PLUS);
  return yyVal;
};
states[180] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.maybe_symbolize(MINUS);
  return yyVal;
};
states[181] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.maybe_symbolize(STAR);
  return yyVal;
};
states[182] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[183] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.maybe_symbolize(SLASH);
  return yyVal;
};
states[184] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.maybe_symbolize(PERCENT);
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
                     yyVal = p.maybe_symbolize(BANG);
  return yyVal;
};
states[188] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.maybe_symbolize(TILDE);
  return yyVal;
};
states[189] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
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
                     yyVal = p.maybe_symbolize(BACKTICK);
  return yyVal;
};
states[194] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.__LINE__.bytes);
  return yyVal;
};
states[195] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.__FILE__.bytes);
  return yyVal;
};
states[196] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.__ENCODING__.bytes);
  return yyVal;
};
states[197] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.LBEGIN.bytes);
  return yyVal;
};
states[198] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.LEND.bytes);
  return yyVal;
};
states[199] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.ALIAS.bytes);
  return yyVal;
};
states[200] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.AND.bytes);
  return yyVal;
};
states[201] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.BEGIN.bytes);
  return yyVal;
};
states[202] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.BREAK.bytes);
  return yyVal;
};
states[203] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.CASE.bytes);
  return yyVal;
};
states[204] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.CLASS.bytes);
  return yyVal;
};
states[205] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.DEF.bytes);
  return yyVal;
};
states[206] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.DEFINED_P.bytes);
  return yyVal;
};
states[207] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.DO.bytes);
  return yyVal;
};
states[208] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.ELSE.bytes);
  return yyVal;
};
states[209] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.ELSIF.bytes);
  return yyVal;
};
states[210] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.END.bytes);
  return yyVal;
};
states[211] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.ENSURE.bytes);
  return yyVal;
};
states[212] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.FALSE.bytes);
  return yyVal;
};
states[213] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.FOR.bytes);
  return yyVal;
};
states[214] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.IN.bytes);
  return yyVal;
};
states[215] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.MODULE.bytes);
  return yyVal;
};
states[216] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.NEXT.bytes);
  return yyVal;
};
states[217] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.NIL.bytes);
  return yyVal;
};
states[218] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.NOT.bytes);
  return yyVal;
};
states[219] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.OR.bytes);
  return yyVal;
};
states[220] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.REDO.bytes);
  return yyVal;
};
states[221] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.RESCUE.bytes);
  return yyVal;
};
states[222] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.RETRY.bytes);
  return yyVal;
};
states[223] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.RETURN.bytes);
  return yyVal;
};
states[224] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.SELF.bytes);
  return yyVal;
};
states[225] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.SUPER.bytes);
  return yyVal;
};
states[226] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.THEN.bytes);
  return yyVal;
};
states[227] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.TRUE.bytes);
  return yyVal;
};
states[228] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.UNDEF.bytes);
  return yyVal;
};
states[229] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.WHEN.bytes);
  return yyVal;
};
states[230] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.YIELD.bytes);
  return yyVal;
};
states[231] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.IF.bytes);
  return yyVal;
};
states[232] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.UNLESS.bytes);
  return yyVal;
};
states[233] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.WHILE.bytes);
  return yyVal;
};
states[234] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(Keyword.UNTIL.bytes);
  return yyVal;
};
states[235] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_assign", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[236] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_opassign", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[237] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[238] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[239] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[240] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[241] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[242] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[243] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.var_field(((IRubyObject)yyVals[-3+yyTop].value));
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_opassign", v1, v2, v3);
                    yyVal = p.backref_error(((IRubyObject)yyVals[-3+yyTop].value), v4);
                    p.error();}
  return yyVal;
};
states[244] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot2", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[245] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot3", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[246] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = null;
                    v3 = p.dispatch("on_dot2", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[247] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = null;
                    v3 = p.dispatch("on_dot3", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[248] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = null;
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot2", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[249] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = null;
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot3", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[250] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), PLUS, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[251] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), MINUS, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[252] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), STAR, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[253] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), SLASH, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[254] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), PERCENT, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[255] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), STAR_STAR, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[256] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), ((IRubyObject)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value), p.src_line()), ((ByteList)yyVals[-3+yyTop].value));
  return yyVal;
};
states[257] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(((IRubyObject)yyVals[0+yyTop].value), PLUS_AT);
  return yyVal;
};
states[258] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(((IRubyObject)yyVals[0+yyTop].value), MINUS_AT);
  return yyVal;
};
states[259] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), OR, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[260] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), CARET, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[261] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), AMPERSAND, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[262] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), LT_EQ_RT, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[263] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[264] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), EQ_EQ, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[265] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), EQ_EQ_EQ, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[266] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), BANG_EQ, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[267] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.match_op(((IRubyObject)yyVals[-2+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[268] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), BANG_TILDE, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[269] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((IRubyObject)yyVals[0+yyTop].value)), BANG);
  return yyVal;
};
states[270] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(((IRubyObject)yyVals[0+yyTop].value), TILDE);
  return yyVal;
};
states[271] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), LT_LT, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[272] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), GT_GT, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[273] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.logop(((IRubyObject)yyVals[-2+yyTop].value), AMPERSAND_AMPERSAND, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[274] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.logop(((IRubyObject)yyVals[-2+yyTop].value), OR_OR, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[275] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_defined = true;
  return yyVal;
};
states[276] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_defined = false;                    
                    yyVal = p.new_defined(yyVals[yyTop - count + 1].start(), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[277] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-5+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_ifop", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[278] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((Holder)yyVals[-3+yyTop].value));
                    p.restore_defun(((Holder)yyVals[-3+yyTop].value));
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8, v9;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = null;
                    v3 = null;
                    v4 = null;
                    v5 = p.dispatch("on_bodystmt", v1, v2, v3, v4);
                    v6 = p.get_value(((Holder)yyVals[-3+yyTop].value));
                    v7 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v8 = v5;
                    v9 = p.dispatch("on_def", v6, v7, v8);
                    yyVal = v9;}
  return yyVal;
};
states[279] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((Holder)yyVals[-5+yyTop].value));
                    p.restore_defun(((Holder)yyVals[-5+yyTop].value));
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_rescue_mod", v1, v2);
                    v4 = v3;
                    v5 = null;
                    v6 = null;
                    v7 = null;
                    v8 = p.dispatch("on_bodystmt", v4, v5, v6, v7);
                    v9 = p.get_value(((Holder)yyVals[-5+yyTop].value));
                    v10 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v11 = v8;
                    v12 = p.dispatch("on_def", v9, v10, v11);
                    yyVal = v12;}
  return yyVal;
};
states[280] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((Holder)yyVals[-3+yyTop].value));
                    p.restore_defun(((Holder)yyVals[-3+yyTop].value));
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = null;
                    v3 = null;
                    v4 = null;
                    v5 = p.dispatch("on_bodystmt", v1, v2, v3, v4);
                    v6 = ((RubyArray) ((Holder)yyVals[-3+yyTop].value).value).eltOk(0);
                    v7 = ((RubyArray) ((Holder)yyVals[-3+yyTop].value).value).eltOk(1);
                    v8 = ((RubyArray) ((Holder)yyVals[-3+yyTop].value).value).eltOk(2);
                    v9 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v10 = v5;
                    v11 = p.dispatch("on_defs", v6, v7, v8, v9, v10);
                    yyVal = v11;}
  return yyVal;
};
states[281] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_rescue_mod", v1, v2);
                    v4 = v3;
                    v5 = null;
                    v6 = null;
                    v7 = null;
                    v8 = p.dispatch("on_bodystmt", v4, v5, v6, v7);
                    v9 = ((RubyArray) ((Holder)yyVals[-5+yyTop].value).value).eltOk(0);
                    v10 = ((RubyArray) ((Holder)yyVals[-5+yyTop].value).value).eltOk(1);
                    v11 = ((RubyArray) ((Holder)yyVals[-5+yyTop].value).value).eltOk(2);
                    v12 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v13 = v8;
                    v14 = p.dispatch("on_defs", v9, v10, v11, v12, v13);
                    yyVal = v14;}
  return yyVal;
};
states[282] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[283] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(GT);
  return yyVal;
};
states[284] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(LT);
  return yyVal;
};
states[285] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[286] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[287] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), ((IRubyObject)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[288] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.warning(p.src_line(), "comparison '" + ((IRubyObject)yyVals[-1+yyTop].value) + "' after comparison");
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), ((IRubyObject)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[289] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = (LexContext) p.getLexContext().clone();
  return yyVal;
};
states[290] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = (LexContext) p.getLexContext().clone();
  return yyVal;
};
states[291] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((IRubyObject)yyVals[0+yyTop].value));
                    yyVal = p.makeNullNil(((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[293] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[294] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((RubyArray)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_bare_assoc_hash", v1);
                    v3 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v4 = v2;
                    v5 = p.dispatch("on_args_add", v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[295] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[296] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((IRubyObject)yyVals[0+yyTop].value));
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[297] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_rescue_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[298] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v2 = p.dispatch("on_arg_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[299] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[300] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[305] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[306] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((RubyArray)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_bare_assoc_hash", v1);
                    v3 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v4 = v2;
                    v5 = p.dispatch("on_args_add", v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[307] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[308] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_args_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[309] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_args_add_block", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[310] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[311] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[312] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_args_add_block", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[313] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[314] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[315] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);}
  return yyVal;
};
states[316] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.nil();

  return yyVal;
};
states[317] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[319] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_args_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[320] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_args_add_star", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[321] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_args_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[322] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_args_add_star", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[323] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[324] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[325] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.dispatch("on_mrhs_new_from_args", v1);
                    v3 = v2;
                    v4 = ((IRubyObject)yyVals[0+yyTop].value);
                    v5 = p.dispatch("on_mrhs_add", v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[326] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = p.dispatch("on_mrhs_new_from_args", v1);
                    v3 = v2;
                    v4 = ((IRubyObject)yyVals[0+yyTop].value);
                    v5 = p.dispatch("on_mrhs_add_star", v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[327] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_mrhs_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_mrhs_add_star", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[332] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[333] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[334] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[335] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[338] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[339] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getCmdArgumentState().push0();
  return yyVal;
};
states[340] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getCmdArgumentState().pop();
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_begin", v1);
                    yyVal = v2;}
  return yyVal;
};
states[341] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_ENDARG);
  return yyVal;
};
states[342] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = null;
                    v2 = p.dispatch("on_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[343] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_ENDARG); 
  return yyVal;
};
states[344] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.dispatch("on_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[345] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[346] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_const_path_ref", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[347] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_top_const_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[348] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v2 = p.dispatch("on_array", v1);
                    yyVal = v2;}
  return yyVal;
};
states[349] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v2 = p.dispatch("on_hash", v1);
                    yyVal = v2;}
  return yyVal;
};
states[350] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_return0");
                    yyVal = v1;}
  return yyVal;
};
states[351] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_paren", v1);
                    v3 = v2;
                    v4 = p.dispatch("on_yield", v3);
                    yyVal = v4;}
  return yyVal;
};
states[352] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = p.dispatch("on_paren", v2);
                    v4 = v3;
                    v5 = p.dispatch("on_yield", v4);
                    yyVal = v5;}
  return yyVal;
};
states[353] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_yield0");
                    yyVal = v1;}
  return yyVal;
};
states[354] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_defined = true;
  return yyVal;
};
states[355] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_defined = false;
                    yyVal = p.new_defined(yyVals[yyTop - count + 1].start, ((IRubyObject)yyVals[-1+yyTop].value));
  return yyVal;
};
states[356] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((IRubyObject)yyVals[-1+yyTop].value)), BANG);
  return yyVal;
};
states[357] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(p.nil()), BANG);
  return yyVal;
};
states[358] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[360] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_method_add_block", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[361] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[362] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v4 = p.dispatch("on_if", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[363] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v4 = p.dispatch("on_unless", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[364] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_while", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[365] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_until", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[366] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.case_labels;
                    p.case_labels = p.getRuntime().getNil();
  return yyVal;
};
states[367] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_case", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[368] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.case_labels;
                    p.case_labels = null;
  return yyVal;
};
states[369] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = null;
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_case", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[370] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_case", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[371] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v4 = p.dispatch("on_for", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[372] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = p.getLexContext();
                    if (ctxt.in_def) {
                        p.yyerror("class definition in method body");
                    }
                    ctxt.in_class = true;
                    p.pushLocalScope();
  return yyVal;
};
states[373] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[374] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_def = false;
                    ctxt.in_class = false;
                    p.pushLocalScope();
  return yyVal;
};
states[375] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[376] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = p.getLexContext();
                    if (ctxt.in_def) { 
                        p.yyerror("module definition in method body");
                    }
                    ctxt.in_class = true;
                    p.pushLocalScope();
  return yyVal;
};
states[377] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[378] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.get_value(((Holder)yyVals[-3+yyTop].value));
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v4 = p.dispatch("on_def", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[379] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5, v6;
                    v1 = ((RubyArray) ((Holder)yyVals[-3+yyTop].value).value).eltOk(0);
                    v2 = ((RubyArray) ((Holder)yyVals[-3+yyTop].value).value).eltOk(1);
                    v3 = ((RubyArray) ((Holder)yyVals[-3+yyTop].value).value).eltOk(2);
                    v4 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v5 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v6 = p.dispatch("on_defs", v1, v2, v3, v4, v5);
                    yyVal = v6;}
  return yyVal;
};
states[380] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = p.dispatch("on_break", v2);
                    yyVal = v3;}
  return yyVal;
};
states[381] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = p.dispatch("on_next", v2);
                    yyVal = v3;}
  return yyVal;
};
states[382] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_redo");
                    yyVal = v1;}
  return yyVal;
};
states[383] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_retry");
                    yyVal = v1;}
  return yyVal;
};
states[384] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((IRubyObject)yyVals[0+yyTop].value));
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
                    if (yyVal == null) yyVal = p.nil();
  return yyVal;
};
states[385] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[386] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[387] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[388] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[389] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[390] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[391] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[392] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = (LexContext) p.getLexContext().clone();
  return yyVal;
};
states[393] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = (LexContext) p.getLexContext().clone();  
  return yyVal;
};
states[394] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
                    p.getLexContext().in_argdef = true;
  return yyVal;
};
states[395] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[396] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[397] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[398] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[399] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[400] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[401] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[402] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[403] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = p.getLexContext();
                    if (ctxt.in_class && !ctxt.in_def && !p.getCurrentScope().isBlockScope()) {
                        p.compile_error("Invalid return in class/module body");
                    }
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[410] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.escape(((IRubyObject)yyVals[0+yyTop].value));
                    v4 = p.dispatch("on_elsif", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[412] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_else", v1);
                    yyVal = v2;}
  return yyVal;
};
states[414] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  return yyVal;
};
states[415] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(((IRubyObject)yyVals[0+yyTop].value));}
  return yyVal;
};
states[416] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_mlhs_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[417] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_mlhs_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_mlhs_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[418] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_mlhs_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[419] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);}
  return yyVal;
};
states[420] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_mlhs_add_star", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[421] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[422] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_mlhs_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_mlhs_add_star", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[423] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[424] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(((IRubyObject)yyVals[0+yyTop].value));}
  return yyVal;
};
states[425] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = null;}
  return yyVal;
};
states[427] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.maybe_symbolize(LexingCommon.NIL);
  return yyVal;
};
states[428] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = false;
  return yyVal;
};
states[430] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-3+yyTop].value), ((IRubyObject)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[431] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-1+yyTop].value), (ByteList) null, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[432] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(p.src_line(), null, ((IRubyObject)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[433] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), null, (ByteList) null, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[434] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[435] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(p.src_line(), null, (ByteList) null, (ByteList) null);
  return yyVal;
};
states[436] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_excessed_comma");
                    yyVal = v1;}
  return yyVal;
};
states[437] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-5+yyTop].value), ((RubyArray)yyVals[-3+yyTop].value), ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[438] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-7+yyTop].value), ((RubyArray)yyVals[-5+yyTop].value), ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[439] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[440] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-5+yyTop].value), ((RubyArray)yyVals[-3+yyTop].value), null, ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[441] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-3+yyTop].value), null, ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[442] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-1+yyTop].value), null, ((IRubyObject)yyVals[0+yyTop].value), null, (ArgsTailHolder) null);
  return yyVal;
};
states[443] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-5+yyTop].value), null, ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[444] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-1+yyTop].value), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[445] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-3+yyTop].value), ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[446] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-5+yyTop].value), ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[447] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[448] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-3+yyTop].value), null, ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[449] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[450] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[451] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[452] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(p.src_line(), null, null, null, null, (ArgsTailHolder) null);
  return yyVal;
};
states[453] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setCommandStart(true);
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[454] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setCurrentArg(null);
                    p.ordinalMaxNumParam();
                    p.getLexContext().in_argdef = false;
{IRubyObject v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11;
                    v1 = null;
                    v2 = null;
                    v3 = null;
                    v4 = null;
                    v5 = null;
                    v6 = null;
                    v7 = null;
                    v8 = p.dispatch("on_params", v1, v2, v3, v4, v5, v6, v7);
                    v9 = v8;
                    v10 = p.escape(((RubyArray)yyVals[-1+yyTop].value));
                    v11 = p.dispatch("on_block_var", v9, v10);
                    yyVal = v11;}
  return yyVal;
};
states[455] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[456] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[457] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-1+yyTop].value);}
  return yyVal;
};
states[458] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[459] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[460] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.new_bv(((IRubyObject)yyVals[0+yyTop].value));
{                    yyVal = p.get_value(((IRubyObject)yyVals[0+yyTop].value));}
  return yyVal;
};
states[461] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[462] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pushBlockScope();
                    yyVal = p.getLeftParenBegin();
                    p.setLeftParenBegin(p.getParenNest());
  return yyVal;
};
states[463] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.resetMaxNumParam();
  return yyVal;
};
states[464] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.numparam_push();
  return yyVal;
};
states[465] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getCmdArgumentState().push0();
  return yyVal;
};
states[466] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    int max_numparam = p.restoreMaxNumParam(((Integer)yyVals[-4+yyTop].value));
                    p.getCmdArgumentState().pop();
                    /* Changed from MRI args_with_numbered put into parser codepath and not used by ripper (since it is just a passthrough method and types do not match).*/
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_lambda", v1, v2);
                    yyVal = v3;}
                    p.setLeftParenBegin(((Integer)yyVals[-5+yyTop].value));
                    p.numparam_pop(((Node)yyVals[-3+yyTop].value));
                    p.popCurrentScope();
  return yyVal;
};
states[467] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = false;
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.dispatch("on_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[468] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = false;
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[469] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[470] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[471] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[472] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /* Workaround for JRUBY-2326 (MRI does not enter this production for some reason)*/
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_method_add_block", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[473] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[474] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[475] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[476] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_fcall", v1);
                    v3 = v2;
                    v4 = ((IRubyObject)yyVals[0+yyTop].value);
                    v5 = p.dispatch("on_method_add_arg", v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[477] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[478] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[479] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.intern("::");
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_call", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[480] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[481] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[482] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_super", v1);
                    yyVal = v2;}
  return yyVal;
};
states[483] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_zsuper");
                    yyVal = v1;}
  return yyVal;
};
states[484] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v3 = p.dispatch("on_aref", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[485] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[486] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[487] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pushBlockScope();
  return yyVal;
};
states[488] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.resetMaxNumParam();
  return yyVal;
};
states[489] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.numparam_push();
  return yyVal;
};
states[490] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    int max_numparam = p.restoreMaxNumParam(((Integer)yyVals[-3+yyTop].value));
                    /* Changed from MRI args_with_numbered put into parser codepath and not used by ripper (since it is just a passthrough method and types do not match).*/
{IRubyObject v1, v2, v3;
                    v1 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_brace_block", v1, v2);
                    yyVal = v3;}
                    p.numparam_pop(((Node)yyVals[-2+yyTop].value));
                    p.popCurrentScope();                    
  return yyVal;
};
states[491] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pushBlockScope();
  return yyVal;
};
states[492] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.resetMaxNumParam();
  return yyVal;
};
states[493] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.numparam_push();
                    p.getCmdArgumentState().push0();
  return yyVal;
};
states[494] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    int max_numparam = p.restoreMaxNumParam(((Integer)yyVals[-3+yyTop].value));
                    /* Changed from MRI args_with_numbered put into parser codepath and not used by ripper (since it is just a passthrough method and types do not match).*/
{IRubyObject v1, v2, v3;
                    v1 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_do_block", v1, v2);
                    yyVal = v3;}
                    p.getCmdArgumentState().pop();
                    p.numparam_pop(((Node)yyVals[-2+yyTop].value));
                    p.popCurrentScope();
  return yyVal;
};
states[495] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_args_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[496] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_args_add_star", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[497] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_args_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[498] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_args_add_star", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[499] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.escape(((IRubyObject)yyVals[0+yyTop].value));
                    v4 = p.dispatch("on_when", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[502] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_BEG|EXPR_LABEL);
                    p.setCommandStart(false);
                    LexContext ctxt = (LexContext) p.getLexContext();
                    yyVals[0+yyTop].value = ctxt.in_kwarg;
                    ctxt.in_kwarg = true;
                    yyVal = p.push_pvtbl();
  return yyVal;
};
states[503] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.push_pktbl();
  return yyVal;
};
states[504] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    p.pop_pvtbl(((Set)yyVals[-3+yyTop].value));
                    p.getLexContext().in_kwarg = ((Boolean)yyVals[-4+yyTop].value);
  return yyVal;
};
states[505] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.escape(((IRubyObject)yyVals[0+yyTop].value));
                    v4 = p.dispatch("on_in", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[507] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[509] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = p.dispatch("on_if_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[510] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = p.dispatch("on_unless_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[512] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, ((IRubyObject)yyVals[-1+yyTop].value),
                                                   p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, true, null, null));
  return yyVal;
};
states[513] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, ((IRubyObject)yyVals[-2+yyTop].value), ((RubyArray)yyVals[0+yyTop].value));
  return yyVal;
};
states[514] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_find_pattern(null, ((RubyArray)yyVals[0+yyTop].value));
  return yyVal;
};
states[515] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, null, ((RubyArray)yyVals[0+yyTop].value));
  return yyVal;
};
states[516] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern(null, ((RubyArray)yyVals[0+yyTop].value));
  return yyVal;
};
states[518] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.symbolID(EQ_GT);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_binary", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[520] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.symbolID(OR);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_binary", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[522] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.push_pktbl();
  return yyVal;
};
states[523] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.push_pktbl();
  return yyVal;
};
states[526] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), ((IRubyObject)yyVals[-3+yyTop].value), null, ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[527] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_find_pattern(((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[528] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_hash_pattern(((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[529] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), ((IRubyObject)yyVals[-2+yyTop].value), null,
                                                    p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, false, null, null));
  return yyVal;
};
states[530] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), ((IRubyObject)yyVals[-3+yyTop].value), null, ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[531] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_find_pattern(((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[532] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_hash_pattern(((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[533] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), ((IRubyObject)yyVals[-2+yyTop].value), null,
                            p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, false, null, null));
  return yyVal;
};
states[534] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, null, ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[535] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_find_pattern(null, ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[536] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, null,
                            p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, false, null, null));
  return yyVal;
};
states[537] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.push_pktbl();
                    LexContext ctxt = p.getLexContext();
                    yyVals[0+yyTop].value = ctxt.in_kwarg;
                    ctxt.in_kwarg = false;
  return yyVal;
};
states[538] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    p.getLexContext().in_kwarg = ((Boolean)yyVals[-3+yyTop].value);
                    yyVal = p.new_hash_pattern(null, ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[539] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern(null, p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), null, (ByteList) null));
  return yyVal;
};
states[540] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.push_pktbl();
  return yyVal;
};
states[541] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[542] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                        yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), p.new_array(((IRubyObject)yyVals[0+yyTop].value)), false, null, null);

  return yyVal;
};
states[543] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[0+yyTop].value), true, null, null);
  return yyVal;
};
states[544] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
			RubyArray pre_args = ((RubyArray)yyVals[-1+yyTop].value).push(((RubyArray)yyVals[0+yyTop].value));
			yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), pre_args, false, null, null);

  return yyVal;
};
states[545] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-2+yyTop].value), true, ((IRubyObject)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[546] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-4+yyTop].value), true, ((IRubyObject)yyVals[-2+yyTop].value), ((RubyArray)yyVals[0+yyTop].value));
  return yyVal;
};
states[547] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-1+yyTop].value), true, null, null);
  return yyVal;
};
states[548] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-3+yyTop].value), true, null, ((RubyArray)yyVals[0+yyTop].value));
  return yyVal;
};
states[549] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((RubyArray)yyVals[0+yyTop].value);
  return yyVal;
};
states[550] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((RubyArray)yyVals[-1+yyTop].value);
  return yyVal;
};
states[551] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.get_value(((RubyArray)yyVals[-1+yyTop].value)));;}
  return yyVal;
};
states[552] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, true, ((IRubyObject)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[553] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, true, ((IRubyObject)yyVals[-2+yyTop].value), ((RubyArray)yyVals[0+yyTop].value));
  return yyVal;
};
states[554] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     p.warn_experimental(yyVals[yyTop - count + 1].start(), "Find pattern is experimental, and the behavior may change in future versions of Ruby!");
                     yyVal = p.new_find_pattern_tail(yyVals[yyTop - count + 1].start(), ((IRubyObject)yyVals[-4+yyTop].value), ((RubyArray)yyVals[-2+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[555] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[556] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[558] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.get_value(((RubyArray)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[559] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[560] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-2+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[561] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[0+yyTop].value), (ByteList) null);
  return yyVal;
};
states[562] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-1+yyTop].value), (ByteList) null);
  return yyVal;
};
states[563] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), null, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[564] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(((RubyArray)yyVals[0+yyTop].value));}
  return yyVal;
};
states[565] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(((RubyArray)yyVals[0+yyTop].value));;}
  return yyVal;
};
states[566] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.error_duplicate_pattern_key(((IRubyObject)yyVals[-1+yyTop].value));
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[-1+yyTop].value)),  p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[567] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.error_duplicate_pattern_key(((IRubyObject)yyVals[0+yyTop].value));
                    if (((IRubyObject)yyVals[0+yyTop].value) != null && !p.is_local_id(((IRubyObject)yyVals[0+yyTop].value))) {
                        p.yyerror("key must be valid as local variables");
                    }
                    p.error_duplicate_pattern_variable(((IRubyObject)yyVals[0+yyTop].value));
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)),  null);}
  return yyVal;
};
states[569] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (((IRubyObject)yyVals[-1+yyTop].value) == null || ((IRubyObject)yyVals[-1+yyTop].value) instanceof StrNode) {
                        yyVal = ((StrNode)yyVals[-1+yyTop].value).getValue();
                    }
                    /* JRuby changed (removed)*/
                    else {
                        p.yyerror("symbol literal with interpolation is not allowed");
                        yyVal = null;
                    }
  return yyVal;
};
states[570] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[571] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[572] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[573] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[574] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = KWNOREST;
  return yyVal;
};
states[576] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot2", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[577] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot3", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[578] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = null;
                    v3 = p.dispatch("on_dot2", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[579] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = null;
                    v3 = p.dispatch("on_dot3", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[583] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = null;
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot2", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[584] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = null;
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot3", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[589] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[590] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[591] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[592] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[593] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[594] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[595] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[596] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[597] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[598] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
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
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[601] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
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
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_begin", v1);
                    yyVal = v2;}
  return yyVal;
};
states[605] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_top_const_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[606] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_const_path_ref", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[607] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[608] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = p.escape(((IRubyObject)yyVals[-4+yyTop].value));
                    v2 = p.escape(((IRubyObject)yyVals[-3+yyTop].value));
                    v3 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v4 = p.escape(((IRubyObject)yyVals[0+yyTop].value));
                    v5 = p.dispatch("on_rescue", v1, v2, v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[609] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null; 
  return yyVal;
};
states[610] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[611] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);}
  return yyVal;
};
states[613] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[615] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_ensure", v1);
                    yyVal = v2;}
  return yyVal;
};
states[617] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[618] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[619] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);}
  return yyVal;
};
states[620] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[621] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[622] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_string_concat", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[623] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = p.heredoc_dedent(((IRubyObject)yyVals[-1+yyTop].value));
                    v2 = p.dispatch("on_string_literal", v1);
                    yyVal = v2;}
  return yyVal;
};
states[624] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = p.heredoc_dedent(((IRubyObject)yyVals[-1+yyTop].value));
                    v2 = p.dispatch("on_xstring_literal", v1);
                    yyVal = v2;}
  return yyVal;
};
states[625] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_regexp(yyVals[yyTop - count + 2].start(), ((IRubyObject)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[626] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_array", v1);
                    yyVal = v2;}
  return yyVal;
};
states[627] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_words_new");
                    yyVal = v1;}
  return yyVal;
};
states[628] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_words_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[629] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_word_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_word_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[630] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_word_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[631] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_array", v1);
                    yyVal = v2;}
  return yyVal;
};
states[632] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_symbols_new");
                    yyVal = v1;}
  return yyVal;
};
states[633] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_symbols_add", v1, v2);
                    yyVal = v3;}
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
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_array", v1);
                    yyVal = v2;}
  return yyVal;
};
states[636] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_qwords_new");
                    yyVal = v1;}
  return yyVal;
};
states[637] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_qwords_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[638] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_qsymbols_new");
                    yyVal = v1;}
  return yyVal;
};
states[639] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_qsymbols_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[640] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    ByteList aChar = ByteList.create("");
                    aChar.setEncoding(p.getEncoding());
                    yyVal = p.createStr(aChar, 0);
{IRubyObject v1;
                    v1 = p.dispatch("on_string_content");
                    yyVal = v1;}
  return yyVal;
};
states[641] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_string_add", v1, v2);
                    yyVal = v3;}
                    /* JRuby changed (removed)*/
  return yyVal;
};
states[642] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_xstring_new");
                    yyVal = v1;}
  return yyVal;
};
states[643] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_xstring_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[644] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_regexp_new");
                    yyVal = v1;}
  return yyVal;
};
states[645] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /* FIXME: mri is different here.*/
			yyVal = p.dispatch("on_regexp_add", ((IRubyObject)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));

  return yyVal;
};
states[646] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
{                    yyVal = p.ripper_new_yylval(null, p.get_value(((IRubyObject)yyVals[0+yyTop].value)), ((IRubyObject)yyVals[0+yyTop].value));}
  return yyVal;
};
states[647] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.getStrTerm();
                    p.setStrTerm(null);
                    p.setState(EXPR_BEG);
  return yyVal;
};
states[648] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_string_dvar", v1);
                    yyVal = v2;}
  return yyVal;
};
states[649] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = p.getStrTerm();
                   p.setStrTerm(null);
                   p.getConditionState().push0();
                   p.getCmdArgumentState().push0();
  return yyVal;
};
states[650] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = p.getState();
                   p.setState(EXPR_BEG);
  return yyVal;
};
states[651] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = p.getBraceNest();
                   p.setBraceNest(0);
  return yyVal;
};
states[652] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = p.getHeredocIndent();
                   p.setHeredocIndent(0);
  return yyVal;
};
states[653] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   p.getConditionState().pop();
                   p.getCmdArgumentState().pop();
                   p.setStrTerm(((StrTerm)yyVals[-5+yyTop].value));
                   p.setState(((Integer)yyVals[-4+yyTop].value));
                   p.setBraceNest(((Integer)yyVals[-3+yyTop].value));
                   p.setHeredocIndent(((Integer)yyVals[-2+yyTop].value));
                   p.setHeredocLineIndent(-1);

{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_string_embexpr", v1);
                    yyVal = v2;}
  return yyVal;
};
states[654] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[655] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[656] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[660] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_END);
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_symbol", v1);
                    v3 = v2;
                    v4 = p.dispatch("on_symbol_literal", v3);
                    yyVal = v4;}
  return yyVal;
};
states[662] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[663] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[664] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[665] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_END);

{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_dyna_symbol", v1);
                    yyVal = v2;}
  return yyVal;
};
states[666] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);  
  return yyVal;
};
states[667] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  return yyVal;
};
states[671] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[672] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[673] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[674] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[675] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (p.id_is_var(yyVals[yyTop - count + 1].id)) {
                        yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop].value));
                    } else {
                        yyVal = p.dispatch("on_vcall", ((IRubyObject)yyVals[0+yyTop].value));
                    }

  return yyVal;
};
states[676] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (p.id_is_var(yyVals[yyTop - count + 1].id)) {
                        yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop].value));
                    } else {
                        yyVal = p.dispatch("on_vcall", ((IRubyObject)yyVals[0+yyTop].value));
                    }

  return yyVal;
};
states[677] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (p.id_is_var(yyVals[yyTop - count + 1].id)) {
                        yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop].value));
                    } else {
                        yyVal = p.dispatch("on_vcall", ((IRubyObject)yyVals[0+yyTop].value));
                    }

  return yyVal;
};
states[678] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (p.id_is_var(yyVals[yyTop - count + 1].id)) {
                        yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop].value));
                    } else {
                        yyVal = p.dispatch("on_vcall", ((IRubyObject)yyVals[0+yyTop].value));
                    }

  return yyVal;
};
states[679] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (p.id_is_var(yyVals[yyTop - count + 1].id)) {
                        yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop].value));
                    } else {
                        yyVal = p.dispatch("on_vcall", ((IRubyObject)yyVals[0+yyTop].value));
                    }

  return yyVal;
};
states[680] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[681] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[682] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[683] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[684] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[685] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
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
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[688] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[689] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[690] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[691] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[692] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[693] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[694] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[695] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[696] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[697] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[698] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.assignable(p.var_field(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[699] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[700] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[701] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   p.setState(EXPR_BEG);
                   p.setCommandStart(true);
  return yyVal;
};
states[702] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[703] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = null;}
  return yyVal;
};
states[705] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = false;
                    yyVal = p.new_args(p.tokline(), null, null, null, null, 
                                    p.new_args_tail(p.src_line(), null, (ByteList) null, (ByteList) null));
  return yyVal;
};
states[706] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_paren", v1);
                    yyVal = v2;}
                    p.setState(EXPR_BEG);
                    p.getLexContext().in_argdef = false;
                    p.setCommandStart(true);
  return yyVal;
};
states[707] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[708] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = p.getLexContext();
                    yyVal = ctxt.in_kwarg;
                    ctxt.in_kwarg = true;
                    ctxt.in_argdef = true;
                    p.setState(p.getState() | EXPR_LABEL);
  return yyVal;
};
states[709] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_kwarg = ((Boolean)yyVals[-2+yyTop].value);
                    ctxt.in_argdef = false;
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
                    p.setState(EXPR_BEG);
                    p.setCommandStart(true);
  return yyVal;
};
states[710] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-3+yyTop].value), ((IRubyObject)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[711] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-1+yyTop].value), (ByteList) null, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[712] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(p.src_line(), null, ((IRubyObject)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[713] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), null, (ByteList) null, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[714] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.add_forwarding_args();
                    yyVal = p.new_args_tail(p.tokline(), null, ((IRubyObject)yyVals[0+yyTop].value), FWD_BLOCK);
  return yyVal;
};
states[715] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[716] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(p.src_line(), null, (ByteList) null, (ByteList) null);
  return yyVal;
};
states[717] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-5+yyTop].value), ((RubyArray)yyVals[-3+yyTop].value), ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[718] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-7+yyTop].value), ((RubyArray)yyVals[-5+yyTop].value), ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[719] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[720] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-5+yyTop].value), ((RubyArray)yyVals[-3+yyTop].value), null, ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[721] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-3+yyTop].value), null, ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[722] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-5+yyTop].value), null, ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[723] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-1+yyTop].value), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[724] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-3+yyTop].value), ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[725] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-5+yyTop].value), ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[726] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[727] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-3+yyTop].value), null, ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[728] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[729] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[730] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[731] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(p.src_line(), null, null, null, null, (ArgsTailHolder) null);
  return yyVal;
};
states[732] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_args_forward");
                    yyVal = v1;}
  return yyVal;
};
states[733] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "formal argument cannot be a constant";
{IRubyObject v1, v2, v3;
                    v1 = p.intern(message);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_param_error", v1, v2);
                    yyVal = v3;
                    p.error();}
  return yyVal;
};
states[734] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "formal argument cannot be an instance variable";
{IRubyObject v1, v2, v3;
                    v1 = p.intern(message);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_param_error", v1, v2);
                    yyVal = v3;
                    p.error();}
  return yyVal;
};
states[735] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "formal argument cannot be a global variable";
{IRubyObject v1, v2, v3;
                    v1 = p.intern(message);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_param_error", v1, v2);
                    yyVal = v3;
                    p.error();}
  return yyVal;
};
states[736] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "formal argument cannot be a class variable";
{IRubyObject v1, v2, v3;
                    v1 = p.intern(message);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_param_error", v1, v2);
                    yyVal = v3;
                    p.error();}
  return yyVal;
};
states[737] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value); /* Not really reached*/
  return yyVal;
};
states[738] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.formal_argument(((IRubyObject)yyVals[0+yyTop].value));
                    p.ordinalMaxNumParam();
  return yyVal;
};
states[739] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    RubySymbol name = p.get_id(((IRubyObject)yyVals[0+yyTop].value));
                    p.setCurrentArg(name);
{                    yyVal = p.get_value(((IRubyObject)yyVals[0+yyTop].value));}
  return yyVal;
};
states[740] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setCurrentArg(null);
{                    yyVal = p.get_value(((IRubyObject)yyVals[0+yyTop].value));}
  return yyVal;
};
states[741] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_mlhs_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[742] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[743] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[744] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.arg_var(p.formal_argument(((IRubyObject)yyVals[0+yyTop].value)));
                    p.setCurrentArg(p.get_id(((IRubyObject)yyVals[0+yyTop].value)));
                    p.ordinalMaxNumParam();
                    p.getLexContext().in_argdef = false;
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[745] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setCurrentArg(null);
                    p.getLexContext().in_argdef = true;
{                    yyVal = p.new_assoc(p.get_value(p.assignable(((IRubyObject)yyVals[-1+yyTop].value))), p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[746] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setCurrentArg(null);
                    p.getLexContext().in_argdef = true;
{                    yyVal = p.new_assoc(p.get_value(p.assignable(((IRubyObject)yyVals[0+yyTop].value))), null);}
  return yyVal;
};
states[747] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = true;
{                    yyVal = p.new_assoc(p.get_value(p.assignable(((IRubyObject)yyVals[-1+yyTop].value))), p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[748] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = true;
{                    yyVal = p.new_assoc(p.get_value(p.assignable(((IRubyObject)yyVals[0+yyTop].value))), null);}
  return yyVal;
};
states[749] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[750] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[751] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[752] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[753] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[754] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[755] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = null;
                    v2 = p.dispatch("on_nokw_param", v1);
                    yyVal = v2;}
  return yyVal;
};
states[756] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.shadowing_lvar(((IRubyObject)yyVals[0+yyTop].value));
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_kwrest_param", v1);
                    yyVal = v2;}
  return yyVal;
};
states[757] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = null;
                    v2 = p.dispatch("on_kwrest_param", v1);
                    yyVal = v2;}
  return yyVal;
};
states[758] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_assoc(p.get_value(p.assignable(((IRubyObject)yyVals[-2+yyTop].value))), p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[759] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = true;
                    p.setCurrentArg(null);
{                    yyVal = p.new_assoc(p.get_value(p.assignable(((IRubyObject)yyVals[-2+yyTop].value))), p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[760] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[761] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[762] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[763] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[764] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = STAR;
  return yyVal;
};
states[765] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[766] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (!p.is_local_id(((IRubyObject)yyVals[0+yyTop].value))) {
                        p.yyerror("rest argument must be local variable");
                    }
                    yyVal = p.arg_var(p.shadowing_lvar(((IRubyObject)yyVals[0+yyTop].value)));
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_rest_param", v1);
                    yyVal = v2;}
  return yyVal;
};
states[767] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = null;
                    v2 = p.dispatch("on_rest_param", v1);
                    yyVal = v2;}
  return yyVal;
};
states[768] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = AMPERSAND;
  return yyVal;
};
states[769] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[770] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (!p.is_local_id(((IRubyObject)yyVals[0+yyTop].value))) {
                        p.yyerror("block argument must be local variable");
                    }
                    yyVal = p.arg_var(p.shadowing_lvar(((IRubyObject)yyVals[0+yyTop].value)));
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_blockarg", v1);
                    yyVal = v2;}
  return yyVal;
};
states[771] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.arg_var(p.shadowing_lvar(FWD_BLOCK));
                        yyVal = p.dispatch("on_blockarg", p.nil());

  return yyVal;
};
states[772] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[773] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[774] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((IRubyObject)yyVals[0+yyTop].value));
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[775] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_BEG);
  return yyVal;
};
states[776] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[777] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  return yyVal;
};
states[778] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((RubyArray)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_assoclist_from_args", v1);
                    yyVal = v2;}
  return yyVal;
};
states[779] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[780] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[781] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_assoc_new", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[782] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_assoc_new", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[783] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = null;
                    v3 = p.dispatch("on_assoc_new", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[784] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.dispatch("on_dyna_symbol", v1);
                    v3 = v2;
                    v4 = ((IRubyObject)yyVals[0+yyTop].value);
                    v5 = p.dispatch("on_assoc_new", v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[785] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_assoc_splat", v1);
                    yyVal = v2;}
  return yyVal;
};
states[786] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[787] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[788] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[789] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[790] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[791] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[792] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
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
                    yyVal = p.maybe_symbolize(DOT);
  return yyVal;
};
states[797] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[798] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(DOT);
  return yyVal;
};
states[799] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[801] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.maybe_symbolize(((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[806] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RPAREN;
  return yyVal;
};
states[807] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RBRACKET;
  return yyVal;
};
states[808] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RCURLY;
  return yyVal;
};
states[816] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = null;
  return yyVal;
};
states[817] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  yyVal = null;
  return yyVal;
};
}
					// line 4513 "ripper_RubyParser.out"

}
					// line 14287 "-"
