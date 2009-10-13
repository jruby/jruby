%{
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
 * Copyright (C) 2008-2009 Thomas E Enebo <enebo@acm.org>
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
import org.jruby.ast.Hash19Node;
import org.jruby.ast.IfNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.LambdaNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgn19Node;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.NilImplicitNode;
import org.jruby.ast.NilNode;
import org.jruby.ast.Node;
import org.jruby.ast.NotNode;
import org.jruby.ast.OpAsgnAndNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OptArgNode;
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
import org.jruby.ast.ZYieldNode;
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

public class Ruby19Parser implements RubyParser {
    protected ParserSupport19 support;
    protected RubyYaccLexer lexer;
    protected IRubyWarnings warnings;

    public Ruby19Parser() {
        this(new ParserSupport19());
    }

    public Ruby19Parser(ParserSupport19 support) {
        this.support = support;
        lexer = new RubyYaccLexer(false);
        lexer.setParserSupport(support);
        support.setLexer(lexer);
    }

    public void setWarnings(IRubyWarnings warnings) {
        this.warnings = warnings;

        support.setWarnings(warnings);
        lexer.setWarnings(warnings);
    }
%}

// We need to make sure we have same tokens in the same order and up
// front so 1.8 and 1.9 parser can use the same Tokens.java file.
%token <Token> kCLASS kMODULE kDEF kUNDEF kBEGIN kRESCUE kENSURE kEND kIF
  kUNLESS kTHEN kELSIF kELSE kCASE kWHEN kWHILE kUNTIL kFOR kBREAK kNEXT
  kREDO kRETRY kIN kDO kDO_COND kDO_BLOCK kRETURN kYIELD kSUPER kSELF kNIL
  kTRUE kFALSE kAND kOR kNOT kIF_MOD kUNLESS_MOD kWHILE_MOD kUNTIL_MOD
  kRESCUE_MOD kALIAS kDEFINED klBEGIN klEND k__LINE__ k__FILE__
  k__ENCODING__ kDO_LAMBDA 

%token <Token> tIDENTIFIER tFID tGVAR tIVAR tCONSTANT tCVAR tLABEL tCHAR
%type <Token> variable
%type <Token> fitem sym symbol operation operation2 operation3 cname fname op 
%type <Token> f_norm_arg dot_or_colon restarg_mark blkarg_mark
%token <Token> tUPLUS         /* unary+ */
%token <Token> tUMINUS        /* unary- */
%token <Token> tUMINUS_NUM    /* unary- */
%token <Token> tPOW           /* ** */
%token <Token> tCMP           /* <=> */
%token <Token> tEQ            /* == */
%token <Token> tEQQ           /* === */
%token <Token> tNEQ           /* != */
%token <Token> tGEQ           /* >= */
%token <Token> tLEQ           /* <= */
%token <Token> tANDOP tOROP   /* && and || */
%token <Token> tMATCH tNMATCH /* =~ and !~ */
%token <Token>  tDOT           /* Is just '.' in ruby and not a token */
%token <Token> tDOT2 tDOT3    /* .. and ... */
%token <Token> tAREF tASET    /* [] and []= */
%token <Token> tLSHFT tRSHFT  /* << and >> */
%token <Token> tCOLON2        /* :: */
%token <Token> tCOLON3        /* :: at EXPR_BEG */
%token <Token> tOP_ASGN       /* +=, -=  etc. */
%token <Token> tASSOC         /* => */
%token <Token> tLPAREN        /* ( */
%token <Token> tLPAREN2        /* ( Is just '(' in ruby and not a token */
%token <Token> tRPAREN        /* ) */
%token <Token> tLPAREN_ARG    /* ( */
%token <Token> tLBRACK        /* [ */
%token <Token> tRBRACK        /* ] */
%token <Token> tLBRACE        /* { */
%token <Token> tLBRACE_ARG    /* { */
%token <Token> tSTAR          /* * */
%token <Token> tSTAR2         /* *  Is just '*' in ruby and not a token */
%token <Token> tAMPER         /* & */
%token <Token> tAMPER2        /* &  Is just '&' in ruby and not a token */
%token <Token> tTILDE         /* ` is just '`' in ruby and not a token */
%token <Token> tPERCENT       /* % is just '%' in ruby and not a token */
%token <Token> tDIVIDE        /* / is just '/' in ruby and not a token */
%token <Token> tPLUS          /* + is just '+' in ruby and not a token */
%token <Token> tMINUS         /* - is just '-' in ruby and not a token */
%token <Token> tLT            /* < is just '<' in ruby and not a token */
%token <Token> tGT            /* > is just '>' in ruby and not a token */
%token <Token> tPIPE          /* | is just '|' in ruby and not a token */
%token <Token> tBANG          /* ! is just '!' in ruby and not a token */
%token <Token> tCARET         /* ^ is just '^' in ruby and not a token */
%token <Token> tLCURLY        /* { is just '{' in ruby and not a token */
%token <Token> tRCURLY        /* } is just '}' in ruby and not a token */
%token <Token> tBACK_REF2     /* { is just '`' in ruby and not a token */
%token <Token> tSYMBEG tSTRING_BEG tXSTRING_BEG tREGEXP_BEG tWORDS_BEG tQWORDS_BEG
%token <Token> tSTRING_DBEG tSTRING_DVAR tSTRING_END
%token <Token> tLAMBDA tLAMBEG
%token <Node> tNTH_REF tBACK_REF tSTRING_CONTENT tINTEGER
%token <FloatNode> tFLOAT  
%token <RegexpNode>  tREGEXP_END
%type <RestArgNode> f_rest_arg 
%type <Node> singleton strings string string1 xstring regexp
%type <Node> string_contents xstring_contents string_content method_call
%type <Node> words qwords word literal numeric dsym cpath command_call
%type <Node> compstmt bodystmt stmts stmt expr arg primary command 
%type <Node> expr_value primary_value opt_else cases if_tail exc_var
   // ENEBO: missing call_args2, open_args
%type <Node> call_args opt_ensure paren_args superclass
%type <Node> command_args var_ref opt_paren_args block_call block_command
%type <Node> f_opt undef_list string_dvar backref
%type <ArgsNode> f_args f_arglist f_larglist block_param block_param_def opt_block_param 
%type <Node> mrhs mlhs_item mlhs_node arg_value case_body exc_list aref_args
   // ENEBO: missing block_var == for_var, opt_block_var
%type <Node> lhs none args
%type <ListNode> qword_list word_list f_arg f_optarg f_marg_list
   // ENEBO: missing when_args
%type <ListNode> mlhs_head assocs assoc assoc_list mlhs_post f_block_optarg
%type <BlockPassNode> opt_block_arg block_arg none_block_pass
%type <BlockArgNode> opt_f_block_arg f_block_arg
%type <IterNode> brace_block do_block cmd_brace_block
   // ENEBO: missing mhls_entry
%type <MultipleAsgn19Node> mlhs mlhs_basic 
%type <RescueBodyNode> opt_rescue
%type <AssignableNode> var_lhs
   // ENEBO: begin all new types
%type <Node> f_arg_item
%type <Node> bv_decls opt_bv_decl lambda_body 
%type <LambdaNode> lambda
%type <Node> mlhs_inner f_block_opt for_var
%type <Node> opt_call_args f_marg f_margs 
%type <Token> bvar fsym
   // ENEBO: end all new types

%type <Token> rparen rbracket reswords f_bad_arg

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
              } compstmt {
  // ENEBO: Removed !compile_for_eval which probably is to reduce warnings
                  if ($2 != null) {
                      /* last expression should not be void */
                      if ($2 instanceof BlockNode) {
                          support.checkUselessStatement($<BlockNode>2.getLast());
                      } else {
                          support.checkUselessStatement($2);
                      }
                  }
                  support.getResult().setAST(support.addRootNode($2, getPosition($2)));
              }

bodystmt      : compstmt opt_rescue opt_else opt_ensure {
                  Node node = $1;

                  if ($2 != null) {
                      node = new RescueNode(getPosition($1), $1, $2, $3);
                  } else if ($3 != null) {
                      warnings.warn(ID.ELSE_WITHOUT_RESCUE, getPosition($1), "else without rescue is useless");
                      node = support.appendToBlock($1, $3);
                  }
                  if ($4 != null) {
                      if (node == null) node = NilImplicitNode.NIL;
                      node = new EnsureNode(getPosition($1), node, $4);
                  }

                  $$ = node;
                }

compstmt        : stmts opt_terms {
                    if ($1 instanceof BlockNode) {
                        support.checkUselessStatements($<BlockNode>1);
                    }
                    $$ = $1;
                }

stmts           : none
                | stmt {
                    $$ = support.newline_node($1, getPosition($1));
                }
                | stmts terms stmt {
                    $$ = support.appendToBlock($1, support.newline_node($3, getPosition($3)));
                }
                | error stmt {
                    $$ = $2;
                }

stmt            : kALIAS fitem {
                    lexer.setState(LexState.EXPR_FNAME);
                } fitem {
                    $$ = new AliasNode(getPosition($1), (String) $2.getValue(), (String) $4.getValue());
                }
                | kALIAS tGVAR tGVAR {
                    $$ = new VAliasNode(getPosition($1), (String) $2.getValue(), (String) $3.getValue());
                }
                | kALIAS tGVAR tBACK_REF {
                    $$ = new VAliasNode(getPosition($1), (String) $2.getValue(), "$" + $<BackRefNode>3.getType());
                }
                | kALIAS tGVAR tNTH_REF {
                    yyerror("can't make alias for the number variables");
                }
                | kUNDEF undef_list {
                    $$ = $2;
                }
                | stmt kIF_MOD expr_value {
                    $$ = new IfNode(getPosition($1), support.getConditionNode($3), $1, null);
                }
                | stmt kUNLESS_MOD expr_value {
                    $$ = new IfNode(getPosition($1), support.getConditionNode($3), null, $1);
                }
                | stmt kWHILE_MOD expr_value {
                    if ($1 != null && $1 instanceof BeginNode) {
                        $$ = new WhileNode(getPosition($1), support.getConditionNode($3), $<BeginNode>1.getBodyNode(), false);
                    } else {
                        $$ = new WhileNode(getPosition($1), support.getConditionNode($3), $1, true);
                    }
                }
                | stmt kUNTIL_MOD expr_value {
                    if ($1 != null && $1 instanceof BeginNode) {
                        $$ = new UntilNode(getPosition($1), support.getConditionNode($3), $<BeginNode>1.getBodyNode(), false);
                    } else {
                        $$ = new UntilNode(getPosition($1), support.getConditionNode($3), $1, true);
                    }
                }
                | stmt kRESCUE_MOD stmt {
                    Node body = $3 == null ? NilImplicitNode.NIL : $3;
                    $$ = new RescueNode(getPosition($1), $1, new RescueBodyNode(getPosition($1), null, body, null), null);
                }
                | klBEGIN {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("BEGIN in method");
                    }
                    support.pushLocalScope();
                } tLCURLY compstmt tRCURLY {
                    support.getResult().addBeginNode(new PreExeNode(getPosition($4), support.getCurrentScope(), $4));
                    support.popCurrentScope();
                    $$ = null;
                }
                | klEND tLCURLY compstmt tRCURLY {
                    if (support.isInDef() || support.isInSingle()) {
                        warnings.warn(ID.END_IN_METHOD, getPosition($1), "END in method; use at_exit");
                    }
                    $$ = new PostExeNode(getPosition($3), $3);
                }
                | lhs '=' command_call {
                    support.checkExpression($3);
                    $$ = support.node_assign($1, $3);
                }
                | mlhs '=' command_call {
                    support.checkExpression($3);
                    $1.setValueNode($3);
                    $$ = $1;
                }
                | var_lhs tOP_ASGN command_call {
                    support.checkExpression($3);

                    String asgnOp = (String) $2.getValue();
                    if (asgnOp.equals("||")) {
                        $1.setValueNode($3);
                        $$ = new OpAsgnOrNode(getPosition($1), support.gettable2($1), $1);
                    } else if (asgnOp.equals("&&")) {
                        $1.setValueNode($3);
                        $$ = new OpAsgnAndNode(getPosition($1), support.gettable2($1), $1);
                    } else {
                        $1.setValueNode(support.getOperatorCallNode(support.gettable2($1), asgnOp, $3));
                        $1.setPosition(getPosition($1));
                        $$ = $1;
                    }
                }
                | primary_value '[' opt_call_args rbracket tOP_ASGN command_call {
  // FIXME: arg_concat logic missing for opt_call_args
                    $$ = support.new_opElementAsgnNode(getPosition($1), $1, (String) $5.getValue(), $3, $6);
                }
                | primary_value tDOT tIDENTIFIER tOP_ASGN command_call {
                    $$ = new OpAsgnNode(getPosition($1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
                }
                | primary_value tDOT tCONSTANT tOP_ASGN command_call {
                    $$ = new OpAsgnNode(getPosition($1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
                }
                | primary_value tCOLON2 tIDENTIFIER tOP_ASGN command_call {
                    $$ = new OpAsgnNode(getPosition($1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
                }
                | backref tOP_ASGN command_call {
                    support.backrefAssignError($1);
                }
                | lhs '=' mrhs {
                    $$ = support.node_assign($1, $3);
                }
                | mlhs '=' arg_value {
                    $1.setValueNode($3);
                    $$ = $1;
                }
                | mlhs '=' mrhs {
                    $<AssignableNode>1.setValueNode($3);
                    $$ = $1;
                    $1.setPosition(getPosition($1));
                }
                | expr

// Node:expr *CURRENT* all but arg so far
expr            : command_call
                | expr kAND expr {
                    $$ = support.newAndNode(getPosition($2), $1, $3);
                }
                | expr kOR expr {
                    $$ = support.newOrNode(getPosition($2), $1, $3);
                }
                | kNOT expr {
                    $$ = new NotNode(getPosition($1), support.getConditionNode($2));
                }
                | tBANG command_call {
                    $$ = new NotNode(getPosition($1), support.getConditionNode($2));
                }
                | arg

expr_value      : expr {
                    support.checkExpression($1);
                }

// Node:command - call with or with block on end [!null]
command_call    : command
                | block_command
                | kRETURN call_args {
                    $$ = new ReturnNode(getPosition($1), support.ret_args($2, getPosition($1)));
                }
                | kBREAK call_args {
                    $$ = new BreakNode(getPosition($1), support.ret_args($2, getPosition($1)));
                }
                | kNEXT call_args {
                    $$ = new NextNode(getPosition($1), support.ret_args($2, getPosition($1)));
                }

// Node:block_command - A call with a block (foo.bar {...}, foo::bar {...}, bar {...}) [!null]
block_command   : block_call
                | block_call tDOT operation2 command_args {
                    $$ = support.new_call($1, $3, $4, null);
                }
                | block_call tCOLON2 operation2 command_args {
                    $$ = support.new_call($1, $3, $4, null);
                }

// :brace_block - [!null]
cmd_brace_block : tLBRACE_ARG {
                    support.pushBlockScope();
                } opt_block_param compstmt tRCURLY {
                    $$ = new IterNode(getPosition($1), $3, $4, support.getCurrentScope());
                    support.popCurrentScope();
                }

// Node:command - fcall/call/yield/super [!null]
command        : operation command_args %prec tLOWEST {
                    $$ = support.new_fcall($1, $2, null);
                }
                | operation command_args cmd_brace_block {
                    $$ = support.new_fcall($1, $2, $3);
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
                    $$ = support.new_super($2, $1); // .setPosFrom($2);
                }
                | kYIELD command_args {
                    $$ = support.new_yield(getPosition($1), $2);
                }

// MultipleAssig19Node:mlhs - [!null]
mlhs            : mlhs_basic
                | tLPAREN mlhs_inner rparen {
                    $$ = $2;
                }

// MultipleAssign19Node:mlhs_entry - mlhs w or w/o parens [!null]
mlhs_inner      : mlhs_basic {
                    $$ = $1;
                }
                | tLPAREN mlhs_inner rparen {
                    $$ = new MultipleAsgn19Node(getPosition($1), support.newArrayNode(getPosition($1), $2), null, null);
                }

// MultipleAssign19Node:mlhs_basic - multiple left hand side (basic because used in multiple context) [!null]
mlhs_basic      : mlhs_head {
                    $$ = new MultipleAsgn19Node(getPosition($1), $1, null, null);
                }
                | mlhs_head mlhs_item {
                    $$ = new MultipleAsgn19Node(getPosition($<Node>1), $1.add($2), null, null);
                }
                | mlhs_head tSTAR mlhs_node {
                    $$ = new MultipleAsgn19Node(getPosition($1), $1, $3, (ListNode) null);
                }
                | mlhs_head tSTAR mlhs_node ',' mlhs_post {
                    $$ = new MultipleAsgn19Node(getPosition($1), $1, $3, $5);
                }
                | mlhs_head tSTAR {
                    $$ = new MultipleAsgn19Node(getPosition($1), $1, new StarNode(getPosition()), null);
                }
                | mlhs_head tSTAR ',' mlhs_post {
                    $$ = new MultipleAsgn19Node(getPosition($1), $1, new StarNode(getPosition()), $4);
                }
                | tSTAR mlhs_node {
                    $$ = new MultipleAsgn19Node(getPosition($1), null, $2, null);
                }
                | tSTAR mlhs_node ',' mlhs_post {
                    $$ = new MultipleAsgn19Node(getPosition($1), null, $2, $4);
                }
                | tSTAR {
                      $$ = new MultipleAsgn19Node(getPosition($1), null, new StarNode(getPosition()), null);
                }
                | tSTAR ',' mlhs_post {
                      $$ = new MultipleAsgn19Node(getPosition($1), null, new StarNode(getPosition()), $3);
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

mlhs_node       : variable {
                    $$ = support.assignable($1, NilImplicitNode.NIL);
                }
                | primary_value '[' opt_call_args rbracket {
                    $$ = support.aryset($1, $3);
                }
                | primary_value tDOT tIDENTIFIER {
                    $$ = support.attrset($1, (String) $3.getValue());
                }
                | primary_value tCOLON2 tIDENTIFIER {
                    $$ = support.attrset($1, (String) $3.getValue());
                }
                | primary_value tDOT tCONSTANT {
                    $$ = support.attrset($1, (String) $3.getValue());
                }
                | primary_value tCOLON2 tCONSTANT {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = getPosition($1);

                    $$ = new ConstDeclNode(position, null, support.new_colon2(position, $1, (String) $3.getValue()), NilImplicitNode.NIL);
                }
                | tCOLON3 tCONSTANT {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = getPosition($1);

                    $$ = new ConstDeclNode(position, null, support.new_colon3(position, (String) $2.getValue()), NilImplicitNode.NIL);
                }
                | backref {
                    support.backrefAssignError($1);
                }

lhs             : variable {
                      // if (!($$ = assignable($1, 0))) $$ = NEW_BEGIN(0);
                    $$ = support.assignable($1, NilImplicitNode.NIL);
                }
                | primary_value '[' opt_call_args rbracket {
                    $$ = support.aryset($1, $3);
                }
                | primary_value tDOT tIDENTIFIER {
                    $$ = support.attrset($1, (String) $3.getValue());
                }
                | primary_value tCOLON2 tIDENTIFIER {
                    $$ = support.attrset($1, (String) $3.getValue());
                }
                | primary_value tDOT tCONSTANT {
                    $$ = support.attrset($1, (String) $3.getValue());
                }
                | primary_value tCOLON2 tCONSTANT {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = getPosition($1);

                    $$ = new ConstDeclNode(position, null, support.new_colon2(position, $1, (String) $3.getValue()), NilImplicitNode.NIL);
                }
                | tCOLON3 tCONSTANT {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = getPosition($1);

                    $$ = new ConstDeclNode(position, null, support.new_colon3(position, (String) $2.getValue()), NilImplicitNode.NIL);
                }
                | backref {
                    support.backrefAssignError($1);
                }

cname           : tIDENTIFIER {
                    yyerror("class/module name must be CONSTANT");
                }
                | tCONSTANT

cpath           : tCOLON3 cname {
                    $$ = support.new_colon3(getPosition($1), (String) $2.getValue());
                }
                | cname {
                    $$ = support.new_colon2($1.getPosition(), null, (String) $1.getValue());
                }
                | primary_value tCOLON2 cname {
                    $$ = support.new_colon2(getPosition($1), $1, (String) $3.getValue());
                }

// Token:fname - A function name [!null]
fname          : tIDENTIFIER | tCONSTANT | tFID 
               | op {
                   lexer.setState(LexState.EXPR_END);
                   $$ = $1;
               }
               | reswords {
                   lexer.setState(LexState.EXPR_END);
                   $$ = $1;
               }

// Token:fsym
fsym           : fname {
                    $$ = $1;
                }
                | symbol {
                    $$ = $1;
                }

// Token:fitem
fitem           : fsym {
                    $$ = $1;
                }
                | dsym {
                    $$ = $1;
                }

undef_list      : fitem {
                    $$ = new UndefNode(getPosition($1), (String) $1.getValue());
                }
                | undef_list ',' {
                    lexer.setState(LexState.EXPR_FNAME);
                } fitem {
                    $$ = support.appendToBlock($1, new UndefNode(getPosition($1), (String) $4.getValue()));
                }

// Token:op
op              : tPIPE | tCARET | tAMPER2 | tCMP | tEQ | tEQQ | tMATCH
                | tNMATCH | tGT | tGEQ | tLT | tLEQ | tNEQ | tLSHFT | tRSHFT
                | tPLUS | tMINUS | tSTAR2 | tSTAR | tDIVIDE | tPERCENT | tPOW
                | tBANG | tTILDE | tUPLUS | tUMINUS | tAREF | tASET | tBACK_REF2

// Token:op
reswords        : k__LINE__ | k__FILE__ | k__ENCODING__ | klBEGIN | klEND
                | kALIAS | kAND | kBEGIN | kBREAK | kCASE | kCLASS | kDEF
                | kDEFINED | kDO | kELSE | kELSIF | kEND | kENSURE | kFALSE
                | kFOR | kIN | kMODULE | kNEXT | kNIL | kNOT
                | kOR | kREDO | kRESCUE | kRETRY | kRETURN | kSELF | kSUPER
                | kTHEN | kTRUE | kUNDEF | kWHEN | kYIELD
                | kIF_MOD | kUNLESS_MOD | kWHILE_MOD | kUNTIL_MOD | kRESCUE_MOD

arg             : lhs '=' arg {
                    $$ = support.node_assign($1, $3);
                    // FIXME: Consider fixing node_assign itself rather than single case
                    $<Node>$.setPosition(getPosition($1));
                }
                | lhs '=' arg kRESCUE_MOD arg {
                    ISourcePosition position = getPosition($4);
                    Node body = $5 == null ? NilImplicitNode.NIL : $5;
                    $$ = support.node_assign($1, new RescueNode(position, $3, new RescueBodyNode(position, null, body, null), null));
                }
                | var_lhs tOP_ASGN arg {
                    support.checkExpression($3);
                    String asgnOp = (String) $2.getValue();

                    if (asgnOp.equals("||")) {
                        $1.setValueNode($3);
                        $$ = new OpAsgnOrNode(getPosition($1), support.gettable2($1), $1);
                    } else if (asgnOp.equals("&&")) {
                        $1.setValueNode($3);
                        $$ = new OpAsgnAndNode(getPosition($1), support.gettable2($1), $1);
                    } else {
                        $1.setValueNode(support.getOperatorCallNode(support.gettable2($1), asgnOp, $3));
                        $1.setPosition(getPosition($1));
                        $$ = $1;
                    }
                }
                | var_lhs tOP_ASGN arg kRESCUE_MOD arg {
                    support.checkExpression($3);
                    ISourcePosition position = getPosition($4);
                    Node body = $5 == null ? NilImplicitNode.NIL : $5;
                    Node rescueNode = new RescueNode(position, $3, new RescueBodyNode(position, null, body, null), null);

                    String asgnOp = (String) $2.getValue();
                    if (asgnOp.equals("||")) {
                        $1.setValueNode($3);
                        $$ = new OpAsgnOrNode(getPosition($1), support.gettable2($1), $1);
                    } else if (asgnOp.equals("&&")) {
                        $1.setValueNode($3);
                        $$ = new OpAsgnAndNode(getPosition($1), support.gettable2($1), $1);
                    } else {
                        $1.setValueNode(support.getOperatorCallNode(support.gettable2($1), asgnOp, $3));
                        $1.setPosition(getPosition($1));
                        $$ = $1;
                    }
                }
                | primary_value '[' opt_call_args rbracket tOP_ASGN arg {
  // FIXME: arg_concat missing for opt_call_args
                    $$ = support.new_opElementAsgnNode(getPosition($1), $1, (String) $5.getValue(), $3, $6);
                }
                | primary_value tDOT tIDENTIFIER tOP_ASGN arg {
                    $$ = new OpAsgnNode(getPosition($1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
                }
                | primary_value tDOT tCONSTANT tOP_ASGN arg {
                    $$ = new OpAsgnNode(getPosition($1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
                }
                | primary_value tCOLON2 tIDENTIFIER tOP_ASGN arg {
                    $$ = new OpAsgnNode(getPosition($1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
                }
                | primary_value tCOLON2 tCONSTANT tOP_ASGN arg {
                    yyerror("constant re-assignment");
                }
                | tCOLON3 tCONSTANT tOP_ASGN arg {
                    yyerror("constant re-assignment");
                }
                | backref tOP_ASGN arg {
                    support.backrefAssignError($1);
                }
                | arg tDOT2 arg {
                    support.checkExpression($1);
                    support.checkExpression($3);
    
                    boolean isLiteral = $1 instanceof FixnumNode && $3 instanceof FixnumNode;
                    $$ = new DotNode(getPosition($1), $1, $3, false, isLiteral);
                }
                | arg tDOT3 arg {
                    support.checkExpression($1);
                    support.checkExpression($3);

                    boolean isLiteral = $1 instanceof FixnumNode && $3 instanceof FixnumNode;
                    $$ = new DotNode(getPosition($1), $1, $3, true, isLiteral);
                }
                | arg tPLUS arg {
                    $$ = support.getOperatorCallNode($1, "+", $3, getPosition());
                }
                | arg tMINUS arg {
                    $$ = support.getOperatorCallNode($1, "-", $3, getPosition());
                }
                | arg tSTAR2 arg {
                    $$ = support.getOperatorCallNode($1, "*", $3, getPosition());
                }
                | arg tDIVIDE arg {
                    $$ = support.getOperatorCallNode($1, "/", $3, getPosition());
                }
                | arg tPERCENT arg {
                    $$ = support.getOperatorCallNode($1, "%", $3, getPosition());
                }
                | arg tPOW arg {
                    $$ = support.getOperatorCallNode($1, "**", $3, getPosition());
                }
                | tUMINUS_NUM tINTEGER tPOW arg {
                    $$ = support.getOperatorCallNode(support.getOperatorCallNode($2, "**", $4, getPosition()), "-@");
                }
                | tUMINUS_NUM tFLOAT tPOW arg {
                    $$ = support.getOperatorCallNode(support.getOperatorCallNode($2, "**", $4, getPosition()), "-@");
                }
                | tUPLUS arg {
                    if (support.isLiteral($2)) {
                        $$ = $2;
                    } else {
                        $$ = support.getOperatorCallNode($2, "+@");
                    }
                }
                | tUMINUS arg {
                    $$ = support.getOperatorCallNode($2, "-@");
                }
                | arg tPIPE arg {
                    $$ = support.getOperatorCallNode($1, "|", $3, getPosition());
                }
                | arg tCARET arg {
                    $$ = support.getOperatorCallNode($1, "^", $3, getPosition());
                }
                | arg tAMPER2 arg {
                    $$ = support.getOperatorCallNode($1, "&", $3, getPosition());
                }
                | arg tCMP arg {
                    $$ = support.getOperatorCallNode($1, "<=>", $3, getPosition());
                }
                | arg tGT arg {
                    $$ = support.getOperatorCallNode($1, ">", $3, getPosition());
                }
                | arg tGEQ arg {
                    $$ = support.getOperatorCallNode($1, ">=", $3, getPosition());
                }
                | arg tLT arg {
                    $$ = support.getOperatorCallNode($1, "<", $3, getPosition());
                }
                | arg tLEQ arg {
                    $$ = support.getOperatorCallNode($1, "<=", $3, getPosition());
                }
                | arg tEQ arg {
                    $$ = support.getOperatorCallNode($1, "==", $3, getPosition());
                }
                | arg tEQQ arg {
                    $$ = support.getOperatorCallNode($1, "===", $3, getPosition());
                }
                | arg tNEQ arg {
                    $$ = support.getOperatorCallNode($1, "!=", $3, getPosition());
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
                    $$ = new NotNode(getPosition($1), support.getMatchNode($1, $3));
                }
                | tBANG arg {
                    $$ = new NotNode(getPosition($1), support.getConditionNode($2));
                }
                | tTILDE arg {
                    $$ = support.getOperatorCallNode($2, "~");
                }
                | arg tLSHFT arg {
                    $$ = support.getOperatorCallNode($1, "<<", $3, getPosition());
                }
                | arg tRSHFT arg {
                    $$ = support.getOperatorCallNode($1, ">>", $3, getPosition());
                }
                | arg tANDOP arg {
                    $$ = support.newAndNode(getPosition($2), $1, $3);
                }
                | arg tOROP arg {
                    $$ = support.newOrNode(getPosition($2), $1, $3);
                }
                | kDEFINED opt_nl arg {
                    // ENEBO: arg surrounded by in_defined set/unset
                    $$ = new DefinedNode(getPosition($1), $3);
                }
                | arg '?' arg opt_nl ':' arg {
                    $$ = new IfNode(getPosition($1), support.getConditionNode($1), $3, $6);
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
                    $$ = support.arg_append($1, new Hash19Node(getPosition(), $3));
                }
                | assocs trailer {
                    $$ = support.newArrayNode(getPosition($1), new Hash19Node(getPosition(), $1));
                }

paren_args      : tLPAREN2 opt_call_args rparen {
                    $$ = $2;
                    if ($$ != null) $<Node>$.setPosition(getPosition($1));
                }

opt_paren_args  : none | paren_args

opt_call_args   : none | call_args

call_args       : command {
                    $$ = support.newArrayNode(getPosition($1), $1);
                }
                | args opt_block_arg {
                    $$ = support.arg_blk_pass($1, $2);
                }
                | assocs opt_block_arg {
                    $$ = support.newArrayNode(getPosition($1), new Hash19Node(getPosition(), $1));
                    $$ = support.arg_blk_pass((Node)$$, $2);
                }
                | args ',' assocs opt_block_arg {
                    $$ = support.arg_append($1, new Hash19Node(getPosition(), $3));
                    $$ = support.arg_blk_pass((Node)$$, $4);
                }
                | block_arg {
                }

command_args    : /* none */ {
                    $$ = new Long(lexer.getCmdArgumentState().begin());
                } call_args {
                    lexer.getCmdArgumentState().reset($<Long>1.longValue());
                    $$ = $2;
                }

block_arg       : tAMPER arg_value {
                    $$ = new BlockPassNode(getPosition($1), $2);
                }

opt_block_arg   : ',' block_arg {
                    $$ = $2;
                }
                | ',' {
                    $$ = null;
                }
                | none_block_pass

args            : arg_value {
                    $$ = support.newArrayNode(getPosition2($1), $1);
                }
                | tSTAR arg_value {
                    $$ = support.newSplatNode(getPosition($1), $2);
                }
                | args ',' arg_value {
                    Node node = support.splat_array($1);

                    if (node != null) {
                        $$ = support.list_append(node, $3);
                    } else {
                        $$ = support.arg_append($1, $3);
                    }
                }
                | args ',' tSTAR arg_value {
                    Node node = null;

                    // FIXME: lose syntactical elements here (and others like this)
                    if ($4 instanceof ArrayNode &&
                        (node = support.splat_array($1)) != null) {
                        $$ = support.list_concat(node, $4);
                    } else {
                        $$ = support.arg_concat(getPosition($1), $1, $4);
                    }
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
                        $$ = support.arg_concat(getPosition($1), $1, $4);
                    }
                }
                | tSTAR arg_value {
                     $$ = support.newSplatNode(getPosition($1), $2);  
                }

primary         : literal
                | strings
                | xstring
                | regexp
                | words
                | qwords
                | var_ref
                | backref
                | tFID {
                    $$ = new FCallNoArgNode($1.getPosition(), (String) $1.getValue());
                }
                | kBEGIN bodystmt kEND {
                    $$ = new BeginNode(getPosition($1), $2 == null ? NilImplicitNode.NIL : $2);
                }
                | tLPAREN_ARG expr {
                    lexer.setState(LexState.EXPR_ENDARG); 
                } rparen {
                    if (warnings.isVerbose()) warnings.warning(ID.GROUPED_EXPRESSION, getPosition($1), "(...) interpreted as grouped expression");
                    $$ = $2;
                }
                | tLPAREN compstmt tRPAREN {
                    if ($2 != null) {
                        // compstmt position includes both parens around it
                        ((ISourcePositionHolder) $2).setPosition(getPosition($1));
                    }
                    $$ = $2;
                }
                | primary_value tCOLON2 tCONSTANT {
                    $$ = support.new_colon2(getPosition($1), $1, (String) $3.getValue());
                }
                | tCOLON3 tCONSTANT {
                    $$ = support.new_colon3(getPosition($1), (String) $2.getValue());
                }
                | tLBRACK aref_args tRBRACK {
                    ISourcePosition position = getPosition($1);
                    if ($2 == null) {
                        $$ = new ZArrayNode(position); /* zero length array */
                    } else {
                        $$ = $2;
                        $<ISourcePositionHolder>$.setPosition(position);
                    }
                }
                | tLBRACE assoc_list tRCURLY {
                    $$ = new Hash19Node(getPosition($1), $2);
                }
                | kRETURN {
                    $$ = new ReturnNode($1.getPosition(), NilImplicitNode.NIL);
                }
                | kYIELD tLPAREN2 call_args rparen {
                    $$ = support.new_yield(getPosition($1), $3);
                }
                | kYIELD tLPAREN2 rparen {
                    $$ = new ZYieldNode(getPosition($1));
                }
                | kYIELD {
                    $$ = new ZYieldNode($1.getPosition());
                }
                | kDEFINED opt_nl tLPAREN2 expr rparen {
                    $$ = new DefinedNode(getPosition($1), $4);
                }
                | kNOT tLPAREN2 expr rparen {
                    $$ = support.getOperatorCallNode(support.getConditionNode($3), "!");
                }
                | kNOT tLPAREN2 rparen {
                    $$ = support.getOperatorCallNode(NilImplicitNode.NIL, "!");
                }
                | operation brace_block {
                    $$ = new FCallNoArgBlockNode(getPosition($1), (String) $1.getValue(), $2);
                }
                | method_call
                | method_call brace_block {
                    if ($1 != null && 
                          $<BlockAcceptingNode>1.getIterNode() instanceof BlockPassNode) {
                        throw new SyntaxException(PID.BLOCK_ARG_AND_BLOCK_GIVEN, getPosition($1), lexer.getCurrentLine(), "Both block arg and actual block given.");
                    }
                    $$ = $<BlockAcceptingNode>1.setIterNode($2);
                    $<Node>$.setPosition(getPosition($1));
                }
                | tLAMBDA lambda {
                    $$ = $2;
                }
                | kIF expr_value then compstmt if_tail kEND {
                    $$ = new IfNode(getPosition($1), support.getConditionNode($2), $4, $5);
                }
                | kUNLESS expr_value then compstmt opt_else kEND {
                    $$ = new IfNode(getPosition($1), support.getConditionNode($2), $5, $4);
                }
                | kWHILE {
                    lexer.getConditionState().begin();
                } expr_value do {
                    lexer.getConditionState().end();
                } compstmt kEND {
                    Node body = $6 == null ? NilImplicitNode.NIL : $6;
                    $$ = new WhileNode(getPosition($1), support.getConditionNode($3), body);
                }
                | kUNTIL {
                  lexer.getConditionState().begin();
                } expr_value do {
                  lexer.getConditionState().end();
                } compstmt kEND {
                    Node body = $6 == null ? NilImplicitNode.NIL : $6;
                    $$ = new UntilNode(getPosition($1), support.getConditionNode($3), body);
                }
                | kCASE expr_value opt_terms case_body kEND {
                    $$ = support.newCaseNode(getPosition($1), $2, $4);
                }
                | kCASE opt_terms case_body kEND {
                    $$ = support.newCaseNode(getPosition($1), null, $3);
                }
                | kFOR for_var kIN {
                    lexer.getConditionState().begin();
                } expr_value do {
                    lexer.getConditionState().end();
                } compstmt kEND {
                      // ENEBO: Lots of optz in 1.9 parser here
                    $$ = new ForNode(getPosition($1), $2, $8, $5);
                }
                | kCLASS cpath superclass {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("class definition in method body");
                    }
                    support.pushLocalScope();
                } bodystmt kEND {
                    Node body = $5 == null ? NilImplicitNode.NIL : $5;

                    $$ = new ClassNode(getPosition($1), $<Colon3Node>2, support.getCurrentScope(), body, $3);
                    support.popCurrentScope();
                }
                | kCLASS tLSHFT expr {
                    $$ = new Boolean(support.isInDef());
                    support.setInDef(false);
                } term {
                    $$ = new Integer(support.getInSingle());
                    support.setInSingle(0);
                    support.pushLocalScope();
                } bodystmt kEND {
                    $$ = new SClassNode(getPosition($1), $3, support.getCurrentScope(), $7);
                    support.popCurrentScope();
                    support.setInDef($<Boolean>4.booleanValue());
                    support.setInSingle($<Integer>6.intValue());
                }
                | kMODULE cpath {
                    if (support.isInDef() || support.isInSingle()) { 
                        yyerror("module definition in method body");
                    }
                    support.pushLocalScope();
                } bodystmt kEND {
                    Node body = $4 == null ? NilImplicitNode.NIL : $4;

                    $$ = new ModuleNode(getPosition($1), $<Colon3Node>2, support.getCurrentScope(), body);
                    support.popCurrentScope();
                }
                | kDEF fname {
                    support.setInDef(true);
                    support.pushLocalScope();
                } f_arglist bodystmt kEND {
                    // TODO: We should use implicit nil for body, but problem (punt til later)
                    Node body = $5; //$5 == null ? NilImplicitNode.NIL : $5;

                    $$ = new DefnNode(getPosition($1), new ArgumentNode($2.getPosition(), (String) $2.getValue()), $4, support.getCurrentScope(), body);
                    support.popCurrentScope();
                    support.setInDef(false);
                }
                | kDEF singleton dot_or_colon {
                    lexer.setState(LexState.EXPR_FNAME);
                } fname {
                    support.setInSingle(support.getInSingle() + 1);
                    support.pushLocalScope();
                    lexer.setState(LexState.EXPR_END); /* force for args */
                } f_arglist bodystmt kEND {
                    // TODO: We should use implicit nil for body, but problem (punt til later)
                    Node body = $8; //$8 == null ? NilImplicitNode.NIL : $8;

                    $$ = new DefsNode(getPosition($1), $2, new ArgumentNode($5.getPosition(), (String) $5.getValue()), $7, support.getCurrentScope(), body);
                    support.popCurrentScope();
                    support.setInSingle(support.getInSingle() - 1);
                }
                | kBREAK {
                    $$ = new BreakNode($1.getPosition(), NilImplicitNode.NIL);
                }
                | kNEXT {
                    $$ = new NextNode($1.getPosition(), NilImplicitNode.NIL);
                }
                | kREDO {
                    $$ = new RedoNode($1.getPosition());
                }
                | kRETRY {
                    $$ = new RetryNode($1.getPosition());
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
                    $$ = new IfNode(getPosition($1), support.getConditionNode($2), $4, $5);
                }

opt_else        : none
                | kELSE compstmt {
                    $$ = $2;
                }

for_var         : lhs
                | mlhs {
                }

f_marg          : f_norm_arg {
                     $$ = support.assignable($1, NilImplicitNode.NIL);
                }
                | tLPAREN f_margs rparen {
                    $$ = $2;
                }

f_marg_list     : f_marg {
                    $$ = support.newArrayNode($1.getPosition(), $1);
                }
                | f_marg_list ',' f_marg {
                    $$ = $1.add($3);
                }

f_margs         : f_marg_list {
                    $$ = new MultipleAsgn19Node(getPosition($1), $1, null, null);
                }
                | f_marg_list ',' tSTAR f_norm_arg {
                    $$ = new MultipleAsgn19Node(getPosition($1), $1, support.assignable($4, null), null);
                }
                | f_marg_list ',' tSTAR f_norm_arg ',' f_marg_list {
                    $$ = new MultipleAsgn19Node(getPosition($1), $1, support.assignable($4, null), $6);
                }
                | f_marg_list ',' tSTAR {
                    $$ = new MultipleAsgn19Node(getPosition($1), $1, new StarNode(getPosition()), null);
                }
                | f_marg_list ',' tSTAR ',' f_marg_list {
                    $$ = new MultipleAsgn19Node(getPosition($1), $1, new StarNode(getPosition()), $5);
                }
                | tSTAR f_norm_arg {
                    $$ = new MultipleAsgn19Node(getPosition($1), null, support.assignable($2, null), null);
                }
                | tSTAR f_norm_arg ',' f_marg_list {
                    $$ = new MultipleAsgn19Node(getPosition($1), null, support.assignable($2, null), $4);
                }
                | tSTAR {
                    $$ = new MultipleAsgn19Node(getPosition($1), null, new StarNode(getPosition()), null);
                }
                | tSTAR ',' f_marg_list {
                    $$ = new MultipleAsgn19Node(getPosition($1), null, null, $3);
                }

block_param     : f_arg ',' f_block_optarg ',' f_rest_arg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), $1, $3, $5, null, $6);
                }
                | f_arg ',' f_block_optarg ',' f_rest_arg ',' f_arg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), $1, $3, $5, $7, $8);
                }
                | f_arg ',' f_block_optarg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), $1, $3, null, null, $4);
                }
                | f_arg ',' f_block_optarg ',' f_arg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), $1, $3, null, $5, $6);
                }
                | f_arg ',' f_rest_arg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), $1, null, $3, null, $4);
                }
                | f_arg ',' {
                    RestArgNode rest = new UnnamedRestArgNode($1.getPosition(), support.getCurrentScope().addVariable("*"));
                    $$ = support.new_args($1.getPosition(), $1, null, rest, null, null);
                }
                | f_arg ',' f_rest_arg ',' f_arg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), $1, null, $3, $5, $6);
                }
                | f_arg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), $1, null, null, null, $2);
                }
                | f_block_optarg ',' f_rest_arg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), null, $1, $3, null, $4);
                }
                | f_block_optarg ',' f_rest_arg ',' f_arg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), null, $1, $3, $5, $6);
                }
                | f_block_optarg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), null, $1, null, null, $2);
                }
                | f_block_optarg ',' f_arg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), null, $1, null, $3, $4);
                }
                | f_rest_arg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), null, null, $1, null, $2);
                }
                | f_rest_arg ',' f_arg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), null, null, $1, $3, $4);
                }
                | f_block_arg {
                    $$ = support.new_args(getPosition($1), null, null, null, null, $1);
                }

opt_block_param : none {
    // was $$ = null;
                   $$ = support.new_args(getPosition(), null, null, null, null, null);
                }
                | block_param_def {
                    lexer.commandStart = true;
                    $$ = $1;
                }

block_param_def : tPIPE opt_bv_decl tPIPE {
                    $$ = support.new_args(getPosition(), null, null, null, null, null);
                }
                | tOROP {
                    $$ = support.new_args(getPosition(), null, null, null, null, null);
                }
                | tPIPE block_param opt_bv_decl tPIPE {
                    $$ = $2;
                }

// shadowed block variables....
opt_bv_decl     : none 
                | ';' bv_decls {
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
                    $$ = new LambdaNode(getPosition($2), $2, $3, support.getCurrentScope());
                    support.popCurrentScope();
                    lexer.setLeftParenBegin($<Integer>1);
                }

f_larglist      : tLPAREN2 f_args opt_bv_decl rparen {
                    $$ = $2;
                    $<ISourcePositionHolder>$.setPosition(getPosition($1));
                }
                | f_args opt_bv_decl {
                    $$ = $1;
                    $<ISourcePositionHolder>$.setPosition(getPosition($1));
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
                    $$ = new IterNode(getPosition($1), $3, $4, support.getCurrentScope());
                    support.popCurrentScope();
                }

block_call      : command do_block {
                    // Workaround for JRUBY-2326 (MRI does not enter this production for some reason)
                    if ($1 instanceof YieldNode) {
                        throw new SyntaxException(PID.BLOCK_GIVEN_TO_YIELD, getPosition($1), lexer.getCurrentLine(), "block given to yield");
                    }
                    if ($<BlockAcceptingNode>1.getIterNode() instanceof BlockPassNode) {
                        throw new SyntaxException(PID.BLOCK_ARG_AND_BLOCK_GIVEN, getPosition($1), lexer.getCurrentLine(), "Both block arg and actual block given.");
                    }
                    $$ = $<BlockAcceptingNode>1.setIterNode($2);
                    $<Node>$.setPosition(getPosition($1));
                }
                | block_call tDOT operation2 opt_paren_args {
                    $$ = support.new_call($1, $3, $4, null);
                }
                | block_call tCOLON2 operation2 opt_paren_args {
                    $$ = support.new_call($1, $3, $4, null);
                }

method_call     : operation paren_args {
                    $$ = support.new_fcall($1, $2, null);
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
                    $$ = support.new_call($1, new Token("call", $1.getPosition()), $3, null);
                }
                | primary_value tCOLON2 paren_args {
                    $$ = support.new_call($1, new Token("call", $1.getPosition()), $3, null);
                }
                | kSUPER paren_args {
                    $$ = support.new_super($2, $1);
                }
                | kSUPER {
                    $$ = new ZSuperNode($1.getPosition());
                }
                | primary_value '[' opt_call_args rbracket {
                    if ($1 instanceof SelfNode) {
                        $$ = support.new_fcall(new Token("[]", getPosition($1)), $3, null);
                    } else {
                        $$ = support.new_call($1, new Token("[]", getPosition($1)), $3, null);
                    }
                }

brace_block     : tLCURLY {
                    support.pushBlockScope();
                } opt_block_param compstmt tRCURLY {
                    $$ = new IterNode(getPosition($1), $3, $4, support.getCurrentScope());
                    support.popCurrentScope();
                }
                | kDO {
                    support.pushBlockScope();
                } opt_block_param compstmt kEND {
                    $$ = new IterNode(getPosition($1), $3, $4, support.getCurrentScope());
                    $<ISourcePositionHolder>0.setPosition(getPosition($<ISourcePositionHolder>0));
                    support.popCurrentScope();
                }

case_body       : kWHEN args then compstmt cases {
                    $$ = support.newWhenNode(getPosition($1), $2, $4, $5);
                }

cases           : opt_else | case_body

opt_rescue      : kRESCUE exc_list exc_var then compstmt opt_rescue {
                    Node node;
                    if ($3 != null) {
                        node = support.appendToBlock(support.node_assign($3, new GlobalVarNode(getPosition($1), "$!")), $5);
                        if ($5 != null) {
                            node.setPosition(support.unwrapNewlineNode($5).getPosition());
                        }
                    } else {
                        node = $5;
                    }
                    Node body = node == null ? NilImplicitNode.NIL : node;
                    $$ = new RescueBodyNode(getPosition($1), $2, body, $6);
                }
                | { 
                    $$ = null; 
                }

exc_list        : arg_value {
                    $$ = support.newArrayNode($1.getPosition(), $1);
                }
                | mrhs {
                    $$ = support.splat_array($1);
                    if ($$ == null) $$ = $1;
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

literal         : numeric
                | symbol {
                    // FIXME: We may be intern'ing more than once.
                    $$ = new SymbolNode($1.getPosition(), ((String) $1.getValue()).intern());
                }
                | dsym

strings         : string {
                    $$ = $1 instanceof EvStrNode ? new DStrNode(getPosition($1)).add($1) : $1;
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

string          : tCHAR {
                    $$ = new StrNode($<Token>0.getPosition(), ByteList.create((String) $1.getValue()));
                }
                | string1 {
                    $$ = $1;
                }
                | string string1 {
                    $$ = support.literal_concat(getPosition($1), $1, $2);
                }

string1         : tSTRING_BEG string_contents tSTRING_END {
                    $$ = $2;

                    $<ISourcePositionHolder>$.setPosition(getPosition($1));
                    int extraLength = ((String) $1.getValue()).length() - 1;

                    // We may need to subtract addition offset off of first 
                    // string fragment (we optimistically take one off in
                    // ParserSupport.literal_concat).  Check token length
                    // and subtract as neeeded.
                    if (($2 instanceof DStrNode) && extraLength > 0) {
                      Node strNode = ((DStrNode)$2).get(0);
                    }
                }

xstring         : tXSTRING_BEG xstring_contents tSTRING_END {
                    ISourcePosition position = getPosition($1);

                    if ($2 == null) {
                        $$ = new XStrNode(position, null);
                    } else if ($2 instanceof StrNode) {
                        $$ = new XStrNode(position, (ByteList) $<StrNode>2.getValue().clone());
                    } else if ($2 instanceof DStrNode) {
                        $$ = new DXStrNode(position, $<DStrNode>2);

                        $<Node>$.setPosition(position);
                    } else {
                        $$ = new DXStrNode(position).add($2);
                    }
                }

regexp          : tREGEXP_BEG xstring_contents tREGEXP_END {
                    int options = $3.getOptions();
                    Node node = $2;

                    if (node == null) {
                        $$ = new RegexpNode(getPosition($1), ByteList.create(""), options & ~ReOptions.RE_OPTION_ONCE);
                    } else if (node instanceof StrNode) {
                        $$ = new RegexpNode($2.getPosition(), (ByteList) ((StrNode) node).getValue().clone(), options & ~ReOptions.RE_OPTION_ONCE);
                    } else if (node instanceof DStrNode) {
                        $$ = new DRegexpNode(getPosition($1), (DStrNode) node, options, (options & ReOptions.RE_OPTION_ONCE) != 0);
                    } else {
                        $$ = new DRegexpNode(getPosition($1), options, (options & ReOptions.RE_OPTION_ONCE) != 0).add(node);
                    }
                }

words           : tWORDS_BEG ' ' tSTRING_END {
                    $$ = new ZArrayNode(getPosition($1));
                }
                | tWORDS_BEG word_list tSTRING_END {
                    $$ = $2;
                }

word_list       : /* none */ {
                    $$ = new ArrayNode(getPosition());
                }
                | word_list word ' ' {
                     $$ = $1.add($2 instanceof EvStrNode ? new DStrNode(getPosition($1)).add($2) : $2);
                }

word            : string_content
                | word string_content {
                     $$ = support.literal_concat(getPosition($1), $1, $2);
                }

qwords          : tQWORDS_BEG ' ' tSTRING_END {
                     $$ = new ZArrayNode(getPosition($1));
                }
                | tQWORDS_BEG qword_list tSTRING_END {
                    $$ = $2;
                    $<ISourcePositionHolder>$.setPosition(getPosition($1));
                }

qword_list      : /* none */ {
                    $$ = new ArrayNode(getPosition());
                }
                | qword_list tSTRING_CONTENT ' ' {
                    $$ = $1.add($2);
                }

string_contents : /* none */ {
                    $$ = new StrNode($<Token>0.getPosition(), ByteList.create(""));
                }
                | string_contents string_content {
                    $$ = support.literal_concat(getPosition($1), $1, $2);
                }

xstring_contents: /* none */ {
                    $$ = null;
                }
                | xstring_contents string_content {
                    $$ = support.literal_concat(getPosition($1), $1, $2);
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
                    $$ = new EvStrNode(getPosition($1), $3);
                }
                | tSTRING_DBEG {
                   $$ = lexer.getStrTerm();
                   lexer.setStrTerm(null);
                   lexer.setState(LexState.EXPR_BEG);
                } compstmt tRCURLY {
                   lexer.setStrTerm($<StrTerm>2);

                   $$ = support.newEvStrNode(getPosition($1), $3);
                }

string_dvar     : tGVAR {
                     $$ = new GlobalVarNode($1.getPosition(), (String) $1.getValue());
                }
                | tIVAR {
                     $$ = new InstVarNode($1.getPosition(), (String) $1.getValue());
                }
                | tCVAR {
                     $$ = new ClassVarNode($1.getPosition(), (String) $1.getValue());
                }
                | backref

// Token:symbol
symbol          : tSYMBEG sym {
                     lexer.setState(LexState.EXPR_END);
                     $$ = $2;
                     $<ISourcePositionHolder>$.setPosition(getPosition($1));
                }

// Token:symbol
sym             : fname | tIVAR | tGVAR | tCVAR

dsym            : tSYMBEG xstring_contents tSTRING_END {
                     lexer.setState(LexState.EXPR_END);

                     // DStrNode: :"some text #{some expression}"
                     // StrNode: :"some text"
                     // EvStrNode :"#{some expression}"
                     if ($2 == null) {
                       yyerror("empty symbol literal");
                     }
                     /* FIXME: No node here seems to be an empty string
                        instead of an error
                        if (!($$ = $2)) {
                        $$ = NEW_LIT(ID2SYM(rb_intern("")));
                        }
                     */

                     if ($2 instanceof DStrNode) {
                         $$ = new DSymbolNode(getPosition($1), $<DStrNode>2);
                     } else {
                         $$ = new DSymbolNode(getPosition($1));
                         $<DSymbolNode>$.add($2);
                     }
                }

numeric         : tINTEGER {
                    $$ = $1;
                }
                | tFLOAT {
                     $$ = $1;
                }
                | tUMINUS_NUM tINTEGER %prec tLOWEST {
                     $$ = support.negateInteger($2);
                }
                | tUMINUS_NUM tFLOAT %prec tLOWEST {
                     $$ = support.negateFloat($2);
                }

variable        : tIDENTIFIER | tIVAR | tGVAR | tCONSTANT | tCVAR
                | kNIL { 
                    $$ = new Token("nil", Tokens.kNIL, $1.getPosition());
                }
                | kSELF {
                    $$ = new Token("self", Tokens.kSELF, $1.getPosition());
                }
                | kTRUE { 
                    $$ = new Token("true", Tokens.kTRUE, $1.getPosition());
                }
                | kFALSE {
                    $$ = new Token("false", Tokens.kFALSE, $1.getPosition());
                }
                | k__FILE__ {
                    $$ = new Token("__FILE__", Tokens.k__FILE__, $1.getPosition());
                }
                | k__LINE__ {
                    $$ = new Token("__LINE__", Tokens.k__LINE__, $1.getPosition());
                }
                | k__ENCODING__ {
                    $$ = new Token("__ENCODING__", Tokens.k__ENCODING__, $1.getPosition());
                }

var_ref         : variable {
                    $$ = support.gettable($1);
                }

var_lhs         : variable {
                    $$ = support.assignable($1, NilImplicitNode.NIL);
                }

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
                   yyerrok();
                   $$ = null;
                }

                  // ENEBO: Look at command_start stuff I am ripping out
f_arglist       : tLPAREN2 f_args rparen {
                    $$ = $2;
                    $<ISourcePositionHolder>$.setPosition(getPosition($1));
                    lexer.setState(LexState.EXPR_BEG);
                }
                | f_args term {
                    $$ = $1;
                }

f_args          : f_arg ',' f_optarg ',' f_rest_arg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), $1, $3, $5, null, $6);
                }
                | f_arg ',' f_optarg ',' f_rest_arg ',' f_arg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), $1, $3, $5, $7, $8);
                }
                | f_arg ',' f_optarg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), $1, $3, null, null, $4);
                }
                | f_arg ',' f_optarg ',' f_arg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), $1, $3, null, $5, $6);
                }
                | f_arg ',' f_rest_arg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), $1, null, $3, null, $4);
                }
                | f_arg ',' f_rest_arg ',' f_arg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), $1, null, $3, $5, $6);
                }
                | f_arg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), $1, null, null, null, $2);
                }
                | f_optarg ',' f_rest_arg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), null, $1, $3, null, $4);
                }
                | f_optarg ',' f_rest_arg ',' f_arg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), null, $1, $3, $5, $6);
                }
                | f_optarg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), null, $1, null, null, $2);
                }
                | f_optarg ',' f_arg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), null, $1, null, $3, $4);
                }
                | f_rest_arg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), null, null, $1, null, $2);
                }
                | f_rest_arg ',' f_arg opt_f_block_arg {
                    $$ = support.new_args(getPosition($1), null, null, $1, $3, $4);
                }
                | f_block_arg {
                    $$ = support.new_args($1.getPosition(), null, null, null, null, $1);
                }
                | /* none */ {
                    $$ = support.new_args(getPosition(), null, null, null, null, null);
                }

f_bad_arg       : tCONSTANT {
                    yyerror("formal argument cannot be a constant");
                }
                | tIVAR {
                    yyerror("formal argument cannot be an instance variable");
                }
                | tGVAR {
                    yyerror("formal argument cannot be a global variable");
                }
                | tCVAR {
                    yyerror("formal argument cannot be a class variable");
                }

// Token:f_norm_arg
f_norm_arg      : f_bad_arg
                | tIDENTIFIER {
    // FIXME: Resolve what the hell is going on
    /*                    if (support.is_local_id($1)) {
                        yyerror("formal argument must be local variable");
                        }*/
                     
                    support.shadowing_lvar($1);
                    $$ = $1;
                }

f_arg_item      : f_norm_arg {
                    support.arg_var($1);
                    $$ = new ArgumentNode($<ISourcePositionHolder>1.getPosition(), (String) $1.getValue());
  /*
                    $$ = new ArgAuxiliaryNode($1.getPosition(), (String) $1.getValue(), 1);
  */
                }
                | tLPAREN f_margs rparen {
                    $$ = $2;
                    /*		    {
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

f_arg           : f_arg_item {
                    $$ = new ArrayNode(getPosition(), $1);
                }
                | f_arg ',' f_arg_item {
                    $1.add($3);
                    $$ = $1;
                }

f_opt           : tIDENTIFIER '=' arg_value {
                    if (!support.is_local_id($1)) {
                        yyerror("formal argument must be local variable");
                    }
                    support.shadowing_lvar($1);
                    support.arg_var($1);
                    $$ = new OptArgNode(getPosition($1), support.assignable($1, $3));
                }

f_block_opt     : tIDENTIFIER '=' primary_value {
                    if (!support.is_local_id($1)) {
                        yyerror("formal argument must be local variable");
                    }
                    support.shadowing_lvar($1);
                    support.arg_var($1);
                    $$ = new OptArgNode(getPosition($1), support.assignable($1, $3));
                }

f_block_optarg  : f_block_opt {
                    $$ = new BlockNode(getPosition($1)).add($1);
                }
                | f_block_optarg ',' f_block_opt {
                    $$ = support.appendToBlock($1, $3);
                }

f_optarg        : f_opt {
                    $$ = new BlockNode(getPosition($1)).add($1);
                }
                | f_optarg ',' f_opt {
                    $$ = support.appendToBlock($1, $3);
                }

restarg_mark    : tSTAR2 | tSTAR

f_rest_arg      : restarg_mark tIDENTIFIER {
                    if (!support.is_local_id($2)) {
                        yyerror("duplicate rest argument name");
                    }
                    support.shadowing_lvar($2);
                    $$ = new RestArgNode(getPosition($1), (String) $2.getValue(), support.arg_var($2));
                }
                | restarg_mark {
                    $$ = new UnnamedRestArgNode($1.getPosition(), support.getCurrentScope().addVariable("*"));
                }

blkarg_mark     : tAMPER2 | tAMPER

// f_block_arg - Block argument def for function (foo(&block)) [!null]
f_block_arg     : blkarg_mark tIDENTIFIER {
                    String identifier = (String) $2.getValue();

                    if (!support.is_local_id($2)) {
                        yyerror("block argument must be local variable");
                    }
                    support.shadowing_lvar($2);
                    $$ = new BlockArgNode(getPosition($1), support.arg_var($2), identifier);
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
                        yyerror("can't define single method for ().");
                    } else if ($3 instanceof ILiteralNode) {
                        yyerror("can't define single method for literals.");
                    }
                    support.checkExpression($3);
                    $$ = $3;
                }

assoc_list      : none {
                    $$ = new ArrayNode(getPosition());
                }
                | assocs trailer {
                    $$ = $1;
                }

assocs          : assoc
                | assocs ',' assoc {
                    $$ = $1.addAll($3);
                }

assoc           : arg_value tASSOC arg_value {
                    ISourcePosition position;
                    if ($1 == null && $3 == null) {
                        position = getPosition($2);
                    } else {
                        position = getPosition($1);
                    }

                    $$ = support.newArrayNode(position, $1).add($3);
                }
                | tLABEL arg_value {
                    $$ = support.newArrayNode(getPosition($1), new SymbolNode(getPosition($1), (String) $1.getValue())).add($2);
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

term            : ';' {
                      yyerrok();
                }
                | '\n'

terms           : term
                | terms ';' {
                      yyerrok();
                }

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
