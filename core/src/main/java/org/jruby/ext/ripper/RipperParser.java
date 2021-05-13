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
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
					// line 53 "-"
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
//yyLhs 651
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
    41,    41,    41,   136,   136,   136,   136,    51,    51,    76,
    79,    79,    79,    79,    62,    62,    54,    58,    58,   130,
   130,   130,   130,   130,    52,    52,    52,    52,    52,   160,
    56,   107,   106,   106,    82,    82,    82,    82,    35,    35,
    73,    73,    73,    42,    42,    42,    42,    42,    42,    42,
    42,    42,    42,    42,   161,    42,   162,    42,   163,   164,
    42,    42,    42,    42,    42,    42,    42,    42,    42,    42,
    42,    42,    42,    42,    42,    42,    42,    42,    42,   165,
   166,    42,   167,   168,    42,    42,    42,   169,   170,    42,
   171,    42,   173,    42,   174,    42,   175,    42,   176,   177,
    42,    42,    42,    42,    42,    46,   150,   150,   150,   149,
   149,    49,    49,    47,    47,   129,   129,   131,   131,    87,
    87,   132,   132,   132,   132,   132,   132,   132,   132,   132,
    94,    94,    94,    94,    93,    93,    69,    69,    69,    69,
    69,    69,    69,    69,    69,    69,    69,    69,    69,    69,
    69,    71,    71,    70,    70,    70,   124,   124,   123,   123,
   133,   133,   178,   179,   126,    68,    68,   125,   125,   112,
    59,    59,    59,    59,    22,    22,    22,    22,    22,    22,
    22,    22,    22,   111,   180,   111,   181,   114,   182,   115,
    77,    48,    48,   118,   118,    78,    78,    78,    50,    50,
    53,    53,    28,    28,    28,    15,    16,    16,    16,    17,
    18,    19,    25,    84,    84,    27,    27,    90,    88,    88,
    26,    91,    83,    83,    89,    89,    20,    20,    21,    21,
    24,    24,    23,   183,    23,   184,   185,   186,   187,   188,
    23,    65,    65,    65,    65,     2,     1,     1,     1,     1,
    29,    33,    33,    34,    34,    34,    34,    57,    57,    57,
    57,    57,    57,    57,    57,    57,    57,    57,    57,   119,
   119,   119,   119,   119,   119,   119,   119,   119,   119,   119,
   119,    66,    66,   189,    55,    55,    72,   190,    72,    95,
    95,    95,    95,    92,    92,    67,    67,    67,    67,    67,
    67,    67,    67,    67,    67,    67,    67,    67,    67,    67,
   135,   135,   135,   135,     9,     9,   147,   122,   122,    85,
    85,   144,    96,    96,    97,    97,    98,    98,    99,    99,
   142,   142,   143,   143,    63,   128,   105,   105,    86,    86,
    10,    10,    13,    13,    12,    12,   110,   109,   109,    14,
   191,    14,   100,   100,   101,   101,   102,   102,   102,   102,
     3,     3,     3,     4,     4,     4,     4,     5,     5,     5,
    11,    11,   145,   145,   146,   146,   152,   152,   157,   157,
   137,   138,   159,   159,   159,   172,   172,   153,   153,    81,
   108,
    }, yyLen = {
//yyLen 651
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
     3,     3,     3,     2,     2,     3,     3,     3,     3,     3,
     3,     4,     2,     2,     3,     3,     3,     3,     1,     3,
     3,     3,     3,     3,     2,     2,     3,     3,     3,     3,
     3,     6,     1,     1,     1,     1,     1,     3,     3,     1,
     1,     2,     4,     2,     1,     3,     3,     1,     1,     1,
     1,     2,     4,     2,     1,     2,     2,     4,     1,     0,
     2,     2,     2,     1,     1,     2,     3,     4,     1,     1,
     3,     4,     2,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     0,     4,     0,     3,     0,     0,
     5,     3,     3,     2,     3,     3,     1,     4,     3,     1,
     5,     4,     3,     2,     1,     2,     2,     6,     6,     0,
     0,     7,     0,     0,     7,     5,     4,     0,     0,     9,
     0,     6,     0,     7,     0,     5,     0,     6,     0,     0,
     9,     1,     1,     1,     1,     1,     1,     1,     2,     1,
     1,     1,     5,     1,     2,     1,     1,     1,     3,     1,
     3,     1,     4,     6,     3,     5,     2,     4,     1,     3,
     4,     2,     2,     1,     2,     0,     6,     8,     4,     6,
     4,     2,     6,     2,     4,     6,     2,     4,     2,     4,
     1,     1,     1,     3,     1,     4,     1,     4,     1,     3,
     1,     1,     0,     0,     4,     4,     1,     3,     3,     3,
     2,     4,     5,     5,     2,     4,     4,     3,     3,     3,
     2,     1,     4,     3,     0,     5,     0,     3,     0,     3,
     5,     1,     1,     6,     0,     1,     1,     1,     2,     1,
     2,     1,     1,     1,     1,     1,     1,     1,     2,     3,
     3,     3,     4,     0,     3,     1,     2,     4,     0,     3,
     4,     4,     0,     3,     0,     3,     0,     2,     0,     2,
     0,     2,     1,     0,     3,     0,     0,     0,     0,     0,
     8,     1,     1,     1,     1,     2,     1,     1,     1,     1,
     3,     1,     2,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     0,     4,     0,     3,     0,     3,     4,
     2,     2,     1,     2,     0,     6,     8,     4,     6,     4,
     6,     2,     4,     6,     2,     4,     2,     4,     1,     0,
     1,     1,     1,     1,     1,     1,     1,     1,     3,     1,
     3,     1,     2,     1,     2,     1,     1,     3,     1,     3,
     1,     1,     2,     1,     3,     3,     1,     3,     1,     3,
     1,     1,     2,     1,     1,     1,     2,     2,     0,     1,
     0,     4,     1,     2,     1,     3,     3,     2,     4,     2,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     0,     1,     0,     1,
     2,     2,     0,     1,     1,     1,     1,     1,     2,     0,
     0,
    }, yyDefRed = {
//yyDefRed 1107
     1,     0,     0,     0,     0,     0,     0,     0,   314,     0,
     0,     0,   339,   342,     0,     0,     0,   363,   364,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     9,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   466,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   486,   488,   490,     0,     0,   422,   541,
   542,   513,   516,   514,   515,     0,     0,   463,    60,   304,
     0,   467,   305,   306,     0,   307,   308,   303,   464,    32,
    48,   462,   511,     0,     0,     0,     0,     0,     0,     0,
   311,     0,    56,     0,     0,    86,     0,     4,   309,   310,
     0,     0,    72,     0,     2,     0,     5,     0,     7,   361,
   362,   326,     0,     0,   523,   522,   524,   525,     0,     0,
   527,   526,   528,     0,   519,   518,     0,   521,     0,     0,
     0,     0,   133,     0,   365,     0,   312,     0,   354,   186,
   197,   187,   210,   183,   203,   193,   192,   213,   214,   208,
   191,   190,   185,   211,   215,   216,   195,   184,   198,   202,
   204,   196,   189,   205,   212,   207,     0,     0,     0,     0,
   182,   201,   200,   217,   218,   219,   220,   221,   181,   188,
   179,   180,     0,     0,     0,     0,   137,     0,   171,   172,
   168,   150,   151,   152,   159,   156,   158,   153,   154,   173,
   174,   160,   161,   610,   165,   164,   149,   170,   167,   166,
   162,   163,   157,   155,   147,   169,   148,   175,   138,   356,
     0,   609,   139,   206,   199,   209,   194,   176,   177,   178,
   135,   136,   141,   140,   143,     0,   142,   144,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   645,
   646,     0,     0,     0,   647,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   375,   376,     0,     0,     0,     0,     0,   486,
     0,     0,   284,    70,     0,     0,     0,   614,   288,    71,
    69,     0,    68,     0,     0,   440,    67,     0,   639,     0,
     0,    20,     0,     0,     0,   242,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,    14,    13,     0,     0,
     0,     0,     0,   270,     0,     0,     0,   612,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   255,    52,   254,
   508,   507,   509,   505,   506,     0,     0,     0,     0,   473,
   482,   336,     0,   478,   484,   468,   444,   446,   335,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   265,   266,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   264,   263,     0,
     0,     0,     0,   448,   430,   632,   633,     0,     0,     0,
     0,   635,   634,     0,     0,    88,     0,     0,     0,     0,
     0,     0,     3,     0,   434,     0,   333,     0,   512,     0,
   130,     0,   132,   543,   350,     0,     0,     0,     0,     0,
     0,   630,   631,   358,   145,     0,     0,     0,   367,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   648,     0,     0,     0,     0,     0,     0,   347,   617,
   295,   291,     0,   619,     0,     0,   285,   293,     0,   286,
     0,   328,     0,   290,   280,   279,     0,     0,     0,     0,
   332,    51,    22,    24,    23,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   321,    12,     0,
     0,   317,     0,   324,     0,   643,   271,     0,   273,   325,
   613,     0,    90,     0,     0,     0,     0,     0,   495,   493,
   510,   492,   487,   469,   470,   489,   471,   491,     0,     0,
   575,   572,   571,   570,   573,   581,   590,     0,     0,   601,
   600,   605,   604,   591,   576,     0,     0,     0,   598,   426,
   423,     0,     0,   568,   588,     0,   552,   579,   574,     0,
     0,     0,     0,     0,     0,     0,     0,     0,    26,    27,
    28,    29,    30,    49,    50,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   437,     0,   439,     0,     0,   625,
     0,     0,   626,   438,     0,   623,   624,     0,    47,     0,
     0,     0,    44,   230,     0,     0,     0,     0,    37,   222,
    34,   294,     0,     0,     0,     0,    89,    33,    35,   298,
     0,    38,   223,     6,   446,    62,     0,     0,     0,     0,
     0,     0,   134,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   315,     0,   368,     0,     0,     0,
     0,     0,     0,     0,     0,   346,   370,   340,   369,   343,
     0,     0,     0,     0,     0,     0,     0,   616,     0,     0,
     0,   292,   615,   327,   640,     0,     0,   276,   331,    21,
     0,     0,    31,     0,   229,     0,     0,    15,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   496,     0,   472,
   475,     0,   480,     0,     0,     0,   377,     0,   379,     0,
     0,   602,   606,     0,   566,     0,     0,   561,     0,   564,
     0,   550,   592,     0,   551,   582,     0,   477,     0,   481,
     0,   414,     0,   412,     0,   411,   443,     0,     0,   429,
     0,     0,     0,   436,     0,     0,     0,     0,     0,   278,
     0,   435,   277,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,    87,     0,     0,     0,     0,     0,     0,
   442,   355,   611,     0,     0,     0,   359,   146,   456,     0,
     0,   457,     0,     0,   373,     0,   371,     0,     0,     0,
     0,     0,     0,     0,   345,     0,     0,     0,     0,     0,
     0,   618,   297,   287,     0,   330,    10,     0,   320,   272,
    91,     0,   497,   501,   502,   503,   494,   504,   474,   476,
   483,     0,     0,     0,     0,   578,     0,     0,     0,   553,
   577,     0,     0,   424,     0,     0,   580,     0,   599,     0,
   589,   607,     0,   594,   479,   485,     0,     0,     0,   410,
   586,     0,     0,   393,     0,   596,     0,     0,     0,     0,
   447,     0,   449,    43,   227,    42,   228,    66,     0,   641,
    40,   225,    41,   226,    64,   433,   432,    46,     0,     0,
     0,     0,     0,     0,     0,     0,     0,    59,     0,   544,
   351,   546,   357,   548,     0,     0,     0,   459,   374,     0,
    11,   461,     0,   337,     0,   338,   296,     0,     0,     0,
   348,     0,    19,   498,   378,     0,     0,     0,   380,   425,
     0,     0,   567,     0,     0,     0,   559,     0,   557,     0,
   562,   565,   549,     0,   408,     0,     0,   403,     0,   391,
     0,   406,   413,   392,     0,     0,     0,     0,     0,   445,
     0,     0,    39,     0,     0,   353,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   458,     0,   460,     0,   451,   450,   452,   341,
   344,     0,   499,     0,     0,     0,     0,   420,     0,   418,
   421,   428,   427,     0,     0,     0,     0,     0,   394,   415,
     0,     0,   587,     0,     0,     0,   597,   323,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   417,   560,     0,   555,   558,   563,     0,   409,
     0,   400,     0,   398,   390,     0,   404,   407,     0,     0,
   360,     0,     0,     0,     0,     0,   453,   372,   349,     0,
     0,   419,     0,     0,     0,     0,     0,     0,   500,   556,
   402,     0,   396,   399,   405,     0,   397,
    }, yyDgoto = {
//yyDgoto 192
     1,   363,    67,    68,   671,   624,   625,   233,   132,   564,
   565,   453,   566,   567,   220,    69,    70,    71,    72,    73,
   365,   367,    74,   542,   368,    75,    76,   741,    77,    78,
   133,    79,    80,    81,    82,   658,   455,   456,   324,   325,
    84,    85,    86,    87,   326,   253,   316,   826,  1017,   827,
   936,    89,   494,   940,   626,   444,   302,    90,   791,    91,
    92,   648,   649,   568,   235,   856,   255,   569,   570,   887,
   773,   774,   678,   650,    94,    95,   294,   470,   820,   332,
   256,   327,   496,   549,   548,   571,   572,   747,   583,   584,
    98,    99,   754,   974,  1038,   869,   574,   890,   891,   575,
   338,   497,   297,   100,   533,   892,   486,   298,   487,   761,
   576,   436,   414,   665,   586,   618,   101,   102,   683,   257,
   236,   237,   577,  1028,   866,   873,   371,   329,   895,   284,
   498,   748,   749,  1029,   222,   578,   412,   491,   785,   104,
   105,   106,   579,   580,   581,   447,   423,   870,   107,   697,
   459,     2,   262,   263,   313,   515,   505,   492,   681,   526,
   303,   238,   330,   331,   728,   265,   837,   266,   838,   705,
  1021,   668,   460,   666,   448,   450,   680,   934,   372,   755,
   585,   587,   619,   738,   737,   852,   953,  1022,  1058,   667,
   679,   449,
    }, yySindex = {
//yySindex 1107
     0,     0, 19727, 21024, 22701, 23088, 23830, 23721,     0, 22185,
 22185, 18943,     0,     0, 22830, 20114, 20114,     0,     0, 20114,
  -314,  -186,     0,     0,     0,     0,   132, 23612,   159,     0,
  -130,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 22314, 22314,   639,   -69, 19856,     0, 20504, 20894, 19211,
 22314, 22443, 23938,     0,     0,     0,   219,   222,     0,     0,
     0,     0,     0,     0,     0,   242,   250,     0,     0,     0,
   -68,     0,     0,     0,  -135,     0,     0,     0,     0,     0,
     0,     0,     0,  1686,    58,  5637,     0,    31,   728,   422,
     0,   434,     0,   -29,   273,     0,   259,     0,     0,     0,
 22959,   291,     0,    -9,     0,   252,     0,  -101,     0,     0,
     0,     0,  -314,  -186,     0,     0,     0,     0,    16,   159,
     0,     0,     0,     0,     0,     0,     0,     0,   639, 22185,
  -113, 19985,     0,    38,     0,   745,     0,  -101,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  -137,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   321,     0,     0, 19985,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   107,    58,   345,   854,    86,   378,   205,   345,     0,
     0,   252,   280,   530,     0, 22185, 22185,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   314,
   872,     0,     0,     0,   370, 22314, 22314, 22314, 22314,     0,
 22314,  5637,     0,     0,   335,   623,   648,     0,     0,     0,
     0, 17155,     0, 20114, 20114,     0,     0, 19072,     0, 22185,
   -87,     0, 21282,   341, 19985,     0,   887,   403,   406,   396,
 21153,     0, 19856,   408,   252,  1686,     0,     0,     0,   159,
   159, 21153,   411,     0,   166,   263,   335,     0,   390,   263,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   456, 23217,  1094,     0,   724,     0,     0,     0,
     0,     0,     0,     0,     0,   694,  1256,  1388,   431,     0,
     0,     0,  2585,     0,     0,     0,     0,     0,     0, 22185,
 22185, 22185, 22185, 21153, 22185, 22185, 22314, 22314, 22314, 22314,
 22314,     0,     0, 22314, 22314, 22314, 22314, 22314, 22314, 22314,
 22314, 22314, 22314, 22314, 22314, 22314, 22314,     0,     0, 22314,
 22314, 22314, 22314,     0,     0,     0,     0,  5973, 20114,  6109,
 22314,     0,     0,  5119, 22443,     0, 21411, 19856, 19340,   730,
 21411, 22443,     0, 19469,     0,   425,     0,   436,     0,    58,
     0,     0,     0,     0,     0,  6485, 20114,  6621, 19985, 22185,
   443,     0,     0,     0,     0,   525,   528,   396,     0, 19985,
   533,  6997, 20114,  7133, 22314, 22314, 22314, 19985,   280, 21540,
   541,     0,    81,    81,     0, 10005, 20114, 13016,     0,     0,
     0,     0,   268,     0, 22314, 20244,     0,     0, 20634,     0,
   159,     0,   455,     0,     0,     0,   767,   769,   159,   320,
     0,     0,     0,     0,     0, 23721, 22185,  5637, 19727,   452,
  6997,  7133, 22314, 22314,  1686,   458,   159,     0,     0, 19598,
     0,     0,  1686,     0, 20764,     0,     0, 20894,     0,     0,
     0,     0,     0,   773, 14396, 20114, 24102, 23217,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  1402,   270,
     0,     0,     0,     0,     0,     0,     0,  1278,  4525,     0,
     0,     0,     0,     0,     0,   512,   519,   786,     0,     0,
     0,   788,   791,     0,     0,   796,     0,     0,     0,   542,
   806, 22314,   793,  1488,   374,    20,   502,    20,     0,     0,
     0,     0,     0,     0,     0,   403,  3444,  3444,  3444,  3444,
  4075,  3563,  3444,  3444,  3060,  3060,  1654,  1654,   403,  2377,
   403,   403,   364,   364,  2552,  2552,  2488,  2927,   592,    20,
   529,     0,   531,  -186,     0,     0,     0,   159,   536,     0,
   544,  -186,     0,     0,  2927,     0,     0,  -186,     0,   569,
  4624,  1120,     0,     0,   -29,   815, 22314,  4624,     0,     0,
     0,     0,   845,   159, 23217,   846,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   252, 22185, 19985,     0,
     0,  -186,     0,   159,  -186,   629,   320,  4525, 19985,  4525,
 24046, 23721, 21669,   631,     0,   391,     0,   565,   570,   159,
   577,   582,   631,   657,   105,     0,     0,     0,     0,     0,
     0,     0,   159,     0,     0, 22185, 22314,     0, 22314,   335,
   648,     0,     0,     0,     0, 20244, 20634,     0,     0,     0,
   320,   560,     0,   403,     0, 19727,     0,     0,   159,   263,
 23217,     0,     0,   159,     0,     0,   773,     0,   864,     0,
     0,   186,     0,   892,  1278,   608,     0,   883,     0,   159,
   159,     0,     0,  2440,     0,  -144,  4525,     0,  4525,     0,
   349,     0,     0,   327,     0,     0, 22314,     0,   245,     0,
   898,     0,   925,     0, 19985,     0,     0, 19985,   879,     0,
 19985, 22443, 22443,     0,   425,   604,   598, 22443, 22443,     0,
   425,     0,     0,    31,  -135, 21153, 22314, 24158, 20114, 24214,
 22443,     0, 21798,     0,   773, 23217,   584, 19985,   252,   684,
     0,     0,     0,   159,   688,   252,     0,     0,     0,     0,
   617,     0, 19985,   698,     0, 22185,     0,   700, 22314, 22314,
   619, 22314, 22314,   702,     0, 21927, 19985, 19985, 19985,     0,
    81,     0,     0,     0,   918,     0,     0,   607,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   159,  1401,   932,  1302,     0,   645,   929,   948,     0,
     0, 19985, 19985,     0,   951,   955,     0,   956,     0,   948,
     0,     0,   806,     0,     0,     0,   960,   159,   961,     0,
     0,   962,   968,     0,   673,     0,   806, 23346,   966,   770,
     0, 22314,     0,     0,     0,     0,     0,     0, 22443,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  5637,   529,
   531,   159,   536,   544, 22314,     0,   773,     0,   772,     0,
     0,     0,     0,     0,   443, 23475,   345,     0,     0, 19985,
     0,     0,   345,     0, 22314,     0,     0,  -103,   774,   783,
     0, 20634,     0,     0,     0,   989,  1401,   720,     0,     0,
  1632,  2440,     0,   784,   674,  2440,     0,  4525,     0,  2440,
     0,     0,     0,  2440,     0,   691,  4525,     0,   349,     0,
  4525,     0,     0,     0,     0,     0,   742,  1167, 23346,     0,
  5637,  5637,     0,   604,     0,     0, 19985,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   743,
  1177,     0,     0, 19985,     0, 19985,     0,     0,     0,     0,
     0, 19985,     0,  1401,   989,  1401,  1016,     0,   628,     0,
     0,     0,     0,   948,  1024,   948,   948,  1025,     0,     0,
  1034,  1039,     0,   806,  1041,  1025,     0,     0, 24270,  1167,
   825,     0, 24326, 20114, 24382,   525,   391,   826, 19985,   989,
  1401,  1632,     0,     0,  2440,     0,     0,     0,  2440,     0,
  2440,     0,  4525,     0,     0,  2440,     0,     0,     0,     0,
     0,     0,     0,   159,     0,     0,     0,     0,     0,   696,
   989,     0,   948,  1025,  1049,  1025,  1025,     0,     0,     0,
     0,  2440,     0,     0,     0,  1025,     0,
    }, yyRindex = {
//yyRindex 1107
     0,     0,   285,     0,     0,     0,     0,     0,     0,     0,
     0,   823,     0,     0,     0, 10673, 10776,     0,     0, 10888,
  4868,  4356, 12433, 12589, 12734, 12837, 22572,     0, 22056,     0,
     0, 12949, 13138, 13250,  5230,  3332, 13353, 13490,  5369, 13654,
     0,     0,     0,     0,     0,    69, 18814,   750,   734,   117,
     0,     0,  1503,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  9938,     0,     0,     0, 10127,     0,     0,     0,     0,     0,
     0,     0,     0,    49, 13600,  1522, 10244,  1988,     0, 13796,
     0,  8783,     0, 13955,     0,     0,     0,     0,     0,     0,
   124,     0,     0,     0,     0,    74,     0, 20374,     0,     0,
     0,     0, 10367,  7994,     0,     0,     0,     0,     0,   757,
     0,     0,     0, 17295,     0,     0, 17435,     0,     0,     0,
     0,    69,     0, 18275,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,  1459,  2387,  2922,  3009,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  3434,  3521,  3946,  4033,     0,  4458,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0, 17056,     0,     0,  1074,  8117,
  8234,  8423,  8540,  8663,  8846,  8969,  2167,  9086,  9275,  2308,
  9392,     0,  2032,     0,     0,  9698,     0,     0,     0,     0,
     0,   823,     0,   832,     0,     0,     0,   858,  1173,  1311,
  1639,  1705,  1798,  1824,  1969,  2009,  2019,  2968,  2496,     0,
     0,  2595,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 16601,     0,     0,  1733, 16828, 16828,     0,     0,     0,
     0,   762,     0,     0,    71,     0,     0,   762,     0,     0,
     0,     0,     0,     0,    85,     0,     0, 11322, 10550, 14095,
     0, 18135,    69,     0,  2514,  1228,     0,     0,   164,   762,
   762,     0,     0,     0,   764,   764,     0,     0,     0,   744,
  1118,  1313,  1846,  1848,  1926,  7281,  8334,  1052,  8605,  8756,
  1096,  9186,     0,     0,     0,  9457,   226,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   -76,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0, 11082, 11219,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,    56,     0,
     0,     0,     0,     0,     0,     0,     0,    69,   276,   290,
     0,     0,     0,    89,     0, 13899,     0,     0,     0,   265,
     0, 17855,     0,     0,     0,     0,    56,     0,  1074,     0,
  4545,     0,     0,     0,     0,   643,     0,  9821,     0,   704,
 17995,     0,    56,     0,     0,     0,     0,   732,     0,     0,
     0,     0,     0,     0,  3008,     0,    56,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   762,     0,     0,     0,     0,     0,    26,    26,   762,   762,
     0,     0,     0,     0,     0,     0,     0,  5060,    85,     0,
     0,     0,     0,     0,  1257,     0,   762,     0,     0,  2524,
   581,     0,   218,     0,   776,     0,     0,   -44,     0,     0,
     0,  9608,     0,   308,     0,    56,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   185,     0,
     0,     0,     0,     0,     0,   538,     0,   179,     0,     0,
     0,   179,   179,     0,     0,   190,     0,     0,     0,   563,
   190,   160,   171,     0,     0, 18545,     0, 18684,     0,     0,
     0,     0,     0,     0,     0, 11459, 15432, 15549, 15658, 15745,
 15848, 16125, 15935, 16022, 16212, 16299, 14623, 14726, 11595, 14844,
 11704, 11821, 14198, 14317, 14966, 15088,  1054, 15206,     0, 18415,
  5742,  3705,  7278, 20374,     0,  3844,     0,   778,  5881,     0,
  6254,  4729,     0,     0, 15309,     0,     0,  7640,     0, 11025,
 16692,     0,     0,     0, 14504,     0,     0, 17006,     0,     0,
     0,     0,     0,   762,     0,   312,     0,     0,     0,     0,
 17020,     0,     0,     0,     0,     0,     0,     0,  1074, 17575,
 17715,     0,     0,   778,  9515,     0,   762,   232,  1074,   328,
     0,     0,    99,   438,     0,   863,     0,  2681,  4217,   778,
  2820,  3193,   863,     0,     0,     0,     0,     0,     0,     0,
  3480,   820,   778,  3992,  4504,     0,     0,     0,     0, 16731,
 16828,     0,     0,     0,     0,    78,   173,     0,     0,     0,
   762,     0,     0, 11977,     0,    85,   119,     0,   762,   764,
     0,  1356,   835,   778,  1570,  1804,   552,     0,     0,     0,
     0,     0,     0,     0,     0,   235,     0,   236,     0,   762,
     8,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,    41,     0,   125,     0,     0,    85,     0,     0,
  1074,     0,     0,     0, 16866, 12127,     0,     0,     0,     0,
 16931,     0,     0,  9635,  1115,     0,     0,     0,    56,     0,
     0,  1282,     0,     0,   583,     0,     0,  1074,     0,     0,
     0,     0,     0,   762,     0,     0,     0,     0,     0,   143,
   540,     0,   100,   863,     0,     0,     0,     0,     0,     0,
  7779,     0,     0,     0,     0,     0,   106,   125,   125,   967,
     0,     0,     0,     0,    26,     0,     0,     0,     0,     0,
     0,  1833,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   762,     0,   237,     0,     0,     0,  -165,   179,     0,
     0,   125,    85,     0,   179,   179,     0,   179,     0,   179,
     0,     0,   190,     0,     0,     0,    43,    41,    43,     0,
     0,    87,    43,     0,     0,     0,    87,    67,    95,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0, 16390,  6393,
  7417,   778,  6766,  6905,     0, 10487,   625,     0,     0,     0,
     0,     0,     0,     0,  4545,     0,     0,     0,     0,   125,
     0,     0,     0,     0,     0,     0,     0,   863,     0,     0,
     0,   202,     0,     0,     0,   243,     0,   246,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,    97,     0,     0,     0,
     0,     0,     0,     0,  1281,  1844,     0,   157,     0,     0,
 16452, 16539,     0, 12283, 16970,     0,  1074,   891,  1092,  1106,
  1269,  1863,  2045,  2072,  5559,  3061,  3573,  6071,  4085,     0,
     0,  4597,     0,  1074,     0,   704,     0,     0,     0,     0,
     0,   125,     0,     0,   258,     0,   260,     0,  -146,     0,
     0,     0,     0,   179,   179,   179,   179,    43,     0,     0,
    43,    43,     0,    87,    43,    43,     0,     0,     0,   165,
     0,  5610,     0,    56,     0,   643,   863,     0,    23,   284,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  5057,   751,
     0,  6583,   794,   778,  7095,  7879,     0,     0,     0,     0,
   286,     0,   179,    43,    43,    43,    43,   493,     0,     0,
     0,     0,     0,     0,     0,    43,     0,
    }, yyGindex = {
//yyGindex 192
     0,     0,     7,     0,  -325,     0,    17,     2,    30,  -266,
     0,     0,     0,   -21,     0,     0,     0,  1048,     0,     0,
    37,     0,     0,  -311,     0,     0,     0,   548,     0,    35,
  1131,  -167,   -33,     0,    70,     0,   331,  -413,     0,    68,
   224,  1270,    82,    24,   620,    27,     1,  -569,     0,    88,
     0,     0,   668,     0,    34,     0,    96,  1139,   509,     0,
     0,  -366,   468,  -621,     0,     0,   203,  -396,     0,     0,
     0,  -397,   213,  -346,   -93,   -23,  1441,  -459,     0,     0,
   527,   547,    57,     0,     0,   984,   393,   -86,     0,     0,
     0,     0,     9,  1316,   382,   167,   399,   172,     0,     0,
     0,    13,  -447,     0,  -431,   188,  -277,  -437,     0,  -535,
  4842,   -73,   387,  -478,   517,     0,  1174,    18,   137,   693,
     0,    -6,  -190,     0,  -624,     0,     0,  -201,  -829,     0,
  -343,  -739,   454,   135,     0,  -803,  1117,  -157,  -590,  -390,
     0,    10,     0,  1239,  1783,   -66,     0,  -287,  1435,  -426,
  -227,     0,    15,   -27,     0,     0,     0,   -26,     0,  -271,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,    19,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,
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
"class","module","def","undef",
"begin","rescue","ensure","end",
"if","unless","then","elsif",
"else","case","when","while",
"until","for","break","next",
"redo","retry","in","do",
"do (for condition)","do (for block)","return","yield",
"super","self","nil","true",
"false","and","or","not",
"if (modifier)","unless (modifier)","while (modifier)","until (modifier)",
"rescue (modifier)","alias","defined","BEGIN",
"END","__LINE__","__FILE__",
"__ENCODING__","do (for lambda)","tIDENTIFIER","tFID",
    "tGVAR","tIVAR","tCONSTANT","tCVAR","tLABEL","tCHAR","unary+",
"unary-","tUMINUS_NUM","**","<=>","==","===","!=",">=",
"<=","&&","||","=~","!~","'.'","..","...",
"[]","[]=","<<",">>","&.","::",":: at EXPR_BEG",
    "tOP_ASGN","=>","'('","'('","')'","( arg",
"'['","']'","'{'","{ arg","'*'","'*'","'&'",
"'&'","'`'","'%'","'/'","'+'","'-'","'<'","'>'",
"'|'","'!'","'^'","'{'","'}'","'`'","tSYMBEG",
    "tSTRING_BEG","tXSTRING_BEG","tREGEXP_BEG","tWORDS_BEG","tQWORDS_BEG",
    "tSTRING_DBEG","tSTRING_DVAR","tSTRING_END","->","tLAMBEG",
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
    "arg : arg tDOT2",
    "arg : arg tDOT3",
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
    "primary : keyword_class tLSHFT expr $$18 term bodystmt keyword_end",
    "$$19 :",
    "primary : keyword_module cpath $$19 bodystmt keyword_end",
    "$$20 :",
    "primary : keyword_def fname $$20 f_arglist bodystmt keyword_end",
    "$$21 :",
    "$$22 :",
    "primary : keyword_def singleton dot_or_colon $$21 fname $$22 f_arglist bodystmt keyword_end",
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
    "$$23 :",
    "$$24 :",
    "lambda : $$23 f_larglist $$24 lambda_body",
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
    "$$25 :",
    "brace_block : keyword_do $$25 opt_block_param compstmt keyword_end",
    "$$26 :",
    "brace_body : $$26 opt_block_param compstmt",
    "$$27 :",
    "do_body : $$27 opt_block_param bodystmt",
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
    "$$28 :",
    "string_content : tSTRING_DVAR $$28 string_dvar",
    "$$29 :",
    "$$30 :",
    "$$31 :",
    "$$32 :",
    "$$33 :",
    "string_content : tSTRING_DBEG $$29 $$30 $$31 $$32 $$33 compstmt tSTRING_DEND",
    "string_dvar : tGVAR",
    "string_dvar : tIVAR",
    "string_dvar : tCVAR",
    "string_dvar : backref",
    "symbol : tSYMBEG sym",
    "sym : fname",
    "sym : tIVAR",
    "sym : tGVAR",
    "sym : tCVAR",
    "dsym : tSYMBEG string_contents tSTRING_END",
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
    "$$34 :",
    "superclass : tLT $$34 expr_value term",
    "superclass :",
    "f_arglist : tLPAREN2 f_args rparen",
    "$$35 :",
    "f_arglist : $$35 f_args term",
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
    "$$36 :",
    "singleton : tLPAREN2 $$36 expr rparen",
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

static RipperParserState[] states = new RipperParserState[651];
static {
states[1] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                  p.setState(EXPR_BEG);
                  p.pushLocalScope();
  return yyVal;
};
states[2] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                  yyVal = p.dispatch("on_program", ((IRubyObject)yyVals[0+yyTop]));
                  p.popCurrentScope();
  return yyVal;
};
states[3] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                  yyVal = ((IRubyObject)yyVals[-1+yyTop]);
  return yyVal;
};
states[4] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                  yyVal = p.dispatch("on_stmts_add", p.dispatch("on_stmts_new"), p.dispatch("on_void_stmt"));
  return yyVal;
};
states[5] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                  yyVal = p.dispatch("on_stmts_add", p.dispatch("on_stmts_new"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[6] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                  yyVal = p.dispatch("on_stmts_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[7] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                  yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[9] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                  if (p.isInDef()) {
                      p.yyerror("BEGIN in method");
                  }
  return yyVal;
};
states[10] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                  yyVal = p.dispatch("on_BEGIN", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[11] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                  yyVal = p.dispatch("on_bodystmt", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[12] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
  return yyVal;
};
states[13] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_stmts_add", p.dispatch("on_stmts_new"), p.dispatch("on_void_stmt"));
  return yyVal;
};
states[14] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_stmts_add", p.dispatch("on_stmts_new"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[15] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_stmts_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[16] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[17] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[18] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.yyerror("BEGIN is permitted only at toplevel");
  return yyVal;
};
states[19] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_BEGIN", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[20] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.setState(EXPR_FNAME|EXPR_FITEM);
  return yyVal;
};
states[21] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_alias", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[22] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_alias", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[23] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_alias", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[24] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_alias_error", p.dispatch("on_var_alias", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop])));
                    p.error();
  return yyVal;
};
states[25] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_undef", ((RubyArray)yyVals[0+yyTop]));
  return yyVal;
};
states[26] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_if_mod", ((IRubyObject)yyVals[0+yyTop]), ((IRubyObject)yyVals[-2+yyTop]));
  return yyVal;
};
states[27] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_unless_mod", ((IRubyObject)yyVals[0+yyTop]), ((IRubyObject)yyVals[-2+yyTop]));
  return yyVal;
};
states[28] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_while_mod", ((IRubyObject)yyVals[0+yyTop]), ((IRubyObject)yyVals[-2+yyTop]));
  return yyVal;
};
states[29] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_until_mod", ((IRubyObject)yyVals[0+yyTop]), ((IRubyObject)yyVals[-2+yyTop]));
  return yyVal;
};
states[30] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_rescue_mod", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[31] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    if (p.isInDef()) {
                        p.warn("END in method; use at_exit");
                    }
                    yyVal = p.dispatch("on_END", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[33] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_massign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[34] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[35] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_massign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[37] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[38] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_opassign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[39] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_aref_field", ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop])),
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[40] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_field", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop])), 
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[41] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_field",((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[42] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_const_path_field", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop])), 
                                    ((IRubyObject)yyVals[-1+yyTop]),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[43] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_field", ((IRubyObject)yyVals[-4+yyTop]), p.intern("::"), ((IRubyObject)yyVals[-2+yyTop])), 
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[44] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assign_error", 
                                    p.dispatch("on_assign", 
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[-2+yyTop])), 
                                    ((IRubyObject)yyVals[0+yyTop])));
                    p.error();
  return yyVal;
};
states[45] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[46] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_rescue_mod", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[49] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("and"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[50] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("or"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[51] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_unary", p.intern("not"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[52] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_unary", p.intern("!"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[54] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[58] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_method_add_arg", 
                                    p.dispatch("on_call", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[59] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
  return yyVal;
};
states[61] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_command", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[62] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_method_add_block",
                                    p.dispatch("on_command", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[63] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_command_call", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[64] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_method_add_block",
                                    p.dispatch("on_command_call", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop])); 
  return yyVal;
};
states[65] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_command_call", ((IRubyObject)yyVals[-3+yyTop]), p.intern("::"), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[66] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_method_add_block",
                                    p.dispatch("on_command_call", ((IRubyObject)yyVals[-4+yyTop]), p.intern("::"), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[67] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_super", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[68] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_yield", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[69] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_return", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[70] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_break", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[71] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_next", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[73] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_paren", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[75] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_paren", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[76] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[77] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_add", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[78] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[79] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_add_post",
                                    p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[80] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-1+yyTop]), null);
  return yyVal;
};
states[81] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_add_post",
                                    p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-3+yyTop]), null),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[82] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_add_star", p.dispatch("on_mlhs_new"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[83] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_add_post",
                                    p.dispatch("on_mlhs_add_star", p.dispatch("on_mlhs_new"), ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[84] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_add_star", p.dispatch("on_mlhs_new"), null);
  return yyVal;
};
states[85] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_add_post",
                                    p.dispatch("on_mlhs_add_star", p.dispatch("on_mlhs_new"), null),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[87] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_paren", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[88] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_add", p.dispatch("on_mlhs_new"), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[89] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[90] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_add", p.dispatch("on_mlhs_new"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[91] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[92] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.assignableIdentifier(p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[93] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[94] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[95] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.assignableConstant(p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[96] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[97] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[98] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[99] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[100] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[101] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[102] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[103] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.yyerror("Can't assign to __ENCODING__");
  return yyVal;
};
states[104] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_aref_field", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[105] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_field", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
                    
  return yyVal;
};
states[106] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_const_path_field", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[107] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
		    yyVal = p.dispatch("on_field", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[108] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_const_path_field", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));

                    if (p.isInDef()) {
                        yyVal = p.dispatch("on_assign_error", ((IRubyObject)yyVal));
                        p.error();
                    }
  return yyVal;
};
states[109] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_top_const_field", ((IRubyObject)yyVals[0+yyTop]));

                    if (p.isInDef()) {
                        yyVal = p.dispatch("on_assign_error", ((IRubyObject)yyVal));
                        p.error();
                    }
  return yyVal;
};
states[110] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assign_error", p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
                    p.error();
  return yyVal;
};
states[111] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_field", p.assignableIdentifier(((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[112] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[113] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[114] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.assignableConstant(p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[115] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[116] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[117] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[118] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[119] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[120] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[121] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[122] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[123] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_aref_field", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[124] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_field", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[125] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_field", ((IRubyObject)yyVals[-2+yyTop]), p.intern("::"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[126] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_field", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[127] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    IRubyObject val = p.dispatch("on_const_path_field", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));

                    if (p.isInDef()) {
                        val = p.dispatch("on_assign_error", val);
                        p.error();
                    }

                    yyVal = val;
  return yyVal;
};
states[128] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    IRubyObject val = p.dispatch("on_top_const_field", ((IRubyObject)yyVals[0+yyTop]));

                    if (p.isInDef()) {
                        val = p.dispatch("on_assign_error", val);
                        p.error();
                    }

                    yyVal = val;
  return yyVal;
};
states[129] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
                    p.error();
  return yyVal;
};
states[130] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_class_name_error", ((IRubyObject)yyVals[0+yyTop]));
                    p.error();
  return yyVal;
};
states[132] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_top_const_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[133] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_const_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[134] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_const_path_ref", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[138] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                   p.setState(EXPR_ENDFN);
                   yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[139] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                   p.setState(EXPR_ENDFN);
                   yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[140] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[141] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[142] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                   yyVal = p.dispatch("on_symbol_literal", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[143] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[144] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[145] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.setState(EXPR_FNAME|EXPR_FITEM);
  return yyVal;
};
states[146] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((RubyArray)yyVals[-3+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[222] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[223] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_opassign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[224] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_aref_field", ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop])),
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[225] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_field", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[226] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_field", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[227] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_field", ((IRubyObject)yyVals[-4+yyTop]), p.intern("::"), ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[228] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assign_error", 
                                    p.dispatch("on_opassign", 
                                               p.dispatch("on_const_path_field", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop])),
                                               ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[229] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assign_error", 
                                    p.dispatch("on_opassign", 
                                               p.dispatch("on_top_const_field", ((IRubyObject)yyVals[-2+yyTop])),
                                               ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[230] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assign_error", 
                                    p.dispatch("on_opassign",
                                               p.dispatch("on_var_field", ((IRubyObject)yyVals[-2+yyTop])),
                                               ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop])));
                    p.error();
  return yyVal;
};
states[231] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_dot2", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[232] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_dot3", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[233] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_dot2", ((IRubyObject)yyVals[-1+yyTop]), p.new_nil_at());
  return yyVal;
};
states[234] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_dot3", ((IRubyObject)yyVals[-1+yyTop]), p.new_nil_at());
  return yyVal;
};
states[235] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("+"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[236] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("-"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[237] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("*"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[238] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("/"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[239] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("%"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[240] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("**"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[241] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_unary", 
                                    p.intern("-@"), 
                                    p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("**"), ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[242] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_unary", p.intern("+@"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[243] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_unary", p.intern("-@"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[244] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("|"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[245] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("^"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[246] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("&"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[247] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("<=>"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[248] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[249] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("=="), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[250] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("==="), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[251] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("!="), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[252] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("=~"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[253] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("!~"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[254] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_unary", p.intern("!"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[255] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_unary", p.intern("~"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[256] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("<<"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[257] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern(">>"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[258] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("&&"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[259] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("||"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[260] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_defined", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[261] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_ifop", ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[262] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[263] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.intern(">");
  return yyVal;
};
states[264] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.intern("<");
  return yyVal;
};
states[265] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.intern(">=");
  return yyVal;
};
states[266] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.intern("<=");
  return yyVal;
};
states[267] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                     yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));

  return yyVal;
};
states[268] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                     p.warning("comparison '" + ((IRubyObject)yyVals[-1+yyTop]) + "' after comparison");
                     yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[269] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[271] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
  return yyVal;
};
states[272] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_args_add", 
                                    ((IRubyObject)yyVals[-3+yyTop]),
                                    p.dispatch("on_bare_assoc_hash", ((RubyArray)yyVals[-1+yyTop])));
  return yyVal;
};
states[273] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_args_add", 
                                    p.dispatch("on_args_new"),
                                    p.dispatch("on_bare_assoc_hash", ((RubyArray)yyVals[-1+yyTop])));
  return yyVal;
};
states[274] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[275] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_rescue_mod", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[276] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_arg_paren", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[281] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
  return yyVal;
};
states[282] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_args_add", ((IRubyObject)yyVals[-3+yyTop]), p.dispatch("on_bare_assoc_hash", ((RubyArray)yyVals[-1+yyTop])));
  return yyVal;
};
states[283] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_args_add", 
                                    p.dispatch("on_args_new"),
                                    p.dispatch("on_bare_assoc_hash", ((RubyArray)yyVals[-1+yyTop])));
  return yyVal;
};
states[284] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_args_add", p.dispatch("on_args_new"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[285] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.arg_add_optblock(((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[286] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal =  p.arg_add_optblock(p.dispatch("on_args_add", 
                                                        p.dispatch("on_args_new"),
                                                        p.dispatch("on_bare_assoc_hash", ((RubyArray)yyVals[-1+yyTop]))),
                                             ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[287] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.arg_add_optblock(p.dispatch("on_args_add", 
                                            ((IRubyObject)yyVals[-3+yyTop]),
                                            p.dispatch("on_bare_assoc_hash", ((RubyArray)yyVals[-1+yyTop]))),
                                            ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[288] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_args_add_block", p.dispatch("on_args_new"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[289] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = Long.valueOf(p.getCmdArgumentState().getStack());
                    p.getCmdArgumentState().begin();
  return yyVal;
};
states[290] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[291] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[292] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[294] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_args_add", p.dispatch("on_args_new"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[295] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_args_add_star", p.dispatch("on_args_new"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[296] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_args_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[297] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_args_add_star", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[298] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[299] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[300] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mrhs_add", 
                                    p.dispatch("on_mrhs_new_from_args", ((IRubyObject)yyVals[-2+yyTop])), 
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[301] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mrhs_add_star",
                                    p.dispatch("on_mrhs_new_from_args", ((IRubyObject)yyVals[-3+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[302] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mrhs_add_star", p.dispatch("on_mrhs_new"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[309] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop]); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[310] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop]); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[313] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_method_add_arg", p.dispatch("on_fcall", ((IRubyObject)yyVals[0+yyTop])), p.dispatch("on_args_new"));
  return yyVal;
};
states[314] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.getCmdArgumentState().getStack();
                    p.getCmdArgumentState().reset();
  return yyVal;
};
states[315] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.getCmdArgumentState().reset(((Long)yyVals[-2+yyTop]).longValue());
                    yyVal = p.dispatch("on_begin", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[316] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.setState(EXPR_ENDARG);
  return yyVal;
};
states[317] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_paren", null);
  return yyVal;
};
states[318] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.getCmdArgumentState().getStack();
                    p.getCmdArgumentState().reset();
  return yyVal;
};
states[319] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.setState(EXPR_ENDARG); 
  return yyVal;
};
states[320] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.getCmdArgumentState().reset(((Long)yyVals[-3+yyTop]).longValue());
                    p.warning("(...) interpreted as grouped expression");
                    yyVal = p.dispatch("on_paren", ((IRubyObject)yyVals[-2+yyTop]));
  return yyVal;
};
states[321] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_paren", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[322] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_const_path_ref", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[323] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_top_const_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[324] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_array", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[325] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_hash", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[326] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_return0");
  return yyVal;
};
states[327] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_yield", p.dispatch("on_paren", ((IRubyObject)yyVals[-1+yyTop])));
  return yyVal;
};
states[328] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_yield", p.dispatch("on_paren", p.dispatch("on_args_new")));
  return yyVal;
};
states[329] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_yield0");
  return yyVal;
};
states[330] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_defined", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[331] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_unary", p.intern("not"), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[332] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_unary", p.intern("not"), null);
  return yyVal;
};
states[333] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_method_add_block",
                                    p.dispatch("on_method_add_arg", 
                                               p.dispatch("on_fcall", ((IRubyObject)yyVals[-1+yyTop])), 
                                               p.dispatch("on_args_new")), 
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[335] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_method_add_block", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[336] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[337] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_if", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[338] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_unless", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[339] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.getConditionState().begin();
  return yyVal;
};
states[340] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.getConditionState().end();
  return yyVal;
};
states[341] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_while", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[342] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                  p.getConditionState().begin();
  return yyVal;
};
states[343] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                  p.getConditionState().end();
  return yyVal;
};
states[344] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_until", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[345] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_case", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[346] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_case", null, ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[347] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.getConditionState().begin();
  return yyVal;
};
states[348] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.getConditionState().end();
  return yyVal;
};
states[349] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_for", ((IRubyObject)yyVals[-7+yyTop]), ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[350] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    if (p.isInDef()) {
                        p.yyerror("class definition in method body");
                    }
                    p.pushLocalScope();
                    yyVal = p.isInClass(); /* MRI reuses $1 but we use the value for position.*/
                    p.setIsInClass(true);
  return yyVal;
};
states[351] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_class", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
                    p.popCurrentScope();
                    p.setIsInClass(((Boolean)yyVals[-2+yyTop]).booleanValue());
  return yyVal;
};
states[352] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = new Integer((p.isInClass() ? 0b10 : 0) |
                                     (p.isInDef()   ? 0b01 : 0));
                    p.setInDef(false);
                    p.setIsInClass(false);
                    p.pushLocalScope();
  return yyVal;
};
states[353] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_sclass", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));

                    p.popCurrentScope();
                    p.setInDef(((((Integer)yyVals[-3+yyTop]).intValue())     & 0b01) != 0);
                    p.setIsInClass(((((Integer)yyVals[-3+yyTop]).intValue()) & 0b10) != 0);
  return yyVal;
};
states[354] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    if (p.isInDef()) { 
                        p.yyerror("module definition in method body");
                    }
                    yyVal = p.isInClass();
                    p.setIsInClass(true);
                    p.pushLocalScope();
  return yyVal;
};
states[355] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_module", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
                    p.popCurrentScope();
  return yyVal;
};
states[356] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.setInDef(true);
                    p.pushLocalScope();
                    yyVal = p.getCurrentArg();
                    p.setCurrentArg(null);
  return yyVal;
};
states[357] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_def", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));

                    p.popCurrentScope();
                    p.setInDef(false);
                    p.setCurrentArg(((IRubyObject)yyVals[-3+yyTop]));
  return yyVal;
};
states[358] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.setState(EXPR_FNAME);
                    yyVal = p.isInDef();
                    p.setInDef(true);
  return yyVal;
};
states[359] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.pushLocalScope();
                    p.setState(EXPR_ENDFN|EXPR_LABEL); /* force for args */
                    yyVal = p.getCurrentArg();
                    p.setCurrentArg(null);                    
  return yyVal;
};
states[360] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_defs", ((IRubyObject)yyVals[-7+yyTop]), ((IRubyObject)yyVals[-6+yyTop]), ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));

                    p.popCurrentScope();
                    p.setInDef(((Boolean)yyVals[-5+yyTop]).booleanValue());
                    p.setCurrentArg(((IRubyObject)yyVals[-3+yyTop]));
  return yyVal;
};
states[361] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_break", p.dispatch("on_args_new"));
  return yyVal;
};
states[362] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_next", p.dispatch("on_args_new"));
  return yyVal;
};
states[363] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_redo");
  return yyVal;
};
states[364] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_retry");
  return yyVal;
};
states[365] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[366] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = null;
  return yyVal;
};
states[368] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[369] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = null;
  return yyVal;
};
states[372] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_elsif", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[374] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_else", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[376] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
  return yyVal;
};
states[377] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[378] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_paren", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[379] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_add", p.dispatch("on_mlhs_new"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[380] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[381] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[382] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[383] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_add_post", 
                                    p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[384] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-2+yyTop]), null);
  return yyVal;
};
states[385] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_add_post", 
                                    p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-4+yyTop]), null),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[386] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_add_star",
                                    p.dispatch("on_mlhs_new"),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[387] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_add_post", 
                                    p.dispatch("on_mlhs_add_star",
                                               p.dispatch("on_mlhs_new"),
                                               ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[388] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_add_star",
                                    p.dispatch("on_mlhs_new"),
                                    null);
  return yyVal;
};
states[389] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_add_post", 
                                    p.dispatch("on_mlhs_add_star",
                                               p.dispatch("on_mlhs_new"),
                                               null),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[390] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args_tail(((RubyArray)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[391] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args_tail(((RubyArray)yyVals[-1+yyTop]), null, ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[392] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args_tail(null, ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[393] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args_tail(null, null, ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[394] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop]);
  return yyVal;
};
states[395] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args_tail(null, null, null);
  return yyVal;
};
states[396] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-5+yyTop]), ((RubyArray)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[397] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-7+yyTop]), ((RubyArray)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[398] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[399] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-5+yyTop]), ((RubyArray)yyVals[-3+yyTop]), null, ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[400] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-3+yyTop]), null, ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[401] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_excessed_comma", 
                                    p.new_args(((RubyArray)yyVals[-1+yyTop]), null, null, null, null));
  return yyVal;
};
states[402] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-5+yyTop]), null, ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[403] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-1+yyTop]), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[404] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[405] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[406] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[407] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-3+yyTop]), null, ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[408] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(null, null, ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));     
  return yyVal;
};
states[409] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(null, null, ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[410] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[412] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.setCommandStart(true);
  return yyVal;
};
states[413] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.setCurrentArg(null);  
                    yyVal = p.dispatch("on_block_var", 
                                    p.new_args(null, null, null, null, null), 
                                    ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[414] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_block_var", 
                                    p.new_args(null, null, null, null, null), 
                                    null);
  return yyVal;
};
states[415] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.setCurrentArg(null);
                    yyVal = p.dispatch("on_block_var", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[416] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.getContext().getRuntime().getFalse();
  return yyVal;
};
states[417] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((RubyArray)yyVals[-1+yyTop]);
  return yyVal;
};
states[418] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[419] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((RubyArray)yyVals[-2+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[420] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_bv(((IRubyObject)yyVals[0+yyTop]));

  return yyVal;
};
states[421] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = null;
  return yyVal;
};
states[422] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.pushBlockScope();
                    yyVal = p.getLeftParenBegin();
                    p.setLeftParenBegin(p.incrementParenNest());
  return yyVal;
};
states[423] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = Long.valueOf(p.getCmdArgumentState().getStack());
                    p.getCmdArgumentState().reset();
  return yyVal;
};
states[424] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                    p.getCmdArgumentState().restart();
                    yyVal = p.dispatch("on_lambda", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
                    p.setLeftParenBegin(((Integer)yyVals[-3+yyTop]));
                    p.popCurrentScope();
  return yyVal;
};
states[425] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_paren", ((IRubyObject)yyVals[-2+yyTop]));
  return yyVal;
};
states[426] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[427] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
  return yyVal;
};
states[428] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
  return yyVal;
};
states[429] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);  
  return yyVal;
};
states[430] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_method_add_block", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[431] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.method_optarg(p.dispatch("on_call", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[432] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.method_add_block(p.dispatch("on_command_call", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[433] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.method_add_block(p.dispatch("on_command_call", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[434] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_method_add_arg", p.dispatch("on_fcall", ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[435] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.method_optarg(p.dispatch("on_call", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[436] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.method_optarg(p.dispatch("on_call", ((IRubyObject)yyVals[-3+yyTop]), p.intern("::"), ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[437] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_call", ((IRubyObject)yyVals[-2+yyTop]), p.intern("::"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[438] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.method_optarg(p.dispatch("on_call", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), p.intern("call")), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[439] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.method_optarg(p.dispatch("on_call", ((IRubyObject)yyVals[-2+yyTop]), p.intern("::"), p.intern("call")), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[440] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_super", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[441] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_zsuper");
  return yyVal;
};
states[442] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_aref", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[443] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
  return yyVal;
};
states[444] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.pushBlockScope();
                    yyVal = Long.valueOf(p.getCmdArgumentState().getStack());
                    p.getCmdArgumentState().reset();
  return yyVal;
};
states[445] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_do_block", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
                    p.getCmdArgumentState().reset(((Long)yyVals[-3+yyTop]).longValue());
                    p.popCurrentScope();
  return yyVal;
};
states[446] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.pushBlockScope();
                    yyVal = Long.valueOf(p.getCmdArgumentState().getStack()) >> 1;
                    p.getCmdArgumentState().reset();
  return yyVal;
};
states[447] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_brace_block", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
                    p.getCmdArgumentState().reset(((Long)yyVals[-2+yyTop]).longValue());
                    p.popCurrentScope();
  return yyVal;
};
states[448] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.pushBlockScope();
                    yyVal = Long.valueOf(p.getCmdArgumentState().getStack());
                    p.getCmdArgumentState().reset();
  return yyVal;
};
states[449] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_do_block", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
                    p.getCmdArgumentState().reset(((Long)yyVals[-2+yyTop]).longValue());
                    p.popCurrentScope();
  return yyVal;
};
states[450] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_when", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));

  return yyVal;
};
states[453] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_rescue", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[454] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = null;
  return yyVal;
};
states[455] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[456] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[458] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[460] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_ensure", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[463] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_symbol_literal", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[465] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[466] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[467] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[468] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_string_concat", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[469] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.heredoc_dedent(((IRubyObject)yyVals[-1+yyTop]));
                    p.setHeredocIndent(0);
                    yyVal = p.dispatch("on_string_literal", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[470] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.heredoc_dedent(((IRubyObject)yyVals[-1+yyTop]));
                    p.setHeredocIndent(0);
                    yyVal = p.dispatch("on_xstring_literal", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[471] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_regexp_literal", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[472] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_array", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[473] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_words_new");
  return yyVal;
};
states[474] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_words_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[475] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_word_add", p.dispatch("on_word_new"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[476] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_word_add", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[477] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_array", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[478] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_symbols_new");
  return yyVal;
};
states[479] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_symbols_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[480] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_array", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[481] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_array", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[482] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_qwords_new");
  return yyVal;
};
states[483] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_qwords_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[484] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_qsymbols_new");
  return yyVal;
};
states[485] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_qsymbols_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[486] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_string_content");
  return yyVal;
};
states[487] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_string_add", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[488] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_xstring_new");
  return yyVal;
};
states[489] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_xstring_add", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[490] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_regexp_new");
  return yyVal;
};
states[491] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_regexp_add", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[493] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.getStrTerm();
                    p.setStrTerm(null);
                    p.setState(EXPR_BEG);
  return yyVal;
};
states[494] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.setStrTerm(((StrTerm)yyVals[-1+yyTop]));
                    yyVal = p.dispatch("on_string_dvar", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[495] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                   yyVal = p.getStrTerm();
                   p.setStrTerm(null);
                   p.getConditionState().stop();
  return yyVal;
};
states[496] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                   yyVal = p.getCmdArgumentState().getStack();
                   p.getCmdArgumentState().reset();
  return yyVal;
};
states[497] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                   yyVal = p.getState();
                   p.setState(EXPR_BEG);
  return yyVal;
};
states[498] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                   yyVal = p.getBraceNest();
                   p.setBraceNest(0);
  return yyVal;
};
states[499] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                   yyVal = p.getHeredocIndent();
                   p.setHeredocIndent(0);
  return yyVal;
};
states[500] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                   p.getConditionState().restart();
                   p.setStrTerm(((StrTerm)yyVals[-6+yyTop]));
                   p.getCmdArgumentState().reset(((Long)yyVals[-5+yyTop]).longValue());
                   p.setState(((Integer)yyVals[-4+yyTop]));
                   p.setBraceNest(((Integer)yyVals[-3+yyTop]));
                   p.setHeredocIndent(((Integer)yyVals[-2+yyTop]));
                   yyVal = p.dispatch("on_string_embexpr", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[501] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                   yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[502] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                   yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[503] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                   yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[505] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                     p.setState(EXPR_END|EXPR_ENDARG);
                     yyVal = p.dispatch("on_symbol", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[510] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                     p.setState(EXPR_END|EXPR_ENDARG);
                     yyVal = p.dispatch("on_dyna_symbol", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[511] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[512] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_unary", p.intern("-@"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[513] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[514] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[515] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[516] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[517] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    if (p.is_id_var()) {
                        yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
                    } else {
                        yyVal = p.dispatch("on_vcall", ((IRubyObject)yyVals[0+yyTop]));
                    }
  return yyVal;
};
states[518] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[519] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[520] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[521] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[522] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[523] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[524] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[525] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[526] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[527] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[528] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[529] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_field", p.assignableIdentifier(((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[530] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[531] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[532] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.assignableConstant(p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[533] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[534] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.yyerror("Can't assign to nil");
  return yyVal;
};
states[535] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.yyerror("Can't change the value of self");
  return yyVal;
};
states[536] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.yyerror("Can't assign to true");
  return yyVal;
};
states[537] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.yyerror("Can't assign to false");
  return yyVal;
};
states[538] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.yyerror("Can't assign to __FILE__");
  return yyVal;
};
states[539] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.yyerror("Can't assign to __LINE__");
  return yyVal;
};
states[540] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.yyerror("Can't assign to __ENCODING__");
  return yyVal;
};
states[543] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                   p.setState(EXPR_BEG);
                   p.setCommandStart(true);
  return yyVal;
};
states[544] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
  return yyVal;
};
states[545] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                   yyVal = null;
  return yyVal;
};
states[546] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.setState(EXPR_BEG);
                    p.setCommandStart(true);
                    yyVal = p.dispatch("on_paren", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[547] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
  /* $$ = lexer.inKwarg;*/
                   /*                   p.inKwarg = true;*/
                   p.setState(p.getState() | EXPR_LABEL);
  return yyVal;
};
states[548] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
  /* p.inKwarg = $<Boolean>1;*/
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
                    p.setState(EXPR_BEG);
                    p.setCommandStart(true);
  return yyVal;
};
states[549] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args_tail(((RubyArray)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[550] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args_tail(((RubyArray)yyVals[-1+yyTop]), null, ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[551] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args_tail(null, ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[552] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args_tail(null, null, ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[553] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop]);
  return yyVal;
};
states[554] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args_tail(null, null, null);
  return yyVal;
};
states[555] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-5+yyTop]), ((RubyArray)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[556] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-7+yyTop]), ((RubyArray)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[557] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[558] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-5+yyTop]), ((RubyArray)yyVals[-3+yyTop]), null, ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[559] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-3+yyTop]), null, ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[560] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-5+yyTop]), null, ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[561] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-1+yyTop]), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[562] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[563] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[564] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[565] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-3+yyTop]), null, ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[566] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(null, null, ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[567] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(null, null, ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[568] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[569] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_args(null, null, null, null, null);
  return yyVal;
};
states[570] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_param_error", ((IRubyObject)yyVals[0+yyTop]));
                    p.error();
  return yyVal;
};
states[571] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_param_error", ((IRubyObject)yyVals[0+yyTop]));
                    p.error();
  return yyVal;
};
states[572] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_param_error", ((IRubyObject)yyVals[0+yyTop]));
                    p.error();
  return yyVal;
};
states[573] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_param_error", ((IRubyObject)yyVals[0+yyTop]));
                    p.error();
  return yyVal;
};
states[575] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.arg_var(p.formal_argument(((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[576] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.setCurrentArg(((IRubyObject)yyVals[0+yyTop]));
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[577] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.setCurrentArg(null);
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[578] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_mlhs_paren", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[579] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[580] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((RubyArray)yyVals[-2+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[581] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.arg_var(p.formal_argument(((IRubyObject)yyVals[0+yyTop])));
                    p.setCurrentArg(((IRubyObject)yyVals[0+yyTop]));
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[582] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.setCurrentArg(null);
                    yyVal = p.keyword_arg(((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[583] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.setCurrentArg(null);
                    yyVal = p.keyword_arg(((IRubyObject)yyVals[0+yyTop]), p.getContext().getRuntime().getFalse());
  return yyVal;
};
states[584] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.keyword_arg(((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[585] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.keyword_arg(((IRubyObject)yyVals[0+yyTop]), p.getContext().getRuntime().getFalse());
  return yyVal;
};
states[586] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[587] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((RubyArray)yyVals[-2+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[588] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[589] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((RubyArray)yyVals[-2+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[590] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[591] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[592] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.shadowing_lvar(((IRubyObject)yyVals[0+yyTop]));
                    yyVal = p.dispatch("on_kwrest_param", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[593] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_kwrest_param", null);
  return yyVal;
};
states[594] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.setCurrentArg(null);
                    yyVal = p.new_assoc(((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));

  return yyVal;
};
states[595] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.setCurrentArg(null);
                    yyVal = p.new_assoc(((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[596] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[597] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((RubyArray)yyVals[-2+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[598] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[599] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((RubyArray)yyVals[-2+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[602] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.arg_var(p.shadowing_lvar(((IRubyObject)yyVals[0+yyTop])));
                    yyVal = p.dispatch("on_rest_param", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[603] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_rest_param", null);
  return yyVal;
};
states[606] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.arg_var(p.shadowing_lvar(((IRubyObject)yyVals[0+yyTop])));
                    yyVal = p.dispatch("on_blockarg", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[607] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[608] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = null;
  return yyVal;
};
states[609] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[610] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    p.setState(EXPR_BEG);
  return yyVal;
};
states[611] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_paren", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[613] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assoclist_from_args", ((RubyArray)yyVals[-1+yyTop]));
  return yyVal;
};
states[614] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[615] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((RubyArray)yyVals[-2+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[616] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assoc_new", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[617] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assoc_new", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[618] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assoc_new", p.dispatch("on_dyna_symbol", ((IRubyObject)yyVals[-2+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[619] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.dispatch("on_assoc_splat", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[630] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[631] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[632] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.intern(".");
  return yyVal;
};
states[633] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = p.intern("&.");
  return yyVal;
};
states[634] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[635] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                   yyVal = p.intern("::");
  return yyVal;
};
states[640] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[641] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[649] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                      yyVal = null;
  return yyVal;
};
states[650] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop) -> {
                  yyVal = null;
  return yyVal;
};
}
					// line 2250 "RipperParser.y"
}
					// line 9824 "-"
