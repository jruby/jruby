/*
 * RubyHash.java - No description
 * Created on 04. Juli 2001, 22:53
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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
import org.jruby.runtime.*;
import org.jruby.util.*;

/** Implementation of the Hash class.
 *
 * @author  jpetersen
 * @version $Revision$
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

    public static RubyClass createHashClass(Ruby ruby) {
        RubyClass hashClass = ruby.defineClass("Hash", ruby.getClasses().getObjectClass());
        hashClass.includeModule(ruby.getClasses().getEnumerableModule());

        hashClass.defineSingletonMethod("new", CallbackFactory.getOptSingletonMethod(RubyHash.class, "newInstance"));
        hashClass.defineSingletonMethod("[]", CallbackFactory.getOptSingletonMethod(RubyHash.class, "create"));
        hashClass.defineMethod("initialize", CallbackFactory.getOptMethod(RubyHash.class, "initialize"));

        //    rb_define_method(rb_cHash,"clone", rb_hash_clone, 0);
        //    rb_define_method(rb_cHash,"rehash", rb_hash_rehash, 0);

        hashClass.defineMethod("to_hash", CallbackFactory.getMethod(RubyHash.class, "to_hash"));
        hashClass.defineMethod("to_a", CallbackFactory.getMethod(RubyHash.class, "to_a"));
        hashClass.defineMethod("to_s", CallbackFactory.getMethod(RubyHash.class, "to_s"));
        hashClass.defineMethod("inspect", CallbackFactory.getMethod(RubyHash.class, "inspect"));

        hashClass.defineMethod("==", CallbackFactory.getMethod(RubyHash.class, "equal", RubyObject.class));
        hashClass.defineMethod("[]", CallbackFactory.getMethod(RubyHash.class, "aref", RubyObject.class));
        //    rb_define_method(rb_cHash,"fetch", rb_hash_fetch, -1);
        hashClass.defineMethod("[]=", CallbackFactory.getMethod(RubyHash.class, "aset", RubyObject.class, RubyObject.class));
        hashClass.defineMethod("store", CallbackFactory.getMethod(RubyHash.class, "aset", RubyObject.class, RubyObject.class));
        //    rb_define_method(rb_cHash,"default", rb_hash_default, 0);
        //    rb_define_method(rb_cHash,"default=", rb_hash_set_default, 1);
        //    rb_define_method(rb_cHash,"index", rb_hash_index, 1);
        hashClass.defineMethod("indexes", CallbackFactory.getOptMethod(RubyHash.class, "indexes"));
        hashClass.defineMethod("indices", CallbackFactory.getOptMethod(RubyHash.class, "indexes"));
        hashClass.defineMethod("size", CallbackFactory.getMethod(RubyHash.class, "size"));
        hashClass.defineMethod("length", CallbackFactory.getMethod(RubyHash.class, "size"));
        hashClass.defineMethod("empty?", CallbackFactory.getMethod(RubyHash.class, "empty_p"));

		hashClass.defineMethod("each", CallbackFactory.getMethod(RubyHash.class, "each"));
		hashClass.defineMethod("each_pair", CallbackFactory.getMethod(RubyHash.class, "each"));
        //    rb_define_method(rb_cHash,"each", rb_hash_each_pair, 0);
        //    rb_define_method(rb_cHash,"each_value", rb_hash_each_value, 0);
        //    rb_define_method(rb_cHash,"each_key", rb_hash_each_key, 0);
        //    rb_define_method(rb_cHash,"each_pair", rb_hash_each_pair, 0);
        //    rb_define_method(rb_cHash,"sort", rb_hash_sort, 0);

		hashClass.defineMethod("keys", CallbackFactory.getMethod(RubyHash.class, "keys"));
		hashClass.defineMethod("values", CallbackFactory.getMethod(RubyHash.class, "values"));

		hashClass.defineMethod("shift", CallbackFactory.getMethod(RubyHash.class, "shift"));
        //    rb_define_method(rb_cHash,"delete", rb_hash_delete, 1);
        //    rb_define_method(rb_cHash,"delete_if", rb_hash_delete_if, 0);
        //    rb_define_method(rb_cHash,"reject", rb_hash_reject, 0);
        //    rb_define_method(rb_cHash,"reject!", rb_hash_reject_bang, 0);
        //    rb_define_method(rb_cHash,"clear", rb_hash_clear, 0);
        //    rb_define_method(rb_cHash,"invert", rb_hash_invert, 0);
        //    rb_define_method(rb_cHash,"update", rb_hash_update, 1);
        //    rb_define_method(rb_cHash,"replace", rb_hash_replace, 1);

        hashClass.defineMethod("include?", CallbackFactory.getMethod(RubyHash.class, "has_key", RubyObject.class));
        hashClass.defineMethod("member?", CallbackFactory.getMethod(RubyHash.class, "has_key", RubyObject.class));
        hashClass.defineMethod("has_key?", CallbackFactory.getMethod(RubyHash.class, "has_key", RubyObject.class));
        hashClass.defineMethod("has_value?", CallbackFactory.getMethod(RubyHash.class, "has_value", RubyObject.class));
        hashClass.defineMethod("key?", CallbackFactory.getMethod(RubyHash.class, "has_key", RubyObject.class));
        hashClass.defineMethod("value?", CallbackFactory.getMethod(RubyHash.class, "has_value", RubyObject.class));

        return hashClass;
    }

    /** rb_hash_modify
     *
     */
    public void modify() {
        if (isFrozen()) {
            throw new RubyFrozenException(getRuby(), "Hash");
        }
        if (isTaint() && getRuby().getSafeLevel() >= 4) {
            throw new RubySecurityException(getRuby(), "Insecure: can't modify hash");
        }
    }

    public int length() {
        return valueMap.size();
    }

    // Hash methods

    public static RubyHash newHash(Ruby ruby) {
        return newInstance(ruby, ruby.getRubyClass("Hash"), new RubyObject[0]);
    }

    public static RubyHash newInstance(Ruby ruby, RubyObject recv, RubyObject[] args) {
        RubyHash hash = new RubyHash(ruby);
        hash.setRubyClass((RubyClass) recv);

        hash.callInit(args);

        return hash;
    }

    public static RubyHash create(Ruby ruby, RubyObject recv, RubyObject[] args) {
        RubyHash hsh = new RubyHash(ruby);
        if (args.length == 1) {
            hsh.setValueMap(new RubyHashMap(((RubyHash) args[0]).getValueMap()));
        } else if (args.length % 2 != 0) {
            throw new RubyArgumentException(ruby, "odd number of args for Hash");
        } else {
            for (int i = 0; i < args.length; i += 2) {
                hsh.aset(args[i], args[i + 1]);
            }
        }
        return hsh;
    }

    public RubyObject initialize(RubyObject[] args) {
        if (args.length > 0) {
            modify();

            setDefaultValue(args[0]);
        }
        return this;
    }

    public RubyString inspect() {
        //FIXME this two strings should be built only once, at leas only once
        //per instance no matter how many calls to inspect and maybe better once
        //per ruby interpreter.
        //Benoit
        final String sep = ", ";
        final String arrow = "=>";
        
        final StringBuffer sb = new StringBuffer("{");

        valueMap.foreach(new RubyMapMethod() {
            boolean firstEntry = true;
            public int execute(Object key, Object value, Object arg) {
                // RubyString str = RubyString.stringValue((RubyObject) arg);
                if (!firstEntry) {
                    sb.append(sep);
                }
                sb.append(((RubyObject) key).funcall("inspect"));
                sb.append(arrow);
                sb.append(((RubyObject) value).funcall("inspect"));
                firstEntry = false;
                return RubyMapMethod.CONTINUE;
            }
        }, null);

        sb.append("}");
        return RubyString.newString(ruby, sb.toString());
    }

    public RubyFixnum size() {
        return RubyFixnum.newFixnum(getRuby(), length());
    }

    public RubyBoolean empty_p() {
        return length() == 0 ? getRuby().getTrue() : getRuby().getFalse();
    }

    public RubyArray to_a() {
        RubyArray result = RubyArray.newArray(getRuby(), length());
        valueMap.foreach(new RubyMapMethod() {
            public int execute(Object key, Object value, Object arg) {
                RubyArray ary = RubyArray.arrayValue((RubyObject) arg);
                ary.push(RubyArray.newArray(getRuby(), (RubyObject) key, (RubyObject) value));
                return RubyMapMethod.CONTINUE;
            }
        }, result);
        return result;
    }

    public RubyString to_s() {
        return to_a().to_s();
    }

    public RubyHash to_hash() {
        return this;
    }

    public RubyObject aset(RubyObject key, RubyObject value) {
        modify();

        if (!(key instanceof RubyString) || valueMap.get(key) != null) {
            valueMap.put(key, value);
        } else {
            RubyObject realKey = ((RubyString) key).dup();
            realKey.setFrozen(true);
            valueMap.put(realKey, value);
        }
        return this;
    }

    public RubyObject aref(RubyObject key) {
        RubyObject value = (RubyObject) valueMap.get(key);

        return value != null ? value : getDefaultValue();
    }
    
    public RubyBoolean has_key(RubyObject key) {
        return RubyBoolean.newBoolean(ruby, valueMap.containsKey(key));
    }

    public RubyBoolean has_value(RubyObject value) {
        return RubyBoolean.newBoolean(ruby, valueMap.containsValue(value));
    }
    
    public RubyObject each() {
        // +++
        Iterator iter = valueMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            
            ruby.yield(RubyArray.newArray(ruby, (RubyObject)entry.getKey(), (RubyObject)entry.getValue()));
        }
        return ruby.getNil();
        // ---
    }
    
    public RubyArray indexes(RubyObject[] indices) {
        ArrayList values = new ArrayList(indices.length);
        
        for (int i = 0; i < indices.length; i++) {
            values.add(aref(indices[i]));
        }
        
        return RubyArray.newArray(ruby, values);
    }

    public RubyArray keys() {
        return RubyArray.newArray(ruby, new ArrayList(valueMap.keySet()));
    }

    public RubyArray values() {
        return RubyArray.newArray(ruby, new ArrayList(valueMap.values()));
    }

    public RubyBoolean equal(RubyObject other) {
        if (this == other) {
            return ruby.getTrue();
        } else if (!(other instanceof RubyHash)) {
            return ruby.getFalse();
        } else if (length() != ((RubyHash)other).length()) {
            return ruby.getFalse();
        }

        // +++
        Iterator iter = valueMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            
            Object value = ((RubyHash)other).valueMap.get(entry.getKey());
            if (value == null || !entry.getValue().equals(value)) {
                return ruby.getFalse();
            }
        }
        return ruby.getTrue();
        // ---
    }

    public RubyArray shift() {
        Iterator iter = valueMap.entrySet().iterator();
        Map.Entry entry = (Map.Entry)iter.next();
        iter.remove();
        
        return RubyArray.newArray(ruby, (RubyObject)entry.getKey(), (RubyObject)entry.getValue());
    }

}