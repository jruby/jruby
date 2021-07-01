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
import org.jruby.lexer.yacc.StackState;
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
					// line 54 "-"
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
//yyLhs 653
    -1,   152,     0,   140,   141,   141,   141,   141,   142,   155,
   142,    37,    36,    38,    38,    38,    38,    44,   156,    44,
   157,    39,    39,    39,    39,    39,    39,    39,    39,    39,
    39,    39,    39,    39,    39,    39,    39,    31,    31,    31,
    31,    31,    31,    31,    31,    62,    62,    62,    40,    40,
    40,    40,    40,    40,    45,   159,   160,    46,    32,    32,
    61,    61,   114,   149,    43,    43,    43,    43,    43,    43,
    43,    43,    43,    43,    43,   117,   117,   128,   128,   118,
   118,   118,   118,   118,   118,   118,   118,   118,   118,    75,
    75,   104,   104,   105,   105,    76,    76,    76,    76,    76,
    76,    76,    76,    76,    76,    76,    76,    76,    76,    76,
    76,    76,    76,    76,    81,    81,    81,    81,    81,    81,
    81,    81,    81,    81,    81,    81,    81,    81,    81,    81,
    81,    81,    81,     8,     8,    30,    30,    30,     7,     7,
     7,     7,     7,   121,   121,   122,   122,    65,   161,    65,
     6,     6,     6,     6,     6,     6,     6,     6,     6,     6,
     6,     6,     6,     6,     6,     6,     6,     6,     6,     6,
     6,     6,     6,     6,     6,     6,     6,     6,     6,   135,
   135,   135,   135,   135,   135,   135,   135,   135,   135,   135,
   135,   135,   135,   135,   135,   135,   135,   135,   135,   135,
   135,   135,   135,   135,   135,   135,   135,   135,   135,   135,
   135,   135,   135,   135,   135,   135,   135,   135,   135,   135,
   135,   135,   135,   135,   135,    41,    41,    41,    41,    41,
    41,    41,    41,    41,    41,    41,    41,    41,    41,    41,
    41,    41,    41,    41,    41,    41,    41,    41,    41,    41,
    41,    41,    41,    41,    41,    41,    41,    41,    41,    41,
    41,    41,    41,    41,    41,    41,   137,   137,   137,   137,
    52,    52,    77,    80,    80,    80,    80,    63,    63,    55,
    59,    59,   131,   131,   131,   131,   131,    53,    53,    53,
    53,    53,   163,    57,   108,   107,   107,    83,    83,    83,
    83,    35,    35,    74,    74,    74,    42,    42,    42,    42,
    42,    42,    42,    42,    42,    42,    42,   164,    42,   165,
    42,   166,   167,    42,    42,    42,    42,    42,    42,    42,
    42,    42,    42,    42,    42,    42,    42,    42,    42,    42,
    42,    42,   168,   169,    42,   170,   171,    42,    42,    42,
   172,   173,    42,   174,    42,   176,    42,   177,    42,   178,
    42,   179,   180,    42,    42,    42,    42,    42,    47,   151,
   151,   151,   150,   150,    50,    50,    48,    48,   130,   130,
   132,   132,    88,    88,   133,   133,   133,   133,   133,   133,
   133,   133,   133,    95,    95,    95,    95,    94,    94,    70,
    70,    70,    70,    70,    70,    70,    70,    70,    70,    70,
    70,    70,    70,    70,    72,    72,    71,    71,    71,   125,
   125,   124,   124,   134,   134,   181,   182,   127,    69,    69,
   126,   126,   113,    60,    60,    60,    60,    22,    22,    22,
    22,    22,    22,    22,    22,    22,   112,   183,   112,   184,
   115,   185,   116,    78,    49,    49,   119,   119,    79,    79,
    79,    51,    51,    54,    54,    28,    28,    28,    15,    16,
    16,    16,    17,    18,    19,    25,    85,    85,    27,    27,
    91,    89,    89,    26,    92,    84,    84,    90,    90,    20,
    20,    21,    21,    24,    24,    23,   186,    23,   187,   188,
   189,   190,    23,    66,    66,    66,    66,     2,     1,     1,
     1,     1,    29,    33,    33,    34,    34,    34,    34,    58,
    58,    58,    58,    58,    58,    58,    58,    58,    58,    58,
    58,   120,   120,   120,   120,   120,   120,   120,   120,   120,
   120,   120,   120,    67,    67,   191,    56,    56,    73,   192,
    73,    96,    96,    96,    96,    93,    93,    68,    68,    68,
    68,    68,    68,    68,    68,    68,    68,    68,    68,    68,
    68,    68,   136,   136,   136,   136,     9,     9,   148,   123,
   123,    86,    86,   145,    97,    97,    98,    98,    99,    99,
   100,   100,   143,   143,   144,   144,    64,   129,   106,   106,
    87,    87,    10,    10,    13,    13,    12,    12,   111,   110,
   110,    14,   193,    14,   101,   101,   102,   102,   103,   103,
   103,   103,     3,     3,     3,     4,     4,     4,     4,     5,
     5,     5,    11,    11,   146,   146,   147,   147,   153,   153,
   158,   158,   138,   139,   162,   162,   162,   175,   175,   154,
   154,    82,   109,
    }, yyLen = {
//yyLen 653
     2,     0,     2,     2,     1,     1,     3,     2,     1,     0,
     5,     4,     2,     1,     1,     3,     2,     1,     0,     5,
     0,     4,     3,     3,     3,     2,     3,     3,     3,     3,
     3,     4,     1,     3,     3,     3,     1,     3,     3,     6,
     5,     5,     5,     5,     3,     1,     3,     1,     1,     3,
     3,     3,     2,     1,     1,     0,     0,     4,     1,     1,
     1,     4,     3,     1,     2,     3,     4,     5,     4,     5,
     2,     2,     2,     2,     2,     1,     3,     1,     3,     1,
     2,     3,     5,     2,     4,     2,     4,     1,     3,     1,
     3,     2,     3,     1,     3,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     4,     3,     3,
     3,     3,     2,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     4,     3,     3,     3,
     3,     2,     1,     1,     1,     2,     1,     3,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     0,     4,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     3,     3,     6,     5,     5,
     5,     5,     4,     3,     3,     3,     2,     2,     3,     3,
     3,     3,     3,     3,     4,     2,     2,     3,     3,     3,
     3,     1,     3,     3,     3,     3,     3,     2,     2,     3,
     3,     3,     3,     3,     6,     1,     1,     1,     1,     1,
     3,     3,     1,     1,     2,     4,     2,     1,     3,     3,
     1,     1,     1,     1,     2,     4,     2,     1,     2,     2,
     4,     1,     0,     2,     2,     2,     1,     1,     2,     3,
     4,     1,     1,     3,     4,     2,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     0,     4,     0,
     3,     0,     0,     5,     3,     3,     2,     3,     3,     1,
     4,     3,     1,     5,     4,     3,     2,     1,     2,     2,
     6,     6,     0,     0,     6,     0,     0,     6,     5,     4,
     0,     0,     8,     0,     6,     0,     7,     0,     5,     0,
     6,     0,     0,     9,     1,     1,     1,     1,     1,     1,
     1,     2,     1,     1,     1,     5,     1,     2,     1,     1,
     1,     3,     1,     3,     1,     4,     6,     3,     5,     2,
     4,     1,     3,     4,     2,     2,     1,     2,     0,     6,
     8,     4,     6,     4,     2,     6,     2,     4,     6,     2,
     4,     2,     4,     1,     1,     1,     3,     1,     4,     1,
     4,     1,     3,     1,     1,     0,     0,     4,     4,     1,
     3,     3,     3,     2,     4,     5,     5,     2,     4,     4,
     3,     3,     3,     2,     1,     4,     3,     0,     5,     0,
     3,     0,     3,     5,     1,     1,     6,     0,     1,     1,
     1,     2,     1,     2,     1,     1,     1,     1,     1,     1,
     1,     2,     3,     3,     3,     4,     0,     3,     1,     2,
     4,     0,     3,     4,     4,     0,     3,     0,     3,     0,
     2,     0,     2,     0,     2,     1,     0,     3,     0,     0,
     0,     0,     7,     1,     1,     1,     1,     2,     1,     1,
     1,     1,     3,     1,     2,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     0,     4,     0,     3,     0,
     3,     4,     2,     2,     1,     2,     0,     6,     8,     4,
     6,     4,     6,     2,     4,     6,     2,     4,     2,     4,
     1,     0,     1,     1,     1,     1,     1,     1,     1,     1,
     3,     1,     3,     1,     2,     1,     2,     1,     1,     3,
     1,     3,     1,     1,     2,     1,     3,     3,     1,     3,
     1,     3,     1,     1,     2,     1,     1,     1,     2,     2,
     0,     1,     0,     4,     1,     2,     1,     3,     3,     2,
     4,     2,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     0,     1,
     0,     1,     2,     2,     0,     1,     1,     1,     1,     1,
     2,     0,     0,
    }, yyDefRed = {
//yyDefRed 1107
     1,     0,     0,     0,     0,     0,     0,     0,   317,     0,
     0,     0,   342,   345,     0,     0,     0,   366,   367,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     9,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   469,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   489,   491,   493,     0,     0,   425,   543,
   544,   515,   518,   516,   517,     0,     0,   466,    63,   307,
     0,   470,   308,   309,     0,   310,   311,   306,   467,    32,
    48,   465,   513,     0,     0,     0,     0,     0,     0,     0,
   314,     0,    59,     0,     0,    89,     0,     4,   312,   313,
     0,     0,    75,     0,     2,     0,     5,     0,     7,   364,
   365,   329,     0,     0,   525,   524,   526,   527,     0,     0,
   529,   528,   530,     0,   521,   520,     0,   523,     0,     0,
     0,     0,   136,     0,   368,     0,   315,     0,   357,   189,
   200,   190,   213,   186,   206,   196,   195,   216,   217,   211,
   194,   193,   188,   214,   218,   219,   198,   187,   201,   205,
   207,   199,   192,   208,   215,   210,     0,     0,     0,     0,
   185,   204,   203,   220,   221,   222,   223,   224,   184,   191,
   182,   183,     0,     0,     0,     0,   140,     0,   174,   175,
   171,   153,   154,   155,   162,   159,   161,   156,   157,   176,
   177,   163,   164,   612,   168,   167,   152,   173,   170,   169,
   165,   166,   160,   158,   150,   172,   151,   178,   141,   359,
     0,   611,   142,   209,   202,   212,   197,   179,   180,   181,
   138,   139,   144,   143,   146,     0,   145,   147,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   647,
   648,     0,     0,     0,   649,    55,    55,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   378,   379,     0,     0,     0,     0,     0,   489,
     0,     0,   287,    73,     0,     0,     0,   616,   291,    74,
    72,     0,    71,     0,     0,   443,    70,     0,   641,     0,
     0,    20,     0,     0,     0,   245,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,    14,    13,     0,     0,
     0,     0,     0,   273,     0,     0,     0,   614,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   258,    52,   257,
   510,   509,   511,   507,   508,     0,     0,     0,     0,   476,
   485,   339,     0,   481,   487,   471,   447,   449,   338,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   268,   269,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   267,   266,     0,
     0,     0,     0,   451,   433,   634,   635,     0,     0,     0,
     0,   637,   636,     0,     0,    91,     0,     0,     0,     0,
     0,     0,     3,     0,   437,     0,   336,     0,   514,     0,
   133,     0,   135,   545,   353,     0,     0,     0,     0,     0,
     0,   632,   633,   361,   148,     0,     0,     0,   370,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   650,   343,     0,   346,     0,     0,     0,     0,   350,
   619,   298,   294,     0,   621,     0,     0,   288,   296,     0,
   289,     0,   331,     0,   293,   283,   282,     0,     0,     0,
     0,   335,    51,    22,    24,    23,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   324,    12,
     0,     0,   320,     0,   327,     0,   645,   274,     0,   276,
   328,   615,     0,    93,     0,     0,     0,     0,     0,   498,
   496,   512,   495,   490,   472,   473,   492,   474,   494,     0,
     0,   577,   574,   573,   572,   575,   583,   592,     0,     0,
   603,   602,   607,   606,   593,   578,     0,     0,     0,   600,
   429,   426,     0,     0,   570,   590,     0,   554,   581,   576,
     0,     0,     0,     0,     0,     0,     0,     0,     0,    26,
    27,    28,    29,    30,    49,    50,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   440,     0,   442,     0,     0,
   627,     0,     0,   628,   441,     0,   625,   626,     0,    47,
     0,     0,     0,    44,   233,     0,     0,     0,     0,    37,
   225,    34,   297,     0,     0,     0,     0,    92,    33,    35,
   301,     0,    38,   226,     6,   449,    65,     0,     0,     0,
     0,     0,     0,   137,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   318,     0,   371,     0,     0,
     0,     0,     0,     0,     0,     0,   349,     0,     0,     0,
     0,     0,     0,     0,     0,    55,     0,   618,     0,     0,
     0,   295,   617,   330,   642,     0,     0,   279,   334,    21,
     0,     0,    31,     0,   232,     0,     0,    15,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   499,     0,   475,
   478,     0,   483,     0,     0,     0,   380,     0,   382,     0,
     0,   604,   608,     0,   568,     0,     0,   563,     0,   566,
     0,   552,   594,     0,   553,   584,     0,   480,     0,   484,
     0,   417,     0,   415,     0,   414,   446,     0,     0,   432,
     0,     0,     0,   439,     0,     0,     0,     0,     0,   281,
     0,   438,   280,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,    90,     0,     0,     0,     0,     0,     0,
   445,   358,   613,     0,     0,     0,   362,   149,   459,     0,
     0,   460,     0,     0,   376,     0,   374,     0,     0,     0,
     0,     0,     0,     0,   348,     0,     0,     0,   373,    56,
   372,     0,     0,   351,   620,   300,   290,     0,   333,    10,
     0,   323,   275,    94,     0,   500,   503,   504,   505,   497,
   506,   477,   479,   486,     0,     0,     0,     0,   580,     0,
     0,     0,   555,   579,     0,     0,   427,     0,     0,   582,
     0,   601,     0,   591,   609,     0,   596,   482,   488,     0,
     0,     0,   413,   588,     0,     0,   396,     0,   598,     0,
     0,     0,     0,   450,     0,   452,    43,   230,    42,   231,
    69,     0,   643,    40,   228,    41,   229,    67,   436,   435,
    46,     0,     0,     0,     0,     0,     0,     0,     0,     0,
    62,     0,   546,   354,   548,   360,   550,     0,     0,     0,
   462,   377,     0,    11,   464,     0,   340,     0,   341,   299,
     0,   344,    57,   347,     0,     0,    19,   501,   381,     0,
     0,     0,   383,   428,     0,     0,   569,     0,     0,     0,
   561,     0,   559,     0,   564,   567,   551,     0,   411,     0,
     0,   406,     0,   394,     0,   409,   416,   395,     0,     0,
     0,     0,     0,   448,     0,     0,    39,     0,     0,   356,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   461,     0,   463,     0,
   454,   453,   455,     0,     0,     0,     0,     0,     0,   423,
     0,   421,   424,   431,   430,     0,     0,     0,     0,     0,
   397,   418,     0,     0,   589,     0,     0,     0,   599,   326,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   352,
     0,     0,     0,     0,   420,   562,     0,   557,   560,   565,
     0,   412,     0,   403,     0,   401,   393,     0,   407,   410,
     0,     0,   363,     0,     0,     0,     0,     0,   456,   375,
   502,     0,   422,     0,     0,     0,     0,     0,     0,   558,
   405,     0,   399,   402,   408,     0,   400,
    }, yyDgoto = {
//yyDgoto 194
     1,   363,    67,    68,   672,   625,   626,   233,   132,   565,
   566,   453,   567,   568,   220,    69,    70,    71,    72,    73,
   365,   367,    74,   543,   368,    75,    76,   741,    77,    78,
   133,    79,    80,    81,    82,   659,   455,   456,   324,   325,
    84,    85,    86,    87,   326,   253,   472,   316,   826,  1021,
   827,   939,    89,   495,   943,   627,   444,   302,    90,   791,
    91,    92,   649,   650,   569,   235,   859,   255,   570,   571,
   890,   773,   774,   679,   651,    94,    95,   294,   470,   820,
   332,   256,   327,   497,   550,   549,   572,   573,   747,   584,
   585,    98,    99,   754,   978,  1040,   872,   575,   893,   894,
   576,   338,   498,   297,   100,   534,   895,   487,   298,   488,
   761,   577,   436,   414,   666,   587,   619,   101,   102,   684,
   257,   236,   237,   578,  1030,   869,   876,   371,   329,   898,
   284,   499,   748,   749,  1031,   222,   579,   412,   492,   785,
   104,   105,   106,   580,   581,   582,   447,   423,   873,   137,
   839,   459,     2,   262,   263,   313,   516,   506,   493,   473,
   952,   682,   527,   303,   238,   330,   331,   728,   265,   697,
   266,   699,   705,   954,   669,   460,   667,   448,   450,   681,
   937,   372,   755,   586,   588,   620,   738,   737,   855,   957,
  1024,   668,   680,   449,
    }, yySindex = {
//yySindex 1107
     0,     0, 19475, 20772, 22449, 22836, 23578, 23469,     0, 21933,
 21933, 18691,     0,     0, 22578, 19862, 19862,     0,     0, 19862,
  -137,  -115,     0,     0,     0,     0,    60, 23360,   231,     0,
   -82,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 22062, 22062,   725,    -9, 19604,     0, 20252, 20642, 18959,
 22062, 22191, 23686,     0,     0,     0,   273,   289,     0,     0,
     0,     0,     0,     0,     0,   295,   298,     0,     0,     0,
   -34,     0,     0,     0,  -164,     0,     0,     0,     0,     0,
     0,     0,     0,  1206,    17,  5680,     0,    54,   696,   480,
     0,   314,     0,     3,   301,     0,   278,     0,     0,     0,
 22707,   305,     0,    12,     0,   158,     0,   -89,     0,     0,
     0,     0,  -137,  -115,     0,     0,     0,     0,    18,   231,
     0,     0,     0,     0,     0,     0,     0,     0,   725, 21933,
  -120, 19733,     0,     5,     0,   767,     0,   -89,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
    50,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   325,     0,     0, 19733,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,    62,    17,   142,   799,    41,   328,   159,   142,     0,
     0,   158,   141,   503,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   264,
   823,     0,     0,     0,   119, 22062, 22062, 22062, 22062,     0,
 22062,  5680,     0,     0,   262,   566,   585,     0,     0,     0,
     0, 16902,     0, 19862, 19862,     0,     0, 18820,     0, 21933,
   -84,     0, 21030,   271, 19733,     0,   931,   319,   321,   306,
 20901,     0, 19604,   309,   158,  1206,     0,     0,     0,   231,
   231, 20901,   313,     0,   166,   172,   262,     0,   297,   172,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   355, 22965,   947,     0,   624,     0,     0,     0,
     0,     0,     0,     0,     0,   830,   868,   908,   568,     0,
     0,     0,  2450,     0,     0,     0,     0,     0,     0, 21933,
 21933, 21933, 21933, 20901, 21933, 21933, 22062, 22062, 22062, 22062,
 22062,     0,     0, 22062, 22062, 22062, 22062, 22062, 22062, 22062,
 22062, 22062, 22062, 22062, 22062, 22062, 22062,     0,     0, 22062,
 22062, 22062, 22062,     0,     0,     0,     0,  6016, 19862,  6152,
 22062,     0,     0,  5162, 22191,     0, 21159, 19604, 19088,   634,
 21159, 22191,     0, 19217,     0,   352,     0,   361,     0,    17,
     0,     0,     0,     0,     0,  6528, 19862,  7040, 19733, 21933,
   374,     0,     0,     0,     0,   454,   464,   306,     0, 19733,
   462, 14088, 19862, 23850, 22062, 22062, 22062, 19733,   141, 21288,
   469,     0,     0, 21933,     0,     0, 23906, 19862, 23962,     0,
     0,     0,     0,   478,     0, 22062, 19992,     0,     0, 20382,
     0,   231,     0,   392,     0,     0,     0,   698,   705,   231,
   161,     0,     0,     0,     0,     0, 23469, 21933,  5680, 19475,
   388, 14088, 23850, 22062, 22062,  1206,   390,   231,     0,     0,
 19346,     0,     0,  1206,     0, 20512,     0,     0, 20642,     0,
     0,     0,     0,     0,   718, 24018, 19862, 24074, 22965,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   985,
  -150,     0,     0,     0,     0,     0,     0,     0,  1134,  2497,
     0,     0,     0,     0,     0,     0,   461,   466,   732,     0,
     0,     0,   733,   736,     0,     0,   739,     0,     0,     0,
   483,   740, 22062,   729,   998,  -117,  -114,   433,  -114,     0,
     0,     0,     0,     0,     0,     0,   319,  2555,  2555,  2555,
  2555,  3606,  3094,  2555,  2555,  4127,  4127,  2467,  2467,   319,
  2477,   319,   319,   -51,   -51,  2100,  2100,  9188,  1394,   533,
  -114,   472,     0,   474,  -115,     0,     0,     0,   231,   476,
     0,   482,  -115,     0,     0,  1394,     0,     0,  -115,     0,
   524,  4667,  1053,     0,     0,     3,   771, 22062,  4667,     0,
     0,     0,     0,   790,   231, 22965,   798,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   158, 21933, 19733,
     0,     0,  -115,     0,   231,  -115,   582,   161,  2497, 19733,
  2497, 23794, 23469, 21417,   579,     0,   339,     0,   523,   525,
   231,   531,   540,   579,   589,   133,     0, 19733,   199, 19733,
     0,     0,   231,     0,     0,     0, 22062,     0, 22062,   262,
   585,     0,     0,     0,     0, 19992, 20382,     0,     0,     0,
   161,   499,     0,   319,     0, 19475,     0,     0,   231,   172,
 22965,     0,     0,   231,     0,     0,   718,     0,   949,     0,
     0,    36,     0,   839,  1134,   742,     0,   844,     0,   231,
   231,     0,     0,  2628,     0,  -146,  2497,     0,  2497,     0,
  -104,     0,     0,   324,     0,     0, 22062,     0,   180,     0,
   857,     0,  1545,     0, 19733,     0,     0, 19733,   833,     0,
 19733, 22191, 22191,     0,   352,   558,   554, 22191, 22191,     0,
   352,     0,     0,    54,  -164, 20901, 22062, 24130, 19862, 24186,
 22191,     0, 21546,     0,   718, 22965,   539, 19733,   158,   643,
     0,     0,     0,   231,   657,   158,     0,     0,     0,     0,
   577,     0, 19733,   670,     0, 21933,     0,   673, 22062, 22062,
   604, 22062, 22062,   684,     0, 21675, 19733,   686,     0,     0,
     0,   690,     0,     0,     0,     0,     0,   911,     0,     0,
   596,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   231,  1158,   916,  1888,     0,   625,
   910,   927,     0,     0, 19733, 19733,     0,   932,   934,     0,
   941,     0,   927,     0,     0,   740,     0,     0,     0,   948,
   231,   950,     0,     0,   958,   961,     0,   647,     0,   740,
 23094,   946,   752,     0, 22062,     0,     0,     0,     0,     0,
     0, 22191,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  5680,   472,   474,   231,   476,   482, 22062,     0,   718,
     0,   754,     0,     0,     0,     0,     0,   374, 23223,   142,
     0,     0, 19733,     0,     0,   142,     0, 22062,     0,     0,
    14,     0,     0,     0, 19733, 20382,     0,     0,     0,   975,
  1158,   808,     0,     0,   995,  2628,     0,   757,   676,  2628,
     0,  2497,     0,  2628,     0,     0,     0,  2628,     0,   674,
  2497,     0,  -104,     0,  2497,     0,     0,     0,     0,     0,
   726,  1096, 23094,     0,  5680,  5680,     0,   558,     0,     0,
 19733,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   734,  1098,     0,     0, 19733,     0, 19733,
     0,     0,     0,   781, 19733,  1158,   975,  1158,  1010,     0,
   266,     0,     0,     0,     0,   927,  1011,   927,   927,  1012,
     0,     0,  1014,  1017,     0,   740,  1019,  1012,     0,     0,
 24242,  1096,   806,     0, 24298, 19862, 24354,   454,   339,     0,
   677,   975,  1158,   995,     0,     0,  2628,     0,     0,     0,
  2628,     0,  2628,     0,  2497,     0,     0,  2628,     0,     0,
     0,     0,     0,     0,     0,   231,     0,     0,     0,     0,
     0,   975,     0,   927,  1012,  1029,  1012,  1012,     0,     0,
     0,  2628,     0,     0,     0,  1012,     0,
    }, yyRindex = {
//yyRindex 1107
     0,     0,   173,     0,     0,     0,     0,     0,     0,     0,
     0,   805,     0,     0,     0,  1724, 10716,     0,     0, 10819,
  4911,  4399, 12309, 12421, 12549, 12661, 22320,     0, 21804,     0,
     0, 12771, 12901, 13011,  5273,  3375, 13123, 13232,  5412, 13371,
     0,     0,     0,     0,     0,   125, 18562,   751,   714,   243,
     0,     0,  1049,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  9981,     0,     0,     0, 10170,     0,     0,     0,     0,     0,
     0,     0,     0,   105, 16770,  9678, 10287, 10530,     0, 13538,
     0, 12118,     0, 13672,     0,     0,     0,     0,     0,     0,
   274,     0,     0,     0,     0,    47,     0, 20122,     0,     0,
     0,     0, 10410,  8037,     0,     0,     0,     0,     0,   738,
     0,     0,     0, 17043,     0,     0, 17182,     0,     0,     0,
     0,   125,     0, 18022,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,  2965,  3052,  3477,  3564,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  3989,  4076,  4501,  4588,     0,  5514,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0, 16788,     0,     0,   720,  8160,
  8277,  8466,  8583,  8706,  8889,  9012,  2212,  9129,  9318,  2351,
  9435,     0, 16716,     0,     0,  9741,     0,     0,     0,     0,
     0,   805,     0,   810,     0,     0,     0,  1303,  1338,  1619,
  1623,  2129,  2638,  3051,   993,  3150,  3523,  2053,  3563,     0,
     0,  3662,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 16299,     0,     0, 16388,  1920,  1920,     0,     0,     0,
     0,   743,     0,     0,    69,     0,     0,   743,     0,     0,
     0,     0,     0,     0,    63,     0,     0, 11259, 10593, 13778,
     0, 17883,   125,     0,  2486,   746,     0,     0,   127,   743,
   743,     0,     0,     0,   779,   779,     0,     0,     0,   724,
  1506,  7324,  8377,  8648,  8799,  9651, 10081,  1639, 10352, 10503,
  2085, 11230,     0,     0,     0, 11267,   299,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  -135,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0, 10931, 11125,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   100,     0,
     0,     0,     0,     0,     0,     0,     0,   125,   515,   545,
     0,     0,     0,    58,     0,  1425,     0,     0,     0,   177,
     0, 17603,     0,     0,     0,     0,   100,     0,   720,     0,
  1145,     0,     0,     0,     0,   383,     0,  9864,     0,   722,
 17742,     0,   100,     0,     0,     0,     0,   613,     0,     0,
     0,     0,     0,     0,     0,  4035,     0,   100,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   743,     0,     0,     0,     0,     0,    -2,    -2,   743,
   743,     0,     0,     0,     0,     0,     0,     0,  5103,    63,
     0,     0,     0,     0,     0,   905,     0,   743,     0,     0,
  2501,    95,     0,   143,     0,   785,     0,     0,  -160,     0,
     0,     0, 11291,     0,   576,     0,   100,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   134,
     0,     0,     0,     0,     0,     0,    43,     0,   238,     0,
     0,     0,   238,   238,     0,     0,   396,     0,     0,     0,
   228,   396,   279,   538,     0,     0, 18293,     0, 18432,     0,
     0,     0,     0,     0,     0,     0, 11368, 15185, 15272, 15363,
 15460, 15546, 15903, 15655, 15794,  2030, 15989, 14295, 14413, 11485,
 14568, 11641, 11758, 13886, 14022, 14686, 14789,  1033, 14912,     0,
 18163,  5785,  3748,  7321, 20122,     0,  3887,     0,   786,  5924,
     0,  6297,  4772,     0,     0, 15029,     0,     0,  7683,     0,
  1309,  1542,     0,     0,     0, 14192,     0,     0,  1864,     0,
     0,     0,     0,     0,   743,     0,   602,     0,     0,     0,
     0, 16752,     0,     0,     0,     0,     0,     0,     0,   720,
 17323, 17462,     0,     0,   786,  9558,     0,   743,   168,   720,
   184,     0,     0,   265,   428,     0,   878,     0,  2724,  4260,
   786,  2863,  3236,   878,     0,     0,     0,   192,     0,   192,
  2499,   652,   786,  2539,  3011,     0,     0,     0,     0, 16424,
  1920,     0,     0,     0,     0,    80,    89,     0,     0,     0,
   743,     0,     0, 11867,     0,    63,    94,     0,   743,   779,
     0,  2104,  2095,   786,  5602,  6114,   633,     0,     0,     0,
     0,     0,     0,     0,     0,   171,     0,   187,     0,   743,
   104,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,    -5,     0,   192,     0,     0,    63,     0,     0,
   720,     0,     0,     0, 16510, 12003,     0,     0,     0,     0,
 16546,     0,     0, 16630,  8826,     0,     0,     0,   100,     0,
     0, 13475,     0,     0,   683,     0,     0,   720,     0,     0,
     0,     0,     0,   743,     0,     0,     0,     0,     0,   760,
   260,     0,   749,   878,     0,     0,     0,     0,     0,     0,
  7822,     0,     0,     0,     0,     0,   699,     0,     0,     0,
     0,     0,  1056,     0,     0,     0,     0,    -2,     0,     0,
     0,     0,     0,     0,    88,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   743,     0,   212,     0,     0,     0,
   -65,   238,     0,     0,   192,    63,     0,   238,   238,     0,
   238,     0,   238,     0,     0,   396,     0,     0,     0,   148,
    -5,   148,     0,     0,   155,   148,     0,     0,     0,   155,
    74,   102,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 16086,  6436,  7460,   786,  6809,  6948,     0, 16404,   687,
     0,     0,     0,     0,     0,     0,     0,  1145,     0,     0,
     0,     0,   192,     0,     0,     0,     0,     0,     0,     0,
   878,     0,     0,     0,   192,   164,     0,     0,     0,   223,
     0,   224,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   156,     0,     0,     0,     0,     0,     0,     0,   565,  1283,
     0,   113,     0,     0, 16148, 16237,     0, 12173, 16680,     0,
   720,  2592,  3104,  3616,  4128,  4640,  6677,  7189,  1183,  7924,
  8202,  1649,  8253,     0,     0,  8319,     0,   720,     0,   722,
     0,     0,     0,     0,    68,     0,   247,     0,   253,     0,
   -25,     0,     0,     0,     0,   238,   238,   238,   238,   148,
     0,     0,   148,   148,     0,   155,   148,   148,     0,     0,
     0,   116,     0,  8342,     0,   100,     0,   383,   878,     0,
     0,   254,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  1466,   850,     0,  1912,  1031,   786,  6626,  7138,     0,     0,
     0,   258,     0,   238,   148,   148,   148,   148,  7922,     0,
     0,     0,     0,     0,     0,   148,     0,
    }, yyGindex = {
//yyGindex 194
     0,     0,    38,     0,  -316,     0,    30,    22,   -98,  -130,
     0,     0,     0,   336,     0,     0,     0,  1073,     0,     0,
    27,     0,     0,  -238,     0,     0,     0,   561,     0,    44,
  1141,  -189,   -27,     0,   146,     0,   246,  -419,     0,    32,
   110,   825,    34,    25,   628,     1,  -223,    11,  -593,     0,
    92,     0,     0,   889,     0,    93,     0,    21,  1147,   516,
     0,     0,  -283,   351,  -683,     0,     0,  1161,  -453,     0,
     0,     0,  -334,   225,  -384,   -91,   -44,   671,  -449,     0,
     0,  1330,     2,    70,     0,     0,   707,   405,  -159,     0,
     0,     0,     0,  -354,  1243,   398,   248,   411,   190,     0,
     0,     0,    48,  -434,     0,  -450,   197,  -278,  -420,     0,
  -529,   435,   -73,   391,  -525,   529,     0,  1167,   -22,   140,
  1444,     0,   -20,  -643,     0,  -691,     0,     0,  -172,  -830,
     0,  -385,  -758,   473,   153,     0,  -784,  1116,    46,  -566,
  -412,     0,     7,     0,   930,    -3,   -85,     0,  -101,   616,
     0,  -241,     0,   489,   -46,     0,     0,     0,   -26,     0,
     0,     0,  -263,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,    20,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,
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
    "$$5 :",
    "$$6 :",
    "expr_value_do : $$5 expr_value do $$6",
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
    "$$7 :",
    "undef_list : undef_list ',' $$7 fitem",
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
    "$$8 :",
    "command_args : $$8 call_args",
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
    "$$9 :",
    "primary : keyword_begin $$9 bodystmt keyword_end",
    "$$10 :",
    "primary : tLPAREN_ARG $$10 rparen",
    "$$11 :",
    "$$12 :",
    "primary : tLPAREN_ARG $$11 stmt $$12 rparen",
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
    "$$13 :",
    "$$14 :",
    "primary : keyword_while $$13 expr_value_do $$14 compstmt keyword_end",
    "$$15 :",
    "$$16 :",
    "primary : keyword_until $$15 expr_value_do $$16 compstmt keyword_end",
    "primary : keyword_case expr_value opt_terms case_body keyword_end",
    "primary : keyword_case opt_terms case_body keyword_end",
    "$$17 :",
    "$$18 :",
    "primary : keyword_for for_var keyword_in $$17 expr_value_do $$18 compstmt keyword_end",
    "$$19 :",
    "primary : keyword_class cpath superclass $$19 bodystmt keyword_end",
    "$$20 :",
    "primary : keyword_class tLSHFT expr $$20 term bodystmt keyword_end",
    "$$21 :",
    "primary : keyword_module cpath $$21 bodystmt keyword_end",
    "$$22 :",
    "primary : keyword_def fname $$22 f_arglist bodystmt keyword_end",
    "$$23 :",
    "$$24 :",
    "primary : keyword_def singleton dot_or_colon $$23 fname $$24 f_arglist bodystmt keyword_end",
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
    "$$25 :",
    "$$26 :",
    "lambda : $$25 f_larglist $$26 lambda_body",
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
    "$$27 :",
    "brace_block : keyword_do $$27 opt_block_param compstmt keyword_end",
    "$$28 :",
    "brace_body : $$28 opt_block_param compstmt",
    "$$29 :",
    "do_body : $$29 opt_block_param bodystmt",
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
    "$$30 :",
    "string_content : tSTRING_DVAR $$30 string_dvar",
    "$$31 :",
    "$$32 :",
    "$$33 :",
    "$$34 :",
    "string_content : tSTRING_DBEG $$31 $$32 $$33 $$34 compstmt tSTRING_DEND",
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
            yyVal = state.execute(this, yyVal, yyVals, yyTop, yyToken);
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

static RipperParserState[] states = new RipperParserState[653];
static {
states[1] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                  p.setState(EXPR_BEG);
                  p.pushLocalScope();
  return yyVal;
};
states[2] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                  yyVal = p.dispatch("on_program", ((IRubyObject)yyVals[0+yyTop]));
                  p.popCurrentScope();
  return yyVal;
};
states[3] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                  yyVal = ((IRubyObject)yyVals[-1+yyTop]);
  return yyVal;
};
states[4] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                  yyVal = p.dispatch("on_stmts_add", p.dispatch("on_stmts_new"), p.dispatch("on_void_stmt"));
  return yyVal;
};
states[5] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                  yyVal = p.dispatch("on_stmts_add", p.dispatch("on_stmts_new"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[6] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                  yyVal = p.dispatch("on_stmts_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[7] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                  yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[9] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                  if (p.isInDef()) {
                      p.yyerror("BEGIN in method");
                  }
  return yyVal;
};
states[10] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                  yyVal = p.dispatch("on_BEGIN", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[11] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                  yyVal = p.dispatch("on_bodystmt", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[12] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
  return yyVal;
};
states[13] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_stmts_add", p.dispatch("on_stmts_new"), p.dispatch("on_void_stmt"));
  return yyVal;
};
states[14] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_stmts_add", p.dispatch("on_stmts_new"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[15] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_stmts_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[16] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[17] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[18] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.yyerror("BEGIN is permitted only at toplevel");
  return yyVal;
};
states[19] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_BEGIN", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[20] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.setState(EXPR_FNAME|EXPR_FITEM);
  return yyVal;
};
states[21] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_alias", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[22] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_alias", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[23] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_alias", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[24] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_alias_error", p.dispatch("on_var_alias", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop])));
                    p.error();
  return yyVal;
};
states[25] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_undef", ((RubyArray)yyVals[0+yyTop]));
  return yyVal;
};
states[26] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_if_mod", ((IRubyObject)yyVals[0+yyTop]), ((IRubyObject)yyVals[-2+yyTop]));
  return yyVal;
};
states[27] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_unless_mod", ((IRubyObject)yyVals[0+yyTop]), ((IRubyObject)yyVals[-2+yyTop]));
  return yyVal;
};
states[28] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_while_mod", ((IRubyObject)yyVals[0+yyTop]), ((IRubyObject)yyVals[-2+yyTop]));
  return yyVal;
};
states[29] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_until_mod", ((IRubyObject)yyVals[0+yyTop]), ((IRubyObject)yyVals[-2+yyTop]));
  return yyVal;
};
states[30] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_rescue_mod", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[31] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    if (p.isInDef()) {
                        p.warn("END in method; use at_exit");
                    }
                    yyVal = p.dispatch("on_END", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[33] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_massign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[34] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[35] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_massign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[37] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[38] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_opassign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[39] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_aref_field", ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop])),
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[40] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_field", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop])), 
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[41] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_field",((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[42] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_const_path_field", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop])), 
                                    ((IRubyObject)yyVals[-1+yyTop]),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[43] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_field", ((IRubyObject)yyVals[-4+yyTop]), p.intern("::"), ((IRubyObject)yyVals[-2+yyTop])), 
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[44] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assign_error", 
                                    p.dispatch("on_assign", 
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[-2+yyTop])), 
                                    ((IRubyObject)yyVals[0+yyTop])));
                    p.error();
  return yyVal;
};
states[45] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[46] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_rescue_mod", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[49] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("and"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[50] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("or"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[51] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_unary", p.intern("not"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[52] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_unary", p.intern("!"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[54] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[55] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.getConditionState().push1();
  return yyVal;
};
states[56] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.getConditionState().pop();
  return yyVal;
};
states[57] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-2+yyTop]);
  return yyVal;
};
states[61] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_method_add_arg", 
                                    p.dispatch("on_call", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[62] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
  return yyVal;
};
states[64] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_command", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[65] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_method_add_block",
                                    p.dispatch("on_command", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[66] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_command_call", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[67] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_method_add_block",
                                    p.dispatch("on_command_call", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop])); 
  return yyVal;
};
states[68] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_command_call", ((IRubyObject)yyVals[-3+yyTop]), p.intern("::"), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[69] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_method_add_block",
                                    p.dispatch("on_command_call", ((IRubyObject)yyVals[-4+yyTop]), p.intern("::"), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[70] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_super", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[71] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_yield", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[72] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_return", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[73] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_break", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[74] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_next", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[76] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_paren", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[78] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_paren", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[79] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[80] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_add", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[81] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[82] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_add_post",
                                    p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[83] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-1+yyTop]), null);
  return yyVal;
};
states[84] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_add_post",
                                    p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-3+yyTop]), null),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[85] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_add_star", p.dispatch("on_mlhs_new"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[86] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_add_post",
                                    p.dispatch("on_mlhs_add_star", p.dispatch("on_mlhs_new"), ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[87] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_add_star", p.dispatch("on_mlhs_new"), null);
  return yyVal;
};
states[88] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_add_post",
                                    p.dispatch("on_mlhs_add_star", p.dispatch("on_mlhs_new"), null),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[90] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_paren", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[91] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_add", p.dispatch("on_mlhs_new"), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[92] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[93] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_add", p.dispatch("on_mlhs_new"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[94] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[95] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.assignableIdentifier(p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[96] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[97] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[98] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.assignableConstant(p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[99] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[100] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[101] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[102] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[103] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[104] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[105] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[106] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.yyerror("Can't assign to __ENCODING__");
  return yyVal;
};
states[107] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_aref_field", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[108] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_field", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
                    
  return yyVal;
};
states[109] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_const_path_field", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[110] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
		    yyVal = p.dispatch("on_field", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[111] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_const_path_field", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));

                    if (p.isInDef()) {
                        yyVal = p.dispatch("on_assign_error", ((IRubyObject)yyVal));
                        p.error();
                    }
  return yyVal;
};
states[112] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_top_const_field", ((IRubyObject)yyVals[0+yyTop]));

                    if (p.isInDef()) {
                        yyVal = p.dispatch("on_assign_error", ((IRubyObject)yyVal));
                        p.error();
                    }
  return yyVal;
};
states[113] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assign_error", p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
                    p.error();
  return yyVal;
};
states[114] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_field", p.assignableIdentifier(((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[115] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[116] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[117] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.assignableConstant(p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[118] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[119] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[120] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[121] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[122] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[123] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[124] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[125] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[126] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_aref_field", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[127] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_field", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[128] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_field", ((IRubyObject)yyVals[-2+yyTop]), p.intern("::"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[129] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_field", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[130] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    IRubyObject val = p.dispatch("on_const_path_field", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));

                    if (p.isInDef()) {
                        val = p.dispatch("on_assign_error", val);
                        p.error();
                    }

                    yyVal = val;
  return yyVal;
};
states[131] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    IRubyObject val = p.dispatch("on_top_const_field", ((IRubyObject)yyVals[0+yyTop]));

                    if (p.isInDef()) {
                        val = p.dispatch("on_assign_error", val);
                        p.error();
                    }

                    yyVal = val;
  return yyVal;
};
states[132] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
                    p.error();
  return yyVal;
};
states[133] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_class_name_error", ((IRubyObject)yyVals[0+yyTop]));
                    p.error();
  return yyVal;
};
states[135] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_top_const_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[136] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_const_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[137] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_const_path_ref", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[141] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                   p.setState(EXPR_ENDFN);
                   yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[142] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                   p.setState(EXPR_ENDFN);
                   yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[143] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[144] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[145] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                   yyVal = p.dispatch("on_symbol_literal", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[146] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                   yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[147] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[148] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.setState(EXPR_FNAME|EXPR_FITEM);
  return yyVal;
};
states[149] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((RubyArray)yyVals[-3+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[225] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[226] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_opassign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[227] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_aref_field", ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop])),
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[228] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_field", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[229] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_field", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[230] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_opassign", 
                                    p.dispatch("on_field", ((IRubyObject)yyVals[-4+yyTop]), p.intern("::"), ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[231] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assign_error", 
                                    p.dispatch("on_opassign", 
                                               p.dispatch("on_const_path_field", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop])),
                                               ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[232] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assign_error", 
                                    p.dispatch("on_opassign", 
                                               p.dispatch("on_top_const_field", ((IRubyObject)yyVals[-2+yyTop])),
                                               ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[233] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assign_error", 
                                    p.dispatch("on_opassign",
                                               p.dispatch("on_var_field", ((IRubyObject)yyVals[-2+yyTop])),
                                               ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop])));
                    p.error();
  return yyVal;
};
states[234] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_dot2", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[235] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_dot3", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[236] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_dot2", ((IRubyObject)yyVals[-1+yyTop]), p.new_nil_at());
  return yyVal;
};
states[237] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_dot3", ((IRubyObject)yyVals[-1+yyTop]), p.new_nil_at());
  return yyVal;
};
states[238] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("+"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[239] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("-"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[240] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("*"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[241] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("/"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[242] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("%"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[243] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("**"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[244] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_unary", 
                                    p.intern("-@"), 
                                    p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("**"), ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[245] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_unary", p.intern("+@"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[246] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_unary", p.intern("-@"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[247] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("|"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[248] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("^"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[249] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("&"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[250] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("<=>"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[251] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[252] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("=="), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[253] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("==="), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[254] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("!="), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[255] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("=~"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[256] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("!~"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[257] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_unary", p.intern("!"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[258] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_unary", p.intern("~"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[259] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("<<"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[260] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern(">>"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[261] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("&&"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[262] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), p.intern("||"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[263] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_defined", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[264] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_ifop", ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[265] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[266] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.intern(">");
  return yyVal;
};
states[267] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.intern("<");
  return yyVal;
};
states[268] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.intern(">=");
  return yyVal;
};
states[269] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.intern("<=");
  return yyVal;
};
states[270] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                     yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));

  return yyVal;
};
states[271] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                     p.warning("comparison '" + ((IRubyObject)yyVals[-1+yyTop]) + "' after comparison");
                     yyVal = p.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[272] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[274] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
  return yyVal;
};
states[275] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_args_add", 
                                    ((IRubyObject)yyVals[-3+yyTop]),
                                    p.dispatch("on_bare_assoc_hash", ((RubyArray)yyVals[-1+yyTop])));
  return yyVal;
};
states[276] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_args_add", 
                                    p.dispatch("on_args_new"),
                                    p.dispatch("on_bare_assoc_hash", ((RubyArray)yyVals[-1+yyTop])));
  return yyVal;
};
states[277] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[278] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_rescue_mod", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[279] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_arg_paren", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[284] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
  return yyVal;
};
states[285] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_args_add", ((IRubyObject)yyVals[-3+yyTop]), p.dispatch("on_bare_assoc_hash", ((RubyArray)yyVals[-1+yyTop])));
  return yyVal;
};
states[286] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_args_add", 
                                    p.dispatch("on_args_new"),
                                    p.dispatch("on_bare_assoc_hash", ((RubyArray)yyVals[-1+yyTop])));
  return yyVal;
};
states[287] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_args_add", p.dispatch("on_args_new"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[288] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.arg_add_optblock(((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[289] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal =  p.arg_add_optblock(p.dispatch("on_args_add", 
                                                        p.dispatch("on_args_new"),
                                                        p.dispatch("on_bare_assoc_hash", ((RubyArray)yyVals[-1+yyTop]))),
                                             ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[290] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.arg_add_optblock(p.dispatch("on_args_add", 
                                            ((IRubyObject)yyVals[-3+yyTop]),
                                            p.dispatch("on_bare_assoc_hash", ((RubyArray)yyVals[-1+yyTop]))),
                                            ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[291] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_args_add_block", p.dispatch("on_args_new"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[292] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    boolean lookahead = false;
                    switch (yychar) {
                    case tLPAREN2: case tLPAREN: case tLPAREN_ARG: case '[': case tLBRACK:
                       lookahead = true;
                    }
                    StackState cmdarg = p.getCmdArgumentState();
                    if (lookahead) cmdarg.pop();
                    cmdarg.push1();
                    if (lookahead) cmdarg.push0();
  return yyVal;
};
states[293] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    StackState cmdarg = p.getCmdArgumentState();

                    boolean lookahead = false;
                    switch (yychar) {
                    case tLBRACE_ARG:
                       lookahead = true;
                    }

                    if (lookahead) cmdarg.pop();
                    cmdarg.pop();
                    if (lookahead) cmdarg.push0();
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[294] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[295] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[297] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_args_add", p.dispatch("on_args_new"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[298] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_args_add_star", p.dispatch("on_args_new"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[299] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_args_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[300] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_args_add_star", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[301] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[302] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[303] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mrhs_add", 
                                    p.dispatch("on_mrhs_new_from_args", ((IRubyObject)yyVals[-2+yyTop])), 
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[304] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mrhs_add_star",
                                    p.dispatch("on_mrhs_new_from_args", ((IRubyObject)yyVals[-3+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[305] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mrhs_add_star", p.dispatch("on_mrhs_new"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[312] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop]); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[313] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop]); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[316] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_method_add_arg", p.dispatch("on_fcall", ((IRubyObject)yyVals[0+yyTop])), p.dispatch("on_args_new"));
  return yyVal;
};
states[317] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.getCmdArgumentState().push0();
  return yyVal;
};
states[318] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.getCmdArgumentState().pop();
                    yyVal = p.dispatch("on_begin", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[319] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.setState(EXPR_ENDARG);
  return yyVal;
};
states[320] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_paren", null);
  return yyVal;
};
states[321] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.getCmdArgumentState().push0();
  return yyVal;
};
states[322] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.setState(EXPR_ENDARG); 
  return yyVal;
};
states[323] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.getCmdArgumentState().pop();
                    p.warning("(...) interpreted as grouped expression");
                    yyVal = p.dispatch("on_paren", ((IRubyObject)yyVals[-2+yyTop]));
  return yyVal;
};
states[324] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_paren", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[325] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_const_path_ref", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[326] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_top_const_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[327] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_array", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[328] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_hash", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[329] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_return0");
  return yyVal;
};
states[330] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_yield", p.dispatch("on_paren", ((IRubyObject)yyVals[-1+yyTop])));
  return yyVal;
};
states[331] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_yield", p.dispatch("on_paren", p.dispatch("on_args_new")));
  return yyVal;
};
states[332] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_yield0");
  return yyVal;
};
states[333] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_defined", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[334] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_unary", p.intern("not"), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[335] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_unary", p.intern("not"), null);
  return yyVal;
};
states[336] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_method_add_block",
                                    p.dispatch("on_method_add_arg", 
                                               p.dispatch("on_fcall", ((IRubyObject)yyVals[-1+yyTop])), 
                                               p.dispatch("on_args_new")), 
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[338] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_method_add_block", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[339] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[340] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_if", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[341] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_unless", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[342] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.getConditionState().push1();
  return yyVal;
};
states[343] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.getConditionState().pop();
  return yyVal;
};
states[344] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_while", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[345] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                  p.getConditionState().push1();
  return yyVal;
};
states[346] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                  p.getConditionState().pop();
  return yyVal;
};
states[347] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_until", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[348] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_case", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[349] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_case", null, ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[350] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.getConditionState().push1();
  return yyVal;
};
states[351] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.getConditionState().pop();
  return yyVal;
};
states[352] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_for", ((IRubyObject)yyVals[-6+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[353] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    if (p.isInDef()) {
                        p.yyerror("class definition in method body");
                    }
                    p.pushLocalScope();
                    yyVal = p.isInClass(); /* MRI reuses $1 but we use the value for position.*/
                    p.setIsInClass(true);
  return yyVal;
};
states[354] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_class", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
                    p.popCurrentScope();
                    p.setIsInClass(((Boolean)yyVals[-2+yyTop]).booleanValue());
  return yyVal;
};
states[355] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = new Integer((p.isInClass() ? 0b10 : 0) |
                                     (p.isInDef()   ? 0b01 : 0));
                    p.setInDef(false);
                    p.setIsInClass(false);
                    p.pushLocalScope();
  return yyVal;
};
states[356] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_sclass", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));

                    p.popCurrentScope();
                    p.setInDef(((((Integer)yyVals[-3+yyTop]).intValue())     & 0b01) != 0);
                    p.setIsInClass(((((Integer)yyVals[-3+yyTop]).intValue()) & 0b10) != 0);
  return yyVal;
};
states[357] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    if (p.isInDef()) { 
                        p.yyerror("module definition in method body");
                    }
                    yyVal = p.isInClass();
                    p.setIsInClass(true);
                    p.pushLocalScope();
  return yyVal;
};
states[358] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_module", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
                    p.popCurrentScope();
  return yyVal;
};
states[359] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.setInDef(true);
                    p.pushLocalScope();
                    yyVal = p.getCurrentArg();
                    p.setCurrentArg(null);
  return yyVal;
};
states[360] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_def", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));

                    p.popCurrentScope();
                    p.setInDef(false);
                    p.setCurrentArg(((IRubyObject)yyVals[-3+yyTop]));
  return yyVal;
};
states[361] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.setState(EXPR_FNAME);
                    yyVal = p.isInDef();
                    p.setInDef(true);
  return yyVal;
};
states[362] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.pushLocalScope();
                    p.setState(EXPR_ENDFN|EXPR_LABEL); /* force for args */
                    yyVal = p.getCurrentArg();
                    p.setCurrentArg(null);                    
  return yyVal;
};
states[363] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_defs", ((IRubyObject)yyVals[-7+yyTop]), ((IRubyObject)yyVals[-6+yyTop]), ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));

                    p.popCurrentScope();
                    p.setInDef(((Boolean)yyVals[-5+yyTop]).booleanValue());
                    p.setCurrentArg(((IRubyObject)yyVals[-3+yyTop]));
  return yyVal;
};
states[364] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_break", p.dispatch("on_args_new"));
  return yyVal;
};
states[365] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_next", p.dispatch("on_args_new"));
  return yyVal;
};
states[366] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_redo");
  return yyVal;
};
states[367] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_retry");
  return yyVal;
};
states[368] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[369] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[371] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[372] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[375] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_elsif", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[377] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_else", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[379] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
  return yyVal;
};
states[380] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[381] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_paren", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[382] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_add", p.dispatch("on_mlhs_new"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[383] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[384] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[385] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[386] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_add_post", 
                                    p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[387] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-2+yyTop]), null);
  return yyVal;
};
states[388] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_add_post", 
                                    p.dispatch("on_mlhs_add_star", ((IRubyObject)yyVals[-4+yyTop]), null),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[389] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_add_star",
                                    p.dispatch("on_mlhs_new"),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[390] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_add_post", 
                                    p.dispatch("on_mlhs_add_star",
                                               p.dispatch("on_mlhs_new"),
                                               ((IRubyObject)yyVals[-2+yyTop])),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[391] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_add_star",
                                    p.dispatch("on_mlhs_new"),
                                    null);
  return yyVal;
};
states[392] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_add_post", 
                                    p.dispatch("on_mlhs_add_star",
                                               p.dispatch("on_mlhs_new"),
                                               null),
                                    ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[393] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args_tail(((RubyArray)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[394] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args_tail(((RubyArray)yyVals[-1+yyTop]), null, ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[395] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args_tail(null, ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[396] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args_tail(null, null, ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[397] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop]);
  return yyVal;
};
states[398] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args_tail(null, null, null);
  return yyVal;
};
states[399] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-5+yyTop]), ((RubyArray)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[400] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-7+yyTop]), ((RubyArray)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[401] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[402] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-5+yyTop]), ((RubyArray)yyVals[-3+yyTop]), null, ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[403] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-3+yyTop]), null, ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[404] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_excessed_comma", 
                                    p.new_args(((RubyArray)yyVals[-1+yyTop]), null, null, null, null));
  return yyVal;
};
states[405] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-5+yyTop]), null, ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[406] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-1+yyTop]), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[407] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[408] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[409] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[410] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-3+yyTop]), null, ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[411] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(null, null, ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));     
  return yyVal;
};
states[412] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(null, null, ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[413] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[415] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.setCommandStart(true);
  return yyVal;
};
states[416] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.setCurrentArg(null);  
                    yyVal = p.dispatch("on_block_var", 
                                    p.new_args(null, null, null, null, null), 
                                    ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[417] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_block_var", 
                                    p.new_args(null, null, null, null, null), 
                                    null);
  return yyVal;
};
states[418] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.setCurrentArg(null);
                    yyVal = p.dispatch("on_block_var", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[419] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.getContext().getRuntime().getFalse();
  return yyVal;
};
states[420] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((RubyArray)yyVals[-1+yyTop]);
  return yyVal;
};
states[421] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[422] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((RubyArray)yyVals[-2+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[423] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_bv(((IRubyObject)yyVals[0+yyTop]));

  return yyVal;
};
states[424] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[425] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.pushBlockScope();
                    yyVal = p.getLeftParenBegin();
                    p.setLeftParenBegin(p.incrementParenNest());
  return yyVal;
};
states[426] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.getCmdArgumentState().push0();
  return yyVal;
};
states[427] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.getCmdArgumentState().pop();
                    yyVal = p.dispatch("on_lambda", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
                    p.setLeftParenBegin(((Integer)yyVals[-3+yyTop]));
                    p.popCurrentScope();
  return yyVal;
};
states[428] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_paren", ((IRubyObject)yyVals[-2+yyTop]));
  return yyVal;
};
states[429] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[430] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
  return yyVal;
};
states[431] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
  return yyVal;
};
states[432] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);  
  return yyVal;
};
states[433] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_method_add_block", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[434] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.method_optarg(p.dispatch("on_call", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[435] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.method_add_block(p.dispatch("on_command_call", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[436] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.method_add_block(p.dispatch("on_command_call", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[437] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_method_add_arg", p.dispatch("on_fcall", ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[438] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.method_optarg(p.dispatch("on_call", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[439] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.method_optarg(p.dispatch("on_call", ((IRubyObject)yyVals[-3+yyTop]), p.intern("::"), ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[440] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_call", ((IRubyObject)yyVals[-2+yyTop]), p.intern("::"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[441] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.method_optarg(p.dispatch("on_call", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), p.intern("call")), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[442] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.method_optarg(p.dispatch("on_call", ((IRubyObject)yyVals[-2+yyTop]), p.intern("::"), p.intern("call")), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[443] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_super", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[444] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_zsuper");
  return yyVal;
};
states[445] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_aref", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[446] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
  return yyVal;
};
states[447] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.pushBlockScope();
  return yyVal;
};
states[448] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_do_block", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
                    p.popCurrentScope();
  return yyVal;
};
states[449] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.pushBlockScope();
  return yyVal;
};
states[450] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_brace_block", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
                    p.popCurrentScope();
  return yyVal;
};
states[451] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.pushBlockScope();
                    p.getCmdArgumentState().push0();
  return yyVal;
};
states[452] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_do_block", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
                    p.getCmdArgumentState().pop();
                    p.popCurrentScope();
  return yyVal;
};
states[453] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_when", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));

  return yyVal;
};
states[456] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_rescue", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[457] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[458] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[459] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[461] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[463] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_ensure", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[466] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_symbol_literal", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[468] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[469] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[470] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[471] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_string_concat", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[472] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.heredoc_dedent(((IRubyObject)yyVals[-1+yyTop]));
                    p.setHeredocIndent(0);
                    yyVal = p.dispatch("on_string_literal", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[473] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.heredoc_dedent(((IRubyObject)yyVals[-1+yyTop]));
                    p.setHeredocIndent(0);
                    yyVal = p.dispatch("on_xstring_literal", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[474] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_regexp_literal", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[475] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_array", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[476] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_words_new");
  return yyVal;
};
states[477] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_words_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[478] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_word_add", p.dispatch("on_word_new"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[479] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_word_add", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[480] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_array", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[481] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_symbols_new");
  return yyVal;
};
states[482] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_symbols_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[483] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_array", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[484] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_array", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[485] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_qwords_new");
  return yyVal;
};
states[486] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_qwords_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[487] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_qsymbols_new");
  return yyVal;
};
states[488] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_qsymbols_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[489] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_string_content");
  return yyVal;
};
states[490] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_string_add", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[491] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_xstring_new");
  return yyVal;
};
states[492] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_xstring_add", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[493] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_regexp_new");
  return yyVal;
};
states[494] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_regexp_add", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[496] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.getStrTerm();
                    p.setStrTerm(null);
                    p.setState(EXPR_BEG);
  return yyVal;
};
states[497] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.setStrTerm(((StrTerm)yyVals[-1+yyTop]));
                    yyVal = p.dispatch("on_string_dvar", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[498] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                   yyVal = p.getStrTerm();
                   p.setStrTerm(null);
                   p.getConditionState().push0();
                   p.getCmdArgumentState().push0();
  return yyVal;
};
states[499] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                   yyVal = p.getState();
                   p.setState(EXPR_BEG);
  return yyVal;
};
states[500] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                   yyVal = p.getBraceNest();
                   p.setBraceNest(0);
  return yyVal;
};
states[501] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                   yyVal = p.getHeredocIndent();
                   p.setHeredocIndent(0);
  return yyVal;
};
states[502] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                   p.getConditionState().pop();
                   p.getCmdArgumentState().pop();
                   p.setStrTerm(((StrTerm)yyVals[-5+yyTop]));
                   p.setState(((Integer)yyVals[-4+yyTop]));
                   p.setBraceNest(((Integer)yyVals[-3+yyTop]));
                   p.setHeredocIndent(((Integer)yyVals[-2+yyTop]));
                   yyVal = p.dispatch("on_string_embexpr", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[503] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                   yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[504] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                   yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[505] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                   yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[507] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                     p.setState(EXPR_END|EXPR_ENDARG);
                     yyVal = p.dispatch("on_symbol", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[512] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                     p.setState(EXPR_END|EXPR_ENDARG);
                     yyVal = p.dispatch("on_dyna_symbol", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[513] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[514] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_unary", p.intern("-@"), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[515] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[516] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[517] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[518] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                     yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[519] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    if (p.is_id_var()) {
                        yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
                    } else {
                        yyVal = p.dispatch("on_vcall", ((IRubyObject)yyVals[0+yyTop]));
                    }
  return yyVal;
};
states[520] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[521] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[522] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[523] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[524] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[525] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[526] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[527] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[528] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[529] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[530] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[531] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_field", p.assignableIdentifier(((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[532] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[533] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[534] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.assignableConstant(p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[535] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[536] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.yyerror("Can't assign to nil");
  return yyVal;
};
states[537] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.yyerror("Can't change the value of self");
  return yyVal;
};
states[538] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.yyerror("Can't assign to true");
  return yyVal;
};
states[539] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.yyerror("Can't assign to false");
  return yyVal;
};
states[540] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.yyerror("Can't assign to __FILE__");
  return yyVal;
};
states[541] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.yyerror("Can't assign to __LINE__");
  return yyVal;
};
states[542] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.yyerror("Can't assign to __ENCODING__");
  return yyVal;
};
states[545] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                   p.setState(EXPR_BEG);
                   p.setCommandStart(true);
  return yyVal;
};
states[546] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
  return yyVal;
};
states[547] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                   yyVal = null;
  return yyVal;
};
states[548] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.setState(EXPR_BEG);
                    p.setCommandStart(true);
                    yyVal = p.dispatch("on_paren", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[549] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
  /* $$ = lexer.inKwarg;*/
                   /*                   p.inKwarg = true;*/
                   p.setState(p.getState() | EXPR_LABEL);
  return yyVal;
};
states[550] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
  /* p.inKwarg = $<Boolean>1;*/
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
                    p.setState(EXPR_BEG);
                    p.setCommandStart(true);
  return yyVal;
};
states[551] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args_tail(((RubyArray)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[552] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args_tail(((RubyArray)yyVals[-1+yyTop]), null, ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[553] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args_tail(null, ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[554] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args_tail(null, null, ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[555] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop]);
  return yyVal;
};
states[556] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args_tail(null, null, null);
  return yyVal;
};
states[557] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-5+yyTop]), ((RubyArray)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[558] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-7+yyTop]), ((RubyArray)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[559] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[560] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-5+yyTop]), ((RubyArray)yyVals[-3+yyTop]), null, ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[561] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-3+yyTop]), null, ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[562] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-5+yyTop]), null, ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[563] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(((RubyArray)yyVals[-1+yyTop]), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[564] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[565] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[566] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[567] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(null, ((RubyArray)yyVals[-3+yyTop]), null, ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[568] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(null, null, ((IRubyObject)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[569] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(null, null, ((IRubyObject)yyVals[-3+yyTop]), ((RubyArray)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[570] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
  return yyVal;
};
states[571] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_args(null, null, null, null, null);
  return yyVal;
};
states[572] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_param_error", ((IRubyObject)yyVals[0+yyTop]));
                    p.error();
  return yyVal;
};
states[573] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_param_error", ((IRubyObject)yyVals[0+yyTop]));
                    p.error();
  return yyVal;
};
states[574] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_param_error", ((IRubyObject)yyVals[0+yyTop]));
                    p.error();
  return yyVal;
};
states[575] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_param_error", ((IRubyObject)yyVals[0+yyTop]));
                    p.error();
  return yyVal;
};
states[577] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.arg_var(p.formal_argument(((IRubyObject)yyVals[0+yyTop])));
  return yyVal;
};
states[578] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.setCurrentArg(((IRubyObject)yyVals[0+yyTop]));
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[579] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.setCurrentArg(null);
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[580] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_mlhs_paren", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[581] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[582] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((RubyArray)yyVals[-2+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[583] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.arg_var(p.formal_argument(((IRubyObject)yyVals[0+yyTop])));
                    p.setCurrentArg(((IRubyObject)yyVals[0+yyTop]));
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[584] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.setCurrentArg(null);
                    yyVal = p.keyword_arg(((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[585] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.setCurrentArg(null);
                    yyVal = p.keyword_arg(((IRubyObject)yyVals[0+yyTop]), p.getContext().getRuntime().getFalse());
  return yyVal;
};
states[586] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.keyword_arg(((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[587] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.keyword_arg(((IRubyObject)yyVals[0+yyTop]), p.getContext().getRuntime().getFalse());
  return yyVal;
};
states[588] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[589] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((RubyArray)yyVals[-2+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[590] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[591] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((RubyArray)yyVals[-2+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[592] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[593] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[594] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.shadowing_lvar(((IRubyObject)yyVals[0+yyTop]));
                    yyVal = p.dispatch("on_kwrest_param", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[595] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_kwrest_param", null);
  return yyVal;
};
states[596] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.setCurrentArg(null);
                    yyVal = p.new_assoc(((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));

  return yyVal;
};
states[597] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.setCurrentArg(null);
                    yyVal = p.new_assoc(((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[598] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[599] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((RubyArray)yyVals[-2+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[600] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[601] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((RubyArray)yyVals[-2+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[604] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.arg_var(p.shadowing_lvar(((IRubyObject)yyVals[0+yyTop])));
                    yyVal = p.dispatch("on_rest_param", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[605] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_rest_param", null);
  return yyVal;
};
states[608] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.arg_var(p.shadowing_lvar(((IRubyObject)yyVals[0+yyTop])));
                    yyVal = p.dispatch("on_blockarg", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[609] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[610] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[611] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[612] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    p.setState(EXPR_BEG);
  return yyVal;
};
states[613] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_paren", ((IRubyObject)yyVals[-1+yyTop]));
  return yyVal;
};
states[615] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assoclist_from_args", ((RubyArray)yyVals[-1+yyTop]));
  return yyVal;
};
states[616] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.new_array(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[617] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((RubyArray)yyVals[-2+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[618] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assoc_new", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[619] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assoc_new", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[620] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assoc_new", p.dispatch("on_dyna_symbol", ((IRubyObject)yyVals[-2+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[621] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.dispatch("on_assoc_splat", ((IRubyObject)yyVals[0+yyTop]));
  return yyVal;
};
states[632] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[633] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[634] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.intern(".");
  return yyVal;
};
states[635] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = p.intern("&.");
  return yyVal;
};
states[636] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[637] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                   yyVal = p.intern("::");
  return yyVal;
};
states[642] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[643] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
  return yyVal;
};
states[651] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                      yyVal = null;
  return yyVal;
};
states[652] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {
                  yyVal = null;
  return yyVal;
};
}
					// line 2263 "RipperParser.y"
}
					// line 9839 "-"
