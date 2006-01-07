// created by jay 1.0.2 (c) 2002-2004 ats@cs.rit.edu
// skeleton Java 1.0 (c) 2002 ats@cs.rit.edu

					// line 2 "DefaultRubyParser.y"
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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
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
import java.math.BigInteger;

import org.jruby.ast.AliasNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArgumentNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.AssignableNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BignumNode;
import org.jruby.ast.BlockArgNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.BlockPassNode;
import org.jruby.ast.BreakNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.CaseNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.Colon3Node;
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
import org.jruby.ast.FCallNode;
import org.jruby.ast.FalseNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FloatNode;
import org.jruby.ast.ForNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.IfNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.NilNode;
import org.jruby.ast.Node;
import org.jruby.ast.NotNode;
import org.jruby.ast.OpAsgnAndNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OpElementAsgnNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.RedoNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.RetryNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SValueNode;
import org.jruby.ast.ScopeNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.StarNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.ToAryNode;
import org.jruby.ast.TrueNode;
import org.jruby.ast.UndefNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.VAliasNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.WhenNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.ZArrayNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.ast.ZeroArgNode;
import org.jruby.ast.types.ILiteralNode;
import org.jruby.ast.types.INameNode;
import org.jruby.common.IRubyWarnings;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.ISourcePositionHolder;
import org.jruby.lexer.yacc.LexState;
import org.jruby.lexer.yacc.LexerSource;
import org.jruby.lexer.yacc.RubyYaccLexer;
import org.jruby.lexer.yacc.StrTerm;
import org.jruby.lexer.yacc.SyntaxException;
import org.jruby.lexer.yacc.Token;
import org.jruby.runtime.Visibility;
import org.jruby.util.IdUtil;

public class DefaultRubyParser {
    private ParserSupport support;
    private RubyYaccLexer lexer;
    private IRubyWarnings warnings;

    public DefaultRubyParser() {
        support = new ParserSupport();
        lexer = new RubyYaccLexer();
        lexer.setParserSupport(support);
    }

    public void setWarnings(IRubyWarnings warnings) {
        this.warnings = warnings;

        support.setWarnings(warnings);
        lexer.setWarnings(warnings);
    }

/*
%union {
    Node *node;
    VALUE val;
    ID id;
    int num;
    struct RVarmap *vars;
}
*/
					// line 163 "-"
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
  public static final int tIDENTIFIER = 304;
  public static final int tFID = 305;
  public static final int tGVAR = 306;
  public static final int tIVAR = 307;
  public static final int tCONSTANT = 308;
  public static final int tCVAR = 309;
  public static final int tSTRING_CONTENT = 310;
  public static final int tINTEGER = 311;
  public static final int tFLOAT = 312;
  public static final int tNTH_REF = 313;
  public static final int tBACK_REF = 314;
  public static final int tREGEXP_END = 315;
  public static final int tUPLUS = 316;
  public static final int tUMINUS = 317;
  public static final int tUMINUS_NUM = 318;
  public static final int tPOW = 319;
  public static final int tCMP = 320;
  public static final int tEQ = 321;
  public static final int tEQQ = 322;
  public static final int tNEQ = 323;
  public static final int tGEQ = 324;
  public static final int tLEQ = 325;
  public static final int tANDOP = 326;
  public static final int tOROP = 327;
  public static final int tMATCH = 328;
  public static final int tNMATCH = 329;
  public static final int tDOT = 330;
  public static final int tDOT2 = 331;
  public static final int tDOT3 = 332;
  public static final int tAREF = 333;
  public static final int tASET = 334;
  public static final int tLSHFT = 335;
  public static final int tRSHFT = 336;
  public static final int tCOLON2 = 337;
  public static final int tCOLON3 = 338;
  public static final int tOP_ASGN = 339;
  public static final int tASSOC = 340;
  public static final int tLPAREN = 341;
  public static final int tLPAREN2 = 342;
  public static final int tLPAREN_ARG = 343;
  public static final int tLBRACK = 344;
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
  public static final int tBACK_REF2 = 362;
  public static final int tSYMBEG = 363;
  public static final int tSTRING_BEG = 364;
  public static final int tXSTRING_BEG = 365;
  public static final int tREGEXP_BEG = 366;
  public static final int tWORDS_BEG = 367;
  public static final int tQWORDS_BEG = 368;
  public static final int tSTRING_DBEG = 369;
  public static final int tSTRING_DVAR = 370;
  public static final int tSTRING_END = 371;
  public static final int tLOWEST = 372;
  public static final int tLAST_TOKEN = 373;
  public static final int yyErrorCode = 256;

  /** number of final state.
    */
  protected static final int yyFinal = 1;

  /** parser tables.
      Order is mandated by <i>jay</i>.
    */
  protected static final short[] yyLhs = {
//yyLhs 495
    -1,   100,     0,    18,    17,    19,    19,    19,    19,   103,
    20,    20,    20,    20,    20,    20,    20,    20,    20,    20,
   104,    20,    20,    20,    20,    20,    20,    20,    20,    20,
    20,    20,    20,    20,    20,    21,    21,    21,    21,    21,
    21,    29,    25,    25,    25,    25,    25,    48,    48,    48,
   105,    63,    24,    24,    24,    24,    24,    24,    24,    24,
    69,    69,    71,    71,    70,    70,    70,    70,    70,    70,
    65,    65,    74,    74,    66,    66,    66,    66,    66,    66,
    66,    66,    59,    59,    59,    59,    59,    59,    59,    59,
    91,    91,    16,    16,    16,    92,    92,    92,    92,    92,
    85,    85,    54,   107,    54,    93,    93,    93,    93,    93,
    93,    93,    93,    93,    93,    93,    93,    93,    93,    93,
    93,    93,    93,    93,    93,    93,    93,    93,    93,    93,
    93,   106,   106,   106,   106,   106,   106,   106,   106,   106,
   106,   106,   106,   106,   106,   106,   106,   106,   106,   106,
   106,   106,   106,   106,   106,   106,   106,   106,   106,   106,
   106,   106,   106,   106,   106,   106,   106,   106,   106,   106,
   106,   106,    22,    22,    22,    22,    22,    22,    22,    22,
    22,    22,    22,    22,    22,    22,    22,    22,    22,    22,
    22,    22,    22,    22,    22,    22,    22,    22,    22,    22,
    22,    22,    22,    22,    22,    22,    22,    22,    22,    22,
    22,    22,    22,   109,    22,    22,    22,    67,    78,    78,
    78,    78,    78,    78,    36,    36,    36,    36,    37,    37,
    38,    38,    38,    38,    38,    38,    38,    38,    38,    39,
    39,    39,    39,    39,    39,    39,    39,    39,    39,    39,
    39,   111,    41,    40,   112,    40,   113,    40,    44,    43,
    43,    72,    72,    64,    64,    64,    23,    23,    23,    23,
    23,    23,    23,    23,    23,    23,    23,    23,    23,    23,
    23,    23,    23,    23,    23,    23,    23,   114,    23,    23,
    23,    23,    23,    23,   116,   118,    23,   119,   120,    23,
    23,    23,    23,   121,   122,    23,   123,    23,   125,   126,
    23,   127,    23,   128,    23,   129,   130,    23,    23,    23,
    23,    23,    30,   115,   115,   115,   115,   117,   117,   117,
    33,    33,    31,    31,    57,    57,    58,    58,    58,    58,
   131,    62,    47,    47,    47,    26,    26,    26,    26,    26,
    26,   132,    61,   133,    61,    68,    73,    73,    73,    32,
    32,    79,    79,    77,    77,    77,    34,    34,    35,    35,
    13,    13,    13,     2,     3,     3,     4,     5,     6,    10,
    10,    28,    28,    12,    12,    11,    11,    27,    27,     7,
     7,     8,     8,     9,   134,     9,   135,     9,    55,    55,
    55,    55,    87,    86,    86,    86,    86,    15,    14,    14,
    14,    14,    80,    80,    80,    80,    80,    80,    80,    80,
    80,    80,    80,    42,    81,    56,    56,    46,   136,    46,
    46,    51,    51,    52,    52,    52,    52,    52,    52,    52,
    52,    52,    94,    94,    94,    94,    96,    96,    53,    84,
    84,    98,    98,    95,    95,    99,    99,    50,    49,    49,
     1,   137,     1,    83,    83,    83,    75,    75,    76,    88,
    88,    88,    89,    89,    89,    89,    90,    90,    90,    97,
    97,   101,   101,   108,   108,   110,   110,   110,   124,   124,
   102,   102,    60,    82,    45,
    }, yyLen = {
//yyLen 495
     2,     0,     2,     4,     2,     1,     1,     3,     2,     0,
     4,     3,     3,     3,     2,     3,     3,     3,     3,     3,
     0,     5,     4,     3,     3,     3,     6,     5,     5,     5,
     3,     3,     3,     3,     1,     1,     3,     3,     2,     2,
     1,     1,     1,     1,     2,     2,     2,     1,     4,     4,
     0,     5,     2,     3,     4,     5,     4,     5,     2,     2,
     1,     3,     1,     3,     1,     2,     3,     2,     2,     1,
     1,     3,     2,     3,     1,     4,     3,     3,     3,     3,
     2,     1,     1,     4,     3,     3,     3,     3,     2,     1,
     1,     1,     2,     1,     3,     1,     1,     1,     1,     1,
     1,     1,     1,     0,     4,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     3,     5,     3,     6,     5,     5,     5,     5,
     4,     3,     3,     3,     3,     3,     3,     3,     3,     3,
     4,     4,     2,     2,     3,     3,     3,     3,     3,     3,
     3,     3,     3,     3,     3,     3,     3,     2,     2,     3,
     3,     3,     3,     0,     4,     5,     1,     1,     1,     2,
     2,     5,     2,     3,     3,     4,     4,     6,     1,     1,
     1,     2,     5,     2,     5,     4,     7,     3,     1,     4,
     3,     5,     7,     2,     5,     4,     6,     7,     9,     3,
     1,     0,     2,     1,     0,     3,     0,     4,     2,     2,
     1,     1,     3,     3,     4,     2,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     3,     4,     3,     3,     2,
     4,     3,     3,     1,     4,     3,     1,     0,     6,     2,
     1,     2,     6,     6,     0,     0,     7,     0,     0,     7,
     5,     4,     5,     0,     0,     9,     0,     6,     0,     0,
     8,     0,     5,     0,     6,     0,     0,     9,     1,     1,
     1,     1,     1,     1,     1,     1,     2,     1,     1,     1,
     1,     5,     1,     2,     1,     1,     1,     2,     1,     3,
     0,     5,     2,     4,     4,     2,     4,     4,     3,     2,
     1,     0,     5,     0,     5,     5,     1,     4,     2,     1,
     1,     6,     0,     1,     1,     1,     2,     1,     2,     1,
     1,     1,     1,     1,     1,     2,     3,     3,     3,     3,
     3,     0,     3,     1,     2,     3,     3,     0,     3,     0,
     2,     0,     2,     1,     0,     3,     0,     4,     1,     1,
     1,     1,     2,     1,     1,     1,     1,     3,     1,     1,
     2,     2,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     0,     4,
     2,     4,     2,     6,     4,     4,     2,     4,     2,     2,
     1,     0,     1,     1,     1,     1,     1,     3,     3,     1,
     3,     1,     1,     2,     1,     1,     1,     2,     2,     0,
     1,     0,     5,     1,     2,     2,     1,     3,     3,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     0,     1,     0,     1,     0,     1,     1,     1,     1,
     1,     2,     0,     0,     0,
    }, yyDefRed = {
//yyDefRed 885
     1,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   294,   297,     0,     0,     0,   320,   321,     0,
     0,     0,   418,   417,   419,   420,     0,     0,     0,    20,
     0,   422,   421,     0,     0,   414,   413,     0,   416,   408,
   409,   425,   426,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   389,   391,   391,     0,     0,
   267,     0,   374,   268,   269,   270,   271,   266,   370,   372,
     2,     0,     0,     0,     0,     0,     0,    35,     0,     0,
   272,     0,    43,     0,     0,     5,     0,    70,     0,    60,
     0,     0,     0,   371,     0,     0,   318,   319,   283,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   322,
     0,   273,   423,     0,    93,   311,   140,   151,   141,   164,
   137,   157,   147,   146,   162,   145,   144,   139,   165,   149,
   138,   152,   156,   158,   150,   143,   159,   166,   161,     0,
     0,     0,     0,   136,   155,   154,   167,   168,   169,   170,
   171,   135,   142,   133,   134,     0,     0,     0,    97,     0,
   126,   127,   124,   108,   109,   110,   113,   115,   111,   128,
   129,   116,   117,   461,   121,   120,   107,   125,   123,   122,
   118,   119,   114,   112,   105,   106,   130,     0,   460,   313,
    98,    99,   160,   153,   163,   148,   131,   132,    95,    96,
     0,     0,   102,   101,   100,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   489,   488,     0,     0,
     0,   490,     0,     0,     0,     0,     0,     0,   334,   335,
     0,     0,     0,     0,     0,   230,    45,   238,     0,     0,
     0,   466,    46,    44,     0,    59,     0,     0,   349,    58,
    38,     0,     9,   484,     0,     0,     0,   192,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   218,     0,     0,     0,     0,     0,   463,     0,     0,     0,
     0,    68,     0,   208,   207,    39,   405,   404,   406,     0,
   402,   403,     0,     0,     0,     0,     0,     0,     0,   375,
     4,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   340,   342,   353,   351,   291,
     0,     0,     0,     0,     0,     0,     0,    72,     0,     0,
     0,     0,     0,   345,     0,   289,     0,   410,   411,     0,
    90,     0,    92,     0,   428,   306,   427,     0,     0,     0,
     0,     0,   479,   480,   315,     0,   103,     0,     0,   275,
     0,   325,   324,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   491,     0,     0,     0,
     0,     0,     0,   303,     0,   258,     0,     0,   231,   260,
     0,   233,   285,     0,     0,   253,   252,     0,     0,     0,
     0,     0,    11,    13,    12,     0,   287,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   277,     0,     0,     0,
   219,     0,   486,   220,     0,   222,   281,     0,   465,   464,
   282,     0,     0,     0,     0,   393,   396,   394,   407,   392,
   376,   390,   377,   378,   379,   380,   383,     0,   385,     0,
   386,     0,    15,    16,    17,    18,    19,    36,    37,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   474,
     0,     0,   475,     0,     0,     0,     0,   348,     0,     0,
   472,   473,     0,     0,     0,    30,     0,     0,    23,    31,
   261,     0,    24,    33,     0,     0,    66,    73,     0,    25,
    50,    53,     0,   430,     0,     0,     0,     0,     0,     0,
    94,     0,     0,     0,     0,     0,   443,   442,   444,     0,
   452,   451,   456,   455,   440,     0,     0,   449,     0,   446,
     0,     0,     0,     0,     0,   365,   364,     0,     0,     0,
     0,   332,     0,   326,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   301,   329,   328,   295,
   327,   298,     0,     0,     0,     0,     0,     0,     0,   237,
   468,     0,   259,     0,     0,     0,     0,   467,   284,     0,
     0,   256,   250,     0,     0,     0,     0,     0,     0,     0,
     0,   224,    10,     0,     0,     0,    22,     0,     0,     0,
     0,   276,   223,     0,   262,     0,     0,     0,     0,     0,
     0,     0,   382,   384,   388,     0,   338,     0,     0,   336,
     0,     0,     0,     0,   229,   346,     0,   228,     0,     0,
   347,     0,     0,   343,    48,   344,    49,   265,     0,     0,
    71,     0,   309,     0,     0,   280,   312,     0,   316,     0,
     0,     0,   432,     0,   438,     0,   439,     0,   436,   453,
   457,   104,     0,     0,   367,   333,     0,     3,   369,     0,
   330,     0,     0,     0,     0,     0,     0,   300,   302,   358,
     0,     0,     0,     0,     0,     0,     0,     0,   235,     0,
     0,     0,     0,     0,   243,   255,   225,     0,     0,   226,
     0,     0,     0,    21,     0,     0,     0,   398,   399,   400,
   395,   401,     0,   337,     0,     0,     0,     0,     0,    27,
     0,    28,    55,     0,    29,     0,    57,     0,     0,     0,
     0,     0,     0,   429,   307,   462,     0,   448,     0,   314,
     0,   458,   450,     0,     0,   447,     0,     0,     0,     0,
   366,     0,     0,   368,     0,   292,     0,   293,     0,     0,
     0,     0,   304,   232,     0,   234,   249,   257,     0,   240,
     0,     0,     0,     0,   288,   221,   397,   339,   341,   354,
   352,     0,    26,   264,     0,     0,     0,   431,   437,     0,
   434,   435,     0,     0,     0,     0,     0,     0,   357,   359,
   355,   360,   296,   299,     0,     0,     0,     0,   239,     0,
   245,     0,   227,    51,   310,     0,     0,     0,     0,     0,
     0,     0,   361,     0,     0,   236,   241,     0,     0,     0,
   244,   317,   433,     0,   331,   305,     0,     0,   246,     0,
   242,     0,   247,     0,   248,
    }, yyDgoto = {
//yyDgoto 138
     1,   187,    60,    61,    62,    63,    64,   292,   289,   459,
    65,    66,   467,    67,    68,    69,   108,   205,   206,    71,
    72,    73,    74,    75,    76,    77,    78,   298,   296,   209,
   258,   710,   840,   711,   703,   707,   664,   665,   236,   621,
   416,   245,    80,   408,   612,   409,   365,    81,    82,   694,
   781,   565,   566,   567,   201,   750,   211,   227,   658,   212,
    85,   355,   336,   541,   529,    86,    87,   238,   395,    88,
    89,   266,   271,   595,    90,   272,   241,   578,   273,   378,
   213,   214,   276,   277,   568,   202,   290,    93,   113,   546,
   517,   114,   204,   512,   569,   570,   571,   374,   572,   573,
     2,   219,   220,   425,   255,   681,   191,   574,   254,   427,
   443,   246,   625,   731,   633,   383,   222,   599,   722,   223,
   723,   607,   844,   545,   384,   542,   772,   370,   375,   554,
   776,   505,   507,   506,   651,   650,   544,   371,
    }, yySindex = {
//yySindex 885
     0,     0, 13609, 13834,  3392, 13400, 17408, 17301, 13609, 15402,
 15402, 10776,     0,     0, 16746, 14058, 14058,     0,     0, 14058,
  -242,  -220,     0,     0,     0,     0, 15402, 17194,   153,     0,
  -159,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0, 16410, 16410,  -131,  -102, 13722, 15402, 15514,
 16410, 16970, 16410, 16522, 17514,     0,     0,     0,   191,   195,
     0,  -127,     0,     0,     0,     0,     0,     0,     0,     0,
     0,    84,   666,   -98,  1925,     0,   -17,     0,  -211,   -46,
     0,  -139,     0,   -65,   216,     0,   237,     0,   231,     0,
 16858,     0,   -18,     0,  -185,   666,     0,     0,     0,  -242,
  -220,   153,     0,     0,   232, 15402,  -136, 13609,    20,     0,
   166,     0,     0,  -185,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   -81,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
 17514,   284,     0,     0,     0,    62,    71,    49,   -98,    26,
   196,    16,   349,     0,    75,    26,     0,     0,    84,  -193,
   366,     0, 15402, 15402,   122,   250,     0,   162,     0,     0,
     0, 16410, 16410, 16410,  1925,     0,     0,     0,   104,   413,
   431,     0,     0,     0, 13084,     0, 14170, 14058,     0,     0,
     0,   254,     0,     0,   135,   177, 13609,     0,   280,   204,
   211,   213,   210, 13722,   494,     0,   515,    52, 16410,   153,
     0,    44,    55,   469,   126,    55,     0,   450,   281,   345,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  -183,
     0,     0,   289,   404,   171,   224,   443,   238,  -230,     0,
     0, 13196, 15402, 15402, 15402, 15402, 13834, 15402, 15402, 16410,
 16410, 16410, 16410, 16410, 16410, 16410, 16410, 16410, 16410, 16410,
 16410, 16410, 16410, 16410, 16410, 16410, 16410, 16410, 16410, 16410,
 16410, 16410, 16410, 16410, 16410,     0,     0,     0,     0,     0,
  2493,  2984, 15514,  3457,  3457, 16522, 15626,     0, 15626, 13722,
 16970,   569, 16522,     0,   270,     0,   135,     0,     0,   -98,
     0,     0,     0,    84,     0,     0,     0,  3457,  3789, 15514,
 13609, 15402,     0,     0,     0,  1484,     0, 15738,   351,     0,
   210,     0,     0, 13609,   367,  3851,  3915, 15514, 16410, 16410,
 16410, 13609,   381, 13609, 15850,   354,     0,    97,    97,     0,
  4262,  4324, 15514,     0,   611,     0, 16410, 14282,     0,     0,
 14394,     0,     0,   596, 13946,     0,     0,   -17,   153,   136,
   618,   616,     0,     0,     0, 17301,     0, 16410, 13609,   541,
  3851,  3915, 16410, 16410, 16410,   626,     0,     0,   627,  1870,
     0, 15962,     0,     0, 16410,     0,     0, 16410,     0,     0,
     0,     0,  4388,  4736, 15514,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,    15,     0,   641,
     0,   666,     0,     0,     0,     0,     0,     0,     0,   204,
  4904,  4904,  4904,  4904,  1543,  1543,  4432,  3959,  4904,  4904,
  2046,  2046,  1129,  1129,   204,  1058,   204,   204,   342,   342,
  1543,  1543,   960,   960,  2427,  -169,  -169,  -169,   339,     0,
   341,  -220,     0,   346,     0,   352,  -220,     0,     0,   583,
     0,     0,  -220,  -220,  1925,     0, 16410,  2539,     0,     0,
     0,   637,     0,     0,     0,   647,     0,     0,  1925,     0,
     0,     0,    84,     0, 15402, 13609,  -220,     0,     0,  -220,
     0,   599,   432,    52, 17620,   639,     0,     0,     0,   821,
     0,     0,     0,     0,     0, 13609,    84,     0,   657,     0,
   658,   665,   411,   415, 17301,     0,     0,     0,   385, 13609,
   464,     0,   337,     0,   389,   392,   395,   352,   644,  2539,
   351,   475,   482, 16410,   712,    26,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   673, 15402,   410,     0,
     0, 16410,     0,   104,   723, 16410,   104,     0,     0, 16410,
  1925,     0,     0,    -4,   736,   741,   743,  3457,  3457,   749,
 14506,     0,     0, 15402,  1925,   674,     0,   204,   204,  1925,
     0,     0,     0, 16410,     0,     0,     0,     0,     0,   698,
 13609,   913,     0,     0,     0, 16410,     0,  2919, 13609,     0,
 13609, 13609, 16522, 16522,     0,     0,   270,     0, 16522, 16410,
     0,   270,   461,     0,     0,     0,     0,     0, 16410, 16074,
     0,  -169,     0,    84,   539,     0,     0,   769,     0, 16410,
   153,   547,     0,   703,     0,   276,     0,   821,     0,     0,
     0,     0, 17082,    26,     0,     0, 13609,     0,     0, 15402,
     0,   560, 16410, 16410, 16410,   478,   567,     0,     0,     0,
 16186, 13609, 13609, 13609,     0,    97,   611, 14618,     0,   611,
   611,   794, 14730, 14842,     0,     0,     0,  -220,  -220,     0,
   -17,   136,   222,     0,  1870,     0,   711,     0,     0,     0,
     0,     0,  1925,     0,   479,   579,   589,   719,  1925,     0,
  1925,     0,     0,  1925,     0,  1925,     0, 16522,  1925, 16410,
     0, 13609, 13609,     0,     0,     0,  1484,     0,   815,     0,
   639,     0,     0,   658,   803,     0,   658,   555,   347,     0,
     0,     0, 13609,     0,    26,     0, 16410,     0, 16410,   -10,
   597,   604,     0,     0, 16410,     0,     0,     0, 16410,     0,
   825,   826, 16410,   832,     0,     0,     0,     0,     0,     0,
     0,  1925,     0,     0,   755,   610, 13609,     0,     0,   703,
     0,     0,     0,  4798,  4862, 15514,    62, 13609,     0,     0,
     0,     0,     0,     0, 13609,  2344,   611, 14954,     0, 15066,
     0,   611,     0,     0,     0,   617,   658,     0,     0,     0,
     0,   800,     0,   337,   630,     0,     0, 16410,   851, 16410,
     0,     0,     0,     0,     0,     0,   611, 15178,     0,   611,
     0, 16410,     0,   611,     0,
    }, yyRindex = {
//yyRindex 885
     0,     0,   123,     0,     0,     0,     0,     0,   613,     0,
     0,    19,     0,     0,     0,  7317,  7452,     0,     0,  7565,
  4183,  3597,     0,     0,     0,     0,     0,     0, 16298,     0,
     0,     0,     0,  1690,  2761,     0,     0,  1813,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   155,     0,   813,
   774,   189,     0,     0,   713,     0,     0,     0,   737,  -227,
     0,  6607,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   997,  1079,  1647,  6337,  6742, 10997,     0,  6855,     0,
     0, 11455,     0,  8624,     0,     0,     0,     0,     0,     0,
   206,  8523,     0,     0, 15290,  5562,     0,     0,     0,  6956,
  5598,   570,  9850,  9963,     0,     0,     0,   155,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   683,
   768,   886,   969,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  1198,  1499,  1577,     0,  2014,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  6047,     0,     0,     0,   319,     0,     0, 13567,     0,
     0,  6145,     0,  6031,     0,     0,     0,     0,   638,     0,
   182,     0,     0,     0,     0,     0,   612,     0,     0,     0,
   664,     0,     0,     0, 12229,     0,     0,     0, 12877, 13039,
 13039,     0,     0,     0,     0,     0,     0,   873,     0,     0,
     0,     0,     0,     0, 16634,     0,    53,     0,     0,  7666,
  7103,  7204,  8738,   155,     0,    29,     0,   875,     0,   828,
     0,   830,   830,     0,   804,   804,     0,     0,     0,     0,
  1431,     0,  1432,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  1062,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   813,     0,     0,     0,     0,     0,     0,   155,
   219,   221,     0,     0, 13353,     0,     0,     0,     0,    86,
     0, 10313,     0,     0,     0,     0,     0,     0,     0,   813,
   613,     0,     0,     0,     0,   108,     0,   161,   379,     0,
  6393,     0,     0,   735, 10426,     0,     0,   813,     0,     0,
     0,   230,     0,   124,     0,     0,     0,     0,     0,   707,
     0,     0,   813,     0, 13039,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   877,     0,     0,   163,   875,   875,
   169,     0,     0,     0,     0,     0,     0,     0,    53,     0,
     0,     0,     0,     0,     0,     0,     0,    98,     0,   828,
     0,   839,     0,     0,    35,     0,     0,   808,     0,     0,
     0,  1447,     0,     0,   813,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  5645,     0,     0,     0,     0,     0,     0,     0,  7813,
 11581, 11635, 11715, 11851, 11066, 11146, 11905, 12067, 11959, 12013,
 12121, 12175,  9272,  9376,  7914,  9456,  8027,  8162,  8983,  9087,
 11226, 11501,  9561,  9745,     0, 10545, 10545, 10658,  4544,     0,
  4656,  3709,     0,  5018,  3123,  5130, 15290,     0,  3235,     0,
     0,     0,  5491,  5491, 12283,     0,     0, 11189,     0,     0,
     0,     0,     0,     0,  5551,     0,     0,     0, 12337,     0,
     0,     0,     0,     0,     0,   613,  5931, 10082, 10195,     0,
     0,     0,     0,   875,     0,   517,     0,     0,     0,   174,
     0,     0,     0,     0,     0,   613,     0,     0,   553,     0,
   553,   553,   592,     0,     0,     0,     0,    81,    72,   273,
   691,     0,   691,     0,  2175,  2287,  2649,  4071,     0, 12931,
   691,     0,     0,     0,   512,     0,     0,     0,     0,     0,
     0,     0,  1022,  1254,  1347,   434,     0,     0,     0,     0,
     0,     0,     0, 12985, 13039,     0,     0,     0,     0,     0,
   225,     0,     0,     0,   906,     0,     0,     0,     0,     0,
     0,     0,     0,     0, 12391,     0,     0,  8275,  8376, 12445,
   207,     0,     0,     0,     0,   578,   580,  1091,  1344,     0,
    53,     0,     0,     0,     0,     0,     0,     0,   124,     0,
   124,    53,     0,     0,     0,     0, 13465,     0,     0,     0,
     0, 13511,  8872,     0,     0,     0,     0,     0,     0,     0,
     0, 10658,     0,     0,     0,     0,     0,     0,     0,     0,
   875,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   124,     0,     0,     0,
     0,     0,     0,     0,     0,  6494,     0,     0,     0,     0,
     0,   472,   124,   124,   881,     0, 13039,     0,     0, 13039,
   906,     0,     0,     0,     0,     0,     0,   125,   125,     0,
     0,   875,     0,     0,   828,  1480,     0,     0,     0,     0,
     0,     0, 12499,     0,     0,     0,     0,     0, 12553,     0,
 12607,     0,     0, 12661,     0, 12715,     0,     0, 12769,     0,
  5841,    53,   613,     0,     0,     0,   108,     0,     0,     0,
     0,     0,     0,   553,   553,     0,   553,     0,     0,   103,
     0,   167,   613,     0,     0,     0,     0,     0,     0,   691,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   906,   906,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 12823,     0,     0,     0,     0,   613,     0,     0,     0,
     0,     0,   820,     0,     0,   813,   319,   735,     0,     0,
     0,     0,     0,     0,   124, 13039,   906,     0,     0,     0,
     0,   906,     0,     0,     0,     0,   553,   187,   760,   806,
   142,     0,     0,   691,     0,     0,     0,     0,   906,     0,
     0,     0,     0,   957,     0,     0,   906,     0,     0,   906,
     0,     0,     0,   906,     0,
    }, yyGindex = {
//yyGindex 138
     0,     0,     0,     0,   895,     0,     0,     0,   593,  -246,
     0,     0,     0,     0,     0,     0,   952,   127,  -337,     0,
    28,  1032,   -15,    47,   105,    48,     0,     0,     0,    63,
   100,  -368,     0,   115,     0,     0,    -7,  -467,   595,     0,
     0,    23,   962,   334,    -3,     0,     0,  -241,     0,  -511,
  -360,   190,   421,  -644,     0,     0,   718,   326,  -439,   793,
  1020,   907,     0,  -574,  -153,   898,   -30,  1053,  -372,   -11,
     1,  -174,    -8,     0,     0,     8,  -369,     0,  -316,   157,
   899,   974,   748,     0,   301,    -5,     0,    -2,   582,   120,
     0,   -42,     3,    12,   303,  -616,     0,     0,     0,     0,
     0,   -54,   931,     0,     0,     0,     0,     0,   -58,     0,
   278,     0,     0,     0,     0,  -213,     0,  -366,     0,     0,
     0,     0,     0,     0,    61,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,
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
    null,null,null,"')'",null,null,"','",null,null,null,null,null,null,
    null,null,null,null,null,null,null,"':'","';'",null,"'='",null,"'?'",
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,
    "'['",null,"']'",null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,"'}'",null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    "kCLASS","kMODULE","kDEF","kUNDEF","kBEGIN","kRESCUE","kENSURE",
    "kEND","kIF","kUNLESS","kTHEN","kELSIF","kELSE","kCASE","kWHEN",
    "kWHILE","kUNTIL","kFOR","kBREAK","kNEXT","kREDO","kRETRY","kIN",
    "kDO","kDO_COND","kDO_BLOCK","kRETURN","kYIELD","kSUPER","kSELF",
    "kNIL","kTRUE","kFALSE","kAND","kOR","kNOT","kIF_MOD","kUNLESS_MOD",
    "kWHILE_MOD","kUNTIL_MOD","kRESCUE_MOD","kALIAS","kDEFINED","klBEGIN",
    "klEND","k__LINE__","k__FILE__","tIDENTIFIER","tFID","tGVAR","tIVAR",
    "tCONSTANT","tCVAR","tSTRING_CONTENT","tINTEGER","tFLOAT","tNTH_REF",
    "tBACK_REF","tREGEXP_END","tUPLUS","tUMINUS","tUMINUS_NUM","tPOW",
    "tCMP","tEQ","tEQQ","tNEQ","tGEQ","tLEQ","tANDOP","tOROP","tMATCH",
    "tNMATCH","tDOT","tDOT2","tDOT3","tAREF","tASET","tLSHFT","tRSHFT",
    "tCOLON2","tCOLON3","tOP_ASGN","tASSOC","tLPAREN","tLPAREN2",
    "tLPAREN_ARG","tLBRACK","tLBRACE","tLBRACE_ARG","tSTAR","tSTAR2",
    "tAMPER","tAMPER2","tTILDE","tPERCENT","tDIVIDE","tPLUS","tMINUS",
    "tLT","tGT","tPIPE","tBANG","tCARET","tLCURLY","tBACK_REF2","tSYMBEG",
    "tSTRING_BEG","tXSTRING_BEG","tREGEXP_BEG","tWORDS_BEG","tQWORDS_BEG",
    "tSTRING_DBEG","tSTRING_DVAR","tSTRING_END","tLOWEST","tLAST_TOKEN",
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
    yyerror(message, null, null);
  }

  /** (syntax) error message.
      Can be overwritten to control message format.
      @param message text to be displayed.
      @param expected list of acceptable tokens, if available.
    */
  public void yyerror (String message, String[] expected, String found) {
    StringBuffer text = new StringBuffer(message);

    if (expected != null && expected.length > 0) {
      text.append(", expecting");
      for (int n = 0; n < expected.length; ++ n) {
        text.append("\t").append(expected[n]);
      }
      text.append(" but found " + found + " instead\n");
    }

    throw new SyntaxException(getPosition(null), text.toString());
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
					// line 322 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_BEG);
                  support.initTopLocalVariables();

              }
  break;
case 2:
					// line 326 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[0+yyTop]) != null) {
                      /* last expression should not be void */
                      if (((Node)yyVals[0+yyTop]) instanceof BlockNode) {
                          support.checkUselessStatement(((BlockNode)yyVals[0+yyTop]).getLast());
                      } else {
                          support.checkUselessStatement(((Node)yyVals[0+yyTop]));
                      }
                  }
                  support.getResult().setAST(support.appendToBlock(support.getResult().getAST(), ((Node)yyVals[0+yyTop])));
                  support.updateTopLocalVariables();
              }
  break;
case 3:
					// line 342 "DefaultRubyParser.y"
  {
                 Node node = ((Node)yyVals[-3+yyTop]);

		 if (((RescueBodyNode)yyVals[-2+yyTop]) != null) {
		   node = new RescueNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop]), true), ((Node)yyVals[-3+yyTop]), ((RescueBodyNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
		 } else if (((Node)yyVals[-1+yyTop]) != null) {
		       warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), "else without rescue is useless");
                       node = support.appendToBlock(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
		 }
		 if (((Node)yyVals[0+yyTop]) != null) {
		    node = new EnsureNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), node, ((Node)yyVals[0+yyTop]));
		 }

		 yyVal = node;
             }
  break;
case 4:
					// line 358 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-1+yyTop]) instanceof BlockNode) {
                     support.checkUselessStatements(((BlockNode)yyVals[-1+yyTop]));
		  }
                  yyVal = ((Node)yyVals[-1+yyTop]);
              }
  break;
case 6:
					// line 366 "DefaultRubyParser.y"
  {
                    yyVal = support.newline_node(((Node)yyVals[0+yyTop]), getPosition(((ISourcePositionHolder)yyVals[0+yyTop]), true));
                }
  break;
case 7:
					// line 369 "DefaultRubyParser.y"
  {
		    yyVal = support.appendToBlock(((Node)yyVals[-2+yyTop]), support.newline_node(((Node)yyVals[0+yyTop]), getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]), true)));
                }
  break;
case 8:
					// line 372 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 9:
					// line 376 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
                }
  break;
case 10:
					// line 378 "DefaultRubyParser.y"
  {
                    yyVal = new AliasNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 11:
					// line 381 "DefaultRubyParser.y"
  {
                    yyVal = new VAliasNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 12:
					// line 384 "DefaultRubyParser.y"
  {
                    yyVal = new VAliasNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), "$" + ((BackRefNode)yyVals[0+yyTop]).getType()); /* XXX*/
                }
  break;
case 13:
					// line 387 "DefaultRubyParser.y"
  {
                    yyerror("can't make alias for the number variables");
                    yyVal = null; /*XXX 0*/
                }
  break;
case 14:
					// line 391 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 15:
					// line 394 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), null);
                }
  break;
case 16:
					// line 397 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), null, ((Node)yyVals[-2+yyTop]));
                }
  break;
case 17:
					// line 400 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = new WhileNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                    } else {
                        yyVal = new WhileNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                    }
                }
  break;
case 18:
					// line 407 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = new UntilNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode());
                    } else {
                        yyVal = new UntilNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]));
                    }
                }
  break;
case 19:
					// line 415 "DefaultRubyParser.y"
  {
		  yyVal = new RescueNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), new RescueBodyNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), null,((Node)yyVals[0+yyTop]), null), null);
                }
  break;
case 20:
					// line 419 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("BEGIN in method");
                    }
                    support.getLocalNames().push(new LocalNamesElement());
                }
  break;
case 21:
					// line 424 "DefaultRubyParser.y"
  {
                    support.getResult().addBeginNode(new ScopeNode(getPosition(((Token)yyVals[-4+yyTop]), true), ((LocalNamesElement) support.getLocalNames().peek()).getNames(), ((Node)yyVals[-1+yyTop])));
                    support.getLocalNames().pop();
                    yyVal = null; /*XXX 0;*/
                }
  break;
case 22:
					// line 429 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("END in method; use at_exit");
                    }
                    support.getResult().addEndNode(new IterNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), null, new PostExeNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop]))), ((Node)yyVals[-1+yyTop])));
                    yyVal = null;
                }
  break;
case 23:
					// line 436 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 24:
					// line 440 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
		    if (((MultipleAsgnNode)yyVals[-2+yyTop]).getHeadNode() != null) {
		        ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(new ToAryNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
		    } else {
		        ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(((Node)yyVals[0+yyTop])));
		    }
		    yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
                }
  break;
case 25:
					// line 449 "DefaultRubyParser.y"
  {
 		    support.checkExpression(((Node)yyVals[0+yyTop]));
		    if (yyVals[-2+yyTop] != null) {
		        String name = ((INameNode)yyVals[-2+yyTop]).getName();
			String asgnOp = (String) ((Token)yyVals[-1+yyTop]).getValue();
		        if (asgnOp.equals("||")) {
	                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
	                    yyVal = new OpAsgnOrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.gettable(name, ((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition()), ((Node)yyVals[-2+yyTop]));
			    /* XXX
			    if (is_asgn_or_id(vid)) {
				$$->nd_aid = vid;
			    }
			    */
			} else if (asgnOp.equals("&&")) {
	                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                            yyVal = new OpAsgnAndNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.gettable(name, ((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition()), ((Node)yyVals[-2+yyTop]));
			} else {
			    yyVal = yyVals[-2+yyTop];
                            if (yyVal != null) {
                                ((AssignableNode)yyVal).setValueNode(support.getOperatorCallNode(support.gettable(name, ((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition()), asgnOp, ((Node)yyVals[0+yyTop])));
                            }
			}
		    } else {
 		        yyVal = null;
		    }
		}
  break;
case 26:
					// line 475 "DefaultRubyParser.y"
  {
                    /* Much smaller than ruby block */
                    yyVal = new OpElementAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));

                }
  break;
case 27:
					// line 480 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 28:
					// line 483 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 29:
					// line 486 "DefaultRubyParser.y"
  {
  yyVal = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 30:
					// line 489 "DefaultRubyParser.y"
  {
                    support.backrefAssignError(((Node)yyVals[-2+yyTop]));
                    yyVal = null;
                }
  break;
case 31:
					// line 493 "DefaultRubyParser.y"
  {
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), new SValueNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
                }
  break;
case 32:
					// line 496 "DefaultRubyParser.y"
  {
                    if (((MultipleAsgnNode)yyVals[-2+yyTop]).getHeadNode() != null) {
		        ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(new ToAryNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
		    } else {
		        ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(((Node)yyVals[0+yyTop])));
		    }
		    yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
		}
  break;
case 33:
					// line 504 "DefaultRubyParser.y"
  {
                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
		    yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
		}
  break;
case 36:
					// line 511 "DefaultRubyParser.y"
  {
                    yyVal = support.newAndNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 37:
					// line 514 "DefaultRubyParser.y"
  {
                    yyVal = support.newOrNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 38:
					// line 517 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
                }
  break;
case 39:
					// line 520 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
                }
  break;
case 41:
					// line 525 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
		    yyVal = ((Node)yyVals[0+yyTop]); /*Do we really need this set? $1 is $$?*/
		}
  break;
case 44:
					// line 532 "DefaultRubyParser.y"
  {
                    yyVal = new ReturnNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))));
                }
  break;
case 45:
					// line 535 "DefaultRubyParser.y"
  {
                    yyVal = new BreakNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))));
                }
  break;
case 46:
					// line 538 "DefaultRubyParser.y"
  {
                    yyVal = new NextNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))));
                }
  break;
case 48:
					// line 543 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 49:
					// line 546 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 50:
					// line 550 "DefaultRubyParser.y"
  {
                      support.getBlockNames().push(new BlockNamesElement());
		  }
  break;
case 51:
					// line 552 "DefaultRubyParser.y"
  {
                      yyVal = new IterNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), null);
                      support.getBlockNames().pop();
		  }
  break;
case 52:
					// line 557 "DefaultRubyParser.y"
  {
                    yyVal = support.new_fcall((String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop])); /* .setPosFrom($2);*/
                }
  break;
case 53:
					// line 560 "DefaultRubyParser.y"
  {
                    yyVal = support.new_fcall((String) ((Token)yyVals[-2+yyTop]).getValue(), ((Node)yyVals[-1+yyTop]), ((Token)yyVals[-2+yyTop])); 
	            if (((IterNode)yyVals[0+yyTop]) != null) {
                        if (yyVal instanceof BlockPassNode) {
                            throw new SyntaxException(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), "Both block arg and actual block given.");
                        }
                        ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVal));
                        yyVal = ((Node)yyVals[-1+yyTop]);
		   }
                }
  break;
case 54:
					// line 570 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 55:
					// line 573 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), ((Node)yyVals[-1+yyTop])); 
		    if (((IterNode)yyVals[0+yyTop]) != null) {
		        if (yyVal instanceof BlockPassNode) {
                            throw new SyntaxException(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), "Both block arg and actual block given.");
                        }
                        ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVal));
			yyVal = ((IterNode)yyVals[0+yyTop]);
		    }
		 }
  break;
case 56:
					// line 583 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 57:
					// line 586 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), ((Node)yyVals[-1+yyTop])); 
		    if (((IterNode)yyVals[0+yyTop]) != null) {
		        if (yyVal instanceof BlockPassNode) {
                            throw new SyntaxException(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), "Both block arg and actual block given.");
                        }
                        ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVal));
			yyVal = ((IterNode)yyVals[0+yyTop]);
		    }
	        }
  break;
case 58:
					// line 596 "DefaultRubyParser.y"
  {
		    yyVal = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop])); /* .setPosFrom($2);*/
		}
  break;
case 59:
					// line 599 "DefaultRubyParser.y"
  {
                    yyVal = support.new_yield(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
		}
  break;
case 61:
					// line 604 "DefaultRubyParser.y"
  {
                    yyVal = ((MultipleAsgnNode)yyVals[-1+yyTop]);
		}
  break;
case 63:
					// line 609 "DefaultRubyParser.y"
  {
	            yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(((MultipleAsgnNode)yyVals[-1+yyTop])), null);
                }
  break;
case 64:
					// line 613 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), ((ListNode)yyVals[0+yyTop]), null);
                }
  break;
case 65:
					// line 616 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((ListNode)yyVals[-1+yyTop]).add(((Node)yyVals[0+yyTop])), null);
                }
  break;
case 66:
					// line 619 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 67:
					// line 622 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((ListNode)yyVals[-1+yyTop]), new StarNode(getPosition(null)));
                }
  break;
case 68:
					// line 625 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), null, ((Node)yyVals[0+yyTop]));
                }
  break;
case 69:
					// line 628 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), null, new StarNode(getPosition(null)));
                }
  break;
case 71:
					// line 633 "DefaultRubyParser.y"
  {
                    yyVal = ((MultipleAsgnNode)yyVals[-1+yyTop]);
                }
  break;
case 72:
					// line 637 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))).add(((Node)yyVals[-1+yyTop]));
                }
  break;
case 73:
					// line 640 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
                }
  break;
case 74:
					// line 644 "DefaultRubyParser.y"
  {
                    yyVal = support.assignable(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), yyVals[0+yyTop], null);
                }
  break;
case 75:
					// line 647 "DefaultRubyParser.y"
  {
                    yyVal = support.getElementAssignmentNode(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 76:
					// line 650 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 77:
					// line 653 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 78:
					// line 656 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 79:
					// line 659 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }
			
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
		}
  break;
case 80:
					// line 666 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }

		    /* ERROR:  VEry likely a big error. */
                    yyVal = new Colon3Node(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
		    /* ruby $$ = NEW_CDECL(0, 0, NEW_COLON3($2)); */
		    }
  break;
case 81:
					// line 676 "DefaultRubyParser.y"
  {
	            support.backrefAssignError(((Node)yyVals[0+yyTop]));
                    yyVal = null;
                }
  break;
case 82:
					// line 681 "DefaultRubyParser.y"
  {
                    yyVal = support.assignable(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), yyVals[0+yyTop], null);
                }
  break;
case 83:
					// line 684 "DefaultRubyParser.y"
  {
                    yyVal = support.getElementAssignmentNode(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 84:
					// line 687 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 85:
					// line 690 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
 	        }
  break;
case 86:
					// line 693 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 87:
					// line 696 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }
			
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
	        }
  break;
case 88:
					// line 703 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }

		    /* ERROR:  VEry likely a big error. */
                    yyVal = new Colon3Node(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
		    /* ruby $$ = NEW_CDECL(0, 0, NEW_COLON3($2)); */
	        }
  break;
case 89:
					// line 712 "DefaultRubyParser.y"
  {
                    support.backrefAssignError(((Node)yyVals[0+yyTop]));
                    yyVal = null;
		}
  break;
case 90:
					// line 717 "DefaultRubyParser.y"
  {
                    yyerror("class/module name must be CONSTANT");
                }
  break;
case 92:
					// line 722 "DefaultRubyParser.y"
  {
                    yyVal = new Colon3Node(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
		}
  break;
case 93:
					// line 725 "DefaultRubyParser.y"
  {
                    /* $1 was $$ in ruby?*/
                    yyVal = new Colon2Node(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), null, (String) ((Token)yyVals[0+yyTop]).getValue());
 	        }
  break;
case 94:
					// line 729 "DefaultRubyParser.y"
  {
                    yyVal = new Colon2Node(support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
		}
  break;
case 98:
					// line 736 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 99:
					// line 740 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    yyVal = yyVals[0+yyTop];
                }
  break;
case 102:
					// line 748 "DefaultRubyParser.y"
  {
                    yyVal = new UndefNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 103:
					// line 751 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
	        }
  break;
case 104:
					// line 753 "DefaultRubyParser.y"
  {
                    yyVal = support.appendToBlock(((Node)yyVals[-3+yyTop]), new UndefNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue()));
                }
  break;
case 105:
					// line 757 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("|"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 106:
					// line 758 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("^"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 107:
					// line 759 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("&"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 108:
					// line 760 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("<=>"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 109:
					// line 761 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("=="); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 110:
					// line 762 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("==="); yyVal = ((Token)yyVals[0+yyTop]);}
  break;
case 111:
					// line 763 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("=~"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 112:
					// line 764 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue(">"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 113:
					// line 765 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue(">="); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 114:
					// line 766 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("<"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 115:
					// line 767 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("<="); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 116:
					// line 768 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("<<"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 117:
					// line 769 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue(">>"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 118:
					// line 770 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("+"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 119:
					// line 771 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("-"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 120:
					// line 772 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("*"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 121:
					// line 773 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("*"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 122:
					// line 774 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("/"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 123:
					// line 775 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("%"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 124:
					// line 776 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("**"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 125:
					// line 777 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("~"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 126:
					// line 778 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("+@"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 127:
					// line 779 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("-@"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 128:
					// line 780 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("[]"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 129:
					// line 781 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("[]="); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 130:
					// line 782 "DefaultRubyParser.y"
  {  yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 172:
					// line 793 "DefaultRubyParser.y"
  {
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 173:
					// line 796 "DefaultRubyParser.y"
  {
                    yyVal = support.node_assign(((Node)yyVals[-4+yyTop]), new RescueNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), new RescueBodyNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), null,((Node)yyVals[0+yyTop]), null), null));
		}
  break;
case 174:
					// line 799 "DefaultRubyParser.y"
  {
		    support.checkExpression(((Node)yyVals[0+yyTop]));
		    if (yyVals[-2+yyTop] != null) {
		        String name = ((INameNode)yyVals[-2+yyTop]).getName();
			String asgnOp = (String) ((Token)yyVals[-1+yyTop]).getValue();

		        if (asgnOp.equals("||")) {
	                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
	                    yyVal = new OpAsgnOrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.gettable(name, ((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition()), ((Node)yyVals[-2+yyTop]));
			    /* FIXME
			    if (is_asgn_or_id(vid)) {
				$$->nd_aid = vid;
			    }
			    */
			} else if (asgnOp.equals("&&")) {
	                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                            yyVal = new OpAsgnAndNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.gettable(name, ((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition()), ((Node)yyVals[-2+yyTop]));
			} else {
			    yyVal = yyVals[-2+yyTop];
                            if (yyVal != null) {
			      ((AssignableNode)yyVal).setValueNode(support.getOperatorCallNode(support.gettable(name, ((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition()), asgnOp, ((Node)yyVals[0+yyTop])));
                            }
			}
		    } else {
 		        yyVal = null; /* XXX 0; */
		    }
                }
  break;
case 175:
					// line 826 "DefaultRubyParser.y"
  {
                    yyVal = new OpElementAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 176:
					// line 829 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 177:
					// line 832 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 178:
					// line 835 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 179:
					// line 838 "DefaultRubyParser.y"
  {
		    yyerror("constant re-assignment");
		    yyVal = null;
	        }
  break;
case 180:
					// line 842 "DefaultRubyParser.y"
  {
		    yyerror("constant re-assignment");
		    yyVal = null;
	        }
  break;
case 181:
					// line 846 "DefaultRubyParser.y"
  {
                    support.backrefAssignError(((Node)yyVals[-2+yyTop]));
                    yyVal = null;
                }
  break;
case 182:
					// line 850 "DefaultRubyParser.y"
  {
		    support.checkExpression(((Node)yyVals[-2+yyTop]));
		    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = new DotNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), false);
                }
  break;
case 183:
					// line 855 "DefaultRubyParser.y"
  {
		    support.checkExpression(((Node)yyVals[-2+yyTop]));
		    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = new DotNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), true);
                }
  break;
case 184:
					// line 860 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "+", ((Node)yyVals[0+yyTop]));
                }
  break;
case 185:
					// line 863 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "-", ((Node)yyVals[0+yyTop]));
                }
  break;
case 186:
					// line 866 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "*", ((Node)yyVals[0+yyTop]));
                }
  break;
case 187:
					// line 869 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "/", ((Node)yyVals[0+yyTop]));
                }
  break;
case 188:
					// line 872 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "%", ((Node)yyVals[0+yyTop]));
                }
  break;
case 189:
					// line 875 "DefaultRubyParser.y"
  {
		      yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]));
                    /* Covert '- number ** number' to '- (number ** number)' 
                    boolean needNegate = false;
                    if (($1 instanceof FixnumNode && $<FixnumNode>1.getValue() < 0) ||
                        ($1 instanceof BignumNode && $<BignumNode>1.getValue().compareTo(BigInteger.ZERO) < 0) ||
                        ($1 instanceof FloatNode && $<FloatNode>1.getValue() < 0.0)) {

                        $<>1 = support.getOperatorCallNode($1, "-@");
                        needNegate = true;
                    }

                    $$ = support.getOperatorCallNode($1, "**", $3);

                    if (needNegate) {
                        $$ = support.getOperatorCallNode($<Node>$, "-@");
                    }
		    */
                }
  break;
case 190:
					// line 894 "DefaultRubyParser.y"
  {
                    Object number = ((Token)yyVals[-2+yyTop]).getValue();

                    yyVal = support.getOperatorCallNode(support.getOperatorCallNode((number instanceof Long ? (Node) new FixnumNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Long) number).longValue()) : (Node)new BignumNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((BigInteger) number))), "**", ((Node)yyVals[0+yyTop])), "-@");
                }
  break;
case 191:
					// line 899 "DefaultRubyParser.y"
  {
  /* ENEBO: Seems like this should be $2*/
                    yyVal = support.getOperatorCallNode(support.getOperatorCallNode(new FloatNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Double) ((Token)yyVals[-3+yyTop]).getValue()).doubleValue()), "**", ((Node)yyVals[0+yyTop])), "-@");
                }
  break;
case 192:
					// line 903 "DefaultRubyParser.y"
  {
 	            if (((Node)yyVals[0+yyTop]) != null && ((Node)yyVals[0+yyTop]) instanceof ILiteralNode) {
		        yyVal = ((Node)yyVals[0+yyTop]);
		    } else {
                        yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "+@");
		    }
                }
  break;
case 193:
					// line 910 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "-@");
		}
  break;
case 194:
					// line 913 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "|", ((Node)yyVals[0+yyTop]));
                }
  break;
case 195:
					// line 916 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "^", ((Node)yyVals[0+yyTop]));
                }
  break;
case 196:
					// line 919 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "&", ((Node)yyVals[0+yyTop]));
                }
  break;
case 197:
					// line 922 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=>", ((Node)yyVals[0+yyTop]));
                }
  break;
case 198:
					// line 925 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">", ((Node)yyVals[0+yyTop]));
                }
  break;
case 199:
					// line 928 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">=", ((Node)yyVals[0+yyTop]));
                }
  break;
case 200:
					// line 931 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<", ((Node)yyVals[0+yyTop]));
                }
  break;
case 201:
					// line 934 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=", ((Node)yyVals[0+yyTop]));
                }
  break;
case 202:
					// line 937 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop]));
                }
  break;
case 203:
					// line 940 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "===", ((Node)yyVals[0+yyTop]));
                }
  break;
case 204:
					// line 943 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop])));
                }
  break;
case 205:
					// line 946 "DefaultRubyParser.y"
  {
                    yyVal = support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 206:
					// line 949 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
                }
  break;
case 207:
					// line 952 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
                }
  break;
case 208:
					// line 955 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "~");
                }
  break;
case 209:
					// line 958 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<<", ((Node)yyVals[0+yyTop]));
                }
  break;
case 210:
					// line 961 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">>", ((Node)yyVals[0+yyTop]));
                }
  break;
case 211:
					// line 964 "DefaultRubyParser.y"
  {
                    yyVal = support.newAndNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 212:
					// line 967 "DefaultRubyParser.y"
  {
                    yyVal = support.newOrNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 213:
					// line 970 "DefaultRubyParser.y"
  {
	            support.setInDefined(true);
		}
  break;
case 214:
					// line 972 "DefaultRubyParser.y"
  {
                    support.setInDefined(false);
                    yyVal = new DefinedNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Node)yyVals[0+yyTop]));
                }
  break;
case 215:
					// line 976 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 216:
					// line 979 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 217:
					// line 983 "DefaultRubyParser.y"
  {
		    support.checkExpression(((Node)yyVals[0+yyTop]));
	            yyVal = ((Node)yyVals[0+yyTop]);   
		}
  break;
case 219:
					// line 989 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), "parenthesize argument(s) for future version");
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))).add(((Node)yyVals[-1+yyTop]));
                }
  break;
case 220:
					// line 993 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                }
  break;
case 221:
					// line 996 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[-1+yyTop]));
                    yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 222:
					// line 1000 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                }
  break;
case 223:
					// line 1003 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[-1+yyTop]));
		    yyVal = new NewlineNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), new SplatNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])));
                }
  break;
case 224:
					// line 1008 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                }
  break;
case 225:
					// line 1011 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-2+yyTop]);
                }
  break;
case 226:
					// line 1014 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), "parenthesize argument(s) for future version");
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop]))).add(((Node)yyVals[-2+yyTop]));
                }
  break;
case 227:
					// line 1018 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), "parenthesize argument(s) for future version");
                    yyVal = ((ListNode)yyVals[-4+yyTop]).add(((Node)yyVals[-2+yyTop]));
                }
  break;
case 230:
					// line 1026 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), "parenthesize argument(s) for future version");
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 231:
					// line 1030 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_blk_pass(((ListNode)yyVals[-1+yyTop]), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 232:
					// line 1033 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 233:
					// line 1037 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 234:
					// line 1041 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop]))).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 235:
					// line 1045 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-3+yyTop]).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 236:
					// line 1049 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[-1+yyTop]));
		    yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop])), ((ListNode)yyVals[-6+yyTop]).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 237:
					// line 1054 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_blk_pass(new SplatNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 238:
					// line 1057 "DefaultRubyParser.y"
  {
	        }
  break;
case 239:
					// line 1060 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_blk_pass(support.list_concat(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop]))).add(((Node)yyVals[-3+yyTop])), ((ListNode)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 240:
					// line 1063 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_blk_pass(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(((Node)yyVals[-2+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
                  }
  break;
case 241:
					// line 1066 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop]))).add(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 242:
					// line 1070 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop])), support.list_concat(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop]))).add(((Node)yyVals[-6+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 243:
					// line 1074 "DefaultRubyParser.y"
  {
                      yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 244:
					// line 1078 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop]))).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 245:
					// line 1082 "DefaultRubyParser.y"
  {
                      yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop]))).add(((Node)yyVals[-3+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 246:
					// line 1086 "DefaultRubyParser.y"
  {
                      yyVal = support.list_concat(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop]))).add(((Node)yyVals[-5+yyTop])), ((ListNode)yyVals[-3+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 247:
					// line 1090 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop])), new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop]))).add(((Node)yyVals[-6+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 248:
					// line 1094 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-8+yyTop])), support.list_concat(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-8+yyTop]))).add(((Node)yyVals[-8+yyTop])), ((ListNode)yyVals[-6+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 249:
					// line 1098 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_blk_pass(new SplatNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 250:
					// line 1101 "DefaultRubyParser.y"
  {}
  break;
case 251:
					// line 1103 "DefaultRubyParser.y"
  { 
		    yyVal = new Long(lexer.getCmdArgumentState().begin());
		}
  break;
case 252:
					// line 1105 "DefaultRubyParser.y"
  {
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 254:
					// line 1111 "DefaultRubyParser.y"
  {                    
		    lexer.setState(LexState.EXPR_ENDARG);
		  }
  break;
case 255:
					// line 1113 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), "don't put space before argument parentheses");
		    yyVal = null;
		  }
  break;
case 256:
					// line 1117 "DefaultRubyParser.y"
  {
		    lexer.setState(LexState.EXPR_ENDARG);
		  }
  break;
case 257:
					// line 1119 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), "don't put space before argument parentheses");
		    yyVal = ((Node)yyVals[-2+yyTop]);
		  }
  break;
case 258:
					// line 1124 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = new BlockPassNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
                }
  break;
case 259:
					// line 1129 "DefaultRubyParser.y"
  {
                    yyVal = ((BlockPassNode)yyVals[0+yyTop]);
                }
  break;
case 261:
					// line 1134 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 262:
					// line 1137 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 263:
					// line 1141 "DefaultRubyParser.y"
  {
		    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 264:
					// line 1144 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
		}
  break;
case 265:
					// line 1147 "DefaultRubyParser.y"
  {  
                    yyVal = new SplatNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
		}
  break;
case 274:
					// line 1159 "DefaultRubyParser.y"
  {
                    yyVal = new VCallNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
		}
  break;
case 275:
					// line 1163 "DefaultRubyParser.y"
  {
                    yyVal = new BeginNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-1+yyTop]));
		}
  break;
case 276:
					// line 1166 "DefaultRubyParser.y"
  {
		    lexer.setState(LexState.EXPR_ENDARG);
		    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), "(...) interpreted as grouped expression");
                    yyVal = ((Node)yyVals[-2+yyTop]);
		}
  break;
case 277:
					// line 1171 "DefaultRubyParser.y"
  {
	            yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 278:
					// line 1174 "DefaultRubyParser.y"
  {
                    yyVal = new Colon2Node(support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 279:
					// line 1177 "DefaultRubyParser.y"
  {
                    yyVal = new Colon3Node(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 280:
					// line 1180 "DefaultRubyParser.y"
  {
                    yyVal = new CallNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop]), "[]", ((Node)yyVals[-1+yyTop]));
                }
  break;
case 281:
					// line 1183 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-1+yyTop]) == null) {
                        yyVal = new ZArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))); /* zero length array*/
                    } else {
                        yyVal = ((Node)yyVals[-1+yyTop]);
                    }
                }
  break;
case 282:
					// line 1190 "DefaultRubyParser.y"
  {
                    yyVal = new HashNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((ListNode)yyVals[-1+yyTop]));
                }
  break;
case 283:
					// line 1193 "DefaultRubyParser.y"
  {
		    yyVal = new ReturnNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), null);
                }
  break;
case 284:
					// line 1196 "DefaultRubyParser.y"
  {
                    yyVal = support.new_yield(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 285:
					// line 1199 "DefaultRubyParser.y"
  {
                    yyVal = new YieldNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), null, false);
                }
  break;
case 286:
					// line 1202 "DefaultRubyParser.y"
  {
                    yyVal = new YieldNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), null, false);
                }
  break;
case 287:
					// line 1205 "DefaultRubyParser.y"
  {
	            support.setInDefined(true);
		}
  break;
case 288:
					// line 1207 "DefaultRubyParser.y"
  {
                    support.setInDefined(false);
                    yyVal = new DefinedNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 289:
					// line 1211 "DefaultRubyParser.y"
  {
                    ((IterNode)yyVals[0+yyTop]).setIterNode(new FCallNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]), true), (String) ((Token)yyVals[-1+yyTop]).getValue(), null));
                    yyVal = ((IterNode)yyVals[0+yyTop]);
                }
  break;
case 291:
					// line 1216 "DefaultRubyParser.y"
  {
		    if (((Node)yyVals[-1+yyTop]) != null && ((Node)yyVals[-1+yyTop]) instanceof BlockPassNode) {
                        throw new SyntaxException(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), "Both block arg and actual block given.");
		    }
                    ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVals[-1+yyTop]));
                    yyVal = ((IterNode)yyVals[0+yyTop]);
                }
  break;
case 292:
					// line 1223 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
		            NODE *tmp = $$->nd_body;
		            $$->nd_body = $$->nd_else;
		            $$->nd_else = tmp;
			    } */
                }
  break;
case 293:
					// line 1232 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-2+yyTop]));
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
		            NODE *tmp = $$->nd_body;
		            $$->nd_body = $$->nd_else;
		            $$->nd_else = tmp;
			    } */
                }
  break;
case 294:
					// line 1241 "DefaultRubyParser.y"
  { 
	            lexer.getConditionState().begin();
		}
  break;
case 295:
					// line 1243 "DefaultRubyParser.y"
  {
		    lexer.getConditionState().end();
		}
  break;
case 296:
					// line 1245 "DefaultRubyParser.y"
  {
                    yyVal = new WhileNode(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
			    nd_set_type($$, NODE_UNTIL);
			    } */
                }
  break;
case 297:
					// line 1252 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().begin();
                }
  break;
case 298:
					// line 1254 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().end();
                }
  break;
case 299:
					// line 1256 "DefaultRubyParser.y"
  {
                    yyVal = new UntilNode(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
			    nd_set_type($$, NODE_WHILE);
			    } */
                }
  break;
case 300:
					// line 1265 "DefaultRubyParser.y"
  {
		    yyVal = new CaseNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop])); /* XXX*/
                }
  break;
case 301:
					// line 1268 "DefaultRubyParser.y"
  {
                    yyVal = new CaseNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), null, ((Node)yyVals[-1+yyTop]));
                }
  break;
case 302:
					// line 1271 "DefaultRubyParser.y"
  {
		    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 303:
					// line 1274 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().begin();
                }
  break;
case 304:
					// line 1276 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().end();
                }
  break;
case 305:
					// line 1279 "DefaultRubyParser.y"
  {
                    yyVal = new ForNode(getPosition(((ISourcePositionHolder)yyVals[-8+yyTop])), ((Node)yyVals[-7+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-4+yyTop]));
                }
  break;
case 306:
					// line 1282 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("class definition in method body");
                    }
                    support.getLocalNames().push(new LocalNamesElement());
                    /* $$ = new Integer(ruby.getSourceLine());*/
                }
  break;
case 307:
					// line 1289 "DefaultRubyParser.y"
  {
                    yyVal = new ClassNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), ((Colon2Node)yyVals[-4+yyTop]), new ScopeNode(getRealPosition(((Node)yyVals[-1+yyTop])), ((LocalNamesElement) support.getLocalNames().peek()).getNames(), ((Node)yyVals[-1+yyTop])), ((Node)yyVals[-3+yyTop]));
                    /* $<Node>$.setLine($<Integer>4.intValue());*/
                    support.getLocalNames().pop();
                }
  break;
case 308:
					// line 1294 "DefaultRubyParser.y"
  {
                    yyVal = new Boolean(support.isInDef());
                    support.setInDef(false);
                }
  break;
case 309:
					// line 1297 "DefaultRubyParser.y"
  {
                    yyVal = new Integer(support.getInSingle());
                    support.setInSingle(0);
                    support.getLocalNames().push(new LocalNamesElement());
                }
  break;
case 310:
					// line 1302 "DefaultRubyParser.y"
  {
                    yyVal = new SClassNode(support.union(((Token)yyVals[-7+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-5+yyTop]), new ScopeNode(getRealPosition(((Node)yyVals[-1+yyTop])), ((LocalNamesElement) support.getLocalNames().peek()).getNames(), ((Node)yyVals[-1+yyTop])));
                    support.getLocalNames().pop();
                    support.setInDef(((Boolean)yyVals[-4+yyTop]).booleanValue());
                    support.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
                }
  break;
case 311:
					// line 1308 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) { 
                        yyerror("module definition in method body");
                    }
                    support.getLocalNames().push(new LocalNamesElement());
                    /* $$ = new Integer(ruby.getSourceLine());*/
                }
  break;
case 312:
					// line 1315 "DefaultRubyParser.y"
  {
                    yyVal = new ModuleNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Colon2Node)yyVals[-3+yyTop]), new ScopeNode(getRealPosition(((Node)yyVals[-1+yyTop])), ((LocalNamesElement) support.getLocalNames().peek()).getNames(), ((Node)yyVals[-1+yyTop])));
                    /* $<Node>$.setLine($<Integer>3.intValue());*/
                    support.getLocalNames().pop();
                }
  break;
case 313:
					// line 1320 "DefaultRubyParser.y"
  {
		      /* missing
			$<id>$ = cur_mid;
			cur_mid = $2; */
                    support.setInDef(true);
                    support.getLocalNames().push(new LocalNamesElement());
                }
  break;
case 314:
					// line 1328 "DefaultRubyParser.y"
  {
		      /* was in old jruby grammar support.getClassNest() !=0 || IdUtil.isAttrSet($2) ? Visibility.PUBLIC : Visibility.PRIVATE); */
                    /* NOEX_PRIVATE for toplevel */
                    yyVal = new DefnNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), new ArgumentNode(((ISourcePositionHolder)yyVals[-4+yyTop]).getPosition(), (String) ((Token)yyVals[-4+yyTop]).getValue()), ((Node)yyVals[-2+yyTop]),
		                      new ScopeNode(getRealPosition(((Node)yyVals[-1+yyTop])), ((LocalNamesElement) support.getLocalNames().peek()).getNames(), ((Node)yyVals[-1+yyTop])), Visibility.PRIVATE);
                    /* $<Node>$.setPosFrom($4);*/
                    support.getLocalNames().pop();
                    support.setInDef(false);
		    /* missing cur_mid = $<id>3; */
                }
  break;
case 315:
					// line 1338 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
                }
  break;
case 316:
					// line 1340 "DefaultRubyParser.y"
  {
                    support.setInSingle(support.getInSingle() + 1);
                    support.getLocalNames().push(new LocalNamesElement());
                    lexer.setState(LexState.EXPR_END); /* force for args */
                }
  break;
case 317:
					// line 1346 "DefaultRubyParser.y"
  {
                    yyVal = new DefsNode(support.union(((Token)yyVals[-8+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-7+yyTop]), (String) ((Token)yyVals[-4+yyTop]).getValue(), ((Node)yyVals[-2+yyTop]), new ScopeNode(getPosition(null), ((LocalNamesElement) support.getLocalNames().peek()).getNames(), ((Node)yyVals[-1+yyTop])));
                    /* $<Node>$.setPosFrom($2);*/
                    support.getLocalNames().pop();
                    support.setInSingle(support.getInSingle() - 1);
                }
  break;
case 318:
					// line 1352 "DefaultRubyParser.y"
  {
                    yyVal = new BreakNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                }
  break;
case 319:
					// line 1355 "DefaultRubyParser.y"
  {
                    yyVal = new NextNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                }
  break;
case 320:
					// line 1358 "DefaultRubyParser.y"
  {
                    yyVal = new RedoNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                }
  break;
case 321:
					// line 1361 "DefaultRubyParser.y"
  {
                    yyVal = new RetryNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                }
  break;
case 322:
					// line 1365 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
		    yyVal = ((Node)yyVals[0+yyTop]);
		}
  break;
case 331:
					// line 1382 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), support.getConditionNode(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 333:
					// line 1387 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 335:
					// line 1392 "DefaultRubyParser.y"
  {}
  break;
case 337:
					// line 1395 "DefaultRubyParser.y"
  {
                    yyVal = new ZeroArgNode(getPosition(null));
                }
  break;
case 338:
					// line 1398 "DefaultRubyParser.y"
  {
                    yyVal = new ZeroArgNode(getPosition(null));
		}
  break;
case 339:
					// line 1401 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 340:
					// line 1405 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push(new BlockNamesElement());
		}
  break;
case 341:
					// line 1408 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), null);
                    support.getBlockNames().pop();
                }
  break;
case 342:
					// line 1413 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-1+yyTop]) instanceof BlockPassNode) {
                        throw new SyntaxException(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), "Both block arg and actual block given.");
                    }
                    ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVals[-1+yyTop]));
                    yyVal = ((IterNode)yyVals[0+yyTop]);
                }
  break;
case 343:
					// line 1420 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 344:
					// line 1423 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 345:
					// line 1427 "DefaultRubyParser.y"
  {
                    yyVal = support.new_fcall((String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop])); /* .setPosFrom($2);*/
                }
  break;
case 346:
					// line 1430 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 347:
					// line 1433 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 348:
					// line 1436 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue(), null);
                }
  break;
case 349:
					// line 1439 "DefaultRubyParser.y"
  {
                    yyVal = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop]));
                }
  break;
case 350:
					// line 1442 "DefaultRubyParser.y"
  {
                    yyVal = new ZSuperNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                }
  break;
case 351:
					// line 1446 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push(new BlockNamesElement());
		}
  break;
case 352:
					// line 1448 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), null);
                    support.getBlockNames().pop();
                }
  break;
case 353:
					// line 1452 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push(new BlockNamesElement());
		}
  break;
case 354:
					// line 1454 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), null);
                    support.getBlockNames().pop();
                }
  break;
case 355:
					// line 1461 "DefaultRubyParser.y"
  {
		    yyVal = new WhenNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 357:
					// line 1466 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-3+yyTop]).add(new WhenNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Node)yyVals[0+yyTop]), null, null));
                }
  break;
case 358:
					// line 1469 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))).add(new WhenNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]), null, null));
                }
  break;
case 361:
					// line 1479 "DefaultRubyParser.y"
  {
                    Node node;
		    if (((Node)yyVals[-3+yyTop]) != null) {
                       node = support.appendToBlock(support.node_assign(((Node)yyVals[-3+yyTop]), new GlobalVarNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), "$!")), ((Node)yyVals[-1+yyTop]));
		    } else {
		       node = ((Node)yyVals[-1+yyTop]);
                    }
                    yyVal = new RescueBodyNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop]), true), ((Node)yyVals[-4+yyTop]), node, ((RescueBodyNode)yyVals[0+yyTop]));
		}
  break;
case 362:
					// line 1488 "DefaultRubyParser.y"
  {yyVal = null;}
  break;
case 363:
					// line 1490 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition()).add(((Node)yyVals[0+yyTop]));
		}
  break;
case 366:
					// line 1496 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 368:
					// line 1501 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[0+yyTop]) != null) {
                        yyVal = ((Node)yyVals[0+yyTop]);
                    } else {
                        yyVal = new NilNode(getPosition(null));
                    }
                }
  break;
case 371:
					// line 1511 "DefaultRubyParser.y"
  {
                    yyVal = new SymbolNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 373:
					// line 1516 "DefaultRubyParser.y"
  {
		    if (((Node)yyVals[0+yyTop]) == null) {
		        yyVal = new StrNode(getPosition(null), "");
		    } else {
		        if (((Node)yyVals[0+yyTop]) instanceof EvStrNode) {
			    yyVal = new DStrNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
			} else {
		            yyVal = ((Node)yyVals[0+yyTop]);
			}
		    }
		}
  break;
case 374:
					// line 1528 "DefaultRubyParser.y"
  {
                    yyVal = support.literal_concat(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), null, ((Node)yyVals[0+yyTop]));
		}
  break;
case 375:
					// line 1531 "DefaultRubyParser.y"
  {
                    yyVal = support.literal_concat(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
		}
  break;
case 376:
					// line 1535 "DefaultRubyParser.y"
  {
		     yyVal = ((Node)yyVals[-1+yyTop]);
		}
  break;
case 377:
					// line 1539 "DefaultRubyParser.y"
  {
		    if (((Node)yyVals[-1+yyTop]) == null) {
			  yyVal = new XStrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), null);
		    } else {
		      if (((Node)yyVals[-1+yyTop]) instanceof StrNode) {
			  yyVal = new XStrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((StrNode)yyVals[-1+yyTop]).getValue());
		      } else if (((Node)yyVals[-1+yyTop]) instanceof DStrNode) {
			  yyVal = new DXStrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(((Node)yyVals[-1+yyTop]));
		      } else {
			yyVal = new DXStrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(((Node)yyVals[-1+yyTop])));
		      }
		    }
                }
  break;
case 378:
					// line 1553 "DefaultRubyParser.y"
  {
		    int options = ((RegexpNode)yyVals[0+yyTop]).getOptions();
		    Node node = ((Node)yyVals[-1+yyTop]);

		    if (node == null) {
		        yyVal = new RegexpNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), "", options & ~ReOptions.RE_OPTION_ONCE);
		    } else if (node instanceof StrNode) {
		      yyVal = new RegexpNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((StrNode) node).getValue(), options & ~ReOptions.RE_OPTION_ONCE);
		    } else {
		        if (node instanceof DStrNode == false) {
			    node = new DStrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(node));
		        } 

			yyVal = new DRegexpNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), options, (options & ReOptions.RE_OPTION_ONCE) != 0).add(node);
		    }
		 }
  break;
case 379:
					// line 1570 "DefaultRubyParser.y"
  {
		     yyVal = new ZArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])));
		 }
  break;
case 380:
					// line 1573 "DefaultRubyParser.y"
  {
		     yyVal = ((ListNode)yyVals[-1+yyTop]);
		 }
  break;
case 381:
					// line 1577 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 382:
					// line 1580 "DefaultRubyParser.y"
  {
                     Node node = ((Node)yyVals[-1+yyTop]);

                     if (node instanceof EvStrNode) {
		       node = new DStrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(node);
		     }

		     yyVal = ((ListNode)yyVals[-2+yyTop]).add(node);
		 }
  break;
case 384:
					// line 1591 "DefaultRubyParser.y"
  {
                     yyVal = support.literal_concat(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
	         }
  break;
case 385:
					// line 1595 "DefaultRubyParser.y"
  {
		     yyVal = new ZArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])));
		 }
  break;
case 386:
					// line 1598 "DefaultRubyParser.y"
  {
		     yyVal = ((ListNode)yyVals[-1+yyTop]);
		 }
  break;
case 387:
					// line 1602 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 388:
					// line 1605 "DefaultRubyParser.y"
  {
                     if (((ListNode)yyVals[-2+yyTop]) == null) {
		         yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(new StrNode(((ISourcePositionHolder)yyVals[-1+yyTop]).getPosition(), (String) ((Token)yyVals[-1+yyTop]).getValue()));
		     } else {
		         yyVal = ((ListNode)yyVals[-2+yyTop]).add(new StrNode(((ISourcePositionHolder)yyVals[-1+yyTop]).getPosition(), (String) ((Token)yyVals[-1+yyTop]).getValue()));
		     }
		 }
  break;
case 389:
					// line 1613 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 390:
					// line 1616 "DefaultRubyParser.y"
  {
                     yyVal = support.literal_concat(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
		 }
  break;
case 391:
					// line 1620 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 392:
					// line 1623 "DefaultRubyParser.y"
  {
                     yyVal = support.literal_concat(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
		 }
  break;
case 393:
					// line 1628 "DefaultRubyParser.y"
  {
                      yyVal = new StrNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
                  }
  break;
case 394:
					// line 1631 "DefaultRubyParser.y"
  {
                      yyVal = lexer.getStrTerm();
		      lexer.setStrTerm(null);
		      lexer.setState(LexState.EXPR_BEG);
		  }
  break;
case 395:
					// line 1635 "DefaultRubyParser.y"
  {
		      lexer.setStrTerm(((StrTerm)yyVals[-1+yyTop]));
		      yyVal = new EvStrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop]));
		  }
  break;
case 396:
					// line 1639 "DefaultRubyParser.y"
  {
		      yyVal = lexer.getStrTerm();
		      lexer.setStrTerm(null);
		      lexer.setState(LexState.EXPR_BEG);
		  }
  break;
case 397:
					// line 1643 "DefaultRubyParser.y"
  {
		      lexer.setStrTerm(((StrTerm)yyVals[-2+yyTop]));
		      Node node = ((Node)yyVals[-1+yyTop]);

		      if (node instanceof NewlineNode) {
		        node = ((NewlineNode)node).getNextNode();
		      }

		      yyVal = support.newEvStrNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), node);
		  }
  break;
case 398:
					// line 1654 "DefaultRubyParser.y"
  {
                      yyVal = new GlobalVarNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                 }
  break;
case 399:
					// line 1657 "DefaultRubyParser.y"
  {
                      yyVal = new InstVarNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                 }
  break;
case 400:
					// line 1660 "DefaultRubyParser.y"
  {
                      yyVal = new ClassVarNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                 }
  break;
case 402:
					// line 1666 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    yyVal = ((Token)yyVals[0+yyTop]);
		    ((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])));
                }
  break;
case 407:
					// line 1677 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);

		    /* In ruby, it seems to be possible to get a*/
		    /* StrNode (NODE_STR) among other node type.  This */
		    /* is not possible for us.  We will always have a */
		    /* DStrNode (NODE_DSTR).*/
		    yyVal = new DSymbolNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((DStrNode)yyVals[-1+yyTop]));
		}
  break;
case 408:
					// line 1687 "DefaultRubyParser.y"
  {
                    Object number = ((Token)yyVals[0+yyTop]).getValue();

                    if (number instanceof Long) {
		        yyVal = new FixnumNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), ((Long) number).longValue());
                    } else {
		        yyVal = new BignumNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (BigInteger) number);
                    }
                }
  break;
case 409:
					// line 1696 "DefaultRubyParser.y"
  {
                    yyVal = new FloatNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), ((Double) ((Token)yyVals[0+yyTop]).getValue()).doubleValue());
	        }
  break;
case 410:
					// line 1699 "DefaultRubyParser.y"
  {
                    Object number = ((Token)yyVals[0+yyTop]).getValue();

                    yyVal = support.getOperatorCallNode((number instanceof Long ? (Node) new FixnumNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Long) number).longValue()) : (Node) new BignumNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), (BigInteger) number)), "-@");
		}
  break;
case 411:
					// line 1704 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(new FloatNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Double) ((Token)yyVals[0+yyTop]).getValue()).doubleValue()), "-@");
		}
  break;
case 412:
					// line 1713 "DefaultRubyParser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 413:
					// line 1716 "DefaultRubyParser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 414:
					// line 1719 "DefaultRubyParser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 415:
					// line 1722 "DefaultRubyParser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 416:
					// line 1725 "DefaultRubyParser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 417:
					// line 1728 "DefaultRubyParser.y"
  { 
                    yyVal = new NilNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
                }
  break;
case 418:
					// line 1731 "DefaultRubyParser.y"
  {
                    yyVal = new SelfNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
                }
  break;
case 419:
					// line 1734 "DefaultRubyParser.y"
  { 
		    yyVal = new TrueNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
                }
  break;
case 420:
					// line 1737 "DefaultRubyParser.y"
  {
		    yyVal = new FalseNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
                }
  break;
case 421:
					// line 1740 "DefaultRubyParser.y"
  {
                    yyVal = new StrNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), getPosition(((ISourcePositionHolder)yyVals[0+yyTop])).getFile());
                }
  break;
case 422:
					// line 1743 "DefaultRubyParser.y"
  {
                    yyVal = new FixnumNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), getPosition(((ISourcePositionHolder)yyVals[0+yyTop])).getEndLine() + 1);
                }
  break;
case 423:
					// line 1747 "DefaultRubyParser.y"
  {
                    /* Work around __LINE__ and __FILE__ */
                    if (yyVals[0+yyTop] instanceof INameNode) {
		        String name = ((INameNode)yyVals[0+yyTop]).getName();
                        yyVal = support.gettable(name, ((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
		    } else if (yyVals[0+yyTop] instanceof Token) {
		      yyVal = support.gettable((String) ((Token)yyVals[0+yyTop]).getValue(), ((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
		    } else {
		        yyVal = yyVals[0+yyTop];
		    }
                }
  break;
case 424:
					// line 1760 "DefaultRubyParser.y"
  {
                    yyVal = support.assignable(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), yyVals[0+yyTop], null);
                }
  break;
case 427:
					// line 1767 "DefaultRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 428:
					// line 1770 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 429:
					// line 1772 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 430:
					// line 1775 "DefaultRubyParser.y"
  {
                    yyerrok();
                    yyVal = null;
                }
  break;
case 431:
					// line 1780 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-2+yyTop]);
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 432:
					// line 1784 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 433:
					// line 1788 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 434:
					// line 1791 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 435:
					// line 1794 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), null, ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 436:
					// line 1797 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(((ISourcePositionHolder)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 437:
					// line 1800 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), null, ((ListNode)yyVals[-3+yyTop]), ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 438:
					// line 1803 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), null, ((ListNode)yyVals[-1+yyTop]), -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 439:
					// line 1806 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), null, null, ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 440:
					// line 1809 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), null, null, -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 441:
					// line 1812 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(getPosition(null), null, null, -1, null);
                }
  break;
case 442:
					// line 1816 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be a constant");
                }
  break;
case 443:
					// line 1819 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be an instance variable");
                }
  break;
case 444:
					// line 1822 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be a class variable");
                }
  break;
case 445:
					// line 1825 "DefaultRubyParser.y"
  {
                   String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();
                   if (!IdUtil.isLocal(identifier)) {
                        yyerror("formal argument must be local variable");
                    } else if (((LocalNamesElement) support.getLocalNames().peek()).isLocalRegistered(identifier)) {
                        yyerror("duplicate argument name");
                    }
		    /* Register new local var or die trying (side-effect)*/
                    ((LocalNamesElement) support.getLocalNames().peek()).getLocalIndex(identifier);
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 446:
					// line 1837 "DefaultRubyParser.y"
  {
                    yyVal = new ListNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
                    ((ListNode) yyVal).add(new ArgumentNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue()));
                }
  break;
case 447:
					// line 1841 "DefaultRubyParser.y"
  {
                    ((ListNode)yyVals[-2+yyTop]).add(new ArgumentNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue()));
                    ((ListNode)yyVals[-2+yyTop]).setPosition(support.union(((ListNode)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
		    yyVal = ((ListNode)yyVals[-2+yyTop]);
                }
  break;
case 448:
					// line 1847 "DefaultRubyParser.y"
  {
                    String identifier = (String) ((Token)yyVals[-2+yyTop]).getValue();

                    if (!IdUtil.isLocal(identifier)) {
                        yyerror("formal argument must be local variable");
                    } else if (((LocalNamesElement) support.getLocalNames().peek()).isLocalRegistered(identifier)) {
                        yyerror("duplicate optional argument name");
                    }
		    ((LocalNamesElement) support.getLocalNames().peek()).getLocalIndex(identifier);
                    yyVal = support.assignable(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), identifier, ((Node)yyVals[0+yyTop]));
                }
  break;
case 449:
					// line 1859 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 450:
					// line 1862 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 453:
					// line 1869 "DefaultRubyParser.y"
  {
                    String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();

                    if (!IdUtil.isLocal(identifier)) {
                        yyerror("rest argument must be local variable");
                    } else if (((LocalNamesElement) support.getLocalNames().peek()).isLocalRegistered(identifier)) {
                        yyerror("duplicate rest argument name");
                    }
		    ((Token)yyVals[-1+yyTop]).setValue(new Integer(((LocalNamesElement) support.getLocalNames().peek()).getLocalIndex(identifier)));
                    yyVal = ((Token)yyVals[-1+yyTop]);
                }
  break;
case 454:
					// line 1880 "DefaultRubyParser.y"
  {
                    ((Token)yyVals[0+yyTop]).setValue(new Integer(-2));
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 457:
					// line 1888 "DefaultRubyParser.y"
  {
                    String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();

                    if (!IdUtil.isLocal(identifier)) {
                        yyerror("block argument must be local variable");
                    } else if (((LocalNamesElement) support.getLocalNames().peek()).isLocalRegistered(identifier)) {
                        yyerror("duplicate block argument name");
                    }
                    yyVal = new BlockArgNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((LocalNamesElement) support.getLocalNames().peek()).getLocalIndex(identifier));
                }
  break;
case 458:
					// line 1899 "DefaultRubyParser.y"
  {
                    yyVal = ((BlockArgNode)yyVals[0+yyTop]);
                }
  break;
case 459:
					// line 1902 "DefaultRubyParser.y"
  {
	            yyVal = null;
	        }
  break;
case 460:
					// line 1906 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[0+yyTop]) instanceof SelfNode) {
		        yyVal = new SelfNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
                    } else {
			support.checkExpression(((Node)yyVals[0+yyTop]));
			yyVal = ((Node)yyVals[0+yyTop]);
		    }
                }
  break;
case 461:
					// line 1914 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 462:
					// line 1916 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-2+yyTop]) instanceof ILiteralNode) {
                        /*case Constants.NODE_STR:
                        case Constants.NODE_DSTR:
                        case Constants.NODE_XSTR:
                        case Constants.NODE_DXSTR:
                        case Constants.NODE_DREGX:
                        case Constants.NODE_LIT:
                        case Constants.NODE_ARRAY:
                        case Constants.NODE_ZARRAY:*/
                        yyerror("Can't define single method for literals.");
                    }
		    support.checkExpression(((Node)yyVals[-2+yyTop]));
                    yyVal = ((Node)yyVals[-2+yyTop]);
                }
  break;
case 464:
					// line 1933 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                }
  break;
case 465:
					// line 1936 "DefaultRubyParser.y"
  {
                    if (((ListNode)yyVals[-1+yyTop]).size() % 2 != 0) {
                        yyerror("Odd number list for Hash.");
                    }
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                }
  break;
case 467:
					// line 1944 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).addAll(((ListNode)yyVals[0+yyTop]));
                }
  break;
case 468:
					// line 1948 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]))).add(((Node)yyVals[-2+yyTop])).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 488:
					// line 1978 "DefaultRubyParser.y"
  {
                    yyerrok();
                }
  break;
case 491:
					// line 1984 "DefaultRubyParser.y"
  {
                    yyerrok();
                }
  break;
case 492:
					// line 1988 "DefaultRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 493:
					// line 1992 "DefaultRubyParser.y"
  {  yyVal = null;
		  }
  break;
case 494:
					// line 1995 "DefaultRubyParser.y"
  {  yyVal = null;
		  }
  break;
					// line 7647 "-"
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

					// line 1999 "DefaultRubyParser.y"

    /** The parse method use an lexer stream and parse it to an AST node 
     * structure
     */
    public RubyParserResult parse(LexerSource source) {
        support.reset();
        support.setResult(new RubyParserResult());
        
        lexer.reset();
        lexer.setSource(source);
        support.setPositionFactory(lexer.getPositionFactory());
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

    public void init(RubyParserConfiguration configuration) {
        support.setConfiguration(configuration);
    }

    // +++
    // Helper Methods
    
    void yyerrok() {}

    private ISourcePosition getRealPosition(Node node) {
      if (node == null) {
	return getPosition(null);
      }

      if (node instanceof BlockNode) {
	return node.getPosition();
      }

      if (node instanceof NewlineNode) {
	while (node instanceof NewlineNode) {
	  node = ((NewlineNode) node).getNextNode();
	}
	return node.getPosition();
      }

      return getPosition((ISourcePositionHolder)node);
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
					// line 7742 "-"
