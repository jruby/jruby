// created by jay 1.0.2 (c) 2002-2004 ats@cs.rit.edu
// skeleton Java 1.0 (c) 2002 ats@cs.rit.edu

					// line 2 "Ripper19Parser.y"
/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2008-2009 Thomas E Enebo <enebo@acm.org>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.parser;

import java.io.IOException;

import org.jruby.RubyArray;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.common.IRubyWarnings;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.ISourcePositionHolder;
import org.jruby.lexer.yacc.LexerSource;
import org.jruby.lexer.yacc.RubyYaccLexer;
import org.jruby.lexer.yacc.RubyYaccLexer.LexState;
import org.jruby.lexer.yacc.StrTerm;
import org.jruby.lexer.yacc.SyntaxException;
import org.jruby.lexer.yacc.SyntaxException.PID;
import org.jruby.lexer.yacc.Token;
import org.jruby.util.ByteList;

public class Ripper19Parser implements RubyParser {
    protected RipperSupport support;
    protected RubyYaccLexer lexer;

    public Ripper19Parser() {
        this(new RipperSupport());
    }

    public Ripper19Parser(RipperSupport support) {
        this.support = support;
        lexer = new RubyYaccLexer(false);
        lexer.setParserSupport(support);
        support.setLexer(lexer);
    }

    public void setWarnings(IRubyWarnings warnings) {
        support.setWarnings(warnings);
        lexer.setWarnings(warnings);
    }
					// line 71 "-"
  // %token constants
  public static final int kCLASS = 257;
  public static final int kMODULE = 258;
  public static final int kDEF = 259;
  public static final int kUNDEF = 260;
  public static final int kBEGIN = 261;
  public static final int kRESCUE = 262;
  public static final int kENSURE = 263;
  public static final int kEND = 264;
  public static final int kIF = 265;
  public static final int kUNLESS = 266;
  public static final int kTHEN = 267;
  public static final int kELSIF = 268;
  public static final int kELSE = 269;
  public static final int kCASE = 270;
  public static final int kWHEN = 271;
  public static final int kWHILE = 272;
  public static final int kUNTIL = 273;
  public static final int kFOR = 274;
  public static final int kBREAK = 275;
  public static final int kNEXT = 276;
  public static final int kREDO = 277;
  public static final int kRETRY = 278;
  public static final int kIN = 279;
  public static final int kDO = 280;
  public static final int kDO_COND = 281;
  public static final int kDO_BLOCK = 282;
  public static final int kRETURN = 283;
  public static final int kYIELD = 284;
  public static final int kSUPER = 285;
  public static final int kSELF = 286;
  public static final int kNIL = 287;
  public static final int kTRUE = 288;
  public static final int kFALSE = 289;
  public static final int kAND = 290;
  public static final int kOR = 291;
  public static final int kNOT = 292;
  public static final int kIF_MOD = 293;
  public static final int kUNLESS_MOD = 294;
  public static final int kWHILE_MOD = 295;
  public static final int kUNTIL_MOD = 296;
  public static final int kRESCUE_MOD = 297;
  public static final int kALIAS = 298;
  public static final int kDEFINED = 299;
  public static final int klBEGIN = 300;
  public static final int klEND = 301;
  public static final int k__LINE__ = 302;
  public static final int k__FILE__ = 303;
  public static final int k__ENCODING__ = 304;
  public static final int kDO_LAMBDA = 305;
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
  public static final int tCOLON2 = 335;
  public static final int tCOLON3 = 336;
  public static final int tOP_ASGN = 337;
  public static final int tASSOC = 338;
  public static final int tLPAREN = 339;
  public static final int tLPAREN2 = 340;
  public static final int tRPAREN = 341;
  public static final int tLPAREN_ARG = 342;
  public static final int tLBRACK = 343;
  public static final int tRBRACK = 344;
  public static final int tLBRACE = 345;
  public static final int tLBRACE_ARG = 346;
  public static final int tSTAR = 347;
  public static final int tSTAR2 = 348;
  public static final int tAMPER = 349;
  public static final int tAMPER2 = 350;
  public static final int tTILDE = 351;
  public static final int tPERCENT = 352;
  public static final int tDIVIDE = 353;
  public static final int tPLUS = 354;
  public static final int tMINUS = 355;
  public static final int tLT = 356;
  public static final int tGT = 357;
  public static final int tPIPE = 358;
  public static final int tBANG = 359;
  public static final int tCARET = 360;
  public static final int tLCURLY = 361;
  public static final int tRCURLY = 362;
  public static final int tBACK_REF2 = 363;
  public static final int tSYMBEG = 364;
  public static final int tSTRING_BEG = 365;
  public static final int tXSTRING_BEG = 366;
  public static final int tREGEXP_BEG = 367;
  public static final int tWORDS_BEG = 368;
  public static final int tQWORDS_BEG = 369;
  public static final int tSTRING_DBEG = 370;
  public static final int tSTRING_DVAR = 371;
  public static final int tSTRING_END = 372;
  public static final int tLAMBDA = 373;
  public static final int tLAMBEG = 374;
  public static final int tNTH_REF = 375;
  public static final int tBACK_REF = 376;
  public static final int tSTRING_CONTENT = 377;
  public static final int tINTEGER = 378;
  public static final int tFLOAT = 379;
  public static final int tREGEXP_END = 380;
  public static final int tLOWEST = 381;
  public static final int keyword_variable = 382;
  public static final int user_variable = 383;
  public static final int yyErrorCode = 256;

  /** number of final state.
    */
  protected static final int yyFinal = 1;

  /** parser tables.
      Order is mandated by <i>jay</i>.
    */
  protected static final short[] yyLhs = {
//yyLhs 553
    -1,   121,     0,   118,   119,   119,   119,   119,   120,   124,
   120,    35,    34,    36,    36,    36,    36,   125,    37,    37,
    37,    37,    37,    37,    37,    37,    37,    37,    37,    37,
    37,    37,    37,    37,    37,    37,    37,    37,    37,    37,
    37,    37,    32,    32,    38,    38,    38,    38,    38,    38,
    42,    33,    33,    56,    56,    56,   127,    95,    41,    41,
    41,    41,    41,    41,    41,    41,    41,    41,    41,    96,
    96,   107,   107,    97,    97,    97,    97,    97,    97,    97,
    97,    97,    97,    68,    68,    82,    82,    86,    86,    69,
    69,    69,    69,    69,    69,    69,    69,    69,    74,    74,
    74,    74,    74,    74,    74,    74,    74,     7,     7,    31,
    31,    31,     8,     8,     8,     8,     8,   100,   100,   101,
   101,    58,   128,    58,     9,     9,     9,     9,     9,     9,
     9,     9,     9,     9,     9,     9,     9,     9,     9,     9,
     9,     9,     9,     9,     9,     9,     9,     9,     9,     9,
     9,     9,     9,   116,   116,   116,   116,   116,   116,   116,
   116,   116,   116,   116,   116,   116,   116,   116,   116,   116,
   116,   116,   116,   116,   116,   116,   116,   116,   116,   116,
   116,   116,   116,   116,   116,   116,   116,   116,   116,   116,
   116,   116,   116,   116,   116,    39,    39,    39,    39,    39,
    39,    39,    39,    39,    39,    39,    39,    39,    39,    39,
    39,    39,    39,    39,    39,    39,    39,    39,    39,    39,
    39,    39,    39,    39,    39,    39,    39,    39,    39,    39,
    39,    39,    39,    39,    39,    39,    39,    39,    39,    39,
    70,    73,    73,    73,    73,    50,    54,    54,   110,   110,
    48,    48,    48,    48,    48,   130,    52,    89,    88,    88,
    88,    76,    76,    76,    76,    67,    67,    67,    40,    40,
    40,    40,    40,    40,    40,    40,    40,    40,   131,    40,
    40,    40,    40,    40,    40,    40,    40,    40,    40,    40,
    40,    40,    40,    40,    40,    40,    40,    40,   133,   135,
    40,   136,   137,    40,    40,    40,   138,   139,    40,   140,
    40,   142,   143,    40,   144,    40,   145,    40,   146,   147,
    40,    40,    40,    40,    40,    43,   132,   132,   132,   134,
   134,    46,    46,    44,    44,   109,   109,   111,   111,    81,
    81,   112,   112,   112,   112,   112,   112,   112,   112,   112,
    64,    64,    64,    64,    64,    64,    64,    64,    64,    64,
    64,    64,    64,    64,    64,    66,    66,    65,    65,    65,
   104,   104,   103,   103,   113,   113,   148,   106,    63,    63,
   105,   105,   149,    94,    55,    55,    55,    24,    24,    24,
    24,    24,    24,    24,    24,    24,   150,    93,   151,    93,
    71,    45,    45,    98,    98,    72,    72,    72,    47,    47,
    49,    49,    28,    28,    28,    16,    17,    17,    17,    18,
    19,    20,    25,    25,    78,    78,    27,    27,    26,    26,
    77,    77,    21,    21,    22,    22,    23,   152,    23,   153,
    23,    59,    59,    59,    59,     3,     2,     2,     2,     2,
    30,    29,    29,    29,    29,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,    53,    99,    60,
    60,    51,   154,    51,    51,    62,    62,    61,    61,    61,
    61,    61,    61,    61,    61,    61,    61,    61,    61,    61,
    61,    61,   117,   117,   117,   117,    10,    10,   102,   102,
    79,    79,    57,   108,    87,    87,    80,    80,    12,    12,
    14,    14,    13,    13,    92,    91,    91,    15,   155,    15,
    85,    85,    83,    83,    84,    84,     4,     4,     4,     5,
     5,     5,     5,     6,     6,     6,    11,    11,   122,   122,
   126,   126,   114,   115,   129,   129,   129,   141,   141,   123,
   123,    75,    90,
    }, yyLen = {
//yyLen 553
     2,     0,     2,     2,     1,     1,     3,     2,     1,     0,
     5,     4,     2,     1,     1,     3,     2,     0,     4,     3,
     3,     3,     2,     3,     3,     3,     3,     3,     4,     1,
     3,     3,     6,     5,     5,     5,     5,     3,     3,     3,
     3,     1,     3,     3,     1,     3,     3,     3,     2,     1,
     1,     1,     1,     1,     4,     4,     0,     5,     2,     3,
     4,     5,     4,     5,     2,     2,     2,     2,     2,     1,
     3,     1,     3,     1,     2,     3,     5,     2,     4,     2,
     4,     1,     3,     1,     3,     2,     3,     1,     3,     1,
     1,     4,     3,     3,     3,     3,     2,     1,     1,     1,
     4,     3,     3,     3,     3,     2,     1,     1,     1,     2,
     1,     3,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     0,     4,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     3,     5,     3,     5,     6,
     5,     5,     5,     5,     4,     3,     3,     3,     3,     3,
     3,     3,     3,     3,     4,     4,     2,     2,     3,     3,
     3,     3,     3,     3,     3,     3,     3,     3,     3,     3,
     3,     2,     2,     3,     3,     3,     3,     3,     6,     1,
     1,     1,     2,     4,     2,     3,     1,     1,     1,     1,
     1,     2,     2,     4,     1,     0,     2,     2,     2,     1,
     1,     1,     2,     3,     4,     3,     4,     2,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     3,     0,     4,
     3,     3,     2,     3,     3,     1,     4,     3,     1,     5,
     4,     3,     2,     1,     2,     2,     6,     6,     0,     0,
     7,     0,     0,     7,     5,     4,     0,     0,     9,     0,
     6,     0,     0,     8,     0,     5,     0,     6,     0,     0,
     9,     1,     1,     1,     1,     1,     1,     1,     2,     1,
     1,     1,     5,     1,     2,     1,     1,     1,     3,     1,
     3,     1,     4,     6,     3,     5,     2,     4,     1,     3,
     6,     8,     4,     6,     4,     2,     6,     2,     4,     6,
     2,     4,     2,     4,     1,     1,     1,     3,     1,     4,
     1,     4,     1,     3,     1,     1,     0,     3,     4,     2,
     3,     3,     0,     5,     2,     4,     4,     2,     4,     4,
     3,     3,     3,     2,     1,     4,     0,     5,     0,     5,
     5,     1,     1,     6,     0,     1,     1,     1,     2,     1,
     2,     1,     1,     1,     1,     1,     1,     1,     2,     3,
     3,     3,     3,     3,     0,     3,     1,     2,     3,     3,
     0,     3,     0,     2,     0,     2,     1,     0,     3,     0,
     4,     1,     1,     1,     1,     2,     1,     1,     1,     1,
     3,     1,     1,     2,     2,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     0,     4,     2,     3,     2,     6,     8,     4,
     6,     4,     6,     2,     4,     6,     2,     4,     2,     4,
     1,     0,     1,     1,     1,     1,     1,     1,     1,     3,
     1,     3,     3,     3,     1,     3,     1,     3,     1,     1,
     2,     1,     1,     1,     2,     2,     0,     1,     0,     4,
     1,     2,     1,     3,     3,     2,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     0,     1,
     0,     1,     2,     2,     0,     1,     1,     1,     1,     1,
     2,     0,     0,
    }, yyDefRed = {
//yyDefRed 964
     1,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   298,   301,     0,     0,     0,   323,   324,     0,
     0,     0,   461,   460,   462,   463,     0,     0,     0,     9,
     0,   465,   464,   466,     0,     0,   457,   456,     0,   459,
   416,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   432,   434,   434,     0,     0,   376,   469,
   470,   451,   452,     0,    98,     0,   413,     0,   269,     0,
   417,   270,   271,     0,   272,   273,   268,   412,   414,    29,
    44,     0,     0,     0,     0,     0,     0,   274,     0,    52,
     0,     0,    83,     0,     4,     0,     0,    69,     0,     2,
     0,     5,     7,   321,   322,   285,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   467,     0,   110,     0,
   325,     0,   275,   314,   163,   174,   164,   187,   160,   180,
   170,   169,   185,   168,   167,   162,   188,   172,   161,   175,
   179,   181,   173,   166,   182,   189,   184,     0,     0,     0,
     0,   159,   178,   177,   190,   191,   192,   193,   194,   158,
   165,   156,   157,     0,     0,     0,     0,   114,     0,   148,
   149,   145,   127,   128,   129,   136,   133,   135,   130,   131,
   150,   151,   137,   138,   518,   142,   141,   126,   147,   144,
   143,   139,   140,   134,   132,   124,   146,   125,   152,   316,
   115,     0,   517,   116,   183,   176,   186,   171,   153,   154,
   155,   112,   113,   118,   117,   120,     0,   119,   121,     0,
     0,     0,     0,     0,    13,     0,    99,     0,     0,     0,
     0,     0,     0,     0,     0,   547,   548,     0,     0,     0,
   549,     0,     0,     0,     0,     0,     0,   335,   336,     0,
     0,     0,     0,     0,     0,   250,    67,     0,     0,     0,
   522,   254,    68,    66,     0,    65,     0,     0,   393,    64,
     0,   541,     0,     0,    17,     0,     0,     0,   216,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   241,     0,     0,     0,   520,     0,     0,     0,     0,    90,
     0,     0,     0,   232,    48,   231,   448,   447,   449,   445,
   446,     0,     0,     0,     0,     0,     0,     0,     0,   295,
     0,   398,   396,   387,     0,   292,   418,   294,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   382,   384,     0,     0,     0,     0,     0,     0,    85,
     0,     0,     0,     0,     0,     0,     3,     0,     0,   453,
   454,     0,   107,     0,   109,     0,   472,   309,   471,     0,
     0,     0,     0,     0,     0,   536,   537,   318,   122,     0,
     0,     0,   277,    12,     0,     0,   327,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   550,
     0,     0,     0,     0,     0,     0,   306,   525,   262,   257,
     0,     0,   251,   260,     0,   252,     0,   287,     0,   256,
   249,   248,     0,     0,   291,    47,    19,    21,    20,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   280,     0,     0,   283,     0,   545,   242,     0,   244,   521,
   284,     0,    87,     0,     0,     0,     0,     0,   439,   437,
   450,   436,   435,   419,   433,   420,   421,   422,   423,   426,
     0,   428,   429,     0,     0,   494,   493,   492,   495,     0,
     0,   509,   508,   513,   512,   498,     0,     0,     0,   506,
     0,     0,     0,     0,   490,   500,   496,     0,     0,    56,
    59,    23,    24,    25,    26,    27,    45,    46,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   531,     0,     0,   532,
   391,     0,     0,     0,     0,   390,     0,   392,     0,   529,
   530,     0,     0,    37,     0,     0,    43,    42,     0,    38,
   261,     0,     0,     0,     0,     0,    86,    30,    40,     0,
    31,     0,     6,     0,   474,     0,     0,     0,     0,     0,
     0,   111,     0,     0,     0,     0,     0,     0,     0,     0,
   406,     0,     0,   407,     0,     0,   333,     0,     0,   328,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   305,
   330,   299,   329,   302,     0,     0,     0,     0,     0,     0,
   524,     0,     0,     0,   258,   523,   286,   542,   245,   290,
    18,     0,     0,    28,     0,     0,     0,     0,   279,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   425,
   427,   431,     0,   497,     0,     0,   337,     0,   339,     0,
     0,   510,   514,     0,   488,   379,     0,     0,     0,   377,
     0,   483,     0,   486,   368,     0,   366,     0,   365,     0,
     0,     0,     0,     0,     0,   247,     0,   388,   246,     0,
     0,   389,     0,     0,     0,    54,   385,    55,   386,     0,
     0,     0,     0,    84,     0,     0,     0,   312,     0,     0,
   395,   315,   519,     0,   476,     0,   319,   123,     0,     0,
   409,   334,     0,    11,   411,     0,   331,     0,     0,     0,
     0,     0,     0,     0,   304,     0,     0,     0,     0,     0,
     0,   264,   253,   289,    10,   243,    88,     0,     0,   441,
   442,   443,   438,   444,   502,     0,     0,     0,     0,   499,
     0,     0,   515,     0,     0,     0,     0,     0,   501,     0,
   507,     0,     0,     0,     0,     0,     0,   364,     0,   504,
     0,     0,     0,     0,     0,    33,     0,    34,     0,    61,
    36,     0,    35,     0,    63,     0,   543,     0,     0,     0,
     0,     0,     0,   473,   310,   475,   317,     0,     0,     0,
     0,   408,     0,   410,     0,   296,     0,   297,   263,     0,
     0,     0,   307,   440,   338,     0,     0,     0,   340,   378,
     0,   489,   374,     0,   372,   375,   381,   380,     0,   481,
     0,   479,     0,   484,   487,     0,     0,   362,     0,     0,
   357,     0,   360,   367,   399,   397,     0,     0,   383,    32,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   401,   400,   402,   300,   303,     0,     0,     0,     0,     0,
     0,   371,     0,     0,     0,     0,     0,     0,     0,   369,
     0,     0,     0,     0,   505,    57,   313,     0,     0,     0,
     0,     0,     0,   403,     0,     0,     0,     0,   373,   482,
     0,   477,   480,   485,   282,     0,   363,     0,   354,     0,
   352,     0,   358,   361,   320,     0,   332,   308,     0,     0,
     0,     0,     0,     0,     0,     0,   478,   356,     0,   350,
   353,   359,     0,   351,
    }, yyDgoto = {
//yyDgoto 156
     1,   227,   309,    66,    67,   600,   565,   118,   214,   559,
   505,   397,   506,   507,   508,   201,    68,    69,    70,    71,
    72,   312,   311,   482,    73,    74,    75,   490,    76,    77,
    78,   119,    79,    80,   220,   221,   222,   223,    82,    83,
    84,    85,   229,   279,   746,   891,   747,   739,   440,   743,
   567,   387,   265,    87,   707,    88,    89,   509,   216,   772,
   231,   606,   607,   511,   794,   696,   697,   579,    91,    92,
   257,   418,   612,   289,   232,   224,   258,   318,   316,   512,
   513,   677,    95,   259,   260,   296,   473,   796,   432,   261,
   433,   684,   782,   325,   362,   520,    96,    97,   401,   233,
   217,   218,   515,   853,   685,   689,   319,   287,   799,   249,
   442,   678,   679,   854,   437,   713,   203,   516,    99,   100,
   101,     2,   238,   239,   276,   449,   438,   700,   609,   466,
   266,   462,   407,   241,   631,   757,   242,   758,   639,   895,
   596,   408,   593,   822,   392,   394,   608,   827,   320,   554,
   518,   517,   668,   667,   595,   393,
    }, yySindex = {
//yySindex 964
     0,     0, 14723, 15106, 18025, 18148, 18610, 18502, 14851, 17011,
 17011, 12822,     0,     0, 17519,  5429,  5429,     0,     0,  5429,
  -274,  -219,     0,     0,     0,     0,    40, 18394,   147,     0,
  -194,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 17138, 17138,   -86,  -130, 14979, 17011, 15614, 15995,  4525,
 17138, 17265, 18717,     0,     0,     0,   162,   217,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   381,     0,  -158,
     0,     0,     0,  -161,     0,     0,     0,     0,     0,     0,
     0,  1332,   306,  4379,     0,   -41,   164,     0,   -31,     0,
   -64,   233,     0,   247,     0, 17773,   259,     0,     2,     0,
    95,     0,     0,     0,     0,     0,  -274,  -219,   -10,   147,
     0,     0,   -68, 17011,   -49, 14851,     0,   381,     0,   106,
     0,   405,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   -16,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   303,     0,     0, 15233,
    94,   149,    95,  1332,     0,   101,     0,     0,   306,   286,
   439,   141,   496,   225,   286,     0,     0,    95,   314,   549,
     0, 17011, 17011,   311,     0,   568,     0,     0,     0,   344,
 17138, 17138, 17138, 17138,  4379,     0,     0,   288,   600,   604,
     0,     0,     0,     0,  5921,     0,  5429,  5429,     0,     0,
  6385,     0, 17011,  -200,     0, 16122,   291, 14851,     0,   618,
   341,   343,   347,   330, 14979,   349,     0,   147,   306,   368,
     0,   115,   128,   288,     0,   128,   356,   410, 17899,     0,
   636,     0,   679,     0,     0,     0,     0,     0,     0,     0,
     0,   321,   647,   694,   358,   365,   798,   372,   -70,     0,
  2418,     0,     0,     0,   403,     0,     0,     0, 17011, 17011,
 17011, 17011, 15233, 17011, 17011, 17138, 17138, 17138, 17138, 17138,
 17138, 17138, 17138, 17138, 17138, 17138, 17138, 17138, 17138, 17138,
 17138, 17138, 17138, 17138, 17138, 17138, 17138, 17138, 17138, 17138,
 17138,     0,     0,  2483,  2568,  5429, 18989, 18989, 17265,     0,
 16249, 14979, 14342,   706, 16249, 17265,     0, 14468,   414,     0,
     0,   306,     0,     0,     0,    95,     0,     0,     0,  2980,
  3062,  5429, 14851, 17011,  2430,     0,     0,     0,     0,  1332,
 16376,   486,     0,     0, 14595,   330,     0, 14851,   491,  3477,
  3559,  5429, 17138, 17138, 17138, 14851,   314, 16503,   497,     0,
   105,   105,     0,  3974,  4056,  5429,     0,     0,     0,     0,
 17138, 15360,     0,     0, 15741,     0,   147,     0,   422,     0,
     0,     0,   147,   242,     0,     0,     0,     0,     0, 18502,
 17011,  4379, 14723,   406,  3477,  3559, 17138, 17138, 17138,   147,
     0,     0,   147,     0, 15868,     0,     0, 15995,     0,     0,
     0,     0,     0,   725,  5511,  6024,  5429, 17899,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
    96,     0,     0,   749,   726,     0,     0,     0,     0,  1054,
  1258,     0,     0,     0,     0,     0,   476,   480,   747,     0,
   147,  -204,   752,   753,     0,     0,     0,  -181,  -181,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   341,  2080,
  2080,  2080,  2080,  2008,  2008,  3894,  3397,  2080,  2080,  2900,
  2900,  1141,  1141,   341,  1037,   341,   341,   471,   471,  2008,
  2008,  1362,  1362,  1859,  -181,   461,     0,   464,  -219,     0,
     0,   468,     0,   473,  -219,     0,     0,     0,   147,     0,
     0,  -219,  -219,     0,  4379, 17138,     0,     0,  4916,     0,
     0,   750,   771,   147, 17899,   772,     0,     0,     0,     0,
     0,  5043,     0,    95,     0, 17011, 14851,  -219,     0,     0,
  -219,     0,   147,   558,   242,  1258,    95, 14851, 18824, 18502,
     0,     0,   490,     0, 14851,   572,     0,  1332,   408,     0,
   494,   499,   501,   510,   147,  4916,   486,   584,    92,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   147, 17011,
     0, 17138,   288,   604,     0,     0,     0,     0,     0,     0,
     0,   242,   487,     0,   341,   341,  4379,     0,     0,   128,
 17899,     0,     0,     0,     0,   147,   725, 14851,   -97,     0,
     0,     0, 17138,     0,  1054,   783,     0,   807,     0,   147,
   147,     0,     0,   933,     0,     0,   793, 14851, 14851,     0,
  1258,     0,  1258,     0,     0,   720,     0, 14851,     0, 14851,
  -181,   797, 14851, 17265, 17265,     0,   403,     0,     0, 17265,
 17265,     0,   403,   523,   521,     0,     0,     0,     0,     0,
 17138, 17265, 16630,     0,   725, 17899, 17138,     0,    95,   602,
     0,     0,     0,   147,     0,   607,     0,     0, 17646,   286,
     0,     0, 14851,     0,     0, 17011,     0,   609, 17138, 17138,
 17138, 17138,   531,   610,     0, 16757, 14851, 14851, 14851,     0,
   105,     0,     0,     0,     0,     0,     0,     0,   514,     0,
     0,     0,     0,     0,     0,   147,  1352,   833,  1516,     0,
   542,   840,     0,  1067,   628,   532,   863,   866,     0,   868,
     0,   840,   852,   872,   147,   875,   886,     0,   576,     0,
   674,   579, 14851, 17138,   680,     0,  4379,     0,  4379,     0,
     0,  4379,     0,  4379,     0, 17265,     0,  4379, 17138,     0,
   725,  4379, 14851,     0,     0,     0,     0,  2430,   635,   718,
     0,     0, 14851,     0,   286,     0, 17138,     0,     0,  -113,
   685,   686,     0,     0,     0,   908,  1352,   979,     0,     0,
   933,     0,     0,   186,     0,     0,     0,     0,   933,     0,
  1258,     0,   933,     0,     0, 18271,   933,     0,   597,  2412,
     0,  2412,     0,     0,     0,     0,   608,  4379,     0,     0,
  4379,     0,   708, 14851,     0,  6950, 18879,  5429,    94, 14851,
     0,     0,     0,     0,     0, 14851,  1352,   908,  1352,   914,
  1067,     0,   840,   929,   840,   840,   676,   781,   840,     0,
   946,   959,   960,   840,     0,     0,     0,   741,     0,     0,
     0,     0,   147,     0,   408,   751,   908,  1352,     0,     0,
   933,     0,     0,     0,     0, 18934,     0,   933,     0,  2412,
     0,   933,     0,     0,     0,     0,     0,     0,   908,   840,
     0,     0,   840,   972,   840,   840,     0,     0,   933,     0,
     0,     0,   840,     0,
    }, yyRindex = {
//yyRindex 964
     0,     0,   145,     0,     0,     0,     0,     0,   731,     0,
     0,   756,     0,     0,     0, 12973, 13103,     0,     0, 13213,
  4806,  4309,     0,     0,     0,     0, 17392,     0, 16884,     0,
     0,     0,     0,     0,  2194,  3315,     0,     0,  2321,     0,
     0,     0,     0,     0,     0,   150,     0,   688,   673,    86,
     0,     0,   829,     0,     0,     0,   926,   188,     0,     0,
     0,     0,     0,    30,     0, 13454,     0, 15487,     0,  7203,
     0,     0,     0,  7317,     0,     0,     0,     0,     0,     0,
     0,    76, 14280, 13862,  7418, 14103,     0,     0, 14140,     0,
 13583,     0,     0,     0,     0,   215,     0,     0,     0,     0,
    34,     0,     0,     0,     0,     0,  7562,  6522,     0,   696,
 11779, 11907,     0,     0,     0,   150,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  2061,  2436,  2516,
  2933,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,  3013,  3430,  3510,  3927,     0,  4007,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0, 14299,     0,     0,     0,
   416,     0,  1053,    19,     0,     0,     0,  6737, 14190,     0,
     0,  6871,     0,     0,     0,     0,     0,   756,     0,   763,
     0,     0,     0,     0,  1283,     0,   661,     0,     0,     0,
     0,     0,     0,     0, 11512,     0,     0, 13805,  1038,  1038,
     0,     0,     0,     0,   702,     0,     0,    31,     0,     0,
   702,     0,     0,     0,     0,     0,     0,    70,     0,     0,
  7911,  7666,  7779, 13702,   150,     0,   124,   702,   130,     0,
     0,   707,   707,     0,     0,   692,     0,     0,     0,     0,
     0,  1447,   466,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
    59,     0,     0,     0,   716,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,    27,     0,     0,     0,     0,
     0,   150,   493,   505,     0,     0,     0,    47,     0,     0,
     0,   110,     0, 12295,     0,     0,     0,     0,     0,     0,
     0,    27,   731,     0,   122,     0,     0,     0,     0,    54,
   317,   436,     0,     0,  1215,  7069,     0,   622, 12423,     0,
     0,    27,     0,     0,     0,   318,     0,     0,     0,     0,
     0,     0,   923,     0,     0,    27,     0,     0,     0,     0,
     0, 11382,     0,     0, 11382,     0,   702,     0,     0,     0,
     0,     0,   702,   702,     0,     0,     0,     0,     0,     0,
     0, 10751,    70,     0,     0,     0,     0,     0,     0,   702,
     0,    45,   702,     0,   732,     0,     0,  -191,     0,     0,
     0,  1638,     0,   621,     0,     0,    27,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,    65,     0,     0,     0,     0,     0,
   172,     0,     0,     0,     0,     0,   140,     0,    41,     0,
   -21,     0,    41,    41,     0,     0,     0, 12553, 12692,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  8027,  9975,
 10096, 10204, 10292,  9514,  9640, 10388, 10661, 10477, 10565,  1613,
  2011,  8953,  9057,  8128,  9179,  8243,  8376,  8708,  8840,  9754,
  9856,  9298,  9400,  1017, 12553,  5167,     0,  5294,  4679,     0,
     0,  5664,  3685,  5791, 15487,     0,  3812,     0,   735,     0,
     0,  6152,  6152,     0, 10838,     0,     0,     0,  1722,     0,
     0,     0,     0,   702,     0,   625,     0,     0,     0, 14228,
     0, 11598,     0,     0,     0,     0,   731,  6623, 12037, 12165,
     0,     0,   735,     0,   702,   156,     0,   731,     0,     0,
     0,   296,   511,     0,   595,   816,     0,    80,   816,     0,
  2691,  2818,  3188,  4182,   735, 11659,   816,     0,     0,     0,
     0,     0,     0,     0,  1510,  1800,  1808,   817,   735,     0,
     0,     0, 13915,  1038,     0,     0,     0,     0,     0,     0,
     0,   702,     0,     0,  8491,  8592, 10878,   117,     0,   707,
     0,   820,   857,  1184,   596,   735,   698,    70,     0,     0,
     0,     0,     0,     0,     0,   169,     0,   182,     0,   702,
     9,     0,     0,     0,     0,     0,   348,   240,    70,     0,
     0,     0,     0,     0,     0,   -20,     0,   240,     0,    70,
 12692,     0,   240,     0,     0,     0, 13951,     0,     0,     0,
     0,     0, 14007, 13343,     0,     0,     0,     0,     0,  1172,
     0,     0,     0,     0,   703,     0,     0,     0,     0,     0,
     0,     0,     0,   702,     0,     0,     0,     0,     0,     0,
     0,     0,   240,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  6274,     0,     0,     0,   535,   240,   240,   574,
     0,     0,     0,     0,     0,     0,     0,  1220,     0,     0,
     0,     0,     0,     0,     0,   702,     0,   183,     0,     0,
     0,    41,     0,     0,     0,     0,    41,    41,     0,    41,
     0,    41,    78,    -4,   -20,    -4,    -4,     0,     0,     0,
     0,     0,    70,     0,     0,     0, 10967,     0, 11028,     0,
     0, 11068,     0, 11155,     0,     0,     0, 11195,     0,  1764,
   722, 11256,   731,     0,     0,     0,     0,   122,     0,     0,
   721,     0,   731,     0,     0,     0,     0,     0,     0,   816,
     0,     0,     0,     0,     0,   185,     0,   187,     0,     0,
     0,     0,     0,   402,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,    83,
     0,     0,     0,     0,     0,     0,     0, 11342,     0,     0,
 11472, 10112,     0,   731,  1146,     0,     0,    27,   416,   622,
     0,     0,     0,     0,     0,   240,     0,   193,     0,   195,
     0,     0,    41,    41,    41,    41,     0,    82,    -4,     0,
    -4,    -4,    -4,    -4,     0,     0,     0,     0,   203,   823,
  1076,  1120,   735,     0,   816,     0,   196,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   207,     0,     0,   198,    41,
  1251,   241,    -4,    -4,    -4,    -4,     0,     0,     0,     0,
     0,     0,    -4,     0,
    }, yyGindex = {
//yyGindex 156
     0,   385,     0,    -2,  1589,  -306,     0,   -61,     7,    -6,
  -473,     0,     0,     0,   481,     0,     0,     0,  1012,     0,
     0,     0,   656,  -218,     0,     0,     0,     0,     0,     0,
     8,  1083,  -343,    -7,    37,  -361,     0,    20,   178,  1403,
    28,    -8,     6,   218,  -383,     0,   174,     0,   631,     0,
    66,     0,     3,  1098,   175,     0,     0,  -620,     0,     0,
   670,  -264,   279,     0,     0,     0,  -447,  -156,   -83,    63,
   591,  -388,     0,     0,   842,     1,     5,     0,     0,  1406,
   421,   -28,     0,    15,  -192,     0,  -410,   243,  -245,  -320,
     0,   894,  -310,  1044,     0,  -496,  1104,   209,   232,   818,
     0,   -18,  -580,     0,  -549,     0,     0,  -185,  -776,     0,
  -346,  -705,   447,   224,   672,  -364,     0,  -702,   677,     0,
    33,     0,    49,    -3,     0,     0,   -24,     0,     0,  -252,
     0,     0,  -214,     0,  -391,     0,     0,     0,     0,     0,
     0,    43,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,
    };
    protected static final short[] yyTable = Ripper19YyTables.yyTable();
    protected static final short[] yyCheck = Ripper19YyTables.yyCheck();

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
    "kCLASS","kMODULE","kDEF","kUNDEF","kBEGIN","kRESCUE","kENSURE",
    "kEND","kIF","kUNLESS","kTHEN","kELSIF","kELSE","kCASE","kWHEN",
    "kWHILE","kUNTIL","kFOR","kBREAK","kNEXT","kREDO","kRETRY","kIN",
    "kDO","kDO_COND","kDO_BLOCK","kRETURN","kYIELD","kSUPER","kSELF",
    "kNIL","kTRUE","kFALSE","kAND","kOR","kNOT","kIF_MOD","kUNLESS_MOD",
    "kWHILE_MOD","kUNTIL_MOD","kRESCUE_MOD","kALIAS","kDEFINED","klBEGIN",
    "klEND","k__LINE__","k__FILE__","k__ENCODING__","kDO_LAMBDA",
    "tIDENTIFIER","tFID","tGVAR","tIVAR","tCONSTANT","tCVAR","tLABEL",
    "tCHAR","tUPLUS","tUMINUS","tUMINUS_NUM","tPOW","tCMP","tEQ","tEQQ",
    "tNEQ","tGEQ","tLEQ","tANDOP","tOROP","tMATCH","tNMATCH","tDOT",
    "tDOT2","tDOT3","tAREF","tASET","tLSHFT","tRSHFT","tCOLON2","tCOLON3",
    "tOP_ASGN","tASSOC","tLPAREN","tLPAREN2","tRPAREN","tLPAREN_ARG",
    "tLBRACK","tRBRACK","tLBRACE","tLBRACE_ARG","tSTAR","tSTAR2","tAMPER",
    "tAMPER2","tTILDE","tPERCENT","tDIVIDE","tPLUS","tMINUS","tLT","tGT",
    "tPIPE","tBANG","tCARET","tLCURLY","tRCURLY","tBACK_REF2","tSYMBEG",
    "tSTRING_BEG","tXSTRING_BEG","tREGEXP_BEG","tWORDS_BEG","tQWORDS_BEG",
    "tSTRING_DBEG","tSTRING_DVAR","tSTRING_END","tLAMBDA","tLAMBEG",
    "tNTH_REF","tBACK_REF","tSTRING_CONTENT","tINTEGER","tFLOAT",
    "tREGEXP_END","tLOWEST","keyword_variable","user_variable",
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
    "top_stmt : klBEGIN $$2 tLCURLY top_compstmt tRCURLY",
    "bodystmt : compstmt opt_rescue opt_else opt_ensure",
    "compstmt : stmts opt_terms",
    "stmts : none",
    "stmts : stmt",
    "stmts : stmts terms stmt",
    "stmts : error stmt",
    "$$3 :",
    "stmt : kALIAS fitem $$3 fitem",
    "stmt : kALIAS tGVAR tGVAR",
    "stmt : kALIAS tGVAR tBACK_REF",
    "stmt : kALIAS tGVAR tNTH_REF",
    "stmt : kUNDEF undef_list",
    "stmt : stmt kIF_MOD expr_value",
    "stmt : stmt kUNLESS_MOD expr_value",
    "stmt : stmt kWHILE_MOD expr_value",
    "stmt : stmt kUNTIL_MOD expr_value",
    "stmt : stmt kRESCUE_MOD stmt",
    "stmt : klEND tLCURLY compstmt tRCURLY",
    "stmt : command_asgn",
    "stmt : mlhs '=' command_call",
    "stmt : var_lhs tOP_ASGN command_call",
    "stmt : primary_value '[' opt_call_args rbracket tOP_ASGN command_call",
    "stmt : primary_value tDOT tIDENTIFIER tOP_ASGN command_call",
    "stmt : primary_value tDOT tCONSTANT tOP_ASGN command_call",
    "stmt : primary_value tCOLON2 tCONSTANT tOP_ASGN command_call",
    "stmt : primary_value tCOLON2 tIDENTIFIER tOP_ASGN command_call",
    "stmt : backref tOP_ASGN command_call",
    "stmt : lhs '=' mrhs",
    "stmt : mlhs '=' arg_value",
    "stmt : mlhs '=' mrhs",
    "stmt : expr",
    "command_asgn : lhs '=' command_call",
    "command_asgn : lhs '=' command_asgn",
    "expr : command_call",
    "expr : expr kAND expr",
    "expr : expr kOR expr",
    "expr : kNOT opt_nl expr",
    "expr : tBANG command_call",
    "expr : arg",
    "expr_value : expr",
    "command_call : command",
    "command_call : block_command",
    "block_command : block_call",
    "block_command : block_call tDOT operation2 command_args",
    "block_command : block_call tCOLON2 operation2 command_args",
    "$$4 :",
    "cmd_brace_block : tLBRACE_ARG $$4 opt_block_param compstmt tRCURLY",
    "command : operation command_args",
    "command : operation command_args cmd_brace_block",
    "command : primary_value tDOT operation2 command_args",
    "command : primary_value tDOT operation2 command_args cmd_brace_block",
    "command : primary_value tCOLON2 operation2 command_args",
    "command : primary_value tCOLON2 operation2 command_args cmd_brace_block",
    "command : kSUPER command_args",
    "command : kYIELD command_args",
    "command : kRETURN call_args",
    "command : kBREAK call_args",
    "command : kNEXT call_args",
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
    "mlhs_node : variable",
    "mlhs_node : keyword_variable",
    "mlhs_node : primary_value '[' opt_call_args rbracket",
    "mlhs_node : primary_value tDOT tIDENTIFIER",
    "mlhs_node : primary_value tCOLON2 tIDENTIFIER",
    "mlhs_node : primary_value tDOT tCONSTANT",
    "mlhs_node : primary_value tCOLON2 tCONSTANT",
    "mlhs_node : tCOLON3 tCONSTANT",
    "mlhs_node : backref",
    "lhs : user_variable",
    "lhs : keyword_variable",
    "lhs : primary_value '[' opt_call_args rbracket",
    "lhs : primary_value tDOT tIDENTIFIER",
    "lhs : primary_value tCOLON2 tIDENTIFIER",
    "lhs : primary_value tDOT tCONSTANT",
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
    "reswords : k__LINE__",
    "reswords : k__FILE__",
    "reswords : k__ENCODING__",
    "reswords : klBEGIN",
    "reswords : klEND",
    "reswords : kALIAS",
    "reswords : kAND",
    "reswords : kBEGIN",
    "reswords : kBREAK",
    "reswords : kCASE",
    "reswords : kCLASS",
    "reswords : kDEF",
    "reswords : kDEFINED",
    "reswords : kDO",
    "reswords : kELSE",
    "reswords : kELSIF",
    "reswords : kEND",
    "reswords : kENSURE",
    "reswords : kFALSE",
    "reswords : kFOR",
    "reswords : kIN",
    "reswords : kMODULE",
    "reswords : kNEXT",
    "reswords : kNIL",
    "reswords : kNOT",
    "reswords : kOR",
    "reswords : kREDO",
    "reswords : kRESCUE",
    "reswords : kRETRY",
    "reswords : kRETURN",
    "reswords : kSELF",
    "reswords : kSUPER",
    "reswords : kTHEN",
    "reswords : kTRUE",
    "reswords : kUNDEF",
    "reswords : kWHEN",
    "reswords : kYIELD",
    "reswords : kIF_MOD",
    "reswords : kUNLESS_MOD",
    "reswords : kWHILE_MOD",
    "reswords : kUNTIL_MOD",
    "reswords : kRESCUE_MOD",
    "arg : lhs '=' arg",
    "arg : lhs '=' arg kRESCUE_MOD arg",
    "arg : var_lhs tOP_ASGN arg",
    "arg : var_lhs tOP_ASGN arg kRESCUE_MOD arg",
    "arg : primary_value '[' opt_call_args rbracket tOP_ASGN arg",
    "arg : primary_value tDOT tIDENTIFIER tOP_ASGN arg",
    "arg : primary_value tDOT tCONSTANT tOP_ASGN arg",
    "arg : primary_value tCOLON2 tIDENTIFIER tOP_ASGN arg",
    "arg : primary_value tCOLON2 tCONSTANT tOP_ASGN arg",
    "arg : tCOLON3 tCONSTANT tOP_ASGN arg",
    "arg : backref tOP_ASGN arg",
    "arg : arg tDOT2 arg",
    "arg : arg tDOT3 arg",
    "arg : arg tPLUS arg",
    "arg : arg tMINUS arg",
    "arg : arg tSTAR2 arg",
    "arg : arg tDIVIDE arg",
    "arg : arg tPERCENT arg",
    "arg : arg tPOW arg",
    "arg : tUMINUS_NUM tINTEGER tPOW arg",
    "arg : tUMINUS_NUM tFLOAT tPOW arg",
    "arg : tUPLUS arg",
    "arg : tUMINUS arg",
    "arg : arg tPIPE arg",
    "arg : arg tCARET arg",
    "arg : arg tAMPER2 arg",
    "arg : arg tCMP arg",
    "arg : arg tGT arg",
    "arg : arg tGEQ arg",
    "arg : arg tLT arg",
    "arg : arg tLEQ arg",
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
    "arg : kDEFINED opt_nl arg",
    "arg : arg '?' arg opt_nl ':' arg",
    "arg : primary",
    "arg_value : arg",
    "aref_args : none",
    "aref_args : args trailer",
    "aref_args : args ',' assocs trailer",
    "aref_args : assocs trailer",
    "paren_args : tLPAREN2 opt_call_args rparen",
    "opt_paren_args : none",
    "opt_paren_args : paren_args",
    "opt_call_args : none",
    "opt_call_args : call_args",
    "call_args : command",
    "call_args : args opt_block_arg",
    "call_args : assocs opt_block_arg",
    "call_args : args ',' assocs opt_block_arg",
    "call_args : block_arg",
    "$$6 :",
    "command_args : $$6 call_args",
    "block_arg : tAMPER arg_value",
    "opt_block_arg : ',' block_arg",
    "opt_block_arg : ','",
    "opt_block_arg : none_block_pass",
    "args : arg_value",
    "args : tSTAR arg_value",
    "args : args ',' arg_value",
    "args : args ',' tSTAR arg_value",
    "mrhs : args ',' arg_value",
    "mrhs : args ',' tSTAR arg_value",
    "mrhs : tSTAR arg_value",
    "primary : literal",
    "primary : strings",
    "primary : xstring",
    "primary : regexp",
    "primary : words",
    "primary : qwords",
    "primary : var_ref",
    "primary : backref",
    "primary : tFID",
    "primary : kBEGIN bodystmt kEND",
    "$$7 :",
    "primary : tLPAREN_ARG expr $$7 rparen",
    "primary : tLPAREN compstmt tRPAREN",
    "primary : primary_value tCOLON2 tCONSTANT",
    "primary : tCOLON3 tCONSTANT",
    "primary : tLBRACK aref_args tRBRACK",
    "primary : tLBRACE assoc_list tRCURLY",
    "primary : kRETURN",
    "primary : kYIELD tLPAREN2 call_args rparen",
    "primary : kYIELD tLPAREN2 rparen",
    "primary : kYIELD",
    "primary : kDEFINED opt_nl tLPAREN2 expr rparen",
    "primary : kNOT tLPAREN2 expr rparen",
    "primary : kNOT tLPAREN2 rparen",
    "primary : operation brace_block",
    "primary : method_call",
    "primary : method_call brace_block",
    "primary : tLAMBDA lambda",
    "primary : kIF expr_value then compstmt if_tail kEND",
    "primary : kUNLESS expr_value then compstmt opt_else kEND",
    "$$8 :",
    "$$9 :",
    "primary : kWHILE $$8 expr_value do $$9 compstmt kEND",
    "$$10 :",
    "$$11 :",
    "primary : kUNTIL $$10 expr_value do $$11 compstmt kEND",
    "primary : kCASE expr_value opt_terms case_body kEND",
    "primary : kCASE opt_terms case_body kEND",
    "$$12 :",
    "$$13 :",
    "primary : kFOR for_var kIN $$12 expr_value do $$13 compstmt kEND",
    "$$14 :",
    "primary : kCLASS cpath superclass $$14 bodystmt kEND",
    "$$15 :",
    "$$16 :",
    "primary : kCLASS tLSHFT expr $$15 term $$16 bodystmt kEND",
    "$$17 :",
    "primary : kMODULE cpath $$17 bodystmt kEND",
    "$$18 :",
    "primary : kDEF fname $$18 f_arglist bodystmt kEND",
    "$$19 :",
    "$$20 :",
    "primary : kDEF singleton dot_or_colon $$19 fname $$20 f_arglist bodystmt kEND",
    "primary : kBREAK",
    "primary : kNEXT",
    "primary : kREDO",
    "primary : kRETRY",
    "primary_value : primary",
    "then : term",
    "then : kTHEN",
    "then : term kTHEN",
    "do : term",
    "do : kDO_COND",
    "if_tail : opt_else",
    "if_tail : kELSIF expr_value then compstmt if_tail",
    "opt_else : none",
    "opt_else : kELSE compstmt",
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
    "block_param : f_arg ',' f_block_optarg ',' f_rest_arg opt_f_block_arg",
    "block_param : f_arg ',' f_block_optarg ',' f_rest_arg ',' f_arg opt_f_block_arg",
    "block_param : f_arg ',' f_block_optarg opt_f_block_arg",
    "block_param : f_arg ',' f_block_optarg ',' f_arg opt_f_block_arg",
    "block_param : f_arg ',' f_rest_arg opt_f_block_arg",
    "block_param : f_arg ','",
    "block_param : f_arg ',' f_rest_arg ',' f_arg opt_f_block_arg",
    "block_param : f_arg opt_f_block_arg",
    "block_param : f_block_optarg ',' f_rest_arg opt_f_block_arg",
    "block_param : f_block_optarg ',' f_rest_arg ',' f_arg opt_f_block_arg",
    "block_param : f_block_optarg opt_f_block_arg",
    "block_param : f_block_optarg ',' f_arg opt_f_block_arg",
    "block_param : f_rest_arg opt_f_block_arg",
    "block_param : f_rest_arg ',' f_arg opt_f_block_arg",
    "block_param : f_block_arg",
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
    "$$21 :",
    "lambda : $$21 f_larglist lambda_body",
    "f_larglist : tLPAREN2 f_args opt_bv_decl tRPAREN",
    "f_larglist : f_args opt_bv_decl",
    "lambda_body : tLAMBEG compstmt tRCURLY",
    "lambda_body : kDO_LAMBDA compstmt kEND",
    "$$22 :",
    "do_block : kDO_BLOCK $$22 opt_block_param compstmt kEND",
    "block_call : command do_block",
    "block_call : block_call tDOT operation2 opt_paren_args",
    "block_call : block_call tCOLON2 operation2 opt_paren_args",
    "method_call : operation paren_args",
    "method_call : primary_value tDOT operation2 opt_paren_args",
    "method_call : primary_value tCOLON2 operation2 paren_args",
    "method_call : primary_value tCOLON2 operation3",
    "method_call : primary_value tDOT paren_args",
    "method_call : primary_value tCOLON2 paren_args",
    "method_call : kSUPER paren_args",
    "method_call : kSUPER",
    "method_call : primary_value '[' opt_call_args rbracket",
    "$$23 :",
    "brace_block : tLCURLY $$23 opt_block_param compstmt tRCURLY",
    "$$24 :",
    "brace_block : kDO $$24 opt_block_param compstmt kEND",
    "case_body : kWHEN args then compstmt cases",
    "cases : opt_else",
    "cases : case_body",
    "opt_rescue : kRESCUE exc_list exc_var then compstmt opt_rescue",
    "opt_rescue :",
    "exc_list : arg_value",
    "exc_list : mrhs",
    "exc_list : none",
    "exc_var : tASSOC lhs",
    "exc_var : none",
    "opt_ensure : kENSURE compstmt",
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
    "regexp : tREGEXP_BEG xstring_contents tREGEXP_END",
    "words : tWORDS_BEG ' ' tSTRING_END",
    "words : tWORDS_BEG word_list tSTRING_END",
    "word_list :",
    "word_list : word_list word ' '",
    "word : string_content",
    "word : word string_content",
    "qwords : tQWORDS_BEG ' ' tSTRING_END",
    "qwords : tQWORDS_BEG qword_list tSTRING_END",
    "qword_list :",
    "qword_list : qword_list tSTRING_CONTENT ' '",
    "string_contents :",
    "string_contents : string_contents string_content",
    "xstring_contents :",
    "xstring_contents : xstring_contents string_content",
    "string_content : tSTRING_CONTENT",
    "$$25 :",
    "string_content : tSTRING_DVAR $$25 string_dvar",
    "$$26 :",
    "string_content : tSTRING_DBEG $$26 compstmt tRCURLY",
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
    "numeric : tINTEGER",
    "numeric : tFLOAT",
    "numeric : tUMINUS_NUM tINTEGER",
    "numeric : tUMINUS_NUM tFLOAT",
    "variable : tIDENTIFIER",
    "variable : tIVAR",
    "variable : tGVAR",
    "variable : tCONSTANT",
    "variable : tCVAR",
    "variable : kNIL",
    "variable : kSELF",
    "variable : kTRUE",
    "variable : kFALSE",
    "variable : k__FILE__",
    "variable : k__LINE__",
    "variable : k__ENCODING__",
    "var_ref : variable",
    "var_lhs : variable",
    "backref : tNTH_REF",
    "backref : tBACK_REF",
    "superclass : term",
    "$$27 :",
    "superclass : tLT $$27 expr_value term",
    "superclass : error term",
    "f_arglist : tLPAREN2 f_args rparen",
    "f_arglist : f_args term",
    "f_args : f_arg ',' f_optarg ',' f_rest_arg opt_f_block_arg",
    "f_args : f_arg ',' f_optarg ',' f_rest_arg ',' f_arg opt_f_block_arg",
    "f_args : f_arg ',' f_optarg opt_f_block_arg",
    "f_args : f_arg ',' f_optarg ',' f_arg opt_f_block_arg",
    "f_args : f_arg ',' f_rest_arg opt_f_block_arg",
    "f_args : f_arg ',' f_rest_arg ',' f_arg opt_f_block_arg",
    "f_args : f_arg opt_f_block_arg",
    "f_args : f_optarg ',' f_rest_arg opt_f_block_arg",
    "f_args : f_optarg ',' f_rest_arg ',' f_arg opt_f_block_arg",
    "f_args : f_optarg opt_f_block_arg",
    "f_args : f_optarg ',' f_arg opt_f_block_arg",
    "f_args : f_rest_arg opt_f_block_arg",
    "f_args : f_rest_arg ',' f_arg opt_f_block_arg",
    "f_args : f_block_arg",
    "f_args :",
    "f_bad_arg : tCONSTANT",
    "f_bad_arg : tIVAR",
    "f_bad_arg : tGVAR",
    "f_bad_arg : tCVAR",
    "f_norm_arg : f_bad_arg",
    "f_norm_arg : tIDENTIFIER",
    "f_arg_item : f_norm_arg",
    "f_arg_item : tLPAREN f_margs rparen",
    "f_arg : f_arg_item",
    "f_arg : f_arg ',' f_arg_item",
    "f_opt : tIDENTIFIER '=' arg_value",
    "f_block_opt : tIDENTIFIER '=' primary_value",
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
    "$$28 :",
    "singleton : tLPAREN2 $$28 expr rparen",
    "assoc_list : none",
    "assoc_list : assocs trailer",
    "assocs : assoc",
    "assocs : assocs ',' assoc",
    "assoc : arg_value tASSOC arg_value",
    "assoc : tLABEL arg_value",
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

  /** debugging support, requires the package <tt>jay.yydebug</tt>.
      Set to <tt>null</tt> to suppress debugging messages.
    */
  protected jay.yydebug.yyDebug yydebug;

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
      @param yydebug debug message writer implementing <tt>yyDebug</tt>, or <tt>null</tt>.
      @return result of the last reduction, if any.
    */
  public Object yyparse (RubyYaccLexer yyLex, Object ayydebug)
				throws java.io.IOException {
    this.yydebug = (jay.yydebug.yyDebug)ayydebug;
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
  public Object yyparse (RubyYaccLexer yyLex) throws java.io.IOException {
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
              support.yyerror("syntax error", yyExpecting(yyState), yyNames[yyToken]);
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
              support.yyerror("irrecoverable syntax error");
  
            case 3:
              if (yyToken == 0) {
                if (yydebug != null) yydebug.reject();
                support.yyerror("irrecoverable syntax error at end-of-file");
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
        ParserState state = states[yyN];
        if (state == null) {
            yyVal = yyDefault(yyV > yyTop ? null : yyVals[yyV]);
        } else {
            yyVal = state.execute(support, lexer, yyVal, yyVals, yyTop);
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

static ParserState[] states = new ParserState[553];
static {
states[502] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.arg_var(support.formal_argument(((IRubyObject)yyVals[-2+yyTop])));
                    yyVal = new OptArgNode(((IRubyObject)yyVals[-2+yyTop]).getPosition(), support.assignable(((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[435] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.literal_concat(support.getPosition(((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[368] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((Token)yyVals[0+yyTop]).getPosition(), null, null, null, null, null);
    return yyVal;
  }
};
states[33] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_field", ((IRubyObject)yyVals[-4+yyTop]), ".", ((IRubyObject)yyVals[-2+yyTop]));
                    yyVal = support.dispatch("on_opassign", yyVal, ((Token)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[234] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.string(">>"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[100] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_aref_field", ((IRubyObject)yyVals[-3+yyTop]), support.escape(((IRubyObject)yyVals[-1+yyTop])));
    return yyVal;
  }
};
states[301] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.getConditionState().begin();
    return yyVal;
  }
};
states[536] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[469] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[201] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ((IRubyObject)yyVals[-4+yyTop]) = support.dispatch("on_field", ((IRubyObject)yyVals[-4+yyTop]), support.symbol('.'), ((IRubyObject)yyVals[-2+yyTop]));
                    yyVal = support.dispatch("on_opassign", ((IRubyObject)yyVals[-4+yyTop]), ((Token)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[67] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_abreak", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[503] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.arg_var(support.formal_argument(((IRubyObject)yyVals[-2+yyTop])));
                    yyVal = new OptArgNode(((IRubyObject)yyVals[-2+yyTop]).getPosition(), support.assignable(((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[436] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[369] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-2+yyTop]);
    return yyVal;
  }
};
states[34] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_field", ((IRubyObject)yyVals[-4+yyTop]), ".", ((IRubyObject)yyVals[-2+yyTop]));
                    yyVal = support.dispatch("on_opassign", yyVal, ((Token)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[235] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.string("&&"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[101] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_field", ((IRubyObject)yyVals[-2+yyTop]), support.symbol('.'), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[302] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.getConditionState().end();
    return yyVal;
  }
};
states[537] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[470] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[403] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node node;
                    if (((IRubyObject)yyVals[-3+yyTop]) != null) {
                        node = support.appendToBlock(support.node_assign(((IRubyObject)yyVals[-3+yyTop]), new GlobalVarNode(((IRubyObject)yyVals[-5+yyTop]).getPosition(), "$!")), ((IRubyObject)yyVals[-1+yyTop]));
                        if (((IRubyObject)yyVals[-1+yyTop]) != null) {
                            node.setPosition(support.unwrapNewlineNode(((IRubyObject)yyVals[-1+yyTop])).getPosition());
                        }
                    } else {
                        node = ((IRubyObject)yyVals[-1+yyTop]);
                    }
                    Node body = node == null ? NilImplicitNode.NIL : node;
                    yyVal = new RescueBodyNode(((IRubyObject)yyVals[-5+yyTop]).getPosition(), ((IRubyObject)yyVals[-4+yyTop]), body, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[336] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
    return yyVal;
  }
};
states[202] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ((IRubyObject)yyVals[-4+yyTop]) = support.dispatch("on_field", ((IRubyObject)yyVals[-4+yyTop]), support.string("::"), ((IRubyObject)yyVals[-2+yyTop]));
                    yyVal = support.dispatch("on_opassign", ((IRubyObject)yyVals[-4+yyTop]), ((Token)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[68] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_next", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[1] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.setState(LexState.EXPR_BEG);
    return yyVal;
  }
};
states[504] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BlockNode(((IRubyObject)yyVals[0+yyTop]).getPosition()).add(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[437] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = lexer.getStrTerm();
                    lexer.setStrTerm(null);
                    lexer.setState(LexState.EXPR_BEG);
    return yyVal;
  }
};
states[370] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[35] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_const_path_field", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop]));
                    yyVal = support.dispatch("on_opassign", yyVal, ((Token)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
                    yyVal = support.dispatch("on_assign_error", yyVal);
    return yyVal;
  }
};
states[236] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.string("||"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[102] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_field", ((IRubyObject)yyVals[-2+yyTop]), support.string("::"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[303] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node body = ((IRubyObject)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((IRubyObject)yyVals[-1+yyTop]);
                    yyVal = new UntilNode(((IRubyObject)yyVals[-6+yyTop]).getPosition(), support.getConditionNode(((IRubyObject)yyVals[-4+yyTop])), body);
    return yyVal;
  }
};
states[471] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[404] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null; 
    return yyVal;
  }
};
states[337] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = support.assignable(((IRubyObject)yyVals[0+yyTop]), NilImplicitNode.NIL);
    return yyVal;
  }
};
states[2] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  support.setResult(support.dispatch("on_program", ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[203] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_const_path_field", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop]));
                    yyVal = support.dispatch("on_opassign", yyVal, ((Token)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
                    yyVal = support.dispatch("on_assign_error", yyVal);
    return yyVal;
  }
};
states[505] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.appendToBlock(((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[438] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setStrTerm(((StrTerm)yyVals[-1+yyTop]));
                    yyVal = new EvStrNode(((Token)yyVals[-2+yyTop]).getPosition(), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[371] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[36] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_field", ((IRubyObject)yyVals[-4+yyTop]), "::", ((IRubyObject)yyVals[-2+yyTop]));
                    yyVal = support.dispatch("on_opassign", yyVal, ((Token)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[237] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_defined", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[103] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_field", ((IRubyObject)yyVals[-2+yyTop]), support.symbol('.'), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[304] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newCaseNode(((IRubyObject)yyVals[-4+yyTop]).getPosition(), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[472] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.setState(LexState.EXPR_BEG);
    return yyVal;
  }
};
states[405] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(((IRubyObject)yyVals[0+yyTop]).getPosition(), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[338] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[70] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_mlhs_paren", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[3] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((IRubyObject)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[204] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_top_const_field", ((IRubyObject)yyVals[-2+yyTop]));
                    yyVal = support.dispatch("on_opassign", yyVal, ((Token)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
                    yyVal = support.dispatch("on_assign_error", yyVal);
    return yyVal;
  }
};
states[506] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BlockNode(((IRubyObject)yyVals[0+yyTop]).getPosition()).add(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[439] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = lexer.getStrTerm();
                   lexer.getConditionState().stop();
                   lexer.getCmdArgumentState().stop();
                   lexer.setStrTerm(null);
                   lexer.setState(LexState.EXPR_BEG);
    return yyVal;
  }
};
states[372] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[238] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_ifop", ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[104] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_const_path_field", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));

                    if (support.isInDef() || support.isInSingle()) {
                        yyVal = support.dispatch("on_assign_error", yyVal);
                    }
    return yyVal;
  }
};
states[305] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newCaseNode(((IRubyObject)yyVals[-3+yyTop]).getPosition(), null, ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[37] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_assign", 
                                             support.dispatch("on_var_field", ((IRubyObject)yyVals[-2+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
                    yyVal = support.dispatch("on_assign_error", yyVal);
    return yyVal;
  }
};
states[473] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[406] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.splat_array(((IRubyObject)yyVals[0+yyTop]));
                    if (yyVal == null) yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[339] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(((IRubyObject)yyVals[0+yyTop]).getPosition(), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[4] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.dispatch("on_stmts_add", 
                                           support.dispatch("on_stmts_new"), 
                                           support.dispatch("on_void_stmt"));
    return yyVal;
  }
};
states[205] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_var_field", ((IRubyObject)yyVals[-2+yyTop]));
                    yyVal = support.dispatch("on_opassign", yyVal, ((Token)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
                    yyVal = support.dispatch("on_assign_error", yyVal);
    return yyVal;
  }
};
states[507] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.appendToBlock(((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[440] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.getConditionState().restart();
                   lexer.getCmdArgumentState().restart();
                   lexer.setStrTerm(((StrTerm)yyVals[-2+yyTop]));

                   yyVal = support.newEvStrNode(((Token)yyVals[-3+yyTop]).getPosition(), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[373] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[239] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[105] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_top_const_field", ((IRubyObject)yyVals[0+yyTop]));

                    if (support.isInDef() || support.isInSingle()) {
                        yyVal = support.dispatch("on_assign_error", yyVal);
                    }
    return yyVal;
  }
};
states[306] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.getConditionState().begin();
    return yyVal;
  }
};
states[38] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_assign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[474] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = null;
    return yyVal;
  }
};
states[340] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-2+yyTop]).add(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[72] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_mlhs_paren", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[5] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.dispatch("on_stmts_add", 
                                           support.dispatch("on_stmts_new"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[206] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_dot2", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[441] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = new GlobalVarNode(((IRubyObject)yyVals[0+yyTop]).getPosition(), (String) ((IRubyObject)yyVals[0+yyTop]).getValue());
    return yyVal;
  }
};
states[374] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.new_bv(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[106] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_assign_error", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[307] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.getConditionState().end();
    return yyVal;
  }
};
states[39] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_assign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[240] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[542] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[475] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
                    ((ISourcePositionHolder)yyVal).setPosition(((Token)yyVals[-2+yyTop]).getPosition());
                    lexer.setState(LexState.EXPR_BEG);
    return yyVal;
  }
};
states[408] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[341] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((IRubyObject)yyVals[0+yyTop]).getPosition(), ((IRubyObject)yyVals[0+yyTop]), null, null);
    return yyVal;
  }
};
states[73] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[6] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.dispatch("on_stmts_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[207] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_dot3", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[442] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = new InstVarNode(((IRubyObject)yyVals[0+yyTop]).getPosition(), (String) ((IRubyObject)yyVals[0+yyTop]).getValue());
    return yyVal;
  }
};
states[375] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[107] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_class_name_error", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[308] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                      /* ENEBO: Lots of optz in 1.9 parser here*/
                    yyVal = new ForNode(((IRubyObject)yyVals[-8+yyTop]).getPosition(), ((IRubyObject)yyVals[-7+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[-4+yyTop]), support.getCurrentScope());
    return yyVal;
  }
};
states[40] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_massign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[543] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[476] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[342] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((IRubyObject)yyVals[-3+yyTop]).getPosition(), ((IRubyObject)yyVals[-3+yyTop]), support.assignable(((IRubyObject)yyVals[0+yyTop]), null), null);
    return yyVal;
  }
};
states[7] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.remove_begin(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[208] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.symbol('+'), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[74] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.mlhs_add(((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[510] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.arg_var(support.shadowing_lvar(((IRubyObject)yyVals[0+yyTop])));
                    yyVal = support.dispatch("on_rest_param", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[443] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = new ClassVarNode(((IRubyObject)yyVals[0+yyTop]).getPosition(), (String) ((IRubyObject)yyVals[0+yyTop]).getValue());
    return yyVal;
  }
};
states[376] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
                    yyVal = lexer.getLeftParenBegin();
                    lexer.setLeftParenBegin(lexer.incrementParenNest());
    return yyVal;
  }
};
states[309] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("class definition in method body");
                    }
                    support.pushLocalScope();
    return yyVal;
  }
};
states[242] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[477] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-5+yyTop]).getPosition(), ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[410] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[343] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((IRubyObject)yyVals[-5+yyTop]).getPosition(), ((IRubyObject)yyVals[-5+yyTop]), support.assignable(((IRubyObject)yyVals[-2+yyTop]), null), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[276] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new FCallNoArgNode(((IRubyObject)yyVals[0+yyTop]).getPosition(), (String) ((IRubyObject)yyVals[0+yyTop]).getValue());
    return yyVal;
  }
};
states[209] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.symbol('-'), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[75] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.mlhs_add_star(((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[511] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_rest_param", null);
    return yyVal;
  }
};
states[377] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new LambdaNode(((IRubyObject)yyVals[-1+yyTop]).getPosition(), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
                    lexer.setLeftParenBegin(((Integer)yyVals[-2+yyTop]));
    return yyVal;
  }
};
states[109] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_top_const_ref", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[310] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node body = ((IRubyObject)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((IRubyObject)yyVals[-1+yyTop]);

                    yyVal = new ClassNode(((IRubyObject)yyVals[-5+yyTop]).getPosition(), ((Colon3Node)yyVals[-4+yyTop]), support.getCurrentScope(), body, ((IRubyObject)yyVals[-3+yyTop]));
                    support.popCurrentScope();
    return yyVal;
  }
};
states[42] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_assign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[243] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.arg_append(((IRubyObject)yyVals[-3+yyTop]), new Hash19Node(lexer.getPosition(), ((IRubyObject)yyVals[-1+yyTop])));
    return yyVal;
  }
};
states[478] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-7+yyTop]).getPosition(), ((IRubyObject)yyVals[-7+yyTop]), ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[344] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((IRubyObject)yyVals[-2+yyTop]).getPosition(), ((IRubyObject)yyVals[-2+yyTop]), new StarNode(lexer.getPosition()), null);
    return yyVal;
  }
};
states[9] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  if (support.isInDef() || support.isInSingle()) {
                      support.yyerror("BEGIN in method");
                  }
    return yyVal;
  }
};
states[210] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.symbol('*'), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[76] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.mlhs_add(support.mlhs_add_star(((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[277] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BeginNode(support.getPosition(((IRubyObject)yyVals[-2+yyTop])), ((IRubyObject)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[445] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     lexer.setState(LexState.EXPR_END);
                     yyVal = ((IRubyObject)yyVals[0+yyTop]);
                     ((ISourcePositionHolder)yyVal).setPosition(((Token)yyVals[-1+yyTop]).getPosition());
    return yyVal;
  }
};
states[378] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-2+yyTop]);
                    ((ISourcePositionHolder)yyVal).setPosition(((Token)yyVals[-3+yyTop]).getPosition());
    return yyVal;
  }
};
states[110] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_const_ref", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[311] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = Boolean.valueOf(support.isInDef());
                    support.setInDef(false);
    return yyVal;
  }
};
states[43] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_assign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[244] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(((IRubyObject)yyVals[-1+yyTop]).getPosition(), new Hash19Node(lexer.getPosition(), ((IRubyObject)yyVals[-1+yyTop])));
    return yyVal;
  }
};
states[479] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-3+yyTop]).getPosition(), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[345] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((IRubyObject)yyVals[-4+yyTop]).getPosition(), ((IRubyObject)yyVals[-4+yyTop]), new StarNode(lexer.getPosition()), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[10] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.dispatch("on_BEGIN", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[211] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.symbol('/'), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[77] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.mlhs_add_star(((IRubyObject)yyVals[-1+yyTop]), Qnil);
    return yyVal;
  }
};
states[278] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setState(LexState.EXPR_ENDARG); 
    return yyVal;
  }
};
states[379] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[312] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = Integer.valueOf(support.getInSingle());
                    support.setInSingle(0);
                    support.pushLocalScope();
    return yyVal;
  }
};
states[245] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
                    if (yyVal != null) ((Node)yyVal).setPosition(((Token)yyVals[-2+yyTop]).getPosition());
    return yyVal;
  }
};
states[111] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_const_path_ref", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[480] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-5+yyTop]).getPosition(), ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), null, ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[413] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    /* FIXME: We may be intern'ing more than once.*/
                    yyVal = new SymbolNode(((IRubyObject)yyVals[0+yyTop]).getPosition(), ((String) ((IRubyObject)yyVals[0+yyTop]).getValue()).intern());
    return yyVal;
  }
};
states[346] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((Token)yyVals[-1+yyTop]).getPosition(), null, support.assignable(((IRubyObject)yyVals[0+yyTop]), null), null);
    return yyVal;
  }
};
states[11] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.dispatch("on_bodystmt", support.escape(((IRubyObject)yyVals[-3+yyTop])), support.escape(((IRubyObject)yyVals[-2+yyTop])),
                                support.escape(((IRubyObject)yyVals[-1+yyTop])), support.escape(((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[212] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.symbol('%'), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[78] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ((IRubyObject)yyVals[-3+yyTop]) = support.mlhs_add_star(((IRubyObject)yyVals[-3+yyTop]), Qnil);
                    yyVal = support.mlhs_add(((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[279] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.warning(ID.GROUPED_EXPRESSION, ((Token)yyVals[-3+yyTop]).getPosition(), "(...) interpreted as grouped expression");
                    yyVal = ((IRubyObject)yyVals[-2+yyTop]);
    return yyVal;
  }
};
states[514] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.arg_var(support.shadowing_lvar(((IRubyObject)yyVals[0+yyTop])));
                    yyVal = support.dispatch("on_blockarg", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[380] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[313] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new SClassNode(((IRubyObject)yyVals[-7+yyTop]).getPosition(), ((IRubyObject)yyVals[-5+yyTop]), support.getCurrentScope(), ((IRubyObject)yyVals[-1+yyTop]));
                    support.popCurrentScope();
                    support.setInDef(((Boolean)yyVals[-4+yyTop]).booleanValue());
                    support.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
    return yyVal;
  }
};
states[45] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), "and", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[481] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-3+yyTop]).getPosition(), ((IRubyObject)yyVals[-3+yyTop]), null, ((IRubyObject)yyVals[-1+yyTop]), null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[347] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((Token)yyVals[-3+yyTop]).getPosition(), null, support.assignable(((IRubyObject)yyVals[-2+yyTop]), null), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[12] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[213] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.string("**"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[79] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.mlhs_add_star(support.mlhs_new(), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[280] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (((IRubyObject)yyVals[-1+yyTop]) != null) {
                        /* compstmt position includes both parens around it*/
                        ((ISourcePositionHolder) ((IRubyObject)yyVals[-1+yyTop])).setPosition(((Token)yyVals[-2+yyTop]).getPosition());
                        yyVal = ((IRubyObject)yyVals[-1+yyTop]);
                    } else {
                        yyVal = new NilNode(((Token)yyVals[-2+yyTop]).getPosition());
                    }
    return yyVal;
  }
};
states[515] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[381] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[46] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), "or", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[314] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) { 
                        support.yyerror("module definition in method body");
                    }
                    support.pushLocalScope();
    return yyVal;
  }
};
states[482] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-5+yyTop]).getPosition(), ((IRubyObject)yyVals[-5+yyTop]), null, ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[415] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]) instanceof EvStrNode ? new DStrNode(((IRubyObject)yyVals[0+yyTop]).getPosition(), lexer.getEncoding()).add(((IRubyObject)yyVals[0+yyTop])) : ((IRubyObject)yyVals[0+yyTop]);
                    /*
                    NODE *node = $1;
                    if (!node) {
                        node = NEW_STR(STR_NEW0());
                    } else {
                        node = evstr2dstr(node);
                    }
                    $$ = node;
                    */
    return yyVal;
  }
};
states[348] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((Token)yyVals[0+yyTop]).getPosition(), null, new StarNode(lexer.getPosition()), null);
    return yyVal;
  }
};
states[13] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_stmts_add", 
                                          support.dispatch("on_stmts_new"),
                                          support.dispatch("on_void_stmt"));
    return yyVal;
  }
};
states[214] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.string("**"), ((IRubyObject)yyVals[0+yyTop]));
                    yyVal = support.dispatch("on_unary", support.string("-@"), yyVal);
    return yyVal;
  }
};
states[80] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.mlhs_add(support.mlhs_add_star(support.mlhs_new(), ((IRubyObject)yyVals[-2+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[281] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_colon2(support.getPosition(((IRubyObject)yyVals[-2+yyTop])), ((IRubyObject)yyVals[-2+yyTop]), (String) ((IRubyObject)yyVals[0+yyTop]).getValue());
    return yyVal;
  }
};
states[516] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[382] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
    return yyVal;
  }
};
states[47] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_unary", ripper_intern("not"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[315] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node body = ((IRubyObject)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((IRubyObject)yyVals[-1+yyTop]);

                    yyVal = new ModuleNode(((IRubyObject)yyVals[-4+yyTop]).getPosition(), ((Colon3Node)yyVals[-3+yyTop]), support.getCurrentScope(), body);
                    support.popCurrentScope();
    return yyVal;
  }
};
states[483] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-1+yyTop]).getPosition(), ((IRubyObject)yyVals[-1+yyTop]), null, null, null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[416] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ByteList aChar = ByteList.create((String) ((IRubyObject)yyVals[0+yyTop]).getValue());
                    aChar.setEncoding(lexer.getEncoding());
                    yyVal = lexer.createStrNode(((Token)yyVals[-1+yyTop]).getPosition(), aChar, 0);
    return yyVal;
  }
};
states[349] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((Token)yyVals[-2+yyTop]).getPosition(), null, null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[14] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_stmts_add",
                                          ripper.dispatch("on_stmts_new"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[215] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.string("**"), ((IRubyObject)yyVals[0+yyTop]));
                    yyVal = support.dispatch("on_unary", support.string("-@"), yyVal);
    return yyVal;
  }
};
states[81] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.mlhs_add_star(support.mlhs_new(), Qnil);
    return yyVal;
  }
};
states[282] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_colon3(((Token)yyVals[-1+yyTop]).getPosition(), (String) ((IRubyObject)yyVals[0+yyTop]).getValue());
    return yyVal;
  }
};
states[517] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[450] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     lexer.setState(LexState.EXPR_END);

                     /* DStrNode: :"some text #{some expression}"*/
                     /* StrNode: :"some text"*/
                     /* EvStrNode :"#{some expression}"*/
                     /* Ruby 1.9 allows empty strings as symbols*/
                     if (((IRubyObject)yyVals[-1+yyTop]) == null) {
                         yyVal = new SymbolNode(((Token)yyVals[-2+yyTop]).getPosition(), "");
                     } else if (((IRubyObject)yyVals[-1+yyTop]) instanceof DStrNode) {
                         yyVal = new DSymbolNode(((Token)yyVals[-2+yyTop]).getPosition(), ((DStrNode)yyVals[-1+yyTop]));
                     } else if (((IRubyObject)yyVals[-1+yyTop]) instanceof StrNode) {
                         yyVal = new SymbolNode(((Token)yyVals[-2+yyTop]).getPosition(), ((StrNode)yyVals[-1+yyTop]).getValue().toString().intern());
                     } else {
                         yyVal = new DSymbolNode(((Token)yyVals[-2+yyTop]).getPosition());
                         ((DSymbolNode)yyVal).add(((IRubyObject)yyVals[-1+yyTop]));
                     }
    return yyVal;
  }
};
states[383] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IterNode(support.getPosition(((IRubyObject)yyVals[-4+yyTop])), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
    return yyVal;
  }
};
states[48] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_unary", ripper_id2sym("!"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[115] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.setState(LexState.EXPR_ENDFN);
                   yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[316] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.setInDef(true);
                    support.pushLocalScope();
    return yyVal;
  }
};
states[551] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                      yyVal = null;
    return yyVal;
  }
};
states[484] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-3+yyTop]).getPosition(), null, ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[417] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[350] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-5+yyTop]).getPosition(), ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[15] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_stmts_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[216] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_unary", support.string("+@"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[82] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.mlhs_add(support.mlhs_add_star(support.mlhs_new(), Qnil), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[283] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ISourcePosition position = ((Token)yyVals[-2+yyTop]).getPosition();
                    if (((IRubyObject)yyVals[-1+yyTop]) == null) {
                        yyVal = new ZArrayNode(position); /* zero length array */
                    } else {
                        yyVal = ((IRubyObject)yyVals[-1+yyTop]);
                        ((ISourcePositionHolder)yyVal).setPosition(position);
                    }
    return yyVal;
  }
};
states[518] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setState(LexState.EXPR_BEG);
    return yyVal;
  }
};
states[451] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[384] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    /* Workaround for JRUBY-2326 (MRI does not enter this production for some reason)*/
                    if (((IRubyObject)yyVals[-1+yyTop]) instanceof YieldNode) {
                        throw new SyntaxException(PID.BLOCK_GIVEN_TO_YIELD, ((IRubyObject)yyVals[-1+yyTop]).getPosition(), lexer.getCurrentLine(), "block given to yield");
                    }
                    if (((BlockAcceptingNode)yyVals[-1+yyTop]).getIterNode() instanceof BlockPassNode) {
                        throw new SyntaxException(PID.BLOCK_ARG_AND_BLOCK_GIVEN, ((IRubyObject)yyVals[-1+yyTop]).getPosition(), lexer.getCurrentLine(), "Both block arg and actual block given.");
                    }
                    yyVal = ((BlockAcceptingNode)yyVals[-1+yyTop]).setIterNode(((IRubyObject)yyVals[0+yyTop]));
                    ((Node)yyVal).setPosition(((IRubyObject)yyVals[-1+yyTop]).getPosition());
    return yyVal;
  }
};
states[250] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(support.getPosition(((IRubyObject)yyVals[0+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[116] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.setState(LexState.EXPR_ENDFN);
                   yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[317] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    /* TODO: We should use implicit nil for body, but problem (punt til later)*/
                    Node body = ((IRubyObject)yyVals[-1+yyTop]); /*$5 == null ? NilImplicitNode.NIL : $5;*/

                    yyVal = new DefnNode(((IRubyObject)yyVals[-5+yyTop]).getPosition(), new ArgumentNode(((IRubyObject)yyVals[-4+yyTop]).getPosition(), (String) ((IRubyObject)yyVals[-4+yyTop]).getValue()), ((IRubyObject)yyVals[-2+yyTop]), support.getCurrentScope(), body);
                    support.popCurrentScope();
                    support.setInDef(false);
    return yyVal;
  }
};
states[552] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = null;
    return yyVal;
  }
};
states[485] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-5+yyTop]).getPosition(), null, ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[418] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.literal_concat(((IRubyObject)yyVals[-1+yyTop]).getPosition(), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[351] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-7+yyTop]).getPosition(), ((IRubyObject)yyVals[-7+yyTop]), ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[16] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = remove_begin(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[217] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_unary", support.string("-@"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[284] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Hash19Node(((Token)yyVals[-2+yyTop]).getPosition(), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[519] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_paren", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[452] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[385] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]), null);
    return yyVal;
  }
};
states[50] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[251] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.arg_blk_pass(((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[117] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[318] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setState(LexState.EXPR_FNAME);
    return yyVal;
  }
};
states[486] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-1+yyTop]).getPosition(), null, ((IRubyObject)yyVals[-1+yyTop]), null, null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[419] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);

                    ((ISourcePositionHolder)yyVal).setPosition(((Token)yyVals[-2+yyTop]).getPosition());
                    int extraLength = ((String) ((Token)yyVals[-2+yyTop]).getValue()).length() - 1;

                    /* We may need to subtract addition offset off of first */
                    /* string fragment (we optimistically take one off in*/
                    /* ParserSupport.literal_concat).  Check token length*/
                    /* and subtract as neeeded.*/
                    if ((((IRubyObject)yyVals[-1+yyTop]) instanceof DStrNode) && extraLength > 0) {
                      Node strNode = ((DStrNode)((IRubyObject)yyVals[-1+yyTop])).get(0);
                    }
    return yyVal;
  }
};
states[352] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-3+yyTop]).getPosition(), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[17] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setState(LexState.EXPR_FNAME);
    return yyVal;
  }
};
states[218] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.symbol('|'), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[84] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_mlhs_paren", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[285] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ReturnNode(((IRubyObject)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
    return yyVal;
  }
};
states[453] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = support.negateInteger(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[386] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]), null);
    return yyVal;
  }
};
states[252] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(((IRubyObject)yyVals[-1+yyTop]).getPosition(), new Hash19Node(lexer.getPosition(), ((IRubyObject)yyVals[-1+yyTop])));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[118] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[319] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.setInSingle(support.getInSingle() + 1);
                    support.pushLocalScope();
                    lexer.setState(LexState.EXPR_ENDFN); /* force for args */
    return yyVal;
  }
};
states[487] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-3+yyTop]).getPosition(), null, ((IRubyObject)yyVals[-3+yyTop]), null, ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[420] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ISourcePosition position = ((Token)yyVals[-2+yyTop]).getPosition();

                    if (((IRubyObject)yyVals[-1+yyTop]) == null) {
                        yyVal = new XStrNode(position, null);
                    } else if (((IRubyObject)yyVals[-1+yyTop]) instanceof StrNode) {
                        yyVal = new XStrNode(position, (ByteList) ((StrNode)yyVals[-1+yyTop]).getValue().clone());
                    } else if (((IRubyObject)yyVals[-1+yyTop]) instanceof DStrNode) {
                        yyVal = new DXStrNode(position, ((DStrNode)yyVals[-1+yyTop]));

                        ((Node)yyVal).setPosition(position);
                    } else {
                        yyVal = new DXStrNode(position).add(((IRubyObject)yyVals[-1+yyTop]));
                    }
    return yyVal;
  }
};
states[353] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-5+yyTop]).getPosition(), ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), null, ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[219] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.symbol('^'), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[85] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.mlhs_add(support.mlhs_new(), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[286] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_yield(((IRubyObject)yyVals[-3+yyTop]).getPosition(), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[18] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_alias", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[521] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_assoclist_from_args", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[454] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = support.negateFloat(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[387] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_fcall(((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]), null);
    return yyVal;
  }
};
states[253] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.arg_append(((IRubyObject)yyVals[-3+yyTop]), new Hash19Node(lexer.getPosition(), ((IRubyObject)yyVals[-1+yyTop])));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[119] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.dispatch("on_symbol_literal", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[320] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    /* TODO: We should use implicit nil for body, but problem (punt til later)*/
                    Node body = ((IRubyObject)yyVals[-1+yyTop]); /*$8 == null ? NilImplicitNode.NIL : $8;*/

                    yyVal = new DefsNode(((IRubyObject)yyVals[-8+yyTop]).getPosition(), ((IRubyObject)yyVals[-7+yyTop]), new ArgumentNode(((IRubyObject)yyVals[-4+yyTop]).getPosition(), (String) ((IRubyObject)yyVals[-4+yyTop]).getValue()), ((IRubyObject)yyVals[-2+yyTop]), support.getCurrentScope(), body);
                    support.popCurrentScope();
                    support.setInSingle(support.getInSingle() - 1);
    return yyVal;
  }
};
states[488] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-1+yyTop]).getPosition(), null, null, ((IRubyObject)yyVals[-1+yyTop]), null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[421] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newRegexpNode(((Token)yyVals[-2+yyTop]).getPosition(), ((IRubyObject)yyVals[-1+yyTop]), (RegexpNode) ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[354] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-3+yyTop]).getPosition(), ((IRubyObject)yyVals[-3+yyTop]), null, ((IRubyObject)yyVals[-1+yyTop]), null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[220] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.symbol('&'), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[86] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.mlhs_add(((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[287] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ZYieldNode(((IRubyObject)yyVals[-2+yyTop]).getPosition());
    return yyVal;
  }
};
states[19] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_var_alias", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[388] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]), null);
    return yyVal;
  }
};
states[254] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
    return yyVal;
  }
};
states[120] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[321] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BreakNode(((IRubyObject)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
    return yyVal;
  }
};
states[489] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-3+yyTop]).getPosition(), null, null, ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[422] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ZArrayNode(((Token)yyVals[-2+yyTop]).getPosition());
    return yyVal;
  }
};
states[355] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    RestArgNode rest = new UnnamedRestArgNode(((IRubyObject)yyVals[-1+yyTop]).getPosition(), null, support.getCurrentScope().addVariable("*"));
                    yyVal = support.new_args(((IRubyObject)yyVals[-1+yyTop]).getPosition(), ((IRubyObject)yyVals[-1+yyTop]), null, rest, null, null);
    return yyVal;
  }
};
states[221] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.string("<=>"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[87] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.mlhs_add(support.mlhs_new(), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[288] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ZYieldNode(((IRubyObject)yyVals[0+yyTop]).getPosition());
    return yyVal;
  }
};
states[20] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_var_alias", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[523] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-2+yyTop]).addAll(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[389] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]), null);
    return yyVal;
  }
};
states[54] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = method_arg(support.dispatch("on_call", ((IRubyObject)yyVals[-3+yyTop]), ".", ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[255] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = Long.valueOf(lexer.getCmdArgumentState().begin());
    return yyVal;
  }
};
states[121] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_array(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[322] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new NextNode(((IRubyObject)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
    return yyVal;
  }
};
states[490] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[0+yyTop]).getPosition(), null, null, null, null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[423] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[356] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-5+yyTop]).getPosition(), ((IRubyObject)yyVals[-5+yyTop]), null, ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[88] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.mlhs_add(((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[289] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new DefinedNode(((IRubyObject)yyVals[-4+yyTop]).getPosition(), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[21] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_alias_error", 
                                          support.dispatch("on_var_alias", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[222] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.symbol('>'), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[524] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_assoc_new", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[390] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]), null, null);
    return yyVal;
  }
};
states[256] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[122] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setState(LexState.EXPR_FNAME);
    return yyVal;
  }
};
states[323] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new RedoNode(((IRubyObject)yyVals[0+yyTop]).getPosition());
    return yyVal;
  }
};
states[55] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = method_arg(support.dispatch("on_call", ((IRubyObject)yyVals[-3+yyTop]), "::", ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[491] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(lexer.getPosition(), null, null, null, null, null);
    return yyVal;
  }
};
states[424] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ArrayNode(lexer.getPosition());
    return yyVal;
  }
};
states[357] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-1+yyTop]).getPosition(), ((IRubyObject)yyVals[-1+yyTop]), null, null, null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[89] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.assignable(((IRubyObject)yyVals[0+yyTop]), null);
    return yyVal;
  }
};
states[290] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(support.getConditionNode(((IRubyObject)yyVals[-1+yyTop])), "!");
    return yyVal;
  }
};
states[22] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_undef", ((RubyArray)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[223] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.stringl("<="), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[525] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_assoc_new", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[391] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((IRubyObject)yyVals[-2+yyTop]), new Token("call", ((IRubyObject)yyVals[-2+yyTop]).getPosition()), ((IRubyObject)yyVals[0+yyTop]), null);
    return yyVal;
  }
};
states[324] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new RetryNode(((IRubyObject)yyVals[0+yyTop]).getPosition());
    return yyVal;
  }
};
states[257] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BlockPassNode(((Token)yyVals[-1+yyTop]).getPosition(), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[123] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((RubyArray)yyVals[-3+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[56] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
    return yyVal;
  }
};
states[492] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("formal argument cannot be a constant");
    return yyVal;
  }
};
states[425] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = ((IRubyObject)yyVals[-2+yyTop]).add(((IRubyObject)yyVals[-1+yyTop]) instanceof EvStrNode ? new DStrNode(((IRubyObject)yyVals[-2+yyTop]).getPosition(), lexer.getEncoding()).add(((IRubyObject)yyVals[-1+yyTop])) : ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[358] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(support.getPosition(((IRubyObject)yyVals[-3+yyTop])), null, ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[90] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.assignable(yyVals[0+yyTop], null);
    return yyVal;
  }
};
states[291] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(NilImplicitNode.NIL, "!");
    return yyVal;
  }
};
states[23] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_if_mod", ((IRubyObject)yyVals[0+yyTop]), ((IRubyObject)yyVals[-2+yyTop]));
    return yyVal;
  }
};
states[224] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.symbol('<'), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[392] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((IRubyObject)yyVals[-2+yyTop]), new Token("call", ((IRubyObject)yyVals[-2+yyTop]).getPosition()), ((IRubyObject)yyVals[0+yyTop]), null);
    return yyVal;
  }
};
states[325] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((IRubyObject)yyVals[0+yyTop]));
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
                    if (yyVal == null) yyVal = NilImplicitNode.NIL;
    return yyVal;
  }
};
states[258] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[57] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_brace_block", support.escape(((IRubyObject)yyVals[-2+yyTop])), ((IRubyObject)yyVals[-1+yyTop]));
                    support.popCurrentScope();
    return yyVal;
  }
};
states[493] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("formal argument cannot be an instance variable");
    return yyVal;
  }
};
states[359] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(support.getPosition(((IRubyObject)yyVals[-5+yyTop])), null, ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[91] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_aref_field", ((IRubyObject)yyVals[-3+yyTop]), support.escape(((IRubyObject)yyVals[-1+yyTop])));
    return yyVal;
  }
};
states[292] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new FCallNoArgBlockNode(((IRubyObject)yyVals[-1+yyTop]).getPosition(), (String) ((IRubyObject)yyVals[-1+yyTop]).getValue(), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[24] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_unless_mod", ((IRubyObject)yyVals[0+yyTop]), ((IRubyObject)yyVals[-2+yyTop]));
    return yyVal;
  }
};
states[225] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.string("<="), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[460] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("nil", Tokens.kNIL, ((IRubyObject)yyVals[0+yyTop]).getPosition());
    return yyVal;
  }
};
states[393] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_super(((IRubyObject)yyVals[0+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[58] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_command", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[259] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[494] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("formal argument cannot be a global variable");
    return yyVal;
  }
};
states[427] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = support.literal_concat(support.getPosition(((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[360] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(support.getPosition(((IRubyObject)yyVals[-1+yyTop])), null, ((IRubyObject)yyVals[-1+yyTop]), null, null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[25] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_while_mod", ((IRubyObject)yyVals[0+yyTop]), ((IRubyObject)yyVals[-2+yyTop]));
    return yyVal;
  }
};
states[226] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.string("=="), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[92] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_field", ((IRubyObject)yyVals[-2+yyTop]), support.symbol('.'), ((IRubyObject)yyVals[0+yyTop]));
                    
    return yyVal;
  }
};
states[461] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("self", Tokens.kSELF, ((IRubyObject)yyVals[0+yyTop]).getPosition());
    return yyVal;
  }
};
states[394] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ZSuperNode(((IRubyObject)yyVals[0+yyTop]).getPosition());
    return yyVal;
  }
};
states[59] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = method_add_block(support.dispatch("on_command", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[495] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("formal argument cannot be a class variable");
    return yyVal;
  }
};
states[428] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = new ZArrayNode(((Token)yyVals[-2+yyTop]).getPosition());
    return yyVal;
  }
};
states[361] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-3+yyTop]).getPosition(), null, ((IRubyObject)yyVals[-3+yyTop]), null, ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[294] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (((IRubyObject)yyVals[-1+yyTop]) != null && 
                          ((BlockAcceptingNode)yyVals[-1+yyTop]).getIterNode() instanceof BlockPassNode) {
                        throw new SyntaxException(PID.BLOCK_ARG_AND_BLOCK_GIVEN, ((IRubyObject)yyVals[-1+yyTop]).getPosition(), lexer.getCurrentLine(), "Both block arg and actual block given.");
                    }
                    yyVal = ((BlockAcceptingNode)yyVals[-1+yyTop]).setIterNode(((IRubyObject)yyVals[0+yyTop]));
                    ((Node)yyVal).setPosition(((IRubyObject)yyVals[-1+yyTop]).getPosition());
    return yyVal;
  }
};
states[26] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_until_mod", ((IRubyObject)yyVals[0+yyTop]), ((IRubyObject)yyVals[-2+yyTop]));
    return yyVal;
  }
};
states[227] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.string("==="), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[93] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_const_path_field", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[462] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("true", Tokens.kTRUE, ((IRubyObject)yyVals[0+yyTop]).getPosition());
    return yyVal;
  }
};
states[395] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (((IRubyObject)yyVals[-3+yyTop]) instanceof SelfNode) {
                        yyVal = support.new_fcall(new Token("[]", support.getPosition(((IRubyObject)yyVals[-3+yyTop]))), ((IRubyObject)yyVals[-1+yyTop]), null);
                    } else {
                        yyVal = support.new_call(((IRubyObject)yyVals[-3+yyTop]), new Token("[]", support.getPosition(((IRubyObject)yyVals[-3+yyTop]))), ((IRubyObject)yyVals[-1+yyTop]), null);
                    }
    return yyVal;
  }
};
states[60] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_command_call", ((IRubyObject)yyVals[-3+yyTop]), ".", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[261] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ISourcePosition pos = ((IRubyObject)yyVals[0+yyTop]) == null ? lexer.getPosition() : ((IRubyObject)yyVals[0+yyTop]).getPosition();
                    yyVal = support.newArrayNode(pos, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[429] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
                    ((ISourcePositionHolder)yyVal).setPosition(((Token)yyVals[-2+yyTop]).getPosition());
    return yyVal;
  }
};
states[362] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-1+yyTop]).getPosition(), null, null, ((IRubyObject)yyVals[-1+yyTop]), null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[295] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[27] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_rescue_mod", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[228] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.string("!="), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[94] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
		    yyVal = support.dispatch('on_field', ((IRubyObject)yyVals[-2+yyTop]), support.symbol('.'), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[463] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("false", Tokens.kFALSE, ((IRubyObject)yyVals[0+yyTop]).getPosition());
    return yyVal;
  }
};
states[396] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
    return yyVal;
  }
};
states[195] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_assign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[61] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_command_call", ((IRubyObject)yyVals[-4+yyTop]), ".", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
                    yyVal = method_add_block(yyVal, ((IRubyObject)yyVals[0+yyTop])); 
    return yyVal;
  }
};
states[262] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newSplatNode(((Token)yyVals[-1+yyTop]).getPosition(), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[497] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.formal_argument(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[430] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ArrayNode(lexer.getPosition());
    return yyVal;
  }
};
states[363] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-3+yyTop]).getPosition(), null, null, ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[28] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        support.warn(ID.END_IN_METHOD, ((IRubyObject)yyVals[-3+yyTop]).getPosition(), "END in method; use at_exit");
                    }
                    yyVal = support.dispatch("on_END", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[229] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.string("=~"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[95] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    yyVal = support.dispatch('on_const_path_field', ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[296] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IfNode(((IRubyObject)yyVals[-5+yyTop]).getPosition(), support.getConditionNode(((IRubyObject)yyVals[-4+yyTop])), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[464] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("__FILE__", Tokens.k__FILE__, ((IRubyObject)yyVals[0+yyTop]).getPosition());
    return yyVal;
  }
};
states[397] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IterNode(((Token)yyVals[-4+yyTop]).getPosition(), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
    return yyVal;
  }
};
states[196] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_assign", ((IRubyObject)yyVals[-4+yyTop]), support.dispatch("on_rescue_mod", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[62] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_command_call", ((IRubyObject)yyVals[-3+yyTop]), "::", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[263] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node node = support.splat_array(((IRubyObject)yyVals[-2+yyTop]));

                    if (node != null) {
                        yyVal = support.list_append(node, ((IRubyObject)yyVals[0+yyTop]));
                    } else {
                        yyVal = support.arg_append(((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
                    }
    return yyVal;
  }
};
states[498] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.arg_var(((IRubyObject)yyVals[0+yyTop]));
  /*
                    $$ = new ArgAuxiliaryNode($1.getPosition(), (String) $1.getValue(), 1);
  */
    return yyVal;
  }
};
states[431] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-2+yyTop]).add(((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[364] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[0+yyTop]).getPosition(), null, null, null, null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[230] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.string("!~"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[96] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_top_const_field", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[297] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IfNode(((IRubyObject)yyVals[-5+yyTop]).getPosition(), support.getConditionNode(((IRubyObject)yyVals[-4+yyTop])), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[-2+yyTop]));
    return yyVal;
  }
};
states[465] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("__LINE__", Tokens.k__LINE__, ((IRubyObject)yyVals[0+yyTop]).getPosition());
    return yyVal;
  }
};
states[398] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
    return yyVal;
  }
};
states[197] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_opassign", ((IRubyObject)yyVals[-2+yyTop]), ((Token)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[63] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_command_call", ((IRubyObject)yyVals[-4+yyTop]), "::", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
                    yyVal = method_add_block(yyVal, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[264] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node node = null;

                    /* FIXME: lose syntactical elements here (and others like this)*/
                    if (((IRubyObject)yyVals[0+yyTop]) instanceof ArrayNode &&
                        (node = support.splat_array(((IRubyObject)yyVals[-3+yyTop]))) != null) {
                        yyVal = support.list_concat(node, ((IRubyObject)yyVals[0+yyTop]));
                    } else {
                        yyVal = support.arg_concat(support.getPosition(((IRubyObject)yyVals[-3+yyTop])), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
                    }
    return yyVal;
  }
};
states[499] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
                    /*		    {
			ID tid = internal_id();
			arg_var(tid);
			if (dyna_in_block()) {
			    $2->nd_value = NEW_DVAR(tid);
			}
			else {
			    $2->nd_value = NEW_LVAR(tid);
			}
			$$ = NEW_ARGS_AUX(tid, 1);
			$$->nd_next = $2;*/
    return yyVal;
  }
};
states[432] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ByteList aChar = ByteList.create("");
                    aChar.setEncoding(lexer.getEncoding());
                    yyVal = lexer.createStrNode(((Token)yyVals[0+yyTop]).getPosition(), aChar, 0);
    return yyVal;
  }
};
states[365] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
    /* was $$ = null;*/
                   yyVal = support.new_args(lexer.getPosition(), null, null, null, null, null);
    return yyVal;
  }
};
states[30] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_massign", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[231] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_unary", support.symbol('!'), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[97] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_var_field", ((IRubyObject)yyVals[0+yyTop]));
                    yyVal = support.dispatch("on_assign_error", yyVal);
    return yyVal;
  }
};
states[298] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.getConditionState().begin();
    return yyVal;
  }
};
states[466] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("__ENCODING__", Tokens.k__ENCODING__, ((IRubyObject)yyVals[0+yyTop]).getPosition());
    return yyVal;
  }
};
states[399] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IterNode(((IRubyObject)yyVals[-4+yyTop]).getPosition(), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), support.getCurrentScope());
                    /* FIXME: What the hell is this?*/
                    ((ISourcePositionHolder)yyVals[-5+yyTop]).setPosition(support.getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])));
                    support.popCurrentScope();
    return yyVal;
  }
};
states[332] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IfNode(((IRubyObject)yyVals[-4+yyTop]).getPosition(), support.getConditionNode(((IRubyObject)yyVals[-3+yyTop])), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[198] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ((IRubyObject)yyVals[-2+yyTop]) = support.dispatch("on_rescue_mod", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
                    yyVal = support.dispatch("on_opassign", ((IRubyObject)yyVals[-4+yyTop]), ((Token)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-2+yyTop]));
    return yyVal;
  }
};
states[64] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_super", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[265] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node node = support.splat_array(((IRubyObject)yyVals[-2+yyTop]));

                    if (node != null) {
                        yyVal = support.list_append(node, ((IRubyObject)yyVals[0+yyTop]));
                    } else {
                        yyVal = support.arg_append(((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
                    }
    return yyVal;
  }
};
states[500] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ArrayNode(lexer.getPosition(), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[433] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.literal_concat(((IRubyObject)yyVals[-1+yyTop]).getPosition(), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[366] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.commandStart = true;
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[31] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_opassign", ((IRubyObject)yyVals[-2+yyTop]), ((Token)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[232] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_unary", support.symbol('~'), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[98] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.assignable(yyVals[0+yyTop], null);
                    yyVal = support.dispatch("on_var_field", yyVal);
    return yyVal;
  }
};
states[299] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.getConditionState().end();
    return yyVal;
  }
};
states[467] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.gettable(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[400] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newWhenNode(((IRubyObject)yyVals[-4+yyTop]).getPosition(), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[199] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ((IRubyObject)yyVals[-5+yyTop]) = support.dispatch("on_aref_field", ((IRubyObject)yyVals[-5+yyTop]), support.escape(((IRubyObject)yyVals[-3+yyTop])));
                    yyVal = support.dispatch("on_opassign", ((IRubyObject)yyVals[-5+yyTop]), ((Token)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[65] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_yield", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[266] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node node = null;

                    if (((IRubyObject)yyVals[0+yyTop]) instanceof ArrayNode &&
                        (node = support.splat_array(((IRubyObject)yyVals[-3+yyTop]))) != null) {
                        yyVal = support.list_concat(node, ((IRubyObject)yyVals[0+yyTop]));
                    } else {
                        yyVal = support.arg_concat(((IRubyObject)yyVals[-3+yyTop]).getPosition(), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
                    }
    return yyVal;
  }
};
states[501] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ((IRubyObject)yyVals[-2+yyTop]).add(((IRubyObject)yyVals[0+yyTop]));
                    yyVal = ((IRubyObject)yyVals[-2+yyTop]);
    return yyVal;
  }
};
states[434] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[367] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((Token)yyVals[-2+yyTop]).getPosition(), null, null, null, null, null);
    return yyVal;
  }
};
states[32] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_aref_field", ((IRubyObject)yyVals[-5+yyTop]), support.escape(((IRubyObject)yyVals[-3+yyTop])));
                    yyVal = support.dispatch("on_opassign", yyVal, ((Token)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[233] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), support.string("<<"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[99] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.assignable(yyVals[0+yyTop], null);
                    yyVal = support.dispatch("on_var_field", yyVal);
    return yyVal;
  }
};
states[300] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node body = ((IRubyObject)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((IRubyObject)yyVals[-1+yyTop]);
                    yyVal = new WhileNode(((IRubyObject)yyVals[-6+yyTop]).getPosition(), support.getConditionNode(((IRubyObject)yyVals[-4+yyTop])), body);
    return yyVal;
  }
};
states[468] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.assignable(((IRubyObject)yyVals[0+yyTop]), NilImplicitNode.NIL);
    return yyVal;
  }
};
states[334] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[200] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ((IRubyObject)yyVals[-4+yyTop]) = support.dispatch("on_field", ((IRubyObject)yyVals[-4+yyTop]), support.symbol('.'), ((IRubyObject)yyVals[-2+yyTop]));
                    yyVal = support.dispatch("on_opassign", ((IRubyObject)yyVals[-4+yyTop]), ((Token)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[66] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_areturn", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[267] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = support.newSplatNode(support.getPosition(((Token)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));  
    return yyVal;
  }
};
}
					// line 1864 "Ripper19Parser.y"

    /** The parse method use an lexer stream and parse it to an AST node 
     * structure
     */
    public RubyParserResult parse(ParserConfiguration configuration, LexerSource source) throws IOException {
        support.reset();
        support.setConfiguration(configuration);
        support.setResult(new RubyParserResult());
        
        lexer.reset();
        lexer.setSource(source);
        lexer.setEncoding(configuration.getDefaultEncoding());

        Object debugger = null;
        if (configuration.isDebug()) {
            try {
                Class yyDebugAdapterClass = Class.forName("jay.yydebug.yyDebugAdapter");
                debugger = yyDebugAdapterClass.newInstance();
            } catch (IllegalAccessException iae) {
                // ignore, no debugger present
            } catch (InstantiationException ie) {
                // ignore, no debugger present
            } catch (ClassNotFoundException cnfe) {
                // ignore, no debugger present
            }
        }
        //yyparse(lexer, new jay.yydebug.yyAnim("JRuby", 9));
        yyparse(lexer, debugger);
        
        return support.getResult();
    }
}
					// line 8020 "-"
