/*
 * NodeFactory.java - No description
 * Created on 04. Oktober 2001, 23:35
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

package org.jruby.interpreter.nodes;

import org.jruby.*;
import org.jruby.original.*;
import org.jruby.parser.*;
import org.jruby.util.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class NodeFactory {
    private Ruby ruby;
    private ParserHelper ph;

    public NodeFactory(Ruby ruby) {
        this.ruby = ruby;
        
        ph = ruby.getParserHelper();
    }

    public NODE newDefaultNode(int nodeType, Object u1, Object u2, Object u3) {
        return new NODE(nodeType, u1, u2, u3);
    }
    
    public NODE newDefaultNode(int nodeType, Object u1, Object u2, int u3) {
        return newDefaultNode(nodeType, u1, u2, new Integer(u3));
    }
    
    public NODE newDefaultNode(int nodeType, Object u1, Object u2, boolean u3) {
        return newDefaultNode(nodeType, u1, u2, new Boolean(u3));
    }
    
    public NODE newMethod(Object n, Object x) {
        return newDefaultNode(NODE.NODE_METHOD, x, n, null);
    }
    
    public NODE newFBody(Object n, Object i, Object o) {
        return newDefaultNode(NODE.NODE_FBODY, n, i, o);
    }
        
    public NODE newDefn(Object i, NODE a, NODE d, int p) {
        return newDefaultNode(NODE.NODE_DEFN, new Integer(p), i, newRFunc(a, d));
    }
    
    public NODE newDefs(Object r, Object i, NODE a, NODE d) {
        return newDefaultNode(NODE.NODE_DEFS, r, i, newRFunc(a, d));
    }
    
    public NODE newCFunc(Object f, Object c) {
        return newDefaultNode(NODE.NODE_CFUNC, f, c, null);
    }
    
    public NODE newIFunc(Object f, Object c) {
        return newDefaultNode(NODE.NODE_IFUNC, f, c, null);
    }
       
    public NODE newRFunc(NODE b1, NODE b2) {
        return newScope(ph.block_append(b1, b2));
    }
       
    public NODE newScope(Object b) {
        return newDefaultNode(NODE.NODE_SCOPE, ph.local_tbl(), null, b);
    }
       
    public NODE newBlock(Object a) {
        return newDefaultNode(NODE.NODE_BLOCK, a, null, null);
    }
       
    public NODE newIf(Object c, Object t, Object e) {
        return newDefaultNode(NODE.NODE_IF, c, t, e);
    }
       
    public NODE newUnless(Object c, Object t, Object e) {
        return newIf(c, e, t);
    }
    
    public NODE newCase(Object h, Object b) {
        return newDefaultNode(NODE.NODE_CASE, h, b, null);
    }
       
    public NODE newWhen(Object c, Object t, Object e) {
        return newDefaultNode(NODE.NODE_WHEN, c, t, e);
    }
       
    public NODE newOptN(Object b) {
        return newDefaultNode(NODE.NODE_OPT_N, null, b, null);
    }
       
    public NODE newWhile(Object c, Object b, int n) {
        return newDefaultNode(NODE.NODE_WHILE, c, b, n);
    }
       
    public NODE newUntil(Object c, Object b, int n) {
        return newDefaultNode(NODE.NODE_UNTIL, c, b, n);
    }
       
    public NODE newFor(Object v, Object i, Object b) {
        return newDefaultNode(NODE.NODE_FOR, v, b, i);
    }
       
    public NODE newIter(Object v, Object i, Object b) {
        return newDefaultNode(NODE.NODE_ITER, v, b, i);
    }
       
    public NODE newBreak() {
        return newDefaultNode(NODE.NODE_BREAK, null, null, null);
    }
       
    public NODE newNext() {
        return newDefaultNode(NODE.NODE_NEXT, null, null, null);
    }
       
    public NODE newRedo() {
        return newDefaultNode(NODE.NODE_REDO, null, null, null);
    }
       
    public NODE newRetry() {
        return newDefaultNode(NODE.NODE_RETRY, null, null, null);
    }
       
    public NODE newBegin(Object b) {
        return newDefaultNode(NODE.NODE_BEGIN, null, b, null);
    }
       
    public NODE newRescue(Object b, Object res, Object e) {
        return newDefaultNode(NODE.NODE_RESCUE, b, res, e);
    }
       
    public NODE newResBody(Object a, Object ex, Object n) {
        return newDefaultNode(NODE.NODE_RESBODY, n, ex, a);
    }
       
    public NODE newEnsure(Object b, Object en) {
        return newDefaultNode(NODE.NODE_ENSURE, b, null, en);
    }
       
    public NODE newReturn(Object s) {
        return newDefaultNode(NODE.NODE_RETURN, s, null, null);
    }
       
    public NODE newYield(Object a) {
        return newDefaultNode(NODE.NODE_YIELD, a, null, null);
    }
       
    public NODE newList(Object a) {
        return newArray(a);
    }
       
    public NODE newArray(Object a) {
        return newDefaultNode(NODE.NODE_ARRAY, a, new Integer(1), null);
    }
       
    public NODE newZArray() {
        return newDefaultNode(NODE.NODE_ZARRAY, null, null, null);
    }
       
    public NODE newHash(Object a)  {
        return newDefaultNode(NODE.NODE_HASH, a, null, null);
    }
       
    public NODE newNot(Object a)   {
        return newDefaultNode(NODE.NODE_NOT, null, a, null);
    }
       
    public NODE newMAsgn(Object l, Object r)   {
        return newDefaultNode(NODE.NODE_MASGN, l, null, r);
    }
       
    public NODE newGAsgn(RubyId v, Object val) {
        return newDefaultNode(NODE.NODE_GASGN, v, val, RubyGlobalEntry.getGlobalEntry(v));
    }
       
    public NODE newLAsgn(RubyId v, Object val) {
        return newDefaultNode(NODE.NODE_LASGN, v, val, ph.local_cnt(v));
    }
       
    public NODE newDAsgn(Object v, Object val) {
        return newDefaultNode(NODE.NODE_DASGN, v, val, null);
    }
       
    public NODE newDAsgnCurr(Object v, Object val) {
        return newDefaultNode(NODE.NODE_DASGN_CURR, v, val, null);
    }
       
    public NODE newIAsgn(Object v, Object val) {
        return newDefaultNode(NODE.NODE_IASGN, v, val, null);
    }
       
    public NODE newCDecl(Object v, Object val) {
        return newDefaultNode(NODE.NODE_CDECL, v, val, null);
    }
       
    public NODE newCVAsgn(Object v, Object val) {
        return newDefaultNode(NODE.NODE_CVASGN, v, val, null);
    }
       
    public NODE newCVDecl(Object v, Object val) {
        return newDefaultNode(NODE.NODE_CVDECL, v, val, null);
    }
       
    public NODE newOpAsgn1(Object p, Object id, Object a) {
        return newDefaultNode(NODE.NODE_OP_ASGN1, p, id, a);
    }
       
    public NODE newOpAsgn2(Object r, RubyId id, Object o, Object val) {
        return newDefaultNode(NODE.NODE_OP_ASGN2, r, val, newOpAsgn22(id, o));
    }
       
    public NODE newOpAsgn22(RubyId id, Object o) {
        return newDefaultNode(NODE.NODE_OP_ASGN2, id, o, new Boolean(id.is_attrset_id()));
    }
       
    public NODE newOpAsgnOr(Object i, Object val) {
        return newDefaultNode(NODE.NODE_OP_ASGN_OR, i, val, null);
    }
       
    public NODE newOpAsgnAnd(Object i, Object val) {
        return newDefaultNode(NODE.NODE_OP_ASGN_AND, i, val, null);
    }
       
    public NODE newGVar(RubyId id) {
        return newDefaultNode(NODE.NODE_GVAR, id, null, RubyGlobalEntry.getGlobalEntry(id));
    }
       
    public NODE newLVar(RubyId v) {
        return newDefaultNode(NODE.NODE_LVAR, v, null, ph.local_cnt(v));
    }
       
    public NODE newDVar(Object v) {
        return newDefaultNode(NODE.NODE_DVAR, v, null, null);
    }
       
    public NODE newIVar(Object v) {
        return newDefaultNode(NODE.NODE_IVAR, v, null, null);
    }
       
    public NODE newConst(Object v) {
        return newDefaultNode(NODE.NODE_CONST, v, null, null);
    }
       
    public NODE newCVar(Object v) {
        return newDefaultNode(NODE.NODE_CVAR, v, null, null);
    }
       
    public NODE newCVar2(Object v) {
        return newDefaultNode(NODE.NODE_CVAR2, v, null, null);
    }
       
    public NODE newNthRef(int n)  {
        return newDefaultNode(NODE.NODE_NTH_REF, null, new Integer(n), ph.local_cnt('~'));
    }
       
    public NODE newBackRef(int n) {
        return newDefaultNode(NODE.NODE_BACK_REF, null, new Integer(n), ph.local_cnt('~'));
    }
       
    public NODE newMatch(Object c) {
        return newDefaultNode(NODE.NODE_MATCH, c, null, null);
    }
       
    public NODE newMatch2(Object n1, Object n2) {
        return newDefaultNode(NODE.NODE_MATCH2, n1, n2, null);
    }
       
    public NODE newMatch3(Object r, Object n2) {
        return newDefaultNode(NODE.NODE_MATCH3, r, n2, null);
    }
       
    public NODE newLit(Object l) {
        return newDefaultNode(NODE.NODE_LIT, l, null, null);
    }
       
    public NODE newStr(Object s) {
        return newDefaultNode(NODE.NODE_STR, s, null, null);
    }
       
    public NODE newDStr(Object s) {
        return newDefaultNode(NODE.NODE_DSTR, s, null, null);
    }
       
    public NODE newXStr(Object s) {
        return newDefaultNode(NODE.NODE_XSTR, s, null, null);
    }
       
    public NODE newDXStr(Object s) {
        return newDefaultNode(NODE.NODE_DXSTR, s, null, null);
    }
       
    public NODE newEVStr(String s, int len) {
        return newDefaultNode(NODE.NODE_EVSTR, RubyString.m_newString(ruby, s, len), null, null);
    }
       
    public NODE newCall(Object r, Object m, Object a) {
        return newDefaultNode(NODE.NODE_CALL, r, m, a);
    }
    
    public NODE newFCall(Object m, Object a) {
        return newDefaultNode(NODE.NODE_FCALL, null, m, a);
    }
    
    public NODE newVCall(Object m) {
        return newDefaultNode(NODE.NODE_VCALL, null, m, null);
    }
    
    public NODE newSuper(Object a) {
        return newDefaultNode(NODE.NODE_SUPER, null, null, a);
    }
    
    public NODE newZSuper() {
        return newDefaultNode(NODE.NODE_ZSUPER, null, null, null);
    }
    
    public NODE newArgs(Object f, Object o, Object r) {
        return newDefaultNode(NODE.NODE_ARGS, o, r, f);
    }
    
    public NODE newArgsCat(Object a, Object b) {
        return newDefaultNode(NODE.NODE_ARGSCAT, a, b, null);
    }
    
    public NODE newArgsPush(Object a, Object b) {
        return newDefaultNode(NODE.NODE_ARGSPUSH, a, b, null);
    }
    
    public NODE newRestArgs(Object a) {
        return newDefaultNode(NODE.NODE_RESTARGS, a, null, null);
    }
    
    public NODE newBlockArg(RubyId v) {
        return newDefaultNode(NODE.NODE_BLOCK_ARG, v, null, ph.local_cnt(v));
    }
    
    public NODE newBlockPass(Object b) {
        return newDefaultNode(NODE.NODE_BLOCK_PASS, null, b, null);
    }
    
    public NODE newAlias(Object n, Object o) {
        return newDefaultNode(NODE.NODE_ALIAS, o, n, null);
    }
    
    public NODE newVAlias(Object n, Object o) {
        return newDefaultNode(NODE.NODE_VALIAS, o, n, null);
    }
    
    public NODE newUndef(Object i) {
        return newDefaultNode(NODE.NODE_UNDEF, null, i, null);
    }
    
    public NODE newClass(Object n, Object b, Object s) {
        return newDefaultNode(NODE.NODE_CLASS, n, newScope(b), s);
    }
    
    public NODE newSClass(Object r, Object b) {
        return newDefaultNode(NODE.NODE_SCLASS, r, newScope(b), null);
    }
    
    public NODE newModule(Object n, Object b) {
        return newDefaultNode(NODE.NODE_MODULE, n, newScope(b), null);
    }
    
    public NODE newColon2(Object c, Object i) {
        return newDefaultNode(NODE.NODE_COLON2, c, i, null);
    }
    
    public NODE newColon3(Object i) {
        return newDefaultNode(NODE.NODE_COLON3, null, i, null);
    }
    
    public NODE newCRef(Object c) {
        return newDefaultNode(NODE.NODE_CREF, null, null, c);
    }
    
    public NODE newDot2(Object b, Object e) {
        return newDefaultNode(NODE.NODE_DOT2, b, e, null);
    }
    
    public NODE newDot3(Object b, Object e) {
        return newDefaultNode(NODE.NODE_DOT3, b, e, null);
    }
    
    public NODE newAttrset(Object a) {
        return newDefaultNode(NODE.NODE_ATTRSET, a, null, null);
    }
    
    public NODE newSelf() {
        return newDefaultNode(NODE.NODE_SELF, null, null, null);
    }
    
    public NODE newNil() {
        return newDefaultNode(NODE.NODE_NIL, null, null, null);
    }
    
    public NODE newTrue() {
        return newDefaultNode(NODE.NODE_TRUE, null, null, null);
    }
    
    public NODE newFalse() {
        return newDefaultNode(NODE.NODE_FALSE, null, null, null);
    }
    
    public NODE newDefined(Object e) {
        return newDefaultNode(NODE.NODE_DEFINED, e, null, null);
    }
    
    public NODE newNewline(Object n) {
        return newDefaultNode(NODE.NODE_NEWLINE, null, null, n);
    }
    
    public NODE newPreExe(Object b) {
        return newScope(b);
    }
    
    public NODE newPostExe() {
        return newDefaultNode(NODE.NODE_POSTEXE, null, null, null);
    }
    
    public NODE newDMethod(Object b) {
        return newDefaultNode(NODE.NODE_DMETHOD, null, null, b);
    }
    
    public NODE newBMethod(Object b) {
        return newDefaultNode(NODE.NODE_BMETHOD, null, null, b);
    }
}
