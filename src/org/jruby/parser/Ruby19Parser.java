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

import org.jruby.ast.AliasNode;
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
import org.jruby.ast.CaseNode;
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
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgn19Node;
import org.jruby.ast.NewlineNode;
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
import org.jruby.ast.SValueNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.StarNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.ToAryNode;
import org.jruby.ast.TypedArgumentNode;
import org.jruby.ast.UndefNode;
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
    protected IRubyWarnings warnings;

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
        this.warnings = warnings;

        support.setWarnings(warnings);
        lexer.setWarnings(warnings);
    }
					// line 152 "-"
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
    -1,   117,     0,    35,    34,    36,    36,    36,    36,   120,
    37,    37,    37,    37,    37,    37,    37,    37,    37,    37,
   121,    37,    37,    37,    37,    37,    37,    37,    37,    37,
    37,    37,    37,    37,    37,    38,    38,    38,    38,    38,
    38,    42,    33,    33,    33,    33,    33,    56,    56,    56,
   122,    95,    41,    41,    41,    41,    41,    41,    41,    41,
    96,    96,   105,   105,    97,    97,    97,    97,    97,    97,
    97,    97,    97,    97,    68,    68,    82,    82,    86,    86,
    69,    69,    69,    69,    69,    69,    69,    69,    74,    74,
    74,    74,    74,    74,    74,    74,     8,     8,    32,    32,
    32,     9,     9,     9,     9,     9,   112,   112,     2,     2,
    58,   123,    58,    10,    10,    10,    10,    10,    10,    10,
    10,    10,    10,    10,    10,    10,    10,    10,    10,    10,
    10,    10,    10,    10,    10,    10,    10,    10,    10,    10,
    10,    10,   115,   115,   115,   115,   115,   115,   115,   115,
   115,   115,   115,   115,   115,   115,   115,   115,   115,   115,
   115,   115,   115,   115,   115,   115,   115,   115,   115,   115,
   115,   115,   115,   115,   115,   115,   115,   115,   115,   115,
   115,   115,   115,   115,    39,    39,    39,    39,    39,    39,
    39,    39,    39,    39,    39,    39,    39,    39,    39,    39,
    39,    39,    39,    39,    39,    39,    39,    39,    39,    39,
    39,    39,    39,    39,    39,    39,    39,    39,    39,    39,
    39,    39,    39,    39,    39,    39,    39,    39,    39,    70,
    73,    73,    73,    73,    50,    54,    54,   108,   108,    48,
    48,    48,    48,    48,   126,    52,    89,    88,    88,    88,
    76,    76,    76,    76,    67,    67,    67,    40,    40,    40,
    40,    40,    40,    40,    40,    40,    40,   127,    40,    40,
    40,    40,    40,    40,    40,    40,    40,    40,    40,    40,
    40,    40,    40,    40,    40,    40,    40,   129,   131,    40,
   132,   133,    40,    40,    40,   134,   135,    40,   136,    40,
   138,   139,    40,   140,    40,   141,    40,   142,   143,    40,
    40,    40,    40,    40,    43,   128,   128,   128,   130,   130,
    46,    46,    44,    44,   107,   107,   109,   109,    81,    81,
   110,   110,   110,   110,   110,   110,   110,   110,   110,    64,
    64,    64,    64,    64,    64,    64,    64,    64,    64,    64,
    64,    64,    64,    64,    66,    66,    65,    65,    65,   102,
   102,   101,   101,   111,   111,   144,   104,    63,    63,   103,
   103,   145,    94,    55,    55,    55,    25,    25,    25,    25,
    25,    25,    25,    25,    25,   146,    93,   147,    93,    71,
    45,    45,    98,    98,    72,    72,    72,    47,    47,    49,
    49,    29,    29,    29,    17,    18,    18,    18,    19,    20,
    21,    26,    26,    78,    78,    28,    28,    27,    27,    77,
    77,    22,    22,    23,    23,    24,   148,    24,   149,    24,
    59,    59,    59,    59,     4,     3,     3,     3,     3,    31,
    30,    30,    30,    30,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,    53,    99,    60,    60,
    51,   150,    51,    51,    62,    62,    61,    61,    61,    61,
    61,    61,    61,    61,    61,    61,    61,    61,    61,    61,
    61,   116,   116,   116,   116,    11,    11,   100,   100,    79,
    79,    57,   106,    87,    87,    80,    80,    13,    13,    15,
    15,    14,    14,    92,    91,    91,    16,   151,    16,    85,
    85,    83,    83,    84,    84,     5,     5,     5,     6,     6,
     6,     6,     7,     7,     7,    12,    12,   118,   118,   124,
   124,   113,   114,   125,   125,   125,   137,   137,   119,   119,
    75,    90,
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
   172,   165,   175,   160,   142,   143,   144,   101,   102,   110,
   107,   106,   109,     0,   108,     0,     0,     0,     0,     0,
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
     1,   218,   209,   300,    64,   113,   585,   553,   114,   211,
   547,   492,   388,   493,   494,   495,   197,    66,    67,    68,
    69,    70,   303,   302,   469,    71,    72,    73,   477,    74,
    75,    76,   115,    77,   215,   216,    79,    80,    81,    82,
    83,    84,   220,   270,   730,   874,   731,   723,   428,   727,
   555,   378,   256,    86,   692,    87,    88,   496,   213,   755,
   222,   591,   592,   498,   780,   681,   682,   566,    90,    91,
   248,   406,   597,   280,   223,    93,   249,   309,   307,   499,
   500,   661,    94,   250,   251,   287,   460,   782,   420,   252,
   421,   668,   765,   316,   355,   507,    95,    96,   391,   224,
   502,   767,   671,   674,   310,   278,   785,   240,   430,   662,
   663,   768,   214,   425,   698,   199,   503,     2,   229,   230,
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
     0,     0,     0,   472,     0,   264,   269,   236,     0,   -22,
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
     0,     0,     0, 12674,     0,   362,     0,     0,  6137,  5102,
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
     0,   504,   -23,     0,    -5,   177,  -240,     0,   -47,    20,
    -6,  -312,     0,     0,     0,   620,     0,     0,     0,   987,
     0,     0,     0,   607,  -228,     0,     0,     0,     0,     0,
     0,    27,  1051,    33,   406,  -342,     0,   164,  1170,  1338,
    72,    16,    57,     4,  -386,     0,   160,     0,   680,     0,
   195,     0,    18,  1066,   105,     0,     0,  -599,     0,     0,
   655,  -249,   261,     0,     0,     0,  -370,  -126,   -43,   -13,
   799,  -375,     0,     0,    91,   759,   106,     0,     0,  5452,
   402,  -677,     0,   -37,  -197,     0,  -400,   232,  -208,   -87,
     0,   936,  -278,  1015,     0,  -439,  1073,   167,   218,   609,
  -605,     0,  -474,     0,     0,  -175,  -773,     0,  -355,  -613,
   433,   257,     0,   304,  -311,     0,  -598,     0,     1,  1018,
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

  /** thrown for irrecoverable syntax errors and stack overflow.
      Nested for convenience, does not depend on parser class.
    */
  public static class yyException extends java.lang.Exception {
    private static final long serialVersionUID = 1L;
    public yyException (String message) {
      super(message);
    }
  }

  /** must be implemented by a scanner object to supply input to the parser.
      Nested for convenience, does not depend on parser class.
    */
  public interface yyInput {

    /** move on to next token.
        @return <tt>false</tt> if positioned beyond tokens.
        @throws IOException on input error.
      */
    boolean advance () throws java.io.IOException;

    /** classifies current token.
        Should not be called if {@link #advance()} returned <tt>false</tt>.
        @return current <tt>%token</tt> or single character.
      */
    int token ();

    /** associated with current token.
        Should not be called if {@link #advance()} returned <tt>false</tt>.
        @return value for {@link #token()}.
      */
    Object value ();
  }

  /** simplified error message.
      @see #yyerror(java.lang.String, java.lang.String[])
    */
  public void yyerror (String message) {
    //new Exception().printStackTrace();
    throw new SyntaxException(PID.GRAMMAR_ERROR, getPosition(null), lexer.getCurrentLine(), message);
  }

  /** (syntax) error message.
      Can be overwritten to control message format.
      @param message text to be displayed.
      @param expected list of acceptable tokens, if available.
    */
  public void yyerror (String message, String[] expected, String found) {
    String text = ", unexpected " + found + "\n";
    //new Exception().printStackTrace();
    throw new SyntaxException(PID.GRAMMAR_ERROR, getPosition(null), lexer.getCurrentLine(), text, found);
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
      @throws yyException on irrecoverable parse error.
    */
  public Object yyparse (RubyYaccLexer yyLex, Object ayydebug)
				throws java.io.IOException, yyException {
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
      @throws yyException on irrecoverable parse error.
    */
  public Object yyparse (RubyYaccLexer yyLex) throws java.io.IOException, yyException {
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
              throw new yyException("irrecoverable syntax error");
  
            case 3:
              if (yyToken == 0) {
                if (yydebug != null) yydebug.reject();
                throw new yyException("irrecoverable syntax error at end-of-file");
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
case 1: yyVal = case1_line280(yyVal, yyVals, yyTop); // line 280
break;
case 2: yyVal = case2_line283(yyVal, yyVals, yyTop); // line 283
break;
case 3: yyVal = case3_line296(yyVal, yyVals, yyTop); // line 296
break;
case 4: yyVal = case4_line313(yyVal, yyVals, yyTop); // line 313
break;
case 6: yyVal = case6_line321(yyVal, yyVals, yyTop); // line 321
break;
case 7: yyVal = case7_line324(yyVal, yyVals, yyTop); // line 324
break;
case 8: yyVal = case8_line327(yyVal, yyVals, yyTop); // line 327
break;
case 9: yyVal = case9_line331(yyVal, yyVals, yyTop); // line 331
break;
case 10: yyVal = case10_line333(yyVal, yyVals, yyTop); // line 333
break;
case 11: yyVal = case11_line336(yyVal, yyVals, yyTop); // line 336
break;
case 12: yyVal = case12_line339(yyVal, yyVals, yyTop); // line 339
break;
case 13: yyVal = case13_line342(yyVal, yyVals, yyTop); // line 342
break;
case 14: yyVal = case14_line345(yyVal, yyVals, yyTop); // line 345
break;
case 15: yyVal = case15_line348(yyVal, yyVals, yyTop); // line 348
break;
case 16: yyVal = case16_line351(yyVal, yyVals, yyTop); // line 351
break;
case 17: yyVal = case17_line354(yyVal, yyVals, yyTop); // line 354
break;
case 18: yyVal = case18_line361(yyVal, yyVals, yyTop); // line 361
break;
case 19: yyVal = case19_line368(yyVal, yyVals, yyTop); // line 368
break;
case 20: yyVal = case20_line372(yyVal, yyVals, yyTop); // line 372
break;
case 21: yyVal = case21_line377(yyVal, yyVals, yyTop); // line 377
break;
case 22: yyVal = case22_line382(yyVal, yyVals, yyTop); // line 382
break;
case 23: yyVal = case23_line388(yyVal, yyVals, yyTop); // line 388
break;
case 24: yyVal = case24_line392(yyVal, yyVals, yyTop); // line 392
break;
case 25: yyVal = case25_line397(yyVal, yyVals, yyTop); // line 397
break;
case 26: yyVal = case26_line413(yyVal, yyVals, yyTop); // line 413
break;
case 27: yyVal = case27_line417(yyVal, yyVals, yyTop); // line 417
break;
case 28: yyVal = case28_line420(yyVal, yyVals, yyTop); // line 420
break;
case 29: yyVal = case29_line423(yyVal, yyVals, yyTop); // line 423
break;
case 30: yyVal = case30_line426(yyVal, yyVals, yyTop); // line 426
break;
case 31: yyVal = case31_line429(yyVal, yyVals, yyTop); // line 429
break;
case 32: yyVal = case32_line432(yyVal, yyVals, yyTop); // line 432
break;
case 33: yyVal = case33_line436(yyVal, yyVals, yyTop); // line 436
break;
case 36: yyVal = case36_line445(yyVal, yyVals, yyTop); // line 445
break;
case 37: yyVal = case37_line448(yyVal, yyVals, yyTop); // line 448
break;
case 38: yyVal = case38_line451(yyVal, yyVals, yyTop); // line 451
break;
case 39: yyVal = case39_line454(yyVal, yyVals, yyTop); // line 454
break;
case 41: yyVal = case41_line459(yyVal, yyVals, yyTop); // line 459
break;
case 44: yyVal = case44_line466(yyVal, yyVals, yyTop); // line 466
break;
case 45: yyVal = case45_line469(yyVal, yyVals, yyTop); // line 469
break;
case 46: yyVal = case46_line472(yyVal, yyVals, yyTop); // line 472
break;
case 48: yyVal = case48_line478(yyVal, yyVals, yyTop); // line 478
break;
case 49: yyVal = case49_line481(yyVal, yyVals, yyTop); // line 481
break;
case 50: yyVal = case50_line486(yyVal, yyVals, yyTop); // line 486
break;
case 51: yyVal = case51_line488(yyVal, yyVals, yyTop); // line 488
break;
case 52: yyVal = case52_line494(yyVal, yyVals, yyTop); // line 494
break;
case 53: yyVal = case53_line497(yyVal, yyVals, yyTop); // line 497
break;
case 54: yyVal = case54_line500(yyVal, yyVals, yyTop); // line 500
break;
case 55: yyVal = case55_line503(yyVal, yyVals, yyTop); // line 503
break;
case 56: yyVal = case56_line506(yyVal, yyVals, yyTop); // line 506
break;
case 57: yyVal = case57_line509(yyVal, yyVals, yyTop); // line 509
break;
case 58: yyVal = case58_line512(yyVal, yyVals, yyTop); // line 512
break;
case 59: yyVal = case59_line515(yyVal, yyVals, yyTop); // line 515
break;
case 61: yyVal = case61_line521(yyVal, yyVals, yyTop); // line 521
break;
case 62: yyVal = case62_line526(yyVal, yyVals, yyTop); // line 526
break;
case 63: yyVal = case63_line529(yyVal, yyVals, yyTop); // line 529
break;
case 64: yyVal = case64_line534(yyVal, yyVals, yyTop); // line 534
break;
case 65: yyVal = case65_line537(yyVal, yyVals, yyTop); // line 537
break;
case 66: yyVal = case66_line540(yyVal, yyVals, yyTop); // line 540
break;
case 67: yyVal = case67_line543(yyVal, yyVals, yyTop); // line 543
break;
case 68: yyVal = case68_line546(yyVal, yyVals, yyTop); // line 546
break;
case 69: yyVal = case69_line549(yyVal, yyVals, yyTop); // line 549
break;
case 70: yyVal = case70_line552(yyVal, yyVals, yyTop); // line 552
break;
case 71: yyVal = case71_line555(yyVal, yyVals, yyTop); // line 555
break;
case 72: yyVal = case72_line558(yyVal, yyVals, yyTop); // line 558
break;
case 73: yyVal = case73_line561(yyVal, yyVals, yyTop); // line 561
break;
case 75: yyVal = case75_line566(yyVal, yyVals, yyTop); // line 566
break;
case 76: yyVal = case76_line571(yyVal, yyVals, yyTop); // line 571
break;
case 77: yyVal = case77_line574(yyVal, yyVals, yyTop); // line 574
break;
case 78: yyVal = case78_line579(yyVal, yyVals, yyTop); // line 579
break;
case 79: yyVal = case79_line582(yyVal, yyVals, yyTop); // line 582
break;
case 80: yyVal = case80_line586(yyVal, yyVals, yyTop); // line 586
break;
case 81: yyVal = case81_line589(yyVal, yyVals, yyTop); // line 589
break;
case 82: yyVal = case82_line592(yyVal, yyVals, yyTop); // line 592
break;
case 83: yyVal = case83_line595(yyVal, yyVals, yyTop); // line 595
break;
case 84: yyVal = case84_line598(yyVal, yyVals, yyTop); // line 598
break;
case 85: yyVal = case85_line601(yyVal, yyVals, yyTop); // line 601
break;
case 86: yyVal = case86_line610(yyVal, yyVals, yyTop); // line 610
break;
case 87: yyVal = case87_line619(yyVal, yyVals, yyTop); // line 619
break;
case 88: yyVal = case88_line623(yyVal, yyVals, yyTop); // line 623
break;
case 89: yyVal = case89_line627(yyVal, yyVals, yyTop); // line 627
break;
case 90: yyVal = case90_line630(yyVal, yyVals, yyTop); // line 630
break;
case 91: yyVal = case91_line633(yyVal, yyVals, yyTop); // line 633
break;
case 92: yyVal = case92_line636(yyVal, yyVals, yyTop); // line 636
break;
case 93: yyVal = case93_line639(yyVal, yyVals, yyTop); // line 639
break;
case 94: yyVal = case94_line648(yyVal, yyVals, yyTop); // line 648
break;
case 95: yyVal = case95_line657(yyVal, yyVals, yyTop); // line 657
break;
case 96: yyVal = case96_line661(yyVal, yyVals, yyTop); // line 661
break;
case 98: yyVal = case98_line666(yyVal, yyVals, yyTop); // line 666
break;
case 99: yyVal = case99_line669(yyVal, yyVals, yyTop); // line 669
break;
case 100: yyVal = case100_line672(yyVal, yyVals, yyTop); // line 672
break;
case 104: yyVal = case104_line678(yyVal, yyVals, yyTop); // line 678
break;
case 105: yyVal = case105_line682(yyVal, yyVals, yyTop); // line 682
break;
case 106: yyVal = case106_line688(yyVal, yyVals, yyTop); // line 688
break;
case 107: yyVal = case107_line691(yyVal, yyVals, yyTop); // line 691
break;
case 108: yyVal = case108_line696(yyVal, yyVals, yyTop); // line 696
break;
case 109: yyVal = case109_line699(yyVal, yyVals, yyTop); // line 699
break;
case 110: yyVal = case110_line703(yyVal, yyVals, yyTop); // line 703
break;
case 111: yyVal = case111_line706(yyVal, yyVals, yyTop); // line 706
break;
case 112: yyVal = case112_line708(yyVal, yyVals, yyTop); // line 708
break;
case 184: yyVal = case184_line727(yyVal, yyVals, yyTop); // line 727
break;
case 185: yyVal = case185_line732(yyVal, yyVals, yyTop); // line 732
break;
case 186: yyVal = case186_line737(yyVal, yyVals, yyTop); // line 737
break;
case 187: yyVal = case187_line753(yyVal, yyVals, yyTop); // line 753
break;
case 188: yyVal = case188_line772(yyVal, yyVals, yyTop); // line 772
break;
case 189: yyVal = case189_line776(yyVal, yyVals, yyTop); // line 776
break;
case 190: yyVal = case190_line779(yyVal, yyVals, yyTop); // line 779
break;
case 191: yyVal = case191_line782(yyVal, yyVals, yyTop); // line 782
break;
case 192: yyVal = case192_line785(yyVal, yyVals, yyTop); // line 785
break;
case 193: yyVal = case193_line788(yyVal, yyVals, yyTop); // line 788
break;
case 194: yyVal = case194_line791(yyVal, yyVals, yyTop); // line 791
break;
case 195: yyVal = case195_line794(yyVal, yyVals, yyTop); // line 794
break;
case 196: yyVal = case196_line801(yyVal, yyVals, yyTop); // line 801
break;
case 197: yyVal = case197_line808(yyVal, yyVals, yyTop); // line 808
break;
case 198: yyVal = case198_line811(yyVal, yyVals, yyTop); // line 811
break;
case 199: yyVal = case199_line814(yyVal, yyVals, yyTop); // line 814
break;
case 200: yyVal = case200_line817(yyVal, yyVals, yyTop); // line 817
break;
case 201: yyVal = case201_line820(yyVal, yyVals, yyTop); // line 820
break;
case 202: yyVal = case202_line823(yyVal, yyVals, yyTop); // line 823
break;
case 203: yyVal = case203_line826(yyVal, yyVals, yyTop); // line 826
break;
case 204: yyVal = case204_line829(yyVal, yyVals, yyTop); // line 829
break;
case 205: yyVal = case205_line832(yyVal, yyVals, yyTop); // line 832
break;
case 206: yyVal = case206_line839(yyVal, yyVals, yyTop); // line 839
break;
case 207: yyVal = case207_line842(yyVal, yyVals, yyTop); // line 842
break;
case 208: yyVal = case208_line845(yyVal, yyVals, yyTop); // line 845
break;
case 209: yyVal = case209_line848(yyVal, yyVals, yyTop); // line 848
break;
case 210: yyVal = case210_line851(yyVal, yyVals, yyTop); // line 851
break;
case 211: yyVal = case211_line854(yyVal, yyVals, yyTop); // line 854
break;
case 212: yyVal = case212_line857(yyVal, yyVals, yyTop); // line 857
break;
case 213: yyVal = case213_line860(yyVal, yyVals, yyTop); // line 860
break;
case 214: yyVal = case214_line863(yyVal, yyVals, yyTop); // line 863
break;
case 215: yyVal = case215_line866(yyVal, yyVals, yyTop); // line 866
break;
case 216: yyVal = case216_line869(yyVal, yyVals, yyTop); // line 869
break;
case 217: yyVal = case217_line872(yyVal, yyVals, yyTop); // line 872
break;
case 218: yyVal = case218_line875(yyVal, yyVals, yyTop); // line 875
break;
case 219: yyVal = case219_line884(yyVal, yyVals, yyTop); // line 884
break;
case 220: yyVal = case220_line887(yyVal, yyVals, yyTop); // line 887
break;
case 221: yyVal = case221_line890(yyVal, yyVals, yyTop); // line 890
break;
case 222: yyVal = case222_line893(yyVal, yyVals, yyTop); // line 893
break;
case 223: yyVal = case223_line896(yyVal, yyVals, yyTop); // line 896
break;
case 224: yyVal = case224_line899(yyVal, yyVals, yyTop); // line 899
break;
case 225: yyVal = case225_line902(yyVal, yyVals, yyTop); // line 902
break;
case 226: yyVal = case226_line905(yyVal, yyVals, yyTop); // line 905
break;
case 227: yyVal = case227_line909(yyVal, yyVals, yyTop); // line 909
break;
case 228: yyVal = case228_line912(yyVal, yyVals, yyTop); // line 912
break;
case 229: yyVal = case229_line916(yyVal, yyVals, yyTop); // line 916
break;
case 231: yyVal = case231_line922(yyVal, yyVals, yyTop); // line 922
break;
case 232: yyVal = case232_line925(yyVal, yyVals, yyTop); // line 925
break;
case 233: yyVal = case233_line928(yyVal, yyVals, yyTop); // line 928
break;
case 234: yyVal = case234_line932(yyVal, yyVals, yyTop); // line 932
break;
case 239: yyVal = case239_line941(yyVal, yyVals, yyTop); // line 941
break;
case 240: yyVal = case240_line944(yyVal, yyVals, yyTop); // line 944
break;
case 241: yyVal = case241_line947(yyVal, yyVals, yyTop); // line 947
break;
case 242: yyVal = case242_line951(yyVal, yyVals, yyTop); // line 951
break;
case 243: yyVal = case243_line955(yyVal, yyVals, yyTop); // line 955
break;
case 244: yyVal = case244_line958(yyVal, yyVals, yyTop); // line 958
break;
case 245: yyVal = case245_line960(yyVal, yyVals, yyTop); // line 960
break;
case 246: yyVal = case246_line965(yyVal, yyVals, yyTop); // line 965
break;
case 247: yyVal = case247_line969(yyVal, yyVals, yyTop); // line 969
break;
case 248: yyVal = case248_line972(yyVal, yyVals, yyTop); // line 972
break;
case 250: yyVal = case250_line977(yyVal, yyVals, yyTop); // line 977
break;
case 251: yyVal = case251_line980(yyVal, yyVals, yyTop); // line 980
break;
case 252: yyVal = case252_line983(yyVal, yyVals, yyTop); // line 983
break;
case 253: yyVal = case253_line992(yyVal, yyVals, yyTop); // line 992
break;
case 254: yyVal = case254_line1004(yyVal, yyVals, yyTop); // line 1004
break;
case 255: yyVal = case255_line1013(yyVal, yyVals, yyTop); // line 1013
break;
case 256: yyVal = case256_line1023(yyVal, yyVals, yyTop); // line 1023
break;
case 265: yyVal = case265_line1035(yyVal, yyVals, yyTop); // line 1035
break;
case 266: yyVal = case266_line1038(yyVal, yyVals, yyTop); // line 1038
break;
case 267: yyVal = case267_line1041(yyVal, yyVals, yyTop); // line 1041
break;
case 268: yyVal = case268_line1043(yyVal, yyVals, yyTop); // line 1043
break;
case 269: yyVal = case269_line1047(yyVal, yyVals, yyTop); // line 1047
break;
case 270: yyVal = case270_line1054(yyVal, yyVals, yyTop); // line 1054
break;
case 271: yyVal = case271_line1057(yyVal, yyVals, yyTop); // line 1057
break;
case 272: yyVal = case272_line1060(yyVal, yyVals, yyTop); // line 1060
break;
case 273: yyVal = case273_line1069(yyVal, yyVals, yyTop); // line 1069
break;
case 274: yyVal = case274_line1072(yyVal, yyVals, yyTop); // line 1072
break;
case 275: yyVal = case275_line1075(yyVal, yyVals, yyTop); // line 1075
break;
case 276: yyVal = case276_line1078(yyVal, yyVals, yyTop); // line 1078
break;
case 277: yyVal = case277_line1081(yyVal, yyVals, yyTop); // line 1081
break;
case 278: yyVal = case278_line1084(yyVal, yyVals, yyTop); // line 1084
break;
case 279: yyVal = case279_line1087(yyVal, yyVals, yyTop); // line 1087
break;
case 280: yyVal = case280_line1090(yyVal, yyVals, yyTop); // line 1090
break;
case 281: yyVal = case281_line1093(yyVal, yyVals, yyTop); // line 1093
break;
case 283: yyVal = case283_line1097(yyVal, yyVals, yyTop); // line 1097
break;
case 284: yyVal = case284_line1105(yyVal, yyVals, yyTop); // line 1105
break;
case 285: yyVal = case285_line1108(yyVal, yyVals, yyTop); // line 1108
break;
case 286: yyVal = case286_line1111(yyVal, yyVals, yyTop); // line 1111
break;
case 287: yyVal = case287_line1114(yyVal, yyVals, yyTop); // line 1114
break;
case 288: yyVal = case288_line1116(yyVal, yyVals, yyTop); // line 1116
break;
case 289: yyVal = case289_line1118(yyVal, yyVals, yyTop); // line 1118
break;
case 290: yyVal = case290_line1122(yyVal, yyVals, yyTop); // line 1122
break;
case 291: yyVal = case291_line1124(yyVal, yyVals, yyTop); // line 1124
break;
case 292: yyVal = case292_line1126(yyVal, yyVals, yyTop); // line 1126
break;
case 293: yyVal = case293_line1130(yyVal, yyVals, yyTop); // line 1130
break;
case 294: yyVal = case294_line1133(yyVal, yyVals, yyTop); // line 1133
break;
case 295: yyVal = case295_line1136(yyVal, yyVals, yyTop); // line 1136
break;
case 296: yyVal = case296_line1138(yyVal, yyVals, yyTop); // line 1138
break;
case 297: yyVal = case297_line1140(yyVal, yyVals, yyTop); // line 1140
break;
case 298: yyVal = case298_line1144(yyVal, yyVals, yyTop); // line 1144
break;
case 299: yyVal = case299_line1149(yyVal, yyVals, yyTop); // line 1149
break;
case 300: yyVal = case300_line1155(yyVal, yyVals, yyTop); // line 1155
break;
case 301: yyVal = case301_line1158(yyVal, yyVals, yyTop); // line 1158
break;
case 302: yyVal = case302_line1162(yyVal, yyVals, yyTop); // line 1162
break;
case 303: yyVal = case303_line1168(yyVal, yyVals, yyTop); // line 1168
break;
case 304: yyVal = case304_line1173(yyVal, yyVals, yyTop); // line 1173
break;
case 305: yyVal = case305_line1179(yyVal, yyVals, yyTop); // line 1179
break;
case 306: yyVal = case306_line1182(yyVal, yyVals, yyTop); // line 1182
break;
case 307: yyVal = case307_line1190(yyVal, yyVals, yyTop); // line 1190
break;
case 308: yyVal = case308_line1192(yyVal, yyVals, yyTop); // line 1192
break;
case 309: yyVal = case309_line1196(yyVal, yyVals, yyTop); // line 1196
break;
case 310: yyVal = case310_line1204(yyVal, yyVals, yyTop); // line 1204
break;
case 311: yyVal = case311_line1207(yyVal, yyVals, yyTop); // line 1207
break;
case 312: yyVal = case312_line1210(yyVal, yyVals, yyTop); // line 1210
break;
case 313: yyVal = case313_line1213(yyVal, yyVals, yyTop); // line 1213
break;
case 314: yyVal = case314_line1217(yyVal, yyVals, yyTop); // line 1217
break;
case 321: yyVal = case321_line1231(yyVal, yyVals, yyTop); // line 1231
break;
case 323: yyVal = case323_line1236(yyVal, yyVals, yyTop); // line 1236
break;
case 325: yyVal = case325_line1241(yyVal, yyVals, yyTop); // line 1241
break;
case 326: yyVal = case326_line1244(yyVal, yyVals, yyTop); // line 1244
break;
case 327: yyVal = case327_line1247(yyVal, yyVals, yyTop); // line 1247
break;
case 328: yyVal = case328_line1251(yyVal, yyVals, yyTop); // line 1251
break;
case 329: yyVal = case329_line1254(yyVal, yyVals, yyTop); // line 1254
break;
case 330: yyVal = case330_line1258(yyVal, yyVals, yyTop); // line 1258
break;
case 331: yyVal = case331_line1261(yyVal, yyVals, yyTop); // line 1261
break;
case 332: yyVal = case332_line1264(yyVal, yyVals, yyTop); // line 1264
break;
case 333: yyVal = case333_line1267(yyVal, yyVals, yyTop); // line 1267
break;
case 334: yyVal = case334_line1270(yyVal, yyVals, yyTop); // line 1270
break;
case 335: yyVal = case335_line1273(yyVal, yyVals, yyTop); // line 1273
break;
case 336: yyVal = case336_line1276(yyVal, yyVals, yyTop); // line 1276
break;
case 337: yyVal = case337_line1279(yyVal, yyVals, yyTop); // line 1279
break;
case 338: yyVal = case338_line1282(yyVal, yyVals, yyTop); // line 1282
break;
case 339: yyVal = case339_line1286(yyVal, yyVals, yyTop); // line 1286
break;
case 340: yyVal = case340_line1289(yyVal, yyVals, yyTop); // line 1289
break;
case 341: yyVal = case341_line1292(yyVal, yyVals, yyTop); // line 1292
break;
case 342: yyVal = case342_line1295(yyVal, yyVals, yyTop); // line 1295
break;
case 343: yyVal = case343_line1298(yyVal, yyVals, yyTop); // line 1298
break;
case 344: yyVal = case344_line1301(yyVal, yyVals, yyTop); // line 1301
break;
case 345: yyVal = case345_line1305(yyVal, yyVals, yyTop); // line 1305
break;
case 346: yyVal = case346_line1308(yyVal, yyVals, yyTop); // line 1308
break;
case 347: yyVal = case347_line1311(yyVal, yyVals, yyTop); // line 1311
break;
case 348: yyVal = case348_line1314(yyVal, yyVals, yyTop); // line 1314
break;
case 349: yyVal = case349_line1317(yyVal, yyVals, yyTop); // line 1317
break;
case 350: yyVal = case350_line1320(yyVal, yyVals, yyTop); // line 1320
break;
case 351: yyVal = case351_line1323(yyVal, yyVals, yyTop); // line 1323
break;
case 352: yyVal = case352_line1326(yyVal, yyVals, yyTop); // line 1326
break;
case 353: yyVal = case353_line1329(yyVal, yyVals, yyTop); // line 1329
break;
case 354: yyVal = case354_line1333(yyVal, yyVals, yyTop); // line 1333
break;
case 355: yyVal = case355_line1337(yyVal, yyVals, yyTop); // line 1337
break;
case 356: yyVal = case356_line1342(yyVal, yyVals, yyTop); // line 1342
break;
case 357: yyVal = case357_line1345(yyVal, yyVals, yyTop); // line 1345
break;
case 358: yyVal = case358_line1348(yyVal, yyVals, yyTop); // line 1348
break;
case 360: yyVal = case360_line1354(yyVal, yyVals, yyTop); // line 1354
break;
case 361: yyVal = case361_line1359(yyVal, yyVals, yyTop); // line 1359
break;
case 362: yyVal = case362_line1362(yyVal, yyVals, yyTop); // line 1362
break;
case 363: yyVal = case363_line1366(yyVal, yyVals, yyTop); // line 1366
break;
case 364: yyVal = case364_line1369(yyVal, yyVals, yyTop); // line 1369
break;
case 365: yyVal = case365_line1373(yyVal, yyVals, yyTop); // line 1373
break;
case 366: yyVal = case366_line1377(yyVal, yyVals, yyTop); // line 1377
break;
case 367: yyVal = case367_line1383(yyVal, yyVals, yyTop); // line 1383
break;
case 368: yyVal = case368_line1387(yyVal, yyVals, yyTop); // line 1387
break;
case 369: yyVal = case369_line1392(yyVal, yyVals, yyTop); // line 1392
break;
case 370: yyVal = case370_line1395(yyVal, yyVals, yyTop); // line 1395
break;
case 371: yyVal = case371_line1399(yyVal, yyVals, yyTop); // line 1399
break;
case 372: yyVal = case372_line1401(yyVal, yyVals, yyTop); // line 1401
break;
case 373: yyVal = case373_line1406(yyVal, yyVals, yyTop); // line 1406
break;
case 374: yyVal = case374_line1417(yyVal, yyVals, yyTop); // line 1417
break;
case 375: yyVal = case375_line1420(yyVal, yyVals, yyTop); // line 1420
break;
case 376: yyVal = case376_line1424(yyVal, yyVals, yyTop); // line 1424
break;
case 377: yyVal = case377_line1427(yyVal, yyVals, yyTop); // line 1427
break;
case 378: yyVal = case378_line1430(yyVal, yyVals, yyTop); // line 1430
break;
case 379: yyVal = case379_line1433(yyVal, yyVals, yyTop); // line 1433
break;
case 380: yyVal = case380_line1436(yyVal, yyVals, yyTop); // line 1436
break;
case 381: yyVal = case381_line1439(yyVal, yyVals, yyTop); // line 1439
break;
case 382: yyVal = case382_line1442(yyVal, yyVals, yyTop); // line 1442
break;
case 383: yyVal = case383_line1445(yyVal, yyVals, yyTop); // line 1445
break;
case 384: yyVal = case384_line1448(yyVal, yyVals, yyTop); // line 1448
break;
case 385: yyVal = case385_line1456(yyVal, yyVals, yyTop); // line 1456
break;
case 386: yyVal = case386_line1458(yyVal, yyVals, yyTop); // line 1458
break;
case 387: yyVal = case387_line1462(yyVal, yyVals, yyTop); // line 1462
break;
case 388: yyVal = case388_line1464(yyVal, yyVals, yyTop); // line 1464
break;
case 389: yyVal = case389_line1470(yyVal, yyVals, yyTop); // line 1470
break;
case 392: yyVal = case392_line1476(yyVal, yyVals, yyTop); // line 1476
break;
case 393: yyVal = case393_line1489(yyVal, yyVals, yyTop); // line 1489
break;
case 394: yyVal = case394_line1493(yyVal, yyVals, yyTop); // line 1493
break;
case 395: yyVal = case395_line1496(yyVal, yyVals, yyTop); // line 1496
break;
case 397: yyVal = case397_line1502(yyVal, yyVals, yyTop); // line 1502
break;
case 399: yyVal = case399_line1507(yyVal, yyVals, yyTop); // line 1507
break;
case 402: yyVal = case402_line1513(yyVal, yyVals, yyTop); // line 1513
break;
case 404: yyVal = case404_line1519(yyVal, yyVals, yyTop); // line 1519
break;
case 405: yyVal = case405_line1532(yyVal, yyVals, yyTop); // line 1532
break;
case 406: yyVal = case406_line1535(yyVal, yyVals, yyTop); // line 1535
break;
case 407: yyVal = case407_line1538(yyVal, yyVals, yyTop); // line 1538
break;
case 408: yyVal = case408_line1542(yyVal, yyVals, yyTop); // line 1542
break;
case 409: yyVal = case409_line1557(yyVal, yyVals, yyTop); // line 1557
break;
case 410: yyVal = case410_line1573(yyVal, yyVals, yyTop); // line 1573
break;
case 411: yyVal = case411_line1588(yyVal, yyVals, yyTop); // line 1588
break;
case 412: yyVal = case412_line1591(yyVal, yyVals, yyTop); // line 1591
break;
case 413: yyVal = case413_line1595(yyVal, yyVals, yyTop); // line 1595
break;
case 414: yyVal = case414_line1598(yyVal, yyVals, yyTop); // line 1598
break;
case 416: yyVal = case416_line1603(yyVal, yyVals, yyTop); // line 1603
break;
case 417: yyVal = case417_line1607(yyVal, yyVals, yyTop); // line 1607
break;
case 418: yyVal = case418_line1610(yyVal, yyVals, yyTop); // line 1610
break;
case 419: yyVal = case419_line1615(yyVal, yyVals, yyTop); // line 1615
break;
case 420: yyVal = case420_line1618(yyVal, yyVals, yyTop); // line 1618
break;
case 421: yyVal = case421_line1622(yyVal, yyVals, yyTop); // line 1622
break;
case 422: yyVal = case422_line1625(yyVal, yyVals, yyTop); // line 1625
break;
case 423: yyVal = case423_line1629(yyVal, yyVals, yyTop); // line 1629
break;
case 424: yyVal = case424_line1632(yyVal, yyVals, yyTop); // line 1632
break;
case 425: yyVal = case425_line1636(yyVal, yyVals, yyTop); // line 1636
break;
case 426: yyVal = case426_line1639(yyVal, yyVals, yyTop); // line 1639
break;
case 427: yyVal = case427_line1643(yyVal, yyVals, yyTop); // line 1643
break;
case 428: yyVal = case428_line1647(yyVal, yyVals, yyTop); // line 1647
break;
case 429: yyVal = case429_line1651(yyVal, yyVals, yyTop); // line 1651
break;
case 430: yyVal = case430_line1657(yyVal, yyVals, yyTop); // line 1657
break;
case 431: yyVal = case431_line1660(yyVal, yyVals, yyTop); // line 1660
break;
case 432: yyVal = case432_line1663(yyVal, yyVals, yyTop); // line 1663
break;
case 434: yyVal = case434_line1669(yyVal, yyVals, yyTop); // line 1669
break;
case 439: yyVal = case439_line1678(yyVal, yyVals, yyTop); // line 1678
break;
case 440: yyVal = case440_line1702(yyVal, yyVals, yyTop); // line 1702
break;
case 441: yyVal = case441_line1705(yyVal, yyVals, yyTop); // line 1705
break;
case 442: yyVal = case442_line1708(yyVal, yyVals, yyTop); // line 1708
break;
case 443: yyVal = case443_line1711(yyVal, yyVals, yyTop); // line 1711
break;
case 449: yyVal = case449_line1716(yyVal, yyVals, yyTop); // line 1716
break;
case 450: yyVal = case450_line1719(yyVal, yyVals, yyTop); // line 1719
break;
case 451: yyVal = case451_line1722(yyVal, yyVals, yyTop); // line 1722
break;
case 452: yyVal = case452_line1725(yyVal, yyVals, yyTop); // line 1725
break;
case 453: yyVal = case453_line1728(yyVal, yyVals, yyTop); // line 1728
break;
case 454: yyVal = case454_line1731(yyVal, yyVals, yyTop); // line 1731
break;
case 455: yyVal = case455_line1734(yyVal, yyVals, yyTop); // line 1734
break;
case 456: yyVal = case456_line1738(yyVal, yyVals, yyTop); // line 1738
break;
case 457: yyVal = case457_line1742(yyVal, yyVals, yyTop); // line 1742
break;
case 458: yyVal = case458_line1746(yyVal, yyVals, yyTop); // line 1746
break;
case 459: yyVal = case459_line1749(yyVal, yyVals, yyTop); // line 1749
break;
case 460: yyVal = case460_line1753(yyVal, yyVals, yyTop); // line 1753
break;
case 461: yyVal = case461_line1756(yyVal, yyVals, yyTop); // line 1756
break;
case 462: yyVal = case462_line1758(yyVal, yyVals, yyTop); // line 1758
break;
case 463: yyVal = case463_line1761(yyVal, yyVals, yyTop); // line 1761
break;
case 464: yyVal = case464_line1767(yyVal, yyVals, yyTop); // line 1767
break;
case 465: yyVal = case465_line1772(yyVal, yyVals, yyTop); // line 1772
break;
case 466: yyVal = case466_line1776(yyVal, yyVals, yyTop); // line 1776
break;
case 467: yyVal = case467_line1779(yyVal, yyVals, yyTop); // line 1779
break;
case 468: yyVal = case468_line1782(yyVal, yyVals, yyTop); // line 1782
break;
case 469: yyVal = case469_line1785(yyVal, yyVals, yyTop); // line 1785
break;
case 470: yyVal = case470_line1788(yyVal, yyVals, yyTop); // line 1788
break;
case 471: yyVal = case471_line1791(yyVal, yyVals, yyTop); // line 1791
break;
case 472: yyVal = case472_line1794(yyVal, yyVals, yyTop); // line 1794
break;
case 473: yyVal = case473_line1797(yyVal, yyVals, yyTop); // line 1797
break;
case 474: yyVal = case474_line1800(yyVal, yyVals, yyTop); // line 1800
break;
case 475: yyVal = case475_line1803(yyVal, yyVals, yyTop); // line 1803
break;
case 476: yyVal = case476_line1806(yyVal, yyVals, yyTop); // line 1806
break;
case 477: yyVal = case477_line1809(yyVal, yyVals, yyTop); // line 1809
break;
case 478: yyVal = case478_line1812(yyVal, yyVals, yyTop); // line 1812
break;
case 479: yyVal = case479_line1815(yyVal, yyVals, yyTop); // line 1815
break;
case 480: yyVal = case480_line1818(yyVal, yyVals, yyTop); // line 1818
break;
case 481: yyVal = case481_line1822(yyVal, yyVals, yyTop); // line 1822
break;
case 482: yyVal = case482_line1825(yyVal, yyVals, yyTop); // line 1825
break;
case 483: yyVal = case483_line1828(yyVal, yyVals, yyTop); // line 1828
break;
case 484: yyVal = case484_line1831(yyVal, yyVals, yyTop); // line 1831
break;
case 486: yyVal = case486_line1837(yyVal, yyVals, yyTop); // line 1837
break;
case 487: yyVal = case487_line1847(yyVal, yyVals, yyTop); // line 1847
break;
case 488: yyVal = case488_line1854(yyVal, yyVals, yyTop); // line 1854
break;
case 489: yyVal = case489_line1869(yyVal, yyVals, yyTop); // line 1869
break;
case 490: yyVal = case490_line1872(yyVal, yyVals, yyTop); // line 1872
break;
case 491: yyVal = case491_line1877(yyVal, yyVals, yyTop); // line 1877
break;
case 492: yyVal = case492_line1886(yyVal, yyVals, yyTop); // line 1886
break;
case 493: yyVal = case493_line1895(yyVal, yyVals, yyTop); // line 1895
break;
case 494: yyVal = case494_line1898(yyVal, yyVals, yyTop); // line 1898
break;
case 495: yyVal = case495_line1902(yyVal, yyVals, yyTop); // line 1902
break;
case 496: yyVal = case496_line1905(yyVal, yyVals, yyTop); // line 1905
break;
case 499: yyVal = case499_line1911(yyVal, yyVals, yyTop); // line 1911
break;
case 500: yyVal = case500_line1918(yyVal, yyVals, yyTop); // line 1918
break;
case 503: yyVal = case503_line1925(yyVal, yyVals, yyTop); // line 1925
break;
case 504: yyVal = case504_line1935(yyVal, yyVals, yyTop); // line 1935
break;
case 505: yyVal = case505_line1938(yyVal, yyVals, yyTop); // line 1938
break;
case 506: yyVal = case506_line1942(yyVal, yyVals, yyTop); // line 1942
break;
case 507: yyVal = case507_line1948(yyVal, yyVals, yyTop); // line 1948
break;
case 508: yyVal = case508_line1950(yyVal, yyVals, yyTop); // line 1950
break;
case 509: yyVal = case509_line1960(yyVal, yyVals, yyTop); // line 1960
break;
case 510: yyVal = case510_line1963(yyVal, yyVals, yyTop); // line 1963
break;
case 512: yyVal = case512_line1968(yyVal, yyVals, yyTop); // line 1968
break;
case 513: yyVal = case513_line1972(yyVal, yyVals, yyTop); // line 1972
break;
case 514: yyVal = case514_line1982(yyVal, yyVals, yyTop); // line 1982
break;
case 531: yyVal = case531_line1992(yyVal, yyVals, yyTop); // line 1992
break;
case 532: yyVal = case532_line1995(yyVal, yyVals, yyTop); // line 1995
break;
case 536: yyVal = case536_line2000(yyVal, yyVals, yyTop); // line 2000
break;
case 539: yyVal = case539_line2006(yyVal, yyVals, yyTop); // line 2006
break;
case 540: yyVal = case540_line2010(yyVal, yyVals, yyTop); // line 2010
break;
case 541: yyVal = case541_line2014(yyVal, yyVals, yyTop); // line 2014
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

public Object case374_line1417(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case371_line1399(Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
    return yyVal;
}
public Object case366_line1377(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new LambdaNode(getPosition(((ArgsNode)yyVals[-1+yyTop])), ((ArgsNode)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
                    lexer.setLeftParenBegin(((Integer)yyVals[-2+yyTop]));
    return yyVal;
}
public Object case305_line1179(Object yyVal, Object[] yyVals, int yyTop) {
                    support.setInDef(true);
                    support.pushLocalScope();
    return yyVal;
}
public Object case288_line1116(Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.getConditionState().end();
    return yyVal;
}
public Object case61_line521(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case306_line1182(Object yyVal, Object[] yyVals, int yyTop) {
                    /* TODO: We should use implicit nil for body, but problem (punt til later)*/
                    Node body = ((Node)yyVals[-1+yyTop]); /*$5 == null ? NilImplicitNode.NIL : $5;*/

                    yyVal = new DefnNode(getPosition(((Token)yyVals[-5+yyTop])), new ArgumentNode(((Token)yyVals[-4+yyTop]).getPosition(), (String) ((Token)yyVals[-4+yyTop]).getValue()), ((ArgsNode)yyVals[-2+yyTop]), support.getCurrentScope(), body);
                    support.popCurrentScope();
                    support.setInDef(false);
    return yyVal;
}
public Object case321_line1231(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IfNode(getPosition(((Token)yyVals[-4+yyTop])), support.getConditionNode(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case207_line842(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "|", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case481_line1822(Object yyVal, Object[] yyVals, int yyTop) {
                    yyerror("formal argument cannot be a constant");
    return yyVal;
}
public Object case456_line1738(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.gettable(((Token)yyVals[0+yyTop]));
    return yyVal;
}
public Object case449_line1716(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("nil", Tokens.kNIL, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case16_line351(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IfNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), null, ((Node)yyVals[-2+yyTop]));
    return yyVal;
}
public Object case196_line801(Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[-2+yyTop]));
                    support.checkExpression(((Node)yyVals[0+yyTop]));

                    boolean isLiteral = ((Node)yyVals[-2+yyTop]) instanceof FixnumNode && ((Node)yyVals[0+yyTop]) instanceof FixnumNode;
                    yyVal = new DotNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), true, isLiteral);
    return yyVal;
}
public Object case299_line1149(Object yyVal, Object[] yyVals, int yyTop) {
                    Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);

                    yyVal = new ClassNode(getPosition(((Token)yyVals[-5+yyTop])), ((Colon3Node)yyVals[-4+yyTop]), support.getCurrentScope(), body, ((Node)yyVals[-3+yyTop]));
                    support.popCurrentScope();
    return yyVal;
}
public Object case265_line1035(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new FCallNoArgNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case496_line1905(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case422_line1625(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case312_line1210(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new RedoNode(((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case298_line1144(Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("class definition in method body");
                    }
                    support.pushLocalScope();
    return yyVal;
}
public Object case233_line928(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(getPosition(((ListNode)yyVals[-1+yyTop])), new Hash19Node(getPosition(), ((ListNode)yyVals[-1+yyTop])));
    return yyVal;
}
public Object case254_line1004(Object yyVal, Object[] yyVals, int yyTop) {
                    Node node = support.splat_array(((Node)yyVals[-2+yyTop]));

                    if (node != null) {
                        yyVal = support.list_append(node, ((Node)yyVals[0+yyTop]));
                    } else {
                        yyVal = support.arg_append(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    }
    return yyVal;
}
public Object case255_line1013(Object yyVal, Object[] yyVals, int yyTop) {
                    Node node = null;

                    if (((Node)yyVals[0+yyTop]) instanceof ArrayNode &&
                        (node = support.splat_array(((Node)yyVals[-3+yyTop]))) != null) {
                        yyVal = support.list_concat(node, ((Node)yyVals[0+yyTop]));
                    } else {
                        yyVal = support.arg_concat(getPosition(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
                    }
    return yyVal;
}
public Object case532_line1995(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
}
public Object case367_line1383(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ArgsNode)yyVals[-2+yyTop]);
                    ((ISourcePositionHolder)yyVal).setPosition(getPosition(((Token)yyVals[-3+yyTop])));
    return yyVal;
}
public Object case349_line1317(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((ListNode)yyVals[-1+yyTop])), null, ((ListNode)yyVals[-1+yyTop]), null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case22_line382(Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        warnings.warn(ID.END_IN_METHOD, getPosition(((Token)yyVals[-3+yyTop])), "END in method; use at_exit");
                    }
                    yyVal = new PostExeNode(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case99_line669(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_colon2(((Token)yyVals[0+yyTop]).getPosition(), null, (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case222_line893(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<<", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case269_line1047(Object yyVal, Object[] yyVals, int yyTop) {
                    if (((Node)yyVals[-1+yyTop]) != null) {
                        /* compstmt position includes both parens around it*/
                        ((ISourcePositionHolder) ((Node)yyVals[-1+yyTop])).setPosition(getPosition(((Token)yyVals[-2+yyTop])));
                    }
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case499_line1911(Object yyVal, Object[] yyVals, int yyTop) {
                    if (!support.is_local_id(((Token)yyVals[0+yyTop]))) {
                        yyerror("duplicate rest argument name");
                    }
                    support.shadowing_lvar(((Token)yyVals[0+yyTop]));
                    yyVal = new RestArgNode(getPosition(((Token)yyVals[-1+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue(), support.arg_var(((Token)yyVals[0+yyTop])));
    return yyVal;
}
public Object case466_line1776(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((ListNode)yyVals[-5+yyTop])), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case408_line1542(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);

                    ((ISourcePositionHolder)yyVal).setPosition(getPosition(((Token)yyVals[-2+yyTop])));
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
public Object case382_line1442(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case33_line436(Object yyVal, Object[] yyVals, int yyTop) {
                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                    yyVal = ((MultipleAsgn19Node)yyVals[-2+yyTop]);
                    ((MultipleAsgn19Node)yyVals[-2+yyTop]).setPosition(getPosition(((MultipleAsgn19Node)yyVals[-2+yyTop])));
    return yyVal;
}
public Object case495_line1902(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BlockNode(getPosition(((Node)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case432_line1663(Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = new ClassVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case397_line1502(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case357_line1345(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(), null, null, null, null, null);
    return yyVal;
}
public Object case310_line1204(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BreakNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
    return yyVal;
}
public Object case27_line417(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public Object case234_line932(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                    if (yyVal != null) ((Node)yyVal).setPosition(getPosition(((Token)yyVals[-2+yyTop])));
    return yyVal;
}
public Object case268_line1043(Object yyVal, Object[] yyVals, int yyTop) {
                    if (warnings.isVerbose()) warnings.warning(ID.GROUPED_EXPRESSION, getPosition(((Token)yyVals[-3+yyTop])), "(...) interpreted as grouped expression");
                    yyVal = ((Node)yyVals[-2+yyTop]);
    return yyVal;
}
public Object case14_line345(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case504_line1935(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((BlockArgNode)yyVals[0+yyTop]);
    return yyVal;
}
public Object case426_line1639(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = lexer.getStrTerm();
                    lexer.setStrTerm(null);
                    lexer.setState(LexState.EXPR_BEG);
    return yyVal;
}
public Object case417_line1607(Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = new ZArrayNode(getPosition(((Token)yyVals[-2+yyTop])));
    return yyVal;
}
public Object case348_line1314(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((ListNode)yyVals[-5+yyTop])), null, ((ListNode)yyVals[-5+yyTop]), ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case278_line1084(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new DefinedNode(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case18_line361(Object yyVal, Object[] yyVals, int yyTop) {
                    if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = new UntilNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                    } else {
                        yyVal = new UntilNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                    }
    return yyVal;
}
public Object case492_line1886(Object yyVal, Object[] yyVals, int yyTop) {
                    if (!support.is_local_id(((Token)yyVals[-2+yyTop]))) {
                        yyerror("formal argument must be local variable");
                    }
                    support.shadowing_lvar(((Token)yyVals[-2+yyTop]));
                    support.arg_var(((Token)yyVals[-2+yyTop]));
                    yyVal = new OptArgNode(getPosition(((Token)yyVals[-2+yyTop])), support.assignable(((Token)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
    return yyVal;
}
public Object case476_line1806(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((ListNode)yyVals[-3+yyTop])), null, ((ListNode)yyVals[-3+yyTop]), null, ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case427_line1643(Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setStrTerm(((StrTerm)yyVals[-1+yyTop]));
                    yyVal = new EvStrNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case364_line1369(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
}
public Object case28_line420(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public Object case220_line887(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new NotNode(getPosition(((Token)yyVals[-1+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
    return yyVal;
}
public Object case490_line1872(Object yyVal, Object[] yyVals, int yyTop) {
                    ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                    yyVal = ((ListNode)yyVals[-2+yyTop]);
    return yyVal;
}
public Object case471_line1791(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((ListNode)yyVals[-5+yyTop])), ((ListNode)yyVals[-5+yyTop]), null, ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case59_line515(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_yield(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case244_line958(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Long(lexer.getCmdArgumentState().begin());
    return yyVal;
}
public Object case252_line983(Object yyVal, Object[] yyVals, int yyTop) {
                    Node node = support.splat_array(((Node)yyVals[-2+yyTop]));

                    if (node != null) {
                        yyVal = support.list_append(node, ((Node)yyVals[0+yyTop]));
                    } else {
                        yyVal = support.arg_append(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    }
    return yyVal;
}
public Object case256_line1023(Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = support.newSplatNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));  
    return yyVal;
}
public Object case45_line469(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BreakNode(getPosition(((Token)yyVals[-1+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
    return yyVal;
}
public Object case245_line960(Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case218_line875(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                  /* ENEBO
                        $$ = match_op($1, $3);
                        if (nd_type($1) == NODE_LIT && TYPE($1->nd_lit) == T_REGEXP) {
                            $$ = reg_named_capture_assign($1->nd_lit, $$);
                        }
                  */
    return yyVal;
}
public Object case475_line1803(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((ListNode)yyVals[-1+yyTop])), null, ((ListNode)yyVals[-1+yyTop]), null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case350_line1320(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((ListNode)yyVals[-3+yyTop])), null, ((ListNode)yyVals[-3+yyTop]), null, ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case330_line1258(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(getPosition(((ListNode)yyVals[0+yyTop])), ((ListNode)yyVals[0+yyTop]), null, null);
    return yyVal;
}
public Object case193_line788(Object yyVal, Object[] yyVals, int yyTop) {
                    yyerror("constant re-assignment");
    return yyVal;
}
public Object case239_line941(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(getPosition(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case184_line727(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    /* FIXME: Consider fixing node_assign itself rather than single case*/
                    ((Node)yyVal).setPosition(getPosition(((Node)yyVals[-2+yyTop])));
    return yyVal;
}
public Object case246_line965(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BlockPassNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case323_line1236(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case454_line1731(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("__LINE__", Tokens.k__LINE__, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case412_line1591(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case300_line1155(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Boolean(support.isInDef());
                    support.setInDef(false);
    return yyVal;
}
public Object case46_line472(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new NextNode(getPosition(((Token)yyVals[-1+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
    return yyVal;
}
public Object case76_line571(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(((Node)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case486_line1837(Object yyVal, Object[] yyVals, int yyTop) {
    /* FIXME: Resolve what the hell is going on*/
    /*                    if (support.is_local_id($1)) {
                        yyerror("formal argument must be local variable");
                        }*/
                     
                    support.shadowing_lvar(((Token)yyVals[0+yyTop]));
                    yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
}
public Object case392_line1476(Object yyVal, Object[] yyVals, int yyTop) {
                    Node node;
                    if (((Node)yyVals[-3+yyTop]) != null) {
                        node = support.appendToBlock(support.node_assign(((Node)yyVals[-3+yyTop]), new GlobalVarNode(getPosition(((Token)yyVals[-5+yyTop])), "$!")), ((Node)yyVals[-1+yyTop]));
                        if (((Node)yyVals[-1+yyTop]) != null) {
                            node.setPosition(support.unwrapNewlineNode(((Node)yyVals[-1+yyTop])).getPosition());
                        }
                    } else {
                        node = ((Node)yyVals[-1+yyTop]);
                    }
                    Node body = node == null ? NilImplicitNode.NIL : node;
                    yyVal = new RescueBodyNode(getPosition(((Token)yyVals[-5+yyTop])), ((Node)yyVals[-4+yyTop]), body, ((RescueBodyNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case335_line1273(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(getPosition(((Token)yyVals[-1+yyTop])), null, support.assignable(((Token)yyVals[0+yyTop]), null), null);
    return yyVal;
}
public Object case194_line791(Object yyVal, Object[] yyVals, int yyTop) {
                    support.backrefAssignError(((Node)yyVals[-2+yyTop]));
    return yyVal;
}
public Object case57_line509(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case106_line688(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
}
public Object case88_line623(Object yyVal, Object[] yyVals, int yyTop) {
                      /* if (!($$ = assignable($1, 0))) $$ = NEW_BEGIN(0);*/
                    yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
    return yyVal;
}
public Object case291_line1124(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.getConditionState().end();
    return yyVal;
}
public Object case539_line2006(Object yyVal, Object[] yyVals, int yyTop) {
                      yyerrok();
    return yyVal;
}
public Object case493_line1895(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BlockNode(getPosition(((Node)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case383_line1445(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ZSuperNode(((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case216_line869(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "===", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case72_line558(Object yyVal, Object[] yyVals, int yyTop) {
                      yyVal = new MultipleAsgn19Node(getPosition(((Token)yyVals[0+yyTop])), null, new StarNode(getPosition()), null);
    return yyVal;
}
public Object case452_line1725(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("false", Tokens.kFALSE, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case395_line1496(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.splat_array(((Node)yyVals[0+yyTop]));
                    if (yyVal == null) yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case376_line1424(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_fcall(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case373_line1406(Object yyVal, Object[] yyVals, int yyTop) {
                    /* Workaround for JRUBY-2326 (MRI does not enter this production for some reason)*/
                    if (((Node)yyVals[-1+yyTop]) instanceof YieldNode) {
                        throw new SyntaxException(PID.BLOCK_GIVEN_TO_YIELD, getPosition(((Node)yyVals[-1+yyTop])), lexer.getCurrentLine(), "block given to yield");
                    }
                    if (((BlockAcceptingNode)yyVals[-1+yyTop]).getIterNode() instanceof BlockPassNode) {
                        throw new SyntaxException(PID.BLOCK_ARG_AND_BLOCK_GIVEN, getPosition(((Node)yyVals[-1+yyTop])), lexer.getCurrentLine(), "Both block arg and actual block given.");
                    }
                    yyVal = ((BlockAcceptingNode)yyVals[-1+yyTop]).setIterNode(((IterNode)yyVals[0+yyTop]));
                    ((Node)yyVal).setPosition(getPosition(((Node)yyVals[-1+yyTop])));
    return yyVal;
}
public Object case107_line691(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
}
public Object case20_line372(Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("BEGIN in method");
                    }
                    support.pushLocalScope();
    return yyVal;
}
public Object case188_line772(Object yyVal, Object[] yyVals, int yyTop) {
  /* FIXME: arg_concat missing for opt_call_args*/
                    yyVal = support.new_opElementAsgnNode(getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case295_line1136(Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.getConditionState().begin();
    return yyVal;
}
public Object case505_line1938(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
}
public Object case399_line1507(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case284_line1105(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((LambdaNode)yyVals[0+yyTop]);
    return yyVal;
}
public Object case6_line321(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newline_node(((Node)yyVals[0+yyTop]), getPosition(((Node)yyVals[0+yyTop])));
    return yyVal;
}
public Object case200_line817(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "/", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case389_line1470(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newWhenNode(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case209_line848(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "&", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case202_line823(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case394_line1493(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case362_line1362(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
}
public Object case345_line1305(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((ListNode)yyVals[-5+yyTop])), ((ListNode)yyVals[-5+yyTop]), null, ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case94_line648(Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = getPosition(((Token)yyVals[-1+yyTop]));

                    yyVal = new ConstDeclNode(position, null, support.new_colon3(position, (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
    return yyVal;
}
public Object case39_line454(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new NotNode(getPosition(((Token)yyVals[-1+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
    return yyVal;
}
public Object case219_line884(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new NotNode(getPosition(((Node)yyVals[-2+yyTop])), support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
    return yyVal;
}
public Object case9_line331(Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setState(LexState.EXPR_FNAME);
    return yyVal;
}
public Object case83_line595(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case457_line1742(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
    return yyVal;
}
public Object case414_line1598(Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]) instanceof EvStrNode ? new DStrNode(getPosition(((ListNode)yyVals[-2+yyTop]))).add(((Node)yyVals[-1+yyTop])) : ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case360_line1354(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
}
public Object case53_line497(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_fcall(((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case31_line429(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case487_line1847(Object yyVal, Object[] yyVals, int yyTop) {
                    support.arg_var(((Token)yyVals[0+yyTop]));
                    yyVal = new ArgumentNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
  /*
                    $$ = new ArgAuxiliaryNode($1.getPosition(), (String) $1.getValue(), 1);
  */
    return yyVal;
}
public Object case473_line1797(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((ListNode)yyVals[-3+yyTop])), null, ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case388_line1464(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IterNode(getPosition(((Token)yyVals[-4+yyTop])), ((ArgsNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), support.getCurrentScope());
                    ((ISourcePositionHolder)yyVals[-5+yyTop]).setPosition(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])));
                    support.popCurrentScope();
    return yyVal;
}
public Object case344_line1301(Object yyVal, Object[] yyVals, int yyTop) {
                    RestArgNode rest = new UnnamedRestArgNode(((ListNode)yyVals[-1+yyTop]).getPosition(), support.getCurrentScope().addVariable("*"));
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, rest, null, null);
    return yyVal;
}
public Object case211_line854(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case221_line890(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "~");
    return yyVal;
}
public Object case232_line925(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.arg_append(((Node)yyVals[-3+yyTop]), new Hash19Node(getPosition(), ((ListNode)yyVals[-1+yyTop])));
    return yyVal;
}
public Object case510_line1963(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case12_line339(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new VAliasNode(getPosition(((Token)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), "$" + ((BackRefNode)yyVals[0+yyTop]).getType());
    return yyVal;
}
public Object case98_line666(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_colon3(getPosition(((Token)yyVals[-1+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case225_line902(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newOrNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case274_line1072(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ReturnNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
    return yyVal;
}
public Object case231_line922(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case32_line432(Object yyVal, Object[] yyVals, int yyTop) {
                    ((MultipleAsgn19Node)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                    yyVal = ((MultipleAsgn19Node)yyVals[-2+yyTop]);
    return yyVal;
}
public Object case540_line2010(Object yyVal, Object[] yyVals, int yyTop) {
                      yyVal = null;
    return yyVal;
}
public Object case464_line1767(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ArgsNode)yyVals[-1+yyTop]);
                    ((ISourcePositionHolder)yyVal).setPosition(getPosition(((Token)yyVals[-2+yyTop])));
                    lexer.setState(LexState.EXPR_BEG);
    return yyVal;
}
public Object case307_line1190(Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setState(LexState.EXPR_FNAME);
    return yyVal;
}
public Object case11_line336(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new VAliasNode(getPosition(((Token)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case26_line413(Object yyVal, Object[] yyVals, int yyTop) {
  /* FIXME: arg_concat logic missing for opt_call_args*/
                    yyVal = support.new_opElementAsgnNode(getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case418_line1610(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                    ((ISourcePositionHolder)yyVal).setPosition(getPosition(((Token)yyVals[-2+yyTop])));
    return yyVal;
}
public Object case393_line1489(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null; 
    return yyVal;
}
public Object case386_line1458(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IterNode(getPosition(((Token)yyVals[-4+yyTop])), ((ArgsNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
    return yyVal;
}
public Object case484_line1831(Object yyVal, Object[] yyVals, int yyTop) {
                    yyerror("formal argument cannot be a class variable");
    return yyVal;
}
public Object case355_line1337(Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.commandStart = true;
                    yyVal = ((ArgsNode)yyVals[0+yyTop]);
    return yyVal;
}
public Object case332_line1264(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(getPosition(((ListNode)yyVals[-5+yyTop])), ((ListNode)yyVals[-5+yyTop]), support.assignable(((Token)yyVals[-2+yyTop]), null), ((ListNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case272_line1060(Object yyVal, Object[] yyVals, int yyTop) {
                    ISourcePosition position = getPosition(((Token)yyVals[-2+yyTop]));
                    if (((Node)yyVals[-1+yyTop]) == null) {
                        yyVal = new ZArrayNode(position); /* zero length array */
                    } else {
                        yyVal = ((Node)yyVals[-1+yyTop]);
                        ((ISourcePositionHolder)yyVal).setPosition(position);
                    }
    return yyVal;
}
public Object case271_line1057(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_colon3(getPosition(((Token)yyVals[-1+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case365_line1373(Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
                    yyVal = lexer.getLeftParenBegin();
                    lexer.setLeftParenBegin(lexer.incrementParenNest());
    return yyVal;
}
public Object case281_line1093(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new FCallNoArgBlockNode(getPosition(((Token)yyVals[-1+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((IterNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case289_line1118(Object yyVal, Object[] yyVals, int yyTop) {
                    Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);
                    yyVal = new WhileNode(getPosition(((Token)yyVals[-6+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), body);
    return yyVal;
}
public Object case459_line1749(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case430_line1657(Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = new GlobalVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case402_line1513(Object yyVal, Object[] yyVals, int yyTop) {
                    /* FIXME: We may be intern'ing more than once.*/
                    yyVal = new SymbolNode(((Token)yyVals[0+yyTop]).getPosition(), ((String) ((Token)yyVals[0+yyTop]).getValue()).intern());
    return yyVal;
}
public Object case356_line1342(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(), null, null, null, null, null);
    return yyVal;
}
public Object case243_line955(Object yyVal, Object[] yyVals, int yyTop) {
    return yyVal;
}
public Object case185_line732(Object yyVal, Object[] yyVals, int yyTop) {
                    ISourcePosition position = getPosition(((Token)yyVals[-1+yyTop]));
                    Node body = ((Node)yyVals[0+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop]);
                    yyVal = support.node_assign(((Node)yyVals[-4+yyTop]), new RescueNode(position, ((Node)yyVals[-2+yyTop]), new RescueBodyNode(position, null, body, null), null));
    return yyVal;
}
public Object case469_line1785(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((ListNode)yyVals[-5+yyTop])), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), null, ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case425_line1636(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case420_line1618(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case405_line1532(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new StrNode(((Token)yyVals[-1+yyTop]).getPosition(), ByteList.create((String) ((Token)yyVals[0+yyTop]).getValue()));
    return yyVal;
}
public Object case331_line1261(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), support.assignable(((Token)yyVals[0+yyTop]), null), null);
    return yyVal;
}
public Object case273_line1069(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Hash19Node(getPosition(((Token)yyVals[-2+yyTop])), ((ListNode)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case266_line1038(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BeginNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case105_line682(Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.setState(LexState.EXPR_END);
                   yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
}
public Object case267_line1041(Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setState(LexState.EXPR_ENDARG); 
    return yyVal;
}
public Object case92_line636(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case434_line1669(Object yyVal, Object[] yyVals, int yyTop) {
                     lexer.setState(LexState.EXPR_END);
                     yyVal = ((Token)yyVals[0+yyTop]);
                     ((ISourcePositionHolder)yyVal).setPosition(getPosition(((Token)yyVals[-1+yyTop])));
    return yyVal;
}
public Object case313_line1213(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new RetryNode(((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case309_line1196(Object yyVal, Object[] yyVals, int yyTop) {
                    /* TODO: We should use implicit nil for body, but problem (punt til later)*/
                    Node body = ((Node)yyVals[-1+yyTop]); /*$8 == null ? NilImplicitNode.NIL : $8;*/

                    yyVal = new DefsNode(getPosition(((Token)yyVals[-8+yyTop])), ((Node)yyVals[-7+yyTop]), new ArgumentNode(((Token)yyVals[-4+yyTop]).getPosition(), (String) ((Token)yyVals[-4+yyTop]).getValue()), ((ArgsNode)yyVals[-2+yyTop]), support.getCurrentScope(), body);
                    support.popCurrentScope();
                    support.setInSingle(support.getInSingle() - 1);
    return yyVal;
}
public Object case342_line1295(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((ListNode)yyVals[-5+yyTop])), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), null, ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case283_line1097(Object yyVal, Object[] yyVals, int yyTop) {
                    if (((Node)yyVals[-1+yyTop]) != null && 
                          ((BlockAcceptingNode)yyVals[-1+yyTop]).getIterNode() instanceof BlockPassNode) {
                        throw new SyntaxException(PID.BLOCK_ARG_AND_BLOCK_GIVEN, getPosition(((Node)yyVals[-1+yyTop])), lexer.getCurrentLine(), "Both block arg and actual block given.");
                    }
                    yyVal = ((BlockAcceptingNode)yyVals[-1+yyTop]).setIterNode(((IterNode)yyVals[0+yyTop]));
                    ((Node)yyVal).setPosition(getPosition(((Node)yyVals[-1+yyTop])));
    return yyVal;
}
public Object case250_line977(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(getPosition2(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case78_line579(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case85_line601(Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = getPosition(((Node)yyVals[-2+yyTop]));

                    yyVal = new ConstDeclNode(position, null, support.new_colon2(position, ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
    return yyVal;
}
public Object case63_line529(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(getPosition(((Token)yyVals[-2+yyTop])), support.newArrayNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])), null, null);
    return yyVal;
}
public Object case536_line2000(Object yyVal, Object[] yyVals, int yyTop) {
                      yyerrok();
    return yyVal;
}
public Object case512_line1968(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).addAll(((ListNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case500_line1918(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new UnnamedRestArgNode(((Token)yyVals[0+yyTop]).getPosition(), support.getCurrentScope().addVariable("*"));
    return yyVal;
}
public Object case416_line1603(Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case71_line555(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(getPosition(((Token)yyVals[-3+yyTop])), null, ((Node)yyVals[-2+yyTop]), ((ListNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case212_line857(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">=", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case51_line488(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IterNode(getPosition(((Token)yyVals[-4+yyTop])), ((ArgsNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
    return yyVal;
}
public Object case110_line703(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new UndefNode(getPosition(((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case541_line2014(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = null;
    return yyVal;
}
public Object case508_line1950(Object yyVal, Object[] yyVals, int yyTop) {
                    if (((Node)yyVals[-1+yyTop]) == null) {
                        yyerror("can't define single method for ().");
                    } else if (((Node)yyVals[-1+yyTop]) instanceof ILiteralNode) {
                        yyerror("can't define single method for literals.");
                    }
                    support.checkExpression(((Node)yyVals[-1+yyTop]));
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case326_line1244(Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
    return yyVal;
}
public Object case277_line1081(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ZYieldNode(((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case227_line909(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IfNode(getPosition(((Node)yyVals[-5+yyTop])), support.getConditionNode(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case226_line905(Object yyVal, Object[] yyVals, int yyTop) {
                    /* ENEBO: arg surrounded by in_defined set/unset*/
                    yyVal = new DefinedNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case58_line512(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop])); /* .setPosFrom($2);*/
    return yyVal;
}
public Object case276_line1078(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ZYieldNode(getPosition(((Token)yyVals[-2+yyTop])));
    return yyVal;
}
public Object case509_line1960(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ArrayNode(getPosition());
    return yyVal;
}
public Object case507_line1948(Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setState(LexState.EXPR_BEG);
    return yyVal;
}
public Object case482_line1825(Object yyVal, Object[] yyVals, int yyTop) {
                    yyerror("formal argument cannot be an instance variable");
    return yyVal;
}
public Object case478_line1812(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((RestArgNode)yyVals[-3+yyTop])), null, null, ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case439_line1678(Object yyVal, Object[] yyVals, int yyTop) {
                     lexer.setState(LexState.EXPR_END);

                     /* DStrNode: :"some text #{some expression}"*/
                     /* StrNode: :"some text"*/
                     /* EvStrNode :"#{some expression}"*/
                     if (((Node)yyVals[-1+yyTop]) == null) {
                       yyerror("empty symbol literal");
                     }
                     /* FIXME: No node here seems to be an empty string
                        instead of an error
                        if (!($$ = $2)) {
                        $$ = NEW_LIT(ID2SYM(rb_intern("")));
                        }
                     */

                     if (((Node)yyVals[-1+yyTop]) instanceof DStrNode) {
                         yyVal = new DSymbolNode(getPosition(((Token)yyVals[-2+yyTop])), ((DStrNode)yyVals[-1+yyTop]));
                     } else {
                         yyVal = new DSymbolNode(getPosition(((Token)yyVals[-2+yyTop])));
                         ((DSymbolNode)yyVal).add(((Node)yyVals[-1+yyTop]));
                     }
    return yyVal;
}
public Object case372_line1401(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IterNode(getPosition(((Token)yyVals[-4+yyTop])), ((ArgsNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
    return yyVal;
}
public Object case333_line1267(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(getPosition(((ListNode)yyVals[-2+yyTop])), ((ListNode)yyVals[-2+yyTop]), new StarNode(getPosition()), null);
    return yyVal;
}
public Object case44_line466(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ReturnNode(getPosition(((Token)yyVals[-1+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
    return yyVal;
}
public Object case217_line872(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "!=", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case81_line589(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.aryset(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case50_line486(Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
    return yyVal;
}
public Object case253_line992(Object yyVal, Object[] yyVals, int yyTop) {
                    Node node = null;

                    /* FIXME: lose syntactical elements here (and others like this)*/
                    if (((Node)yyVals[0+yyTop]) instanceof ArrayNode &&
                        (node = support.splat_array(((Node)yyVals[-3+yyTop]))) != null) {
                        yyVal = support.list_concat(node, ((Node)yyVals[0+yyTop]));
                    } else {
                        yyVal = support.arg_concat(getPosition(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
                    }
    return yyVal;
}
public Object case65_line537(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(getPosition(((Node)yyVals[-1+yyTop])), ((ListNode)yyVals[-1+yyTop]).add(((Node)yyVals[0+yyTop])), null, null);
    return yyVal;
}
public Object case206_line839(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "-@");
    return yyVal;
}
public Object case37_line448(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newOrNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case460_line1753(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
}
public Object case428_line1647(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = lexer.getStrTerm();
                   lexer.setStrTerm(null);
                   lexer.setState(LexState.EXPR_BEG);
    return yyVal;
}
public Object case375_line1420(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case192_line785(Object yyVal, Object[] yyVals, int yyTop) {
                    yyerror("constant re-assignment");
    return yyVal;
}
public Object case80_line586(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
    return yyVal;
}
public Object case64_line534(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(getPosition(((ListNode)yyVals[0+yyTop])), ((ListNode)yyVals[0+yyTop]), null, null);
    return yyVal;
}
public Object case429_line1651(Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.setStrTerm(((StrTerm)yyVals[-2+yyTop]));

                   yyVal = support.newEvStrNode(getPosition(((Token)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case338_line1282(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(getPosition(((Token)yyVals[-2+yyTop])), null, null, ((ListNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case337_line1279(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(getPosition(((Token)yyVals[0+yyTop])), null, new StarNode(getPosition()), null);
    return yyVal;
}
public Object case4_line313(Object yyVal, Object[] yyVals, int yyTop) {
                    if (((Node)yyVals[-1+yyTop]) instanceof BlockNode) {
                        support.checkUselessStatements(((BlockNode)yyVals[-1+yyTop]));
                    }
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case294_line1133(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newCaseNode(getPosition(((Token)yyVals[-3+yyTop])), null, ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case17_line354(Object yyVal, Object[] yyVals, int yyTop) {
                    if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = new WhileNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                    } else {
                        yyVal = new WhileNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                    }
    return yyVal;
}
public Object case82_line592(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case199_line814(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "*", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case38_line451(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new NotNode(getPosition(((Token)yyVals[-1+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
    return yyVal;
}
public Object case491_line1877(Object yyVal, Object[] yyVals, int yyTop) {
                    if (!support.is_local_id(((Token)yyVals[-2+yyTop]))) {
                        yyerror("formal argument must be local variable");
                    }
                    support.shadowing_lvar(((Token)yyVals[-2+yyTop]));
                    support.arg_var(((Token)yyVals[-2+yyTop]));
                    yyVal = new OptArgNode(getPosition(((Token)yyVals[-2+yyTop])), support.assignable(((Token)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
    return yyVal;
}
public Object case224_line899(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newAndNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case455_line1734(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("__ENCODING__", Tokens.k__ENCODING__, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case379_line1433(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop]), null, null);
    return yyVal;
}
public Object case100_line672(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_colon2(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case223_line896(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">>", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case210_line851(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=>", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case311_line1207(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new NextNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
    return yyVal;
}
public Object case440_line1702(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case407_line1538(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case340_line1289(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((ListNode)yyVals[-7+yyTop])), ((ListNode)yyVals[-7+yyTop]), ((ListNode)yyVals[-5+yyTop]), ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case336_line1276(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(getPosition(((Token)yyVals[-3+yyTop])), null, support.assignable(((Token)yyVals[-2+yyTop]), null), ((ListNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case302_line1162(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new SClassNode(getPosition(((Token)yyVals[-7+yyTop])), ((Node)yyVals[-5+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                    support.popCurrentScope();
                    support.setInDef(((Boolean)yyVals[-4+yyTop]).booleanValue());
                    support.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
    return yyVal;
}
public Object case303_line1168(Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) { 
                        yyerror("module definition in method body");
                    }
                    support.pushLocalScope();
    return yyVal;
}
public Object case297_line1140(Object yyVal, Object[] yyVals, int yyTop) {
                      /* ENEBO: Lots of optz in 1.9 parser here*/
                    yyVal = new ForNode(getPosition(((Token)yyVals[-8+yyTop])), ((Node)yyVals[-7+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-4+yyTop]));
    return yyVal;
}
public Object case287_line1114(Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.getConditionState().begin();
    return yyVal;
}
public Object case494_line1898(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case477_line1809(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((RestArgNode)yyVals[-1+yyTop])), null, null, ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case421_line1622(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new StrNode(((Token)yyVals[0+yyTop]).getPosition(), ByteList.create(""));
    return yyVal;
}
public Object case197_line808(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "+", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case25_line397(Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[0+yyTop]));

                    String asgnOp = (String) ((Token)yyVals[-1+yyTop]).getValue();
                    if (asgnOp.equals("||")) {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                        yyVal = new OpAsgnOrNode(getPosition(((AssignableNode)yyVals[-2+yyTop])), support.gettable2(((AssignableNode)yyVals[-2+yyTop])), ((AssignableNode)yyVals[-2+yyTop]));
                    } else if (asgnOp.equals("&&")) {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                        yyVal = new OpAsgnAndNode(getPosition(((AssignableNode)yyVals[-2+yyTop])), support.gettable2(((AssignableNode)yyVals[-2+yyTop])), ((AssignableNode)yyVals[-2+yyTop]));
                    } else {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(support.getOperatorCallNode(support.gettable2(((AssignableNode)yyVals[-2+yyTop])), asgnOp, ((Node)yyVals[0+yyTop])));
                        ((AssignableNode)yyVals[-2+yyTop]).setPosition(getPosition(((AssignableNode)yyVals[-2+yyTop])));
                        yyVal = ((AssignableNode)yyVals[-2+yyTop]);
                    }
    return yyVal;
}
public Object case10_line333(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new AliasNode(getPosition(((Token)yyVals[-3+yyTop])), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case531_line1992(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
}
public Object case472_line1794(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((ListNode)yyVals[-1+yyTop])), ((ListNode)yyVals[-1+yyTop]), null, null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case465_line1772(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ArgsNode)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case381_line1439(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop]), new Token("call", ((Node)yyVals[-2+yyTop]).getPosition()), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case378_line1430(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case13_line342(Object yyVal, Object[] yyVals, int yyTop) {
                    yyerror("can't make alias for the number variables");
    return yyVal;
}
public Object case304_line1173(Object yyVal, Object[] yyVals, int yyTop) {
                    Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);

                    yyVal = new ModuleNode(getPosition(((Token)yyVals[-4+yyTop])), ((Colon3Node)yyVals[-3+yyTop]), support.getCurrentScope(), body);
                    support.popCurrentScope();
    return yyVal;
}
public Object case431_line1660(Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = new InstVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case370_line1395(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case351_line1323(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((RestArgNode)yyVals[-1+yyTop])), null, null, ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case30_line426(Object yyVal, Object[] yyVals, int yyTop) {
                    support.backrefAssignError(((Node)yyVals[-2+yyTop]));
    return yyVal;
}
public Object case251_line980(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newSplatNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case347_line1311(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((ListNode)yyVals[-3+yyTop])), null, ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case301_line1158(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Integer(support.getInSingle());
                    support.setInSingle(0);
                    support.pushLocalScope();
    return yyVal;
}
public Object case462_line1758(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case363_line1366(Object yyVal, Object[] yyVals, int yyTop) {
                    support.new_bv(((Token)yyVals[0+yyTop]));
    return yyVal;
}
public Object case204_line829(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((FloatNode)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), getPosition()), "-@");
    return yyVal;
}
public Object case109_line699(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case296_line1138(Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.getConditionState().end();
    return yyVal;
}
public Object case241_line947(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(getPosition(((ListNode)yyVals[-1+yyTop])), new Hash19Node(getPosition(), ((ListNode)yyVals[-1+yyTop])));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case69_line549(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), new StarNode(getPosition()), ((ListNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case280_line1090(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(NilImplicitNode.NIL, "!");
    return yyVal;
}
public Object case453_line1728(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("__FILE__", Tokens.k__FILE__, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case377_line1427(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case205_line832(Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isLiteral(((Node)yyVals[0+yyTop]))) {
                        yyVal = ((Node)yyVals[0+yyTop]);
                    } else {
                        yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "+@");
                    }
    return yyVal;
}
public Object case112_line708(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.appendToBlock(((Node)yyVals[-3+yyTop]), new UndefNode(getPosition(((Node)yyVals[-3+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue()));
    return yyVal;
}
public Object case467_line1779(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((ListNode)yyVals[-7+yyTop])), ((ListNode)yyVals[-7+yyTop]), ((ListNode)yyVals[-5+yyTop]), ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case56_line506(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case285_line1108(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IfNode(getPosition(((Token)yyVals[-5+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case62_line526(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((MultipleAsgn19Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case89_line627(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.aryset(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case358_line1348(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ArgsNode)yyVals[-2+yyTop]);
    return yyVal;
}
public Object case55_line503(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])); 
    return yyVal;
}
public Object case242_line951(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.arg_append(((Node)yyVals[-3+yyTop]), new Hash19Node(getPosition(), ((ListNode)yyVals[-1+yyTop])));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case215_line866(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case346_line1308(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((ListNode)yyVals[-1+yyTop])), ((ListNode)yyVals[-1+yyTop]), null, null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case341_line1292(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case334_line1270(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(getPosition(((ListNode)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), new StarNode(getPosition()), ((ListNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case73_line561(Object yyVal, Object[] yyVals, int yyTop) {
                      yyVal = new MultipleAsgn19Node(getPosition(((Token)yyVals[-2+yyTop])), null, new StarNode(getPosition()), ((ListNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case214_line863(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case190_line779(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public Object case514_line1982(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(getPosition(((Token)yyVals[-1+yyTop])), new SymbolNode(getPosition(((Token)yyVals[-1+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue())).add(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case506_line1942(Object yyVal, Object[] yyVals, int yyTop) {
                    if (!(((Node)yyVals[0+yyTop]) instanceof SelfNode)) {
                        support.checkExpression(((Node)yyVals[0+yyTop]));
                    }
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case91_line633(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case290_line1122(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.getConditionState().begin();
    return yyVal;
}
public Object case15_line348(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IfNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), null);
    return yyVal;
}
public Object case21_line377(Object yyVal, Object[] yyVals, int yyTop) {
                    support.getResult().addBeginNode(new PreExeNode(getPosition(((Node)yyVals[-1+yyTop])), support.getCurrentScope(), ((Node)yyVals[-1+yyTop])));
                    support.popCurrentScope();
                    yyVal = null;
    return yyVal;
}
public Object case503_line1925(Object yyVal, Object[] yyVals, int yyTop) {
                    String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();

                    if (!support.is_local_id(((Token)yyVals[0+yyTop]))) {
                        yyerror("block argument must be local variable");
                    }
                    support.shadowing_lvar(((Token)yyVals[0+yyTop]));
                    yyVal = new BlockArgNode(getPosition(((Token)yyVals[-1+yyTop])), support.arg_var(((Token)yyVals[0+yyTop])), identifier);
    return yyVal;
}
public Object case458_line1746(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case424_line1632(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case423_line1629(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
}
public Object case410_line1573(Object yyVal, Object[] yyVals, int yyTop) {
                    int options = ((RegexpNode)yyVals[0+yyTop]).getOptions();
                    Node node = ((Node)yyVals[-1+yyTop]);

                    if (node == null) {
                        yyVal = new RegexpNode(getPosition(((Token)yyVals[-2+yyTop])), ByteList.create(""), options & ~ReOptions.RE_OPTION_ONCE);
                    } else if (node instanceof StrNode) {
                        yyVal = new RegexpNode(((Node)yyVals[-1+yyTop]).getPosition(), (ByteList) ((StrNode) node).getValue().clone(), options & ~ReOptions.RE_OPTION_ONCE);
                    } else if (node instanceof DStrNode) {
                        yyVal = new DRegexpNode(getPosition(((Token)yyVals[-2+yyTop])), (DStrNode) node, options, (options & ReOptions.RE_OPTION_ONCE) != 0);
                    } else {
                        yyVal = new DRegexpNode(getPosition(((Token)yyVals[-2+yyTop])), options, (options & ReOptions.RE_OPTION_ONCE) != 0).add(node);
                    }
    return yyVal;
}
public Object case228_line912(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case67_line543(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(getPosition(((ListNode)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-2+yyTop]), ((ListNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case208_line845(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "^", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case41_line459(Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case191_line782(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public Object case23_line388(Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case201_line820(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "%", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case229_line916(Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = ((Node)yyVals[0+yyTop]) != null ? ((Node)yyVals[0+yyTop]) : NilImplicitNode.NIL;
    return yyVal;
}
public Object case368_line1387(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ArgsNode)yyVals[-1+yyTop]);
                    ((ISourcePositionHolder)yyVal).setPosition(getPosition(((ArgsNode)yyVals[-1+yyTop])));
    return yyVal;
}
public Object case361_line1359(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
}
public Object case29_line423(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public Object case275_line1075(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_yield(getPosition(((Token)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case66_line540(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(getPosition(((ListNode)yyVals[-2+yyTop])), ((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), (ListNode) null);
    return yyVal;
}
public Object case325_line1241(Object yyVal, Object[] yyVals, int yyTop) {
    return yyVal;
}
public Object case480_line1818(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(), null, null, null, null, null);
    return yyVal;
}
public Object case474_line1800(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((ListNode)yyVals[-5+yyTop])), null, ((ListNode)yyVals[-5+yyTop]), ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case52_line494(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_fcall(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case404_line1519(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]) instanceof EvStrNode ? new DStrNode(getPosition(((Node)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop])) : ((Node)yyVals[0+yyTop]);
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
public Object case369_line1392(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case2_line283(Object yyVal, Object[] yyVals, int yyTop) {
  /* ENEBO: Removed !compile_for_eval which probably is to reduce warnings*/
                  if (((Node)yyVals[0+yyTop]) != null) {
                      /* last expression should not be void */
                      if (((Node)yyVals[0+yyTop]) instanceof BlockNode) {
                          support.checkUselessStatement(((BlockNode)yyVals[0+yyTop]).getLast());
                      } else {
                          support.checkUselessStatement(((Node)yyVals[0+yyTop]));
                      }
                  }
                  support.getResult().setAST(support.addRootNode(((Node)yyVals[0+yyTop]), getPosition(((Node)yyVals[0+yyTop]))));
    return yyVal;
}
public Object case279_line1087(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(support.getConditionNode(((Node)yyVals[-1+yyTop])), "!");
    return yyVal;
}
public Object case24_line392(Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    ((MultipleAsgn19Node)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                    yyVal = ((MultipleAsgn19Node)yyVals[-2+yyTop]);
    return yyVal;
}
public Object case463_line1761(Object yyVal, Object[] yyVals, int yyTop) {
                   yyerrok();
                   yyVal = null;
    return yyVal;
}
public Object case413_line1595(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ArrayNode(getPosition());
    return yyVal;
}
public Object case86_line610(Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = getPosition(((Token)yyVals[-1+yyTop]));

                    yyVal = new ConstDeclNode(position, null, support.new_colon3(position, (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
    return yyVal;
}
public Object case1_line280(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.setState(LexState.EXPR_BEG);
                  support.initTopLocalVariables();
    return yyVal;
}
public Object case329_line1254(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case419_line1615(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ArrayNode(getPosition());
    return yyVal;
}
public Object case36_line445(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newAndNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case488_line1854(Object yyVal, Object[] yyVals, int yyTop) {
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
public Object case451_line1722(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("true", Tokens.kTRUE, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case387_line1462(Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
    return yyVal;
}
public Object case450_line1719(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("self", Tokens.kSELF, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case406_line1535(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case328_line1251(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case286_line1111(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IfNode(getPosition(((Token)yyVals[-5+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-2+yyTop]));
    return yyVal;
}
public Object case198_line811(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "-", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case104_line678(Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.setState(LexState.EXPR_END);
                   yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
}
public Object case483_line1828(Object yyVal, Object[] yyVals, int yyTop) {
                    yyerror("formal argument cannot be a global variable");
    return yyVal;
}
public Object case380_line1436(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop]), new Token("call", ((Node)yyVals[-2+yyTop]).getPosition()), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case95_line657(Object yyVal, Object[] yyVals, int yyTop) {
                    support.backrefAssignError(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case292_line1126(Object yyVal, Object[] yyVals, int yyTop) {
                    Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);
                    yyVal = new UntilNode(getPosition(((Token)yyVals[-6+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), body);
    return yyVal;
}
public Object case443_line1711(Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = support.negateFloat(((FloatNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case442_line1708(Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = support.negateInteger(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case343_line1298(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), null, ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case314_line1217(Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = ((Node)yyVals[0+yyTop]);
                    if (yyVal == null) yyVal = NilImplicitNode.NIL;
    return yyVal;
}
public Object case87_line619(Object yyVal, Object[] yyVals, int yyTop) {
                    support.backrefAssignError(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case93_line639(Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = getPosition(((Node)yyVals[-2+yyTop]));

                    yyVal = new ConstDeclNode(position, null, support.new_colon2(position, ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
    return yyVal;
}
public Object case293_line1130(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newCaseNode(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case384_line1448(Object yyVal, Object[] yyVals, int yyTop) {
                    if (((Node)yyVals[-3+yyTop]) instanceof SelfNode) {
                        yyVal = support.new_fcall(new Token("[]", getPosition(((Node)yyVals[-3+yyTop]))), ((Node)yyVals[-1+yyTop]), null);
                    } else {
                        yyVal = support.new_call(((Node)yyVals[-3+yyTop]), new Token("[]", getPosition(((Node)yyVals[-3+yyTop]))), ((Node)yyVals[-1+yyTop]), null);
                    }
    return yyVal;
}
public Object case339_line1286(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((ListNode)yyVals[-5+yyTop])), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case186_line737(Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    String asgnOp = (String) ((Token)yyVals[-1+yyTop]).getValue();

                    if (asgnOp.equals("||")) {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                        yyVal = new OpAsgnOrNode(getPosition(((AssignableNode)yyVals[-2+yyTop])), support.gettable2(((AssignableNode)yyVals[-2+yyTop])), ((AssignableNode)yyVals[-2+yyTop]));
                    } else if (asgnOp.equals("&&")) {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                        yyVal = new OpAsgnAndNode(getPosition(((AssignableNode)yyVals[-2+yyTop])), support.gettable2(((AssignableNode)yyVals[-2+yyTop])), ((AssignableNode)yyVals[-2+yyTop]));
                    } else {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(support.getOperatorCallNode(support.gettable2(((AssignableNode)yyVals[-2+yyTop])), asgnOp, ((Node)yyVals[0+yyTop])));
                        ((AssignableNode)yyVals[-2+yyTop]).setPosition(getPosition(((AssignableNode)yyVals[-2+yyTop])));
                        yyVal = ((AssignableNode)yyVals[-2+yyTop]);
                    }
    return yyVal;
}
public Object case48_line478(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case19_line368(Object yyVal, Object[] yyVals, int yyTop) {
                    Node body = ((Node)yyVals[0+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop]);
                    yyVal = new RescueNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), new RescueBodyNode(getPosition(((Node)yyVals[-2+yyTop])), null, body, null), null);
    return yyVal;
}
public Object case108_line696(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
}
public Object case409_line1557(Object yyVal, Object[] yyVals, int yyTop) {
                    ISourcePosition position = getPosition(((Token)yyVals[-2+yyTop]));

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
public Object case96_line661(Object yyVal, Object[] yyVals, int yyTop) {
                    yyerror("class/module name must be CONSTANT");
    return yyVal;
}
public Object case68_line546(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(getPosition(((ListNode)yyVals[-1+yyTop])), ((ListNode)yyVals[-1+yyTop]), new StarNode(getPosition()), null);
    return yyVal;
}
public Object case247_line969(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((BlockPassNode)yyVals[0+yyTop]);
    return yyVal;
}
public Object case187_line753(Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[-2+yyTop]));
                    ISourcePosition position = getPosition(((Token)yyVals[-1+yyTop]));
                    Node body = ((Node)yyVals[0+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop]);
                    Node rescueNode = new RescueNode(position, ((Node)yyVals[-2+yyTop]), new RescueBodyNode(position, null, body, null), null);

                    String asgnOp = (String) ((Token)yyVals[-3+yyTop]).getValue();
                    if (asgnOp.equals("||")) {
                        ((AssignableNode)yyVals[-4+yyTop]).setValueNode(((Node)yyVals[-2+yyTop]));
                        yyVal = new OpAsgnOrNode(getPosition(((AssignableNode)yyVals[-4+yyTop])), support.gettable2(((AssignableNode)yyVals[-4+yyTop])), ((AssignableNode)yyVals[-4+yyTop]));
                    } else if (asgnOp.equals("&&")) {
                        ((AssignableNode)yyVals[-4+yyTop]).setValueNode(((Node)yyVals[-2+yyTop]));
                        yyVal = new OpAsgnAndNode(getPosition(((AssignableNode)yyVals[-4+yyTop])), support.gettable2(((AssignableNode)yyVals[-4+yyTop])), ((AssignableNode)yyVals[-4+yyTop]));
                    } else {
                        ((AssignableNode)yyVals[-4+yyTop]).setValueNode(support.getOperatorCallNode(support.gettable2(((AssignableNode)yyVals[-4+yyTop])), asgnOp, ((Node)yyVals[-2+yyTop])));
                        ((AssignableNode)yyVals[-4+yyTop]).setPosition(getPosition(((AssignableNode)yyVals[-4+yyTop])));
                        yyVal = ((AssignableNode)yyVals[-4+yyTop]);
                    }
    return yyVal;
}
public Object case77_line574(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case84_line598(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case240_line944(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.arg_blk_pass(((Node)yyVals[-1+yyTop]), ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case489_line1869(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ArrayNode(getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case468_line1782(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case441_line1705(Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = ((FloatNode)yyVals[0+yyTop]);
    return yyVal;
}
public Object case411_line1588(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ZArrayNode(getPosition(((Token)yyVals[-2+yyTop])));
    return yyVal;
}
public Object case308_line1192(Object yyVal, Object[] yyVals, int yyTop) {
                    support.setInSingle(support.getInSingle() + 1);
                    support.pushLocalScope();
                    lexer.setState(LexState.EXPR_END); /* force for args */
    return yyVal;
}
public Object case49_line481(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case327_line1247(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case195_line794(Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[-2+yyTop]));
                    support.checkExpression(((Node)yyVals[0+yyTop]));
    
                    boolean isLiteral = ((Node)yyVals[-2+yyTop]) instanceof FixnumNode && ((Node)yyVals[0+yyTop]) instanceof FixnumNode;
                    yyVal = new DotNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), false, isLiteral);
    return yyVal;
}
public Object case479_line1815(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((BlockArgNode)yyVals[0+yyTop]).getPosition(), null, null, null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case353_line1329(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((BlockArgNode)yyVals[0+yyTop])), null, null, null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case75_line566(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case248_line972(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
}
public Object case111_line706(Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setState(LexState.EXPR_FNAME);
    return yyVal;
}
public Object case513_line1972(Object yyVal, Object[] yyVals, int yyTop) {
                    ISourcePosition position;
                    if (((Node)yyVals[-2+yyTop]) == null && ((Node)yyVals[0+yyTop]) == null) {
                        position = getPosition(((Token)yyVals[-1+yyTop]));
                    } else {
                        position = getPosition(((Node)yyVals[-2+yyTop]));
                    }

                    yyVal = support.newArrayNode(position, ((Node)yyVals[-2+yyTop])).add(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case470_line1788(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), null, ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case461_line1756(Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.setState(LexState.EXPR_BEG);
    return yyVal;
}
public Object case354_line1333(Object yyVal, Object[] yyVals, int yyTop) {
    /* was $$ = null;*/
                   yyVal = support.new_args(getPosition(), null, null, null, null, null);
    return yyVal;
}
public Object case8_line327(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case79_line582(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case54_line500(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case70_line552(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(getPosition(((Token)yyVals[-1+yyTop])), null, ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case385_line1456(Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
    return yyVal;
}
public Object case270_line1054(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_colon2(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case7_line324(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.appendToBlock(((Node)yyVals[-2+yyTop]), support.newline_node(((Node)yyVals[0+yyTop]), getPosition(((Node)yyVals[0+yyTop]))));
    return yyVal;
}
public Object case189_line776(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public Object case213_line860(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case3_line296(Object yyVal, Object[] yyVals, int yyTop) {
                  Node node = ((Node)yyVals[-3+yyTop]);

                  if (((RescueBodyNode)yyVals[-2+yyTop]) != null) {
                      node = new RescueNode(getPosition(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop]), ((RescueBodyNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
                  } else if (((Node)yyVals[-1+yyTop]) != null) {
                      warnings.warn(ID.ELSE_WITHOUT_RESCUE, getPosition(((Node)yyVals[-3+yyTop])), "else without rescue is useless");
                      node = support.appendToBlock(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                  }
                  if (((Node)yyVals[0+yyTop]) != null) {
                      if (node == null) node = NilImplicitNode.NIL;
                      node = new EnsureNode(getPosition(((Node)yyVals[-3+yyTop])), node, ((Node)yyVals[0+yyTop]));
                  }

                  yyVal = node;
    return yyVal;
}
public Object case352_line1326(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(getPosition(((RestArgNode)yyVals[-3+yyTop])), null, null, ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case90_line630(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case203_line826(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), getPosition()), "-@");
    return yyVal;
}
					// line 2019 "Ruby19Parser.y"

    /** The parse method use an lexer stream and parse it to an AST node 
     * structure
     */
    public RubyParserResult parse(ParserConfiguration configuration, LexerSource source) {
        support.reset();
        support.setConfiguration(configuration);
        support.setResult(new RubyParserResult());
        
        lexer.reset();
        lexer.setSource(source);
        lexer.setEncoding(configuration.getKCode().getEncoding());
        try {
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
        } catch (IOException e) {
            e.printStackTrace();
        } catch (yyException e) {
            e.printStackTrace();
        }
        
        return support.getResult();
    }

    // +++
    // Helper Methods
    
    void yyerrok() {}

    /**
     * Since we can recieve positions at times we know can be null we
     * need an extra safety net here.
     */
    private ISourcePosition getPosition2(ISourcePositionHolder pos) {
        return pos == null ? lexer.getPosition() : pos.getPosition();
    }

    private ISourcePosition getPosition(ISourcePositionHolder start) {
        return start != null ? lexer.getPosition(start.getPosition()) : lexer.getPosition();
    }

    private ISourcePosition getPosition() {
        return lexer.getPosition();
    }
}
					// line 8143 "-"
