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
					// line 164 "-"
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
//yyLhs 496
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
    23,    23,    23,    23,    23,    23,   114,    23,    23,    23,
    23,    23,    23,    23,    23,    23,    23,    23,   115,    23,
    23,    23,    23,    23,    23,   117,   119,    23,   120,   121,
    23,    23,    23,    23,   122,   123,    23,   124,    23,   126,
   127,    23,   128,    23,   129,    23,   130,   131,    23,    23,
    23,    23,    23,    30,   116,   116,   116,   116,   118,   118,
   118,    33,    33,    31,    31,    57,    57,    58,    58,    58,
    58,   132,    62,    47,    47,    47,    26,    26,    26,    26,
    26,    26,   133,    61,   134,    61,    68,    73,    73,    73,
    32,    32,    79,    79,    77,    77,    77,    34,    34,    35,
    35,    13,    13,    13,     2,     3,     3,     4,     5,     6,
    10,    10,    28,    28,    12,    12,    11,    11,    27,    27,
     7,     7,     8,     8,     9,   135,     9,   136,     9,    55,
    55,    55,    55,    87,    86,    86,    86,    86,    15,    14,
    14,    14,    14,    80,    80,    80,    80,    80,    80,    80,
    80,    80,    80,    80,    42,    81,    56,    56,    46,   137,
    46,    46,    51,    51,    52,    52,    52,    52,    52,    52,
    52,    52,    52,    94,    94,    94,    94,    96,    96,    53,
    84,    84,    98,    98,    95,    95,    99,    99,    50,    49,
    49,     1,   138,     1,    83,    83,    83,    75,    75,    76,
    88,    88,    88,    89,    89,    89,    89,    90,    90,    90,
    97,    97,   101,   101,   108,   108,   110,   110,   110,   125,
   125,   102,   102,    60,    82,    45,
    }, yyLen = {
//yyLen 496
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
     1,     1,     1,     1,     1,     3,     0,     5,     3,     3,
     2,     4,     3,     3,     1,     4,     3,     1,     0,     6,
     2,     1,     2,     6,     6,     0,     0,     7,     0,     0,
     7,     5,     4,     5,     0,     0,     9,     0,     6,     0,
     0,     8,     0,     5,     0,     6,     0,     0,     9,     1,
     1,     1,     1,     1,     1,     1,     1,     2,     1,     1,
     1,     1,     5,     1,     2,     1,     1,     1,     2,     1,
     3,     0,     5,     2,     4,     4,     2,     4,     4,     3,
     2,     1,     0,     5,     0,     5,     5,     1,     4,     2,
     1,     1,     6,     0,     1,     1,     1,     2,     1,     2,
     1,     1,     1,     1,     1,     1,     2,     3,     3,     3,
     3,     3,     0,     3,     1,     2,     3,     3,     0,     3,
     0,     2,     0,     2,     1,     0,     3,     0,     4,     1,
     1,     1,     1,     2,     1,     1,     1,     1,     3,     1,
     1,     2,     2,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     0,
     4,     2,     4,     2,     6,     4,     4,     2,     4,     2,
     2,     1,     0,     1,     1,     1,     1,     1,     3,     3,
     1,     3,     1,     1,     2,     1,     1,     1,     2,     2,
     0,     1,     0,     5,     1,     2,     2,     1,     3,     3,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     0,     1,     0,     1,     0,     1,     1,     1,
     1,     1,     2,     0,     0,     0,
    }, yyDefRed = {
//yyDefRed 886
     1,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   295,   298,     0,     0,     0,   321,   322,     0,
     0,     0,   419,   418,   420,   421,     0,     0,     0,    20,
     0,   423,   422,     0,     0,   415,   414,     0,   417,   409,
   410,   426,   427,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   390,   392,   392,     0,     0,
   267,     0,   375,   268,   269,   270,   271,   266,   371,   373,
     2,     0,     0,     0,     0,     0,     0,    35,     0,     0,
   272,     0,    43,     0,     0,     5,     0,    70,     0,    60,
     0,     0,     0,   372,     0,     0,   319,   320,   284,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   323,
     0,   273,   424,     0,    93,   312,   140,   151,   141,   164,
   137,   157,   147,   146,   162,   145,   144,   139,   165,   149,
   138,   152,   156,   158,   150,   143,   159,   166,   161,     0,
     0,     0,     0,   136,   155,   154,   167,   168,   169,   170,
   171,   135,   142,   133,   134,     0,     0,     0,    97,     0,
   126,   127,   124,   108,   109,   110,   113,   115,   111,   128,
   129,   116,   117,   462,   121,   120,   107,   125,   123,   122,
   118,   119,   114,   112,   105,   106,   130,     0,   461,   314,
    98,    99,   160,   153,   163,   148,   131,   132,    95,    96,
     0,     0,   102,   101,   100,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   490,   489,     0,     0,
     0,   491,     0,     0,     0,     0,     0,     0,   335,   336,
     0,     0,     0,     0,     0,   230,    45,   238,     0,     0,
     0,   467,    46,    44,     0,    59,     0,     0,   350,    58,
    38,     0,     9,   485,     0,     0,     0,   192,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   218,     0,     0,     0,     0,     0,   464,     0,     0,     0,
     0,    68,     0,   208,   207,    39,   406,   405,   407,     0,
   403,   404,     0,     0,     0,     0,     0,     0,     0,   376,
     4,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   341,   343,   354,   352,   292,
     0,     0,     0,     0,     0,     0,     0,    72,     0,     0,
     0,     0,     0,   346,     0,   290,     0,   411,   412,     0,
    90,     0,    92,     0,   429,   307,   428,     0,     0,     0,
     0,     0,   480,   481,   316,     0,   103,     0,     0,   275,
     0,   326,   325,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   492,     0,     0,     0,
     0,     0,     0,   304,     0,   258,     0,     0,   231,   260,
     0,   233,   286,     0,     0,   253,   252,     0,     0,     0,
     0,     0,    11,    13,    12,     0,   288,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   278,     0,     0,     0,
   219,     0,   487,   220,     0,   222,   282,     0,   466,   465,
   283,     0,     0,     0,     0,   394,   397,   395,   408,   393,
   377,   391,   378,   379,   380,   381,   384,     0,   386,     0,
   387,     0,    15,    16,    17,    18,    19,    36,    37,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   475,
     0,     0,   476,     0,     0,     0,     0,   349,     0,     0,
   473,   474,     0,     0,     0,    30,     0,     0,    23,    31,
   261,     0,    24,    33,     0,     0,    66,    73,     0,    25,
    50,    53,     0,   431,     0,     0,     0,     0,     0,     0,
    94,     0,     0,     0,     0,     0,   444,   443,   445,     0,
   453,   452,   457,   456,   441,     0,     0,   450,     0,   447,
     0,     0,     0,     0,     0,   366,   365,     0,     0,     0,
     0,   333,     0,   327,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   302,   330,   329,   296,
   328,   299,     0,     0,     0,     0,     0,     0,     0,   237,
   469,     0,   259,     0,     0,     0,     0,   468,   285,     0,
     0,   256,   250,     0,     0,     0,     0,     0,     0,     0,
     0,   224,    10,     0,     0,     0,    22,     0,     0,     0,
     0,     0,   223,     0,   262,     0,     0,     0,     0,     0,
     0,     0,   383,   385,   389,     0,   339,     0,     0,   337,
     0,     0,     0,     0,   229,   347,     0,   228,     0,     0,
   348,     0,     0,   344,    48,   345,    49,   265,     0,     0,
    71,     0,   310,     0,     0,   281,   313,     0,   317,     0,
     0,     0,   433,     0,   439,     0,   440,     0,   437,   454,
   458,   104,     0,     0,   368,   334,     0,     3,   370,     0,
   331,     0,     0,     0,     0,     0,     0,   301,   303,   359,
     0,     0,     0,     0,     0,     0,     0,     0,   235,     0,
     0,     0,     0,     0,   243,   255,   225,     0,     0,   226,
     0,     0,     0,    21,   277,     0,     0,     0,   399,   400,
   401,   396,   402,     0,   338,     0,     0,     0,     0,     0,
    27,     0,    28,    55,     0,    29,     0,    57,     0,     0,
     0,     0,     0,     0,   430,   308,   463,     0,   449,     0,
   315,     0,   459,   451,     0,     0,   448,     0,     0,     0,
     0,   367,     0,     0,   369,     0,   293,     0,   294,     0,
     0,     0,     0,   305,   232,     0,   234,   249,   257,     0,
   240,     0,     0,     0,     0,   289,   221,   398,   340,   342,
   355,   353,     0,    26,   264,     0,     0,     0,   432,   438,
     0,   435,   436,     0,     0,     0,     0,     0,     0,   358,
   360,   356,   361,   297,   300,     0,     0,     0,     0,   239,
     0,   245,     0,   227,    51,   311,     0,     0,     0,     0,
     0,     0,     0,   362,     0,     0,   236,   241,     0,     0,
     0,   244,   318,   434,     0,   332,   306,     0,     0,   246,
     0,   242,     0,   247,     0,   248,
    }, yyDgoto = {
//yyDgoto 139
     1,   187,    60,    61,    62,    63,    64,   292,   289,   459,
    65,    66,   467,    67,    68,    69,   108,   205,   206,    71,
    72,    73,    74,    75,    76,    77,    78,   298,   296,   209,
   258,   710,   841,   711,   703,   707,   664,   665,   236,   621,
   416,   245,    80,   408,   612,   409,   365,    81,    82,   694,
   782,   565,   566,   567,   201,   751,   211,   227,   658,   212,
    85,   355,   336,   541,   529,    86,    87,   238,   395,    88,
    89,   266,   271,   595,    90,   272,   241,   578,   273,   378,
   213,   214,   276,   277,   568,   202,   290,    93,   113,   546,
   517,   114,   204,   512,   569,   570,   571,   374,   572,   573,
     2,   219,   220,   425,   255,   681,   191,   574,   254,   427,
   443,   246,   625,   731,   438,   633,   383,   222,   599,   722,
   223,   723,   607,   845,   545,   384,   542,   773,   370,   375,
   554,   777,   505,   507,   506,   651,   650,   544,   371,
    }, yySindex = {
//yySindex 886
     0,     0, 13458, 13683, 13249, 16819, 17369, 17262, 13458, 15251,
 15251, 10625,     0,     0, 16595, 13907, 13907,     0,     0, 13907,
  -265,  -259,     0,     0,     0,     0, 15251, 17155,    97,     0,
  -222,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0, 16259, 16259,   287,  -128, 13571, 15251, 15363,
 16259, 16931, 16259, 16371, 17475,     0,     0,     0,   151,   192,
     0,  -134,     0,     0,     0,     0,     0,     0,     0,     0,
     0,    76,   428,   208,  3043,     0,   -49,     0,  -229,   -61,
     0,    12,     0,   -96,   204,     0,   227,     0,   219,     0,
 16707,     0,    49,     0,  -204,   428,     0,     0,     0,  -265,
  -259,    97,     0,     0,   310, 15251,  -146, 13458,    14,     0,
   159,     0,     0,  -204,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   108,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
 17475,   333,     0,     0,     0,   174,   200,    88,   208,   126,
   234,   144,   426,     0,   182,   126,     0,     0,    76,   251,
   457,     0, 15251, 15251,   225,   260,     0,   248,     0,     0,
     0, 16259, 16259, 16259,  3043,     0,     0,     0,   196,   495,
   499,     0,     0,     0, 12933,     0, 14019, 13907,     0,     0,
     0,   212,     0,     0,   217,   211, 13458,     0,   295,   257,
   266,   286,   256, 13571,   567,     0,   589,   208, 16259,    97,
     0,   132,   239,   546,   247,   239,     0,   516,   347,   440,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  -159,
     0,     0,  -124,   232,   334,   288,   264,   313,  -208,     0,
     0, 13045, 15251, 15251, 15251, 15251, 13683, 15251, 15251, 16259,
 16259, 16259, 16259, 16259, 16259, 16259, 16259, 16259, 16259, 16259,
 16259, 16259, 16259, 16259, 16259, 16259, 16259, 16259, 16259, 16259,
 16259, 16259, 16259, 16259, 16259,     0,     0,     0,     0,     0,
  2065,  2523, 15363,  2997,  2997, 16371, 15475,     0, 15475, 13571,
 16931,   613, 16371,     0,   343,     0,   217,     0,     0,   208,
     0,     0,     0,    76,     0,     0,     0,  2997,  3488, 15363,
 13458, 15251,     0,     0,     0,  1304,     0, 15587,   410,     0,
   256,     0,     0, 13458,   425,  3819,  3881, 15363, 16259, 16259,
 16259, 13458,   430, 13458, 15699,   432,     0,   109,   109,     0,
  3945,  4292, 15363,     0,   658,     0, 16259, 14131,     0,     0,
 14243,     0,     0,   666, 13795,     0,     0,   -49,    97,    29,
   665,   678,     0,     0,     0, 17262,     0, 16259, 13458,   610,
  3819,  3881, 16259, 16259, 16259,   701,     0,     0,    97,  1902,
     0, 15811,     0,     0, 16259,     0,     0, 16259,     0,     0,
     0,     0,  4354,  4418, 15363,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,    58,     0,   711,
     0,   428,     0,     0,     0,     0,     0,     0,     0,   257,
  1999,  1999,  1999,  1999,  2408,  2408,  1955,  3989,  1999,  1999,
  2574,  2574,   691,   691,   257,  1339,   257,   257,  -253,  -253,
  2408,  2408,   751,   751,  2457,  -266,  -266,  -266,   408,     0,
   409,  -259,     0,   411,     0,   413,  -259,     0,     0,   661,
     0,     0,  -259,  -259,  3043,     0, 16259,  2931,     0,     0,
     0,   712,     0,     0,     0,   716,     0,     0,  3043,     0,
     0,     0,    76,     0, 15251, 13458,  -259,     0,     0,  -259,
     0,   674,   512,    31, 17581,   720,     0,     0,     0,   869,
     0,     0,     0,     0,     0, 13458,    76,     0,   742,     0,
   743,   747,   488,   489, 17262,     0,     0,     0,   460, 13458,
   534,     0,   383,     0,   462,   465,   467,   413,   732,  2931,
   410,   549,   564, 16259,   785,   126,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   737, 15251,   487,     0,
     0, 16259,     0,   196,   793, 16259,   196,     0,     0, 16259,
  3043,     0,     0,     1,   799,   803,   805,  2997,  2997,   808,
 14355,     0,     0, 15251,  3043,   725,     0,   257,   257,  3043,
     0,   815,     0, 16259,     0,     0,     0,     0,     0,   768,
 13458,   502,     0,     0,     0, 16259,     0,  3423, 13458,     0,
 13458, 13458, 16371, 16371,     0,     0,   343,     0, 16371, 16259,
     0,   343,   523,     0,     0,     0,     0,     0, 16259, 15923,
     0,  -266,     0,    76,   602,     0,     0,   830,     0, 16259,
    97,   608,     0,   492,     0,   311,     0,   869,     0,     0,
     0,     0, 17043,   126,     0,     0, 13458,     0,     0, 15251,
     0,   611, 16259, 16259, 16259,   541,   620,     0,     0,     0,
 16035, 13458, 13458, 13458,     0,   109,   658, 14467,     0,   658,
   658,   852, 14579, 14691,     0,     0,     0,  -259,  -259,     0,
   -49,    29,   168,     0,     0,  1902,     0,   773,     0,     0,
     0,     0,     0,  3043,     0,   542,   635,   639,   781,  3043,
     0,  3043,     0,     0,  3043,     0,  3043,     0, 16371,  3043,
 16259,     0, 13458, 13458,     0,     0,     0,  1304,     0,   867,
     0,   720,     0,     0,   743,   865,     0,   743,   604,   475,
     0,     0,     0, 13458,     0,   126,     0, 16259,     0, 16259,
   308,   646,   651,     0,     0, 16259,     0,     0,     0, 16259,
     0,   873,   874, 16259,   878,     0,     0,     0,     0,     0,
     0,     0,  3043,     0,     0,   795,   657, 13458,     0,     0,
   492,     0,     0,     0,  4766,  4828, 15363,   174, 13458,     0,
     0,     0,     0,     0,     0, 13458,  2848,   658, 14803,     0,
 14915,     0,   658,     0,     0,     0,   663,   743,     0,     0,
     0,     0,   829,     0,   383,   668,     0,     0, 16259,   881,
 16259,     0,     0,     0,     0,     0,     0,   658, 15027,     0,
   658,     0, 16259,     0,   658,     0,
    }, yyRindex = {
//yyRindex 886
     0,     0,   164,     0,     0,     0,     0,     0,   374,     0,
     0,   349,     0,     0,     0,  7347,  7482,     0,     0,  7595,
  4213,  3627,     0,     0,     0,     0,     0,     0, 16147,     0,
     0,     0,     0,  1731,  2791,     0,     0,  1843,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   178,     0,   842,
   811,    13,     0,     0,   302,     0,     0,     0,   375,  -202,
     0,  6637,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  1023,  5675,  6077,  6367,  6772, 10846,     0,  6885,     0,
     0, 11304,     0,  8654,     0,     0,     0,     0,     0,     0,
    82,  8553,     0,     0, 15139,  6001,     0,     0,     0,  6986,
  5628,   596,  9699,  9812,     0,     0,     0,   178,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   652,
  1179,  1408,  1417,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  1498,  1522,  2384,     0,  2399,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 10835,     0,     0,     0,  -175,     0,     0, 13416,     0,
     0,  6175,     0,  6061,     0,     0,     0,     0,   669,     0,
   360,     0,     0,     0,     0,     0,     2,     0,     0,     0,
   872,     0,     0,     0, 12078,     0,     0,     0, 12726, 12888,
 12888,     0,     0,     0,     0,     0,     0,   900,     0,     0,
     0,     0,     0,     0, 16483,     0,    71,     0,     0,  7696,
  7133,  7234,  8768,   178,     0,   187,     0,   203,     0,   851,
     0,   854,   854,     0,   820,   820,     0,     0,     0,     0,
   935,     0,  1075,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  2176,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   842,     0,     0,     0,     0,     0,     0,   178,
   198,   199,     0,     0, 13202,     0,     0,     0,     0,   136,
     0, 10162,     0,     0,     0,     0,     0,     0,     0,   842,
   374,     0,     0,     0,     0,   161,     0,   114,   404,     0,
  6423,     0,     0,   276, 10275,     0,     0,   842,     0,     0,
     0,   324,     0,   116,     0,     0,     0,     0,     0,   890,
     0,     0,   842,     0, 12888,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   908,     0,     0,   415,   909,   909,
   464,     0,     0,     0,     0,     0,     0,     0,    71,     0,
     0,     0,     0,     0,     0,     0,     0,    57,   909,   851,
     0,   858,     0,     0,    40,     0,     0,   827,     0,     0,
     0,  1101,     0,     0,   842,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  6540,     0,     0,     0,     0,     0,     0,     0,  7843,
 11430, 11484, 11564, 11700, 10915, 10995, 11754, 11916, 11808, 11862,
 11970, 12024,  9117,  9302,  7944,  9406,  8057,  8192,  1627,  9013,
 11075, 11350,  9486,  9591,     0, 10393, 10393, 10506,  4574,     0,
  4686,  3739,     0,  5048,  3153,  5160, 15139,     0,  3265,     0,
     0,     0,  5521,  5521, 12132,     0,     0, 11154,     0,     0,
     0,     0,     0,     0,  5581,     0,     0,     0, 12186,     0,
     0,     0,     0,     0,     0,   374,  5961,  9930, 10043,     0,
     0,     0,     0,   909,     0,   156,     0,     0,     0,   500,
     0,     0,     0,     0,     0,   374,     0,     0,   197,     0,
   197,   197,   838,     0,     0,     0,     0,     6,   134,   427,
   690,     0,   690,     0,  2205,  2317,  2679,  4101,     0, 12780,
   690,     0,     0,     0,   143,     0,     0,     0,     0,     0,
     0,     0,   813,   833,  2002,   989,     0,     0,     0,     0,
     0,     0,     0, 12834, 12888,     0,     0,     0,     0,     0,
    38,     0,     0,     0,   916,     0,     0,     0,     0,     0,
     0,     0,     0,     0, 12240,     0,     0,  8305,  8406, 12294,
   550,     0,     0,     0,     0,  1222,  1318,  5574,  1035,     0,
    71,     0,     0,     0,     0,     0,     0,     0,   116,     0,
   116,    71,     0,     0,     0,     0, 13314,     0,     0,     0,
     0, 13360,  8902,     0,     0,     0,     0,     0,     0,     0,
     0, 10506,     0,     0,     0,     0,     0,     0,     0,     0,
   909,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   116,     0,     0,     0,
     0,     0,     0,     0,     0,  6524,     0,     0,     0,     0,
     0,   231,   116,   116,  1065,     0, 12888,     0,     0, 12888,
   916,     0,     0,     0,     0,     0,     0,    39,    39,     0,
     0,   909,     0,     0,     0,   851,  1140,     0,     0,     0,
     0,     0,     0, 12348,     0,     0,     0,     0,     0, 12402,
     0, 12456,     0,     0, 12510,     0, 12564,     0,     0, 12618,
     0,  5871,    71,   374,     0,     0,     0,   161,     0,     0,
     0,     0,     0,     0,   197,   197,     0,   197,     0,     0,
   131,     0,   177,   374,     0,     0,     0,     0,     0,     0,
   690,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   916,   916,     0,     0,     0,     0,     0,     0,     0,
     0,     0, 12672,     0,     0,     0,     0,   374,     0,     0,
     0,     0,     0,   528,     0,     0,   842,  -175,   276,     0,
     0,     0,     0,     0,     0,   116, 12888,   916,     0,     0,
     0,     0,   916,     0,     0,     0,     0,   197,   193,   522,
   565,   167,     0,     0,   690,     0,     0,     0,     0,   916,
     0,     0,     0,     0,   551,     0,     0,   916,     0,     0,
   916,     0,     0,     0,   916,     0,
    }, yyGindex = {
//yyGindex 139
     0,     0,     0,     0,   897,     0,     0,     0,   631,  -205,
     0,     0,     0,     0,     0,     0,   957,   941,  -351,     0,
    37,  1270,   -15,     8,   106,   117,     0,     0,     0,   244,
   101,  -369,     0,   100,     0,     0,    34,  -460,   573,     0,
     0,   -10,   959,   277,    41,     0,     0,  -233,     0,   -19,
  -354,   189,   418,  -651,     0,     0,   477,   321,  -240,   692,
   968,   889,     0,  -526,  -150,   893,   -24,  1010,  -384,    -7,
   -29,  -188,    17,     0,     0,    28,  -313,     0,  -317,   147,
   724,   884,   738,     0,   289,   -12,     0,    -4,   667,  -272,
     0,   -86,     4,    -1,   293,  -537,     0,     0,     0,     0,
     0,   -40,   921,     0,     0,     0,     0,     0,   188,     0,
   342,     0,     0,     0,     0,     0,  -213,     0,  -381,     0,
     0,     0,     0,     0,     0,   171,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,
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
					// line 323 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_BEG);
                  support.initTopLocalVariables();

              }
  break;
case 2:
					// line 327 "DefaultRubyParser.y"
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
					// line 343 "DefaultRubyParser.y"
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
					// line 359 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-1+yyTop]) instanceof BlockNode) {
                     support.checkUselessStatements(((BlockNode)yyVals[-1+yyTop]));
		  }
                  yyVal = ((Node)yyVals[-1+yyTop]);
              }
  break;
case 6:
					// line 367 "DefaultRubyParser.y"
  {
                    yyVal = support.newline_node(((Node)yyVals[0+yyTop]), getPosition(((ISourcePositionHolder)yyVals[0+yyTop]), true));
                }
  break;
case 7:
					// line 370 "DefaultRubyParser.y"
  {
		    yyVal = support.appendToBlock(((Node)yyVals[-2+yyTop]), support.newline_node(((Node)yyVals[0+yyTop]), getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]), true)));
                }
  break;
case 8:
					// line 373 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 9:
					// line 377 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
                }
  break;
case 10:
					// line 379 "DefaultRubyParser.y"
  {
                    yyVal = new AliasNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 11:
					// line 382 "DefaultRubyParser.y"
  {
                    yyVal = new VAliasNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 12:
					// line 385 "DefaultRubyParser.y"
  {
                    yyVal = new VAliasNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), "$" + ((BackRefNode)yyVals[0+yyTop]).getType()); /* XXX*/
                }
  break;
case 13:
					// line 388 "DefaultRubyParser.y"
  {
                    yyerror("can't make alias for the number variables");
                    yyVal = null; /*XXX 0*/
                }
  break;
case 14:
					// line 392 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 15:
					// line 395 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), null);
                }
  break;
case 16:
					// line 398 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), null, ((Node)yyVals[-2+yyTop]));
                }
  break;
case 17:
					// line 401 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = new WhileNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                    } else {
                        yyVal = new WhileNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                    }
                }
  break;
case 18:
					// line 408 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = new UntilNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode());
                    } else {
                        yyVal = new UntilNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]));
                    }
                }
  break;
case 19:
					// line 416 "DefaultRubyParser.y"
  {
		  yyVal = new RescueNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), new RescueBodyNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), null,((Node)yyVals[0+yyTop]), null), null);
                }
  break;
case 20:
					// line 420 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("BEGIN in method");
                    }
                    support.getLocalNames().push(new LocalNamesElement());
                }
  break;
case 21:
					// line 425 "DefaultRubyParser.y"
  {
                    support.getResult().addBeginNode(new ScopeNode(getPosition(((Token)yyVals[-4+yyTop]), true), ((LocalNamesElement) support.getLocalNames().peek()).getNamesArray(), ((Node)yyVals[-1+yyTop])));
                    support.getLocalNames().pop();
                    yyVal = null; /*XXX 0;*/
                }
  break;
case 22:
					// line 430 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("END in method; use at_exit");
                    }
                    support.getResult().addEndNode(new IterNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), null, new PostExeNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop]))), ((Node)yyVals[-1+yyTop])));
                    yyVal = null;
                }
  break;
case 23:
					// line 437 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 24:
					// line 441 "DefaultRubyParser.y"
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
					// line 450 "DefaultRubyParser.y"
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
					// line 476 "DefaultRubyParser.y"
  {
                    /* Much smaller than ruby block */
                    yyVal = new OpElementAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));

                }
  break;
case 27:
					// line 481 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 28:
					// line 484 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 29:
					// line 487 "DefaultRubyParser.y"
  {
  yyVal = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 30:
					// line 490 "DefaultRubyParser.y"
  {
                    support.backrefAssignError(((Node)yyVals[-2+yyTop]));
                    yyVal = null;
                }
  break;
case 31:
					// line 494 "DefaultRubyParser.y"
  {
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), new SValueNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
                }
  break;
case 32:
					// line 497 "DefaultRubyParser.y"
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
					// line 505 "DefaultRubyParser.y"
  {
                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
		    yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
		}
  break;
case 36:
					// line 512 "DefaultRubyParser.y"
  {
                    yyVal = support.newAndNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 37:
					// line 515 "DefaultRubyParser.y"
  {
                    yyVal = support.newOrNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 38:
					// line 518 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
                }
  break;
case 39:
					// line 521 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
                }
  break;
case 41:
					// line 526 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
		    yyVal = ((Node)yyVals[0+yyTop]); /*Do we really need this set? $1 is $$?*/
		}
  break;
case 44:
					// line 533 "DefaultRubyParser.y"
  {
                    yyVal = new ReturnNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))));
                }
  break;
case 45:
					// line 536 "DefaultRubyParser.y"
  {
                    yyVal = new BreakNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))));
                }
  break;
case 46:
					// line 539 "DefaultRubyParser.y"
  {
                    yyVal = new NextNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))));
                }
  break;
case 48:
					// line 544 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 49:
					// line 547 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 50:
					// line 551 "DefaultRubyParser.y"
  {
                      support.getBlockNames().push(new BlockNamesElement());
		  }
  break;
case 51:
					// line 553 "DefaultRubyParser.y"
  {
                      yyVal = new IterNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), null);
                      support.getBlockNames().pop();
		  }
  break;
case 52:
					// line 558 "DefaultRubyParser.y"
  {
                    yyVal = support.new_fcall((String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop])); /* .setPosFrom($2);*/
                }
  break;
case 53:
					// line 561 "DefaultRubyParser.y"
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
					// line 571 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 55:
					// line 574 "DefaultRubyParser.y"
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
					// line 584 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 57:
					// line 587 "DefaultRubyParser.y"
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
					// line 597 "DefaultRubyParser.y"
  {
		    yyVal = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop])); /* .setPosFrom($2);*/
		}
  break;
case 59:
					// line 600 "DefaultRubyParser.y"
  {
                    yyVal = support.new_yield(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
		}
  break;
case 61:
					// line 605 "DefaultRubyParser.y"
  {
                    yyVal = ((MultipleAsgnNode)yyVals[-1+yyTop]);
		}
  break;
case 63:
					// line 610 "DefaultRubyParser.y"
  {
	            yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(((MultipleAsgnNode)yyVals[-1+yyTop])), null);
                }
  break;
case 64:
					// line 614 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), ((ListNode)yyVals[0+yyTop]), null);
                }
  break;
case 65:
					// line 617 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((ListNode)yyVals[-1+yyTop]).add(((Node)yyVals[0+yyTop])), null);
                }
  break;
case 66:
					// line 620 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 67:
					// line 623 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((ListNode)yyVals[-1+yyTop]), new StarNode(getPosition(null)));
                }
  break;
case 68:
					// line 626 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), null, ((Node)yyVals[0+yyTop]));
                }
  break;
case 69:
					// line 629 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), null, new StarNode(getPosition(null)));
                }
  break;
case 71:
					// line 634 "DefaultRubyParser.y"
  {
                    yyVal = ((MultipleAsgnNode)yyVals[-1+yyTop]);
                }
  break;
case 72:
					// line 638 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))).add(((Node)yyVals[-1+yyTop]));
                }
  break;
case 73:
					// line 641 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
                }
  break;
case 74:
					// line 645 "DefaultRubyParser.y"
  {
                    yyVal = support.assignable(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), yyVals[0+yyTop], null);
                }
  break;
case 75:
					// line 648 "DefaultRubyParser.y"
  {
                    yyVal = support.getElementAssignmentNode(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 76:
					// line 651 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 77:
					// line 654 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 78:
					// line 657 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 79:
					// line 660 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }
			
		    yyVal = new ConstDeclNode(support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue(), null);
		}
  break;
case 80:
					// line 667 "DefaultRubyParser.y"
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
					// line 677 "DefaultRubyParser.y"
  {
	            support.backrefAssignError(((Node)yyVals[0+yyTop]));
                    yyVal = null;
                }
  break;
case 82:
					// line 682 "DefaultRubyParser.y"
  {
                    yyVal = support.assignable(getPosition(((ISourcePositionHolder)yyVals[0+yyTop]), true), yyVals[0+yyTop], null);
                }
  break;
case 83:
					// line 685 "DefaultRubyParser.y"
  {
                    yyVal = support.getElementAssignmentNode(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 84:
					// line 688 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 85:
					// line 691 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
 	        }
  break;
case 86:
					// line 694 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 87:
					// line 697 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }
			
                    yyVal = new ConstDeclNode(support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue(), null);
	        }
  break;
case 88:
					// line 704 "DefaultRubyParser.y"
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
					// line 713 "DefaultRubyParser.y"
  {
                    support.backrefAssignError(((Node)yyVals[0+yyTop]));
                    yyVal = null;
		}
  break;
case 90:
					// line 718 "DefaultRubyParser.y"
  {
                    yyerror("class/module name must be CONSTANT");
                }
  break;
case 92:
					// line 723 "DefaultRubyParser.y"
  {
                    yyVal = new Colon3Node(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
		}
  break;
case 93:
					// line 726 "DefaultRubyParser.y"
  {
                    /* $1 was $$ in ruby?*/
                    yyVal = new Colon2Node(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), null, (String) ((Token)yyVals[0+yyTop]).getValue());
 	        }
  break;
case 94:
					// line 730 "DefaultRubyParser.y"
  {
                    yyVal = new Colon2Node(support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
		}
  break;
case 98:
					// line 737 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 99:
					// line 741 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    yyVal = yyVals[0+yyTop];
                }
  break;
case 102:
					// line 749 "DefaultRubyParser.y"
  {
                    yyVal = new UndefNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 103:
					// line 752 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
	        }
  break;
case 104:
					// line 754 "DefaultRubyParser.y"
  {
                    yyVal = support.appendToBlock(((Node)yyVals[-3+yyTop]), new UndefNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue()));
                }
  break;
case 105:
					// line 758 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("|"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 106:
					// line 759 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("^"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 107:
					// line 760 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("&"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 108:
					// line 761 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("<=>"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 109:
					// line 762 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("=="); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 110:
					// line 763 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("==="); yyVal = ((Token)yyVals[0+yyTop]);}
  break;
case 111:
					// line 764 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("=~"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 112:
					// line 765 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue(">"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 113:
					// line 766 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue(">="); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 114:
					// line 767 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("<"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 115:
					// line 768 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("<="); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 116:
					// line 769 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("<<"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 117:
					// line 770 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue(">>"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 118:
					// line 771 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("+"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 119:
					// line 772 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("-"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 120:
					// line 773 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("*"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 121:
					// line 774 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("*"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 122:
					// line 775 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("/"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 123:
					// line 776 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("%"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 124:
					// line 777 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("**"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 125:
					// line 778 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("~"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 126:
					// line 779 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("+@"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 127:
					// line 780 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("-@"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 128:
					// line 781 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("[]"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 129:
					// line 782 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("[]="); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 130:
					// line 783 "DefaultRubyParser.y"
  {  yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 172:
					// line 794 "DefaultRubyParser.y"
  {
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
		    /* FIXME: Consider fixing node_assign itself rather than single case*/
		    ((Node)yyVal).setPosition(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
                }
  break;
case 173:
					// line 799 "DefaultRubyParser.y"
  {
                    yyVal = support.node_assign(((Node)yyVals[-4+yyTop]), new RescueNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), new RescueBodyNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), null,((Node)yyVals[0+yyTop]), null), null));
		}
  break;
case 174:
					// line 802 "DefaultRubyParser.y"
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
					// line 829 "DefaultRubyParser.y"
  {
                    yyVal = new OpElementAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 176:
					// line 832 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 177:
					// line 835 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 178:
					// line 838 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                }
  break;
case 179:
					// line 841 "DefaultRubyParser.y"
  {
		    yyerror("constant re-assignment");
		    yyVal = null;
	        }
  break;
case 180:
					// line 845 "DefaultRubyParser.y"
  {
		    yyerror("constant re-assignment");
		    yyVal = null;
	        }
  break;
case 181:
					// line 849 "DefaultRubyParser.y"
  {
                    support.backrefAssignError(((Node)yyVals[-2+yyTop]));
                    yyVal = null;
                }
  break;
case 182:
					// line 853 "DefaultRubyParser.y"
  {
		    support.checkExpression(((Node)yyVals[-2+yyTop]));
		    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = new DotNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), false);
                }
  break;
case 183:
					// line 858 "DefaultRubyParser.y"
  {
		    support.checkExpression(((Node)yyVals[-2+yyTop]));
		    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = new DotNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), true);
                }
  break;
case 184:
					// line 863 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "+", ((Node)yyVals[0+yyTop]));
                }
  break;
case 185:
					// line 866 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "-", ((Node)yyVals[0+yyTop]));
                }
  break;
case 186:
					// line 869 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "*", ((Node)yyVals[0+yyTop]));
                }
  break;
case 187:
					// line 872 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "/", ((Node)yyVals[0+yyTop]));
                }
  break;
case 188:
					// line 875 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "%", ((Node)yyVals[0+yyTop]));
                }
  break;
case 189:
					// line 878 "DefaultRubyParser.y"
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
					// line 897 "DefaultRubyParser.y"
  {
                    Object number = ((Token)yyVals[-2+yyTop]).getValue();

                    yyVal = support.getOperatorCallNode(support.getOperatorCallNode((number instanceof Long ? (Node) new FixnumNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Long) number).longValue()) : (Node)new BignumNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((BigInteger) number))), "**", ((Node)yyVals[0+yyTop])), "-@");
                }
  break;
case 191:
					// line 902 "DefaultRubyParser.y"
  {
  /* ENEBO: Seems like this should be $2*/
                    yyVal = support.getOperatorCallNode(support.getOperatorCallNode(new FloatNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Double) ((Token)yyVals[-3+yyTop]).getValue()).doubleValue()), "**", ((Node)yyVals[0+yyTop])), "-@");
                }
  break;
case 192:
					// line 906 "DefaultRubyParser.y"
  {
 	            if (((Node)yyVals[0+yyTop]) != null && ((Node)yyVals[0+yyTop]) instanceof ILiteralNode) {
		        yyVal = ((Node)yyVals[0+yyTop]);
		    } else {
                        yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "+@");
		    }
                }
  break;
case 193:
					// line 913 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "-@");
		}
  break;
case 194:
					// line 916 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "|", ((Node)yyVals[0+yyTop]));
                }
  break;
case 195:
					// line 919 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "^", ((Node)yyVals[0+yyTop]));
                }
  break;
case 196:
					// line 922 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "&", ((Node)yyVals[0+yyTop]));
                }
  break;
case 197:
					// line 925 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=>", ((Node)yyVals[0+yyTop]));
                }
  break;
case 198:
					// line 928 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">", ((Node)yyVals[0+yyTop]));
                }
  break;
case 199:
					// line 931 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">=", ((Node)yyVals[0+yyTop]));
                }
  break;
case 200:
					// line 934 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<", ((Node)yyVals[0+yyTop]));
                }
  break;
case 201:
					// line 937 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=", ((Node)yyVals[0+yyTop]));
                }
  break;
case 202:
					// line 940 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop]));
                }
  break;
case 203:
					// line 943 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "===", ((Node)yyVals[0+yyTop]));
                }
  break;
case 204:
					// line 946 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop])));
                }
  break;
case 205:
					// line 949 "DefaultRubyParser.y"
  {
                    yyVal = support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 206:
					// line 952 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
                }
  break;
case 207:
					// line 955 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
                }
  break;
case 208:
					// line 958 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "~");
                }
  break;
case 209:
					// line 961 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<<", ((Node)yyVals[0+yyTop]));
                }
  break;
case 210:
					// line 964 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">>", ((Node)yyVals[0+yyTop]));
                }
  break;
case 211:
					// line 967 "DefaultRubyParser.y"
  {
                    yyVal = support.newAndNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 212:
					// line 970 "DefaultRubyParser.y"
  {
                    yyVal = support.newOrNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 213:
					// line 973 "DefaultRubyParser.y"
  {
	            support.setInDefined(true);
		}
  break;
case 214:
					// line 975 "DefaultRubyParser.y"
  {
                    support.setInDefined(false);
                    yyVal = new DefinedNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Node)yyVals[0+yyTop]));
                }
  break;
case 215:
					// line 979 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 216:
					// line 982 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 217:
					// line 986 "DefaultRubyParser.y"
  {
		    support.checkExpression(((Node)yyVals[0+yyTop]));
	            yyVal = ((Node)yyVals[0+yyTop]);   
		}
  break;
case 219:
					// line 992 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), "parenthesize argument(s) for future version");
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))).add(((Node)yyVals[-1+yyTop]));
                }
  break;
case 220:
					// line 996 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                }
  break;
case 221:
					// line 999 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[-1+yyTop]));
                    yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 222:
					// line 1003 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                }
  break;
case 223:
					// line 1006 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[-1+yyTop]));
		    yyVal = new NewlineNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), new SplatNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])));
                }
  break;
case 224:
					// line 1011 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                }
  break;
case 225:
					// line 1014 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-2+yyTop]);
                }
  break;
case 226:
					// line 1017 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), "parenthesize argument(s) for future version");
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop]))).add(((Node)yyVals[-2+yyTop]));
                }
  break;
case 227:
					// line 1021 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), "parenthesize argument(s) for future version");
                    yyVal = ((ListNode)yyVals[-4+yyTop]).add(((Node)yyVals[-2+yyTop]));
                }
  break;
case 230:
					// line 1029 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), "parenthesize argument(s) for future version");
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 231:
					// line 1033 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_blk_pass(((ListNode)yyVals[-1+yyTop]), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 232:
					// line 1036 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 233:
					// line 1040 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 234:
					// line 1044 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop]))).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 235:
					// line 1048 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-3+yyTop]).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 236:
					// line 1052 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[-1+yyTop]));
		    yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop])), ((ListNode)yyVals[-6+yyTop]).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 237:
					// line 1057 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_blk_pass(new SplatNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 238:
					// line 1060 "DefaultRubyParser.y"
  {
	        }
  break;
case 239:
					// line 1063 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_blk_pass(support.list_concat(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop]))).add(((Node)yyVals[-3+yyTop])), ((ListNode)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 240:
					// line 1066 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_blk_pass(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(((Node)yyVals[-2+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
                  }
  break;
case 241:
					// line 1069 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop]))).add(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 242:
					// line 1073 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop])), support.list_concat(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop]))).add(((Node)yyVals[-6+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 243:
					// line 1077 "DefaultRubyParser.y"
  {
                      yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 244:
					// line 1081 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop]))).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 245:
					// line 1085 "DefaultRubyParser.y"
  {
                      yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop]))).add(((Node)yyVals[-3+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 246:
					// line 1089 "DefaultRubyParser.y"
  {
                      yyVal = support.list_concat(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop]))).add(((Node)yyVals[-5+yyTop])), ((ListNode)yyVals[-3+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 247:
					// line 1093 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop])), new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop]))).add(((Node)yyVals[-6+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 248:
					// line 1097 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-8+yyTop])), support.list_concat(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-8+yyTop]))).add(((Node)yyVals[-8+yyTop])), ((ListNode)yyVals[-6+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 249:
					// line 1101 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_blk_pass(new SplatNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 250:
					// line 1104 "DefaultRubyParser.y"
  {}
  break;
case 251:
					// line 1106 "DefaultRubyParser.y"
  { 
		    yyVal = new Long(lexer.getCmdArgumentState().begin());
		}
  break;
case 252:
					// line 1108 "DefaultRubyParser.y"
  {
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 254:
					// line 1114 "DefaultRubyParser.y"
  {                    
		    lexer.setState(LexState.EXPR_ENDARG);
		  }
  break;
case 255:
					// line 1116 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), "don't put space before argument parentheses");
		    yyVal = null;
		  }
  break;
case 256:
					// line 1120 "DefaultRubyParser.y"
  {
		    lexer.setState(LexState.EXPR_ENDARG);
		  }
  break;
case 257:
					// line 1122 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), "don't put space before argument parentheses");
		    yyVal = ((Node)yyVals[-2+yyTop]);
		  }
  break;
case 258:
					// line 1127 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = new BlockPassNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
                }
  break;
case 259:
					// line 1132 "DefaultRubyParser.y"
  {
                    yyVal = ((BlockPassNode)yyVals[0+yyTop]);
                }
  break;
case 261:
					// line 1137 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 262:
					// line 1140 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 263:
					// line 1144 "DefaultRubyParser.y"
  {
		    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 264:
					// line 1147 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
		}
  break;
case 265:
					// line 1150 "DefaultRubyParser.y"
  {  
                    yyVal = new SplatNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
		}
  break;
case 274:
					// line 1162 "DefaultRubyParser.y"
  {
                    yyVal = new VCallNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
		}
  break;
case 275:
					// line 1166 "DefaultRubyParser.y"
  {
                    yyVal = new BeginNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-1+yyTop]));
		}
  break;
case 276:
					// line 1169 "DefaultRubyParser.y"
  { lexer.setState(LexState.EXPR_ENDARG); }
  break;
case 277:
					// line 1169 "DefaultRubyParser.y"
  {
		    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), "(...) interpreted as grouped expression");
                    yyVal = ((Node)yyVals[-3+yyTop]);
		}
  break;
case 278:
					// line 1173 "DefaultRubyParser.y"
  {
	            yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 279:
					// line 1176 "DefaultRubyParser.y"
  {
                    yyVal = new Colon2Node(support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 280:
					// line 1179 "DefaultRubyParser.y"
  {
                    yyVal = new Colon3Node(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 281:
					// line 1182 "DefaultRubyParser.y"
  {
                    yyVal = new CallNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop]), "[]", ((Node)yyVals[-1+yyTop]));
                }
  break;
case 282:
					// line 1185 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-1+yyTop]) == null) {
                        yyVal = new ZArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))); /* zero length array*/
                    } else {
                        yyVal = ((Node)yyVals[-1+yyTop]);
                    }
                }
  break;
case 283:
					// line 1192 "DefaultRubyParser.y"
  {
                    yyVal = new HashNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((ListNode)yyVals[-1+yyTop]));
                }
  break;
case 284:
					// line 1195 "DefaultRubyParser.y"
  {
		    yyVal = new ReturnNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), null);
                }
  break;
case 285:
					// line 1198 "DefaultRubyParser.y"
  {
                    yyVal = support.new_yield(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 286:
					// line 1201 "DefaultRubyParser.y"
  {
                    yyVal = new YieldNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), null, false);
                }
  break;
case 287:
					// line 1204 "DefaultRubyParser.y"
  {
                    yyVal = new YieldNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), null, false);
                }
  break;
case 288:
					// line 1207 "DefaultRubyParser.y"
  {
	            support.setInDefined(true);
		}
  break;
case 289:
					// line 1209 "DefaultRubyParser.y"
  {
                    support.setInDefined(false);
                    yyVal = new DefinedNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 290:
					// line 1213 "DefaultRubyParser.y"
  {
                    ((IterNode)yyVals[0+yyTop]).setIterNode(new FCallNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]), true), (String) ((Token)yyVals[-1+yyTop]).getValue(), null));
                    yyVal = ((IterNode)yyVals[0+yyTop]);
                }
  break;
case 292:
					// line 1218 "DefaultRubyParser.y"
  {
		    if (((Node)yyVals[-1+yyTop]) != null && ((Node)yyVals[-1+yyTop]) instanceof BlockPassNode) {
                        throw new SyntaxException(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), "Both block arg and actual block given.");
		    }
                    ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVals[-1+yyTop]));
                    yyVal = ((IterNode)yyVals[0+yyTop]);
                }
  break;
case 293:
					// line 1225 "DefaultRubyParser.y"
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
case 294:
					// line 1234 "DefaultRubyParser.y"
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
case 295:
					// line 1243 "DefaultRubyParser.y"
  { 
	            lexer.getConditionState().begin();
		}
  break;
case 296:
					// line 1245 "DefaultRubyParser.y"
  {
		    lexer.getConditionState().end();
		}
  break;
case 297:
					// line 1247 "DefaultRubyParser.y"
  {
                    yyVal = new WhileNode(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
			    nd_set_type($$, NODE_UNTIL);
			    } */
                }
  break;
case 298:
					// line 1254 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().begin();
                }
  break;
case 299:
					// line 1256 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().end();
                }
  break;
case 300:
					// line 1258 "DefaultRubyParser.y"
  {
                    yyVal = new UntilNode(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
			    nd_set_type($$, NODE_WHILE);
			    } */
                }
  break;
case 301:
					// line 1267 "DefaultRubyParser.y"
  {
		    yyVal = new CaseNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop])); /* XXX*/
                }
  break;
case 302:
					// line 1270 "DefaultRubyParser.y"
  {
                    yyVal = new CaseNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), null, ((Node)yyVals[-1+yyTop]));
                }
  break;
case 303:
					// line 1273 "DefaultRubyParser.y"
  {
		    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 304:
					// line 1276 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().begin();
                }
  break;
case 305:
					// line 1278 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().end();
                }
  break;
case 306:
					// line 1281 "DefaultRubyParser.y"
  {
                    yyVal = new ForNode(getPosition(((ISourcePositionHolder)yyVals[-8+yyTop])), ((Node)yyVals[-7+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-4+yyTop]));
                }
  break;
case 307:
					// line 1284 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("class definition in method body");
                    }
                    support.getLocalNames().push(new LocalNamesElement());
                    /* $$ = new Integer(ruby.getSourceLine());*/
                }
  break;
case 308:
					// line 1291 "DefaultRubyParser.y"
  {
                    yyVal = new ClassNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-4+yyTop]), new ScopeNode(getRealPosition(((Node)yyVals[-1+yyTop])), ((LocalNamesElement) support.getLocalNames().peek()).getNamesArray(), ((Node)yyVals[-1+yyTop])), ((Node)yyVals[-3+yyTop]));
                    /* $<Node>$.setLine($<Integer>4.intValue());*/
                    support.getLocalNames().pop();
                }
  break;
case 309:
					// line 1296 "DefaultRubyParser.y"
  {
                    yyVal = new Boolean(support.isInDef());
                    support.setInDef(false);
                }
  break;
case 310:
					// line 1299 "DefaultRubyParser.y"
  {
                    yyVal = new Integer(support.getInSingle());
                    support.setInSingle(0);
                    support.getLocalNames().push(new LocalNamesElement());
                }
  break;
case 311:
					// line 1304 "DefaultRubyParser.y"
  {
                    yyVal = new SClassNode(support.union(((Token)yyVals[-7+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-5+yyTop]), new ScopeNode(getRealPosition(((Node)yyVals[-1+yyTop])), ((LocalNamesElement) support.getLocalNames().peek()).getNamesArray(), ((Node)yyVals[-1+yyTop])));
                    support.getLocalNames().pop();
                    support.setInDef(((Boolean)yyVals[-4+yyTop]).booleanValue());
                    support.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
                }
  break;
case 312:
					// line 1310 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) { 
                        yyerror("module definition in method body");
                    }
                    support.getLocalNames().push(new LocalNamesElement());
                    /* $$ = new Integer(ruby.getSourceLine());*/
                }
  break;
case 313:
					// line 1317 "DefaultRubyParser.y"
  {
                    yyVal = new ModuleNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-3+yyTop]), new ScopeNode(getRealPosition(((Node)yyVals[-1+yyTop])), ((LocalNamesElement) support.getLocalNames().peek()).getNamesArray(), ((Node)yyVals[-1+yyTop])));
                    /* $<Node>$.setLine($<Integer>3.intValue());*/
                    support.getLocalNames().pop();
                }
  break;
case 314:
					// line 1322 "DefaultRubyParser.y"
  {
		      /* missing
			$<id>$ = cur_mid;
			cur_mid = $2; */
                    support.setInDef(true);
                    support.getLocalNames().push(new LocalNamesElement());
                }
  break;
case 315:
					// line 1330 "DefaultRubyParser.y"
  {
		      /* was in old jruby grammar support.getClassNest() !=0 || IdUtil.isAttrSet($2) ? Visibility.PUBLIC : Visibility.PRIVATE); */
                    /* NOEX_PRIVATE for toplevel */
                    yyVal = new DefnNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), new ArgumentNode(((ISourcePositionHolder)yyVals[-4+yyTop]).getPosition(), (String) ((Token)yyVals[-4+yyTop]).getValue()), ((Node)yyVals[-2+yyTop]),
		                      new ScopeNode(getRealPosition(((Node)yyVals[-1+yyTop])), ((LocalNamesElement) support.getLocalNames().peek()).getNamesArray(), ((Node)yyVals[-1+yyTop])), Visibility.PRIVATE);
                    /* $<Node>$.setPosFrom($4);*/
                    support.getLocalNames().pop();
                    support.setInDef(false);
		    /* missing cur_mid = $<id>3; */
                }
  break;
case 316:
					// line 1340 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
                }
  break;
case 317:
					// line 1342 "DefaultRubyParser.y"
  {
                    support.setInSingle(support.getInSingle() + 1);
                    support.getLocalNames().push(new LocalNamesElement());
                    lexer.setState(LexState.EXPR_END); /* force for args */
                }
  break;
case 318:
					// line 1348 "DefaultRubyParser.y"
  {
                    yyVal = new DefsNode(support.union(((Token)yyVals[-8+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-7+yyTop]), (String) ((Token)yyVals[-4+yyTop]).getValue(), ((Node)yyVals[-2+yyTop]), new ScopeNode(getPosition(null), ((LocalNamesElement) support.getLocalNames().peek()).getNamesArray(), ((Node)yyVals[-1+yyTop])));
                    /* $<Node>$.setPosFrom($2);*/
                    support.getLocalNames().pop();
                    support.setInSingle(support.getInSingle() - 1);
                }
  break;
case 319:
					// line 1354 "DefaultRubyParser.y"
  {
                    yyVal = new BreakNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                }
  break;
case 320:
					// line 1357 "DefaultRubyParser.y"
  {
                    yyVal = new NextNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                }
  break;
case 321:
					// line 1360 "DefaultRubyParser.y"
  {
                    yyVal = new RedoNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                }
  break;
case 322:
					// line 1363 "DefaultRubyParser.y"
  {
                    yyVal = new RetryNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                }
  break;
case 323:
					// line 1367 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
		    yyVal = ((Node)yyVals[0+yyTop]);
		}
  break;
case 332:
					// line 1384 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), support.getConditionNode(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 334:
					// line 1389 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 336:
					// line 1394 "DefaultRubyParser.y"
  {}
  break;
case 338:
					// line 1397 "DefaultRubyParser.y"
  {
                    yyVal = new ZeroArgNode(getPosition(null));
                }
  break;
case 339:
					// line 1400 "DefaultRubyParser.y"
  {
                    yyVal = new ZeroArgNode(getPosition(null));
		}
  break;
case 340:
					// line 1403 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 341:
					// line 1407 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push(new BlockNamesElement());
		}
  break;
case 342:
					// line 1410 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), null);
                    support.getBlockNames().pop();
                }
  break;
case 343:
					// line 1415 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-1+yyTop]) instanceof BlockPassNode) {
                        throw new SyntaxException(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), "Both block arg and actual block given.");
                    }
                    ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVals[-1+yyTop]));
                    yyVal = ((IterNode)yyVals[0+yyTop]);
                }
  break;
case 344:
					// line 1422 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 345:
					// line 1425 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 346:
					// line 1429 "DefaultRubyParser.y"
  {
                    yyVal = support.new_fcall((String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop])); /* .setPosFrom($2);*/
                }
  break;
case 347:
					// line 1432 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 348:
					// line 1435 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 349:
					// line 1438 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue(), null);
                }
  break;
case 350:
					// line 1441 "DefaultRubyParser.y"
  {
                    yyVal = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop]));
                }
  break;
case 351:
					// line 1444 "DefaultRubyParser.y"
  {
                    yyVal = new ZSuperNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                }
  break;
case 352:
					// line 1448 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push(new BlockNamesElement());
		}
  break;
case 353:
					// line 1450 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), null);
                    support.getBlockNames().pop();
                }
  break;
case 354:
					// line 1454 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push(new BlockNamesElement());
		}
  break;
case 355:
					// line 1456 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), null);
                    support.getBlockNames().pop();
                }
  break;
case 356:
					// line 1463 "DefaultRubyParser.y"
  {
		    yyVal = new WhenNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 358:
					// line 1468 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-3+yyTop]).add(new WhenNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Node)yyVals[0+yyTop]), null, null));
                }
  break;
case 359:
					// line 1471 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))).add(new WhenNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]), null, null));
                }
  break;
case 362:
					// line 1481 "DefaultRubyParser.y"
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
case 363:
					// line 1490 "DefaultRubyParser.y"
  {yyVal = null;}
  break;
case 364:
					// line 1492 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition()).add(((Node)yyVals[0+yyTop]));
		}
  break;
case 367:
					// line 1498 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 369:
					// line 1503 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[0+yyTop]) != null) {
                        yyVal = ((Node)yyVals[0+yyTop]);
                    } else {
                        yyVal = new NilNode(getPosition(null));
                    }
                }
  break;
case 372:
					// line 1513 "DefaultRubyParser.y"
  {
                    yyVal = new SymbolNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
                }
  break;
case 374:
					// line 1518 "DefaultRubyParser.y"
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
case 375:
					// line 1530 "DefaultRubyParser.y"
  {
                    yyVal = support.literal_concat(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), null, ((Node)yyVals[0+yyTop]));
		}
  break;
case 376:
					// line 1533 "DefaultRubyParser.y"
  {
                    yyVal = support.literal_concat(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
		}
  break;
case 377:
					// line 1537 "DefaultRubyParser.y"
  {
		     yyVal = ((Node)yyVals[-1+yyTop]);
		}
  break;
case 378:
					// line 1541 "DefaultRubyParser.y"
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
case 379:
					// line 1555 "DefaultRubyParser.y"
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
case 380:
					// line 1572 "DefaultRubyParser.y"
  {
		     yyVal = new ZArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])));
		 }
  break;
case 381:
					// line 1575 "DefaultRubyParser.y"
  {
		     yyVal = ((ListNode)yyVals[-1+yyTop]);
		 }
  break;
case 382:
					// line 1579 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 383:
					// line 1582 "DefaultRubyParser.y"
  {
                     Node node = ((Node)yyVals[-1+yyTop]);

                     if (node instanceof EvStrNode) {
		       node = new DStrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(node);
		     }

		     if (((ListNode)yyVals[-2+yyTop]) == null) {
		       yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(node);
		     } else {
		       yyVal = ((ListNode)yyVals[-2+yyTop]).add(node);
		     }
		 }
  break;
case 385:
					// line 1597 "DefaultRubyParser.y"
  {
                     yyVal = support.literal_concat(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
	         }
  break;
case 386:
					// line 1601 "DefaultRubyParser.y"
  {
		     yyVal = new ZArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])));
		 }
  break;
case 387:
					// line 1604 "DefaultRubyParser.y"
  {
		     yyVal = ((ListNode)yyVals[-1+yyTop]);
		 }
  break;
case 388:
					// line 1608 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 389:
					// line 1611 "DefaultRubyParser.y"
  {
                     if (((ListNode)yyVals[-2+yyTop]) == null) {
		         yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(new StrNode(((ISourcePositionHolder)yyVals[-1+yyTop]).getPosition(), (String) ((Token)yyVals[-1+yyTop]).getValue()));
		     } else {
		         yyVal = ((ListNode)yyVals[-2+yyTop]).add(new StrNode(((ISourcePositionHolder)yyVals[-1+yyTop]).getPosition(), (String) ((Token)yyVals[-1+yyTop]).getValue()));
		     }
		 }
  break;
case 390:
					// line 1619 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 391:
					// line 1622 "DefaultRubyParser.y"
  {
                     yyVal = support.literal_concat(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
		 }
  break;
case 392:
					// line 1626 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 393:
					// line 1629 "DefaultRubyParser.y"
  {
                     yyVal = support.literal_concat(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
		 }
  break;
case 394:
					// line 1634 "DefaultRubyParser.y"
  {
                      yyVal = new StrNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
                  }
  break;
case 395:
					// line 1637 "DefaultRubyParser.y"
  {
                      yyVal = lexer.getStrTerm();
		      lexer.setStrTerm(null);
		      lexer.setState(LexState.EXPR_BEG);
		  }
  break;
case 396:
					// line 1641 "DefaultRubyParser.y"
  {
		      lexer.setStrTerm(((StrTerm)yyVals[-1+yyTop]));
		      yyVal = new EvStrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop]));
		  }
  break;
case 397:
					// line 1645 "DefaultRubyParser.y"
  {
		      yyVal = lexer.getStrTerm();
		      lexer.setStrTerm(null);
		      lexer.setState(LexState.EXPR_BEG);
		  }
  break;
case 398:
					// line 1649 "DefaultRubyParser.y"
  {
		      lexer.setStrTerm(((StrTerm)yyVals[-2+yyTop]));
		      Node node = ((Node)yyVals[-1+yyTop]);

		      if (node instanceof NewlineNode) {
		        node = ((NewlineNode)node).getNextNode();
		      }

		      yyVal = support.newEvStrNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), node);
		  }
  break;
case 399:
					// line 1660 "DefaultRubyParser.y"
  {
                      yyVal = new GlobalVarNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                 }
  break;
case 400:
					// line 1663 "DefaultRubyParser.y"
  {
                      yyVal = new InstVarNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                 }
  break;
case 401:
					// line 1666 "DefaultRubyParser.y"
  {
                      yyVal = new ClassVarNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                 }
  break;
case 403:
					// line 1672 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    yyVal = ((Token)yyVals[0+yyTop]);
		    ((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])));
                }
  break;
case 408:
					// line 1683 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);

		    /* In ruby, it seems to be possible to get a*/
		    /* StrNode (NODE_STR) among other node type.  This */
		    /* is not possible for us.  We will always have a */
		    /* DStrNode (NODE_DSTR).*/
		    yyVal = new DSymbolNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((DStrNode)yyVals[-1+yyTop]));
		}
  break;
case 409:
					// line 1693 "DefaultRubyParser.y"
  {
                    Object number = ((Token)yyVals[0+yyTop]).getValue();

                    if (number instanceof Long) {
		        yyVal = new FixnumNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), ((Long) number).longValue());
                    } else {
		        yyVal = new BignumNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (BigInteger) number);
                    }
                }
  break;
case 410:
					// line 1702 "DefaultRubyParser.y"
  {
                    yyVal = new FloatNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), ((Double) ((Token)yyVals[0+yyTop]).getValue()).doubleValue());
	        }
  break;
case 411:
					// line 1705 "DefaultRubyParser.y"
  {
                    Object number = ((Token)yyVals[0+yyTop]).getValue();

                    yyVal = support.getOperatorCallNode((number instanceof Long ? (Node) new FixnumNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Long) number).longValue()) : (Node) new BignumNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), (BigInteger) number)), "-@");
		}
  break;
case 412:
					// line 1710 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(new FloatNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Double) ((Token)yyVals[0+yyTop]).getValue()).doubleValue()), "-@");
		}
  break;
case 413:
					// line 1719 "DefaultRubyParser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 414:
					// line 1722 "DefaultRubyParser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 415:
					// line 1725 "DefaultRubyParser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 416:
					// line 1728 "DefaultRubyParser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 417:
					// line 1731 "DefaultRubyParser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 418:
					// line 1734 "DefaultRubyParser.y"
  { 
                    yyVal = new NilNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
                }
  break;
case 419:
					// line 1737 "DefaultRubyParser.y"
  {
                    yyVal = new SelfNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
                }
  break;
case 420:
					// line 1740 "DefaultRubyParser.y"
  { 
		    yyVal = new TrueNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
                }
  break;
case 421:
					// line 1743 "DefaultRubyParser.y"
  {
		    yyVal = new FalseNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
                }
  break;
case 422:
					// line 1746 "DefaultRubyParser.y"
  {
                    yyVal = new StrNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), getPosition(((ISourcePositionHolder)yyVals[0+yyTop])).getFile());
                }
  break;
case 423:
					// line 1749 "DefaultRubyParser.y"
  {
                    yyVal = new FixnumNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), getPosition(((ISourcePositionHolder)yyVals[0+yyTop])).getEndLine() + 1);
                }
  break;
case 424:
					// line 1753 "DefaultRubyParser.y"
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
case 425:
					// line 1766 "DefaultRubyParser.y"
  {
                    yyVal = support.assignable(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), yyVals[0+yyTop], null);
                }
  break;
case 428:
					// line 1773 "DefaultRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 429:
					// line 1776 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 430:
					// line 1778 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 431:
					// line 1781 "DefaultRubyParser.y"
  {
                    yyerrok();
                    yyVal = null;
                }
  break;
case 432:
					// line 1786 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-2+yyTop]);
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 433:
					// line 1790 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 434:
					// line 1794 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 435:
					// line 1797 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 436:
					// line 1800 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), null, ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 437:
					// line 1803 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(((ISourcePositionHolder)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 438:
					// line 1806 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), null, ((ListNode)yyVals[-3+yyTop]), ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 439:
					// line 1809 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), null, ((ListNode)yyVals[-1+yyTop]), -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 440:
					// line 1812 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), null, null, ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 441:
					// line 1815 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), null, null, -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 442:
					// line 1818 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(getPosition(null), null, null, -1, null);
                }
  break;
case 443:
					// line 1822 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be a constant");
                }
  break;
case 444:
					// line 1825 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be an instance variable");
                }
  break;
case 445:
					// line 1828 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be a class variable");
                }
  break;
case 446:
					// line 1831 "DefaultRubyParser.y"
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
case 447:
					// line 1843 "DefaultRubyParser.y"
  {
                    yyVal = new ListNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
                    ((ListNode) yyVal).add(new ArgumentNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue()));
                }
  break;
case 448:
					// line 1847 "DefaultRubyParser.y"
  {
                    ((ListNode)yyVals[-2+yyTop]).add(new ArgumentNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue()));
                    ((ListNode)yyVals[-2+yyTop]).setPosition(support.union(((ListNode)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
		    yyVal = ((ListNode)yyVals[-2+yyTop]);
                }
  break;
case 449:
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
case 450:
					// line 1865 "DefaultRubyParser.y"
  {
                    yyVal = new BlockNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 451:
					// line 1868 "DefaultRubyParser.y"
  {
                    yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 454:
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
case 455:
					// line 1886 "DefaultRubyParser.y"
  {
                    ((Token)yyVals[0+yyTop]).setValue(new Integer(-2));
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 458:
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
case 459:
					// line 1905 "DefaultRubyParser.y"
  {
                    yyVal = ((BlockArgNode)yyVals[0+yyTop]);
                }
  break;
case 460:
					// line 1908 "DefaultRubyParser.y"
  {
	            yyVal = null;
	        }
  break;
case 461:
					// line 1912 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[0+yyTop]) instanceof SelfNode) {
		        yyVal = new SelfNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
                    } else {
			support.checkExpression(((Node)yyVals[0+yyTop]));
			yyVal = ((Node)yyVals[0+yyTop]);
		    }
                }
  break;
case 462:
					// line 1920 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 463:
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
case 465:
					// line 1939 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                }
  break;
case 466:
					// line 1942 "DefaultRubyParser.y"
  {
                    if (((ListNode)yyVals[-1+yyTop]).size() % 2 != 0) {
                        yyerror("Odd number list for Hash.");
                    }
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                }
  break;
case 468:
					// line 1950 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).addAll(((ListNode)yyVals[0+yyTop]));
                }
  break;
case 469:
					// line 1954 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]))).add(((Node)yyVals[-2+yyTop])).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 489:
					// line 1984 "DefaultRubyParser.y"
  {
                    yyerrok();
                }
  break;
case 492:
					// line 1990 "DefaultRubyParser.y"
  {
                    yyerrok();
                }
  break;
case 493:
					// line 1994 "DefaultRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 494:
					// line 1998 "DefaultRubyParser.y"
  {  yyVal = null;
		  }
  break;
case 495:
					// line 2001 "DefaultRubyParser.y"
  {  yyVal = null;
		  }
  break;
					// line 7650 "-"
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
					// line 7745 "-"
