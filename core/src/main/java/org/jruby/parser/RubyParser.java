// created by jay 1.0.2 (c) 2002-2004 ats@cs.rit.edu
// skeleton Java 1.0 (c) 2002 ats@cs.rit.edu

					// line 2 "RubyParser.y"
/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2008-2017 Thomas E Enebo <enebo@acm.org>
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

import org.jruby.RubySymbol;
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
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.Colon3Node;
import org.jruby.ast.ConstNode;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.DefinedNode;
import org.jruby.ast.DStrNode;
import org.jruby.ast.DSymbolNode;
import org.jruby.ast.DXStrNode;
import org.jruby.ast.DefnNode;
import org.jruby.ast.DefsNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.EncodingNode;
import org.jruby.ast.EnsureNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.FalseNode;
import org.jruby.ast.FileNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FloatNode;
import org.jruby.ast.ForNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.KeywordArgNode;
import org.jruby.ast.LambdaNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.LiteralNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.NilImplicitNode;
import org.jruby.ast.NilNode;
import org.jruby.ast.Node;
import org.jruby.ast.NonLocalControlFlowNode;
import org.jruby.ast.NumericNode;
import org.jruby.ast.OptArgNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.PreExe19Node;
import org.jruby.ast.RationalNode;
import org.jruby.ast.RedoNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.RequiredKeywordArgumentValueNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RestArgNode;
import org.jruby.ast.RetryNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.StarNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.TrueNode;
import org.jruby.ast.UnnamedRestArgNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.VAliasNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.ZArrayNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.ast.types.ILiteralNode;
import org.jruby.common.IRubyWarnings;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.lexer.LexerSource;
import org.jruby.lexer.LexingCommon;
import org.jruby.lexer.yacc.RubyLexer;
import org.jruby.lexer.yacc.StackState;
import org.jruby.lexer.yacc.StrTerm;
import org.jruby.util.ByteList;
import org.jruby.util.CommonByteLists;
import org.jruby.util.KeyValuePair;
import org.jruby.util.StringSupport;
import static org.jruby.lexer.LexingCommon.EXPR_BEG;
import static org.jruby.lexer.LexingCommon.EXPR_FITEM;
import static org.jruby.lexer.LexingCommon.EXPR_FNAME;
import static org.jruby.lexer.LexingCommon.EXPR_ENDFN;
import static org.jruby.lexer.LexingCommon.EXPR_ENDARG;
import static org.jruby.lexer.LexingCommon.EXPR_END;
import static org.jruby.lexer.LexingCommon.EXPR_LABEL;
import static org.jruby.parser.ParserSupport.arg_blk_pass;
import static org.jruby.parser.ParserSupport.node_assign;

 
public class RubyParser {
    protected ParserSupport support;
    protected RubyLexer lexer;

    public RubyParser(LexerSource source, IRubyWarnings warnings) {
        this.support = new ParserSupport();
        this.lexer = new RubyLexer(support, source, warnings);
        support.setLexer(lexer);
        support.setWarnings(warnings);
    }

    @Deprecated
    public RubyParser(LexerSource source) {
        this(new ParserSupport(), source);
    }

    @Deprecated
    public RubyParser(ParserSupport support, LexerSource source) {
        this.support = support;
        lexer = new RubyLexer(support, source);
        support.setLexer(lexer);
    }

    public void setWarnings(IRubyWarnings warnings) {
        support.setWarnings(warnings);
        lexer.setWarnings(warnings);
    }
					// line 165 "-"
  // %token constants
  public static final int keyword_class = 257;
  public static final int keyword_module = 258;
  public static final int keyword_def = 259;
  public static final int keyword_undef = 260;
  public static final int keyword_begin = 261;
  public static final int keyword_rescue = 262;
  public static final int keyword_ensure = 263;
  public static final int keyword_end = 264;
  public static final int keyword_if = 265;
  public static final int keyword_unless = 266;
  public static final int keyword_then = 267;
  public static final int keyword_elsif = 268;
  public static final int keyword_else = 269;
  public static final int keyword_case = 270;
  public static final int keyword_when = 271;
  public static final int keyword_while = 272;
  public static final int keyword_until = 273;
  public static final int keyword_for = 274;
  public static final int keyword_break = 275;
  public static final int keyword_next = 276;
  public static final int keyword_redo = 277;
  public static final int keyword_retry = 278;
  public static final int keyword_in = 279;
  public static final int keyword_do = 280;
  public static final int keyword_do_cond = 281;
  public static final int keyword_do_block = 282;
  public static final int keyword_return = 283;
  public static final int keyword_yield = 284;
  public static final int keyword_super = 285;
  public static final int keyword_self = 286;
  public static final int keyword_nil = 287;
  public static final int keyword_true = 288;
  public static final int keyword_false = 289;
  public static final int keyword_and = 290;
  public static final int keyword_or = 291;
  public static final int keyword_not = 292;
  public static final int modifier_if = 293;
  public static final int modifier_unless = 294;
  public static final int modifier_while = 295;
  public static final int modifier_until = 296;
  public static final int modifier_rescue = 297;
  public static final int keyword_alias = 298;
  public static final int keyword_defined = 299;
  public static final int keyword_BEGIN = 300;
  public static final int keyword_END = 301;
  public static final int keyword__LINE__ = 302;
  public static final int keyword__FILE__ = 303;
  public static final int keyword__ENCODING__ = 304;
  public static final int keyword_do_lambda = 305;
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
  public static final int tANDDOT = 335;
  public static final int tCOLON2 = 336;
  public static final int tCOLON3 = 337;
  public static final int tOP_ASGN = 338;
  public static final int tASSOC = 339;
  public static final int tLPAREN = 340;
  public static final int tLPAREN2 = 341;
  public static final int tRPAREN = 342;
  public static final int tLPAREN_ARG = 343;
  public static final int tLBRACK = 344;
  public static final int tRBRACK = 345;
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
  public static final int tRCURLY = 363;
  public static final int tBACK_REF2 = 364;
  public static final int tSYMBEG = 365;
  public static final int tSTRING_BEG = 366;
  public static final int tXSTRING_BEG = 367;
  public static final int tREGEXP_BEG = 368;
  public static final int tWORDS_BEG = 369;
  public static final int tQWORDS_BEG = 370;
  public static final int tSTRING_DBEG = 371;
  public static final int tSTRING_DVAR = 372;
  public static final int tSTRING_END = 373;
  public static final int tLAMBDA = 374;
  public static final int tLAMBEG = 375;
  public static final int tNTH_REF = 376;
  public static final int tBACK_REF = 377;
  public static final int tSTRING_CONTENT = 378;
  public static final int tINTEGER = 379;
  public static final int tIMAGINARY = 380;
  public static final int tFLOAT = 381;
  public static final int tRATIONAL = 382;
  public static final int tREGEXP_END = 383;
  public static final int tSYMBOLS_BEG = 384;
  public static final int tQSYMBOLS_BEG = 385;
  public static final int tDSTAR = 386;
  public static final int tSTRING_DEND = 387;
  public static final int tLABEL_END = 388;
  public static final int tSP = 389;
  public static final int tLOWEST = 390;
  public static final int yyErrorCode = 256;

  /** number of final state.
    */
  protected static final int yyFinal = 1;

  /** parser tables.
      Order is mandated by <i>jay</i>.
    */
  protected static final short[] yyLhs = {
//yyLhs 656
    -1,   154,     0,   140,   141,   141,   141,   141,   142,   142,
   157,    37,    37,    36,    38,    38,    38,    38,    44,   158,
    44,   159,    39,    39,    39,    39,    39,    39,    39,    39,
    39,    39,    39,    39,    39,    39,    39,    39,    31,    31,
    31,    31,    31,    31,    31,    31,    62,    62,    62,    40,
    40,    40,    40,    40,    40,    45,   161,   163,    46,    32,
    32,    61,    61,   114,   149,    43,    43,    43,    43,    43,
    43,    43,    43,    43,    43,    43,   117,   117,   128,   128,
   118,   118,   118,   118,   118,   118,   118,   118,   118,   118,
    75,    75,   104,   104,   105,   105,    76,    76,    76,    76,
    76,    76,    76,    76,    76,    76,    76,    76,    76,    76,
    76,    76,    76,    76,    76,    81,    81,    81,    81,    81,
    81,    81,    81,    81,    81,    81,    81,    81,    81,    81,
    81,    81,    81,    81,     8,     8,    30,    30,    30,     7,
     7,     7,     7,     7,   121,   121,   122,   122,    65,   164,
    65,     6,     6,     6,     6,     6,     6,     6,     6,     6,
     6,     6,     6,     6,     6,     6,     6,     6,     6,     6,
     6,     6,     6,     6,     6,     6,     6,     6,     6,     6,
     6,   135,   135,   135,   135,   135,   135,   135,   135,   135,
   135,   135,   135,   135,   135,   135,   135,   135,   135,   135,
   135,   135,   135,   135,   135,   135,   135,   135,   135,   135,
   135,   135,   135,   135,   135,   135,   135,   135,   135,   135,
   135,   135,   135,    41,    41,    41,    41,    41,    41,    41,
    41,    41,    41,    41,    41,    41,    41,    41,    41,    41,
    41,    41,    41,    41,    41,    41,    41,    41,    41,    41,
    41,    41,    41,    41,    41,    41,    41,    41,    41,    41,
    41,    41,    41,    41,   137,   137,   137,   137,    52,    52,
    77,    80,    80,    80,    80,    63,    63,    55,    59,    59,
   131,   131,   131,   131,   131,    53,    53,    53,    53,    53,
   166,    57,   108,   107,   107,    83,    83,    83,    83,    35,
    35,    74,    74,    74,    42,    42,    42,    42,    42,    42,
    42,    42,    42,    42,    42,   167,    42,   168,    42,   169,
    42,    42,    42,    42,    42,    42,    42,    42,    42,    42,
    42,    42,    42,    42,    42,    42,    42,    42,    42,   171,
   172,    42,   173,   174,    42,    42,    42,   175,   176,    42,
   177,    42,   179,    42,   180,    42,   181,   182,    42,   183,
   184,    42,    42,    42,    42,    42,    47,   151,   153,   152,
   150,   170,   170,   170,   162,   162,    50,    50,    48,    48,
   130,   130,   132,   132,    88,    88,   133,   133,   133,   133,
   133,   133,   133,   133,   133,    95,    95,    95,    95,    94,
    94,    70,    70,    70,    70,    70,    70,    70,    70,    70,
    70,    70,    70,    70,    70,    70,    72,    72,    71,    71,
    71,   125,   125,   124,   124,   134,   134,   185,   186,   127,
    69,    69,   126,   126,   113,    60,    60,    60,    60,    22,
    22,    22,    22,    22,    22,    22,    22,    22,   112,   112,
   187,   188,   115,   189,   190,   116,    78,    49,    49,   119,
   119,    79,    79,    79,    51,    51,    54,    54,    28,    28,
    28,    15,    16,    16,    16,    17,    18,    19,    25,    85,
    85,    27,    27,    91,    89,    89,    26,    92,    84,    84,
    90,    90,    20,    20,    21,    21,    24,    24,    23,   191,
    23,   192,   193,   194,   195,    23,    66,    66,    66,    66,
     2,     1,     1,     1,     1,    29,    33,    33,    34,    34,
    34,    34,    58,    58,    58,    58,    58,    58,    58,    58,
    58,    58,    58,    58,   120,   120,   120,   120,   120,   120,
   120,   120,   120,   120,   120,   120,    67,    67,   196,    56,
    56,    73,   197,    73,    96,    96,    96,    96,    93,    93,
    68,    68,    68,    68,    68,    68,    68,    68,    68,    68,
    68,    68,    68,    68,    68,   136,   136,   136,   136,     9,
     9,   148,   123,   123,    86,    86,   145,    97,    97,    98,
    98,    99,    99,   100,   100,   143,   143,   144,   144,    64,
   129,   106,   106,    87,    87,    10,    10,    13,    13,    12,
    12,   111,   110,   110,    14,   198,    14,   101,   101,   102,
   102,   103,   103,   103,   103,     3,     3,     3,     4,     4,
     4,     4,     5,     5,     5,    11,    11,   146,   146,   147,
   147,   155,   155,   160,   160,   138,   139,   165,   165,   165,
   178,   178,   156,   156,    82,   109,
    }, yyLen = {
//yyLen 656
     2,     0,     2,     2,     1,     1,     3,     2,     1,     4,
     0,     6,     3,     2,     1,     1,     3,     2,     1,     0,
     5,     0,     4,     3,     3,     3,     2,     3,     3,     3,
     3,     3,     4,     1,     3,     3,     3,     1,     3,     3,
     6,     5,     5,     5,     5,     3,     1,     3,     1,     1,
     3,     3,     3,     2,     1,     1,     0,     0,     4,     1,
     1,     1,     4,     3,     1,     2,     3,     4,     5,     4,
     5,     2,     2,     2,     2,     2,     1,     3,     1,     3,
     1,     2,     3,     5,     2,     4,     2,     4,     1,     3,
     1,     3,     2,     3,     1,     3,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     4,     3,
     3,     3,     3,     2,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     4,     3,     3,
     3,     3,     2,     1,     1,     1,     2,     1,     3,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     0,
     4,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     3,     3,     6,     5,     5,     5,     5,
     4,     3,     3,     3,     2,     2,     3,     3,     3,     3,
     3,     3,     4,     2,     2,     3,     3,     3,     3,     1,
     3,     3,     3,     3,     3,     2,     2,     3,     3,     3,
     3,     3,     6,     1,     1,     1,     1,     1,     3,     3,
     1,     1,     2,     4,     2,     1,     3,     3,     1,     1,
     1,     1,     2,     4,     2,     1,     2,     2,     4,     1,
     0,     2,     2,     2,     1,     1,     2,     3,     4,     1,
     1,     3,     4,     2,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     0,     4,     0,     3,     0,
     4,     3,     3,     2,     3,     3,     1,     4,     3,     1,
     5,     4,     3,     2,     1,     2,     2,     6,     6,     0,
     0,     6,     0,     0,     6,     5,     4,     0,     0,     8,
     0,     6,     0,     7,     0,     5,     0,     0,     7,     0,
     0,     9,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     2,     1,     1,     1,     5,     1,     2,
     1,     1,     1,     3,     1,     3,     1,     4,     6,     3,
     5,     2,     4,     1,     3,     4,     2,     2,     1,     2,
     0,     6,     8,     4,     6,     4,     2,     6,     2,     4,
     6,     2,     4,     2,     4,     1,     1,     1,     3,     1,
     4,     1,     4,     1,     3,     1,     1,     0,     0,     4,
     4,     1,     3,     3,     3,     2,     4,     5,     5,     2,
     4,     4,     3,     3,     3,     2,     1,     4,     3,     3,
     0,     0,     4,     0,     0,     4,     5,     1,     1,     6,
     0,     1,     1,     1,     2,     1,     2,     1,     1,     1,
     1,     1,     1,     1,     2,     3,     3,     3,     4,     0,
     3,     1,     2,     4,     0,     3,     4,     4,     0,     3,
     0,     3,     0,     2,     0,     2,     0,     2,     1,     0,
     3,     0,     0,     0,     0,     7,     1,     1,     1,     1,
     2,     1,     1,     1,     1,     3,     1,     2,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     0,     4,
     0,     3,     0,     3,     4,     2,     2,     1,     2,     0,
     6,     8,     4,     6,     4,     6,     2,     4,     6,     2,
     4,     2,     4,     1,     0,     1,     1,     1,     1,     1,
     1,     1,     1,     3,     1,     3,     1,     2,     1,     2,
     1,     1,     3,     1,     3,     1,     1,     2,     1,     3,
     3,     1,     3,     1,     3,     1,     1,     2,     1,     1,
     1,     2,     2,     0,     1,     0,     4,     1,     2,     1,
     3,     3,     2,     4,     2,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     0,     1,     0,     1,     2,     2,     0,     1,     1,
     1,     1,     1,     2,     0,     0,
    }, yyDefRed = {
//yyDefRed 1110
     1,     0,     0,     0,   367,   369,     0,     0,   315,     0,
     0,     0,   339,   342,     0,     0,     0,   364,   365,   370,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   472,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   492,   494,   496,     0,     0,   427,   546,
   547,   518,   521,   519,   520,     0,     0,   469,    64,   305,
     0,   473,   306,   307,     0,   308,   309,   304,   470,    33,
    49,   468,   516,     0,     0,     0,     0,     0,     0,     0,
   312,     0,    60,     0,     0,    90,     0,     4,   310,   311,
     0,     0,    76,     0,     2,     0,     5,     0,     0,     0,
     0,     7,   191,   202,   192,   215,   188,   208,   198,   197,
   218,   219,   213,   196,   195,   190,   216,   220,   221,   200,
   189,   203,   207,   209,   201,   194,   210,   217,   212,     0,
     0,     0,     0,   187,   206,   205,   222,   186,   193,   184,
   185,     0,     0,     0,     0,   141,   524,   523,     0,   526,
   176,   177,   173,   154,   155,   156,   163,   160,   162,   157,
   158,   178,   179,   164,   165,   615,   170,   169,   153,   175,
   172,   171,   167,   168,   161,   159,   151,   174,   152,   180,
   166,   142,   356,     0,   614,   143,   211,   204,   214,   199,
   181,   182,   183,   139,   140,   145,   144,   147,     0,   146,
   148,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   650,   651,     0,     0,     0,   652,    56,
    56,   362,   363,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   366,     0,     0,   380,   381,     0,     0,   326,     0,
     0,     0,     0,   492,     0,     0,   285,    74,     0,     0,
     0,   619,   289,    75,     0,    72,     0,     0,   445,    71,
     0,   644,     0,     0,    21,     0,     0,     0,   243,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,    15,
    14,     0,     0,     0,     0,     0,   271,     0,     0,     0,
   617,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   256,    53,   255,   513,   512,   514,   510,   511,     0,     0,
     0,     0,   479,   488,   336,     0,   484,   490,   474,   453,
   450,   335,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   266,   267,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   265,   264,     0,     0,     0,     0,   453,   435,   637,   638,
     0,     0,     0,     0,   640,   639,     0,     0,    92,     0,
     0,     0,     0,     0,     0,     3,     0,   439,     0,   333,
    73,   528,   527,   529,   530,   532,   531,   533,     0,     0,
     0,     0,   137,     0,     0,   313,   354,     0,   357,   635,
   636,   359,   149,     0,     0,     0,   372,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   653,
   340,     0,   343,     0,   517,     0,     0,     0,     0,   347,
   622,   296,   292,     0,   624,     0,     0,   286,   294,     0,
   287,     0,   328,     0,   291,   281,   280,     0,     0,     0,
     0,   332,    52,    23,    25,    24,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   321,    13,
     0,     0,     0,   318,   324,     0,   648,   272,     0,   274,
   325,   618,     0,    94,     0,     0,     0,     0,     0,   501,
   499,   515,   498,   495,   475,   493,   476,   477,   497,     0,
     0,   580,   577,   576,   575,   578,   586,   595,     0,     0,
   606,   605,   610,   609,   596,   581,     0,     0,     0,   603,
   431,   428,     0,     0,   573,   593,     0,   557,   584,   579,
     0,     0,     0,     0,     0,     0,     0,   454,     0,   451,
    27,    28,    29,    30,    31,    50,    51,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   442,     0,   444,     0,     0,
   630,     0,     0,   631,   443,     0,   628,   629,     0,    48,
     0,     0,     0,    45,   231,     0,     0,     0,     0,    38,
   223,    35,   295,     0,     0,     0,     0,    93,    34,    36,
   299,     0,    39,   224,     6,   450,    66,     0,   134,     0,
   136,   548,   350,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   316,     0,   373,     0,     0,     0,     0,
     0,     0,     0,     0,   346,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,    56,     0,   621,     0,     0,
     0,   293,   620,   327,   645,     0,     0,   277,   331,    22,
     0,     9,    32,     0,   230,     0,     0,    16,   320,     0,
     0,     0,     0,     0,     0,     0,     0,   502,     0,   478,
   481,     0,   486,     0,     0,     0,   382,     0,   384,     0,
     0,   607,   611,     0,   571,     0,     0,   566,     0,   569,
     0,   555,   597,     0,   556,   587,     0,   483,     0,   487,
     0,   449,     0,   448,     0,     0,   434,     0,     0,   441,
     0,     0,     0,     0,     0,   279,     0,   440,   278,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,    91,
     0,     0,     0,     0,     0,     0,     0,     0,   138,     0,
     0,   616,     0,     0,     0,   360,   150,   462,     0,     0,
   463,     0,   368,    12,   467,    10,     0,   376,     0,   378,
     0,     0,     0,     0,     0,     0,     0,   345,     0,     0,
     0,   375,    57,   374,     0,     0,   348,   623,   298,   288,
     0,   330,     0,   273,    95,     0,   503,   506,   507,   508,
   500,   509,   480,   482,   489,     0,     0,     0,     0,   583,
     0,     0,     0,   558,   582,     0,     0,   429,     0,     0,
   585,     0,   604,     0,   594,   612,     0,   599,   485,   491,
   419,     0,   417,     0,   416,     0,     0,    44,   228,    43,
   229,    70,     0,   646,    41,   226,    42,   227,    68,   438,
   437,    47,     0,     0,     0,     0,     0,     0,     0,     0,
     0,    63,     0,     0,     0,   447,   355,     0,     0,     0,
     0,     0,     0,   465,   466,     0,     0,   337,   379,     0,
   338,   297,     0,   341,    58,   344,     0,     0,    20,   504,
   383,     0,     0,     0,   385,   430,     0,     0,   572,     0,
     0,     0,   564,     0,   562,     0,   567,   570,   554,     0,
     0,     0,   415,   591,     0,     0,   398,     0,   601,     0,
     0,     0,   455,   452,     0,     0,    40,     0,     0,     0,
   549,   351,   551,   358,   553,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   464,     0,     0,     0,   457,   456,   458,     0,     0,
     0,     0,     0,     0,   425,     0,   423,   426,   433,   432,
     0,     0,     0,     0,     0,   413,     0,     0,   408,     0,
   396,     0,   411,   418,   397,     0,     0,     0,     0,     0,
   353,     0,     0,     0,     0,     0,     0,    11,     0,   349,
     0,     0,     0,     0,   422,   565,     0,   560,   563,   568,
     0,   399,   420,     0,     0,   592,     0,     0,     0,   602,
   323,     0,     0,   361,     0,     0,     0,     0,     0,   459,
   377,   505,     0,   424,     0,     0,   414,     0,   405,     0,
   403,   395,     0,   409,   412,     0,     0,     0,   561,     0,
     0,     0,     0,   407,     0,   401,   404,   410,     0,   402,
    }, yyDgoto = {
//yyDgoto 199
     1,   346,    67,    68,   690,   615,   616,   206,   432,   555,
   556,   441,   557,   558,   193,    69,    70,    71,    72,    73,
   349,   348,    74,   533,   351,    75,    76,   731,    77,    78,
   433,    79,    80,    81,    82,   649,   443,   444,   307,   308,
    84,    85,    86,    87,   309,   227,   460,   299,   817,  1006,
   818,   922,    89,   485,   813,   617,   662,   285,    90,   777,
    91,    92,   639,   640,   559,   208,   850,   229,   560,   561,
   960,   882,   883,   803,   641,    94,    95,   278,   458,   809,
   315,   230,   310,   487,   540,   539,   562,   563,   737,   574,
   575,    98,    99,   744,  1025,  1061,   863,   565,   963,   964,
   566,   321,   488,   281,   100,   524,   965,   477,   282,   478,
   751,   567,   419,   397,   656,   578,   576,   101,   102,   672,
   231,   209,   210,   568,  1015,   860,   867,   354,   312,   968,
   266,   489,   738,   739,  1016,   195,   569,   395,   482,   771,
   104,   105,   106,   570,   571,   572,   665,   406,   864,   267,
   268,   109,   110,   820,     2,   236,   237,   925,   506,   496,
   483,   461,   832,   934,   670,   517,   286,   211,   314,   512,
   447,   239,   685,   240,   687,   695,   936,   795,   448,   793,
   666,   438,   668,   669,   920,   355,   745,   579,   764,   577,
   762,   728,   727,   846,   939,  1009,   794,   804,   437,
    }, yySindex = {
//yySindex 1110
     0,     0, 20015, 21314,     0,     0, 19380, 19770,     0, 22475,
 22475, 18725,     0,     0,  3987, 20404, 20404,     0,     0,     0,
  -209,  -165,     0,     0,     0,     0,    59, 19640,   177,  -150,
  -135,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 22604, 22604,  1118,  -113, 20145, 21443, 20794, 21184,  2955,
 22604, 22733, 19510,     0,     0,     0,   202,   249,     0,     0,
     0,     0,     0,     0,     0,   254,   270,     0,     0,     0,
   -96,     0,     0,     0,  -109,     0,     0,     0,     0,     0,
     0,     0,     0,  1160,   368,  4978,     0,     2,   487,   -24,
     0,   312,     0,   -43,   285,     0,   309,     0,     0,     0,
 22991,   323,     0,     8,     0,   152,     0,  -106, 20404, 23120,
 23249,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     7,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   287,     0,
     0, 20275,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   101, 20275,   368,   116,   632,   121,
   336,   132,   116,     0,     0,   152,    94,   350,     0,     0,
     0,     0,     0,  -209,  -165,     0,     0,     0,     0,   163,
   177,     0,     0,     0,     0,     0,     0,     0,     0,  1118,
   207,     0,   796,     0,     0,     0,   247,  -106,     0, 22604,
 22604, 22604, 22604,     0, 22604,  4978,     0,     0,   221,   525,
   554,     0,     0,     0, 17241,     0, 20404, 20404,     0,     0,
 18860,     0, 22475,   306,     0, 21572, 20015, 20275,     0,   824,
   286,   300,   281, 21443,     0, 20145,   280,   152,  1160,     0,
     0,     0,   177,  1160,   177,   284,     0,   182,   197,   221,
     0,   261,   197,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   321, 23378,  1065,     0,   605,
     0,     0,     0,     0,     0,     0,     0,     0,   800,   885,
  1060,   400,     0,     0,     0,  2966,     0,     0,     0,     0,
     0,     0, 22475, 22475, 22475, 22475, 21443, 22475, 22475, 22604,
 22604, 22604, 22604, 22604,     0,     0, 22604, 22604, 22604, 22604,
 22604, 22604, 22604, 22604, 22604, 22604, 22604, 22604, 22604, 22604,
     0,     0, 22604, 22604, 22604, 22604,     0,     0,     0,     0,
  3470, 20404,  5820, 22604,     0,     0, 23828, 22733,     0, 21701,
 20145, 18993,   612, 21701, 22733,     0, 19122,     0,   313,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
 22475,    65,     0,   316,  1089,     0,     0, 22475,     0,     0,
     0,     0,     0,   407,   413,   281,     0, 20275,   426,  5909,
 20404,  6279, 22604, 22604, 22604, 20275,    94, 21830,   439,     0,
     0, 22475,     0,   365,     0,     0,  6357, 20404,  6413,     0,
     0,     0,     0,  1214,     0, 22604, 20534,     0,     0, 20924,
     0,   177,     0,   369,     0,     0,     0,   674,   676,   177,
    62,     0,     0,     0,     0,     0, 19770, 22475,  4978,   358,
   364,  5909,  6279, 22604, 22604,  1160,   385,   177,     0,     0,
 19251,     0,   177,     0,     0, 21054,     0,     0, 21184,     0,
     0,     0,     0,     0,   687,  6783, 20404,  6861, 23378,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  1328,
  -118,     0,     0,     0,     0,     0,     0,     0,  1444,  3343,
     0,     0,     0,     0,     0,     0,   448,   449,   720,     0,
     0,     0,   721,   725,     0,     0,   745,     0,     0,     0,
   474,   750, 22604,   734,  1331,  -115,   537,     0,   442,     0,
     0,     0,     0,     0,     0,     0,     0,   286,  2836,  2836,
  2836,  2836,  3876,  5417,  2836,  2836,  4410,  4410,  1578,  1578,
   286,  2993,   286,   286,  1179,  1179,  2361,  2361, 13789,  2850,
   543,   480,     0,   483,  -165,     0,     0,     0,   177,   489,
     0,   512,  -165,     0,     0,  2850,     0,     0,  -165,     0,
   515,  4493,  1142,     0,     0,   -43,   797, 22604,  4493,     0,
     0,     0,     0,   807,   177, 23378,   816,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   368,     0,     0,
     0,     0,     0,  6917, 20404,  7287, 20275,    62,   524, 19900,
 19770, 21959,    41,     0,    87,     0,   526,   532,   177,   533,
   542,   593,   621,   110,     0, 20275,    85, 20275,     0,     0,
  -165,   177,     0,     0,  -165,     0, 22604,     0, 22604,   221,
   554,     0,     0,     0,     0, 20534, 20924,     0,     0,     0,
    62,     0,     0,   286,     0, 20015,     0,     0,     0,   197,
 23378,     0,     0,   177,     0,     0,   687,     0,   840,     0,
     0,   215,     0,   854,  1444,   451,     0,   825,     0,   177,
   177,     0,     0,  3409,     0,  -189,  3343,     0,  3343,     0,
  1057,     0,     0,   345,     0,     0, 22604,     0,   217,     0,
   856,     0,  -100,     0,  -100,   838,     0, 22733, 22733,     0,
   313,   559,   566, 22733, 22733,     0,   313,     0,     0,     2,
  -109, 21443, 22604,  7365, 20404,  7421, 22733,     0, 22088,     0,
   687, 23378,   553,   152, 22475, 20275,     0,     0,     0,   177,
   661,     0,  3343, 20275,  3343,     0,     0,     0,     0,   588,
     0, 20275,     0,     0,     0,     0, 22475,     0,   671,     0,
 20275, 22604, 22604,   609, 22604, 22604,   689,     0, 22217, 20275,
   692,     0,     0,     0,   694,     0,     0,     0,     0,     0,
   915,     0,   599,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   177,   857,   936,  1928,     0,
   649,   933,   953,     0,     0, 20275, 20275,     0,   964,   972,
     0,   982,     0,   953,     0,     0,   750,     0,     0,     0,
     0,  2033,     0, 20275,     0, 20275, 22604,     0,     0,     0,
     0,     0, 22733,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  4978,   480,   483,   177,   489,   512, 22604,     0,
   687,     0, 20275,   152,   764,     0,     0,   177,   772,   152,
   524, 23507,   116,     0,     0, 20275,   116,     0,     0, 22604,
     0,     0,    55,     0,     0,     0, 20275, 20924,     0,     0,
     0,   994,   857,   595,     0,     0,  1350,  3409,     0,   775,
   686,  3409,     0,  3343,     0,  3409,     0,     0,     0,  1001,
   177,  1012,     0,     0,  1029,  1031,     0,   724,     0,   750,
 23636,  1018,     0,     0,  4978,  4978,     0,   559,     0,   823,
     0,     0,     0,     0,     0, 20275,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   770,  1185,
     0,     0, 20275,   832, 20275,     0,     0,     0,   833, 20275,
   857,   994,   857,  1044,     0,   241,     0,     0,     0,     0,
   953,  1069,   953,   953,  3409,     0,   752,  3343,     0,  1057,
     0,  3343,     0,     0,     0,     0,     0,   810,  1286, 23636,
     0,   858,     0, 12847, 20404, 23716,   407,     0,    87,     0,
   741,   994,   857,  1350,     0,     0,  3409,     0,     0,     0,
  1079,     0,     0,  1085,  1090,     0,   750,  1092,  1079,     0,
     0, 23772,  1286,     0,     0,     0,   177,     0,     0,     0,
     0,     0,   994,     0,   953,  3409,     0,  3409,     0,  3343,
     0,     0,  3409,     0,     0,     0,     0,     0,     0,  1079,
  1098,  1079,  1079,     0,  3409,     0,     0,     0,  1079,     0,
    }, yyRindex = {
//yyRindex 1110
     0,     0,   233,     0,     0,     0,     0,     0,     0,     0,
     0,   872,     0,     0,     0, 11167, 11271,     0,     0,     0,
  5224,  4762, 12753, 12950, 13054, 13131, 22862,     0, 22346,     0,
     0, 13251, 13432, 13552,  5555,  3754, 13629, 13733,  5686, 13930,
     0,     0,     0,     0,     0,   156,   146,   808,   782,    68,
     0,     0,  1403,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
 10433,     0,     0,     0, 10555,     0,     0,     0,     0,     0,
     0,     0,     0,    58,  8521, 11633, 10739, 13355,     0, 14039,
     0, 17051,     0, 14141,     0,     0,     0,     0,     0,     0,
   185,     0,     0,     0,     0,    22,     0, 20664, 11383,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  1154,
  1939,  2020,  2485,     0,     0,     0,     0,     0,     0,     0,
     0,  3360,  3864,  4368,  4851,     0,     0,     0,  5313,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  8827,     0,
     0,  1022,  8291,  8413,  8597,  8719,  8903,  9025,  9209,  2611,
  9331,  9515,  2746,  9637,     0,   156,   931,     0,     0, 10127,
     0,     0,     0,     0,     0,   872,     0,   883,     0,     0,
     0,     0,     0, 10861,  9821,   726,  1091,  1190,  1263,     0,
   821,  1819,  2303,  2365,  1492,  2492,  2510,  2272,  2531,     0,
     0,     0,     0,  3033,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0, 16752,     0,     0,  1936, 16879,
 16879,     0,     0,     0,   815,     0,     0,   155,     0,     0,
   815,     0,     0,     0,     0,     0,    54,    54,     0,     0,
 11825, 11045, 14280,     0, 18187,   156,     0,  1752,   613,     0,
     0,   142,   815,   186,   815,     0,     0,   829,   829,     0,
     0,     0,   806,  1100,  2444,  5746,  6193,  6697,  7201,  7705,
  1828,  9395,  9398,  2191,  9422,     0,     0,     0,  9426,   200,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  -182,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
 11519, 11689,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,    82,     0,     0,     0,     0,     0,     0,     0,     0,
   156,   258,   357,     0,     0,     0,    33,     0,  1282,     0,
     0,     0,     0,     0,     0,     0,     0,     0, 17373, 17511,
     0,     0,     0, 18322,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   521,     0, 10249,     0,   347, 18053,     0,
    82,     0,     0,     0,     0,   732,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  3406,     0,    82,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   815,     0,     0,     0,     0,     0,    56,    56,   815,
   815,     0,     0,     0,     0,     0,     0,     0,  1386,     0,
     0,     0,     0,     0,     0,   631,     0,   815,     0,     0,
  2893,   129,   815,     0,     0,   836,     0,     0,  -141,     0,
     0,     0,  9547,     0,   610,     0,    82,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   179,
     0,     0,     0,     0,     0,     0,   497,     0,   238,     0,
     0,     0,   238,   238,     0,     0,   304,     0,     0,     0,
   682,   304,   102,   279,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0, 11934, 15606, 15736,
 15846, 15932, 16035,  2133, 16121, 16207, 16310, 16396, 14743, 14853,
 12051, 15016, 12177, 12294, 14389, 14503, 15126, 15256,  1119, 15366,
     0,  6059,  4127,  7571, 20664,     0,  4258,     0,   847,  6190,
     0,  6563,  5093,     0,     0, 15496,     0,     0,  7944,     0,
  9133, 16840,     0,     0,     0, 14630,     0,     0,  1402,     0,
     0,     0,     0,     0,   815,     0,   736,     0,     0,     0,
     0,  2294,     0,     0,     0,     0,     0,   154,     0, 17917,
     0,     0,     0,     0,    82,     0,  1022,   815,   790,     0,
     0,   303,   919,     0,   919,     0,  3119,  4631,   847,  3250,
  3623,   919,     0,     0,     0,   118,     0,   118,  2368,   735,
     0,   847,  4918,  5380,  9943,     0,     0,     0,     0,  2469,
 16879,     0,     0,     0,     0,   178,   204,     0,     0,     0,
   815,     0,     0, 12403,     0,    54,   140,     0,     0,   829,
     0,  8107,  2132,   847,  8507,  8813,   742,     0,     0,     0,
     0,     0,     0,     0,     0,   190,     0,   198,     0,   815,
    31,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0, 18456,     0, 18591,     0,     0,     0,     0,     0,
 16966, 12539,     0,     0,     0,     0, 17007,     0,     0, 17095,
  1738,     0,     0,     0,    82,     0,     0,  1584,     0,     0,
   743,     0,     0,     0,     0,  1022, 17645, 17783,     0,   847,
     0,     0,   206,  1022,   160,     0,     0,     0,   819,   295,
     0,   118,     0,     0,     0,     0,     0,     0,     0,     0,
   118,     0,     0,  8075,     0,     0,     0,     0,     0,  1067,
     0,     0,     0,     0,     0,   960,     0,     0,     0,     0,
    56,     0,     0,     0,     0,  9119,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   815,     0,   210,     0,     0,
     0,  -102,   238,     0,     0,  1022,    54,     0,   238,   238,
     0,   238,     0,   238,     0,     0,   304,     0,     0,     0,
     0,    27,     0,  1022,     0,    54,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0, 16482,  6694,  7702,   847,  7067,  7198,     0, 17149,
   837,     0,  1022,     0,     0,     0,     0,   815,     0,     0,
   790,     0,     0,     0,     0,   571,     0,     0,     0,     0,
     0,     0,   919,     0,     0,     0,   118,   232,     0,     0,
     0,   222,     0,   234,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,    78,
    27,    78,     0,     0,   109,    78,     0,     0,     0,   109,
    83,    92,     0,     0, 16580, 16666,     0, 12644, 17199,     0,
     0,     0,     0,     0,     0,  1022,  1234,  1475,  1668,  1802,
  1846,  1977,  2050,   394,  2210,  2439,   939,  8498,     0,     0,
  9683,     0,  1022,   919,   347,     0,     0,     0,     0,    79,
     0,   252,     0,   263,     0,   -22,     0,     0,     0,     0,
   238,   238,   238,   238,     0,     0,     0,   150,     0,     0,
     0,     0,     0,     0,     0,   684,  1030,     0,    99,     0,
     0,     0,  9701,     0,    82,     0,   521,     0,   919,     0,
     0,   264,     0,     0,     0,     0,     0,     0,     0,     0,
    78,     0,     0,    78,    78,     0,   109,    78,    78,     0,
     0,     0,   135,     0,  1463,  1572,   847,  1529,  2317,     0,
     0,     0,   268,     0,   238,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  8132,   643,   999,     0,    78,
    78,    78,    78,     0,     0,     0,     0,     0,    78,     0,
    }, yyGindex = {
//yyGindex 199
     0,     0,    16,     0,  -296,     0,    -3,    15,  -324,  -427,
     0,     0,     0,    28,     0,     0,     0,  1126,     0,     0,
   941,  1153,     0,  -268,     0,     0,     0,   635,     0,    38,
  1105,  -229,   -35,     0,    81,     0,    63,   148,     0,    29,
   138,  1669,     5,    -7,   708,    50,  -226,    25,  -587,     0,
   173,     0,     0,   628,   227,    23,     0,   -10,  1231,   614,
     0,     0,  -177,   531,  -650,     0,     0,  1101,  -453,     0,
     0,     0,   481,   326,  -329,   -82,    13,   934,  -449,     0,
     0,  1799,     4,    10,     0,     0,   427,   501,  -725,     0,
     0,     0,     0,    71,  2185,   373,  -273,   499,   231,     0,
     0,     0,    32,  -441,     0,  -443,   223,  -267,  -423,     0,
  -512,   224,   -73,   482,  -531,   607,   868,  1251,   -30,   244,
  1856,     0,   -15,  -683,     0,  -778,     0,     0,  -138,  -890,
     0,  -328,  -758,   536,   225,     0,  -835,  1198,  -131,  -636,
  -266,     0,    34,     0,   387,   887,   -71,     0,  -326,   556,
   822,     0,     0,   625,     0,   523,   -14,     0,     0,     0,
   -26,     0,     0,     0,     0,  -272,     0,     0,     0,     0,
  -222,     0,     0,     0,     0,     0,     0,     0,    -6,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,
    };
    protected static final short[] yyTable = YyTables.yyTable();
    protected static final short[] yyCheck = YyTables.yyCheck();

  /** maps symbol value to printable name.
      @see #yyExpecting
    */
  protected static final String[] yyNames = {
    "end-of-file",null,null,null,null,null,null,null,null,"escaped horizontal tab","'\\n'",
"escaped vertical tab","escaped form feed","escaped carriage return",null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,"' '",null,null,null,
    null,null,null,null,null,null,null,null,"','",null,null,null,null,
    null,null,null,null,null,null,null,null,null,"':'","';'",null,"'='",
    null,"'?'",null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,"'['","backslash",null,null,null,null,null,null,null,null,null,
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
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,"class","module","def","undef",
"begin","rescue","ensure","end",
"if","unless","then","elsif",
"else","case","when","while",
"until","for","break","next",
"redo","retry","in","do",
"do (for condition)","do (for block)","return","yield",
"super","self","nil","true",
"false","and","or","not",
"if (modifier)","unless (modifier)","while (modifier)","until (modifier)",
"rescue (modifier)","alias","defined","BEGIN",
"END","__LINE__","__FILE__",
"__ENCODING__","do (for lambda)","tIDENTIFIER","tFID",
    "tGVAR","tIVAR","tCONSTANT","tCVAR","tLABEL","tCHAR","unary+",
"unary-","tUMINUS_NUM","**","<=>","==","===","!=",">=",
"<=","&&","||","=~","!~","'.'","..","...",
"[]","[]=","<<",">>","&.","::",":: at EXPR_BEG",
    "tOP_ASGN","=>","'('","'('","')'","( arg",
"'['","']'","'{'","{ arg","'*'","'*'","'&'",
"'&'","'`'","'%'","'/'","'+'","'-'","'<'","'>'",
"'|'","'!'","'^'","'{'","'}'","'`'","tSYMBEG",
    "tSTRING_BEG","tXSTRING_BEG","tREGEXP_BEG","tWORDS_BEG","tQWORDS_BEG",
    "tSTRING_DBEG","tSTRING_DVAR","tSTRING_END","->","tLAMBEG",
    "tNTH_REF","tBACK_REF","tSTRING_CONTENT","tINTEGER","tIMAGINARY",
    "tFLOAT","tRATIONAL","tREGEXP_END","tSYMBOLS_BEG","tQSYMBOLS_BEG",
"**arg","tSTRING_DEND","tLABEL_END","escaped space","tLOWEST",
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
    "top_stmt : keyword_BEGIN tLCURLY top_compstmt tRCURLY",
    "$$2 :",
    "bodystmt : compstmt opt_rescue k_else $$2 compstmt opt_ensure",
    "bodystmt : compstmt opt_rescue opt_ensure",
    "compstmt : stmts opt_terms",
    "stmts : none",
    "stmts : stmt_or_begin",
    "stmts : stmts terms stmt_or_begin",
    "stmts : error stmt",
    "stmt_or_begin : stmt",
    "$$3 :",
    "stmt_or_begin : keyword_begin $$3 tLCURLY top_compstmt tRCURLY",
    "$$4 :",
    "stmt : keyword_alias fitem $$4 fitem",
    "stmt : keyword_alias tGVAR tGVAR",
    "stmt : keyword_alias tGVAR tBACK_REF",
    "stmt : keyword_alias tGVAR tNTH_REF",
    "stmt : keyword_undef undef_list",
    "stmt : stmt modifier_if expr_value",
    "stmt : stmt modifier_unless expr_value",
    "stmt : stmt modifier_while expr_value",
    "stmt : stmt modifier_until expr_value",
    "stmt : stmt modifier_rescue stmt",
    "stmt : keyword_END tLCURLY compstmt tRCURLY",
    "stmt : command_asgn",
    "stmt : mlhs '=' command_call",
    "stmt : lhs '=' mrhs",
    "stmt : mlhs '=' mrhs_arg",
    "stmt : expr",
    "command_asgn : lhs '=' command_rhs",
    "command_asgn : var_lhs tOP_ASGN command_rhs",
    "command_asgn : primary_value '[' opt_call_args rbracket tOP_ASGN command_rhs",
    "command_asgn : primary_value call_op tIDENTIFIER tOP_ASGN command_rhs",
    "command_asgn : primary_value call_op tCONSTANT tOP_ASGN command_rhs",
    "command_asgn : primary_value tCOLON2 tCONSTANT tOP_ASGN command_rhs",
    "command_asgn : primary_value tCOLON2 tIDENTIFIER tOP_ASGN command_rhs",
    "command_asgn : backref tOP_ASGN command_rhs",
    "command_rhs : command_call",
    "command_rhs : command_call modifier_rescue stmt",
    "command_rhs : command_asgn",
    "expr : command_call",
    "expr : expr keyword_and expr",
    "expr : expr keyword_or expr",
    "expr : keyword_not opt_nl expr",
    "expr : tBANG command_call",
    "expr : arg",
    "expr_value : expr",
    "$$5 :",
    "$$6 :",
    "expr_value_do : $$5 expr_value do $$6",
    "command_call : command",
    "command_call : block_command",
    "block_command : block_call",
    "block_command : block_call call_op2 operation2 command_args",
    "cmd_brace_block : tLBRACE_ARG brace_body tRCURLY",
    "fcall : operation",
    "command : fcall command_args",
    "command : fcall command_args cmd_brace_block",
    "command : primary_value call_op operation2 command_args",
    "command : primary_value call_op operation2 command_args cmd_brace_block",
    "command : primary_value tCOLON2 operation2 command_args",
    "command : primary_value tCOLON2 operation2 command_args cmd_brace_block",
    "command : keyword_super command_args",
    "command : keyword_yield command_args",
    "command : k_return call_args",
    "command : keyword_break call_args",
    "command : keyword_next call_args",
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
    "mlhs_node : tIDENTIFIER",
    "mlhs_node : tIVAR",
    "mlhs_node : tGVAR",
    "mlhs_node : tCONSTANT",
    "mlhs_node : tCVAR",
    "mlhs_node : keyword_nil",
    "mlhs_node : keyword_self",
    "mlhs_node : keyword_true",
    "mlhs_node : keyword_false",
    "mlhs_node : keyword__FILE__",
    "mlhs_node : keyword__LINE__",
    "mlhs_node : keyword__ENCODING__",
    "mlhs_node : primary_value '[' opt_call_args rbracket",
    "mlhs_node : primary_value call_op tIDENTIFIER",
    "mlhs_node : primary_value tCOLON2 tIDENTIFIER",
    "mlhs_node : primary_value call_op tCONSTANT",
    "mlhs_node : primary_value tCOLON2 tCONSTANT",
    "mlhs_node : tCOLON3 tCONSTANT",
    "mlhs_node : backref",
    "lhs : tIDENTIFIER",
    "lhs : tIVAR",
    "lhs : tGVAR",
    "lhs : tCONSTANT",
    "lhs : tCVAR",
    "lhs : keyword_nil",
    "lhs : keyword_self",
    "lhs : keyword_true",
    "lhs : keyword_false",
    "lhs : keyword__FILE__",
    "lhs : keyword__LINE__",
    "lhs : keyword__ENCODING__",
    "lhs : primary_value '[' opt_call_args rbracket",
    "lhs : primary_value call_op tIDENTIFIER",
    "lhs : primary_value tCOLON2 tIDENTIFIER",
    "lhs : primary_value call_op tCONSTANT",
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
    "$$7 :",
    "undef_list : undef_list ',' $$7 fitem",
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
    "reswords : keyword__LINE__",
    "reswords : keyword__FILE__",
    "reswords : keyword__ENCODING__",
    "reswords : keyword_BEGIN",
    "reswords : keyword_END",
    "reswords : keyword_alias",
    "reswords : keyword_and",
    "reswords : keyword_begin",
    "reswords : keyword_break",
    "reswords : keyword_case",
    "reswords : keyword_class",
    "reswords : keyword_def",
    "reswords : keyword_defined",
    "reswords : keyword_do",
    "reswords : keyword_else",
    "reswords : keyword_elsif",
    "reswords : keyword_end",
    "reswords : keyword_ensure",
    "reswords : keyword_false",
    "reswords : keyword_for",
    "reswords : keyword_in",
    "reswords : keyword_module",
    "reswords : keyword_next",
    "reswords : keyword_nil",
    "reswords : keyword_not",
    "reswords : keyword_or",
    "reswords : keyword_redo",
    "reswords : keyword_rescue",
    "reswords : keyword_retry",
    "reswords : keyword_return",
    "reswords : keyword_self",
    "reswords : keyword_super",
    "reswords : keyword_then",
    "reswords : keyword_true",
    "reswords : keyword_undef",
    "reswords : keyword_when",
    "reswords : keyword_yield",
    "reswords : keyword_if",
    "reswords : keyword_unless",
    "reswords : keyword_while",
    "reswords : keyword_until",
    "reswords : modifier_rescue",
    "arg : lhs '=' arg_rhs",
    "arg : var_lhs tOP_ASGN arg_rhs",
    "arg : primary_value '[' opt_call_args rbracket tOP_ASGN arg",
    "arg : primary_value call_op tIDENTIFIER tOP_ASGN arg_rhs",
    "arg : primary_value call_op tCONSTANT tOP_ASGN arg_rhs",
    "arg : primary_value tCOLON2 tIDENTIFIER tOP_ASGN arg_rhs",
    "arg : primary_value tCOLON2 tCONSTANT tOP_ASGN arg_rhs",
    "arg : tCOLON3 tCONSTANT tOP_ASGN arg_rhs",
    "arg : backref tOP_ASGN arg_rhs",
    "arg : arg tDOT2 arg",
    "arg : arg tDOT3 arg",
    "arg : arg tDOT2",
    "arg : arg tDOT3",
    "arg : arg tPLUS arg",
    "arg : arg tMINUS arg",
    "arg : arg tSTAR2 arg",
    "arg : arg tDIVIDE arg",
    "arg : arg tPERCENT arg",
    "arg : arg tPOW arg",
    "arg : tUMINUS_NUM simple_numeric tPOW arg",
    "arg : tUPLUS arg",
    "arg : tUMINUS arg",
    "arg : arg tPIPE arg",
    "arg : arg tCARET arg",
    "arg : arg tAMPER2 arg",
    "arg : arg tCMP arg",
    "arg : rel_expr",
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
    "arg : keyword_defined opt_nl arg",
    "arg : arg '?' arg opt_nl ':' arg",
    "arg : primary",
    "relop : tGT",
    "relop : tLT",
    "relop : tGEQ",
    "relop : tLEQ",
    "rel_expr : arg relop arg",
    "rel_expr : rel_expr relop arg",
    "arg_value : arg",
    "aref_args : none",
    "aref_args : args trailer",
    "aref_args : args ',' assocs trailer",
    "aref_args : assocs trailer",
    "arg_rhs : arg",
    "arg_rhs : arg modifier_rescue arg",
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
    "$$8 :",
    "command_args : $$8 call_args",
    "block_arg : tAMPER arg_value",
    "opt_block_arg : ',' block_arg",
    "opt_block_arg : none_block_pass",
    "args : arg_value",
    "args : tSTAR arg_value",
    "args : args ',' arg_value",
    "args : args ',' tSTAR arg_value",
    "mrhs_arg : mrhs",
    "mrhs_arg : arg_value",
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
    "$$9 :",
    "primary : keyword_begin $$9 bodystmt keyword_end",
    "$$10 :",
    "primary : tLPAREN_ARG $$10 rparen",
    "$$11 :",
    "primary : tLPAREN_ARG stmt $$11 rparen",
    "primary : tLPAREN compstmt tRPAREN",
    "primary : primary_value tCOLON2 tCONSTANT",
    "primary : tCOLON3 tCONSTANT",
    "primary : tLBRACK aref_args tRBRACK",
    "primary : tLBRACE assoc_list tRCURLY",
    "primary : k_return",
    "primary : keyword_yield tLPAREN2 call_args rparen",
    "primary : keyword_yield tLPAREN2 rparen",
    "primary : keyword_yield",
    "primary : keyword_defined opt_nl tLPAREN2 expr rparen",
    "primary : keyword_not tLPAREN2 expr rparen",
    "primary : keyword_not tLPAREN2 rparen",
    "primary : fcall brace_block",
    "primary : method_call",
    "primary : method_call brace_block",
    "primary : tLAMBDA lambda",
    "primary : keyword_if expr_value then compstmt if_tail keyword_end",
    "primary : keyword_unless expr_value then compstmt opt_else keyword_end",
    "$$12 :",
    "$$13 :",
    "primary : keyword_while $$12 expr_value_do $$13 compstmt keyword_end",
    "$$14 :",
    "$$15 :",
    "primary : keyword_until $$14 expr_value_do $$15 compstmt keyword_end",
    "primary : keyword_case expr_value opt_terms case_body keyword_end",
    "primary : keyword_case opt_terms case_body keyword_end",
    "$$16 :",
    "$$17 :",
    "primary : keyword_for for_var keyword_in $$16 expr_value_do $$17 compstmt keyword_end",
    "$$18 :",
    "primary : k_class cpath superclass $$18 bodystmt keyword_end",
    "$$19 :",
    "primary : k_class tLSHFT expr $$19 term bodystmt keyword_end",
    "$$20 :",
    "primary : k_module cpath $$20 bodystmt keyword_end",
    "$$21 :",
    "$$22 :",
    "primary : keyword_def fname $$21 $$22 f_arglist bodystmt keyword_end",
    "$$23 :",
    "$$24 :",
    "primary : keyword_def singleton dot_or_colon $$23 fname $$24 f_arglist bodystmt keyword_end",
    "primary : keyword_break",
    "primary : keyword_next",
    "primary : keyword_redo",
    "primary : keyword_retry",
    "primary_value : primary",
    "k_class : keyword_class",
    "k_else : keyword_else",
    "k_module : keyword_module",
    "k_return : keyword_return",
    "then : term",
    "then : keyword_then",
    "then : term keyword_then",
    "do : term",
    "do : keyword_do_cond",
    "if_tail : opt_else",
    "if_tail : keyword_elsif expr_value then compstmt if_tail",
    "opt_else : none",
    "opt_else : k_else compstmt",
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
    "$$25 :",
    "$$26 :",
    "lambda : $$25 f_larglist $$26 lambda_body",
    "f_larglist : tLPAREN2 f_args opt_bv_decl tRPAREN",
    "f_larglist : f_args",
    "lambda_body : tLAMBEG compstmt tRCURLY",
    "lambda_body : keyword_do_lambda bodystmt keyword_end",
    "do_block : keyword_do_block do_body keyword_end",
    "block_call : command do_block",
    "block_call : block_call call_op2 operation2 opt_paren_args",
    "block_call : block_call call_op2 operation2 opt_paren_args brace_block",
    "block_call : block_call call_op2 operation2 command_args do_block",
    "method_call : fcall paren_args",
    "method_call : primary_value call_op operation2 opt_paren_args",
    "method_call : primary_value tCOLON2 operation2 paren_args",
    "method_call : primary_value tCOLON2 operation3",
    "method_call : primary_value call_op paren_args",
    "method_call : primary_value tCOLON2 paren_args",
    "method_call : keyword_super paren_args",
    "method_call : keyword_super",
    "method_call : primary_value '[' opt_call_args rbracket",
    "brace_block : tLCURLY brace_body tRCURLY",
    "brace_block : keyword_do do_body keyword_end",
    "$$27 :",
    "$$28 :",
    "brace_body : $$27 $$28 opt_block_param compstmt",
    "$$29 :",
    "$$30 :",
    "do_body : $$29 $$30 opt_block_param bodystmt",
    "case_body : keyword_when args then compstmt cases",
    "cases : opt_else",
    "cases : case_body",
    "opt_rescue : keyword_rescue exc_list exc_var then compstmt opt_rescue",
    "opt_rescue :",
    "exc_list : arg_value",
    "exc_list : mrhs",
    "exc_list : none",
    "exc_var : tASSOC lhs",
    "exc_var : none",
    "opt_ensure : keyword_ensure compstmt",
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
    "words : tWORDS_BEG ' ' word_list tSTRING_END",
    "word_list :",
    "word_list : word_list word ' '",
    "word : string_content",
    "word : word string_content",
    "symbols : tSYMBOLS_BEG ' ' symbol_list tSTRING_END",
    "symbol_list :",
    "symbol_list : symbol_list word ' '",
    "qwords : tQWORDS_BEG ' ' qword_list tSTRING_END",
    "qsymbols : tQSYMBOLS_BEG ' ' qsym_list tSTRING_END",
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
    "$$31 :",
    "string_content : tSTRING_DVAR $$31 string_dvar",
    "$$32 :",
    "$$33 :",
    "$$34 :",
    "$$35 :",
    "string_content : tSTRING_DBEG $$32 $$33 $$34 $$35 compstmt tSTRING_DEND",
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
    "numeric : simple_numeric",
    "numeric : tUMINUS_NUM simple_numeric",
    "simple_numeric : tINTEGER",
    "simple_numeric : tFLOAT",
    "simple_numeric : tRATIONAL",
    "simple_numeric : tIMAGINARY",
    "var_ref : tIDENTIFIER",
    "var_ref : tIVAR",
    "var_ref : tGVAR",
    "var_ref : tCONSTANT",
    "var_ref : tCVAR",
    "var_ref : keyword_nil",
    "var_ref : keyword_self",
    "var_ref : keyword_true",
    "var_ref : keyword_false",
    "var_ref : keyword__FILE__",
    "var_ref : keyword__LINE__",
    "var_ref : keyword__ENCODING__",
    "var_lhs : tIDENTIFIER",
    "var_lhs : tIVAR",
    "var_lhs : tGVAR",
    "var_lhs : tCONSTANT",
    "var_lhs : tCVAR",
    "var_lhs : keyword_nil",
    "var_lhs : keyword_self",
    "var_lhs : keyword_true",
    "var_lhs : keyword_false",
    "var_lhs : keyword__FILE__",
    "var_lhs : keyword__LINE__",
    "var_lhs : keyword__ENCODING__",
    "backref : tNTH_REF",
    "backref : tBACK_REF",
    "$$36 :",
    "superclass : tLT $$36 expr_value term",
    "superclass :",
    "f_arglist : tLPAREN2 f_args rparen",
    "$$37 :",
    "f_arglist : $$37 f_args term",
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
    "f_arg_asgn : f_norm_arg",
    "f_arg_item : f_arg_asgn",
    "f_arg_item : tLPAREN f_margs rparen",
    "f_arg : f_arg_item",
    "f_arg : f_arg ',' f_arg_item",
    "f_label : tLABEL",
    "f_kw : f_label arg_value",
    "f_kw : f_label",
    "f_block_kw : f_label primary_value",
    "f_block_kw : f_label",
    "f_block_kwarg : f_block_kw",
    "f_block_kwarg : f_block_kwarg ',' f_block_kw",
    "f_kwarg : f_kw",
    "f_kwarg : f_kwarg ',' f_kw",
    "kwrest_mark : tPOW",
    "kwrest_mark : tDSTAR",
    "f_kwrest : kwrest_mark tIDENTIFIER",
    "f_kwrest : kwrest_mark",
    "f_opt : f_arg_asgn '=' arg_value",
    "f_block_opt : f_arg_asgn '=' primary_value",
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
    "$$38 :",
    "singleton : tLPAREN2 $$38 expr rparen",
    "assoc_list : none",
    "assoc_list : assocs trailer",
    "assocs : assoc",
    "assocs : assocs ',' assoc",
    "assoc : arg_value tASSOC arg_value",
    "assoc : tLABEL arg_value",
    "assoc : tSTRING_BEG string_contents tLABEL_END arg_value",
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
    "call_op : tDOT",
    "call_op : tANDDOT",
    "call_op2 : call_op",
    "call_op2 : tCOLON2",
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
      @param ayydebug debug message writer implementing <tt>yyDebug</tt>, or <tt>null</tt>.
      @return result of the last reduction, if any.
    */
  public Object yyparse (RubyLexer yyLex, Object ayydebug)
				throws java.io.IOException {
    this.yydebug = (org.jruby.parser.YYDebug) ayydebug;
    return yyparse(yyLex);
  }

  private static void initializeStates(ProductionState[] states, int start, int length) {
      for (int i = 0; i < length; i++) {
          states[start + i] = new ProductionState();
      }
  }

  private static void printstates(int yytop, ProductionState[] yystates) {
     for (int i = 0; i <= yytop; i++) {
         System.out.println("yytop: " + i + ", S/E: " +
             (yystates[i].start % 0xff) + "/" +
             (yystates[i].end % 0xff) +
             yystates[i].value);
     }
  }

  private static final int NEEDS_TOKEN = -1;
  private static final int DEFAULT = 0;
  private static final int YYMAX = 256;

  /** the generated parser.
      Maintains a dynamic state and value stack.
      @param yyLex scanner.
      @return result of the last reduction, if any.
    */
  public Object yyparse (RubyLexer yyLex) throws java.io.IOException {
    int yystate = 0;
    Object yyVal = null;
    ProductionState[] yystates = new ProductionState[YYMAX];        // stack of states and values.
    initializeStates(yystates, 0, yystates.length);
    int yytoken = NEEDS_TOKEN;     // current token
    int yyErrorFlag = 0;           // #tokens to shift
    int start = 0;
    int end = 0;

    yyLoop: for (int yytop = 0;; yytop++) {
      if (yytop + 1 >= yystates.length) {			// dynamically increase
          ProductionState[] newStates = new ProductionState[yystates.length+YYMAX];
          System.arraycopy(yystates, 0, newStates, 0, yystates.length);
          initializeStates(newStates, yystates.length, newStates.length - yystates.length);
          yystates = newStates;
      }

      yystates[yytop].state = yystate;
      yystates[yytop].value = yyVal;
      yystates[yytop].start = start;
      yystates[yytop].end = end;
   //         printstates(yytop, yystates);

      if (yydebug != null) yydebug.push(yystate, yyVal);

      yyDiscarded: for (;;) {	// discarding a token does not change stack
        int yyn = yyDefRed[yystate];
        if (yyn == DEFAULT) {	//ja else [default] reduce (yyn)
            if (yytoken == NEEDS_TOKEN) {
                yytoken = yyLex.nextToken();
                if (yydebug != null) yydebug.lex(yystate, yytoken, yyName(yytoken), yyLex.value());
            }

            yyn = yySindex[yystate];
            if (yyn != 0 &&
                (yyn += yytoken) >= 0 &&
                yyn < yyTable.length &&
                yyCheck[yyn] == yytoken) {
                if (yydebug != null) yydebug.shift(yystate, yyTable[yyn], yyErrorFlag-1);
                yystate = yyTable[yyn];		// shift to yyn
                yyVal = yyLex.value();
                start = yyLex.start;
                end = yyLex.end;
                yytoken = NEEDS_TOKEN;
                if (yyErrorFlag > 0) --yyErrorFlag;
                continue yyLoop;
            }

            yyn = yyRindex[yystate];
            if (yyn != 0 &&
                (yyn += yytoken) >= 0 &&
                 yyn < yyTable.length &&
                 yyCheck[yyn] == yytoken) {
                yyn = yyTable[yyn];			// reduce (yyn)
            } else {
                switch (yyErrorFlag) {
  
                case 0:
                    support.yyerror("syntax error", yyExpecting(yystate), yyNames[yytoken]);
                    if (yydebug != null) yydebug.error("syntax error");
                    // falls through...
                case 1: case 2:
                    yyErrorFlag = 3;
                    do {
                        yyn = yySindex[yystates[yytop].state];
                        if (yyn != 0 &&
                            (yyn += yyErrorCode) >= 0 &&
                            yyn < yyTable.length &&
                            yyCheck[yyn] == yyErrorCode) {
                            if (yydebug != null) yydebug.shift(yystates[yytop].state, yyTable[yyn], 3);
                            yystate = yyTable[yyn];
                            yyVal = yyLex.value();
                            continue yyLoop;
                        }
                        if (yydebug != null) yydebug.pop(yystates[yytop].state);
                    } while (--yytop >= 0);
                    if (yydebug != null) yydebug.reject();
                    support.yyerror("irrecoverable syntax error"); // throws
                case 3:
                    if (yytoken == 0) {
                        if (yydebug != null) yydebug.reject();
                        support.yyerror("irrecoverable syntax error at end-of-file");
                    }
                    if (yydebug != null) yydebug.discard(yystate, yytoken, yyName(yytoken), yyLex.value());
                    yytoken = NEEDS_TOKEN;
                    continue yyDiscarded; // leave stack alone
                }
            }
        }

        if (yydebug != null) yydebug.reduce(yystate, yystates[yytop-yyLen[yyn]].state, yyn, yyRule[yyn], yyLen[yyn]);

        ParserState parserState = states[yyn];
        if (parserState == null) {
            yyVal = yyLen[yyn] > 0 ? yystates[yytop - yyLen[yyn] + 1].value : null;
        } else {
            int count = yyLen[yyn];
            start = yystates[yytop - count + 1].start;
            end = yystates[yytop].end;
            yyVal = parserState.execute(support, lexer, yyVal, yystates, yytop, count, yytoken);
        }
// ACTIONS_END (line used by optimize_parser)
        yytop -= yyLen[yyn];
        yystate = yystates[yytop].state;
        int yyM = yyLhs[yyn];
        if (yystate == 0 && yyM == 0) {
            if (yydebug != null) yydebug.shift(0, yyFinal);
            yystate = yyFinal;
            if (yytoken == NEEDS_TOKEN) {
                yytoken = yyLex.nextToken();
                if (yydebug != null) yydebug.lex(yystate, yytoken,yyName(yytoken), yyLex.value());
            }
            if (yytoken == 0) {
                if (yydebug != null) yydebug.accept(yyVal);
                return yyVal;
            }
            continue yyLoop;
        }
        yyn = yyGindex[yyM];
        if (yyn != 0 &&
            (yyn += yystate) >= 0 &&
            yyn < yyTable.length &&
            yyCheck[yyn] == yystate) {
            yystate = yyTable[yyn];
        } else {
            yystate = yyDgoto[yyM];
        }

        if (yydebug != null) yydebug.shift(yystates[yytop].state, yystate);
        continue yyLoop;
      }
    }
  }

static ParserState[] states = new ParserState[656];
static {
states[1] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  lexer.setState(EXPR_BEG);
                  support.initTopLocalVariables();
  return yyVal;
};
states[2] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  if (((Node)yyVals[0+yyTop].value) != null && !support.getConfiguration().isEvalParse()) {
                      /* last expression should not be void */
                      if (((Node)yyVals[0+yyTop].value) instanceof BlockNode) {
                          support.void_expr(((BlockNode)yyVals[0+yyTop].value).getLast());
                      } else {
                          support.void_expr(((Node)yyVals[0+yyTop].value));
                      }
                  }
                  support.getResult().setAST(support.addRootNode(((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[3] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  yyVal = support.void_stmts(((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[5] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  yyVal = support.newline_node(((Node)yyVals[0+yyTop].value), support.getPosition(((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[6] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  yyVal = support.appendToBlock(((Node)yyVals[-2+yyTop].value), support.newline_node(((Node)yyVals[0+yyTop].value), support.getPosition(((Node)yyVals[0+yyTop].value))));
  return yyVal;
};
states[7] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[9] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.getResult().addBeginNode(new PreExe19Node(((Integer)yyVals[-3+yyTop].value), support.getCurrentScope(), ((Node)yyVals[-1+yyTop].value), lexer.getRubySourceline()));
                    yyVal = null;
  return yyVal;
};
states[10] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   if (((RescueBodyNode)yyVals[-1+yyTop].value) == null) support.yyerror("else without rescue is useless"); 
  return yyVal;
};
states[11] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = support.new_bodystmt(((Node)yyVals[-5+yyTop].value), ((RescueBodyNode)yyVals[-4+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[12] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_bodystmt(((Node)yyVals[-2+yyTop].value), ((RescueBodyNode)yyVals[-1+yyTop].value), null, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[13] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.void_stmts(((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[15] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newline_node(((Node)yyVals[0+yyTop].value), support.getPosition(((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[16] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.appendToBlock(((Node)yyVals[-2+yyTop].value), support.newline_node(((Node)yyVals[0+yyTop].value), support.getPosition(((Node)yyVals[0+yyTop].value))));
  return yyVal;
};
states[17] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[18] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[19] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   support.yyerror("BEGIN is permitted only at toplevel");
  return yyVal;
};
states[20] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new BeginNode(((Integer)yyVals[-4+yyTop].value), support.makeNullNil(((Node)yyVals[-3+yyTop].value)));
  return yyVal;
};
states[21] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setState(EXPR_FNAME|EXPR_FITEM);
  return yyVal;
};
states[22] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ParserSupport.newAlias(((Integer)yyVals[-3+yyTop].value), ((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[23] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new VAliasNode(((Integer)yyVals[-2+yyTop].value), support.symbolID(((ByteList)yyVals[-1+yyTop].value)), support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[24] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new VAliasNode(((Integer)yyVals[-2+yyTop].value), support.symbolID(((ByteList)yyVals[-1+yyTop].value)), support.symbolID(((BackRefNode)yyVals[0+yyTop].value).getByteName()));
  return yyVal;
};
states[25] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.yyerror("can't make alias for the number variables");
  return yyVal;
};
states[26] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[27] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_if(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.cond(((Node)yyVals[0+yyTop].value)), ((Node)yyVals[-2+yyTop].value), null);
                    support.fixpos(((Node)yyVal), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[28] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_if(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.cond(((Node)yyVals[0+yyTop].value)), null, ((Node)yyVals[-2+yyTop].value));
                    support.fixpos(((Node)yyVal), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[29] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (((Node)yyVals[-2+yyTop].value) != null && ((Node)yyVals[-2+yyTop].value) instanceof BeginNode) {
                        yyVal = new WhileNode(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.cond(((Node)yyVals[0+yyTop].value)), ((BeginNode)yyVals[-2+yyTop].value).getBodyNode(), false);
                    } else {
                        yyVal = new WhileNode(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.cond(((Node)yyVals[0+yyTop].value)), ((Node)yyVals[-2+yyTop].value), true);
                    }
  return yyVal;
};
states[30] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (((Node)yyVals[-2+yyTop].value) != null && ((Node)yyVals[-2+yyTop].value) instanceof BeginNode) {
                        yyVal = new UntilNode(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.cond(((Node)yyVals[0+yyTop].value)), ((BeginNode)yyVals[-2+yyTop].value).getBodyNode(), false);
                    } else {
                        yyVal = new UntilNode(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.cond(((Node)yyVals[0+yyTop].value)), ((Node)yyVals[-2+yyTop].value), true);
                    }
  return yyVal;
};
states[31] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newRescueModNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[32] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (support.isInDef()) {
                       support.warn(ID.END_IN_METHOD, ((Integer)yyVals[-3+yyTop].value), "END in method; use at_exit");
                    }
                    yyVal = new PostExeNode(((Integer)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[34] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = node_assign(((MultipleAsgnNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[35] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = node_assign(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[36] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = node_assign(((MultipleAsgnNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[38] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = node_assign(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[39] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_op_assign(((AssignableNode)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[40] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_ary_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[41] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_attr_op_assign(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value));
  return yyVal;
};
states[42] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_attr_op_assign(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value));
  return yyVal;
};
states[43] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    int line = ((Node)yyVals[-4+yyTop].value).getLine();
                    yyVal = support.new_const_op_assign(line, support.new_colon2(line, ((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-2+yyTop].value)), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[44] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_attr_op_assign(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value));
  return yyVal;
};
states[45] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.backrefAssignError(((Node)yyVals[-2+yyTop].value));
  return yyVal;
};
states[46] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[47] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[-2+yyTop].value));
                    yyVal = support.newRescueModNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[50] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newAndNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[51] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newOrNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[52] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(support.method_cond(((Node)yyVals[0+yyTop].value)), lexer.BANG);
  return yyVal;
};
states[53] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(support.method_cond(((Node)yyVals[0+yyTop].value)), ((ByteList)yyVals[-1+yyTop].value));
  return yyVal;
};
states[55] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[56] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.getConditionState().push1();
  return yyVal;
};
states[57] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.getConditionState().pop();
  return yyVal;
};
states[58] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-2+yyTop].value);
  return yyVal;
};
states[62] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null, (yyVals[yyTop - count + 3].start >> 16));
  return yyVal;
};
states[63] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IterNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[64] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_fcall(((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[65] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.frobnicate_fcall_args(((FCallNode)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
                    yyVal = ((FCallNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[66] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.frobnicate_fcall_args(((FCallNode)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value));
                    yyVal = ((FCallNode)yyVals[-2+yyTop].value);
  return yyVal;
};
states[67] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null, (yyVals[yyTop - count + 3].start >> 16));
  return yyVal;
};
states[68] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value), (yyVals[yyTop - count + 3].start >> 16));
  return yyVal;
};
states[69] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[70] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[71] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_super(((Integer)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[72] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_yield(((Integer)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[73] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ReturnNode(((Integer)yyVals[-1+yyTop].value), support.ret_args(((Node)yyVals[0+yyTop].value), ((Integer)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[74] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new BreakNode(((Integer)yyVals[-1+yyTop].value), support.ret_args(((Node)yyVals[0+yyTop].value), ((Integer)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[75] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new NextNode(((Integer)yyVals[-1+yyTop].value), support.ret_args(((Node)yyVals[0+yyTop].value), ((Integer)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[77] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[78] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((MultipleAsgnNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[79] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((Integer)yyVals[-2+yyTop].value), support.newArrayNode(((Integer)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value)), null, null);
  return yyVal;
};
states[80] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[0+yyTop].value).getLine(), ((ListNode)yyVals[0+yyTop].value), null, null);
  return yyVal;
};
states[81] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-1+yyTop].value).getLine(), ((ListNode)yyVals[-1+yyTop].value).add(((Node)yyVals[0+yyTop].value)), null, null);
  return yyVal;
};
states[82] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-2+yyTop].value).getLine(), ((ListNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value), (ListNode) null);
  return yyVal;
};
states[83] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-4+yyTop].value).getLine(), ((ListNode)yyVals[-4+yyTop].value), ((Node)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[84] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-1+yyTop].value).getLine(), ((ListNode)yyVals[-1+yyTop].value), new StarNode(lexer.getRubySourceline()), null);
  return yyVal;
};
states[85] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), new StarNode(lexer.getRubySourceline()), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[86] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((Node)yyVals[0+yyTop].value).getLine(), null, ((Node)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[87] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((Node)yyVals[-2+yyTop].value).getLine(), null, ((Node)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[88] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = new MultipleAsgnNode(lexer.getRubySourceline(), null, new StarNode(lexer.getRubySourceline()), null);
  return yyVal;
};
states[89] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = new MultipleAsgnNode(lexer.getRubySourceline(), null, new StarNode(lexer.getRubySourceline()), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[91] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[92] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newArrayNode(((Node)yyVals[-1+yyTop].value).getLine(), ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[93] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[94] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[95] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[96] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = support.assignableLabelOrIdentifier(((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[97] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = new InstAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[98] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = new GlobalAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[99] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (support.isInDef()) support.compile_error("dynamic constant assignment");
                    yyVal = new ConstDeclNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), null, NilImplicitNode.NIL);
  return yyVal;
};
states[100] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ClassVarAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[101] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to nil");
                    yyVal = null;
  return yyVal;
};
states[102] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't change the value of self");
                    yyVal = null;
  return yyVal;
};
states[103] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to true");
                    yyVal = null;
  return yyVal;
};
states[104] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to false");
                    yyVal = null;
  return yyVal;
};
states[105] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to __FILE__");
                    yyVal = null;
  return yyVal;
};
states[106] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to __LINE__");
                    yyVal = null;
  return yyVal;
};
states[107] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to __ENCODING__");
                    yyVal = null;
  return yyVal;
};
states[108] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.aryset(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[109] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[110] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[111] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[112] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (support.isInDef()) support.yyerror("dynamic constant assignment");

                    Integer position = support.getPosition(((Node)yyVals[-2+yyTop].value));

                    yyVal = new ConstDeclNode(position, (RubySymbol) null, support.new_colon2(position, ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[113] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (support.isInDef()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    Integer position = lexer.tokline;

                    yyVal = new ConstDeclNode(position, (RubySymbol) null, support.new_colon3(position, ((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[114] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.backrefAssignError(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[115] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.assignableLabelOrIdentifier(((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[116] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new InstAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[117] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new GlobalAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[118] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (support.isInDef()) support.compile_error("dynamic constant assignment");

                    yyVal = new ConstDeclNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), null, NilImplicitNode.NIL);
  return yyVal;
};
states[119] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ClassVarAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[120] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to nil");
                    yyVal = null;
  return yyVal;
};
states[121] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't change the value of self");
                    yyVal = null;
  return yyVal;
};
states[122] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to true");
                    yyVal = null;
  return yyVal;
};
states[123] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to false");
                    yyVal = null;
  return yyVal;
};
states[124] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to __FILE__");
                    yyVal = null;
  return yyVal;
};
states[125] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to __LINE__");
                    yyVal = null;
  return yyVal;
};
states[126] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to __ENCODING__");
                    yyVal = null;
  return yyVal;
};
states[127] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.aryset(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[128] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[129] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[130] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[131] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (support.isInDef()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    Integer position = support.getPosition(((Node)yyVals[-2+yyTop].value));

                    yyVal = new ConstDeclNode(position, (RubySymbol) null, support.new_colon2(position, ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[132] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (support.isInDef()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    Integer position = lexer.tokline;

                    yyVal = new ConstDeclNode(position, (RubySymbol) null, support.new_colon3(position, ((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[133] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.backrefAssignError(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[134] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.yyerror("class/module name must be CONSTANT", yyVals[yyTop - count + 1]);
  return yyVal;
};
states[135] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[136] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_colon3(lexer.tokline, ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[137] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_colon2(lexer.tokline, null, ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[138] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_colon2(support.getPosition(((Node)yyVals[-2+yyTop].value)), ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[139] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[140] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[141] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[142] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   lexer.setState(EXPR_ENDFN);
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[143] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   lexer.setState(EXPR_ENDFN);
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[144] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = new LiteralNode(lexer.getRubySourceline(), support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[145] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = new LiteralNode(lexer.getRubySourceline(), support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[146] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((LiteralNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[147] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[148] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ParserSupport.newUndef(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[149] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setState(EXPR_FNAME|EXPR_FITEM);
  return yyVal;
};
states[150] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.appendToBlock(((Node)yyVals[-3+yyTop].value), ParserSupport.newUndef(((Node)yyVals[-3+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[151] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[152] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[153] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[154] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[155] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[156] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[157] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[158] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[159] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[160] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[161] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[162] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[163] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[164] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[165] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[166] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[167] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[168] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[169] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[170] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[171] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[172] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[173] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[174] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[175] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[176] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[177] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[178] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[179] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[180] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[181] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.__LINE__.bytes;
  return yyVal;
};
states[182] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.__FILE__.bytes;
  return yyVal;
};
states[183] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.__ENCODING__.bytes;
  return yyVal;
};
states[184] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.LBEGIN.bytes;
  return yyVal;
};
states[185] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.LEND.bytes;
  return yyVal;
};
states[186] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.ALIAS.bytes;
  return yyVal;
};
states[187] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.AND.bytes;
  return yyVal;
};
states[188] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.BEGIN.bytes;
  return yyVal;
};
states[189] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.BREAK.bytes;
  return yyVal;
};
states[190] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.CASE.bytes;
  return yyVal;
};
states[191] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.CLASS.bytes;
  return yyVal;
};
states[192] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.DEF.bytes;
  return yyVal;
};
states[193] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.DEFINED_P.bytes;
  return yyVal;
};
states[194] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.DO.bytes;
  return yyVal;
};
states[195] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.ELSE.bytes;
  return yyVal;
};
states[196] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.ELSIF.bytes;
  return yyVal;
};
states[197] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.END.bytes;
  return yyVal;
};
states[198] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.ENSURE.bytes;
  return yyVal;
};
states[199] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.FALSE.bytes;
  return yyVal;
};
states[200] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.FOR.bytes;
  return yyVal;
};
states[201] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.IN.bytes;
  return yyVal;
};
states[202] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.MODULE.bytes;
  return yyVal;
};
states[203] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.NEXT.bytes;
  return yyVal;
};
states[204] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.NIL.bytes;
  return yyVal;
};
states[205] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.NOT.bytes;
  return yyVal;
};
states[206] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.OR.bytes;
  return yyVal;
};
states[207] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.REDO.bytes;
  return yyVal;
};
states[208] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.RESCUE.bytes;
  return yyVal;
};
states[209] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.RETRY.bytes;
  return yyVal;
};
states[210] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.RETURN.bytes;
  return yyVal;
};
states[211] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.SELF.bytes;
  return yyVal;
};
states[212] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.SUPER.bytes;
  return yyVal;
};
states[213] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.THEN.bytes;
  return yyVal;
};
states[214] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.TRUE.bytes;
  return yyVal;
};
states[215] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.UNDEF.bytes;
  return yyVal;
};
states[216] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.WHEN.bytes;
  return yyVal;
};
states[217] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.YIELD.bytes;
  return yyVal;
};
states[218] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.IF.bytes;
  return yyVal;
};
states[219] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.UNLESS.bytes;
  return yyVal;
};
states[220] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.WHILE.bytes;
  return yyVal;
};
states[221] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.UNTIL.bytes;
  return yyVal;
};
states[222] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.RESCUE.bytes;
  return yyVal;
};
states[223] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = node_assign(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[224] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_op_assign(((AssignableNode)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[225] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_ary_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[226] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_attr_op_assign(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value));
  return yyVal;
};
states[227] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_attr_op_assign(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value));
  return yyVal;
};
states[228] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_attr_op_assign(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value));
  return yyVal;
};
states[229] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Integer pos = support.getPosition(((Node)yyVals[-4+yyTop].value));
                    yyVal = support.new_const_op_assign(pos, support.new_colon2(pos, ((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-2+yyTop].value)), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[230] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Integer pos = lexer.getRubySourceline();
                    yyVal = support.new_const_op_assign(pos, new Colon3Node(pos, support.symbolID(((ByteList)yyVals[-2+yyTop].value))), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[231] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.backrefAssignError(((Node)yyVals[-2+yyTop].value));
  return yyVal;
};
states[232] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[-2+yyTop].value));
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
    
                    boolean isLiteral = ((Node)yyVals[-2+yyTop].value) instanceof FixnumNode && ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.makeNullNil(((Node)yyVals[-2+yyTop].value)), support.makeNullNil(((Node)yyVals[0+yyTop].value)), false, isLiteral);
  return yyVal;
};
states[233] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[-2+yyTop].value));
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));

                    boolean isLiteral = ((Node)yyVals[-2+yyTop].value) instanceof FixnumNode && ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.makeNullNil(((Node)yyVals[-2+yyTop].value)), support.makeNullNil(((Node)yyVals[0+yyTop].value)), true, isLiteral);
  return yyVal;
};
states[234] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[-1+yyTop].value));

                    boolean isLiteral = ((Node)yyVals[-1+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(support.getPosition(((Node)yyVals[-1+yyTop].value)), support.makeNullNil(((Node)yyVals[-1+yyTop].value)), NilImplicitNode.NIL, false, isLiteral);
  return yyVal;
};
states[235] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[-1+yyTop].value));

                    boolean isLiteral = ((Node)yyVals[-1+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(support.getPosition(((Node)yyVals[-1+yyTop].value)), support.makeNullNil(((Node)yyVals[-1+yyTop].value)), NilImplicitNode.NIL, true, isLiteral);
  return yyVal;
};
states[236] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[237] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[238] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[239] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[240] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[241] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[242] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((NumericNode)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline()), ((ByteList)yyVals[-3+yyTop].value));
  return yyVal;
};
states[243] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-1+yyTop].value));
  return yyVal;
};
states[244] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-1+yyTop].value));
  return yyVal;
};
states[245] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[246] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[247] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[248] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[249] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[250] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[251] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[252] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[253] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getMatchNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                  /* ENEBO
                        $$ = match_op($1, $3);
                        if (nd_type($1) == NODE_LIT && TYPE($1->nd_lit) == T_REGEXP) {
                            $$ = reg_named_capture_assign($1->nd_lit, $$);
                        }
                  */
  return yyVal;
};
states[254] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[255] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(support.method_cond(((Node)yyVals[0+yyTop].value)), ((ByteList)yyVals[-1+yyTop].value));
  return yyVal;
};
states[256] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-1+yyTop].value));
  return yyVal;
};
states[257] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[258] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[259] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newAndNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[260] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newOrNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[261] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new DefinedNode(((Integer)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[262] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[-5+yyTop].value));
                    yyVal = support.new_if(support.getPosition(((Node)yyVals[-5+yyTop].value)), support.cond(((Node)yyVals[-5+yyTop].value)), ((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[263] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[264] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[265] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[266] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[267] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[268] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[269] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     support.warning(ID.MISCELLANEOUS, lexer.getRubySourceline(), "comparison '" + ((ByteList)yyVals[-1+yyTop].value) + "' after comparison");
                     yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[270] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.makeNullNil(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[272] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[273] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.arg_append(((Node)yyVals[-3+yyTop].value), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[274] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newArrayNode(((HashNode)yyVals[-1+yyTop].value).getLine(), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[275] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[276] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[-2+yyTop].value));
                    yyVal = support.newRescueModNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[277] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
                    if (yyVal != null) ((Node)yyVal).setLine(((Integer)yyVals[-2+yyTop].value));
  return yyVal;
};
states[282] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[283] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.arg_append(((Node)yyVals[-3+yyTop].value), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[284] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newArrayNode(((HashNode)yyVals[-1+yyTop].value).getLine(), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[285] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.newArrayNode(support.getPosition(((Node)yyVals[0+yyTop].value)), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[286] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = arg_blk_pass(((Node)yyVals[-1+yyTop].value), ((BlockPassNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[287] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newArrayNode(((HashNode)yyVals[-1+yyTop].value).getLine(), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
                    yyVal = arg_blk_pass(((Node)yyVal), ((BlockPassNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[288] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.arg_append(((Node)yyVals[-3+yyTop].value), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
                    yyVal = arg_blk_pass(((Node)yyVal), ((BlockPassNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[289] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  return yyVal;
};
states[290] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    boolean lookahead = false;
                    switch (yychar) {
                    case tLPAREN2: case tLPAREN: case tLPAREN_ARG: case '[': case tLBRACK:
                       lookahead = true;
                    }
                    StackState cmdarg = lexer.getCmdArgumentState();
                    if (lookahead) cmdarg.pop();
                    cmdarg.push1();
                    if (lookahead) cmdarg.push0();
  return yyVal;
};
states[291] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    StackState cmdarg = lexer.getCmdArgumentState();
                    boolean lookahead = false;
                    switch (yychar) {
                    case tLBRACE_ARG:
                       lookahead = true;
                    }
                      
                    if (lookahead) cmdarg.pop();
                    cmdarg.pop();
                    if (lookahead) cmdarg.push0();
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[292] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new BlockPassNode(support.getPosition(((Node)yyVals[0+yyTop].value)), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[293] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((BlockPassNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[295] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    int line = ((Node)yyVals[0+yyTop].value) instanceof NilImplicitNode ? lexer.getRubySourceline() : ((Node)yyVals[0+yyTop].value).getLine();
                    yyVal = support.newArrayNode(line, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[296] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newSplatNode(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[297] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node node = support.splat_array(((Node)yyVals[-2+yyTop].value));

                    if (node != null) {
                        yyVal = support.list_append(node, ((Node)yyVals[0+yyTop].value));
                    } else {
                        yyVal = support.arg_append(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    }
  return yyVal;
};
states[298] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node node = null;

                    /* FIXME: lose syntactical elements here (and others like this)*/
                    if (((Node)yyVals[0+yyTop].value) instanceof ArrayNode &&
                        (node = support.splat_array(((Node)yyVals[-3+yyTop].value))) != null) {
                        yyVal = support.list_concat(node, ((Node)yyVals[0+yyTop].value));
                    } else {
                        yyVal = ParserSupport.arg_concat(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    }
  return yyVal;
};
states[299] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[300] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[301] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node node = support.splat_array(((Node)yyVals[-2+yyTop].value));

                    if (node != null) {
                        yyVal = support.list_append(node, ((Node)yyVals[0+yyTop].value));
                    } else {
                        yyVal = support.arg_append(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    }
  return yyVal;
};
states[302] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node node = null;

                    if (((Node)yyVals[0+yyTop].value) instanceof ArrayNode &&
                        (node = support.splat_array(((Node)yyVals[-3+yyTop].value))) != null) {
                        yyVal = support.list_concat(node, ((Node)yyVals[0+yyTop].value));
                    } else {
                        yyVal = ParserSupport.arg_concat(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    }
  return yyVal;
};
states[303] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = support.newSplatNode(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[310] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ListNode)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[311] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ListNode)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[314] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = support.new_fcall(((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[315] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.getCmdArgumentState().push0();
  return yyVal;
};
states[316] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.getCmdArgumentState().pop();
                    yyVal = new BeginNode(((Integer)yyVals[-3+yyTop].value), support.makeNullNil(((Node)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[317] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setState(EXPR_ENDARG);
  return yyVal;
};
states[318] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null; /*FIXME: Should be implicit nil?*/
  return yyVal;
};
states[319] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setState(EXPR_ENDARG); 
  return yyVal;
};
states[320] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-2+yyTop].value);
  return yyVal;
};
states[321] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (((Node)yyVals[-1+yyTop].value) != null) {
                        /* compstmt position includes both parens around it*/
                        ((Node)yyVals[-1+yyTop].value).setLine(((Integer)yyVals[-2+yyTop].value));
                        yyVal = ((Node)yyVals[-1+yyTop].value);
                    } else {
                        yyVal = new NilNode(((Integer)yyVals[-2+yyTop].value));
                    }
  return yyVal;
};
states[322] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_colon2(support.getPosition(((Node)yyVals[-2+yyTop].value)), ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[323] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_colon3(lexer.tokline, ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[324] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Integer position = support.getPosition(((Node)yyVals[-1+yyTop].value));
                    if (((Node)yyVals[-1+yyTop].value) == null) {
                        yyVal = new ZArrayNode(position); /* zero length array */
                    } else {
                        yyVal = ((Node)yyVals[-1+yyTop].value);
                    }
  return yyVal;
};
states[325] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((HashNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[326] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ReturnNode(((Integer)yyVals[0+yyTop].value), NilImplicitNode.NIL);
  return yyVal;
};
states[327] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_yield(((Integer)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[328] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new YieldNode(((Integer)yyVals[-2+yyTop].value), null);
  return yyVal;
};
states[329] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new YieldNode(((Integer)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[330] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new DefinedNode(((Integer)yyVals[-4+yyTop].value), ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[331] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(support.method_cond(((Node)yyVals[-1+yyTop].value)), lexer.BANG);
  return yyVal;
};
states[332] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(support.method_cond(NilImplicitNode.NIL), lexer.BANG);
  return yyVal;
};
states[333] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.frobnicate_fcall_args(((FCallNode)yyVals[-1+yyTop].value), null, ((IterNode)yyVals[0+yyTop].value));
                    yyVal = ((FCallNode)yyVals[-1+yyTop].value);                    
  return yyVal;
};
states[335] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (((Node)yyVals[-1+yyTop].value) != null && 
                          ((BlockAcceptingNode)yyVals[-1+yyTop].value).getIterNode() instanceof BlockPassNode) {
                          lexer.compile_error("Both block arg and actual block given.");
                    }
                    yyVal = ((BlockAcceptingNode)yyVals[-1+yyTop].value).setIterNode(((IterNode)yyVals[0+yyTop].value));
                    ((Node)yyVal).setLine(((Node)yyVals[-1+yyTop].value).getLine());
  return yyVal;
};
states[336] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((LambdaNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[337] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_if(((Integer)yyVals[-5+yyTop].value), support.cond(((Node)yyVals[-4+yyTop].value)), ((Node)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[338] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_if(((Integer)yyVals[-5+yyTop].value), support.cond(((Node)yyVals[-4+yyTop].value)), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[-2+yyTop].value));
  return yyVal;
};
states[339] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.getConditionState().push1();
  return yyVal;
};
states[340] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.getConditionState().pop();
  return yyVal;
};
states[341] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node body = support.makeNullNil(((Node)yyVals[-1+yyTop].value));
                    yyVal = new WhileNode(((Integer)yyVals[-5+yyTop].value), support.cond(((Node)yyVals[-3+yyTop].value)), body);
  return yyVal;
};
states[342] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  lexer.getConditionState().push1();
  return yyVal;
};
states[343] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  lexer.getConditionState().pop();
  return yyVal;
};
states[344] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node body = support.makeNullNil(((Node)yyVals[-1+yyTop].value));
                    yyVal = new UntilNode(((Integer)yyVals[-5+yyTop].value), support.cond(((Node)yyVals[-3+yyTop].value)), body);
  return yyVal;
};
states[345] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newCaseNode(((Integer)yyVals[-4+yyTop].value), ((Node)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value));
                    support.fixpos(((Node)yyVal), ((Node)yyVals[-3+yyTop].value));
  return yyVal;
};
states[346] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newCaseNode(((Integer)yyVals[-3+yyTop].value), null, ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[347] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.getConditionState().push1();
  return yyVal;
};
states[348] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.getConditionState().pop();
  return yyVal;
};
states[349] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      /* ENEBO: Lots of optz in 1.9 parser here*/
                    yyVal = new ForNode(((Integer)yyVals[-7+yyTop].value), ((Node)yyVals[-6+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[-3+yyTop].value), support.getCurrentScope(), lexer.getRubySourceline());
  return yyVal;
};
states[350] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (support.isInDef()) {
                        support.yyerror("class definition in method body");
                    }
                    support.pushLocalScope();
                    yyVal = support.isInClass(); /* MRI reuses $1 but we use the value for position.*/
                    support.setIsInClass(true);
  return yyVal;
};
states[351] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node body = support.makeNullNil(((Node)yyVals[-1+yyTop].value));

                    yyVal = new ClassNode(((Integer)yyVals[-5+yyTop].value), ((Colon3Node)yyVals[-4+yyTop].value), support.getCurrentScope(), body, ((Node)yyVals[-3+yyTop].value), lexer.getRubySourceline());
                    support.popCurrentScope();
                    support.setIsInClass(((Boolean)yyVals[-2+yyTop].value).booleanValue());
  return yyVal;
};
states[352] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new Integer((support.isInClass() ? 0b10 : 0) |
                                     (support.isInDef()   ? 0b01 : 0));
                    support.setInDef(false);
                    support.setIsInClass(false);
                    support.pushLocalScope();
  return yyVal;
};
states[353] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node body = support.makeNullNil(((Node)yyVals[-1+yyTop].value));

                    yyVal = new SClassNode(((Integer)yyVals[-6+yyTop].value), ((Node)yyVals[-4+yyTop].value), support.getCurrentScope(), body, lexer.getRubySourceline());
                    support.popCurrentScope();
                    support.setInDef(((((Integer)yyVals[-3+yyTop].value).intValue())     & 0b01) != 0);
                    support.setIsInClass(((((Integer)yyVals[-3+yyTop].value).intValue()) & 0b10) != 0);
  return yyVal;
};
states[354] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (support.isInDef()) { 
                        support.yyerror("module definition in method body");
                    }
                    yyVal = support.isInClass();
                    support.setIsInClass(true);
                    support.pushLocalScope();
  return yyVal;
};
states[355] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node body = support.makeNullNil(((Node)yyVals[-1+yyTop].value));

                    yyVal = new ModuleNode(((Integer)yyVals[-4+yyTop].value), ((Colon3Node)yyVals[-3+yyTop].value), support.getCurrentScope(), body, lexer.getRubySourceline());
                    support.popCurrentScope();
                    support.setIsInClass(((Boolean)yyVals[-2+yyTop].value).booleanValue());
  return yyVal;
};
states[356] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.isNextBreak = false;
                    support.pushLocalScope();
                    yyVal = lexer.getCurrentArg();
                    lexer.setCurrentArg(null);
  return yyVal;
};
states[357] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.isInDef();
                    support.setInDef(true);
  return yyVal;
};
states[358] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node body = support.makeNullNil(((Node)yyVals[-1+yyTop].value));

                    yyVal = new DefnNode(((Integer)yyVals[-6+yyTop].value), support.symbolID(((ByteList)yyVals[-5+yyTop].value)), (ArgsNode) yyVals[-2+yyTop].value, support.getCurrentScope(), body, ((Integer)yyVals[0+yyTop].value));
                    if (support.isNextBreak) ((DefnNode)yyVal).setContainsNextBreak();
                    support.isNextBreak = false;
                    support.popCurrentScope();
                    support.setInDef(((Boolean)yyVals[-3+yyTop].value).booleanValue());
                    lexer.setCurrentArg(((ByteList)yyVals[-4+yyTop].value));
  return yyVal;
};
states[359] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.isNextBreak = false;  
                    lexer.setState(EXPR_FNAME); 
                    yyVal = support.isInDef();
                    support.setInDef(true);
  return yyVal;
};
states[360] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.pushLocalScope();
                    lexer.setState(EXPR_ENDFN|EXPR_LABEL); /* force for args */
                    yyVal = lexer.getCurrentArg();
                    lexer.setCurrentArg(null);
  return yyVal;
};
states[361] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node body = ((Node)yyVals[-1+yyTop].value);
                    if (body == null) body = NilImplicitNode.NIL;

                    yyVal = new DefsNode(((Integer)yyVals[-8+yyTop].value), ((Node)yyVals[-7+yyTop].value), support.symbolID(((ByteList)yyVals[-4+yyTop].value)), (ArgsNode) yyVals[-2+yyTop].value, support.getCurrentScope(), body, ((Integer)yyVals[0+yyTop].value));
                    if (support.isNextBreak) ((DefsNode)yyVal).setContainsNextBreak();
                    support.isNextBreak = false;
                    support.popCurrentScope();
                    support.setInDef(((Boolean)yyVals[-5+yyTop].value).booleanValue());
                    lexer.setCurrentArg(((ByteList)yyVals[-3+yyTop].value));
  return yyVal;
};
states[362] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.isNextBreak = true;
                    yyVal = new BreakNode(((Integer)yyVals[0+yyTop].value), NilImplicitNode.NIL);
  return yyVal;
};
states[363] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.isNextBreak = true;
                    yyVal = new NextNode(((Integer)yyVals[0+yyTop].value), NilImplicitNode.NIL);
  return yyVal;
};
states[364] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new RedoNode(((Integer)yyVals[0+yyTop].value));
  return yyVal;
};
states[365] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new RetryNode(((Integer)yyVals[0+yyTop].value));
  return yyVal;
};
states[366] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = ((Node)yyVals[0+yyTop].value);
                    if (yyVal == null) yyVal = NilImplicitNode.NIL;
  return yyVal;
};
states[367] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[368] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[369] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[370] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (support.isInClass() && !support.isInDef() && !support.getCurrentScope().isBlockScope()) {
                        lexer.compile_error("Invalid return in class/module body");
                    }
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[377] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_if(((Integer)yyVals[-4+yyTop].value), support.cond(((Node)yyVals[-3+yyTop].value)), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[379] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[381] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  return yyVal;
};
states[382] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.assignableInCurr(((ByteList)yyVals[0+yyTop].value), NilImplicitNode.NIL);
  return yyVal;
};
states[383] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[384] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[385] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[386] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[0+yyTop].value).getLine(), ((ListNode)yyVals[0+yyTop].value), null, null);
  return yyVal;
};
states[387] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), support.assignableInCurr(((ByteList)yyVals[0+yyTop].value), null), null);
  return yyVal;
};
states[388] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-5+yyTop].value).getLine(), ((ListNode)yyVals[-5+yyTop].value), support.assignableInCurr(((ByteList)yyVals[-2+yyTop].value), null), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[389] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-2+yyTop].value).getLine(), ((ListNode)yyVals[-2+yyTop].value), new StarNode(lexer.getRubySourceline()), null);
  return yyVal;
};
states[390] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-4+yyTop].value).getLine(), ((ListNode)yyVals[-4+yyTop].value), new StarNode(lexer.getRubySourceline()), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[391] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(lexer.getRubySourceline(), null, support.assignableInCurr(((ByteList)yyVals[0+yyTop].value), null), null);
  return yyVal;
};
states[392] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(lexer.getRubySourceline(), null, support.assignableInCurr(((ByteList)yyVals[-2+yyTop].value), null), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[393] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(lexer.getRubySourceline(), null, new StarNode(lexer.getRubySourceline()), null);
  return yyVal;
};
states[394] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(support.getPosition(((ListNode)yyVals[0+yyTop].value)), null, null, ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[395] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args_tail(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[396] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args_tail(((ListNode)yyVals[-1+yyTop].value).getLine(), ((ListNode)yyVals[-1+yyTop].value), (ByteList) null, ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[397] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args_tail(lexer.getRubySourceline(), null, ((ByteList)yyVals[-1+yyTop].value), ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[398] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args_tail(((BlockArgNode)yyVals[0+yyTop].value).getLine(), null, (ByteList) null, ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[399] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[400] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args_tail(lexer.getRubySourceline(), null, (ByteList) null, null);
  return yyVal;
};
states[401] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), ((ListNode)yyVals[-5+yyTop].value), ((ListNode)yyVals[-3+yyTop].value), ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[402] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-7+yyTop].value).getLine(), ((ListNode)yyVals[-7+yyTop].value), ((ListNode)yyVals[-5+yyTop].value), ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[403] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[404] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), ((ListNode)yyVals[-5+yyTop].value), ((ListNode)yyVals[-3+yyTop].value), null, ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[405] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), null, ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[406] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    RestArgNode rest = new UnnamedRestArgNode(((ListNode)yyVals[-1+yyTop].value).getLine(), null, support.getCurrentScope().addVariable("*"));
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop].value).getLine(), ((ListNode)yyVals[-1+yyTop].value), null, rest, null, (ArgsTailHolder) null);
  return yyVal;
};
states[407] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), ((ListNode)yyVals[-5+yyTop].value), null, ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[408] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop].value).getLine(), ((ListNode)yyVals[-1+yyTop].value), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[409] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(support.getPosition(((ListNode)yyVals[-3+yyTop].value)), null, ((ListNode)yyVals[-3+yyTop].value), ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[410] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(support.getPosition(((ListNode)yyVals[-5+yyTop].value)), null, ((ListNode)yyVals[-5+yyTop].value), ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[411] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(support.getPosition(((ListNode)yyVals[-1+yyTop].value)), null, ((ListNode)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[412] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), null, ((ListNode)yyVals[-3+yyTop].value), null, ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[413] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((RestArgNode)yyVals[-1+yyTop].value).getLine(), null, null, ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[414] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((RestArgNode)yyVals[-3+yyTop].value).getLine(), null, null, ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[415] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ArgsTailHolder)yyVals[0+yyTop].value).getLine(), null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[416] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
    /* was $$ = null;*/
                    yyVal = support.new_args(lexer.getRubySourceline(), null, null, null, null, (ArgsTailHolder) null);
  return yyVal;
};
states[417] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.commandStart = true;
                    yyVal = ((ArgsNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[418] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setCurrentArg(null);
                    yyVal = support.new_args(lexer.getRubySourceline(), null, null, null, null, (ArgsTailHolder) null);
  return yyVal;
};
states[419] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(lexer.getRubySourceline(), null, null, null, null, (ArgsTailHolder) null);
  return yyVal;
};
states[420] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setCurrentArg(null);
                    yyVal = ((ArgsNode)yyVals[-2+yyTop].value);
  return yyVal;
};
states[421] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[422] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[423] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[424] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[425] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.new_bv(((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[426] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[427] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.pushBlockScope();
                    yyVal = lexer.getLeftParenBegin();
                    lexer.setLeftParenBegin(lexer.incrementParenNest());
  return yyVal;
};
states[428] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.getCmdArgumentState().push0();
  return yyVal;
};
states[429] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.getCmdArgumentState().pop();
                    yyVal = new LambdaNode(((ArgsNode)yyVals[-2+yyTop].value).getLine(), ((ArgsNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value), support.getCurrentScope(), lexer.getRubySourceline());
                    lexer.setLeftParenBegin(((Integer)yyVals[-3+yyTop].value));
                    support.popCurrentScope();
  return yyVal;
};
states[430] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ArgsNode)yyVals[-2+yyTop].value);
  return yyVal;
};
states[431] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ArgsNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[432] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[433] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[434] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IterNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[435] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    /* Workaround for JRUBY-2326 (MRI does not enter this production for some reason)*/
                    if (((Node)yyVals[-1+yyTop].value) instanceof YieldNode) {
                        lexer.compile_error("block given to yield");
                    }
                    if (((Node)yyVals[-1+yyTop].value) instanceof BlockAcceptingNode && ((BlockAcceptingNode)yyVals[-1+yyTop].value).getIterNode() instanceof BlockPassNode) {
                        lexer.compile_error("Both block arg and actual block given.");
                    }
                    if (((Node)yyVals[-1+yyTop].value) instanceof NonLocalControlFlowNode) {
                        ((BlockAcceptingNode) ((NonLocalControlFlowNode)yyVals[-1+yyTop].value).getValueNode()).setIterNode(((IterNode)yyVals[0+yyTop].value));
                    } else {
                        ((BlockAcceptingNode)yyVals[-1+yyTop].value).setIterNode(((IterNode)yyVals[0+yyTop].value));
                    }
                    yyVal = ((Node)yyVals[-1+yyTop].value);
                    ((Node)yyVal).setLine(((Node)yyVals[-1+yyTop].value).getLine());
  return yyVal;
};
states[436] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null, (yyVals[yyTop - count + 3].start >> 16));
  return yyVal;
};
states[437] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value), (yyVals[yyTop - count + 3].start >> 16));
  return yyVal;
};
states[438] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value), (yyVals[yyTop - count + 3].start >> 16));
  return yyVal;
};
states[439] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.frobnicate_fcall_args(((FCallNode)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
                    yyVal = ((FCallNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[440] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null, (yyVals[yyTop - count + 3].start >> 16));
  return yyVal;
};
states[441] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[442] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value), null, null);
  return yyVal;
};
states[443] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), LexingCommon.CALL, ((Node)yyVals[0+yyTop].value), null, (yyVals[yyTop - count + 3].start >> 16));
  return yyVal;
};
states[444] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop].value), LexingCommon.CALL, ((Node)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[445] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_super(((Integer)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[446] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ZSuperNode(((Integer)yyVals[0+yyTop].value));
  return yyVal;
};
states[447] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (((Node)yyVals[-3+yyTop].value) instanceof SelfNode) {
                        yyVal = support.new_fcall(LexingCommon.LBRACKET_RBRACKET);
                        support.frobnicate_fcall_args(((FCallNode)yyVal), ((Node)yyVals[-1+yyTop].value), null);
                    } else {
                        yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), lexer.LBRACKET_RBRACKET, ((Node)yyVals[-1+yyTop].value), null);
                    }
  return yyVal;
};
states[448] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IterNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[449] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IterNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[450] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = lexer.getRubySourceline();
  return yyVal;
};
states[451] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.pushBlockScope();
  return yyVal;
};
states[452] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new IterNode(((Integer)yyVals[-3+yyTop].value), ((ArgsNode)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), support.getCurrentScope(), lexer.getRubySourceline());
                    support.popCurrentScope();
  return yyVal;
};
states[453] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = lexer.getRubySourceline();
  return yyVal;
};
states[454] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.pushBlockScope();
                    lexer.getCmdArgumentState().push0();
  return yyVal;
};
states[455] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new IterNode(((Integer)yyVals[-3+yyTop].value), ((ArgsNode)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), support.getCurrentScope(), lexer.getRubySourceline());
                    lexer.getCmdArgumentState().pop();
                    support.popCurrentScope();
  return yyVal;
};
states[456] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newWhenNode(((Integer)yyVals[-4+yyTop].value), ((Node)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[459] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node node;
                    if (((Node)yyVals[-3+yyTop].value) != null) {
                        node = support.appendToBlock(node_assign(((Node)yyVals[-3+yyTop].value), new GlobalVarNode(((Integer)yyVals[-5+yyTop].value), support.symbolID(lexer.DOLLAR_BANG))), ((Node)yyVals[-1+yyTop].value));
                        if (((Node)yyVals[-1+yyTop].value) != null) {
                            node.setLine(((Integer)yyVals[-5+yyTop].value));
                        }
                    } else {
                        node = ((Node)yyVals[-1+yyTop].value);
                    }
                    Node body = support.makeNullNil(node);
                    yyVal = new RescueBodyNode(((Integer)yyVals[-5+yyTop].value), ((Node)yyVals[-4+yyTop].value), body, ((RescueBodyNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[460] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null; 
  return yyVal;
};
states[461] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[462] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.splat_array(((Node)yyVals[0+yyTop].value));
                    if (yyVal == null) yyVal = ((Node)yyVals[0+yyTop].value); /* ArgsCat or ArgsPush*/
  return yyVal;
};
states[464] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[466] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[468] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((NumericNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[469] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.asSymbol(lexer.getRubySourceline(), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[471] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value) instanceof EvStrNode ? new DStrNode(((Node)yyVals[0+yyTop].value).getLine(), lexer.getEncoding()).add(((Node)yyVals[0+yyTop].value)) : ((Node)yyVals[0+yyTop].value);
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
};
states[472] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((StrNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[473] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[474] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[475] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.heredoc_dedent(((Node)yyVals[-1+yyTop].value));
		    lexer.setHeredocIndent(0);
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[476] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    int line = support.getPosition(((Node)yyVals[-1+yyTop].value));

                    lexer.heredoc_dedent(((Node)yyVals[-1+yyTop].value));
		    lexer.setHeredocIndent(0);

                    if (((Node)yyVals[-1+yyTop].value) == null) {
                        yyVal = new XStrNode(line, null, StringSupport.CR_7BIT);
                    } else if (((Node)yyVals[-1+yyTop].value) instanceof StrNode) {
                        yyVal = new XStrNode(line, (ByteList) ((StrNode)yyVals[-1+yyTop].value).getValue().clone(), ((StrNode)yyVals[-1+yyTop].value).getCodeRange());
                    } else if (((Node)yyVals[-1+yyTop].value) instanceof DStrNode) {
                        yyVal = new DXStrNode(line, ((DStrNode)yyVals[-1+yyTop].value));

                        ((Node)yyVal).setLine(line);
                    } else {
                        yyVal = new DXStrNode(line).add(((Node)yyVals[-1+yyTop].value));
                    }
  return yyVal;
};
states[477] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newRegexpNode(support.getPosition(((Node)yyVals[-1+yyTop].value)), ((Node)yyVals[-1+yyTop].value), (RegexpNode) ((RegexpNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[478] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[479] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = new ArrayNode(lexer.getRubySourceline());
  return yyVal;
};
states[480] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[-1+yyTop].value) instanceof EvStrNode ? new DStrNode(((ListNode)yyVals[-2+yyTop].value).getLine(), lexer.getEncoding()).add(((Node)yyVals[-1+yyTop].value)) : ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[481] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[482] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = support.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[483] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[484] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ArrayNode(lexer.getRubySourceline());
  return yyVal;
};
states[485] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[-1+yyTop].value) instanceof EvStrNode ? new DSymbolNode(((ListNode)yyVals[-2+yyTop].value).getLine()).add(((Node)yyVals[-1+yyTop].value)) : support.asSymbol(((ListNode)yyVals[-2+yyTop].value).getLine(), ((Node)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[486] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[487] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[488] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ArrayNode(lexer.getRubySourceline());
  return yyVal;
};
states[489] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[490] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ArrayNode(lexer.getRubySourceline());
  return yyVal;
};
states[491] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(support.asSymbol(((ListNode)yyVals[-2+yyTop].value).getLine(), ((Node)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[492] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    ByteList aChar = ByteList.create("");
                    aChar.setEncoding(lexer.getEncoding());
                    yyVal = lexer.createStr(aChar, 0);
  return yyVal;
};
states[493] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[494] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    ByteList aChar = ByteList.create("");
                    aChar.setEncoding(lexer.getEncoding());
                    yyVal = lexer.createStr(aChar, 0);
  return yyVal;
};
states[495] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[496] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[497] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
    /* FIXME: mri is different here.*/
                    yyVal = support.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[498] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[499] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = lexer.getStrTerm();
                    lexer.setStrTerm(null);
                    lexer.setState(EXPR_BEG);
  return yyVal;
};
states[500] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setStrTerm(((StrTerm)yyVals[-1+yyTop].value));
                    yyVal = new EvStrNode(support.getPosition(((Node)yyVals[0+yyTop].value)), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[501] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = lexer.getStrTerm();
                   lexer.setStrTerm(null);
                   lexer.getConditionState().push0();
                   lexer.getCmdArgumentState().push0();
  return yyVal;
};
states[502] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = lexer.getState();
                   lexer.setState(EXPR_BEG);
  return yyVal;
};
states[503] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = lexer.getBraceNest();
                   lexer.setBraceNest(0);
  return yyVal;
};
states[504] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = lexer.getHeredocIndent();
                   lexer.setHeredocIndent(0);
  return yyVal;
};
states[505] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   lexer.getConditionState().pop();
                   lexer.getCmdArgumentState().pop();
                   lexer.setStrTerm(((StrTerm)yyVals[-5+yyTop].value));
                   lexer.setState(((Integer)yyVals[-4+yyTop].value));
                   lexer.setBraceNest(((Integer)yyVals[-3+yyTop].value));
                   lexer.setHeredocIndent(((Integer)yyVals[-2+yyTop].value));
                   lexer.setHeredocLineIndent(-1);

                   if (((Node)yyVals[-1+yyTop].value) != null) ((Node)yyVals[-1+yyTop].value).unsetNewline();
                   yyVal = support.newEvStrNode(support.getPosition(((Node)yyVals[-1+yyTop].value)), ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[506] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = new GlobalVarNode(lexer.getRubySourceline(), support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[507] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = new InstVarNode(lexer.getRubySourceline(), support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[508] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = new ClassVarNode(lexer.getRubySourceline(), support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[510] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     lexer.setState(EXPR_END);
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[512] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[513] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[514] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[515] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     lexer.setState(EXPR_END);

                     /* DStrNode: :"some text #{some expression}"*/
                     /* StrNode: :"some text"*/
                     /* EvStrNode :"#{some expression}"*/
                     /* Ruby 1.9 allows empty strings as symbols*/
                     if (((Node)yyVals[-1+yyTop].value) == null) {
                         yyVal = support.asSymbol(lexer.getRubySourceline(), new ByteList(new byte[] {}));
                     } else if (((Node)yyVals[-1+yyTop].value) instanceof DStrNode) {
                         yyVal = new DSymbolNode(((Node)yyVals[-1+yyTop].value).getLine(), ((DStrNode)yyVals[-1+yyTop].value));
                     } else if (((Node)yyVals[-1+yyTop].value) instanceof StrNode) {
                         yyVal = support.asSymbol(((Node)yyVals[-1+yyTop].value).getLine(), ((Node)yyVals[-1+yyTop].value));
                     } else {
                         yyVal = new DSymbolNode(((Node)yyVals[-1+yyTop].value).getLine());
                         ((DSymbolNode)yyVal).add(((Node)yyVals[-1+yyTop].value));
                     }
  return yyVal;
};
states[516] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((NumericNode)yyVals[0+yyTop].value);  
  return yyVal;
};
states[517] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = support.negateNumeric(((NumericNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[518] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[519] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((FloatNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[520] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((RationalNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[521] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[522] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.declareIdentifier(((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[523] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new InstVarNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[524] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new GlobalVarNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[525] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ConstNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[526] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ClassVarNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[527] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new NilNode(lexer.tokline);
  return yyVal;
};
states[528] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new SelfNode(lexer.tokline);
  return yyVal;
};
states[529] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new TrueNode(lexer.tokline);
  return yyVal;
};
states[530] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new FalseNode(lexer.tokline);
  return yyVal;
};
states[531] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new FileNode(lexer.tokline, new ByteList(lexer.getFile().getBytes(),
                    support.getConfiguration().getRuntime().getEncodingService().getLocaleEncoding()));
  return yyVal;
};
states[532] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new FixnumNode(lexer.tokline, lexer.tokline+1);
  return yyVal;
};
states[533] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new EncodingNode(lexer.tokline, lexer.getEncoding());
  return yyVal;
};
states[534] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.assignableLabelOrIdentifier(((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[535] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new InstAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[536] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new GlobalAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[537] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (support.isInDef()) support.compile_error("dynamic constant assignment");

                    yyVal = new ConstDeclNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), null, NilImplicitNode.NIL);
  return yyVal;
};
states[538] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ClassVarAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[539] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to nil");
                    yyVal = null;
  return yyVal;
};
states[540] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't change the value of self");
                    yyVal = null;
  return yyVal;
};
states[541] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to true");
                    yyVal = null;
  return yyVal;
};
states[542] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to false");
                    yyVal = null;
  return yyVal;
};
states[543] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to __FILE__");
                    yyVal = null;
  return yyVal;
};
states[544] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to __LINE__");
                    yyVal = null;
  return yyVal;
};
states[545] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to __ENCODING__");
                    yyVal = null;
  return yyVal;
};
states[546] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[547] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[548] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   lexer.setState(EXPR_BEG);
                   lexer.commandStart = true;
  return yyVal;
};
states[549] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[550] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = null;
  return yyVal;
};
states[551] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ArgsNode)yyVals[-1+yyTop].value);
                    lexer.setState(EXPR_BEG);
                    lexer.commandStart = true;
  return yyVal;
};
states[552] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = lexer.inKwarg;
                   lexer.inKwarg = true;
                   lexer.setState(lexer.getState() | EXPR_LABEL);
  return yyVal;
};
states[553] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   lexer.inKwarg = ((Boolean)yyVals[-2+yyTop].value);
                    yyVal = ((ArgsNode)yyVals[-1+yyTop].value);
                    lexer.setState(EXPR_BEG);
                    lexer.commandStart = true;
  return yyVal;
};
states[554] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args_tail(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[555] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args_tail(((ListNode)yyVals[-1+yyTop].value).getLine(), ((ListNode)yyVals[-1+yyTop].value), (ByteList) null, ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[556] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args_tail(lexer.getRubySourceline(), null, ((ByteList)yyVals[-1+yyTop].value), ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[557] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args_tail(((BlockArgNode)yyVals[0+yyTop].value).getLine(), null, (ByteList) null, ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[558] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[559] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args_tail(lexer.getRubySourceline(), null, (ByteList) null, null);
  return yyVal;
};
states[560] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), ((ListNode)yyVals[-5+yyTop].value), ((ListNode)yyVals[-3+yyTop].value), ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[561] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-7+yyTop].value).getLine(), ((ListNode)yyVals[-7+yyTop].value), ((ListNode)yyVals[-5+yyTop].value), ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[562] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[563] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), ((ListNode)yyVals[-5+yyTop].value), ((ListNode)yyVals[-3+yyTop].value), null, ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[564] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), null, ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[565] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), ((ListNode)yyVals[-5+yyTop].value), null, ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[566] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop].value).getLine(), ((ListNode)yyVals[-1+yyTop].value), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[567] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), null, ((ListNode)yyVals[-3+yyTop].value), ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[568] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), null, ((ListNode)yyVals[-5+yyTop].value), ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[569] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop].value).getLine(), null, ((ListNode)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[570] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), null, ((ListNode)yyVals[-3+yyTop].value), null, ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[571] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((RestArgNode)yyVals[-1+yyTop].value).getLine(), null, null, ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[572] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((RestArgNode)yyVals[-3+yyTop].value).getLine(), null, null, ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[573] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ArgsTailHolder)yyVals[0+yyTop].value).getLine(), null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[574] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(lexer.getRubySourceline(), null, null, null, null, (ArgsTailHolder) null);
  return yyVal;
};
states[575] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.yyerror("formal argument cannot be a constant");
  return yyVal;
};
states[576] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.yyerror("formal argument cannot be an instance variable");
  return yyVal;
};
states[577] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.yyerror("formal argument cannot be a global variable");
  return yyVal;
};
states[578] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.yyerror("formal argument cannot be a class variable");
  return yyVal;
};
states[579] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value); /* Not really reached*/
  return yyVal;
};
states[580] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.formal_argument(((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[581] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setCurrentArg(((ByteList)yyVals[0+yyTop].value));
                    yyVal = support.arg_var(((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[582] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setCurrentArg(null);
                    yyVal = ((ArgumentNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[583] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
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
};
states[584] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ArrayNode(lexer.getRubySourceline(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[585] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[0+yyTop].value));
                    yyVal = ((ListNode)yyVals[-2+yyTop].value);
  return yyVal;
};
states[586] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.arg_var(support.formal_argument(((ByteList)yyVals[0+yyTop].value)));
                    lexer.setCurrentArg(((ByteList)yyVals[0+yyTop].value));
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[587] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setCurrentArg(null);
                    yyVal = new KeywordArgNode(((Node)yyVals[0+yyTop].value).getLine(), support.assignableKeyword(((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[588] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setCurrentArg(null);
                    yyVal = new KeywordArgNode(lexer.getRubySourceline(), support.assignableKeyword(((ByteList)yyVals[0+yyTop].value), new RequiredKeywordArgumentValueNode()));
  return yyVal;
};
states[589] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new KeywordArgNode(support.getPosition(((Node)yyVals[0+yyTop].value)), support.assignableKeyword(((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[590] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new KeywordArgNode(lexer.getRubySourceline(), support.assignableKeyword(((ByteList)yyVals[0+yyTop].value), new RequiredKeywordArgumentValueNode()));
  return yyVal;
};
states[591] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[592] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[593] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[594] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[595] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[596] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[597] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.shadowing_lvar(((ByteList)yyVals[0+yyTop].value));
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[598] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.INTERNAL_ID;
  return yyVal;
};
states[599] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setCurrentArg(null);
                    yyVal = new OptArgNode(support.getPosition(((Node)yyVals[0+yyTop].value)), support.assignableLabelOrIdentifier(((ArgumentNode)yyVals[-2+yyTop].value).getName().getBytes(), ((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[600] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setCurrentArg(null);
                    yyVal = new OptArgNode(support.getPosition(((Node)yyVals[0+yyTop].value)), support.assignableLabelOrIdentifier(((ArgumentNode)yyVals[-2+yyTop].value).getName().getBytes(), ((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[601] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new BlockNode(((Node)yyVals[0+yyTop].value).getLine()).add(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[602] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[603] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new BlockNode(((Node)yyVals[0+yyTop].value).getLine()).add(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[604] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[605] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[606] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[607] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (!support.is_local_id(((ByteList)yyVals[0+yyTop].value))) {
                        support.yyerror("rest argument must be local variable");
                    }
                    
                    yyVal = new RestArgNode(support.arg_var(support.shadowing_lvar(((ByteList)yyVals[0+yyTop].value))));
  return yyVal;
};
states[608] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  /* FIXME: bytelist_love: somewhat silly to remake the empty bytelist over and over but this type should change (using null vs "" is a strange distinction).*/
  yyVal = new UnnamedRestArgNode(lexer.getRubySourceline(), support.symbolID(CommonByteLists.EMPTY), support.getCurrentScope().addVariable("*"));
  return yyVal;
};
states[609] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[610] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[611] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (!support.is_local_id(((ByteList)yyVals[0+yyTop].value))) {
                        support.yyerror("block argument must be local variable");
                    }
                    
                    yyVal = new BlockArgNode(support.arg_var(support.shadowing_lvar(((ByteList)yyVals[0+yyTop].value))));
  return yyVal;
};
states[612] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((BlockArgNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[613] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[614] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[615] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setState(EXPR_BEG);
  return yyVal;
};
states[616] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (((Node)yyVals[-1+yyTop].value) == null) {
                        support.yyerror("can't define single method for ().");
                    } else if (((Node)yyVals[-1+yyTop].value) instanceof ILiteralNode) {
                        support.yyerror("can't define single method for literals.");
                    }
                    support.value_expr(lexer, ((Node)yyVals[-1+yyTop].value));
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[617] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new HashNode(lexer.getRubySourceline());
  return yyVal;
};
states[618] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[619] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new HashNode(lexer.getRubySourceline(), ((KeyValuePair)yyVals[0+yyTop].value));
  return yyVal;
};
states[620] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((HashNode)yyVals[-2+yyTop].value).add(((KeyValuePair)yyVals[0+yyTop].value));
  return yyVal;
};
states[621] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.createKeyValue(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[622] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node label = support.asSymbol(support.getPosition(((Node)yyVals[0+yyTop].value)), ((ByteList)yyVals[-1+yyTop].value));
                    yyVal = support.createKeyValue(label, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[623] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (((Node)yyVals[-2+yyTop].value) instanceof StrNode) {
                        DStrNode dnode = new DStrNode(support.getPosition(((Node)yyVals[-2+yyTop].value)), lexer.getEncoding());
                        dnode.add(((Node)yyVals[-2+yyTop].value));
                        yyVal = support.createKeyValue(new DSymbolNode(support.getPosition(((Node)yyVals[-2+yyTop].value)), dnode), ((Node)yyVals[0+yyTop].value));
                    } else if (((Node)yyVals[-2+yyTop].value) instanceof DStrNode) {
                        yyVal = support.createKeyValue(new DSymbolNode(support.getPosition(((Node)yyVals[-2+yyTop].value)), ((DStrNode)yyVals[-2+yyTop].value)), ((Node)yyVals[0+yyTop].value));
                    } else {
                        support.compile_error("Uknown type for assoc in strings: " + ((Node)yyVals[-2+yyTop].value));
                    }

  return yyVal;
};
states[624] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.createKeyValue(null, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[625] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[626] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[627] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[628] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[629] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[630] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[631] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[632] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[633] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[634] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[635] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[636] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[637] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[638] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[640] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[645] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[646] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[654] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = null;
  return yyVal;
};
states[655] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  yyVal = null;
  return yyVal;
};
}
					// line 2817 "RubyParser.y"

    /** The parse method use an lexer stream and parse it to an AST node 
     * structure
     */
    public RubyParserResult parse(ParserConfiguration configuration) throws IOException {
        support.reset();
        support.setConfiguration(configuration);
        support.setResult(new RubyParserResult());
        
        yyparse(lexer, configuration.isDebug() ? new YYDebug() : null);
        
        return support.getResult();
    }
}
					// line 10631 "-"
