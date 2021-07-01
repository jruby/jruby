%{
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
import org.jruby.ast.DefinedNode;
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
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.KeywordArgNode;
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
import org.jruby.lexer.yacc.RubyLexer;
import org.jruby.lexer.yacc.StackState;
import org.jruby.lexer.yacc.StrTerm;
import org.jruby.util.ByteList;
import org.jruby.util.CommonByteLists;
import org.jruby.util.KeyValuePair;
import org.jruby.util.StringSupport;
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
%}

// patch_parser.rb will look for token lines with {{ and }} within it to put
// in reasonable strings we expect during a parsing error.
%token <Integer> keyword_class        /* {{class}} */
%token <Integer> keyword_module       /* {{module}} */
%token <Integer> keyword_def          /* {{def}} */
%token <Integer> keyword_undef        /* {{undef}} */
%token <Integer> keyword_begin        /* {{begin}} */
%token <Integer> keyword_rescue       /* {{rescue}} */
%token <Integer> keyword_ensure       /* {{ensure}} */
%token <Integer> keyword_end          /* {{end}} */
%token <Integer> keyword_if           /* {{if}} */
%token <Integer> keyword_unless       /* {{unless}} */
%token <Integer> keyword_then         /* {{then}} */
%token <Integer> keyword_elsif        /* {{elsif}} */
%token <Integer> keyword_else         /* {{else}} */
%token <Integer> keyword_case         /* {{case}} */
%token <Integer> keyword_when         /* {{when}} */
%token <Integer> keyword_while        /* {{while}} */
%token <Integer> keyword_until        /* {{until}} */
%token <Integer> keyword_for          /* {{for}} */
%token <Integer> keyword_break        /* {{break}} */
%token <Integer> keyword_next         /* {{next}} */
%token <Integer> keyword_redo         /* {{redo}} */
%token <Integer> keyword_retry        /* {{retry}} */
%token <Integer> keyword_in           /* {{in}} */
%token <Integer> keyword_do           /* {{do}} */
%token <Integer> keyword_do_cond      /* {{do (for condition)}} */
%token <Integer> keyword_do_block     /* {{do (for block)}} */
%token <Integer> keyword_return       /* {{return}} */
%token <Integer> keyword_yield        /* {{yield}} */
%token <Integer> keyword_super        /* {{super}} */
%token <Integer> keyword_self         /* {{self}} */
%token <Integer> keyword_nil          /* {{nil}} */
%token <Integer> keyword_true         /* {{true}} */
%token <Integer> keyword_false        /* {{false}} */
%token <Integer> keyword_and          /* {{and}} */
%token <Integer> keyword_or           /* {{or}} */
%token <Integer> keyword_not          /* {{not}} */
%token <Integer> modifier_if          /* {{if (modifier)}} */
%token <Integer> modifier_unless      /* {{unless (modifier)}} */
%token <Integer> modifier_while       /* {{while (modifier)}} */
%token <Integer> modifier_until       /* {{until (modifier)}} */
%token <Integer> modifier_rescue      /* {{rescue (modifier)}} */
%token <Integer> keyword_alias        /* {{alias}} */
%token <Integer> keyword_defined      /* {{defined}} */
%token <Integer> keyword_BEGIN        /* {{BEGIN}} */
%token <Integer> keyword_END          /* {{END}} */
%token <Integer> keyword__LINE__      /* {{__LINE__}} */
%token <Integer> keyword__FILE__      /* {{__FILE__}} */
%token <Integer> keyword__ENCODING__  /* {{__ENCODING__}} */
%token <Integer> keyword_do_lambda    /* {{do (for lambda)}} */
%token <ByteList> tIDENTIFIER         
%token <ByteList> tFID
%token <ByteList> tGVAR
%token <ByteList> tIVAR
%token <ByteList> tCONSTANT
%token <ByteList> tCVAR
%token <ByteList> tLABEL
%token <StrNode> tCHAR
%token <ByteList> tUPLUS               /* {{unary+}} */
%token <ByteList> tUMINUS              /* {{unary-}} */
%token <ByteList> tUMINUS_NUM
%token <ByteList> tPOW                 /* {{**}} */
%token <ByteList> tCMP                 /* {{<=>}} */
%token <ByteList> tEQ                  /* {{==}} */
%token <ByteList> tEQQ                 /* {{===}} */
%token <ByteList> tNEQ                 /* {{!=}} */
%token <ByteList> tGEQ                 /* {{>=}} */
%token <ByteList> tLEQ                 /* {{<=}} */
%token <ByteList> tANDOP               /* {{&&}}*/
%token <ByteList> tOROP                /* {{||}} */
%token <ByteList> tMATCH               /* {{=~}} */
%token <ByteList> tNMATCH              /* {{!~}} */
%token <ByteList> tDOT                 /* {{.}} -  '.' in ruby and not a token */
%token <ByteList> tDOT2                /* {{..}} */
%token <ByteList> tDOT3                /* {{...}} */
%token <ByteList> tAREF                /* {{[]}} */
%token <ByteList> tASET                /* {{[]=}} */
%token <ByteList> tLSHFT               /* {{<<}} */
%token <ByteList> tRSHFT               /* {{>>}} */
%token <ByteList> tANDDOT              /* {{&.}} */
%token <ByteList> tCOLON2              /* {{::}} */
%token <ByteList> tCOLON3              /* {{:: at EXPR_BEG}} */
%token <ByteList> tOP_ASGN             /* +=, -=  etc. */
%token <ByteList> tASSOC               /* {{=>}} */
%token <Integer> tLPAREN               /* {{(}} */
%token <Integer> tLPAREN2              /* {{(}} - '(' in ruby and not a token */
%token <ByteList> tRPAREN              /* {{)}} */
%token <Integer> tLPAREN_ARG           /* {{( arg}} */
%token <ByteList> tLBRACK              /* {{[}} */
%token <ByteList> tRBRACK              /* {{]}} */
%token <Integer> tLBRACE               /* {{{}} */
%token <Integer> tLBRACE_ARG           /* {{{ arg}} */
%token <ByteList> tSTAR                /* {{*}} */
%token <ByteList> tSTAR2               /* {{*}} - '*' in ruby and not a token */
%token <ByteList> tAMPER               /* {{&}} */
%token <ByteList> tAMPER2              /* {{&}} - '&' in ruby and not a token */
%token <ByteList> tTILDE               /* {{`}} - '`' in ruby and not a token */
%token <ByteList> tPERCENT             /* {{%}} - '%' in ruby and not a token */
%token <ByteList> tDIVIDE              /* {{/}} - '/' in ruby and not a token */
%token <ByteList> tPLUS                /* {{+}} - '+' in ruby and not a token */
%token <ByteList> tMINUS               /* {{-}} - '-' in ruby and not a token */
%token <ByteList> tLT                  /* {{<}} - '<' in ruby and not a token */
%token <ByteList> tGT                  /* {{>}} - '>' in ruby and not a token */
%token <ByteList> tPIPE                /* {{|}} - '|' in ruby and not a token */
%token <ByteList> tBANG                /* {{!}} - '!' in ruby and not a token */
%token <ByteList> tCARET               /* {{^}} - '^' in ruby and not a token */
%token <Integer> tLCURLY               /* {{{}} - '{' in ruby and not a token */
%token <ByteList> tRCURLY              /* {{}}} - '}' in ruby and not a token */
%token <ByteList> tBACK_REF2           /* {{`}} - '`' in ruby and not a token */
%token <ByteList> tSYMBEG tSTRING_BEG tXSTRING_BEG tREGEXP_BEG tWORDS_BEG tQWORDS_BEG
%token <ByteList> tSTRING_DBEG tSTRING_DVAR tSTRING_END
%token <ByteList> tLAMBDA              /* {{->}} */
%token <ByteList> tLAMBEG
%token <Node> tNTH_REF tBACK_REF tSTRING_CONTENT tINTEGER tIMAGINARY
%token <FloatNode> tFLOAT  
%token <RationalNode> tRATIONAL
%token <RegexpNode>  tREGEXP_END

%type <ByteList> sym symbol operation operation2 operation3 op fname cname
%type <ByteList> f_norm_arg restarg_mark
%type <ByteList> dot_or_colon  blkarg_mark
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
%type <Node> expr_value expr_value_do primary_value opt_else cases if_tail exc_var rel_expr
%type <Node> call_args opt_ensure paren_args superclass
%type <Node> command_args var_ref opt_paren_args block_call block_command
%type <Node> command_rhs arg_rhs
%type <Node> f_opt
%type <Node> undef_list
%type <Node> string_dvar backref
%type <ArgsNode> f_args f_larglist block_param block_param_def opt_block_param
%type <Object> f_arglist
%type <Node> mrhs mlhs_item mlhs_node arg_value case_body exc_list aref_args
%type <Node> lhs none args
%type <ListNode> qword_list word_list
%type <ListNode> f_arg f_optarg
%type <ListNode> f_marg_list symbol_list
%type <ListNode> qsym_list symbols qsymbols
   // FIXME: These are node until a better understanding of underlying type
%type <ArgsTailHolder> opt_args_tail opt_block_args_tail block_args_tail args_tail
%type <Node> f_kw f_block_kw
%type <ListNode> f_block_kwarg f_kwarg
%type <HashNode> assoc_list
%type <HashNode> assocs
%type <KeyValuePair> assoc
%type <ListNode> mlhs_head mlhs_post
%type <ListNode> f_block_optarg
%type <BlockPassNode> opt_block_arg block_arg none_block_pass
%type <BlockArgNode> opt_f_block_arg f_block_arg
%type <IterNode> brace_block do_block cmd_brace_block brace_body do_body
%type <MultipleAsgnNode> mlhs mlhs_basic 
%type <RescueBodyNode> opt_rescue
%type <AssignableNode> var_lhs
%type <LiteralNode> fsym
%type <Node> fitem
%type <Node> f_arg_item
%type <Node> bv_decls
%type <Node> opt_bv_decl lambda_body 
%type <LambdaNode> lambda
%type <Node> mlhs_inner f_block_opt for_var
%type <Node> opt_call_args f_marg f_margs
%type <ByteList> bvar
%type <ByteList> reswords f_bad_arg relop
%type <ByteList> rparen rbracket 
%type <Node> top_compstmt top_stmts top_stmt
%token <ByteList> tSYMBOLS_BEG
%token <ByteList> tQSYMBOLS_BEG
%token <ByteList> tDSTAR                /* {{**arg}} */
%token <ByteList> tSTRING_DEND
%type <ByteList> kwrest_mark f_kwrest f_label 
%type <ByteList> call_op call_op2
%type <ArgumentNode> f_arg_asgn
%type <FCallNode> fcall
%token <ByteList> tLABEL_END
%type <Integer> k_return k_class k_module k_else

// For error reporting
%token <Integer> '\\'                   /* {{backslash}} */
%token <Integer> tSP                    /* {{escaped space}} */
%token <Integer> '\t'                   /* {{escaped horizontal tab}} */
%token <Integer> '\f'                   /* {{escaped form feed}} */
%token <Integer> '\r'                   /* {{escaped carriage return}} */
%token <Integer> '\v'                   /* {{escaped vertical tab}} */

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

%%
program       : {
                  lexer.setState(EXPR_BEG);
                  support.initTopLocalVariables();
              } top_compstmt {
                  if ($2 != null && !support.getConfiguration().isEvalParse()) {
                      /* last expression should not be void */
                      if ($2 instanceof BlockNode) {
                          support.void_expr($<BlockNode>2.getLast());
                      } else {
                          support.void_expr($2);
                      }
                  }
                  support.getResult().setAST(support.addRootNode($2));
              }

top_compstmt  : top_stmts opt_terms {
                  $$ = support.void_stmts($1);
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
              | keyword_BEGIN tLCURLY top_compstmt tRCURLY {
                    support.getResult().addBeginNode(new PreExe19Node($1, support.getCurrentScope(), $3, lexer.getRubySourceline()));
                    $$ = null;
              }

 bodystmt     : compstmt opt_rescue k_else {
                   if ($2 == null) support.yyerror("else without rescue is useless"); 
               } compstmt opt_ensure {
                   $$ = support.new_bodystmt($1, $2, $5, $6);
                }
                | compstmt opt_rescue opt_ensure {
                    $$ = support.new_bodystmt($1, $2, null, $3);
                }

compstmt        : stmts opt_terms {
                    $$ = support.void_stmts($1);
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
                | keyword_begin {
                   support.yyerror("BEGIN is permitted only at toplevel");
                } tLCURLY top_compstmt tRCURLY {
                    $$ = new BeginNode($1, support.makeNullNil($2));
                }

stmt            : keyword_alias fitem {
                    lexer.setState(EXPR_FNAME|EXPR_FITEM);
                } fitem {
                    $$ = ParserSupport.newAlias($1, $2, $4);
                }
                | keyword_alias tGVAR tGVAR {
                    $$ = new VAliasNode($1, support.symbolID($2), support.symbolID($3));
                }
                | keyword_alias tGVAR tBACK_REF {
                    $$ = new VAliasNode($1, support.symbolID($2), support.symbolID($<BackRefNode>3.getByteName()));
                }
                | keyword_alias tGVAR tNTH_REF {
                    support.yyerror("can't make alias for the number variables");
                }
                | keyword_undef undef_list {
                    $$ = $2;
                }
                | stmt modifier_if expr_value {
                    $$ = support.new_if(support.getPosition($1), support.cond($3), $1, null);
                    support.fixpos($<Node>$, $3);
                }
                | stmt modifier_unless expr_value {
                    $$ = support.new_if(support.getPosition($1), support.cond($3), null, $1);
                    support.fixpos($<Node>$, $3);
                }
                | stmt modifier_while expr_value {
                    if ($1 != null && $1 instanceof BeginNode) {
                        $$ = new WhileNode(support.getPosition($1), support.cond($3), $<BeginNode>1.getBodyNode(), false);
                    } else {
                        $$ = new WhileNode(support.getPosition($1), support.cond($3), $1, true);
                    }
                }
                | stmt modifier_until expr_value {
                    if ($1 != null && $1 instanceof BeginNode) {
                        $$ = new UntilNode(support.getPosition($1), support.cond($3), $<BeginNode>1.getBodyNode(), false);
                    } else {
                        $$ = new UntilNode(support.getPosition($1), support.cond($3), $1, true);
                    }
                }
                | stmt modifier_rescue stmt {
                    $$ = support.newRescueModNode($1, $3);
                }
                | keyword_END tLCURLY compstmt tRCURLY {
                    if (support.isInDef()) {
                       support.warn(ID.END_IN_METHOD, $1, "END in method; use at_exit");
                    }
                    $$ = new PostExeNode($1, $3, lexer.getRubySourceline());
                }
                | command_asgn
                | mlhs '=' command_call {
                    support.value_expr(lexer, $3);
                    $$ = node_assign($1, $3);
                }
                | lhs '=' mrhs {
                    support.value_expr(lexer, $3);
                    $$ = node_assign($1, $3);
                }
                | mlhs '=' mrhs_arg {
                    $$ = node_assign($1, $3);
                }
                | expr

command_asgn    : lhs '=' command_rhs {
                    support.value_expr(lexer, $3);
                    $$ = node_assign($1, $3);
                }
                | var_lhs tOP_ASGN command_rhs {
                    support.value_expr(lexer, $3);
                    $$ = support.new_op_assign($1, $2, $3);
                }
                | primary_value '[' opt_call_args rbracket tOP_ASGN command_rhs {
                    support.value_expr(lexer, $6);
                    $$ = support.new_ary_op_assign($1, $5, $3, $6);
                }
                | primary_value call_op tIDENTIFIER tOP_ASGN command_rhs {
                    support.value_expr(lexer, $5);
                    $$ = support.new_attr_op_assign($1, $2, $5, $3, $4);
                }
                | primary_value call_op tCONSTANT tOP_ASGN command_rhs {
                    support.value_expr(lexer, $5);
                    $$ = support.new_attr_op_assign($1, $2, $5, $3, $4);
                }
                | primary_value tCOLON2 tCONSTANT tOP_ASGN command_rhs {
                    int line = $1.getLine();
                    $$ = support.new_const_op_assign(line, support.new_colon2(line, $1, $3), $4, $5);
                }

                | primary_value tCOLON2 tIDENTIFIER tOP_ASGN command_rhs {
                    support.value_expr(lexer, $5);
                    $$ = support.new_attr_op_assign($1, $2, $5, $3, $4);
                }
                | backref tOP_ASGN command_rhs {
                    support.backrefAssignError($1);
                }

command_rhs     : command_call %prec tOP_ASGN {
                    support.value_expr(lexer, $1);
                    $$ = $1;
                }
		| command_call modifier_rescue stmt {
                    support.value_expr(lexer, $1);
                    $$ = support.newRescueModNode($1, $3);
                }
		| command_asgn
 

// Node:expr *CURRENT* all but arg so far
expr            : command_call
                | expr keyword_and expr {
                    $$ = support.newAndNode($1, $3);
                }
                | expr keyword_or expr {
                    $$ = support.newOrNode($1, $3);
                }
                | keyword_not opt_nl expr {
                    $$ = support.getOperatorCallNode(support.method_cond($3), lexer.BANG);
                }
                | tBANG command_call {
                    $$ = support.getOperatorCallNode(support.method_cond($2), $1);
                }
                | arg

expr_value      : expr {
                    support.value_expr(lexer, $1);
                }

expr_value_do   : {
                    lexer.getConditionState().push1();
                } expr_value do {
                    lexer.getConditionState().pop();
                } {
                    $$ = $2;
                }

// Node:command - call with or with block on end [!null]
command_call    : command
                | block_command

// Node:block_command - A call with a block (foo.bar {...}, foo::bar {...}, bar {...}) [!null]
block_command   : block_call
                | block_call call_op2 operation2 command_args {
                    $$ = support.new_call($1, $2, $3, $4, null, (@3.start >> 16));
                }

// :brace_block - [!null]
cmd_brace_block : tLBRACE_ARG brace_body tRCURLY {
                    $$ = $2;
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
                | primary_value call_op operation2 command_args %prec tLOWEST {
                    $$ = support.new_call($1, $2, $3, $4, null, (@3.start >> 16));
                }
                | primary_value call_op operation2 command_args cmd_brace_block {
                    $$ = support.new_call($1, $2, $3, $4, $5, (@3.start >> 16));
                }
                | primary_value tCOLON2 operation2 command_args %prec tLOWEST {
                    $$ = support.new_call($1, $3, $4, null);
                }
                | primary_value tCOLON2 operation2 command_args cmd_brace_block {
                    $$ = support.new_call($1, $3, $4, $5);
                }
                | keyword_super command_args {
                    $$ = support.new_super($1, $2);
                }
                | keyword_yield command_args {
                    $$ = support.new_yield($1, $2);
                }
                | k_return call_args {
                    $$ = new ReturnNode($1, support.ret_args($2, $1));
                }
                | keyword_break call_args {
                    $$ = new BreakNode($1, support.ret_args($2, $1));
                }
                | keyword_next call_args {
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
                    $$ = new MultipleAsgnNode($1.getLine(), $1, null, null);
                }
                | mlhs_head mlhs_item {
                    $$ = new MultipleAsgnNode($1.getLine(), $1.add($2), null, null);
                }
                | mlhs_head tSTAR mlhs_node {
                    $$ = new MultipleAsgnNode($1.getLine(), $1, $3, (ListNode) null);
                }
                | mlhs_head tSTAR mlhs_node ',' mlhs_post {
                    $$ = new MultipleAsgnNode($1.getLine(), $1, $3, $5);
                }
                | mlhs_head tSTAR {
                    $$ = new MultipleAsgnNode($1.getLine(), $1, new StarNode(lexer.getRubySourceline()), null);
                }
                | mlhs_head tSTAR ',' mlhs_post {
                    $$ = new MultipleAsgnNode($1.getLine(), $1, new StarNode(lexer.getRubySourceline()), $4);
                }
                | tSTAR mlhs_node {
                    $$ = new MultipleAsgnNode($2.getLine(), null, $2, null);
                }
                | tSTAR mlhs_node ',' mlhs_post {
                    $$ = new MultipleAsgnNode($2.getLine(), null, $2, $4);
                }
                | tSTAR {
                      $$ = new MultipleAsgnNode(lexer.getRubySourceline(), null, new StarNode(lexer.getRubySourceline()), null);
                }
                | tSTAR ',' mlhs_post {
                      $$ = new MultipleAsgnNode(lexer.getRubySourceline(), null, new StarNode(lexer.getRubySourceline()), $3);
                }

mlhs_item       : mlhs_node
                | tLPAREN mlhs_inner rparen {
                    $$ = $2;
                }

// Set of mlhs terms at front of mlhs (a, *b, d, e = arr  # a is head)
mlhs_head       : mlhs_item ',' {
                    $$ = support.newArrayNode($1.getLine(), $1);
                }
                | mlhs_head mlhs_item ',' {
                    $$ = $1.add($2);
                }

// Set of mlhs terms at end of mlhs (a, *b, d, e = arr  # d,e is post)
mlhs_post       : mlhs_item {
                    $$ = support.newArrayNode($1.getLine(), $1);
                }
                | mlhs_post ',' mlhs_item {
                    $$ = $1.add($3);
                }

mlhs_node       : /*mri:user_variable*/ tIDENTIFIER {
                   $$ = support.assignableLabelOrIdentifier($1, null);
                }
                | tIVAR {
                   $$ = new InstAsgnNode(lexer.tokline, support.symbolID($1), NilImplicitNode.NIL);
                }
                | tGVAR {
                   $$ = new GlobalAsgnNode(lexer.tokline, support.symbolID($1), NilImplicitNode.NIL);
                }
                | tCONSTANT {
                    if (support.isInDef()) support.compile_error("dynamic constant assignment");
                    $$ = new ConstDeclNode(lexer.tokline, support.symbolID($1), null, NilImplicitNode.NIL);
                }
                | tCVAR {
                    $$ = new ClassVarAsgnNode(lexer.tokline, support.symbolID($1), NilImplicitNode.NIL);
                } /*mri:user_variable*/
                | /*mri:keyword_variable*/ keyword_nil {
                    support.compile_error("Can't assign to nil");
                    $$ = null;
                }
                | keyword_self {
                    support.compile_error("Can't change the value of self");
                    $$ = null;
                }
                | keyword_true {
                    support.compile_error("Can't assign to true");
                    $$ = null;
                }
                | keyword_false {
                    support.compile_error("Can't assign to false");
                    $$ = null;
                }
                | keyword__FILE__ {
                    support.compile_error("Can't assign to __FILE__");
                    $$ = null;
                }
                | keyword__LINE__ {
                    support.compile_error("Can't assign to __LINE__");
                    $$ = null;
                }
                | keyword__ENCODING__ {
                    support.compile_error("Can't assign to __ENCODING__");
                    $$ = null;
                } /*mri:keyword_variable*/
                | primary_value '[' opt_call_args rbracket {
                    $$ = support.aryset($1, $3);
                }
                | primary_value call_op tIDENTIFIER {
                    $$ = support.attrset($1, $2, $3);
                }
                | primary_value tCOLON2 tIDENTIFIER {
                    $$ = support.attrset($1, $3);
                }
                | primary_value call_op tCONSTANT {
                    $$ = support.attrset($1, $2, $3);
                }
                | primary_value tCOLON2 tCONSTANT {
                    if (support.isInDef()) support.yyerror("dynamic constant assignment");

                    Integer position = support.getPosition($1);

                    $$ = new ConstDeclNode(position, (RubySymbol) null, support.new_colon2(position, $1, $3), NilImplicitNode.NIL);
                }
                | tCOLON3 tCONSTANT {
                    if (support.isInDef()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    Integer position = lexer.tokline;

                    $$ = new ConstDeclNode(position, (RubySymbol) null, support.new_colon3(position, $2), NilImplicitNode.NIL);
                }
                | backref {
                    support.backrefAssignError($1);
                }

// [!null or throws]
lhs             : /*mri:user_variable*/ tIDENTIFIER {
                    $$ = support.assignableLabelOrIdentifier($1, null);
                }
                | tIVAR {
                    $$ = new InstAsgnNode(lexer.tokline, support.symbolID($1), NilImplicitNode.NIL);
                }
                | tGVAR {
                    $$ = new GlobalAsgnNode(lexer.tokline, support.symbolID($1), NilImplicitNode.NIL);
                }
                | tCONSTANT {
                    if (support.isInDef()) support.compile_error("dynamic constant assignment");

                    $$ = new ConstDeclNode(lexer.tokline, support.symbolID($1), null, NilImplicitNode.NIL);
                }
                | tCVAR {
                    $$ = new ClassVarAsgnNode(lexer.tokline, support.symbolID($1), NilImplicitNode.NIL);
                } /*mri:user_variable*/
                | /*mri:keyword_variable*/ keyword_nil {
                    support.compile_error("Can't assign to nil");
                    $$ = null;
                }
                | keyword_self {
                    support.compile_error("Can't change the value of self");
                    $$ = null;
                }
                | keyword_true {
                    support.compile_error("Can't assign to true");
                    $$ = null;
                }
                | keyword_false {
                    support.compile_error("Can't assign to false");
                    $$ = null;
                }
                | keyword__FILE__ {
                    support.compile_error("Can't assign to __FILE__");
                    $$ = null;
                }
                | keyword__LINE__ {
                    support.compile_error("Can't assign to __LINE__");
                    $$ = null;
                }
                | keyword__ENCODING__ {
                    support.compile_error("Can't assign to __ENCODING__");
                    $$ = null;
                } /*mri:keyword_variable*/
                | primary_value '[' opt_call_args rbracket {
                    $$ = support.aryset($1, $3);
                }
                | primary_value call_op tIDENTIFIER {
                    $$ = support.attrset($1, $2, $3);
                }
                | primary_value tCOLON2 tIDENTIFIER {
                    $$ = support.attrset($1, $3);
                }
                | primary_value call_op tCONSTANT {
                    $$ = support.attrset($1, $2, $3);
                }
                | primary_value tCOLON2 tCONSTANT {
                    if (support.isInDef()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    Integer position = support.getPosition($1);

                    $$ = new ConstDeclNode(position, (RubySymbol) null, support.new_colon2(position, $1, $3), NilImplicitNode.NIL);
                }
                | tCOLON3 tCONSTANT {
                    if (support.isInDef()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    Integer position = lexer.tokline;

                    $$ = new ConstDeclNode(position, (RubySymbol) null, support.new_colon3(position, $2), NilImplicitNode.NIL);
                }
                | backref {
                    support.backrefAssignError($1);
                }

cname           : tIDENTIFIER {
                    support.yyerror("class/module name must be CONSTANT", @1);
                }
                | tCONSTANT {
                   $$ = $1;
                }

cpath           : tCOLON3 cname {
                    $$ = support.new_colon3(lexer.tokline, $2);
                }
                | cname {
                    $$ = support.new_colon2(lexer.tokline, null, $1);
                }
                | primary_value tCOLON2 cname {
                    $$ = support.new_colon2(support.getPosition($1), $1, $3);
                }

// ByteList:fname - A function name [!null]
fname          : tIDENTIFIER {
                   $$ = $1;
               }
               | tCONSTANT {
                   $$ = $1;
               }
               | tFID  {
                   $$ = $1;
               }
               | op {
                   lexer.setState(EXPR_ENDFN);
                   $$ = $1;
               }
               | reswords {
                   lexer.setState(EXPR_ENDFN);
                   $$ = $1;
               }

// LiteralNode:fsym
fsym           : fname {
                   $$ = new LiteralNode(lexer.getRubySourceline(), support.symbolID($1));
               }
               | symbol {
                   $$ = new LiteralNode(lexer.getRubySourceline(), support.symbolID($1));
               }

// Node:fitem
fitem           : fsym {  // LiteralNode
                    $$ = $1;
                }
                | dsym {  // SymbolNode/DSymbolNode
                    $$ = $1;
                }

undef_list      : fitem {
                    $$ = ParserSupport.newUndef($1.getLine(), $1);
                }
                | undef_list ',' {
                    lexer.setState(EXPR_FNAME|EXPR_FITEM);
                } fitem {
                    $$ = support.appendToBlock($1, ParserSupport.newUndef($1.getLine(), $4));
                }

// ByteList:op
 op              : tPIPE {
                     $$ = $1;
                 }
                 | tCARET {
                     $$ = $1;
                 }
                 | tAMPER2 {
                     $$ = $1;
                 }
                 | tCMP {
                     $$ = $1;
                 }
                 | tEQ {
                     $$ = $1;
                 }
                 | tEQQ {
                     $$ = $1;
                 }
                 | tMATCH {
                     $$ = $1;
                 }
                 | tNMATCH {
                     $$ = $1;
                 }
                 | tGT {
                     $$ = $1;
                 }
                 | tGEQ {
                     $$ = $1;
                 }
                 | tLT {
                     $$ = $1;
                 }
                 | tLEQ {
                     $$ = $1;
                 }
                 | tNEQ {
                     $$ = $1;
                 }
                 | tLSHFT {
                     $$ = $1;
                 }
                 | tRSHFT{
                     $$ = $1;
                 }
                 | tDSTAR {
                     $$ = $1;
                 }
                 | tPLUS {
                     $$ = $1;
                 }
                 | tMINUS {
                     $$ = $1;
                 }
                 | tSTAR2 {
                     $$ = $1;
                 }
                 | tSTAR {
                     $$ = $1;
                 }
                 | tDIVIDE {
                     $$ = $1;
                 }
                 | tPERCENT {
                     $$ = $1;
                 }
                 | tPOW {
                     $$ = $1;
                 }
                 | tBANG {
                     $$ = $1;
                 }
                 | tTILDE {
                     $$ = $1;
                 }
                 | tUPLUS {
                     $$ = $1;
                 }
                 | tUMINUS {
                     $$ = $1;
                 }
                 | tAREF {
                     $$ = $1;
                 }
                 | tASET {
                     $$ = $1;
                 }
                 | tBACK_REF2 {
                     $$ = $1;
                 }
 
// String:op
reswords        : keyword__LINE__ {
                    $$ = RubyLexer.Keyword.__LINE__.bytes;
                }
                | keyword__FILE__ {
                    $$ = RubyLexer.Keyword.__FILE__.bytes;
                }
                | keyword__ENCODING__ {
                    $$ = RubyLexer.Keyword.__ENCODING__.bytes;
                }
                | keyword_BEGIN {
                    $$ = RubyLexer.Keyword.LBEGIN.bytes;
                }
                | keyword_END {
                    $$ = RubyLexer.Keyword.LEND.bytes;
                }
                | keyword_alias {
                    $$ = RubyLexer.Keyword.ALIAS.bytes;
                }
                | keyword_and {
                    $$ = RubyLexer.Keyword.AND.bytes;
                }
                | keyword_begin {
                    $$ = RubyLexer.Keyword.BEGIN.bytes;
                }
                | keyword_break {
                    $$ = RubyLexer.Keyword.BREAK.bytes;
                }
                | keyword_case {
                    $$ = RubyLexer.Keyword.CASE.bytes;
                }
                | keyword_class {
                    $$ = RubyLexer.Keyword.CLASS.bytes;
                }
                | keyword_def {
                    $$ = RubyLexer.Keyword.DEF.bytes;
                }
                | keyword_defined {
                    $$ = RubyLexer.Keyword.DEFINED_P.bytes;
                }
                | keyword_do {
                    $$ = RubyLexer.Keyword.DO.bytes;
                }
                | keyword_else {
                    $$ = RubyLexer.Keyword.ELSE.bytes;
                }
                | keyword_elsif {
                    $$ = RubyLexer.Keyword.ELSIF.bytes;
                }
                | keyword_end {
                    $$ = RubyLexer.Keyword.END.bytes;
                }
                | keyword_ensure {
                    $$ = RubyLexer.Keyword.ENSURE.bytes;
                }
                | keyword_false {
                    $$ = RubyLexer.Keyword.FALSE.bytes;
                }
                | keyword_for {
                    $$ = RubyLexer.Keyword.FOR.bytes;
                }
                | keyword_in {
                    $$ = RubyLexer.Keyword.IN.bytes;
                }
                | keyword_module {
                    $$ = RubyLexer.Keyword.MODULE.bytes;
                }
                | keyword_next {
                    $$ = RubyLexer.Keyword.NEXT.bytes;
                }
                | keyword_nil {
                    $$ = RubyLexer.Keyword.NIL.bytes;
                }
                | keyword_not {
                    $$ = RubyLexer.Keyword.NOT.bytes;
                }
                | keyword_or {
                    $$ = RubyLexer.Keyword.OR.bytes;
                }
                | keyword_redo {
                    $$ = RubyLexer.Keyword.REDO.bytes;
                }
                | keyword_rescue {
                    $$ = RubyLexer.Keyword.RESCUE.bytes;
                }
                | keyword_retry {
                    $$ = RubyLexer.Keyword.RETRY.bytes;
                }
                | keyword_return {
                    $$ = RubyLexer.Keyword.RETURN.bytes;
                }
                | keyword_self {
                    $$ = RubyLexer.Keyword.SELF.bytes;
                }
                | keyword_super {
                    $$ = RubyLexer.Keyword.SUPER.bytes;
                }
                | keyword_then {
                    $$ = RubyLexer.Keyword.THEN.bytes;
                }
                | keyword_true {
                    $$ = RubyLexer.Keyword.TRUE.bytes;
                }
                | keyword_undef {
                    $$ = RubyLexer.Keyword.UNDEF.bytes;
                }
                | keyword_when {
                    $$ = RubyLexer.Keyword.WHEN.bytes;
                }
                | keyword_yield {
                    $$ = RubyLexer.Keyword.YIELD.bytes;
                }
                | keyword_if {
                    $$ = RubyLexer.Keyword.IF.bytes;
                }
                | keyword_unless {
                    $$ = RubyLexer.Keyword.UNLESS.bytes;
                }
                | keyword_while {
                    $$ = RubyLexer.Keyword.WHILE.bytes;
                }
                | keyword_until {
                    $$ = RubyLexer.Keyword.UNTIL.bytes;
                }
                | modifier_rescue {
                    $$ = RubyLexer.Keyword.RESCUE.bytes;
                }

arg             : lhs '=' arg_rhs {
                    $$ = node_assign($1, $3);
                }
                | var_lhs tOP_ASGN arg_rhs {
                    $$ = support.new_op_assign($1, $2, $3);
                }
                | primary_value '[' opt_call_args rbracket tOP_ASGN arg {
                    support.value_expr(lexer, $6);
                    $$ = support.new_ary_op_assign($1, $5, $3, $6);
                }
                | primary_value call_op tIDENTIFIER tOP_ASGN arg_rhs {
                    support.value_expr(lexer, $5);
                    $$ = support.new_attr_op_assign($1, $2, $5, $3, $4);
                }
                | primary_value call_op tCONSTANT tOP_ASGN arg_rhs {
                    support.value_expr(lexer, $5);
                    $$ = support.new_attr_op_assign($1, $2, $5, $3, $4);
                }
                | primary_value tCOLON2 tIDENTIFIER tOP_ASGN arg_rhs {
                    support.value_expr(lexer, $5);
                    $$ = support.new_attr_op_assign($1, $2, $5, $3, $4);
                }
                | primary_value tCOLON2 tCONSTANT tOP_ASGN arg_rhs {
                    Integer pos = support.getPosition($1);
                    $$ = support.new_const_op_assign(pos, support.new_colon2(pos, $1, $3), $4, $5);
                }
                | tCOLON3 tCONSTANT tOP_ASGN arg_rhs {
                    Integer pos = lexer.getRubySourceline();
                    $$ = support.new_const_op_assign(pos, new Colon3Node(pos, support.symbolID($2)), $3, $4);
                }
                | backref tOP_ASGN arg_rhs {
                    support.backrefAssignError($1);
                }
                | arg tDOT2 arg {
                    support.value_expr(lexer, $1);
                    support.value_expr(lexer, $3);
    
                    boolean isLiteral = $1 instanceof FixnumNode && $3 instanceof FixnumNode;
                    $$ = new DotNode(support.getPosition($1), support.makeNullNil($1), support.makeNullNil($3), false, isLiteral);
                }
                | arg tDOT3 arg {
                    support.value_expr(lexer, $1);
                    support.value_expr(lexer, $3);

                    boolean isLiteral = $1 instanceof FixnumNode && $3 instanceof FixnumNode;
                    $$ = new DotNode(support.getPosition($1), support.makeNullNil($1), support.makeNullNil($3), true, isLiteral);
                }
                | arg tDOT2 {
                    support.value_expr(lexer, $1);

                    boolean isLiteral = $1 instanceof FixnumNode;
                    $$ = new DotNode(support.getPosition($1), support.makeNullNil($1), NilImplicitNode.NIL, false, isLiteral);
                }
                | arg tDOT3 {
                    support.value_expr(lexer, $1);

                    boolean isLiteral = $1 instanceof FixnumNode;
                    $$ = new DotNode(support.getPosition($1), support.makeNullNil($1), NilImplicitNode.NIL, true, isLiteral);
                }
                | arg tPLUS arg {
                    $$ = support.getOperatorCallNode($1, $2, $3, lexer.getRubySourceline());
                }
                | arg tMINUS arg {
                    $$ = support.getOperatorCallNode($1, $2, $3, lexer.getRubySourceline());
                }
                | arg tSTAR2 arg {
                    $$ = support.getOperatorCallNode($1, $2, $3, lexer.getRubySourceline());
                }
                | arg tDIVIDE arg {
                    $$ = support.getOperatorCallNode($1, $2, $3, lexer.getRubySourceline());
                }
                | arg tPERCENT arg {
                    $$ = support.getOperatorCallNode($1, $2, $3, lexer.getRubySourceline());
                }
                | arg tPOW arg {
                    $$ = support.getOperatorCallNode($1, $2, $3, lexer.getRubySourceline());
                }
                | tUMINUS_NUM simple_numeric tPOW arg {
                    $$ = support.getOperatorCallNode(support.getOperatorCallNode($2, $3, $4, lexer.getRubySourceline()), $1);
                }
                | tUPLUS arg {
                    $$ = support.getOperatorCallNode($2, $1);
                }
                | tUMINUS arg {
                    $$ = support.getOperatorCallNode($2, $1);
                }
                | arg tPIPE arg {
                    $$ = support.getOperatorCallNode($1, $2, $3, lexer.getRubySourceline());
                }
                | arg tCARET arg {
                    $$ = support.getOperatorCallNode($1, $2, $3, lexer.getRubySourceline());
                }
                | arg tAMPER2 arg {
                    $$ = support.getOperatorCallNode($1, $2, $3, lexer.getRubySourceline());
                }
                | arg tCMP arg {
                    $$ = support.getOperatorCallNode($1, $2, $3, lexer.getRubySourceline());
                }
                | rel_expr   %prec tCMP {
                    $$ = $1;
                }
                | arg tEQ arg {
                    $$ = support.getOperatorCallNode($1, $2, $3, lexer.getRubySourceline());
                }
                | arg tEQQ arg {
                    $$ = support.getOperatorCallNode($1, $2, $3, lexer.getRubySourceline());
                }
                | arg tNEQ arg {
                    $$ = support.getOperatorCallNode($1, $2, $3, lexer.getRubySourceline());
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
                    $$ = support.getOperatorCallNode($1, $2, $3, lexer.getRubySourceline());
                }
                | tBANG arg {
                    $$ = support.getOperatorCallNode(support.method_cond($2), $1);
                }
                | tTILDE arg {
                    $$ = support.getOperatorCallNode($2, $1);
                }
                | arg tLSHFT arg {
                    $$ = support.getOperatorCallNode($1, $2, $3, lexer.getRubySourceline());
                }
                | arg tRSHFT arg {
                    $$ = support.getOperatorCallNode($1, $2, $3, lexer.getRubySourceline());
                }
                | arg tANDOP arg {
                    $$ = support.newAndNode($1, $3);
                }
                | arg tOROP arg {
                    $$ = support.newOrNode($1, $3);
                }
                | keyword_defined opt_nl arg {
                    $$ = new DefinedNode($1, $3);
                }
                | arg '?' arg opt_nl ':' arg {
                    support.value_expr(lexer, $1);
                    $$ = support.new_if(support.getPosition($1), support.cond($1), $3, $6);
                }
                | primary {
                    $$ = $1;
                }
 
relop           : tGT {
                    $$ = $1;
                }
                | tLT  {
                    $$ = $1;
                }
                | tGEQ {
                     $$ = $1;
                }
                | tLEQ {
                     $$ = $1;
                }

rel_expr        : arg relop arg   %prec tGT {
                     $$ = support.getOperatorCallNode($1, $2, $3, lexer.getRubySourceline());
                }
		| rel_expr relop arg   %prec tGT {
                     support.warning(ID.MISCELLANEOUS, lexer.getRubySourceline(), "comparison '" + $2 + "' after comparison");
                     $$ = support.getOperatorCallNode($1, $2, $3, lexer.getRubySourceline());
                }
 
arg_value       : arg {
                    support.value_expr(lexer, $1);
                    $$ = support.makeNullNil($1);
                }

aref_args       : none
                | args trailer {
                    $$ = $1;
                }
                | args ',' assocs trailer {
                    $$ = support.arg_append($1, support.remove_duplicate_keys($3));
                }
                | assocs trailer {
                    $$ = support.newArrayNode($1.getLine(), support.remove_duplicate_keys($1));
                }

arg_rhs         : arg %prec tOP_ASGN {
                    support.value_expr(lexer, $1);
                    $$ = $1;
                }
                | arg modifier_rescue arg {
                    support.value_expr(lexer, $1);
                    $$ = support.newRescueModNode($1, $3);
                }

paren_args      : tLPAREN2 opt_call_args rparen {
                    $$ = $2;
                    if ($$ != null) $<Node>$.setLine($1);
                }

opt_paren_args  : none | paren_args

opt_call_args   : none
                | call_args
                | args ',' {
                    $$ = $1;
                }
                | args ',' assocs ',' {
                    $$ = support.arg_append($1, support.remove_duplicate_keys($3));
                }
                | assocs ',' {
                    $$ = support.newArrayNode($1.getLine(), support.remove_duplicate_keys($1));
                }
   

// [!null] - ArgsCatNode, SplatNode, ArrayNode, HashNode, BlockPassNode
call_args       : command {
                    support.value_expr(lexer, $1);
                    $$ = support.newArrayNode(support.getPosition($1), $1);
                }
                | args opt_block_arg {
                    $$ = arg_blk_pass($1, $2);
                }
                | assocs opt_block_arg {
                    $$ = support.newArrayNode($1.getLine(), support.remove_duplicate_keys($1));
                    $$ = arg_blk_pass($<Node>$, $2);
                }
                | args ',' assocs opt_block_arg {
                    $$ = support.arg_append($1, support.remove_duplicate_keys($3));
                    $$ = arg_blk_pass($<Node>$, $4);
                }
                | block_arg {
                }

// [!null] - ArgsCatNode, SplatNode, ArrayNode, HashNode, BlockPassNode
command_args    : /* none */ {
                    boolean lookahead = false;
                    switch (yychar) {
                    case tLPAREN2: case tLPAREN: case tLPAREN_ARG: case '[': case tLBRACK:
                       lookahead = true;
                    }
                    StackState cmdarg = lexer.getCmdArgumentState();
                    if (lookahead) cmdarg.pop();
                    cmdarg.push1();
                    if (lookahead) cmdarg.push0();
                } call_args {
                    StackState cmdarg = lexer.getCmdArgumentState();
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
                    $$ = new BlockPassNode(support.getPosition($2), $2);
                }

opt_block_arg   : ',' block_arg {
                    $$ = $2;
                }
                | none_block_pass

// [!null]
args            : arg_value { // ArrayNode
                    int line = $1 instanceof NilImplicitNode ? lexer.getRubySourceline() : $1.getLine();
                    $$ = support.newArrayNode(line, $1);
                }
                | tSTAR arg_value { // SplatNode
                    $$ = support.newSplatNode($2);
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
                        $$ = ParserSupport.arg_concat($1, $4);
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
                        $$ = ParserSupport.arg_concat($1, $4);
                    }
                }
                | tSTAR arg_value {
                     $$ = support.newSplatNode($2);
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
                | keyword_begin {
                    lexer.getCmdArgumentState().push0();
                } bodystmt keyword_end {
                    lexer.getCmdArgumentState().pop();
                    $$ = new BeginNode($1, support.makeNullNil($3));
                }
                | tLPAREN_ARG {
                    lexer.setState(EXPR_ENDARG);
                } rparen {
                    $$ = null; //FIXME: Should be implicit nil?
                }
                | tLPAREN_ARG stmt {
                    lexer.setState(EXPR_ENDARG); 
                } rparen {
                    $$ = $2;
                }
                | tLPAREN compstmt tRPAREN {
                    if ($2 != null) {
                        // compstmt position includes both parens around it
                        $<Node>2.setLine($1);
                        $$ = $2;
                    } else {
                        $$ = new NilNode($1);
                    }
                }
                | primary_value tCOLON2 tCONSTANT {
                    $$ = support.new_colon2(support.getPosition($1), $1, $3);
                }
                | tCOLON3 tCONSTANT {
                    $$ = support.new_colon3(lexer.tokline, $2);
                }
                | tLBRACK aref_args tRBRACK {
                    Integer position = support.getPosition($2);
                    if ($2 == null) {
                        $$ = new ZArrayNode(position); /* zero length array */
                    } else {
                        $$ = $2;
                    }
                }
                | tLBRACE assoc_list tRCURLY {
                    $$ = $2;
                }
                | k_return {
                    $$ = new ReturnNode($1, NilImplicitNode.NIL);
                }
                | keyword_yield tLPAREN2 call_args rparen {
                    $$ = support.new_yield($1, $3);
                }
                | keyword_yield tLPAREN2 rparen {
                    $$ = new YieldNode($1, null);
                }
                | keyword_yield {
                    $$ = new YieldNode($1, null);
                }
                | keyword_defined opt_nl tLPAREN2 expr rparen {
                    $$ = new DefinedNode($1, $4);
                }
                | keyword_not tLPAREN2 expr rparen {
                    $$ = support.getOperatorCallNode(support.method_cond($3), lexer.BANG);
                }
                | keyword_not tLPAREN2 rparen {
                    $$ = support.getOperatorCallNode(support.method_cond(NilImplicitNode.NIL), lexer.BANG);
                }
                | fcall brace_block {
                    support.frobnicate_fcall_args($1, null, $2);
                    $$ = $1;                    
                }
                | method_call
                | method_call brace_block {
                    if ($1 != null && 
                          $<BlockAcceptingNode>1.getIterNode() instanceof BlockPassNode) {
                          lexer.compile_error("Both block arg and actual block given.");
                    }
                    $$ = $<BlockAcceptingNode>1.setIterNode($2);
                    $<Node>$.setLine($1.getLine());
                }
                | tLAMBDA lambda {
                    $$ = $2;
                }
                | keyword_if expr_value then compstmt if_tail keyword_end {
                    $$ = support.new_if($1, support.cond($2), $4, $5);
                }
                | keyword_unless expr_value then compstmt opt_else keyword_end {
                    $$ = support.new_if($1, support.cond($2), $5, $4);
                }
                | keyword_while {
                    lexer.getConditionState().push1();
                } expr_value_do {
                    lexer.getConditionState().pop();
                } compstmt keyword_end {
                    Node body = support.makeNullNil($5);
                    $$ = new WhileNode($1, support.cond($3), body);
                }
                | keyword_until {
                  lexer.getConditionState().push1();
                } expr_value_do {
                  lexer.getConditionState().pop();
                } compstmt keyword_end {
                    Node body = support.makeNullNil($5);
                    $$ = new UntilNode($1, support.cond($3), body);
                }
                | keyword_case expr_value opt_terms case_body keyword_end {
                    $$ = support.newCaseNode($1, $2, $4);
                    support.fixpos($<Node>$, $2);
                }
                | keyword_case opt_terms case_body keyword_end {
                    $$ = support.newCaseNode($1, null, $3);
                }
                | keyword_for for_var keyword_in {
                    lexer.getConditionState().push1();
                } expr_value_do {
                    lexer.getConditionState().pop();
                } compstmt keyword_end {
                      // ENEBO: Lots of optz in 1.9 parser here
                    $$ = new ForNode($1, $2, $7, $5, support.getCurrentScope(), lexer.getRubySourceline());
                }
                | k_class cpath superclass {
                    if (support.isInDef()) {
                        support.yyerror("class definition in method body");
                    }
                    support.pushLocalScope();
                    $$ = support.isInClass(); // MRI reuses $1 but we use the value for position.
                    support.setIsInClass(true);
                } bodystmt keyword_end {
                    Node body = support.makeNullNil($5);

                    $$ = new ClassNode($1, $<Colon3Node>2, support.getCurrentScope(), body, $3, lexer.getRubySourceline());
                    support.popCurrentScope();
                    support.setIsInClass($<Boolean>4.booleanValue());
                }
                | k_class tLSHFT expr {
                    $$ = new Integer((support.isInClass() ? 0b10 : 0) |
                                     (support.isInDef()   ? 0b01 : 0));
                    support.setInDef(false);
                    support.setIsInClass(false);
                    support.pushLocalScope();
                } term bodystmt keyword_end {
                    Node body = support.makeNullNil($6);

                    $$ = new SClassNode($1, $3, support.getCurrentScope(), body, lexer.getRubySourceline());
                    support.popCurrentScope();
                    support.setInDef((($<Integer>4.intValue())     & 0b01) != 0);
                    support.setIsInClass((($<Integer>4.intValue()) & 0b10) != 0);
                }
                | k_module cpath {
                    if (support.isInDef()) { 
                        support.yyerror("module definition in method body");
                    }
                    $$ = support.isInClass();
                    support.setIsInClass(true);
                    support.pushLocalScope();
                } bodystmt keyword_end {
                    Node body = support.makeNullNil($4);

                    $$ = new ModuleNode($1, $<Colon3Node>2, support.getCurrentScope(), body, lexer.getRubySourceline());
                    support.popCurrentScope();
                    support.setIsInClass($<Boolean>3.booleanValue());
                }
                | keyword_def fname {
                    support.isNextBreak = false;
                    support.pushLocalScope();
                    $$ = lexer.getCurrentArg();
                    lexer.setCurrentArg(null);
                } {
                    $$ = support.isInDef();
                    support.setInDef(true);
                } f_arglist bodystmt keyword_end {
                    Node body = support.makeNullNil($6);

                    $$ = new DefnNode($1, support.symbolID($2), (ArgsNode) $5, support.getCurrentScope(), body, $7);
                    if (support.isNextBreak) $<DefnNode>$.setContainsNextBreak();
                    support.isNextBreak = false;
                    support.popCurrentScope();
                    support.setInDef($<Boolean>4.booleanValue());
                    lexer.setCurrentArg($<ByteList>3);
                }
                | keyword_def singleton dot_or_colon {
                    support.isNextBreak = false;  
                    lexer.setState(EXPR_FNAME); 
                    $$ = support.isInDef();
                    support.setInDef(true);
               } fname {
                    support.pushLocalScope();
                    lexer.setState(EXPR_ENDFN|EXPR_LABEL); /* force for args */
                    $$ = lexer.getCurrentArg();
                    lexer.setCurrentArg(null);
                } f_arglist bodystmt keyword_end {
                    Node body = $8;
                    if (body == null) body = NilImplicitNode.NIL;

                    $$ = new DefsNode($1, $2, support.symbolID($5), (ArgsNode) $7, support.getCurrentScope(), body, $9);
                    if (support.isNextBreak) $<DefsNode>$.setContainsNextBreak();
                    support.isNextBreak = false;
                    support.popCurrentScope();
                    support.setInDef($<Boolean>4.booleanValue());
                    lexer.setCurrentArg($<ByteList>6);
                }
                | keyword_break {
                    support.isNextBreak = true;
                    $$ = new BreakNode($1, NilImplicitNode.NIL);
                }
                | keyword_next {
                    support.isNextBreak = true;
                    $$ = new NextNode($1, NilImplicitNode.NIL);
                }
                | keyword_redo {
                    $$ = new RedoNode($1);
                }
                | keyword_retry {
                    $$ = new RetryNode($1);
                }

primary_value   : primary {
                    support.value_expr(lexer, $1);
                    $$ = $1;
                    if ($$ == null) $$ = NilImplicitNode.NIL;
                }

k_class         : keyword_class {
                    $$ = $1;
                }

k_else          : keyword_else {
                    $$ = $1;
                }

k_module        : keyword_module {
                    $$ = $1;
                }

k_return        : keyword_return {
                    if (support.isInClass() && !support.isInDef() && !support.getCurrentScope().isBlockScope()) {
                        lexer.compile_error("Invalid return in class/module body");
                    }
                    $$ = $1;
                }

then            : term
                | keyword_then
                | term keyword_then

do              : term
                | keyword_do_cond

if_tail         : opt_else
                | keyword_elsif expr_value then compstmt if_tail {
                    $$ = support.new_if($1, support.cond($2), $4, $5);
                }

opt_else        : none
                | k_else compstmt {
                    $$ = $2;
                }

// [!null]
for_var         : lhs
                | mlhs {
                }

f_marg          : f_norm_arg {
                    $$ = support.assignableInCurr($1, NilImplicitNode.NIL);
                }
                | tLPAREN f_margs rparen {
                    $$ = $2;
                }

// [!null]
f_marg_list     : f_marg {
                    $$ = support.newArrayNode($1.getLine(), $1);
                }
                | f_marg_list ',' f_marg {
                    $$ = $1.add($3);
                }

f_margs         : f_marg_list {
                    $$ = new MultipleAsgnNode($1.getLine(), $1, null, null);
                }
                | f_marg_list ',' tSTAR f_norm_arg {
                    $$ = new MultipleAsgnNode($1.getLine(), $1, support.assignableInCurr($4, null), null);
                }
                | f_marg_list ',' tSTAR f_norm_arg ',' f_marg_list {
                    $$ = new MultipleAsgnNode($1.getLine(), $1, support.assignableInCurr($4, null), $6);
                }
                | f_marg_list ',' tSTAR {
                    $$ = new MultipleAsgnNode($1.getLine(), $1, new StarNode(lexer.getRubySourceline()), null);
                }
                | f_marg_list ',' tSTAR ',' f_marg_list {
                    $$ = new MultipleAsgnNode($1.getLine(), $1, new StarNode(lexer.getRubySourceline()), $5);
                }
                | tSTAR f_norm_arg {
                    $$ = new MultipleAsgnNode(lexer.getRubySourceline(), null, support.assignableInCurr($2, null), null);
                }
                | tSTAR f_norm_arg ',' f_marg_list {
                    $$ = new MultipleAsgnNode(lexer.getRubySourceline(), null, support.assignableInCurr($2, null), $4);
                }
                | tSTAR {
                    $$ = new MultipleAsgnNode(lexer.getRubySourceline(), null, new StarNode(lexer.getRubySourceline()), null);
                }
                | tSTAR ',' f_marg_list {
                    $$ = new MultipleAsgnNode(support.getPosition($3), null, null, $3);
                }

block_args_tail : f_block_kwarg ',' f_kwrest opt_f_block_arg {
                    $$ = support.new_args_tail($1.getLine(), $1, $3, $4);
                }
                | f_block_kwarg opt_f_block_arg {
                    $$ = support.new_args_tail($1.getLine(), $1, (ByteList) null, $2);
                }
                | f_kwrest opt_f_block_arg {
                    $$ = support.new_args_tail(lexer.getRubySourceline(), null, $1, $2);
                }
                | f_block_arg {
                    $$ = support.new_args_tail($1.getLine(), null, (ByteList) null, $1);
                }

opt_block_args_tail : ',' block_args_tail {
                    $$ = $2;
                }
                | /* none */ {
                    $$ = support.new_args_tail(lexer.getRubySourceline(), null, (ByteList) null, null);
                }

// [!null]
block_param     : f_arg ',' f_block_optarg ',' f_rest_arg opt_block_args_tail {
                    $$ = support.new_args($1.getLine(), $1, $3, $5, null, $6);
                }
                | f_arg ',' f_block_optarg ',' f_rest_arg ',' f_arg opt_block_args_tail {
                    $$ = support.new_args($1.getLine(), $1, $3, $5, $7, $8);
                }
                | f_arg ',' f_block_optarg opt_block_args_tail {
                    $$ = support.new_args($1.getLine(), $1, $3, null, null, $4);
                }
                | f_arg ',' f_block_optarg ',' f_arg opt_block_args_tail {
                    $$ = support.new_args($1.getLine(), $1, $3, null, $5, $6);
                }
                | f_arg ',' f_rest_arg opt_block_args_tail {
                    $$ = support.new_args($1.getLine(), $1, null, $3, null, $4);
                }
                | f_arg ',' {
                    RestArgNode rest = new UnnamedRestArgNode($1.getLine(), null, support.getCurrentScope().addVariable("*"));
                    $$ = support.new_args($1.getLine(), $1, null, rest, null, (ArgsTailHolder) null);
                }
                | f_arg ',' f_rest_arg ',' f_arg opt_block_args_tail {
                    $$ = support.new_args($1.getLine(), $1, null, $3, $5, $6);
                }
                | f_arg opt_block_args_tail {
                    $$ = support.new_args($1.getLine(), $1, null, null, null, $2);
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
                    $$ = support.new_args($1.getLine(), null, $1, null, $3, $4);
                }
                | f_rest_arg opt_block_args_tail {
                    $$ = support.new_args($1.getLine(), null, null, $1, null, $2);
                }
                | f_rest_arg ',' f_arg opt_block_args_tail {
                    $$ = support.new_args($1.getLine(), null, null, $1, $3, $4);
                }
                | block_args_tail {
                    $$ = support.new_args($1.getLine(), null, null, null, null, $1);
                }

opt_block_param : none {
    // was $$ = null;
                    $$ = support.new_args(lexer.getRubySourceline(), null, null, null, null, (ArgsTailHolder) null);
                }
                | block_param_def {
                    lexer.commandStart = true;
                    $$ = $1;
                }

block_param_def : tPIPE opt_bv_decl tPIPE {
                    lexer.setCurrentArg(null);
                    $$ = support.new_args(lexer.getRubySourceline(), null, null, null, null, (ArgsTailHolder) null);
                }
                | tOROP {
                    $$ = support.new_args(lexer.getRubySourceline(), null, null, null, null, (ArgsTailHolder) null);
                }
                | tPIPE block_param opt_bv_decl tPIPE {
                    lexer.setCurrentArg(null);
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
                } f_larglist {
                    lexer.getCmdArgumentState().push0();
                } lambda_body {
                    lexer.getCmdArgumentState().pop();
                    $$ = new LambdaNode($2.getLine(), $2, $4, support.getCurrentScope(), lexer.getRubySourceline());
                    lexer.setLeftParenBegin($<Integer>1);
                    support.popCurrentScope();
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
                | keyword_do_lambda bodystmt keyword_end {
                    $$ = $2;
                }

do_block        : keyword_do_block do_body keyword_end {
                    $$ = $2;
                }

  // JRUBY-2326 and GH #305 both end up hitting this production whereas in
  // MRI these do not.  I have never isolated the cause but I can work around
  // the individual reported problems with a few extra conditionals in this
  // first production
block_call      : command do_block {
                    // Workaround for JRUBY-2326 (MRI does not enter this production for some reason)
                    if ($1 instanceof YieldNode) {
                        lexer.compile_error("block given to yield");
                    }
                    if ($1 instanceof BlockAcceptingNode && $<BlockAcceptingNode>1.getIterNode() instanceof BlockPassNode) {
                        lexer.compile_error("Both block arg and actual block given.");
                    }
                    if ($1 instanceof NonLocalControlFlowNode) {
                        ((BlockAcceptingNode) $<NonLocalControlFlowNode>1.getValueNode()).setIterNode($2);
                    } else {
                        $<BlockAcceptingNode>1.setIterNode($2);
                    }
                    $$ = $1;
                    $<Node>$.setLine($1.getLine());
                }
                | block_call call_op2 operation2 opt_paren_args {
                    $$ = support.new_call($1, $2, $3, $4, null, (@3.start >> 16));
                }
                | block_call call_op2 operation2 opt_paren_args brace_block {
                    $$ = support.new_call($1, $2, $3, $4, $5, (@3.start >> 16));
                }
                | block_call call_op2 operation2 command_args do_block {
                    $$ = support.new_call($1, $2, $3, $4, $5, (@3.start >> 16));
                }

// [!null]
method_call     : fcall paren_args {
                    support.frobnicate_fcall_args($1, $2, null);
                    $$ = $1;
                }
                | primary_value call_op operation2 opt_paren_args {
                    $$ = support.new_call($1, $2, $3, $4, null, (@3.start >> 16));
                }
                | primary_value tCOLON2 operation2 paren_args {
                    $$ = support.new_call($1, $3, $4, null);
                }
                | primary_value tCOLON2 operation3 {
                    $$ = support.new_call($1, $3, null, null);
                }
                | primary_value call_op paren_args {
                    $$ = support.new_call($1, $2, LexingCommon.CALL, $3, null, (@3.start >> 16));
                }
                | primary_value tCOLON2 paren_args {
                    $$ = support.new_call($1, LexingCommon.CALL, $3, null);
                }
                | keyword_super paren_args {
                    $$ = support.new_super($1, $2);
                }
                | keyword_super {
                    $$ = new ZSuperNode($1);
                }
                | primary_value '[' opt_call_args rbracket {
                    if ($1 instanceof SelfNode) {
                        $$ = support.new_fcall(LexingCommon.LBRACKET_RBRACKET);
                        support.frobnicate_fcall_args($<FCallNode>$, $3, null);
                    } else {
                        $$ = support.new_call($1, lexer.LBRACKET_RBRACKET, $3, null);
                    }
                }

brace_block     : tLCURLY brace_body tRCURLY {
                    $$ = $2;
                }
                | keyword_do do_body keyword_end {
                    $$ = $2;
                }

brace_body      : {
                    $$ = lexer.getRubySourceline();
                } {
                    support.pushBlockScope();
                } opt_block_param compstmt {
                    $$ = new IterNode($<Integer>1, $3, $4, support.getCurrentScope(), lexer.getRubySourceline());
                    support.popCurrentScope();
                }

do_body 	: {
                    $$ = lexer.getRubySourceline();
                } {
                    support.pushBlockScope();
                    lexer.getCmdArgumentState().push0();
                } opt_block_param bodystmt {
                    $$ = new IterNode($<Integer>1, $3, $4, support.getCurrentScope(), lexer.getRubySourceline());
                    lexer.getCmdArgumentState().pop();
                    support.popCurrentScope();
                }
 
case_body       : keyword_when args then compstmt cases {
                    $$ = support.newWhenNode($1, $2, $4, $5);
                }

cases           : opt_else | case_body

opt_rescue      : keyword_rescue exc_list exc_var then compstmt opt_rescue {
                    Node node;
                    if ($3 != null) {
                        node = support.appendToBlock(node_assign($3, new GlobalVarNode($1, support.symbolID(lexer.DOLLAR_BANG))), $5);
                        if ($5 != null) {
                            node.setLine($1);
                        }
                    } else {
                        node = $5;
                    }
                    Node body = support.makeNullNil(node);
                    $$ = new RescueBodyNode($1, $2, body, $6);
                }
                | {
                    $$ = null; 
                }

exc_list        : arg_value {
                    $$ = support.newArrayNode($1.getLine(), $1);
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

opt_ensure      : keyword_ensure compstmt {
                    $$ = $2;
                }
                | none

literal         : numeric {
                    $$ = $1;
                }
                | symbol {
                    $$ = support.asSymbol(lexer.getRubySourceline(), $1);
                }
                | dsym

strings         : string {
                    $$ = $1 instanceof EvStrNode ? new DStrNode($1.getLine(), lexer.getEncoding()).add($1) : $1;
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
                    $$ = support.literal_concat($1, $2);
                }

string1         : tSTRING_BEG string_contents tSTRING_END {
                    lexer.heredoc_dedent($2);
		    lexer.setHeredocIndent(0);
                    $$ = $2;
                }

xstring         : tXSTRING_BEG xstring_contents tSTRING_END {
                    int line = support.getPosition($2);

                    lexer.heredoc_dedent($2);
		    lexer.setHeredocIndent(0);

                    if ($2 == null) {
                        $$ = new XStrNode(line, null, StringSupport.CR_7BIT);
                    } else if ($2 instanceof StrNode) {
                        $$ = new XStrNode(line, (ByteList) $<StrNode>2.getValue().clone(), $<StrNode>2.getCodeRange());
                    } else if ($2 instanceof DStrNode) {
                        $$ = new DXStrNode(line, $<DStrNode>2);

                        $<Node>$.setLine(line);
                    } else {
                        $$ = new DXStrNode(line).add($2);
                    }
                }

regexp          : tREGEXP_BEG regexp_contents tREGEXP_END {
                    $$ = support.newRegexpNode(support.getPosition($2), $2, (RegexpNode) $3);
                }

words           : tWORDS_BEG ' ' word_list tSTRING_END {
                    $$ = $3;
                }

word_list       : /* none */ {
                     $$ = new ArrayNode(lexer.getRubySourceline());
                }
                | word_list word ' ' {
                     $$ = $1.add($2 instanceof EvStrNode ? new DStrNode($1.getLine(), lexer.getEncoding()).add($2) : $2);
                }

word            : string_content {
                     $$ = $<Node>1;
                }
                | word string_content {
                     $$ = support.literal_concat($1, $<Node>2);
                }

symbols         : tSYMBOLS_BEG ' ' symbol_list tSTRING_END {
                    $$ = $3;
                }

symbol_list     : /* none */ {
                    $$ = new ArrayNode(lexer.getRubySourceline());
                }
                | symbol_list word ' ' {
                    $$ = $1.add($2 instanceof EvStrNode ? new DSymbolNode($1.getLine()).add($2) : support.asSymbol($1.getLine(), $2));
                }

qwords          : tQWORDS_BEG ' ' qword_list tSTRING_END {
                    $$ = $3;
                }

qsymbols        : tQSYMBOLS_BEG ' ' qsym_list tSTRING_END {
                    $$ = $3;
                }


qword_list      : /* none */ {
                    $$ = new ArrayNode(lexer.getRubySourceline());
                }
                | qword_list tSTRING_CONTENT ' ' {
                    $$ = $1.add($2);
                }

qsym_list      : /* none */ {
                    $$ = new ArrayNode(lexer.getRubySourceline());
                }
                | qsym_list tSTRING_CONTENT ' ' {
                    $$ = $1.add(support.asSymbol($1.getLine(), $2));
                }

string_contents : /* none */ {
                    ByteList aChar = ByteList.create("");
                    aChar.setEncoding(lexer.getEncoding());
                    $$ = lexer.createStr(aChar, 0);
                }
                | string_contents string_content {
                    $$ = support.literal_concat($1, $<Node>2);
                }

xstring_contents: /* none */ {
                    ByteList aChar = ByteList.create("");
                    aChar.setEncoding(lexer.getEncoding());
                    $$ = lexer.createStr(aChar, 0);
                }
                | xstring_contents string_content {
                    $$ = support.literal_concat($1, $<Node>2);
                }

regexp_contents: /* none */ {
                    $$ = null;
                }
                | regexp_contents string_content {
    // FIXME: mri is different here.
                    $$ = support.literal_concat($1, $<Node>2);
                }

string_content  : tSTRING_CONTENT {
                    $$ = $1;
                }
                | tSTRING_DVAR {
                    $$ = lexer.getStrTerm();
                    lexer.setStrTerm(null);
                    lexer.setState(EXPR_BEG);
                } string_dvar {
                    lexer.setStrTerm($<StrTerm>2);
                    $$ = new EvStrNode(support.getPosition($3), $3);
                }
                | tSTRING_DBEG {
                   $$ = lexer.getStrTerm();
                   lexer.setStrTerm(null);
                   lexer.getConditionState().push0();
                   lexer.getCmdArgumentState().push0();
                } {
                   $$ = lexer.getState();
                   lexer.setState(EXPR_BEG);
                } {
                   $$ = lexer.getBraceNest();
                   lexer.setBraceNest(0);
                } {
                   $$ = lexer.getHeredocIndent();
                   lexer.setHeredocIndent(0);
                } compstmt tSTRING_DEND {
                   lexer.getConditionState().pop();
                   lexer.getCmdArgumentState().pop();
                   lexer.setStrTerm($<StrTerm>2);
                   lexer.setState($<Integer>3);
                   lexer.setBraceNest($<Integer>4);
                   lexer.setHeredocIndent($<Integer>5);
                   lexer.setHeredocLineIndent(-1);

                   if ($6 != null) $6.unsetNewline();
                   $$ = support.newEvStrNode(support.getPosition($6), $6);
                }

string_dvar     : tGVAR {
                     $$ = new GlobalVarNode(lexer.getRubySourceline(), support.symbolID($1));
                }
                | tIVAR {
                     $$ = new InstVarNode(lexer.getRubySourceline(), support.symbolID($1));
                }
                | tCVAR {
                     $$ = new ClassVarNode(lexer.getRubySourceline(), support.symbolID($1));
                }
                | backref

// ByteList:symbol
symbol          : tSYMBEG sym {
                     lexer.setState(EXPR_END);
                     $$ = $2;
                }

// ByteList:symbol
sym             : fname
                | tIVAR {
                    $$ = $1;
                }
                | tGVAR {
                    $$ = $1;
                }
                | tCVAR {
                    $$ = $1;
                }

dsym            : tSYMBEG xstring_contents tSTRING_END {
                     lexer.setState(EXPR_END);

                     // DStrNode: :"some text #{some expression}"
                     // StrNode: :"some text"
                     // EvStrNode :"#{some expression}"
                     // Ruby 1.9 allows empty strings as symbols
                     if ($2 == null) {
                         $$ = support.asSymbol(lexer.getRubySourceline(), new ByteList(new byte[] {}));
                     } else if ($2 instanceof DStrNode) {
                         $$ = new DSymbolNode($2.getLine(), $<DStrNode>2);
                     } else if ($2 instanceof StrNode) {
                         $$ = support.asSymbol($2.getLine(), $2);
                     } else {
                         $$ = new DSymbolNode($2.getLine());
                         $<DSymbolNode>$.add($2);
                     }
                }

numeric         : simple_numeric {
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
                    $$ = new InstVarNode(lexer.tokline, support.symbolID($1));
                }
                | tGVAR {
                    $$ = new GlobalVarNode(lexer.tokline, support.symbolID($1));
                }
                | tCONSTANT {
                    $$ = new ConstNode(lexer.tokline, support.symbolID($1));
                }
                | tCVAR {
                    $$ = new ClassVarNode(lexer.tokline, support.symbolID($1));
                } /*mri:user_variable*/
                | /*mri:keyword_variable*/ keyword_nil { 
                    $$ = new NilNode(lexer.tokline);
                }
                | keyword_self {
                    $$ = new SelfNode(lexer.tokline);
                }
                | keyword_true { 
                    $$ = new TrueNode(lexer.tokline);
                }
                | keyword_false {
                    $$ = new FalseNode(lexer.tokline);
                }
                | keyword__FILE__ {
                    $$ = new FileNode(lexer.tokline, new ByteList(lexer.getFile().getBytes(),
                    support.getConfiguration().getRuntime().getEncodingService().getLocaleEncoding()));
                }
                | keyword__LINE__ {
                    $$ = new FixnumNode(lexer.tokline, lexer.tokline+1);
                }
                | keyword__ENCODING__ {
                    $$ = new EncodingNode(lexer.tokline, lexer.getEncoding());
                } /*mri:keyword_variable*/

// [!null]
var_lhs         : /*mri:user_variable*/ tIDENTIFIER {
                    $$ = support.assignableLabelOrIdentifier($1, null);
                }
                | tIVAR {
                    $$ = new InstAsgnNode(lexer.tokline, support.symbolID($1), NilImplicitNode.NIL);
                }
                | tGVAR {
                    $$ = new GlobalAsgnNode(lexer.tokline, support.symbolID($1), NilImplicitNode.NIL);
                }
                | tCONSTANT {
                    if (support.isInDef()) support.compile_error("dynamic constant assignment");

                    $$ = new ConstDeclNode(lexer.tokline, support.symbolID($1), null, NilImplicitNode.NIL);
                }
                | tCVAR {
                    $$ = new ClassVarAsgnNode(lexer.tokline, support.symbolID($1), NilImplicitNode.NIL);
                } /*mri:user_variable*/
                | /*mri:keyword_variable*/ keyword_nil {
                    support.compile_error("Can't assign to nil");
                    $$ = null;
                }
                | keyword_self {
                    support.compile_error("Can't change the value of self");
                    $$ = null;
                }
                | keyword_true {
                    support.compile_error("Can't assign to true");
                    $$ = null;
                }
                | keyword_false {
                    support.compile_error("Can't assign to false");
                    $$ = null;
                }
                | keyword__FILE__ {
                    support.compile_error("Can't assign to __FILE__");
                    $$ = null;
                }
                | keyword__LINE__ {
                    support.compile_error("Can't assign to __LINE__");
                    $$ = null;
                }
                | keyword__ENCODING__ {
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

superclass      : tLT {
                   lexer.setState(EXPR_BEG);
                   lexer.commandStart = true;
                } expr_value term {
                    $$ = $3;
                }
                | /* none */ {
                   $$ = null;
                }

// [!null]
f_arglist       : tLPAREN2 f_args rparen {
                    $$ = $2;
                    lexer.setState(EXPR_BEG);
                    lexer.commandStart = true;
                }
                | {
                   $$ = lexer.inKwarg;
                   lexer.inKwarg = true;
                   lexer.setState(lexer.getState() | EXPR_LABEL);
                } f_args term {
                   lexer.inKwarg = $<Boolean>1;
                    $$ = $2;
                    lexer.setState(EXPR_BEG);
                    lexer.commandStart = true;
                }


args_tail       : f_kwarg ',' f_kwrest opt_f_block_arg {
                    $$ = support.new_args_tail($1.getLine(), $1, $3, $4);
                }
                | f_kwarg opt_f_block_arg {
                    $$ = support.new_args_tail($1.getLine(), $1, (ByteList) null, $2);
                }
                | f_kwrest opt_f_block_arg {
                    $$ = support.new_args_tail(lexer.getRubySourceline(), null, $1, $2);
                }
                | f_block_arg {
                    $$ = support.new_args_tail($1.getLine(), null, (ByteList) null, $1);
                }

opt_args_tail   : ',' args_tail {
                    $$ = $2;
                }
                | /* none */ {
                    $$ = support.new_args_tail(lexer.getRubySourceline(), null, (ByteList) null, null);
                }

// [!null]
f_args          : f_arg ',' f_optarg ',' f_rest_arg opt_args_tail {
                    $$ = support.new_args($1.getLine(), $1, $3, $5, null, $6);
                }
                | f_arg ',' f_optarg ',' f_rest_arg ',' f_arg opt_args_tail {
                    $$ = support.new_args($1.getLine(), $1, $3, $5, $7, $8);
                }
                | f_arg ',' f_optarg opt_args_tail {
                    $$ = support.new_args($1.getLine(), $1, $3, null, null, $4);
                }
                | f_arg ',' f_optarg ',' f_arg opt_args_tail {
                    $$ = support.new_args($1.getLine(), $1, $3, null, $5, $6);
                }
                | f_arg ',' f_rest_arg opt_args_tail {
                    $$ = support.new_args($1.getLine(), $1, null, $3, null, $4);
                }
                | f_arg ',' f_rest_arg ',' f_arg opt_args_tail {
                    $$ = support.new_args($1.getLine(), $1, null, $3, $5, $6);
                }
                | f_arg opt_args_tail {
                    $$ = support.new_args($1.getLine(), $1, null, null, null, $2);
                }
                | f_optarg ',' f_rest_arg opt_args_tail {
                    $$ = support.new_args($1.getLine(), null, $1, $3, null, $4);
                }
                | f_optarg ',' f_rest_arg ',' f_arg opt_args_tail {
                    $$ = support.new_args($1.getLine(), null, $1, $3, $5, $6);
                }
                | f_optarg opt_args_tail {
                    $$ = support.new_args($1.getLine(), null, $1, null, null, $2);
                }
                | f_optarg ',' f_arg opt_args_tail {
                    $$ = support.new_args($1.getLine(), null, $1, null, $3, $4);
                }
                | f_rest_arg opt_args_tail {
                    $$ = support.new_args($1.getLine(), null, null, $1, null, $2);
                }
                | f_rest_arg ',' f_arg opt_args_tail {
                    $$ = support.new_args($1.getLine(), null, null, $1, $3, $4);
                }
                | args_tail {
                    $$ = support.new_args($1.getLine(), null, null, null, null, $1);
                }
                | /* none */ {
                    $$ = support.new_args(lexer.getRubySourceline(), null, null, null, null, (ArgsTailHolder) null);
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

// ByteList:f_norm_arg [!null]
f_norm_arg      : f_bad_arg {
                    $$ = $1; // Not really reached
                }
                | tIDENTIFIER {
                    $$ = support.formal_argument($1);
                }

f_arg_asgn      : f_norm_arg {
                    lexer.setCurrentArg($1);
                    $$ = support.arg_var($1);
                }

f_arg_item      : f_arg_asgn {
                    lexer.setCurrentArg(null);
                    $$ = $1;
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
                    $$ = new ArrayNode(lexer.getRubySourceline(), $1);
                }
                | f_arg ',' f_arg_item {
                    $1.add($3);
                    $$ = $1;
                }

f_label 	: tLABEL {
                    support.arg_var(support.formal_argument($1));
                    lexer.setCurrentArg($1);
                    $$ = $1;
                }

f_kw            : f_label arg_value {
                    lexer.setCurrentArg(null);
                    $$ = new KeywordArgNode($2.getLine(), support.assignableKeyword($1, $2));
                }
                | f_label {
                    lexer.setCurrentArg(null);
                    $$ = new KeywordArgNode(lexer.getRubySourceline(), support.assignableKeyword($1, new RequiredKeywordArgumentValueNode()));
                }

f_block_kw      : f_label primary_value {
                    $$ = new KeywordArgNode(support.getPosition($2), support.assignableKeyword($1, $2));
                }
                | f_label {
                    $$ = new KeywordArgNode(lexer.getRubySourceline(), support.assignableKeyword($1, new RequiredKeywordArgumentValueNode()));
                }
             

f_block_kwarg   : f_block_kw {
                    $$ = new ArrayNode($1.getLine(), $1);
                }
                | f_block_kwarg ',' f_block_kw {
                    $$ = $1.add($3);
                }

f_kwarg         : f_kw {
                    $$ = new ArrayNode($1.getLine(), $1);
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
                    $$ = support.INTERNAL_ID;
                }

f_opt           : f_arg_asgn '=' arg_value {
                    lexer.setCurrentArg(null);
                    $$ = new OptArgNode(support.getPosition($3), support.assignableLabelOrIdentifier($1.getName().getBytes(), $3));
                }

f_block_opt     : f_arg_asgn '=' primary_value {
                    lexer.setCurrentArg(null);
                    $$ = new OptArgNode(support.getPosition($3), support.assignableLabelOrIdentifier($1.getName().getBytes(), $3));
                }

f_block_optarg  : f_block_opt {
                    $$ = new BlockNode($1.getLine()).add($1);
                }
                | f_block_optarg ',' f_block_opt {
                    $$ = support.appendToBlock($1, $3);
                }

f_optarg        : f_opt {
                    $$ = new BlockNode($1.getLine()).add($1);
                }
                | f_optarg ',' f_opt {
                    $$ = support.appendToBlock($1, $3);
                }

restarg_mark    : tSTAR2 {
                    $$ = $1;
                }
                | tSTAR {
                    $$ = $1;
                }

// [!null]
f_rest_arg      : restarg_mark tIDENTIFIER {
                    if (!support.is_local_id($2)) {
                        support.yyerror("rest argument must be local variable");
                    }
                    
                    $$ = new RestArgNode(support.arg_var(support.shadowing_lvar($2)));
                }
                | restarg_mark {
  // FIXME: bytelist_love: somewhat silly to remake the empty bytelist over and over but this type should change (using null vs "" is a strange distinction).
  $$ = new UnnamedRestArgNode(lexer.getRubySourceline(), support.symbolID(CommonByteLists.EMPTY), support.getCurrentScope().addVariable("*"));
                }

// [!null]
blkarg_mark     : tAMPER2 {
                    $$ = $1;
                }
                | tAMPER {
                    $$ = $1;
                }

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
                    support.value_expr(lexer, $1);
                    $$ = $1;
                }
                | tLPAREN2 {
                    lexer.setState(EXPR_BEG);
                } expr rparen {
                    if ($3 == null) {
                        support.yyerror("can't define single method for ().");
                    } else if ($3 instanceof ILiteralNode) {
                        support.yyerror("can't define single method for literals.");
                    }
                    support.value_expr(lexer, $3);
                    $$ = $3;
                }

// HashNode: [!null]
assoc_list      : none {
                    $$ = new HashNode(lexer.getRubySourceline());
                }
                | assocs trailer {
                    $$ = support.remove_duplicate_keys($1);
                }

// [!null]
assocs          : assoc {
                    $$ = new HashNode(lexer.getRubySourceline(), $1);
                }
                | assocs ',' assoc {
                    $$ = $1.add($3);
                }

// Cons: [!null]
assoc           : arg_value tASSOC arg_value {
                    $$ = support.createKeyValue($1, $3);
                }
                | tLABEL arg_value {
                    Node label = support.asSymbol(support.getPosition($2), $1);
                    $$ = support.createKeyValue(label, $2);
                }
                | tSTRING_BEG string_contents tLABEL_END arg_value {
                    if ($2 instanceof StrNode) {
                        DStrNode dnode = new DStrNode(support.getPosition($2), lexer.getEncoding());
                        dnode.add($2);
                        $$ = support.createKeyValue(new DSymbolNode(support.getPosition($2), dnode), $4);
                    } else if ($2 instanceof DStrNode) {
                        $$ = support.createKeyValue(new DSymbolNode(support.getPosition($2), $<DStrNode>2), $4);
                    } else {
                        support.compile_error("Uknown type for assoc in strings: " + $2);
                    }

                }
                | tDSTAR arg_value {
                    $$ = support.createKeyValue(null, $2);
                }

operation       : tIDENTIFIER {
                    $$ = $1;
                }
                | tCONSTANT {
                    $$ = $1;
                }
                | tFID {
                    $$ = $1;
                }
operation2      : tIDENTIFIER  {
                    $$ = $1;
                }
                | tCONSTANT {
                    $$ = $1;
                }
                | tFID {
                    $$ = $1;
                }
                | op {
                    $$ = $1;
                }
                    
operation3      : tIDENTIFIER {
                    $$ = $1;
                }
                | tFID {
                    $$ = $1;
                }
                | op {
                    $$ = $1;
                }
                    
dot_or_colon    : tDOT {
                    $$ = $1;
                }
                | tCOLON2 {
                    $$ = $1;
                }

call_op 	: tDOT {
                    $$ = $1;
                }
                | tANDDOT {
                    $$ = $1;
                }

call_op2        : call_op
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
    public RubyParserResult parse(ParserConfiguration configuration) throws IOException {
        support.reset();
        support.setConfiguration(configuration);
        support.setResult(new RubyParserResult());
        
        yyparse(lexer, configuration.isDebug() ? new YYDebug() : null);
        
        return support.getResult();
    }
}
