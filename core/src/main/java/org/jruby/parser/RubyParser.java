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

import org.jruby.RubySymbol;
import org.jruby.ast.*;
import org.jruby.common.IRubyWarnings;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.lexer.LexerSource;
import org.jruby.lexer.LexingCommon;
import org.jruby.lexer.yacc.StrTerm;

import org.jruby.util.ByteList;
import org.jruby.util.CommonByteLists;
import org.jruby.util.KeyValuePair;
import org.jruby.util.StringSupport;
import org.jruby.lexer.yacc.LexContext;
import org.jruby.lexer.yacc.RubyLexer;
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
import static org.jruby.lexer.LexingCommon.COLON_COLON;
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
 
 public class RubyParser extends RubyParserBase {
    public RubyParser(LexerSource source, IRubyWarnings warnings) {
        super(warnings); setLexer(new RubyLexer(this, source, warnings));
    }
					// line 99 "-"
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
  public static final int k__END__ = 380;
  public static final int tLOWEST = 381;
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
    "tEMBDOC","tEMBDOC_END","tHEREDOC_BEG","tHEREDOC_END","k__END__",
    "tLOWEST",
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

static ParserState<RubyParser>[] states = new ParserState[818];
static {
states[1] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  p.setState(EXPR_BEG);
                  p.initTopLocalVariables();
  return yyVal;
};
states[2] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  /*%%%*/
                  Node expr = ((Node)yyVals[0+yyTop].value);
                  if (expr != null && !p.getConfiguration().isEvalParse()) {
                      /* last expression should not be void */
                      if (((Node)yyVals[0+yyTop].value) instanceof BlockNode) {
                        expr = ((BlockNode)yyVals[0+yyTop].value).getLast();
                      } else {
                        expr = ((Node)yyVals[0+yyTop].value);
                      }
                      expr = p.remove_begin(expr);
                      p.void_expr(expr);
                  }
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
                  yyVal = p.remove_begin(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[9] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[10] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  /*%%%*/
                  p.getResult().addBeginNode(new PreExe19Node(yyVals[yyTop - count + 1].start(), p.getCurrentScope(), ((Node)yyVals[-1+yyTop].value), p.src_line()));
                  /*                  $$ = new BeginNode(yyVals[yyTop - count + 1].start(), p.makeNullNil($2));*/
                  yyVal = null;
                  /*% %*/
                  /*% ripper: BEGIN!($2) %*/
  return yyVal;
};
states[11] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  return yyVal;
};
states[12] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  /*%%%*/
                   yyVal = p.new_bodystmt(((Node)yyVals[-5+yyTop].value), ((RescueBodyNode)yyVals[-4+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
                  /*% %*/
                  /*% ripper: bodystmt!(escape_Qundef($1), escape_Qundef($2), escape_Qundef($5), escape_Qundef($6)) %*/
  return yyVal;
};
states[13] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  /*%%%*/
                   yyVal = p.new_bodystmt(((Node)yyVals[-2+yyTop].value), ((RescueBodyNode)yyVals[-1+yyTop].value), null, ((Node)yyVals[0+yyTop].value));
                  /*% %*/
                  /*% ripper: bodystmt!(escape_Qundef($1), escape_Qundef($2), Qnil, escape_Qundef($3)) %*/
  return yyVal;
};
states[14] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.void_stmts(((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[15] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   /*%%%*/
                   yyVal = null;
                   /*% %*/
                   /*% ripper: stmts_add!(stmts_new!, void_stmt!) %*/
  return yyVal;
};
states[16] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   /*%%%*/
                    yyVal = p.newline_node(((Node)yyVals[0+yyTop].value), yyVals[yyTop - count + 1].start());
                   /*% %*/
                   /*% ripper: stmts_add!(stmts_new!, $1) %*/
  return yyVal;
};
states[17] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   /*%%%*/
                    yyVal = p.appendToBlock(((Node)yyVals[-2+yyTop].value), p.newline_node(((Node)yyVals[0+yyTop].value), yyVals[yyTop - count + 3].start()));
                   /*% %*/
                   /*% ripper: stmts_add!($1, $3) %*/
  return yyVal;
};
states[18] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[19] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[20] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   p.yyerror("BEGIN is permitted only at toplevel");
  return yyVal;
};
states[21] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[22] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_FNAME|EXPR_FITEM);
  return yyVal;
};
states[23] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = newAlias(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: alias!($2, $4) %*/
  return yyVal;
};
states[24] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new VAliasNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[-1+yyTop].value)), p.symbolID(((ByteList)yyVals[0+yyTop].value)));
                    /*% %*/
                    /*% ripper: var_alias!($2, $3) %*/
  return yyVal;
};
states[25] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new VAliasNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[-1+yyTop].value)), p.symbolID(((BackRefNode)yyVals[0+yyTop].value).getByteName()));
                    /*% %*/
                    /*% ripper: var_alias!($2, $3) %*/
  return yyVal;
};
states[26] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "can't make alias for the number variables";
                    /*%%%*/
                    p.yyerror(message);
                    /*% %*/
                    /*% ripper[error]: alias_error!(ERR_MESG(), $3) %*/
  return yyVal;
};
states[27] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((Node)yyVals[0+yyTop].value);
                    /*% %*/
                    /*% ripper: undef!($2) %*/
  return yyVal;
};
states[28] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_if(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value), p.remove_begin(((Node)yyVals[-2+yyTop].value)), null);
                    p.fixpos(((Node)yyVal), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: if_mod!($3, $1) %*/
  return yyVal;
};
states[29] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_if(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value), null, p.remove_begin(((Node)yyVals[-2+yyTop].value)));
                    p.fixpos(((Node)yyVal), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: unless_mod!($3, $1) %*/
  return yyVal;
};
states[30] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (((Node)yyVals[-2+yyTop].value) != null && ((Node)yyVals[-2+yyTop].value) instanceof BeginNode) {
                        yyVal = new WhileNode(yyVals[yyTop - count + 1].start(), p.cond(((Node)yyVals[0+yyTop].value)), ((BeginNode)yyVals[-2+yyTop].value).getBodyNode(), false);
                    } else {
                        yyVal = new WhileNode(yyVals[yyTop - count + 1].start(), p.cond(((Node)yyVals[0+yyTop].value)), ((Node)yyVals[-2+yyTop].value), true);
                    }
                    /*% %*/
                    /*% ripper: while_mod!($3, $1) %*/
  return yyVal;
};
states[31] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (((Node)yyVals[-2+yyTop].value) != null && ((Node)yyVals[-2+yyTop].value) instanceof BeginNode) {
                        yyVal = new UntilNode(yyVals[yyTop - count + 1].start(), p.cond(((Node)yyVals[0+yyTop].value)), ((BeginNode)yyVals[-2+yyTop].value).getBodyNode(), false);
                    } else {
                        yyVal = new UntilNode(yyVals[yyTop - count + 1].start(), p.cond(((Node)yyVals[0+yyTop].value)), ((Node)yyVals[-2+yyTop].value), true);
                    }
                    /*% %*/
                    /*% ripper: until_mod!($3, $1) %*/
  return yyVal;
};
states[32] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newRescueModNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rescue_mod!($1, $3) %*/
  return yyVal;
};
states[33] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   if (p.getLexContext().in_def) {
                       p.warn("END in method; use at_exit");
                    }
                    /*%%%*/
                   yyVal = new PostExeNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-1+yyTop].value), p.src_line());
                    /*% %*/
                    /*% ripper: END!($3) %*/
  return yyVal;
};
states[35] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = node_assign(((MultipleAsgnNode)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: massign!($1, $4) %*/
  return yyVal;
};
states[36] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = node_assign(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: assign!($1, $4) %*/
  return yyVal;
};
states[37] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = node_assign(((MultipleAsgnNode)yyVals[-5+yyTop].value), p.newRescueModNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value)));
                    /*% %*/
                    /*% ripper: massign!($1, rescue_mod!($4, $6)) %*/
  return yyVal;
};
states[38] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = node_assign(((MultipleAsgnNode)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: massign!($1, $4) %*/
  return yyVal;
};
states[40] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = node_assign(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: assign!($1, $4) %*/
  return yyVal;
};
states[41] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = p.new_op_assign(((AssignableNode)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!($1, $2, $4) %*/
  return yyVal;
};
states[42] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = p.new_ary_op_assign(((Node)yyVals[-6+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!(aref_field!($1, escape_Qundef($3)), $5, $7) %*/
  return yyVal;
};
states[43] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = p.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!(field!($1, $2, $3), $4, $6) %*/
  return yyVal;
};
states[44] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = p.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!(field!($1, $2, $3), $4, $6) %*/
  return yyVal;
};
states[45] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    int line = yyVals[yyTop - count + 1].start();
                    yyVal = p.new_const_op_assign(line, p.new_colon2(line, ((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-3+yyTop].value)), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!(const_path_field!($1, $3), $4, $6) %*/
  return yyVal;
};
states[46] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = p.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!(field!($1, ID2VAL(idCOLON2), $3), $4, $6) %*/
  return yyVal;
};
states[47] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((DefHolder)yyVals[-3+yyTop].value));
                    p.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
                    /*%%%*/
                    yyVal = new DefnNode(((DefHolder)yyVals[-3+yyTop].value).line, ((DefHolder)yyVals[-3+yyTop].value).name, ((ArgsNode)yyVals[-2+yyTop].value), p.getCurrentScope(), p.reduce_nodes(p.remove_begin(((Node)yyVals[0+yyTop].value))), yyVals[yyTop - count + 4].end());
                    /* Changed from MRI*/
                    /*% %*/
                    /*% ripper: def!(get_value($1), $2, bodystmt!($4, Qnil, Qnil, Qnil)) %*/
                    p.popCurrentScope();
  return yyVal;
};
states[48] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((DefHolder)yyVals[-5+yyTop].value));
                    p.restore_defun(((DefHolder)yyVals[-5+yyTop].value));
                    /*%%%*/
                    Node body = p.reduce_nodes(p.remove_begin(p.rescued_expr(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value))));
                    yyVal = new DefnNode(((DefHolder)yyVals[-5+yyTop].value).line, ((DefHolder)yyVals[-5+yyTop].value).name, ((ArgsNode)yyVals[-4+yyTop].value), p.getCurrentScope(), body, yyVals[yyTop - count + 6].end());
                    /* Changed from MRI (cmobined two stmts)*/
                    /*% %*/
                    /*% ripper: def!(get_value($1), $2, bodystmt!(rescue_mod!($4, $6), Qnil, Qnil, Qnil)) %*/

                    p.popCurrentScope();
  return yyVal;
};
states[49] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((DefHolder)yyVals[-3+yyTop].value));
                    p.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
                    /*%%%*/
                    yyVal = new DefsNode(((DefHolder)yyVals[-3+yyTop].value).line, (Node) ((DefHolder)yyVals[-3+yyTop].value).singleton, ((DefHolder)yyVals[-3+yyTop].value).name, ((ArgsNode)yyVals[-2+yyTop].value), p.getCurrentScope(), p.reduce_nodes(p.remove_begin(((Node)yyVals[0+yyTop].value))), yyVals[yyTop - count + 4].end());
                    /*% %*/                    
                    /*% ripper: defs!(AREF($1, 0), AREF($1, 1), AREF($1, 2), $2, bodystmt!($4, Qnil, Qnil, Qnil)) %*/
                    p.popCurrentScope();
  return yyVal;
};
states[50] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((DefHolder)yyVals[-5+yyTop].value));
                    p.restore_defun(((DefHolder)yyVals[-5+yyTop].value));
                    /*%%%*/
                    Node body = p.reduce_nodes(p.remove_begin(p.rescued_expr(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value))));
                    yyVal = new DefsNode(((DefHolder)yyVals[-5+yyTop].value).line, (Node) ((DefHolder)yyVals[-5+yyTop].value).singleton, ((DefHolder)yyVals[-5+yyTop].value).name, ((ArgsNode)yyVals[-4+yyTop].value), p.getCurrentScope(), body, yyVals[yyTop - count + 6].end());
                    /*% %*/                    
                    /*% ripper: defs!(AREF($1, 0), AREF($1, 1), AREF($1, 2), $2, bodystmt!(rescue_mod!($4, $6), Qnil, Qnil, Qnil)) %*/
                    p.popCurrentScope();
  return yyVal;
};
states[51] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.backrefAssignError(((Node)yyVals[-3+yyTop].value));
                    /*% %*/
                    /*% ripper[error]: backref_error(p, RNODE($1), assign!(var_field(p, $1), $4)) %*/
  return yyVal;
};
states[52] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[53] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[-2+yyTop].value));
                    yyVal = p.newRescueModNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rescue_mod!($1, $3) %*/
  return yyVal;
};
states[56] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.logop(((Node)yyVals[-2+yyTop].value), AMPERSAND_AMPERSAND, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[57] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.logop(((Node)yyVals[-2+yyTop].value), OR_OR, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[58] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((Node)yyVals[0+yyTop].value)), NOT);
  return yyVal;
};
states[59] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((Node)yyVals[0+yyTop].value)), BANG);
  return yyVal;
};
states[60] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((Node)yyVals[-1+yyTop].value));
                    p.setState(EXPR_BEG|EXPR_LABEL);
                    p.setCommandStart(false);
                    /* MRI 3.1 uses $2 but we want tASSOC typed?*/
                    LexContext ctxt = p.getLexContext();
                    yyVal = ctxt.in_kwarg;
                    ctxt.in_kwarg = true;
  return yyVal;
};
states[61] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.push_pvtbl();
  return yyVal;
};
states[62] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pvtbl(((Set)yyVals[-1+yyTop].value));
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_kwarg = ((Boolean)yyVals[-2+yyTop].value);
                    /*%%%*/
                    yyVal = p.newPatternCaseNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-4+yyTop].value), p.newIn(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value), null, null));
                    /*% %*/
                    /*% ripper: case!($1, in!($5, Qnil, Qnil)) %*/
  return yyVal;
};
states[63] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((Node)yyVals[-1+yyTop].value));
                    p.setState(EXPR_BEG|EXPR_LABEL);
                    p.setCommandStart(false);
                    LexContext ctxt = p.getLexContext();
                    yyVal = ctxt.in_kwarg;
                    ctxt.in_kwarg = true;
  return yyVal;
};
states[64] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.push_pvtbl();
  return yyVal;
};
states[65] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pvtbl(((Set)yyVals[-1+yyTop].value));
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_kwarg = ((Boolean)yyVals[-2+yyTop].value);
                    /*%%%*/
                    yyVal = p.newPatternCaseNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-4+yyTop].value), p.newIn(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value), new TrueNode(yyVals[yyTop - count + 1].start()), new FalseNode(yyVals[yyTop - count + 1].start())));
                    /*% %*/
                    /*% ripper: case!($1, in!($5, Qnil, Qnil)) %*/
  return yyVal;
};
states[67] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pushLocalScope();
                    ByteList currentArg = p.getCurrentArg();
                    p.setCurrentArg(null);
                    LexContext ctxt = p.getLexContext();
                    RubySymbol name = p.get_id(((ByteList)yyVals[0+yyTop].value));
                    /*%%%*/
                    p.numparam_name(((ByteList)yyVals[0+yyTop].value));
                    yyVal = new DefHolder(name, currentArg, (LexContext) ctxt.clone());
                    /* Changed from MRI*/
                    /*%
                        p.numparam_name(yyVals[yyTop - count + 1].id);
                        $$ = new DefHolder(p.get_id(yyVals[yyTop - count + 1].id), currentArg, p.get_value($1), (LexContext) ctxt.clone());
                    %*/
                    ctxt.in_def = true;
                    p.setCurrentArg(null);
  return yyVal;
};
states[68] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    ((DefHolder)yyVals[0+yyTop].value).line = yyVals[yyTop - count + 1].start();
                    yyVal = ((DefHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[69] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_FNAME); 
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_argdef = true;
  return yyVal;
};
states[70] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_ENDFN|EXPR_LABEL);
                    ((DefHolder)yyVals[0+yyTop].value).line = yyVals[yyTop - count + 1].start();
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
states[71] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[72] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getConditionState().push1();
  return yyVal;
};
states[73] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getConditionState().pop();
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[77] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].start());
                    /*% %*/
                    /*% ripper: method_add_arg!(call!($1, $2, $3), $4) %*/
  return yyVal;
};
states[78] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IterNode)yyVals[-1+yyTop].value);
                    /*%%%*/
                    /* FIXME: Missing loc stuff here.*/
                    /*% %*/
  return yyVal;
};
states[79] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_fcall(((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: $1 %*/
  return yyVal;
};
states[80] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.frobnicate_fcall_args(((FCallNode)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
                    yyVal = ((FCallNode)yyVals[-1+yyTop].value);
                    /*% %*/
                    /*% ripper: command!($1, $2) %*/
  return yyVal;
};
states[81] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.frobnicate_fcall_args(((FCallNode)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value));
                    yyVal = ((FCallNode)yyVals[-2+yyTop].value);
                    /*% %*/
                    /*% ripper: method_add_block!(command!($1, $2), $3) %*/
  return yyVal;
};
states[82] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].start());
                    /*% %*/
                    /*% ripper: command_call!($1, $2, $3, $4) %*/
  return yyVal;
};
states[83] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value), yyVals[yyTop - count + 3].start());
                    /*% %*/
                    /*% ripper: method_add_block!(command_call!($1, $2, $3, $4), $5) %*/
  return yyVal;
};
states[84] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
                    /*% %*/
                    /*% ripper: command_call!($1, ID2VAL(idCOLON2), $3, $4) %*/
  return yyVal;
};
states[85] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: method_add_block!(command_call!($1, ID2VAL(idCOLON2), $3, $4), $5) %*/
  return yyVal;
};
states[86] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_super(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: super!($2) %*/
  return yyVal;
};
states[87] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_yield(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: yield!($2) %*/
  return yyVal;
};
states[88] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ReturnNode(yyVals[yyTop - count + 1].start(), p.ret_args(((Node)yyVals[0+yyTop].value), yyVals[yyTop - count + 1].start()));
                    /*% %*/
                    /*% ripper: return!($2) %*/
  return yyVal;
};
states[89] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new BreakNode(yyVals[yyTop - count + 1].start(), p.ret_args(((Node)yyVals[0+yyTop].value), yyVals[yyTop - count + 1].start()));
                    /*% %*/
                    /*% ripper: break!($2) %*/
  return yyVal;
};
states[90] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new NextNode(yyVals[yyTop - count + 1].start(), p.ret_args(((Node)yyVals[0+yyTop].value), yyVals[yyTop - count + 1].start()));
                    /*% %*/
                    /*% ripper: next!($2) %*/
  return yyVal;
};
states[92] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((Node)yyVals[-1+yyTop].value);
                    /*% %*/
                    /*% ripper: mlhs_paren!($2) %*/
  return yyVal;
};
states[93] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((MultipleAsgnNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[94] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(((Integer)yyVals[-2+yyTop].value), p.newArrayNode(((Integer)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value)), null, null);
                    /*% %*/
                    /*% ripper: mlhs_paren!($2) %*/
  return yyVal;
};
states[95] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[0+yyTop].value), null, null);
                    /*% %*/
                    /*% ripper: $1 %*/
  return yyVal;
};
states[96] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-1+yyTop].value).add(((Node)yyVals[0+yyTop].value)), null, null);
                    /*% %*/
                    /*% ripper: mlhs_add!($1, $2) %*/
  return yyVal;
};
states[97] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value), (ListNode) null);
                    /*% %*/
                    /*% ripper: mlhs_add_star!($1, $3) %*/
  return yyVal;
};
states[98] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-4+yyTop].value), ((Node)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add_post!(mlhs_add_star!($1, $3), $5) %*/
  return yyVal;
};
states[99] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-1+yyTop].value), new StarNode(p.src_line()), null);
                    /*% %*/
                    /*% ripper: mlhs_add_star!($1, Qnil) %*/
  return yyVal;
};
states[100] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-3+yyTop].value), new StarNode(p.src_line()), ((ListNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add_post!(mlhs_add_star!($1, Qnil), $4) %*/
  return yyVal;
};
states[101] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(yyVals[yyTop - count + 2].start(), null, ((Node)yyVals[0+yyTop].value), null);
                    /*% %*/
                    /*% ripper: mlhs_add_star!(mlhs_new!, $2) %*/
  return yyVal;
};
states[102] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(yyVals[yyTop - count + 2].start(), null, ((Node)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add_post!(mlhs_add_star!(mlhs_new!, $2), $4) %*/
  return yyVal;
};
states[103] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(p.src_line(), null, new StarNode(p.src_line()), null);
                    /*% %*/
                    /*% ripper: mlhs_add_star!(mlhs_new!, Qnil) %*/
  return yyVal;
};
states[104] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(p.src_line(), null, new StarNode(p.src_line()), ((ListNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add_post!(mlhs_add_star!(mlhs_new!, Qnil), $3) %*/
  return yyVal;
};
states[106] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((Node)yyVals[-1+yyTop].value);
                    /*% %*/
                    /*% ripper: mlhs_paren!($2) %*/
  return yyVal;
};
states[107] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newArrayNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add!(mlhs_new!, $1) %*/
  return yyVal;
};
states[108] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add!($1, $2) %*/
  return yyVal;
};
states[109] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newArrayNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add!(mlhs_new!, $1) %*/
  return yyVal;
};
states[110] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add!($1, $3) %*/
  return yyVal;
};
states[111] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                   yyVal = p.assignableLabelOrIdentifier(((ByteList)yyVals[0+yyTop].value), null);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[112] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                   yyVal = new InstAsgnNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[113] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                   yyVal = new GlobalAsgnNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[114] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (p.getLexContext().in_def) p.compile_error("dynamic constant assignment");
                    yyVal = new ConstDeclNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), null, NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[115] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ClassVarAsgnNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[116] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to nil");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[117] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't change the value of self");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[118] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to true");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[119] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to false");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[120] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to __FILE__");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[121] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to __LINE__");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[122] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to __ENCODING__");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[123] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.aryset(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: aref_field!($1, escape_Qundef($3)) %*/
  return yyVal;
};
states[124] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (((ByteList)yyVals[-1+yyTop].value) == AMPERSAND_DOT) {
                        p.compile_error("&. inside multiple assignment destination");
                    }
                    /*%%%*/
                    yyVal = p.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: field!($1, $2, $3) %*/
  return yyVal;
};
states[125] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: const_path_field!($1, $3) %*/
  return yyVal;
};
states[126] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (((ByteList)yyVals[-1+yyTop].value) == AMPERSAND_DOT) {
                        p.compile_error("&. inside multiple assignment destination");
                    }
                    /*%%%*/
                    yyVal = p.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: field!($1, $2, $3) %*/
  return yyVal;
};
states[127] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (p.getLexContext().in_def) p.yyerror("dynamic constant assignment");

                    Integer position = yyVals[yyTop - count + 1].start();

                    yyVal = new ConstDeclNode(position, (RubySymbol) null, p.new_colon2(position, ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*% %*/
                    /*% ripper: const_decl(p, const_path_field!($1, $3)) %*/
  return yyVal;
};
states[128] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[129] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.backrefAssignError(((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper[error]: backref_error(p, RNODE($1), var_field(p, $1)) %*/
  return yyVal;
};
states[130] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.assignableLabelOrIdentifier(((ByteList)yyVals[0+yyTop].value), null);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
                    
  return yyVal;
};
states[131] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new InstAsgnNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[132] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new GlobalAsgnNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[133] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (p.getLexContext().in_def) p.compile_error("dynamic constant assignment");

                    yyVal = new ConstDeclNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), null, NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[134] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ClassVarAsgnNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[135] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to nil");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[136] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't change the value of self");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[137] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to true");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[138] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to false");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[139] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to __FILE__");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[140] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to __LINE__");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[141] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to __ENCODING__");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[142] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.aryset(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: aref_field!($1, escape_Qundef($3)) %*/
  return yyVal;
};
states[143] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: field!($1, $2, $3) %*/
  return yyVal;
};
states[144] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: field!($1, ID2VAL(idCOLON2), $3) %*/
  return yyVal;
};
states[145] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: field!($1, $2, $3) %*/
  return yyVal;
};
states[146] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (p.getLexContext().in_def) {
                        p.yyerror("dynamic constant assignment");
                    }

                    Integer position = yyVals[yyTop - count + 1].start();

                    yyVal = new ConstDeclNode(position, (RubySymbol) null, p.new_colon2(position, ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*% %*/
                    /*% ripper: const_decl(p, const_path_field!($1, $3)) %*/
  return yyVal;
};
states[147] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[148] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.backrefAssignError(((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper[error]: backref_error(p, RNODE($1), var_field(p, $1)) %*/
  return yyVal;
};
states[149] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "class/module name must be CONSTANT";
                    /*%%%*/
                    p.yyerror(message, yyVals[yyTop - count + 1]);
                    /*% %*/
                    /*% ripper[error]: class_name_error!(ERR_MESG(), $1) %*/
  return yyVal;
};
states[150] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[151] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_colon3(yyVals[yyTop - count + 1].start(), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: top_const_ref!($2) %*/
  return yyVal;
};
states[152] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_colon2(yyVals[yyTop - count + 1].start(), null, ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: const_ref!($1) %*/
  return yyVal;
};
states[153] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_colon2(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: const_path_ref!($1, $3) %*/
  return yyVal;
};
states[154] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[155] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[156] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[157] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   p.setState(EXPR_ENDFN);
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[158] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[159] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal =  new LiteralNode(p.src_line(), p.symbolID(((ByteList)yyVals[0+yyTop].value)));
                    /*% %*/
                    /*% ripper: symbol_literal!($1) %*/

  return yyVal;
};
states[160] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[161] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = newUndef(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_new3(1, get_value($1)) %*/
  return yyVal;
};
states[162] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_FNAME|EXPR_FITEM);
  return yyVal;
};
states[163] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.appendToBlock(((Node)yyVals[-3+yyTop].value), newUndef(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value)));
                    /*% %*/
                    /*% ripper: rb_ary_push($1, get_value($4)) %*/
  return yyVal;
};
states[164] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[165] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[166] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[167] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[168] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[169] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[170] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[171] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
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
                    yyVal = yyVals[0+yyTop].value;
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
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
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
                    /*%%%*/
                    yyVal = node_assign(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: assign!($1, $4) %*/
  return yyVal;
};
states[236] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_op_assign(((AssignableNode)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!($1, $2, $4) %*/
  return yyVal;
};
states[237] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = p.new_ary_op_assign(((Node)yyVals[-6+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!(aref_field!($1, escape_Qundef($3)), $5, $7) %*/
  return yyVal;
};
states[238] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = p.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!(field!($1, $2, $3), $4, $6) %*/
  return yyVal;
};
states[239] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = p.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!(field!($1, $2, $3), $4, $6) %*/
  return yyVal;
};
states[240] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = p.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!(field!($1, ID2VAL(idCOLON2), $3), $4, $6) %*/
  return yyVal;
};
states[241] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    Integer pos = yyVals[yyTop - count + 1].start();
                    yyVal = p.new_const_op_assign(pos, p.new_colon2(pos, ((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-3+yyTop].value)), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!(const_path_field!($1, $3), $4, $6) %*/
  return yyVal;
};
states[242] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    Integer pos = p.src_line();
                    yyVal = p.new_const_op_assign(pos, new Colon3Node(pos, p.symbolID(((ByteList)yyVals[-3+yyTop].value))), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: opassign!(top_const_field!($2), $3, $5) %*/
  return yyVal;
};
states[243] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.backrefAssignError(((Node)yyVals[-3+yyTop].value));
                    /*% %*/
                    /*% ripper[error]: backref_error(p, RNODE($1), opassign!(var_field(p, $1), $2, $4)) %*/
  return yyVal;
};
states[244] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[-2+yyTop].value));
                    p.value_expr(((Node)yyVals[0+yyTop].value));
    
                    boolean isLiteral = ((Node)yyVals[-2+yyTop].value) instanceof FixnumNode && ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), p.makeNullNil(((Node)yyVals[-2+yyTop].value)), p.makeNullNil(((Node)yyVals[0+yyTop].value)), false, isLiteral);
                    /*% %*/
                    /*% ripper: dot2!($1, $3) %*/
  return yyVal;
};
states[245] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[-2+yyTop].value));
                    p.value_expr(((Node)yyVals[0+yyTop].value));

                    boolean isLiteral = ((Node)yyVals[-2+yyTop].value) instanceof FixnumNode && ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), p.makeNullNil(((Node)yyVals[-2+yyTop].value)), p.makeNullNil(((Node)yyVals[0+yyTop].value)), true, isLiteral);
                    /*% %*/
                    /*% ripper: dot3!($1, $3) %*/
  return yyVal;
};
states[246] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[-1+yyTop].value));

                    boolean isLiteral = ((Node)yyVals[-1+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), p.makeNullNil(((Node)yyVals[-1+yyTop].value)), NilImplicitNode.NIL, false, isLiteral);
                    /*% %*/
                    /*% ripper: dot2!($1, Qnil) %*/
  return yyVal;
};
states[247] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[-1+yyTop].value));

                    boolean isLiteral = ((Node)yyVals[-1+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), p.makeNullNil(((Node)yyVals[-1+yyTop].value)), NilImplicitNode.NIL, true, isLiteral);
                    /*% %*/
                    /*% ripper: dot3!($1, Qnil) %*/
  return yyVal;
};
states[248] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), NilImplicitNode.NIL, p.makeNullNil(((Node)yyVals[0+yyTop].value)), false, isLiteral);
                    /*% %*/
                    /*% ripper: dot2!(Qnil, $2) %*/
  return yyVal;
};
states[249] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), NilImplicitNode.NIL, p.makeNullNil(((Node)yyVals[0+yyTop].value)), true, isLiteral);
                    /*% %*/
                    /*% ripper: dot3!(Qnil, $2) %*/
  return yyVal;
};
states[250] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), PLUS, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[251] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), MINUS, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[252] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), STAR, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[253] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), SLASH, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[254] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), PERCENT, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[255] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), STAR_STAR, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[256] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.call_bin_op(((NumericNode)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), p.src_line()), ((ByteList)yyVals[-3+yyTop].value));
  return yyVal;
};
states[257] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(((Node)yyVals[0+yyTop].value), PLUS_AT);
  return yyVal;
};
states[258] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(((Node)yyVals[0+yyTop].value), MINUS_AT);
  return yyVal;
};
states[259] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), OR, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[260] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), CARET, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[261] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), AMPERSAND, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[262] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), LT_EQ_RT, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[263] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[264] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), EQ_EQ, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[265] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), EQ_EQ_EQ, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[266] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), BANG_EQ, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[267] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.match_op(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[268] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), BANG_TILDE, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[269] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((Node)yyVals[0+yyTop].value)), BANG);
  return yyVal;
};
states[270] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(((Node)yyVals[0+yyTop].value), TILDE);
  return yyVal;
};
states[271] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), LT_LT, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[272] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), GT_GT, ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[273] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.logop(((Node)yyVals[-2+yyTop].value), AMPERSAND_AMPERSAND, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[274] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.logop(((Node)yyVals[-2+yyTop].value), OR_OR, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[275] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_defined = true;
  return yyVal;
};
states[276] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_defined = false;                    
                    yyVal = p.new_defined(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[277] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[-5+yyTop].value));
                    yyVal = p.new_if(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-5+yyTop].value), ((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: ifop!($1, $3, $6) %*/
  return yyVal;
};
states[278] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((DefHolder)yyVals[-3+yyTop].value));
                    p.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
                    /*%%%*/
                    yyVal = new DefnNode(((DefHolder)yyVals[-3+yyTop].value).line, ((DefHolder)yyVals[-3+yyTop].value).name, ((ArgsNode)yyVals[-2+yyTop].value), p.getCurrentScope(), p.reduce_nodes(p.remove_begin(((Node)yyVals[0+yyTop].value))), yyVals[yyTop - count + 4].end());
                    if (p.isNextBreak) ((DefnNode)yyVal).setContainsNextBreak();
                    /* Changed from MRI (combined two stmts)*/
                    /*% %*/
                    /*% ripper: def!(get_value($1), $2, bodystmt!($4, Qnil, Qnil, Qnil)) %*/
                    p.popCurrentScope();
  return yyVal;
};
states[279] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((DefHolder)yyVals[-5+yyTop].value));
                    p.restore_defun(((DefHolder)yyVals[-5+yyTop].value));
                    /*%%%*/
                    Node body = p.reduce_nodes(p.remove_begin(p.rescued_expr(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value))));
                    yyVal = new DefnNode(((DefHolder)yyVals[-5+yyTop].value).line, ((DefHolder)yyVals[-5+yyTop].value).name, ((ArgsNode)yyVals[-4+yyTop].value), p.getCurrentScope(), body, yyVals[yyTop - count + 6].end());
                    if (p.isNextBreak) ((DefnNode)yyVal).setContainsNextBreak();
                    /* Changed from MRI (combined two stmts)*/
                    /*% %*/
                    /*% ripper: def!(get_value($1), $2, bodystmt!(rescue_mod!($4, $6), Qnil, Qnil, Qnil)) %*/
                    p.popCurrentScope();
  return yyVal;
};
states[280] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((DefHolder)yyVals[-3+yyTop].value));
                    p.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
                    /*%%%*/
                    yyVal = new DefsNode(((DefHolder)yyVals[-3+yyTop].value).line, (Node) ((DefHolder)yyVals[-3+yyTop].value).singleton, ((DefHolder)yyVals[-3+yyTop].value).name, ((ArgsNode)yyVals[-2+yyTop].value), p.getCurrentScope(), p.reduce_nodes(p.remove_begin(((Node)yyVals[0+yyTop].value))), yyVals[yyTop - count + 4].end());
                    if (p.isNextBreak) ((DefsNode)yyVal).setContainsNextBreak();
                    /*% %*/
                    /*% ripper: defs!(AREF($1, 0), AREF($1, 1), AREF($1, 2), $2, bodystmt!($4, Qnil, Qnil, Qnil)) %*/
                    p.popCurrentScope();
  return yyVal;
};
states[281] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.endless_method_name(((DefHolder)yyVals[-5+yyTop].value));
                    p.restore_defun(((DefHolder)yyVals[-5+yyTop].value));
                    /*%%%*/
                    Node body = p.reduce_nodes(p.remove_begin(p.rescued_expr(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value))));
                    yyVal = new DefsNode(((DefHolder)yyVals[-5+yyTop].value).line, (Node) ((DefHolder)yyVals[-5+yyTop].value).singleton, ((DefHolder)yyVals[-5+yyTop].value).name, ((ArgsNode)yyVals[-4+yyTop].value), p.getCurrentScope(), body, yyVals[yyTop - count + 6].end());
                    if (p.isNextBreak) ((DefsNode)yyVal).setContainsNextBreak();
                    /*% %*/
                    /*% ripper: defs!(AREF($1, 0), AREF($1, 1), AREF($1, 2), $2, bodystmt!(rescue_mod!($4, $6), Qnil, Qnil, Qnil)) %*/
                    p.popCurrentScope();
  return yyVal;
};
states[282] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[283] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[284] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[285] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[286] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[287] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[288] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.warning(p.src_line(), "comparison '" + ((ByteList)yyVals[-1+yyTop].value) + "' after comparison");
                    yyVal = p.call_bin_op(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[289] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = (LexContext) p.getLexContext().clone();
  return yyVal;
};
states[290] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = (LexContext) p.getLexContext().clone();
  return yyVal;
};
states[291] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = p.makeNullNil(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[293] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[294] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.arg_append(((Node)yyVals[-3+yyTop].value), p.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
                    /*% %*/
                    /*% ripper: args_add!($1, bare_assoc_hash!($3)) %*/
  return yyVal;
};
states[295] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newArrayNode(yyVals[yyTop - count + 1].start(), p.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
                    /*% %*/
                    /*% ripper: args_add!(args_new!, bare_assoc_hash!($1)) %*/
  return yyVal;
};
states[296] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[297] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[-2+yyTop].value));
                    yyVal = p.newRescueModNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rescue_mod!($1, $3) %*/
  return yyVal;
};
states[298] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((Node)yyVals[-1+yyTop].value);
                    /*% %*/
                    /*% ripper: arg_paren!(escape_Qundef($2)) %*/
  return yyVal;
};
states[299] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[300] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[305] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[306] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.arg_append(((Node)yyVals[-3+yyTop].value), p.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
                    /*% %*/
                    /*% ripper: args_add!($1, bare_assoc_hash!($3)) %*/
  return yyVal;
};
states[307] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newArrayNode(yyVals[yyTop - count + 1].start(), p.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
                    /*% %*/
                    /*% ripper: args_add!(args_new!, bare_assoc_hash!($1)) %*/
  return yyVal;
};
states[308] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = p.newArrayNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: args_add!(args_new!, $1) %*/
  return yyVal;
};
states[309] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = arg_blk_pass(((Node)yyVals[-1+yyTop].value), ((BlockPassNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: args_add_block!($1, $2) %*/
  return yyVal;
};
states[310] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newArrayNode(yyVals[yyTop - count + 1].start(), p.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
                    yyVal = arg_blk_pass(((Node)yyVal), ((BlockPassNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: args_add_block!(args_add!(args_new!, bare_assoc_hash!($1)), $2) %*/
  return yyVal;
};
states[311] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.arg_append(((Node)yyVals[-3+yyTop].value), p.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
                    yyVal = arg_blk_pass(((Node)yyVal), ((BlockPassNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: args_add_block!(args_add!($1, bare_assoc_hash!($3)), $4) %*/
  return yyVal;
};
states[312] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*% ripper[brace]: args_add_block!(args_new!, $1) %*/
  return yyVal;
};
states[313] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[314] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[315] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new BlockPassNode(yyVals[yyTop - count + 2].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: $2 %*/
  return yyVal;
};
states[316] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (!p.local_id(FWD_BLOCK)) p.compile_error("no anonymous block parameter");
                    yyVal = new BlockPassNode(yyVals[yyTop - count + 1].start(), p.arg_var(FWD_BLOCK));
                    /* Changed from MRI*/
                    /*%
                    $$ = p.nil();
                    %*/
  return yyVal;
};
states[317] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((BlockPassNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[319] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    int line = ((Node)yyVals[0+yyTop].value) instanceof NilImplicitNode ? p.src_line() : yyVals[yyTop - count + 1].start();
                    yyVal = p.newArrayNode(line, ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: args_add!(args_new!, $1) %*/
  return yyVal;
};
states[320] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newSplatNode(((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: args_add_star!(args_new!, $2) %*/
  return yyVal;
};
states[321] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[322] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    Node node = null;

                    /* FIXME: lose syntactical elements here (and others like this)*/
                    if (((Node)yyVals[0+yyTop].value) instanceof ArrayNode &&
                        (node = p.splat_array(((Node)yyVals[-3+yyTop].value))) != null) {
                        yyVal = p.list_concat(node, ((Node)yyVals[0+yyTop].value));
                    } else {
                        yyVal = arg_concat(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    }
                    /*% %*/
                    /*% ripper: args_add_star!($1, $4) %*/
  return yyVal;
};
states[323] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[324] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[325] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[326] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[327] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newSplatNode(((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: mrhs_add_star!(mrhs_new!, $2) %*/
  return yyVal;
};
states[332] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ListNode)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[333] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ListNode)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[334] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ListNode)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[335] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ListNode)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[338] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_fcall(((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: method_add_arg!(fcall!($1), args_new!) %*/
  return yyVal;
};
states[339] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getCmdArgumentState().push0();
  return yyVal;
};
states[340] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getCmdArgumentState().pop();
                    /*%%%*/
                    yyVal = new BeginNode(yyVals[yyTop - count + 1].start(), p.makeNullNil(((Node)yyVals[-1+yyTop].value)));
                    /*% %*/
                    /*% ripper: begin!($3) %*/
  return yyVal;
};
states[341] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_ENDARG);
  return yyVal;
};
states[342] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = null; /*FIXME: Should be implicit nil?*/
                    /*% %*/
                    /*% ripper: paren!(0) %*/
  return yyVal;
};
states[343] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_ENDARG); 
  return yyVal;
};
states[344] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((Node)yyVals[-2+yyTop].value);
                    /*% %*/
                    /*% ripper: paren!($2) %*/
  return yyVal;
};
states[345] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (((Node)yyVals[-1+yyTop].value) != null) {
                        /* compstmt position includes both parens around it*/
                        ((Node)yyVals[-1+yyTop].value).setLine(((Integer)yyVals[-2+yyTop].value));
                        yyVal = ((Node)yyVals[-1+yyTop].value);
                    } else {
                        yyVal = new NilNode(((Integer)yyVals[-2+yyTop].value));
                    }
                    /*% %*/
                    /*% ripper: paren!($2) %*/
  return yyVal;
};
states[346] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_colon2(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: const_path_ref!($1, $3) %*/
  return yyVal;
};
states[347] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_colon3(yyVals[yyTop - count + 1].start(), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: top_const_ref!($2) %*/
  return yyVal;
};
states[348] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    Integer position = yyVals[yyTop - count + 2].start();
                    if (((Node)yyVals[-1+yyTop].value) == null) {
                        yyVal = new ZArrayNode(position); /* zero length array */
                    } else {
                        yyVal = ((Node)yyVals[-1+yyTop].value);
                    }
                    /*% %*/
                    /*% ripper: array!(escape_Qundef($2)) %*/
  return yyVal;
};
states[349] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((HashNode)yyVals[-1+yyTop].value);
                    ((HashNode)yyVal).setIsLiteral();
                    /*% %*/
                    /*% ripper: hash!(escape_Qundef($2)) %*/
  return yyVal;
};
states[350] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ReturnNode(yyVals[yyTop - count + 1].start(), NilImplicitNode.NIL);
                    /*% %*/
                    /*% ripper: return0! %*/
  return yyVal;
};
states[351] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_yield(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: yield!(paren!($3)) %*/
  return yyVal;
};
states[352] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new YieldNode(yyVals[yyTop - count + 1].start(), null);
                    /*% %*/
                    /*% ripper: yield!(paren!(args_new!)) %*/
  return yyVal;
};
states[353] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new YieldNode(yyVals[yyTop - count + 1].start(), null);
                    /*% %*/
                    /*% ripper: yield0! %*/
  return yyVal;
};
states[354] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_defined = true;
  return yyVal;
};
states[355] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_defined = false;
                    yyVal = p.new_defined(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[356] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((Node)yyVals[-1+yyTop].value)), NOT);
  return yyVal;
};
states[357] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(p.nil()), NOT);
  return yyVal;
};
states[358] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.frobnicate_fcall_args(((FCallNode)yyVals[-1+yyTop].value), null, ((IterNode)yyVals[0+yyTop].value));
                    yyVal = ((FCallNode)yyVals[-1+yyTop].value);                    
                    /*% %*/
                    /*% ripper: method_add_block!(method_add_arg!(fcall!($1), args_new!), $2) %*/
  return yyVal;
};
states[360] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (((Node)yyVals[-1+yyTop].value) != null && 
                          ((BlockAcceptingNode)yyVals[-1+yyTop].value).getIterNode() instanceof BlockPassNode) {
                          p.compile_error("Both block arg and actual block given.");
                    }
                    yyVal = ((BlockAcceptingNode)yyVals[-1+yyTop].value).setIterNode(((IterNode)yyVals[0+yyTop].value));
                    ((Node)yyVal).setLine(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: method_add_block!($1, $2) %*/
  return yyVal;
};
states[361] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((LambdaNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[362] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_if(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-4+yyTop].value), ((Node)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: if!($2, $4, escape_Qundef($5)) %*/
  return yyVal;
};
states[363] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_if(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-4+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[-2+yyTop].value));
                    /*% %*/
                    /*% ripper: unless!($2, $4, escape_Qundef($5)) %*/
  return yyVal;
};
states[364] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new WhileNode(yyVals[yyTop - count + 1].start(), p.cond(((Node)yyVals[-2+yyTop].value)), p.makeNullNil(((Node)yyVals[-1+yyTop].value)));
                    /*% %*/
                    /*% ripper: while!($2, $3) %*/
  return yyVal;
};
states[365] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new UntilNode(yyVals[yyTop - count + 1].start(), p.cond(((Node)yyVals[-2+yyTop].value)), p.makeNullNil(((Node)yyVals[-1+yyTop].value)));
                    /*% %*/
                    /*% ripper: until!($2, $3) %*/
  return yyVal;
};
states[366] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.case_labels;
                    p.case_labels = p.getRuntime().getNil();
  return yyVal;
};
states[367] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newCaseNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-4+yyTop].value), ((Node)yyVals[-1+yyTop].value));
                    p.fixpos(((Node)yyVal), ((Node)yyVals[-4+yyTop].value));
                    /*% %*/
                    /*% ripper: case!($2, $5) %*/
  return yyVal;
};
states[368] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.case_labels;
                    p.case_labels = null;
  return yyVal;
};
states[369] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newCaseNode(yyVals[yyTop - count + 1].start(), null, ((Node)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: case!(Qnil, $4) %*/
  return yyVal;
};
states[370] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newPatternCaseNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-3+yyTop].value), ((InNode)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: case!($2, $4) %*/
  return yyVal;
};
states[371] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ForNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-4+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[-2+yyTop].value), p.getCurrentScope(), 111);
                    /*% %*/
                    /*% ripper: for!($2, $4, $5) %*/
  return yyVal;
};
states[372] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = p.getLexContext();
                    if (ctxt.in_def) {
                        p.yyerror("class definition in method body");
                    }
                    ctxt.in_class = true;
                    p.pushLocalScope();
  return yyVal;
};
states[373] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    Node body = p.makeNullNil(((Node)yyVals[-1+yyTop].value));

                    yyVal = new ClassNode(yyVals[yyTop - count + 1].start(), ((Colon3Node)yyVals[-4+yyTop].value), p.getCurrentScope(), body, ((Node)yyVals[-3+yyTop].value), p.src_line());
                    /*% %*/
                    /*% ripper: class!($2, $3, $5) %*/
                    LexContext ctxt = p.getLexContext();
                    p.popCurrentScope();
                    ctxt.in_class = ((LexContext)yyVals[-5+yyTop].value).in_class;
                    ctxt.shareable_constant_value = ((LexContext)yyVals[-5+yyTop].value).shareable_constant_value;
  return yyVal;
};
states[374] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_def = false;
                    ctxt.in_class = false;
                    p.pushLocalScope();
  return yyVal;
};
states[375] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    Node body = p.makeNullNil(((Node)yyVals[-1+yyTop].value));

                    yyVal = new SClassNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-4+yyTop].value), p.getCurrentScope(), body, p.src_line());
                    /*% %*/
                    /*% ripper: sclass!($3, $6) %*/
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_def = ((LexContext)yyVals[-6+yyTop].value).in_def;
                    ctxt.in_class = ((LexContext)yyVals[-6+yyTop].value).in_class;
                    ctxt.shareable_constant_value = ((LexContext)yyVals[-6+yyTop].value).shareable_constant_value;
                    p.popCurrentScope();
  return yyVal;
};
states[376] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = p.getLexContext();
                    if (ctxt.in_def) { 
                        p.yyerror("module definition in method body");
                    }
                    ctxt.in_class = true;
                    p.pushLocalScope();
  return yyVal;
};
states[377] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    Node body = p.makeNullNil(((Node)yyVals[-1+yyTop].value));

                    yyVal = new ModuleNode(yyVals[yyTop - count + 1].start(), ((Colon3Node)yyVals[-3+yyTop].value), p.getCurrentScope(), body, p.src_line());
                    /*% %*/
                    /*% ripper: module!($2, $4) %*/
                    p.popCurrentScope();
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_class = ((LexContext)yyVals[-4+yyTop].value).in_class;
                    ctxt.shareable_constant_value = ((LexContext)yyVals[-4+yyTop].value).shareable_constant_value;
  return yyVal;
};
states[378] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
                    /*%%%*/
                    Node body = p.reduce_nodes(p.remove_begin(p.makeNullNil(((Node)yyVals[-1+yyTop].value))));
                    yyVal = new DefnNode(((DefHolder)yyVals[-3+yyTop].value).line, ((DefHolder)yyVals[-3+yyTop].value).name, ((ArgsNode)yyVals[-2+yyTop].value), p.getCurrentScope(), body, yyVals[yyTop - count + 4].end());
                    if (p.isNextBreak) ((DefnNode)yyVal).setContainsNextBreak();
                    /*% %*/
                    /*% ripper: def!(get_value($1), $2, $3) %*/
                    p.popCurrentScope();
  return yyVal;
};
states[379] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
                    /*%%%*/
                    Node body = p.reduce_nodes(p.remove_begin(p.makeNullNil(((Node)yyVals[-1+yyTop].value))));
                    yyVal = new DefsNode(((DefHolder)yyVals[-3+yyTop].value).line, (Node) ((DefHolder)yyVals[-3+yyTop].value).singleton, ((DefHolder)yyVals[-3+yyTop].value).name, ((ArgsNode)yyVals[-2+yyTop].value), p.getCurrentScope(), body, yyVals[yyTop - count + 4].end());
                    if (p.isNextBreak) ((DefsNode)yyVal).setContainsNextBreak();
                    /* Changed from MRI (no more get_value)*/
                    /*% %*/                    
                    /*% ripper: defs!(AREF($1, 0), AREF($1, 1), AREF($1, 2), $2, $3) %*/
                    p.popCurrentScope();
  return yyVal;
};
states[380] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.isNextBreak = true;
                    yyVal = new BreakNode(yyVals[yyTop - count + 1].start(), NilImplicitNode.NIL);
                    /*% %*/
                    /*% ripper: break!(args_new!) %*/
  return yyVal;
};
states[381] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.isNextBreak = true;
                    yyVal = new NextNode(yyVals[yyTop - count + 1].start(), NilImplicitNode.NIL);
                    /*% %*/
                    /*% ripper: next!(args_new!) %*/
  return yyVal;
};
states[382] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new RedoNode(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: redo! %*/
  return yyVal;
};
states[383] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new RetryNode(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: retry! %*/
  return yyVal;
};
states[384] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = ((Node)yyVals[0+yyTop].value);
                    if (yyVal == null) yyVal = p.nil();
  return yyVal;
};
states[385] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[386] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[387] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[388] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[389] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[390] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[391] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[392] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = (LexContext) p.getLexContext().clone();
  return yyVal;
};
states[393] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = (LexContext) p.getLexContext().clone();  
  return yyVal;
};
states[394] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
                    p.getLexContext().in_argdef = true;
  return yyVal;
};
states[395] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[396] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[397] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[398] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[399] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[400] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[401] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[402] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[403] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = p.getLexContext();
                    if (ctxt.in_class && !ctxt.in_def && !p.getCurrentScope().isBlockScope()) {
                        p.compile_error("Invalid return in class/module body");
                    }
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[410] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_if(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: elsif!($2, $4, escape_Qundef($5)) %*/
  return yyVal;
};
states[412] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((Node)yyVals[0+yyTop].value) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop].value);
                    /*% %*/
                    /*% ripper: else!($2) %*/
  return yyVal;
};
states[414] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  return yyVal;
};
states[415] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.assignableInCurr(((ByteList)yyVals[0+yyTop].value), NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, $1);
                    %*/
  return yyVal;
};
states[416] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((Node)yyVals[-1+yyTop].value);
                    /*% %*/
                    /*% ripper: mlhs_paren!($2) %*/
  return yyVal;
};
states[417] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newArrayNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add!(mlhs_new!, $1) %*/
  return yyVal;
};
states[418] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add!($1, $3) %*/
  return yyVal;
};
states[419] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[0+yyTop].value), null, null);
                    /*% %*/
                    /*% ripper: $1 %*/
  return yyVal;
};
states[420] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
                    /*% %*/
                    /*% ripper: mlhs_add_star!($1, $3) %*/
  return yyVal;
};
states[421] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-4+yyTop].value), ((Node)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add_post!(mlhs_add_star!($1, $3), $5) %*/
  return yyVal;
};
states[422] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(p.src_line(), null, ((Node)yyVals[0+yyTop].value), null);
                    /*% %*/
                    /*% ripper: mlhs_add_star!(mlhs_new!, $1) %*/
  return yyVal;
};
states[423] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new MultipleAsgnNode(p.src_line(), null, ((Node)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: mlhs_add_post!(mlhs_add_star!(mlhs_new!, $1), $3) %*/
  return yyVal;
};
states[424] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    ByteList id = yyVals[yyTop - count + 2].id;
                    /*%%%*/
                    yyVal = p.assignableInCurr(id, null);
                    /*%
                      $$ = p.assignable(id, $2);
                    %*/
  return yyVal;
};
states[425] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new StarNode(p.src_line());
                    /*% %*/
                    /*% ripper: Qnil %*/
  return yyVal;
};
states[427] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
                    /*%
                      $$ = p.symbolID(LexingCommon.NIL);
                      %*/
  return yyVal;
};
states[428] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = false;
  return yyVal;
};
states[430] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-3+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[431] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-1+yyTop].value), (ByteList) null, ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[432] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(p.src_line(), null, ((ByteList)yyVals[-1+yyTop].value), ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[433] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), null, (ByteList) null, ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[434] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[435] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(p.src_line(), null, (ByteList) null, (ByteList) null);
  return yyVal;
};
states[436] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new UnnamedRestArgNode(yyVals[yyTop - count + 1].start(), null, p.getCurrentScope().addVariable("*"));
                    /*% %*/
                    /*% ripper: excessed_comma! %*/
  return yyVal;
};
states[437] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-5+yyTop].value), ((ListNode)yyVals[-3+yyTop].value), ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[438] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-7+yyTop].value), ((ListNode)yyVals[-5+yyTop].value), ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[439] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[440] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-5+yyTop].value), ((ListNode)yyVals[-3+yyTop].value), null, ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[441] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-3+yyTop].value), null, ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[442] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-1+yyTop].value), null, ((RestArgNode)yyVals[0+yyTop].value), null, (ArgsTailHolder) null);
  return yyVal;
};
states[443] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-5+yyTop].value), null, ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[444] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-1+yyTop].value), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[445] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((ListNode)yyVals[-3+yyTop].value), ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[446] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((ListNode)yyVals[-5+yyTop].value), ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[447] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((ListNode)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[448] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((ListNode)yyVals[-3+yyTop].value), null, ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[449] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[450] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[451] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[452] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(p.src_line(), null, null, null, null, (ArgsTailHolder) null);
  return yyVal;
};
states[453] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setCommandStart(true);
                    yyVal = ((ArgsNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[454] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setCurrentArg(null);
                    p.ordinalMaxNumParam();
                    p.getLexContext().in_argdef = false;
                    /*%%%*/
                    yyVal = p.new_args(p.src_line(), null, null, null, null, (ArgsTailHolder) null);
                    /*% %*/
                    /*% ripper: block_var!(params!(Qnil,Qnil,Qnil,Qnil,Qnil,Qnil,Qnil), escape_Qundef($2)) %*/
  return yyVal;
};
states[455] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setCurrentArg(null);
                    p.ordinalMaxNumParam();
                    p.getLexContext().in_argdef = false;
                    /*%%%*/
                    yyVal = ((ArgsNode)yyVals[-2+yyTop].value);
                    /*% %*/
                    /*% ripper: block_var!(escape_Qundef($2), escape_Qundef($3)) %*/
  return yyVal;
};
states[456] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[457] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = null;
                    /*% %*/
                    /*% ripper: $3 %*/
  return yyVal;
};
states[458] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
                    /*% ripper[brace]: rb_ary_new3(1, get_value($1)) %*/
  return yyVal;
};
states[459] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
                    /*% ripper[brace]: rb_ary_push($1, get_value($3)) %*/
  return yyVal;
};
states[460] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.new_bv(yyVals[yyTop - count + 1].id);
                    /*% ripper: get_value($1) %*/
  return yyVal;
};
states[461] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[462] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pushBlockScope();
                    yyVal = p.getLeftParenBegin();
                    p.setLeftParenBegin(p.getParenNest());
  return yyVal;
};
states[463] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.resetMaxNumParam();
  return yyVal;
};
states[464] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.numparam_push();
  return yyVal;
};
states[465] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getCmdArgumentState().push0();
  return yyVal;
};
states[466] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    int max_numparam = p.restoreMaxNumParam(((Integer)yyVals[-4+yyTop].value));
                    p.getCmdArgumentState().pop();
                    /* Changed from MRI args_with_numbered put into parser codepath and not used by ripper (since it is just a passthrough method and types do not match).*/
                    /*%%%*/
                    ArgsNode args = p.args_with_numbered(((ArgsNode)yyVals[-2+yyTop].value), max_numparam);
                    yyVal = new LambdaNode(yyVals[yyTop - count + 1].start(), args, ((Node)yyVals[0+yyTop].value), p.getCurrentScope(), p.src_line());
                    /*% %*/
                    /*% ripper: lambda!($5, $7) %*/
                    p.setLeftParenBegin(((Integer)yyVals[-5+yyTop].value));
                    p.numparam_pop(((Node)yyVals[-3+yyTop].value));
                    p.popCurrentScope();
  return yyVal;
};
states[467] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = false;
                    /*%%%*/
                    yyVal = ((ArgsNode)yyVals[-2+yyTop].value);
                    p.ordinalMaxNumParam();
                    /*% %*/
                    /*% ripper: paren!($2) %*/
  return yyVal;
};
states[468] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = false;
                    /*%%%*/
                    if (!p.isArgsInfoEmpty(((ArgsNode)yyVals[0+yyTop].value))) {
                        p.ordinalMaxNumParam();
                    }
                    /*% %*/
                    yyVal = ((ArgsNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[469] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[470] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[471] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IterNode)yyVals[-1+yyTop].value);
                    /*%%%*/
                    /*% %*/
  return yyVal;
};
states[472] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /* Workaround for JRUBY-2326 (MRI does not enter this production for some reason)*/
                    /*%%%*/
                    if (((Node)yyVals[-1+yyTop].value) instanceof YieldNode) {
                        p.compile_error("block given to yield");
                    }
                    if (((Node)yyVals[-1+yyTop].value) instanceof BlockAcceptingNode && ((BlockAcceptingNode)yyVals[-1+yyTop].value).getIterNode() instanceof BlockPassNode) {
                        p.compile_error("Both block arg and actual block given.");
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
states[473] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].start());
                    /*% %*/
                    /*% ripper: opt_event(:method_add_arg!, call!($1, $2, $3), $4) %*/
  return yyVal;
};
states[474] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value), yyVals[yyTop - count + 3].start());
                    /*% %*/
                    /*% ripper: opt_event(:method_add_block!, command_call!($1, $2, $3, $4), $5) %*/
  return yyVal;
};
states[475] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value), yyVals[yyTop - count + 3].start());
                    /*% %*/
                    /*% ripper: method_add_block!(command_call!($1, $2, $3, $4), $5) %*/
  return yyVal;
};
states[476] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.frobnicate_fcall_args(((FCallNode)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
                    yyVal = ((FCallNode)yyVals[-1+yyTop].value);
                    /*% %*/
                    /*% ripper: method_add_arg!(fcall!($1), $2) %*/
  return yyVal;
};
states[477] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].start());
                    /*% %*/
                    /*% ripper: opt_event(:method_add_arg!, call!($1, $2, $3), $4) %*/
  return yyVal;
};
states[478] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
                    /*% %*/
                    /*% ripper: method_add_arg!(call!($1, ID2VAL(idCOLON2), $3), $4) %*/
  return yyVal;
};
states[479] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value), null, null);
                    /*% %*/
                    /*% ripper: call!($1, ID2VAL(idCOLON2), $3) %*/
  return yyVal;
};
states[480] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), LexingCommon.CALL, ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].start());
                    /*% %*/
                    /*% ripper: method_add_arg!(call!($1, $2, ID2VAL(idCall)), $3) %*/
  return yyVal;
};
states[481] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_call(((Node)yyVals[-2+yyTop].value), LexingCommon.CALL, ((Node)yyVals[0+yyTop].value), null);
                    /*% %*/
                    /*% ripper: method_add_arg!(call!($1, ID2VAL(idCOLON2), ID2VAL(idCall)), $3) %*/
  return yyVal;
};
states[482] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_super(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: super!($2) %*/
  return yyVal;
};
states[483] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ZSuperNode(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: zsuper! %*/
  return yyVal;
};
states[484] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[485] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IterNode)yyVals[-1+yyTop].value);
                    /*%%%*/
                    ((IterNode)yyVals[-1+yyTop].value).setLine(yyVals[yyTop - count + 1].end());
                    /*% %*/
  return yyVal;
};
states[486] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IterNode)yyVals[-1+yyTop].value);
                    /*%%%*/
                    ((IterNode)yyVals[-1+yyTop].value).setLine(yyVals[yyTop - count + 1].end());
                    /*% %*/
  return yyVal;
};
states[487] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pushBlockScope();
  return yyVal;
};
states[488] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.resetMaxNumParam();
  return yyVal;
};
states[489] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.numparam_push();
  return yyVal;
};
states[490] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    int max_numparam = p.restoreMaxNumParam(((Integer)yyVals[-3+yyTop].value));
                    /* Changed from MRI args_with_numbered put into parser codepath and not used by ripper (since it is just a passthrough method and types do not match).*/
                    /*%%%*/
                    ArgsNode args = p.args_with_numbered(((ArgsNode)yyVals[-1+yyTop].value), max_numparam);
                    yyVal = new IterNode(yyVals[yyTop - count + 1].start(), args, ((Node)yyVals[0+yyTop].value), p.getCurrentScope(), p.src_line());
                    /*% %*/
                    /*% ripper: brace_block!(escape_Qundef($4), $5) %*/
                    p.numparam_pop(((Node)yyVals[-2+yyTop].value));
                    p.popCurrentScope();                    
  return yyVal;
};
states[491] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pushBlockScope();
  return yyVal;
};
states[492] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.resetMaxNumParam();
  return yyVal;
};
states[493] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.numparam_push();
                    p.getCmdArgumentState().push0();
  return yyVal;
};
states[494] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    int max_numparam = p.restoreMaxNumParam(((Integer)yyVals[-3+yyTop].value));
                    /* Changed from MRI args_with_numbered put into parser codepath and not used by ripper (since it is just a passthrough method and types do not match).*/
                    /*%%%*/
                    ArgsNode args = p.args_with_numbered(((ArgsNode)yyVals[-1+yyTop].value), max_numparam);
                    yyVal = new IterNode(yyVals[yyTop - count + 1].start(), args, ((Node)yyVals[0+yyTop].value), p.getCurrentScope(), p.src_line());
                    /*% %*/
                    /*% ripper: do_block!(escape_Qundef($4), $5) %*/
                    p.getCmdArgumentState().pop();
                    p.numparam_pop(((Node)yyVals[-2+yyTop].value));
                    p.popCurrentScope();
  return yyVal;
};
states[495] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.check_literal_when(((Node)yyVals[0+yyTop].value));
                    yyVal = p.newArrayNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: args_add!(args_new!, $1) %*/
  return yyVal;
};
states[496] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newSplatNode(((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: args_add_star!(args_new!, $2) %*/
  return yyVal;
};
states[497] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.check_literal_when(((Node)yyVals[0+yyTop].value));
                    yyVal = p.last_arg_append(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: args_add!($1, $3) %*/
  return yyVal;
};
states[498] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.rest_arg_append(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: args_add_star!($1, $4) %*/
  return yyVal;
};
states[499] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newWhenNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: when!($2, $4, escape_Qundef($5)) %*/
  return yyVal;
};
states[502] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_BEG|EXPR_LABEL);
                    p.setCommandStart(false);
                    LexContext ctxt = (LexContext) p.getLexContext();
                    yyVals[0+yyTop].value = ctxt.in_kwarg;
                    ctxt.in_kwarg = true;
                    yyVal = p.push_pvtbl();
  return yyVal;
};
states[503] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.push_pktbl();
  return yyVal;
};
states[504] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    p.pop_pvtbl(((Set)yyVals[-3+yyTop].value));
                    p.getLexContext().in_kwarg = ((Boolean)yyVals[-4+yyTop].value);
  return yyVal;
};
states[505] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newIn(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-4+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: in!($4, $7, escape_Qundef($8)) %*/
  return yyVal;
};
states[507] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((InNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[509] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_if(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value), ((Node)yyVals[-2+yyTop].value), null);
                    p.fixpos(((Node)yyVal), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: if_mod!($3, $1) %*/
  return yyVal;
};
states[510] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_if(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value), null, ((Node)yyVals[-2+yyTop].value));
                    p.fixpos(((Node)yyVal), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: unless_mod!($3, $1) %*/
  return yyVal;
};
states[512] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, ((Node)yyVals[-1+yyTop].value),
                                                   p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, true, null, null));
  return yyVal;
};
states[513] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, ((Node)yyVals[-2+yyTop].value), ((ArrayPatternNode)yyVals[0+yyTop].value));
                    /*%%%*/
                    p.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].start());
                    /*% %*/
  return yyVal;
};
states[514] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_find_pattern(null, ((FindPatternNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[515] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, null, ((ArrayPatternNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[516] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern(null, ((HashPatternNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[518] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new HashNode(yyVals[yyTop - count + 1].start(), new KeyValuePair(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value)));
                    /*% %*/
                    /*% ripper: binary!($1, STATIC_ID2SYM((id_assoc)), $3) %*/
  return yyVal;
};
states[520] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.logop(((Node)yyVals[-2+yyTop].value), OR_OR, ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: binary!($1, STATIC_ID2SYM(idOr), $3) %*/
  return yyVal;
};
states[522] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.push_pktbl();
  return yyVal;
};
states[523] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.push_pktbl();
  return yyVal;
};
states[526] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-3+yyTop].value), null, ((ArrayPatternNode)yyVals[-1+yyTop].value));
                    /*%%%*/
                    p.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].start());
                    /*% %*/
  return yyVal;
};
states[527] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_find_pattern(((Node)yyVals[-3+yyTop].value), ((FindPatternNode)yyVals[-1+yyTop].value));
                    /*%%%*/
                    p.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].start());
                    /*% %*/
  return yyVal;
};
states[528] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_hash_pattern(((Node)yyVals[-3+yyTop].value), ((HashPatternNode)yyVals[-1+yyTop].value));
                    /*%%%*/
                    p.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].start());
                    /*% %*/
  return yyVal;
};
states[529] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-2+yyTop].value), null,
                                                    p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, false, null, null));
  return yyVal;
};
states[530] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-3+yyTop].value), null, ((ArrayPatternNode)yyVals[-1+yyTop].value));
                    /*%%%*/
                    p.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].start());
                    /*% %*/
  return yyVal;
};
states[531] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_find_pattern(((Node)yyVals[-3+yyTop].value), ((FindPatternNode)yyVals[-1+yyTop].value));
                    /*%%%*/
                    p.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].start());
                    /*% %*/
  return yyVal;
};
states[532] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_hash_pattern(((Node)yyVals[-3+yyTop].value), ((HashPatternNode)yyVals[-1+yyTop].value));
                    /*%%%*/
                    p.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].start());
                    /*% %*/
  return yyVal;
};
states[533] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-2+yyTop].value), null,
                            p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, false, null, null));
  return yyVal;
};
states[534] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, null, ((ArrayPatternNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[535] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_find_pattern(null, ((FindPatternNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[536] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, null,
                            p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, false, null, null));
  return yyVal;
};
states[537] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.push_pktbl();
                    LexContext ctxt = p.getLexContext();
                    yyVals[0+yyTop].value = ctxt.in_kwarg;
                    ctxt.in_kwarg = false;
  return yyVal;
};
states[538] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    p.getLexContext().in_kwarg = ((Boolean)yyVals[-3+yyTop].value);
                    yyVal = p.new_hash_pattern(null, ((HashPatternNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[539] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern(null, p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), p.none(), null));
  return yyVal;
};
states[540] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.push_pktbl();
  return yyVal;
};
states[541] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[542] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    ListNode preArgs = p.newArrayNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), preArgs, false, null, null);
                    /* JRuby Changed*/
                    /*% 
                        $$ = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), p.new_array($1), false, null, null);
                    %*/
  return yyVal;
};
states[543] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[0+yyTop].value), true, null, null);
  return yyVal;
};
states[544] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), p.list_concat(((ListNode)yyVals[-1+yyTop].value), ((ListNode)yyVals[0+yyTop].value)), false, null, null);
                    /* JRuby Changed*/
                    /*%
			RubyArray pre_args = $1.concat($2);
			$$ = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), pre_args, false, null, null);
                    %*/
  return yyVal;
};
states[545] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-2+yyTop].value), true, ((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[546] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-4+yyTop].value), true, ((ByteList)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[547] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-1+yyTop].value), true, null, null);
  return yyVal;
};
states[548] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-3+yyTop].value), true, null, ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[549] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ArrayPatternNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[550] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ListNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[551] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.list_concat(((ListNode)yyVals[-2+yyTop].value), ((ListNode)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_concat($1, get_value($2)) %*/
  return yyVal;
};
states[552] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, true, ((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[553] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, true, ((ByteList)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[554] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     p.warn_experimental(yyVals[yyTop - count + 1].start(), "Find pattern is experimental, and the behavior may change in future versions of Ruby!");
                     yyVal = p.new_find_pattern_tail(yyVals[yyTop - count + 1].start(), ((ByteList)yyVals[-4+yyTop].value), ((ListNode)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[555] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[556] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[558] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.list_concat(((ListNode)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_concat($1, get_value($3)) %*/
  return yyVal;
};
states[559] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newArrayNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_new_from_args(1, get_value($1)) %*/
  return yyVal;
};
states[560] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), ((HashNode)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[561] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), ((HashNode)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[562] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), ((HashNode)yyVals[-1+yyTop].value), null);
  return yyVal;
};
states[563] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), null, ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[564] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new HashNode(yyVals[yyTop - count + 1].start(), ((KeyValuePair)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper[brace]: rb_ary_new_from_args(1, $1) %*/
  return yyVal;
};
states[565] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    ((HashNode)yyVals[-2+yyTop].value).add(((KeyValuePair)yyVals[0+yyTop].value));
                    yyVal = ((HashNode)yyVals[-2+yyTop].value);
                    /*% %*/
                    /*% ripper: rb_ary_push($1, $3) %*/
  return yyVal;
};
states[566] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.error_duplicate_pattern_key(yyVals[yyTop - count + 1].id);
                    /*%%%*/
                    Node label = p.asSymbol(yyVals[yyTop - count + 1].start(), ((ByteList)yyVals[-1+yyTop].value));

                    yyVal = new KeyValuePair(label, ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_new_from_args(2, get_value($1), get_value($2)) %*/
  return yyVal;
};
states[567] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[569] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[570] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[571] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[572] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                       yyVal = KWNOREST;
                    /*%
                       $$ = null;
                    %*/
  return yyVal;
};
states[573] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[574] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[576] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[-2+yyTop].value));
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[-2+yyTop].value) instanceof FixnumNode && ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), p.makeNullNil(((Node)yyVals[-2+yyTop].value)), p.makeNullNil(((Node)yyVals[0+yyTop].value)), false, isLiteral);
                    /*% %*/
                    /*% ripper: dot2!($1, $3) %*/
  return yyVal;
};
states[577] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[-2+yyTop].value));
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[-2+yyTop].value) instanceof FixnumNode && ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), p.makeNullNil(((Node)yyVals[-2+yyTop].value)), p.makeNullNil(((Node)yyVals[0+yyTop].value)), true, isLiteral);
                    /*% %*/
                    /*% ripper: dot3!($1, $3) %*/
  return yyVal;
};
states[578] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[-1+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[-1+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), p.makeNullNil(((Node)yyVals[-1+yyTop].value)), NilImplicitNode.NIL, false, isLiteral);
                    /*% %*/
                    /*% ripper: dot2!($1, Qnil) %*/
  return yyVal;
};
states[579] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[-1+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[-1+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), p.makeNullNil(((Node)yyVals[-1+yyTop].value)), NilImplicitNode.NIL, true, isLiteral);
                    /*% %*/
                    /*% ripper: dot3!($1, Qnil) %*/
  return yyVal;
};
states[583] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), NilImplicitNode.NIL, p.makeNullNil(((Node)yyVals[0+yyTop].value)), false, isLiteral);
                    /*% %*/
                    /*% ripper: dot2!(Qnil, $2) %*/
  return yyVal;
};
states[584] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].start(), NilImplicitNode.NIL, p.makeNullNil(((Node)yyVals[0+yyTop].value)), true, isLiteral);
                    /*% %*/
                    /*% ripper: dot3!(Qnil, $2) %*/
  return yyVal;
};
states[589] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[590] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[591] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[592] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[593] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new NilNode(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[594] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new SelfNode(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[595] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new TrueNode(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[596] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new FalseNode(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[597] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new FileNode(yyVals[yyTop - count + 1].start(), new ByteList(p.getFile().getBytes(),
                    p.getRuntime().getEncodingService().getLocaleEncoding()));
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[598] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new FixnumNode(yyVals[yyTop - count + 1].start(), yyVals[yyTop - count + 1].start()+1);
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[599] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new EncodingNode(yyVals[yyTop - count + 1].start(), p.getEncoding());
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[600] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((LambdaNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[601] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.error_duplicate_pattern_variable(((ByteList)yyVals[0+yyTop].value));
                    yyVal = p.assignableInCurr(((ByteList)yyVals[0+yyTop].value), null);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[602] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[603] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.gettable(((ByteList)yyVals[0+yyTop].value));
                    if (yyVal == null) yyVal = new BeginNode(yyVals[yyTop - count + 1].start(), NilImplicitNode.NIL);
                    /*% %*/
                    /*% ripper: var_ref!($2) %*/
  return yyVal;
};
states[604] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new BeginNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: begin!($3) %*/
  return yyVal;
};
states[605] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_colon3(yyVals[yyTop - count + 1].start(), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: top_const_ref!($2) %*/
  return yyVal;
};
states[606] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.new_colon2(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: const_path_ref!($1, $3) %*/
  return yyVal;
};
states[607] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ConstNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)));
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[608] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    Node node;
                    if (((Node)yyVals[-3+yyTop].value) != null) {
                        node = p.appendToBlock(node_assign(((Node)yyVals[-3+yyTop].value), new GlobalVarNode(yyVals[yyTop - count + 1].start(), p.symbolID(DOLLAR_BANG))), p.makeNullNil(((Node)yyVals[-1+yyTop].value)));
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
states[609] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null; 
  return yyVal;
};
states[610] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.newArrayNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_new3(1, get_value($1)) %*/
  return yyVal;
};
states[611] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.splat_array(((Node)yyVals[0+yyTop].value));
                    if (yyVal == null) yyVal = ((Node)yyVals[0+yyTop].value); /* ArgsCat or ArgsPush*/
                    /*% %*/
                    /*% ripper: $1 %*/
  return yyVal;
};
states[613] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[615] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((Node)yyVals[0+yyTop].value);
                    /*% %*/
                    /*% ripper: ensure!($2) %*/
  return yyVal;
};
states[617] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((NumericNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[618] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[619] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((Node)yyVals[0+yyTop].value) instanceof EvStrNode ? new DStrNode(yyVals[yyTop - count + 1].start(), p.getEncoding()).add(((Node)yyVals[0+yyTop].value)) : ((Node)yyVals[0+yyTop].value);
                    /*% %*/
                    /*% ripper: $1 %*/
  return yyVal;
};
states[620] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((StrNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[621] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[622] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: string_concat!($1, $2) %*/
  return yyVal;
};
states[623] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.heredoc_dedent(((Node)yyVals[-1+yyTop].value));
                    yyVal = ((Node)yyVals[-1+yyTop].value);
                    /*% %*/
                    /*% ripper: string_literal!(heredoc_dedent(p, $2)) %*/
  return yyVal;
};
states[624] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    int line = yyVals[yyTop - count + 2].start();

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
states[625] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_regexp(yyVals[yyTop - count + 2].start(), ((Node)yyVals[-1+yyTop].value), ((RegexpNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[626] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ListNode)yyVals[-1+yyTop].value);
                    /*% %*/
                    /*% ripper: array!($3) %*/
  return yyVal;
};
states[627] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                     yyVal = new ArrayNode(p.src_line());
                    /*% %*/
                    /*% ripper: words_new! %*/
  return yyVal;
};
states[628] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                     yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[-1+yyTop].value) instanceof EvStrNode ? new DStrNode(yyVals[yyTop - count + 1].start(), p.getEncoding()).add(((Node)yyVals[-1+yyTop].value)) : ((Node)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: words_add!($1, $2) %*/
  return yyVal;
};
states[629] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((Node)yyVals[0+yyTop].value);
                     /*% ripper[brace]: word_add!(word_new!, $1) %*/
  return yyVal;
};
states[630] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: word_add!($1, $2) %*/
  return yyVal;
};
states[631] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ListNode)yyVals[-1+yyTop].value);
                    /*% %*/
                    /*% ripper: array!($3) %*/
  return yyVal;
};
states[632] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ArrayNode(p.src_line());
                    /*% %*/
                    /*% ripper: symbols_new! %*/
  return yyVal;
};
states[633] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[-1+yyTop].value) instanceof EvStrNode ? new DSymbolNode(yyVals[yyTop - count + 1].start()).add(((Node)yyVals[-1+yyTop].value)) : p.asSymbol(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-1+yyTop].value)));
                    /*% %*/
                    /*% ripper: symbols_add!($1, $2) %*/
  return yyVal;
};
states[634] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ListNode)yyVals[-1+yyTop].value);
                    /*% %*/
                    /*% ripper: array!($3) %*/
  return yyVal;
};
states[635] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ListNode)yyVals[-1+yyTop].value);
                    /*% %*/
                    /*% ripper: array!($3) %*/
  return yyVal;
};
states[636] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ArrayNode(p.src_line());
                    /*% %*/
                    /*% ripper: qwords_new! %*/
  return yyVal;
};
states[637] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: qwords_add!($1, $2) %*/
  return yyVal;
};
states[638] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ArrayNode(p.src_line());
                    /*% %*/
                    /*% ripper: qsymbols_new! %*/
  return yyVal;
};
states[639] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(p.asSymbol(yyVals[yyTop - count + 1].start(), ((Node)yyVals[-1+yyTop].value)));
                    /*% %*/
                    /*% ripper: qsymbols_add!($1, $2) %*/
  return yyVal;
};
states[640] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    ByteList aChar = ByteList.create("");
                    aChar.setEncoding(p.getEncoding());
                    yyVal = p.createStr(aChar, 0);
                    /*% ripper: string_content! %*/
  return yyVal;
};
states[641] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: string_add!($1, $2) %*/
                    /* JRuby changed (removed)*/
  return yyVal;
};
states[642] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    ByteList aChar = ByteList.create("");
                    aChar.setEncoding(p.getEncoding());
                    yyVal = p.createStr(aChar, 0);
                    /*% %*/
                    /*% ripper: xstring_new! %*/
  return yyVal;
};
states[643] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: xstring_add!($1, $2) %*/
  return yyVal;
};
states[644] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = null;
                    /*% %*/
                    /*% ripper: regexp_new! %*/
  return yyVal;
};
states[645] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /* FIXME: mri is different here.*/
                    /*%%%*/
                    yyVal = p.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /* JRuby changed*/
                    /*% 
			$$ = p.dispatch("on_regexp_add", $1, $2);
                    %*/
  return yyVal;
};
states[646] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
		    /*% ripper[brace]: ripper_new_yylval(p, 0, get_value($1), $1) %*/
  return yyVal;
};
states[647] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.getStrTerm();
                    p.setStrTerm(null);
                    p.setState(EXPR_BEG);
  return yyVal;
};
states[648] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setStrTerm(((StrTerm)yyVals[-1+yyTop].value));
                   /*%%%*/
                    yyVal = new EvStrNode(yyVals[yyTop - count + 3].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: string_dvar!($3) %*/
  return yyVal;
};
states[649] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = p.getStrTerm();
                   p.setStrTerm(null);
                   p.getConditionState().push0();
                   p.getCmdArgumentState().push0();
  return yyVal;
};
states[650] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = p.getState();
                   p.setState(EXPR_BEG);
  return yyVal;
};
states[651] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = p.getBraceNest();
                   p.setBraceNest(0);
  return yyVal;
};
states[652] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = p.getHeredocIndent();
                   p.setHeredocIndent(0);
  return yyVal;
};
states[653] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   p.getConditionState().pop();
                   p.getCmdArgumentState().pop();
                   p.setStrTerm(((StrTerm)yyVals[-5+yyTop].value));
                   p.setState(((Integer)yyVals[-4+yyTop].value));
                   p.setBraceNest(((Integer)yyVals[-3+yyTop].value));
                   p.setHeredocIndent(((Integer)yyVals[-2+yyTop].value));
                   p.setHeredocLineIndent(-1);

                   /*%%%*/
                   if (((Node)yyVals[-1+yyTop].value) != null) ((Node)yyVals[-1+yyTop].value).unsetNewline();
                   yyVal = p.newEvStrNode(yyVals[yyTop - count + 6].start(), ((Node)yyVals[-1+yyTop].value));
                   /*% %*/
                   /*% ripper: string_embexpr!($6) %*/
  return yyVal;
};
states[654] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new GlobalVarNode(p.src_line(), p.symbolID(((ByteList)yyVals[0+yyTop].value)));
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[655] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new InstVarNode(p.src_line(), p.symbolID(((ByteList)yyVals[0+yyTop].value)));
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[656] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ClassVarNode(p.src_line(), p.symbolID(((ByteList)yyVals[0+yyTop].value)));
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[660] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_END);
                    /*%%%*/
                    yyVal = p.asSymbol(p.src_line(), ((ByteList)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: symbol_literal!(symbol!($2)) %*/
  return yyVal;
};
states[662] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[663] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[664] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[665] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_END);

                    /*%%%*/
                    /* DStrNode: :"some text #{some expression}"*/
                    /* StrNode: :"some text"*/
                    /* EvStrNode :"#{some expression}"*/
                    /* Ruby 1.9 allows empty strings as symbols*/
                    if (((Node)yyVals[-1+yyTop].value) == null) {
                        yyVal = p.asSymbol(p.src_line(), new ByteList(new byte[] {}));
                    } else if (((Node)yyVals[-1+yyTop].value) instanceof DStrNode) {
                        yyVal = new DSymbolNode(yyVals[yyTop - count + 2].start(), ((DStrNode)yyVals[-1+yyTop].value));
                    } else if (((Node)yyVals[-1+yyTop].value) instanceof StrNode) {
                        yyVal = p.asSymbol(yyVals[yyTop - count + 2].start(), ((Node)yyVals[-1+yyTop].value));
                    } else {
                        yyVal = new DSymbolNode(yyVals[yyTop - count + 2].start());
                        ((DSymbolNode)yyVal).add(((Node)yyVals[-1+yyTop].value));
                    }
                    /*% %*/
                    /*% ripper: dyna_symbol!($2) %*/
  return yyVal;
};
states[666] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((NumericNode)yyVals[0+yyTop].value);  
  return yyVal;
};
states[667] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.negateNumeric(((NumericNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: unary!(ID2VAL(idUMinus), $2) %*/
  return yyVal;
};
states[671] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[672] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((FloatNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[673] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((RationalNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[674] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ComplexNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[675] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[676] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[677] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[678] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[679] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[680] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new NilNode(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[681] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new SelfNode(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[682] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new TrueNode(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[683] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new FalseNode(yyVals[yyTop - count + 1].start());
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[684] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new FileNode(yyVals[yyTop - count + 1].start(), new ByteList(p.getFile().getBytes(),
                    p.getRuntime().getEncodingService().getLocaleEncoding()));
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[685] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new FixnumNode(yyVals[yyTop - count + 1].start(), yyVals[yyTop - count + 1].start()+1);
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[686] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new EncodingNode(yyVals[yyTop - count + 1].start(), p.getEncoding());
                    /*% %*/
                    /*% ripper: var_ref!($1) %*/
  return yyVal;
};
states[687] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.assignableLabelOrIdentifier(((ByteList)yyVals[0+yyTop].value), null);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
                    
  return yyVal;
};
states[688] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new InstAsgnNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[689] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new GlobalAsgnNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[690] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (p.getLexContext().in_def) p.compile_error("dynamic constant assignment");

                    yyVal = new ConstDeclNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), null, NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[691] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ClassVarAsgnNode(yyVals[yyTop - count + 1].start(), p.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[692] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to nil");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[693] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't change the value of self");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[694] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to true");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[695] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to false");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[696] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to __FILE__");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[697] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to __LINE__");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[698] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.compile_error("Can't assign to __ENCODING__");
                    yyVal = null;
                    /*%
                      $$ = p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1));
                    %*/
  return yyVal;
};
states[699] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[700] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[701] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   p.setState(EXPR_BEG);
                   p.setCommandStart(true);
  return yyVal;
};
states[702] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[703] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = null;
                    /*% %*/
                    /*% ripper: Qnil %*/
  return yyVal;
};
states[705] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = false;
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, null, null, 
                                    p.new_args_tail(p.src_line(), null, (ByteList) null, (ByteList) null));
  return yyVal;
};
states[706] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ArgsNode)yyVals[-1+yyTop].value);
                    /*% %*/
                    /*% ripper: paren!($2) %*/
                    p.setState(EXPR_BEG);
                    p.getLexContext().in_argdef = false;
                    p.setCommandStart(true);
  return yyVal;
};
states[707] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((ArgsNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[708] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = p.getLexContext();
                    yyVal = ctxt.in_kwarg;
                    ctxt.in_kwarg = true;
                    ctxt.in_argdef = true;
                    p.setState(p.getState() | EXPR_LABEL);
  return yyVal;
};
states[709] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_kwarg = ((Boolean)yyVals[-2+yyTop].value);
                    ctxt.in_argdef = false;
                    yyVal = ((ArgsNode)yyVals[-1+yyTop].value);
                    p.setState(EXPR_BEG);
                    p.setCommandStart(true);
  return yyVal;
};
states[710] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-3+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[711] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-1+yyTop].value), (ByteList) null, ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[712] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(p.src_line(), null, ((ByteList)yyVals[-1+yyTop].value), ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[713] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), null, (ByteList) null, ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[714] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.add_forwarding_args();
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), null, ((ByteList)yyVals[0+yyTop].value), FWD_BLOCK);
  return yyVal;
};
states[715] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[716] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(p.src_line(), null, (ByteList) null, (ByteList) null);
  return yyVal;
};
states[717] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-5+yyTop].value), ((ListNode)yyVals[-3+yyTop].value), ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[718] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-7+yyTop].value), ((ListNode)yyVals[-5+yyTop].value), ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[719] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[720] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-5+yyTop].value), ((ListNode)yyVals[-3+yyTop].value), null, ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[721] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-3+yyTop].value), null, ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[722] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-5+yyTop].value), null, ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[723] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((ListNode)yyVals[-1+yyTop].value), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[724] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((ListNode)yyVals[-3+yyTop].value), ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[725] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((ListNode)yyVals[-5+yyTop].value), ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[726] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((ListNode)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[727] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((ListNode)yyVals[-3+yyTop].value), null, ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[728] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[729] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[730] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[731] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(p.src_line(), null, null, null, null, (ArgsTailHolder) null);
  return yyVal;
};
states[732] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = FWD_KWREST;
                    /*% %*/
                    /*% ripper: args_forward! %*/
  return yyVal;
};
states[733] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "formal argument cannot be a constant";
                    /*%%%*/
                    p.yyerror(message);
                    /*% %*/
                    /*% ripper[error]: param_error!(ERR_MESG(), $1) %*/
  return yyVal;
};
states[734] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "formal argument cannot be an instance variable";
                    /*%%%*/
                    p.yyerror(message);
                    /*% %*/
                    /*% ripper[error]: param_error!(ERR_MESG(), $1) %*/
  return yyVal;
};
states[735] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "formal argument cannot be a global variable";
                    /*%%%*/
                    p.yyerror(message);
                    /*% %*/
                    /*% ripper[error]: param_error!(ERR_MESG(), $1) %*/
  return yyVal;
};
states[736] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "formal argument cannot be a class variable";
                    /*%%%*/
                    p.yyerror(message);
                    /*% %*/
                    /*% ripper[error]: param_error!(ERR_MESG(), $1) %*/
  return yyVal;
};
states[737] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value); /* Not really reached*/
  return yyVal;
};
states[738] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.formal_argument(yyVals[yyTop - count + 1].id, ((ByteList)yyVals[0+yyTop].value));
                    p.ordinalMaxNumParam();
  return yyVal;
};
states[739] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    RubySymbol name = p.get_id(((ByteList)yyVals[0+yyTop].value));
                    p.setCurrentArg(name);
                    /*%%%*/
                    yyVal = p.arg_var(yyVals[yyTop - count + 1].id);
                    /*%
                      p.arg_var(yyVals[yyTop - count + 1].id);
                      $$ = $1;
                      %*/

  return yyVal;
};
states[740] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setCurrentArg(null);
                    /*%%%*/
                    yyVal = ((ArgumentNode)yyVals[0+yyTop].value);
                    /*% %*/
                    /*% ripper: get_value($1) %*/
  return yyVal;
};
states[741] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((Node)yyVals[-1+yyTop].value);
                    /*% %*/
                    /*% ripper: mlhs_paren!($2) %*/
  return yyVal;
};
states[742] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ArrayNode(p.src_line(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper[brace]: rb_ary_new3(1, get_value($1)) %*/
  return yyVal;
};
states[743] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[0+yyTop].value));
                    yyVal = ((ListNode)yyVals[-2+yyTop].value);
                    /*% %*/
                    /*% ripper: rb_ary_push($1, get_value($3)) %*/
  return yyVal;
};
states[744] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.formal_argument(yyVals[yyTop - count + 1].id, ((ByteList)yyVals[0+yyTop].value));
                    p.arg_var(yyVals[yyTop - count + 1].id);
                    p.setCurrentArg(p.get_id(((ByteList)yyVals[0+yyTop].value)));
                    p.ordinalMaxNumParam();
                    p.getLexContext().in_argdef = false;
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[745] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setCurrentArg(null);
                    p.getLexContext().in_argdef = true;
                    /*%%%*/
                    yyVal = new KeywordArgNode(yyVals[yyTop - count + 2].start(), p.assignableKeyword(((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value)));
                    /*%
                      $$ = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, $1), $2);
                    %*/
  return yyVal;
};
states[746] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setCurrentArg(null);
                    p.getLexContext().in_argdef = true;
                    /*%%%*/
                    yyVal = new KeywordArgNode(p.src_line(), p.assignableKeyword(((ByteList)yyVals[0+yyTop].value), new RequiredKeywordArgumentValueNode()));
                    /*%
                      $$ = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, $1), p.nil());
                    %*/
  return yyVal;
};
states[747] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = true;
                    /*%%%*/
                    yyVal = new KeywordArgNode(yyVals[yyTop - count + 2].start(), p.assignableKeyword(((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value)));
                    /*%
                      $$ = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, $1), $2);
                    %*/
  return yyVal;
};
states[748] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = true;
                    /*%%%*/
                    yyVal = new KeywordArgNode(p.src_line(), p.assignableKeyword(((ByteList)yyVals[0+yyTop].value), new RequiredKeywordArgumentValueNode()));
                    /*%
                      $$ = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, $1), p.fals());
                    %*/
  return yyVal;
};
states[749] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ArrayNode(yyVals[yyTop - count + 1].start(), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_new3(1, get_value($1)) %*/
  return yyVal;
};
states[750] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_push($1, get_value($3)) %*/
  return yyVal;
};
states[751] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new ArrayNode(yyVals[yyTop - count + 1].start(), ((KeywordArgNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_new3(1, get_value($1)) %*/
  return yyVal;
};
states[752] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((KeywordArgNode)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_push($1, get_value($3)) %*/
  return yyVal;
};
states[753] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[754] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[755] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = KWNOREST;
                    /*% %*/
                    /*% ripper: nokw_param!(Qnil) %*/
  return yyVal;
};
states[756] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.shadowing_lvar(yyVals[yyTop - count + 2].id);
                    /*%%%*/
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
                    /*% %*/
                    /*% ripper: kwrest_param!($2) %*/
  return yyVal;
};
states[757] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.INTERNAL_ID;
                    /*% %*/
                    /*% ripper: kwrest_param!(Qnil) %*/
  return yyVal;
};
states[758] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    p.setCurrentArg(null);
                    p.getLexContext().in_argdef = true;
                    yyVal = new OptArgNode(yyVals[yyTop - count + 3].start(), p.assignableLabelOrIdentifier(((ArgumentNode)yyVals[-2+yyTop].value).getName().getBytes(), ((Node)yyVals[0+yyTop].value)));
                    /*%
                      $$ = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1)), $3);
                    %*/

  return yyVal;
};
states[759] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = true;
                    p.setCurrentArg(null);
                    /*%%%*/
                    yyVal = new OptArgNode(yyVals[yyTop - count + 3].start(), p.assignableLabelOrIdentifier(((ArgumentNode)yyVals[-2+yyTop].value).getName().getBytes(), ((Node)yyVals[0+yyTop].value)));
                    /*%
                      $$ = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, p.var_field($1)), $3);
                    %*/
  return yyVal;
};
states[760] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new BlockNode(yyVals[yyTop - count + 1].start()).add(((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_new3(1, get_value($1)) %*/
  return yyVal;
};
states[761] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.appendToBlock(((ListNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_push($1, get_value($3)) %*/
  return yyVal;
};
states[762] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new BlockNode(yyVals[yyTop - count + 1].start()).add(((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_new3(1, get_value($1)) %*/
  return yyVal;
};
states[763] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.appendToBlock(((ListNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_push($1, get_value($3)) %*/
  return yyVal;
};
states[764] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = STAR;
  return yyVal;
};
states[765] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[766] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[767] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new UnnamedRestArgNode(p.src_line(), p.symbolID(CommonByteLists.EMPTY), p.getCurrentScope().addVariable("*"));
                    /*% %*/
                    /*% ripper: rest_param!(Qnil) %*/
  return yyVal;
};
states[768] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = AMPERSAND;
  return yyVal;
};
states[769] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[770] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[771] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.arg_var(p.shadowing_lvar(FWD_BLOCK));
                    /*%%%*/
                    yyVal = new BlockArgNode(((ArgumentNode)yyVal));
                    /* Changed from MRI*/
                    /*% 
                        $$ = p.dispatch("on_blockarg", p.nil());
                    %*/
  return yyVal;
};
states[772] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((BlockArgNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[773] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[774] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((Node)yyVals[0+yyTop].value));
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[775] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_BEG);
  return yyVal;
};
states[776] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[777] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new HashNode(p.src_line());
                    /*% %*/
  return yyVal;
};
states[778] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value));
                    /*% %*/
                    /*% ripper: assoclist_from_args!($1) %*/
  return yyVal;
};
states[779] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = new HashNode(p.src_line(), ((KeyValuePair)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper[brace]: rb_ary_new3(1, get_value($1)) %*/
  return yyVal;
};
states[780] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = ((HashNode)yyVals[-2+yyTop].value).add(((KeyValuePair)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: rb_ary_push($1, get_value($3)) %*/
  return yyVal;
};
states[781] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.createKeyValue(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: assoc_new!($1, $3) %*/
  return yyVal;
};
states[782] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    Node label = p.asSymbol(yyVals[yyTop - count + 2].start(), ((ByteList)yyVals[-1+yyTop].value));
                    yyVal = p.createKeyValue(label, ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: assoc_new!($1, $2) %*/
  return yyVal;
};
states[783] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    Node label = p.asSymbol(yyVals[yyTop - count + 1].start(), ((ByteList)yyVals[0+yyTop].value));
                    Node var = p.gettable(((ByteList)yyVals[0+yyTop].value));
                    if (var == null) var = new BeginNode(yyVals[yyTop - count + 1].start(), NilImplicitNode.NIL);
                    yyVal = p.createKeyValue(label, var);
                    /*% %*/
                    /*% ripper: assoc_new!($1, Qnil) %*/
  return yyVal;
};
states[784] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    if (((Node)yyVals[-2+yyTop].value) instanceof StrNode) {
                        DStrNode dnode = new DStrNode(yyVals[yyTop - count + 2].start(), p.getEncoding());
                        dnode.add(((Node)yyVals[-2+yyTop].value));
                        yyVal = p.createKeyValue(new DSymbolNode(yyVals[yyTop - count + 2].start(), dnode), ((Node)yyVals[0+yyTop].value));
                    } else if (((Node)yyVals[-2+yyTop].value) instanceof DStrNode) {
                        yyVal = p.createKeyValue(new DSymbolNode(yyVals[yyTop - count + 2].start(), ((DStrNode)yyVals[-2+yyTop].value)), ((Node)yyVals[0+yyTop].value));
                    } else {
                        p.compile_error("Uknown type for assoc in strings: " + ((Node)yyVals[-2+yyTop].value));
                    }
                    /*% %*/
                    /*% ripper: assoc_new!(dyna_symbol!($2), $4) %*/
  return yyVal;
};
states[785] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /*%%%*/
                    yyVal = p.createKeyValue(null, ((Node)yyVals[0+yyTop].value));
                    /*% %*/
                    /*% ripper: assoc_splat!($2) %*/
  return yyVal;
};
states[786] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[787] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[788] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[789] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[790] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[791] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[792] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[793] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[794] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[795] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[796] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[797] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[798] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[799] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[801] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[806] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RPAREN;
  return yyVal;
};
states[807] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RBRACKET;
  return yyVal;
};
states[808] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RCURLY;
  return yyVal;
};
states[816] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = null;
  return yyVal;
};
states[817] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  yyVal = null;
  return yyVal;
};
}
					// line 4735 "parse.y"

}
					// line 14512 "-"
