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
 * Copyright (C) 2007 Mirko Stocker <me@misto.ch>
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
import org.jruby.ast.PreExeNode;
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
import org.jruby.ast.WhenNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.ZArrayNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.ast.ZeroArgNode;
import org.jruby.ast.types.ILiteralNode;
import org.jruby.common.IRubyWarnings;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.ISourcePositionHolder;
import org.jruby.lexer.yacc.LexerSource;
import org.jruby.lexer.yacc.RubyYaccLexer;
import org.jruby.lexer.yacc.RubyYaccLexer.LexState;
import org.jruby.lexer.yacc.StrTerm;
import org.jruby.lexer.yacc.SyntaxException;
import org.jruby.lexer.yacc.Token;
import org.jruby.runtime.Visibility;
import org.jruby.util.ByteList;

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
					// line 151 "-"
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
//yyLhs 494
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
    45,    94,    94,    94,    94,    94,    63,    63,    46,    64,
    64,    97,    97,    95,    95,    98,    98,    75,    74,    74,
     1,   135,     1,    70,    70,    70,    68,    68,    69,    88,
    88,    88,    89,    89,    89,    89,    90,    90,    90,    96,
    96,   100,   100,   107,   107,   108,   108,   108,   122,   122,
   101,   101,    60,    73,
    }, yyLen = {
//yyLen 494
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
     0,     1,     1,     1,     1,     1,     1,     3,     3,     1,
     3,     1,     1,     2,     1,     1,     1,     2,     2,     0,
     1,     0,     5,     1,     2,     2,     1,     3,     3,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     0,     1,     0,     1,     0,     1,     1,     1,     1,
     1,     2,     0,     0,
    }, yyDefRed = {
//yyDefRed 885
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
   129,   116,   117,   461,   121,   120,   107,   125,   123,   122,
   118,   119,   114,   112,   105,   106,   130,     0,   460,   312,
    98,    99,   160,   153,   163,   148,   131,   132,    95,    96,
     0,     0,   102,   101,   100,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   489,   488,     0,     0,
     0,   490,     0,     0,     0,     0,     0,     0,   333,   334,
     0,     0,     0,     0,     0,   229,    45,     0,     0,     0,
   466,   237,    46,    44,     0,    59,     0,     0,   348,    58,
    38,     0,     9,   484,     0,     0,     0,   192,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   217,     0,     0,   463,     0,     0,     0,     0,     0,
     0,    68,     0,   208,    39,   207,   404,   403,   405,     0,
   401,   402,     0,     0,     0,     0,     0,     0,     0,   374,
   352,   350,   290,     4,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   339,   341,
     0,     0,     0,     0,     0,     0,    72,     0,     0,     0,
     0,     0,     0,   344,     0,   288,     0,   409,   410,     0,
    90,     0,    92,     0,   427,   305,   426,     0,     0,     0,
     0,     0,   479,   480,   314,     0,   103,     0,     0,   274,
     0,   324,   323,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   491,     0,     0,     0,
     0,     0,     0,   302,     0,   257,     0,     0,   230,   259,
     0,   232,   285,     0,     0,   252,   251,     0,     0,     0,
     0,     0,    11,    13,    12,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   277,     0,     0,     0,
   218,   281,     0,   486,   219,     0,   221,     0,   465,   464,
   282,     0,     0,     0,     0,   392,   395,   393,   406,   391,
   375,   389,   376,   377,   378,   379,   382,     0,   384,     0,
   385,     0,     0,     0,    15,    16,    17,    18,    19,    36,
    37,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   474,
     0,     0,   475,     0,     0,     0,     0,   347,     0,     0,
   472,   473,     0,     0,    30,     0,     0,    23,     0,    31,
   260,     0,     0,    66,    73,    24,    33,     0,    25,     0,
    50,    53,     0,   429,     0,     0,     0,     0,     0,     0,
    94,     0,     0,     0,     0,     0,   443,   442,   441,   444,
     0,   452,   451,   456,   455,     0,     0,   449,     0,     0,
   439,   446,     0,     0,     0,     0,   363,     0,     0,   364,
     0,     0,   331,     0,   325,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   300,   328,   327,
   294,   326,   297,     0,     0,     0,     0,     0,     0,     0,
   236,   468,     0,     0,     0,   258,     0,     0,   467,   284,
     0,     0,   255,     0,     0,   249,     0,     0,     0,     0,
     0,   223,     0,    10,     0,     0,    22,     0,     0,     0,
     0,     0,   222,     0,   261,     0,     0,     0,     0,     0,
     0,     0,   381,   383,   387,   337,     0,     0,   335,     0,
     0,     0,     0,     0,   228,     0,   345,   227,     0,     0,
   346,     0,     0,    48,   342,    49,   343,   264,     0,     0,
    71,     0,   308,     0,     0,   280,   311,     0,   315,     0,
     0,     0,   431,     0,   435,     0,   437,     0,   438,   453,
   457,   104,     0,     0,   366,   332,     0,     3,   368,     0,
   329,     0,     0,     0,     0,     0,     0,   299,   301,   357,
     0,     0,     0,     0,     0,     0,     0,     0,   234,     0,
     0,     0,     0,     0,   242,   254,   224,     0,     0,   225,
     0,     0,   287,    21,   276,     0,     0,     0,   397,   398,
   399,   394,   400,   336,     0,     0,     0,     0,     0,    27,
     0,    28,     0,    55,    29,     0,     0,    57,     0,     0,
     0,     0,     0,     0,   428,   306,   462,     0,   448,     0,
   313,     0,   458,   447,     0,     0,   450,     0,     0,     0,
     0,   365,     0,     0,   367,     0,   291,     0,   292,     0,
     0,     0,     0,   303,   231,     0,   233,   248,   256,     0,
     0,     0,   239,     0,     0,   220,   396,   338,   353,   351,
   340,    26,     0,   263,     0,     0,     0,   430,     0,   433,
   434,   436,     0,     0,     0,     0,     0,     0,   356,   358,
   354,   359,   295,   298,     0,     0,     0,     0,   238,     0,
   244,     0,   226,    51,   309,     0,     0,     0,     0,     0,
     0,     0,   360,     0,     0,   235,   240,     0,     0,     0,
   243,   316,   432,     0,   330,   304,     0,     0,   245,     0,
   241,     0,   246,     0,   247,
    }, yyDgoto = {
//yyDgoto 136
     1,   187,    60,    61,    62,    63,    64,   292,   289,   459,
    65,    66,    67,   467,    68,    69,    70,   108,    71,   205,
   206,    73,    74,    75,    76,    77,    78,   209,   258,   710,
   840,   711,   703,   236,   622,   416,   707,   664,   365,   245,
    80,   666,    81,    82,   565,   566,   567,   201,   751,   211,
   529,    84,    85,   237,   395,   578,   270,   227,   657,   212,
    87,   298,   296,   568,   569,   272,   596,    88,   273,   240,
   277,   408,   615,   409,   694,   782,   355,   339,   541,    89,
    90,   266,   378,   213,   214,   202,   290,    93,   113,   546,
   517,   114,   204,   512,   571,   572,   374,   573,   574,     2,
   219,   220,   425,   255,   681,   191,   575,   254,   444,   246,
   626,   731,   438,   383,   222,   600,   722,   223,   723,   608,
   844,   545,   384,   542,   773,   370,   375,   554,   777,   507,
   472,   471,   651,   650,   544,   371,
    }, yySindex = {
//yySindex 885
     0,     0,  4261, 13296, 16171, 16516, 17081, 16971,  4261,  5744,
  5744,  4658,     0,     0, 16286, 13411, 13411,     0,     0, 13411,
  -241,  -231,     0,     0,     0,     0,  5744, 16861,   126,     0,
  -192,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0, 15826, 15826,   -80,  -140,  5219,  5744, 14791,
 15826, 16631, 15826, 15941, 17190,     0,     0,     0,   151,   178,
     0,  -144,     0,     0,     0,  -224,     0,     0,     0,     0,
     0,     0,     0,    95,   398,    43,  2849,     0,   -81,   -42,
     0,  -230,     0,  -112,   206,     0,   193,     0, 16401,   204,
     0,   -72,     0,     0,  -197,   398,     0,     0,     0,  -241,
  -231,   126,     0,     0,   -38,  5744,  -110,  4261,    35,     0,
   372,     0,     0,  -197,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  -146,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
 17190,   225,     0,     0,     0,    19,    26,   -25,    43,    72,
   484,   -16,   264,    32,     0,    72,     0,     0,    95,   -58,
   281,     0,  5744,  5744,   142,   494,     0,   179,     0,     0,
     0, 15826, 15826, 15826,  2849,     0,     0,   130,   431,   459,
     0,     0,     0,     0,  3337,     0, 13526, 13411,     0,     0,
     0,   301,     0,     0, 14906,   146,  4261,     0,   549,   211,
   221,   231,   173,  5219,   217,     0,   229,    43, 15826,   126,
   243,     0,    76,    99,     0,   145,    99,   199,   269,   586,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  -222,
     0,     0,  -209,  -198,  -100,   210,  -166,   219,  -242,     0,
     0,     0,     0,     0, 13181,  5744,  5744,  5744,  5744, 13296,
  5744,  5744, 15826, 15826, 15826, 15826, 15826, 15826, 15826, 15826,
 15826, 15826, 15826, 15826, 15826, 15826, 15826, 15826, 15826, 15826,
 15826, 15826, 15826, 15826, 15826, 15826, 15826, 15826,     0,     0,
  2281,  5812, 14791, 17358, 17358, 15941,     0, 15021,  5219, 16631,
   562, 15021, 15941,     0,   261,     0,   274,     0,     0,    43,
     0,     0,     0,    95,     0,     0,     0, 17358, 17417, 14791,
  4261,  5744,     0,     0,     0,  1014,     0, 15136,   354,     0,
   173,     0,     0,  4261,   358, 17476, 17535, 14791, 15826, 15826,
 15826,  4261,   359,  4261, 15251,   368,     0,   119,   119,     0,
 17594, 17653, 14791,     0,   590,     0, 15826, 13641,     0,     0,
 13756,     0,     0,   294,  3817,     0,     0,   -81,   126,    29,
   303,   607,     0,     0,     0, 16971,  5744,  2849,  4261,   292,
 17476, 17535, 15826, 15826, 15826,   317,     0,     0,   126,  3150,
     0,     0, 15366,     0,     0, 15826,     0, 15826,     0,     0,
     0,     0, 17712, 17771, 14791,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,    70,     0,   629,
     0,  -160,  -160,   398,     0,     0,     0,     0,     0,     0,
     0,   211,  3663,  3663,  3663,  3663,  1583,  1583,  4720,  2785,
  3663,  3663,  2209,  2209,   663,   663,   211,  1057,   211,   211,
  -129,  -129,  1583,  1583,  1225,  1225,  5070,  -160,   330,     0,
   336,  -231,     0,   339,     0,   349,  -231,     0,     0,   350,
     0,     0,  -231,  -231,     0,  2849, 15826,     0,  2361,     0,
     0,   656,   365,     0,     0,     0,     0,     0,     0,  2849,
     0,     0,    95,     0,  5744,  4261,  -231,     0,     0,  -231,
     0,   369,   452,   189, 17299,   661,     0,     0,     0,     0,
  1021,     0,     0,     0,     0,  4261,    95,     0,   682,   683,
     0,     0,   686,   428,   433, 16971,     0,     0,   395,     0,
  4261,   475,     0,   312,     0,   401,   402,   405,   349,   399,
  2361,   354,   486,   488, 15826,   704,    72,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   410,  5744,   409,
     0,     0, 15826,   130,   725,     0, 15826,   130,     0,     0,
 15826,  2849,     0,   105,   728,     0,   432,   434, 17358, 17358,
   442,     0, 13871,     0,  -163,   437,     0,   211,   211,  2849,
     0,   453,     0, 15826,     0,     0,     0,     0,     0,   451,
  4261,   285,     0,     0,     0,     0, 16056,  4261,     0,  4261,
 15826,  4261, 15941, 15941,     0,   261,     0,     0, 15941, 15826,
     0,   261,   435,     0,     0,     0,     0,     0, 15826, 15481,
     0,  -160,     0,    95,   543,     0,     0,   470,     0, 15826,
   126,   552,     0,  1021,     0,   332,     0,   247,     0,     0,
     0,     0, 16746,    72,     0,     0,  4261,     0,     0,  5744,
     0,   555, 15826, 15826, 15826,   491,   561,     0,     0,     0,
 15596,  4261,  4261,  4261,     0,   119,   590, 13986,     0,   590,
   590,   498, 14101, 14216,     0,     0,     0,  -231,  -231,     0,
   -81,    29,     0,     0,     0,  3150,     0,   465,     0,     0,
     0,     0,     0,     0,   473,   578,   489,  2849,   591,     0,
  2849,     0,  2849,     0,     0,  2849,  2849,     0, 15941,  2849,
 15826,     0,  4261,  4261,     0,     0,     0,  1014,     0,   513,
     0,   813,     0,     0,   686,   661,     0,   686,   557,   619,
     0,     0,     0,  4261,     0,    72,     0, 15826,     0, 15826,
    25,   599,   605,     0,     0, 15826,     0,     0,     0, 15826,
   826,   833,     0, 15826,   544,     0,     0,     0,     0,     0,
     0,     0,  2849,     0,   526,   636,  4261,     0,   332,     0,
     0,     0,     0, 17830, 17889, 14791,    19,  4261,     0,     0,
     0,     0,     0,     0,  4261,  4767,   590, 14331,     0, 14446,
     0,   590,     0,     0,     0,   642,   686,     0,     0,     0,
     0,   546,     0,   312,   645,     0,     0, 15826,   849, 15826,
     0,     0,     0,     0,     0,     0,   590, 14561,     0,   590,
     0, 15826,     0,   590,     0,
    }, yyRindex = {
//yyRindex 885
     0,     0,   134,     0,     0,     0,     0,     0,   272,     0,
     0,   226,     0,     0,     0,  7501,  7606,     0,     0,  7715,
  4532,  3937,     0,     0,     0,     0,     0,     0, 15711,     0,
     0,     0,     0,  2015,  3092,     0,     0,  2132,     0,     0,
     0,     0,     0,     0,     0,     0,     0,    37,     0,   573,
   556,   131,     0,     0,   241,     0,     0,     0,   246,  -214,
     0,  6797,     0,     0,     0,  6902,     0,     0,     0,     0,
     0,     0,     0,   260,   736,  2256,  1723,  7011,  1834,     0,
     0, 12102,     0,  8648,     0,     0,     0,     0,   205,     0,
     0,     0,  1890,     0, 14676,  1042,     0,     0,     0,  7150,
  5932,   579, 12247, 12365,     0,     0,     0,    37,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   721,
  1423,  1733,  2739,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  3219,  3642,  3699,     0,  4122,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  2730,     0,     0,     0,    23,     0,     0, 10906,     0,
     0,  6446,     0,     0,  6035,     0,     0,     0,   654,     0,
   249,     0,     0,     0,     0,     0,   325,     0,     0,     0,
   463,     0,     0,     0,  1616,     0,     0, 11753,  6386,  6386,
     0,     0,     0,     0,     0,     0,     0,   587,     0,     0,
     0,     0,     0,     0,     0,     0,    12,     0,     0,  7835,
  7253,  7364,  8765,    37,     0,    18,     0,    53,     0,   583,
     0,     0,   588,   588,     0,   567,   567,     0,     0,     0,
   628,     0,   817,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   791,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   573,     0,     0,     0,     0,     0,    37,   209,
   223,     0,     0,     0, 11971,     0,     0,     0,     0,   137,
     0, 12715,     0,     0,     0,     0,     0,     0,     0,   573,
   272,     0,     0,     0,     0,   169,     0,    98,    84,     0,
  6549,     0,     0,   490, 12831,     0,     0,   573,     0,     0,
     0,   523,     0,   185,     0,     0,     0,     0,     0,   566,
     0,     0,   573,     0,  6386,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   589,     0,     0,    55,   602,   602,
     0,    62,     0,     0,     0,     0,     0, 10993,    12,     0,
     0,     0,     0,     0,     0,     0,     0,    74,   602,   583,
     0,     0,   604,     0,     0,   -84,     0,   584,     0,     0,
     0,  1061,     0,     0,   573,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 12947, 13065,  1084,     0,     0,     0,     0,     0,     0,
     0,  7944, 10213, 10321, 10408, 10511,  9771,  9879, 10599, 10864,
 10686, 10776,  6283, 10951,  9208,  9319,  8049,  9437,  8186,  8297,
  8982,  9103,  9987, 10105,  9545,  9653,     0, 12947,  4895,     0,
  5010,  4052,     0,  5375,  3457,  5490, 14676,     0,  3572,     0,
     0,     0,  5617,  5617,     0, 11085,     0,     0, 10229,     0,
     0,     0,     0,     0,     0,     0,     0,  1704,     0, 11172,
     0,     0,     0,     0,     0,   272,  6139, 12481, 12597,     0,
     0,     0,     0,   602,     0,    34,     0,     0,     0,     0,
    63,     0,     0,     0,     0,   272,     0,     0,    54,    54,
     0,     0,    54,    47,     0,     0,     0,    82,    79,     0,
   515,   687,     0,   687,     0,  2497,  2612,  2977,  4417,     0,
 11847,   687,     0,     0,     0,   220,     0,     0,     0,     0,
     0,     0,     0,   924,  1059,  1506,   948,     0,     0,     0,
     0,     0,     0, 11884,  6386,     0,     0,     0,     0,     0,
     0,   138,     0,     0,   609,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  8400,  8523, 11259,
     8,     0,     0,     0,     0,   902,  1107,  1288,   391,     0,
    12,     0,     0,     0,     0,     0,     0,   185,     0,    12,
     0,   185,     0,     0,     0, 12008,     0,     0,     0,     0,
     0, 12065,  8874,     0,     0,     0,     0,     0,     0,     0,
     0, 13065,     0,     0,     0,     0,     0,     0,     0,     0,
   602,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   185,     0,     0,     0,
     0,     0,     0,     0,     0,  6660,     0,     0,     0,     0,
     0,   517,   185,   185,   767,     0,  6386,     0,     0,  6386,
   609,     0,     0,     0,     0,     0,     0,    78,    78,     0,
     0,   602,     0,     0,     0,   583,  1123,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0, 11300,     0,     0,
 11363,     0, 11450,     0,     0, 11487, 11577,     0,     0, 11618,
     0, 12154,    12,   272,     0,     0,     0,   169,     0,     0,
     0,    54,     0,     0,    54,     0,     0,    54,     0,     0,
   480,     0,   568,   272,     0,     0,     0,     0,     0,     0,
   687,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   609,   609,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0, 11716,     0,     0,     0,   272,     0,     0,     0,
     0,     0,   648,     0,     0,   573,    23,   490,     0,     0,
     0,     0,     0,     0,   185,  6386,   609,     0,     0,     0,
     0,   609,     0,     0,     0,     0,    54,   114,   509,   580,
   111,     0,     0,   687,     0,     0,     0,     0,   609,     0,
     0,     0,     0,   666,     0,     0,   609,     0,     0,   609,
     0,     0,     0,   609,     0,
    }, yyGindex = {
//yyGindex 136
     0,     0,     0,     0,   892,     0,     0,     0,   585,  -216,
     0,     0,     0,     0,     0,     0,     0,   949,     5,  1088,
  -309,     0,    51,   964,   -15,     9,     1,   106,   457,  -367,
     0,    92,     0,   885,     0,     0,     0,    10,     0,    21,
   951,    33,  -228,     0,   187,   406,  -640,     0,     0,   238,
  -252,   874,    91,  1390,  -380,     0,  -321,   311,  -388,   835,
  1308,     0,     0,     0,   276,    -9,     0,     0,    17,  -392,
     0,   198,    11,     0,  -295,  -351,   911,     0,  -484,   -12,
    -4,  -195,   141,  1033,   933,   -18,     0,    -2,   865,  -182,
     0,   -44,    -3,    68,   286,  -608,     0,     0,     0,     0,
   -33,   908,     0,     0,     0,     0,     0,   -55,   240,     0,
     0,     0,     0,  -207,     0,  -339,     0,     0,     0,     0,
     0,     0,     4,     0,     0,     0,     0,     0,     0,     0,
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
					// line 260 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_BEG);
                  support.initTopLocalVariables();
              }
  break;
case 2:
					// line 263 "DefaultRubyParser.y"
  {
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
					// line 275 "DefaultRubyParser.y"
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
					// line 291 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-1+yyTop]) instanceof BlockNode) {
                      support.checkUselessStatements(((BlockNode)yyVals[-1+yyTop]));
		  }
                  yyVal = ((Node)yyVals[-1+yyTop]);
              }
  break;
case 6:
					// line 299 "DefaultRubyParser.y"
  {
                  yyVal = support.newline_node(((Node)yyVals[0+yyTop]), getPosition(((Node)yyVals[0+yyTop]), true));
              }
  break;
case 7:
					// line 302 "DefaultRubyParser.y"
  {
	          yyVal = support.appendToBlock(((Node)yyVals[-2+yyTop]), support.newline_node(((Node)yyVals[0+yyTop]), getPosition(((Node)yyVals[0+yyTop]), true)));
              }
  break;
case 8:
					// line 305 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 9:
					// line 309 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_FNAME);
              }
  break;
case 10:
					// line 311 "DefaultRubyParser.y"
  {
                  yyVal = new AliasNode(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 11:
					// line 314 "DefaultRubyParser.y"
  {
                  yyVal = new VAliasNode(getPosition(((Token)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 12:
					// line 317 "DefaultRubyParser.y"
  {
                  yyVal = new VAliasNode(getPosition(((Token)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), "$" + ((BackRefNode)yyVals[0+yyTop]).getType()); /* XXX*/
              }
  break;
case 13:
					// line 320 "DefaultRubyParser.y"
  {
                  yyerror("can't make alias for the number variables");
              }
  break;
case 14:
					// line 323 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 15:
					// line 326 "DefaultRubyParser.y"
  {
                  yyVal = new IfNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), null);
              }
  break;
case 16:
					// line 329 "DefaultRubyParser.y"
  {
                  yyVal = new IfNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), null, ((Node)yyVals[-2+yyTop]));
              }
  break;
case 17:
					// line 332 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                      yyVal = new WhileNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                  } else {
                      yyVal = new WhileNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                  }
              }
  break;
case 18:
					// line 339 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                      yyVal = new UntilNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                  } else {
                      yyVal = new UntilNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                  }
              }
  break;
case 19:
					// line 346 "DefaultRubyParser.y"
  {
	          yyVal = new RescueNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), new RescueBodyNode(getPosition(((Node)yyVals[-2+yyTop])), null,((Node)yyVals[0+yyTop]), null), null);
              }
  break;
case 20:
					// line 349 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
                      yyerror("BEGIN in method");
                  }
		  support.pushLocalScope();
              }
  break;
case 21:
					// line 354 "DefaultRubyParser.y"
  {
                  support.getResult().addBeginNode(new PreExeNode(getPosition(((Node)yyVals[-1+yyTop])), support.getCurrentScope(), ((Node)yyVals[-1+yyTop])));
                  support.popCurrentScope();
                  yyVal = null; /*XXX 0;*/
              }
  break;
case 22:
					// line 359 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
                      yyerror("END in method; use at_exit");
                  }
                  yyVal = new PostExeNode(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 23:
					// line 365 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
                  yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 24:
					// line 369 "DefaultRubyParser.y"
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
					// line 378 "DefaultRubyParser.y"
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
					// line 394 "DefaultRubyParser.y"
  {
                  yyVal = new OpElementAsgnNode(getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));

              }
  break;
case 27:
					// line 398 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 28:
					// line 401 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 29:
					// line 404 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 30:
					// line 407 "DefaultRubyParser.y"
  {
                  support.backrefAssignError(((Node)yyVals[-2+yyTop]));
              }
  break;
case 31:
					// line 410 "DefaultRubyParser.y"
  {
                  yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), new SValueNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
              }
  break;
case 32:
					// line 413 "DefaultRubyParser.y"
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
					// line 421 "DefaultRubyParser.y"
  {
                  ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
		  yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
                  ((MultipleAsgnNode)yyVals[-2+yyTop]).setPosition(support.union(((MultipleAsgnNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
	      }
  break;
case 36:
					// line 429 "DefaultRubyParser.y"
  {
                  yyVal = support.newAndNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 37:
					// line 432 "DefaultRubyParser.y"
  {
                  yyVal = support.newOrNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 38:
					// line 435 "DefaultRubyParser.y"
  {
                  yyVal = new NotNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
              }
  break;
case 39:
					// line 438 "DefaultRubyParser.y"
  {
                  yyVal = new NotNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
              }
  break;
case 41:
					// line 443 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
	      }
  break;
case 44:
					// line 449 "DefaultRubyParser.y"
  {
                  yyVal = new ReturnNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
              }
  break;
case 45:
					// line 452 "DefaultRubyParser.y"
  {
                  yyVal = new BreakNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
              }
  break;
case 46:
					// line 455 "DefaultRubyParser.y"
  {
                  yyVal = new NextNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
              }
  break;
case 48:
					// line 460 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 49:
					// line 463 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 50:
					// line 467 "DefaultRubyParser.y"
  {
                    support.pushBlockScope();
		}
  break;
case 51:
					// line 469 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                    support.popCurrentScope();
		}
  break;
case 52:
					// line 474 "DefaultRubyParser.y"
  {
                  yyVal = support.new_fcall(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 53:
					// line 477 "DefaultRubyParser.y"
  {
                  yyVal = support.new_fcall(((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])); 
              }
  break;
case 54:
					// line 480 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 55:
					// line 483 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])); 
	      }
  break;
case 56:
					// line 486 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 57:
					// line 489 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])); 
	      }
  break;
case 58:
					// line 492 "DefaultRubyParser.y"
  {
		  yyVal = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop])); /* .setPosFrom($2);*/
	      }
  break;
case 59:
					// line 495 "DefaultRubyParser.y"
  {
                  yyVal = support.new_yield(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
	      }
  break;
case 61:
					// line 500 "DefaultRubyParser.y"
  {
                  yyVal = ((MultipleAsgnNode)yyVals[-1+yyTop]);
	      }
  break;
case 63:
					// line 505 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((Token)yyVals[-2+yyTop])), new ArrayNode(getPosition(((Token)yyVals[-2+yyTop])), ((MultipleAsgnNode)yyVals[-1+yyTop])), null);
              }
  break;
case 64:
					// line 509 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((ListNode)yyVals[0+yyTop])), ((ListNode)yyVals[0+yyTop]), null);
              }
  break;
case 65:
					// line 512 "DefaultRubyParser.y"
  {
/*mirko: check*/
                  yyVal = new MultipleAsgnNode(support.union(((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), ((ListNode)yyVals[-1+yyTop]).add(((Node)yyVals[0+yyTop])), null);
                  ((Node)yyVals[-1+yyTop]).setPosition(support.union(((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])));
              }
  break;
case 66:
					// line 517 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((ListNode)yyVals[-2+yyTop])), ((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 67:
					// line 520 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((ListNode)yyVals[-1+yyTop])), ((ListNode)yyVals[-1+yyTop]), new StarNode(getPosition(null)));
              }
  break;
case 68:
					// line 523 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((Token)yyVals[-1+yyTop])), null, ((Node)yyVals[0+yyTop]));
              }
  break;
case 69:
					// line 526 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((Token)yyVals[0+yyTop])), null, new StarNode(getPosition(null)));
              }
  break;
case 71:
					// line 531 "DefaultRubyParser.y"
  {
                  yyVal = ((MultipleAsgnNode)yyVals[-1+yyTop]);
              }
  break;
case 72:
					// line 535 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(((Node)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 73:
					// line 538 "DefaultRubyParser.y"
  {
                  yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
              }
  break;
case 74:
					// line 542 "DefaultRubyParser.y"
  {
                  yyVal = support.assignable(((Token)yyVals[0+yyTop]), null);
              }
  break;
case 75:
					// line 545 "DefaultRubyParser.y"
  {
                  yyVal = support.aryset(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 76:
					// line 548 "DefaultRubyParser.y"
  {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 77:
					// line 551 "DefaultRubyParser.y"
  {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 78:
					// line 554 "DefaultRubyParser.y"
  {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 79:
					// line 557 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
		      yyerror("dynamic constant assignment");
		  }

		  ISourcePosition position = support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop]));

                  yyVal = new ConstDeclNode(position, null, new Colon2Node(position, ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue()), null);
	      }
  break;
case 80:
					// line 566 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
		      yyerror("dynamic constant assignment");
		  }

                  ISourcePosition position = support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop]));

                  yyVal = new ConstDeclNode(position, null, new Colon3Node(position, (String) ((Token)yyVals[0+yyTop]).getValue()), null);
	      }
  break;
case 81:
					// line 575 "DefaultRubyParser.y"
  {
	          support.backrefAssignError(((Node)yyVals[0+yyTop]));
              }
  break;
case 82:
					// line 579 "DefaultRubyParser.y"
  {
                  yyVal = support.assignable(((Token)yyVals[0+yyTop]), null);
              }
  break;
case 83:
					// line 582 "DefaultRubyParser.y"
  {
                  yyVal = support.aryset(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 84:
					// line 585 "DefaultRubyParser.y"
  {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 85:
					// line 588 "DefaultRubyParser.y"
  {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
 	      }
  break;
case 86:
					// line 591 "DefaultRubyParser.y"
  {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 87:
					// line 594 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
		      yyerror("dynamic constant assignment");
		  }
			
		  ISourcePosition position = support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop]));

                  yyVal = new ConstDeclNode(position, null, new Colon2Node(position, ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue()), null);
              }
  break;
case 88:
					// line 603 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
		      yyerror("dynamic constant assignment");
		  }

                  ISourcePosition position = support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop]));

                  yyVal = new ConstDeclNode(position, null, new Colon3Node(position, (String) ((Token)yyVals[0+yyTop]).getValue()), null);
	      }
  break;
case 89:
					// line 612 "DefaultRubyParser.y"
  {
                   support.backrefAssignError(((Node)yyVals[0+yyTop]));
	      }
  break;
case 90:
					// line 616 "DefaultRubyParser.y"
  {
                  yyerror("class/module name must be CONSTANT");
              }
  break;
case 92:
					// line 621 "DefaultRubyParser.y"
  {
                  yyVal = new Colon3Node(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
	      }
  break;
case 93:
					// line 624 "DefaultRubyParser.y"
  {
                  yyVal = new Colon2Node(((Token)yyVals[0+yyTop]).getPosition(), null, (String) ((Token)yyVals[0+yyTop]).getValue());
 	      }
  break;
case 94:
					// line 627 "DefaultRubyParser.y"
  {
                  yyVal = new Colon2Node(support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
	      }
  break;
case 98:
					// line 633 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_END);
                  yyVal = ((Token)yyVals[0+yyTop]);
              }
  break;
case 99:
					// line 638 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_END);
                  yyVal = yyVals[0+yyTop];
              }
  break;
case 102:
					// line 645 "DefaultRubyParser.y"
  {
                  yyVal = new UndefNode(getPosition(((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 103:
					// line 648 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_FNAME);
	      }
  break;
case 104:
					// line 650 "DefaultRubyParser.y"
  {
                  yyVal = support.appendToBlock(((Node)yyVals[-3+yyTop]), new UndefNode(getPosition(((Node)yyVals[-3+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue()));
              }
  break;
case 172:
					// line 669 "DefaultRubyParser.y"
  {
                  yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
		  /* FIXME: Consider fixing node_assign itself rather than single case*/
		  ((Node)yyVal).setPosition(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
              }
  break;
case 173:
					// line 674 "DefaultRubyParser.y"
  {
                  ISourcePosition position = support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                  yyVal = support.node_assign(((Node)yyVals[-4+yyTop]), new RescueNode(position, ((Node)yyVals[-2+yyTop]), new RescueBodyNode(position, null, ((Node)yyVals[0+yyTop]), null), null));
	      }
  break;
case 174:
					// line 678 "DefaultRubyParser.y"
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
case 175:
					// line 694 "DefaultRubyParser.y"
  {
                  yyVal = new OpElementAsgnNode(getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 176:
					// line 697 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 177:
					// line 700 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 178:
					// line 703 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 179:
					// line 706 "DefaultRubyParser.y"
  {
	          yyerror("constant re-assignment");
	      }
  break;
case 180:
					// line 709 "DefaultRubyParser.y"
  {
		  yyerror("constant re-assignment");
	      }
  break;
case 181:
					// line 712 "DefaultRubyParser.y"
  {
                  support.backrefAssignError(((Node)yyVals[-2+yyTop]));
              }
  break;
case 182:
					// line 715 "DefaultRubyParser.y"
  {
		  support.checkExpression(((Node)yyVals[-2+yyTop]));
		  support.checkExpression(((Node)yyVals[0+yyTop]));
    
                  boolean isLiteral = ((Node)yyVals[-2+yyTop]) instanceof FixnumNode && ((Node)yyVals[0+yyTop]) instanceof FixnumNode;
                  yyVal = new DotNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), false, isLiteral);
              }
  break;
case 183:
					// line 722 "DefaultRubyParser.y"
  {
		  support.checkExpression(((Node)yyVals[-2+yyTop]));
		  support.checkExpression(((Node)yyVals[0+yyTop]));
                  boolean isLiteral = ((Node)yyVals[-2+yyTop]) instanceof FixnumNode && ((Node)yyVals[0+yyTop]) instanceof FixnumNode;
                  yyVal = new DotNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), true, isLiteral);
              }
  break;
case 184:
					// line 728 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "+", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 185:
					// line 731 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "-", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 186:
					// line 734 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "*", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 187:
					// line 737 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "/", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 188:
					// line 740 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "%", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 189:
					// line 743 "DefaultRubyParser.y"
  {
		  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 190:
					// line 746 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), getPosition(null)), "-@");
              }
  break;
case 191:
					// line 749 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((FloatNode)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), getPosition(null)), "-@");
              }
  break;
case 192:
					// line 752 "DefaultRubyParser.y"
  {
                  if (support.isLiteral(((Node)yyVals[0+yyTop]))) {
		      yyVal = ((Node)yyVals[0+yyTop]);
		  } else {
                      yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "+@");
		  }
              }
  break;
case 193:
					// line 759 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "-@");
	      }
  break;
case 194:
					// line 762 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "|", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 195:
					// line 765 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "^", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 196:
					// line 768 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "&", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 197:
					// line 771 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=>", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 198:
					// line 774 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 199:
					// line 777 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">=", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 200:
					// line 780 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 201:
					// line 783 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 202:
					// line 786 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 203:
					// line 789 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "===", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 204:
					// line 792 "DefaultRubyParser.y"
  {
                  yyVal = new NotNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop]), getPosition(null)));
              }
  break;
case 205:
					// line 795 "DefaultRubyParser.y"
  {
                  yyVal = support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 206:
					// line 798 "DefaultRubyParser.y"
  {
                  yyVal = new NotNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
              }
  break;
case 207:
					// line 801 "DefaultRubyParser.y"
  {
                  yyVal = new NotNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
              }
  break;
case 208:
					// line 804 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "~");
              }
  break;
case 209:
					// line 807 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<<", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 210:
					// line 810 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">>", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 211:
					// line 813 "DefaultRubyParser.y"
  {
                  yyVal = support.newAndNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 212:
					// line 816 "DefaultRubyParser.y"
  {
                  yyVal = support.newOrNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 213:
					// line 819 "DefaultRubyParser.y"
  {
                  yyVal = new DefinedNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop]));
              }
  break;
case 214:
					// line 822 "DefaultRubyParser.y"
  {
                  yyVal = new IfNode(getPosition(((Node)yyVals[-4+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 215:
					// line 825 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 216:
					// line 829 "DefaultRubyParser.y"
  {
	          support.checkExpression(((Node)yyVals[0+yyTop]));
	          yyVal = ((Node)yyVals[0+yyTop]);   
	      }
  break;
case 218:
					// line 835 "DefaultRubyParser.y"
  {
                  warnings.warn(getPosition(((Node)yyVals[-1+yyTop])), "parenthesize argument(s) for future version");
                  yyVal = new ArrayNode(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 219:
					// line 839 "DefaultRubyParser.y"
  {
                  yyVal = ((ListNode)yyVals[-1+yyTop]);
              }
  break;
case 220:
					// line 842 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 221:
					// line 846 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition(((ListNode)yyVals[-1+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
              }
  break;
case 222:
					// line 849 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[-1+yyTop]));
		  yyVal = new NewlineNode(getPosition(((Token)yyVals[-2+yyTop])), new SplatNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])));
              }
  break;
case 223:
					// line 854 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
              }
  break;
case 224:
					// line 857 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[-2+yyTop]);
		  ((Node)yyVal).setPosition(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])));
              }
  break;
case 225:
					// line 861 "DefaultRubyParser.y"
  {
                  warnings.warn(getPosition(((Token)yyVals[-3+yyTop])), "parenthesize argument(s) for future version");
                  yyVal = new ArrayNode(getPosition(((Token)yyVals[-3+yyTop])), ((Node)yyVals[-2+yyTop]));
              }
  break;
case 226:
					// line 865 "DefaultRubyParser.y"
  {
                  warnings.warn(getPosition(((Token)yyVals[-5+yyTop])), "parenthesize argument(s) for future version");
                  yyVal = ((ListNode)yyVals[-4+yyTop]).add(((Node)yyVals[-2+yyTop]));
              }
  break;
case 229:
					// line 873 "DefaultRubyParser.y"
  {
                  warnings.warn(((Node)yyVals[0+yyTop]).getPosition(), "parenthesize argument(s) for future version");
                  yyVal = new ArrayNode(getPosition(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
              }
  break;
case 230:
					// line 877 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_blk_pass(((ListNode)yyVals[-1+yyTop]), ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 231:
					// line 880 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass(((Node)yyVal), ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 232:
					// line 884 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition(((ListNode)yyVals[-1+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 233:
					// line 888 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), new ArrayNode(getPosition(((ListNode)yyVals[-4+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 234:
					// line 892 "DefaultRubyParser.y"
  {
                  yyVal = ((ListNode)yyVals[-3+yyTop]).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 235:
					// line 896 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[-1+yyTop]));
		  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-6+yyTop])), ((ListNode)yyVals[-6+yyTop]).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 236:
					// line 901 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_blk_pass(new SplatNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 237:
					// line 904 "DefaultRubyParser.y"
  {}
  break;
case 238:
					// line 906 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_blk_pass(new ArrayNode(getPosition(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop])).addAll(((ListNode)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 239:
					// line 909 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_blk_pass(new ArrayNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 240:
					// line 912 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-4+yyTop])), new ArrayNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 241:
					// line 916 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-6+yyTop])), new ArrayNode(getPosition(((Node)yyVals[-6+yyTop])), ((Node)yyVals[-6+yyTop])).addAll(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 242:
					// line 920 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition(((ListNode)yyVals[-1+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 243:
					// line 924 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), new ArrayNode(getPosition(((ListNode)yyVals[-4+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 244:
					// line 928 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 245:
					// line 932 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop])).addAll(((ListNode)yyVals[-3+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 246:
					// line 936 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-6+yyTop])), new ArrayNode(getPosition(((Node)yyVals[-6+yyTop])), ((Node)yyVals[-6+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 247:
					// line 940 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-8+yyTop])), new ArrayNode(getPosition(((Node)yyVals[-8+yyTop])), ((Node)yyVals[-8+yyTop])).addAll(((ListNode)yyVals[-6+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 248:
					// line 944 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_blk_pass(new SplatNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 249:
					// line 947 "DefaultRubyParser.y"
  {}
  break;
case 250:
					// line 949 "DefaultRubyParser.y"
  { 
	          yyVal = new Long(lexer.getCmdArgumentState().begin());
	      }
  break;
case 251:
					// line 951 "DefaultRubyParser.y"
  {
                  lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 253:
					// line 957 "DefaultRubyParser.y"
  {                    
		  lexer.setState(LexState.EXPR_ENDARG);
	      }
  break;
case 254:
					// line 959 "DefaultRubyParser.y"
  {
                  warnings.warn(getPosition(((Token)yyVals[-2+yyTop])), "don't put space before argument parentheses");
	          yyVal = null;
	      }
  break;
case 255:
					// line 963 "DefaultRubyParser.y"
  {
		  lexer.setState(LexState.EXPR_ENDARG);
	      }
  break;
case 256:
					// line 965 "DefaultRubyParser.y"
  {
                  warnings.warn(getPosition(((Token)yyVals[-3+yyTop])), "don't put space before argument parentheses");
		  yyVal = ((Node)yyVals[-2+yyTop]);
	      }
  break;
case 257:
					// line 970 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
                  yyVal = new BlockPassNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
              }
  break;
case 258:
					// line 975 "DefaultRubyParser.y"
  {
                  yyVal = ((BlockPassNode)yyVals[0+yyTop]);
              }
  break;
case 260:
					// line 980 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition2(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
              }
  break;
case 261:
					// line 983 "DefaultRubyParser.y"
  {
                  yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
              }
  break;
case 262:
					// line 987 "DefaultRubyParser.y"
  {
		  yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
              }
  break;
case 263:
					// line 990 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
	      }
  break;
case 264:
					// line 993 "DefaultRubyParser.y"
  {  
                  yyVal = new SplatNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
	      }
  break;
case 273:
					// line 1005 "DefaultRubyParser.y"
  {
                  yyVal = new FCallNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue(), null);
	      }
  break;
case 274:
					// line 1008 "DefaultRubyParser.y"
  {
                  yyVal = new BeginNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-1+yyTop]));
	      }
  break;
case 275:
					// line 1011 "DefaultRubyParser.y"
  { 
                  lexer.setState(LexState.EXPR_ENDARG); 
              }
  break;
case 276:
					// line 1013 "DefaultRubyParser.y"
  {
		  warnings.warning(getPosition(((Token)yyVals[-4+yyTop])), "(...) interpreted as grouped expression");
                  yyVal = ((Node)yyVals[-3+yyTop]);
	      }
  break;
case 277:
					// line 1017 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-1+yyTop]) != null) {
                      /* compstmt position includes both parens around it*/
                      ((ISourcePositionHolder) ((Node)yyVals[-1+yyTop])).setPosition(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
                  }
		  yyVal = ((Node)yyVals[-1+yyTop]);
              }
  break;
case 278:
					// line 1024 "DefaultRubyParser.y"
  {
                  yyVal = new Colon2Node(support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 279:
					// line 1027 "DefaultRubyParser.y"
  {
                  yyVal = new Colon3Node(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 280:
					// line 1030 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-3+yyTop]) instanceof SelfNode) {
                      yyVal = new FCallNode(getPosition(((Node)yyVals[-3+yyTop])), "[]", ((Node)yyVals[-1+yyTop]));
                  } else {
                      yyVal = new CallNode(getPosition(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop]), "[]", ((Node)yyVals[-1+yyTop]));
                  }
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
                  yyVal = new HashNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((ListNode)yyVals[-1+yyTop]));
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
                  yyVal = new FCallNode(support.union(((Token)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), null, ((IterNode)yyVals[0+yyTop]));
              }
  break;
case 290:
					// line 1068 "DefaultRubyParser.y"
  {
	          if (((Node)yyVals[-1+yyTop]) != null && 
                      ((BlockAcceptingNode)yyVals[-1+yyTop]).getIterNode() instanceof BlockPassNode) {
                      throw new SyntaxException(getPosition(((Node)yyVals[-1+yyTop])), "Both block arg and actual block given.");
		  }
		  ((BlockAcceptingNode)yyVals[-1+yyTop]).setIterNode(((IterNode)yyVals[0+yyTop]));
		  ((Node)yyVals[-1+yyTop]).setPosition(support.union(((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])));
              }
  break;
case 291:
					// line 1076 "DefaultRubyParser.y"
  {
                  yyVal = new IfNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 292:
					// line 1079 "DefaultRubyParser.y"
  {
                  yyVal = new IfNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-2+yyTop]));
              }
  break;
case 293:
					// line 1082 "DefaultRubyParser.y"
  { 
                  lexer.getConditionState().begin();
	      }
  break;
case 294:
					// line 1084 "DefaultRubyParser.y"
  {
		  lexer.getConditionState().end();
	      }
  break;
case 295:
					// line 1086 "DefaultRubyParser.y"
  {
                  yyVal = new WhileNode(support.union(((Token)yyVals[-6+yyTop]), ((Token)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 296:
					// line 1089 "DefaultRubyParser.y"
  {
                  lexer.getConditionState().begin();
              }
  break;
case 297:
					// line 1091 "DefaultRubyParser.y"
  {
                  lexer.getConditionState().end();
              }
  break;
case 298:
					// line 1093 "DefaultRubyParser.y"
  {
                  yyVal = new UntilNode(getPosition(((Token)yyVals[-6+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 299:
					// line 1096 "DefaultRubyParser.y"
  {
                  yyVal = new CaseNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 300:
					// line 1099 "DefaultRubyParser.y"
  {
                  yyVal = new CaseNode(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])), null, ((Node)yyVals[-1+yyTop]));
              }
  break;
case 301:
					// line 1102 "DefaultRubyParser.y"
  {
		  yyVal = ((Node)yyVals[-1+yyTop]);
              }
  break;
case 302:
					// line 1105 "DefaultRubyParser.y"
  {
                  lexer.getConditionState().begin();
              }
  break;
case 303:
					// line 1107 "DefaultRubyParser.y"
  {
                  lexer.getConditionState().end();
              }
  break;
case 304:
					// line 1109 "DefaultRubyParser.y"
  {
                  yyVal = new ForNode(support.union(((Token)yyVals[-8+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-7+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-4+yyTop]));
              }
  break;
case 305:
					// line 1112 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
                      yyerror("class definition in method body");
                  }
		  support.pushLocalScope();
              }
  break;
case 306:
					// line 1117 "DefaultRubyParser.y"
  {
                  yyVal = new ClassNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), ((Colon3Node)yyVals[-4+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-3+yyTop]));
                  support.popCurrentScope();
              }
  break;
case 307:
					// line 1121 "DefaultRubyParser.y"
  {
                  yyVal = new Boolean(support.isInDef());
                  support.setInDef(false);
              }
  break;
case 308:
					// line 1124 "DefaultRubyParser.y"
  {
                  yyVal = new Integer(support.getInSingle());
                  support.setInSingle(0);
		  support.pushLocalScope();
              }
  break;
case 309:
					// line 1128 "DefaultRubyParser.y"
  {
                  yyVal = new SClassNode(support.union(((Token)yyVals[-7+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-5+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  support.popCurrentScope();
                  support.setInDef(((Boolean)yyVals[-4+yyTop]).booleanValue());
                  support.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
              }
  break;
case 310:
					// line 1134 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) { 
                      yyerror("module definition in method body");
                  }
		  support.pushLocalScope();
              }
  break;
case 311:
					// line 1139 "DefaultRubyParser.y"
  {
                  yyVal = new ModuleNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Colon3Node)yyVals[-3+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  support.popCurrentScope();
              }
  break;
case 312:
					// line 1143 "DefaultRubyParser.y"
  {
                  support.setInDef(true);
		  support.pushLocalScope();
              }
  break;
case 313:
					// line 1146 "DefaultRubyParser.y"
  {
                    /* NOEX_PRIVATE for toplevel */
                  yyVal = new DefnNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), new ArgumentNode(((Token)yyVals[-4+yyTop]).getPosition(), (String) ((Token)yyVals[-4+yyTop]).getValue()), ((ArgsNode)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]), Visibility.PRIVATE);
                  support.popCurrentScope();
                  support.setInDef(false);
              }
  break;
case 314:
					// line 1152 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_FNAME);
              }
  break;
case 315:
					// line 1154 "DefaultRubyParser.y"
  {
                  support.setInSingle(support.getInSingle() + 1);
		  support.pushLocalScope();
                  lexer.setState(LexState.EXPR_END); /* force for args */
              }
  break;
case 316:
					// line 1158 "DefaultRubyParser.y"
  {
                  yyVal = new DefsNode(support.union(((Token)yyVals[-8+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-7+yyTop]), new ArgumentNode(((Token)yyVals[-4+yyTop]).getPosition(), (String) ((Token)yyVals[-4+yyTop]).getValue()), ((ArgsNode)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  support.popCurrentScope();
                  support.setInSingle(support.getInSingle() - 1);
              }
  break;
case 317:
					// line 1163 "DefaultRubyParser.y"
  {
                  yyVal = new BreakNode(((Token)yyVals[0+yyTop]).getPosition());
              }
  break;
case 318:
					// line 1166 "DefaultRubyParser.y"
  {
                  yyVal = new NextNode(((Token)yyVals[0+yyTop]).getPosition());
              }
  break;
case 319:
					// line 1169 "DefaultRubyParser.y"
  {
                  yyVal = new RedoNode(((Token)yyVals[0+yyTop]).getPosition());
              }
  break;
case 320:
					// line 1172 "DefaultRubyParser.y"
  {
                  yyVal = new RetryNode(((Token)yyVals[0+yyTop]).getPosition());
              }
  break;
case 321:
					// line 1176 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
		  yyVal = ((Node)yyVals[0+yyTop]);
	      }
  break;
case 330:
					// line 1191 "DefaultRubyParser.y"
  {
/*mirko: support.union($<ISourcePositionHolder>1.getPosition(), getPosition($<ISourcePositionHolder>1)) ?*/
                  yyVal = new IfNode(getPosition(((Token)yyVals[-4+yyTop])), support.getConditionNode(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 332:
					// line 1197 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 334:
					// line 1202 "DefaultRubyParser.y"
  {}
  break;
case 336:
					// line 1205 "DefaultRubyParser.y"
  {
                  yyVal = new ZeroArgNode(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])));
              }
  break;
case 337:
					// line 1208 "DefaultRubyParser.y"
  {
                  yyVal = new ZeroArgNode(((Token)yyVals[0+yyTop]).getPosition());
	      }
  break;
case 338:
					// line 1211 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[-1+yyTop]);

		  /* Include pipes on multiple arg type*/
                  if (((Node)yyVals[-1+yyTop]) instanceof MultipleAsgnNode) {
		      ((Node)yyVals[-1+yyTop]).setPosition(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
		  } 
              }
  break;
case 339:
					// line 1220 "DefaultRubyParser.y"
  {
                  support.pushBlockScope();
	      }
  break;
case 340:
					// line 1222 "DefaultRubyParser.y"
  {
                  yyVal = new IterNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  support.popCurrentScope();
              }
  break;
case 341:
					// line 1227 "DefaultRubyParser.y"
  {
	          if (((Node)yyVals[-1+yyTop]) != null && 
                      ((BlockAcceptingNode)yyVals[-1+yyTop]).getIterNode() instanceof BlockPassNode) {
                      throw new SyntaxException(getPosition(((Node)yyVals[-1+yyTop])), "Both block arg and actual block given.");
                  }
		  ((BlockAcceptingNode)yyVals[-1+yyTop]).setIterNode(((IterNode)yyVals[0+yyTop]));
		  ((Node)yyVals[-1+yyTop]).setPosition(support.union(((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])));
              }
  break;
case 342:
					// line 1235 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 343:
					// line 1238 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 344:
					// line 1242 "DefaultRubyParser.y"
  {
                  yyVal = support.new_fcall(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 345:
					// line 1245 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 346:
					// line 1248 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 347:
					// line 1251 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop]), null, null);
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
                  yyVal = new ZSuperNode(((Token)yyVals[0+yyTop]).getPosition());
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
                  yyVal = new IterNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
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
                  yyVal = new IterNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  ((ISourcePositionHolder)yyVals[-5+yyTop]).setPosition(support.union(((ISourcePositionHolder)yyVals[-5+yyTop]), ((ISourcePositionHolder)yyVal)));
                  support.popCurrentScope();
              }
  break;
case 354:
					// line 1276 "DefaultRubyParser.y"
  {
                  yyVal = new WhenNode(support.union(((Token)yyVals[-4+yyTop]), support.unwrapNewlineNode(((Node)yyVals[-1+yyTop]))), ((ListNode)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 356:
					// line 1281 "DefaultRubyParser.y"
  {
                  yyVal = ((ListNode)yyVals[-3+yyTop]).add(new WhenNode(getPosition(((ListNode)yyVals[-3+yyTop])), ((Node)yyVals[0+yyTop]), null, null));
              }
  break;
case 357:
					// line 1284 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition(((Token)yyVals[-1+yyTop])), new WhenNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]), null, null));
              }
  break;
case 360:
					// line 1291 "DefaultRubyParser.y"
  {
                  Node node;
                  if (((Node)yyVals[-3+yyTop]) != null) {
                     node = support.appendToBlock(support.node_assign(((Node)yyVals[-3+yyTop]), new GlobalVarNode(getPosition(((Token)yyVals[-5+yyTop])), "$!")), ((Node)yyVals[-1+yyTop]));
                     if(((Node)yyVals[-1+yyTop]) != null) {
                        node.setPosition(support.unwrapNewlineNode(((Node)yyVals[-1+yyTop])).getPosition());
                     }
		  } else {
		     node = ((Node)yyVals[-1+yyTop]);
                  }
                  yyVal = new RescueBodyNode(getPosition(((Token)yyVals[-5+yyTop]), true), ((Node)yyVals[-4+yyTop]), node, ((RescueBodyNode)yyVals[0+yyTop]));
	      }
  break;
case 361:
					// line 1303 "DefaultRubyParser.y"
  {yyVal = null;}
  break;
case 362:
					// line 1305 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
	      }
  break;
case 365:
					// line 1311 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 367:
					// line 1316 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[0+yyTop]) != null) {
                      yyVal = ((Node)yyVals[0+yyTop]);
                  } else {
                      yyVal = new NilNode(getPosition(null));
                  }
              }
  break;
case 370:
					// line 1326 "DefaultRubyParser.y"
  {
                  /* FIXME: We may be intern'ing more than once.*/
                  yyVal = new SymbolNode(((Token)yyVals[0+yyTop]).getPosition(), ((String) ((Token)yyVals[0+yyTop]).getValue()).intern());
              }
  break;
case 372:
					// line 1332 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[0+yyTop]) instanceof EvStrNode) {
                      yyVal = new DStrNode(getPosition(((Node)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
                  } else {
                      yyVal = ((Node)yyVals[0+yyTop]);
                  }
	      }
  break;
case 374:
					// line 1341 "DefaultRubyParser.y"
  {
                  yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 375:
					// line 1345 "DefaultRubyParser.y"
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
					// line 1361 "DefaultRubyParser.y"
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
case 377:
					// line 1377 "DefaultRubyParser.y"
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
case 378:
					// line 1392 "DefaultRubyParser.y"
  {
                   yyVal = new ZArrayNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
	       }
  break;
case 379:
					// line 1395 "DefaultRubyParser.y"
  {
		   yyVal = ((ListNode)yyVals[-1+yyTop]);
                   ((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
	       }
  break;
case 380:
					// line 1400 "DefaultRubyParser.y"
  {
                   yyVal = new ArrayNode(getPosition(null));
	       }
  break;
case 381:
					// line 1403 "DefaultRubyParser.y"
  {
                   yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]) instanceof EvStrNode ? new DStrNode(getPosition(((ListNode)yyVals[-2+yyTop]))).add(((Node)yyVals[-1+yyTop])) : ((Node)yyVals[-1+yyTop]));
	       }
  break;
case 383:
					// line 1408 "DefaultRubyParser.y"
  {
                   yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
	       }
  break;
case 384:
					// line 1412 "DefaultRubyParser.y"
  {
                   yyVal = new ZArrayNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
	       }
  break;
case 385:
					// line 1415 "DefaultRubyParser.y"
  {
		   yyVal = ((ListNode)yyVals[-1+yyTop]);
                   ((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
	       }
  break;
case 386:
					// line 1420 "DefaultRubyParser.y"
  {
                   yyVal = new ArrayNode(getPosition(null));
	       }
  break;
case 387:
					// line 1423 "DefaultRubyParser.y"
  {
                   yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
	       }
  break;
case 388:
					// line 1427 "DefaultRubyParser.y"
  {
                   yyVal = new StrNode(((Token)yyVals[0+yyTop]).getPosition(), ByteList.create(""));
	       }
  break;
case 389:
					// line 1430 "DefaultRubyParser.y"
  {
                   yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
	       }
  break;
case 390:
					// line 1434 "DefaultRubyParser.y"
  {
		   yyVal = null;
	       }
  break;
case 391:
					// line 1437 "DefaultRubyParser.y"
  {
                   yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
	       }
  break;
case 392:
					// line 1441 "DefaultRubyParser.y"
  {
                   yyVal = ((Node)yyVals[0+yyTop]);
               }
  break;
case 393:
					// line 1444 "DefaultRubyParser.y"
  {
                   yyVal = lexer.getStrTerm();
		   lexer.setStrTerm(null);
		   lexer.setState(LexState.EXPR_BEG);
	       }
  break;
case 394:
					// line 1448 "DefaultRubyParser.y"
  {
		   lexer.setStrTerm(((StrTerm)yyVals[-1+yyTop]));
	           yyVal = new EvStrNode(support.union(((Token)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
	       }
  break;
case 395:
					// line 1452 "DefaultRubyParser.y"
  {
		   yyVal = lexer.getStrTerm();
		   lexer.setStrTerm(null);
		   lexer.setState(LexState.EXPR_BEG);
	       }
  break;
case 396:
					// line 1456 "DefaultRubyParser.y"
  {
		   lexer.setStrTerm(((StrTerm)yyVals[-2+yyTop]));

		   yyVal = support.newEvStrNode(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-1+yyTop]));
	       }
  break;
case 397:
					// line 1462 "DefaultRubyParser.y"
  {
                   yyVal = new GlobalVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
               }
  break;
case 398:
					// line 1465 "DefaultRubyParser.y"
  {
                   yyVal = new InstVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
               }
  break;
case 399:
					// line 1468 "DefaultRubyParser.y"
  {
                   yyVal = new ClassVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
               }
  break;
case 401:
					// line 1474 "DefaultRubyParser.y"
  {
                   lexer.setState(LexState.EXPR_END);
                   yyVal = ((Token)yyVals[0+yyTop]);
		   ((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])));
               }
  break;
case 406:
					// line 1482 "DefaultRubyParser.y"
  {
                   lexer.setState(LexState.EXPR_END);

		   /* DStrNode: :"some text #{some expression}"*/
                   /* StrNode: :"some text"*/
		   /* EvStrNode :"#{some expression}"*/
                   if (((Node)yyVals[-1+yyTop]) == null) {
                       yyerror("empty symbol literal");
                   }

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
case 408:
					// line 1509 "DefaultRubyParser.y"
  {
                   yyVal = ((FloatNode)yyVals[0+yyTop]);
               }
  break;
case 409:
					// line 1512 "DefaultRubyParser.y"
  {
                   yyVal = support.negateInteger(((Node)yyVals[0+yyTop]));
	       }
  break;
case 410:
					// line 1515 "DefaultRubyParser.y"
  {
                   yyVal = support.negateFloat(((FloatNode)yyVals[0+yyTop]));
	       }
  break;
case 416:
					// line 1521 "DefaultRubyParser.y"
  { 
                   yyVal = new Token("nil", Tokens.kNIL, ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 417:
					// line 1524 "DefaultRubyParser.y"
  {
                   yyVal = new Token("self", Tokens.kSELF, ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 418:
					// line 1527 "DefaultRubyParser.y"
  { 
                   yyVal = new Token("true", Tokens.kTRUE, ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 419:
					// line 1530 "DefaultRubyParser.y"
  {
                   yyVal = new Token("false", Tokens.kFALSE, ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 420:
					// line 1533 "DefaultRubyParser.y"
  {
                   yyVal = new Token("__FILE__", Tokens.k__FILE__, ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 421:
					// line 1536 "DefaultRubyParser.y"
  {
                   yyVal = new Token("__LINE__", Tokens.k__LINE__, ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 422:
					// line 1540 "DefaultRubyParser.y"
  {
                   yyVal = support.gettable(((Token)yyVals[0+yyTop]));
               }
  break;
case 423:
					// line 1544 "DefaultRubyParser.y"
  {
                   yyVal = support.assignable(((Token)yyVals[0+yyTop]), null);
               }
  break;
case 426:
					// line 1550 "DefaultRubyParser.y"
  {
                   yyVal = null;
               }
  break;
case 427:
					// line 1553 "DefaultRubyParser.y"
  {
                   lexer.setState(LexState.EXPR_BEG);
               }
  break;
case 428:
					// line 1555 "DefaultRubyParser.y"
  {
                   yyVal = ((Node)yyVals[-1+yyTop]);
               }
  break;
case 429:
					// line 1558 "DefaultRubyParser.y"
  {
                   yyerrok();
                   yyVal = null;
               }
  break;
case 430:
					// line 1564 "DefaultRubyParser.y"
  {
                   yyVal = ((Node)yyVals[-2+yyTop]);
                   ((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])));
                   lexer.setState(LexState.EXPR_BEG);
               }
  break;
case 431:
					// line 1569 "DefaultRubyParser.y"
  {
                   yyVal = ((Node)yyVals[-1+yyTop]);
               }
  break;
case 432:
					// line 1573 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(support.union(((ListNode)yyVals[-5+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), support.getRestArgNode(((Token)yyVals[-1+yyTop])), ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 433:
					// line 1576 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), -1, null, ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 434:
					// line 1579 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(support.union(((ListNode)yyVals[-3+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), ((ListNode)yyVals[-3+yyTop]), null, ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), support.getRestArgNode(((Token)yyVals[-1+yyTop])), ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 435:
					// line 1582 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(((ISourcePositionHolder)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, -1, null, ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 436:
					// line 1585 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(getPosition(((ListNode)yyVals[-3+yyTop])), null, ((ListNode)yyVals[-3+yyTop]), ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), support.getRestArgNode(((Token)yyVals[-1+yyTop])), ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 437:
					// line 1588 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(getPosition(((ListNode)yyVals[-1+yyTop])), null, ((ListNode)yyVals[-1+yyTop]), -1, null, ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 438:
					// line 1591 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(getPosition(((Token)yyVals[-1+yyTop])), null, null, ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), support.getRestArgNode(((Token)yyVals[-1+yyTop])), ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 439:
					// line 1594 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(getPosition(((BlockArgNode)yyVals[0+yyTop])), null, null, -1, null, ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 440:
					// line 1597 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(support.createEmptyArgsNodePosition(getPosition(null)), null, null, -1, null, null);
               }
  break;
case 441:
					// line 1601 "DefaultRubyParser.y"
  {
                   yyerror("formal argument cannot be a constant");
               }
  break;
case 442:
					// line 1604 "DefaultRubyParser.y"
  {
                   yyerror("formal argument cannot be a global variable");
               }
  break;
case 443:
					// line 1607 "DefaultRubyParser.y"
  {
                   yyerror("formal argument cannot be an instance variable");
               }
  break;
case 444:
					// line 1610 "DefaultRubyParser.y"
  {
                   yyerror("formal argument cannot be a class variable");
               }
  break;
case 445:
					// line 1613 "DefaultRubyParser.y"
  {
                   String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();
                   if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                       yyerror("duplicate argument name");
                   }

		   support.getCurrentScope().getLocalScope().addVariable(identifier);
                   yyVal = ((Token)yyVals[0+yyTop]);
               }
  break;
case 446:
					// line 1623 "DefaultRubyParser.y"
  {
                    yyVal = new ListNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
                    ((ListNode) yyVal).add(new ArgumentNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue()));
               }
  break;
case 447:
					// line 1627 "DefaultRubyParser.y"
  {
                   ((ListNode)yyVals[-2+yyTop]).add(new ArgumentNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue()));
                   ((ListNode)yyVals[-2+yyTop]).setPosition(support.union(((ListNode)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
		   yyVal = ((ListNode)yyVals[-2+yyTop]);
               }
  break;
case 448:
					// line 1633 "DefaultRubyParser.y"
  {
                   String identifier = (String) ((Token)yyVals[-2+yyTop]).getValue();

                   if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                       yyerror("duplicate optional argument name");
                   }
		   support.getCurrentScope().getLocalScope().addVariable(identifier);
                   yyVal = support.assignable(((Token)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 449:
					// line 1643 "DefaultRubyParser.y"
  {
                  yyVal = new BlockNode(getPosition(((Node)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
              }
  break;
case 450:
					// line 1646 "DefaultRubyParser.y"
  {
                  yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 453:
					// line 1652 "DefaultRubyParser.y"
  {
                  String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();

                  if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                      yyerror("duplicate rest argument name");
                  }
		  ((Token)yyVals[-1+yyTop]).setValue(new Integer(support.getCurrentScope().getLocalScope().addVariable(identifier)));
                  yyVal = ((Token)yyVals[-1+yyTop]);
              }
  break;
case 454:
					// line 1661 "DefaultRubyParser.y"
  {
                  ((Token)yyVals[0+yyTop]).setValue(new Integer(support.getCurrentScope().getLocalScope().addVariable("*")));
                  yyVal = ((Token)yyVals[0+yyTop]);
              }
  break;
case 457:
					// line 1668 "DefaultRubyParser.y"
  {
                  String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();

                  if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                      yyerror("duplicate block argument name");
                  }
                  yyVal = new BlockArgNode(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), support.getCurrentScope().getLocalScope().addVariable(identifier), identifier);
              }
  break;
case 458:
					// line 1677 "DefaultRubyParser.y"
  {
                  yyVal = ((BlockArgNode)yyVals[0+yyTop]);
              }
  break;
case 459:
					// line 1680 "DefaultRubyParser.y"
  {
	          yyVal = null;
	      }
  break;
case 460:
					// line 1684 "DefaultRubyParser.y"
  {
                  if (!(((Node)yyVals[0+yyTop]) instanceof SelfNode)) {
		      support.checkExpression(((Node)yyVals[0+yyTop]));
		  }
		  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 461:
					// line 1690 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_BEG);
              }
  break;
case 462:
					// line 1692 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-2+yyTop]) instanceof ILiteralNode) {
                      yyerror("Can't define single method for literals.");
                  }
		  support.checkExpression(((Node)yyVals[-2+yyTop]));
                  yyVal = ((Node)yyVals[-2+yyTop]);
              }
  break;
case 463:
					// line 1702 "DefaultRubyParser.y"
  { /* [!null]*/
                  yyVal = new ArrayNode(getPosition(null));
              }
  break;
case 464:
					// line 1705 "DefaultRubyParser.y"
  { /* [!null]*/
                  yyVal = ((ListNode)yyVals[-1+yyTop]);
              }
  break;
case 465:
					// line 1708 "DefaultRubyParser.y"
  {
                  if (((ListNode)yyVals[-1+yyTop]).size() % 2 != 0) {
                      yyerror("Odd number list for Hash.");
                  }
                  yyVal = ((ListNode)yyVals[-1+yyTop]);
              }
  break;
case 467:
					// line 1717 "DefaultRubyParser.y"
  { /* [!null]*/
                  yyVal = ((ListNode)yyVals[-2+yyTop]).addAll(((ListNode)yyVals[0+yyTop]));
              }
  break;
case 468:
					// line 1722 "DefaultRubyParser.y"
  { /* [!null]*/
                  yyVal = new ArrayNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop])).add(((Node)yyVals[0+yyTop]));
              }
  break;
case 488:
					// line 1734 "DefaultRubyParser.y"
  {
                  yyerrok();
              }
  break;
case 491:
					// line 1740 "DefaultRubyParser.y"
  {
                  yyerrok();
              }
  break;
case 492:
					// line 1744 "DefaultRubyParser.y"
  {
                  yyVal = null;
              }
  break;
case 493:
					// line 1748 "DefaultRubyParser.y"
  {  
                  yyVal = null;
	      }
  break;
					// line 7452 "-"
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

					// line 1753 "DefaultRubyParser.y"

    /** The parse method use an lexer stream and parse it to an AST node 
     * structure
     */
    public RubyParserResult parse(ParserConfiguration configuration, LexerSource source) {
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
					// line 7532 "-"
