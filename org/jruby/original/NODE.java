/*
 * NODE.java - No description
 * Created on 23. Juli 2001, 19:25
 * 
 * Copyright (C) 2001 Stefan Matthias Aust, Jan Arne Petersen
 * Stefan Matthias Aust <sma@3plus4.de>
 * Jan Arne Petersen <japetersen@web.de>
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
package org.jruby.original;

import java.util.*;

import org.jruby.*;
import org.jruby.core.*;

public class NODE implements node_type, VALUE, Scope {

    private NODE() {}

    public static NODE MINUS_ONE = new NODE();
    public static NODE ONE = new NODE();
    public static NODE undefined = new NODE(); //XXX

    private Object u1;
    private Object u2;
    private Object u3;

    public NODE nd_head() { return (NODE)u1; }
    public void nd_head(NODE n) { u1 = n; }

    public int nd_alen() { return u2 == null ? 0 : ((Integer)u2).intValue(); }
    public void nd_alen(int i) { u2 = new Integer(i); }

    public NODE nd_next() { return (NODE)u3; }
    public void nd_next(NODE n) { u3 = n; }

    public NODE nd_cond() { return (NODE)u1; }
    public NODE nd_body() { return (NODE)u2; }
    public NODE nd_else() { return (NODE)u3; }

    public VALUE nd_orig() { return (VALUE)u3; }

    //NODE nd_resq() { return (NODE)u2; }
    public NODE nd_ensr() { return (NODE)u3; }

    public NODE nd_1st() { return (NODE)u1; }
    public NODE nd_2nd() { return (NODE)u2; }

    public NODE nd_stts() { return (NODE)u1; }

    public global_entry nd_entry() { return (global_entry)u3; }
    public ID nd_vid() { return (ID)u1; }
    public ID nd_cflag() { return (ID)u2; }
    public void nd_cflag(ID id) { u2 = id; }
    //VALUE nd_cval() { return (VALUE)u3; }

    public int nd_cnt() { return u3 == null ? 0 : ((Integer)u3).intValue(); }
    public void nd_cnt(int i) { u3 = new Integer(i); }
    public ID[] nd_tbl() { return (ID[])u1; }
    public void nd_tbl(ID[] idTable) { u1 = idTable; }

    public NODE nd_var() { return (NODE)u1; }
    //NODE nd_ibdy() { return (NODE)u2; }
    public NODE nd_iter() { return (NODE)u3; }
    public void nd_iter(NODE n) { u3 = n; }

    public NODE nd_value() { return (NODE)u2; }
    public void nd_value(NODE n) { u2 = n; }
    public ID nd_aid() { return (ID)u3; }
    public void nd_aid(ID id) { u3 = id; }

    public VALUE nd_lit() { return (VALUE)u1; }
    public void nd_lit(VALUE v) { u1 = v; }

    //NODE nd_frml() { return (NODE)u1; }
    public int nd_rest() { return u2 == null ? 0 : ((Integer)u2).intValue(); }
    public NODE nd_opt() { return (NODE)u1; }

    public NODE nd_recv() { return (NODE)u1; }
    public ID nd_mid() { return (ID)u2; }
    public void nd_mid(ID id) { u2 = id; }
    public NODE nd_args() { return (NODE)u3; }
    public void nd_args(NODE n) { u3 = n; }

    public int nd_noex() { return u1 == null ? 0 : ((Integer)u1).intValue(); }
    public void nd_noex(int i) { u1 = new Integer(i); }
    public NODE nd_defn() { return (NODE)u3; }

    public ID nd_old() { return (ID)u1; }
    public ID nd_new() { return (ID)u2; }

    public Object nd_cfnc() { return u1; }
    //int nd_argc() { return u2 == null ? 0 : ((Integer)u2).intValue(); }

    public ID nd_cname() { return (ID)u1; }
    public NODE nd_super() { return (NODE)u3; }

    //ID nd_modl() { return (ID)u1; }
    public VALUE nd_clss() { return (VALUE)u1; }

    public NODE nd_beg() { return (NODE)u1; }
    public void nd_beg(NODE n) { u1 = n; }
    public NODE nd_end() { return (NODE)u2; }
    public void nd_end(NODE n) { u2 = n; }
    public long nd_state() { return u3 == null ? 0L : ((Long)u3).longValue(); }
    public void nd_state(long l) { u3 = new Long(l);  }
    public VALUE nd_rval() { return (VALUE)u2; }
    public void nd_rval(VALUE val) { u2 = val; }

    public int nd_nth() { return u2 == null ? 0 : ((Integer)u2).intValue(); }
    public void nd_nth(int i) { u2 = new Integer(i); }

    //ID nd_tag() { return (ID)u1; }
    public VALUE nd_tval() { return (VALUE)u2; }

    private short type;
    public int nd_type() { return type; }
    public void nd_set_type(int type) { this.type = (short)type; }

    private short line;
    public int nd_line() { return line; }
    public void nd_set_line(int line) { this.line = (short)line; }

    public String nd_file;

    public NODE(int type, Object u1, Object u2, Object u3) {
        nd_set_type(type);
        this.u1 = u1;
        this.u2 = u2;
        this.u3 = u3;
    }
    
    public static NODE newNode(int type, Object u1, Object u2, Object u3) {
        return new NODE(type, u1, u2, u3);
    }
    
    /** copy_node_scope
     *
     */
    public NODE copyNodeScope(VALUE rval) {
        NODE copy = new NODE(NODE_SCOPE, null, rval, nd_next());
        
        if (nd_tbl() != null) {
            // ???
            // copy->nd_tbl = ALLOC_N( ID, node->nd_tbl[ 0 ] + 1 );
            // MEMCPY( copy->nd_tbl, node->nd_tbl, ID, node->nd_tbl[ 0 ] + 1 );
            
            ID[] idTable = new ID[nd_tbl()[0].intValue() + 1];
            System.arraycopy(nd_tbl(), 0, idTable, 0, nd_tbl().length);
            
            copy.nd_tbl(idTable);
            // ???
        } else {
            copy.nd_tbl(null);
        }
        
        return copy;
    }
    
    /** NEW_METHOD
     *
     */
    public static NODE newMethod(NODE n, int noex) {
        return new NODE(NODE_METHOD, new Integer(noex), n, null);
    }
    
    /** NEW_CFUNC
     *
     */
    public static NODE newCallbackMethod(RubyCallbackMethod method) {
        return new NODE(NODE_CFUNC, method, null, null);
    }
    
    /** NEW_FBODY
     *
     */
    public static NODE newFBody(NODE n, RubyId i, RubyModule o) {
        return new NODE(NODE_FBODY, n, i, o);
    }
    
    /** NEW_IVAR
     *
     */
    public static NODE newIVar(RubyId v) {
        return new NODE(NODE_IVAR, v, null, null);
    }
    
    /** NEW_IFUNC
     *
     */
    public static NODE newIFunc(RubyCallbackMethod method, RubyObject arg) {
        return new NODE(NODE_IFUNC, method, arg, null);
    }
    
    /** NEW_IFUNC
     *
     */
    public static NODE newZSuper() {
        return new NODE(NODE_ZSUPER, null, null, null);
    }
    
    /** NEW_ATTRSET
     *
     */
    public static NODE newAttrSet(RubyId v) {
        return new NODE(NODE_ATTRSET, v, null, null);
    }
            
    public String toString() {
        return super.toString() + "("+type_name(type)+")";
    }

    /** Uses the magic of reflections to determine the node type string. */
    private static String type_name(int t) {
        java.lang.reflect.Field[] f = node_type.class.getDeclaredFields();
        try {
            for (int i = 0; i < f.length; i++)
                if (f[i].getInt(null) == t)
                    return f[i].getName();
        } catch (IllegalAccessException e) {}
        return String.valueOf(t);
    }

    /** Prints this node and all subnodes with indentation. */
    public void print(int indent, Set visited) {
        visited.add(this);
        indent(indent); System.out.println(this);
        print(indent + 1, u1, visited);
        print(indent + 1, u2, visited);
        print(indent + 1, u3, visited);
    }
    public static void print(int indent, Object o, Set visited) {
        if (o == null) return;
        if (visited.contains(o)) {
            indent(indent);
            System.out.println("(...@" + Integer.toHexString(o.hashCode()) + "...)");
        }
        else if (o instanceof NODE)
            ((NODE)o).print(indent, visited);
        else {
            indent(indent); System.out.println(o);
        }
    }
    static void indent(int indent) {
        for (int i = 0; i < indent; i++) System.out.print(" | ");
    }
}

