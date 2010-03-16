// created by jay 1.0.2 (c) 2002-2004 ats@cs.rit.edu
// skeleton Java 1.0 (c) 2002 ats@cs.rit.edu

					// line 2 "Ruby19Parser.y"
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

import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArgumentNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.AssignableNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BlockAcceptingNode;
import org.jruby.ast.BlockArgNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.BlockPassNode;
import org.jruby.ast.BreakNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarNode;
import org.jruby.ast.Colon3Node;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.DRegexpNode;
import org.jruby.ast.DStrNode;
import org.jruby.ast.DSymbolNode;
import org.jruby.ast.DXStrNode;
import org.jruby.ast.DefinedNode;
import org.jruby.ast.DefnNode;
import org.jruby.ast.DefsNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.EnsureNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.FCallNoArgBlockNode;
import org.jruby.ast.FCallNoArgNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FloatNode;
import org.jruby.ast.ForNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.Hash19Node;
import org.jruby.ast.IfNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.LambdaNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.LiteralNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgn19Node;
import org.jruby.ast.NextNode;
import org.jruby.ast.NilImplicitNode;
import org.jruby.ast.NilNode;
import org.jruby.ast.Node;
import org.jruby.ast.NotNode;
import org.jruby.ast.OpAsgnAndNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OptArgNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.PreExeNode;
import org.jruby.ast.RedoNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.RestArgNode;
import org.jruby.ast.RetryNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.StarNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.UnnamedRestArgNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.VAliasNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.ZArrayNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.ast.ZYieldNode;
import org.jruby.ast.types.ILiteralNode;
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

public class Ruby19Parser implements RubyParser {
    protected ParserSupport19 support;
    protected RubyYaccLexer lexer;

    public Ruby19Parser() {
        this(new ParserSupport19());
    }

    public Ruby19Parser(ParserSupport19 support) {
        this.support = support;
        lexer = new RubyYaccLexer(false);
        lexer.setParserSupport(support);
        support.setLexer(lexer);
    }

    public void setWarnings(IRubyWarnings warnings) {
        support.setWarnings(warnings);
        lexer.setWarnings(warnings);
    }
					// line 142 "-"
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
  public static final int yyErrorCode = 256;

  /** number of final state.
    */
  protected static final int yyFinal = 1;

  /** parser tables.
      Order is mandated by <i>jay</i>.
    */
  protected static final short[] yyLhs = {
//yyLhs 542
    -1,   117,     0,    34,    33,    35,    35,    35,    35,   120,
    36,    36,    36,    36,    36,    36,    36,    36,    36,    36,
   121,    36,    36,    36,    36,    36,    36,    36,    36,    36,
    36,    36,    36,    36,    36,    37,    37,    37,    37,    37,
    37,    41,    32,    32,    32,    32,    32,    55,    55,    55,
   122,    94,    40,    40,    40,    40,    40,    40,    40,    40,
    95,    95,   106,   106,    96,    96,    96,    96,    96,    96,
    96,    96,    96,    96,    67,    67,    81,    81,    85,    85,
    68,    68,    68,    68,    68,    68,    68,    68,    73,    73,
    73,    73,    73,    73,    73,    73,     7,     7,    31,    31,
    31,     8,     8,     8,     8,     8,    99,    99,   100,   100,
    57,   123,    57,     9,     9,     9,     9,     9,     9,     9,
     9,     9,     9,     9,     9,     9,     9,     9,     9,     9,
     9,     9,     9,     9,     9,     9,     9,     9,     9,     9,
     9,     9,   115,   115,   115,   115,   115,   115,   115,   115,
   115,   115,   115,   115,   115,   115,   115,   115,   115,   115,
   115,   115,   115,   115,   115,   115,   115,   115,   115,   115,
   115,   115,   115,   115,   115,   115,   115,   115,   115,   115,
   115,   115,   115,   115,    38,    38,    38,    38,    38,    38,
    38,    38,    38,    38,    38,    38,    38,    38,    38,    38,
    38,    38,    38,    38,    38,    38,    38,    38,    38,    38,
    38,    38,    38,    38,    38,    38,    38,    38,    38,    38,
    38,    38,    38,    38,    38,    38,    38,    38,    38,    69,
    72,    72,    72,    72,    49,    53,    53,   109,   109,    47,
    47,    47,    47,    47,   126,    51,    88,    87,    87,    87,
    75,    75,    75,    75,    66,    66,    66,    39,    39,    39,
    39,    39,    39,    39,    39,    39,    39,   127,    39,    39,
    39,    39,    39,    39,    39,    39,    39,    39,    39,    39,
    39,    39,    39,    39,    39,    39,    39,   129,   131,    39,
   132,   133,    39,    39,    39,   134,   135,    39,   136,    39,
   138,   139,    39,   140,    39,   141,    39,   142,   143,    39,
    39,    39,    39,    39,    42,   128,   128,   128,   130,   130,
    45,    45,    43,    43,   108,   108,   110,   110,    80,    80,
   111,   111,   111,   111,   111,   111,   111,   111,   111,    63,
    63,    63,    63,    63,    63,    63,    63,    63,    63,    63,
    63,    63,    63,    63,    65,    65,    64,    64,    64,   103,
   103,   102,   102,   112,   112,   144,   105,    62,    62,   104,
   104,   145,    93,    54,    54,    54,    24,    24,    24,    24,
    24,    24,    24,    24,    24,   146,    92,   147,    92,    70,
    44,    44,    97,    97,    71,    71,    71,    46,    46,    48,
    48,    28,    28,    28,    16,    17,    17,    17,    18,    19,
    20,    25,    25,    77,    77,    27,    27,    26,    26,    76,
    76,    21,    21,    22,    22,    23,   148,    23,   149,    23,
    58,    58,    58,    58,     3,     2,     2,     2,     2,    30,
    29,    29,    29,    29,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,    52,    98,    59,    59,
    50,   150,    50,    50,    61,    61,    60,    60,    60,    60,
    60,    60,    60,    60,    60,    60,    60,    60,    60,    60,
    60,   116,   116,   116,   116,    10,    10,   101,   101,    78,
    78,    56,   107,    86,    86,    79,    79,    12,    12,    14,
    14,    13,    13,    91,    90,    90,    15,   151,    15,    84,
    84,    82,    82,    83,    83,     4,     4,     4,     5,     5,
     5,     5,     6,     6,     6,    11,    11,   118,   118,   124,
   124,   113,   114,   125,   125,   125,   137,   137,   119,   119,
    74,    89,
    }, yyLen = {
//yyLen 542
     2,     0,     2,     4,     2,     1,     1,     3,     2,     0,
     4,     3,     3,     3,     2,     3,     3,     3,     3,     3,
     0,     5,     4,     3,     3,     3,     6,     5,     5,     5,
     3,     3,     3,     3,     1,     1,     3,     3,     2,     2,
     1,     1,     1,     1,     2,     2,     2,     1,     4,     4,
     0,     5,     2,     3,     4,     5,     4,     5,     2,     2,
     1,     3,     1,     3,     1,     2,     3,     5,     2,     4,
     2,     4,     1,     3,     1,     3,     2,     3,     1,     3,
     1,     4,     3,     3,     3,     3,     2,     1,     1,     4,
     3,     3,     3,     3,     2,     1,     1,     1,     2,     1,
     3,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     0,     4,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     3,     5,     3,     5,     6,     5,
     5,     5,     5,     4,     3,     3,     3,     3,     3,     3,
     3,     3,     3,     4,     4,     2,     2,     3,     3,     3,
     3,     3,     3,     3,     3,     3,     3,     3,     3,     3,
     2,     2,     3,     3,     3,     3,     3,     6,     1,     1,
     1,     2,     4,     2,     3,     1,     1,     1,     1,     1,
     2,     2,     4,     1,     0,     2,     2,     2,     1,     1,
     1,     2,     3,     4,     3,     4,     2,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     3,     0,     4,     3,
     3,     2,     3,     3,     1,     4,     3,     1,     5,     4,
     3,     2,     1,     2,     2,     6,     6,     0,     0,     7,
     0,     0,     7,     5,     4,     0,     0,     9,     0,     6,
     0,     0,     8,     0,     5,     0,     6,     0,     0,     9,
     1,     1,     1,     1,     1,     1,     1,     2,     1,     1,
     1,     5,     1,     2,     1,     1,     1,     3,     1,     3,
     1,     4,     6,     3,     5,     2,     4,     1,     3,     6,
     8,     4,     6,     4,     2,     6,     2,     4,     6,     2,
     4,     2,     4,     1,     1,     1,     3,     1,     4,     1,
     2,     1,     3,     1,     1,     0,     3,     4,     2,     3,
     3,     0,     5,     2,     4,     4,     2,     4,     4,     3,
     3,     3,     2,     1,     4,     0,     5,     0,     5,     5,
     1,     1,     6,     0,     1,     1,     1,     2,     1,     2,
     1,     1,     1,     1,     1,     1,     1,     2,     3,     3,
     3,     3,     3,     0,     3,     1,     2,     3,     3,     0,
     3,     0,     2,     0,     2,     1,     0,     3,     0,     4,
     1,     1,     1,     1,     2,     1,     1,     1,     1,     3,
     1,     1,     2,     2,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     0,     4,     2,     3,     2,     6,     8,     4,     6,
     4,     6,     2,     4,     6,     2,     4,     2,     4,     1,
     0,     1,     1,     1,     1,     1,     1,     1,     3,     1,
     3,     3,     3,     1,     3,     1,     3,     1,     1,     2,
     1,     1,     1,     2,     2,     0,     1,     0,     4,     1,
     2,     1,     3,     3,     2,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     0,     1,     0,
     1,     2,     2,     0,     1,     1,     1,     1,     1,     2,
     0,     0,
    }, yyDefRed = {
//yyDefRed 945
     1,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   287,   290,     0,     0,     0,   312,   313,     0,
     0,     0,   450,   449,   451,   452,     0,     0,     0,    20,
     0,   454,   453,   455,     0,     0,   446,   445,     0,   448,
   405,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   421,   423,   423,     0,     0,   365,   458,
   459,   440,   441,     0,   402,     0,   258,     0,   406,   259,
   260,     0,   261,   262,   257,   401,   403,    35,     2,     0,
     0,     0,     0,     0,     0,     0,   263,     0,    43,     0,
     0,    74,     0,     5,     0,     0,    60,     0,     0,   310,
   311,   274,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   456,     0,    99,     0,   314,     0,   264,   303,
   152,   163,   153,   176,   149,   169,   159,   158,   174,   157,
   156,   151,   177,   161,   150,   164,   168,   170,   162,   155,
   171,   178,   173,     0,     0,     0,     0,   148,   167,   166,
   179,   180,   181,   182,   183,   147,   154,   145,   146,     0,
     0,     0,     0,   103,     0,   137,   138,   134,   116,   117,
   118,   125,   122,   124,   119,   120,   139,   140,   126,   127,
   507,   131,   130,   115,   136,   133,   132,   128,   129,   123,
   121,   113,   135,   114,   141,   305,   104,     0,   506,   105,
   172,   165,   175,   160,   142,   143,   144,   101,   102,   107,
   106,   109,     0,   108,   110,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   536,   537,     0,     0,
     0,   538,     0,     0,     0,     0,     0,     0,   324,   325,
     0,     0,     0,     0,     0,     0,   239,    45,     0,     0,
     0,   511,   243,    46,    44,     0,    59,     0,     0,   382,
    58,     0,    38,     0,     9,   530,     0,     0,     0,   205,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   230,     0,     0,     0,   509,     0,     0,     0,     0,
     0,     0,     0,     0,   221,    39,   220,   437,   436,   438,
   434,   435,     0,     0,     0,     0,     0,     0,     0,     0,
   284,     0,   387,   385,   376,     0,   281,   407,   283,     4,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   371,   373,     0,     0,     0,     0,
     0,     0,    76,     0,     0,     0,     0,     0,     0,     0,
   442,   443,     0,    96,     0,    98,     0,   461,   298,   460,
     0,     0,     0,     0,     0,     0,   525,   526,   307,   111,
     0,     0,   266,     0,   316,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   539,     0,     0,
     0,     0,     0,     0,   295,   514,   251,   246,     0,     0,
   240,   249,     0,   241,     0,   276,     0,   245,   238,   237,
     0,     0,   280,    11,    13,    12,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   269,     0,     0,
   272,     0,   534,   231,     0,   233,   510,   273,     0,    78,
     0,     0,     0,     0,     0,   428,   426,   439,   425,   424,
   408,   422,   409,   410,   411,   412,   415,     0,   417,   418,
     0,     0,   483,   482,   481,   484,     0,     0,   498,   497,
   502,   501,   487,     0,     0,     0,   495,     0,     0,     0,
     0,   479,   489,   485,     0,     0,    50,    53,     0,    15,
    16,    17,    18,    19,    36,    37,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   520,     0,     0,   521,   380,     0,
     0,     0,     0,   379,     0,   381,     0,   518,   519,     0,
     0,    30,     0,     0,    23,     0,    31,   250,     0,     0,
     0,     0,    77,    24,    33,     0,    25,     0,     0,   463,
     0,     0,     0,     0,     0,     0,   100,     0,     0,     0,
     0,     0,     0,     0,     0,   395,     0,     0,   396,     0,
     0,   322,     0,   317,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   294,   319,   288,   318,   291,     0,     0,
     0,     0,     0,     0,   513,     0,     0,     0,   247,   512,
   275,   531,   234,   279,    10,     0,     0,    22,     0,     0,
     0,     0,   268,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   414,   416,   420,     0,   486,     0,     0,
   326,     0,   328,     0,     0,   499,   503,     0,   477,     0,
   359,   368,     0,     0,   366,     0,   472,     0,   475,   357,
     0,   355,     0,   354,     0,     0,     0,     0,     0,     0,
   236,     0,   377,   235,     0,     0,   378,     0,     0,     0,
    48,   374,    49,   375,     0,     0,     0,    75,     0,     0,
     0,   301,     0,     0,   384,   304,   508,     0,   465,     0,
   308,   112,     0,     0,   398,   323,     0,     3,   400,     0,
   320,     0,     0,     0,     0,     0,     0,   293,     0,     0,
     0,     0,     0,     0,   253,   242,   278,    21,   232,    79,
     0,     0,   430,   431,   432,   427,   433,   491,     0,     0,
     0,     0,   488,     0,     0,   504,   363,     0,   361,   364,
     0,     0,     0,     0,   490,     0,   496,     0,     0,     0,
     0,     0,     0,   353,     0,   493,     0,     0,     0,     0,
     0,    27,     0,    28,     0,    55,    29,     0,     0,    57,
     0,   532,     0,     0,     0,     0,     0,     0,   462,   299,
   464,   306,     0,     0,     0,     0,     0,   397,     0,   399,
     0,   285,     0,   286,   252,     0,     0,     0,   296,   429,
   327,     0,     0,     0,   329,   367,     0,   478,     0,   370,
   369,     0,   470,     0,   468,     0,   473,   476,     0,     0,
   351,     0,     0,   346,     0,   349,   356,   388,   386,     0,
     0,   372,    26,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   390,   389,   391,   289,   292,     0,     0,
     0,     0,     0,   362,     0,     0,     0,     0,     0,     0,
     0,   358,     0,     0,     0,     0,   494,    51,   302,     0,
     0,     0,     0,     0,     0,   392,     0,     0,     0,     0,
   471,     0,   466,   469,   474,   271,     0,   352,     0,   343,
     0,   341,     0,   347,   350,   309,     0,   321,   297,     0,
     0,     0,     0,     0,     0,     0,     0,   467,   345,     0,
   339,   342,   348,     0,   340,
    }, yyDgoto = {
//yyDgoto 152
     1,   218,   300,    64,   113,   585,   553,   114,   210,   547,
   492,   388,   493,   494,   495,   197,    66,    67,    68,    69,
    70,   303,   302,   469,    71,    72,    73,   477,    74,    75,
    76,   115,    77,   215,   216,    79,    80,    81,    82,    83,
    84,   220,   270,   730,   874,   731,   723,   428,   727,   555,
   378,   256,    86,   692,    87,    88,   496,   212,   755,   222,
   591,   592,   498,   780,   681,   682,   566,    90,    91,   248,
   406,   597,   280,   223,    93,   249,   309,   307,   499,   500,
   661,    94,   250,   251,   287,   460,   782,   420,   252,   421,
   668,   765,   316,   355,   507,    95,    96,   391,   224,   213,
   214,   502,   767,   671,   674,   310,   278,   785,   240,   430,
   662,   663,   768,   425,   698,   199,   503,     2,   229,   230,
   436,   267,   685,   594,   426,   453,   257,   449,   395,   232,
   615,   740,   233,   741,   623,   878,   581,   396,   578,   807,
   383,   385,   593,   812,   311,   542,   505,   504,   652,   651,
   580,   384,
    }, yySindex = {
//yySindex 945
     0,     0, 14227, 14474, 16811, 17180, 17888, 17780, 14227, 16442,
 16442, 12613,     0,     0, 16934, 14597, 14597,     0,     0, 14597,
  -250,  -238,     0,     0,     0,     0, 15335, 17672,   133,     0,
  -201,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 16565, 16565,  -124,  -141, 14351, 16442, 14966, 15458,  5733,
 16565, 16688, 17995,     0,     0,     0,   185,   262,     0,     0,
     0,     0,     0,     0,     0,   -45,     0,   -48,     0,     0,
     0,  -188,     0,     0,     0,     0,     0,     0,     0,   216,
   740,   -22,  4610,     0,    58,   -44,     0,  -224,     0,    11,
   313,     0,   312,     0, 17057,   393,     0,   176,   740,     0,
     0,     0,  -250,  -238,   165,   133,     0,     0,  -101, 16442,
    54, 14227,     0,   -45,     0,     7,     0,   -32,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   -15,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   472,     0,     0,   264,   269,   236,     0,   -22,
    55,   219,   225,   512,   242,    55,     0,     0,   216,   323,
   540,     0, 16442, 16442,   297,     0,   475,     0,     0,     0,
   340, 16565, 16565, 16565, 16565,  4610,     0,     0,   284,   585,
   591,     0,     0,     0,     0,  3181,     0, 14597, 14597,     0,
     0,  4167,     0,   -74,     0,     0, 15581,   288, 14227,     0,
   477,   350,   351,   353,   342, 14351,   337,     0,   133,   -22,
   338,     0,   119,   289,   284,     0,   289,   327,   387, 17303,
     0,   509,     0,   657,     0,     0,     0,     0,     0,     0,
     0,     0,   -90,   444,   542,   273,   331,   554,   347,   171,
     0,  2130,     0,     0,     0,   366,     0,     0,     0,     0,
 14103, 16442, 16442, 16442, 16442, 14474, 16442, 16442, 16565, 16565,
 16565, 16565, 16565, 16565, 16565, 16565, 16565, 16565, 16565, 16565,
 16565, 16565, 16565, 16565, 16565, 16565, 16565, 16565, 16565, 16565,
 16565, 16565, 16565, 16565,     0,     0,  2198,  2643, 14597, 18542,
 18542, 16688,     0, 15704, 14351, 13980,   681, 15704, 16688,   391,
     0,     0,   -22,     0,     0,     0,   216,     0,     0,     0,
  3666,  4740, 14597, 14227, 16442,  2263,     0,     0,     0,     0,
 15827,   467,     0,   342,     0, 14227,   470,  5233,  6353, 14597,
 16565, 16565, 16565, 14227,   323, 15950,   474,     0,    38,    38,
     0, 18157, 18212, 14597,     0,     0,     0,     0, 16565, 14720,
     0,     0, 15089,     0,   133,     0,   398,     0,     0,     0,
   133,    46,     0,     0,     0,     0, 17780, 16442,  4610, 14227,
   386,  5233,  6353, 16565, 16565, 16565,   133,     0,     0,   133,
     0, 15212,     0,     0, 15458,     0,     0,     0,     0,     0,
   715, 18267, 18322, 14597, 17303,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   101,     0,     0,
   735,   701,     0,     0,     0,     0,  1160,  1671,     0,     0,
     0,     0,     0,   462,   476,   739,     0,   725,  -121,   741,
   744,     0,     0,     0,  -183,  -183,     0,     0,   740,     0,
     0,     0,     0,     0,     0,     0,   350,  2770,  2770,  2770,
  2770,  1218,  1218,  4244,  3714,  2770,  2770,  2728,  2728,   728,
   728,   350,  1556,   350,   350,   354,   354,  1218,  1218,  1452,
  1452,  2588,  -183,   459,     0,   460,  -238,     0,     0,   461,
     0,   481,  -238,     0,     0,     0,   133,     0,     0,  -238,
  -238,     0,  4610, 16565,     0,  4679,     0,     0,   755,   133,
 17303,   775,     0,     0,     0,     0,     0,  5170,   216,     0,
 16442, 14227,  -238,     0,     0,  -238,     0,   133,   562,    46,
  1671,   216, 14227, 18102, 17780,     0,     0,   498,     0, 14227,
   575,     0,   341,     0,   517,   519,   520,   481,   133,  4679,
   467,   584,    78,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   133, 16442,     0, 16565,   284,   591,     0,     0,
     0,     0,     0,     0,     0,    46,   496,     0,   350,   350,
  4610,     0,     0,   289, 17303,     0,     0,     0,     0,   133,
   715, 14227,   376,     0,     0,     0, 16565,     0,  1160,   650,
     0,   818,     0,   133,   725,     0,     0,   889,     0,  1023,
     0,     0, 14227, 14227,     0,  1671,     0,  1671,     0,     0,
  1751,     0, 14227,     0, 14227,  -183,   817, 14227, 16688, 16688,
     0,   366,     0,     0, 16688, 16565,     0,   366,   541,   546,
     0,     0,     0,     0,     0, 16565, 16073,     0,   715, 17303,
 16565,     0,   216,   621,     0,     0,     0,   133,     0,   628,
     0,     0, 17426,    55,     0,     0, 14227,     0,     0, 16442,
     0,   633, 16565, 16565, 16565,   567,   641,     0, 16196, 14227,
 14227, 14227,     0,    38,     0,     0,     0,     0,     0,     0,
     0,   549,     0,     0,     0,     0,     0,     0,   133,  1628,
   864,  1615,     0,   133,   871,     0,     0,   877,     0,     0,
   664,   574,   896,   902,     0,   903,     0,   871,   872,   904,
   725,   906,   907,     0,   594,     0,   691,   600, 14227, 16565,
   700,     0,  4610,     0,  4610,     0,     0,  4610,  4610,     0,
 16688,     0,  4610, 16565,     0,   715,  4610, 14227,     0,     0,
     0,     0,  2263,   656,     0,   532,     0,     0, 14227,     0,
    55,     0, 16565,     0,     0,    36,   706,   708,     0,     0,
     0,   930,  1628,   679,     0,     0,   889,     0,  1023,     0,
     0,   889,     0,  1671,     0,   889,     0,     0, 17549,   889,
     0,   618,  2281,     0,  2281,     0,     0,     0,     0,   616,
  4610,     0,     0,  4610,     0,   717, 14227,     0, 18377, 18432,
 14597,   264, 14227,     0,     0,     0,     0,     0, 14227,  1628,
   930,  1628,   935,     0,   871,   938,   871,   871,   673,   560,
   871,     0,   947,   952,   953,   871,     0,     0,     0,   737,
     0,     0,     0,     0,   133,     0,   341,   743,   930,  1628,
     0,   889,     0,     0,     0,     0, 18487,     0,   889,     0,
  2281,     0,   889,     0,     0,     0,     0,     0,     0,   930,
   871,     0,     0,   871,   964,   871,   871,     0,     0,   889,
     0,     0,     0,   871,     0,
    }, yyRindex = {
//yyRindex 945
     0,     0,   168,     0,     0,     0,     0,     0,   294,     0,
     0,   742,     0,     0,     0, 12951, 13057,     0,     0, 13199,
  4495,  4002,     0,     0,     0,     0,     0,     0, 16319,     0,
     0,     0,     0,     0,  1907,  3016,     0,     0,  2030,     0,
     0,     0,     0,     0,     0,   136,     0,   666,   649,    42,
     0,     0,   698,     0,     0,     0,   797,   214,     0,     0,
     0,     0,     0, 13302,     0, 14843,     0,  6717,     0,     0,
     0,  6818,     0,     0,     0,     0,     0,     0,     0,   370,
   761, 11139,  1251,  6962,  1587,     0,     0, 13801,     0, 13416,
     0,     0,     0,     0,    63,     0,     0,     0,  1006,     0,
     0,     0,  7066,  6023,     0,   675, 11578, 11704,     0,     0,
     0,   136,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,  1424,  2146,  2269,  2762,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  3255,
  3606,  3748,  4241,     0,  5076,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0, 12674,     0,     0,   362,     0,     0,  6137,  5102,
     0,     0,  6469,     0,     0,     0,     0,     0,   742,     0,
   751,     0,     0,     0,     0,   511,     0,   836,     0,     0,
     0,     0,     0,     0,     0,  1651,     0,     0, 13661,  1787,
  1787,     0,     0,     0,     0,   686,     0,     0,    35,     0,
     0,   686,     0,     0,     0,     0,     0,     0,    28,     0,
     0,  7427,  7179,  7311, 13550,   136,     0,   120,   686,   129,
     0,     0,   684,   684,     0,     0,   677,     0,     0,     0,
  1557,     0,  1559,    64,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,    53,     0,     0,     0,   843,     0,     0,     0,     0,
   446,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     6,     0,
     0,     0,     0,     0,   136,   116,   139,     0,     0,     0,
     0,     0,   233,     0, 12094,     0,     0,     0,     0,     0,
     0,     0,     6,   294,     0,   241,     0,     0,     0,     0,
    51,   383,     0,  6603,     0,   730, 12220,     0,     0,     6,
     0,     0,     0,    75,     0,     0,     0,     0,     0,     0,
   847,     0,     0,     6,     0,     0,     0,     0,     0,  4613,
     0,     0,  4613,     0,   686,     0,     0,     0,     0,     0,
   686,   686,     0,     0,     0,     0,     0,     0, 10403,    28,
     0,     0,     0,     0,     0,     0,   686,     0,   169,   686,
     0,   702,     0,     0,   -77,     0,     0,     0,  1774,     0,
   149,     0,     0,     6,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,    79,     0,     0,     0,     0,     0,   140,     0,     0,
     0,     0,     0,   117,     0,    50,     0,   -91,     0,    50,
    50,     0,     0,     0, 12352, 12489,     0,     0,  1144,     0,
     0,     0,     0,     0,     0,     0,  7528,  9492,  9614,  9732,
  9829,  9037,  9157,  9915, 10188, 10005, 10102, 10278, 10318,  8457,
  8579,  7643,  8702,  7776,  7891,  8240,  8353,  9277,  9374,  8820,
  8928,   986, 12352,  4856,     0,  4979,  4372,     0,     0,  5349,
  3386,  5472, 14843,     0,  3509,     0,   707,     0,     0,  5595,
  5595,     0, 10499,     0,     0, 12770,     0,     0,     0,   686,
     0,   154,     0,     0,     0, 10226,     0, 11395,     0,     0,
     0,   294,  6271, 11836, 11962,     0,     0,   707,     0,   686,
   141,     0,   294,     0,     0,     0,    47,   197,     0,   571,
   788,     0,   788,     0,  2400,  2523,  2893,  3879,   707, 11456,
   788,     0,     0,     0,     0,     0,     0,     0,   561,  1121,
  1650,   674,   707,     0,     0,     0, 13704,  1787,     0,     0,
     0,     0,     0,     0,     0,   686,     0,     0,  7992,  8108,
 10584,   121,     0,   684,     0,   781,  1149,  1222,  1468,   707,
   181,    28,     0,     0,     0,     0,     0,     0,     0,   142,
     0,   144,     0,   686,    35,     0,     0,     0,     0,     0,
     0,     0,    59,    28,     0,     0,     0,     0,     0,     0,
   695,     0,    59,     0,    28, 12489,     0,    59,     0,     0,
     0,  1363,     0,     0,     0,     0,     0, 13765, 12850,     0,
     0,     0,     0,     0, 11241,     0,     0,     0,   187,     0,
     0,     0,     0,     0,     0,     0,     0,   686,     0,     0,
     0,     0,     0,     0,     0,     0,    59,     0,     0,     0,
     0,     0,     0,     0,     0,  5922,     0,     0,     0,    85,
    59,    59,   850,     0,     0,     0,     0,     0,     0,     0,
  1499,     0,     0,     0,     0,     0,     0,     0,   686,     0,
   146,     0,     0,   686,    50,     0,     0,   118,     0,     0,
     0,     0,    50,    50,     0,    50,     0,    50,   161,    82,
   695,    82,    82,     0,     0,     0,     0,     0,    28,     0,
     0,     0, 10645,     0, 10742,     0,     0, 10828, 10937,     0,
     0,     0, 10998,     0, 13852,   209, 11084,   294,     0,     0,
     0,     0,   241,     0,   736,     0,   764,     0,   294,     0,
     0,     0,     0,     0,     0,   788,     0,     0,     0,     0,
     0,   152,     0,   173,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,    86,     0,     0,     0,     0,     0,     0,     0,
 11181,     0,     0, 11305, 13889,     0,   294,   792,     0,     0,
     6,   362,   730,     0,     0,     0,     0,     0,    59,     0,
   184,     0,   188,     0,    50,    50,    50,    50,     0,   174,
    82,     0,    82,    82,    82,    82,     0,     0,     0,     0,
  1757,  1785,  2148,   614,   707,     0,   788,     0,   198,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   665,     0,     0,   201,
    50,   927,  1281,    82,    82,    82,    82,     0,     0,     0,
     0,     0,     0,    82,     0,
    }, yyGindex = {
//yyGindex 152
     0,   504,     0,    -5,   177,  -240,     0,   -47,    20,    -6,
  -312,     0,     0,     0,   620,     0,     0,     0,   987,     0,
     0,     0,   607,  -228,     0,     0,     0,     0,     0,     0,
    27,  1051,    33,   406,  -342,     0,   164,  1170,  1338,    72,
    16,    57,     4,  -386,     0,   160,     0,   680,     0,   195,
     0,    18,  1066,   105,     0,     0,  -599,     0,     0,   655,
  -249,   261,     0,     0,     0,  -370,  -126,   -43,   -13,   799,
  -375,     0,     0,    91,   759,   106,     0,     0,  5452,   402,
  -677,     0,   -37,  -197,     0,  -400,   232,  -208,   -87,     0,
   936,  -278,  1015,     0,  -439,  1073,   167,   218,   609,     0,
   -23,  -605,     0,  -474,     0,     0,  -175,  -773,     0,  -355,
  -613,   433,   257,   304,  -311,     0,  -598,     0,     1,  1018,
     0,     0,     0,     0,    -4,  -246,     0,     0,  -200,     0,
  -366,     0,     0,     0,     0,     0,     0,    17,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,
    };
    protected static final short[] yyTable = Ruby19YyTables.yyTable();
    protected static final short[] yyCheck = Ruby19YyTables.yyCheck();

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
    "tREGEXP_END","tLOWEST",
    };

  /** printable rules for debugging.
    */
  protected static final String [] yyRule = {
    "$accept : program",
    "$$1 :",
    "program : $$1 compstmt",
    "bodystmt : compstmt opt_rescue opt_else opt_ensure",
    "compstmt : stmts opt_terms",
    "stmts : none",
    "stmts : stmt",
    "stmts : stmts terms stmt",
    "stmts : error stmt",
    "$$2 :",
    "stmt : kALIAS fitem $$2 fitem",
    "stmt : kALIAS tGVAR tGVAR",
    "stmt : kALIAS tGVAR tBACK_REF",
    "stmt : kALIAS tGVAR tNTH_REF",
    "stmt : kUNDEF undef_list",
    "stmt : stmt kIF_MOD expr_value",
    "stmt : stmt kUNLESS_MOD expr_value",
    "stmt : stmt kWHILE_MOD expr_value",
    "stmt : stmt kUNTIL_MOD expr_value",
    "stmt : stmt kRESCUE_MOD stmt",
    "$$3 :",
    "stmt : klBEGIN $$3 tLCURLY compstmt tRCURLY",
    "stmt : klEND tLCURLY compstmt tRCURLY",
    "stmt : lhs '=' command_call",
    "stmt : mlhs '=' command_call",
    "stmt : var_lhs tOP_ASGN command_call",
    "stmt : primary_value '[' opt_call_args rbracket tOP_ASGN command_call",
    "stmt : primary_value tDOT tIDENTIFIER tOP_ASGN command_call",
    "stmt : primary_value tDOT tCONSTANT tOP_ASGN command_call",
    "stmt : primary_value tCOLON2 tIDENTIFIER tOP_ASGN command_call",
    "stmt : backref tOP_ASGN command_call",
    "stmt : lhs '=' mrhs",
    "stmt : mlhs '=' arg_value",
    "stmt : mlhs '=' mrhs",
    "stmt : expr",
    "expr : command_call",
    "expr : expr kAND expr",
    "expr : expr kOR expr",
    "expr : kNOT expr",
    "expr : tBANG command_call",
    "expr : arg",
    "expr_value : expr",
    "command_call : command",
    "command_call : block_command",
    "command_call : kRETURN call_args",
    "command_call : kBREAK call_args",
    "command_call : kNEXT call_args",
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
    "mlhs_node : primary_value '[' opt_call_args rbracket",
    "mlhs_node : primary_value tDOT tIDENTIFIER",
    "mlhs_node : primary_value tCOLON2 tIDENTIFIER",
    "mlhs_node : primary_value tDOT tCONSTANT",
    "mlhs_node : primary_value tCOLON2 tCONSTANT",
    "mlhs_node : tCOLON3 tCONSTANT",
    "mlhs_node : backref",
    "lhs : variable",
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
    "opt_bv_decl : none",
    "opt_bv_decl : ';' bv_decls",
    "bv_decls : bvar",
    "bv_decls : bv_decls ',' bvar",
    "bvar : tIDENTIFIER",
    "bvar : f_bad_arg",
    "$$21 :",
    "lambda : $$21 f_larglist lambda_body",
    "f_larglist : tLPAREN2 f_args opt_bv_decl rparen",
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
            yyToken = yyLex.advance() ? yyLex.token() : 0;
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
        yyVal = yyDefault(yyV > yyTop ? null : yyVals[yyV]);
        switch (yyN) {
case 1: yyVal = case1_line272(support, lexer, yyVal, yyVals, yyTop); // line 272
break;
case 2: yyVal = case2_line275(support, lexer, yyVal, yyVals, yyTop); // line 275
break;
case 3: yyVal = case3_line288(support, lexer, yyVal, yyVals, yyTop); // line 288
break;
case 4: yyVal = case4_line305(support, lexer, yyVal, yyVals, yyTop); // line 305
break;
case 6: yyVal = case6_line313(support, lexer, yyVal, yyVals, yyTop); // line 313
break;
case 7: yyVal = case7_line316(support, lexer, yyVal, yyVals, yyTop); // line 316
break;
case 8: yyVal = case8_line319(support, lexer, yyVal, yyVals, yyTop); // line 319
break;
case 9: yyVal = case9_line323(support, lexer, yyVal, yyVals, yyTop); // line 323
break;
case 10: yyVal = case10_line325(support, lexer, yyVal, yyVals, yyTop); // line 325
break;
case 11: yyVal = case11_line328(support, lexer, yyVal, yyVals, yyTop); // line 328
break;
case 12: yyVal = case12_line331(support, lexer, yyVal, yyVals, yyTop); // line 331
break;
case 13: yyVal = case13_line334(support, lexer, yyVal, yyVals, yyTop); // line 334
break;
case 14: yyVal = case14_line337(support, lexer, yyVal, yyVals, yyTop); // line 337
break;
case 15: yyVal = case15_line340(support, lexer, yyVal, yyVals, yyTop); // line 340
break;
case 16: yyVal = case16_line343(support, lexer, yyVal, yyVals, yyTop); // line 343
break;
case 17: yyVal = case17_line346(support, lexer, yyVal, yyVals, yyTop); // line 346
break;
case 18: yyVal = case18_line353(support, lexer, yyVal, yyVals, yyTop); // line 353
break;
case 19: yyVal = case19_line360(support, lexer, yyVal, yyVals, yyTop); // line 360
break;
case 20: yyVal = case20_line364(support, lexer, yyVal, yyVals, yyTop); // line 364
break;
case 21: yyVal = case21_line369(support, lexer, yyVal, yyVals, yyTop); // line 369
break;
case 22: yyVal = case22_line374(support, lexer, yyVal, yyVals, yyTop); // line 374
break;
case 23: yyVal = case23_line380(support, lexer, yyVal, yyVals, yyTop); // line 380
break;
case 24: yyVal = case24_line384(support, lexer, yyVal, yyVals, yyTop); // line 384
break;
case 25: yyVal = case25_line389(support, lexer, yyVal, yyVals, yyTop); // line 389
break;
case 26: yyVal = case26_line406(support, lexer, yyVal, yyVals, yyTop); // line 406
break;
case 27: yyVal = case27_line410(support, lexer, yyVal, yyVals, yyTop); // line 410
break;
case 28: yyVal = case28_line413(support, lexer, yyVal, yyVals, yyTop); // line 413
break;
case 29: yyVal = case29_line416(support, lexer, yyVal, yyVals, yyTop); // line 416
break;
case 30: yyVal = case30_line419(support, lexer, yyVal, yyVals, yyTop); // line 419
break;
case 31: yyVal = case31_line422(support, lexer, yyVal, yyVals, yyTop); // line 422
break;
case 32: yyVal = case32_line425(support, lexer, yyVal, yyVals, yyTop); // line 425
break;
case 33: yyVal = case33_line429(support, lexer, yyVal, yyVals, yyTop); // line 429
break;
case 36: yyVal = case36_line438(support, lexer, yyVal, yyVals, yyTop); // line 438
break;
case 37: yyVal = case37_line441(support, lexer, yyVal, yyVals, yyTop); // line 441
break;
case 38: yyVal = case38_line444(support, lexer, yyVal, yyVals, yyTop); // line 444
break;
case 39: yyVal = case39_line447(support, lexer, yyVal, yyVals, yyTop); // line 447
break;
case 41: yyVal = case41_line452(support, lexer, yyVal, yyVals, yyTop); // line 452
break;
case 44: yyVal = case44_line459(support, lexer, yyVal, yyVals, yyTop); // line 459
break;
case 45: yyVal = case45_line462(support, lexer, yyVal, yyVals, yyTop); // line 462
break;
case 46: yyVal = case46_line465(support, lexer, yyVal, yyVals, yyTop); // line 465
break;
case 48: yyVal = case48_line471(support, lexer, yyVal, yyVals, yyTop); // line 471
break;
case 49: yyVal = case49_line474(support, lexer, yyVal, yyVals, yyTop); // line 474
break;
case 50: yyVal = case50_line479(support, lexer, yyVal, yyVals, yyTop); // line 479
break;
case 51: yyVal = case51_line481(support, lexer, yyVal, yyVals, yyTop); // line 481
break;
case 52: yyVal = case52_line487(support, lexer, yyVal, yyVals, yyTop); // line 487
break;
case 53: yyVal = case53_line490(support, lexer, yyVal, yyVals, yyTop); // line 490
break;
case 54: yyVal = case54_line493(support, lexer, yyVal, yyVals, yyTop); // line 493
break;
case 55: yyVal = case55_line496(support, lexer, yyVal, yyVals, yyTop); // line 496
break;
case 56: yyVal = case56_line499(support, lexer, yyVal, yyVals, yyTop); // line 499
break;
case 57: yyVal = case57_line502(support, lexer, yyVal, yyVals, yyTop); // line 502
break;
case 58: yyVal = case58_line505(support, lexer, yyVal, yyVals, yyTop); // line 505
break;
case 59: yyVal = case59_line508(support, lexer, yyVal, yyVals, yyTop); // line 508
break;
case 61: yyVal = case61_line514(support, lexer, yyVal, yyVals, yyTop); // line 514
break;
case 62: yyVal = case62_line519(support, lexer, yyVal, yyVals, yyTop); // line 519
break;
case 63: yyVal = case63_line522(support, lexer, yyVal, yyVals, yyTop); // line 522
break;
case 64: yyVal = case64_line527(support, lexer, yyVal, yyVals, yyTop); // line 527
break;
case 65: yyVal = case65_line530(support, lexer, yyVal, yyVals, yyTop); // line 530
break;
case 66: yyVal = case66_line533(support, lexer, yyVal, yyVals, yyTop); // line 533
break;
case 67: yyVal = case67_line536(support, lexer, yyVal, yyVals, yyTop); // line 536
break;
case 68: yyVal = case68_line539(support, lexer, yyVal, yyVals, yyTop); // line 539
break;
case 69: yyVal = case69_line542(support, lexer, yyVal, yyVals, yyTop); // line 542
break;
case 70: yyVal = case70_line545(support, lexer, yyVal, yyVals, yyTop); // line 545
break;
case 71: yyVal = case71_line548(support, lexer, yyVal, yyVals, yyTop); // line 548
break;
case 72: yyVal = case72_line551(support, lexer, yyVal, yyVals, yyTop); // line 551
break;
case 73: yyVal = case73_line554(support, lexer, yyVal, yyVals, yyTop); // line 554
break;
case 75: yyVal = case75_line559(support, lexer, yyVal, yyVals, yyTop); // line 559
break;
case 76: yyVal = case76_line564(support, lexer, yyVal, yyVals, yyTop); // line 564
break;
case 77: yyVal = case77_line567(support, lexer, yyVal, yyVals, yyTop); // line 567
break;
case 78: yyVal = case78_line572(support, lexer, yyVal, yyVals, yyTop); // line 572
break;
case 79: yyVal = case79_line575(support, lexer, yyVal, yyVals, yyTop); // line 575
break;
case 80: yyVal = case80_line579(support, lexer, yyVal, yyVals, yyTop); // line 579
break;
case 81: yyVal = case81_line582(support, lexer, yyVal, yyVals, yyTop); // line 582
break;
case 82: yyVal = case82_line585(support, lexer, yyVal, yyVals, yyTop); // line 585
break;
case 83: yyVal = case83_line588(support, lexer, yyVal, yyVals, yyTop); // line 588
break;
case 84: yyVal = case84_line591(support, lexer, yyVal, yyVals, yyTop); // line 591
break;
case 85: yyVal = case85_line594(support, lexer, yyVal, yyVals, yyTop); // line 594
break;
case 86: yyVal = case86_line603(support, lexer, yyVal, yyVals, yyTop); // line 603
break;
case 87: yyVal = case87_line612(support, lexer, yyVal, yyVals, yyTop); // line 612
break;
case 88: yyVal = case88_line616(support, lexer, yyVal, yyVals, yyTop); // line 616
break;
case 89: yyVal = case89_line620(support, lexer, yyVal, yyVals, yyTop); // line 620
break;
case 90: yyVal = case90_line623(support, lexer, yyVal, yyVals, yyTop); // line 623
break;
case 91: yyVal = case91_line626(support, lexer, yyVal, yyVals, yyTop); // line 626
break;
case 92: yyVal = case92_line629(support, lexer, yyVal, yyVals, yyTop); // line 629
break;
case 93: yyVal = case93_line632(support, lexer, yyVal, yyVals, yyTop); // line 632
break;
case 94: yyVal = case94_line641(support, lexer, yyVal, yyVals, yyTop); // line 641
break;
case 95: yyVal = case95_line650(support, lexer, yyVal, yyVals, yyTop); // line 650
break;
case 96: yyVal = case96_line654(support, lexer, yyVal, yyVals, yyTop); // line 654
break;
case 98: yyVal = case98_line659(support, lexer, yyVal, yyVals, yyTop); // line 659
break;
case 99: yyVal = case99_line662(support, lexer, yyVal, yyVals, yyTop); // line 662
break;
case 100: yyVal = case100_line665(support, lexer, yyVal, yyVals, yyTop); // line 665
break;
case 104: yyVal = case104_line671(support, lexer, yyVal, yyVals, yyTop); // line 671
break;
case 105: yyVal = case105_line675(support, lexer, yyVal, yyVals, yyTop); // line 675
break;
case 106: yyVal = case106_line681(support, lexer, yyVal, yyVals, yyTop); // line 681
break;
case 107: yyVal = case107_line684(support, lexer, yyVal, yyVals, yyTop); // line 684
break;
case 108: yyVal = case108_line689(support, lexer, yyVal, yyVals, yyTop); // line 689
break;
case 109: yyVal = case109_line692(support, lexer, yyVal, yyVals, yyTop); // line 692
break;
case 110: yyVal = case110_line696(support, lexer, yyVal, yyVals, yyTop); // line 696
break;
case 111: yyVal = case111_line699(support, lexer, yyVal, yyVals, yyTop); // line 699
break;
case 112: yyVal = case112_line701(support, lexer, yyVal, yyVals, yyTop); // line 701
break;
case 184: yyVal = case184_line720(support, lexer, yyVal, yyVals, yyTop); // line 720
break;
case 185: yyVal = case185_line725(support, lexer, yyVal, yyVals, yyTop); // line 725
break;
case 186: yyVal = case186_line730(support, lexer, yyVal, yyVals, yyTop); // line 730
break;
case 187: yyVal = case187_line747(support, lexer, yyVal, yyVals, yyTop); // line 747
break;
case 188: yyVal = case188_line767(support, lexer, yyVal, yyVals, yyTop); // line 767
break;
case 189: yyVal = case189_line771(support, lexer, yyVal, yyVals, yyTop); // line 771
break;
case 190: yyVal = case190_line774(support, lexer, yyVal, yyVals, yyTop); // line 774
break;
case 191: yyVal = case191_line777(support, lexer, yyVal, yyVals, yyTop); // line 777
break;
case 192: yyVal = case192_line780(support, lexer, yyVal, yyVals, yyTop); // line 780
break;
case 193: yyVal = case193_line783(support, lexer, yyVal, yyVals, yyTop); // line 783
break;
case 194: yyVal = case194_line786(support, lexer, yyVal, yyVals, yyTop); // line 786
break;
case 195: yyVal = case195_line789(support, lexer, yyVal, yyVals, yyTop); // line 789
break;
case 196: yyVal = case196_line796(support, lexer, yyVal, yyVals, yyTop); // line 796
break;
case 197: yyVal = case197_line803(support, lexer, yyVal, yyVals, yyTop); // line 803
break;
case 198: yyVal = case198_line806(support, lexer, yyVal, yyVals, yyTop); // line 806
break;
case 199: yyVal = case199_line809(support, lexer, yyVal, yyVals, yyTop); // line 809
break;
case 200: yyVal = case200_line812(support, lexer, yyVal, yyVals, yyTop); // line 812
break;
case 201: yyVal = case201_line815(support, lexer, yyVal, yyVals, yyTop); // line 815
break;
case 202: yyVal = case202_line818(support, lexer, yyVal, yyVals, yyTop); // line 818
break;
case 203: yyVal = case203_line821(support, lexer, yyVal, yyVals, yyTop); // line 821
break;
case 204: yyVal = case204_line824(support, lexer, yyVal, yyVals, yyTop); // line 824
break;
case 205: yyVal = case205_line827(support, lexer, yyVal, yyVals, yyTop); // line 827
break;
case 206: yyVal = case206_line830(support, lexer, yyVal, yyVals, yyTop); // line 830
break;
case 207: yyVal = case207_line833(support, lexer, yyVal, yyVals, yyTop); // line 833
break;
case 208: yyVal = case208_line836(support, lexer, yyVal, yyVals, yyTop); // line 836
break;
case 209: yyVal = case209_line839(support, lexer, yyVal, yyVals, yyTop); // line 839
break;
case 210: yyVal = case210_line842(support, lexer, yyVal, yyVals, yyTop); // line 842
break;
case 211: yyVal = case211_line845(support, lexer, yyVal, yyVals, yyTop); // line 845
break;
case 212: yyVal = case212_line848(support, lexer, yyVal, yyVals, yyTop); // line 848
break;
case 213: yyVal = case213_line851(support, lexer, yyVal, yyVals, yyTop); // line 851
break;
case 214: yyVal = case214_line854(support, lexer, yyVal, yyVals, yyTop); // line 854
break;
case 215: yyVal = case215_line857(support, lexer, yyVal, yyVals, yyTop); // line 857
break;
case 216: yyVal = case216_line860(support, lexer, yyVal, yyVals, yyTop); // line 860
break;
case 217: yyVal = case217_line863(support, lexer, yyVal, yyVals, yyTop); // line 863
break;
case 218: yyVal = case218_line866(support, lexer, yyVal, yyVals, yyTop); // line 866
break;
case 219: yyVal = case219_line875(support, lexer, yyVal, yyVals, yyTop); // line 875
break;
case 220: yyVal = case220_line878(support, lexer, yyVal, yyVals, yyTop); // line 878
break;
case 221: yyVal = case221_line881(support, lexer, yyVal, yyVals, yyTop); // line 881
break;
case 222: yyVal = case222_line884(support, lexer, yyVal, yyVals, yyTop); // line 884
break;
case 223: yyVal = case223_line887(support, lexer, yyVal, yyVals, yyTop); // line 887
break;
case 224: yyVal = case224_line890(support, lexer, yyVal, yyVals, yyTop); // line 890
break;
case 225: yyVal = case225_line893(support, lexer, yyVal, yyVals, yyTop); // line 893
break;
case 226: yyVal = case226_line896(support, lexer, yyVal, yyVals, yyTop); // line 896
break;
case 227: yyVal = case227_line900(support, lexer, yyVal, yyVals, yyTop); // line 900
break;
case 228: yyVal = case228_line903(support, lexer, yyVal, yyVals, yyTop); // line 903
break;
case 229: yyVal = case229_line907(support, lexer, yyVal, yyVals, yyTop); // line 907
break;
case 231: yyVal = case231_line913(support, lexer, yyVal, yyVals, yyTop); // line 913
break;
case 232: yyVal = case232_line916(support, lexer, yyVal, yyVals, yyTop); // line 916
break;
case 233: yyVal = case233_line919(support, lexer, yyVal, yyVals, yyTop); // line 919
break;
case 234: yyVal = case234_line923(support, lexer, yyVal, yyVals, yyTop); // line 923
break;
case 239: yyVal = case239_line933(support, lexer, yyVal, yyVals, yyTop); // line 933
break;
case 240: yyVal = case240_line936(support, lexer, yyVal, yyVals, yyTop); // line 936
break;
case 241: yyVal = case241_line939(support, lexer, yyVal, yyVals, yyTop); // line 939
break;
case 242: yyVal = case242_line943(support, lexer, yyVal, yyVals, yyTop); // line 943
break;
case 243: yyVal = case243_line947(support, lexer, yyVal, yyVals, yyTop); // line 947
break;
case 244: yyVal = case244_line950(support, lexer, yyVal, yyVals, yyTop); // line 950
break;
case 245: yyVal = case245_line952(support, lexer, yyVal, yyVals, yyTop); // line 952
break;
case 246: yyVal = case246_line957(support, lexer, yyVal, yyVals, yyTop); // line 957
break;
case 247: yyVal = case247_line961(support, lexer, yyVal, yyVals, yyTop); // line 961
break;
case 248: yyVal = case248_line964(support, lexer, yyVal, yyVals, yyTop); // line 964
break;
case 250: yyVal = case250_line970(support, lexer, yyVal, yyVals, yyTop); // line 970
break;
case 251: yyVal = case251_line974(support, lexer, yyVal, yyVals, yyTop); // line 974
break;
case 252: yyVal = case252_line977(support, lexer, yyVal, yyVals, yyTop); // line 977
break;
case 253: yyVal = case253_line986(support, lexer, yyVal, yyVals, yyTop); // line 986
break;
case 254: yyVal = case254_line998(support, lexer, yyVal, yyVals, yyTop); // line 998
break;
case 255: yyVal = case255_line1007(support, lexer, yyVal, yyVals, yyTop); // line 1007
break;
case 256: yyVal = case256_line1017(support, lexer, yyVal, yyVals, yyTop); // line 1017
break;
case 265: yyVal = case265_line1029(support, lexer, yyVal, yyVals, yyTop); // line 1029
break;
case 266: yyVal = case266_line1032(support, lexer, yyVal, yyVals, yyTop); // line 1032
break;
case 267: yyVal = case267_line1035(support, lexer, yyVal, yyVals, yyTop); // line 1035
break;
case 268: yyVal = case268_line1037(support, lexer, yyVal, yyVals, yyTop); // line 1037
break;
case 269: yyVal = case269_line1041(support, lexer, yyVal, yyVals, yyTop); // line 1041
break;
case 270: yyVal = case270_line1050(support, lexer, yyVal, yyVals, yyTop); // line 1050
break;
case 271: yyVal = case271_line1053(support, lexer, yyVal, yyVals, yyTop); // line 1053
break;
case 272: yyVal = case272_line1056(support, lexer, yyVal, yyVals, yyTop); // line 1056
break;
case 273: yyVal = case273_line1065(support, lexer, yyVal, yyVals, yyTop); // line 1065
break;
case 274: yyVal = case274_line1068(support, lexer, yyVal, yyVals, yyTop); // line 1068
break;
case 275: yyVal = case275_line1071(support, lexer, yyVal, yyVals, yyTop); // line 1071
break;
case 276: yyVal = case276_line1074(support, lexer, yyVal, yyVals, yyTop); // line 1074
break;
case 277: yyVal = case277_line1077(support, lexer, yyVal, yyVals, yyTop); // line 1077
break;
case 278: yyVal = case278_line1080(support, lexer, yyVal, yyVals, yyTop); // line 1080
break;
case 279: yyVal = case279_line1083(support, lexer, yyVal, yyVals, yyTop); // line 1083
break;
case 280: yyVal = case280_line1086(support, lexer, yyVal, yyVals, yyTop); // line 1086
break;
case 281: yyVal = case281_line1089(support, lexer, yyVal, yyVals, yyTop); // line 1089
break;
case 283: yyVal = case283_line1093(support, lexer, yyVal, yyVals, yyTop); // line 1093
break;
case 284: yyVal = case284_line1101(support, lexer, yyVal, yyVals, yyTop); // line 1101
break;
case 285: yyVal = case285_line1104(support, lexer, yyVal, yyVals, yyTop); // line 1104
break;
case 286: yyVal = case286_line1107(support, lexer, yyVal, yyVals, yyTop); // line 1107
break;
case 287: yyVal = case287_line1110(support, lexer, yyVal, yyVals, yyTop); // line 1110
break;
case 288: yyVal = case288_line1112(support, lexer, yyVal, yyVals, yyTop); // line 1112
break;
case 289: yyVal = case289_line1114(support, lexer, yyVal, yyVals, yyTop); // line 1114
break;
case 290: yyVal = case290_line1118(support, lexer, yyVal, yyVals, yyTop); // line 1118
break;
case 291: yyVal = case291_line1120(support, lexer, yyVal, yyVals, yyTop); // line 1120
break;
case 292: yyVal = case292_line1122(support, lexer, yyVal, yyVals, yyTop); // line 1122
break;
case 293: yyVal = case293_line1126(support, lexer, yyVal, yyVals, yyTop); // line 1126
break;
case 294: yyVal = case294_line1129(support, lexer, yyVal, yyVals, yyTop); // line 1129
break;
case 295: yyVal = case295_line1132(support, lexer, yyVal, yyVals, yyTop); // line 1132
break;
case 296: yyVal = case296_line1134(support, lexer, yyVal, yyVals, yyTop); // line 1134
break;
case 297: yyVal = case297_line1136(support, lexer, yyVal, yyVals, yyTop); // line 1136
break;
case 298: yyVal = case298_line1140(support, lexer, yyVal, yyVals, yyTop); // line 1140
break;
case 299: yyVal = case299_line1145(support, lexer, yyVal, yyVals, yyTop); // line 1145
break;
case 300: yyVal = case300_line1151(support, lexer, yyVal, yyVals, yyTop); // line 1151
break;
case 301: yyVal = case301_line1154(support, lexer, yyVal, yyVals, yyTop); // line 1154
break;
case 302: yyVal = case302_line1158(support, lexer, yyVal, yyVals, yyTop); // line 1158
break;
case 303: yyVal = case303_line1164(support, lexer, yyVal, yyVals, yyTop); // line 1164
break;
case 304: yyVal = case304_line1169(support, lexer, yyVal, yyVals, yyTop); // line 1169
break;
case 305: yyVal = case305_line1175(support, lexer, yyVal, yyVals, yyTop); // line 1175
break;
case 306: yyVal = case306_line1178(support, lexer, yyVal, yyVals, yyTop); // line 1178
break;
case 307: yyVal = case307_line1186(support, lexer, yyVal, yyVals, yyTop); // line 1186
break;
case 308: yyVal = case308_line1188(support, lexer, yyVal, yyVals, yyTop); // line 1188
break;
case 309: yyVal = case309_line1192(support, lexer, yyVal, yyVals, yyTop); // line 1192
break;
case 310: yyVal = case310_line1200(support, lexer, yyVal, yyVals, yyTop); // line 1200
break;
case 311: yyVal = case311_line1203(support, lexer, yyVal, yyVals, yyTop); // line 1203
break;
case 312: yyVal = case312_line1206(support, lexer, yyVal, yyVals, yyTop); // line 1206
break;
case 313: yyVal = case313_line1209(support, lexer, yyVal, yyVals, yyTop); // line 1209
break;
case 314: yyVal = case314_line1213(support, lexer, yyVal, yyVals, yyTop); // line 1213
break;
case 321: yyVal = case321_line1227(support, lexer, yyVal, yyVals, yyTop); // line 1227
break;
case 323: yyVal = case323_line1232(support, lexer, yyVal, yyVals, yyTop); // line 1232
break;
case 325: yyVal = case325_line1237(support, lexer, yyVal, yyVals, yyTop); // line 1237
break;
case 326: yyVal = case326_line1240(support, lexer, yyVal, yyVals, yyTop); // line 1240
break;
case 327: yyVal = case327_line1243(support, lexer, yyVal, yyVals, yyTop); // line 1243
break;
case 328: yyVal = case328_line1248(support, lexer, yyVal, yyVals, yyTop); // line 1248
break;
case 329: yyVal = case329_line1251(support, lexer, yyVal, yyVals, yyTop); // line 1251
break;
case 330: yyVal = case330_line1255(support, lexer, yyVal, yyVals, yyTop); // line 1255
break;
case 331: yyVal = case331_line1258(support, lexer, yyVal, yyVals, yyTop); // line 1258
break;
case 332: yyVal = case332_line1261(support, lexer, yyVal, yyVals, yyTop); // line 1261
break;
case 333: yyVal = case333_line1264(support, lexer, yyVal, yyVals, yyTop); // line 1264
break;
case 334: yyVal = case334_line1267(support, lexer, yyVal, yyVals, yyTop); // line 1267
break;
case 335: yyVal = case335_line1270(support, lexer, yyVal, yyVals, yyTop); // line 1270
break;
case 336: yyVal = case336_line1273(support, lexer, yyVal, yyVals, yyTop); // line 1273
break;
case 337: yyVal = case337_line1276(support, lexer, yyVal, yyVals, yyTop); // line 1276
break;
case 338: yyVal = case338_line1279(support, lexer, yyVal, yyVals, yyTop); // line 1279
break;
case 339: yyVal = case339_line1284(support, lexer, yyVal, yyVals, yyTop); // line 1284
break;
case 340: yyVal = case340_line1287(support, lexer, yyVal, yyVals, yyTop); // line 1287
break;
case 341: yyVal = case341_line1290(support, lexer, yyVal, yyVals, yyTop); // line 1290
break;
case 342: yyVal = case342_line1293(support, lexer, yyVal, yyVals, yyTop); // line 1293
break;
case 343: yyVal = case343_line1296(support, lexer, yyVal, yyVals, yyTop); // line 1296
break;
case 344: yyVal = case344_line1299(support, lexer, yyVal, yyVals, yyTop); // line 1299
break;
case 345: yyVal = case345_line1303(support, lexer, yyVal, yyVals, yyTop); // line 1303
break;
case 346: yyVal = case346_line1306(support, lexer, yyVal, yyVals, yyTop); // line 1306
break;
case 347: yyVal = case347_line1309(support, lexer, yyVal, yyVals, yyTop); // line 1309
break;
case 348: yyVal = case348_line1312(support, lexer, yyVal, yyVals, yyTop); // line 1312
break;
case 349: yyVal = case349_line1315(support, lexer, yyVal, yyVals, yyTop); // line 1315
break;
case 350: yyVal = case350_line1318(support, lexer, yyVal, yyVals, yyTop); // line 1318
break;
case 351: yyVal = case351_line1321(support, lexer, yyVal, yyVals, yyTop); // line 1321
break;
case 352: yyVal = case352_line1324(support, lexer, yyVal, yyVals, yyTop); // line 1324
break;
case 353: yyVal = case353_line1327(support, lexer, yyVal, yyVals, yyTop); // line 1327
break;
case 354: yyVal = case354_line1331(support, lexer, yyVal, yyVals, yyTop); // line 1331
break;
case 355: yyVal = case355_line1335(support, lexer, yyVal, yyVals, yyTop); // line 1335
break;
case 356: yyVal = case356_line1340(support, lexer, yyVal, yyVals, yyTop); // line 1340
break;
case 357: yyVal = case357_line1343(support, lexer, yyVal, yyVals, yyTop); // line 1343
break;
case 358: yyVal = case358_line1346(support, lexer, yyVal, yyVals, yyTop); // line 1346
break;
case 360: yyVal = case360_line1352(support, lexer, yyVal, yyVals, yyTop); // line 1352
break;
case 361: yyVal = case361_line1357(support, lexer, yyVal, yyVals, yyTop); // line 1357
break;
case 362: yyVal = case362_line1360(support, lexer, yyVal, yyVals, yyTop); // line 1360
break;
case 363: yyVal = case363_line1364(support, lexer, yyVal, yyVals, yyTop); // line 1364
break;
case 364: yyVal = case364_line1367(support, lexer, yyVal, yyVals, yyTop); // line 1367
break;
case 365: yyVal = case365_line1371(support, lexer, yyVal, yyVals, yyTop); // line 1371
break;
case 366: yyVal = case366_line1375(support, lexer, yyVal, yyVals, yyTop); // line 1375
break;
case 367: yyVal = case367_line1381(support, lexer, yyVal, yyVals, yyTop); // line 1381
break;
case 368: yyVal = case368_line1385(support, lexer, yyVal, yyVals, yyTop); // line 1385
break;
case 369: yyVal = case369_line1389(support, lexer, yyVal, yyVals, yyTop); // line 1389
break;
case 370: yyVal = case370_line1392(support, lexer, yyVal, yyVals, yyTop); // line 1392
break;
case 371: yyVal = case371_line1396(support, lexer, yyVal, yyVals, yyTop); // line 1396
break;
case 372: yyVal = case372_line1398(support, lexer, yyVal, yyVals, yyTop); // line 1398
break;
case 373: yyVal = case373_line1403(support, lexer, yyVal, yyVals, yyTop); // line 1403
break;
case 374: yyVal = case374_line1414(support, lexer, yyVal, yyVals, yyTop); // line 1414
break;
case 375: yyVal = case375_line1417(support, lexer, yyVal, yyVals, yyTop); // line 1417
break;
case 376: yyVal = case376_line1422(support, lexer, yyVal, yyVals, yyTop); // line 1422
break;
case 377: yyVal = case377_line1425(support, lexer, yyVal, yyVals, yyTop); // line 1425
break;
case 378: yyVal = case378_line1428(support, lexer, yyVal, yyVals, yyTop); // line 1428
break;
case 379: yyVal = case379_line1431(support, lexer, yyVal, yyVals, yyTop); // line 1431
break;
case 380: yyVal = case380_line1434(support, lexer, yyVal, yyVals, yyTop); // line 1434
break;
case 381: yyVal = case381_line1437(support, lexer, yyVal, yyVals, yyTop); // line 1437
break;
case 382: yyVal = case382_line1440(support, lexer, yyVal, yyVals, yyTop); // line 1440
break;
case 383: yyVal = case383_line1443(support, lexer, yyVal, yyVals, yyTop); // line 1443
break;
case 384: yyVal = case384_line1446(support, lexer, yyVal, yyVals, yyTop); // line 1446
break;
case 385: yyVal = case385_line1454(support, lexer, yyVal, yyVals, yyTop); // line 1454
break;
case 386: yyVal = case386_line1456(support, lexer, yyVal, yyVals, yyTop); // line 1456
break;
case 387: yyVal = case387_line1460(support, lexer, yyVal, yyVals, yyTop); // line 1460
break;
case 388: yyVal = case388_line1462(support, lexer, yyVal, yyVals, yyTop); // line 1462
break;
case 389: yyVal = case389_line1469(support, lexer, yyVal, yyVals, yyTop); // line 1469
break;
case 392: yyVal = case392_line1475(support, lexer, yyVal, yyVals, yyTop); // line 1475
break;
case 393: yyVal = case393_line1488(support, lexer, yyVal, yyVals, yyTop); // line 1488
break;
case 394: yyVal = case394_line1492(support, lexer, yyVal, yyVals, yyTop); // line 1492
break;
case 395: yyVal = case395_line1495(support, lexer, yyVal, yyVals, yyTop); // line 1495
break;
case 397: yyVal = case397_line1501(support, lexer, yyVal, yyVals, yyTop); // line 1501
break;
case 399: yyVal = case399_line1506(support, lexer, yyVal, yyVals, yyTop); // line 1506
break;
case 402: yyVal = case402_line1512(support, lexer, yyVal, yyVals, yyTop); // line 1512
break;
case 404: yyVal = case404_line1518(support, lexer, yyVal, yyVals, yyTop); // line 1518
break;
case 405: yyVal = case405_line1532(support, lexer, yyVal, yyVals, yyTop); // line 1532
break;
case 406: yyVal = case406_line1535(support, lexer, yyVal, yyVals, yyTop); // line 1535
break;
case 407: yyVal = case407_line1538(support, lexer, yyVal, yyVals, yyTop); // line 1538
break;
case 408: yyVal = case408_line1542(support, lexer, yyVal, yyVals, yyTop); // line 1542
break;
case 409: yyVal = case409_line1557(support, lexer, yyVal, yyVals, yyTop); // line 1557
break;
case 410: yyVal = case410_line1573(support, lexer, yyVal, yyVals, yyTop); // line 1573
break;
case 411: yyVal = case411_line1588(support, lexer, yyVal, yyVals, yyTop); // line 1588
break;
case 412: yyVal = case412_line1591(support, lexer, yyVal, yyVals, yyTop); // line 1591
break;
case 413: yyVal = case413_line1595(support, lexer, yyVal, yyVals, yyTop); // line 1595
break;
case 414: yyVal = case414_line1598(support, lexer, yyVal, yyVals, yyTop); // line 1598
break;
case 416: yyVal = case416_line1603(support, lexer, yyVal, yyVals, yyTop); // line 1603
break;
case 417: yyVal = case417_line1607(support, lexer, yyVal, yyVals, yyTop); // line 1607
break;
case 418: yyVal = case418_line1610(support, lexer, yyVal, yyVals, yyTop); // line 1610
break;
case 419: yyVal = case419_line1615(support, lexer, yyVal, yyVals, yyTop); // line 1615
break;
case 420: yyVal = case420_line1618(support, lexer, yyVal, yyVals, yyTop); // line 1618
break;
case 421: yyVal = case421_line1622(support, lexer, yyVal, yyVals, yyTop); // line 1622
break;
case 422: yyVal = case422_line1625(support, lexer, yyVal, yyVals, yyTop); // line 1625
break;
case 423: yyVal = case423_line1629(support, lexer, yyVal, yyVals, yyTop); // line 1629
break;
case 424: yyVal = case424_line1632(support, lexer, yyVal, yyVals, yyTop); // line 1632
break;
case 425: yyVal = case425_line1636(support, lexer, yyVal, yyVals, yyTop); // line 1636
break;
case 426: yyVal = case426_line1639(support, lexer, yyVal, yyVals, yyTop); // line 1639
break;
case 427: yyVal = case427_line1643(support, lexer, yyVal, yyVals, yyTop); // line 1643
break;
case 428: yyVal = case428_line1647(support, lexer, yyVal, yyVals, yyTop); // line 1647
break;
case 429: yyVal = case429_line1651(support, lexer, yyVal, yyVals, yyTop); // line 1651
break;
case 430: yyVal = case430_line1657(support, lexer, yyVal, yyVals, yyTop); // line 1657
break;
case 431: yyVal = case431_line1660(support, lexer, yyVal, yyVals, yyTop); // line 1660
break;
case 432: yyVal = case432_line1663(support, lexer, yyVal, yyVals, yyTop); // line 1663
break;
case 434: yyVal = case434_line1669(support, lexer, yyVal, yyVals, yyTop); // line 1669
break;
case 439: yyVal = case439_line1678(support, lexer, yyVal, yyVals, yyTop); // line 1678
break;
case 440: yyVal = case440_line1702(support, lexer, yyVal, yyVals, yyTop); // line 1702
break;
case 441: yyVal = case441_line1705(support, lexer, yyVal, yyVals, yyTop); // line 1705
break;
case 442: yyVal = case442_line1708(support, lexer, yyVal, yyVals, yyTop); // line 1708
break;
case 443: yyVal = case443_line1711(support, lexer, yyVal, yyVals, yyTop); // line 1711
break;
case 449: yyVal = case449_line1717(support, lexer, yyVal, yyVals, yyTop); // line 1717
break;
case 450: yyVal = case450_line1720(support, lexer, yyVal, yyVals, yyTop); // line 1720
break;
case 451: yyVal = case451_line1723(support, lexer, yyVal, yyVals, yyTop); // line 1723
break;
case 452: yyVal = case452_line1726(support, lexer, yyVal, yyVals, yyTop); // line 1726
break;
case 453: yyVal = case453_line1729(support, lexer, yyVal, yyVals, yyTop); // line 1729
break;
case 454: yyVal = case454_line1732(support, lexer, yyVal, yyVals, yyTop); // line 1732
break;
case 455: yyVal = case455_line1735(support, lexer, yyVal, yyVals, yyTop); // line 1735
break;
case 456: yyVal = case456_line1740(support, lexer, yyVal, yyVals, yyTop); // line 1740
break;
case 457: yyVal = case457_line1745(support, lexer, yyVal, yyVals, yyTop); // line 1745
break;
case 458: yyVal = case458_line1750(support, lexer, yyVal, yyVals, yyTop); // line 1750
break;
case 459: yyVal = case459_line1753(support, lexer, yyVal, yyVals, yyTop); // line 1753
break;
case 460: yyVal = case460_line1757(support, lexer, yyVal, yyVals, yyTop); // line 1757
break;
case 461: yyVal = case461_line1760(support, lexer, yyVal, yyVals, yyTop); // line 1760
break;
case 462: yyVal = case462_line1762(support, lexer, yyVal, yyVals, yyTop); // line 1762
break;
case 463: yyVal = case463_line1765(support, lexer, yyVal, yyVals, yyTop); // line 1765
break;
case 464: yyVal = case464_line1771(support, lexer, yyVal, yyVals, yyTop); // line 1771
break;
case 465: yyVal = case465_line1776(support, lexer, yyVal, yyVals, yyTop); // line 1776
break;
case 466: yyVal = case466_line1781(support, lexer, yyVal, yyVals, yyTop); // line 1781
break;
case 467: yyVal = case467_line1784(support, lexer, yyVal, yyVals, yyTop); // line 1784
break;
case 468: yyVal = case468_line1787(support, lexer, yyVal, yyVals, yyTop); // line 1787
break;
case 469: yyVal = case469_line1790(support, lexer, yyVal, yyVals, yyTop); // line 1790
break;
case 470: yyVal = case470_line1793(support, lexer, yyVal, yyVals, yyTop); // line 1793
break;
case 471: yyVal = case471_line1796(support, lexer, yyVal, yyVals, yyTop); // line 1796
break;
case 472: yyVal = case472_line1799(support, lexer, yyVal, yyVals, yyTop); // line 1799
break;
case 473: yyVal = case473_line1802(support, lexer, yyVal, yyVals, yyTop); // line 1802
break;
case 474: yyVal = case474_line1805(support, lexer, yyVal, yyVals, yyTop); // line 1805
break;
case 475: yyVal = case475_line1808(support, lexer, yyVal, yyVals, yyTop); // line 1808
break;
case 476: yyVal = case476_line1811(support, lexer, yyVal, yyVals, yyTop); // line 1811
break;
case 477: yyVal = case477_line1814(support, lexer, yyVal, yyVals, yyTop); // line 1814
break;
case 478: yyVal = case478_line1817(support, lexer, yyVal, yyVals, yyTop); // line 1817
break;
case 479: yyVal = case479_line1820(support, lexer, yyVal, yyVals, yyTop); // line 1820
break;
case 480: yyVal = case480_line1823(support, lexer, yyVal, yyVals, yyTop); // line 1823
break;
case 481: yyVal = case481_line1827(support, lexer, yyVal, yyVals, yyTop); // line 1827
break;
case 482: yyVal = case482_line1830(support, lexer, yyVal, yyVals, yyTop); // line 1830
break;
case 483: yyVal = case483_line1833(support, lexer, yyVal, yyVals, yyTop); // line 1833
break;
case 484: yyVal = case484_line1836(support, lexer, yyVal, yyVals, yyTop); // line 1836
break;
case 486: yyVal = case486_line1842(support, lexer, yyVal, yyVals, yyTop); // line 1842
break;
case 487: yyVal = case487_line1852(support, lexer, yyVal, yyVals, yyTop); // line 1852
break;
case 488: yyVal = case488_line1859(support, lexer, yyVal, yyVals, yyTop); // line 1859
break;
case 489: yyVal = case489_line1875(support, lexer, yyVal, yyVals, yyTop); // line 1875
break;
case 490: yyVal = case490_line1878(support, lexer, yyVal, yyVals, yyTop); // line 1878
break;
case 491: yyVal = case491_line1883(support, lexer, yyVal, yyVals, yyTop); // line 1883
break;
case 492: yyVal = case492_line1892(support, lexer, yyVal, yyVals, yyTop); // line 1892
break;
case 493: yyVal = case493_line1901(support, lexer, yyVal, yyVals, yyTop); // line 1901
break;
case 494: yyVal = case494_line1904(support, lexer, yyVal, yyVals, yyTop); // line 1904
break;
case 495: yyVal = case495_line1908(support, lexer, yyVal, yyVals, yyTop); // line 1908
break;
case 496: yyVal = case496_line1911(support, lexer, yyVal, yyVals, yyTop); // line 1911
break;
case 499: yyVal = case499_line1918(support, lexer, yyVal, yyVals, yyTop); // line 1918
break;
case 500: yyVal = case500_line1925(support, lexer, yyVal, yyVals, yyTop); // line 1925
break;
case 503: yyVal = case503_line1933(support, lexer, yyVal, yyVals, yyTop); // line 1933
break;
case 504: yyVal = case504_line1943(support, lexer, yyVal, yyVals, yyTop); // line 1943
break;
case 505: yyVal = case505_line1946(support, lexer, yyVal, yyVals, yyTop); // line 1946
break;
case 506: yyVal = case506_line1950(support, lexer, yyVal, yyVals, yyTop); // line 1950
break;
case 507: yyVal = case507_line1956(support, lexer, yyVal, yyVals, yyTop); // line 1956
break;
case 508: yyVal = case508_line1958(support, lexer, yyVal, yyVals, yyTop); // line 1958
break;
case 509: yyVal = case509_line1969(support, lexer, yyVal, yyVals, yyTop); // line 1969
break;
case 510: yyVal = case510_line1972(support, lexer, yyVal, yyVals, yyTop); // line 1972
break;
case 512: yyVal = case512_line1978(support, lexer, yyVal, yyVals, yyTop); // line 1978
break;
case 513: yyVal = case513_line1983(support, lexer, yyVal, yyVals, yyTop); // line 1983
break;
case 514: yyVal = case514_line1993(support, lexer, yyVal, yyVals, yyTop); // line 1993
break;
case 531: yyVal = case531_line2004(support, lexer, yyVal, yyVals, yyTop); // line 2004
break;
case 532: yyVal = case532_line2007(support, lexer, yyVal, yyVals, yyTop); // line 2007
break;
case 540: yyVal = case540_line2018(support, lexer, yyVal, yyVals, yyTop); // line 2018
break;
case 541: yyVal = case541_line2022(support, lexer, yyVal, yyVals, yyTop); // line 2022
break;
// ACTIONS_END
        }
        yyTop -= yyLen[yyN];
        yyState = yyStates[yyTop];
        int yyM = yyLhs[yyN];
        if (yyState == 0 && yyM == 0) {
          if (yydebug != null) yydebug.shift(0, yyFinal);
          yyState = yyFinal;
          if (yyToken < 0) {
            yyToken = yyLex.advance() ? yyLex.token() : 0;
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

public static Object case469_line1790(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop]).getPosition(), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), null, ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case468_line1787(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop]).getPosition(), ((ListNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case244_line950(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = Long.valueOf(lexer.getCmdArgumentState().begin());
    return yyVal;
}
public static Object case251_line974(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newSplatNode(((Token)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case241_line939(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(((ListNode)yyVals[-1+yyTop]).getPosition(), new Hash19Node(lexer.getPosition(), ((ListNode)yyVals[-1+yyTop])));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case378_line1428(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public static Object case367_line1381(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ArgsNode)yyVals[-2+yyTop]);
                    ((ISourcePositionHolder)yyVal).setPosition(((Token)yyVals[-3+yyTop]).getPosition());
    return yyVal;
}
public static Object case349_line1315(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(support.getPosition(((ListNode)yyVals[-1+yyTop])), null, ((ListNode)yyVals[-1+yyTop]), null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case310_line1200(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BreakNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
    return yyVal;
}
public static Object case449_line1717(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("nil", Tokens.kNIL, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public static Object case422_line1625(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.literal_concat(((Node)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case382_line1440(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop]));
    return yyVal;
}
public static Object case45_line462(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BreakNode(((Token)yyVals[-1+yyTop]).getPosition(), support.ret_args(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop]).getPosition()));
    return yyVal;
}
public static Object case278_line1080(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new DefinedNode(((Token)yyVals[-4+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public static Object case215_line857(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
}
public static Object case184_line720(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    /* FIXME: Consider fixing node_assign itself rather than single case*/
                    ((Node)yyVal).setPosition(support.getPosition(((Node)yyVals[-2+yyTop])));
    return yyVal;
}
public static Object case357_line1343(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((Token)yyVals[0+yyTop]).getPosition(), null, null, null, null, null);
    return yyVal;
}
public static Object case62_line519(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((MultipleAsgn19Node)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case242_line943(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.arg_append(((Node)yyVals[-3+yyTop]), new Hash19Node(lexer.getPosition(), ((ListNode)yyVals[-1+yyTop])));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case214_line854(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
}
public static Object case408_line1542(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);

                    ((ISourcePositionHolder)yyVal).setPosition(((Token)yyVals[-2+yyTop]).getPosition());
                    int extraLength = ((String) ((Token)yyVals[-2+yyTop]).getValue()).length() - 1;

                    /* We may need to subtract addition offset off of first */
                    /* string fragment (we optimistically take one off in*/
                    /* ParserSupport.literal_concat).  Check token length*/
                    /* and subtract as neeeded.*/
                    if ((((Node)yyVals[-1+yyTop]) instanceof DStrNode) && extraLength > 0) {
                      Node strNode = ((DStrNode)((Node)yyVals[-1+yyTop])).get(0);
                    }
    return yyVal;
}
public static Object case397_line1501(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case348_line1312(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(support.getPosition(((ListNode)yyVals[-5+yyTop])), null, ((ListNode)yyVals[-5+yyTop]), ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case347_line1309(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(support.getPosition(((ListNode)yyVals[-3+yyTop])), null, ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case280_line1086(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(NilImplicitNode.NIL, "!");
    return yyVal;
}
public static Object case205_line827(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "+@");
    return yyVal;
}
public static Object case216_line860(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "===", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
}
public static Object case432_line1663(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = new ClassVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public static Object case364_line1367(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
}
public static Object case193_line783(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("constant re-assignment");
    return yyVal;
}
public static Object case57_line502(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case229_line907(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = ((Node)yyVals[0+yyTop]) != null ? ((Node)yyVals[0+yyTop]) : NilImplicitNode.NIL;
    return yyVal;
}
public static Object case228_line903(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case106_line681(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new LiteralNode(((Token)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case208_line836(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "^", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
}
public static Object case73_line554(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                      yyVal = new MultipleAsgn19Node(((Token)yyVals[-2+yyTop]).getPosition(), null, new StarNode(lexer.getPosition()), ((ListNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case21_line369(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.getResult().addBeginNode(new PreExeNode(((Token)yyVals[-4+yyTop]).getPosition(), support.getCurrentScope(), ((Node)yyVals[-1+yyTop])));
                    support.popCurrentScope();
                    yyVal = null;
    return yyVal;
}
public static Object case500_line1925(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new UnnamedRestArgNode(((Token)yyVals[0+yyTop]).getPosition(), support.getCurrentScope().addVariable("*"));
    return yyVal;
}
public static Object case482_line1830(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("formal argument cannot be an instance variable");
    return yyVal;
}
public static Object case426_line1639(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = lexer.getStrTerm();
                    lexer.setStrTerm(null);
                    lexer.setState(LexState.EXPR_BEG);
    return yyVal;
}
public static Object case417_line1607(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = new ZArrayNode(((Token)yyVals[-2+yyTop]).getPosition());
    return yyVal;
}
public static Object case91_line626(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public static Object case72_line551(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                      yyVal = new MultipleAsgn19Node(((Token)yyVals[0+yyTop]).getPosition(), null, new StarNode(lexer.getPosition()), null);
    return yyVal;
}
public static Object case323_line1232(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case481_line1827(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("formal argument cannot be a constant");
    return yyVal;
}
public static Object case427_line1643(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setStrTerm(((StrTerm)yyVals[-1+yyTop]));
                    yyVal = new EvStrNode(((Token)yyVals[-2+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case330_line1255(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((ListNode)yyVals[0+yyTop]).getPosition(), ((ListNode)yyVals[0+yyTop]), null, null);
    return yyVal;
}
public static Object case300_line1151(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = Boolean.valueOf(support.isInDef());
                    support.setInDef(false);
    return yyVal;
}
public static Object case67_line536(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((ListNode)yyVals[-4+yyTop]).getPosition(), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-2+yyTop]), ((ListNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case541_line2022(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = null;
    return yyVal;
}
public static Object case29_line416(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new OpAsgnNode(support.getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public static Object case66_line533(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((ListNode)yyVals[-2+yyTop]).getPosition(), ((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), (ListNode) null);
    return yyVal;
}
public static Object case291_line1120(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.getConditionState().end();
    return yyVal;
}
public static Object case512_line1978(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).addAll(((ListNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case507_line1956(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setState(LexState.EXPR_BEG);
    return yyVal;
}
public static Object case335_line1270(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((Token)yyVals[-1+yyTop]).getPosition(), null, support.assignable(((Token)yyVals[0+yyTop]), null), null);
    return yyVal;
}
public static Object case334_line1267(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((ListNode)yyVals[-4+yyTop]).getPosition(), ((ListNode)yyVals[-4+yyTop]), new StarNode(lexer.getPosition()), ((ListNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case52_line487(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_fcall(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public static Object case24_line384(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    ((MultipleAsgn19Node)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                    yyVal = ((MultipleAsgn19Node)yyVals[-2+yyTop]);
    return yyVal;
}
public static Object case225_line893(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newOrNode(((Token)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case2_line275(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
  /* ENEBO: Removed !compile_for_eval which probably is to reduce warnings*/
                  if (((Node)yyVals[0+yyTop]) != null) {
                      /* last expression should not be void */
                      if (((Node)yyVals[0+yyTop]) instanceof BlockNode) {
                          support.checkUselessStatement(((BlockNode)yyVals[0+yyTop]).getLast());
                      } else {
                          support.checkUselessStatement(((Node)yyVals[0+yyTop]));
                      }
                  }
                  support.getResult().setAST(support.addRootNode(((Node)yyVals[0+yyTop]), support.getPosition(((Node)yyVals[0+yyTop]))));
    return yyVal;
}
public static Object case191_line777(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new OpAsgnNode(support.getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public static Object case201_line815(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "%", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
}
public static Object case491_line1883(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (!support.is_local_id(((Token)yyVals[-2+yyTop]))) {
                        support.yyerror("formal argument must be local variable");
                    }
                    support.shadowing_lvar(((Token)yyVals[-2+yyTop]));
                    support.arg_var(((Token)yyVals[-2+yyTop]));
                    yyVal = new OptArgNode(((Token)yyVals[-2+yyTop]).getPosition(), support.assignable(((Token)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
    return yyVal;
}
public static Object case200_line812(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "/", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
}
public static Object case290_line1118(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.getConditionState().begin();
    return yyVal;
}
public static Object case1_line272(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.setState(LexState.EXPR_BEG);
                  support.initTopLocalVariables();
    return yyVal;
}
public static Object case94_line641(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = ((Token)yyVals[-1+yyTop]).getPosition();

                    yyVal = new ConstDeclNode(position, null, support.new_colon3(position, (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
    return yyVal;
}
public static Object case295_line1132(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.getConditionState().begin();
    return yyVal;
}
public static Object case499_line1918(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (!support.is_local_id(((Token)yyVals[0+yyTop]))) {
                        support.yyerror("duplicate rest argument name");
                    }
                    support.shadowing_lvar(((Token)yyVals[0+yyTop]));
                    yyVal = new RestArgNode(((Token)yyVals[-1+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue(), support.arg_var(((Token)yyVals[0+yyTop])));
    return yyVal;
}
public static Object case412_line1591(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
    return yyVal;
}
public static Object case392_line1475(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node node;
                    if (((Node)yyVals[-3+yyTop]) != null) {
                        node = support.appendToBlock(support.node_assign(((Node)yyVals[-3+yyTop]), new GlobalVarNode(((Token)yyVals[-5+yyTop]).getPosition(), "$!")), ((Node)yyVals[-1+yyTop]));
                        if (((Node)yyVals[-1+yyTop]) != null) {
                            node.setPosition(support.unwrapNewlineNode(((Node)yyVals[-1+yyTop])).getPosition());
                        }
                    } else {
                        node = ((Node)yyVals[-1+yyTop]);
                    }
                    Node body = node == null ? NilImplicitNode.NIL : node;
                    yyVal = new RescueBodyNode(((Token)yyVals[-5+yyTop]).getPosition(), ((Node)yyVals[-4+yyTop]), body, ((RescueBodyNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case383_line1443(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ZSuperNode(((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public static Object case373_line1403(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    /* Workaround for JRUBY-2326 (MRI does not enter this production for some reason)*/
                    if (((Node)yyVals[-1+yyTop]) instanceof YieldNode) {
                        throw new SyntaxException(PID.BLOCK_GIVEN_TO_YIELD, ((Node)yyVals[-1+yyTop]).getPosition(), lexer.getCurrentLine(), "block given to yield");
                    }
                    if (((BlockAcceptingNode)yyVals[-1+yyTop]).getIterNode() instanceof BlockPassNode) {
                        throw new SyntaxException(PID.BLOCK_ARG_AND_BLOCK_GIVEN, ((Node)yyVals[-1+yyTop]).getPosition(), lexer.getCurrentLine(), "Both block arg and actual block given.");
                    }
                    yyVal = ((BlockAcceptingNode)yyVals[-1+yyTop]).setIterNode(((IterNode)yyVals[0+yyTop]));
                    ((Node)yyVal).setPosition(((Node)yyVals[-1+yyTop]).getPosition());
    return yyVal;
}
public static Object case53_line490(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_fcall(((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case31_line422(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case86_line603(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = ((Token)yyVals[-1+yyTop]).getPosition();

                    yyVal = new ConstDeclNode(position, null, support.new_colon3(position, (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
    return yyVal;
}
public static Object case325_line1237(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
    return yyVal;
}
public static Object case284_line1101(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((LambdaNode)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case495_line1908(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BlockNode(((Node)yyVals[0+yyTop]).getPosition()).add(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case477_line1814(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((RestArgNode)yyVals[-1+yyTop]).getPosition(), null, null, ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case454_line1732(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("__LINE__", Tokens.k__LINE__, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public static Object case376_line1422(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_fcall(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public static Object case12_line331(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new VAliasNode(((Token)yyVals[-2+yyTop]).getPosition(), (String) ((Token)yyVals[-1+yyTop]).getValue(), "$" + ((BackRefNode)yyVals[0+yyTop]).getType());
    return yyVal;
}
public static Object case36_line438(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newAndNode(((Token)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case471_line1796(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop]).getPosition(), ((ListNode)yyVals[-5+yyTop]), null, ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case395_line1495(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.splat_array(((Node)yyVals[0+yyTop]));
                    if (yyVal == null) yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case399_line1506(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case369_line1389(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public static Object case490_line1878(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                    yyVal = ((ListNode)yyVals[-2+yyTop]);
    return yyVal;
}
public static Object case452_line1726(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("false", Tokens.kFALSE, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public static Object case362_line1360(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
}
public static Object case345_line1303(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop]).getPosition(), ((ListNode)yyVals[-5+yyTop]), null, ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case344_line1299(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    RestArgNode rest = new UnnamedRestArgNode(((ListNode)yyVals[-1+yyTop]).getPosition(), support.getCurrentScope().addVariable("*"));
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, rest, null, null);
    return yyVal;
}
public static Object case475_line1808(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop]).getPosition(), null, ((ListNode)yyVals[-1+yyTop]), null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case462_line1762(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public static Object case394_line1492(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case360_line1352(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
}
public static Object case198_line806(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "-", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
}
public static Object case388_line1462(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IterNode(((Token)yyVals[-4+yyTop]).getPosition(), ((ArgsNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), support.getCurrentScope());
                    /* FIXME: What the hell is this?*/
                    ((ISourcePositionHolder)yyVals[-5+yyTop]).setPosition(support.getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])));
                    support.popCurrentScope();
    return yyVal;
}
public static Object case240_line936(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.arg_blk_pass(((Node)yyVals[-1+yyTop]), ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case108_line689(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((LiteralNode)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case286_line1107(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IfNode(((Token)yyVals[-5+yyTop]).getPosition(), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-2+yyTop]));
    return yyVal;
}
public static Object case414_line1598(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]) instanceof EvStrNode ? new DStrNode(((ListNode)yyVals[-2+yyTop]).getPosition()).add(((Node)yyVals[-1+yyTop])) : ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public static Object case328_line1248(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case96_line654(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("class/module name must be CONSTANT");
    return yyVal;
}
public static Object case68_line539(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((ListNode)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), new StarNode(lexer.getPosition()), null);
    return yyVal;
}
public static Object case77_line567(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public static Object case293_line1126(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newCaseNode(((Token)yyVals[-4+yyTop]).getPosition(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public static Object case248_line964(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
}
public static Object case49_line474(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public static Object case187_line747(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[-2+yyTop]));
                    ISourcePosition pos = ((Token)yyVals[-1+yyTop]).getPosition();
                    Node body = ((Node)yyVals[0+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop]);
                    Node rescueNode = new RescueNode(pos, ((Node)yyVals[-2+yyTop]), new RescueBodyNode(pos, null, body, null), null);

                    pos = ((AssignableNode)yyVals[-4+yyTop]).getPosition();
                    String asgnOp = (String) ((Token)yyVals[-3+yyTop]).getValue();
                    if (asgnOp.equals("||")) {
                        ((AssignableNode)yyVals[-4+yyTop]).setValueNode(((Node)yyVals[-2+yyTop]));
                        yyVal = new OpAsgnOrNode(pos, support.gettable2(((AssignableNode)yyVals[-4+yyTop])), ((AssignableNode)yyVals[-4+yyTop]));
                    } else if (asgnOp.equals("&&")) {
                        ((AssignableNode)yyVals[-4+yyTop]).setValueNode(((Node)yyVals[-2+yyTop]));
                        yyVal = new OpAsgnAndNode(pos, support.gettable2(((AssignableNode)yyVals[-4+yyTop])), ((AssignableNode)yyVals[-4+yyTop]));
                    } else {
                        ((AssignableNode)yyVals[-4+yyTop]).setValueNode(support.getOperatorCallNode(support.gettable2(((AssignableNode)yyVals[-4+yyTop])), asgnOp, ((Node)yyVals[-2+yyTop])));
                        ((AssignableNode)yyVals[-4+yyTop]).setPosition(pos);
                        yyVal = ((AssignableNode)yyVals[-4+yyTop]);
                    }
    return yyVal;
}
public static Object case271_line1053(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_colon3(((Token)yyVals[-1+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public static Object case467_line1784(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-7+yyTop]).getPosition(), ((ListNode)yyVals[-7+yyTop]), ((ListNode)yyVals[-5+yyTop]), ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case386_line1456(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IterNode(((Token)yyVals[-4+yyTop]).getPosition(), ((ArgsNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
    return yyVal;
}
public static Object case332_line1261(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((ListNode)yyVals[-5+yyTop]).getPosition(), ((ListNode)yyVals[-5+yyTop]), support.assignable(((Token)yyVals[-2+yyTop]), null), ((ListNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case289_line1114(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);
                    yyVal = new WhileNode(((Token)yyVals[-6+yyTop]).getPosition(), support.getConditionNode(((Node)yyVals[-4+yyTop])), body);
    return yyVal;
}
public static Object case213_line851(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
}
public static Object case266_line1032(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BeginNode(support.getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public static Object case8_line319(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case75_line559(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public static Object case85_line594(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = support.getPosition(((Node)yyVals[-2+yyTop]));

                    yyVal = new ConstDeclNode(position, null, support.new_colon2(position, ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
    return yyVal;
}
public static Object case226_line896(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    /* ENEBO: arg surrounded by in_defined set/unset*/
                    yyVal = new DefinedNode(((Token)yyVals[-2+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case493_line1901(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BlockNode(((Node)yyVals[0+yyTop]).getPosition()).add(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case457_line1745(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
    return yyVal;
}
public static Object case393_line1488(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null; 
    return yyVal;
}
public static Object case355_line1335(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.commandStart = true;
                    yyVal = ((ArgsNode)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case110_line696(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newUndef(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case70_line545(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((Token)yyVals[-1+yyTop]).getPosition(), null, ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public static Object case250_line970(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ISourcePosition pos = ((Node)yyVals[0+yyTop]) == null ? lexer.getPosition() : ((Node)yyVals[0+yyTop]).getPosition();
                    yyVal = support.newArrayNode(pos, ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case7_line316(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.appendToBlock(((Node)yyVals[-2+yyTop]), support.newline_node(((Node)yyVals[0+yyTop]), support.getPosition(((Node)yyVals[0+yyTop]))));
    return yyVal;
}
public static Object case79_line575(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case418_line1610(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                    ((ISourcePositionHolder)yyVal).setPosition(((Token)yyVals[-2+yyTop]).getPosition());
    return yyVal;
}
public static Object case365_line1371(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
                    yyVal = lexer.getLeftParenBegin();
                    lexer.setLeftParenBegin(lexer.incrementParenNest());
    return yyVal;
}
public static Object case63_line522(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((Token)yyVals[-2+yyTop]).getPosition(), support.newArrayNode(((Token)yyVals[-2+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop])), null, null);
    return yyVal;
}
public static Object case3_line288(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  Node node = ((Node)yyVals[-3+yyTop]);

                  if (((RescueBodyNode)yyVals[-2+yyTop]) != null) {
                      node = new RescueNode(support.getPosition(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop]), ((RescueBodyNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
                  } else if (((Node)yyVals[-1+yyTop]) != null) {
                      support.warn(ID.ELSE_WITHOUT_RESCUE, support.getPosition(((Node)yyVals[-3+yyTop])), "else without rescue is useless");
                      node = support.appendToBlock(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                  }
                  if (((Node)yyVals[0+yyTop]) != null) {
                      if (node == null) node = NilImplicitNode.NIL;
                      node = new EnsureNode(support.getPosition(((Node)yyVals[-3+yyTop])), node, ((Node)yyVals[0+yyTop]));
                  }

                  yyVal = node;
    return yyVal;
}
public static Object case308_line1188(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.setInSingle(support.getInSingle() + 1);
                    support.pushLocalScope();
                    lexer.setState(LexState.EXPR_END); /* force for args */
    return yyVal;
}
public static Object case273_line1065(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Hash19Node(((Token)yyVals[-2+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]));
    return yyVal;
}
public static Object case227_line900(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IfNode(support.getPosition(((Node)yyVals[-5+yyTop])), support.getConditionNode(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case78_line572(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case195_line789(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[-2+yyTop]));
                    support.checkExpression(((Node)yyVals[0+yyTop]));
    
                    boolean isLiteral = ((Node)yyVals[-2+yyTop]) instanceof FixnumNode && ((Node)yyVals[0+yyTop]) instanceof FixnumNode;
                    yyVal = new DotNode(support.getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), false, isLiteral);
    return yyVal;
}
public static Object case207_line833(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "|", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
}
public static Object case458_line1750(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case356_line1340(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((Token)yyVals[-2+yyTop]).getPosition(), null, null, null, null, null);
    return yyVal;
}
public static Object case206_line830(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "-@");
    return yyVal;
}
public static Object case51_line481(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IterNode(((Token)yyVals[-4+yyTop]).getPosition(), ((ArgsNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
    return yyVal;
}
public static Object case90_line623(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public static Object case309_line1192(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    /* TODO: We should use implicit nil for body, but problem (punt til later)*/
                    Node body = ((Node)yyVals[-1+yyTop]); /*$8 == null ? NilImplicitNode.NIL : $8;*/

                    yyVal = new DefsNode(((Token)yyVals[-8+yyTop]).getPosition(), ((Node)yyVals[-7+yyTop]), new ArgumentNode(((Token)yyVals[-4+yyTop]).getPosition(), (String) ((Token)yyVals[-4+yyTop]).getValue()), ((ArgsNode)yyVals[-2+yyTop]), support.getCurrentScope(), body);
                    support.popCurrentScope();
                    support.setInSingle(support.getInSingle() - 1);
    return yyVal;
}
public static Object case402_line1512(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    /* FIXME: We may be intern'ing more than once.*/
                    yyVal = new SymbolNode(((Token)yyVals[0+yyTop]).getPosition(), ((String) ((Token)yyVals[0+yyTop]).getValue()).intern());
    return yyVal;
}
public static Object case283_line1093(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (((Node)yyVals[-1+yyTop]) != null && 
                          ((BlockAcceptingNode)yyVals[-1+yyTop]).getIterNode() instanceof BlockPassNode) {
                        throw new SyntaxException(PID.BLOCK_ARG_AND_BLOCK_GIVEN, ((Node)yyVals[-1+yyTop]).getPosition(), lexer.getCurrentLine(), "Both block arg and actual block given.");
                    }
                    yyVal = ((BlockAcceptingNode)yyVals[-1+yyTop]).setIterNode(((IterNode)yyVals[0+yyTop]));
                    ((Node)yyVal).setPosition(((Node)yyVals[-1+yyTop]).getPosition());
    return yyVal;
}
public static Object case16_line343(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IfNode(support.getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), null, ((Node)yyVals[-2+yyTop]));
    return yyVal;
}
public static Object case61_line514(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public static Object case233_line919(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(((ListNode)yyVals[-1+yyTop]).getPosition(), new Hash19Node(lexer.getPosition(), ((ListNode)yyVals[-1+yyTop])));
    return yyVal;
}
public static Object case480_line1823(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(lexer.getPosition(), null, null, null, null, null);
    return yyVal;
}
public static Object case430_line1657(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = new GlobalVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public static Object case37_line441(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newOrNode(((Token)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case81_line582(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.aryset(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public static Object case222_line884(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<<", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
}
public static Object case65_line530(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((ListNode)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]).add(((Node)yyVals[0+yyTop])), null, null);
    return yyVal;
}
public static Object case506_line1950(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (!(((Node)yyVals[0+yyTop]) instanceof SelfNode)) {
                        support.checkExpression(((Node)yyVals[0+yyTop]));
                    }
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case473_line1802(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop]).getPosition(), null, ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case450_line1720(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("self", Tokens.kSELF, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public static Object case425_line1636(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case420_line1618(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public static Object case405_line1532(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new StrNode(((Token)yyVals[-1+yyTop]).getPosition(), ByteList.create((String) ((Token)yyVals[0+yyTop]).getValue()));
    return yyVal;
}
public static Object case342_line1293(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop]).getPosition(), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), null, ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case265_line1029(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new FCallNoArgNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public static Object case224_line890(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newAndNode(((Token)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case22_line374(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        support.warn(ID.END_IN_METHOD, ((Token)yyVals[-3+yyTop]).getPosition(), "END in method; use at_exit");
                    }
                    yyVal = new PostExeNode(((Token)yyVals[-3+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public static Object case503_line1933(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();

                    if (!support.is_local_id(((Token)yyVals[0+yyTop]))) {
                        support.yyerror("block argument must be local variable");
                    }
                    support.shadowing_lvar(((Token)yyVals[0+yyTop]));
                    yyVal = new BlockArgNode(((Token)yyVals[-1+yyTop]).getPosition(), support.arg_var(((Token)yyVals[0+yyTop])), identifier);
    return yyVal;
}
public static Object case434_line1669(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     lexer.setState(LexState.EXPR_END);
                     yyVal = ((Token)yyVals[0+yyTop]);
                     ((ISourcePositionHolder)yyVal).setPosition(((Token)yyVals[-1+yyTop]).getPosition());
    return yyVal;
}
public static Object case372_line1398(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IterNode(support.getPosition(((Token)yyVals[-4+yyTop])), ((ArgsNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
    return yyVal;
}
public static Object case276_line1074(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ZYieldNode(((Token)yyVals[-2+yyTop]).getPosition());
    return yyVal;
}
public static Object case255_line1007(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node node = null;

                    if (((Node)yyVals[0+yyTop]) instanceof ArrayNode &&
                        (node = support.splat_array(((Node)yyVals[-3+yyTop]))) != null) {
                        yyVal = support.list_concat(node, ((Node)yyVals[0+yyTop]));
                    } else {
                        yyVal = support.arg_concat(((Node)yyVals[-3+yyTop]).getPosition(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
                    }
    return yyVal;
}
public static Object case326_line1240(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
    return yyVal;
}
public static Object case306_line1178(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    /* TODO: We should use implicit nil for body, but problem (punt til later)*/
                    Node body = ((Node)yyVals[-1+yyTop]); /*$5 == null ? NilImplicitNode.NIL : $5;*/

                    yyVal = new DefnNode(((Token)yyVals[-5+yyTop]).getPosition(), new ArgumentNode(((Token)yyVals[-4+yyTop]).getPosition(), (String) ((Token)yyVals[-4+yyTop]).getValue()), ((ArgsNode)yyVals[-2+yyTop]), support.getCurrentScope(), body);
                    support.popCurrentScope();
                    support.setInDef(false);
    return yyVal;
}
public static Object case234_line923(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                    if (yyVal != null) ((Node)yyVal).setPosition(((Token)yyVals[-2+yyTop]).getPosition());
    return yyVal;
}
public static Object case321_line1227(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IfNode(((Token)yyVals[-4+yyTop]).getPosition(), support.getConditionNode(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case484_line1836(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("formal argument cannot be a class variable");
    return yyVal;
}
public static Object case14_line337(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case192_line780(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("constant re-assignment");
    return yyVal;
}
public static Object case33_line429(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                    yyVal = ((MultipleAsgn19Node)yyVals[-2+yyTop]);
                    ((MultipleAsgn19Node)yyVals[-2+yyTop]).setPosition(support.getPosition(((MultipleAsgn19Node)yyVals[-2+yyTop])));
    return yyVal;
}
public static Object case540_line2018(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                      yyVal = null;
    return yyVal;
}
public static Object case514_line1993(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ISourcePosition pos = ((Token)yyVals[-1+yyTop]).getPosition();
                    yyVal = support.newArrayNode(pos, new SymbolNode(pos, (String) ((Token)yyVals[-1+yyTop]).getValue())).add(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case333_line1264(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((ListNode)yyVals[-2+yyTop]).getPosition(), ((ListNode)yyVals[-2+yyTop]), new StarNode(lexer.getPosition()), null);
    return yyVal;
}
public static Object case220_line878(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(support.getConditionNode(((Node)yyVals[0+yyTop])), "!");
    return yyVal;
}
public static Object case18_line353(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = new UntilNode(support.getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                    } else {
                        yyVal = new UntilNode(support.getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                    }
    return yyVal;
}
public static Object case312_line1206(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new RedoNode(((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public static Object case416_line1603(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = support.literal_concat(support.getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case268_line1037(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.warning(ID.GROUPED_EXPRESSION, ((Token)yyVals[-3+yyTop]).getPosition(), "(...) interpreted as grouped expression");
                    yyVal = ((Node)yyVals[-2+yyTop]);
    return yyVal;
}
public static Object case483_line1833(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("formal argument cannot be a global variable");
    return yyVal;
}
public static Object case337_line1276(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((Token)yyVals[0+yyTop]).getPosition(), null, new StarNode(lexer.getPosition()), null);
    return yyVal;
}
public static Object case218_line866(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                  /* ENEBO
                        $$ = match_op($1, $3);
                        if (nd_type($1) == NODE_LIT && TYPE($1->nd_lit) == T_REGEXP) {
                            $$ = reg_named_capture_assign($1->nd_lit, $$);
                        }
                  */
    return yyVal;
}
public static Object case28_line413(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new OpAsgnNode(support.getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public static Object case532_line2007(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case439_line1678(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     lexer.setState(LexState.EXPR_END);

                     /* DStrNode: :"some text #{some expression}"*/
                     /* StrNode: :"some text"*/
                     /* EvStrNode :"#{some expression}"*/
                     if (((Node)yyVals[-1+yyTop]) == null) {
                       support.yyerror("empty symbol literal");
                     }
                     /* FIXME: No node here seems to be an empty string
                        instead of an error
                        if (!($$ = $2)) {
                        $$ = NEW_LIT(ID2SYM(rb_intern("")));
                        }
                     */

                     if (((Node)yyVals[-1+yyTop]) instanceof DStrNode) {
                         yyVal = new DSymbolNode(((Token)yyVals[-2+yyTop]).getPosition(), ((DStrNode)yyVals[-1+yyTop]));
                     } else {
                         yyVal = new DSymbolNode(((Token)yyVals[-2+yyTop]).getPosition());
                         ((DSymbolNode)yyVal).add(((Node)yyVals[-1+yyTop]));
                     }
    return yyVal;
}
public static Object case245_line952(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case59_line508(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_yield(((Token)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case311_line1203(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new NextNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
    return yyVal;
}
public static Object case428_line1647(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = lexer.getStrTerm();
                   lexer.setStrTerm(null);
                   lexer.setState(LexState.EXPR_BEG);
    return yyVal;
}
public static Object case287_line1110(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.getConditionState().begin();
    return yyVal;
}
public static Object case252_line977(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node node = support.splat_array(((Node)yyVals[-2+yyTop]));

                    if (node != null) {
                        yyVal = support.list_append(node, ((Node)yyVals[0+yyTop]));
                    } else {
                        yyVal = support.arg_append(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    }
    return yyVal;
}
public static Object case197_line803(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "+", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
}
public static Object case303_line1164(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) { 
                        support.yyerror("module definition in method body");
                    }
                    support.pushLocalScope();
    return yyVal;
}
public static Object case239_line933(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(support.getPosition(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case246_line957(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BlockPassNode(((Token)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case429_line1651(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.setStrTerm(((StrTerm)yyVals[-2+yyTop]));

                   yyVal = support.newEvStrNode(((Token)yyVals[-3+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public static Object case379_line1431(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop]), null, null);
    return yyVal;
}
public static Object case336_line1273(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((Token)yyVals[-3+yyTop]).getPosition(), null, support.assignable(((Token)yyVals[-2+yyTop]), null), ((ListNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case256_line1017(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = support.newSplatNode(support.getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));  
    return yyVal;
}
public static Object case340_line1287(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-7+yyTop]).getPosition(), ((ListNode)yyVals[-7+yyTop]), ((ListNode)yyVals[-5+yyTop]), ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case46_line465(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new NextNode(((Token)yyVals[-1+yyTop]).getPosition(), support.ret_args(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop]).getPosition()));
    return yyVal;
}
public static Object case76_line564(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(((Node)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public static Object case254_line998(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node node = support.splat_array(((Node)yyVals[-2+yyTop]));

                    if (node != null) {
                        yyVal = support.list_append(node, ((Node)yyVals[0+yyTop]));
                    } else {
                        yyVal = support.arg_append(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    }
    return yyVal;
}
public static Object case381_line1437(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop]), new Token("call", ((Node)yyVals[-2+yyTop]).getPosition()), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public static Object case88_line616(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                      /* if (!($$ = assignable($1, 0))) $$ = NEW_BEGIN(0);*/
                    yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
    return yyVal;
}
public static Object case109_line692(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case489_line1875(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ArrayNode(lexer.getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case479_line1820(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((BlockArgNode)yyVals[0+yyTop]).getPosition(), null, null, null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case478_line1817(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((RestArgNode)yyVals[-3+yyTop]).getPosition(), null, null, ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case461_line1760(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.setState(LexState.EXPR_BEG);
    return yyVal;
}
public static Object case460_line1757(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
}
public static Object case455_line1735(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("__ENCODING__", Tokens.k__ENCODING__, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public static Object case440_line1702(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case407_line1538(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.literal_concat(((Node)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case69_line542(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((ListNode)yyVals[-3+yyTop]).getPosition(), ((ListNode)yyVals[-3+yyTop]), new StarNode(lexer.getPosition()), ((ListNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case56_line499(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public static Object case20_line364(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("BEGIN in method");
                    }
                    support.pushLocalScope();
    return yyVal;
}
public static Object case470_line1793(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop]).getPosition(), ((ListNode)yyVals[-3+yyTop]), null, ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case421_line1622(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new StrNode(((Token)yyVals[0+yyTop]).getPosition(), ByteList.create(""));
    return yyVal;
}
public static Object case370_line1392(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public static Object case194_line786(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.backrefAssignError(((Node)yyVals[-2+yyTop]));
    return yyVal;
}
public static Object case204_line824(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((FloatNode)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), lexer.getPosition()), "-@");
    return yyVal;
}
public static Object case55_line496(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])); 
    return yyVal;
}
public static Object case107_line684(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new LiteralNode(((Token)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case112_line701(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.appendToBlock(((Node)yyVals[-3+yyTop]), support.newUndef(((Node)yyVals[-3+yyTop]).getPosition(), ((Node)yyVals[0+yyTop])));
    return yyVal;
}
public static Object case209_line839(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "&", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
}
public static Object case301_line1154(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = Integer.valueOf(support.getInSingle());
                    support.setInSingle(0);
                    support.pushLocalScope();
    return yyVal;
}
public static Object case6_line313(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newline_node(((Node)yyVals[0+yyTop]), support.getPosition(((Node)yyVals[0+yyTop])));
    return yyVal;
}
public static Object case508_line1958(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (((Node)yyVals[-1+yyTop]) == null) {
                        support.yyerror("can't define single method for ().");
                    } else if (((Node)yyVals[-1+yyTop]) instanceof ILiteralNode) {
                        support.yyerror("can't define single method for literals.");
                    }
                    support.checkExpression(((Node)yyVals[-1+yyTop]));
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public static Object case456_line1740(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.gettable(((Token)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case351_line1321(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((RestArgNode)yyVals[-1+yyTop]).getPosition(), null, null, ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case350_line1318(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop]).getPosition(), null, ((ListNode)yyVals[-3+yyTop]), null, ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case219_line875(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new NotNode(support.getPosition(((Node)yyVals[-2+yyTop])), support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
    return yyVal;
}
public static Object case89_line620(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.aryset(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public static Object case9_line323(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setState(LexState.EXPR_FNAME);
    return yyVal;
}
public static Object case296_line1134(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.getConditionState().end();
    return yyVal;
}
public static Object case188_line767(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
  /* FIXME: arg_concat missing for opt_call_args*/
                    yyVal = support.new_opElementAsgnNode(support.getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case509_line1969(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ArrayNode(lexer.getPosition());
    return yyVal;
}
public static Object case431_line1660(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = new InstVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public static Object case363_line1364(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.new_bv(((Token)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case232_line916(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.arg_append(((Node)yyVals[-3+yyTop]), new Hash19Node(lexer.getPosition(), ((ListNode)yyVals[-1+yyTop])));
    return yyVal;
}
public static Object case83_line588(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public static Object case211_line845(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
}
public static Object case15_line340(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IfNode(support.getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), null);
    return yyVal;
}
public static Object case39_line447(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(support.getConditionNode(((Node)yyVals[0+yyTop])), "!");
    return yyVal;
}
public static Object case221_line881(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "~");
    return yyVal;
}
public static Object case202_line818(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
}
public static Object case231_line913(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public static Object case285_line1104(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IfNode(((Token)yyVals[-5+yyTop]).getPosition(), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public static Object case23_line380(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case465_line1776(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ArgsNode)yyVals[-1+yyTop]);
    return yyVal;
}
public static Object case377_line1425(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public static Object case41_line452(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case190_line774(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new OpAsgnNode(support.getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public static Object case513_line1983(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ISourcePosition pos;
                    if (((Node)yyVals[-2+yyTop]) == null && ((Node)yyVals[0+yyTop]) == null) {
                        pos = ((Token)yyVals[-1+yyTop]).getPosition();
                    } else {
                        pos = ((Node)yyVals[-2+yyTop]).getPosition();
                    }

                    yyVal = support.newArrayNode(pos, ((Node)yyVals[-2+yyTop])).add(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case496_line1911(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case472_line1799(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case32_line425(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ((MultipleAsgn19Node)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                    yyVal = ((MultipleAsgn19Node)yyVals[-2+yyTop]);
    return yyVal;
}
public static Object case11_line328(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new VAliasNode(((Token)yyVals[-2+yyTop]).getPosition(), (String) ((Token)yyVals[-1+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public static Object case98_line659(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_colon3(((Token)yyVals[-1+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public static Object case466_line1781(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop]).getPosition(), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case358_line1346(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ArgsNode)yyVals[-2+yyTop]);
    return yyVal;
}
public static Object case26_line406(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
  /* FIXME: arg_concat logic missing for opt_call_args*/
                    yyVal = support.new_opElementAsgnNode(support.getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case453_line1729(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("__FILE__", Tokens.k__FILE__, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public static Object case389_line1469(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newWhenNode(((Token)yyVals[-4+yyTop]).getPosition(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case346_line1306(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case341_line1290(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop]).getPosition(), ((ListNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case494_line1904(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case275_line1071(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_yield(((Token)yyVals[-3+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public static Object case274_line1068(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ReturnNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
    return yyVal;
}
public static Object case476_line1811(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop]).getPosition(), null, ((ListNode)yyVals[-3+yyTop]), null, ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case307_line1186(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setState(LexState.EXPR_FNAME);
    return yyVal;
}
public static Object case243_line947(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
    return yyVal;
}
public static Object case492_line1892(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (!support.is_local_id(((Token)yyVals[-2+yyTop]))) {
                        support.yyerror("formal argument must be local variable");
                    }
                    support.shadowing_lvar(((Token)yyVals[-2+yyTop]));
                    support.arg_var(((Token)yyVals[-2+yyTop]));
                    yyVal = new OptArgNode(((Token)yyVals[-2+yyTop]).getPosition(), support.assignable(((Token)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
    return yyVal;
}
public static Object case368_line1385(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ArgsNode)yyVals[-1+yyTop]);
    return yyVal;
}
public static Object case361_line1357(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
}
public static Object case279_line1083(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(support.getConditionNode(((Node)yyVals[-1+yyTop])), "!");
    return yyVal;
}
public static Object case185_line725(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ISourcePosition position = ((Token)yyVals[-1+yyTop]).getPosition();
                    Node body = ((Node)yyVals[0+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop]);
                    yyVal = support.node_assign(((Node)yyVals[-4+yyTop]), new RescueNode(position, ((Node)yyVals[-2+yyTop]), new RescueBodyNode(position, null, body, null), null));
    return yyVal;
}
public static Object case504_line1943(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((BlockArgNode)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case424_line1632(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.literal_concat(support.getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case423_line1629(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
}
public static Object case410_line1573(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    int options = ((RegexpNode)yyVals[0+yyTop]).getOptions();
                    Node node = ((Node)yyVals[-1+yyTop]);

                    if (node == null) {
                        yyVal = new RegexpNode(((Token)yyVals[-2+yyTop]).getPosition(), ByteList.create(""), options & ~ReOptions.RE_OPTION_ONCE);
                    } else if (node instanceof StrNode) {
                        yyVal = new RegexpNode(((Node)yyVals[-1+yyTop]).getPosition(), (ByteList) ((StrNode) node).getValue().clone(), options & ~ReOptions.RE_OPTION_ONCE);
                    } else if (node instanceof DStrNode) {
                        yyVal = new DRegexpNode(((Token)yyVals[-2+yyTop]).getPosition(), (DStrNode) node, options, (options & ReOptions.RE_OPTION_ONCE) != 0);
                    } else {
                        yyVal = new DRegexpNode(((Token)yyVals[-2+yyTop]).getPosition(), options, (options & ReOptions.RE_OPTION_ONCE) != 0).add(node);
                    }
    return yyVal;
}
public static Object case104_line671(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.setState(LexState.EXPR_END);
                   yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case92_line629(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public static Object case272_line1056(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ISourcePosition position = ((Token)yyVals[-2+yyTop]).getPosition();
                    if (((Node)yyVals[-1+yyTop]) == null) {
                        yyVal = new ZArrayNode(position); /* zero length array */
                    } else {
                        yyVal = ((Node)yyVals[-1+yyTop]);
                        ((ISourcePositionHolder)yyVal).setPosition(position);
                    }
    return yyVal;
}
public static Object case105_line675(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.setState(LexState.EXPR_END);
                   yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case267_line1035(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setState(LexState.EXPR_ENDARG); 
    return yyVal;
}
public static Object case95_line650(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.backrefAssignError(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case329_line1251(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case281_line1089(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new FCallNoArgBlockNode(((Token)yyVals[-1+yyTop]).getPosition(), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((IterNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case531_line2004(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case404_line1518(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]) instanceof EvStrNode ? new DStrNode(((Node)yyVals[0+yyTop]).getPosition()).add(((Node)yyVals[0+yyTop])) : ((Node)yyVals[0+yyTop]);
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
public static Object case93_line632(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = support.getPosition(((Node)yyVals[-2+yyTop]));

                    yyVal = new ConstDeclNode(position, null, support.new_colon2(position, ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
    return yyVal;
}
public static Object case212_line848(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">=", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
}
public static Object case19_line360(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node body = ((Node)yyVals[0+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop]);
                    yyVal = new RescueNode(support.getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), new RescueBodyNode(support.getPosition(((Node)yyVals[-2+yyTop])), null, body, null), null);
    return yyVal;
}
public static Object case87_line612(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.backrefAssignError(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case486_line1842(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
    /* FIXME: Resolve what the hell is going on*/
    /*                    if (support.is_local_id($1)) {
                        support.yyerror("formal argument must be local variable");
                        }*/
                     
                    support.shadowing_lvar(((Token)yyVals[0+yyTop]));
                    yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case186_line730(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[0+yyTop]));

                    ISourcePosition pos = ((AssignableNode)yyVals[-2+yyTop]).getPosition();
                    String asgnOp = (String) ((Token)yyVals[-1+yyTop]).getValue();
                    if (asgnOp.equals("||")) {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                        yyVal = new OpAsgnOrNode(pos, support.gettable2(((AssignableNode)yyVals[-2+yyTop])), ((AssignableNode)yyVals[-2+yyTop]));
                    } else if (asgnOp.equals("&&")) {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                        yyVal = new OpAsgnAndNode(pos, support.gettable2(((AssignableNode)yyVals[-2+yyTop])), ((AssignableNode)yyVals[-2+yyTop]));
                    } else {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(support.getOperatorCallNode(support.gettable2(((AssignableNode)yyVals[-2+yyTop])), asgnOp, ((Node)yyVals[0+yyTop])));
                        ((AssignableNode)yyVals[-2+yyTop]).setPosition(pos);
                        yyVal = ((AssignableNode)yyVals[-2+yyTop]);
                    }
    return yyVal;
}
public static Object case247_line961(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((BlockPassNode)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case48_line471(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public static Object case413_line1595(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ArrayNode(lexer.getPosition());
    return yyVal;
}
public static Object case387_line1460(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
    return yyVal;
}
public static Object case331_line1258(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((ListNode)yyVals[-3+yyTop]).getPosition(), ((ListNode)yyVals[-3+yyTop]), support.assignable(((Token)yyVals[0+yyTop]), null), null);
    return yyVal;
}
public static Object case111_line699(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setState(LexState.EXPR_FNAME);
    return yyVal;
}
public static Object case71_line548(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((Token)yyVals[-3+yyTop]).getPosition(), null, ((Node)yyVals[-2+yyTop]), ((ListNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case217_line863(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "!=", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
}
public static Object case292_line1122(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);
                    yyVal = new UntilNode(((Token)yyVals[-6+yyTop]).getPosition(), support.getConditionNode(((Node)yyVals[-4+yyTop])), body);
    return yyVal;
}
public static Object case313_line1209(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new RetryNode(((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public static Object case84_line591(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public static Object case419_line1615(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ArrayNode(lexer.getPosition());
    return yyVal;
}
public static Object case314_line1213(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = ((Node)yyVals[0+yyTop]);
                    if (yyVal == null) yyVal = NilImplicitNode.NIL;
    return yyVal;
}
public static Object case54_line493(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public static Object case58_line505(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop])); /* .setPosFrom($2);*/
    return yyVal;
}
public static Object case380_line1434(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop]), new Token("call", ((Node)yyVals[-2+yyTop]).getPosition()), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public static Object case44_line459(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ReturnNode(((Token)yyVals[-1+yyTop]).getPosition(), support.ret_args(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop]).getPosition()));
    return yyVal;
}
public static Object case50_line479(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
    return yyVal;
}
public static Object case474_line1805(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop]).getPosition(), null, ((ListNode)yyVals[-5+yyTop]), ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case451_line1723(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("true", Tokens.kTRUE, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public static Object case406_line1535(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case343_line1296(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop]).getPosition(), ((ListNode)yyVals[-3+yyTop]), null, ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case64_line527(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((ListNode)yyVals[0+yyTop]).getPosition(), ((ListNode)yyVals[0+yyTop]), null, null);
    return yyVal;
}
public static Object case4_line305(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (((Node)yyVals[-1+yyTop]) instanceof BlockNode) {
                        support.checkUselessStatements(((BlockNode)yyVals[-1+yyTop]));
                    }
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public static Object case17_line346(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = new WhileNode(support.getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                    } else {
                        yyVal = new WhileNode(support.getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                    }
    return yyVal;
}
public static Object case80_line579(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
    return yyVal;
}
public static Object case463_line1765(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = null;
    return yyVal;
}
public static Object case384_line1446(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (((Node)yyVals[-3+yyTop]) instanceof SelfNode) {
                        yyVal = support.new_fcall(new Token("[]", support.getPosition(((Node)yyVals[-3+yyTop]))), ((Node)yyVals[-1+yyTop]), null);
                    } else {
                        yyVal = support.new_call(((Node)yyVals[-3+yyTop]), new Token("[]", support.getPosition(((Node)yyVals[-3+yyTop]))), ((Node)yyVals[-1+yyTop]), null);
                    }
    return yyVal;
}
public static Object case339_line1284(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop]).getPosition(), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case38_line444(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(support.getConditionNode(((Node)yyVals[0+yyTop])), "!");
    return yyVal;
}
public static Object case82_line585(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public static Object case223_line887(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">>", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
}
public static Object case277_line1077(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ZYieldNode(((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public static Object case253_line986(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node node = null;

                    /* FIXME: lose syntactical elements here (and others like this)*/
                    if (((Node)yyVals[0+yyTop]) instanceof ArrayNode &&
                        (node = support.splat_array(((Node)yyVals[-3+yyTop]))) != null) {
                        yyVal = support.list_concat(node, ((Node)yyVals[0+yyTop]));
                    } else {
                        yyVal = support.arg_concat(support.getPosition(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
                    }
    return yyVal;
}
public static Object case327_line1243(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public static Object case210_line842(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=>", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
}
public static Object case505_line1946(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
}
public static Object case443_line1711(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = support.negateFloat(((FloatNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case442_line1708(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = support.negateInteger(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case487_line1852(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.arg_var(((Token)yyVals[0+yyTop]));
                    yyVal = new ArgumentNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
  /*
                    $$ = new ArgAuxiliaryNode($1.getPosition(), (String) $1.getValue(), 1);
  */
    return yyVal;
}
public static Object case189_line771(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new OpAsgnNode(support.getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public static Object case196_line796(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[-2+yyTop]));
                    support.checkExpression(((Node)yyVals[0+yyTop]));

                    boolean isLiteral = ((Node)yyVals[-2+yyTop]) instanceof FixnumNode && ((Node)yyVals[0+yyTop]) instanceof FixnumNode;
                    yyVal = new DotNode(support.getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), true, isLiteral);
    return yyVal;
}
public static Object case199_line809(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "*", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
}
public static Object case488_line1859(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
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
public static Object case464_line1771(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ArgsNode)yyVals[-1+yyTop]);
                    ((ISourcePositionHolder)yyVal).setPosition(((Token)yyVals[-2+yyTop]).getPosition());
                    lexer.setState(LexState.EXPR_BEG);
    return yyVal;
}
public static Object case409_line1557(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ISourcePosition position = ((Token)yyVals[-2+yyTop]).getPosition();

                    if (((Node)yyVals[-1+yyTop]) == null) {
                        yyVal = new XStrNode(position, null);
                    } else if (((Node)yyVals[-1+yyTop]) instanceof StrNode) {
                        yyVal = new XStrNode(position, (ByteList) ((StrNode)yyVals[-1+yyTop]).getValue().clone());
                    } else if (((Node)yyVals[-1+yyTop]) instanceof DStrNode) {
                        yyVal = new DXStrNode(position, ((DStrNode)yyVals[-1+yyTop]));

                        ((Node)yyVal).setPosition(position);
                    } else {
                        yyVal = new DXStrNode(position).add(((Node)yyVals[-1+yyTop]));
                    }
    return yyVal;
}
public static Object case375_line1417(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public static Object case353_line1327(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((BlockArgNode)yyVals[0+yyTop]).getPosition(), null, null, null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case294_line1129(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newCaseNode(((Token)yyVals[-3+yyTop]).getPosition(), null, ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public static Object case270_line1050(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_colon2(support.getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public static Object case100_line665(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_colon2(support.getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public static Object case25_line389(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[0+yyTop]));

                    ISourcePosition pos = ((AssignableNode)yyVals[-2+yyTop]).getPosition();
                    String asgnOp = (String) ((Token)yyVals[-1+yyTop]).getValue();
                    if (asgnOp.equals("||")) {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                        yyVal = new OpAsgnOrNode(pos, support.gettable2(((AssignableNode)yyVals[-2+yyTop])), ((AssignableNode)yyVals[-2+yyTop]));
                    } else if (asgnOp.equals("&&")) {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                        yyVal = new OpAsgnAndNode(pos, support.gettable2(((AssignableNode)yyVals[-2+yyTop])), ((AssignableNode)yyVals[-2+yyTop]));
                    } else {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(support.getOperatorCallNode(support.gettable2(((AssignableNode)yyVals[-2+yyTop])), asgnOp, ((Node)yyVals[0+yyTop])));
                        ((AssignableNode)yyVals[-2+yyTop]).setPosition(pos);
                        yyVal = ((AssignableNode)yyVals[-2+yyTop]);
                    }
    return yyVal;
}
public static Object case203_line821(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), lexer.getPosition()), "-@");
    return yyVal;
}
public static Object case10_line325(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newAlias(((Token)yyVals[-3+yyTop]).getPosition(), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case441_line1705(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = ((FloatNode)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case411_line1588(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ZArrayNode(((Token)yyVals[-2+yyTop]).getPosition());
    return yyVal;
}
public static Object case354_line1331(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
    /* was $$ = null;*/
                   yyVal = support.new_args(lexer.getPosition(), null, null, null, null, null);
    return yyVal;
}
public static Object case338_line1279(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((Token)yyVals[-2+yyTop]).getPosition(), null, null, ((ListNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case13_line334(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("can't make alias for the number variables");
    return yyVal;
}
public static Object case385_line1454(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
    return yyVal;
}
public static Object case305_line1175(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.setInDef(true);
                    support.pushLocalScope();
    return yyVal;
}
public static Object case99_line662(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_colon2(((Token)yyVals[0+yyTop]).getPosition(), null, (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public static Object case288_line1112(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.getConditionState().end();
    return yyVal;
}
public static Object case374_line1414(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public static Object case371_line1396(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
    return yyVal;
}
public static Object case352_line1324(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((RestArgNode)yyVals[-3+yyTop]).getPosition(), null, null, ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public static Object case297_line1136(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                      /* ENEBO: Lots of optz in 1.9 parser here*/
                    yyVal = new ForNode(((Token)yyVals[-8+yyTop]).getPosition(), ((Node)yyVals[-7+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-4+yyTop]));
    return yyVal;
}
public static Object case302_line1158(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new SClassNode(((Token)yyVals[-7+yyTop]).getPosition(), ((Node)yyVals[-5+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                    support.popCurrentScope();
                    support.setInDef(((Boolean)yyVals[-4+yyTop]).booleanValue());
                    support.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
    return yyVal;
}
public static Object case299_line1145(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);

                    yyVal = new ClassNode(((Token)yyVals[-5+yyTop]).getPosition(), ((Colon3Node)yyVals[-4+yyTop]), support.getCurrentScope(), body, ((Node)yyVals[-3+yyTop]));
                    support.popCurrentScope();
    return yyVal;
}
public static Object case269_line1041(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (((Node)yyVals[-1+yyTop]) != null) {
                        /* compstmt position includes both parens around it*/
                        ((ISourcePositionHolder) ((Node)yyVals[-1+yyTop])).setPosition(((Token)yyVals[-2+yyTop]).getPosition());
                        yyVal = ((Node)yyVals[-1+yyTop]);
                    } else {
                        yyVal = new NilNode(((Token)yyVals[-2+yyTop]).getPosition());
                    }
    return yyVal;
}
public static Object case510_line1972(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
    return yyVal;
}
public static Object case459_line1753(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public static Object case366_line1375(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new LambdaNode(((ArgsNode)yyVals[-1+yyTop]).getPosition(), ((ArgsNode)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
                    lexer.setLeftParenBegin(((Integer)yyVals[-2+yyTop]));
    return yyVal;
}
public static Object case298_line1140(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("class definition in method body");
                    }
                    support.pushLocalScope();
    return yyVal;
}
public static Object case30_line419(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.backrefAssignError(((Node)yyVals[-2+yyTop]));
    return yyVal;
}
public static Object case27_line410(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new OpAsgnNode(support.getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public static Object case304_line1169(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);

                    yyVal = new ModuleNode(((Token)yyVals[-4+yyTop]).getPosition(), ((Colon3Node)yyVals[-3+yyTop]), support.getCurrentScope(), body);
                    support.popCurrentScope();
    return yyVal;
}
					// line 2027 "Ruby19Parser.y"

    /** The parse method use an lexer stream and parse it to an AST node 
     * structure
     */
    public RubyParserResult parse(ParserConfiguration configuration, LexerSource source) throws IOException {
        support.reset();
        support.setConfiguration(configuration);
        support.setResult(new RubyParserResult());
        
        lexer.reset();
        lexer.setSource(source);
        lexer.setEncoding(configuration.getKCode().getEncoding());

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
					// line 8043 "-"
