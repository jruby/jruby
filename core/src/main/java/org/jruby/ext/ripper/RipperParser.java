// created by jay 1.0.2 (c) 2002-2004 ats@cs.rit.edu
// skeleton Java 1.0 (c) 2002 ats@cs.rit.edu

					// line 2 "RipperParser.y"
/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2013-2017 The JRuby Team (jruby@jruby.org)
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

import org.jruby.RubyArray;
import org.jruby.lexer.LexerSource;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.lexer.LexingCommon.EXPR_BEG;
import static org.jruby.lexer.LexingCommon.EXPR_FITEM;
import static org.jruby.lexer.LexingCommon.EXPR_FNAME;
import static org.jruby.lexer.LexingCommon.EXPR_ENDFN;
import static org.jruby.lexer.LexingCommon.EXPR_ENDARG;
import static org.jruby.lexer.LexingCommon.EXPR_END;
import static org.jruby.lexer.LexingCommon.EXPR_LABEL;

public class RipperParser extends RipperParserBase {
    public RipperParser(ThreadContext context, IRubyObject ripper, LexerSource source) {
        super(context, ripper, source);
    }
					// line 52 "-"
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
  public static final int keyword_return = 283;
  public static final int keyword_yield = 284;
  public static final int keyword_super = 285;
  public static final int keyword_self = 286;
  public static final int keyword_nil = 287;
  public static final int keyword_true = 288;
  public static final int keyword_false = 289;
  public static final int keyword_and = 290;
  public static final int keyword_or = 291;
  public static final int keyword_not = 292;
  public static final int modifier_if = 293;
  public static final int modifier_unless = 294;
  public static final int modifier_while = 295;
  public static final int modifier_until = 296;
  public static final int modifier_rescue = 297;
  public static final int keyword_alias = 298;
  public static final int keyword_defined = 299;
  public static final int keyword_BEGIN = 300;
  public static final int keyword_END = 301;
  public static final int keyword__LINE__ = 302;
  public static final int keyword__FILE__ = 303;
  public static final int keyword__ENCODING__ = 304;
  public static final int keyword_do_lambda = 305;
  public static final int tIDENTIFIER = 306;
  public static final int tFID = 307;
  public static final int tGVAR = 308;
  public static final int tIVAR = 309;
  public static final int tCONSTANT = 310;
  public static final int tCVAR = 311;
  public static final int tLABEL = 312;
  public static final int tCHAR = 313;
  public static final int tUPLUS = 314;
  public static final int tUMINUS = 315;
  public static final int tUMINUS_NUM = 316;
  public static final int tPOW = 317;
  public static final int tCMP = 318;
  public static final int tEQ = 319;
  public static final int tEQQ = 320;
  public static final int tNEQ = 321;
  public static final int tGEQ = 322;
  public static final int tLEQ = 323;
  public static final int tANDOP = 324;
  public static final int tOROP = 325;
  public static final int tMATCH = 326;
  public static final int tNMATCH = 327;
  public static final int tDOT = 328;
  public static final int tDOT2 = 329;
  public static final int tDOT3 = 330;
  public static final int tAREF = 331;
  public static final int tASET = 332;
  public static final int tLSHFT = 333;
  public static final int tRSHFT = 334;
  public static final int tANDDOT = 335;
  public static final int tCOLON2 = 336;
  public static final int tCOLON3 = 337;
  public static final int tOP_ASGN = 338;
  public static final int tASSOC = 339;
  public static final int tLPAREN = 340;
  public static final int tLPAREN2 = 341;
  public static final int tRPAREN = 342;
  public static final int tLPAREN_ARG = 343;
  public static final int tLBRACK = 344;
  public static final int tRBRACK = 345;
  public static final int tLBRACE = 346;
  public static final int tLBRACE_ARG = 347;
  public static final int tSTAR = 348;
  public static final int tSTAR2 = 349;
  public static final int tAMPER = 350;
  public static final int tAMPER2 = 351;
  public static final int tTILDE = 352;
  public static final int tPERCENT = 353;
  public static final int tDIVIDE = 354;
  public static final int tPLUS = 355;
  public static final int tMINUS = 356;
  public static final int tLT = 357;
  public static final int tGT = 358;
  public static final int tPIPE = 359;
  public static final int tBANG = 360;
  public static final int tCARET = 361;
  public static final int tLCURLY = 362;
  public static final int tRCURLY = 363;
  public static final int tBACK_REF2 = 364;
  public static final int tSYMBEG = 365;
  public static final int tSTRING_BEG = 366;
  public static final int tXSTRING_BEG = 367;
  public static final int tREGEXP_BEG = 368;
  public static final int tWORDS_BEG = 369;
  public static final int tQWORDS_BEG = 370;
  public static final int tSTRING_DBEG = 371;
  public static final int tSTRING_DVAR = 372;
  public static final int tSTRING_END = 373;
  public static final int tLAMBDA = 374;
  public static final int tLAMBEG = 375;
  public static final int tNTH_REF = 376;
  public static final int tBACK_REF = 377;
  public static final int tSTRING_CONTENT = 378;
  public static final int tINTEGER = 379;
  public static final int tIMAGINARY = 380;
  public static final int tFLOAT = 381;
  public static final int tRATIONAL = 382;
  public static final int tREGEXP_END = 383;
  public static final int tIGNORED_NL = 384;
  public static final int tCOMMENT = 385;
  public static final int tEMBDOC_BEG = 386;
  public static final int tEMBDOC = 387;
  public static final int tEMBDOC_END = 388;
  public static final int tSP = 389;
  public static final int tHEREDOC_BEG = 390;
  public static final int tHEREDOC_END = 391;
  public static final int tSYMBOLS_BEG = 392;
  public static final int tQSYMBOLS_BEG = 393;
  public static final int tDSTAR = 394;
  public static final int tSTRING_DEND = 395;
  public static final int tLABEL_END = 396;
  public static final int tLOWEST = 397;
  public static final int k__END__ = 398;
  public static final int yyErrorCode = 256;

  /** number of final state.
    */
  protected static final int yyFinal = 1;

  /** parser tables.
      Order is mandated by <i>jay</i>.
    */
  protected static final short[] yyLhs = {
//yyLhs 650
    -1,   151,     0,   139,   140,   140,   140,   140,   141,   154,
   141,    37,    36,    38,    38,    38,    38,    44,   155,    44,
   156,    39,    39,    39,    39,    39,    39,    39,    39,    39,
    39,    39,    39,    39,    39,    39,    39,    31,    31,    31,
    31,    31,    31,    31,    31,    61,    61,    61,    40,    40,
    40,    40,    40,    40,    45,    32,    32,    60,    60,   113,
   148,    43,    43,    43,    43,    43,    43,    43,    43,    43,
    43,    43,   116,   116,   127,   127,   117,   117,   117,   117,
   117,   117,   117,   117,   117,   117,    74,    74,   103,   103,
   104,   104,    75,    75,    75,    75,    75,    75,    75,    75,
    75,    75,    75,    75,    75,    75,    75,    75,    75,    75,
    75,    80,    80,    80,    80,    80,    80,    80,    80,    80,
    80,    80,    80,    80,    80,    80,    80,    80,    80,    80,
     8,     8,    30,    30,    30,     7,     7,     7,     7,     7,
   120,   120,   121,   121,    64,   158,    64,     6,     6,     6,
     6,     6,     6,     6,     6,     6,     6,     6,     6,     6,
     6,     6,     6,     6,     6,     6,     6,     6,     6,     6,
     6,     6,     6,     6,     6,     6,   134,   134,   134,   134,
   134,   134,   134,   134,   134,   134,   134,   134,   134,   134,
   134,   134,   134,   134,   134,   134,   134,   134,   134,   134,
   134,   134,   134,   134,   134,   134,   134,   134,   134,   134,
   134,   134,   134,   134,   134,   134,   134,   134,   134,   134,
   134,   134,    41,    41,    41,    41,    41,    41,    41,    41,
    41,    41,    41,    41,    41,    41,    41,    41,    41,    41,
    41,    41,    41,    41,    41,    41,    41,    41,    41,    41,
    41,    41,    41,    41,    41,    41,    41,    41,    41,    41,
    41,   136,   136,   136,   136,    51,    51,    76,    79,    79,
    79,    79,    62,    62,    54,    58,    58,   130,   130,   130,
   130,   130,    52,    52,    52,    52,    52,   160,    56,   107,
   106,   106,    82,    82,    82,    82,    35,    35,    73,    73,
    73,    42,    42,    42,    42,    42,    42,    42,    42,    42,
    42,    42,   161,    42,   162,    42,   163,   164,    42,    42,
    42,    42,    42,    42,    42,    42,    42,    42,    42,    42,
    42,    42,    42,    42,    42,    42,    42,   165,   166,    42,
   167,   168,    42,    42,    42,   169,   170,    42,   171,    42,
   173,   174,    42,   175,    42,   176,    42,   177,   178,    42,
    42,    42,    42,    42,    46,   150,   150,   150,   149,   149,
    49,    49,    47,    47,   129,   129,   131,   131,    87,    87,
   132,   132,   132,   132,   132,   132,   132,   132,   132,    94,
    94,    94,    94,    93,    93,    69,    69,    69,    69,    69,
    69,    69,    69,    69,    69,    69,    69,    69,    69,    69,
    71,    71,    70,    70,    70,   124,   124,   123,   123,   133,
   133,   179,   180,   126,    68,    68,   125,   125,   112,    59,
    59,    59,    59,    22,    22,    22,    22,    22,    22,    22,
    22,    22,   111,   181,   111,   182,   114,   183,   115,    77,
    48,    48,   118,   118,    78,    78,    78,    50,    50,    53,
    53,    28,    28,    28,    15,    16,    16,    16,    17,    18,
    19,    25,    84,    84,    27,    27,    90,    88,    88,    26,
    91,    83,    83,    89,    89,    20,    20,    21,    21,    24,
    24,    23,   184,    23,   185,   186,   187,   188,   189,    23,
    65,    65,    65,    65,     2,     1,     1,     1,     1,    29,
    33,    33,    34,    34,    34,    34,    57,    57,    57,    57,
    57,    57,    57,    57,    57,    57,    57,    57,   119,   119,
   119,   119,   119,   119,   119,   119,   119,   119,   119,   119,
    66,    66,   190,    55,    55,    72,   191,    72,    95,    95,
    95,    95,    92,    92,    67,    67,    67,    67,    67,    67,
    67,    67,    67,    67,    67,    67,    67,    67,    67,   135,
   135,   135,   135,     9,     9,   147,   122,   122,    85,    85,
   144,    96,    96,    97,    97,    98,    98,    99,    99,   142,
   142,   143,   143,    63,   128,   105,   105,    86,    86,    10,
    10,    13,    13,    12,    12,   110,   109,   109,    14,   192,
    14,   100,   100,   101,   101,   102,   102,   102,   102,     3,
     3,     3,     4,     4,     4,     4,     5,     5,     5,    11,
    11,   145,   145,   146,   146,   152,   152,   157,   157,   137,
   138,   159,   159,   159,   172,   172,   153,   153,    81,   108,
    }, yyLen = {
//yyLen 650
     2,     0,     2,     2,     1,     1,     3,     2,     1,     0,
     5,     4,     2,     1,     1,     3,     2,     1,     0,     5,
     0,     4,     3,     3,     3,     2,     3,     3,     3,     3,
     3,     4,     1,     3,     3,     3,     1,     3,     3,     6,
     5,     5,     5,     5,     3,     1,     3,     1,     1,     3,
     3,     3,     2,     1,     1,     1,     1,     1,     4,     3,
     1,     2,     3,     4,     5,     4,     5,     2,     2,     2,
     2,     2,     1,     3,     1,     3,     1,     2,     3,     5,
     2,     4,     2,     4,     1,     3,     1,     3,     2,     3,
     1,     3,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     4,     3,     3,     3,     3,     2,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     4,     3,     3,     3,     3,     2,     1,
     1,     1,     2,     1,     3,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     0,     4,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     3,     3,     6,     5,     5,     5,     5,     4,
     3,     3,     3,     3,     3,     3,     3,     3,     3,     4,
     2,     2,     3,     3,     3,     3,     1,     3,     3,     3,
     3,     3,     2,     2,     3,     3,     3,     3,     3,     6,
     1,     1,     1,     1,     1,     3,     3,     1,     1,     2,
     4,     2,     1,     3,     3,     1,     1,     1,     1,     2,
     4,     2,     1,     2,     2,     4,     1,     0,     2,     2,
     2,     1,     1,     2,     3,     4,     1,     1,     3,     4,
     2,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     0,     4,     0,     3,     0,     0,     5,     3,
     3,     2,     3,     3,     1,     4,     3,     1,     5,     4,
     3,     2,     1,     2,     2,     6,     6,     0,     0,     7,
     0,     0,     7,     5,     4,     0,     0,     9,     0,     6,
     0,     0,     8,     0,     5,     0,     6,     0,     0,     9,
     1,     1,     1,     1,     1,     1,     1,     2,     1,     1,
     1,     5,     1,     2,     1,     1,     1,     3,     1,     3,
     1,     4,     6,     3,     5,     2,     4,     1,     3,     4,
     2,     2,     1,     2,     0,     6,     8,     4,     6,     4,
     2,     6,     2,     4,     6,     2,     4,     2,     4,     1,
     1,     1,     3,     1,     4,     1,     4,     1,     3,     1,
     1,     0,     0,     4,     4,     1,     3,     3,     3,     2,
     4,     5,     5,     2,     4,     4,     3,     3,     3,     2,
     1,     4,     3,     0,     5,     0,     3,     0,     3,     5,
     1,     1,     6,     0,     1,     1,     1,     2,     1,     2,
     1,     1,     1,     1,     1,     1,     1,     2,     3,     3,
     3,     4,     0,     3,     1,     2,     4,     0,     3,     4,
     4,     0,     3,     0,     3,     0,     2,     0,     2,     0,
     2,     1,     0,     3,     0,     0,     0,     0,     0,     8,
     1,     1,     1,     1,     2,     1,     1,     1,     1,     3,
     1,     2,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     0,     4,     0,     3,     0,     3,     4,     2,
     2,     1,     2,     0,     6,     8,     4,     6,     4,     6,
     2,     4,     6,     2,     4,     2,     4,     1,     0,     1,
     1,     1,     1,     1,     1,     1,     1,     3,     1,     3,
     1,     2,     1,     2,     1,     1,     3,     1,     3,     1,
     1,     2,     1,     3,     3,     1,     3,     1,     3,     1,
     1,     2,     1,     1,     1,     2,     2,     0,     1,     0,
     4,     1,     2,     1,     3,     3,     2,     4,     2,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     0,     1,     0,     1,     2,
     2,     0,     1,     1,     1,     1,     1,     2,     0,     0,
    }, yyDefRed = {
//yyDefRed 1108
     1,     0,     0,     0,     0,     0,     0,     0,   312,     0,
     0,     0,   337,   340,     0,     0,     0,   362,   363,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     9,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   465,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   485,   487,   489,     0,     0,   421,   540,
   541,   512,   515,   513,   514,     0,     0,   462,    60,   302,
     0,   466,   303,   304,     0,   305,   306,   301,   463,    32,
    48,   461,   510,     0,     0,     0,     0,     0,     0,     0,
   309,     0,    56,     0,     0,    86,     0,     4,   307,   308,
     0,     0,    72,     0,     2,     0,     5,     0,     7,   360,
   361,   324,     0,     0,   522,   521,   523,   524,     0,     0,
   526,   525,   527,     0,   518,   517,     0,   520,     0,     0,
     0,     0,   133,     0,   364,     0,   310,     0,   353,   186,
   197,   187,   210,   183,   203,   193,   192,   213,   214,   208,
   191,   190,   185,   211,   215,   216,   195,   184,   198,   202,
   204,   196,   189,   205,   212,   207,     0,     0,     0,     0,
   182,   201,   200,   217,   218,   219,   220,   221,   181,   188,
   179,   180,     0,     0,     0,     0,   137,     0,   171,   172,
   168,   150,   151,   152,   159,   156,   158,   153,   154,   173,
   174,   160,   161,   609,   165,   164,   149,   170,   167,   166,
   162,   163,   157,   155,   147,   169,   148,   175,   138,   355,
     0,   608,   139,   206,   199,   209,   194,   176,   177,   178,
   135,   136,   141,   140,   143,     0,   142,   144,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   644,
   645,     0,     0,     0,   646,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   374,   375,     0,     0,     0,     0,     0,   485,
     0,     0,   282,    70,     0,     0,     0,   613,   286,    71,
    69,     0,    68,     0,     0,   439,    67,     0,   638,     0,
     0,    20,     0,     0,     0,   240,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,    14,    13,     0,     0,
     0,     0,     0,   268,     0,     0,     0,   611,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   253,    52,   252,
   507,   506,   508,   504,   505,     0,     0,     0,     0,   472,
   481,   334,   422,   477,   483,   467,   443,   445,   333,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   263,   264,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   262,   261,     0,
     0,     0,     0,   447,   429,   631,   632,     0,     0,     0,
     0,   634,   633,     0,     0,    88,     0,     0,     0,     0,
     0,     0,     3,     0,   433,     0,   331,     0,   511,     0,
   130,     0,   132,   542,   348,     0,     0,     0,     0,     0,
     0,   629,   630,   357,   145,     0,     0,     0,   366,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   647,     0,     0,     0,     0,     0,     0,   345,   616,
   293,   289,     0,   618,     0,     0,   283,   291,     0,   284,
     0,   326,     0,   288,   278,   277,     0,     0,     0,     0,
   330,    51,    22,    24,    23,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   319,    12,     0,
     0,   315,     0,   322,     0,   642,   269,     0,   271,   323,
   612,     0,    90,     0,     0,     0,     0,     0,   494,   492,
   509,   491,   488,   468,   486,   469,   470,   490,     0,     0,
     0,     0,     0,     0,     0,     0,    26,    27,    28,    29,
    30,    49,    50,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   436,     0,   438,     0,     0,   624,     0,     0,
   625,   437,     0,   622,   623,     0,    47,     0,     0,     0,
    44,   230,     0,     0,     0,     0,    37,   222,    34,   292,
     0,     0,     0,     0,    89,    33,    35,   296,     0,    38,
   223,     6,   445,    62,     0,     0,     0,     0,     0,     0,
   134,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   313,     0,   367,     0,     0,     0,     0,     0,
     0,     0,     0,   344,   369,   338,   368,   341,     0,     0,
     0,     0,     0,     0,     0,   615,     0,     0,     0,   290,
   614,   325,   639,     0,     0,   274,   329,    21,     0,     0,
    31,     0,   229,     0,     0,    15,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   495,     0,   471,   474,     0,
   479,     0,   574,   571,   570,   569,   572,   580,   589,     0,
     0,   600,   599,   604,   603,   590,   575,     0,     0,     0,
   597,   425,     0,     0,     0,   567,   587,     0,   551,   578,
   573,     0,     0,     0,     0,   476,     0,   480,     0,   413,
     0,   411,     0,   410,   442,     0,     0,   428,     0,     0,
     0,   435,     0,     0,     0,     0,     0,   276,     0,   434,
   275,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,    87,     0,     0,     0,   351,     0,     0,   441,   354,
   610,     0,     0,     0,   358,   146,   455,     0,     0,   456,
     0,     0,   372,     0,   370,     0,     0,     0,     0,     0,
     0,     0,   343,     0,     0,     0,     0,     0,     0,   617,
   295,   285,     0,   328,    10,     0,   318,   270,    91,     0,
   496,   500,   501,   502,   493,   503,   473,   475,   482,     0,
     0,   376,     0,   378,     0,     0,   601,   605,     0,   565,
     0,     0,   423,     0,   560,     0,   563,     0,   549,   591,
     0,   550,   581,     0,   478,   484,     0,     0,     0,   409,
   585,     0,     0,   392,     0,   595,     0,     0,     0,     0,
     0,   446,     0,   448,    43,   227,    42,   228,    66,     0,
   640,    40,   225,    41,   226,    64,   432,   431,    46,     0,
     0,     0,     0,     0,     0,     0,     0,     0,    59,     0,
   543,   349,   545,   356,   547,     0,     0,     0,   458,   373,
     0,    11,   460,     0,   335,     0,   336,   294,     0,     0,
     0,   346,     0,    19,   497,     0,     0,     0,     0,   577,
     0,     0,   552,   576,     0,     0,     0,     0,   579,     0,
   598,     0,   588,   606,     0,   593,     0,   407,     0,     0,
   402,     0,   390,     0,   405,   412,   391,     0,     0,     0,
     0,     0,     0,   444,     0,     0,    39,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   457,     0,   459,     0,
   450,   449,   451,   339,   342,     0,   498,   377,     0,     0,
     0,   379,   424,     0,   566,   427,   426,     0,   558,     0,
   556,     0,   561,   564,   548,     0,   393,   414,     0,     0,
   586,     0,     0,     0,   596,   321,     0,     0,   419,     0,
   417,   420,   352,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   408,     0,   399,     0,   397,   389,     0,   403,   406,
     0,     0,     0,   416,   359,     0,     0,     0,     0,     0,
   452,   371,   347,     0,     0,     0,   559,     0,   554,   557,
   562,     0,     0,     0,     0,   418,     0,   499,     0,     0,
   401,     0,   395,   398,   404,   555,     0,   396,
    }, yyDgoto = {
//yyDgoto 193
     1,   363,    67,    68,   639,   592,   593,   233,   132,   726,
   727,   453,   728,   729,   220,    69,    70,    71,    72,    73,
   366,   365,    74,   542,   368,    75,    76,   709,    77,    78,
   133,    79,    80,    81,    82,   626,   455,   456,   324,   325,
    84,    85,    86,    87,   326,   253,   316,   804,  1001,   805,
   917,    89,   494,   921,   594,   444,   302,    90,   769,    91,
    92,   616,   617,   730,   235,   834,   255,   731,   732,   867,
   751,   752,   646,   618,    94,    95,   294,   470,   798,   332,
   256,   327,   496,   549,   548,   733,   734,   842,   551,   552,
    98,    99,   849,   957,  1026,   942,   736,   870,   871,   737,
   338,   497,   297,   100,   533,   872,   486,   298,   487,   858,
   738,   436,   414,   633,   554,   586,   101,   102,   651,   257,
   236,   237,   739,  1039,   874,   852,   371,   329,   875,   284,
   498,   843,   844,  1040,   222,   740,   412,   491,   763,   104,
   105,   106,   741,   742,   743,   447,   423,   943,   107,   665,
   459,     2,   262,   263,   313,   515,   505,   492,   649,   526,
   303,   238,   330,   331,   696,   265,   815,   266,   816,   673,
  1005,   636,   460,   634,   909,   448,   450,   648,   915,   372,
   550,   553,   555,   587,   706,   705,   830,   934,  1006,  1051,
   635,   647,   449,
    }, yySindex = {
//yySindex 1108
     0,     0, 19266, 20563, 22240, 22627, 23369, 23260,     0, 21724,
 21724, 18482,     0,     0, 22369, 19653, 19653,     0,     0, 19653,
  -239,  -204,     0,     0,     0,     0,    64, 23151,   158,     0,
  -118,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 21853, 21853,  1332,   -51, 19395,     0, 20043, 20433, 18750,
 21853, 21982, 23477,     0,     0,     0,   230,   290,     0,     0,
     0,     0,     0,     0,     0,   317,   320,     0,     0,     0,
   -87,     0,     0,     0,  -119,     0,     0,     0,     0,     0,
     0,     0,     0,  1342,   -49,  5956,     0,    17,    33,   450,
     0,   267,     0,   -29,   285,     0,   332,     0,     0,     0,
 22498,   349,     0,    37,     0,   136,     0,  -108,     0,     0,
     0,     0,  -239,  -204,     0,     0,     0,     0,    80,   158,
     0,     0,     0,     0,     0,     0,     0,     0,  1332, 21724,
   110, 19524,     0,    21,     0,   406,     0,  -108,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  -187,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   391,     0,     0, 19524,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   166,   -49,   107,   560,   146,   445,   179,   107,     0,
     0,   136,   232,   462,     0, 21724, 21724,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   299,
   626,     0,     0,     0,   263, 21853, 21853, 21853, 21853,     0,
 21853,  5956,     0,     0,   266,   533,   597,     0,     0,     0,
     0, 16660,     0, 19653, 19653,     0,     0, 18611,     0, 21724,
   -43,     0, 20821,   280, 19524,     0,   666,   328,   344,   316,
 20692,     0, 19395,   322,   136,  1342,     0,     0,     0,   158,
   158, 20692,   324,     0,   224,   291,   266,     0,   335,   291,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   373, 22756,   806,     0,   641,     0,     0,     0,
     0,     0,     0,     0,     0,   877,  1026,  1075,   348,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0, 21724,
 21724, 21724, 21724, 20692, 21724, 21724, 21853, 21853, 21853, 21853,
 21853,     0,     0, 21853, 21853, 21853, 21853, 21853, 21853, 21853,
 21853, 21853, 21853, 21853, 21853, 21853, 21853,     0,     0, 21853,
 21853, 21853, 21853,     0,     0,     0,     0,  6055, 19653,  6431,
 21853,     0,     0,  5065, 21982,     0, 20950, 19395, 18879,   656,
 20950, 21982,     0, 19008,     0,   355,     0,   365,     0,   -49,
     0,     0,     0,     0,     0,  6567, 19653,  6943, 19524, 21724,
   371,     0,     0,     0,     0,   454,   457,   316,     0, 19524,
   456,  7033, 19653, 12376, 21853, 21853, 21853, 19524,   232, 21079,
   461,     0,    86,    86,     0, 12900, 19653, 23641,     0,     0,
     0,     0,  1039,     0, 21853, 19783,     0,     0, 20173,     0,
   158,     0,   385,     0,     0,     0,   689,   693,   158,    73,
     0,     0,     0,     0,     0, 23260, 21724,  5956, 19266,   377,
  7033, 12376, 21853, 21853,  1342,   381,   158,     0,     0, 19137,
     0,     0,  1342,     0, 20303,     0,     0, 20433,     0,     0,
     0,     0,     0,   702, 23697, 19653, 23753, 22756,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  1213,  -179,
  4519,  1289,   331,    54,   382,    54,     0,     0,     0,     0,
     0,     0,     0,   328,  2833,  2833,  2833,  2833,  4570,  3509,
  2833,  2833,  4030,  4030,  1101,  1101,   328,  2366,   328,   328,
   876,   876,  2358,  2358,  2434,  1948,   486,    54,   414,     0,
   420,  -204,     0,     0,     0,   158,   427,     0,   430,  -204,
     0,     0,  1948,     0,     0,  -204,     0,   473,  5583,   898,
     0,     0,   -29,   719, 21853,  5583,     0,     0,     0,     0,
   742,   158, 22756,   750,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   136, 21724, 19524,     0,     0,  -204,
     0,   158,  -204,   535,    73,  4471, 19524,  4471, 23585, 23260,
 21208,   536,     0,   295,     0,   472,   476,   158,   480,   490,
   536,   565,    70,     0,     0,     0,     0,     0,     0,     0,
   158,     0,     0, 21724, 21853,     0, 21853,   266,   597,     0,
     0,     0,     0, 19783, 20173,     0,     0,     0,    73,   468,
     0,   328,     0, 19266,     0,     0,   158,   291, 22756,     0,
     0,   158,     0,     0,   702,     0,   746,     0,     0,   121,
     0,   800,     0,     0,     0,     0,     0,     0,     0,  1193,
  4471,     0,     0,     0,     0,     0,     0,   527,   529,   793,
     0,     0,  -162,   796,   798,     0,     0,   799,     0,     0,
     0,   549,   817, 21853,   801,     0,   184,     0,   833,     0,
  1517,     0, 19524,     0,     0, 19524,   810,     0, 19524, 21982,
 21982,     0,   355,   541,   526, 21982, 21982,     0,   355,     0,
     0,    17,  -119, 20692, 21853, 23809, 19653, 23865, 21982,     0,
 21337,     0,   702, 22756,   510,     0,   136,   616,     0,     0,
     0,   158,   617,   136,     0,     0,     0,     0,   544,     0,
 19524,   622,     0, 21724,     0,   630, 21853, 21853,   562, 21853,
 21853,   634,     0, 21466, 19524, 19524, 19524,     0,    86,     0,
     0,     0,   859,     0,     0,   543,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  1193,
   482,     0,   860,     0,   158,   158,     0,     0,  1640,     0,
 19524, 19524,     0,  4471,     0,  4471,     0,   513,     0,     0,
   337,     0,     0, 21853,     0,     0,   865,   158,   867,     0,
     0,   869,   880,     0,   572,     0,   817, 22885,   873,   878,
   671,     0, 21853,     0,     0,     0,     0,     0,     0, 21982,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  5956,
   414,   420,   158,   427,   430, 21853,     0,   702,     0, 19524,
     0,     0,     0,     0,     0,   371, 23014,   107,     0,     0,
 19524,     0,     0,   107,     0, 21853,     0,     0,   314,   677,
   678,     0, 20173,     0,     0,   158,  1262,   900,  1205,     0,
   605,   909,     0,     0,   695,   601,   922,   923,     0,   927,
     0,   909,     0,     0,   817,     0,  1640,     0,   618,  4471,
     0,   513,     0,  4471,     0,     0,     0,     0,     0,   668,
  1021, 22885,  1174,     0,  5956,  5956,     0,   541,     0,   715,
 19524,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   673,  1044,     0,     0, 19524,     0, 19524,
     0,     0,     0,     0,     0, 19524,     0,     0,   942,  1262,
   906,     0,     0,  1640,     0,     0,     0,  1640,     0,  4471,
     0,  1640,     0,     0,     0,   961,     0,     0,   963,   964,
     0,   817,   965,   961,     0,     0, 23921,  1021,     0,   309,
     0,     0,     0,   751,     0, 23977, 19653, 24033,   454,   295,
   756, 19524,  1262,   942,  1262,   980,   909,   984,   909,   909,
  1640,     0,  1640,     0,  4471,     0,     0,  1640,     0,     0,
     0,     0,  1174,     0,     0,     0,     0,   158,     0,     0,
     0,     0,     0,   645,   942,  1262,     0,  1640,     0,     0,
     0,   961,   985,   961,   961,     0,     0,     0,   942,   909,
     0,  1640,     0,     0,     0,     0,   961,     0,
    }, yyRindex = {
//yyRindex 1108
     0,     0,   188,     0,     0,     0,     0,     0,     0,     0,
     0,   765,     0,     0,     0, 10619, 10722,     0,     0, 10834,
  4814,  4302, 12098, 12201, 12309, 12502, 22111,     0, 21595,     0,
     0, 12610, 12713, 12833,  5176,  3278, 13014, 13134,  5315, 13237,
     0,     0,     0,     0,     0,   101, 18353,   700,   683,   129,
     0,     0,  1441,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  9884,     0,     0,     0, 10073,     0,     0,     0,     0,     0,
     0,     0,     0,    76,  1820,  9581, 10190, 10433,     0, 13374,
     0, 11923,     0, 13538,     0,     0,     0,     0,     0,     0,
   145,     0,     0,     0,     0,    49,     0, 19913,     0,     0,
     0,     0, 10313,  7940,     0,     0,     0,     0,     0,   718,
     0,     0,     0, 16804,     0,     0, 16955,     0,     0,     0,
     0,   101,     0, 17813,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,  2016,  2353,  2868,  2955,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  3380,  3467,  3892,  3979,     0,  4404,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0, 11842,     0,     0,   754,  8063,
  8180,  8369,  8486,  8609,  8792,  8915,  2112,  9032,  9221,  2254,
  9338,     0, 16597,     0,     0,  9644,     0,     0,     0,     0,
     0,   765,     0,   789,     0,     0,     0,  1599,  1603,  1898,
  1910,  2399,  2541,  2914,  2973,  2954,  3053,  3426,  3466,     0,
     0,  3565,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   857,     0,     0,  1897,  1756,  1756,     0,     0,     0,
     0,   721,     0,     0,    96,     0,     0,   721,     0,     0,
     0,     0,     0,     0,    46,     0,     0, 11028, 10496, 13673,
     0, 17674,   101,     0,  1580,   828,     0,     0,   106,   721,
   721,     0,     0,     0,   726,   726,     0,     0,     0,   703,
  1641,  1801,  1951,  7227,  8280,  8551,  8702,  1553,  9132,  9403,
  1949,  9984,     0,     0,     0, 10255,   296,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,    59,     0,
     0,     0,     0,     0,     0,     0,     0,   101,   338,   350,
     0,     0,     0,    51,     0, 16381,     0,     0,     0,   165,
     0, 17394,     0,     0,     0,     0,    59,     0,   754,     0,
  1411,     0,     0,     0,     0,   534,     0,  9767,     0,   926,
 17533,     0,    59,     0,     0,     0,     0,   426,     0,     0,
     0,     0,     0,     0,  3978,     0,    59,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   721,     0,     0,     0,     0,     0,   204,   204,   721,   721,
     0,     0,     0,     0,     0,     0,     0,  5006,    46,     0,
     0,     0,     0,     0,   904,     0,   721,     0,     0,  1862,
   596,     0,   183,     0,   728,     0,     0,  -166,     0,     0,
     0, 10406,     0,   623,     0,    59,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  -123,     0,     0, 18084,     0, 18223,     0,     0,     0,     0,
     0,     0,     0, 11165, 15083, 15180, 15268, 15356, 15453, 15726,
 15541, 15629, 15814, 15902, 14194, 14311, 11268, 14467, 11405, 11541,
 13779, 13888, 14584, 14693,  1023, 14810,     0, 17954,  5688,  3651,
  7224, 19913,     0,  3790,     0,   739,  5827,     0,  6200,  4675,
     0,     0, 14966,     0,     0,  7586,     0, 10971, 16251,     0,
     0,     0, 14085,     0,     0,  1287,     0,     0,     0,     0,
     0,   721,     0,   653,     0,     0,     0,     0,  1476,     0,
     0,     0,     0,     0,     0,     0,   754, 17099, 17250,     0,
     0,   739,  9461,     0,   721,   209,   754,   227,     0,     0,
   265,    78,     0,   822,     0,  2627,  4163,   739,  2766,  3139,
   822,     0,     0,     0,     0,     0,     0,     0,  3938,  1136,
   739,  4450,  5432,     0,     0,     0,     0, 16291,  1756,     0,
     0,     0,     0,   215,   254,     0,     0,     0,   721,     0,
     0, 11650,     0,    46,   112,     0,   721,   726,     0,  5505,
   987,   739,  6017,  6529,   661,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   173,     0,     0,     0,     0,     0,     0,   219,     0,   226,
     0,     0,     0,   226,   226,     0,     0,   271,     0,     0,
     0,   396,   271,   141,   307,     0,     0,     0,     0,     0,
   -21,     0,   238,     0,     0,    46,     0,     0,   754,     0,
     0,     0, 16420, 11767,     0,     0,     0,     0, 16506,     0,
     0, 14004,  8729,     0,     0,     0,    59,     0,     0,  1198,
     0,     0,   701,     0,     0,     0,     0,     0,     0,     0,
     0,   721,     0,     0,     0,     0,     0,    53,   197,     0,
   277,   822,     0,     0,     0,     0,     0,     0,  7725,     0,
     0,     0,     0,     0,   840,   238,   238,   981,     0,     0,
     0,     0,   204,     0,     0,     0,     0,     0,     0,  7041,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   239,     0,   247,     0,   721,    20,     0,     0,     0,     0,
   238,    46,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,    56,   -21,    56,     0,
     0,    98,    56,     0,     0,     0,    98,    44,    74,  -155,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0, 15991,
  6339,  7363,   739,  6712,  6851,     0, 13615,   708,     0,   754,
     0,     0,     0,     0,     0,  1411,     0,     0,     0,     0,
   238,     0,     0,     0,     0,     0,     0,     0,   822,     0,
     0,     0,   283,     0,     0,   721,     0,   248,     0,     0,
     0,   226,     0,     0,     0,     0,   226,   226,     0,   226,
     0,   226,     0,     0,   271,     0,     0,     0,     0,   100,
     0,     0,     0,     0,     0,     0,     0,  1054,  1873,     0,
   137,     0,     0,     0, 16080, 16142,     0, 11978, 16561,     0,
   754,  1097,  1328,  1337,  1877,  1913,  1915,  1995,  1390,  2011,
  3519,  7825,  4031,     0,     0,  4543,     0,   754,     0,   926,
     0,     0,     0,     0,     0,   238,     0,     0,   274,     0,
   278,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,    56,     0,     0,    56,    56,
     0,    98,    56,    56,     0,     0,     0,   142,     0,  -124,
     0,     0,     0,     0,  7827,     0,    59,     0,   534,   822,
     0,    12,     0,   282,     0,   288,   226,   226,   226,   226,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  5003,   983,     0,     0,     0,  8313,  1009,   739,  9165,  9554,
     0,     0,     0,     0,   293,     0,     0,     0,     0,     0,
     0,    56,    56,    56,    56,     0,   286,     0,   297,   226,
     0,     0,     0,     0,     0,     0,    56,     0,
    }, yyGindex = {
//yyGindex 193
     0,     0,    28,     0,  -319,     0,    18,    25,   -48,   -64,
     0,     0,     0,   635,     0,     0,     0,  1017,     0,     0,
   805,  1041,     0,  -279,     0,     0,     0,   551,     0,    40,
  1096,  -186,   -30,     0,   148,     0,   555,  -415,     0,    39,
   442,  1222,   122,    24,   587,     8,     1,  -449,     0,    61,
     0,     0,   357,     0,    71,     0,    27,  1102,   509,     0,
     0,  -255,   323,  -736,     0,     0,   160,  -365,     0,     0,
     0,  -375,   202,  -339,   -92,    -5,  1279,  -467,     0,     0,
   525,   550,    43,     0,     0,  3785,   305,  -759,     0,     0,
     0,     0,   236,  1773,   386,   310,   287,   182,     0,     0,
     0,    13,  -454,     0,  -452,   187,  -287,  -413,     0,   -27,
  7254,   -67,   389,  -397,   531,     0,  1150,    -9,   126,   608,
     0,     3,  -713,     0,  -555,     0,     0,  -199,  -838,     0,
  -381,  -825,   330,   104,     0,  -852,  1089,   563,  -563,  -399,
     0,    11,     0,  2209,  -178,   -65,     0,  -497,  1389,  -416,
  -245,     0,    -6,    26,     0,     0,     0,   -26,     0,  -271,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,    16,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,
    };
    protected static final short[] yyTable = YyTables.yyTable();
    protected static final short[] yyCheck = YyTables.yyCheck();

  /** maps symbol value to printable name.
      @see #yyExpecting
    */
  protected static final String[] yyNames = {
    "end-of-file",null,null,null,null,null,null,null,null,null,"'\\n'",
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,"' '",null,null,null,null,null,
    null,null,null,null,null,null,"','",null,null,null,null,null,null,
    null,null,null,null,null,null,null,"':'","';'",null,"'='",null,"'?'",
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,
    "'['",null,null,null,null,null,null,null,null,null,null,null,null,
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
    null,null,null,null,null,null,null,null,null,null,null,null,null,
    "keyword_class","keyword_module","keyword_def","keyword_undef",
    "keyword_begin","keyword_rescue","keyword_ensure","keyword_end",
    "keyword_if","keyword_unless","keyword_then","keyword_elsif",
    "keyword_else","keyword_case","keyword_when","keyword_while",
    "keyword_until","keyword_for","keyword_break","keyword_next",
    "keyword_redo","keyword_retry","keyword_in","keyword_do",
    "keyword_do_cond","keyword_do_block","keyword_return","keyword_yield",
    "keyword_super","keyword_self","keyword_nil","keyword_true",
    "keyword_false","keyword_and","keyword_or","keyword_not",
    "modifier_if","modifier_unless","modifier_while","modifier_until",
    "modifier_rescue","keyword_alias","keyword_defined","keyword_BEGIN",
    "keyword_END","keyword__LINE__","keyword__FILE__",
    "keyword__ENCODING__","keyword_do_lambda","tIDENTIFIER","tFID",
    "tGVAR","tIVAR","tCONSTANT","tCVAR","tLABEL","tCHAR","tUPLUS",
    "tUMINUS","tUMINUS_NUM","tPOW","tCMP","tEQ","tEQQ","tNEQ","tGEQ",
    "tLEQ","tANDOP","tOROP","tMATCH","tNMATCH","tDOT","tDOT2","tDOT3",
    "tAREF","tASET","tLSHFT","tRSHFT","tANDDOT","tCOLON2","tCOLON3",
    "tOP_ASGN","tASSOC","tLPAREN","tLPAREN2","tRPAREN","tLPAREN_ARG",
    "tLBRACK","tRBRACK","tLBRACE","tLBRACE_ARG","tSTAR","tSTAR2","tAMPER",
    "tAMPER2","tTILDE","tPERCENT","tDIVIDE","tPLUS","tMINUS","tLT","tGT",
    "tPIPE","tBANG","tCARET","tLCURLY","tRCURLY","tBACK_REF2","tSYMBEG",
    "tSTRING_BEG","tXSTRING_BEG","tREGEXP_BEG","tWORDS_BEG","tQWORDS_BEG",
    "tSTRING_DBEG","tSTRING_DVAR","tSTRING_END","tLAMBDA","tLAMBEG",
    "tNTH_REF","tBACK_REF","tSTRING_CONTENT","tINTEGER","tIMAGINARY",
    "tFLOAT","tRATIONAL","tREGEXP_END","tIGNORED_NL","tCOMMENT",
    "tEMBDOC_BEG","tEMBDOC","tEMBDOC_END","tSP","tHEREDOC_BEG",
    "tHEREDOC_END","tSYMBOLS_BEG","tQSYMBOLS_BEG","tDSTAR","tSTRING_DEND",
    "tLABEL_END","tLOWEST","k__END__",
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
    "$$2 :",
    "top_stmt : keyword_BEGIN $$2 tLCURLY top_compstmt tRCURLY",
    "bodystmt : compstmt opt_rescue opt_else opt_ensure",
    "compstmt : stmts opt_terms",
    "stmts : none",
    "stmts : stmt_or_begin",
    "stmts : stmts terms stmt_or_begin",
    "stmts : error stmt",
    "stmt_or_begin : stmt",
    "$$3 :",
    "stmt_or_begin : keyword_begin $$3 tLCURLY top_compstmt tRCURLY",
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
    "stmt : keyword_END tLCURLY compstmt tRCURLY",
    "stmt : command_asgn",
    "stmt : mlhs '=' command_call",
    "stmt : lhs '=' mrhs",
    "stmt : mlhs '=' mrhs_arg",
    "stmt : expr",
    "command_asgn : lhs '=' command_rhs",
    "command_asgn : var_lhs tOP_ASGN command_rhs",
    "command_asgn : primary_value '[' opt_call_args rbracket tOP_ASGN command_rhs",
    "command_asgn : primary_value call_op tIDENTIFIER tOP_ASGN command_rhs",
    "command_asgn : primary_value call_op tCONSTANT tOP_ASGN command_rhs",
    "command_asgn : primary_value tCOLON2 tCONSTANT tOP_ASGN command_rhs",
    "command_asgn : primary_value tCOLON2 tIDENTIFIER tOP_ASGN command_rhs",
    "command_asgn : backref tOP_ASGN command_rhs",
    "command_rhs : command_call",
    "command_rhs : command_call modifier_rescue stmt",
    "command_rhs : command_asgn",
    "expr : command_call",
    "expr : expr keyword_and expr",
    "expr : expr keyword_or expr",
    "expr : keyword_not opt_nl expr",
    "expr : tBANG command_call",
    "expr : arg",
    "expr_value : expr",
    "command_call : command",
    "command_call : block_command",
    "block_command : block_call",
    "block_command : block_call call_op2 operation2 command_args",
    "cmd_brace_block : tLBRACE_ARG brace_body tRCURLY",
    "fcall : operation",
    "command : fcall command_args",
    "command : fcall command_args cmd_brace_block",
    "command : primary_value call_op operation2 command_args",
    "command : primary_value call_op operation2 command_args cmd_brace_block",
    "command : primary_value tCOLON2 operation2 command_args",
    "command : primary_value tCOLON2 operation2 command_args cmd_brace_block",
    "command : keyword_super command_args",
    "command : keyword_yield command_args",
    "command : keyword_return call_args",
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
    "fsym : fname",
    "fsym : symbol",
    "fitem : fsym",
    "fitem : dsym",
    "undef_list : fitem",
    "$$5 :",
    "undef_list : undef_list ',' $$5 fitem",
    "op : tPIPE",
    "op : tCARET",
    "op : tAMPER2",
    "op : tCMP",
    "op : tEQ",
    "op : tEQQ",
    "op : tMATCH",
    "op : tNMATCH",
    "op : tGT",
    "op : tGEQ",
    "op : tLT",
    "op : tLEQ",
    "op : tNEQ",
    "op : tLSHFT",
    "op : tRSHFT",
    "op : tPLUS",
    "op : tMINUS",
    "op : tSTAR2",
    "op : tSTAR",
    "op : tDIVIDE",
    "op : tPERCENT",
    "op : tPOW",
    "op : tBANG",
    "op : tTILDE",
    "op : tUPLUS",
    "op : tUMINUS",
    "op : tAREF",
    "op : tASET",
    "op : tBACK_REF2",
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
    "reswords : modifier_if",
    "reswords : modifier_unless",
    "reswords : modifier_while",
    "reswords : modifier_until",
    "reswords : modifier_rescue",
    "arg : lhs '=' arg_rhs",
    "arg : var_lhs tOP_ASGN arg_rhs",
    "arg : primary_value '[' opt_call_args rbracket tOP_ASGN arg",
    "arg : primary_value call_op tIDENTIFIER tOP_ASGN arg_rhs",
    "arg : primary_value call_op tCONSTANT tOP_ASGN arg_rhs",
    "arg : primary_value tCOLON2 tIDENTIFIER tOP_ASGN arg_rhs",
    "arg : primary_value tCOLON2 tCONSTANT tOP_ASGN arg_rhs",
    "arg : tCOLON3 tCONSTANT tOP_ASGN arg_rhs",
    "arg : backref tOP_ASGN arg_rhs",
    "arg : arg tDOT2 arg",
    "arg : arg tDOT3 arg",
    "arg : arg tPLUS arg",
    "arg : arg tMINUS arg",
    "arg : arg tSTAR2 arg",
    "arg : arg tDIVIDE arg",
    "arg : arg tPERCENT arg",
    "arg : arg tPOW arg",
    "arg : tUMINUS_NUM simple_numeric tPOW arg",
    "arg : tUPLUS arg",
    "arg : tUMINUS arg",
    "arg : arg tPIPE arg",
    "arg : arg tCARET arg",
    "arg : arg tAMPER2 arg",
    "arg : arg tCMP arg",
    "arg : rel_expr",
    "arg : arg tEQ arg",
    "arg : arg tEQQ arg",
    "arg : arg tNEQ arg",
    "arg : arg tMATCH arg",
    "arg : arg tNMATCH arg",
    "arg : tBANG arg",
    "arg : tTILDE arg",
    "arg : arg tLSHFT arg",
    "arg : arg tRSHFT arg",
    "arg : arg tANDOP arg",
    "arg : arg tOROP arg",
    "arg : keyword_defined opt_nl arg",
    "arg : arg '?' arg opt_nl ':' arg",
    "arg : primary",
    "relop : tGT",
    "relop : tLT",
    "relop : tGEQ",
    "relop : tLEQ",
    "rel_expr : arg relop arg",
    "rel_expr : rel_expr relop arg",
    "arg_value : arg",
    "aref_args : none",
    "aref_args : args trailer",
    "aref_args : args ',' assocs trailer",
    "aref_args : assocs trailer",
    "arg_rhs : arg",
    "arg_rhs : arg modifier_rescue arg",
    "paren_args : tLPAREN2 opt_call_args rparen",
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
    "$$6 :",
    "command_args : $$6 call_args",
    "block_arg : tAMPER arg_value",
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
    "$$7 :",
    "primary : keyword_begin $$7 bodystmt keyword_end",
    "$$8 :",
    "primary : tLPAREN_ARG $$8 rparen",
    "$$9 :",
    "$$10 :",
    "primary : tLPAREN_ARG $$9 stmt $$10 rparen",
    "primary : tLPAREN compstmt tRPAREN",
    "primary : primary_value tCOLON2 tCONSTANT",
    "primary : tCOLON3 tCONSTANT",
    "primary : tLBRACK aref_args tRBRACK",
    "primary : tLBRACE assoc_list tRCURLY",
    "primary : keyword_return",
    "primary : keyword_yield tLPAREN2 call_args rparen",
    "primary : keyword_yield tLPAREN2 rparen",
    "primary : keyword_yield",
    "primary : keyword_defined opt_nl tLPAREN2 expr rparen",
    "primary : keyword_not tLPAREN2 expr rparen",
    "primary : keyword_not tLPAREN2 rparen",
    "primary : fcall brace_block",
    "primary : method_call",
    "primary : method_call brace_block",
    "primary : tLAMBDA lambda",
    "primary : keyword_if expr_value then compstmt if_tail keyword_end",
    "primary : keyword_unless expr_value then compstmt opt_else keyword_end",
    "$$11 :",
    "$$12 :",
    "primary : keyword_while $$11 expr_value do $$12 compstmt keyword_end",
    "$$13 :",
    "$$14 :",
    "primary : keyword_until $$13 expr_value do $$14 compstmt keyword_end",
    "primary : keyword_case expr_value opt_terms case_body keyword_end",
    "primary : keyword_case opt_terms case_body keyword_end",
    "$$15 :",
    "$$16 :",
    "primary : keyword_for for_var keyword_in $$15 expr_value do $$16 compstmt keyword_end",
    "$$17 :",
    "primary : keyword_class cpath superclass $$17 bodystmt keyword_end",
    "$$18 :",
    "$$19 :",
    "primary : keyword_class tLSHFT expr $$18 term $$19 bodystmt keyword_end",
    "$$20 :",
    "primary : keyword_module cpath $$20 bodystmt keyword_end",
    "$$21 :",
    "primary : keyword_def fname $$21 f_arglist bodystmt keyword_end",
    "$$22 :",
    "$$23 :",
    "primary : keyword_def singleton dot_or_colon $$22 fname $$23 f_arglist bodystmt keyword_end",
    "primary : keyword_break",
    "primary : keyword_next",
    "primary : keyword_redo",
    "primary : keyword_retry",
    "primary_value : primary",
    "then : term",
    "then : keyword_then",
    "then : term keyword_then",
    "do : term",
    "do : keyword_do_cond",
    "if_tail : opt_else",
    "if_tail : keyword_elsif expr_value then compstmt if_tail",
    "opt_else : none",
    "opt_else : keyword_else compstmt",
    "for_var : lhs",
    "for_var : mlhs",
    "f_marg : f_norm_arg",
    "f_marg : tLPAREN f_margs rparen",
    "f_marg_list : f_marg",
    "f_marg_list : f_marg_list ',' f_marg",
    "f_margs : f_marg_list",
    "f_margs : f_marg_list ',' tSTAR f_norm_arg",
    "f_margs : f_marg_list ',' tSTAR f_norm_arg ',' f_marg_list",
    "f_margs : f_marg_list ',' tSTAR",
    "f_margs : f_marg_list ',' tSTAR ',' f_marg_list",
    "f_margs : tSTAR f_norm_arg",
    "f_margs : tSTAR f_norm_arg ',' f_marg_list",
    "f_margs : tSTAR",
    "f_margs : tSTAR ',' f_marg_list",
    "block_args_tail : f_block_kwarg ',' f_kwrest opt_f_block_arg",
    "block_args_tail : f_block_kwarg opt_f_block_arg",
    "block_args_tail : f_kwrest opt_f_block_arg",
    "block_args_tail : f_block_arg",
    "opt_block_args_tail : ',' block_args_tail",
    "opt_block_args_tail :",
    "block_param : f_arg ',' f_block_optarg ',' f_rest_arg opt_block_args_tail",
    "block_param : f_arg ',' f_block_optarg ',' f_rest_arg ',' f_arg opt_block_args_tail",
    "block_param : f_arg ',' f_block_optarg opt_block_args_tail",
    "block_param : f_arg ',' f_block_optarg ',' f_arg opt_block_args_tail",
    "block_param : f_arg ',' f_rest_arg opt_block_args_tail",
    "block_param : f_arg ','",
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
    "block_param_def : tPIPE opt_bv_decl tPIPE",
    "block_param_def : tOROP",
    "block_param_def : tPIPE block_param opt_bv_decl tPIPE",
    "opt_bv_decl : opt_nl",
    "opt_bv_decl : opt_nl ';' bv_decls opt_nl",
    "bv_decls : bvar",
    "bv_decls : bv_decls ',' bvar",
    "bvar : tIDENTIFIER",
    "bvar : f_bad_arg",
    "$$24 :",
    "$$25 :",
    "lambda : $$24 $$25 f_larglist lambda_body",
    "f_larglist : tLPAREN2 f_args opt_bv_decl tRPAREN",
    "f_larglist : f_args",
    "lambda_body : tLAMBEG compstmt tRCURLY",
    "lambda_body : keyword_do_lambda compstmt keyword_end",
    "do_block : keyword_do_block do_body keyword_end",
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
    "brace_block : tLCURLY brace_body tRCURLY",
    "$$26 :",
    "brace_block : keyword_do $$26 opt_block_param compstmt keyword_end",
    "$$27 :",
    "brace_body : $$27 opt_block_param compstmt",
    "$$28 :",
    "do_body : $$28 opt_block_param bodystmt",
    "case_body : keyword_when args then compstmt cases",
    "cases : opt_else",
    "cases : case_body",
    "opt_rescue : keyword_rescue exc_list exc_var then compstmt opt_rescue",
    "opt_rescue :",
    "exc_list : arg_value",
    "exc_list : mrhs",
    "exc_list : none",
    "exc_var : tASSOC lhs",
    "exc_var : none",
    "opt_ensure : keyword_ensure compstmt",
    "opt_ensure : none",
    "literal : numeric",
    "literal : symbol",
    "literal : dsym",
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
    "$$29 :",
    "string_content : tSTRING_DVAR $$29 string_dvar",
    "$$30 :",
    "$$31 :",
    "$$32 :",
    "$$33 :",
    "$$34 :",
    "string_content : tSTRING_DBEG $$30 $$31 $$32 $$33 $$34 compstmt tSTRING_DEND",
    "string_dvar : tGVAR",
    "string_dvar : tIVAR",
    "string_dvar : tCVAR",
    "string_dvar : backref",
    "symbol : tSYMBEG sym",
    "sym : fname",
    "sym : tIVAR",
    "sym : tGVAR",
    "sym : tCVAR",
    "dsym : tSYMBEG xstring_contents tSTRING_END",
    "numeric : simple_numeric",
    "numeric : tUMINUS_NUM simple_numeric",
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
    "$$35 :",
    "superclass : tLT $$35 expr_value term",
    "superclass :",
    "f_arglist : tLPAREN2 f_args rparen",
    "$$36 :",
    "f_arglist : $$36 f_args term",
    "args_tail : f_kwarg ',' f_kwrest opt_f_block_arg",
    "args_tail : f_kwarg opt_f_block_arg",
    "args_tail : f_kwrest opt_f_block_arg",
    "args_tail : f_block_arg",
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
    "f_kwrest : kwrest_mark tIDENTIFIER",
    "f_kwrest : kwrest_mark",
    "f_opt : f_arg_asgn '=' arg_value",
    "f_block_opt : f_arg_asgn '=' primary_value",
    "f_block_optarg : f_block_opt",
    "f_block_optarg : f_block_optarg ',' f_block_opt",
    "f_optarg : f_opt",
    "f_optarg : f_optarg ',' f_opt",
    "restarg_mark : tSTAR2",
    "restarg_mark : tSTAR",
    "f_rest_arg : restarg_mark tIDENTIFIER",
    "f_rest_arg : restarg_mark",
    "blkarg_mark : tAMPER2",
    "blkarg_mark : tAMPER",
    "f_block_arg : blkarg_mark tIDENTIFIER",
    "opt_f_block_arg : ',' f_block_arg",
    "opt_f_block_arg :",
    "singleton : var_ref",
    "$$37 :",
    "singleton : tLPAREN2 $$37 expr rparen",
    "assoc_list : none",
    "assoc_list : assocs trailer",
    "assocs : assoc",
    "assocs : assocs ',' assoc",
    "assoc : arg_value tASSOC arg_value",
    "assoc : tLABEL arg_value",
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
    "dot_or_colon : tDOT",
    "dot_or_colon : tCOLON2",
    "call_op : tDOT",
    "call_op : tANDDOT",
    "call_op2 : call_op",
    "call_op2 : tCOLON2",
    "opt_terms :",
    "opt_terms : terms",
    "opt_nl :",
    "opt_nl : '\\n'",
    "rparen : opt_nl tRPAREN",
    "rbracket : opt_nl tRBRACK",
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
  public static final String yyName (int token) {
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
  public Object yyparse (RipperLexer yyLex, Object ayydebug)
				throws java.io.IOException {
    this.yydebug = (org.jruby.parser.YYDebug)ayydebug;
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
    */
  public Object yyparse (RipperLexer yyLex) throws java.io.IOException {
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
      if (yydebug != null) yydebug.push(yyState, yyVal);

      yyDiscarded: for (;;) {	// discarding a token does not change stack
        int yyN;
        if ((yyN = yyDefRed[yyState]) == 0) {	// else [default] reduce (yyN)
          if (yyToken < 0) {
//            yyToken = yyLex.advance() ? yyLex.token() : 0;
            yyToken = yyLex.nextToken();
            if (yydebug != null)
              yydebug.lex(yyState, yyToken, yyName(yyToken), yyLex.value());
          }
          if ((yyN = yySindex[yyState]) != 0 && (yyN += yyToken) >= 0
              && yyN < yyTable.length && yyCheck[yyN] == yyToken) {
            if (yydebug != null)
              yydebug.shift(yyState, yyTable[yyN], yyErrorFlag-1);
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
              if (yydebug != null) yydebug.error("syntax error");
  
            case 1: case 2:
              yyErrorFlag = 3;
              do {
                if ((yyN = yySindex[yyStates[yyTop]]) != 0
                    && (yyN += yyErrorCode) >= 0 && yyN < yyTable.length
                    && yyCheck[yyN] == yyErrorCode) {
                  if (yydebug != null)
                    yydebug.shift(yyStates[yyTop], yyTable[yyN], 3);
                  yyState = yyTable[yyN];
                  yyVal = yyLex.value();
                  continue yyLoop;
                }
                if (yydebug != null) yydebug.pop(yyStates[yyTop]);
              } while (-- yyTop >= 0);
              if (yydebug != null) yydebug.reject();
              yyerror("irrecoverable syntax error");
  
            case 3:
              if (yyToken == 0) {
                if (yydebug != null) yydebug.reject();
                yyerror("irrecoverable syntax error at end-of-file");
              }
              if (yydebug != null)
                yydebug.discard(yyState, yyToken, yyName(yyToken),
  							yyLex.value());
              yyToken = -1;
              continue yyDiscarded;		// leave stack alone
            }
        }
        int yyV = yyTop + 1-yyLen[yyN];
        if (yydebug != null)
          yydebug.reduce(yyState, yyStates[yyV-1], yyN, yyRule[yyN], yyLen[yyN]);
        RipperParserState state = states[yyN];
        if (state == null) {
            yyVal = yyDefault(yyV > yyTop ? null : yyVals[yyV]);
        } else {
            yyVal = state.execute(this, yyVal, yyVals, yyTop);
        }
//        switch (yyN) {
// ACTIONS_END
//        }
        yyTop -= yyLen[yyN];
        yyState = yyStates[yyTop];
        int yyM = yyLhs[yyN];
        if (yyState == 0 && yyM == 0) {
          if (yydebug != null) yydebug.shift(0, yyFinal);
          yyState = yyFinal;
          if (yyToken < 0) {
            yyToken = yyLex.nextToken();
//            yyToken = yyLex.advance() ? yyLex.token() : 0;
            if (yydebug != null)
               yydebug.lex(yyState, yyToken,yyName(yyToken), yyLex.value());
          }
          if (yyToken == 0) {
            if (yydebug != null) yydebug.accept(yyVal);
            return yyVal;
          }
          continue yyLoop;
        }
        if ((yyN = yyGindex[yyM]) != 0 && (yyN += yyState) >= 0
            && yyN < yyTable.length && yyCheck[yyN] == yyState)
          yyState = yyTable[yyN];
        else
          yyState = yyDgoto[yyM];
        if (yydebug != null) yydebug.shift(yyStates[yyTop], yyState);
        continue yyLoop;
      }
    }
  }

static RipperParserState[] states = new RipperParserState[650];
static {
states[1] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                  p.setState(EXPR_BEG);
                  p.pushLocalScope();
    return yyVal;
  }
};
states[2] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = p.dispatch("on_program", ((IRubyObject)yyVals[0+yyTop]));
                  p.popCurrentScope();
    return yyVal;
  }
};
states[3] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((IRubyObject)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[4] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = p.dispatch("on_stmts_add", p.dispatch("on_stmts_new"), p.dispatch("on_void_stmt"));
    return yyVal;
  }
};
states[5] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = p.dispatch("on_stmts_add", p.dispatch("on_stmts_new"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[6] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = p.dispatch("on_stmts_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[7] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[9] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                  if (p.isInDef() || p.isInSingle()) {
                      p.yyerror("BEGIN in method");
                  }
    return yyVal;
  }
};
states[10] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = p.dispatch("on_BEGIN", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[11] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = p.dispatch("on_bodystmt", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[12] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[13] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_stmts_add", p.dispatch("on_stmts_new"), p.dispatch("on_void_stmt"));
    return yyVal;
  }
};
states[14] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_stmts_add", p.dispatch("on_stmts_new"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[15] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_stmts_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[16] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[17] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[18] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.yyerror("BEGIN is permitted only at toplevel");
    return yyVal;
  }
};
states[19] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_BEGIN", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[20] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.setState(EXPR_FNAME|EXPR_FITEM);
    return yyVal;
  }
};
states[21] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_alias", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[22] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_alias", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[23] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_alias", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[24] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_alias_error", p.dispatch("on_var_alias", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop])));
                    p.error();
    return yyVal;
  }
};
states[25] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_undef", ((RubyArray)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[26] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_if_mod", ((IRubyObject)yyVals[0+yyTop]), ((IRubyObject)yyVals[-2+yyTop]));
    return yyVal;
  }
};
states[27] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_unless_mod", ((IRubyObject)yyVals[0+yyTop]), ((IRubyObject)yyVals[-2+yyTop]));
    return yyVal;
  }
};
states[28] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_while_mod", ((IRubyObject)yyVals[0+yyTop]), ((IRubyObject)yyVals[-2+yyTop]));
    return yyVal;
  }
};
states[29] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_until_mod", ((IRubyObject)yyVals[0+yyTop]), ((IRubyObject)yyVals[-2+yyTop]));
    return yyVal;
  }
};
states[30] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_rescue_mod", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[31] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    if (p.isInDef() || p.isInSingle()) {
                        p.warn("END in method; use at_exit");
                    }
                    yyVal = p.dispatch("on_END", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[33] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_massign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[34] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[35] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_massign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[37] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[38] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_opassign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[39] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_aref_field", ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop])),
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[40] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_field", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop])), 
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[41] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_field",((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[42] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_const_path_field", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop])), 
                                    ((IRubyObject)yyVals[-1+yyTop]),
                                    ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[43] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_field", ((IRubyObject)yyVals[-4+yyTop]), p.intern("::"), ((IRubyObject)yyVals[-2+yyTop])), 
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[44] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assign_error", 
                                    p.dispatch("on_assign", 
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[-2+yyTop])), 
                                    ((IRubyObject)yyVals[0+yyTop])));
                    p.error();
    return yyVal;
  }
};
states[45] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[46] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_rescue_mod", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[49] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("and"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[50] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("or"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[51] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_unary", p.intern("not"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[52] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_unary", p.intern("!"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[54] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[58] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_method_add_arg", 
                                    p.dispatch("on_call", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[59] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[61] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_command", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[62] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_method_add_block",
                                    p.dispatch("on_command", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[63] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_command_call", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[64] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_method_add_block",
                                    p.dispatch("on_command_call", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop])); 
    return yyVal;
  }
};
states[65] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_command_call", ((IRubyObject)yyVals[-3+yyTop]), p.intern("::"), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[66] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_method_add_block",
                                    p.dispatch("on_command_call", ((IRubyObject)yyVals[-4+yyTop]), p.intern("::"), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[67] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_super", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[68] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_yield", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[69] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_return", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[70] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_break", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[71] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_next", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[73] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_paren", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[75] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_paren", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[76] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[77] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_add", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[78] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[79] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_add_post",
                                    p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[80] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-1+yyTop]), null);
    return yyVal;
  }
};
states[81] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_add_post",
                                    p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-3+yyTop]), null),
                                    ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[82] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_add_star", p.dispatch("on_mlhs_new"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[83] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_add_post",
                                    p.dispatch("on_mlhs_add_star", p.dispatch("on_mlhs_new"), ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[84] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_add_star", p.dispatch("on_mlhs_new"), null);
    return yyVal;
  }
};
states[85] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_add_post",
                                    p.dispatch("on_mlhs_add_star", p.dispatch("on_mlhs_new"), null),
                                    ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[87] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_paren", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[88] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_add", p.dispatch("on_mlhs_new"), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[89] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[90] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_add", p.dispatch("on_mlhs_new"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[91] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[92] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.assignableIdentifier(p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[93] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[94] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[95] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.assignableConstant(p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[96] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[97] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[98] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[99] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[100] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[101] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[102] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[103] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.yyerror("Can't assign to __ENCODING__");
    return yyVal;
  }
};
states[104] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_aref_field", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[105] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_field", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
                    
    return yyVal;
  }
};
states[106] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_const_path_field", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[107] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
		    yyVal = p.dispatch("on_field", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[108] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_const_path_field", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));

                    if (p.isInDef() || p.isInSingle()) {
                        yyVal = p.dispatch("on_assign_error", ((IRubyObject)yyVal));
                        p.error();
                    }
    return yyVal;
  }
};
states[109] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_top_const_field", ((IRubyObject)yyVals[0+yyTop]));

                    if (p.isInDef() || p.isInSingle()) {
                        yyVal = p.dispatch("on_assign_error", ((IRubyObject)yyVal));
                        p.error();
                    }
    return yyVal;
  }
};
states[110] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assign_error", p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
                    p.error();
    return yyVal;
  }
};
states[111] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_field", p.assignableIdentifier(((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[112] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[113] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[114] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.assignableConstant(p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[115] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[116] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[117] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[118] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[119] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[120] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[121] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[122] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[123] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_aref_field", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[124] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_field", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[125] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_field", ((IRubyObject)yyVals[-2+yyTop]), p.intern("::"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[126] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_field", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[127] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    IRubyObject val = p.dispatch("on_const_path_field", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));

                    if (p.isInDef() || p.isInSingle()) {
                        val = p.dispatch("on_assign_error", val);
                        p.error();
                    }

                    yyVal = val;
    return yyVal;
  }
};
states[128] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    IRubyObject val = p.dispatch("on_top_const_field", ((IRubyObject)yyVals[0+yyTop]));

                    if (p.isInDef() || p.isInSingle()) {
                        val = p.dispatch("on_assign_error", val);
                        p.error();
                    }

                    yyVal = val;
    return yyVal;
  }
};
states[129] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
                    p.error();
    return yyVal;
  }
};
states[130] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_class_name_error", ((IRubyObject)yyVals[0+yyTop]));
                    p.error();
    return yyVal;
  }
};
states[132] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_top_const_ref", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[133] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_const_ref", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[134] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_const_path_ref", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[138] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                   p.setState(EXPR_ENDFN);
                   yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[139] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                   p.setState(EXPR_ENDFN);
                   yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[140] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[141] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[142] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = p.dispatch("on_symbol_literal", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[143] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[144] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[145] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.setState(EXPR_FNAME|EXPR_FITEM);
    return yyVal;
  }
};
states[146] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((RubyArray)yyVals[-3+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[222] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[223] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_opassign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[224] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_aref_field", ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop])),
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[225] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_field", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[226] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_field", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[227] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_field", ((IRubyObject)yyVals[-4+yyTop]), p.intern("::"), ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[228] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assign_error", 
                                    p.dispatch("on_opassign", 
                                               p.dispatch("on_const_path_field", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop])),
                                               ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[229] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assign_error", 
                                    p.dispatch("on_opassign", 
                                               p.dispatch("on_top_const_field", ((IRubyObject)yyVals[-2+yyTop])),
                                               ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[230] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assign_error", 
                                    p.dispatch("on_opassign",
                                               p.dispatch("on_var_field", ((IRubyObject)yyVals[-2+yyTop])),
                                               ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop])));
                    p.error();
    return yyVal;
  }
};
states[231] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_dot2", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[232] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_dot3", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[233] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("+"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[234] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("-"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[235] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("*"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[236] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("/"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[237] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("%"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[238] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("**"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[239] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_unary", 
                                    p.intern("-@"), 
                                    p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("**"), ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[240] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_unary", p.intern("+@"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[241] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_unary", p.intern("-@"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[242] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("|"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[243] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("^"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[244] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("&"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[245] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("<=>"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[246] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[247] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("=="), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[248] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("==="), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[249] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("!="), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[250] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("=~"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[251] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("!~"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[252] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_unary", p.intern("!"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[253] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_unary", p.intern("~"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[254] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("<<"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[255] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern(">>"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[256] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("&&"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[257] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("||"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[258] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_defined", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[259] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_ifop", ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[260] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[261] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[262] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[263] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[264] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[265] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern(">"), ((IRubyObject)yyVals[0+yyTop]));

    return yyVal;
  }
};
states[266] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                     p.warning("comparison '" + ((IRubyObject)yyVals[-1+yyTop]) + "%s' after comparison");
                     yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern(">"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[267] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[269] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[270] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_args_add", 
                                    ((IRubyObject)yyVals[-3+yyTop]),
                                    p.dispatch("on_bare_assoc_hash", ((RubyArray)yyVals[-1+yyTop])));
    return yyVal;
  }
};
states[271] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_args_add", 
                                    p.dispatch("on_args_new"),
                                    p.dispatch("on_bare_assoc_hash", ((RubyArray)yyVals[-1+yyTop])));
    return yyVal;
  }
};
states[272] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[273] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_rescue_mod", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[274] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_arg_paren", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[279] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[280] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_args_add", ((IRubyObject)yyVals[-3+yyTop]), p.dispatch("on_bare_assoc_hash", ((RubyArray)yyVals[-1+yyTop])));
    return yyVal;
  }
};
states[281] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_args_add", 
                                    p.dispatch("on_args_new"),
                                    p.dispatch("on_bare_assoc_hash", ((RubyArray)yyVals[-1+yyTop])));
    return yyVal;
  }
};
states[282] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_args_add", p.dispatch("on_args_new"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[283] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.arg_add_optblock(((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[284] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal =  p.arg_add_optblock(p.dispatch("on_args_add", 
                                                        p.dispatch("on_args_new"),
                                                        p.dispatch("on_bare_assoc_hash", ((RubyArray)yyVals[-1+yyTop]))),
                                             ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[285] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.arg_add_optblock(p.dispatch("on_args_add", 
                                            ((IRubyObject)yyVals[-3+yyTop]),
                                            p.dispatch("on_bare_assoc_hash", ((RubyArray)yyVals[-1+yyTop]))),
                                            ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[286] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_args_add_block", p.dispatch("on_args_new"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[287] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = Long.valueOf(p.getCmdArgumentState().getStack());
                    p.getCmdArgumentState().begin();
    return yyVal;
  }
};
states[288] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[289] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[290] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[292] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_args_add", p.dispatch("on_args_new"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[293] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_args_add_star", p.dispatch("on_args_new"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[294] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_args_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[295] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_args_add_star", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[296] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[297] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[298] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mrhs_add", 
                                    p.dispatch("on_mrhs_new_from_args", ((IRubyObject)yyVals[-2+yyTop])), 
                                    ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[299] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mrhs_add_star",
                                    p.dispatch("on_mrhs_new_from_args", ((IRubyObject)yyVals[-3+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[300] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mrhs_add_star", p.dispatch("on_mrhs_new"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[307] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = ((IRubyObject)yyVals[0+yyTop]); /* FIXME: Why complaining without $$ = $1;*/
    return yyVal;
  }
};
states[308] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = ((IRubyObject)yyVals[0+yyTop]); /* FIXME: Why complaining without $$ = $1;*/
    return yyVal;
  }
};
states[311] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_method_add_arg", p.dispatch("on_fcall", ((IRubyObject)yyVals[0+yyTop])), p.dispatch("on_args_new"));
    return yyVal;
  }
};
states[312] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.getCmdArgumentState().getStack();
                    p.getCmdArgumentState().reset();
    return yyVal;
  }
};
states[313] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.getCmdArgumentState().reset(((Long)yyVals[-2+yyTop]).longValue());
                    yyVal = p.dispatch("on_begin", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[314] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.setState(EXPR_ENDARG);
    return yyVal;
  }
};
states[315] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_paren", null);
    return yyVal;
  }
};
states[316] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.getCmdArgumentState().getStack();
                    p.getCmdArgumentState().reset();
    return yyVal;
  }
};
states[317] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.setState(EXPR_ENDARG); 
    return yyVal;
  }
};
states[318] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.getCmdArgumentState().reset(((Long)yyVals[-3+yyTop]).longValue());
                    p.warning("(...) interpreted as grouped expression");
                    yyVal = p.dispatch("on_paren", ((IRubyObject)yyVals[-2+yyTop]));
    return yyVal;
  }
};
states[319] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_paren", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[320] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_const_path_ref", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[321] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_top_const_ref", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[322] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_array", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[323] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_hash", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[324] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_return0");
    return yyVal;
  }
};
states[325] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_yield", p.dispatch("on_paren", ((IRubyObject)yyVals[-1+yyTop])));
    return yyVal;
  }
};
states[326] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_yield", p.dispatch("on_paren", p.dispatch("on_args_new")));
    return yyVal;
  }
};
states[327] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_yield0");
    return yyVal;
  }
};
states[328] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_defined", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[329] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_unary", p.intern("not"), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[330] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_unary", p.intern("not"), null);
    return yyVal;
  }
};
states[331] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_method_add_block",
                                    p.dispatch("on_method_add_arg", 
                                               p.dispatch("on_fcall", ((IRubyObject)yyVals[-1+yyTop])), 
                                               p.dispatch("on_args_new")), 
                                    ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[333] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_method_add_block", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[334] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[335] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_if", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[336] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_unless", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[337] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.getConditionState().begin();
    return yyVal;
  }
};
states[338] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.getConditionState().end();
    return yyVal;
  }
};
states[339] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_while", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[340] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                  p.getConditionState().begin();
    return yyVal;
  }
};
states[341] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                  p.getConditionState().end();
    return yyVal;
  }
};
states[342] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_until", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[343] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_case", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[344] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_case", null, ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[345] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.getConditionState().begin();
    return yyVal;
  }
};
states[346] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.getConditionState().end();
    return yyVal;
  }
};
states[347] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_for", ((IRubyObject)yyVals[-7+yyTop]), ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[348] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    if (p.isInDef() || p.isInSingle()) {
                        p.yyerror("class definition in method body");
                    }
                    p.pushLocalScope();
    return yyVal;
  }
};
states[349] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_class", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
                    p.popCurrentScope();
    return yyVal;
  }
};
states[350] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = Boolean.valueOf(p.isInDef());
                    p.setInDef(false);
    return yyVal;
  }
};
states[351] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = Integer.valueOf(p.getInSingle());
                    p.setInSingle(0);
                    p.pushLocalScope();
    return yyVal;
  }
};
states[352] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_sclass", ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));

                    p.popCurrentScope();
                    p.setInDef(((Boolean)yyVals[-4+yyTop]).booleanValue());
                    p.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
    return yyVal;
  }
};
states[353] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    if (p.isInDef() || p.isInSingle()) { 
                        p.yyerror("module definition in method body");
                    }
                    p.pushLocalScope();
    return yyVal;
  }
};
states[354] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_module", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
                    p.popCurrentScope();
    return yyVal;
  }
};
states[355] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.setInDef(true);
                    p.pushLocalScope();
                    yyVal = p.getCurrentArg();
                    p.setCurrentArg(null);
    return yyVal;
  }
};
states[356] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_def", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));

                    p.popCurrentScope();
                    p.setInDef(false);
                    p.setCurrentArg(((IRubyObject)yyVals[-3+yyTop]));
    return yyVal;
  }
};
states[357] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.setState(EXPR_FNAME);
    return yyVal;
  }
};
states[358] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.setInSingle(p.getInSingle() + 1);
                    p.pushLocalScope();
                    p.setState(EXPR_ENDFN|EXPR_LABEL); /* force for args */
                    yyVal = p.getCurrentArg();
                    p.setCurrentArg(null);                    
    return yyVal;
  }
};
states[359] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_defs", ((IRubyObject)yyVals[-7+yyTop]), ((IRubyObject)yyVals[-6+yyTop]), ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));

                    p.popCurrentScope();
                    p.setInSingle(p.getInSingle() - 1);
                    p.setCurrentArg(((IRubyObject)yyVals[-3+yyTop]));
    return yyVal;
  }
};
states[360] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_break", p.dispatch("on_args_new"));
    return yyVal;
  }
};
states[361] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_next", p.dispatch("on_args_new"));
    return yyVal;
  }
};
states[362] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_redo");
    return yyVal;
  }
};
states[363] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_retry");
    return yyVal;
  }
};
states[364] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[365] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[367] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[368] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[371] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_elsif", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[373] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_else", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[375] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
    return yyVal;
  }
};
states[376] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[377] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_paren", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[378] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_add", p.dispatch("on_mlhs_new"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[379] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[380] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[381] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[382] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_add_post", 
                                    p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[383] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-2+yyTop]), null);
    return yyVal;
  }
};
states[384] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_add_post", 
                                    p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-4+yyTop]), null),
                                    ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[385] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_add_star",
                                    p.dispatch("on_mlhs_new"),
                                    ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[386] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_add_post", 
                                    p.dispatch("on_mlhs_add_star",
                                               p.dispatch("on_mlhs_new"),
                                               ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[387] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_add_star",
                                    p.dispatch("on_mlhs_new"),
                                    null);
    return yyVal;
  }
};
states[388] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_add_post", 
                                    p.dispatch("on_mlhs_add_star",
                                               p.dispatch("on_mlhs_new"),
                                               null),
                                    ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[389] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args_tail(((RubyArray)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[390] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args_tail(((RubyArray)yyVals[-1+yyTop]), null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[391] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args_tail(null, ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[392] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args_tail(null, null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[393] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[394] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args_tail(null, null, null);
    return yyVal;
  }
};
states[395] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(((RubyArray)yyVals[-5+yyTop]), ((RubyArray)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[396] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(((RubyArray)yyVals[-7+yyTop]), ((RubyArray)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[397] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(((RubyArray)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[398] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(((RubyArray)yyVals[-5+yyTop]), ((RubyArray)yyVals[-3+yyTop]), null, ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[399] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(((RubyArray)yyVals[-3+yyTop]), null, ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[400] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_excessed_comma", 
                                    p.new_args(((RubyArray)yyVals[-1+yyTop]), null, null, null, null));
    return yyVal;
  }
};
states[401] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(((RubyArray)yyVals[-5+yyTop]), null, ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[402] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(((RubyArray)yyVals[-1+yyTop]), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[403] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[404] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[405] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[406] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-3+yyTop]), null, ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[407] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(null, null, ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));     
    return yyVal;
  }
};
states[408] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(null, null, ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[409] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[411] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.setCommandStart(true);
    return yyVal;
  }
};
states[412] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.setCurrentArg(null);  
                    yyVal = p.dispatch("on_block_var", 
                                    p.new_args(null, null, null, null, null), 
                                    ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[413] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_block_var", 
                                    p.new_args(null, null, null, null, null), 
                                    null);
    return yyVal;
  }
};
states[414] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.setCurrentArg(null);
                    yyVal = p.dispatch("on_block_var", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[415] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.getContext().getRuntime().getFalse();
    return yyVal;
  }
};
states[416] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((RubyArray)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[417] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[418] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((RubyArray)yyVals[-2+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[419] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_bv(((IRubyObject)yyVals[0+yyTop]));

    return yyVal;
  }
};
states[420] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[421] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.pushBlockScope();
                    yyVal = p.getLeftParenBegin();
                    p.setLeftParenBegin(p.incrementParenNest());
    return yyVal;
  }
};
states[422] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.getCmdArgumentState().getStack();
                    p.getCmdArgumentState().reset();
    return yyVal;
  }
};
states[423] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_lambda", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
                    p.popCurrentScope();
                    p.setLeftParenBegin(((Integer)yyVals[-3+yyTop]));
                    p.getCmdArgumentState().reset(((Long)yyVals[-2+yyTop]).longValue());
    return yyVal;
  }
};
states[424] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_paren", ((IRubyObject)yyVals[-2+yyTop]));
    return yyVal;
  }
};
states[425] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[426] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[427] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[428] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);  
    return yyVal;
  }
};
states[429] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_method_add_block", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[430] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.method_optarg(p.dispatch("on_call", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[431] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.method_add_block(p.dispatch("on_command_call", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[432] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.method_add_block(p.dispatch("on_command_call", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[433] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_method_add_arg", p.dispatch("on_fcall", ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[434] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.method_optarg(p.dispatch("on_call", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[435] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.method_optarg(p.dispatch("on_call", ((IRubyObject)yyVals[-3+yyTop]), p.intern("::"), ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[436] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_call", ((IRubyObject)yyVals[-2+yyTop]), p.intern("::"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[437] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.method_optarg(p.dispatch("on_call", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), p.intern("call")), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[438] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.method_optarg(p.dispatch("on_call", ((IRubyObject)yyVals[-2+yyTop]), p.intern("::"), p.intern("call")), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[439] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_super", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[440] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_zsuper");
    return yyVal;
  }
};
states[441] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_aref", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[442] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[443] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.pushBlockScope();
                    yyVal = Long.valueOf(p.getCmdArgumentState().getStack());
                    p.getCmdArgumentState().reset();
    return yyVal;
  }
};
states[444] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_do_block", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
                    p.getCmdArgumentState().reset(((Long)yyVals[-3+yyTop]).longValue());
                    p.popCurrentScope();
    return yyVal;
  }
};
states[445] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.pushBlockScope();
                    yyVal = Long.valueOf(p.getCmdArgumentState().getStack()) >> 1;
                    p.getCmdArgumentState().reset();
    return yyVal;
  }
};
states[446] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_brace_block", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
                    p.getCmdArgumentState().reset(((Long)yyVals[-2+yyTop]).longValue());
                    p.popCurrentScope();
    return yyVal;
  }
};
states[447] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.pushBlockScope();
                    yyVal = Long.valueOf(p.getCmdArgumentState().getStack());
                    p.getCmdArgumentState().reset();
    return yyVal;
  }
};
states[448] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_do_block", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
                    p.getCmdArgumentState().reset(((Long)yyVals[-2+yyTop]).longValue());
                    p.popCurrentScope();
    return yyVal;
  }
};
states[449] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_when", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));

    return yyVal;
  }
};
states[452] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_rescue", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[453] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[454] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[455] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[457] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[459] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_ensure", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[462] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_symbol_literal", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[464] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[465] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[466] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[467] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_string_concat", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[468] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.heredoc_dedent(((IRubyObject)yyVals[-1+yyTop]));
                    p.setHeredocIndent(0);
                    yyVal = p.dispatch("on_string_literal", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[469] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.heredoc_dedent(((IRubyObject)yyVals[-1+yyTop]));
                    p.setHeredocIndent(0);
                    yyVal = p.dispatch("on_xstring_literal", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[470] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_regexp_literal", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[471] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_array", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[472] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_words_new");
    return yyVal;
  }
};
states[473] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_words_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[474] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_word_add", p.dispatch("on_word_new"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[475] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_word_add", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[476] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_array", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[477] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_symbols_new");
    return yyVal;
  }
};
states[478] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_symbols_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[479] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_array", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[480] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_array", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[481] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_qwords_new");
    return yyVal;
  }
};
states[482] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_qwords_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[483] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_qsymbols_new");
    return yyVal;
  }
};
states[484] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_qsymbols_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[485] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_string_content");
    return yyVal;
  }
};
states[486] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_string_add", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[487] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_xstring_new");
    return yyVal;
  }
};
states[488] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_xstring_add", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[489] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_regexp_new");
    return yyVal;
  }
};
states[490] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_regexp_add", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[492] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.getStrTerm();
                    p.setStrTerm(null);
                    p.setState(EXPR_BEG);
    return yyVal;
  }
};
states[493] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.setStrTerm(((StrTerm)yyVals[-1+yyTop]));
                    yyVal = p.dispatch("on_string_dvar", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[494] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = p.getStrTerm();
                   p.setStrTerm(null);
                   p.getConditionState().stop();
    return yyVal;
  }
};
states[495] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = p.getCmdArgumentState().getStack();
                   p.getCmdArgumentState().reset();
    return yyVal;
  }
};
states[496] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = p.getState();
                   p.setState(EXPR_BEG);
    return yyVal;
  }
};
states[497] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = p.getBraceNest();
                   p.setBraceNest(0);
    return yyVal;
  }
};
states[498] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = p.getHeredocIndent();
                   p.setHeredocIndent(0);
    return yyVal;
  }
};
states[499] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                   p.getConditionState().restart();
                   p.setStrTerm(((StrTerm)yyVals[-6+yyTop]));
                   p.getCmdArgumentState().reset(((Long)yyVals[-5+yyTop]).longValue());
                   p.setState(((Integer)yyVals[-4+yyTop]));
                   p.setBraceNest(((Integer)yyVals[-3+yyTop]));
                   p.setHeredocIndent(((Integer)yyVals[-2+yyTop]));
                   yyVal = p.dispatch("on_string_embexpr", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[500] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[501] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[502] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[504] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                     p.setState(EXPR_END|EXPR_ENDARG);
                     yyVal = p.dispatch("on_symbol", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[509] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                     p.setState(EXPR_END|EXPR_ENDARG);
                     yyVal = p.dispatch("on_dyna_symbol", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[510] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[511] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_unary", p.intern("-@"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[512] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[513] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[514] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[515] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[516] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    if (p.is_id_var()) {
                        yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
                    } else {
                        yyVal = p.dispatch("on_vcall", ((IRubyObject)yyVals[0+yyTop]));
                    }
    return yyVal;
  }
};
states[517] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[518] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[519] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[520] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[521] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[522] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[523] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[524] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[525] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[526] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[527] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[528] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_field", p.assignableIdentifier(((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[529] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[530] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[531] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.assignableConstant(p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[532] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[533] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.yyerror("Can't assign to nil");
    return yyVal;
  }
};
states[534] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.yyerror("Can't change the value of self");
    return yyVal;
  }
};
states[535] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.yyerror("Can't assign to true");
    return yyVal;
  }
};
states[536] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.yyerror("Can't assign to false");
    return yyVal;
  }
};
states[537] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.yyerror("Can't assign to __FILE__");
    return yyVal;
  }
};
states[538] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.yyerror("Can't assign to __LINE__");
    return yyVal;
  }
};
states[539] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.yyerror("Can't assign to __ENCODING__");
    return yyVal;
  }
};
states[542] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                   p.setState(EXPR_BEG);
                   p.setCommandStart(true);
    return yyVal;
  }
};
states[543] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[544] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = null;
    return yyVal;
  }
};
states[545] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.setState(EXPR_BEG);
                    p.setCommandStart(true);
                    yyVal = p.dispatch("on_paren", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[546] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
  /* $$ = lexer.inKwarg;*/
                   /*                   p.inKwarg = true;*/
                   p.setState(p.getState() | EXPR_LABEL);
    return yyVal;
  }
};
states[547] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
  /* p.inKwarg = $<Boolean>1;*/
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
                    p.setState(EXPR_BEG);
                    p.setCommandStart(true);
    return yyVal;
  }
};
states[548] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args_tail(((RubyArray)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[549] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args_tail(((RubyArray)yyVals[-1+yyTop]), null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[550] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args_tail(null, ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[551] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args_tail(null, null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[552] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[553] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args_tail(null, null, null);
    return yyVal;
  }
};
states[554] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(((RubyArray)yyVals[-5+yyTop]), ((RubyArray)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[555] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(((RubyArray)yyVals[-7+yyTop]), ((RubyArray)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[556] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(((RubyArray)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[557] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(((RubyArray)yyVals[-5+yyTop]), ((RubyArray)yyVals[-3+yyTop]), null, ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[558] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(((RubyArray)yyVals[-3+yyTop]), null, ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[559] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(((RubyArray)yyVals[-5+yyTop]), null, ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[560] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(((RubyArray)yyVals[-1+yyTop]), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[561] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[562] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[563] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[564] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-3+yyTop]), null, ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[565] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(null, null, ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[566] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(null, null, ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[567] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[568] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_args(null, null, null, null, null);
    return yyVal;
  }
};
states[569] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_param_error", ((IRubyObject)yyVals[0+yyTop]));
                    p.error();
    return yyVal;
  }
};
states[570] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_param_error", ((IRubyObject)yyVals[0+yyTop]));
                    p.error();
    return yyVal;
  }
};
states[571] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_param_error", ((IRubyObject)yyVals[0+yyTop]));
                    p.error();
    return yyVal;
  }
};
states[572] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_param_error", ((IRubyObject)yyVals[0+yyTop]));
                    p.error();
    return yyVal;
  }
};
states[574] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.arg_var(p.formal_argument(((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[575] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.setCurrentArg(((IRubyObject)yyVals[0+yyTop]));
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[576] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.setCurrentArg(null);
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[577] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_mlhs_paren", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[578] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[579] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((RubyArray)yyVals[-2+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[580] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.arg_var(p.formal_argument(((IRubyObject)yyVals[0+yyTop])));
                    p.setCurrentArg(((IRubyObject)yyVals[0+yyTop]));
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[581] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.setCurrentArg(null);
                    yyVal = p.keyword_arg(((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[582] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.setCurrentArg(null);
                    yyVal = p.keyword_arg(((IRubyObject)yyVals[0+yyTop]), p.getContext().getRuntime().getFalse());
    return yyVal;
  }
};
states[583] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.keyword_arg(((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[584] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.keyword_arg(((IRubyObject)yyVals[0+yyTop]), p.getContext().getRuntime().getFalse());
    return yyVal;
  }
};
states[585] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[586] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((RubyArray)yyVals[-2+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[587] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[588] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((RubyArray)yyVals[-2+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[589] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[590] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[591] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.shadowing_lvar(((IRubyObject)yyVals[0+yyTop]));
                    yyVal = p.dispatch("on_kwrest_param", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[592] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_kwrest_param", null);
    return yyVal;
  }
};
states[593] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.setCurrentArg(null);
                    yyVal = p.new_assoc(((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));

    return yyVal;
  }
};
states[594] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.setCurrentArg(null);
                    yyVal = p.new_assoc(((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[595] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[596] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((RubyArray)yyVals[-2+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[597] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[598] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((RubyArray)yyVals[-2+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[601] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.arg_var(p.shadowing_lvar(((IRubyObject)yyVals[0+yyTop])));
                    yyVal = p.dispatch("on_rest_param", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[602] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_rest_param", null);
    return yyVal;
  }
};
states[605] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.arg_var(p.shadowing_lvar(((IRubyObject)yyVals[0+yyTop])));
                    yyVal = p.dispatch("on_blockarg", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[606] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[607] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[608] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[609] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    p.setState(EXPR_BEG);
    return yyVal;
  }
};
states[610] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_paren", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[612] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assoclist_from_args", ((RubyArray)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[613] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[614] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((RubyArray)yyVals[-2+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[615] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assoc_new", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[616] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assoc_new", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[617] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assoc_new", p.dispatch("on_dyna_symbol", ((IRubyObject)yyVals[-2+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[618] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.dispatch("on_assoc_splat", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[629] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[630] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[631] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.intern(".");
    return yyVal;
  }
};
states[632] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = p.intern("&.");
    return yyVal;
  }
};
states[633] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[634] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = p.intern("::");
    return yyVal;
  }
};
states[639] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[640] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[648] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                      yyVal = null;
    return yyVal;
  }
};
states[649] = new RipperParserState() {
  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = null;
    return yyVal;
  }
};
}
					// line 2186 "RipperParser.y"
}
					// line 9736 "-"
