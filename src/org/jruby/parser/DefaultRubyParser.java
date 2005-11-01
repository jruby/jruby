// created by jay 1.0 (c) 2002 ats@cs.rit.edu
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
        this.support = new ParserSupport();
        this.lexer = new RubyYaccLexer();
        // lame
        this.lexer.setParserSupport(support);
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
  public static final int tBACK_REF2 = 315;
  public static final int tREGEXP_END = 316;
  public static final int tUPLUS = 317;
  public static final int tUMINUS = 318;
  public static final int tUMINUS_NUM = 319;
  public static final int tPOW = 320;
  public static final int tCMP = 321;
  public static final int tEQ = 322;
  public static final int tEQQ = 323;
  public static final int tNEQ = 324;
  public static final int tGEQ = 325;
  public static final int tLEQ = 326;
  public static final int tANDOP = 327;
  public static final int tOROP = 328;
  public static final int tMATCH = 329;
  public static final int tNMATCH = 330;
  public static final int tDOT = 331;
  public static final int tDOT2 = 332;
  public static final int tDOT3 = 333;
  public static final int tAREF = 334;
  public static final int tASET = 335;
  public static final int tLSHFT = 336;
  public static final int tRSHFT = 337;
  public static final int tCOLON2 = 338;
  public static final int tCOLON3 = 339;
  public static final int tOP_ASGN = 340;
  public static final int tASSOC = 341;
  public static final int tLPAREN = 342;
  public static final int tLPAREN2 = 343;
  public static final int tLPAREN_ARG = 344;
  public static final int tLBRACK = 345;
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
    52,    52,    94,    94,    94,    94,    95,    95,    53,    84,
    84,    98,    98,    96,    96,    99,    99,    50,    49,    49,
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
   130,   126,   127,   124,   108,   109,   110,   113,   115,   111,
   128,   129,   116,   117,   461,   121,   120,   107,   125,   123,
   122,   118,   119,   114,   112,   105,   106,     0,   460,   313,
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
     0,     0,   432,     0,   438,     0,   436,     0,   439,   453,
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
     0,     0, 14421, 14646, 17670, 18006, 18556, 18449, 14421, 16214,
 16214, 11555,     0,     0, 17782, 14870, 14870,     0,     0, 14870,
  -255,  -207,     0,     0,     0,     0, 16214, 18342,   140,     0,
  -135,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0, 17222, 17222,  -115,  -136, 14534, 16214, 16326,
 17222, 18118, 17222, 17334, 18661,     0,     0,     0,   233,   249,
     0,  -119,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   147,   719,   250,  2411,     0,   -31,     0,  -216,     4,
     0,   -51,     0,   -71,   217,     0,   292,     0,   298,     0,
 17894,     0,    94,     0,  -220,   719,     0,     0,     0,  -255,
  -207,   140,     0,     0,   241, 16214,  -152, 14421,    20,     0,
   146,     0,     0,  -220,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   295,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
 18661,   386,     0,     0,     0,   178,   202,   173,   250,    26,
   184,   192,   425,     0,   204,    26,     0,     0,   147,  -175,
   471,     0, 16214, 16214,   274,   186,     0,   270,     0,     0,
     0, 17222, 17222, 17222,  2411,     0,     0,     0,   258,   578,
   580,     0,     0,     0, 13952,     0, 14982, 14870,     0,     0,
     0,  -237,     0,     0,   282,   272, 14421,     0,   237,   317,
   319,   321,   296, 14534,   602,     0,   608,    30, 17222,   140,
     0,    45,    55,   549,   123,    55,     0,   525,   343,   347,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  -188,
     0,     0,  -185,  -176,   219,   281,  -108,   286,  -228,     0,
     0, 14064, 16214, 16214, 16214, 16214, 14646, 16214, 16214, 17222,
 17222, 17222, 17222, 17222, 17222, 17222, 17222, 17222, 17222, 17222,
 17222, 17222, 17222, 17222, 17222, 17222, 17222, 17222, 17222, 17222,
 17222, 17222, 17222, 17222, 17222,     0,     0,     0,     0,     0,
  1988,  2469, 16326,  2535,  2535, 17334, 16438,     0, 16438, 14534,
 18118,   614, 17334,     0,   314,     0,   282,     0,     0,   250,
     0,     0,     0,   147,     0,     0,     0,  2535,  2896, 16326,
 14421, 16214,     0,     0,     0,   767,     0, 16550,   393,     0,
   296,     0,     0, 14421,   399,  2969,  3035, 16326, 17222, 17222,
 17222, 14421,   396, 14421, 16662,   405,     0,   129,   129,     0,
  3377,  3450, 16326,     0,   627,     0, 17222, 15094,     0,     0,
 15206,     0,     0,   631, 14758,     0,     0,   -31,   140,    90,
   629,   634,     0,     0,     0, 18449,     0, 17222, 14421,   551,
  2969,  3035, 17222, 17222, 17222,   636,     0,     0,   639,  1876,
     0, 16774,     0,     0, 17222,     0,     0, 17222,     0,     0,
     0,     0,  3516,  3823, 16326,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,    15,     0,   650,
     0,   719,     0,     0,     0,     0,     0,     0,     0,   317,
  2055,  2055,  2055,  2055,  4364,  4364,  4324,  4274,  2055,  2055,
  3993,  3993,   263,   263,   317,  1587,   317,   317,  -190,  -190,
  4364,  4364,  2014,  2014,  3319,  -193,  -193,  -193,   344,     0,
   351,  -207,     0,   353,     0,   356,  -207,     0,     0,   616,
     0,     0,  -207,  -207,  2411,     0, 17222,  1927,     0,     0,
     0,   675,     0,     0,     0,   684,     0,     0,  2411,     0,
     0,     0,   147,     0, 16214, 14421,  -207,     0,     0,  -207,
     0,   641,   472,    30, 18766,   676,     0,     0,     0,   916,
     0,     0,     0,     0,     0, 14421,   147,     0,   695,     0,
   697,   698,   439,   442, 18449,     0,     0,     0,   404, 14421,
   488,     0,   326,     0,   413,   415,   416,   356,   666,  1927,
   393,   507,   510, 17222,   731,    26,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   683, 16214,   432,     0,
     0, 17222,     0,   258,   747, 17222,   258,     0,     0, 17222,
  2411,     0,     0,     2,   748,   757,   758,  2535,  2535,   759,
 15318,     0,     0, 16214,  2411,   678,     0,   317,   317,  2411,
     0,     0,     0, 17222,     0,     0,     0,     0,     0,   717,
 14421,   568,     0,     0,     0, 17222,     0, 17558, 14421,     0,
 14421, 14421, 17334, 17334,     0,     0,   314,     0, 17334, 17222,
     0,   314,   473,     0,     0,     0,     0,     0, 17222, 16886,
     0,  -193,     0,   147,   560,     0,     0,   786,     0, 17222,
   140,   566,     0,   352,     0,   916,     0,   213,     0,     0,
     0,     0, 18230,    26,     0,     0, 14421,     0,     0, 16214,
     0,   571, 17222, 17222, 17222,   496,   575,     0,     0,     0,
 16998, 14421, 14421, 14421,     0,   129,   627, 15430,     0,   627,
   627,   800, 15542, 15654,     0,     0,     0,  -207,  -207,     0,
   -31,    90,   -24,     0,  1876,     0,   722,     0,     0,     0,
     0,     0,  2411,     0,   483,   579,   589,   732,  2411,     0,
  2411,     0,     0,  2411,     0,  2411,     0, 17334,  2411, 17222,
     0, 14421, 14421,     0,     0,     0,   767,     0,   803,     0,
   676,     0,     0,   698,   817,     0,   698,   556,   350,     0,
     0,     0, 14421,     0,    26,     0, 17222,     0, 17222,   -96,
   605,   609,     0,     0, 17222,     0,     0,     0, 17222,     0,
   832,   834, 17222,   838,     0,     0,     0,     0,     0,     0,
     0,  2411,     0,     0,   768,   630, 14421,     0,     0,   352,
     0,     0,     0,  3885,  3949, 16326,   178, 14421,     0,     0,
     0,     0,     0,     0, 14421,  2838,   627, 15766,     0, 15878,
     0,   627,     0,     0,     0,   633,   698,     0,     0,     0,
     0,   802,     0,   326,   635,     0,     0, 17222,   862, 17222,
     0,     0,     0,     0,     0,     0,   627, 15990,     0,   627,
     0, 17222,     0,   627,     0,
    }, yyRindex = {
//yyRindex 885
     0,     0,   118,     0,     0,     0,     0,     0,   596,     0,
     0,    21,     0,     0,     0,  7744,  7906,     0,     0,  8094,
  4218,  3625,     0,     0,     0,     0,     0,     0, 17110,     0,
     0,     0,     0,  1690,  2775,     0,     0,  1813,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   130,     0,   816,
   785,    66,     0,     0,   -97,     0,     0,     0,   191,  -227,
     0,  6708,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  1306,  5596,  6441,  6353,  7012,  9417,     0,  7112,     0,
     0, 14333,     0,  9291,     0,     0,     0,     0,     0,     0,
   174,  9177,     0,     0, 16102,  5943,     0,     0,     0,  7226,
  5642,   569, 10631, 10745,     0,     0,     0,   130,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   821,
   969,  1197,  1305,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  1378,  1499,  2052,     0,  2362,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  6954,     0,     0,     0,  -172,     0,     0, 14379,     0,
     0,  6190,     0,  6077,     0,     0,     0,     0,   640,     0,
    70,     0,     0,     0,     0,     0,   532,     0,     0,     0,
   577,     0,     0,     0, 10342,     0,     0,     0, 13755,  5835,
  5835,     0,     0,     0,     0,     0,     0,   875,     0,     0,
     0,     0,     0,     0, 17446,     0,    53,     0,     0,  8210,
  7530,  7630,  9478,   130,     0,    29,     0,   876,     0,   827,
     0,   828,   828,     0,   797,   797,     0,     0,     0,     0,
  1107,     0,  1542,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  1625,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   816,     0,     0,     0,     0,     0,     0,   130,
   189,   199,     0,     0,  8051,     0,     0,     0,     0,   160,
     0, 11093,     0,     0,     0,     0,     0,     0,     0,   816,
   596,     0,     0,     0,     0,   223,     0,   103,   346,     0,
  6494,     0,     0,   426, 11207,     0,     0,   816,     0,     0,
     0,   145,     0,   211,     0,     0,     0,     0,     0,   870,
     0,     0,   816,     0,  5835,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   882,     0,     0,   150,   876,   876,
   316,     0,     0,     0,     0,     0,     0,     0,    53,     0,
     0,     0,     0,     0,     0,     0,     0,    97,     0,   827,
     0,   836,     0,     0,   163,     0,     0,   808,     0,     0,
     0,  1567,     0,     0,   816,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  6016,     0,     0,     0,     0,     0,     0,     0,  8311,
 12311, 12397, 12472, 12577, 11845, 11927, 12663, 12961, 12770, 12856,
  6898,  7416,  9968, 10082,  8587, 10182,  8687,  8801,  9692,  9840,
 12009, 12121, 10424, 10524,     0, 11324, 11324, 11438,  4580,     0,
  4692,  3737,     0,  5061,  3144,  5173, 16102,     0,  3256,     0,
     0,     0,  5535,  5535, 13036,     0,     0,  8470,     0,     0,
     0,     0,     0,     0,  6429,     0,     0,     0, 13122,     0,
     0,     0,     0,     0,     0,   596,  5976, 10862, 10976,     0,
     0,     0,     0,   876,     0,   506,     0,     0,     0,   484,
     0,     0,     0,     0,     0,   596,     0,     0,   441,     0,
   441,   441,   561,     0,     0,     0,     0,   121,   188,   273,
   661,     0,   661,     0,  2182,  2294,  2663,  4106,     0, 13817,
   661,     0,     0,     0,   190,     0,     0,     0,     0,     0,
     0,     0,   746,   852,  1361,   435,     0,     0,     0,     0,
     0,     0,     0, 13906,  5835,     0,     0,     0,     0,     0,
   182,     0,     0,     0,   897,     0,     0,     0,     0,     0,
     0,     0,     0,     0, 13160,     0,     0,  8929,  9077, 13221,
   526,     0,     0,     0,     0,   723,  1431,  5583,  1493,     0,
    53,     0,     0,     0,     0,     0,     0,     0,   211,     0,
   211,    53,     0,     0,     0,     0, 14221,     0,     0,     0,
     0, 14268,  9592,     0,     0,     0,     0,     0,     0,     0,
     0, 11438,     0,     0,     0,     0,     0,     0,     0,     0,
   876,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   211,     0,     0,     0,
     0,     0,     0,     0,     0,  6594,     0,     0,     0,     0,
     0,   444,   211,   211,   903,     0,  5835,     0,     0,  5835,
   897,     0,     0,     0,     0,     0,     0,    40,    40,     0,
     0,   876,     0,     0,   827,  5589,     0,     0,     0,     0,
     0,     0, 13309,     0,     0,     0,     0,     0, 13351,     0,
 13420,     0,     0, 13538,     0, 13627,     0,     0, 13664,     0,
  8458,    53,   596,     0,     0,     0,   223,     0,     0,     0,
     0,     0,     0,   441,   441,     0,   441,     0,     0,   166,
     0,   601,   596,     0,     0,     0,     0,     0,     0,   661,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   897,   897,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 13719,     0,     0,     0,     0,   596,     0,     0,     0,
     0,     0,   721,     0,     0,   816,  -172,   426,     0,     0,
     0,     0,     0,     0,   211,  5835,   897,     0,     0,     0,
     0,   897,     0,     0,     0,     0,   441,   760,   908,  1028,
   200,     0,     0,   661,     0,     0,     0,     0,   897,     0,
     0,     0,     0,  1062,     0,     0,   897,     0,     0,   897,
     0,     0,     0,   897,     0,
    }, yyGindex = {
//yyGindex 138
     0,     0,     0,     0,   880,     0,     0,     0,   557,  -247,
     0,     0,     0,     0,     0,     0,   937,   127,  -337,     0,
    28,  1032,   -15,    47,   105,    48,     0,     0,     0,    63,
   100,  -368,     0,    82,     0,     0,    -7,   -10,   252,     0,
     0,    23,   943,   334,    -3,     0,     0,  -241,     0,   246,
  -360,   180,   398,  -661,     0,     0,   718,   302,  -439,   793,
  1020,   877,     0,  -382,   179,   872,     5,  1053,  -372,   -11,
     1,  -204,    -8,     0,     0,     8,  -369,     0,  -316,   124,
   899,   974,   716,     0,   275,    -5,     0,    -2,   582,   120,
     0,   -44,     3,    12,   277,     0,  -615,     0,     0,     0,
     0,   -17,   907,     0,     0,     0,     0,     0,   -58,     0,
   305,     0,     0,     0,     0,  -213,     0,  -377,     0,     0,
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
    "tBACK_REF","tBACK_REF2","tREGEXP_END","tUPLUS","tUMINUS",
    "tUMINUS_NUM","tPOW","tCMP","tEQ","tEQQ","tNEQ","tGEQ","tLEQ",
    "tANDOP","tOROP","tMATCH","tNMATCH","tDOT","tDOT2","tDOT3","tAREF",
    "tASET","tLSHFT","tRSHFT","tCOLON2","tCOLON3","tOP_ASGN","tASSOC",
    "tLPAREN","tLPAREN2","tLPAREN_ARG","tLBRACK","tLBRACE","tLBRACE_ARG",
    "tSTAR","tSTAR2","tAMPER","tAMPER2","tTILDE","tPERCENT","tDIVIDE",
    "tPLUS","tMINUS","tLT","tGT","tPIPE","tBANG","tCARET","tLCURLY",
    "tSYMBEG","tSTRING_BEG","tXSTRING_BEG","tREGEXP_BEG","tWORDS_BEG",
    "tQWORDS_BEG","tSTRING_DBEG","tSTRING_DVAR","tSTRING_END","tLOWEST",
    "tLAST_TOKEN",
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
            // We only want to wrap tokens and nodes are sometimes
            // created by the lexer...
            if (!(yyVal instanceof Node)) {
              yyVal = new Token(yyVal, yyLex.getPosition(null));
            }
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
                  // We only want to wrap tokens and nodes are sometimes
                  // created by the lexer...
                  if (!(yyVal instanceof Node)) {
                      yyVal = new Token(yyVal, yyLex.getPosition());
                  }
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
					// line 320 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_BEG);
                  support.initTopLocalVariables();

		  /* Fix: Move to ruby runtime....?*/
                  /*if (ruby.getRubyClass() == ruby.getClasses().getObjectClass()) {*/
                  /*    support.setClassNest(0);*/
                  /*} else {*/
                  /*    support.setClassNest(1);*/
                  /*}*/
              }
  break;
case 2:
					// line 330 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[0+yyTop]) != null && !support.isCompileForEval()) {
                      /* last expression should not be void */
                      if (((Node)yyVals[0+yyTop]) instanceof BlockNode) {
                          support.checkUselessStatement(((BlockNode)yyVals[0+yyTop]).getLast());
                      } else {
                          support.checkUselessStatement(((Node)yyVals[0+yyTop]));
                      }
                  }
                  support.getResult().setAST(support.appendToBlock(support.getResult().getAST(), ((Node)yyVals[0+yyTop])));
                  support.updateTopLocalVariables();
                  support.setClassNest(0);
              }
  break;
case 3:
					// line 347 "DefaultRubyParser.y"
  {
                 Node node = ((Node)yyVals[-3+yyTop]);

		 if (((RescueBodyNode)yyVals[-2+yyTop]) != null) {
		       node = new RescueNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop]), ((RescueBodyNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
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
					// line 363 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-1+yyTop]) instanceof BlockNode) {
                     support.checkUselessStatements(((BlockNode)yyVals[-1+yyTop]));
		  }
                  yyVal = ((Node)yyVals[-1+yyTop]);
              }
  break;
case 6:
					// line 371 "DefaultRubyParser.y"
  {
                    yyVal = support.newline_node(((Node)yyVals[0+yyTop]), getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                }
  break;
case 7:
					// line 374 "DefaultRubyParser.y"
  {
                    yyVal = support.appendToBlock(((Node)yyVals[-2+yyTop]), support.newline_node(((Node)yyVals[0+yyTop]), getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))));
                }
  break;
case 8:
					// line 377 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 9:
					// line 381 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
                }
  break;
case 10:
					// line 383 "DefaultRubyParser.y"
  {
                    yyVal = new AliasNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 11:
					// line 386 "DefaultRubyParser.y"
  {
                    yyVal = new VAliasNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 12:
					// line 389 "DefaultRubyParser.y"
  {
                    yyVal = new VAliasNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), "$" + ((BackRefNode)yyVals[0+yyTop]).getType()); /* XXX*/
                }
  break;
case 13:
					// line 392 "DefaultRubyParser.y"
  {
                    yyerror("can't make alias for the number variables");
                    yyVal = null; /*XXX 0*/
                }
  break;
case 14:
					// line 396 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 15:
					// line 399 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), null);
                }
  break;
case 16:
					// line 402 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), null, ((Node)yyVals[-2+yyTop]));
                }
  break;
case 17:
					// line 405 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = new WhileNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                    } else {
                        yyVal = new WhileNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                    }
                }
  break;
case 18:
					// line 412 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = new UntilNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode());
                    } else {
                        yyVal = new UntilNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]));
                    }
                }
  break;
case 19:
					// line 420 "DefaultRubyParser.y"
  {
		  yyVal = new RescueNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), new RescueBodyNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), null,((Node)yyVals[0+yyTop]), null), null);
                }
  break;
case 20:
					// line 424 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("BEGIN in method");
                    }
                    support.getLocalNames().push(new LocalNamesElement());
                }
  break;
case 21:
					// line 429 "DefaultRubyParser.y"
  {
  support.getResult().addBeginNode(new ScopeNode(getPosition(null), ((LocalNamesElement) support.getLocalNames().peek()).getNames(), ((Node)yyVals[-1+yyTop])));
                    support.getLocalNames().pop();
                    yyVal = null; /*XXX 0;*/
                }
  break;
case 22:
					// line 434 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("END in method; use at_exit");
                    }
                    support.getResult().addEndNode(new IterNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), null, new PostExeNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop]))), ((Node)yyVals[-1+yyTop])));
                    yyVal = null;
                }
  break;
case 23:
					// line 441 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 24:
					// line 445 "DefaultRubyParser.y"
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
					// line 454 "DefaultRubyParser.y"
  {
 		    support.checkExpression(((Node)yyVals[0+yyTop]));
		    if (yyVals[-2+yyTop] != null) {
		        String name = ((INameNode)yyVals[-2+yyTop]).getName();
			String asgnOp = (String) ((Token)yyVals[-1+yyTop]).getValue();
		        if (asgnOp.equals("||")) {
	                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
	                    yyVal = new OpAsgnOrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.gettable(name, getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))), ((Node)yyVals[-2+yyTop]));
			    /* XXX
			    if (is_asgn_or_id(vid)) {
				$$->nd_aid = vid;
			    }
			    */
			} else if (asgnOp.equals("&&")) {
	                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                            yyVal = new OpAsgnAndNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.gettable(name, getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))), ((Node)yyVals[-2+yyTop]));
			} else {
			    yyVal = yyVals[-2+yyTop];
                            if (yyVal != null) {
                                ((AssignableNode)yyVal).setValueNode(support.getOperatorCallNode(support.gettable(name, getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))), asgnOp, ((Node)yyVals[0+yyTop])));
                            }
			}
		    } else {
 		        yyVal = null;
		    }
		}
  break;
case 26:
					// line 480 "DefaultRubyParser.y"
  {
                    /* Much smaller than ruby block */
                    yyVal = new OpElementAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));

                }
  break;
case 27:
					// line 485 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 28:
					// line 488 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 29:
					// line 491 "DefaultRubyParser.y"
  {
  yyVal = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 30:
					// line 494 "DefaultRubyParser.y"
  {
                    support.backrefAssignError(((Node)yyVals[-2+yyTop]));
                    yyVal = null;
                }
  break;
case 31:
					// line 498 "DefaultRubyParser.y"
  {
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), new SValueNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
                }
  break;
case 32:
					// line 501 "DefaultRubyParser.y"
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
					// line 509 "DefaultRubyParser.y"
  {
                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
		    yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
		}
  break;
case 36:
					// line 516 "DefaultRubyParser.y"
  {
                    yyVal = support.newAndNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 37:
					// line 519 "DefaultRubyParser.y"
  {
                    yyVal = support.newOrNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 38:
					// line 522 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
                }
  break;
case 39:
					// line 525 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
                }
  break;
case 41:
					// line 530 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
		    yyVal = ((Node)yyVals[0+yyTop]); /*Do we really need this set? $1 is $$?*/
		}
  break;
case 44:
					// line 537 "DefaultRubyParser.y"
  {
                    yyVal = new ReturnNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))));
                }
  break;
case 45:
					// line 540 "DefaultRubyParser.y"
  {
                    yyVal = new BreakNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))));
                }
  break;
case 46:
					// line 543 "DefaultRubyParser.y"
  {
                    yyVal = new NextNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))));
                }
  break;
case 48:
					// line 548 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 49:
					// line 551 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 50:
					// line 555 "DefaultRubyParser.y"
  {
                      support.getBlockNames().push(new BlockNamesElement());
		  }
  break;
case 51:
					// line 557 "DefaultRubyParser.y"
  {
                      yyVal = new IterNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), null);
                      support.getBlockNames().pop();
		  }
  break;
case 52:
					// line 562 "DefaultRubyParser.y"
  {
                    yyVal = support.new_fcall((String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop]), getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))); /* .setPosFrom($2);*/
                }
  break;
case 53:
					// line 565 "DefaultRubyParser.y"
  {
                    yyVal = support.new_fcall((String) ((Token)yyVals[-2+yyTop]).getValue(), ((Node)yyVals[-1+yyTop]), getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))); 
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
					// line 575 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 55:
					// line 578 "DefaultRubyParser.y"
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
					// line 588 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 57:
					// line 591 "DefaultRubyParser.y"
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
					// line 601 "DefaultRubyParser.y"
  {
		    yyVal = support.new_super(((Node)yyVals[0+yyTop]), getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))); /* .setPosFrom($2);*/
		}
  break;
case 59:
					// line 604 "DefaultRubyParser.y"
  {
                    yyVal = support.new_yield(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
		}
  break;
case 61:
					// line 609 "DefaultRubyParser.y"
  {
                    yyVal = ((MultipleAsgnNode)yyVals[-1+yyTop]);
		}
  break;
case 63:
					// line 614 "DefaultRubyParser.y"
  {
	            yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(((MultipleAsgnNode)yyVals[-1+yyTop])), null);
                }
  break;
case 64:
					// line 618 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), ((ListNode)yyVals[0+yyTop]), null);
                }
  break;
case 65:
					// line 621 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((ListNode)yyVals[-1+yyTop]).add(((Node)yyVals[0+yyTop])), null);
                }
  break;
case 66:
					// line 624 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 67:
					// line 627 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((ListNode)yyVals[-1+yyTop]), new StarNode(getPosition(null)));
                }
  break;
case 68:
					// line 630 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), null, ((Node)yyVals[0+yyTop]));
                }
  break;
case 69:
					// line 633 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), null, new StarNode(getPosition(null)));
                }
  break;
case 71:
					// line 638 "DefaultRubyParser.y"
  {
                    yyVal = ((MultipleAsgnNode)yyVals[-1+yyTop]);
                }
  break;
case 72:
					// line 642 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))).add(((Node)yyVals[-1+yyTop]));
                }
  break;
case 73:
					// line 645 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
                }
  break;
case 74:
					// line 649 "DefaultRubyParser.y"
  {
                    yyVal = support.assignable(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), yyVals[0+yyTop], null);
                }
  break;
case 75:
					// line 652 "DefaultRubyParser.y"
  {
                    yyVal = support.getElementAssignmentNode(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 76:
					// line 655 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 77:
					// line 658 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 78:
					// line 661 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 79:
					// line 664 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }
			
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
		}
  break;
case 80:
					// line 671 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }

		    /* ERROR:  VEry likely a big error. */
                    yyVal = new Colon3Node(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
		    /* ruby $$ = NEW_CDECL(0, 0, NEW_COLON3($2)); */
		    }
  break;
case 81:
					// line 681 "DefaultRubyParser.y"
  {
	            support.backrefAssignError(((Node)yyVals[0+yyTop]));
                    yyVal = null;
                }
  break;
case 82:
					// line 686 "DefaultRubyParser.y"
  {
                    yyVal = support.assignable(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), yyVals[0+yyTop], null);
                }
  break;
case 83:
					// line 689 "DefaultRubyParser.y"
  {
                    yyVal = support.getElementAssignmentNode(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 84:
					// line 692 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 85:
					// line 695 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
 	        }
  break;
case 86:
					// line 698 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 87:
					// line 701 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }
			
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
	        }
  break;
case 88:
					// line 708 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }

		    /* ERROR:  VEry likely a big error. */
                    yyVal = new Colon3Node(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
		    /* ruby $$ = NEW_CDECL(0, 0, NEW_COLON3($2)); */
	        }
  break;
case 89:
					// line 717 "DefaultRubyParser.y"
  {
                    support.backrefAssignError(((Node)yyVals[0+yyTop]));
                    yyVal = null;
		}
  break;
case 90:
					// line 722 "DefaultRubyParser.y"
  {
                    yyerror("class/module name must be CONSTANT");
                }
  break;
case 92:
					// line 727 "DefaultRubyParser.y"
  {
                    yyVal = new Colon3Node(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
		}
  break;
case 93:
					// line 730 "DefaultRubyParser.y"
  {
                    /* $1 was $$ in ruby?*/
                    yyVal = new Colon2Node(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), null, (String) ((Token)yyVals[0+yyTop]).getValue());
 	        }
  break;
case 94:
					// line 734 "DefaultRubyParser.y"
  {
                    yyVal = new Colon2Node(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
		}
  break;
case 98:
					// line 741 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 99:
					// line 745 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    yyVal = yyVals[0+yyTop];
                }
  break;
case 102:
					// line 753 "DefaultRubyParser.y"
  {
                    yyVal = new UndefNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 103:
					// line 756 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
	        }
  break;
case 104:
					// line 758 "DefaultRubyParser.y"
  {
                    yyVal = support.appendToBlock(((Node)yyVals[-3+yyTop]), new UndefNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue()));
                }
  break;
case 105:
					// line 762 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("|"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 106:
					// line 763 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("^"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 107:
					// line 764 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("&"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 108:
					// line 765 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("<=>"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 109:
					// line 766 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("=="); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 110:
					// line 767 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("==="); yyVal = ((Token)yyVals[0+yyTop]);}
  break;
case 111:
					// line 768 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("=~"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 112:
					// line 769 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue(">"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 113:
					// line 770 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue(">="); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 114:
					// line 771 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("<"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 115:
					// line 772 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("<="); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 116:
					// line 773 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("<<"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 117:
					// line 774 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue(">>"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 118:
					// line 775 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("+"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 119:
					// line 776 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("-"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 120:
					// line 777 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("*"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 121:
					// line 778 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("*"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 122:
					// line 779 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("/"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 123:
					// line 780 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("%"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 124:
					// line 781 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("**"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 125:
					// line 782 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("~"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 126:
					// line 783 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("+@"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 127:
					// line 784 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("-@"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 128:
					// line 785 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("[]"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 129:
					// line 786 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("[]="); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 130:
					// line 787 "DefaultRubyParser.y"
  {  yyVal = ((Node)yyVals[0+yyTop]); }
  break;
case 172:
					// line 798 "DefaultRubyParser.y"
  {
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 173:
					// line 801 "DefaultRubyParser.y"
  {
                    yyVal = support.node_assign(((Node)yyVals[-4+yyTop]), new RescueNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), new RescueBodyNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), null,((Node)yyVals[0+yyTop]), null), null));
		}
  break;
case 174:
					// line 804 "DefaultRubyParser.y"
  {
		    support.checkExpression(((Node)yyVals[0+yyTop]));
		    if (yyVals[-2+yyTop] != null) {
		        String name = ((INameNode)yyVals[-2+yyTop]).getName();
			String asgnOp = (String) ((Token)yyVals[-1+yyTop]).getValue();

		        if (asgnOp.equals("||")) {
	                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
	                    yyVal = new OpAsgnOrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.gettable(name, getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))), ((Node)yyVals[-2+yyTop]));
			    /* FIXME
			    if (is_asgn_or_id(vid)) {
				$$->nd_aid = vid;
			    }
			    */
			} else if (asgnOp.equals("&&")) {
	                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                            yyVal = new OpAsgnAndNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.gettable(name, getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))), ((Node)yyVals[-2+yyTop]));
			} else {
			    yyVal = yyVals[-2+yyTop];
                            if (yyVal != null) {
			      ((AssignableNode)yyVal).setValueNode(support.getOperatorCallNode(support.gettable(name, getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))), asgnOp, ((Node)yyVals[0+yyTop])));
                            }
			}
		    } else {
 		        yyVal = null; /* XXX 0; */
		    }
                }
  break;
case 175:
					// line 831 "DefaultRubyParser.y"
  {
                    yyVal = new OpElementAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 176:
					// line 834 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 177:
					// line 837 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 178:
					// line 840 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 179:
					// line 843 "DefaultRubyParser.y"
  {
		    yyerror("constant re-assignment");
		    yyVal = null;
	        }
  break;
case 180:
					// line 847 "DefaultRubyParser.y"
  {
		    yyerror("constant re-assignment");
		    yyVal = null;
	        }
  break;
case 181:
					// line 851 "DefaultRubyParser.y"
  {
                    support.backrefAssignError(((Node)yyVals[-2+yyTop]));
                    yyVal = null;
                }
  break;
case 182:
					// line 855 "DefaultRubyParser.y"
  {
		    support.checkExpression(((Node)yyVals[-2+yyTop]));
		    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = new DotNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), false);
                }
  break;
case 183:
					// line 860 "DefaultRubyParser.y"
  {
		    support.checkExpression(((Node)yyVals[-2+yyTop]));
		    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = new DotNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), true);
                }
  break;
case 184:
					// line 865 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "+", ((Node)yyVals[0+yyTop]));
                }
  break;
case 185:
					// line 868 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "-", ((Node)yyVals[0+yyTop]));
                }
  break;
case 186:
					// line 871 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "*", ((Node)yyVals[0+yyTop]));
                }
  break;
case 187:
					// line 874 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "/", ((Node)yyVals[0+yyTop]));
                }
  break;
case 188:
					// line 877 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "%", ((Node)yyVals[0+yyTop]));
                }
  break;
case 189:
					// line 880 "DefaultRubyParser.y"
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
					// line 899 "DefaultRubyParser.y"
  {
                    Object number = ((Token)yyVals[-2+yyTop]).getValue();

                    yyVal = support.getOperatorCallNode(support.getOperatorCallNode((number instanceof Long ? (Node) new FixnumNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Long) number).longValue()) : (Node)new BignumNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((BigInteger) number))), "**", ((Node)yyVals[0+yyTop])), "-@");
                }
  break;
case 191:
					// line 904 "DefaultRubyParser.y"
  {
  /* ENEBO: Seems like this should be $2*/
                    yyVal = support.getOperatorCallNode(support.getOperatorCallNode(new FloatNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Double) ((Token)yyVals[-3+yyTop]).getValue()).doubleValue()), "**", ((Node)yyVals[0+yyTop])), "-@");
                }
  break;
case 192:
					// line 908 "DefaultRubyParser.y"
  {
 	            if (((Node)yyVals[0+yyTop]) != null && ((Node)yyVals[0+yyTop]) instanceof ILiteralNode) {
		        yyVal = ((Node)yyVals[0+yyTop]);
		    } else {
                        yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "+@");
		    }
                }
  break;
case 193:
					// line 915 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "-@");
		}
  break;
case 194:
					// line 918 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "|", ((Node)yyVals[0+yyTop]));
                }
  break;
case 195:
					// line 921 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "^", ((Node)yyVals[0+yyTop]));
                }
  break;
case 196:
					// line 924 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "&", ((Node)yyVals[0+yyTop]));
                }
  break;
case 197:
					// line 927 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=>", ((Node)yyVals[0+yyTop]));
                }
  break;
case 198:
					// line 930 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">", ((Node)yyVals[0+yyTop]));
                }
  break;
case 199:
					// line 933 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">=", ((Node)yyVals[0+yyTop]));
                }
  break;
case 200:
					// line 936 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<", ((Node)yyVals[0+yyTop]));
                }
  break;
case 201:
					// line 939 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=", ((Node)yyVals[0+yyTop]));
                }
  break;
case 202:
					// line 942 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop]));
                }
  break;
case 203:
					// line 945 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "===", ((Node)yyVals[0+yyTop]));
                }
  break;
case 204:
					// line 948 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop])));
                }
  break;
case 205:
					// line 951 "DefaultRubyParser.y"
  {
                    yyVal = support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 206:
					// line 954 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
                }
  break;
case 207:
					// line 957 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
                }
  break;
case 208:
					// line 960 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "~");
                }
  break;
case 209:
					// line 963 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<<", ((Node)yyVals[0+yyTop]));
                }
  break;
case 210:
					// line 966 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">>", ((Node)yyVals[0+yyTop]));
                }
  break;
case 211:
					// line 969 "DefaultRubyParser.y"
  {
                    yyVal = support.newAndNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 212:
					// line 972 "DefaultRubyParser.y"
  {
                    yyVal = support.newOrNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 213:
					// line 975 "DefaultRubyParser.y"
  {
	            support.setInDefined(true);
		}
  break;
case 214:
					// line 977 "DefaultRubyParser.y"
  {
                    support.setInDefined(false);
                    yyVal = new DefinedNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Node)yyVals[0+yyTop]));
                }
  break;
case 215:
					// line 981 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 216:
					// line 984 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 217:
					// line 988 "DefaultRubyParser.y"
  {
		    support.checkExpression(((Node)yyVals[0+yyTop]));
	            yyVal = ((Node)yyVals[0+yyTop]);   
		}
  break;
case 219:
					// line 994 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), "parenthesize argument(s) for future version");
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))).add(((Node)yyVals[-1+yyTop]));
                }
  break;
case 220:
					// line 998 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                }
  break;
case 221:
					// line 1001 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[-1+yyTop]));
                    yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 222:
					// line 1005 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                }
  break;
case 223:
					// line 1008 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[-1+yyTop]));
		    yyVal = new NewlineNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), new SplatNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])));
                }
  break;
case 224:
					// line 1013 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                }
  break;
case 225:
					// line 1016 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-2+yyTop]);
                }
  break;
case 226:
					// line 1019 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), "parenthesize argument(s) for future version");
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop]))).add(((Node)yyVals[-2+yyTop]));
                }
  break;
case 227:
					// line 1023 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), "parenthesize argument(s) for future version");
                    yyVal = ((ListNode)yyVals[-4+yyTop]).add(((Node)yyVals[-2+yyTop]));
                }
  break;
case 230:
					// line 1031 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), "parenthesize argument(s) for future version");
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 231:
					// line 1035 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_blk_pass(((ListNode)yyVals[-1+yyTop]), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 232:
					// line 1038 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 233:
					// line 1042 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 234:
					// line 1046 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop]))).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 235:
					// line 1050 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-3+yyTop]).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 236:
					// line 1054 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[-1+yyTop]));
		    yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop])), ((ListNode)yyVals[-6+yyTop]).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 237:
					// line 1059 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_blk_pass(new SplatNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 238:
					// line 1062 "DefaultRubyParser.y"
  {
	        }
  break;
case 239:
					// line 1065 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_blk_pass(support.list_concat(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop]))).add(((Node)yyVals[-3+yyTop])), ((ListNode)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 240:
					// line 1068 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_blk_pass(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(((Node)yyVals[-2+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
                  }
  break;
case 241:
					// line 1071 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop]))).add(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 242:
					// line 1075 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop])), support.list_concat(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop]))).add(((Node)yyVals[-6+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 243:
					// line 1079 "DefaultRubyParser.y"
  {
                      yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 244:
					// line 1083 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop]))).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 245:
					// line 1087 "DefaultRubyParser.y"
  {
                      yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop]))).add(((Node)yyVals[-3+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 246:
					// line 1091 "DefaultRubyParser.y"
  {
                      yyVal = support.list_concat(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop]))).add(((Node)yyVals[-5+yyTop])), ((ListNode)yyVals[-3+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 247:
					// line 1095 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop])), new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop]))).add(((Node)yyVals[-6+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 248:
					// line 1099 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-8+yyTop])), support.list_concat(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-8+yyTop]))).add(((Node)yyVals[-8+yyTop])), ((ListNode)yyVals[-6+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 249:
					// line 1103 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_blk_pass(new SplatNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 250:
					// line 1106 "DefaultRubyParser.y"
  {}
  break;
case 251:
					// line 1108 "DefaultRubyParser.y"
  { 
		    yyVal = new Long(lexer.getCmdArgumentState().begin());
		}
  break;
case 252:
					// line 1110 "DefaultRubyParser.y"
  {
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 254:
					// line 1116 "DefaultRubyParser.y"
  {                    
		    lexer.setState(LexState.EXPR_ENDARG);
		  }
  break;
case 255:
					// line 1118 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), "don't put space before argument parentheses");
		    yyVal = null;
		  }
  break;
case 256:
					// line 1122 "DefaultRubyParser.y"
  {
		    lexer.setState(LexState.EXPR_ENDARG);
		  }
  break;
case 257:
					// line 1124 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), "don't put space before argument parentheses");
		    yyVal = ((Node)yyVals[-2+yyTop]);
		  }
  break;
case 258:
					// line 1129 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = new BlockPassNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
                }
  break;
case 259:
					// line 1134 "DefaultRubyParser.y"
  {
                    yyVal = ((BlockPassNode)yyVals[0+yyTop]);
                }
  break;
case 261:
					// line 1139 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 262:
					// line 1142 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 263:
					// line 1146 "DefaultRubyParser.y"
  {
		    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 264:
					// line 1149 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
		}
  break;
case 265:
					// line 1152 "DefaultRubyParser.y"
  {  
                    yyVal = new SplatNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
		}
  break;
case 274:
					// line 1164 "DefaultRubyParser.y"
  {
                    yyVal = new VCallNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
		}
  break;
case 275:
					// line 1168 "DefaultRubyParser.y"
  {
                    yyVal = new BeginNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop]));
		}
  break;
case 276:
					// line 1171 "DefaultRubyParser.y"
  {
		    lexer.setState(LexState.EXPR_ENDARG);
		    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), "(...) interpreted as grouped expression");
                    yyVal = ((Node)yyVals[-2+yyTop]);
		}
  break;
case 277:
					// line 1176 "DefaultRubyParser.y"
  {
	            yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 278:
					// line 1179 "DefaultRubyParser.y"
  {
                    yyVal = new Colon2Node(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 279:
					// line 1182 "DefaultRubyParser.y"
  {
                    yyVal = new Colon3Node(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 280:
					// line 1185 "DefaultRubyParser.y"
  {
                    yyVal = new CallNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop]), "[]", ((Node)yyVals[-1+yyTop]));
                }
  break;
case 281:
					// line 1188 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-1+yyTop]) == null) {
                        yyVal = new ZArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))); /* zero length array*/
                    } else {
                        yyVal = ((Node)yyVals[-1+yyTop]);
                    }
                }
  break;
case 282:
					// line 1195 "DefaultRubyParser.y"
  {
                    yyVal = new HashNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((ListNode)yyVals[-1+yyTop]));
                }
  break;
case 283:
					// line 1198 "DefaultRubyParser.y"
  {
		    yyVal = new ReturnNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), null);
                }
  break;
case 284:
					// line 1201 "DefaultRubyParser.y"
  {
                    yyVal = support.new_yield(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 285:
					// line 1204 "DefaultRubyParser.y"
  {
                    yyVal = new YieldNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), null, false);
                }
  break;
case 286:
					// line 1207 "DefaultRubyParser.y"
  {
                    yyVal = new YieldNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), null, false);
                }
  break;
case 287:
					// line 1210 "DefaultRubyParser.y"
  {
	            support.setInDefined(true);
		}
  break;
case 288:
					// line 1212 "DefaultRubyParser.y"
  {
                    support.setInDefined(false);
                    yyVal = new DefinedNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 289:
					// line 1216 "DefaultRubyParser.y"
  {
                    ((IterNode)yyVals[0+yyTop]).setIterNode(new FCallNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), null));
                    yyVal = ((IterNode)yyVals[0+yyTop]);
                }
  break;
case 291:
					// line 1221 "DefaultRubyParser.y"
  {
		    if (((Node)yyVals[-1+yyTop]) != null && ((Node)yyVals[-1+yyTop]) instanceof BlockPassNode) {
                        throw new SyntaxException(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), "Both block arg and actual block given.");
		    }
                    ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVals[-1+yyTop]));
                    yyVal = ((IterNode)yyVals[0+yyTop]);
                }
  break;
case 292:
					// line 1228 "DefaultRubyParser.y"
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
					// line 1237 "DefaultRubyParser.y"
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
					// line 1246 "DefaultRubyParser.y"
  { 
	            lexer.getConditionState().begin();
		}
  break;
case 295:
					// line 1248 "DefaultRubyParser.y"
  {
		    lexer.getConditionState().end();
		}
  break;
case 296:
					// line 1250 "DefaultRubyParser.y"
  {
                    yyVal = new WhileNode(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
			    nd_set_type($$, NODE_UNTIL);
			    } */
                }
  break;
case 297:
					// line 1257 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().begin();
                }
  break;
case 298:
					// line 1259 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().end();
                }
  break;
case 299:
					// line 1261 "DefaultRubyParser.y"
  {
                    yyVal = new UntilNode(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
			    nd_set_type($$, NODE_WHILE);
			    } */
                }
  break;
case 300:
					// line 1270 "DefaultRubyParser.y"
  {
		    yyVal = new CaseNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop])); /* XXX*/
                }
  break;
case 301:
					// line 1273 "DefaultRubyParser.y"
  {
                    yyVal = new CaseNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), null, ((Node)yyVals[-1+yyTop]));
                }
  break;
case 302:
					// line 1276 "DefaultRubyParser.y"
  {
		    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 303:
					// line 1279 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().begin();
                }
  break;
case 304:
					// line 1281 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().end();
                }
  break;
case 305:
					// line 1284 "DefaultRubyParser.y"
  {
                    yyVal = new ForNode(getPosition(((ISourcePositionHolder)yyVals[-8+yyTop])), ((Node)yyVals[-7+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-4+yyTop]));
                }
  break;
case 306:
					// line 1287 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("class definition in method body");
                    }
                    support.setClassNest(support.getClassNest() + 1);
                    support.getLocalNames().push(new LocalNamesElement());
                    /* $$ = new Integer(ruby.getSourceLine());*/
                }
  break;
case 307:
					// line 1295 "DefaultRubyParser.y"
  {
  yyVal = new ClassNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), ((Colon2Node)yyVals[-4+yyTop]), new ScopeNode(getPosition(null), ((LocalNamesElement) support.getLocalNames().peek()).getNames(), ((Node)yyVals[-1+yyTop])), ((Node)yyVals[-3+yyTop]));
                    /* $<Node>$.setLine($<Integer>4.intValue());*/
                    support.getLocalNames().pop();
                    support.setClassNest(support.getClassNest() - 1);
                }
  break;
case 308:
					// line 1301 "DefaultRubyParser.y"
  {
                    yyVal = new Boolean(support.isInDef());
                    support.setInDef(false);
                }
  break;
case 309:
					// line 1304 "DefaultRubyParser.y"
  {
                    yyVal = new Integer(support.getInSingle());
                    support.setInSingle(0);
                    support.setClassNest(support.getClassNest() + 1);
                    support.getLocalNames().push(new LocalNamesElement());
                }
  break;
case 310:
					// line 1310 "DefaultRubyParser.y"
  {
                    yyVal = new SClassNode(getPosition(((ISourcePositionHolder)yyVals[-7+yyTop])), ((Node)yyVals[-5+yyTop]), new ScopeNode(getPosition(null), ((LocalNamesElement) support.getLocalNames().peek()).getNames(), ((Node)yyVals[-1+yyTop])));
                    support.getLocalNames().pop();
                    support.setClassNest(support.getClassNest() - 1);
                    support.setInDef(((Boolean)yyVals[-4+yyTop]).booleanValue());
                    support.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
                }
  break;
case 311:
					// line 1317 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) { 
                        yyerror("module definition in method body");
                    }
                    support.setClassNest(support.getClassNest() + 1);
                    support.getLocalNames().push(new LocalNamesElement());
                    /* $$ = new Integer(ruby.getSourceLine());*/
                }
  break;
case 312:
					// line 1325 "DefaultRubyParser.y"
  {
  yyVal = new ModuleNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Colon2Node)yyVals[-3+yyTop]), new ScopeNode(getPosition(null), ((LocalNamesElement) support.getLocalNames().peek()).getNames(), ((Node)yyVals[-1+yyTop])));
                    /* $<Node>$.setLine($<Integer>3.intValue());*/
                    support.getLocalNames().pop();
                    support.setClassNest(support.getClassNest() - 1);
                }
  break;
case 313:
					// line 1331 "DefaultRubyParser.y"
  {
		      /* missing
			$<id>$ = cur_mid;
			cur_mid = $2; */
                    support.setInDef(true);
                    support.getLocalNames().push(new LocalNamesElement());
                }
  break;
case 314:
					// line 1339 "DefaultRubyParser.y"
  {
		      /* was in old jruby grammar support.getClassNest() !=0 || IdUtil.isAttrSet($2) ? Visibility.PUBLIC : Visibility.PRIVATE); */
                    /* NOEX_PRIVATE for toplevel */
                    yyVal = new DefnNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), (String) ((Token)yyVals[-4+yyTop]).getValue(), ((Node)yyVals[-2+yyTop]),
		                      new ScopeNode(getPosition(null), ((LocalNamesElement) support.getLocalNames().peek()).getNames(), ((Node)yyVals[-1+yyTop])), Visibility.PRIVATE);
                    /* $<Node>$.setPosFrom($4);*/
                    support.getLocalNames().pop();
                    support.setInDef(false);
		    /* missing cur_mid = $<id>3; */
                }
  break;
case 315:
					// line 1349 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
                }
  break;
case 316:
					// line 1351 "DefaultRubyParser.y"
  {
                    support.setInSingle(support.getInSingle() + 1);
                    support.getLocalNames().push(new LocalNamesElement());
                    lexer.setState(LexState.EXPR_END); /* force for args */
                }
  break;
case 317:
					// line 1357 "DefaultRubyParser.y"
  {
                    yyVal = new DefsNode(getPosition(((ISourcePositionHolder)yyVals[-8+yyTop])), ((Node)yyVals[-7+yyTop]), (String) ((Token)yyVals[-4+yyTop]).getValue(), ((Node)yyVals[-2+yyTop]), new ScopeNode(getPosition(null), ((LocalNamesElement) support.getLocalNames().peek()).getNames(), ((Node)yyVals[-1+yyTop])));
                    /* $<Node>$.setPosFrom($2);*/
                    support.getLocalNames().pop();
                    support.setInSingle(support.getInSingle() - 1);
                }
  break;
case 318:
					// line 1363 "DefaultRubyParser.y"
  {
                    yyVal = new BreakNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                }
  break;
case 319:
					// line 1366 "DefaultRubyParser.y"
  {
                    yyVal = new NextNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                }
  break;
case 320:
					// line 1369 "DefaultRubyParser.y"
  {
                    yyVal = new RedoNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                }
  break;
case 321:
					// line 1372 "DefaultRubyParser.y"
  {
                    yyVal = new RetryNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                }
  break;
case 322:
					// line 1376 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
		    yyVal = ((Node)yyVals[0+yyTop]);
		}
  break;
case 331:
					// line 1393 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), support.getConditionNode(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 333:
					// line 1398 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 335:
					// line 1403 "DefaultRubyParser.y"
  {}
  break;
case 337:
					// line 1406 "DefaultRubyParser.y"
  {
                    yyVal = new ZeroArgNode(getPosition(null));
                }
  break;
case 338:
					// line 1409 "DefaultRubyParser.y"
  {
                    yyVal = new ZeroArgNode(getPosition(null));
		}
  break;
case 339:
					// line 1412 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 340:
					// line 1416 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push(new BlockNamesElement());
		}
  break;
case 341:
					// line 1419 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), null);
                    support.getBlockNames().pop();
                }
  break;
case 342:
					// line 1424 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-1+yyTop]) instanceof BlockPassNode) {
                        throw new SyntaxException(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), "Both block arg and actual block given.");
                    }
                    ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVals[-1+yyTop]));
                    yyVal = ((IterNode)yyVals[0+yyTop]);
                }
  break;
case 343:
					// line 1431 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 344:
					// line 1434 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 345:
					// line 1438 "DefaultRubyParser.y"
  {
                    yyVal = support.new_fcall((String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop]), getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))); /* .setPosFrom($2);*/
                }
  break;
case 346:
					// line 1441 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 347:
					// line 1444 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 348:
					// line 1447 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue(), null);
                }
  break;
case 349:
					// line 1450 "DefaultRubyParser.y"
  {
                    yyVal = support.new_super(((Node)yyVals[0+yyTop]), getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])));
                }
  break;
case 350:
					// line 1453 "DefaultRubyParser.y"
  {
                    yyVal = new ZSuperNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                }
  break;
case 351:
					// line 1457 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push(new BlockNamesElement());
		}
  break;
case 352:
					// line 1459 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), null);
                    support.getBlockNames().pop();
                }
  break;
case 353:
					// line 1463 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push(new BlockNamesElement());
		}
  break;
case 354:
					// line 1465 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), null);
                    support.getBlockNames().pop();
                }
  break;
case 355:
					// line 1472 "DefaultRubyParser.y"
  {
		    yyVal = new WhenNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 357:
					// line 1477 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-3+yyTop]).add(new WhenNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Node)yyVals[0+yyTop]), null, null));
                }
  break;
case 358:
					// line 1480 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))).add(new WhenNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]), null, null));
                }
  break;
case 361:
					// line 1490 "DefaultRubyParser.y"
  {
                    Node node;
		    if (((Node)yyVals[-3+yyTop]) != null) {
                       node = support.appendToBlock(support.node_assign(((Node)yyVals[-3+yyTop]), new GlobalVarNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), "$!")), ((Node)yyVals[-1+yyTop]));
		    } else {
		       node = ((Node)yyVals[-1+yyTop]);
                    }
                    yyVal = new RescueBodyNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), ((Node)yyVals[-4+yyTop]), node, ((RescueBodyNode)yyVals[0+yyTop]));
		}
  break;
case 362:
					// line 1499 "DefaultRubyParser.y"
  {yyVal = null;}
  break;
case 363:
					// line 1501 "DefaultRubyParser.y"
  {
	            yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
		}
  break;
case 366:
					// line 1507 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 368:
					// line 1512 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[0+yyTop]) != null) {
                        yyVal = ((Node)yyVals[0+yyTop]);
                    } else {
                        yyVal = new NilNode(getPosition(null));
                    }
                }
  break;
case 371:
					// line 1522 "DefaultRubyParser.y"
  {
                    yyVal = new SymbolNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 373:
					// line 1527 "DefaultRubyParser.y"
  {
		    if (((Node)yyVals[0+yyTop]) == null) {
		        yyVal = new StrNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), "");
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
					// line 1539 "DefaultRubyParser.y"
  {
                    yyVal = support.literal_concat(getPosition(null), null, ((Node)yyVals[0+yyTop]));
		}
  break;
case 375:
					// line 1542 "DefaultRubyParser.y"
  {
                    yyVal = support.literal_concat(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
		}
  break;
case 376:
					// line 1546 "DefaultRubyParser.y"
  {
		     yyVal = ((Node)yyVals[-1+yyTop]);
		}
  break;
case 377:
					// line 1550 "DefaultRubyParser.y"
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
					// line 1564 "DefaultRubyParser.y"
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
					// line 1581 "DefaultRubyParser.y"
  {
		     yyVal = new ZArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])));
		 }
  break;
case 380:
					// line 1584 "DefaultRubyParser.y"
  {
		     yyVal = ((ListNode)yyVals[-1+yyTop]);
		 }
  break;
case 381:
					// line 1588 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 382:
					// line 1591 "DefaultRubyParser.y"
  {
                     Node node = ((Node)yyVals[-1+yyTop]);

                     if (node instanceof EvStrNode) {
		       node = new DStrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(node);
		     }

		     yyVal = ((ListNode)yyVals[-2+yyTop]).add(node);
		 }
  break;
case 384:
					// line 1602 "DefaultRubyParser.y"
  {
                     yyVal = support.literal_concat(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
	         }
  break;
case 385:
					// line 1606 "DefaultRubyParser.y"
  {
		     yyVal = new ZArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])));
		 }
  break;
case 386:
					// line 1609 "DefaultRubyParser.y"
  {
		     yyVal = ((ListNode)yyVals[-1+yyTop]);
		 }
  break;
case 387:
					// line 1613 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 388:
					// line 1616 "DefaultRubyParser.y"
  {
                     if (((ListNode)yyVals[-2+yyTop]) == null) {
		         yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(new StrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue()));
		     } else {
		         yyVal = ((ListNode)yyVals[-2+yyTop]).add(new StrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue()));
		     }
		 }
  break;
case 389:
					// line 1624 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 390:
					// line 1627 "DefaultRubyParser.y"
  {
                     yyVal = support.literal_concat(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
		 }
  break;
case 391:
					// line 1631 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 392:
					// line 1634 "DefaultRubyParser.y"
  {
                     yyVal = support.literal_concat(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
		 }
  break;
case 393:
					// line 1639 "DefaultRubyParser.y"
  {
                      yyVal = new StrNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                  }
  break;
case 394:
					// line 1642 "DefaultRubyParser.y"
  {
                      yyVal = lexer.getStrTerm();
		      lexer.setStrTerm(null);
		      lexer.setState(LexState.EXPR_BEG);
		  }
  break;
case 395:
					// line 1646 "DefaultRubyParser.y"
  {
		      lexer.setStrTerm(((StrTerm)yyVals[-1+yyTop]));
		      yyVal = new EvStrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop]));
		  }
  break;
case 396:
					// line 1650 "DefaultRubyParser.y"
  {
		      yyVal = lexer.getStrTerm();
		      lexer.setStrTerm(null);
		      lexer.setState(LexState.EXPR_BEG);
		  }
  break;
case 397:
					// line 1654 "DefaultRubyParser.y"
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
					// line 1665 "DefaultRubyParser.y"
  {
                      yyVal = new GlobalVarNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                 }
  break;
case 399:
					// line 1668 "DefaultRubyParser.y"
  {
                      yyVal = new InstVarNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                 }
  break;
case 400:
					// line 1671 "DefaultRubyParser.y"
  {
                      yyVal = new ClassVarNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                 }
  break;
case 402:
					// line 1677 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 407:
					// line 1687 "DefaultRubyParser.y"
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
					// line 1697 "DefaultRubyParser.y"
  {
                    Object number = ((Token)yyVals[0+yyTop]).getValue();

                    if (number instanceof Long) {
		        yyVal = new FixnumNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), ((Long) number).longValue());
                    } else {
		        yyVal = new BignumNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), (BigInteger) number);
                    }
                }
  break;
case 409:
					// line 1706 "DefaultRubyParser.y"
  {
                    yyVal = new FloatNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), ((Double) ((Token)yyVals[0+yyTop]).getValue()).doubleValue());
	        }
  break;
case 410:
					// line 1709 "DefaultRubyParser.y"
  {
                    Object number = ((Token)yyVals[0+yyTop]).getValue();

                    yyVal = support.getOperatorCallNode((number instanceof Long ? (Node) new FixnumNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Long) number).longValue()) : (Node) new BignumNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), (BigInteger) number)), "-@");
		}
  break;
case 411:
					// line 1714 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(new FloatNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Double) ((Token)yyVals[0+yyTop]).getValue()).doubleValue()), "-@");
		}
  break;
case 412:
					// line 1723 "DefaultRubyParser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 413:
					// line 1726 "DefaultRubyParser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 414:
					// line 1729 "DefaultRubyParser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 415:
					// line 1732 "DefaultRubyParser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 416:
					// line 1735 "DefaultRubyParser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 417:
					// line 1738 "DefaultRubyParser.y"
  { 
                    yyVal = new NilNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                }
  break;
case 418:
					// line 1741 "DefaultRubyParser.y"
  {
                    yyVal = new SelfNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                }
  break;
case 419:
					// line 1744 "DefaultRubyParser.y"
  { 
                    yyVal = new TrueNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                }
  break;
case 420:
					// line 1747 "DefaultRubyParser.y"
  {
                    yyVal = new FalseNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                }
  break;
case 421:
					// line 1750 "DefaultRubyParser.y"
  {
                    yyVal = new StrNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), getPosition(((ISourcePositionHolder)yyVals[0+yyTop])).getFile());
                }
  break;
case 422:
					// line 1753 "DefaultRubyParser.y"
  {
                    yyVal = new FixnumNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), getPosition(((ISourcePositionHolder)yyVals[0+yyTop])).getEndLine() + 1);
                }
  break;
case 423:
					// line 1757 "DefaultRubyParser.y"
  {
                    /* Work around __LINE__ and __FILE__ */
                    if (yyVals[0+yyTop] instanceof INameNode) {
		        String name = ((INameNode)yyVals[0+yyTop]).getName();
                        yyVal = support.gettable(name, getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
		    } else if (yyVals[0+yyTop] instanceof Token) {
                        yyVal = support.gettable((String) ((Token)yyVals[0+yyTop]).getValue(), getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
		    } else {
		        yyVal = yyVals[0+yyTop];
		    }
                }
  break;
case 424:
					// line 1770 "DefaultRubyParser.y"
  {
                    yyVal = support.assignable(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), yyVals[0+yyTop], null);
                }
  break;
case 427:
					// line 1777 "DefaultRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 428:
					// line 1780 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 429:
					// line 1782 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 430:
					// line 1785 "DefaultRubyParser.y"
  {
                    yyerrok();
                    yyVal = null;
                }
  break;
case 431:
					// line 1790 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-2+yyTop]);
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 432:
					// line 1794 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 433:
					// line 1798 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), ((Integer) ((Token)yyVals[-5+yyTop]).getValue()).intValue(), ((ListNode)yyVals[-3+yyTop]), ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 434:
					// line 1801 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Integer) ((Token)yyVals[-3+yyTop]).getValue()).intValue(), ((ListNode)yyVals[-1+yyTop]), -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 435:
					// line 1804 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Integer) ((Token)yyVals[-3+yyTop]).getValue()).intValue(), null, ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 436:
					// line 1807 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), null, -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 437:
					// line 1810 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), 0, ((ListNode)yyVals[-3+yyTop]), ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 438:
					// line 1813 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), 0, ((ListNode)yyVals[-1+yyTop]), -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 439:
					// line 1816 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), 0, null, ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 440:
					// line 1819 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), 0, null, -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 441:
					// line 1822 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(getPosition(null), 0, null, -1, null);
                }
  break;
case 442:
					// line 1826 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be a constant");
                }
  break;
case 443:
					// line 1829 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be an instance variable");
                }
  break;
case 444:
					// line 1832 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be a class variable");
                }
  break;
case 445:
					// line 1835 "DefaultRubyParser.y"
  {
                   String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();
                   if (!IdUtil.isLocal(identifier)) {
                        yyerror("formal argument must be local variable");
                    } else if (((LocalNamesElement) support.getLocalNames().peek()).isLocalRegistered(identifier)) {
                        yyerror("duplicate argument name");
                    }
                    ((LocalNamesElement) support.getLocalNames().peek()).getLocalIndex(identifier);
		    ((Token)yyVals[0+yyTop]).setValue(new Integer(1));
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 447:
					// line 1848 "DefaultRubyParser.y"
  {
		    ((Token)yyVals[-2+yyTop]).setValue(new Integer(((Integer) ((Token)yyVals[-2+yyTop]).getValue()).intValue() + 1));
		    yyVal = ((Token)yyVals[-2+yyTop]);
                }
  break;
case 448:
					// line 1853 "DefaultRubyParser.y"
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
					// line 1865 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 450:
					// line 1868 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 453:
					// line 1875 "DefaultRubyParser.y"
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
					// line 1886 "DefaultRubyParser.y"
  {
                    ((Token)yyVals[0+yyTop]).setValue(new Integer(-2));
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 457:
					// line 1894 "DefaultRubyParser.y"
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
					// line 1905 "DefaultRubyParser.y"
  {
                    yyVal = ((BlockArgNode)yyVals[0+yyTop]);
                }
  break;
case 459:
					// line 1908 "DefaultRubyParser.y"
  {
	            yyVal = null;
	        }
  break;
case 460:
					// line 1912 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[0+yyTop]) instanceof SelfNode) {
                        yyVal = new SelfNode(getPosition(null));
                    } else {
			support.checkExpression(((Node)yyVals[0+yyTop]));
			yyVal = ((Node)yyVals[0+yyTop]);
		    }
                }
  break;
case 461:
					// line 1920 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 462:
					// line 1922 "DefaultRubyParser.y"
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
					// line 1939 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                }
  break;
case 465:
					// line 1942 "DefaultRubyParser.y"
  {
                    if (((ListNode)yyVals[-1+yyTop]).size() % 2 != 0) {
                        yyerror("Odd number list for Hash.");
                    }
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                }
  break;
case 467:
					// line 1950 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).addAll(((ListNode)yyVals[0+yyTop]));
                }
  break;
case 468:
					// line 1954 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(((Node)yyVals[-2+yyTop])).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 488:
					// line 1984 "DefaultRubyParser.y"
  {
                    yyerrok();
                }
  break;
case 491:
					// line 1990 "DefaultRubyParser.y"
  {
                    yyerrok();
                }
  break;
case 492:
					// line 1994 "DefaultRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 493:
					// line 1998 "DefaultRubyParser.y"
  {  yyVal = null;
		  }
  break;
case 494:
					// line 2001 "DefaultRubyParser.y"
  {  yyVal = null;
		  }
  break;
					// line 7890 "-"
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

					// line 2005 "DefaultRubyParser.y"

    /** The parse method use an lexer stream and parse it to an AST node 
     * structure
     */
    public RubyParserResult parse(LexerSource source) {
        support.reset();
        support.setResult(new RubyParserResult());
        
        lexer.reset();
        lexer.setSource(source);
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

    private ISourcePosition getPosition(ISourcePositionHolder start) {
        if (start != null) {
            return lexer.getPosition(start.getPosition());
	} 
	
	return lexer.getPosition(null);
    }
}
					// line 7961 "-"
