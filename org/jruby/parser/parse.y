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
import org.jruby.interpreter.*;
import org.jruby.interpreter.nodes.*;
import org.jruby.original.*;
import org.jruby.util.*;

public class DefaultRubyParser implements RubyParser {
    private Ruby ruby;
    private ParserHelper ph;
    private NodeFactory nf;
    
    private long cond_stack;
    private int cond_nest = 0;
    
    public DefaultRubyParser(Ruby ruby) {
        this.ruby = ruby;
        this.ph = ruby.getParserHelper();
        this.nf = new NodeFactory(ruby);
    }

    private void COND_PUSH() {
        cond_nest++;
        cond_stack = (cond_stack << 1) | 1;
    }

    private void COND_POP() {
        cond_nest--;
        cond_stack >>= 1;
    }

    private boolean COND_P() {
        return (cond_nest > 0 && (cond_stack & 1) != 0);
    }

    private void COND_LEXPOP() {
        boolean last = COND_P();
        cond_stack >>= 1;
        if (last) cond_stack |= 1;
    }


    private long cmdarg_stack;

    void CMDARG_PUSH() {
        cmdarg_stack = (cmdarg_stack << 1) | 1;
    }

    private void CMDARG_POP() {
        cmdarg_stack >>= 1;
    }

    private void CMDARG_LEXPOP() {
        boolean last = CMDARG_P();
        cmdarg_stack >>= 1;
        if (last) cmdarg_stack |= 1;
    }

    private boolean CMDARG_P() {
        return (cmdarg_stack != 0 && (cmdarg_stack & 1) != 0);
    }
/*
%union {
    NODE *node;
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
%token <VALUE> tINTEGER tFLOAT tSTRING tXSTRING tREGEXP
%token <NODE>  tDSTRING tDXSTRING tDREGEXP tNTH_REF tBACK_REF

%type <NODE>  singleton string
%type <VALUE> literal numeric
%type <NODE>  compstmt stmts stmt expr arg primary command command_call method_call
%type <NODE>  if_tail opt_else case_body cases rescue exc_list exc_var ensure
%type <NODE>  args ret_args when_args call_args paren_args opt_paren_args
%type <NODE>  command_args aref_args opt_block_arg block_arg var_ref
%type <NODE>  mrhs mrhs_basic superclass block_call block_command
%type <NODE>  f_arglist f_args f_optarg f_opt f_block_arg opt_f_block_arg
%type <NODE>  assoc_list assocs assoc undef_list backref
%type <NODE>  block_var opt_block_var brace_block do_block lhs none
%type <NODE>  mlhs mlhs_head mlhs_basic mlhs_entry mlhs_item mlhs_node
%type <RubyId>    fitem variable sym symbol operation operation2 operation3
%type <RubyId>    cname fname op f_rest_arg
%type <Integer>   f_norm_arg f_arg
%token <Integer> tUPLUS 	/* unary+ */
%token <Integer> tUMINUS 	/* unary- */
%token <Integer> tPOW		/* ** */
%token <Integer> tCMP  		/* <=> */
%token <Integer> tEQ  		/* == */
%token <Integer> tEQQ  		/* === */
%token <Integer> tNEQ  		/* != */
%token <Integer> tGEQ  		/* >= */
%token <Integer> tLEQ  		/* <= */
%token <Integer> tANDOP tOROP	/* && and || */
%token <Integer> tMATCH tNMATCH	/* =~ and !~ */
%token <Integer> tDOT2 tDOT3	/* .. and ... */
%token <Integer> tAREF tASET	/* [] and []= */
%token <Integer> tLSHFT tRSHFT	/* << and >> */
%token <Integer> tCOLON2	/* :: */
%token <Integer> tCOLON3	/* :: at EXPR_BEG */
%token <Integer> tOP_ASGN	/* +=, -=  etc. */
%token <Integer> tASSOC		/* => */
%token <Integer> tLPAREN	/* ( */
%token <Integer> tLBRACK	/* [ */
%token <Integer> tLBRACE	/* { */
%token <Integer> tSTAR		/* * */
%token <Integer> tAMPER		/* & */
%token <Integer> tSYMBEG

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
			        if ($2.nd_type() != NODE.NODE_BLOCK)
                        ph.void_expr($2);
			        else {
                        NODE node = $2;
				        while (node.nd_next() != null) {
				            node = node.nd_next();
				        }
				        ph.void_expr(node.nd_head());
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
			    String buf = "$" + (char)$3.nd_nth();
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
			    if ($1 != null && $1.nd_type() == NODE.NODE_BEGIN) {
			        $$ = nf.newWhile(ph.cond($3), $1.nd_body(), 0);
			    } else {
			        $$ = nf.newWhile(ph.cond($3), $1, 1);
			    }
		    }
		| stmt kUNTIL_MOD expr
		    {
			    ph.value_expr($3);
			    if ($1 != null && $1.nd_type() == NODE.NODE_BEGIN) {
			        $$ = nf.newUntil(ph.cond($3), $1.nd_body(), 0);
			    } else {
			        $$ = nf.newUntil(ph.cond($3), $1, 1);
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
			    $1.nd_value($3);
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
			    $1.nd_value($3);
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
			    $$ = ph.logop(NODE.NODE_AND, $1, $3);
		    }
		| expr kOR expr
		    {
			    $$ = ph.logop(NODE.NODE_OR, $1, $3);
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
			    $$ = nf.newMAsgn($1, NODE.MINUS_ONE);
		    }
		| tSTAR mlhs_node
		    {
			    $$ = nf.newMAsgn(null, $2);
		    }
		| tSTAR
		    {
			    $$ = nf.newMAsgn(null, NODE.MINUS_ONE);
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
			    $$ = $<RubyId>1;
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

op		: '|'		{ $$ = new Integer('|'); }
		| '^'		{ $$ = new Integer('^'); }
		| '&'		{ $$ = new Integer('&'); }
		| tCMP		{ $$ = new Integer(tCMP); }
		| tEQ		{ $$ = new Integer(tEQ); }
		| tEQQ		{ $$ = new Integer(tEQQ); }
		| tMATCH	{ $$ = new Integer(tMATCH); }
		| '>'		{ $$ = new Integer('>'); }
		| tGEQ		{ $$ = new Integer(tGEQ); }
		| '<'		{ $$ = new Integer('<'); }
		| tLEQ		{ $$ = new Integer(tLEQ); }
		| tLSHFT	{ $$ = new Integer(tLSHFT); }
		| tRSHFT	{ $$ = new Integer(tRSHFT); }
		| '+'		{ $$ = new Integer('+'); }
		| '-'		{ $$ = new Integer('-'); }
		| '*'		{ $$ = new Integer('*'); }
		| tSTAR		{ $$ = new Integer('*'); }
		| '/'		{ $$ = new Integer('/'); }
		| '%'		{ $$ = new Integer('%'); }
		| tPOW		{ $$ = new Integer(tPOW); }
		| '~'		{ $$ = new Integer('~'); }
		| tUPLUS	{ $$ = new Integer(tUPLUS); }
		| tUMINUS	{ $$ = new Integer(tUMINUS); }
		| tAREF		{ $$ = new Integer(tAREF); }
		| tASET		{ $$ = new Integer(tASET); }
		| '`'		{ $$ = new Integer('`'); }

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
			        $<NODE>3.nd_value($4);
			        $$ = nf.newOpAsgnOr(ph.gettable($1), $<NODE>3);
			        if ($1.is_instance_id()) {
				        $<NODE>$.nd_aid($1);
			        }
			    } else if ($2.intValue() == tANDOP) {
			        $<NODE>3.nd_value($4);
			        $$ = nf.newOpAsgnAnd(ph.gettable($1), $<NODE>3);
			    } else {
			        $$ = $3;
			        if ($$ != null) {
				        $<NODE>$.nd_value(ph.call_op(ph.gettable($1),$2.intValue(),1,$4));
			        }
			    }
			    ph.fixpos($$, $4);
		    }
		| primary '[' aref_args ']' tOP_ASGN arg
		    {
			    NODE args = nf.newList($6);

			    ph.list_append($3, nf.newNil());
			    ph.list_concat(args, $3);
                if ($5.intValue() == Token.tOROP) {
			        $<>5 = new Integer(0);
			    } else if ($5.intValue() == Token.tANDOP) {
			        $<>5 = new Integer(1);
			    }
			    $$ = nf.newOpAsgn1($1, $5, args);
		        ph.fixpos($$, $1);
		    }
		| primary '.' tIDENTIFIER tOP_ASGN arg
		    {
                if ($4.intValue() == Token.tOROP) {
			        $<>4 = new Integer(0);
			    } else if ($4.intValue() == Token.tANDOP) {
			        $<>4 = new Integer(1);
			    }
			    $$ = nf.newOpAsgn2($1, $3, $4, $5);
		        ph.fixpos($$, $1);
		    }
		| primary '.' tCONSTANT tOP_ASGN arg
		    {
                if ($4.intValue() == Token.tOROP) {
			        $<>4 = new Integer(0);
			    } else if ($4.intValue() == Token.tANDOP) {
			        $<>4 = new Integer(1);
			    }
			    $$ = nf.newOpAsgn2($1, $3, $4, $5);
		        ph.fixpos($$, $1);
		    }
		| primary tCOLON2 tIDENTIFIER tOP_ASGN arg
		    {
			    if ($4.intValue() == Token.tOROP) {
			        $<>4 = new Integer(0);
			    } else if ($4.intValue() == Token.tANDOP) {
			        $<>4 = new Integer(1);
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

			    if ($1.nd_type() == NODE.NODE_LIT) {
                    if ($1.nd_lit() instanceof RubyFixnum || 
                        $1.nd_lit() instanceof RubyFloat ||
                        $1.nd_lit() instanceof RubyBignum) {
                        if ($1.nd_lit().funcall(ruby.intern("<"), RubyFixnum.zero(ruby)).isTrue()) {
                            $1.nd_lit($1.nd_lit().funcall(ruby.intern("-@")));
                            need_negate = true;
                        }
                    }
			    }
			    $$ = ph.call_op($1, tPOW, 1, $3);
			    if (need_negate) {
			        $$ = ph.call_op($<NODE>$, tUMINUS, 0, null);
			    }
		    }
		| tUPLUS arg
		    {
			    if ($2 != null && $2.nd_type() == NODE.NODE_LIT) {
			        $$ = $2;
			    } else {
			        $$ = ph.call_op($2, tUPLUS, 0, null);
			    }
		    }
		| tUMINUS arg
		    {
			    if ($2 != null && $2.nd_type() == NODE.NODE_LIT && $2.nd_lit() instanceof RubyFixnum) {
			        long i = ((RubyFixnum)$2.nd_lit()).getValue();

			        $2.nd_lit(RubyFixnum.m_newFixnum(ruby, -i));
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
			    $$ = ph.logop(NODE.NODE_AND, $1, $3);
		    }
		| arg tOROP arg
		    {
			    $$ = ph.logop(NODE.NODE_OR, $1, $3);
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
			    $$ = ph.arg_blk_pass($<NODE>1, $2);
		    }
		| args ',' tSTAR arg opt_block_arg
		    {
			    ph.value_expr($4);
			    $$ = ph.arg_concat($1, $4);
			    $$ = ph.arg_blk_pass($<NODE>$, $5);
		    }
		| assocs opt_block_arg
		    {
			    $$ = nf.newList(nf.newHash($1));
			    $$ = ph.arg_blk_pass($<NODE>$, $2);
		    }
		| assocs ',' tSTAR arg opt_block_arg
		    {
			    ph.value_expr($4);
			    $$ = ph.arg_concat(nf.newList(nf.newHash($1)), $4);
			    $$ = ph.arg_blk_pass($<NODE>$, $5);
		    }
		| args ',' assocs opt_block_arg
		    {
			    $$ = ph.list_append($1, nf.newHash($3));
			    $$ = ph.arg_blk_pass($<NODE>$, $4);
		    }
		| args ',' assocs ',' tSTAR arg opt_block_arg
		    {
			    ph.value_expr($6);
			    $$ = ph.arg_concat(ph.list_append($1, nf.newHash($3)), $6);
			    $$ = ph.arg_blk_pass($<NODE>$, $7);
		    }
		| tSTAR arg opt_block_arg
		    {
			    ph.value_expr($2);
			    $$ = ph.arg_blk_pass(nf.newRestArgs($2), $3);
		    }
		| block_arg

command_args	:  {CMDARG_PUSH();} call_args
		    {
			    CMDARG_POP();
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
			        if ($1.nd_type() == NODE.NODE_ARRAY && $1.nd_next() == null) {
				        $$ = $1.nd_head();
    			    } else if ($1.nd_type() == NODE.NODE_BLOCK_PASS) {
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
			    $2.nd_iter(nf.newFCall($1, null));
			    $$ = $2;
		    }
		| method_call
		| method_call brace_block
		    {
			    if ($1 != null && $1.nd_type() == NODE.NODE_BLOCK_PASS) {
			        ph.rb_compile_error("both block arg and actual block given");
			    }
			    $2.nd_iter($1);
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
		| kWHILE {COND_PUSH();} expr do {COND_POP();}
		  compstmt
		  kEND
		    {
			    ph.value_expr($3);
			    $$ = nf.newWhile(ph.cond($3), $6, 1);
		        ph.fixpos($$, $3);
		    }
		| kUNTIL {COND_PUSH();} expr do {COND_POP();} 
		  compstmt
		  kEND
		    {
			    ph.value_expr($3);
			    $$ = nf.newUntil(ph.cond($3), $6, 1);
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
		| kFOR block_var kIN {COND_PUSH();} expr do {COND_POP();}
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
		        $<NODE>$.nd_set_line($<Integer>4.intValue());
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
		        $<NODE>$.nd_set_line($<Integer>3.intValue());
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
			    $$ = nf.newDefn($2, $4, $5, ph.getClassNest() !=0 ? NODE.NOEX_PUBLIC : NODE.NOEX_PRIVATE);
			    if ($2.is_attrset_id())
                    $<NODE>$.nd_noex(NODE.NOEX_PUBLIC);
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
		        $$ = new Integer(1); //XXX (NODE*)1;
		    }
		| tOROP
		    {
		        $$ = new Integer(1); //XXX (NODE*)1;
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
			    if ($1 != null && $1.nd_type() == NODE.NODE_BLOCK_PASS) {
			        ph.rb_compile_error("both block arg and actual block given");
			    }
			    $2.nd_iter($1);
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
		        if ($1.nd_type() == NODE.NODE_DSTR) {
			        ph.list_append($1, nf.newStr($2));
			    } else {
			        ((RubyString)$1.nd_lit()).m_cat((RubyString)$2);
			    }
			    $$ = $1;
		    }
		| string tDSTRING
		    {
		        if ($1.nd_type() == NODE.NODE_STR) {
			        $$ = nf.newDStr($1.nd_lit());
			    } else {
			        $$ = $1;
			    }
			    $2.nd_head(nf.newStr($2.nd_lit()));
			    $2.nd_set_type(NODE.NODE_ARRAY);
			    ph.list_concat($<NODE>$, $2);
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
			    $$ = ph.block_append(nf.newArgs($1, $3, new Integer(-1)), $4);
		    }
		| f_arg ',' f_rest_arg opt_f_block_arg
		    {
			    $$ = ph.block_append(nf.newArgs($1, null, $3), $4);
		    }
		| f_arg opt_f_block_arg
		    {
			    $$ = ph.block_append(nf.newArgs($1, null, new Integer(-1)), $2);
		    }
		| f_optarg ',' f_rest_arg opt_f_block_arg
		    {
			    $$ = ph.block_append(nf.newArgs(null, $1, $3), $4);
		    }
		| f_optarg opt_f_block_arg
		    {
			    $$ = ph.block_append(nf.newArgs(null, $1, new Integer(-1)), $2);
		    }
		| f_rest_arg opt_f_block_arg
		    {
			    $$ = ph.block_append(nf.newArgs(null, null, $1), $2);
		    }
		| f_block_arg
		    {
			    $$ = ph.block_append(nf.newArgs(null, null, new Integer(-1)), $1);
		    }
		| /* none */
		    {
			    $$ = nf.newArgs(null, null, new Integer(-1));
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
			    if (!$1.is_local_id())
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
			    if (!$1.is_local_id())
			        yyerror("formal argument must be local variable");
			    else if (ph.local_id($1))
			        yyerror("duplicate optional argument name");
			    $$ = ph.assignable($1, $3);
		    }

f_optarg	: f_opt
		    {
			    $$ = nf.newBlock($1);
			    $<NODE>$.nd_end($<NODE>$);
		    }
		| f_optarg ',' f_opt
		    {
			    $$ = ph.block_append($1, $3);
		    }

f_rest_arg	: tSTAR tIDENTIFIER
		    {
			    if (!$2.is_local_id())
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
			    if (!$2.is_local_id())
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
			    if ($1.nd_type() == NODE.NODE_SELF) {
			        $$ = nf.newSelf();
			    } else {
			        $$ = $1;
			    }
		    }
		| '(' {ph.setLexState(LexState.EXPR_BEG);} expr opt_nl ')'
		    {
			    switch ($3.nd_type()) {
			        case NODE.NODE_STR:
			        case NODE.NODE_DSTR:
			        case NODE.NODE_XSTR:
			        case NODE.NODE_DXSTR:
			        case NODE.NODE_DREGX:
			        case NODE.NODE_LIT:
			        case NODE.NODE_ARRAY:
			        case NODE.NODE_ZARRAY:
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
			    if ($1.nd_alen()%2 != 0) {
			        yyerror("odd number list for Hash");
			    }
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
    
    private kwtable rb_reserved_word(String w, int len) {
        return kwtable.rb_reserved_word(w, len);
    }
        
    private RubyFixnum rb_cstr2inum(String s, int radix) {
        //XXX no support for _ or leading and trailing spaces
        return RubyFixnum.m_newFixnum(ruby, Integer.parseInt(s, radix));
    }
    
    private RubyRegexp rb_reg_new(String s, int len, int options) {
        return new RubyRegexp(ruby, RubyString.m_newString(ruby, s, len), options);
    }
    
    // -----------------------------------------------------------------------
    // scanner stuff
    // -----------------------------------------------------------------------

    //XXX yyInput implementation
    private int token = 0;
    //XXX yyInput END

    private String lex_curline; // current line
    //private int lex_pbeg;     //XXX not needed
    private int lex_p;          // pointer in current line
    private int lex_pend;       // pointer to end of line
    
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
    
    // beginning of the next line
    private int lex_gets_ptr;
    private RubyObject lex_input;    // non-nil if File
    private RubyObject lex_lastline;

    /**
     *  true, if scanner source is a file and false, if lex_get_str() shall be
     *  used.
     */
    private boolean lex_file_io;

    // deal with tokens..................

    private StringBuffer tokenbuf;

    private Object yyVal;

    public boolean advance() throws java.io.IOException {
        return (token = yylex()) != 0;
    }

    public int token() {
        return token;
    }

    public Object value() {
        return yyVal;
    }

    public NODE compileString(String f, RubyObject s, int line) {
        lex_file_io = false;
        lex_gets_ptr = 0;
        lex_input = s;
        lex_p = lex_pend = 0;
        ruby.setSourceLine(line - 1);

        ph.setCompileForEval(ruby.getInEval());

        return yycompile(f, line);
    }

    public NODE compileJavaString(String f, String s, int len, int line) {
        return compileString(f, RubyString.m_newString(ruby, s, len), line);
    }

    public NODE compileFile(String f, RubyObject file, int start) {
        lex_file_io = true;
        lex_input = file;
        lex_p = lex_pend = 0;
        ruby.setSourceLine(start - 1);

        return yycompile(f, start);
    }

    /**
     *  Returns true if "c" is a valid identifier character (letter, digit or
     *  underscore)
     *
     *@param  ch  Description of Parameter
     *@return     Description of the Returned Value
     */
    private boolean is_identchar(int ch) {
        return Character.isLetterOrDigit((char) ch) || ch == '_';
    }

    /* gc protect */
    private RubyObject lex_gets_str(RubyObject _s) {
        String s = ((RubyString)_s).getValue();
        if (lex_gets_ptr != 0) {
            if (s.length() == lex_gets_ptr) {
                return ruby.getNil();
            }
            s = s.substring(lex_gets_ptr);
        }
        int end = 0;
        while (end < s.length()) {
            if (s.charAt(end++) == '\n') {
                break;
            }
        }
        lex_gets_ptr += end;
        return RubyString.m_newString(ruby, s, end);
    }

    /**
     *  Returns in next line either from file or from a string.
     *
     *@return    Description of the Returned Value
     */
    private RubyObject lex_getline() {
        RubyObject line;
        if (lex_file_io) {
            // uses rb_io_gets(lex_input)
            throw new Error();
        } else {
            line = lex_gets_str(lex_input);
        }
        if (ph.getRubyDebugLines() != null && !line.isNil()) {
            ph.getRubyDebugLines().m_push(line);
        }
        return line;
    }

    private void init_for_scanner(String s) {
        lex_file_io = false;
        lex_gets_ptr = 0;
        lex_input = RubyString.m_newString(ruby, s);
        lex_p = lex_pend = 0;
        ruby.setSourceLine(0);
        ph.setCompileForEval(ruby.getInEval());
        ph.setRubyEndSeen(false); // is there an __end__{} statement?
        ph.setHeredocEnd(0);
        ph.setRubyInCompile(true);
    }

    /**
     *  Returns the next character from input
     *
     *@return    Description of the Returned Value
     */
    private int nextc() {
        int c;

        if (lex_p == lex_pend) {
            if (lex_input != null) {
                RubyObject v = lex_getline();

                if (v.isNil()) {
                    return -1;
                }
                if (ph.getHeredocEnd() > 0) {
                    ruby.setSourceLine(ph.getHeredocEnd());
                    ph.setHeredocEnd(0);
                }
                ruby.setSourceLine(ruby.getSourceLine() + 1);
                lex_curline = ((RubyString)v).getValue();
                lex_p = 0;
                lex_pend = lex_curline.length();
                if (lex_curline.startsWith("__END__") && (lex_pend == 7 || lex_curline.charAt(7) == '\n' || lex_curline.charAt(7) == '\r')) {
                    ph.setRubyEndSeen(true);
                    lex_lastline = null;
                    return -1;
                }
                lex_lastline = v;
            } else {
                lex_lastline = null;
                return -1;
            }
        }
        c = lex_curline.charAt(lex_p++);
        if (c == '\r' && lex_p <= lex_pend && lex_curline.charAt(lex_p) == '\n') {
            lex_p++;
            c = '\n';
        }

        return c;
    }

    /**
     *  Puts back the given character so that nextc() will answer it next time
     *  it'll be called.
     *
     *@param  c  Description of Parameter
     */
    private void pushback(int c) {
        if (c == -1) {
            return;
        }
        lex_p--;
    }

    /**
     *  Returns true if the given character is the current one in the input
     *  stream
     *
     *@param  c  Description of Parameter
     *@return    Description of the Returned Value
     */
    private boolean peek(int c) {
        return lex_p != lex_pend && c == lex_curline.charAt(lex_p);
    }

    private String tok() {
        return tokenbuf.toString();
    }

    private int toklen() {
        return tokenbuf.length();
    }

    private void tokfix() { }

    private char toklast() {
        return tokenbuf.charAt(toklen() - 1);
    }

    private void newtok() {
        tokenbuf = new StringBuffer(60);
    }

    private void tokadd(int c) {
        tokenbuf.append((char) c);
    }


    // yylex helpers...................

    private int read_escape() {
        int c;

        switch (c = nextc()) {
            case '\\': // Backslash
                return c;
            case 'n':  // newline
                return '\n';
            case 't':  // horizontal tab
                return '\t';
            case 'r':  // carriage-return
                return '\r';
            case 'f':  // form-feed
                return '\f';
            case 'v':  // vertical tab
                return '\013';
            case 'a':  // alarm(bell)
                return '\007';
            case 'e':  // escape
                return '\033';
            case '0':
            case '1':
            case '2':
            case '3': // octal constant
            case '4':
            case '5':
            case '6':
            case '7':
            {
                int cc = 0;

                pushback(c);
                for (int i = 0; i < 3; i++) {
                    c = nextc();
                    if (c == -1) {
                        // goto eof
                        yyerror("Invalid escape character syntax");
                        return '\0';
                    }
                    if (c < '0' || '7' < c) {
                        pushback(c);
                        break;
                    }
                    cc = cc * 8 + c - '0';
                }
                c = cc;
            }
                return c;
            case 'x':
            {
                /*
                 *  hex constant
                 */
                int[] numlen = new int[1];
                c = (int) scan_hex(lex_curline, lex_p, 2, numlen);
                lex_p += numlen[0];
            }
                return c;
            case 'b':
                /*
                 *  backspace
                 */
                return '\010';
            case 's':
                /*
                 *  space
                 */
                return ' ';
            case 'M':
                if ((c = nextc()) != '-') {
                    yyerror("Invalid escape character syntax");
                    pushback(c);
                    return '\0';
                }
                if ((c = nextc()) == '\\') {
                    return read_escape() | 0x80;
                } else if (c == -1) {
                    // goto eof
                    yyerror("Invalid escape character syntax");
                    return '\0';
                } else {
                    return ((c & 0xff) | 0x80);
                }

            case 'C':
                if ((c = nextc()) != '-') {
                    yyerror("Invalid escape character syntax");
                    pushback(c);
                    return '\0';
                }
            case 'c':
                if ((c = nextc()) == '\\') {
                    c = read_escape();
                } else if (c == '?') {
                    return 0177;
                } else if (c == -1) {
                    // goto eof
                    yyerror("Invalid escape character syntax");
                    return '\0';
                }
                return c & 0x9f;
            case -1:
                // eof:
                yyerror("Invalid escape character syntax");
                return '\0';
            default:
                return c;
        }
    }

    private int tokadd_escape(int term) {
        /*
         *  FIX 1.6.5
         */
        int c;

        switch (c = nextc()) {
            case '\n':
                return 0;
            /*
             *  just ignore
             */
            case '0':
            case '1':
            case '2':
            case '3':
            /*
             *  octal constant
             */
            case '4':
            case '5':
            case '6':
            case '7':
            {
                int i;

                tokadd('\\');
                tokadd(c);
                for (i = 0; i < 2; i++) {
                    c = nextc();
                    if (c == -1) {
                        // goto eof;
                        yyerror("Invalid escape character syntax");
                        return -1;
                        // goto eof; end
                    }

                    if (c < '0' || '7' < c) {
                        pushback(c);
                        break;
                    }
                    tokadd(c);
                }
            }
                return 0;
            case 'x':
            {
                /*
                 *  hex constant
                 */
                tokadd('\\');
                tokadd(c);

                int[] numlen = new int[1];
                scan_hex(lex_curline, lex_p, 2, numlen);
                while (numlen[0]-- != 0) {
                    tokadd(nextc());
                }
            }
                return 0;
            case 'M':
                if ((c = nextc()) != '-') {
                    yyerror("Invalid escape character syntax");
                    pushback(c);
                    return 0;
                }
                tokadd('\\');
                tokadd('M');
                tokadd('-');
                //goto escaped;
                if ((c = nextc()) == '\\') {
                    return tokadd_escape(term);
                    /*
                     *  FIX 1.6.5
                     */
                } else if (c == -1) {
                    // goto eof;
                    yyerror("Invalid escape character syntax");
                    return -1;
                    // goto eof; end
                }
                tokadd(c);
                return 0;
            case 'C':
                if ((c = nextc()) != '-') {
                    yyerror("Invalid escape character syntax");
                    pushback(c);
                    return 0;
                }
                tokadd('\\');
                tokadd('C');
                tokadd('-');
                //goto escaped;
                if ((c = nextc()) == '\\') {
                    return tokadd_escape(term);
                    /*
                     *  FIX 1.6.5
                     */
                } else if (c == -1) {
                    // goto eof;
                    yyerror("Invalid escape character syntax");
                    return -1;
                    // goto eof; end
                }
                tokadd(c);
                return 0;
            case 'c':
                tokadd('\\');
                tokadd('c');
                //escaped:
                if ((c = nextc()) == '\\') {
                    return tokadd_escape(term);
                    /*
                     *  FIX 1.6.5
                     */
                } else if (c == -1) {
                    // goto eof;
                    yyerror("Invalid escape character syntax");
                    return -1;
                    // goto eof; end
                }
                tokadd(c);
                return 0;
            case -1:
                // eof:
                yyerror("Invalid escape character syntax");
                return -1;
            default:
                if (c != term) {
                    /*
                     *  FIX 1.6.5
                     */
                    tokadd('\\');
                }
                /*
                 *  FIX 1.6.5
                 */
                tokadd(c);
        }
        return 0;
    }

    private int parse_regx(int term, int paren) {
        int c;
        char kcode = 0;
        boolean once = false;
        int nest = 0;
        int options = 0;
        int re_start = ruby.getSourceLine();
        NODE list = null;

        newtok();
        regx_end :
        while ((c = nextc()) != -1) {
            if (c == term && nest == 0) {
                break regx_end;
            }

            switch (c) {
                case '#':
                    list = str_extend(list, term);
                    if (list == NODE.MINUS_ONE) {
                        return 0;
                    }
                    continue;
                case '\\':
                    if (tokadd_escape(term) < 0) {
                        /*
                         *  FIX 1.6.5
                         */
                        return 0;
                    }
                    continue;
                case -1:
                    //goto unterminated;
                    ruby.setSourceLine(re_start);
                    ph.rb_compile_error("unterminated regexp meets end of file");
                    return 0;
                default:
                    if (paren != 0) {
                        if (c == paren) {
                            nest++;
                        }
                        if (c == term) {
                            nest--;
                        }
                    }
                    /*
                     *  if (ismbchar(c)) {
                     *  int i, len = mbclen(c)-1;
                     *  for (i = 0; i < len; i++) {
                     *  tokadd(c);
                     *  c = nextc();
                     *  }
                     *  }
                     */
                    break;
            }
            tokadd(c);
        }

        end_options :
        for (; ; ) {
            switch (c = nextc()) {
                case 'i':
                    options |= ReOptions.RE_OPTION_IGNORECASE;
                    break;
                case 'x':
                    options |= ReOptions.RE_OPTION_EXTENDED;
                    break;
                case 'p': // /p is obsolete
                    ph.rb_warn("/p option is obsolete; use /m\n\tnote: /m does not change ^, $ behavior");
                    options |= ReOptions.RE_OPTION_POSIXLINE;
                    break;
                case 'm':
                    options |= ReOptions.RE_OPTION_MULTILINE;
                    break;
                case 'o':
                    once = true;
                    break;
                case 'n':
                    kcode = 16;
                    break;
                case 'e':
                    kcode = 32;
                    break;
                case 's':
                    kcode = 48;
                    break;
                case 'u':
                    kcode = 64;
                    break;
                default:
                    pushback(c);
                    break end_options;
            }
        }

        tokfix();
        ph.setLexState(LexState.EXPR_END);
        if (list != null) {
            list.nd_set_line(re_start);
            if (toklen() > 0) {
                RubyObject ss = RubyString.m_newString(ruby, tok(), toklen());
                ph.list_append(list, nf.newStr(ss));
            }
            list.nd_set_type(once ? NODE.NODE_DREGX_ONCE : NODE.NODE_DREGX);
            list.nd_cflag(ph.newId(options | kcode));
            yyVal = (NODE)list;
            return Token.tDREGEXP;
        } else {
            yyVal = rb_reg_new(tok(), toklen(), options | kcode);
            return Token.tREGEXP;
        }
        //unterminated:
        //ruby_sourceline = re_start;
        //rb_compile_error("unterminated regexp meets end of file");
        //return 0;
    }

    private int parse_string(int func, int term, int paren) {
        int c;
        NODE list = null;
        int strstart;
        int nest = 0;

        if (func == '\'') {
            return parse_qstring(term, paren);
        }
        if (func == 0) {
            // read 1 line for heredoc
            // -1 for chomp
            yyVal = RubyString.m_newString(ruby, lex_curline, lex_pend - 1);
            lex_p = lex_pend;
            return Token.tSTRING;
        }
        strstart = ruby.getSourceLine();
        newtok();
        while ((c = nextc()) != term || nest > 0) {
            if (c == -1) {
                //unterm_str:
                ruby.setSourceLine(strstart);
                ph.rb_compile_error("unterminated string meets end of file");
                return 0;
            }
            /*
             *  if (ismbchar(c)) {
             *  int i, len = mbclen(c)-1;
             *  for (i = 0; i < len; i++) {
             *  tokadd(c);
             *  c = nextc();
             *  }
             *  }
             *  else
             */
                    if (c == '#') {
                list = str_extend(list, term);
                if (list == NODE.MINUS_ONE) {
                    //goto unterm_str;
                    ruby.setSourceLine(strstart);
                    ph.rb_compile_error("unterminated string meets end of file");
                    return 0;
                }
                continue;
            } else if (c == '\\') {
                c = nextc();
                if (c == '\n') {
                    continue;
                }
                if (c == term) {
                    tokadd(c);
                } else {
                    pushback(c);
                    if (func != '"') {
                        tokadd('\\');
                    }
                    tokadd(read_escape());
                }
                continue;
            }
            if (paren != 0) {
                if (c == paren) {
                    nest++;
                }
                if (c == term && nest-- == 0) {
                    break;
                }
            }
            tokadd(c);
        }

        tokfix();
        ph.setLexState(LexState.EXPR_END);

        if (list != null) {
            list.nd_set_line(strstart);
            if (toklen() > 0) {
                RubyObject ss = RubyString.m_newString(ruby, tok(), toklen());
                ph.list_append(list, nf.newStr(ss));
            }
            yyVal = (NODE) list;
            if (func == '`') {
                list.nd_set_type(NODE.NODE_DXSTR);
                return Token.tDXSTRING;
            } else {
                return Token.tDSTRING;
            }
        } else {
            yyVal = RubyString.m_newString(ruby, tok(), toklen());
            return (func == '`') ? Token.tXSTRING : Token.tSTRING;
        }
    }

    private int parse_qstring(int term, int paren) {
        int strstart;
        int c;
        int nest = 0;

        strstart = ruby.getSourceLine();
        newtok();
        while ((c = nextc()) != term || nest > 0) {
            if (c == -1) {
                ruby.setSourceLine(strstart);
                ph.rb_compile_error("unterminated string meets end of file");
                return 0;
            }
            /*
             *  if (ismbchar(c)) {
             *  int i, len = mbclen(c)-1;
             *  for (i = 0; i < len; i++) {
             *  tokadd(c);
             *  c = nextc();
             *  }
             *  }
             *  else
             */
                    if (c == '\\') {
                c = nextc();
                switch (c) {
                    case '\n':
                        continue;
                    case '\\':
                        c = '\\';
                        break;
                    default:
                        // fall through
                        if (c == term || (paren != 0 && c == paren)) {
                            tokadd(c);
                            continue;
                        }
                        tokadd('\\');
                }
            }
            if (paren != 0) {
                if (c == paren) {
                    nest++;
                }
                if (c == term && nest-- == 0) {
                    break;
                }
            }
            tokadd(c);
        }

        tokfix();
        yyVal = RubyString.m_newString(ruby, tok(), toklen());
        ph.setLexState(LexState.EXPR_END);
        return Token.tSTRING;
    }

    private int parse_quotedwords(int term, int paren) {
        NODE qwords = null;
        int strstart;
        int c;
        int nest = 0;

        strstart = ruby.getSourceLine();
        newtok();

        c = nextc();
        while (ISSPACE(c)) {
            c = nextc();
        }
        /*
         *  skip preceding spaces
         */
        pushback(c);
        while ((c = nextc()) != term || nest > 0) {
            if (c == -1) {
                ruby.setSourceLine(strstart);
                ph.rb_compile_error("unterminated string meets end of file");
                return 0;
            }
            /*
             *  if (ismbchar(c)) {
             *  int i, len = mbclen(c)-1;
             *  for (i = 0; i < len; i++) {
             *  tokadd(c);
             *  c = nextc();
             *  }
             *  }
             *  else
             */
                    if (c == '\\') {
                c = nextc();
                switch (c) {
                    case '\n':
                        continue;
                    case '\\':
                        c = '\\';
                        break;
                    default:
                        if (c == term || (paren != 0 && c == paren)) {
                            tokadd(c);
                            continue;
                        }
                        if (!ISSPACE(c)) {
                            tokadd('\\');
                        }
                        break;
                }
            } else if (ISSPACE(c)) {
                NODE str;

                tokfix();
                str = nf.newStr(RubyString.m_newString(ruby, tok(), toklen()));
                newtok();
                if (qwords == null) {
                    qwords = nf.newList(str);
                } else {
                    ph.list_append(qwords, str);
                }
                c = nextc();
                while (ISSPACE(c)) {
                    c = nextc();
                }
                // skip continuous spaces
                pushback(c);
                continue;
            }
            if (paren != 0) {
                if (c == paren) {
                    nest++;
                }
                if (c == term && nest-- == 0) {
                    break;
                }
            }
            tokadd(c);
        }

        tokfix();
        if (toklen() > 0) {
            NODE str = nf.newStr(RubyString.m_newString(ruby, tok(), toklen()));
            if (qwords == null) {
                qwords = nf.newList(str);
            } else {
                ph.list_append(qwords, str);
            }
        }
        if (qwords == null) {
            qwords = nf.newZArray();
        }
        yyVal = (NODE) qwords;
        ph.setLexState(LexState.EXPR_END);
        return Token.tDSTRING;
    }

    private int here_document(int term, int indent) {
        throw new Error("not supported yet");
    }

    /*
     *  private int here_document(int term, int indent) {
     *  int c;
     *  /char *eos, *p;
     *  int len;
     *  VALUE str;
     *  VALUE line = 0;
     *  VALUE lastline_save;
     *  int offset_save;
     *  NODE *list = 0;
     *  int linesave = ruby_sourceline;
     *  newtok();
     *  switch (term) {
     *  case '\'':
     *  case '"':
     *  case '`':
     *  while ((c = nextc()) != term) {
     *  tokadd(c);
     *  }
     *  if (term == '\'') term = 0;
     *  break;
     *  default:
     *  c = term;
     *  term = '"';
     *  if (!is_identchar(c)) {
     *  rb_warn("use of bare << to mean <<\"\" is deprecated");
     *  break;
     *  }
     *  while (is_identchar(c)) {
     *  tokadd(c);
     *  c = nextc();
     *  }
     *  pushback(c);
     *  break;
     *  }
     *  tokfix();
     *  lastline_save = lex_lastline;
     *  offset_save = lex_p - lex_pbeg;
     *  eos = strdup(tok());
     *  len = strlen(eos);
     *  str = rb_str_new(0,0);
     *  for (;;) {
     *  lex_lastline = line = lex_getline();
     *  if (NIL_P(line)) {
     *  /error:
     *  ruby_sourceline = linesave;
     *  rb_compile_error("can't find string \"%s\" anywhere before EOF", eos);
     *  free(eos);
     *  return 0;
     *  }
     *  ruby_sourceline++;
     *  p = RSTRING(line).ptr;
     *  if (indent) {
     *  while (*p && (*p == ' ' || *p == '\t')) {
     *  p++;
     *  }
     *  }
     *  if (strncmp(eos, p, len) == 0) {
     *  if (p[len] == '\n' || p[len] == '\r')
     *  break;
     *  if (len == RSTRING(line).len)
     *  break;
     *  }
     *  lex_pbeg = lex_p = RSTRING(line).ptr;
     *  lex_pend = lex_p + RSTRING(line).len;
     *  retry:for(;;) {
     *  switch (parse_string(term, '\n', '\n')) {
     *  case tSTRING:
     *  case tXSTRING:
     *  rb_str_cat2((VALUE)yyVal, "\n");
     *  if (!list) {
     *  rb_str_append(str, (VALUE)yyVal);
     *  }
     *  else {
     *  list_append(list, NEW_STR((VALUE)yyVal));
     *  }
     *  break;
     *  case tDSTRING:
     *  if (!list) list = NEW_DSTR(str);
     *  / fall through
     *  case tDXSTRING:
     *  if (!list) list = NEW_DXSTR(str);
     *  list_append((NODE)yyVal, NEW_STR(rb_str_new2("\n")));
     *  nd_set_type((NODE)yyVal, NODE_STR);
     *  yyVal = (NODE)NEW_LIST((NODE)yyVal);
     *  ((NODE)yyVal).nd_next(((NODE)yyVal).nd_head().nd_next());
     *  list_concat(list, (NODE)yyVal);
     *  break;
     *  case 0:
     *  ruby_sourceline = linesave;
     *  rb_compile_error("can't find string \"%s\" anywhere before EOF", eos);
     *  free(eos);
     *  return 0;
     *  }
     *  if (lex_p != lex_pend) {
     *  continue retry;
     *  }
     *  break retry;}
     *  }
     *  free(eos);
     *  lex_lastline = lastline_save;
     *  lex_pbeg = RSTRING(lex_lastline).ptr;
     *  lex_pend = lex_pbeg + RSTRING(lex_lastline).len;
     *  lex_p = lex_pbeg + offset_save;
     *  lex_state = EXPR_END;
     *  heredoc_end = ruby_sourceline;
     *  ruby_sourceline = linesave;
     *  if (list) {
     *  nd_set_line(list, linesave+1);
     *  yyVal = (NODE)list;
     *  }
     *  switch (term) {
     *  case '\0':
     *  case '\'':
     *  case '"':
     *  if (list) return tDSTRING;
     *  yyVal = (VALUE)str;
     *  return tSTRING;
     *  case '`':
     *  if (list) return tDXSTRING;
     *  yyVal = (VALUE)str;
     *  return tXSTRING;
     *  }
     *  return 0;
     *  }
     */

    private void arg_ambiguous() {
        ph.rb_warning("ambiguous first argument; make sure");
    }


    private boolean IS_ARG() {
        return ph.getLexState() == LexState.EXPR_ARG;
    }


    /**
     *  Returns the next token. Also sets yyVal is needed.
     *
     *@return    Description of the Returned Value
     */
    private int yylex() {
        int c;
        int space_seen = 0;
        // boolean cmd_state;
        kwtable kw;

        // cmd_state = ph.isCommandStart();
        // ph.setCommandStart(false);
        retry :
        for (; ; ) {
            switch (c = nextc()) {
                case '\0': // NUL
                case '\004': // ^D
                case '\032': // ^Z
                case -1: //end of script.
                    return 0;
                // white spaces
                case ' ':
                case '\t':
                case '\f':
                case '\r':
                case '\013': // '\v'
                    space_seen++;
                    continue retry;
                case '#': // it's a comment
                    while ((c = nextc()) != '\n') {
                        if (c == -1) {
                            return 0;
                        }
                    }
                    // fall through
                case '\n':
                    switch (ph.getLexState()) {
                        case LexState.EXPR_BEG:
                        case LexState.EXPR_FNAME:
                        case LexState.EXPR_DOT:
                            continue retry;
                        default:
                            break;
                    }
                    // ph.setCommandStart(true);
                    ph.setLexState(LexState.EXPR_BEG);
                    return '\n';
                case '*':
                    if ((c = nextc()) == '*') {
                        ph.setLexState(LexState.EXPR_BEG);
                        if (nextc() == '=') {
                            yyVal = ph.newId(Token.tPOW);
                            return Token.tOP_ASGN;
                        }
                        pushback(c);
                        return Token.tPOW;
                    }
                    if (c == '=') {
                        yyVal = ph.newId('*');
                        ph.setLexState(LexState.EXPR_BEG);
                        return Token.tOP_ASGN;
                    }
                    pushback(c);
                    if (IS_ARG() && space_seen != 0 && !ISSPACE(c)) {
                        ph.rb_warning("`*' interpreted as argument prefix");
                        c = Token.tSTAR;
                    } else if (ph.getLexState() == LexState.EXPR_BEG || 
                               ph.getLexState() == LexState.EXPR_MID) {
                        c = Token.tSTAR;
                    } else {
                        c = '*';
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    return c;
                case '!':
                    ph.setLexState(LexState.EXPR_BEG);
                    if ((c = nextc()) == '=') {
                        return Token.tNEQ;
                    }
                    if (c == '~') {
                        return Token.tNMATCH;
                    }
                    pushback(c);
                    return '!';
                case '=':
                    if (lex_p == 1) {
                        // skip embedded rd document
                        if (lex_curline.startsWith("=begin") && (lex_pend == 6 || ISSPACE(lex_curline.charAt(6)))) {
                            for (; ; ) {
                                lex_p = lex_pend;
                                c = nextc();
                                if (c == -1) {
                                    ph.rb_compile_error("embedded document meets end of file");
                                    return 0;
                                }
                                if (c != '=') {
                                    continue;
                                }
                                if (lex_curline.substring(lex_p, lex_p + 3).equals("end") &&
                                        (lex_p + 3 == lex_pend || ISSPACE(lex_curline.charAt(lex_p + 3)))) {
                                    break;
                                }
                            }
                            lex_p = lex_pend;
                            continue retry;
                        }
                    }

                    ph.setLexState(LexState.EXPR_BEG);
                    if ((c = nextc()) == '=') {
                        if ((c = nextc()) == '=') {
                            return Token.tEQQ;
                        }
                        pushback(c);
                        return Token.tEQ;
                    }
                    if (c == '~') {
                        return Token.tMATCH;
                    } else if (c == '>') {
                        return Token.tASSOC;
                    }
                    pushback(c);
                    return '=';
                case '<':
                    c = nextc();
                    if (c == '<' && 
                            ph.getLexState() != LexState.EXPR_END &&
                            ph.getLexState() != LexState.EXPR_ENDARG &&
                            ph.getLexState() != LexState.EXPR_CLASS &&
                            (!IS_ARG() || space_seen != 0)) {
                        int c2 = nextc();
                        int indent = 0;
                        if (c2 == '-') {
                            indent = 1;
                            c2 = nextc();
                        }
                        if (!ISSPACE(c2) && ("\"'`".indexOf(c2) != -1 || is_identchar(c2))) {
                            return here_document(c2, indent);
                        }
                        pushback(c2);
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    if (c == '=') {
                        if ((c = nextc()) == '>') {
                            return Token.tCMP;
                        }
                        pushback(c);
                        return Token.tLEQ;
                    }
                    if (c == '<') {
                        if (nextc() == '=') {
                            yyVal = ph.newId(Token.tLSHFT);
                            return Token.tOP_ASGN;
                        }
                        pushback(c);
                        return Token.tLSHFT;
                    }
                    pushback(c);
                    return '<';
                case '>':
                    ph.setLexState(LexState.EXPR_BEG);
                    if ((c = nextc()) == '=') {
                        return Token.tGEQ;
                    }
                    if (c == '>') {
                        if ((c = nextc()) == '=') {
                            yyVal = ph.newId(Token.tRSHFT);
                            return Token.tOP_ASGN;
                        }
                        pushback(c);
                        return Token.tRSHFT;
                    }
                    pushback(c);
                    return '>';
                case '"':
                    return parse_string(c, c, c);
                case '`':
                    if (ph.getLexState() == LexState.EXPR_FNAME) {
                        return c;
                    }
                    if (ph.getLexState() == LexState.EXPR_DOT) {
                        return c;
                    }
                    return parse_string(c, c, c);
                case '\'':
                    return parse_qstring(c, 0);
                case '?':
                    if (ph.getLexState() == LexState.EXPR_END) {
                        ph.setLexState(LexState.EXPR_BEG);
                        return '?';
                    }
                    c = nextc();
                    if (c == -1) { /* FIX 1.6.5 */
                        ph.rb_compile_error("incomplete character syntax");
                        return 0;
                    }
                    if (IS_ARG() && ISSPACE(c)) {
                        pushback(c);
                        ph.setLexState(LexState.EXPR_BEG);
                        return '?';
                    }
                    if (c == '\\') {
                        c = read_escape();
                    }
                    c &= 0xff;
                    yyVal = RubyFixnum.m_newFixnum(ruby, c);
                    ph.setLexState(LexState.EXPR_END);
                    return Token.tINTEGER;
                case '&':
                    if ((c = nextc()) == '&') {
                        ph.setLexState(LexState.EXPR_BEG);
                        if ((c = nextc()) == '=') {
                            yyVal = ph.newId(Token.tANDOP);
                            return Token.tOP_ASGN;
                        }
                        pushback(c);
                        return Token.tANDOP;
                    } else if (c == '=') {
                        yyVal = ph.newId('&');
                        ph.setLexState(LexState.EXPR_BEG);
                        return Token.tOP_ASGN;
                    }
                    pushback(c);
                    if (IS_ARG() && space_seen != 0 && !ISSPACE(c)) {
                        ph.rb_warning("`&' interpeted as argument prefix");
                        c = Token.tAMPER;
                    } else if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID) {
                        c = Token.tAMPER;
                    } else {
                        c = '&';
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    return c;
                case '|':
                    ph.setLexState(LexState.EXPR_BEG);
                    if ((c = nextc()) == '|') {
                        if ((c = nextc()) == '=') {
                            yyVal = ph.newId(Token.tOROP);
                            return Token.tOP_ASGN;
                        }
                        pushback(c);
                        return Token.tOROP;
                    } else if (c == '=') {
                        yyVal = ph.newId('|');
                        return Token.tOP_ASGN;
                    }
                    pushback(c);
                    return '|';
                case '+':
                    c = nextc();
                    if (ph.getLexState() == LexState.EXPR_FNAME || 
                        ph.getLexState() == LexState.EXPR_DOT) {
                        if (c == '@') {
                            return Token.tUPLUS;
                        }
                        pushback(c);
                        return '+';
                    }
                    if (c == '=') {
                        ph.setLexState(LexState.EXPR_BEG);
                        yyVal = ph.newId('+');
                        return Token.tOP_ASGN;
                    }
                    if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID ||
                            (IS_ARG() && space_seen != 0 && !ISSPACE(c))) {
                        if (IS_ARG()) {
                            arg_ambiguous();
                        }
                        ph.setLexState(LexState.EXPR_BEG);
                        pushback(c);
                        if (Character.isDigit((char) c)) {
                            c = '+';
                            return start_num(c);
                        }
                        return Token.tUPLUS;
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    pushback(c);
                    return '+';
                case '-':
                    c = nextc();
                    if (ph.getLexState() == LexState.EXPR_FNAME || ph.getLexState() == LexState.EXPR_DOT) {
                        if (c == '@') {
                            return Token.tUMINUS;
                        }
                        pushback(c);
                        return '-';
                    }
                    if (c == '=') {
                        ph.setLexState(LexState.EXPR_BEG);
                        yyVal = ph.newId('-');
                        return Token.tOP_ASGN;
                    }
                    if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID ||
                            (IS_ARG() && space_seen != 0 && !ISSPACE(c))) {
                        if (IS_ARG()) {
                            arg_ambiguous();
                        }
                        ph.setLexState(LexState.EXPR_BEG);
                        pushback(c);
                        if (Character.isDigit((char) c)) {
                            c = '-';
                            return start_num(c);
                        }
                        return Token.tUMINUS;
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    pushback(c);
                    return '-';
                case '.':
                    ph.setLexState(LexState.EXPR_BEG);
                    if ((c = nextc()) == '.') {
                        if ((c = nextc()) == '.') {
                            return Token.tDOT3;
                        }
                        pushback(c);
                        return Token.tDOT2;
                    }
                    pushback(c);
                    if (!Character.isDigit((char) c)) {
                        ph.setLexState(LexState.EXPR_DOT);
                        return '.';
                    }
                    c = '.';
                // fall through

                //start_num:
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    return start_num(c);
                case ']':
                case '}':
                    ph.setLexState(LexState.EXPR_END);
                    return c;
                case ')':
                    if (cond_nest > 0) {
	                    cond_stack >>= 1;
	                }
                    ph.setLexState(LexState.EXPR_END);
                    return c;
                case ':':
                    c = nextc();
                    if (c == ':') {
                        if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID ||
                                (IS_ARG() && space_seen != 0)) {
                            ph.setLexState(LexState.EXPR_BEG);
                            return Token.tCOLON3;
                        }
                        ph.setLexState(LexState.EXPR_DOT);
                        return Token.tCOLON2;
                    }
                    pushback(c);
                    if (ph.getLexState() == LexState.EXPR_END || ISSPACE(c)) {
                        ph.setLexState(LexState.EXPR_BEG);
                        return ':';
                    }
                    ph.setLexState(LexState.EXPR_FNAME);
                    return Token.tSYMBEG;
                case '/':
                    if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID) {
                        return parse_regx('/', '/');
                    }
                    if ((c = nextc()) == '=') {
                        ph.setLexState(LexState.EXPR_BEG);
                        yyVal = ph.newId('/');
                        return Token.tOP_ASGN;
                    }
                    pushback(c);
                    if (IS_ARG() && space_seen != 0) {
                        if (!ISSPACE(c)) {
                            arg_ambiguous();
                            return parse_regx('/', '/');
                        }
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    return '/';
                case '^':
                    ph.setLexState(LexState.EXPR_BEG);
                    if ((c = nextc()) == '=') {
                        yyVal = ph.newId('^');
                        return Token.tOP_ASGN;
                    }
                    pushback(c);
                    return '^';
                case ',':
                case ';':
                    // ph.setCommandStart(true);
                    ph.setLexState(LexState.EXPR_BEG);
                    return c;
                case '~':
                    if (ph.getLexState() == LexState.EXPR_FNAME || ph.getLexState() == LexState.EXPR_DOT) {
                        if ((c = nextc()) != '@') {
                            pushback(c);
                        }
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    return '~';
                case '(':
                    if (cond_nest > 0) {
	                    cond_stack = (cond_stack << 1 ) | 0;
	                }
                    // ph.setCommandStart(true);
                    if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID) {
                        c = Token.tLPAREN;
                    } else if (ph.getLexState() == LexState.EXPR_ARG && space_seen != 0) {
                        ph.rb_warning(tok() + " (...) interpreted as method call");
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    return c;
                case '[':
                    if (ph.getLexState() == LexState.EXPR_FNAME || ph.getLexState() == LexState.EXPR_DOT) {
                        if ((c = nextc()) == ']') {
                            if ((c = nextc()) == '=') {
                                return Token.tASET;
                            }
                            pushback(c);
                            return Token.tAREF;
                        }
                        pushback(c);
                        return '[';
                    } else if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID) {
                        c = Token.tLBRACK;
                    } else if (IS_ARG() && space_seen != 0) {
                        c = Token.tLBRACK;
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    return c;
                case '{':
                    if (ph.getLexState() != LexState.EXPR_END &&
                        ph.getLexState() != LexState.EXPR_ARG) {
                        
                        c = Token.tLBRACE;
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    return c;
                case '\\':
                    c = nextc();
                    if (c == '\n') {
                        space_seen = 1;
                        continue retry; // skip \\n
                    }
                    pushback(c);
                    return '\\';
                case '%':
                    quotation :
                    for (; ; ) {
                        if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID) {
                            int term;
                            int paren;

                            c = nextc();
                            if (!Character.isLetterOrDigit((char) c)) {
                                term = c;
                                c = 'Q';
                            } else {
                                term = nextc();
                            }
                            if (c == -1 || term == -1) {
                                ph.rb_compile_error("unterminated quoted string meets end of file");
                                return 0;
                            }
                            paren = term;
                            if (term == '(') {
                                term = ')';
                            } else if (term == '[') {
                                term = ']';
                            } else if (term == '{') {
                                term = '}';
                            } else if (term == '<') {
                                term = '>';
                            } else {
                                paren = 0;
                            }

                            switch (c) {
                                case 'Q':
                                    return parse_string('"', term, paren);
                                case 'q':
                                    return parse_qstring(term, paren);
                                case 'w':
                                    return parse_quotedwords(term, paren);
                                case 'x':
                                    return parse_string('`', term, paren);
                                case 'r':
                                    return parse_regx(term, paren);
                                default:
                                    yyerror("unknown type of %string");
                                    return 0;
                            }
                        }
                        if ((c = nextc()) == '=') {
                            yyVal = ph.newId('%');
                            return Token.tOP_ASGN;
                        }
                        if (IS_ARG() && space_seen != 0 && !ISSPACE(c)) {
                            pushback(c);
                            continue quotation;
                        }
                        break quotation;
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    pushback(c);
                    return '%';
                case '$':
                    ph.setLexState(LexState.EXPR_END);
                    newtok();
                    c = nextc();
                    switch (c) {
                        case '_': // $_: last read line string
                            c = nextc();
                            if (is_identchar(c)) {
                                tokadd('$');
                                tokadd('_');
                                break;
                            }
                            pushback(c);
                            c = '_';
                        // fall through
                        case '~': // $~: match-data
                            ph.local_cnt(c);
                        // fall through
                        case '*': // $*: argv
                        case '$': // $$: pid
                        case '?': // $?: last status
                        case '!': // $!: error string
                        case '@': // $@: error position
                        case '/': // $/: input record separator
                        case '\\':// $\: output record separator
                        case ';': // $;: field separator
                        case ',': // $,: output field separator
                        case '.': // $.: last read line number
                        case '=': // $=: ignorecase
                        case ':': // $:: load path
                        case '<': // $<: reading filename
                        case '>': // $>: default output handle
                        case '\"':// $": already loaded files
                            tokadd('$');
                            tokadd(c);
                            tokfix();
                            yyVal = ruby.intern(tok());
                            return Token.tGVAR;
                        case '-':
                            tokadd('$');
                            tokadd(c);
                            c = nextc();
                            tokadd(c);
                            tokfix();
                            yyVal = ruby.intern(tok());
                            /* xxx shouldn't check if valid option variable */
                            return Token.tGVAR;
                        case '&':   // $&: last match
                        case '`':   // $`: string before last match
                        case '\'':  // $': string after last match
                        case '+':   // $+: string matches last paren.
                            yyVal = nf.newBackRef(c);
                            return Token.tBACK_REF;
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            tokadd('$');
                            while (Character.isDigit((char) c)) {
                                tokadd(c);
                                c = nextc();
                            }
                            if (is_identchar(c)) {
                                break;
                            }
                            pushback(c);
                            tokfix();
                            yyVal = nf.newNthRef(Integer.parseInt(tok().substring(1)));
                            return Token.tNTH_REF;
                        default:
                            if (!is_identchar(c)) {
                                pushback(c);
                                return '$';
                            }
                        case '0':
                            tokadd('$');
                    }
                    break;
                case '@':
                    c = nextc();
                    newtok();
                    tokadd('@');
                    if (c == '@') {
                        tokadd('@');
                        c = nextc();
                    }
                    if (Character.isDigit((char) c)) {
                        ph.rb_compile_error("`@" + c + "' is not a valid instance variable name");
                    }
                    if (!is_identchar(c)) {
                        pushback(c);
                        return '@';
                    }
                    break;
                default:
                    if (!is_identchar(c) || Character.isDigit((char) c)) {
                        ph.rb_compile_error("Invalid char `\\" + c + "' in expression");
                        continue retry;
                    }

                    newtok();
                    break;
            }
            break retry;
        }

        while (is_identchar(c)) {
            tokadd(c);
            /*
             *  if (ismbchar(c)) {
             *  int i, len = mbclen(c)-1;
             *  for (i = 0; i < len; i++) {
             *  c = nextc();
             *  tokadd(c);
             *  }
             *  }
             */
            c = nextc();
        }
        if ((c == '!' || c == '?') && is_identchar(tok().charAt(0)) && !peek('=')) {
            tokadd(c);
        } else {
            pushback(c);
        }
        tokfix();
         {
            int result = 0;

            switch (tok().charAt(0)) {
                case '$':
                    ph.setLexState(LexState.EXPR_END);
                    result = Token.tGVAR;
                    break;
                case '@':
                    ph.setLexState(LexState.EXPR_END);
                    if (tok().charAt(1) == '@') {
                        result = Token.tCVAR;
                    } else {
                        result = Token.tIVAR;
                    }
                    break;
                default:
                    if (ph.getLexState() != LexState.EXPR_DOT) {
                        // See if it is a reserved word.
                        kw = rb_reserved_word(tok(), toklen());
                        if (kw != null) {
                            // enum lex_state
                            int state = ph.getLexState();
                            ph.setLexState(kw.state);
                            if (state == LexState.EXPR_FNAME) {
                                yyVal = ruby.intern(kw.name);
                            }
                            if (kw.id0 == Token.kDO) {
                                if (COND_P()) {
                                    return Token.kDO_COND;
                                }
                                if (CMDARG_P()) {
                                    return Token.kDO_BLOCK;
                                }
                                return Token.kDO;
                            }
                            if (state == LexState.EXPR_BEG) {
                                return kw.id0;
                            } else {
                                if (kw.id0 != kw.id1) {
                                    ph.setLexState(LexState.EXPR_BEG);
                                }
                                return kw.id1;
                            }
                        }
                    }

                    if (toklast() == '!' || toklast() == '?') {
                        result = Token.tFID;
                    } else {
                        if (ph.getLexState() == LexState.EXPR_FNAME) {
                            if ((c = nextc()) == '=' && !peek('~') && !peek('>') &&
                                    (!peek('=') || lex_p + 1 < lex_pend && lex_curline.charAt(lex_p + 1) == '>')) {
                                result = Token.tIDENTIFIER;
                                tokadd(c);
                            } else {
                                pushback(c);
                            }
                        }
                        if (result == 0 && Character.isUpperCase(tok().charAt(0))) {
                            result = Token.tCONSTANT;
                        } else {
                            result = Token.tIDENTIFIER;
                        }
                    }
                    if (ph.getLexState() == LexState.EXPR_BEG ||
                        ph.getLexState() == LexState.EXPR_DOT ||
                        ph.getLexState() == LexState.EXPR_ARG) {
                            ph.setLexState(LexState.EXPR_ARG);
                    } else {
                        ph.setLexState(LexState.EXPR_END);
                    }
            }
            tokfix();
            
            yyVal = ruby.intern(tok());
            return result;
        }
    }


    /**
     *  Description of the Method
     *
     *@param  c  Description of Parameter
     *@return    Description of the Returned Value
     */
    private int start_num(int c) {
        boolean is_float;
        boolean seen_point;
        boolean seen_e;
        boolean seen_uc;

        is_float = seen_point = seen_e = seen_uc = false;
        ph.setLexState(LexState.EXPR_END);
        newtok();
        if (c == '-' || c == '+') {
            tokadd(c);
            c = nextc();
        }
        if (c == '0') {
            c = nextc();
            if (c == 'x' || c == 'X') {
                /*
                 *  hexadecimal
                 */
                c = nextc();
                do {
                    if (c == '_') {
                        seen_uc = true;
                        continue;
                    }
                    if (!ISXDIGIT(c)) {
                        break;
                    }
                    seen_uc = false;
                    tokadd(c);
                } while ((c = nextc()) != 0);
                pushback(c);
                tokfix();
                if (toklen() == 0) {
                    yyerror("hexadecimal number without hex-digits");
                } else if (seen_uc) {
                    return decode_num(c, is_float, seen_uc, true);
                }
                yyVal = rb_cstr2inum(tok(), 16);
                return Token.tINTEGER;
            }
            if (c == 'b' || c == 'B') {
                // binary
                c = nextc();
                do {
                    if (c == '_') {
                        seen_uc = true;
                        continue;
                    }
                    if (c != '0' && c != '1') {
                        break;
                    }
                    seen_uc = false;
                    tokadd(c);
                } while ((c = nextc()) != 0);
                pushback(c);
                tokfix();
                if (toklen() == 0) {
                    yyerror("numeric literal without digits");
                } else if (seen_uc) {
                    return decode_num(c, is_float, seen_uc, true);
                }
                yyVal = (VALUE) rb_cstr2inum(tok(), 2);
                return Token.tINTEGER;
            }
            if (c >= '0' && c <= '7' || c == '_') {
                // octal
                do {
                    if (c == '_') {
                        seen_uc = true;
                        continue;
                    }
                    if (c < '0' || c > '7') {
                        break;
                    }
                    seen_uc = false;
                    tokadd(c);
                } while ((c = nextc()) != 0);
                pushback(c);
                tokfix();
                if (seen_uc) {
                    return decode_num(c, is_float, seen_uc, true);
                }
                yyVal = (VALUE) rb_cstr2inum(tok(), 8);
                return Token.tINTEGER;
            }
            if (c > '7' && c <= '9') {
                yyerror("Illegal octal digit");
            } else if (c == '.') {
                tokadd('0');
            } else {
                pushback(c);
                yyVal = RubyFixnum.zero(ruby);
                return Token.tINTEGER;
            }
        }

        for (; ; ) {
            switch (c) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    seen_uc = false;
                    tokadd(c);
                    break;
                case '.':
                    if (seen_point || seen_e) {
                        return decode_num(c, is_float, seen_uc);
                    } else {
                        int c0 = nextc();
                        if (!Character.isDigit((char) c0)) {
                            pushback(c0);
                            return decode_num(c, is_float, seen_uc);
                        }
                        c = c0;
                    }
                    tokadd('.');
                    tokadd(c);
                    is_float = true;
                    seen_point = true;
                    seen_uc = false;
                    break;
                case 'e':
                case 'E':
                    if (seen_e) {
                        return decode_num(c, is_float, seen_uc);
                    }
                    tokadd(c);
                    seen_e = true;
                    is_float = true;
                    while ((c = nextc()) == '_') {
                        seen_uc = true;
                    }
                    if (c == '-' || c == '+') {
                        tokadd(c);
                    } else {
                        continue;
                    }
                    break;
                case '_':
                    /*
                     *  `_' in number just ignored
                     */
                    seen_uc = true;
                    break;
                default:
                    return decode_num(c, is_float, seen_uc);
            }
            c = nextc();
        }
    }


    /**
     *  Description of the Method
     *
     *@param  c         Description of Parameter
     *@param  is_float  Description of Parameter
     *@param  seen_uc   Description of Parameter
     *@return           Description of the Returned Value
     */
    private int decode_num(int c, boolean is_float, boolean seen_uc) {
        return decode_num(c, is_float, seen_uc, false);
    }


    /**
     *  Description of the Method
     *
     *@param  c            Description of Parameter
     *@param  is_float     Description of Parameter
     *@param  seen_uc      Description of Parameter
     *@param  trailing_uc  Description of Parameter
     *@return              Description of the Returned Value
     */
    private int decode_num(int c, boolean is_float, boolean seen_uc, boolean trailing_uc) {
        if (!trailing_uc) {
            pushback(c);
            tokfix();
        }
        if (seen_uc || trailing_uc) {
            //trailing_uc:
            yyerror("trailing `_' in number");
        }
        if (is_float) {
            double d = 0.0;
            try {
                d = Double.parseDouble(tok());
            } catch (NumberFormatException e) {
                ph.rb_warn("Float " + tok() + " out of range");
            }
            yyVal = RubyFloat.m_newFloat(ruby, d);
            return Token.tFLOAT;
        }
        yyVal = rb_cstr2inum(tok(), 10);
        return Token.tINTEGER;
    }


    /**
     *  Description of the Method
     *
     *@param  list  Description of Parameter
     *@param  term  Description of Parameter
     *@return       Description of the Returned Value
     */
    private NODE str_extend(NODE list, int term) {
        int c;
        int brace = -1;
        VALUE ss;
        NODE node;
        int nest;

        c = nextc();
        switch (c) {
            case '$':
            case '@':
            case '{':
                break;
            default:
                tokadd('#');
                pushback(c);
                return list;
        }

        ss = RubyString.m_newString(ruby, tok(), toklen());
        if (list == null) {
            list = nf.newDStr(ss);
        } else if (toklen() > 0) {
            ph.list_append(list, nf.newStr(ss));
        }
        newtok();

        fetch_id :
        for (; ; ) {
            switch (c) {
                case '$':
                    tokadd('$');
                    c = nextc();
                    if (c == -1) {
                        return NODE.MINUS_ONE;
                    }
                    switch (c) {
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            while (Character.isDigit((char) c)) {
                                tokadd(c);
                                c = nextc();
                            }
                            pushback(c);
                            break fetch_id;
                        case '&':
                        case '+':
                        case '_':
                        case '~':
                        case '*':
                        case '$':
                        case '?':
                        case '!':
                        case '@':
                        case ',':
                        case '.':
                        case '=':
                        case ':':
                        case '<':
                        case '>':
                        case '\\':
                            //refetch:
                            tokadd(c);
                            break fetch_id;
                        default:
                            if (c == term) {
                                ph.list_append(list, nf.newStr(RubyString.m_newString(ruby, "#$")));
                                pushback(c);
                                newtok();
                                return list;
                            }
                            switch (c) {
                                case '\"':
                                case '/':
                                case '\'':
                                case '`':
                                    //goto refetch;
                                    tokadd(c);
                                    break fetch_id;
                            }
                            if (!is_identchar(c)) {
                                yyerror("bad global variable in string");
                                newtok();
                                return list;
                            }
                    }

                    while (is_identchar(c)) {
                        tokadd(c);
                        /*
                         *  if (ismbchar(c)) {
                         *  int i, len = mbclen(c)-1;
                         *  for (i = 0; i < len; i++) {
                         *  c = nextc();
                         *  tokadd(c);
                         *  }
                         *  }
                         */
                        c = nextc();
                    }
                    pushback(c);
                    break;
                case '@':
                    tokadd(c);
                    c = nextc();
                    if (c == '@') {
                        tokadd(c);
                        c = nextc();
                    }
                    while (is_identchar(c)) {
                        tokadd(c);
                        /*
                         *  if (ismbchar(c)) {
                         *  int i, len = mbclen(c)-1;
                         *  for (i = 0; i < len; i++) {
                         *  c = nextc();
                         *  tokadd(c);
                         *  }
                         *  }
                         */
                        c = nextc();
                    }
                    pushback(c);
                    break;
                case '{':
                    if (c == '{') {
                        brace = '}';
                    }
                    nest = 0;
                    do {
                        loop_again :
                        for (; ; ) {
                            c = nextc();
                            switch (c) {
                                case -1:
                                    if (nest > 0) {
                                        yyerror("bad substitution in string");
                                        newtok();
                                        return list;
                                    }
                                    return NODE.MINUS_ONE;
                                case '}':
                                    if (c == brace) {
                                        if (nest == 0) {
                                            break;
                                        }
                                        nest--;
                                    }
                                    tokadd(c);
                                    continue loop_again;
                                case '\\':
                                    c = nextc();
                                    if (c == -1) {
                                        return NODE.MINUS_ONE;
                                    }
                                    if (c == term) {
                                        tokadd(c);
                                    } else {
                                        tokadd('\\');
                                        tokadd(c);
                                    }
                                    break;
                                case '{':
                                    if (brace != -1) {
                                        nest++;
                                    }
                                case '\"':
                                case '/':
                                case '`':
                                    if (c == term) {
                                        pushback(c);
                                        ph.list_append(list, nf.newStr(RubyString.m_newString(ruby, "#")));
                                        ph.rb_warning("bad substitution in string");
                                        tokfix();
                                        ph.list_append(list, nf.newStr(RubyString.m_newString(ruby, tok(), toklen())));
                                        newtok();
                                        return list;
                                    }
                                default:
                                    tokadd(c);
                                    break;
                            }
                            break loop_again;
                        }
                    } while (c != brace);
            }
            break;
        }

        //fetch_id:
        tokfix();
        node = nf.newEVStr(tok(), toklen());
        ph.list_append(list, node);
        newtok();

        return list;
    }
    // yylex


    // Helper functions....................

    //XXX private helpers, can be inlined

    /**
     *  Returns true if "c" is a white space character.
     *
     *@param  c  Description of Parameter
     *@return    Description of the Returned Value
     */
    private boolean ISSPACE(int c) {
        return Character.isWhitespace((char) c);
    }


    /**
     *  Returns true if "c" is a hex-digit.
     *
     *@param  c  Description of Parameter
     *@return    Description of the Returned Value
     */
    private boolean ISXDIGIT(int c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
    }


    /**
     *  Returns the value of a hex number with max "len" characters. Also
     *  returns the number of characters read. Please note the "x"-hack.
     *
     *@param  s       Description of Parameter
     *@param  start   Description of Parameter
     *@param  len     Description of Parameter
     *@param  retlen  Description of Parameter
     *@return         Description of the Returned Value
     */
    private long scan_hex(String s, int start, int len, int[] retlen) {
        String hexdigit = "0123456789abcdef0123456789ABCDEFx";
        long retval = 0;
        int tmp;
        int st = start;

        while (len-- != 0 && st < s.length() && (tmp = hexdigit.indexOf(s.charAt(st))) != -1) {
            retval <<= 4;
            retval |= tmp & 15;
            st++;
        }
        retlen[0] = st - start;
        return retval;
    }

    
    /** This function compiles a given String into a NODE.
     *
     */
    public NODE yycompile(String f, int line) {
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
        ph.setEvalTree(null);       // parser stores NODEs here
        ph.setHeredocEnd(0);
        ruby.setSourceFile(f);      // source file name
        ph.setRubyInCompile(true);

        try {
            yyparse(new yyInput() {
                public boolean advance() throws IOException {
                    return DefaultRubyParser.this.advance();
                }
                public int token() { 
                    return DefaultRubyParser.this.token();
                }
                public Object value() { 
                    return DefaultRubyParser.this.value();
                }
            }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ph.setRubyDebugLines(null); // remove debug info
        ph.setCompileForEval(0);
        ph.setRubyInCompile(false);
        cond_nest = 0;
        cond_stack = 0;             // reset stuff for next compile
        cmdarg_stack = 0;           // reset stuff for next compile
        ph.setClassNest(0);
        ph.setInSingle(0);
        ph.setInDef(0);
        ph.setCurMid(null);

        return ph.getEvalTree();
    }
}
