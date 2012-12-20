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

import org.jruby.RubyArray;
import org.jruby.runtime.builtin.IRubyObject;
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

public class Ripper19Parser implements RubyParser {
    protected RipperSupport support;
    protected RubyYaccLexer lexer;

    public Ripper19Parser() {
        this(new RipperSupport());
    }

    public Ripper19Parser(RipperSupport support) {
        this.support = support;
        lexer = new RubyYaccLexer(false);
        lexer.setParserSupport(support);
        support.setLexer(lexer);
    }

    public void setWarnings(IRubyWarnings warnings) {
        support.setWarnings(warnings);
        lexer.setWarnings(warnings);
    }
%}

// We need to make sure we have same tokens in the same order and up
// front so 1.8 and 1.9 parser can use the same Tokens.java file.
%token <IRubyObject> kCLASS kMODULE kDEF kUNDEF kBEGIN kRESCUE kENSURE kEND kIF
  kUNLESS kTHEN kELSIF kELSE kCASE kWHEN kWHILE kUNTIL kFOR kBREAK kNEXT
  kREDO kRETRY kIN kDO kDO_COND kDO_BLOCK kRETURN kYIELD kSUPER kSELF kNIL
  kTRUE kFALSE kAND kOR kNOT kIF_MOD kUNLESS_MOD kWHILE_MOD kUNTIL_MOD
  kRESCUE_MOD kALIAS kDEFINED klBEGIN klEND k__LINE__ k__FILE__
  k__ENCODING__ kDO_LAMBDA 

%token <IRubyObject> tIDENTIFIER tFID tGVAR tIVAR tCONSTANT tCVAR tLABEL tCHAR
%type <IRubyObject> sym symbol operation operation2 operation3 cname fname 
%type <IRubyObject> op
%type <IRubyObject> f_norm_arg dot_or_colon restarg_mark blkarg_mark
%token <IRubyObject> tUPLUS         /* unary+ */
%token <IRubyObject> tUMINUS        /* unary- */
%token <IRubyObject> tUMINUS_NUM    /* unary- */
%token <IRubyObject> tPOW           /* ** */
%token <IRubyObject> tCMP           /* <=> */
%token <IRubyObject> tEQ            /* == */
%token <IRubyObject> tEQQ           /* === */
%token <IRubyObject> tNEQ           /* != */
%token <IRubyObject> tGEQ           /* >= */
%token <IRubyObject> tLEQ           /* <= */
%token <IRubyObject> tANDOP tOROP   /* && and || */
%token <IRubyObject> tMATCH tNMATCH /* =~ and !~ */
%token <IRubyObject>  tDOT           /* Is just '.' in ruby and not a token */
%token <IRubyObject> tDOT2 tDOT3    /* .. and ... */
%token <IRubyObject> tAREF tASET    /* [] and []= */
%token <IRubyObject> tLSHFT tRSHFT  /* << and >> */
%token <IRubyObject> tCOLON2        /* :: */
%token <IRubyObject> tCOLON3        /* :: at EXPR_BEG */
%token <IRubyObject> tOP_ASGN       /* +=, -=  etc. */
%token <IRubyObject> tASSOC         /* => */
%token <IRubyObject> tLPAREN        /* ( */
%token <IRubyObject> tLPAREN2        /* ( Is just '(' in ruby and not a token */
%token <IRubyObject> tRPAREN        /* ) */
%token <IRubyObject> tLPAREN_ARG    /* ( */
%token <IRubyObject> tLBRACK        /* [ */
%token <IRubyObject> tRBRACK        /* ] */
%token <IRubyObject> tLBRACE        /* { */
%token <IRubyObject> tLBRACE_ARG    /* { */
%token <IRubyObject> tSTAR          /* * */
%token <IRubyObject> tSTAR2         /* *  Is just '*' in ruby and not a token */
%token <IRubyObject> tAMPER         /* & */
%token <IRubyObject> tAMPER2        /* &  Is just '&' in ruby and not a token */
%token <IRubyObject> tTILDE         /* ` is just '`' in ruby and not a token */
%token <IRubyObject> tPERCENT       /* % is just '%' in ruby and not a token */
%token <IRubyObject> tDIVIDE        /* / is just '/' in ruby and not a token */
%token <IRubyObject> tPLUS          /* + is just '+' in ruby and not a token */
%token <IRubyObject> tMINUS         /* - is just '-' in ruby and not a token */
%token <IRubyObject> tLT            /* < is just '<' in ruby and not a token */
%token <IRubyObject> tGT            /* > is just '>' in ruby and not a token */
%token <IRubyObject> tPIPE          /* | is just '|' in ruby and not a token */
%token <IRubyObject> tBANG          /* ! is just '!' in ruby and not a token */
%token <IRubyObject> tCARET         /* ^ is just '^' in ruby and not a token */
%token <IRubyObject> tLCURLY        /* { is just '{' in ruby and not a token */
%token <IRubyObject> tRCURLY        /* } is just '}' in ruby and not a token */
%token <IRubyObject> tBACK_REF2     /* { is just '`' in ruby and not a token */
%token <IRubyObject> tSYMBEG tSTRING_BEG tXSTRING_BEG tREGEXP_BEG tWORDS_BEG tQWORDS_BEG
%token <IRubyObject> tSTRING_DBEG tSTRING_DVAR tSTRING_END
%token <IRubyObject> tLAMBDA tLAMBEG
%token <IRubyObject> tNTH_REF tBACK_REF tSTRING_CONTENT tINTEGER
%token <IRubyObject> tFLOAT  
%token <IRubyObject>  tREGEXP_END
%type <IRubyObject> f_rest_arg 
%type <IRubyObject> singleton strings string string1 xstring regexp
%type <IRubyObject> string_contents xstring_contents string_content method_call
%type <IRubyObject> words qwords word literal numeric dsym cpath command_asgn command_call
%type <IRubyObject> compstmt bodystmt stmts stmt expr arg primary command 
%type <IRubyObject> expr_value primary_value opt_else cases if_tail exc_var
   // ENEBO: missing call_args2, open_args
%type <IRubyObject> call_args opt_ensure paren_args superclass
%type <IRubyObject> command_args var_ref opt_paren_args block_call block_command
%type <IRubyObject> f_opt
%type <RubyArray> undef_list 
%type <IRubyObject> string_dvar backref
%type <IRubyObject> f_args f_arglist f_larglist block_param block_param_def opt_block_param 
%type <IRubyObject> mrhs mlhs_item mlhs_node arg_value case_body exc_list aref_args
   // ENEBO: missing block_var == for_var, opt_block_var
%type <IRubyObject> lhs none args
%type <IRubyObject> qword_list word_list f_arg f_optarg f_marg_list
   // ENEBO: missing when_args
%type <IRubyObject> mlhs_head assocs assoc assoc_list mlhs_post f_block_optarg
%type <IRubyObject> opt_block_arg block_arg none_block_pass
%type <IRubyObject> opt_f_block_arg f_block_arg
%type <IRubyObject> brace_block do_block cmd_brace_block
   // ENEBO: missing mhls_entry
%type <IRubyObject> mlhs mlhs_basic 
%type <IRubyObject> opt_rescue
%type <IRubyObject> var_lhs
%type <IRubyObject> fsym
%type <IRubyObject> fitem
   // ENEBO: begin all new types
%type <IRubyObject> f_arg_item
%type <IRubyObject> bv_decls opt_bv_decl lambda_body 
%type <IRubyObject> lambda
%type <IRubyObject> mlhs_inner f_block_opt for_var
%type <IRubyObject> opt_call_args f_marg f_margs
%type <IRubyObject> bvar
%type <IRubyObject> user_variable, keyword_variable
   // ENEBO: end all new types

%type <IRubyObject> rparen rbracket reswords f_bad_arg
%type <IRubyObject> top_compstmt top_stmts top_stmt

%type <IRubyObject> do then

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
              } top_compstmt {
                  support.setResult(support.dispatch("on_program", $2));
              }

top_compstmt  : top_stmts opt_terms {
                  $$ = $1;
              }

top_stmts     : none {
                  $$ = support.dispatch("on_stmts_add", 
                                        support.dispatch("on_stmts_new"), 
                                        support.dispatch("on_void_stmt"));
              }
              | top_stmt {
                  $$ = support.dispatch("on_stmts_add", 
                                        support.dispatch("on_stmts_new"), 
                                        $1);
              }
              | top_stmts terms top_stmt {
                  $$ = support.dispatch("on_stmts_add", $1, $3);
              }
              | error top_stmt {
                  $$ = support.remove_begin($2);
              }

top_stmt      : stmt
              | klBEGIN {
                  if (support.isInDef() || support.isInSingle()) {
                      support.yyerror("BEGIN in method");
                  }
              } tLCURLY top_compstmt tRCURLY {
                  $$ = support.dispatch("on_BEGIN", $4);
              }

bodystmt      : compstmt opt_rescue opt_else opt_ensure {
                  $$ = support.dispatch("on_bodystmt", 
                                        support.escape($1),
                                        support.escape($2),
                                        support.escape($3),
                                        support.escape($4));
                }

compstmt        : stmts opt_terms {
                    $$ = $1;
                }

 stmts          : none {
                    $$ = support.dispatch("on_stmts_add", 
                                          support.dispatch("on_stmts_new"),
                                          support.dispatch("on_void_stmt"));
                }
                | stmt {
                    $$ = support.dispatch("on_stmts_add",
                                          support.dispatch("on_stmts_new"), 
                                          $1);
                }
                | stmts terms stmt {
                    $$ = support.dispatch("on_stmts_add", $1, $3);
                }
                | error stmt {
                    $$ = remove_begin($2);
                }

stmt            : kALIAS fitem {
                    lexer.setState(LexState.EXPR_FNAME);
                } fitem {
                    $$ = support.dispatch("on_alias", $2, $4);
                }
                | kALIAS tGVAR tGVAR {
                    $$ = support.dispatch("on_var_alias", $2, $3);
                }
                | kALIAS tGVAR tBACK_REF {
                    $$ = support.dispatch("on_var_alias", $2, $3);
                }
                | kALIAS tGVAR tNTH_REF {
                    $$ = support.dispatch("on_alias_error", 
                                          support.dispatch("on_var_alias", $2, $3));
                }
                | kUNDEF undef_list {
                    $$ = support.dispatch("on_undef", $2);
                }
                | stmt kIF_MOD expr_value {
                    $$ = support.dispatch("on_if_mod", $3, $1);
                }
                | stmt kUNLESS_MOD expr_value {
                    $$ = support.dispatch("on_unless_mod", $3, $1);
                }
                | stmt kWHILE_MOD expr_value {
                    $$ = support.dispatch("on_while_mod", $3, $1);
                }
                | stmt kUNTIL_MOD expr_value {
                    $$ = support.dispatch("on_until_mod", $3, $1);
                }
                | stmt kRESCUE_MOD stmt {
                    $$ = support.dispatch("on_rescue_mod", $1, $3);
                }
                | klEND tLCURLY compstmt tRCURLY {
                    if (support.isInDef() || support.isInSingle()) {
                        support.warn(ID.END_IN_METHOD, $1.getPosition(), "END in method; use at_exit");
                    }
                    $$ = support.dispatch("on_END", $3);
                }
                | command_asgn
                | mlhs '=' command_call {
                    $$ = support.dispatch("on_massign", $1, $3);
                }
                | var_lhs tOP_ASGN command_call {
                    $$ = support.dispatch("on_opassign", $1, $2, $3);
                }
                | primary_value '[' opt_call_args rbracket tOP_ASGN command_call {
                    $$ = support.dispatch("on_opassign", 
                                          support.dispatch("on_aref_field", 
                                                           $1, 
                                                           support.escape($3)),
                                          $5, $6);
                }
                | primary_value tDOT tIDENTIFIER tOP_ASGN command_call {
                    $$ = support.dispatch("on_opassign", 
                                          support.dispatch("on_field", 
                                                           $1, 
                                                           support.symbol('.'),
                                                           $3), 
                                          $4, $5);
                }
                | primary_value tDOT tCONSTANT tOP_ASGN command_call {
                    $$ = support.dispatch("on_opassign", 
                                          support.dispatch("on_field",
                                                           $1,
                                                           support.symbol('.'),
                                                           $3),
                                          $4, $5);
                }
                | primary_value tCOLON2 tCONSTANT tOP_ASGN command_call {
                    $$ = support.dispatch("on_assign_error", 
                                          support.dispatch("on_opassign", 
                                                           support.dispatch("on_const_path_field", $1, $3), 
                                                           $4, $5));
                }
                | primary_value tCOLON2 tIDENTIFIER tOP_ASGN command_call {
                    $$ = support.dispatch("on_opassign", 
                                          support.dispatch("on_field", 
                                                           $1, 
                                                           support.intern("::"),
                                                           $3), 
                                          $4, $5);
                }
                | backref tOP_ASGN command_call {
                    $$ = support.dispatch("on_assign_error", 
                                          support.dispatch("on_assign", 
                                                           support.dispatch("on_var_field", $1), 
                                                           $3));
               }
                | lhs '=' mrhs {
                    $$ = support.dispatch("on_assign", $1, $3);
                }
                | mlhs '=' arg_value {
                    $$ = support.dispatch("on_assign", $1, $3);
                }
                | mlhs '=' mrhs {
                    $$ = support.dispatch("on_massign", $1, $3);
                }
                | expr

command_asgn    : lhs '=' command_call {
                    $$ = support.dispatch("on_assign", $1, $3);
                }
                | lhs '=' command_asgn {
                    $$ = support.dispatch("on_assign", $1, $3);
                }

// Node:expr *CURRENT* all but arg so far
expr            : command_call
                | expr kAND expr {
                    $$ = support.dispatch("on_binary", $1, "and", $3);
                }
                | expr kOR expr {
                    $$ = support.dispatch("on_binary", $1, "or", $3);
                }
                | kNOT opt_nl expr {
                    $$ = support.dispatch("on_unary", support.intern("not"), $3);
                }
                | tBANG command_call {
                    $$ = support.dispatch("on_unary", support.symbol('!'), $2);
                }
                | arg

expr_value      : expr {
                    $$ = $1;
                }

// Node:command - call with or with block on end [!null]
command_call    : command
                | block_command
                ;

// Node:block_command - A call with a block (foo.bar {...}, foo::bar {...}, bar {...}) [!null]
block_command   : block_call
                | block_call tDOT operation2 command_args {
                    $$ = method_arg(support.dispatch("on_call", $1, ".", $3), $4);
                }
                | block_call tCOLON2 operation2 command_args {
                    $$ = method_arg(support.dispatch("on_call", $1, support.intern("::"), $3), $4);
                }

// :brace_block - [!null]
cmd_brace_block : tLBRACE_ARG {
                    support.pushBlockScope();
                } opt_block_param compstmt tRCURLY {
                    $$ = support.dispatch("on_brace_block", support.escape($3), $4);
                    support.popCurrentScope();
                }

// Node:command - fcall/call/yield/super [!null]
command        : operation command_args %prec tLOWEST {
                    $$ = support.dispatch("on_command", $1, $2);
                }
                | operation command_args cmd_brace_block {
                    $$ = method_add_block(support.dispatch("on_command", $1, $2), $3);
                }
                | primary_value tDOT operation2 command_args %prec tLOWEST {
                    $$ = support.dispatch("on_command_call", $1, support.symbol('.'), $3, $4);
                }
                | primary_value tDOT operation2 command_args cmd_brace_block {
                    $$ = method_add_block(support.dispatch("on_command_call",
                                                           $1,
                                                           support.symbol('.'),
                                                           $3, $4),
                                          $5); 
                }
                | primary_value tCOLON2 operation2 command_args %prec tLOWEST {
                    $$ = support.dispatch("on_command_call", $1, support.intern("::"), $3, $4);
                }
                | primary_value tCOLON2 operation2 command_args cmd_brace_block {
                    $$ = method_add_block(support.dispatch("on_command_call",
                                                           $1,
                                                           support.intern("::"),
                                                           $3, $4),
                                          $5);
                }
                | kSUPER command_args {
                    $$ = support.dispatch("on_super", $2);
                }
                | kYIELD command_args {
                    $$ = support.dispatch("on_yield", $2);
                }
                | kRETURN call_args {
                    $$ = support.dispatch("on_areturn", $2);
                }
		| kBREAK call_args {
                    $$ = support.dispatch("on_abreak", $2);
                }
		| kNEXT call_args {
                    $$ = support.dispatch("on_next", $2);
                }


// MultipleAssig19Node:mlhs - [!null]
mlhs            : mlhs_basic
                | tLPAREN mlhs_inner rparen {
                    $$ = support.dispatch("on_mlhs_paren", $2);
                }

// MultipleAssign19Node:mlhs_entry - mlhs w or w/o parens [!null]
mlhs_inner      : mlhs_basic
                | tLPAREN mlhs_inner rparen {
                    $$ = support.dispatch("on_mlhs_paren", $2);
                }

// MultipleAssign19Node:mlhs_basic - multiple left hand side (basic because used in multiple context) [!null]
mlhs_basic      : mlhs_head {
                    $$ = $1;
                }
                | mlhs_head mlhs_item {
                    $$ = support.mlhs_add($1, $2);
                }
                | mlhs_head tSTAR mlhs_node {
                    $$ = support.mlhs_add_star($1, $3);
                }
                | mlhs_head tSTAR mlhs_node ',' mlhs_post {
                    $$ = support.mlhs_add(support.mlhs_add_star($1, $3), $5);
                }
                | mlhs_head tSTAR {
                    $$ = support.mlhs_add_star($1, null);
                }
                | mlhs_head tSTAR ',' mlhs_post {
                    $$ = support.mlhs_add(support.mlhs_add_star($1, null), $4);
                }
                | tSTAR mlhs_node {
                    $$ = support.mlhs_add_star(support.mlhs_new(), $2);
                }
                | tSTAR mlhs_node ',' mlhs_post {
                    $$ = support.mlhs_add(support.mlhs_add_star(support.mlhs_new(), $2), $4);
                }
                | tSTAR {
                    $$ = support.mlhs_add_star(support.mlhs_new(), null);
                }
                | tSTAR ',' mlhs_post {
                    $$ = support.mlhs_add(support.mlhs_add_star(support.mlhs_new(), null), $3);
                }

mlhs_item       : mlhs_node
                | tLPAREN mlhs_inner rparen {
                    $$ = support.dispatch("on_mlhs_paren", $2);
                }

// Set of mlhs terms at front of mlhs (a, *b, d, e = arr  # a is head)
mlhs_head       : mlhs_item ',' {
                    $$ = support.mlhs_add(support.mlhs_new(), $1);
                }
                | mlhs_head mlhs_item ',' {
                    $$ = support.mlhs_add($1, $2);
                }

// Set of mlhs terms at end of mlhs (a, *b, d, e = arr  # d,e is post)
mlhs_post       : mlhs_item {
                    $$ = support.mlhs_add(support.mlhs_new(), $1);
                }
                | mlhs_post ',' mlhs_item {
                    $$ = support.mlhs_add($1, $3);
                }

mlhs_node       : user_variable {
                    $$ = support.assignable($1, null);
                }
		| keyword_variable {
                    $$ = support.assignable($1, null);
                }
                | primary_value '[' opt_call_args rbracket {
                    $$ = support.dispatch("on_aref_field", $1, support.escape($3));
                }
                | primary_value tDOT tIDENTIFIER {
                    $$ = support.dispatch("on_field", $1, support.symbol('.'), $3);
                    
                }
                | primary_value tCOLON2 tIDENTIFIER {
                    $$ = support.dispatch("on_const_path_field", $1, $3);
                }
                | primary_value tDOT tCONSTANT {
		    $$ = support.dispatch('on_field', $1, support.symbol('.'), $3);
                }
                | primary_value tCOLON2 tCONSTANT {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    $$ = support.dispatch('on_const_path_field', $1, $3);
                }
                | tCOLON3 tCONSTANT {
                    $$ = support.dispatch("on_top_const_field", $2);
                }
                | backref {
                    $$ = support.dispatch("on_assign_error", 
                                          support.dispatch("on_var_field", $1));
                }

lhs             : user_variable {
                    $$ = support.dispatch("on_var_field", 
                                          support.assignable($1, null));
                }
		| keyword_variable {
                    $$ = support.dispatch("on_var_field", 
                                          support.assignable($1, null));
                }
                | primary_value '[' opt_call_args rbracket {
                    $$ = support.dispatch("on_aref_field", $1, support.escape($3));
                }
                | primary_value tDOT tIDENTIFIER {
                    $$ = support.dispatch("on_field", $1, support.symbol('.'), $3);
                }
                | primary_value tCOLON2 tIDENTIFIER {
                    $$ = support.dispatch("on_field", $1, support.intern("::"), $3);
                }
                | primary_value tDOT tCONSTANT {
                    $$ = support.dispatch("on_field", $1, support.symbol('.'), $3);
                }
                | primary_value tCOLON2 tCONSTANT {
                    $$ = support.dispatch("on_const_path_field", $1, $3);

                    if (support.isInDef() || support.isInSingle()) {
                        $$ = support.dispatch("on_assign_error", $$);
                    }
                }
                | tCOLON3 tCONSTANT {
                    $$ = support.dispatch("on_top_const_field", $2);

                    if (support.isInDef() || support.isInSingle()) {
                        $$ = support.dispatch("on_assign_error", $$);
                    }
                }
                | backref {
                    $$ = support.dispatch("on_assign_error", $1);
                }

cname           : tIDENTIFIER {
                    $$ = support.dispatch("on_class_name_error", $1);
                }
                | tCONSTANT

cpath           : tCOLON3 cname {
                    $$ = support.dispatch("on_top_const_ref", $2);
                }
                | cname {
                    $$ = support.dispatch("on_const_ref", $1);
                }
                | primary_value tCOLON2 cname {
                    $$ = support.dispatch("on_const_path_ref", $1, $3);
                }

// Token:fname - A function name [!null]
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
 fsym          : fname {
                   $$ = $1;
               }
               | symbol {
                   $$ = $1;
               }

// Node:fitem
fitem           : fsym {
                   $$ = support.dispatch("on_symbol_literal", $1);
                }
                | dsym {
                   $$ = $1;
                }

undef_list      : fitem {
                    $$ = support.new_array($1);
                }
                | undef_list ',' {
                    lexer.setState(LexState.EXPR_FNAME);
                } fitem {
                    $$ = $1.append($4);
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
                    $$ = support.dispatch("on_assign", $1, $3);
                }
                | lhs '=' arg kRESCUE_MOD arg {
                    $$ = support.dispatch("on_assign", $1, 
                                          support.dispatch("on_rescue_mod", 
                                                           $3, $5));
                }
                | var_lhs tOP_ASGN arg {
                    $$ = support.dispatch("on_opassign", $1, $2, $3);
                }
                | var_lhs tOP_ASGN arg kRESCUE_MOD arg {
                    $$ = support.dispatch("on_opassign", $1, $2,
                                          support.dispatch("on_rescue_mod", 
                                                           $3, $5));
                }
                | primary_value '[' opt_call_args rbracket tOP_ASGN arg {
                    $$ = support.dispatch("on_opassign", 
                                          support.dispatch("on_aref_field", 
                                                           $1, 
                                                           support.escape($3)),
                                          $5, $6);
                }
                | primary_value tDOT tIDENTIFIER tOP_ASGN arg {
                    $$ = support.dispatch("on_opassign", 
                                          support.dispatch("on_field", $1, 
                                                           support.symbol('.'),
                                                           $3),
                                          $4, $5);
                }
                | primary_value tDOT tCONSTANT tOP_ASGN arg {
                    $$ = support.dispatch("on_opassign", 
                                          support.dispatch("on_field", 
                                                           $1, 
                                                           support.symbol('.'),
                                                           $3),
                                          $4, $5);
                }
                | primary_value tCOLON2 tIDENTIFIER tOP_ASGN arg {
                    $$ = support.dispatch("on_opassign", 
                                          support.dispatch("on_field",
                                                           $1,
                                                           support.intern("::"),
                                                           $3),
                                          $4, $5);
                }
                | primary_value tCOLON2 tCONSTANT tOP_ASGN arg {
                    $$ = support.dispatch("on_assign_error", 
                                          support.dispatch("on_opassign", 
                                                           support.dispatch("on_const_path_field",
                                                                            $1,
                                                                            $3),
                                                           $4, $5));
                }
                | tCOLON3 tCONSTANT tOP_ASGN arg {
                    $$ = support.dispatch("on_assign_error", 
                                          support.dispatch("on_opassign", 
                                                           support.dispatch("on_top_const_field",
                                                                            $2),
                                                           $3,
                                                           $4));
                }
                | backref tOP_ASGN arg {
                    $$ = support.dispatch("on_assign_error", 
                                          support.dispatch("on_opassign",
                                                           support.dispatch("on_var_field",
                                                                            $1),
                                                           $2, $3));
                }
                | arg tDOT2 arg {
                    $$ = support.dispatch("on_dot2", $1, $3);
                }
                | arg tDOT3 arg {
                    $$ = support.dispatch("on_dot3", $1, $3);
                }
                | arg tPLUS arg {
                    $$ = support.dispatch("on_binary", $1, support.symbol('+'), $3);
                }
                | arg tMINUS arg {
                    $$ = support.dispatch("on_binary", $1, support.symbol('-'), $3);
                }
                | arg tSTAR2 arg {
                    $$ = support.dispatch("on_binary", $1, support.symbol('*'), $3);
                }
                | arg tDIVIDE arg {
                    $$ = support.dispatch("on_binary", $1, support.symbol('/'), $3);
                }
                | arg tPERCENT arg {
                    $$ = support.dispatch("on_binary", $1, support.symbol('%'), $3);
                }
                | arg tPOW arg {
                    $$ = support.dispatch("on_binary", $1, support.intern("**"), $3);
                }
                | tUMINUS_NUM tINTEGER tPOW arg {
                    $$ = support.dispatch("on_unary", 
                                          support.intern("-@"), 
                                          support.dispatch("on_binary", 
                                                           $2, 
                                                           support.intern("**"),
                                                           $4));
                }
                | tUMINUS_NUM tFLOAT tPOW arg {
                    $$ = support.dispatch("on_unary", 
                                          support.intern("-@"), 
                                          support.dispatch("on_binary", 
                                                           $2, 
                                                           support.intern("**"),
                                                           $4));
                }
                | tUPLUS arg {
                    $$ = support.dispatch("on_unary", support.intern("+@"), $2);
                }
                | tUMINUS arg {
                    $$ = support.dispatch("on_unary", support.intern("-@"), $2);
                }
                | arg tPIPE arg {
                    $$ = support.dispatch("on_binary", $1, support.symbol('|'), $3);
                }
                | arg tCARET arg {
                    $$ = support.dispatch("on_binary", $1, support.symbol('^'), $3);
                }
                | arg tAMPER2 arg {
                    $$ = support.dispatch("on_binary", $1, support.symbol('&'), $3);
                }
                | arg tCMP arg {
                    $$ = support.dispatch("on_binary", $1, support.intern("<=>"), $3);
                }
                | arg tGT arg {
                    $$ = support.dispatch("on_binary", $1, support.symbol('>'), $3);
                }
                | arg tGEQ arg {
                    $$ = support.dispatch("on_binary", $1, support.internl("<="), $3);
                }
                | arg tLT arg {
                    $$ = support.dispatch("on_binary", $1, support.symbol('<'), $3);
                }
                | arg tLEQ arg {
                    $$ = support.dispatch("on_binary", $1, support.intern("<="), $3);
                }
                | arg tEQ arg {
                    $$ = support.dispatch("on_binary", $1, support.intern("=="), $3);
                }
                | arg tEQQ arg {
                    $$ = support.dispatch("on_binary", $1, support.intern("==="), $3);
                }
                | arg tNEQ arg {
                    $$ = support.dispatch("on_binary", $1, support.intern("!="), $3);
                }
                | arg tMATCH arg {
                    $$ = support.dispatch("on_binary", $1, support.intern("=~"), $3);
                }
                | arg tNMATCH arg {
                    $$ = support.dispatch("on_binary", $1, support.intern("!~"), $3);
                }
                | tBANG arg {
                    $$ = support.dispatch("on_unary", support.symbol('!'), $2);
                }
                | tTILDE arg {
                    $$ = support.dispatch("on_unary", support.symbol('~'), $2);
                }
                | arg tLSHFT arg {
                    $$ = support.dispatch("on_binary", $1, support.intern("<<"), $3);
                }
                | arg tRSHFT arg {
                    $$ = support.dispatch("on_binary", $1, support.intern(">>"), $3);
                }
                | arg tANDOP arg {
                    $$ = support.dispatch("on_binary", $1, support.intern("&&"), $3);
                }
                | arg tOROP arg {
                    $$ = support.dispatch("on_binary", $1, support.intern("||"), $3);
                }
                | kDEFINED opt_nl arg {
                    $$ = support.dispatch("on_defined", $3);
                }
                | arg '?' arg opt_nl ':' arg {
                    $$ = support.dispatch("on_ifop", $1, $3, $6);
                }
                | primary {
                    $$ = $1;
                }

arg_value       : arg {
                    $$ = $1;
                }

aref_args       : none
                | args trailer {
                    $$ = $1;
                }
                | args ',' assocs trailer {
                    $$ = support.arg_add_assocs($1, $3);
                }
                | assocs trailer {
                    $$ = support.arg_add_assocs(support.arg_new(), $1);
                }

paren_args      : tLPAREN2 opt_call_args rparen {
                    $$ = support.dispatch("on_arg_paren", support.escape($2));
                }

opt_paren_args  : none | paren_args

opt_call_args   : none
                | call_args
                | args ',' {
                    $$ = $1;
                }
                | args ',' assocs ',' {
                    $$ = support.arg_add_assocs($1, $3);
                }
                | assocs ',' {
                    $$ = support.arg_add_assocs(support.arg_new(), $1);
                }

// [!null]
call_args       : command {
                    $$ = support.arg_add(support.arg_new(), $1);
                }
                | args opt_block_arg {
                    $$ = support.arg_add_optblock($1, $2);
                }
                | assocs opt_block_arg {
                    $$ = support.arg_add_optblock(support.arg_add_assocs(support.arg_new(), 
                                                                         $1),
                                                  $2);
                }
                | args ',' assocs opt_block_arg {
                    $$ = support.arg_add_optblock(support.arg_add_assocs($1, $3), $4);
                }
                | block_arg {
                    $$ = support.arg_add_block(support.arg_new(), $1);
                }

command_args    : /* none */ {
                    $$ = Long.valueOf(lexer.getCmdArgumentState().begin());
                } call_args {
                    lexer.getCmdArgumentState().reset($<Long>1.longValue());
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
                    $$ = support.arg_add(support.arg_new(), $1);
                }
                | tSTAR arg_value {
                    $$ = support.arg_add_star(support.arg_new(), $2);
                }
                | args ',' arg_value {
                    $$ = support.arg_add($1, $3);
                }
                | args ',' tSTAR arg_value {
                    $$ = support.arg_add_star($1, $4);
                }

mrhs            : args ',' arg_value {
                    $$ = support.mrhs_add(support.dispatch("on_mrhs_new_from_args", $1), $3);
                }
                | args ',' tSTAR arg_value {
                    $$ = support.mrhs_add_star(support.dispatch("on_mrhs_new_from_args", $1), $4);
                }
                | tSTAR arg_value {
                    $$ = support.mrhs_add_star(mrhs_new(), $2);
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
                    $$ = support.method_arg(support.dispatch("on_fcall", $1), 
                                            support.arg_new());
                }
                | kBEGIN bodystmt kEND {
                    $$ = support.dispatch("on_begin", $3);
                }
                | tLPAREN_ARG expr {
                    lexer.setState(LexState.EXPR_ENDARG); 
                } rparen {
                    support.warning(ID.GROUPED_EXPRESSION, $1.getPosition(), "(...) interpreted as grouped expression");
                    $$ = support.dispatch("on_paren", $2);
                }
                | tLPAREN compstmt tRPAREN {
                    $$ = support.dispatch("on_paren", $2);
                }
                | primary_value tCOLON2 tCONSTANT {
                    $$ = support.dispatch("on_const_path_ref", $1, $3);
                }
                | tCOLON3 tCONSTANT {
                    $$ = support.dispatch("on_top_const_ref", $2);
                }
                | tLBRACK aref_args tRBRACK {
                    $$ = support.dispatch("on_array", support.escape($2));
                }
                | tLBRACE assoc_list tRCURLY {
                    $$ = support.dispatch("on_hash", escape($2));
                }
                | kRETURN {
                    $$ = support.dispatch("on_return0");
                }
                | kYIELD tLPAREN2 call_args rparen {
                    $$ = support.dispatch("on_yield", 
                                          support.dispatch("on_paren", $3));
                }
                | kYIELD tLPAREN2 rparen {
                    $$ = support.dispatch("on_yield", 
                                          support.dispatch("on_paren", 
                                                           support.arg_new()));
                }
                | kYIELD {
                    $$ = support.dispatch("on_yield0");
                }
                | kDEFINED opt_nl tLPAREN2 expr rparen {
                    $$ = support.dispatch("on_defined", $5);
                }
                | kNOT tLPAREN2 expr rparen {
                    $$ = support.dispatch("on_unary", support.intern("not"), $3);
                }
                | kNOT tLPAREN2 rparen {
                    $$ = support.dispatch("on_unary", support.intern("not"), null); //FIXME: null should be nil
                }
                | operation brace_block {
                    $$ = support.method_add_block(support.method_arg(support.dispatch("on_fcall", 
                                                                                       $1), 
                                                                     support.arg_new()), 
                                                  $2);
                }
                | method_call
                | method_call brace_block {
                    $$ = support.method_add_block($1, $2);
                }
                | tLAMBDA lambda {
                    $$ = $2;
                }
                | kIF expr_value then compstmt if_tail kEND {
                    $$ = support.dispatch("on_if", $2, $4, supoprt.escape($5));
                }
                | kUNLESS expr_value then compstmt opt_else kEND {
                    $$ = support.dispatch("on_unless", $2, $4, supoprt.escape($5));
                }
                | kWHILE {
                    lexer.getConditionState().begin();
                } expr_value do {
                    lexer.getConditionState().end();
                } compstmt kEND {
                    $$ = support.dispatch("on_while", $3, $6);
                }
                | kUNTIL {
                  lexer.getConditionState().begin();
                } expr_value do {
                  lexer.getConditionState().end();
                } compstmt kEND {
                    $$ = support.dispatch("on_until", $3, $6);
                }
                | kCASE expr_value opt_terms case_body kEND {
                    $$ = support.dispatch("on_case", $2, $4);
                }
                | kCASE opt_terms case_body kEND {
                    $$ = support.dispatch("on_case", null, $4);
                }
                | kFOR for_var kIN {
                    lexer.getConditionState().begin();
                } expr_value do {
                    lexer.getConditionState().end();
                } compstmt kEND {
                    $$ = support.dispatch("on_for", $2, $5, $8);
                }
                | kCLASS cpath superclass {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("class definition in method body");
                    }
                    support.pushLocalScope();
                } bodystmt kEND {
                    $$ = support.dispatch("on_class", $2, $3, $5);
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
                    $$ = support.dispatch("on_sclass", $3, $7);

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
                    $$ = support.dispatch("on_module", $2, $4);
                    support.popCurrentScope();
                }
                | kDEF fname {
                    support.setInDef(true);
                    support.pushLocalScope();
                } f_arglist bodystmt kEND {
                    $$ = support.dispatch("on_def", $2, $4, $5);

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
                    $$ = support.dispatch("on_defs", $2, $3, $5, $7, $8);

                    support.popCurrentScope();
                    support.setInSingle(support.getInSingle() - 1);
                }
                | kBREAK {
                    $$ = support.dispatch("on_break", support.arg_new());
                }
                | kNEXT {
                    $$ = support.dispatch("on_next", support.arg_new());
                }
                | kREDO {
                    $$ = support.dispatch("on_redo");
                }
                | kRETRY {
                    $$ = support.dispatch("on_retry");
                }

primary_value   : primary {
                    $$ = $1;
                }

then            : term {
                    $$ = null;
                }
                | kTHEN
                | term kTHEN {
                    $$ = $2;
                }

do              : term {
                    $$ = null;
                }
                | kDO_COND

if_tail         : opt_else
                | kELSIF expr_value then compstmt if_tail {
                    $$ = support.dispatch("on_elsif", $2, $4, support.escape($5));
                }

opt_else        : none
                | kELSE compstmt {
                    $$ = support.dispatch("on_else", $2);
                }

for_var         : lhs
                | mlhs {
                }

f_marg          : f_norm_arg {
                    $$ = support.dispatch("on_mlhs_paren", 
                                          support.assignable($1, null));
                }
                | tLPAREN f_margs rparen {
                    $$ = support.dispatch("on_mlhs_paren", $2);
                }

// [!null]
f_marg_list     : f_marg {
                    $$ = support.mlhs_add(support.mlhs_new(), $1);
                }
                | f_marg_list ',' f_marg {
                    $$ = support.mlhs_add($1, $3);
                }

f_margs         : f_marg_list {
                    $$ = $1;
                }
                | f_marg_list ',' tSTAR f_norm_arg {
                    $$ = support.mlhs_add_star($1, 
                                               support.assignable($4, null));
                }
                | f_marg_list ',' tSTAR f_norm_arg ',' f_marg_list {
                    $$ = support.mlhs_add_star($1, 
                                               support.assignable($4, null));
                }
                | f_marg_list ',' tSTAR {
                    $$ = support.mlhs_add_star($1, null);
                }
                | f_marg_list ',' tSTAR ',' f_marg_list {
                    $$ = support.mlhs_add_star($1, $5);
                }
                | tSTAR f_norm_arg {
                    $$ = support.mlhs_add_star(support.mlhs_new(),
                                               support.assignable($2, null));
                }
                | tSTAR f_norm_arg ',' f_marg_list {
                    $$ = support.mlhs_add_star(support.assignable($2, null),
                                               $4);
                }
                | tSTAR {
                    $$ = support.mlhs_add_star(support.mlhs_new(), null);
                }
                | tSTAR ',' f_marg_list {
                    $$ = support.mlhs_add_star(suuport.mlhs_new(), null);
                }

// [!null]
block_param     : f_arg ',' f_block_optarg ',' f_rest_arg opt_f_block_arg {
                    $$ = support.params_new($1, $3, $5, null, support.escape($6));
                }
                | f_arg ',' f_block_optarg ',' f_rest_arg ',' f_arg opt_f_block_arg {
                    $$ = support.params_new($1, $3, $5, $7, support.escape($8));
                }
                | f_arg ',' f_block_optarg opt_f_block_arg {
                    $$ = support.params_new($1, $3, null, null, support.escape($4));
                }
                | f_arg ',' f_block_optarg ',' f_arg opt_f_block_arg {
                    $$ = support.params_new($1, $3, null, $5, support.escape($6));
                }
                | f_arg ',' f_rest_arg opt_f_block_arg {
                    $$ = support.params_new($1, null, $3, null, support.escape($4));
                }
                | f_arg ',' {
                    $$ = support.dispatch("on_excessed_comma", 
                                          support.params_new($1, null, null, null, null));
                }
                | f_arg ',' f_rest_arg ',' f_arg opt_f_block_arg {
                    $$ = support.params_new($1, null, $3, $5, support.escape($6));
                }
                | f_arg opt_f_block_arg {
                    $$ = support.params_new($1, null, null, null, support.escape($2));
                }
                | f_block_optarg ',' f_rest_arg opt_f_block_arg {
                    $$ = support.params_new(null, $1, $3, null, support.escape($4));
                }
                | f_block_optarg ',' f_rest_arg ',' f_arg opt_f_block_arg {
                    $$ = support.params_new(null, $1, $3, $5, support.escape($6));
                }
                | f_block_optarg opt_f_block_arg {
                    $$ = support.params_new(null, $1, null, null, support.escape($2));
                }
                | f_block_optarg ',' f_arg opt_f_block_arg {
                    $$ = support.params_new(null, $1, null, $3, support.escape($4));
                }
                | f_rest_arg opt_f_block_arg {
                    $$ = support.params_new(null, null, $1, null, support.escape($2));     
                }
                | f_rest_arg ',' f_arg opt_f_block_arg {
                    $$ = support.params_new(null, null, $1, $3, support.escape($4));
                }
                | f_block_arg {
                    $$ = support.params_new(null, null, null, null, $1);
                }

opt_block_param : none 
                | block_param_def {
                    lexer.commandStart = true;
                }

block_param_def : tPIPE opt_bv_decl tPIPE {
                    $$ = support.blockvar_new(support.params_new(null, null, null, null, null), support.escape($2));
                }
                | tOROP {
                    $$ = support.blockvar_new(support.params_new(null, null, null, null, null), null);
                }
                | tPIPE block_param opt_bv_decl tPIPE {
                    $$ = support.blockvar_new(support.escape($2), support.escape($3));
                }

// shadowed block variables....
opt_bv_decl     : none
                | ';' bv_decls {
                    $$ = $2;
                }

// ENEBO: This is confusing...
bv_decls        : bvar {
                    $$ = support.new_array($1);
                }
                | bv_decls ',' bvar {
                    $$ = $1.append($3);
                }

bvar            : tIDENTIFIER {
                    support.new_bv(support.get_id($1));
                    $$ = support.get_value($1);
                }
                | f_bad_arg {
                    $$ = null;
                }

lambda          : /* none */  {
                    support.pushBlockScope();
                    $$ = lexer.getLeftParenBegin();
                    lexer.setLeftParenBegin(lexer.incrementParenNest());
                } f_larglist lambda_body {
                    $$ = support.dispatch("on_lambda", $2, $3);
                    support.popCurrentScope();
                    lexer.setLeftParenBegin($<Integer>1);
                }

f_larglist      : tLPAREN2 f_args opt_bv_decl tRPAREN {
                    $$ = $2;
                    $<ISourcePositionHolder>$.setPosition($1.getPosition());
                }
                | f_args opt_bv_decl {
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
                    $$ = support.dispatch("on_do_block", support.escape($3), $4);
                    support.popCurrentScope();
                }

block_call      : command do_block {
                    $$ = support.method_add_block($1, $2);
                }
                | block_call tDOT operation2 opt_paren_args {
                    $$ = support.method_optarg(support.dispatch("on_call", 
                                                                $1,
                                                                support.symbol('.'),
                                                                $3),
                                               $4);
                }
                | block_call tCOLON2 operation2 opt_paren_args {
                    $$ = support.method_optarg(support.dispatch("on_call", 
                                                                $1,
                                                                support.intern("::"),
                                                                $3),
                                               $4);
                }

// [!null]
method_call     : operation paren_args {
                    $$ = support.method_arg(support.dispatch("on_fcall", $1), $2);
                }
                | primary_value tDOT operation2 opt_paren_args {
                    $$ = support.method_optarg(support.dispatch("on_call", 
                                                                $1,
                                                                support.symbol('.'),
                                                                $3),
                                               $4);
                }
                | primary_value tCOLON2 operation2 paren_args {
                    $$ = support.method_optarg(support.dispatch("on_call",
                                                                $1,
                                                                support.symbol('.'),
                                                                $3),
                                               $4);
                }
                | primary_value tCOLON2 operation3 {
                    $$ = support.dispatch("on_call", $1, support.intern("::"), $3);
                }
                | primary_value tDOT paren_args {
                    $$ = support.method_optarg(support.dispatch("on_call",
                                                                $1,
                                                                support.symbol('.'),
                                                                support.intern("call")),
                                               $3);
                }
                | primary_value tCOLON2 paren_args {
                    $$ = support.method_optarg(support.dispatch("on_call",
                                                                $1,
                                                                support.intern("::"),
                                                                support.intern("call")),
                                               $3);
                }
                | kSUPER paren_args {
                    $$ = support.dispatch("on_super", $2);
                }
                | kSUPER {
                    $$ = support.dispatch("on_zsuper");
                }
                | primary_value '[' opt_call_args rbracket {
                    $$ = support.dispatch("on_aref", $1, support.escape($3));
                }

brace_block     : tLCURLY {
                    support.pushBlockScope();
                } opt_block_param compstmt tRCURLY {
                    $$ = support.dispatch("on_brace_block", support.escape($3), $4);
                    support.popCurrentScope();
                }
                | kDO {
                    support.pushBlockScope();
                } opt_block_param compstmt kEND {
                    $$ = support.dispatch("on_do_block", support.escape($3), $4);
                    support.popCurrentScope();
                }

case_body       : kWHEN args then compstmt cases {
                    $$ = support.dispatch("on_when", $2, $4, support.escape($5));

                }

cases           : opt_else | case_body

opt_rescue      : kRESCUE exc_list exc_var then compstmt opt_rescue {
                    $$ = support.dispatch4("on_rescue",
				       support.escape($2),
				       support.escape($3),
				       support.escape($5),
				       support.escape($6));
                }
                | none

exc_list        : arg_value {
                    $$ = support.new_array($1);
                }
                | mrhs {
                    $$ = $1;
                }
                | none

exc_var         : tASSOC lhs {
                    $$ = $2;
                }
                | none

opt_ensure      : kENSURE compstmt {
                    $$ = support.dispatch("on_ensure", $2);
                }
                | none

literal         : numeric
                | symbol {
                    $$ = support.dispatch("on_symbol_literal", $1);
                }
                | dsym

strings         : string {
                    $$ = $1;
                }

// [!null]
string          : tCHAR 
                | string1
                | string string1 {
                    $$ = support.dispatch("on_string_concat", $1, $2);
                }

string1         : tSTRING_BEG string_contents tSTRING_END {
                    $$ = support.dispatch("on_string_literal", $2);
                }

xstring         : tXSTRING_BEG xstring_contents tSTRING_END {
                    $$ = support.dispatch("on_xstring_literal", $2);
                }

regexp          : tREGEXP_BEG xstring_contents tREGEXP_END {
                    $$ = support.dispatch("on_regexp_literal", $2, $3);
                }

words           : tWORDS_BEG ' ' tSTRING_END {
                    $$ = support.dispatch("on_array", 
                                          support.dispatch("on_words_new"));
                }
                | tWORDS_BEG word_list tSTRING_END {
                    $$ = support.dispatch("on_array", $2);
                }

word_list       : /* none */ {
                    $$ = support.dispatch("on_words_new");
                }
                | word_list word ' ' {
                    $$ = support.dispatch("on_words_add", $1, $2);
                }

 word            : string_content {
                    $$ = support.dispatch("on_word_add",
                                          support.dispatch("on_word_new"),
                                          $1);
                 }
                | word string_content {
                    $$ = support.dispatch("on_word_add", $1, $2);
                }

qwords          : tQWORDS_BEG ' ' tSTRING_END {
                    $$ = support.dispatch("on_array", 
                                          support.dispatch("on_qwords_new"));
                }
                | tQWORDS_BEG qword_list tSTRING_END {
                    $$ = support.dispatch("on_array", $2);
                }

qword_list      : /* none */ {
                    $$ = support.dispatch("on_qwords_new");
                }
                | qword_list tSTRING_CONTENT ' ' {
                    $$ = support.dispatch("on_qwords_add", $1, $2);
                }

string_contents : /* none */ {
                    $$ = support.dispatch("on_string_content");
                }
                | string_contents string_content {
                    $$ = support.dispatch("on_string_add", $1, $2);
                }

xstring_contents: /* none */ {
                    $$ = support.dispatch("on_xstring_new");
                }
                | xstring_contents string_content {
                    $$ = support.dispatch("on_xstring_add", $1, $2);
                }

regexp_contents: /* none */ {
                    $$ = support.dispatch("on_regexp_new");
                }
                | regexp_contents string_content {
                    $$ = support.dispatch("on_regexp_add", $1, $2);
                }

string_content  : tSTRING_CONTENT
                | tSTRING_DVAR {
                    $$ = lexer.getStrTerm();
                    lexer.setStrTerm(null);
                    lexer.setState(LexState.EXPR_BEG);
                } string_dvar {
                    lexer.setStrTerm($<StrTerm>2);
                    $$ = support.dispatch("on_string_dvar", $3);
                }
                | tSTRING_DBEG {
                   $$ = lexer.getStrTerm();
                   lexer.getConditionState().stop();
                   lexer.getCmdArgumentState().stop();
                   lexer.setStrTerm(null);
                   lexer.setState(LexState.EXPR_BEG);
                } compstmt tRCURLY {
                   lexer.getConditionState().restart();
                   lexer.getCmdArgumentState().restart();
                   lexer.setStrTerm($<StrTerm>2);
                   $$ = support.dispatch("on_string_embexpr", $4);
                }

string_dvar     : tGVAR {
                   $$ = support.dispatch("on_var_ref", $1);
                }
                | tIVAR {
                   $$ = support.dispatch("on_var_ref", $1);
                }
                | tCVAR {
                   $$ = support.dispatch("on_var_ref", $1);
                }
                | backref

// Token:symbol
symbol          : tSYMBEG sym {
                     lexer.setState(LexState.EXPR_END);
                     $$ = support.dispatch("on_symbol", $2);
                }

// Token:symbol
sym             : fname | tIVAR | tGVAR | tCVAR

dsym            : tSYMBEG xstring_contents tSTRING_END {
                     lexer.setState(LexState.EXPR_END);
                     $$ = support.dispatch("on_dyna_symbol", $2);
                }

numeric         : tINTEGER {
                    $$ = $1;
                }
                | tFLOAT {
                    $$ = $1;
                }
                | tUMINUS_NUM tINTEGER %prec tLOWEST {
                    $$ = support.dispatch("on_unary", support.intern("-@"), $2);
                }
                | tUMINUS_NUM tFLOAT %prec tLOWEST {
                    $$ = support.dispatch("on_unary", support.intern("-@"), $2);
                }

user_variable	: tIDENTIFIER
		| tIVAR
		| tGVAR
		| tCONSTANT
		| tCVAR

// [!null]
keyword_variable : kNIL
                 | kSELF
                 | kTRUE
                 | kFALSE
                 | k__FILE__
                 | k__LINE__
                 | k__ENCODING__

// [!null]
var_ref         : user_variable {
                    if (support.id_is_var(support.get_id($1))) {
                        $$ = support.dispatch("on_var_ref", $1);
                    } else {
                        $$ = support.dispatch("on_vcall", $1);
                    }
                }
                | keyword_variable {
                    $$ = support.dispatch("on_var_ref", $1);
                }

// [!null]
var_lhs         : user_variable {
                    $$ = support.dispatch("on_var_field", 
                                          support.assignable($1, null));
                }
                | keyword_variable {
                    $$ = support.dispatch("on_var_field", 
                                          support.assignable($1, null));
                }

// [!null]
backref         : tNTH_REF
                | tBACK_REF

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
                    $<ISourcePositionHolder>$.setPosition($1.getPosition());
                    lexer.setState(LexState.EXPR_BEG);
                }
                | f_args term {
                    $$ = $1;
                }

// [!null]
f_args          : f_arg ',' f_optarg ',' f_rest_arg opt_f_block_arg {
                    $$ = support.params_new($1, $3, $5, null, support.escape($6));
                }
                | f_arg ',' f_optarg ',' f_rest_arg ',' f_arg opt_f_block_arg {
                    $$ = support.params_new($1, $3, $5, $7, support.escape($8));
                }
                | f_arg ',' f_optarg opt_f_block_arg {
                    $$ = support.params_new($1, $3, null, null, support.escape($4));
                }
                | f_arg ',' f_optarg ',' f_arg opt_f_block_arg {
                    $$ = support.params_new($1, $3, null, $5, support.escape($6));
                }
                | f_arg ',' f_rest_arg opt_f_block_arg {
                    $$ = support.params_new($1, null, $3, null, support.escape($4));
                }
                | f_arg ',' f_rest_arg ',' f_arg opt_f_block_arg {
                    $$ = support.params_new($1, null, $3, $5, support.escape($6));
                }
                | f_arg opt_f_block_arg {
                    $$ = support.params_new($1, null, null, null,support.escape($2));
                }
                | f_optarg ',' f_rest_arg opt_f_block_arg {
                    $$ = support.params_new(null, $1, $3, null, support.escape($4));
                }
                | f_optarg ',' f_rest_arg ',' f_arg opt_f_block_arg {
                    $$ = support.params_new(null, $1, $3, $5, support.escape($6));
                }
                | f_optarg opt_f_block_arg {
                    $$ = support.params_new(null, $1, null, null,support.escape($2));
                }
                | f_optarg ',' f_arg opt_f_block_arg {
                    $$ = support.params_new(null, $1, null, $3, support.escape($4));
                }
                | f_rest_arg opt_f_block_arg {
                    $$ = support.params_new(null, null, $1, null,support.escape($2));
                }
                | f_rest_arg ',' f_arg opt_f_block_arg {
                    $$ = support.params_new(null, null, $1, $3, support.escape($4));
                }
                | f_block_arg {
                    $$ = support.params_new(null, null, null, null, $1);
                }
                | /* none */ {
                    $$ = support.params_new(null, null, null, null, null);
                }

f_bad_arg       : tCONSTANT {
                    $$ = support.dispatch("on_param_error", $1);
                }
                | tIVAR {
                    $$ = support.dispatch("on_param_error", $1);
                }
                | tGVAR {
                    $$ = support.dispatch("on_param_error", $1);
                }
                | tCVAR {
                    $$ = support.dispatch("on_param_error", $1);
                }

// Token:f_norm_arg [!null]
f_norm_arg      : f_bad_arg
                | tIDENTIFIER {
                    $$ = support.formal_argument($1);
                }

f_arg_item      : f_norm_arg {
                    $$ = support.arg_var($1);
                }
                | tLPAREN f_margs rparen {
                    $$ = support.dispatch("on_mlhs_paren", $2);
                }

// [!null]
f_arg           : f_arg_item {
                    $$ = support.new_array($1);
                }
                | f_arg ',' f_arg_item {
                    $$ = $1.append($3)
                }

f_opt           : tIDENTIFIER '=' arg_value {
                    support.arg_var(support.formal_argument($1));
                    $$ = support.new_assoc(support.assignable($1, $3), $3);

                }

f_block_opt     : tIDENTIFIER '=' primary_value {
                    support.arg_var(support.formal_argument($1));
                    $$ = support.new_assoc(support.assignable($1, $3), $3);
                }

f_block_optarg  : f_block_opt {
                    $$ = support.new_array($1);
                }
                | f_block_optarg ',' f_block_opt {
                    $$ = $1.append($3);
                }

f_optarg        : f_opt {
                    $$ = support.new_array($1);
                }
                | f_optarg ',' f_opt {
                    $$ = $1.append($3);
                }

restarg_mark    : tSTAR2 | tSTAR

// [!null]
f_rest_arg      : restarg_mark tIDENTIFIER {
                    support.arg_var(support.shadowing_lvar($2));
                    $$ = support.dispatch("on_rest_param", $2);
                }
                | restarg_mark {
                    $$ = support.dispatch("on_rest_param", null);
                }

// [!null]
blkarg_mark     : tAMPER2 | tAMPER

// f_block_arg - Block argument def for function (foo(&block)) [!null]
f_block_arg     : blkarg_mark tIDENTIFIER {
                    support.arg_var(support.shadowing_lvar($2));
                    $$ = support.dispatch("on_blockarg", $2);
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
                    lexer.setState(LexState.EXPR_BEG);
                } expr rparen {
                    $$ = support.dispatch("on_paren", $3);
                }

// [!null]
assoc_list      : none
                | assocs trailer {
                    $$ = support.dispatch("on_assoclist_from_args", $1);
                }

// [!null]
assocs          : assoc {
                    $$ = support.new_array($1);
                }
                | assocs ',' assoc {
                    $$ = $1.append($3);
                }

// [!null]
assoc           : arg_value tASSOC arg_value {
                    $$ = support.dispatch("on_assoc_new", $1, $3);
                }
                | tLABEL arg_value {
                    $$ = support.dispatch("on_assoc_new", $1, $2);
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
    public RubyParserResult parse(ParserConfiguration configuration, LexerSource source) throws IOException {
        support.reset();
        support.setConfiguration(configuration);
        support.setResult(new RubyParserResult());
        
        lexer.reset();
        lexer.setSource(source);
        lexer.setEncoding(configuration.getDefaultEncoding());

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
        
        return support.getResult();
    }
}
