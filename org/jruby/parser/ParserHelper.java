/*
 * ParserHelper.java - No description
 * Created on 05. November 2001, 21:49
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
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
import org.jruby.nodes.*;
import org.jruby.runtime.*;
import org.jruby.util.*;
import org.jruby.util.collections.*;

/**
 *  Description of the Class
 *
 *@author     jpetersen
 *@created    4. Oktober 2001
 */
public class ParserHelper {
    private Ruby ruby;
    private NodeFactory nf;

    private Node evalTree;
    private Node evalTreeBegin;

    private int inSingle;
    private int inDef;

    private boolean inDefined;

    private int compileForEval;

    private int lexState;
    private int classNest;
    private String curMid;

    // Scanner ?

    private boolean rubyInCompile = false;
    private boolean rubyEndSeen;

    private int heredocEnd;
    private boolean commandStart = true;
    // XXX is this really needed?
    private RubyArray rubyDebugLines; // separate a Ruby string into lines...

	private int _line;

	public String getFile()
	{
		return ruby.getSourceFile();
	}

	public void setLine(int iLine)
	{
		_line = iLine;
	}
	public int getLine()
	{
		return _line;
	}
	
	public void incrementLine()
	{
		_line++;
	}
	
    public ParserHelper(Ruby ruby) {
        this.ruby = ruby;
    }

    public void init() {
        nf = new NodeFactory(ruby);
    }

    private LocalVars lvtbl = new LocalVars();

    /*public RubyId newId(int id) {
        return RubyId.newId(ruby, id);
    }*/

    private void yyerror(String message) {
        ruby.getRuntime().getErrorStream().println(message);
    }

    public void rb_compile_error(String message) {
        ruby.getRuntime().getErrorStream().println(message);
    }

    public void rb_warn(String message) {
        ruby.getRuntime().getErrorStream().println("[WARN] " + message);
    }

    public void rb_warning(String message) {
        ruby.getRuntime().getErrorStream().println("[WARNING] " + message);
    }

    public void rb_bug(String message) {
        ruby.getRuntime().getErrorStream().println("[BUG] " + message);
    }

    // ---------------------------------------------------------------------------
    // here the parser methods start....
    // ---------------------------------------------------------------------------

    public String getOperatorId(int id) {
        switch (id) {
            case Token.tDOT2 :
                return "..";
            case Token.tDOT3 :
                return "...";
            case '+' :
                return "+";
            case '-' :
                return "-";
            case '*' :
                return "*";
            case '/' :
                return "/";
            case '%' :
                return "%";
            case Token.tPOW :
                return "**";
            case Token.tUPLUS :
                return "+@";
            case Token.tUMINUS :
                return "-@";
            case '|' :
                return "|";
            case '^' :
                return "^";
            case '&' :
                return "&";
            case Token.tCMP :
                return "<=>";
            case '>' :
                return ">";
            case Token.tGEQ :
                return ">=";
            case '<' :
                return "<";
            case Token.tLEQ :
                return "<=";
            case Token.tEQ :
                return "==";
            case Token.tEQQ :
                return "===";
            case Token.tNEQ :
                return "!=";
            case Token.tMATCH :
                return "=~";
            case Token.tNMATCH :
                return "!~";
            case '!' :
                return "!";
            case '~' :
                return "~";
            case Token.tAREF :
                return "[]";
            case Token.tASET :
                return "[]=";
            case Token.tLSHFT :
                return "<<";
            case Token.tRSHFT :
                return ">>";
            case Token.tCOLON2 :
                return "::";
            case '`' :
                return "`";
            default :
                rb_bug("Operator: \"" + id + "\" not known.");
                return null;
        }
    }

    /**
     *  Copies position info (added to reduce the need for casts).
     *
     *@param  n1  Description of Parameter
     *@param  n2  Description of Parameter
     */
    public void fixpos(Object n1, Node n2) {
        fixpos((Node) n1, n2);
    }

    /**
     *  Description of the Method
     *
     *@param  node1  Description of Parameter
     *@param  node2  Description of Parameter
     *@return        Description of the Returned Value
     */
    public Node arg_blk_pass(Node node1, Node node2) {
        if (node2 != null) {
            node2.setHeadNode(node1);
            return node2;
        }
        return node1;
    }

    /**
     *  Description of the Method
     */
    void rb_parser_append_print() {
        evalTree = block_append(evalTree, nf.newFCall("print", nf.newArray(nf.newGVar("$_"))));
    }

    /**
     *  Description of the Method
     *
     *@param  chop   Description of Parameter
     *@param  split  Description of Parameter
     */
    void rb_parser_while_loop(int chop, int split) {
        if (split != 0) {
            evalTree = block_append(nf.newGAsgn("$F", nf.newCall(nf.newGVar("$_"), "split", null)), evalTree);
        }
        if (chop != 0) {
            evalTree = block_append(nf.newCall(nf.newGVar("$_"), "chop!", null), evalTree);
        }
        evalTree = nf.newOptN(evalTree);
    }

    /**
     *  Description of the Method
     *
     *@return    Description of the Returned Value
     */
    RubyObject rb_backref_get() {
        if (ruby.getRubyScope().getLocalValues() != null) {
            return ruby.getRubyScope().getValue(1);
        }
        return ruby.getNil();
    }

    /**
     *  Description of the Method
     *
     *@param  val  Description of Parameter
     */
    void rb_backref_set(RubyObject val) {
        if (ruby.getRubyScope().getLocalValues() != null) {
            ruby.getRubyScope().setValue(1, val);
        } else {
            special_local_set("~", val);
        }
    }

    /**
     *  Description of the Method
     *
     *@return    Description of the Returned Value
     */
    RubyObject rb_lastline_get() {
        if (ruby.getRubyScope().getLocalValues() != null) {
            return ruby.getRubyScope().getValue(0);
        }
        return ruby.getNil();
    }

    /**
     *  Description of the Method
     *
     *@param  val  Description of Parameter
     */
    void rb_lastline_set(RubyObject val) {
        if (ruby.getRubyScope().getLocalValues() != null) {
            ruby.getRubyScope().setValue(0, val);
        } else {
            special_local_set("_", val);
        }
    }

    /**
     *  Description of the Method
     *
     *@param  id  Description of Parameter
     *@return     Description of the Returned Value
     */
    public Node gettable(String id) {
        if (id.startsWith("~~")) {
            if (id.endsWith("SELF")) {
                return nf.newSelf();
            } else if (id.endsWith("NIL")) {
                return nf.newNil();
            } else if (id.endsWith("TRUE")) {
                return nf.newTrue();
            } else if (id.endsWith("FALSE")) {
                return nf.newFalse();
            } else if (id.endsWith("__FILE__")) {
                return nf.newStr(RubyString.newString(ruby, ruby.getSourceFile()));
            } else if (id.endsWith("__LINE__")) {
                return nf.newLit(RubyFixnum.newFixnum(ruby, ruby.getSourceLine()));
            }
        } else {
            if (IdUtil.isLocal(id)) {
                if (dyna_in_block() && RubyVarmap.isDefined(ruby, id)) {
                    return nf.newDVar(id);
                } else if (local_id(id)) {
                    return nf.newLVar(id);
                }
                /*
                 *  method call without arguments
                 */
                return nf.newVCall(id);
            } else if (IdUtil.isGlobal(id)) {
                return nf.newGVar(id);
            } else if (IdUtil.isInstanceVariable(id)) {
                return nf.newIVar(id);
            } else if (IdUtil.isConstant(id)) {
                return nf.newConst(id);
            } else if (IdUtil.isClassVariable(id)) {
                if (isInSingle()) {
                    return nf.newCVar2(id);
                }
                return nf.newCVar(id);
            }
        }
        rb_bug("invalid id for gettable. id = " + id);
        return null;
    }

    /**
     *  Copies filename and line number from "orig" to "node".
     *
     *@param  node  Description of Parameter
     *@param  orig  Description of Parameter
     */
    private void fixpos(Node node, Node orig) {
        if (node == null || orig == null) {
            return;
        }

        node.setFile(orig.getFile());
        node.setLine(orig.getLine());
    }

    /**
     *  Wraps node with NEWLINE node.
     *
     *@param  node  Description of Parameter
     *@return       Description of the Returned Value
     */
    public Node newline_node(Node node) {
        NewlineNode nl = null;
        if (node != null) {
            nl = nf.newNewline(node);
            fixpos(nl, node);
            nl.setNth(node.getLine());
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
    public Node block_append(Node head, Node tail) {
        if (tail == null) {
            return head;
        } else if (head == null) {
            return tail;
        }

        Node end;
        if (head instanceof BlockNode) {
            end = ((BlockNode) head).getEndNode();
        } else {
            end = nf.newBlock(head);
            end.setEndNode(end);
            fixpos(end, head);
            head = end;
        }

        if (ruby.isVerbose()) {
            Node nd = end.getHeadNode();
            while (true) {
                switch (nd.getType()) {
                    case Constants.NODE_RETURN :
                    case Constants.NODE_BREAK :
                    case Constants.NODE_NEXT :
                    case Constants.NODE_REDO :
                    case Constants.NODE_RETRY :
                        rb_warning("statement not reached");
                        break;
                    case Constants.NODE_NEWLINE :
                        nd = nd.getNextNode();
                        continue;
                    default :
                        break;
                }
                break;
            }
        }

        if (!(tail instanceof BlockNode)) {
            tail = nf.newBlock(tail);
            tail.setEndNode(tail);
        }
        end.setNextNode(tail);
        head.setEndNode(tail.getEndNode());
        return head;
    }

    /**
     *  Description of the Method
     *
     *@param  head  Description of Parameter
     *@param  tail  Description of Parameter
     *@return       Description of the Returned Value
     */
    public Node list_append(Node head, Node tail) {
        if (head == null) {
            return nf.newList(tail);
        }

        Node last = head;
        while (last.getNextNode() != null) {
            last = last.getNextNode();
        }

        last.setNextNode(nf.newList(tail));
        head.setALength(head.getALength() + 1);
        return head;
    }

    /**
     *  Description of the Method
     *
     *@param  head  Description of Parameter
     *@param  tail  Description of Parameter
     *@return       Description of the Returned Value
     */
    public Node list_concat(Node head, Node tail) {
        Node last = head;

        while (last.getNextNode() != null) {
            last = last.getNextNode();
        }

        last.setNextNode(tail);
        head.setALength(head.getALength() + tail.getALength());

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
    public Node call_op(Node recv, int op, int narg, Node arg1) {
        return call_op(recv, getOperatorId(op), narg, arg1);
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
    public Node call_op(Node recv, String id, int narg, Node arg1) {
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
    public Node match_gen(Node node1, Node node2) {
        local_cnt("~");

        switch (node1.getType()) {
            case Constants.NODE_DREGX :
            case Constants.NODE_DREGX_ONCE :
                return nf.newMatch2(node1, node2);
            case Constants.NODE_LIT :
                if (node1.getLiteral() instanceof RubyRegexp) {
                    return nf.newMatch2(node1, node2);
                }
        }

        switch (node2.getType()) {
            case Constants.NODE_DREGX :
            case Constants.NODE_DREGX_ONCE :
                return nf.newMatch3(node2, node1);
            case Constants.NODE_LIT :
                if (node2.getLiteral() instanceof RubyRegexp) {
                    return nf.newMatch3(node2, node1);
                }
        }

        return nf.newCall(node1, "=~", nf.newList(node2));
    }

    /**
     *  Description of the Method
     *
     *@param  id   Description of Parameter
     *@param  val  Description of Parameter
     *@return      Description of the Returned Value
     */
    public Node assignable(String id, Node val) {
        value_expr(val);
        if (id.startsWith("~~")) {
            if (id.endsWith("SELF")) {
                yyerror("Can't change the value of self");
            } else if (id.endsWith("NIL")) {
                yyerror("Can't assign to nil");
            } else if (id.endsWith("TRUE")) {
                yyerror("Can't assign to true");
            } else if (id.endsWith("FALSE")) {
                yyerror("Can't assign to false");
            } else if (id.endsWith("__FILE__")) {
                yyerror("Can't assign to __FILE__");
            } else if (id.endsWith("__LINE__")) {
                yyerror("Can't assign to __LINE__");
            }
        } else {
            if (IdUtil.isLocal(id)) {
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
            } else if (IdUtil.isGlobal(id)) {
                return nf.newGAsgn(id, val);
            } else if (IdUtil.isInstanceVariable(id)) {
                return nf.newIAsgn(id, val);
            } else if (IdUtil.isConstant(id)) {
                if (isInDef() || isInSingle()) {
                    yyerror("dynamic constant assignment");
                }
                return nf.newCDecl(id, val);
            } else if (IdUtil.isClassVariable(id)) {
                if (isInSingle()) {
                    return nf.newCVAsgn(id, val);
                }
                return nf.newCVDecl(id, val);
            } else {
                rb_bug("bad id for variable");
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
    public Node aryset(Node recv, Node idx) {
        value_expr(recv);
        return nf.newCall(recv, "[]=", idx);
    }

    /**
     *  Description of the Method
     *
     *@param  recv  Description of Parameter
     *@param  id    Description of Parameter
     *@return       Description of the Returned Value
     */
    public Node attrset(Node recv, String id) {
        value_expr(recv);

        return nf.newCall(recv, id + "=", null);
    }

    /**
     *  Description of the Method
     *
     *@param  node  Description of Parameter
     */
    public void rb_backref_error(Node node) {
        switch (node.getType()) {
            case Constants.NODE_NTH_REF :
                rb_compile_error("Can't set variable $" + ((NthRefNode) node).getNth());
                break;
            case Constants.NODE_BACK_REF :
                rb_compile_error("Can't set variable $" + ((BackRefNode) node).getNth());
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
    public Node arg_concat(Node node1, Node node2) {
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
    private Node arg_add(Node node1, Node node2) {
        if (node1 == null) {
            return nf.newList(node2);
        }

        if (node1 instanceof ArrayNode) {
            return list_append((ArrayNode) node1, node2);
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
    public Node node_assign(Node lhs, Node rhs) {
        if (lhs == null) {
            return null;
        }

        value_expr(rhs);
        switch (lhs.getType()) {
            case Constants.NODE_GASGN :
            case Constants.NODE_IASGN :
            case Constants.NODE_LASGN :
            case Constants.NODE_DASGN :
            case Constants.NODE_DASGN_CURR :
            case Constants.NODE_MASGN :
            case Constants.NODE_CDECL :
            case Constants.NODE_CVDECL :
            case Constants.NODE_CVASGN :
                lhs.setValueNode(rhs);
                break;
            case Constants.NODE_CALL :
                lhs.setArgsNode(arg_add(lhs.getArgsNode(), rhs));
                break;
            default :
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
    public boolean value_expr(Node node) {
        if (node == null) {
            return true;
        }

        switch (node.getType()) {
            case Constants.NODE_RETURN :
            case Constants.NODE_BREAK :
            case Constants.NODE_NEXT :
            case Constants.NODE_REDO :
            case Constants.NODE_RETRY :
            case Constants.NODE_WHILE :
            case Constants.NODE_UNTIL :
            case Constants.NODE_CLASS :
            case Constants.NODE_MODULE :
            case Constants.NODE_DEFN :
            case Constants.NODE_DEFS :
                yyerror("void value expression");
                return false;
            case Constants.NODE_BLOCK :
                while (node.getNextNode() != null) {
                    node = node.getNextNode();
                }
                return value_expr(node.getHeadNode());
            case Constants.NODE_BEGIN :
                return value_expr(node.getBodyNode());
            case Constants.NODE_IF :
                return value_expr(node.getBodyNode()) && value_expr(node.getElseNode());
            case Constants.NODE_NEWLINE :
                return value_expr(node.getNextNode());
            default :
                return true;
        }
    }

    /**
     *  Description of the Method
     *
     *@param  node  Description of Parameter
     */
    public void void_expr(Node node) {
        String useless = null;

        if (!ruby.isVerbose() || node == null) {
            return;
        }

        //        while(true) {
        while (node instanceof NewlineNode) {
            node = node.getNextNode();
        }
        switch (node.getType()) {
            /*                case Constants.NODE_NEWLINE:
                                node = node.getNext();
                                continue;*/
            case Constants.NODE_CALL :
                if (node.getMId().equals("+") ||
                    node.getMId().equals("-") ||
                    node.getMId().equals("*") ||
                    node.getMId().equals("/") ||
                    node.getMId().equals("%") ||
                    node.getMId().equals("**") ||
                    node.getMId().equals("+@") ||
                    node.getMId().equals("-@") ||
                    node.getMId().equals("|") ||
                    node.getMId().equals("^") ||
                    node.getMId().equals("&") ||
                    node.getMId().equals("<=>") ||
                    node.getMId().equals(">") ||
                    node.getMId().equals(">=") ||
                    node.getMId().equals("<") ||
                    node.getMId().equals("<=") ||
                    node.getMId().equals("==") ||
                    node.getMId().equals("!=")) {
                        useless = node.getMId();
                        break;
                }
                break;
            case Constants.NODE_LVAR :
            case Constants.NODE_DVAR :
            case Constants.NODE_GVAR :
            case Constants.NODE_IVAR :
            case Constants.NODE_CVAR :
            case Constants.NODE_NTH_REF :
            case Constants.NODE_BACK_REF :
                useless = "a variable";
                break;
            case Constants.NODE_CONST :
            case Constants.NODE_CREF :
                useless = "a constant";
                break;
            case Constants.NODE_LIT :
            case Constants.NODE_STR :
            case Constants.NODE_DSTR :
            case Constants.NODE_DREGX :
            case Constants.NODE_DREGX_ONCE :
                useless = "a literal";
                break;
            case Constants.NODE_COLON2 :
            case Constants.NODE_COLON3 :
                useless = "::";
                break;
            case Constants.NODE_DOT2 :
                useless = "..";
                break;
            case Constants.NODE_DOT3 :
                useless = "...";
                break;
            case Constants.NODE_SELF :
                useless = "self";
                break;
            case Constants.NODE_NIL :
                useless = "nil";
                break;
            case Constants.NODE_TRUE :
                useless = "true";
                break;
            case Constants.NODE_FALSE :
                useless = "false";
                break;
            case Constants.NODE_DEFINED :
                useless = "defined?";
                break;
        }
        //            break;
        //        }

        if (useless != null) {
            int line = ruby.getSourceLine();

            ruby.setSourceLine(node.getLine());
            rb_warn("useless use of " + useless + "in void context");
            ruby.setSourceLine(line);
        }
    }

    /**
     *  Description of the Method
     *
     *@param  node  Description of Parameter
     */
    public void void_stmts(Node node) {
        if (!ruby.isVerbose()) {
            return;
        }
        if (node == null) {
            return;
        }
        if (node.getType() != Constants.NODE_BLOCK) {
            return;
        }

        while (true) {
            if (node.getNextNode() == null) {
                return;
            }
            void_expr(node.getHeadNode());
            node = node.getNextNode();
        }
    }

    /**
     *  Description of the Method
     *
     *@param  node  Description of Parameter
     *@return       Description of the Returned Value
     */
    private boolean assign_in_cond(Node node) {
        switch (node.getType()) {
            case Constants.NODE_MASGN :
                yyerror("multiple assignment in conditional");
                return true;
            case Constants.NODE_LASGN :
            case Constants.NODE_DASGN :
            case Constants.NODE_GASGN :
            case Constants.NODE_IASGN :
                break;
            case Constants.NODE_NEWLINE :
            default :
                return false;
        }

        switch (node.getValueNode().getType()) {
            case Constants.NODE_LIT :
            case Constants.NODE_STR :
            case Constants.NODE_NIL :
            case Constants.NODE_TRUE :
            case Constants.NODE_FALSE :
                /*
                 *  reports always
                 */
                rb_warn("found = in conditional, should be ==");
                return true;
            case Constants.NODE_DSTR :
            case Constants.NODE_XSTR :
            case Constants.NODE_DXSTR :
            case Constants.NODE_EVSTR :
            case Constants.NODE_DREGX :
            default :
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
    private Node range_op(Node node, int logop) {
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
        type = node.getType();
        if (type == Constants.NODE_NEWLINE) {
            node = node.getNextNode();
        }
        if (type == Constants.NODE_LIT && node.getLiteral() instanceof RubyFixnum) {
            return call_op(node, Token.tEQ, 1, nf.newGVar("$."));
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
    private Node cond0(Node node, int logop) {
        int type = node.getType(); // enum node_type

        assign_in_cond(node);
        switch (type) {
            case Constants.NODE_DSTR :
            case Constants.NODE_STR :
                if (logop != 0) {
                    break;
                }
                rb_warn("string literal in condition");
                break;
            case Constants.NODE_DREGX :
            case Constants.NODE_DREGX_ONCE :
                warning_unless_e_option("regex literal in condition");
                local_cnt("_");
                local_cnt("~");
                return nf.newMatch2(node, nf.newGVar("$_"));
            case Constants.NODE_DOT2 :
            case Constants.NODE_DOT3 :
                node.setBeginNode(range_op(node.getBeginNode(), logop));
                node.setEndNode(range_op(node.getEndNode(), logop));
                if (type == Constants.NODE_DOT2) {
                    new RuntimeException("[BUG] want to replace DOT2 with FLIP2").printStackTrace();
                    // node.setReplacedNode(new Flip2Node(node.getBeginNode(), node.getEndNode(), local_append(newId(0))));
                } else if (type == Constants.NODE_DOT3) {
                    new RuntimeException("[BUG] want to replace DOT3 with FLIP3").printStackTrace();
                    // node.setReplacedNode(new Flip3Node(node.getBeginNode(), node.getEndNode(), local_append(newId(0))));
                }
                // node.nd_cnt(local_append(newId(0)));
                warning_unless_e_option("range literal in condition");
                break;
            case Constants.NODE_LIT :
                if (node.getLiteral() instanceof RubyRegexp) {
                    warning_unless_e_option("regex literal in condition");
                    // +++
                    // node.nd_set_type(Constants.NODE_MATCH);
                    // ---
                    local_cnt("_");
                    local_cnt("~");
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
    private Node cond1(Node node, int logop) {
        if (node == null) {
            return null;
        }
        if (node.getType() == Constants.NODE_NEWLINE) {
            node.setNextNode(cond0(node.getNextNode(), logop));
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
    public Node cond(Node node) {
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
    public Node logop(int type, Node left, Node right) {
        value_expr(left);

        switch (type) {
            case Constants.NODE_AND :
                return new AndNode(cond1(left, 1), cond1(right, 1));
            case Constants.NODE_OR :
                return new OrNode(cond1(left, 1), cond1(right, 1));
            default :
                throw new RuntimeException("[BUG] ParserHelper#logop: Nodetype=" + type);
        }
        // return nf.newDefaultNode(type, cond1(left, 1), cond1(right, 1), null);
    }

    /**
     *  Description of the Method
     *
     *@param  node  Description of Parameter
     *@return       Description of the Returned Value
     */
    public Node ret_args(Node node) {
        if (node != null) {
            if (node.getType() == Constants.NODE_BLOCK_PASS) {
                rb_compile_error("block argument should not be given");
            }
        }
        return node;
    }

    /*
     *  private Node arg_prepend(Node node1, Node node2) {
     *      switch (nodetype(node2)) {
     *          case Constants.NODE_ARRAY:
     *              return list_concat(NEW_LIST(node1), node2);
     *          case Constants.NODE_RESTARGS:
     *              return arg_concat(node1, node2.getHead());
     *          case Constants.NODE_BLOCK_PASS:
     *              node2.nd_body(arg_prepend(node1, node2.nd_body()));
     *              return node2;
     *          default:
     *              rb_bug("unknown nodetype(%d) for arg_prepend");
     *      }
     *      return null; // not reached
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
    public Node new_call(Node r, String m, Node args) {
        if (args != null && args instanceof BlockPassNode) {
            args.setIterNode(nf.newCall(r, m, args.getHeadNode()));
            return args;
        }
        return nf.newCall(r, m, args);
    }

    /**
     *  Description of the Method
     *
     *@param  m  Description of Parameter
     *@param  a  Description of Parameter
     *@return    Description of the Returned Value
     */
    public Node new_fcall(String mid, Node args) {
        if (args != null && args instanceof BlockPassNode) {
            args.setIterNode(nf.newFCall(mid, args.getHeadNode()));
            return args;
        }
        return nf.newFCall(mid, args);
    }

    /**
     *  Description of the Method
     *
     *@param  a  Description of Parameter
     *@return    Description of the Returned Value
     */
    public Node new_super(Node args) {
        if (args != null && args instanceof BlockPassNode) {
            args.setIterNode(nf.newSuper(args.getHeadNode()));
            return args;
        }
        return nf.newSuper(args);
    }

    /**
     *  Description of the Method
     */
    public void local_push() {
        LocalVars local = new LocalVars();
        
        local.prev = lvtbl;
        local.nofree = false;
        local.cnt = 0;
        local.tbl = null;
        local.dlev = 0;
        
        lvtbl = local;
    }

    /**
     *  Description of the Method
     */
    public void local_pop() {
        LocalVars local = lvtbl.prev;

        /*if (lvtbl.tbl != null) {
            if (lvtbl.nofree) {
                lvtbl.tbl.setCount(lvtbl.cnt);
            }
        }*/
        lvtbl = local;
    }

    /**
     *  Description of the Method
     *
     *@return    Description of the Returned Value
     */
    public ExtendedList local_tbl() {
        lvtbl.nofree = true;
        return lvtbl.tbl;
    }

    /**
     *  Description of the Method
     *
     *@param  id  Description of Parameter
     *@return     Description of the Returned Value
     */
    private int local_append(String id) {
        if (lvtbl.tbl == null) {
            lvtbl.tbl = new ExtendedList();
            // lvtbl.tbl.setCount(0);
            lvtbl.tbl.add("_");
            lvtbl.tbl.add("~");
            lvtbl.cnt = 2;
            if (id.equals("_")) {
                return 0;
            } else if (id.equals("~")) {
                return 1;
            }
        } else {
            /*RubyId[] ntbl = new RubyId[lvtbl.cnt + 2];
            System.arraycopy(lvtbl.tbl, 0, ntbl, 0, lvtbl.tbl.length);
            lvtbl.tbl = ntbl;*/
        }

        lvtbl.tbl.add(id);
        return lvtbl.cnt++;
    }

    /**
     *  Description of the Method
     *
     *@param  id  Description of Parameter
     *@return     Description of the Returned Value
     */
    public int local_cnt(String id) {
        if (id == null) {
            return lvtbl.cnt;
        } else if (lvtbl.tbl == null) {
            return local_append(id);
        }

        for (int i = 0; i < lvtbl.tbl.size(); i++) {
            if (id.equals(lvtbl.tbl.get(i))) {
                return i;
            }
        }

        return local_append(id);
    }

    public boolean local_id(String id) {
        // int i;
        // int max;

        if (lvtbl == null || lvtbl.tbl == null) {
            return false;
        }
        /*
        for (i = 3, max = lvtbl.cnt + 1; i < max; i++) {
            if (lvtbl.tbl.get(i).equals(id)) {
                return true;
            }
        }
        */
        for (int i = 0; i < lvtbl.tbl.size(); i++) {
            if (id.equals(lvtbl.tbl.get(i))) {
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
        lvtbl.cnt = ruby.getRubyScope().getLocalNames() != null ? ruby.getRubyScope().getLocalNames().size() : 0;
        if (lvtbl.cnt > 0) {
            lvtbl.tbl = new ExtendedList(lvtbl.cnt + 2, null);
            ruby.getRubyScope().setLocalNames(lvtbl.tbl);
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
            i = ruby.getRubyScope().getLocalNames() != null ? ruby.getRubyScope().getLocalNames().size() : 0;

            if (i < len) {
                if (i == 0 || (ruby.getRubyScope().getFlags() & RubyScope.SCOPE_MALLOC) == 0) {
                    ExtendedList values = new ExtendedList(len + 1, ruby.getNil());
                    //int vi = 0;
                    if (ruby.getRubyScope().getLocalValues() != null) {
                        // vars.set(0, ruby.getRubyScope().getLocalVars(-1));
                        // vars.inc();
                        values.copy(ruby.getRubyScope().getLocalValues(), i);
                    } /*else {
                        vars.set(0, null);
                        vars.inc(1);
                    }*/
                    ruby.getRubyScope().setLocalValues(values);
                    ruby.getRubyScope().setFlags(ruby.getRubyScope().getFlags() | RubyScope.SCOPE_MALLOC);
                } else {
                    ExtendedList values = new ExtendedList(len/* + 1*/, ruby.getNil());
                    values.copy(ruby.getRubyScope().getLocalValues(), values.size());
                    ruby.getRubyScope().setLocalValues(values);
                }

                // if (ruby.getRubyScope().getLocalNames() != null && ruby.getRubyScope().getLocalVars(-1) == null) {
                //    ruby.getRubyScope().setLocalNames(null);
                // }

                // ruby.getRubyScope().setLocalVars(-1, null);
                ruby.getRubyScope().setLocalNames(local_tbl());
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
    private void special_local_set(String c, RubyObject val) {
        top_local_init();
        int cnt = local_cnt(c);
        top_local_setup();
        ruby.getRubyScope().setValue(cnt, val);
    }

    /** Getter for property evalTree.
     * @return Value of property evalTree.
     */
    public Node getEvalTree() {
        return evalTree;
    }

    /** Setter for property evalTree.
     * @param evalTree New value of property evalTree.
     */
    public void setEvalTree(Node evalTree) {
        this.evalTree = evalTree;
    }

    /** Getter for property evalTreeBegin.
     * @return Value of property evalTreeBegin.
     */
    public Node getEvalTreeBegin() {
        return evalTreeBegin;
    }

    /** Setter for property evalTreeBegin.
     * @param evalTreeBegin New value of property evalTreeBegin.
     */
    public void setEvalTreeBegin(Node evalTreeBegin) {
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
    public String getCurMid() {
        return curMid;
    }

    /** Setter for property curMid.
     * @param curMid New value of property curMid.
     */
    public void setCurMid(String curMid) {
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
    private class LocalVars {
        ExtendedList tbl;
        boolean nofree;
        int cnt;
        int dlev;
        LocalVars prev;
    }
}
