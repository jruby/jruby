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
import org.jruby.util.RegexpOptions;

public class DefaultRubyParser implements RubyParser {
    protected ParserSupport support;
    protected RubyYaccLexer lexer;

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
        support.setWarnings(warnings);
        lexer.setWarnings(warnings);
    }
%}

%token <Token> kCLASS kMODULE kDEF kUNDEF kBEGIN kRESCUE kENSURE kEND kIF
  kUNLESS kTHEN kELSIF kELSE kCASE kWHEN kWHILE kUNTIL kFOR kBREAK kNEXT
  kREDO kRETRY kIN kDO kDO_COND kDO_BLOCK kRETURN kYIELD kSUPER kSELF kNIL
  kTRUE kFALSE kAND kOR kNOT kIF_MOD kUNLESS_MOD kWHILE_MOD kUNTIL_MOD
  kRESCUE_MOD kALIAS kDEFINED klBEGIN klEND k__LINE__ k__FILE__ 
  k__ENCODING__ kDO_LAMBDA 

%token <Token> tIDENTIFIER tFID tGVAR tIVAR tCONSTANT tCVAR tLABEL tCHAR
%type <Token> variable 
%type <Token>  sym symbol operation operation2 operation3 cname fname op
%type <Token>  dot_or_colon restarg_mark blkarg_mark
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
%token <RegexpNode> tREGEXP_END

%type <Node>  singleton strings string string1 xstring regexp
%type <Node>  string_contents xstring_contents string_content method_call
%type <Node>  words qwords word literal numeric dsym cpath command_call
%type <Node>  compstmt bodystmt stmts stmt expr arg primary command 
%type <Node>  expr_value primary_value opt_else cases if_tail exc_var 
%type <Node>  call_args call_args2 open_args opt_ensure paren_args superclass
%type <Node>  command_args var_ref opt_paren_args block_call block_command
%type <Node>  f_arglist f_args f_opt undef_list string_dvar backref 
%type <Node>  mrhs mlhs_item mlhs_node arg_value case_body exc_list aref_args 
%type <Node>  block_var opt_block_var lhs none
%type <ListNode> qword_list word_list f_arg f_optarg
%type <ListNode> args mlhs_head assocs assoc assoc_list
%type <Node> when_args
%type <BlockPassNode> opt_block_arg block_arg none_block_pass
%type <BlockArgNode> opt_f_block_arg f_block_arg 
%type <IterNode> brace_block do_block cmd_brace_block 
%type <MultipleAsgnNode> mlhs mlhs_basic mlhs_entry
%type <RescueBodyNode> opt_rescue
%type <AssignableNode> var_lhs
%type <LiteralNode> fsym
%type <Node> fitem
%type <Node> for_var 
%type <ListNode> block_par

%type <ArgumentNode> f_norm_arg
%type <RestArgNode> f_rest_arg 
   //%type <Token> rparen rbracket reswords f_bad_arg

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

%%
program       : {
                  lexer.setState(LexState.EXPR_BEG);
                  support.initTopLocalVariables();
              } compstmt {
                  if ($2 != null) {
                      /* last expression should not be void */
                      if ($2 instanceof BlockNode) {
                          support.checkUselessStatement($<BlockNode>2.getLast());
                      } else {
                          support.checkUselessStatement($2);
                      }
                  }
                  support.getResult().setAST(support.addRootNode($2, support.getPosition($2)));
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

	          $$ = node;
              }

compstmt      : stmts opt_terms {
                  if ($1 instanceof BlockNode) {
                      support.checkUselessStatements($<BlockNode>1);
		  }
                  $$ = $1;
              }

stmts         : none
              | stmt {
                  $$ = support.newline_node($1, support.getPosition($1));
              }
              | stmts terms stmt {
	          $$ = support.appendToBlock($1, support.newline_node($3, support.getPosition($3)));
              }
              | error stmt {
                  $$ = $2;
              }

stmt          : kALIAS fitem {
                  lexer.setState(LexState.EXPR_FNAME);
              } fitem {
                  $$ = support.newAlias($1.getPosition(), $2, $4);
              }
              | kALIAS tGVAR tGVAR {
                  $$ = new VAliasNode($1.getPosition(), (String) $2.getValue(), (String) $3.getValue());
              }
              | kALIAS tGVAR tBACK_REF {
                  $$ = new VAliasNode($1.getPosition(), (String) $2.getValue(), "$" + $<BackRefNode>3.getType()); // XXX
              }
              | kALIAS tGVAR tNTH_REF {
                  support.yyerror("can't make alias for the number variables");
              }
              | kUNDEF undef_list {
                  $$ = $2;
              }
              | stmt kIF_MOD expr_value {
                  $$ = new IfNode(support.getPosition($1), support.getConditionNode($3), $1, null);
              }
              | stmt kUNLESS_MOD expr_value {
                  $$ = new IfNode(support.getPosition($1), support.getConditionNode($3), null, $1);
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
              | klBEGIN {
                  if (support.isInDef() || support.isInSingle()) {
                      support.yyerror("BEGIN in method");
                  }
		  support.pushLocalScope();
              } tLCURLY compstmt tRCURLY {
                  support.getResult().addBeginNode(new PreExeNode($1.getPosition(), support.getCurrentScope(), $4));
                  support.popCurrentScope();
                  $$ = null; //XXX 0;
              }
              | klEND tLCURLY compstmt tRCURLY {
                  if (support.isInDef() || support.isInSingle()) {
                      support.warn(ID.END_IN_METHOD, $1.getPosition(), "END in method; use at_exit");
                  }
                  $$ = new PostExeNode($1.getPosition(), $3);
              }
              | lhs '=' command_call {
                  $$ = support.node_assign($1, $3);
              }
              | mlhs '=' command_call {
                  support.checkExpression($3);
		  if ($1.getHeadNode() != null) {
		      $1.setValueNode(new ToAryNode(support.getPosition($1), $3));
		  } else {
		      $1.setValueNode(support.newArrayNode(support.getPosition($1), $3));
		  }
		  $$ = $1;
              }
              | var_lhs tOP_ASGN command_call {
 	          support.checkExpression($3);

		  String asgnOp = (String) $2.getValue();
		  if (asgnOp.equals("||")) {
	              $1.setValueNode($3);
	              $$ = new OpAsgnOrNode(support.getPosition($1), support.gettable2($1), $1);
		  } else if (asgnOp.equals("&&")) {
	              $1.setValueNode($3);
                      $$ = new OpAsgnAndNode(support.getPosition($1), support.gettable2($1), $1);
		  } else {
                      $1.setValueNode(support.getOperatorCallNode(support.gettable2($1), asgnOp, $3));
                      $1.setPosition(support.getPosition($1));
		      $$ = $1;
		  }
	      }
              | primary_value '[' aref_args tRBRACK tOP_ASGN command_call {
                  support.checkExpression($6);

                  $$ = support.new_opElementAsgnNode(support.getPosition($1), $1, (String) $5.getValue(), $3, $6);

              }
              | primary_value tDOT tIDENTIFIER tOP_ASGN command_call {
                  support.checkExpression($5);

                  $$ = new OpAsgnNode(support.getPosition($1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
              }
              | primary_value tDOT tCONSTANT tOP_ASGN command_call {
                  support.checkExpression($5);

                  $$ = new OpAsgnNode(support.getPosition($1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
              }
              | primary_value tCOLON2 tIDENTIFIER tOP_ASGN command_call {
                  support.checkExpression($5);

                  $$ = new OpAsgnNode(support.getPosition($1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
              }
              | backref tOP_ASGN command_call {
                  support.backrefAssignError($1);
              }
              | lhs '=' mrhs {
                  $$ = support.node_assign($1, support.newSValueNode(support.getPosition($1), $3));
              }
 	      | mlhs '=' arg_value {
                  if ($1.getHeadNode() != null) {
		      $1.setValueNode(new ToAryNode(support.getPosition($1), $3));
		  } else {
		      $1.setValueNode(support.newArrayNode(support.getPosition($1), $3));
		  }
		  $$ = $1;
	      }
	      | mlhs '=' mrhs {
                  $<AssignableNode>1.setValueNode($3);
		  $$ = $1;
                  $1.setPosition(support.getPosition($1));
	      }
              | expr 

// Node:expr *CURRENT* all but arg so far
expr          : command_call 
              | expr kAND expr {
                  $$ = support.newAndNode($2.getPosition(), $1, $3);
              }
              | expr kOR expr {
                  $$ = support.newOrNode($2.getPosition(), $1, $3);
              }
              | kNOT expr {
                  $$ = new NotNode($1.getPosition(), support.getConditionNode($2));
              }
              | tBANG command_call {
                  $$ = new NotNode($1.getPosition(), support.getConditionNode($2));
              }
              | arg

expr_value    : expr {
                  support.checkExpression($1);
	      }

// Node:command - call with or with block on end [!null]
command_call  : command
              | block_command
              | kRETURN call_args {
                  $$ = new ReturnNode($1.getPosition(), support.ret_args($2, $1.getPosition()));
              }
              | kBREAK call_args {
                  $$ = new BreakNode($1.getPosition(), support.ret_args($2, $1.getPosition()));
              }
              | kNEXT call_args {
                  $$ = new NextNode($1.getPosition(), support.ret_args($2, $1.getPosition()));
              }

// Node:block_command - A call with a block (foo.bar {...}, foo::bar {...}, bar {...}) [!null]
block_command : block_call
              | block_call tDOT operation2 command_args {
                  $$ = support.new_call($1, $3, $4, null);
              }
              | block_call tCOLON2 operation2 command_args {
                  $$ = support.new_call($1, $3, $4, null);
              }

// :brace_block - [!null]
cmd_brace_block	: tLBRACE_ARG {
                    support.pushBlockScope();
		} opt_block_var compstmt tRCURLY {
                    $$ = support.new_iter(support.getPosition($1), $3, support.getCurrentScope(), $4);
                    support.popCurrentScope();
		}

// Node:command - fcall/call/yield/super [!null]
command       : operation command_args  %prec tLOWEST {
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
                  $$ = support.new_yield($1.getPosition(), $2);
	      }

// MultipleAssigNode:mlhs - [!null]
mlhs          : mlhs_basic
              | tLPAREN mlhs_entry tRPAREN {
                  $$ = $2;
	      }

// MultipleAssignNode:mlhs_entry - mlhs w or w/o parens [!null]
mlhs_entry    : mlhs_basic
              | tLPAREN mlhs_entry tRPAREN {
                  $$ = new MultipleAsgnNode($1.getPosition(), support.newArrayNode($1.getPosition(), $2), null);
              }

// MultipleAssignNode:mlhs_basic - multiple left hand side (basic because used in multiple context) [!null]
mlhs_basic    : mlhs_head {
                  $$ = new MultipleAsgnNode(support.getPosition($1), $1, null);
              }
              | mlhs_head mlhs_item {
//mirko: check
                  $$ = new MultipleAsgnNode(support.getPosition($<Node>1), $1.add($2), null);
              }
              | mlhs_head tSTAR mlhs_node {
                  $$ = new MultipleAsgnNode(support.getPosition($1), $1, $3);
              }
              | mlhs_head tSTAR {
                  $$ = new MultipleAsgnNode(support.getPosition($1), $1, new StarNode(lexer.getPosition()));
              }
              | tSTAR mlhs_node {
                  $$ = new MultipleAsgnNode($1.getPosition(), null, $2);
              }
              | tSTAR {
                  $$ = new MultipleAsgnNode($1.getPosition(), null, new StarNode(lexer.getPosition()));
              }

mlhs_item     : mlhs_node 
              | tLPAREN mlhs_entry tRPAREN {
                  $$ = $2;
              }

// [!null]
mlhs_head     : mlhs_item ',' {
                  $$ = support.newArrayNode($1.getPosition(), $1);
              }
              | mlhs_head mlhs_item ',' {
                  $$ = $1.add($2);
              }

mlhs_node     : variable {
                  $$ = support.assignable($1, NilImplicitNode.NIL);
              }
              | primary_value '[' aref_args tRBRACK {
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
		      support.yyerror("dynamic constant assignment");
		  }

		  ISourcePosition position = support.getPosition($1);

                  $$ = new ConstDeclNode(position, null, support.new_colon2(position, $1, (String) $3.getValue()), NilImplicitNode.NIL);
	      }
 	      | tCOLON3 tCONSTANT {
                  if (support.isInDef() || support.isInSingle()) {
		      support.yyerror("dynamic constant assignment");
		  }

                  ISourcePosition position = support.getPosition($1);

                  $$ = new ConstDeclNode(position, null, support.new_colon3(position, (String) $2.getValue()), NilImplicitNode.NIL);
	      }
              | backref {
	          support.backrefAssignError($1);
              }

// Node:lhs - left hand side of an assignment [!null]
lhs           : variable {
                  $$ = support.assignable($1, NilImplicitNode.NIL);
              }
              | primary_value '[' aref_args tRBRACK {
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
		      support.yyerror("dynamic constant assignment");
		  }
			
		  ISourcePosition position = support.getPosition($1);

                  $$ = new ConstDeclNode(position, null, support.new_colon2(position, $1, (String) $3.getValue()), NilImplicitNode.NIL);
              }
	      | tCOLON3 tCONSTANT {
                  if (support.isInDef() || support.isInSingle()) {
		      support.yyerror("dynamic constant assignment");
		  }

                  ISourcePosition position = support.getPosition($1);

                  $$ = new ConstDeclNode(position, null, support.new_colon3(position, (String) $2.getValue()), NilImplicitNode.NIL);
	      }
              | backref {
                   support.backrefAssignError($1);
	      }

cname         : tIDENTIFIER {
                  support.yyerror("class/module name must be CONSTANT");
              }
              | tCONSTANT

cpath	      : tCOLON3 cname {
                  $$ = support.new_colon3($1.getPosition(), (String) $2.getValue());
	      }
	      | cname {
                  $$ = support.new_colon2($1.getPosition(), null, (String) $1.getValue());
 	      }
	      | primary_value tCOLON2 cname {
                  $$ = support.new_colon2($1.getPosition(), $1, (String) $3.getValue());
	      }

// Token:fname - A function name [!null]
fname         : tIDENTIFIER | tCONSTANT | tFID
              | op {
                  lexer.setState(LexState.EXPR_END);
                  $$ = $1;
              }
// FIXME: reswords is really Keyword which is not a Token...This should bomb
              | reswords {
                  lexer.setState(LexState.EXPR_END);
                  $$ = $<>1;
              }

// LiteralNode:fsym
fsym          : fname {
                  $$ = new LiteralNode($1);
              }
              | symbol {
                  $$ = new LiteralNode($1);
              }

// Node:fitem
fitem         : fsym {
                  $$ = $1;
              }
              | dsym {
                  $$ = $1;
              }

undef_list    : fitem {
                  $$ = support.newUndef($1.getPosition(), $1);
              }
              | undef_list ',' {
                  lexer.setState(LexState.EXPR_FNAME);
	      } fitem {
                  $$ = support.appendToBlock($1, support.newUndef($1.getPosition(), $4));
              }

// Token:op - inline operations [!null]
op            : tPIPE | tCARET | tAMPER2 | tCMP | tEQ | tEQQ | tMATCH | tGT
              | tGEQ | tLT | tLEQ | tLSHFT | tRSHFT | tPLUS  | tMINUS | tSTAR2
              | tSTAR | tDIVIDE | tPERCENT | tPOW | tTILDE | tUPLUS | tUMINUS
              | tAREF | tASET | tBACK_REF2

// Keyword:reswords - reserved words [!null]
reswords	: k__LINE__ | k__FILE__  | klBEGIN | klEND
		| kALIAS | kAND | kBEGIN | kBREAK | kCASE | kCLASS | kDEF
		| kDEFINED | kDO | kELSE | kELSIF | kEND | kENSURE | kFALSE
		| kFOR | kIN | kMODULE | kNEXT | kNIL | kNOT
		| kOR | kREDO | kRESCUE | kRETRY | kRETURN | kSELF | kSUPER
		| kTHEN | kTRUE | kUNDEF | kWHEN | kYIELD
		| kIF_MOD | kUNLESS_MOD | kWHILE_MOD | kUNTIL_MOD | kRESCUE_MOD

arg           : lhs '=' arg {
                  $$ = support.node_assign($1, $3);
		  // FIXME: Consider fixing node_assign itself rather than single case
		  $<Node>$.setPosition(support.getPosition($1));
              }
	      | lhs '=' arg kRESCUE_MOD arg {
                  ISourcePosition position = $4.getPosition();
                  Node body = $5 == null ? NilImplicitNode.NIL : $5;
                  $$ = support.node_assign($1, new RescueNode(position, $3, new RescueBodyNode(position, null, body, null), null));
	      }
	      | var_lhs tOP_ASGN arg {
		  support.checkExpression($3);
		  String asgnOp = (String) $2.getValue();

		  if (asgnOp.equals("||")) {
	              $1.setValueNode($3);
	              $$ = new OpAsgnOrNode(support.getPosition($1), support.gettable2($1), $1);
		  } else if (asgnOp.equals("&&")) {
	              $1.setValueNode($3);
                      $$ = new OpAsgnAndNode(support.getPosition($1), support.gettable2($1), $1);
		  } else {
		      $1.setValueNode(support.getOperatorCallNode(support.gettable2($1), asgnOp, $3));
                      $1.setPosition(support.getPosition($1));
		      $$ = $1;
		  }
              }
              | primary_value '[' aref_args tRBRACK tOP_ASGN arg {
		  support.checkExpression($6);

                  $$ = support.new_opElementAsgnNode(support.getPosition($1), $1, (String) $5.getValue(), $3, $6);
              }
              | primary_value tDOT tIDENTIFIER tOP_ASGN arg {
		  support.checkExpression($5);

                  $$ = new OpAsgnNode(support.getPosition($1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
              }
              | primary_value tDOT tCONSTANT tOP_ASGN arg {
		  support.checkExpression($5);

                  $$ = new OpAsgnNode(support.getPosition($1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
              }
              | primary_value tCOLON2 tIDENTIFIER tOP_ASGN arg {
		  support.checkExpression($5);

                  $$ = new OpAsgnNode(support.getPosition($1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
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
	      | tUMINUS_NUM tINTEGER tPOW arg {
                  $$ = support.getOperatorCallNode(support.getOperatorCallNode($2, "**", $4, lexer.getPosition()), "-@");
              }
	      | tUMINUS_NUM tFLOAT tPOW arg {
                  $$ = support.getOperatorCallNode(support.getOperatorCallNode($2, "**", $4, lexer.getPosition()), "-@");
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
                  $$ = new NotNode(support.getPosition($1), support.getOperatorCallNode($1, "==", $3, lexer.getPosition()));
              }
              | arg tMATCH arg {
                  $$ = support.getMatchNode($1, $3);
              }
              | arg tNMATCH arg {
                  $$ = new NotNode(support.getPosition($1), support.getMatchNode($1, $3));
              }
              | tBANG arg {
                  $$ = new NotNode(support.getPosition($1), support.getConditionNode($2));
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
                  $$ = support.newAndNode($2.getPosition(), $1, $3);
              }
              | arg tOROP arg {
                  $$ = support.newOrNode($2.getPosition(), $1, $3);
              }
              | kDEFINED opt_nl arg {
                  $$ = new DefinedNode($1.getPosition(), $3);
              }
              | arg '?' arg ':' arg {
                  $$ = new IfNode(support.getPosition($1), support.getConditionNode($1), $3, $5);
              }
              | primary {
                  $$ = $1;
              }

arg_value     : arg {
	          support.checkExpression($1);
	          $$ = $1;   
	      }

aref_args     : none
              | command opt_nl {
                  $$ = support.newArrayNode(support.getPosition($1), $1);
              }
              | args trailer {
                  $$ = $1;
              }
              | args ',' tSTAR arg_value opt_nl {
                  $$ = support.arg_concat(support.getPosition($1), $1, $4);
              }
              | assocs trailer {
                  ISourcePosition pos = $1.getPosition();
                  $$ = support.newArrayNode(pos, new HashNode(pos, $1));
              }
              | tSTAR arg_value opt_nl {
		  $$ = new NewlineNode($1.getPosition(), support.newSplatNode($1.getPosition(), $2));
              }

paren_args    : tLPAREN2 none tRPAREN {
                  $$ = new ArrayNode($1.getPosition());
              }
              | tLPAREN2 call_args opt_nl tRPAREN {
                  $$ = $2;
		  $<Node>$.setPosition($1.getPosition());
              }
              | tLPAREN2 block_call opt_nl tRPAREN {
                  $$ = support.newArrayNode($1.getPosition(), $2);
              }
              | tLPAREN2 args ',' block_call opt_nl tRPAREN {
                  $$ = $2.add($4);
              }

opt_paren_args: none | paren_args 

// Node:call_args - Arguments for a function call
call_args     : command {
                  $$ = support.newArrayNode(support.getPosition($1), $1);
              }
              | args opt_block_arg {
                  $$ = support.arg_blk_pass($1, $2);
              }
              | args ',' tSTAR arg_value opt_block_arg {
                  $$ = support.arg_concat(support.getPosition($1), $1, $4);
                  $$ = support.arg_blk_pass($<Node>$, $5);
              }
              | assocs opt_block_arg {
                  ISourcePosition pos = $1.getPosition();
                  $$ = support.newArrayNode(pos, new HashNode(pos, $1));
                  $$ = support.arg_blk_pass((Node)$$, $2);
              }
              | assocs ',' tSTAR arg_value opt_block_arg {
                  ISourcePosition pos = $1.getPosition();
                  $$ = support.arg_concat(pos, support.newArrayNode(pos, new HashNode(pos, $1)), $4);
                  $$ = support.arg_blk_pass((Node)$$, $5);
              }
              | args ',' assocs opt_block_arg {
                  $$ = $1.add(new HashNode(lexer.getPosition(), $3));
                  $$ = support.arg_blk_pass((Node)$$, $4);
              }
              | args ',' assocs ',' tSTAR arg opt_block_arg {
                  support.checkExpression($6);
		  $$ = support.arg_concat(support.getPosition($1), $1.add(new HashNode(lexer.getPosition(), $3)), $6);
                  $$ = support.arg_blk_pass((Node)$$, $7);
              }
              | tSTAR arg_value opt_block_arg {
                  $$ = support.arg_blk_pass(support.newSplatNode($1.getPosition(), $2), $3);
              }
              | block_arg {
              }

call_args2    : arg_value ',' args opt_block_arg {
                  $$ = support.arg_blk_pass(support.newArrayNode(support.getPosition($1), $1).addAll($3), $4);
	      }
	      | arg_value ',' block_arg {
                  $$ = support.arg_blk_pass(support.newArrayNode(support.getPosition($1), $1), $3);
              }
              | arg_value ',' tSTAR arg_value opt_block_arg {
                  $$ = support.arg_concat(support.getPosition($1), support.newArrayNode(support.getPosition($1), $1), $4);
                  $$ = support.arg_blk_pass((Node)$$, $5);
	      }
	      | arg_value ',' args ',' tSTAR arg_value opt_block_arg {
                  $$ = support.arg_concat(support.getPosition($1), support.newArrayNode(support.getPosition($1), $1).addAll(new HashNode(lexer.getPosition(), $3)), $6);
                  $$ = support.arg_blk_pass((Node)$$, $7);
	      }
	      | assocs opt_block_arg {
                  ISourcePosition pos = $1.getPosition();
                  $$ = support.newArrayNode(pos, new HashNode(pos, $1));
                  $$ = support.arg_blk_pass((Node)$$, $2);
	      }
	      | assocs ',' tSTAR arg_value opt_block_arg {
                  ISourcePosition pos = $1.getPosition();
                  $$ = support.arg_concat(pos, support.newArrayNode(pos, new HashNode(pos, $1)), $4);
                  $$ = support.arg_blk_pass((Node)$$, $5);
	      }
	      | arg_value ',' assocs opt_block_arg {
                  $$ = support.newArrayNode(support.getPosition($1), $1).add(new HashNode(lexer.getPosition(), $3));
                  $$ = support.arg_blk_pass((Node)$$, $4);
	      }
	      | arg_value ',' args ',' assocs opt_block_arg {
                  $$ = support.newArrayNode(support.getPosition($1), $1).addAll($3).add(new HashNode(lexer.getPosition(), $5));
                  $$ = support.arg_blk_pass((Node)$$, $6);
	      }
	      | arg_value ',' assocs ',' tSTAR arg_value opt_block_arg {
                  $$ = support.arg_concat(support.getPosition($1), support.newArrayNode(support.getPosition($1), $1).add(new HashNode(lexer.getPosition(), $3)), $6);
                  $$ = support.arg_blk_pass((Node)$$, $7);
	      }
	      | arg_value ',' args ',' assocs ',' tSTAR arg_value opt_block_arg {
                  $$ = support.arg_concat(support.getPosition($1), support.newArrayNode(support.getPosition($1), $1).addAll($3).add(new HashNode(lexer.getPosition(), $5)), $8);
                  $$ = support.arg_blk_pass((Node)$$, $9);
	      }
	      | tSTAR arg_value opt_block_arg {
                  $$ = support.arg_blk_pass(support.newSplatNode($1.getPosition(), $2), $3);
	      }
	      | block_arg {
              }

command_args  : /* none */ { 
	          $$ = Long.valueOf(lexer.getCmdArgumentState().begin());
	      } open_args {
                  lexer.getCmdArgumentState().reset($<Long>1.longValue());
                  $$ = $2;
              }

 open_args    : call_args
	      | tLPAREN_ARG  {                    
		  lexer.setState(LexState.EXPR_ENDARG);
	      } tRPAREN {
                  support.warn(ID.ARGUMENT_EXTRA_SPACE, $1.getPosition(), "don't put space before argument parentheses");
	          $$ = null;
	      }
	      | tLPAREN_ARG call_args2 {
		  lexer.setState(LexState.EXPR_ENDARG);
	      } tRPAREN {
                  support.warn(ID.ARGUMENT_EXTRA_SPACE, $1.getPosition(), "don't put space before argument parentheses");
		  $$ = $2;
	      }

block_arg     : tAMPER arg_value {
                  support.checkExpression($2);
                  $$ = new BlockPassNode($1.getPosition(), $2);
              }

opt_block_arg : ',' block_arg {
                  $$ = $2;
              }
              | none_block_pass

args          : arg_value {
                  ISourcePosition pos = $1 == null ? lexer.getPosition() : $1.getPosition();
                  $$ = support.newArrayNode(pos, $1);
              }
              | args ',' arg_value {
                  $$ = $1.add($3);
              }

mrhs          : args ',' arg_value {
		  $$ = $1.add($3);
              }
 	      | args ',' tSTAR arg_value {
                  $$ = support.arg_concat(support.getPosition($1), $1, $4);
	      }
              | tSTAR arg_value {  
                  $$ = support.newSplatNode($1.getPosition(), $2);
	      }

primary       : literal
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
                  $$ = new BeginNode($1.getPosition(), $2 == null ? NilImplicitNode.NIL : $2);
	      }
              | tLPAREN_ARG expr { 
                  lexer.setState(LexState.EXPR_ENDARG); 
              } opt_nl tRPAREN {
		  support.warning(ID.GROUPED_EXPRESSION, $1.getPosition(), "(...) interpreted as grouped expression");
                  $$ = $2;
	      }
              | tLPAREN compstmt tRPAREN {
                  if ($2 != null) {
                      // compstmt position includes both parens around it
                      ((ISourcePositionHolder) $2).setPosition($1.getPosition());
                      $$ = $2;
                  } else {
                      $$ = new NilNode($1.getPosition());
                  }
              }
              | primary_value tCOLON2 tCONSTANT {
                  $$ = support.new_colon2(support.getPosition($1), $1, (String) $3.getValue());
              }
              | tCOLON3 tCONSTANT {
                  $$ = support.new_colon3($1.getPosition(), (String) $2.getValue());
              }
              | primary_value '[' aref_args tRBRACK {
                  if ($1 instanceof SelfNode) {
                      $$ = support.new_fcall(new Token("[]", support.getPosition($1)), $3, null);
                  } else {
                      $$ = support.new_aref($1, new Token("[]", support.getPosition($1)), $3);
                  }
              }
              | tLBRACK aref_args tRBRACK {
                  ISourcePosition position = $1.getPosition();
                  if ($2 == null) {
                      $$ = new ZArrayNode(position); /* zero length array */
                  } else {
                      $$ = $2;
                      $<ISourcePositionHolder>$.setPosition(position);
                  }
              }
              | tLBRACE assoc_list tRCURLY {
                  $$ = new HashNode($1.getPosition(), $2);
              }
              | kRETURN {
		  $$ = new ReturnNode($1.getPosition(), NilImplicitNode.NIL);
              }
              | kYIELD tLPAREN2 call_args tRPAREN {
                  $$ = support.new_yield($1.getPosition(), $3);
              }
              | kYIELD tLPAREN2 tRPAREN {
                  $$ = new ZYieldNode($1.getPosition());
              }
              | kYIELD {
                  $$ = new ZYieldNode($1.getPosition());
              }
              | kDEFINED opt_nl tLPAREN2 expr tRPAREN {
                  $$ = new DefinedNode($1.getPosition(), $4);
              }
              | operation brace_block {
                  $$ = new FCallNoArgBlockNode(support.getPosition($1), (String) $1.getValue(), $2);
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
              | kIF expr_value then compstmt if_tail kEND {
                  $$ = new IfNode($1.getPosition(), support.getConditionNode($2), $4, $5);
              }
              | kUNLESS expr_value then compstmt opt_else kEND {
                  $$ = new IfNode($1.getPosition(), support.getConditionNode($2), $5, $4);
              }
              | kWHILE { 
                  lexer.getConditionState().begin();
	      } expr_value do {
		  lexer.getConditionState().end();
	      } compstmt kEND {
                  Node body = $6 == null ? NilImplicitNode.NIL : $6;
                  $$ = new WhileNode($1.getPosition(), support.getConditionNode($3), body);
              }
              | kUNTIL {
                  lexer.getConditionState().begin();
              } expr_value do {
                  lexer.getConditionState().end();
              } compstmt kEND {
                  Node body = $6 == null ? NilImplicitNode.NIL : $6;
                  $$ = new UntilNode($1.getPosition(), support.getConditionNode($3), body);
              }
              | kCASE expr_value opt_terms case_body kEND {
                  $$ = support.newCaseNode($1.getPosition(), $2, $4);
              }
              | kCASE opt_terms case_body kEND {
// TODO: MRI is just a when node.  We need this extra logic for IDE consumers (null in casenode statement should be implicit nil)
//                  if (support.getConfiguration().hasExtraPositionInformation()) {
                      $$ = support.newCaseNode($1.getPosition(), null, $3);
//                  } else {
//                      $$ = $3;
//                  }
              }
              | kCASE opt_terms kELSE compstmt kEND {
		  $$ = $4;
              }
              | kFOR for_var kIN {
                  lexer.getConditionState().begin();
              } expr_value do {
                  lexer.getConditionState().end();
              } compstmt kEND {
                  $$ = new ForNode($1.getPosition(), $2, $8, $5, support.getCurrentScope());
              }
              | kCLASS cpath superclass {
                  if (support.isInDef() || support.isInSingle()) {
                      support.yyerror("class definition in method body");
                  }
		  support.pushLocalScope();
              } bodystmt kEND {
                  Node body = $5 == null ? NilImplicitNode.NIL : $5;

                  $$ = new ClassNode($1.getPosition(), $<Colon3Node>2, support.getCurrentScope(), body, $3);
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
                  $$ = new SClassNode($1.getPosition(), $3, support.getCurrentScope(), $7);
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

                  $$ = new ModuleNode($1.getPosition(), $<Colon3Node>2, support.getCurrentScope(), body);
                  support.popCurrentScope();
              }
	      | kDEF fname {
                  support.setInDef(true);
		  support.pushLocalScope();
              } f_arglist bodystmt kEND {
                  // TODO: We should use implicit nil for body, but problem (punt til later)
                  Node body = $5; //$5 == null ? NilImplicitNode.NIL : $5;

                  /* NOEX_PRIVATE for toplevel */
                  $$ = new DefnNode($1.getPosition(), new ArgumentNode($2.getPosition(), (String) $2.getValue()), $<ArgsNode>4, support.getCurrentScope(), body);
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

                  $$ = new DefsNode($1.getPosition(), $2, new ArgumentNode($5.getPosition(), (String) $5.getValue()), $<ArgsNode>7, support.getCurrentScope(), body);
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

primary_value : primary {
                  support.checkExpression($1);
		  $$ = $1;
	      }
 
then          : term
              | ":"
              | kTHEN
              | term kTHEN

do            : term
              | ":"
              | kDO_COND

if_tail       : opt_else 
              | kELSIF expr_value then compstmt if_tail {
                  $$ = new IfNode($1.getPosition(), support.getConditionNode($2), $4, $5);
              }

opt_else      : none 
              | kELSE compstmt {
                  $$ = $2;
              }

for_var       : lhs
              | mlhs {
              }

// [!null]
block_par     : mlhs_item {
                  $$ = support.newArrayNode($1.getPosition(), $1);
              }
              | block_par ',' mlhs_item {
                  $$ = $1.add($3);
              }

block_var     : block_par {
                  if ($1.size() == 1) {
                      $$ = $1.get(0);
                  } else {
                      $$ = new MultipleAsgnNode($1.getPosition(), $1, null);
                  }
              }
              | block_par ',' {
                  $$ = new MultipleAsgnNode($1.getPosition(), $1, null);
              }
              | block_par ',' tAMPER lhs {
                  $$ = support.newBlockArg18($3.getPosition(), $4, new MultipleAsgnNode($1.getPosition(), $1, null));
              }
              | block_par ',' tSTAR lhs ',' tAMPER lhs {
                  $$ = support.newBlockArg18($6.getPosition(), $7, new MultipleAsgnNode($1.getPosition(), $1, $4));
              }
              | block_par ',' tSTAR ',' tAMPER lhs {
                  $$ = support.newBlockArg18($5.getPosition(), $6, new MultipleAsgnNode($1.getPosition(), $1, new StarNode($3.getPosition())));
              }
              | block_par ',' tSTAR lhs {
                  $$ = new MultipleAsgnNode($1.getPosition(), $1, $4);
              }
              | block_par ',' tSTAR {
                  $$ = new MultipleAsgnNode($1.getPosition(), $1, new StarNode($3.getPosition()));
              }
              | tSTAR lhs ',' tAMPER lhs {
                  $$ = support.newBlockArg18($4.getPosition(), $5, new MultipleAsgnNode($1.getPosition(), null, $2));
              }
              | tSTAR ',' tAMPER lhs {
                  $$ = support.newBlockArg18($3.getPosition(), $4, new MultipleAsgnNode($1.getPosition(), null, new StarNode($3.getPosition())));
              }
              | tSTAR lhs {
                  $$ = new MultipleAsgnNode($1.getPosition(), null, $2);
              }
              | tSTAR {
                  $$ = new MultipleAsgnNode($1.getPosition(), null, new StarNode($1.getPosition()));
              }
              | tAMPER lhs {
                  $$ = support.newBlockArg18($1.getPosition(), $2, null);
              }

opt_block_var : none
              | tPIPE /* none */ tPIPE {
                  $$ = new ZeroArgNode($1.getPosition());
                  lexer.commandStart = true;
              }
              | tOROP {
                  $$ = new ZeroArgNode($1.getPosition());
                  lexer.commandStart = true;
	      }
              | tPIPE block_var tPIPE {
                  $$ = $2;
                  lexer.commandStart = true;

		  // Include pipes on multiple arg type
                  if ($2 instanceof MultipleAsgnNode) {
		      $2.setPosition($1.getPosition());
		  } 
              }

do_block      : kDO_BLOCK {
                  support.pushBlockScope();
	      } opt_block_var compstmt kEND {
                  $$ = support.new_iter($1.getPosition(), $3, support.getCurrentScope(), $4);
                  support.popCurrentScope();
              }

// block_call - A call with a block (foo {...}) [!null]
block_call    : command do_block {
                  // Workaround for JRUBY-2326 (MRI does not enter this production for some reason)
                  if ($1 instanceof YieldNode) {
                      throw new SyntaxException(PID.BLOCK_GIVEN_TO_YIELD, $1.getPosition(), lexer.getCurrentLine(), "block given to yield");
                  }
	          if ($<BlockAcceptingNode>1.getIterNode() instanceof BlockPassNode) {
                      throw new SyntaxException(PID.BLOCK_ARG_AND_BLOCK_GIVEN, $1.getPosition(), lexer.getCurrentLine(), "Both block arg and actual block given.");
                  }
		  $$ = $<BlockAcceptingNode>1.setIterNode($2);
		  $<Node>$.setPosition(support.getPosition($1));
              }
              | block_call tDOT operation2 opt_paren_args {
                  $$ = support.new_call($1, $3, $4, null);
              }
              | block_call tCOLON2 operation2 opt_paren_args {
                  $$ = support.new_call($1, $3, $4, null);
              }

method_call   : operation paren_args {
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
              | kSUPER paren_args {
                  $$ = support.new_super($2, $1);
              }
              | kSUPER {
                  $$ = new ZSuperNode($1.getPosition());
              }

// IterNode:brace_block - block invocation argument (foo >{...}< | foo >do end<) [!null]
brace_block   : tLCURLY {
                  support.pushBlockScope();
	      } opt_block_var compstmt tRCURLY {
                  $$ = support.new_iter($1.getPosition(), $3, support.getCurrentScope(), $4);
                  support.popCurrentScope();
              }
              | kDO {
                  support.pushBlockScope();
	      } opt_block_var compstmt kEND {
                  $$ = support.new_iter($1.getPosition(), $3, support.getCurrentScope(), $4);
                  support.popCurrentScope();
              }

case_body     : kWHEN when_args then compstmt cases {
                  $$ = support.newWhenNode($1.getPosition(), $2, $4, $5);
              }

when_args     : args
              | args ',' tSTAR arg_value {
                  $$ = support.arg_concat(support.getPosition($1), $1, $4);
              }
              | tSTAR arg_value {
                  $$ = new SplatNode($1.getPosition(), $2);
              }

cases         : opt_else | case_body


opt_rescue    : kRESCUE exc_list exc_var then compstmt opt_rescue {
                  Node node;
                  if ($3 != null) {
                     node = support.appendToBlock(support.node_assign($3, new GlobalVarNode($1.getPosition(), "$!")), $5);
                     if($5 != null) {
                        node.setPosition(support.unwrapNewlineNode($5).getPosition());
                     }
		  } else {
		     node = $5;
                  }
                  Node body = node == null ? NilImplicitNode.NIL : node;
                  $$ = new RescueBodyNode($1.getPosition(), $2, body, $6);
	      }
              | {
                  $$ = null;
              }

exc_list      : arg_value {
                  $$ = support.newArrayNode($1.getPosition(), $1);
	      }
              | mrhs
	      | none

exc_var       : tASSOC lhs {
                  $$ = $2;
              }
              | none

opt_ensure    : kENSURE compstmt {
                  if ($2 != null) {
                      $$ = $2;
                  } else {
                      $$ = new NilNode(lexer.getPosition());
                  }
              }
              | none

// literal - Any literal value (5, :foo, :"#{bar}") [!null]
literal       : numeric
              | symbol {
                  // FIXME: We may be intern'ing more than once.
                  $$ = new SymbolNode($1.getPosition(), ((String) $1.getValue()).intern());
              }
              | dsym

// Node:strings - EvStr wrapper for strings [!null]
strings       : string {
                  $$ = $1 instanceof EvStrNode ? new DStrNode($1.getPosition()).add($1) : $1;
	      } 

// Node:string - Collection of strings together (a = "a" "b" "c") [!null]
string        : string1
              | string string1 {
                  $$ = support.literal_concat($1.getPosition(), $1, $2);
              }

// Node:string1 - Simple String form ("foo", %q{foo}) [!null]
string1       : tSTRING_BEG string_contents tSTRING_END {
                  $$ = $2;
                  $<ISourcePositionHolder>$.setPosition($1.getPosition());
              }

// Node:xstring - `string` [!null]
xstring	      : tXSTRING_BEG xstring_contents tSTRING_END {
                  ISourcePosition position = $1.getPosition();

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

// Node:regexp - /foo/ [!null]
regexp	      : tREGEXP_BEG xstring_contents tREGEXP_END {
                  $$ = support.newRegexpNode($1.getPosition(), $2, (RegexpNode) $3);
               }

// Node:words - collection of words (e.g. %w{a b c}) with delimeters [!null]
words	       : tWORDS_BEG ' ' tSTRING_END {
                   $$ = new ZArrayNode($1.getPosition());
	       }
	       | tWORDS_BEG word_list tSTRING_END {
		   $$ = $2;
                   $<ISourcePositionHolder>$.setPosition($1.getPosition());
	       }

// ListNode:word_list - collection of words (e.g. %W{a b c}) with out delimeters [!null]
word_list      : /* none */ {
                   $$ = new ArrayNode(lexer.getPosition());
	       }
	       | word_list word ' ' {
                   $$ = $1.add($2 instanceof EvStrNode ? new DStrNode($1.getPosition()).add($2) : $2);
	       }

word	       : string_content
	       | word string_content {
                   $$ = support.literal_concat($1.getPosition(), $1, $2);
	       }

// ListNode:qwords - collection of works (e.g. %w{...}) [!null]
qwords	       : tQWORDS_BEG ' ' tSTRING_END {
                   $$ = new ZArrayNode($1.getPosition());
	       }
	       | tQWORDS_BEG qword_list tSTRING_END {
		   $$ = $2;
                   $<ISourcePositionHolder>$.setPosition($1.getPosition());
	       }

// ListNode:qword_list - collection of works (e.g. %w{...}) [!null]
qword_list     : /* none */ {
                   $$ = new ArrayNode(lexer.getPosition());
	       }
	       | qword_list tSTRING_CONTENT ' ' {
                   $$ = $1.add($2);
	       }

// Node:string_contents - content of a string in raw hunks
string_contents: /* none */ {
                   $$ = new StrNode(lexer.getPosition(), ByteList.create(""));
	       }
	       | string_contents string_content {
                   $$ = support.literal_concat($1.getPosition(), $1, $2);
	       }

xstring_contents: /* none */ {
		   $$ = null;
	       }
	       | xstring_contents string_content {
                   $$ = support.literal_concat(support.getPosition($1), $1, $2);
	       }

// Node:string_content - string contents not including delimeters [!null]
string_content : tSTRING_CONTENT {
                   $$ = $1;
               }
	       | tSTRING_DVAR {
                   $$ = lexer.getStrTerm();
		   lexer.setStrTerm(null);
		   lexer.setState(LexState.EXPR_BEG);
	       } string_dvar {
		   lexer.setStrTerm($<StrTerm>2);
	           $$ = new EvStrNode($1.getPosition(), $3);
	       }
	       | tSTRING_DBEG {
		   $$ = lexer.getStrTerm();
		   lexer.setStrTerm(null);
		   lexer.setState(LexState.EXPR_BEG);
                   lexer.getConditionState().stop();
	           lexer.getCmdArgumentState().stop();
	       } compstmt tRCURLY {
		   lexer.setStrTerm($<StrTerm>2);
                   lexer.getConditionState().restart();
	           lexer.getCmdArgumentState().restart();

		   $$ = support.newEvStrNode($1.getPosition(), $3);
	       }

string_dvar    : tGVAR {
                   $$ = new GlobalVarNode($1.getPosition(), (String) $1.getValue());
               }
	       | tIVAR {
                   $$ = new InstVarNode($1.getPosition(), (String) $1.getValue());
               }
	       | tCVAR {
                   $$ = new ClassVarNode($1.getPosition(), (String) $1.getValue());
               }
	       | backref


// Token:symbol - Represents a symbol [!null]
symbol         : tSYMBEG sym {
                   lexer.setState(LexState.EXPR_END);
                   $$ = $2;
		   $<ISourcePositionHolder>$.setPosition($1.getPosition());
               }

// Token:sym - anything valid after ':' [!null]
sym            : fname | tIVAR | tGVAR | tCVAR

// Node:dsym - Any symbol which has dynamic evaluation [!null]
dsym	       : tSYMBEG xstring_contents tSTRING_END {
                   lexer.setState(LexState.EXPR_END);

                   // DStrNode: :"some text #{some expression}"
                   // StrNode: :"some text"
                   // EvStrNode :"#{some expression}"
                   if ($2 == null) support.yyerror("empty symbol literal");

                   if ($2 instanceof DStrNode) {
                       $$ = new DSymbolNode($1.getPosition(), $<DStrNode>2);
                   } else if ($2 instanceof StrNode) {
                       $$ = new SymbolNode($1.getPosition(), $<StrNode>2.getValue().toString().intern());
                   } else {
                       $$ = new DSymbolNode($1.getPosition());
                       $<DSymbolNode>$.add($2);
                   }
               }

// Node:numeric - numeric value [!null]
numeric        : tINTEGER | tFLOAT {
                   $$ = $1;
               }
	       | tUMINUS_NUM tINTEGER	       %prec tLOWEST {
                   $$ = support.negateInteger($2);
	       }
	       | tUMINUS_NUM tFLOAT	       %prec tLOWEST {
                   $$ = support.negateFloat($2);
	       }

// Token:variable - name (special and normal onces)
variable       : tIDENTIFIER | tIVAR | tGVAR | tCONSTANT | tCVAR
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

// Node:var_ref - Refers to built-in or user-defined variable [!null]
var_ref        : variable {
                   $$ = support.gettable($1);
               }

// AssignableNode:var_lhs - Variable on left hand side of assignment [!null]
var_lhs	       : variable {
                   $$ = support.assignable($1, NilImplicitNode.NIL);
               }

// Token:backref - Back reference (e.g. $3) [!null]
backref        : tNTH_REF | tBACK_REF

// Node:superclass - super class for class definition
superclass     : term {
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

// f_arglist - Function Argument list for definitions
f_arglist      : tLPAREN2 f_args opt_nl tRPAREN {
                   $$ = $2;
                   $<ISourcePositionHolder>$.setPosition($1.getPosition());
                   lexer.setState(LexState.EXPR_BEG);
                   lexer.commandStart = true;
               }
               | f_args term {
                   $$ = $1;
               }

// Node:f_args - Arguments for a method definition [!null]
f_args         : f_arg ',' f_optarg ',' f_rest_arg opt_f_block_arg {
                   $$ = support.new_args($1.getPosition(), $1, $3, $5, null, $6);
               }
               | f_arg ',' f_optarg opt_f_block_arg {
                   $$ = support.new_args($1.getPosition(), $1, $3, null, null, $4);
               }
               | f_arg ',' f_rest_arg opt_f_block_arg {
                   $$ = support.new_args($1.getPosition(), $1, null, $3, null, $4);
               }
               | f_arg opt_f_block_arg {
                   $$ = support.new_args($<ISourcePositionHolder>1.getPosition(), $1, null, null, null, $2);
               }
               | f_optarg ',' f_rest_arg opt_f_block_arg {
                   $$ = support.new_args($1.getPosition(), null, $1, $3, null, $4);
               }
               | f_optarg opt_f_block_arg {
                   $$ = support.new_args($1.getPosition(), null, $1, null, null, $2);
               }
               | f_rest_arg opt_f_block_arg {
                   $$ = support.new_args($1.getPosition(), null, null, $1, null, $2);
               }
               | f_block_arg {
                   $$ = support.new_args($1.getPosition(), null, null, null, null, $1);
               }
               | /* none */ {
                   $$ = support.new_args(support.createEmptyArgsNodePosition(lexer.getPosition()), null, null, null, null, (BlockArgNode) null);
               }

// ArgumentNode:f_norm_arg - normal argument to method declaration [!null]
f_norm_arg     : tCONSTANT {
                   support.yyerror("formal argument cannot be a constant");
               }
               | tIVAR {
                   support.yyerror("formal argument cannot be a instance variable");
               }
               | tGVAR {
                   support.yyerror("formal argument cannot be an global variable");
               }
               | tCVAR {
                   support.yyerror("formal argument cannot be a class variable");
               }
               | tIDENTIFIER {
                   String identifier = (String) $1.getValue();
                   if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                       support.yyerror("duplicate argument name");
                   }

		               int location = support.getCurrentScope().getLocalScope().addVariable(identifier);
                   $$ = new ArgumentNode($1.getPosition(), identifier, location);
               }

// ListNode:f_arg - normal arguments in a method definition [!null]
f_arg          : f_norm_arg tASSOC arg_value {
                    ISourcePosition position = $<ISourcePositionHolder>1.getPosition();
                    support.allowDubyExtension(position);
                    $$ = new ListNode(position).add(new TypedArgumentNode($1, $3));
               }
               | f_norm_arg {
                    $$ = new ListNode($<ISourcePositionHolder>1.getPosition()).add($1);
               }
               | f_arg ',' f_norm_arg tASSOC arg_value {
                   ISourcePosition position = $<ISourcePositionHolder>1.getPosition();
                   support.allowDubyExtension(position);
                   $1.add(new TypedArgumentNode($3, $5));
                   $$ = $1;
               }
               | f_arg ',' f_norm_arg {
                   $1.add($3);
                   $$ = $1;
               }

// Node:f_opt - optional argument in a method definition [!null]
f_opt          : tIDENTIFIER '=' arg_value {
                   String identifier = (String) $1.getValue();

                   if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                       support.yyerror("duplicate optional argument name");
                   }
		   support.getCurrentScope().getLocalScope().addVariable(identifier);
                   $$ = support.assignable($1, $3);
              }

// ListNode:f_optarg - one or more optional arguments in a method definition [!null]
f_optarg      : f_opt {
                  $$ = new BlockNode($1.getPosition()).add($1);
              }
              | f_optarg ',' f_opt {
                  $$ = support.appendToBlock($1, $3);
              }

// Token:restarg_mark - '*' as in '*rest' [!null]
restarg_mark  : tSTAR2 | tSTAR

// Token: rest argument in method declaration (foo(*rest)) [!null]
f_rest_arg    : restarg_mark tIDENTIFIER {
                  String identifier = (String) $2.getValue();

                  if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                      support.yyerror("duplicate rest argument name");
                  }

                  $$ = new RestArgNode($1.getPosition(), (String) $2.getValue(), support.getCurrentScope().getLocalScope().addVariable(identifier));
              }
              | restarg_mark {
                  $$ = new UnnamedRestArgNode($1.getPosition(), "", support.getCurrentScope().getLocalScope().addVariable("*"));
              }

// Token:blkarg_mark - '&' as in '&block' [!null]
blkarg_mark   : tAMPER2 | tAMPER

// f_block_arg - Block argument def for function (foo(&block)) [!null]
f_block_arg   : blkarg_mark tIDENTIFIER {
                  $$ = support.newBlockArg($1.getPosition(), $2);
              }

opt_f_block_arg: ',' f_block_arg {
                  $$ = $2;
              }
              | /* none */ {
	          $$ = null;
	      }

singleton     : var_ref {
		  $$ = $1;
                  support.checkExpression($1);
              }
              | tLPAREN2 {
                  lexer.setState(LexState.EXPR_BEG);
              } expr opt_nl tRPAREN {
                  if ($3 == null) {
                      support.yyerror("can't define single method for ().");
                  } else if ($3 instanceof ILiteralNode) {
                      support.yyerror("can't define single method for literals.");
                  }
		  support.checkExpression($3);
                  $$ = $3;
              }

// ListNode:assoc_list - list of hash values pairs, like assocs but also
//   will accept ordinary list-style (e.g. a,b,c,d or a=>b,c=>d) [?null]
assoc_list    : none { // [!null]
                  $$ = new ArrayNode(lexer.getPosition());
              }
              | assocs trailer { // [!null]
                  $$ = $1;
              }
              | args trailer {
                  if ($1.size() % 2 != 0) {
                      support.yyerror("odd number list for Hash.");
                  }
                  $$ = $1;
              }

// ListNode:assocs - list of hash value pairs (e.g. a => b, c => d) [!null]
assocs        : assoc // [!null]
              | assocs ',' assoc { // [!null]
                  $$ = $1.addAll($3);
              }

// ListNode:assoc - A single hash value pair (e.g. a => b) [!null]
assoc         : arg_value tASSOC arg_value { // [!null]
                  ISourcePosition position;
                  if ($1 == null && $3 == null) {
                      position = $2.getPosition();
                  } else {
                      position = $1.getPosition();
                  }

                  $$ = support.newArrayNode(position, $1).add($3);
              }

operation     : tIDENTIFIER | tCONSTANT | tFID
operation2    : tIDENTIFIER | tCONSTANT | tFID | op
operation3    : tIDENTIFIER | tFID | op
dot_or_colon  : tDOT | tCOLON2
opt_terms     : /* none */ | terms
opt_nl        : /* none */ | '\n'
trailer       : /* none */ | '\n' | ','

term          : ';' {
              }
              | '\n'

terms         : term
              | terms ';' {
              }

none          : /* none */ {
                  $$ = null;
              }

none_block_pass: /* none */ {  
                  $$ = null;
	      }

%%

    /** The parse method use an lexer stream and parse it to an AST node 
     * structure
     */
    public RubyParserResult parse(ParserConfiguration configuration, LexerSource source) throws IOException {
        support.reset();
        support.setConfiguration(configuration);
        support.setResult(new RubyParserResult());
        
        lexer.reset();
        lexer.setSource(source);
        lexer.setEncoding(configuration.getKCode().getEncoding());

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
        // convenience: awt debugger
        //yyparse(lexer, new jay.yydebug.yyAnim("JRuby", 9));
        yyparse(lexer, debugger);
        
        return support.getResult();
    }
}
