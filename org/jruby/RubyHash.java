/*
 * RubyHash.java - No description
 * Created on 04. Juli 2001, 22:53
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

import java.util.*;

import org.jruby.exceptions.*;
import org.jruby.util.*;

/**
 *
 * @author  jpetersen
 */
public class RubyHash extends RubyObject {
    private RubyMap valueMap;
    private RubyObject defaultValue;

    public RubyHash(Ruby ruby) {
        this(ruby, new RubyHashMap());
    }

    public RubyHash(Ruby ruby, RubyMap valueMap) {
        this(ruby, valueMap, ruby.getNil());
    }

    public RubyHash(Ruby ruby, RubyMap valueMap, RubyObject defaultValue) {
        super(ruby, ruby.getRubyClass("Hash"));
        this.valueMap = valueMap;
        this.defaultValue = defaultValue;
    }

    public RubyObject getDefaultValue() {
        return (defaultValue == null) ? getRuby().getNil() : defaultValue;
    }

    public void setDefaultValue(RubyObject defaultValue) {
        this.defaultValue = defaultValue;
    }

    public RubyMap getValueMap() {
        return valueMap;
    }

    public void setValueMap(RubyMap valueMap) {
        this.valueMap = valueMap;
    }

    /** rb_hash_modify
     *
     */
    public void modify() {
        if (isFrozen()) {
            throw new RubyFrozenException(getRuby(), "Hash");
        }
        if (isTaint() && getRuby().getSecurityLevel() >= 4) {
            throw new RubySecurityException(getRuby(), "Insecure: can't modify hash");
        }
    }

    public int length() {
        return valueMap.size();
    }

    // Hash methods

    public static RubyHash m_newHash(Ruby ruby) {
        return m_new(ruby, ruby.getRubyClass("Hash"), new RubyObject[0]);
    }

    public static RubyHash m_new(Ruby ruby, RubyObject recv, RubyObject[] args) {
        RubyHash hash = new RubyHash(ruby);
        hash.setRubyClass((RubyClass) recv);

        hash.callInit(args);

        return hash;
    }

    public static RubyHash m_create(Ruby ruby, RubyObject recv, RubyObject[] args) {
        RubyHash hsh = new RubyHash(ruby);
        if (args.length == 1) {
            hsh.setValueMap(new RubyHashMap(((RubyHash) args[0]).getValueMap()));
        } else if (args.length % 2 != 0) {
            throw new RubyArgumentException(ruby, "odd number of args for Hash");
        } else {
            for (int i = 0; i < args.length; i += 2) {
                hsh.m_aset(args[i], args[i + 1]);
            }
        }
        return hsh;
    }

    public RubyObject m_initialize(RubyObject[] args) {
        if (args.length > 0) {
            modify();

            setDefaultValue(args[0]);
        }
        return this;
    }

    public RubyString m_inspect() {
        final RubyString sep = RubyString.newString(getRuby(), ", ");
        final RubyString arrow = RubyString.newString(getRuby(), "=>");
        RubyString result = RubyString.newString(getRuby(), "{");

        valueMap.foreach(new RubyMapMethod() {
            boolean firstEntry = true;
            public int execute(Object key, Object value, Object arg) {
                RubyString str = RubyString.stringValue((RubyObject) arg);
                if (!firstEntry) {
                    str.m_append(sep);
                }
                str.m_append(((RubyObject) key).inspect());
                str.m_append(arrow);
                str.m_append(((RubyObject) value).inspect());
                firstEntry = false;
                return RubyMapMethod.CONTINUE;
            }
        }, result);

        result.m_append(RubyString.newString(getRuby(), "}"));
        return result;
    }

    public RubyFixnum m_size() {
        return RubyFixnum.m_newFixnum(getRuby(), length());
    }

    public RubyBoolean m_empty_p() {
        return length() == 0 ? getRuby().getTrue() : getRuby().getFalse();
    }

    public RubyArray m_to_a() {
        RubyArray result = RubyArray.m_newArray(getRuby(), length());
        valueMap.foreach(new RubyMapMethod() {
            public int execute(Object key, Object value, Object arg) {
                RubyArray ary = RubyArray.arrayValue((RubyObject) arg);
                ary.push(RubyArray.m_newArray(getRuby(), (RubyObject) key, (RubyObject) value));
                return RubyMapMethod.CONTINUE;
            }
        }, result);
        return result;
    }

    public RubyString m_to_s() {
        return m_to_a().m_to_s();
    }

    public RubyHash m_to_hash() {
        return this;
    }

    public RubyObject m_aset(RubyObject key, RubyObject value) {
        modify();

        if (!(key instanceof RubyString) || valueMap.get(key) != null) {
            valueMap.put(key, value);
        } else {
            RubyObject realKey = ((RubyString) key).m_dup();
            realKey.setFrozen(true);
            valueMap.put(realKey, value);
        }
        return this;
    }

    public RubyObject m_aref(RubyObject key) {
        RubyObject value = (RubyObject) valueMap.get(key);

        return value != null ? value : getDefaultValue();
    }
}