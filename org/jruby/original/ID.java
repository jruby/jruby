/*
 * ID.java - No description
 * Created on 10. September 2001, 17:53
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

public class ID implements VALUE, token {
    protected int value;

    /*public ID(int value) {
	this.value = value;
    }*/

    public int intValue() {
	return value;
    }
    
    public int hashCode() {
        return intValue();
    }

    public boolean equals(Object obj) {
        if (obj instanceof ID && obj != null) {
            return intValue() == ((ID)obj).intValue();
        }
        return false;
    }
    
    public final boolean is_notop_id() {
        return value > LAST_TOKEN;
    }

    public final boolean is_local_id() {
        return is_notop_id() && (value & ID_SCOPE_MASK) == ID_LOCAL;
    }

    public final boolean is_instance_id() {
        return is_notop_id() && (value & ID_SCOPE_MASK) == ID_INSTANCE;
    }

    public final boolean is_global_id() {
        return is_notop_id() && (value & ID_SCOPE_MASK) == ID_GLOBAL;
    }

    public final boolean is_attrset_id() {
        return is_notop_id() && (value & ID_SCOPE_MASK) == ID_ATTRSET;
    }

    public final ID rb_id_attrset(Ruby ruby) {
        return new RubyId(ruby, (value & ~ID_SCOPE_MASK) | ID_ATTRSET);
    }

    public final boolean is_const_id() {
        return is_notop_id() && (value & ID_SCOPE_MASK) == ID_CONST;
    }

    public final boolean is_class_id() {
        return is_notop_id() && (value & ID_SCOPE_MASK) == ID_CLASS;
    }

    // ------------------------------------------------------------------------
    // Symbol table
    // ------------------------------------------------------------------------

    private static final int ID_SCOPE_SHIFT = 3;
    private static final int ID_SCOPE_MASK = 7;
    private static final int ID_LOCAL    = 0x01;
    private static final int ID_INSTANCE = 0x02;
    private static final int ID_GLOBAL   = 0x03;
    private static final int ID_ATTRSET  = 0x04;
    private static final int ID_CONST    = 0x05;
    private static final int ID_CLASS    = 0x06;
    private static final int ID_JUNK     = 0x07;

    private static Map sym_tbl = new HashMap(200);
    private static Map sym_rev_tbl = new HashMap(200);

    private static ID last_id = null;
    
    private static ID last_id(Ruby ruby) {
        return last_id == null ? new RubyId(ruby, LAST_TOKEN) : last_id;
    }

    /** Create a new ID (aka symbol) for the given string. */
    public static ID rb_intern(String name, Ruby ruby) {
	int last;

	ID _id = (ID)sym_tbl.get(name);
	if (_id != null)
	    return _id;

	int id = 0;
	int m = 0;
        id_regist:for(;;) {
	switch (name.charAt(m)) {
	case '$':
	    id |= ID_GLOBAL;
	    m++;
	    if (!is_identchar(name.charAt(m)))
                m++;
	    break;
	case '@':
	    if (name.charAt(m + 1) == '@') {
		m++;
		id |= ID_CLASS;
	    }
	    else {
		id |= ID_INSTANCE;
	    }
	    m++;
	    break;
	default:
	    if (name.charAt(m) != '_' && !Character.isLetter(name.charAt(m))) {
                if (m != 0) throw new Error("m="+m);
		/* operators */
		for (int i=0; ruby.op_tbl[i].token != null; i++) {
		    if (ruby.op_tbl[i].name.equals(name)) {
			id = ruby.op_tbl[i].token.intValue();
			break id_regist;
		    }
		}
	    }

	    last = name.length() - 1;
	    if (name.charAt(last) == '=') {
		/* attribute assignment */
                String buf = name.substring(m, last);
		_id = rb_intern(buf, ruby); id = _id.intValue();
		if (id > LAST_TOKEN && !_id.is_attrset_id()) {
		    _id = _id.rb_id_attrset(ruby);
                    id = _id.intValue();
		    break id_regist;
		}
		id = ID_ATTRSET;
	    }
	    else if (Character.isUpperCase(name.charAt(m))) {
		id = ID_CONST;
	    }
	    else {
		id = ID_LOCAL;
	    }
	    break;
	} //switch
	while (m < name.length() && is_identchar(name.charAt(m))) {
	    m++;
	}
	if (m < name.length()) id = ID_JUNK;
        last_id = new RubyId(ruby, last_id(ruby).intValue() + 1);
	id |= last_id(ruby).intValue() << ID_SCOPE_SHIFT;
        break id_regist;
        }
        //id_regist:
        _id = new RubyId(ruby, id);
	sym_tbl.put(name, _id);
	sym_rev_tbl.put(_id, name);
	return _id;
    }

    public static final boolean is_identchar(char ch) {
	return Character.isLetterOrDigit(ch) || ch == '_';
    }

    /** Returns the ID's print string */
    public static String rb_id2name(Ruby ruby, ID id) {
	if (id.intValue() < LAST_TOKEN) {
	    for (int i = 0; ruby.op_tbl[i].token != null; i++) {
		if (ruby.op_tbl[i].token.equals(id))
		    return ruby.op_tbl[i].name;
	    }
	}

	String name = (String)sym_rev_tbl.get(id);
	if (name != null)
	    return name;

	if (id.is_attrset_id()) {
	    ID id2 = new RubyId(ruby, (id.intValue() & ~ID_SCOPE_MASK) | ID_LOCAL);

	again: for(;;) {
	    name = rb_id2name(ruby, id2);
	    if (name != null) {
		ID.rb_intern(name + "=", ((RubyId)id).getRuby());
		return rb_id2name(ruby, id);
	    }
	    if (id2.is_local_id()) {
		id2 = new RubyId(ruby, (id.intValue() & ~ID_SCOPE_MASK) | ID_CONST);
		continue again;
	    }
	    break again;
	    }
	}
	return null;
    }

    public static String rb_id2name_last_id(Ruby ruby) {
        return rb_id2name(ruby, last_id(ruby));
    }
}
