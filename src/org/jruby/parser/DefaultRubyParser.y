%{
/*
 * DefaultRubyParser.java - JRuby - Parser constructed from parse.y
 * Created on 07. Oktober 2001, 01:28
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package org.jruby.parser;

import java.math.*;

import org.ablaf.ast.*;
import org.ablaf.common.*;
import org.ablaf.lexer.*;
import org.ablaf.parser.*;

import org.jruby.common.*;
import org.jruby.lexer.yacc.*;
import org.jruby.ast.*;
import org.jruby.ast.types.*;

import org.jruby.ast.util.*;
import org.jruby.runtime.*;
import org.jruby.util.*;

public class DefaultRubyParser implements IParser {
    private ParserSupport support;
    private RubyYaccLexer lexer;

    private IErrorHandler errorHandler;

    public DefaultRubyParser() {
        this.support = new ParserSupport();
        this.lexer = new RubyYaccLexer();
    }

    public void setErrorHandler(IErrorHandler errorHandler) {
        this.errorHandler = errorHandler;

	support.setErrorHandler(errorHandler);
	lexer.setErrorHandler(errorHandler);
    }

/*
%union {
    Node *node;
    VALUE val;
    ID id;
    int num;
    struct RVarmap *vars;
}
*/
%}

%token  kCLASS
    kMODULE
    kDEF
    kUNDEF
    kBEGIN
    kRESCUE
    kENSURE
    kEND
    kIF
    kUNLESS
    kTHEN
    kELSIF
    kELSE
    kCASE
    kWHEN
    kWHILE
    kUNTIL
    kFOR
    kBREAK
    kNEXT
    kREDO
    kRETRY
    kIN
    kDO
    kDO_COND
    kDO_BLOCK
    kRETURN
    kYIELD
    kSUPER
    kSELF
    kNIL
    kTRUE
    kFALSE
    kAND
    kOR
    kNOT
    kIF_MOD
    kUNLESS_MOD
    kWHILE_MOD
    kUNTIL_MOD
    kRESCUE_MOD
    kALIAS
    kDEFINED
    klBEGIN
    klEND
    k__LINE__
    k__FILE__

%token <String>  tIDENTIFIER tFID tGVAR tIVAR tCONSTANT tCVAR
%token <Number>  tINTEGER tFLOAT
%token <String>  tSTRING tXSTRING
%token <RegexpNode>    tREGEXP
%token <INode>    tDXSTRING tDREGEXP tBACK_REF tNTH_REF
%token <DStrNode> tDSTRING
%token <ArrayNode> tARRAY

%type <INode>  singleton string
%type <INode>  literal numeric
%type <INode>  compstmt stmts stmt expr arg primary command command_call method_call
%type <INode>  if_tail opt_else case_body exc_var ensure
%type <INode>  ret_args call_args paren_args opt_paren_args
%type <INode>  command_args var_ref
%type <INode>  mrhs mrhs_basic superclass block_call block_command
%type <INode>  f_arglist f_args f_opt 
%type <BlockArgNode> f_block_arg opt_f_block_arg
%type <INode>  assoc_list undef_list backref
%type <INode>  block_var opt_block_var lhs none
%type <IterNode> brace_block do_block
%type <INode>  mlhs_item mlhs_node
%type <MultipleAsgnNode> mlhs mlhs_basic mlhs_entry
%type <ArrayNode> mlhs_head args when_args exc_list assoc assocs
%type <IListNode> f_optarg aref_args rescue cases
%type <BlockPassNode> block_arg opt_block_arg
%type <String>    fitem variable sym symbol operation operation2 operation3
%type <String>    cname fname op global_var
%type <Integer>   f_rest_arg f_norm_arg f_arg
%token <String> tUPLUS         /* unary+ */
%token <String> tUMINUS        /* unary- */
%token <String> tPOW           /* ** */
%token <String> tCMP           /* <=> */
%token <String> tEQ            /* == */
%token <String> tEQQ           /* === */
%token <String> tNEQ           /* != */
%token <String> tGEQ           /* >= */
%token <String> tLEQ           /* <= */
%token <String> tANDOP tOROP   /* && and || */
%token <String> tMATCH tNMATCH /* =~ and !~ */
%token <String> tDOT2 tDOT3    /* .. and ... */
%token <String> tAREF tASET    /* [] and []= */
%token <String> tLSHFT tRSHFT  /* << and >> */
%token <String> tCOLON2        /* :: */
%token <String> tCOLON3        /* :: at EXPR_BEG */
%token <String> tOP_ASGN       /* +=, -=  etc. */
%token <String> tASSOC         /* => */
%token <String> tLPAREN        /* ( */
%token <String> tLBRACK        /* [ */
%token <String> tLBRACE        /* { */
%token <String> tSTAR          /* * */
%token <String> tAMPER         /* & */
%token <String> tSYMBEG        /* : */

/*
 *    precedence table
 */

%left  kIF_MOD kUNLESS_MOD kWHILE_MOD kUNTIL_MOD kRESCUE_MOD
%left  kOR kAND
%right kNOT
%nonassoc kDEFINED
%right '=' tOP_ASGN
%right '?' ':'
%nonassoc tDOT2 tDOT3
%left  tOROP
%left  tANDOP
%nonassoc  tCMP tEQ tEQQ tNEQ tMATCH tNMATCH
%left  '>' tGEQ '<' tLEQ
%left  '|' '^'
%left  '&'
%left  tLSHFT tRSHFT
%left  '+' '-'
%left  '*' '/' '%'
%right '!' '~' tUPLUS tUMINUS
%right tPOW

%token <Integer> LAST_TOKEN

%%
program     : {
                  // $<Object>$ = ruby.getDynamicVars();
                  lexer.setState(LexState.EXPR_BEG);
                  support.initTopLocalVariables();
		  // FIXME move to ruby runtime
                  //if (ruby.getRubyClass() == ruby.getClasses().getObjectClass()) {
                  //    support.setClassNest(0);
                  //} else {
                  //    support.setClassNest(1);
                  //}
              } compstmt {
                  if ($2 != null && !support.isCompileForEval()) {
                      /* last expression should not be void */
                      if ($2 instanceof BlockNode) {
                          support.checkUselessStatement(ListNodeUtil.getLast($<IListNode>2));
                      } else {
                          support.checkUselessStatement($2);
                      }
                  }
                  support.getResult().setAST(support.appendToBlock(support.getResult().getAST(), $2));
                  support.updateTopLocalVariables();
                  support.setClassNest(0);
                  // ruby.setDynamicVars($<RubyVarmap>1);
              }

compstmt    : stmts opt_terms {
                  if ($1 instanceof BlockNode) {
                     support.checkUselessStatements($<BlockNode>1);
		      }
                  $$ = $1;
              }

stmts         : none
              | stmt {
                    $$ = support.newline_node($1, getPosition());
                }
              | stmts terms stmt {
                    $$ = support.appendToBlock($1, support.newline_node($3, getPosition()));
                }
              | error stmt {
                    $$ = $2;
                }

stmt          : kALIAS fitem {
                    lexer.setState(LexState.EXPR_FNAME);
                } fitem {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("alias within method");
                    }
                    $$ = new AliasNode(getPosition(), $2, $4);
                }
              | kALIAS global_var global_var {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("alias within method");
                    }
                    $$ = new VAliasNode(getPosition(), $2, $3);
                }
              | kALIAS global_var tBACK_REF {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("alias within method");
                    }
                    $$ = new VAliasNode(getPosition(), $2, "$" + $<BackRefNode>3.getType()); // XXX
                }
              | kALIAS global_var tNTH_REF {
                    yyerror("can't make alias for the number variables");
                    $$ = null; //XXX 0
                }
              | kUNDEF undef_list {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("undef within method");
                    }
                    $$ = $2;
                }
              | stmt kIF_MOD expr {
                    support.checkExpression($3);
                    $$ = new IfNode(getPosition(), support.getConditionNode($3), $1, null);
                }
              | stmt kUNLESS_MOD expr {
                    support.checkExpression($3);
                    $$ = new IfNode(getPosition(), support.getConditionNode($3), null, $1);
                }
              | stmt kWHILE_MOD expr {
                    support.checkExpression($3);
                    if ($1 != null && $1 instanceof BeginNode) {
                        $$ = new WhileNode(getPosition(), support.getConditionNode($3), $<BeginNode>1.getBodyNode(), false);
                    } else {
                        $$ = new WhileNode(getPosition(), support.getConditionNode($3), $1, false);
                    }
                }
              | stmt kUNTIL_MOD expr {
                    support.checkExpression($3);
                    if ($1 != null && $1 instanceof BeginNode) {
                        $$ = new UntilNode(getPosition(), support.getConditionNode($3), $<BeginNode>1.getBodyNode());
                    } else {
                        $$ = new UntilNode(getPosition(), support.getConditionNode($3), $1);
                    }
                }
              | stmt kRESCUE_MOD stmt
                {
                    $$ = new RescueNode(getPosition(), $1, new ArrayNode(getPosition()).add(new RescueBodyNode(getPosition(), null,$3)), null);
                }
              | klBEGIN
                {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("BEGIN in method");
                    }
                    support.getLocalNames().push();
                } '{' compstmt '}' {
                    support.getResult().setBeginNodes(support.appendToBlock(support.getResult().getBeginNodes(), new ScopeNode(support.getLocalNames().getNames(), $4)));
                    support.getLocalNames().pop();
                    $$ = null; //XXX 0;
                }
              | klEND '{' compstmt '}' {
                    if (support.isCompileForEval() && (support.isInDef() 
                                              || support.isInSingle())) {
                        yyerror("END in method; use at_exit");
                    }
                    $$ = new IterNode(getPosition(), null, new PostExeNode(getPosition()), $3);
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
              | lhs '=' mrhs_basic {
                    $$ = support.node_assign($1, $3);
                }
              | expr

expr          : mlhs '=' mrhs {
                    support.checkExpression($3);
                    $1.setValueNode($3);
                    $$ = $1;
                }
              | kRETURN ret_args {
                    if (!support.isCompileForEval() && !support.isInDef()
                                               && !support.isInSingle()) {
                        yyerror("return appeared outside of method");
                    }
                    $$ = new ReturnNode(getPosition(), $2);
                }
              | command_call
              | expr kAND expr {
                    $$ = support.newAndNode($1, $3);
                }
              | expr kOR expr {
                    $$ = support.newOrNode($1, $3);
                }
              | kNOT expr {
                    support.checkExpression($2);
                    $$ = new NotNode(getPosition(), support.getConditionNode($2));
                }
              | '!' command_call {
                    $$ = new NotNode(getPosition(), support.getConditionNode($2));
                }
              | arg

command_call  : command
              | block_command

block_command : block_call
              | block_call '.' operation2 command_args {
                    support.checkExpression($1);
                    $$ = support.new_call($1, $3, $4);
                }
              | block_call tCOLON2 operation2 command_args {
	            support.checkExpression($1);
                    $$ = support.new_call($1, $3, $4);
                }

command       : operation command_args {
                    $$ = support.new_fcall($1, $2, getPosition()); // .setPosFrom($2);
                }
              | primary '.' operation2 command_args {
                    support.checkExpression($1);
                    $$ = support.new_call($1, $3, $4); //.setPosFrom($1);
                }
              | primary tCOLON2 operation2 command_args {
                    support.checkExpression($1);
                    $$ = support.new_call($1, $3, $4); //.setPosFrom($1);
                }
              | kSUPER command_args {
                    if (!support.isCompileForEval() && support.isInDef() 
                                               && support.isInSingle()){
                        yyerror("super called outside of method");
                    }
		    $$ = support.new_super($2, getPosition()); // .setPosFrom($2);
		}
              | kYIELD ret_args {
	            $$ = new YieldNode(getPosition(), $2); // .setPosFrom($2);
		}

mlhs          : mlhs_basic
              | tLPAREN mlhs_entry ')' {
                    $$ = $2;
		}

mlhs_entry    : mlhs_basic
              | tLPAREN mlhs_entry ')' {
	            $$ = new MultipleAsgnNode(getPosition(), new ArrayNode(getPosition()).add($2), null);
                }

mlhs_basic    : mlhs_head {
                    $$ = new MultipleAsgnNode(getPosition(), $1, null);
                }
              | mlhs_head mlhs_item {
                    $$ = new MultipleAsgnNode(getPosition(), $1.add($2), null);
                }
              | mlhs_head tSTAR mlhs_node {
                    $$ = new MultipleAsgnNode(getPosition(), $1, $3);
                }
              | mlhs_head tSTAR {
                    $$ = new MultipleAsgnNode(getPosition(), $1, new StarNode());
                }
              | tSTAR mlhs_node {
                    $$ = new MultipleAsgnNode(getPosition(), null, $2);
                }
              | tSTAR {
                    $$ = new MultipleAsgnNode(getPosition(), null, new StarNode());
                }

mlhs_item     : mlhs_node
              | tLPAREN mlhs_entry ')' {
                    $$ = $2;
                }

mlhs_head     : mlhs_item ',' {
                    $$ = new ArrayNode(getPosition()).add($1);
                }
              | mlhs_head mlhs_item ',' {
                    $$ = $1.add($2);
                }

mlhs_node     : variable {
                    $$ = support.getAssignmentNode($1, null, getPosition());
                }
              | primary '[' aref_args ']' {
                    $$ = support.getElementAssignmentNode($1, $3);
                }
              | primary '.' tIDENTIFIER {
                    $$ = support.getAttributeAssignmentNode($1, $3);
                }
              | primary tCOLON2 tIDENTIFIER {
                    $$ = support.getAttributeAssignmentNode($1, $3);
                }
              | primary '.' tCONSTANT {
                    $$ = support.getAttributeAssignmentNode($1, $3);
                }
              | backref {
	            support.backrefAssignError($1);
                    $$ = null;
                }

lhs           : variable {
                    $$ = support.getAssignmentNode($1, null, getPosition());
                }
              | primary '[' aref_args ']' {
                    $$ = support.getElementAssignmentNode($1, $3);
                }
              | primary '.' tIDENTIFIER {
                    $$ = support.getAttributeAssignmentNode($1, $3);
                }
              | primary tCOLON2 tIDENTIFIER {
                    $$ = support.getAttributeAssignmentNode($1, $3);
                }
              | primary '.' tCONSTANT {
                    $$ = support.getAttributeAssignmentNode($1, $3);
                }
              | backref {
                    support.backrefAssignError($1);
                    $$ = null;
		}

cname         : tIDENTIFIER {
                    yyerror("class/module name must be CONSTANT");
                }
              | tCONSTANT

fname         : tIDENTIFIER
              | tCONSTANT
              | tFID
              | op {
                    lexer.setState(LexState.EXPR_END);
                    $$ = $1;
                }
              | reswords {
                    lexer.setState(LexState.EXPR_END);
                    $$ = $<>1;
                }

fitem         : fname
              | symbol

undef_list    : fitem {
                    $$ = new UndefNode(getPosition(), $1);
                }
              | undef_list ',' {
                    lexer.setState(LexState.EXPR_FNAME);
	        } fitem {
                    $$ = support.appendToBlock($1, new UndefNode(getPosition(), $4));
                }

op            : '|'     { $$ = "|"; }
              | '^'     { $$ = "^"; }
              | '&'     { $$ = "&"; }
              | tCMP    { $$ = "<=>"; }
              | tEQ     { $$ = "=="; }
              | tEQQ    { $$ = "==="; }
              | tMATCH  { $$ = "=~"; }
              | '>'     { $$ = ">"; }
              | tGEQ    { $$ = ">="; }
              | '<'     { $$ = "<"; }
              | tLEQ    { $$ = "<="; }
              | tLSHFT  { $$ = "<<"; }
              | tRSHFT  { $$ = ">>"; }
              | '+'     { $$ = "+"; }
              | '-'     { $$ = "-"; }
              | '*'     { $$ = "*"; }
              | tSTAR   { $$ = "*"; }
              | '/'     { $$ = "/"; }
              | '%'     { $$ = "%"; }
              | tPOW    { $$ = "**"; }
              | '~'     { $$ = "~"; }
              | tUPLUS  { $$ = "+@"; }
              | tUMINUS { $$ = "-@"; }
              | tAREF   { $$ = "[]"; }
              | tASET   { $$ = "[]="; }
              | '`'     { $$ = "`"; }

reswords      : k__LINE__ 
              | k__FILE__ 
              | klBEGIN 
              | klEND
              | kALIAS
              | kAND
              | kBEGIN
              | kBREAK
              | kCASE
              | kCLASS
              | kDEF
              | kDEFINED
              | kDO
              | kELSE
              | kELSIF
              | kEND
              | kENSURE
              | kFALSE
              | kFOR
              | kIF_MOD
              | kIN
              | kMODULE
              | kNEXT
              | kNIL
              | kNOT
              | kOR
              | kREDO 
              | kRESCUE 
              | kRETRY 
              | kRETURN 
              | kSELF 
              | kSUPER
              | kTHEN 
              | kTRUE 
              | kUNDEF 
              | kUNLESS_MOD 
              | kUNTIL_MOD 
              | kWHEN
              | kWHILE_MOD 
              | kYIELD 
              | kRESCUE_MOD

arg           : lhs '=' arg {
                    support.checkExpression($3);
                    $$ = support.node_assign($1, $3);
                }
              | variable tOP_ASGN {
                    $$ = support.getAssignmentNode($1, null, getPosition());
                } arg {
                    if ($2.equals("||")) {
	                $<IAssignableNode>3.setValueNode($4);
	                $$ = new OpAsgnOrNode(getPosition(), support.getAccessNode($1, getPosition()), $<INode>3);
                        // FIXME
			// if (IdUtil.isInstanceVariable($1)) {
                        //    $<Node>$.setAId($1);
                        // }
                    } else if ($2.equals("&&")) {
                        $<IAssignableNode>3.setValueNode($4);
                        $$ = new OpAsgnAndNode(getPosition(), support.getAccessNode($1, getPosition()), $<INode>3);
                    } else {
                        $$ = $3;
                        if ($$ != null) {
                            $<IAssignableNode>$.setValueNode(support.getOperatorCallNode(support.getAccessNode($1, getPosition()), $2, $4));
                        }
                    }
                    // $<Node>$.setPosFrom($4);
                }
              | primary '[' aref_args ']' tOP_ASGN arg {
                    $$ = new OpElementAsgnNode(getPosition(), $1, $5, $3, $6);
                }
              | primary '.' tIDENTIFIER tOP_ASGN arg {
                    $$ = new OpAsgnNode(getPosition(), $1, $5, $3, $4);
                }
              | primary '.' tCONSTANT tOP_ASGN arg {
                    $$ = new OpAsgnNode(getPosition(), $1, $5, $3, $4);
                }
              | primary tCOLON2 tIDENTIFIER tOP_ASGN arg {
                    $$ = new OpAsgnNode(getPosition(), $1, $5, $3, $4);
                }
              | backref tOP_ASGN arg {
                    support.backrefAssignError($1);
                    $$ = null;
                }
              | arg tDOT2 arg {
                    $$ = new DotNode(getPosition(), $1, $3, false);
                }
              | arg tDOT3 arg {
                    $$ = new DotNode(getPosition(), $1, $3, true);
                }
              | arg '+' arg {
                    $$ = support.getOperatorCallNode($1, "+", $3);
                }
              | arg '-' arg {
                    $$ = support.getOperatorCallNode($1, "-", $3);
                }
              | arg '*' arg {
                    $$ = support.getOperatorCallNode($1, "*", $3);
                }
              | arg '/' arg {
                    $$ = support.getOperatorCallNode($1, "/", $3);
                }
              | arg '%' arg {
                    $$ = support.getOperatorCallNode($1, "%", $3);
                }
              | arg tPOW arg {
                    /* Covert '- number ** number' to '- (number ** number)' */
                    boolean needNegate = false;
                    if (($1 instanceof FixnumNode && $<FixnumNode>1.getValue() < 0) ||
                        ($1 instanceof BignumNode && $<BignumNode>1.getValue().compareTo(BigInteger.ZERO) < 0) ||
                        ($1 instanceof FloatNode && $<FloatNode>1.getValue() < 0.0)) {

                        $<>1 = support.getOperatorCallNode($1, "-@");
                        needNegate = true;
                    }

                    $$ = support.getOperatorCallNode($1, "**", $3);

                    if (needNegate) {
                        $$ = support.getOperatorCallNode($<INode>$, "-@");
                    }
                }
              | tUPLUS arg {
                    $$ = support.getOperatorCallNode($2, "+@");
                }
              | tUMINUS arg {
                    $$ = support.getOperatorCallNode($2, "-@");
                }
              | arg '|' arg {
                    $$ = support.getOperatorCallNode($1, "|", $3);
                }
              | arg '^' arg {
                    $$ = support.getOperatorCallNode($1, "^", $3);
                }
              | arg '&' arg {
                    $$ = support.getOperatorCallNode($1, "&", $3);
                }
              | arg tCMP arg {
                    $$ = support.getOperatorCallNode($1, "<=>", $3);
                }
              | arg '>' arg {
                    $$ = support.getOperatorCallNode($1, ">", $3);
                }
              | arg tGEQ arg {
                    $$ = support.getOperatorCallNode($1, ">=", $3);
                }
              | arg '<' arg {
                    $$ = support.getOperatorCallNode($1, "<", $3);
                }
              | arg tLEQ arg {
                    $$ = support.getOperatorCallNode($1, "<=", $3);
                }
              | arg tEQ arg {
                    $$ = support.getOperatorCallNode($1, "==", $3);
                }
              | arg tEQQ arg {
                    $$ = support.getOperatorCallNode($1, "===", $3);
                }
              | arg tNEQ arg {
                    $$ = new NotNode(getPosition(), support.getOperatorCallNode($1, "==", $3));
                }
              | arg tMATCH arg {
                    $$ = support.getMatchNode($1, $3);
                }
              | arg tNMATCH arg {
                    $$ = new NotNode(getPosition(), support.getMatchNode($1, $3));
                }
              | '!' arg {
                    support.checkExpression($2);
                    $$ = new NotNode(getPosition(), support.getConditionNode($2));
                }
              | '~' arg {
                    $$ = support.getOperatorCallNode($2, "~");
                }
              | arg tLSHFT arg {
                    $$ = support.getOperatorCallNode($1, "<<", $3);
                }
              | arg tRSHFT arg {
                    $$ = support.getOperatorCallNode($1, ">>", $3);
                }
              | arg tANDOP arg {
                    $$ = support.newAndNode($1, $3);
                }
              | arg tOROP arg {
                    $$ = support.newOrNode($1, $3);
                }
              | kDEFINED opt_nl {
	            support.setInDefined(true);
		} arg {
                    support.setInDefined(false);
                    $$ = new DefinedNode(getPosition(), $4);
                }
              | arg '?' arg ':' arg {
                    support.checkExpression($1);
                    $$ = new IfNode(getPosition(), support.getConditionNode($1), $3, $5);
                }
              | primary
                {
                    $$ = $1;
                }

aref_args     : /* none */ {
                    $$ = null;
                }
              | command_call opt_nl {
                    $$ = new ArrayNode(getPosition()).add($1);
                }
              | args ',' command_call opt_nl {
                    $$ = $1.add($3);
                }
              | args trailer {
                    $$ = $1;
                }
              | args ',' tSTAR arg opt_nl {
                    support.checkExpression($4);
                    $$ = $1.add(new ExpandArrayNode($4));
                }
              | assocs trailer {
                    $$ = new ArrayNode(getPosition()).add(new HashNode($1));
                }
              | tSTAR arg opt_nl {
                    support.checkExpression($2);

                    $$ = new ArrayNode(getPosition()).add(new ExpandArrayNode($2));
                }

paren_args    : '(' none ')' {
                    $$ = $2;
                }
              | '(' call_args opt_nl ')' {
                    $$ = $2;
                }
              | '(' block_call opt_nl ')' {
                    $$ = new ArrayNode(getPosition()).add($2);
                }
              | '(' args ',' block_call opt_nl ')' {
                    $$ = $2.add($4);
                }

opt_paren_args: none
              | paren_args

call_args     : command {
                    $$ = new ArrayNode(getPosition()).add($1);
                }
              | args ',' command {
                    $$ = $1.add($3);
                }
              | args opt_block_arg {
                    $$ = support.arg_blk_pass($1, $2);
                }
              | args ',' tSTAR arg opt_block_arg {
                    support.checkExpression($4);
                    $$ = support.arg_blk_pass($1.add(new ExpandArrayNode($4)), $5);
                }
              | assocs opt_block_arg {
                    $$ = support.arg_blk_pass(new ArrayNode(getPosition()).add(new HashNode($1)), $2);
                }
              | assocs ',' tSTAR arg opt_block_arg {
                    support.checkExpression($4);
                    $$ = support.arg_blk_pass($1.add(new ExpandArrayNode($4)), $5);
                }
              | args ',' assocs opt_block_arg {
                    $$ = support.arg_blk_pass($1.add(new HashNode($3)), $4);
                }
              | args ',' assocs ',' tSTAR arg opt_block_arg {
                    support.checkExpression($6);
                    $$ = support.arg_blk_pass($1.add(new HashNode($3)).add(new ExpandArrayNode($6)), $7);
                }
              | tSTAR arg opt_block_arg {
                    support.checkExpression($2);
		    // FIXME
                    // $$ = support.arg_blk_pass(new RestArgsNode(getPosition(), $2), $3);
		    $$ = support.arg_blk_pass(new ArrayNode(getPosition()).add(new ExpandArrayNode($2)), $3);
                }
              | block_arg {
	            $$ = $1;
	        }

command_args  : { 
		    $$ = new Long(lexer.getCmdArgumentState().begin());
		} call_args {
                    lexer.getCmdArgumentState().reset($<Long>1.longValue());
                    $$ = $2;
                }

block_arg     : tAMPER arg {
                    support.checkExpression($2);
                    $$ = new BlockPassNode(getPosition(), $2);
                }

opt_block_arg : ',' block_arg {
                    $$ = $2;
                }
              | /* none */ {
	            $$ = null;
	      }

args          : arg {
                    support.checkExpression($1);
                    $$ = new ArrayNode(getPosition()).add($1);
                }
              | args ',' arg {
                    support.checkExpression($3);
                    $$ = $1.add($3);
                }

mrhs          : arg {
                    support.checkExpression($1);
                    $$ = $1;
                }
              | mrhs_basic

mrhs_basic    : args ',' arg {
                    support.checkExpression($3);
                    $$ = $1.add($3);
                }
              | args ',' tSTAR arg {
                    support.checkExpression($4);
                    $$ = $1.add(new ExpandArrayNode($4));
                }
              | tSTAR arg {
                    support.checkExpression($2);
                    $$ = $2;
                }

ret_args      : call_args {
                    $$ = $1;
                    if ($1 instanceof ArrayNode && ListNodeUtil.getLength($<IListNode>1) == 1) {
                        $$ = ListNodeUtil.getLast($<IListNode>1);
                    } else if ($1 instanceof BlockPassNode) {
                        errorHandler.handleError(IErrors.COMPILE_ERROR, null, "Block argument should not be given.");
                    }
                }

primary       : literal
              | string
	      | tARRAY {
	            $$ = $1;
	        }
              | tXSTRING {
                    $$ = new XStrNode(getPosition(), $1);
                }
              | tDXSTRING
              | tDREGEXP {
	            support.getLocalNames().getLocalIndex("~");
	            $$ = $1;
	        }
              | var_ref
              | backref
              | tFID {
                    $$ = new VCallNode(getPosition(), $1);
                }
              | kBEGIN compstmt rescue opt_else ensure kEND {
                    if ($3 == null && $4 == null && $5 == null) {
                        $$ = new BeginNode(getPosition(), $2);
                    } else {
                        if ($3 != null) {
                            $<>2 = new RescueNode(getPosition(), $2, $3, $4);
                        } else if ($4 != null) {
			    errorHandler.handleError(IErrors.WARN, null, "else without rescue is useless");
                            $<>2 = support.appendToBlock($2, $4);
                        }
                        if ($5 != null) {
                            $<>2 = new EnsureNode(getPosition(), $2, $5);
                        }
                        $$ = $2;
                    }
                    // $<Node>$.setPosFrom($2);
                }
              | tLPAREN compstmt ')' {
                    $$ = $2;
                }
              | primary tCOLON2 tCONSTANT {
                    support.checkExpression($1);
                    $$ = new Colon2Node(getPosition(), $1, $3);
                }
              | tCOLON3 cname {
                    $$ = new Colon3Node(getPosition(), $2);
                }
              | primary '[' aref_args ']' {
                    support.checkExpression($1);
                    $$ = new CallNode(getPosition(), $1, "[]", $3);
                }
              | tLBRACK aref_args ']' {
                    if ($2 == null) {
                        $$ = new ArrayNode(getPosition()); /* zero length array*/
                    } else {
                        $$ = $2;
                    }
                }
              | tLBRACE assoc_list '}' {
                    $$ = new HashNode(getPosition(), $<IListNode>2);
                }
              | kRETURN '(' ret_args ')' {
                    if (!support.isCompileForEval() && !support.isInDef() 
                                               && !support.isInSingle()) {
                        yyerror("return appeared outside of method");
                    }
                    support.checkExpression($3);
                    $$ = new ReturnNode(getPosition(), $3);
                }
              | kRETURN '(' ')' {
                    if (!support.isCompileForEval() && !support.isInDef()
                                               && !support.isInSingle()) {
                        yyerror("return appeared outside of method");
                    }
                    $$ = new ReturnNode(getPosition(), null);
                }
              | kRETURN {
                    if (!support.isCompileForEval() && !support.isInDef()
                                               && !support.isInSingle()) {
                        yyerror("return appeared outside of method");
                    }
                    $$ = new ReturnNode(getPosition(), null);
                }
              | kYIELD '(' ret_args ')' {
                    support.checkExpression($3);
                    $$ = new YieldNode(getPosition(), $3);
                }
              | kYIELD '(' ')' {
                    $$ = new YieldNode(getPosition(), null);
                }
              | kYIELD {
                    $$ = new YieldNode(getPosition(), null);
                }
              | kDEFINED opt_nl '(' {
	            support.setInDefined(true);
		} expr ')' {
                    support.setInDefined(false);
                    $$ = new DefinedNode(getPosition(), $5);
                }
              | operation brace_block {
                    $2.setIterNode(new FCallNode(getPosition(), $1, null));
                    $$ = $2;
                }
              | method_call
              | method_call brace_block {
                    if ($1 instanceof BlockPassNode) {
                       errorHandler.handleError(IErrors.COMPILE_ERROR, null, "Both block arg and actual block given.");
                    }
                    $2.setIterNode($1);
                    $$ = $2;
                    // $<Node>$.setPosFrom($1);
                }
              | kIF expr then compstmt if_tail kEND {
                    support.checkExpression($2);
                    $$ = new IfNode(getPosition(), support.getConditionNode($2), $4, $5);
                }
              | kUNLESS expr then compstmt opt_else kEND {
                    support.checkExpression($2);
                    $$ = new IfNode(getPosition(), support.getConditionNode($2), $5, $4);
                }
              | kWHILE { 
	            lexer.getConditionState().begin();
		} expr do {
		    lexer.getConditionState().end();
		} compstmt kEND {
                    support.checkExpression($3);
                    $$ = new WhileNode(getPosition(), support.getConditionNode($3), $6);
                }
              | kUNTIL {
                    lexer.getConditionState().begin();
                } expr do {
                    lexer.getConditionState().end();
                } compstmt kEND {
                    support.checkExpression($3);
                    $$ = new UntilNode(getPosition(), support.getConditionNode($3), $6);
                }
              | kCASE expr opt_terms cases opt_else kEND {
                    support.checkExpression($2);
                    $$ = new CaseNode(getPosition(), $2, $4, $5); // XXX
                }
              | kCASE opt_terms cases opt_else kEND {
                    $$ = new CaseNode(getPosition(), null, $3, $4);
                }
              | kFOR block_var kIN {
                    lexer.getConditionState().begin();
                } expr do {
                    lexer.getConditionState().end();
                } compstmt kEND {
                    support.checkExpression($5);
                    $$ = new ForNode(getPosition(), $2, $8, $5);
                }
              | kCLASS cname superclass {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("class definition in method body");
                    }
                    support.setClassNest(support.getClassNest() + 1);
                    support.getLocalNames().push();
                    // $$ = new Integer(ruby.getSourceLine());
                } compstmt kEND {
                    $$ = new ClassNode(getPosition(), $2, new ScopeNode(support.getLocalNames().getNames(), $5), $3);
                    // $<INode>$.setLine($<Integer>4.intValue());
                    support.getLocalNames().pop();
                    support.setClassNest(support.getClassNest() - 1);
                }
              | kCLASS tLSHFT expr {
                    $$ = new Boolean(support.isInDef());
                    support.setInDef(false);
                } term {
                    $$ = new Integer(support.getInSingle());
                    support.setInSingle(0);
                    support.setClassNest(support.getClassNest() + 1);
                    support.getLocalNames().push();
                } compstmt kEND {
                    $$ = new SClassNode(getPosition(), $3, new ScopeNode(support.getLocalNames().getNames(), $7));
                    support.getLocalNames().pop();
                    support.setClassNest(support.getClassNest() - 1);
                    support.setInDef($<Boolean>4.booleanValue());
                    support.setInSingle($<Integer>6.intValue());
                }
              | kMODULE cname {
                    if (support.isInDef() || support.isInSingle()) { 
                        yyerror("module definition in method body");
                    }
                    support.setClassNest(support.getClassNest() + 1);
                    support.getLocalNames().push();
                    // $$ = new Integer(ruby.getSourceLine());
                } compstmt kEND {
                    $$ = new ModuleNode(getPosition(), $2, new ScopeNode(support.getLocalNames().getNames(), $4));
                    // $<Node>$.setLine($<Integer>3.intValue());
                    support.getLocalNames().pop();
                    support.setClassNest(support.getClassNest() - 1);
                }
	      | kDEF fname {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("nested method definition");
                    }
                    support.setInDef(true);
                    support.getLocalNames().push();
                } f_arglist compstmt rescue opt_else ensure kEND {
                    if ($6 != null) {
                        $<>5 = new RescueNode(getPosition(), $5, $6, $7);
                    } else if ($7 != null) {
		        errorHandler.handleError(IErrors.WARN, null, "Else without rescue is useless.");
                        $<>5 = support.appendToBlock($5, $7);
                    }
                    if ($8 != null) {
                        $<>5 = new EnsureNode(getPosition(), $5, $8);
                    }

                    /* NOEX_PRIVATE for toplevel */
                    $$ = new DefnNode(getPosition(), $2, $4,
		                      new ScopeNode(support.getLocalNames().getNames(), $5),
		                      support.getClassNest() !=0 || IdUtil.isAttrSet($2) ? Visibility.PUBLIC : Visibility.PRIVATE);
                    // $<Node>$.setPosFrom($4);
                    support.getLocalNames().pop();
                    support.setInDef(false);
                }
              | kDEF singleton dot_or_colon {
                    lexer.setState(LexState.EXPR_FNAME);
                } fname {
                    support.checkExpression($2);
                    support.setInSingle(support.getInSingle() + 1);
                    support.getLocalNames().push();
                    lexer.setState(LexState.EXPR_END); /* force for args */
                } f_arglist compstmt rescue opt_else ensure kEND {
                    if ($9 != null) {
                        $<>8 = new RescueNode(getPosition(), $8, $9, $10);
                    } else if ($10 != null) {
		        errorHandler.handleError(IErrors.WARN, null, "Else without rescue is useless.");
                        $<>8 = support.appendToBlock($8, $10);
                    }
                    if ($11 != null) {
                        $<>8 = new EnsureNode(getPosition(), $8, $11);
                    }
                    $$ = new DefsNode(getPosition(), $2, $5, $7, new ScopeNode(support.getLocalNames().getNames(), $8));
                    // $<Node>$.setPosFrom($2);
                    support.getLocalNames().pop();
                    support.setInSingle(support.getInSingle() - 1);
                }
              | kBREAK {
                    $$ = new BreakNode(getPosition());
                }
              | kNEXT {
                    $$ = new NextNode(getPosition());
                }
              | kREDO {
                    $$ = new RedoNode(getPosition());
                }
              | kRETRY {
                    $$ = new RetryNode(getPosition());
                }
 
then          : term
              | kTHEN
              | term kTHEN

do            : term
              | kDO_COND

if_tail       : opt_else
              | kELSIF expr then compstmt if_tail {
                    support.checkExpression($2);
                    $$ = new IfNode(getPosition(), support.getConditionNode($2), $4, $5);
                }

opt_else      : none
              | kELSE compstmt {
                    $$ = $2;
                }

block_var     : lhs
              | mlhs {
	            $$ = $1;
	      }

opt_block_var : none
              | '|' /* none */ '|' {
                    $$ = new ZeroArgNode();
                }
              | tOROP {
                    $$ = new ZeroArgNode();
		}
              | '|' block_var '|' {
                    $$ = $2;
                }

do_block      : kDO_BLOCK {
                    support.getBlockNames().push();
                } opt_block_var compstmt kEND {
                    $$ = new IterNode(getPosition(), $3, $4, null);
                    support.getBlockNames().pop();
                }

block_call    : command do_block {
                    if ($1 instanceof BlockPassNode) {
		        errorHandler.handleError(IErrors.COMPILE_ERROR, null, "Both block arg and actual block given.");
                    }
                    $2.setIterNode($1);
                    $$ = $2;
                    // $$$2);
                }
              | block_call '.' operation2 opt_paren_args {
                    support.checkExpression($1);
                    $$ = support.new_call($1, $3, $4);
                }
              | block_call tCOLON2 operation2 opt_paren_args {
                    support.checkExpression($1);
                    $$ = support.new_call($1, $3, $4);
                }

method_call   : operation paren_args {
                    $$ = support.new_fcall($1, $2, getPosition()); // .setPosFrom($2);
                }
              | primary '.' operation2 opt_paren_args {
                    support.checkExpression($1);
                    $$ = support.new_call($1, $3, $4); //.setPosFrom($1);
                }
              | primary tCOLON2 operation2 paren_args {
                    support.checkExpression($1);
                    $$ = support.new_call($1, $3, $4); //.setPosFrom($1);
                }
              | primary tCOLON2 operation3 {
                    support.checkExpression($1);
                    $$ = support.new_call($1, $3, null);
                }
              | kSUPER paren_args {
                    if (!support.isCompileForEval() && !support.isInDef()
                                    && !support.isInSingle() && !support.isInDefined()) {
                        yyerror("super called outside of method");
                    }
                    $$ = support.new_super($2, getPosition());
                }
              | kSUPER {
                    if (!support.isCompileForEval() && !support.isInDef()
                                    && !support.isInSingle() && !support.isInDefined()) {
                        yyerror("super called outside of method");
                    }
                    $$ = new ZSuperNode(getPosition());
                }

brace_block   : '{' {
                    support.getBlockNames().push();
                } opt_block_var compstmt '}' {
                    $$ = new IterNode(getPosition(), $3, $4, null);
                    support.getBlockNames().pop();
                }
              | kDO {
                    support.getBlockNames().push();
                } opt_block_var compstmt kEND {
                    $$ = new IterNode(getPosition(), $3, $4, null);
                    support.getBlockNames().pop();
                }

case_body     : kWHEN when_args then compstmt {
                    $$ = new WhenNode(getPosition(), $2, $4);
                }

when_args     : args
              | args ',' tSTAR arg {
                    support.checkExpression($4);
                    $$ = $1.add(new ExpandArrayNode($4));
                }
              | tSTAR arg {
                    support.checkExpression($2);
                    $$ = new ArrayNode(getPosition()).add(new ExpandArrayNode($2));
                }

cases         : cases case_body {
                    $$ = $1.add($2);
                }
              | case_body {
	            $$ = new ArrayNode(getPosition()).add($1);
	        }

exc_list      : /* none */ {
                    $$ = null;
                }
              | args

exc_var       : tASSOC lhs {
                    $$ = $2;
                }
              | none

rescue        : rescue kRESCUE exc_list exc_var then compstmt {
                    if ($4 != null) {
                        $<>4 = support.node_assign($4, new GlobalVarNode(getPosition(), "$!"));
                        $<>6 = support.appendToBlock($4, $6);
                    }
		    if ($1 == null) {
		    	$<>1 = new ArrayNode(getPosition());
		    }
                    $$ = $1.add(new RescueBodyNode(getPosition(), $3, $6));
                }
              | /* none */ {
	            $$ = null;
	        }

ensure        : none
              | kENSURE compstmt {
                    //if ($2 != null) {
                        $$ = $2;
                    //} else {
                    //    $$ = new NilNode(null);
                    //}
                }

literal       : numeric
              | symbol {
                    $$ = new SymbolNode(getPosition(), $1);
                }
              | tREGEXP {
	            support.getLocalNames().getLocalIndex("~");  
	            $$ = $1;
	        }

string        : string tSTRING {
	            /* FIXME */
                    if ($1 instanceof DStrNode) {
                        $<DStrNode>1.add(new StrNode(getPosition(), $2));
                    } else {
                        $<StrNode>1.setValue($<StrNode>1.getValue() + $2);
                    }
                    $$ = $1;
                }
              | string tDSTRING {
	            /* FIXME */
                    if ($1 instanceof StrNode) {
                        $$ = new DStrNode(getPosition());
			$<DStrNode>$.add($1);
                    } else {
                        $$ = $1;
                    }
		    $$ = ListNodeUtil.addAll($<DStrNode>$, $2);
                }
              | tSTRING {
                    $$ = new StrNode(getPosition(), $1);
                }
	      | tDSTRING {
	            $$ = $1;
	        }

symbol        : tSYMBEG sym {
                    lexer.setState(LexState.EXPR_END);
                    $$ = $2;
                }

sym           : fname
              | tIVAR
              | global_var
              | tCVAR

numeric       : tINTEGER {
                    if ($1 instanceof Long) {
                        $$ = new FixnumNode(getPosition(), $<Long>1.longValue());
                    } else {
                        $$ = new BignumNode(getPosition(), $<BigInteger>1);
                    }
                }
              | tFLOAT {
	                $$ = new FloatNode(getPosition(), $<Double>1.doubleValue());
	            }

variable      : tIDENTIFIER
              | tIVAR
              | global_var
              | tCONSTANT
              | tCVAR

global_var    : tGVAR {
                    if ($1.equals("$_") || $1.equals("$~")) {
		        support.getLocalNames().getLocalIndex("~");
		    }
                    $$ = $1;
                }

var_ref       : variable {
                    $$ = support.getAccessNode($1, getPosition());
                }
              | kNIL { 
                    $$ = new NilNode(getPosition());
                }
              | kSELF {
                    $$ = new SelfNode(getPosition());
                }
              | kTRUE { 
                    $$ = new TrueNode(getPosition());
                }
              | kFALSE {
                    $$ = new FalseNode(getPosition());
                }
              | k__FILE__ {
                    $$ = new StrNode(getPosition(), getPosition().getFile());
                }
              | k__LINE__ {
                    $$ = new FixnumNode(getPosition(), getPosition().getLine());
                }

backref       : tNTH_REF  {
                    $$ = $1;
                }
              | tBACK_REF {
	            $$ = $1;
	        }

superclass    : term {
                    $$ = null;
                }
              | '<' {
                    lexer.setState(LexState.EXPR_BEG);
                } expr term {
                    $$ = $3;
                }
              | error term {
                    yyerrok();
                    $$ = null;
                }

f_arglist     : '(' f_args opt_nl ')' {
                    $$ = $2;
                    lexer.setState(LexState.EXPR_BEG);
                }
              | f_args term {
                    $$ = $1;
                }

f_args        : f_arg ',' f_optarg ',' f_rest_arg opt_f_block_arg {
                    $$ = new ArgsNode(getPosition(), $1.intValue(), $3, $5.intValue(), $6);
                }
              | f_arg ',' f_optarg opt_f_block_arg {
                    $$ = new ArgsNode(getPosition(), $1.intValue(), $3, -1, $4);
                }
              | f_arg ',' f_rest_arg opt_f_block_arg {
                    $$ = new ArgsNode(getPosition(), $1.intValue(), null, $3.intValue(), $4);
                }
              | f_arg opt_f_block_arg {
                    $$ = new ArgsNode(getPosition(), $1.intValue(), null, -1, $2);
                }
              | f_optarg ',' f_rest_arg opt_f_block_arg {
                    $$ = new ArgsNode(getPosition(), 0, $1, $3.intValue(), $4);
                }
              | f_optarg opt_f_block_arg {
                    $$ = new ArgsNode(getPosition(), 0, $1, -1, $2);
                }
              | f_rest_arg opt_f_block_arg {
                    $$ = new ArgsNode(getPosition(), 0, null, $1.intValue(), $2);
                }
              | f_block_arg {
                    $$ = new ArgsNode(getPosition(), 0, null, -1, $1);
                }
              | /* none */ {
                    $$ = new ArgsNode(getPosition(), 0, null, -1, null);
                }

f_norm_arg    : tCONSTANT {
                    yyerror("formal argument cannot be a constant");
                }
              | tIVAR {
                    yyerror("formal argument cannot be an instance variable");
                }
              | global_var {
                    yyerror("formal argument cannot be a global variable");
                }
              | tCVAR {
                    yyerror("formal argument cannot be a class variable");
                }
              | tIDENTIFIER {
                    if (!IdUtil.isLocal($1)) {
                        yyerror("formal argument must be local variable");
                    } else if (support.getLocalNames().isLocalRegistered($1)) {
                        yyerror("duplicate argument name");
                    }
                    support.getLocalNames().getLocalIndex($1);
                    $$ = new Integer(1);
                }

f_arg         : f_norm_arg
              | f_arg ',' f_norm_arg {
                    $$ = new Integer($<Integer>$.intValue() + 1);
                }

f_opt         : tIDENTIFIER '=' arg {
                    if (!IdUtil.isLocal($1)) {
                        yyerror("formal argument must be local variable");
                    } else if (support.getLocalNames().isLocalRegistered($1)) {
                        yyerror("duplicate optional argument name");
                    }
		    support.getLocalNames().getLocalIndex($1);
                    $$ = support.getAssignmentNode($1, $3, getPosition());
                }

f_optarg      : f_opt {
                    $$ = new ArrayNode(getPosition()).add($1);
                }
              | f_optarg ',' f_opt {
                    $$ = $1.add($3);
                }

f_rest_arg    : tSTAR tIDENTIFIER {
                    if (!IdUtil.isLocal($2)) {
                        yyerror("rest argument must be local variable");
                    } else if (support.getLocalNames().isLocalRegistered($2)) {
                        yyerror("duplicate rest argument name");
                    }
                    $$ = new Integer(support.getLocalNames().getLocalIndex($2));
                }
              | tSTAR {
                    $$ = new Integer(-2);
                }

f_block_arg   : tAMPER tIDENTIFIER {
                    if (!IdUtil.isLocal($2)) {
                        yyerror("block argument must be local variable");
                    } else if (support.getLocalNames().isLocalRegistered($2)) {
                        yyerror("duplicate block argument name");
                    }
                    $$ = new BlockArgNode(getPosition(), support.getLocalNames().getLocalIndex($2));
                }

opt_f_block_arg: ',' f_block_arg {
                    $$ = $2;
                }
              | /* none */ {
	            $$ = null;
	        }

singleton     : var_ref {
                    /*if ($1 instanceof SelfNode()) {
                        $$ = new SelfNode(null);
                    } else {*/
                        $$ = $1;
                    //}
                }
              | '(' {
                    lexer.setState(LexState.EXPR_BEG);
                } expr opt_nl ')' {
                    if ($3 instanceof ILiteralNode) {
                        /*case Constants.NODE_STR:
                        case Constants.NODE_DSTR:
                        case Constants.NODE_XSTR:
                        case Constants.NODE_DXSTR:
                        case Constants.NODE_DREGX:
                        case Constants.NODE_LIT:
                        case Constants.NODE_ARRAY:
                        case Constants.NODE_ZARRAY:*/
                        yyerror("Can't define single method for literals.");
                    }
                    $$ = $3;
                }

assoc_list    : none
              | assocs trailer {
                    $$ = $1;
                }
              | args trailer {
                    if (ListNodeUtil.getLength($1) % 2 != 0) {
                        yyerror("Odd number list for Hash.");
                    }
                    $$ = $1;
                }

assocs        : assoc
              | assocs ',' assoc {
                    $$ = ListNodeUtil.addAll($1, $3);
                }

assoc         : arg tASSOC arg {
                    $$ = new ArrayNode(getPosition()).add($1).add($3);
                }

operation     : tIDENTIFIER
              | tCONSTANT
              | tFID

operation2    : tIDENTIFIER
              | tCONSTANT
              | tFID
              | op

operation3    : tIDENTIFIER
              | tFID
              | op

dot_or_colon  : '.'
              | tCOLON2

opt_terms     : /* none */
              | terms

opt_nl        : /* none */
              | '\n'

trailer       : /* none */
              | '\n'
              | ','

term          : ';' {
                    yyerrok();
                }
              | '\n'

terms         : term
              | terms ';' {
                    yyerrok();
                }

none          : /* none */ {
                    $$ = null;
                }
%%

    /** The parse method use an lexer stream and parse it to an AST node 
     * structure
     */
    public IParserResult parse(ILexerSource source) {
        support.reset();
        support.setResult(new RubyParserResult());

	lexer.setSource(source);
	try {
            yyparse(lexer, null);
	} catch (Exception excptn) {
            excptn.printStackTrace();
	}

        return support.getResult();
    }

    public void init(IConfiguration configuration) {
        support.setConfiguration((IRubyParserConfiguration)configuration);
    }

    // +++
    // Helper Methods
    
    void yyerrok() {}

    private ISourcePosition getPosition() {
        return lexer.getPosition();
    }
}
