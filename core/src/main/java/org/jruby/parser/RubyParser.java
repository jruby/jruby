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
					// line 191 "-"
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
//yyLhs 808
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
   246,   247,   114,   115,   115,   116,   116,   121,    81,    81,
    81,    81,    44,    44,    44,    44,    44,    44,    44,    44,
    44,   119,   119,   248,   249,   250,   117,   251,   252,   253,
   118,    54,    54,    54,    54,    53,    55,    55,   254,   255,
   256,   132,   133,   133,   134,   134,   134,   135,   135,   135,
   135,   135,   135,   136,   137,   137,   138,   138,   210,   211,
   139,   139,   139,   139,   139,   139,   139,   139,   139,   139,
   139,   139,   257,   139,   139,   258,   139,   141,   141,   141,
   141,   141,   141,   141,   141,   142,   142,   143,   143,   140,
   174,   174,   144,   144,   145,   151,   151,   151,   151,   152,
   152,   153,   153,   178,   178,   175,   175,   176,   177,   177,
   146,   146,   146,   146,   146,   146,   146,   146,   146,   146,
   147,   147,   147,   147,   147,   147,   147,   147,   147,   147,
   147,   147,   147,   147,   147,   147,   148,   149,   150,   150,
   150,    56,    56,    57,    57,    57,    58,    58,    59,    59,
    20,    20,     2,     3,     3,     3,     4,     5,     6,    11,
    16,    16,    19,    19,    12,    13,    13,    14,    15,    17,
    17,    18,    18,     7,     7,     8,     8,     9,     9,    10,
   259,    10,   260,   261,   262,   263,   264,    10,   101,   101,
   101,   101,    25,    25,    23,   154,   154,   154,   154,    24,
    21,    21,    22,    22,    22,    22,    73,    73,    73,    73,
    73,    73,    73,    73,    73,    73,    73,    73,    74,    74,
    74,    74,    74,    74,    74,    74,    74,    74,    74,    74,
   100,   100,   265,    80,    80,    86,    86,    87,    87,    87,
    85,   266,    85,    65,    65,    65,    65,    66,    66,    88,
    88,    88,    88,    88,    88,    88,    88,    88,    88,    88,
    88,    88,    88,    88,   181,   165,   165,   165,   165,   164,
   164,   168,    90,    90,    89,    89,   167,   108,   108,   110,
   110,   109,   109,   107,   107,   186,   186,   179,   166,   166,
   106,    84,    83,    83,    91,    91,   185,   185,   161,   161,
   184,   184,   162,   163,   163,     1,   267,     1,    96,    96,
    97,    97,    98,    98,    98,    98,   155,   155,   155,   156,
   156,   156,   156,   157,   157,   157,   173,   173,   169,   169,
   170,   170,   213,   213,   218,   218,   187,   188,   202,   230,
   230,   230,   241,   241,   214,   214,   123,   189,
    }, yyLen = {
//yyLen 808
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
     0,     0,     7,     4,     1,     3,     3,     3,     2,     4,
     5,     5,     2,     4,     4,     3,     3,     3,     2,     1,
     4,     3,     3,     0,     0,     0,     5,     0,     0,     0,
     5,     1,     2,     3,     4,     5,     1,     1,     0,     0,
     0,     8,     1,     1,     1,     3,     3,     1,     2,     3,
     1,     1,     1,     1,     3,     1,     3,     1,     1,     1,
     1,     4,     4,     4,     3,     4,     4,     4,     3,     3,
     3,     2,     0,     4,     2,     0,     4,     1,     1,     2,
     3,     5,     2,     4,     1,     2,     3,     1,     3,     5,
     2,     1,     1,     3,     1,     3,     1,     2,     1,     1,
     3,     2,     1,     1,     3,     2,     1,     2,     1,     1,
     1,     3,     3,     2,     2,     1,     1,     1,     2,     2,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     2,     2,     3,
     1,     6,     1,     1,     1,     1,     2,     1,     2,     1,
     1,     1,     1,     1,     1,     2,     3,     3,     3,     4,
     0,     3,     1,     2,     4,     0,     3,     4,     4,     0,
     3,     0,     3,     0,     2,     0,     2,     0,     2,     1,
     0,     3,     0,     0,     0,     0,     0,     8,     1,     1,
     1,     1,     1,     1,     2,     1,     1,     1,     1,     3,
     1,     2,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     0,     4,     0,     1,     1,     3,     5,     3,
     1,     0,     3,     4,     2,     2,     1,     2,     0,     6,
     8,     4,     6,     4,     6,     2,     4,     6,     2,     4,
     2,     4,     1,     0,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     3,     1,     3,     1,     2,     1,     2,
     1,     1,     3,     1,     3,     1,     1,     2,     2,     1,
     3,     3,     1,     3,     1,     3,     1,     1,     2,     1,
     1,     1,     2,     2,     0,     1,     0,     4,     1,     2,
     1,     3,     3,     2,     4,     2,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     0,     1,     0,     1,     2,     2,     2,     0,
     1,     1,     1,     1,     1,     2,     0,     0,
    }, yyDefRed = {
//yyDefRed 1346
     1,     0,     0,     0,   390,   391,   392,     0,   383,   384,
   385,   388,   386,   387,   389,     0,     0,   380,   381,   401,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   662,   663,   664,   665,   613,   690,   691,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   458,     0,
   633,   635,   637,     0,     0,     0,     0,     0,     0,   326,
     0,   614,   327,   328,   329,   331,   330,   332,   325,   610,
   660,   652,   653,   611,     0,     0,     2,     0,     5,     0,
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
     0,     0,     0,     0,     0,   633,     0,     0,   306,     0,
     0,     0,    87,   310,     0,     0,   770,     0,     0,    88,
     0,    85,     0,     0,   478,    84,     0,   795,     0,     0,
    22,     0,     0,     9,     0,     0,   378,   379,     0,     0,
   255,     0,     0,   348,     0,     0,     0,     0,     0,    20,
     0,     0,     0,    16,     0,    15,     0,     0,     0,     0,
     0,     0,     0,   290,     0,     0,     0,   768,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   382,     0,     0,     0,
   459,   657,   656,   658,     0,   654,   655,     0,     0,     0,
   620,   629,   625,   631,   267,    55,   268,   615,     0,     0,
     0,     0,   696,     0,     0,     0,   802,   803,     3,     0,
   804,     0,     0,     0,     0,     0,     0,     0,    60,     0,
     0,     0,     0,     0,   283,   284,     0,     0,     0,     0,
     0,     0,     0,     0,    56,     0,   281,   282,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   394,   468,   487,
   393,   483,   358,   487,   789,     0,     0,   788,     0,   472,
     0,   356,     0,     0,   791,   790,     0,     0,     0,     0,
     0,     0,     0,   105,    86,   672,   671,   673,   674,   676,
   675,   677,     0,   668,   667,     0,   670,     0,     0,     0,
     0,   334,   150,   374,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   766,     0,    66,   765,
    65,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   411,   412,     0,   366,     0,     0,   160,   773,     0,   317,
   775,   313,     0,     0,     0,     0,     0,     0,   307,   315,
     0,     0,   308,     0,     0,     0,   350,     0,   312,     0,
     0,   302,     0,     0,   301,     0,     0,   355,    54,    24,
    26,    25,     0,   352,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   343,    14,     0,     0,   339,     0,
     0,   800,   291,   346,     0,   293,   347,   769,     0,   661,
     0,   107,     0,   700,     0,     0,     0,     0,   460,   639,
   659,   642,   640,   634,   616,   617,   636,   618,   638,     0,
     0,     0,     0,   730,   727,   726,   725,   728,   736,   745,
   724,     0,   757,   746,   761,   760,   756,   722,     0,     0,
   734,     0,   754,     0,   743,     0,   706,   731,   729,   424,
     0,     0,   425,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   805,     6,    28,    29,    30,
    31,    32,    52,    53,    61,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,    57,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   488,     0,   484,     0,     0,     0,     0,   477,     0,   475,
     0,     0,     0,     0,   781,     0,   476,     0,   782,   483,
    79,     0,   287,   288,     0,   779,   780,     0,     0,     0,
     0,     0,     0,     0,   106,     0,   147,     0,   149,   692,
   370,     0,     0,     0,     0,     0,   403,     0,     0,     0,
   787,   786,    67,     0,     0,     0,     0,     0,     0,     0,
    70,     0,     0,     0,     0,     0,   772,     0,     0,     0,
     0,     0,     0,     0,   314,     0,     0,   771,     0,     0,
   349,   796,     0,   296,     0,   298,   354,    23,     0,     0,
    10,    33,     0,     0,     0,     0,    21,     0,    17,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   643,
     0,   619,   622,     0,     0,   627,   624,     0,     0,   628,
     0,     0,   415,     0,     0,     0,   413,   697,     0,   715,
     0,   718,     0,   704,     0,   720,   737,     0,     0,   705,
   699,   762,   758,   747,   748,   400,   376,   395,     0,   602,
     0,     0,   702,     0,   377,     0,     0,     0,     0,   467,
   489,   481,   485,   482,     0,     0,   474,     0,     0,     0,
     0,     0,     0,   300,   473,     0,   299,     0,     0,     0,
     0,    41,   234,    50,     0,     0,     0,     0,    47,   241,
     0,     0,   316,     0,    40,   233,    36,    35,     0,   320,
     0,   104,     0,     0,     0,     0,     0,     0,     0,   151,
     0,     0,   337,     0,   404,     0,     0,   362,   406,    71,
   405,   363,     0,     0,     0,     0,     0,     0,   498,     0,
     0,   397,     0,     0,     0,   161,   774,     0,     0,     0,
     0,     0,   319,   309,     0,     0,     0,   240,   342,   292,
   108,     0,     0,   464,   461,   644,   648,   649,   650,   651,
   641,   621,   623,   630,   626,   632,     0,   422,     0,   733,
     0,   707,   735,     0,     0,     0,     0,   755,     0,   744,
   763,     0,     0,     0,   732,   750,   396,   398,    13,   609,
    11,     0,     0,     0,   604,   605,     0,     0,   589,   588,
   590,   591,   593,   592,   594,   596,   600,     0,     0,     0,
   535,     0,     0,     0,   581,   582,   583,   584,   586,   585,
   587,   580,   595,     0,   513,     0,   517,   520,     0,   575,
   576,     0,     0,     0,     0,     0,     0,     0,    83,     0,
   797,     0,     0,    81,    76,     0,     0,     0,     0,     0,
     0,   470,   471,     0,     0,     0,     0,     0,     0,     0,
   480,   375,   399,     0,   407,   409,     0,     0,   767,    68,
     0,     0,   499,   368,     0,   367,     0,   491,     0,     0,
     0,     0,     0,     0,     0,     0,   297,   353,     0,     0,
   645,   414,   416,     0,     0,     0,   711,     0,   713,   698,
     0,   719,     0,   716,   703,   721,     0,   608,     0,     0,
   607,     0,     0,   578,   579,   148,   598,     0,     0,     0,
     0,     0,     0,   544,     0,     0,   531,   534,     0,     0,
   597,     0,    63,     0,     0,     0,     0,     0,     0,     0,
     0,    59,     0,     0,     0,   449,   448,     0,    46,   238,
    45,   239,     0,    43,   236,    44,   237,     0,    49,     0,
     0,     0,     0,     0,     0,     0,     0,    37,     0,   693,
   371,   360,   410,     0,   369,     0,   365,   492,     0,     0,
   361,     0,     0,     0,     0,     0,   462,   646,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   606,     0,     0,   550,   530,   529,     0,     0,     0,   545,
     0,   798,   563,   633,     0,     0,   559,   568,   569,   558,
     0,     0,   514,   516,   571,   572,   599,   528,   524,   633,
     0,     0,     0,     0,     0,     0,   447,     0,   752,     0,
     0,     0,   741,     0,     0,   429,     0,     0,     0,   490,
   486,    42,   235,     0,     0,   373,     0,     0,     0,     0,
   510,   511,   512,     0,   493,     0,   463,     0,     0,     0,
     0,     0,   712,     0,   709,   714,   717,    12,     0,     0,
     0,     0,     0,   536,     0,     0,   546,     0,   552,     0,
   533,     0,     0,   567,   565,     0,   522,   521,   523,   526,
   525,   527,     0,   443,     0,   440,   438,     0,     0,   427,
   450,     0,   445,     0,     0,     0,     0,     0,   428,     0,
   500,     0,     0,     0,   494,   496,   497,   495,   456,     0,
   454,   457,   466,   465,     0,     0,     0,     0,     0,     0,
     0,   601,     0,     0,     0,   564,   560,   555,   430,   753,
     0,     0,     0,     0,   451,   742,     0,     0,   345,     0,
     0,   408,     0,   505,   506,   509,     0,     0,   453,   647,
   710,     0,     0,     0,   553,   549,     0,   444,     0,   441,
     0,   435,     0,   437,   426,   446,     0,     0,     0,     0,
   455,     0,     0,     0,     0,   502,   503,   501,     0,   442,
   436,     0,   433,   439,     0,   434,
    }, yyDgoto = {
//yyDgoto 268
     1,   437,    69,    70,    71,    72,    73,   314,   318,   319,
   543,    74,    75,   551,    76,    77,   549,   550,   552,   743,
    78,    79,    80,    81,    82,    83,   419,   438,    84,    85,
    86,    87,    88,   253,   588,   589,   272,   273,   274,    90,
    91,    92,    93,    94,    95,   426,   441,   229,   261,   262,
    98,   993,   994,   862,  1008,  1267,   778,   923,  1039,   918,
   641,   491,   492,   637,   804,   901,   759,  1288,  1243,   241,
   281,   478,   233,    99,   234,   824,   825,   101,   826,   830,
   670,   102,   103,  1177,  1178,   329,   330,   331,   568,   592,
   570,   571,   752,   753,   754,   755,   285,   493,   236,   201,
   237,   890,   459,  1180,  1074,  1075,   572,   573,   574,  1181,
  1182,  1269,  1112,  1270,   105,   884,  1116,   631,   629,   391,
   650,   378,   238,   275,   202,   108,   109,   110,   111,   112,
   532,   277,   859,  1337,  1197,  1198,  1147,   954,   955,   956,
  1050,  1051,  1052,  1053,  1227,  1054,   957,   958,   959,   960,
   961,  1154,  1155,  1156,   315,   113,   723,   639,   422,   640,
   204,   575,   576,   763,   577,   578,   579,   580,   914,   673,
   396,   205,   376,   682,  1055,  1157,  1158,  1159,  1160,   582,
   583,   495,  1246,   654,   585,   586,   587,   486,   799,   479,
   263,   115,   116,   996,   863,   117,   118,   383,   379,   780,
   921,   997,  1057,   119,   776,   120,   121,   122,   123,   124,
  1069,  1070,     2,   338,   464,  1036,   512,   502,   487,   618,
   787,  1071,   604,   786,  1062,   846,   442,   849,   694,   504,
   522,   242,   424,   278,   279,   729,   718,   677,   860,   692,
   836,   678,   834,   674,   310,   538,   738,  1019,   632,   792,
   965,   630,   790,   964,  1002,  1105,  1302,  1059,  1047,   740,
   739,   885,  1020,  1117,  1210,   835,   333,   679,
    }, yySindex = {
//yySindex 1346
     0,     0, 22556, 23159,     0,     0,     0, 26142,     0,     0,
     0,     0,     0,     0,     0, 20914, 20914,     0,     0,     0,
    77,   143,     0,     0,     0,     0,   518, 26039,   215,   222,
   244,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  1155, 25135, 25135,
 25135, 25135,   -15, 22668,     0, 23876, 24216, 26732,     0, 26562,
     0,     0,     0,   351,   395,   409,   411, 25249, 25135,     0,
   110,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   416,   416,     0,   134,     0,  1586,
   792, 21644,     0,   207,     0,   -26,    40,    49,   153,     0,
   164,     0,   103,     0,   177,     0,   490,     0,   507, 28803,
     0,   522,     0,     0, 20914, 28914, 29136,     0, 25364, 26462,
     0,     0, 29025, 22327, 25364,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   607,     0,     0,     0,     0,     0,     0,     0,     0,
   531,     0,     0,     0,     0,     0,     0,     0,     0, 25135,
   318, 22786, 25135, 25135, 25135,     0, 25135, 27817,     0,   304,
   130,   616,     0,     0,   340,   621,     0,   379,   685,     0,
 22216,     0, 20914, 23270,     0,     0, 22439,     0, 25364,   929,
     0,   729, 22556,     0, 22786,   457,     0,     0,    77,   143,
     0,   363,    49,     0,   472,  8952,  8952,   464, 23382,     0,
 22668,   780,   134,     0,  1586,     0,     0,   215,   215, 23382,
   538,   739,   657,     0,   304,   752,   657,     0,     0,     0,
     0,     0,   215,     0,     0,     0,     0,     0,     0,     0,
     0,  1155,   570, 29247,   416,   416,     0,   558,     0,   840,
     0,     0,     0,     0,   576,     0,     0,   862,  1456,   932,
     0,     0,     0,     0,     0,     0,     0,     0,  5709, 22786,
   831,     0,     0,  6018, 22786,   881,     0,     0,     0, 22929,
     0, 25364, 25364, 25364, 25364, 23382, 25364, 25364,     0, 25135,
 25135, 25135, 25135, 25135,     0,     0, 25135, 25135, 25135, 25135,
 25135, 25135, 25135, 25135,     0, 25135,     0,     0, 25135, 25135,
 25135, 25135, 25135, 25135, 25135, 25135, 25135,     0,     0,     0,
     0,     0,     0,     0,     0, 27064, 20914,     0, 27307,     0,
   563,     0, 25135,   602,     0,     0, 28475,   602,   602,   602,
 22668, 26969,   926,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0, 25364,   554,   914,
   591,     0,     0,     0, 22786,   792,   167,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,    48,     0,     0,
     0, 22786, 25364, 22786,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   682,   652,     0,   716,
     0,     0,   134,     0,   943,   167,     0,     0,   464,     0,
     0,     0,  1176, 25135, 27385, 20914, 27424, 23518,     0,     0,
   602, 23987,     0,   602,   602,   215,     0,   970,     0, 25135,
   980,     0,   215,   986,     0,   215,   174,     0,     0,     0,
     0,     0, 26142,     0, 25135,   909,   918, 25135, 27385, 27424,
   602,  1586,   222,   215,     0,     0, 23047,     0,     0,  1586,
 24098,     0,     0,     0, 24216,     0,     0,     0,   729,     0,
     0,     0,  1005,     0, 27463, 20914, 27502, 29247,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  1558,
   258,  1575,   297,     0,     0,     0,     0,     0,     0,     0,
     0,  2045,     0,     0,     0,     0,     0,     0,   215,  1018,
     0,  1025,     0,  1055,     0,  1059,     0,     0,     0,     0,
 25135,  1046,     0,  1076,   215,   758,   819,  -125,   875,   883,
 25135,   134,  1114,   875, 25135,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   472,  6610,  6610,  6610,  6610,
 18032,  9457,  6610,  6610,  8952,  8952,   200,   200,     0, 20806,
  2914,  2914,  2599,   124,   124,   472,   472,   472,  2710,   875,
     0,  1037,     0,   875,   818,     0,   820,     0,   143,     0,
     0,  1129,   215,   830,     0,   836,     0,   143,     0,     0,
     0,  2710,     0,     0, 25249,     0,     0,   143, 25249, 24329,
 24329,   215, 29247,  1145,     0,   792,     0,     0,     0,     0,
     0, 27562, 20914, 27882, 22786,   875,     0, 22786,   931, 25364,
     0,     0,     0,   875,    96,   875,     0, 27921, 20914, 27960,
     0,   927,   939, 22786, 26142, 25135,     0,   869,   907,   215,
   912,   917, 25135,   304,     0,   621, 25135,     0, 25135, 25135,
     0,     0, 23629,     0, 23987,     0,     0,     0, 25364, 27817,
     0,     0,   472,   143,   143, 25135,     0,     0,     0,   215,
   657, 29247,     0,     0,   215,     0,     0,  1005,  6010,     0,
  1786,     0,     0,   135,  1183,     0,     0,   154,  1197,     0,
  2045,  1912,     0,  1223,   215,  1260,     0,     0,  5709,     0,
  6018,     0,    67,     0,  3035,     0,     0, 25135,    46,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   744,     0,
 24445, 21947,     0,  6018,     0, 28510, 26331, 26331,  1229,     0,
     0,     0,     0,     0,   602,   602,     0,   563, 23518,   950,
  1213,   602,   602,     0,     0,   563,     0,  1185, 28551,  1015,
   731,     0,     0,     0,   177,  1256,   -26,   207,     0,     0,
 25135, 28551,     0,  1280,     0,     0,     0,     0,     0,     0,
  1030,     0,  1005, 29247,   134, 25364, 22786,     0,     0,     0,
   215,   875,     0,   886,     0,   174, 26877,     0,     0,     0,
     0,     0,     0,     0,   215,     0,     0, 22786,     0,   875,
   939,     0,   875, 24559,  1054,     0,     0,   602,   602,   983,
   602,   602,     0,     0,  1294,   215,   174,     0,     0,     0,
     0,     0,  6018,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   215,     0,  2045,     0,
  1684,     0,     0,  1297,  1298,   215,  1302,     0,  1306,     0,
     0,  1076,  1060,  1302,     0,     0,     0,     0,     0,     0,
     0, 22786,     0,  1028,     0,     0, 25135, 25135,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  3660,  3660,   667,
     0, 20032,   215,  1061,     0,     0,     0,     0,     0,     0,
     0,     0,     0,  1029,     0,  1228,     0,     0,   849,     0,
     0,    20,  1029, 25135,  1258,  1258, 25249, 25249,     0,   602,
     0, 25249, 25249,     0,     0, 25135, 23382, 27999, 20914, 28038,
   602,     0,     0,     0, 24675, 23382,  1005, 22786,   134,   875,
     0,     0,     0,   875,     0,     0, 22786, 25364,     0,     0,
     0,   875,     0,     0,   875,     0, 25135,     0,   179,   875,
 25135, 25135,   602, 25135, 25135, 23987,     0,     0,   215,  -136,
     0,     0,     0,  1310,  1340,  6018,     0,  3035,     0,     0,
  3035,     0,  3035,     0,     0,     0, 22786,     0, 29358,   167,
     0, 27817, 27817,     0,     0,     0,     0, 26331,  1079,  1029,
   215,   215, 28611,     0,  1343,  1362,     0,     0,  1288,   865,
     0,  1109,     0, 26331,  3660,  3660,   667,   215,   215, 28334,
 28334,     0, 27817,  3054, 22786,     0,     0, 22786,     0,     0,
     0,     0, 25249,     0,     0,     0,     0, 27817,     0,   818,
   820,   215,   830,   836, 25249, 25135,     0,     0,   875,     0,
     0,     0,     0,   167,     0, 28334,     0,     0, 24789, 22786,
     0, 25135,  1375,  1360, 22786, 22786,     0,     0,  1684,  1684,
  1302,  1381,  1302,  1302,  1163,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  1117,   877,     0,
     0, 22786,    66,     0,     0,     0,    94,  1029,  1385,     0,
 26331,     0,     0,     0,   215,  1386,     0,     0,     0,     0,
 26331,   584,     0,     0,     0,     0,     0,     0,     0,     0,
   215,   215,   215,   215,   215,   215,     0,  1391,     0,  1395,
   215,  1396,     0,  1318,  1402,     0, 29469,  1387,  1076,     0,
     0,     0,     0,   950,     0,     0, 22786,   167,   978,     7,
     0,     0,     0, 25135,     0,  -112,     0,  2132,   875,  1326,
 22786,  1340,     0,  3035,     0,     0,     0,     0,     0, 28077,
 20914, 28397,   883,     0,  1410, 26331,     0,  1416,     0,   745,
     0,   865,  1029,     0,     0,  1279,     0,     0,     0,     0,
     0,     0,  6018,     0,  6018,     0,     0,  1329,    67,     0,
     0,  3035,     0,     0,     0,  1151,   897, 29469,     0,   886,
     0, 25364, 25364, 28692,     0,     0,     0,     0,     0,   718,
     0,     0,     0,     0,  1105,  1302,     0,     0,   215,     0,
     0,     0, 26331,  1432, 28692,     0,     0,     0,     0,     0,
  1440,  1448,  1449,  1457,     0,     0,  1076,  1440,     0, 28436,
   897,     0, 22786,     0,     0,     0,  1458,  2132,     0,     0,
     0,     0,  1432, 26331,     0,     0,  3035,     0,  3035,     0,
  6018,     0,  3035,     0,     0,     0,     0,     0,   516, 26331,
     0,  1440,  1440,  1460,  1440,     0,     0,     0,  1432,     0,
     0,  3035,     0,     0,  1440,     0,
    }, yyRindex = {
//yyRindex 1346
     0,     0,   420,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0, 16874, 16969,     0,     0,     0,
 14045, 13560, 17563, 17876, 17966, 18058, 25478,     0, 24906,     0,
     0, 18371, 18461, 18553, 14157,  4474, 18866, 18956, 14522, 19048,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   239, 22104,  1413,  1380,   965,     0,  1585,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  7966,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,  2684,  2684,     0,   188,     0,   165,
  1742, 17229,  8280, 10248,     0,  8375,     0, 23740, 10302,     0,
     0,     0, 11136,     0, 19361,     0,     0,     0,     0,  1077,
     0,     0,     0,     0, 17067,     0,     0,     0,     0,     0,
     0,     0,     0,  1241,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  1906,     0,     0,     0,     0,  5745,  5840,  5938,  6252,
     0,  6347,  6445,  6759,  3390,  6854,  6952,  3510,  7266,     0,
     0,   239,     0,     0,     0,     0,     0, 13140,     0,  3176,
     0,  1126,     0,     0,     0,  1126,     0,  7773,     0,     0,
  1476,     0,     0,   314,     0,     0,  1476,     0,     0,     0,
     0, 25593,   194,     0,   194,  8787,     0,     0,  8473,  7361,
     0,     0,     0,     0,  9292,  2875, 12497, 19451,     0,     0,
   239,     0,  1793,     0,  1813,     0,   748,  1476,  1476,     0,
  1427,     0,  1427,     0,     0,     0,  1403,     0,  1519,  1540,
  1850,  2325,  1490,  2373,  2835,  3551,  1230,  4033,  4515,  2389,
  4997,     0,     0,     0,  5887,  5887,     0,     0,  5455,  1080,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   540,  1697,
     0, 21470,     0,   547,  1697,     0,     0,     0,     0,   191,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  8882,  8978,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   230,     0,     0,     0,
  3025,     0,     0, 25707,     0,     0,     0, 25707, 25019, 25019,
   239,  1096,  1254,     0,     0,     0,     0,     0,     0,     0,
     0,     0, 20203,     0,     0, 20317,     0,     0,     0, 21582,
     0,     0,     0,     0,  1697,  1849,     0,  1481,  2268,  3272,
  5503, 11056, 13175, 16616, 17231, 20391,     0,     0,     0,     0,
     0,   150,     0,   150,   465,   867,   947,  1002,  1270,  1880,
  2254,  1957,  2360,  2470,  5869,  2519,     0,     0,  2815,     0,
     0,     0,    10,     0,   489,     0,     0,     0,  7868,     0,
     0,     0,     0,     0,     0,   230,     0,     0,     0,     0,
 25707,     0,     0, 25707, 25707,  1476,     0,     0,     0,   557,
   664,     0,  1476,   317,     0,  1476,  1476,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
 25707,  1894,     0,  1476,     0,     0,  2025,   158,     0,   817,
  1441,     0,     0,     0,   346,     0,     0,     0,     0,     0,
  5474,     0,  1404,     0,     0,   230,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  1476,   928,
     0,   312,     0,   595,     0,   312,     0,     0,     0,     0,
   218,   292,     0,   595,  1476,     0,   826,  1195,     0,  1020,
     0,     0,   312,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  9387, 11685, 11781, 11892, 11988,
 12083, 12386, 12195, 12290, 12593, 12688, 10794, 10889,     0,  1494,
 11234, 11330, 10985, 10397, 10493,  9483,  9797,  9892, 11425,     0,
     0,     0,     0,     0, 14634,  4839, 16065,     0, 23740,     0,
  4956,   315,  1446, 14999,     0, 15111,     0, 13680,     0,     0,
     0, 11590,     0,     0,     0,     0,     0, 16542,     0,     0,
     0,  1476,     0,  1418,     0,   671,     0, 21071,     0,     0,
     0,     0,   230,     0,  1697,     0,     0,   574, 21183,     0,
     0,     0,     0,     0,     0,     0,  2876,     0,   230,     0,
     0,  1290,     0,   189,     0,     0,     0,  3875,  5321,  1446,
  3992,  4357,     0,  8689,     0,  1126,     0,     0,     0,     0,
     0,     0,   930,     0,   663,     0,     0,     0,     0, 10613,
     0,     0,  9988,     0,  7459,     0,     0,   920,     0,  1476,
  1427,     0,  2448,   815,  1446,  2807,  5362,  1422,  -135,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   878,     0,   976,  1476,  1048,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  1295,     0,
    64, 13179,     0,     0,     0, 13290,     0,     0,     0,     0,
     0,     0,     0,     0, 25707, 25707,     0,  9602,   476, 17381,
     0, 25707, 25707,     0,     0, 10107,     0,     0, 13410,  2272,
     0,     0,     0,     0, 19543,     0,  9097, 12535,     0,     0,
     0,  1431,     0,     0,     0,     0,     0,     0,  1720,     0,
  3232,     0,  1533,     0,     0,     0,  1697, 20649, 20761,     0,
  1446,     0,     0,  1295,     0,  1476,     0,     0,     0,     0,
     0,     0,  5880,   172,  1446,  6101,  6214,   150,     0,     0,
     0,     0,     0,     0,  1295,     0,     0, 25707, 25707,  5431,
 25707, 25707,     0,     0,   317,  1476,  1476,     0,     0,     0,
     0,   740,   861,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,  1476,     0,     0,     0,
     0,     0,     0,   312,   312,  1476,   312,     0,   312,     0,
     0,   595,  1195,   312,     0,     0,     0,     0,     0,     0,
     0,   150,   114,   505,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  1446,   -21,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0, 19914,     0, 19854,     0,     0, 10152,     0,
     0, 19641, 19956,     0, 21791, 21903,     0,     0,     0, 25707,
     0,     0,     0,     0,     0,     0,     0,     0,   230,     0,
 25707,     0,     0,  9142,     0,     0,  1537,  1697,     0,     0,
     0,     0,     0,     0,     0,     0,   150,     0,     0,     0,
  1412,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0, 25707,     0,     0,   864,     0,     0,   893,     0,
     0,     0,     0,  1078,  1094,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   637,     0,     0,     0,
     0, 12728, 12838,     0,     0,     0,     0,     0,  6338,  6373,
  1446,  1446,  6458,     0,     0,  6471,     0,     0,     0,     0,
     0,     0,     0,     0, 19731, 19792,     0, 25856, 21269,     0,
     0,     0, 12988,   131,  1697,     0,     0,   194,     0,     0,
     0,     0,     0,     0,     0,     0,     0, 13030,     0, 15476,
 16430,  1446, 15588, 15953,     0,     0,  9647,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   549,
     0,     0,     0,   461,  1697,   194,     0,     0,     0,     0,
   312,   312,   312,   312,  1295,  2331,  2378,  3009,  3153,  3193,
  3195,  3293,  1088,  5449,  5451,  1498,  5477,     0,     0,  5817,
     0,  1697,  1476,     0,     0,     0,  6661,  6392,  6785,     0,
     0,     0,     0,     0,  1437,  5834,     0,     0,     0,     0,
  1581,  6468,     0,     0,     0,     0,     0,     0,     0,     0,
  1476,  1476,  1476,  1446,  1446,  1446,     0,    93,     0,    93,
   131,   120,     0,     0,    93,     0,   977,  1064,   120,     0,
     0,     0,     0, 17471, 20006,     0,   574,     0,   520,   835,
     0,     0,     0,     0,     0,  1295,     0,     0,     0,     0,
    19,  1101,     0,     0,     0,     0,     0,     0,  5845,     0,
   230,     0,  1020,     0,  6815,     0,     0,  6845,     0,     0,
     0,  6563,  5945,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   597,     0,     0,     0,     0,     0,
     0,     0,     0,  1807,  2063,     0,  1072,     0,     0,  1295,
     0,     0,     0,  1143,     0,     0,     0,     0,     0,   488,
     0,     0,     0,     0,     0,   312,  2782,  1939,  1446,  3179,
  3265,     0,     0,  6865,     0,     0,     0,     0,     0,     0,
    93,    93,    93,    93,     0,     0,   120,    93,     0,     0,
  1320,     0,   527,     0,     0,     0,  1259,     0,     0,     0,
     0,   889,  6880,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,  2498,  1622,  1295,     0,
     0,    93,    93,    93,    93,     0,     0,     0,  1312,     0,
     0,     0,     0,     0,    93,     0,
    }, yyGindex = {
//yyGindex 268
     0,     0,   548,     0,  1497,  1186,  1200,   -56,     0,     0,
  -113,  1615,  1645,     0,  1761,  1901,     0,     0,     0,  1013,
  1952,     0,    45,     0,     0,     5,  1455,   756,     4,     6,
  1333,     0,    63,  1097,  -326,   147,     0,  1074,    33,   -68,
  2411,   195,   -13,   -57,     0,  -110,   -91,   856,    18,   373,
     0,   349,  -825,  -815,     0,     0,   392,     0,     0,   495,
    25,   759,  -385,    56,   963,  -306,  -368,   551,  2714,   104,
     0,  -210,  -386,  1507,  1534,   -12,  -164,  -138,  -558,     0,
     0,     0,     0,   383, -1124,    -6,  1543,  1003,  -322,  -146,
  -690,  -148,  -811,  -812,   887,   735,     0,    54,  -443,     0,
  1409,     0,     0,     0,   673,     0,  -695,     0,   879,     0,
   391,     0,  -966,   335,  2046,     0,     0,   996,  1263,   -76,
   -34,   833,  1243,    -2,    -3,  1525,     0,    -1,   -92,    -9,
  -464,  -154,   324,     0,     0,     0,  -641,     0,     0,   592,
  -813,  -970,     0,   555,  -525,   455,     0,    85,   603,     0,
     0,  -412,     0,   425,     0,     0,  -339,     0,  -387,    37,
   -52,  -573,  -425,  -557,  -556, -1056,  -734,  2638,  -291,   -87,
     0,     0,  1567,     0,  -335,     0,     0,   440,     0,     0,
  3122,  -281,     0,   802,     0,     0,  -648,   487,   199,     0,
  1057,     0,     0,   900,     0,     0,     0,     0,     0,     0,
     0,     0,   528,     0,  -521,     0,     0,     0,     0,     0,
     0,     0,     0,   -80,    42,     0,     0,     0,   -10,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  -224,     0,     0,     0,     0,     0,     0,  -459,     0,     0,
     0,   -64,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,
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
    "$$28 :",
    "$$29 :",
    "lambda : tLAMBDA $$26 $$27 $$28 f_larglist $$29 lambda_body",
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
    "$$30 :",
    "$$31 :",
    "$$32 :",
    "brace_body : $$30 $$31 $$32 opt_block_param compstmt",
    "$$33 :",
    "$$34 :",
    "$$35 :",
    "do_body : $$33 $$34 $$35 opt_block_param bodystmt",
    "case_args : arg_value",
    "case_args : tSTAR arg_value",
    "case_args : case_args ',' arg_value",
    "case_args : case_args ',' tSTAR arg_value",
    "case_body : k_when case_args then compstmt cases",
    "cases : opt_else",
    "cases : case_body",
    "$$36 :",
    "$$37 :",
    "$$38 :",
    "p_case_body : keyword_in $$36 $$37 p_top_expr then $$38 compstmt p_cases",
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
    "$$39 :",
    "p_expr_basic : tLBRACE $$39 p_kwargs rbrace",
    "p_expr_basic : tLBRACE rbrace",
    "$$40 :",
    "p_expr_basic : tLPAREN $$40 p_expr rparen",
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
    "$$41 :",
    "string_content : tSTRING_DVAR $$41 string_dvar",
    "$$42 :",
    "$$43 :",
    "$$44 :",
    "$$45 :",
    "$$46 :",
    "string_content : tSTRING_DBEG $$42 $$43 $$44 $$45 $$46 compstmt tSTRING_DEND",
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
    "$$47 :",
    "superclass : '<' $$47 expr_value term",
    "superclass :",
    "f_opt_paren_args : f_paren_args",
    "f_opt_paren_args : none",
    "f_paren_args : '(' f_args rparen",
    "f_paren_args : '(' f_arg ',' args_forward rparen",
    "f_paren_args : '(' args_forward rparen",
    "f_arglist : f_paren_args",
    "$$48 :",
    "f_arglist : $$48 f_args term",
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
    "$$49 :",
    "singleton : '(' $$49 expr rparen",
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

static ParserState[] states = new ParserState[808];
static {
states[1] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                  lexer.setState(EXPR_BEG);
                  support.initTopLocalVariables();
  return yyVal;
};
states[2] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
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
                  yyVal = support.remove_begin(((Node)yyVals[0+yyTop].value));
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
                    yyVal = support.new_if(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.cond(((Node)yyVals[0+yyTop].value)), support.remove_begin(((Node)yyVals[-2+yyTop].value)), null);
                    support.fixpos(((Node)yyVal), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[29] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_if(support.getPosition(((Node)yyVals[-2+yyTop].value)), support.cond(((Node)yyVals[0+yyTop].value)), null, support.remove_begin(((Node)yyVals[-2+yyTop].value)));
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
                    support.value_expr(lexer, ((Node)yyVals[-2+yyTop].value));
                    yyVal = node_assign(((MultipleAsgnNode)yyVals[-5+yyTop].value), support.newRescueModNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value)));
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
                    support.pop_pvtbl(((Set)yyVals[-1+yyTop].value));
  return yyVal;
};
states[59] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    LexContext ctxt = lexer.getLexContext();
                    ctxt.in_kwarg = ((LexContext)yyVals[-3+yyTop].value).in_kwarg;
                    yyVal = support.newPatternCaseNode(((Node)yyVals[-5+yyTop].value).getLine(), ((Node)yyVals[-5+yyTop].value), support.newIn(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-1+yyTop].value), null, null));
                    support.warn_one_line_pattern_matching(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-1+yyTop].value), true);
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
                    support.pop_pvtbl(((Set)yyVals[-1+yyTop].value));
  return yyVal;
};
states[63] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    LexContext ctxt = lexer.getLexContext();
                    ctxt.in_kwarg = ((LexContext)yyVals[-3+yyTop].value).in_kwarg;
                    yyVal = support.newPatternCaseNode(((Node)yyVals[-5+yyTop].value).getLine(), ((Node)yyVals[-5+yyTop].value), support.newIn(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-1+yyTop].value), new TrueNode(lexer.tokline), new FalseNode(lexer.tokline)));
                    support.warn_one_line_pattern_matching(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-1+yyTop].value), false);
  return yyVal;
};
states[65] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.pushLocalScope();
                    LexContext ctxt = lexer.getLexContext();
                    RubySymbol name = support.symbolID(((ByteList)yyVals[0+yyTop].value));
                    support.numparam_name(name);
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
                    yyVal = node_assign(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
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
                    yyVal = new DefnNode(((DefHolder)yyVals[-3+yyTop].value).line, ((DefHolder)yyVals[-3+yyTop].value).name, ((ArgsNode)yyVals[-2+yyTop].value), support.getCurrentScope(), support.reduce_nodes(support.remove_begin(((Node)yyVals[0+yyTop].value))), yyVals[yyTop - count + 4].end);
                    if (support.isNextBreak) ((DefnNode)yyVal).setContainsNextBreak();
                    support.popCurrentScope();
  return yyVal;
};
states[277] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.endless_method_name(((DefHolder)yyVals[-5+yyTop].value));
                    support.restore_defun(((DefHolder)yyVals[-5+yyTop].value));
                    Node body = support.reduce_nodes(support.remove_begin(support.rescued_expr(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value))));
                    yyVal = new DefnNode(((DefHolder)yyVals[-5+yyTop].value).line, ((DefHolder)yyVals[-5+yyTop].value).name, ((ArgsNode)yyVals[-4+yyTop].value), support.getCurrentScope(), body, yyVals[yyTop - count + 6].end);
                    if (support.isNextBreak) ((DefnNode)yyVal).setContainsNextBreak();
                    support.popCurrentScope();
  return yyVal;
};
states[278] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.endless_method_name(((DefHolder)yyVals[-3+yyTop].value));
                    support.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
                    yyVal = new DefsNode(((DefHolder)yyVals[-3+yyTop].value).line, ((DefHolder)yyVals[-3+yyTop].value).singleton, ((DefHolder)yyVals[-3+yyTop].value).name, ((ArgsNode)yyVals[-2+yyTop].value), support.getCurrentScope(), support.reduce_nodes(support.remove_begin(((Node)yyVals[0+yyTop].value))), yyVals[yyTop - count + 4].end);
                    if (support.isNextBreak) ((DefsNode)yyVal).setContainsNextBreak();
                    support.popCurrentScope();
  return yyVal;
};
states[279] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.endless_method_name(((DefHolder)yyVals[-5+yyTop].value));
                    support.restore_defun(((DefHolder)yyVals[-5+yyTop].value));
                    Node body = support.reduce_nodes(support.remove_begin(support.rescued_expr(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value))));
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
                    yyVal = support.newPatternCaseNode(((Integer)yyVals[-4+yyTop].value), ((Node)yyVals[-3+yyTop].value), ((InNode)yyVals[-1+yyTop].value));
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
                    Node body = support.reduce_nodes(support.remove_begin(support.makeNullNil(((Node)yyVals[-1+yyTop].value))));
                    yyVal = new DefnNode(((DefHolder)yyVals[-3+yyTop].value).line, ((DefHolder)yyVals[-3+yyTop].value).name, ((ArgsNode)yyVals[-2+yyTop].value), support.getCurrentScope(), body, yyVals[yyTop - count + 4].end);
                    if (support.isNextBreak) ((DefnNode)yyVal).setContainsNextBreak();                    support.popCurrentScope();
  return yyVal;
};
states[377] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.restore_defun(((DefHolder)yyVals[-3+yyTop].value));
                    Node body = support.reduce_nodes(support.remove_begin(support.makeNullNil(((Node)yyVals[-1+yyTop].value))));
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
                    yyVal = ((Node)yyVals[0+yyTop].value) == null ? NilImplicitNode.NIL : ((Node)yyVals[0+yyTop].value);
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
                    support.ordinalMaxNumParam();
                    yyVal = support.new_args(lexer.getRubySourceline(), null, null, null, null, (ArgsTailHolder) null);
  return yyVal;
};
states[451] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setCurrentArg(null);
                    support.ordinalMaxNumParam();
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
                    yyVal = support.resetMaxNumParam();
  return yyVal;
};
states[460] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.numparam_push();
  return yyVal;
};
states[461] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = Long.valueOf(lexer.getCmdArgumentState().getStack());
                    lexer.getCmdArgumentState().reset();
  return yyVal;
};
states[462] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    int max_numparam = support.restoreMaxNumParam(((Integer)yyVals[-4+yyTop].value));
                    ArgsNode args = support.args_with_numbered(((ArgsNode)yyVals[-2+yyTop].value), max_numparam);
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop].value).longValue());
                    lexer.getCmdArgumentState().restart();
                    yyVal = new LambdaNode(yyVals[yyTop - count + 1].startLine(), args, ((Node)yyVals[0+yyTop].value), support.getCurrentScope(), lexer.getRubySourceline());
                    lexer.setLeftParenBegin(((Integer)yyVals[-5+yyTop].value));
                    support.numparam_pop(((Node)yyVals[-3+yyTop].value));
                    support.popCurrentScope();
  return yyVal;
};
states[463] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ArgsNode)yyVals[-2+yyTop].value);
                    support.ordinalMaxNumParam();
  return yyVal;
};
states[464] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (!support.isArgsInfoEmpty(((ArgsNode)yyVals[0+yyTop].value))) {
                        support.ordinalMaxNumParam();
                    }
                    yyVal = ((ArgsNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[465] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[466] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[467] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((IterNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[468] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
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
states[469] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].startLine());
  return yyVal;
};
states[470] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value), yyVals[yyTop - count + 3].startLine());
  return yyVal;
};
states[471] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop].value), ((ByteList)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((IterNode)yyVals[0+yyTop].value), yyVals[yyTop - count + 3].startLine());
  return yyVal;
};
states[472] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.frobnicate_fcall_args(((FCallNode)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
                    yyVal = ((FCallNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[473] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].startLine());
  return yyVal;
};
states[474] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[475] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value), null, null);
  return yyVal;
};
states[476] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), LexingCommon.CALL, ((Node)yyVals[0+yyTop].value), null, yyVals[yyTop - count + 3].startLine());
  return yyVal;
};
states[477] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop].value), LexingCommon.CALL, ((Node)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[478] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_super(((Integer)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[479] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ZSuperNode(((Integer)yyVals[0+yyTop].value));
  return yyVal;
};
states[480] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (((Node)yyVals[-3+yyTop].value) instanceof SelfNode) {
                        yyVal = support.new_fcall(LexingCommon.LBRACKET_RBRACKET);
                        support.frobnicate_fcall_args(((FCallNode)yyVal), ((Node)yyVals[-1+yyTop].value), null);
                    } else {
                        yyVal = support.new_call(((Node)yyVals[-3+yyTop].value), lexer.LBRACKET_RBRACKET, ((Node)yyVals[-1+yyTop].value), null);
                    }
  return yyVal;
};
states[481] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((IterNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[482] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((IterNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[483] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.pushBlockScope();
                    yyVal = Long.valueOf(lexer.getCmdArgumentState().getStack()) >> 1;
                    lexer.getCmdArgumentState().reset();
  return yyVal;
};
states[484] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.resetMaxNumParam();
  return yyVal;
};
states[485] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.numparam_push();
  return yyVal;
};
states[486] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    int max_numparam = support.restoreMaxNumParam(((Integer)yyVals[-3+yyTop].value));
                    ArgsNode args = support.args_with_numbered(((ArgsNode)yyVals[-1+yyTop].value), max_numparam);
                    yyVal = new IterNode(yyVals[yyTop - count + 1].startLine(), args, ((Node)yyVals[0+yyTop].value), support.getCurrentScope(), lexer.getRubySourceline());
                    support.numparam_pop(((Node)yyVals[-2+yyTop].value));
                    support.popCurrentScope();
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-4+yyTop].value).longValue());
  return yyVal;
};
states[487] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.pushBlockScope();
                    yyVal = Long.valueOf(lexer.getCmdArgumentState().getStack());
                    lexer.getCmdArgumentState().reset();
  return yyVal;
};
states[488] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.resetMaxNumParam();
  return yyVal;
};
states[489] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.numparam_push();
  return yyVal;
};
states[490] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    int max_numparam = support.restoreMaxNumParam(((Integer)yyVals[-3+yyTop].value));
                    ArgsNode args = support.args_with_numbered(((ArgsNode)yyVals[-1+yyTop].value), max_numparam);
                    yyVal = new IterNode(yyVals[yyTop - count + 1].startLine(), args, ((Node)yyVals[0+yyTop].value), support.getCurrentScope(), lexer.getRubySourceline());
                    support.numparam_pop(((Node)yyVals[-2+yyTop].value));
                    support.popCurrentScope();
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-4+yyTop].value).longValue());
  return yyVal;
};
states[491] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     support.check_literal_when(((Node)yyVals[0+yyTop].value));
                     yyVal = support.newArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[492] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newSplatNode(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[493] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.check_literal_when(((Node)yyVals[0+yyTop].value));
                    yyVal = support.last_arg_append(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[494] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.rest_arg_append(((Node)yyVals[-3+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[495] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newWhenNode(((Integer)yyVals[-4+yyTop].value), ((Node)yyVals[-3+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[498] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setState(EXPR_BEG|EXPR_LABEL);
                    lexer.commandStart = false;
                    LexContext ctxt = (LexContext) lexer.getLexContext();
                    yyVals[0+yyTop].value = (LexContext) ctxt.clone();
                    ctxt.in_kwarg = true;
                    yyVal = support.push_pvtbl();
  return yyVal;
};
states[499] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.push_pktbl();
  return yyVal;
};
states[500] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    support.pop_pvtbl(((Set)yyVals[-3+yyTop].value));
                    lexer.getLexContext().in_kwarg = ((LexContext)yyVals[-4+yyTop].value).in_kwarg;
  return yyVal;
};
states[501] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newIn(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-4+yyTop].value), ((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[503] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((InNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[505] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_if(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[0+yyTop].value), support.remove_begin(((Node)yyVals[-2+yyTop].value)), null);
                    support.fixpos(((Node)yyVal), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[506] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_if(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[0+yyTop].value), null, support.remove_begin(((Node)yyVals[-2+yyTop].value)));
                    support.fixpos(((Node)yyVal), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[508] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), null, ((Node)yyVals[-1+yyTop].value),
                                                   support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), null, true, null, null));
  return yyVal;
};
states[509] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), null, ((Node)yyVals[-2+yyTop].value), ((ArrayPatternNode)yyVals[0+yyTop].value));
                   support.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].startLine());
  return yyVal;
};
states[510] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_find_pattern(null, ((FindPatternNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[511] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), null, null, ((ArrayPatternNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[512] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_hash_pattern(null, ((HashPatternNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[514] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new HashNode(yyVals[yyTop - count + 1].startLine(), new KeyValuePair(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[516] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newOrNode(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[518] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.push_pktbl();
  return yyVal;
};
states[519] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.push_pktbl();
  return yyVal;
};
states[521] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-3+yyTop].value), null, ((ArrayPatternNode)yyVals[-1+yyTop].value));
                    support.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].startLine());
  return yyVal;
};
states[522] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     support.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                     yyVal = support.new_find_pattern(((Node)yyVals[-3+yyTop].value), ((FindPatternNode)yyVals[-1+yyTop].value));
                     support.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].startLine());
  return yyVal;
};
states[523] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     support.pop_pktbl(((Set)yyVals[-2+yyTop].value));
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
                     support.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                     yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-3+yyTop].value), null, ((ArrayPatternNode)yyVals[-1+yyTop].value));
                     support.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].startLine());
  return yyVal;
};
states[526] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = support.new_find_pattern(((Node)yyVals[-3+yyTop].value), ((FindPatternNode)yyVals[-1+yyTop].value));
                    support.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].startLine());
  return yyVal;
};
states[527] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = support.new_hash_pattern(((Node)yyVals[-3+yyTop].value), ((HashPatternNode)yyVals[-1+yyTop].value));
                    support.nd_set_first_loc(((Node)yyVal), yyVals[yyTop - count + 1].startLine());
  return yyVal;
};
states[528] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), ((Node)yyVals[-2+yyTop].value), null,
                            support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), null, false, null, null));
  return yyVal;
};
states[529] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), null, null, ((ArrayPatternNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[530] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_find_pattern(null, ((FindPatternNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[531] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_array_pattern(yyVals[yyTop - count + 1].startLine(), null, null,
                            support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), null, false, null, null));
  return yyVal;
};
states[532] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.push_pktbl();
                    LexContext ctxt = lexer.getLexContext();
                    yyVals[0+yyTop].value = ctxt;
                    ctxt.in_kwarg = false;
  return yyVal;
};
states[533] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    lexer.getLexContext().in_kwarg = ((LexContext)yyVals[-3+yyTop].value).in_kwarg;
                    yyVal = support.new_hash_pattern(null, ((HashPatternNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[534] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_hash_pattern(null, support.new_hash_pattern_tail(yyVals[yyTop - count + 1].startLine(), null, null));
  return yyVal;
};
states[535] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.push_pktbl();
  return yyVal;
};
states[536] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.pop_pktbl(((Set)yyVals[-2+yyTop].value));
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[537] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     ListNode preArgs = support.newArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), preArgs, false, null, null);
  return yyVal;
};
states[538] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((ListNode)yyVals[0+yyTop].value), true, null, null);
  return yyVal;
};
states[539] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), support.list_concat(((ListNode)yyVals[-1+yyTop].value), ((ListNode)yyVals[0+yyTop].value)), false, null, null);
  return yyVal;
};
states[540] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((ListNode)yyVals[-2+yyTop].value), true, ((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[541] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((ListNode)yyVals[-4+yyTop].value), true, ((ByteList)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[542] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((ListNode)yyVals[-1+yyTop].value), true, null, null);
  return yyVal;
};
states[543] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((ListNode)yyVals[-3+yyTop].value), true, null, ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[544] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ArrayPatternNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[545] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ListNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[546] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.list_concat(((ListNode)yyVals[-2+yyTop].value), ((ListNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[547] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), null, true, ((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[548] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.new_array_pattern_tail(yyVals[yyTop - count + 1].startLine(), null, true, ((ByteList)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[549] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.new_find_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((ByteList)yyVals[-4+yyTop].value), ((ListNode)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
                     support.warn_experimental(yyVals[yyTop - count + 1].startLine(), "Find pattern is experimental, and the behavior may change in future versions of Ruby!");
  return yyVal;
};
states[550] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[551] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = null;
  return yyVal;
};
states[553] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.list_concat(((ListNode)yyVals[-2+yyTop].value), ((ListNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[554] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[555] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_hash_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((HashNode)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[556] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_hash_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((HashNode)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[557] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_hash_pattern_tail(yyVals[yyTop - count + 1].startLine(), ((HashNode)yyVals[-1+yyTop].value), null);
  return yyVal;
};
states[558] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_hash_pattern_tail(yyVals[yyTop - count + 1].startLine(), null, ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[559] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new HashNode(yyVals[yyTop - count + 1].start, ((KeyValuePair)yyVals[0+yyTop].value));
  return yyVal;
};
states[560] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    ((HashNode)yyVals[-2+yyTop].value).add(((KeyValuePair)yyVals[0+yyTop].value));
                    yyVal = ((HashNode)yyVals[-2+yyTop].value);
  return yyVal;
};
states[561] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.error_duplicate_pattern_key(((ByteList)yyVals[-1+yyTop].value));

                    Node label = support.asSymbol(yyVals[yyTop - count + 1].startLine(), ((ByteList)yyVals[-1+yyTop].value));

                    yyVal = new KeyValuePair(label, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[562] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.error_duplicate_pattern_key(((ByteList)yyVals[0+yyTop].value));
                    if (((ByteList)yyVals[0+yyTop].value) != null && !support.is_local_id(((ByteList)yyVals[0+yyTop].value))) {
                        support.yyerror("key must be valid as local variables");
                    }
                    support.error_duplicate_pattern_variable(((ByteList)yyVals[0+yyTop].value));

                    Node label = support.asSymbol(yyVals[yyTop - count + 1].startLine(), ((ByteList)yyVals[0+yyTop].value));
                    yyVal = new KeyValuePair(label, support.assignableLabelOrIdentifier(((ByteList)yyVals[0+yyTop].value), null));
  return yyVal;
};
states[564] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (((Node)yyVals[-1+yyTop].value) == null || ((Node)yyVals[-1+yyTop].value) instanceof StrNode) {
                        yyVal = ((StrNode)yyVals[-1+yyTop].value).getValue();
                    } else {
                        support.yyerror("symbol literal with interpolation is not allowed");
                        yyVal = null;
                    }
  return yyVal;
};
states[565] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[566] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = null;
  return yyVal;
};
states[567] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = null;
  return yyVal;
};
states[569] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.KWNOREST;
  return yyVal;
};
states[571] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[-2+yyTop].value));
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[-2+yyTop].value) instanceof FixnumNode && ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].startLine(), support.makeNullNil(((Node)yyVals[-2+yyTop].value)), support.makeNullNil(((Node)yyVals[0+yyTop].value)), false, isLiteral);
  return yyVal;
};
states[572] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[-2+yyTop].value));
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[-2+yyTop].value) instanceof FixnumNode && ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].startLine(), support.makeNullNil(((Node)yyVals[-2+yyTop].value)), support.makeNullNil(((Node)yyVals[0+yyTop].value)), true, isLiteral);
  return yyVal;
};
states[573] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[-1+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[-1+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].startLine(), support.makeNullNil(((Node)yyVals[-1+yyTop].value)), NilImplicitNode.NIL, false, isLiteral);
  return yyVal;
};
states[574] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[-1+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[-1+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].startLine(), support.makeNullNil(((Node)yyVals[-1+yyTop].value)), NilImplicitNode.NIL, true, isLiteral);
  return yyVal;
};
states[578] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].startLine(), NilImplicitNode.NIL, support.makeNullNil(((Node)yyVals[0+yyTop].value)), false, isLiteral);
  return yyVal;
};
states[579] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    boolean isLiteral = ((Node)yyVals[0+yyTop].value) instanceof FixnumNode;
                    yyVal = new DotNode(yyVals[yyTop - count + 1].startLine(), NilImplicitNode.NIL, support.makeNullNil(((Node)yyVals[0+yyTop].value)), true, isLiteral);
  return yyVal;
};
states[584] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[585] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[586] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[587] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[588] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new NilNode(lexer.tokline);
  return yyVal;
};
states[589] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new SelfNode(lexer.tokline);
  return yyVal;
};
states[590] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new TrueNode(lexer.tokline);
  return yyVal;
};
states[591] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new FalseNode(lexer.tokline);
  return yyVal;
};
states[592] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new FileNode(lexer.tokline, new ByteList(lexer.getFile().getBytes(),
                    support.getConfiguration().getRuntime().getEncodingService().getLocaleEncoding()));
  return yyVal;
};
states[593] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new FixnumNode(lexer.tokline, lexer.tokline+1);
  return yyVal;
};
states[594] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new EncodingNode(lexer.tokline, lexer.getEncoding());
  return yyVal;
};
states[595] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((LambdaNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[596] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.error_duplicate_pattern_variable(((ByteList)yyVals[0+yyTop].value));
                    yyVal = support.assignableInCurr(((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[597] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    Node n = support.declareIdentifier(((ByteList)yyVals[0+yyTop].value));
                    if (!(n instanceof LocalVarNode || n instanceof DVarNode)) {
                        support.compile_error("" + ((ByteList)yyVals[0+yyTop].value) + ": no such local variable");
                    }
                    yyVal = n;
  return yyVal;
};
states[598] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_colon3(lexer.tokline, ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[599] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_colon2(lexer.tokline, ((Node)yyVals[-2+yyTop].value), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[600] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ConstNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[601] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
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
states[602] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = null; 
  return yyVal;
};
states[603] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[604] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.splat_array(((Node)yyVals[0+yyTop].value));
                    if (yyVal == null) yyVal = ((Node)yyVals[0+yyTop].value); /* ArgsCat or ArgsPush*/
  return yyVal;
};
states[606] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[608] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[610] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((NumericNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[611] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[612] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
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
states[613] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((StrNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[614] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[615] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[616] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.heredoc_dedent(((Node)yyVals[-1+yyTop].value));
		    lexer.setHeredocIndent(0);
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[617] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
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
states[618] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.newRegexpNode(support.getPosition(((Node)yyVals[-1+yyTop].value)), ((Node)yyVals[-1+yyTop].value), (RegexpNode) ((RegexpNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[619] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[620] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = new ArrayNode(lexer.getRubySourceline());
  return yyVal;
};
states[621] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[-1+yyTop].value) instanceof EvStrNode ? new DStrNode(((ListNode)yyVals[-2+yyTop].value).getLine(), lexer.getEncoding()).add(((Node)yyVals[-1+yyTop].value)) : ((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[622] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[623] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
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
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[-1+yyTop].value) instanceof EvStrNode ? new DSymbolNode(((ListNode)yyVals[-2+yyTop].value).getLine()).add(((Node)yyVals[-1+yyTop].value)) : support.asSymbol(((ListNode)yyVals[-2+yyTop].value).getLine(), ((Node)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[627] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[628] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[-1+yyTop].value);
  return yyVal;
};
states[629] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ArrayNode(lexer.getRubySourceline());
  return yyVal;
};
states[630] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[-1+yyTop].value));
  return yyVal;
};
states[631] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ArrayNode(lexer.getRubySourceline());
  return yyVal;
};
states[632] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(support.asSymbol(((ListNode)yyVals[-2+yyTop].value).getLine(), ((Node)yyVals[-1+yyTop].value)));
  return yyVal;
};
states[633] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    ByteList aChar = ByteList.create("");
                    aChar.setEncoding(lexer.getEncoding());
                    yyVal = lexer.createStr(aChar, 0);
  return yyVal;
};
states[634] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[635] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    ByteList aChar = ByteList.create("");
                    aChar.setEncoding(lexer.getEncoding());
                    yyVal = lexer.createStr(aChar, 0);
  return yyVal;
};
states[636] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[637] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = null;
  return yyVal;
};
states[638] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
    /* FIXME: mri is different here.*/
                    yyVal = support.literal_concat(((Node)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[639] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[640] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = lexer.getStrTerm();
                    lexer.setStrTerm(null);
                    lexer.setState(EXPR_BEG);
  return yyVal;
};
states[641] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setStrTerm(((StrTerm)yyVals[-1+yyTop].value));
                    yyVal = new EvStrNode(support.getPosition(((Node)yyVals[0+yyTop].value)), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[642] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = lexer.getStrTerm();
                   lexer.setStrTerm(null);
                   lexer.getConditionState().stop();
  return yyVal;
};
states[643] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = lexer.getCmdArgumentState().getStack();
                   lexer.getCmdArgumentState().reset();
  return yyVal;
};
states[644] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = lexer.getState();
                   lexer.setState(EXPR_BEG);
  return yyVal;
};
states[645] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = lexer.getBraceNest();
                   lexer.setBraceNest(0);
  return yyVal;
};
states[646] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = lexer.getHeredocIndent();
                   lexer.setHeredocIndent(0);
  return yyVal;
};
states[647] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
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
states[648] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = new GlobalVarNode(lexer.getRubySourceline(), support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[649] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = new InstVarNode(lexer.getRubySourceline(), support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[650] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = new ClassVarNode(lexer.getRubySourceline(), support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[654] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     lexer.setState(EXPR_END);
                     yyVal = support.asSymbol(lexer.getRubySourceline(), ((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[656] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[657] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[658] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[659] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
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
states[660] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((NumericNode)yyVals[0+yyTop].value);  
  return yyVal;
};
states[661] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = support.negateNumeric(((NumericNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[662] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[663] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((FloatNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[664] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((RationalNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[665] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                     yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[666] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.declareIdentifier(((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[667] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new InstVarNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[668] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new GlobalVarNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[669] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ConstNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[670] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ClassVarNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)));
  return yyVal;
};
states[671] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new NilNode(lexer.tokline);
  return yyVal;
};
states[672] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new SelfNode(lexer.tokline);
  return yyVal;
};
states[673] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new TrueNode(lexer.tokline);
  return yyVal;
};
states[674] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new FalseNode(lexer.tokline);
  return yyVal;
};
states[675] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new FileNode(lexer.tokline, new ByteList(lexer.getFile().getBytes(),
                    support.getConfiguration().getRuntime().getEncodingService().getLocaleEncoding()));
  return yyVal;
};
states[676] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new FixnumNode(lexer.tokline, lexer.tokline+1);
  return yyVal;
};
states[677] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new EncodingNode(lexer.tokline, lexer.getEncoding());
  return yyVal;
};
states[678] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.assignableLabelOrIdentifier(((ByteList)yyVals[0+yyTop].value), null);
  return yyVal;
};
states[679] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new InstAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[680] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new GlobalAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[681] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (lexer.getLexContext().in_def) support.compile_error("dynamic constant assignment");

                    yyVal = new ConstDeclNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), null, NilImplicitNode.NIL);
  return yyVal;
};
states[682] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ClassVarAsgnNode(lexer.tokline, support.symbolID(((ByteList)yyVals[0+yyTop].value)), NilImplicitNode.NIL);
  return yyVal;
};
states[683] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to nil");
                    yyVal = null;
  return yyVal;
};
states[684] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't change the value of self");
                    yyVal = null;
  return yyVal;
};
states[685] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to true");
                    yyVal = null;
  return yyVal;
};
states[686] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to false");
                    yyVal = null;
  return yyVal;
};
states[687] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to __FILE__");
                    yyVal = null;
  return yyVal;
};
states[688] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to __LINE__");
                    yyVal = null;
  return yyVal;
};
states[689] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.compile_error("Can't assign to __ENCODING__");
                    yyVal = null;
  return yyVal;
};
states[690] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[691] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[692] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   lexer.setState(EXPR_BEG);
                   lexer.commandStart = true;
  return yyVal;
};
states[693] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[694] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = null;
  return yyVal;
};
states[696] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = null;
  return yyVal;
};
states[697] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ArgsNode)yyVals[-1+yyTop].value);
                    lexer.setState(EXPR_BEG);
                    lexer.commandStart = true;
  return yyVal;
};
states[698] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.add_forwarding_args();
                    yyVal = support.new_args_forward_def(yyVals[yyTop - count + 1].startLine(), ((ListNode)yyVals[-3+yyTop].value));
                    lexer.setState(EXPR_BEG);
                    lexer.commandStart = true;

  return yyVal;
};
states[699] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.add_forwarding_args();
                    yyVal = support.new_args_forward_def(yyVals[yyTop - count + 1].startLine(), null);
                    lexer.setState(EXPR_BEG);
                    lexer.commandStart = true;
  return yyVal;
};
states[700] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   yyVal = ((ArgsNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[701] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                   LexContext ctxt = lexer.getLexContext();
                   yyVal = (LexContext) ctxt.clone();
                   ctxt.in_kwarg = true;
                   lexer.setState(lexer.getState() | EXPR_LABEL);
  return yyVal;
};
states[702] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.getLexContext().in_kwarg = ((LexContext)yyVals[-2+yyTop].value).in_kwarg;
                    yyVal = ((ArgsNode)yyVals[-1+yyTop].value);
                    lexer.setState(EXPR_BEG);
                    lexer.commandStart = true;
  return yyVal;
};
states[703] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args_tail(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), ((ByteList)yyVals[-1+yyTop].value), ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[704] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args_tail(((ListNode)yyVals[-1+yyTop].value).getLine(), ((ListNode)yyVals[-1+yyTop].value), (ByteList) null, ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[705] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args_tail(lexer.getRubySourceline(), null, ((ByteList)yyVals[-1+yyTop].value), ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[706] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args_tail(((BlockArgNode)yyVals[0+yyTop].value).getLine(), null, (ByteList) null, ((BlockArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[707] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop].value);
  return yyVal;
};
states[708] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args_tail(lexer.getRubySourceline(), null, (ByteList) null, null);
  return yyVal;
};
states[709] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), ((ListNode)yyVals[-5+yyTop].value), ((ListNode)yyVals[-3+yyTop].value), ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[710] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-7+yyTop].value).getLine(), ((ListNode)yyVals[-7+yyTop].value), ((ListNode)yyVals[-5+yyTop].value), ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[711] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[712] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), ((ListNode)yyVals[-5+yyTop].value), ((ListNode)yyVals[-3+yyTop].value), null, ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[713] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), ((ListNode)yyVals[-3+yyTop].value), null, ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[714] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), ((ListNode)yyVals[-5+yyTop].value), null, ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[715] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop].value).getLine(), ((ListNode)yyVals[-1+yyTop].value), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[716] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), null, ((ListNode)yyVals[-3+yyTop].value), ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[717] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop].value).getLine(), null, ((ListNode)yyVals[-5+yyTop].value), ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[718] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop].value).getLine(), null, ((ListNode)yyVals[-1+yyTop].value), null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[719] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop].value).getLine(), null, ((ListNode)yyVals[-3+yyTop].value), null, ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[720] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((RestArgNode)yyVals[-1+yyTop].value).getLine(), null, null, ((RestArgNode)yyVals[-1+yyTop].value), null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[721] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((RestArgNode)yyVals[-3+yyTop].value).getLine(), null, null, ((RestArgNode)yyVals[-3+yyTop].value), ((ListNode)yyVals[-1+yyTop].value), ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[722] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(((ArgsTailHolder)yyVals[0+yyTop].value).getLine(), null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop].value));
  return yyVal;
};
states[723] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.new_args(lexer.getRubySourceline(), null, null, null, null, (ArgsTailHolder) null);
  return yyVal;
};
states[724] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[725] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.yyerror("formal argument cannot be a constant");
  return yyVal;
};
states[726] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.yyerror("formal argument cannot be an instance variable");
  return yyVal;
};
states[727] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.yyerror("formal argument cannot be a global variable");
  return yyVal;
};
states[728] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.yyerror("formal argument cannot be a class variable");
  return yyVal;
};
states[729] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value); /* Not really reached*/
  return yyVal;
};
states[730] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.formal_argument(((ByteList)yyVals[0+yyTop].value));
                    support.ordinalMaxNumParam();
  return yyVal;
};
states[731] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setCurrentArg(((ByteList)yyVals[0+yyTop].value));
                    yyVal = support.arg_var(((ByteList)yyVals[0+yyTop].value));
  return yyVal;
};
states[732] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setCurrentArg(null);
                    yyVal = ((ArgumentNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[733] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[734] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ArrayNode(lexer.getRubySourceline(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[735] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[0+yyTop].value));
                    yyVal = ((ListNode)yyVals[-2+yyTop].value);
  return yyVal;
};
states[736] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.arg_var(support.formal_argument(((ByteList)yyVals[0+yyTop].value)));
                    lexer.setCurrentArg(((ByteList)yyVals[0+yyTop].value));
                    support.ordinalMaxNumParam();
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[737] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setCurrentArg(null);
                    yyVal = new KeywordArgNode(((Node)yyVals[0+yyTop].value).getLine(), support.assignableKeyword(((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[738] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setCurrentArg(null);
                    yyVal = new KeywordArgNode(lexer.getRubySourceline(), support.assignableKeyword(((ByteList)yyVals[0+yyTop].value), new RequiredKeywordArgumentValueNode()));
  return yyVal;
};
states[739] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new KeywordArgNode(support.getPosition(((Node)yyVals[0+yyTop].value)), support.assignableKeyword(((ByteList)yyVals[-1+yyTop].value), ((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[740] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new KeywordArgNode(lexer.getRubySourceline(), support.assignableKeyword(((ByteList)yyVals[0+yyTop].value), new RequiredKeywordArgumentValueNode()));
  return yyVal;
};
states[741] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ArrayNode(((Node)yyVals[0+yyTop].value).getLine(), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[742] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[743] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new ArrayNode(((KeywordArgNode)yyVals[0+yyTop].value).getLine(), ((KeywordArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[744] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ListNode)yyVals[-2+yyTop].value).add(((KeywordArgNode)yyVals[0+yyTop].value));
  return yyVal;
};
states[745] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[746] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[747] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((Integer)yyVals[0+yyTop].value);
  return yyVal;
};
states[748] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.shadowing_lvar(((ByteList)yyVals[0+yyTop].value));
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[749] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.INTERNAL_ID;
  return yyVal;
};
states[750] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setCurrentArg(null);
                    yyVal = new OptArgNode(support.getPosition(((Node)yyVals[0+yyTop].value)), support.assignableLabelOrIdentifier(((ArgumentNode)yyVals[-2+yyTop].value).getName().getBytes(), ((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[751] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setCurrentArg(null);
                    yyVal = new OptArgNode(support.getPosition(((Node)yyVals[0+yyTop].value)), support.assignableLabelOrIdentifier(((ArgumentNode)yyVals[-2+yyTop].value).getName().getBytes(), ((Node)yyVals[0+yyTop].value)));
  return yyVal;
};
states[752] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new BlockNode(((Node)yyVals[0+yyTop].value).getLine()).add(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[753] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[754] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new BlockNode(((Node)yyVals[0+yyTop].value).getLine()).add(((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[755] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[756] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = STAR;
  return yyVal;
};
states[757] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[758] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (!support.is_local_id(((ByteList)yyVals[0+yyTop].value))) {
                        support.yyerror("rest argument must be local variable");
                    }
                    
                    yyVal = new RestArgNode(support.arg_var(support.shadowing_lvar(((ByteList)yyVals[0+yyTop].value))));
  return yyVal;
};
states[759] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
  /* FIXME: bytelist_love: somewhat silly to remake the empty bytelist over and over but this type should change (using null vs "" is a strange distinction).*/
  yyVal = new UnnamedRestArgNode(lexer.getRubySourceline(), support.symbolID(CommonByteLists.EMPTY), support.getCurrentScope().addVariable("*"));
  return yyVal;
};
states[760] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = AMPERSAND;
  return yyVal;
};
states[761] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[762] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (!support.is_local_id(((ByteList)yyVals[0+yyTop].value))) {
                        support.yyerror("block argument must be local variable");
                    }
                    
                    yyVal = new BlockArgNode(support.arg_var(support.shadowing_lvar(((ByteList)yyVals[0+yyTop].value))));
  return yyVal;
};
states[763] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((BlockArgNode)yyVals[0+yyTop].value);
  return yyVal;
};
states[764] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = null;
  return yyVal;
};
states[765] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    support.value_expr(lexer, ((Node)yyVals[0+yyTop].value));
                    yyVal = ((Node)yyVals[0+yyTop].value);
  return yyVal;
};
states[766] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    lexer.setState(EXPR_BEG);
  return yyVal;
};
states[767] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    if (((Node)yyVals[-1+yyTop].value) == null) {
                        support.yyerror("can't define single method for ().");
                    } else if (((Node)yyVals[-1+yyTop].value) instanceof ILiteralNode) {
                        support.yyerror("can't define single method for literals.");
                    }
                    support.value_expr(lexer, ((Node)yyVals[-1+yyTop].value));
                    yyVal = ((Node)yyVals[-1+yyTop].value);
  return yyVal;
};
states[768] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new HashNode(lexer.getRubySourceline());
  return yyVal;
};
states[769] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop].value));
  return yyVal;
};
states[770] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = new HashNode(lexer.getRubySourceline(), ((KeyValuePair)yyVals[0+yyTop].value));
  return yyVal;
};
states[771] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((HashNode)yyVals[-2+yyTop].value).add(((KeyValuePair)yyVals[0+yyTop].value));
  return yyVal;
};
states[772] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.createKeyValue(((Node)yyVals[-2+yyTop].value), ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[773] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    Node label = support.asSymbol(support.getPosition(((Node)yyVals[0+yyTop].value)), ((ByteList)yyVals[-1+yyTop].value));
                    yyVal = support.createKeyValue(label, ((Node)yyVals[0+yyTop].value));
  return yyVal;
};
states[774] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
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
states[775] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = support.createKeyValue(null, ((Node)yyVals[0+yyTop].value));
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
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[783] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[784] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[785] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[786] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = DOT;
  return yyVal;
};
states[787] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[788] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = DOT;
  return yyVal;
};
states[789] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[791] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = ((ByteList)yyVals[0+yyTop].value);
  return yyVal;
};
states[796] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RPAREN;
  return yyVal;
};
states[797] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RBRACKET;
  return yyVal;
};
states[798] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                    yyVal = RCURLY;
  return yyVal;
};
states[806] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                      yyVal = null;
  return yyVal;
};
states[807] = (ParserSupport support, RubyLexer lexer, Object yyVal, ProductionState[] yyVals, int yyTop, int count) -> {
                  yyVal = null;
  return yyVal;
};
}
					// line 3486 "RubyParser.y"

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
					// line 12950 "-"
