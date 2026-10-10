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
    90,    90,    90,    90,    52,    52,    52,    52,    52,    52,
    52,    52,    52,   128,   128,   264,   125,   265,   126,    62,
    62,    62,    62,    61,    63,    63,   225,   224,   221,   266,
   141,   142,   142,   143,   143,   143,   144,   144,   144,   144,
   144,   144,   145,   146,   146,   147,   147,   222,   223,   148,
   148,   148,   148,   148,   148,   148,   148,   148,   148,   148,
   148,   148,   267,   148,   148,   148,   150,   150,   150,   150,
   150,   150,   151,   151,   152,   152,   149,   184,   184,   153,
   153,   154,   161,   161,   161,   161,   162,   162,   163,   163,
   188,   188,   185,   185,   186,   187,   187,   155,   155,   155,
   155,   155,   155,   155,   155,   155,   155,   156,   156,   156,
   156,   156,   156,   156,   156,   156,   156,   156,   156,   156,
   156,   156,   156,   157,   158,   158,   159,   160,   160,   160,
    64,    64,    65,    65,    65,    66,    66,    67,    67,    20,
    20,     2,     3,     3,     3,     4,     5,     6,   268,   268,
    11,    16,    16,    19,    19,    12,    13,    13,    14,    15,
    17,    17,    18,    18,     7,     7,     8,     8,     9,     9,
    10,   269,    10,   270,   271,   272,   273,    10,   274,   274,
   110,   110,    25,    25,    23,   164,   164,    24,    21,    21,
    22,    22,    22,    22,   193,   193,   193,    82,    82,    82,
    82,    82,    82,    82,    82,    82,    82,    82,    82,    83,
    83,    83,    83,    83,    83,    83,    83,    83,    83,    83,
    83,   109,   109,   275,    89,    89,    95,    95,    96,    94,
   276,    94,    74,    74,    74,    74,    74,    75,    75,    97,
    97,    97,    97,    97,    97,    97,    97,    97,    97,    97,
    97,    97,    97,    97,   191,   175,   175,   175,   175,   174,
   174,   178,    99,    99,    98,    98,   177,   117,   117,   119,
   119,   118,   118,   116,   116,   197,   197,   189,   176,   176,
   115,    93,    92,    92,   100,   100,   196,   196,   171,   171,
   195,   195,   172,   172,   173,   173,     1,   277,     1,   105,
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
     5,     5,     3,     3,     2,     4,     4,     3,     3,     3,
     2,     1,     4,     3,     3,     0,     7,     0,     7,     1,
     2,     3,     4,     5,     1,     1,     0,     0,     0,     0,
     9,     1,     1,     1,     3,     3,     1,     2,     3,     1,
     1,     1,     1,     3,     1,     3,     1,     2,     2,     1,
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
   323,     0,     0,   791,     0,     0,    98,     0,   500,    94,
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
     0,     0,     0,     0,     0,     0,     0,   408,   488,   507,
   407,   505,   370,   507,   810,     0,     0,   809,     0,     0,
   494,     0,   368,   827,     0,     0,     0,   827,   827,   827,
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
    33,    34,   299,     0,    62,    63,   518,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   518,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   476,     0,   476,     0,     0,     0,     0,   499,
   802,     0,   497,     0,     0,     0,     0,   801,     0,   498,
     0,   803,     0,   505,    88,     0,   493,   492,   799,   800,
     0,     0,     0,     0,     0,     0,     0,   116,     0,   827,
   419,     0,     0,     0,   808,   807,    73,     0,     0,     0,
   384,   157,     0,   159,   713,   382,     0,     0,     0,     0,
     0,     0,   363,     0,   827,     0,     0,     0,   793,     0,
     0,     0,     0,     0,     0,   333,   328,     0,     0,   792,
     0,     0,     0,   307,     0,   309,   366,   817,    26,     0,
     0,    10,     0,     0,     0,     0,     0,     0,     0,    22,
     0,    19,   355,     0,     0,     0,     0,     0,     0,     0,
     0,   478,   664,     0,   640,   643,     0,     0,   648,   645,
     0,     0,   649,     0,     0,   431,     0,     0,     0,   429,
   718,     0,   735,     0,   738,     0,   723,     0,   740,   757,
     0,     0,     0,   724,   782,   778,   584,   768,     0,     0,
     0,     0,     0,    55,   721,     0,     0,     0,   414,   415,
   374,   422,    78,   421,   375,     0,     0,     0,     0,     0,
     0,    35,   516,   516,     0,   487,   477,   503,   477,   504,
   827,   827,   505,   496,     0,     0,     0,     0,   827,   827,
   311,   495,     0,   310,     0,     0,     0,    82,     0,     0,
    45,   244,    60,     0,     0,     0,     0,    52,   251,     0,
     0,   330,     0,    44,   243,    39,    38,     0,   336,     0,
   114,     0,     0,   353,     0,     0,   420,     0,     0,   518,
     0,     0,   411,     0,     0,     0,     0,     0,     0,     0,
     0,   161,     0,     0,     0,   362,   171,     0,   796,     0,
     0,   827,   827,     0,   827,   827,   322,     0,     0,     0,
     0,    51,   250,   303,   118,     0,    23,   665,   671,   662,
   670,   644,     0,     0,     0,     0,     0,   438,     0,     0,
   753,   727,   755,     0,     0,     0,   775,     0,   764,   784,
     0,     0,     0,   752,   770,   443,   389,     0,   827,   827,
   391,    79,     0,     0,   517,   517,     0,   478,   478,     0,
     0,     0,    92,   827,   818,     0,     0,    90,    85,     0,
   827,   827,     0,     0,     0,   827,   490,   491,     0,     0,
   827,     0,   409,     0,   621,     0,   413,   412,     0,   423,
   425,     0,     0,   788,    74,   516,   380,     0,   379,     0,
   509,     0,     0,     0,     0,     0,   502,   387,    36,     0,
     0,   827,     0,     0,     0,   308,   365,     0,     0,   666,
   430,   432,     0,     0,     0,   731,     0,   733,     0,   739,
     0,   736,   722,   741,     0,     0,     0,     0,   381,     0,
     0,     0,    23,    23,    49,   248,    50,   249,    93,     0,
    47,   246,    48,   247,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,    11,     0,     0,     0,   623,
   624,   372,   426,     0,   517,   377,   510,     0,     0,   373,
     0,   714,   383,     0,     0,   483,   480,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   606,   605,   607,   608,
   610,   609,   611,   613,   619,   580,     0,     0,     0,   517,
     0,     0,     0,   654,     0,   598,   599,   600,   601,   603,
   602,   604,   597,   612,    69,     0,   532,     0,   536,   529,
   530,   539,     0,   540,   592,   593,     0,   531,     0,   576,
     0,   585,   586,   575,     0,     0,    67,     0,     0,    46,
   245,     0,    59,     0,     0,    40,     0,   410,    15,   628,
     0,     0,     0,   626,     0,     0,     0,   511,     0,   385,
     0,     0,     0,     0,   732,     0,   729,   734,   737,   595,
   596,   158,   617,     0,     0,     0,     0,     0,   561,     0,
   551,   554,   827,     0,   567,     0,   614,     0,   615,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   582,     0,     0,   467,   466,     0,    12,   627,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   625,     0,     0,     0,     0,
   512,   514,   515,   513,     0,     0,   485,     0,   481,   669,
   668,   667,     0,     0,   550,   549,     0,     0,     0,   562,
   552,   819,   581,     0,   533,   528,     0,   535,   588,   589,
   618,   548,   538,   544,   537,     0,     0,     0,     0,     0,
     0,   654,   577,   572,     0,   569,   465,     0,   772,     0,
     0,     0,   761,     0,     0,   447,     0,     0,     0,   508,
   506,     0,     0,     0,     0,     0,     0,   424,   519,     0,
     0,   482,     0,     0,     0,   730,   555,   563,     0,     0,
   616,     0,   542,   541,   543,   546,   545,   547,     0,     0,
     0,   461,     0,   458,   456,     0,     0,   445,   468,     0,
   463,     0,     0,     0,     0,     0,   446,    13,     0,     0,
     0,     0,     0,   620,     0,   524,   525,   474,     0,   472,
   475,     0,   484,     0,     0,     0,   570,   566,   448,   773,
     0,     0,     0,     0,   469,   762,     0,     0,   358,     0,
     0,     0,     0,     0,   471,   486,     0,   553,     0,   462,
     0,   459,     0,   453,     0,   455,   444,   464,     0,     0,
   521,   522,   520,   473,     0,     0,     0,     0,   460,   454,
     0,   451,   457,     0,   452,
    }, yyDgoto = {
//yyDgoto 278
     1,   452,    69,    70,    71,    72,    73,   321,   326,   327,
   556,    74,    75,   565,    76,    77,   563,   564,   566,   766,
    78,    79,    80,    81,    82,    83,   471,   453,   266,   267,
   258,    86,    87,    88,   207,    89,    90,    91,   259,   800,
   801,   688,   689,   280,   281,   282,    93,    94,    95,    96,
    97,   848,    98,   438,   345,   235,   269,   100,   270,   988,
   989,   883,  1001,  1243,   983,  1068,  1162,  1158,   664,   238,
   502,   503,   659,   841,   931,   782,  1368,  1331,   249,   288,
   493,   240,   102,   241,   863,   864,   104,   865,   869,   705,
   105,   106,  1287,  1288,   338,   339,   340,   582,   583,   584,
   585,   775,   776,   777,   778,   292,   504,   243,   202,   244,
   919,   363,  1290,  1214,  1215,   586,   587,   588,  1291,  1292,
  1358,  1244,  1359,   108,  1248,   653,   651,  1086,   422,   674,
   408,   245,   260,   203,   111,   112,   113,   114,   115,   545,
   285,   880,  1402,  1238,  1124,  1256,  1126,  1127,  1128,  1129,
  1186,  1187,  1188,  1284,  1189,  1131,  1132,  1133,  1134,  1135,
  1136,  1137,  1138,  1139,   322,   116,   746,   662,   474,   663,
   205,   589,   590,   786,   591,   592,   593,   594,   943,   708,
   426,   206,   406,   696,  1140,  1141,   596,  1143,  1144,   597,
   598,   599,  1334,   324,   622,   600,   601,   602,   509,   836,
   494,   271,   991,   884,   118,   119,   413,   409,   992,  1191,
   120,   810,   121,   122,   518,   123,   124,   125,   985,  1160,
   623,   822,  1207,  1208,  1039,   954,   551,   761,   916,     2,
   368,   458,  1156,  1301,  1066,   526,   515,   510,   640,   626,
   878,   346,   812,   951,   272,   713,   535,   250,   435,   532,
   691,   881,   698,   888,   692,   886,   709,   603,   606,   790,
   791,   317,  1171,  1313,   654,   652,  1354,  1319,   329,   763,
   762,   917,  1019,  1087,  1251,   887,   342,   693,
    }, yySindex = {
//yySindex 1415
     0,     0, 26674,     0,     0,     0,     0, 30245,     0,     0,
     0,     0,     0,     0,     0, 27134, 27134,     0,     0,     0,
     0,   110,     0,     0,     0,     0,   608, 30140,   194,   185,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  1422, 29002, 29002,
 29002, 29002,    15, 26788, 26904, 27708, 27938, 30912,     0, 30763,
     0,     0,     0,   335,   335,   335,   335, 29116, 29002,     0,
    53,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   420,   420,     0,     0, 32968,     0,
   180,     0,  1789,   851, 25631,     0,   190,     0,    10,   133,
   333,   322,     0,   143,     0,   131,     0,   166,     0,   494,
     0,   549, 33081,     0,   595,     0,     0, 27134,     0, 28054,
 30661, 25897, 28054, 33194, 33307,     0,   624,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   601,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   644,     0,     0,     0,     0,     0,
     0,     0,     0, 29002,   425, 26904, 29002, 29002, 29002,     0,
 29002,   420,   420, 25660,     0,   401,   151,   731,     0,     0,
     0,   428,   775,     0,   479,   813,     0, 27248,     0,     0,
 27134, 26012,     0, 29232,   995,     0,   845,     0, 26674,     0,
     0,     0,   592,     0,     0,   110,   420,   420,     0,   328,
   322,     0,   624,   598, 10885, 10885,   602,     0, 26788,   922,
   180,     0,  1789,     0,     0,   194,     0,   747,   898,   887,
     0,   401,   855,   887,     0,     0,     0,     0,     0,   194,
     0,     0,     0,     0,     0,     0,     0,     0,  1422,   692,
 33420,   420,   420,     0,   424,     0,   961,     0,     0,     0,
     0,   800,     0,     0,     0,  1180,  1197,   739,     0,   983,
   983,   983,   983,     0,     0,     0,     0,  3413,     0,   960,
     0,     0,  3413,     0,   967, 26904, 28054, 26904,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   733,   528,     0,   789,     0,     0,     0,     0,     0, 26424,
     0, 28054, 28054, 28054, 28054,     0, 29232, 29232,     0, 29002,
 29002, 29002, 29002, 29002,     0,     0, 29002, 29002, 29002, 29002,
 29002, 29002, 29002, 29002,     0, 29002,     0,     0, 29002, 29002,
 29002, 29002, 29002, 29002, 29002, 29002, 29002,     0,     0,     0,
     0,     0,     0,     0,     0, 31267, 27134,     0, 31458, 29002,
     0,   728,     0,     0,   110,   110, 32748,     0,     0,     0,
 26788, 31170,  1050,     0,     0, 26904,     0,   851,   237,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,    11,     0,     0,     0,   180,     0,  1041,   237,
     0,     0,     0,     0,     0,     0,     0,     0,     0, 28054,
   318,  1043,   543,     0,     0,     0,   991, 25782,     0,     0,
     0,   833,     0,     0,     0,   996,  1064,  1078, 29002, 31523,
 27134, 31590, 27364,     0,     0,     0, 27824,     0,     0,     0,
 29002,  1134,     0,   194,  1147,     0,   194,     0,    62,     0,
  1155,     0,     0,     0,     0, 30245,     0,     0, 29002,  1085,
 29002,  1152,  1159, 31631, 31590,     0,   185,   194,     0,     0,
 26538,     0,  1188, 27708,     0,     0,     0, 27938,     0,     0,
     0,   845,     0,     0,     0,  1181,     0, 31686, 27134, 31780,
 33420,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,  1332,   120,  1347,   214,     0,     0,     0,
     0,     0,     0,     0,     0,  1966,     0,     0,     0,     0,
     0,     0,   194,  1195,     0,  1218,     0,  1238,     0,  1252,
     0,     0,     0,     0, 29002,     0,     0,     0,  1273,     0,
   965,  1027,   366, 26904, 29346,   180, 26904, 29346,    19,    88,
    19,     0, 32008, 27134, 32102,     0,     0,     0,     0,     0,
     0,     0,     0, 27018,     0,     0,     0,   598,  6799,  6799,
  6799,  6799, 11899, 11392,  6799,  6799, 10885, 10885,   779,   779,
     0,  3515,   804,   804,   895,   163,   163,   598,   598,   598,
  4232,    19,     0,  1211,     0,    19,   992,     0,     2,     0,
     0,   110,     0,     0,  1302,   194,  1003,     0,  1004,     0,
   110,     0,  4232,     0,     0, 29462,     0,     0,     0,     0,
   110, 29462, 28168, 28284,   194, 33420,  1312,     0,    19,     0,
     0, 26904,  1090, 29232,     0,     0,     0,  1094,  1106, 26904,
     0,     0,     0,     0,     0,     0, 32143, 27134, 32187, 26904,
 26904,   194,     0, 30245,     0, 29002, 29346, 29346,     0,  1033,
    52,   194,  1037,  1056,   401,     0,     0,   775, 29002,     0,
 29002, 29002, 27478,     0, 27824,     0,     0,     0,     0, 29232,
 25660,     0,   598, 29576, 29576,  1057,   110,   110, 29462,     0,
     0,     0,     0,   887, 33420,     0,     0,   194,     0,     0,
  1181,     0,     0,  1439,     0,     0,    26,   335,     0,     0,
    26,   335,     0,  1966,  1519,     0,  1330,  1334,   194,     0,
     0,  3413,     0,  3413,     0,   248,     0,  3659,     0,     0,
 29002,  1331,    17,     0,     0,     0,     0,     0,    19,   608,
  1102,  1112, 25660,     0,     0,    19,  1102,  1112,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   194,     0,     0,
 26904,     0,     0,     0,  1353,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   728, 27364,  1062,  1321,     0,     0,
     0,     0,   728,     0,  1296,  1114, 24497,     0,  1127,   637,
     0,     0,     0,   166,  1377,    10,   190,     0,     0, 29002,
 24497,     0,  1396,     0,     0,     0,     0,     0,     0,  1145,
     0,  1181, 33420,     0,  1183,   901,     0,    62, 31080,     0,
    19,  1106,     0,    19, 28398,  1186,   180, 28054, 26904,     0,
     0,     0,   194,    19,  1327,     0,     0, 29002,     0,  1112,
  1112,     0,     0,  1107,     0,     0,     0,  1415,   194,    62,
   608,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   983,   983,   983,   983,   194,     0,  1966,  2428,
     0,     0,     0,  1420,  1423,  1425,     0,  1427,     0,     0,
  1273,  1167,  1425,     0,     0,     0,     0, 29346,     0,     0,
     0,     0,     0,    19,     0,     0, 29002,     0,     0, 29462,
 29462,  1352,     0,     0,     0, 29462, 29462,     0,     0,   602,
     0,     0, 32233, 27134, 32330,     0,     0,     0,     0, 28514,
     0,  1181,     0,  1186,     0, 28628,     0,     0,    19,     0,
     0, 26904, 28054,     0,     0,     0,     0,    19,     0, 29002,
     0,   108,    19, 26904,   180,    19,     0,     0,     0, 29002,
 29002,     0, 29002, 29002, 27824,     0,     0, 29576,  4789,     0,
     0,     0,  1437,  1441,  3413,     0,  3659,     0,  3659,     0,
  3659,     0,     0,     0,  1102,  1112, 29002, 29002,     0, 29957,
 29957, 25660,     0,     0,     0,     0,     0,     0,     0, 29462,
     0,     0,     0,     0, 29002, 27018,   992,     2,   194,  1003,
  1004, 29462, 29002,     0, 27018,     0,  1223,     0,  1148,     0,
     0,     0,     0,   237,     0,     0,     0, 28744, 26904,     0,
    19,     0,     0, 29002,  3413,     0,     0, 26904,  2428,  2428,
  1425,  1454,  1425,  1425, 25660, 25660,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,  4403,  4403,   430,     0,
 13078,   194,  1196,     0,  1087,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,    35,     0,  1381,     0,     0,
     0,     0,   816,     0,     0,     0,    -1,     0,  1457,     0,
  1464,     0,     0,     0, 30530,   690,     0,  1388,  1388,     0,
     0, 25660,     0,  1062,     0,     0, 26904,     0,     0,     0,
 26904, 33533,   237,     0, 26904, 29957, 29002,     0,   917,     0,
   194,  -162,   132,  1441,     0,  3659,     0,     0,     0,     0,
     0,     0,     0, 30530,  1179,   194,   194, 18220,     0,  1495,
     0,     0,     0,  1408,     0,  1108,     0, 28054,     0,  1234,
 18220, 30530,  4403,  4403,   430,   194,   194, 29957, 29957,   856,
 30530,  1179,     0,  1138, 26904,     0,     0, 26904,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  1232,   723,     0,     0, 26904,   901,   237,   955,
     0,     0,     0,     0,  1505,  1499,     0, 26904,     0,     0,
     0,     0,  1425,    58,     0,     0,  1179,  1504,  1527,     0,
     0,     0,     0,   194,     0,     0,  1530,     0,     0,     0,
     0,     0,     0,     0,     0,   194,   194,   194,   194,   194,
   194,     0,     0,     0,  1534,     0,     0,  1535,     0,  1537,
   194,  1540,     0,  1466,  1553,     0, 33646,     0,  1273,     0,
     0,  1223,     0, 32555, 27134, 32652,  1183,     0,     0, 28054,
 28054,     0,  1642, 26904,  1478,     0,     0,     0, 30530,   856,
     0, 30530,     0,     0,     0,     0,     0,     0,   642, 18220,
  3865,     0,  3865,     0,     0,  1480,   248,     0,     0,  1895,
     0,     0,     0,  1295,   931, 33646,     0,     0,     0,     0,
   194,     0,     0,     0, 26904,     0,     0,     0,   946,     0,
     0,    19,     0,  1563,   194,  1563,     0,     0,     0,     0,
  1570,  1573,  1574,  1582,     0,     0,  1273,  1570,     0, 32693,
   931,     0,    31,  1642,     0,     0, 30530,     0,  1895,     0,
  1895,     0,  3865,     0,  1895,     0,     0,     0,     0,     0,
     0,     0,     0,     0,  1570,  1570,  1583,  1570,     0,     0,
  1895,     0,     0,  1570,     0,
    }, yyRindex = {
//yyRindex 1415
     0,     0,   722,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0, 18807, 18897,     0,     0,     0,
     0, 15085, 19499, 19814, 19904, 19996, 29692,     0, 28886,     0,
     0, 20311, 20401, 20493, 15936,  6247, 20808, 20898, 16053, 20990,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   161,   161,  1536,  1211,   250,     0,  1357,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  9704,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,  1928,  1928,     0,     0,     0,     0,
   137,     0,   538, 12583, 23579,  9799, 23135,     0,  9897,     0,
 12241, 27594,     0,     0,     0, 23174,     0, 21305,     0,     0,
     0,     0,   500,     0,     0,     0,     0, 19001,     0,     0,
     0,  1360,     0,     0,     0,     0, 16420,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0, 23659,     0,     0,     0,     0,     0,     0,     0,
  7159,  7254,  7352,  7668,     0,  7763,  7861,  8177,  4912,  8272,
  8370,  5279,  8686,  3436,     0,   161,  4120,  4433,  4251,     0,
     0,  1928,  1928,  2771,     0, 21661,     0,  7571,     0,     0,
     0,     0,  7571,     0,  9290,     0,     0,   153,     0,     0,
     0,  1592,     0,     0,     0,     0, 29806,     0,   240,     0,
     0,     0, 10308,     0,     0,  8781,  1928,  1928,     0,     0,
     0,     0, 10213, 10720,  1649,  3651, 21395,     0,   161,     0,
  2898,     0,  1291,     0,  1118,  1592,     0,  1542,     0,  1542,
     0,     0,     0,  1514,     0,  1175,  1246,  1531,  1539,  1601,
  2130,  2374,  2393,   913,  2454,  2709,  1443,  2750,     0,     0,
     0,  2299,  2299,     0,     0,  2864,   552,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  1389,
   323,  1435,   533,     0,     0,     0,     0,   751,     0,     0,
 25149,     0,   683,     0,     0,   149,     0,   149,   929,  1072,
  1264,  1517,  1780,  2400,  2498,  1771,  2621,  2753,  1935,  2770,
     0,     0,  2832,     0,     0,     0,     0,     0,     0,   267,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  4554, 10404,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   308,     0,     0,     0,
     0,  8589,     0,     0, 32808, 32878,     0,     0,     0,     0,
   161,   724,   784,     0,     0,   154,     0,  1798,     0,  3954,
  4701,  5073,  6917, 10555, 11062, 11569, 14276,     0,     0, 14958,
     0,     0,     0,     0,     0,     0,   544,     0,   687,     0,
     0,     0,     0,     0,     0,     0,     0, 23853, 23975,     0,
     0, 25263,     0,     0,     0,     0,     0,  1592,     0,     0,
     0,  9388,     0,     0,     0,     0,     0,     0,     0,     0,
   308,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   868,   893,     0,  1592,   581,     0,  1592,     0,  1592,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  1592,     0,     0,
  3342,   183,     0,  1549,     0,     0,     0,   755,     0,     0,
     0,     0,     0,  3183,     0,   988,     0,     0,   308,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  1592,   199,     0,   199,     0,   247,     0,   199,
     0,     0,     0,     0,   144,   121,     0,     0,   247,     0,
   174,   555,   736,   154,     0,     0,   154,     0,     0,     0,
     0,  2842,     0,   308,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0, 10815,  3113, 13529,
 13773, 13873, 13969, 14271, 14090, 14176, 14393, 14520, 12735, 12830,
     0,  1585, 13226, 13312, 12926, 12336, 12432, 10911, 11227, 11322,
 13407,     0,     0,     0,     0,     0, 16537,  6364, 17989,     0,
     0, 27594,     0,  6731,   421,  1560, 16904,     0, 17021,     0,
 15452,     0, 13656,     0,     0,     0,     0,     0,     0,     0,
 18473,     0,     0,     0,  1592,     0,  1086,     0,     0,     0,
     0,   221, 24862,     0,     0,     0,     0,  1384,     0,   311,
     0,     0, 24748,     0,     0,     0,     0,   308,     0,   154,
   240,  1592,     0,     0,     0,     0,     0,     0,     0,  5396,
 15569,  1560,  5763,  5880, 21895,     0,     0,  7571,     0,     0,
     0,     0,   938,     0,   714,     0,     0,     0,     0,     0,
 14636,     0, 11418,     0,     0,  8879,     0,  9195,     0,     0,
  1036,     0,     0,  1542,     0,  1555,  1001,  1560,  1579,  2202,
  1103,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   900,     0,  1014,  1020,  1592,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0, 29692,
 11734,  2261, 14683,     0,     0,     0, 11829,  2649,     0,     0,
     0,     0,     0,     0,     0,  2384,  1184,  1560,  2791,  2811,
   149,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,  9098,   427, 19317,     0,     0,     0,
     0,     0,  9607,     0,     0,     0,  3781,     0, 15213,     0,
     0,     0,     0, 21487,     0,  4362, 23266,     0,     0,  1612,
  1898,     0,     0,     0,     0,     0,     0, 12089,     0, 23709,
     0,  1227,     0,     0,   593,   263,     0,  1592,     0,     0,
     0,     0,     0,     0,     0,   263,     0,     0,   154, 24309,
 24423,     0,  1560,     0,     0,     0,     0,     0,     0, 10115,
 10526,     0,     0,  6843,     0,     0,     0,   581,  1592,  1592,
 29692,     0,     0,     0,     0,  2255,     0,     0,     0,     0,
     0,     0,  1469,   557,  1489,   569,  1592,     0,     0,     0,
     0,     0,     0,   199,   199,   199,     0,   199,     0,     0,
   247,   736,   199,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  1863,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0, 21802,
     0,     0,     0,   308,     0,     0,     0,     0,  2685,     0,
     0,  1228,     0,   495,     0,    83,     0,     0,     0,     0,
     0,   149,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   154,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   894,     0,     0,     0,  -148,     0,
     0,     0,  1024,  1028,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0, 11925, 11033,     0,     0,     0,     0,
     0, 14769,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0, 17388, 18356,  1560, 17505,
 17872,     0,  1612,  3048,     0,     0,   263,   124,   245,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   860,     0,
     0,     0,     0,     0,   853,     0,     0,    65,     0,     0,
   199,   199,   199,   199, 14819,  8080,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  1560,   330, 22355,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0, 23303,     0, 22318,     0,     0,
     0,     0, 21934,     0,     0,     0, 21981,     0, 11540,     0,
 12047,     0,     0,     0, 22432, 22712,     0, 25472, 25586,     0,
     0, 14953,     0, 19409, 10065,     0,   204,     0,     0,     0,
   149,     0,     0,     0,   221,     0,     0,     0,   263,     0,
   947,     0,     0,  1046,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0, 22469,  1560,  1560, 22749,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
 23366,     0, 22019, 22111,     0, 26290, 30327,     0,     0, 22812,
     0, 22532,     0,   173,   154,     0,     0,   240,     0,     0,
   936,  1754,  1757,  2171,  2297,  2367,  2415,  1042,  2417,  2437,
  1588,  2440,     0,     0,  2457,     0,   154,   263,     0,   546,
     0,     0,     0,     0,     0,   102,     0,   240,     0,     0,
     0,     0,   199,  1592,     0,     0, 22627, 22875, 22960,     0,
     0,     0,     0,  1592,     0,     0, 23434,     0,     0,     0,
     0,     0,     0,     0,     0,  1592,  1592,  1592,  1560,  1560,
  1560,     0,     0,     0, 23025,     0,     0,   177,     0,   177,
   173,   218,     0,     0,   177,     0,   606,  1432,   218,     0,
     0,   263,  2501,     0,   308,     0,   593,     0,     0,     0,
     0,     0,     0,   154,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   464,     0,     0,     0,     0,     0,     0,     0,
     0,   743,   793,     0,  1547,     0,     0,     0,  1723,  1335,
  1560,  2271,  2349,     0,   380,     0,     0,     0,   413,     0,
     0,     0,     0, 23088,  1538, 23484,     0,     0,     0,     0,
   177,   177,   177,   177,     0,     0,   218,   177,     0,     0,
  1577,  1654,   263,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  1681,  1987,
     0,     0,     0,     0,   177,   177,   177,   177,     0,     0,
     0,     0,     0,   177,     0,
    }, yyGindex = {
//yyGindex 278
     0,     0,  3000,     0,  1596, 23320, 23758,   -59,     0,     0,
  -197, 23770, 24156,     0, 25203, 25235,     0,     0,     0,  1097,
 25309,     0,    79,     0,     0,    36,  1544,   791,  2143,  2582,
     0,     0,     0,     0,   -11,  1414,     0,  1304,  1154,  -551,
  -518,  -458,   -30,     0,  1144,     3,   -16,  4067,    16,    -4,
   -52,  1005,     0,   -37,   -58,  1908,    34,     0,   828,   449,
  -854,  -819,     0,     0,   390,     0,     0,   386,    55,  -384,
   101,  -375,    59,  1025,  -300,  -282,   493,  1889,     1,     0,
  -222,  -462,  1590,  3281,  -459,   685,   369,  -563,     0,     0,
     0,     0,   385, -1014,   807,  1170,   974,  -324,    78,  -753,
   939,  -796,  -817,   798,   957,     0,    44,  -456,     0,  2818,
     0,     0,     0,   583,     0,  -697,     0,   966,     0,   396,
     0, -1094,   376, 25358,     0,  -537,  1333,     0,   -92,   319,
   906,  2992,    -2,   -18,  1677,     0,    -9,   -98,     7,  -484,
  -181,   384,     0,     0,  -707,  2479,     0,     0,   570,  -869,
   -85,     0,  -717, -1129,   363,     0,  -238,   571,     0,     0,
     0,  -460,     0,   564,     0,   880,  -380,     0,  -438,    18,
   -46,  -705,   126,  -577,  -389, -1131,  -777,  2084,  -320,   -95,
     0,     0,  1685,     0, -1009,     0,   -72,   577,     0,     0,
   959,  -214,     0,  -737,    -3,     0,     0,  -766,   109,    47,
     0,  1407,   809,     0,     0,     0,     0,     0,     0,   431,
     0,  -434,     0,     0,  1281,     0,     0,     0,     0,     0,
   294,  -548,     0,     0,   404,  -716,   605,   381,   383,     0,
   -73,    33,     0,     0,     0,     0,     0,    41,     0,     0,
     0,     0,     0,     0,  1622,     0,  -228,     0,     0,     0,
  -452,     0,     0,     0,   -74,     0,     0,     0,     0,   502,
     0,     0,     0,     0,     0,     0,     0,     0,    30,     0,
     0,     0,     0,     0,     0,     0,     0,     0,
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
    "block_call : block_call call_op paren_args",
    "block_call : block_call tCOLON2 paren_args",
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
            // an empty production is located, empty, right after the previous symbol (as bison does)
            start = count > 0 ? yystates[yytop - count + 1].start : yystates[yytop].end;
            end = yystates[yytop].end;
            yyVal = parserState.execute(this, yyVal, yystates, yytop, count, yytoken);
            // record the production's source span on the node it produced (see Node#setAutoSourceSpan: each
            // enclosing production handing the node along widens it until an explicit span locks it; a node
            // made by an empty production has no source of its own)
            if (count > 0 && yyVal instanceof org.jruby.ast.Node node) node.setAutoSourceSpan(start, end);
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

static ParserState<RipperParser>[] states = new ParserState[829];
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
states[50] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[51] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[52] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = p.var_field(((IRubyObject)yyVals[-3+yyTop].value));
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_assign", v1, v2);
                    yyVal = p.backref_error(((IRubyObject)yyVals[-3+yyTop].value), v3);
                    p.error();}
  return yyVal;
};
states[53] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[54] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[56] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_rescue = ((LexContext)yyVals[-1+yyTop].value).in_rescue;
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_rescue_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[57] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((IRubyObject)yyVals[0+yyTop].value)), NOT);
{IRubyObject v1, v2, v3;
                    v1 = p.intern("!");
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_unary", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[58] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[59] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_rescue_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[62] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.logop(((IRubyObject)yyVals[-2+yyTop].value), AND_KEYWORD, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[63] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.logop(((IRubyObject)yyVals[-2+yyTop].value), OR_KEYWORD, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[64] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((IRubyObject)yyVals[0+yyTop].value)), NOT);
  return yyVal;
};
states[65] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((IRubyObject)yyVals[0+yyTop].value)), BANG);
  return yyVal;
};
states[66] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((IRubyObject)yyVals[-1+yyTop].value));
  return yyVal;
};
states[67] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[68] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((IRubyObject)yyVals[-1+yyTop].value));
  return yyVal;
};
states[69] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[71] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pushLocalScope();
                    ByteList currentArg = p.getCurrentArg();
                    p.setCurrentArg(null);
                    LexContext ctxt = p.getLexContext();
                    RubySymbol name = p.get_id(((IRubyObject)yyVals[0+yyTop].value));
                        p.numparam_name(yyVals[yyTop - count + 1].id);
                        yyVal = new DefHolder(p.get_id(yyVals[yyTop - count + 1].id), currentArg, p.get_value(((IRubyObject)yyVals[0+yyTop].value)), (LexContext) ctxt.clone());

                    ctxt.in_def = true;
                    ctxt.in_rescue = LexContext.InRescue.BEFORE_RESCUE;
                    ctxt.cant_return = false;
                    p.setCurrentArg(null);
  return yyVal;
};
states[72] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    ((DefHolder)yyVals[0+yyTop].value).line = yyVals[yyTop - count + 1].start();
                    ((DefHolder)yyVals[0+yyTop].value).column = ProductionState.column(yyVals[yyTop - count + 1].start);
                    yyVal = ((DefHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[73] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_FNAME); 
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_argdef = true;
  return yyVal;
};
states[74] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_ENDFN|EXPR_LABEL);
                    ((DefHolder)yyVals[0+yyTop].value).line = yyVals[yyTop - count + 1].start();
                    ((DefHolder)yyVals[0+yyTop].value).column = ProductionState.column(yyVals[yyTop - count + 1].start);
                    yyVal = ((DefHolder)yyVals[0+yyTop].value);
                       ((DefHolder)yyVal).value = p.new_array(((IRubyObject)yyVals[-3+yyTop].value), ((IRubyObject)yyVals[-2+yyTop].value), ((DefHolder)yyVal).value);

  return yyVal;
};
states[75] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[76] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.NEW_ERROR(yyVals[yyTop - count + 1]);
  return yyVal;
};
states[77] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getConditionState().push1();
  return yyVal;
};
states[78] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getConditionState().pop();
  return yyVal;
};
states[79] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-2+yyTop].value);
{                    yyVal = p.get_value(((IRubyObject)yyVals[-2+yyTop].value));}
  return yyVal;
};
states[82] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                       p.value_expr(((IRubyObject)yyVals[0+yyTop].value));
                       yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[84] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[85] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[86] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);}
  return yyVal;
};
states[87] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_command", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[88] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[89] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v4 = ((IRubyObject)yyVals[0+yyTop].value);
                    v5 = p.dispatch("on_command_call", v1, v2, v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[90] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[91] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = p.intern("::");
                    v3 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v4 = ((IRubyObject)yyVals[0+yyTop].value);
                    v5 = p.dispatch("on_command_call", v1, v2, v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[92] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[93] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[94] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_super", v1);
                    yyVal = v2;}
  return yyVal;
};
states[95] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_yield", v1);
                    yyVal = v2;}
  return yyVal;
};
states[96] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_return", v1);
                    yyVal = v2;}
  return yyVal;
};
states[97] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_break", v1);
                    yyVal = v2;}
  return yyVal;
};
states[98] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_next", v1);
                    yyVal = v2;}
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
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[102] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_mlhs_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[103] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);}
  return yyVal;
};
states[104] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_mlhs_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[105] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_mlhs_add_star", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[106] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[107] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.nil();
                    v3 = p.dispatch("on_mlhs_add_star", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[108] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[109] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_mlhs_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_mlhs_add_star", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[110] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[111] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_mlhs_new");
                    v2 = v1;
                    v3 = p.nil();
                    v4 = p.dispatch("on_mlhs_add_star", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[112] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[114] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_mlhs_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[115] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_mlhs_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v4 = p.dispatch("on_mlhs_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[116] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_mlhs_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[117] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_mlhs_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_mlhs_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[118] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_mlhs_add", v1, v2);
                    yyVal = v3;}
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
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[130] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[131] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v3 = p.dispatch("on_aref_field", v1, v2);
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
                    yyVal = v3;}
  return yyVal;
};
states[134] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[135] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_const_path_field", v1, v2);
                    yyVal = p.const_decl(v3);}
  return yyVal;
};
states[136] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_top_const_field", v1);
                    yyVal = p.const_decl(v2);}
  return yyVal;
};
states[137] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.backref_error(((IRubyObject)yyVals[0+yyTop].value), p.var_field(((IRubyObject)yyVals[0+yyTop].value)));
                    p.error();}
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
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[149] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[150] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v3 = p.dispatch("on_aref_field", v1, v2);
                    yyVal = v3;}
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
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.intern("::");
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_field", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[153] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_field", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[154] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_const_path_field", v1, v2);
                    yyVal = p.const_decl(v3);}
  return yyVal;
};
states[155] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_top_const_field", v1);
                    yyVal = p.const_decl(v2);}
  return yyVal;
};
states[156] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.backref_error(((IRubyObject)yyVals[0+yyTop].value), p.var_field(((IRubyObject)yyVals[0+yyTop].value)));
                    p.error();}
  return yyVal;
};
states[157] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "class/module name must be CONSTANT";
{IRubyObject v1, v2, v3;
                    v1 = p.intern(message);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_class_name_error", v1, v2);
                    yyVal = v3;
                    p.error();}
  return yyVal;
};
states[158] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[159] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_top_const_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[160] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_const_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[161] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_const_path_ref", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[162] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[163] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[164] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[165] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   p.setState(EXPR_ENDFN);
                   yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[166] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[167] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_symbol_literal", v1);
                    yyVal = v2;}

  return yyVal;
};
states[168] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[169] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[170] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_FNAME|EXPR_FITEM);
  return yyVal;
};
states[171] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-3+yyTop].value).push(p.getContext(), p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
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
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[221] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[222] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = yyVals[0+yyTop].value;
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
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[242] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[243] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_assign", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[244] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_opassign", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[245] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[246] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[247] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[248] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[249] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[250] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[251] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.var_field(((IRubyObject)yyVals[-3+yyTop].value));
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_opassign", v1, v2, v3);
                    yyVal = p.backref_error(((IRubyObject)yyVals[-3+yyTop].value), v4);
                    p.error();}
  return yyVal;
};
states[252] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot2", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[253] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot3", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[254] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.nil();
                    v3 = p.dispatch("on_dot2", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[255] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.nil();
                    v3 = p.dispatch("on_dot3", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[256] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = p.nil();
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot2", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[257] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = p.nil();
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot3", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[258] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), PLUS, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[259] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), MINUS, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[260] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), STAR, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[261] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), SLASH, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[262] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), PERCENT, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[263] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), STAR_STAR, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[264] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), STAR_STAR, ((IRubyObject)yyVals[0+yyTop].value), p.src_line()), MINUS_AT);
  return yyVal;
};
states[265] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(((IRubyObject)yyVals[0+yyTop].value), PLUS_AT);
  return yyVal;
};
states[266] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(((IRubyObject)yyVals[0+yyTop].value), MINUS_AT);
  return yyVal;
};
states[267] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), OR, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[268] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), CARET, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[269] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), AMPERSAND, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[270] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), LT_EQ_RT, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[271] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[272] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), EQ_EQ, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[273] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), EQ_EQ_EQ, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[274] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), BANG_EQ, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[275] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.match_op(((IRubyObject)yyVals[-2+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[276] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), BANG_TILDE, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[277] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((IRubyObject)yyVals[0+yyTop].value)), BANG);
  return yyVal;
};
states[278] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(((IRubyObject)yyVals[0+yyTop].value), TILDE);
  return yyVal;
};
states[279] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), LT_LT, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[280] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), GT_GT, ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[281] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.logop(((IRubyObject)yyVals[-2+yyTop].value), AMPERSAND_AMPERSAND, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[282] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.logop(((IRubyObject)yyVals[-2+yyTop].value), OR_OR, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[283] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_defined = false;                    
                    yyVal = p.new_defined(yyVals[yyTop - count + 1].start(), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[284] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[285] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[286] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-5+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_ifop", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[287] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[289] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_rescue = ((LexContext)yyVals[-1+yyTop].value).in_rescue;
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_rescue_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[290] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((IRubyObject)yyVals[0+yyTop].value)), NOT);
{IRubyObject v1, v2, v3;
                    v1 = p.intern("!");
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_unary", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[291] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = GT;
  return yyVal;
};
states[292] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = LT;
  return yyVal;
};
states[293] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = GT_EQ;
  return yyVal;
};
states[294] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = LT_EQ;
  return yyVal;
};
states[295] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[296] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.warning(p.src_line(), "comparison '" + ((ByteList)yyVals[-1+yyTop].value) + "' after comparison");
                    yyVal = p.call_bin_op(((IRubyObject)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value), p.src_line());
  return yyVal;
};
states[297] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = (LexContext) p.getLexContext().clone();
  return yyVal;
};
states[298] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_defined = true;
                    yyVal = ((LexContext)yyVals[0+yyTop].value);
  return yyVal;
};
states[299] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_rescue = LexContext.InRescue.AFTER_RESCUE;
                    yyVal = ((LexContext)yyVals[0+yyTop].value);
  return yyVal;
};
states[300] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((IRubyObject)yyVals[0+yyTop].value));
                    yyVal = p.makeNullNil(((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[302] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[303] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((RubyArray)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_bare_assoc_hash", v1);
                    v3 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v4 = v2;
                    v5 = p.dispatch("on_args_add", v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[304] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[305] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((IRubyObject)yyVals[0+yyTop].value));
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[306] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_rescue = ((LexContext)yyVals[-1+yyTop].value).in_rescue;
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_rescue_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[307] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v2 = p.dispatch("on_arg_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[308] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[309] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[314] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[315] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((RubyArray)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_bare_assoc_hash", v1);
                    v3 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v4 = v2;
                    v5 = p.dispatch("on_args_add", v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[316] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[317] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_args_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[318] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[319] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[320] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_args_add_block", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[321] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[322] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[323] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_args_add_block", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[324] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[325] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[326] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);}
  return yyVal;
};
states[327] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.nil();

  return yyVal;
};
states[328] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[329] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.fals();

  return yyVal;
};
states[330] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_args_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[331] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_args_add_star", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[332] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_args_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[333] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_args_add_star", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[334] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
{                    yyVal = p.get_value(((IRubyObject)yyVals[0+yyTop].value));}
  return yyVal;
};
states[335] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.nil();}
  return yyVal;
};
states[336] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[337] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[338] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.dispatch("on_mrhs_new_from_args", v1);
                    v3 = v2;
                    v4 = ((IRubyObject)yyVals[0+yyTop].value);
                    v5 = p.dispatch("on_mrhs_add", v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[339] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = p.dispatch("on_mrhs_new_from_args", v1);
                    v3 = v2;
                    v4 = ((IRubyObject)yyVals[0+yyTop].value);
                    v5 = p.dispatch("on_mrhs_add_star", v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[340] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_mrhs_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_mrhs_add_star", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[345] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[346] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[347] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[348] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[351] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[352] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getCmdArgumentState().push0();
  return yyVal;
};
states[353] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getCmdArgumentState().pop();
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_begin", v1);
                    yyVal = v2;}
  return yyVal;
};
states[354] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_ENDARG); 
  return yyVal;
};
states[355] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.dispatch("on_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[356] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[357] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_const_path_ref", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[358] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_top_const_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[359] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v2 = p.dispatch("on_array", v1);
                    yyVal = v2;}
  return yyVal;
};
states[360] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v2 = p.dispatch("on_hash", v1);
                    yyVal = v2;}
  return yyVal;
};
states[361] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_return0");
                    yyVal = v1;}
  return yyVal;
};
states[362] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_paren", v1);
                    v3 = v2;
                    v4 = p.dispatch("on_yield", v3);
                    yyVal = v4;}
  return yyVal;
};
states[363] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = p.dispatch("on_paren", v2);
                    v4 = v3;
                    v5 = p.dispatch("on_yield", v4);
                    yyVal = v5;}
  return yyVal;
};
states[364] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_yield0");
                    yyVal = v1;}
  return yyVal;
};
states[365] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_defined = false;
                    yyVal = p.new_defined(yyVals[yyTop - count + 1].start(), ((IRubyObject)yyVals[-1+yyTop].value));
  return yyVal;
};
states[366] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(((IRubyObject)yyVals[-1+yyTop].value)), NOT);
  return yyVal;
};
states[367] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.call_uni_op(p.method_cond(p.nil()), NOT);
  return yyVal;
};
states[368] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[370] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_method_add_block", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[371] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[372] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v4 = p.dispatch("on_if", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[373] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v4 = p.dispatch("on_unless", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[374] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_while", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[375] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_until", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[376] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.case_labels;
                    p.case_labels = p.getRuntime().getNil();
  return yyVal;
};
states[377] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_case", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[378] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.case_labels;
                    p.case_labels = null;
  return yyVal;
};
states[379] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = p.nil();
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_case", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[380] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_case", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[381] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v4 = p.dispatch("on_for", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[382] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.begin_definition("class");
  return yyVal;
};
states[383] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v3 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v4 = p.dispatch("on_class", v1, v2, v3);
                    yyVal = v4;}
                    LexContext ctxt = p.getLexContext();
                    p.popCurrentScope();
                    ctxt.in_class = ((LexContext)yyVals[-5+yyTop].value).in_class;
                    ctxt.cant_return = ((LexContext)yyVals[-5+yyTop].value).cant_return;
                    ctxt.shareable_constant_value = ((LexContext)yyVals[-5+yyTop].value).shareable_constant_value;
  return yyVal;
};
states[384] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.begin_definition(null);
  return yyVal;
};
states[385] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_sclass", v1, v2);
                    yyVal = v3;}
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_def = ((LexContext)yyVals[-6+yyTop].value).in_def;
                    ctxt.in_class = ((LexContext)yyVals[-6+yyTop].value).in_class;
                    ctxt.cant_return = ((LexContext)yyVals[-6+yyTop].value).cant_return;
                    ctxt.shareable_constant_value = ((LexContext)yyVals[-6+yyTop].value).shareable_constant_value;
                    p.popCurrentScope();
  return yyVal;
};
states[386] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.begin_definition("module");
  return yyVal;
};
states[387] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_module", v1, v2);
                    yyVal = v3;}
                    p.popCurrentScope();
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_class = ((LexContext)yyVals[-4+yyTop].value).in_class;
                    ctxt.cant_return = ((LexContext)yyVals[-4+yyTop].value).cant_return;
                    ctxt.shareable_constant_value = ((LexContext)yyVals[-4+yyTop].value).shareable_constant_value;
  return yyVal;
};
states[388] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.push_end_expect_token_locations(yyVals[yyTop - count + 1].start());
  return yyVal;
};
states[389] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[390] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.push_end_expect_token_locations(yyVals[yyTop - count + 1].start());
  return yyVal;
};
states[391] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[392] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = p.dispatch("on_break", v2);
                    yyVal = v3;}
  return yyVal;
};
states[393] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = p.dispatch("on_next", v2);
                    yyVal = v3;}
  return yyVal;
};
states[394] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_redo");
                    yyVal = v1;}
  return yyVal;
};
states[395] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_retry");
                    yyVal = v1;}
  return yyVal;
};
states[396] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((IRubyObject)yyVals[0+yyTop].value));
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value) == null ? p.nil() : ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[397] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.token_info_push("begin", yyVals[yyTop - count + 1]);
                    p.push_end_expect_token_locations(yyVals[yyTop - count + 1].start());
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[398] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[399] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[400] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((NodeExits)yyVals[0+yyTop].value);
  return yyVal;
};
states[401] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[402] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[403] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((NodeExits)yyVals[0+yyTop].value);
  return yyVal;
};
states[404] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = (LexContext) p.getLexContext().clone();
                    p.getLexContext().in_rescue = LexContext.InRescue.BEFORE_RESCUE;
                    p.push_end_expect_token_locations(yyVals[yyTop - count + 1].start());
  return yyVal;
};
states[405] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = (LexContext) p.getLexContext().clone();  
                    p.getLexContext().in_rescue = LexContext.InRescue.BEFORE_RESCUE;
  return yyVal;
};
states[406] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
                    p.getLexContext().in_argdef = true;
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
                    yyVal = p.getLexContext().clone();
                    p.getLexContext().in_rescue = LexContext.InRescue.AFTER_RESCUE;
  return yyVal;
};
states[410] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.getLexContext().clone();
  return yyVal;
};
states[411] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[412] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[413] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[414] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[415] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.compile_error("syntax error, unexpected end-of-input");
  return yyVal;
};
states[416] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = p.getLexContext();
                    if (ctxt.cant_return && !p.dyna_in_block()) {
                        p.compile_error("Invalid return in class/module body");
                    }
  return yyVal;
};
states[417] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = p.getLexContext();
                    if (ctxt.in_defined && !ctxt.in_def && !p.isEval()) {
                        p.compile_error("Invalid yield");
                    }
  return yyVal;
};
states[418] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  return yyVal;
};
states[419] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  return yyVal;
};
states[420] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  return yyVal;
};
states[424] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.escape(((IRubyObject)yyVals[0+yyTop].value));
                    v4 = p.dispatch("on_elsif", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[426] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_else", v1);
                    yyVal = v2;}
  return yyVal;
};
states[428] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  return yyVal;
};
states[429] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, ((IRubyObject)yyVals[0+yyTop].value));

  return yyVal;
};
states[430] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_mlhs_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[431] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_mlhs_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_mlhs_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[432] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_mlhs_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[433] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);}
  return yyVal;
};
states[434] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_mlhs_add_star", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[435] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[436] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_mlhs_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_mlhs_add_star", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[437] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[438] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    ByteList id = yyVals[yyTop - count + 2].id;
                      yyVal = p.assignable(id, ((IRubyObject)yyVals[0+yyTop].value));

  return yyVal;
};
states[439] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.nil();}
  return yyVal;
};
states[441] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.symbolID(LexingCommon.NIL);

  return yyVal;
};
states[442] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = false;
  return yyVal;
};
states[444] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-3+yyTop].value), ((IRubyObject)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[445] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-1+yyTop].value), (ByteList) null, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[446] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(p.src_line(), null, ((IRubyObject)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[447] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), null, (ByteList) null, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[448] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[449] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(p.src_line(), null, (ByteList) null, (ByteList) null);
  return yyVal;
};
states[450] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_excessed_comma");
                    yyVal = v1;}
  return yyVal;
};
states[451] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-5+yyTop].value), ((RubyArray)yyVals[-3+yyTop].value), ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[452] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-7+yyTop].value), ((RubyArray)yyVals[-5+yyTop].value), ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[453] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[454] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-5+yyTop].value), ((RubyArray)yyVals[-3+yyTop].value), null, ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[455] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-3+yyTop].value), null, ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[456] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-1+yyTop].value), null, ((IRubyObject)yyVals[0+yyTop].value), null, (ArgsTailHolder) null);
  return yyVal;
};
states[457] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-5+yyTop].value), null, ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[458] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-1+yyTop].value), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[459] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-3+yyTop].value), ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[460] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-5+yyTop].value), ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[461] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[462] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-3+yyTop].value), null, ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[463] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[464] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[465] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[466] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = null;

  return yyVal;
};
states[467] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setCommandStart(true);
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[468] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[469] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[470] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[471] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-1+yyTop].value);}
  return yyVal;
};
states[472] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[473] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.getContext(), p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[474] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.new_bv(yyVals[yyTop - count + 1].id);
{                    yyVal = p.get_value(((IRubyObject)yyVals[0+yyTop].value));}
  return yyVal;
};
states[475] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[476] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.resetMaxNumParam();
  return yyVal;
};
states[477] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.numparam_push();
  return yyVal;
};
states[478] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.it_id();
                    p.set_it_id(null);
  return yyVal;
};
states[479] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pushBlockScope();
                    yyVal = p.getLeftParenBegin();
                    p.setLeftParenBegin(p.getParenNest());
  return yyVal;
};
states[480] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getCmdArgumentState().push0();
  return yyVal;
};
states[481] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[482] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = false;
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.dispatch("on_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[483] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = false;
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[484] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.token_info_pop("}", yyVals[yyTop - count + 1]);
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[485] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.push_end_expect_token_locations(yyVals[yyTop - count + 1].start());
  return yyVal;
};
states[486] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[487] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[488] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /* Workaround for JRUBY-2326 (MRI does not enter this production for some reason)*/
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_method_add_block", v1, v2);
                    yyVal = v3;}
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
states[491] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_fcall", v1);
                    v3 = v2;
                    v4 = ((IRubyObject)yyVals[0+yyTop].value);
                    v5 = p.dispatch("on_method_add_arg", v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[495] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[496] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[497] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.intern("::");
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_call", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[498] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[499] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[500] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_super", v1);
                    yyVal = v2;}
  return yyVal;
};
states[501] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_zsuper");
                    yyVal = v1;}
  return yyVal;
};
states[502] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v3 = p.dispatch("on_aref", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[503] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[504] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[505] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pushBlockScope();
  return yyVal;
};
states[506] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[507] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pushBlockScope();
                    p.getCmdArgumentState().push0();
  return yyVal;
};
states[508] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[509] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_args_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[510] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_args_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_args_add_star", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[511] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_args_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[512] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_args_add_star", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[513] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-3+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.escape(((IRubyObject)yyVals[0+yyTop].value));
                    v4 = p.dispatch("on_when", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[516] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.push_pvtbl();
  return yyVal;
};
states[517] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.push_pktbl();
  return yyVal;
};
states[518] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.getLexContext().clone();
                    p.setState(EXPR_BEG|EXPR_LABEL);
                    p.setCommandStart(false);
                    p.getLexContext().in_kwarg = true;
  return yyVal;
};
states[519] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pvtbl(((Set)yyVals[-3+yyTop].value));
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    p.getLexContext().in_kwarg = ((LexContext)yyVals[-4+yyTop].value).in_kwarg;
  return yyVal;
};
states[520] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-4+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.escape(((IRubyObject)yyVals[0+yyTop].value));
                    v4 = p.dispatch("on_in", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[522] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[524] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = p.dispatch("on_if_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[525] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v3 = p.dispatch("on_unless_mod", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[527] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, ((IRubyObject)yyVals[-1+yyTop].value),
                                                   p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, true, null, null));
  return yyVal;
};
states[528] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, ((IRubyObject)yyVals[-2+yyTop].value), ((RubyArray)yyVals[0+yyTop].value));
  return yyVal;
};
states[529] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_find_pattern(null, ((RubyArray)yyVals[0+yyTop].value));
  return yyVal;
};
states[530] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, null, ((RubyArray)yyVals[0+yyTop].value));
  return yyVal;
};
states[531] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern(null, ((RubyArray)yyVals[0+yyTop].value));
  return yyVal;
};
states[533] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.symbolID(EQ_GT);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_binary", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[535] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.symbolID(OR);
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_binary", v1, v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[537] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Set)yyVals[0+yyTop].value);
{                    yyVal = p.get_value(((Set)yyVals[0+yyTop].value));}
  return yyVal;
};
states[538] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Set)yyVals[0+yyTop].value);
{                    yyVal = p.get_value(((Set)yyVals[0+yyTop].value));}
  return yyVal;
};
states[541] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), ((IRubyObject)yyVals[-3+yyTop].value), null, ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[542] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_find_pattern(((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[543] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_hash_pattern(((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[544] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), ((IRubyObject)yyVals[-2+yyTop].value), null,
                                                    p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, false, null, null));
  return yyVal;
};
states[545] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), ((IRubyObject)yyVals[-3+yyTop].value), null, ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[546] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_find_pattern(((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[547] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = p.new_hash_pattern(((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[548] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), ((IRubyObject)yyVals[-2+yyTop].value), null,
                            p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, false, null, null));
  return yyVal;
};
states[549] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, null, ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[550] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_find_pattern(null, ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[551] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern(yyVals[yyTop - count + 1].start(), null, null,
                            p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, false, null, null));
  return yyVal;
};
states[552] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_kwarg = false;
  return yyVal;
};
states[553] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-4+yyTop].value));
                    p.getLexContext().in_kwarg = ((LexContext)yyVals[-3+yyTop].value).in_kwarg;
                    yyVal = p.new_hash_pattern(null, ((RubyArray)yyVals[-1+yyTop].value));
  return yyVal;
};
states[554] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern(null, p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), p.none(), null));
  return yyVal;
};
states[555] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[556] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                        yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), p.new_array(((IRubyObject)yyVals[0+yyTop].value)), false, null, null);

  return yyVal;
};
states[557] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[0+yyTop].value), true, null, null);
  return yyVal;
};
states[558] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
			RubyArray pre_args = ((RubyArray)yyVals[-1+yyTop].value).concat(((RubyArray)yyVals[0+yyTop].value));
			yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), pre_args, false, null, null);

  return yyVal;
};
states[559] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-1+yyTop].value), true, ((IRubyObject)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[560] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-3+yyTop].value), true, ((IRubyObject)yyVals[-2+yyTop].value), ((RubyArray)yyVals[0+yyTop].value));
  return yyVal;
};
states[561] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((RubyArray)yyVals[0+yyTop].value);
  return yyVal;
};
states[562] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((RubyArray)yyVals[-1+yyTop].value);
  return yyVal;
};
states[563] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.getContext(), p.get_value(((RubyArray)yyVals[-1+yyTop].value)));;}
  return yyVal;
};
states[564] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, true, ((IRubyObject)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[565] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_array_pattern_tail(yyVals[yyTop - count + 1].start(), null, true, ((IRubyObject)yyVals[-2+yyTop].value), ((RubyArray)yyVals[0+yyTop].value));
  return yyVal;
};
states[566] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_find_pattern_tail(yyVals[yyTop - count + 1].start(), ((IRubyObject)yyVals[-4+yyTop].value), ((RubyArray)yyVals[-2+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[567] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.error_duplicate_pattern_variable(yyVals[yyTop - count + 2].id);
  return yyVal;
};
states[568] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
{                    yyVal = p.var_field(p.nil());}
  return yyVal;
};
states[570] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.getContext(), p.get_value(((RubyArray)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[571] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[572] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-2+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[573] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[574] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-1+yyTop].value), null);
  return yyVal;
};
states[575] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_hash_pattern_tail(yyVals[yyTop - count + 1].start(), null, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[576] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(((RubyArray)yyVals[0+yyTop].value));}
  return yyVal;
};
states[577] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.getContext(), ((RubyArray)yyVals[0+yyTop].value));;}
  return yyVal;
};
states[578] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.error_duplicate_pattern_key(yyVals[yyTop - count + 1].id);
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[-1+yyTop].value)),  p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[579] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.error_duplicate_pattern_key(yyVals[yyTop - count + 1].id);
                    if (yyVals[yyTop - count + 1].id != null && !p.is_local_id(yyVals[yyTop - count + 1].id)) {
                        p.yyerror("key must be valid as local variables");
                    }
                    p.error_duplicate_pattern_variable(yyVals[yyTop - count + 1].id);
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)),  p.nil());}
  return yyVal;
};
states[581] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      if (true) {
                        yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
                      }

                    else {
                        p.yyerror("symbol literal with interpolation is not allowed");
                        yyVal = null;
                    }
  return yyVal;
};
states[582] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[583] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                       yyVal = null;

  return yyVal;
};
states[584] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                       yyVal = p.symbolID(KWNOREST);

  return yyVal;
};
states[585] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[586] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[588] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot2", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[589] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot3", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[590] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.nil();
                    v3 = p.dispatch("on_dot2", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[591] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.nil();
                    v3 = p.dispatch("on_dot3", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[595] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = p.nil();
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot2", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[596] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = p.nil();
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_dot3", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[601] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[602] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[603] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[604] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
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
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
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
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[611] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[612] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[613] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[614] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[615] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[616] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_begin", v1);
                    yyVal = v2;}
  return yyVal;
};
states[617] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_top_const_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[618] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_const_path_ref", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[619] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[620] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = p.escape(((IRubyObject)yyVals[-4+yyTop].value));
                    v2 = p.escape(((IRubyObject)yyVals[-3+yyTop].value));
                    v3 = p.escape(((IRubyObject)yyVals[-1+yyTop].value));
                    v4 = p.escape(((IRubyObject)yyVals[0+yyTop].value));
                    v5 = p.dispatch("on_rescue", v1, v2, v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[621] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null; 
  return yyVal;
};
states[622] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[623] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);}
  return yyVal;
};
states[625] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[627] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_ensure", v1);
                    yyVal = v2;}
  return yyVal;
};
states[629] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[630] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[631] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);}
  return yyVal;
};
states[632] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[633] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[634] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_string_concat", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[635] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = p.heredoc_dedent(((IRubyObject)yyVals[-1+yyTop].value));
                    v2 = p.dispatch("on_string_literal", v1);
                    yyVal = v2;}
  return yyVal;
};
states[636] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = p.heredoc_dedent(((IRubyObject)yyVals[-1+yyTop].value));
                    v2 = p.dispatch("on_xstring_literal", v1);
                    yyVal = v2;}
  return yyVal;
};
states[637] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_regexp(yyVals[yyTop - count + 1].start(), ((IRubyObject)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[638] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  return yyVal;
};
states[640] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_array", v1);
                    yyVal = v2;}
  return yyVal;
};
states[641] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_words_new");
                    yyVal = v1;}
  return yyVal;
};
states[642] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_words_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[643] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
{IRubyObject v1, v2, v3, v4;
                    v1 = p.dispatch("on_word_new");
                    v2 = v1;
                    v3 = ((IRubyObject)yyVals[0+yyTop].value);
                    v4 = p.dispatch("on_word_add", v2, v3);
                    yyVal = v4;}
  return yyVal;
};
states[644] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_word_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[645] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_array", v1);
                    yyVal = v2;}
  return yyVal;
};
states[646] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_symbols_new");
                    yyVal = v1;}
  return yyVal;
};
states[647] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_symbols_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[648] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_array", v1);
                    yyVal = v2;}
  return yyVal;
};
states[649] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_array", v1);
                    yyVal = v2;}
  return yyVal;
};
states[650] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_qwords_new");
                    yyVal = v1;}
  return yyVal;
};
states[651] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_qwords_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[652] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_qsymbols_new");
                    yyVal = v1;}
  return yyVal;
};
states[653] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v3 = p.dispatch("on_qsymbols_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[654] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    ByteList aChar = ByteList.create("");
                    aChar.setEncoding(p.getEncoding());
                    yyVal = p.createStr(aChar, 0);
{IRubyObject v1;
                    v1 = p.dispatch("on_string_content");
                    yyVal = v1;}
  return yyVal;
};
states[655] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_string_add", v1, v2);
                    yyVal = v3;}
                    /* JRuby changed (removed)*/
  return yyVal;
};
states[656] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_xstring_new");
                    yyVal = v1;}
  return yyVal;
};
states[657] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_xstring_add", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[658] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_regexp_new");
                    yyVal = v1;}
  return yyVal;
};
states[659] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /* FIXME: mri is different here.*/
			yyVal = p.dispatch("on_regexp_add", ((IRubyObject)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));

  return yyVal;
};
states[660] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
{                    yyVal = p.ripper_new_yylval(null, p.get_value(((IRubyObject)yyVals[0+yyTop].value)), ((IRubyObject)yyVals[0+yyTop].value));}
  return yyVal;
};
states[661] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.getStrTerm();
                    p.setStrTerm(null);
                    p.setState(EXPR_BEG);
  return yyVal;
};
states[662] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setStrTerm(((StrTerm)yyVals[-1+yyTop].value));
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_string_dvar", v1);
                    yyVal = v2;}
  return yyVal;
};
states[663] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = p.getStrTerm();
                   p.setStrTerm(null);
                   p.getConditionState().push0();
                   p.getCmdArgumentState().push0();
  return yyVal;
};
states[664] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = p.getState();
                   p.setState(EXPR_BEG);
  return yyVal;
};
states[665] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = p.getBraceNest();
                   p.setBraceNest(0);
  return yyVal;
};
states[666] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = p.getHeredocIndent();
                   p.setHeredocIndent(0);
  return yyVal;
};
states[667] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[670] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[674] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_END);
{IRubyObject v1, v2, v3, v4;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_symbol", v1);
                    v3 = v2;
                    v4 = p.dispatch("on_symbol_literal", v3);
                    yyVal = v4;}
  return yyVal;
};
states[677] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_END);

{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_dyna_symbol", v1);
                    yyVal = v2;}
  return yyVal;
};
states[678] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);  
  return yyVal;
};
states[679] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = p.intern("-@");
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_unary", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[680] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[681] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[682] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[683] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[687] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (p.id_is_var(yyVals[yyTop - count + 1].id)) {
                        yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop].value));
                    } else {
                        yyVal = p.dispatch("on_vcall", ((IRubyObject)yyVals[0+yyTop].value));
                    }

  return yyVal;
};
states[688] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (p.id_is_var(yyVals[yyTop - count + 1].id)) {
                        yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop].value));
                    } else {
                        yyVal = p.dispatch("on_vcall", ((IRubyObject)yyVals[0+yyTop].value));
                    }

  return yyVal;
};
states[689] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (p.id_is_var(yyVals[yyTop - count + 1].id)) {
                        yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop].value));
                    } else {
                        yyVal = p.dispatch("on_vcall", ((IRubyObject)yyVals[0+yyTop].value));
                    }

  return yyVal;
};
states[690] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (p.id_is_var(yyVals[yyTop - count + 1].id)) {
                        yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop].value));
                    } else {
                        yyVal = p.dispatch("on_vcall", ((IRubyObject)yyVals[0+yyTop].value));
                    }

  return yyVal;
};
states[691] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (p.id_is_var(yyVals[yyTop - count + 1].id)) {
                        yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop].value));
                    } else {
                        yyVal = p.dispatch("on_vcall", ((IRubyObject)yyVals[0+yyTop].value));
                    }

  return yyVal;
};
states[692] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[693] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[694] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[695] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[696] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[697] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
  return yyVal;
};
states[698] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_var_ref", v1);
                    yyVal = v2;}
                    /* mri:keyword_variable*/
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

  return yyVal;
};
states[705] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[706] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[707] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[708] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[709] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

  return yyVal;
};
states[710] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.assignable(yyVals[yyTop - count + 1].id, p.var_field(((IRubyObject)yyVals[0+yyTop].value)));

                    /* mri:keyword_variable*/
  return yyVal;
};
states[711] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[712] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[713] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   p.setState(EXPR_BEG);
                   p.setCommandStart(true);
  return yyVal;
};
states[714] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
  return yyVal;
};
states[715] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.nil();}
  return yyVal;
};
states[717] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = false;
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, null, null, 
                                    p.new_args_tail(p.src_line(), null, (ByteList) null, (ByteList) null));
  return yyVal;
};
states[718] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_paren", v1);
                    yyVal = v2;}
                    p.setState(EXPR_BEG);
                    p.getLexContext().in_argdef = false;
                    p.setCommandStart(true);
  return yyVal;
};
states[719] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[720] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.getLexContext().clone();
                    p.getLexContext().in_kwarg = true;
                    p.getLexContext().in_argdef = true;
                    p.setState(p.getState() | EXPR_LABEL);
  return yyVal;
};
states[721] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = p.getLexContext();
                    ctxt.in_kwarg = ((LexContext)yyVals[-2+yyTop].value).in_kwarg;
                    ctxt.in_argdef = false;
                    yyVal = ((IRubyObject)yyVals[-1+yyTop].value);
                    p.setState(EXPR_BEG);
                    p.setCommandStart(true);
  return yyVal;
};
states[722] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-3+yyTop].value), ((IRubyObject)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[723] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-1+yyTop].value), (ByteList) null, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[724] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(p.src_line(), null, ((IRubyObject)yyVals[-1+yyTop].value), ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[725] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), null, (ByteList) null, ((IRubyObject)yyVals[0+yyTop].value));
  return yyVal;
};
states[726] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.add_forwarding_args();
                    yyVal = p.new_args_tail(yyVals[yyTop - count + 1].start(), null, ((IRubyObject)yyVals[0+yyTop].value), FWD_BLOCK);
  return yyVal;
};
states[727] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[728] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args_tail(p.src_line(), null, (ByteList) null, (ByteList) null);
  return yyVal;
};
states[729] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-5+yyTop].value), ((RubyArray)yyVals[-3+yyTop].value), ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[730] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-7+yyTop].value), ((RubyArray)yyVals[-5+yyTop].value), ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[731] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[732] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-5+yyTop].value), ((RubyArray)yyVals[-3+yyTop].value), null, ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[733] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-3+yyTop].value), null, ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[734] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-5+yyTop].value), null, ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[735] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), ((RubyArray)yyVals[-1+yyTop].value), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[736] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-3+yyTop].value), ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[737] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-5+yyTop].value), ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[738] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[739] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, ((RubyArray)yyVals[-3+yyTop].value), null, ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[740] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, ((IRubyObject)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[741] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, ((IRubyObject)yyVals[-3+yyTop].value), ((RubyArray)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[742] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(yyVals[yyTop - count + 1].start(), null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[743] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.new_args(p.src_line(), null, null, null, null, (ArgsTailHolder) null);
  return yyVal;
};
states[744] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1;
                    v1 = p.dispatch("on_args_forward");
                    yyVal = v1;}
  return yyVal;
};
states[745] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "formal argument cannot be a constant";
{IRubyObject v1, v2, v3;
                    v1 = p.intern(message);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_param_error", v1, v2);
                    yyVal = v3;
                    p.error();}
  return yyVal;
};
states[746] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "formal argument cannot be an instance variable";
{IRubyObject v1, v2, v3;
                    v1 = p.intern(message);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_param_error", v1, v2);
                    yyVal = v3;
                    p.error();}
  return yyVal;
};
states[747] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "formal argument cannot be a global variable";
{IRubyObject v1, v2, v3;
                    v1 = p.intern(message);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_param_error", v1, v2);
                    yyVal = v3;
                    p.error();}
  return yyVal;
};
states[748] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    String message = "formal argument cannot be a class variable";
{IRubyObject v1, v2, v3;
                    v1 = p.intern(message);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_param_error", v1, v2);
                    yyVal = v3;
                    p.error();}
  return yyVal;
};
states[749] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value); /* Not really reached*/
  return yyVal;
};
states[750] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.formal_argument(yyVals[yyTop - count + 1].id, ((IRubyObject)yyVals[0+yyTop].value));
                    p.ordinalMaxNumParam();
  return yyVal;
};
states[751] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    RubySymbol name = p.get_id(((IRubyObject)yyVals[0+yyTop].value));
                    p.setCurrentArg(name);
                      p.arg_var(yyVals[yyTop - count + 1].id);
                      yyVal = ((IRubyObject)yyVals[0+yyTop].value);


  return yyVal;
};
states[752] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setCurrentArg(null);
{                    yyVal = p.get_value(((IRubyObject)yyVals[0+yyTop].value));}
  return yyVal;
};
states[753] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_mlhs_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[754] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[755] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.getContext(), p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[756] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.formal_argument(yyVals[yyTop - count + 1].id, ((IRubyObject)yyVals[0+yyTop].value));
                    p.arg_var(yyVals[yyTop - count + 1].id);
                    p.setCurrentArg(p.get_id(((IRubyObject)yyVals[0+yyTop].value)));
                    p.ordinalMaxNumParam();
                    p.getLexContext().in_argdef = false;
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[757] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setCurrentArg(null);
                    p.getLexContext().in_argdef = true;
                      yyVal = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, ((IRubyObject)yyVals[-1+yyTop].value)), ((IRubyObject)yyVals[0+yyTop].value));

  return yyVal;
};
states[758] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setCurrentArg(null);
                    p.getLexContext().in_argdef = true;
                      yyVal = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, ((IRubyObject)yyVals[0+yyTop].value)), p.nil());

  return yyVal;
};
states[759] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = true;
                      yyVal = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, ((IRubyObject)yyVals[-1+yyTop].value)), ((IRubyObject)yyVals[0+yyTop].value));

  return yyVal;
};
states[760] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = true;
                      yyVal = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, ((IRubyObject)yyVals[0+yyTop].value)), p.fals());

  return yyVal;
};
states[761] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[762] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.getContext(), p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[763] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[764] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.getContext(), p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[765] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[766] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[767] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = p.nil();
                    v2 = p.dispatch("on_nokw_param", v1);
                    yyVal = v2;}
  return yyVal;
};
states[768] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.shadowing_lvar(yyVals[yyTop - count + 2].id);
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_kwrest_param", v1);
                    yyVal = v2;}
  return yyVal;
};
states[769] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = p.nil();
                    v2 = p.dispatch("on_kwrest_param", v1);
                    yyVal = v2;}
  return yyVal;
};
states[770] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, ((IRubyObject)yyVals[-2+yyTop].value)), ((IRubyObject)yyVals[0+yyTop].value));

  return yyVal;
};
states[771] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.getLexContext().in_argdef = true;
                    p.setCurrentArg(null);
                      yyVal = p.new_assoc(p.assignable(yyVals[yyTop - count + 1].id, ((IRubyObject)yyVals[-2+yyTop].value)), ((IRubyObject)yyVals[0+yyTop].value));

  return yyVal;
};
states[772] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[773] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.getContext(), p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[774] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[775] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.getContext(), p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[776] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = STAR;
  return yyVal;
};
states[777] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[778] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[779] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = p.nil();
                    v2 = p.dispatch("on_rest_param", v1);
                    yyVal = v2;}
  return yyVal;
};
states[780] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = AMPERSAND;
  return yyVal;
};
states[781] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[782] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[783] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = p.arg_var(p.shadowing_lvar(ANON_BLOCK));
                        yyVal = p.dispatch("on_blockarg", p.nil());

  return yyVal;
};
states[784] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[785] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[786] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.value_expr(((IRubyObject)yyVals[0+yyTop].value));
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[787] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.setState(EXPR_BEG);
  return yyVal;
};
states[788] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_paren", v1);
                    yyVal = v2;}
  return yyVal;
};
states[789] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  return yyVal;
};
states[790] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((RubyArray)yyVals[-1+yyTop].value);
                    v2 = p.dispatch("on_assoclist_from_args", v1);
                    yyVal = v2;}
  return yyVal;
};
states[791] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = p.new_array(p.get_value(((IRubyObject)yyVals[0+yyTop].value)));}
  return yyVal;
};
states[792] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{                    yyVal = ((RubyArray)yyVals[-2+yyTop].value).push(p.getContext(), p.get_value(((IRubyObject)yyVals[0+yyTop].value)));;}
  return yyVal;
};
states[793] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_assoc_new", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[794] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[-1+yyTop].value);
                    v2 = ((IRubyObject)yyVals[0+yyTop].value);
                    v3 = p.dispatch("on_assoc_new", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[795] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.nil();
                    v3 = p.dispatch("on_assoc_new", v1, v2);
                    yyVal = v3;}
  return yyVal;
};
states[796] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2, v3, v4, v5;
                    v1 = ((IRubyObject)yyVals[-2+yyTop].value);
                    v2 = p.dispatch("on_dyna_symbol", v1);
                    v3 = v2;
                    v4 = ((IRubyObject)yyVals[0+yyTop].value);
                    v5 = p.dispatch("on_assoc_new", v3, v4);
                    yyVal = v5;}
  return yyVal;
};
states[797] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
{IRubyObject v1, v2;
                    v1 = ((IRubyObject)yyVals[0+yyTop].value);
                    v2 = p.dispatch("on_assoc_splat", v1);
                    yyVal = v2;}
  return yyVal;
};
states[798] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    p.forwarding_arg_check(FWD_KWREST, FWD_ALL, "keyword rest");
{IRubyObject v1, v2;
                    v1 = p.nil();
                    v2 = p.dispatch("on_assoc_splat", v1);
                    yyVal = v2;}
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
states[805] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[806] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[807] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[808] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[809] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[810] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[812] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop].value);
  return yyVal;
};
states[817] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RPAREN;
  return yyVal;
};
states[818] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RBRACKET;
  return yyVal;
};
states[819] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RCURLY;
  return yyVal;
};
states[827] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = null;
  return yyVal;
};
states[828] = (RipperParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  yyVal = null;
  return yyVal;
};
}
					// line 4715 "ripper_RubyParser.out"

}
					// line 15041 "-"
