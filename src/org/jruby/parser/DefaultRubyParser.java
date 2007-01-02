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
 * Copyright (C) 2004-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
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
import org.jruby.ast.SelfNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.StarNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.ToAryNode;
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
					// line 148 "-"
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
  public static final int tNTH_REF = 310;
  public static final int tBACK_REF = 311;
  public static final int tSTRING_CONTENT = 312;
  public static final int tINTEGER = 313;
  public static final int tFLOAT = 314;
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
  public static final int tRPAREN = 343;
  public static final int tLPAREN_ARG = 344;
  public static final int tLBRACK = 345;
  public static final int tRBRACK = 346;
  public static final int tLBRACE = 347;
  public static final int tLBRACE_ARG = 348;
  public static final int tSTAR = 349;
  public static final int tSTAR2 = 350;
  public static final int tAMPER = 351;
  public static final int tAMPER2 = 352;
  public static final int tTILDE = 353;
  public static final int tPERCENT = 354;
  public static final int tDIVIDE = 355;
  public static final int tPLUS = 356;
  public static final int tMINUS = 357;
  public static final int tLT = 358;
  public static final int tGT = 359;
  public static final int tPIPE = 360;
  public static final int tBANG = 361;
  public static final int tCARET = 362;
  public static final int tLCURLY = 363;
  public static final int tRCURLY = 364;
  public static final int tBACK_REF2 = 365;
  public static final int tSYMBEG = 366;
  public static final int tSTRING_BEG = 367;
  public static final int tXSTRING_BEG = 368;
  public static final int tREGEXP_BEG = 369;
  public static final int tWORDS_BEG = 370;
  public static final int tQWORDS_BEG = 371;
  public static final int tSTRING_DBEG = 372;
  public static final int tSTRING_DVAR = 373;
  public static final int tSTRING_END = 374;
  public static final int tLOWEST = 375;
  public static final int tLAST_TOKEN = 376;
  public static final int yyErrorCode = 256;

  /** number of final state.
    */
  protected static final int yyFinal = 1;

  /** parser tables.
      Order is mandated by <i>jay</i>.
    */
  protected static final short[] yyLhs = {
//yyLhs 493
    -1,    99,     0,    20,    19,    21,    21,    21,    21,   102,
    22,    22,    22,    22,    22,    22,    22,    22,    22,    22,
   103,    22,    22,    22,    22,    22,    22,    22,    22,    22,
    22,    22,    22,    22,    22,    23,    23,    23,    23,    23,
    23,    27,    18,    18,    18,    18,    18,    43,    43,    43,
   104,    78,    26,    26,    26,    26,    26,    26,    26,    26,
    79,    79,    81,    81,    80,    80,    80,    80,    80,    80,
    51,    51,    67,    67,    52,    52,    52,    52,    52,    52,
    52,    52,    59,    59,    59,    59,    59,    59,    59,    59,
    91,    91,    17,    17,    17,    92,    92,    92,    92,    92,
    85,    85,    47,   106,    47,    93,    93,    93,    93,    93,
    93,    93,    93,    93,    93,    93,    93,    93,    93,    93,
    93,    93,    93,    93,    93,    93,    93,    93,    93,    93,
    93,   105,   105,   105,   105,   105,   105,   105,   105,   105,
   105,   105,   105,   105,   105,   105,   105,   105,   105,   105,
   105,   105,   105,   105,   105,   105,   105,   105,   105,   105,
   105,   105,   105,   105,   105,   105,   105,   105,   105,   105,
   105,   105,    24,    24,    24,    24,    24,    24,    24,    24,
    24,    24,    24,    24,    24,    24,    24,    24,    24,    24,
    24,    24,    24,    24,    24,    24,    24,    24,    24,    24,
    24,    24,    24,    24,    24,    24,    24,    24,    24,    24,
    24,    24,    24,    24,    24,    24,    53,    56,    56,    56,
    56,    56,    56,    37,    37,    37,    37,    41,    41,    33,
    33,    33,    33,    33,    33,    33,    33,    33,    34,    34,
    34,    34,    34,    34,    34,    34,    34,    34,    34,    34,
   109,    39,    35,   110,    35,   111,    35,    72,    71,    71,
    65,    65,    50,    50,    50,    25,    25,    25,    25,    25,
    25,    25,    25,    25,    25,   112,    25,    25,    25,    25,
    25,    25,    25,    25,    25,    25,    25,    25,    25,    25,
    25,    25,    25,   114,   116,    25,   117,   118,    25,    25,
    25,    25,   119,   120,    25,   121,    25,   123,   124,    25,
   125,    25,   126,    25,   127,   128,    25,    25,    25,    25,
    25,    28,   113,   113,   113,   113,   115,   115,   115,    31,
    31,    29,    29,    57,    57,    58,    58,    58,    58,   129,
    77,    42,    42,    42,    10,    10,    10,    10,    10,    10,
   130,    76,   131,    76,    54,    66,    66,    66,    30,    30,
    82,    82,    55,    55,    55,    32,    32,    36,    36,    14,
    14,    14,     2,     3,     3,     4,     5,     6,    11,    11,
    62,    62,    13,    13,    12,    12,    61,    61,     7,     7,
     8,     8,     9,   132,     9,   133,     9,    48,    48,    48,
    48,    87,    86,    86,    86,    86,    16,    15,    15,    15,
    15,    84,    84,    84,    84,    84,    84,    84,    84,    84,
    84,    84,    40,    83,    49,    49,    38,   134,    38,    38,
    44,    44,    45,    45,    45,    45,    45,    45,    45,    45,
    45,    94,    94,    94,    94,    63,    63,    46,    64,    64,
    97,    97,    95,    95,    98,    98,    75,    74,    74,     1,
   135,     1,    70,    70,    70,    68,    68,    69,    88,    88,
    88,    89,    89,    89,    89,    90,    90,    90,    96,    96,
   100,   100,   107,   107,   108,   108,   108,   122,   122,   101,
   101,    60,    73,
    }, yyLen = {
//yyLen 493
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
     3,     3,     3,     3,     5,     1,     1,     1,     2,     2,
     5,     2,     3,     3,     4,     4,     6,     1,     1,     1,
     2,     5,     2,     5,     4,     7,     3,     1,     4,     3,
     5,     7,     2,     5,     4,     6,     7,     9,     3,     1,
     0,     2,     1,     0,     3,     0,     4,     2,     2,     1,
     1,     3,     3,     4,     2,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     3,     0,     5,     3,     3,     2,
     4,     3,     3,     1,     4,     3,     1,     5,     2,     1,
     2,     6,     6,     0,     0,     7,     0,     0,     7,     5,
     4,     5,     0,     0,     9,     0,     6,     0,     0,     8,
     0,     5,     0,     6,     0,     0,     9,     1,     1,     1,
     1,     1,     1,     1,     1,     2,     1,     1,     1,     1,
     5,     1,     2,     1,     1,     1,     2,     1,     3,     0,
     5,     2,     4,     4,     2,     4,     4,     3,     2,     1,
     0,     5,     0,     5,     5,     1,     4,     2,     1,     1,
     6,     0,     1,     1,     1,     2,     1,     2,     1,     1,
     1,     1,     1,     1,     2,     3,     3,     3,     3,     3,
     0,     3,     1,     2,     3,     3,     0,     3,     0,     2,
     0,     2,     1,     0,     3,     0,     4,     1,     1,     1,
     1,     2,     1,     1,     1,     1,     3,     1,     1,     2,
     2,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     0,     4,     2,
     4,     2,     6,     4,     4,     2,     4,     2,     2,     1,
     0,     1,     1,     1,     1,     1,     3,     3,     1,     3,
     1,     1,     2,     1,     1,     1,     2,     2,     0,     1,
     0,     5,     1,     2,     2,     1,     3,     3,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     0,     1,     0,     1,     0,     1,     1,     1,     1,     1,
     2,     0,     0,
    }, yyDefRed = {
//yyDefRed 884
     1,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   293,   296,     0,     0,     0,   319,   320,     0,
     0,     0,   417,   416,   418,   419,     0,     0,     0,    20,
     0,   421,   420,     0,     0,   413,   412,     0,   415,   424,
   425,   407,   408,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   388,   390,   390,     0,     0,
   266,     0,   373,   267,   268,     0,   269,   270,   265,   369,
   371,    35,     2,     0,     0,     0,     0,     0,     0,     0,
   271,     0,    43,     0,     0,    70,     0,     5,     0,     0,
    60,     0,     0,   370,     0,     0,   317,   318,   283,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   321,
     0,   272,   422,     0,    93,   310,   140,   151,   141,   164,
   137,   157,   147,   146,   162,   145,   144,   139,   165,   149,
   138,   152,   156,   158,   150,   143,   159,   166,   161,     0,
     0,     0,     0,   136,   155,   154,   167,   168,   169,   170,
   171,   135,   142,   133,   134,     0,     0,     0,    97,     0,
   126,   127,   124,   108,   109,   110,   113,   115,   111,   128,
   129,   116,   117,   460,   121,   120,   107,   125,   123,   122,
   118,   119,   114,   112,   105,   106,   130,     0,   459,   312,
    98,    99,   160,   153,   163,   148,   131,   132,    95,    96,
     0,     0,   102,   101,   100,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   488,   487,     0,     0,
     0,   489,     0,     0,     0,     0,     0,     0,   333,   334,
     0,     0,     0,     0,     0,   229,    45,     0,     0,     0,
   465,   237,    46,    44,     0,    59,     0,     0,   348,    58,
    38,     0,     9,   483,     0,     0,     0,   192,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   217,     0,     0,   462,     0,     0,     0,     0,     0,
     0,    68,     0,   208,    39,   207,   404,   403,   405,     0,
   401,   402,     0,     0,     0,     0,     0,     0,     0,   374,
   352,   350,   290,     4,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   339,   341,
     0,     0,     0,     0,     0,     0,    72,     0,     0,     0,
     0,     0,     0,   344,     0,   288,     0,   409,   410,     0,
    90,     0,    92,     0,   427,   305,   426,     0,     0,     0,
     0,     0,   478,   479,   314,     0,   103,     0,     0,   274,
     0,   324,   323,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   490,     0,     0,     0,
     0,     0,     0,   302,     0,   257,     0,     0,   230,   259,
     0,   232,   285,     0,     0,   252,   251,     0,     0,     0,
     0,     0,    11,    13,    12,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   277,     0,     0,     0,
   218,   281,     0,   485,   219,     0,   221,     0,   464,   463,
   282,     0,     0,     0,     0,   392,   395,   393,   406,   391,
   375,   389,   376,   377,   378,   379,   382,     0,   384,     0,
   385,     0,     0,     0,    15,    16,    17,    18,    19,    36,
    37,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   473,
     0,     0,   474,     0,     0,     0,     0,   347,     0,     0,
   471,   472,     0,     0,    30,     0,     0,    23,     0,    31,
   260,     0,     0,    66,    73,    24,    33,     0,    25,     0,
    50,    53,     0,   429,     0,     0,     0,     0,     0,     0,
    94,     0,     0,     0,     0,     0,   442,   441,   443,     0,
   451,   450,   455,   454,     0,     0,   448,     0,     0,   439,
   445,     0,     0,     0,     0,   363,     0,     0,   364,     0,
     0,   331,     0,   325,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   300,   328,   327,   294,
   326,   297,     0,     0,     0,     0,     0,     0,     0,   236,
   467,     0,     0,     0,   258,     0,     0,   466,   284,     0,
     0,   255,     0,     0,   249,     0,     0,     0,     0,     0,
   223,     0,    10,     0,     0,    22,     0,     0,     0,     0,
     0,   222,     0,   261,     0,     0,     0,     0,     0,     0,
     0,   381,   383,   387,   337,     0,     0,   335,     0,     0,
     0,     0,     0,   228,     0,   345,   227,     0,     0,   346,
     0,     0,    48,   342,    49,   343,   264,     0,     0,    71,
     0,   308,     0,     0,   280,   311,     0,   315,     0,     0,
     0,   431,     0,   435,     0,   437,     0,   438,   452,   456,
   104,     0,     0,   366,   332,     0,     3,   368,     0,   329,
     0,     0,     0,     0,     0,     0,   299,   301,   357,     0,
     0,     0,     0,     0,     0,     0,     0,   234,     0,     0,
     0,     0,     0,   242,   254,   224,     0,     0,   225,     0,
     0,   287,    21,   276,     0,     0,     0,   397,   398,   399,
   394,   400,   336,     0,     0,     0,     0,     0,    27,     0,
    28,     0,    55,    29,     0,     0,    57,     0,     0,     0,
     0,     0,     0,   428,   306,   461,     0,   447,     0,   313,
     0,   457,   446,     0,     0,   449,     0,     0,     0,     0,
   365,     0,     0,   367,     0,   291,     0,   292,     0,     0,
     0,     0,   303,   231,     0,   233,   248,   256,     0,     0,
     0,   239,     0,     0,   220,   396,   338,   353,   351,   340,
    26,     0,   263,     0,     0,     0,   430,     0,   433,   434,
   436,     0,     0,     0,     0,     0,     0,   356,   358,   354,
   359,   295,   298,     0,     0,     0,     0,   238,     0,   244,
     0,   226,    51,   309,     0,     0,     0,     0,     0,     0,
     0,   360,     0,     0,   235,   240,     0,     0,     0,   243,
   316,   432,     0,   330,   304,     0,     0,   245,     0,   241,
     0,   246,     0,   247,
    }, yyDgoto = {
//yyDgoto 136
     1,   187,    60,    61,    62,    63,    64,   292,   289,   459,
    65,    66,    67,   467,    68,    69,    70,   108,    71,   205,
   206,    73,    74,    75,    76,    77,    78,   209,   258,   709,
   839,   710,   702,   236,   621,   416,   706,   663,   365,   245,
    80,   665,    81,    82,   564,   565,   566,   201,   750,   211,
   529,    84,    85,   237,   395,   577,   270,   227,   656,   212,
    87,   298,   296,   567,   568,   272,   595,    88,   273,   240,
   277,   408,   614,   409,   693,   781,   355,   339,   541,    89,
    90,   266,   378,   213,   214,   202,   290,    93,   113,   546,
   517,   114,   204,   512,   570,   571,   374,   572,   573,     2,
   219,   220,   425,   255,   680,   191,   574,   254,   444,   246,
   625,   730,   438,   383,   222,   599,   721,   223,   722,   607,
   843,   545,   384,   542,   772,   370,   375,   554,   776,   507,
   472,   471,   650,   649,   544,   371,
    }, yySindex = {
//yySindex 884
     0,     0,  3681,  5119, 16053, 16398, 16963, 16853,  3681,  4675,
  4675,  4558,     0,     0, 16168, 13523, 13523,     0,     0, 13523,
  -295,  -286,     0,     0,     0,     0,  4675, 16743,    62,     0,
  -267,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0, 15823, 15823,   -86,  -111,  4161,  4675, 14903,
 15823, 16513, 15823, 15938, 17072,     0,     0,     0,   187,   204,
     0,  -125,     0,     0,     0,  -244,     0,     0,     0,     0,
     0,     0,     0,    55,  1056,   179,  2741,     0,   -16,   -46,
     0,  -159,     0,   -59,   243,     0,   232,     0, 16283,   270,
     0,    -4,     0,     0,  -209,  1056,     0,     0,     0,  -295,
  -286,    62,     0,     0,   -36,  4675,   -74,  3681,    39,     0,
   129,     0,     0,  -209,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  -142,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
 17072,   289,     0,     0,     0,    77,   172,    48,   179,    83,
   156,   107,   394,   123,     0,    83,     0,     0,    55,  -184,
   409,     0,  4675,  4675,   152,   170,     0,   193,     0,     0,
     0, 15823, 15823, 15823,  2741,     0,     0,   140,   459,   482,
     0,     0,     0,     0, 13293,     0, 13638, 13523,     0,     0,
     0,   212,     0,     0,  5644,   120,  3681,     0,   205,   218,
   225,   235,   219,  4161,   217,     0,   237,   179, 15823,    62,
   186,     0,    60,    76,     0,   118,    76,   211,   277,   439,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  -199,
     0,     0,  -190,  -162,   318,   220,  -110,   223,  -234,     0,
     0,     0,     0,     0, 13178,  4675,  4675,  4675,  4675,  5119,
  4675,  4675, 15823, 15823, 15823, 15823, 15823, 15823, 15823, 15823,
 15823, 15823, 15823, 15823, 15823, 15823, 15823, 15823, 15823, 15823,
 15823, 15823, 15823, 15823, 15823, 15823, 15823, 15823,     0,     0,
  5712,  9042, 14903, 17240, 17240, 15938,     0, 15018,  4161, 16513,
   559, 15018, 15938,     0,   239,     0,   267,     0,     0,   179,
     0,     0,     0,    55,     0,     0,     0, 17240, 17299, 14903,
  3681,  4675,     0,     0,     0,   718,     0, 15133,   344,     0,
   219,     0,     0,  3681,   354, 17358, 17417, 14903, 15823, 15823,
 15823,  3681,   352,  3681, 15248,   362,     0,    80,    80,     0,
 17476, 17535, 14903,     0,   587,     0, 15823, 13753,     0,     0,
 13868,     0,     0,   292, 13408,     0,     0,   -16,    62,    15,
   294,   595,     0,     0,     0, 16853,  4675,  2741,  3681,   285,
 17358, 17417, 15823, 15823, 15823,   308,     0,     0,    62,  1669,
     0,     0, 15363,     0,     0, 15823,     0, 15823,     0,     0,
     0,     0, 17594, 17653, 14903,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,    29,     0,   636,
     0,  -166,  -166,  1056,     0,     0,     0,     0,     0,     0,
     0,   218,  1727,  1727,  1727,  1727,  1512,  1512,  8470,  2685,
  1727,  1727,  2205,  2205,   631,   631,   218,   826,   218,   218,
   286,   286,  1512,  1512,  1427,  1427,  4968,  -166,   322,     0,
   337,  -286,     0,   338,     0,   339,  -286,     0,     0,   341,
     0,     0,  -286,  -286,     0,  2741, 15823,     0,  2261,     0,
     0,   645,   349,     0,     0,     0,     0,     0,     0,  2741,
     0,     0,    55,     0,  4675,  3681,  -286,     0,     0,  -286,
     0,   353,   437,   154, 17181,   641,     0,     0,     0,  1449,
     0,     0,     0,     0,  3681,    55,     0,   659,   663,     0,
     0,   666,   411,   420, 16853,     0,     0,   388,     0,  3681,
   469,     0,   226,     0,   398,   399,   405,   339,   387,  2261,
   344,   483,   485, 15823,   702,    83,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   404,  4675,   402,     0,
     0, 15823,   140,   712,     0, 15823,   140,     0,     0, 15823,
  2741,     0,    -2,   717,     0,   421,   427, 17240, 17240,   431,
     0, 13983,     0,  -147,   413,     0,   218,   218,  2741,     0,
   436,     0, 15823,     0,     0,     0,     0,     0,   445,  3681,
   986,     0,     0,     0,     0,  3235,  3681,     0,  3681, 15823,
  3681, 15938, 15938,     0,   239,     0,     0, 15938, 15823,     0,
   239,   434,     0,     0,     0,     0,     0, 15823, 15478,     0,
  -166,     0,    55,   528,     0,     0,   453,     0, 15823,    62,
   536,     0,  1449,     0,   654,     0,   215,     0,     0,     0,
     0, 16628,    83,     0,     0,  3681,     0,     0,  4675,     0,
   538, 15823, 15823, 15823,   465,   542,     0,     0,     0, 15593,
  3681,  3681,  3681,     0,    80,   587, 14098,     0,   587,   587,
   466, 14213, 14328,     0,     0,     0,  -286,  -286,     0,   -16,
    15,     0,     0,     0,  1669,     0,   444,     0,     0,     0,
     0,     0,     0,   461,   558,   468,  2741,   570,     0,  2741,
     0,  2741,     0,     0,  2741,  2741,     0, 15938,  2741, 15823,
     0,  3681,  3681,     0,     0,     0,   718,     0,   493,     0,
   794,     0,     0,   666,   641,     0,   666,   531,   464,     0,
     0,     0,  3681,     0,    83,     0, 15823,     0, 15823,  -172,
   577,   585,     0,     0, 15823,     0,     0,     0, 15823,   811,
   813,     0, 15823,   516,     0,     0,     0,     0,     0,     0,
     0,  2741,     0,   496,   598,  3681,     0,   654,     0,     0,
     0,     0, 17712, 17771, 14903,    77,  3681,     0,     0,     0,
     0,     0,     0,  3681,  4010,   587, 14443,     0, 14558,     0,
   587,     0,     0,     0,   602,   666,     0,     0,     0,     0,
   521,     0,   226,   604,     0,     0, 15823,   835, 15823,     0,
     0,     0,     0,     0,     0,   587, 14673,     0,   587,     0,
 15823,     0,   587,     0,
    }, yyRindex = {
//yyRindex 884
     0,     0,   167,     0,     0,     0,     0,     0,   566,     0,
     0,  -145,     0,     0,     0,  7401,  7506,     0,     0,  7615,
  4432,  3837,     0,     0,     0,     0,     0,     0, 15708,     0,
     0,     0,     0,  1917,  2992,     0,     0,  2032,     0,     0,
     0,     0,     0,     0,     0,     0,     0,    57,     0,   537,
   522,   105,     0,     0,   190,     0,     0,     0,   245,  -223,
     0,  6697,     0,     0,     0,  6802,     0,     0,     0,     0,
     0,     0,     0,   708,   213,  2630,  8683,  6911, 12063,     0,
     0, 12108,     0,  8665,     0,     0,     0,     0,   222,     0,
     0,     0,  8548,     0, 14788,   265,     0,     0,     0,  7050,
  5832,   551, 12242, 12363,     0,     0,     0,    57,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   919,
  1802,  2102,  2159,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  2639,  3119,  3541,     0,  3552,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  3110,     0,     0,     0,   351,     0,     0, 12149,     0,
     0,  6346,     0,     0,  5935,     0,     0,     0,   618,     0,
   -71,     0,     0,     0,     0,     0,   547,     0,     0,     0,
   759,     0,     0,     0, 10949,     0,     0, 11797,  6286,  6286,
     0,     0,     0,     0,     0,     0,     0,   555,     0,     0,
     0,     0,     0,     0,     0,     0,    16,     0,     0,  7735,
  7153,  7264,  8774,    57,     0,    33,     0,    20,     0,   563,
     0,     0,   564,   564,     0,   541,   541,     0,     0,     0,
   363,     0,  1338,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   842,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   537,     0,     0,     0,     0,     0,    57,   435,
   520,     0,     0,     0,  1619,     0,     0,     0,     0,    98,
     0, 12713,     0,     0,     0,     0,     0,     0,     0,   537,
   566,     0,     0,     0,     0,   150,     0,   121,   401,     0,
  6449,     0,     0,   676, 12829,     0,     0,   537,     0,     0,
     0,    59,     0,   142,     0,     0,     0,     0,     0,   830,
     0,     0,   537,     0,  6286,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   568,     0,     0,    43,   571,   571,
     0,    53,     0,     0,     0,     0,     0, 11038,    16,     0,
     0,     0,     0,     0,     0,     0,     0,   146,   571,   563,
     0,     0,   574,     0,     0,  -143,     0,   552,     0,     0,
     0,  1473,     0,     0,   537,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 12947, 13063,   854,     0,     0,     0,     0,     0,     0,
     0,  7844, 10177, 10267, 10370, 10469,  9734,  9838, 10559, 10824,
 10646, 10735,  6183, 10911,  9279,  9383,  7949,  9494,  8086,  8197,
  8972,  9157,  9960, 10078,  1776,  9623,     0, 12947,  4795,     0,
  4910,  3952,     0,  5275,  3357,  5390, 14788,     0,  3472,     0,
     0,     0,  5517,  5517,     0, 11079,     0,     0,   878,     0,
     0,     0,     0,     0,     0,     0,     0,  1561,     0, 11166,
     0,     0,     0,     0,     0,   566,  6039, 12479, 12597,     0,
     0,     0,     0,   571,     0,    73,     0,     0,     0,    70,
     0,     0,     0,     0,   566,     0,     0,    97,    97,     0,
     0,    97,    90,     0,     0,     0,   111,   100,     0,   189,
   661,     0,   661,     0,  2397,  2512,  2877,  4317,     0, 11833,
   661,     0,     0,     0,   127,     0,     0,     0,     0,     0,
     0,     0,   178,  1046,  1600,   430,     0,     0,     0,     0,
     0,     0, 11924,  6286,     0,     0,     0,     0,     0,     0,
    91,     0,     0,   580,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,  8300,  8423, 11226,    92,
     0,     0,     0,     0,  1329,  1363,  1591,   874,     0,    16,
     0,     0,     0,     0,     0,     0,   142,     0,    16,     0,
   142,     0,     0,     0, 11960,     0,     0,     0,     0,     0,
 12023,  8850,     0,     0,     0,     0,     0,     0,     0,     0,
 13063,     0,     0,     0,     0,     0,     0,     0,     0,   571,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   142,     0,     0,     0,     0,
     0,     0,     0,     0,  6560,     0,     0,     0,     0,     0,
   543,   142,   142,   873,     0,  6286,     0,     0,  6286,   580,
     0,     0,     0,     0,     0,     0,    14,    14,     0,     0,
   571,     0,     0,     0,   563,  2086,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0, 11264,     0,     0, 11352,
     0, 11394,     0,     0, 11481, 11541,     0,     0, 11650,     0,
  1445,    16,   566,     0,     0,     0,   150,     0,     0,     0,
    97,     0,     0,    97,     0,     0,    97,     0,     0,   481,
     0,   515,   566,     0,     0,     0,     0,     0,     0,   661,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   580,
   580,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 11710,     0,     0,     0,   566,     0,     0,     0,     0,
     0,   765,     0,     0,   537,   351,   676,     0,     0,     0,
     0,     0,     0,   142,  6286,   580,     0,     0,     0,     0,
   580,     0,     0,     0,     0,    97,   985,  1248,  1288,   182,
     0,     0,   661,     0,     0,     0,     0,   580,     0,     0,
     0,     0,   833,     0,     0,   580,     0,     0,   580,     0,
     0,     0,   580,     0,
    }, yyGindex = {
//yyGindex 136
     0,     0,     0,     0,   865,     0,     0,     0,   615,  -120,
     0,     0,     0,     0,     0,     0,     0,   922,    13,   928,
  -358,     0,   106,  1189,   -15,     4,    35,    63,   241,  -357,
     0,    66,     0,   629,     0,     0,     0,   -18,     0,    -6,
   925,   134,  -228,     0,   158,   379,  -646,     0,     0,   680,
  -186,   853,   -24,  1280,  -376,     0,  -325,   293,  -426,   892,
   903,     0,     0,     0,   255,    96,     0,     0,    -9,  -377,
     0,    37,    24,     0,    30,  -355,   886,     0,  -456,    -1,
    11,  -203,   124,  1131,   737,   -13,     0,    -5,   710,  -238,
     0,   -42,     5,    25,   263,  -569,     0,     0,     0,     0,
   -50,   887,     0,     0,     0,     0,     0,   -10,   295,     0,
     0,     0,     0,  -210,     0,  -341,     0,     0,     0,     0,
     0,     0,   116,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,
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
    "kCLASS","kMODULE","kDEF","kUNDEF","kBEGIN","kRESCUE","kENSURE",
    "kEND","kIF","kUNLESS","kTHEN","kELSIF","kELSE","kCASE","kWHEN",
    "kWHILE","kUNTIL","kFOR","kBREAK","kNEXT","kREDO","kRETRY","kIN",
    "kDO","kDO_COND","kDO_BLOCK","kRETURN","kYIELD","kSUPER","kSELF",
    "kNIL","kTRUE","kFALSE","kAND","kOR","kNOT","kIF_MOD","kUNLESS_MOD",
    "kWHILE_MOD","kUNTIL_MOD","kRESCUE_MOD","kALIAS","kDEFINED","klBEGIN",
    "klEND","k__LINE__","k__FILE__","tIDENTIFIER","tFID","tGVAR","tIVAR",
    "tCONSTANT","tCVAR","tNTH_REF","tBACK_REF","tSTRING_CONTENT",
    "tINTEGER","tFLOAT","tREGEXP_END","tUPLUS","tUMINUS","tUMINUS_NUM",
    "tPOW","tCMP","tEQ","tEQQ","tNEQ","tGEQ","tLEQ","tANDOP","tOROP",
    "tMATCH","tNMATCH","tDOT","tDOT2","tDOT3","tAREF","tASET","tLSHFT",
    "tRSHFT","tCOLON2","tCOLON3","tOP_ASGN","tASSOC","tLPAREN","tLPAREN2",
    "tRPAREN","tLPAREN_ARG","tLBRACK","tRBRACK","tLBRACE","tLBRACE_ARG",
    "tSTAR","tSTAR2","tAMPER","tAMPER2","tTILDE","tPERCENT","tDIVIDE",
    "tPLUS","tMINUS","tLT","tGT","tPIPE","tBANG","tCARET","tLCURLY",
    "tRCURLY","tBACK_REF2","tSYMBEG","tSTRING_BEG","tXSTRING_BEG",
    "tREGEXP_BEG","tWORDS_BEG","tQWORDS_BEG","tSTRING_DBEG",
    "tSTRING_DVAR","tSTRING_END","tLOWEST","tLAST_TOKEN",
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
					// line 256 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_BEG);
                  support.initTopLocalVariables();
              }
  break;
case 2:
					// line 259 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[0+yyTop]) != null) {
                      /* last expression should not be void */
                      if (((Node)yyVals[0+yyTop]) instanceof BlockNode) {
                          support.checkUselessStatement(((BlockNode)yyVals[0+yyTop]).getLast());
                      } else {
                          support.checkUselessStatement(((Node)yyVals[0+yyTop]));
                      }
                  }
                  support.getResult().setAST(support.addRootNode(((Node)yyVals[0+yyTop])));
              }
  break;
case 3:
					// line 271 "DefaultRubyParser.y"
  {
                  Node node = ((Node)yyVals[-3+yyTop]);

		  if (((RescueBodyNode)yyVals[-2+yyTop]) != null) {
		      node = new RescueNode(getPosition(((Node)yyVals[-3+yyTop]), true), ((Node)yyVals[-3+yyTop]), ((RescueBodyNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
		  } else if (((Node)yyVals[-1+yyTop]) != null) {
		      warnings.warn(getPosition(((Node)yyVals[-3+yyTop])), "else without rescue is useless");
                      node = support.appendToBlock(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
		  }
		  if (((Node)yyVals[0+yyTop]) != null) {
		      node = new EnsureNode(getPosition(((Node)yyVals[-3+yyTop])), node, ((Node)yyVals[0+yyTop]));
		  }

	          yyVal = node;
              }
  break;
case 4:
					// line 287 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-1+yyTop]) instanceof BlockNode) {
                      support.checkUselessStatements(((BlockNode)yyVals[-1+yyTop]));
		  }
                  yyVal = ((Node)yyVals[-1+yyTop]);
              }
  break;
case 6:
					// line 295 "DefaultRubyParser.y"
  {
                  yyVal = support.newline_node(((Node)yyVals[0+yyTop]), getPosition(((Node)yyVals[0+yyTop]), true));
              }
  break;
case 7:
					// line 298 "DefaultRubyParser.y"
  {
	          yyVal = support.appendToBlock(((Node)yyVals[-2+yyTop]), support.newline_node(((Node)yyVals[0+yyTop]), getPosition(((Node)yyVals[-2+yyTop]), true)));
              }
  break;
case 8:
					// line 301 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 9:
					// line 305 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_FNAME);
              }
  break;
case 10:
					// line 307 "DefaultRubyParser.y"
  {
                  yyVal = new AliasNode(getPosition(((Token)yyVals[-3+yyTop])), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 11:
					// line 310 "DefaultRubyParser.y"
  {
                  yyVal = new VAliasNode(getPosition(((Token)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 12:
					// line 313 "DefaultRubyParser.y"
  {
                  yyVal = new VAliasNode(getPosition(((Token)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), "$" + ((BackRefNode)yyVals[0+yyTop]).getType()); /* XXX*/
              }
  break;
case 13:
					// line 316 "DefaultRubyParser.y"
  {
                  yyerror("can't make alias for the number variables");
              }
  break;
case 14:
					// line 319 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 15:
					// line 322 "DefaultRubyParser.y"
  {
                  yyVal = new IfNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), null);
              }
  break;
case 16:
					// line 325 "DefaultRubyParser.y"
  {
                  yyVal = new IfNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), null, ((Node)yyVals[-2+yyTop]));
              }
  break;
case 17:
					// line 328 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                      yyVal = new WhileNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                  } else {
                      yyVal = new WhileNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                  }
              }
  break;
case 18:
					// line 335 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                      yyVal = new UntilNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode());
                  } else {
                      yyVal = new UntilNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]));
                  }
              }
  break;
case 19:
					// line 342 "DefaultRubyParser.y"
  {
	          yyVal = new RescueNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), new RescueBodyNode(getPosition(((Node)yyVals[-2+yyTop])), null,((Node)yyVals[0+yyTop]), null), null);
              }
  break;
case 20:
					// line 345 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
                      yyerror("BEGIN in method");
                  }
		  support.pushLocalScope();
              }
  break;
case 21:
					// line 350 "DefaultRubyParser.y"
  {
                  support.getResult().addBeginNode(support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  support.popCurrentScope();
                  yyVal = null; /*XXX 0;*/
              }
  break;
case 22:
					// line 355 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
                      yyerror("END in method; use at_exit");
                  }
                  support.getResult().addEndNode(new IterNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), null, null, new PostExeNode(getPosition(((Token)yyVals[-3+yyTop]))), ((Node)yyVals[-1+yyTop])));
                  yyVal = null;
              }
  break;
case 23:
					// line 362 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
                  yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 24:
					// line 366 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
		  if (((MultipleAsgnNode)yyVals[-2+yyTop]).getHeadNode() != null) {
		      ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(new ToAryNode(getPosition(((MultipleAsgnNode)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
		  } else {
		      ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(new ArrayNode(getPosition(((MultipleAsgnNode)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
		  }
		  yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
              }
  break;
case 25:
					// line 375 "DefaultRubyParser.y"
  {
 	          support.checkExpression(((Node)yyVals[0+yyTop]));

	          String name = ((INameNode)yyVals[-2+yyTop]).getName();
		  String asgnOp = (String) ((Token)yyVals[-1+yyTop]).getValue();
		  if (asgnOp.equals("||")) {
	              ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
	              yyVal = new OpAsgnOrNode(getPosition(((AssignableNode)yyVals[-2+yyTop])), support.gettable2(name, ((AssignableNode)yyVals[-2+yyTop]).getPosition()), ((AssignableNode)yyVals[-2+yyTop]));
		  } else if (asgnOp.equals("&&")) {
	              ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                      yyVal = new OpAsgnAndNode(getPosition(((AssignableNode)yyVals[-2+yyTop])), support.gettable2(name, ((AssignableNode)yyVals[-2+yyTop]).getPosition()), ((AssignableNode)yyVals[-2+yyTop]));
		  } else {
                      ((AssignableNode)yyVals[-2+yyTop]).setValueNode(support.getOperatorCallNode(support.gettable2(name, ((AssignableNode)yyVals[-2+yyTop]).getPosition()), asgnOp, ((Node)yyVals[0+yyTop])));
		      yyVal = ((AssignableNode)yyVals[-2+yyTop]);
		  }
	      }
  break;
case 26:
					// line 391 "DefaultRubyParser.y"
  {
                  yyVal = new OpElementAsgnNode(getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));

              }
  break;
case 27:
					// line 395 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 28:
					// line 398 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 29:
					// line 401 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 30:
					// line 404 "DefaultRubyParser.y"
  {
                  support.backrefAssignError(((Node)yyVals[-2+yyTop]));
              }
  break;
case 31:
					// line 407 "DefaultRubyParser.y"
  {
                  yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), new SValueNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
              }
  break;
case 32:
					// line 410 "DefaultRubyParser.y"
  {
                  if (((MultipleAsgnNode)yyVals[-2+yyTop]).getHeadNode() != null) {
		      ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(new ToAryNode(getPosition(((MultipleAsgnNode)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
		  } else {
		      ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(new ArrayNode(getPosition(((MultipleAsgnNode)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
		  }
		  yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
	      }
  break;
case 33:
					// line 418 "DefaultRubyParser.y"
  {
                  ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
		  yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
	      }
  break;
case 36:
					// line 425 "DefaultRubyParser.y"
  {
                  yyVal = support.newAndNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 37:
					// line 428 "DefaultRubyParser.y"
  {
                  yyVal = support.newOrNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 38:
					// line 431 "DefaultRubyParser.y"
  {
                  yyVal = new NotNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
              }
  break;
case 39:
					// line 434 "DefaultRubyParser.y"
  {
                  yyVal = new NotNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
              }
  break;
case 41:
					// line 439 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
	      }
  break;
case 44:
					// line 445 "DefaultRubyParser.y"
  {
                  yyVal = new ReturnNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
              }
  break;
case 45:
					// line 448 "DefaultRubyParser.y"
  {
                  yyVal = new BreakNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
              }
  break;
case 46:
					// line 451 "DefaultRubyParser.y"
  {
                  yyVal = new NextNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
              }
  break;
case 48:
					// line 456 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 49:
					// line 459 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 50:
					// line 463 "DefaultRubyParser.y"
  {
                    support.pushBlockScope();
		}
  break;
case 51:
					// line 465 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]), null);
                    support.popCurrentScope();
		}
  break;
case 52:
					// line 470 "DefaultRubyParser.y"
  {
                  yyVal = support.new_fcall(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 53:
					// line 473 "DefaultRubyParser.y"
  {
                  yyVal = support.new_fcall(((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop])); 
	          if (((IterNode)yyVals[0+yyTop]) != null) {
                      if (yyVal instanceof BlockPassNode) {
                          throw new SyntaxException(getPosition(((Token)yyVals[-2+yyTop])), "Both block arg and actual block given.");
                      }
                      ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVal));
                      yyVal = ((Node)yyVals[-1+yyTop]);
		  }
              }
  break;
case 54:
					// line 483 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])); /*.setPosFrom($1);*/
              }
  break;
case 55:
					// line 486 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop])); 
		  if (((IterNode)yyVals[0+yyTop]) != null) {
		      if (yyVal instanceof BlockPassNode) {
                          throw new SyntaxException(getPosition(((Node)yyVals[-4+yyTop])), "Both block arg and actual block given.");
                      }
                      ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVal));
		      yyVal = ((IterNode)yyVals[0+yyTop]);
		  }
	      }
  break;
case 56:
					// line 496 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 57:
					// line 499 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop])); 
		  if (((IterNode)yyVals[0+yyTop]) != null) {
		      if (yyVal instanceof BlockPassNode) {
                          throw new SyntaxException(getPosition(((Node)yyVals[-4+yyTop])), "Both block arg and actual block given.");
                      }
                      ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVal));
		      yyVal = ((IterNode)yyVals[0+yyTop]);
		  }
	      }
  break;
case 58:
					// line 509 "DefaultRubyParser.y"
  {
		  yyVal = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop])); /* .setPosFrom($2);*/
	      }
  break;
case 59:
					// line 512 "DefaultRubyParser.y"
  {
                  yyVal = support.new_yield(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
	      }
  break;
case 61:
					// line 517 "DefaultRubyParser.y"
  {
                  yyVal = ((MultipleAsgnNode)yyVals[-1+yyTop]);
	      }
  break;
case 63:
					// line 522 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((Token)yyVals[-2+yyTop])), new ArrayNode(getPosition(((Token)yyVals[-2+yyTop])), ((MultipleAsgnNode)yyVals[-1+yyTop])), null);
              }
  break;
case 64:
					// line 526 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((ListNode)yyVals[0+yyTop])), ((ListNode)yyVals[0+yyTop]), null);
              }
  break;
case 65:
					// line 529 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((ListNode)yyVals[-1+yyTop])), ((ListNode)yyVals[-1+yyTop]).add(((Node)yyVals[0+yyTop])), null);
              }
  break;
case 66:
					// line 532 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((ListNode)yyVals[-2+yyTop])), ((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 67:
					// line 535 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((ListNode)yyVals[-1+yyTop])), ((ListNode)yyVals[-1+yyTop]), new StarNode(getPosition(null)));
              }
  break;
case 68:
					// line 538 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((Token)yyVals[-1+yyTop])), null, ((Node)yyVals[0+yyTop]));
              }
  break;
case 69:
					// line 541 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((Token)yyVals[0+yyTop])), null, new StarNode(getPosition(null)));
              }
  break;
case 71:
					// line 546 "DefaultRubyParser.y"
  {
                  yyVal = ((MultipleAsgnNode)yyVals[-1+yyTop]);
              }
  break;
case 72:
					// line 550 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(((Node)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 73:
					// line 553 "DefaultRubyParser.y"
  {
                  yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
              }
  break;
case 74:
					// line 557 "DefaultRubyParser.y"
  {
                  yyVal = support.assignable(((Token)yyVals[0+yyTop]), null);
              }
  break;
case 75:
					// line 560 "DefaultRubyParser.y"
  {
                  yyVal = support.aryset(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 76:
					// line 563 "DefaultRubyParser.y"
  {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 77:
					// line 566 "DefaultRubyParser.y"
  {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 78:
					// line 569 "DefaultRubyParser.y"
  {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 79:
					// line 572 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
		      yyerror("dynamic constant assignment");
		  }
			
		  yyVal = new ConstDeclNode(support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue(), null);
	      }
  break;
case 80:
					// line 579 "DefaultRubyParser.y"
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
					// line 588 "DefaultRubyParser.y"
  {
	          support.backrefAssignError(((Node)yyVals[0+yyTop]));
              }
  break;
case 82:
					// line 592 "DefaultRubyParser.y"
  {
                  yyVal = support.assignable(((Token)yyVals[0+yyTop]), null);
              }
  break;
case 83:
					// line 595 "DefaultRubyParser.y"
  {
                  yyVal = support.aryset(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 84:
					// line 598 "DefaultRubyParser.y"
  {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 85:
					// line 601 "DefaultRubyParser.y"
  {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
 	      }
  break;
case 86:
					// line 604 "DefaultRubyParser.y"
  {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 87:
					// line 607 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
		      yyerror("dynamic constant assignment");
		  }
			
                  yyVal = new ConstDeclNode(support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue(), null);
	      }
  break;
case 88:
					// line 614 "DefaultRubyParser.y"
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
					// line 623 "DefaultRubyParser.y"
  {
                   support.backrefAssignError(((Node)yyVals[0+yyTop]));
	      }
  break;
case 90:
					// line 627 "DefaultRubyParser.y"
  {
                  yyerror("class/module name must be CONSTANT");
              }
  break;
case 92:
					// line 632 "DefaultRubyParser.y"
  {
                  yyVal = new Colon3Node(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
	      }
  break;
case 93:
					// line 635 "DefaultRubyParser.y"
  {
                  yyVal = new Colon2Node(((Token)yyVals[0+yyTop]).getPosition(), null, (String) ((Token)yyVals[0+yyTop]).getValue());
 	      }
  break;
case 94:
					// line 638 "DefaultRubyParser.y"
  {
                  yyVal = new Colon2Node(support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
	      }
  break;
case 98:
					// line 644 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_END);
                  yyVal = ((Token)yyVals[0+yyTop]);
              }
  break;
case 99:
					// line 649 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_END);
                  yyVal = yyVals[0+yyTop];
              }
  break;
case 102:
					// line 656 "DefaultRubyParser.y"
  {
                  yyVal = new UndefNode(getPosition(((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 103:
					// line 659 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_FNAME);
	      }
  break;
case 104:
					// line 661 "DefaultRubyParser.y"
  {
                  yyVal = support.appendToBlock(((Node)yyVals[-3+yyTop]), new UndefNode(getPosition(((Node)yyVals[-3+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue()));
              }
  break;
case 172:
					// line 680 "DefaultRubyParser.y"
  {
                  yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
		  /* FIXME: Consider fixing node_assign itself rather than single case*/
		  ((Node)yyVal).setPosition(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
              }
  break;
case 173:
					// line 685 "DefaultRubyParser.y"
  {
                  ISourcePosition position = support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                  yyVal = support.node_assign(((Node)yyVals[-4+yyTop]), new RescueNode(position, ((Node)yyVals[-2+yyTop]), new RescueBodyNode(position, null, ((Node)yyVals[0+yyTop]), null), null));
	      }
  break;
case 174:
					// line 689 "DefaultRubyParser.y"
  {
		  support.checkExpression(((Node)yyVals[0+yyTop]));
	          String name = ((INameNode)yyVals[-2+yyTop]).getName();
		  String asgnOp = (String) ((Token)yyVals[-1+yyTop]).getValue();

		  if (asgnOp.equals("||")) {
	              ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
	              yyVal = new OpAsgnOrNode(getPosition(((AssignableNode)yyVals[-2+yyTop])), support.gettable2(name, ((AssignableNode)yyVals[-2+yyTop]).getPosition()), ((AssignableNode)yyVals[-2+yyTop]));
		  } else if (asgnOp.equals("&&")) {
	              ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                      yyVal = new OpAsgnAndNode(getPosition(((AssignableNode)yyVals[-2+yyTop])), support.gettable2(name, ((AssignableNode)yyVals[-2+yyTop]).getPosition()), ((AssignableNode)yyVals[-2+yyTop]));
		  } else {
		      ((AssignableNode)yyVals[-2+yyTop]).setValueNode(support.getOperatorCallNode(support.gettable2(name, ((AssignableNode)yyVals[-2+yyTop]).getPosition()), asgnOp, ((Node)yyVals[0+yyTop])));
		      yyVal = ((AssignableNode)yyVals[-2+yyTop]);
		  }
              }
  break;
case 175:
					// line 705 "DefaultRubyParser.y"
  {
                  yyVal = new OpElementAsgnNode(getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 176:
					// line 708 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 177:
					// line 711 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 178:
					// line 714 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 179:
					// line 717 "DefaultRubyParser.y"
  {
	          yyerror("constant re-assignment");
	      }
  break;
case 180:
					// line 720 "DefaultRubyParser.y"
  {
		  yyerror("constant re-assignment");
	      }
  break;
case 181:
					// line 723 "DefaultRubyParser.y"
  {
                  support.backrefAssignError(((Node)yyVals[-2+yyTop]));
              }
  break;
case 182:
					// line 726 "DefaultRubyParser.y"
  {
		  support.checkExpression(((Node)yyVals[-2+yyTop]));
		  support.checkExpression(((Node)yyVals[0+yyTop]));
                  yyVal = new DotNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), false);
              }
  break;
case 183:
					// line 731 "DefaultRubyParser.y"
  {
		  support.checkExpression(((Node)yyVals[-2+yyTop]));
		  support.checkExpression(((Node)yyVals[0+yyTop]));
                  yyVal = new DotNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), true);
              }
  break;
case 184:
					// line 736 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "+", ((Node)yyVals[0+yyTop]));
              }
  break;
case 185:
					// line 739 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "-", ((Node)yyVals[0+yyTop]));
              }
  break;
case 186:
					// line 742 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "*", ((Node)yyVals[0+yyTop]));
              }
  break;
case 187:
					// line 745 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "/", ((Node)yyVals[0+yyTop]));
              }
  break;
case 188:
					// line 748 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "%", ((Node)yyVals[0+yyTop]));
              }
  break;
case 189:
					// line 751 "DefaultRubyParser.y"
  {
		  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]));
              }
  break;
case 190:
					// line 754 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop])), "-@");
              }
  break;
case 191:
					// line 757 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop])), "-@");
              }
  break;
case 192:
					// line 760 "DefaultRubyParser.y"
  {
 	          if (((Node)yyVals[0+yyTop]) != null && ((Node)yyVals[0+yyTop]) instanceof ILiteralNode) {
		      yyVal = ((Node)yyVals[0+yyTop]);
		  } else {
                      yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "+@");
		  }
              }
  break;
case 193:
					// line 767 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "-@");
	      }
  break;
case 194:
					// line 770 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "|", ((Node)yyVals[0+yyTop]));
              }
  break;
case 195:
					// line 773 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "^", ((Node)yyVals[0+yyTop]));
              }
  break;
case 196:
					// line 776 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "&", ((Node)yyVals[0+yyTop]));
              }
  break;
case 197:
					// line 779 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=>", ((Node)yyVals[0+yyTop]));
              }
  break;
case 198:
					// line 782 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">", ((Node)yyVals[0+yyTop]));
              }
  break;
case 199:
					// line 785 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">=", ((Node)yyVals[0+yyTop]));
              }
  break;
case 200:
					// line 788 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<", ((Node)yyVals[0+yyTop]));
              }
  break;
case 201:
					// line 791 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=", ((Node)yyVals[0+yyTop]));
              }
  break;
case 202:
					// line 794 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop]));
              }
  break;
case 203:
					// line 797 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "===", ((Node)yyVals[0+yyTop]));
              }
  break;
case 204:
					// line 800 "DefaultRubyParser.y"
  {
                  yyVal = new NotNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop])));
              }
  break;
case 205:
					// line 803 "DefaultRubyParser.y"
  {
                  yyVal = support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 206:
					// line 806 "DefaultRubyParser.y"
  {
                  yyVal = new NotNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
              }
  break;
case 207:
					// line 809 "DefaultRubyParser.y"
  {
                  yyVal = new NotNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
              }
  break;
case 208:
					// line 812 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "~");
              }
  break;
case 209:
					// line 815 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<<", ((Node)yyVals[0+yyTop]));
              }
  break;
case 210:
					// line 818 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">>", ((Node)yyVals[0+yyTop]));
              }
  break;
case 211:
					// line 821 "DefaultRubyParser.y"
  {
                  yyVal = support.newAndNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 212:
					// line 824 "DefaultRubyParser.y"
  {
                  yyVal = support.newOrNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 213:
					// line 827 "DefaultRubyParser.y"
  {
                  yyVal = new DefinedNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop]));
              }
  break;
case 214:
					// line 830 "DefaultRubyParser.y"
  {
                  yyVal = new IfNode(getPosition(((Node)yyVals[-4+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 215:
					// line 833 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 216:
					// line 837 "DefaultRubyParser.y"
  {
	          support.checkExpression(((Node)yyVals[0+yyTop]));
	          yyVal = ((Node)yyVals[0+yyTop]);   
	      }
  break;
case 218:
					// line 843 "DefaultRubyParser.y"
  {
                  warnings.warn(getPosition(((Node)yyVals[-1+yyTop])), "parenthesize argument(s) for future version");
                  yyVal = new ArrayNode(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 219:
					// line 847 "DefaultRubyParser.y"
  {
                  yyVal = ((ListNode)yyVals[-1+yyTop]);
              }
  break;
case 220:
					// line 850 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 221:
					// line 854 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition(((ListNode)yyVals[-1+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
              }
  break;
case 222:
					// line 857 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[-1+yyTop]));
		  yyVal = new NewlineNode(getPosition(((Token)yyVals[-2+yyTop])), new SplatNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])));
              }
  break;
case 223:
					// line 862 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
              }
  break;
case 224:
					// line 865 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[-2+yyTop]);
		  ((Node)yyVal).setPosition(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])));
              }
  break;
case 225:
					// line 869 "DefaultRubyParser.y"
  {
                  warnings.warn(getPosition(((Token)yyVals[-3+yyTop])), "parenthesize argument(s) for future version");
                  yyVal = new ArrayNode(getPosition(((Token)yyVals[-3+yyTop])), ((Node)yyVals[-2+yyTop]));
              }
  break;
case 226:
					// line 873 "DefaultRubyParser.y"
  {
                  warnings.warn(getPosition(((Token)yyVals[-5+yyTop])), "parenthesize argument(s) for future version");
                  yyVal = ((ListNode)yyVals[-4+yyTop]).add(((Node)yyVals[-2+yyTop]));
              }
  break;
case 229:
					// line 881 "DefaultRubyParser.y"
  {
                  warnings.warn(getPosition(((Node)yyVals[0+yyTop])), "parenthesize argument(s) for future version");
                  yyVal = new ArrayNode(getPosition(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
              }
  break;
case 230:
					// line 885 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_blk_pass(((ListNode)yyVals[-1+yyTop]), ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 231:
					// line 888 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass(((Node)yyVal), ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 232:
					// line 892 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition(((ListNode)yyVals[-1+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 233:
					// line 896 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), new ArrayNode(getPosition(((ListNode)yyVals[-4+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 234:
					// line 900 "DefaultRubyParser.y"
  {
                  yyVal = ((ListNode)yyVals[-3+yyTop]).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 235:
					// line 904 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[-1+yyTop]));
		  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-6+yyTop])), ((ListNode)yyVals[-6+yyTop]).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 236:
					// line 909 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_blk_pass(new SplatNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 237:
					// line 912 "DefaultRubyParser.y"
  {}
  break;
case 238:
					// line 914 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_blk_pass(new ArrayNode(getPosition(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop])).addAll(((ListNode)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 239:
					// line 917 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_blk_pass(new ArrayNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 240:
					// line 920 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-4+yyTop])), new ArrayNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 241:
					// line 924 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-6+yyTop])), new ArrayNode(getPosition(((Node)yyVals[-6+yyTop])), ((Node)yyVals[-6+yyTop])).addAll(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 242:
					// line 928 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition(((ListNode)yyVals[-1+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 243:
					// line 932 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), new ArrayNode(getPosition(((ListNode)yyVals[-4+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 244:
					// line 936 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 245:
					// line 940 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop])).addAll(((ListNode)yyVals[-3+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 246:
					// line 944 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-6+yyTop])), new ArrayNode(getPosition(((Node)yyVals[-6+yyTop])), ((Node)yyVals[-6+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 247:
					// line 948 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-8+yyTop])), new ArrayNode(getPosition(((Node)yyVals[-8+yyTop])), ((Node)yyVals[-8+yyTop])).addAll(((ListNode)yyVals[-6+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 248:
					// line 952 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_blk_pass(new SplatNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 249:
					// line 955 "DefaultRubyParser.y"
  {}
  break;
case 250:
					// line 957 "DefaultRubyParser.y"
  { 
	          yyVal = new Long(lexer.getCmdArgumentState().begin());
	      }
  break;
case 251:
					// line 959 "DefaultRubyParser.y"
  {
                  lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 253:
					// line 965 "DefaultRubyParser.y"
  {                    
		  lexer.setState(LexState.EXPR_ENDARG);
	      }
  break;
case 254:
					// line 967 "DefaultRubyParser.y"
  {
                  warnings.warn(getPosition(((Token)yyVals[-2+yyTop])), "don't put space before argument parentheses");
	          yyVal = null;
	      }
  break;
case 255:
					// line 971 "DefaultRubyParser.y"
  {
		  lexer.setState(LexState.EXPR_ENDARG);
	      }
  break;
case 256:
					// line 973 "DefaultRubyParser.y"
  {
                  warnings.warn(getPosition(((Token)yyVals[-3+yyTop])), "don't put space before argument parentheses");
		  yyVal = ((Node)yyVals[-2+yyTop]);
	      }
  break;
case 257:
					// line 978 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
                  yyVal = new BlockPassNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
              }
  break;
case 258:
					// line 983 "DefaultRubyParser.y"
  {
                  yyVal = ((BlockPassNode)yyVals[0+yyTop]);
              }
  break;
case 260:
					// line 988 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition2(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
              }
  break;
case 261:
					// line 991 "DefaultRubyParser.y"
  {
                  yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
              }
  break;
case 262:
					// line 995 "DefaultRubyParser.y"
  {
		  yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
              }
  break;
case 263:
					// line 998 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
	      }
  break;
case 264:
					// line 1001 "DefaultRubyParser.y"
  {  
                  yyVal = new SplatNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
	      }
  break;
case 273:
					// line 1013 "DefaultRubyParser.y"
  {
                  yyVal = new VCallNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
	      }
  break;
case 274:
					// line 1016 "DefaultRubyParser.y"
  {
                  yyVal = new BeginNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-1+yyTop]));
	      }
  break;
case 275:
					// line 1019 "DefaultRubyParser.y"
  { 
                  lexer.setState(LexState.EXPR_ENDARG); 
              }
  break;
case 276:
					// line 1021 "DefaultRubyParser.y"
  {
		  warnings.warning(getPosition(((Token)yyVals[-4+yyTop])), "(...) interpreted as grouped expression");
                  yyVal = ((Node)yyVals[-3+yyTop]);
	      }
  break;
case 277:
					// line 1025 "DefaultRubyParser.y"
  {
		  yyVal = ((Node)yyVals[-1+yyTop]);
              }
  break;
case 278:
					// line 1028 "DefaultRubyParser.y"
  {
                  yyVal = new Colon2Node(support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 279:
					// line 1031 "DefaultRubyParser.y"
  {
                  yyVal = new Colon3Node(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 280:
					// line 1034 "DefaultRubyParser.y"
  {
                  yyVal = new CallNode(getPosition(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop]), "[]", ((Node)yyVals[-1+yyTop]));
              }
  break;
case 281:
					// line 1037 "DefaultRubyParser.y"
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
case 282:
					// line 1046 "DefaultRubyParser.y"
  {
                  yyVal = new HashNode(getPosition(((Token)yyVals[-2+yyTop])), ((ListNode)yyVals[-1+yyTop]));
              }
  break;
case 283:
					// line 1049 "DefaultRubyParser.y"
  {
		  yyVal = new ReturnNode(((Token)yyVals[0+yyTop]).getPosition(), null);
              }
  break;
case 284:
					// line 1052 "DefaultRubyParser.y"
  {
                  yyVal = support.new_yield(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 285:
					// line 1055 "DefaultRubyParser.y"
  {
                  yyVal = new YieldNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), null, false);
              }
  break;
case 286:
					// line 1058 "DefaultRubyParser.y"
  {
                  yyVal = new YieldNode(((Token)yyVals[0+yyTop]).getPosition(), null, false);
              }
  break;
case 287:
					// line 1061 "DefaultRubyParser.y"
  {
                  yyVal = new DefinedNode(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 288:
					// line 1064 "DefaultRubyParser.y"
  {
                  ((IterNode)yyVals[0+yyTop]).setIterNode(new FCallNode(support.union(((Token)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), null));
                  yyVal = ((IterNode)yyVals[0+yyTop]);
              }
  break;
case 290:
					// line 1069 "DefaultRubyParser.y"
  {
		  if (((Node)yyVals[-1+yyTop]) != null && ((Node)yyVals[-1+yyTop]) instanceof BlockPassNode) {
                      throw new SyntaxException(getPosition(((Node)yyVals[-1+yyTop])), "Both block arg and actual block given.");
		  }
                  ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVals[-1+yyTop]));
                  yyVal = ((IterNode)yyVals[0+yyTop]);
		  /* XXXEnebo: Having iter around a call seems backwards*/
                  ((Node)yyVals[-1+yyTop]).setPosition(support.union(((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])));
              }
  break;
case 291:
					// line 1078 "DefaultRubyParser.y"
  {
                  yyVal = new IfNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 292:
					// line 1081 "DefaultRubyParser.y"
  {
                  yyVal = new IfNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-2+yyTop]));
              }
  break;
case 293:
					// line 1084 "DefaultRubyParser.y"
  { 
                  lexer.getConditionState().begin();
	      }
  break;
case 294:
					// line 1086 "DefaultRubyParser.y"
  {
		  lexer.getConditionState().end();
	      }
  break;
case 295:
					// line 1088 "DefaultRubyParser.y"
  {
                  yyVal = new WhileNode(getPosition(((Token)yyVals[-6+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 296:
					// line 1091 "DefaultRubyParser.y"
  {
                  lexer.getConditionState().begin();
              }
  break;
case 297:
					// line 1093 "DefaultRubyParser.y"
  {
                  lexer.getConditionState().end();
              }
  break;
case 298:
					// line 1095 "DefaultRubyParser.y"
  {
                  yyVal = new UntilNode(getPosition(((Token)yyVals[-6+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 299:
					// line 1098 "DefaultRubyParser.y"
  {
                  yyVal = new CaseNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 300:
					// line 1101 "DefaultRubyParser.y"
  {
                  yyVal = new CaseNode(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])), null, ((Node)yyVals[-1+yyTop]));
              }
  break;
case 301:
					// line 1104 "DefaultRubyParser.y"
  {
		  yyVal = ((Node)yyVals[-1+yyTop]);
              }
  break;
case 302:
					// line 1107 "DefaultRubyParser.y"
  {
                  lexer.getConditionState().begin();
              }
  break;
case 303:
					// line 1109 "DefaultRubyParser.y"
  {
                  lexer.getConditionState().end();
              }
  break;
case 304:
					// line 1111 "DefaultRubyParser.y"
  {
                  yyVal = new ForNode(support.union(((Token)yyVals[-8+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-7+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-4+yyTop]));
              }
  break;
case 305:
					// line 1114 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
                      yyerror("class definition in method body");
                  }
		  support.pushLocalScope();
              }
  break;
case 306:
					// line 1119 "DefaultRubyParser.y"
  {
                  yyVal = new ClassNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-4+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-3+yyTop]));
                  support.popCurrentScope();
              }
  break;
case 307:
					// line 1123 "DefaultRubyParser.y"
  {
                  yyVal = new Boolean(support.isInDef());
                  support.setInDef(false);
              }
  break;
case 308:
					// line 1126 "DefaultRubyParser.y"
  {
                  yyVal = new Integer(support.getInSingle());
                  support.setInSingle(0);
		  support.pushLocalScope();
              }
  break;
case 309:
					// line 1130 "DefaultRubyParser.y"
  {
                  yyVal = new SClassNode(support.union(((Token)yyVals[-7+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-5+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  support.popCurrentScope();
                  support.setInDef(((Boolean)yyVals[-4+yyTop]).booleanValue());
                  support.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
              }
  break;
case 310:
					// line 1136 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) { 
                      yyerror("module definition in method body");
                  }
		  support.pushLocalScope();
              }
  break;
case 311:
					// line 1141 "DefaultRubyParser.y"
  {
                  yyVal = new ModuleNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-3+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  support.popCurrentScope();
              }
  break;
case 312:
					// line 1145 "DefaultRubyParser.y"
  {
                  support.setInDef(true);
		  support.pushLocalScope();
              }
  break;
case 313:
					// line 1148 "DefaultRubyParser.y"
  {
                    /* NOEX_PRIVATE for toplevel */
                  yyVal = new DefnNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), new ArgumentNode(((Token)yyVals[-4+yyTop]).getPosition(), (String) ((Token)yyVals[-4+yyTop]).getValue()), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]), Visibility.PRIVATE);
                  support.popCurrentScope();
                  support.setInDef(false);
              }
  break;
case 314:
					// line 1154 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_FNAME);
              }
  break;
case 315:
					// line 1156 "DefaultRubyParser.y"
  {
                  support.setInSingle(support.getInSingle() + 1);
		  support.pushLocalScope();
                  lexer.setState(LexState.EXPR_END); /* force for args */
              }
  break;
case 316:
					// line 1160 "DefaultRubyParser.y"
  {
                  yyVal = new DefsNode(support.union(((Token)yyVals[-8+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-7+yyTop]), (String) ((Token)yyVals[-4+yyTop]).getValue(), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  support.popCurrentScope();
                  support.setInSingle(support.getInSingle() - 1);
              }
  break;
case 317:
					// line 1165 "DefaultRubyParser.y"
  {
                  yyVal = new BreakNode(((Token)yyVals[0+yyTop]).getPosition());
              }
  break;
case 318:
					// line 1168 "DefaultRubyParser.y"
  {
                  yyVal = new NextNode(((Token)yyVals[0+yyTop]).getPosition());
              }
  break;
case 319:
					// line 1171 "DefaultRubyParser.y"
  {
                  yyVal = new RedoNode(((Token)yyVals[0+yyTop]).getPosition());
              }
  break;
case 320:
					// line 1174 "DefaultRubyParser.y"
  {
                  yyVal = new RetryNode(((Token)yyVals[0+yyTop]).getPosition());
              }
  break;
case 321:
					// line 1178 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
		  yyVal = ((Node)yyVals[0+yyTop]);
	      }
  break;
case 330:
					// line 1193 "DefaultRubyParser.y"
  {
                  yyVal = new IfNode(getPosition(((Token)yyVals[-4+yyTop])), support.getConditionNode(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 332:
					// line 1198 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 334:
					// line 1203 "DefaultRubyParser.y"
  {}
  break;
case 336:
					// line 1206 "DefaultRubyParser.y"
  {
                  yyVal = new ZeroArgNode(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])));
              }
  break;
case 337:
					// line 1209 "DefaultRubyParser.y"
  {
                  yyVal = new ZeroArgNode(((Token)yyVals[0+yyTop]).getPosition());
	      }
  break;
case 338:
					// line 1212 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[-1+yyTop]);

		  /* Include pipes on multiple arg type*/
                  if (((Node)yyVals[-1+yyTop]) instanceof MultipleAsgnNode) {
		      ((Node)yyVals[-1+yyTop]).setPosition(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
		  } 
              }
  break;
case 339:
					// line 1221 "DefaultRubyParser.y"
  {
                  support.pushBlockScope();
	      }
  break;
case 340:
					// line 1223 "DefaultRubyParser.y"
  {
                  yyVal = new IterNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]), null);
                  support.popCurrentScope();
              }
  break;
case 341:
					// line 1228 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-1+yyTop]) instanceof BlockPassNode) {
                      throw new SyntaxException(getPosition(((Node)yyVals[-1+yyTop])), "Both block arg and actual block given.");
                  }
                  ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVals[-1+yyTop]));
                  yyVal = ((IterNode)yyVals[0+yyTop]);
              }
  break;
case 342:
					// line 1235 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 343:
					// line 1238 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 344:
					// line 1242 "DefaultRubyParser.y"
  {
                  yyVal = support.new_fcall(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 345:
					// line 1245 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 346:
					// line 1248 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 347:
					// line 1251 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop]), null);
              }
  break;
case 348:
					// line 1254 "DefaultRubyParser.y"
  {
                  yyVal = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop]));
              }
  break;
case 349:
					// line 1257 "DefaultRubyParser.y"
  {
                  yyVal = new ZSuperNode(getPosition(((Token)yyVals[0+yyTop])));
              }
  break;
case 350:
					// line 1262 "DefaultRubyParser.y"
  {
                  support.pushBlockScope();
	      }
  break;
case 351:
					// line 1264 "DefaultRubyParser.y"
  {
                  yyVal = new IterNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]), null);
                  support.popCurrentScope();
              }
  break;
case 352:
					// line 1268 "DefaultRubyParser.y"
  {
                  support.pushBlockScope();
	      }
  break;
case 353:
					// line 1270 "DefaultRubyParser.y"
  {
                  yyVal = new IterNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]), null);
                  support.popCurrentScope();
              }
  break;
case 354:
					// line 1275 "DefaultRubyParser.y"
  {
                  yyVal = new WhenNode(((Token)yyVals[-4+yyTop]).getPosition(), ((ListNode)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 356:
					// line 1280 "DefaultRubyParser.y"
  {
                  yyVal = ((ListNode)yyVals[-3+yyTop]).add(new WhenNode(getPosition(((ListNode)yyVals[-3+yyTop])), ((Node)yyVals[0+yyTop]), null, null));
              }
  break;
case 357:
					// line 1283 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition(((Token)yyVals[-1+yyTop])), new WhenNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]), null, null));
              }
  break;
case 360:
					// line 1290 "DefaultRubyParser.y"
  {
                  Node node;
                  if (((Node)yyVals[-3+yyTop]) != null) {
                     node = support.appendToBlock(support.node_assign(((Node)yyVals[-3+yyTop]), new GlobalVarNode(getPosition(((Token)yyVals[-5+yyTop])), "$!")), ((Node)yyVals[-1+yyTop]));
		  } else {
		     node = ((Node)yyVals[-1+yyTop]);
                  }
                  yyVal = new RescueBodyNode(getPosition(((Token)yyVals[-5+yyTop]), true), ((Node)yyVals[-4+yyTop]), node, ((RescueBodyNode)yyVals[0+yyTop]));
	      }
  break;
case 361:
					// line 1299 "DefaultRubyParser.y"
  {yyVal = null;}
  break;
case 362:
					// line 1301 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
	      }
  break;
case 365:
					// line 1307 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 367:
					// line 1312 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[0+yyTop]) != null) {
                      yyVal = ((Node)yyVals[0+yyTop]);
                  } else {
                      yyVal = new NilNode(getPosition(null));
                  }
              }
  break;
case 370:
					// line 1322 "DefaultRubyParser.y"
  {
                  yyVal = new SymbolNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 372:
					// line 1327 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[0+yyTop]) instanceof EvStrNode) {
                      yyVal = new DStrNode(getPosition(((Node)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
                  } else {
                      yyVal = ((Node)yyVals[0+yyTop]);
                  }
	      }
  break;
case 374:
					// line 1336 "DefaultRubyParser.y"
  {
                  yyVal = support.literal_concat(((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 375:
					// line 1340 "DefaultRubyParser.y"
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
case 376:
					// line 1356 "DefaultRubyParser.y"
  {
                  ISourcePosition position = support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop]));

		  if (((Node)yyVals[-1+yyTop]) == null) {
		      yyVal = new XStrNode(position, null);
		  } else {
		      if (((Node)yyVals[-1+yyTop]) instanceof StrNode) {
			  yyVal = new XStrNode(position, ((StrNode)yyVals[-1+yyTop]).getValue());
		      } else if (((Node)yyVals[-1+yyTop]) instanceof DStrNode) {
		          ((Node)yyVals[-1+yyTop]).setPosition(position);
		          yyVal = new DXStrNode(position).add(((Node)yyVals[-1+yyTop]));
		      } else {
			  yyVal = new DXStrNode(position).add(new ArrayNode(position, ((Node)yyVals[-1+yyTop])));
		      }
		  }
              }
  break;
case 377:
					// line 1373 "DefaultRubyParser.y"
  {
		  int options = ((RegexpNode)yyVals[0+yyTop]).getOptions();
		  Node node = ((Node)yyVals[-1+yyTop]);

		  if (node == null) {
		      yyVal = new RegexpNode(getPosition(((Token)yyVals[-2+yyTop])), "", options & ~ReOptions.RE_OPTION_ONCE);
		  } else if (node instanceof StrNode) {
		      yyVal = new RegexpNode(getPosition(((Token)yyVals[-2+yyTop])), ((StrNode) node).getValue(), options & ~ReOptions.RE_OPTION_ONCE);
		  } else {
		      if (node instanceof DStrNode == false) {
			  node = new DStrNode(getPosition(((Token)yyVals[-2+yyTop]))).add(new ArrayNode(getPosition(((Token)yyVals[-2+yyTop])), node));
		      } 

		      yyVal = new DRegexpNode(getPosition(((Token)yyVals[-2+yyTop])), options, (options & ReOptions.RE_OPTION_ONCE) != 0).add(node);
		    }
	       }
  break;
case 378:
					// line 1390 "DefaultRubyParser.y"
  {
                   yyVal = new ZArrayNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
	       }
  break;
case 379:
					// line 1393 "DefaultRubyParser.y"
  {
		   yyVal = ((ListNode)yyVals[-1+yyTop]);
                   ((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
	       }
  break;
case 380:
					// line 1398 "DefaultRubyParser.y"
  {
                   yyVal = new ArrayNode(getPosition(null));
	       }
  break;
case 381:
					// line 1401 "DefaultRubyParser.y"
  {
                   yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]) instanceof EvStrNode ? new DStrNode(getPosition(((ListNode)yyVals[-2+yyTop]))).add(((Node)yyVals[-1+yyTop])) : ((Node)yyVals[-1+yyTop]));
	       }
  break;
case 383:
					// line 1406 "DefaultRubyParser.y"
  {
                   yyVal = support.literal_concat(((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
	       }
  break;
case 384:
					// line 1410 "DefaultRubyParser.y"
  {
                   yyVal = new ZArrayNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
	       }
  break;
case 385:
					// line 1413 "DefaultRubyParser.y"
  {
		   yyVal = ((ListNode)yyVals[-1+yyTop]);
                   ((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
	       }
  break;
case 386:
					// line 1418 "DefaultRubyParser.y"
  {
                   yyVal = new ArrayNode(getPosition(null));
	       }
  break;
case 387:
					// line 1421 "DefaultRubyParser.y"
  {
                   yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
	       }
  break;
case 388:
					// line 1425 "DefaultRubyParser.y"
  {
                   yyVal = new StrNode(getPosition(null), "");
	       }
  break;
case 389:
					// line 1428 "DefaultRubyParser.y"
  {
                   yyVal = support.literal_concat(((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
	       }
  break;
case 390:
					// line 1432 "DefaultRubyParser.y"
  {
		   yyVal = null;
	       }
  break;
case 391:
					// line 1435 "DefaultRubyParser.y"
  {
                   yyVal = support.literal_concat(((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
	       }
  break;
case 392:
					// line 1439 "DefaultRubyParser.y"
  {
                   yyVal = ((Node)yyVals[0+yyTop]);
               }
  break;
case 393:
					// line 1442 "DefaultRubyParser.y"
  {
                   yyVal = lexer.getStrTerm();
		   lexer.setStrTerm(null);
		   lexer.setState(LexState.EXPR_BEG);
	       }
  break;
case 394:
					// line 1446 "DefaultRubyParser.y"
  {
		   lexer.setStrTerm(((StrTerm)yyVals[-1+yyTop]));
	           yyVal = new EvStrNode(support.union(((Token)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
	       }
  break;
case 395:
					// line 1450 "DefaultRubyParser.y"
  {
		   yyVal = lexer.getStrTerm();
		   lexer.setStrTerm(null);
		   lexer.setState(LexState.EXPR_BEG);
	       }
  break;
case 396:
					// line 1454 "DefaultRubyParser.y"
  {
		   lexer.setStrTerm(((StrTerm)yyVals[-2+yyTop]));

		   yyVal = support.newEvStrNode(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-1+yyTop]));
	       }
  break;
case 397:
					// line 1460 "DefaultRubyParser.y"
  {
                   yyVal = new GlobalVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
               }
  break;
case 398:
					// line 1463 "DefaultRubyParser.y"
  {
                   yyVal = new InstVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
               }
  break;
case 399:
					// line 1466 "DefaultRubyParser.y"
  {
                   yyVal = new ClassVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
               }
  break;
case 401:
					// line 1472 "DefaultRubyParser.y"
  {
                   lexer.setState(LexState.EXPR_END);
                   yyVal = ((Token)yyVals[0+yyTop]);
		   ((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])));
               }
  break;
case 406:
					// line 1480 "DefaultRubyParser.y"
  {
                   lexer.setState(LexState.EXPR_END);

		   /* DStrNode: :"some text #{some expression}"*/
                   /* StrNode: :"some text"*/
		   /* EvStrNode :"#{some expression}"*/
		   DStrNode node;

                   if (((Node)yyVals[-1+yyTop]) == null) {
                       yyerror("empty symbol literal");
                   }

		   if (((Node)yyVals[-1+yyTop]) instanceof DStrNode) {
		       node = (DStrNode) ((Node)yyVals[-1+yyTop]);
		   } else {
                       ISourcePosition position = support.union(((Node)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop]));

                       /* We substract one since tsymbeg is longer than one*/
		       /* and we cannot union it directly so we assume quote*/
                       /* and subtract for it.*/
		       position.adjustStartOffset(-1);
		       node = new DStrNode(position);
		       ((Node)yyVals[-1+yyTop]).setPosition(position);
		       node.add(new ArrayNode(position, ((Node)yyVals[-1+yyTop])));
                   }
		   yyVal = new DSymbolNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), node);
	       }
  break;
case 409:
					// line 1510 "DefaultRubyParser.y"
  {
                   yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "-@");
	       }
  break;
case 410:
					// line 1513 "DefaultRubyParser.y"
  {
                   yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "-@");
	       }
  break;
case 416:
					// line 1519 "DefaultRubyParser.y"
  { 
		   yyVal = new Token("nil", ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 417:
					// line 1522 "DefaultRubyParser.y"
  {
		   yyVal = new Token("self", ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 418:
					// line 1525 "DefaultRubyParser.y"
  { 
		   yyVal = new Token("true", ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 419:
					// line 1528 "DefaultRubyParser.y"
  {
		   yyVal = new Token("false", ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 420:
					// line 1531 "DefaultRubyParser.y"
  {
		   yyVal = new Token("__FILE__", ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 421:
					// line 1534 "DefaultRubyParser.y"
  {
		   yyVal = new Token("__LINE__", ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 422:
					// line 1538 "DefaultRubyParser.y"
  {
		   yyVal = support.gettable((String) ((Token)yyVals[0+yyTop]).getValue(), ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 423:
					// line 1542 "DefaultRubyParser.y"
  {
                   yyVal = support.assignable(((Token)yyVals[0+yyTop]), null);
               }
  break;
case 426:
					// line 1548 "DefaultRubyParser.y"
  {
                   yyVal = null;
               }
  break;
case 427:
					// line 1551 "DefaultRubyParser.y"
  {
                   lexer.setState(LexState.EXPR_BEG);
               }
  break;
case 428:
					// line 1553 "DefaultRubyParser.y"
  {
                   yyVal = ((Node)yyVals[-1+yyTop]);
               }
  break;
case 429:
					// line 1556 "DefaultRubyParser.y"
  {
                   yyerrok();
                   yyVal = null;
               }
  break;
case 430:
					// line 1562 "DefaultRubyParser.y"
  {
                   yyVal = ((Node)yyVals[-2+yyTop]);
                   ((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])));
                   lexer.setState(LexState.EXPR_BEG);
               }
  break;
case 431:
					// line 1567 "DefaultRubyParser.y"
  {
                   yyVal = ((Node)yyVals[-1+yyTop]);
               }
  break;
case 432:
					// line 1571 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(getPosition(((ListNode)yyVals[-5+yyTop])), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 433:
					// line 1574 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), -1, ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 434:
					// line 1577 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), null, ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 435:
					// line 1580 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(((ISourcePositionHolder)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, -1, ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 436:
					// line 1583 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(getPosition(((ListNode)yyVals[-3+yyTop])), null, ((ListNode)yyVals[-3+yyTop]), ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 437:
					// line 1586 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(getPosition(((ListNode)yyVals[-1+yyTop])), null, ((ListNode)yyVals[-1+yyTop]), -1, ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 438:
					// line 1589 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(getPosition(((Token)yyVals[-1+yyTop])), null, null, ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 439:
					// line 1592 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(getPosition(((BlockArgNode)yyVals[0+yyTop])), null, null, -1, ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 440:
					// line 1595 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(getPosition(null), null, null, -1, null);
               }
  break;
case 441:
					// line 1599 "DefaultRubyParser.y"
  {
                   yyerror("formal argument cannot be a constant");
               }
  break;
case 442:
					// line 1602 "DefaultRubyParser.y"
  {
                   yyerror("formal argument cannot be an instance variable");
               }
  break;
case 443:
					// line 1605 "DefaultRubyParser.y"
  {
                   yyerror("formal argument cannot be a class variable");
               }
  break;
case 444:
					// line 1608 "DefaultRubyParser.y"
  {
                   String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();
                   if (IdUtil.getVarType(identifier) != IdUtil.LOCAL_VAR) {
                       yyerror("formal argument must be local variable");
                   } else if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                       yyerror("duplicate argument name");
                   }

		   support.getCurrentScope().getLocalScope().addVariable(identifier);
                   yyVal = ((Token)yyVals[0+yyTop]);
               }
  break;
case 445:
					// line 1620 "DefaultRubyParser.y"
  {
                    yyVal = new ListNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
                    ((ListNode) yyVal).add(new ArgumentNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue()));
               }
  break;
case 446:
					// line 1624 "DefaultRubyParser.y"
  {
                   ((ListNode)yyVals[-2+yyTop]).add(new ArgumentNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue()));
                   ((ListNode)yyVals[-2+yyTop]).setPosition(support.union(((ListNode)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
		   yyVal = ((ListNode)yyVals[-2+yyTop]);
               }
  break;
case 447:
					// line 1630 "DefaultRubyParser.y"
  {
                   String identifier = (String) ((Token)yyVals[-2+yyTop]).getValue();

                   if (IdUtil.getVarType(identifier) != IdUtil.LOCAL_VAR) {
                       yyerror("formal argument must be local variable");
                   } else if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                       yyerror("duplicate optional argument name");
                   }
		   support.getCurrentScope().getLocalScope().addVariable(identifier);
                   yyVal = support.assignable(((Token)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 448:
					// line 1642 "DefaultRubyParser.y"
  {
                  yyVal = new BlockNode(getPosition(((Node)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
              }
  break;
case 449:
					// line 1645 "DefaultRubyParser.y"
  {
                  yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 452:
					// line 1651 "DefaultRubyParser.y"
  {
                  String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();

                  if (IdUtil.getVarType(identifier) != IdUtil.LOCAL_VAR) {
                      yyerror("rest argument must be local variable");
                   } else if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                      yyerror("duplicate rest argument name");
                  }
		  ((Token)yyVals[-1+yyTop]).setValue(new Integer(support.getCurrentScope().getLocalScope().addVariable(identifier)));
                  yyVal = ((Token)yyVals[-1+yyTop]);
              }
  break;
case 453:
					// line 1662 "DefaultRubyParser.y"
  {
                  ((Token)yyVals[0+yyTop]).setValue(new Integer(-2));
                  yyVal = ((Token)yyVals[0+yyTop]);
              }
  break;
case 456:
					// line 1669 "DefaultRubyParser.y"
  {
                  String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();

                  if (IdUtil.getVarType(identifier) != IdUtil.LOCAL_VAR) {
                      yyerror("block argument must be local variable");
		  } else if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                      yyerror("duplicate block argument name");
                  }
                  yyVal = new BlockArgNode(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), support.getCurrentScope().getLocalScope().addVariable(identifier), identifier);
              }
  break;
case 457:
					// line 1680 "DefaultRubyParser.y"
  {
                  yyVal = ((BlockArgNode)yyVals[0+yyTop]);
              }
  break;
case 458:
					// line 1683 "DefaultRubyParser.y"
  {
	          yyVal = null;
	      }
  break;
case 459:
					// line 1687 "DefaultRubyParser.y"
  {
                  if (!(((Node)yyVals[0+yyTop]) instanceof SelfNode)) {
		      support.checkExpression(((Node)yyVals[0+yyTop]));
		  }
		  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 460:
					// line 1693 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_BEG);
              }
  break;
case 461:
					// line 1695 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-2+yyTop]) instanceof ILiteralNode) {
                      yyerror("Can't define single method for literals.");
                  }
		  support.checkExpression(((Node)yyVals[-2+yyTop]));
                  yyVal = ((Node)yyVals[-2+yyTop]);
              }
  break;
case 462:
					// line 1705 "DefaultRubyParser.y"
  { /* [!null]*/
                  yyVal = new ArrayNode(getPosition(null));
              }
  break;
case 463:
					// line 1708 "DefaultRubyParser.y"
  { /* [!null]*/
                  yyVal = ((ListNode)yyVals[-1+yyTop]);
              }
  break;
case 464:
					// line 1711 "DefaultRubyParser.y"
  {
                  if (((ListNode)yyVals[-1+yyTop]).size() % 2 != 0) {
                      yyerror("Odd number list for Hash.");
                  }
                  yyVal = ((ListNode)yyVals[-1+yyTop]);
              }
  break;
case 466:
					// line 1720 "DefaultRubyParser.y"
  { /* [!null]*/
                  yyVal = ((ListNode)yyVals[-2+yyTop]).addAll(((ListNode)yyVals[0+yyTop]));
              }
  break;
case 467:
					// line 1725 "DefaultRubyParser.y"
  { /* [!null]*/
                  yyVal = new ArrayNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop])).add(((Node)yyVals[0+yyTop]));
              }
  break;
case 487:
					// line 1737 "DefaultRubyParser.y"
  {
                  yyerrok();
              }
  break;
case 490:
					// line 1743 "DefaultRubyParser.y"
  {
                  yyerrok();
              }
  break;
case 491:
					// line 1747 "DefaultRubyParser.y"
  {
                  yyVal = null;
              }
  break;
case 492:
					// line 1751 "DefaultRubyParser.y"
  {  
                  yyVal = null;
	      }
  break;
					// line 7424 "-"
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

					// line 1756 "DefaultRubyParser.y"

    /** The parse method use an lexer stream and parse it to an AST node 
     * structure
     */
    public RubyParserResult parse(RubyParserConfiguration configuration, LexerSource source) {
        support.reset();
        support.setConfiguration(configuration);
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
					// line 7504 "-"
