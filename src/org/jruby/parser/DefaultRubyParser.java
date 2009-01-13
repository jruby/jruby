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
    }

    public void setWarnings(IRubyWarnings warnings) {
        this.warnings = warnings;

        support.setWarnings(warnings);
        lexer.setWarnings(warnings);
    }
					// line 157 "-"
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
//yyLhs 496
    -1,    99,     0,    34,    33,    35,    35,    35,    35,   102,
    36,    36,    36,    36,    36,    36,    36,    36,    36,    36,
   103,    36,    36,    36,    36,    36,    36,    36,    36,    36,
    36,    36,    36,    36,    36,    37,    37,    37,    37,    37,
    37,    41,    32,    32,    32,    32,    32,    57,    57,    57,
   104,    92,    40,    40,    40,    40,    40,    40,    40,    40,
    93,    93,    95,    95,    94,    94,    94,    94,    94,    94,
    65,    65,    80,    80,    66,    66,    66,    66,    66,    66,
    66,    66,    73,    73,    73,    73,    73,    73,    73,    73,
     8,     8,    31,    31,    31,     9,     9,     9,     9,     9,
     2,     2,    61,   106,    61,    10,    10,    10,    10,    10,
    10,    10,    10,    10,    10,    10,    10,    10,    10,    10,
    10,    10,    10,    10,    10,    10,    10,    10,    10,    10,
    10,   105,   105,   105,   105,   105,   105,   105,   105,   105,
   105,   105,   105,   105,   105,   105,   105,   105,   105,   105,
   105,   105,   105,   105,   105,   105,   105,   105,   105,   105,
   105,   105,   105,   105,   105,   105,   105,   105,   105,   105,
   105,   105,    38,    38,    38,    38,    38,    38,    38,    38,
    38,    38,    38,    38,    38,    38,    38,    38,    38,    38,
    38,    38,    38,    38,    38,    38,    38,    38,    38,    38,
    38,    38,    38,    38,    38,    38,    38,    38,    38,    38,
    38,    38,    38,    38,    38,    38,    67,    70,    70,    70,
    70,    70,    70,    51,    51,    51,    51,    55,    55,    47,
    47,    47,    47,    47,    47,    47,    47,    47,    48,    48,
    48,    48,    48,    48,    48,    48,    48,    48,    48,    48,
   109,    53,    49,   110,    49,   111,    49,    86,    85,    85,
    79,    79,    64,    64,    64,    39,    39,    39,    39,    39,
    39,    39,    39,    39,    39,   112,    39,    39,    39,    39,
    39,    39,    39,    39,    39,    39,    39,    39,    39,    39,
    39,    39,    39,   114,   116,    39,   117,   118,    39,    39,
    39,    39,   119,   120,    39,   121,    39,   123,   124,    39,
   125,    39,   126,    39,   127,   128,    39,    39,    39,    39,
    39,    42,   113,   113,   113,   113,   115,   115,   115,    45,
    45,    43,    43,    71,    71,    72,    72,    72,    72,   129,
    91,    56,    56,    56,    24,    24,    24,    24,    24,    24,
   130,    90,   131,    90,    68,    84,    84,    84,    44,    44,
    96,    96,    69,    69,    69,    46,    46,    50,    50,    28,
    28,    28,    16,    17,    17,    18,    19,    20,    25,    25,
    76,    76,    27,    27,    26,    26,    75,    75,    21,    21,
    22,    22,    23,   132,    23,   133,    23,    62,    62,    62,
    62,     4,     3,     3,     3,     3,    30,    29,    29,    29,
    29,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,    54,    97,    63,    63,    52,   134,    52,    52,
    58,    58,    59,    59,    59,    59,    59,    59,    59,    59,
    59,    11,    11,    11,    11,    11,    77,    77,    77,    77,
    60,    78,    78,    13,    13,    98,    98,    14,    14,    89,
    88,    88,    15,   135,    15,    83,    83,    83,    81,    81,
    82,     5,     5,     5,     6,     6,     6,     6,     7,     7,
     7,    12,    12,   100,   100,   107,   107,   108,   108,   108,
   122,   122,   101,   101,    74,    87,
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
     0,   421,   420,     0,     0,   413,   412,     0,   415,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   388,   390,   390,     0,     0,   424,   425,   407,   408,
     0,   370,     0,   266,     0,   373,   267,   268,     0,   269,
   270,   265,   369,   371,    35,     2,     0,     0,     0,     0,
     0,     0,     0,   271,     0,    43,     0,     0,    70,     0,
     5,     0,     0,    60,     0,     0,   317,   318,   283,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   422,     0,
    93,     0,   321,     0,   272,   310,   140,   151,   141,   164,
   137,   157,   147,   146,   162,   145,   144,   139,   165,   149,
   138,   152,   156,   158,   150,   143,   159,   166,   161,     0,
     0,     0,     0,   136,   155,   154,   167,   168,   169,   170,
   171,   135,   142,   133,   134,     0,     0,     0,    97,     0,
   126,   127,   124,   108,   109,   110,   113,   115,   111,   128,
   129,   116,   117,   463,   121,   120,   107,   125,   123,   122,
   118,   119,   114,   112,   105,   106,   130,   312,    98,     0,
   462,    99,   160,   153,   163,   148,   131,   132,    95,    96,
     0,   102,   101,   100,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   491,   490,     0,     0,
     0,   492,     0,     0,     0,     0,     0,     0,     0,   333,
   334,     0,     0,     0,     0,   229,    45,     0,     0,     0,
   468,   237,    46,    44,     0,    59,     0,     0,   348,    58,
    38,     0,     9,   486,     0,     0,     0,   192,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   217,     0,     0,   465,     0,     0,     0,     0,     0,
     0,     0,    68,   208,    39,   207,   404,   403,   405,   401,
   402,     0,     0,     0,     0,     0,     0,     0,     0,   352,
   350,   344,     0,   288,   374,   290,     4,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   339,   341,     0,     0,     0,     0,     0,     0,    72,
     0,     0,     0,     0,     0,     0,     0,   409,   410,     0,
    90,     0,    92,     0,   427,   305,   426,     0,     0,     0,
     0,     0,     0,   481,   482,   314,   103,     0,     0,   274,
     0,   324,   323,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   493,     0,     0,     0,
     0,     0,     0,   302,     0,   257,     0,     0,   230,   259,
     0,   232,   285,     0,     0,   252,   251,     0,     0,     0,
     0,     0,    11,    13,    12,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   277,     0,     0,     0,
   218,   281,     0,   488,   219,     0,   221,     0,   467,   466,
   282,     0,     0,     0,     0,   395,   393,   406,   392,   391,
   375,   389,   376,   377,   378,   379,   382,     0,   384,   385,
     0,     0,     0,    50,    53,     0,    15,    16,    17,    18,
    19,    36,    37,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   476,     0,     0,   477,     0,     0,     0,     0,   347,
     0,     0,   474,   475,     0,     0,    30,     0,     0,    23,
     0,    31,   260,     0,     0,    66,    73,    24,    33,     0,
    25,     0,     0,   429,     0,     0,     0,     0,     0,     0,
    94,     0,     0,     0,     0,   443,   442,   441,   444,     0,
   454,   453,   458,   457,     0,     0,     0,     0,     0,   451,
     0,     0,   439,     0,     0,     0,   363,     0,     0,   364,
     0,     0,   331,     0,   325,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   300,   328,   327,
   294,   326,   297,     0,     0,     0,     0,     0,     0,     0,
   236,   470,     0,     0,     0,   258,     0,     0,   469,   284,
     0,     0,   255,     0,     0,   249,     0,     0,     0,     0,
     0,   223,     0,    10,     0,     0,    22,     0,     0,     0,
     0,     0,   222,     0,   261,     0,     0,     0,     0,     0,
     0,     0,   381,   383,   387,   337,     0,     0,   335,     0,
     0,     0,     0,     0,     0,   228,     0,   345,   227,     0,
     0,   346,     0,     0,    48,   342,    49,   343,   264,     0,
     0,    71,   308,     0,     0,   280,   311,     0,     0,     0,
     0,   455,   459,     0,   431,     0,   435,     0,   437,     0,
   438,   315,   104,     0,     0,   366,   332,     0,     3,   368,
     0,   329,     0,     0,     0,     0,     0,     0,   299,   301,
   357,     0,     0,     0,     0,     0,     0,     0,     0,   234,
     0,     0,     0,     0,     0,   242,   254,   224,     0,     0,
   225,     0,     0,   287,    21,   276,     0,     0,     0,   397,
   398,   399,   394,   400,   336,     0,     0,     0,     0,     0,
     0,    27,     0,    28,     0,    55,    29,     0,     0,    57,
     0,     0,     0,     0,     0,   428,   306,   464,   450,     0,
   446,   313,     0,     0,   460,     0,     0,   452,     0,     0,
     0,     0,     0,     0,   365,     0,   367,     0,   291,     0,
   292,     0,     0,     0,     0,   303,   231,     0,   233,   248,
   256,     0,     0,     0,   239,     0,     0,   220,   396,   338,
   353,   351,     0,   340,    26,     0,   263,     0,   430,     0,
     0,   433,   434,   436,     0,     0,     0,     0,     0,     0,
     0,   356,   358,   354,   359,   295,   298,     0,     0,     0,
     0,   238,     0,   244,     0,   226,    51,   309,   448,     0,
     0,     0,     0,     0,     0,     0,   360,     0,     0,   235,
   240,     0,     0,     0,   243,   432,   316,     0,   330,   304,
     0,     0,   245,     0,   241,     0,   246,     0,   247,
    }, yyDgoto = {
//yyDgoto 136
     1,   208,   201,   289,    61,   109,   546,   519,   110,   203,
   514,   564,   375,   565,   566,   189,    63,    64,    65,    66,
    67,   292,   291,   459,    68,    69,    70,   467,    71,    72,
    73,   111,    74,   205,   206,    76,    77,    78,    79,    80,
    81,   210,   258,   711,   843,   712,   704,   236,   622,   416,
   708,   665,   365,   245,    83,   667,    84,    85,   567,   568,
   569,   204,   752,   212,   531,    87,    88,   237,   395,   578,
   270,   228,   657,   213,    90,   298,   296,   570,   571,   272,
    91,   273,   240,   277,   596,   408,   615,   409,   696,   784,
   303,   342,   474,    92,    93,   266,   378,   214,   573,     2,
   219,   220,   425,   255,   660,   191,   575,   254,   444,   246,
   626,   732,   438,   383,   222,   600,   723,   223,   724,   608,
   847,   545,   384,   542,   774,   370,   372,   574,   789,   509,
   472,   471,   651,   650,   544,   371,
    }, yySindex = {
//yySindex 889
     0,     0,  5308, 13396, 16717, 17086, 17671, 17563,  5308, 15241,
 15241,  6905,     0,     0, 16840, 13765, 13765,     0,     0, 13765,
  -229,  -170,     0,     0,     0,     0, 15241, 17455,   163,     0,
  -178,     0,     0,     0,     0,     0,     0,     0,     0, 16471,
 16471,  -188,   -97, 13273, 15241, 15364, 16471, 17209, 16471, 16594,
 17778,     0,     0,     0,   191,   206,     0,     0,     0,     0,
     0,     0,  -189,     0,  -122,     0,     0,     0,  -227,     0,
     0,     0,     0,     0,     0,     0,    78,  1036,   -92,  4356,
     0,   -36,   160,     0,  -181,     0,   -74,   223,     0,   210,
     0, 16963,   217,     0,   -56,  1036,     0,     0,     0,  -229,
  -170,   163,     0,     0,   174, 15241,   -54,  5308,     0,  -189,
     0,    10,     0,   192,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  -164,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
 17778,     0,     0,     0,   249,    36,    73,    21,     0,   -92,
   156,   195,    15,   333,   109,   156,     0,     0,    78,    13,
   351,     0, 15241, 15241,   144,     0,   401,     0,   180,     0,
     0, 16471, 16471, 16471,  4356,     0,     0,   132,   456,   468,
     0,     0,     0,     0, 13519,     0, 13888, 13765,     0,     0,
     0,  -214,     0,     0, 15487,   161,  5308,     0,   454,   200,
   225,   230,   197, 13273,   216,     0,   221,   -92, 16471,   163,
   228,     0,   113,   130,     0,   143,   130,   215,   289,     0,
   470,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  -150,  -112,   280,  -192,   240,   354,   244,  -275,     0,
     0,     0,   256,     0,     0,     0,     0, 13149, 15241, 15241,
 15241, 15241, 13396, 15241, 15241, 16471, 16471, 16471, 16471, 16471,
 16471, 16471, 16471, 16471, 16471, 16471, 16471, 16471, 16471, 16471,
 16471, 16471, 16471, 16471, 16471, 16471, 16471, 16471, 16471, 16471,
 16471,     0,     0, 17940, 17995, 15364, 18050, 18050, 16594,     0,
 15610, 13273, 17209,   560, 15610, 16594,   278,     0,     0,   -92,
     0,     0,     0,    78,     0,     0,     0, 18050, 18105, 15364,
  5308, 15241,  1294,     0,     0,     0,     0, 15733,   350,     0,
   197,     0,     0,  5308,   356, 18160, 18215, 15364, 16471, 16471,
 16471,  5308,   355,  5308, 15856,   357,     0,    83,    83,     0,
 18270, 18325, 15364,     0,   583,     0, 16471, 14011,     0,     0,
 14134,     0,     0,   291, 13642,     0,     0,   -36,   163,   121,
   296,   594,     0,     0,     0, 17563, 15241,  4356,  5308,   283,
 18160, 18215, 16471, 16471, 16471,   312,     0,     0,   163,  2781,
     0,     0, 15979,     0,     0, 16471,     0, 16471,     0,     0,
     0,     0, 18380, 18435, 15364,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,    67,     0,     0,
   608,  -166,  -166,     0,     0,  1036,     0,     0,     0,     0,
     0,     0,     0,   200,  1924,  1924,  1924,  1924,  1750,  1750,
  3305,  2834,  1924,  1924,  2418,  2418,   281,   281,   200,  1408,
   200,   200,   -98,   -98,  1750,  1750,   897,   897,  3391,  -166,
   319,     0,   324,  -170,     0,   337,     0,   339,  -170,     0,
     0,   314,     0,     0,  -170,  -170,     0,  4356, 16471,     0,
  3870,     0,     0,   638,   345,     0,     0,     0,     0,     0,
     0,  4356,    78,     0, 15241,  5308,  -170,     0,     0,  -170,
     0,   344,   430,   190,   634,     0,     0,     0,     0,   913,
     0,     0,     0,     0,   370,   405,   414,  5308,    78,     0,
   678,   684,     0,   686, 17885, 17563,     0,     0,   394,     0,
  5308,   472,     0,   299,     0,   396,   403,   407,   339,   398,
  3870,   350,   474,   481, 16471,   703,   156,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   406, 15241,   402,
     0,     0, 16471,   132,   712,     0, 16471,   132,     0,     0,
 16471,  4356,     0,     4,   714,     0,   421,   438, 18050, 18050,
   442,     0, 14257,     0,  -145,   419,     0,   200,   200,  4356,
     0,   450,     0, 16471,     0,     0,     0,     0,     0,   449,
  5308,  -127,     0,     0,     0,     0,  4820,  5308,     0,  5308,
  -166, 16471,  5308, 16594, 16594,     0,   256,     0,     0, 16594,
 16471,     0,   256,   462,     0,     0,     0,     0,     0, 16471,
 16102,     0,     0,    78,   533,     0,     0,   466, 16471,   163,
 16471,     0,     0,   547,     0,   913,     0,  -143,     0,   232,
     0,     0,     0, 17332,   156,     0,     0,  5308,     0,     0,
 15241,     0,   553, 16471, 16471, 16471,   483,   558,     0,     0,
     0, 16225,  5308,  5308,  5308,     0,    83,   583, 14380,     0,
   583,   583,   485, 14503, 14626,     0,     0,     0,  -170,  -170,
     0,   -36,   121,     0,     0,     0,  2781,     0,   463,     0,
     0,     0,     0,     0,     0,   486,   566,   480,  5308,  4356,
   579,     0,  4356,     0,  4356,     0,     0,  4356,  4356,     0,
 16594,  4356, 16471,     0,  5308,     0,     0,     0,     0,   513,
     0,     0,   525,   820,     0,   686,   634,     0,   686,  1294,
   555,     0,   522,     0,     0,  5308,     0,   156,     0, 16471,
     0, 16471,    58,   603,   607,     0,     0, 16471,     0,     0,
     0, 16471,   831,   845,     0, 16471,   532,     0,     0,     0,
     0,     0,   530,     0,     0,  4356,     0,   630,     0, 16471,
  -143,     0,     0,     0,  5308,     0, 18490, 18545, 15364,    36,
  5308,     0,     0,     0,     0,     0,     0,  5308,  2898,   583,
 14749,     0, 14872,     0,   583,     0,     0,     0,     0,   686,
   631,     0,     0,     0,     0,   557,     0,   299,   641,     0,
     0, 16471,   858, 16471,     0,     0,     0,     0,     0,     0,
   583, 14995,     0,   583,     0, 16471,     0,   583,     0,
    }, yyRindex = {
//yyRindex 889
     0,     0,   108,     0,     0,     0,     0,     0,  1114,     0,
     0,    63,     0,     0,     0,  8485,  8614,     0,     0,  8725,
  4607,  3998,     0,     0,     0,     0,     0,     0, 16348,     0,
     0,     0,     0,  2053,  3149,     0,     0,  2177,     0,     0,
     0,     0,     0,     5,     0,   562,   549,   321,     0,     0,
   508,     0,     0,     0,   605,  -147,     0,     0,     0,     0,
  9685,     0, 15118,     0,  7765,     0,     0,     0,  7894,     0,
     0,     0,     0,     0,     0,     0,    28,   269,  5209,  3268,
  8005,  3754,     0,     0,  4240,     0,  9814,     0,     0,     0,
     0,   436,     0,     0,     0,   584,     0,     0,     0,  8134,
  7045,   573,  5849,  5991,     0,     0,     0,     5,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  1009,
  1178,  1616,  1875,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  2298,  3271,  3757,     0,  4243,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0, 13056,    11,     0,     0,  7174,  4726,
     0,     0,  7414,     0,     0,     0,     0,     0,   645,     0,
   270,     0,     0,     0,     0,   245,     0,   250,     0,     0,
     0,     0,     0,     0,  1724,     0,     0, 12849,  2295,  2295,
     0,     0,     0,     0,     0,     0,     0,   577,     0,     0,
     0,     0,     0,     0,     0,     0,    19,     0,     0,  8854,
  8245,  8374,  9925,     5,     0,    45,     0,    29,     0,   575,
     0,     0,   578,   578,     0,   564,   564,     0,     0,  1062,
     0,  1101,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  1577,     0,     0,     0,     0,   247,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   562,     0,     0,     0,     0,
     0,     5,   475,   559,     0,     0,     0,     0,     0,   117,
     0,  6378,     0,     0,     0,     0,     0,     0,     0,   562,
  1114,     0,   142,     0,     0,     0,     0,   110,   327,     0,
  7525,     0,     0,   484,  6507,     0,     0,   562,     0,     0,
     0,   176,     0,   138,     0,     0,     0,     0,     0,   460,
     0,     0,   562,     0,  2295,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   586,     0,     0,    48,   587,   587,
     0,    55,     0,     0,     0,     0,     0, 12106,    19,     0,
     0,     0,     0,     0,     0,     0,     0,    99,   587,   575,
     0,     0,   590,     0,     0,  -262,     0,   568,     0,     0,
     0,  1361,     0,     0,   562,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  6636,  6776,     0,     0,   687,     0,     0,     0,     0,
     0,     0,     0,  8965,   717, 11368, 11474, 11559, 10921, 11036,
 11644, 11899, 11729, 11814, 11984, 12021, 10372, 10478,  9094, 10589,
  9205,  9334, 10146, 10257, 11142, 11253, 10704, 10810,     0,  6636,
  4968,     0,  5091,  4121,     0,  5454,  3512,  5577, 15118,     0,
  3635,     0,     0,     0,  5700,  5700,     0, 12191,     0,     0,
 11384,     0,     0,     0,     0,     0,     0,     0,     0, 13042,
     0, 12276,     0,     0,     0,  1114,  7285,  6120,  6249,     0,
     0,     0,     0,   587,    71,     0,     0,     0,     0,    75,
     0,     0,     0,     0,    66,    70,     0,  1114,     0,     0,
    27,    27,     0,    27,     0,     0,     0,    77,   440,     0,
    97,   671,     0,   671,     0,  2540,  2663,  3026,  4484,     0,
 12934,   671,     0,     0,     0,   511,     0,     0,     0,     0,
     0,     0,     0,   849,  1134,  1453,   150,     0,     0,     0,
     0,     0,     0, 12971,  2295,     0,     0,     0,     0,     0,
     0,   145,     0,     0,   598,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  9445,  9574, 12313,
    22,     0,     0,     0,     0,   741,   743,  1230,   699,     0,
    19,     0,     0,     0,     0,     0,     0,   138,     0,    19,
  6776,     0,   138,     0,     0,     0,  2423,     0,     0,     0,
     0,     0,  2782, 10036,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   587,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   138,     0,     0,
     0,     0,     0,     0,     0,     0,  7654,     0,     0,     0,
     0,     0,   218,   138,   138,   580,     0,  2295,     0,     0,
  2295,   598,     0,     0,     0,     0,     0,     0,   123,   123,
     0,     0,   587,     0,     0,     0,   575,  1548,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,    19, 12398,
     0,     0, 12435,     0, 12520,     0,     0, 12605, 12642,     0,
     0, 12727,     0,  1901,  1114,     0,     0,     0,     0,     0,
     0,     0,   106,    27,     0,    27,     0,     0,    27,   142,
     0,   496,     0,   548,     0,  1114,     0,     0,     0,     0,
     0,     0,   671,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   598,   598,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0, 12764,     0,     0,     0,     0,
     0,     0,     0,     0,  1114,   601,     0,     0,   562,    11,
   484,     0,     0,     0,     0,     0,     0,   138,  2295,   598,
     0,     0,     0,     0,   598,     0,     0,     0,     0,    27,
     0,   829,   938,  1067,   534,     0,     0,   671,     0,     0,
     0,     0,   598,     0,     0,     0,     0,   851,     0,     0,
   598,     0,     0,   598,     0,     0,     0,   598,     0,
    }, yyGindex = {
//yyGindex 136
     0,   957,    -1,     0,    -4,   888,  -273,     0,    -3,    12,
    40,   246,     0,     0,     0,     0,     0,     0,   879,     0,
     0,     0,   543,  -191,     0,     0,     0,     0,     0,     0,
     0,   942,     3,  1366,  -343,     0,    86,   659,   -15,     7,
    -2,    39,   458,  -323,     0,    85,     0,   677,     0,     0,
     0,    17,     0,     1,   947,   -49,  -231,     0,   165,   415,
  -653,     0,     0,  1048,  -245,   866,    -7,  1419,  -360,     0,
  -310,   308,  -334,  1128,   899,     0,     0,     0,   271,    26,
     0,   -10,  -306,     0,     0,  -171,    41,     0,   248,  -364,
   915,     0,  -463,   -12,    32,  -202,   148,  1320,  -573,     0,
   -25,   914,     0,     0,     0,     0,     0,    -5,   -65,     0,
     0,     0,     0,  -208,     0,  -388,     0,     0,     0,     0,
     0,     0,     8,     0,     0,     0,     0,     0,     0,     0,
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
					// line 269 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_BEG);
                  support.initTopLocalVariables();
              }
  break;
case 2:
					// line 272 "DefaultRubyParser.y"
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
					// line 284 "DefaultRubyParser.y"
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
					// line 301 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-1+yyTop]) instanceof BlockNode) {
                      support.checkUselessStatements(((BlockNode)yyVals[-1+yyTop]));
		  }
                  yyVal = ((Node)yyVals[-1+yyTop]);
              }
  break;
case 6:
					// line 309 "DefaultRubyParser.y"
  {
                  yyVal = support.newline_node(((Node)yyVals[0+yyTop]), getPosition(((Node)yyVals[0+yyTop]), true));
              }
  break;
case 7:
					// line 312 "DefaultRubyParser.y"
  {
	          yyVal = support.appendToBlock(((Node)yyVals[-2+yyTop]), support.newline_node(((Node)yyVals[0+yyTop]), getPosition(((Node)yyVals[0+yyTop]), true)));
              }
  break;
case 8:
					// line 315 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 9:
					// line 319 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_FNAME);
              }
  break;
case 10:
					// line 321 "DefaultRubyParser.y"
  {
                  yyVal = new AliasNode(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 11:
					// line 324 "DefaultRubyParser.y"
  {
                  yyVal = new VAliasNode(getPosition(((Token)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 12:
					// line 327 "DefaultRubyParser.y"
  {
                  yyVal = new VAliasNode(getPosition(((Token)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), "$" + ((BackRefNode)yyVals[0+yyTop]).getType()); /* XXX*/
              }
  break;
case 13:
					// line 330 "DefaultRubyParser.y"
  {
                  yyerror("can't make alias for the number variables");
              }
  break;
case 14:
					// line 333 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 15:
					// line 336 "DefaultRubyParser.y"
  {
                  yyVal = new IfNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), null);
              }
  break;
case 16:
					// line 339 "DefaultRubyParser.y"
  {
                  yyVal = new IfNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), null, ((Node)yyVals[-2+yyTop]));
              }
  break;
case 17:
					// line 342 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                      yyVal = new WhileNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                  } else {
                      yyVal = new WhileNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                  }
              }
  break;
case 18:
					// line 349 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                      yyVal = new UntilNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                  } else {
                      yyVal = new UntilNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                  }
              }
  break;
case 19:
					// line 356 "DefaultRubyParser.y"
  {
                  Node body = ((Node)yyVals[0+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop]);
	          yyVal = new RescueNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), new RescueBodyNode(getPosition(((Node)yyVals[-2+yyTop])), null, body, null), null);
              }
  break;
case 20:
					// line 360 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
                      yyerror("BEGIN in method");
                  }
		  support.pushLocalScope();
              }
  break;
case 21:
					// line 365 "DefaultRubyParser.y"
  {
                  support.getResult().addBeginNode(new PreExeNode(getPosition(((Node)yyVals[-1+yyTop])), support.getCurrentScope(), ((Node)yyVals[-1+yyTop])));
                  support.popCurrentScope();
                  yyVal = null; /*XXX 0;*/
              }
  break;
case 22:
					// line 370 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
                      warnings.warn(ID.END_IN_METHOD, getPosition(((Token)yyVals[-3+yyTop])), "END in method; use at_exit");
                  }
                  yyVal = new PostExeNode(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 23:
					// line 376 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
                  yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 24:
					// line 380 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
		  if (((MultipleAsgnNode)yyVals[-2+yyTop]).getHeadNode() != null) {
		      ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(new ToAryNode(getPosition(((MultipleAsgnNode)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
		  } else {
		      ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(support.newArrayNode(getPosition(((MultipleAsgnNode)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
		  }
		  yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
              }
  break;
case 25:
					// line 389 "DefaultRubyParser.y"
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
					// line 405 "DefaultRubyParser.y"
  {
                  yyVal = support.new_opElementAsgnNode(getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));

              }
  break;
case 27:
					// line 409 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 28:
					// line 412 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 29:
					// line 415 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 30:
					// line 418 "DefaultRubyParser.y"
  {
                  support.backrefAssignError(((Node)yyVals[-2+yyTop]));
              }
  break;
case 31:
					// line 421 "DefaultRubyParser.y"
  {
                  yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), new SValueNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
              }
  break;
case 32:
					// line 424 "DefaultRubyParser.y"
  {
                  if (((MultipleAsgnNode)yyVals[-2+yyTop]).getHeadNode() != null) {
		      ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(new ToAryNode(getPosition(((MultipleAsgnNode)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
		  } else {
		      ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(support.newArrayNode(getPosition(((MultipleAsgnNode)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
		  }
		  yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
	      }
  break;
case 33:
					// line 432 "DefaultRubyParser.y"
  {
                  ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
		  yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
                  ((MultipleAsgnNode)yyVals[-2+yyTop]).setPosition(support.union(((MultipleAsgnNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
	      }
  break;
case 36:
					// line 441 "DefaultRubyParser.y"
  {
                  yyVal = support.newAndNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 37:
					// line 444 "DefaultRubyParser.y"
  {
                  yyVal = support.newOrNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 38:
					// line 447 "DefaultRubyParser.y"
  {
                  yyVal = new NotNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
              }
  break;
case 39:
					// line 450 "DefaultRubyParser.y"
  {
                  yyVal = new NotNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
              }
  break;
case 41:
					// line 455 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
	      }
  break;
case 44:
					// line 462 "DefaultRubyParser.y"
  {
                  yyVal = new ReturnNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
              }
  break;
case 45:
					// line 465 "DefaultRubyParser.y"
  {
                  yyVal = new BreakNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
              }
  break;
case 46:
					// line 468 "DefaultRubyParser.y"
  {
                  yyVal = new NextNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
              }
  break;
case 48:
					// line 474 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 49:
					// line 477 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 50:
					// line 482 "DefaultRubyParser.y"
  {
                    support.pushBlockScope();
		}
  break;
case 51:
					// line 484 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                    support.popCurrentScope();
		}
  break;
case 52:
					// line 490 "DefaultRubyParser.y"
  {
                  yyVal = support.new_fcall(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 53:
					// line 493 "DefaultRubyParser.y"
  {
                  yyVal = support.new_fcall(((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])); 
              }
  break;
case 54:
					// line 496 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 55:
					// line 499 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])); 
	      }
  break;
case 56:
					// line 502 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 57:
					// line 505 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])); 
	      }
  break;
case 58:
					// line 508 "DefaultRubyParser.y"
  {
		  yyVal = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop])); /* .setPosFrom($2);*/
	      }
  break;
case 59:
					// line 511 "DefaultRubyParser.y"
  {
                  yyVal = support.new_yield(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
	      }
  break;
case 61:
					// line 517 "DefaultRubyParser.y"
  {
                  yyVal = ((MultipleAsgnNode)yyVals[-1+yyTop]);
	      }
  break;
case 63:
					// line 523 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((Token)yyVals[-2+yyTop])), support.newArrayNode(getPosition(((Token)yyVals[-2+yyTop])), ((MultipleAsgnNode)yyVals[-1+yyTop])), null);
              }
  break;
case 64:
					// line 528 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((ListNode)yyVals[0+yyTop])), ((ListNode)yyVals[0+yyTop]), null);
              }
  break;
case 65:
					// line 531 "DefaultRubyParser.y"
  {
/*mirko: check*/
                  yyVal = new MultipleAsgnNode(support.union(((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), ((ListNode)yyVals[-1+yyTop]).add(((Node)yyVals[0+yyTop])), null);
                  ((Node)yyVals[-1+yyTop]).setPosition(support.union(((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])));
              }
  break;
case 66:
					// line 536 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((ListNode)yyVals[-2+yyTop])), ((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 67:
					// line 539 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((ListNode)yyVals[-1+yyTop])), ((ListNode)yyVals[-1+yyTop]), new StarNode(getPosition(null)));
              }
  break;
case 68:
					// line 542 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((Token)yyVals[-1+yyTop])), null, ((Node)yyVals[0+yyTop]));
              }
  break;
case 69:
					// line 545 "DefaultRubyParser.y"
  {
                  yyVal = new MultipleAsgnNode(getPosition(((Token)yyVals[0+yyTop])), null, new StarNode(getPosition(null)));
              }
  break;
case 71:
					// line 550 "DefaultRubyParser.y"
  {
                  yyVal = ((MultipleAsgnNode)yyVals[-1+yyTop]);
              }
  break;
case 72:
					// line 555 "DefaultRubyParser.y"
  {
                  yyVal = support.newArrayNode(((Node)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 73:
					// line 558 "DefaultRubyParser.y"
  {
                  yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
              }
  break;
case 74:
					// line 562 "DefaultRubyParser.y"
  {
                  yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
              }
  break;
case 75:
					// line 565 "DefaultRubyParser.y"
  {
                  yyVal = support.aryset(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 76:
					// line 568 "DefaultRubyParser.y"
  {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 77:
					// line 571 "DefaultRubyParser.y"
  {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 78:
					// line 574 "DefaultRubyParser.y"
  {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 79:
					// line 577 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
		      yyerror("dynamic constant assignment");
		  }

		  ISourcePosition position = support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop]));

                  yyVal = new ConstDeclNode(position, null, support.new_colon2(position, ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
	      }
  break;
case 80:
					// line 586 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
		      yyerror("dynamic constant assignment");
		  }

                  ISourcePosition position = support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop]));

                  yyVal = new ConstDeclNode(position, null, support.new_colon3(position, (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
	      }
  break;
case 81:
					// line 595 "DefaultRubyParser.y"
  {
	          support.backrefAssignError(((Node)yyVals[0+yyTop]));
              }
  break;
case 82:
					// line 600 "DefaultRubyParser.y"
  {
                  yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
              }
  break;
case 83:
					// line 603 "DefaultRubyParser.y"
  {
                  yyVal = support.aryset(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 84:
					// line 606 "DefaultRubyParser.y"
  {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 85:
					// line 609 "DefaultRubyParser.y"
  {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
 	      }
  break;
case 86:
					// line 612 "DefaultRubyParser.y"
  {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 87:
					// line 615 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
		      yyerror("dynamic constant assignment");
		  }
			
		  ISourcePosition position = support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop]));

                  yyVal = new ConstDeclNode(position, null, support.new_colon2(position, ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
              }
  break;
case 88:
					// line 624 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
		      yyerror("dynamic constant assignment");
		  }

                  ISourcePosition position = support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop]));

                  yyVal = new ConstDeclNode(position, null, support.new_colon3(position, (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
	      }
  break;
case 89:
					// line 633 "DefaultRubyParser.y"
  {
                   support.backrefAssignError(((Node)yyVals[0+yyTop]));
	      }
  break;
case 90:
					// line 637 "DefaultRubyParser.y"
  {
                  yyerror("class/module name must be CONSTANT");
              }
  break;
case 92:
					// line 642 "DefaultRubyParser.y"
  {
                  yyVal = support.new_colon3(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
	      }
  break;
case 93:
					// line 645 "DefaultRubyParser.y"
  {
                  yyVal = support.new_colon2(((Token)yyVals[0+yyTop]).getPosition(), null, (String) ((Token)yyVals[0+yyTop]).getValue());
 	      }
  break;
case 94:
					// line 648 "DefaultRubyParser.y"
  {
                  yyVal = support.new_colon2(support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
	      }
  break;
case 98:
					// line 654 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_END);
                  yyVal = ((Token)yyVals[0+yyTop]);
              }
  break;
case 99:
					// line 659 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_END);
                  yyVal = yyVals[0+yyTop];
              }
  break;
case 102:
					// line 666 "DefaultRubyParser.y"
  {
                  yyVal = new UndefNode(getPosition(((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 103:
					// line 669 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_FNAME);
	      }
  break;
case 104:
					// line 671 "DefaultRubyParser.y"
  {
                  yyVal = support.appendToBlock(((Node)yyVals[-3+yyTop]), new UndefNode(getPosition(((Node)yyVals[-3+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue()));
              }
  break;
case 172:
					// line 690 "DefaultRubyParser.y"
  {
                  yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
		  /* FIXME: Consider fixing node_assign itself rather than single case*/
		  ((Node)yyVal).setPosition(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
              }
  break;
case 173:
					// line 695 "DefaultRubyParser.y"
  {
                  ISourcePosition position = support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                  Node body = ((Node)yyVals[0+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop]);
                  yyVal = support.node_assign(((Node)yyVals[-4+yyTop]), new RescueNode(position, ((Node)yyVals[-2+yyTop]), new RescueBodyNode(position, null, body, null), null));
	      }
  break;
case 174:
					// line 700 "DefaultRubyParser.y"
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
					// line 716 "DefaultRubyParser.y"
  {
                  yyVal = support.new_opElementAsgnNode(getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 176:
					// line 719 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 177:
					// line 722 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 178:
					// line 725 "DefaultRubyParser.y"
  {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
              }
  break;
case 179:
					// line 728 "DefaultRubyParser.y"
  {
	          yyerror("constant re-assignment");
	      }
  break;
case 180:
					// line 731 "DefaultRubyParser.y"
  {
		  yyerror("constant re-assignment");
	      }
  break;
case 181:
					// line 734 "DefaultRubyParser.y"
  {
                  support.backrefAssignError(((Node)yyVals[-2+yyTop]));
              }
  break;
case 182:
					// line 737 "DefaultRubyParser.y"
  {
		  support.checkExpression(((Node)yyVals[-2+yyTop]));
		  support.checkExpression(((Node)yyVals[0+yyTop]));
    
                  boolean isLiteral = ((Node)yyVals[-2+yyTop]) instanceof FixnumNode && ((Node)yyVals[0+yyTop]) instanceof FixnumNode;
                  yyVal = new DotNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), false, isLiteral);
              }
  break;
case 183:
					// line 744 "DefaultRubyParser.y"
  {
		  support.checkExpression(((Node)yyVals[-2+yyTop]));
		  support.checkExpression(((Node)yyVals[0+yyTop]));
                  boolean isLiteral = ((Node)yyVals[-2+yyTop]) instanceof FixnumNode && ((Node)yyVals[0+yyTop]) instanceof FixnumNode;
                  yyVal = new DotNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), true, isLiteral);
              }
  break;
case 184:
					// line 750 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "+", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 185:
					// line 753 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "-", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 186:
					// line 756 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "*", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 187:
					// line 759 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "/", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 188:
					// line 762 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "%", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 189:
					// line 765 "DefaultRubyParser.y"
  {
		  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 190:
					// line 768 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), getPosition(null)), "-@");
              }
  break;
case 191:
					// line 771 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((FloatNode)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), getPosition(null)), "-@");
              }
  break;
case 192:
					// line 774 "DefaultRubyParser.y"
  {
                  if (support.isLiteral(((Node)yyVals[0+yyTop]))) {
		      yyVal = ((Node)yyVals[0+yyTop]);
		  } else {
                      yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "+@");
		  }
              }
  break;
case 193:
					// line 781 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "-@");
	      }
  break;
case 194:
					// line 784 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "|", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 195:
					// line 787 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "^", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 196:
					// line 790 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "&", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 197:
					// line 793 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=>", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 198:
					// line 796 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 199:
					// line 799 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">=", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 200:
					// line 802 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 201:
					// line 805 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 202:
					// line 808 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 203:
					// line 811 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "===", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 204:
					// line 814 "DefaultRubyParser.y"
  {
                  yyVal = new NotNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop]), getPosition(null)));
              }
  break;
case 205:
					// line 817 "DefaultRubyParser.y"
  {
                  yyVal = support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 206:
					// line 820 "DefaultRubyParser.y"
  {
                  yyVal = new NotNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
              }
  break;
case 207:
					// line 823 "DefaultRubyParser.y"
  {
                  yyVal = new NotNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
              }
  break;
case 208:
					// line 826 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "~");
              }
  break;
case 209:
					// line 829 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<<", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 210:
					// line 832 "DefaultRubyParser.y"
  {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">>", ((Node)yyVals[0+yyTop]), getPosition(null));
              }
  break;
case 211:
					// line 835 "DefaultRubyParser.y"
  {
                  yyVal = support.newAndNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 212:
					// line 838 "DefaultRubyParser.y"
  {
                  yyVal = support.newOrNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 213:
					// line 841 "DefaultRubyParser.y"
  {
                  yyVal = new DefinedNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop]));
              }
  break;
case 214:
					// line 844 "DefaultRubyParser.y"
  {
                  yyVal = new IfNode(getPosition(((Node)yyVals[-4+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 215:
					// line 847 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 216:
					// line 851 "DefaultRubyParser.y"
  {
	          support.checkExpression(((Node)yyVals[0+yyTop]));
	          yyVal = ((Node)yyVals[0+yyTop]);   
	      }
  break;
case 218:
					// line 857 "DefaultRubyParser.y"
  {
                  yyVal = support.newArrayNode(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 219:
					// line 860 "DefaultRubyParser.y"
  {
                  yyVal = ((ListNode)yyVals[-1+yyTop]);
              }
  break;
case 220:
					// line 863 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 221:
					// line 867 "DefaultRubyParser.y"
  {
                  yyVal = support.newArrayNode(getPosition(((ListNode)yyVals[-1+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
              }
  break;
case 222:
					// line 870 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[-1+yyTop]));
		  yyVal = new NewlineNode(getPosition(((Token)yyVals[-2+yyTop])), support.newSplatNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])));
              }
  break;
case 223:
					// line 875 "DefaultRubyParser.y"
  {
                  yyVal = new ArrayNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
              }
  break;
case 224:
					// line 878 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[-2+yyTop]);
		  ((Node)yyVal).setPosition(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])));
              }
  break;
case 225:
					// line 882 "DefaultRubyParser.y"
  {
                  yyVal = support.newArrayNode(getPosition(((Token)yyVals[-3+yyTop])), ((Node)yyVals[-2+yyTop]));
              }
  break;
case 226:
					// line 885 "DefaultRubyParser.y"
  {
                  yyVal = ((ListNode)yyVals[-4+yyTop]).add(((Node)yyVals[-2+yyTop]));
              }
  break;
case 229:
					// line 892 "DefaultRubyParser.y"
  {
                  yyVal = support.newArrayNode(getPosition(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
              }
  break;
case 230:
					// line 895 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_blk_pass(((ListNode)yyVals[-1+yyTop]), ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 231:
					// line 898 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass(((Node)yyVal), ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 232:
					// line 902 "DefaultRubyParser.y"
  {
                  yyVal = support.newArrayNode(getPosition(((ListNode)yyVals[-1+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 233:
					// line 906 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), support.newArrayNode(getPosition(((ListNode)yyVals[-4+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 234:
					// line 910 "DefaultRubyParser.y"
  {
                  yyVal = ((ListNode)yyVals[-3+yyTop]).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 235:
					// line 914 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[-1+yyTop]));
		  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-6+yyTop])), ((ListNode)yyVals[-6+yyTop]).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 236:
					// line 919 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_blk_pass(support.newSplatNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 237:
					// line 922 "DefaultRubyParser.y"
  {}
  break;
case 238:
					// line 924 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_blk_pass(support.newArrayNode(getPosition(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop])).addAll(((ListNode)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 239:
					// line 927 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_blk_pass(support.newArrayNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
              }
  break;
case 240:
					// line 930 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-4+yyTop])), support.newArrayNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 241:
					// line 934 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-6+yyTop])), support.newArrayNode(getPosition(((Node)yyVals[-6+yyTop])), ((Node)yyVals[-6+yyTop])).addAll(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 242:
					// line 938 "DefaultRubyParser.y"
  {
                  yyVal = support.newArrayNode(getPosition(((ListNode)yyVals[-1+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 243:
					// line 942 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), support.newArrayNode(getPosition(((ListNode)yyVals[-4+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 244:
					// line 946 "DefaultRubyParser.y"
  {
                  yyVal = support.newArrayNode(getPosition(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 245:
					// line 950 "DefaultRubyParser.y"
  {
                  yyVal = support.newArrayNode(getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop])).addAll(((ListNode)yyVals[-3+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 246:
					// line 954 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-6+yyTop])), support.newArrayNode(getPosition(((Node)yyVals[-6+yyTop])), ((Node)yyVals[-6+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 247:
					// line 958 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-8+yyTop])), support.newArrayNode(getPosition(((Node)yyVals[-8+yyTop])), ((Node)yyVals[-8+yyTop])).addAll(((ListNode)yyVals[-6+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 248:
					// line 962 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_blk_pass(support.newSplatNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
	      }
  break;
case 249:
					// line 965 "DefaultRubyParser.y"
  {}
  break;
case 250:
					// line 967 "DefaultRubyParser.y"
  { 
	          yyVal = new Long(lexer.getCmdArgumentState().begin());
	      }
  break;
case 251:
					// line 969 "DefaultRubyParser.y"
  {
                  lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 253:
					// line 975 "DefaultRubyParser.y"
  {                    
		  lexer.setState(LexState.EXPR_ENDARG);
	      }
  break;
case 254:
					// line 977 "DefaultRubyParser.y"
  {
                  warnings.warn(ID.ARGUMENT_EXTRA_SPACE, getPosition(((Token)yyVals[-2+yyTop])), "don't put space before argument parentheses");
	          yyVal = null;
	      }
  break;
case 255:
					// line 981 "DefaultRubyParser.y"
  {
		  lexer.setState(LexState.EXPR_ENDARG);
	      }
  break;
case 256:
					// line 983 "DefaultRubyParser.y"
  {
                  warnings.warn(ID.ARGUMENT_EXTRA_SPACE, getPosition(((Token)yyVals[-3+yyTop])), "don't put space before argument parentheses");
		  yyVal = ((Node)yyVals[-2+yyTop]);
	      }
  break;
case 257:
					// line 988 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
                  yyVal = new BlockPassNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
              }
  break;
case 258:
					// line 993 "DefaultRubyParser.y"
  {
                  yyVal = ((BlockPassNode)yyVals[0+yyTop]);
              }
  break;
case 260:
					// line 998 "DefaultRubyParser.y"
  {
                  yyVal = support.newArrayNode(getPosition2(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
              }
  break;
case 261:
					// line 1001 "DefaultRubyParser.y"
  {
                  yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
              }
  break;
case 262:
					// line 1005 "DefaultRubyParser.y"
  {
		  yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
              }
  break;
case 263:
					// line 1008 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
	      }
  break;
case 264:
					// line 1011 "DefaultRubyParser.y"
  {  
                  yyVal = support.newSplatNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
	      }
  break;
case 273:
					// line 1023 "DefaultRubyParser.y"
  {
                  yyVal = new FCallNoArgNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
	      }
  break;
case 274:
					// line 1026 "DefaultRubyParser.y"
  {
                  yyVal = new BeginNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]));
	      }
  break;
case 275:
					// line 1029 "DefaultRubyParser.y"
  { 
                  lexer.setState(LexState.EXPR_ENDARG); 
              }
  break;
case 276:
					// line 1031 "DefaultRubyParser.y"
  {
		 if (warnings.isVerbose()) warnings.warning(ID.GROUPED_EXPRESSION, getPosition(((Token)yyVals[-4+yyTop])), "(...) interpreted as grouped expression");
                  yyVal = ((Node)yyVals[-3+yyTop]);
	      }
  break;
case 277:
					// line 1035 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-1+yyTop]) != null) {
                      /* compstmt position includes both parens around it*/
                      ((ISourcePositionHolder) ((Node)yyVals[-1+yyTop])).setPosition(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
                  }
		  yyVal = ((Node)yyVals[-1+yyTop]);
              }
  break;
case 278:
					// line 1042 "DefaultRubyParser.y"
  {
                  yyVal = support.new_colon2(support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 279:
					// line 1045 "DefaultRubyParser.y"
  {
                  yyVal = support.new_colon3(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
              }
  break;
case 280:
					// line 1048 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-3+yyTop]) instanceof SelfNode) {
                      yyVal = support.new_fcall(new Token("[]", support.union(((Node)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop]))), ((Node)yyVals[-1+yyTop]), null);
                  } else {
                      yyVal = support.new_call(((Node)yyVals[-3+yyTop]), new Token("[]", support.union(((Node)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop]))), ((Node)yyVals[-1+yyTop]), null);
                  }
              }
  break;
case 281:
					// line 1055 "DefaultRubyParser.y"
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
					// line 1064 "DefaultRubyParser.y"
  {
                  yyVal = new HashNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((ListNode)yyVals[-1+yyTop]));
              }
  break;
case 283:
					// line 1067 "DefaultRubyParser.y"
  {
		  yyVal = new ReturnNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
              }
  break;
case 284:
					// line 1070 "DefaultRubyParser.y"
  {
                  yyVal = support.new_yield(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 285:
					// line 1073 "DefaultRubyParser.y"
  {
                  yyVal = new YieldNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), null, false);
              }
  break;
case 286:
					// line 1076 "DefaultRubyParser.y"
  {
                  yyVal = new YieldNode(((Token)yyVals[0+yyTop]).getPosition(), null, false);
              }
  break;
case 287:
					// line 1079 "DefaultRubyParser.y"
  {
                  yyVal = new DefinedNode(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 288:
					// line 1082 "DefaultRubyParser.y"
  {
                  yyVal = new FCallNoArgBlockNode(support.union(((Token)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((IterNode)yyVals[0+yyTop]));
              }
  break;
case 290:
					// line 1086 "DefaultRubyParser.y"
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
					// line 1094 "DefaultRubyParser.y"
  {
                  yyVal = new IfNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 292:
					// line 1097 "DefaultRubyParser.y"
  {
                  yyVal = new IfNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-2+yyTop]));
              }
  break;
case 293:
					// line 1100 "DefaultRubyParser.y"
  { 
                  lexer.getConditionState().begin();
	      }
  break;
case 294:
					// line 1102 "DefaultRubyParser.y"
  {
		  lexer.getConditionState().end();
	      }
  break;
case 295:
					// line 1104 "DefaultRubyParser.y"
  {
                  Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);
                  yyVal = new WhileNode(support.union(((Token)yyVals[-6+yyTop]), ((Token)yyVals[0+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), body);
              }
  break;
case 296:
					// line 1108 "DefaultRubyParser.y"
  {
                  lexer.getConditionState().begin();
              }
  break;
case 297:
					// line 1110 "DefaultRubyParser.y"
  {
                  lexer.getConditionState().end();
              }
  break;
case 298:
					// line 1112 "DefaultRubyParser.y"
  {
                  Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);
                  yyVal = new UntilNode(getPosition(((Token)yyVals[-6+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), body);
              }
  break;
case 299:
					// line 1116 "DefaultRubyParser.y"
  {
                  yyVal = support.newCaseNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
              }
  break;
case 300:
					// line 1119 "DefaultRubyParser.y"
  {
/* TODO: MRI is just a when node.  We need this extra logic for IDE consumers (null in casenode statement should be implicit nil)*/
/*                  if (support.getConfiguration().hasExtraPositionInformation()) {*/
                      yyVal = support.newCaseNode(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])), null, ((Node)yyVals[-1+yyTop]));
/*                  } else {*/
/*                      $$ = $3;*/
/*                  }*/
              }
  break;
case 301:
					// line 1127 "DefaultRubyParser.y"
  {
		  yyVal = ((Node)yyVals[-1+yyTop]);
              }
  break;
case 302:
					// line 1130 "DefaultRubyParser.y"
  {
                  lexer.getConditionState().begin();
              }
  break;
case 303:
					// line 1132 "DefaultRubyParser.y"
  {
                  lexer.getConditionState().end();
              }
  break;
case 304:
					// line 1134 "DefaultRubyParser.y"
  {
                  yyVal = new ForNode(support.union(((Token)yyVals[-8+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-7+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-4+yyTop]));
              }
  break;
case 305:
					// line 1137 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) {
                      yyerror("class definition in method body");
                  }
		  support.pushLocalScope();
              }
  break;
case 306:
					// line 1142 "DefaultRubyParser.y"
  {
                  Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);

                  yyVal = new ClassNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), ((Colon3Node)yyVals[-4+yyTop]), support.getCurrentScope(), body, ((Node)yyVals[-3+yyTop]));
                  support.popCurrentScope();
              }
  break;
case 307:
					// line 1148 "DefaultRubyParser.y"
  {
                  yyVal = new Boolean(support.isInDef());
                  support.setInDef(false);
              }
  break;
case 308:
					// line 1151 "DefaultRubyParser.y"
  {
                  yyVal = new Integer(support.getInSingle());
                  support.setInSingle(0);
		  support.pushLocalScope();
              }
  break;
case 309:
					// line 1155 "DefaultRubyParser.y"
  {
                  yyVal = new SClassNode(support.union(((Token)yyVals[-7+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-5+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  support.popCurrentScope();
                  support.setInDef(((Boolean)yyVals[-4+yyTop]).booleanValue());
                  support.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
              }
  break;
case 310:
					// line 1161 "DefaultRubyParser.y"
  {
                  if (support.isInDef() || support.isInSingle()) { 
                      yyerror("module definition in method body");
                  }
		  support.pushLocalScope();
              }
  break;
case 311:
					// line 1166 "DefaultRubyParser.y"
  {
                  Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);

                  yyVal = new ModuleNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Colon3Node)yyVals[-3+yyTop]), support.getCurrentScope(), body);
                  support.popCurrentScope();
              }
  break;
case 312:
					// line 1172 "DefaultRubyParser.y"
  {
                  support.setInDef(true);
		  support.pushLocalScope();
              }
  break;
case 313:
					// line 1175 "DefaultRubyParser.y"
  {
                  /* TODO: We should use implicit nil for body, but problem (punt til later)*/
                  Node body = ((Node)yyVals[-1+yyTop]); /*$5 == null ? NilImplicitNode.NIL : $5;*/

                  /* NOEX_PRIVATE for toplevel */
                  yyVal = new DefnNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), new ArgumentNode(((Token)yyVals[-4+yyTop]).getPosition(), (String) ((Token)yyVals[-4+yyTop]).getValue()), ((ArgsNode)yyVals[-2+yyTop]), support.getCurrentScope(), body);
                  support.popCurrentScope();
                  support.setInDef(false);
              }
  break;
case 314:
					// line 1184 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_FNAME);
              }
  break;
case 315:
					// line 1186 "DefaultRubyParser.y"
  {
                  support.setInSingle(support.getInSingle() + 1);
		  support.pushLocalScope();
                  lexer.setState(LexState.EXPR_END); /* force for args */
              }
  break;
case 316:
					// line 1190 "DefaultRubyParser.y"
  {
                  /* TODO: We should use implicit nil for body, but problem (punt til later)*/
                  Node body = ((Node)yyVals[-1+yyTop]); /*$8 == null ? NilImplicitNode.NIL : $8;*/

                  yyVal = new DefsNode(support.union(((Token)yyVals[-8+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-7+yyTop]), new ArgumentNode(((Token)yyVals[-4+yyTop]).getPosition(), (String) ((Token)yyVals[-4+yyTop]).getValue()), ((ArgsNode)yyVals[-2+yyTop]), support.getCurrentScope(), body);
                  support.popCurrentScope();
                  support.setInSingle(support.getInSingle() - 1);
              }
  break;
case 317:
					// line 1198 "DefaultRubyParser.y"
  {
                  yyVal = new BreakNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
              }
  break;
case 318:
					// line 1201 "DefaultRubyParser.y"
  {
                  yyVal = new NextNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
              }
  break;
case 319:
					// line 1204 "DefaultRubyParser.y"
  {
                  yyVal = new RedoNode(((Token)yyVals[0+yyTop]).getPosition());
              }
  break;
case 320:
					// line 1207 "DefaultRubyParser.y"
  {
                  yyVal = new RetryNode(((Token)yyVals[0+yyTop]).getPosition());
              }
  break;
case 321:
					// line 1211 "DefaultRubyParser.y"
  {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
		  yyVal = ((Node)yyVals[0+yyTop]);
	      }
  break;
case 330:
					// line 1226 "DefaultRubyParser.y"
  {
/*mirko: support.union($<ISourcePositionHolder>1.getPosition(), getPosition($<ISourcePositionHolder>1)) ?*/
                  yyVal = new IfNode(getPosition(((Token)yyVals[-4+yyTop])), support.getConditionNode(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 332:
					// line 1232 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 334:
					// line 1237 "DefaultRubyParser.y"
  {}
  break;
case 336:
					// line 1240 "DefaultRubyParser.y"
  {
                  yyVal = new ZeroArgNode(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])));
              }
  break;
case 337:
					// line 1243 "DefaultRubyParser.y"
  {
                  yyVal = new ZeroArgNode(((Token)yyVals[0+yyTop]).getPosition());
	      }
  break;
case 338:
					// line 1246 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[-1+yyTop]);

		  /* Include pipes on multiple arg type*/
                  if (((Node)yyVals[-1+yyTop]) instanceof MultipleAsgnNode) {
		      ((Node)yyVals[-1+yyTop]).setPosition(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
		  } 
              }
  break;
case 339:
					// line 1255 "DefaultRubyParser.y"
  {
                  support.pushBlockScope();
	      }
  break;
case 340:
					// line 1257 "DefaultRubyParser.y"
  {
                  yyVal = new IterNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  support.popCurrentScope();
              }
  break;
case 341:
					// line 1263 "DefaultRubyParser.y"
  {
	          if (((BlockAcceptingNode)yyVals[-1+yyTop]).getIterNode() instanceof BlockPassNode) {
                      throw new SyntaxException(PID.BLOCK_ARG_AND_BLOCK_GIVEN, getPosition(((Node)yyVals[-1+yyTop])), "Both block arg and actual block given.");
                  }
		  yyVal = ((BlockAcceptingNode)yyVals[-1+yyTop]).setIterNode(((IterNode)yyVals[0+yyTop]));
		  ((Node)yyVal).setPosition(support.union(((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])));
              }
  break;
case 342:
					// line 1270 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 343:
					// line 1273 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 344:
					// line 1277 "DefaultRubyParser.y"
  {
                  yyVal = support.new_fcall(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 345:
					// line 1280 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 346:
					// line 1283 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
              }
  break;
case 347:
					// line 1286 "DefaultRubyParser.y"
  {
                  yyVal = support.new_call(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop]), null, null);
              }
  break;
case 348:
					// line 1289 "DefaultRubyParser.y"
  {
                  yyVal = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop]));
              }
  break;
case 349:
					// line 1292 "DefaultRubyParser.y"
  {
                  yyVal = new ZSuperNode(((Token)yyVals[0+yyTop]).getPosition());
              }
  break;
case 350:
					// line 1297 "DefaultRubyParser.y"
  {
                  support.pushBlockScope();
	      }
  break;
case 351:
					// line 1299 "DefaultRubyParser.y"
  {
                  yyVal = new IterNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  support.popCurrentScope();
              }
  break;
case 352:
					// line 1303 "DefaultRubyParser.y"
  {
                  support.pushBlockScope();
	      }
  break;
case 353:
					// line 1305 "DefaultRubyParser.y"
  {
                  yyVal = new IterNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  ((ISourcePositionHolder)yyVals[-5+yyTop]).setPosition(support.union(((ISourcePositionHolder)yyVals[-5+yyTop]), ((ISourcePositionHolder)yyVal)));
                  support.popCurrentScope();
              }
  break;
case 354:
					// line 1311 "DefaultRubyParser.y"
  {
                  yyVal = support.newWhenNode(support.union(((Token)yyVals[-4+yyTop]), support.unwrapNewlineNode(((Node)yyVals[-1+yyTop]))), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 356:
					// line 1316 "DefaultRubyParser.y"
  {
                  yyVal = support.arg_concat(support.union(((ListNode)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 357:
					// line 1319 "DefaultRubyParser.y"
  {
                  yyVal = new SplatNode(support.union(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
              }
  break;
case 360:
					// line 1326 "DefaultRubyParser.y"
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
					// line 1339 "DefaultRubyParser.y"
  {yyVal = null;}
  break;
case 362:
					// line 1341 "DefaultRubyParser.y"
  {
                  yyVal = support.newArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
	      }
  break;
case 365:
					// line 1347 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 367:
					// line 1352 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[0+yyTop]) != null) {
                      yyVal = ((Node)yyVals[0+yyTop]);
                  } else {
                      yyVal = new NilNode(getPosition(null));
                  }
              }
  break;
case 370:
					// line 1363 "DefaultRubyParser.y"
  {
                  /* FIXME: We may be intern'ing more than once.*/
                  yyVal = new SymbolNode(((Token)yyVals[0+yyTop]).getPosition(), ((String) ((Token)yyVals[0+yyTop]).getValue()).intern());
              }
  break;
case 372:
					// line 1370 "DefaultRubyParser.y"
  {
                  yyVal = ((Node)yyVals[0+yyTop]) instanceof EvStrNode ? new DStrNode(getPosition(((Node)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop])) : ((Node)yyVals[0+yyTop]);
	      }
  break;
case 374:
					// line 1376 "DefaultRubyParser.y"
  {
                  yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 375:
					// line 1381 "DefaultRubyParser.y"
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
					// line 1398 "DefaultRubyParser.y"
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
					// line 1415 "DefaultRubyParser.y"
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
					// line 1431 "DefaultRubyParser.y"
  {
                   yyVal = new ZArrayNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
	       }
  break;
case 379:
					// line 1434 "DefaultRubyParser.y"
  {
		   yyVal = ((ListNode)yyVals[-1+yyTop]);
                   ((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
	       }
  break;
case 380:
					// line 1440 "DefaultRubyParser.y"
  {
                   yyVal = new ArrayNode(getPosition(null));
	       }
  break;
case 381:
					// line 1443 "DefaultRubyParser.y"
  {
                   yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]) instanceof EvStrNode ? new DStrNode(getPosition(((ListNode)yyVals[-2+yyTop]))).add(((Node)yyVals[-1+yyTop])) : ((Node)yyVals[-1+yyTop]));
	       }
  break;
case 383:
					// line 1448 "DefaultRubyParser.y"
  {
                   yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
	       }
  break;
case 384:
					// line 1453 "DefaultRubyParser.y"
  {
                   yyVal = new ZArrayNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
	       }
  break;
case 385:
					// line 1456 "DefaultRubyParser.y"
  {
		   yyVal = ((ListNode)yyVals[-1+yyTop]);
                   ((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
	       }
  break;
case 386:
					// line 1462 "DefaultRubyParser.y"
  {
                   yyVal = new ArrayNode(getPosition(null));
	       }
  break;
case 387:
					// line 1465 "DefaultRubyParser.y"
  {
                   yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
	       }
  break;
case 388:
					// line 1470 "DefaultRubyParser.y"
  {
                   yyVal = new StrNode(((Token)yyVals[0+yyTop]).getPosition(), ByteList.create(""));
	       }
  break;
case 389:
					// line 1473 "DefaultRubyParser.y"
  {
                   yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
	       }
  break;
case 390:
					// line 1477 "DefaultRubyParser.y"
  {
		   yyVal = null;
	       }
  break;
case 391:
					// line 1480 "DefaultRubyParser.y"
  {
                   yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
	       }
  break;
case 392:
					// line 1485 "DefaultRubyParser.y"
  {
                   yyVal = ((Node)yyVals[0+yyTop]);
               }
  break;
case 393:
					// line 1488 "DefaultRubyParser.y"
  {
                   yyVal = lexer.getStrTerm();
		   lexer.setStrTerm(null);
		   lexer.setState(LexState.EXPR_BEG);
	       }
  break;
case 394:
					// line 1492 "DefaultRubyParser.y"
  {
		   lexer.setStrTerm(((StrTerm)yyVals[-1+yyTop]));
	           yyVal = new EvStrNode(support.union(((Token)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
	       }
  break;
case 395:
					// line 1496 "DefaultRubyParser.y"
  {
		   yyVal = lexer.getStrTerm();
		   lexer.setStrTerm(null);
		   lexer.setState(LexState.EXPR_BEG);
	       }
  break;
case 396:
					// line 1500 "DefaultRubyParser.y"
  {
		   lexer.setStrTerm(((StrTerm)yyVals[-2+yyTop]));

		   yyVal = support.newEvStrNode(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-1+yyTop]));
	       }
  break;
case 397:
					// line 1506 "DefaultRubyParser.y"
  {
                   yyVal = new GlobalVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
               }
  break;
case 398:
					// line 1509 "DefaultRubyParser.y"
  {
                   yyVal = new InstVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
               }
  break;
case 399:
					// line 1512 "DefaultRubyParser.y"
  {
                   yyVal = new ClassVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
               }
  break;
case 401:
					// line 1519 "DefaultRubyParser.y"
  {
                   lexer.setState(LexState.EXPR_END);
                   yyVal = ((Token)yyVals[0+yyTop]);
		   ((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])));
               }
  break;
case 406:
					// line 1529 "DefaultRubyParser.y"
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
					// line 1556 "DefaultRubyParser.y"
  {
                   yyVal = ((FloatNode)yyVals[0+yyTop]);
               }
  break;
case 409:
					// line 1559 "DefaultRubyParser.y"
  {
                   yyVal = support.negateInteger(((Node)yyVals[0+yyTop]));
	       }
  break;
case 410:
					// line 1562 "DefaultRubyParser.y"
  {
                   yyVal = support.negateFloat(((FloatNode)yyVals[0+yyTop]));
	       }
  break;
case 416:
					// line 1568 "DefaultRubyParser.y"
  { 
                   yyVal = new Token("nil", Tokens.kNIL, ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 417:
					// line 1571 "DefaultRubyParser.y"
  {
                   yyVal = new Token("self", Tokens.kSELF, ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 418:
					// line 1574 "DefaultRubyParser.y"
  { 
                   yyVal = new Token("true", Tokens.kTRUE, ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 419:
					// line 1577 "DefaultRubyParser.y"
  {
                   yyVal = new Token("false", Tokens.kFALSE, ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 420:
					// line 1580 "DefaultRubyParser.y"
  {
                   yyVal = new Token("__FILE__", Tokens.k__FILE__, ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 421:
					// line 1583 "DefaultRubyParser.y"
  {
                   yyVal = new Token("__LINE__", Tokens.k__LINE__, ((Token)yyVals[0+yyTop]).getPosition());
               }
  break;
case 422:
					// line 1588 "DefaultRubyParser.y"
  {
                   yyVal = support.gettable(((Token)yyVals[0+yyTop]));
               }
  break;
case 423:
					// line 1593 "DefaultRubyParser.y"
  {
                   yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
               }
  break;
case 426:
					// line 1601 "DefaultRubyParser.y"
  {
                   yyVal = null;
               }
  break;
case 427:
					// line 1604 "DefaultRubyParser.y"
  {
                   lexer.setState(LexState.EXPR_BEG);
               }
  break;
case 428:
					// line 1606 "DefaultRubyParser.y"
  {
                   yyVal = ((Node)yyVals[-1+yyTop]);
               }
  break;
case 429:
					// line 1609 "DefaultRubyParser.y"
  {
                   yyerrok();
                   yyVal = null;
               }
  break;
case 430:
					// line 1615 "DefaultRubyParser.y"
  {
                   yyVal = ((Node)yyVals[-2+yyTop]);
                   ((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-3+yyTop]), ((Token)yyVals[0+yyTop])));
                   lexer.setState(LexState.EXPR_BEG);
               }
  break;
case 431:
					// line 1620 "DefaultRubyParser.y"
  {
                   yyVal = ((Node)yyVals[-1+yyTop]);
               }
  break;
case 432:
					// line 1625 "DefaultRubyParser.y"
  {
                   yyVal = support.new_args(support.union(((ListNode)yyVals[-5+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 433:
					// line 1628 "DefaultRubyParser.y"
  {
                   yyVal = support.new_args(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), null, null, ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 434:
					// line 1631 "DefaultRubyParser.y"
  {
                   yyVal = support.new_args(support.union(((ListNode)yyVals[-3+yyTop]), ((BlockArgNode)yyVals[0+yyTop])), ((ListNode)yyVals[-3+yyTop]), null, ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 435:
					// line 1634 "DefaultRubyParser.y"
  {
                   yyVal = support.new_args(((ISourcePositionHolder)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, null, null, ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 436:
					// line 1637 "DefaultRubyParser.y"
  {
                   yyVal = support.new_args(getPosition(((ListNode)yyVals[-3+yyTop])), null, ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 437:
					// line 1640 "DefaultRubyParser.y"
  {
                   yyVal = support.new_args(getPosition(((ListNode)yyVals[-1+yyTop])), null, ((ListNode)yyVals[-1+yyTop]), null, null, ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 438:
					// line 1643 "DefaultRubyParser.y"
  {
                   yyVal = support.new_args(getPosition(((RestArgNode)yyVals[-1+yyTop])), null, null, ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 439:
					// line 1646 "DefaultRubyParser.y"
  {
                   yyVal = support.new_args(getPosition(((BlockArgNode)yyVals[0+yyTop])), null, null, null, null, ((BlockArgNode)yyVals[0+yyTop]));
               }
  break;
case 440:
					// line 1649 "DefaultRubyParser.y"
  {
                   yyVal = support.new_args(support.createEmptyArgsNodePosition(getPosition(null)), null, null, null, null, null);
               }
  break;
case 441:
					// line 1654 "DefaultRubyParser.y"
  {
                   yyerror("formal argument cannot be a constant");
               }
  break;
case 442:
					// line 1657 "DefaultRubyParser.y"
  {
                   yyerror("formal argument cannot be a instance variable");
               }
  break;
case 443:
					// line 1660 "DefaultRubyParser.y"
  {
                   yyerror("formal argument cannot be an global variable");
               }
  break;
case 444:
					// line 1663 "DefaultRubyParser.y"
  {
                   yyerror("formal argument cannot be a class variable");
               }
  break;
case 445:
					// line 1666 "DefaultRubyParser.y"
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
					// line 1677 "DefaultRubyParser.y"
  {
                    support.allowDubyExtension(((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition());
                    yyVal = new ListNode(((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition());
                    ((ListNode) yyVal).add(new TypedArgumentNode(((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition(), (String) ((Token)yyVals[-2+yyTop]).getValue(), ((Node)yyVals[0+yyTop])));
               }
  break;
case 447:
					// line 1682 "DefaultRubyParser.y"
  {
                    yyVal = new ListNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
                    ((ListNode) yyVal).add(new ArgumentNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue()));
               }
  break;
case 448:
					// line 1686 "DefaultRubyParser.y"
  {
                   support.allowDubyExtension(((ISourcePositionHolder)yyVals[-4+yyTop]).getPosition());
                   ((ListNode)yyVals[-4+yyTop]).add(new TypedArgumentNode(((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition(), (String) ((Token)yyVals[-2+yyTop]).getValue(), ((Node)yyVals[0+yyTop])));
                   ((ListNode)yyVals[-4+yyTop]).setPosition(support.union(((ListNode)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop])));
		   yyVal = ((ListNode)yyVals[-4+yyTop]);
               }
  break;
case 449:
					// line 1692 "DefaultRubyParser.y"
  {
                   ((ListNode)yyVals[-2+yyTop]).add(new ArgumentNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue()));
                   ((ListNode)yyVals[-2+yyTop]).setPosition(support.union(((ListNode)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
		   yyVal = ((ListNode)yyVals[-2+yyTop]);
               }
  break;
case 450:
					// line 1699 "DefaultRubyParser.y"
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
					// line 1710 "DefaultRubyParser.y"
  {
                  yyVal = new BlockNode(getPosition(((Node)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
              }
  break;
case 452:
					// line 1713 "DefaultRubyParser.y"
  {
                  yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
              }
  break;
case 455:
					// line 1721 "DefaultRubyParser.y"
  {
                  String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();

                  if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                      yyerror("duplicate rest argument name");
                  }

                  yyVal = new RestArgNode(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue(), support.getCurrentScope().getLocalScope().addVariable(identifier));
              }
  break;
case 456:
					// line 1730 "DefaultRubyParser.y"
  {
                  yyVal = new UnnamedRestArgNode(((Token)yyVals[0+yyTop]).getPosition(), support.getCurrentScope().getLocalScope().addVariable("*"));
              }
  break;
case 459:
					// line 1738 "DefaultRubyParser.y"
  {
                  String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();

                  if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                      yyerror("duplicate block argument name");
                  }
                  yyVal = new BlockArgNode(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), support.getCurrentScope().getLocalScope().addVariable(identifier), identifier);
              }
  break;
case 460:
					// line 1747 "DefaultRubyParser.y"
  {
                  yyVal = ((BlockArgNode)yyVals[0+yyTop]);
              }
  break;
case 461:
					// line 1750 "DefaultRubyParser.y"
  {
	          yyVal = null;
	      }
  break;
case 462:
					// line 1754 "DefaultRubyParser.y"
  {
                  if (!(((Node)yyVals[0+yyTop]) instanceof SelfNode)) {
		      support.checkExpression(((Node)yyVals[0+yyTop]));
		  }
		  yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 463:
					// line 1760 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_BEG);
              }
  break;
case 464:
					// line 1762 "DefaultRubyParser.y"
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
					// line 1774 "DefaultRubyParser.y"
  { /* [!null]*/
                  yyVal = new ArrayNode(getPosition(null));
              }
  break;
case 466:
					// line 1777 "DefaultRubyParser.y"
  { /* [!null]*/
                  yyVal = ((ListNode)yyVals[-1+yyTop]);
              }
  break;
case 467:
					// line 1780 "DefaultRubyParser.y"
  {
                  if (((ListNode)yyVals[-1+yyTop]).size() % 2 != 0) {
                      yyerror("odd number list for Hash.");
                  }
                  yyVal = ((ListNode)yyVals[-1+yyTop]);
              }
  break;
case 469:
					// line 1789 "DefaultRubyParser.y"
  { /* [!null]*/
                  yyVal = ((ListNode)yyVals[-2+yyTop]).addAll(((ListNode)yyVals[0+yyTop]));
              }
  break;
case 470:
					// line 1794 "DefaultRubyParser.y"
  { /* [!null]*/
                  ISourcePosition position;
                  if (((Node)yyVals[-2+yyTop]) == null && ((Node)yyVals[0+yyTop]) == null) {
                      position = getPosition(((Token)yyVals[-1+yyTop]));
                  } else {
                      position = support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                  }

                  yyVal = support.newArrayNode(position, ((Node)yyVals[-2+yyTop])).add(((Node)yyVals[0+yyTop]));
              }
  break;
case 490:
					// line 1813 "DefaultRubyParser.y"
  {
                  yyerrok();
              }
  break;
case 493:
					// line 1819 "DefaultRubyParser.y"
  {
                  yyerrok();
              }
  break;
case 494:
					// line 1823 "DefaultRubyParser.y"
  {
                  yyVal = null;
              }
  break;
case 495:
					// line 1827 "DefaultRubyParser.y"
  {  
                  yyVal = null;
	      }
  break;
					// line 7626 "-"
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

					// line 1832 "DefaultRubyParser.y"

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
					// line 7707 "-"
