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
					// line 190 "-"
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
//yyLhs 804
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
   234,   235,    41,    41,    41,    41,    41,    41,    41,    41,
    41,    41,   236,    41,    41,    41,    41,    41,    41,    41,
    41,    41,    41,    41,   238,    41,   239,    41,    41,    41,
   240,    41,   242,    41,   243,    41,    41,    41,    41,    41,
    41,    41,    48,   195,   196,   209,   205,   206,   208,   207,
   191,   192,   203,   197,   198,   199,   200,   194,   193,   201,
   204,   190,   237,   237,   237,   227,   227,    51,    51,    52,
    52,   102,   102,    92,    92,    93,    93,    94,    94,    94,
    94,    94,    95,    95,   180,   180,    67,    67,    67,    67,
    68,    68,   182,   103,   103,   103,   103,   103,   103,   103,
   103,   103,   103,   103,   103,   103,   103,   103,   104,   104,
   105,   105,   112,   112,   111,   111,   113,   113,   244,   245,
   114,   115,   115,   116,   116,   121,    81,    81,    81,    81,
    44,    44,    44,    44,    44,    44,    44,    44,    44,   119,
   119,   246,   247,   117,   248,   249,   118,    54,    54,    54,
    54,    53,    55,    55,   250,   251,   252,   132,   133,   133,
   134,   134,   134,   135,   135,   135,   135,   135,   135,   136,
   137,   137,   138,   138,   210,   211,   139,   139,   139,   139,
   139,   139,   139,   139,   139,   139,   139,   139,   253,   139,
   139,   254,   139,   141,   141,   141,   141,   141,   141,   141,
   141,   142,   142,   143,   143,   140,   174,   174,   144,   144,
   145,   151,   151,   151,   151,   152,   152,   153,   153,   178,
   178,   175,   175,   176,   177,   177,   146,   146,   146,   146,
   146,   146,   146,   146,   146,   146,   147,   147,   147,   147,
   147,   147,   147,   147,   147,   147,   147,   147,   147,   147,
   147,   147,   148,   149,   150,   150,   150,    56,    56,    57,
    57,    57,    58,    58,    59,    59,    20,    20,     2,     3,
     3,     3,     4,     5,     6,    11,    16,    16,    19,    19,
    12,    13,    13,    14,    15,    17,    17,    18,    18,     7,
     7,     8,     8,     9,     9,    10,   255,    10,   256,   257,
   258,   259,   260,    10,   101,   101,   101,   101,    25,    25,
    23,   154,   154,   154,   154,    24,    21,    21,    22,    22,
    22,    22,    73,    73,    73,    73,    73,    73,    73,    73,
    73,    73,    73,    73,    74,    74,    74,    74,    74,    74,
    74,    74,    74,    74,    74,    74,   100,   100,   261,    80,
    80,    86,    86,    87,    87,    87,    85,   262,    85,    65,
    65,    65,    65,    66,    66,    88,    88,    88,    88,    88,
    88,    88,    88,    88,    88,    88,    88,    88,    88,    88,
   181,   165,   165,   165,   165,   164,   164,   168,    90,    90,
    89,    89,   167,   108,   108,   110,   110,   109,   109,   107,
   107,   186,   186,   179,   166,   166,   106,    84,    83,    83,
    91,    91,   185,   185,   161,   161,   184,   184,   162,   163,
   163,     1,   263,     1,    96,    96,    97,    97,    98,    98,
    98,    98,   155,   155,   155,   156,   156,   156,   156,   157,
   157,   157,   173,   173,   169,   169,   170,   170,   213,   213,
   218,   218,   187,   188,   202,   230,   230,   230,   241,   241,
   214,   214,   123,   189,
    }, yyLen = {
//yyLen 804
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
     0,     0,     5,     3,     3,     2,     3,     3,     1,     4,
     3,     1,     0,     6,     4,     3,     2,     1,     2,     1,
     6,     6,     4,     4,     0,     6,     0,     5,     5,     6,
     0,     6,     0,     7,     0,     5,     4,     4,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     2,     1,     1,     1,     5,     1,
     2,     1,     1,     1,     3,     1,     3,     1,     3,     5,
     1,     3,     2,     1,     1,     1,     4,     2,     2,     1,
     2,     0,     1,     6,     8,     4,     6,     4,     2,     6,
     2,     4,     6,     2,     4,     2,     4,     1,     1,     1,
     3,     4,     1,     4,     1,     3,     1,     1,     0,     0,
     5,     4,     1,     3,     3,     3,     2,     4,     5,     5,
     2,     4,     4,     3,     3,     3,     2,     1,     4,     3,
     3,     0,     0,     4,     0,     0,     4,     1,     2,     3,
     4,     5,     1,     1,     0,     0,     0,     8,     1,     1,
     1,     3,     3,     1,     2,     3,     1,     1,     1,     1,
     3,     1,     3,     1,     1,     1,     1,     4,     4,     4,
     3,     4,     4,     4,     3,     3,     3,     2,     0,     4,
     2,     0,     4,     1,     1,     2,     3,     5,     2,     4,
     1,     2,     3,     1,     3,     5,     2,     1,     1,     3,
     1,     3,     1,     2,     1,     1,     3,     2,     1,     1,
     3,     2,     1,     2,     1,     1,     1,     3,     3,     2,
     2,     1,     1,     1,     2,     2,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     2,     2,     3,     1,     6,     1,     1,
     1,     1,     2,     1,     2,     1,     1,     1,     1,     1,
     1,     2,     3,     3,     3,     4,     0,     3,     1,     2,
     4,     0,     3,     4,     4,     0,     3,     0,     3,     0,
     2,     0,     2,     0,     2,     1,     0,     3,     0,     0,
     0,     0,     0,     8,     1,     1,     1,     1,     1,     1,
     2,     1,     1,     1,     1,     3,     1,     2,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     0,     4,
     0,     1,     1,     3,     5,     3,     1,     0,     3,     4,
     2,     2,     1,     2,     0,     6,     8,     4,     6,     4,
     6,     2,     4,     6,     2,     4,     2,     4,     1,     0,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     3,
     1,     3,     1,     2,     1,     2,     1,     1,     3,     1,
     3,     1,     1,     2,     2,     1,     3,     3,     1,     3,
     1,     3,     1,     1,     2,     1,     1,     1,     2,     2,
     0,     1,     0,     4,     1,     2,     1,     3,     3,     2,
     4,     2,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     0,     1,
     0,     1,     2,     2,     2,     0,     1,     1,     1,     1,
     1,     2,     0,     0,
    }, yyDefRed = {
//yyDefRed 1342
     1,     0,     0,     0,   390,   391,   392,     0,   383,   384,
   385,   388,   386,   387,   389,     0,     0,   380,   381,   401,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   658,   659,   660,   661,   609,   686,   687,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   458,     0,
   629,   631,   633,     0,     0,     0,     0,     0,     0,   326,
     0,   610,   327,   328,   329,   331,   330,   332,   325,   606,
   656,   648,   649,   607,     0,     0,     2,     0,     5,     0,
     0,     0,     0,     0,    51,     0,     0,     0,     0,   333,
     0,    34,     0,    73,     0,   359,     0,     4,     0,     0,
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
     0,     0,     0,     0,     0,   629,     0,     0,   306,     0,
     0,     0,    87,   310,     0,     0,   766,     0,     0,    88,
     0,    85,     0,     0,   476,    84,     0,   791,     0,     0,
    22,     0,     0,     9,     0,     0,   378,   379,     0,     0,
   255,     0,     0,   348,     0,     0,     0,     0,     0,    20,
     0,     0,     0,    16,     0,    15,     0,     0,     0,     0,
     0,     0,     0,   290,     0,     0,     0,   764,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   382,     0,     0,     0,
     0,   653,   652,   654,     0,   650,   651,     0,     0,     0,
   616,   625,   621,   627,   267,    55,   268,   611,     0,     0,
     0,     0,   692,     0,     0,     0,   798,   799,     3,     0,
   800,     0,     0,     0,     0,     0,     0,     0,    60,     0,
     0,     0,     0,     0,   283,   284,     0,     0,     0,     0,
     0,     0,     0,     0,    56,     0,   281,   282,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   394,   466,   484,
   393,   481,   358,   484,   785,     0,     0,   784,     0,   470,
     0,   356,     0,     0,   787,   786,     0,     0,     0,     0,
     0,     0,     0,   105,    86,   668,   667,   669,   670,   672,
   671,   673,     0,   664,   663,     0,   666,     0,     0,     0,
     0,   334,   150,   374,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   762,     0,    66,   761,
    65,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   411,   412,     0,   366,     0,     0,   160,   769,     0,   317,
   771,   313,     0,     0,     0,     0,     0,     0,   307,   315,
     0,     0,   308,     0,     0,     0,   350,     0,   312,     0,
     0,   302,     0,     0,   301,     0,     0,   355,    54,    24,
    26,    25,     0,   352,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   343,    14,     0,     0,   339,     0,
     0,   796,   291,   346,     0,   293,   347,   765,     0,   657,
     0,   107,     0,   696,     0,     0,     0,     0,   726,   723,
   722,   721,   724,   732,   741,     0,   753,   742,   757,   756,
   752,     0,   718,   462,     0,   730,     0,   750,     0,   739,
   459,     0,   702,   727,   725,   424,     0,     0,   425,     0,
     0,     0,     0,   635,   655,   638,   636,   630,   612,   613,
   632,   614,   634,     0,     0,     0,     0,   720,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   801,     6,    28,
    29,    30,    31,    32,    52,    53,    61,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
    57,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   485,     0,   482,     0,     0,     0,     0,   475,
     0,   473,     0,     0,     0,     0,   777,     0,   474,     0,
   778,   481,    79,     0,   287,   288,     0,   775,   776,     0,
     0,     0,     0,     0,     0,     0,   106,     0,   147,     0,
   149,   688,   370,     0,     0,     0,     0,     0,   403,     0,
     0,     0,   783,   782,    67,     0,     0,     0,     0,     0,
     0,     0,    70,     0,     0,     0,     0,     0,   768,     0,
     0,     0,     0,     0,     0,     0,   314,     0,     0,   767,
     0,     0,   349,   792,     0,   296,     0,   298,   354,    23,
     0,     0,    10,    33,     0,     0,     0,     0,    21,     0,
    17,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   415,     0,     0,     0,   413,     0,     0,   711,
     0,   714,     0,   700,     0,     0,   716,   733,     0,     0,
   701,   758,   754,   743,   744,   639,     0,   615,   618,     0,
     0,   623,   620,     0,     0,   624,   693,     0,   695,   400,
   376,   395,     0,   598,     0,     0,   698,   377,     0,     0,
     0,     0,   465,     0,   479,     0,   480,     0,     0,   472,
     0,     0,     0,     0,     0,     0,   300,   471,     0,   299,
     0,     0,     0,     0,    41,   234,    50,     0,     0,     0,
     0,    47,   241,     0,     0,   316,     0,    40,   233,    36,
    35,     0,   320,     0,   104,     0,     0,     0,     0,     0,
     0,     0,   151,     0,     0,   337,     0,   404,     0,     0,
   362,   406,    71,   405,   363,     0,     0,     0,     0,     0,
     0,   494,     0,     0,   397,     0,     0,     0,   161,   770,
     0,     0,     0,     0,     0,   319,   309,     0,     0,     0,
   240,   342,   292,   108,     0,     0,   422,     0,   729,     0,
     0,     0,   703,   731,     0,     0,     0,   751,     0,   740,
   759,     0,     0,     0,     0,   460,     0,   728,   746,   640,
   644,   645,   646,   647,   637,   617,   619,   626,   622,   628,
     0,   396,   398,    13,   605,    11,     0,     0,     0,   600,
   601,     0,     0,   585,   584,   586,   587,   589,   588,   590,
   592,   596,     0,     0,     0,   531,     0,     0,     0,   577,
   578,   579,   580,   582,   581,   583,   576,   591,     0,   509,
     0,   513,   516,     0,   571,   572,     0,     0,     0,     0,
     0,   449,   448,     0,     0,     0,    83,     0,   793,     0,
     0,    81,    76,     0,     0,     0,     0,     0,     0,   468,
   469,     0,     0,     0,     0,     0,     0,     0,   478,   375,
   399,     0,   407,   409,     0,     0,   763,    68,     0,     0,
   495,   368,     0,   367,     0,   487,     0,     0,     0,     0,
     0,     0,     0,     0,   297,   353,   414,   416,     0,     0,
   461,     0,     0,   707,     0,   709,     0,   715,     0,   712,
   699,     0,     0,   717,   641,   694,     0,   604,     0,     0,
   603,     0,     0,   574,   575,   148,   594,     0,     0,     0,
     0,     0,     0,   540,     0,     0,   527,   530,     0,     0,
   593,     0,    63,     0,     0,     0,     0,     0,     0,     0,
     0,    59,     0,   447,     0,   748,     0,     0,     0,   737,
     0,     0,   429,     0,     0,     0,   486,   483,    46,   238,
    45,   239,     0,    43,   236,    44,   237,     0,    49,     0,
     0,     0,     0,     0,     0,     0,     0,    37,     0,   689,
   371,   360,   410,     0,   369,     0,   365,   488,     0,     0,
   361,     0,     0,     0,   456,     0,   454,   457,     0,     0,
     0,     0,   464,   463,   642,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   602,     0,     0,   546,   526,   525,     0,     0,     0,
   541,     0,   794,   559,   629,     0,     0,   555,   564,   565,
   554,     0,     0,   510,   512,   567,   568,   595,   524,   520,
   629,     0,     0,     0,     0,     0,     0,     0,   443,     0,
   440,   438,     0,     0,   427,   450,     0,   445,     0,     0,
     0,     0,     0,   428,    42,   235,     0,     0,   373,     0,
     0,     0,     0,   506,   507,   508,     0,   489,     0,     0,
     0,   453,   708,     0,   705,   710,   713,     0,    12,     0,
     0,     0,     0,     0,   532,     0,     0,   542,     0,   548,
     0,   529,     0,     0,   563,   561,     0,   518,   517,   519,
   522,   521,   523,   430,   749,     0,     0,     0,     0,   451,
   738,     0,     0,   345,     0,     0,     0,   496,     0,     0,
     0,   490,   492,   493,   491,   455,     0,     0,     0,     0,
     0,     0,     0,   597,     0,     0,     0,   560,   556,   551,
     0,   444,     0,   441,     0,   435,     0,   437,   426,   446,
     0,     0,   408,     0,   501,   502,   505,     0,   706,   643,
     0,     0,     0,   549,   545,     0,     0,     0,     0,     0,
     0,   442,   436,     0,   433,   439,   498,   499,   497,     0,
     0,   434,
    }, yyDgoto = {
//yyDgoto 264
     1,   437,    69,    70,    71,    72,    73,   314,   318,   319,
   577,    74,    75,   585,    76,    77,   583,   584,   586,   769,
    78,    79,    80,    81,    82,    83,   419,   438,    84,    85,
    86,    87,    88,   253,   591,   592,   272,   273,   274,    90,
    91,    92,    93,    94,    95,   426,   441,   229,   261,   262,
    98,  1001,  1002,   865,  1016,  1284,   782,   928,  1049,   923,
   643,   491,   492,   639,   807,   892,   749,  1263,  1198,   241,
   281,   478,   233,    99,   234,   827,   828,   101,   829,   833,
   672,   102,   103,  1084,  1085,   329,   330,   331,   553,   554,
   555,   556,   742,   743,   744,   745,   285,   493,   236,   201,
   237,   914,   459,  1087,   970,   971,   557,   558,   559,  1088,
  1089,  1135,   890,  1136,   105,   560,   905,   633,   631,   391,
   652,   378,   238,   275,   202,   108,   109,   110,   111,   112,
   532,   277,   862,  1338,  1220,  1221,  1168,   959,   960,   961,
  1060,  1061,  1062,  1063,  1248,  1064,   962,   963,   964,   965,
   966,  1175,  1176,  1177,   315,   113,   725,   641,   422,   642,
   204,   561,   562,   753,   563,   564,   565,   566,   907,   675,
   396,   205,   376,   684,  1065,  1178,  1179,  1180,  1181,   568,
   569,   495,  1201,   656,   570,   571,   572,   486,   802,   479,
   263,   115,   116,  1004,   866,   117,   118,   383,   379,   784,
   926,  1005,  1067,   119,   780,   120,   121,   122,   123,   124,
  1079,  1080,     2,   338,   464,  1046,   512,   502,   487,   620,
   790,  1081,   606,   789,  1072,   849,   442,   852,   696,   504,
   522,   242,   424,   278,   279,   731,   720,   679,   863,   694,
   839,   680,   837,   676,   310,   754,   634,   795,   632,   793,
  1010,  1125,  1313,  1069,  1057,   766,   765,   909,  1044,  1144,
  1237,   838,   333,   681,
    }, yySindex = {
//yySindex 1342
     0,     0, 22451, 23054,     0,     0,     0, 26037,     0,     0,
     0,     0,     0,     0,     0, 20809, 20809,     0,     0,     0,
   117,   124,     0,     0,     0,     0,   512, 25934,   189,    89,
   100,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  1783, 25030, 25030,
 25030, 25030,  -125, 22563,     0, 23771, 24111, 26627,     0, 26457,
     0,     0,     0,   227,   284,   287,   303, 25144, 25030,     0,
    22,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   368,   368,     0,   114,     0,  1699,
   763, 21539,     0,   123,     0,    53,    67,   575,   205,     0,
   110,     0,    84,     0,   119,     0,   423,     0,   511, 28534,
     0,   544,     0,     0, 20809, 28645, 28867,     0, 25259, 26357,
     0,     0, 28756, 22222, 25259,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   588,     0,     0,     0,     0,     0,     0,     0,     0,
   600,     0,     0,     0,     0,     0,     0,     0,     0, 25030,
   336, 22681, 25030, 25030, 25030,     0, 25030, 27712,     0,   310,
   202,   662,     0,     0,   369,   687,     0,   425,   723,     0,
 22111,     0, 20809, 23165,     0,     0, 22334,     0, 25259,   577,
     0,   760, 22451,     0, 22681,   485,     0,     0,   117,   124,
     0,   331,   575,     0,   497,  8724,  8724,   500, 23277,     0,
 22563,   792,   114,     0,  1699,     0,     0,   189,   189, 23277,
   137,   795,   428,     0,   310,   810,   428,     0,     0,     0,
     0,     0,   189,     0,     0,     0,     0,     0,     0,     0,
     0,  1783,   628, 28978,   368,   368,     0,   374,     0,   898,
  5890,     0,     0,     0,  1221,     0,     0,  1247,  1334,   922,
     0,     0,     0,     0,     0,     0,     0,     0,  5880, 22681,
   893,     0,     0,  6399, 22681,   938,     0,     0,     0, 22824,
     0, 25259, 25259, 25259, 25259, 23277, 25259, 25259,     0, 25030,
 25030, 25030, 25030, 25030,     0,     0, 25030, 25030, 25030, 25030,
 25030, 25030, 25030, 25030,     0, 25030,     0,     0, 25030, 25030,
 25030, 25030, 25030, 25030, 25030, 25030, 25030,     0,     0,     0,
     0,     0,     0,     0,     0, 26959, 20809,     0, 27202,     0,
   605,     0, 25030,   680,     0,     0, 28370,   680,   680,   680,
 22563, 26864,   968,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0, 25259,   333,   960,
   429,     0,     0,     0, 22681,   763,    79,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     5,     0,     0,
     0, 22681, 25259, 22681,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   726,   547,     0,   765,
     0,     0,   114,     0,   987,    79,     0,     0,   500,     0,
     0,     0,  1417, 25030, 27280, 20809, 27319, 23413,     0,     0,
   680, 23882,     0,   680,   680,   189,     0,   998,     0, 25030,
  1006,     0,   189,  1014,     0,   189,    49,     0,     0,     0,
     0,     0, 26037,     0, 25030,   940,   946, 25030, 27280, 27319,
   680,  1699,    89,   189,     0,     0, 22942,     0,     0,  1699,
 23993,     0,     0,     0, 24111,     0,     0,     0,   760,     0,
     0,     0,  1030,     0, 27358, 20809, 27397, 28978,     0,     0,
     0,     0,     0,     0,     0,  2159,     0,     0,     0,     0,
     0,  6399,     0,     0,  1066,     0,  1081,     0,  1085,     0,
     0,  1090,     0,     0,     0,     0, 25030,  1094,     0,  1102,
   858,   866,   -79,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,  1399,  -243,  1423,  -219,     0,   189,  1135,
   189,   921,   932, 25030,   114,   921, 25030,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   497,  6889,  6889,
  6889,  6889, 18192,  9229,  6889,  6889,  8724,  8724,   837,   837,
     0, 20701,   798,   798,   918,   770,   770,   497,   497,   497,
  1780,   921,     0,  1075,     0,   921,   869,     0,   871,     0,
   124,     0,     0,  1161,   189,   875,     0,   879,     0,   124,
     0,     0,     0,  1780,     0,     0, 25144,     0,     0,   124,
 25144, 24224, 24224,   189, 28978,  1191,     0,   763,     0,     0,
     0,     0,     0, 27457, 20809, 27777, 22681,   921,     0, 22681,
   996, 25259,     0,     0,     0,   921,    93,   921,     0, 27816,
 20809, 27855,     0,   969,  1000, 22681, 26037, 25030,     0,   909,
   919,   189,   925,   928, 25030,   310,     0,   687, 25030,     0,
 25030, 25030,     0,     0, 23524,     0, 23882,     0,     0,     0,
 25259, 27712,     0,     0,   497,   124,   124, 25030,     0,     0,
     0,   189,   428, 28978,     0,     0,   189,     0,     0,  1030,
  2159,  1293,     0,  1225,   189,  1234,     0,   189,  6399,     0,
  6399,     0,   209,     0,  -175,  2927,     0,     0, 25030,     9,
     0,     0,     0,     0,     0,     0,  1229,     0,     0,    29,
  1261,     0,     0,   187,  1266,     0,     0,  5880,     0,     0,
     0,     0,  -157,     0, 24340, 21842,     0,     0, 28405, 26226,
 26226,  1241,     0,  1193,     0,  1193,     0,   680,   680,     0,
   605, 23413,   970,  1227,   680,   680,     0,     0,   605,     0,
  1201, 28446,  1027,   574,     0,     0,     0,   119,  1271,    53,
   123,     0,     0, 25030, 28446,     0,  1292,     0,     0,     0,
     0,     0,     0,  1040,     0,  1030, 28978,   114, 25259, 22681,
     0,     0,     0,   189,   921,     0,   913,     0,    49, 26772,
     0,     0,     0,     0,     0,     0,     0,   189,     0,     0,
 22681,     0,   921,  1000,     0,   921, 24454,  1073,     0,     0,
   680,   680,  1001,   680,   680,     0,     0,  1300,   189,    49,
     0,     0,     0,     0,     0,   189,     0,  2159,     0,  2049,
  1316,  1295,     0,     0,  1332,  1340,  1341,     0,  1348,     0,
     0,  1102,  1054, 22681, 22681,     0,  1341,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   189,     0,     0,     0,     0,     0, 22681,     0,  1049,     0,
     0, 25030, 25030,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  3911,  3911,   510,     0, 13270,   189,  1097,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  1056,     0,
  1282,     0,     0,   873,     0,     0,    -3,  1056, 25030,  1810,
 22681,     0,     0, 22681, 25144, 25144,     0,   680,     0, 25144,
 25144,     0,     0, 25030, 23277, 27894, 20809, 27933,   680,     0,
     0,     0, 24570, 23277,  1030, 22681,   114,   921,     0,     0,
     0,   921,     0,     0, 22681, 25259,     0,     0,     0,   921,
     0,     0,   921,     0, 25030,     0,   570,   921, 25030, 25030,
   680, 25030, 25030, 23882,     0,     0,     0,     0,  1367,  1379,
     0,  1755,  6399,     0,  2927,     0,  2927,     0,  2927,     0,
     0,   921,  1301,     0,     0,     0, 22681,     0, 29089,    79,
     0, 27712, 27712,     0,     0,     0,     0, 26226,  1121,  1056,
   189,   189, 16441,     0,  1388,  1390,     0,     0,  1312,   990,
     0,  1138,     0, 26226,  3911,  3911,   510,   189,   189, 28229,
 28229,     0, 27712,     0,  1396,     0,  1402,   189,  1407,     0,
  1319,  1409,     0, 29200,  1398,  1102,     0,     0,     0,     0,
     0,     0, 25144,     0,     0,     0,     0, 27712,     0,   869,
   871,   189,   875,   879, 25144, 25030,     0,     0,   921,     0,
     0,     0,     0,    79,     0, 28229,     0,     0, 24684, 22681,
     0, 25030,  2049,  2049,     0,   617,     0,     0,  1341,  1421,
  1341,  1341,     0,     0,     0,  1198,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  1156,   578,
     0,     0, 22681,    31,     0,     0,     0,    32,  1056,  1430,
     0, 26226,     0,     0,     0,   189,  1433,     0,     0,     0,
     0, 26226,   263,     0,     0,     0,     0,     0,     0,     0,
     0,   189,   189,   189,   189,   189,   189,  6399,     0,  6399,
     0,     0,  1356,   209,     0,     0,  2927,     0,     0,     0,
  1174,   612, 29200,     0,     0,     0,   970,     0,     0, 22681,
    79,   955,   -13,     0,     0,     0, 25030,     0,   476,  1379,
  1755,     0,     0,  2927,     0,     0,     0, 22681,     0,     0,
 27972, 20809, 28292,   932,     0,  1456, 26226,     0,  1472,     0,
  1025,     0,   990,  1056,     0,     0,  1443,     0,     0,     0,
     0,     0,     0,     0,     0,  1473,  1475,  1477,  1480,     0,
     0,  1102,  1473,     0, 28331,   612,   913,     0, 25259, 25259,
 16851,     0,     0,     0,     0,     0,  1341,  1157,     0,     0,
   189,     0,     0,     0, 26226,  1487, 16851,     0,     0,     0,
  2927,     0,  2927,     0,  6399,     0,  2927,     0,     0,     0,
     0,     0,     0, 22681,     0,     0,     0,  1489,     0,     0,
     0,  1487, 26226,     0,     0,  1473,  1473,  1492,  1473,   766,
 26226,     0,     0,  2927,     0,     0,     0,     0,     0,  1487,
  1473,     0,
    }, yyRindex = {
//yyRindex 1342
     0,     0,   804,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0, 16724, 17038,     0,     0,     0,
 13867, 13390, 17631, 17723, 18036, 18126, 25373,     0, 24801,     0,
     0, 18218, 18531, 18621, 14232,  4246, 18713, 19026, 14344, 19116,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   832, 21999,  1449,  1364,   121,     0,  1515,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  7738,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,  1445,  1445,     0,   120,     0,   852,
  6433, 12500,  8052, 12309,     0,  8147,     0, 23635, 10074,     0,
     0,     0, 19422,     0, 19208,     0,     0,     0,     0,   177,
     0,     0,     0,     0, 17133,     0,     0,     0,     0,     0,
     0,     0,     0,  1272,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  6841,     0,     0,     0,     0,  5517,  5612,  5710,  6024,
     0,  6119,  6217,  6531,  3165,  6626,  6724,  3282,  7038,     0,
     0,   832,     0,     0,     0,     0,     0, 12995,     0,  8461,
     0,  9374,     0,     0,     0,  9374,     0,  7545,     0,     0,
  1503,     0,     0,   487,     0,     0,  1503,     0,     0,     0,
     0, 25488,    90,     0,    90,  8559,     0,     0,  8245,  7133,
     0,     0,     0,     0,  9064, 12269, 12365, 19518,     0,     0,
   832,     0,  2239,     0,  1099,     0,   980,  1503,  1503,     0,
  1453,     0,  1453,     0,     0,     0,  1425,     0,  2526,  2660,
  3075,  3808,  1511,  4287,  4769,  5227,  1432,  5246,  5623,  1979,
  5650,     0,     0,     0,  1799,  1799,     0,     0,  5683,   246,
  -168,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   548,  1476,
     0, 21365,     0,   280,  1476,     0,     0,     0,     0,   143,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  8654,  8750,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,    74,     0,     0,     0,
  3120,     0,     0, 25602,     0,     0,     0, 25602, 24914, 24914,
   832,   700,   769,     0,     0,     0,     0,     0,     0,     0,
     0,     0, 20100,     0,     0, 20212,     0,     0,     0, 21477,
     0,     0,     0,     0,  1476,  5827,     0,  1963,  2625,  5275,
 10828, 12584, 16322, 16796, 19998, 20288,     0,     0,     0,     0,
     0,   236,     0,   236,   515,  1169,  1587,  1629,  2146,  2600,
  2639,  1068,  2704,  4104,  1920,  4290,     0,     0,  4772,     0,
     0,     0,  -172,     0,   308,     0,     0,     0,  7640,     0,
     0,     0,     0,     0,     0,    74,     0,     0,     0,     0,
 25602,     0,     0, 25602, 25602,  1503,     0,     0,     0,   625,
   767,     0,  1503,   413,     0,  1503,  1503,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
 25602,  1294,     0,  1503,     0,     0,  5471,    34,     0,   891,
  1466,     0,     0,     0,   506,     0,     0,     0,     0,     0,
  5823,     0,   777,     0,     0,    74,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  1092,     0,     0,   169,     0,   169,     0,   259,     0,
     0,   169,     0,     0,     0,     0,   134,   365,     0,   259,
     0,   170,   566,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  1503,   935,
  1503,     0,  1124,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  9159, 11457, 11553,
 11664, 11760, 11855, 12158, 11967, 12062, 12460, 12558, 10566, 10661,
     0,  1502, 11006, 11102, 10757, 10169, 10265,  9255,  9569,  9664,
 11197,     0,     0,     0,     0,     0, 14709,  4611, 16140,     0,
 23635,     0,  4728,   141,  1468, 14821,     0, 15186,     0, 13755,
     0,     0,     0, 11362,     0,     0,     0,     0,     0, 16617,
     0,     0,     0,  1503,     0,   895,     0,   378,     0, 20966,
     0,     0,     0,     0,    74,     0,  1476,     0,     0,  1060,
 21078,     0,     0,     0,     0,     0,     0,     0,  4821,     0,
    74,     0,     0,  1304,     0,   634,     0,     0,     0,  3647,
  5093,  1468,  3764,  4129,     0,  8870,     0,  9374,     0,     0,
     0,     0,     0,     0,   954,     0,   907,     0,     0,     0,
     0, 10385,     0,     0,  9760,     0,  7231,     0,     0,  1125,
     0,  1503,  1453,     0,  2171,  2387,  1468,  5134,  5641,   986,
     0,   660,     0,  1019,  1503,  1042,     0,   709,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  1302,     0,    38, 13089,     0,     0, 13125,     0,
     0,     0,     0, 21686,     0, 21798,     0, 25602, 25602,     0,
 10020,   150, 17231,     0, 25602, 25602,     0,     0, 10908,     0,
     0, 13272,  3042,     0,     0,     0,     0, 19608,     0,  9879,
 19784,     0,     0,     0, 19914,     0,     0,     0,     0,     0,
     0,  6335,     0,  7348,     0,  1077,     0,     0,     0,  1476,
 20544, 20656,     0,  1468,     0,     0,  1302,     0,  1503,     0,
     0,     0,     0,     0,     0,  2529,   288,  1468,  2985,  3622,
   236,     0,     0,     0,     0,     0,     0,  1302,     0,     0,
 25602, 25602,  5203, 25602, 25602,     0,     0,   413,  1503,  1503,
     0,     0,     0,     0,  1609,  1503,     0,     0,     0,     0,
     0,   129,     0,     0,   169,   169,   169,     0,   169,     0,
     0,   259,   566,  1476,    90,     0,   169,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  1503,     0,     0,     0,     0,     0,   236,   106,   212,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,  1468,   627,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0, 19824,     0,
 19715,     0,     0,  2800,     0,     0,  9924, 19874,     0,    62,
  1476,     0,     0,    90,     0,     0,     0, 25602,     0,     0,
     0,     0,     0,     0,     0,     0,    74,     0, 25602,     0,
     0,  2999,     0,     0,  1095,  1476,     0,     0,     0,     0,
     0,     0,     0,     0,   236,     0,     0,     0,  2557,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
 25602,     0,     0,   939,     0,     0,     0,     0,  1063,  1188,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   643,     0,     0,     0,
     0, 12654, 12691,     0,     0,     0,     0,     0,  1883,  5210,
  1468,  1468,  5736,     0,     0,  5957,     0,     0,     0,     0,
     0,     0,     0,     0, 17447, 19473,     0, 25751, 21164,     0,
     0,     0, 12789,     0,   380,     0,   380,    62,   727,     0,
     0,   380,     0,   746,  1041,   727,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0, 12957,     0, 15298,
 16252,  1468, 15663, 15775,     0,     0,  9419,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   857,
     0,     0,     0,     0,     0,   488,     0,     0,   169,   169,
   169,   169,     0,     0,     0,  1302,   554,   887,  1260,  1313,
  1429,  1435,  1439,  1242,  1636,  1800,  1575,  1996,     0,     0,
  2237,     0,  1476,  1503,     0,     0,     0,  6164,  6126,  6208,
     0,     0,     0,     0,     0,  1447,  1020,     0,     0,     0,
     0,  1958,  3240,     0,     0,     0,     0,     0,     0,     0,
     0,  1503,  1503,  1503,  1468,  1468,  1468,     0,     0,   975,
     0,     0,     0,     0,     0,     0,     0,     0,  1372,  2909,
     0,  1446,     0,     0,     0,     0, 17541, 17349,     0,  1060,
     0,   271,   552,     0,     0,     0,     0,     0,  1302,  1197,
     0,     0,     0,     0,     0,     0,     0,    60,     0,  2436,
     0,    74,     0,  1124,     0,  6224,     0,     0,  6240,     0,
     0,     0,  5606,  3722,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   380,   380,   380,   380,     0,
     0,   727,   380,     0,     0,  1459,  1302,     0,     0,     0,
   676,     0,     0,     0,     0,     0,   169,     0,  1598,   149,
  1468,  1939,  2727,     0,     0,  6243,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  3739,   843,     0,   799,     0,     0,     0,  1134,     0,     0,
  2539,  6731,     0,     0,     0,   380,   380,   380,   380,  1302,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  1163,
   380,     0,
    }, yyGindex = {
//yyGindex 264
     0,     0,   635,     0,  1494,  1259,  1517,   -59,     0,     0,
  -117,  1620,  1667,     0,  1682,  1697,     0,     0,     0,   994,
  1832,     0,    -5,     0,     0,    25,  1455,   745,    68,    82,
  1343,     0,    54,  1086,  -261,    21,     0,  1091,    47,  -111,
  2172,    91,    40,   -65,     0,   -32,  -107,  1330,    18,   102,
     0,   334,  -852,  -839,     0,     0,   377,     0,     0,   480,
   269,  1137,  -382,    14,   973,   415,  -312,   666,  3415,     1,
     0,  -190,  -387,  1510,  1397,  -358,  1694,    35,  -590,     0,
     0,     0,     0,   437, -1083,    17,  1552,   981,   421,   158,
  -719,  -345,  -808,  -779,   899,   761,     0,    12,  -415,     0,
  1168,     0,     0,     0,   865,     0,  -663,     0,   911,     0,
   465,     0,  -798,   439,  1847,     0,     0,  1021,  1288,   -77,
   -85,   854,  1200,    -2,   -11,  1555,     0,     7,   -66,   -46,
  -409,  -208,   350,     0,     0,     0,  -291,     0,     0,   607,
  -248,  -817,     0,   556,  -776,  1053,     0,   -67,   614,     0,
     0,   -63,     0,   431,     0,     0,   175,     0,  -395,    37,
   -50,  -712,  -535,  -564,  -373,  -951,  -746,  -105,  -247,   -56,
     0,     0,  1589,     0, -1135,     0,     0,   436,     0,     0,
    69,  -303,     0,  1113,     0,     0,  -629,   845,   553,     0,
   966,     0,     0,   908,     0,     0,     0,     0,     0,     0,
     0,     0,   516,     0,   897,     0,     0,     0,     0,     0,
     0,     0,     0,  -104,    42,     0,     0,     0,   -18,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  -256,     0,     0,     0,     0,     0,     0,  -448,     0,     0,
     0,   -84,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,
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
    "$$19 :",
    "primary : tLPAREN_ARG $$18 stmt $$19 rparen",
    "primary : tLPAREN compstmt ')'",
    "primary : primary_value tCOLON2 tCONSTANT",
    "primary : tCOLON3 tCONSTANT",
    "primary : tLBRACK aref_args ']'",
    "primary : tLBRACE assoc_list '}'",
    "primary : k_return",
    "primary : keyword_yield '(' call_args rparen",
    "primary : keyword_yield '(' rparen",
    "primary : keyword_yield",
    "$$20 :",
    "primary : keyword_defined opt_nl '(' $$20 expr rparen",
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
    "$$21 :",
    "primary : k_case expr_value opt_terms $$21 case_body k_end",
    "$$22 :",
    "primary : k_case opt_terms $$22 case_body k_end",
    "primary : k_case expr_value opt_terms p_case_body k_end",
    "primary : k_for for_var keyword_in expr_value_do compstmt k_end",
    "$$23 :",
    "primary : k_class cpath superclass $$23 bodystmt k_end",
    "$$24 :",
    "primary : k_class tLSHFT expr $$24 term bodystmt k_end",
    "$$25 :",
    "primary : k_module cpath $$25 bodystmt k_end",
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
    "$$26 :",
    "$$27 :",
    "lambda : tLAMBDA $$26 f_larglist $$27 lambda_body",
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
    "$$28 :",
    "$$29 :",
    "brace_body : $$28 $$29 opt_block_param compstmt",
    "$$30 :",
    "$$31 :",
    "do_body : $$30 $$31 opt_block_param bodystmt",
    "case_args : arg_value",
    "case_args : tSTAR arg_value",
    "case_args : case_args ',' arg_value",
    "case_args : case_args ',' tSTAR arg_value",
    "case_body : k_when case_args then compstmt cases",
    "cases : opt_else",
    "cases : case_body",
    "$$32 :",
    "$$33 :",
    "$$34 :",
    "p_case_body : keyword_in $$32 $$33 p_top_expr then $$34 compstmt p_cases",
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
    "$$35 :",
    "p_expr_basic : tLBRACE $$35 p_kwargs rbrace",
    "p_expr_basic : tLBRACE rbrace",
    "$$36 :",
    "p_expr_basic : tLPAREN $$36 p_expr rparen",
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
    "$$37 :",
    "string_content : tSTRING_DVAR $$37 string_dvar",
    "$$38 :",
    "$$39 :",
    "$$40 :",
    "$$41 :",
    "$$42 :",
    "string_content : tSTRING_DBEG $$38 $$39 $$40 $$41 $$42 compstmt tSTRING_DEND",
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
    "$$43 :",
    "superclass : '<' $$43 expr_value term",
    "superclass :",
    "f_opt_paren_args : f_paren_args",
    "f_opt_paren_args : none",
    "f_paren_args : '(' f_args rparen",
    "f_paren_args : '(' f_arg ',' args_forward rparen",
    "f_paren_args : '(' args_forward rparen",
    "f_arglist : f_paren_args",
    "$$44 :",
    "f_arglist : $$44 f_args term",
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
    "$$45 :",
    "singleton : '(' $$45 expr rparen",
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
            yyVal = parserState.execute(support, lexer, yyVal, yystates, yytop, count);
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

static ParserState[] states = new ParserState[804];
static {
states[1] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                  lexer.setState(EXPR_BEG);
                  support.initTopLocalVariables();
  return yyVal;
};
states[2] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
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
states[3] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                  yyVal = support.void_stmts(((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[5] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                  yyVal = support.newline_node(((Node)yyVals[0+yyTop].value), support.getPosition(((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[6] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                  yyVal = support.appendToBlock(((Node)yyVals[-2+yyTop].value), support.newline_node(((Node)yyVals[0+yyTop].value), support.getPosition(((Node)yyVals[0+yyTop].value))));
  return yyVal;
};
states[7] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                  yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[9] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                  yyVal = null;
  return yyVal;
};
states[10] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                  support.getResult().addBeginNode(new PreExe19Node(yyVals[yyTop - count + 1].startLine(), support.getCurrentScope(), ((Node)yyVals[-1+yyTop].value), lexer.getRubySourceline()));
                  /*                  $$ = new BeginNode(yyVals[yyTop - count + 1].startLine(), support.makeNullNil($2));*/
                  yyVal = null;
  return yyVal;
};
states[11] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   if (((RescueBodyNode)yyVals[-1+yyTop].value) == null) support.yyerror("else without rescue is useless"); 
  return yyVal;
};
states[12] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = support.new_bodystmt(((Node)yyVals[-5+yyTop].value), ((RescueBodyNode)yyVals[-4+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[13] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = support.new_bodystmt(((Node)yyVals[-2+yyTop].value), ((RescueBodyNode)yyVals[-1+yyTop].value), null, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[14] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.void_stmts(((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[16] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newline_node(((Node)yyVals[0+yyTop].value), support.getPosition(((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[17] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.appendToBlock(((Node)yyVals[-2+yyTop].value), support.newline_node(((Node)yyVals[0+yyTop].value), support.getPosition(((Node)yyVals[0+yyTop].value))));
  return yyVal;
};
states[18] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[19] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[20] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   support.yyerror("BEGIN is permitted only at toplevel");
  return yyVal;
};
states[21] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[22] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setState(EXPR_FNAME|EXPR_FITEM);
  return yyVal;
};
states[23] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ParserSupport.newAlias(((Integer)yyVals[-3+yyTop].value), ((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[24] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new VAliasNode(((Integer)yyVals[-2+yyTop].value), support.symbolID(((ByteList)yyVals[-1+yyTop].value)), support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[25] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new VAliasNode(((Integer)yyVals[-2+yyTop].value), support.symbolID(((ByteList)yyVals[-1+yyTop].value)), support.symbolID(((BackRefNode)yyVals[0+yyTop].value).getByteName()));
  return yyVal;
};
states[26] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.yyerror("can't make alias for the number variables");
  return yyVal;
};
states[27] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[28] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_if(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.cond(((Node)yyVals[0+yyTop].value)), ((Node)yyVals[-2+yyTop].value), null);
                    support.fixpos(((Node)yyVal), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[29] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_if(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.cond(((Node)yyVals[0+yyTop].value)), null, ((Node)yyVals[-2+yyTop].value));
                    support.fixpos(((Node)yyVal), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[30] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (((Node)yyVals[-2+yyTop].value) != null && ((Node)yyVals[-2+yyTop].value) instanceof BeginNode) {
                        yyVal = new WhileNode(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.cond(((Node)yyVals[0+yyTop].value)), ((BeginNode)yyVals[-2+yyTop].value).getBodyNode(), false);
                    } else {
                        yyVal = new WhileNode(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.cond(((Node)yyVals[0+yyTop].value)), ((Node)yyVals[-2+yyTop].value), true);
                    }
  return yyVal;
};
states[31] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (((Node)yyVals[-2+yyTop].value) != null && ((Node)yyVals[-2+yyTop].value) instanceof BeginNode) {
                        yyVal = new UntilNode(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.cond(((Node)yyVals[0+yyTop].value)), ((BeginNode)yyVals[-2+yyTop].value).getBodyNode(), false);
                    } else {
                        yyVal = new UntilNode(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.cond(((Node)yyVals[0+yyTop].value)), ((Node)yyVals[-2+yyTop].value), true);
                    }
  return yyVal;
};
states[32] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newRescueModNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[33] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   if (lexer.getLexContext().in_def) {
                       support.warn(ID.END_IN_METHOD, ((Integer)yyVals[-3+yyTop].value), "END in method; use at_exit");
                    }
                    yyVal = new PostExeNode(((Integer)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[35] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = node_assign(((MultipleAsgnNode)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[36] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = node_assign(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[37] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     /* FIXME: ...*/
                    yyVal = null;
  return yyVal;
};
states[38] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = node_assign(((MultipleAsgnNode)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[40] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = node_assign(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[41] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_op_assign(((AssignableNode)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[42] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_ary_op_assign(((Node)yyVals[-6+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[43] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value));
  return yyVal;
};
states[44] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value));
  return yyVal;
};
states[45] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    int line = ((Node)yyVals[-5+yyTop].value).getLine();
                    yyVal = support.new_const_op_assign(line, support.new_colon2(line, ((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-3+yyTop].value)), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[46] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value));
  return yyVal;
};
states[47] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.backrefAssignError(((Node)yyVals[-3+yyTop].value));
  return yyVal;
};
states[48] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[49] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[-2+yyTop].value));
                    yyVal = support.newRescueModNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[52] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newAndNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[53] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newOrNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[54] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(support.method_cond(((Node)yyVals[0+yyTop].value)), lexer.BANG);
  return yyVal;
};
states[55] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(support.method_cond(((Node)yyVals[0+yyTop].value)), BANG);
  return yyVal;
};
states[56] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[-1+yyTop].value));
                    lexer.setState(EXPR_BEG|EXPR_LABEL);
                    lexer.commandStart = false;
                    LexContext ctxt = lexer.getLexContext();
                    yyVal = (LexContext) ctxt.clone();
                    ctxt.in_kwarg = true;
  return yyVal;
};
states[57] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.push_pvtbl();
  return yyVal;
};
states[58] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.pop_pvtbl(((Table)yyVals[-1+yyTop].value));
  return yyVal;
};
states[59] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    LexContext ctxt = lexer.getLexContext();
                    ctxt.in_kwarg = ((LexContext)yyVals[-3+yyTop].value).in_kwarg;
                    yyVal = support.newCaseNode(((Node)yyVals[-5+yyTop].value).getLine(), ((Node)yyVals[-5+yyTop].value), support.newIn(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-1+yyTop].value), null, null));
                    support.warn_one_line_pattern_matching(yyVal, ((Node)yyVals[-1+yyTop].value), true);
  return yyVal;
};
states[60] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[-1+yyTop].value));
                    lexer.setState(EXPR_BEG|EXPR_LABEL);
                    lexer.commandStart = false;
                    LexContext ctxt = lexer.getLexContext();
                    yyVal = (LexContext) ctxt.clone();
                    ctxt.in_kwarg = true;
  return yyVal;
};
states[61] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.push_pvtbl();
  return yyVal;
};
states[62] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.pop_pvtbl(((Table)yyVals[-1+yyTop].value));
  return yyVal;
};
states[63] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    LexContext ctxt = lexer.getLexContext();
                    ctxt.in_kwarg = ((LexContext)yyVals[-3+yyTop].value).in_kwarg;
                    yyVal = support.newCaseNode(((Node)yyVals[-5+yyTop].value).getLine(), ((Node)yyVals[-5+yyTop].value), support.newIn(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-1+yyTop].value), new TrueNode(lexer.tokline), new FalseNode(lexer.tokline)));
                    support.warn_one_line_pattern_matching(yyVal, ((Node)yyVals[-1+yyTop].value), false);
  return yyVal;
};
states[65] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.pushLocalScope();
                    LexContext ctxt = lexer.getLexContext();
                    yyVal = new DefHolder(support.symbolID(((ByteList)yyVals[0+yyTop].value)), lexer.getCurrentArg(), (LexContext) ctxt.clone());
                    ctxt.in_def = true;
                    lexer.setCurrentArg(null);
  return yyVal;
};
states[66] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    ((DefHolder)yyVals[0+yyTop].value).line = ((Integer)yyVals[-1+yyTop].value);
                    yyVal = ((DefHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[67] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setState(EXPR_FNAME); 
  return yyVal;
};
states[68] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setState(EXPR_ENDFN|EXPR_LABEL);
                    ((DefHolder)yyVals[0+yyTop].value).line = ((Integer)yyVals[-4+yyTop].value);
                    ((DefHolder)yyVals[0+yyTop].value).setSingleton(((Node)yyVals[-3+yyTop].value));
                    yyVal = ((DefHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[69] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[70] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                  lexer.getConditionState().begin();
  return yyVal;
};
states[71] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   lexer.getConditionState().end();
                   yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[75] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].startLine());
  return yyVal;
};
states[76] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((IterNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[77] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_fcall(((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[78] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.frobnicate_fcall_args(((FCallNode)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
                    yyVal = ((FCallNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[79] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.frobnicate_fcall_args(((FCallNode)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value));
                    yyVal = ((FCallNode)yyVals[-2+yyTop].value);
  return yyVal;
};
states[80] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].startLine());
  return yyVal;
};
states[81] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value), yyVals[yyTop - count + 3].startLine());
  return yyVal;
};
states[82] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[83] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[84] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_super(((Integer)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[85] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_yield(((Integer)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[86] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ReturnNode(((Integer)yyVals[-1+yyTop].value), support.ret_args(((Node)yyVals[0+yyTop].value), ((Integer)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[87] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new BreakNode(((Integer)yyVals[-1+yyTop].value), support.ret_args(((Node)yyVals[0+yyTop].value), ((Integer)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[88] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new NextNode(((Integer)yyVals[-1+yyTop].value), support.ret_args(((Node)yyVals[0+yyTop].value), ((Integer)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[90] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[91] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((MultipleAsgnNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[92] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new MultipleAsgnNode(((Integer)yyVals[-2+yyTop].value), support.newArrayNode(((Integer)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value)), null, null);
  return yyVal;
};
states[93] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[0+yyTop].value).getLine(), ((ListNode)yyVals[0+yyTop].value), null, null);
  return yyVal;
};
states[94] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-1+yyTop].value).getLine(), ((ListNode)yyVals[-1+yyTop].value).add(((Node)yyVals[0+yyTop].value)), null, null);
  return yyVal;
};
states[95] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-2+yyTop].value).getLine(), ((ListNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value), (ListNode) null);
  return yyVal;
};
states[96] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-4+yyTop].value).getLine(), ((ListNode)yyVals[-4+yyTop].value), ((Node)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[97] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-1+yyTop].value).getLine(), ((ListNode)yyVals[-1+yyTop].value), new StarNode(lexer.getRubySourceline()), null);
  return yyVal;
};
states[98] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), new StarNode(lexer.getRubySourceline()), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[99] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new MultipleAsgnNode(((Node)yyVals[0+yyTop].value).getLine(), null, ((Node)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[100] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new MultipleAsgnNode(((Node)yyVals[-2+yyTop].value).getLine(), null, ((Node)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[101] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                      yyVal = new MultipleAsgnNode(lexer.getRubySourceline(), null, new StarNode(lexer.getRubySourceline()), null);
  return yyVal;
};
states[102] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                      yyVal = new MultipleAsgnNode(lexer.getRubySourceline(), null, new StarNode(lexer.getRubySourceline()), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[104] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[105] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newArrayNode(((Node)yyVals[-1+yyTop].value).getLine(), ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[106] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[107] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[108] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[109] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = support.assignableLabelOrIdentifier(((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[110] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = new InstAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[111] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = new GlobalAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[112] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (lexer.getLexContext().in_def) support.compile_error("dynamic constant assignment");
                    yyVal = new ConstDeclNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), null, NilImplicitNode.NIL);
  return yyVal;
};
states[113] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ClassVarAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[114] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to nil");
                    yyVal = null;
  return yyVal;
};
states[115] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't change the value of self");
                    yyVal = null;
  return yyVal;
};
states[116] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to true");
                    yyVal = null;
  return yyVal;
};
states[117] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to false");
                    yyVal = null;
  return yyVal;
};
states[118] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to __FILE__");
                    yyVal = null;
  return yyVal;
};
states[119] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to __LINE__");
                    yyVal = null;
  return yyVal;
};
states[120] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to __ENCODING__");
                    yyVal = null;
  return yyVal;
};
states[121] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.aryset(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[122] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[123] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[124] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[125] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (lexer.getLexContext().in_def) support.yyerror("dynamic constant assignment");

                    Integer position = support.getPosition(((Node)yyVals[-2+yyTop].value));

                    yyVal = new ConstDeclNode(position, (RubySymbol) null, support.new_colon2(position, ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[126] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (lexer.getLexContext().in_def) {
                        support.yyerror("dynamic constant assignment");
                    }

                    Integer position = lexer.tokline;

                    yyVal = new ConstDeclNode(position, (RubySymbol) null, support.new_colon3(position, ((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[127] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.backrefAssignError(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[128] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.assignableLabelOrIdentifier(((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[129] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new InstAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[130] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new GlobalAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[131] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (lexer.getLexContext().in_def) support.compile_error("dynamic constant assignment");

                    yyVal = new ConstDeclNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), null, NilImplicitNode.NIL);
  return yyVal;
};
states[132] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ClassVarAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[133] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to nil");
                    yyVal = null;
  return yyVal;
};
states[134] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't change the value of self");
                    yyVal = null;
  return yyVal;
};
states[135] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to true");
                    yyVal = null;
  return yyVal;
};
states[136] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to false");
                    yyVal = null;
  return yyVal;
};
states[137] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to __FILE__");
                    yyVal = null;
  return yyVal;
};
states[138] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to __LINE__");
                    yyVal = null;
  return yyVal;
};
states[139] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to __ENCODING__");
                    yyVal = null;
  return yyVal;
};
states[140] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.aryset(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[141] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[142] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[143] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[144] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (lexer.getLexContext().in_def) {
                        support.yyerror("dynamic constant assignment");
                    }

                    Integer position = support.getPosition(((Node)yyVals[-2+yyTop].value));

                    yyVal = new ConstDeclNode(position, (RubySymbol) null, support.new_colon2(position, ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[145] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (lexer.getLexContext().in_def) {
                        support.yyerror("dynamic constant assignment");
                    }

                    Integer position = lexer.tokline;

                    yyVal = new ConstDeclNode(position, (RubySymbol) null, support.new_colon3(position, ((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[146] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.backrefAssignError(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[147] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.yyerror("class/module name must be CONSTANT", yyVals[yyTop - count + 1]);
  return yyVal;
};
states[148] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[149] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_colon3(lexer.tokline, ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[150] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_colon2(lexer.tokline, null, ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[151] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_colon2(support.getPosition(((Node)yyVals[-2+yyTop].value)), ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[152] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[153] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[154] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[155] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   lexer.setState(EXPR_ENDFN);
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[156] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[157] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal =  new LiteralNode(lexer.getRubySourceline(), support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[158] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[159] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ParserSupport.newUndef(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[160] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setState(EXPR_FNAME|EXPR_FITEM);
  return yyVal;
};
states[161] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.appendToBlock(((Node)yyVals[-3+yyTop].value), ParserSupport.newUndef(((Node)yyVals[-3+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[162] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = OR;
  return yyVal;
};
states[163] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = CARET;
  return yyVal;
};
states[164] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = AMPERSAND;
  return yyVal;
};
states[165] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[166] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[167] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[168] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[169] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[170] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = GT;
  return yyVal;
};
states[171] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[172] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = LT;
  return yyVal;
};
states[173] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[174] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[175] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[176] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[177] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = PLUS;
  return yyVal;
};
states[178] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = MINUS;
  return yyVal;
};
states[179] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = STAR;
  return yyVal;
};
states[180] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[181] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = SLASH;
  return yyVal;
};
states[182] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = PERCENT;
  return yyVal;
};
states[183] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[184] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[185] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = BANG;
  return yyVal;
};
states[186] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = TILDE;
  return yyVal;
};
states[187] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[188] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[189] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[190] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[191] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = BACKTICK;
  return yyVal;
};
states[192] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.__LINE__.bytes;
  return yyVal;
};
states[193] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.__FILE__.bytes;
  return yyVal;
};
states[194] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.__ENCODING__.bytes;
  return yyVal;
};
states[195] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.LBEGIN.bytes;
  return yyVal;
};
states[196] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.LEND.bytes;
  return yyVal;
};
states[197] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.ALIAS.bytes;
  return yyVal;
};
states[198] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.AND.bytes;
  return yyVal;
};
states[199] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.BEGIN.bytes;
  return yyVal;
};
states[200] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.BREAK.bytes;
  return yyVal;
};
states[201] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.CASE.bytes;
  return yyVal;
};
states[202] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.CLASS.bytes;
  return yyVal;
};
states[203] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.DEF.bytes;
  return yyVal;
};
states[204] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.DEFINED_P.bytes;
  return yyVal;
};
states[205] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.DO.bytes;
  return yyVal;
};
states[206] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.ELSE.bytes;
  return yyVal;
};
states[207] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.ELSIF.bytes;
  return yyVal;
};
states[208] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.END.bytes;
  return yyVal;
};
states[209] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.ENSURE.bytes;
  return yyVal;
};
states[210] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.FALSE.bytes;
  return yyVal;
};
states[211] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.FOR.bytes;
  return yyVal;
};
states[212] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.IN.bytes;
  return yyVal;
};
states[213] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.MODULE.bytes;
  return yyVal;
};
states[214] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.NEXT.bytes;
  return yyVal;
};
states[215] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.NIL.bytes;
  return yyVal;
};
states[216] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.NOT.bytes;
  return yyVal;
};
states[217] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.OR.bytes;
  return yyVal;
};
states[218] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.REDO.bytes;
  return yyVal;
};
states[219] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.RESCUE.bytes;
  return yyVal;
};
states[220] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.RETRY.bytes;
  return yyVal;
};
states[221] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.RETURN.bytes;
  return yyVal;
};
states[222] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.SELF.bytes;
  return yyVal;
};
states[223] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.SUPER.bytes;
  return yyVal;
};
states[224] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.THEN.bytes;
  return yyVal;
};
states[225] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.TRUE.bytes;
  return yyVal;
};
states[226] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.UNDEF.bytes;
  return yyVal;
};
states[227] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.WHEN.bytes;
  return yyVal;
};
states[228] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.YIELD.bytes;
  return yyVal;
};
states[229] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.IF.bytes;
  return yyVal;
};
states[230] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.UNLESS.bytes;
  return yyVal;
};
states[231] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.WHILE.bytes;
  return yyVal;
};
states[232] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RubyLexer.Keyword.UNTIL.bytes;
  return yyVal;
};
states[233] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.node_assign(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[234] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_op_assign(((AssignableNode)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[235] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_ary_op_assign(((Node)yyVals[-6+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[236] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value));
  return yyVal;
};
states[237] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value));
  return yyVal;
};
states[238] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.new_attr_op_assign(((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-4+yyTop].value), ((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value));
  return yyVal;
};
states[239] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    Integer pos = support.getPosition(((Node)yyVals[-5+yyTop].value));
                    yyVal = support.new_const_op_assign(pos, support.new_colon2(pos, ((Node)yyVals[-5+yyTop].value), ((ByteList)yyVals[-3+yyTop].value)), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[240] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    Integer pos = lexer.getRubySourceline();
                    yyVal = support.new_const_op_assign(pos, new Colon3Node(pos, support.symbolID(((ByteList)yyVals[-3+yyTop].value))), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[241] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.backrefAssignError(((Node)yyVals[-3+yyTop].value));
  return yyVal;
};
states[242] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[-2+yyTop].value));
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
    
                    boolean isLiteral = ((Node)yyVals[-2+yyTop].value) instanceof FixnumNode && ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.makeNullNil(((Node)yyVals[-2+yyTop].value)), support.makeNullNil(((Node)yyVals[0+yyTop].value)), false, isLiteral);
  return yyVal;
};
states[243] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[-2+yyTop].value));
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));

                    boolean isLiteral = ((Node)yyVals[-2+yyTop].value) instanceof FixnumNode && ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.makeNullNil(((Node)yyVals[-2+yyTop].value)), support.makeNullNil(((Node)yyVals[0+yyTop].value)), true, isLiteral);
  return yyVal;
};
states[244] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[-1+yyTop].value));

                    boolean isLiteral = ((Node)yyVals[-1+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(support.getPosition(((Node)yyVals[-1+yyTop].value)), support.makeNullNil(((Node)yyVals[-1+yyTop].value)), NilImplicitNode.NIL, false, isLiteral);
  return yyVal;
};
states[245] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[-1+yyTop].value));

                    boolean isLiteral = ((Node)yyVals[-1+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(support.getPosition(((Node)yyVals[-1+yyTop].value)), support.makeNullNil(((Node)yyVals[-1+yyTop].value)), NilImplicitNode.NIL, true, isLiteral);
  return yyVal;
};
states[246] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].startLine(), NilImplicitNode.NIL, support.makeNullNil(((Node)yyVals[0+yyTop].value)), false, isLiteral);
  return yyVal;
};
states[247] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].startLine(), NilImplicitNode.NIL, support.makeNullNil(((Node)yyVals[0+yyTop].value)), true, isLiteral);
  return yyVal;
};
states[248] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), PLUS, ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[249] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), MINUS, ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[250] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), STAR, ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[251] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), SLASH, ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[252] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), PERCENT, ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[253] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[254] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((NumericNode)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline()), ((ByteList)yyVals[-3+yyTop].value));
  return yyVal;
};
states[255] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-1+yyTop].value));
  return yyVal;
};
states[256] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop].value), ((ByteList)yyVals[-1+yyTop].value));
  return yyVal;
};
states[257] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), OR, ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[258] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), CARET, ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[259] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), AMPERSAND, ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[260] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[261] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[262] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[263] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[264] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[265] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getMatchNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[266] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[267] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(support.method_cond(((Node)yyVals[0+yyTop].value)), BANG);
  return yyVal;
};
states[268] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop].value), TILDE);
  return yyVal;
};
states[269] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[270] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[271] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newAndNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[272] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newOrNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[273] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.getLexContext().in_defined = true;
  return yyVal;
};
states[274] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.getLexContext().in_defined = false;                    
                    yyVal = new DefinedNode(((Integer)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[275] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[-5+yyTop].value));
                    yyVal = support.new_if(support.getPosition(((Node)yyVals[-5+yyTop].value)), support.cond(((Node)yyVals[-5+yyTop].value)), ((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[276] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.endless_method_name(((DefHolder)yyVals[-3+yyTop].value));
                    support.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
                    yyVal = new DefnNode(((DefHolder)yyVals[-3+yyTop].value).line, ((DefHolder)yyVals[-3+yyTop].value).name, ((ArgsNode)yyVals[-2+yyTop].value), support.getCurrentScope(), support.reduce_nodes(((Node)yyVals[0+yyTop].value)), yyVals[yyTop - count + 4].end);
                    if (support.isNextBreak) ((DefnNode)yyVal).setContainsNextBreak();
                    support.popCurrentScope();
  return yyVal;
};
states[277] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.endless_method_name(((DefHolder)yyVals[-5+yyTop].value));
                    support.restore_defun(((DefHolder)yyVals[-5+yyTop].value));
                    Node body = support.reduce_nodes(support.rescued_expr(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value)));
                    yyVal = new DefnNode(((DefHolder)yyVals[-5+yyTop].value).line, ((DefHolder)yyVals[-5+yyTop].value).name, ((ArgsNode)yyVals[-4+yyTop].value), support.getCurrentScope(), body, yyVals[yyTop - count + 6].end);
                    if (support.isNextBreak) ((DefnNode)yyVal).setContainsNextBreak();
                    support.popCurrentScope();
  return yyVal;
};
states[278] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.endless_method_name(((DefHolder)yyVals[-3+yyTop].value));
                    support.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
                    yyVal = new DefsNode(((DefHolder)yyVals[-3+yyTop].value).line, ((DefHolder)yyVals[-3+yyTop].value).singleton, ((DefHolder)yyVals[-3+yyTop].value).name, ((ArgsNode)yyVals[-2+yyTop].value), support.getCurrentScope(), support.reduce_nodes(((Node)yyVals[0+yyTop].value)), yyVals[yyTop - count + 4].end);
                    if (support.isNextBreak) ((DefsNode)yyVal).setContainsNextBreak();
                    support.popCurrentScope();
  return yyVal;
};
states[279] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.endless_method_name(((DefHolder)yyVals[-5+yyTop].value));
                    support.restore_defun(((DefHolder)yyVals[-5+yyTop].value));
                    Node body = support.reduce_nodes(support.rescued_expr(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value)));
                    yyVal = new DefsNode(((DefHolder)yyVals[-5+yyTop].value).line, ((DefHolder)yyVals[-5+yyTop].value).singleton, ((DefHolder)yyVals[-5+yyTop].value).name, ((ArgsNode)yyVals[-4+yyTop].value), support.getCurrentScope(), body, yyVals[yyTop - count + 6].end);
                    if (support.isNextBreak) ((DefsNode)yyVal).setContainsNextBreak();                    support.popCurrentScope();
  return yyVal;
};
states[280] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[281] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = GT;
  return yyVal;
};
states[282] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = LT;
  return yyVal;
};
states[283] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[284] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[285] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[286] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     support.warning(ID.MISCELLANEOUS, lexer.getRubySourceline(), "comparison '" + ((ByteList)yyVals[-1+yyTop].value) + "' after comparison");
                     yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), lexer.getRubySourceline());
  return yyVal;
};
states[287] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = (LexContext) lexer.getLexContext().clone();
  return yyVal;
};
states[288] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = (LexContext) lexer.getLexContext().clone();
  return yyVal;
};
states[289] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.makeNullNil(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[291] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[292] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.arg_append(((Node)yyVals[-3+yyTop].value), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[293] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newArrayNode(((HashNode)yyVals[-1+yyTop].value).getLine(), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[294] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[295] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[-2+yyTop].value));
                    yyVal = support.newRescueModNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[296] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[297] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (!support.check_forwarding_args()) {
                        yyVal = null;
                    } else {
                        yyVal = support.new_args_forward_call(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-3+yyTop].value));
                    }
  return yyVal;
};
states[298] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (!support.check_forwarding_args()) {
                        yyVal = null;
                    } else {
                        yyVal = support.new_args_forward_call(yyVals[yyTop - count + 1].startLine(), null);
                    }
  return yyVal;
};
states[303] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[304] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.arg_append(((Node)yyVals[-3+yyTop].value), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[305] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newArrayNode(((HashNode)yyVals[-1+yyTop].value).getLine(), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[306] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = support.newArrayNode(support.getPosition(((Node)yyVals[0+yyTop].value)), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[307] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = arg_blk_pass(((Node)yyVals[-1+yyTop].value), ((BlockPassNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[308] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newArrayNode(((HashNode)yyVals[-1+yyTop].value).getLine(), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
                    yyVal = arg_blk_pass(((Node)yyVal), ((BlockPassNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[309] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.arg_append(((Node)yyVals[-3+yyTop].value), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value)));
                    yyVal = arg_blk_pass(((Node)yyVal), ((BlockPassNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[310] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
  return yyVal;
};
states[311] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = Long.valueOf(lexer.getCmdArgumentState().getStack());
                    lexer.getCmdArgumentState().begin();
  return yyVal;
};
states[312] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop].value).longValue());
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[313] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new BlockPassNode(support.getPosition(((Node)yyVals[0+yyTop].value)), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[314] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((BlockPassNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[316] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    int line = ((Node)yyVals[0+yyTop].value) instanceof NilImplicitNode ? lexer.getRubySourceline() : ((Node)yyVals[0+yyTop].value).getLine();
                    yyVal = support.newArrayNode(line, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[317] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newSplatNode(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[318] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    Node node = support.splat_array(((Node)yyVals[-2+yyTop].value));

                    if (node != null) {
                        yyVal = support.list_append(node, ((Node)yyVals[0+yyTop].value));
                    } else {
                        yyVal = support.arg_append(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    }
  return yyVal;
};
states[319] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
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
states[320] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[321] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[322] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    Node node = support.splat_array(((Node)yyVals[-2+yyTop].value));

                    if (node != null) {
                        yyVal = support.list_append(node, ((Node)yyVals[0+yyTop].value));
                    } else {
                        yyVal = support.arg_append(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    }
  return yyVal;
};
states[323] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    Node node = null;

                    if (((Node)yyVals[0+yyTop].value) instanceof ArrayNode &&
                        (node = support.splat_array(((Node)yyVals[-3+yyTop].value))) != null) {
                        yyVal = support.list_concat(node, ((Node)yyVals[0+yyTop].value));
                    } else {
                        yyVal = ParserSupport.arg_concat(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
                    }
  return yyVal;
};
states[324] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.newSplatNode(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[329] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ListNode)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[330] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ListNode)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[331] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ListNode)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[332] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ListNode)yyVals[0+yyTop].value); /* FIXME: Why complaining without $$ = $1;*/
  return yyVal;
};
states[335] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.new_fcall(((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[336] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = lexer.getCmdArgumentState().getStack();
                    lexer.getCmdArgumentState().reset();
  return yyVal;
};
states[337] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-2+yyTop].value).longValue());
                    yyVal = new BeginNode(((Integer)yyVals[-3+yyTop].value), support.makeNullNil(((Node)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[338] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setState(EXPR_ENDARG);
  return yyVal;
};
states[339] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = null; /*FIXME: Should be implicit nil?*/
  return yyVal;
};
states[340] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = lexer.getCmdArgumentState().getStack();
                    lexer.getCmdArgumentState().reset();
  return yyVal;
};
states[341] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setState(EXPR_ENDARG); 
  return yyVal;
};
states[342] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-3+yyTop].value).longValue());
                    yyVal = ((Node)yyVals[-2+yyTop].value);
  return yyVal;
};
states[343] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (((Node)yyVals[-1+yyTop].value) != null) {
                        /* compstmt position includes both parens around it*/
                        ((Node)yyVals[-1+yyTop].value).setLine(((Integer)yyVals[-2+yyTop].value));
                        yyVal = ((Node)yyVals[-1+yyTop].value);
                    } else {
                        yyVal = new NilNode(((Integer)yyVals[-2+yyTop].value));
                    }
  return yyVal;
};
states[344] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_colon2(support.getPosition(((Node)yyVals[-2+yyTop].value)), ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[345] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_colon3(lexer.tokline, ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[346] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    Integer position = support.getPosition(((Node)yyVals[-1+yyTop].value));
                    if (((Node)yyVals[-1+yyTop].value) == null) {
                        yyVal = new ZArrayNode(position); /* zero length array */
                    } else {
                        yyVal = ((Node)yyVals[-1+yyTop].value);
                    }
  return yyVal;
};
states[347] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((HashNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[348] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ReturnNode(((Integer)yyVals[0+yyTop].value), NilImplicitNode.NIL);
  return yyVal;
};
states[349] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_yield(((Integer)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[350] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new YieldNode(((Integer)yyVals[-2+yyTop].value), null);
  return yyVal;
};
states[351] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new YieldNode(((Integer)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[352] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.getLexContext().in_defined = true;
  return yyVal;
};
states[353] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.getLexContext().in_defined = false;
                    yyVal = new DefinedNode(((Integer)yyVals[-5+yyTop].value), ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[354] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(support.method_cond(((Node)yyVals[-1+yyTop].value)), lexer.BANG);
  return yyVal;
};
states[355] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.getOperatorCallNode(support.method_cond(NilImplicitNode.NIL), lexer.BANG);
  return yyVal;
};
states[356] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.frobnicate_fcall_args(((FCallNode)yyVals[-1+yyTop].value), null, ((IterNode)yyVals[0+yyTop].value));
                    yyVal = ((FCallNode)yyVals[-1+yyTop].value);                    
  return yyVal;
};
states[358] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (((Node)yyVals[-1+yyTop].value) != null && 
                          ((BlockAcceptingNode)yyVals[-1+yyTop].value).getIterNode() instanceof BlockPassNode) {
                          lexer.compile_error("Both block arg and actual block given.");
                    }
                    yyVal = ((BlockAcceptingNode)yyVals[-1+yyTop].value).setIterNode(((IterNode)yyVals[0+yyTop].value));
                    ((Node)yyVal).setLine(((Node)yyVals[-1+yyTop].value).getLine());
  return yyVal;
};
states[359] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((LambdaNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[360] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_if(((Integer)yyVals[-5+yyTop].value), support.cond(((Node)yyVals[-4+yyTop].value)), ((Node)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[361] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_if(((Integer)yyVals[-5+yyTop].value), support.cond(((Node)yyVals[-4+yyTop].value)), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[-2+yyTop].value));
  return yyVal;
};
states[362] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new WhileNode(((Integer)yyVals[-3+yyTop].value), support.cond(((Node)yyVals[-2+yyTop].value)), support.makeNullNil(((Node)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[363] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new UntilNode(((Integer)yyVals[-3+yyTop].value), support.cond(((Node)yyVals[-2+yyTop].value)), support.makeNullNil(((Node)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[364] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.case_labels;
                    support.case_labels = support.getConfiguration().getRuntime().getNil();
  return yyVal;
};
states[365] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newCaseNode(((Integer)yyVals[-5+yyTop].value), ((Node)yyVals[-4+yyTop].value), ((Node)yyVals[-1+yyTop].value));
                    support.fixpos(((Node)yyVal), ((Node)yyVals[-4+yyTop].value));
  return yyVal;
};
states[366] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.case_labels;
                    support.case_labels = null;
  return yyVal;
};
states[367] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newCaseNode(((Integer)yyVals[-4+yyTop].value), null, ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[368] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newCaseNode(((Integer)yyVals[-4+yyTop].value), ((Node)yyVals[-3+yyTop].value), ((InNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[369] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ForNode(((Integer)yyVals[-5+yyTop].value), ((Node)yyVals[-4+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[-2+yyTop].value), support.getCurrentScope(), 111);
  return yyVal;
};
states[370] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    LexContext ctxt = lexer.getLexContext();
                    if (ctxt.in_def) {
                        support.yyerror("class definition in method body");
                    }
                    ctxt.in_class = true;
                    support.pushLocalScope();
  return yyVal;
};
states[371] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    Node body = support.makeNullNil(((Node)yyVals[-1+yyTop].value));

                    yyVal = new ClassNode(yyVals[yyTop - count + 1].startLine(), ((Colon3Node)yyVals[-4+yyTop].value), support.getCurrentScope(), body, ((Node)yyVals[-3+yyTop].value), lexer.getRubySourceline());
                    LexContext ctxt = lexer.getLexContext();
                    support.popCurrentScope();
                    ctxt.in_class = ((LexContext)yyVals[-5+yyTop].value).in_class;
                    ctxt.shareable_constant_value = ((LexContext)yyVals[-5+yyTop].value).shareable_constant_value;
  return yyVal;
};
states[372] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    LexContext ctxt = lexer.getLexContext();
                    ctxt.in_def = false;
                    ctxt.in_class = false;
                    support.pushLocalScope();
  return yyVal;
};
states[373] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    Node body = support.makeNullNil(((Node)yyVals[-1+yyTop].value));

                    yyVal = new SClassNode(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-4+yyTop].value), support.getCurrentScope(), body, lexer.getRubySourceline());
                    LexContext ctxt = lexer.getLexContext();
                    ctxt.in_def = ((LexContext)yyVals[-6+yyTop].value).in_def;
                    ctxt.in_class = ((LexContext)yyVals[-6+yyTop].value).in_class;
                    ctxt.shareable_constant_value = ((LexContext)yyVals[-6+yyTop].value).shareable_constant_value;
                    support.popCurrentScope();
  return yyVal;
};
states[374] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    LexContext ctxt = lexer.getLexContext();
                    if (ctxt.in_def) { 
                        support.yyerror("module definition in method body");
                    }
                    ctxt.in_class = true;
                    support.pushLocalScope();
  return yyVal;
};
states[375] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    Node body = support.makeNullNil(((Node)yyVals[-1+yyTop].value));

                    yyVal = new ModuleNode(yyVals[yyTop - count + 1].startLine(), ((Colon3Node)yyVals[-3+yyTop].value), support.getCurrentScope(), body, lexer.getRubySourceline());
                    support.popCurrentScope();
                    LexContext ctxt = lexer.getLexContext();
                    ctxt.in_class = ((LexContext)yyVals[-4+yyTop].value).in_class;
                    ctxt.shareable_constant_value = ((LexContext)yyVals[-4+yyTop].value).shareable_constant_value;
  return yyVal;
};
states[376] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
                    Node body = support.reduce_nodes(support.makeNullNil(((Node)yyVals[-1+yyTop].value)));
                    yyVal = new DefnNode(((DefHolder)yyVals[-3+yyTop].value).line, ((DefHolder)yyVals[-3+yyTop].value).name, ((ArgsNode)yyVals[-2+yyTop].value), support.getCurrentScope(), body, yyVals[yyTop - count + 4].end);
                    if (support.isNextBreak) ((DefnNode)yyVal).setContainsNextBreak();                    support.popCurrentScope();
  return yyVal;
};
states[377] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
                    Node body = support.reduce_nodes(support.makeNullNil(((Node)yyVals[-1+yyTop].value)));
                    yyVal = new DefsNode(((DefHolder)yyVals[-3+yyTop].value).line, ((DefHolder)yyVals[-3+yyTop].value).singleton, ((DefHolder)yyVals[-3+yyTop].value).name, ((ArgsNode)yyVals[-2+yyTop].value), support.getCurrentScope(), body, yyVals[yyTop - count + 4].end);
                    if (support.isNextBreak) ((DefsNode)yyVal).setContainsNextBreak();
                    support.popCurrentScope();
  return yyVal;
};
states[378] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.isNextBreak = true;
                    yyVal = new BreakNode(((Integer)yyVals[0+yyTop].value), NilImplicitNode.NIL);
  return yyVal;
};
states[379] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.isNextBreak = true;
                    yyVal = new NextNode(((Integer)yyVals[0+yyTop].value), NilImplicitNode.NIL);
  return yyVal;
};
states[380] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new RedoNode(((Integer)yyVals[0+yyTop].value));
  return yyVal;
};
states[381] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new RetryNode(((Integer)yyVals[0+yyTop].value));
  return yyVal;
};
states[382] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = ((Node)yyVals[0+yyTop].value);
                    if (yyVal == null) yyVal = NilImplicitNode.NIL;
  return yyVal;
};
states[383] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[384] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[385] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[386] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[387] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[388] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[389] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[390] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = (LexContext) lexer.getLexContext().clone();
  return yyVal;
};
states[391] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = (LexContext) lexer.getLexContext().clone();  
  return yyVal;
};
states[392] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[393] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[394] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[395] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[396] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[397] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[398] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[399] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[400] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[401] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    LexContext ctxt = lexer.getLexContext();
                    if (ctxt.in_class && !ctxt.in_def && !support.getCurrentScope().isBlockScope()) {
                        lexer.compile_error("Invalid return in class/module body");
                    }
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[408] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_if(((Integer)yyVals[-4+yyTop].value), support.cond(((Node)yyVals[-3+yyTop].value)), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[410] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[412] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
  return yyVal;
};
states[413] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.assignableInCurr(((ByteList)yyVals[0+yyTop].value), NilImplicitNode.NIL);
  return yyVal;
};
states[414] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[415] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[416] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[417] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[0+yyTop].value).getLine(), ((ListNode)yyVals[0+yyTop].value), null, null);
  return yyVal;
};
states[418] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-2+yyTop].value).getLine(), ((ListNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[419] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-4+yyTop].value).getLine(), ((ListNode)yyVals[-4+yyTop].value), ((Node)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[420] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new MultipleAsgnNode(lexer.getRubySourceline(), null, ((Node)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[421] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new MultipleAsgnNode(lexer.getRubySourceline(), null, ((Node)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[422] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.assignableInCurr(((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[423] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new StarNode(lexer.getRubySourceline());
  return yyVal;
};
states[425] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = NilImplicitNode.NIL;
  return yyVal;
};
states[426] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args_tail(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[427] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args_tail(((ListNode)yyVals[-1+yyTop].value).getLine(), ((ListNode)yyVals[-1+yyTop].value), (ByteList) null, ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[428] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args_tail(lexer.getRubySourceline(), null, ((ByteList)yyVals[-1+yyTop].value), ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[429] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args_tail(((BlockArgNode)yyVals[0+yyTop].value).getLine(), null, (ByteList) null, ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[430] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[431] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args_tail(lexer.getRubySourceline(), null, (ByteList) null, null);
  return yyVal;
};
states[432] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = null;
  return yyVal;
};
states[433] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), ((ListNode)yyVals[-5+yyTop].value), ((ListNode)yyVals[-3+yyTop].value), ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[434] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-7+yyTop].value).getLine(), ((ListNode)yyVals[-7+yyTop].value), ((ListNode)yyVals[-5+yyTop].value), ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[435] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[436] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), ((ListNode)yyVals[-5+yyTop].value), ((ListNode)yyVals[-3+yyTop].value), null, ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[437] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), null, ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[438] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    RestArgNode rest = new UnnamedRestArgNode(((ListNode)yyVals[-1+yyTop].value).getLine(), null, support.getCurrentScope().addVariable("*"));
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop].value).getLine(), ((ListNode)yyVals[-1+yyTop].value), null, rest, null, (ArgsTailHolder) null);
  return yyVal;
};
states[439] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), ((ListNode)yyVals[-5+yyTop].value), null, ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[440] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop].value).getLine(), ((ListNode)yyVals[-1+yyTop].value), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[441] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(support.getPosition(((ListNode)yyVals[-3+yyTop].value)), null, ((ListNode)yyVals[-3+yyTop].value), ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[442] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(support.getPosition(((ListNode)yyVals[-5+yyTop].value)), null, ((ListNode)yyVals[-5+yyTop].value), ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[443] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(support.getPosition(((ListNode)yyVals[-1+yyTop].value)), null, ((ListNode)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[444] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), null, ((ListNode)yyVals[-3+yyTop].value), null, ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[445] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((RestArgNode)yyVals[-1+yyTop].value).getLine(), null, null, ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[446] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((RestArgNode)yyVals[-3+yyTop].value).getLine(), null, null, ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[447] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ArgsTailHolder)yyVals[0+yyTop].value).getLine(), null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[448] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
    /* was $$ = null;*/
                    yyVal = support.new_args(lexer.getRubySourceline(), null, null, null, null, (ArgsTailHolder) null);
  return yyVal;
};
states[449] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.commandStart = true;
                    yyVal = ((ArgsNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[450] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setCurrentArg(null);
                    yyVal = support.new_args(lexer.getRubySourceline(), null, null, null, null, (ArgsTailHolder) null);
  return yyVal;
};
states[451] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setCurrentArg(null);
                    yyVal = ((ArgsNode)yyVals[-2+yyTop].value);
  return yyVal;
};
states[452] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = null;
  return yyVal;
};
states[453] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = null;
  return yyVal;
};
states[454] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = null;
  return yyVal;
};
states[455] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = null;
  return yyVal;
};
states[456] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.new_bv(((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[457] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = null;
  return yyVal;
};
states[458] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.pushBlockScope();
                    yyVal = lexer.getLeftParenBegin();
                    lexer.setLeftParenBegin(lexer.incrementParenNest());
  return yyVal;
};
states[459] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = Long.valueOf(lexer.getCmdArgumentState().getStack());
                    lexer.getCmdArgumentState().reset();
  return yyVal;
};
states[460] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop].value).longValue());
                    lexer.getCmdArgumentState().restart();
                    yyVal = new LambdaNode(yyVals[yyTop - count + 1].startLine(), ((ArgsNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value), support.getCurrentScope(), lexer.getRubySourceline());
                    lexer.setLeftParenBegin(((Integer)yyVals[-3+yyTop].value));
                    support.popCurrentScope();
  return yyVal;
};
states[461] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ArgsNode)yyVals[-2+yyTop].value);
  return yyVal;
};
states[462] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ArgsNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[463] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[464] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[465] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((IterNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[466] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
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
states[467] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].startLine());
  return yyVal;
};
states[468] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value), yyVals[yyTop - count + 3].startLine());
  return yyVal;
};
states[469] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value), yyVals[yyTop - count + 3].startLine());
  return yyVal;
};
states[470] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.frobnicate_fcall_args(((FCallNode)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
                    yyVal = ((FCallNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[471] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].startLine());
  return yyVal;
};
states[472] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[473] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value), null, null);
  return yyVal;
};
states[474] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), LexingCommon.CALL, ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].startLine());
  return yyVal;
};
states[475] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop].value), LexingCommon.CALL, ((Node)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[476] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_super(((Integer)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[477] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ZSuperNode(((Integer)yyVals[0+yyTop].value));
  return yyVal;
};
states[478] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (((Node)yyVals[-3+yyTop].value) instanceof SelfNode) {
                        yyVal = support.new_fcall(LexingCommon.LBRACKET_RBRACKET);
                        support.frobnicate_fcall_args(((FCallNode)yyVal), ((Node)yyVals[-1+yyTop].value), null);
                    } else {
                        yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), lexer.LBRACKET_RBRACKET, ((Node)yyVals[-1+yyTop].value), null);
                    }
  return yyVal;
};
states[479] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((IterNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[480] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((IterNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[481] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = lexer.getRubySourceline();
  return yyVal;
};
states[482] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.pushBlockScope();
                    yyVal = Long.valueOf(lexer.getCmdArgumentState().getStack()) >> 1;
                    lexer.getCmdArgumentState().reset();
  return yyVal;
};
states[483] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new IterNode(((Integer)yyVals[-3+yyTop].value), ((ArgsNode)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), support.getCurrentScope(), lexer.getRubySourceline());
                    support.popCurrentScope();
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-2+yyTop].value).longValue());
  return yyVal;
};
states[484] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = lexer.getRubySourceline();
  return yyVal;
};
states[485] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.pushBlockScope();
                    yyVal = Long.valueOf(lexer.getCmdArgumentState().getStack());
                    lexer.getCmdArgumentState().reset();
  return yyVal;
};
states[486] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new IterNode(((Integer)yyVals[-3+yyTop].value), ((ArgsNode)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), support.getCurrentScope(), lexer.getRubySourceline());
                    support.popCurrentScope();
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-2+yyTop].value).longValue());
  return yyVal;
};
states[487] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     support.check_literal_when(((Node)yyVals[0+yyTop].value));
                     yyVal = support.newArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[488] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newSplatNode(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[489] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.check_literal_when(((Node)yyVals[0+yyTop].value));
                    yyVal = support.last_arg_append(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[490] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.rest_arg_append(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[491] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newWhenNode(((Integer)yyVals[-4+yyTop].value), ((Node)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[494] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setState(EXPR_BEG|EXPR_LABEL);
                    lexer.commandStart = false;
                    LexContext ctxt = (LexContext) lexer.getLexContext();
                    yyVals[0+yyTop].value = (LexContext) ctxt.clone();
                    ctxt.in_kwarg = true;
                    yyVal = support.push_pvtbl();
  return yyVal;
};
states[495] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.push_pktbl();
  return yyVal;
};
states[496] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.pop_pktbl(((Table)yyVals[-2+yyTop].value));
                    support.pop_pvtbl(((Table)yyVals[-3+yyTop].value));
                    lexer.getLexContext().in_kwarg = ((LexContext)yyVals[-4+yyTop].value).in_kwarg;
  return yyVal;
};
states[497] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newIn(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-4+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[499] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((InNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[501] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_if(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[0+yyTop].value), support.remove_begin(((Node)yyVals[-2+yyTop].value)), null);
                    support.fixpos(((Node)yyVal), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[502] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
  yyVal = support.new_if(yyVals[yyTop - count + 1].startLine(), support.remove_begin(((Node)yyVals[-2+yyTop].value)), ((Node)yyVals[0+yyTop].value), null);
                    support.fixpos(((Node)yyVal), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[504] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), null, ((Node)yyVals[-1+yyTop].value),
                                                   support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), null, true, null, null));
  return yyVal;
};
states[505] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), null, ((Node)yyVals[-2+yyTop].value), ((ArrayPatternNode)yyVals[0+yyTop].value));
                   support.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].startLine());
  return yyVal;
};
states[506] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_find_pattern(null, ((FindPatternNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[507] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), null, null, ((ArrayPatternNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[508] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_hash_pattern(null, ((HashPatternNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[510] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new HashNode(yyVals[yyTop - count + 1].startLine(), new KeyValuePair(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[512] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newOrNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[514] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.push_pktbl();
  return yyVal;
};
states[515] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.push_pktbl();
  return yyVal;
};
states[517] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.pop_pktbl(((Table)yyVals[-2+yyTop].value));
                    yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-3+yyTop].value), null, ((ArrayPatternNode)yyVals[-1+yyTop].value));
                    support.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].startLine());
  return yyVal;
};
states[518] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     support.pop_pktbl(((Table)yyVals[-2+yyTop].value));
                     yyVal = support.new_find_pattern(((Node)yyVals[-3+yyTop].value), ((FindPatternNode)yyVals[-1+yyTop].value));
                     support.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].startLine());
  return yyVal;
};
states[519] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     support.pop_pktbl(((Table)yyVals[-2+yyTop].value));
                     yyVal = support.new_hash_pattern(((Node)yyVals[-3+yyTop].value), ((HashPatternNode)yyVals[-1+yyTop].value));
                     support.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].startLine());
  return yyVal;
};
states[520] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-2+yyTop].value), null,
                                                    support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), null, false, null, null));
  return yyVal;
};
states[521] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     support.pop_pktbl(((Table)yyVals[-2+yyTop].value));
                     yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-3+yyTop].value), null, ((ArrayPatternNode)yyVals[-1+yyTop].value));
                     support.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].startLine());
  return yyVal;
};
states[522] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.pop_pktbl(((Table)yyVals[-2+yyTop].value));
                    yyVal = support.new_find_pattern(((Node)yyVals[-3+yyTop].value), ((FindPatternNode)yyVals[-1+yyTop].value));
                    support.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].startLine());
  return yyVal;
};
states[523] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.pop_pktbl(((Table)yyVals[-2+yyTop].value));
                    yyVal = support.new_hash_pattern(((Node)yyVals[-3+yyTop].value), ((HashPatternNode)yyVals[-1+yyTop].value));
                    support.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].startLine());
  return yyVal;
};
states[524] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-2+yyTop].value), null,
                            support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), null, false, null, null));
  return yyVal;
};
states[525] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), null, null, ((ArrayPatternNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[526] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_find_pattern(null, ((FindPatternNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[527] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), null, null,
                            support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), null, false, null, null));
  return yyVal;
};
states[528] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.push_pktbl();
                    LexContext ctxt = lexer.getLexContext();
                    yyVals[0+yyTop].value = ctxt;
                    ctxt.in_kwarg = false;
  return yyVal;
};
states[529] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.pop_pktbl(((Table)yyVals[-2+yyTop].value));
                    lexer.getLexContext().in_kwarg = ((LexContext)yyVals[-3+yyTop].value).in_kwarg;
                    yyVal = support.new_hash_pattern(null, ((HashPatternNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[530] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_hash_pattern(null, support.new_hash_pattern_tail(yyVals[yyTop - count + 1].startLine(), null, null));
  return yyVal;
};
states[531] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.push_pktbl();
  return yyVal;
};
states[532] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.pop_pktbl(((Table)yyVals[-2+yyTop].value));
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[533] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     ListNode preArgs = support.newArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), preArgs, false, null, null);
  return yyVal;
};
states[534] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((ListNode)yyVals[0+yyTop].value), true, null, null);
  return yyVal;
};
states[535] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), support.list_concat(((ListNode)yyVals[-1+yyTop].value), ((ListNode)yyVals[0+yyTop].value)), false, null, null);
  return yyVal;
};
states[536] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((ListNode)yyVals[-2+yyTop].value), true, ((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[537] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((ListNode)yyVals[-4+yyTop].value), true, ((ByteList)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[538] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((ListNode)yyVals[-1+yyTop].value), true, null, null);
  return yyVal;
};
states[539] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((ListNode)yyVals[-3+yyTop].value), true, null, ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[540] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ArrayPatternNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[541] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ListNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[542] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.list_concat(((ListNode)yyVals[-2+yyTop].value), ((ListNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[543] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), null, true, ((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[544] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), null, true, ((ByteList)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[545] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.new_find_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((ByteList)yyVals[-4+yyTop].value), ((ListNode)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));

                     /* FIXME: impl
                     if (rb_warning_category_enabled_p(RB_WARN_CATEGORY_EXPERIMENTAL)) {
                         rb_warn0L_experimental(yyVals[yyTop - count + 1].startLine(), "Find pattern is experimental, and the behavior may change in future versions of Ruby!");
                         }*/
  return yyVal;
};
states[546] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[547] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = null;
  return yyVal;
};
states[549] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.list_concat(((ListNode)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[550] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[551] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_hash_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((HashNode)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[552] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_hash_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((HashNode)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[553] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_hash_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((HashNode)yyVals[-1+yyTop].value), null);
  return yyVal;
};
states[554] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_hash_pattern_tail(yyVals[yyTop - count + 1].startLine(), null, ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[555] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new HashNode(yyVals[yyTop - count + 1].start, ((KeyValuePair)yyVals[0+yyTop].value));
  return yyVal;
};
states[556] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    ((HashNode)yyVals[-2+yyTop].value).add(((KeyValuePair)yyVals[0+yyTop].value));
                    yyVal = ((HashNode)yyVals[-2+yyTop].value);
  return yyVal;
};
states[557] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.error_duplicate_pattern_key(((ByteList)yyVals[-1+yyTop].value));

                    yyVal = new KeyValuePair(((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[558] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.error_duplicate_pattern_key(((ByteList)yyVals[0+yyTop].value));
                    if (((ByteList)yyVals[0+yyTop].value) != null && !support.is_local_id(((ByteList)yyVals[0+yyTop].value))) {
                        support.yyerror("key must be valid as local variables");
                    }
                    support.error_duplicate_pattern_variable(((ByteList)yyVals[0+yyTop].value));
                    yyVal = new KeyValuePair(((ByteList)yyVals[0+yyTop].value), support.assignableLabelOrIdentifier(((ByteList)yyVals[0+yyTop].value), null));
  return yyVal;
};
states[560] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (((Node)yyVals[-1+yyTop].value) == null || ((Node)yyVals[-1+yyTop].value) instanceof StrNode) {
                        yyVal = ((StrNode)yyVals[-1+yyTop].value).getValue();
                    } else {
                        support.yyerror("symbol literal with interpolation is not allowed");
                        yyVal = null;
                    }
  return yyVal;
};
states[561] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[562] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = null;
  return yyVal;
};
states[563] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = null;
  return yyVal;
};
states[565] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.KWNOREST;
  return yyVal;
};
states[567] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[-2+yyTop].value));
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[-2+yyTop].value) instanceof FixnumNode && ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].startLine(), support.makeNullNil(((Node)yyVals[-2+yyTop].value)), support.makeNullNil(((Node)yyVals[0+yyTop].value)), false, isLiteral);
  return yyVal;
};
states[568] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[-2+yyTop].value));
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[-2+yyTop].value) instanceof FixnumNode && ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].startLine(), support.makeNullNil(((Node)yyVals[-2+yyTop].value)), support.makeNullNil(((Node)yyVals[0+yyTop].value)), true, isLiteral);
  return yyVal;
};
states[569] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[-1+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[-1+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].startLine(), support.makeNullNil(((Node)yyVals[-1+yyTop].value)), NilImplicitNode.NIL, false, isLiteral);
  return yyVal;
};
states[570] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[-1+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[-1+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].startLine(), support.makeNullNil(((Node)yyVals[-1+yyTop].value)), NilImplicitNode.NIL, true, isLiteral);
  return yyVal;
};
states[574] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].startLine(), NilImplicitNode.NIL, support.makeNullNil(((Node)yyVals[0+yyTop].value)), false, isLiteral);
  return yyVal;
};
states[575] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].startLine(), NilImplicitNode.NIL, support.makeNullNil(((Node)yyVals[0+yyTop].value)), true, isLiteral);
  return yyVal;
};
states[580] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[581] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[582] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[583] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[584] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new NilNode(lexer.tokline);
  return yyVal;
};
states[585] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new SelfNode(lexer.tokline);
  return yyVal;
};
states[586] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new TrueNode(lexer.tokline);
  return yyVal;
};
states[587] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new FalseNode(lexer.tokline);
  return yyVal;
};
states[588] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new FileNode(lexer.tokline, new ByteList(lexer.getFile().getBytes(),
                    support.getConfiguration().getRuntime().getEncodingService().getLocaleEncoding()));
  return yyVal;
};
states[589] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new FixnumNode(lexer.tokline, lexer.tokline+1);
  return yyVal;
};
states[590] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new EncodingNode(lexer.tokline, lexer.getEncoding());
  return yyVal;
};
states[591] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((LambdaNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[592] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.error_duplicate_pattern_variable(((ByteList)yyVals[0+yyTop].value));
                    yyVal = support.assignableInCurr(((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[593] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    Node n = support.declareIdentifier(((ByteList)yyVals[0+yyTop].value));
                    if (!(n instanceof LocalVarNode || n instanceof DVarNode)) {
                        support.compile_error("" + ((ByteList)yyVals[0+yyTop].value) + ": no such local variable");
                    }
                    yyVal = n;
  return yyVal;
};
states[594] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_colon3(lexer.tokline, ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[595] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_colon2(lexer.tokline, ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[596] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ConstNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[597] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
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
states[598] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = null; 
  return yyVal;
};
states[599] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[600] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.splat_array(((Node)yyVals[0+yyTop].value));
                    if (yyVal == null) yyVal = ((Node)yyVals[0+yyTop].value); /* ArgsCat or ArgsPush*/
  return yyVal;
};
states[602] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[604] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[606] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((NumericNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[607] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[608] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
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
states[609] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((StrNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[610] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[611] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[612] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.heredoc_dedent(((Node)yyVals[-1+yyTop].value));
		    lexer.setHeredocIndent(0);
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[613] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
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
states[614] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newRegexpNode(support.getPosition(((Node)yyVals[-1+yyTop].value)), ((Node)yyVals[-1+yyTop].value), (RegexpNode) ((RegexpNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[615] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[616] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = new ArrayNode(lexer.getRubySourceline());
  return yyVal;
};
states[617] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[-1+yyTop].value) instanceof EvStrNode ? new DStrNode(((ListNode)yyVals[-2+yyTop].value).getLine(), lexer.getEncoding()).add(((Node)yyVals[-1+yyTop].value)) : ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[618] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[619] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[620] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[621] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ArrayNode(lexer.getRubySourceline());
  return yyVal;
};
states[622] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[-1+yyTop].value) instanceof EvStrNode ? new DSymbolNode(((ListNode)yyVals[-2+yyTop].value).getLine()).add(((Node)yyVals[-1+yyTop].value)) : support.asSymbol(((ListNode)yyVals[-2+yyTop].value).getLine(), ((Node)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[623] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[624] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[625] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ArrayNode(lexer.getRubySourceline());
  return yyVal;
};
states[626] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[627] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ArrayNode(lexer.getRubySourceline());
  return yyVal;
};
states[628] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(support.asSymbol(((ListNode)yyVals[-2+yyTop].value).getLine(), ((Node)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[629] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    ByteList aChar = ByteList.create("");
                    aChar.setEncoding(lexer.getEncoding());
                    yyVal = lexer.createStr(aChar, 0);
  return yyVal;
};
states[630] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[631] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    ByteList aChar = ByteList.create("");
                    aChar.setEncoding(lexer.getEncoding());
                    yyVal = lexer.createStr(aChar, 0);
  return yyVal;
};
states[632] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[633] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = null;
  return yyVal;
};
states[634] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
    /* FIXME: mri is different here.*/
                    yyVal = support.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[635] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[636] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = lexer.getStrTerm();
                    lexer.setStrTerm(null);
                    lexer.setState(EXPR_BEG);
  return yyVal;
};
states[637] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setStrTerm(((StrTerm)yyVals[-1+yyTop].value));
                    yyVal = new EvStrNode(support.getPosition(((Node)yyVals[0+yyTop].value)), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[638] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = lexer.getStrTerm();
                   lexer.setStrTerm(null);
                   lexer.getConditionState().stop();
  return yyVal;
};
states[639] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = lexer.getCmdArgumentState().getStack();
                   lexer.getCmdArgumentState().reset();
  return yyVal;
};
states[640] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = lexer.getState();
                   lexer.setState(EXPR_BEG);
  return yyVal;
};
states[641] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = lexer.getBraceNest();
                   lexer.setBraceNest(0);
  return yyVal;
};
states[642] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = lexer.getHeredocIndent();
                   lexer.setHeredocIndent(0);
  return yyVal;
};
states[643] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   lexer.getConditionState().restart();
                   lexer.setStrTerm(((StrTerm)yyVals[-6+yyTop].value));
                   lexer.getCmdArgumentState().reset(((Long)yyVals[-5+yyTop].value).longValue());
                   lexer.setState(((Integer)yyVals[-4+yyTop].value));
                   lexer.setBraceNest(((Integer)yyVals[-3+yyTop].value));
                   lexer.setHeredocIndent(((Integer)yyVals[-2+yyTop].value));
                   lexer.setHeredocLineIndent(-1);

                   if (((Node)yyVals[-1+yyTop].value) != null) ((Node)yyVals[-1+yyTop].value).unsetNewline();
                   yyVal = support.newEvStrNode(support.getPosition(((Node)yyVals[-1+yyTop].value)), ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[644] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = new GlobalVarNode(lexer.getRubySourceline(), support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[645] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = new InstVarNode(lexer.getRubySourceline(), support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[646] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = new ClassVarNode(lexer.getRubySourceline(), support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[650] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     lexer.setState(EXPR_END);
                     yyVal = support.asSymbol(lexer.getRubySourceline(), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[652] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[653] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[654] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[655] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
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
states[656] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((NumericNode)yyVals[0+yyTop].value);  
  return yyVal;
};
states[657] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.negateNumeric(((NumericNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[658] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[659] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((FloatNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[660] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((RationalNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[661] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[662] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.declareIdentifier(((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[663] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new InstVarNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[664] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new GlobalVarNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[665] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ConstNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[666] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ClassVarNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[667] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new NilNode(lexer.tokline);
  return yyVal;
};
states[668] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new SelfNode(lexer.tokline);
  return yyVal;
};
states[669] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new TrueNode(lexer.tokline);
  return yyVal;
};
states[670] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new FalseNode(lexer.tokline);
  return yyVal;
};
states[671] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new FileNode(lexer.tokline, new ByteList(lexer.getFile().getBytes(),
                    support.getConfiguration().getRuntime().getEncodingService().getLocaleEncoding()));
  return yyVal;
};
states[672] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new FixnumNode(lexer.tokline, lexer.tokline+1);
  return yyVal;
};
states[673] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new EncodingNode(lexer.tokline, lexer.getEncoding());
  return yyVal;
};
states[674] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.assignableLabelOrIdentifier(((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[675] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new InstAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[676] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new GlobalAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[677] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (lexer.getLexContext().in_def) support.compile_error("dynamic constant assignment");

                    yyVal = new ConstDeclNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), null, NilImplicitNode.NIL);
  return yyVal;
};
states[678] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ClassVarAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[679] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to nil");
                    yyVal = null;
  return yyVal;
};
states[680] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't change the value of self");
                    yyVal = null;
  return yyVal;
};
states[681] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to true");
                    yyVal = null;
  return yyVal;
};
states[682] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to false");
                    yyVal = null;
  return yyVal;
};
states[683] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to __FILE__");
                    yyVal = null;
  return yyVal;
};
states[684] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to __LINE__");
                    yyVal = null;
  return yyVal;
};
states[685] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to __ENCODING__");
                    yyVal = null;
  return yyVal;
};
states[686] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[687] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[688] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   lexer.setState(EXPR_BEG);
                   lexer.commandStart = true;
  return yyVal;
};
states[689] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[690] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = null;
  return yyVal;
};
states[692] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = null;
  return yyVal;
};
states[693] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ArgsNode)yyVals[-1+yyTop].value);
                    lexer.setState(EXPR_BEG);
                    lexer.commandStart = true;
  return yyVal;
};
states[694] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.add_forwarding_args();
                    yyVal = support.new_args_forward_def(yyVals[yyTop - count + 1].startLine(), ((ListNode)yyVals[-3+yyTop].value));
                    lexer.setState(EXPR_BEG);
                    lexer.commandStart = true;

  return yyVal;
};
states[695] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.add_forwarding_args();
                    yyVal = support.new_args_forward_def(yyVals[yyTop - count + 1].startLine(), null);
                    lexer.setState(EXPR_BEG);
                    lexer.commandStart = true;
  return yyVal;
};
states[696] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = ((ArgsNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[697] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   LexContext ctxt = lexer.getLexContext();
                   yyVal = (LexContext) ctxt.clone();
                   ctxt.in_kwarg = true;
                   lexer.setState(lexer.getState() | EXPR_LABEL);
  return yyVal;
};
states[698] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.getLexContext().in_kwarg = ((LexContext)yyVals[-2+yyTop].value).in_kwarg;
                    yyVal = ((ArgsNode)yyVals[-1+yyTop].value);
                    lexer.setState(EXPR_BEG);
                    lexer.commandStart = true;
  return yyVal;
};
states[699] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args_tail(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[700] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args_tail(((ListNode)yyVals[-1+yyTop].value).getLine(), ((ListNode)yyVals[-1+yyTop].value), (ByteList) null, ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[701] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args_tail(lexer.getRubySourceline(), null, ((ByteList)yyVals[-1+yyTop].value), ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[702] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args_tail(((BlockArgNode)yyVals[0+yyTop].value).getLine(), null, (ByteList) null, ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[703] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[704] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args_tail(lexer.getRubySourceline(), null, (ByteList) null, null);
  return yyVal;
};
states[705] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), ((ListNode)yyVals[-5+yyTop].value), ((ListNode)yyVals[-3+yyTop].value), ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[706] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-7+yyTop].value).getLine(), ((ListNode)yyVals[-7+yyTop].value), ((ListNode)yyVals[-5+yyTop].value), ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[707] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[708] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), ((ListNode)yyVals[-5+yyTop].value), ((ListNode)yyVals[-3+yyTop].value), null, ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[709] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), null, ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[710] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), ((ListNode)yyVals[-5+yyTop].value), null, ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[711] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop].value).getLine(), ((ListNode)yyVals[-1+yyTop].value), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[712] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), null, ((ListNode)yyVals[-3+yyTop].value), ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[713] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), null, ((ListNode)yyVals[-5+yyTop].value), ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[714] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop].value).getLine(), null, ((ListNode)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[715] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), null, ((ListNode)yyVals[-3+yyTop].value), null, ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[716] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((RestArgNode)yyVals[-1+yyTop].value).getLine(), null, null, ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[717] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((RestArgNode)yyVals[-3+yyTop].value).getLine(), null, null, ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[718] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ArgsTailHolder)yyVals[0+yyTop].value).getLine(), null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[719] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(lexer.getRubySourceline(), null, null, null, null, (ArgsTailHolder) null);
  return yyVal;
};
states[720] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[721] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.yyerror("formal argument cannot be a constant");
  return yyVal;
};
states[722] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.yyerror("formal argument cannot be an instance variable");
  return yyVal;
};
states[723] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.yyerror("formal argument cannot be a global variable");
  return yyVal;
};
states[724] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.yyerror("formal argument cannot be a class variable");
  return yyVal;
};
states[725] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value); /* Not really reached*/
  return yyVal;
};
states[726] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.formal_argument(((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[727] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setCurrentArg(((ByteList)yyVals[0+yyTop].value));
                    yyVal = support.arg_var(((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[728] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setCurrentArg(null);
                    yyVal = ((ArgumentNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[729] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[730] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ArrayNode(lexer.getRubySourceline(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[731] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[0+yyTop].value));
                    yyVal = ((ListNode)yyVals[-2+yyTop].value);
  return yyVal;
};
states[732] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.arg_var(support.formal_argument(((ByteList)yyVals[0+yyTop].value)));
                    lexer.setCurrentArg(((ByteList)yyVals[0+yyTop].value));
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[733] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setCurrentArg(null);
                    yyVal = new KeywordArgNode(((Node)yyVals[0+yyTop].value).getLine(), support.assignableKeyword(((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[734] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setCurrentArg(null);
                    yyVal = new KeywordArgNode(lexer.getRubySourceline(), support.assignableKeyword(((ByteList)yyVals[0+yyTop].value), new RequiredKeywordArgumentValueNode()));
  return yyVal;
};
states[735] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new KeywordArgNode(support.getPosition(((Node)yyVals[0+yyTop].value)), support.assignableKeyword(((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[736] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new KeywordArgNode(lexer.getRubySourceline(), support.assignableKeyword(((ByteList)yyVals[0+yyTop].value), new RequiredKeywordArgumentValueNode()));
  return yyVal;
};
states[737] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[738] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[739] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ArrayNode(((KeywordArgNode)yyVals[0+yyTop].value).getLine(), ((KeywordArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[740] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((KeywordArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[741] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[742] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[743] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[744] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.shadowing_lvar(((ByteList)yyVals[0+yyTop].value));
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[745] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.INTERNAL_ID;
  return yyVal;
};
states[746] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setCurrentArg(null);
                    yyVal = new OptArgNode(support.getPosition(((Node)yyVals[0+yyTop].value)), support.assignableLabelOrIdentifier(((ArgumentNode)yyVals[-2+yyTop].value).getName().getBytes(), ((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[747] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setCurrentArg(null);
                    yyVal = new OptArgNode(support.getPosition(((Node)yyVals[0+yyTop].value)), support.assignableLabelOrIdentifier(((ArgumentNode)yyVals[-2+yyTop].value).getName().getBytes(), ((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[748] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new BlockNode(((Node)yyVals[0+yyTop].value).getLine()).add(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[749] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[750] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new BlockNode(((Node)yyVals[0+yyTop].value).getLine()).add(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[751] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[752] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = STAR;
  return yyVal;
};
states[753] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[754] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (!support.is_local_id(((ByteList)yyVals[0+yyTop].value))) {
                        support.yyerror("rest argument must be local variable");
                    }
                    
                    yyVal = new RestArgNode(support.arg_var(support.shadowing_lvar(((ByteList)yyVals[0+yyTop].value))));
  return yyVal;
};
states[755] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
  /* FIXME: bytelist_love: somewhat silly to remake the empty bytelist over and over but this type should change (using null vs "" is a strange distinction).*/
  yyVal = new UnnamedRestArgNode(lexer.getRubySourceline(), support.symbolID(CommonByteLists.EMPTY), support.getCurrentScope().addVariable("*"));
  return yyVal;
};
states[756] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = AMPERSAND;
  return yyVal;
};
states[757] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[758] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (!support.is_local_id(((ByteList)yyVals[0+yyTop].value))) {
                        support.yyerror("block argument must be local variable");
                    }
                    
                    yyVal = new BlockArgNode(support.arg_var(support.shadowing_lvar(((ByteList)yyVals[0+yyTop].value))));
  return yyVal;
};
states[759] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((BlockArgNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[760] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = null;
  return yyVal;
};
states[761] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[762] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setState(EXPR_BEG);
  return yyVal;
};
states[763] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (((Node)yyVals[-1+yyTop].value) == null) {
                        support.yyerror("can't define single method for ().");
                    } else if (((Node)yyVals[-1+yyTop].value) instanceof ILiteralNode) {
                        support.yyerror("can't define single method for literals.");
                    }
                    support.value_expr(lexer, ((Node)yyVals[-1+yyTop].value));
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[764] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new HashNode(lexer.getRubySourceline());
  return yyVal;
};
states[765] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[766] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new HashNode(lexer.getRubySourceline(), ((KeyValuePair)yyVals[0+yyTop].value));
  return yyVal;
};
states[767] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((HashNode)yyVals[-2+yyTop].value).add(((KeyValuePair)yyVals[0+yyTop].value));
  return yyVal;
};
states[768] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.createKeyValue(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[769] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    Node label = support.asSymbol(support.getPosition(((Node)yyVals[0+yyTop].value)), ((ByteList)yyVals[-1+yyTop].value));
                    yyVal = support.createKeyValue(label, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[770] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
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
states[771] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.createKeyValue(null, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[772] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[773] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[774] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[775] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[776] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[777] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[778] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[779] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[780] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[781] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[782] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = DOT;
  return yyVal;
};
states[783] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[784] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = DOT;
  return yyVal;
};
states[785] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[787] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[792] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RPAREN;
  return yyVal;
};
states[793] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RBRACKET;
  return yyVal;
};
states[794] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RCURLY;
  return yyVal;
};
states[802] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                      yyVal = null;
  return yyVal;
};
states[803] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                  yyVal = null;
  return yyVal;
};
}
					// line 3454 "RubyParser.y"

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
					// line 12845 "-"
