/*
 * RubyId.java - No description
 * Created on 09. Juli 2001, 21:38
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

package org.jruby;

import org.jruby.original.*;
import org.jruby.parser.*;
import org.jruby.util.*;

/**
 *
 * @author  jpetersen
 */
public final class RubyId {
    private static final int ID_SCOPE_SHIFT     = 3;
    private static final int ID_SCOPE_MASK      = 7;
    private static final int ID_LOCAL           = 0x01;
    private static final int ID_INSTANCE        = 0x02;
    private static final int ID_GLOBAL          = 0x03;
    private static final int ID_ATTRSET         = 0x04;
    private static final int ID_CONST           = 0x05;
    private static final int ID_CLASS           = 0x06;
    
    private Ruby ruby;

    private int value;
    
    private RubyId(Ruby ruby, int value) {
        super();
        this.ruby = ruby;
	this.value = value;
    }
    
    public static RubyId newId(Ruby ruby) {
        return newId(ruby, 0);
    }
    
    public static RubyId newId(Ruby ruby, int value) {
        // todo: add cache.
        
        return new RubyId(ruby, value);
    }
    
    /** Getter for property ruby.
     * @return Value of property ruby.
     */
    public Ruby getRuby() {
        return ruby;
    }
    
    public int intValue() {
	return value;
    }
    
    public int hashCode() {
        return value;
    }

    public boolean equals(Object obj) {
        if (obj instanceof RubyId && obj != null) {
            return value == obj.hashCode();
        }
        return false;
    }
    
    public boolean isConstId() {
        return is_const_id();
    }
    
    public boolean isClassId() {
        return is_class_id();
    }
    
    public boolean isGlobalId() {
        return is_global_id();
    }
    
    public boolean isAttrSetId() {
        return is_attrset_id();
    }
    
    public RubyId toAttrSetId() {
        return rb_id_attrset();
    }
    
    public boolean isLocalId() {
        return is_local_id();
    }

    public boolean isInstanceId() {
        return is_instance_id();
    }
    
    public String toName() {
        if (value < Token.LAST_TOKEN) {
            RubyOperatorEntry[] opTable = ruby.getOperatorTable();
            for (int i = 0; i < opTable.length; i++) {
                if (this.equals(opTable[i].token)) {
                    return opTable[i].name;
                }
            }
	}

	String name = (String)ruby.getSymbolReverseTable().get(this);
	if (name != null) {
	    return name;
        }

	if (isAttrSetId()) {
            RubyId id2 = newId(ruby, (value & ~ID_SCOPE_MASK) | ID_LOCAL);
            while (true) {
                name = id2.toName();
                if (name != null) {
                    return ruby.intern(name + "=").toName();
                }
                if (id2.isLocalId()) {
                    id2 = new RubyId(ruby, (value & ~ID_SCOPE_MASK) | ID_CONST);
                    continue;
                }
                break;
	    }
	}
	return null;
    }
    
    public RubySymbol toSymbol() {
        return RubySymbol.m_newSymbol(ruby, this);
    }
    
    public static RubyId intern(Ruby ruby, String name) {
        RubyId rubyId = (RubyId)ruby.getSymbolTable().get(name);
        if (rubyId != null) {
            return rubyId;
        }
        
        int id = 0;
        // int m = 0;

        switch (name.charAt(0)) {
            case '$':
                id |= ID_GLOBAL;
                break;
            case '@':
                if (name.charAt(1) == '@') {
                    id |= ID_CLASS;
                } else {
                    id |= ID_INSTANCE;
                }
                break;
            default:
                if (name.charAt(0) != '_' && !Character.isLetter(name.charAt(0))) {
                    /* operators */
                    RubyOperatorEntry[] opTable = ruby.getOperatorTable();
                    for (int i = 0; i < opTable.length; i++) {
                        if (opTable[i].name.equals(name)) {
                            return opTable[i].token;
                        }
                    }
                }
                    
                if (name.endsWith("=")) {
                    /* attribute assignment */
                    String buf = name.substring(0, name.length() - 1);
                    rubyId = intern(ruby, buf);
                    id = rubyId.intValue();
                    if (id > Token.LAST_TOKEN && !rubyId.isAttrSetId()) {
                        return registerId(ruby, rubyId.toAttrSetId(), name);
                    }
                    id = ID_ATTRSET;
                } else if (Character.isUpperCase(name.charAt(0))) {
                    id = ID_CONST;
                } else {
                    id = ID_LOCAL;
                }
                break;
        } //switch
        ruby.setLastId(ruby.getLastId() + 1);
        id |= ruby.getLastId() << ID_SCOPE_SHIFT;
        return registerId(ruby, id, name);
    }
    
    private static RubyId registerId(Ruby ruby, int id, String name) {
        return registerId(ruby, newId(ruby, id), name);
    }
    
    private static RubyId registerId(Ruby ruby, RubyId rubyId, String name) {
        ruby.getSymbolTable().put(name, rubyId);
        ruby.getSymbolReverseTable().put(rubyId, name);
	return rubyId;
    }

    public static final boolean isIdentChar(char ch) {
	return Character.isLetterOrDigit(ch) || ch == '_';
    }
    
    // deprecated methods
    
    /**
     * @deprecated replaced by isNotopId()
     */
    public final boolean is_notop_id() {
        return value > Token.LAST_TOKEN;
    }

    /**
     * @deprecated replaced by isLocalId()
     */
    public final boolean is_local_id() {
        return is_notop_id() && (value & ID_SCOPE_MASK) == ID_LOCAL;
    }

    /**
     * @deprecated replaced by isInstanceId()
     */
    public final boolean is_instance_id() {
        return is_notop_id() && (value & ID_SCOPE_MASK) == ID_INSTANCE;
    }

    /**
     * @deprecated replaced by isGlobalId()
     */
    public final boolean is_global_id() {
        return is_notop_id() && (value & ID_SCOPE_MASK) == ID_GLOBAL;
    }

    /**
     * @deprecated replaced by isAttrSetId()
     */
    public final boolean is_attrset_id() {
        return is_notop_id() && (value & ID_SCOPE_MASK) == ID_ATTRSET;
    }

    /**
     * @deprecated replaced by toAttrSetId()
     */
    public final RubyId rb_id_attrset() {
        return RubyId.newId(ruby, (value & ~ID_SCOPE_MASK) | ID_ATTRSET);
    }

    /**
     * @deprecated replaced by isConstId()
     */
    public final boolean is_const_id() {
        return is_notop_id() && (value & ID_SCOPE_MASK) == ID_CONST;
    }

    /**
     * @deprecated replaced by isClassId()
     */
    public final boolean is_class_id() {
        return is_notop_id() && (value & ID_SCOPE_MASK) == ID_CLASS;
    }
}