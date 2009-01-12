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
 * Copyright (C) 2008 Thomas E Enebo <enebo@acm.org>
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
import org.jruby.ast.HashNode;
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
    }

    public void setWarnings(IRubyWarnings warnings) {
        this.warnings = warnings;

        support.setWarnings(warnings);
        lexer.setWarnings(warnings);
    }
					// line 150 "-"
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
//yyLhs 541
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
    48,    48,    48,    48,   126,    52,    89,    88,    88,    76,
    76,    76,    76,    67,    67,    67,    40,    40,    40,    40,
    40,    40,    40,    40,    40,    40,   127,    40,    40,    40,
    40,    40,    40,    40,    40,    40,    40,    40,    40,    40,
    40,    40,    40,    40,    40,    40,   129,   131,    40,   132,
   133,    40,    40,    40,   134,   135,    40,   136,    40,   138,
   139,    40,   140,    40,   141,    40,   142,   143,    40,    40,
    40,    40,    40,    43,   128,   128,   128,   130,   130,    46,
    46,    44,    44,   107,   107,   109,   109,    81,    81,   110,
   110,   110,   110,   110,   110,   110,   110,   110,    64,    64,
    64,    64,    64,    64,    64,    64,    64,    64,    64,    64,
    64,    64,    64,    66,    66,    65,    65,    65,   102,   102,
   101,   101,   111,   111,   144,   104,    63,    63,   103,   103,
   145,    94,    55,    55,    55,    25,    25,    25,    25,    25,
    25,    25,    25,    25,   146,    93,   147,    93,    71,    45,
    45,    98,    98,    72,    72,    72,    47,    47,    49,    49,
    29,    29,    29,    17,    18,    18,    18,    19,    20,    21,
    26,    26,    78,    78,    28,    28,    27,    27,    77,    77,
    22,    22,    23,    23,    24,   148,    24,   149,    24,    59,
    59,    59,    59,     4,     3,     3,     3,     3,    31,    30,
    30,    30,    30,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,    53,    99,    60,    60,    51,
   150,    51,    51,    62,    62,    61,    61,    61,    61,    61,
    61,    61,    61,    61,    61,    61,    61,    61,    61,    61,
   116,   116,   116,   116,    11,    11,   100,   100,    79,    79,
    57,   106,    87,    87,    80,    80,    13,    13,    15,    15,
    14,    14,    92,    91,    91,    16,   151,    16,    85,    85,
    83,    83,    84,    84,     5,     5,     5,     6,     6,     6,
     6,     7,     7,     7,    12,    12,   118,   118,   124,   124,
   113,   114,   125,   125,   125,   137,   137,   119,   119,    75,
    90,
    }, yyLen = {
//yyLen 541
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
     2,     3,     4,     3,     4,     2,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     3,     0,     4,     3,     3,
     2,     3,     3,     1,     4,     3,     1,     5,     4,     3,
     2,     1,     2,     2,     6,     6,     0,     0,     7,     0,
     0,     7,     5,     4,     0,     0,     9,     0,     6,     0,
     0,     8,     0,     5,     0,     6,     0,     0,     9,     1,
     1,     1,     1,     1,     1,     1,     2,     1,     1,     1,
     5,     1,     2,     1,     1,     1,     3,     1,     3,     1,
     4,     6,     3,     5,     2,     4,     1,     3,     6,     8,
     4,     6,     4,     2,     6,     2,     4,     6,     2,     4,
     2,     4,     1,     1,     1,     3,     1,     4,     1,     2,
     1,     3,     1,     1,     0,     3,     4,     2,     3,     3,
     0,     5,     2,     4,     4,     2,     4,     4,     3,     3,
     3,     2,     1,     4,     0,     5,     0,     5,     5,     1,
     1,     6,     0,     1,     1,     1,     2,     1,     2,     1,
     1,     1,     1,     1,     1,     1,     2,     3,     3,     3,
     3,     3,     0,     3,     1,     2,     3,     3,     0,     3,
     0,     2,     0,     2,     1,     0,     3,     0,     4,     1,
     1,     1,     1,     2,     1,     1,     1,     1,     3,     1,
     1,     2,     2,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     0,     4,     2,     3,     2,     6,     8,     4,     6,     4,
     6,     2,     4,     6,     2,     4,     2,     4,     1,     0,
     1,     1,     1,     1,     1,     1,     1,     3,     1,     3,
     3,     3,     1,     3,     1,     3,     1,     1,     2,     1,
     1,     1,     2,     2,     0,     1,     0,     4,     1,     2,
     1,     3,     3,     2,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     0,     1,     0,     1,
     2,     2,     0,     1,     1,     1,     1,     1,     2,     0,
     0,
    }, yyDefRed = {
//yyDefRed 945
     1,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   286,   289,     0,     0,     0,   311,   312,     0,
     0,     0,   449,   448,   450,   451,     0,     0,     0,    20,
     0,   453,   452,   454,     0,     0,   445,   444,     0,   447,
   404,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   420,   422,   422,     0,     0,   364,   457,
   458,   439,   440,     0,   401,     0,   257,     0,   405,   258,
   259,     0,   260,   261,   256,   400,   402,    35,     2,     0,
     0,     0,     0,     0,     0,     0,   262,     0,    43,     0,
     0,    74,     0,     5,     0,     0,    60,     0,     0,   309,
   310,   273,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   455,     0,    99,     0,   313,     0,   263,   302,
   152,   163,   153,   176,   149,   169,   159,   158,   174,   157,
   156,   151,   177,   161,   150,   164,   168,   170,   162,   155,
   171,   178,   173,     0,     0,     0,     0,   148,   167,   166,
   179,   180,   181,   182,   183,   147,   154,   145,   146,     0,
     0,     0,     0,   103,     0,   137,   138,   134,   116,   117,
   118,   125,   122,   124,   119,   120,   139,   140,   126,   127,
   506,   131,   130,   115,   136,   133,   132,   128,   129,   123,
   121,   113,   135,   114,   141,   304,   104,     0,   505,   105,
   172,   165,   175,   160,   142,   143,   144,   101,   102,   110,
   107,   106,   109,     0,   108,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   535,   536,     0,     0,
     0,   537,     0,     0,     0,     0,     0,     0,   323,   324,
     0,     0,     0,     0,     0,     0,   239,    45,     0,     0,
     0,   510,   243,    46,    44,     0,    59,     0,     0,   381,
    58,     0,    38,     0,     9,   529,     0,     0,     0,   205,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   230,     0,     0,     0,   508,     0,     0,     0,     0,
     0,     0,     0,     0,   221,    39,   220,   436,   435,   437,
   433,   434,     0,     0,     0,     0,     0,     0,     0,     0,
   283,     0,   386,   384,   375,     0,   280,   406,   282,     4,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   370,   372,     0,     0,     0,     0,
     0,     0,    76,     0,     0,     0,     0,     0,     0,     0,
   441,   442,     0,    96,     0,    98,     0,   460,   297,   459,
     0,     0,     0,     0,     0,     0,   524,   525,   306,   111,
     0,     0,   265,     0,   315,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   538,     0,     0,
     0,     0,     0,     0,   294,   513,   250,   246,     0,     0,
   240,   248,     0,   241,     0,   275,     0,   245,   238,   237,
     0,     0,   279,    11,    13,    12,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   268,     0,     0,
   271,     0,   533,   231,     0,   233,   509,   272,     0,    78,
     0,     0,     0,     0,     0,   427,   425,   438,   424,   423,
   407,   421,   408,   409,   410,   411,   414,     0,   416,   417,
     0,     0,   482,   481,   480,   483,     0,     0,   497,   496,
   501,   500,   486,     0,     0,     0,   494,     0,     0,     0,
     0,   478,   488,   484,     0,     0,    50,    53,     0,    15,
    16,    17,    18,    19,    36,    37,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   519,     0,     0,   520,   379,     0,
     0,     0,     0,   378,     0,   380,     0,   517,   518,     0,
     0,    30,     0,     0,    23,     0,    31,   249,     0,     0,
     0,     0,    77,    24,    33,     0,    25,     0,     0,   462,
     0,     0,     0,     0,     0,     0,   100,     0,     0,     0,
     0,     0,     0,     0,     0,   394,     0,     0,   395,     0,
     0,   321,     0,   316,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   293,   318,   287,   317,   290,     0,     0,
     0,     0,     0,     0,   512,     0,     0,     0,   247,   511,
   274,   530,   234,   278,    10,     0,     0,    22,     0,     0,
     0,     0,   267,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   413,   415,   419,     0,   485,     0,     0,
   325,     0,   327,     0,     0,   498,   502,     0,   476,     0,
   358,   367,     0,     0,   365,     0,   471,     0,   474,   356,
     0,   354,     0,   353,     0,     0,     0,     0,     0,     0,
   236,     0,   376,   235,     0,     0,   377,     0,     0,     0,
    48,   373,    49,   374,     0,     0,     0,    75,     0,     0,
     0,   300,     0,     0,   383,   303,   507,     0,   464,     0,
   307,   112,     0,     0,   397,   322,     0,     3,   399,     0,
   319,     0,     0,     0,     0,     0,     0,   292,     0,     0,
     0,     0,     0,     0,   252,   242,   277,    21,   232,    79,
     0,     0,   429,   430,   431,   426,   432,   490,     0,     0,
     0,     0,   487,     0,     0,   503,   362,     0,   360,   363,
     0,     0,     0,     0,   489,     0,   495,     0,     0,     0,
     0,     0,     0,   352,     0,   492,     0,     0,     0,     0,
     0,    27,     0,    28,     0,    55,    29,     0,     0,    57,
     0,   531,     0,     0,     0,     0,     0,     0,   461,   298,
   463,   305,     0,     0,     0,     0,     0,   396,     0,   398,
     0,   284,     0,   285,   251,     0,     0,     0,   295,   428,
   326,     0,     0,     0,   328,   366,     0,   477,     0,   369,
   368,     0,   469,     0,   467,     0,   472,   475,     0,     0,
   350,     0,     0,   345,     0,   348,   355,   387,   385,     0,
     0,   371,    26,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   389,   388,   390,   288,   291,     0,     0,
     0,     0,     0,   361,     0,     0,     0,     0,     0,     0,
     0,   357,     0,     0,     0,     0,   493,    51,   301,     0,
     0,     0,     0,     0,     0,   391,     0,     0,     0,     0,
   470,     0,   465,   468,   473,   270,     0,   351,     0,   342,
     0,   340,     0,   346,   349,   308,     0,   320,   296,     0,
     0,     0,     0,     0,     0,     0,     0,   466,   344,     0,
   338,   341,   347,     0,   339,
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
     0,     0, 14162, 14409, 16746, 17115, 17823, 17715, 14162, 16377,
 16377, 12613,     0,     0, 16869, 14532, 14532,     0,     0, 14532,
  -250,  -238,     0,     0,     0,     0, 15270, 17607,   133,     0,
  -201,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 16500, 16500,  -124,  -141, 14286, 16377, 14901, 15393,  5733,
 16500, 16623, 17930,     0,     0,     0,   185,   262,     0,     0,
     0,     0,     0,     0,     0,   -45,     0,   -48,     0,     0,
     0,  -188,     0,     0,     0,     0,     0,     0,     0,   216,
   740,   -22,  4610,     0,    58,   -44,     0,  -224,     0,    11,
   313,     0,   312,     0, 16992,   393,     0,   176,   740,     0,
     0,     0,  -250,  -238,   165,   133,     0,     0,  -101, 16377,
    54, 14162,     0,   -45,     0,     7,     0,   -32,     0,     0,
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
   540,     0, 16377, 16377,   297,     0,   475,     0,     0,     0,
   340, 16500, 16500, 16500, 16500,  4610,     0,     0,   284,   585,
   591,     0,     0,     0,     0,  3181,     0, 14532, 14532,     0,
     0,  4167,     0,   -74,     0,     0, 15516,   288, 14162,     0,
   477,   350,   351,   353,   342, 14286,   337,     0,   133,   -22,
   338,     0,   119,   289,   284,     0,   289,   327,   387, 17238,
     0,   509,     0,   657,     0,     0,     0,     0,     0,     0,
     0,     0,   -90,   444,   542,   273,   331,   554,   347,   171,
     0,  2130,     0,     0,     0,   366,     0,     0,     0,     0,
 14038, 16377, 16377, 16377, 16377, 14409, 16377, 16377, 16500, 16500,
 16500, 16500, 16500, 16500, 16500, 16500, 16500, 16500, 16500, 16500,
 16500, 16500, 16500, 16500, 16500, 16500, 16500, 16500, 16500, 16500,
 16500, 16500, 16500, 16500,     0,     0,  2198,  2643, 14532, 18477,
 18477, 16623,     0, 15639, 14286, 13915,   681, 15639, 16623,   391,
     0,     0,   -22,     0,     0,     0,   216,     0,     0,     0,
  3666,  4740, 14532, 14162, 16377,  2263,     0,     0,     0,     0,
 15762,   467,     0,   342,     0, 14162,   470,  5233,  6353, 14532,
 16500, 16500, 16500, 14162,   323, 15885,   474,     0,    38,    38,
     0, 18092, 18147, 14532,     0,     0,     0,     0, 16500, 14655,
     0,     0, 15024,     0,   133,     0,   398,     0,     0,     0,
   133,    46,     0,     0,     0,     0, 17715, 16377,  4610, 14162,
   386,  5233,  6353, 16500, 16500, 16500,   133,     0,     0,   133,
     0, 15147,     0,     0, 15393,     0,     0,     0,     0,     0,
   715, 18202, 18257, 14532, 17238,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   101,     0,     0,
   735,   701,     0,     0,     0,     0,  1160,  1671,     0,     0,
     0,     0,     0,   462,   476,   739,     0,   725,  -121,   741,
   744,     0,     0,     0,  -183,  -183,     0,     0,   740,     0,
     0,     0,     0,     0,     0,     0,   350,  2770,  2770,  2770,
  2770,  1218,  1218,  4244,  3714,  2770,  2770,  2728,  2728,   728,
   728,   350,  1556,   350,   350,   354,   354,  1218,  1218,  1452,
  1452,  2588,  -183,   459,     0,   460,  -238,     0,     0,   461,
     0,   481,  -238,     0,     0,     0,   133,     0,     0,  -238,
  -238,     0,  4610, 16500,     0,  4679,     0,     0,   755,   133,
 17238,   775,     0,     0,     0,     0,     0,  5170,   216,     0,
 16377, 14162,  -238,     0,     0,  -238,     0,   133,   562,    46,
  1671,   216, 14162, 18037, 17715,     0,     0,   498,     0, 14162,
   575,     0,   341,     0,   517,   519,   520,   481,   133,  4679,
   467,   584,    78,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   133, 16377,     0, 16500,   284,   591,     0,     0,
     0,     0,     0,     0,     0,    46,   496,     0,   350,   350,
  4610,     0,     0,   289, 17238,     0,     0,     0,     0,   133,
   715, 14162,   376,     0,     0,     0, 16500,     0,  1160,   650,
     0,   818,     0,   133,   725,     0,     0,   889,     0,  1023,
     0,     0, 14162, 14162,     0,  1671,     0,  1671,     0,     0,
  1751,     0, 14162,     0, 14162,  -183,   817, 14162, 16623, 16623,
     0,   366,     0,     0, 16623, 16500,     0,   366,   541,   546,
     0,     0,     0,     0,     0, 16500, 16008,     0,   715, 17238,
 16500,     0,   216,   621,     0,     0,     0,   133,     0,   628,
     0,     0, 17361,    55,     0,     0, 14162,     0,     0, 16377,
     0,   633, 16500, 16500, 16500,   567,   641,     0, 16131, 14162,
 14162, 14162,     0,    38,     0,     0,     0,     0,     0,     0,
     0,   549,     0,     0,     0,     0,     0,     0,   133,  1628,
   864,  1615,     0,   133,   871,     0,     0,   877,     0,     0,
   664,   574,   896,   902,     0,   903,     0,   871,   872,   904,
   725,   906,   907,     0,   594,     0,   691,   600, 14162, 16500,
   700,     0,  4610,     0,  4610,     0,     0,  4610,  4610,     0,
 16623,     0,  4610, 16500,     0,   715,  4610, 14162,     0,     0,
     0,     0,  2263,   656,     0,   532,     0,     0, 14162,     0,
    55,     0, 16500,     0,     0,    36,   706,   708,     0,     0,
     0,   930,  1628,   679,     0,     0,   889,     0,  1023,     0,
     0,   889,     0,  1671,     0,   889,     0,     0, 17484,   889,
     0,   618,  2281,     0,  2281,     0,     0,     0,     0,   616,
  4610,     0,     0,  4610,     0,   717, 14162,     0, 18312, 18367,
 14532,   264, 14162,     0,     0,     0,     0,     0, 14162,  1628,
   930,  1628,   935,     0,   871,   938,   871,   871,   673,   560,
   871,     0,   947,   952,   953,   871,     0,     0,     0,   737,
     0,     0,     0,     0,   133,     0,   341,   743,   930,  1628,
     0,   889,     0,     0,     0,     0, 18422,     0,   889,     0,
  2281,     0,   889,     0,     0,     0,     0,     0,     0,   930,
   871,     0,     0,   871,   964,   871,   871,     0,     0,   889,
     0,     0,     0,   871,     0,
    }, yyRindex = {
//yyRindex 945
     0,     0,   168,     0,     0,     0,     0,     0,   294,     0,
     0,   742,     0,     0,     0, 12951, 13057,     0,     0, 13199,
  4495,  4002,     0,     0,     0,     0,     0,     0, 16254,     0,
     0,     0,     0,     0,  1907,  3016,     0,     0,  2030,     0,
     0,     0,     0,     0,     0,   136,     0,   666,   649,    42,
     0,     0,   698,     0,     0,     0,   797,   214,     0,     0,
     0,     0,     0, 13302,     0, 14778,     0,  6717,     0,     0,
     0,  6818,     0,     0,     0,     0,     0,     0,     0,   370,
   761, 11139,  1251,  6962,  1587,     0,     0, 13765,     0, 13416,
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
   847,     0,     0,     6,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   686,     0,     0,     0,     0,     0,
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
  3386,  5472, 14778,     0,  3509,     0,   707,     0,     0,  5595,
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
     0,  1363,     0,     0,     0,     0,     0,  4613, 12850,     0,
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
     0,     0, 10998,     0, 13805,   209, 11084,   294,     0,     0,
     0,     0,   241,     0,   736,     0,   764,     0,   294,     0,
     0,     0,     0,     0,     0,   788,     0,     0,     0,     0,
     0,   152,     0,   173,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,    86,     0,     0,     0,     0,     0,     0,     0,
 11181,     0,     0, 11305, 13822,     0,   294,   792,     0,     0,
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
    throw new SyntaxException(PID.GRAMMAR_ERROR, getPosition(null), message);
  }

  /** (syntax) error message.
      Can be overwritten to control message format.
      @param message text to be displayed.
      @param expected list of acceptable tokens, if available.
    */
  public void yyerror (String message, String[] expected, String found) {
    String text = ", unexpected " + found + "\n"; 
    //new Exception().printStackTrace();
    throw new SyntaxException(PID.GRAMMAR_ERROR, getPosition(null), text, found);
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

      yyDiscarded: for (;;) {	// discarding a token does not change stack
        int yyN;
        if ((yyN = yyDefRed[yyState]) == 0) {	// else [default] reduce (yyN)
          if (yyToken < 0) {
            yyToken = yyLex.advance() ? yyLex.token() : 0;
          }
          if ((yyN = yySindex[yyState]) != 0 && (yyN += yyToken) >= 0
              && yyN < yyTable.length && yyCheck[yyN] == yyToken) {
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
  
            case 1: case 2:
              yyErrorFlag = 3;
              do {
                if ((yyN = yySindex[yyStates[yyTop]]) != 0
                    && (yyN += yyErrorCode) >= 0 && yyN < yyTable.length
                    && yyCheck[yyN] == yyErrorCode) {
                  yyState = yyTable[yyN];
                  yyVal = yyLex.value();
                  continue yyLoop;
                }
              } while (-- yyTop >= 0);
              throw new yyException("irrecoverable syntax error");
  
            case 3:
              if (yyToken == 0) {
                throw new yyException("irrecoverable syntax error at end-of-file");
              }
              yyToken = -1;
              continue yyDiscarded;		// leave stack alone
            }
        }
        int yyV = yyTop + 1-yyLen[yyN];
        yyVal = yyDefault(yyV > yyTop ? null : yyVals[yyV]);
        switch (yyN) {
case 1:
					// line 278 "Ruby19Parser.y"
  {
                  lexer.setState(LexState.EXPR_BEG);
                  support.initTopLocalVariables();
              }
  break;
case 2:
					// line 281 "Ruby19Parser.y"
  {
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
              }
  break;
case 3:
					// line 294 "Ruby19Parser.y"
  {
                  Node node = ((Node)yyVals[-3+yyTop]);

                  if (((RescueBodyNode)yyVals[-2+yyTop]) != null) {
                      node = new RescueNode(getPosition(((Node)yyVals[-3+yyTop]), true), ((Node)yyVals[-3+yyTop]), ((RescueBodyNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
                  } else if (((Node)yyVals[-1+yyTop]) != null) {
                      warnings.warn(ID.ELSE_WITHOUT_RESCUE, getPosition(((Node)yyVals[-3+yyTop])), "else without rescue is useless");
                      node = support.appendToBlock(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                  }
                  if (((Node)yyVals[0+yyTop]) != null) {
                      if (node == null) node = NilImplicitNode.NIL;
                      node = new EnsureNode(getPosition(((Node)yyVals[-3+yyTop])), node, ((Node)yyVals[0+yyTop]));
                  }

                  yyVal = node;
                }
  break;
case 4:
					// line 311 "Ruby19Parser.y"
  {
                    if (((Node)yyVals[-1+yyTop]) instanceof BlockNode) {
                        support.checkUselessStatements(((BlockNode)yyVals[-1+yyTop]));
                    }
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 6:
					// line 319 "Ruby19Parser.y"
  {
                    yyVal = support.newline_node(((Node)yyVals[0+yyTop]), getPosition(((Node)yyVals[0+yyTop]), true));
                }
  break;
case 7:
					// line 322 "Ruby19Parser.y"
  {
                    yyVal = support.appendToBlock(((Node)yyVals[-2+yyTop]), support.newline_node(((Node)yyVals[0+yyTop]), getPosition(((Node)yyVals[0+yyTop]), true)));
                }
  break;
case 8:
					// line 325 "Ruby19Parser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 9:
					// line 329 "Ruby19Parser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
                }
  break;
case 10:
					// line 331 "Ruby19Parser.y"
  {
                    yyVal = new AliasNode(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 11:
					// line 334 "Ruby19Parser.y"
  {
                    yyVal = new VAliasNode(getPosition(((Token)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 12:
					// line 337 "Ruby19Parser.y"
  {
                    yyVal = new VAliasNode(getPosition(((Token)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), "$" + ((BackRefNode)yyVals[0+yyTop]).getType());
                }
  break;
case 13:
					// line 340 "Ruby19Parser.y"
  {
                    yyerror("can't make alias for the number variables");
                }
  break;
case 14:
					// line 343 "Ruby19Parser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 15:
					// line 346 "Ruby19Parser.y"
  {
                    yyVal = new IfNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), null);
                }
  break;
case 16:
					// line 349 "Ruby19Parser.y"
  {
                    yyVal = new IfNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), null, ((Node)yyVals[-2+yyTop]));
                }
  break;
case 17:
					// line 352 "Ruby19Parser.y"
  {
                    if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = new WhileNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                    } else {
                        yyVal = new WhileNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                    }
                }
  break;
case 18:
					// line 359 "Ruby19Parser.y"
  {
                    if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = new UntilNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                    } else {
                        yyVal = new UntilNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                    }
                }
  break;
case 19:
					// line 366 "Ruby19Parser.y"
  {
                    Node body = ((Node)yyVals[0+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop]);
                    yyVal = new RescueNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), new RescueBodyNode(getPosition(((Node)yyVals[-2+yyTop])), null, body, null), null);
                }
  break;
case 20:
					// line 370 "Ruby19Parser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("BEGIN in method");
                    }
                    support.pushLocalScope();
                }
  break;
case 21:
					// line 375 "Ruby19Parser.y"
  {
                    support.getResult().addBeginNode(new PreExeNode(getPosition(((Node)yyVals[-1+yyTop])), support.getCurrentScope(), ((Node)yyVals[-1+yyTop])));
                    support.popCurrentScope();
                    yyVal = null;
                }
  break;
case 22:
					// line 380 "Ruby19Parser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        warnings.warn(ID.END_IN_METHOD, getPosition(((Token)yyVals[-3+yyTop])), "END in method; use at_exit");
                    }
                    yyVal = new PostExeNode(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 23:
					// line 386 "Ruby19Parser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 24:
					// line 390 "Ruby19Parser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    ((MultipleAsgn19Node)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                    yyVal = ((MultipleAsgn19Node)yyVals[-2+yyTop]);
                }
  break;
case 25:
					// line 395 "Ruby19Parser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));

                    String asgnOp = (String) ((Token)yyVals[-1+yyTop]).getValue();
                    if (asgnOp.equals("||")) {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                        yyVal = new OpAsgnOrNode(support.union(((AssignableNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), support.gettable2(((AssignableNode)yyVals[-2+yyTop])), ((AssignableNode)yyVals[-2+yyTop]));
                    } else if (asgnOp.equals("&&")) {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                        yyVal = new OpAsgnAndNode(support.union(((AssignableNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), support.gettable2(((AssignableNode)yyVals[-2+yyTop])), ((AssignableNode)yyVals[-2+yyTop]));
                    } else {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(support.getOperatorCallNode(support.gettable2(((AssignableNode)yyVals[-2+yyTop])), asgnOp, ((Node)yyVals[0+yyTop])));
                        ((AssignableNode)yyVals[-2+yyTop]).setPosition(support.union(((AssignableNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
                        yyVal = ((AssignableNode)yyVals[-2+yyTop]);
                    }
                }
  break;
case 26:
					// line 411 "Ruby19Parser.y"
  {
  /* FIXME: arg_concat logic missing for opt_call_args*/
                    yyVal = support.new_opElementAsgnNode(getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 27:
					// line 415 "Ruby19Parser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 28:
					// line 418 "Ruby19Parser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 29:
					// line 421 "Ruby19Parser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 30:
					// line 424 "Ruby19Parser.y"
  {
                    support.backrefAssignError(((Node)yyVals[-2+yyTop]));
                }
  break;
case 31:
					// line 427 "Ruby19Parser.y"
  {
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 32:
					// line 430 "Ruby19Parser.y"
  {
                    ((MultipleAsgn19Node)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                    yyVal = ((MultipleAsgn19Node)yyVals[-2+yyTop]);
                }
  break;
case 33:
					// line 434 "Ruby19Parser.y"
  {
                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                    yyVal = ((MultipleAsgn19Node)yyVals[-2+yyTop]);
                    ((MultipleAsgn19Node)yyVals[-2+yyTop]).setPosition(support.union(((MultipleAsgn19Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
                }
  break;
case 36:
					// line 443 "Ruby19Parser.y"
  {
                    yyVal = support.newAndNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 37:
					// line 446 "Ruby19Parser.y"
  {
                    yyVal = support.newOrNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 38:
					// line 449 "Ruby19Parser.y"
  {
                    yyVal = new NotNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
                }
  break;
case 39:
					// line 452 "Ruby19Parser.y"
  {
                    yyVal = new NotNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
                }
  break;
case 41:
					// line 457 "Ruby19Parser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                }
  break;
case 44:
					// line 464 "Ruby19Parser.y"
  {
                    yyVal = new ReturnNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
                }
  break;
case 45:
					// line 467 "Ruby19Parser.y"
  {
                    yyVal = new BreakNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
                }
  break;
case 46:
					// line 470 "Ruby19Parser.y"
  {
                    yyVal = new NextNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
                }
  break;
case 48:
					// line 476 "Ruby19Parser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
                }
  break;
case 49:
					// line 479 "Ruby19Parser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
                }
  break;
case 50:
					// line 484 "Ruby19Parser.y"
  {
                    support.pushBlockScope();
                }
  break;
case 51:
					// line 486 "Ruby19Parser.y"
  {
                    yyVal = new IterNode(getPosition(((Token)yyVals[-4+yyTop])), ((ArgsNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
                }
  break;
case 52:
					// line 492 "Ruby19Parser.y"
  {
                    yyVal = support.new_fcall(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
                }
  break;
case 53:
					// line 495 "Ruby19Parser.y"
  {
                    yyVal = support.new_fcall(((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop]));
                }
  break;
case 54:
					// line 498 "Ruby19Parser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
                }
  break;
case 55:
					// line 501 "Ruby19Parser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])); 
                }
  break;
case 56:
					// line 504 "Ruby19Parser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
                }
  break;
case 57:
					// line 507 "Ruby19Parser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop]));
                }
  break;
case 58:
					// line 510 "Ruby19Parser.y"
  {
                    yyVal = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop])); /* .setPosFrom($2);*/
                }
  break;
case 59:
					// line 513 "Ruby19Parser.y"
  {
                    yyVal = support.new_yield(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
                }
  break;
case 61:
					// line 519 "Ruby19Parser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 62:
					// line 524 "Ruby19Parser.y"
  {
                    yyVal = ((MultipleAsgn19Node)yyVals[0+yyTop]);
                }
  break;
case 63:
					// line 527 "Ruby19Parser.y"
  {
                    yyVal = new MultipleAsgn19Node(getPosition(((Token)yyVals[-2+yyTop])), support.newArrayNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])), null, null);
                }
  break;
case 64:
					// line 532 "Ruby19Parser.y"
  {
                    yyVal = new MultipleAsgn19Node(getPosition(((ListNode)yyVals[0+yyTop])), ((ListNode)yyVals[0+yyTop]), null, null);
                }
  break;
case 65:
					// line 535 "Ruby19Parser.y"
  {
                    yyVal = new MultipleAsgn19Node(support.union(((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), ((ListNode)yyVals[-1+yyTop]).add(((Node)yyVals[0+yyTop])), null, null);
                }
  break;
case 66:
					// line 538 "Ruby19Parser.y"
  {
                    yyVal = new MultipleAsgn19Node(getPosition(((ListNode)yyVals[-2+yyTop])), ((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), (ListNode) null);
                }
  break;
case 67:
					// line 541 "Ruby19Parser.y"
  {
                    yyVal = new MultipleAsgn19Node(getPosition(((ListNode)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-2+yyTop]), ((ListNode)yyVals[0+yyTop]));
                }
  break;
case 68:
					// line 544 "Ruby19Parser.y"
  {
                    yyVal = new MultipleAsgn19Node(getPosition(((ListNode)yyVals[-1+yyTop])), ((ListNode)yyVals[-1+yyTop]), new StarNode(getPosition(null)), null);
                }
  break;
case 69:
					// line 547 "Ruby19Parser.y"
  {
                    yyVal = new MultipleAsgn19Node(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), new StarNode(getPosition(null)), ((ListNode)yyVals[0+yyTop]));
                }
  break;
case 70:
					// line 550 "Ruby19Parser.y"
  {
                    yyVal = new MultipleAsgn19Node(getPosition(((Token)yyVals[-1+yyTop])), null, ((Node)yyVals[0+yyTop]), null);
                }
  break;
case 71:
					// line 553 "Ruby19Parser.y"
  {
                    yyVal = new MultipleAsgn19Node(getPosition(((Token)yyVals[-3+yyTop])), null, ((Node)yyVals[-2+yyTop]), ((ListNode)yyVals[0+yyTop]));
                }
  break;
case 72:
					// line 556 "Ruby19Parser.y"
  {
                      yyVal = new MultipleAsgn19Node(getPosition(((Token)yyVals[0+yyTop])), null, new StarNode(getPosition(null)), null);
                }
  break;
case 73:
					// line 559 "Ruby19Parser.y"
  {
                      yyVal = new MultipleAsgn19Node(getPosition(((Token)yyVals[-2+yyTop])), null, new StarNode(getPosition(null)), ((ListNode)yyVals[0+yyTop]));
                }
  break;
case 75:
					// line 564 "Ruby19Parser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 76:
					// line 569 "Ruby19Parser.y"
  {
                    yyVal = support.newArrayNode(((Node)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 77:
					// line 572 "Ruby19Parser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
                }
  break;
case 78:
					// line 577 "Ruby19Parser.y"
  {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 79:
					// line 580 "Ruby19Parser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 80:
					// line 584 "Ruby19Parser.y"
  {
                    yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
                }
  break;
case 81:
					// line 587 "Ruby19Parser.y"
  {
                    yyVal = support.aryset(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 82:
					// line 590 "Ruby19Parser.y"
  {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 83:
					// line 593 "Ruby19Parser.y"
  {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 84:
					// line 596 "Ruby19Parser.y"
  {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 85:
					// line 599 "Ruby19Parser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop]));

                    yyVal = new ConstDeclNode(position, null, support.new_colon2(position, ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
                }
  break;
case 86:
					// line 608 "Ruby19Parser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop]));

                    yyVal = new ConstDeclNode(position, null, support.new_colon3(position, (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
                }
  break;
case 87:
					// line 617 "Ruby19Parser.y"
  {
                    support.backrefAssignError(((Node)yyVals[0+yyTop]));
                }
  break;
case 88:
					// line 621 "Ruby19Parser.y"
  {
                      /* if (!($$ = assignable($1, 0))) $$ = NEW_BEGIN(0);*/
                    yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
                }
  break;
case 89:
					// line 625 "Ruby19Parser.y"
  {
                    yyVal = support.aryset(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 90:
					// line 628 "Ruby19Parser.y"
  {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 91:
					// line 631 "Ruby19Parser.y"
  {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 92:
					// line 634 "Ruby19Parser.y"
  {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 93:
					// line 637 "Ruby19Parser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop]));

                    yyVal = new ConstDeclNode(position, null, support.new_colon2(position, ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
                }
  break;
case 94:
					// line 646 "Ruby19Parser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop]));

                    yyVal = new ConstDeclNode(position, null, support.new_colon3(position, (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
                }
  break;
case 95:
					// line 655 "Ruby19Parser.y"
  {
                    support.backrefAssignError(((Node)yyVals[0+yyTop]));
                }
  break;
case 96:
					// line 659 "Ruby19Parser.y"
  {
                    yyerror("class/module name must be CONSTANT");
                }
  break;
case 98:
					// line 664 "Ruby19Parser.y"
  {
                    yyVal = support.new_colon3(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 99:
					// line 667 "Ruby19Parser.y"
  {
                    yyVal = support.new_colon2(((Token)yyVals[0+yyTop]).getPosition(), null, (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 100:
					// line 670 "Ruby19Parser.y"
  {
                    yyVal = support.new_colon2(support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 104:
					// line 676 "Ruby19Parser.y"
  {
                   lexer.setState(LexState.EXPR_END);
                   yyVal = ((Token)yyVals[0+yyTop]);
               }
  break;
case 105:
					// line 680 "Ruby19Parser.y"
  {
                   lexer.setState(LexState.EXPR_END);
                   yyVal = ((Token)yyVals[0+yyTop]);
               }
  break;
case 106:
					// line 686 "Ruby19Parser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 107:
					// line 689 "Ruby19Parser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 108:
					// line 694 "Ruby19Parser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 109:
					// line 697 "Ruby19Parser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 110:
					// line 701 "Ruby19Parser.y"
  {
                    yyVal = new UndefNode(getPosition(((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 111:
					// line 704 "Ruby19Parser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
                }
  break;
case 112:
					// line 706 "Ruby19Parser.y"
  {
                    yyVal = support.appendToBlock(((Node)yyVals[-3+yyTop]), new UndefNode(getPosition(((Node)yyVals[-3+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue()));
                }
  break;
case 184:
					// line 725 "Ruby19Parser.y"
  {
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    /* FIXME: Consider fixing node_assign itself rather than single case*/
                    ((Node)yyVal).setPosition(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
                }
  break;
case 185:
					// line 730 "Ruby19Parser.y"
  {
                    ISourcePosition position = support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                    Node body = ((Node)yyVals[0+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop]);
                    yyVal = support.node_assign(((Node)yyVals[-4+yyTop]), new RescueNode(position, ((Node)yyVals[-2+yyTop]), new RescueBodyNode(position, null, body, null), null));
                }
  break;
case 186:
					// line 735 "Ruby19Parser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    String asgnOp = (String) ((Token)yyVals[-1+yyTop]).getValue();

                    if (asgnOp.equals("||")) {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                        yyVal = new OpAsgnOrNode(support.union(((AssignableNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), support.gettable2(((AssignableNode)yyVals[-2+yyTop])), ((AssignableNode)yyVals[-2+yyTop]));
                    } else if (asgnOp.equals("&&")) {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                        yyVal = new OpAsgnAndNode(support.union(((AssignableNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), support.gettable2(((AssignableNode)yyVals[-2+yyTop])), ((AssignableNode)yyVals[-2+yyTop]));
                    } else {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(support.getOperatorCallNode(support.gettable2(((AssignableNode)yyVals[-2+yyTop])), asgnOp, ((Node)yyVals[0+yyTop])));
                        ((AssignableNode)yyVals[-2+yyTop]).setPosition(support.union(((AssignableNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
                        yyVal = ((AssignableNode)yyVals[-2+yyTop]);
                    }
                }
  break;
case 187:
					// line 751 "Ruby19Parser.y"
  {
                    support.checkExpression(((Node)yyVals[-2+yyTop]));
                    ISourcePosition position = support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                    Node body = ((Node)yyVals[0+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop]);
                    Node rescueNode = new RescueNode(position, ((Node)yyVals[-2+yyTop]), new RescueBodyNode(position, null, body, null), null);

                    String asgnOp = (String) ((Token)yyVals[-3+yyTop]).getValue();
                    if (asgnOp.equals("||")) {
                        ((AssignableNode)yyVals[-4+yyTop]).setValueNode(((Node)yyVals[-2+yyTop]));
                        yyVal = new OpAsgnOrNode(support.union(((AssignableNode)yyVals[-4+yyTop]), ((Node)yyVals[-2+yyTop])), support.gettable2(((AssignableNode)yyVals[-4+yyTop])), ((AssignableNode)yyVals[-4+yyTop]));
                    } else if (asgnOp.equals("&&")) {
                        ((AssignableNode)yyVals[-4+yyTop]).setValueNode(((Node)yyVals[-2+yyTop]));
                        yyVal = new OpAsgnAndNode(support.union(((AssignableNode)yyVals[-4+yyTop]), ((Node)yyVals[-2+yyTop])), support.gettable2(((AssignableNode)yyVals[-4+yyTop])), ((AssignableNode)yyVals[-4+yyTop]));
                    } else {
                        ((AssignableNode)yyVals[-4+yyTop]).setValueNode(support.getOperatorCallNode(support.gettable2(((AssignableNode)yyVals[-4+yyTop])), asgnOp, ((Node)yyVals[-2+yyTop])));
                        ((AssignableNode)yyVals[-4+yyTop]).setPosition(support.union(((AssignableNode)yyVals[-4+yyTop]), ((Node)yyVals[-2+yyTop])));
                        yyVal = ((AssignableNode)yyVals[-4+yyTop]);
                    }
                }
  break;
case 188:
					// line 770 "Ruby19Parser.y"
  {
  /* FIXME: arg_concat missing for opt_call_args*/
                    yyVal = support.new_opElementAsgnNode(getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 189:
					// line 774 "Ruby19Parser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 190:
					// line 777 "Ruby19Parser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 191:
					// line 780 "Ruby19Parser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 192:
					// line 783 "Ruby19Parser.y"
  {
                    yyerror("constant re-assignment");
                }
  break;
case 193:
					// line 786 "Ruby19Parser.y"
  {
                    yyerror("constant re-assignment");
                }
  break;
case 194:
					// line 789 "Ruby19Parser.y"
  {
                    support.backrefAssignError(((Node)yyVals[-2+yyTop]));
                }
  break;
case 195:
					// line 792 "Ruby19Parser.y"
  {
                    support.checkExpression(((Node)yyVals[-2+yyTop]));
                    support.checkExpression(((Node)yyVals[0+yyTop]));
    
                    boolean isLiteral = ((Node)yyVals[-2+yyTop]) instanceof FixnumNode && ((Node)yyVals[0+yyTop]) instanceof FixnumNode;
                    yyVal = new DotNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), false, isLiteral);
                }
  break;
case 196:
					// line 799 "Ruby19Parser.y"
  {
                    support.checkExpression(((Node)yyVals[-2+yyTop]));
                    support.checkExpression(((Node)yyVals[0+yyTop]));

                    boolean isLiteral = ((Node)yyVals[-2+yyTop]) instanceof FixnumNode && ((Node)yyVals[0+yyTop]) instanceof FixnumNode;
                    yyVal = new DotNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), true, isLiteral);
                }
  break;
case 197:
					// line 806 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "+", ((Node)yyVals[0+yyTop]), getPosition(null));
                }
  break;
case 198:
					// line 809 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "-", ((Node)yyVals[0+yyTop]), getPosition(null));
                }
  break;
case 199:
					// line 812 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "*", ((Node)yyVals[0+yyTop]), getPosition(null));
                }
  break;
case 200:
					// line 815 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "/", ((Node)yyVals[0+yyTop]), getPosition(null));
                }
  break;
case 201:
					// line 818 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "%", ((Node)yyVals[0+yyTop]), getPosition(null));
                }
  break;
case 202:
					// line 821 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), getPosition(null));
                }
  break;
case 203:
					// line 824 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), getPosition(null)), "-@");
                }
  break;
case 204:
					// line 827 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((FloatNode)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), getPosition(null)), "-@");
                }
  break;
case 205:
					// line 830 "Ruby19Parser.y"
  {
                    if (support.isLiteral(((Node)yyVals[0+yyTop]))) {
                        yyVal = ((Node)yyVals[0+yyTop]);
                    } else {
                        yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "+@");
                    }
                }
  break;
case 206:
					// line 837 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "-@");
                }
  break;
case 207:
					// line 840 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "|", ((Node)yyVals[0+yyTop]), getPosition(null));
                }
  break;
case 208:
					// line 843 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "^", ((Node)yyVals[0+yyTop]), getPosition(null));
                }
  break;
case 209:
					// line 846 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "&", ((Node)yyVals[0+yyTop]), getPosition(null));
                }
  break;
case 210:
					// line 849 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=>", ((Node)yyVals[0+yyTop]), getPosition(null));
                }
  break;
case 211:
					// line 852 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">", ((Node)yyVals[0+yyTop]), getPosition(null));
                }
  break;
case 212:
					// line 855 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">=", ((Node)yyVals[0+yyTop]), getPosition(null));
                }
  break;
case 213:
					// line 858 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<", ((Node)yyVals[0+yyTop]), getPosition(null));
                }
  break;
case 214:
					// line 861 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=", ((Node)yyVals[0+yyTop]), getPosition(null));
                }
  break;
case 215:
					// line 864 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop]), getPosition(null));
                }
  break;
case 216:
					// line 867 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "===", ((Node)yyVals[0+yyTop]), getPosition(null));
                }
  break;
case 217:
					// line 870 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "!=", ((Node)yyVals[0+yyTop]), getPosition(null));
                }
  break;
case 218:
					// line 873 "Ruby19Parser.y"
  {
                    yyVal = support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                  /* ENEBO
                        $$ = match_op($1, $3);
                        if (nd_type($1) == NODE_LIT && TYPE($1->nd_lit) == T_REGEXP) {
                            $$ = reg_named_capture_assign($1->nd_lit, $$);
                        }
                  */
                }
  break;
case 219:
					// line 882 "Ruby19Parser.y"
  {
                    yyVal = new NotNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
                }
  break;
case 220:
					// line 885 "Ruby19Parser.y"
  {
                    yyVal = new NotNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
                }
  break;
case 221:
					// line 888 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "~");
                }
  break;
case 222:
					// line 891 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<<", ((Node)yyVals[0+yyTop]), getPosition(null));
                }
  break;
case 223:
					// line 894 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">>", ((Node)yyVals[0+yyTop]), getPosition(null));
                }
  break;
case 224:
					// line 897 "Ruby19Parser.y"
  {
                    yyVal = support.newAndNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 225:
					// line 900 "Ruby19Parser.y"
  {
                    yyVal = support.newOrNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 226:
					// line 903 "Ruby19Parser.y"
  {
                    /* ENEBO: arg surrounded by in_defined set/unset*/
                    yyVal = new DefinedNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop]));
                }
  break;
case 227:
					// line 907 "Ruby19Parser.y"
  {
                    yyVal = new IfNode(getPosition(((Node)yyVals[-5+yyTop])), support.getConditionNode(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 228:
					// line 910 "Ruby19Parser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 229:
					// line 914 "Ruby19Parser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = ((Node)yyVals[0+yyTop]) != null ? ((Node)yyVals[0+yyTop]) : NilImplicitNode.NIL;
                }
  break;
case 231:
					// line 920 "Ruby19Parser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 232:
					// line 923 "Ruby19Parser.y"
  {
                    yyVal = support.arg_append(((Node)yyVals[-3+yyTop]), new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                }
  break;
case 233:
					// line 926 "Ruby19Parser.y"
  {
                    yyVal = support.newArrayNode(getPosition(((ListNode)yyVals[-1+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                }
  break;
case 234:
					// line 930 "Ruby19Parser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                    if (yyVal != null) ((Node)yyVal).setPosition(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
                }
  break;
case 239:
					// line 939 "Ruby19Parser.y"
  {
                    yyVal = support.newArrayNode(getPosition(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
                }
  break;
case 240:
					// line 942 "Ruby19Parser.y"
  {
                    yyVal = support.arg_blk_pass(((Node)yyVals[-1+yyTop]), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 241:
					// line 945 "Ruby19Parser.y"
  {
                    yyVal = support.newArrayNode(getPosition(((ListNode)yyVals[-1+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 242:
					// line 949 "Ruby19Parser.y"
  {
                    yyVal = support.arg_append(((Node)yyVals[-3+yyTop]), new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 243:
					// line 953 "Ruby19Parser.y"
  {}
  break;
case 244:
					// line 955 "Ruby19Parser.y"
  {
                    yyVal = new Long(lexer.getCmdArgumentState().begin());
                }
  break;
case 245:
					// line 957 "Ruby19Parser.y"
  {
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 246:
					// line 962 "Ruby19Parser.y"
  {
                    yyVal = new BlockPassNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
                }
  break;
case 247:
					// line 966 "Ruby19Parser.y"
  {
                    yyVal = ((BlockPassNode)yyVals[0+yyTop]);
                }
  break;
case 249:
					// line 971 "Ruby19Parser.y"
  {
                    yyVal = support.newArrayNode(getPosition2(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
                }
  break;
case 250:
					// line 974 "Ruby19Parser.y"
  {
/* Newline this*/
                    yyVal = support.newSplatNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
                }
  break;
case 251:
					// line 978 "Ruby19Parser.y"
  {
                    Node node = support.splat_array(((Node)yyVals[-2+yyTop]));

                    if (node != null) {
                        yyVal = support.list_append(node, ((Node)yyVals[0+yyTop]));
                    } else {
                        yyVal = support.arg_append(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    }
                }
  break;
case 252:
					// line 987 "Ruby19Parser.y"
  {
                    Node node = null;

                    /* FIXME: lose syntactical elements here (and others like this)*/
                    if (((Node)yyVals[0+yyTop]) instanceof ArrayNode &&
                        (node = support.splat_array(((Node)yyVals[-3+yyTop]))) != null) {
                        yyVal = support.list_concat(node, ((Node)yyVals[0+yyTop]));
                    } else {
                        yyVal = support.arg_concat(support.union(((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
                    }
                }
  break;
case 253:
					// line 999 "Ruby19Parser.y"
  {
                    Node node = support.splat_array(((Node)yyVals[-2+yyTop]));

                    if (node != null) {
                        yyVal = support.list_append(node, ((Node)yyVals[0+yyTop]));
                    } else {
                        yyVal = support.arg_append(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    }
                }
  break;
case 254:
					// line 1008 "Ruby19Parser.y"
  {
                    Node node = null;

                    if (((Node)yyVals[0+yyTop]) instanceof ArrayNode &&
                        (node = support.splat_array(((Node)yyVals[-3+yyTop]))) != null) {
                        yyVal = support.list_concat(node, ((Node)yyVals[0+yyTop]));
                    } else {
                        yyVal = support.arg_concat(support.union(((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
                    }
                }
  break;
case 255:
					// line 1018 "Ruby19Parser.y"
  {
                     yyVal = support.newSplatNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));  
                }
  break;
case 264:
					// line 1030 "Ruby19Parser.y"
  {
                    yyVal = new FCallNoArgNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 265:
					// line 1033 "Ruby19Parser.y"
  {
                    yyVal = new BeginNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]));
                }
  break;
case 266:
					// line 1036 "Ruby19Parser.y"
  {
                    lexer.setState(LexState.EXPR_ENDARG); 
                }
  break;
case 267:
					// line 1038 "Ruby19Parser.y"
  {
                    if (warnings.isVerbose()) warnings.warning(ID.GROUPED_EXPRESSION, getPosition(((Token)yyVals[-3+yyTop])), "(...) interpreted as grouped expression");
                    yyVal = ((Node)yyVals[-2+yyTop]);
                }
  break;
case 268:
					// line 1042 "Ruby19Parser.y"
  {
                    if (((Node)yyVals[-1+yyTop]) != null) {
                        /* compstmt position includes both parens around it*/
                        ((ISourcePositionHolder) ((Node)yyVals[-1+yyTop])).setPosition(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
                    }
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 269:
					// line 1049 "Ruby19Parser.y"
  {
                    yyVal = support.new_colon2(support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 270:
					// line 1052 "Ruby19Parser.y"
  {
                    yyVal = support.new_colon3(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 271:
					// line 1055 "Ruby19Parser.y"
  {
                    ISourcePosition position = support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop]));
                    if (((Node)yyVals[-1+yyTop]) == null) {
                        yyVal = new ZArrayNode(position); /* zero length array */
                    } else {
                        yyVal = ((Node)yyVals[-1+yyTop]);
                        ((ISourcePositionHolder)yyVal).setPosition(position);
                    }
                }
  break;
case 272:
					// line 1064 "Ruby19Parser.y"
  {
                    yyVal = new HashNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((ListNode)yyVals[-1+yyTop]));
                }
  break;
case 273:
					// line 1067 "Ruby19Parser.y"
  {
                    yyVal = new ReturnNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
                }
  break;
case 274:
					// line 1070 "Ruby19Parser.y"
  {
                    yyVal = support.new_yield(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 275:
					// line 1073 "Ruby19Parser.y"
  {
                    yyVal = new YieldNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), null, false);
                }
  break;
case 276:
					// line 1076 "Ruby19Parser.y"
  {
                    yyVal = new YieldNode(((Token)yyVals[0+yyTop]).getPosition(), null, false);
                }
  break;
case 277:
					// line 1079 "Ruby19Parser.y"
  {
                    yyVal = new DefinedNode(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 278:
					// line 1082 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(support.getConditionNode(((Node)yyVals[-1+yyTop])), "!");
                }
  break;
case 279:
					// line 1085 "Ruby19Parser.y"
  {
                    yyVal = support.getOperatorCallNode(NilImplicitNode.NIL, "!");
                }
  break;
case 280:
					// line 1088 "Ruby19Parser.y"
  {
                    yyVal = new FCallNoArgBlockNode(support.union(((Token)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((IterNode)yyVals[0+yyTop]));
                }
  break;
case 282:
					// line 1092 "Ruby19Parser.y"
  {
                    if (((Node)yyVals[-1+yyTop]) != null && 
                          ((BlockAcceptingNode)yyVals[-1+yyTop]).getIterNode() instanceof BlockPassNode) {
                        throw new SyntaxException(PID.BLOCK_ARG_AND_BLOCK_GIVEN, getPosition(((Node)yyVals[-1+yyTop])), "Both block arg and actual block given.");
                    }
                    yyVal = ((BlockAcceptingNode)yyVals[-1+yyTop]).setIterNode(((IterNode)yyVals[0+yyTop]));
                    ((Node)yyVal).setPosition(support.union(((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])));
                }
  break;
case 283:
					// line 1100 "Ruby19Parser.y"
  {
                    yyVal = ((LambdaNode)yyVals[0+yyTop]);
                }
  break;
case 284:
					// line 1103 "Ruby19Parser.y"
  {
                    yyVal = new IfNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 285:
					// line 1106 "Ruby19Parser.y"
  {
                    yyVal = new IfNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-2+yyTop]));
                }
  break;
case 286:
					// line 1109 "Ruby19Parser.y"
  {
                    lexer.getConditionState().begin();
                }
  break;
case 287:
					// line 1111 "Ruby19Parser.y"
  {
                    lexer.getConditionState().end();
                }
  break;
case 288:
					// line 1113 "Ruby19Parser.y"
  {
                    Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);
                    yyVal = new WhileNode(support.union(((Token)yyVals[-6+yyTop]), ((Token)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), body);
                }
  break;
case 289:
					// line 1117 "Ruby19Parser.y"
  {
                  lexer.getConditionState().begin();
                }
  break;
case 290:
					// line 1119 "Ruby19Parser.y"
  {
                  lexer.getConditionState().end();
                }
  break;
case 291:
					// line 1121 "Ruby19Parser.y"
  {
                    Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);
                    yyVal = new UntilNode(getPosition(((Token)yyVals[-6+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), body);
                }
  break;
case 292:
					// line 1125 "Ruby19Parser.y"
  {
                    yyVal = new CaseNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 293:
					// line 1128 "Ruby19Parser.y"
  {
                    yyVal = new CaseNode(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])), null, ((Node)yyVals[-1+yyTop]));
                }
  break;
case 294:
					// line 1131 "Ruby19Parser.y"
  {
                    lexer.getConditionState().begin();
                }
  break;
case 295:
					// line 1133 "Ruby19Parser.y"
  {
                    lexer.getConditionState().end();
                }
  break;
case 296:
					// line 1135 "Ruby19Parser.y"
  {
                      /* ENEBO: Lots of optz in 1.9 parser here*/
                    yyVal = new ForNode(support.union(((Token)yyVals[-8+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-7+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-4+yyTop]));
                }
  break;
case 297:
					// line 1139 "Ruby19Parser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("class definition in method body");
                    }
                    support.pushLocalScope();
                }
  break;
case 298:
					// line 1144 "Ruby19Parser.y"
  {
                    Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);

                    yyVal = new ClassNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), ((Colon3Node)yyVals[-4+yyTop]), support.getCurrentScope(), body, ((Node)yyVals[-3+yyTop]));
                    support.popCurrentScope();
                }
  break;
case 299:
					// line 1150 "Ruby19Parser.y"
  {
                    yyVal = new Boolean(support.isInDef());
                    support.setInDef(false);
                }
  break;
case 300:
					// line 1153 "Ruby19Parser.y"
  {
                    yyVal = new Integer(support.getInSingle());
                    support.setInSingle(0);
                    support.pushLocalScope();
                }
  break;
case 301:
					// line 1157 "Ruby19Parser.y"
  {
                    yyVal = new SClassNode(support.union(((Token)yyVals[-7+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-5+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                    support.popCurrentScope();
                    support.setInDef(((Boolean)yyVals[-4+yyTop]).booleanValue());
                    support.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
                }
  break;
case 302:
					// line 1163 "Ruby19Parser.y"
  {
                    if (support.isInDef() || support.isInSingle()) { 
                        yyerror("module definition in method body");
                    }
                    support.pushLocalScope();
                }
  break;
case 303:
					// line 1168 "Ruby19Parser.y"
  {
                    Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);

                    yyVal = new ModuleNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Colon3Node)yyVals[-3+yyTop]), support.getCurrentScope(), body);
                    support.popCurrentScope();
                }
  break;
case 304:
					// line 1174 "Ruby19Parser.y"
  {
                    support.setInDef(true);
                    support.pushLocalScope();
                }
  break;
case 305:
					// line 1177 "Ruby19Parser.y"
  {
                    /* TODO: We should use implicit nil for body, but problem (punt til later)*/
                    Node body = ((Node)yyVals[-1+yyTop]); /*$5 == null ? NilImplicitNode.NIL : $5;*/

                    yyVal = new DefnNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), new ArgumentNode(((Token)yyVals[-4+yyTop]).getPosition(), (String) ((Token)yyVals[-4+yyTop]).getValue()), ((ArgsNode)yyVals[-2+yyTop]), support.getCurrentScope(), body);
                    support.popCurrentScope();
                    support.setInDef(false);
                }
  break;
case 306:
					// line 1185 "Ruby19Parser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
                }
  break;
case 307:
					// line 1187 "Ruby19Parser.y"
  {
                    support.setInSingle(support.getInSingle() + 1);
                    support.pushLocalScope();
                    lexer.setState(LexState.EXPR_END); /* force for args */
                }
  break;
case 308:
					// line 1191 "Ruby19Parser.y"
  {
                    /* TODO: We should use implicit nil for body, but problem (punt til later)*/
                    Node body = ((Node)yyVals[-1+yyTop]); /*$8 == null ? NilImplicitNode.NIL : $8;*/

                    yyVal = new DefsNode(support.union(((Token)yyVals[-8+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-7+yyTop]), new ArgumentNode(((Token)yyVals[-4+yyTop]).getPosition(), (String) ((Token)yyVals[-4+yyTop]).getValue()), ((ArgsNode)yyVals[-2+yyTop]), support.getCurrentScope(), body);
                    support.popCurrentScope();
                    support.setInSingle(support.getInSingle() - 1);
                }
  break;
case 309:
					// line 1199 "Ruby19Parser.y"
  {
                    yyVal = new BreakNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
                }
  break;
case 310:
					// line 1202 "Ruby19Parser.y"
  {
                    yyVal = new NextNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
                }
  break;
case 311:
					// line 1205 "Ruby19Parser.y"
  {
                    yyVal = new RedoNode(((Token)yyVals[0+yyTop]).getPosition());
                }
  break;
case 312:
					// line 1208 "Ruby19Parser.y"
  {
                    yyVal = new RetryNode(((Token)yyVals[0+yyTop]).getPosition());
                }
  break;
case 313:
					// line 1212 "Ruby19Parser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = ((Node)yyVals[0+yyTop]);
                    if (yyVal == null) yyVal = NilImplicitNode.NIL;
                }
  break;
case 320:
					// line 1226 "Ruby19Parser.y"
  {
                    yyVal = new IfNode(getPosition(((Token)yyVals[-4+yyTop])), support.getConditionNode(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 322:
					// line 1231 "Ruby19Parser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 324:
					// line 1236 "Ruby19Parser.y"
  {}
  break;
case 325:
					// line 1238 "Ruby19Parser.y"
  {
                     yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
                }
  break;
case 326:
					// line 1241 "Ruby19Parser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 327:
					// line 1245 "Ruby19Parser.y"
  {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 328:
					// line 1248 "Ruby19Parser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 329:
					// line 1252 "Ruby19Parser.y"
  {
                    yyVal = new MultipleAsgn19Node(getPosition(((ListNode)yyVals[0+yyTop])), ((ListNode)yyVals[0+yyTop]), null, null);
                }
  break;
case 330:
					// line 1255 "Ruby19Parser.y"
  {
                    yyVal = new MultipleAsgn19Node(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), support.assignable(((Token)yyVals[0+yyTop]), null), null);
                }
  break;
case 331:
					// line 1258 "Ruby19Parser.y"
  {
                    yyVal = new MultipleAsgn19Node(getPosition(((ListNode)yyVals[-5+yyTop])), ((ListNode)yyVals[-5+yyTop]), support.assignable(((Token)yyVals[-2+yyTop]), null), ((ListNode)yyVals[0+yyTop]));
                }
  break;
case 332:
					// line 1261 "Ruby19Parser.y"
  {
                    yyVal = new MultipleAsgn19Node(getPosition(((ListNode)yyVals[-2+yyTop])), ((ListNode)yyVals[-2+yyTop]), new StarNode(getPosition(null)), null);
                }
  break;
case 333:
					// line 1264 "Ruby19Parser.y"
  {
                    yyVal = new MultipleAsgn19Node(getPosition(((ListNode)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), new StarNode(getPosition(null)), ((ListNode)yyVals[0+yyTop]));
                }
  break;
case 334:
					// line 1267 "Ruby19Parser.y"
  {
                    yyVal = new MultipleAsgn19Node(getPosition(((Token)yyVals[-1+yyTop])), null, support.assignable(((Token)yyVals[0+yyTop]), null), null);
                }
  break;
case 335:
					// line 1270 "Ruby19Parser.y"
  {
                    yyVal = new MultipleAsgn19Node(getPosition(((Token)yyVals[-3+yyTop])), null, support.assignable(((Token)yyVals[-2+yyTop]), null), ((ListNode)yyVals[0+yyTop]));
                }
  break;
case 336:
					// line 1273 "Ruby19Parser.y"
  {
                    yyVal = new MultipleAsgn19Node(getPosition(((Token)yyVals[0+yyTop])), null, new StarNode(getPosition(null)), null);
                }
  break;
case 337:
					// line 1276 "Ruby19Parser.y"
  {
                    yyVal = new MultipleAsgn19Node(getPosition(((Token)yyVals[-2+yyTop])), null, null, ((ListNode)yyVals[0+yyTop]));
                }
  break;
case 338:
					// line 1280 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((ListNode)yyVals[-5+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 339:
					// line 1283 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((ListNode)yyVals[-7+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), ((ListNode)yyVals[-7+yyTop]), ((ListNode)yyVals[-5+yyTop]), ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 340:
					// line 1286 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((ListNode)yyVals[-3+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), null, null, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 341:
					// line 1289 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((ListNode)yyVals[-5+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), null, ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 342:
					// line 1292 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((ListNode)yyVals[-3+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), ((ListNode)yyVals[-3+yyTop]), null, ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 343:
					// line 1295 "Ruby19Parser.y"
  {
    /* FIXME, weird unnamed rest*/
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, null, null, null);
                }
  break;
case 344:
					// line 1299 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((ListNode)yyVals[-5+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), ((ListNode)yyVals[-5+yyTop]), null, ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 345:
					// line 1302 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), ((ListNode)yyVals[-1+yyTop]), null, null, null, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 346:
					// line 1305 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((ListNode)yyVals[-3+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), null, ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 347:
					// line 1308 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((ListNode)yyVals[-5+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), null, ((ListNode)yyVals[-5+yyTop]), ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 348:
					// line 1311 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), null, ((ListNode)yyVals[-1+yyTop]), null, null, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 349:
					// line 1314 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((ListNode)yyVals[-3+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), null, ((ListNode)yyVals[-3+yyTop]), null, ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 350:
					// line 1317 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((RestArgNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), null, null, ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 351:
					// line 1320 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((RestArgNode)yyVals[-3+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), null, null, ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 352:
					// line 1323 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(getPosition(((BlockArgNode)yyVals[0+yyTop])), null, null, null, null, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 353:
					// line 1327 "Ruby19Parser.y"
  {
    /* was $$ = null;*/
                   yyVal = support.new_args(getPosition(null), null, null, null, null, null);
                }
  break;
case 354:
					// line 1331 "Ruby19Parser.y"
  {
                    lexer.commandStart = true;
                    yyVal = ((ArgsNode)yyVals[0+yyTop]);
                }
  break;
case 355:
					// line 1336 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(getPosition(null), null, null, null, null, null);
                }
  break;
case 356:
					// line 1339 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(getPosition(null), null, null, null, null, null);
                }
  break;
case 357:
					// line 1342 "Ruby19Parser.y"
  {
                    yyVal = ((ArgsNode)yyVals[-2+yyTop]);
                }
  break;
case 359:
					// line 1348 "Ruby19Parser.y"
  {
                    yyVal = null;
                }
  break;
case 360:
					// line 1353 "Ruby19Parser.y"
  {
                    yyVal = null;
                }
  break;
case 361:
					// line 1356 "Ruby19Parser.y"
  {
                    yyVal = null;
                }
  break;
case 362:
					// line 1360 "Ruby19Parser.y"
  {
                    support.new_bv(((Token)yyVals[0+yyTop]));
                }
  break;
case 363:
					// line 1363 "Ruby19Parser.y"
  {
                    yyVal = null;
                }
  break;
case 364:
					// line 1367 "Ruby19Parser.y"
  {
                    support.pushBlockScope();
                    yyVal = lexer.getLeftParenBegin();
                    lexer.setLeftParenBegin(lexer.incrementParenNest());
                }
  break;
case 365:
					// line 1371 "Ruby19Parser.y"
  {
                    yyVal = new LambdaNode(support.union(((ArgsNode)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), ((ArgsNode)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
                    lexer.setLeftParenBegin(((Integer)yyVals[-2+yyTop]));
                }
  break;
case 366:
					// line 1377 "Ruby19Parser.y"
  {
                    yyVal = ((ArgsNode)yyVals[-2+yyTop]);
                    ((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop])));
                }
  break;
case 367:
					// line 1381 "Ruby19Parser.y"
  {
                    yyVal = ((ArgsNode)yyVals[-1+yyTop]);
                    ((ISourcePositionHolder)yyVal).setPosition(support.union(((ArgsNode)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])));
                }
  break;
case 368:
					// line 1386 "Ruby19Parser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 369:
					// line 1389 "Ruby19Parser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 370:
					// line 1393 "Ruby19Parser.y"
  {
                    support.pushBlockScope();
                }
  break;
case 371:
					// line 1395 "Ruby19Parser.y"
  {
                    yyVal = new IterNode(getPosition(((Token)yyVals[-4+yyTop])), ((ArgsNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
                }
  break;
case 372:
					// line 1400 "Ruby19Parser.y"
  {
                    if (((BlockAcceptingNode)yyVals[-1+yyTop]).getIterNode() instanceof BlockPassNode) {
                        throw new SyntaxException(PID.BLOCK_ARG_AND_BLOCK_GIVEN, getPosition(((Node)yyVals[-1+yyTop])), "Both block arg and actual block given.");
                    }
                    yyVal = ((BlockAcceptingNode)yyVals[-1+yyTop]).setIterNode(((IterNode)yyVals[0+yyTop]));
                    ((Node)yyVal).setPosition(support.union(((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])));
                }
  break;
case 373:
					// line 1407 "Ruby19Parser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
                }
  break;
case 374:
					// line 1410 "Ruby19Parser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
                }
  break;
case 375:
					// line 1414 "Ruby19Parser.y"
  {
                    yyVal = support.new_fcall(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
                }
  break;
case 376:
					// line 1417 "Ruby19Parser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
                }
  break;
case 377:
					// line 1420 "Ruby19Parser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
                }
  break;
case 378:
					// line 1423 "Ruby19Parser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop]), null, null);
                }
  break;
case 379:
					// line 1426 "Ruby19Parser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop]), new Token("call", ((Node)yyVals[-2+yyTop]).getPosition()), ((Node)yyVals[0+yyTop]), null);
                }
  break;
case 380:
					// line 1429 "Ruby19Parser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop]), new Token("call", ((Node)yyVals[-2+yyTop]).getPosition()), ((Node)yyVals[0+yyTop]), null);
                }
  break;
case 381:
					// line 1432 "Ruby19Parser.y"
  {
                    yyVal = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop]));
                }
  break;
case 382:
					// line 1435 "Ruby19Parser.y"
  {
                    yyVal = new ZSuperNode(((Token)yyVals[0+yyTop]).getPosition());
                }
  break;
case 383:
					// line 1438 "Ruby19Parser.y"
  {
                    if (((Node)yyVals[-3+yyTop]) instanceof SelfNode) {
                        yyVal = support.new_fcall(new Token("[]", support.union(((Node)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop]))), ((Node)yyVals[-1+yyTop]), null);
                    } else {
                        yyVal = support.new_call(((Node)yyVals[-3+yyTop]), new Token("[]", support.union(((Node)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop]))), ((Node)yyVals[-1+yyTop]), null);
                    }
                }
  break;
case 384:
					// line 1446 "Ruby19Parser.y"
  {
                    support.pushBlockScope();
                }
  break;
case 385:
					// line 1448 "Ruby19Parser.y"
  {
                    yyVal = new IterNode(getPosition(((Token)yyVals[-4+yyTop])), ((ArgsNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
                }
  break;
case 386:
					// line 1452 "Ruby19Parser.y"
  {
                    support.pushBlockScope();
                }
  break;
case 387:
					// line 1454 "Ruby19Parser.y"
  {
                    yyVal = new IterNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((ArgsNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), support.getCurrentScope());
                    ((ISourcePositionHolder)yyVals[-5+yyTop]).setPosition(support.union(((ISourcePositionHolder)yyVals[-5+yyTop]), ((ISourcePositionHolder)yyVal)));
                    support.popCurrentScope();
                }
  break;
case 388:
					// line 1460 "Ruby19Parser.y"
  {
                    yyVal = support.newWhenNode(support.union(((Token)yyVals[-4+yyTop]), support.unwrapNewlineNode(((Node)yyVals[-1+yyTop]))), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 391:
					// line 1466 "Ruby19Parser.y"
  {
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
                    yyVal = new RescueBodyNode(getPosition(((Token)yyVals[-5+yyTop]), true), ((Node)yyVals[-4+yyTop]), body, ((RescueBodyNode)yyVals[0+yyTop]));
                }
  break;
case 392:
					// line 1479 "Ruby19Parser.y"
  { yyVal = null; }
  break;
case 393:
					// line 1481 "Ruby19Parser.y"
  {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 394:
					// line 1484 "Ruby19Parser.y"
  {
                    yyVal = support.splat_array(((Node)yyVals[0+yyTop]));
                    if (yyVal == null) yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 396:
					// line 1490 "Ruby19Parser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 398:
					// line 1495 "Ruby19Parser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 401:
					// line 1501 "Ruby19Parser.y"
  {
                    /* FIXME: We may be intern'ing more than once.*/
                    yyVal = new SymbolNode(((Token)yyVals[0+yyTop]).getPosition(), ((String) ((Token)yyVals[0+yyTop]).getValue()).intern());
                }
  break;
case 403:
					// line 1507 "Ruby19Parser.y"
  {
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
                }
  break;
case 404:
					// line 1520 "Ruby19Parser.y"
  {
                    yyVal = new StrNode(((Token)yyVals[-1+yyTop]).getPosition(), ByteList.create((String) ((Token)yyVals[0+yyTop]).getValue()));
                }
  break;
case 405:
					// line 1523 "Ruby19Parser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 406:
					// line 1526 "Ruby19Parser.y"
  {
                    yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 407:
					// line 1530 "Ruby19Parser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);

                    ((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
                    int extraLength = ((String) ((Token)yyVals[-2+yyTop]).getValue()).length() - 1;

                    /* We may need to subtract addition offset off of first */
                    /* string fragment (we optimistically take one off in*/
                    /* ParserSupport.literal_concat).  Check token length*/
                    /* and subtract as neeeded.*/
                    if ((((Node)yyVals[-1+yyTop]) instanceof DStrNode) && extraLength > 0) {
                      Node strNode = ((DStrNode)((Node)yyVals[-1+yyTop])).get(0);
                      assert strNode != null;
                      strNode.getPosition().adjustStartOffset(-extraLength);
                    }
                }
  break;
case 408:
					// line 1547 "Ruby19Parser.y"
  {
                    ISourcePosition position = support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop]));

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
                }
  break;
case 409:
					// line 1563 "Ruby19Parser.y"
  {
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
                }
  break;
case 410:
					// line 1578 "Ruby19Parser.y"
  {
                    yyVal = new ZArrayNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
                }
  break;
case 411:
					// line 1581 "Ruby19Parser.y"
  {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                }
  break;
case 412:
					// line 1585 "Ruby19Parser.y"
  {
                    yyVal = new ArrayNode(getPosition(null));
                }
  break;
case 413:
					// line 1588 "Ruby19Parser.y"
  {
                     yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]) instanceof EvStrNode ? new DStrNode(getPosition(((ListNode)yyVals[-2+yyTop]))).add(((Node)yyVals[-1+yyTop])) : ((Node)yyVals[-1+yyTop]));
                }
  break;
case 415:
					// line 1593 "Ruby19Parser.y"
  {
                     yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 416:
					// line 1597 "Ruby19Parser.y"
  {
                     yyVal = new ZArrayNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
                }
  break;
case 417:
					// line 1600 "Ruby19Parser.y"
  {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                    ((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
                }
  break;
case 418:
					// line 1605 "Ruby19Parser.y"
  {
                    yyVal = new ArrayNode(getPosition(null));
                }
  break;
case 419:
					// line 1608 "Ruby19Parser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
                }
  break;
case 420:
					// line 1612 "Ruby19Parser.y"
  {
                    yyVal = new StrNode(((Token)yyVals[0+yyTop]).getPosition(), ByteList.create(""));
                }
  break;
case 421:
					// line 1615 "Ruby19Parser.y"
  {
                    yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 422:
					// line 1619 "Ruby19Parser.y"
  {
                    yyVal = null;
                }
  break;
case 423:
					// line 1622 "Ruby19Parser.y"
  {
                    yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 424:
					// line 1626 "Ruby19Parser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 425:
					// line 1629 "Ruby19Parser.y"
  {
                    yyVal = lexer.getStrTerm();
                    lexer.setStrTerm(null);
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 426:
					// line 1633 "Ruby19Parser.y"
  {
                    lexer.setStrTerm(((StrTerm)yyVals[-1+yyTop]));
                    yyVal = new EvStrNode(support.union(((Token)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
                }
  break;
case 427:
					// line 1637 "Ruby19Parser.y"
  {
                   yyVal = lexer.getStrTerm();
                   lexer.setStrTerm(null);
                   lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 428:
					// line 1641 "Ruby19Parser.y"
  {
                   lexer.setStrTerm(((StrTerm)yyVals[-2+yyTop]));

                   yyVal = support.newEvStrNode(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 429:
					// line 1647 "Ruby19Parser.y"
  {
                     yyVal = new GlobalVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 430:
					// line 1650 "Ruby19Parser.y"
  {
                     yyVal = new InstVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 431:
					// line 1653 "Ruby19Parser.y"
  {
                     yyVal = new ClassVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 433:
					// line 1659 "Ruby19Parser.y"
  {
                     lexer.setState(LexState.EXPR_END);
                     yyVal = ((Token)yyVals[0+yyTop]);
                     ((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])));
                }
  break;
case 438:
					// line 1668 "Ruby19Parser.y"
  {
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
                         yyVal = new DSymbolNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((DStrNode)yyVals[-1+yyTop]));
                     } else {
                       ISourcePosition position = support.union(((Node)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop]));

                       /* We substract one since tsymbeg is longer than one*/
                       /* and we cannot union it directly so we assume quote*/
                       /* is one character long and subtract for it.*/
                       position.adjustStartOffset(-1);
                       ((Node)yyVals[-1+yyTop]).setPosition(position);

                       yyVal = new DSymbolNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
                       ((DSymbolNode)yyVal).add(((Node)yyVals[-1+yyTop]));
                     }
                }
  break;
case 439:
					// line 1700 "Ruby19Parser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 440:
					// line 1703 "Ruby19Parser.y"
  {
                     yyVal = ((FloatNode)yyVals[0+yyTop]);
                }
  break;
case 441:
					// line 1706 "Ruby19Parser.y"
  {
                     yyVal = support.negateInteger(((Node)yyVals[0+yyTop]));
                }
  break;
case 442:
					// line 1709 "Ruby19Parser.y"
  {
                     yyVal = support.negateFloat(((FloatNode)yyVals[0+yyTop]));
                }
  break;
case 448:
					// line 1714 "Ruby19Parser.y"
  { 
                    yyVal = new Token("nil", Tokens.kNIL, ((Token)yyVals[0+yyTop]).getPosition());
                }
  break;
case 449:
					// line 1717 "Ruby19Parser.y"
  {
                    yyVal = new Token("self", Tokens.kSELF, ((Token)yyVals[0+yyTop]).getPosition());
                }
  break;
case 450:
					// line 1720 "Ruby19Parser.y"
  { 
                    yyVal = new Token("true", Tokens.kTRUE, ((Token)yyVals[0+yyTop]).getPosition());
                }
  break;
case 451:
					// line 1723 "Ruby19Parser.y"
  {
                    yyVal = new Token("false", Tokens.kFALSE, ((Token)yyVals[0+yyTop]).getPosition());
                }
  break;
case 452:
					// line 1726 "Ruby19Parser.y"
  {
                    yyVal = new Token("__FILE__", Tokens.k__FILE__, ((Token)yyVals[0+yyTop]).getPosition());
                }
  break;
case 453:
					// line 1729 "Ruby19Parser.y"
  {
                    yyVal = new Token("__LINE__", Tokens.k__LINE__, ((Token)yyVals[0+yyTop]).getPosition());
                }
  break;
case 454:
					// line 1732 "Ruby19Parser.y"
  {
                    yyVal = new Token("__ENCODING__", Tokens.k__LINE__, ((Token)yyVals[0+yyTop]).getPosition());
                }
  break;
case 455:
					// line 1736 "Ruby19Parser.y"
  {
                    yyVal = support.gettable(((Token)yyVals[0+yyTop]));
                }
  break;
case 456:
					// line 1740 "Ruby19Parser.y"
  {
                    yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
                }
  break;
case 457:
					// line 1744 "Ruby19Parser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 458:
					// line 1747 "Ruby19Parser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 459:
					// line 1751 "Ruby19Parser.y"
  {
                    yyVal = null;
                }
  break;
case 460:
					// line 1754 "Ruby19Parser.y"
  {
                   lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 461:
					// line 1756 "Ruby19Parser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 462:
					// line 1759 "Ruby19Parser.y"
  {
                   yyerrok();
                   yyVal = null;
                }
  break;
case 463:
					// line 1765 "Ruby19Parser.y"
  {
                    yyVal = ((ArgsNode)yyVals[-1+yyTop]);
                    ((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 464:
					// line 1770 "Ruby19Parser.y"
  {
                    yyVal = ((ArgsNode)yyVals[-1+yyTop]);
                }
  break;
case 465:
					// line 1774 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((ListNode)yyVals[-5+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 466:
					// line 1777 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((ListNode)yyVals[-7+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), ((ListNode)yyVals[-7+yyTop]), ((ListNode)yyVals[-5+yyTop]), ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 467:
					// line 1780 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((ListNode)yyVals[-3+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), null, null, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 468:
					// line 1783 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((ListNode)yyVals[-5+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), null, ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 469:
					// line 1786 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((ListNode)yyVals[-3+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), ((ListNode)yyVals[-3+yyTop]), null, ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 470:
					// line 1789 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((ListNode)yyVals[-5+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), ((ListNode)yyVals[-5+yyTop]), null, ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 471:
					// line 1792 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), ((ListNode)yyVals[-1+yyTop]), null, null, null, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 472:
					// line 1795 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((ListNode)yyVals[-3+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), null, ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 473:
					// line 1798 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((ListNode)yyVals[-5+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), null, ((ListNode)yyVals[-5+yyTop]), ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 474:
					// line 1801 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), null, ((ListNode)yyVals[-1+yyTop]), null, null, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 475:
					// line 1804 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((ListNode)yyVals[-3+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), null, ((ListNode)yyVals[-3+yyTop]), null, ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 476:
					// line 1807 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((RestArgNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), null, null, ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 477:
					// line 1810 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(support.union(((RestArgNode)yyVals[-3+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), null, null, ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 478:
					// line 1813 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(((BlockArgNode)yyVals[0+yyTop]).getPosition(), null, null, null, null, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 479:
					// line 1816 "Ruby19Parser.y"
  {
                    yyVal = support.new_args(getPosition(null), null, null, null, null, null);
                }
  break;
case 480:
					// line 1820 "Ruby19Parser.y"
  {
                    yyerror("formal argument cannot be a constant");
                }
  break;
case 481:
					// line 1823 "Ruby19Parser.y"
  {
                    yyerror("formal argument cannot be an instance variable");
                }
  break;
case 482:
					// line 1826 "Ruby19Parser.y"
  {
                    yyerror("formal argument cannot be a global variable");
                }
  break;
case 483:
					// line 1829 "Ruby19Parser.y"
  {
                    yyerror("formal argument cannot be a class variable");
                }
  break;
case 485:
					// line 1835 "Ruby19Parser.y"
  {
    /* FIXME: Resolve what the hell is going on*/
    /*                    if (support.is_local_id($1)) {
                        yyerror("formal argument must be local variable");
                        }*/
                     
                    support.shadowing_lvar(((Token)yyVals[0+yyTop]));
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 486:
					// line 1845 "Ruby19Parser.y"
  {
                    support.arg_var(((Token)yyVals[0+yyTop]));
                    yyVal = new ArgumentNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
  /*
                    $$ = new ArgAuxiliaryNode($1.getPosition(), (String) $1.getValue(), 1);
  */
                }
  break;
case 487:
					// line 1852 "Ruby19Parser.y"
  {
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
                }
  break;
case 488:
					// line 1867 "Ruby19Parser.y"
  {
                    yyVal = new ArrayNode(getPosition(null), ((Node)yyVals[0+yyTop]));
                }
  break;
case 489:
					// line 1870 "Ruby19Parser.y"
  {
                    ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                    yyVal = ((ListNode)yyVals[-2+yyTop]);
                }
  break;
case 490:
					// line 1875 "Ruby19Parser.y"
  {
                    if (!support.is_local_id(((Token)yyVals[-2+yyTop]))) {
                        yyerror("formal argument must be local variable");
                    }
                    support.shadowing_lvar(((Token)yyVals[-2+yyTop]));
                    support.arg_var(((Token)yyVals[-2+yyTop]));
                    yyVal = new OptArgNode(getPosition(((Token)yyVals[-2+yyTop])), support.assignable(((Token)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
                }
  break;
case 491:
					// line 1884 "Ruby19Parser.y"
  {
                    if (!support.is_local_id(((Token)yyVals[-2+yyTop]))) {
                        yyerror("formal argument must be local variable");
                    }
                    support.shadowing_lvar(((Token)yyVals[-2+yyTop]));
                    support.arg_var(((Token)yyVals[-2+yyTop]));
                    yyVal = new OptArgNode(getPosition(((Token)yyVals[-2+yyTop])), support.assignable(((Token)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
                }
  break;
case 492:
					// line 1893 "Ruby19Parser.y"
  {
                    yyVal = new BlockNode(getPosition(((Node)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 493:
					// line 1896 "Ruby19Parser.y"
  {
                    yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 494:
					// line 1900 "Ruby19Parser.y"
  {
                    yyVal = new BlockNode(getPosition(((Node)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 495:
					// line 1903 "Ruby19Parser.y"
  {
                    yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 498:
					// line 1909 "Ruby19Parser.y"
  {
                    if (!support.is_local_id(((Token)yyVals[0+yyTop]))) {
                        yyerror("duplicate rest argument name");
                    }
                    support.shadowing_lvar(((Token)yyVals[0+yyTop]));
                    yyVal = new RestArgNode(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue(), support.arg_var(((Token)yyVals[0+yyTop])));
                }
  break;
case 499:
					// line 1916 "Ruby19Parser.y"
  {
                    yyVal = new UnnamedRestArgNode(((Token)yyVals[0+yyTop]).getPosition(), support.getCurrentScope().getLocalScope().addVariable("*"));
                }
  break;
case 502:
					// line 1923 "Ruby19Parser.y"
  {
                    String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();

                    if (!support.is_local_id(((Token)yyVals[0+yyTop]))) {
                        yyerror("block argument must be local variable");
                    }
                    support.shadowing_lvar(((Token)yyVals[0+yyTop]));
                    yyVal = new BlockArgNode(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), support.arg_var(((Token)yyVals[0+yyTop])), identifier);
                }
  break;
case 503:
					// line 1933 "Ruby19Parser.y"
  {
                    yyVal = ((BlockArgNode)yyVals[0+yyTop]);
                }
  break;
case 504:
					// line 1936 "Ruby19Parser.y"
  {
                    yyVal = null;
                }
  break;
case 505:
					// line 1940 "Ruby19Parser.y"
  {
                    if (!(((Node)yyVals[0+yyTop]) instanceof SelfNode)) {
                        support.checkExpression(((Node)yyVals[0+yyTop]));
                    }
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 506:
					// line 1946 "Ruby19Parser.y"
  {
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 507:
					// line 1948 "Ruby19Parser.y"
  {
                    if (((Node)yyVals[-1+yyTop]) == null) {
                        yyerror("can't define single method for ().");
                    } else if (((Node)yyVals[-1+yyTop]) instanceof ILiteralNode) {
                        yyerror("can't define single method for literals.");
                    }
                    support.checkExpression(((Node)yyVals[-1+yyTop]));
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 508:
					// line 1958 "Ruby19Parser.y"
  {
                    yyVal = new ArrayNode(getPosition(null));
                }
  break;
case 509:
					// line 1961 "Ruby19Parser.y"
  {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                }
  break;
case 511:
					// line 1966 "Ruby19Parser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).addAll(((ListNode)yyVals[0+yyTop]));
                }
  break;
case 512:
					// line 1970 "Ruby19Parser.y"
  {
                    ISourcePosition position;
                    if (((Node)yyVals[-2+yyTop]) == null && ((Node)yyVals[0+yyTop]) == null) {
                        position = getPosition(((Token)yyVals[-1+yyTop]));
                    } else {
                        position = support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    }

                    yyVal = support.newArrayNode(position, ((Node)yyVals[-2+yyTop])).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 513:
					// line 1980 "Ruby19Parser.y"
  {
                    yyVal = support.newArrayNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), new SymbolNode(getPosition(((Token)yyVals[-1+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue())).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 530:
					// line 1990 "Ruby19Parser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 531:
					// line 1993 "Ruby19Parser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 535:
					// line 1998 "Ruby19Parser.y"
  {
                      yyerrok();
                }
  break;
case 538:
					// line 2004 "Ruby19Parser.y"
  {
                      yyerrok();
                }
  break;
case 539:
					// line 2008 "Ruby19Parser.y"
  {
                      yyVal = null;
                }
  break;
case 540:
					// line 2012 "Ruby19Parser.y"
  {  
                  yyVal = null;
                }
  break;
					// line 8033 "-"
        }
        yyTop -= yyLen[yyN];
        yyState = yyStates[yyTop];
        int yyM = yyLhs[yyN];
        if (yyState == 0 && yyM == 0) {
          yyState = yyFinal;
          if (yyToken < 0) {
            yyToken = yyLex.advance() ? yyLex.token() : 0;
          }
          if (yyToken == 0) {
            return yyVal;
          }
          continue yyLoop;
        }
        if ((yyN = yyGindex[yyM]) != 0 && (yyN += yyState) >= 0
            && yyN < yyTable.length && yyCheck[yyN] == yyState)
          yyState = yyTable[yyN];
        else
          yyState = yyDgoto[yyM];
        continue yyLoop;
      }
    }
  }

					// line 2017 "Ruby19Parser.y"

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
   //yyparse(lexer, new jay.yydebug.yyAnim("JRuby", 9));
    //yyparse(lexer, new jay.yydebug.yyDebugAdapter());
            yyparse(lexer, null);
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
        return pos == null ? lexer.getPosition(null, false) : pos.getPosition();
    }

    private ISourcePosition getPosition(ISourcePositionHolder start) {
        return getPosition(start, false);
    }

    private ISourcePosition getPosition(ISourcePositionHolder start, boolean inclusive) {
        if (start != null) {
            return lexer.getPosition(start.getPosition(), inclusive);
        } 

        return lexer.getPosition(null, inclusive);
    }
}
					// line 8114 "-"
