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

import java.util.*;
import java.io.*;

import org.jruby.*;
import org.jruby.nodes.*;
import org.jruby.runtime.*;
import org.jruby.util.*;

public class DefaultRubyParser implements RubyParser {
    private Ruby ruby;
    private ParserHelper ph;
    private NodeFactory nf;
    private DefaultRubyScanner rs;
    
    public DefaultRubyParser(Ruby ruby) {
        this.ruby = ruby;
        this.ph = ruby.getParserHelper();
        this.nf = new NodeFactory(ruby);
        this.rs = new DefaultRubyScanner(ruby);
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

%token <RubyId>    tIDENTIFIER tFID tGVAR tIVAR tCONSTANT tCVAR
%token <RubyObject> tINTEGER tFLOAT tSTRING tXSTRING tREGEXP
%token <Node>  tDSTRING tDXSTRING tDREGEXP tNTH_REF tBACK_REF

%type <Node>  singleton string
%type <RubyObject> literal numeric
%type <Node>  compstmt stmts stmt expr arg primary command command_call method_call
%type <Node>  if_tail opt_else case_body cases rescue exc_list exc_var ensure
%type <Node>  args ret_args when_args call_args paren_args opt_paren_args
%type <Node>  command_args aref_args opt_block_arg block_arg var_ref
%type <Node>  mrhs mrhs_basic superclass block_call block_command
%type <Node>  f_arglist f_args f_optarg f_opt f_block_arg opt_f_block_arg
%type <Node>  assoc_list assocs assoc undef_list backref
%type <Node>  block_var opt_block_var brace_block do_block lhs none
%type <Node>  mlhs mlhs_head mlhs_basic mlhs_entry mlhs_item mlhs_node
%type <RubyId>    fitem variable sym symbol operation operation2 operation3
%type <RubyId>    cname fname op f_rest_arg
%type <Integer>   f_norm_arg f_arg
%token <RubyId> tUPLUS 	/* unary+ */
%token <RubyId> tUMINUS 	/* unary- */
%token <RubyId> tPOW		/* ** */
%token <RubyId> tCMP  		/* <=> */
%token <RubyId> tEQ  		/* == */
%token <RubyId> tEQQ  		/* === */
%token <RubyId> tNEQ  		/* != */
%token <RubyId> tGEQ  		/* >= */
%token <RubyId> tLEQ  		/* <= */
%token <RubyId> tANDOP tOROP	/* && and || */
%token <RubyId> tMATCH tNMATCH	/* =~ and !~ */
%token <RubyId> tDOT2 tDOT3	/* .. and ... */
%token <RubyId> tAREF tASET	/* [] and []= */
%token <RubyId> tLSHFT tRSHFT	/* << and >> */
%token <RubyId> tCOLON2	/* :: */
%token <RubyId> tCOLON3	/* :: at EXPR_BEG */
%token <RubyId> tOP_ASGN	/* +=, -=  etc. */
%token <RubyId> tASSOC		/* => */
%token <RubyId> tLPAREN	/* ( */
%token <RubyId> tLBRACK	/* [ */
%token <RubyId> tLBRACE	/* { */
%token <RubyId> tSTAR		/* * */
%token <RubyId> tAMPER		/* & */
%token <RubyId> tSYMBEG

/*
 *	precedence table
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
program		:
            {
                $<Object>$ = ruby.getDynamicVars();
			    ph.setLexState(LexState.EXPR_BEG);
                ph.top_local_init();
			    if (ruby.getRubyClass() == ruby.getClasses().getObjectClass())
                    ph.setClassNest(0);
			    else
                    ph.setClassNest(1);
            }
compstmt
            {
                if ($2 != null && !ph.isCompileForEval()) {
                    /* last expression should not be void */
			        if ($2.getType() != Constants.NODE_BLOCK)
                        ph.void_expr($2);
			        else {
                        Node node = $2;
				        while (node.getNextNode() != null) {
				            node = node.getNextNode();
				        }
				        ph.void_expr(node.getHeadNode());
			        }
			    }
			    ph.setEvalTree(ph.block_append(ph.getEvalTree(), $2));
                ph.top_local_setup();
			    ph.setClassNest(0);
		        ruby.setDynamicVars($<RubyVarmap>1);
		    }

compstmt	: stmts opt_terms
		    {
			    ph.void_stmts($1);
			    $$ = $1;
		    }

stmts		: none
		| stmt
		    {
			    $$ = ph.newline_node($1);
		    }
		| stmts terms stmt
		    {
			    $$ = ph.block_append($1, ph.newline_node($3));
		    }
		| error stmt
		    {
			    $$ = $2;
		    }

stmt		: kALIAS fitem {ph.setLexState(LexState.EXPR_FNAME);} fitem
		    {
			    if (ph.isInDef() || ph.isInSingle())
			        yyerror("alias within method");
		        $$ = nf.newAlias($2, $4);
		    }
		| kALIAS tGVAR tGVAR
		    {
			    if (ph.isInDef() || ph.isInSingle())
			        yyerror("alias within method");
		        $$ = nf.newVAlias($2, $3);
		    }
		| kALIAS tGVAR tBACK_REF
		    {
			    if (ph.isInDef() || ph.isInSingle())
			        yyerror("alias within method");
			    String buf = "$" + (char)$3.getNth();
		        $$ = nf.newVAlias($2, ruby.intern(buf));
		    }
		| kALIAS tGVAR tNTH_REF
		    {
		        yyerror("can't make alias for the number variables");
		        $$ = null; //XXX 0
		    }
		| kUNDEF undef_list
		    {
			    if (ph.isInDef() || ph.isInSingle())
			        yyerror("undef within method");
			    $$ = $2;
		    }
		| stmt kIF_MOD expr
		    {
			    ph.value_expr($3);
			    $$ = nf.newIf(ph.cond($3), $1, null);
		        ph.fixpos($$, $3);
		    }
		| stmt kUNLESS_MOD expr
		    {
			    ph.value_expr($3);
			    $$ = nf.newUnless(ph.cond($3), $1, null);
		        ph.fixpos($$, $3);
		    }
		| stmt kWHILE_MOD expr
		    {
			    ph.value_expr($3);
			    if ($1 != null && $1 instanceof BeginNode) {
			        $$ = nf.newWhile(ph.cond($3), $1.getBodyNode()); // , 0
			    } else {
			        $$ = nf.newWhile(ph.cond($3), $1); // , 1
			    }
		    }
		| stmt kUNTIL_MOD expr
		    {
			    ph.value_expr($3);
			    if ($1 != null && $1 instanceof BeginNode) {
			        $$ = nf.newUntil(ph.cond($3), $1.getBodyNode()); // , 0
			    } else {
			        $$ = nf.newUntil(ph.cond($3), $1); // , 1
			    }
		    }
		| stmt kRESCUE_MOD stmt
		    {
			    $$ = nf.newRescue($1, nf.newResBody(null,$3,null), null);
		    }
		| klBEGIN
		    {
			    if (ph.isInDef() || ph.isInSingle()) {
			        yyerror("BEGIN in method");
			    }
			    ph.local_push();
		    }
		  '{' compstmt '}'
		    {
			    ph.setEvalTreeBegin(ph.block_append(ph.getEvalTree(), nf.newPreExe($4)));
		        ph.local_pop();
		        $$ = null; //XXX 0;
		    }
		| klEND '{' compstmt '}'
		    {
			    if (ph.isCompileForEval() && (ph.isInDef() || ph.isInSingle())) {
			        yyerror("END in method; use at_exit");
			    }

			    $$ = nf.newIter(null, nf.newPostExe(), $3);
		    }
		| lhs '=' command_call
		    {
			    ph.value_expr($3);
			    $$ = ph.node_assign($1, $3);
		    }
		| mlhs '=' command_call
		    {
			    ph.value_expr($3);
			    $1.setValueNode($3);
			    $$ = $1;
		    }
		| lhs '=' mrhs_basic
		    {
			    $$ = ph.node_assign($1, $3);
		    }
		| expr

expr	: mlhs '=' mrhs
		    {
			    ph.value_expr($3);
			    $1.setValueNode($3);
			    $$ = $1;
		    }
		| kRETURN call_args
		    {
			    if (!ph.isCompileForEval() && !ph.isInDef() && !ph.isInSingle())
			        yyerror("return appeared outside of method");
			    $$ = nf.newReturn(ph.ret_args($2));
		    }
		| command_call
		| expr kAND expr
		    {
			    $$ = ph.logop(Constants.NODE_AND, $1, $3);
		    }
		| expr kOR expr
		    {
			    $$ = ph.logop(Constants.NODE_OR, $1, $3);
		    }
		| kNOT expr
		    {
			    ph.value_expr($2);
			    $$ = nf.newNot(ph.cond($2));
		    }
		| '!' command_call
		    {
			    $$ = nf.newNot(ph.cond($2));
		    }
		| arg

command_call	: command
		| block_command

block_command	: block_call
		| block_call '.' operation2 command_args
		    {
			    ph.value_expr($1);
			    $$ = ph.new_call($1, $3, $4);
		    }
		| block_call tCOLON2 operation2 command_args
		    {
			    ph.value_expr($1);
			    $$ = ph.new_call($1, $3, $4);
		    }

command		: operation command_args
		    {
			    $$ = ph.new_fcall($1, $2);
		        ph.fixpos($$, $2);
		    }
		| primary '.' operation2 command_args
		    {
			    ph.value_expr($1);
			    $$ = ph.new_call($1, $3, $4);
		        ph.fixpos($$, $1);
		    }
		| primary tCOLON2 operation2 command_args
		    {
			    ph.value_expr($1);
			    $$ = ph.new_call($1, $3, $4);
		        ph.fixpos($$, $1);
		    }
		| kSUPER command_args
		    {
			    if (!ph.isCompileForEval() && ph.isInDef() && ph.isInSingle())
			        yyerror("super called outside of method");
			    $$ = ph.new_super($2);
		        ph.fixpos($$, $2);
		    }
		| kYIELD call_args
		    {
			    $$ = nf.newYield(ph.ret_args($2));
		        ph.fixpos($$, $2);
		    }

mlhs		: mlhs_basic
		| tLPAREN mlhs_entry ')'
		    {
			    $$ = $2;
		    }

mlhs_entry	: mlhs_basic
		| tLPAREN mlhs_entry ')'
		    {
			    $$ = nf.newMAsgn(nf.newList($2), null);
		    }

mlhs_basic	: mlhs_head
		    {
			    $$ = nf.newMAsgn($1, null);
		    }
		| mlhs_head mlhs_item
		    {
			    $$ = nf.newMAsgn(ph.list_append($1,$2), null);
		    }
		| mlhs_head tSTAR mlhs_node
		    {
			    $$ = nf.newMAsgn($1, $3);
		    }
		| mlhs_head tSTAR
		    {
			    $$ = nf.newMAsgn($1, Node.MINUS_ONE);
		    }
		| tSTAR mlhs_node
		    {
			    $$ = nf.newMAsgn(null, $2);
		    }
		| tSTAR
		    {
			    $$ = nf.newMAsgn(null, Node.MINUS_ONE);
		    }

mlhs_item	: mlhs_node
		| tLPAREN mlhs_entry ')'
		    {
			    $$ = $2;
		    }

mlhs_head	: mlhs_item ','
		    {
			    $$ = nf.newList($1);
		    }
		| mlhs_head mlhs_item ','
		    {
			    $$ = ph.list_append($1, $2);
		    }

mlhs_node	: variable
		    {
			    $$ = ph.assignable($1, null);
		    }
		| primary '[' aref_args ']'
		    {
			    $$ = ph.aryset($1, $3);
		    }
		| primary '.' tIDENTIFIER
		    {
			    $$ = ph.attrset($1, $3);
		    }
		| primary tCOLON2 tIDENTIFIER
		    {
			    $$ = ph.attrset($1, $3);
		    }
		| primary '.' tCONSTANT
		    {
			    $$ = ph.attrset($1, $3);
		    }
		| backref
		    {
		        ph.rb_backref_error($1);
			    $$ = null; //XXX 0;
		    }

lhs		: variable
		    {
			    $$ = ph.assignable($1, null);
		    }
		| primary '[' aref_args ']'
		    {
			    $$ = ph.aryset($1, $3);
		    }
		| primary '.' tIDENTIFIER
		    {
			    $$ = ph.attrset($1, $3);
		    }
		| primary tCOLON2 tIDENTIFIER
		    {
			    $$ = ph.attrset($1, $3);
		    }
		| primary '.' tCONSTANT
		    {
			    $$ = ph.attrset($1, $3);
		    }
		| backref
		    {
		        ph.rb_backref_error($1);
			    $$ = null; //XXX 0;
		    }

cname		: tIDENTIFIER
		    {
			    yyerror("class/module name must be CONSTANT");
		    }
		| tCONSTANT

fname		: tIDENTIFIER
		| tCONSTANT
		| tFID
		| op
		    {
			    ph.setLexState(LexState.EXPR_END);
                $$ = $1;
		    }
		| reswords
		    {
			    ph.setLexState(LexState.EXPR_END);
			    $$ = $<>1;
		    }

fitem		: fname
		| symbol

undef_list	: fitem
		    {
			    $$ = nf.newUndef($1);
		    }
		| undef_list ',' {ph.setLexState(LexState.EXPR_FNAME);} fitem
		    {
			    $$ = ph.block_append($1, nf.newUndef($4));
		    }

op		: '|'		{ $$ = RubyId.newId(ruby, '|'); }
		| '^'		{ $$ = RubyId.newId(ruby, '^'); }
		| '&'		{ $$ = RubyId.newId(ruby, '&'); }
		| tCMP		{ $$ = RubyId.newId(ruby, tCMP); }
		| tEQ		{ $$ = RubyId.newId(ruby, tEQ); }
		| tEQQ		{ $$ = RubyId.newId(ruby, tEQQ); }
		| tMATCH	{ $$ = RubyId.newId(ruby, tMATCH); }
		| '>'		{ $$ = RubyId.newId(ruby, '>'); }
		| tGEQ		{ $$ = RubyId.newId(ruby, tGEQ); }
		| '<'		{ $$ = RubyId.newId(ruby, '<'); }
		| tLEQ		{ $$ = RubyId.newId(ruby, tLEQ); }
		| tLSHFT	{ $$ = RubyId.newId(ruby, tLSHFT); }
		| tRSHFT	{ $$ = RubyId.newId(ruby, tRSHFT); }
		| '+'		{ $$ = RubyId.newId(ruby, '+'); }
		| '-'		{ $$ = RubyId.newId(ruby, '-'); }
		| '*'		{ $$ = RubyId.newId(ruby, '*'); }
		| tSTAR		{ $$ = RubyId.newId(ruby, '*'); }
		| '/'		{ $$ = RubyId.newId(ruby, '/'); }
		| '%'		{ $$ = RubyId.newId(ruby, '%'); }
		| tPOW		{ $$ = RubyId.newId(ruby, tPOW); }
		| '~'		{ $$ = RubyId.newId(ruby, '~'); }
		| tUPLUS	{ $$ = RubyId.newId(ruby, tUPLUS); }
		| tUMINUS	{ $$ = RubyId.newId(ruby, tUMINUS); }
		| tAREF		{ $$ = RubyId.newId(ruby, tAREF); }
		| tASET		{ $$ = RubyId.newId(ruby, tASET); }
		| '`'		{ $$ = RubyId.newId(ruby, '`'); }

reswords	: k__LINE__ | k__FILE__  | klBEGIN | klEND
		| kALIAS | kAND | kBEGIN | kBREAK | kCASE | kCLASS | kDEF
		| kDEFINED | kDO | kELSE | kELSIF | kEND | kENSURE | kFALSE
		| kFOR | kIF_MOD | kIN | kMODULE | kNEXT | kNIL | kNOT
		| kOR | kREDO | kRESCUE | kRETRY | kRETURN | kSELF | kSUPER
		| kTHEN | kTRUE | kUNDEF | kUNLESS_MOD | kUNTIL_MOD | kWHEN
		| kWHILE_MOD | kYIELD | kRESCUE_MOD

arg		: lhs '=' arg
		    {
			    ph.value_expr($3);
			    $$ = ph.node_assign($1, $3);
		    }
		| variable tOP_ASGN {$$ = ph.assignable($1, null);} arg
		    {
			    if ($2.intValue() == tOROP) {
			        $3.setValueNode($4);
			        $$ = nf.newOpAsgnOr(ph.gettable($1), $<Node>3);
			        if ($1.isInstanceId()) {
				        $<Node>$.setAId($1);
			        }
			    } else if ($2.intValue() == tANDOP) {
			        $3.setValueNode($4);
			        $$ = nf.newOpAsgnAnd(ph.gettable($1), $<Node>3);
			    } else {
			        $$ = $3;
			        if ($$ != null) {
				        $<Node>$.setValueNode(ph.call_op(ph.gettable($1),$2.intValue(),1,$4));
			        }
			    }
			    ph.fixpos($$, $4);
		    }
		| primary '[' aref_args ']' tOP_ASGN arg
		    {
			    ArrayNode args = nf.newList($6);

			    ph.list_append($3, nf.newNil());
			    ph.list_concat(args, $3);
                if ($5.intValue() == Token.tOROP) {
			        $<>5 = RubyId.newId(ruby, 0);
			    } else if ($5.intValue() == Token.tANDOP) {
			        $<>5 = RubyId.newId(ruby, 1);
			    }
			    $$ = nf.newOpAsgn1($1, $5, args);
		        ph.fixpos($$, $1);
		    }
		| primary '.' tIDENTIFIER tOP_ASGN arg
		    {
                if ($4.intValue() == Token.tOROP) {
			        $<>4 = RubyId.newId(ruby, 0);
			    } else if ($4.intValue() == Token.tANDOP) {
			        $<>4 = RubyId.newId(ruby, 1);
			    }
			    $$ = nf.newOpAsgn2($1, $3, $4, $5);
		        ph.fixpos($$, $1);
		    }
		| primary '.' tCONSTANT tOP_ASGN arg
		    {
                if ($4.intValue() == Token.tOROP) {
			        $<>4 = RubyId.newId(ruby, 0);
			    } else if ($4.intValue() == Token.tANDOP) {
			        $<>4 = RubyId.newId(ruby, 1);
			    }
			    $$ = nf.newOpAsgn2($1, $3, $4, $5);
		        ph.fixpos($$, $1);
		    }
		| primary tCOLON2 tIDENTIFIER tOP_ASGN arg
		    {
			    if ($4.intValue() == Token.tOROP) {
			        $<>4 = RubyId.newId(ruby, 0);
			    } else if ($4.intValue() == Token.tANDOP) {
			        $<>4 = RubyId.newId(ruby, 1);
			    }
			    $$ = nf.newOpAsgn2($1, $3, $4, $5);
		        ph.fixpos($$, $1);
		    }
		| backref tOP_ASGN arg
		    {
		        ph.rb_backref_error($1);
			    $$ = null; //XXX 0
		    }
		| arg tDOT2 arg
		    {
			    $$ = nf.newDot2($1, $3);
		    }
		| arg tDOT3 arg
		    {
			    $$ = nf.newDot3($1, $3);
		    }
		| arg '+' arg
		    {
			    $$ = ph.call_op($1, '+', 1, $3);
		    }
		| arg '-' arg
		    {
		        $$ = ph.call_op($1, '-', 1, $3);
		    }
		| arg '*' arg
		    {
		        $$ = ph.call_op($1, '*', 1, $3);
		    }
		| arg '/' arg
		    {
			    $$ = ph.call_op($1, '/', 1, $3);
		    }
		| arg '%' arg
		    {
			    $$ = ph.call_op($1, '%', 1, $3);
		    }
		| arg tPOW arg
		    {
			    boolean need_negate = false;

			    if ($1 instanceof LitNode) {
                    if ($1.getLiteral() instanceof RubyFixnum || 
                        $1.getLiteral() instanceof RubyFloat ||
                        $1.getLiteral() instanceof RubyBignum) {
                        if ($1.getLiteral().funcall(ruby.intern("<"), RubyFixnum.zero(ruby)).isTrue()) {
                            $1.setLiteral($1.getLiteral().funcall(ruby.intern("-@")));
                            need_negate = true;
                        }
                    }
			    }
			    $$ = ph.call_op($1, tPOW, 1, $3);
			    if (need_negate) {
			        $$ = ph.call_op($<Node>$, tUMINUS, 0, null);
			    }
		    }
		| tUPLUS arg
		    {
			    if ($2 != null && $2 instanceof LitNode) {
			        $$ = $2;
			    } else {
			        $$ = ph.call_op($2, tUPLUS, 0, null);
			    }
		    }
		| tUMINUS arg
		    {
			    if ($2 != null && $2 instanceof LitNode && $2.getLiteral() instanceof RubyFixnum) {
			        long i = ((RubyFixnum)$2.getLiteral()).getValue();

			        $2.setLiteral(RubyFixnum.m_newFixnum(ruby, -i));
			        $$ = $2;
			    } else {
			        $$ = ph.call_op($2, tUMINUS, 0, null);
			    }
		    }
		| arg '|' arg
		    {
		        $$ = ph.call_op($1, '|', 1, $3);
		    }
		| arg '^' arg
		    {
			    $$ = ph.call_op($1, '^', 1, $3);
		    }
		| arg '&' arg
		    {
			    $$ = ph.call_op($1, '&', 1, $3);
		    }
		| arg tCMP arg
		    {
			    $$ = ph.call_op($1, tCMP, 1, $3);
		    }
		| arg '>' arg
		    {
			    $$ = ph.call_op($1, '>', 1, $3);
		    }
		| arg tGEQ arg
		    {
			    $$ = ph.call_op($1, tGEQ, 1, $3);
		    }
		| arg '<' arg
		    {
			    $$ = ph.call_op($1, '<', 1, $3);
		    }
		| arg tLEQ arg
		    {
			    $$ = ph.call_op($1, tLEQ, 1, $3);
		    }
		| arg tEQ arg
		    {
			    $$ = ph.call_op($1, tEQ, 1, $3);
		    }
		| arg tEQQ arg
		    {
			    $$ = ph.call_op($1, tEQQ, 1, $3);
		    }
		| arg tNEQ arg
		    {
			    $$ = nf.newNot(ph.call_op($1, tEQ, 1, $3));
		    }
		| arg tMATCH arg
		    {
			    $$ = ph.match_gen($1, $3);
		    }
		| arg tNMATCH arg
		    {
			    $$ = nf.newNot(ph.match_gen($1, $3));
		    }
		| '!' arg
		    {
			    ph.value_expr($2);
			    $$ = nf.newNot(ph.cond($2));
		    }
		| '~' arg
		    {
			    $$ = ph.call_op($2, '~', 0, null);
		    }
		| arg tLSHFT arg
		    {
			    $$ = ph.call_op($1, tLSHFT, 1, $3);
		    }
		| arg tRSHFT arg
		    {
			    $$ = ph.call_op($1, tRSHFT, 1, $3);
		    }
		| arg tANDOP arg
		    {
			    $$ = ph.logop(Constants.NODE_AND, $1, $3);
		    }
		| arg tOROP arg
		    {
			    $$ = ph.logop(Constants.NODE_OR, $1, $3);
		    }
		| kDEFINED opt_nl { ph.setInDefined(true);} arg
		    {
		        ph.setInDefined(false);
			    $$ = nf.newDefined($4);
		    }
		| arg '?' arg ':' arg
		    {
			    ph.value_expr($1);
			    $$ = nf.newIf(ph.cond($1), $3, $5);
		        ph.fixpos($$, $1);
		    }
		| primary
		    {
			    $$ = $1;
		    }

aref_args	: none
		| command_call opt_nl
		    {
			$$ = nf.newList($1);
		    }
		| args ',' command_call opt_nl
		    {
			$$ = ph.list_append($1, $3);
            }
		| args trailer
		    {
			    $$ = $1;
		    }
		| args ',' tSTAR arg opt_nl
		    {
			    ph.value_expr($4);
			    $$ = ph.arg_concat($1, $4);
		    }
		| assocs trailer
		    {
			    $$ = nf.newList(nf.newHash($1));
		    }
		| tSTAR arg opt_nl
		    {
			    ph.value_expr($2);
			    $$ = nf.newRestArgs($2);
		    }

paren_args	: '(' none ')'
		    {
			    $$ = $2;
		    }
		| '(' call_args opt_nl ')'
		    {
			    $$ = $2;
		    }
		| '(' block_call opt_nl ')'
		    {
			    $$ = nf.newList($2);
		    }
		| '(' args ',' block_call opt_nl ')'
		    {
			    $$ = ph.list_append($2, $4);
		    }

opt_paren_args	: none
		| paren_args

call_args	: command
		    {
			    $$ = nf.newList($1);
		    }
		| args ',' command
		    {
			$$ = ph.list_append($1, $3);
		    }
		| args opt_block_arg
		    {
			    $$ = ph.arg_blk_pass($<Node>1, $2);
		    }
		| args ',' tSTAR arg opt_block_arg
		    {
			    ph.value_expr($4);
			    $$ = ph.arg_concat($1, $4);
			    $$ = ph.arg_blk_pass($<Node>$, $5);
		    }
		| assocs opt_block_arg
		    {
			    $$ = nf.newList(nf.newHash($1));
			    $$ = ph.arg_blk_pass($<Node>$, $2);
		    }
		| assocs ',' tSTAR arg opt_block_arg
		    {
			    ph.value_expr($4);
			    $$ = ph.arg_concat(nf.newList(nf.newHash($1)), $4);
			    $$ = ph.arg_blk_pass($<Node>$, $5);
		    }
		| args ',' assocs opt_block_arg
		    {
			    $$ = ph.list_append($1, nf.newHash($3));
			    $$ = ph.arg_blk_pass($<Node>$, $4);
		    }
		| args ',' assocs ',' tSTAR arg opt_block_arg
		    {
			    ph.value_expr($6);
			    $$ = ph.arg_concat(ph.list_append($1, nf.newHash($3)), $6);
			    $$ = ph.arg_blk_pass($<Node>$, $7);
		    }
		| tSTAR arg opt_block_arg
		    {
			    ph.value_expr($2);
			    $$ = ph.arg_blk_pass(nf.newRestArgs($2), $3);
		    }
		| block_arg

command_args	:  { rs.CMDARG_PUSH(); } call_args
		    {
			    rs.CMDARG_POP();
		        $$ = $2;
		    }

block_arg	: tAMPER arg
		    {
			    ph.value_expr($2);
			    $$ = nf.newBlockPass($2);
		    }

opt_block_arg	: ',' block_arg
		    {
			    $$ = $2;
		    }
		| none

args 		: arg
		    {
			    ph.value_expr($1);
			    $$ = nf.newList($1);
		    }
		| args ',' arg
		    {
			    ph.value_expr($3);
			    $$ = ph.list_append($1, $3);
		    }

mrhs		: arg
		    {
			    ph.value_expr($1);
			    $$ = $1;
		    }
		| mrhs_basic

mrhs_basic	: args ',' arg
		    {
			    ph.value_expr($3);
			    $$ = ph.list_append($1, $3);
		    }
		| args ',' tSTAR arg
		    {
			    ph.value_expr($4);
			    $$ = ph.arg_concat($1, $4);
		    }
		| tSTAR arg
		    {
			    ph.value_expr($2);
			    $$ = $2;
		    }

ret_args	: call_args
		    {
			    $$ = $1;
			    if ($1 != null) {
			        if ($1.getType() == Constants.NODE_ARRAY && $1.getNextNode() == null) {
				        $$ = $1.getHeadNode();
    			    } else if ($1.getType() == Constants.NODE_BLOCK_PASS) {
	    			    ph.rb_compile_error("block argument should not be given");
		    	    }
			    }
		    }

primary		: literal
		    {
			    $$ = nf.newLit($1);
		    }
		| string
		| tXSTRING
		    {
			    $$ = nf.newXStr($1);
		    }
		| tDXSTRING
		| tDREGEXP
		| var_ref
		| backref
		| tFID
		    {
			    $$ = nf.newVCall($1);
		    }
		| kBEGIN
		  compstmt
		  rescue
		  opt_else
		  ensure
		  kEND
		    {
			    if ($3 == null && $4 == null && $5 == null)
			        $$ = nf.newBegin($2);
			    else {
			        if ($3 != null) $<>2 = nf.newRescue($2, $3, $4);
			        else if ($4 != null) {
				        ph.rb_warn("else without rescue is useless");
				        $<>2 = ph.block_append($2, $4);
			        }
			        if ($5 != null) $<>2 = nf.newEnsure($2, $5);
			        $$ = $2;
			    }
		        ph.fixpos($$, $2);
		    }
		| tLPAREN compstmt ')'
		    {
			    $$ = $2;
		    }
		| primary tCOLON2 tCONSTANT
		    {
			    ph.value_expr($1);
			    $$ = nf.newColon2($1, $3);
		    }
		| tCOLON3 cname
		    {
			    $$ = nf.newColon3($2);
		    }
		| primary '[' aref_args ']'
		    {
			    ph.value_expr($1);
			    $$ = nf.newCall($1, ph.newId(tAREF), $3);
		    }
		| tLBRACK aref_args ']'
		    {
		        if ($2 == null) {
			        $$ = nf.newZArray(); /* zero length array*/
			    } else {
			        $$ = $2;
			    }
		    }
		| tLBRACE assoc_list '}'
		    {
			    $$ = nf.newHash($2);
		    }
		| kRETURN '(' ret_args ')'
		    {
			    if (!ph.isCompileForEval() && !ph.isInDef() && !ph.isInSingle())
			        yyerror("return appeared outside of method");
			    ph.value_expr($3);
			    $$ = nf.newReturn($3);
		    }
		| kRETURN '(' ')'
		    {
			    if (!ph.isCompileForEval() && !ph.isInDef() && !ph.isInSingle())
			        yyerror("return appeared outside of method");
			    $$ = nf.newReturn(null);
		    }
		| kRETURN
		    {
			    if (!ph.isCompileForEval() && !ph.isInDef() && !ph.isInSingle())
			        yyerror("return appeared outside of method");
			    $$ = nf.newReturn(null);
		    }
		| kYIELD '(' ret_args ')'
		    {
			    ph.value_expr($3);
			    $$ = nf.newYield($3);
		    }
		| kYIELD '(' ')'
		    {
			    $$ = nf.newYield(null);
		    }
		| kYIELD
		    {
			    $$ = nf.newYield(null);
		    }
		| kDEFINED opt_nl '(' {ph.setInDefined(true);} expr ')'
		    {
		        ph.setInDefined(false);
			    $$ = nf.newDefined($5);
		    }
		| operation brace_block
		    {
			    $2.setIterNode(nf.newFCall($1, null));
			    $$ = $2;
		    }
		| method_call
		| method_call brace_block
		    {
			    if ($1 != null && $1.getType() == Constants.NODE_BLOCK_PASS) {
			        ph.rb_compile_error("both block arg and actual block given");
			    }
			    $2.setIterNode($1);
			    $$ = $2;
		        ph.fixpos($$, $1);
		    }
		| kIF expr then
		  compstmt
		  if_tail
		  kEND
		    {
			    ph.value_expr($2);
			    $$ = nf.newIf(ph.cond($2), $4, $5);
		        ph.fixpos($$, $2);
		    }
		| kUNLESS expr then
		  compstmt
		  opt_else
		  kEND
		    {
			    ph.value_expr($2);
			    $$ = nf.newUnless(ph.cond($2), $4, $5);
		        ph.fixpos($$, $2);
		    }
		| kWHILE { rs.COND_PUSH(); } expr do { rs.COND_POP(); }
		  compstmt
		  kEND
		    {
			    ph.value_expr($3);
			    $$ = nf.newWhile(ph.cond($3), $6); // , 1
		        ph.fixpos($$, $3);
		    }
		| kUNTIL { rs.COND_PUSH(); } expr do { rs.COND_POP(); } 
		  compstmt
		  kEND
		    {
			    ph.value_expr($3);
			    $$ = nf.newUntil(ph.cond($3), $6); //, 1
		        ph.fixpos($$, $3);
		    }
		| kCASE expr opt_terms
		  case_body
		  kEND
		    {
			    ph.value_expr($2);
			    $$ = nf.newCase($2, $4);
		        ph.fixpos($$, $2);
		    }
		| kCASE opt_terms case_body kEND
		    {
			    $$ = $3;
		    }
		| kFOR block_var kIN { rs.COND_PUSH(); } expr do { rs.COND_POP(); }
		  compstmt
		  kEND
		    {
			    ph.value_expr($5);
			    $$ = nf.newFor($2, $5, $8);
		        ph.fixpos($$, $2);
		    }
		| kCLASS cname superclass
		    {
			    if (ph.isInDef() || ph.isInSingle())
			        yyerror("class definition in method body");
			    ph.setClassNest(ph.getClassNest() + 1);
			    ph.local_push();
		        $$ = new Integer(ruby.getSourceLine());
		    }
		  compstmt
		  kEND
		    {
		        $$ = nf.newClass($2, $5, $3);
		        $<Node>$.setLine($<Integer>4.intValue());
		        ph.local_pop();
			    ph.setClassNest(ph.getClassNest() - 1);
		    }
		| kCLASS tLSHFT expr
		    {
			    $$ = new Integer(ph.getInDef());
		        ph.setInDef(0);
		    }
		  term
		    {
		        $$ = new Integer(ph.getInSingle());
		        ph.setInSingle(0);
			    ph.setClassNest(ph.getClassNest() - 1);
			    ph.local_push();
		    }
		  compstmt
		  kEND
		    {
		        $$ = nf.newSClass($3, $7);
		        ph.fixpos($$, $3);
		        ph.local_pop();
			    ph.setClassNest(ph.getClassNest() - 1);
		        ph.setInDef($<Integer>4.intValue());
		        ph.setInSingle($<Integer>6.intValue());
		    }
		| kMODULE cname
		    {
			    if (ph.isInDef() || ph.isInSingle())
			        yyerror("module definition in method body");
			    ph.setClassNest(ph.getClassNest() + 1);
			    ph.local_push();
		        $$ = new Integer(ruby.getSourceLine());
		    }
		  compstmt
		  kEND
		    {
		        $$ = nf.newModule($2, $4);
		        $<Node>$.setLine($<Integer>3.intValue());
		        ph.local_pop();
			    ph.setClassNest(ph.getClassNest() - 1);
		    }
		| kDEF fname
		    {
			    if (ph.isInDef() || ph.isInSingle())
			        yyerror("nested method definition");
			    $$ = ph.getCurMid();
                ph.setCurMid($2);
			    ph.setInDef(ph.getInDef() + 1);
			    ph.local_push();
		    }
		  f_arglist
		  compstmt
		  rescue
		  opt_else
		  ensure
		  kEND
		    {
		        if ($6 != null)
                    $<>5 = nf.newRescue($5, $6, $7);
			    else if ($7 != null) {
			        ph.rb_warn("else without rescue is useless");
			        $<>5 = ph.block_append($5, $7);
			    }
			    if ($8 != null)
                    $<>5 = nf.newEnsure($5, $8);

		        /* NOEX_PRIVATE for toplevel */
			    $$ = nf.newDefn($2, $4, $5, ph.getClassNest() !=0 ? 
                                Constants.NOEX_PUBLIC : Constants.NOEX_PRIVATE);
			    if ($2.isAttrSetId())
                    $<Node>$.setNoex(Constants.NOEX_PUBLIC);
		        ph.fixpos($$, $4);
		        ph.local_pop();
			    ph.setInDef(ph.getInDef() - 1);
			    ph.setCurMid($<RubyId>3);
		    }
		| kDEF singleton dot_or_colon {ph.setLexState(LexState.EXPR_FNAME);} fname
		    {
			    ph.value_expr($2);
                ph.setInSingle(ph.getInSingle() + 1);
			    ph.local_push();
		        ph.setLexState(LexState.EXPR_END); /* force for args */
		    }
		  f_arglist
		  compstmt
		  rescue
		  opt_else
		  ensure
		  kEND
		    {
		        if ($9 != null)
                    $<>8 = nf.newRescue($8, $9, $10);
			    else if ($10 != null) {
			        ph.rb_warn("else without rescue is useless");
			        $<>8 = ph.block_append($8, $10);
			    }
			    if ($11 != null) $<>8 = nf.newEnsure($8, $11);

			    $$ = nf.newDefs($2, $5, $7, $8);
		        ph.fixpos($$, $2);
		        ph.local_pop();
			    ph.setInSingle(ph.getInSingle() - 1);
		    }
		| kBREAK
		    {
			    $$ = nf.newBreak();
		    }
		| kNEXT
		    {
			    $$ = nf.newNext();
		    }
		| kREDO
		    {
			    $$ = nf.newRedo();
		    }
		| kRETRY
		    {
			    $$ = nf.newRetry();
		    }

then		: term
		| kTHEN
		| term kTHEN

do		: term
		| kDO_COND

if_tail		: opt_else
		| kELSIF expr then
		  compstmt
		  if_tail
		    {
			    ph.value_expr($2);
			    $$ = nf.newIf(ph.cond($2), $4, $5);
		        ph.fixpos($$, $2);
		    }

opt_else	: none
		| kELSE compstmt
		    {
			    $$ = $2;
		    }

block_var	: lhs
		| mlhs

opt_block_var	: none
		| '|' /* none */ '|'
		    {
		        $$ = Node.ONE; // new Integer(1); //XXX (Node*)1;
		    }
		| tOROP
		    {
		        $$ = Node.ONE; // new Integer(1); //XXX (Node*)1;
		    }
		| '|' block_var '|'
		    {
			$$ = $2;
		    }

do_block	: kDO_BLOCK
		    {
		        $$ = ph.dyna_push();
		    }
		  opt_block_var
		  compstmt
		  kEND
		    {
			    $$ = nf.newIter($3, null, $4);
		        ph.fixpos($$, $3!=null?$3:$4);
			    ph.dyna_pop($<RubyVarmap>2);
		    }

block_call	: command do_block
		    {
			    if ($1 != null && $1.getType() == Constants.NODE_BLOCK_PASS) {
			        ph.rb_compile_error("both block arg and actual block given");
			    }
			    $2.setIterNode($1);
			    $$ = $2;
		        ph.fixpos($$, $2);
		    }
		| block_call '.' operation2 opt_paren_args
		    {
			    ph.value_expr($1);
			    $$ = ph.new_call($1, $3, $4);
		    }
		| block_call tCOLON2 operation2 opt_paren_args
		    {
			    ph.value_expr($1);
			    $$ = ph.new_call($1, $3, $4);
		    }

method_call	: operation paren_args
		    {
			    $$ = ph.new_fcall($1, $2);
		        ph.fixpos($$, $2);
		    }
		| primary '.' operation2 opt_paren_args
		    {
			    ph.value_expr($1);
			    $$ = ph.new_call($1, $3, $4);
		        ph.fixpos($$, $1);
		    }
		| primary tCOLON2 operation2 paren_args
		    {
			    ph.value_expr($1);
			    $$ = ph.new_call($1, $3, $4);
		        ph.fixpos($$, $1);
		    }
		| primary tCOLON2 operation3
		    {
			    ph.value_expr($1);
			    $$ = ph.new_call($1, $3, null);
		    }
		| kSUPER paren_args
		    {
			    if (!ph.isCompileForEval() && !ph.isInDef() && !ph.isInSingle() && !ph.isInDefined())
			        yyerror("super called outside of method");
			    $$ = ph.new_super($2);
		    }
		| kSUPER
		    {
			    if (!ph.isCompileForEval() && !ph.isInDef() && !ph.isInSingle() && !ph.isInDefined())
			        yyerror("super called outside of method");
			    $$ = nf.newZSuper();
		    }

brace_block	: '{'
		    {
		        $$ = ph.dyna_push();
		    }
		  opt_block_var
		  compstmt '}'
		    {
			    $$ = nf.newIter($3, null, $4);
		        ph.fixpos($$, $4);
			    ph.dyna_pop($<RubyVarmap>2);
		    }
		| kDO
		    {
		        $$ = ph.dyna_push();
		    }
		  opt_block_var
		  compstmt kEND
		    {
			    $$ = nf.newIter($3, null, $4);
		        ph.fixpos($$, $4);
			    ph.dyna_pop($<RubyVarmap>2);
		    }

case_body	: kWHEN when_args then
		  compstmt
		  cases
		    {
			    $$ = nf.newWhen($2, $4, $5);
		    }

when_args	: args
		| args ',' tSTAR arg
		    {
			    ph.value_expr($4);
			    $$ = ph.list_append($1, nf.newWhen($4, null, null));
		    }
		| tSTAR arg
		    {
			    ph.value_expr($2);
			    $$ = nf.newList(nf.newWhen($2, null, null));
		    }

cases		: opt_else
		| case_body

exc_list	: none
		| args

exc_var		: tASSOC lhs
		    {
			    $$ = $2;
		    }
		| none

rescue		: kRESCUE exc_list exc_var then
		  compstmt
		  rescue
		    {
		        if ($3 != null) {
		            $<>3 = ph.node_assign($3, nf.newGVar(ruby.intern("$!")));
			        $<>5 = ph.block_append($3, $5);
			    }
			    $$ = nf.newResBody($2, $5, $6);
		        ph.fixpos($$, $2!=null?$2:$5);
		    }
		| none

ensure		: none
		| kENSURE compstmt
		    {
			    if ($2 != null)
			        $$ = $2;
			    else
			        /* place holder */
			    $$ = nf.newNil();
		    }

literal		: numeric
		| symbol
		    {
			    $$ = $1.toSymbol();
		    }
		| tREGEXP

string		: tSTRING
		    {
			    $$ = nf.newStr($1);
		    }
		| tDSTRING
		| string tSTRING
		    {
		        if ($1.getType() == Constants.NODE_DSTR) {
			        ph.list_append($1, nf.newStr($2));
			    } else {
			        ((RubyString)$1.getLiteral()).m_concat((RubyString)$2);
			    }
			    $$ = $1;
		    }
		| string tDSTRING
		    {
		        if ($1.getType() == Constants.NODE_STR) {
			        $$ = nf.newDStr($1.getLiteral());
			    } else {
			        $$ = $1;
			    }
			    $2.setHeadNode(nf.newStr($2.getLiteral()));
                new RuntimeException("[BUG] Want to change " + $2.getClass().getName() + " to ArrayNode.").printStackTrace();
			    // $2.nd_set_type(Constants.NODE_ARRAY);
			    ph.list_concat($<Node>$, $2);
		    }

symbol		: tSYMBEG sym
		    {
		        ph.setLexState(LexState.EXPR_END);
			    $$ = $2;
		    }

sym		: fname
		| tIVAR
		| tGVAR
		| tCVAR

numeric		: tINTEGER
		| tFLOAT

variable	: tIDENTIFIER
		| tIVAR
		| tGVAR
		| tCONSTANT
		| tCVAR
		| kNIL {$$ = ph.newId(kNIL);}
		| kSELF {$$ = ph.newId(kSELF);}
		| kTRUE {$$ = ph.newId(kTRUE);}
		| kFALSE {$$ = ph.newId(kFALSE);}
		| k__FILE__ {$$ = ph.newId(k__FILE__);}
		| k__LINE__ {$$ = ph.newId(k__LINE__);}

var_ref		: variable
		    {
			    $$ = ph.gettable($1);
		    }

backref		: tNTH_REF
		| tBACK_REF

superclass	: term
		    {
			    $$ = null;
		    }
		| '<'
		    {
			    ph.setLexState(LexState.EXPR_BEG);
		    }
		  expr term
		    {
			    $$ = $3;
		    }
		| error term {yyerrok(); $$ = null;}

f_arglist	: '(' f_args opt_nl ')'
		    {
			    $$ = $2;
			    ph.setLexState(LexState.EXPR_BEG);
		    }
		| f_args term
		    {
			    $$ = $1;
		    }

f_args		: f_arg ',' f_optarg ',' f_rest_arg opt_f_block_arg
		    {
                $$ = ph.block_append(nf.newArgs($1, $3, $5), $6);
		    }
		| f_arg ',' f_optarg opt_f_block_arg
		    {
                $$ = ph.block_append(nf.newArgs($1, $3, RubyId.newId(ruby, -1)), $4);
		    }
		| f_arg ',' f_rest_arg opt_f_block_arg
		    {
                $$ = ph.block_append(nf.newArgs($1, null, $3), $4);
		    }
		| f_arg opt_f_block_arg
		    {
                $$ = ph.block_append(nf.newArgs($1, null, RubyId.newId(ruby, -1)), $2);
		    }
		| f_optarg ',' f_rest_arg opt_f_block_arg
		    {
			    $$ = ph.block_append(nf.newArgs(null, $1, $3), $4);
		    }
		| f_optarg opt_f_block_arg
		    {
			    $$ = ph.block_append(nf.newArgs(null, $1, RubyId.newId(ruby, -1)), $2);
		    }
		| f_rest_arg opt_f_block_arg
		    {
			    $$ = ph.block_append(nf.newArgs(null, null, $1), $2);
		    }
		| f_block_arg
		    {
			    $$ = ph.block_append(nf.newArgs(null, null, RubyId.newId(ruby, -1)), $1);
		    }
		| /* none */
		    {
			    $$ = nf.newArgs(null, null, RubyId.newId(ruby, -1));
		    }

f_norm_arg	: tCONSTANT
		    {
			    yyerror("formal argument cannot be a constant");
		    }
                | tIVAR
		    {
                yyerror("formal argument cannot be an instance variable");
		    }
                | tGVAR
		    {
                yyerror("formal argument cannot be a global variable");
		    }
                | tCVAR
		    {
                yyerror("formal argument cannot be a class variable");
		    }
		| tIDENTIFIER
		    {
			    if (!$1.isLocalId())
			        yyerror("formal argument must be local variable");
			    else if (ph.local_id($1))
			        yyerror("duplicate argument name");
			    ph.local_cnt($1);
			    $$ = new Integer(1);
		    }

f_arg		: f_norm_arg
		| f_arg ',' f_norm_arg
		    {
			    $$ = new Integer($<Integer>$.intValue() + 1);
		    }

f_opt		: tIDENTIFIER '=' arg
		    {
			    if (!$1.isLocalId())
			        yyerror("formal argument must be local variable");
			    else if (ph.local_id($1))
			        yyerror("duplicate optional argument name");
			    $$ = ph.assignable($1, $3);
		    }

f_optarg	: f_opt
		    {
			    $$ = nf.newBlock($1);
			    $<Node>$.setEndNode($<Node>$);
		    }
		| f_optarg ',' f_opt
		    {
			    $$ = ph.block_append($1, $3);
		    }

f_rest_arg	: tSTAR tIDENTIFIER
		    {
			    if (!$2.isLocalId())
			        yyerror("rest argument must be local variable");
			    else if (ph.local_id($2))
			        yyerror("duplicate rest argument name");
			    $$ = new Integer(ph.local_cnt($2));
		    }
		| tSTAR
		    {
			    $$ = new Integer(-2);
		    }

f_block_arg	: tAMPER tIDENTIFIER
		    {
			    if (!$2.isLocalId())
			        yyerror("block argument must be local variable");
			    else if (ph.local_id($2))
			        yyerror("duplicate block argument name");
			    $$ = nf.newBlockArg($2);
		    }

opt_f_block_arg	: ',' f_block_arg
		    {
			    $$ = $2;
		    }
		| none

singleton	: var_ref
		    {
			    if ($1.getType() == Constants.NODE_SELF) {
			        $$ = nf.newSelf();
			    } else {
			        $$ = $1;
			    }
		    }
		| '(' {ph.setLexState(LexState.EXPR_BEG);} expr opt_nl ')'
		    {
			    switch ($3.getType()) {
			        case Constants.NODE_STR:
			        case Constants.NODE_DSTR:
			        case Constants.NODE_XSTR:
			        case Constants.NODE_DXSTR:
			        case Constants.NODE_DREGX:
			        case Constants.NODE_LIT:
			        case Constants.NODE_ARRAY:
			        case Constants.NODE_ZARRAY:
			            yyerror("can't define single method for literals.");
			        default:
			            break;
			    }
			    $$ = $3;
		    }

assoc_list	: none
		| assocs trailer
		    {
			    $$ = $1;
		    }
		| args trailer
		    {
			    /* if ($1.getLength() % 2 != 0) {
			        yyerror("odd number list for Hash");
			    }*/
			    $$ = $1;
		    }

assocs		: assoc
		| assocs ',' assoc
		    {
			    $$ = ph.list_concat($1, $3);
		    }

assoc		: arg tASSOC arg
		    {
			    $$ = ph.list_append(nf.newList($1), $3);
		    }

operation	: tIDENTIFIER
		| tCONSTANT
		| tFID

operation2	: tIDENTIFIER
		| tCONSTANT
		| tFID
		| op

operation3	: tIDENTIFIER
		| tFID
		| op

dot_or_colon	: '.'
		| tCOLON2

opt_terms	: /* none */
		| terms

opt_nl		: /* none */
		| '\n'

trailer		: /* none */
		| '\n'
		| ','

term		: ';' {yyerrok();}
		| '\n'

terms		: term
		| terms ';' {yyerrok();}

none		: /* none */
		    {
		        $$ = null; //XXX 0;
		    }
%%

    // XXX +++
    // Helper Methods
    
    void yyerrok() {}
    
    // XXX ---
    
    // -----------------------------------------------------------------------
    // scanner stuff
    // -----------------------------------------------------------------------

    /*
     *  int yyerror(String msg) {
     *  char *p, *pe, *buf;
     *  int len, i;
     *  rb_compile_error("%s", msg);
     *  p = lex_p;
     *  while (lex_pbeg <= p) {
     *  if (*p == '\n') break;
     *  p--;
     *  }
     *  p++;
     *  pe = lex_p;
     *  while (pe < lex_pend) {
     *  if (*pe == '\n') break;
     *  pe++;
     *  }
     *  len = pe - p;
     *  if (len > 4) {
     *  buf = ALLOCA_N(char, len+2);
     *  MEMCPY(buf, p, char, len);
     *  buf[len] = '\0';
     *  rb_compile_error_append("%s", buf);
     *  i = lex_p - p;
     *  p = buf; pe = p + len;
     *  while (p < pe) {
     *  if (*p != '\t') *p = ' ';
     *  p++;
     *  }
     *  buf[i] = '^';
     *  buf[i+1] = '\0';
     *  rb_compile_error_append("%s", buf);
     *  }
     *  return 0;
     *  }
     */
    
    public Node compileString(String f, RubyObject s, int line) {
        rs.setLexFileIo(false);
        rs.setLexGetsPtr(0);
        rs.setLexInput(s);
        rs.setLexP(0);
        rs.setLexPEnd(0);
        ruby.setSourceLine(line - 1);

        ph.setCompileForEval(ruby.getInEval());

        return yycompile(f, line);
    }

    public Node compileJavaString(String f, String s, int len, int line) {
        return compileString(f, RubyString.m_newString(ruby, s, len), line);
    }

    public Node compileFile(String f, RubyObject file, int start) {
        rs.setLexFileIo(true);
        rs.setLexInput(file);
        rs.setLexP(0);
        rs.setLexPEnd(0);
        ruby.setSourceLine(start - 1);

        return yycompile(f, start);
    }
    
    private void init_for_scanner(String s) {
        rs.setLexFileIo(false);
        rs.setLexGetsPtr(0);
        rs.setLexInput(RubyString.m_newString(ruby, s));
        rs.setLexP(0);
        rs.setLexPEnd(0);
        ruby.setSourceLine(0);
        ph.setCompileForEval(ruby.getInEval());
        ph.setRubyEndSeen(false); // is there an __end__{} statement?
        ph.setHeredocEnd(0);
        ph.setRubyInCompile(true);
    }
    
    /** This function compiles a given String into a Node.
     *
     */
    public Node yycompile(String f, int line) {
        RubyId sl_id = ruby.intern("SCRIPT_LINES__");
        if (!ph.isCompileForEval() && ruby.getSecurityLevel() == 0 && ruby.getClasses().getObjectClass().isConstantDefined(sl_id)) {
            RubyHash hash = (RubyHash)ruby.getClasses().getObjectClass().getConstant(sl_id);
            RubyString fName = RubyString.m_newString(ruby, f);
            
            // XXX +++
            RubyObject debugLines = ruby.getNil(); // = rb_hash_aref(hash, fName);
            // XXX ---
            
            if (debugLines.isNil()) {
                ph.setRubyDebugLines(RubyArray.m_newArray(ruby));
                hash.m_aset(fName, ph.getRubyDebugLines());
            } else {
                ph.setRubyDebugLines((RubyArray)debugLines);
            }
            
            if (line > 1) {
                RubyString str = RubyString.m_newString(ruby, null);
                while (line > 1) {
                    ph.getRubyDebugLines().m_push(str);
                    line--;
                }
            }
        }

        ph.setRubyEndSeen(false);   // is there an __end__{} statement?
        ph.setEvalTree(null);       // parser stores Nodes here
        ph.setHeredocEnd(0);
        ruby.setSourceFile(f);      // source file name
        ph.setRubyInCompile(true);

        try {
            yyparse(rs, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ph.setRubyDebugLines(null); // remove debug info
        ph.setCompileForEval(0);
        ph.setRubyInCompile(false);
        rs.resetStacks();
        ph.setClassNest(0);
        ph.setInSingle(0);
        ph.setInDef(0);
        ph.setCurMid(null);

        return ph.getEvalTree();
    }
}
