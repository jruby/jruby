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
    "fitem : fname",
    "fitem : symbol",
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
    "primary : kFOR block_var kIN $$14 expr_value do $$15 compstmt kEND",
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
    "block_var : lhs",
    "block_var : mlhs",
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
case 1: yyVal = case1_line269(yyVal, yyVals, yyTop); // line 269
break;
case 2: yyVal = case2_line272(yyVal, yyVals, yyTop); // line 272
break;
case 3: yyVal = case3_line284(yyVal, yyVals, yyTop); // line 284
break;
case 4: yyVal = case4_line301(yyVal, yyVals, yyTop); // line 301
break;
case 6: yyVal = case6_line309(yyVal, yyVals, yyTop); // line 309
break;
case 7: yyVal = case7_line312(yyVal, yyVals, yyTop); // line 312
break;
case 8: yyVal = case8_line315(yyVal, yyVals, yyTop); // line 315
break;
case 9: yyVal = case9_line319(yyVal, yyVals, yyTop); // line 319
break;
case 10: yyVal = case10_line321(yyVal, yyVals, yyTop); // line 321
break;
case 11: yyVal = case11_line324(yyVal, yyVals, yyTop); // line 324
break;
case 12: yyVal = case12_line327(yyVal, yyVals, yyTop); // line 327
break;
case 13: yyVal = case13_line330(yyVal, yyVals, yyTop); // line 330
break;
case 14: yyVal = case14_line333(yyVal, yyVals, yyTop); // line 333
break;
case 15: yyVal = case15_line336(yyVal, yyVals, yyTop); // line 336
break;
case 16: yyVal = case16_line339(yyVal, yyVals, yyTop); // line 339
break;
case 17: yyVal = case17_line342(yyVal, yyVals, yyTop); // line 342
break;
case 18: yyVal = case18_line349(yyVal, yyVals, yyTop); // line 349
break;
case 19: yyVal = case19_line356(yyVal, yyVals, yyTop); // line 356
break;
case 20: yyVal = case20_line360(yyVal, yyVals, yyTop); // line 360
break;
case 21: yyVal = case21_line365(yyVal, yyVals, yyTop); // line 365
break;
case 22: yyVal = case22_line370(yyVal, yyVals, yyTop); // line 370
break;
case 23: yyVal = case23_line376(yyVal, yyVals, yyTop); // line 376
break;
case 24: yyVal = case24_line380(yyVal, yyVals, yyTop); // line 380
break;
case 25: yyVal = case25_line389(yyVal, yyVals, yyTop); // line 389
break;
case 26: yyVal = case26_line405(yyVal, yyVals, yyTop); // line 405
break;
case 27: yyVal = case27_line409(yyVal, yyVals, yyTop); // line 409
break;
case 28: yyVal = case28_line412(yyVal, yyVals, yyTop); // line 412
break;
case 29: yyVal = case29_line415(yyVal, yyVals, yyTop); // line 415
break;
case 30: yyVal = case30_line418(yyVal, yyVals, yyTop); // line 418
break;
case 31: yyVal = case31_line421(yyVal, yyVals, yyTop); // line 421
break;
case 32: yyVal = case32_line424(yyVal, yyVals, yyTop); // line 424
break;
case 33: yyVal = case33_line432(yyVal, yyVals, yyTop); // line 432
break;
case 36: yyVal = case36_line441(yyVal, yyVals, yyTop); // line 441
break;
case 37: yyVal = case37_line444(yyVal, yyVals, yyTop); // line 444
break;
case 38: yyVal = case38_line447(yyVal, yyVals, yyTop); // line 447
break;
case 39: yyVal = case39_line450(yyVal, yyVals, yyTop); // line 450
break;
case 41: yyVal = case41_line455(yyVal, yyVals, yyTop); // line 455
break;
case 44: yyVal = case44_line462(yyVal, yyVals, yyTop); // line 462
break;
case 45: yyVal = case45_line465(yyVal, yyVals, yyTop); // line 465
break;
case 46: yyVal = case46_line468(yyVal, yyVals, yyTop); // line 468
break;
case 48: yyVal = case48_line474(yyVal, yyVals, yyTop); // line 474
break;
case 49: yyVal = case49_line477(yyVal, yyVals, yyTop); // line 477
break;
case 50: yyVal = case50_line482(yyVal, yyVals, yyTop); // line 482
break;
case 51: yyVal = case51_line484(yyVal, yyVals, yyTop); // line 484
break;
case 52: yyVal = case52_line490(yyVal, yyVals, yyTop); // line 490
break;
case 53: yyVal = case53_line493(yyVal, yyVals, yyTop); // line 493
break;
case 54: yyVal = case54_line496(yyVal, yyVals, yyTop); // line 496
break;
case 55: yyVal = case55_line499(yyVal, yyVals, yyTop); // line 499
break;
case 56: yyVal = case56_line502(yyVal, yyVals, yyTop); // line 502
break;
case 57: yyVal = case57_line505(yyVal, yyVals, yyTop); // line 505
break;
case 58: yyVal = case58_line508(yyVal, yyVals, yyTop); // line 508
break;
case 59: yyVal = case59_line511(yyVal, yyVals, yyTop); // line 511
break;
case 61: yyVal = case61_line517(yyVal, yyVals, yyTop); // line 517
break;
case 63: yyVal = case63_line523(yyVal, yyVals, yyTop); // line 523
break;
case 64: yyVal = case64_line528(yyVal, yyVals, yyTop); // line 528
break;
case 65: yyVal = case65_line531(yyVal, yyVals, yyTop); // line 531
break;
case 66: yyVal = case66_line535(yyVal, yyVals, yyTop); // line 535
break;
case 67: yyVal = case67_line538(yyVal, yyVals, yyTop); // line 538
break;
case 68: yyVal = case68_line541(yyVal, yyVals, yyTop); // line 541
break;
case 69: yyVal = case69_line544(yyVal, yyVals, yyTop); // line 544
break;
case 71: yyVal = case71_line549(yyVal, yyVals, yyTop); // line 549
break;
case 72: yyVal = case72_line554(yyVal, yyVals, yyTop); // line 554
break;
case 73: yyVal = case73_line557(yyVal, yyVals, yyTop); // line 557
break;
case 74: yyVal = case74_line561(yyVal, yyVals, yyTop); // line 561
break;
case 75: yyVal = case75_line564(yyVal, yyVals, yyTop); // line 564
break;
case 76: yyVal = case76_line567(yyVal, yyVals, yyTop); // line 567
break;
case 77: yyVal = case77_line570(yyVal, yyVals, yyTop); // line 570
break;
case 78: yyVal = case78_line573(yyVal, yyVals, yyTop); // line 573
break;
case 79: yyVal = case79_line576(yyVal, yyVals, yyTop); // line 576
break;
case 80: yyVal = case80_line585(yyVal, yyVals, yyTop); // line 585
break;
case 81: yyVal = case81_line594(yyVal, yyVals, yyTop); // line 594
break;
case 82: yyVal = case82_line599(yyVal, yyVals, yyTop); // line 599
break;
case 83: yyVal = case83_line602(yyVal, yyVals, yyTop); // line 602
break;
case 84: yyVal = case84_line605(yyVal, yyVals, yyTop); // line 605
break;
case 85: yyVal = case85_line608(yyVal, yyVals, yyTop); // line 608
break;
case 86: yyVal = case86_line611(yyVal, yyVals, yyTop); // line 611
break;
case 87: yyVal = case87_line614(yyVal, yyVals, yyTop); // line 614
break;
case 88: yyVal = case88_line623(yyVal, yyVals, yyTop); // line 623
break;
case 89: yyVal = case89_line632(yyVal, yyVals, yyTop); // line 632
break;
case 90: yyVal = case90_line636(yyVal, yyVals, yyTop); // line 636
break;
case 92: yyVal = case92_line641(yyVal, yyVals, yyTop); // line 641
break;
case 93: yyVal = case93_line644(yyVal, yyVals, yyTop); // line 644
break;
case 94: yyVal = case94_line647(yyVal, yyVals, yyTop); // line 647
break;
case 98: yyVal = case98_line653(yyVal, yyVals, yyTop); // line 653
break;
case 99: yyVal = case99_line658(yyVal, yyVals, yyTop); // line 658
break;
case 102: yyVal = case102_line665(yyVal, yyVals, yyTop); // line 665
break;
case 103: yyVal = case103_line668(yyVal, yyVals, yyTop); // line 668
break;
case 104: yyVal = case104_line670(yyVal, yyVals, yyTop); // line 670
break;
case 172: yyVal = case172_line689(yyVal, yyVals, yyTop); // line 689
break;
case 173: yyVal = case173_line694(yyVal, yyVals, yyTop); // line 694
break;
case 174: yyVal = case174_line699(yyVal, yyVals, yyTop); // line 699
break;
case 175: yyVal = case175_line715(yyVal, yyVals, yyTop); // line 715
break;
case 176: yyVal = case176_line718(yyVal, yyVals, yyTop); // line 718
break;
case 177: yyVal = case177_line721(yyVal, yyVals, yyTop); // line 721
break;
case 178: yyVal = case178_line724(yyVal, yyVals, yyTop); // line 724
break;
case 179: yyVal = case179_line727(yyVal, yyVals, yyTop); // line 727
break;
case 180: yyVal = case180_line730(yyVal, yyVals, yyTop); // line 730
break;
case 181: yyVal = case181_line733(yyVal, yyVals, yyTop); // line 733
break;
case 182: yyVal = case182_line736(yyVal, yyVals, yyTop); // line 736
break;
case 183: yyVal = case183_line743(yyVal, yyVals, yyTop); // line 743
break;
case 184: yyVal = case184_line749(yyVal, yyVals, yyTop); // line 749
break;
case 185: yyVal = case185_line752(yyVal, yyVals, yyTop); // line 752
break;
case 186: yyVal = case186_line755(yyVal, yyVals, yyTop); // line 755
break;
case 187: yyVal = case187_line758(yyVal, yyVals, yyTop); // line 758
break;
case 188: yyVal = case188_line761(yyVal, yyVals, yyTop); // line 761
break;
case 189: yyVal = case189_line764(yyVal, yyVals, yyTop); // line 764
break;
case 190: yyVal = case190_line767(yyVal, yyVals, yyTop); // line 767
break;
case 191: yyVal = case191_line770(yyVal, yyVals, yyTop); // line 770
break;
case 192: yyVal = case192_line773(yyVal, yyVals, yyTop); // line 773
break;
case 193: yyVal = case193_line780(yyVal, yyVals, yyTop); // line 780
break;
case 194: yyVal = case194_line783(yyVal, yyVals, yyTop); // line 783
break;
case 195: yyVal = case195_line786(yyVal, yyVals, yyTop); // line 786
break;
case 196: yyVal = case196_line789(yyVal, yyVals, yyTop); // line 789
break;
case 197: yyVal = case197_line792(yyVal, yyVals, yyTop); // line 792
break;
case 198: yyVal = case198_line795(yyVal, yyVals, yyTop); // line 795
break;
case 199: yyVal = case199_line798(yyVal, yyVals, yyTop); // line 798
break;
case 200: yyVal = case200_line801(yyVal, yyVals, yyTop); // line 801
break;
case 201: yyVal = case201_line804(yyVal, yyVals, yyTop); // line 804
break;
case 202: yyVal = case202_line807(yyVal, yyVals, yyTop); // line 807
break;
case 203: yyVal = case203_line810(yyVal, yyVals, yyTop); // line 810
break;
case 204: yyVal = case204_line813(yyVal, yyVals, yyTop); // line 813
break;
case 205: yyVal = case205_line816(yyVal, yyVals, yyTop); // line 816
break;
case 206: yyVal = case206_line819(yyVal, yyVals, yyTop); // line 819
break;
case 207: yyVal = case207_line822(yyVal, yyVals, yyTop); // line 822
break;
case 208: yyVal = case208_line825(yyVal, yyVals, yyTop); // line 825
break;
case 209: yyVal = case209_line828(yyVal, yyVals, yyTop); // line 828
break;
case 210: yyVal = case210_line831(yyVal, yyVals, yyTop); // line 831
break;
case 211: yyVal = case211_line834(yyVal, yyVals, yyTop); // line 834
break;
case 212: yyVal = case212_line837(yyVal, yyVals, yyTop); // line 837
break;
case 213: yyVal = case213_line840(yyVal, yyVals, yyTop); // line 840
break;
case 214: yyVal = case214_line843(yyVal, yyVals, yyTop); // line 843
break;
case 215: yyVal = case215_line846(yyVal, yyVals, yyTop); // line 846
break;
case 216: yyVal = case216_line850(yyVal, yyVals, yyTop); // line 850
break;
case 218: yyVal = case218_line856(yyVal, yyVals, yyTop); // line 856
break;
case 219: yyVal = case219_line859(yyVal, yyVals, yyTop); // line 859
break;
case 220: yyVal = case220_line862(yyVal, yyVals, yyTop); // line 862
break;
case 221: yyVal = case221_line866(yyVal, yyVals, yyTop); // line 866
break;
case 222: yyVal = case222_line869(yyVal, yyVals, yyTop); // line 869
break;
case 223: yyVal = case223_line874(yyVal, yyVals, yyTop); // line 874
break;
case 224: yyVal = case224_line877(yyVal, yyVals, yyTop); // line 877
break;
case 225: yyVal = case225_line881(yyVal, yyVals, yyTop); // line 881
break;
case 226: yyVal = case226_line884(yyVal, yyVals, yyTop); // line 884
break;
case 229: yyVal = case229_line891(yyVal, yyVals, yyTop); // line 891
break;
case 230: yyVal = case230_line894(yyVal, yyVals, yyTop); // line 894
break;
case 231: yyVal = case231_line897(yyVal, yyVals, yyTop); // line 897
break;
case 232: yyVal = case232_line901(yyVal, yyVals, yyTop); // line 901
break;
case 233: yyVal = case233_line905(yyVal, yyVals, yyTop); // line 905
break;
case 234: yyVal = case234_line909(yyVal, yyVals, yyTop); // line 909
break;
case 235: yyVal = case235_line913(yyVal, yyVals, yyTop); // line 913
break;
case 236: yyVal = case236_line918(yyVal, yyVals, yyTop); // line 918
break;
case 237: yyVal = case237_line921(yyVal, yyVals, yyTop); // line 921
break;
case 238: yyVal = case238_line924(yyVal, yyVals, yyTop); // line 924
break;
case 239: yyVal = case239_line927(yyVal, yyVals, yyTop); // line 927
break;
case 240: yyVal = case240_line930(yyVal, yyVals, yyTop); // line 930
break;
case 241: yyVal = case241_line934(yyVal, yyVals, yyTop); // line 934
break;
case 242: yyVal = case242_line938(yyVal, yyVals, yyTop); // line 938
break;
case 243: yyVal = case243_line942(yyVal, yyVals, yyTop); // line 942
break;
case 244: yyVal = case244_line946(yyVal, yyVals, yyTop); // line 946
break;
case 245: yyVal = case245_line950(yyVal, yyVals, yyTop); // line 950
break;
case 246: yyVal = case246_line954(yyVal, yyVals, yyTop); // line 954
break;
case 247: yyVal = case247_line958(yyVal, yyVals, yyTop); // line 958
break;
case 248: yyVal = case248_line962(yyVal, yyVals, yyTop); // line 962
break;
case 249: yyVal = case249_line965(yyVal, yyVals, yyTop); // line 965
break;
case 250: yyVal = case250_line968(yyVal, yyVals, yyTop); // line 968
break;
case 251: yyVal = case251_line970(yyVal, yyVals, yyTop); // line 970
break;
case 253: yyVal = case253_line976(yyVal, yyVals, yyTop); // line 976
break;
case 254: yyVal = case254_line978(yyVal, yyVals, yyTop); // line 978
break;
case 255: yyVal = case255_line982(yyVal, yyVals, yyTop); // line 982
break;
case 256: yyVal = case256_line984(yyVal, yyVals, yyTop); // line 984
break;
case 257: yyVal = case257_line989(yyVal, yyVals, yyTop); // line 989
break;
case 258: yyVal = case258_line994(yyVal, yyVals, yyTop); // line 994
break;
case 260: yyVal = case260_line999(yyVal, yyVals, yyTop); // line 999
break;
case 261: yyVal = case261_line1002(yyVal, yyVals, yyTop); // line 1002
break;
case 262: yyVal = case262_line1006(yyVal, yyVals, yyTop); // line 1006
break;
case 263: yyVal = case263_line1009(yyVal, yyVals, yyTop); // line 1009
break;
case 264: yyVal = case264_line1012(yyVal, yyVals, yyTop); // line 1012
break;
case 273: yyVal = case273_line1024(yyVal, yyVals, yyTop); // line 1024
break;
case 274: yyVal = case274_line1027(yyVal, yyVals, yyTop); // line 1027
break;
case 275: yyVal = case275_line1030(yyVal, yyVals, yyTop); // line 1030
break;
case 276: yyVal = case276_line1032(yyVal, yyVals, yyTop); // line 1032
break;
case 277: yyVal = case277_line1036(yyVal, yyVals, yyTop); // line 1036
break;
case 278: yyVal = case278_line1043(yyVal, yyVals, yyTop); // line 1043
break;
case 279: yyVal = case279_line1046(yyVal, yyVals, yyTop); // line 1046
break;
case 280: yyVal = case280_line1049(yyVal, yyVals, yyTop); // line 1049
break;
case 281: yyVal = case281_line1056(yyVal, yyVals, yyTop); // line 1056
break;
case 282: yyVal = case282_line1065(yyVal, yyVals, yyTop); // line 1065
break;
case 283: yyVal = case283_line1068(yyVal, yyVals, yyTop); // line 1068
break;
case 284: yyVal = case284_line1071(yyVal, yyVals, yyTop); // line 1071
break;
case 285: yyVal = case285_line1074(yyVal, yyVals, yyTop); // line 1074
break;
case 286: yyVal = case286_line1077(yyVal, yyVals, yyTop); // line 1077
break;
case 287: yyVal = case287_line1080(yyVal, yyVals, yyTop); // line 1080
break;
case 288: yyVal = case288_line1083(yyVal, yyVals, yyTop); // line 1083
break;
case 290: yyVal = case290_line1087(yyVal, yyVals, yyTop); // line 1087
break;
case 291: yyVal = case291_line1095(yyVal, yyVals, yyTop); // line 1095
break;
case 292: yyVal = case292_line1098(yyVal, yyVals, yyTop); // line 1098
break;
case 293: yyVal = case293_line1101(yyVal, yyVals, yyTop); // line 1101
break;
case 294: yyVal = case294_line1103(yyVal, yyVals, yyTop); // line 1103
break;
case 295: yyVal = case295_line1105(yyVal, yyVals, yyTop); // line 1105
break;
case 296: yyVal = case296_line1109(yyVal, yyVals, yyTop); // line 1109
break;
case 297: yyVal = case297_line1111(yyVal, yyVals, yyTop); // line 1111
break;
case 298: yyVal = case298_line1113(yyVal, yyVals, yyTop); // line 1113
break;
case 299: yyVal = case299_line1117(yyVal, yyVals, yyTop); // line 1117
break;
case 300: yyVal = case300_line1120(yyVal, yyVals, yyTop); // line 1120
break;
case 301: yyVal = case301_line1128(yyVal, yyVals, yyTop); // line 1128
break;
case 302: yyVal = case302_line1131(yyVal, yyVals, yyTop); // line 1131
break;
case 303: yyVal = case303_line1133(yyVal, yyVals, yyTop); // line 1133
break;
case 304: yyVal = case304_line1135(yyVal, yyVals, yyTop); // line 1135
break;
case 305: yyVal = case305_line1138(yyVal, yyVals, yyTop); // line 1138
break;
case 306: yyVal = case306_line1143(yyVal, yyVals, yyTop); // line 1143
break;
case 307: yyVal = case307_line1149(yyVal, yyVals, yyTop); // line 1149
break;
case 308: yyVal = case308_line1152(yyVal, yyVals, yyTop); // line 1152
break;
case 309: yyVal = case309_line1156(yyVal, yyVals, yyTop); // line 1156
break;
case 310: yyVal = case310_line1162(yyVal, yyVals, yyTop); // line 1162
break;
case 311: yyVal = case311_line1167(yyVal, yyVals, yyTop); // line 1167
break;
case 312: yyVal = case312_line1173(yyVal, yyVals, yyTop); // line 1173
break;
case 313: yyVal = case313_line1176(yyVal, yyVals, yyTop); // line 1176
break;
case 314: yyVal = case314_line1185(yyVal, yyVals, yyTop); // line 1185
break;
case 315: yyVal = case315_line1187(yyVal, yyVals, yyTop); // line 1187
break;
case 316: yyVal = case316_line1191(yyVal, yyVals, yyTop); // line 1191
break;
case 317: yyVal = case317_line1199(yyVal, yyVals, yyTop); // line 1199
break;
case 318: yyVal = case318_line1202(yyVal, yyVals, yyTop); // line 1202
break;
case 319: yyVal = case319_line1205(yyVal, yyVals, yyTop); // line 1205
break;
case 320: yyVal = case320_line1208(yyVal, yyVals, yyTop); // line 1208
break;
case 321: yyVal = case321_line1212(yyVal, yyVals, yyTop); // line 1212
break;
case 330: yyVal = case330_line1227(yyVal, yyVals, yyTop); // line 1227
break;
case 332: yyVal = case332_line1232(yyVal, yyVals, yyTop); // line 1232
break;
case 334: yyVal = case334_line1237(yyVal, yyVals, yyTop); // line 1237
break;
case 336: yyVal = case336_line1241(yyVal, yyVals, yyTop); // line 1241
break;
case 337: yyVal = case337_line1244(yyVal, yyVals, yyTop); // line 1244
break;
case 338: yyVal = case338_line1247(yyVal, yyVals, yyTop); // line 1247
break;
case 339: yyVal = case339_line1256(yyVal, yyVals, yyTop); // line 1256
break;
case 340: yyVal = case340_line1258(yyVal, yyVals, yyTop); // line 1258
break;
case 341: yyVal = case341_line1264(yyVal, yyVals, yyTop); // line 1264
break;
case 342: yyVal = case342_line1275(yyVal, yyVals, yyTop); // line 1275
break;
case 343: yyVal = case343_line1278(yyVal, yyVals, yyTop); // line 1278
break;
case 344: yyVal = case344_line1282(yyVal, yyVals, yyTop); // line 1282
break;
case 345: yyVal = case345_line1285(yyVal, yyVals, yyTop); // line 1285
break;
case 346: yyVal = case346_line1288(yyVal, yyVals, yyTop); // line 1288
break;
case 347: yyVal = case347_line1291(yyVal, yyVals, yyTop); // line 1291
break;
case 348: yyVal = case348_line1294(yyVal, yyVals, yyTop); // line 1294
break;
case 349: yyVal = case349_line1297(yyVal, yyVals, yyTop); // line 1297
break;
case 350: yyVal = case350_line1302(yyVal, yyVals, yyTop); // line 1302
break;
case 351: yyVal = case351_line1304(yyVal, yyVals, yyTop); // line 1304
break;
case 352: yyVal = case352_line1308(yyVal, yyVals, yyTop); // line 1308
break;
case 353: yyVal = case353_line1310(yyVal, yyVals, yyTop); // line 1310
break;
case 354: yyVal = case354_line1315(yyVal, yyVals, yyTop); // line 1315
break;
case 356: yyVal = case356_line1320(yyVal, yyVals, yyTop); // line 1320
break;
case 357: yyVal = case357_line1323(yyVal, yyVals, yyTop); // line 1323
break;
case 360: yyVal = case360_line1330(yyVal, yyVals, yyTop); // line 1330
break;
case 361: yyVal = case361_line1343(yyVal, yyVals, yyTop); // line 1343
break;
case 362: yyVal = case362_line1347(yyVal, yyVals, yyTop); // line 1347
break;
case 365: yyVal = case365_line1353(yyVal, yyVals, yyTop); // line 1353
break;
case 367: yyVal = case367_line1358(yyVal, yyVals, yyTop); // line 1358
break;
case 370: yyVal = case370_line1369(yyVal, yyVals, yyTop); // line 1369
break;
case 372: yyVal = case372_line1376(yyVal, yyVals, yyTop); // line 1376
break;
case 374: yyVal = case374_line1382(yyVal, yyVals, yyTop); // line 1382
break;
case 375: yyVal = case375_line1387(yyVal, yyVals, yyTop); // line 1387
break;
case 376: yyVal = case376_line1404(yyVal, yyVals, yyTop); // line 1404
break;
case 377: yyVal = case377_line1421(yyVal, yyVals, yyTop); // line 1421
break;
case 378: yyVal = case378_line1437(yyVal, yyVals, yyTop); // line 1437
break;
case 379: yyVal = case379_line1440(yyVal, yyVals, yyTop); // line 1440
break;
case 380: yyVal = case380_line1446(yyVal, yyVals, yyTop); // line 1446
break;
case 381: yyVal = case381_line1449(yyVal, yyVals, yyTop); // line 1449
break;
case 383: yyVal = case383_line1454(yyVal, yyVals, yyTop); // line 1454
break;
case 384: yyVal = case384_line1459(yyVal, yyVals, yyTop); // line 1459
break;
case 385: yyVal = case385_line1462(yyVal, yyVals, yyTop); // line 1462
break;
case 386: yyVal = case386_line1468(yyVal, yyVals, yyTop); // line 1468
break;
case 387: yyVal = case387_line1471(yyVal, yyVals, yyTop); // line 1471
break;
case 388: yyVal = case388_line1476(yyVal, yyVals, yyTop); // line 1476
break;
case 389: yyVal = case389_line1479(yyVal, yyVals, yyTop); // line 1479
break;
case 390: yyVal = case390_line1483(yyVal, yyVals, yyTop); // line 1483
break;
case 391: yyVal = case391_line1486(yyVal, yyVals, yyTop); // line 1486
break;
case 392: yyVal = case392_line1491(yyVal, yyVals, yyTop); // line 1491
break;
case 393: yyVal = case393_line1494(yyVal, yyVals, yyTop); // line 1494
break;
case 394: yyVal = case394_line1498(yyVal, yyVals, yyTop); // line 1498
break;
case 395: yyVal = case395_line1502(yyVal, yyVals, yyTop); // line 1502
break;
case 396: yyVal = case396_line1508(yyVal, yyVals, yyTop); // line 1508
break;
case 397: yyVal = case397_line1516(yyVal, yyVals, yyTop); // line 1516
break;
case 398: yyVal = case398_line1519(yyVal, yyVals, yyTop); // line 1519
break;
case 399: yyVal = case399_line1522(yyVal, yyVals, yyTop); // line 1522
break;
case 401: yyVal = case401_line1529(yyVal, yyVals, yyTop); // line 1529
break;
case 406: yyVal = case406_line1539(yyVal, yyVals, yyTop); // line 1539
break;
case 408: yyVal = case408_line1566(yyVal, yyVals, yyTop); // line 1566
break;
case 409: yyVal = case409_line1569(yyVal, yyVals, yyTop); // line 1569
break;
case 410: yyVal = case410_line1572(yyVal, yyVals, yyTop); // line 1572
break;
case 416: yyVal = case416_line1578(yyVal, yyVals, yyTop); // line 1578
break;
case 417: yyVal = case417_line1581(yyVal, yyVals, yyTop); // line 1581
break;
case 418: yyVal = case418_line1584(yyVal, yyVals, yyTop); // line 1584
break;
case 419: yyVal = case419_line1587(yyVal, yyVals, yyTop); // line 1587
break;
case 420: yyVal = case420_line1590(yyVal, yyVals, yyTop); // line 1590
break;
case 421: yyVal = case421_line1593(yyVal, yyVals, yyTop); // line 1593
break;
case 422: yyVal = case422_line1598(yyVal, yyVals, yyTop); // line 1598
break;
case 423: yyVal = case423_line1603(yyVal, yyVals, yyTop); // line 1603
break;
case 426: yyVal = case426_line1611(yyVal, yyVals, yyTop); // line 1611
break;
case 427: yyVal = case427_line1614(yyVal, yyVals, yyTop); // line 1614
break;
case 428: yyVal = case428_line1616(yyVal, yyVals, yyTop); // line 1616
break;
case 429: yyVal = case429_line1619(yyVal, yyVals, yyTop); // line 1619
break;
case 430: yyVal = case430_line1625(yyVal, yyVals, yyTop); // line 1625
break;
case 431: yyVal = case431_line1630(yyVal, yyVals, yyTop); // line 1630
break;
case 432: yyVal = case432_line1635(yyVal, yyVals, yyTop); // line 1635
break;
case 433: yyVal = case433_line1638(yyVal, yyVals, yyTop); // line 1638
break;
case 434: yyVal = case434_line1641(yyVal, yyVals, yyTop); // line 1641
break;
case 435: yyVal = case435_line1644(yyVal, yyVals, yyTop); // line 1644
break;
case 436: yyVal = case436_line1647(yyVal, yyVals, yyTop); // line 1647
break;
case 437: yyVal = case437_line1650(yyVal, yyVals, yyTop); // line 1650
break;
case 438: yyVal = case438_line1653(yyVal, yyVals, yyTop); // line 1653
break;
case 439: yyVal = case439_line1656(yyVal, yyVals, yyTop); // line 1656
break;
case 440: yyVal = case440_line1659(yyVal, yyVals, yyTop); // line 1659
break;
case 441: yyVal = case441_line1664(yyVal, yyVals, yyTop); // line 1664
break;
case 442: yyVal = case442_line1667(yyVal, yyVals, yyTop); // line 1667
break;
case 443: yyVal = case443_line1670(yyVal, yyVals, yyTop); // line 1670
break;
case 444: yyVal = case444_line1673(yyVal, yyVals, yyTop); // line 1673
break;
case 445: yyVal = case445_line1676(yyVal, yyVals, yyTop); // line 1676
break;
case 446: yyVal = case446_line1687(yyVal, yyVals, yyTop); // line 1687
break;
case 447: yyVal = case447_line1692(yyVal, yyVals, yyTop); // line 1692
break;
case 448: yyVal = case448_line1696(yyVal, yyVals, yyTop); // line 1696
break;
case 449: yyVal = case449_line1702(yyVal, yyVals, yyTop); // line 1702
break;
case 450: yyVal = case450_line1709(yyVal, yyVals, yyTop); // line 1709
break;
case 451: yyVal = case451_line1720(yyVal, yyVals, yyTop); // line 1720
break;
case 452: yyVal = case452_line1723(yyVal, yyVals, yyTop); // line 1723
break;
case 455: yyVal = case455_line1731(yyVal, yyVals, yyTop); // line 1731
break;
case 456: yyVal = case456_line1740(yyVal, yyVals, yyTop); // line 1740
break;
case 459: yyVal = case459_line1748(yyVal, yyVals, yyTop); // line 1748
break;
case 460: yyVal = case460_line1757(yyVal, yyVals, yyTop); // line 1757
break;
case 461: yyVal = case461_line1760(yyVal, yyVals, yyTop); // line 1760
break;
case 462: yyVal = case462_line1764(yyVal, yyVals, yyTop); // line 1764
break;
case 463: yyVal = case463_line1770(yyVal, yyVals, yyTop); // line 1770
break;
case 464: yyVal = case464_line1772(yyVal, yyVals, yyTop); // line 1772
break;
case 465: yyVal = case465_line1784(yyVal, yyVals, yyTop); // line 1784
break;
case 466: yyVal = case466_line1787(yyVal, yyVals, yyTop); // line 1787
break;
case 467: yyVal = case467_line1790(yyVal, yyVals, yyTop); // line 1790
break;
case 469: yyVal = case469_line1799(yyVal, yyVals, yyTop); // line 1799
break;
case 470: yyVal = case470_line1804(yyVal, yyVals, yyTop); // line 1804
break;
case 490: yyVal = case490_line1823(yyVal, yyVals, yyTop); // line 1823
break;
case 493: yyVal = case493_line1829(yyVal, yyVals, yyTop); // line 1829
break;
case 494: yyVal = case494_line1833(yyVal, yyVals, yyTop); // line 1833
break;
case 495: yyVal = case495_line1837(yyVal, yyVals, yyTop); // line 1837
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

public Object case444_line1673(Object yyVal, Object[] yyVals, int yyTop) {
                   yyerror("formal argument cannot be a class variable");
    return yyVal;
}
public Object case388_line1476(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new StrNode(((Token)yyVals[0+yyTop]).getPosition(), ByteList.create(""));
    return yyVal;
}
public Object case372_line1376(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((Node)yyVals[0+yyTop]) instanceof EvStrNode ? new DStrNode(getPosition(((Node)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop])) : ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case190_line767(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), getPosition(null)), "-@");
    return yyVal;
}
public Object case9_line319(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.setState(LexState.EXPR_FNAME);
    return yyVal;
}
public Object case92_line641(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_colon3(getPosition(((Token)yyVals[-1+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case33_line432(Object yyVal, Object[] yyVals, int yyTop) {
                  ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
		  yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
                  ((MultipleAsgnNode)yyVals[-2+yyTop]).setPosition(getPosition(((MultipleAsgnNode)yyVals[-2+yyTop])));
    return yyVal;
}
public Object case447_line1692(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new ListNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
                    ((ListNode) yyVal).add(new ArgumentNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue()));
    return yyVal;
}
public Object case386_line1468(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new ArrayNode(getPosition(null));
    return yyVal;
}
public Object case307_line1149(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new Boolean(support.isInDef());
                  support.setInDef(false);
    return yyVal;
}
public Object case15_line336(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new IfNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), null);
    return yyVal;
}
public Object case21_line365(Object yyVal, Object[] yyVals, int yyTop) {
                  support.getResult().addBeginNode(new PreExeNode(getPosition(((Node)yyVals[-1+yyTop])), support.getCurrentScope(), ((Node)yyVals[-1+yyTop])));
                  support.popCurrentScope();
                  yyVal = null; /*XXX 0;*/
    return yyVal;
}
public Object case421_line1593(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new Token("__LINE__", Tokens.k__LINE__, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case191_line770(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((FloatNode)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), getPosition(null)), "-@");
    return yyVal;
}
public Object case23_line376(Object yyVal, Object[] yyVals, int yyTop) {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
                  yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case193_line780(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "-@");
    return yyVal;
}
public Object case300_line1120(Object yyVal, Object[] yyVals, int yyTop) {
/* TODO: MRI is just a when node.  We need this extra logic for IDE consumers (null in casenode statement should be implicit nil)*/
/*                  if (support.getConfiguration().hasExtraPositionInformation()) {*/
                      yyVal = support.newCaseNode(getPosition(((Token)yyVals[-3+yyTop])), null, ((Node)yyVals[-1+yyTop]));
/*                  } else {*/
/*                      $$ = $3;*/
/*                  }*/
    return yyVal;
}
public Object case279_line1046(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_colon3(getPosition(((Token)yyVals[-1+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case432_line1635(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.new_args(getPosition(((ListNode)yyVals[-5+yyTop])), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case342_line1275(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case84_line605(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case198_line795(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">", ((Node)yyVals[0+yyTop]), getPosition(null));
    return yyVal;
}
public Object case427_line1614(Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.setState(LexState.EXPR_BEG);
    return yyVal;
}
public Object case367_line1358(Object yyVal, Object[] yyVals, int yyTop) {
                  if (((Node)yyVals[0+yyTop]) != null) {
                      yyVal = ((Node)yyVals[0+yyTop]);
                  } else {
                      yyVal = new NilNode(getPosition(null));
                  }
    return yyVal;
}
public Object case59_line511(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_yield(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case446_line1687(Object yyVal, Object[] yyVals, int yyTop) {
                    support.allowDubyExtension(((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition());
                    yyVal = new ListNode(((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition());
                    ((ListNode) yyVal).add(new TypedArgumentNode(((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition(), (String) ((Token)yyVals[-2+yyTop]).getValue(), ((Node)yyVals[0+yyTop])));
    return yyVal;
}
public Object case439_line1656(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.new_args(getPosition(((BlockArgNode)yyVals[0+yyTop])), null, null, null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case334_line1237(Object yyVal, Object[] yyVals, int yyTop) {
    return yyVal;
}
public Object case330_line1227(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new IfNode(getPosition(((Token)yyVals[-4+yyTop])), support.getConditionNode(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case222_line869(Object yyVal, Object[] yyVals, int yyTop) {
                  support.checkExpression(((Node)yyVals[-1+yyTop]));
		  yyVal = new NewlineNode(getPosition(((Token)yyVals[-2+yyTop])), support.newSplatNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])));
    return yyVal;
}
public Object case24_line380(Object yyVal, Object[] yyVals, int yyTop) {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
		  if (((MultipleAsgnNode)yyVals[-2+yyTop]).getHeadNode() != null) {
		      ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(new ToAryNode(getPosition(((MultipleAsgnNode)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
		  } else {
		      ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(support.newArrayNode(getPosition(((MultipleAsgnNode)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
		  }
		  yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
    return yyVal;
}
public Object case90_line636(Object yyVal, Object[] yyVals, int yyTop) {
                  yyerror("class/module name must be CONSTANT");
    return yyVal;
}
public Object case285_line1074(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new YieldNode(getPosition(((Token)yyVals[-2+yyTop])), null, false);
    return yyVal;
}
public Object case212_line837(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newOrNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case45_line465(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new BreakNode(getPosition(((Token)yyVals[-1+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
    return yyVal;
}
public Object case81_line594(Object yyVal, Object[] yyVals, int yyTop) {
	          support.backrefAssignError(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case2_line272(Object yyVal, Object[] yyVals, int yyTop) {
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
public Object case277_line1036(Object yyVal, Object[] yyVals, int yyTop) {
                  if (((Node)yyVals[-1+yyTop]) != null) {
                      /* compstmt position includes both parens around it*/
                      ((ISourcePositionHolder) ((Node)yyVals[-1+yyTop])).setPosition(getPosition(((Token)yyVals[-2+yyTop])));
                  }
		  yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case29_line415(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public Object case206_line819(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new NotNode(getPosition(((Node)yyVals[-2+yyTop])), support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
    return yyVal;
}
public Object case236_line918(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_blk_pass(support.newSplatNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case353_line1310(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new IterNode(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  support.popCurrentScope();
    return yyVal;
}
public Object case57_line505(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])); 
    return yyVal;
}
public Object case234_line909(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((ListNode)yyVals[-3+yyTop]).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case238_line924(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_blk_pass(support.newArrayNode(getPosition(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop])).addAll(((ListNode)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case73_line557(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case224_line877(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((Node)yyVals[-2+yyTop]);
		  ((Node)yyVal).setPosition(getPosition(((Token)yyVals[-3+yyTop])));
    return yyVal;
}
public Object case469_line1799(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((ListNode)yyVals[-2+yyTop]).addAll(((ListNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case417_line1581(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new Token("self", Tokens.kSELF, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case223_line874(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new ArrayNode(getPosition(((Token)yyVals[-2+yyTop])));
    return yyVal;
}
public Object case67_line538(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new MultipleAsgnNode(getPosition(((ListNode)yyVals[-1+yyTop])), ((ListNode)yyVals[-1+yyTop]), new StarNode(getPosition(null)));
    return yyVal;
}
public Object case237_line921(Object yyVal, Object[] yyVals, int yyTop) {
    return yyVal;
}
public Object case72_line554(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newArrayNode(((Node)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case295_line1105(Object yyVal, Object[] yyVals, int yyTop) {
                  Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);
                  yyVal = new WhileNode(getPosition(((Token)yyVals[-6+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), body);
    return yyVal;
}
public Object case31_line421(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), new SValueNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
    return yyVal;
}
public Object case66_line535(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new MultipleAsgnNode(getPosition(((ListNode)yyVals[-2+yyTop])), ((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case178_line724(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public Object case254_line978(Object yyVal, Object[] yyVals, int yyTop) {
                  warnings.warn(ID.ARGUMENT_EXTRA_SPACE, getPosition(((Token)yyVals[-2+yyTop])), "don't put space before argument parentheses");
	          yyVal = null;
    return yyVal;
}
public Object case450_line1709(Object yyVal, Object[] yyVals, int yyTop) {
                   String identifier = (String) ((Token)yyVals[-2+yyTop]).getValue();

                   if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                       yyerror("duplicate optional argument name");
                   }
		   support.getCurrentScope().getLocalScope().addVariable(identifier);
                   yyVal = support.assignable(((Token)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case260_line999(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newArrayNode(getPosition2(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case210_line831(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">>", ((Node)yyVals[0+yyTop]), getPosition(null));
    return yyVal;
}
public Object case240_line930(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-4+yyTop])), support.newArrayNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case316_line1191(Object yyVal, Object[] yyVals, int yyTop) {
                  /* TODO: We should use implicit nil for body, but problem (punt til later)*/
                  Node body = ((Node)yyVals[-1+yyTop]); /*$8 == null ? NilImplicitNode.NIL : $8;*/

                  yyVal = new DefsNode(getPosition(((Token)yyVals[-8+yyTop])), ((Node)yyVals[-7+yyTop]), new ArgumentNode(((Token)yyVals[-4+yyTop]).getPosition(), (String) ((Token)yyVals[-4+yyTop]).getValue()), ((ArgsNode)yyVals[-2+yyTop]), support.getCurrentScope(), body);
                  support.popCurrentScope();
                  support.setInSingle(support.getInSingle() - 1);
    return yyVal;
}
public Object case452_line1723(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case379_line1440(Object yyVal, Object[] yyVals, int yyTop) {
		   yyVal = ((ListNode)yyVals[-1+yyTop]);
                   ((ISourcePositionHolder)yyVal).setPosition(getPosition(((Token)yyVals[-2+yyTop])));
    return yyVal;
}
public Object case349_line1297(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new ZSuperNode(((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case173_line694(Object yyVal, Object[] yyVals, int yyTop) {
                  ISourcePosition position = getPosition(((Token)yyVals[-1+yyTop]));
                  Node body = ((Node)yyVals[0+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop]);
                  yyVal = support.node_assign(((Node)yyVals[-4+yyTop]), new RescueNode(position, ((Node)yyVals[-2+yyTop]), new RescueBodyNode(position, null, body, null), null));
    return yyVal;
}
public Object case88_line623(Object yyVal, Object[] yyVals, int yyTop) {
                  if (support.isInDef() || support.isInSingle()) {
		      yyerror("dynamic constant assignment");
		  }

                  ISourcePosition position = getPosition(((Token)yyVals[-1+yyTop]));

                  yyVal = new ConstDeclNode(position, null, support.new_colon3(position, (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
    return yyVal;
}
public Object case308_line1152(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new Integer(support.getInSingle());
                  support.setInSingle(0);
		  support.pushLocalScope();
    return yyVal;
}
public Object case19_line356(Object yyVal, Object[] yyVals, int yyTop) {
                  Node body = ((Node)yyVals[0+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop]);
	          yyVal = new RescueNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), new RescueBodyNode(getPosition(((Node)yyVals[-2+yyTop])), null, body, null), null);
    return yyVal;
}
public Object case253_line976(Object yyVal, Object[] yyVals, int yyTop) {
		  lexer.setState(LexState.EXPR_ENDARG);
    return yyVal;
}
public Object case39_line450(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new NotNode(getPosition(((Token)yyVals[-1+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
    return yyVal;
}
public Object case391_line1486(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case203_line810(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "===", ((Node)yyVals[0+yyTop]), getPosition(null));
    return yyVal;
}
public Object case174_line699(Object yyVal, Object[] yyVals, int yyTop) {
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
public Object case176_line718(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public Object case247_line958(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-8+yyTop])), support.newArrayNode(getPosition(((Node)yyVals[-8+yyTop])), ((Node)yyVals[-8+yyTop])).addAll(((ListNode)yyVals[-6+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case53_line493(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_fcall(((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])); 
    return yyVal;
}
public Object case313_line1176(Object yyVal, Object[] yyVals, int yyTop) {
                  /* TODO: We should use implicit nil for body, but problem (punt til later)*/
                  Node body = ((Node)yyVals[-1+yyTop]); /*$5 == null ? NilImplicitNode.NIL : $5;*/

                  /* NOEX_PRIVATE for toplevel */
                  yyVal = new DefnNode(getPosition(((Token)yyVals[-5+yyTop])), new ArgumentNode(((Token)yyVals[-4+yyTop]).getPosition(), (String) ((Token)yyVals[-4+yyTop]).getValue()), ((ArgsNode)yyVals[-2+yyTop]), support.getCurrentScope(), body);
                  support.popCurrentScope();
                  support.setInDef(false);
    return yyVal;
}
public Object case348_line1294(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case94_line647(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_colon2(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case305_line1138(Object yyVal, Object[] yyVals, int yyTop) {
                  if (support.isInDef() || support.isInSingle()) {
                      yyerror("class definition in method body");
                  }
		  support.pushLocalScope();
    return yyVal;
}
public Object case186_line755(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "*", ((Node)yyVals[0+yyTop]), getPosition(null));
    return yyVal;
}
public Object case8_line315(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case381_line1449(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]) instanceof EvStrNode ? new DStrNode(getPosition(((ListNode)yyVals[-2+yyTop]))).add(((Node)yyVals[-1+yyTop])) : ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case351_line1304(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new IterNode(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  support.popCurrentScope();
    return yyVal;
}
public Object case332_line1232(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case3_line284(Object yyVal, Object[] yyVals, int yyTop) {
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
    return yyVal;
}
public Object case248_line962(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_blk_pass(support.newSplatNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case7_line312(Object yyVal, Object[] yyVals, int yyTop) {
	          yyVal = support.appendToBlock(((Node)yyVals[-2+yyTop]), support.newline_node(((Node)yyVals[0+yyTop]), getPosition(((Node)yyVals[0+yyTop]), true)));
    return yyVal;
}
public Object case189_line764(Object yyVal, Object[] yyVals, int yyTop) {
		  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), getPosition(null));
    return yyVal;
}
public Object case462_line1764(Object yyVal, Object[] yyVals, int yyTop) {
                  if (!(((Node)yyVals[0+yyTop]) instanceof SelfNode)) {
		      support.checkExpression(((Node)yyVals[0+yyTop]));
		  }
		  yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case449_line1702(Object yyVal, Object[] yyVals, int yyTop) {
                   ((ListNode)yyVals[-2+yyTop]).add(new ArgumentNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue()));
                   ((ListNode)yyVals[-2+yyTop]).setPosition(getPosition(((ListNode)yyVals[-2+yyTop])));
		   yyVal = ((ListNode)yyVals[-2+yyTop]);
    return yyVal;
}
public Object case219_line859(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((ListNode)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case196_line789(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "&", ((Node)yyVals[0+yyTop]), getPosition(null));
    return yyVal;
}
public Object case306_line1143(Object yyVal, Object[] yyVals, int yyTop) {
                  Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);

                  yyVal = new ClassNode(getPosition(((Token)yyVals[-5+yyTop])), ((Colon3Node)yyVals[-4+yyTop]), support.getCurrentScope(), body, ((Node)yyVals[-3+yyTop]));
                  support.popCurrentScope();
    return yyVal;
}
public Object case195_line786(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "^", ((Node)yyVals[0+yyTop]), getPosition(null));
    return yyVal;
}
public Object case430_line1625(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = ((Node)yyVals[-2+yyTop]);
                   ((ISourcePositionHolder)yyVal).setPosition(getPosition(((Token)yyVals[-3+yyTop])));
                   lexer.setState(LexState.EXPR_BEG);
    return yyVal;
}
public Object case423_line1603(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
    return yyVal;
}
public Object case398_line1519(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new InstVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case197_line792(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=>", ((Node)yyVals[0+yyTop]), getPosition(null));
    return yyVal;
}
public Object case465_line1784(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new ArrayNode(getPosition(null));
    return yyVal;
}
public Object case419_line1587(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new Token("false", Tokens.kFALSE, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case89_line632(Object yyVal, Object[] yyVals, int yyTop) {
                   support.backrefAssignError(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case314_line1185(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.setState(LexState.EXPR_FNAME);
    return yyVal;
}
public Object case311_line1167(Object yyVal, Object[] yyVals, int yyTop) {
                  Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);

                  yyVal = new ModuleNode(getPosition(((Token)yyVals[-4+yyTop])), ((Colon3Node)yyVals[-3+yyTop]), support.getCurrentScope(), body);
                  support.popCurrentScope();
    return yyVal;
}
public Object case262_line1006(Object yyVal, Object[] yyVals, int yyTop) {
		  yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case230_line894(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_blk_pass(((ListNode)yyVals[-1+yyTop]), ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case395_line1502(Object yyVal, Object[] yyVals, int yyTop) {
		   yyVal = lexer.getStrTerm();
		   lexer.setStrTerm(null);
		   lexer.setState(LexState.EXPR_BEG);
                   lexer.getConditionState().stop();
	           lexer.getCmdArgumentState().stop();
    return yyVal;
}
public Object case394_line1498(Object yyVal, Object[] yyVals, int yyTop) {
		   lexer.setStrTerm(((StrTerm)yyVals[-1+yyTop]));
	           yyVal = new EvStrNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case215_line846(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case221_line866(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newArrayNode(getPosition(((ListNode)yyVals[-1+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
    return yyVal;
}
public Object case22_line370(Object yyVal, Object[] yyVals, int yyTop) {
                  if (support.isInDef() || support.isInSingle()) {
                      warnings.warn(ID.END_IN_METHOD, getPosition(((Token)yyVals[-3+yyTop])), "END in method; use at_exit");
                  }
                  yyVal = new PostExeNode(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case49_line477(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case79_line576(Object yyVal, Object[] yyVals, int yyTop) {
                  if (support.isInDef() || support.isInSingle()) {
		      yyerror("dynamic constant assignment");
		  }

		  ISourcePosition position = getPosition(((Node)yyVals[-2+yyTop]));

                  yyVal = new ConstDeclNode(position, null, support.new_colon2(position, ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
    return yyVal;
}
public Object case235_line913(Object yyVal, Object[] yyVals, int yyTop) {
                  support.checkExpression(((Node)yyVals[-1+yyTop]));
		  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-6+yyTop])), ((ListNode)yyVals[-6+yyTop]).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case278_line1043(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_colon2(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case63_line523(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new MultipleAsgnNode(getPosition(((Token)yyVals[-2+yyTop])), support.newArrayNode(getPosition(((Token)yyVals[-2+yyTop])), ((MultipleAsgnNode)yyVals[-1+yyTop])), null);
    return yyVal;
}
public Object case214_line843(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new IfNode(getPosition(((Node)yyVals[-4+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case99_line658(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.setState(LexState.EXPR_END);
                  yyVal = yyVals[0+yyTop];
    return yyVal;
}
public Object case292_line1098(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new IfNode(getPosition(((Token)yyVals[-5+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-2+yyTop]));
    return yyVal;
}
public Object case78_line573(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case459_line1748(Object yyVal, Object[] yyVals, int yyTop) {
                  String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();

                  if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                      yyerror("duplicate block argument name");
                  }
                  yyVal = new BlockArgNode(getPosition(((Token)yyVals[-1+yyTop])), support.getCurrentScope().getLocalScope().addVariable(identifier), identifier);
    return yyVal;
}
public Object case375_line1387(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((Node)yyVals[-1+yyTop]);
                  ((ISourcePositionHolder)yyVal).setPosition(getPosition(((Token)yyVals[-2+yyTop])));
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
    return yyVal;
}
public Object case14_line333(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case280_line1049(Object yyVal, Object[] yyVals, int yyTop) {
                  if (((Node)yyVals[-3+yyTop]) instanceof SelfNode) {
                      yyVal = support.new_fcall(new Token("[]", getPosition(((Node)yyVals[-3+yyTop]))), ((Node)yyVals[-1+yyTop]), null);
                  } else {
                      yyVal = support.new_call(((Node)yyVals[-3+yyTop]), new Token("[]", getPosition(((Node)yyVals[-3+yyTop]))), ((Node)yyVals[-1+yyTop]), null);
                  }
    return yyVal;
}
public Object case205_line816(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case494_line1833(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = null;
    return yyVal;
}
public Object case435_line1644(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.new_args(((ISourcePositionHolder)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case389_line1479(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case374_line1382(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case337_line1244(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new ZeroArgNode(getPosition(((Token)yyVals[0+yyTop])));
    return yyVal;
}
public Object case208_line825(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "~");
    return yyVal;
}
public Object case216_line850(Object yyVal, Object[] yyVals, int yyTop) {
	          support.checkExpression(((Node)yyVals[0+yyTop]));
	          yyVal = ((Node)yyVals[0+yyTop]);   
    return yyVal;
}
public Object case283_line1068(Object yyVal, Object[] yyVals, int yyTop) {
		  yyVal = new ReturnNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
    return yyVal;
}
public Object case345_line1285(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case51_line484(Object yyVal, Object[] yyVals, int yyTop) {
                    yyVal = new IterNode(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                    support.popCurrentScope();
    return yyVal;
}
public Object case83_line602(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.aryset(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case65_line531(Object yyVal, Object[] yyVals, int yyTop) {
/*mirko: check*/
                  yyVal = new MultipleAsgnNode(getPosition(((Node)yyVals[-1+yyTop])), ((ListNode)yyVals[-1+yyTop]).add(((Node)yyVals[0+yyTop])), null);
    return yyVal;
}
public Object case420_line1590(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new Token("__FILE__", Tokens.k__FILE__, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case244_line946(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newArrayNode(getPosition(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case288_line1083(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new FCallNoArgBlockNode(getPosition(((Token)yyVals[-1+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((IterNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case61_line517(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((MultipleAsgnNode)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case177_line721(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public Object case433_line1638(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.new_args(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case428_line1616(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case336_line1241(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new ZeroArgNode(getPosition(((Token)yyVals[-1+yyTop])));
    return yyVal;
}
public Object case37_line444(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newOrNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case44_line462(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new ReturnNode(getPosition(((Token)yyVals[-1+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
    return yyVal;
}
public Object case74_line561(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
    return yyVal;
}
public Object case50_line482(Object yyVal, Object[] yyVals, int yyTop) {
                    support.pushBlockScope();
    return yyVal;
}
public Object case225_line881(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newArrayNode(getPosition(((Token)yyVals[-3+yyTop])), ((Node)yyVals[-2+yyTop]));
    return yyVal;
}
public Object case303_line1133(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.getConditionState().end();
    return yyVal;
}
public Object case27_line409(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public Object case344_line1282(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_fcall(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case201_line804(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=", ((Node)yyVals[0+yyTop]), getPosition(null));
    return yyVal;
}
public Object case318_line1202(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new NextNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
    return yyVal;
}
public Object case242_line938(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newArrayNode(getPosition(((ListNode)yyVals[-1+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case246_line954(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-6+yyTop])), support.newArrayNode(getPosition(((Node)yyVals[-6+yyTop])), ((Node)yyVals[-6+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case294_line1103(Object yyVal, Object[] yyVals, int yyTop) {
		  lexer.getConditionState().end();
    return yyVal;
}
public Object case245_line950(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newArrayNode(getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop])).addAll(((ListNode)yyVals[-3+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case200_line801(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<", ((Node)yyVals[0+yyTop]), getPosition(null));
    return yyVal;
}
public Object case255_line982(Object yyVal, Object[] yyVals, int yyTop) {
		  lexer.setState(LexState.EXPR_ENDARG);
    return yyVal;
}
public Object case28_line412(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new OpAsgnNode(getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
    return yyVal;
}
public Object case493_line1829(Object yyVal, Object[] yyVals, int yyTop) {
                  yyerrok();
    return yyVal;
}
public Object case445_line1676(Object yyVal, Object[] yyVals, int yyTop) {
                   String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();
                   if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                       yyerror("duplicate argument name");
                   }

		   support.getCurrentScope().getLocalScope().addVariable(identifier);
                   yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
}
public Object case463_line1770(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.setState(LexState.EXPR_BEG);
    return yyVal;
}
public Object case392_line1491(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case340_line1258(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new IterNode(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  support.popCurrentScope();
    return yyVal;
}
public Object case302_line1131(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.getConditionState().begin();
    return yyVal;
}
public Object case80_line585(Object yyVal, Object[] yyVals, int yyTop) {
                  if (support.isInDef() || support.isInSingle()) {
		      yyerror("dynamic constant assignment");
		  }

                  ISourcePosition position = getPosition(((Token)yyVals[-1+yyTop]));

                  yyVal = new ConstDeclNode(position, null, support.new_colon3(position, (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
    return yyVal;
}
public Object case455_line1731(Object yyVal, Object[] yyVals, int yyTop) {
                  String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();

                  if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                      yyerror("duplicate rest argument name");
                  }

                  yyVal = new RestArgNode(getPosition(((Token)yyVals[-1+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue(), support.getCurrentScope().getLocalScope().addVariable(identifier));
    return yyVal;
}
public Object case448_line1696(Object yyVal, Object[] yyVals, int yyTop) {
                   support.allowDubyExtension(((ISourcePositionHolder)yyVals[-4+yyTop]).getPosition());
                   ((ListNode)yyVals[-4+yyTop]).add(new TypedArgumentNode(((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition(), (String) ((Token)yyVals[-2+yyTop]).getValue(), ((Node)yyVals[0+yyTop])));
                   ((ListNode)yyVals[-4+yyTop]).setPosition(getPosition(((ListNode)yyVals[-4+yyTop])));
		   yyVal = ((ListNode)yyVals[-4+yyTop]);
    return yyVal;
}
public Object case408_line1566(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = ((FloatNode)yyVals[0+yyTop]);
    return yyVal;
}
public Object case93_line644(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_colon2(getPosition(((Token)yyVals[0+yyTop])), null, (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case20_line360(Object yyVal, Object[] yyVals, int yyTop) {
                  if (support.isInDef() || support.isInSingle()) {
                      yyerror("BEGIN in method");
                  }
		  support.pushLocalScope();
    return yyVal;
}
public Object case387_line1471(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case343_line1278(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case188_line761(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "%", ((Node)yyVals[0+yyTop]), getPosition(null));
    return yyVal;
}
public Object case297_line1111(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.getConditionState().end();
    return yyVal;
}
public Object case422_line1598(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.gettable(((Token)yyVals[0+yyTop]));
    return yyVal;
}
public Object case399_line1522(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new ClassVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case194_line783(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "|", ((Node)yyVals[0+yyTop]), getPosition(null));
    return yyVal;
}
public Object case85_line608(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case461_line1760(Object yyVal, Object[] yyVals, int yyTop) {
	          yyVal = null;
    return yyVal;
}
public Object case460_line1757(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((BlockArgNode)yyVals[0+yyTop]);
    return yyVal;
}
public Object case431_line1630(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case380_line1446(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new ArrayNode(getPosition(null));
    return yyVal;
}
public Object case184_line749(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "+", ((Node)yyVals[0+yyTop]), getPosition(null));
    return yyVal;
}
public Object case46_line468(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new NextNode(getPosition(((Token)yyVals[-1+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((Token)yyVals[-1+yyTop]))));
    return yyVal;
}
public Object case76_line567(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case290_line1087(Object yyVal, Object[] yyVals, int yyTop) {
	          if (((Node)yyVals[-1+yyTop]) != null && 
                      ((BlockAcceptingNode)yyVals[-1+yyTop]).getIterNode() instanceof BlockPassNode) {
                      throw new SyntaxException(PID.BLOCK_ARG_AND_BLOCK_GIVEN, getPosition(((Node)yyVals[-1+yyTop])), "Both block arg and actual block given.");
		  }
		  yyVal = ((BlockAcceptingNode)yyVals[-1+yyTop]).setIterNode(((IterNode)yyVals[0+yyTop]));
		  ((Node)yyVal).setPosition(getPosition(((Node)yyVals[-1+yyTop])));
    return yyVal;
}
public Object case301_line1128(Object yyVal, Object[] yyVals, int yyTop) {
		  yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case273_line1024(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new FCallNoArgNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case98_line653(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.setState(LexState.EXPR_END);
                  yyVal = ((Token)yyVals[0+yyTop]);
    return yyVal;
}
public Object case456_line1740(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new UnnamedRestArgNode(((Token)yyVals[0+yyTop]).getPosition(), support.getCurrentScope().getLocalScope().addVariable("*"));
    return yyVal;
}
public Object case390_line1483(Object yyVal, Object[] yyVals, int yyTop) {
		   yyVal = null;
    return yyVal;
}
public Object case377_line1421(Object yyVal, Object[] yyVals, int yyTop) {
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
public Object case226_line884(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((ListNode)yyVals[-4+yyTop]).add(((Node)yyVals[-2+yyTop]));
    return yyVal;
}
public Object case12_line327(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new VAliasNode(getPosition(((Token)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), "$" + ((BackRefNode)yyVals[0+yyTop]).getType()); /* XXX*/
    return yyVal;
}
public Object case1_line269(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.setState(LexState.EXPR_BEG);
                  support.initTopLocalVariables();
    return yyVal;
}
public Object case69_line544(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new MultipleAsgnNode(getPosition(((Token)yyVals[0+yyTop])), null, new StarNode(getPosition(null)));
    return yyVal;
}
public Object case426_line1611(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = null;
    return yyVal;
}
public Object case385_line1462(Object yyVal, Object[] yyVals, int yyTop) {
		   yyVal = ((ListNode)yyVals[-1+yyTop]);
                   ((ISourcePositionHolder)yyVal).setPosition(getPosition(((Token)yyVals[-2+yyTop])));
    return yyVal;
}
public Object case384_line1459(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new ZArrayNode(getPosition(((Token)yyVals[-2+yyTop])));
    return yyVal;
}
public Object case213_line840(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new DefinedNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case11_line324(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new VAliasNode(getPosition(((Token)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case370_line1369(Object yyVal, Object[] yyVals, int yyTop) {
                  /* FIXME: We may be intern'ing more than once.*/
                  yyVal = new SymbolNode(((Token)yyVals[0+yyTop]).getPosition(), ((String) ((Token)yyVals[0+yyTop]).getValue()).intern());
    return yyVal;
}
public Object case354_line1315(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newWhenNode(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case82_line599(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.assignable(((Token)yyVals[0+yyTop]), NilImplicitNode.NIL);
    return yyVal;
}
public Object case257_line989(Object yyVal, Object[] yyVals, int yyTop) {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
                  yyVal = new BlockPassNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case233_line905(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), support.newArrayNode(getPosition(((ListNode)yyVals[-4+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case55_line499(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])); 
    return yyVal;
}
public Object case296_line1109(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.getConditionState().begin();
    return yyVal;
}
public Object case284_line1071(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_yield(getPosition(((Token)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case443_line1670(Object yyVal, Object[] yyVals, int yyTop) {
                   yyerror("formal argument cannot be an global variable");
    return yyVal;
}
public Object case442_line1667(Object yyVal, Object[] yyVals, int yyTop) {
                   yyerror("formal argument cannot be a instance variable");
    return yyVal;
}
public Object case409_line1569(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.negateInteger(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case56_line502(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case207_line822(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new NotNode(getPosition(((Token)yyVals[-1+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
    return yyVal;
}
public Object case276_line1032(Object yyVal, Object[] yyVals, int yyTop) {
		 if (warnings.isVerbose()) warnings.warning(ID.GROUPED_EXPRESSION, getPosition(((Token)yyVals[-4+yyTop])), "(...) interpreted as grouped expression");
                  yyVal = ((Node)yyVals[-3+yyTop]);
    return yyVal;
}
public Object case352_line1308(Object yyVal, Object[] yyVals, int yyTop) {
                  support.pushBlockScope();
    return yyVal;
}
public Object case321_line1212(Object yyVal, Object[] yyVals, int yyTop) {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
		  yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case32_line424(Object yyVal, Object[] yyVals, int yyTop) {
                  if (((MultipleAsgnNode)yyVals[-2+yyTop]).getHeadNode() != null) {
		      ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(new ToAryNode(getPosition(((MultipleAsgnNode)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
		  } else {
		      ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(support.newArrayNode(getPosition(((MultipleAsgnNode)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
		  }
		  yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
    return yyVal;
}
public Object case243_line942(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), support.newArrayNode(getPosition(((ListNode)yyVals[-4+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case179_line727(Object yyVal, Object[] yyVals, int yyTop) {
	          yyerror("constant re-assignment");
    return yyVal;
}
public Object case291_line1095(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new IfNode(getPosition(((Token)yyVals[-5+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case26_line405(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_opElementAsgnNode(getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));

    return yyVal;
}
public Object case309_line1156(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new SClassNode(getPosition(((Token)yyVals[-7+yyTop])), ((Node)yyVals[-5+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop]));
                  support.popCurrentScope();
                  support.setInDef(((Boolean)yyVals[-4+yyTop]).booleanValue());
                  support.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
    return yyVal;
}
public Object case220_line862(Object yyVal, Object[] yyVals, int yyTop) {
                  support.checkExpression(((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case281_line1056(Object yyVal, Object[] yyVals, int yyTop) {
                  ISourcePosition position = getPosition(((Token)yyVals[-2+yyTop]));
                  if (((Node)yyVals[-1+yyTop]) == null) {
                      yyVal = new ZArrayNode(position); /* zero length array */
                  } else {
                      yyVal = ((Node)yyVals[-1+yyTop]);
                      ((ISourcePositionHolder)yyVal).setPosition(position);
                  }
    return yyVal;
}
public Object case441_line1664(Object yyVal, Object[] yyVals, int yyTop) {
                   yyerror("formal argument cannot be a constant");
    return yyVal;
}
public Object case357_line1323(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new SplatNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case182_line736(Object yyVal, Object[] yyVals, int yyTop) {
		  support.checkExpression(((Node)yyVals[-2+yyTop]));
		  support.checkExpression(((Node)yyVals[0+yyTop]));
    
                  boolean isLiteral = ((Node)yyVals[-2+yyTop]) instanceof FixnumNode && ((Node)yyVals[0+yyTop]) instanceof FixnumNode;
                  yyVal = new DotNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), false, isLiteral);
    return yyVal;
}
public Object case181_line733(Object yyVal, Object[] yyVals, int yyTop) {
                  support.backrefAssignError(((Node)yyVals[-2+yyTop]));
    return yyVal;
}
public Object case378_line1437(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new ZArrayNode(getPosition(((Token)yyVals[-2+yyTop])));
    return yyVal;
}
public Object case341_line1264(Object yyVal, Object[] yyVals, int yyTop) {
                  /* Workaround for JRUBY-2326 (MRI does not enter this production for some reason)*/
                  if (((Node)yyVals[-1+yyTop]) instanceof YieldNode) {
                      throw new SyntaxException(PID.BLOCK_GIVEN_TO_YIELD, getPosition(((Node)yyVals[-1+yyTop])), "block given to yield");
                  }
	          if (((BlockAcceptingNode)yyVals[-1+yyTop]).getIterNode() instanceof BlockPassNode) {
                      throw new SyntaxException(PID.BLOCK_ARG_AND_BLOCK_GIVEN, getPosition(((Node)yyVals[-1+yyTop])), "Both block arg and actual block given.");
                  }
		  yyVal = ((BlockAcceptingNode)yyVals[-1+yyTop]).setIterNode(((IterNode)yyVals[0+yyTop]));
		  ((Node)yyVal).setPosition(getPosition(((Node)yyVals[-1+yyTop])));
    return yyVal;
}
public Object case264_line1012(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newSplatNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case41_line455(Object yyVal, Object[] yyVals, int yyTop) {
                  support.checkExpression(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case263_line1009(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case438_line1653(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.new_args(getPosition(((RestArgNode)yyVals[-1+yyTop])), null, null, ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case199_line798(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">=", ((Node)yyVals[0+yyTop]), getPosition(null));
    return yyVal;
}
public Object case317_line1199(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new BreakNode(((Token)yyVals[0+yyTop]).getPosition(), NilImplicitNode.NIL);
    return yyVal;
}
public Object case286_line1077(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new YieldNode(getPosition(((Token)yyVals[0+yyTop])), null, false);
    return yyVal;
}
public Object case490_line1823(Object yyVal, Object[] yyVals, int yyTop) {
                  yyerrok();
    return yyVal;
}
public Object case320_line1208(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new RetryNode(((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case103_line668(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.setState(LexState.EXPR_FNAME);
    return yyVal;
}
public Object case175_line715(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_opElementAsgnNode(getPosition(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case52_line490(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_fcall(((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case440_line1659(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.new_args(support.createEmptyArgsNodePosition(getPosition(null)), null, null, null, null, null);
    return yyVal;
}
public Object case410_line1572(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.negateFloat(((FloatNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case104_line670(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.appendToBlock(((Node)yyVals[-3+yyTop]), new UndefNode(getPosition(((Node)yyVals[-3+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue()));
    return yyVal;
}
public Object case185_line752(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "-", ((Node)yyVals[0+yyTop]), getPosition(null));
    return yyVal;
}
public Object case437_line1650(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.new_args(getPosition(((ListNode)yyVals[-1+yyTop])), null, ((ListNode)yyVals[-1+yyTop]), null, null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case436_line1647(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.new_args(getPosition(((ListNode)yyVals[-3+yyTop])), null, ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case183_line743(Object yyVal, Object[] yyVals, int yyTop) {
		  support.checkExpression(((Node)yyVals[-2+yyTop]));
		  support.checkExpression(((Node)yyVals[0+yyTop]));
                  boolean isLiteral = ((Node)yyVals[-2+yyTop]) instanceof FixnumNode && ((Node)yyVals[0+yyTop]) instanceof FixnumNode;
                  yyVal = new DotNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), true, isLiteral);
    return yyVal;
}
public Object case466_line1787(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((ListNode)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case347_line1291(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_call(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop]), null, null);
    return yyVal;
}
public Object case36_line441(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newAndNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case250_line968(Object yyVal, Object[] yyVals, int yyTop) {
	          yyVal = new Long(lexer.getCmdArgumentState().begin());
    return yyVal;
}
public Object case16_line339(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new IfNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), null, ((Node)yyVals[-2+yyTop]));
    return yyVal;
}
public Object case470_line1804(Object yyVal, Object[] yyVals, int yyTop) {
                  ISourcePosition position;
                  if (((Node)yyVals[-2+yyTop]) == null && ((Node)yyVals[0+yyTop]) == null) {
                      position = getPosition(((Token)yyVals[-1+yyTop]));
                  } else {
                      position = getPosition(((Node)yyVals[-2+yyTop]));
                  }

                  yyVal = support.newArrayNode(position, ((Node)yyVals[-2+yyTop])).add(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case218_line856(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newArrayNode(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case192_line773(Object yyVal, Object[] yyVals, int yyTop) {
                  if (support.isLiteral(((Node)yyVals[0+yyTop]))) {
		      yyVal = ((Node)yyVals[0+yyTop]);
		  } else {
                      yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "+@");
		  }
    return yyVal;
}
public Object case4_line301(Object yyVal, Object[] yyVals, int yyTop) {
                  if (((Node)yyVals[-1+yyTop]) instanceof BlockNode) {
                      support.checkUselessStatements(((BlockNode)yyVals[-1+yyTop]));
		  }
                  yyVal = ((Node)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case17_line342(Object yyVal, Object[] yyVals, int yyTop) {
                  if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                      yyVal = new WhileNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                  } else {
                      yyVal = new WhileNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                  }
    return yyVal;
}
public Object case451_line1720(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new BlockNode(getPosition(((Node)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case350_line1302(Object yyVal, Object[] yyVals, int yyTop) {
                  support.pushBlockScope();
    return yyVal;
}
public Object case229_line891(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newArrayNode(getPosition(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case310_line1162(Object yyVal, Object[] yyVals, int yyTop) {
                  if (support.isInDef() || support.isInSingle()) { 
                      yyerror("module definition in method body");
                  }
		  support.pushLocalScope();
    return yyVal;
}
public Object case87_line614(Object yyVal, Object[] yyVals, int yyTop) {
                  if (support.isInDef() || support.isInSingle()) {
		      yyerror("dynamic constant assignment");
		  }
			
		  ISourcePosition position = getPosition(((Node)yyVals[-2+yyTop]));

                  yyVal = new ConstDeclNode(position, null, support.new_colon2(position, ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue()), NilImplicitNode.NIL);
    return yyVal;
}
public Object case401_line1529(Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.setState(LexState.EXPR_END);
                   yyVal = ((Token)yyVals[0+yyTop]);
		   ((ISourcePositionHolder)yyVal).setPosition(getPosition(((Token)yyVals[-1+yyTop])));
    return yyVal;
}
public Object case397_line1516(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new GlobalVarNode(((Token)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case86_line611(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case71_line549(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((MultipleAsgnNode)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case231_line897(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass(((Node)yyVal), ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case418_line1584(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new Token("true", Tokens.kTRUE, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case396_line1508(Object yyVal, Object[] yyVals, int yyTop) {
		   lexer.setStrTerm(((StrTerm)yyVals[-2+yyTop]));
                   lexer.getConditionState().restart();
	           lexer.getCmdArgumentState().restart();

		   yyVal = support.newEvStrNode(getPosition(((Token)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case376_line1404(Object yyVal, Object[] yyVals, int yyTop) {
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
public Object case360_line1330(Object yyVal, Object[] yyVals, int yyTop) {
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
    return yyVal;
}
public Object case339_line1256(Object yyVal, Object[] yyVals, int yyTop) {
                  support.pushBlockScope();
    return yyVal;
}
public Object case338_line1247(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((Node)yyVals[-1+yyTop]);

		  /* Include pipes on multiple arg type*/
                  if (((Node)yyVals[-1+yyTop]) instanceof MultipleAsgnNode) {
		      ((Node)yyVals[-1+yyTop]).setPosition(((Token)yyVals[-2+yyTop]).getPosition());
		  } 
    return yyVal;
}
public Object case293_line1101(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.getConditionState().begin();
    return yyVal;
}
public Object case68_line541(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new MultipleAsgnNode(getPosition(((Token)yyVals[-1+yyTop])), null, ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case18_line349(Object yyVal, Object[] yyVals, int yyTop) {
                  if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                      yyVal = new UntilNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                  } else {
                      yyVal = new UntilNode(getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                  }
    return yyVal;
}
public Object case312_line1173(Object yyVal, Object[] yyVals, int yyTop) {
                  support.setInDef(true);
		  support.pushLocalScope();
    return yyVal;
}
public Object case48_line474(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case261_line1002(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case495_line1837(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = null;
    return yyVal;
}
public Object case346_line1288(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case232_line901(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newArrayNode(getPosition(((ListNode)yyVals[-1+yyTop])), new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case77_line570(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.attrset(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case10_line321(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new AliasNode(getPosition(((Token)yyVals[-3+yyTop])), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case383_line1454(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.literal_concat(getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case304_line1135(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new ForNode(getPosition(((Token)yyVals[-8+yyTop])), ((Node)yyVals[-7+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-4+yyTop]));
    return yyVal;
}
public Object case64_line528(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new MultipleAsgnNode(getPosition(((ListNode)yyVals[0+yyTop])), ((ListNode)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case209_line828(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<<", ((Node)yyVals[0+yyTop]), getPosition(null));
    return yyVal;
}
public Object case13_line330(Object yyVal, Object[] yyVals, int yyTop) {
                  yyerror("can't make alias for the number variables");
    return yyVal;
}
public Object case239_line927(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_blk_pass(support.newArrayNode(getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case54_line496(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
    return yyVal;
}
public Object case204_line813(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new NotNode(getPosition(((Node)yyVals[-2+yyTop])), support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop]), getPosition(null)));
    return yyVal;
}
public Object case58_line508(Object yyVal, Object[] yyVals, int yyTop) {
		  yyVal = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop])); /* .setPosFrom($2);*/
    return yyVal;
}
public Object case429_line1619(Object yyVal, Object[] yyVals, int yyTop) {
                   yyerrok();
                   yyVal = null;
    return yyVal;
}
public Object case172_line689(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
		  /* FIXME: Consider fixing node_assign itself rather than single case*/
		  ((Node)yyVal).setPosition(getPosition(((Node)yyVals[-2+yyTop])));
    return yyVal;
}
public Object case319_line1205(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new RedoNode(((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case434_line1641(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = support.new_args(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), null, ((RestArgNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case406_line1539(Object yyVal, Object[] yyVals, int yyTop) {
                   lexer.setState(LexState.EXPR_END);

		   /* DStrNode: :"some text #{some expression}"*/
                   /* StrNode: :"some text"*/
		   /* EvStrNode :"#{some expression}"*/
                   if (((Node)yyVals[-1+yyTop]) == null) {
                       yyerror("empty symbol literal");
                   }

		   if (((Node)yyVals[-1+yyTop]) instanceof DStrNode) {
		       yyVal = new DSymbolNode(getPosition(((Token)yyVals[-2+yyTop])), ((DStrNode)yyVals[-1+yyTop]));
		   } else {
                       ISourcePosition position = getPosition(((Node)yyVals[-1+yyTop]));

                       /* We substract one since tsymbeg is longer than one*/
		       /* and we cannot union it directly so we assume quote*/
                       /* is one character long and subtract for it.*/
		       position.adjustStartOffset(-1);
                       ((Node)yyVals[-1+yyTop]).setPosition(position);
		       
		       yyVal = new DSymbolNode(getPosition(((Token)yyVals[-2+yyTop])));
                       ((DSymbolNode)yyVal).add(((Node)yyVals[-1+yyTop]));
                   }
    return yyVal;
}
public Object case38_line447(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new NotNode(getPosition(((Token)yyVals[-1+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
    return yyVal;
}
public Object case75_line564(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.aryset(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case249_line965(Object yyVal, Object[] yyVals, int yyTop) {
    return yyVal;
}
public Object case25_line389(Object yyVal, Object[] yyVals, int yyTop) {
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
public Object case282_line1065(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new HashNode(getPosition(((Token)yyVals[-2+yyTop])), ((ListNode)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case211_line834(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newAndNode(getPosition(((Token)yyVals[-1+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case467_line1790(Object yyVal, Object[] yyVals, int yyTop) {
                  if (((ListNode)yyVals[-1+yyTop]).size() % 2 != 0) {
                      yyerror("odd number list for Hash.");
                  }
                  yyVal = ((ListNode)yyVals[-1+yyTop]);
    return yyVal;
}
public Object case464_line1772(Object yyVal, Object[] yyVals, int yyTop) {
                  if (((Node)yyVals[-2+yyTop]) == null) {
                      yyerror("can't define single method for ().");
                  } else if (((Node)yyVals[-2+yyTop]) instanceof ILiteralNode) {
                      yyerror("can't define single method for literals.");
                  }
		  support.checkExpression(((Node)yyVals[-2+yyTop]));
                  yyVal = ((Node)yyVals[-2+yyTop]);
    return yyVal;
}
public Object case251_line970(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                  yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case202_line807(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop]), getPosition(null));
    return yyVal;
}
public Object case256_line984(Object yyVal, Object[] yyVals, int yyTop) {
                  warnings.warn(ID.ARGUMENT_EXTRA_SPACE, getPosition(((Token)yyVals[-3+yyTop])), "don't put space before argument parentheses");
		  yyVal = ((Node)yyVals[-2+yyTop]);
    return yyVal;
}
public Object case299_line1117(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newCaseNode(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case258_line994(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((BlockPassNode)yyVals[0+yyTop]);
    return yyVal;
}
public Object case180_line730(Object yyVal, Object[] yyVals, int yyTop) {
		  yyerror("constant re-assignment");
    return yyVal;
}
public Object case241_line934(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_concat(getPosition(((Node)yyVals[-6+yyTop])), support.newArrayNode(getPosition(((Node)yyVals[-6+yyTop])), ((Node)yyVals[-6+yyTop])).addAll(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                  yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
    return yyVal;
}
public Object case393_line1494(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = lexer.getStrTerm();
		   lexer.setStrTerm(null);
		   lexer.setState(LexState.EXPR_BEG);
    return yyVal;
}
public Object case287_line1080(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new DefinedNode(getPosition(((Token)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case274_line1027(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new BeginNode(getPosition(((Token)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]));
    return yyVal;
}
public Object case275_line1030(Object yyVal, Object[] yyVals, int yyTop) {
                  lexer.setState(LexState.EXPR_ENDARG); 
    return yyVal;
}
public Object case362_line1347(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case298_line1113(Object yyVal, Object[] yyVals, int yyTop) {
                  Node body = ((Node)yyVals[-1+yyTop]) == null ? NilImplicitNode.NIL : ((Node)yyVals[-1+yyTop]);
                  yyVal = new UntilNode(getPosition(((Token)yyVals[-6+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), body);
    return yyVal;
}
public Object case30_line418(Object yyVal, Object[] yyVals, int yyTop) {
                  support.backrefAssignError(((Node)yyVals[-2+yyTop]));
    return yyVal;
}
public Object case416_line1578(Object yyVal, Object[] yyVals, int yyTop) {
                   yyVal = new Token("nil", Tokens.kNIL, ((Token)yyVals[0+yyTop]).getPosition());
    return yyVal;
}
public Object case356_line1320(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.arg_concat(getPosition(((ListNode)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
    return yyVal;
}
public Object case6_line309(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.newline_node(((Node)yyVals[0+yyTop]), getPosition(((Node)yyVals[0+yyTop]), true));
    return yyVal;
}
public Object case102_line665(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = new UndefNode(getPosition(((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
    return yyVal;
}
public Object case365_line1353(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = ((Node)yyVals[0+yyTop]);
    return yyVal;
}
public Object case361_line1343(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = null;
    return yyVal;
}
public Object case187_line758(Object yyVal, Object[] yyVals, int yyTop) {
                  yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "/", ((Node)yyVals[0+yyTop]), getPosition(null));
    return yyVal;
}
public Object case315_line1187(Object yyVal, Object[] yyVals, int yyTop) {
                  support.setInSingle(support.getInSingle() + 1);
		  support.pushLocalScope();
                  lexer.setState(LexState.EXPR_END); /* force for args */
    return yyVal;
}
					// line 1842 "DefaultRubyParser.y"

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
            Object debugger = configuration.isDebug() ? new jay.yydebug.yyDebugAdapter() : null;
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
					// line 7720 "-"
