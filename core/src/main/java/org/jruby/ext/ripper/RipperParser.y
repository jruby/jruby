%{
/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2013-2017 The JRuby Team (jruby@jruby.org)
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

package org.jruby.ext.ripper;

import org.jruby.RubyArray;
import org.jruby.lexer.LexerSource;
import org.jruby.lexer.yacc.StackState;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.lexer.LexingCommon.EXPR_BEG;
import static org.jruby.lexer.LexingCommon.EXPR_FITEM;
import static org.jruby.lexer.LexingCommon.EXPR_FNAME;
import static org.jruby.lexer.LexingCommon.EXPR_ENDFN;
import static org.jruby.lexer.LexingCommon.EXPR_ENDARG;
import static org.jruby.lexer.LexingCommon.EXPR_END;
import static org.jruby.lexer.LexingCommon.EXPR_LABEL;

public class RipperParser extends RipperParserBase {
    public RipperParser(ThreadContext context, IRubyObject ripper, LexerSource source) {
        super(context, ripper, source);
    }
%}

%token <IRubyObject> keyword_class        /* {{class}} */
%token <IRubyObject> keyword_module       /* {{module}} */
%token <IRubyObject> keyword_def          /* {{def}} */
%token <IRubyObject> keyword_undef        /* {{undef}} */
%token <IRubyObject> keyword_begin        /* {{begin}} */
%token <IRubyObject> keyword_rescue       /* {{rescue}} */
%token <IRubyObject> keyword_ensure       /* {{ensure}} */
%token <IRubyObject> keyword_end          /* {{end}} */
%token <IRubyObject> keyword_if           /* {{if}} */
%token <IRubyObject> keyword_unless       /* {{unless}} */
%token <IRubyObject> keyword_then         /* {{then}} */
%token <IRubyObject> keyword_elsif        /* {{elsif}} */
%token <IRubyObject> keyword_else         /* {{else}} */
%token <IRubyObject> keyword_case         /* {{case}} */
%token <IRubyObject> keyword_when         /* {{when}} */
%token <IRubyObject> keyword_while        /* {{while}} */
%token <IRubyObject> keyword_until        /* {{until}} */
%token <IRubyObject> keyword_for          /* {{for}} */
%token <IRubyObject> keyword_break        /* {{break}} */
%token <IRubyObject> keyword_next         /* {{next}} */
%token <IRubyObject> keyword_redo         /* {{redo}} */
%token <IRubyObject> keyword_retry        /* {{retry}} */
%token <IRubyObject> keyword_in           /* {{in}} */
%token <IRubyObject> keyword_do           /* {{do}} */
%token <IRubyObject> keyword_do_cond      /* {{do (for condition)}} */
%token <IRubyObject> keyword_do_block     /* {{do (for block)}} */
%token <IRubyObject> keyword_return       /* {{return}} */
%token <IRubyObject> keyword_yield        /* {{yield}} */
%token <IRubyObject> keyword_super        /* {{super}} */
%token <IRubyObject> keyword_self         /* {{self}} */
%token <IRubyObject> keyword_nil          /* {{nil}} */
%token <IRubyObject> keyword_true         /* {{true}} */
%token <IRubyObject> keyword_false        /* {{false}} */
%token <IRubyObject> keyword_and          /* {{and}} */
%token <IRubyObject> keyword_or           /* {{or}} */
%token <IRubyObject> keyword_not          /* {{not}} */
%token <IRubyObject> modifier_if          /* {{if (modifier)}} */
%token <IRubyObject> modifier_unless      /* {{unless (modifier)}} */
%token <IRubyObject> modifier_while       /* {{while (modifier)}} */
%token <IRubyObject> modifier_until       /* {{until (modifier)}} */
%token <IRubyObject> modifier_rescue      /* {{rescue (modifier)}} */
%token <IRubyObject> keyword_alias        /* {{alias}} */
%token <IRubyObject> keyword_defined      /* {{defined}} */
%token <IRubyObject> keyword_BEGIN        /* {{BEGIN}} */
%token <IRubyObject> keyword_END          /* {{END}} */
%token <IRubyObject> keyword__LINE__      /* {{__LINE__}} */
%token <IRubyObject> keyword__FILE__      /* {{__FILE__}} */
%token <IRubyObject> keyword__ENCODING__  /* {{__ENCODING__}} */
%token <IRubyObject> keyword_do_lambda    /* {{do (for lambda)}} */
%token <IRubyObject> tIDENTIFIER         
%token <IRubyObject> tFID
%token <IRubyObject> tGVAR
%token <IRubyObject> tIVAR
%token <IRubyObject> tCONSTANT
%token <IRubyObject> tCVAR
%token <IRubyObject> tLABEL
%token <IRubyObject> tCHAR
%token <IRubyObject> tUPLUS               /* {{unary+}} */
%token <IRubyObject> tUMINUS              /* {{unary-}} */
%token <IRubyObject> tUMINUS_NUM
%token <IRubyObject> tPOW                 /* {{**}} */
%token <IRubyObject> tCMP                 /* {{<=>}} */
%token <IRubyObject> tEQ                  /* {{==}} */
%token <IRubyObject> tEQQ                 /* {{===}} */
%token <IRubyObject> tNEQ                 /* {{!=}} */
%token <IRubyObject> tGEQ                 /* {{>=}} */
%token <IRubyObject> tLEQ                 /* {{<=}} */
%token <IRubyObject> tANDOP               /* {{&&}}*/
%token <IRubyObject> tOROP                /* {{||}} */
%token <IRubyObject> tMATCH               /* {{=~}} */
%token <IRubyObject> tNMATCH              /* {{!~}} */
%token <IRubyObject> tDOT                 /* {{.}} -  '.' in ruby and not a token */
%token <IRubyObject> tDOT2                /* {{..}} */
%token <IRubyObject> tDOT3                /* {{...}} */
%token <IRubyObject> tAREF                /* {{[]}} */
%token <IRubyObject> tASET                /* {{[]=}} */
%token <IRubyObject> tLSHFT               /* {{<<}} */
%token <IRubyObject> tRSHFT               /* {{>>}} */
%token <String> tANDDOT                /* {{&.}} */
%token <IRubyObject> tCOLON2              /* {{::}} */
%token <IRubyObject> tCOLON3              /* {{:: at EXPR_BEG}} */
%token <IRubyObject> tOP_ASGN             /* +=, -=  etc. */
%token <IRubyObject> tASSOC               /* {{=>}} */
%token <IRubyObject> tLPAREN               /* {{(}} */
%token <IRubyObject> tLPAREN2              /* {{(}} - '(' in ruby and not a token */
%token <IRubyObject> tRPAREN              /* {{)}} */
%token <IRubyObject> tLPAREN_ARG           /* {{( arg}} */
%token <IRubyObject> tLBRACK              /* {{[}} */
%token <IRubyObject> tRBRACK              /* {{]}} */
%token <IRubyObject> tLBRACE               /* {{{}} */
%token <IRubyObject> tLBRACE_ARG           /* {{{ arg}} */
%token <IRubyObject> tSTAR                /* {{*}} */
%token <IRubyObject> tSTAR2               /* {{*}} - '*' in ruby and not a token */
%token <IRubyObject> tAMPER               /* {{&}} */
%token <IRubyObject> tAMPER2              /* {{&}} - '&' in ruby and not a token */
%token <IRubyObject> tTILDE               /* {{`}} - '`' in ruby and not a token */
%token <IRubyObject> tPERCENT             /* {{%}} - '%' in ruby and not a token */
%token <IRubyObject> tDIVIDE              /* {{/}} - '/' in ruby and not a token */
%token <IRubyObject> tPLUS                /* {{+}} - '+' in ruby and not a token */
%token <IRubyObject> tMINUS               /* {{-}} - '-' in ruby and not a token */
%token <IRubyObject> tLT                  /* {{<}} - '<' in ruby and not a token */
%token <IRubyObject> tGT                  /* {{>}} - '>' in ruby and not a token */
%token <IRubyObject> tPIPE                /* {{|}} - '|' in ruby and not a token */
%token <IRubyObject> tBANG                /* {{!}} - '!' in ruby and not a token */
%token <IRubyObject> tCARET               /* {{^}} - '^' in ruby and not a token */
%token <IRubyObject> tLCURLY               /* {{{}} - '{' in ruby and not a token */
%token <IRubyObject> tRCURLY              /* {{}}} - '}' in ruby and not a token */
%token <IRubyObject> tBACK_REF2           /* {{`}} - '`' in ruby and not a token */
%token <IRubyObject> tSYMBEG tSTRING_BEG tXSTRING_BEG tREGEXP_BEG tWORDS_BEG tQWORDS_BEG
%token <IRubyObject> tSTRING_DBEG tSTRING_DVAR tSTRING_END
%token <IRubyObject> tLAMBDA              /* {{->}} */
%token <IRubyObject> tLAMBEG
%token <IRubyObject> tNTH_REF tBACK_REF tSTRING_CONTENT tINTEGER tIMAGINARY
%token <IRubyObject> tFLOAT
%token <IRubyObject> tRATIONAL
%token <IRubyObject>  tREGEXP_END
   
%type <IRubyObject> sym symbol operation operation2 operation3 op fname cname 
%type <IRubyObject> f_norm_arg restarg_mark
%type <IRubyObject> dot_or_colon blkarg_mark
/* RIPPER-ONY TOKENS { */
%token <IRubyObject> tIGNORED_NL tCOMMENT tEMBDOC_BEG tEMBDOC tEMBDOC_END
%token <IRubyObject> tSP tHEREDOC_BEG tHEREDOC_END
/* } RIPPER-ONY TOKENS */
%type <IRubyObject> f_rest_arg 
%type <IRubyObject> singleton strings string string1 xstring regexp
%type <IRubyObject> string_contents xstring_contents method_call
%type <IRubyObject> string_content
%type <IRubyObject> regexp_contents
%type <IRubyObject> words qwords word literal dsym cpath command_asgn command_call
%type <IRubyObject> numeric simple_numeric
%type <IRubyObject> mrhs_arg
%type <IRubyObject> compstmt bodystmt stmts stmt expr arg primary command
%type <IRubyObject> stmt_or_begin
%type <IRubyObject> expr_value expr_value_do primary_value opt_else cases if_tail exc_var rel_expr
%type <IRubyObject> call_args opt_ensure paren_args superclass
%type <IRubyObject> command_args var_ref opt_paren_args block_call block_command
%type <IRubyObject> command_rhs arg_rhs
%type <IRubyObject> f_opt
%type <RubyArray> undef_list 
%type <IRubyObject> string_dvar backref
%type <IRubyObject> f_args f_larglist block_param block_param_def opt_block_param
%type <IRubyObject> f_arglist
%type <IRubyObject> mrhs mlhs_item mlhs_node arg_value case_body exc_list aref_args
%type <IRubyObject> lhs none args
%type <IRubyObject> qword_list word_list
%type <RubyArray> f_arg f_optarg
%type <IRubyObject> f_marg_list symbol_list
%type <IRubyObject> qsym_list symbols qsymbols
%type <ArgsTailHolder> opt_args_tail opt_block_args_tail block_args_tail args_tail
%type <IRubyObject> f_kw f_block_kw
%type <RubyArray> f_block_kwarg f_kwarg
%type <IRubyObject> assoc_list
%type <RubyArray> assocs
%type <IRubyObject> assoc 
%type <IRubyObject> mlhs_head mlhs_post 
%type <RubyArray> f_block_optarg
%type <IRubyObject> opt_block_arg block_arg none_block_pass
%type <IRubyObject> opt_f_block_arg f_block_arg
%type <IRubyObject> brace_block do_block cmd_brace_block brace_body do_body
%type <IRubyObject> mlhs mlhs_basic 
%type <IRubyObject> opt_rescue
%type <IRubyObject> var_lhs
%type <IRubyObject> fsym
%type <IRubyObject> fitem
%type <IRubyObject> f_arg_item
%type <RubyArray> bv_decls
%type <IRubyObject> opt_bv_decl lambda_body 
%type <IRubyObject> lambda
%type <IRubyObject> mlhs_inner f_block_opt for_var
%type <IRubyObject> opt_call_args f_marg f_margs
%type <IRubyObject> bvar
%type <IRubyObject> reswords f_bad_arg relop
%type <IRubyObject> rparen rbracket
%type <IRubyObject> top_compstmt top_stmts top_stmt
%token <IRubyObject> tSYMBOLS_BEG
%token <IRubyObject> tQSYMBOLS_BEG
%token <IRubyObject> tDSTAR
%token <IRubyObject> tSTRING_DEND
%type <IRubyObject> kwrest_mark f_kwrest f_label
%type <IRubyObject> call_op call_op2
%type <IRubyObject> f_arg_asgn
%type <IRubyObject> fcall
%token <IRubyObject> tLABEL_END

%type <IRubyObject> do then
%type <IRubyObject> program

/*
 *    precedence table
 */

%nonassoc tLOWEST
%nonassoc tLBRACE_ARG

%nonassoc  modifier_if modifier_unless modifier_while modifier_until
%left  keyword_or keyword_and
%right keyword_not
%nonassoc keyword_defined
%right '=' tOP_ASGN
%left modifier_rescue
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
%token <IRubyObject> k__END__

%%
program       : {
                  p.setState(EXPR_BEG);
                  p.pushLocalScope();
              } top_compstmt {
                  $$ = p.dispatch("on_program", $2);
                  p.popCurrentScope();
              }

top_compstmt  : top_stmts opt_terms {
                  $$ = $1;
              }

top_stmts     : none {
                  $$ = p.dispatch("on_stmts_add", p.dispatch("on_stmts_new"), p.dispatch("on_void_stmt"));
              }
              | top_stmt {
                  $$ = p.dispatch("on_stmts_add", p.dispatch("on_stmts_new"), $1);
              }
              | top_stmts terms top_stmt {
                  $$ = p.dispatch("on_stmts_add", $1, $3);
              }
              | error top_stmt {
                  $$ = $2;
              }

top_stmt      : stmt
              | keyword_BEGIN {
                  if (p.isInDef()) {
                      p.yyerror("BEGIN in method");
                  }
              } tLCURLY top_compstmt tRCURLY {
                  $$ = p.dispatch("on_BEGIN", $4);
              }

bodystmt      : compstmt opt_rescue opt_else opt_ensure {
                  $$ = p.dispatch("on_bodystmt", $1, $2, $3, $4);
              }

compstmt        : stmts opt_terms {
                    $$ = $1;
                }

stmts           : none {
                    $$ = p.dispatch("on_stmts_add", p.dispatch("on_stmts_new"), p.dispatch("on_void_stmt"));
                }
                | stmt_or_begin {
                    $$ = p.dispatch("on_stmts_add", p.dispatch("on_stmts_new"), $1);
                }
                | stmts terms stmt_or_begin {
                    $$ = p.dispatch("on_stmts_add", $1, $3);
                }
                | error stmt {
                    $$ = $2;
                }

stmt_or_begin   : stmt {
                    $$ = $1;
                }
// FIXME: How can this new begin ever work?  is yyerror conditional in MRI?
                | keyword_begin {
                    p.yyerror("BEGIN is permitted only at toplevel");
                } tLCURLY top_compstmt tRCURLY {
                    $$ = p.dispatch("on_BEGIN", $4);
                }

stmt            : keyword_alias fitem {
                    p.setState(EXPR_FNAME|EXPR_FITEM);
                } fitem {
                    $$ = p.dispatch("on_alias", $2, $4);
                }
                | keyword_alias tGVAR tGVAR {
                    $$ = p.dispatch("on_var_alias", $2, $3);
                }
                | keyword_alias tGVAR tBACK_REF {
                    $$ = p.dispatch("on_var_alias", $2, $3);
                }
                | keyword_alias tGVAR tNTH_REF {
                    $$ = p.dispatch("on_alias_error", p.dispatch("on_var_alias", $2, $3));
                    p.error();
                }
                | keyword_undef undef_list {
                    $$ = p.dispatch("on_undef", $2);
                }
                | stmt modifier_if expr_value {
                    $$ = p.dispatch("on_if_mod", $3, $1);
                }
                | stmt modifier_unless expr_value {
                    $$ = p.dispatch("on_unless_mod", $3, $1);
                }
                | stmt modifier_while expr_value {
                    $$ = p.dispatch("on_while_mod", $3, $1);
                }
                | stmt modifier_until expr_value {
                    $$ = p.dispatch("on_until_mod", $3, $1);
                }
                | stmt modifier_rescue stmt {
                    $$ = p.dispatch("on_rescue_mod", $1, $3);
                }
                | keyword_END tLCURLY compstmt tRCURLY {
                    if (p.isInDef()) {
                        p.warn("END in method; use at_exit");
                    }
                    $$ = p.dispatch("on_END", $3);
                }
                | command_asgn
                | mlhs '=' command_call {
                    $$ = p.dispatch("on_massign", $1, $3);
                }
                | lhs '=' mrhs {
                    $$ = p.dispatch("on_assign", $1, $3);
                }
                | mlhs '=' mrhs_arg {
                    $$ = p.dispatch("on_massign", $1, $3);
                }
                | expr

command_asgn    : lhs '=' command_rhs {
                    $$ = p.dispatch("on_assign", $1, $3);
                }
                | var_lhs tOP_ASGN command_rhs {
                    $$ = p.dispatch("on_opassign", $1, $2, $3);
                }
                | primary_value '[' opt_call_args rbracket tOP_ASGN command_rhs {
                    $$ = p.dispatch("on_opassign", 
                                    p.dispatch("on_aref_field", $1, $3),
                                    $5, $6);
                }
                | primary_value call_op tIDENTIFIER tOP_ASGN command_rhs {
                    $$ = p.dispatch("on_opassign", 
                                    p.dispatch("on_field", $1, $2, $3), 
                                    $4, $5);
                }
                | primary_value call_op tCONSTANT tOP_ASGN command_rhs {
                    $$ = p.dispatch("on_opassign", 
                                    p.dispatch("on_field",$1, $2, $3),
                                    $4, $5);
                }
                | primary_value tCOLON2 tCONSTANT tOP_ASGN command_rhs {
                    $$ = p.dispatch("on_opassign", 
                                    p.dispatch("on_const_path_field", $1, $3), 
                                    $4,
                                    $5);
                }
                | primary_value tCOLON2 tIDENTIFIER tOP_ASGN command_rhs {
                    $$ = p.dispatch("on_opassign", 
                                    p.dispatch("on_field", $1, p.intern("::"), $3), 
                                    $4, $5);
                }
                | backref tOP_ASGN command_rhs {
                    $$ = p.dispatch("on_assign_error", 
                                    p.dispatch("on_assign", 
                                    p.dispatch("on_var_field", $1), 
                                    $3));
                    p.error();
                }

command_rhs     : command_call %prec tOP_ASGN {
                    $$ = $1;
                }
                | command_call modifier_rescue stmt {
                    $$ = p.dispatch("on_rescue_mod", $1, $3);
                }
                | command_asgn

// Node:expr *CURRENT* all but arg so far
expr            : command_call
                | expr keyword_and expr {
                    $$ = p.dispatch("on_binary", $1, p.intern("and"), $3);
                }
                | expr keyword_or expr {
                    $$ = p.dispatch("on_binary", $1, p.intern("or"), $3);
                }
                | keyword_not opt_nl expr {
                    $$ = p.dispatch("on_unary", p.intern("not"), $3);
                }
                | tBANG command_call {
                    $$ = p.dispatch("on_unary", p.intern("!"), $2);
                }
                | arg

expr_value      : expr {
                    $$ = $1;
                }

expr_value_do   : {
                    p.getConditionState().push1();
                } expr_value do {
                    p.getConditionState().pop();
                } {
                    $$ = $2;
                }

// Node:command - call with or with block on end [!null]
command_call    : command
                | block_command
                ;

// Node:block_command - A call with a block (foo.bar {...}, foo::bar {...}, bar {...}) [!null]
block_command   : block_call
                | block_call call_op2 operation2 command_args {
                    $$ = p.dispatch("on_method_add_arg", 
                                    p.dispatch("on_call", $1, $2, $3),
                                    $4);
                }

// :brace_block - [!null]
cmd_brace_block : tLBRACE_ARG brace_body tRCURLY {
                    $$ = $2;
                }

fcall           : operation
 

// Node:command - fcall/call/yield/super [!null]
command        : fcall command_args %prec tLOWEST {
                    $$ = p.dispatch("on_command", $1, $2);
                }
                | fcall command_args cmd_brace_block {
                    $$ = p.dispatch("on_method_add_block",
                                    p.dispatch("on_command", $1, $2),
                                    $3);
                }
                | primary_value call_op operation2 command_args %prec tLOWEST {
                    $$ = p.dispatch("on_command_call", $1, $2, $3, $4);
                }
                | primary_value call_op operation2 command_args cmd_brace_block {
                    $$ = p.dispatch("on_method_add_block",
                                    p.dispatch("on_command_call", $1, $2, $3, $4),
                                    $5); 
                }
                | primary_value tCOLON2 operation2 command_args %prec tLOWEST {
                    $$ = p.dispatch("on_command_call", $1, p.intern("::"), $3, $4);
                }
                | primary_value tCOLON2 operation2 command_args cmd_brace_block {
                    $$ = p.dispatch("on_method_add_block",
                                    p.dispatch("on_command_call", $1, p.intern("::"), $3, $4),
                                    $5);
                }
                | keyword_super command_args {
                    $$ = p.dispatch("on_super", $2);
                }
                | keyword_yield command_args {
                    $$ = p.dispatch("on_yield", $2);
                }
                | keyword_return call_args {
                    $$ = p.dispatch("on_return", $2);
                }
		| keyword_break call_args {
                    $$ = p.dispatch("on_break", $2);
                }
		| keyword_next call_args {
                    $$ = p.dispatch("on_next", $2);
                }


// MultipleAssigNode:mlhs - [!null]
mlhs            : mlhs_basic
                | tLPAREN mlhs_inner rparen {
                    $$ = p.dispatch("on_mlhs_paren", $2);
                }

// MultipleAssignNode:mlhs_entry - mlhs w or w/o parens [!null]
mlhs_inner      : mlhs_basic
                | tLPAREN mlhs_inner rparen {
                    $$ = p.dispatch("on_mlhs_paren", $2);
                }

// MultipleAssignNode:mlhs_basic - multiple left hand side (basic because used in multiple context) [!null]
mlhs_basic      : mlhs_head {
                    $$ = $1;
                }
                | mlhs_head mlhs_item {
                    $$ = p.dispatch("on_mlhs_add", $1, $2);
                }
                | mlhs_head tSTAR mlhs_node {
                    $$ = p.dispatch("on_mlhs_add_star", $1, $3);
                }
                | mlhs_head tSTAR mlhs_node ',' mlhs_post {
                    $$ = p.dispatch("on_mlhs_add_post",
                                    p.dispatch("on_mlhs_add_star", $1, $3),
                                    $5);
                }
                | mlhs_head tSTAR {
                    $$ = p.dispatch("on_mlhs_add_star", $1, null);
                }
                | mlhs_head tSTAR ',' mlhs_post {
                    $$ = p.dispatch("on_mlhs_add_post",
                                    p.dispatch("on_mlhs_add_star", $1, null),
                                    $4);
                }
                | tSTAR mlhs_node {
                    $$ = p.dispatch("on_mlhs_add_star", p.dispatch("on_mlhs_new"), $2);
                }
                | tSTAR mlhs_node ',' mlhs_post {
                    $$ = p.dispatch("on_mlhs_add_post",
                                    p.dispatch("on_mlhs_add_star", p.dispatch("on_mlhs_new"), $2),
                                    $4);
                }
                | tSTAR {
                    $$ = p.dispatch("on_mlhs_add_star", p.dispatch("on_mlhs_new"), null);
                }
                | tSTAR ',' mlhs_post {
                    $$ = p.dispatch("on_mlhs_add_post",
                                    p.dispatch("on_mlhs_add_star", p.dispatch("on_mlhs_new"), null),
                                    $3);
                }

mlhs_item       : mlhs_node
                | tLPAREN mlhs_inner rparen {
                    $$ = p.dispatch("on_mlhs_paren", $2);
                }

// Set of mlhs terms at front of mlhs (a, *b, d, e = arr  # a is head)
mlhs_head       : mlhs_item ',' {
                    $$ = p.dispatch("on_mlhs_add", p.dispatch("on_mlhs_new"), $1);
                }
                | mlhs_head mlhs_item ',' {
                    $$ = p.dispatch("on_mlhs_add", $1, $2);
                }

// Set of mlhs terms at end of mlhs (a, *b, d, e = arr  # d,e is post)
mlhs_post       : mlhs_item {
                    $$ = p.dispatch("on_mlhs_add", p.dispatch("on_mlhs_new"), $1);
                }
                | mlhs_post ',' mlhs_item {
                    $$ = p.dispatch("on_mlhs_add", $1, $3);
                }

mlhs_node       : /*mri:user_variable*/ tIDENTIFIER {
                    $$ = p.assignableIdentifier(p.dispatch("on_var_field", $1));
                }
                | tIVAR {
                    $$ = p.dispatch("on_var_field", $1);
                }
                | tGVAR {
                    $$ = p.dispatch("on_var_field", $1);
                }
                | tCONSTANT {
                    $$ = p.assignableConstant(p.dispatch("on_var_field", $1));
                }
                | tCVAR {
                    $$ = p.dispatch("on_var_field", $1);
                } /*mri:user_variable*/
                | /*mri:keyword_variable*/ keyword_nil {
                    $$ = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", $1));
                }
                | keyword_self {
                    $$ = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", $1));
                }
                | keyword_true {
                    $$ = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", $1));
                }
                | keyword_false {
                    $$ = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", $1));
                }
                | keyword__FILE__ {
                    $$ = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", $1));
                }
                | keyword__LINE__ {
                    $$ = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", $1));
                }
                | keyword__ENCODING__ {
                    p.yyerror("Can't assign to __ENCODING__");
                } /*mri:keyword_variable*/
                | primary_value '[' opt_call_args rbracket {
                    $$ = p.dispatch("on_aref_field", $1, $3);
                }
                | primary_value call_op tIDENTIFIER {
                    $$ = p.dispatch("on_field", $1, $2, $3);
                    
                }
                | primary_value tCOLON2 tIDENTIFIER {
                    $$ = p.dispatch("on_const_path_field", $1, $3);
                }
                | primary_value call_op tCONSTANT {
		    $$ = p.dispatch("on_field", $1, $2, $3);
                }
                | primary_value tCOLON2 tCONSTANT {
                    $$ = p.dispatch("on_const_path_field", $1, $3);

                    if (p.isInDef()) {
                        $$ = p.dispatch("on_assign_error", $<IRubyObject>$);
                        p.error();
                    }
                }
                | tCOLON3 tCONSTANT {
                    $$ = p.dispatch("on_top_const_field", $2);

                    if (p.isInDef()) {
                        $$ = p.dispatch("on_assign_error", $<IRubyObject>$);
                        p.error();
                    }
                }
                | backref {
                    $$ = p.dispatch("on_assign_error", p.dispatch("on_var_field", $1));
                    p.error();
                }

lhs             : /*mri:user_variable*/ tIDENTIFIER {
                    $$ = p.dispatch("on_var_field", p.assignableIdentifier($1));
                }
                | tIVAR {
                    $$ = p.dispatch("on_var_field", $1);
                }
                | tGVAR {
                    $$ = p.dispatch("on_var_field", $1);
                }
                | tCONSTANT {
                    $$ = p.assignableConstant(p.dispatch("on_var_field", $1));
                }
                | tCVAR {
                    $$ = p.dispatch("on_var_field", $1);
                } /*mri:user_variable*/
                | /*mri:keyword_variable*/ keyword_nil {
                    $$ = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", $1));
                }
                | keyword_self {
                    $$ = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", $1));
                }
                | keyword_true {
                    $$ = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", $1));
                }
                | keyword_false {
                    $$ = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", $1));
                }
                | keyword__FILE__ {
                    $$ = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", $1));
                }
                | keyword__LINE__ {
                    $$ = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", $1));
                }
                | keyword__ENCODING__ {
                    $$ = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", $1));
                } /*mri:keyword_variable*/
                | primary_value '[' opt_call_args rbracket {
                    $$ = p.dispatch("on_aref_field", $1, $3);
                }
                | primary_value call_op tIDENTIFIER {
                    $$ = p.dispatch("on_field", $1, $2, $3);
                }
                | primary_value tCOLON2 tIDENTIFIER {
                    $$ = p.dispatch("on_field", $1, p.intern("::"), $3);
                }
                | primary_value call_op tCONSTANT {
                    $$ = p.dispatch("on_field", $1, $2, $3);
                }
                | primary_value tCOLON2 tCONSTANT {
                    IRubyObject val = p.dispatch("on_const_path_field", $1, $3);

                    if (p.isInDef()) {
                        val = p.dispatch("on_assign_error", val);
                        p.error();
                    }

                    $$ = val;
                }
                | tCOLON3 tCONSTANT {
                    IRubyObject val = p.dispatch("on_top_const_field", $2);

                    if (p.isInDef()) {
                        val = p.dispatch("on_assign_error", val);
                        p.error();
                    }

                    $$ = val;
                }
                | backref {
                    $$ = p.dispatch("on_assign_error",
                                    p.dispatch("on_var_field", $1));
                    p.error();
                }

cname           : tIDENTIFIER {
                    $$ = p.dispatch("on_class_name_error", $1);
                    p.error();
                }
                | tCONSTANT

cpath           : tCOLON3 cname {
                    $$ = p.dispatch("on_top_const_ref", $2);
                }
                | cname {
                    $$ = p.dispatch("on_const_ref", $1);
                }
                | primary_value tCOLON2 cname {
                    $$ = p.dispatch("on_const_path_ref", $1, $3);
                }

// Token:fname - A function name [!null]
fname          : tIDENTIFIER | tCONSTANT | tFID 
               | op {
                   p.setState(EXPR_ENDFN);
                   $$ = $1;
               }
               | reswords {
                   p.setState(EXPR_ENDFN);
                   $$ = $1;
               }

// LiteralNode:fsym
fsym           : fname {
                   $$ = $1;
               }
               | symbol {
                   $$ = $1;
               }

// Node:fitem
fitem           : fsym {
                   $$ = p.dispatch("on_symbol_literal", $1);
                }
                | dsym {
                   $$ = $1;
                }

undef_list      : fitem {
                    $$ = p.new_array($1);
                }
                | undef_list ',' {
                    p.setState(EXPR_FNAME|EXPR_FITEM);
                } fitem {
                    $$ = $1.append($4);
                }

// Token:op
op              : tPIPE | tCARET | tAMPER2 | tCMP | tEQ | tEQQ | tMATCH
                | tNMATCH | tGT | tGEQ | tLT | tLEQ | tNEQ | tLSHFT | tRSHFT
                | tPLUS | tMINUS | tSTAR2 | tSTAR | tDIVIDE | tPERCENT | tPOW
                | tBANG | tTILDE | tUPLUS | tUMINUS | tAREF | tASET | tBACK_REF2

// Token:op
reswords        : keyword__LINE__ | keyword__FILE__ | keyword__ENCODING__ | keyword_BEGIN | keyword_END
                | keyword_alias | keyword_and | keyword_begin | keyword_break | keyword_case | keyword_class | keyword_def
                | keyword_defined | keyword_do | keyword_else | keyword_elsif | keyword_end | keyword_ensure | keyword_false
                | keyword_for | keyword_in | keyword_module | keyword_next | keyword_nil | keyword_not
                | keyword_or | keyword_redo | keyword_rescue | keyword_retry | keyword_return | keyword_self | keyword_super
                | keyword_then | keyword_true | keyword_undef | keyword_when | keyword_yield
                | keyword_if | keyword_unless | keyword_while | keyword_until
                | modifier_if | modifier_unless | modifier_while | modifier_until | modifier_rescue

arg             : lhs '=' arg_rhs {
                    $$ = p.dispatch("on_assign", $1, $3);
                }
                | var_lhs tOP_ASGN arg_rhs {
                    $$ = p.dispatch("on_opassign", $1, $2, $3);
                }
                | primary_value '[' opt_call_args rbracket tOP_ASGN arg {
                    $$ = p.dispatch("on_opassign", 
                                    p.dispatch("on_aref_field", $1, $3),
                                    $5, $6);
                }
                | primary_value call_op tIDENTIFIER tOP_ASGN arg_rhs {
                    $$ = p.dispatch("on_opassign", 
                                    p.dispatch("on_field", $1, $2, $3),
                                    $4, $5);
                }
                | primary_value call_op tCONSTANT tOP_ASGN arg_rhs {
                    $$ = p.dispatch("on_opassign", 
                                    p.dispatch("on_field", $1, $2, $3),
                                    $4, $5);
                }
                | primary_value tCOLON2 tIDENTIFIER tOP_ASGN arg_rhs {
                    $$ = p.dispatch("on_opassign", 
                                    p.dispatch("on_field", $1, p.intern("::"), $3),
                                    $4, $5);
                }
                | primary_value tCOLON2 tCONSTANT tOP_ASGN arg_rhs {
                    $$ = p.dispatch("on_assign_error", 
                                    p.dispatch("on_opassign", 
                                               p.dispatch("on_const_path_field", $1, $3),
                                               $4, $5));
                }
                | tCOLON3 tCONSTANT tOP_ASGN arg_rhs {
                    $$ = p.dispatch("on_assign_error", 
                                    p.dispatch("on_opassign", 
                                               p.dispatch("on_top_const_field", $2),
                                               $3, $4));
                }
                | backref tOP_ASGN arg_rhs {
                    $$ = p.dispatch("on_assign_error", 
                                    p.dispatch("on_opassign",
                                               p.dispatch("on_var_field", $1),
                                               $2, $3));
                    p.error();
                }
                | arg tDOT2 arg {
                    $$ = p.dispatch("on_dot2", $1, $3);
                }
                | arg tDOT3 arg {
                    $$ = p.dispatch("on_dot3", $1, $3);
                }
                | arg tDOT2 {
                    $$ = p.dispatch("on_dot2", $1, p.new_nil_at());
                }
                | arg tDOT3 {
                    $$ = p.dispatch("on_dot3", $1, p.new_nil_at());
                }
                | arg tPLUS arg {
                    $$ = p.dispatch("on_binary", $1, p.intern("+"), $3);
                }
                | arg tMINUS arg {
                    $$ = p.dispatch("on_binary", $1, p.intern("-"), $3);
                }
                | arg tSTAR2 arg {
                    $$ = p.dispatch("on_binary", $1, p.intern("*"), $3);
                }
                | arg tDIVIDE arg {
                    $$ = p.dispatch("on_binary", $1, p.intern("/"), $3);
                }
                | arg tPERCENT arg {
                    $$ = p.dispatch("on_binary", $1, p.intern("%"), $3);
                }
                | arg tPOW arg {
                    $$ = p.dispatch("on_binary", $1, p.intern("**"), $3);
                }
                | tUMINUS_NUM simple_numeric tPOW arg {
                    $$ = p.dispatch("on_unary", 
                                    p.intern("-@"), 
                                    p.dispatch("on_binary", $2, p.intern("**"), $4));
                }
                | tUPLUS arg {
                    $$ = p.dispatch("on_unary", p.intern("+@"), $2);
                }
                | tUMINUS arg {
                    $$ = p.dispatch("on_unary", p.intern("-@"), $2);
                }
                | arg tPIPE arg {
                    $$ = p.dispatch("on_binary", $1, p.intern("|"), $3);
                }
                | arg tCARET arg {
                    $$ = p.dispatch("on_binary", $1, p.intern("^"), $3);
                }
                | arg tAMPER2 arg {
                    $$ = p.dispatch("on_binary", $1, p.intern("&"), $3);
                }
                | arg tCMP arg {
                    $$ = p.dispatch("on_binary", $1, p.intern("<=>"), $3);
                }
                | rel_expr   %prec tCMP {
                    $$ = $1;
                }
                | arg tEQ arg {
                    $$ = p.dispatch("on_binary", $1, p.intern("=="), $3);
                }
                | arg tEQQ arg {
                    $$ = p.dispatch("on_binary", $1, p.intern("==="), $3);
                }
                | arg tNEQ arg {
                    $$ = p.dispatch("on_binary", $1, p.intern("!="), $3);
                }
                | arg tMATCH arg {
                    $$ = p.dispatch("on_binary", $1, p.intern("=~"), $3);
                }
                | arg tNMATCH arg {
                    $$ = p.dispatch("on_binary", $1, p.intern("!~"), $3);
                }
                | tBANG arg {
                    $$ = p.dispatch("on_unary", p.intern("!"), $2);
                }
                | tTILDE arg {
                    $$ = p.dispatch("on_unary", p.intern("~"), $2);
                }
                | arg tLSHFT arg {
                    $$ = p.dispatch("on_binary", $1, p.intern("<<"), $3);
                }
                | arg tRSHFT arg {
                    $$ = p.dispatch("on_binary", $1, p.intern(">>"), $3);
                }
                | arg tANDOP arg {
                    $$ = p.dispatch("on_binary", $1, p.intern("&&"), $3);
                }
                | arg tOROP arg {
                    $$ = p.dispatch("on_binary", $1, p.intern("||"), $3);
                }
                | keyword_defined opt_nl arg {
                    $$ = p.dispatch("on_defined", $3);
                }
                | arg '?' arg opt_nl ':' arg {
                    $$ = p.dispatch("on_ifop", $1, $3, $6);
                }
                | primary {
                    $$ = $1;
                }

relop           : tGT {
                    $$ = p.intern(">");
                }
                | tLT  {
                    $$ = p.intern("<");
                }
                | tGEQ {
                    $$ = p.intern(">=");
                }
                | tLEQ {
                    $$ = p.intern("<=");
                }

rel_expr        : arg relop arg   %prec tGT {
                     $$ = p.dispatch("on_binary", $1, $2, $3);

                }
		| rel_expr relop arg   %prec tGT {
                     p.warning("comparison '" + $2 + "' after comparison");
                     $$ = p.dispatch("on_binary", $1, $2, $3);
                }
 
arg_value       : arg {
                    $$ = $1;
                }

aref_args       : none
                | args trailer {
                    $$ = $1;
                }
                | args ',' assocs trailer {
                    $$ = p.dispatch("on_args_add", 
                                    $1,
                                    p.dispatch("on_bare_assoc_hash", $3));
                }
                | assocs trailer {
                    $$ = p.dispatch("on_args_add", 
                                    p.dispatch("on_args_new"),
                                    p.dispatch("on_bare_assoc_hash", $1));
                }

arg_rhs         : arg %prec tOP_ASGN {
                    $$ = $1;
                }
                | arg modifier_rescue arg {
                    $$ = p.dispatch("on_rescue_mod", $1, $3);
                }

paren_args      : tLPAREN2 opt_call_args rparen {
                    $$ = p.dispatch("on_arg_paren", $2);
                }

opt_paren_args  : none | paren_args

opt_call_args   : none
                | call_args
                | args ',' {
                    $$ = $1;
                }
                | args ',' assocs ',' {
                    $$ = p.dispatch("on_args_add", $1, p.dispatch("on_bare_assoc_hash", $3));
                }
                | assocs ',' {
                    $$ = p.dispatch("on_args_add", 
                                    p.dispatch("on_args_new"),
                                    p.dispatch("on_bare_assoc_hash", $1));
                }

// [!null]
call_args       : command {
                    $$ = p.dispatch("on_args_add", p.dispatch("on_args_new"), $1);
                }
                | args opt_block_arg {
                    $$ = p.arg_add_optblock($1, $2);
                }
                | assocs opt_block_arg {
                    $$ =  p.arg_add_optblock(p.dispatch("on_args_add", 
                                                        p.dispatch("on_args_new"),
                                                        p.dispatch("on_bare_assoc_hash", $1)),
                                             $2);
                }
                | args ',' assocs opt_block_arg {
                    $$ = p.arg_add_optblock(p.dispatch("on_args_add", 
                                            $1,
                                            p.dispatch("on_bare_assoc_hash", $3)),
                                            $4);
                }
                | block_arg {
                    $$ = p.dispatch("on_args_add_block", p.dispatch("on_args_new"), $1);
                }

command_args    : /* none */ {
                    boolean lookahead = false;
                    switch (yychar) {
                    case tLPAREN2: case tLPAREN: case tLPAREN_ARG: case '[': case tLBRACK:
                       lookahead = true;
                    }
                    StackState cmdarg = p.getCmdArgumentState();
                    if (lookahead) cmdarg.pop();
                    cmdarg.push1();
                    if (lookahead) cmdarg.push0();
                } call_args {
                    StackState cmdarg = p.getCmdArgumentState();

                    boolean lookahead = false;
                    switch (yychar) {
                    case tLBRACE_ARG:
                       lookahead = true;
                    }

                    if (lookahead) cmdarg.pop();
                    cmdarg.pop();
                    if (lookahead) cmdarg.push0();
                    $$ = $2;
                }

block_arg       : tAMPER arg_value {
                    $$ = $2;
                }

opt_block_arg   : ',' block_arg {
                    $$ = $2;
                }
                | none_block_pass

// [!null]
args            : arg_value {
                    $$ = p.dispatch("on_args_add", p.dispatch("on_args_new"), $1);
                }
                | tSTAR arg_value {
                    $$ = p.dispatch("on_args_add_star", p.dispatch("on_args_new"), $2);
                }
                | args ',' arg_value {
                    $$ = p.dispatch("on_args_add", $1, $3);
                }
                | args ',' tSTAR arg_value {
                    $$ = p.dispatch("on_args_add_star", $1, $4);
                }

mrhs_arg	: mrhs {
                    $$ = $1;
                }
		| arg_value {
                    $$ = $1;
                }

mrhs            : args ',' arg_value {
                    $$ = p.dispatch("on_mrhs_add", 
                                    p.dispatch("on_mrhs_new_from_args", $1), 
                                    $3);
                }
                | args ',' tSTAR arg_value {
                    $$ = p.dispatch("on_mrhs_add_star",
                                    p.dispatch("on_mrhs_new_from_args", $1),
                                    $4);
                }
                | tSTAR arg_value {
                    $$ = p.dispatch("on_mrhs_add_star", p.dispatch("on_mrhs_new"), $2);
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
                    $$ = p.dispatch("on_method_add_arg", p.dispatch("on_fcall", $1), p.dispatch("on_args_new"));
                }
                | keyword_begin {
                    p.getCmdArgumentState().push0();
                } bodystmt keyword_end {
                    p.getCmdArgumentState().pop();
                    $$ = p.dispatch("on_begin", $3);
                }
                | tLPAREN_ARG {
                    p.setState(EXPR_ENDARG);
                } rparen {
                    $$ = p.dispatch("on_paren", null);
                }
                | tLPAREN_ARG {
                    p.getCmdArgumentState().push0();
                } stmt {
                    p.setState(EXPR_ENDARG); 
                } rparen {
                    p.getCmdArgumentState().pop();
                    p.warning("(...) interpreted as grouped expression");
                    $$ = p.dispatch("on_paren", $3);
                }
                | tLPAREN compstmt tRPAREN {
                    $$ = p.dispatch("on_paren", $2);
                }
                | primary_value tCOLON2 tCONSTANT {
                    $$ = p.dispatch("on_const_path_ref", $1, $3);
                }
                | tCOLON3 tCONSTANT {
                    $$ = p.dispatch("on_top_const_ref", $2);
                }
                | tLBRACK aref_args tRBRACK {
                    $$ = p.dispatch("on_array", $2);
                }
                | tLBRACE assoc_list tRCURLY {
                    $$ = p.dispatch("on_hash", $2);
                }
                | keyword_return {
                    $$ = p.dispatch("on_return0");
                }
                | keyword_yield tLPAREN2 call_args rparen {
                    $$ = p.dispatch("on_yield", p.dispatch("on_paren", $3));
                }
                | keyword_yield tLPAREN2 rparen {
                    $$ = p.dispatch("on_yield", p.dispatch("on_paren", p.dispatch("on_args_new")));
                }
                | keyword_yield {
                    $$ = p.dispatch("on_yield0");
                }
                | keyword_defined opt_nl tLPAREN2 expr rparen {
                    $$ = p.dispatch("on_defined", $4);
                }
                | keyword_not tLPAREN2 expr rparen {
                    $$ = p.dispatch("on_unary", p.intern("not"), $3);
                }
                | keyword_not tLPAREN2 rparen {
                    $$ = p.dispatch("on_unary", p.intern("not"), null);
                }
                | fcall brace_block {
                    $$ = p.dispatch("on_method_add_block",
                                    p.dispatch("on_method_add_arg", 
                                               p.dispatch("on_fcall", $1), 
                                               p.dispatch("on_args_new")), 
                                    $2);
                }
                | method_call
                | method_call brace_block {
                    $$ = p.dispatch("on_method_add_block", $1, $2);
                }
                | tLAMBDA lambda {
                    $$ = $2;
                }
                | keyword_if expr_value then compstmt if_tail keyword_end {
                    $$ = p.dispatch("on_if", $2, $4, $5);
                }
                | keyword_unless expr_value then compstmt opt_else keyword_end {
                    $$ = p.dispatch("on_unless", $2, $4, $5);
                }
                | keyword_while {
                    p.getConditionState().push1();
                } expr_value_do {
                    p.getConditionState().pop();
                } compstmt keyword_end {
                    $$ = p.dispatch("on_while", $3, $5);
                }
                | keyword_until {
                  p.getConditionState().push1();
                } expr_value_do {
                  p.getConditionState().pop();
                } compstmt keyword_end {
                    $$ = p.dispatch("on_until", $3, $5);
                }
                | keyword_case expr_value opt_terms case_body keyword_end {
                    $$ = p.dispatch("on_case", $2, $4);
                }
                | keyword_case opt_terms case_body keyword_end {
                    $$ = p.dispatch("on_case", null, $3);
                }
                | keyword_for for_var keyword_in {
                    p.getConditionState().push1();
                } expr_value_do {
                    p.getConditionState().pop();
                } compstmt keyword_end {
                    $$ = p.dispatch("on_for", $2, $5, $7);
                }
                | keyword_class cpath superclass {
                    if (p.isInDef()) {
                        p.yyerror("class definition in method body");
                    }
                    p.pushLocalScope();
                    $$ = p.isInClass(); // MRI reuses $1 but we use the value for position.
                    p.setIsInClass(true);
                } bodystmt keyword_end {
                    $$ = p.dispatch("on_class", $2, $3, $5);
                    p.popCurrentScope();
                    p.setIsInClass($<Boolean>4.booleanValue());
                }
                | keyword_class tLSHFT expr {
                    $$ = new Integer((p.isInClass() ? 0b10 : 0) |
                                     (p.isInDef()   ? 0b01 : 0));
                    p.setInDef(false);
                    p.setIsInClass(false);
                    p.pushLocalScope();
                } term bodystmt keyword_end {
                    $$ = p.dispatch("on_sclass", $3, $6);

                    p.popCurrentScope();
                    p.setInDef((($<Integer>4.intValue())     & 0b01) != 0);
                    p.setIsInClass((($<Integer>4.intValue()) & 0b10) != 0);
                }
                | keyword_module cpath {
                    if (p.isInDef()) { 
                        p.yyerror("module definition in method body");
                    }
                    $$ = p.isInClass();
                    p.setIsInClass(true);
                    p.pushLocalScope();
                } bodystmt keyword_end {
                    $$ = p.dispatch("on_module", $2, $4);
                    p.popCurrentScope();
                }
                | keyword_def fname {
                    p.setInDef(true);
                    p.pushLocalScope();
                    $$ = p.getCurrentArg();
                    p.setCurrentArg(null);
                } f_arglist bodystmt keyword_end {
                    $$ = p.dispatch("on_def", $2, $4, $5);

                    p.popCurrentScope();
                    p.setInDef(false);
                    p.setCurrentArg($<IRubyObject>3);
                }
                | keyword_def singleton dot_or_colon {
                    p.setState(EXPR_FNAME);
                    $$ = p.isInDef();
                    p.setInDef(true);
                } fname {
                    p.pushLocalScope();
                    p.setState(EXPR_ENDFN|EXPR_LABEL); /* force for args */
                    $$ = p.getCurrentArg();
                    p.setCurrentArg(null);                    
                } f_arglist bodystmt keyword_end {
                    $$ = p.dispatch("on_defs", $2, $3, $5, $7, $8);

                    p.popCurrentScope();
                    p.setInDef($<Boolean>4.booleanValue());
                    p.setCurrentArg($<IRubyObject>6);
                }
                | keyword_break {
                    $$ = p.dispatch("on_break", p.dispatch("on_args_new"));
                }
                | keyword_next {
                    $$ = p.dispatch("on_next", p.dispatch("on_args_new"));
                }
                | keyword_redo {
                    $$ = p.dispatch("on_redo");
                }
                | keyword_retry {
                    $$ = p.dispatch("on_retry");
                }

primary_value   : primary {
                    $$ = $1;
                }

then            : term {
                    $$ = null;
                }
                | keyword_then
                | term keyword_then {
                    $$ = $2;
                }

do              : term {
                    $$ = null;
                }
                | keyword_do_cond

if_tail         : opt_else
                | keyword_elsif expr_value then compstmt if_tail {
                    $$ = p.dispatch("on_elsif", $2, $4, $5);
                }

opt_else        : none
                | keyword_else compstmt {
                    $$ = p.dispatch("on_else", $2);
                }

for_var         : lhs
                | mlhs {
                }

f_marg          : f_norm_arg {
                    $$ = $1;
                }
                | tLPAREN f_margs rparen {
                    $$ = p.dispatch("on_mlhs_paren", $2);
                }

// [!null]
f_marg_list     : f_marg {
                    $$ = p.dispatch("on_mlhs_add", p.dispatch("on_mlhs_new"), $1);
                }
                | f_marg_list ',' f_marg {
                    $$ = p.dispatch("on_mlhs_add", $1, $3);
                }

f_margs         : f_marg_list {
                    $$ = $1;
                }
                | f_marg_list ',' tSTAR f_norm_arg {
                    $$ = p.dispatch("on_mlhs_add_star", $1, $4);
                }
                | f_marg_list ',' tSTAR f_norm_arg ',' f_marg_list {
                    $$ = p.dispatch("on_mlhs_add_post", 
                                    p.dispatch("on_mlhs_add_star", $1, $4),
                                    $6);
                }
                | f_marg_list ',' tSTAR {
                    $$ = p.dispatch("on_mlhs_add_star", $1, null);
                }
                | f_marg_list ',' tSTAR ',' f_marg_list {
                    $$ = p.dispatch("on_mlhs_add_post", 
                                    p.dispatch("on_mlhs_add_star", $1, null),
                                    $5);
                }
                | tSTAR f_norm_arg {
                    $$ = p.dispatch("on_mlhs_add_star",
                                    p.dispatch("on_mlhs_new"),
                                    $2);
                }
                | tSTAR f_norm_arg ',' f_marg_list {
                    $$ = p.dispatch("on_mlhs_add_post", 
                                    p.dispatch("on_mlhs_add_star",
                                               p.dispatch("on_mlhs_new"),
                                               $2),
                                    $4);
                }
                | tSTAR {
                    $$ = p.dispatch("on_mlhs_add_star",
                                    p.dispatch("on_mlhs_new"),
                                    null);
                }
                | tSTAR ',' f_marg_list {
                    $$ = p.dispatch("on_mlhs_add_post", 
                                    p.dispatch("on_mlhs_add_star",
                                               p.dispatch("on_mlhs_new"),
                                               null),
                                    $3);
                }

block_args_tail : f_block_kwarg ',' f_kwrest opt_f_block_arg {
                    $$ = p.new_args_tail($1, $3, $4);
                }
                | f_block_kwarg opt_f_block_arg {
                    $$ = p.new_args_tail($1, null, $2);
                }
                | f_kwrest opt_f_block_arg {
                    $$ = p.new_args_tail(null, $1, $2);
                }
                | f_block_arg {
                    $$ = p.new_args_tail(null, null, $1);
                }
 
opt_block_args_tail : ',' block_args_tail {
                    $$ = $2;
                }
                | /* none */ {
                    $$ = p.new_args_tail(null, null, null);
                }
 

// [!null]
block_param     : f_arg ',' f_block_optarg ',' f_rest_arg opt_block_args_tail {
                    $$ = p.new_args($1, $3, $5, null, $6);
                }
                | f_arg ',' f_block_optarg ',' f_rest_arg ',' f_arg opt_block_args_tail {
                    $$ = p.new_args($1, $3, $5, $7, $8);
                }
                | f_arg ',' f_block_optarg opt_block_args_tail {
                    $$ = p.new_args($1, $3, null, null, $4);
                }
                | f_arg ',' f_block_optarg ',' f_arg opt_block_args_tail {
                    $$ = p.new_args($1, $3, null, $5, $6);
                }
                | f_arg ',' f_rest_arg opt_block_args_tail {
                    $$ = p.new_args($1, null, $3, null, $4);
                }
                | f_arg ',' {
                    $$ = p.dispatch("on_excessed_comma", 
                                    p.new_args($1, null, null, null, null));
                }
                | f_arg ',' f_rest_arg ',' f_arg opt_block_args_tail {
                    $$ = p.new_args($1, null, $3, $5, $6);
                }
                | f_arg opt_block_args_tail {
                    $$ = p.new_args($1, null, null, null, $2);
                }
                | f_block_optarg ',' f_rest_arg opt_block_args_tail {
                    $$ = p.new_args(null, $1, $3, null, $4);
                }
                | f_block_optarg ',' f_rest_arg ',' f_arg opt_block_args_tail {
                    $$ = p.new_args(null, $1, $3, $5, $6);
                }
                | f_block_optarg opt_block_args_tail {
                    $$ = p.new_args(null, $1, null, null, $2);
                }
                | f_block_optarg ',' f_arg opt_block_args_tail {
                    $$ = p.new_args(null, $1, null, $3, $4);
                }
                | f_rest_arg opt_block_args_tail {
                    $$ = p.new_args(null, null, $1, null, $2);     
                }
                | f_rest_arg ',' f_arg opt_block_args_tail {
                    $$ = p.new_args(null, null, $1, $3, $4);
                }
                | block_args_tail {
                    $$ = p.new_args(null, null, null, null, $1);
                }

opt_block_param : none 
                | block_param_def {
                    p.setCommandStart(true);
                }

block_param_def : tPIPE opt_bv_decl tPIPE {
                    p.setCurrentArg(null);  
                    $$ = p.dispatch("on_block_var", 
                                    p.new_args(null, null, null, null, null), 
                                    $2);
                }
                | tOROP {
                    $$ = p.dispatch("on_block_var", 
                                    p.new_args(null, null, null, null, null), 
                                    null);
                }
                | tPIPE block_param opt_bv_decl tPIPE {
                    p.setCurrentArg(null);
                    $$ = p.dispatch("on_block_var", $2, $3);
                }

// shadowed block variables....
opt_bv_decl     : opt_nl {
                    $$ = p.getContext().getRuntime().getFalse();
                }
                | opt_nl ';' bv_decls opt_nl {
                    $$ = $3;
                }

// ENEBO: This is confusing...
bv_decls        : bvar {
                    $$ = p.new_array($1);
                }
                | bv_decls ',' bvar {
                    $$ = $1.append($3);
                }

bvar            : tIDENTIFIER {
                    $$ = p.new_bv($1);

 }
                | f_bad_arg {
                    $$ = null;
                }

lambda          : /* none */  {
                    p.pushBlockScope();
                    $$ = p.getLeftParenBegin();
                    p.setLeftParenBegin(p.incrementParenNest());
                } f_larglist {
                    p.getCmdArgumentState().push0();
                } lambda_body {
                    p.getCmdArgumentState().pop();
                    $$ = p.dispatch("on_lambda", $2, $4);
                    p.setLeftParenBegin($<Integer>1);
                    p.popCurrentScope();
                }
 
f_larglist      : tLPAREN2 f_args opt_bv_decl tRPAREN {
                    $$ = p.dispatch("on_paren", $2);
                }
                | f_args {
                    $$ = $1;
                }

lambda_body     : tLAMBEG compstmt tRCURLY {
                    $$ = $2;
                }
                | keyword_do_lambda compstmt keyword_end {
                    $$ = $2;
                }

do_block        : keyword_do_block do_body keyword_end {
                    $$ = $2;  
                }

block_call      : command do_block {
                    $$ = p.dispatch("on_method_add_block", $1, $2);
                }
                | block_call call_op2 operation2 opt_paren_args {
                    $$ = p.method_optarg(p.dispatch("on_call", $1, $2, $3), $4);
                }
                | block_call call_op2 operation2 opt_paren_args brace_block {
                    $$ = p.method_add_block(p.dispatch("on_command_call", $1, $2, $3, $4), $5);
                }
                | block_call call_op2 operation2 command_args do_block {
                    $$ = p.method_add_block(p.dispatch("on_command_call", $1, $2, $3, $4), $5);
                }

// [!null]
method_call     : fcall paren_args {
                    $$ = p.dispatch("on_method_add_arg", p.dispatch("on_fcall", $1), $2);
                }
                | primary_value call_op operation2 opt_paren_args {
                    $$ = p.method_optarg(p.dispatch("on_call", $1, $2, $3), $4);
                }
                | primary_value tCOLON2 operation2 paren_args {
                    $$ = p.method_optarg(p.dispatch("on_call", $1, p.intern("::"), $3), $4);
                }
                | primary_value tCOLON2 operation3 {
                    $$ = p.dispatch("on_call", $1, p.intern("::"), $3);
                }
                | primary_value call_op paren_args {
                    $$ = p.method_optarg(p.dispatch("on_call", $1, $2, p.intern("call")), $3);
                }
                | primary_value tCOLON2 paren_args {
                    $$ = p.method_optarg(p.dispatch("on_call", $1, p.intern("::"), p.intern("call")), $3);
                }
                | keyword_super paren_args {
                    $$ = p.dispatch("on_super", $2);
                }
                | keyword_super {
                    $$ = p.dispatch("on_zsuper");
                }
                | primary_value '[' opt_call_args rbracket {
                    $$ = p.dispatch("on_aref", $1, $3);
                }

brace_block     : tLCURLY brace_body tRCURLY {
                    $$ = $2;
                }
                | keyword_do {
                    p.pushBlockScope();
                } opt_block_param compstmt keyword_end {
                    $$ = p.dispatch("on_do_block", $3, $4);
                    p.popCurrentScope();
                }

brace_body      : {
                    p.pushBlockScope();
                } opt_block_param compstmt  {
                    $$ = p.dispatch("on_brace_block", $2, $3);
                    p.popCurrentScope();
                }

do_body 	: {
                    p.pushBlockScope();
                    p.getCmdArgumentState().push0();
                } opt_block_param bodystmt {
                    $$ = p.dispatch("on_do_block", $2, $3);
                    p.getCmdArgumentState().pop();
                    p.popCurrentScope();
                }

case_body       : keyword_when args then compstmt cases {
                    $$ = p.dispatch("on_when", $2, $4, $5);

                }

cases           : opt_else | case_body

opt_rescue      : keyword_rescue exc_list exc_var then compstmt opt_rescue {
                    $$ = p.dispatch("on_rescue", $2, $3, $5, $6);
                }
                | {
                    $$ = null;
                }

exc_list        : arg_value {
                    $$ = p.new_array($1);
                }
                | mrhs {
                    $$ = $1;
                }
                | none

exc_var         : tASSOC lhs {
                    $$ = $2;
                }
                | none

opt_ensure      : keyword_ensure compstmt {
                    $$ = p.dispatch("on_ensure", $2);
                }
                | none

literal         : numeric
                | symbol {
                    $$ = p.dispatch("on_symbol_literal", $1);
                }
                | dsym

strings         : string {
                    $$ = $1;
                }

// [!null]
string          : tCHAR {
                    $$ = $1;
                }
                | string1 {
                    $$ = $1;
                }
                | string string1 {
                    $$ = p.dispatch("on_string_concat", $1, $2);
                }

string1         : tSTRING_BEG string_contents tSTRING_END {
                    p.heredoc_dedent($2);
                    p.setHeredocIndent(0);
                    $$ = p.dispatch("on_string_literal", $2);
                }

xstring         : tXSTRING_BEG xstring_contents tSTRING_END {
                    p.heredoc_dedent($2);
                    p.setHeredocIndent(0);
                    $$ = p.dispatch("on_xstring_literal", $2);
                }

regexp          : tREGEXP_BEG regexp_contents tREGEXP_END {
                    $$ = p.dispatch("on_regexp_literal", $2, $3);
                }

words           : tWORDS_BEG ' ' word_list tSTRING_END {
                    $$ = p.dispatch("on_array", $3);
                }

word_list       : /* none */ {
                    $$ = p.dispatch("on_words_new");
                }
                | word_list word ' ' {
                    $$ = p.dispatch("on_words_add", $1, $2);
                }

word            : string_content {
                    $$ = p.dispatch("on_word_add", p.dispatch("on_word_new"), $1);
                 }
                | word string_content {
                    $$ = p.dispatch("on_word_add", $1, $2);
                }

symbols         : tSYMBOLS_BEG ' ' symbol_list tSTRING_END {
                    $$ = p.dispatch("on_array", $3);
                }

symbol_list     : /* none */ {
                    $$ = p.dispatch("on_symbols_new");
                }
                | symbol_list word ' ' {
                    $$ = p.dispatch("on_symbols_add", $1, $2);
                }

qwords          : tQWORDS_BEG ' ' qword_list tSTRING_END {
                    $$ = p.dispatch("on_array", $3);
                }

qsymbols        : tQSYMBOLS_BEG ' ' qsym_list tSTRING_END {
                    $$ = p.dispatch("on_array", $3);
                }

qword_list      : /* none */ {
                    $$ = p.dispatch("on_qwords_new");
                }
                | qword_list tSTRING_CONTENT ' ' {
                    $$ = p.dispatch("on_qwords_add", $1, $2);
                }

qsym_list      : /* none */ {
                    $$ = p.dispatch("on_qsymbols_new");
                }
                | qsym_list tSTRING_CONTENT ' ' {
                    $$ = p.dispatch("on_qsymbols_add", $1, $2);
                }

string_contents : /* none */ {
                    $$ = p.dispatch("on_string_content");
                }
                | string_contents string_content {
                    $$ = p.dispatch("on_string_add", $1, $2);
                }

xstring_contents: /* none */ {
                    $$ = p.dispatch("on_xstring_new");
                }
                | xstring_contents string_content {
                    $$ = p.dispatch("on_xstring_add", $1, $2);
                }

regexp_contents: /* none */ {
                    $$ = p.dispatch("on_regexp_new");
                }
                | regexp_contents string_content {
                    $$ = p.dispatch("on_regexp_add", $1, $2);
                }

string_content  : tSTRING_CONTENT
                | tSTRING_DVAR {
                    $$ = p.getStrTerm();
                    p.setStrTerm(null);
                    p.setState(EXPR_BEG);
                } string_dvar {
                    p.setStrTerm($<StrTerm>2);
                    $$ = p.dispatch("on_string_dvar", $3);
                }
                | tSTRING_DBEG {
                   $$ = p.getStrTerm();
                   p.setStrTerm(null);
                   p.getConditionState().push0();
                   p.getCmdArgumentState().push0();
                } {
                   $$ = p.getState();
                   p.setState(EXPR_BEG);
                } {
                   $$ = p.getBraceNest();
                   p.setBraceNest(0);
                } {
                   $$ = p.getHeredocIndent();
                   p.setHeredocIndent(0);
                } compstmt tSTRING_DEND {
                   p.getConditionState().pop();
                   p.getCmdArgumentState().pop();
                   p.setStrTerm($<StrTerm>2);
                   p.setState($<Integer>3);
                   p.setBraceNest($<Integer>4);
                   p.setHeredocIndent($<Integer>5);
                   $$ = p.dispatch("on_string_embexpr", $6);
                }

string_dvar     : tGVAR {
                   $$ = p.dispatch("on_var_ref", $1);
                }
                | tIVAR {
                   $$ = p.dispatch("on_var_ref", $1);
                }
                | tCVAR {
                   $$ = p.dispatch("on_var_ref", $1);
                }
                | backref

// Token:symbol
symbol          : tSYMBEG sym {
                     p.setState(EXPR_END|EXPR_ENDARG);
                     $$ = p.dispatch("on_symbol", $2);
                }

// Token:symbol
sym             : fname | tIVAR | tGVAR | tCVAR

dsym            : tSYMBEG string_contents tSTRING_END {
                     p.setState(EXPR_END|EXPR_ENDARG);
                     $$ = p.dispatch("on_dyna_symbol", $2);
                }

numeric         : simple_numeric {
                    $$ = $1;
                }
                | tUMINUS_NUM simple_numeric %prec tLOWEST {
                    $$ = p.dispatch("on_unary", p.intern("-@"), $2);
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
                    if (p.is_id_var()) {
                        $$ = p.dispatch("on_var_ref", $1);
                    } else {
                        $$ = p.dispatch("on_vcall", $1);
                    }
                }
                | tIVAR {
                    $$ = p.dispatch("on_var_ref", $1);
                }
                | tGVAR {
                    $$ = p.dispatch("on_var_ref", $1);
                }
                | tCONSTANT {
                    $$ = p.dispatch("on_var_ref", $1);
                }
                | tCVAR {
                    $$ = p.dispatch("on_var_ref", $1);
                } /*mri:user_variable*/
                | /*mri:keyword_variable*/ keyword_nil {
                    $$ = p.dispatch("on_var_ref", $1);
                }
                | keyword_self {
                    $$ = p.dispatch("on_var_ref", $1);
                }
                | keyword_true {
                    $$ = p.dispatch("on_var_ref", $1);
                }
                | keyword_false {
                    $$ = p.dispatch("on_var_ref", $1);
                 }
                | keyword__FILE__ {
                    $$ = p.dispatch("on_var_ref", $1);
                }
                | keyword__LINE__ {
                    $$ = p.dispatch("on_var_ref", $1);
                }
                | keyword__ENCODING__ {
                    $$ = p.dispatch("on_var_ref", $1);
                } /*mri:keyword_variable*/

// [!null]
var_lhs         : /*mri:user_variable*/ tIDENTIFIER {
                    $$ = p.dispatch("on_var_field", p.assignableIdentifier($1));
                }
                | tIVAR {
                    $$ = p.dispatch("on_var_field", $1);
                }
                | tGVAR {
                    $$ = p.dispatch("on_var_field", $1);
                }
                | tCONSTANT {
                    $$ = p.assignableConstant(p.dispatch("on_var_field", $1));
                }
                | tCVAR {
                    $$ = p.dispatch("on_var_field", $1);
                } /*mri:user_variable*/
                | /*mri:keyword_variable*/ keyword_nil {
                    p.yyerror("Can't assign to nil");
                }
                | keyword_self {
                    p.yyerror("Can't change the value of self");
                }
                | keyword_true {
                    p.yyerror("Can't assign to true");
                }
                | keyword_false {
                    p.yyerror("Can't assign to false");
                }
                | keyword__FILE__ {
                    p.yyerror("Can't assign to __FILE__");
                }
                | keyword__LINE__ {
                    p.yyerror("Can't assign to __LINE__");
                }
                | keyword__ENCODING__ {
                    p.yyerror("Can't assign to __ENCODING__");
                } /*mri:keyword_variable*/
 

// [!null]
backref         : tNTH_REF
                | tBACK_REF

superclass      : tLT {
                   p.setState(EXPR_BEG);
                   p.setCommandStart(true);
                } expr_value term {
                    $$ = $3;
                }
                | /* none */ {
                   $$ = null;
                }

// [!null]
// ENEBO: Look at command_start stuff I am ripping out
f_arglist       : tLPAREN2 f_args rparen {
                    p.setState(EXPR_BEG);
                    p.setCommandStart(true);
                    $$ = p.dispatch("on_paren", $2);
                }
                | {
  // $$ = lexer.inKwarg;
                   //                   p.inKwarg = true;
                   p.setState(p.getState() | EXPR_LABEL);
                } f_args term {
  // p.inKwarg = $<Boolean>1;
                    $$ = $2;
                    p.setState(EXPR_BEG);
                    p.setCommandStart(true);
                }
 
args_tail       : f_kwarg ',' f_kwrest opt_f_block_arg {
                    $$ = p.new_args_tail($1, $3, $4);
                }
                | f_kwarg opt_f_block_arg {
                    $$ = p.new_args_tail($1, null, $2);
                }
                | f_kwrest opt_f_block_arg {
                    $$ = p.new_args_tail(null, $1, $2);
                }
                | f_block_arg {
                    $$ = p.new_args_tail(null, null, $1);
                }

opt_args_tail   : ',' args_tail {
                    $$ = $2;
                }
                | /* none */ {
                    $$ = p.new_args_tail(null, null, null);
                }

// [!null]
f_args          : f_arg ',' f_optarg ',' f_rest_arg opt_args_tail {
                    $$ = p.new_args($1, $3, $5, null, $6);
                }
                | f_arg ',' f_optarg ',' f_rest_arg ',' f_arg opt_args_tail {
                    $$ = p.new_args($1, $3, $5, $7, $8);
                }
                | f_arg ',' f_optarg opt_args_tail {
                    $$ = p.new_args($1, $3, null, null, $4);
                }
                | f_arg ',' f_optarg ',' f_arg opt_args_tail {
                    $$ = p.new_args($1, $3, null, $5, $6);
                }
                | f_arg ',' f_rest_arg opt_args_tail {
                    $$ = p.new_args($1, null, $3, null, $4);
                }
                | f_arg ',' f_rest_arg ',' f_arg opt_args_tail {
                    $$ = p.new_args($1, null, $3, $5, $6);
                }
                | f_arg opt_args_tail {
                    $$ = p.new_args($1, null, null, null, $2);
                }
                | f_optarg ',' f_rest_arg opt_args_tail {
                    $$ = p.new_args(null, $1, $3, null, $4);
                }
                | f_optarg ',' f_rest_arg ',' f_arg opt_args_tail {
                    $$ = p.new_args(null, $1, $3, $5, $6);
                }
                | f_optarg opt_args_tail {
                    $$ = p.new_args(null, $1, null, null, $2);
                }
                | f_optarg ',' f_arg opt_args_tail {
                    $$ = p.new_args(null, $1, null, $3, $4);
                }
                | f_rest_arg opt_args_tail {
                    $$ = p.new_args(null, null, $1, null, $2);
                }
                | f_rest_arg ',' f_arg opt_args_tail {
                    $$ = p.new_args(null, null, $1, $3, $4);
                }
                | args_tail {
                    $$ = p.new_args(null, null, null, null, $1);
                }
                | /* none */ {
                    $$ = p.new_args(null, null, null, null, null);
                }

f_bad_arg       : tCONSTANT {
                    $$ = p.dispatch("on_param_error", $1);
                    p.error();
                }
                | tIVAR {
                    $$ = p.dispatch("on_param_error", $1);
                    p.error();
                }
                | tGVAR {
                    $$ = p.dispatch("on_param_error", $1);
                    p.error();
                }
                | tCVAR {
                    $$ = p.dispatch("on_param_error", $1);
                    p.error();
                }

// Token:f_norm_arg [!null]
f_norm_arg      : f_bad_arg
                | tIDENTIFIER {
                    $$ = p.arg_var(p.formal_argument($1));
                }

f_arg_asgn      : f_norm_arg {
                    p.setCurrentArg($1);
                    $$ = $1;
                }

f_arg_item      : f_arg_asgn {
                    p.setCurrentArg(null);
                    $$ = $1;
                }
                | tLPAREN f_margs rparen {
                    $$ = p.dispatch("on_mlhs_paren", $2);
                }

// [!null]
f_arg           : f_arg_item {
                    $$ = p.new_array($1);
                }
                | f_arg ',' f_arg_item {
                    $$ = $1.append($3);
                }

f_label 	: tLABEL {
                    p.arg_var(p.formal_argument($1));
                    p.setCurrentArg($1);
                    $$ = $1;
                }
 
f_kw            : f_label arg_value {
                    p.setCurrentArg(null);
                    $$ = p.keyword_arg($1, $2);
                }
                | f_label {
                    p.setCurrentArg(null);
                    $$ = p.keyword_arg($1, p.getContext().getRuntime().getFalse());
                }

f_block_kw      : f_label primary_value {
                    $$ = p.keyword_arg($1, $2);
                }
                | f_label {
                    $$ = p.keyword_arg($1, p.getContext().getRuntime().getFalse());
                }

f_block_kwarg   : f_block_kw {
                    $$ = p.new_array($1);
                }
                | f_block_kwarg ',' f_block_kw {
                    $$ = $1.append($3);
                }

f_kwarg         : f_kw {
                    $$ = p.new_array($1);
                }
                | f_kwarg ',' f_kw {
                    $$ = $1.append($3);
                }

kwrest_mark     : tPOW {
                    $$ = $1;
                }
                | tDSTAR {
                    $$ = $1;
                }

f_kwrest        : kwrest_mark tIDENTIFIER {
                    p.shadowing_lvar($2);
                    $$ = p.dispatch("on_kwrest_param", $2);
                }
                | kwrest_mark {
                    $$ = p.dispatch("on_kwrest_param", null);
                }

f_opt           : f_arg_asgn '=' arg_value {
                    p.setCurrentArg(null);
                    $$ = p.new_assoc($1, $3);

                }

f_block_opt     : f_arg_asgn '=' primary_value {
                    p.setCurrentArg(null);
                    $$ = p.new_assoc($1, $3);
                }

f_block_optarg  : f_block_opt {
                    $$ = p.new_array($1);
                }
                | f_block_optarg ',' f_block_opt {
                    $$ = $1.append($3);
                }

f_optarg        : f_opt {
                    $$ = p.new_array($1);
                }
                | f_optarg ',' f_opt {
                    $$ = $1.append($3);
                }

restarg_mark    : tSTAR2 | tSTAR

// [!null]
f_rest_arg      : restarg_mark tIDENTIFIER {
                    p.arg_var(p.shadowing_lvar($2));
                    $$ = p.dispatch("on_rest_param", $2);
                }
                | restarg_mark {
                    $$ = p.dispatch("on_rest_param", null);
                }

// [!null]
blkarg_mark     : tAMPER2 | tAMPER

// f_block_arg - Block argument def for function (foo(&block)) [!null]
f_block_arg     : blkarg_mark tIDENTIFIER {
                    p.arg_var(p.shadowing_lvar($2));
                    $$ = p.dispatch("on_blockarg", $2);
                }

opt_f_block_arg : ',' f_block_arg {
                    $$ = $2;
                }
                | /* none */ {
                    $$ = null;
                }

singleton       : var_ref {
                    $$ = $1;
                }
                | tLPAREN2 {
                    p.setState(EXPR_BEG);
                } expr rparen {
                    $$ = p.dispatch("on_paren", $3);
                }

// [!null]
assoc_list      : none
                | assocs trailer {
                    $$ = p.dispatch("on_assoclist_from_args", $1);
                }

// [!null]
assocs          : assoc {
                    $$ = p.new_array($1);
                }
                | assocs ',' assoc {
                    $$ = $1.append($3);
                }

// [!null]
assoc           : arg_value tASSOC arg_value {
                    $$ = p.dispatch("on_assoc_new", $1, $3);
                }
                | tLABEL arg_value {
                    $$ = p.dispatch("on_assoc_new", $1, $2);
                }
                | tSTRING_BEG string_contents tLABEL_END arg_value {
                    $$ = p.dispatch("on_assoc_new", p.dispatch("on_dyna_symbol", $2), $4);
                }
                | tDSTAR arg_value {
                    $$ = p.dispatch("on_assoc_splat", $2);
                }
 

operation       : tIDENTIFIER | tCONSTANT | tFID
operation2      : tIDENTIFIER | tCONSTANT | tFID | op
operation3      : tIDENTIFIER | tFID | op
dot_or_colon    : tDOT {
                    $$ = $1;
                }
                | tCOLON2 {
                    $$ = $1;
                }

call_op 	: tDOT {
                    $$ = p.intern(".");
                }
                | tANDDOT {
                    $$ = p.intern("&.");
                }

call_op2        : call_op {
                    $$ = $1;
                }
                | tCOLON2 {
                   $$ = p.intern("::");
                }

 
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
}
