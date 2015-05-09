%{
/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
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
 * Copyright (C) 2008-2009 Thomas E Enebo <enebo@acm.org>
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
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.Colon3Node;
import org.jruby.ast.ConstNode;
import org.jruby.ast.ConstDeclNode;
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
import org.jruby.ast.IfNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.IterNode;
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
import org.jruby.ast.OpAsgnAndNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OptArgNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.PreExe19Node;
import org.jruby.ast.RationalNode;
import org.jruby.ast.RedoNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.RequiredKeywordArgumentValueNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.RestArgNode;
import org.jruby.ast.RetryNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.StarNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SymbolNode;
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
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.ISourcePositionHolder;
import org.jruby.lexer.LexerSource;
import org.jruby.lexer.yacc.RubyLexer;
import org.jruby.lexer.yacc.RubyLexer.LexState;
import org.jruby.lexer.yacc.StrTerm;
import org.jruby.lexer.yacc.SyntaxException;
import org.jruby.lexer.yacc.SyntaxException.PID;
import org.jruby.util.ByteList;
import org.jruby.util.KeyValuePair;
import org.jruby.util.cli.Options;
import org.jruby.util.StringSupport;
 
public class RubyParser {
    protected ParserSupport support;
    protected RubyLexer lexer;

    public RubyParser(LexerSource source) {
        this(new ParserSupport(), source);
    }

    public RubyParser(ParserSupport support, LexerSource source) {
        this.support = support;
        lexer = new RubyLexer(support, source);
        support.setLexer(lexer);
    }

    public void setWarnings(IRubyWarnings warnings) {
        support.setWarnings(warnings);
        lexer.setWarnings(warnings);
    }
%}

%token <ISourcePosition> kCLASS kMODULE kDEF kUNDEF kBEGIN kRESCUE kENSURE kEND kIF
  kUNLESS kTHEN kELSIF kELSE kCASE kWHEN kWHILE kUNTIL kFOR kBREAK kNEXT
  kREDO kRETRY kIN kDO kDO_COND kDO_BLOCK kRETURN kYIELD kSUPER kSELF kNIL
  kTRUE kFALSE kAND kOR kNOT kIF_MOD kUNLESS_MOD kWHILE_MOD kUNTIL_MOD
  kRESCUE_MOD kALIAS kDEFINED klBEGIN klEND k__LINE__ k__FILE__
  k__ENCODING__ kDO_LAMBDA 

%token <String> tIDENTIFIER tFID tGVAR tIVAR tCONSTANT tCVAR tLABEL
%token <StrNode> tCHAR
%type <String> sym symbol operation operation2 operation3 cname fname op 
%type <String> f_norm_arg dot_or_colon restarg_mark blkarg_mark
%token <String> tUPLUS         /* unary+ */
%token <String> tUMINUS        /* unary- */
%token <String> tUMINUS_NUM    /* unary- */
%token <String> tPOW           /* ** */
%token <String> tCMP           /* <=> */
%token <String> tEQ            /* == */
%token <String> tEQQ           /* === */
%token <String> tNEQ           /* != */
%token <String> tGEQ           /* >= */
%token <String> tLEQ           /* <= */
%token <String> tANDOP tOROP   /* && and || */
%token <String> tMATCH tNMATCH /* =~ and !~ */
%token <String>  tDOT           /* Is just '.' in ruby and not a token */
%token <String> tDOT2 tDOT3    /* .. and ... */
%token <String> tAREF tASET    /* [] and []= */
%token <String> tLSHFT tRSHFT  /* << and >> */
%token <String> tCOLON2        /* :: */
%token <String> tCOLON3        /* :: at EXPR_BEG */
%token <String> tOP_ASGN       /* +=, -=  etc. */
%token <String> tASSOC         /* => */
%token <ISourcePosition> tLPAREN       /* ( */
%token <ISourcePosition> tLPAREN2      /* ( Is just '(' in ruby and not a token */
%token <String> tRPAREN        /* ) */
%token <ISourcePosition> tLPAREN_ARG    /* ( */
%token <String> tLBRACK        /* [ */
%token <String> tRBRACK        /* ] */
%token <ISourcePosition> tLBRACE        /* { */
%token <ISourcePosition> tLBRACE_ARG    /* { */
%token <String> tSTAR          /* * */
%token <String> tSTAR2         /* *  Is just '*' in ruby and not a token */
%token <String> tAMPER         /* & */
%token <String> tAMPER2        /* &  Is just '&' in ruby and not a token */
%token <String> tTILDE         /* ` is just '`' in ruby and not a token */
%token <String> tPERCENT       /* % is just '%' in ruby and not a token */
%token <String> tDIVIDE        /* / is just '/' in ruby and not a token */
%token <String> tPLUS          /* + is just '+' in ruby and not a token */
%token <String> tMINUS         /* - is just '-' in ruby and not a token */
%token <String> tLT            /* < is just '<' in ruby and not a token */
%token <String> tGT            /* > is just '>' in ruby and not a token */
%token <String> tPIPE          /* | is just '|' in ruby and not a token */
%token <String> tBANG          /* ! is just '!' in ruby and not a token */
%token <String> tCARET         /* ^ is just '^' in ruby and not a token */
%token <ISourcePosition> tLCURLY        /* { is just '{' in ruby and not a token */
%token <String> tRCURLY        /* } is just '}' in ruby and not a token */
%token <String> tBACK_REF2     /* { is just '`' in ruby and not a token */
%token <String> tSYMBEG tSTRING_BEG tXSTRING_BEG tREGEXP_BEG tWORDS_BEG tQWORDS_BEG
%token <String> tSTRING_DBEG tSTRING_DVAR tSTRING_END
%token <String> tLAMBDA tLAMBEG
%token <Node> tNTH_REF tBACK_REF tSTRING_CONTENT tINTEGER tIMAGINARY
%token <FloatNode> tFLOAT  
%token <RationalNode> tRATIONAL
%token <RegexpNode>  tREGEXP_END
%type <RestArgNode> f_rest_arg
%type <Node> singleton strings string string1 xstring regexp
%type <Node> string_contents xstring_contents method_call
%type <Object> string_content
%type <Node> regexp_contents
%type <Node> words qwords word literal dsym cpath command_asgn command_call
%type <NumericNode> numeric simple_numeric 
%type <Node> mrhs_arg
%type <Node> compstmt bodystmt stmts stmt expr arg primary command 
%type <Node> stmt_or_begin
%type <Node> expr_value primary_value opt_else cases if_tail exc_var
   // ENEBO: missing call_args2, open_args
%type <Node> call_args opt_ensure paren_args superclass
%type <Node> command_args var_ref opt_paren_args block_call block_command
%type <Node> f_opt
%type <Node> undef_list
%type <Node> string_dvar backref
%type <ArgsNode> f_args f_larglist block_param block_param_def opt_block_param
%type <Object> f_arglist
%type <Node> mrhs mlhs_item mlhs_node arg_value case_body exc_list aref_args
   // ENEBO: missing block_var == for_var, opt_block_var
%type <Node> lhs none args
%type <ListNode> qword_list word_list
%type <ListNode> f_arg f_optarg
%type <ListNode> f_marg_list, symbol_list
%type <ListNode> qsym_list, symbols, qsymbols
   // FIXME: These are node until a better understanding of underlying type
%type <ArgsTailHolder> opt_args_tail, opt_block_args_tail, block_args_tail, args_tail
%type <Node> f_kw, f_block_kw
%type <ListNode> f_block_kwarg, f_kwarg
   // ENEBO: missing when_args
%type <HashNode> assoc_list
%type <HashNode> assocs
%type <KeyValuePair> assoc
%type <ListNode> mlhs_head mlhs_post
%type <ListNode> f_block_optarg
%type <BlockPassNode> opt_block_arg block_arg none_block_pass
%type <BlockArgNode> opt_f_block_arg f_block_arg
%type <IterNode> brace_block do_block cmd_brace_block
   // ENEBO: missing mhls_entry
%type <MultipleAsgnNode> mlhs mlhs_basic 
%type <RescueBodyNode> opt_rescue
%type <AssignableNode> var_lhs
%type <LiteralNode> fsym
%type <Node> fitem
   // ENEBO: begin all new types
%type <Node> f_arg_item
%type <Node> bv_decls
%type <Node> opt_bv_decl lambda_body 
%type <LambdaNode> lambda
%type <Node> mlhs_inner f_block_opt for_var
%type <Node> opt_call_args f_marg f_margs
%type <String> bvar
   // ENEBO: end all new types

%type <String> rparen rbracket reswords f_bad_arg
%type <Node> top_compstmt top_stmts top_stmt
%token <String> tSYMBOLS_BEG
%token <String> tQSYMBOLS_BEG
%token <String> tDSTAR
%token <String> tSTRING_DEND
%type <String> kwrest_mark, f_kwrest, f_label
%type <FCallNode> fcall
%token <String> tLABEL_END, tSTRING_DEND

/*
 *    precedence table
 */

%nonassoc tLOWEST
%nonassoc tLBRACE_ARG

%nonassoc  kIF_MOD kUNLESS_MOD kWHILE_MOD kUNTIL_MOD
%left  kOR kAND
%right kNOT
%nonassoc kDEFINED
%right '=' tOP_ASGN
%left kRESCUE_MOD
%right '?' ':'
%nonassoc tDOT2 tDOT3
%left  tOROP
%left  tANDOP
%nonassoc  tCMP tEQ tEQQ tNEQ tMATCH tNMATCH
%left  tGT tGEQ tLT tLEQ
%left  tPIPE tCARET
%left  tAMPER2
%left  tLSHFT tRSHFT
%left  tPLUS tMINUS
%left  tSTAR2 tDIVIDE tPERCENT
%right tUMINUS_NUM tUMINUS
%right tPOW
%right tBANG tTILDE tUPLUS

   //%token <Integer> tLAST_TOKEN

%%
program       : {
                  lexer.setState(LexState.EXPR_BEG);
                  support.initTopLocalVariables();
              } top_compstmt {
  // ENEBO: Removed !compile_for_eval which probably is to reduce warnings
                  if ($2 != null) {
                      /* last expression should not be void */
                      if ($2 instanceof BlockNode) {
                          support.checkUselessStatement($<BlockNode>2.getLast());
                      } else {
                          support.checkUselessStatement($2);
                      }
                  }
                  support.getResult().setAST(support.addRootNode($2));
              }

top_compstmt  : top_stmts opt_terms {
                  if ($1 instanceof BlockNode) {
                      support.checkUselessStatements($<BlockNode>1);
                  }
                  $$ = $1;
              }

top_stmts     : none
              | top_stmt {
                    $$ = support.newline_node($1, support.getPosition($1));
              }
              | top_stmts terms top_stmt {
                    $$ = support.appendToBlock($1, support.newline_node($3, support.getPosition($3)));
              }
              | error top_stmt {
                    $$ = $2;
              }

top_stmt      : stmt
              | klBEGIN {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("BEGIN in method");
                    }
              } tLCURLY top_compstmt tRCURLY {
                    support.getResult().addBeginNode(new PreExe19Node($1, support.getCurrentScope(), $4));
                    $$ = null;
              }

bodystmt      : compstmt opt_rescue opt_else opt_ensure {
                  Node node = $1;

                  if ($2 != null) {
                      node = new RescueNode(support.getPosition($1), $1, $2, $3);
                  } else if ($3 != null) {
                      support.warn(ID.ELSE_WITHOUT_RESCUE, support.getPosition($1), "else without rescue is useless");
                      node = support.appendToBlock($1, $3);
                  }
                  if ($4 != null) {
                      if (node == null) node = NilImplicitNode.NIL;
                      node = new EnsureNode(support.getPosition($1), node, $4);
                  }

                  support.fixpos(node, $1);
                  $$ = node;
                }

compstmt        : stmts opt_terms {
                    if ($1 instanceof BlockNode) {
                        support.checkUselessStatements($<BlockNode>1);
                    }
                    $$ = $1;
                }

stmts           : none
                | stmt_or_begin {
                    $$ = support.newline_node($1, support.getPosition($1));
                }
                | stmts terms stmt_or_begin {
                    $$ = support.appendToBlock($1, support.newline_node($3, support.getPosition($3)));
                }
                | error stmt {
                    $$ = $2;
                }

stmt_or_begin   : stmt {
                    $$ = $1;
                }
// FIXME: How can this new begin ever work?  is yyerror conditional in MRI?
                | kBEGIN {
                   support.yyerror("BEGIN is permitted only at toplevel");
                } tLCURLY top_compstmt tRCURLY {
                    $$ = new BeginNode($1, $2 == null ? NilImplicitNode.NIL : $2);
                }

stmt            : kALIAS fitem {
                    lexer.setState(LexState.EXPR_FNAME);
                } fitem {
                    $$ = support.newAlias($1, $2, $4);
                }
                | kALIAS tGVAR tGVAR {
                    $$ = new VAliasNode($1, $2, $3);
                }
                | kALIAS tGVAR tBACK_REF {
                    $$ = new VAliasNode($1, $2, "$" + $<BackRefNode>3.getType());
                }
                | kALIAS tGVAR tNTH_REF {
                    support.yyerror("can't make alias for the number variables");
                }
                | kUNDEF undef_list {
                    $$ = $2;
                }
                | stmt kIF_MOD expr_value {
                    $$ = new IfNode(support.getPosition($1), support.getConditionNode($3), $1, null);
                    support.fixpos($<Node>$, $3);
                }
                | stmt kUNLESS_MOD expr_value {
                    $$ = new IfNode(support.getPosition($1), support.getConditionNode($3), null, $1);
                    support.fixpos($<Node>$, $3);
                }
                | stmt kWHILE_MOD expr_value {
                    if ($1 != null && $1 instanceof BeginNode) {
                        $$ = new WhileNode(support.getPosition($1), support.getConditionNode($3), $<BeginNode>1.getBodyNode(), false);
                    } else {
                        $$ = new WhileNode(support.getPosition($1), support.getConditionNode($3), $1, true);
                    }
                }
                | stmt kUNTIL_MOD expr_value {
                    if ($1 != null && $1 instanceof BeginNode) {
                        $$ = new UntilNode(support.getPosition($1), support.getConditionNode($3), $<BeginNode>1.getBodyNode(), false);
                    } else {
                        $$ = new UntilNode(support.getPosition($1), support.getConditionNode($3), $1, true);
                    }
                }
                | stmt kRESCUE_MOD stmt {
                    Node body = $3 == null ? NilImplicitNode.NIL : $3;
                    $$ = new RescueNode(support.getPosition($1), $1, new RescueBodyNode(support.getPosition($1), null, body, null), null);
                }
                | klEND tLCURLY compstmt tRCURLY {
                    if (support.isInDef() || support.isInSingle()) {
                        support.warn(ID.END_IN_METHOD, $1, "END in method; use at_exit");
                    }
                    $$ = new PostExeNode($1, $3);
                }
                | command_asgn
                | mlhs '=' command_call {
                    support.checkExpression($3);
                    $1.setValueNode($3);
                    $$ = $1;
                }
                | var_lhs tOP_ASGN command_call {
                    support.checkExpression($3);

                    ISourcePosition pos = $1.getPosition();
                    String asgnOp = $2;
                    if (asgnOp.equals("||")) {
                        $1.setValueNode($3);
                        $$ = new OpAsgnOrNode(pos, support.gettable2($1), $1);
                    } else if (asgnOp.equals("&&")) {
                        $1.setValueNode($3);
                        $$ = new OpAsgnAndNode(pos, support.gettable2($1), $1);
                    } else {
                        $1.setValueNode(support.getOperatorCallNode(support.gettable2($1), asgnOp, $3));
                        $1.setPosition(pos);
                        $$ = $1;
                    }
                }
                | primary_value '[' opt_call_args rbracket tOP_ASGN command_call {
  // FIXME: arg_concat logic missing for opt_call_args
                    $$ = support.new_opElementAsgnNode($1, $5, $3, $6);
                }
                | primary_value tDOT tIDENTIFIER tOP_ASGN command_call {
                    $$ = new OpAsgnNode(support.getPosition($1), $1, $5, $3, $4);
                }
                | primary_value tDOT tCONSTANT tOP_ASGN command_call {
                    $$ = new OpAsgnNode(support.getPosition($1), $1, $5, $3, $4);
                }
                | primary_value tCOLON2 tCONSTANT tOP_ASGN command_call {
                    support.yyerror("can't make alias for the number variables");
                    $$ = null;
                }

                | primary_value tCOLON2 tIDENTIFIER tOP_ASGN command_call {
                    $$ = new OpAsgnNode(support.getPosition($1), $1, $5, $3, $4);
                }
                | backref tOP_ASGN command_call {
                    support.backrefAssignError($1);
                }
                | lhs '=' mrhs {
                    $$ = support.node_assign($1, $3);
                }
                | mlhs '=' mrhs_arg {
                    $<AssignableNode>1.setValueNode($3);
                    $$ = $1;
                    $1.setPosition(support.getPosition($1));
                }
                | expr

command_asgn    : lhs '=' command_call {
                    support.checkExpression($3);
                    $$ = support.node_assign($1, $3);
                }
                | lhs '=' command_asgn {
                    support.checkExpression($3);
                    $$ = support.node_assign($1, $3);
                }

// Node:expr *CURRENT* all but arg so far
expr            : command_call
                | expr kAND expr {
                    $$ = support.newAndNode(support.getPosition($1), $1, $3);
                }
                | expr kOR expr {
                    $$ = support.newOrNode(support.getPosition($1), $1, $3);
                }
                | kNOT opt_nl expr {
                    $$ = support.getOperatorCallNode(support.getConditionNode($3), "!");
                }
                | tBANG command_call {
                    $$ = support.getOperatorCallNode(support.getConditionNode($2), "!");
                }
                | arg

expr_value      : expr {
                    support.checkExpression($1);
                }

// Node:command - call with or with block on end [!null]
command_call    : command
                | block_command

// Node:block_command - A call with a block (foo.bar {...}, foo::bar {...}, bar {...}) [!null]
block_command   : block_call
                | block_call dot_or_colon operation2 command_args {
                    $$ = support.new_call($1, $3, $4, null);
                }

// :brace_block - [!null]
cmd_brace_block : tLBRACE_ARG {
                    support.pushBlockScope();
                } opt_block_param compstmt tRCURLY {
                    $$ = new IterNode($1, $3, $4, support.getCurrentScope());
                    support.popCurrentScope();
                }

fcall           : operation {
                    $$ = support.new_fcall($1);
                }

// Node:command - fcall/call/yield/super [!null]
command        : fcall command_args %prec tLOWEST {
                    support.frobnicate_fcall_args($1, $2, null);
                    $$ = $1;
                }
                | fcall command_args cmd_brace_block {
                    support.frobnicate_fcall_args($1, $2, $3);
                    $$ = $1;
                }
                | primary_value tDOT operation2 command_args %prec tLOWEST {
                    $$ = support.new_call($1, $3, $4, null);
                }
                | primary_value tDOT operation2 command_args cmd_brace_block {
                    $$ = support.new_call($1, $3, $4, $5); 
                }
                | primary_value tCOLON2 operation2 command_args %prec tLOWEST {
                    $$ = support.new_call($1, $3, $4, null);
                }
                | primary_value tCOLON2 operation2 command_args cmd_brace_block {
                    $$ = support.new_call($1, $3, $4, $5);
                }
                | kSUPER command_args {
                    $$ = support.new_super($1, $2);
                }
                | kYIELD command_args {
                    $$ = support.new_yield($1, $2);
                }
                | kRETURN call_args {
                    $$ = new ReturnNode($1, support.ret_args($2, $1));
                }
                | kBREAK call_args {
                    $$ = new BreakNode($1, support.ret_args($2, $1));
                }
                | kNEXT call_args {
                    $$ = new NextNode($1, support.ret_args($2, $1));
                }

// MultipleAssigNode:mlhs - [!null]
mlhs            : mlhs_basic
                | tLPAREN mlhs_inner rparen {
                    $$ = $2;
                }

// MultipleAssignNode:mlhs_entry - mlhs w or w/o parens [!null]
mlhs_inner      : mlhs_basic {
                    $$ = $1;
                }
                | tLPAREN mlhs_inner rparen {
                    $$ = new MultipleAsgnNode($1, support.newArrayNode($1, $2), null, null);
                }

// MultipleAssignNode:mlhs_basic - multiple left hand side (basic because used in multiple context) [!null]
mlhs_basic      : mlhs_head {
                    $$ = new MultipleAsgnNode($1.getPosition(), $1, null, null);
                }
                | mlhs_head mlhs_item {
                    $$ = new MultipleAsgnNode($1.getPosition(), $1.add($2), null, null);
                }
                | mlhs_head tSTAR mlhs_node {
                    $$ = new MultipleAsgnNode($1.getPosition(), $1, $3, (ListNode) null);
                }
                | mlhs_head tSTAR mlhs_node ',' mlhs_post {
                    $$ = new MultipleAsgnNode($1.getPosition(), $1, $3, $5);
                }
                | mlhs_head tSTAR {
                    $$ = new MultipleAsgnNode($1.getPosition(), $1, new StarNode(lexer.getPosition()), null);
                }
                | mlhs_head tSTAR ',' mlhs_post {
                    $$ = new MultipleAsgnNode($1.getPosition(), $1, new StarNode(lexer.getPosition()), $4);
                }
                | tSTAR mlhs_node {
                    $$ = new MultipleAsgnNode($2.getPosition(), null, $2, null);
                }
                | tSTAR mlhs_node ',' mlhs_post {
                    $$ = new MultipleAsgnNode($2.getPosition(), null, $2, $4);
                }
                | tSTAR {
                      $$ = new MultipleAsgnNode(lexer.getPosition(), null, new StarNode(lexer.getPosition()), null);
                }
                | tSTAR ',' mlhs_post {
                      $$ = new MultipleAsgnNode(lexer.getPosition(), null, new StarNode(lexer.getPosition()), $3);
                }

mlhs_item       : mlhs_node
                | tLPAREN mlhs_inner rparen {
                    $$ = $2;
                }

// Set of mlhs terms at front of mlhs (a, *b, d, e = arr  # a is head)
mlhs_head       : mlhs_item ',' {
                    $$ = support.newArrayNode($1.getPosition(), $1);
                }
                | mlhs_head mlhs_item ',' {
                    $$ = $1.add($2);
                }

// Set of mlhs terms at end of mlhs (a, *b, d, e = arr  # d,e is post)
mlhs_post       : mlhs_item {
                    $$ = support.newArrayNode($1.getPosition(), $1);
                }
                | mlhs_post ',' mlhs_item {
                    $$ = $1.add($3);
                }

mlhs_node       : /*mri:user_variable*/ tIDENTIFIER {
                    $$ = support.assignableLabelOrIdentifier($1, null);
                }
                | tIVAR {
                   $$ = new InstAsgnNode(lexer.getPosition(), $1, NilImplicitNode.NIL);
                }
                | tGVAR {
                   $$ = new GlobalAsgnNode(lexer.getPosition(), $1, NilImplicitNode.NIL);
                }
                | tCONSTANT {
                    if (support.isInDef() || support.isInSingle()) support.compile_error("dynamic constant assignment");

                    $$ = new ConstDeclNode(lexer.getPosition(), $1, null, NilImplicitNode.NIL);
                }
                | tCVAR {
                    $$ = new ClassVarAsgnNode(lexer.getPosition(), $1, NilImplicitNode.NIL);
                } /*mri:user_variable*/
                | /*mri:keyword_variable*/ kNIL {
                    support.compile_error("Can't assign to nil");
                    $$ = null;
                }
                | kSELF {
                    support.compile_error("Can't change the value of self");
                    $$ = null;
                }
                | kTRUE {
                    support.compile_error("Can't assign to true");
                    $$ = null;
                }
                | kFALSE {
                    support.compile_error("Can't assign to false");
                    $$ = null;
                }
                | k__FILE__ {
                    support.compile_error("Can't assign to __FILE__");
                    $$ = null;
                }
                | k__LINE__ {
                    support.compile_error("Can't assign to __LINE__");
                    $$ = null;
                }
                | k__ENCODING__ {
                    support.compile_error("Can't assign to __ENCODING__");
                    $$ = null;
                } /*mri:keyword_variable*/
                | primary_value '[' opt_call_args rbracket {
                    $$ = support.aryset($1, $3);
                }
                | primary_value tDOT tIDENTIFIER {
                    $$ = support.attrset($1, $3);
                }
                | primary_value tCOLON2 tIDENTIFIER {
                    $$ = support.attrset($1, $3);
                }
                | primary_value tDOT tCONSTANT {
                    $$ = support.attrset($1, $3);
                }
                | primary_value tCOLON2 tCONSTANT {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = support.getPosition($1);

                    $$ = new ConstDeclNode(position, null, support.new_colon2(position, $1, $3), NilImplicitNode.NIL);
                }
                | tCOLON3 tCONSTANT {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = lexer.getPosition();

                    $$ = new ConstDeclNode(position, null, support.new_colon3(position, $2), NilImplicitNode.NIL);
                }
                | backref {
                    support.backrefAssignError($1);
                }

lhs             : /*mri:user_variable*/ tIDENTIFIER {
                    $$ = support.assignableLabelOrIdentifier($1, null);
                }
                | tIVAR {
                   $$ = new InstAsgnNode(lexer.getPosition(), $1, NilImplicitNode.NIL);
                }
                | tGVAR {
                   $$ = new GlobalAsgnNode(lexer.getPosition(), $1, NilImplicitNode.NIL);
                }
                | tCONSTANT {
                    if (support.isInDef() || support.isInSingle()) support.compile_error("dynamic constant assignment");

                    $$ = new ConstDeclNode(lexer.getPosition(), $1, null, NilImplicitNode.NIL);
                }
                | tCVAR {
                    $$ = new ClassVarAsgnNode(lexer.getPosition(), $1, NilImplicitNode.NIL);
                } /*mri:user_variable*/
                | /*mri:keyword_variable*/ kNIL {
                    support.compile_error("Can't assign to nil");
                    $$ = null;
                }
                | kSELF {
                    support.compile_error("Can't change the value of self");
                    $$ = null;
                }
                | kTRUE {
                    support.compile_error("Can't assign to true");
                    $$ = null;
                }
                | kFALSE {
                    support.compile_error("Can't assign to false");
                    $$ = null;
                }
                | k__FILE__ {
                    support.compile_error("Can't assign to __FILE__");
                    $$ = null;
                }
                | k__LINE__ {
                    support.compile_error("Can't assign to __LINE__");
                    $$ = null;
                }
                | k__ENCODING__ {
                    support.compile_error("Can't assign to __ENCODING__");
                    $$ = null;
                } /*mri:keyword_variable*/
                | primary_value '[' opt_call_args rbracket {
                    $$ = support.aryset($1, $3);
                }
                | primary_value tDOT tIDENTIFIER {
                    $$ = support.attrset($1, $3);
                }
                | primary_value tCOLON2 tIDENTIFIER {
                    $$ = support.attrset($1, $3);
                }
                | primary_value tDOT tCONSTANT {
                    $$ = support.attrset($1, $3);
                }
                | primary_value tCOLON2 tCONSTANT {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = support.getPosition($1);

                    $$ = new ConstDeclNode(position, null, support.new_colon2(position, $1, $3), NilImplicitNode.NIL);
                }
                | tCOLON3 tCONSTANT {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = lexer.getPosition();

                    $$ = new ConstDeclNode(position, null, support.new_colon3(position, $2), NilImplicitNode.NIL);
                }
                | backref {
                    support.backrefAssignError($1);
                }

cname           : tIDENTIFIER {
                    support.yyerror("class/module name must be CONSTANT");
                }
                | tCONSTANT

cpath           : tCOLON3 cname {
                    $$ = support.new_colon3(lexer.getPosition(), $2);
                }
                | cname {
                    $$ = support.new_colon2(lexer.getPosition(), null, $1);
                }
                | primary_value tCOLON2 cname {
                    $$ = support.new_colon2(support.getPosition($1), $1, $3);
                }

// String:fname - A function name [!null]
fname          : tIDENTIFIER | tCONSTANT | tFID 
               | op {
                   lexer.setState(LexState.EXPR_ENDFN);
                   $$ = $1;
               }
               | reswords {
                   lexer.setState(LexState.EXPR_ENDFN);
                   $$ = $1;
               }

// LiteralNode:fsym
fsym           : fname {
                    $$ = new LiteralNode(lexer.getPosition(), $1);
                }
                | symbol {
                    $$ = new LiteralNode(lexer.getPosition(), $1);
                }

// Node:fitem
fitem           : fsym {
                    $$ = $1;
                }
                | dsym {
                    $$ = $1;
                }

undef_list      : fitem {
                    $$ = support.newUndef($1.getPosition(), $1);
                }
                | undef_list ',' {
                    lexer.setState(LexState.EXPR_FNAME);
                } fitem {
                    $$ = support.appendToBlock($1, support.newUndef($1.getPosition(), $4));
                }

// String:op
op              : tPIPE | tCARET | tAMPER2 | tCMP | tEQ | tEQQ | tMATCH
                | tNMATCH | tGT | tGEQ | tLT | tLEQ | tNEQ | tLSHFT | tRSHFT
                | tDSTAR | tPLUS | tMINUS | tSTAR2 | tSTAR | tDIVIDE | tPERCENT 
                | tPOW | tBANG | tTILDE | tUPLUS | tUMINUS | tAREF | tASET 
                | tBACK_REF2

// String:op
reswords        : k__LINE__ {
                    $$ = "__LINE__";
                }
                | k__FILE__ {
                    $$ = "__FILE__";
                }
                | k__ENCODING__ {
                    $$ = "__ENCODING__";
                }
                | klBEGIN {
                    $$ = "BEGIN";
                }
                | klEND {
                    $$ = "END";
                }
                | kALIAS {
                    $$ = "alias";
                }
                | kAND {
                    $$ = "and";
                }
                | kBEGIN {
                    $$ = "begin";
                }
                | kBREAK {
                    $$ = "break";
                }
                | kCASE {
                    $$ = "case";
                }
                | kCLASS {
                    $$ = "class";
                }
                | kDEF {
                    $$ = "def";
                }
                | kDEFINED {
                    $$ = "defined?";
                }
                | kDO {
                    $$ = "do";
                }
                | kELSE {
                    $$ = "else";
                }
                | kELSIF {
                    $$ = "elsif";
                }
                | kEND {
                    $$ = "end";
                }
                | kENSURE {
                    $$ = "ensure";
                }
                | kFALSE {
                    $$ = "false";
                }
                | kFOR {
                    $$ = "for";
                }
                | kIN {
                    $$ = "in";
                }
                | kMODULE {
                    $$ = "module";
                }
                | kNEXT {
                    $$ = "next";
                }
                | kNIL {
                    $$ = "nil";
                }
                | kNOT {
                    $$ = "not";
                }
                | kOR {
                    $$ = "or";
                }
                | kREDO {
                    $$ = "redo";
                }
                | kRESCUE {
                    $$ = "rescue";
                }
                | kRETRY {
                    $$ = "retry";
                }
                | kRETURN {
                    $$ = "return";
                }
                | kSELF {
                    $$ = "self";
                }
                | kSUPER {
                    $$ = "super";
                }
                | kTHEN {
                    $$ = "then";
                }
                | kTRUE {
                    $$ = "true";
                }
                | kUNDEF {
                    $$ = "undef";
                }
                | kWHEN {
                    $$ = "when";
                }
                | kYIELD {
                    $$ = "yield";
                }
                | kIF_MOD {
                    $$ = "if";
                }
                | kUNLESS_MOD {
                    $$ = "unless";
                }
                | kWHILE_MOD {
                    $$ = "while";
                }
                | kUNTIL_MOD {
                    $$ = "until";
                }
                | kRESCUE_MOD {
                    $$ = "rescue";
                }

arg             : lhs '=' arg {
                    $$ = support.node_assign($1, $3);
                    // FIXME: Consider fixing node_assign itself rather than single case
                    $<Node>$.setPosition(support.getPosition($1));
                }
                | lhs '=' arg kRESCUE_MOD arg {
                    ISourcePosition position = support.getPosition($1);
                    Node body = $5 == null ? NilImplicitNode.NIL : $5;
                    $$ = support.node_assign($1, new RescueNode(position, $3, new RescueBodyNode(position, null, body, null), null));
                }
                | var_lhs tOP_ASGN arg {
                    support.checkExpression($3);

                    ISourcePosition pos = $1.getPosition();
                    String asgnOp = $2;
                    if (asgnOp.equals("||")) {
                        $1.setValueNode($3);
                        $$ = new OpAsgnOrNode(pos, support.gettable2($1), $1);
                    } else if (asgnOp.equals("&&")) {
                        $1.setValueNode($3);
                        $$ = new OpAsgnAndNode(pos, support.gettable2($1), $1);
                    } else {
                        $1.setValueNode(support.getOperatorCallNode(support.gettable2($1), asgnOp, $3));
                        $1.setPosition(pos);
                        $$ = $1;
                    }
                }
                | var_lhs tOP_ASGN arg kRESCUE_MOD arg {
                    support.checkExpression($3);
                    ISourcePosition pos = support.getPosition($5);
                    Node body = $5 == null ? NilImplicitNode.NIL : $5;
                    Node rescue = new RescueNode(pos, $3, new RescueBodyNode(support.getPosition($3), null, body, null), null);

                    pos = $1.getPosition();
                    String asgnOp = $2;
                    if (asgnOp.equals("||")) {
                        $1.setValueNode(rescue);
                        $$ = new OpAsgnOrNode(pos, support.gettable2($1), $1);
                    } else if (asgnOp.equals("&&")) {
                        $1.setValueNode(rescue);
                        $$ = new OpAsgnAndNode(pos, support.gettable2($1), $1);
                    } else {
                        $1.setValueNode(support.getOperatorCallNode(support.gettable2($1), asgnOp, rescue));
                        $1.setPosition(pos);
                        $$ = $1;
                    }
                }
                | primary_value '[' opt_call_args rbracket tOP_ASGN arg {
  // FIXME: arg_concat missing for opt_call_args
                    $$ = support.new_opElementAsgnNode($1, $5, $3, $6);
                }
                | primary_value tDOT tIDENTIFIER tOP_ASGN arg {
                    $$ = new OpAsgnNode(support.getPosition($1), $1, $5, $3, $4);
                }
                | primary_value tDOT tCONSTANT tOP_ASGN arg {
                    $$ = new OpAsgnNode(support.getPosition($1), $1, $5, $3, $4);
                }
                | primary_value tCOLON2 tIDENTIFIER tOP_ASGN arg {
                    $$ = new OpAsgnNode(support.getPosition($1), $1, $5, $3, $4);
                }
                | primary_value tCOLON2 tCONSTANT tOP_ASGN arg {
                    support.yyerror("constant re-assignment");
                }
                | tCOLON3 tCONSTANT tOP_ASGN arg {
                    support.yyerror("constant re-assignment");
                }
                | backref tOP_ASGN arg {
                    support.backrefAssignError($1);
                }
                | arg tDOT2 arg {
                    support.checkExpression($1);
                    support.checkExpression($3);
    
                    boolean isLiteral = $1 instanceof FixnumNode && $3 instanceof FixnumNode;
                    $$ = new DotNode(support.getPosition($1), $1, $3, false, isLiteral);
                }
                | arg tDOT3 arg {
                    support.checkExpression($1);
                    support.checkExpression($3);

                    boolean isLiteral = $1 instanceof FixnumNode && $3 instanceof FixnumNode;
                    $$ = new DotNode(support.getPosition($1), $1, $3, true, isLiteral);
                }
                | arg tPLUS arg {
                    $$ = support.getOperatorCallNode($1, "+", $3, lexer.getPosition());
                }
                | arg tMINUS arg {
                    $$ = support.getOperatorCallNode($1, "-", $3, lexer.getPosition());
                }
                | arg tSTAR2 arg {
                    $$ = support.getOperatorCallNode($1, "*", $3, lexer.getPosition());
                }
                | arg tDIVIDE arg {
                    $$ = support.getOperatorCallNode($1, "/", $3, lexer.getPosition());
                }
                | arg tPERCENT arg {
                    $$ = support.getOperatorCallNode($1, "%", $3, lexer.getPosition());
                }
                | arg tPOW arg {
                    $$ = support.getOperatorCallNode($1, "**", $3, lexer.getPosition());
                }
                | tUMINUS_NUM simple_numeric tPOW arg {
                    $$ = support.getOperatorCallNode(support.getOperatorCallNode($2, "**", $4, lexer.getPosition()), "-@");
                }
                | tUPLUS arg {
                    $$ = support.getOperatorCallNode($2, "+@");
                }
                | tUMINUS arg {
                    $$ = support.getOperatorCallNode($2, "-@");
                }
                | arg tPIPE arg {
                    $$ = support.getOperatorCallNode($1, "|", $3, lexer.getPosition());
                }
                | arg tCARET arg {
                    $$ = support.getOperatorCallNode($1, "^", $3, lexer.getPosition());
                }
                | arg tAMPER2 arg {
                    $$ = support.getOperatorCallNode($1, "&", $3, lexer.getPosition());
                }
                | arg tCMP arg {
                    $$ = support.getOperatorCallNode($1, "<=>", $3, lexer.getPosition());
                }
                | arg tGT arg {
                    $$ = support.getOperatorCallNode($1, ">", $3, lexer.getPosition());
                }
                | arg tGEQ arg {
                    $$ = support.getOperatorCallNode($1, ">=", $3, lexer.getPosition());
                }
                | arg tLT arg {
                    $$ = support.getOperatorCallNode($1, "<", $3, lexer.getPosition());
                }
                | arg tLEQ arg {
                    $$ = support.getOperatorCallNode($1, "<=", $3, lexer.getPosition());
                }
                | arg tEQ arg {
                    $$ = support.getOperatorCallNode($1, "==", $3, lexer.getPosition());
                }
                | arg tEQQ arg {
                    $$ = support.getOperatorCallNode($1, "===", $3, lexer.getPosition());
                }
                | arg tNEQ arg {
                    $$ = support.getOperatorCallNode($1, "!=", $3, lexer.getPosition());
                }
                | arg tMATCH arg {
                    $$ = support.getMatchNode($1, $3);
                  /* ENEBO
                        $$ = match_op($1, $3);
                        if (nd_type($1) == NODE_LIT && TYPE($1->nd_lit) == T_REGEXP) {
                            $$ = reg_named_capture_assign($1->nd_lit, $$);
                        }
                  */
                }
                | arg tNMATCH arg {
                    $$ = support.getOperatorCallNode($1, "!~", $3, lexer.getPosition());
                }
                | tBANG arg {
                    $$ = support.getOperatorCallNode(support.getConditionNode($2), "!");
                }
                | tTILDE arg {
                    $$ = support.getOperatorCallNode($2, "~");
                }
                | arg tLSHFT arg {
                    $$ = support.getOperatorCallNode($1, "<<", $3, lexer.getPosition());
                }
                | arg tRSHFT arg {
                    $$ = support.getOperatorCallNode($1, ">>", $3, lexer.getPosition());
                }
                | arg tANDOP arg {
                    $$ = support.newAndNode($1.getPosition(), $1, $3);
                }
                | arg tOROP arg {
                    $$ = support.newOrNode($1.getPosition(), $1, $3);
                }
                | kDEFINED opt_nl arg {
                    $$ = support.new_defined($1, $3);
                }
                | arg '?' arg opt_nl ':' arg {
                    $$ = new IfNode(support.getPosition($1), support.getConditionNode($1), $3, $6);
                }
                | primary {
                    $$ = $1;
                }

arg_value       : arg {
                    support.checkExpression($1);
                    $$ = $1 != null ? $1 : NilImplicitNode.NIL;
                }

aref_args       : none
                | args trailer {
                    $$ = $1;
                }
                | args ',' assocs trailer {
                    $$ = support.arg_append($1, $3);
                }
                | assocs trailer {
                    $$ = support.newArrayNode($1.getPosition(), $1);
                }

paren_args      : tLPAREN2 opt_call_args rparen {
                    $$ = $2;
                    if ($$ != null) $<Node>$.setPosition($1);
                }

opt_paren_args  : none | paren_args

opt_call_args   : none 
                | call_args
                | args ',' {
                    $$ = $1;
                }
                | args ',' assocs ',' {
                    $$ = support.arg_append($1, $3);
                }
                | assocs ',' {
                    $$ = support.newArrayNode($1.getPosition(), $1);
                }
   

// [!null]
call_args       : command {
                    $$ = support.newArrayNode(support.getPosition($1), $1);
                }
                | args opt_block_arg {
                    $$ = support.arg_blk_pass($1, $2);
                }
                | assocs opt_block_arg {
                    $$ = support.newArrayNode($1.getPosition(), $1);
                    $$ = support.arg_blk_pass((Node)$$, $2);
                }
                | args ',' assocs opt_block_arg {
                    $$ = support.arg_append($1, $3);
                    $$ = support.arg_blk_pass((Node)$$, $4);
                }
                | block_arg {
                }

command_args    : /* none */ {
                    $$ = Long.valueOf(lexer.getCmdArgumentState().begin());
                } call_args {
                    lexer.getCmdArgumentState().reset($<Long>1.longValue());
                    $$ = $2;
                }

block_arg       : tAMPER arg_value {
                    $$ = new BlockPassNode(support.getPosition($2), $2);
                }

opt_block_arg   : ',' block_arg {
                    $$ = $2;
                }
                | none_block_pass

// [!null]
args            : arg_value { // ArrayNode
                    ISourcePosition pos = $1 == null ? lexer.getPosition() : $1.getPosition();
                    $$ = support.newArrayNode(pos, $1);
                }
                | tSTAR arg_value { // SplatNode
                    $$ = support.newSplatNode(support.getPosition($2), $2);
                }
                | args ',' arg_value { // ArgsCatNode, SplatNode, ArrayNode
                    Node node = support.splat_array($1);

                    if (node != null) {
                        $$ = support.list_append(node, $3);
                    } else {
                        $$ = support.arg_append($1, $3);
                    }
                }
                | args ',' tSTAR arg_value { // ArgsCatNode, SplatNode, ArrayNode
                    Node node = null;

                    // FIXME: lose syntactical elements here (and others like this)
                    if ($4 instanceof ArrayNode &&
                        (node = support.splat_array($1)) != null) {
                        $$ = support.list_concat(node, $4);
                    } else {
                        $$ = support.arg_concat(support.getPosition($1), $1, $4);
                    }
                }

mrhs_arg	: mrhs {
                    $$ = $1;
                }
		| arg_value {
                    $$ = $1;
                }


mrhs            : args ',' arg_value {
                    Node node = support.splat_array($1);

                    if (node != null) {
                        $$ = support.list_append(node, $3);
                    } else {
                        $$ = support.arg_append($1, $3);
                    }
                }
                | args ',' tSTAR arg_value {
                    Node node = null;

                    if ($4 instanceof ArrayNode &&
                        (node = support.splat_array($1)) != null) {
                        $$ = support.list_concat(node, $4);
                    } else {
                        $$ = support.arg_concat($1.getPosition(), $1, $4);
                    }
                }
                | tSTAR arg_value {
                     $$ = support.newSplatNode(support.getPosition($2), $2);
                }

primary         : literal
                | strings
                | xstring
                | regexp
                | words
                | qwords
                | symbols { 
                     $$ = $1; // FIXME: Why complaining without $$ = $1;
                }
                | qsymbols {
                     $$ = $1; // FIXME: Why complaining without $$ = $1;
                }
                | var_ref
                | backref
                | tFID {
                    $$ = support.new_fcall($1);
                }
                | kBEGIN bodystmt kEND {
                    $$ = new BeginNode($1, $2 == null ? NilImplicitNode.NIL : $2);
                }
                | tLPAREN_ARG {
                    lexer.setState(LexState.EXPR_ENDARG);
                } rparen {
                    $$ = null; //FIXME: Should be implicit nil?
                }
                | tLPAREN_ARG expr {
                    lexer.setState(LexState.EXPR_ENDARG); 
                } rparen {
                    if (Options.PARSER_WARN_GROUPED_EXPRESSIONS.load()) {
                      support.warning(ID.GROUPED_EXPRESSION, $1, "(...) interpreted as grouped expression");
                    }
                    $$ = $2;
                }
                | tLPAREN compstmt tRPAREN {
                    if ($2 != null) {
                        // compstmt position includes both parens around it
                        ((ISourcePositionHolder) $2).setPosition($1);
                        $$ = $2;
                    } else {
                        $$ = new NilNode($1);
                    }
                }
                | primary_value tCOLON2 tCONSTANT {
                    $$ = support.new_colon2(support.getPosition($1), $1, $3);
                }
                | tCOLON3 tCONSTANT {
                    $$ = support.new_colon3(lexer.getPosition(), $2);
                }
                | tLBRACK aref_args tRBRACK {
                    ISourcePosition position = support.getPosition($2);
                    if ($2 == null) {
                        $$ = new ZArrayNode(position); /* zero length array */
                    } else {
                        $$ = $2;
                    }
                }
                | tLBRACE assoc_list tRCURLY {
                    $$ = $2;
                }
                | kRETURN {
                    $$ = new ReturnNode($1, NilImplicitNode.NIL);
                }
                | kYIELD tLPAREN2 call_args rparen {
                    $$ = support.new_yield($1, $3);
                }
                | kYIELD tLPAREN2 rparen {
                    $$ = new YieldNode($1, null);
                }
                | kYIELD {
                    $$ = new YieldNode($1, null);
                }
                | kDEFINED opt_nl tLPAREN2 expr rparen {
                    $$ = support.new_defined($1, $4);
                }
                | kNOT tLPAREN2 expr rparen {
                    $$ = support.getOperatorCallNode(support.getConditionNode($3), "!");
                }
                | kNOT tLPAREN2 rparen {
                    $$ = support.getOperatorCallNode(NilImplicitNode.NIL, "!");
                }
                | fcall brace_block {
                    support.frobnicate_fcall_args($1, null, $2);
                    $$ = $1;                    
                }
                | method_call
                | method_call brace_block {
                    if ($1 != null && 
                          $<BlockAcceptingNode>1.getIterNode() instanceof BlockPassNode) {
                        throw new SyntaxException(PID.BLOCK_ARG_AND_BLOCK_GIVEN, $1.getPosition(), lexer.getCurrentLine(), "Both block arg and actual block given.");
                    }
                    $$ = $<BlockAcceptingNode>1.setIterNode($2);
                    $<Node>$.setPosition($1.getPosition());
                }
                | tLAMBDA lambda {
                    $$ = $2;
                }
                | kIF expr_value then compstmt if_tail kEND {
                    $$ = new IfNode($1, support.getConditionNode($2), $4, $5);
                }
                | kUNLESS expr_value then compstmt opt_else kEND {
                    $$ = new IfNode($1, support.getConditionNode($2), $5, $4);
                }
                | kWHILE {
                    lexer.getConditionState().begin();
                } expr_value do {
                    lexer.getConditionState().end();
                } compstmt kEND {
                    Node body = $6 == null ? NilImplicitNode.NIL : $6;
                    $$ = new WhileNode($1, support.getConditionNode($3), body);
                }
                | kUNTIL {
                  lexer.getConditionState().begin();
                } expr_value do {
                  lexer.getConditionState().end();
                } compstmt kEND {
                    Node body = $6 == null ? NilImplicitNode.NIL : $6;
                    $$ = new UntilNode($1, support.getConditionNode($3), body);
                }
                | kCASE expr_value opt_terms case_body kEND {
                    $$ = support.newCaseNode($1, $2, $4);
                }
                | kCASE opt_terms case_body kEND {
                    $$ = support.newCaseNode($1, null, $3);
                }
                | kFOR for_var kIN {
                    lexer.getConditionState().begin();
                } expr_value do {
                    lexer.getConditionState().end();
                } compstmt kEND {
                      // ENEBO: Lots of optz in 1.9 parser here
                    $$ = new ForNode($1, $2, $8, $5, support.getCurrentScope());
                }
                | kCLASS cpath superclass {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("class definition in method body");
                    }
                    support.pushLocalScope();
                } bodystmt kEND {
                    Node body = $5 == null ? NilImplicitNode.NIL : $5;

                    $$ = new ClassNode($1, $<Colon3Node>2, support.getCurrentScope(), body, $3);
                    support.popCurrentScope();
                }
                | kCLASS tLSHFT expr {
                    $$ = Boolean.valueOf(support.isInDef());
                    support.setInDef(false);
                } term {
                    $$ = Integer.valueOf(support.getInSingle());
                    support.setInSingle(0);
                    support.pushLocalScope();
                } bodystmt kEND {
                    Node body = $7 == null ? NilImplicitNode.NIL : $7;

                    $$ = new SClassNode($1, $3, support.getCurrentScope(), body);
                    support.popCurrentScope();
                    support.setInDef($<Boolean>4.booleanValue());
                    support.setInSingle($<Integer>6.intValue());
                }
                | kMODULE cpath {
                    if (support.isInDef() || support.isInSingle()) { 
                        support.yyerror("module definition in method body");
                    }
                    support.pushLocalScope();
                } bodystmt kEND {
                    Node body = $4 == null ? NilImplicitNode.NIL : $4;

                    $$ = new ModuleNode($1, $<Colon3Node>2, support.getCurrentScope(), body);
                    support.popCurrentScope();
                }
                | kDEF fname {
                    support.setInDef(true);
                    support.pushLocalScope();
                } f_arglist bodystmt kEND {
                    Node body = $5;
                    if (body == null) body = NilImplicitNode.NIL;

                    $$ = new DefnNode($1, new ArgumentNode($1, $2), (ArgsNode) $4, support.getCurrentScope(), body);
                    support.popCurrentScope();
                    support.setInDef(false);
                }
                | kDEF singleton dot_or_colon {
                    lexer.setState(LexState.EXPR_FNAME);
                } fname {
                    support.setInSingle(support.getInSingle() + 1);
                    support.pushLocalScope();
                    lexer.setState(LexState.EXPR_ENDFN); /* force for args */
                } f_arglist bodystmt kEND {
                    Node body = $8;
                    if (body == null) body = NilImplicitNode.NIL;

                    $$ = new DefsNode($1, $2, new ArgumentNode($1, $5), (ArgsNode) $7, support.getCurrentScope(), body);
                    support.popCurrentScope();
                    support.setInSingle(support.getInSingle() - 1);
                }
                | kBREAK {
                    $$ = new BreakNode($1, NilImplicitNode.NIL);
                }
                | kNEXT {
                    $$ = new NextNode($1, NilImplicitNode.NIL);
                }
                | kREDO {
                    $$ = new RedoNode($1);
                }
                | kRETRY {
                    $$ = new RetryNode($1);
                }

primary_value   : primary {
                    support.checkExpression($1);
                    $$ = $1;
                    if ($$ == null) $$ = NilImplicitNode.NIL;
                }

then            : term
                | kTHEN
                | term kTHEN

do              : term
                | kDO_COND

if_tail         : opt_else
                | kELSIF expr_value then compstmt if_tail {
                    $$ = new IfNode($1, support.getConditionNode($2), $4, $5);
                }

opt_else        : none
                | kELSE compstmt {
                    $$ = $2;
                }

for_var         : lhs
                | mlhs {
                }

f_marg          : f_norm_arg {
                     $$ = support.assignableLabelOrIdentifier($1, NilImplicitNode.NIL);
                }
                | tLPAREN f_margs rparen {
                    $$ = $2;
                }

// [!null]
f_marg_list     : f_marg {
                    $$ = support.newArrayNode($1.getPosition(), $1);
                }
                | f_marg_list ',' f_marg {
                    $$ = $1.add($3);
                }

f_margs         : f_marg_list {
                    $$ = new MultipleAsgnNode($1.getPosition(), $1, null, null);
                }
                | f_marg_list ',' tSTAR f_norm_arg {
                    $$ = new MultipleAsgnNode($1.getPosition(), $1, support.assignableLabelOrIdentifier($4, null), null);
                }
                | f_marg_list ',' tSTAR f_norm_arg ',' f_marg_list {
                    $$ = new MultipleAsgnNode($1.getPosition(), $1, support.assignableLabelOrIdentifier($4, null), $6);
                }
                | f_marg_list ',' tSTAR {
                    $$ = new MultipleAsgnNode($1.getPosition(), $1, new StarNode(lexer.getPosition()), null);
                }
                | f_marg_list ',' tSTAR ',' f_marg_list {
                    $$ = new MultipleAsgnNode($1.getPosition(), $1, new StarNode(lexer.getPosition()), $5);
                }
                | tSTAR f_norm_arg {
                    $$ = new MultipleAsgnNode(lexer.getPosition(), null, support.assignableLabelOrIdentifier($2, null), null);
                }
                | tSTAR f_norm_arg ',' f_marg_list {
                    $$ = new MultipleAsgnNode(lexer.getPosition(), null, support.assignableLabelOrIdentifier($2, null), $4);
                }
                | tSTAR {
                    $$ = new MultipleAsgnNode(lexer.getPosition(), null, new StarNode(lexer.getPosition()), null);
                }
                | tSTAR ',' f_marg_list {
                    $$ = new MultipleAsgnNode(support.getPosition($3), null, null, $3);
                }

block_args_tail : f_block_kwarg ',' f_kwrest opt_f_block_arg {
                    $$ = support.new_args_tail($1.getPosition(), $1, $3, $4);
                }
                | f_block_kwarg opt_f_block_arg {
                    $$ = support.new_args_tail($1.getPosition(), $1, null, $2);
                }
                | f_kwrest opt_f_block_arg {
                    $$ = support.new_args_tail(lexer.getPosition(), null, $1, $2);
                }
                | f_block_arg {
                    $$ = support.new_args_tail($1.getPosition(), null, null, $1);
                }

opt_block_args_tail : ',' block_args_tail {
                    $$ = $2;
                }
                | /* none */ {
                    $$ = support.new_args_tail(lexer.getPosition(), null, null, null);
                }

// [!null]
block_param     : f_arg ',' f_block_optarg ',' f_rest_arg opt_block_args_tail {
                    $$ = support.new_args($1.getPosition(), $1, $3, $5, null, $6);
                }
                | f_arg ',' f_block_optarg ',' f_rest_arg ',' f_arg opt_block_args_tail {
                    $$ = support.new_args($1.getPosition(), $1, $3, $5, $7, $8);
                }
                | f_arg ',' f_block_optarg opt_block_args_tail {
                    $$ = support.new_args($1.getPosition(), $1, $3, null, null, $4);
                }
                | f_arg ',' f_block_optarg ',' f_arg opt_block_args_tail {
                    $$ = support.new_args($1.getPosition(), $1, $3, null, $5, $6);
                }
                | f_arg ',' f_rest_arg opt_block_args_tail {
                    $$ = support.new_args($1.getPosition(), $1, null, $3, null, $4);
                }
                | f_arg ',' {
                    RestArgNode rest = new UnnamedRestArgNode($1.getPosition(), null, support.getCurrentScope().addVariable("*"));
                    $$ = support.new_args($1.getPosition(), $1, null, rest, null, (ArgsTailHolder) null);
                }
                | f_arg ',' f_rest_arg ',' f_arg opt_block_args_tail {
                    $$ = support.new_args($1.getPosition(), $1, null, $3, $5, $6);
                }
                | f_arg opt_block_args_tail {
                    $$ = support.new_args($1.getPosition(), $1, null, null, null, $2);
                }
                | f_block_optarg ',' f_rest_arg opt_block_args_tail {
                    $$ = support.new_args(support.getPosition($1), null, $1, $3, null, $4);
                }
                | f_block_optarg ',' f_rest_arg ',' f_arg opt_block_args_tail {
                    $$ = support.new_args(support.getPosition($1), null, $1, $3, $5, $6);
                }
                | f_block_optarg opt_block_args_tail {
                    $$ = support.new_args(support.getPosition($1), null, $1, null, null, $2);
                }
                | f_block_optarg ',' f_arg opt_block_args_tail {
                    $$ = support.new_args($1.getPosition(), null, $1, null, $3, $4);
                }
                | f_rest_arg opt_block_args_tail {
                    $$ = support.new_args($1.getPosition(), null, null, $1, null, $2);
                }
                | f_rest_arg ',' f_arg opt_block_args_tail {
                    $$ = support.new_args($1.getPosition(), null, null, $1, $3, $4);
                }
                | block_args_tail {
                    $$ = support.new_args($1.getPosition(), null, null, null, null, $1);
                }

opt_block_param : none {
    // was $$ = null;
                    $$ = support.new_args(lexer.getPosition(), null, null, null, null, (ArgsTailHolder) null);
                }
                | block_param_def {
                    lexer.commandStart = true;
                    $$ = $1;
                }

block_param_def : tPIPE opt_bv_decl tPIPE {
                    $$ = support.new_args(lexer.getPosition(), null, null, null, null, (ArgsTailHolder) null);
                }
                | tOROP {
                    $$ = support.new_args(lexer.getPosition(), null, null, null, null, (ArgsTailHolder) null);
                }
                | tPIPE block_param opt_bv_decl tPIPE {
                    $$ = $2;
                }

// shadowed block variables....
opt_bv_decl     : opt_nl {
                    $$ = null;
                }
                | opt_nl ';' bv_decls opt_nl {
                    $$ = null;
                }

// ENEBO: This is confusing...
bv_decls        : bvar {
                    $$ = null;
                }
                | bv_decls ',' bvar {
                    $$ = null;
                }

bvar            : tIDENTIFIER {
                    support.new_bv($1);
                }
                | f_bad_arg {
                    $$ = null;
                }

lambda          : /* none */  {
                    support.pushBlockScope();
                    $$ = lexer.getLeftParenBegin();
                    lexer.setLeftParenBegin(lexer.incrementParenNest());
                } f_larglist lambda_body {
                    $$ = new LambdaNode($2.getPosition(), $2, $3, support.getCurrentScope());
                    support.popCurrentScope();
                    lexer.setLeftParenBegin($<Integer>1);
                }

f_larglist      : tLPAREN2 f_args opt_bv_decl tRPAREN {
                    $$ = $2;
                }
                | f_args {
                    $$ = $1;
                }

lambda_body     : tLAMBEG compstmt tRCURLY {
                    $$ = $2;
                }
                | kDO_LAMBDA compstmt kEND {
                    $$ = $2;
                }

do_block        : kDO_BLOCK {
                    support.pushBlockScope();
                } opt_block_param compstmt kEND {
                    $$ = new IterNode($1, $3, $4, support.getCurrentScope());
                    support.popCurrentScope();
                }

  // JRUBY-2326 and GH #305 both end up hitting this production whereas in
  // MRI these do not.  I have never isolated the cause but I can work around
  // the individual reported problems with a few extra conditionals in this
  // first production
block_call      : command do_block {
                    // Workaround for JRUBY-2326 (MRI does not enter this production for some reason)
                    if ($1 instanceof YieldNode) {
                        throw new SyntaxException(PID.BLOCK_GIVEN_TO_YIELD, $1.getPosition(), lexer.getCurrentLine(), "block given to yield");
                    }
                    if ($1 instanceof BlockAcceptingNode && $<BlockAcceptingNode>1.getIterNode() instanceof BlockPassNode) {
                        throw new SyntaxException(PID.BLOCK_ARG_AND_BLOCK_GIVEN, $1.getPosition(), lexer.getCurrentLine(), "Both block arg and actual block given.");
                    }
                    if ($1 instanceof NonLocalControlFlowNode) {
                        ((BlockAcceptingNode) $<NonLocalControlFlowNode>1.getValueNode()).setIterNode($2);
                    } else {
                        $<BlockAcceptingNode>1.setIterNode($2);
                    }
                    $$ = $1;
                    $<Node>$.setPosition($1.getPosition());
                }
                | block_call dot_or_colon operation2 opt_paren_args {
                    $$ = support.new_call($1, $3, $4, null);
                }
                | block_call dot_or_colon operation2 opt_paren_args brace_block {
                    $$ = support.new_call($1, $3, $4, $5);
                }
                | block_call dot_or_colon operation2 command_args do_block {
                    $$ = support.new_call($1, $3, $4, $5);
                }

// [!null]
method_call     : fcall paren_args {
                    support.frobnicate_fcall_args($1, $2, null);
                    $$ = $1;
                }
                | primary_value tDOT operation2 opt_paren_args {
                    $$ = support.new_call($1, $3, $4, null);
                }
                | primary_value tCOLON2 operation2 paren_args {
                    $$ = support.new_call($1, $3, $4, null);
                }
                | primary_value tCOLON2 operation3 {
                    $$ = support.new_call($1, $3, null, null);
                }
                | primary_value tDOT paren_args {
                    $$ = support.new_call($1, "call", $3, null);
                }
                | primary_value tCOLON2 paren_args {
                    $$ = support.new_call($1, "call", $3, null);
                }
                | kSUPER paren_args {
                    $$ = support.new_super($1, $2);
                }
                | kSUPER {
                    $$ = new ZSuperNode($1);
                }
                | primary_value '[' opt_call_args rbracket {
                    if ($1 instanceof SelfNode) {
                        $$ = support.new_fcall("[]");
                        support.frobnicate_fcall_args($<FCallNode>$, $3, null);
                    } else {
                        $$ = support.new_call($1, "[]", $3, null);
                    }
                }

brace_block     : tLCURLY {
                    support.pushBlockScope();
                } opt_block_param compstmt tRCURLY {
                    $$ = new IterNode($1, $3, $4, support.getCurrentScope());
                    support.popCurrentScope();
                }
                | kDO {
                    support.pushBlockScope();
                } opt_block_param compstmt kEND {
                    $$ = new IterNode($1, $3, $4, support.getCurrentScope());
                    support.popCurrentScope();
                }

case_body       : kWHEN args then compstmt cases {
                    $$ = support.newWhenNode($1, $2, $4, $5);
                }

cases           : opt_else | case_body

opt_rescue      : kRESCUE exc_list exc_var then compstmt opt_rescue {
                    Node node;
                    if ($3 != null) {
                        node = support.appendToBlock(support.node_assign($3, new GlobalVarNode($1, "$!")), $5);
                        if ($5 != null) {
                            node.setPosition($1);
                        }
                    } else {
                        node = $5;
                    }
                    Node body = node == null ? NilImplicitNode.NIL : node;
                    $$ = new RescueBodyNode($1, $2, body, $6);
                }
                | { 
                    $$ = null; 
                }

exc_list        : arg_value {
                    $$ = support.newArrayNode($1.getPosition(), $1);
                }
                | mrhs {
                    $$ = support.splat_array($1);
                    if ($$ == null) $$ = $1; // ArgsCat or ArgsPush
                }
                | none

exc_var         : tASSOC lhs {
                    $$ = $2;
                }
                | none

opt_ensure      : kENSURE compstmt {
                    $$ = $2;
                }
                | none

literal         : numeric {
                    $$ = $1;
                }
                | symbol {
                    $$ = new SymbolNode(lexer.getPosition(), new ByteList($1.getBytes(), lexer.getEncoding()));
                }
                | dsym

strings         : string {
                    $$ = $1 instanceof EvStrNode ? new DStrNode($1.getPosition(), lexer.getEncoding()).add($1) : $1;
                    /*
                    NODE *node = $1;
                    if (!node) {
                        node = NEW_STR(STR_NEW0());
                    } else {
                        node = evstr2dstr(node);
                    }
                    $$ = node;
                    */
                }

// [!null]
string          : tCHAR {
                    $$ = $1;
                }
                | string1 {
                    $$ = $1;
                }
                | string string1 {
                    $$ = support.literal_concat($1.getPosition(), $1, $2);
                }

string1         : tSTRING_BEG string_contents tSTRING_END {
                    $$ = $2;
                }

xstring         : tXSTRING_BEG xstring_contents tSTRING_END {
                    ISourcePosition position = support.getPosition($2);

                    if ($2 == null) {
                        $$ = new XStrNode(position, null, StringSupport.CR_7BIT);
                    } else if ($2 instanceof StrNode) {
                        $$ = new XStrNode(position, (ByteList) $<StrNode>2.getValue().clone(), $<StrNode>2.getCodeRange());
                    } else if ($2 instanceof DStrNode) {
                        $$ = new DXStrNode(position, $<DStrNode>2);

                        $<Node>$.setPosition(position);
                    } else {
                        $$ = new DXStrNode(position).add($2);
                    }
                }

regexp          : tREGEXP_BEG regexp_contents tREGEXP_END {
                    $$ = support.newRegexpNode(support.getPosition($2), $2, (RegexpNode) $3);
                }

words           : tWORDS_BEG ' ' tSTRING_END {
                    $$ = new ZArrayNode(lexer.getPosition());
                }
                | tWORDS_BEG word_list tSTRING_END {
                    $$ = $2;
                }

word_list       : /* none */ {
                    $$ = new ArrayNode(lexer.getPosition());
                }
                | word_list word ' ' {
                     $$ = $1.add($2 instanceof EvStrNode ? new DStrNode($1.getPosition(), lexer.getEncoding()).add($2) : $2);
                }

word            : string_content {
                     $$ = $<Node>1;
                }
                | word string_content {
                     $$ = support.literal_concat(support.getPosition($1), $1, $<Node>2);
                }

symbols         : tSYMBOLS_BEG ' ' tSTRING_END {
                    $$ = new ArrayNode(lexer.getPosition());
                }
                | tSYMBOLS_BEG symbol_list tSTRING_END {
                    $$ = $2;
                }

symbol_list     : /* none */ {
                    $$ = new ArrayNode(lexer.getPosition());
                }
                | symbol_list word ' ' {
                    $$ = $1.add($2 instanceof EvStrNode ? new DSymbolNode($1.getPosition()).add($2) : support.asSymbol($1.getPosition(), $2));
                }

qwords          : tQWORDS_BEG ' ' tSTRING_END {
                     $$ = new ZArrayNode(lexer.getPosition());
                }
                | tQWORDS_BEG qword_list tSTRING_END {
                    $$ = $2;
                }

qsymbols        : tQSYMBOLS_BEG ' ' tSTRING_END {
                    $$ = new ZArrayNode(lexer.getPosition());
                }
                | tQSYMBOLS_BEG qsym_list tSTRING_END {
                    $$ = $2;
                }


qword_list      : /* none */ {
                    $$ = new ArrayNode(lexer.getPosition());
                }
                | qword_list tSTRING_CONTENT ' ' {
                    $$ = $1.add($2);
                }

qsym_list      : /* none */ {
                    $$ = new ArrayNode(lexer.getPosition());
                }
                | qsym_list tSTRING_CONTENT ' ' {
                    $$ = $1.add(support.asSymbol($1.getPosition(), $2));
                }

string_contents : /* none */ {
                    ByteList aChar = ByteList.create("");
                    aChar.setEncoding(lexer.getEncoding());
                    $$ = lexer.createStr(aChar, 0);
                }
                | string_contents string_content {
                    $$ = support.literal_concat($1.getPosition(), $1, $<Node>2);
                }

xstring_contents: /* none */ {
                    $$ = null;
                }
                | xstring_contents string_content {
                    $$ = support.literal_concat(support.getPosition($1), $1, $<Node>2);
                }

regexp_contents :  /* none */ {
                    $$ = null;
                }
                | regexp_contents string_content {
    // FIXME: mri is different here.
                    $$ = support.literal_concat(support.getPosition($1), $1, $<Node>2);
                }

string_content  : tSTRING_CONTENT {
                    $$ = $1;
                }
                | tSTRING_DVAR {
                    $$ = lexer.getStrTerm();
                    lexer.setStrTerm(null);
                    lexer.setState(LexState.EXPR_BEG);
                } string_dvar {
                    lexer.setStrTerm($<StrTerm>2);
                    $$ = new EvStrNode(support.getPosition($3), $3);
                }
                | tSTRING_DBEG {
                   $$ = lexer.getStrTerm();
                   lexer.setStrTerm(null);
                   lexer.getConditionState().stop();
                   lexer.getCmdArgumentState().stop();
                } {
                   $$ = lexer.getState();
                   lexer.setState(LexState.EXPR_BEG);
                } compstmt tRCURLY {
                   lexer.getConditionState().restart();
                   lexer.getCmdArgumentState().restart();
                   lexer.setStrTerm($<StrTerm>2);
                   lexer.setState($<LexState>3);

                   $$ = support.newEvStrNode(support.getPosition($4), $4);
                }

string_dvar     : tGVAR {
                     $$ = new GlobalVarNode(lexer.getPosition(), $1);
                }
                | tIVAR {
                     $$ = new InstVarNode(lexer.getPosition(), $1);
                }
                | tCVAR {
                     $$ = new ClassVarNode(lexer.getPosition(), $1);
                }
                | backref

// String:symbol
symbol          : tSYMBEG sym {
                     lexer.setState(LexState.EXPR_END);
                     $$ = $2;
                }

// String:symbol
sym             : fname | tIVAR | tGVAR | tCVAR

dsym            : tSYMBEG xstring_contents tSTRING_END {
                     lexer.setState(LexState.EXPR_END);

                     // DStrNode: :"some text #{some expression}"
                     // StrNode: :"some text"
                     // EvStrNode :"#{some expression}"
                     // Ruby 1.9 allows empty strings as symbols
                     if ($2 == null) {
                         $$ = new SymbolNode(lexer.getPosition(), new ByteList(new byte[0], lexer.getEncoding()));
                     } else if ($2 instanceof DStrNode) {
                         $$ = new DSymbolNode($2.getPosition(), $<DStrNode>2);
                     } else if ($2 instanceof StrNode) {
                         $$ = new SymbolNode($2.getPosition(), $<StrNode>2.getValue());
                     } else {
                         $$ = new DSymbolNode($2.getPosition());
                         $<DSymbolNode>$.add($2);
                     }
                }

 numeric        : simple_numeric {
                    $$ = $1;  
                }
                | tUMINUS_NUM simple_numeric %prec tLOWEST {
                     $$ = support.negateNumeric($2);
                }

simple_numeric  : tINTEGER {
                    $$ = $1;
                }
                | tFLOAT {
                     $$ = $1;
                }
                | tRATIONAL {
                     $$ = $1;
                }
                | tIMAGINARY {
                     $$ = $1;
                }

// [!null]
var_ref         : /*mri:user_variable*/ tIDENTIFIER {
                    $$ = support.declareIdentifier($1);
                }
                | tIVAR {
                    $$ = new InstVarNode(lexer.getPosition(), $1);
                }
                | tGVAR {
                    $$ = new GlobalVarNode(lexer.getPosition(), $1);
                }
                | tCONSTANT {
                    $$ = new ConstNode(lexer.getPosition(), $1);
                }
                | tCVAR {
                    $$ = new ClassVarNode(lexer.getPosition(), $1);
                } /*mri:user_variable*/
                | /*mri:keyword_variable*/ kNIL { 
                    $$ = new NilNode(lexer.getPosition());
                }
                | kSELF {
                    $$ = new SelfNode(lexer.getPosition());
                }
                | kTRUE { 
                    $$ = new TrueNode(lexer.getPosition());
                }
                | kFALSE {
                    $$ = new FalseNode(lexer.getPosition());
                }
                | k__FILE__ {
                    $$ = new FileNode(lexer.getPosition(), new ByteList(lexer.getPosition().getFile().getBytes(),
                    support.getConfiguration().getRuntime().getEncodingService().getLocaleEncoding()));
                }
                | k__LINE__ {
                    $$ = new FixnumNode(lexer.getPosition(), lexer.tokline.getLine()+1);
                }
                | k__ENCODING__ {
                    $$ = new EncodingNode(lexer.getPosition(), lexer.getEncoding());
                } /*mri:keyword_variable*/

// [!null]
var_lhs         : /*mri:user_variable*/ tIDENTIFIER {
                    $$ = support.assignableLabelOrIdentifier($1, null);
                }
                | tIVAR {
                   $$ = new InstAsgnNode(lexer.getPosition(), $1, NilImplicitNode.NIL);
                }
                | tGVAR {
                   $$ = new GlobalAsgnNode(lexer.getPosition(), $1, NilImplicitNode.NIL);
                }
                | tCONSTANT {
                    if (support.isInDef() || support.isInSingle()) support.compile_error("dynamic constant assignment");

                    $$ = new ConstDeclNode(lexer.getPosition(), $1, null, NilImplicitNode.NIL);
                }
                | tCVAR {
                    $$ = new ClassVarAsgnNode(lexer.getPosition(), $1, NilImplicitNode.NIL);
                } /*mri:user_variable*/
                | /*mri:keyword_variable*/ kNIL {
                    support.compile_error("Can't assign to nil");
                    $$ = null;
                }
                | kSELF {
                    support.compile_error("Can't change the value of self");
                    $$ = null;
                }
                | kTRUE {
                    support.compile_error("Can't assign to true");
                    $$ = null;
                }
                | kFALSE {
                    support.compile_error("Can't assign to false");
                    $$ = null;
                }
                | k__FILE__ {
                    support.compile_error("Can't assign to __FILE__");
                    $$ = null;
                }
                | k__LINE__ {
                    support.compile_error("Can't assign to __LINE__");
                    $$ = null;
                }
                | k__ENCODING__ {
                    support.compile_error("Can't assign to __ENCODING__");
                    $$ = null;
                } /*mri:keyword_variable*/

// [!null]
backref         : tNTH_REF {
                    $$ = $1;
                }
                | tBACK_REF {
                    $$ = $1;
                }

superclass      : term {
                    $$ = null;
                }
                | tLT {
                   lexer.setState(LexState.EXPR_BEG);
                } expr_value term {
                    $$ = $3;
                }
                | error term {
                   $$ = null;
                }

// [!null]
// ENEBO: Look at command_start stuff I am ripping out
f_arglist       : tLPAREN2 f_args rparen {
                    $$ = $2;
                    lexer.setState(LexState.EXPR_BEG);
                    lexer.commandStart = true;
                }
                | {
                   $$ = lexer.inKwarg;
                   lexer.inKwarg = true;
                } f_args term {
                   lexer.inKwarg = $<Boolean>1;
                    $$ = $2;
                    lexer.setState(LexState.EXPR_BEG);
                    lexer.commandStart = true;
                }


args_tail       : f_kwarg ',' f_kwrest opt_f_block_arg {
                    $$ = support.new_args_tail($1.getPosition(), $1, $3, $4);
                }
                | f_kwarg opt_f_block_arg {
                    $$ = support.new_args_tail($1.getPosition(), $1, null, $2);
                }
                | f_kwrest opt_f_block_arg {
                    $$ = support.new_args_tail(lexer.getPosition(), null, $1, $2);
                }
                | f_block_arg {
                    $$ = support.new_args_tail($1.getPosition(), null, null, $1);
                }

opt_args_tail   : ',' args_tail {
                    $$ = $2;
                }
                | /* none */ {
                    $$ = support.new_args_tail(lexer.getPosition(), null, null, null);
                }

// [!null]
f_args          : f_arg ',' f_optarg ',' f_rest_arg opt_args_tail {
                    $$ = support.new_args($1.getPosition(), $1, $3, $5, null, $6);
                }
                | f_arg ',' f_optarg ',' f_rest_arg ',' f_arg opt_args_tail {
                    $$ = support.new_args($1.getPosition(), $1, $3, $5, $7, $8);
                }
                | f_arg ',' f_optarg opt_args_tail {
                    $$ = support.new_args($1.getPosition(), $1, $3, null, null, $4);
                }
                | f_arg ',' f_optarg ',' f_arg opt_args_tail {
                    $$ = support.new_args($1.getPosition(), $1, $3, null, $5, $6);
                }
                | f_arg ',' f_rest_arg opt_args_tail {
                    $$ = support.new_args($1.getPosition(), $1, null, $3, null, $4);
                }
                | f_arg ',' f_rest_arg ',' f_arg opt_args_tail {
                    $$ = support.new_args($1.getPosition(), $1, null, $3, $5, $6);
                }
                | f_arg opt_args_tail {
                    $$ = support.new_args($1.getPosition(), $1, null, null, null, $2);
                }
                | f_optarg ',' f_rest_arg opt_args_tail {
                    $$ = support.new_args($1.getPosition(), null, $1, $3, null, $4);
                }
                | f_optarg ',' f_rest_arg ',' f_arg opt_args_tail {
                    $$ = support.new_args($1.getPosition(), null, $1, $3, $5, $6);
                }
                | f_optarg opt_args_tail {
                    $$ = support.new_args($1.getPosition(), null, $1, null, null, $2);
                }
                | f_optarg ',' f_arg opt_args_tail {
                    $$ = support.new_args($1.getPosition(), null, $1, null, $3, $4);
                }
                | f_rest_arg opt_args_tail {
                    $$ = support.new_args($1.getPosition(), null, null, $1, null, $2);
                }
                | f_rest_arg ',' f_arg opt_args_tail {
                    $$ = support.new_args($1.getPosition(), null, null, $1, $3, $4);
                }
                | args_tail {
                    $$ = support.new_args($1.getPosition(), null, null, null, null, $1);
                }
                | /* none */ {
                    $$ = support.new_args(lexer.getPosition(), null, null, null, null, (ArgsTailHolder) null);
                }

f_bad_arg       : tCONSTANT {
                    support.yyerror("formal argument cannot be a constant");
                }
                | tIVAR {
                    support.yyerror("formal argument cannot be an instance variable");
                }
                | tGVAR {
                    support.yyerror("formal argument cannot be a global variable");
                }
                | tCVAR {
                    support.yyerror("formal argument cannot be a class variable");
                }

// String:f_norm_arg [!null]
f_norm_arg      : f_bad_arg
                | tIDENTIFIER {
                    $$ = support.formal_argument($1);
                }

f_arg_item      : f_norm_arg {
                    $$ = support.arg_var($1);
                }
                | tLPAREN f_margs rparen {
                    $$ = $2;
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
                }

// [!null]
f_arg           : f_arg_item {
                    $$ = new ArrayNode(lexer.getPosition(), $1);
                }
                | f_arg ',' f_arg_item {
                    $1.add($3);
                    $$ = $1;
                }

f_label 	: tLABEL {
                    support.arg_var(support.formal_argument($1));
                    $$ = $1;
                }

f_kw            : f_label arg_value {
                    $$ = support.keyword_arg($2.getPosition(), support.assignableLabelOrIdentifier($1, $2));
                }
                | f_label {
                    $$ = support.keyword_arg(lexer.getPosition(), support.assignableLabelOrIdentifier($1, new RequiredKeywordArgumentValueNode()));
                }

f_block_kw      : f_label primary_value {
                    $$ = support.keyword_arg(support.getPosition($2), support.assignableLabelOrIdentifier($1, $2));
                }
                | f_label {
                    $$ = support.keyword_arg(lexer.getPosition(), support.assignableLabelOrIdentifier($1, new RequiredKeywordArgumentValueNode()));
                }
             

f_block_kwarg   : f_block_kw {
                    $$ = new ArrayNode($1.getPosition(), $1);
                }
                | f_block_kwarg ',' f_block_kw {
                    $$ = $1.add($3);
                }

f_kwarg         : f_kw {
                    $$ = new ArrayNode($1.getPosition(), $1);
                }
                | f_kwarg ',' f_kw {
                    $$ = $1.add($3);
                }

kwrest_mark     : tPOW {
                    $$ = $1;
                }
                | tDSTAR {
                    $$ = $1;
                }

f_kwrest        : kwrest_mark tIDENTIFIER {
                    support.shadowing_lvar($2);
                    $$ = $2;
                }
                | kwrest_mark {
                    $$ = support.internalId();
                }

f_opt           : f_norm_arg '=' arg_value {
                    support.arg_var($1);
                    $$ = new OptArgNode(support.getPosition($3), support.assignableLabelOrIdentifier($1, $3));
                }

f_block_opt     : tIDENTIFIER '=' primary_value {
                    support.arg_var(support.formal_argument($1));
                    $$ = new OptArgNode(support.getPosition($3), support.assignableLabelOrIdentifier($1, $3));
                }

f_block_optarg  : f_block_opt {
                    $$ = new BlockNode($1.getPosition()).add($1);
                }
                | f_block_optarg ',' f_block_opt {
                    $$ = support.appendToBlock($1, $3);
                }

f_optarg        : f_opt {
                    $$ = new BlockNode($1.getPosition()).add($1);
                }
                | f_optarg ',' f_opt {
                    $$ = support.appendToBlock($1, $3);
                }

restarg_mark    : tSTAR2 | tSTAR

// [!null]
f_rest_arg      : restarg_mark tIDENTIFIER {
                    if (!support.is_local_id($2)) {
                        support.yyerror("rest argument must be local variable");
                    }
                    
                    $$ = new RestArgNode(support.arg_var(support.shadowing_lvar($2)));
                }
                | restarg_mark {
                    $$ = new UnnamedRestArgNode(lexer.getPosition(), "", support.getCurrentScope().addVariable("*"));
                }

// [!null]
blkarg_mark     : tAMPER2 | tAMPER

// f_block_arg - Block argument def for function (foo(&block)) [!null]
f_block_arg     : blkarg_mark tIDENTIFIER {
                    if (!support.is_local_id($2)) {
                        support.yyerror("block argument must be local variable");
                    }
                    
                    $$ = new BlockArgNode(support.arg_var(support.shadowing_lvar($2)));
                }

opt_f_block_arg : ',' f_block_arg {
                    $$ = $2;
                }
                | /* none */ {
                    $$ = null;
                }

singleton       : var_ref {
                    if (!($1 instanceof SelfNode)) {
                        support.checkExpression($1);
                    }
                    $$ = $1;
                }
                | tLPAREN2 {
                    lexer.setState(LexState.EXPR_BEG);
                } expr rparen {
                    if ($3 == null) {
                        support.yyerror("can't define single method for ().");
                    } else if ($3 instanceof ILiteralNode) {
                        support.yyerror("can't define single method for literals.");
                    }
                    support.checkExpression($3);
                    $$ = $3;
                }

// HashNode: [!null]
assoc_list      : none {
                    $$ = new HashNode(lexer.getPosition());
                }
                | assocs trailer {
                    $$ = $1;
                }

// [!null]
assocs          : assoc {
                    $$ = new HashNode(lexer.getPosition(), $1);
                }
                | assocs ',' assoc {
                    $$ = $1.add($3);
                }

// Cons: [!null]
assoc           : arg_value tASSOC arg_value {
                    $$ = new KeyValuePair<Node,Node>($1, $3);
                }
                | tLABEL arg_value {
                    SymbolNode label = new SymbolNode(support.getPosition($2), new ByteList($1.getBytes(), lexer.getEncoding()));
                    $$ = new KeyValuePair<Node,Node>(label, $2);
                }
                | tSTRING_BEG string_contents tLABEL_END arg_value {
                    if ($2 instanceof StrNode) {
                        DStrNode dnode = new DStrNode(support.getPosition($2), lexer.getEncoding());
                        dnode.add($2);
                        $$ = new KeyValuePair<Node,Node>(new DSymbolNode(support.getPosition($2), dnode), $4);
                    } else if ($2 instanceof DStrNode) {
                        $$ = new KeyValuePair<Node,Node>(new DSymbolNode(support.getPosition($2), $<DStrNode>2), $4);
                    } else {
                        support.compile_error("Uknown type for assoc in strings: " + $2);
                    }

                }

                | tDSTAR arg_value {
                    $$ = new KeyValuePair<Node,Node>(null, $2);
                }

operation       : tIDENTIFIER | tCONSTANT | tFID
operation2      : tIDENTIFIER | tCONSTANT | tFID | op
operation3      : tIDENTIFIER | tFID | op
dot_or_colon    : tDOT | tCOLON2
opt_terms       : /* none */ | terms
opt_nl          : /* none */ | '\n'
rparen          : opt_nl tRPAREN {
                    $$ = $2;
                }
rbracket        : opt_nl tRBRACK {
                    $$ = $2;
                }
trailer         : /* none */ | '\n' | ','

term            : ';'
                | '\n'

terms           : term
                | terms ';'

none            : /* none */ {
                      $$ = null;
                }

none_block_pass : /* none */ {  
                  $$ = null;
                }

%%

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
