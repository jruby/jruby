/*
 * ParserHelper.java - No description
 * Created on 04. Oktober 2001, 23:26
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

import org.jruby.*;
import org.jruby.interpreter.*;
import org.jruby.interpreter.nodes.*;
import org.jruby.original.*;
import org.jruby.util.*;

/**
 *  Description of the Class
 *
 *@author     jpetersen
 *@created    4. Oktober 2001
 */
public class ParserHelper {
    private Ruby ruby;
    private NodeFactory nf;
    
    private NODE evalTree;
    private NODE evalTreeBegin;
    
    private int inSingle;
    private int inDef;
    
    private boolean inDefined;
    
    private int compileForEval;
    
    private int lexState;
    private int classNest;
    private RubyId curMid;

    // Scanner ?
    
    private boolean rubyInCompile = false;
    private boolean rubyEndSeen;
    
    private int heredocEnd;
    private boolean commandStart = true;
    // XXX is this really needed?
    
    private RubyArray rubyDebugLines; // separate a Ruby string into lines...

    
    public ParserHelper(Ruby ruby) {
        this.ruby = ruby;
    }
    
    public void init() {
        nf = new NodeFactory(ruby);
    }

    private local_vars lvtbl = new local_vars();

    public RubyId newId(int id) {
        return RubyId.newId(ruby, id);
    }
    
    private void yyerror(String message) {
        System.err.println(message);
    }
    
    public void rb_compile_error(String message) {
        System.err.println(message);
    }

    public void rb_warn(String message) {
        System.err.println("[WARN] " + message);
    }

    public void rb_warning(String message) {
        System.err.println("[WARNING] " + message);
    }

    public void rb_bug(String message) {
        System.err.println("[BUG] " + message);
    }

// ---------------------------------------------------------------------------
// here the parser methods start....
// ---------------------------------------------------------------------------

    /**
     *  Copies position info (added to reduce the need for casts).
     *
     *@param  n1  Description of Parameter
     *@param  n2  Description of Parameter
     */
    public void fixpos(Object n1, NODE n2) {
        fixpos((NODE) n1, n2);
    }


    /**
     *  Description of the Method
     *
     *@param  node1  Description of Parameter
     *@param  node2  Description of Parameter
     *@return        Description of the Returned Value
     */
    public NODE arg_blk_pass(NODE node1, NODE node2) {
        if (node2 != null) {
            node2.nd_head(node1);
            return node2;
        }
        return node1;
    }


    /**
     *  Description of the Method
     */
    void rb_parser_append_print() {
        evalTree = block_append(evalTree, nf.newFCall(ruby.intern("print"), nf.newArray(nf.newGVar(ruby.intern("$_")))));
    }


    /**
     *  Description of the Method
     *
     *@param  chop   Description of Parameter
     *@param  split  Description of Parameter
     */
    void rb_parser_while_loop(int chop, int split) {
        if (split != 0) {
            evalTree = block_append(nf.newGAsgn(ruby.intern("$F"),
                    nf.newCall(nf.newGVar(ruby.intern("$_")), ruby.intern("split"), null)), evalTree);
        }
        if (chop != 0) {
            evalTree = block_append(nf.newCall(nf.newGVar(ruby.intern("$_")),
                    ruby.intern("chop!"), null), evalTree);
        }
        evalTree = nf.newOptN(evalTree);
    }

    /**
     *  Description of the Method
     *
     *@return    Description of the Returned Value
     */
    RubyObject rb_backref_get() {
        if (ruby.getRubyScope().getLocalVars() != null) {
            return ruby.getRubyScope().getLocalVars(1);
        }
        return ruby.getNil();
    }


    /**
     *  Description of the Method
     *
     *@param  val  Description of Parameter
     */
    void rb_backref_set(RubyObject val) {
        if (ruby.getRubyScope().getLocalVars() != null) {
            ruby.getRubyScope().setLocalVars(1, val);
        } else {
            special_local_set('~', val);
        }
    }


    /**
     *  Description of the Method
     *
     *@return    Description of the Returned Value
     */
    RubyObject rb_lastline_get() {
        if (ruby.getRubyScope().getLocalVars() != null) {
            return ruby.getRubyScope().getLocalVars(0);
        }
        return ruby.getNil();
    }


    /**
     *  Description of the Method
     *
     *@param  val  Description of Parameter
     */
    void rb_lastline_set(RubyObject val) {
        if (ruby.getRubyScope().getLocalVars() != null) {
            ruby.getRubyScope().setLocalVars(0, val);
        } else {
            special_local_set('_', val);
        }
    }


    /**
     *  Description of the Method
     *
     *@param  id  Description of Parameter
     *@return     Description of the Returned Value
     */
    public NODE gettable(RubyId id) {
        switch (id.intValue()) {
            case Token.kSELF:
                return nf.newSelf();
            case Token.kNIL:
                return nf.newNil();
            case Token.kTRUE:
                return nf.newTrue();
            case Token.kFALSE:
                return nf.newFalse();
            case Token.k__FILE__:
                return nf.newStr(RubyString.m_newString(ruby, ruby.getSourceFile()));
            case Token.k__LINE__:
                return nf.newLit(RubyFixnum.m_newFixnum(ruby, ruby.getSourceLine()));
            default: {
                if (id.is_local_id()) {
                    if (dyna_in_block() && RubyVarmap.isDefined(ruby, id)) {
                        return nf.newDVar(id);
                    } else if (local_id(id)) {
                        return nf.newLVar(id);
                    }
                    /*
                     *  method call without arguments
                     */
                    return nf.newVCall(id);
                } else if (id.is_global_id()) {
                    return nf.newGVar(id);
                } else if (id.is_instance_id()) {
                    return nf.newIVar(id);
                } else if (id.is_const_id()) {
                    return nf.newConst(id);
                } else if (id.is_class_id()) {
                    if (isInSingle()) {
                        return nf.newCVar2(id);
                    }
                    return nf.newCVar(id);
                }
            }
        }
        rb_bug("invalid id for gettable. id = " + id.intValue() + ", name= " + id.toName());
        return null;
    }


    /**
     *  Copies filename and line number from "orig" to "node".
     *
     *@param  node  Description of Parameter
     *@param  orig  Description of Parameter
     */
    private void fixpos(NODE node, NODE orig) {
        if (node == null) {
            return;
        }
        if (orig == null) {
            return;
        }
        node.nd_file = orig.nd_file;
        node.nd_set_line(orig.nd_line());
    }


    /**
     *  Wraps node with NEWLINE node.
     *
     *@param  node  Description of Parameter
     *@return       Description of the Returned Value
     */
    public NODE newline_node(NODE node) {
        NODE nl = null;
        if (node != null) {
            nl = nf.newNewline(node);
            fixpos(nl, node);
            nl.nd_nth(node.nd_line());
        }
        return nl;
    }


    /**
     *  Description of the Method
     *
     *@param  head  Description of Parameter
     *@param  tail  Description of Parameter
     *@return       Description of the Returned Value
     */
    public NODE block_append(NODE head, NODE tail) {
        if (tail == null) {
            return head;
        }
        if (head == null) {
            return tail;
        }

        NODE end;
        if (head.nd_type() != NODE.NODE_BLOCK) {
            end = nf.newBlock(head);
            end.nd_end(end);
            fixpos(end, head);
            head = end;
        } else {
            end = head.nd_end();
        }

        if (ruby.isVerbose()) {
            NODE nd = end.nd_head();
            while (true) {
                switch (nd.nd_type()) {
                    case NODE.NODE_RETURN:
                    case NODE.NODE_BREAK:
                    case NODE.NODE_NEXT:
                    case NODE.NODE_REDO:
                    case NODE.NODE_RETRY:
                        rb_warning("statement not reached");
                        break;
                    case NODE.NODE_NEWLINE:
                        nd = nd.nd_next();
                        continue;
                    default:
                        break;
                }
                break;
            }
        }

        if (tail.nd_type() != NODE.NODE_BLOCK) {
            tail = nf.newBlock(tail);
            tail.nd_end(tail);
        }
        end.nd_next(tail);
        head.nd_end(tail.nd_end());
        return head;
    }


    /**
     *  Description of the Method
     *
     *@param  head  Description of Parameter
     *@param  tail  Description of Parameter
     *@return       Description of the Returned Value
     */
    public NODE list_append(NODE head, NODE tail) {
        if (head == null) {
            return nf.newList(tail);
        }

        NODE last = head;
        while (last.nd_next() != null) {
            last = last.nd_next();
        }

        last.nd_next(nf.newList(tail));
        head.nd_alen(head.nd_alen() + 1);
        return head;
    }


    /**
     *  Description of the Method
     *
     *@param  head  Description of Parameter
     *@param  tail  Description of Parameter
     *@return       Description of the Returned Value
     */
    public NODE list_concat(NODE head, NODE tail) {
        NODE last = head;

        while (last.nd_next() != null) {
            last = last.nd_next();
        }

        last.nd_next(tail);
        head.nd_alen(head.nd_alen() + tail.nd_alen());

        return head;
    }


    /**
     *  Description of the Method
     *
     *@param  recv  Description of Parameter
     *@param  op    Description of Parameter
     *@param  narg  Description of Parameter
     *@param  arg1  Description of Parameter
     *@return       Description of the Returned Value
     */
    public NODE call_op(NODE recv, int op, int narg, NODE arg1) {
        return call_op(recv, newId(op), narg, arg1);
    }


    /**
     *  Description of the Method
     *
     *@param  recv  Description of Parameter
     *@param  id    Description of Parameter
     *@param  narg  Description of Parameter
     *@param  arg1  Description of Parameter
     *@return       Description of the Returned Value
     */
    public NODE call_op(NODE recv, RubyId id, int narg, NODE arg1) {
        value_expr(recv);
        if (narg == 1) {
            value_expr(arg1);
        }
        return nf.newCall(recv, id, narg == 1 ? nf.newList(arg1) : null);
    }


    /**
     *  Description of the Method
     *
     *@param  node1  Description of Parameter
     *@param  node2  Description of Parameter
     *@return        Description of the Returned Value
     */
    public NODE match_gen(NODE node1, NODE node2) {
        local_cnt('~');

        switch (node1.nd_type()) {
            case NODE.NODE_DREGX:
            case NODE.NODE_DREGX_ONCE:
                return nf.newMatch2(node1, node2);
            case NODE.NODE_LIT:
                if (node1.nd_lit() instanceof RubyRegexp) {
                    return nf.newMatch2(node1, node2);
                }
        }

        switch (node2.nd_type()) {
            case NODE.NODE_DREGX:
            case NODE.NODE_DREGX_ONCE:
                return nf.newMatch3(node2, node1);
            case NODE.NODE_LIT:
                if (node2.nd_lit() instanceof RubyRegexp) {
                    return nf.newMatch3(node2, node1);
                }
        }

        return nf.newCall(node1, newId(Token.tMATCH), nf.newList(node2));
    }


    /**
     *  Description of the Method
     *
     *@param  id   Description of Parameter
     *@param  val  Description of Parameter
     *@return      Description of the Returned Value
     */
    public NODE assignable(RubyId id, NODE val) {
        value_expr(val);
        switch (id.intValue()) {
            case Token.kSELF:
                yyerror("Can't change the value of self");
            case Token.kNIL:
                yyerror("Can't assign to nil");
            case Token.kTRUE:
                yyerror("Can't assign to true");
            case Token.kFALSE:
                yyerror("Can't assign to false");
            case Token.k__FILE__:
                yyerror("Can't assign to __FILE__");
            case Token.k__LINE__:
                yyerror("Can't assign to __LINE__");
            default: {
                if (id.is_local_id()) {
                    if (RubyVarmap.isCurrent(ruby, id)) {
                        return nf.newDAsgnCurr(id, val);
                    } else if (RubyVarmap.isDefined(ruby, id)) {
                        return nf.newDAsgn(id, val);
                    } else if (local_id(id) || !dyna_in_block()) {
                        return nf.newLAsgn(id, val);
                    } else {
                        RubyVarmap.push(ruby, id, ruby.getNil());
                        return nf.newDAsgnCurr(id, val);
                    }
                } else if (id.is_global_id()) {
                    return nf.newGAsgn(id, val);
                } else if (id.is_instance_id()) {
                    return nf.newIAsgn(id, val);
                } else if (id.is_const_id()) {
                    if (isInDef() || isInSingle()) {
                        yyerror("dynamic constant assignment");
                    }
                    return nf.newCDecl(id, val);
                } else if (id.is_class_id()) {
                    if (isInSingle()) {
                        return nf.newCVAsgn(id, val);
                    }
                    return nf.newCVDecl(id, val);
                } else {
                    rb_bug("bad id for variable");
                }
            }
        }
        return null;
    }

    /**
     *  Description of the Method
     *
     *@param  recv  Description of Parameter
     *@param  idx   Description of Parameter
     *@return       Description of the Returned Value
     */
    public NODE aryset(NODE recv, NODE idx) {
        value_expr(recv);
        return nf.newCall(recv, newId(Token.tASET), idx);
    }


    /**
     *  Description of the Method
     *
     *@param  recv  Description of Parameter
     *@param  id    Description of Parameter
     *@return       Description of the Returned Value
     */
    public NODE attrset(NODE recv, RubyId id) {
        value_expr(recv);

        return nf.newCall(recv, id.toAttrSetId(), null);
    }


    /**
     *  Description of the Method
     *
     *@param  node  Description of Parameter
     */
    public void rb_backref_error(NODE node) {
        switch (node.nd_type()) {
            case NODE.NODE_NTH_REF:
                rb_compile_error("Can't set variable $" + node.nd_nth());
                break;
            case NODE.NODE_BACK_REF:
                rb_compile_error("Can't set variable $" + node.nd_nth());
                break;
        }
    }


    /**
     *  Description of the Method
     *
     *@param  node1  Description of Parameter
     *@param  node2  Description of Parameter
     *@return        Description of the Returned Value
     */
    public NODE arg_concat(NODE node1, NODE node2) {
        if (node2 == null) {
            return node1;
        }
        return nf.newArgsCat(node1, node2);
    }


    /**
     *  Description of the Method
     *
     *@param  node1  Description of Parameter
     *@param  node2  Description of Parameter
     *@return        Description of the Returned Value
     */
    private NODE arg_add(NODE node1, NODE node2) {
        if (node1 == null) {
            return nf.newList(node2);
        }
        if (node1.nd_type() == NODE.NODE_ARRAY) {
            return list_append(node1, node2);
        } else {
            return nf.newArgsPush(node1, node2);
        }
    }


    /**
     *  Description of the Method
     *
     *@param  lhs  Description of Parameter
     *@param  rhs  Description of Parameter
     *@return      Description of the Returned Value
     */
    public NODE node_assign(NODE lhs, NODE rhs) {
        if (lhs == null) {
            return null;
        }

        value_expr(rhs);
        switch (lhs.nd_type()) {
            case NODE.NODE_GASGN:
            case NODE.NODE_IASGN:
            case NODE.NODE_LASGN:
            case NODE.NODE_DASGN:
            case NODE.NODE_DASGN_CURR:
            case NODE.NODE_MASGN:
            case NODE.NODE_CDECL:
            case NODE.NODE_CVDECL:
            case NODE.NODE_CVASGN:
                lhs.nd_value(rhs);
                break;
            case NODE.NODE_CALL:
                lhs.nd_args(arg_add(lhs.nd_args(), rhs));
                break;
            default:
                /*
                 *  should not happen
                 */
                break;
        }

        if (rhs != null) {
            fixpos(lhs, rhs);
        }
        return lhs;
    }


    /**
     *  Description of the Method
     *
     *@param  node  Description of Parameter
     *@return       Description of the Returned Value
     */
    public boolean value_expr(NODE node) {
        if (node == null) {
            return true;
        }

        switch (node.nd_type()) {
            case NODE.NODE_RETURN:
            case NODE.NODE_BREAK:
            case NODE.NODE_NEXT:
            case NODE.NODE_REDO:
            case NODE.NODE_RETRY:
            case NODE.NODE_WHILE:
            case NODE.NODE_UNTIL:
            case NODE.NODE_CLASS:
            case NODE.NODE_MODULE:
            case NODE.NODE_DEFN:
            case NODE.NODE_DEFS:
                yyerror("void value expression");
                return false;
            case NODE.NODE_BLOCK:
                while (node.nd_next() != null) {
                    node = node.nd_next();
                }
                return value_expr(node.nd_head());
            case NODE.NODE_BEGIN:
                return value_expr(node.nd_body());
            case NODE.NODE_IF:
                return value_expr(node.nd_body()) && value_expr(node.nd_else());
            case NODE.NODE_NEWLINE:
                return value_expr(node.nd_next());
            default:
                return true;
        }
    }


    /**
     *  Description of the Method
     *
     *@param  node  Description of Parameter
     */
    public void void_expr(NODE node) {
        String useless = null;

        if (!ruby.isVerbose()) {
            return;
        }
        if (node == null) {
            return;
        }

        while(true) {
            switch (node.nd_type()) {
                case NODE.NODE_NEWLINE:
                    node = node.nd_next();
                    continue;
                case NODE.NODE_CALL:
                    switch (node.nd_mid().intValue()) {
                        case '+':
                        case '-':
                        case '*':
                        case '/':
                        case '%':
                        case Token.tPOW:
                        case Token.tUPLUS:
                        case Token.tUMINUS:
                        case '|':
                        case '^':
                        case '&':
                        case Token.tCMP:
                        case '>':
                        case Token.tGEQ:
                        case '<':
                        case Token.tLEQ:
                        case Token.tEQ:
                        case Token.tNEQ:
                            /*
                             *  case tAREF:
                             *  case tRSHFT:
                             *  case tCOLON2:
                             *  case tCOLON3: FIX 1.6.5
                             */
                        useless = ((RubyId)node.nd_mid()).toName();
                        break;
                    }
                    break;
                case NODE.NODE_LVAR:
                case NODE.NODE_DVAR:
                case NODE.NODE_GVAR:
                case NODE.NODE_IVAR:
                case NODE.NODE_CVAR:
                case NODE.NODE_NTH_REF:
                case NODE.NODE_BACK_REF:
                    useless = "a variable";
                    break;
                case NODE.NODE_CONST:
                case NODE.NODE_CREF:
                    useless = "a constant";
                    break;
                case NODE.NODE_LIT:
                case NODE.NODE_STR:
                case NODE.NODE_DSTR:
                case NODE.NODE_DREGX:
                case NODE.NODE_DREGX_ONCE:
                    useless = "a literal";
                    break;
                case NODE.NODE_COLON2:
                case NODE.NODE_COLON3:
                    useless = "::";
                    break;
                case NODE.NODE_DOT2:
                    useless = "..";
                    break;
                case NODE.NODE_DOT3:
                    useless = "...";
                    break;
                case NODE.NODE_SELF:
                    useless = "self";
                    break;
                case NODE.NODE_NIL:
                    useless = "nil";
                    break;
                case NODE.NODE_TRUE:
                    useless = "true";
                    break;
                case NODE.NODE_FALSE:
                    useless = "false";
                    break;
                case NODE.NODE_DEFINED:
                    useless = "defined?";
                    break;
            }
            break;
        }

        if (useless != null) {
            int line = ruby.getSourceLine();

            ruby.setSourceLine(node.nd_line());
            rb_warn("useless use of " + useless + "in void context");
            ruby.setSourceLine(line);
        }
    }

    /**
     *  Description of the Method
     *
     *@param  node  Description of Parameter
     */
    public void void_stmts(NODE node) {
        if (!ruby.isVerbose()) {
            return;
        }
        if (node == null) {
            return;
        }
        if (node.nd_type() != NODE.NODE_BLOCK) {
            return;
        }

        while (true) {
            if (node.nd_next() == null) {
                return;
            }
            void_expr(node.nd_head());
            node = node.nd_next();
        }
    }


    /**
     *  Description of the Method
     *
     *@param  node  Description of Parameter
     *@return       Description of the Returned Value
     */
    private boolean assign_in_cond(NODE node) {
        switch (node.nd_type()) {
            case NODE.NODE_MASGN:
                yyerror("multiple assignment in conditional");
                return true;
            case NODE.NODE_LASGN:
            case NODE.NODE_DASGN:
            case NODE.NODE_GASGN:
            case NODE.NODE_IASGN:
                break;
            case NODE.NODE_NEWLINE:
            default:
                return false;
        }

        switch (node.nd_value().nd_type()) {
            case NODE.NODE_LIT:
            case NODE.NODE_STR:
            case NODE.NODE_NIL:
            case NODE.NODE_TRUE:
            case NODE.NODE_FALSE:
                /*
                 *  reports always
                 */
                rb_warn("found = in conditional, should be ==");
                return true;
            case NODE.NODE_DSTR:
            case NODE.NODE_XSTR:
            case NODE.NODE_DXSTR:
            case NODE.NODE_EVSTR:
            case NODE.NODE_DREGX:
            default:
                break;
        }
        return true;
    }


    /**
     *  Description of the Method
     *
     *@return    Description of the Returned Value
     */
    private boolean e_option_supplied() {
        return ruby.getSourceFile().equals("-e");
    }


    /**
     *  Description of the Method
     *
     *@param  str  Description of Parameter
     */
    private void warn_unless_e_option(String str) {
        if (!e_option_supplied()) {
            rb_warn(str);
        }
    }


    /**
     *  Description of the Method
     *
     *@param  str  Description of Parameter
     */
    private void warning_unless_e_option(String str) {
        if (!e_option_supplied()) {
            rb_warning(str);
        }
    }


    /**
     *  Description of the Method
     *
     *@param  node   Description of Parameter
     *@param  logop  Description of Parameter
     *@return        Description of the Returned Value
     */
    private NODE range_op(NODE node, int logop) {
        int type; // enum node_type

        if (logop != 0) {
            return node;
        }
        if (!e_option_supplied()) {
            return node;
        }

        warn_unless_e_option("integer literal in condition");
        node = cond0(node, 0);
        //XXX second argument was missing
        type = node.nd_type();
        if (type == NODE.NODE_NEWLINE) {
            node = node.nd_next();
        }
        if (type == NODE.NODE_LIT && node.nd_lit() instanceof RubyFixnum) {
            return call_op(node, Token.tEQ, 1, nf.newGVar(ruby.intern("$.")));
        }
        return node;
    }


    /**
     *  Description of the Method
     *
     *@param  node   Description of Parameter
     *@param  logop  Description of Parameter
     *@return        Description of the Returned Value
     */
    private NODE cond0(NODE node, int logop) {
        int type = node.nd_type(); // enum node_type
        
        assign_in_cond(node);
        switch (type) {
            case NODE.NODE_DSTR:
            case NODE.NODE_STR:
                if (logop != 0) {
                    break;
                }
                rb_warn("string literal in condition");
                break;
            case NODE.NODE_DREGX:
            case NODE.NODE_DREGX_ONCE:
                warning_unless_e_option("regex literal in condition");
                local_cnt('_');
                local_cnt('~');
                return nf.newMatch2(node, nf.newGVar(ruby.intern("$_")));
            case NODE.NODE_DOT2:
            case NODE.NODE_DOT3:
                node.nd_beg(range_op(node.nd_beg(), logop));
                node.nd_end(range_op(node.nd_end(), logop));
                if (type == NODE.NODE_DOT2) {
                    node.nd_set_type(NODE.NODE_FLIP2);
                } else if (type == NODE.NODE_DOT3) {
                    node.nd_set_type(NODE.NODE_FLIP3);
                }
                node.nd_cnt(local_append(newId(0)));
                warning_unless_e_option("range literal in condition");
                break;
            case NODE.NODE_LIT:
                if (node.nd_lit() instanceof RubyRegexp) {
                    warning_unless_e_option("regex literal in condition");
                    node.nd_set_type(NODE.NODE_MATCH);
                    local_cnt('_');
                    local_cnt('~');
                }
        }
        return node;
    }


    /**
     *  Description of the Method
     *
     *@param  node   Description of Parameter
     *@param  logop  Description of Parameter
     *@return        Description of the Returned Value
     */
    private NODE cond1(NODE node, int logop) {
        if (node == null) {
            return null;
        }
        if (node.nd_type() == NODE.NODE_NEWLINE) {
            node.nd_next(cond0(node.nd_next(), logop));
            return node;
        }
        return cond0(node, logop);
    }


    /**
     *  Description of the Method
     *
     *@param  node  Description of Parameter
     *@return       Description of the Returned Value
     */
    public NODE cond(NODE node) {
        return cond1(node, 0);
    }


    /**
     *  Description of the Method
     *
     *@param  type   Description of Parameter
     *@param  left   Description of Parameter
     *@param  right  Description of Parameter
     *@return        Description of the Returned Value
     */
    public NODE logop(/*node_type*/ int type, NODE left, NODE right) {
        value_expr(left);
        return nf.newDefaultNode(type, cond1(left, 1), cond1(right, 1), null);
    }


    /**
     *  Description of the Method
     *
     *@param  node  Description of Parameter
     *@return       Description of the Returned Value
     */
    public NODE ret_args(NODE node) {
        if (node != null) {
            if (node.nd_type() == NODE.NODE_BLOCK_PASS) {
                rb_compile_error("block argument should not be given");
            }
        }
        return node;
    }


    /*
     *  private NODE arg_prepend(NODE node1, NODE node2) {
     *  switch (nodetype(node2)) {
     *  case NODE.NODE_ARRAY:
     *  return list_concat(NEW_LIST(node1), node2);
     *  case NODE.NODE_RESTARGS:
     *  return arg_concat(node1, node2.nd_head());
     *  case NODE.NODE_BLOCK_PASS:
     *  node2.nd_body(arg_prepend(node1, node2.nd_body()));
     *  return node2;
     *  default:
     *  rb_bug("unknown nodetype(%d) for arg_prepend");
     *  }
     *  return null;			// not reached
     *  }
     */
    
    /**
     *  Description of the Method
     *
     *@param  r  Description of Parameter
     *@param  m  Description of Parameter
     *@param  a  Description of Parameter
     *@return    Description of the Returned Value
     */
    public NODE new_call(NODE r, RubyId m, NODE a) {
        if (a != null && a.nd_type() == NODE.NODE_BLOCK_PASS) {
            a.nd_iter(nf.newCall(r, m, a.nd_head()));
            return a;
        }
        return nf.newCall(r, m, a);
    }


    /**
     *  Description of the Method
     *
     *@param  m  Description of Parameter
     *@param  a  Description of Parameter
     *@return    Description of the Returned Value
     */
    public NODE new_fcall(RubyId m, NODE a) {
        if (a != null && a.nd_type() == NODE.NODE_BLOCK_PASS) {
            a.nd_iter(nf.newFCall(m, a.nd_head()));
            return a;
        }
        return nf.newFCall(m, a);
    }


    /**
     *  Description of the Method
     *
     *@param  a  Description of Parameter
     *@return    Description of the Returned Value
     */
    public NODE new_super(NODE a) {
        if (a != null && a.nd_type() == NODE.NODE_BLOCK_PASS) {
            a.nd_iter(nf.newSuper(a.nd_head()));
            return a;
        }
        return nf.newSuper(a);
    }


    /**
     *  Description of the Method
     */
    public void local_push() {
        local_vars local = new local_vars();
        local.prev = lvtbl;
        local.nofree = 0;
        local.cnt = 0;
        local.tbl = null;
        local.dlev = 0;
        lvtbl = local;
    }


    /**
     *  Description of the Method
     */
    public void local_pop() {
        local_vars local = lvtbl.prev;

        if (lvtbl.tbl != null) {
            if (lvtbl.nofree == 0) {
                // free(lvtbl.tbl);
            } else {
                lvtbl.tbl[0] = newId(lvtbl.cnt);
            }
        }
        // free(lvtbl);
        lvtbl = local;
    }


    /**
     *  Description of the Method
     *
     *@return    Description of the Returned Value
     */
    public RubyId[] local_tbl() {
        lvtbl.nofree = 1;
        return lvtbl.tbl;
    }


    /**
     *  Description of the Method
     *
     *@param  id  Description of Parameter
     *@return     Description of the Returned Value
     */
    private int local_append(RubyId id) {
        if (lvtbl.tbl == null) {
            lvtbl.tbl = new RubyId[4];
            lvtbl.tbl[0] = newId(0);
            lvtbl.tbl[1] = newId('_');
            lvtbl.tbl[2] = newId('~');
            lvtbl.cnt = 2;
            if (id.intValue() == '_') {
                return 0;
            }
            if (id.intValue() == '~') {
                return 1;
            }
        } else {
            RubyId[] ntbl = new RubyId[lvtbl.cnt + 2];
            System.arraycopy(lvtbl.tbl, 0, ntbl, 0, lvtbl.tbl.length);
            lvtbl.tbl = ntbl;
        }

        lvtbl.tbl[lvtbl.cnt + 1] = id;
        return lvtbl.cnt++;
    }


    /**
     *  Description of the Method
     *
     *@param  id  Description of Parameter
     *@return     Description of the Returned Value
     */
    public int local_cnt(int id) {
        return local_cnt(newId(id));
    }


    /**
     *  Description of the Method
     *
     *@param  id  Description of Parameter
     *@return     Description of the Returned Value
     */
    public int local_cnt(RubyId id) {
        if (id == null) {
            return lvtbl.cnt;
        }

        for (int cnt = 1, max = lvtbl.cnt + 1; cnt < max; cnt++) {
            if (lvtbl.tbl[cnt] == id) {
                return cnt - 1;
            }
        }
        return local_append(id);
    }

    public boolean local_id(RubyId id) {
        int i;
        int max;

        if (lvtbl == null) {
            return false;
        }
        for (i = 3, max = lvtbl.cnt + 1; i < max; i++) {
            if (lvtbl.tbl[i] == id) {
                return true;
            }
        }
        return false;
    }


    /**
     *  Description of the Method
     */
    public void top_local_init() {
        local_push();
        lvtbl.cnt = ruby.getRubyScope().getLocalTbl() != null ? ruby.getRubyScope().getLocalTbl(0).intValue() : 0;
        if (lvtbl.cnt > 0) {
            lvtbl.tbl = new RubyId[lvtbl.cnt + 3];
            System.arraycopy(lvtbl.tbl, 0, ruby.getRubyScope().getLocalTbl(), 0, lvtbl.cnt + 1);
        } else {
            lvtbl.tbl = null;
        }
        if (ruby.getDynamicVars() != null) {
            lvtbl.dlev = 1;
        } else {
            lvtbl.dlev = 0;
        }
    }


    /**
     *  Description of the Method
     */
    public void top_local_setup() {
        int len = lvtbl.cnt;
        int i;

        if (len > 0) {
            i = ruby.getRubyScope().getLocalTbl() != null && ruby.getRubyScope().getLocalTbl(0) != null ? ruby.getRubyScope().getLocalTbl(0).intValue() : 0;

            if (i < len) {
                if (i == 0 || (ruby.getRubyScope().getFlags() & RubyScope.SCOPE_MALLOC) == 0) {
                    ShiftableList vars = new ShiftableList(new RubyObject[len + 1]);
                    int vi = 0;
                    if (ruby.getRubyScope().getLocalVars() != null) {
                        vars.set(0, ruby.getRubyScope().getLocalVars(-1));
                        vars.shift(1);
                        vars.fill(ruby.getRubyScope().getLocalVars(), i);
                        vars.fill(ruby.getNil(), i, len - i);
                    } else {
                        vars.set(0, null);
                        vars.shift(1);
                        vars.fill(ruby.getNil(), len);
                    }
                    ruby.getRubyScope().setLocalVars(vars);
                    ruby.getRubyScope().setFlags(ruby.getRubyScope().getFlags() | RubyScope.SCOPE_MALLOC);
                } else {
                    ShiftableList vars = ruby.getRubyScope().getLocalVars().getList(-1);
                    
                    vars.setSize(len + 1);
                    
                    ruby.getRubyScope().setLocalVars(vars.getList(1));
                    ruby.getRubyScope().getLocalVars().fill(ruby.getNil(), i, len - i);
                }
                
                if (ruby.getRubyScope().getLocalTbl() != null && ruby.getRubyScope().getLocalVars(-1) == null) {
                    ruby.getRubyScope().setLocalTbl(null);
                }
                
                ruby.getRubyScope().setLocalVars(-1, null);
                ruby.getRubyScope().setLocalTbl(local_tbl());
            }
        }
        local_pop();
    }


    /**
     *  Description of the Method
     *
     *@return    Description of the Returned Value
     */
    public RubyVarmap dyna_push() {
        RubyVarmap vars = ruby.getDynamicVars();

        RubyVarmap.push(ruby, null, null);
        lvtbl.dlev++;
        return vars;
    }


    /**
     *  Description of the Method
     *
     *@param  vars  Description of Parameter
     */
    public void dyna_pop(RubyVarmap vars) {
        lvtbl.dlev--;
        
        ruby.setDynamicVars(vars);
    }


    /**
     *  Description of the Method
     *
     *@return    Description of the Returned Value
     */
    private boolean dyna_in_block() {
        return lvtbl.dlev > 0;
    }


    /**
     *  Description of the Method
     *
     *@param  c    Description of Parameter
     *@param  val  Description of Parameter
     */
    private void special_local_set(char c, RubyObject val) {
        top_local_init();
        int cnt = local_cnt(c);
        top_local_setup();
        ruby.getRubyScope().setLocalVars(cnt, val);
    }

    /** Getter for property evalTree.
     * @return Value of property evalTree.
     */
    public NODE getEvalTree() {
        return evalTree;
    }    

    /** Setter for property evalTree.
     * @param evalTree New value of property evalTree.
     */
    public void setEvalTree(NODE evalTree) {
        this.evalTree = evalTree;
    }
    
    /** Getter for property evalTreeBegin.
     * @return Value of property evalTreeBegin.
     */
    public NODE getEvalTreeBegin() {
        return evalTreeBegin;
    }
    
    /** Setter for property evalTreeBegin.
     * @param evalTreeBegin New value of property evalTreeBegin.
     */
    public void setEvalTreeBegin(NODE evalTreeBegin) {
        this.evalTreeBegin = evalTreeBegin;
    }
    
    /** Getter for property inSingle.
     * @return Value of property inSingle.
     */
    public boolean isInSingle() {
        return inSingle != 0;
    }
    
    /** Setter for property inSingle.
     * @param inSingle New value of property inSingle.
     */
    public void setInSingle(int inSingle) {
        this.inSingle = inSingle;
    }
    
    /** Getter for property inDef.
     * @return Value of property inDef.
     */
    public boolean isInDef() {
        return inDef != 0;
    }
    
    /** Setter for property inDef.
     * @param inDef New value of property inDef.
     */
    public void setInDef(int inDef) {
        this.inDef = inDef;
    }
    
    /** Getter for property inSingle.
     * @return Value of property inSingle.
     */
    public int getInSingle() {
        return inSingle;
    }
    
    /** Getter for property inDef.
     * @return Value of property inDef.
     */
    public int getInDef() {
        return inDef;
    }
    
    /** Getter for property inDefined.
     * @return Value of property inDefined.
     */
    public boolean isInDefined() {
        return inDefined;
    }
    
    /** Setter for property inDefined.
     * @param inDefined New value of property inDefined.
     */
    public void setInDefined(boolean inDefined) {
        this.inDefined = inDefined;
    }
    
    /** Getter for property compileForEval.
     * @return Value of property compileForEval.
     */
    public int getCompileForEval() {
        return compileForEval;
    }
    
    public boolean isCompileForEval() {
        return compileForEval != 0;
    }
    
    /** Setter for property compileForEval.
     * @param compileForEval New value of property compileForEval.
     */
    public void setCompileForEval(int compileForEval) {
        this.compileForEval = compileForEval;
    }
    
    /** Getter for property lexState.
     * @return Value of property lexState.
     */
    public int getLexState() {
        return lexState;
    }
    
    /** Setter for property lexState.
     * @param lexState New value of property lexState.
     */
    public void setLexState(int lexState) {
        this.lexState = lexState;
    }
    
    /** Getter for property classNest.
     * @return Value of property classNest.
     */
    public int getClassNest() {
        return classNest;
    }
    
    /** Setter for property classNest.
     * @param classNest New value of property classNest.
     */
    public void setClassNest(int classNest) {
        this.classNest = classNest;
    }
    
    /** Getter for property curMid.
     * @return Value of property curMid.
     */
    public RubyId getCurMid() {
        return curMid;
    }
    
    /** Setter for property curMid.
     * @param curMid New value of property curMid.
     */
    public void setCurMid(RubyId curMid) {
        this.curMid = curMid;
    }
    
    /** Getter for property rubyInCompile.
     * @return Value of property rubyInCompile.
     */
    public boolean isRubyInCompile() {
        return rubyInCompile;
    }
    
    /** Setter for property rubyInCompile.
     * @param rubyInCompile New value of property rubyInCompile.
     */
    public void setRubyInCompile(boolean rubyInCompile) {
        this.rubyInCompile = rubyInCompile;
    }
    
    /** Getter for property rubyEndSeen.
     * @return Value of property rubyEndSeen.
     */
    public boolean isRubyEndSeen() {
        return rubyEndSeen;
    }
    
    /** Setter for property rubyEndSeen.
     * @param rubyEndSeen New value of property rubyEndSeen.
     */
    public void setRubyEndSeen(boolean rubyEndSeen) {
        this.rubyEndSeen = rubyEndSeen;
    }
    
    /** Getter for property rubyDebugLines.
     * @return Value of property rubyDebugLines.
     */
    public RubyArray getRubyDebugLines() {
        return rubyDebugLines;
    }
    
    /** Setter for property rubyDebugLines.
     * @param rubyDebugLines New value of property rubyDebugLines.
     */
    public void setRubyDebugLines(RubyArray rubyDebugLines) {
        this.rubyDebugLines = rubyDebugLines;
    }
    
    /** Getter for property heredocEnd.
     * @return Value of property heredocEnd.
     */
    public int getHeredocEnd() {
        return heredocEnd;
    }
    
    /** Setter for property heredocEnd.
     * @param heredocEnd New value of property heredocEnd.
     */
    public void setHeredocEnd(int heredocEnd) {
        this.heredocEnd = heredocEnd;
    }
    
    /** Getter for property commandStart.
     * @return Value of property commandStart.
     */
    public boolean isCommandStart() {
        return commandStart;
    }
    
    /** Setter for property commandStart.
     * @param commandStart New value of property commandStart.
     */
    public void setCommandStart(boolean commandStart) {
        this.commandStart = commandStart;
    }
    
    /**
     *  Description of the Class
     *
     *@author     jpetersen
     *@created    4. Oktober 2001
     */
    private class local_vars {
        RubyId[] tbl;
        int nofree;
        int cnt;
        int dlev;
        local_vars prev;
    }
}