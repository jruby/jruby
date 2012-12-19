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
  public static final int variable = 314;
  public static final int tUPLUS = 315;
  public static final int tUMINUS = 316;
  public static final int tUMINUS_NUM = 317;
  public static final int tPOW = 318;
  public static final int tCMP = 319;
  public static final int tEQ = 320;
  public static final int tEQQ = 321;
  public static final int tNEQ = 322;
  public static final int tGEQ = 323;
  public static final int tLEQ = 324;
  public static final int tANDOP = 325;
  public static final int tOROP = 326;
  public static final int tMATCH = 327;
  public static final int tNMATCH = 328;
  public static final int tDOT = 329;
  public static final int tDOT2 = 330;
  public static final int tDOT3 = 331;
  public static final int tAREF = 332;
  public static final int tASET = 333;
  public static final int tLSHFT = 334;
  public static final int tRSHFT = 335;
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
  public static final int tFLOAT = 380;
  public static final int tREGEXP_END = 381;
  public static final int tLOWEST = 382;
  public static final int keyword_variable = 383;
  public static final int yyErrorCode = 256;

  /** number of final state.
    */
  protected static final int yyFinal = 1;

  /** parser tables.
      Order is mandated by <i>jay</i>.
    */
  protected static final short[] yyLhs = {
//yyLhs 559
    -1,   121,     0,   117,   118,   118,   118,   118,   119,   124,
   119,    34,    33,    35,    35,    35,    35,   125,    36,    36,
    36,    36,    36,    36,    36,    36,    36,    36,    36,    36,
    36,    36,    36,    36,    36,    36,    36,    36,    36,    36,
    36,    36,    31,    31,    37,    37,    37,    37,    37,    37,
    41,    32,    32,    55,    55,    55,   127,    94,    40,    40,
    40,    40,    40,    40,    40,    40,    40,    40,    40,    95,
    95,   106,   106,    96,    96,    96,    96,    96,    96,    96,
    96,    96,    96,    67,    67,    81,    81,    85,    85,    68,
    68,    68,    68,    68,    68,    68,    68,    68,    73,    73,
    73,    73,    73,    73,    73,    73,    73,     6,     6,    30,
    30,    30,     7,     7,     7,     7,     7,    99,    99,   100,
   100,    57,   129,    57,     8,     8,     8,     8,     8,     8,
     8,     8,     8,     8,     8,     8,     8,     8,     8,     8,
     8,     8,     8,     8,     8,     8,     8,     8,     8,     8,
     8,     8,     8,   115,   115,   115,   115,   115,   115,   115,
   115,   115,   115,   115,   115,   115,   115,   115,   115,   115,
   115,   115,   115,   115,   115,   115,   115,   115,   115,   115,
   115,   115,   115,   115,   115,   115,   115,   115,   115,   115,
   115,   115,   115,   115,   115,    38,    38,    38,    38,    38,
    38,    38,    38,    38,    38,    38,    38,    38,    38,    38,
    38,    38,    38,    38,    38,    38,    38,    38,    38,    38,
    38,    38,    38,    38,    38,    38,    38,    38,    38,    38,
    38,    38,    38,    38,    38,    38,    38,    38,    38,    38,
    69,    72,    72,    72,    72,    49,    53,    53,   109,   109,
   109,   109,   109,    47,    47,    47,    47,    47,   131,    51,
    88,    87,    87,    75,    75,    75,    75,    66,    66,    66,
    39,    39,    39,    39,    39,    39,    39,    39,    39,    39,
   132,    39,    39,    39,    39,    39,    39,    39,    39,    39,
    39,    39,    39,    39,    39,    39,    39,    39,    39,    39,
   134,   135,    39,   136,   137,    39,    39,    39,   138,   139,
    39,   140,    39,   142,   143,    39,   144,    39,   145,    39,
   146,   147,    39,    39,    39,    39,    39,    42,   133,   133,
   133,   120,   120,    45,    45,    43,    43,   108,   108,   110,
   110,    80,    80,   111,   111,   111,   111,   111,   111,   111,
   111,   111,    63,    63,    63,    63,    63,    63,    63,    63,
    63,    63,    63,    63,    63,    63,    63,    65,    65,    64,
    64,    64,   103,   103,   102,   102,   112,   112,   148,   105,
    62,    62,   104,   104,   149,    93,    54,    54,    54,    23,
    23,    23,    23,    23,    23,    23,    23,    23,   150,    92,
   151,    92,    70,    44,    44,    97,    97,    71,    71,    71,
    46,    46,    48,    48,    27,    27,    27,    15,    16,    16,
    16,    17,    18,    19,    24,    24,    77,    77,    26,    26,
    25,    25,    76,    76,    20,    20,    21,    21,   152,   152,
    22,   153,    22,   154,    22,    58,    58,    58,    58,     2,
     1,     1,     1,     1,    29,    28,    28,    28,    28,   128,
   128,   128,   128,   128,   155,   155,   155,   155,   155,   155,
   155,    52,    52,    98,    98,    59,    59,    50,   156,    50,
    50,    61,    61,    60,    60,    60,    60,    60,    60,    60,
    60,    60,    60,    60,    60,    60,    60,    60,   116,   116,
   116,   116,     9,     9,   101,   101,    78,    78,    56,   107,
    86,    86,    79,    79,    11,    11,    13,    13,    12,    12,
    91,    90,    90,    14,   157,    14,    84,    84,    82,    82,
    83,    83,     3,     3,     3,     4,     4,     4,     4,     5,
     5,     5,    10,    10,   122,   122,   126,   126,   113,   114,
   130,   130,   130,   141,   141,   123,   123,    74,    89,
    }, yyLen = {
//yyLen 559
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
     2,     4,     2,     1,     2,     2,     4,     1,     0,     2,
     2,     2,     1,     1,     2,     3,     4,     3,     4,     2,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     3,
     0,     4,     3,     3,     2,     3,     3,     1,     4,     3,
     1,     5,     4,     3,     2,     1,     2,     2,     6,     6,
     0,     0,     7,     0,     0,     7,     5,     4,     0,     0,
     9,     0,     6,     0,     0,     8,     0,     5,     0,     6,
     0,     0,     9,     1,     1,     1,     1,     1,     1,     1,
     2,     1,     1,     1,     5,     1,     2,     1,     1,     1,
     3,     1,     3,     1,     4,     6,     3,     5,     2,     4,
     1,     3,     6,     8,     4,     6,     4,     2,     6,     2,
     4,     6,     2,     4,     2,     4,     1,     1,     1,     3,
     1,     4,     1,     2,     1,     3,     1,     1,     0,     3,
     4,     2,     3,     3,     0,     5,     2,     4,     4,     2,
     4,     4,     3,     3,     3,     2,     1,     4,     0,     5,
     0,     5,     5,     1,     1,     6,     1,     1,     1,     1,
     2,     1,     2,     1,     1,     1,     1,     1,     1,     1,
     2,     3,     3,     3,     3,     3,     0,     3,     1,     2,
     3,     3,     0,     3,     0,     2,     0,     2,     0,     2,
     1,     0,     3,     0,     4,     1,     1,     1,     1,     2,
     1,     1,     1,     1,     3,     1,     1,     2,     2,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     0,     4,
     2,     3,     2,     6,     8,     4,     6,     4,     6,     2,
     4,     6,     2,     4,     2,     4,     1,     0,     1,     1,
     1,     1,     1,     1,     1,     3,     1,     3,     3,     3,
     1,     3,     1,     3,     1,     1,     2,     1,     1,     1,
     2,     2,     0,     1,     0,     4,     1,     2,     1,     3,
     3,     2,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     0,     1,     0,     1,     2,     2,
     0,     1,     1,     1,     1,     1,     2,     0,     0,
    }, yyDefRed = {
//yyDefRed 971
     1,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   300,   303,     0,     0,     0,   325,   326,     0,
     0,     0,   465,   464,   466,   467,     0,     0,     0,     9,
     0,   469,   468,   470,     0,     0,   461,   460,     0,   463,
   418,    89,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   434,   436,   436,     0,     0,   378,
   475,   476,   455,   456,     0,   415,     0,   271,     0,   419,
   272,   273,     0,   274,   275,   270,   414,   416,    29,    44,
     0,     0,     0,     0,     0,     0,   276,     0,    52,     0,
     0,    83,     0,     4,     0,     0,    69,     0,     2,     0,
     5,     0,     0,     7,   323,   324,   287,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   110,     0,
   327,     0,   277,   471,   472,   316,   163,   174,   164,   187,
   160,   180,   170,   169,   185,   168,   167,   162,   188,   172,
   161,   175,   179,   181,   173,   166,   182,   189,   184,     0,
     0,     0,     0,   159,   178,   177,   190,   191,   192,   193,
   194,   158,   165,   156,   157,     0,     0,     0,     0,   114,
     0,   148,   149,   145,   127,   128,   129,   136,   133,   135,
   130,   131,   150,   151,   137,   138,   524,   142,   141,   126,
   147,   144,   143,   139,   140,   134,   132,   124,   146,   125,
   152,   318,   115,     0,   523,   116,   183,   176,   186,   171,
   153,   154,   155,   112,   113,   118,   117,   120,     0,   119,
   121,     0,     0,     0,     0,     0,    13,     0,    99,     0,
     0,     0,     0,     0,     0,     0,   553,   554,     0,     0,
     0,   555,     0,     0,     0,     0,     0,   337,   338,     0,
     0,     0,     0,     0,     0,     0,   253,    67,     0,     0,
     0,   528,   257,    68,    66,     0,    65,     0,     0,   395,
    64,     0,   547,     0,     0,    17,     0,     0,     0,   216,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   241,     0,     0,     0,   526,     0,     0,     0,     0,
    90,     0,     0,     0,   232,    48,   231,   452,   451,   453,
   449,   450,     0,     0,     0,     0,     0,     0,     0,     0,
   297,     0,   400,   398,   389,     0,   294,   420,   296,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   384,   386,     0,     0,     0,     0,     0,     0,
    85,     0,     0,     0,     0,     0,     0,     3,     0,     0,
   457,   458,     0,   107,     0,   109,     0,   478,   311,   477,
     0,     0,     0,     0,     0,     0,   542,   543,   320,   122,
     0,     0,   406,     0,   279,    12,     0,     0,   329,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   556,     0,     0,     0,     0,     0,     0,   308,   531,
   264,   260,     0,     0,   254,   262,     0,   255,     0,   289,
     0,   259,   249,   248,     0,     0,     0,     0,   293,    47,
    19,    21,    20,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   282,     0,     0,   285,     0,   551,
   242,     0,   244,   527,   286,     0,    87,     0,     0,     0,
     0,     0,   443,   441,   454,   440,   437,   421,   435,   422,
   423,   424,   425,   428,     0,   430,   431,     0,     0,   500,
   499,   498,   501,     0,     0,   515,   514,   519,   518,   504,
     0,     0,     0,   512,     0,     0,     0,     0,   496,   506,
   502,     0,     0,    56,    59,    23,    24,    25,    26,    27,
    45,    46,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   537,     0,     0,   538,   393,     0,     0,     0,     0,   392,
     0,   394,     0,   535,   536,     0,     0,    37,     0,     0,
    43,    42,     0,    38,   263,     0,     0,     0,     0,     0,
    86,    30,    40,     0,    31,     0,     6,     0,   480,     0,
     0,     0,     0,     0,     0,   111,     0,     0,     0,     0,
     0,     0,     0,     0,   408,     0,     0,   409,     0,     0,
   335,     0,     0,   330,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   307,   332,   301,   331,   304,     0,     0,
     0,     0,     0,     0,   530,     0,     0,     0,   261,   529,
   288,   548,     0,     0,   245,   292,    18,     0,     0,    28,
     0,     0,     0,     0,   281,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   427,   429,   433,     0,   503,
     0,     0,   339,     0,   341,     0,     0,   516,   520,     0,
   494,     0,   372,   381,     0,     0,   379,     0,   489,     0,
   492,   370,     0,   368,     0,   367,     0,     0,     0,     0,
     0,     0,   247,     0,   390,   246,     0,     0,   391,     0,
     0,     0,    54,   387,    55,   388,     0,     0,     0,     0,
    84,     0,     0,     0,   314,     0,     0,   397,   317,   525,
     0,   482,     0,   321,   123,     0,     0,   411,   336,     0,
    11,   413,     0,   333,     0,     0,     0,     0,     0,     0,
     0,   306,     0,     0,     0,     0,     0,     0,   266,   256,
     0,   291,    10,   243,    88,     0,     0,   445,   446,   447,
   442,   448,   508,     0,     0,     0,     0,   505,     0,     0,
   521,   376,     0,   374,   377,     0,     0,     0,     0,   507,
     0,   513,     0,     0,     0,     0,     0,     0,   366,     0,
   510,     0,     0,     0,     0,     0,    33,     0,    34,     0,
    61,    36,     0,    35,     0,    63,     0,   549,     0,     0,
     0,     0,     0,     0,   479,   312,   481,   319,     0,     0,
     0,     0,   410,     0,   412,     0,   298,     0,   299,   265,
     0,     0,     0,   309,     0,   444,   340,     0,     0,     0,
   342,   380,     0,   495,     0,   383,   382,     0,   487,     0,
   485,     0,   490,   493,     0,     0,   364,     0,     0,   359,
     0,   362,   369,   401,   399,     0,     0,   385,    32,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   403,
   402,   404,   302,   305,     0,     0,     0,     0,     0,   375,
     0,     0,     0,     0,     0,     0,     0,   371,     0,     0,
     0,     0,   511,    57,   315,     0,     0,     0,     0,     0,
     0,   405,     0,     0,     0,     0,   488,     0,   483,   486,
   491,   284,     0,   365,     0,   356,     0,   354,     0,   360,
   363,   322,     0,   334,   310,     0,     0,     0,     0,     0,
     0,     0,     0,   484,   358,     0,   352,   355,   361,     0,
   353,
    }, yyDgoto = {
//yyDgoto 158
     1,   310,    65,   117,   604,   569,   118,   216,   563,   509,
   398,   510,   511,   512,   203,    67,    68,    69,    70,    71,
   313,   312,   486,    72,    73,    74,   494,    75,    76,    77,
   119,    78,    79,   222,   223,   224,   225,    81,    82,    83,
    84,   230,   280,   753,   900,   754,   746,   442,   750,   571,
   388,   266,    86,   714,    87,    88,   513,   218,   780,   232,
   610,   611,   515,   805,   703,   704,   583,    90,    91,   258,
   420,   616,   290,   233,   226,   444,   319,   317,   516,   517,
   683,    94,   445,   261,   297,   477,   807,   434,   262,   435,
   690,   790,   326,   363,   524,    95,    96,   403,   234,   219,
   220,   519,   792,   693,   696,   320,   288,   810,   249,   446,
   684,   685,   793,   439,   720,   205,   520,    98,    99,   100,
   635,     2,   239,   240,   277,   453,   440,   707,   101,   613,
   470,   267,   466,   409,   242,   764,   243,   765,   643,   904,
   600,   410,   597,   833,   393,   395,   612,   838,   321,   558,
   522,   521,     0,   674,   673,   102,   599,   394,
    }, yySindex = {
//yySindex 971
     0,     0, 14720, 15103, 18024, 18148, 18614, 18505, 14848, 17135,
 17135, 13015,     0,     0,  3964, 15357, 15357,     0,     0, 15357,
  -190,  -170,     0,     0,     0,     0,   185, 18396,   210,     0,
  -128,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0, 17262, 17262,  -143,   -34, 14976, 17135, 15738, 16119,
  4442, 17262, 17389, 18722,     0,     0,     0,   240,   252,     0,
     0,     0,     0,     0,     0,     0,  -153,     0,  -116,     0,
     0,     0,  -172,     0,     0,     0,     0,     0,     0,     0,
  1520,    74,  3490,     0,    15,   202,     0,  -131,     0,   -37,
   247,     0,   243,     0, 17643,   249,     0,   -22,     0,    85,
     0,     0,     0,     0,     0,     0,     0,  -190,  -170,   -19,
   210,     0,     0,   199, 17135,   -58, 14848,  -153,     0,    55,
     0,   298,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,  -130,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   270,     0,
     0, 15230,    57,    65,    85,  1520,     0,    40,     0,    74,
   524,   422,    17,   292,    30,   524,     0,     0,    85,   130,
   322,     0, 17135, 17135,    84,   427,     0,     0,     0,   261,
     0, 17262, 17262, 17262, 17262,  3490,     0,     0,   203,   507,
   525,     0,     0,     0,     0,  5850,     0, 15357, 15357,     0,
     0, 13144,     0, 17135,  -155,     0, 16246,   213, 14848,     0,
   437,   269,   287,   294,   299, 14976,   288,     0,   210,    74,
   300,     0,   110,   129,   203,     0,   129,   301,   357, 17770,
     0,   498,     0,   629,     0,     0,     0,     0,     0,     0,
     0,     0,   481,   587,   903,   352,   302,   988,   308,  -177,
     0,  1426,     0,     0,     0,   337,     0,     0,     0, 17135,
 17135, 17135, 17135, 15230, 17135, 17135, 17262, 17262, 17262, 17262,
 17262, 17262, 17262, 17262, 17262, 17262, 17262, 17262, 17262, 17262,
 17262, 17262, 17262, 17262, 17262, 17262, 17262, 17262, 17262, 17262,
 17262, 17262,     0,     0,  2404,  2902, 15357, 19110, 19110, 17389,
     0, 16373, 14976,  4860,   649, 16373, 17389,     0, 14465,   364,
     0,     0,    74,     0,     0,     0,    85,     0,     0,     0,
  3400,  4941, 15357, 14848, 17135,  1664,     0,     0,     0,     0,
  1520, 16500,     0,   453,     0,     0, 14592,   299,     0, 14848,
   461,  5312,  5964, 15357, 17262, 17262, 17262, 14848,   130, 16627,
   467,     0,   264,   264,     0,  6275,  7310, 15357,     0,     0,
     0,     0, 17262, 15484,     0,     0, 15865,     0,   210,     0,
   406,     0,     0,     0,   715,   718,   210,    50,     0,     0,
     0,     0,     0, 18505, 17135,  3490, 14720,   402,  5312,  5964,
 17262, 17262, 17262,   210,     0,     0,   210,     0, 15992,     0,
     0, 16119,     0,     0,     0,     0,     0,   731,  7842, 18886,
 15357, 17770,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,    71,     0,     0,   746,   719,     0,
     0,     0,     0,   436,  1790,     0,     0,     0,     0,     0,
   480,   490,   758,     0,   753,  -184,   764,   770,     0,     0,
     0,  -185,  -185,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   269,  2001,  2001,  2001,  2001,  1475,  1475,  3321,
  2823,  2001,  2001,  2325,  2325,   -47,   -47,   269,  1332,   269,
   269,   353,   353,  1475,  1475,  1364,  1364,  4305,  -185,   485,
     0,   486,  -170,     0,     0,   504,     0,   508,  -170,     0,
     0,     0,   210,     0,     0,  -170,  -170,     0,  3490, 17262,
     0,     0,  2494,     0,     0,   775,   803,   210, 17770,   812,
     0,     0,     0,     0,     0,  2992,     0,    85,     0, 17135,
 14848,  -170,     0,     0,  -170,     0,   210,   555,    50,  1790,
    85, 14848, 18830, 18505,     0,     0,   519,     0, 14848,   598,
     0,  1520,   381,     0,   527,   531,   536,   538,   210,  2494,
   453,   623,   618,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   210, 17135,     0, 17262,   203,   525,     0,     0,
     0,     0, 15484, 15865,     0,     0,     0,    50,   499,     0,
   269,   269,  3490,     0,     0,   129, 17770,     0,     0,     0,
     0,   210,   731, 14848,   463,     0,     0,     0, 17262,     0,
   436,   571,     0,   848,     0,   210,   753,     0,     0,  1243,
     0,   833,     0,     0, 14848, 14848,     0,  1790,     0,  1790,
     0,     0,  1615,     0, 14848,     0, 14848,  -185,   858, 14848,
 17389, 17389,     0,   337,     0,     0, 17389, 17389,     0,   337,
   567,   592,     0,     0,     0,     0,     0, 17262, 17389, 16754,
     0,   731, 17770, 17262,     0,    85,   678,     0,     0,     0,
   210,     0,   679,     0,     0, 17897,   524,     0,     0, 14848,
     0,     0, 17135,     0,   684, 17262, 17262, 17262, 17262,   619,
   692,     0, 16881, 14848, 14848, 14848,     0,   264,     0,     0,
   918,     0,     0,     0,     0,     0,   605,     0,     0,     0,
     0,     0,     0,   210,  1497,   925,   982,     0,   633,   927,
     0,     0,   932,     0,     0,   721,   620,   938,   943,     0,
   944,     0,   927,   929,   948,   753,   950,   951,     0,   639,
     0,   735,   640, 14848, 17262,   740,     0,  3490,     0,  3490,
     0,     0,  3490,     0,  3490,     0, 17389,     0,  3490, 17262,
     0,   731,  3490, 14848,     0,     0,     0,     0,  1664,   696,
   574,     0,     0, 14848,     0,   524,     0, 17262,     0,     0,
  -129,   744,   750,     0, 15865,     0,     0,   972,  1497,   794,
     0,     0,  1243,     0,   833,     0,     0,  1243,     0,  1790,
     0,  1243,     0,     0, 18272,  1243,     0,   661,  1797,     0,
  1797,     0,     0,     0,     0,   662,  3490,     0,     0,  3490,
     0,   760, 14848,     0, 18942, 18998, 15357,    57, 14848,     0,
     0,     0,     0,     0, 14848,  1497,   972,  1497,   984,     0,
   927,   987,   927,   927,   727,   589,   927,     0,   994,   998,
  1004,   927,     0,     0,     0,   785,     0,     0,     0,     0,
   210,     0,   381,   795,   972,  1497,     0,  1243,     0,     0,
     0,     0, 19054,     0,  1243,     0,  1797,     0,  1243,     0,
     0,     0,     0,     0,     0,   972,   927,     0,     0,   927,
  1017,   927,   927,     0,     0,  1243,     0,     0,     0,   927,
     0,
    }, yyRindex = {
//yyRindex 971
     0,     0,   277,     0,     0,     0,     0,     0,  1125,     0,
     0,   796,     0,     0,     0, 13305, 13434,     0,     0, 13546,
  4733,  4235,     0,     0,     0,     0, 17516,     0, 17008,     0,
     0,     0,     0,     0,  2116,  3239,     0,     0,  2243,     0,
     0,     0,     0,     0,     0,     0,    89,     0,   720,   706,
   150,     0,     0,  1104,     0,     0,     0,  1127,  -105,     0,
     0,     0,     0,     0,    56,     0, 15611,     0,  7431,     0,
     0,     0,  7545,     0,     0,     0,     0,     0,     0,     0,
    59,  1739, 12814,  7647, 13853,     0,     0, 13982,     0, 13835,
     0,     0,     0,     0,   166,     0,     0,     0,     0,    20,
     0,  6510,  7023,     0,     0,     0,     0,  7771,  6612,     0,
   729, 12082, 12210,     0,     0,     0,    89,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  1033,
  1980,  2358,  2438,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  2856,  2936,  3354,  3434,     0,
  3852,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  6460,     0,
     0,     0,   -38,     0,  1176,   450,     0,     0,     0, 14373,
     0,     0,  7125,     0,     0,     0,     0,     0,   796,     0,
   802,     0,     0,     0,     0,     0,   585,     0,     0,     0,
   650,     0,     0,     0,     0, 11813,     0,     0, 14068, 14197,
 14197,     0,     0,     0,     0,   732,     0,     0,    54,     0,
     0,   732,     0,     0,     0,     0,     0,     0,    70,     0,
     0,  8179,  7953,  8077, 13964,    89,     0,   136,   732,   128,
     0,     0,   734,   734,     0,     0,   713,     0,     0,     0,
     0,     0,   214,   177,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   -27,     0,     0,     0, 10869,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,    43,     0,     0,     0,
     0,     0,    89,   179,   204,     0,     0,     0,    63,     0,
     0,     0,   145,     0, 12626,     0,     0,     0,     0,     0,
     0,     0,    43,  1125,     0,   156,     0,     0,     0,     0,
   604,   470,     0,   447,     0,     0,  1327,  7239,     0,   676,
  5430,     0,     0,    43,     0,     0,     0,   293,     0,     0,
     0,     0,     0,     0,   634,     0,     0,    43,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   732,     0,
     0,     0,     0,     0,     4,     4,   732,   732,     0,     0,
     0,     0,     0,     0,     0,  6396,    70,     0,     0,     0,
     0,     0,     0,   732,     0,   622,   732,     0,   738,     0,
     0,  -151,     0,     0,     0,  1114,     0,   218,     0,     0,
    43,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,    66,     0,
     0,     0,     0,     0,     9,     0,     0,     0,     0,     0,
    38,     0,    69,     0,  -183,     0,    69,    69,     0,     0,
     0, 12754, 12887,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  8293,  1789, 10262, 10386, 10483,  9807,  9924, 10569,
  1918, 10656, 10743,  6860, 10829,  9241,  9355,  8427,  9457,  8541,
  8643,  8993,  9107, 10048, 10145,  9581,  9698,  1019, 12754,  5095,
     0,  5222,  4606,     0,     0,  5593,  3610,  5720, 15611,     0,
  3737,     0,   739,     0,     0,  6082,  6082,     0, 10926,     0,
     0,     0,  1313,     0,     0,     0,     0,   732,     0,   219,
     0,     0,     0,  1460,     0, 11899,     0,     0,     0,     0,
  1125,  6717, 12354, 12482,     0,     0,   739,     0,   732,   146,
     0,  1125,     0,     0,     0,   286,   582,     0,   690,   816,
     0,   827,   816,     0,  2614,  2741,  3112,  4108,   739, 11986,
   816,     0,     0,     0,     0,     0,     0,     0,   975,  1308,
  1380,   591,   739,     0,     0,     0, 14108, 14197,     0,     0,
     0,     0,    45,   123,     0,     0,     0,   732,     0,     0,
  8767,  8891, 11013,    88,     0,   734,     0,   542,   694,   920,
  1150,   739,   231,    70,     0,     0,     0,     0,     0,     0,
     0,   147,     0,   153,     0,   732,   743,     0,     0,     0,
     0,     0,     0,     0,   285,    70,     0,     0,     0,     0,
     0,     0,   747,     0,   285,     0,    70, 12887,     0,   285,
     0,     0,     0, 11226,     0,     0,     0,     0,     0, 11669,
 13675,     0,     0,     0,     0,     0,  9496,     0,     0,     0,
     0,   256,     0,     0,     0,     0,     0,     0,     0,     0,
   732,     0,     0,     0,     0,     0,     0,     0,     0,   285,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  6204,
     0,     0,     0,   638,   285,   285,   971,     0,     0,     0,
     4,     0,     0,     0,     0,   847,     0,     0,     0,     0,
     0,     0,     0,   732,     0,   154,     0,     0,     0,    69,
     0,     0,   -60,     0,     0,     0,     0,    69,    69,     0,
    69,     0,    69,    -3,    -5,   747,    -5,    -5,     0,     0,
     0,     0,     0,    70,     0,     0,     0, 11099,     0, 11186,
     0,     0, 11283,     0, 11369,     0,     0,     0, 11456,     0,
 14237,   257, 11543,  1125,     0,     0,     0,     0,   156,     0,
     0,   724,     0,  1125,     0,     0,     0,     0,     0,     0,
   816,     0,     0,     0,   137,     0,     0,   159,     0,   160,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     2,     0,
     0,     0,     0,     0,     0,     0, 11629,     0,     0, 11726,
 14334,     0,  1125,   742,     0,     0,    43,   -38,   676,     0,
     0,     0,     0,     0,   285,     0,   162,     0,   170,     0,
    69,    69,    69,    69,     0,   188,    -5,     0,    -5,    -5,
    -5,    -5,     0,     0,     0,     0,   158,   967,  1071,   583,
   739,     0,   816,     0,   172,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  1151,     0,     0,   174,    69,  1179,  1304,    -5,
    -5,    -5,    -5,     0,     0,     0,     0,     0,     0,    -5,
     0,
    }, yyGindex = {
//yyGindex 158
     0,     0,     8,   779,  -275,     0,   -44,    -1,    -6,  -368,
     0,     0,     0,   749,     0,     0,     0,  1025,     0,     0,
     0,   671,  -206,     0,     0,     0,     0,     0,     0,    11,
  1089,  -346,   100,   563,  -381,     0,    99,  1192,  1518,    73,
    -8,    47,   323,  -384,     0,   165,     0,   565,     0,    -4,
     0,    64,  1093,   193,     0,     0,  -605,     0,     0,   947,
   -68,   263,     0,     0,     0,  -413,  -216,   -81,   127,    -9,
  -395,     0,     0,   999,     1,   745,     0,     0,  6222,   410,
  -672,     0,    18,  -420,     0,  -375,   239,  -238,  -391,     0,
  1107,  -311,  1046,     0,  -503,  1105,    78,   224,   522,     0,
   -18,  -623,     0,  -524,     0,     0,  -197,  -746,     0,  -322,
  -703,   443,   265,   397,  -463,     0,  -610,   669,     0,    21,
  -327,     0,    32,    27,     0,     0,   -24,     0,    22,     0,
  -250,     0,     0,  -229,     0,     0,     0,     0,     0,     0,
     0,    62,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,    25,     0,     0,
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
    "tCHAR","variable","tUPLUS","tUMINUS","tUMINUS_NUM","tPOW","tCMP",
    "tEQ","tEQQ","tNEQ","tGEQ","tLEQ","tANDOP","tOROP","tMATCH","tNMATCH",
    "tDOT","tDOT2","tDOT3","tAREF","tASET","tLSHFT","tRSHFT","tCOLON2",
    "tCOLON3","tOP_ASGN","tASSOC","tLPAREN","tLPAREN2","tRPAREN",
    "tLPAREN_ARG","tLBRACK","tRBRACK","tLBRACE","tLBRACE_ARG","tSTAR",
    "tSTAR2","tAMPER","tAMPER2","tTILDE","tPERCENT","tDIVIDE","tPLUS",
    "tMINUS","tLT","tGT","tPIPE","tBANG","tCARET","tLCURLY","tRCURLY",
    "tBACK_REF2","tSYMBEG","tSTRING_BEG","tXSTRING_BEG","tREGEXP_BEG",
    "tWORDS_BEG","tQWORDS_BEG","tSTRING_DBEG","tSTRING_DVAR",
    "tSTRING_END","tLAMBDA","tLAMBEG","tNTH_REF","tBACK_REF",
    "tSTRING_CONTENT","tINTEGER","tFLOAT","tREGEXP_END","tLOWEST",
    "keyword_variable",
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
    "opt_bv_decl : none",
    "opt_bv_decl : ';' bv_decls",
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
    "opt_rescue : none",
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
    "regexp_contents :",
    "regexp_contents : regexp_contents string_content",
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
    "user_variable : tIDENTIFIER",
    "user_variable : tIVAR",
    "user_variable : tGVAR",
    "user_variable : tCONSTANT",
    "user_variable : tCVAR",
    "kernel_variable : kNIL",
    "kernel_variable : kSELF",
    "kernel_variable : kTRUE",
    "kernel_variable : kFALSE",
    "kernel_variable : k__FILE__",
    "kernel_variable : k__LINE__",
    "kernel_variable : k__ENCODING__",
    "var_ref : user_variable",
    "var_ref : kernel_variable",
    "var_lhs : user_variable",
    "var_lhs : kernel_variable",
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

static ParserState[] states = new ParserState[559];
static {
states[435] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_string_add", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[368] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.commandStart = true;
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
                    lexer.getConditionState().end();
    return yyVal;
  }
};
states[402] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_when", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), support.escape(((IRubyObject)yyVals[0+yyTop])));

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
states[268] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.mrhs_add_star(args2mrhs(((IRubyObject)yyVals[-3+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[503] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.formal_argument(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[436] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_xstring_new");
    return yyVal;
  }
};
states[369] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.blockvar_new(support.params_new(null, null, null, null, null), support.escape(((IRubyObject)yyVals[-1+yyTop])));
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
                    yyVal = support.dispatch("on_while", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[336] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_else", ((IRubyObject)yyVals[0+yyTop]));
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
states[269] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.mrhs_add_star(mrhs_new(), ((IRubyObject)yyVals[0+yyTop]));
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
                    yyVal = support.arg_var(((IRubyObject)yyVals[0+yyTop]));
  /*
                    $$ = new ArgAuxiliaryNode($1.getPosition(), (String) $1.getValue(), 1);
  */
    return yyVal;
  }
};
states[437] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_xstring_add", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[370] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.blockvar_new(support.params_new(null, null, null, null, null), null);
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
                  lexer.getConditionState().begin();
    return yyVal;
  }
};
states[471] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.id_is_var(support.get_id(yyVals[0+yyTop]))) {
                        yyVal = support.dispatch1("on_var_ref", yyVals[0+yyTop]);
                    } else {
                        yyVal = support.dispatch1("on_vcall", yyVals[0+yyTop]);
                    }
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
states[438] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_regexp_new");
    return yyVal;
  }
};
states[371] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.blockvar_new(support.escape(((IRubyObject)yyVals[-2+yyTop])), support.escape(((IRubyObject)yyVals[-1+yyTop])));
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
                  lexer.getConditionState().end();
    return yyVal;
  }
};
states[472] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_var_ref", yyVals[0+yyTop]);
    return yyVal;
  }
};
states[405] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch4("on_rescue",
				       support.escape(((IRubyObject)yyVals[-4+yyTop])),
				       support.escape(((IRubyObject)yyVals[-3+yyTop])),
				       support.escape(((IRubyObject)yyVals[-1+yyTop])),
				       support.escape(((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[338] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
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
                    yyVal = new ArrayNode(lexer.getPosition(), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[439] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_regexp_add", yyVals[-1+yyTop], ((IRubyObject)yyVals[0+yyTop]));
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
                    yyVal = support.dispatch("on_until", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
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
                    yyVal = support.assignable(yyVals[0+yyTop], null);
                    yyVal = support.dispatch("on_var_field", yyVal);
    return yyVal;
  }
};
states[339] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.assignable(((IRubyObject)yyVals[0+yyTop]), null);
                    yyVal = support.dispatch("on_mlhs_paren", yyVal);
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
                    ((IRubyObject)yyVals[-2+yyTop]).add(((IRubyObject)yyVals[0+yyTop]));
                    yyVal = ((IRubyObject)yyVals[-2+yyTop]);
    return yyVal;
  }
};
states[373] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
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
                    yyVal = support.dispatch("on_case", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
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
                    yyVal = support.assignable(yyVals[0+yyTop], null);
                    yyVal = support.dispatch("on_var_field", yyVal);
    return yyVal;
  }
};
states[407] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_array(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[340] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch1("on_mlhs_paren", ((IRubyObject)yyVals[-1+yyTop]));
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
states[508] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.arg_var(support.formal_argument(((IRubyObject)yyVals[-2+yyTop])));
                    yyVal = new OptArgNode(((IRubyObject)yyVals[-2+yyTop]).getPosition(), support.assignable(((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[441] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = lexer.getStrTerm();
                    lexer.setStrTerm(null);
                    lexer.setState(LexState.EXPR_BEG);
    return yyVal;
  }
};
states[374] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_array(((IRubyObject)yyVals[0+yyTop]));
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
                    yyVal = support.dispatch("on_case", null, ((IRubyObject)yyVals[0+yyTop]));
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
states[408] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[341] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.mlhs_add(support.mlhs_new(), ((IRubyObject)yyVals[0+yyTop]));
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
states[509] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.arg_var(support.formal_argument(((IRubyObject)yyVals[-2+yyTop])));
                    yyVal = new OptArgNode(((IRubyObject)yyVals[-2+yyTop]).getPosition(), support.assignable(((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[442] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setStrTerm(((StrTerm)yyVals[-1+yyTop]));
                    yyVal = support.dispatch("on_string_dvar", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[375] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-2+yyTop]).append(((IRubyObject)yyVals[0+yyTop]));
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
                    lexer.getConditionState().begin();
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
states[342] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.mlhs_add(((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
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
                    yyVal = new BlockNode(((IRubyObject)yyVals[0+yyTop]).getPosition()).add(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[443] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = lexer.getStrTerm();
                   lexer.getConditionState().stop();
                   lexer.getCmdArgumentState().stop();
                   lexer.setStrTerm(null);
                   lexer.setState(LexState.EXPR_BEG);
    return yyVal;
  }
};
states[376] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.new_bv(support.get_id(((IRubyObject)yyVals[0+yyTop])));
                    yyVal = support.get_value(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[309] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.getConditionState().end();
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
                    yyVal = null;
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
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
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
                    yyVal = support.appendToBlock(((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[444] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.getConditionState().restart();
                   lexer.getCmdArgumentState().restart();
                   lexer.setStrTerm(((StrTerm)yyVals[-2+yyTop]));
                   yyVal = support.dispatch("on_string_embexpr", ((Token)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[377] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
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
                    yyVal = support.dispatch("on_for", ((IRubyObject)yyVals[-7+yyTop]), ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
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
                    yyVal = support.arg_add_assocs(((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[478] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.setState(LexState.EXPR_BEG);
    return yyVal;
  }
};
states[344] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.assignable(((IRubyObject)yyVals[0+yyTop]), null);
                    yyVal = support.mlhs_add_star(((IRubyObject)yyVals[-3+yyTop]), yyVal);
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
states[512] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BlockNode(((IRubyObject)yyVals[0+yyTop]).getPosition()).add(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[445] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[378] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
                    yyVal = lexer.getLeftParenBegin();
                    lexer.setLeftParenBegin(lexer.incrementParenNest());
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
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("class definition in method body");
                    }
                    support.pushLocalScope();
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
                    yyVal = support.arg_add_assocs(support.arg_new(), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[479] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[412] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_ensure", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[345] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.assignable(((IRubyObject)yyVals[-2+yyTop]), 0);
                    yyVal = support.mlhs_add_star(((IRubyObject)yyVals[-5+yyTop]), yyVal);
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
                    yyVal = support.method_arg(support.dispatch1("on_fcall", ((IRubyObject)yyVals[0+yyTop])), 
                                            support.arg_new());
    return yyVal;
  }
};
states[513] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.appendToBlock(((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[446] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[379] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_lambda", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
                    support.popCurrentScope();
                    lexer.setLeftParenBegin(((Integer)yyVals[-2+yyTop]));
    return yyVal;
  }
};
states[312] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_class", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
                    support.popCurrentScope();
    return yyVal;
  }
};
states[245] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_arg_paren", support.escape(((IRubyObject)yyVals[-1+yyTop])));
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
                   yyVal = null;
    return yyVal;
  }
};
states[346] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.mlhs_add_star(((IRubyObject)yyVals[-2+yyTop]), null);
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
                    yyVal = support.dispatch("on_begin", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[447] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.dispatch("on_var_ref", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[380] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-2+yyTop]);
                    ((ISourcePositionHolder)yyVal).setPosition(((Token)yyVals[-3+yyTop]).getPosition());
    return yyVal;
  }
};
states[313] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = Boolean.valueOf(support.isInDef());
                    support.setInDef(false);
    return yyVal;
  }
};
states[45] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_binary", ((IRubyObject)yyVals[-2+yyTop]), "and", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[548] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[481] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
                    ((ISourcePositionHolder)yyVal).setPosition(((Token)yyVals[-2+yyTop]).getPosition());
                    lexer.setState(LexState.EXPR_BEG);
    return yyVal;
  }
};
states[347] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.mlhs_add_star(((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
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
                    lexer.setState(LexState.EXPR_ENDARG); 
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
                    yyVal = Integer.valueOf(support.getInSingle());
                    support.setInSingle(0);
                    support.pushLocalScope();
    return yyVal;
  }
};
states[549] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[482] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[415] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_symbol_literal", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[348] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.assignable(((IRubyObject)yyVals[0+yyTop]), null);
                    yyVal = supportmlhs_add_star(support.mlhs_new(), yyVal);
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
                    support.warning(ID.GROUPED_EXPRESSION, ((Token)yyVals[-3+yyTop]).getPosition(), "(...) interpreted as grouped expression");
                    yyVal = support.dispatch("on_paren", ((IRubyObject)yyVals[-2+yyTop]));
    return yyVal;
  }
};
states[516] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.arg_var(support.shadowing_lvar(((IRubyObject)yyVals[0+yyTop])));
                    yyVal = support.dispatch("on_rest_param", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[449] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     lexer.setState(LexState.EXPR_END);
                     yyVal = support.dispatch("on_symbol", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[382] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
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
                    yyVal = support.dispatch("on_sclass", ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));

                    support.popCurrentScope();
                    support.setInDef(((Boolean)yyVals[-4+yyTop]).booleanValue());
                    support.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
    return yyVal;
  }
};
states[483] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-5+yyTop]).getPosition(), ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[349] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.assignable(((IRubyObject)yyVals[-2+yyTop]), null);
                    yyVal = support.mlhs_add_star(yyVal, ((IRubyObject)yyVals[0+yyTop]));
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
                    yyVal = support.dispatch("on_paren", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[517] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_rest_param", null);
    return yyVal;
  }
};
states[383] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
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
                    if (support.isInDef() || support.isInSingle()) { 
                        support.yyerror("module definition in method body");
                    }
                    support.pushLocalScope();
    return yyVal;
  }
};
states[484] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-7+yyTop]).getPosition(), ((IRubyObject)yyVals[-7+yyTop]), ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
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
                    yyVal = support.mlhs_add_star(support.mlhs_new(), null);
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
                    yyVal = support.dispatch("on_const_path_ref", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[384] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
    return yyVal;
  }
};
states[250] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-1+yyTop]);
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
                    yyVal = support.dispatch("on_module", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
                    support.popCurrentScope();
    return yyVal;
  }
};
states[485] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-3+yyTop]).getPosition(), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[351] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.mlhs_add_star(suuport.mlhs_new(), Qnil);
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
                    yyVal = support.dispatch("on_top_const_ref", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[385] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch2("on_do_block", support.escape(((IRubyObject)yyVals[-2+yyTop])), ((IRubyObject)yyVals[-1+yyTop]));
                    support.popCurrentScope();
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
                    yyVal = support.arg_add_assocs(((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
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
                    support.setInDef(true);
                    support.pushLocalScope();
    return yyVal;
  }
};
states[486] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-5+yyTop]).getPosition(), ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), null, ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[352] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.params_new(((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, support.escape(((IRubyObject)yyVals[0+yyTop])));
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
                    yyVal = support.dispatch("on_array", support.escape(((IRubyObject)yyVals[-1+yyTop])));
    return yyVal;
  }
};
states[520] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.arg_var(support.shadowing_lvar(((IRubyObject)yyVals[0+yyTop])));
                    yyVal = support.dispatch("on_blockarg", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[386] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.method_add_block(((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[252] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.arg_add_assocs(support.arg_new(), ((IRubyObject)yyVals[-1+yyTop]));
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
                    yyVal = support.dispatch("on_def", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));

                    support.popCurrentScope();
                    support.setInDef(false);
    return yyVal;
  }
};
states[487] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-3+yyTop]).getPosition(), ((IRubyObject)yyVals[-3+yyTop]), null, ((IRubyObject)yyVals[-1+yyTop]), null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[420] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_string_concat", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[353] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.params_new(((IRubyObject)yyVals[-7+yyTop]), ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), support.escape(((IRubyObject)yyVals[0+yyTop])));
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
                    yyVal = support.dispatch("on_hash", escape(((IRubyObject)yyVals[-1+yyTop])));
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
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[454] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     lexer.setState(LexState.EXPR_END);
                     yyVal = support.dispatch("on_dyna_symbol", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[387] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_call", ((IRubyObject)yyVals[-3+yyTop]), support.symbol('.'), ((IRubyObject)yyVals[-1+yyTop]));
                    yyVal = support.method_optarg(yyVal, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[253] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.arg_add(support.arg_new(), ((IRubyObject)yyVals[0+yyTop]));
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
                    lexer.setState(LexState.EXPR_FNAME);
    return yyVal;
  }
};
states[488] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-5+yyTop]).getPosition(), ((IRubyObject)yyVals[-5+yyTop]), null, ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[421] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_string_literal", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[354] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.params_new(((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, null, support.escape(((IRubyObject)yyVals[0+yyTop])));
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
                    yyVal = support.dispatch("on_return0");
    return yyVal;
  }
};
states[19] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_var_alias", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[522] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[455] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[388] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch3(call, ((IRubyObject)yyVals[-3+yyTop]), support.string("::"), ((IRubyObject)yyVals[-1+yyTop]));
                    yyVal = support.method_optarg(yyVal, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[254] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.arg_add_optblock(((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
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
                    support.setInSingle(support.getInSingle() + 1);
                    support.pushLocalScope();
                    lexer.setState(LexState.EXPR_ENDFN); /* force for args */
    return yyVal;
  }
};
states[489] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-1+yyTop]).getPosition(), ((IRubyObject)yyVals[-1+yyTop]), null, null, null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[422] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_xstring_literal", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[355] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.params_new(((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), null, ((IRubyObject)yyVals[-1+yyTop]), support.escape(((IRubyObject)yyVals[0+yyTop])));
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
                    yyVal = support.dispatch("on_yield", 
                                          support.dispatch("on_paren", ((IRubyObject)yyVals[-1+yyTop])));
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
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[456] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[389] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.method_arg(support.dispatch("on_fcall", ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
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
                    yyVal = support.arg_add_assocs(support.arg_new(), ((IRubyObject)yyVals[-1+yyTop]));
                    yyVal = support.arg_add_optblock(yyVal, ((IRubyObject)yyVals[0+yyTop]));
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
                    yyVal = support.dispatch("on_defs", ((IRubyObject)yyVals[-7+yyTop]), ((IRubyObject)yyVals[-6+yyTop]), ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));

                    support.popCurrentScope();
                    support.setInSingle(support.getInSingle() - 1);
    return yyVal;
  }
};
states[557] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                      yyVal = null;
    return yyVal;
  }
};
states[490] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-3+yyTop]).getPosition(), null, ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[423] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_regexp_literal", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[356] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.params_new(((IRubyObject)yyVals[-3+yyTop]), null, ((IRubyObject)yyVals[-1+yyTop]), null, support.escape(((IRubyObject)yyVals[0+yyTop])));
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
                    yyVal = support.dispatch("on_yield", 
                                          support.dispatch("on_paren", 
                                                           support.arg_new()));
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
                    lexer.setState(LexState.EXPR_BEG);
    return yyVal;
  }
};
states[457] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_unary", support.string("-@"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[390] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_call", ((IRubyObject)yyVals[-3+yyTop]), support.symbol('.'), ((IRubyObject)yyVals[-1+yyTop]));
                    yyVal = support.method_optarg(yyVal, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[256] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.arg_add_optblock(support.arg_add_assocs(((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
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
                    yyVal = support.dispatch("on_break", support.arg_new());
    return yyVal;
  }
};
states[55] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = method_arg(support.dispatch("on_call", ((IRubyObject)yyVals[-3+yyTop]), "::", ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[558] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = null;
    return yyVal;
  }
};
states[491] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-5+yyTop]).getPosition(), null, ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[424] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_words_new");
                    yyVal = support.dispatch("on_array", yyVal);
    return yyVal;
  }
};
states[357] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.params_new(((IRubyObject)yyVals[-1+yyTop]), null, null, null, null);
                    support.dispatch("on_excessed_comma", yyVal);
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
                    yyVal = support.dispatch("on_yield0");
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
                    yyVal = support.dispatch("on_paren", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[458] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_unary", support.string("-@"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[391] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_call", ((IRubyObject)yyVals[-3+yyTop]), support.symbol('.'), ((IRubyObject)yyVals[-1+yyTop]));
                    yyVal = support.method_optarg(yyVal, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[324] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_next", support.arg_new());
    return yyVal;
  }
};
states[257] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.arg_add_block(support.arg_new(), ((IRubyObject)yyVals[0+yyTop]));
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
                    yyVal = support.new_args(((IRubyObject)yyVals[-1+yyTop]).getPosition(), null, ((IRubyObject)yyVals[-1+yyTop]), null, null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[425] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_array", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[358] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.params_new(((IRubyObject)yyVals[-5+yyTop]), null, ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), support.escape(((IRubyObject)yyVals[0+yyTop])));
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
                    yyVal = support.dispatch("on_defined", ((IRubyObject)yyVals[0+yyTop]));
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
                    yyVal = support.dispatch("on_call", ((IRubyObject)yyVals[-2+yyTop]), support.string("::"), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[325] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_redo");
    return yyVal;
  }
};
states[258] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = Long.valueOf(lexer.getCmdArgumentState().begin());
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
                    yyVal = support.new_args(((IRubyObject)yyVals[-3+yyTop]).getPosition(), null, ((IRubyObject)yyVals[-3+yyTop]), null, ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[426] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_words_new");
    return yyVal;
  }
};
states[359] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.params_new(((IRubyObject)yyVals[-1+yyTop]), null, null, null, support.escape(((IRubyObject)yyVals[0+yyTop])));
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
                    yyVal = support.dispatch("on_unary", support.string("not"), ((IRubyObject)yyVals[-1+yyTop]));
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
states[527] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_assoclist_from_args", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[393] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_call", ((IRubyObject)yyVals[-2+yyTop]), support.symbol('.'),
                                          support.string("call"));
                    yyVal = support.method_optarg(yyVal, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[326] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_retry");
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
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[494] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-1+yyTop]).getPosition(), null, null, ((IRubyObject)yyVals[-1+yyTop]), null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[427] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_words_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[360] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.params_new(null, ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), null, support.escape(((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[293] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_unary", support.string("not"), null); /*FIXME: null should be nil*/
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
states[394] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_call", ((IRubyObject)yyVals[-2+yyTop]), support.string("::"),
                                          support.string("call"));
                    yyVal = support.method_optarg(yyVal, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[327] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[59] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = method_add_block(support.dispatch("on_command", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[260] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[495] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[-3+yyTop]).getPosition(), null, null, ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[428] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_word_new");
                    yyVal = support.dispatch("on_word_add", yyVal, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[361] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.params_new(null, ((IRubyObject)yyVals[-5+yyTop]), ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), support.escape(((IRubyObject)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[294] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.method_arg(support.dispatch1("on_fcall", ((IRubyObject)yyVals[-1+yyTop])), 
                                            support.arg_new());
                    yyVal = support.method_add_block(yyVal, ((IRubyObject)yyVals[0+yyTop]));
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
states[529] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[-2+yyTop]).addAll(((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[395] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_super", ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[328] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
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
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[496] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((IRubyObject)yyVals[0+yyTop]).getPosition(), null, null, null, null, ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[429] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_word_add", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[362] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.params_new(null, ((IRubyObject)yyVals[-1+yyTop]), null, null, support.escape(((IRubyObject)yyVals[0+yyTop])));
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
states[530] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_assoc_new", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[396] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_zsuper");
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
states[497] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(lexer.getPosition(), null, null, null, null, null);
    return yyVal;
  }
};
states[430] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_qwords_new");
                    yyVal = support.dispatch("on_array", yyVal);
    return yyVal;
  }
};
states[363] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.params_new(null, ((IRubyObject)yyVals[-3+yyTop]), null, ((IRubyObject)yyVals[-1+yyTop]), support.escape(((IRubyObject)yyVals[0+yyTop])));
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
                    yyVal = support.method_add_block(((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[531] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_assoc_new", ((IRubyObject)yyVals[-1+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[397] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_aref", ((IRubyObject)yyVals[-3+yyTop]), support.escape(((IRubyObject)yyVals[-1+yyTop])));
    return yyVal;
  }
};
states[330] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
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
                    yyVal = support.arg_add(support.arg_new(), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[498] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("formal argument cannot be a constant");
    return yyVal;
  }
};
states[431] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_array", ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[364] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.params_new(null, null, ((IRubyObject)yyVals[-1+yyTop]), null, support.escape(((IRubyObject)yyVals[0+yyTop])));     
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
                    yyVal = ((IRubyObject)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[398] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
    return yyVal;
  }
};
states[331] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
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
                    yyVal = support.arg_add_star(support.arg_new(), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[499] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("formal argument cannot be an instance variable");
    return yyVal;
  }
};
states[432] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_qwords_new");
    return yyVal;
  }
};
states[365] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.params_new(null, null, ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), support.escape(((IRubyObject)yyVals[0+yyTop])));
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
                    yyVal = support.dispatch("on_if", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), supoprt.escape(((IRubyObject)yyVals[-1+yyTop])));
    return yyVal;
  }
};
states[399] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_brace_block", support.escape(((IRubyObject)yyVals[-2+yyTop])), ((IRubyObject)yyVals[-1+yyTop]));
                    support.popCurrentScope();
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
                    yyVal = support.arg_add(((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[500] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("formal argument cannot be a global variable");
    return yyVal;
  }
};
states[433] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_qwords_add", ((IRubyObject)yyVals[-2+yyTop]), ((IRubyObject)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[366] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.params_new(null, null, null, null, ((IRubyObject)yyVals[0+yyTop]));
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
                    yyVal = support.dispatch("on_unless", ((IRubyObject)yyVals[-4+yyTop]), ((IRubyObject)yyVals[-2+yyTop]), supoprt.escape(((IRubyObject)yyVals[-1+yyTop])));
    return yyVal;
  }
};
states[400] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
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
                    yyVal = support.arg_add_star(((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[501] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("formal argument cannot be a class variable");
    return yyVal;
  }
};
states[434] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_string_content");
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
                    lexer.getConditionState().begin();
    return yyVal;
  }
};
states[401] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_do_block", support.escape(((IRubyObject)yyVals[-2+yyTop])), ((IRubyObject)yyVals[-1+yyTop]));
                    support.popCurrentScope();
    return yyVal;
  }
};
states[334] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.dispatch("on_elsif", ((IRubyObject)yyVals[-3+yyTop]), ((IRubyObject)yyVals[-1+yyTop]), support.escape(((IRubyObject)yyVals[0+yyTop])));
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
                    yyVal = support.mrhs_add(args2mrhs(((IRubyObject)yyVals[-2+yyTop])), ((IRubyObject)yyVals[0+yyTop]));
    return yyVal;
  }
};
}
					// line 1764 "Ripper19Parser.y"

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
					// line 7919 "-"
