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
import java.util.Set;

import org.jruby.RubySymbol;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArgumentNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.ArrayPatternNode;
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
import org.jruby.ast.DefHolder;
import org.jruby.ast.DefinedNode;
import org.jruby.ast.DStrNode;
import org.jruby.ast.DSymbolNode;
import org.jruby.ast.DVarNode;
import org.jruby.ast.DXStrNode;
import org.jruby.ast.DefnNode;
import org.jruby.ast.DefsNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.EncodingNode;
import org.jruby.ast.EnsureNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.FalseNode;
import org.jruby.ast.FileNode;
import org.jruby.ast.FindPatternNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FloatNode;
import org.jruby.ast.ForNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.HashPatternNode;
import org.jruby.ast.InNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.KeywordArgNode;
import org.jruby.ast.LambdaNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.LiteralNode;
import org.jruby.ast.LocalVarNode;
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
import org.jruby.lexer.yacc.LexContext;
import org.jruby.lexer.yacc.RubyLexer;
import org.jruby.lexer.yacc.StackState;
import org.jruby.lexer.yacc.StrTerm;
import org.jruby.util.ByteList;
import org.jruby.util.CommonByteLists;
import org.jruby.util.KeyValuePair;
import org.jruby.util.StringSupport;
import static org.jruby.lexer.LexingCommon.AMPERSAND;
import static org.jruby.lexer.LexingCommon.BACKTICK;
import static org.jruby.lexer.LexingCommon.BANG;
import static org.jruby.lexer.LexingCommon.CARET;
import static org.jruby.lexer.LexingCommon.DOT;
import static org.jruby.lexer.LexingCommon.GT;
import static org.jruby.lexer.LexingCommon.LCURLY;
import static org.jruby.lexer.LexingCommon.LT;
import static org.jruby.lexer.LexingCommon.MINUS;
import static org.jruby.lexer.LexingCommon.PERCENT;
import static org.jruby.lexer.LexingCommon.OR;
import static org.jruby.lexer.LexingCommon.PLUS;
import static org.jruby.lexer.LexingCommon.RBRACKET;
import static org.jruby.lexer.LexingCommon.RCURLY;
import static org.jruby.lexer.LexingCommon.RPAREN;
import static org.jruby.lexer.LexingCommon.SLASH;
import static org.jruby.lexer.LexingCommon.STAR;
import static org.jruby.lexer.LexingCommon.TILDE;
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
					// line 192 "-"
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
  public static final int keyword_do_LAMBDA = 283;
  public static final int keyword_return = 284;
  public static final int keyword_yield = 285;
  public static final int keyword_super = 286;
  public static final int keyword_self = 287;
  public static final int keyword_nil = 288;
  public static final int keyword_true = 289;
  public static final int keyword_false = 290;
  public static final int keyword_and = 291;
  public static final int keyword_or = 292;
  public static final int keyword_not = 293;
  public static final int modifier_if = 294;
  public static final int modifier_unless = 295;
  public static final int modifier_while = 296;
  public static final int modifier_until = 297;
  public static final int modifier_rescue = 298;
  public static final int keyword_alias = 299;
  public static final int keyword_defined = 300;
  public static final int keyword_BEGIN = 301;
  public static final int keyword_END = 302;
  public static final int keyword__LINE__ = 303;
  public static final int keyword__FILE__ = 304;
  public static final int keyword__ENCODING__ = 305;
  public static final int tIDENTIFIER = 306;
  public static final int tFID = 307;
  public static final int tGVAR = 308;
  public static final int tIVAR = 309;
  public static final int tCONSTANT = 310;
  public static final int tCVAR = 311;
  public static final int tLABEL = 312;
  public static final int tINTEGER = 313;
  public static final int tFLOAT = 314;
  public static final int tRATIONAL = 315;
  public static final int tIMAGINARY = 316;
  public static final int tCHAR = 317;
  public static final int tNTH_REF = 318;
  public static final int tBACK_REF = 319;
  public static final int tSTRING_CONTENT = 320;
  public static final int tREGEXP_END = 321;
  public static final int tUMINUS_NUM = 322;
  public static final int tSP = 323;
  public static final int tUPLUS = 324;
  public static final int tUMINUS = 325;
  public static final int tPOW = 326;
  public static final int tCMP = 327;
  public static final int tEQ = 328;
  public static final int tEQQ = 329;
  public static final int tNEQ = 330;
  public static final int tGEQ = 331;
  public static final int tLEQ = 332;
  public static final int tANDOP = 333;
  public static final int tOROP = 334;
  public static final int tMATCH = 335;
  public static final int tNMATCH = 336;
  public static final int tDOT2 = 337;
  public static final int tDOT3 = 338;
  public static final int tBDOT2 = 339;
  public static final int tBDOT3 = 340;
  public static final int tAREF = 341;
  public static final int tASET = 342;
  public static final int tLSHFT = 343;
  public static final int tRSHFT = 344;
  public static final int tANDDOT = 345;
  public static final int tCOLON2 = 346;
  public static final int tCOLON3 = 347;
  public static final int tOP_ASGN = 348;
  public static final int tASSOC = 349;
  public static final int tLPAREN = 350;
  public static final int tLPAREN_ARG = 351;
  public static final int tLBRACK = 352;
  public static final int tLBRACE = 353;
  public static final int tLBRACE_ARG = 354;
  public static final int tSTAR = 355;
  public static final int tDSTAR = 356;
  public static final int tAMPER = 357;
  public static final int tLAMBDA = 358;
  public static final int tSYMBEG = 359;
  public static final int tSTRING_BEG = 360;
  public static final int tXSTRING_BEG = 361;
  public static final int tREGEXP_BEG = 362;
  public static final int tWORDS_BEG = 363;
  public static final int tQWORDS_BEG = 364;
  public static final int tSTRING_END = 365;
  public static final int tSYMBOLS_BEG = 366;
  public static final int tQSYMBOLS_BEG = 367;
  public static final int tSTRING_DEND = 368;
  public static final int tSTRING_DBEG = 369;
  public static final int tSTRING_DVAR = 370;
  public static final int tLAMBEG = 371;
  public static final int tLABEL_END = 372;
  public static final int tLOWEST = 373;
  public static final int yyErrorCode = 256;

  /** number of final state.
    */
  protected static final int yyFinal = 1;

  /** parser tables.
      Order is mandated by <i>jay</i>.
    */
  protected static final short[] yyLhs = {
//yyLhs 806
    -1,   212,     0,    30,    31,    31,    31,    31,    32,    32,
    33,   215,    34,    34,    35,    36,    36,    36,    36,    37,
   216,    37,   217,    38,    38,    38,    38,    38,    38,    38,
    38,    38,    38,    38,    38,    38,    38,    38,    38,    38,
    77,    77,    77,    77,    77,    77,    77,    77,    75,    75,
    75,    39,    39,    39,    39,    39,   219,   220,   221,    39,
   222,   223,   224,    39,    39,    27,    28,   225,    29,    45,
   226,    46,    43,    43,    82,    82,   120,    49,    42,    42,
    42,    42,    42,    42,    42,    42,    42,    42,    42,   125,
   125,   131,   131,   127,   127,   127,   127,   127,   127,   127,
   127,   127,   127,   128,   128,   126,   126,   130,   130,   129,
   129,   129,   129,   129,   129,   129,   129,   129,   129,   129,
   129,   129,   129,   129,   129,   129,   129,   129,   122,   122,
   122,   122,   122,   122,   122,   122,   122,   122,   122,   122,
   122,   122,   122,   122,   122,   122,   122,   158,   158,    26,
    26,    26,   160,   160,   160,   160,   160,   124,   124,    99,
   228,    99,   159,   159,   159,   159,   159,   159,   159,   159,
   159,   159,   159,   159,   159,   159,   159,   159,   159,   159,
   159,   159,   159,   159,   159,   159,   159,   159,   159,   159,
   159,   159,   171,   171,   171,   171,   171,   171,   171,   171,
   171,   171,   171,   171,   171,   171,   171,   171,   171,   171,
   171,   171,   171,   171,   171,   171,   171,   171,   171,   171,
   171,   171,   171,   171,   171,   171,   171,   171,   171,   171,
   171,   171,   171,    40,    40,    40,    40,    40,    40,    40,
    40,    40,    40,    40,    40,    40,    40,    40,    40,    40,
    40,    40,    40,    40,    40,    40,    40,    40,    40,    40,
    40,    40,    40,    40,    40,    40,    40,    40,    40,    40,
    40,    40,    40,   229,    40,    40,    40,    40,    40,    40,
    40,   172,   172,   172,   172,    50,    50,   183,   183,    47,
    70,    70,    70,    70,    76,    76,    63,    63,    63,    64,
    64,    62,    62,    62,    62,    62,    61,    61,    61,    61,
    61,   231,    69,    72,    71,    71,    60,    60,    60,    60,
    79,    79,    78,    78,    78,    41,    41,    41,    41,    41,
    41,    41,    41,    41,    41,    41,   232,    41,   233,    41,
   234,    41,    41,    41,    41,    41,    41,    41,    41,    41,
    41,   235,    41,    41,    41,    41,    41,    41,    41,    41,
    41,    41,    41,   237,    41,   238,    41,    41,    41,   239,
    41,   241,    41,   242,    41,    41,    41,    41,    41,    41,
    41,    48,   195,   196,   209,   205,   206,   208,   207,   191,
   192,   203,   197,   198,   199,   200,   194,   193,   201,   204,
   190,   236,   236,   236,   227,   227,    51,    51,    52,    52,
   102,   102,    92,    92,    93,    93,    94,    94,    94,    94,
    94,    95,    95,   180,   180,    67,    67,    67,    67,    68,
    68,   182,   103,   103,   103,   103,   103,   103,   103,   103,
   103,   103,   103,   103,   103,   103,   103,   104,   104,   105,
   105,   112,   112,   111,   111,   113,   113,   243,   244,   245,
   246,   114,   115,   115,   116,   116,   121,    81,    81,    81,
    81,    44,    44,    44,    44,    44,    44,    44,    44,    44,
   119,   119,   247,   248,   249,   117,   250,   251,   252,   118,
    54,    54,    54,    54,    53,    55,    55,   253,   254,   255,
   132,   133,   133,   134,   134,   134,   135,   135,   135,   135,
   135,   135,   136,   137,   137,   138,   138,   210,   211,   139,
   139,   139,   139,   139,   139,   139,   139,   139,   139,   139,
   139,   256,   139,   139,   257,   139,   141,   141,   141,   141,
   141,   141,   141,   141,   142,   142,   143,   143,   140,   174,
   174,   144,   144,   145,   151,   151,   151,   151,   152,   152,
   153,   153,   178,   178,   175,   175,   176,   177,   177,   146,
   146,   146,   146,   146,   146,   146,   146,   146,   146,   147,
   147,   147,   147,   147,   147,   147,   147,   147,   147,   147,
   147,   147,   147,   147,   147,   148,   149,   150,   150,   150,
    56,    56,    57,    57,    57,    58,    58,    59,    59,    20,
    20,     2,     3,     3,     3,     4,     5,     6,    11,    16,
    16,    19,    19,    12,    13,    13,    14,    15,    17,    17,
    18,    18,     7,     7,     8,     8,     9,     9,    10,   258,
    10,   259,   260,   261,   262,    10,   101,   101,   101,   101,
    25,    25,    23,   154,   154,   154,   154,    24,    21,    21,
    22,    22,    22,    22,    73,    73,    73,    73,    73,    73,
    73,    73,    73,    73,    73,    73,    74,    74,    74,    74,
    74,    74,    74,    74,    74,    74,    74,    74,   100,   100,
   263,    80,    80,    86,    86,    87,    87,    87,    85,   264,
    85,    65,    65,    65,    65,    66,    66,    88,    88,    88,
    88,    88,    88,    88,    88,    88,    88,    88,    88,    88,
    88,    88,   181,   165,   165,   165,   165,   164,   164,   168,
    90,    90,    89,    89,   167,   108,   108,   110,   110,   109,
   109,   107,   107,   186,   186,   179,   166,   166,   106,    84,
    83,    83,    91,    91,   185,   185,   161,   161,   184,   184,
   162,   163,   163,     1,   265,     1,    96,    96,    97,    97,
    98,    98,    98,    98,   155,   155,   155,   156,   156,   156,
   156,   157,   157,   157,   173,   173,   169,   169,   170,   170,
   213,   213,   218,   218,   187,   188,   202,   230,   230,   230,
   240,   240,   214,   214,   123,   189,
    }, yyLen = {
//yyLen 806
     2,     0,     2,     2,     1,     1,     3,     2,     1,     2,
     3,     0,     6,     3,     2,     1,     1,     3,     2,     1,
     0,     3,     0,     4,     3,     3,     3,     2,     3,     3,
     3,     3,     3,     4,     1,     4,     4,     6,     4,     1,
     4,     4,     7,     6,     6,     6,     6,     4,     1,     3,
     1,     1,     3,     3,     3,     2,     0,     0,     0,     6,
     0,     0,     0,     6,     1,     1,     2,     0,     5,     1,
     0,     3,     1,     1,     1,     4,     3,     1,     2,     3,
     4,     5,     4,     5,     2,     2,     2,     2,     2,     1,
     3,     1,     3,     1,     2,     3,     5,     2,     4,     2,
     4,     1,     3,     1,     3,     2,     3,     1,     3,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     4,     3,     3,     3,     3,     2,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     4,     3,     3,     3,     3,     2,     1,     1,     1,     2,
     1,     3,     1,     1,     1,     1,     1,     1,     1,     1,
     0,     4,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     4,     4,     7,     6,     6,     6,     6,
     5,     4,     3,     3,     2,     2,     2,     2,     3,     3,
     3,     3,     3,     3,     4,     2,     2,     3,     3,     3,
     3,     1,     3,     3,     3,     3,     3,     2,     2,     3,
     3,     3,     3,     0,     4,     6,     4,     6,     4,     6,
     1,     1,     1,     1,     1,     3,     3,     1,     1,     1,
     1,     2,     4,     2,     1,     3,     3,     5,     3,     1,
     1,     1,     1,     2,     4,     2,     1,     2,     2,     4,
     1,     0,     2,     2,     2,     1,     1,     2,     3,     4,
     1,     1,     3,     4,     2,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     0,     4,     0,     3,
     0,     4,     3,     3,     2,     3,     3,     1,     4,     3,
     1,     0,     6,     4,     3,     2,     1,     2,     1,     6,
     6,     4,     4,     0,     6,     0,     5,     5,     6,     0,
     6,     0,     7,     0,     5,     4,     4,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     2,     1,     1,     1,     5,     1,     2,
     1,     1,     1,     3,     1,     3,     1,     3,     5,     1,
     3,     2,     1,     1,     1,     4,     2,     2,     1,     2,
     0,     1,     6,     8,     4,     6,     4,     2,     6,     2,
     4,     6,     2,     4,     2,     4,     1,     1,     1,     3,
     4,     1,     4,     1,     3,     1,     1,     0,     0,     0,
     0,     7,     4,     1,     3,     3,     3,     2,     4,     5,
     5,     2,     4,     4,     3,     3,     3,     2,     1,     4,
     3,     3,     0,     0,     0,     5,     0,     0,     0,     5,
     1,     2,     3,     4,     5,     1,     1,     0,     0,     0,
     8,     1,     1,     1,     3,     3,     1,     2,     3,     1,
     1,     1,     1,     3,     1,     3,     1,     1,     1,     1,
     4,     4,     4,     3,     4,     4,     4,     3,     3,     3,
     2,     0,     4,     2,     0,     4,     1,     1,     2,     3,
     5,     2,     4,     1,     2,     3,     1,     3,     5,     2,
     1,     1,     3,     1,     3,     1,     2,     1,     1,     3,
     2,     1,     1,     3,     2,     1,     2,     1,     1,     1,
     3,     3,     2,     2,     1,     1,     1,     2,     2,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     2,     2,     3,     1,
     6,     1,     1,     1,     1,     2,     1,     2,     1,     1,
     1,     1,     1,     1,     2,     3,     3,     3,     4,     0,
     3,     1,     2,     4,     0,     3,     4,     4,     0,     3,
     0,     3,     0,     2,     0,     2,     0,     2,     1,     0,
     3,     0,     0,     0,     0,     7,     1,     1,     1,     1,
     1,     1,     2,     1,     1,     1,     1,     3,     1,     2,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     0,     4,     0,     1,     1,     3,     5,     3,     1,     0,
     3,     4,     2,     2,     1,     2,     0,     6,     8,     4,
     6,     4,     6,     2,     4,     6,     2,     4,     2,     4,
     1,     0,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     3,     1,     3,     1,     2,     1,     2,     1,     1,
     3,     1,     3,     1,     1,     2,     2,     1,     3,     3,
     1,     3,     1,     3,     1,     1,     2,     1,     1,     1,
     2,     2,     0,     1,     0,     4,     1,     2,     1,     3,
     3,     2,     4,     2,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     0,     1,     0,     1,     2,     2,     2,     0,     1,     1,
     1,     1,     1,     2,     0,     0,
    }, yyDefRed = {
//yyDefRed 1344
     1,     0,     0,     0,   389,   390,   391,     0,   382,   383,
   384,   387,   385,   386,   388,     0,     0,   379,   380,   400,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   660,   661,   662,   663,   612,   688,   689,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   457,     0,
   632,   634,   636,     0,     0,     0,     0,     0,     0,   326,
     0,   613,   327,   328,   329,   331,   330,   332,   325,   609,
   658,   650,   651,   610,     0,     0,     2,     0,     5,     0,
     0,     0,     0,     0,    51,     0,     0,     0,     0,   333,
     0,    34,     0,    73,     0,   358,     0,     4,     0,     0,
    89,     0,   103,    77,     0,     0,     0,   336,     0,     0,
    70,    70,     0,     0,     0,     7,   202,   213,   203,   226,
   199,   219,   209,   208,   229,   230,   224,   207,   206,   201,
   227,   231,   232,   211,   200,   214,   218,   220,   212,   205,
   221,   228,   223,   222,   215,   225,   210,   198,   217,   216,
   197,   204,   195,   196,   192,   193,   194,   152,   154,   153,
   187,   188,   183,   165,   166,   167,   174,   171,   173,   168,
   169,   189,   190,   175,   176,   180,   184,   170,   172,   162,
   163,   164,   177,   178,   179,   181,   182,   185,   186,   191,
   158,     0,   159,   155,   157,   156,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   632,     0,     0,   306,     0,
     0,     0,    87,   310,     0,     0,   768,     0,     0,    88,
     0,    85,     0,     0,   477,    84,     0,   793,     0,     0,
    22,     0,     0,     9,     0,     0,   377,   378,     0,     0,
   255,     0,     0,   347,     0,     0,     0,     0,     0,    20,
     0,     0,     0,    16,     0,    15,     0,     0,     0,     0,
     0,     0,     0,   290,     0,     0,     0,   766,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   381,     0,     0,     0,
   458,   655,   654,   656,     0,   652,   653,     0,     0,     0,
   619,   628,   624,   630,   267,    55,   268,   614,     0,     0,
     0,     0,   694,     0,     0,     0,   800,   801,     3,     0,
   802,     0,     0,     0,     0,     0,     0,     0,    60,     0,
     0,     0,     0,     0,   283,   284,     0,     0,     0,     0,
     0,     0,     0,     0,    56,     0,   281,   282,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   393,   467,   486,
   392,   482,   357,   486,   787,     0,     0,   786,     0,   471,
     0,   355,     0,     0,   789,   788,     0,     0,     0,     0,
     0,     0,     0,   105,    86,   670,   669,   671,   672,   674,
   673,   675,     0,   666,   665,     0,   668,     0,     0,     0,
     0,   334,   150,   373,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   764,     0,    66,   763,
    65,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   410,   411,     0,   365,     0,     0,   160,   771,     0,   317,
   773,   313,     0,     0,     0,     0,     0,     0,   307,   315,
     0,     0,   308,     0,     0,     0,   349,     0,   312,     0,
     0,   302,     0,     0,   301,     0,     0,   354,    54,    24,
    26,    25,     0,   351,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   342,    14,     0,     0,     0,   339,
     0,   798,   291,   345,     0,   293,   346,   767,     0,   659,
     0,   107,     0,   698,     0,     0,     0,     0,   459,   638,
   657,   641,   639,   633,   615,   616,   635,   617,   637,     0,
     0,     0,     0,   728,   725,   724,   723,   726,   734,   743,
   722,     0,   755,   744,   759,   758,   754,   720,     0,     0,
   732,     0,   752,     0,   741,     0,   704,   729,   727,   423,
     0,     0,   424,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   803,     6,    28,    29,    30,
    31,    32,    52,    53,    61,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,    57,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   487,     0,   483,     0,     0,     0,     0,   476,     0,   474,
     0,     0,     0,     0,   779,     0,   475,     0,   780,   482,
    79,     0,   287,   288,     0,   777,   778,     0,     0,     0,
     0,     0,     0,     0,   106,     0,   147,     0,   149,   690,
   369,     0,     0,     0,     0,     0,   402,     0,     0,     0,
   785,   784,    67,     0,     0,     0,     0,     0,     0,     0,
    70,     0,     0,     0,     0,     0,   770,     0,     0,     0,
     0,     0,     0,     0,   314,     0,     0,   769,     0,     0,
   348,   794,     0,   296,     0,   298,   353,    23,     0,     0,
    10,    33,     0,     0,     0,     0,    21,     0,    17,   341,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   642,
     0,   618,   621,     0,     0,   626,   623,     0,     0,   627,
     0,     0,   414,     0,     0,     0,   412,   695,     0,   713,
     0,   716,     0,   702,     0,   718,   735,     0,     0,   703,
   697,   760,   756,   745,   746,   399,   375,   394,     0,   601,
     0,     0,   700,     0,   376,     0,     0,     0,     0,   466,
   488,   480,   484,   481,     0,     0,   473,     0,     0,     0,
     0,     0,     0,   300,   472,     0,   299,     0,     0,     0,
     0,    41,   234,    50,     0,     0,     0,     0,    47,   241,
     0,     0,   316,     0,    40,   233,    36,    35,     0,   320,
     0,   104,     0,     0,     0,     0,     0,     0,     0,   151,
     0,     0,   337,     0,   403,     0,     0,   361,   405,    71,
   404,   362,     0,     0,     0,     0,     0,     0,   497,     0,
     0,   396,     0,     0,     0,   161,   772,     0,     0,     0,
     0,     0,   319,   309,     0,     0,     0,   240,   292,   108,
     0,     0,   463,   460,   643,   646,   647,   648,   649,   640,
   620,   622,   629,   625,   631,     0,   421,     0,   731,     0,
   705,   733,     0,     0,     0,     0,   753,     0,   742,   761,
     0,     0,     0,   730,   748,   395,   397,    13,   608,    11,
     0,     0,     0,   603,   604,     0,     0,   588,   587,   589,
   590,   592,   591,   593,   595,   599,     0,     0,     0,   534,
     0,     0,     0,   580,   581,   582,   583,   585,   584,   586,
   579,   594,     0,   512,     0,   516,   519,     0,   574,   575,
     0,     0,     0,     0,     0,     0,     0,    83,     0,   795,
     0,     0,    81,    76,     0,     0,     0,     0,     0,     0,
   469,   470,     0,     0,     0,     0,     0,     0,     0,   479,
   374,   398,     0,   406,   408,     0,     0,   765,    68,     0,
     0,   498,   367,     0,   366,     0,   490,     0,     0,     0,
     0,     0,     0,     0,     0,   297,   352,     0,     0,   644,
   413,   415,     0,     0,     0,   709,     0,   711,   696,     0,
   717,     0,   714,   701,   719,     0,   607,     0,     0,   606,
     0,     0,   577,   578,   148,   597,     0,     0,     0,     0,
     0,     0,   543,     0,     0,   530,   533,     0,     0,   596,
     0,    63,     0,     0,     0,     0,     0,     0,     0,     0,
    59,     0,     0,     0,   448,   447,     0,    46,   238,    45,
   239,     0,    43,   236,    44,   237,     0,    49,     0,     0,
     0,     0,     0,     0,     0,     0,    37,     0,   691,   370,
   359,   409,     0,   368,     0,   364,   491,     0,     0,   360,
     0,     0,     0,     0,     0,   461,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   605,
     0,     0,   549,   529,   528,     0,     0,     0,   544,     0,
   796,   562,   632,     0,     0,   558,   567,   568,   557,     0,
     0,   513,   515,   570,   571,   598,   527,   523,   632,     0,
     0,     0,     0,     0,     0,   446,     0,   750,     0,     0,
     0,   739,     0,     0,   428,     0,     0,     0,   489,   485,
    42,   235,     0,     0,   372,     0,     0,     0,     0,   509,
   510,   511,     0,   492,     0,   462,     0,     0,     0,     0,
     0,   710,     0,   707,   712,   715,    12,     0,     0,     0,
     0,     0,   535,     0,     0,   545,     0,   551,     0,   532,
     0,     0,   566,   564,     0,   521,   520,   522,   525,   524,
   526,     0,   442,     0,   439,   437,     0,     0,   426,   449,
     0,   444,     0,     0,     0,     0,     0,   427,     0,   499,
     0,     0,     0,   493,   495,   496,   494,   455,     0,   453,
   456,   465,   464,   645,     0,     0,     0,     0,     0,     0,
   600,     0,     0,     0,   563,   559,   554,   429,   751,     0,
     0,     0,     0,   450,   740,     0,     0,   344,     0,     0,
   407,     0,   504,   505,   508,     0,     0,   452,   708,     0,
     0,     0,   552,   548,     0,   443,     0,   440,     0,   434,
     0,   436,   425,   445,     0,     0,     0,     0,   454,     0,
     0,     0,     0,   501,   502,   500,     0,   441,   435,     0,
   432,   438,     0,   433,
    }, yyDgoto = {
//yyDgoto 266
     1,   437,    69,    70,    71,    72,    73,   314,   318,   319,
   543,    74,    75,   551,    76,    77,   549,   550,   552,   743,
    78,    79,    80,    81,    82,    83,   419,   438,    84,    85,
    86,    87,    88,   253,   588,   589,   272,   273,   274,    90,
    91,    92,    93,    94,    95,   426,   441,   229,   261,   262,
    98,   992,   993,   862,  1007,  1266,   778,   922,  1038,   917,
   641,   491,   492,   637,   804,   900,   759,  1287,  1242,   241,
   281,   478,   233,    99,   234,   824,   825,   101,   826,   830,
   670,   102,   103,  1176,  1177,   329,   330,   331,   568,   592,
   570,   571,   752,   753,   754,   755,   285,   493,   236,   201,
   237,   889,   459,  1179,  1073,  1074,   572,   573,   574,  1180,
  1181,  1268,  1111,  1269,   105,   883,  1115,   631,   629,   391,
   650,   378,   238,   275,   202,   108,   109,   110,   111,   112,
   532,   277,   859,  1335,  1196,  1197,  1146,   953,   954,   955,
  1049,  1050,  1051,  1052,  1226,  1053,   956,   957,   958,   959,
   960,  1153,  1154,  1155,   315,   113,   723,   639,   422,   640,
   204,   575,   576,   763,   577,   578,   579,   580,   913,   673,
   396,   205,   376,   682,  1054,  1156,  1157,  1158,  1159,   582,
   583,   495,  1245,   654,   585,   586,   587,   486,   799,   479,
   263,   115,   116,   995,   863,   117,   118,   383,   379,   780,
   920,   996,  1056,   119,   776,   120,   121,   122,   123,   124,
  1068,  1069,     2,   338,   464,  1035,   512,   502,   487,   618,
   787,  1070,   604,   786,  1061,   846,   442,   849,   694,   504,
   522,   242,   424,   279,   518,   718,   677,   860,   692,   836,
   678,   834,   674,   310,   538,   738,  1018,   632,   792,   964,
   630,   790,   963,  1001,  1104,  1301,  1058,  1046,   740,   739,
   884,  1019,  1116,   835,   333,   679,
    }, yySindex = {
//yySindex 1344
     0,     0, 22540, 23166,     0,     0,     0, 26286,     0,     0,
     0,     0,     0,     0,     0, 23279, 23279,     0,     0,     0,
   118,   133,     0,     0,     0,     0,   303, 26183,   135,   109,
   200,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  1333, 25282, 25282,
 25282, 25282,  -128, 22657, 23396, 24017, 24363, 26876,     0, 26706,
     0,     0,     0,   385,   440,   454,   508, 25399, 25282,     0,
   207,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   335,   335,     0,   100,     0,  2226,
  -170, 21745,     0,   278,     0,   -35,   211,   151,   250,     0,
   242,     0,   283,     0,   255,     0,   572,     0,   575, 29028,
     0,   603,     0,     0, 23279, 29139, 29361,     0, 25511, 26606,
     0,     0, 29250, 22317, 25511,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   649,     0,     0,     0,     0,     0,     0,     0,     0,
   715,     0,     0,     0,     0,     0,     0,     0,     0, 25282,
   462, 22770, 25282, 25282, 25282,     0, 25282, 27961,     0,   445,
   254,   766,     0,     0,   457,   771,     0,   472,   812,     0,
 22205,     0, 23279, 23525,     0,     0, 22428,     0, 25511,  1045,
     0,   782, 22540,     0, 22770,   552,     0,     0,   118,   133,
     0,   343,   151,     0,   590,  8945,  8945,   577, 23396,     0,
 22657,   890,   100,     0,  2226,     0,     0,   135,  2226,   135,
   261,   853,   418,     0,   445,   816,   418,     0,     0,     0,
     0,     0,   135,     0,     0,     0,     0,     0,     0,     0,
     0,  1333,   638, 29472,   335,   335,     0,   380,     0,   908,
     0,     0,     0,     0,   604,     0,     0,  1020,  1373,  1106,
     0,     0,     0,     0,     0,     0,     0,     0,  3040, 22770,
   896,     0,     0,  3264, 22770,   903,     0,     0,     0, 22936,
     0, 25511, 25511, 25511, 25511, 23396, 25511, 25511,     0, 25282,
 25282, 25282, 25282, 25282,     0,     0, 25282, 25282, 25282, 25282,
 25282, 25282, 25282, 25282,     0, 25282,     0,     0, 25282, 25282,
 25282, 25282, 25282, 25282, 25282, 25282, 25282,     0,     0,     0,
     0,     0,     0,     0,     0, 27208, 23279,     0, 27451,     0,
   624,     0, 25282,   671,     0,     0, 28619,   671,   671,   671,
 22657, 27113,   953,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0, 25511,   -63,   941,
   434,     0,     0,     0, 22770,  -170,   187,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,    46,     0,     0,
     0, 22770, 25511, 22770,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   701,   484,     0,   745,
     0,     0,   100,     0,   968,   187,     0,     0,   577,     0,
     0,     0,   557, 25282, 27529, 23279, 27568, 23642,     0,     0,
   671, 24134,     0,   671,   671,   135,     0,   992,     0, 25282,
   993,     0,   135,   995,     0,   135,   499,     0,     0,     0,
     0,     0, 26286,     0, 25282,   916,   919, 25282, 27529, 27568,
   671,  2226,   109,   135,     0,     0, 23049,     0,   135,     0,
 24248,     0,     0,     0, 24363,     0,     0,     0,   782,     0,
     0,     0,  1005,     0, 27607, 23279, 27646, 29472,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  1564,
  -203,  1636,  -169,     0,     0,     0,     0,     0,     0,     0,
     0,   697,     0,     0,     0,     0,     0,     0,   135,  1012,
     0,  1016,     0,  1025,     0,  1027,     0,     0,     0,     0,
 25282,  1023,     0,  1048,   135,   789,   792,   332,   837,   852,
 25282,   100,  1066,   837, 25282,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   590,  6603,  6603,  6603,  6603,
 18092,  9450,  6603,  6603,  8945,  8945,   132,   132,     0, 21327,
  1816,  1816,  2307,   148,   148,   590,   590,   590,   848,   837,
     0,   987,     0,   837,   768,     0,   770,     0,   133,     0,
     0,  1078,   135,   794,     0,   836,     0,   133,     0,     0,
     0,   848,     0,     0, 25399,     0,     0,   133, 25399, 24479,
 24479,   135, 29472,  1086,     0,  -170,     0,     0,     0,     0,
     0, 27706, 23279, 28026, 22770,   837,     0, 22770,   872, 25511,
     0,     0,     0,   837,   203,   837,     0, 28065, 23279, 28104,
     0,   930,   927, 22770, 26286, 25282,     0,   866,   870,   135,
   875,   885, 25282,   445,     0,   771, 25282,     0, 25282, 25282,
     0,     0, 23771,     0, 24134,     0,     0,     0, 25511, 27961,
     0,     0,   590,   133,   133, 25282,     0,     0,     0,     0,
   418, 29472,     0,     0,   135,     0,     0,  1005,  5880,     0,
  1666,     0,     0,    24,  1188,     0,     0,    31,  1194,     0,
   697,  1950,     0,  1199,   135,  1222,     0,     0,  3040,     0,
  3264,     0,   202,     0,  2745,     0,     0, 25282,    10,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   598,     0,
 24592, 22080,     0,  3264,     0, 28654, 26055, 26055,  1210,     0,
     0,     0,     0,     0,   671,   671,     0,   624, 23642,   942,
  1204,   671,   671,     0,     0,   624,     0,  1175, 28695,  1008,
   498,     0,     0,     0,   255,  1253,   -35,   278,     0,     0,
 25282, 28695,     0,  1278,     0,     0,     0,     0,     0,     0,
  1031,     0,  1005, 29472,   100, 25511, 22770,     0,     0,     0,
   135,   837,     0,   925,     0,   499, 27021,     0,     0,     0,
     0,     0,     0,     0,   135,     0,     0, 22770,     0,   837,
   927,     0,   837, 24709,  1054,     0,     0,   671,   671,   983,
   671,   671,     0,     0,  1291,   135,   499,     0,     0,     0,
     0,  3264,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   135,     0,   697,     0,  1499,
     0,     0,  1300,  1301,   135,  1307,     0,  1312,     0,     0,
  1048,  1052,  1307,     0,     0,     0,     0,     0,     0,     0,
 22770,     0,  1011,     0,     0, 25282, 25282,     0,     0,     0,
     0,     0,     0,     0,     0,     0,  4132,  4132,   392,     0,
 13454,   135,  1059,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  1021,     0,  1251,     0,     0,    73,     0,     0,
    37,  1021, 25282,  1258,  1258, 25399, 25399,     0,   671,     0,
 25399, 25399,     0,     0, 25282, 23396, 28143, 23279, 28182,   671,
     0,     0,     0, 24822, 23396,  1005, 22770,   100,   837,     0,
     0,     0,   837,     0,     0, 22770, 25511,     0,     0,     0,
   837,     0,     0,   837,     0, 25282,     0,   791,   837, 25282,
 25282,   671, 25282, 25282, 24134,     0,     0,   135,  -145,     0,
     0,     0,  1344,  1349,  3264,     0,  2745,     0,     0,  2745,
     0,  2745,     0,     0,     0, 22770,     0, 29583,   187,     0,
 27961, 27961,     0,     0,     0,     0, 26055,  1092,  1021,   135,
   135, 28836,     0,  1356,  1357,     0,     0,  1293,  1441,     0,
  1115,     0, 26055,  4132,  4132,   392,   135,   135, 28755, 28755,
     0, 27961,  1714, 22770,     0,     0, 22770,     0,     0,     0,
     0, 25399,     0,     0,     0,     0, 27961,     0,   768,   770,
   135,   794,   836, 25399, 25282,     0,     0,   837,     0,     0,
     0,     0,   187,     0, 28755,     0,     0, 24939, 22770,     0,
 25282,  1381,  1365, 22770, 22770,     0, 22770,  1499,  1499,  1307,
  1384,  1307,  1307,  1174,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,  1129,   735,     0,     0,
 22770,    64,     0,     0,     0,    21,  1021,  1396,     0, 26055,
     0,     0,     0,   135,  1398,     0,     0,     0,     0, 26055,
   583,     0,     0,     0,     0,     0,     0,     0,     0,   135,
   135,   135,   135,   135,   135,     0,  1402,     0,  1409,   135,
  1417,     0,  1338,  1419,     0, 29694,  1364,  1048,     0,     0,
     0,     0,   942,     0,     0, 22770,   187,   952,   -14,     0,
     0,     0, 25282,     0,   634,     0,  2250,   837,  1353,  1112,
  1349,     0,  2745,     0,     0,     0,     0,     0, 28221, 23279,
 28541,   852,     0,  1437, 26055,     0,  1438,     0,  1007,     0,
  1441,  1021,     0,     0,   755,     0,     0,     0,     0,     0,
     0,  3264,     0,  3264,     0,     0,  1369,   202,     0,     0,
  2745,     0,     0,     0,  1179,   751, 29694,     0,   925,     0,
 25511, 25511, 28917,     0,     0,     0,     0,     0,   641,     0,
     0,     0,     0,     0,  1307,     0,     0,   135,     0,     0,
     0, 26055,  1454, 28917,     0,     0,     0,     0,     0,  1455,
  1466,  1473,  1475,     0,     0,  1048,  1455,     0, 28580,   751,
     0, 22770,     0,     0,     0,  1479,  2250,     0,     0,     0,
  1454, 26055,     0,     0,  2745,     0,  2745,     0,  3264,     0,
  2745,     0,     0,     0,     0,     0,   527, 26055,     0,  1455,
  1455,  1484,  1455,     0,     0,     0,  1454,     0,     0,  2745,
     0,     0,  1455,     0,
    }, yyRindex = {
//yyRindex 1344
     0,     0,   849,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0, 16934, 17029,     0,     0,     0,
 14105, 13628, 17623, 17936, 18026, 18118, 25622,     0, 25053,     0,
     0, 18431, 18521, 18613, 14217,  4467, 18926, 19016, 14582, 19108,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   835,   258,  1436,  1408,   162,     0,  1691,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  7959,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,  1407,  1407,     0,    81,     0,   178,
  6555, 12434,  8273,  2286,     0,  8368,     0, 23888, 10295,     0,
     0,     0, 19974,     0, 19421,     0,     0,     0,     0,   525,
     0,     0,     0,     0, 17127,     0,     0,     0,     0,     0,
     0,     0,     0,  1263,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  7062,     0,     0,     0,     0,  5738,  5833,  5931,  6245,
     0,  6340,  6438,  6752,  3383,  6847,  6945,  3503,  7259,     0,
     0,   835,     0,     0,     0,     0,     0, 13161,     0,  3199,
     0,  9090,     0,     0,     0,  9090,     0,  7766,     0,     0,
  1498,     0,     0,   395,     0,     0,  1498,     0,     0,     0,
     0, 25734,   127,     0,   127,  8780,     0,     0,  8466,  7354,
     0,     0,     0,     0,  9285, 12486, 12536, 19511,     0,     0,
   835,     0,  1527,     0,  1041,     0,   102,  1498,   538,  1498,
  1451,     0,  1451,     0,     0,     0,  1422,     0,  1308,  2043,
  2133,  2525,  1505,  2636,  2856,  3254,  1850,  3478,  3547,  1972,
  4026,     0,     0,     0,  2375,  2375,     0,     0,  4508,   775,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   650,  2234,
     0, 21571,     0,   142,  2234,     0,     0,     0,     0,    99,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  8875,  8971,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,    96,     0,     0,     0,
 10100,     0,     0, 25851,     0,     0,     0, 25851, 25169, 25169,
   835,   869,   918,     0,     0,     0,     0,     0,     0,     0,
     0,     0, 20306,     0,     0, 20418,     0,     0,     0, 21683,
     0,     0,     0,     0,  2234,  6048,     0,  5496, 11049, 11982,
 13307, 16676, 17291, 20489, 20928, 20940,     0,     0,     0,     0,
     0,   121,     0,   121,   248,   405,   720,  1211,  1404,  1988,
  2061,  2669,  2092,  2319,  3093,  2829,     0,     0,  2927,     0,
     0,     0,    30,     0,   351,     0,     0,     0,  7861,     0,
     0,     0,     0,     0,     0,    96,     0,     0,     0,     0,
 25851,     0,     0, 25851, 25851,  1498,     0,     0,     0,   757,
   873,     0,  1498,   807,     0,  1498,  1498,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
 25851,  1773,     0,  1498,     0,     0,  2768,   154,  1498,     0,
  1461,     0,     0,     0,   158,     0,     0,     0,     0,     0,
  4990,     0,  1022,     0,     0,    96,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  1498,   955,
     0,   140,     0,   312,     0,   140,     0,     0,     0,     0,
    85,   105,     0,   312,  1498,     0,   161,   297,     0,  1249,
     0,     0,   140,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  9380, 11538, 11678, 11798, 11859,
 11977, 12373, 12124, 12237, 12650, 12785, 10787, 10882,     0,  1502,
 11227, 11323, 10978, 10390, 10486,  9476,  9790,  9885,  2950,     0,
     0,     0,     0,     0, 14694,  4832, 16125,     0, 23888,     0,
  4949,   310,  1470, 15059,     0, 15171,     0, 13740,     0,     0,
     0, 11418,     0,     0,     0,     0,     0, 16602,     0,     0,
     0,  1498,     0,  1047,     0,   245,     0, 21172,     0,     0,
     0,     0,    96,     0,  2234,     0,     0,  1397, 21284,     0,
     0,     0,     0,     0,     0,     0,  3014,     0,    96,     0,
     0,  1294,     0,   505,     0,     0,     0,  3868,  5314,  1470,
  3985,  4350,     0,  8682,     0,  9090,     0,     0,     0,     0,
     0,     0,  1063,     0,   878,     0,     0,     0,     0, 10606,
     0,     0,  9981,     0,  7452,     0,     0,  1289,     0,     0,
  1451,     0,  2801,  1252,  1470,  2808,  2843,  1191,  -136,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   270,     0,  1158,  1498,  1171,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  1306,     0,
    50, 13209,     0,     0,     0, 13278,     0,     0,     0,     0,
     0,     0,     0,     0, 25851, 25851,     0, 10241,   503, 17441,
     0, 25851, 25851,     0,     0, 11129,     0,     0, 13329,   892,
     0,     0,     0,     0, 19603,     0,  9595, 20016,     0,     0,
     0, 20174,     0,     0,     0,     0,     0,     0,  2659,     0,
  1610,     0,  1198,     0,     0,     0,  2234, 20750, 20862,     0,
  1470,     0,     0,  1306,     0,  1498,     0,     0,     0,     0,
     0,     0,  3105,  1113,  1470,  3180,  3843,   121,     0,     0,
     0,     0,     0,     0,  1306,     0,     0, 25851, 25851,  5424,
 25851, 25851,     0,     0,   807,  1498,  1498,     0,     0,     0,
  1646,   957,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  1498,     0,     0,     0,     0,
     0,     0,   140,   140,  1498,   140,     0,   140,     0,     0,
   312,   297,   140,     0,     0,     0,     0,     0,     0,     0,
   121,    59,   672,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  1470,   961,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0, 20066,     0, 19914,     0,     0, 10145,     0,     0,
 19701, 20127,     0, 21892, 22004,     0,     0,     0, 25851,     0,
     0,     0,     0,     0,     0,     0,     0,    96,     0, 25851,
     0,     0,  9135,     0,     0,  1362,  2234,     0,     0,     0,
     0,     0,     0,     0,     0,   121,     0,     0,     0,  2915,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 25851,     0,     0,   922,     0,     0,   840,     0,     0,
     0,     0,  1186,  1228,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   686,     0,     0,     0,     0,
 12850, 12895,     0,     0,     0,     0,     0,   811,  2425,  1470,
  1470,  1598,     0,     0,  1735,     0,     0,     0,     0,     0,
     0,     0,     0, 19791, 19852,     0, 28478, 21398,     0,     0,
     0, 12964,    43,  2234,     0,     0,   127,     0,     0,     0,
     0,     0,     0,     0,     0,     0, 13029,     0, 15536, 16490,
  1470, 15648, 16013,     0,     0,  9640,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  1172,     0,
     0,     0,   497,  2234,   127,     0,    41,     0,     0,   140,
   140,   140,   140,  1306,  1248,  1322,  1492,  1568,  1650,  1908,
  1958,   551,  2063,  2159,  1664,  2267,     0,     0,  2277,     0,
  2234,  1498,     0,     0,     0,  2047,  5416,  3260,     0,     0,
     0,     0,     0,  1439,   977,     0,     0,     0,     0,  3111,
  5431,     0,     0,     0,     0,     0,     0,     0,     0,  1498,
  1498,  1498,  1470,  1470,  1470,     0,   296,     0,   296,    43,
   519,     0,     0,   296,     0,   234,  1376,   519,     0,     0,
     0,     0, 17531, 11583,     0,  1397,     0,   743,   492,     0,
     0,     0,     0,     0,  1306,     0,     0,     0,     0,     0,
  1243,     0,     0,     0,     0,     0,     0,  2484,     0,    96,
     0,  1249,     0,  5435,     0,     0,  5470,     0,     0,     0,
  5433,  3284,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   797,     0,     0,     0,     0,     0,     0,
     0,     0,  1028,  2171,     0,  1596,     0,     0,  1306,     0,
     0,     0,   640,     0,     0,     0,     0,     0,   653,     0,
     0,     0,     0,     0,   140,  1720,  1190,  1470,  2327,  2893,
     0,     0,  5824,     0,     0,     0,     0,     0,     0,   296,
   296,   296,   296,     0,     0,   519,   296,     0,     0,  1758,
     0,  1482,     0,     0,     0,   850,     0,     0,     0,   493,
  5826,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,  2449,   478,  1306,     0,     0,   296,
   296,   296,   296,     0,     0,     0,  1230,     0,     0,     0,
     0,     0,   296,     0,
    }, yyGindex = {
//yyGindex 266
     0,     0,   889,     0,  1501,  1122,  1328,   -59,     0,     0,
  -135,  1379,  1574,     0,  1739,  1759,     0,     0,     0,  1024,
  1924,     0,    23,     0,     0,    15,  1460,   731,    39,    78,
  1330,     0,    76,  1076,  -305,    27,     0,  1083,     1,   -10,
  2405,   103,    -9,   -53,     0,  -114,  -119,   914,    18,   371,
     0,   344,  -825,  -824,     0,     0,   390,     0,     0,   489,
   490,  1964,  -318,    14,   964,  -309,  -455,   546,  2708,    -3,
     0,  -218,  -449,  1503,  1631,  -339,   761,  -138,  -570,     0,
     0,     0,     0,   382, -1111,   -18,  1538,   978,  -293,  -297,
  -731,  -433,  -859,  -821,   879,   733,     0,    42,  -437,     0,
  1126,     0,     0,     0,   694,     0,  -676,     0,   874,     0,
   388,     0,  -965,   358,  2028,     0,     0,  1030,  1287,   -72,
   229,   854,  1262,    -2,   -19,  1551,     0,   -28,   -94,   -11,
  -451,  -179,   354,     0,     0,     0,  -322,     0,     0,   615,
   107,  -944,     0,   578, -1032,  -204,     0,   -73,   628,     0,
     0,   293,     0,   465,     0,     0,  -220,     0,  -392,    34,
   -43,  -694,   299,  -534,  -460, -1102,  -749,  1420,  -283,   -91,
     0,     0,  1600,     0,  -500,     0,     0,   469,     0,     0,
  2632,  -266,     0,  1075,     0,     0,  -495,   677,  -192,     0,
  1162,     0,     0,   923,     0,     0,     0,     0,     0,     0,
     0,     0,   547,     0,   592,     0,     0,     0,     0,     0,
     0,     0,     0,   -64,    36,     0,     0,     0,   471,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  -239,     0,     0,     0,     0,     0,  -453,     0,     0,     0,
   -50,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,
    };
    protected static final short[] yyTable = YyTables.yyTable();
    protected static final short[] yyCheck = YyTables.yyCheck();

  /** maps symbol value to printable name.
      @see #yyExpecting
    */
  protected static final String[] yyNames = {
    "end-of-file",null,null,null,null,null,null,null,null,"escaped horizontal tab","'\\n'",
"escaped vertical tab","escaped form feed","escaped carriage return",null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,"' '","'!'",null,null,
    null,"'%'","'&'",null,"'('","')'","'*'","'+'","','","'-'","'.'","'/'",
    null,null,null,null,null,null,null,null,null,null,"':'","';'","'<'",
    "'='","'>'","'?'",null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,"'['","backslash","']'","'^'",null,"'`'",null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,"'{'","'|'","'}'","'~'",
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,"`class''","`module'","`def'",
"`undef'","`begin'","`rescue'","`ensure'",
"`end'","`if'","`unless'","`then'",
"`elsif'","`else'","`case'","`when'",
"`while'","`until'","`for'","`break'",
"`next'","`redo'","`retry'","`in'",
"`do'","`do' for condition","`do' for block","`do' for lambda",
"`return'","`yield'","`super'","`self'",
"`nil'","`true'","`false'","`and'",
"`or'","`not'","`if' modifier","`unless' modifier",
"`while' modifier","`until' modifier","`rescue' modifier","`alias'",
"`defined'","`BEGIN'","`END'","`__LINE__'",
"`__FILE__'","`__ENCODING__'","local variable or method","method","global variable",
"instance variable","constant","class variable","label","integer literal","float literal","rational literal",
"imaginary literal","char literal","numbered reference","back reference","literal content",
    "tREGEXP_END","tUMINUS_NUM","escaped space","unary+","unary-","**","<=>",
"==","===","!=",">=","<=","&&","||","=~","!~",
"..","...","(..","(...","[]","[]=","<<",">>",
"&.","::",":: at EXPR_BEG","operator assignment","=>","'('",
"( arg","'['","'{'","{ arg","'*'","**arg",
"'&'","->","symbol literal","string literal","backtick literal",
"regexp literal","word list","verbatim work list","terminator","symbol list",
"verbatim symbol list","'}'","tSTRING_DBEG","tSTRING_DVAR",
    "tLAMBEG","tLABEL_END","tLOWEST",
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
    "top_stmt : keyword_BEGIN begin_block",
    "begin_block : '{' top_compstmt '}'",
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
    "stmt_or_begin : keyword_BEGIN $$3 begin_block",
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
    "stmt : keyword_END '{' compstmt '}'",
    "stmt : command_asgn",
    "stmt : mlhs '=' lex_ctxt command_call",
    "stmt : lhs '=' lex_ctxt mrhs",
    "stmt : mlhs '=' lex_ctxt mrhs_arg modifier_rescue stmt",
    "stmt : mlhs '=' lex_ctxt mrhs_arg",
    "stmt : expr",
    "command_asgn : lhs '=' lex_ctxt command_rhs",
    "command_asgn : var_lhs tOP_ASGN lex_ctxt command_rhs",
    "command_asgn : primary_value '[' opt_call_args rbracket tOP_ASGN lex_ctxt command_rhs",
    "command_asgn : primary_value call_op tIDENTIFIER tOP_ASGN lex_ctxt command_rhs",
    "command_asgn : primary_value call_op tCONSTANT tOP_ASGN lex_ctxt command_rhs",
    "command_asgn : primary_value tCOLON2 tCONSTANT tOP_ASGN lex_ctxt command_rhs",
    "command_asgn : primary_value tCOLON2 tIDENTIFIER tOP_ASGN lex_ctxt command_rhs",
    "command_asgn : backref tOP_ASGN lex_ctxt command_rhs",
    "command_rhs : command_call",
    "command_rhs : command_call modifier_rescue stmt",
    "command_rhs : command_asgn",
    "expr : command_call",
    "expr : expr keyword_and expr",
    "expr : expr keyword_or expr",
    "expr : keyword_not opt_nl expr",
    "expr : '!' command_call",
    "$$5 :",
    "$$6 :",
    "$$7 :",
    "expr : arg tASSOC $$5 $$6 p_expr $$7",
    "$$8 :",
    "$$9 :",
    "$$10 :",
    "expr : arg keyword_in $$8 $$9 p_expr $$10",
    "expr : arg",
    "def_name : fname",
    "defn_head : k_def def_name",
    "$$11 :",
    "defs_head : k_def singleton dot_or_colon $$11 def_name",
    "expr_value : expr",
    "$$12 :",
    "expr_value_do : $$12 expr_value do",
    "command_call : command",
    "command_call : block_command",
    "block_command : block_call",
    "block_command : block_call call_op2 operation2 command_args",
    "cmd_brace_block : tLBRACE_ARG brace_body '}'",
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
    "fitem : fname",
    "fitem : symbol",
    "undef_list : fitem",
    "$$13 :",
    "undef_list : undef_list ',' $$13 fitem",
    "op : '|'",
    "op : '^'",
    "op : '&'",
    "op : tCMP",
    "op : tEQ",
    "op : tEQQ",
    "op : tMATCH",
    "op : tNMATCH",
    "op : '>'",
    "op : tGEQ",
    "op : '<'",
    "op : tLEQ",
    "op : tNEQ",
    "op : tLSHFT",
    "op : tRSHFT",
    "op : '+'",
    "op : '-'",
    "op : '*'",
    "op : tSTAR",
    "op : '/'",
    "op : '%'",
    "op : tPOW",
    "op : tDSTAR",
    "op : '!'",
    "op : '~'",
    "op : tUPLUS",
    "op : tUMINUS",
    "op : tAREF",
    "op : tASET",
    "op : '`'",
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
    "arg : lhs '=' lex_ctxt arg_rhs",
    "arg : var_lhs tOP_ASGN lex_ctxt arg_rhs",
    "arg : primary_value '[' opt_call_args rbracket tOP_ASGN lex_ctxt arg_rhs",
    "arg : primary_value call_op tIDENTIFIER tOP_ASGN lex_ctxt arg_rhs",
    "arg : primary_value call_op tCONSTANT tOP_ASGN lex_ctxt arg_rhs",
    "arg : primary_value tCOLON2 tIDENTIFIER tOP_ASGN lex_ctxt arg_rhs",
    "arg : primary_value tCOLON2 tCONSTANT tOP_ASGN lex_ctxt arg_rhs",
    "arg : tCOLON3 tCONSTANT tOP_ASGN lex_ctxt arg_rhs",
    "arg : backref tOP_ASGN lex_ctxt arg_rhs",
    "arg : arg tDOT2 arg",
    "arg : arg tDOT3 arg",
    "arg : arg tDOT2",
    "arg : arg tDOT3",
    "arg : tBDOT2 arg",
    "arg : tBDOT3 arg",
    "arg : arg '+' arg",
    "arg : arg '-' arg",
    "arg : arg '*' arg",
    "arg : arg '/' arg",
    "arg : arg '%' arg",
    "arg : arg tPOW arg",
    "arg : tUMINUS_NUM simple_numeric tPOW arg",
    "arg : tUPLUS arg",
    "arg : tUMINUS arg",
    "arg : arg '|' arg",
    "arg : arg '^' arg",
    "arg : arg '&' arg",
    "arg : arg tCMP arg",
    "arg : rel_expr",
    "arg : arg tEQ arg",
    "arg : arg tEQQ arg",
    "arg : arg tNEQ arg",
    "arg : arg tMATCH arg",
    "arg : arg tNMATCH arg",
    "arg : '!' arg",
    "arg : '~' arg",
    "arg : arg tLSHFT arg",
    "arg : arg tRSHFT arg",
    "arg : arg tANDOP arg",
    "arg : arg tOROP arg",
    "$$14 :",
    "arg : keyword_defined opt_nl $$14 arg",
    "arg : arg '?' arg opt_nl ':' arg",
    "arg : defn_head f_opt_paren_args '=' arg",
    "arg : defn_head f_opt_paren_args '=' arg modifier_rescue arg",
    "arg : defs_head f_opt_paren_args '=' arg",
    "arg : defs_head f_opt_paren_args '=' arg modifier_rescue arg",
    "arg : primary",
    "relop : '>'",
    "relop : '<'",
    "relop : tGEQ",
    "relop : tLEQ",
    "rel_expr : arg relop arg",
    "rel_expr : rel_expr relop arg",
    "lex_ctxt : tSP",
    "lex_ctxt : none",
    "arg_value : arg",
    "aref_args : none",
    "aref_args : args trailer",
    "aref_args : args ',' assocs trailer",
    "aref_args : assocs trailer",
    "arg_rhs : arg",
    "arg_rhs : arg modifier_rescue arg",
    "paren_args : '(' opt_call_args rparen",
    "paren_args : '(' args ',' args_forward rparen",
    "paren_args : '(' args_forward rparen",
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
    "$$15 :",
    "command_args : $$15 call_args",
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
    "$$16 :",
    "primary : k_begin $$16 bodystmt k_end",
    "$$17 :",
    "primary : tLPAREN_ARG $$17 rparen",
    "$$18 :",
    "primary : tLPAREN_ARG stmt $$18 rparen",
    "primary : tLPAREN compstmt ')'",
    "primary : primary_value tCOLON2 tCONSTANT",
    "primary : tCOLON3 tCONSTANT",
    "primary : tLBRACK aref_args ']'",
    "primary : tLBRACE assoc_list '}'",
    "primary : k_return",
    "primary : keyword_yield '(' call_args rparen",
    "primary : keyword_yield '(' rparen",
    "primary : keyword_yield",
    "$$19 :",
    "primary : keyword_defined opt_nl '(' $$19 expr rparen",
    "primary : keyword_not '(' expr rparen",
    "primary : keyword_not '(' rparen",
    "primary : fcall brace_block",
    "primary : method_call",
    "primary : method_call brace_block",
    "primary : lambda",
    "primary : k_if expr_value then compstmt if_tail k_end",
    "primary : k_unless expr_value then compstmt opt_else k_end",
    "primary : k_while expr_value_do compstmt k_end",
    "primary : k_until expr_value_do compstmt k_end",
    "$$20 :",
    "primary : k_case expr_value opt_terms $$20 case_body k_end",
    "$$21 :",
    "primary : k_case opt_terms $$21 case_body k_end",
    "primary : k_case expr_value opt_terms p_case_body k_end",
    "primary : k_for for_var keyword_in expr_value_do compstmt k_end",
    "$$22 :",
    "primary : k_class cpath superclass $$22 bodystmt k_end",
    "$$23 :",
    "primary : k_class tLSHFT expr $$23 term bodystmt k_end",
    "$$24 :",
    "primary : k_module cpath $$24 bodystmt k_end",
    "primary : defn_head f_arglist bodystmt k_end",
    "primary : defs_head f_arglist bodystmt k_end",
    "primary : keyword_break",
    "primary : keyword_next",
    "primary : keyword_redo",
    "primary : keyword_retry",
    "primary_value : primary",
    "k_begin : keyword_begin",
    "k_if : keyword_if",
    "k_unless : keyword_unless",
    "k_while : keyword_while",
    "k_until : keyword_until",
    "k_case : keyword_case",
    "k_for : keyword_for",
    "k_class : keyword_class",
    "k_module : keyword_module",
    "k_def : keyword_def",
    "k_do : keyword_do",
    "k_do_block : keyword_do_block",
    "k_rescue : keyword_rescue",
    "k_ensure : keyword_ensure",
    "k_when : keyword_when",
    "k_else : keyword_else",
    "k_elsif : keyword_elsif",
    "k_end : keyword_end",
    "k_return : keyword_return",
    "then : term",
    "then : keyword_then",
    "then : term keyword_then",
    "do : term",
    "do : keyword_do_cond",
    "if_tail : opt_else",
    "if_tail : k_elsif expr_value then compstmt if_tail",
    "opt_else : none",
    "opt_else : k_else compstmt",
    "for_var : lhs",
    "for_var : mlhs",
    "f_marg : f_norm_arg",
    "f_marg : tLPAREN f_margs rparen",
    "f_marg_list : f_marg",
    "f_marg_list : f_marg_list ',' f_marg",
    "f_margs : f_marg_list",
    "f_margs : f_marg_list ',' f_rest_marg",
    "f_margs : f_marg_list ',' f_rest_marg ',' f_marg_list",
    "f_margs : f_rest_marg",
    "f_margs : f_rest_marg ',' f_marg_list",
    "f_rest_marg : tSTAR f_norm_arg",
    "f_rest_marg : tSTAR",
    "f_any_kwrest : f_kwrest",
    "f_any_kwrest : f_no_kwarg",
    "block_args_tail : f_block_kwarg ',' f_kwrest opt_f_block_arg",
    "block_args_tail : f_block_kwarg opt_f_block_arg",
    "block_args_tail : f_any_kwrest opt_f_block_arg",
    "block_args_tail : f_block_arg",
    "opt_block_args_tail : ',' block_args_tail",
    "opt_block_args_tail :",
    "excessed_comma : ','",
    "block_param : f_arg ',' f_block_optarg ',' f_rest_arg opt_block_args_tail",
    "block_param : f_arg ',' f_block_optarg ',' f_rest_arg ',' f_arg opt_block_args_tail",
    "block_param : f_arg ',' f_block_optarg opt_block_args_tail",
    "block_param : f_arg ',' f_block_optarg ',' f_arg opt_block_args_tail",
    "block_param : f_arg ',' f_rest_arg opt_block_args_tail",
    "block_param : f_arg excessed_comma",
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
    "block_param_def : '|' opt_bv_decl '|'",
    "block_param_def : '|' block_param opt_bv_decl '|'",
    "opt_bv_decl : opt_nl",
    "opt_bv_decl : opt_nl ';' bv_decls opt_nl",
    "bv_decls : bvar",
    "bv_decls : bv_decls ',' bvar",
    "bvar : tIDENTIFIER",
    "bvar : f_bad_arg",
    "$$25 :",
    "$$26 :",
    "$$27 :",
    "$$28 :",
    "lambda : tLAMBDA $$25 $$26 $$27 f_larglist $$28 lambda_body",
    "f_larglist : '(' f_args opt_bv_decl ')'",
    "f_larglist : f_args",
    "lambda_body : tLAMBEG compstmt '}'",
    "lambda_body : keyword_do_LAMBDA bodystmt k_end",
    "do_block : k_do_block do_body k_end",
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
    "brace_block : '{' brace_body '}'",
    "brace_block : k_do do_body k_end",
    "$$29 :",
    "$$30 :",
    "$$31 :",
    "brace_body : $$29 $$30 $$31 opt_block_param compstmt",
    "$$32 :",
    "$$33 :",
    "$$34 :",
    "do_body : $$32 $$33 $$34 opt_block_param bodystmt",
    "case_args : arg_value",
    "case_args : tSTAR arg_value",
    "case_args : case_args ',' arg_value",
    "case_args : case_args ',' tSTAR arg_value",
    "case_body : k_when case_args then compstmt cases",
    "cases : opt_else",
    "cases : case_body",
    "$$35 :",
    "$$36 :",
    "$$37 :",
    "p_case_body : keyword_in $$35 $$36 p_top_expr then $$37 compstmt p_cases",
    "p_cases : opt_else",
    "p_cases : p_case_body",
    "p_top_expr : p_top_expr_body",
    "p_top_expr : p_top_expr_body modifier_if expr_value",
    "p_top_expr : p_top_expr_body modifier_unless expr_value",
    "p_top_expr_body : p_expr",
    "p_top_expr_body : p_expr ','",
    "p_top_expr_body : p_expr ',' p_args",
    "p_top_expr_body : p_find",
    "p_top_expr_body : p_args_tail",
    "p_top_expr_body : p_kwargs",
    "p_expr : p_as",
    "p_as : p_expr tASSOC p_variable",
    "p_as : p_alt",
    "p_alt : p_alt '|' p_expr_basic",
    "p_alt : p_expr_basic",
    "p_lparen : '('",
    "p_lbracket : '['",
    "p_expr_basic : p_value",
    "p_expr_basic : p_const p_lparen p_args rparen",
    "p_expr_basic : p_const p_lparen p_find rparen",
    "p_expr_basic : p_const p_lparen p_kwargs rparen",
    "p_expr_basic : p_const '(' rparen",
    "p_expr_basic : p_const p_lbracket p_args rbracket",
    "p_expr_basic : p_const p_lbracket p_find rbracket",
    "p_expr_basic : p_const p_lbracket p_kwargs rbracket",
    "p_expr_basic : p_const '[' rbracket",
    "p_expr_basic : tLBRACK p_args rbracket",
    "p_expr_basic : tLBRACK p_find rbracket",
    "p_expr_basic : tLBRACK rbracket",
    "$$38 :",
    "p_expr_basic : tLBRACE $$38 p_kwargs rbrace",
    "p_expr_basic : tLBRACE rbrace",
    "$$39 :",
    "p_expr_basic : tLPAREN $$39 p_expr rparen",
    "p_args : p_expr",
    "p_args : p_args_head",
    "p_args : p_args_head p_arg",
    "p_args : p_args_head tSTAR tIDENTIFIER",
    "p_args : p_args_head tSTAR tIDENTIFIER ',' p_args_post",
    "p_args : p_args_head tSTAR",
    "p_args : p_args_head tSTAR ',' p_args_post",
    "p_args : p_args_tail",
    "p_args_head : p_arg ','",
    "p_args_head : p_args_head p_arg ','",
    "p_args_tail : p_rest",
    "p_args_tail : p_rest ',' p_args_post",
    "p_find : p_rest ',' p_args_post ',' p_rest",
    "p_rest : tSTAR tIDENTIFIER",
    "p_rest : tSTAR",
    "p_args_post : p_arg",
    "p_args_post : p_args_post ',' p_arg",
    "p_arg : p_expr",
    "p_kwargs : p_kwarg ',' p_any_kwrest",
    "p_kwargs : p_kwarg",
    "p_kwargs : p_kwarg ','",
    "p_kwargs : p_any_kwrest",
    "p_kwarg : p_kw",
    "p_kwarg : p_kwarg ',' p_kw",
    "p_kw : p_kw_label p_expr",
    "p_kw : p_kw_label",
    "p_kw_label : tLABEL",
    "p_kw_label : tSTRING_BEG string_contents tLABEL_END",
    "p_kwrest : kwrest_mark tIDENTIFIER",
    "p_kwrest : kwrest_mark",
    "p_kwnorest : kwrest_mark keyword_nil",
    "p_any_kwrest : p_kwrest",
    "p_any_kwrest : p_kwnorest",
    "p_value : p_primitive",
    "p_value : p_primitive tDOT2 p_primitive",
    "p_value : p_primitive tDOT3 p_primitive",
    "p_value : p_primitive tDOT2",
    "p_value : p_primitive tDOT3",
    "p_value : p_variable",
    "p_value : p_var_ref",
    "p_value : p_const",
    "p_value : tBDOT2 p_primitive",
    "p_value : tBDOT3 p_primitive",
    "p_primitive : literal",
    "p_primitive : strings",
    "p_primitive : xstring",
    "p_primitive : regexp",
    "p_primitive : words",
    "p_primitive : qwords",
    "p_primitive : symbols",
    "p_primitive : qsymbols",
    "p_primitive : keyword_nil",
    "p_primitive : keyword_self",
    "p_primitive : keyword_true",
    "p_primitive : keyword_false",
    "p_primitive : keyword__FILE__",
    "p_primitive : keyword__LINE__",
    "p_primitive : keyword__ENCODING__",
    "p_primitive : lambda",
    "p_variable : tIDENTIFIER",
    "p_var_ref : '^' tIDENTIFIER",
    "p_const : tCOLON3 cname",
    "p_const : p_const tCOLON2 cname",
    "p_const : tCONSTANT",
    "opt_rescue : k_rescue exc_list exc_var then compstmt opt_rescue",
    "opt_rescue : none",
    "exc_list : arg_value",
    "exc_list : mrhs",
    "exc_list : none",
    "exc_var : tASSOC lhs",
    "exc_var : none",
    "opt_ensure : k_ensure compstmt",
    "opt_ensure : none",
    "literal : numeric",
    "literal : symbol",
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
    "$$40 :",
    "string_content : tSTRING_DVAR $$40 string_dvar",
    "$$41 :",
    "$$42 :",
    "$$43 :",
    "$$44 :",
    "string_content : tSTRING_DBEG $$41 $$42 $$43 $$44 compstmt tSTRING_DEND",
    "string_dvar : tGVAR",
    "string_dvar : tIVAR",
    "string_dvar : tCVAR",
    "string_dvar : backref",
    "symbol : ssym",
    "symbol : dsym",
    "ssym : tSYMBEG sym",
    "sym : fname",
    "sym : tIVAR",
    "sym : tGVAR",
    "sym : tCVAR",
    "dsym : tSYMBEG string_contents tSTRING_END",
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
    "$$45 :",
    "superclass : '<' $$45 expr_value term",
    "superclass :",
    "f_opt_paren_args : f_paren_args",
    "f_opt_paren_args : none",
    "f_paren_args : '(' f_args rparen",
    "f_paren_args : '(' f_arg ',' args_forward rparen",
    "f_paren_args : '(' args_forward rparen",
    "f_arglist : f_paren_args",
    "$$46 :",
    "f_arglist : $$46 f_args term",
    "args_tail : f_kwarg ',' f_kwrest opt_f_block_arg",
    "args_tail : f_kwarg opt_f_block_arg",
    "args_tail : f_any_kwrest opt_f_block_arg",
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
    "args_forward : tBDOT3",
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
    "f_no_kwarg : kwrest_mark keyword_nil",
    "f_kwrest : kwrest_mark tIDENTIFIER",
    "f_kwrest : kwrest_mark",
    "f_opt : f_arg_asgn '=' arg_value",
    "f_block_opt : f_arg_asgn '=' primary_value",
    "f_block_optarg : f_block_opt",
    "f_block_optarg : f_block_optarg ',' f_block_opt",
    "f_optarg : f_opt",
    "f_optarg : f_optarg ',' f_opt",
    "restarg_mark : '*'",
    "restarg_mark : tSTAR",
    "f_rest_arg : restarg_mark tIDENTIFIER",
    "f_rest_arg : restarg_mark",
    "blkarg_mark : '&'",
    "blkarg_mark : tAMPER",
    "f_block_arg : blkarg_mark tIDENTIFIER",
    "opt_f_block_arg : ',' f_block_arg",
    "opt_f_block_arg :",
    "singleton : var_ref",
    "$$47 :",
    "singleton : '(' $$47 expr rparen",
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
    "dot_or_colon : '.'",
    "dot_or_colon : tCOLON2",
    "call_op : '.'",
    "call_op : tANDDOT",
    "call_op2 : call_op",
    "call_op2 : tCOLON2",
    "opt_terms :",
    "opt_terms : terms",
    "opt_nl :",
    "opt_nl : '\\n'",
    "rparen : opt_nl ')'",
    "rbracket : opt_nl ']'",
    "rbrace : opt_nl '}'",
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

static ParserState[] states = new ParserState[806];
static {
states[1] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  lexer.setState(EXPR_BEG);
                  support.initTopLocalVariables();
  return yyVal;
};
states[2] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  Node expr = ((Node)yyVals[0+yyTop].value);
                  if (expr != null && !support.getConfiguration().isEvalParse()) {
                      /* last expression should not be void */
                      if (((Node)yyVals[0+yyTop].value) instanceof BlockNode) {
                        expr = ((BlockNode)yyVals[0+yyTop].value).getLast();
                      } else {
                        expr = ((Node)yyVals[0+yyTop].value);
                      }
                      expr = support.remove_begin(expr);
                      support.void_expr(expr);
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
                  yyVal = support.remove_begin(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[9] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  yyVal = null;
  return yyVal;
};
states[10] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  support.getResult().addBeginNode(new PreExe19Node(yyVals[yyTop - count + 1].startLine(), support.getCurrentScope(), ((Node)yyVals[-1+yyTop].value), lexer.getRubySourceline()));
                  /*                  $$ = new BeginNode(yyVals[yyTop - count + 1].startLine(), support.makeNullNil($2));*/
                  yyVal = null;
  return yyVal;
};
states[11] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   if (((RescueBodyNode)yyVals[-1+yyTop].value) == null) support.yyerror("else without rescue is useless"); 
  return yyVal;
};
states[12] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = support.new_bodystmt(((Node)yyVals[-5+yyTop].value), ((RescueBodyNode)yyVals[-4+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[13] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = support.new_bodystmt(((Node)yyVals[-2+yyTop].value), ((RescueBodyNode)yyVals[-1+yyTop].value), null, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[14] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.void_stmts(((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[16] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newline_node(((Node)yyVals[0+yyTop].value), support.getPosition(((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[17] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.appendToBlock(((Node)yyVals[-2+yyTop].value), support.newline_node(((Node)yyVals[0+yyTop].value), support.getPosition(((Node)yyVals[0+yyTop].value))));
  return yyVal;
};
states[18] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[19] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[20] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   support.yyerror("BEGIN is permitted only at toplevel");
  return yyVal;
};
states[21] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[22] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setState(EXPR_FNAME|EXPR_FITEM);
  return yyVal;
};
states[23] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ParserSupport.newAlias(((Integer)yyVals[-3+yyTop].value), ((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[24] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new VAliasNode(((Integer)yyVals[-2+yyTop].value), support.symbolID(((ByteList)yyVals[-1+yyTop].value)), support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[25] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new VAliasNode(((Integer)yyVals[-2+yyTop].value), support.symbolID(((ByteList)yyVals[-1+yyTop].value)), support.symbolID(((BackRefNode)yyVals[0+yyTop].value).getByteName()));
  return yyVal;
};
states[26] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.yyerror("can't make alias for the number variables");
  return yyVal;
};
states[27] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[28] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_if(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.cond(((Node)yyVals[0+yyTop].value)), support.remove_begin(((Node)yyVals[-2+yyTop].value)), null);
                    support.fixpos(((Node)yyVal), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[29] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_if(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.cond(((Node)yyVals[0+yyTop].value)), null, support.remove_begin(((Node)yyVals[-2+yyTop].value)));
                    support.fixpos(((Node)yyVal), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[30] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (((Node)yyVals[-2+yyTop].value) != null && ((Node)yyVals[-2+yyTop].value) instanceof BeginNode) {
                        yyVal = new WhileNode(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.cond(((Node)yyVals[0+yyTop].value)), ((BeginNode)yyVals[-2+yyTop].value).getBodyNode(), false);
                    } else {
                        yyVal = new WhileNode(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.cond(((Node)yyVals[0+yyTop].value)), ((Node)yyVals[-2+yyTop].value), true);
                    }
  return yyVal;
};
states[31] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (((Node)yyVals[-2+yyTop].value) != null && ((Node)yyVals[-2+yyTop].value) instanceof BeginNode) {
                        yyVal = new UntilNode(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.cond(((Node)yyVals[0+yyTop].value)), ((BeginNode)yyVals[-2+yyTop].value).getBodyNode(), false);
                    } else {
                        yyVal = new UntilNode(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.cond(((Node)yyVals[0+yyTop].value)), ((Node)yyVals[-2+yyTop].value), true);
                    }
  return yyVal;
};
states[32] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newRescueModNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[33] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   if (lexer.getLexContext().in_def) {
                       support.warn(ID.END_IN_METHOD, ((Integer)yyVals[-3+yyTop].value), "END in method; use at_exit");
                    }
                    yyVal = new PostExeNode(((Integer)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[35] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = node_assign(((MultipleAsgnNode)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[36] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = node_assign(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[37] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[-2+yyTop].value));
                    yyVal = node_assign(((MultipleAsgnNode)yyVals[-5+yyTop].value), support.newRescueModNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[38] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = node_assign(((MultipleAsgnNode)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[40] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = node_assign(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[41] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_op_assign(((AssignableNode)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[42] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_ary_op_assign(((Node)yyVals[-6+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[43] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value));
  return yyVal;
};
states[44] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value));
  return yyVal;
};
states[45] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    int line = ((Node)yyVals[-5+yyTop].value).getLine();
                    yyVal = support.new_const_op_assign(line, support.new_colon2(line, ((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-3+yyTop].value)), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[46] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value));
  return yyVal;
};
states[47] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.backrefAssignError(((Node)yyVals[-3+yyTop].value));
  return yyVal;
};
states[48] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[49] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[-2+yyTop].value));
                    yyVal = support.newRescueModNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[52] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newAndNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[53] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newOrNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[54] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(support.method_cond(((Node)yyVals[0+yyTop].value)), lexer.BANG);
  return yyVal;
};
states[55] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(support.method_cond(((Node)yyVals[0+yyTop].value)), BANG);
  return yyVal;
};
states[56] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[-1+yyTop].value));
                    lexer.setState(EXPR_BEG|EXPR_LABEL);
                    lexer.commandStart = false;
                    LexContext ctxt = lexer.getLexContext();
                    yyVal = ctxt.in_kwarg;
                    ctxt.in_kwarg = true;
  return yyVal;
};
states[57] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.push_pvtbl();
  return yyVal;
};
states[58] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.pop_pvtbl(((Set)yyVals[-1+yyTop].value));
  return yyVal;
};
states[59] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = lexer.getLexContext();
                    ctxt.in_kwarg = ((Boolean)yyVals[-3+yyTop].value);
                    yyVal = support.newPatternCaseNode(((Node)yyVals[-5+yyTop].value).getLine(), ((Node)yyVals[-5+yyTop].value), support.newIn(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-1+yyTop].value), null, null));
                    support.warn_one_line_pattern_matching(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-1+yyTop].value), true);
  return yyVal;
};
states[60] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[-1+yyTop].value));
                    lexer.setState(EXPR_BEG|EXPR_LABEL);
                    lexer.commandStart = false;
                    LexContext ctxt = lexer.getLexContext();
                    yyVal = ctxt.in_kwarg;
                    ctxt.in_kwarg = true;
  return yyVal;
};
states[61] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.push_pvtbl();
  return yyVal;
};
states[62] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.pop_pvtbl(((Set)yyVals[-1+yyTop].value));
  return yyVal;
};
states[63] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = lexer.getLexContext();
                    ctxt.in_kwarg = ((Boolean)yyVals[-3+yyTop].value);
                    yyVal = support.newPatternCaseNode(((Node)yyVals[-5+yyTop].value).getLine(), ((Node)yyVals[-5+yyTop].value), support.newIn(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-1+yyTop].value), new TrueNode(lexer.tokline), new FalseNode(lexer.tokline)));
                    support.warn_one_line_pattern_matching(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-1+yyTop].value), false);
  return yyVal;
};
states[65] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.pushLocalScope();
                    LexContext ctxt = lexer.getLexContext();
                    RubySymbol name = support.symbolID(((ByteList)yyVals[0+yyTop].value));
                    support.numparam_name(name);
                    yyVal = new DefHolder(support.symbolID(((ByteList)yyVals[0+yyTop].value)), lexer.getCurrentArg(), (LexContext) ctxt.clone());
                    ctxt.in_def = true;
                    lexer.setCurrentArg(null);
  return yyVal;
};
states[66] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    ((DefHolder)yyVals[0+yyTop].value).line = ((Integer)yyVals[-1+yyTop].value);
                    yyVal = ((DefHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[67] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setState(EXPR_FNAME); 
  return yyVal;
};
states[68] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setState(EXPR_ENDFN|EXPR_LABEL);
                    ((DefHolder)yyVals[0+yyTop].value).line = ((Integer)yyVals[-4+yyTop].value);
                    ((DefHolder)yyVals[0+yyTop].value).setSingleton(((Node)yyVals[-3+yyTop].value));
                    yyVal = ((DefHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[69] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[70] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.getConditionState().push1();
  return yyVal;
};
states[71] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.getConditionState().pop();
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[75] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].startLine());
  return yyVal;
};
states[76] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IterNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[77] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_fcall(((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[78] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.frobnicate_fcall_args(((FCallNode)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
                    yyVal = ((FCallNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[79] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.frobnicate_fcall_args(((FCallNode)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value));
                    yyVal = ((FCallNode)yyVals[-2+yyTop].value);
  return yyVal;
};
states[80] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].startLine());
  return yyVal;
};
states[81] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value), yyVals[yyTop - count + 3].startLine());
  return yyVal;
};
states[82] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[83] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[84] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_super(((Integer)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[85] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_yield(((Integer)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[86] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ReturnNode(((Integer)yyVals[-1+yyTop].value), support.ret_args(((Node)yyVals[0+yyTop].value), ((Integer)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[87] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new BreakNode(((Integer)yyVals[-1+yyTop].value), support.ret_args(((Node)yyVals[0+yyTop].value), ((Integer)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[88] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new NextNode(((Integer)yyVals[-1+yyTop].value), support.ret_args(((Node)yyVals[0+yyTop].value), ((Integer)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[90] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[91] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((MultipleAsgnNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[92] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((Integer)yyVals[-2+yyTop].value), support.newArrayNode(((Integer)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value)), null, null);
  return yyVal;
};
states[93] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[0+yyTop].value).getLine(), ((ListNode)yyVals[0+yyTop].value), null, null);
  return yyVal;
};
states[94] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-1+yyTop].value).getLine(), ((ListNode)yyVals[-1+yyTop].value).add(((Node)yyVals[0+yyTop].value)), null, null);
  return yyVal;
};
states[95] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-2+yyTop].value).getLine(), ((ListNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value), (ListNode) null);
  return yyVal;
};
states[96] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-4+yyTop].value).getLine(), ((ListNode)yyVals[-4+yyTop].value), ((Node)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[97] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-1+yyTop].value).getLine(), ((ListNode)yyVals[-1+yyTop].value), new StarNode(lexer.getRubySourceline()), null);
  return yyVal;
};
states[98] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), new StarNode(lexer.getRubySourceline()), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[99] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((Node)yyVals[0+yyTop].value).getLine(), null, ((Node)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[100] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((Node)yyVals[-2+yyTop].value).getLine(), null, ((Node)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[101] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = new MultipleAsgnNode(lexer.getRubySourceline(), null, new StarNode(lexer.getRubySourceline()), null);
  return yyVal;
};
states[102] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = new MultipleAsgnNode(lexer.getRubySourceline(), null, new StarNode(lexer.getRubySourceline()), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[104] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[105] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newArrayNode(((Node)yyVals[-1+yyTop].value).getLine(), ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[106] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[107] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[108] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[109] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = support.assignableLabelOrIdentifier(((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[110] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = new InstAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[111] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = new GlobalAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[112] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (lexer.getLexContext().in_def) support.compile_error("dynamic constant assignment");
                    yyVal = new ConstDeclNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), null, NilImplicitNode.NIL);
  return yyVal;
};
states[113] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ClassVarAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[114] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to nil");
                    yyVal = null;
  return yyVal;
};
states[115] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't change the value of self");
                    yyVal = null;
  return yyVal;
};
states[116] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to true");
                    yyVal = null;
  return yyVal;
};
states[117] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to false");
                    yyVal = null;
  return yyVal;
};
states[118] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to __FILE__");
                    yyVal = null;
  return yyVal;
};
states[119] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to __LINE__");
                    yyVal = null;
  return yyVal;
};
states[120] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to __ENCODING__");
                    yyVal = null;
  return yyVal;
};
states[121] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.aryset(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[122] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[123] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[124] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[125] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (lexer.getLexContext().in_def) support.yyerror("dynamic constant assignment");

                    Integer position = support.getPosition(((Node)yyVals[-2+yyTop].value));

                    yyVal = new ConstDeclNode(position, (RubySymbol) null, support.new_colon2(position, ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[126] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (lexer.getLexContext().in_def) {
                        support.yyerror("dynamic constant assignment");
                    }

                    Integer position = lexer.tokline;

                    yyVal = new ConstDeclNode(position, (RubySymbol) null, support.new_colon3(position, ((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[127] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.backrefAssignError(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[128] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.assignableLabelOrIdentifier(((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[129] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new InstAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[130] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new GlobalAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[131] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (lexer.getLexContext().in_def) support.compile_error("dynamic constant assignment");

                    yyVal = new ConstDeclNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), null, NilImplicitNode.NIL);
  return yyVal;
};
states[132] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ClassVarAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[133] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to nil");
                    yyVal = null;
  return yyVal;
};
states[134] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't change the value of self");
                    yyVal = null;
  return yyVal;
};
states[135] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to true");
                    yyVal = null;
  return yyVal;
};
states[136] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to false");
                    yyVal = null;
  return yyVal;
};
states[137] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to __FILE__");
                    yyVal = null;
  return yyVal;
};
states[138] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to __LINE__");
                    yyVal = null;
  return yyVal;
};
states[139] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to __ENCODING__");
                    yyVal = null;
  return yyVal;
};
states[140] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.aryset(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[141] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[142] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[143] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[144] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (lexer.getLexContext().in_def) {
                        support.yyerror("dynamic constant assignment");
                    }

                    Integer position = support.getPosition(((Node)yyVals[-2+yyTop].value));

                    yyVal = new ConstDeclNode(position, (RubySymbol) null, support.new_colon2(position, ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[145] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (lexer.getLexContext().in_def) {
                        support.yyerror("dynamic constant assignment");
                    }

                    Integer position = lexer.tokline;

                    yyVal = new ConstDeclNode(position, (RubySymbol) null, support.new_colon3(position, ((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[146] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.backrefAssignError(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[147] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.yyerror("class/module name must be CONSTANT", yyVals[yyTop - count + 1]);
  return yyVal;
};
states[148] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[149] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_colon3(lexer.tokline, ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[150] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_colon2(lexer.tokline, null, ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[151] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_colon2(support.getPosition(((Node)yyVals[-2+yyTop].value)), ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
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
                   lexer.setState(EXPR_ENDFN);
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[156] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[157] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal =  new LiteralNode(lexer.getRubySourceline(), support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[158] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[159] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ParserSupport.newUndef(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[160] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setState(EXPR_FNAME|EXPR_FITEM);
  return yyVal;
};
states[161] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.appendToBlock(((Node)yyVals[-3+yyTop].value), ParserSupport.newUndef(((Node)yyVals[-3+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[162] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = OR;
  return yyVal;
};
states[163] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = CARET;
  return yyVal;
};
states[164] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = AMPERSAND;
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
                     yyVal = GT;
  return yyVal;
};
states[171] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[172] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = LT;
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
                     yyVal = PLUS;
  return yyVal;
};
states[178] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = MINUS;
  return yyVal;
};
states[179] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = STAR;
  return yyVal;
};
states[180] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[181] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = SLASH;
  return yyVal;
};
states[182] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = PERCENT;
  return yyVal;
};
states[183] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[184] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[185] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = BANG;
  return yyVal;
};
states[186] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = TILDE;
  return yyVal;
};
states[187] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[188] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[189] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[190] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[191] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = BACKTICK;
  return yyVal;
};
states[192] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.__LINE__.bytes;
  return yyVal;
};
states[193] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.__FILE__.bytes;
  return yyVal;
};
states[194] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.__ENCODING__.bytes;
  return yyVal;
};
states[195] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.LBEGIN.bytes;
  return yyVal;
};
states[196] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.LEND.bytes;
  return yyVal;
};
states[197] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.ALIAS.bytes;
  return yyVal;
};
states[198] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.AND.bytes;
  return yyVal;
};
states[199] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.BEGIN.bytes;
  return yyVal;
};
states[200] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.BREAK.bytes;
  return yyVal;
};
states[201] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.CASE.bytes;
  return yyVal;
};
states[202] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.CLASS.bytes;
  return yyVal;
};
states[203] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.DEF.bytes;
  return yyVal;
};
states[204] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.DEFINED_P.bytes;
  return yyVal;
};
states[205] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.DO.bytes;
  return yyVal;
};
states[206] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.ELSE.bytes;
  return yyVal;
};
states[207] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.ELSIF.bytes;
  return yyVal;
};
states[208] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.END.bytes;
  return yyVal;
};
states[209] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.ENSURE.bytes;
  return yyVal;
};
states[210] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.FALSE.bytes;
  return yyVal;
};
states[211] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.FOR.bytes;
  return yyVal;
};
states[212] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.IN.bytes;
  return yyVal;
};
states[213] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.MODULE.bytes;
  return yyVal;
};
states[214] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.NEXT.bytes;
  return yyVal;
};
states[215] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.NIL.bytes;
  return yyVal;
};
states[216] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.NOT.bytes;
  return yyVal;
};
states[217] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.OR.bytes;
  return yyVal;
};
states[218] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.REDO.bytes;
  return yyVal;
};
states[219] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.RESCUE.bytes;
  return yyVal;
};
states[220] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.RETRY.bytes;
  return yyVal;
};
states[221] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.RETURN.bytes;
  return yyVal;
};
states[222] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.SELF.bytes;
  return yyVal;
};
states[223] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.SUPER.bytes;
  return yyVal;
};
states[224] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.THEN.bytes;
  return yyVal;
};
states[225] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.TRUE.bytes;
  return yyVal;
};
states[226] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.UNDEF.bytes;
  return yyVal;
};
states[227] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.WHEN.bytes;
  return yyVal;
};
states[228] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.YIELD.bytes;
  return yyVal;
};
states[229] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.IF.bytes;
  return yyVal;
};
states[230] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.UNLESS.bytes;
  return yyVal;
};
states[231] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.WHILE.bytes;
  return yyVal;
};
states[232] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RubyLexer.Keyword.UNTIL.bytes;
  return yyVal;
};
states[233] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = node_assign(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[234] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_op_assign(((AssignableNode)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[235] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_ary_op_assign(((Node)yyVals[-6+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[236] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value));
  return yyVal;
};
states[237] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value));
  return yyVal;
};
states[238] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value));
  return yyVal;
};
states[239] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Integer pos = support.getPosition(((Node)yyVals[-5+yyTop].value));
                    yyVal = support.new_const_op_assign(pos, support.new_colon2(pos, ((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-3+yyTop].value)), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[240] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Integer pos = lexer.getRubySourceline();
                    yyVal = support.new_const_op_assign(pos, new Colon3Node(pos, support.symbolID(((ByteList)yyVals[-3+yyTop].value))), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[241] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.backrefAssignError(((Node)yyVals[-3+yyTop].value));
  return yyVal;
};
states[242] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[-2+yyTop].value));
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
    
                    boolean isLiteral = ((Node)yyVals[-2+yyTop].value) instanceof FixnumNode && ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.makeNullNil(((Node)yyVals[-2+yyTop].value)), support.makeNullNil(((Node)yyVals[0+yyTop].value)), false, isLiteral);
  return yyVal;
};
states[243] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[-2+yyTop].value));
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));

                    boolean isLiteral = ((Node)yyVals[-2+yyTop].value) instanceof FixnumNode && ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.makeNullNil(((Node)yyVals[-2+yyTop].value)), support.makeNullNil(((Node)yyVals[0+yyTop].value)), true, isLiteral);
  return yyVal;
};
states[244] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[-1+yyTop].value));

                    boolean isLiteral = ((Node)yyVals[-1+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(support.getPosition(((Node)yyVals[-1+yyTop].value)), support.makeNullNil(((Node)yyVals[-1+yyTop].value)), NilImplicitNode.NIL, false, isLiteral);
  return yyVal;
};
states[245] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[-1+yyTop].value));

                    boolean isLiteral = ((Node)yyVals[-1+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(support.getPosition(((Node)yyVals[-1+yyTop].value)), support.makeNullNil(((Node)yyVals[-1+yyTop].value)), NilImplicitNode.NIL, true, isLiteral);
  return yyVal;
};
states[246] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].startLine(), NilImplicitNode.NIL, support.makeNullNil(((Node)yyVals[0+yyTop].value)), false, isLiteral);
  return yyVal;
};
states[247] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].startLine(), NilImplicitNode.NIL, support.makeNullNil(((Node)yyVals[0+yyTop].value)), true, isLiteral);
  return yyVal;
};
states[248] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), PLUS, ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[249] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), MINUS, ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[250] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), STAR, ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[251] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), SLASH, ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[252] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), PERCENT, ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[253] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[254] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((NumericNode)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline()), ((ByteList)yyVals[-3+yyTop].value));
  return yyVal;
};
states[255] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-1+yyTop].value));
  return yyVal;
};
states[256] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-1+yyTop].value));
  return yyVal;
};
states[257] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), OR, ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[258] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), CARET, ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[259] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), AMPERSAND, ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[260] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[261] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[262] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[263] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[264] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[265] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getMatchNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[266] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[267] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(support.method_cond(((Node)yyVals[0+yyTop].value)), BANG);
  return yyVal;
};
states[268] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop].value), TILDE);
  return yyVal;
};
states[269] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[270] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[271] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newAndNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[272] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newOrNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[273] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.getLexContext().in_defined = true;
  return yyVal;
};
states[274] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.getLexContext().in_defined = false;                    
                    yyVal = new DefinedNode(((Integer)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[275] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[-5+yyTop].value));
                    yyVal = support.new_if(support.getPosition(((Node)yyVals[-5+yyTop].value)), support.cond(((Node)yyVals[-5+yyTop].value)), ((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[276] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.endless_method_name(((DefHolder)yyVals[-3+yyTop].value));
                    support.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
                    yyVal = new DefnNode(((DefHolder)yyVals[-3+yyTop].value).line, ((DefHolder)yyVals[-3+yyTop].value).name, ((ArgsNode)yyVals[-2+yyTop].value), support.getCurrentScope(), support.reduce_nodes(support.remove_begin(((Node)yyVals[0+yyTop].value))), yyVals[yyTop - count + 4].end);
                    if (support.isNextBreak) ((DefnNode)yyVal).setContainsNextBreak();
                    support.popCurrentScope();
  return yyVal;
};
states[277] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.endless_method_name(((DefHolder)yyVals[-5+yyTop].value));
                    support.restore_defun(((DefHolder)yyVals[-5+yyTop].value));
                    Node body = support.reduce_nodes(support.remove_begin(support.rescued_expr(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value))));
                    yyVal = new DefnNode(((DefHolder)yyVals[-5+yyTop].value).line, ((DefHolder)yyVals[-5+yyTop].value).name, ((ArgsNode)yyVals[-4+yyTop].value), support.getCurrentScope(), body, yyVals[yyTop - count + 6].end);
                    if (support.isNextBreak) ((DefnNode)yyVal).setContainsNextBreak();
                    support.popCurrentScope();
  return yyVal;
};
states[278] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.endless_method_name(((DefHolder)yyVals[-3+yyTop].value));
                    support.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
                    yyVal = new DefsNode(((DefHolder)yyVals[-3+yyTop].value).line, ((DefHolder)yyVals[-3+yyTop].value).singleton, ((DefHolder)yyVals[-3+yyTop].value).name, ((ArgsNode)yyVals[-2+yyTop].value), support.getCurrentScope(), support.reduce_nodes(support.remove_begin(((Node)yyVals[0+yyTop].value))), yyVals[yyTop - count + 4].end);
                    if (support.isNextBreak) ((DefsNode)yyVal).setContainsNextBreak();
                    support.popCurrentScope();
  return yyVal;
};
states[279] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.endless_method_name(((DefHolder)yyVals[-5+yyTop].value));
                    support.restore_defun(((DefHolder)yyVals[-5+yyTop].value));
                    Node body = support.reduce_nodes(support.remove_begin(support.rescued_expr(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value))));
                    yyVal = new DefsNode(((DefHolder)yyVals[-5+yyTop].value).line, ((DefHolder)yyVals[-5+yyTop].value).singleton, ((DefHolder)yyVals[-5+yyTop].value).name, ((ArgsNode)yyVals[-4+yyTop].value), support.getCurrentScope(), body, yyVals[yyTop - count + 6].end);
                    if (support.isNextBreak) ((DefsNode)yyVal).setContainsNextBreak();                    support.popCurrentScope();
  return yyVal;
};
states[280] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[281] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = GT;
  return yyVal;
};
states[282] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = LT;
  return yyVal;
};
states[283] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[284] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[285] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[286] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     support.warning(ID.MISCELLANEOUS, lexer.getRubySourceline(), "comparison '" + ((ByteList)yyVals[-1+yyTop].value) + "' after comparison");
                     yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[287] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = (LexContext) lexer.getLexContext().clone();
  return yyVal;
};
states[288] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = (LexContext) lexer.getLexContext().clone();
  return yyVal;
};
states[289] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.makeNullNil(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[291] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[292] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.arg_append(((Node)yyVals[-3+yyTop].value), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[293] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newArrayNode(((HashNode)yyVals[-1+yyTop].value).getLine(), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[294] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[295] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[-2+yyTop].value));
                    yyVal = support.newRescueModNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[296] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[297] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (!support.check_forwarding_args()) {
                        yyVal = null;
                    } else {
                        yyVal = support.new_args_forward_call(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-3+yyTop].value));
                    }
  return yyVal;
};
states[298] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (!support.check_forwarding_args()) {
                        yyVal = null;
                    } else {
                        yyVal = support.new_args_forward_call(yyVals[yyTop - count + 1].startLine(), null);
                    }
  return yyVal;
};
states[303] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[304] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.arg_append(((Node)yyVals[-3+yyTop].value), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[305] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newArrayNode(((HashNode)yyVals[-1+yyTop].value).getLine(), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[306] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.newArrayNode(support.getPosition(((Node)yyVals[0+yyTop].value)), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[307] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = arg_blk_pass(((Node)yyVals[-1+yyTop].value), ((BlockPassNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[308] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newArrayNode(((HashNode)yyVals[-1+yyTop].value).getLine(), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
                    yyVal = arg_blk_pass(((Node)yyVal), ((BlockPassNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[309] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.arg_append(((Node)yyVals[-3+yyTop].value), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
                    yyVal = arg_blk_pass(((Node)yyVal), ((BlockPassNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[310] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  return yyVal;
};
states[311] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    boolean lookahead = false;
                    switch (yychar) {
                    case '(': case tLPAREN: case tLPAREN_ARG: case '[': case tLBRACK:
                       lookahead = true;
                    }
                    StackState cmdarg = lexer.getCmdArgumentState();
                    if (lookahead) cmdarg.pop();
                    cmdarg.push1();
                    if (lookahead) cmdarg.push0();
  return yyVal;
};
states[312] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[313] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new BlockPassNode(support.getPosition(((Node)yyVals[0+yyTop].value)), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[314] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((BlockPassNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[316] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    int line = ((Node)yyVals[0+yyTop].value) instanceof NilImplicitNode ? lexer.getRubySourceline() : ((Node)yyVals[0+yyTop].value).getLine();
                    yyVal = support.newArrayNode(line, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[317] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newSplatNode(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[318] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node node = support.splat_array(((Node)yyVals[-2+yyTop].value));

                    if (node != null) {
                        yyVal = support.list_append(node, ((Node)yyVals[0+yyTop].value));
                    } else {
                        yyVal = support.arg_append(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    }
  return yyVal;
};
states[319] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[320] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[321] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[322] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node node = support.splat_array(((Node)yyVals[-2+yyTop].value));

                    if (node != null) {
                        yyVal = support.list_append(node, ((Node)yyVals[0+yyTop].value));
                    } else {
                        yyVal = support.arg_append(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    }
  return yyVal;
};
states[323] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node node = null;

                    if (((Node)yyVals[0+yyTop].value) instanceof ArrayNode &&
                        (node = support.splat_array(((Node)yyVals[-3+yyTop].value))) != null) {
                        yyVal = support.list_concat(node, ((Node)yyVals[0+yyTop].value));
                    } else {
                        yyVal = ParserSupport.arg_concat(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    }
  return yyVal;
};
states[324] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = support.newSplatNode(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[329] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ListNode)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[330] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ListNode)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[331] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ListNode)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[332] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ListNode)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[335] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = support.new_fcall(((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[336] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.getCmdArgumentState().push0();
  return yyVal;
};
states[337] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.getCmdArgumentState().pop();
                    yyVal = new BeginNode(((Integer)yyVals[-3+yyTop].value), support.makeNullNil(((Node)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[338] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setState(EXPR_ENDARG);
  return yyVal;
};
states[339] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null; /*FIXME: Should be implicit nil?*/
  return yyVal;
};
states[340] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setState(EXPR_ENDARG); 
  return yyVal;
};
states[341] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-2+yyTop].value);
  return yyVal;
};
states[342] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (((Node)yyVals[-1+yyTop].value) != null) {
                        /* compstmt position includes both parens around it*/
                        ((Node)yyVals[-1+yyTop].value).setLine(((Integer)yyVals[-2+yyTop].value));
                        yyVal = ((Node)yyVals[-1+yyTop].value);
                    } else {
                        yyVal = new NilNode(((Integer)yyVals[-2+yyTop].value));
                    }
  return yyVal;
};
states[343] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_colon2(support.getPosition(((Node)yyVals[-2+yyTop].value)), ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[344] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_colon3(lexer.tokline, ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[345] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Integer position = support.getPosition(((Node)yyVals[-1+yyTop].value));
                    if (((Node)yyVals[-1+yyTop].value) == null) {
                        yyVal = new ZArrayNode(position); /* zero length array */
                    } else {
                        yyVal = ((Node)yyVals[-1+yyTop].value);
                    }
  return yyVal;
};
states[346] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((HashNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[347] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ReturnNode(((Integer)yyVals[0+yyTop].value), NilImplicitNode.NIL);
  return yyVal;
};
states[348] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_yield(((Integer)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[349] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new YieldNode(((Integer)yyVals[-2+yyTop].value), null);
  return yyVal;
};
states[350] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new YieldNode(((Integer)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[351] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.getLexContext().in_defined = true;
  return yyVal;
};
states[352] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.getLexContext().in_defined = false;
                    yyVal = new DefinedNode(((Integer)yyVals[-5+yyTop].value), ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[353] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(support.method_cond(((Node)yyVals[-1+yyTop].value)), lexer.BANG);
  return yyVal;
};
states[354] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.getOperatorCallNode(support.method_cond(NilImplicitNode.NIL), lexer.BANG);
  return yyVal;
};
states[355] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.frobnicate_fcall_args(((FCallNode)yyVals[-1+yyTop].value), null, ((IterNode)yyVals[0+yyTop].value));
                    yyVal = ((FCallNode)yyVals[-1+yyTop].value);                    
  return yyVal;
};
states[357] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (((Node)yyVals[-1+yyTop].value) != null && 
                          ((BlockAcceptingNode)yyVals[-1+yyTop].value).getIterNode() instanceof BlockPassNode) {
                          lexer.compile_error("Both block arg and actual block given.");
                    }
                    yyVal = ((BlockAcceptingNode)yyVals[-1+yyTop].value).setIterNode(((IterNode)yyVals[0+yyTop].value));
                    ((Node)yyVal).setLine(((Node)yyVals[-1+yyTop].value).getLine());
  return yyVal;
};
states[358] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((LambdaNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[359] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_if(((Integer)yyVals[-5+yyTop].value), support.cond(((Node)yyVals[-4+yyTop].value)), ((Node)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[360] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_if(((Integer)yyVals[-5+yyTop].value), support.cond(((Node)yyVals[-4+yyTop].value)), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[-2+yyTop].value));
  return yyVal;
};
states[361] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new WhileNode(((Integer)yyVals[-3+yyTop].value), support.cond(((Node)yyVals[-2+yyTop].value)), support.makeNullNil(((Node)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[362] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new UntilNode(((Integer)yyVals[-3+yyTop].value), support.cond(((Node)yyVals[-2+yyTop].value)), support.makeNullNil(((Node)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[363] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.case_labels;
                    support.case_labels = support.getConfiguration().getRuntime().getNil();
  return yyVal;
};
states[364] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newCaseNode(((Integer)yyVals[-5+yyTop].value), ((Node)yyVals[-4+yyTop].value), ((Node)yyVals[-1+yyTop].value));
                    support.fixpos(((Node)yyVal), ((Node)yyVals[-4+yyTop].value));
  return yyVal;
};
states[365] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.case_labels;
                    support.case_labels = null;
  return yyVal;
};
states[366] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newCaseNode(((Integer)yyVals[-4+yyTop].value), null, ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[367] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newPatternCaseNode(((Integer)yyVals[-4+yyTop].value), ((Node)yyVals[-3+yyTop].value), ((InNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[368] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ForNode(((Integer)yyVals[-5+yyTop].value), ((Node)yyVals[-4+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[-2+yyTop].value), support.getCurrentScope(), 111);
  return yyVal;
};
states[369] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = lexer.getLexContext();
                    if (ctxt.in_def) {
                        support.yyerror("class definition in method body");
                    }
                    ctxt.in_class = true;
                    support.pushLocalScope();
  return yyVal;
};
states[370] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node body = support.makeNullNil(((Node)yyVals[-1+yyTop].value));

                    yyVal = new ClassNode(yyVals[yyTop - count + 1].startLine(), ((Colon3Node)yyVals[-4+yyTop].value), support.getCurrentScope(), body, ((Node)yyVals[-3+yyTop].value), lexer.getRubySourceline());
                    LexContext ctxt = lexer.getLexContext();
                    support.popCurrentScope();
                    ctxt.in_class = ((LexContext)yyVals[-5+yyTop].value).in_class;
                    ctxt.shareable_constant_value = ((LexContext)yyVals[-5+yyTop].value).shareable_constant_value;
  return yyVal;
};
states[371] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = lexer.getLexContext();
                    ctxt.in_def = false;
                    ctxt.in_class = false;
                    support.pushLocalScope();
  return yyVal;
};
states[372] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node body = support.makeNullNil(((Node)yyVals[-1+yyTop].value));

                    yyVal = new SClassNode(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-4+yyTop].value), support.getCurrentScope(), body, lexer.getRubySourceline());
                    LexContext ctxt = lexer.getLexContext();
                    ctxt.in_def = ((LexContext)yyVals[-6+yyTop].value).in_def;
                    ctxt.in_class = ((LexContext)yyVals[-6+yyTop].value).in_class;
                    ctxt.shareable_constant_value = ((LexContext)yyVals[-6+yyTop].value).shareable_constant_value;
                    support.popCurrentScope();
  return yyVal;
};
states[373] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = lexer.getLexContext();
                    if (ctxt.in_def) { 
                        support.yyerror("module definition in method body");
                    }
                    ctxt.in_class = true;
                    support.pushLocalScope();
  return yyVal;
};
states[374] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node body = support.makeNullNil(((Node)yyVals[-1+yyTop].value));

                    yyVal = new ModuleNode(yyVals[yyTop - count + 1].startLine(), ((Colon3Node)yyVals[-3+yyTop].value), support.getCurrentScope(), body, lexer.getRubySourceline());
                    support.popCurrentScope();
                    LexContext ctxt = lexer.getLexContext();
                    ctxt.in_class = ((LexContext)yyVals[-4+yyTop].value).in_class;
                    ctxt.shareable_constant_value = ((LexContext)yyVals[-4+yyTop].value).shareable_constant_value;
  return yyVal;
};
states[375] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
                    Node body = support.reduce_nodes(support.remove_begin(support.makeNullNil(((Node)yyVals[-1+yyTop].value))));
                    yyVal = new DefnNode(((DefHolder)yyVals[-3+yyTop].value).line, ((DefHolder)yyVals[-3+yyTop].value).name, ((ArgsNode)yyVals[-2+yyTop].value), support.getCurrentScope(), body, yyVals[yyTop - count + 4].end);
                    if (support.isNextBreak) ((DefnNode)yyVal).setContainsNextBreak();                    support.popCurrentScope();
  return yyVal;
};
states[376] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
                    Node body = support.reduce_nodes(support.remove_begin(support.makeNullNil(((Node)yyVals[-1+yyTop].value))));
                    yyVal = new DefsNode(((DefHolder)yyVals[-3+yyTop].value).line, ((DefHolder)yyVals[-3+yyTop].value).singleton, ((DefHolder)yyVals[-3+yyTop].value).name, ((ArgsNode)yyVals[-2+yyTop].value), support.getCurrentScope(), body, yyVals[yyTop - count + 4].end);
                    if (support.isNextBreak) ((DefsNode)yyVal).setContainsNextBreak();
                    support.popCurrentScope();
  return yyVal;
};
states[377] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.isNextBreak = true;
                    yyVal = new BreakNode(((Integer)yyVals[0+yyTop].value), NilImplicitNode.NIL);
  return yyVal;
};
states[378] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.isNextBreak = true;
                    yyVal = new NextNode(((Integer)yyVals[0+yyTop].value), NilImplicitNode.NIL);
  return yyVal;
};
states[379] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new RedoNode(((Integer)yyVals[0+yyTop].value));
  return yyVal;
};
states[380] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new RetryNode(((Integer)yyVals[0+yyTop].value));
  return yyVal;
};
states[381] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = ((Node)yyVals[0+yyTop].value);
                    if (yyVal == null) yyVal = NilImplicitNode.NIL;
  return yyVal;
};
states[382] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[383] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[384] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[385] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[386] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[387] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[388] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[389] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = (LexContext) lexer.getLexContext().clone();
  return yyVal;
};
states[390] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = (LexContext) lexer.getLexContext().clone();  
  return yyVal;
};
states[391] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[392] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[393] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[394] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[395] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[396] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[397] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[398] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[399] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[400] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    LexContext ctxt = lexer.getLexContext();
                    if (ctxt.in_class && !ctxt.in_def && !support.getCurrentScope().isBlockScope()) {
                        lexer.compile_error("Invalid return in class/module body");
                    }
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[407] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_if(((Integer)yyVals[-4+yyTop].value), support.cond(((Node)yyVals[-3+yyTop].value)), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[409] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[411] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  return yyVal;
};
states[412] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.assignableInCurr(((ByteList)yyVals[0+yyTop].value), NilImplicitNode.NIL);
  return yyVal;
};
states[413] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[414] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[415] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[416] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[0+yyTop].value).getLine(), ((ListNode)yyVals[0+yyTop].value), null, null);
  return yyVal;
};
states[417] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-2+yyTop].value).getLine(), ((ListNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[418] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-4+yyTop].value).getLine(), ((ListNode)yyVals[-4+yyTop].value), ((Node)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[419] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(lexer.getRubySourceline(), null, ((Node)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[420] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new MultipleAsgnNode(lexer.getRubySourceline(), null, ((Node)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[421] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.assignableInCurr(((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[422] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new StarNode(lexer.getRubySourceline());
  return yyVal;
};
states[424] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = NilImplicitNode.NIL;
  return yyVal;
};
states[425] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args_tail(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[426] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args_tail(((ListNode)yyVals[-1+yyTop].value).getLine(), ((ListNode)yyVals[-1+yyTop].value), (ByteList) null, ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[427] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args_tail(lexer.getRubySourceline(), null, ((ByteList)yyVals[-1+yyTop].value), ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[428] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args_tail(((BlockArgNode)yyVals[0+yyTop].value).getLine(), null, (ByteList) null, ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[429] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[430] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args_tail(lexer.getRubySourceline(), null, (ByteList) null, null);
  return yyVal;
};
states[431] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[432] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), ((ListNode)yyVals[-5+yyTop].value), ((ListNode)yyVals[-3+yyTop].value), ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[433] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-7+yyTop].value).getLine(), ((ListNode)yyVals[-7+yyTop].value), ((ListNode)yyVals[-5+yyTop].value), ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[434] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[435] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), ((ListNode)yyVals[-5+yyTop].value), ((ListNode)yyVals[-3+yyTop].value), null, ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[436] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), null, ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[437] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    RestArgNode rest = new UnnamedRestArgNode(((ListNode)yyVals[-1+yyTop].value).getLine(), null, support.getCurrentScope().addVariable("*"));
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop].value).getLine(), ((ListNode)yyVals[-1+yyTop].value), null, rest, null, (ArgsTailHolder) null);
  return yyVal;
};
states[438] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), ((ListNode)yyVals[-5+yyTop].value), null, ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[439] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop].value).getLine(), ((ListNode)yyVals[-1+yyTop].value), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[440] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(support.getPosition(((ListNode)yyVals[-3+yyTop].value)), null, ((ListNode)yyVals[-3+yyTop].value), ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[441] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(support.getPosition(((ListNode)yyVals[-5+yyTop].value)), null, ((ListNode)yyVals[-5+yyTop].value), ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[442] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(support.getPosition(((ListNode)yyVals[-1+yyTop].value)), null, ((ListNode)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[443] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), null, ((ListNode)yyVals[-3+yyTop].value), null, ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[444] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((RestArgNode)yyVals[-1+yyTop].value).getLine(), null, null, ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[445] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((RestArgNode)yyVals[-3+yyTop].value).getLine(), null, null, ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[446] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ArgsTailHolder)yyVals[0+yyTop].value).getLine(), null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[447] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
    /* was $$ = null;*/
                    yyVal = support.new_args(lexer.getRubySourceline(), null, null, null, null, (ArgsTailHolder) null);
  return yyVal;
};
states[448] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.commandStart = true;
                    yyVal = ((ArgsNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[449] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setCurrentArg(null);
                    support.ordinalMaxNumParam();
                    yyVal = support.new_args(lexer.getRubySourceline(), null, null, null, null, (ArgsTailHolder) null);
  return yyVal;
};
states[450] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setCurrentArg(null);
                    support.ordinalMaxNumParam();
                    yyVal = ((ArgsNode)yyVals[-2+yyTop].value);
  return yyVal;
};
states[451] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[452] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[453] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[454] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[455] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.new_bv(((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[456] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[457] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.pushBlockScope();
                    yyVal = lexer.getLeftParenBegin();
                    lexer.setLeftParenBegin(lexer.getParenNest());
  return yyVal;
};
states[458] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.resetMaxNumParam();
  return yyVal;
};
states[459] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.numparam_push();
  return yyVal;
};
states[460] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.getCmdArgumentState().push0();
  return yyVal;
};
states[461] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    int max_numparam = support.restoreMaxNumParam(((Integer)yyVals[-4+yyTop].value));
                    ArgsNode args = support.args_with_numbered(((ArgsNode)yyVals[-2+yyTop].value), max_numparam);
                    lexer.getCmdArgumentState().pop();
                    yyVal = new LambdaNode(yyVals[yyTop - count + 1].startLine(), args, ((Node)yyVals[0+yyTop].value), support.getCurrentScope(), lexer.getRubySourceline());
                    lexer.setLeftParenBegin(((Integer)yyVals[-5+yyTop].value));
                    support.numparam_pop(((Node)yyVals[-3+yyTop].value));
                    support.popCurrentScope();
  return yyVal;
};
states[462] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ArgsNode)yyVals[-2+yyTop].value);
                    support.ordinalMaxNumParam();
  return yyVal;
};
states[463] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (!support.isArgsInfoEmpty(((ArgsNode)yyVals[0+yyTop].value))) {
                        support.ordinalMaxNumParam();
                    }
                    yyVal = ((ArgsNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[464] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[465] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[466] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IterNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[467] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[468] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].startLine());
  return yyVal;
};
states[469] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value), yyVals[yyTop - count + 3].startLine());
  return yyVal;
};
states[470] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value), yyVals[yyTop - count + 3].startLine());
  return yyVal;
};
states[471] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.frobnicate_fcall_args(((FCallNode)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
                    yyVal = ((FCallNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[472] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].startLine());
  return yyVal;
};
states[473] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[474] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value), null, null);
  return yyVal;
};
states[475] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), LexingCommon.CALL, ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].startLine());
  return yyVal;
};
states[476] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop].value), LexingCommon.CALL, ((Node)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[477] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_super(((Integer)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[478] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ZSuperNode(((Integer)yyVals[0+yyTop].value));
  return yyVal;
};
states[479] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (((Node)yyVals[-3+yyTop].value) instanceof SelfNode) {
                        yyVal = support.new_fcall(LexingCommon.LBRACKET_RBRACKET);
                        support.frobnicate_fcall_args(((FCallNode)yyVal), ((Node)yyVals[-1+yyTop].value), null);
                    } else {
                        yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), lexer.LBRACKET_RBRACKET, ((Node)yyVals[-1+yyTop].value), null);
                    }
  return yyVal;
};
states[480] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IterNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[481] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((IterNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[482] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.pushBlockScope();
  return yyVal;
};
states[483] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.resetMaxNumParam();
  return yyVal;
};
states[484] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.numparam_push();
  return yyVal;
};
states[485] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    int max_numparam = support.restoreMaxNumParam(((Integer)yyVals[-3+yyTop].value));
                    ArgsNode args = support.args_with_numbered(((ArgsNode)yyVals[-1+yyTop].value), max_numparam);
                    yyVal = new IterNode(yyVals[yyTop - count + 1].startLine(), args, ((Node)yyVals[0+yyTop].value), support.getCurrentScope(), lexer.getRubySourceline());
                    support.numparam_pop(((Node)yyVals[-2+yyTop].value));
                    support.popCurrentScope();
  return yyVal;
};
states[486] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.pushBlockScope();
  return yyVal;
};
states[487] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.resetMaxNumParam();
  return yyVal;
};
states[488] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.numparam_push();
                    lexer.getCmdArgumentState().push0();
  return yyVal;
};
states[489] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    int max_numparam = support.restoreMaxNumParam(((Integer)yyVals[-3+yyTop].value));
                    ArgsNode args = support.args_with_numbered(((ArgsNode)yyVals[-1+yyTop].value), max_numparam);
                    yyVal = new IterNode(yyVals[yyTop - count + 1].startLine(), args, ((Node)yyVals[0+yyTop].value), support.getCurrentScope(), lexer.getRubySourceline());
                    lexer.getCmdArgumentState().pop();
                    support.numparam_pop(((Node)yyVals[-2+yyTop].value));
                    support.popCurrentScope();
  return yyVal;
};
states[490] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     support.check_literal_when(((Node)yyVals[0+yyTop].value));
                     yyVal = support.newArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[491] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newSplatNode(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[492] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.check_literal_when(((Node)yyVals[0+yyTop].value));
                    yyVal = support.last_arg_append(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[493] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.rest_arg_append(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[494] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newWhenNode(((Integer)yyVals[-4+yyTop].value), ((Node)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[497] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setState(EXPR_BEG|EXPR_LABEL);
                    lexer.commandStart = false;
                    LexContext ctxt = (LexContext) lexer.getLexContext();
                    yyVals[0+yyTop].value = ctxt.in_kwarg;
                    ctxt.in_kwarg = true;
                    yyVal = support.push_pvtbl();
  return yyVal;
};
states[498] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.push_pktbl();
  return yyVal;
};
states[499] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    support.pop_pvtbl(((Set)yyVals[-3+yyTop].value));
                    lexer.getLexContext().in_kwarg = ((Boolean)yyVals[-4+yyTop].value);
  return yyVal;
};
states[500] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newIn(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-4+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[502] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((InNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[504] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_if(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[0+yyTop].value), support.remove_begin(((Node)yyVals[-2+yyTop].value)), null);
                    support.fixpos(((Node)yyVal), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[505] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_if(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[0+yyTop].value), null, support.remove_begin(((Node)yyVals[-2+yyTop].value)));
                    support.fixpos(((Node)yyVal), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[507] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), null, ((Node)yyVals[-1+yyTop].value),
                                                   support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), null, true, null, null));
  return yyVal;
};
states[508] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), null, ((Node)yyVals[-2+yyTop].value), ((ArrayPatternNode)yyVals[0+yyTop].value));
                   support.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].startLine());
  return yyVal;
};
states[509] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_find_pattern(null, ((FindPatternNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[510] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), null, null, ((ArrayPatternNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[511] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_hash_pattern(null, ((HashPatternNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[513] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new HashNode(yyVals[yyTop - count + 1].startLine(), new KeyValuePair(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[515] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newOrNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[517] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.push_pktbl();
  return yyVal;
};
states[518] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.push_pktbl();
  return yyVal;
};
states[520] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-3+yyTop].value), null, ((ArrayPatternNode)yyVals[-1+yyTop].value));
                    support.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].startLine());
  return yyVal;
};
states[521] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     support.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                     yyVal = support.new_find_pattern(((Node)yyVals[-3+yyTop].value), ((FindPatternNode)yyVals[-1+yyTop].value));
                     support.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].startLine());
  return yyVal;
};
states[522] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     support.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                     yyVal = support.new_hash_pattern(((Node)yyVals[-3+yyTop].value), ((HashPatternNode)yyVals[-1+yyTop].value));
                     support.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].startLine());
  return yyVal;
};
states[523] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-2+yyTop].value), null,
                                                    support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), null, false, null, null));
  return yyVal;
};
states[524] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     support.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                     yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-3+yyTop].value), null, ((ArrayPatternNode)yyVals[-1+yyTop].value));
                     support.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].startLine());
  return yyVal;
};
states[525] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = support.new_find_pattern(((Node)yyVals[-3+yyTop].value), ((FindPatternNode)yyVals[-1+yyTop].value));
                    support.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].startLine());
  return yyVal;
};
states[526] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = support.new_hash_pattern(((Node)yyVals[-3+yyTop].value), ((HashPatternNode)yyVals[-1+yyTop].value));
                    support.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].startLine());
  return yyVal;
};
states[527] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-2+yyTop].value), null,
                            support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), null, false, null, null));
  return yyVal;
};
states[528] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), null, null, ((ArrayPatternNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[529] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_find_pattern(null, ((FindPatternNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[530] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), null, null,
                            support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), null, false, null, null));
  return yyVal;
};
states[531] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.push_pktbl();
                    LexContext ctxt = lexer.getLexContext();
                    yyVals[0+yyTop].value = ctxt.in_kwarg;
                    ctxt.in_kwarg = false;
  return yyVal;
};
states[532] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    lexer.getLexContext().in_kwarg = ((Boolean)yyVals[-3+yyTop].value);
                    yyVal = support.new_hash_pattern(null, ((HashPatternNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[533] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_hash_pattern(null, support.new_hash_pattern_tail(yyVals[yyTop - count + 1].startLine(), null, null));
  return yyVal;
};
states[534] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.push_pktbl();
  return yyVal;
};
states[535] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[536] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     ListNode preArgs = support.newArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), preArgs, false, null, null);
  return yyVal;
};
states[537] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((ListNode)yyVals[0+yyTop].value), true, null, null);
  return yyVal;
};
states[538] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), support.list_concat(((ListNode)yyVals[-1+yyTop].value), ((ListNode)yyVals[0+yyTop].value)), false, null, null);
  return yyVal;
};
states[539] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((ListNode)yyVals[-2+yyTop].value), true, ((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[540] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((ListNode)yyVals[-4+yyTop].value), true, ((ByteList)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[541] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((ListNode)yyVals[-1+yyTop].value), true, null, null);
  return yyVal;
};
states[542] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((ListNode)yyVals[-3+yyTop].value), true, null, ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[543] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ArrayPatternNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[544] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ListNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[545] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = support.list_concat(((ListNode)yyVals[-2+yyTop].value), ((ListNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[546] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), null, true, ((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[547] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), null, true, ((ByteList)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[548] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = support.new_find_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((ByteList)yyVals[-4+yyTop].value), ((ListNode)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
                     support.warn_experimental(yyVals[yyTop - count + 1].startLine(), "Find pattern is experimental, and the behavior may change in future versions of Ruby!");
  return yyVal;
};
states[549] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[550] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[552] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.list_concat(((ListNode)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[553] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[554] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_hash_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((HashNode)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[555] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_hash_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((HashNode)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[556] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_hash_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((HashNode)yyVals[-1+yyTop].value), null);
  return yyVal;
};
states[557] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_hash_pattern_tail(yyVals[yyTop - count + 1].startLine(), null, ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[558] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new HashNode(yyVals[yyTop - count + 1].start, ((KeyValuePair)yyVals[0+yyTop].value));
  return yyVal;
};
states[559] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    ((HashNode)yyVals[-2+yyTop].value).add(((KeyValuePair)yyVals[0+yyTop].value));
                    yyVal = ((HashNode)yyVals[-2+yyTop].value);
  return yyVal;
};
states[560] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.error_duplicate_pattern_key(((ByteList)yyVals[-1+yyTop].value));

                    Node label = support.asSymbol(yyVals[yyTop - count + 1].startLine(), ((ByteList)yyVals[-1+yyTop].value));

                    yyVal = new KeyValuePair(label, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[561] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.error_duplicate_pattern_key(((ByteList)yyVals[0+yyTop].value));
                    if (((ByteList)yyVals[0+yyTop].value) != null && !support.is_local_id(((ByteList)yyVals[0+yyTop].value))) {
                        support.yyerror("key must be valid as local variables");
                    }
                    support.error_duplicate_pattern_variable(((ByteList)yyVals[0+yyTop].value));

                    Node label = support.asSymbol(yyVals[yyTop - count + 1].startLine(), ((ByteList)yyVals[0+yyTop].value));
                    yyVal = new KeyValuePair(label, support.assignableLabelOrIdentifier(((ByteList)yyVals[0+yyTop].value), null));
  return yyVal;
};
states[563] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (((Node)yyVals[-1+yyTop].value) == null || ((Node)yyVals[-1+yyTop].value) instanceof StrNode) {
                        yyVal = ((StrNode)yyVals[-1+yyTop].value).getValue();
                    } else {
                        support.yyerror("symbol literal with interpolation is not allowed");
                        yyVal = null;
                    }
  return yyVal;
};
states[564] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[565] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[566] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[568] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.KWNOREST;
  return yyVal;
};
states[570] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[-2+yyTop].value));
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[-2+yyTop].value) instanceof FixnumNode && ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].startLine(), support.makeNullNil(((Node)yyVals[-2+yyTop].value)), support.makeNullNil(((Node)yyVals[0+yyTop].value)), false, isLiteral);
  return yyVal;
};
states[571] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[-2+yyTop].value));
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[-2+yyTop].value) instanceof FixnumNode && ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].startLine(), support.makeNullNil(((Node)yyVals[-2+yyTop].value)), support.makeNullNil(((Node)yyVals[0+yyTop].value)), true, isLiteral);
  return yyVal;
};
states[572] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[-1+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[-1+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].startLine(), support.makeNullNil(((Node)yyVals[-1+yyTop].value)), NilImplicitNode.NIL, false, isLiteral);
  return yyVal;
};
states[573] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[-1+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[-1+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].startLine(), support.makeNullNil(((Node)yyVals[-1+yyTop].value)), NilImplicitNode.NIL, true, isLiteral);
  return yyVal;
};
states[577] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].startLine(), NilImplicitNode.NIL, support.makeNullNil(((Node)yyVals[0+yyTop].value)), false, isLiteral);
  return yyVal;
};
states[578] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].startLine(), NilImplicitNode.NIL, support.makeNullNil(((Node)yyVals[0+yyTop].value)), true, isLiteral);
  return yyVal;
};
states[583] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[584] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[585] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[586] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[587] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new NilNode(lexer.tokline);
  return yyVal;
};
states[588] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new SelfNode(lexer.tokline);
  return yyVal;
};
states[589] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new TrueNode(lexer.tokline);
  return yyVal;
};
states[590] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new FalseNode(lexer.tokline);
  return yyVal;
};
states[591] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new FileNode(lexer.tokline, new ByteList(lexer.getFile().getBytes(),
                    support.getConfiguration().getRuntime().getEncodingService().getLocaleEncoding()));
  return yyVal;
};
states[592] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new FixnumNode(lexer.tokline, lexer.tokline+1);
  return yyVal;
};
states[593] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new EncodingNode(lexer.tokline, lexer.getEncoding());
  return yyVal;
};
states[594] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((LambdaNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[595] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.error_duplicate_pattern_variable(((ByteList)yyVals[0+yyTop].value));
                    yyVal = support.assignableInCurr(((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[596] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node n = support.declareIdentifier(((ByteList)yyVals[0+yyTop].value));
                    if (!(n instanceof LocalVarNode || n instanceof DVarNode)) {
                        support.compile_error("" + ((ByteList)yyVals[0+yyTop].value) + ": no such local variable");
                    }
                    yyVal = n;
  return yyVal;
};
states[597] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_colon3(lexer.tokline, ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[598] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_colon2(lexer.tokline, ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[599] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ConstNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[600] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[601] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null; 
  return yyVal;
};
states[602] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[603] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.splat_array(((Node)yyVals[0+yyTop].value));
                    if (yyVal == null) yyVal = ((Node)yyVals[0+yyTop].value); /* ArgsCat or ArgsPush*/
  return yyVal;
};
states[605] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[607] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[609] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((NumericNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[610] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[611] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[612] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((StrNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[613] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[614] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[615] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.heredoc_dedent(((Node)yyVals[-1+yyTop].value));
		    lexer.setHeredocIndent(0);
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[616] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[617] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.newRegexpNode(support.getPosition(((Node)yyVals[-1+yyTop].value)), ((Node)yyVals[-1+yyTop].value), (RegexpNode) ((RegexpNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[618] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[619] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = new ArrayNode(lexer.getRubySourceline());
  return yyVal;
};
states[620] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[-1+yyTop].value) instanceof EvStrNode ? new DStrNode(((ListNode)yyVals[-2+yyTop].value).getLine(), lexer.getEncoding()).add(((Node)yyVals[-1+yyTop].value)) : ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[621] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[622] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = support.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[623] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[624] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ArrayNode(lexer.getRubySourceline());
  return yyVal;
};
states[625] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[-1+yyTop].value) instanceof EvStrNode ? new DSymbolNode(((ListNode)yyVals[-2+yyTop].value).getLine()).add(((Node)yyVals[-1+yyTop].value)) : support.asSymbol(((ListNode)yyVals[-2+yyTop].value).getLine(), ((Node)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[626] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[627] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[628] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ArrayNode(lexer.getRubySourceline());
  return yyVal;
};
states[629] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[630] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ArrayNode(lexer.getRubySourceline());
  return yyVal;
};
states[631] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(support.asSymbol(((ListNode)yyVals[-2+yyTop].value).getLine(), ((Node)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[632] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    ByteList aChar = ByteList.create("");
                    aChar.setEncoding(lexer.getEncoding());
                    yyVal = lexer.createStr(aChar, 0);
  return yyVal;
};
states[633] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[634] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    ByteList aChar = ByteList.create("");
                    aChar.setEncoding(lexer.getEncoding());
                    yyVal = lexer.createStr(aChar, 0);
  return yyVal;
};
states[635] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[636] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[637] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
    /* FIXME: mri is different here.*/
                    yyVal = support.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[638] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[639] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = lexer.getStrTerm();
                    lexer.setStrTerm(null);
                    lexer.setState(EXPR_BEG);
  return yyVal;
};
states[640] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setStrTerm(((StrTerm)yyVals[-1+yyTop].value));
                    yyVal = new EvStrNode(support.getPosition(((Node)yyVals[0+yyTop].value)), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[641] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = lexer.getStrTerm();
                   lexer.setStrTerm(null);
                   lexer.getConditionState().push0();
                   lexer.getCmdArgumentState().push0();
  return yyVal;
};
states[642] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = lexer.getState();
                   lexer.setState(EXPR_BEG);
  return yyVal;
};
states[643] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = lexer.getBraceNest();
                   lexer.setBraceNest(0);
  return yyVal;
};
states[644] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = lexer.getHeredocIndent();
                   lexer.setHeredocIndent(0);
  return yyVal;
};
states[645] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[646] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = new GlobalVarNode(lexer.getRubySourceline(), support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[647] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = new InstVarNode(lexer.getRubySourceline(), support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[648] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = new ClassVarNode(lexer.getRubySourceline(), support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[652] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     lexer.setState(EXPR_END);
                     yyVal = support.asSymbol(lexer.getRubySourceline(), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[654] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[655] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[656] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[657] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[658] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((NumericNode)yyVals[0+yyTop].value);  
  return yyVal;
};
states[659] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = support.negateNumeric(((NumericNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[660] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[661] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((FloatNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[662] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((RationalNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[663] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                     yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[664] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.declareIdentifier(((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[665] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new InstVarNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[666] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new GlobalVarNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[667] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ConstNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[668] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ClassVarNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[669] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new NilNode(lexer.tokline);
  return yyVal;
};
states[670] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new SelfNode(lexer.tokline);
  return yyVal;
};
states[671] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new TrueNode(lexer.tokline);
  return yyVal;
};
states[672] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new FalseNode(lexer.tokline);
  return yyVal;
};
states[673] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new FileNode(lexer.tokline, new ByteList(lexer.getFile().getBytes(),
                    support.getConfiguration().getRuntime().getEncodingService().getLocaleEncoding()));
  return yyVal;
};
states[674] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new FixnumNode(lexer.tokline, lexer.tokline+1);
  return yyVal;
};
states[675] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new EncodingNode(lexer.tokline, lexer.getEncoding());
  return yyVal;
};
states[676] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.assignableLabelOrIdentifier(((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[677] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new InstAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[678] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new GlobalAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[679] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (lexer.getLexContext().in_def) support.compile_error("dynamic constant assignment");

                    yyVal = new ConstDeclNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), null, NilImplicitNode.NIL);
  return yyVal;
};
states[680] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ClassVarAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[681] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to nil");
                    yyVal = null;
  return yyVal;
};
states[682] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't change the value of self");
                    yyVal = null;
  return yyVal;
};
states[683] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to true");
                    yyVal = null;
  return yyVal;
};
states[684] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to false");
                    yyVal = null;
  return yyVal;
};
states[685] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to __FILE__");
                    yyVal = null;
  return yyVal;
};
states[686] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to __LINE__");
                    yyVal = null;
  return yyVal;
};
states[687] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.compile_error("Can't assign to __ENCODING__");
                    yyVal = null;
  return yyVal;
};
states[688] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[689] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[690] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   lexer.setState(EXPR_BEG);
                   lexer.commandStart = true;
  return yyVal;
};
states[691] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[692] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = null;
  return yyVal;
};
states[694] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[695] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ArgsNode)yyVals[-1+yyTop].value);
                    lexer.setState(EXPR_BEG);
                    lexer.commandStart = true;
  return yyVal;
};
states[696] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.add_forwarding_args();
                    yyVal = support.new_args_forward_def(yyVals[yyTop - count + 1].startLine(), ((ListNode)yyVals[-3+yyTop].value));
                    lexer.setState(EXPR_BEG);
                    lexer.commandStart = true;

  return yyVal;
};
states[697] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.add_forwarding_args();
                    yyVal = support.new_args_forward_def(yyVals[yyTop - count + 1].startLine(), null);
                    lexer.setState(EXPR_BEG);
                    lexer.commandStart = true;
  return yyVal;
};
states[698] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   yyVal = ((ArgsNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[699] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                   LexContext ctxt = lexer.getLexContext();
                   yyVal = ctxt.in_kwarg;
                   ctxt.in_kwarg = true;
                   lexer.setState(lexer.getState() | EXPR_LABEL);
  return yyVal;
};
states[700] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.getLexContext().in_kwarg = ((Boolean)yyVals[-2+yyTop].value);
                    yyVal = ((ArgsNode)yyVals[-1+yyTop].value);
                    lexer.setState(EXPR_BEG);
                    lexer.commandStart = true;
  return yyVal;
};
states[701] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args_tail(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[702] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args_tail(((ListNode)yyVals[-1+yyTop].value).getLine(), ((ListNode)yyVals[-1+yyTop].value), (ByteList) null, ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[703] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args_tail(lexer.getRubySourceline(), null, ((ByteList)yyVals[-1+yyTop].value), ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[704] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args_tail(((BlockArgNode)yyVals[0+yyTop].value).getLine(), null, (ByteList) null, ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[705] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[706] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args_tail(lexer.getRubySourceline(), null, (ByteList) null, null);
  return yyVal;
};
states[707] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), ((ListNode)yyVals[-5+yyTop].value), ((ListNode)yyVals[-3+yyTop].value), ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[708] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-7+yyTop].value).getLine(), ((ListNode)yyVals[-7+yyTop].value), ((ListNode)yyVals[-5+yyTop].value), ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[709] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[710] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), ((ListNode)yyVals[-5+yyTop].value), ((ListNode)yyVals[-3+yyTop].value), null, ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[711] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), null, ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[712] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), ((ListNode)yyVals[-5+yyTop].value), null, ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[713] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop].value).getLine(), ((ListNode)yyVals[-1+yyTop].value), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[714] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), null, ((ListNode)yyVals[-3+yyTop].value), ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[715] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), null, ((ListNode)yyVals[-5+yyTop].value), ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[716] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop].value).getLine(), null, ((ListNode)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[717] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), null, ((ListNode)yyVals[-3+yyTop].value), null, ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[718] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((RestArgNode)yyVals[-1+yyTop].value).getLine(), null, null, ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[719] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((RestArgNode)yyVals[-3+yyTop].value).getLine(), null, null, ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[720] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(((ArgsTailHolder)yyVals[0+yyTop].value).getLine(), null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[721] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.new_args(lexer.getRubySourceline(), null, null, null, null, (ArgsTailHolder) null);
  return yyVal;
};
states[722] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[723] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.yyerror("formal argument cannot be a constant");
  return yyVal;
};
states[724] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.yyerror("formal argument cannot be an instance variable");
  return yyVal;
};
states[725] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.yyerror("formal argument cannot be a global variable");
  return yyVal;
};
states[726] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.yyerror("formal argument cannot be a class variable");
  return yyVal;
};
states[727] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value); /* Not really reached*/
  return yyVal;
};
states[728] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.formal_argument(((ByteList)yyVals[0+yyTop].value));
                    support.ordinalMaxNumParam();
  return yyVal;
};
states[729] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setCurrentArg(((ByteList)yyVals[0+yyTop].value));
                    yyVal = support.arg_var(((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[730] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setCurrentArg(null);
                    yyVal = ((ArgumentNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[731] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[732] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ArrayNode(lexer.getRubySourceline(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[733] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[0+yyTop].value));
                    yyVal = ((ListNode)yyVals[-2+yyTop].value);
  return yyVal;
};
states[734] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.arg_var(support.formal_argument(((ByteList)yyVals[0+yyTop].value)));
                    lexer.setCurrentArg(((ByteList)yyVals[0+yyTop].value));
                    support.ordinalMaxNumParam();
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[735] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setCurrentArg(null);
                    yyVal = new KeywordArgNode(((Node)yyVals[0+yyTop].value).getLine(), support.assignableKeyword(((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[736] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setCurrentArg(null);
                    yyVal = new KeywordArgNode(lexer.getRubySourceline(), support.assignableKeyword(((ByteList)yyVals[0+yyTop].value), new RequiredKeywordArgumentValueNode()));
  return yyVal;
};
states[737] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new KeywordArgNode(support.getPosition(((Node)yyVals[0+yyTop].value)), support.assignableKeyword(((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[738] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new KeywordArgNode(lexer.getRubySourceline(), support.assignableKeyword(((ByteList)yyVals[0+yyTop].value), new RequiredKeywordArgumentValueNode()));
  return yyVal;
};
states[739] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[740] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[741] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new ArrayNode(((KeywordArgNode)yyVals[0+yyTop].value).getLine(), ((KeywordArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[742] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((KeywordArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[743] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[744] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[745] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[746] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.shadowing_lvar(((ByteList)yyVals[0+yyTop].value));
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[747] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.INTERNAL_ID;
  return yyVal;
};
states[748] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setCurrentArg(null);
                    yyVal = new OptArgNode(support.getPosition(((Node)yyVals[0+yyTop].value)), support.assignableLabelOrIdentifier(((ArgumentNode)yyVals[-2+yyTop].value).getName().getBytes(), ((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[749] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setCurrentArg(null);
                    yyVal = new OptArgNode(support.getPosition(((Node)yyVals[0+yyTop].value)), support.assignableLabelOrIdentifier(((ArgumentNode)yyVals[-2+yyTop].value).getName().getBytes(), ((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[750] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new BlockNode(((Node)yyVals[0+yyTop].value).getLine()).add(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[751] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[752] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new BlockNode(((Node)yyVals[0+yyTop].value).getLine()).add(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[753] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[754] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = STAR;
  return yyVal;
};
states[755] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[756] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (!support.is_local_id(((ByteList)yyVals[0+yyTop].value))) {
                        support.yyerror("rest argument must be local variable");
                    }
                    
                    yyVal = new RestArgNode(support.arg_var(support.shadowing_lvar(((ByteList)yyVals[0+yyTop].value))));
  return yyVal;
};
states[757] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
  /* FIXME: bytelist_love: somewhat silly to remake the empty bytelist over and over but this type should change (using null vs "" is a strange distinction).*/
  yyVal = new UnnamedRestArgNode(lexer.getRubySourceline(), support.symbolID(CommonByteLists.EMPTY), support.getCurrentScope().addVariable("*"));
  return yyVal;
};
states[758] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = AMPERSAND;
  return yyVal;
};
states[759] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[760] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (!support.is_local_id(((ByteList)yyVals[0+yyTop].value))) {
                        support.yyerror("block argument must be local variable");
                    }
                    
                    yyVal = new BlockArgNode(support.arg_var(support.shadowing_lvar(((ByteList)yyVals[0+yyTop].value))));
  return yyVal;
};
states[761] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((BlockArgNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[762] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = null;
  return yyVal;
};
states[763] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[764] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    lexer.setState(EXPR_BEG);
  return yyVal;
};
states[765] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    if (((Node)yyVals[-1+yyTop].value) == null) {
                        support.yyerror("can't define single method for ().");
                    } else if (((Node)yyVals[-1+yyTop].value) instanceof ILiteralNode) {
                        support.yyerror("can't define single method for literals.");
                    }
                    support.value_expr(lexer, ((Node)yyVals[-1+yyTop].value));
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[766] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new HashNode(lexer.getRubySourceline());
  return yyVal;
};
states[767] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[768] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = new HashNode(lexer.getRubySourceline(), ((KeyValuePair)yyVals[0+yyTop].value));
  return yyVal;
};
states[769] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((HashNode)yyVals[-2+yyTop].value).add(((KeyValuePair)yyVals[0+yyTop].value));
  return yyVal;
};
states[770] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.createKeyValue(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[771] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    Node label = support.asSymbol(support.getPosition(((Node)yyVals[0+yyTop].value)), ((ByteList)yyVals[-1+yyTop].value));
                    yyVal = support.createKeyValue(label, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[772] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
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
states[773] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = support.createKeyValue(null, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[774] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[775] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[776] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[777] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[778] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[779] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[780] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[781] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[782] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[783] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[784] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = DOT;
  return yyVal;
};
states[785] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[786] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = DOT;
  return yyVal;
};
states[787] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[789] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[794] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RPAREN;
  return yyVal;
};
states[795] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RBRACKET;
  return yyVal;
};
states[796] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                    yyVal = RCURLY;
  return yyVal;
};
states[804] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                      yyVal = null;
  return yyVal;
};
states[805] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {
                  yyVal = null;
  return yyVal;
};
}
					// line 3489 "RubyParser.y"

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
					// line 12990 "-"
