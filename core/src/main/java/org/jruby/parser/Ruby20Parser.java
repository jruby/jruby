// created by jay 1.0.2 (c) 2002-2004 ats@cs.rit.edu
// skeleton Java 1.0 (c) 2002 ats@cs.rit.edu

					// line 2 "Ruby20Parser.y"
/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
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
import org.jruby.ast.NonLocalControlFlowNode;
import org.jruby.ast.NotNode;
import org.jruby.ast.OpAsgnAndNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OptArgNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.PreExe19Node;
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

public class Ruby20Parser implements RubyParser {
    protected ParserSupport19 support;
    protected RubyYaccLexer lexer;

    public Ruby20Parser() {
        this(new ParserSupport19());
    }

    public Ruby20Parser(ParserSupport19 support) {
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
  public static final int tSYMBOLS_BEG = 381;
  public static final int tQSYMBOLS_BEG = 382;
  public static final int tDSTAR = 383;
  public static final int tSTRING_DEND = 384;
  public static final int tLOWEST = 385;
  public static final int yyErrorCode = 256;

  /** number of final state.
    */
  protected static final int yyFinal = 1;

  /** parser tables.
      Order is mandated by <i>jay</i>.
    */
  protected static final short[] yyLhs = {
//yyLhs 597
    -1,   138,     0,   133,   134,   134,   134,   134,   135,   141,
   135,    35,    34,    36,    36,    36,    36,    42,   142,    42,
   143,    37,    37,    37,    37,    37,    37,    37,    37,    37,
    37,    37,    37,    37,    37,    37,    37,    37,    37,    37,
    37,    37,    37,    37,    37,    32,    32,    38,    38,    38,
    38,    38,    38,    43,    33,    33,    57,    57,   145,   108,
    41,    41,    41,    41,    41,    41,    41,    41,    41,    41,
    41,   109,   109,   120,   120,   110,   110,   110,   110,   110,
   110,   110,   110,   110,   110,    69,    69,    95,    95,    99,
    99,    70,    70,    70,    70,    70,    70,    70,    70,    70,
    75,    75,    75,    75,    75,    75,    75,    75,    75,     6,
     6,    31,    31,    31,     7,     7,     7,     7,     7,   113,
   113,   114,   114,    59,   146,    59,     8,     8,     8,     8,
     8,     8,     8,     8,     8,     8,     8,     8,     8,     8,
     8,     8,     8,     8,     8,     8,     8,     8,     8,     8,
     8,     8,     8,     8,     8,     8,   131,   131,   131,   131,
   131,   131,   131,   131,   131,   131,   131,   131,   131,   131,
   131,   131,   131,   131,   131,   131,   131,   131,   131,   131,
   131,   131,   131,   131,   131,   131,   131,   131,   131,   131,
   131,   131,   131,   131,   131,   131,   131,   131,    39,    39,
    39,    39,    39,    39,    39,    39,    39,    39,    39,    39,
    39,    39,    39,    39,    39,    39,    39,    39,    39,    39,
    39,    39,    39,    39,    39,    39,    39,    39,    39,    39,
    39,    39,    39,    39,    39,    39,    39,    39,    39,    39,
    39,    39,    39,    71,    74,    74,    74,    74,    51,    55,
    55,   123,   123,   123,   123,   123,    49,    49,    49,    49,
    49,   148,    53,   102,   101,   101,    77,    77,    77,    77,
    68,    68,    68,    40,    40,    40,    40,    40,    40,    40,
    40,    40,    40,    40,    40,   149,    40,   150,    40,    40,
    40,    40,    40,    40,    40,    40,    40,    40,    40,    40,
    40,    40,    40,    40,    40,    40,    40,   152,   154,    40,
   155,   156,    40,    40,    40,   157,   158,    40,   159,    40,
   161,   162,    40,   163,    40,   164,    40,   165,   166,    40,
    40,    40,    40,    40,    44,   151,   151,   151,   153,   153,
    47,    47,    45,    45,   122,   122,   124,   124,    82,    82,
   125,   125,   125,   125,   125,   125,   125,   125,   125,    89,
    89,    89,    89,    88,    88,    65,    65,    65,    65,    65,
    65,    65,    65,    65,    65,    65,    65,    65,    65,    65,
    67,    67,    66,    66,    66,   117,   117,   116,   116,   126,
   126,   167,   119,    64,    64,   118,   118,   168,   107,    56,
    56,    56,    56,    23,    23,    23,    23,    23,    23,    23,
    23,    23,   169,   106,   170,   106,    72,    46,    46,   111,
   111,    73,    73,    73,    48,    48,    50,    50,    28,    28,
    28,    15,    16,    16,    16,    17,    18,    19,    25,    25,
    79,    79,    27,    27,    85,    85,    83,    83,    26,    26,
    86,    86,    78,    78,    84,    84,    20,    20,    21,    21,
    24,    24,    22,   171,    22,   172,    22,    60,    60,    60,
    60,     2,     1,     1,     1,     1,    30,    29,    29,    29,
    29,   127,   127,   127,   127,   127,   128,   128,   128,   128,
   128,   128,   128,    54,    54,   112,   112,    61,    61,    52,
   173,    52,    52,    63,    63,    90,    90,    90,    90,    87,
    87,    62,    62,    62,    62,    62,    62,    62,    62,    62,
    62,    62,    62,    62,    62,    62,   132,   132,   132,   132,
     9,     9,   115,   115,    80,    80,    91,    92,    93,    93,
    94,    94,   136,   136,   137,    58,   121,   100,   100,    81,
    81,    11,    11,    13,    13,    12,    12,   105,   104,   104,
    14,   174,    14,    98,    98,    96,    96,    97,    97,    97,
     3,     3,     3,     4,     4,     4,     4,     5,     5,     5,
    10,    10,   139,   139,   144,   144,   129,   130,   147,   147,
   147,   160,   160,   140,   140,    76,   103,
    }, yyLen = {
//yyLen 597
     2,     0,     2,     2,     1,     1,     3,     2,     1,     0,
     5,     4,     2,     1,     1,     3,     2,     1,     0,     5,
     0,     4,     3,     3,     3,     2,     3,     3,     3,     3,
     3,     4,     1,     3,     3,     6,     5,     5,     5,     5,
     3,     3,     3,     3,     1,     3,     3,     1,     3,     3,
     3,     2,     1,     1,     1,     1,     1,     4,     0,     5,
     2,     3,     4,     5,     4,     5,     2,     2,     2,     2,
     2,     1,     3,     1,     3,     1,     2,     3,     5,     2,
     4,     2,     4,     1,     3,     1,     3,     2,     3,     1,
     3,     1,     1,     4,     3,     3,     3,     3,     2,     1,
     1,     1,     4,     3,     3,     3,     3,     2,     1,     1,
     1,     2,     1,     3,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     0,     4,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     3,     5,
     3,     5,     6,     5,     5,     5,     5,     4,     3,     3,
     3,     3,     3,     3,     3,     3,     3,     4,     4,     2,
     2,     3,     3,     3,     3,     3,     3,     3,     3,     3,
     3,     3,     3,     3,     2,     2,     3,     3,     3,     3,
     3,     6,     1,     1,     1,     2,     4,     2,     3,     1,
     1,     1,     1,     2,     4,     2,     1,     2,     2,     4,
     1,     0,     2,     2,     2,     1,     1,     2,     3,     4,
     3,     4,     2,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     3,     0,     3,     0,     4,     3,
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
     1,     0,     3,     4,     2,     3,     3,     0,     5,     2,
     4,     5,     5,     2,     4,     4,     3,     3,     3,     2,
     1,     4,     0,     5,     0,     5,     5,     1,     1,     6,
     0,     1,     1,     1,     2,     1,     2,     1,     1,     1,
     1,     1,     1,     1,     2,     3,     3,     3,     3,     3,
     0,     3,     1,     2,     3,     3,     0,     3,     3,     3,
     3,     3,     0,     3,     0,     3,     0,     2,     0,     2,
     0,     2,     1,     0,     3,     0,     4,     1,     1,     1,
     1,     2,     1,     1,     1,     1,     3,     1,     1,     2,
     2,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     0,     4,     2,     3,     2,     4,     2,     2,     1,     2,
     0,     6,     8,     4,     6,     4,     6,     2,     4,     6,
     2,     4,     2,     4,     1,     0,     1,     1,     1,     1,
     1,     1,     1,     3,     1,     3,     2,     2,     1,     3,
     1,     3,     1,     1,     2,     3,     3,     1,     3,     1,
     3,     1,     1,     2,     1,     1,     1,     2,     2,     0,
     1,     0,     4,     1,     2,     1,     3,     3,     2,     2,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     0,     1,     0,     1,     2,     2,     0,     1,
     1,     1,     1,     1,     2,     0,     0,
    }, yyDefRed = {
//yyDefRed 1032
     1,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   307,   310,     0,     0,     0,   332,   333,     0,
     0,     0,   487,   486,   488,   489,     0,     0,     0,     9,
     0,   491,   490,   492,     0,     0,   483,   482,     0,   485,
   432,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   456,   458,   460,     0,     0,   391,   497,
   498,   477,   478,     0,     0,   429,     0,   274,     0,   433,
   275,   276,     0,   277,   278,   273,   428,   430,    32,    47,
     0,     0,     0,     0,     0,     0,   281,     0,    55,     0,
     0,    85,     0,     4,   279,   280,     0,     0,    71,     0,
     0,     0,     2,     0,     5,     7,   330,   331,   294,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   112,     0,   334,     0,   282,   493,   494,   323,   166,   177,
   167,   190,   163,   183,   173,   172,   188,   171,   170,   165,
   191,   175,   164,   178,   182,   184,   176,   169,   185,   192,
   187,     0,     0,     0,     0,   162,   181,   180,   193,   194,
   195,   196,   197,   161,   168,   159,   160,     0,     0,     0,
     0,   116,     0,   151,   152,   148,   129,   130,   131,   138,
   135,   137,   132,   133,   153,   154,   139,   140,   561,   145,
   144,   128,   150,   147,   146,   142,   143,   136,   134,   126,
   149,   127,   155,   141,   325,   117,     0,   560,   118,   186,
   179,   189,   174,   156,   157,   158,   114,   115,   120,   119,
   122,     0,   121,   123,     0,     0,     0,     0,     0,     0,
    14,    13,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   591,   592,     0,     0,     0,   593,     0,     0,
     0,     0,     0,   344,   345,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   256,    69,     0,     0,     0,   565,
   260,    70,    68,     0,    67,     0,     0,   409,    66,     0,
   585,     0,     0,    20,     0,     0,     0,   219,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   244,     0,     0,     0,   563,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   235,    51,   234,   474,   473,   475,
   471,   472,     0,     0,     0,     0,     0,     0,     0,     0,
   304,     0,     0,     0,     0,     0,   414,   412,   403,     0,
   301,   434,   303,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   397,   399,     0,     0,
     0,   580,   581,     0,     0,    87,     0,     0,     0,     0,
     0,     0,     3,     0,     0,   479,   480,     0,   109,     0,
   111,     0,   500,   318,   499,     0,     0,     0,     0,     0,
     0,   327,   124,     0,     0,     0,     0,   284,    12,     0,
     0,   336,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   594,     0,     0,     0,     0,     0,
     0,   315,   568,   267,   263,   569,     0,     0,   257,   265,
     0,   258,     0,   296,     0,   262,   252,   251,     0,     0,
     0,     0,   300,    50,    22,    24,    23,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   289,     0,
     0,   286,   292,     0,   589,   245,     0,   247,   564,   293,
     0,    89,     0,     0,     0,     0,     0,   465,   463,   476,
   462,   459,   435,   457,   436,   437,   461,   438,   439,   442,
     0,   448,   449,     0,     0,   528,   527,   526,   529,     0,
   542,     0,     0,   552,   551,   556,   555,   543,   532,     0,
     0,     0,   549,     0,     0,     0,     0,   524,   540,     0,
   508,   534,   530,     0,     0,   444,   445,     0,   450,   451,
     0,     0,     0,    58,    61,    26,    27,    28,    29,    30,
    48,    49,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   575,     0,     0,   576,   407,     0,     0,     0,     0,   406,
     0,   408,     0,   573,   574,     0,    40,     0,     0,    46,
    45,     0,    41,   266,     0,     0,     0,     0,     0,    88,
    33,    43,     0,    34,     0,     6,     0,   502,     0,     0,
     0,     0,     0,     0,   113,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   422,     0,     0,   423,     0,     0,
   342,    15,     0,   337,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   314,   339,   308,   338,   311,     0,     0,
     0,     0,     0,     0,   567,     0,     0,     0,   264,   566,
   295,   586,     0,     0,   248,   299,    21,     0,     0,    31,
     0,     0,     0,     0,   288,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   441,   443,   453,     0,   536,
   531,     0,     0,   346,     0,   348,     0,     0,   553,   557,
     0,   522,   394,     0,     0,     0,   392,     0,   517,     0,
   520,     0,   506,   544,     0,   507,   447,   455,   383,     0,
   381,     0,   380,     0,     0,     0,     0,     0,     0,   250,
     0,   404,   249,     0,     0,   405,     0,     0,     0,     0,
     0,     0,     0,     0,     0,    86,     0,     0,     0,   321,
     0,     0,   411,   324,   562,     0,   504,     0,   328,   125,
     0,     0,     0,   425,   343,     0,    11,   427,     0,   340,
     0,     0,     0,     0,     0,     0,     0,   313,     0,     0,
     0,     0,     0,     0,   269,   259,     0,   298,    10,   246,
    90,     0,     0,   467,   468,   469,   464,   470,   545,     0,
     0,     0,     0,   533,     0,     0,   509,     0,     0,     0,
     0,     0,   535,     0,   550,     0,   541,   558,     0,     0,
     0,     0,     0,     0,   379,   538,     0,     0,   362,     0,
   547,     0,     0,     0,     0,     0,     0,    36,     0,    37,
     0,    63,    39,     0,    38,     0,    65,     0,   587,   402,
   401,     0,     0,     0,     0,     0,     0,   501,   319,   503,
   326,     0,    19,     0,     0,     0,   424,     0,     0,     0,
   426,     0,   305,     0,   306,   268,     0,     0,     0,   316,
     0,   466,   347,     0,     0,     0,   349,   393,     0,   523,
   389,     0,   387,   390,   396,   395,     0,   515,     0,   513,
     0,   518,   521,   505,     0,     0,     0,     0,   377,     0,
     0,   372,     0,   360,     0,   375,   382,   361,   415,   413,
     0,     0,   398,    35,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   417,   416,   418,   309,   312,     0,
     0,     0,     0,     0,     0,   386,     0,     0,     0,     0,
     0,   291,     0,     0,   363,   384,     0,     0,   539,     0,
     0,     0,   548,    59,   322,     0,     0,     0,     0,     0,
     0,   419,     0,     0,     0,     0,   388,   516,     0,   511,
   514,   519,     0,     0,     0,   378,     0,   369,     0,   367,
   359,     0,   373,   376,   329,     0,   341,   317,     0,     0,
     0,     0,     0,     0,   512,   371,     0,   365,   368,   374,
     0,   366,
    }, yyDgoto = {
//yyDgoto 175
     1,   320,    65,   119,   633,   599,   120,   219,   593,   528,
   383,   529,   530,   531,   206,    67,    68,    69,    70,    71,
   323,   322,   501,    72,   325,    73,    74,   510,    75,    76,
    77,   121,    78,    79,   226,   227,   228,   229,    81,    82,
    83,    84,   230,   234,   288,   789,   955,   790,   782,   456,
   786,   601,   403,   274,    86,   751,    87,    88,   532,   221,
   816,   236,   639,   640,   534,   842,   740,   741,   612,    90,
    91,   266,   433,   646,   299,   237,   231,   458,   329,   327,
   535,   536,   714,   333,   335,    94,    95,   721,   928,   974,
   826,   538,   845,   846,   539,    96,   459,   269,   306,   492,
   847,   448,   270,   449,   732,   540,   340,   377,   554,    97,
    98,   416,   238,   222,   223,   541,   911,   722,   726,   330,
   296,   850,   255,   460,   715,   716,   912,   239,   240,   453,
   757,   208,   542,   102,   103,   104,   543,   544,     2,   245,
   246,   285,   414,   467,   454,   744,   642,   485,   275,   298,
   480,   422,   248,   665,   800,   249,   801,   673,   959,   629,
   423,   626,   876,   408,   410,   641,   881,   331,   588,   552,
   551,   704,   703,   628,   409,
    }, yySindex = {
//yySindex 1032
     0,     0, 16000, 17015,  6098, 18905, 15448, 15778, 16127, 18149,
 18149, 13429,     0,     0, 18653,  4028,  4028,     0,     0,  4028,
  -194,  -163,     0,     0,     0,     0,    48, 15668,   204,     0,
  -123,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 18275, 18275,  -182,   -65, 16254, 18149, 16507, 16888,  4606,
 18275, 18401, 15558,     0,     0,     0,   253,   262,     0,     0,
     0,     0,     0,   271,   283,     0,  -145,     0,   -52,     0,
     0,     0,  -181,     0,     0,     0,     0,     0,     0,     0,
  1334,    40,  5942,     0,    45,   534,     0,  -167,     0,   148,
   291,     0,   301,     0,     0,     0, 18779,   333,     0,   181,
     0,     0,     0,   108,     0,     0,     0,     0,     0,  -194,
  -163,   182,   204,     0,     0,   -25, 18149,    11, 16127,  -145,
     0,    85,     0,   647,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,  -167,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   481,     0,     0, 17141, 16127,   304,   299,   108,  1334,
     0,     0,   287,    40,    80,   695,   276,   551,   303,     0,
     0,    80,     0,     0,   108,   353,   575,     0, 18149, 18149,
   363,   743,     0,     0,     0,   398,     0,     0, 18275, 18275,
 18275, 18275, 18275,  5942,     0,     0,   341,   640,   648,     0,
     0,     0,     0, 12255,     0,  4028,  4028,     0,     0, 13555,
     0, 18149,  -132,     0, 17267,   334, 16127,     0,   746,   385,
   392,   393,   375, 16254,   372,     0,   204,    40,   204,   377,
     0,   156,   266,   341,     0,   266,   360,   413, 19031,   751,
     0,   688,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   217,   543,   583,   -66,   361,   722,   367,  -217,
     0,  4615,   369,   857,   371,  -125,     0,     0,     0,   405,
     0,     0,     0, 18149, 18149, 18149, 18149, 17141, 18149, 18149,
 18275, 18275, 18275, 18275, 18275, 18275, 18275, 18275, 18275, 18275,
 18275, 18275, 18275, 18275, 18275, 18275, 18275, 18275, 18275, 18275,
 18275, 18275, 18275, 18275, 18275, 18275,     0,     0,  3060,  3146,
  4028,     0,     0,  4479, 18401,     0, 17393, 16254,  5103,   708,
 17393, 18401,     0, 15196,   414,     0,     0,    40,     0,     0,
     0,   108,     0,     0,     0,  3557,  3643,  4028, 16127, 18149,
  4987,     0,     0,  1334,   399, 17519,   484,     0,     0, 15322,
   375,     0, 16127,   495,  4110,  6450,  4028, 18275, 18275, 18275,
 16127,   353, 17645,   499,     0,    88,    88,     0, 19360, 19415,
  4028,     0,     0,     0,     0,     0, 18275,  6593,     0,     0,
 16634,     0,   204,     0,   425,     0,     0,     0,   737,   740,
   204,    73,     0,     0,     0,     0,     0, 15778, 18149,  5942,
 16000,   423,  4110,  6450, 18275, 18275, 18275,   204,     0,     0,
   204,     0,     0, 16761,     0,     0, 16888,     0,     0,     0,
     0,     0,   747, 19470, 19525,  4028, 19031,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   159,     0,     0,   765,   744,     0,     0,     0,     0, 18275,
     0,  1430,  2636,     0,     0,     0,     0,     0,     0,   503,
   508,   757,     0,   204,  -153,   771,   772,     0,     0,   775,
     0,     0,     0,   516,   796,     0,     0,   162,     0,     0,
   811,  -155,  -155,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   385,  2108,  2108,  2108,  2108,  1801,  1801,  4157,
  5989,  2108,  2108,  5540,  5540,  1044,  1044,   385,  1525,   385,
   385,   338,   338,  1801,  1801,  1941,  1941,  4957,  -155,   507,
     0,   511,  -163,     0,     0,   513,     0,   515,  -163,     0,
     0,     0,   204,     0,     0,  -163,     0,  5942, 18275,     0,
     0,  5494,     0,     0,   793,   817,   204, 19031,   826,     0,
     0,     0,     0,     0,  5621,     0,   108,     0, 18149, 16127,
  -163,     0,     0,  -163,     0,   204,   607,    73,  2636,   108,
 16127, 15888, 15778, 16000,     0,     0,   542,     0, 16127,   609,
     0,     0,   -45,     0,   545,   548,   554,   563,   204,  5494,
   484,   638,   831,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   204, 18149,     0, 18275,   341,   648,     0,     0,
     0,     0,  6593, 16634,     0,     0,     0,    73,   549,     0,
   385,   385,  5942,     0,     0,   266, 19031,     0,     0,     0,
     0,   204,   747, 16127,   875,     0,     0,     0, 18275,     0,
     0,  1430,   975,     0,   866,     0,   204,   204,     0,     0,
  2406,     0,     0,   863, 16127, 16127,     0,  2636,     0,  2636,
     0,   763,     0,     0,   232,     0,     0,     0,     0,  1591,
     0, 16127,     0, 16127,  -155,   889, 16127, 18401, 18401,     0,
   405,     0,     0, 18401, 18401,     0,   405,   600,   606,    45,
  -181,     0, 18275, 18401, 17771,     0,   747, 19031, 18275,     0,
   108,   680,     0,     0,     0,   204,     0,   692,     0,     0,
   589, 19157,    80,     0,     0, 16127,     0,     0, 18149,     0,
   716, 18275, 18275, 18275, 18275,   622,   717,     0, 17897, 16127,
 16127, 16127,     0,    88,     0,     0,   939,     0,     0,     0,
     0,     0,   624,     0,     0,     0,     0,     0,     0,   204,
  1667,   949,  1616,     0,   654,   959,     0,  1656,   758,   642,
   963,   971,     0,   981,     0,   959,     0,     0,   796,   965,
 19283,   987,   204,   989,     0,     0,   994,  1004,     0,   693,
     0,   796,   792,   690, 16127, 18275,   798,     0,  5942,     0,
  5942,     0,     0,  5942,     0,  5942,     0, 18401,     0,     0,
     0,  5942, 18275,     0,   747,  5942, 16127,     0,     0,     0,
     0,  4987,     0,   754,   773,     0,     0,     0,     0, 16127,
     0,    80,     0, 18275,     0,     0,   394,   801,   806,     0,
 16634,     0,     0,  1028,  1667,  1314,     0,     0,  2406,     0,
     0,   282,     0,     0,     0,     0,  2406,     0,  2636,     0,
  2406,     0,     0,     0, 19283,   778,   804,  3487,     0,   715,
  2990,     0,   905,     0,  2990,     0,     0,     0,     0,     0,
   720,  5942,     0,     0,  5942,     0,   827, 16127,     0, 19580,
 19635,  4028,   304, 16127,     0,     0,     0,     0,     0, 16127,
  1667,  1028,  1667,  1051,  1656,     0,   959,  1053,   959,   959,
   804,     0, 19690,  1058,     0,     0,  1065,  1071,     0,   796,
  1077,  1058,     0,     0,     0,   846,     0,     0,     0,     0,
   204,     0,   -45,   862,  1028,  1667,     0,     0,  2406,     0,
     0,     0,     0,     0,  3487,     0,  3487,     0,  2990,     0,
     0,  3487,     0,     0,     0,     0,     0,     0,  1028,   959,
  1058,  1089,  1058,  1058,     0,     0,  3487,     0,     0,     0,
  1058,     0,
    }, yyRindex = {
//yyRindex 1032
     0,     0,   189,     0,     0,     0,     0,     0,  1166,     0,
     0,   876,     0,     0,     0, 13710, 13836,     0,     0, 13950,
  5384,  4887,     0,     0,     0,     0, 18527,     0, 18023,     0,
     0,     0,     0,     0,  2772,  3893,     0,     0,  2899,     0,
     0,     0,     0,     0,     0,   183,    66,   808,   791,    87,
     0,     0,   861,     0,     0,     0,   927,   -77,     0,     0,
     0,     0,     0,  1133,   -75,     0, 16380,     0,  7626,     0,
     0,     0,  7770,     0,     0,     0,     0,     0,     0,     0,
    74, 14903,  1754,  7874, 14967,     0,     0, 15028,     0, 14408,
     0,     0,     0,     0,     0,     0,   268,     0,     0,     0,
 14195, 14299,     0,    31,     0,     0,     0,     0,     0,  7987,
  6831,     0,   820, 12383, 12516,     0,     0,     0,   183,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   518,  1066,  1259,  1326,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  1750,  1886,  2624,
  3014,     0,  3511,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 15103,     0,     0,     0,   257,   665,     0,  1911,   635,
     0,     0,     0,  2555,     0,     0,  7411,     0,     0,  6945,
  7079,     0,     0,     0,   876,     0,   890,     0,     0,     0,
     0,     0,   768,     0,     0,     0,  1275,  1467,     0,     0,
     0,     0,     0, 11978,     0,     0, 14587, 14740, 14740,     0,
     0,     0,     0,   829,     0,     0,   218,     0,     0,   829,
     0,     0,     0,     0,     0,     0,    37,     0,     0,  8336,
  8119,  8235, 14483,   183,     0,   205,   829,   220,   829,     0,
     0,   824,   824,     0,     0,   810,     0,     0,     0,     0,
   673,   281,  1488,  1489,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,    27,     0,     0,     0,     0,     0,     0,     0, 12106,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   131,     0,     0,     0,     0,     0,     0,   183,   497,   566,
     0,     0,     0,    44,     0,     0,     0,   126,     0, 12902,
     0,     0,     0,     0,     0,     0,     0,   131,  1166,     0,
   133,     0,     0,   874,     0,   297,   110,     0,     0,  2052,
  7525,     0,  1153, 13032,     0,     0,   131,     0,     0,     0,
   112,     0,     0,     0,     0,     0,     0,  1509,     0,     0,
   131,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   829,     0,     0,     0,     0,     0,   132,   132,
   829,   829,     0,     0,     0,     0,     0,     0,     0, 11086,
    37,     0,     0,     0,     0,     0,     0,   829,     0,    70,
   829,     0,     0,   854,     0,     0,  -193,     0,     0,     0,
  1670,     0,   686,     0,     0,   131,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   139,     0,     0,     0,     0,     0,
     0,     0,   199,     0,     0,     0,     0,     0,     0,    75,
     0,   153,     0,   275,     0,   153,   153,     0,     0,   174,
     0,     0,     0,     0,   174,     0,     0,     0,     0,     0,
     0, 13160, 13301,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  8451,  2516, 10422, 10540, 10637,  9965, 10085, 10723,
  1690, 10813, 10910,  2066, 10996,  9387,  9510,  8584,  9628,  8699,
  8800,  9161,  9265, 10182, 10300,  9736,  9845,  1117, 13160,  5745,
     0,  5872,  5257,     0,     0,  6242,  4263,  6369, 16380,     0,
  4390,     0,   860,     0,     0,  2385,     0, 11126,     0,     0,
     0, 11034,     0,     0,     0,     0,   829,     0,   687,     0,
     0,     0, 12049,     0, 12066,     0,     0,     0,     0,  1166,
  7277, 12644, 12774,     0,     0,   860,     0,   829,   221,     0,
  1166,     0,     0,    37,     0,   626,   328,     0,   610,   930,
     0,     0,   930,     0,  3269,  3396,  3766,  4760,   860, 12162,
   930,     0,     0,     0,     0,     0,     0,     0,   839,  1200,
  1447,   659,   860,     0,     0,     0, 14648, 14740,     0,     0,
     0,     0,   168,   194,     0,     0,     0,   829,     0,     0,
  8916,  9048, 11211,   176,     0,   824,     0,   879,  1110,  1486,
  1186,   860,   694,    37,     0,     0,     0,     0,     0,     0,
     0,     0,   226,     0,   229,     0,   829,    20,     0,     0,
     0,     0,     0,  -133,   103,    37,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   106,
     0,   103,     0,    37, 13301,     0,   103,     0,     0,     0,
 14783,     0,     0,     0,     0,     0, 14847, 14076,     0, 15089,
 14887,   749,     0,     0,     0,     0,   709,     0,     0,     0,
     0,     0,     0,     0,     0,   829,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   103,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  6730,     0,     0,     0,   886,
   103,   103,   608,     0,     0,     0,   132,     0,     0,     0,
     0,   992,     0,     0,     0,     0,     0,     0,     0,   829,
     0,   251,     0,     0,     0,   153,     0,     0,     0,     0,
   153,   153,     0,   153,     0,   153,     0,     0,   174,    57,
     0,    62,   106,    62,     0,     0,    81,    62,     0,     0,
     0,    81,     0,     0,    37,     0,     0,     0, 11307,     0,
 11392,     0,     0, 11453,     0, 11550,     0,     0,     0,     0,
     0, 11636,     0,  1193,   726, 11745,  1166,     0,     0,     0,
     0,   133,     0,     0,     0,   649,     0,  1167,  1321,  1166,
     0,     0,     0,     0,     0,     0,   930,     0,     0,     0,
   200,     0,     0,   255,     0,   258,     0,     0,     0,     0,
     0,   -22,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   143,     0,     0,     0,
   114,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 11806,     0,     0, 11892,  2246,     0,  1166,  1385,     0,
     0,   131,   665,  1153,     0,     0,     0,     0,     0,   103,
     0,   259,     0,   261,     0,     0,   153,   153,   153,   153,
   146,     0,     0,    62,     0,     0,    62,    62,     0,    81,
    62,    62,     0,     0,     0,     0,   464,   478,   850,  1097,
   860,     0,   930,     0,   267,     0,     0,     0,     0,     0,
     0,     0,  1553,   120,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   788,     0,     0,   270,   153,
    62,    62,    62,    62,     0,     0,     0,     0,     0,     0,
    62,     0,
    }, yyGindex = {
//yyGindex 175
     0,     0,    29,   916,  -318,     0,   -51,     2,    -4,  -452,
   999,     0,     0,   618,     0,     0,     0,  1138,     0,     0,
     0,  1154,  1358,     0,     0,     0,     0,   883,     0,     0,
    36,  1218,  -329,   -47,   619,  -376,     0,    25,   208,  2121,
    15,    -3,   805,    14,   381,  -405,     0,   247,     0,   764,
     0,    -7,     0,     9,  1235,   637,     0,     0,  -606,     0,
     0,   570,  -229,   362,     0,     0,     0,  -443,  -208,   -86,
    24,  1388,  -398,     0,     0,  1498,    -2,   699,     0,     0,
  9732,   519,  -716,     0,     0,     0,     0,  -485,  1369,   506,
  -316,   522,   317,     0,     0,     0,    23,  -433,     0,  -416,
   326,  -227,  -395,     0,  -518,  1572,   -67,   509,  -516,  1261,
    72,   324,    32,     0,   -21,  -622,     0,  -558,     0,     0,
  -161,  -834,     0,  -358,  -733,   567,   318,  1055,  1464,  -171,
  -543,     0,  -689,  -344,     0,    18,     0,  1251,     0,   -15,
    54,     0,     0,     0,   -19,     0,     0,  -224,     0,     0,
     0,  -240,     0,  -374,     0,     0,     0,     0,     0,     0,
     7,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,
    };
    protected static final short[] yyTable = Ruby20YyTables.yyTable();
    protected static final short[] yyCheck = Ruby20YyTables.yyCheck();

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
    "tREGEXP_END","tSYMBOLS_BEG","tQSYMBOLS_BEG","tDSTAR","tSTRING_DEND",
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
    "$$2 :",
    "top_stmt : klBEGIN $$2 tLCURLY top_compstmt tRCURLY",
    "bodystmt : compstmt opt_rescue opt_else opt_ensure",
    "compstmt : stmts opt_terms",
    "stmts : none",
    "stmts : stmt_or_begin",
    "stmts : stmts terms stmt_or_begin",
    "stmts : error stmt",
    "stmt_or_begin : stmt",
    "$$3 :",
    "stmt_or_begin : kBEGIN $$3 tLCURLY top_compstmt tRCURLY",
    "$$4 :",
    "stmt : kALIAS fitem $$4 fitem",
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
    "block_command : block_call dot_or_colon operation2 command_args",
    "$$5 :",
    "cmd_brace_block : tLBRACE_ARG $$5 opt_block_param compstmt tRCURLY",
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
    "mlhs_node : user_variable",
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
    "$$6 :",
    "undef_list : undef_list ',' $$6 fitem",
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
    "op : tDSTAR",
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
    "$$7 :",
    "command_args : $$7 call_args",
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
    "primary : symbols",
    "primary : qsymbols",
    "primary : var_ref",
    "primary : backref",
    "primary : tFID",
    "primary : kBEGIN bodystmt kEND",
    "$$8 :",
    "primary : tLPAREN_ARG $$8 rparen",
    "$$9 :",
    "primary : tLPAREN_ARG expr $$9 rparen",
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
    "$$10 :",
    "$$11 :",
    "primary : kWHILE $$10 expr_value do $$11 compstmt kEND",
    "$$12 :",
    "$$13 :",
    "primary : kUNTIL $$12 expr_value do $$13 compstmt kEND",
    "primary : kCASE expr_value opt_terms case_body kEND",
    "primary : kCASE opt_terms case_body kEND",
    "$$14 :",
    "$$15 :",
    "primary : kFOR for_var kIN $$14 expr_value do $$15 compstmt kEND",
    "$$16 :",
    "primary : kCLASS cpath superclass $$16 bodystmt kEND",
    "$$17 :",
    "$$18 :",
    "primary : kCLASS tLSHFT expr $$17 term $$18 bodystmt kEND",
    "$$19 :",
    "primary : kMODULE cpath $$19 bodystmt kEND",
    "$$20 :",
    "primary : kDEF fname $$20 f_arglist bodystmt kEND",
    "$$21 :",
    "$$22 :",
    "primary : kDEF singleton dot_or_colon $$21 fname $$22 f_arglist bodystmt kEND",
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
    "lambda : $$23 f_larglist lambda_body",
    "f_larglist : tLPAREN2 f_args opt_bv_decl tRPAREN",
    "f_larglist : f_args opt_bv_decl",
    "lambda_body : tLAMBEG compstmt tRCURLY",
    "lambda_body : kDO_LAMBDA compstmt kEND",
    "$$24 :",
    "do_block : kDO_BLOCK $$24 opt_block_param compstmt kEND",
    "block_call : command do_block",
    "block_call : block_call dot_or_colon operation2 opt_paren_args",
    "block_call : block_call dot_or_colon operation2 opt_paren_args brace_block",
    "block_call : block_call dot_or_colon operation2 command_args do_block",
    "method_call : operation paren_args",
    "method_call : primary_value tDOT operation2 opt_paren_args",
    "method_call : primary_value tCOLON2 operation2 paren_args",
    "method_call : primary_value tCOLON2 operation3",
    "method_call : primary_value tDOT paren_args",
    "method_call : primary_value tCOLON2 paren_args",
    "method_call : kSUPER paren_args",
    "method_call : kSUPER",
    "method_call : primary_value '[' opt_call_args rbracket",
    "$$25 :",
    "brace_block : tLCURLY $$25 opt_block_param compstmt tRCURLY",
    "$$26 :",
    "brace_block : kDO $$26 opt_block_param compstmt kEND",
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
    "regexp : tREGEXP_BEG regexp_contents tREGEXP_END",
    "words : tWORDS_BEG ' ' tSTRING_END",
    "words : tWORDS_BEG word_list tSTRING_END",
    "word_list :",
    "word_list : word_list word ' '",
    "word : string_content",
    "word : word string_content",
    "symbols : tSYMBOLS_BEG ' ' tSTRING_END",
    "symbols : tSYMBOLS_BEG symbol_list tSTRING_END",
    "symbol_list :",
    "symbol_list : symbol_list word ' '",
    "qwords : tQWORDS_BEG ' ' tSTRING_END",
    "qwords : tQWORDS_BEG qword_list tSTRING_END",
    "qsymbols : tQSYMBOLS_BEG ' ' tSTRING_END",
    "qsymbols : tQSYMBOLS_BEG qsym_list tSTRING_END",
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
    "$$27 :",
    "string_content : tSTRING_DVAR $$27 string_dvar",
    "$$28 :",
    "string_content : tSTRING_DBEG $$28 compstmt tRCURLY",
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
    "keyword_variable : kNIL",
    "keyword_variable : kSELF",
    "keyword_variable : kTRUE",
    "keyword_variable : kFALSE",
    "keyword_variable : k__FILE__",
    "keyword_variable : k__LINE__",
    "keyword_variable : k__ENCODING__",
    "var_ref : user_variable",
    "var_ref : keyword_variable",
    "var_lhs : user_variable",
    "var_lhs : keyword_variable",
    "backref : tNTH_REF",
    "backref : tBACK_REF",
    "superclass : term",
    "$$29 :",
    "superclass : tLT $$29 expr_value term",
    "superclass : error term",
    "f_arglist : tLPAREN2 f_args rparen",
    "f_arglist : f_args term",
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
    "f_arg_item : f_norm_arg",
    "f_arg_item : tLPAREN f_margs rparen",
    "f_arg : f_arg_item",
    "f_arg : f_arg ',' f_arg_item",
    "f_kw : tLABEL arg_value",
    "f_block_kw : tLABEL primary_value",
    "f_block_kwarg : f_block_kw",
    "f_block_kwarg : f_block_kwarg ',' f_block_kw",
    "f_kwarg : f_kw",
    "f_kwarg : f_kwarg ',' f_kw",
    "kwrest_mark : tPOW",
    "kwrest_mark : tDSTAR",
    "f_kwrest : kwrest_mark tIDENTIFIER",
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
    "$$30 :",
    "singleton : tLPAREN2 $$30 expr rparen",
    "assoc_list : none",
    "assoc_list : assocs trailer",
    "assocs : assoc",
    "assocs : assocs ',' assoc",
    "assoc : arg_value tASSOC arg_value",
    "assoc : tLABEL arg_value",
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
      @param yydebug debug message writer implementing <tt>yyDebug</tt>, or <tt>null</tt>.
      @return result of the last reduction, if any.
    */
  public Object yyparse (RubyYaccLexer yyLex, Object ayydebug)
				throws java.io.IOException {
    this.yydebug = (org.jruby.parser.YYDebug) ayydebug;
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

static ParserState[] states = new ParserState[597];
static {
states[65] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[327] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setState(LexState.EXPR_FNAME);
    return yyVal;
  }
};
states[458] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[262] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[393] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ArgsNode)yyVals[-2+yyTop]);
                    ((ISourcePositionHolder)yyVal).setPosition(((Token)yyVals[-3+yyTop]).getPosition());
    return yyVal;
  }
};
states[524] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ArgsTailHolder)yyVals[0+yyTop]).getPosition(), null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[66] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop])); /* .setPosFrom($2);*/
    return yyVal;
  }
};
states[328] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.setInSingle(support.getInSingle() + 1);
                    support.pushLocalScope();
                    lexer.setState(LexState.EXPR_ENDFN); /* force for args */
    return yyVal;
  }
};
states[459] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.literal_concat(support.getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[263] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BlockPassNode(((Token)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[394] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ArgsNode)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[525] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(lexer.getPosition(), null, null, null, null, (ArgsTailHolder) null);
    return yyVal;
  }
};
states[1] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.setState(LexState.EXPR_BEG);
                  support.initTopLocalVariables();
    return yyVal;
  }
};
states[198] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    /* FIXME: Consider fixing node_assign itself rather than single case*/
                    ((Node)yyVal).setPosition(support.getPosition(((Node)yyVals[-2+yyTop])));
    return yyVal;
  }
};
states[329] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    /* TODO: We should use implicit nil for body, but problem (punt til later)*/
                    Node body = ((Node)yyVals[-1+yyTop]); /*$8 == null ? NilImplicitNode.NIL : $8;*/

                    yyVal = new DefsNode(((Token)yyVals[-8+yyTop]).getPosition(), ((Node)yyVals[-7+yyTop]), new ArgumentNode(((Token)yyVals[-4+yyTop]).getPosition(), (String) ((Token)yyVals[-4+yyTop]).getValue()), ((ArgsNode)yyVals[-2+yyTop]), support.getCurrentScope(), body);
                    support.popCurrentScope();
                    support.setInSingle(support.getInSingle() - 1);
    return yyVal;
  }
};
states[460] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[67] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_yield(((Token)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[264] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((BlockPassNode)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[395] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[526] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("formal argument cannot be a constant");
    return yyVal;
  }
};
states[2] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
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
};
states[199] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ISourcePosition position = ((Token)yyVals[-1+yyTop]).getPosition();
                    Node body = ((Node)yyVals[0+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop]);
                    yyVal = support.node_assign(((Node)yyVals[-4+yyTop]), new RescueNode(position, ((Node)yyVals[-2+yyTop]), new RescueBodyNode(position, null, body, null), null));
    return yyVal;
  }
};
states[330] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BreakNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
    return yyVal;
  }
};
states[461] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
    /* FIXME: mri is different here.*/
                    yyVal = support.literal_concat(support.getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[68] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ReturnNode(((Token)yyVals[-1+yyTop]).getPosition(), support.ret_args(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop]).getPosition()));
    return yyVal;
  }
};
states[396] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[527] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("formal argument cannot be an instance variable");
    return yyVal;
  }
};
states[3] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  if (((Node)yyVals[-1+yyTop]) instanceof BlockNode) {
                      support.checkUselessStatements(((BlockNode)yyVals[-1+yyTop]));
                  }
                  yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[200] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
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
};
states[331] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new NextNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
    return yyVal;
  }
};
states[462] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[69] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BreakNode(((Token)yyVals[-1+yyTop]).getPosition(), support.ret_args(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop]).getPosition()));
    return yyVal;
  }
};
states[266] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ISourcePosition pos = ((Node)yyVals[0+yyTop]) == null ? lexer.getPosition() : ((Node)yyVals[0+yyTop]).getPosition();
                    yyVal = support.newArrayNode(pos, ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[397] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
    return yyVal;
  }
};
states[528] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("formal argument cannot be a global variable");
    return yyVal;
  }
};
states[332] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new RedoNode(((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
  }
};
states[463] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = lexer.getStrTerm();
                    lexer.setStrTerm(null);
                    lexer.setState(LexState.EXPR_BEG);
    return yyVal;
  }
};
states[70] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new NextNode(((Token)yyVals[-1+yyTop]).getPosition(), support.ret_args(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop]).getPosition()));
    return yyVal;
  }
};
states[201] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[-2+yyTop]));
                    ISourcePosition pos = ((Token)yyVals[-1+yyTop]).getPosition();
                    Node body = ((Node)yyVals[0+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop]);
                    Node rescue = new RescueNode(((Token)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-2+yyTop]), new RescueBodyNode(((Token)yyVals[-1+yyTop]).getPosition(), null, body, null), null);

                    pos = ((AssignableNode)yyVals[-4+yyTop]).getPosition();
                    String asgnOp = (String) ((Token)yyVals[-3+yyTop]).getValue();
                    if (asgnOp.equals("||")) {
                        ((AssignableNode)yyVals[-4+yyTop]).setValueNode(rescue);
                        yyVal = new OpAsgnOrNode(pos, support.gettable2(((AssignableNode)yyVals[-4+yyTop])), ((AssignableNode)yyVals[-4+yyTop]));
                    } else if (asgnOp.equals("&&")) {
                        ((AssignableNode)yyVals[-4+yyTop]).setValueNode(rescue);
                        yyVal = new OpAsgnAndNode(pos, support.gettable2(((AssignableNode)yyVals[-4+yyTop])), ((AssignableNode)yyVals[-4+yyTop]));
                    } else {
                        ((AssignableNode)yyVals[-4+yyTop]).setValueNode(support.getOperatorCallNode(support.gettable2(((AssignableNode)yyVals[-4+yyTop])), asgnOp, rescue));
                        ((AssignableNode)yyVals[-4+yyTop]).setPosition(pos);
                        yyVal = ((AssignableNode)yyVals[-4+yyTop]);
                    }
    return yyVal;
  }
};
states[267] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newSplatNode(((Token)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[398] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IterNode(support.getPosition(((Token)yyVals[-4+yyTop])), ((ArgsNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
    return yyVal;
  }
};
states[529] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("formal argument cannot be a class variable");
    return yyVal;
  }
};
states[5] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newline_node(((Node)yyVals[0+yyTop]), support.getPosition(((Node)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[595] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                      yyVal = null;
    return yyVal;
  }
};
states[333] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new RetryNode(((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
  }
};
states[464] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setStrTerm(((StrTerm)yyVals[-1+yyTop]));
                    yyVal = new EvStrNode(((Token)yyVals[-2+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[202] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
  /* FIXME: arg_concat missing for opt_call_args*/
                    yyVal = support.new_opElementAsgnNode(support.getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[399] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    /* Workaround for JRUBY-2326 (MRI does not enter this production for some reason)*/
                    if (((Node)yyVals[-1+yyTop]) instanceof YieldNode) {
                        throw new SyntaxException(PID.BLOCK_GIVEN_TO_YIELD, ((Node)yyVals[-1+yyTop]).getPosition(), lexer.getCurrentLine(), "block given to yield");
                    }
                    if (((Node)yyVals[-1+yyTop]) instanceof BlockAcceptingNode && ((BlockAcceptingNode)yyVals[-1+yyTop]).getIterNode() instanceof BlockPassNode) {
                        throw new SyntaxException(PID.BLOCK_ARG_AND_BLOCK_GIVEN, ((Node)yyVals[-1+yyTop]).getPosition(), lexer.getCurrentLine(), "Both block arg and actual block given.");
                    }
                    if (((Node)yyVals[-1+yyTop]) instanceof NonLocalControlFlowNode) {
                      yyVal = ((BlockAcceptingNode) ((NonLocalControlFlowNode)yyVals[-1+yyTop]).getValueNode()).setIterNode(((IterNode)yyVals[0+yyTop]));
                    } else {
                        yyVal = ((BlockAcceptingNode)yyVals[-1+yyTop]).setIterNode(((IterNode)yyVals[0+yyTop]));
                    }
                    ((Node)yyVal).setPosition(((Node)yyVals[-1+yyTop]).getPosition());
    return yyVal;
  }
};
states[6] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.appendToBlock(((Node)yyVals[-2+yyTop]), support.newline_node(((Node)yyVals[0+yyTop]), support.getPosition(((Node)yyVals[0+yyTop]))));
    return yyVal;
  }
};
states[268] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node node = support.splat_array(((Node)yyVals[-2+yyTop]));

                    if (node != null) {
                        yyVal = support.list_append(node, ((Node)yyVals[0+yyTop]));
                    } else {
                        yyVal = support.arg_append(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    }
    return yyVal;
  }
};
states[596] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = null;
    return yyVal;
  }
};
states[334] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = ((Node)yyVals[0+yyTop]);
                    if (yyVal == null) yyVal = NilImplicitNode.NIL;
    return yyVal;
  }
};
states[465] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = lexer.getStrTerm();
                   lexer.getConditionState().stop();
                   lexer.getCmdArgumentState().stop();
                   lexer.setStrTerm(null);
                   lexer.setState(LexState.EXPR_BEG);
    return yyVal;
  }
};
states[72] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[203] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new OpAsgnNode(support.getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
  }
};
states[400] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
  }
};
states[531] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.formal_argument(((Token)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[7] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[269] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
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
};
states[466] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.getConditionState().restart();
                   lexer.getCmdArgumentState().restart();
                   lexer.setStrTerm(((StrTerm)yyVals[-2+yyTop]));

                   yyVal = support.newEvStrNode(((Token)yyVals[-3+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[73] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((MultipleAsgn19Node)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[204] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new OpAsgnNode(support.getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
  }
};
states[401] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[532] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.arg_var(((Token)yyVals[0+yyTop]));
  /*
                    $$ = new ArgAuxiliaryNode($1.getPosition(), (String) $1.getValue(), 1);
  */
    return yyVal;
  }
};
states[270] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node node = support.splat_array(((Node)yyVals[-2+yyTop]));

                    if (node != null) {
                        yyVal = support.list_append(node, ((Node)yyVals[0+yyTop]));
                    } else {
                        yyVal = support.arg_append(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    }
    return yyVal;
  }
};
states[467] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = new GlobalVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
  }
};
states[74] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((Token)yyVals[-2+yyTop]).getPosition(), support.newArrayNode(((Token)yyVals[-2+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop])), null, null);
    return yyVal;
  }
};
states[205] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new OpAsgnNode(support.getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
  }
};
states[533] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                    /*            {
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
states[9] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("BEGIN in method");
                    }
    return yyVal;
  }
};
states[271] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node node = null;

                    if (((Node)yyVals[0+yyTop]) instanceof ArrayNode &&
                        (node = support.splat_array(((Node)yyVals[-3+yyTop]))) != null) {
                        yyVal = support.list_concat(node, ((Node)yyVals[0+yyTop]));
                    } else {
                        yyVal = support.arg_concat(((Node)yyVals[-3+yyTop]).getPosition(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
                    }
    return yyVal;
  }
};
states[402] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[468] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = new InstVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
  }
};
states[75] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((ListNode)yyVals[0+yyTop]).getPosition(), ((ListNode)yyVals[0+yyTop]), null, null);
    return yyVal;
  }
};
states[206] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("constant re-assignment");
    return yyVal;
  }
};
states[534] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ArrayNode(lexer.getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[10] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.getResult().addBeginNode(new PreExe19Node(((Token)yyVals[-4+yyTop]).getPosition(), support.getCurrentScope(), ((Node)yyVals[-1+yyTop])));
                    yyVal = null;
    return yyVal;
  }
};
states[272] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = support.newSplatNode(support.getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));  
    return yyVal;
  }
};
states[403] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_fcall(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
  }
};
states[76] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((ListNode)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]).add(((Node)yyVals[0+yyTop])), null, null);
    return yyVal;
  }
};
states[207] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("constant re-assignment");
    return yyVal;
  }
};
states[469] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = new ClassVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
  }
};
states[535] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                    yyVal = ((ListNode)yyVals[-2+yyTop]);
    return yyVal;
  }
};
states[11] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
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
};
states[404] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
  }
};
states[77] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((ListNode)yyVals[-2+yyTop]).getPosition(), ((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), (ListNode) null);
    return yyVal;
  }
};
states[208] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.backrefAssignError(((Node)yyVals[-2+yyTop]));
    return yyVal;
  }
};
states[536] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.arg_var(support.formal_argument(((Token)yyVals[-1+yyTop])));
                    yyVal = support.keyword_arg(((Token)yyVals[-1+yyTop]).getPosition(), support.assignable(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[12] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (((Node)yyVals[-1+yyTop]) instanceof BlockNode) {
                        support.checkUselessStatements(((BlockNode)yyVals[-1+yyTop]));
                    }
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[405] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
  }
};
states[78] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((ListNode)yyVals[-4+yyTop]).getPosition(), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-2+yyTop]), ((ListNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[209] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[-2+yyTop]));
                    support.checkExpression(((Node)yyVals[0+yyTop]));
    
                    boolean isLiteral = ((Node)yyVals[-2+yyTop]) instanceof FixnumNode && ((Node)yyVals[0+yyTop]) instanceof FixnumNode;
                    yyVal = new DotNode(support.getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), false, isLiteral);
    return yyVal;
  }
};
states[471] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     lexer.setState(LexState.EXPR_END);
                     yyVal = ((Token)yyVals[0+yyTop]);
                     ((ISourcePositionHolder)yyVal).setPosition(((Token)yyVals[-1+yyTop]).getPosition());
    return yyVal;
  }
};
states[537] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.arg_var(support.formal_argument(((Token)yyVals[-1+yyTop])));
                    yyVal = support.keyword_arg(((Token)yyVals[-1+yyTop]).getPosition(), support.assignable(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[406] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop]), null, null);
    return yyVal;
  }
};
states[79] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((ListNode)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), new StarNode(lexer.getPosition()), null);
    return yyVal;
  }
};
states[210] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[-2+yyTop]));
                    support.checkExpression(((Node)yyVals[0+yyTop]));

                    boolean isLiteral = ((Node)yyVals[-2+yyTop]) instanceof FixnumNode && ((Node)yyVals[0+yyTop]) instanceof FixnumNode;
                    yyVal = new DotNode(support.getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), true, isLiteral);
    return yyVal;
  }
};
states[341] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IfNode(((Token)yyVals[-4+yyTop]).getPosition(), support.getConditionNode(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[538] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[14] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newline_node(((Node)yyVals[0+yyTop]), support.getPosition(((Node)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[407] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop]), new Token("call", ((Node)yyVals[-2+yyTop]).getPosition()), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
  }
};
states[80] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((ListNode)yyVals[-3+yyTop]).getPosition(), ((ListNode)yyVals[-3+yyTop]), new StarNode(lexer.getPosition()), ((ListNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[211] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "+", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
  }
};
states[539] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[15] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.appendToBlock(((Node)yyVals[-2+yyTop]), support.newline_node(((Node)yyVals[0+yyTop]), support.getPosition(((Node)yyVals[0+yyTop]))));
    return yyVal;
  }
};
states[408] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop]), new Token("call", ((Node)yyVals[-2+yyTop]).getPosition()), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
  }
};
states[81] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((Token)yyVals[-1+yyTop]).getPosition(), null, ((Node)yyVals[0+yyTop]), null);
    return yyVal;
  }
};
states[212] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "-", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
  }
};
states[343] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[540] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[16] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[409] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[82] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((Token)yyVals[-3+yyTop]).getPosition(), null, ((Node)yyVals[-2+yyTop]), ((ListNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[213] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "*", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
  }
};
states[541] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[17] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[279] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = ((ListNode)yyVals[0+yyTop]); /* FIXME: Why complaining without $$ = $1;*/
    return yyVal;
  }
};
states[410] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ZSuperNode(((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
  }
};
states[83] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                      yyVal = new MultipleAsgn19Node(((Token)yyVals[0+yyTop]).getPosition(), null, new StarNode(lexer.getPosition()), null);
    return yyVal;
  }
};
states[214] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "/", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
  }
};
states[345] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
    return yyVal;
  }
};
states[476] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     lexer.setState(LexState.EXPR_END);

                     /* DStrNode: :"some text #{some expression}"*/
                     /* StrNode: :"some text"*/
                     /* EvStrNode :"#{some expression}"*/
                     /* Ruby 1.9 allows empty strings as symbols*/
                     if (((Node)yyVals[-1+yyTop]) == null) {
                         yyVal = new SymbolNode(((Token)yyVals[-2+yyTop]).getPosition(), "");
                     } else if (((Node)yyVals[-1+yyTop]) instanceof DStrNode) {
                         yyVal = new DSymbolNode(((Token)yyVals[-2+yyTop]).getPosition(), ((DStrNode)yyVals[-1+yyTop]));
                     } else if (((Node)yyVals[-1+yyTop]) instanceof StrNode) {
                         yyVal = new SymbolNode(((Token)yyVals[-2+yyTop]).getPosition(), ((StrNode)yyVals[-1+yyTop]).getValue().toString().intern());
                     } else {
                         yyVal = new DSymbolNode(((Token)yyVals[-2+yyTop]).getPosition());
                         ((DSymbolNode)yyVal).add(((Node)yyVals[-1+yyTop]));
                     }
    return yyVal;
  }
};
states[542] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[18] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   support.yyerror("BEGIN is permitted only at toplevel");
    return yyVal;
  }
};
states[280] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = ((ListNode)yyVals[0+yyTop]); /* FIXME: Why complaining without $$ = $1;*/
    return yyVal;
  }
};
states[411] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (((Node)yyVals[-3+yyTop]) instanceof SelfNode) {
                        yyVal = support.new_fcall(new Token("[]", support.getPosition(((Node)yyVals[-3+yyTop]))), ((Node)yyVals[-1+yyTop]), null);
                    } else {
                        yyVal = support.new_call(((Node)yyVals[-3+yyTop]), new Token("[]", support.getPosition(((Node)yyVals[-3+yyTop]))), ((Node)yyVals[-1+yyTop]), null);
                    }
    return yyVal;
  }
};
states[84] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                      yyVal = new MultipleAsgn19Node(((Token)yyVals[-2+yyTop]).getPosition(), null, new StarNode(lexer.getPosition()), ((ListNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[215] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "%", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
  }
};
states[346] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
    return yyVal;
  }
};
states[477] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[543] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[19] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BeginNode(support.getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-3+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-3+yyTop]));
    return yyVal;
  }
};
states[412] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
    return yyVal;
  }
};
states[216] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
  }
};
states[347] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[478] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = ((FloatNode)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[544] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[20] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setState(LexState.EXPR_FNAME);
    return yyVal;
  }
};
states[413] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IterNode(((Token)yyVals[-4+yyTop]).getPosition(), ((ArgsNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
    return yyVal;
  }
};
states[86] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[217] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), lexer.getPosition()), "-@");
    return yyVal;
  }
};
states[348] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[479] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = support.negateInteger(((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[545] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.arg_var(support.formal_argument(((Token)yyVals[-2+yyTop])));
                    yyVal = new OptArgNode(((Token)yyVals[-2+yyTop]).getPosition(), support.assignable(((Token)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[21] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newAlias(((Token)yyVals[-3+yyTop]).getPosition(), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[283] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new FCallNoArgNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
  }
};
states[414] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
    return yyVal;
  }
};
states[87] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(((Node)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[218] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((FloatNode)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), lexer.getPosition()), "-@");
    return yyVal;
  }
};
states[349] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[480] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = support.negateFloat(((FloatNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[546] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.arg_var(support.formal_argument(((Token)yyVals[-2+yyTop])));
                    yyVal = new OptArgNode(((Token)yyVals[-2+yyTop]).getPosition(), support.assignable(((Token)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[22] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new VAliasNode(((Token)yyVals[-2+yyTop]).getPosition(), (String) ((Token)yyVals[-1+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
  }
};
states[284] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BeginNode(support.getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[415] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IterNode(((Token)yyVals[-4+yyTop]).getPosition(), ((ArgsNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), support.getCurrentScope());
                    /* FIXME: What the hell is this?*/
                    ((ISourcePositionHolder)yyVals[-5+yyTop]).setPosition(support.getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])));
                    support.popCurrentScope();
    return yyVal;
  }
};
states[88] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[219] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "+@");
    return yyVal;
  }
};
states[350] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((ListNode)yyVals[0+yyTop]).getPosition(), ((ListNode)yyVals[0+yyTop]), null, null);
    return yyVal;
  }
};
states[547] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BlockNode(((Node)yyVals[0+yyTop]).getPosition()).add(((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[23] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new VAliasNode(((Token)yyVals[-2+yyTop]).getPosition(), (String) ((Token)yyVals[-1+yyTop]).getValue(), "$" + ((BackRefNode)yyVals[0+yyTop]).getType());
    return yyVal;
  }
};
states[285] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setState(LexState.EXPR_ENDARG);
    return yyVal;
  }
};
states[416] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newWhenNode(((Token)yyVals[-4+yyTop]).getPosition(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[89] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[220] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "-@");
    return yyVal;
  }
};
states[351] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((ListNode)yyVals[-3+yyTop]).getPosition(), ((ListNode)yyVals[-3+yyTop]), support.assignable(((Token)yyVals[0+yyTop]), null), null);
    return yyVal;
  }
};
states[548] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[24] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("can't make alias for the number variables");
    return yyVal;
  }
};
states[286] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null; /*FIXME: Should be implicit nil?*/
    return yyVal;
  }
};
states[90] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[221] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "|", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
  }
};
states[352] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((ListNode)yyVals[-5+yyTop]).getPosition(), ((ListNode)yyVals[-5+yyTop]), support.assignable(((Token)yyVals[-2+yyTop]), null), ((ListNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[549] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new BlockNode(((Node)yyVals[0+yyTop]).getPosition()).add(((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[25] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[287] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setState(LexState.EXPR_ENDARG); 
    return yyVal;
  }
};
states[91] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
    return yyVal;
  }
};
states[222] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "^", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
  }
};
states[353] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((ListNode)yyVals[-2+yyTop]).getPosition(), ((ListNode)yyVals[-2+yyTop]), new StarNode(lexer.getPosition()), null);
    return yyVal;
  }
};
states[550] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[26] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IfNode(support.getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), null);
    return yyVal;
  }
};
states[288] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.warning(ID.GROUPED_EXPRESSION, ((Token)yyVals[-3+yyTop]).getPosition(), "(...) interpreted as grouped expression");
                    yyVal = ((Node)yyVals[-2+yyTop]);
    return yyVal;
  }
};
states[419] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
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
};
states[92] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
    return yyVal;
  }
};
states[223] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "&", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
  }
};
states[354] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((ListNode)yyVals[-4+yyTop]).getPosition(), ((ListNode)yyVals[-4+yyTop]), new StarNode(lexer.getPosition()), ((ListNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[27] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IfNode(support.getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), null, ((Node)yyVals[-2+yyTop]));
    return yyVal;
  }
};
states[289] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (((Node)yyVals[-1+yyTop]) != null) {
                        /* compstmt position includes both parens around it*/
                        ((ISourcePositionHolder) ((Node)yyVals[-1+yyTop])).setPosition(((Token)yyVals[-2+yyTop]).getPosition());
                        yyVal = ((Node)yyVals[-1+yyTop]);
                    } else {
                        yyVal = new NilNode(((Token)yyVals[-2+yyTop]).getPosition());
                    }
    return yyVal;
  }
};
states[420] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null; 
    return yyVal;
  }
};
states[93] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.aryset(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[224] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=>", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
  }
};
states[355] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((Token)yyVals[-1+yyTop]).getPosition(), null, support.assignable(((Token)yyVals[0+yyTop]), null), null);
    return yyVal;
  }
};
states[486] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("nil", Tokens.kNIL, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
  }
};
states[28] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = new WhileNode(support.getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                    } else {
                        yyVal = new WhileNode(support.getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                    }
    return yyVal;
  }
};
states[290] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_colon2(support.getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
  }
};
states[421] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[94] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
  }
};
states[225] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
  }
};
states[356] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((Token)yyVals[-3+yyTop]).getPosition(), null, support.assignable(((Token)yyVals[-2+yyTop]), null), ((ListNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[487] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("self", Tokens.kSELF, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
  }
};
states[553] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (!support.is_local_id(((Token)yyVals[0+yyTop]))) {
                        support.yyerror("rest argument must be local variable");
                    }
                    
                    yyVal = new RestArgNode(support.arg_var(support.shadowing_lvar(((Token)yyVals[0+yyTop]))));
    return yyVal;
  }
};
states[29] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = new UntilNode(support.getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                    } else {
                        yyVal = new UntilNode(support.getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                    }
    return yyVal;
  }
};
states[291] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_colon3(((Token)yyVals[-1+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
  }
};
states[422] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.splat_array(((Node)yyVals[0+yyTop]));
                    if (yyVal == null) yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[95] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
  }
};
states[226] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">=", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
  }
};
states[357] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((Token)yyVals[0+yyTop]).getPosition(), null, new StarNode(lexer.getPosition()), null);
    return yyVal;
  }
};
states[488] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("true", Tokens.kTRUE, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
  }
};
states[554] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new UnnamedRestArgNode(((Token)yyVals[0+yyTop]).getPosition(), "", support.getCurrentScope().addVariable("*"));
    return yyVal;
  }
};
states[30] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node body = ((Node)yyVals[0+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop]);
                    yyVal = new RescueNode(support.getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), new RescueBodyNode(support.getPosition(((Node)yyVals[-2+yyTop])), null, body, null), null);
    return yyVal;
  }
};
states[292] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ISourcePosition position = ((Token)yyVals[-2+yyTop]).getPosition();
                    if (((Node)yyVals[-1+yyTop]) == null) {
                        yyVal = new ZArrayNode(position); /* zero length array */
                    } else {
                        yyVal = ((Node)yyVals[-1+yyTop]);
                        ((ISourcePositionHolder)yyVal).setPosition(position);
                    }
    return yyVal;
  }
};
states[96] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
  }
};
states[227] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
  }
};
states[358] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new MultipleAsgn19Node(((Token)yyVals[-2+yyTop]).getPosition(), null, null, ((ListNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[489] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("false", Tokens.kFALSE, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
  }
};
states[31] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        support.warn(ID.END_IN_METHOD, ((Token)yyVals[-3+yyTop]).getPosition(), "END in method; use at_exit");
                    }
                    yyVal = new PostExeNode(((Token)yyVals[-3+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[293] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Hash19Node(((Token)yyVals[-2+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[424] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[97] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = support.getPosition(((Node)yyVals[-2+yyTop]));

                    yyVal = new ConstDeclNode(position, null, support.new_colon2(position, ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
    return yyVal;
  }
};
states[228] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
  }
};
states[359] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args_tail(((ListNode)yyVals[-3+yyTop]).getPosition(), ((ListNode)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[490] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("__FILE__", Tokens.k__FILE__, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
  }
};
states[294] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ReturnNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
    return yyVal;
  }
};
states[98] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = ((Token)yyVals[-1+yyTop]).getPosition();

                    yyVal = new ConstDeclNode(position, null, support.new_colon3(position, (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
    return yyVal;
  }
};
states[229] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
  }
};
states[360] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args_tail(((ListNode)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[491] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("__LINE__", Tokens.k__LINE__, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
  }
};
states[557] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (!support.is_local_id(((Token)yyVals[0+yyTop]))) {
                        support.yyerror("block argument must be local variable");
                    }
                    
                    yyVal = new BlockArgNode(support.arg_var(support.shadowing_lvar(((Token)yyVals[0+yyTop]))));
    return yyVal;
  }
};
states[295] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_yield(((Token)yyVals[-3+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[426] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[33] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    ((MultipleAsgn19Node)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                    yyVal = ((MultipleAsgn19Node)yyVals[-2+yyTop]);
    return yyVal;
  }
};
states[99] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.backrefAssignError(((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[230] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "===", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
  }
};
states[361] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args_tail(((Token)yyVals[-1+yyTop]).getPosition(), null, ((Token)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[492] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new Token("__ENCODING__", Tokens.k__ENCODING__, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
  }
};
states[558] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((BlockArgNode)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[296] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ZYieldNode(((Token)yyVals[-2+yyTop]).getPosition());
    return yyVal;
  }
};
states[34] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
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
};
states[231] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "!=", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
  }
};
states[362] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args_tail(((BlockArgNode)yyVals[0+yyTop]).getPosition(), null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[493] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.gettable(((Token)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[100] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
    return yyVal;
  }
};
states[559] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[297] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ZYieldNode(((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
  }
};
states[35] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
  /* FIXME: arg_concat logic missing for opt_call_args*/
                    yyVal = support.new_opElementAsgnNode(support.getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[232] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                  /* ENEBO
                        $$ = match_op($1, $3);
                        if (nd_type($1) == NODE_LIT && TYPE($1->nd_lit) == T_REGEXP) {
                            $$ = reg_named_capture_assign($1->nd_lit, $$);
                        }
                  */
    return yyVal;
  }
};
states[363] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[494] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.gettable(((Token)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[101] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
    return yyVal;
  }
};
states[560] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (!(((Node)yyVals[0+yyTop]) instanceof SelfNode)) {
                        support.checkExpression(((Node)yyVals[0+yyTop]));
                    }
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[298] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new DefinedNode(((Token)yyVals[-4+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[429] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    /* FIXME: We may be intern'ing more than once.*/
                    yyVal = new SymbolNode(((Token)yyVals[0+yyTop]).getPosition(), ((String) ((Token)yyVals[0+yyTop]).getValue()).intern());
    return yyVal;
  }
};
states[36] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new OpAsgnNode(support.getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
  }
};
states[233] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "!~", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
  }
};
states[364] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args_tail(lexer.getPosition(), null, null, null);
    return yyVal;
  }
};
states[495] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
    return yyVal;
  }
};
states[102] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.aryset(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[561] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setState(LexState.EXPR_BEG);
    return yyVal;
  }
};
states[299] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(support.getConditionNode(((Node)yyVals[-1+yyTop])), "!");
    return yyVal;
  }
};
states[37] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new OpAsgnNode(support.getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
  }
};
states[365] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop]).getPosition(), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[496] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
    return yyVal;
  }
};
states[103] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
  }
};
states[234] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(support.getConditionNode(((Node)yyVals[0+yyTop])), "!");
    return yyVal;
  }
};
states[562] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (((Node)yyVals[-1+yyTop]) == null) {
                        support.yyerror("can't define single method for ().");
                    } else if (((Node)yyVals[-1+yyTop]) instanceof ILiteralNode) {
                        support.yyerror("can't define single method for literals.");
                    }
                    support.checkExpression(((Node)yyVals[-1+yyTop]));
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[300] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(NilImplicitNode.NIL, "!");
    return yyVal;
  }
};
states[431] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]) instanceof EvStrNode ? new DStrNode(((Node)yyVals[0+yyTop]).getPosition(), lexer.getEncoding()).add(((Node)yyVals[0+yyTop])) : ((Node)yyVals[0+yyTop]);
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
states[38] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("can't make alias for the number variables");
                    yyVal = null;
    return yyVal;
  }
};
states[366] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-7+yyTop]).getPosition(), ((ListNode)yyVals[-7+yyTop]), ((ListNode)yyVals[-5+yyTop]), ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[497] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[104] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
  }
};
states[235] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "~");
    return yyVal;
  }
};
states[563] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ArrayNode(lexer.getPosition());
    return yyVal;
  }
};
states[432] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ByteList aChar = ByteList.create((String) ((Token)yyVals[0+yyTop]).getValue());
                    aChar.setEncoding(lexer.getEncoding());
                    yyVal = lexer.createStrNode(((Token)yyVals[-1+yyTop]).getPosition(), aChar, 0);
    return yyVal;
  }
};
states[39] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new OpAsgnNode(support.getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
  }
};
states[301] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new FCallNoArgBlockNode(((Token)yyVals[-1+yyTop]).getPosition(), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((IterNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[367] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop]).getPosition(), ((ListNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[498] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[105] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
  }
};
states[236] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<<", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
  }
};
states[564] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[433] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[40] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.backrefAssignError(((Node)yyVals[-2+yyTop]));
    return yyVal;
  }
};
states[499] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[106] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = support.getPosition(((Node)yyVals[-2+yyTop]));

                    yyVal = new ConstDeclNode(position, null, support.new_colon2(position, ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
    return yyVal;
  }
};
states[237] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">>", ((Node)yyVals[0+yyTop]), lexer.getPosition());
    return yyVal;
  }
};
states[368] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop]).getPosition(), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), null, ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[434] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.literal_concat(((Node)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[41] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[303] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (((Node)yyVals[-1+yyTop]) != null && 
                          ((BlockAcceptingNode)yyVals[-1+yyTop]).getIterNode() instanceof BlockPassNode) {
                        throw new SyntaxException(PID.BLOCK_ARG_AND_BLOCK_GIVEN, ((Node)yyVals[-1+yyTop]).getPosition(), lexer.getCurrentLine(), "Both block arg and actual block given.");
                    }
                    yyVal = ((BlockAcceptingNode)yyVals[-1+yyTop]).setIterNode(((IterNode)yyVals[0+yyTop]));
                    ((Node)yyVal).setPosition(((Node)yyVals[-1+yyTop]).getPosition());
    return yyVal;
  }
};
states[500] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.setState(LexState.EXPR_BEG);
    return yyVal;
  }
};
states[107] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = ((Token)yyVals[-1+yyTop]).getPosition();

                    yyVal = new ConstDeclNode(position, null, support.new_colon3(position, (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
    return yyVal;
  }
};
states[238] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newAndNode(((Token)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[369] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop]).getPosition(), ((ListNode)yyVals[-3+yyTop]), null, ((RestArgNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[566] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).addAll(((ListNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[42] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ((MultipleAsgn19Node)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                    yyVal = ((MultipleAsgn19Node)yyVals[-2+yyTop]);
    return yyVal;
  }
};
states[304] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((LambdaNode)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[435] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
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
};
states[501] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[108] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.backrefAssignError(((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[239] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newOrNode(((Token)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[370] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    RestArgNode rest = new UnnamedRestArgNode(((ListNode)yyVals[-1+yyTop]).getPosition(), null, support.getCurrentScope().addVariable("*"));
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, rest, null, (ArgsTailHolder) null);
    return yyVal;
  }
};
states[567] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ISourcePosition pos;
                    if (((Node)yyVals[-2+yyTop]) == null && ((Node)yyVals[0+yyTop]) == null) {
                        pos = ((Token)yyVals[-1+yyTop]).getPosition();
                    } else {
                        pos = ((Node)yyVals[-2+yyTop]).getPosition();
                    }

                    yyVal = support.newArrayNode(pos, ((Node)yyVals[-2+yyTop])).add(((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[43] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                    yyVal = ((MultipleAsgn19Node)yyVals[-2+yyTop]);
                    ((MultipleAsgn19Node)yyVals[-2+yyTop]).setPosition(support.getPosition(((MultipleAsgn19Node)yyVals[-2+yyTop])));
    return yyVal;
  }
};
states[305] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IfNode(((Token)yyVals[-5+yyTop]).getPosition(), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[436] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
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
};
states[109] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.yyerror("class/module name must be CONSTANT");
    return yyVal;
  }
};
states[240] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new DefinedNode(((Token)yyVals[-2+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[371] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop]).getPosition(), ((ListNode)yyVals[-5+yyTop]), null, ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[502] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = null;
    return yyVal;
  }
};
states[568] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ISourcePosition pos = ((Token)yyVals[-1+yyTop]).getPosition();
                    yyVal = support.newArrayNode(pos, new SymbolNode(pos, (String) ((Token)yyVals[-1+yyTop]).getValue())).add(((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[306] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IfNode(((Token)yyVals[-5+yyTop]).getPosition(), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-2+yyTop]));
    return yyVal;
  }
};
states[437] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newRegexpNode(((Token)yyVals[-2+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]), (RegexpNode) ((RegexpNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[241] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IfNode(support.getPosition(((Node)yyVals[-5+yyTop])), support.getConditionNode(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[372] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[503] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ArgsNode)yyVals[-1+yyTop]);
                    ((ISourcePositionHolder)yyVal).setPosition(((Token)yyVals[-2+yyTop]).getPosition());
                    lexer.setState(LexState.EXPR_BEG);
                    lexer.commandStart = true;
    return yyVal;
  }
};
states[569] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(((Token)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[0+yyTop])).add(null);
    return yyVal;
  }
};
states[45] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[307] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.getConditionState().begin();
    return yyVal;
  }
};
states[438] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ZArrayNode(((Token)yyVals[-2+yyTop]).getPosition());
    return yyVal;
  }
};
states[111] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_colon3(((Token)yyVals[-1+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
  }
};
states[242] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[373] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(support.getPosition(((ListNode)yyVals[-3+yyTop])), null, ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[504] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ArgsNode)yyVals[-1+yyTop]);
                    lexer.setState(LexState.EXPR_BEG);
                    lexer.commandStart = true;
    return yyVal;
  }
};
states[46] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[308] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.getConditionState().end();
    return yyVal;
  }
};
states[439] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[112] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_colon2(((Token)yyVals[0+yyTop]).getPosition(), null, (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
  }
};
states[243] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = ((Node)yyVals[0+yyTop]) != null ? ((Node)yyVals[0+yyTop]) : NilImplicitNode.NIL;
    return yyVal;
  }
};
states[374] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(support.getPosition(((ListNode)yyVals[-5+yyTop])), null, ((ListNode)yyVals[-5+yyTop]), ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[505] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args_tail(((ListNode)yyVals[-3+yyTop]).getPosition(), ((ListNode)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[309] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);
                    yyVal = new WhileNode(((Token)yyVals[-6+yyTop]).getPosition(), support.getConditionNode(((Node)yyVals[-4+yyTop])), body);
    return yyVal;
  }
};
states[440] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ArrayNode(lexer.getPosition());
    return yyVal;
  }
};
states[113] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_colon2(support.getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
  }
};
states[375] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(support.getPosition(((ListNode)yyVals[-1+yyTop])), null, ((ListNode)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[506] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args_tail(((ListNode)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[48] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newAndNode(((Token)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[310] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.getConditionState().begin();
    return yyVal;
  }
};
states[441] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]) instanceof EvStrNode ? new DStrNode(((ListNode)yyVals[-2+yyTop]).getPosition(), lexer.getEncoding()).add(((Node)yyVals[-1+yyTop])) : ((Node)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[245] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[376] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop]).getPosition(), null, ((ListNode)yyVals[-3+yyTop]), null, ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[507] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args_tail(((Token)yyVals[-1+yyTop]).getPosition(), null, ((Token)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[49] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newOrNode(((Token)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[311] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.getConditionState().end();
    return yyVal;
  }
};
states[246] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.arg_append(((Node)yyVals[-3+yyTop]), new Hash19Node(lexer.getPosition(), ((ListNode)yyVals[-1+yyTop])));
    return yyVal;
  }
};
states[377] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((RestArgNode)yyVals[-1+yyTop]).getPosition(), null, null, ((RestArgNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[508] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args_tail(((BlockArgNode)yyVals[0+yyTop]).getPosition(), null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[50] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(support.getConditionNode(((Node)yyVals[0+yyTop])), "!");
    return yyVal;
  }
};
states[312] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);
                    yyVal = new UntilNode(((Token)yyVals[-6+yyTop]).getPosition(), support.getConditionNode(((Node)yyVals[-4+yyTop])), body);
    return yyVal;
  }
};
states[443] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = support.literal_concat(support.getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[247] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(((ListNode)yyVals[-1+yyTop]).getPosition(), new Hash19Node(lexer.getPosition(), ((ListNode)yyVals[-1+yyTop])));
    return yyVal;
  }
};
states[378] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((RestArgNode)yyVals[-3+yyTop]).getPosition(), null, null, ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[509] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[51] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.getOperatorCallNode(support.getConditionNode(((Node)yyVals[0+yyTop])), "!");
    return yyVal;
  }
};
states[313] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newCaseNode(((Token)yyVals[-4+yyTop]).getPosition(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[444] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ArrayNode(lexer.getPosition());
    return yyVal;
  }
};
states[117] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.setState(LexState.EXPR_ENDFN);
                   yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[248] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                    if (yyVal != null) ((Node)yyVal).setPosition(((Token)yyVals[-2+yyTop]).getPosition());
    return yyVal;
  }
};
states[379] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ArgsTailHolder)yyVals[0+yyTop]).getPosition(), null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[510] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args_tail(lexer.getPosition(), null, null, null);
    return yyVal;
  }
};
states[314] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newCaseNode(((Token)yyVals[-3+yyTop]).getPosition(), null, ((Node)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[445] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[118] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.setState(LexState.EXPR_ENDFN);
                   yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[380] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
    /* was $$ = null;*/
                    yyVal = support.new_args(lexer.getPosition(), null, null, null, null, (ArgsTailHolder) null);
    return yyVal;
  }
};
states[511] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop]).getPosition(), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[53] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[315] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.getConditionState().begin();
    return yyVal;
  }
};
states[446] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ArrayNode(lexer.getPosition());
    return yyVal;
  }
};
states[119] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new LiteralNode(((Token)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[381] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.commandStart = true;
                    yyVal = ((ArgsNode)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[512] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-7+yyTop]).getPosition(), ((ListNode)yyVals[-7+yyTop]), ((ListNode)yyVals[-5+yyTop]), ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[316] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.getConditionState().end();
    return yyVal;
  }
};
states[447] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]) instanceof EvStrNode ? new DSymbolNode(((ListNode)yyVals[-2+yyTop]).getPosition()).add(((Node)yyVals[-1+yyTop])) : support.asSymbol(((ListNode)yyVals[-2+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop])));
    return yyVal;
  }
};
states[120] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new LiteralNode(((Token)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[382] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((Token)yyVals[-2+yyTop]).getPosition(), null, null, null, null, (ArgsTailHolder) null);
    return yyVal;
  }
};
states[513] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop]).getPosition(), ((ListNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[317] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                      /* ENEBO: Lots of optz in 1.9 parser here*/
                    yyVal = new ForNode(((Token)yyVals[-8+yyTop]).getPosition(), ((Node)yyVals[-7+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-4+yyTop]), support.getCurrentScope());
    return yyVal;
  }
};
states[448] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                     yyVal = new ZArrayNode(((Token)yyVals[-2+yyTop]).getPosition());
    return yyVal;
  }
};
states[121] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((LiteralNode)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[383] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((Token)yyVals[0+yyTop]).getPosition(), null, null, null, null, (ArgsTailHolder) null);
    return yyVal;
  }
};
states[514] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop]).getPosition(), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), null, ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[318] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("class definition in method body");
                    }
                    support.pushLocalScope();
    return yyVal;
  }
};
states[449] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                    ((ISourcePositionHolder)yyVal).setPosition(((Token)yyVals[-2+yyTop]).getPosition());
    return yyVal;
  }
};
states[122] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[253] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
  }
};
states[384] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ArgsNode)yyVals[-2+yyTop]);
    return yyVal;
  }
};
states[515] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop]).getPosition(), ((ListNode)yyVals[-3+yyTop]), null, ((RestArgNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[57] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
  }
};
states[319] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);

                    yyVal = new ClassNode(((Token)yyVals[-5+yyTop]).getPosition(), ((Colon3Node)yyVals[-4+yyTop]), support.getCurrentScope(), body, ((Node)yyVals[-3+yyTop]));
                    support.popCurrentScope();
    return yyVal;
  }
};
states[450] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ZArrayNode(((Token)yyVals[-2+yyTop]).getPosition());
    return yyVal;
  }
};
states[123] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newUndef(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[254] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.arg_append(((Node)yyVals[-3+yyTop]), new Hash19Node(lexer.getPosition(), ((ListNode)yyVals[-1+yyTop])));
    return yyVal;
  }
};
states[385] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[516] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop]).getPosition(), ((ListNode)yyVals[-5+yyTop]), null, ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[58] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
    return yyVal;
  }
};
states[320] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = Boolean.valueOf(support.isInDef());
                    support.setInDef(false);
    return yyVal;
  }
};
states[451] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                    ((ISourcePositionHolder)yyVal).setPosition(((Token)yyVals[-2+yyTop]).getPosition());
    return yyVal;
  }
};
states[124] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    lexer.setState(LexState.EXPR_FNAME);
    return yyVal;
  }
};
states[255] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(((ListNode)yyVals[-1+yyTop]).getPosition(), new Hash19Node(lexer.getPosition(), ((ListNode)yyVals[-1+yyTop])));
    return yyVal;
  }
};
states[386] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[517] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[59] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IterNode(((Token)yyVals[-4+yyTop]).getPosition(), ((ArgsNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
    return yyVal;
  }
};
states[321] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = Integer.valueOf(support.getInSingle());
                    support.setInSingle(0);
                    support.pushLocalScope();
    return yyVal;
  }
};
states[452] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ArrayNode(lexer.getPosition());
    return yyVal;
  }
};
states[125] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.appendToBlock(((Node)yyVals[-3+yyTop]), support.newUndef(((Node)yyVals[-3+yyTop]).getPosition(), ((Node)yyVals[0+yyTop])));
    return yyVal;
  }
};
states[256] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(support.getPosition(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[387] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[518] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop]).getPosition(), null, ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[60] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_fcall(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
  }
};
states[322] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new SClassNode(((Token)yyVals[-7+yyTop]).getPosition(), ((Node)yyVals[-5+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                    support.popCurrentScope();
                    support.setInDef(((Boolean)yyVals[-4+yyTop]).booleanValue());
                    support.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
    return yyVal;
  }
};
states[453] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
    return yyVal;
  }
};
states[257] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.arg_blk_pass(((Node)yyVals[-1+yyTop]), ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[388] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[519] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop]).getPosition(), null, ((ListNode)yyVals[-5+yyTop]), ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[61] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_fcall(((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[323] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    if (support.isInDef() || support.isInSingle()) { 
                        support.yyerror("module definition in method body");
                    }
                    support.pushLocalScope();
    return yyVal;
  }
};
states[454] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ArrayNode(lexer.getPosition());
    return yyVal;
  }
};
states[258] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.newArrayNode(((ListNode)yyVals[-1+yyTop]).getPosition(), new Hash19Node(lexer.getPosition(), ((ListNode)yyVals[-1+yyTop])));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[389] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.new_bv(((Token)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[520] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop]).getPosition(), null, ((ListNode)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[586] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[62] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
  }
};
states[324] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);

                    yyVal = new ModuleNode(((Token)yyVals[-4+yyTop]).getPosition(), ((Colon3Node)yyVals[-3+yyTop]), support.getCurrentScope(), body);
                    support.popCurrentScope();
    return yyVal;
  }
};
states[455] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(support.asSymbol(((ListNode)yyVals[-2+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop])));
    return yyVal;
  }
};
states[259] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.arg_append(((Node)yyVals[-3+yyTop]), new Hash19Node(lexer.getPosition(), ((ListNode)yyVals[-1+yyTop])));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[390] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = null;
    return yyVal;
  }
};
states[521] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop]).getPosition(), null, ((ListNode)yyVals[-3+yyTop]), null, ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[587] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
  }
};
states[63] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])); 
    return yyVal;
  }
};
states[325] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.setInDef(true);
                    support.pushLocalScope();
    return yyVal;
  }
};
states[456] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    ByteList aChar = ByteList.create("");
                    aChar.setEncoding(lexer.getEncoding());
                    yyVal = lexer.createStrNode(((Token)yyVals[0+yyTop]).getPosition(), aChar, 0);
    return yyVal;
  }
};
states[260] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
    return yyVal;
  }
};
states[391] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
                    yyVal = lexer.getLeftParenBegin();
                    lexer.setLeftParenBegin(lexer.incrementParenNest());
    return yyVal;
  }
};
states[522] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((RestArgNode)yyVals[-1+yyTop]).getPosition(), null, null, ((RestArgNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[64] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
  }
};
states[326] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    /* TODO: We should use implicit nil for body, but problem (punt til later)*/
                    Node body = ((Node)yyVals[-1+yyTop]); /*$5 == null ? NilImplicitNode.NIL : $5;*/

                    yyVal = new DefnNode(((Token)yyVals[-5+yyTop]).getPosition(), new ArgumentNode(((Token)yyVals[-4+yyTop]).getPosition(), (String) ((Token)yyVals[-4+yyTop]).getValue()), ((ArgsNode)yyVals[-2+yyTop]), support.getCurrentScope(), body);
                    support.popCurrentScope();
                    support.setInDef(false);
    return yyVal;
  }
};
states[457] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.literal_concat(((Node)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
  }
};
states[261] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = Long.valueOf(lexer.getCmdArgumentState().begin());
    return yyVal;
  }
};
states[392] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new LambdaNode(((ArgsNode)yyVals[-1+yyTop]).getPosition(), ((ArgsNode)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
                    lexer.setLeftParenBegin(((Integer)yyVals[-2+yyTop]));
    return yyVal;
  }
};
states[523] = new ParserState() {
  @Override public Object execute(ParserSupport support, RubyYaccLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_args(((RestArgNode)yyVals[-3+yyTop]).getPosition(), null, null, ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
    return yyVal;
  }
};
}
					// line 2215 "Ruby20Parser.y"

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

        yyparse(lexer, configuration.isDebug() ? new YYDebug() : null);
        
        return support.getResult();
    }
}
					// line 8667 "-"
