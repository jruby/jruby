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
import org.jruby.ast.ListNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.NilImplicitNode;
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
import org.jruby.ast.TypedArgumentNode;
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
					// line 155 "-"
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
//yyLhs 496
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
    45,    94,    94,    94,    94,    94,    63,    63,    63,    63,
    46,    64,    64,    97,    97,    95,    95,    98,    98,    75,
    74,    74,     1,   135,     1,    70,    70,    70,    68,    68,
    69,    88,    88,    88,    89,    89,    89,    89,    90,    90,
    90,    96,    96,   100,   100,   107,   107,   108,   108,   108,
   122,   122,   101,   101,    60,    73,
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
     0,     1,     1,     1,     1,     1,     3,     1,     5,     3,
     3,     1,     3,     1,     1,     2,     1,     1,     1,     2,
     2,     0,     1,     0,     5,     1,     2,     2,     1,     3,
     3,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     0,     1,     0,     1,     0,     1,     1,
     1,     1,     1,     2,     0,     0,
    }, yyDefRed = {
//yyDefRed 889
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
   129,   116,   117,   463,   121,   120,   107,   125,   123,   122,
   118,   119,   114,   112,   105,   106,   130,     0,   462,   312,
    98,    99,   160,   153,   163,   148,   131,   132,    95,    96,
     0,     0,   102,   101,   100,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   491,   490,     0,     0,
     0,   492,     0,     0,     0,     0,     0,     0,   333,   334,
     0,     0,     0,     0,     0,   229,    45,     0,     0,     0,
   468,   237,    46,    44,     0,    59,     0,     0,   348,    58,
    38,     0,     9,   486,     0,     0,     0,   192,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   217,     0,     0,   465,     0,     0,     0,     0,     0,
     0,    68,     0,   208,    39,   207,   404,   403,   405,     0,
   401,   402,     0,     0,     0,     0,     0,     0,     0,   374,
   352,   350,   290,     4,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   339,   341,
     0,     0,     0,     0,     0,     0,    72,     0,     0,     0,
     0,     0,     0,   344,     0,   288,     0,   409,   410,     0,
    90,     0,    92,     0,   427,   305,   426,     0,     0,     0,
     0,     0,   481,   482,   314,     0,   103,     0,     0,   274,
     0,   324,   323,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   493,     0,     0,     0,
     0,     0,     0,   302,     0,   257,     0,     0,   230,   259,
     0,   232,   285,     0,     0,   252,   251,     0,     0,     0,
     0,     0,    11,    13,    12,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   277,     0,     0,     0,
   218,   281,     0,   488,   219,     0,   221,     0,   467,   466,
   282,     0,     0,     0,     0,   392,   395,   393,   406,   391,
   375,   389,   376,   377,   378,   379,   382,     0,   384,     0,
   385,     0,     0,     0,    15,    16,    17,    18,    19,    36,
    37,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   476,
     0,     0,   477,     0,     0,     0,     0,   347,     0,     0,
   474,   475,     0,     0,    30,     0,     0,    23,     0,    31,
   260,     0,     0,    66,    73,    24,    33,     0,    25,     0,
    50,    53,     0,   429,     0,     0,     0,     0,     0,     0,
    94,     0,     0,     0,     0,     0,   443,   442,   441,   444,
     0,   454,   453,   458,   457,     0,     0,   451,     0,     0,
   439,     0,     0,     0,     0,     0,   363,     0,     0,   364,
     0,     0,   331,     0,   325,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   300,   328,   327,
   294,   326,   297,     0,     0,     0,     0,     0,     0,     0,
   236,   470,     0,     0,     0,   258,     0,     0,   469,   284,
     0,     0,   255,     0,     0,   249,     0,     0,     0,     0,
     0,   223,     0,    10,     0,     0,    22,     0,     0,     0,
     0,     0,   222,     0,   261,     0,     0,     0,     0,     0,
     0,     0,   381,   383,   387,   337,     0,     0,   335,     0,
     0,     0,     0,     0,   228,     0,   345,   227,     0,     0,
   346,     0,     0,    48,   342,    49,   343,   264,     0,     0,
    71,     0,   308,     0,     0,   280,   311,     0,   315,     0,
     0,     0,   431,     0,   435,     0,   437,     0,     0,   438,
   455,   459,   104,     0,     0,   366,   332,     0,     3,   368,
     0,   329,     0,     0,     0,     0,     0,     0,   299,   301,
   357,     0,     0,     0,     0,     0,     0,     0,     0,   234,
     0,     0,     0,     0,     0,   242,   254,   224,     0,     0,
   225,     0,     0,   287,    21,   276,     0,     0,     0,   397,
   398,   399,   394,   400,   336,     0,     0,     0,     0,     0,
    27,     0,    28,     0,    55,    29,     0,     0,    57,     0,
     0,     0,     0,     0,     0,   428,   306,   464,     0,   450,
     0,   313,     0,   460,     0,     0,     0,   452,     0,   446,
     0,     0,     0,   365,     0,     0,   367,     0,   291,     0,
   292,     0,     0,     0,     0,   303,   231,     0,   233,   248,
   256,     0,     0,     0,   239,     0,     0,   220,   396,   338,
   353,   351,   340,    26,     0,   263,     0,     0,     0,   430,
     0,   433,     0,   434,   436,     0,     0,     0,     0,     0,
     0,   356,   358,   354,   359,   295,   298,     0,     0,     0,
     0,   238,     0,   244,     0,   226,    51,   309,     0,     0,
   448,     0,     0,     0,     0,     0,   360,     0,     0,   235,
   240,     0,     0,     0,   243,   316,   432,     0,   330,   304,
     0,     0,   245,     0,   241,     0,   246,     0,   247,
    }, yyDgoto = {
//yyDgoto 136
     1,   187,    60,    61,    62,    63,    64,   292,   289,   459,
    65,    66,    67,   467,    68,    69,    70,   108,    71,   205,
   206,    73,    74,    75,    76,    77,    78,   209,   258,   711,
   843,   712,   704,   236,   622,   416,   708,   664,   365,   245,
    80,   666,    81,    82,   565,   566,   567,   201,   752,   211,
   529,    84,    85,   237,   395,   578,   270,   227,   657,   212,
    87,   298,   296,   568,   569,   272,   596,    88,   273,   240,
   277,   408,   615,   409,   694,   783,   355,   339,   541,    89,
    90,   266,   378,   213,   214,   202,   290,    93,   113,   546,
   517,   114,   204,   512,   571,   572,   374,   573,   574,     2,
   219,   220,   425,   255,   681,   191,   575,   254,   444,   246,
   626,   732,   438,   383,   222,   600,   723,   223,   724,   608,
   847,   545,   384,   542,   774,   370,   375,   554,   778,   507,
   472,   471,   651,   650,   544,   371,
    }, yySindex = {
//yySindex 889
     0,     0,  4307, 13318, 16078, 16423, 16988, 16878,  4307,  5790,
  5790,  4704,     0,     0, 16193,  4819,  4819,     0,     0,  4819,
  -279,  -209,     0,     0,     0,     0,  5790, 16768,   132,     0,
  -193,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0, 15733, 15733,   -52,  -125,  5265,  5790, 14698,
 15733, 16538, 15733, 15848, 17097,     0,     0,     0,   162,   167,
     0,  -157,     0,     0,     0,  -197,     0,     0,     0,     0,
     0,     0,     0,    69,   792,    -6,  3252,     0,   -62,   159,
     0,  -269,     0,  -112,   194,     0,   179,     0, 16308,   204,
     0,   -68,     0,     0,  -177,   792,     0,     0,     0,  -279,
  -209,   132,     0,     0,   169,  5790,  -170,  4307,    11,     0,
   160,     0,     0,  -177,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  -118,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
 17097,   231,     0,     0,     0,    19,    22,   -20,    -6,    91,
   452,   -43,   232,     2,     0,    91,     0,     0,    69,   -80,
   302,     0,  5790,  5790,    62,   492,     0,   117,     0,     0,
     0, 15733, 15733, 15733,  3252,     0,     0,   116,   455,   458,
     0,     0,     0,     0,  2423,     0, 13433,  4819,     0,     0,
     0,   -32,     0,     0, 14813,   142,  4307,     0,   520,   208,
   219,   228,   174,  5265,   238,     0,   252,    -6, 15733,   132,
   240,     0,   133,   163,     0,   178,   163,   187,   266,   541,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  -205,
     0,     0,  -172,   218,  -218,   224,   361,   225,  -217,     0,
     0,     0,     0,     0,  3778,  5790,  5790,  5790,  5790, 13318,
  5790,  5790, 15733, 15733, 15733, 15733, 15733, 15733, 15733, 15733,
 15733, 15733, 15733, 15733, 15733, 15733, 15733, 15733, 15733, 15733,
 15733, 15733, 15733, 15733, 15733, 15733, 15733, 15733,     0,     0,
  3846,  5136, 14698,  5858,  5858, 15848,     0, 14928,  5265, 16538,
   562, 14928, 15848,     0,   261,     0,   271,     0,     0,    -6,
     0,     0,     0,    69,     0,     0,     0,  5858,  9188, 14698,
  4307,  5790,     0,     0,     0,  1554,     0, 15043,   343,     0,
   174,     0,     0,  4307,   348, 17265, 17324, 14698, 15733, 15733,
 15733,  4307,   347,  4307, 15158,   357,     0,    54,    54,     0,
 17383, 17442, 14698,     0,   583,     0, 15733, 13548,     0,     0,
 13663,     0,     0,   286,  3380,     0,     0,   -62,   132,     1,
   288,   592,     0,     0,     0, 16878,  5790,  3252,  4307,   273,
 17265, 17324, 15733, 15733, 15733,   299,     0,     0,   132,  2236,
     0,     0, 15273,     0,     0, 15733,     0, 15733,     0,     0,
     0,     0, 17501, 17560, 14698,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,    53,     0,   614,
     0,  -167,  -167,   792,     0,     0,     0,     0,     0,     0,
     0,   208,  4346,  4346,  4346,  4346,  2289,  2289, 16079,  8616,
  4346,  4346,  2831,  2831,   682,   682,   208,   607,   208,   208,
   -91,   -91,  2289,  2289,   740,   740,  4156,  -167,   312,     0,
   318,  -209,     0,   320,     0,   322,  -209,     0,     0,   316,
     0,     0,  -209,  -209,     0,  3252, 15733,     0,  2887,     0,
     0,   625,   328,     0,     0,     0,     0,     0,     0,  3252,
     0,     0,    69,     0,  5790,  4307,  -209,     0,     0,  -209,
     0,   329,   412,   171, 17206,   617,     0,     0,     0,     0,
   758,     0,     0,     0,     0,  4307,    69,     0,   636,   641,
     0,   346,   646,   390,   392, 16878,     0,     0,   363,     0,
  4307,   434,     0,   272,     0,   362,   371,   372,   322,   366,
  2887,   343,   456,   468, 15733,   672,    91,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   376,  5790,   374,
     0,     0, 15733,   116,   693,     0, 15733,   116,     0,     0,
 15733,  3252,     0,    42,   701,     0,   393,   405,  5858,  5858,
   406,     0, 13778,     0,   -86,   389,     0,   208,   208,  3252,
     0,   414,     0, 15733,     0,     0,     0,     0,     0,   409,
  4307,   626,     0,     0,     0,     0, 15963,  4307,     0,  4307,
 15733,  4307, 15848, 15848,     0,   261,     0,     0, 15848, 15733,
     0,   261,   421,     0,     0,     0,     0,     0, 15733, 15388,
     0,  -167,     0,    69,   513,     0,     0,   436,     0, 15733,
   132,   517,     0,   758,     0,   579,     0, 15733,   206,     0,
     0,     0,     0, 16653,    91,     0,     0,  4307,     0,     0,
  5790,     0,   524, 15733, 15733, 15733,   460,   527,     0,     0,
     0, 15503,  4307,  4307,  4307,     0,    54,   583, 13893,     0,
   583,   583,   459, 14008, 14123,     0,     0,     0,  -209,  -209,
     0,   -62,     1,     0,     0,     0,  2236,     0,   440,     0,
     0,     0,     0,     0,     0,   445,   550,   454,  3252,   552,
     0,  3252,     0,  3252,     0,     0,  3252,  3252,     0, 15848,
  3252, 15733,     0,  4307,  4307,     0,     0,     0,  1554,     0,
   481,     0,   781,     0,   487,   646,   617,     0,   646,     0,
   525,   543,     0,     0,     0,  4307,     0,    91,     0, 15733,
     0, 15733,   -60,   575,   578,     0,     0, 15733,     0,     0,
     0, 15733,   803,   805,     0, 15733,   508,     0,     0,     0,
     0,     0,     0,     0,  3252,     0,   489,   591,  4307,     0,
   579,     0, 15733,     0,     0,     0, 17619, 17678, 14698,    19,
  4307,     0,     0,     0,     0,     0,     0,  4307,  3196,   583,
 14238,     0, 14353,     0,   583,     0,     0,     0,   595,   646,
     0,     0,     0,     0,     0,   515,     0,   272,   598,     0,
     0, 15733,   830, 15733,     0,     0,     0,     0,     0,     0,
   583, 14468,     0,   583,     0, 15733,     0,   583,     0,
    }, yyRindex = {
//yyRindex 889
     0,     0,   126,     0,     0,     0,     0,     0,   557,     0,
     0,   -46,     0,     0,     0,  7547,  7652,     0,     0,  7761,
  4578,  3983,     0,     0,     0,     0,     0,     0, 15618,     0,
     0,     0,     0,  2063,  3138,     0,     0,  2178,     0,     0,
     0,     0,     0,     0,     0,     0,     0,    55,     0,   530,
   518,   205,     0,     0,   580,     0,     0,     0,   593,  -190,
     0,  6843,     0,     0,     0,  6948,     0,     0,     0,     0,
     0,     0,     0,   260,   182,   787,  8829,  7057, 11338,     0,
     0, 12290,     0,  8811,     0,     0,     0,     0,   209,     0,
     0,     0,  8694,     0, 14583,   650,     0,     0,     0,  7196,
  5978,   537, 12383, 12503,     0,     0,     0,    55,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  1018,
  1178,  1438,  1662,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  1825,  2305,  2728,     0,  2785,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  1586,     0,     0,     0,   281,     0,     0, 11921,     0,
     0,  6492,     0,     0,  6081,     0,     0,     0,   610,     0,
   -36,     0,     0,     0,     0,     0,   881,     0,     0,     0,
   953,     0,     0,     0, 11113,     0,     0, 11941,  6432,  6432,
     0,     0,     0,     0,     0,     0,     0,   545,     0,     0,
     0,     0,     0,     0,     0,     0,    46,     0,     0,  7881,
  7299,  7410,  8920,    55,     0,   -17,     0,    59,     0,   538,
     0,     0,   540,   540,     0,   526,   526,     0,     0,     0,
  1055,     0,  1941,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   325,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   530,     0,     0,     0,     0,     0,    55,   216,
   386,     0,     0,     0, 12105,     0,     0,     0,     0,   105,
     0, 12853,     0,     0,     0,     0,     0,     0,     0,   530,
   557,     0,     0,     0,     0,   120,     0,   270,   298,     0,
  6595,     0,     0,   721, 12969,     0,     0,   530,     0,     0,
     0,   494,     0,    70,     0,     0,     0,     0,     0,  1272,
     0,     0,   530,     0,  6432,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   565,     0,     0,    86,   566,   566,
     0,    98,     0,     0,     0,     0,     0, 11149,    46,     0,
     0,     0,     0,     0,     0,     0,     0,   114,   566,   538,
     0,     0,   549,     0,     0,  -227,     0,   547,     0,     0,
     0,  1946,     0,     0,   530,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 13085, 13203,   945,     0,     0,     0,     0,     0,     0,
     0,  7990,  1918, 10440, 10563, 10657,  9984, 10106, 10747, 11022,
 10838, 10932,  1115,  6329,  9425,  9529,  8095,  9640,  8232,  8343,
  9118,  9303, 10224, 10323,  9769,  9880,     0, 13085,  4941,     0,
  5056,  4098,     0,  5421,  3503,  5536, 14583,     0,  3618,     0,
     0,     0,  5663,  5663,     0, 11239,     0,     0,  1294,     0,
     0,     0,     0,     0,     0,     0,     0,  1181,     0, 11277,
     0,     0,     0,     0,     0,   557,  6185, 12619, 12735,     0,
     0,     0,     0,   566,     0,   100,     0,     0,     0,     0,
    99,     0,     0,     0,     0,   557,     0,     0,    88,    88,
     0,    33,    88,    72,     0,     0,     0,   214,   113,     0,
   475,   652,     0,   652,     0,  2543,  2658,  3023,  4463,     0,
 11977,   652,     0,     0,     0,   188,     0,     0,     0,     0,
     0,     0,     0,  1180,  1296,  1421,   670,     0,     0,     0,
     0,     0,     0, 12069,  6432,     0,     0,     0,     0,     0,
     0,   177,     0,     0,   603,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  8446,  8569, 11389,
    28,     0,     0,     0,     0,  1741,  1751,  1917,  1373,     0,
    46,     0,     0,     0,     0,     0,     0,    70,     0,    46,
     0,    70,     0,     0,     0, 12168,     0,     0,     0,     0,
     0, 12253,  8996,     0,     0,     0,     0,     0,     0,     0,
     0, 13203,     0,     0,     0,     0,     0,     0,     0,     0,
   566,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,    70,     0,     0,
     0,     0,     0,     0,     0,     0,  6706,     0,     0,     0,
     0,     0,   574,    70,    70,  1331,     0,  6432,     0,     0,
  6432,   603,     0,     0,     0,     0,     0,     0,    23,    23,
     0,     0,   566,     0,     0,     0,   538,  1949,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0, 11425,     0,
     0, 11517,     0, 11553,     0,     0, 11665, 11701,     0,     0,
 11793,     0, 12233,    46,   557,     0,     0,     0,   120,     0,
     0,     0,    88,     0,    80,    88,     0,     0,    88,     0,
     0,     0,   567,     0,   597,   557,     0,     0,     0,     0,
     0,     0,   652,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   603,   603,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0, 11829,     0,     0,     0,   557,     0,
     0,     0,     0,     0,     0,   692,     0,     0,   530,   281,
   721,     0,     0,     0,     0,     0,     0,    70,  6432,   603,
     0,     0,     0,     0,   603,     0,     0,     0,     0,    88,
     0,    15,   448,   561,   233,     0,     0,   652,     0,     0,
     0,     0,   603,     0,     0,     0,     0,   716,     0,     0,
   603,     0,     0,   603,     0,     0,     0,   603,     0,
    }, yyGindex = {
//yyGindex 136
     0,     0,     0,     0,   886,     0,     0,     0,   516,  -144,
     0,     0,     0,     0,     0,     0,     0,   943,     4,    20,
  -350,     0,   150,   333,   -15,    37,    -2,   186,   461,  -342,
     0,    89,     0,   676,     0,     0,     0,   -13,     0,    10,
   952,  -470,  -244,     0,   195,   400,  -604,     0,     0,   966,
  -216,   887,    -3,  1423,  -390,     0,  -303,   321,  -331,  1251,
  1195,     0,     0,     0,   283,    43,     0,     0,   -10,  -395,
     0,   138,     8,     0,    13,  -357,   913,     0,  -491,    -7,
    85,  -192,   140,  1344,  1098,   -11,     0,     3,   891,  -223,
     0,   -74,     6,    48,   289,  -617,     0,     0,     0,     0,
   -28,   908,     0,     0,     0,     0,     0,   -55,   243,     0,
     0,     0,     0,  -189,     0,  -379,     0,     0,     0,     0,
     0,     0,    38,     0,     0,     0,     0,     0,     0,     0,
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
    String text = ", unexpected " + found + "\n"; 
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
					// line 264 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_BEG);
                  support.initTopLocalVariables();
              }
  break;
case 2:
					// line 267 "DefaultRubyParser.y"
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
					// line 279 "DefaultRubyParser.y"
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
					// line 296 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-1+yyTop]) instanceof BlockNode) {
                      support.checkUselessStatements(((BlockNode)yyVals[-1+yyTop]));
		  }
                  yyVal = ((Node)yyVals[-1+yyTop]);
              }
  break;
case 6:
					// line 304 "DefaultRubyParser.y"
  {
                  yyVal = support.newline_node(((Node)yyVals[0+yyTop]), getPosition(((Node)yyVals[0+yyTop]), true));
              }
  break;
case 7:
					// line 307 "DefaultRubyParser.y"
  {
	          yyVal = support.appendToBlock(((Node)yyVals[-2+yyTop]), support.newline_node(((Node)yyVals[0+yyTop]), getPosition(((Node)yyVals[0+yyTop]), true)));
              }
  break;
case 8:
					// line 310 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 9:
					// line 314 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_FNAME);
              }
  break;
case 10:
					// line 316 "DefaultRubyParser.y"
  {
                  yyVal = new AliasNode(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 11:
					// line 319 "DefaultRubyParser.y"
  {
                  yyVal = new VAliasNode(getPosition(((Token)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 12:
					// line 322 "DefaultRubyParser.y"
  {
                  yyVal = new VAliasNode(getPosition(((Token)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), "$" + ((BackRefNode)yyVals[0+yyTop]).getType()); /* XXX*/
              }
  break;
case 13:
					// line 325 "DefaultRubyParser.y"
  {
                  yyerror("can't make alias for the number variables");
              }
  break;
case 14:
					// line 328 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 15:
					// line 331 "DefaultRubyParser.y"
  {
                  yyVal = new IfNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), null);
              }
  break;
case 16:
					// line 334 "DefaultRubyParser.y"
  {
                  yyVal = new IfNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), null, ((Node)yyVals[-2+yyTop]));
              }
  break;
case 17:
					// line 337 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                      yyVal = new WhileNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                  } else {
                      yyVal = new WhileNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                  }
              }
  break;
case 18:
					// line 344 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                      yyVal = new UntilNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                  } else {
                      yyVal = new UntilNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                  }
              }
  break;
case 19:
					// line 351 "DefaultRubyParser.y"
  {
                  Node body = ((Node)yyVals[0+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop]);
	          yyVal = new RescueNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), new RescueBodyNode(getPosition(((Node)yyVals[-2+yyTop])), null, body, null), null);
              }
  break;
case 20:
					// line 355 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
                      yyerror("BEGIN in method");
                  }
		  support.pushLocalScope();
              }
  break;
case 21:
					// line 360 "DefaultRubyParser.y"
  {
                  support.getResult().addBeginNode(new PreExeNode(getPosition(((Node)yyVals[-1+yyTop])), support.getCurrentScope(), ((Node)yyVals[-1+yyTop])));
                  support.popCurrentScope();
                  yyVal = null; /*XXX 0;*/
              }
  break;
case 22:
					// line 365 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
                      warnings.warn(ID.END_IN_METHOD, getPosition(((Token)yyVals[-3+yyTop])), "END in method; use at_exit");
                  }
                  yyVal = new PostExeNode(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 23:
					// line 371 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
                  yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 24:
					// line 375 "DefaultRubyParser.y"
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
					// line 384 "DefaultRubyParser.y"
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
					// line 400 "DefaultRubyParser.y"
  {
                  yyVal = new OpElementAsgnNode(getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));

              }
  break;
case 27:
					// line 404 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 28:
					// line 407 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 29:
					// line 410 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 30:
					// line 413 "DefaultRubyParser.y"
  {
                  support.backrefAssignError(((Node)yyVals[-2+yyTop]));
              }
  break;
case 31:
					// line 416 "DefaultRubyParser.y"
  {
                  yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), new SValueNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
              }
  break;
case 32:
					// line 419 "DefaultRubyParser.y"
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
					// line 427 "DefaultRubyParser.y"
  {
                  ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
		  yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
                  ((MultipleAsgnNode)yyVals[-2+yyTop]).setPosition(support.union(((MultipleAsgnNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
	      }
  break;
case 36:
					// line 436 "DefaultRubyParser.y"
  {
                  yyVal = support.newAndNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 37:
					// line 439 "DefaultRubyParser.y"
  {
                  yyVal = support.newOrNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 38:
					// line 442 "DefaultRubyParser.y"
  {
                  yyVal = new NotNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
              }
  break;
case 39:
					// line 445 "DefaultRubyParser.y"
  {
                  yyVal = new NotNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
              }
  break;
case 41:
					// line 450 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
	      }
  break;
case 44:
					// line 457 "DefaultRubyParser.y"
  {
                  yyVal = new ReturnNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
              }
  break;
case 45:
					// line 460 "DefaultRubyParser.y"
  {
                  yyVal = new BreakNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
              }
  break;
case 46:
					// line 463 "DefaultRubyParser.y"
  {
                  yyVal = new NextNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
              }
  break;
case 48:
					// line 469 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 49:
					// line 472 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 50:
					// line 477 "DefaultRubyParser.y"
  {
                    support.pushBlockScope();
		}
  break;
case 51:
					// line 479 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                    support.popCurrentScope();
		}
  break;
case 52:
					// line 485 "DefaultRubyParser.y"
  {
                  yyVal = support.new_fcall(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 53:
					// line 488 "DefaultRubyParser.y"
  {
                  yyVal = support.new_fcall(((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])); 
              }
  break;
case 54:
					// line 491 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 55:
					// line 494 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])); 
	      }
  break;
case 56:
					// line 497 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 57:
					// line 500 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])); 
	      }
  break;
case 58:
					// line 503 "DefaultRubyParser.y"
  {
		  yyVal = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop])); /* .setPosFrom($2);*/
	      }
  break;
case 59:
					// line 506 "DefaultRubyParser.y"
  {
                  yyVal = support.new_yield(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
	      }
  break;
case 61:
					// line 512 "DefaultRubyParser.y"
  {
                  yyVal = ((MultipleAsgnNode)yyVals[-1+yyTop]);
	      }
  break;
case 63:
					// line 518 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((Token)yyVals[-2+yyTop])), new ArrayNode(getPosition(((Token)yyVals[-2+yyTop])), ((MultipleAsgnNode)yyVals[-1+yyTop])), null);
              }
  break;
case 64:
					// line 523 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((ListNode)yyVals[0+yyTop])), ((ListNode)yyVals[0+yyTop]), null);
              }
  break;
case 65:
					// line 526 "DefaultRubyParser.y"
  {
/*mirko: check*/
                  yyVal = new MultipleAsgnNode(support.union(((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), ((ListNode)yyVals[-1+yyTop]).add(((Node)yyVals[0+yyTop])), null);
                  ((Node)yyVals[-1+yyTop]).setPosition(support.union(((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])));
              }
  break;
case 66:
					// line 531 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((ListNode)yyVals[-2+yyTop])), ((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 67:
					// line 534 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((ListNode)yyVals[-1+yyTop])), ((ListNode)yyVals[-1+yyTop]), new StarNode(getPosition(null)));
              }
  break;
case 68:
					// line 537 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((Token)yyVals[-1+yyTop])), null, ((Node)yyVals[0+yyTop]));
              }
  break;
case 69:
					// line 540 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((Token)yyVals[0+yyTop])), null, new StarNode(getPosition(null)));
              }
  break;
case 71:
					// line 545 "DefaultRubyParser.y"
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
                  yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
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

		  ISourcePosition position = support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop]));

                  yyVal = new ConstDeclNode(position, null, new Colon2Node(position, ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
	      }
  break;
case 80:
					// line 581 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
		      yyerror("dynamic constant assignment");
		  }

                  ISourcePosition position = support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop]));

                  yyVal = new ConstDeclNode(position, null, new Colon3Node(position, (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
	      }
  break;
case 81:
					// line 590 "DefaultRubyParser.y"
  {
	          support.backrefAssignError(((Node)yyVals[0+yyTop]));
              }
  break;
case 82:
					// line 595 "DefaultRubyParser.y"
  {
                  yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
              }
  break;
case 83:
					// line 598 "DefaultRubyParser.y"
  {
                  yyVal = support.aryset(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 84:
					// line 601 "DefaultRubyParser.y"
  {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 85:
					// line 604 "DefaultRubyParser.y"
  {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
 	      }
  break;
case 86:
					// line 607 "DefaultRubyParser.y"
  {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 87:
					// line 610 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
		      yyerror("dynamic constant assignment");
		  }
			
		  ISourcePosition position = support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop]));

                  yyVal = new ConstDeclNode(position, null, new Colon2Node(position, ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
              }
  break;
case 88:
					// line 619 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
		      yyerror("dynamic constant assignment");
		  }

                  ISourcePosition position = support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop]));

                  yyVal = new ConstDeclNode(position, null, new Colon3Node(position, (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
	      }
  break;
case 89:
					// line 628 "DefaultRubyParser.y"
  {
                   support.backrefAssignError(((Node)yyVals[0+yyTop]));
	      }
  break;
case 90:
					// line 632 "DefaultRubyParser.y"
  {
                  yyerror("class/module name must be CONSTANT");
              }
  break;
case 92:
					// line 637 "DefaultRubyParser.y"
  {
                  yyVal = new Colon3Node(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
	      }
  break;
case 93:
					// line 640 "DefaultRubyParser.y"
  {
                  yyVal = new Colon2Node(((Token)yyVals[0+yyTop]).getPosition(), null, (String) ((Token)yyVals[0+yyTop]).getValue());
 	      }
  break;
case 94:
					// line 643 "DefaultRubyParser.y"
  {
                  yyVal = new Colon2Node(support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
	      }
  break;
case 98:
					// line 649 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_END);
                  yyVal = ((Token)yyVals[0+yyTop]);
              }
  break;
case 99:
					// line 654 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_END);
                  yyVal = yyVals[0+yyTop];
              }
  break;
case 102:
					// line 661 "DefaultRubyParser.y"
  {
                  yyVal = new UndefNode(getPosition(((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 103:
					// line 664 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_FNAME);
	      }
  break;
case 104:
					// line 666 "DefaultRubyParser.y"
  {
                  yyVal = support.appendToBlock(((Node)yyVals[-3+yyTop]), new UndefNode(getPosition(((Node)yyVals[-3+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue()));
              }
  break;
case 172:
					// line 685 "DefaultRubyParser.y"
  {
                  yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
		  /* FIXME: Consider fixing node_assign itself rather than single case*/
		  ((Node)yyVal).setPosition(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
              }
  break;
case 173:
					// line 690 "DefaultRubyParser.y"
  {
                  ISourcePosition position = support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                  Node body = ((Node)yyVals[0+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop]);
                  yyVal = support.node_assign(((Node)yyVals[-4+yyTop]), new RescueNode(position, ((Node)yyVals[-2+yyTop]), new RescueBodyNode(position, null, body, null), null));
	      }
  break;
case 174:
					// line 695 "DefaultRubyParser.y"
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
					// line 711 "DefaultRubyParser.y"
  {
                  yyVal = new OpElementAsgnNode(getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 176:
					// line 714 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 177:
					// line 717 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 178:
					// line 720 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 179:
					// line 723 "DefaultRubyParser.y"
  {
	          yyerror("constant re-assignment");
	      }
  break;
case 180:
					// line 726 "DefaultRubyParser.y"
  {
		  yyerror("constant re-assignment");
	      }
  break;
case 181:
					// line 729 "DefaultRubyParser.y"
  {
                  support.backrefAssignError(((Node)yyVals[-2+yyTop]));
              }
  break;
case 182:
					// line 732 "DefaultRubyParser.y"
  {
		  support.checkExpression(((Node)yyVals[-2+yyTop]));
		  support.checkExpression(((Node)yyVals[0+yyTop]));
    
                  boolean isLiteral = ((Node)yyVals[-2+yyTop]) instanceof FixnumNode && ((Node)yyVals[0+yyTop]) instanceof FixnumNode;
                  yyVal = new DotNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), false, isLiteral);
              }
  break;
case 183:
					// line 739 "DefaultRubyParser.y"
  {
		  support.checkExpression(((Node)yyVals[-2+yyTop]));
		  support.checkExpression(((Node)yyVals[0+yyTop]));
                  boolean isLiteral = ((Node)yyVals[-2+yyTop]) instanceof FixnumNode && ((Node)yyVals[0+yyTop]) instanceof FixnumNode;
                  yyVal = new DotNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), true, isLiteral);
              }
  break;
case 184:
					// line 745 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "+", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 185:
					// line 748 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "-", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 186:
					// line 751 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "*", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 187:
					// line 754 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "/", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 188:
					// line 757 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "%", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 189:
					// line 760 "DefaultRubyParser.y"
  {
		  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 190:
					// line 763 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), getPosition(null)), "-@");
              }
  break;
case 191:
					// line 766 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((FloatNode)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), getPosition(null)), "-@");
              }
  break;
case 192:
					// line 769 "DefaultRubyParser.y"
  {
                  if (support.isLiteral(((Node)yyVals[0+yyTop]))) {
		      yyVal = ((Node)yyVals[0+yyTop]);
		  } else {
                      yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "+@");
		  }
              }
  break;
case 193:
					// line 776 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "-@");
	      }
  break;
case 194:
					// line 779 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "|", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 195:
					// line 782 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "^", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 196:
					// line 785 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "&", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 197:
					// line 788 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=>", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 198:
					// line 791 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 199:
					// line 794 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">=", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 200:
					// line 797 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 201:
					// line 800 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 202:
					// line 803 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 203:
					// line 806 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "===", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 204:
					// line 809 "DefaultRubyParser.y"
  {
                  yyVal = new NotNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop]), getPosition(null)));
              }
  break;
case 205:
					// line 812 "DefaultRubyParser.y"
  {
                  yyVal = support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 206:
					// line 815 "DefaultRubyParser.y"
  {
                  yyVal = new NotNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
              }
  break;
case 207:
					// line 818 "DefaultRubyParser.y"
  {
                  yyVal = new NotNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
              }
  break;
case 208:
					// line 821 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "~");
              }
  break;
case 209:
					// line 824 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<<", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 210:
					// line 827 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">>", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 211:
					// line 830 "DefaultRubyParser.y"
  {
                  yyVal = support.newAndNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 212:
					// line 833 "DefaultRubyParser.y"
  {
                  yyVal = support.newOrNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 213:
					// line 836 "DefaultRubyParser.y"
  {
                  yyVal = new DefinedNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop]));
              }
  break;
case 214:
					// line 839 "DefaultRubyParser.y"
  {
                  yyVal = new IfNode(getPosition(((Node)yyVals[-4+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 215:
					// line 842 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 216:
					// line 846 "DefaultRubyParser.y"
  {
	          support.checkExpression(((Node)yyVals[0+yyTop]));
	          yyVal = ((Node)yyVals[0+yyTop]);   
	      }
  break;
case 218:
					// line 852 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 219:
					// line 855 "DefaultRubyParser.y"
  {
                  yyVal = ((ListNode)yyVals[-1+yyTop]);
              }
  break;
case 220:
					// line 858 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 221:
					// line 862 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition(((ListNode)yyVals[-1+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
              }
  break;
case 222:
					// line 865 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[-1+yyTop]));
		  yyVal = new NewlineNode(getPosition(((Token)yyVals[-2+yyTop])), new SplatNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])));
              }
  break;
case 223:
					// line 870 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
              }
  break;
case 224:
					// line 873 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[-2+yyTop]);
		  ((Node)yyVal).setPosition(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])));
              }
  break;
case 225:
					// line 877 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition(((Token)yyVals[-3+yyTop])), ((Node)yyVals[-2+yyTop]));
              }
  break;
case 226:
					// line 880 "DefaultRubyParser.y"
  {
                  yyVal = ((ListNode)yyVals[-4+yyTop]).add(((Node)yyVals[-2+yyTop]));
              }
  break;
case 229:
					// line 887 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
              }
  break;
case 230:
					// line 890 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_blk_pass(((ListNode)yyVals[-1+yyTop]), ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 231:
					// line 893 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass(((Node)yyVal), ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 232:
					// line 897 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition(((ListNode)yyVals[-1+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 233:
					// line 901 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), new ArrayNode(getPosition(((ListNode)yyVals[-4+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 234:
					// line 905 "DefaultRubyParser.y"
  {
                  yyVal = ((ListNode)yyVals[-3+yyTop]).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 235:
					// line 909 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[-1+yyTop]));
		  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-6+yyTop])), ((ListNode)yyVals[-6+yyTop]).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 236:
					// line 914 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_blk_pass(new SplatNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 237:
					// line 917 "DefaultRubyParser.y"
  {}
  break;
case 238:
					// line 919 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_blk_pass(new ArrayNode(getPosition(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop])).addAll(((ListNode)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 239:
					// line 922 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_blk_pass(new ArrayNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 240:
					// line 925 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-4+yyTop])), new ArrayNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 241:
					// line 929 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-6+yyTop])), new ArrayNode(getPosition(((Node)yyVals[-6+yyTop])), ((Node)yyVals[-6+yyTop])).addAll(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 242:
					// line 933 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition(((ListNode)yyVals[-1+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 243:
					// line 937 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), new ArrayNode(getPosition(((ListNode)yyVals[-4+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 244:
					// line 941 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 245:
					// line 945 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop])).addAll(((ListNode)yyVals[-3+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 246:
					// line 949 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-6+yyTop])), new ArrayNode(getPosition(((Node)yyVals[-6+yyTop])), ((Node)yyVals[-6+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 247:
					// line 953 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-8+yyTop])), new ArrayNode(getPosition(((Node)yyVals[-8+yyTop])), ((Node)yyVals[-8+yyTop])).addAll(((ListNode)yyVals[-6+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 248:
					// line 957 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_blk_pass(new SplatNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 249:
					// line 960 "DefaultRubyParser.y"
  {}
  break;
case 250:
					// line 962 "DefaultRubyParser.y"
  { 
	          yyVal = new Long(lexer.getCmdArgumentState().begin());
	      }
  break;
case 251:
					// line 964 "DefaultRubyParser.y"
  {
                  lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 253:
					// line 970 "DefaultRubyParser.y"
  {                    
		  lexer.setState(LexState.EXPR_ENDARG);
	      }
  break;
case 254:
					// line 972 "DefaultRubyParser.y"
  {
                  warnings.warn(ID.ARGUMENT_EXTRA_SPACE, getPosition(((Token)yyVals[-2+yyTop])), "don't put space before argument parentheses");
	          yyVal = null;
	      }
  break;
case 255:
					// line 976 "DefaultRubyParser.y"
  {
		  lexer.setState(LexState.EXPR_ENDARG);
	      }
  break;
case 256:
					// line 978 "DefaultRubyParser.y"
  {
                  warnings.warn(ID.ARGUMENT_EXTRA_SPACE, getPosition(((Token)yyVals[-3+yyTop])), "don't put space before argument parentheses");
		  yyVal = ((Node)yyVals[-2+yyTop]);
	      }
  break;
case 257:
					// line 983 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
                  yyVal = new BlockPassNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
              }
  break;
case 258:
					// line 988 "DefaultRubyParser.y"
  {
                  yyVal = ((BlockPassNode)yyVals[0+yyTop]);
              }
  break;
case 260:
					// line 993 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition2(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
              }
  break;
case 261:
					// line 996 "DefaultRubyParser.y"
  {
                  yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
              }
  break;
case 262:
					// line 1000 "DefaultRubyParser.y"
  {
		  yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
              }
  break;
case 263:
					// line 1003 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
	      }
  break;
case 264:
					// line 1006 "DefaultRubyParser.y"
  {  
                  yyVal = new SplatNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
	      }
  break;
case 273:
					// line 1018 "DefaultRubyParser.y"
  {
                  yyVal = new FCallNoArgNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
	      }
  break;
case 274:
					// line 1021 "DefaultRubyParser.y"
  {
                  yyVal = new BeginNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]));
	      }
  break;
case 275:
					// line 1024 "DefaultRubyParser.y"
  { 
                  lexer.setState(LexState.EXPR_ENDARG); 
              }
  break;
case 276:
					// line 1026 "DefaultRubyParser.y"
  {
		  warnings.warning(ID.GROUPED_EXPRESSION, getPosition(((Token)yyVals[-4+yyTop])), "(...) interpreted as grouped expression");
                  yyVal = ((Node)yyVals[-3+yyTop]);
	      }
  break;
case 277:
					// line 1030 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-1+yyTop]) != null) {
                      /* compstmt position includes both parens around it*/
                      ((ISourcePositionHolder) ((Node)yyVals[-1+yyTop])).setPosition(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
                  }
		  yyVal = ((Node)yyVals[-1+yyTop]);
              }
  break;
case 278:
					// line 1037 "DefaultRubyParser.y"
  {
                  yyVal = new Colon2Node(support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 279:
					// line 1040 "DefaultRubyParser.y"
  {
                  yyVal = new Colon3Node(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 280:
					// line 1043 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-3+yyTop]) instanceof SelfNode) {
                      yyVal = support.new_fcall(new Token("[]", support.union(((Node)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop]))), ((Node)yyVals[-1+yyTop]), null);
                  } else {
                      yyVal = support.new_call(((Node)yyVals[-3+yyTop]), new Token("[]", support.union(((Node)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop]))), ((Node)yyVals[-1+yyTop]), null);
                  }
              }
  break;
case 281:
					// line 1050 "DefaultRubyParser.y"
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
					// line 1059 "DefaultRubyParser.y"
  {
                  yyVal = new HashNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((ListNode)yyVals[-1+yyTop]));
              }
  break;
case 283:
					// line 1062 "DefaultRubyParser.y"
  {
		  yyVal = new ReturnNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
              }
  break;
case 284:
					// line 1065 "DefaultRubyParser.y"
  {
                  yyVal = support.new_yield(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 285:
					// line 1068 "DefaultRubyParser.y"
  {
                  yyVal = new YieldNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), null, false);
              }
  break;
case 286:
					// line 1071 "DefaultRubyParser.y"
  {
                  yyVal = new YieldNode(((Token)yyVals[0+yyTop]).getPosition(), null, false);
              }
  break;
case 287:
					// line 1074 "DefaultRubyParser.y"
  {
                  yyVal = new DefinedNode(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 288:
					// line 1077 "DefaultRubyParser.y"
  {
                  yyVal = new FCallNoArgBlockNode(support.union(((Token)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((IterNode)yyVals[0+yyTop]));
              }
  break;
case 290:
					// line 1081 "DefaultRubyParser.y"
  {
	          if (((Node)yyVals[-1+yyTop]) != null && 
                      ((BlockAcceptingNode)yyVals[-1+yyTop]).getIterNode() instanceof BlockPassNode) {
                      throw new SyntaxException(PID.BLOCK_ARG_AND_BLOCK_GIVEN, getPosition(((Node)yyVals[-1+yyTop])), "Both block arg and actual block given.");
		  }
		  yyVal = ((BlockAcceptingNode)yyVals[-1+yyTop]).setIterNode(((IterNode)yyVals[0+yyTop]));
		  ((Node)yyVal).setPosition(support.union(((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])));
              }
  break;
case 291:
					// line 1089 "DefaultRubyParser.y"
  {
                  yyVal = new IfNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 292:
					// line 1092 "DefaultRubyParser.y"
  {
                  yyVal = new IfNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-2+yyTop]));
              }
  break;
case 293:
					// line 1095 "DefaultRubyParser.y"
  { 
                  lexer.getConditionState().begin();
	      }
  break;
case 294:
					// line 1097 "DefaultRubyParser.y"
  {
		  lexer.getConditionState().end();
	      }
  break;
case 295:
					// line 1099 "DefaultRubyParser.y"
  {
                  Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);
                  yyVal = new WhileNode(support.union(((Token)yyVals[-6+yyTop]), ((Token)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), body);
              }
  break;
case 296:
					// line 1103 "DefaultRubyParser.y"
  {
                  lexer.getConditionState().begin();
              }
  break;
case 297:
					// line 1105 "DefaultRubyParser.y"
  {
                  lexer.getConditionState().end();
              }
  break;
case 298:
					// line 1107 "DefaultRubyParser.y"
  {
                  Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);
                  yyVal = new UntilNode(getPosition(((Token)yyVals[-6+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), body);
              }
  break;
case 299:
					// line 1111 "DefaultRubyParser.y"
  {
                  yyVal = new CaseNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 300:
					// line 1114 "DefaultRubyParser.y"
  {
/* TODO: MRI is just a when node.  We need this extra logic for IDE consumers (null in casenode statement should be implicit nil)*/
/*                  if (support.getConfiguration().hasExtraPositionInformation()) {*/
                      yyVal = new CaseNode(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])), null, ((Node)yyVals[-1+yyTop]));
/*                  } else {*/
/*                      $$ = $3;*/
/*                  }*/
              }
  break;
case 301:
					// line 1122 "DefaultRubyParser.y"
  {
		  yyVal = ((Node)yyVals[-1+yyTop]);
              }
  break;
case 302:
					// line 1125 "DefaultRubyParser.y"
  {
                  lexer.getConditionState().begin();
              }
  break;
case 303:
					// line 1127 "DefaultRubyParser.y"
  {
                  lexer.getConditionState().end();
              }
  break;
case 304:
					// line 1129 "DefaultRubyParser.y"
  {
                  yyVal = new ForNode(support.union(((Token)yyVals[-8+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-7+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-4+yyTop]));
              }
  break;
case 305:
					// line 1132 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
                      yyerror("class definition in method body");
                  }
		  support.pushLocalScope();
              }
  break;
case 306:
					// line 1137 "DefaultRubyParser.y"
  {
                  Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);

                  yyVal = new ClassNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), ((Colon3Node)yyVals[-4+yyTop]), support.getCurrentScope(), body, ((Node)yyVals[-3+yyTop]));
                  support.popCurrentScope();
              }
  break;
case 307:
					// line 1143 "DefaultRubyParser.y"
  {
                  yyVal = new Boolean(support.isInDef());
                  support.setInDef(false);
              }
  break;
case 308:
					// line 1146 "DefaultRubyParser.y"
  {
                  yyVal = new Integer(support.getInSingle());
                  support.setInSingle(0);
		  support.pushLocalScope();
              }
  break;
case 309:
					// line 1150 "DefaultRubyParser.y"
  {
                  yyVal = new SClassNode(support.union(((Token)yyVals[-7+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-5+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  support.popCurrentScope();
                  support.setInDef(((Boolean)yyVals[-4+yyTop]).booleanValue());
                  support.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
              }
  break;
case 310:
					// line 1156 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) { 
                      yyerror("module definition in method body");
                  }
		  support.pushLocalScope();
              }
  break;
case 311:
					// line 1161 "DefaultRubyParser.y"
  {
                  Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);

                  yyVal = new ModuleNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Colon3Node)yyVals[-3+yyTop]), support.getCurrentScope(), body);
                  support.popCurrentScope();
              }
  break;
case 312:
					// line 1167 "DefaultRubyParser.y"
  {
                  support.setInDef(true);
		  support.pushLocalScope();
              }
  break;
case 313:
					// line 1170 "DefaultRubyParser.y"
  {
                  /* TODO: We should use implicit nil for body, but problem (punt til later)*/
                  Node body = ((Node)yyVals[-1+yyTop]); /*$5 == null ? NilImplicitNode.NIL : $5;*/

                  /* NOEX_PRIVATE for toplevel */
                  yyVal = new DefnNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), new ArgumentNode(((Token)yyVals[-4+yyTop]).getPosition(), (String) ((Token)yyVals[-4+yyTop]).getValue()), ((ArgsNode)yyVals[-2+yyTop]), support.getCurrentScope(), body, Visibility.PRIVATE);
                  support.popCurrentScope();
                  support.setInDef(false);
              }
  break;
case 314:
					// line 1179 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_FNAME);
              }
  break;
case 315:
					// line 1181 "DefaultRubyParser.y"
  {
                  support.setInSingle(support.getInSingle() + 1);
		  support.pushLocalScope();
                  lexer.setState(LexState.EXPR_END); /* force for args */
              }
  break;
case 316:
					// line 1185 "DefaultRubyParser.y"
  {
                  /* TODO: We should use implicit nil for body, but problem (punt til later)*/
                  Node body = ((Node)yyVals[-1+yyTop]); /*$8 == null ? NilImplicitNode.NIL : $8;*/

                  yyVal = new DefsNode(support.union(((Token)yyVals[-8+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-7+yyTop]), new ArgumentNode(((Token)yyVals[-4+yyTop]).getPosition(), (String) ((Token)yyVals[-4+yyTop]).getValue()), ((ArgsNode)yyVals[-2+yyTop]), support.getCurrentScope(), body);
                  support.popCurrentScope();
                  support.setInSingle(support.getInSingle() - 1);
              }
  break;
case 317:
					// line 1193 "DefaultRubyParser.y"
  {
                  yyVal = new BreakNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
              }
  break;
case 318:
					// line 1196 "DefaultRubyParser.y"
  {
                  yyVal = new NextNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
              }
  break;
case 319:
					// line 1199 "DefaultRubyParser.y"
  {
                  yyVal = new RedoNode(((Token)yyVals[0+yyTop]).getPosition());
              }
  break;
case 320:
					// line 1202 "DefaultRubyParser.y"
  {
                  yyVal = new RetryNode(((Token)yyVals[0+yyTop]).getPosition());
              }
  break;
case 321:
					// line 1206 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
		  yyVal = ((Node)yyVals[0+yyTop]);
	      }
  break;
case 330:
					// line 1221 "DefaultRubyParser.y"
  {
/*mirko: support.union($<ISourcePositionHolder>1.getPosition(), getPosition($<ISourcePositionHolder>1)) ?*/
                  yyVal = new IfNode(getPosition(((Token)yyVals[-4+yyTop])), support.getConditionNode(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 332:
					// line 1227 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 334:
					// line 1232 "DefaultRubyParser.y"
  {}
  break;
case 336:
					// line 1235 "DefaultRubyParser.y"
  {
                  yyVal = new ZeroArgNode(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])));
              }
  break;
case 337:
					// line 1238 "DefaultRubyParser.y"
  {
                  yyVal = new ZeroArgNode(((Token)yyVals[0+yyTop]).getPosition());
	      }
  break;
case 338:
					// line 1241 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[-1+yyTop]);

		  /* Include pipes on multiple arg type*/
                  if (((Node)yyVals[-1+yyTop]) instanceof MultipleAsgnNode) {
		      ((Node)yyVals[-1+yyTop]).setPosition(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
		  } 
              }
  break;
case 339:
					// line 1250 "DefaultRubyParser.y"
  {
                  support.pushBlockScope();
	      }
  break;
case 340:
					// line 1252 "DefaultRubyParser.y"
  {
                  yyVal = new IterNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  support.popCurrentScope();
              }
  break;
case 341:
					// line 1258 "DefaultRubyParser.y"
  {
	          if (((BlockAcceptingNode)yyVals[-1+yyTop]).getIterNode() instanceof BlockPassNode) {
                      throw new SyntaxException(PID.BLOCK_ARG_AND_BLOCK_GIVEN, getPosition(((Node)yyVals[-1+yyTop])), "Both block arg and actual block given.");
                  }
		  yyVal = ((BlockAcceptingNode)yyVals[-1+yyTop]).setIterNode(((IterNode)yyVals[0+yyTop]));
		  ((Node)yyVal).setPosition(support.union(((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])));
              }
  break;
case 342:
					// line 1265 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 343:
					// line 1268 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 344:
					// line 1272 "DefaultRubyParser.y"
  {
                  yyVal = support.new_fcall(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 345:
					// line 1275 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 346:
					// line 1278 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 347:
					// line 1281 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop]), null, null);
              }
  break;
case 348:
					// line 1284 "DefaultRubyParser.y"
  {
                  yyVal = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop]));
              }
  break;
case 349:
					// line 1287 "DefaultRubyParser.y"
  {
                  yyVal = new ZSuperNode(((Token)yyVals[0+yyTop]).getPosition());
              }
  break;
case 350:
					// line 1292 "DefaultRubyParser.y"
  {
                  support.pushBlockScope();
	      }
  break;
case 351:
					// line 1294 "DefaultRubyParser.y"
  {
                  yyVal = new IterNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  support.popCurrentScope();
              }
  break;
case 352:
					// line 1298 "DefaultRubyParser.y"
  {
                  support.pushBlockScope();
	      }
  break;
case 353:
					// line 1300 "DefaultRubyParser.y"
  {
                  yyVal = new IterNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  ((ISourcePositionHolder)yyVals[-5+yyTop]).setPosition(support.union(((ISourcePositionHolder)yyVals[-5+yyTop]), ((ISourcePositionHolder)yyVal)));
                  support.popCurrentScope();
              }
  break;
case 354:
					// line 1306 "DefaultRubyParser.y"
  {
                  yyVal = new WhenNode(support.union(((Token)yyVals[-4+yyTop]), support.unwrapNewlineNode(((Node)yyVals[-1+yyTop]))), ((ListNode)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 356:
					// line 1311 "DefaultRubyParser.y"
  {
                  yyVal = ((ListNode)yyVals[-3+yyTop]).add(new WhenNode(getPosition(((ListNode)yyVals[-3+yyTop])), ((Node)yyVals[0+yyTop]), null, null));
              }
  break;
case 357:
					// line 1314 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(getPosition(((Token)yyVals[-1+yyTop])), new WhenNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]), null, null));
              }
  break;
case 360:
					// line 1321 "DefaultRubyParser.y"
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
                  Node body = node == null ? NilImplicitNode.NIL : node;
                  yyVal = new RescueBodyNode(getPosition(((Token)yyVals[-5+yyTop]), true), ((Node)yyVals[-4+yyTop]), body, ((RescueBodyNode)yyVals[0+yyTop]));
	      }
  break;
case 361:
					// line 1334 "DefaultRubyParser.y"
  {yyVal = null;}
  break;
case 362:
					// line 1336 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
	      }
  break;
case 365:
					// line 1342 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 367:
					// line 1347 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[0+yyTop]) != null) {
                      yyVal = ((Node)yyVals[0+yyTop]);
                  } else {
                      yyVal = new NilNode(getPosition(null));
                  }
              }
  break;
case 370:
					// line 1358 "DefaultRubyParser.y"
  {
                  /* FIXME: We may be intern'ing more than once.*/
                  yyVal = new SymbolNode(((Token)yyVals[0+yyTop]).getPosition(), ((String) ((Token)yyVals[0+yyTop]).getValue()).intern());
              }
  break;
case 372:
					// line 1365 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[0+yyTop]) instanceof EvStrNode ? new DStrNode(getPosition(((Node)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop])) : ((Node)yyVals[0+yyTop]);
	      }
  break;
case 374:
					// line 1371 "DefaultRubyParser.y"
  {
                  yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 375:
					// line 1376 "DefaultRubyParser.y"
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
					// line 1393 "DefaultRubyParser.y"
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
					// line 1410 "DefaultRubyParser.y"
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
					// line 1426 "DefaultRubyParser.y"
  {
                   yyVal = new ZArrayNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
	       }
  break;
case 379:
					// line 1429 "DefaultRubyParser.y"
  {
		   yyVal = ((ListNode)yyVals[-1+yyTop]);
                   ((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
	       }
  break;
case 380:
					// line 1435 "DefaultRubyParser.y"
  {
                   yyVal = new ArrayNode(getPosition(null));
	       }
  break;
case 381:
					// line 1438 "DefaultRubyParser.y"
  {
                   yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]) instanceof EvStrNode ? new DStrNode(getPosition(((ListNode)yyVals[-2+yyTop]))).add(((Node)yyVals[-1+yyTop])) : ((Node)yyVals[-1+yyTop]));
	       }
  break;
case 383:
					// line 1443 "DefaultRubyParser.y"
  {
                   yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
	       }
  break;
case 384:
					// line 1448 "DefaultRubyParser.y"
  {
                   yyVal = new ZArrayNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
	       }
  break;
case 385:
					// line 1451 "DefaultRubyParser.y"
  {
		   yyVal = ((ListNode)yyVals[-1+yyTop]);
                   ((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
	       }
  break;
case 386:
					// line 1457 "DefaultRubyParser.y"
  {
                   yyVal = new ArrayNode(getPosition(null));
	       }
  break;
case 387:
					// line 1460 "DefaultRubyParser.y"
  {
                   yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
	       }
  break;
case 388:
					// line 1465 "DefaultRubyParser.y"
  {
                   yyVal = new StrNode(((Token)yyVals[0+yyTop]).getPosition(), ByteList.create(""));
	       }
  break;
case 389:
					// line 1468 "DefaultRubyParser.y"
  {
                   yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
	       }
  break;
case 390:
					// line 1472 "DefaultRubyParser.y"
  {
		   yyVal = null;
	       }
  break;
case 391:
					// line 1475 "DefaultRubyParser.y"
  {
                   yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
	       }
  break;
case 392:
					// line 1480 "DefaultRubyParser.y"
  {
                   yyVal = ((Node)yyVals[0+yyTop]);
               }
  break;
case 393:
					// line 1483 "DefaultRubyParser.y"
  {
                   yyVal = lexer.getStrTerm();
		   lexer.setStrTerm(null);
		   lexer.setState(LexState.EXPR_BEG);
	       }
  break;
case 394:
					// line 1487 "DefaultRubyParser.y"
  {
		   lexer.setStrTerm(((StrTerm)yyVals[-1+yyTop]));
	           yyVal = new EvStrNode(support.union(((Token)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
	       }
  break;
case 395:
					// line 1491 "DefaultRubyParser.y"
  {
		   yyVal = lexer.getStrTerm();
		   lexer.setStrTerm(null);
		   lexer.setState(LexState.EXPR_BEG);
	       }
  break;
case 396:
					// line 1495 "DefaultRubyParser.y"
  {
		   lexer.setStrTerm(((StrTerm)yyVals[-2+yyTop]));

		   yyVal = support.newEvStrNode(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-1+yyTop]));
	       }
  break;
case 397:
					// line 1501 "DefaultRubyParser.y"
  {
                   yyVal = new GlobalVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
               }
  break;
case 398:
					// line 1504 "DefaultRubyParser.y"
  {
                   yyVal = new InstVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
               }
  break;
case 399:
					// line 1507 "DefaultRubyParser.y"
  {
                   yyVal = new ClassVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
               }
  break;
case 401:
					// line 1514 "DefaultRubyParser.y"
  {
                   lexer.setState(LexState.EXPR_END);
                   yyVal = ((Token)yyVals[0+yyTop]);
		   ((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])));
               }
  break;
case 406:
					// line 1524 "DefaultRubyParser.y"
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
					// line 1551 "DefaultRubyParser.y"
  {
                   yyVal = ((FloatNode)yyVals[0+yyTop]);
               }
  break;
case 409:
					// line 1554 "DefaultRubyParser.y"
  {
                   yyVal = support.negateInteger(((Node)yyVals[0+yyTop]));
	       }
  break;
case 410:
					// line 1557 "DefaultRubyParser.y"
  {
                   yyVal = support.negateFloat(((FloatNode)yyVals[0+yyTop]));
	       }
  break;
case 416:
					// line 1563 "DefaultRubyParser.y"
  { 
                   yyVal = new Token("nil", Tokens.kNIL, ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 417:
					// line 1566 "DefaultRubyParser.y"
  {
                   yyVal = new Token("self", Tokens.kSELF, ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 418:
					// line 1569 "DefaultRubyParser.y"
  { 
                   yyVal = new Token("true", Tokens.kTRUE, ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 419:
					// line 1572 "DefaultRubyParser.y"
  {
                   yyVal = new Token("false", Tokens.kFALSE, ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 420:
					// line 1575 "DefaultRubyParser.y"
  {
                   yyVal = new Token("__FILE__", Tokens.k__FILE__, ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 421:
					// line 1578 "DefaultRubyParser.y"
  {
                   yyVal = new Token("__LINE__", Tokens.k__LINE__, ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 422:
					// line 1583 "DefaultRubyParser.y"
  {
                   yyVal = support.gettable(((Token)yyVals[0+yyTop]));
               }
  break;
case 423:
					// line 1588 "DefaultRubyParser.y"
  {
                   yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
               }
  break;
case 426:
					// line 1596 "DefaultRubyParser.y"
  {
                   yyVal = null;
               }
  break;
case 427:
					// line 1599 "DefaultRubyParser.y"
  {
                   lexer.setState(LexState.EXPR_BEG);
               }
  break;
case 428:
					// line 1601 "DefaultRubyParser.y"
  {
                   yyVal = ((Node)yyVals[-1+yyTop]);
               }
  break;
case 429:
					// line 1604 "DefaultRubyParser.y"
  {
                   yyerrok();
                   yyVal = null;
               }
  break;
case 430:
					// line 1610 "DefaultRubyParser.y"
  {
                   yyVal = ((Node)yyVals[-2+yyTop]);
                   ((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])));
                   lexer.setState(LexState.EXPR_BEG);
               }
  break;
case 431:
					// line 1615 "DefaultRubyParser.y"
  {
                   yyVal = ((Node)yyVals[-1+yyTop]);
               }
  break;
case 432:
					// line 1620 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(support.union(((ListNode)yyVals[-5+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), support.getRestArgNode(((Token)yyVals[-1+yyTop])), ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 433:
					// line 1623 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), -1, null, ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 434:
					// line 1626 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(support.union(((ListNode)yyVals[-3+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), ((ListNode)yyVals[-3+yyTop]), null, ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), support.getRestArgNode(((Token)yyVals[-1+yyTop])), ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 435:
					// line 1629 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(((ISourcePositionHolder)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, -1, null, ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 436:
					// line 1632 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(getPosition(((ListNode)yyVals[-3+yyTop])), null, ((ListNode)yyVals[-3+yyTop]), ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), support.getRestArgNode(((Token)yyVals[-1+yyTop])), ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 437:
					// line 1635 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(getPosition(((ListNode)yyVals[-1+yyTop])), null, ((ListNode)yyVals[-1+yyTop]), -1, null, ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 438:
					// line 1638 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(getPosition(((Token)yyVals[-1+yyTop])), null, null, ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), support.getRestArgNode(((Token)yyVals[-1+yyTop])), ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 439:
					// line 1641 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(getPosition(((BlockArgNode)yyVals[0+yyTop])), null, null, -1, null, ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 440:
					// line 1644 "DefaultRubyParser.y"
  {
                   yyVal = new ArgsNode(support.createEmptyArgsNodePosition(getPosition(null)), null, null, -1, null, null);
               }
  break;
case 441:
					// line 1649 "DefaultRubyParser.y"
  {
                   yyerror("formal argument cannot be a constant");
               }
  break;
case 442:
					// line 1652 "DefaultRubyParser.y"
  {
                   yyerror("formal argument cannot be a instance variable");
               }
  break;
case 443:
					// line 1655 "DefaultRubyParser.y"
  {
                   yyerror("formal argument cannot be an global variable");
               }
  break;
case 444:
					// line 1658 "DefaultRubyParser.y"
  {
                   yyerror("formal argument cannot be a class variable");
               }
  break;
case 445:
					// line 1661 "DefaultRubyParser.y"
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
					// line 1672 "DefaultRubyParser.y"
  {
                    support.allowDubyExtension(((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition());
                    yyVal = new ListNode(((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition());
                    ((ListNode) yyVal).add(new TypedArgumentNode(((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition(), (String) ((Token)yyVals[-2+yyTop]).getValue(), ((Node)yyVals[0+yyTop])));
               }
  break;
case 447:
					// line 1677 "DefaultRubyParser.y"
  {
                    yyVal = new ListNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
                    ((ListNode) yyVal).add(new ArgumentNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue()));
               }
  break;
case 448:
					// line 1681 "DefaultRubyParser.y"
  {
                   support.allowDubyExtension(((ISourcePositionHolder)yyVals[-4+yyTop]).getPosition());
                   ((ListNode)yyVals[-4+yyTop]).add(new TypedArgumentNode(((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition(), (String) ((Token)yyVals[-2+yyTop]).getValue(), ((Node)yyVals[0+yyTop])));
                   ((ListNode)yyVals[-4+yyTop]).setPosition(support.union(((ListNode)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop])));
		   yyVal = ((ListNode)yyVals[-4+yyTop]);
               }
  break;
case 449:
					// line 1687 "DefaultRubyParser.y"
  {
                   ((ListNode)yyVals[-2+yyTop]).add(new ArgumentNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue()));
                   ((ListNode)yyVals[-2+yyTop]).setPosition(support.union(((ListNode)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
		   yyVal = ((ListNode)yyVals[-2+yyTop]);
               }
  break;
case 450:
					// line 1694 "DefaultRubyParser.y"
  {
                   String identifier = (String) ((Token)yyVals[-2+yyTop]).getValue();

                   if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                       yyerror("duplicate optional argument name");
                   }
		   support.getCurrentScope().getLocalScope().addVariable(identifier);
                   yyVal = support.assignable(((Token)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 451:
					// line 1705 "DefaultRubyParser.y"
  {
                  yyVal = new BlockNode(getPosition(((Node)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
              }
  break;
case 452:
					// line 1708 "DefaultRubyParser.y"
  {
                  yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 455:
					// line 1716 "DefaultRubyParser.y"
  {
                  String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();

                  if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                      yyerror("duplicate rest argument name");
                  }
		  ((Token)yyVals[-1+yyTop]).setValue(new Integer(support.getCurrentScope().getLocalScope().addVariable(identifier)));
                  yyVal = ((Token)yyVals[-1+yyTop]);
              }
  break;
case 456:
					// line 1725 "DefaultRubyParser.y"
  {
                  ((Token)yyVals[0+yyTop]).setValue(new Integer(support.getCurrentScope().getLocalScope().addVariable("*")));
                  yyVal = ((Token)yyVals[0+yyTop]);
              }
  break;
case 459:
					// line 1734 "DefaultRubyParser.y"
  {
                  String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();

                  if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                      yyerror("duplicate block argument name");
                  }
                  yyVal = new BlockArgNode(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), support.getCurrentScope().getLocalScope().addVariable(identifier), identifier);
              }
  break;
case 460:
					// line 1743 "DefaultRubyParser.y"
  {
                  yyVal = ((BlockArgNode)yyVals[0+yyTop]);
              }
  break;
case 461:
					// line 1746 "DefaultRubyParser.y"
  {
	          yyVal = null;
	      }
  break;
case 462:
					// line 1750 "DefaultRubyParser.y"
  {
                  if (!(((Node)yyVals[0+yyTop]) instanceof SelfNode)) {
		      support.checkExpression(((Node)yyVals[0+yyTop]));
		  }
		  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 463:
					// line 1756 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_BEG);
              }
  break;
case 464:
					// line 1758 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-2+yyTop]) == null) {
                      yyerror("can't define single method for ().");
                  } else if (((Node)yyVals[-2+yyTop]) instanceof ILiteralNode) {
                      yyerror("can't define single method for literals.");
                  }
		  support.checkExpression(((Node)yyVals[-2+yyTop]));
                  yyVal = ((Node)yyVals[-2+yyTop]);
              }
  break;
case 465:
					// line 1770 "DefaultRubyParser.y"
  { /* [!null]*/
                  yyVal = new ArrayNode(getPosition(null));
              }
  break;
case 466:
					// line 1773 "DefaultRubyParser.y"
  { /* [!null]*/
                  yyVal = ((ListNode)yyVals[-1+yyTop]);
              }
  break;
case 467:
					// line 1776 "DefaultRubyParser.y"
  {
                  if (((ListNode)yyVals[-1+yyTop]).size() % 2 != 0) {
                      yyerror("odd number list for Hash.");
                  }
                  yyVal = ((ListNode)yyVals[-1+yyTop]);
              }
  break;
case 469:
					// line 1785 "DefaultRubyParser.y"
  { /* [!null]*/
                  yyVal = ((ListNode)yyVals[-2+yyTop]).addAll(((ListNode)yyVals[0+yyTop]));
              }
  break;
case 470:
					// line 1790 "DefaultRubyParser.y"
  { /* [!null]*/
                  yyVal = new ArrayNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop])).add(((Node)yyVals[0+yyTop]));
              }
  break;
case 490:
					// line 1802 "DefaultRubyParser.y"
  {
                  yyerrok();
              }
  break;
case 493:
					// line 1808 "DefaultRubyParser.y"
  {
                  yyerrok();
              }
  break;
case 494:
					// line 1812 "DefaultRubyParser.y"
  {
                  yyVal = null;
              }
  break;
case 495:
					// line 1816 "DefaultRubyParser.y"
  {  
                  yyVal = null;
	      }
  break;
					// line 7438 "-"
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

					// line 1821 "DefaultRubyParser.y"

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
					// line 7518 "-"
