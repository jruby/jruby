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
import org.jruby.ast.ListNode;
import org.jruby.ast.LiteralNode;
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
import org.jruby.ast.UnnamedRestArgNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.VAliasNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.ZArrayNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.ast.ZYieldNode;
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
import org.jruby.util.ByteList;

public class DefaultRubyParser implements RubyParser {
    protected ParserSupport support;
    protected RubyYaccLexer lexer;
    protected IRubyWarnings warnings;

    public DefaultRubyParser() {
        this(new ParserSupport());
    }

    public DefaultRubyParser(ParserSupport support) {
        this.support = support;
        lexer = new RubyYaccLexer();
        lexer.setParserSupport(support);
        support.setLexer(lexer);
    }

    public void setWarnings(IRubyWarnings warnings) {
        this.warnings = warnings;

        support.setWarnings(warnings);
        lexer.setWarnings(warnings);
    }
					// line 158 "-"
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
//yyLhs 512
    -1,   102,     0,    33,    32,    34,    34,    34,    34,   105,
    35,    35,    35,    35,    35,    35,    35,    35,    35,    35,
   106,    35,    35,    35,    35,    35,    35,    35,    35,    35,
    35,    35,    35,    35,    35,    36,    36,    36,    36,    36,
    36,    40,    31,    31,    31,    31,    31,    56,    56,    56,
   107,    91,    39,    39,    39,    39,    39,    39,    39,    39,
    92,    92,    94,    94,    93,    93,    93,    93,    93,    93,
    64,    64,    79,    79,    65,    65,    65,    65,    65,    65,
    65,    65,    72,    72,    72,    72,    72,    72,    72,    72,
     7,     7,    30,    30,    30,     8,     8,     8,     8,     8,
    97,    97,    98,    98,    60,   109,    60,     9,     9,     9,
     9,     9,     9,     9,     9,     9,     9,     9,     9,     9,
     9,     9,     9,     9,     9,     9,     9,     9,     9,     9,
     9,     9,     9,   108,   108,   108,   108,   108,   108,   108,
   108,   108,   108,   108,   108,   108,   108,   108,   108,   108,
   108,   108,   108,   108,   108,   108,   108,   108,   108,   108,
   108,   108,   108,   108,   108,   108,   108,   108,   108,   108,
   108,   108,   108,   108,    37,    37,    37,    37,    37,    37,
    37,    37,    37,    37,    37,    37,    37,    37,    37,    37,
    37,    37,    37,    37,    37,    37,    37,    37,    37,    37,
    37,    37,    37,    37,    37,    37,    37,    37,    37,    37,
    37,    37,    37,    37,    37,    37,    37,    37,    66,    69,
    69,    69,    69,    69,    69,    50,    50,    50,    50,    54,
    54,    46,    46,    46,    46,    46,    46,    46,    46,    46,
    47,    47,    47,    47,    47,    47,    47,    47,    47,    47,
    47,    47,   112,    52,    48,   113,    48,   114,    48,    85,
    84,    84,    78,    78,    63,    63,    63,    38,    38,    38,
    38,    38,    38,    38,    38,    38,    38,   115,    38,    38,
    38,    38,    38,    38,    38,    38,    38,    38,    38,    38,
    38,    38,    38,    38,    38,   117,   119,    38,   120,   121,
    38,    38,    38,    38,   122,   123,    38,   124,    38,   126,
   127,    38,   128,    38,   129,    38,   130,   131,    38,    38,
    38,    38,    38,    41,   116,   116,   116,   116,   118,   118,
   118,    44,    44,    42,    42,    99,    99,   100,   100,    70,
    70,    70,    70,    70,    70,    70,    70,    70,    70,    70,
    70,    71,    71,    71,    71,   132,    90,    55,    55,    55,
    23,    23,    23,    23,    23,    23,   133,    89,   134,    89,
    67,    83,    83,    83,    43,    43,    95,    95,    68,    68,
    68,    45,    45,    49,    49,    27,    27,    27,    15,    16,
    16,    17,    18,    19,    24,    24,    75,    75,    26,    26,
    25,    25,    74,    74,    20,    20,    21,    21,    22,   135,
    22,   136,    22,    61,    61,    61,    61,     3,     2,     2,
     2,     2,    29,    28,    28,    28,    28,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,    53,    96,
    62,    62,    51,   137,    51,    51,    57,    57,    58,    58,
    58,    58,    58,    58,    58,    58,    58,    10,    10,    10,
    10,    10,    76,    76,    76,    76,    59,    77,    77,    12,
    12,   101,   101,    13,    13,    88,    87,    87,    14,   138,
    14,    82,    82,    82,    80,    80,    81,     4,     4,     4,
     5,     5,     5,     5,     6,     6,     6,    11,    11,   103,
   103,   110,   110,   111,   111,   111,   125,   125,   104,   104,
    73,    86,
    }, yyLen = {
//yyLen 512
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
     1,     1,     1,     1,     1,     0,     4,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     3,     5,     3,     6,     5,     5,
     5,     5,     4,     3,     3,     3,     3,     3,     3,     3,
     3,     3,     4,     4,     2,     2,     3,     3,     3,     3,
     3,     3,     3,     3,     3,     3,     3,     3,     3,     2,
     2,     3,     3,     3,     3,     3,     5,     1,     1,     1,
     2,     2,     5,     2,     3,     3,     4,     4,     6,     1,
     1,     1,     2,     5,     2,     5,     4,     7,     3,     1,
     4,     3,     5,     7,     2,     5,     4,     6,     7,     9,
     3,     1,     0,     2,     1,     0,     3,     0,     4,     2,
     2,     1,     1,     3,     3,     4,     2,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     3,     0,     5,     3,
     3,     2,     4,     3,     3,     1,     4,     3,     1,     5,
     2,     1,     2,     6,     6,     0,     0,     7,     0,     0,
     7,     5,     4,     5,     0,     0,     9,     0,     6,     0,
     0,     8,     0,     5,     0,     6,     0,     0,     9,     1,
     1,     1,     1,     1,     1,     1,     1,     2,     1,     1,
     1,     1,     5,     1,     2,     1,     1,     1,     3,     1,
     2,     4,     7,     6,     4,     3,     5,     4,     2,     1,
     2,     1,     2,     1,     3,     0,     5,     2,     4,     4,
     2,     4,     4,     3,     2,     1,     0,     5,     0,     5,
     5,     1,     4,     2,     1,     1,     6,     0,     1,     1,
     1,     2,     1,     2,     1,     1,     1,     1,     1,     1,
     2,     3,     3,     3,     3,     3,     0,     3,     1,     2,
     3,     3,     0,     3,     0,     2,     0,     2,     1,     0,
     3,     0,     4,     1,     1,     1,     1,     2,     1,     1,
     1,     1,     3,     1,     1,     2,     2,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     0,     4,     2,     4,     2,     6,     4,
     4,     2,     4,     2,     2,     1,     0,     1,     1,     1,
     1,     1,     3,     1,     5,     3,     3,     1,     3,     1,
     1,     2,     1,     1,     1,     2,     2,     0,     1,     0,
     5,     1,     2,     2,     1,     3,     3,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     0,
     1,     0,     1,     0,     1,     1,     1,     1,     1,     2,
     0,     0,
    }, yyDefRed = {
//yyDefRed 914
     1,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   295,   298,     0,     0,     0,   321,   322,     0,
     0,     0,   433,   432,   434,   435,     0,     0,     0,    20,
     0,   437,   436,     0,     0,   429,   428,     0,   431,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   404,   406,   406,     0,     0,   440,   441,   423,   424,
     0,   386,     0,   268,     0,   389,   269,   270,     0,   271,
   272,   267,   385,   387,    35,     2,     0,     0,     0,     0,
     0,     0,     0,   273,     0,    43,     0,     0,    70,     0,
     5,     0,     0,    60,     0,     0,   319,   320,   285,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   438,     0,
    93,     0,   323,     0,   274,   312,   142,   153,   143,   166,
   139,   159,   149,   148,   164,   147,   146,   141,   167,   151,
   140,   154,   158,   160,   152,   145,   161,   168,   163,     0,
     0,     0,     0,   138,   157,   156,   169,   170,   171,   172,
   173,   137,   144,   135,   136,     0,     0,     0,    97,     0,
   128,   129,   126,   110,   111,   112,   115,   117,   113,   130,
   131,   118,   119,   479,   123,   122,   109,   127,   125,   124,
   120,   121,   116,   114,   107,   108,   132,   314,    98,     0,
   478,    99,   162,   155,   165,   150,   133,   134,    95,    96,
   101,   100,   103,     0,   102,   104,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   507,   506,     0,
     0,     0,   508,     0,     0,     0,     0,     0,     0,   335,
   336,     0,     0,     0,     0,     0,   231,    45,     0,     0,
     0,   484,   239,    46,    44,     0,    59,     0,     0,   364,
    58,    38,     0,     9,   502,     0,     0,     0,   194,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   219,     0,     0,   481,     0,     0,     0,     0,
     0,     0,     0,    68,   210,    39,   209,   420,   419,   421,
   417,   418,     0,     0,     0,     0,     0,     0,     0,     0,
   368,   366,   360,     0,   290,   390,   292,     4,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   355,   357,     0,     0,     0,     0,     0,     0,
    72,     0,     0,     0,     0,     0,     0,     0,   425,   426,
     0,    90,     0,    92,     0,   443,   307,   442,     0,     0,
     0,     0,     0,     0,   497,   498,   316,   105,     0,     0,
   276,     0,   326,   325,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   509,     0,     0,
     0,     0,     0,     0,   304,     0,   259,     0,     0,   232,
   261,     0,   234,   287,     0,     0,   254,   253,     0,     0,
     0,     0,     0,    11,    13,    12,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   279,     0,     0,
     0,   220,   283,     0,   504,   221,     0,   223,     0,   483,
   482,   284,     0,     0,     0,     0,   411,   409,   422,   408,
   407,   391,   405,   392,   393,   394,   395,   398,     0,   400,
   401,     0,     0,     0,    50,    53,     0,    15,    16,    17,
    18,    19,    36,    37,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   492,     0,     0,   493,     0,     0,     0,     0,
   363,     0,     0,   490,   491,     0,     0,    30,     0,     0,
    23,     0,    31,   262,     0,     0,    66,    73,    24,    33,
     0,    25,     0,     0,   445,     0,     0,     0,     0,     0,
     0,    94,     0,     0,     0,     0,   459,   458,   457,   460,
     0,   470,   469,   474,   473,     0,     0,     0,     0,     0,
   467,     0,     0,   455,     0,     0,     0,   379,     0,     0,
   380,     0,     0,   333,     0,   327,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   302,   330,
   329,   296,   328,   299,     0,     0,     0,     0,     0,     0,
     0,   238,   486,     0,     0,     0,   260,     0,     0,   485,
   286,     0,     0,   257,     0,     0,   251,     0,     0,     0,
     0,     0,   225,     0,    10,     0,     0,    22,     0,     0,
     0,     0,     0,   224,     0,   263,     0,     0,     0,     0,
     0,     0,     0,   397,   399,   403,   353,     0,     0,   351,
     0,     0,     0,     0,     0,     0,   230,     0,   361,   229,
     0,     0,   362,     0,     0,    48,   358,    49,   359,   266,
     0,     0,    71,   310,     0,     0,   282,   313,     0,     0,
     0,     0,   471,   475,     0,   447,     0,   451,     0,   453,
     0,   454,   317,   106,     0,     0,   382,   334,     0,     3,
   384,     0,   331,     0,     0,     0,     0,     0,     0,   301,
   303,   373,     0,     0,     0,     0,     0,     0,     0,     0,
   236,     0,     0,     0,     0,     0,   244,   256,   226,     0,
     0,   227,     0,     0,   289,    21,   278,     0,     0,     0,
   413,   414,   415,   410,   416,     0,     0,   352,   337,     0,
     0,     0,     0,     0,     0,     0,    27,     0,    28,     0,
    55,    29,     0,     0,    57,     0,     0,     0,     0,     0,
   444,   308,   480,   466,     0,   462,   315,     0,     0,   476,
     0,     0,   468,     0,     0,     0,     0,     0,     0,   381,
     0,   383,     0,   293,     0,   294,     0,     0,     0,     0,
   305,   233,     0,   235,   250,   258,     0,     0,     0,   241,
     0,     0,   222,   412,     0,     0,   350,   354,     0,   369,
   367,     0,   356,    26,     0,   265,     0,   446,     0,     0,
   449,   450,   452,     0,     0,     0,     0,     0,     0,     0,
   372,   374,   370,   375,   297,   300,     0,     0,     0,     0,
   240,     0,   246,     0,   228,     0,     0,     0,     0,   338,
    51,   311,   464,     0,     0,     0,     0,     0,     0,     0,
   376,     0,     0,   237,   242,     0,     0,     0,   245,   347,
     0,     0,     0,   341,   448,   318,     0,   332,   306,     0,
     0,   247,     0,   346,     0,     0,   243,     0,   248,   343,
     0,     0,   342,   249,
    }, yyDgoto = {
//yyDgoto 139
     1,   209,   290,    61,   109,   547,   520,   110,   201,   515,
   565,   376,   566,   567,   189,    63,    64,    65,    66,    67,
   293,   292,   460,    68,    69,    70,   468,    71,    72,    73,
   111,    74,   206,   207,    76,    77,    78,    79,    80,    81,
   211,   259,   712,   852,   713,   705,   237,   623,   417,   709,
   666,   366,   246,    83,   668,    84,    85,   568,   569,   570,
   203,   753,   213,   532,    87,    88,   238,   396,   579,   271,
   759,   658,   214,    90,   299,   297,   571,   572,   273,    91,
   274,   241,   278,   597,   409,   616,   410,   697,   789,   304,
   343,   475,    92,    93,   267,   379,   215,   204,   205,   231,
   760,   574,     2,   220,   221,   426,   256,   661,   191,   576,
   255,   445,   247,   627,   733,   439,   384,   223,   601,   724,
   224,   725,   609,   856,   546,   385,   543,   779,   371,   373,
   575,   794,   510,   473,   472,   652,   651,   545,   372,
    }, yySindex = {
//yySindex 914
     0,     0,  4346, 13396, 16963, 17332, 17917, 17809,  4346, 15241,
 15241,  6913,     0,     0, 17086, 13765, 13765,     0,     0, 13765,
  -255,  -252,     0,     0,     0,     0, 15241, 17701,   119,     0,
  -221,     0,     0,     0,     0,     0,     0,     0,     0, 16471,
 16471,  -141,  -146,  5316, 15241, 15364, 16471, 17455, 16471, 16594,
 18024,     0,     0,     0,   148,   174,     0,     0,     0,     0,
     0,     0,  -149,     0,  -104,     0,     0,     0,  -218,     0,
     0,     0,     0,     0,     0,     0,   135,   942,    12,  3878,
     0,   -98,   -56,     0,  -174,     0,   -72,   230,     0,   232,
     0, 17209,   244,     0,   -19,   942,     0,     0,     0,  -255,
  -252,   119,     0,     0,   -43, 15241,  -129,  4346,     0,  -149,
     0,    90,     0,   124,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  -160,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   283,     0,     0,   112,   130,   110,     0,
    12,    76,   165,   102,   397,   129,    76,     0,     0,   135,
   -62,   409,     0, 15241, 15241,   164,     0,   199,     0,     0,
     0,   216, 16471, 16471, 16471,  3878,     0,     0,   161,   459,
   474,     0,     0,     0,     0, 13519,     0, 13888, 13765,     0,
     0,     0,  -188,     0,     0, 15487,   169,  4346,     0,   213,
   209,   239,   243,   247,  5316,   253,     0,   270,    12, 16471,
   119,   269,     0,   160,   172,     0,   186,   172,   250,   278,
     0,   268,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   514,   663,   718,   316,   258,   752,   264,  -138,
     0,     0,     0,   276,     0,     0,     0,     0, 13273, 15241,
 15241, 15241, 15241, 13396, 15241, 15241, 16471, 16471, 16471, 16471,
 16471, 16471, 16471, 16471, 16471, 16471, 16471, 16471, 16471, 16471,
 16471, 16471, 16471, 16471, 16471, 16471, 16471, 16471, 16471, 16471,
 16471, 16471,     0,     0,  4816, 18186, 15364, 18241, 18241, 16594,
     0, 15610,  5316, 17455,   593, 15610, 16594,   298,     0,     0,
    12,     0,     0,     0,   135,     0,     0,     0, 18241, 18296,
 15364,  4346, 15241,  1094,     0,     0,     0,     0, 15733,   370,
     0,   247,     0,     0,  4346,   378, 18351, 18406, 15364, 16471,
 16471, 16471,  4346,   377,  4346, 15856,   386,     0,   168,   168,
     0, 18461, 18516, 15364,     0,   607,     0, 16471, 14011,     0,
     0, 14134,     0,     0,   312, 13642,     0,     0,   -98,   119,
    13,   318,   620,     0,     0,     0, 17809, 15241,  3878,  4346,
   305, 18351, 18406, 16471, 16471, 16471,   329,     0,     0,   119,
  2303,     0,     0, 15979,     0,     0, 16471,     0, 16471,     0,
     0,     0,     0, 18571, 18626, 15364,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     2,     0,
     0,   640,  -133,  -133,     0,     0,   942,     0,     0,     0,
     0,     0,     0,     0,   209,  3794,  3794,  3794,  3794,  1208,
  1208,  3313,  2827,  3794,  3794,  2366,  2366,   824,   824,   209,
  1880,   209,   209,   394,   394,  1208,  1208,   873,   873,  2427,
  -133,   336,     0,   338,  -252,     0,   343,     0,   344,  -252,
     0,     0,   333,     0,     0,  -252,  -252,     0,  3878, 16471,
     0,  3392,     0,     0,   645,   354,     0,     0,     0,     0,
     0,     0,  3878,   135,     0, 15241,  4346,  -252,     0,     0,
  -252,     0,   353,   420,    67,   637,     0,     0,     0,     0,
  1410,     0,     0,     0,     0,   362,   395,   398,  4346,   135,
     0,   659,   661,     0,   664, 18131, 17809,     0,     0,   375,
     0,  4346,   457,     0,    96,     0,   389,   401,   403,   344,
   387,  3392,   370,   479,   487, 16471,   716,    76,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   422, 15241,
   419,     0,     0, 16471,   161,   728,     0, 16471,   161,     0,
     0, 16471,  3878,     0,    29,   731,     0,   435,   437, 18241,
 18241,   441,     0, 14257,     0,   -55,   423,     0,   209,   209,
  3878,     0,   453,     0, 16471,     0,     0,     0,     0,     0,
   452,  4346,  -156,     0,     0,     0,     0, 16717,  4346,     0,
  4346,  -133, 16471,  4346, 16594, 16594,     0,   276,     0,     0,
 16594, 16471,     0,   276,   460,     0,     0,     0,     0,     0,
 16471, 16102,     0,     0,   135,   536,     0,     0,   461, 16471,
   119, 16471,     0,     0,   539,     0,  1410,     0,   502,     0,
   128,     0,     0,     0, 17578,    76,     0,     0,  4346,     0,
     0, 15241,     0,   542, 16471, 16471, 16471,   470,   550,     0,
     0,     0, 16225,  4346,  4346,  4346,     0,   168,   607, 14380,
     0,   607,   607,   475, 14503, 14626,     0,     0,     0,  -252,
  -252,     0,   -98,    13,     0,     0,     0,  2303,     0,   456,
     0,     0,     0,     0,     0, 13027, 17578,     0,     0,   465,
   775,   557,   478,  4346,  3878,   568,     0,  3878,     0,  3878,
     0,     0,  3878,  3878,     0, 16594,  3878, 16471,     0,  4346,
     0,     0,     0,     0,   495,     0,     0,   499,   800,     0,
   664,   637,     0,   664,  1094,   547,     0,   289,     0,     0,
  4346,     0,    76,     0, 16471,     0, 16471,   191,   582,   595,
     0,     0, 16471,     0,     0,     0, 16471,   816,   817,     0,
 16471,   526,     0,     0,   521,   828,     0,     0, 16840,     0,
     0,   511,     0,     0,  3878,     0,   613,     0, 16471,   502,
     0,     0,     0,  4346,     0, 18681, 18736, 15364,   112,  4346,
     0,     0,     0,     0,     0,     0,  4346,  2906,   607, 14749,
     0, 14872,     0,   607,     0, 17578,   529, 13150, 17578,     0,
     0,     0,     0,   664,   618,     0,     0,     0,     0,   553,
     0,    96,   634,     0,     0, 16471,   859, 16471,     0,     0,
 17578,   555,   866,     0,     0,     0,     0,     0,     0,   607,
 14995,     0,   607,     0, 17578,   565,     0, 16471,     0,     0,
 17578,   607,     0,     0,
    }, yyRindex = {
//yyRindex 914
     0,     0,   138,     0,     0,     0,     0,     0,    68,     0,
     0,   201,     0,     0,     0,  8493,  8622,     0,     0,  8733,
  4615,  4006,     0,     0,     0,     0,     0,     0, 16348,     0,
     0,     0,     0,  2062,  3157,     0,     0,  2185,     0,     0,
     0,     0,     0,   100,     0,   569,   554,   142,     0,     0,
   823,     0,     0,     0,   831,   214,     0,     0,     0,     0,
  9693,     0, 15118,     0,  7773,     0,     0,     0,  7902,     0,
     0,     0,     0,     0,     0,     0,   218,   669,  1912,  3762,
  8013,  4248,     0,     0,  4734,     0,  9822,     0,     0,     0,
     0,   147,     0,     0,     0,   725,     0,     0,     0,  8142,
  7053,   577,  5857,  5999,     0,     0,     0,   100,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  1952,
  2793,  3279,  3765,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  4251,  4737,  5221,     0,  6270,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,  5217,     0,     0,   282,     0,     0,  7182,
  1614,     0,     0,  7422,     0,     0,     0,     0,     0,   648,
     0,   321,     0,     0,     0,     0,   233,     0,   458,     0,
     0,     0,     0,     0,     0, 12029,     0,     0, 12857,  1745,
  1745,     0,     0,     0,     0,     0,     0,     0,   584,     0,
     0,     0,     0,     0,     0,     0,     0,    22,     0,     0,
  8862,  8253,  8382,  9933,   100,     0,    35,     0,    34,     0,
   586,     0,     0,   598,   598,     0,   566,   566,     0,     0,
   583,     0,  1261,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,  2304,     0,     0,     0,     0,   494,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   569,     0,     0,     0,
     0,     0,   100,   226,   236,     0,     0,     0,     0,     0,
   141,     0,  6386,     0,     0,     0,     0,     0,     0,     0,
   569,    68,     0,   152,     0,     0,     0,     0,   632,   228,
     0,  7533,     0,     0,   523,  6515,     0,     0,   569,     0,
     0,     0,   240,     0,    88,     0,     0,     0,     0,     0,
   541,     0,     0,   569,     0,  1745,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   615,     0,     0,    66,   619,
   619,     0,    74,     0,     0,     0,     0,     0, 12114,    22,
     0,     0,     0,     0,     0,     0,     0,     0,   132,   619,
   586,     0,     0,   605,     0,     0,   -77,     0,   592,     0,
     0,     0,  1349,     0,     0,   569,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  6644,  6784,     0,     0,   805,     0,     0,     0,
     0,     0,     0,     0,  8973,  1786,  1874, 11376, 11482, 10929,
 11044, 11567, 11822, 11652, 11737, 11907, 11944, 10380, 10486,  9102,
 10597,  9213,  9342, 10154, 10265, 11150, 11261, 10712, 10818,     0,
  6644,  4976,     0,  5099,  4129,     0,  5462,  3520,  5585, 15118,
     0,  3643,     0,     0,     0,  5708,  5708,     0, 12199,     0,
     0,   789,     0,     0,     0,     0,     0,     0,     0,     0,
  1437,     0, 12236,     0,     0,     0,    68,  7293,  6128,  6257,
     0,     0,     0,     0,   619,    64,     0,     0,     0,     0,
   116,     0,     0,     0,     0,    48,    65,     0,    68,     0,
     0,   101,   101,     0,   101,     0,     0,     0,   463,   588,
     0,   564,   675,     0,   675,     0,  2548,  2671,  3034,  4492,
     0, 12894,   675,     0,     0,     0,   754,     0,     0,     0,
     0,     0,     0,     0,   814,  1143,  1259,   197,     0,     0,
     0,     0,     0,     0, 12979,  1745,     0,     0,     0,     0,
     0,     0,   319,     0,     0,   623,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  9453,  9582,
 12321,    97,     0,     0,     0,     0,   683,  1057,  8836,  1251,
     0,    22,     0,     0,     0,     0,     0,     0,    88,     0,
    22,  6784,     0,    88,     0,     0,     0,  2790,     0,     0,
     0,     0,     0,  3276, 10044,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   619,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,    88,     0,
     0,     0,     0,     0,     0,     0,     0,  7662,     0,     0,
     0,     0,     0,    91,    88,    88,   608,     0,  1745,     0,
     0,  1745,   623,     0,     0,     0,     0,     0,     0,   120,
   120,     0,     0,   619,     0,     0,     0,   586,  1773,     0,
     0,     0,     0,     0,     0,   603,     0,     0,     0,     0,
   609,     0,     0,    22, 12358,     0,     0, 12443,     0, 12528,
     0,     0, 12565, 12650,     0,     0, 12687,     0,   948,    68,
     0,     0,     0,     0,     0,     0,     0,    77,   101,     0,
   101,     0,     0,   101,   152,     0,   173,     0,   200,     0,
    68,     0,     0,     0,     0,     0,     0,   675,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   623,   623,     0,
     0,     0,     0,     0,     0,   610,     0,     0,   611,     0,
     0,     0,     0,     0, 12772,     0,     0,     0,     0,     0,
     0,     0,     0,    68,   780,     0,     0,   569,   282,   523,
     0,     0,     0,     0,     0,     0,    88,  1745,   623,     0,
     0,     0,     0,   623,     0,     0,     0,   621,     0,     0,
     0,     0,     0,   101,     0,  1595,  1623,  1910,   189,     0,
     0,   675,     0,     0,     0,     0,   623,     0,     0,     0,
     0,     0,   622,     0,     0,     0,  1189,     0,     0,   623,
     0,     0,   623,     0,     0,     0,     0,     0,     0,     0,
     0,   623,     0,     0,
    }, yyGindex = {
//yyGindex 139
     0,    25,     0,    11,  1117,  -230,     0,   -41,    14,    43,
   281,     0,     0,     0,     0,     0,     0,   902,     0,     0,
     0,   520,  -107,     0,     0,     0,     0,     0,     0,    39,
   973,   -30,   340,  -345,     0,    58,  1006,   936,    55,   150,
     5,    -2,  -322,     0,   105,     0,   699,     0,     0,     0,
    33,     0,    50,   977,   103,  -223,     0,   196,   439,  -579,
     0,     0,   249,  -211,   -87,    32,  1411,  -383,     0,  -266,
     0,  -268,   171,  1112,     0,     0,     0,   299,    36,     0,
    21,  -390,     0,     0,  -192,    75,     0,  -274,  -362,   933,
     0,  -372,   988,    70,  -181,   177,  1142,     0,   -22,     0,
     0,  -599,     0,    30,   932,     0,     0,     0,     0,     0,
    24,    -1,     0,     0,     0,     0,  -194,     0,  -346,     0,
     0,     0,     0,     0,     0,    52,     0,     0,     0,     0,
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
    "stmt : primary_value '[' aref_args tRBRACK tOP_ASGN command_call",
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
    "cmd_brace_block : tLBRACE_ARG $$4 opt_block_var compstmt tRCURLY",
    "command : operation command_args",
    "command : operation command_args cmd_brace_block",
    "command : primary_value tDOT operation2 command_args",
    "command : primary_value tDOT operation2 command_args cmd_brace_block",
    "command : primary_value tCOLON2 operation2 command_args",
    "command : primary_value tCOLON2 operation2 command_args cmd_brace_block",
    "command : kSUPER command_args",
    "command : kYIELD command_args",
    "mlhs : mlhs_basic",
    "mlhs : tLPAREN mlhs_entry tRPAREN",
    "mlhs_entry : mlhs_basic",
    "mlhs_entry : tLPAREN mlhs_entry tRPAREN",
    "mlhs_basic : mlhs_head",
    "mlhs_basic : mlhs_head mlhs_item",
    "mlhs_basic : mlhs_head tSTAR mlhs_node",
    "mlhs_basic : mlhs_head tSTAR",
    "mlhs_basic : tSTAR mlhs_node",
    "mlhs_basic : tSTAR",
    "mlhs_item : mlhs_node",
    "mlhs_item : tLPAREN mlhs_entry tRPAREN",
    "mlhs_head : mlhs_item ','",
    "mlhs_head : mlhs_head mlhs_item ','",
    "mlhs_node : variable",
    "mlhs_node : primary_value '[' aref_args tRBRACK",
    "mlhs_node : primary_value tDOT tIDENTIFIER",
    "mlhs_node : primary_value tCOLON2 tIDENTIFIER",
    "mlhs_node : primary_value tDOT tCONSTANT",
    "mlhs_node : primary_value tCOLON2 tCONSTANT",
    "mlhs_node : tCOLON3 tCONSTANT",
    "mlhs_node : backref",
    "lhs : variable",
    "lhs : primary_value '[' aref_args tRBRACK",
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
    "op : tGT",
    "op : tGEQ",
    "op : tLT",
    "op : tLEQ",
    "op : tLSHFT",
    "op : tRSHFT",
    "op : tPLUS",
    "op : tMINUS",
    "op : tSTAR2",
    "op : tSTAR",
    "op : tDIVIDE",
    "op : tPERCENT",
    "op : tPOW",
    "op : tTILDE",
    "op : tUPLUS",
    "op : tUMINUS",
    "op : tAREF",
    "op : tASET",
    "op : tBACK_REF2",
    "reswords : k__LINE__",
    "reswords : k__FILE__",
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
    "arg : primary_value '[' aref_args tRBRACK tOP_ASGN arg",
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
    "arg : arg '?' arg ':' arg",
    "arg : primary",
    "arg_value : arg",
    "aref_args : none",
    "aref_args : command opt_nl",
    "aref_args : args trailer",
    "aref_args : args ',' tSTAR arg opt_nl",
    "aref_args : assocs trailer",
    "aref_args : tSTAR arg opt_nl",
    "paren_args : tLPAREN2 none tRPAREN",
    "paren_args : tLPAREN2 call_args opt_nl tRPAREN",
    "paren_args : tLPAREN2 block_call opt_nl tRPAREN",
    "paren_args : tLPAREN2 args ',' block_call opt_nl tRPAREN",
    "opt_paren_args : none",
    "opt_paren_args : paren_args",
    "call_args : command",
    "call_args : args opt_block_arg",
    "call_args : args ',' tSTAR arg_value opt_block_arg",
    "call_args : assocs opt_block_arg",
    "call_args : assocs ',' tSTAR arg_value opt_block_arg",
    "call_args : args ',' assocs opt_block_arg",
    "call_args : args ',' assocs ',' tSTAR arg opt_block_arg",
    "call_args : tSTAR arg_value opt_block_arg",
    "call_args : block_arg",
    "call_args2 : arg_value ',' args opt_block_arg",
    "call_args2 : arg_value ',' block_arg",
    "call_args2 : arg_value ',' tSTAR arg_value opt_block_arg",
    "call_args2 : arg_value ',' args ',' tSTAR arg_value opt_block_arg",
    "call_args2 : assocs opt_block_arg",
    "call_args2 : assocs ',' tSTAR arg_value opt_block_arg",
    "call_args2 : arg_value ',' assocs opt_block_arg",
    "call_args2 : arg_value ',' args ',' assocs opt_block_arg",
    "call_args2 : arg_value ',' assocs ',' tSTAR arg_value opt_block_arg",
    "call_args2 : arg_value ',' args ',' assocs ',' tSTAR arg_value opt_block_arg",
    "call_args2 : tSTAR arg_value opt_block_arg",
    "call_args2 : block_arg",
    "$$6 :",
    "command_args : $$6 open_args",
    "open_args : call_args",
    "$$7 :",
    "open_args : tLPAREN_ARG $$7 tRPAREN",
    "$$8 :",
    "open_args : tLPAREN_ARG call_args2 $$8 tRPAREN",
    "block_arg : tAMPER arg_value",
    "opt_block_arg : ',' block_arg",
    "opt_block_arg : none_block_pass",
    "args : arg_value",
    "args : args ',' arg_value",
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
    "$$9 :",
    "primary : tLPAREN_ARG expr $$9 opt_nl tRPAREN",
    "primary : tLPAREN compstmt tRPAREN",
    "primary : primary_value tCOLON2 tCONSTANT",
    "primary : tCOLON3 tCONSTANT",
    "primary : primary_value '[' aref_args tRBRACK",
    "primary : tLBRACK aref_args tRBRACK",
    "primary : tLBRACE assoc_list tRCURLY",
    "primary : kRETURN",
    "primary : kYIELD tLPAREN2 call_args tRPAREN",
    "primary : kYIELD tLPAREN2 tRPAREN",
    "primary : kYIELD",
    "primary : kDEFINED opt_nl tLPAREN2 expr tRPAREN",
    "primary : operation brace_block",
    "primary : method_call",
    "primary : method_call brace_block",
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
    "primary : kCASE opt_terms kELSE compstmt kEND",
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
    "then : ':'",
    "then : kTHEN",
    "then : term kTHEN",
    "do : term",
    "do : ':'",
    "do : kDO_COND",
    "if_tail : opt_else",
    "if_tail : kELSIF expr_value then compstmt if_tail",
    "opt_else : none",
    "opt_else : kELSE compstmt",
    "for_var : lhs",
    "for_var : mlhs",
    "block_par : mlhs_item",
    "block_par : block_par ',' mlhs_item",
    "block_var : block_par",
    "block_var : block_par ','",
    "block_var : block_par ',' tAMPER lhs",
    "block_var : block_par ',' tSTAR lhs ',' tAMPER lhs",
    "block_var : block_par ',' tSTAR ',' tAMPER lhs",
    "block_var : block_par ',' tSTAR lhs",
    "block_var : block_par ',' tSTAR",
    "block_var : tSTAR lhs ',' tAMPER lhs",
    "block_var : tSTAR ',' tAMPER lhs",
    "block_var : tSTAR lhs",
    "block_var : tSTAR",
    "block_var : tAMPER lhs",
    "opt_block_var : none",
    "opt_block_var : tPIPE tPIPE",
    "opt_block_var : tOROP",
    "opt_block_var : tPIPE block_var tPIPE",
    "$$23 :",
    "do_block : kDO_BLOCK $$23 opt_block_var compstmt kEND",
    "block_call : command do_block",
    "block_call : block_call tDOT operation2 opt_paren_args",
    "block_call : block_call tCOLON2 operation2 opt_paren_args",
    "method_call : operation paren_args",
    "method_call : primary_value tDOT operation2 opt_paren_args",
    "method_call : primary_value tCOLON2 operation2 paren_args",
    "method_call : primary_value tCOLON2 operation3",
    "method_call : kSUPER paren_args",
    "method_call : kSUPER",
    "$$24 :",
    "brace_block : tLCURLY $$24 opt_block_var compstmt tRCURLY",
    "$$25 :",
    "brace_block : kDO $$25 opt_block_var compstmt kEND",
    "case_body : kWHEN when_args then compstmt cases",
    "when_args : args",
    "when_args : args ',' tSTAR arg_value",
    "when_args : tSTAR arg_value",
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
    "$$26 :",
    "string_content : tSTRING_DVAR $$26 string_dvar",
    "$$27 :",
    "string_content : tSTRING_DBEG $$27 compstmt tRCURLY",
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
    "var_ref : variable",
    "var_lhs : variable",
    "backref : tNTH_REF",
    "backref : tBACK_REF",
    "superclass : term",
    "$$28 :",
    "superclass : tLT $$28 expr_value term",
    "superclass : error term",
    "f_arglist : tLPAREN2 f_args opt_nl tRPAREN",
    "f_arglist : f_args term",
    "f_args : f_arg ',' f_optarg ',' f_rest_arg opt_f_block_arg",
    "f_args : f_arg ',' f_optarg opt_f_block_arg",
    "f_args : f_arg ',' f_rest_arg opt_f_block_arg",
    "f_args : f_arg opt_f_block_arg",
    "f_args : f_optarg ',' f_rest_arg opt_f_block_arg",
    "f_args : f_optarg opt_f_block_arg",
    "f_args : f_rest_arg opt_f_block_arg",
    "f_args : f_block_arg",
    "f_args :",
    "f_norm_arg : tCONSTANT",
    "f_norm_arg : tIVAR",
    "f_norm_arg : tGVAR",
    "f_norm_arg : tCVAR",
    "f_norm_arg : tIDENTIFIER",
    "f_arg : f_norm_arg tASSOC arg_value",
    "f_arg : f_norm_arg",
    "f_arg : f_arg ',' f_norm_arg tASSOC arg_value",
    "f_arg : f_arg ',' f_norm_arg",
    "f_opt : tIDENTIFIER '=' arg_value",
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
    "$$29 :",
    "singleton : tLPAREN2 $$29 expr opt_nl tRPAREN",
    "assoc_list : none",
    "assoc_list : assocs trailer",
    "assoc_list : args trailer",
    "assocs : assoc",
    "assocs : assocs ',' assoc",
    "assoc : arg_value tASSOC arg_value",
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
case 1: yyVal = case1_line274(yyVal, yyVals, yyTop); // line 274
break;
case 2: yyVal = case2_line277(yyVal, yyVals, yyTop); // line 277
break;
case 3: yyVal = case3_line289(yyVal, yyVals, yyTop); // line 289
break;
case 4: yyVal = case4_line306(yyVal, yyVals, yyTop); // line 306
break;
case 6: yyVal = case6_line314(yyVal, yyVals, yyTop); // line 314
break;
case 7: yyVal = case7_line317(yyVal, yyVals, yyTop); // line 317
break;
case 8: yyVal = case8_line320(yyVal, yyVals, yyTop); // line 320
break;
case 9: yyVal = case9_line324(yyVal, yyVals, yyTop); // line 324
break;
case 10: yyVal = case10_line326(yyVal, yyVals, yyTop); // line 326
break;
case 11: yyVal = case11_line329(yyVal, yyVals, yyTop); // line 329
break;
case 12: yyVal = case12_line332(yyVal, yyVals, yyTop); // line 332
break;
case 13: yyVal = case13_line335(yyVal, yyVals, yyTop); // line 335
break;
case 14: yyVal = case14_line338(yyVal, yyVals, yyTop); // line 338
break;
case 15: yyVal = case15_line341(yyVal, yyVals, yyTop); // line 341
break;
case 16: yyVal = case16_line344(yyVal, yyVals, yyTop); // line 344
break;
case 17: yyVal = case17_line347(yyVal, yyVals, yyTop); // line 347
break;
case 18: yyVal = case18_line354(yyVal, yyVals, yyTop); // line 354
break;
case 19: yyVal = case19_line361(yyVal, yyVals, yyTop); // line 361
break;
case 20: yyVal = case20_line365(yyVal, yyVals, yyTop); // line 365
break;
case 21: yyVal = case21_line370(yyVal, yyVals, yyTop); // line 370
break;
case 22: yyVal = case22_line375(yyVal, yyVals, yyTop); // line 375
break;
case 23: yyVal = case23_line381(yyVal, yyVals, yyTop); // line 381
break;
case 24: yyVal = case24_line385(yyVal, yyVals, yyTop); // line 385
break;
case 25: yyVal = case25_line394(yyVal, yyVals, yyTop); // line 394
break;
case 26: yyVal = case26_line410(yyVal, yyVals, yyTop); // line 410
break;
case 27: yyVal = case27_line414(yyVal, yyVals, yyTop); // line 414
break;
case 28: yyVal = case28_line417(yyVal, yyVals, yyTop); // line 417
break;
case 29: yyVal = case29_line420(yyVal, yyVals, yyTop); // line 420
break;
case 30: yyVal = case30_line423(yyVal, yyVals, yyTop); // line 423
break;
case 31: yyVal = case31_line426(yyVal, yyVals, yyTop); // line 426
break;
case 32: yyVal = case32_line429(yyVal, yyVals, yyTop); // line 429
break;
case 33: yyVal = case33_line437(yyVal, yyVals, yyTop); // line 437
break;
case 36: yyVal = case36_line446(yyVal, yyVals, yyTop); // line 446
break;
case 37: yyVal = case37_line449(yyVal, yyVals, yyTop); // line 449
break;
case 38: yyVal = case38_line452(yyVal, yyVals, yyTop); // line 452
break;
case 39: yyVal = case39_line455(yyVal, yyVals, yyTop); // line 455
break;
case 41: yyVal = case41_line460(yyVal, yyVals, yyTop); // line 460
break;
case 44: yyVal = case44_line467(yyVal, yyVals, yyTop); // line 467
break;
case 45: yyVal = case45_line470(yyVal, yyVals, yyTop); // line 470
break;
case 46: yyVal = case46_line473(yyVal, yyVals, yyTop); // line 473
break;
case 48: yyVal = case48_line479(yyVal, yyVals, yyTop); // line 479
break;
case 49: yyVal = case49_line482(yyVal, yyVals, yyTop); // line 482
break;
case 50: yyVal = case50_line487(yyVal, yyVals, yyTop); // line 487
break;
case 51: yyVal = case51_line489(yyVal, yyVals, yyTop); // line 489
break;
case 52: yyVal = case52_line495(yyVal, yyVals, yyTop); // line 495
break;
case 53: yyVal = case53_line498(yyVal, yyVals, yyTop); // line 498
break;
case 54: yyVal = case54_line501(yyVal, yyVals, yyTop); // line 501
break;
case 55: yyVal = case55_line504(yyVal, yyVals, yyTop); // line 504
break;
case 56: yyVal = case56_line507(yyVal, yyVals, yyTop); // line 507
break;
case 57: yyVal = case57_line510(yyVal, yyVals, yyTop); // line 510
break;
case 58: yyVal = case58_line513(yyVal, yyVals, yyTop); // line 513
break;
case 59: yyVal = case59_line516(yyVal, yyVals, yyTop); // line 516
break;
case 61: yyVal = case61_line522(yyVal, yyVals, yyTop); // line 522
break;
case 63: yyVal = case63_line528(yyVal, yyVals, yyTop); // line 528
break;
case 64: yyVal = case64_line533(yyVal, yyVals, yyTop); // line 533
break;
case 65: yyVal = case65_line536(yyVal, yyVals, yyTop); // line 536
break;
case 66: yyVal = case66_line540(yyVal, yyVals, yyTop); // line 540
break;
case 67: yyVal = case67_line543(yyVal, yyVals, yyTop); // line 543
break;
case 68: yyVal = case68_line546(yyVal, yyVals, yyTop); // line 546
break;
case 69: yyVal = case69_line549(yyVal, yyVals, yyTop); // line 549
break;
case 71: yyVal = case71_line554(yyVal, yyVals, yyTop); // line 554
break;
case 72: yyVal = case72_line559(yyVal, yyVals, yyTop); // line 559
break;
case 73: yyVal = case73_line562(yyVal, yyVals, yyTop); // line 562
break;
case 74: yyVal = case74_line566(yyVal, yyVals, yyTop); // line 566
break;
case 75: yyVal = case75_line569(yyVal, yyVals, yyTop); // line 569
break;
case 76: yyVal = case76_line572(yyVal, yyVals, yyTop); // line 572
break;
case 77: yyVal = case77_line575(yyVal, yyVals, yyTop); // line 575
break;
case 78: yyVal = case78_line578(yyVal, yyVals, yyTop); // line 578
break;
case 79: yyVal = case79_line581(yyVal, yyVals, yyTop); // line 581
break;
case 80: yyVal = case80_line590(yyVal, yyVals, yyTop); // line 590
break;
case 81: yyVal = case81_line599(yyVal, yyVals, yyTop); // line 599
break;
case 82: yyVal = case82_line604(yyVal, yyVals, yyTop); // line 604
break;
case 83: yyVal = case83_line607(yyVal, yyVals, yyTop); // line 607
break;
case 84: yyVal = case84_line610(yyVal, yyVals, yyTop); // line 610
break;
case 85: yyVal = case85_line613(yyVal, yyVals, yyTop); // line 613
break;
case 86: yyVal = case86_line616(yyVal, yyVals, yyTop); // line 616
break;
case 87: yyVal = case87_line619(yyVal, yyVals, yyTop); // line 619
break;
case 88: yyVal = case88_line628(yyVal, yyVals, yyTop); // line 628
break;
case 89: yyVal = case89_line637(yyVal, yyVals, yyTop); // line 637
break;
case 90: yyVal = case90_line641(yyVal, yyVals, yyTop); // line 641
break;
case 92: yyVal = case92_line646(yyVal, yyVals, yyTop); // line 646
break;
case 93: yyVal = case93_line649(yyVal, yyVals, yyTop); // line 649
break;
case 94: yyVal = case94_line652(yyVal, yyVals, yyTop); // line 652
break;
case 98: yyVal = case98_line658(yyVal, yyVals, yyTop); // line 658
break;
case 99: yyVal = case99_line663(yyVal, yyVals, yyTop); // line 663
break;
case 100: yyVal = case100_line669(yyVal, yyVals, yyTop); // line 669
break;
case 101: yyVal = case101_line672(yyVal, yyVals, yyTop); // line 672
break;
case 102: yyVal = case102_line677(yyVal, yyVals, yyTop); // line 677
break;
case 103: yyVal = case103_line680(yyVal, yyVals, yyTop); // line 680
break;
case 104: yyVal = case104_line684(yyVal, yyVals, yyTop); // line 684
break;
case 105: yyVal = case105_line687(yyVal, yyVals, yyTop); // line 687
break;
case 106: yyVal = case106_line689(yyVal, yyVals, yyTop); // line 689
break;
case 174: yyVal = case174_line708(yyVal, yyVals, yyTop); // line 708
break;
case 175: yyVal = case175_line713(yyVal, yyVals, yyTop); // line 713
break;
case 176: yyVal = case176_line718(yyVal, yyVals, yyTop); // line 718
break;
case 177: yyVal = case177_line734(yyVal, yyVals, yyTop); // line 734
break;
case 178: yyVal = case178_line737(yyVal, yyVals, yyTop); // line 737
break;
case 179: yyVal = case179_line740(yyVal, yyVals, yyTop); // line 740
break;
case 180: yyVal = case180_line743(yyVal, yyVals, yyTop); // line 743
break;
case 181: yyVal = case181_line746(yyVal, yyVals, yyTop); // line 746
break;
case 182: yyVal = case182_line749(yyVal, yyVals, yyTop); // line 749
break;
case 183: yyVal = case183_line752(yyVal, yyVals, yyTop); // line 752
break;
case 184: yyVal = case184_line755(yyVal, yyVals, yyTop); // line 755
break;
case 185: yyVal = case185_line762(yyVal, yyVals, yyTop); // line 762
break;
case 186: yyVal = case186_line768(yyVal, yyVals, yyTop); // line 768
break;
case 187: yyVal = case187_line771(yyVal, yyVals, yyTop); // line 771
break;
case 188: yyVal = case188_line774(yyVal, yyVals, yyTop); // line 774
break;
case 189: yyVal = case189_line777(yyVal, yyVals, yyTop); // line 777
break;
case 190: yyVal = case190_line780(yyVal, yyVals, yyTop); // line 780
break;
case 191: yyVal = case191_line783(yyVal, yyVals, yyTop); // line 783
break;
case 192: yyVal = case192_line786(yyVal, yyVals, yyTop); // line 786
break;
case 193: yyVal = case193_line789(yyVal, yyVals, yyTop); // line 789
break;
case 194: yyVal = case194_line792(yyVal, yyVals, yyTop); // line 792
break;
case 195: yyVal = case195_line799(yyVal, yyVals, yyTop); // line 799
break;
case 196: yyVal = case196_line802(yyVal, yyVals, yyTop); // line 802
break;
case 197: yyVal = case197_line805(yyVal, yyVals, yyTop); // line 805
break;
case 198: yyVal = case198_line808(yyVal, yyVals, yyTop); // line 808
break;
case 199: yyVal = case199_line811(yyVal, yyVals, yyTop); // line 811
break;
case 200: yyVal = case200_line814(yyVal, yyVals, yyTop); // line 814
break;
case 201: yyVal = case201_line817(yyVal, yyVals, yyTop); // line 817
break;
case 202: yyVal = case202_line820(yyVal, yyVals, yyTop); // line 820
break;
case 203: yyVal = case203_line823(yyVal, yyVals, yyTop); // line 823
break;
case 204: yyVal = case204_line826(yyVal, yyVals, yyTop); // line 826
break;
case 205: yyVal = case205_line829(yyVal, yyVals, yyTop); // line 829
break;
case 206: yyVal = case206_line832(yyVal, yyVals, yyTop); // line 832
break;
case 207: yyVal = case207_line835(yyVal, yyVals, yyTop); // line 835
break;
case 208: yyVal = case208_line838(yyVal, yyVals, yyTop); // line 838
break;
case 209: yyVal = case209_line841(yyVal, yyVals, yyTop); // line 841
break;
case 210: yyVal = case210_line844(yyVal, yyVals, yyTop); // line 844
break;
case 211: yyVal = case211_line847(yyVal, yyVals, yyTop); // line 847
break;
case 212: yyVal = case212_line850(yyVal, yyVals, yyTop); // line 850
break;
case 213: yyVal = case213_line853(yyVal, yyVals, yyTop); // line 853
break;
case 214: yyVal = case214_line856(yyVal, yyVals, yyTop); // line 856
break;
case 215: yyVal = case215_line859(yyVal, yyVals, yyTop); // line 859
break;
case 216: yyVal = case216_line862(yyVal, yyVals, yyTop); // line 862
break;
case 217: yyVal = case217_line865(yyVal, yyVals, yyTop); // line 865
break;
case 218: yyVal = case218_line869(yyVal, yyVals, yyTop); // line 869
break;
case 220: yyVal = case220_line875(yyVal, yyVals, yyTop); // line 875
break;
case 221: yyVal = case221_line878(yyVal, yyVals, yyTop); // line 878
break;
case 222: yyVal = case222_line881(yyVal, yyVals, yyTop); // line 881
break;
case 223: yyVal = case223_line885(yyVal, yyVals, yyTop); // line 885
break;
case 224: yyVal = case224_line888(yyVal, yyVals, yyTop); // line 888
break;
case 225: yyVal = case225_line893(yyVal, yyVals, yyTop); // line 893
break;
case 226: yyVal = case226_line896(yyVal, yyVals, yyTop); // line 896
break;
case 227: yyVal = case227_line900(yyVal, yyVals, yyTop); // line 900
break;
case 228: yyVal = case228_line903(yyVal, yyVals, yyTop); // line 903
break;
case 231: yyVal = case231_line910(yyVal, yyVals, yyTop); // line 910
break;
case 232: yyVal = case232_line913(yyVal, yyVals, yyTop); // line 913
break;
case 233: yyVal = case233_line916(yyVal, yyVals, yyTop); // line 916
break;
case 234: yyVal = case234_line920(yyVal, yyVals, yyTop); // line 920
break;
case 235: yyVal = case235_line924(yyVal, yyVals, yyTop); // line 924
break;
case 236: yyVal = case236_line928(yyVal, yyVals, yyTop); // line 928
break;
case 237: yyVal = case237_line932(yyVal, yyVals, yyTop); // line 932
break;
case 238: yyVal = case238_line937(yyVal, yyVals, yyTop); // line 937
break;
case 239: yyVal = case239_line940(yyVal, yyVals, yyTop); // line 940
break;
case 240: yyVal = case240_line943(yyVal, yyVals, yyTop); // line 943
break;
case 241: yyVal = case241_line946(yyVal, yyVals, yyTop); // line 946
break;
case 242: yyVal = case242_line949(yyVal, yyVals, yyTop); // line 949
break;
case 243: yyVal = case243_line953(yyVal, yyVals, yyTop); // line 953
break;
case 244: yyVal = case244_line957(yyVal, yyVals, yyTop); // line 957
break;
case 245: yyVal = case245_line961(yyVal, yyVals, yyTop); // line 961
break;
case 246: yyVal = case246_line965(yyVal, yyVals, yyTop); // line 965
break;
case 247: yyVal = case247_line969(yyVal, yyVals, yyTop); // line 969
break;
case 248: yyVal = case248_line973(yyVal, yyVals, yyTop); // line 973
break;
case 249: yyVal = case249_line977(yyVal, yyVals, yyTop); // line 977
break;
case 250: yyVal = case250_line981(yyVal, yyVals, yyTop); // line 981
break;
case 251: yyVal = case251_line984(yyVal, yyVals, yyTop); // line 984
break;
case 252: yyVal = case252_line987(yyVal, yyVals, yyTop); // line 987
break;
case 253: yyVal = case253_line989(yyVal, yyVals, yyTop); // line 989
break;
case 255: yyVal = case255_line995(yyVal, yyVals, yyTop); // line 995
break;
case 256: yyVal = case256_line997(yyVal, yyVals, yyTop); // line 997
break;
case 257: yyVal = case257_line1001(yyVal, yyVals, yyTop); // line 1001
break;
case 258: yyVal = case258_line1003(yyVal, yyVals, yyTop); // line 1003
break;
case 259: yyVal = case259_line1008(yyVal, yyVals, yyTop); // line 1008
break;
case 260: yyVal = case260_line1013(yyVal, yyVals, yyTop); // line 1013
break;
case 262: yyVal = case262_line1018(yyVal, yyVals, yyTop); // line 1018
break;
case 263: yyVal = case263_line1021(yyVal, yyVals, yyTop); // line 1021
break;
case 264: yyVal = case264_line1025(yyVal, yyVals, yyTop); // line 1025
break;
case 265: yyVal = case265_line1028(yyVal, yyVals, yyTop); // line 1028
break;
case 266: yyVal = case266_line1031(yyVal, yyVals, yyTop); // line 1031
break;
case 275: yyVal = case275_line1043(yyVal, yyVals, yyTop); // line 1043
break;
case 276: yyVal = case276_line1046(yyVal, yyVals, yyTop); // line 1046
break;
case 277: yyVal = case277_line1049(yyVal, yyVals, yyTop); // line 1049
break;
case 278: yyVal = case278_line1051(yyVal, yyVals, yyTop); // line 1051
break;
case 279: yyVal = case279_line1055(yyVal, yyVals, yyTop); // line 1055
break;
case 280: yyVal = case280_line1064(yyVal, yyVals, yyTop); // line 1064
break;
case 281: yyVal = case281_line1067(yyVal, yyVals, yyTop); // line 1067
break;
case 282: yyVal = case282_line1070(yyVal, yyVals, yyTop); // line 1070
break;
case 283: yyVal = case283_line1077(yyVal, yyVals, yyTop); // line 1077
break;
case 284: yyVal = case284_line1086(yyVal, yyVals, yyTop); // line 1086
break;
case 285: yyVal = case285_line1089(yyVal, yyVals, yyTop); // line 1089
break;
case 286: yyVal = case286_line1092(yyVal, yyVals, yyTop); // line 1092
break;
case 287: yyVal = case287_line1095(yyVal, yyVals, yyTop); // line 1095
break;
case 288: yyVal = case288_line1098(yyVal, yyVals, yyTop); // line 1098
break;
case 289: yyVal = case289_line1101(yyVal, yyVals, yyTop); // line 1101
break;
case 290: yyVal = case290_line1104(yyVal, yyVals, yyTop); // line 1104
break;
case 292: yyVal = case292_line1108(yyVal, yyVals, yyTop); // line 1108
break;
case 293: yyVal = case293_line1116(yyVal, yyVals, yyTop); // line 1116
break;
case 294: yyVal = case294_line1119(yyVal, yyVals, yyTop); // line 1119
break;
case 295: yyVal = case295_line1122(yyVal, yyVals, yyTop); // line 1122
break;
case 296: yyVal = case296_line1124(yyVal, yyVals, yyTop); // line 1124
break;
case 297: yyVal = case297_line1126(yyVal, yyVals, yyTop); // line 1126
break;
case 298: yyVal = case298_line1130(yyVal, yyVals, yyTop); // line 1130
break;
case 299: yyVal = case299_line1132(yyVal, yyVals, yyTop); // line 1132
break;
case 300: yyVal = case300_line1134(yyVal, yyVals, yyTop); // line 1134
break;
case 301: yyVal = case301_line1138(yyVal, yyVals, yyTop); // line 1138
break;
case 302: yyVal = case302_line1141(yyVal, yyVals, yyTop); // line 1141
break;
case 303: yyVal = case303_line1149(yyVal, yyVals, yyTop); // line 1149
break;
case 304: yyVal = case304_line1152(yyVal, yyVals, yyTop); // line 1152
break;
case 305: yyVal = case305_line1154(yyVal, yyVals, yyTop); // line 1154
break;
case 306: yyVal = case306_line1156(yyVal, yyVals, yyTop); // line 1156
break;
case 307: yyVal = case307_line1159(yyVal, yyVals, yyTop); // line 1159
break;
case 308: yyVal = case308_line1164(yyVal, yyVals, yyTop); // line 1164
break;
case 309: yyVal = case309_line1170(yyVal, yyVals, yyTop); // line 1170
break;
case 310: yyVal = case310_line1173(yyVal, yyVals, yyTop); // line 1173
break;
case 311: yyVal = case311_line1177(yyVal, yyVals, yyTop); // line 1177
break;
case 312: yyVal = case312_line1183(yyVal, yyVals, yyTop); // line 1183
break;
case 313: yyVal = case313_line1188(yyVal, yyVals, yyTop); // line 1188
break;
case 314: yyVal = case314_line1194(yyVal, yyVals, yyTop); // line 1194
break;
case 315: yyVal = case315_line1197(yyVal, yyVals, yyTop); // line 1197
break;
case 316: yyVal = case316_line1206(yyVal, yyVals, yyTop); // line 1206
break;
case 317: yyVal = case317_line1208(yyVal, yyVals, yyTop); // line 1208
break;
case 318: yyVal = case318_line1212(yyVal, yyVals, yyTop); // line 1212
break;
case 319: yyVal = case319_line1220(yyVal, yyVals, yyTop); // line 1220
break;
case 320: yyVal = case320_line1223(yyVal, yyVals, yyTop); // line 1223
break;
case 321: yyVal = case321_line1226(yyVal, yyVals, yyTop); // line 1226
break;
case 322: yyVal = case322_line1229(yyVal, yyVals, yyTop); // line 1229
break;
case 323: yyVal = case323_line1233(yyVal, yyVals, yyTop); // line 1233
break;
case 332: yyVal = case332_line1248(yyVal, yyVals, yyTop); // line 1248
break;
case 334: yyVal = case334_line1253(yyVal, yyVals, yyTop); // line 1253
break;
case 336: yyVal = case336_line1258(yyVal, yyVals, yyTop); // line 1258
break;
case 337: yyVal = case337_line1261(yyVal, yyVals, yyTop); // line 1261
break;
case 338: yyVal = case338_line1264(yyVal, yyVals, yyTop); // line 1264
break;
case 339: yyVal = case339_line1268(yyVal, yyVals, yyTop); // line 1268
break;
case 340: yyVal = case340_line1275(yyVal, yyVals, yyTop); // line 1275
break;
case 341: yyVal = case341_line1278(yyVal, yyVals, yyTop); // line 1278
break;
case 342: yyVal = case342_line1281(yyVal, yyVals, yyTop); // line 1281
break;
case 343: yyVal = case343_line1284(yyVal, yyVals, yyTop); // line 1284
break;
case 344: yyVal = case344_line1287(yyVal, yyVals, yyTop); // line 1287
break;
case 345: yyVal = case345_line1290(yyVal, yyVals, yyTop); // line 1290
break;
case 346: yyVal = case346_line1293(yyVal, yyVals, yyTop); // line 1293
break;
case 347: yyVal = case347_line1296(yyVal, yyVals, yyTop); // line 1296
break;
case 348: yyVal = case348_line1299(yyVal, yyVals, yyTop); // line 1299
break;
case 349: yyVal = case349_line1302(yyVal, yyVals, yyTop); // line 1302
break;
case 350: yyVal = case350_line1305(yyVal, yyVals, yyTop); // line 1305
break;
case 352: yyVal = case352_line1310(yyVal, yyVals, yyTop); // line 1310
break;
case 353: yyVal = case353_line1314(yyVal, yyVals, yyTop); // line 1314
break;
case 354: yyVal = case354_line1318(yyVal, yyVals, yyTop); // line 1318
break;
case 355: yyVal = case355_line1328(yyVal, yyVals, yyTop); // line 1328
break;
case 356: yyVal = case356_line1330(yyVal, yyVals, yyTop); // line 1330
break;
case 357: yyVal = case357_line1336(yyVal, yyVals, yyTop); // line 1336
break;
case 358: yyVal = case358_line1347(yyVal, yyVals, yyTop); // line 1347
break;
case 359: yyVal = case359_line1350(yyVal, yyVals, yyTop); // line 1350
break;
case 360: yyVal = case360_line1354(yyVal, yyVals, yyTop); // line 1354
break;
case 361: yyVal = case361_line1357(yyVal, yyVals, yyTop); // line 1357
break;
case 362: yyVal = case362_line1360(yyVal, yyVals, yyTop); // line 1360
break;
case 363: yyVal = case363_line1363(yyVal, yyVals, yyTop); // line 1363
break;
case 364: yyVal = case364_line1366(yyVal, yyVals, yyTop); // line 1366
break;
case 365: yyVal = case365_line1369(yyVal, yyVals, yyTop); // line 1369
break;
case 366: yyVal = case366_line1374(yyVal, yyVals, yyTop); // line 1374
break;
case 367: yyVal = case367_line1376(yyVal, yyVals, yyTop); // line 1376
break;
case 368: yyVal = case368_line1380(yyVal, yyVals, yyTop); // line 1380
break;
case 369: yyVal = case369_line1382(yyVal, yyVals, yyTop); // line 1382
break;
case 370: yyVal = case370_line1387(yyVal, yyVals, yyTop); // line 1387
break;
case 372: yyVal = case372_line1392(yyVal, yyVals, yyTop); // line 1392
break;
case 373: yyVal = case373_line1395(yyVal, yyVals, yyTop); // line 1395
break;
case 376: yyVal = case376_line1402(yyVal, yyVals, yyTop); // line 1402
break;
case 377: yyVal = case377_line1415(yyVal, yyVals, yyTop); // line 1415
break;
case 378: yyVal = case378_line1419(yyVal, yyVals, yyTop); // line 1419
break;
case 381: yyVal = case381_line1425(yyVal, yyVals, yyTop); // line 1425
break;
case 383: yyVal = case383_line1430(yyVal, yyVals, yyTop); // line 1430
break;
case 386: yyVal = case386_line1441(yyVal, yyVals, yyTop); // line 1441
break;
case 388: yyVal = case388_line1448(yyVal, yyVals, yyTop); // line 1448
break;
case 390: yyVal = case390_line1454(yyVal, yyVals, yyTop); // line 1454
break;
case 391: yyVal = case391_line1459(yyVal, yyVals, yyTop); // line 1459
break;
case 392: yyVal = case392_line1465(yyVal, yyVals, yyTop); // line 1465
break;
case 393: yyVal = case393_line1482(yyVal, yyVals, yyTop); // line 1482
break;
case 394: yyVal = case394_line1498(yyVal, yyVals, yyTop); // line 1498
break;
case 395: yyVal = case395_line1501(yyVal, yyVals, yyTop); // line 1501
break;
case 396: yyVal = case396_line1507(yyVal, yyVals, yyTop); // line 1507
break;
case 397: yyVal = case397_line1510(yyVal, yyVals, yyTop); // line 1510
break;
case 399: yyVal = case399_line1515(yyVal, yyVals, yyTop); // line 1515
break;
case 400: yyVal = case400_line1520(yyVal, yyVals, yyTop); // line 1520
break;
case 401: yyVal = case401_line1523(yyVal, yyVals, yyTop); // line 1523
break;
case 402: yyVal = case402_line1529(yyVal, yyVals, yyTop); // line 1529
break;
case 403: yyVal = case403_line1532(yyVal, yyVals, yyTop); // line 1532
break;
case 404: yyVal = case404_line1537(yyVal, yyVals, yyTop); // line 1537
break;
case 405: yyVal = case405_line1540(yyVal, yyVals, yyTop); // line 1540
break;
case 406: yyVal = case406_line1544(yyVal, yyVals, yyTop); // line 1544
break;
case 407: yyVal = case407_line1547(yyVal, yyVals, yyTop); // line 1547
break;
case 408: yyVal = case408_line1552(yyVal, yyVals, yyTop); // line 1552
break;
case 409: yyVal = case409_line1555(yyVal, yyVals, yyTop); // line 1555
break;
case 410: yyVal = case410_line1559(yyVal, yyVals, yyTop); // line 1559
break;
case 411: yyVal = case411_line1563(yyVal, yyVals, yyTop); // line 1563
break;
case 412: yyVal = case412_line1569(yyVal, yyVals, yyTop); // line 1569
break;
case 413: yyVal = case413_line1577(yyVal, yyVals, yyTop); // line 1577
break;
case 414: yyVal = case414_line1580(yyVal, yyVals, yyTop); // line 1580
break;
case 415: yyVal = case415_line1583(yyVal, yyVals, yyTop); // line 1583
break;
case 417: yyVal = case417_line1590(yyVal, yyVals, yyTop); // line 1590
break;
case 422: yyVal = case422_line1600(yyVal, yyVals, yyTop); // line 1600
break;
case 424: yyVal = case424_line1617(yyVal, yyVals, yyTop); // line 1617
break;
case 425: yyVal = case425_line1620(yyVal, yyVals, yyTop); // line 1620
break;
case 426: yyVal = case426_line1623(yyVal, yyVals, yyTop); // line 1623
break;
case 432: yyVal = case432_line1629(yyVal, yyVals, yyTop); // line 1629
break;
case 433: yyVal = case433_line1632(yyVal, yyVals, yyTop); // line 1632
break;
case 434: yyVal = case434_line1635(yyVal, yyVals, yyTop); // line 1635
break;
case 435: yyVal = case435_line1638(yyVal, yyVals, yyTop); // line 1638
break;
case 436: yyVal = case436_line1641(yyVal, yyVals, yyTop); // line 1641
break;
case 437: yyVal = case437_line1644(yyVal, yyVals, yyTop); // line 1644
break;
case 438: yyVal = case438_line1649(yyVal, yyVals, yyTop); // line 1649
break;
case 439: yyVal = case439_line1654(yyVal, yyVals, yyTop); // line 1654
break;
case 442: yyVal = case442_line1662(yyVal, yyVals, yyTop); // line 1662
break;
case 443: yyVal = case443_line1665(yyVal, yyVals, yyTop); // line 1665
break;
case 444: yyVal = case444_line1667(yyVal, yyVals, yyTop); // line 1667
break;
case 445: yyVal = case445_line1670(yyVal, yyVals, yyTop); // line 1670
break;
case 446: yyVal = case446_line1676(yyVal, yyVals, yyTop); // line 1676
break;
case 447: yyVal = case447_line1682(yyVal, yyVals, yyTop); // line 1682
break;
case 448: yyVal = case448_line1687(yyVal, yyVals, yyTop); // line 1687
break;
case 449: yyVal = case449_line1690(yyVal, yyVals, yyTop); // line 1690
break;
case 450: yyVal = case450_line1693(yyVal, yyVals, yyTop); // line 1693
break;
case 451: yyVal = case451_line1696(yyVal, yyVals, yyTop); // line 1696
break;
case 452: yyVal = case452_line1699(yyVal, yyVals, yyTop); // line 1699
break;
case 453: yyVal = case453_line1702(yyVal, yyVals, yyTop); // line 1702
break;
case 454: yyVal = case454_line1705(yyVal, yyVals, yyTop); // line 1705
break;
case 455: yyVal = case455_line1708(yyVal, yyVals, yyTop); // line 1708
break;
case 456: yyVal = case456_line1711(yyVal, yyVals, yyTop); // line 1711
break;
case 457: yyVal = case457_line1716(yyVal, yyVals, yyTop); // line 1716
break;
case 458: yyVal = case458_line1719(yyVal, yyVals, yyTop); // line 1719
break;
case 459: yyVal = case459_line1722(yyVal, yyVals, yyTop); // line 1722
break;
case 460: yyVal = case460_line1725(yyVal, yyVals, yyTop); // line 1725
break;
case 461: yyVal = case461_line1728(yyVal, yyVals, yyTop); // line 1728
break;
case 462: yyVal = case462_line1739(yyVal, yyVals, yyTop); // line 1739
break;
case 463: yyVal = case463_line1744(yyVal, yyVals, yyTop); // line 1744
break;
case 464: yyVal = case464_line1748(yyVal, yyVals, yyTop); // line 1748
break;
case 465: yyVal = case465_line1754(yyVal, yyVals, yyTop); // line 1754
break;
case 466: yyVal = case466_line1761(yyVal, yyVals, yyTop); // line 1761
break;
case 467: yyVal = case467_line1772(yyVal, yyVals, yyTop); // line 1772
break;
case 468: yyVal = case468_line1775(yyVal, yyVals, yyTop); // line 1775
break;
case 471: yyVal = case471_line1783(yyVal, yyVals, yyTop); // line 1783
break;
case 472: yyVal = case472_line1792(yyVal, yyVals, yyTop); // line 1792
break;
case 475: yyVal = case475_line1800(yyVal, yyVals, yyTop); // line 1800
break;
case 476: yyVal = case476_line1804(yyVal, yyVals, yyTop); // line 1804
break;
case 477: yyVal = case477_line1807(yyVal, yyVals, yyTop); // line 1807
break;
case 478: yyVal = case478_line1811(yyVal, yyVals, yyTop); // line 1811
break;
case 479: yyVal = case479_line1817(yyVal, yyVals, yyTop); // line 1817
break;
case 480: yyVal = case480_line1819(yyVal, yyVals, yyTop); // line 1819
break;
case 481: yyVal = case481_line1831(yyVal, yyVals, yyTop); // line 1831
break;
case 482: yyVal = case482_line1834(yyVal, yyVals, yyTop); // line 1834
break;
case 483: yyVal = case483_line1837(yyVal, yyVals, yyTop); // line 1837
break;
case 485: yyVal = case485_line1846(yyVal, yyVals, yyTop); // line 1846
break;
case 486: yyVal = case486_line1851(yyVal, yyVals, yyTop); // line 1851
break;
case 506: yyVal = case506_line1870(yyVal, yyVals, yyTop); // line 1870
break;
case 509: yyVal = case509_line1876(yyVal, yyVals, yyTop); // line 1876
break;
case 510: yyVal = case510_line1880(yyVal, yyVals, yyTop); // line 1880
break;
case 511: yyVal = case511_line1884(yyVal, yyVals, yyTop); // line 1884
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

public Object case345_line1290(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new MultipleAsgnNode(getPosition(((ListNode)yyVals[-2+yyTop])), ((ListNode)yyVals[-2+yyTop]), new StarNode(getPosition()));
    return yyVal;
}
public Object case101_line672(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new LiteralNode(((Token)yyVals[0+yyTop]));
    return yyVal;
}
public Object case306_line1156(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new ForNode(getPosition(((Token)yyVals[-8+yyTop])), ((Node)yyVals[-7+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-4+yyTop]));
    return yyVal;
}
public Object case258_line1003(Object yyVal, Object[] yyVals, int yyTop) {
                  warnings.warn(ID.ARGUMENT_EXTRA_SPACE, getPosition(((Token)yyVals[-3+yyTop])), "don't put space before argument parentheses");
		  yyVal = ((Node)yyVals[-2+yyTop]);
    return yyVal;
}
public Object case286_line1092(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_yield(getPosition(((Token)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case443_line1665(Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.setState(LexState.EXPR_BEG);
    return yyVal;
}
public Object case373_line1395(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new SplatNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case320_line1223(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new NextNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
    return yyVal;
}
public Object case250_line981(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_blk_pass(support.newSplatNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case195_line799(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "-@");
    return yyVal;
}
public Object case61_line522(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((MultipleAsgnNode)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case332_line1248(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new IfNode(getPosition(((Token)yyVals[-4+yyTop])), support.getConditionNode(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case196_line802(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "|", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case27_line414(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public Object case235_line924(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), support.newArrayNode(getPosition(((ListNode)yyVals[-4+yyTop])), new HashNode(getPosition(), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case262_line1018(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newArrayNode(getPosition2(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case439_line1654(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
    return yyVal;
}
public Object case405_line1540(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case283_line1077(Object yyVal, Object[] yyVals, int yyTop) {
                  ISourcePosition position = getPosition(((Token)yyVals[-2+yyTop]));
                  if (((Node)yyVals[-1+yyTop]) == null) {
                      yyVal = new ZArrayNode(position); /* zero length array */
                  } else {
                      yyVal = ((Node)yyVals[-1+yyTop]);
                      ((ISourcePositionHolder)yyVal).setPosition(position);
                  }
    return yyVal;
}
public Object case221_line878(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((ListNode)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case449_line1690(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.new_args(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case364_line1366(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case215_line859(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new DefinedNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case476_line1804(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((BlockArgNode)yyVals[0+yyTop]);
    return yyVal;
}
public Object case228_line903(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((ListNode)yyVals[-4+yyTop]).add(((Node)yyVals[-2+yyTop]));
    return yyVal;
}
public Object case92_line646(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_colon3(getPosition(((Token)yyVals[-1+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case33_line437(Object yyVal, Object[] yyVals, int yyTop) {
                  ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
		  yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
                  ((MultipleAsgnNode)yyVals[-2+yyTop]).setPosition(getPosition(((MultipleAsgnNode)yyVals[-2+yyTop])));
    return yyVal;
}
public Object case214_line856(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newOrNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case80_line590(Object yyVal, Object[] yyVals, int yyTop) {
                  if (support.isInDef() || support.isInSingle()) {
		      yyerror("dynamic constant assignment");
		  }

                  ISourcePosition position = getPosition(((Token)yyVals[-1+yyTop]));

                  yyVal = new ConstDeclNode(position, null, support.new_colon3(position, (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
    return yyVal;
}
public Object case205_line829(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "===", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case216_line862(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new IfNode(getPosition(((Node)yyVals[-4+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case475_line1800(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newBlockArg(((Token)yyVals[-1+yyTop]).getPosition(), ((Token)yyVals[0+yyTop]));
    return yyVal;
}
public Object case438_line1649(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.gettable(((Token)yyVals[0+yyTop]));
    return yyVal;
}
public Object case369_line1382(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_iter(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  support.popCurrentScope();
    return yyVal;
}
public Object case323_line1233(Object yyVal, Object[] yyVals, int yyTop) {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
		  yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case208_line838(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new NotNode(getPosition(((Node)yyVals[-2+yyTop])), support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
    return yyVal;
}
public Object case244_line957(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newArrayNode(getPosition(((ListNode)yyVals[-1+yyTop])), new HashNode(getPosition(), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case437_line1644(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new Token("__LINE__", Tokens.k__LINE__, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case318_line1212(Object yyVal, Object[] yyVals, int yyTop) {
                  /* TODO: We should use implicit nil for body, but problem (punt til later)*/
                  Node body = ((Node)yyVals[-1+yyTop]); /*$8 == null ? NilImplicitNode.NIL : $8;*/

                  yyVal = new DefsNode(getPosition(((Token)yyVals[-8+yyTop])), ((Node)yyVals[-7+yyTop]), new ArgumentNode(((Token)yyVals[-4+yyTop]).getPosition(), (String) ((Token)yyVals[-4+yyTop]).getValue()), ((ArgsNode)yyVals[-2+yyTop]), support.getCurrentScope(), body);
                  support.popCurrentScope();
                  support.setInSingle(support.getInSingle() - 1);
    return yyVal;
}
public Object case404_line1537(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new StrNode(((Token)yyVals[0+yyTop]).getPosition(), ByteList.create(""));
    return yyVal;
}
public Object case343_line1284(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newBlockArg18(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]), new MultipleAsgnNode(getPosition(((ListNode)yyVals[-5+yyTop])), ((ListNode)yyVals[-5+yyTop]), new StarNode(getPosition())));
    return yyVal;
}
public Object case225_line893(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new ArrayNode(getPosition(((Token)yyVals[-2+yyTop])));
    return yyVal;
}
public Object case59_line516(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_yield(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case177_line734(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_opElementAsgnNode(getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case209_line841(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new NotNode(getPosition(((Token)yyVals[-1+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
    return yyVal;
}
public Object case242_line949(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-4+yyTop])), support.newArrayNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case239_line940(Object yyVal, Object[] yyVals, int yyTop) {
    return yyVal;
}
public Object case482_line1834(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((ListNode)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case403_line1532(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case402_line1529(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new ArrayNode(getPosition());
    return yyVal;
}
public Object case24_line385(Object yyVal, Object[] yyVals, int yyTop) {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
		  if (((MultipleAsgnNode)yyVals[-2+yyTop]).getHeadNode() != null) {
		      ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(new ToAryNode(getPosition(((MultipleAsgnNode)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
		  } else {
		      ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(support.newArrayNode(getPosition(((MultipleAsgnNode)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
		  }
		  yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
    return yyVal;
}
public Object case85_line613(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case246_line965(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newArrayNode(getPosition(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop])).add(new HashNode(getPosition(), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case245_line961(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), support.newArrayNode(getPosition(((ListNode)yyVals[-4+yyTop])), new HashNode(getPosition(), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case454_line1705(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.new_args(getPosition(((RestArgNode)yyVals[-1+yyTop])), null, null, ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case415_line1583(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new ClassVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case338_line1264(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case288_line1098(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new ZYieldNode(getPosition(((Token)yyVals[0+yyTop])));
    return yyVal;
}
public Object case81_line599(Object yyVal, Object[] yyVals, int yyTop) {
	          support.backrefAssignError(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case302_line1141(Object yyVal, Object[] yyVals, int yyTop) {
/* TODO: MRI is just a when node.  We need this extra logic for IDE consumers (null in casenode statement should be implicit nil)*/
/*                  if (support.getConfiguration().hasExtraPositionInformation()) {*/
                      yyVal = support.newCaseNode(getPosition(((Token)yyVals[-3+yyTop])), null, ((Node)yyVals[-1+yyTop]));
/*                  } else {*/
/*                      $$ = $3;*/
/*                  }*/
    return yyVal;
}
public Object case201_line817(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">=", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case353_line1314(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new ZeroArgNode(getPosition(((Token)yyVals[0+yyTop])));
                  lexer.commandStart = true;
    return yyVal;
}
public Object case200_line814(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case255_line995(Object yyVal, Object[] yyVals, int yyTop) {
		  lexer.setState(LexState.EXPR_ENDARG);
    return yyVal;
}
public Object case2_line277(Object yyVal, Object[] yyVals, int yyTop) {
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
public Object case1_line274(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.setState(LexState.EXPR_BEG);
                  support.initTopLocalVariables();
    return yyVal;
}
public Object case184_line755(Object yyVal, Object[] yyVals, int yyTop) {
		  support.checkExpression(((Node)yyVals[-2+yyTop]));
		  support.checkExpression(((Node)yyVals[0+yyTop]));
    
                  boolean isLiteral = ((Node)yyVals[-2+yyTop]) instanceof FixnumNode && ((Node)yyVals[0+yyTop]) instanceof FixnumNode;
                  yyVal = new DotNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), false, isLiteral);
    return yyVal;
}
public Object case46_line473(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new NextNode(getPosition(((Token)yyVals[-1+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
    return yyVal;
}
public Object case76_line572(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case252_line987(Object yyVal, Object[] yyVals, int yyTop) {
	          yyVal = Long.valueOf(lexer.getCmdArgumentState().begin());
    return yyVal;
}
public Object case193_line789(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((FloatNode)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), getPosition()), "-@");
    return yyVal;
}
public Object case391_line1459(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((Node)yyVals[-1+yyTop]);
                  ((ISourcePositionHolder)yyVal).setPosition(getPosition(((Token)yyVals[-2+yyTop])));
    return yyVal;
}
public Object case319_line1220(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new BreakNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
    return yyVal;
}
public Object case303_line1149(Object yyVal, Object[] yyVals, int yyTop) {
		  yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case106_line689(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.appendToBlock(((Node)yyVals[-3+yyTop]), support.newUndef(getPosition(((Node)yyVals[-3+yyTop])), ((Node)yyVals[0+yyTop])));
    return yyVal;
}
public Object case12_line332(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new VAliasNode(getPosition(((Token)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), "$" + ((BackRefNode)yyVals[0+yyTop]).getType()); /* XXX*/
    return yyVal;
}
public Object case304_line1152(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.getConditionState().begin();
    return yyVal;
}
public Object case202_line820(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case194_line792(Object yyVal, Object[] yyVals, int yyTop) {
                  if (support.isLiteral(((Node)yyVals[0+yyTop]))) {
		      yyVal = ((Node)yyVals[0+yyTop]);
		  } else {
                      yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "+@");
		  }
    return yyVal;
}
public Object case453_line1702(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.new_args(getPosition(((ListNode)yyVals[-1+yyTop])), null, ((ListNode)yyVals[-1+yyTop]), null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case352_line1310(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new ZeroArgNode(getPosition(((Token)yyVals[-1+yyTop])));
                  lexer.commandStart = true;
    return yyVal;
}
public Object case336_line1258(Object yyVal, Object[] yyVals, int yyTop) {
    return yyVal;
}
public Object case176_line718(Object yyVal, Object[] yyVals, int yyTop) {
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
public Object case72_line559(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newArrayNode(((Node)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case412_line1569(Object yyVal, Object[] yyVals, int yyTop) {
		   lexer.setStrTerm(((StrTerm)yyVals[-2+yyTop]));
                   lexer.getConditionState().restart();
	           lexer.getCmdArgumentState().restart();

		   yyVal = support.newEvStrNode(getPosition(((Token)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case407_line1547(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case393_line1482(Object yyVal, Object[] yyVals, int yyTop) {
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
public Object case362_line1360(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case31_line426(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), support.newSValueNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
    return yyVal;
}
public Object case299_line1132(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.getConditionState().end();
    return yyVal;
}
public Object case294_line1119(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new IfNode(getPosition(((Token)yyVals[-5+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-2+yyTop]));
    return yyVal;
}
public Object case465_line1754(Object yyVal, Object[] yyVals, int yyTop) {
                   ((ListNode)yyVals[-2+yyTop]).add(new ArgumentNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue()));
                   ((ListNode)yyVals[-2+yyTop]).setPosition(getPosition(((ListNode)yyVals[-2+yyTop])));
		   yyVal = ((ListNode)yyVals[-2+yyTop]);
    return yyVal;
}
public Object case188_line774(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "*", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case187_line771(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "-", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case88_line628(Object yyVal, Object[] yyVals, int yyTop) {
                  if (support.isInDef() || support.isInSingle()) {
		      yyerror("dynamic constant assignment");
		  }

                  ISourcePosition position = getPosition(((Token)yyVals[-1+yyTop]));

                  yyVal = new ConstDeclNode(position, null, support.new_colon3(position, (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
    return yyVal;
}
public Object case301_line1138(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newCaseNode(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case510_line1880(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = null;
    return yyVal;
}
public Object case457_line1716(Object yyVal, Object[] yyVals, int yyTop) {
                   yyerror("formal argument cannot be a constant");
    return yyVal;
}
public Object case400_line1520(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new ZArrayNode(getPosition(((Token)yyVals[-2+yyTop])));
    return yyVal;
}
public Object case360_line1354(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_fcall(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case198_line808(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "&", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case297_line1126(Object yyVal, Object[] yyVals, int yyTop) {
                  Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);
                  yyVal = new WhileNode(getPosition(((Token)yyVals[-6+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), body);
    return yyVal;
}
public Object case190_line780(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "%", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case39_line455(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new NotNode(getPosition(((Token)yyVals[-1+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
    return yyVal;
}
public Object case26_line410(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_opElementAsgnNode(getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));

    return yyVal;
}
public Object case468_line1775(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case462_line1739(Object yyVal, Object[] yyVals, int yyTop) {
                    support.allowDubyExtension(((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition());
                    yyVal = new ListNode(((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition());
                    ((ListNode) yyVal).add(new TypedArgumentNode(((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition(), (String) ((Token)yyVals[-2+yyTop]).getValue(), ((Node)yyVals[0+yyTop])));
    return yyVal;
}
public Object case434_line1635(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new Token("true", Tokens.kTRUE, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case349_line1302(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new MultipleAsgnNode(getPosition(((Token)yyVals[0+yyTop])), null, new StarNode(getPosition(((Token)yyVals[0+yyTop]))));
    return yyVal;
}
public Object case266_line1031(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newSplatNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case298_line1130(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.getConditionState().begin();
    return yyVal;
}
public Object case53_line498(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_fcall(((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])); 
    return yyVal;
}
public Object case466_line1761(Object yyVal, Object[] yyVals, int yyTop) {
                   String identifier = (String) ((Token)yyVals[-2+yyTop]).getValue();

                   if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                       yyerror("duplicate optional argument name");
                   }
		   support.getCurrentScope().getLocalScope().addVariable(identifier);
                   yyVal = support.assignable(((Token)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case348_line1299(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new MultipleAsgnNode(getPosition(((Node)yyVals[0+yyTop])), null, ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case41_line460(Object yyVal, Object[] yyVals, int yyTop) {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case226_line896(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((Node)yyVals[-2+yyTop]);
		  ((Node)yyVal).setPosition(getPosition(((Token)yyVals[-3+yyTop])));
    return yyVal;
}
public Object case426_line1623(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.negateFloat(((FloatNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case395_line1501(Object yyVal, Object[] yyVals, int yyTop) {
		   yyVal = ((ListNode)yyVals[-1+yyTop]);
                   ((ISourcePositionHolder)yyVal).setPosition(getPosition(((Token)yyVals[-2+yyTop])));
    return yyVal;
}
public Object case372_line1392(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case233_line916(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass(((Node)yyVal), ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case447_line1682(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case433_line1632(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new Token("self", Tokens.kSELF, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case394_line1498(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new ZArrayNode(getPosition(((Token)yyVals[-2+yyTop])));
    return yyVal;
}
public Object case213_line853(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newAndNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case222_line881(Object yyVal, Object[] yyVals, int yyTop) {
                  support.checkExpression(((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case7_line317(Object yyVal, Object[] yyVals, int yyTop) {
	          yyVal = support.appendToBlock(((Node)yyVals[-2+yyTop]), support.newline_node(((Node)yyVals[0+yyTop]), getPosition(((Node)yyVals[0+yyTop]))));
    return yyVal;
}
public Object case227_line900(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newArrayNode(getPosition(((Token)yyVals[-3+yyTop])), ((Node)yyVals[-2+yyTop]));
    return yyVal;
}
public Object case451_line1696(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.new_args(((ISourcePositionHolder)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case378_line1419(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case3_line289(Object yyVal, Object[] yyVals, int yyTop) {
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
public Object case243_line953(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-6+yyTop])), support.newArrayNode(getPosition(((Node)yyVals[-6+yyTop])), ((Node)yyVals[-6+yyTop])).addAll(new HashNode(getPosition(), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case212_line850(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">>", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case281_line1067(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_colon3(getPosition(((Token)yyVals[-1+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case234_line920(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newArrayNode(getPosition(((ListNode)yyVals[-1+yyTop])), new HashNode(getPosition(), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case207_line835(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case446_line1676(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = ((Node)yyVals[-2+yyTop]);
                   ((ISourcePositionHolder)yyVal).setPosition(getPosition(((Token)yyVals[-3+yyTop])));
                   lexer.setState(LexState.EXPR_BEG);
                   lexer.commandStart = true;
    return yyVal;
}
public Object case445_line1670(Object yyVal, Object[] yyVals, int yyTop) {
                   yyerrok();
                   yyVal = null;
    return yyVal;
}
public Object case444_line1667(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case206_line832(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new NotNode(getPosition(((Node)yyVals[-2+yyTop])), support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop]), getPosition()));
    return yyVal;
}
public Object case89_line637(Object yyVal, Object[] yyVals, int yyTop) {
                   support.backrefAssignError(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case265_line1028(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case276_line1046(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new BeginNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case284_line1086(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new HashNode(getPosition(((Token)yyVals[-2+yyTop])), ((ListNode)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case16_line344(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new IfNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), null, ((Node)yyVals[-2+yyTop]));
    return yyVal;
}
public Object case174_line708(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
		  /* FIXME: Consider fixing node_assign itself rather than single case*/
		  ((Node)yyVal).setPosition(getPosition(((Node)yyVals[-2+yyTop])));
    return yyVal;
}
public Object case334_line1253(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case321_line1226(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new RedoNode(((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case290_line1104(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new FCallNoArgBlockNode(getPosition(((Token)yyVals[-1+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((IterNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case220_line875(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newArrayNode(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case506_line1870(Object yyVal, Object[] yyVals, int yyTop) {
                  yyerrok();
    return yyVal;
}
public Object case367_line1376(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_iter(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  support.popCurrentScope();
    return yyVal;
}
public Object case357_line1336(Object yyVal, Object[] yyVals, int yyTop) {
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
public Object case22_line375(Object yyVal, Object[] yyVals, int yyTop) {
                  if (support.isInDef() || support.isInSingle()) {
                      warnings.warn(ID.END_IN_METHOD, getPosition(((Token)yyVals[-3+yyTop])), "END in method; use at_exit");
                  }
                  yyVal = new PostExeNode(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case182_line749(Object yyVal, Object[] yyVals, int yyTop) {
		  yyerror("constant re-assignment");
    return yyVal;
}
public Object case432_line1629(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new Token("nil", Tokens.kNIL, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case183_line752(Object yyVal, Object[] yyVals, int yyTop) {
                  support.backrefAssignError(((Node)yyVals[-2+yyTop]));
    return yyVal;
}
public Object case181_line746(Object yyVal, Object[] yyVals, int yyTop) {
	          yyerror("constant re-assignment");
    return yyVal;
}
public Object case185_line762(Object yyVal, Object[] yyVals, int yyTop) {
		  support.checkExpression(((Node)yyVals[-2+yyTop]));
		  support.checkExpression(((Node)yyVals[0+yyTop]));
                  boolean isLiteral = ((Node)yyVals[-2+yyTop]) instanceof FixnumNode && ((Node)yyVals[0+yyTop]) instanceof FixnumNode;
                  yyVal = new DotNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), true, isLiteral);
    return yyVal;
}
public Object case316_line1206(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.setState(LexState.EXPR_FNAME);
    return yyVal;
}
public Object case78_line578(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case264_line1025(Object yyVal, Object[] yyVals, int yyTop) {
		  yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case63_line528(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new MultipleAsgnNode(getPosition(((Token)yyVals[-2+yyTop])), support.newArrayNode(getPosition(((Token)yyVals[-2+yyTop])), ((MultipleAsgnNode)yyVals[-1+yyTop])), null);
    return yyVal;
}
public Object case383_line1430(Object yyVal, Object[] yyVals, int yyTop) {
                  if (((Node)yyVals[0+yyTop]) != null) {
                      yyVal = ((Node)yyVals[0+yyTop]);
                  } else {
                      yyVal = new NilNode(getPosition());
                  }
    return yyVal;
}
public Object case341_line1278(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newBlockArg18(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]), new MultipleAsgnNode(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), null));
    return yyVal;
}
public Object case71_line554(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((MultipleAsgnNode)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case14_line338(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case424_line1617(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = ((FloatNode)yyVals[0+yyTop]);
    return yyVal;
}
public Object case18_line354(Object yyVal, Object[] yyVals, int yyTop) {
                  if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                      yyVal = new UntilNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                  } else {
                      yyVal = new UntilNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                  }
    return yyVal;
}
public Object case486_line1851(Object yyVal, Object[] yyVals, int yyTop) {
                  ISourcePosition position;
                  if (((Node)yyVals[-2+yyTop]) == null && ((Node)yyVals[0+yyTop]) == null) {
                      position = getPosition(((Token)yyVals[-1+yyTop]));
                  } else {
                      position = getPosition(((Node)yyVals[-2+yyTop]));
                  }

                  yyVal = support.newArrayNode(position, ((Node)yyVals[-2+yyTop])).add(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case478_line1811(Object yyVal, Object[] yyVals, int yyTop) {
                  if (!(((Node)yyVals[0+yyTop]) instanceof SelfNode)) {
		      support.checkExpression(((Node)yyVals[0+yyTop]));
		  }
		  yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case471_line1783(Object yyVal, Object[] yyVals, int yyTop) {
                  String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();

                  if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                      yyerror("duplicate rest argument name");
                  }

                  yyVal = new RestArgNode(getPosition(((Token)yyVals[-1+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue(), support.getCurrentScope().getLocalScope().addVariable(identifier));
    return yyVal;
}
public Object case460_line1725(Object yyVal, Object[] yyVals, int yyTop) {
                   yyerror("formal argument cannot be a class variable");
    return yyVal;
}
public Object case414_line1580(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new InstVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case413_line1577(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new GlobalVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case410_line1559(Object yyVal, Object[] yyVals, int yyTop) {
		   lexer.setStrTerm(((StrTerm)yyVals[-1+yyTop]));
	           yyVal = new EvStrNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case346_line1293(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newBlockArg18(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]), new MultipleAsgnNode(getPosition(((Node)yyVals[-3+yyTop])), null, ((Node)yyVals[-3+yyTop])));
    return yyVal;
}
public Object case51_line489(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = support.new_iter(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                    support.popCurrentScope();
    return yyVal;
}
public Object case295_line1122(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.getConditionState().begin();
    return yyVal;
}
public Object case83_line607(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.aryset(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case105_line687(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.setState(LexState.EXPR_FNAME);
    return yyVal;
}
public Object case199_line811(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=>", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case65_line536(Object yyVal, Object[] yyVals, int yyTop) {
/*mirko: check*/
                  yyVal = new MultipleAsgnNode(getPosition(((Node)yyVals[-1+yyTop])), ((ListNode)yyVals[-1+yyTop]).add(((Node)yyVals[0+yyTop])), null);
    return yyVal;
}
public Object case463_line1744(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ListNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
                    ((ListNode) yyVal).add(new ArgumentNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue()));
    return yyVal;
}
public Object case411_line1563(Object yyVal, Object[] yyVals, int yyTop) {
		   yyVal = lexer.getStrTerm();
		   lexer.setStrTerm(null);
		   lexer.setState(LexState.EXPR_BEG);
                   lexer.getConditionState().stop();
	           lexer.getCmdArgumentState().stop();
    return yyVal;
}
public Object case392_line1465(Object yyVal, Object[] yyVals, int yyTop) {
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
public Object case58_line513(Object yyVal, Object[] yyVals, int yyTop) {
		  yyVal = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop])); /* .setPosFrom($2);*/
    return yyVal;
}
public Object case104_line684(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newUndef(getPosition(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case64_line533(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new MultipleAsgnNode(getPosition(((ListNode)yyVals[0+yyTop])), ((ListNode)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case44_line467(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new ReturnNode(getPosition(((Token)yyVals[-1+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
    return yyVal;
}
public Object case74_line566(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
    return yyVal;
}
public Object case50_line487(Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
    return yyVal;
}
public Object case259_line1008(Object yyVal, Object[] yyVals, int yyTop) {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
                  yyVal = new BlockPassNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case37_line449(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newOrNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case344_line1287(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new MultipleAsgnNode(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case218_line869(Object yyVal, Object[] yyVals, int yyTop) {
	          support.checkExpression(((Node)yyVals[0+yyTop]));
	          yyVal = ((Node)yyVals[0+yyTop]);   
    return yyVal;
}
public Object case192_line786(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), getPosition()), "-@");
    return yyVal;
}
public Object case310_line1173(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = Integer.valueOf(support.getInSingle());
                  support.setInSingle(0);
		  support.pushLocalScope();
    return yyVal;
}
public Object case485_line1846(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((ListNode)yyVals[-2+yyTop]).addAll(((ListNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case483_line1837(Object yyVal, Object[] yyVals, int yyTop) {
                  if (((ListNode)yyVals[-1+yyTop]).size() % 2 != 0) {
                      yyerror("odd number list for Hash.");
                  }
                  yyVal = ((ListNode)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case406_line1544(Object yyVal, Object[] yyVals, int yyTop) {
		   yyVal = null;
    return yyVal;
}
public Object case312_line1183(Object yyVal, Object[] yyVals, int yyTop) {
                  if (support.isInDef() || support.isInSingle()) { 
                      yyerror("module definition in method body");
                  }
		  support.pushLocalScope();
    return yyVal;
}
public Object case197_line805(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "^", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case25_line394(Object yyVal, Object[] yyVals, int yyTop) {
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
public Object case28_line417(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public Object case282_line1070(Object yyVal, Object[] yyVals, int yyTop) {
                  if (((Node)yyVals[-3+yyTop]) instanceof SelfNode) {
                      yyVal = support.new_fcall(new Token("[]", getPosition(((Node)yyVals[-3+yyTop]))), ((Node)yyVals[-1+yyTop]), null);
                  } else {
                      yyVal = support.new_aref(((Node)yyVals[-3+yyTop]), new Token("[]", getPosition(((Node)yyVals[-3+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  }
    return yyVal;
}
public Object case38_line452(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new NotNode(getPosition(((Token)yyVals[-1+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
    return yyVal;
}
public Object case477_line1807(Object yyVal, Object[] yyVals, int yyTop) {
	          yyVal = null;
    return yyVal;
}
public Object case456_line1711(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.new_args(support.createEmptyArgsNodePosition(getPosition()), null, null, null, null, null);
    return yyVal;
}
public Object case455_line1708(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.new_args(getPosition(((BlockArgNode)yyVals[0+yyTop])), null, null, null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case390_line1454(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case386_line1441(Object yyVal, Object[] yyVals, int yyTop) {
                  /* FIXME: We may be intern'ing more than once.*/
                  yyVal = new SymbolNode(((Token)yyVals[0+yyTop]).getPosition(), ((String) ((Token)yyVals[0+yyTop]).getValue()).intern());
    return yyVal;
}
public Object case292_line1108(Object yyVal, Object[] yyVals, int yyTop) {
	          if (((Node)yyVals[-1+yyTop]) != null && 
                      ((BlockAcceptingNode)yyVals[-1+yyTop]).getIterNode() instanceof BlockPassNode) {
                      throw new SyntaxException(PID.BLOCK_ARG_AND_BLOCK_GIVEN, getPosition(((Node)yyVals[-1+yyTop])), lexer.getCurrentLine(), "Both block arg and actual block given.");
		  }
		  yyVal = ((BlockAcceptingNode)yyVals[-1+yyTop]).setIterNode(((IterNode)yyVals[0+yyTop]));
		  ((Node)yyVal).setPosition(getPosition(((Node)yyVals[-1+yyTop])));
    return yyVal;
}
public Object case472_line1792(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new UnnamedRestArgNode(((Token)yyVals[0+yyTop]).getPosition(), support.getCurrentScope().getLocalScope().addVariable("*"));
    return yyVal;
}
public Object case442_line1662(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = null;
    return yyVal;
}
public Object case339_line1268(Object yyVal, Object[] yyVals, int yyTop) {
                  if (((ListNode)yyVals[0+yyTop]).size() == 1) {
                      yyVal = ((ListNode)yyVals[0+yyTop]).get(0);
                  } else {
                      yyVal = new MultipleAsgnNode(getPosition(((ListNode)yyVals[0+yyTop])), ((ListNode)yyVals[0+yyTop]), null);
                  }
    return yyVal;
}
public Object case305_line1154(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.getConditionState().end();
    return yyVal;
}
public Object case232_line913(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_blk_pass(((ListNode)yyVals[-1+yyTop]), ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case30_line423(Object yyVal, Object[] yyVals, int yyTop) {
                  support.backrefAssignError(((Node)yyVals[-2+yyTop]));
    return yyVal;
}
public Object case289_line1101(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new DefinedNode(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case93_line649(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_colon2(getPosition(((Token)yyVals[0+yyTop])), null, (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case20_line365(Object yyVal, Object[] yyVals, int yyTop) {
                  if (support.isInDef() || support.isInSingle()) {
                      yyerror("BEGIN in method");
                  }
		  support.pushLocalScope();
    return yyVal;
}
public Object case467_line1772(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new BlockNode(getPosition(((Node)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case422_line1600(Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.setState(LexState.EXPR_END);

                   /* DStrNode: :"some text #{some expression}"*/
                   /* StrNode: :"some text"*/
                   /* EvStrNode :"#{some expression}"*/
                   if (((Node)yyVals[-1+yyTop]) == null) yyerror("empty symbol literal");

                   if (((Node)yyVals[-1+yyTop]) instanceof DStrNode) {
                       yyVal = new DSymbolNode(getPosition(((Token)yyVals[-2+yyTop])), ((DStrNode)yyVals[-1+yyTop]));
                   } else {
                       yyVal = new DSymbolNode(getPosition(((Token)yyVals[-2+yyTop])));
                       ((DSymbolNode)yyVal).add(((Node)yyVals[-1+yyTop]));
                   }
    return yyVal;
}
public Object case231_line910(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newArrayNode(getPosition(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case6_line314(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newline_node(((Node)yyVals[0+yyTop]), getPosition(((Node)yyVals[0+yyTop])));
    return yyVal;
}
public Object case363_line1363(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_call(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop]), null, null);
    return yyVal;
}
public Object case204_line826(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case293_line1116(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new IfNode(getPosition(((Token)yyVals[-5+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case425_line1620(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.negateInteger(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case9_line324(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.setState(LexState.EXPR_FNAME);
    return yyVal;
}
public Object case249_line977(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-8+yyTop])), support.newArrayNode(getPosition(((Node)yyVals[-8+yyTop])), ((Node)yyVals[-8+yyTop])).addAll(((ListNode)yyVals[-6+yyTop])).add(new HashNode(getPosition(), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case260_line1013(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((BlockPassNode)yyVals[0+yyTop]);
    return yyVal;
}
public Object case15_line341(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new IfNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), null);
    return yyVal;
}
public Object case21_line370(Object yyVal, Object[] yyVals, int yyTop) {
                  support.getResult().addBeginNode(new PreExeNode(getPosition(((Node)yyVals[-1+yyTop])), support.getCurrentScope(), ((Node)yyVals[-1+yyTop])));
                  support.popCurrentScope();
                  yyVal = null; /*XXX 0;*/
    return yyVal;
}
public Object case257_line1001(Object yyVal, Object[] yyVals, int yyTop) {
		  lexer.setState(LexState.EXPR_ENDARG);
    return yyVal;
}
public Object case211_line847(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<<", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case241_line946(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_blk_pass(support.newArrayNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case98_line658(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.setState(LexState.EXPR_END);
                  yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
}
public Object case23_line381(Object yyVal, Object[] yyVals, int yyTop) {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
                  yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case436_line1641(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new Token("__FILE__", Tokens.k__FILE__, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case368_line1380(Object yyVal, Object[] yyVals, int yyTop) {
                  support.pushBlockScope();
    return yyVal;
}
public Object case356_line1330(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_iter(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  support.popCurrentScope();
    return yyVal;
}
public Object case354_line1318(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((Node)yyVals[-1+yyTop]);
                  lexer.commandStart = true;

		  /* Include pipes on multiple arg type*/
                  if (((Node)yyVals[-1+yyTop]) instanceof MultipleAsgnNode) {
		      ((Node)yyVals[-1+yyTop]).setPosition(((Token)yyVals[-2+yyTop]).getPosition());
		  } 
    return yyVal;
}
public Object case84_line610(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case256_line997(Object yyVal, Object[] yyVals, int yyTop) {
                  warnings.warn(ID.ARGUMENT_EXTRA_SPACE, getPosition(((Token)yyVals[-2+yyTop])), "don't put space before argument parentheses");
	          yyVal = null;
    return yyVal;
}
public Object case180_line743(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public Object case280_line1064(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_colon2(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case69_line549(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new MultipleAsgnNode(getPosition(((Token)yyVals[0+yyTop])), null, new StarNode(getPosition()));
    return yyVal;
}
public Object case355_line1328(Object yyVal, Object[] yyVals, int yyTop) {
                  support.pushBlockScope();
    return yyVal;
}
public Object case342_line1281(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newBlockArg18(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]), new MultipleAsgnNode(getPosition(((ListNode)yyVals[-6+yyTop])), ((ListNode)yyVals[-6+yyTop]), ((Node)yyVals[-3+yyTop])));
    return yyVal;
}
public Object case275_line1043(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new FCallNoArgNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case11_line329(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new VAliasNode(getPosition(((Token)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case315_line1197(Object yyVal, Object[] yyVals, int yyTop) {
                  /* TODO: We should use implicit nil for body, but problem (punt til later)*/
                  Node body = ((Node)yyVals[-1+yyTop]); /*$5 == null ? NilImplicitNode.NIL : $5;*/

                  /* NOEX_PRIVATE for toplevel */
                  yyVal = new DefnNode(getPosition(((Token)yyVals[-5+yyTop])), new ArgumentNode(((Token)yyVals[-4+yyTop]).getPosition(), (String) ((Token)yyVals[-4+yyTop]).getValue()), ((ArgsNode)yyVals[-2+yyTop]), support.getCurrentScope(), body);
                  support.popCurrentScope();
                  support.setInDef(false);
    return yyVal;
}
public Object case481_line1831(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new ArrayNode(getPosition());
    return yyVal;
}
public Object case359_line1350(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case358_line1347(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case102_line677(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((LiteralNode)yyVals[0+yyTop]);
    return yyVal;
}
public Object case90_line641(Object yyVal, Object[] yyVals, int yyTop) {
                  yyerror("class/module name must be CONSTANT");
    return yyVal;
}
public Object case175_line713(Object yyVal, Object[] yyVals, int yyTop) {
                  ISourcePosition position = getPosition(((Token)yyVals[-1+yyTop]));
                  Node body = ((Node)yyVals[0+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop]);
                  yyVal = support.node_assign(((Node)yyVals[-4+yyTop]), new RescueNode(position, ((Node)yyVals[-2+yyTop]), new RescueBodyNode(position, null, body, null), null));
    return yyVal;
}
public Object case45_line470(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new BreakNode(getPosition(((Token)yyVals[-1+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
    return yyVal;
}
public Object case251_line984(Object yyVal, Object[] yyVals, int yyTop) {
    return yyVal;
}
public Object case337_line1261(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case307_line1159(Object yyVal, Object[] yyVals, int yyTop) {
                  if (support.isInDef() || support.isInSingle()) {
                      yyerror("class definition in method body");
                  }
		  support.pushLocalScope();
    return yyVal;
}
public Object case279_line1055(Object yyVal, Object[] yyVals, int yyTop) {
                  if (((Node)yyVals[-1+yyTop]) != null) {
                      /* compstmt position includes both parens around it*/
                      ((ISourcePositionHolder) ((Node)yyVals[-1+yyTop])).setPosition(getPosition(((Token)yyVals[-2+yyTop])));
                      yyVal = ((Node)yyVals[-1+yyTop]);
                  } else {
                      yyVal = new NilNode(getPosition(((Token)yyVals[-2+yyTop])));
                  }
    return yyVal;
}
public Object case287_line1095(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new ZYieldNode(getPosition(((Token)yyVals[-2+yyTop])));
    return yyVal;
}
public Object case56_line507(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case452_line1699(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.new_args(getPosition(((ListNode)yyVals[-3+yyTop])), null, ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case55_line504(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])); 
    return yyVal;
}
public Object case32_line429(Object yyVal, Object[] yyVals, int yyTop) {
                  if (((MultipleAsgnNode)yyVals[-2+yyTop]).getHeadNode() != null) {
		      ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(new ToAryNode(getPosition(((MultipleAsgnNode)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
		  } else {
		      ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(support.newArrayNode(getPosition(((MultipleAsgnNode)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
		  }
		  yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
    return yyVal;
}
public Object case29_line420(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public Object case82_line604(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
    return yyVal;
}
public Object case458_line1719(Object yyVal, Object[] yyVals, int yyTop) {
                   yyerror("formal argument cannot be a instance variable");
    return yyVal;
}
public Object case401_line1523(Object yyVal, Object[] yyVals, int yyTop) {
		   yyVal = ((ListNode)yyVals[-1+yyTop]);
                   ((ISourcePositionHolder)yyVal).setPosition(getPosition(((Token)yyVals[-2+yyTop])));
    return yyVal;
}
public Object case397_line1510(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]) instanceof EvStrNode ? new DStrNode(getPosition(((ListNode)yyVals[-2+yyTop]))).add(((Node)yyVals[-1+yyTop])) : ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case361_line1357(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case73_line562(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case57_line510(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])); 
    return yyVal;
}
public Object case511_line1884(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = null;
    return yyVal;
}
public Object case435_line1638(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new Token("false", Tokens.kFALSE, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case408_line1552(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case67_line543(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new MultipleAsgnNode(getPosition(((ListNode)yyVals[-1+yyTop])), ((ListNode)yyVals[-1+yyTop]), new StarNode(getPosition()));
    return yyVal;
}
public Object case365_line1369(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new ZSuperNode(((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case66_line540(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new MultipleAsgnNode(getPosition(((ListNode)yyVals[-2+yyTop])), ((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case191_line783(Object yyVal, Object[] yyVals, int yyTop) {
		  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case300_line1134(Object yyVal, Object[] yyVals, int yyTop) {
                  Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);
                  yyVal = new UntilNode(getPosition(((Token)yyVals[-6+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), body);
    return yyVal;
}
public Object case480_line1819(Object yyVal, Object[] yyVals, int yyTop) {
                  if (((Node)yyVals[-2+yyTop]) == null) {
                      yyerror("can't define single method for ().");
                  } else if (((Node)yyVals[-2+yyTop]) instanceof ILiteralNode) {
                      yyerror("can't define single method for literals.");
                  }
		  support.checkExpression(((Node)yyVals[-2+yyTop]));
                  yyVal = ((Node)yyVals[-2+yyTop]);
    return yyVal;
}
public Object case236_line928(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((ListNode)yyVals[-3+yyTop]).add(new HashNode(getPosition(), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case19_line361(Object yyVal, Object[] yyVals, int yyTop) {
                  Node body = ((Node)yyVals[0+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop]);
	          yyVal = new RescueNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), new RescueBodyNode(getPosition(((Node)yyVals[-2+yyTop])), null, body, null), null);
    return yyVal;
}
public Object case52_line495(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_fcall(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case417_line1590(Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.setState(LexState.EXPR_END);
                   yyVal = ((Token)yyVals[0+yyTop]);
		   ((ISourcePositionHolder)yyVal).setPosition(getPosition(((Token)yyVals[-1+yyTop])));
    return yyVal;
}
public Object case376_line1402(Object yyVal, Object[] yyVals, int yyTop) {
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
                  yyVal = new RescueBodyNode(getPosition(((Token)yyVals[-5+yyTop])), ((Node)yyVals[-4+yyTop]), body, ((RescueBodyNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case340_line1275(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new MultipleAsgnNode(getPosition(((ListNode)yyVals[-1+yyTop])), ((ListNode)yyVals[-1+yyTop]), null);
    return yyVal;
}
public Object case448_line1687(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.new_args(getPosition(((ListNode)yyVals[-5+yyTop])), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case396_line1507(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new ArrayNode(getPosition());
    return yyVal;
}
public Object case381_line1425(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case224_line888(Object yyVal, Object[] yyVals, int yyTop) {
                  support.checkExpression(((Node)yyVals[-1+yyTop]));
		  yyVal = new NewlineNode(getPosition(((Token)yyVals[-2+yyTop])), support.newSplatNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])));
    return yyVal;
}
public Object case347_line1296(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newBlockArg18(((Token)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]), new MultipleAsgnNode(getPosition(((Token)yyVals[-3+yyTop])), null, new StarNode(getPosition(((Token)yyVals[-3+yyTop])))));
    return yyVal;
}
public Object case36_line446(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newAndNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case237_line932(Object yyVal, Object[] yyVals, int yyTop) {
                  support.checkExpression(((Node)yyVals[-1+yyTop]));
		  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-6+yyTop])), ((ListNode)yyVals[-6+yyTop]).add(new HashNode(getPosition(), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case217_line865(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case277_line1049(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.setState(LexState.EXPR_ENDARG); 
    return yyVal;
}
public Object case223_line885(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newArrayNode(getPosition(((ListNode)yyVals[-1+yyTop])), new HashNode(getPosition(), ((ListNode)yyVals[-1+yyTop])));
    return yyVal;
}
public Object case285_line1089(Object yyVal, Object[] yyVals, int yyTop) {
		  yyVal = new ReturnNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
    return yyVal;
}
public Object case350_line1305(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newBlockArg18(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case322_line1229(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new RetryNode(((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case238_line937(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_blk_pass(support.newSplatNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case94_line652(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_colon2(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case8_line320(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case4_line306(Object yyVal, Object[] yyVals, int yyTop) {
                  if (((Node)yyVals[-1+yyTop]) instanceof BlockNode) {
                      support.checkUselessStatements(((BlockNode)yyVals[-1+yyTop]));
		  }
                  yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case17_line347(Object yyVal, Object[] yyVals, int yyTop) {
                  if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                      yyVal = new WhileNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                  } else {
                      yyVal = new WhileNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                  }
    return yyVal;
}
public Object case317_line1208(Object yyVal, Object[] yyVals, int yyTop) {
                  support.setInSingle(support.getInSingle() + 1);
		  support.pushLocalScope();
                  lexer.setState(LexState.EXPR_END); /* force for args */
    return yyVal;
}
public Object case450_line1693(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.new_args(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), null, ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case409_line1555(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = lexer.getStrTerm();
		   lexer.setStrTerm(null);
		   lexer.setState(LexState.EXPR_BEG);
    return yyVal;
}
public Object case399_line1515(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case178_line737(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public Object case87_line619(Object yyVal, Object[] yyVals, int yyTop) {
                  if (support.isInDef() || support.isInSingle()) {
		      yyerror("dynamic constant assignment");
		  }
			
		  ISourcePosition position = getPosition(((Node)yyVals[-2+yyTop]));

                  yyVal = new ConstDeclNode(position, null, support.new_colon2(position, ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
    return yyVal;
}
public Object case509_line1876(Object yyVal, Object[] yyVals, int yyTop) {
                  yyerrok();
    return yyVal;
}
public Object case459_line1722(Object yyVal, Object[] yyVals, int yyTop) {
                   yyerror("formal argument cannot be an global variable");
    return yyVal;
}
public Object case308_line1164(Object yyVal, Object[] yyVals, int yyTop) {
                  Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);

                  yyVal = new ClassNode(getPosition(((Token)yyVals[-5+yyTop])), ((Colon3Node)yyVals[-4+yyTop]), support.getCurrentScope(), body, ((Node)yyVals[-3+yyTop]));
                  support.popCurrentScope();
    return yyVal;
}
public Object case210_line844(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "~");
    return yyVal;
}
public Object case240_line943(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_blk_pass(support.newArrayNode(getPosition(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop])).addAll(((ListNode)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case86_line616(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case68_line546(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new MultipleAsgnNode(getPosition(((Token)yyVals[-1+yyTop])), null, ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case179_line740(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public Object case247_line969(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newArrayNode(getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop])).addAll(((ListNode)yyVals[-3+yyTop])).add(new HashNode(getPosition(), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case48_line479(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case253_line989(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                  yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case296_line1124(Object yyVal, Object[] yyVals, int yyTop) {
		  lexer.getConditionState().end();
    return yyVal;
}
public Object case77_line575(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case313_line1188(Object yyVal, Object[] yyVals, int yyTop) {
                  Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);

                  yyVal = new ModuleNode(getPosition(((Token)yyVals[-4+yyTop])), ((Colon3Node)yyVals[-3+yyTop]), support.getCurrentScope(), body);
                  support.popCurrentScope();
    return yyVal;
}
public Object case10_line326(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newAlias(getPosition(((Token)yyVals[-3+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case461_line1728(Object yyVal, Object[] yyVals, int yyTop) {
                   String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();
                   if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                       yyerror("duplicate argument name");
                   }

		   support.getCurrentScope().getLocalScope().addVariable(identifier);
                   yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
}
public Object case263_line1021(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case49_line482(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case79_line581(Object yyVal, Object[] yyVals, int yyTop) {
                  if (support.isInDef() || support.isInSingle()) {
		      yyerror("dynamic constant assignment");
		  }

		  ISourcePosition position = getPosition(((Node)yyVals[-2+yyTop]));

                  yyVal = new ConstDeclNode(position, null, support.new_colon2(position, ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
    return yyVal;
}
public Object case309_line1170(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = Boolean.valueOf(support.isInDef());
                  support.setInDef(false);
    return yyVal;
}
public Object case203_line823(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case13_line335(Object yyVal, Object[] yyVals, int yyTop) {
                  yyerror("can't make alias for the number variables");
    return yyVal;
}
public Object case464_line1748(Object yyVal, Object[] yyVals, int yyTop) {
                   support.allowDubyExtension(((ISourcePositionHolder)yyVals[-4+yyTop]).getPosition());
                   ((ListNode)yyVals[-4+yyTop]).add(new TypedArgumentNode(((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition(), (String) ((Token)yyVals[-2+yyTop]).getValue(), ((Node)yyVals[0+yyTop])));
                   ((ListNode)yyVals[-4+yyTop]).setPosition(getPosition(((ListNode)yyVals[-4+yyTop])));
		   yyVal = ((ListNode)yyVals[-4+yyTop]);
    return yyVal;
}
public Object case377_line1415(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = null;
    return yyVal;
}
public Object case370_line1387(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newWhenNode(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case366_line1374(Object yyVal, Object[] yyVals, int yyTop) {
                  support.pushBlockScope();
    return yyVal;
}
public Object case99_line663(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.setState(LexState.EXPR_END);
                  yyVal = yyVals[0+yyTop];
    return yyVal;
}
public Object case248_line973(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-6+yyTop])), support.newArrayNode(getPosition(((Node)yyVals[-6+yyTop])), ((Node)yyVals[-6+yyTop])).add(new HashNode(getPosition(), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case103_line680(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case479_line1817(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.setState(LexState.EXPR_BEG);
    return yyVal;
}
public Object case388_line1448(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((Node)yyVals[0+yyTop]) instanceof EvStrNode ? new DStrNode(getPosition(((Node)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop])) : ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case186_line768(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "+", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case100_line669(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new LiteralNode(((Token)yyVals[0+yyTop]));
    return yyVal;
}
public Object case54_line501(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case314_line1194(Object yyVal, Object[] yyVals, int yyTop) {
                  support.setInDef(true);
		  support.pushLocalScope();
    return yyVal;
}
public Object case75_line569(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.aryset(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case189_line777(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "/", ((Node)yyVals[0+yyTop]), getPosition());
    return yyVal;
}
public Object case278_line1051(Object yyVal, Object[] yyVals, int yyTop) {
		 if (warnings.isVerbose()) warnings.warning(ID.GROUPED_EXPRESSION, getPosition(((Token)yyVals[-4+yyTop])), "(...) interpreted as grouped expression");
                  yyVal = ((Node)yyVals[-3+yyTop]);
    return yyVal;
}
public Object case311_line1177(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new SClassNode(getPosition(((Token)yyVals[-7+yyTop])), ((Node)yyVals[-5+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  support.popCurrentScope();
                  support.setInDef(((Boolean)yyVals[-4+yyTop]).booleanValue());
                  support.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
    return yyVal;
}
					// line 1889 "DefaultRubyParser.y"

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
					// line 7888 "-"
