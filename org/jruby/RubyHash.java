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
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.*;

/** Implementation of the Hash class.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyHash extends RubyObject {
    private RubyMap valueMap;
    private IRubyObject defaultValue;

    public RubyHash(Ruby ruby) {
        this(ruby, ruby.getNil());
    }

    public RubyHash(Ruby ruby, IRubyObject defaultValue) {
        this(ruby, new RubyHashMap(), defaultValue);
    }

    public RubyHash(Ruby ruby, Map valueMap, IRubyObject defaultValue) {
        super(ruby, ruby.getRubyClass("Hash"));
        this.valueMap = new RubyHashMap(valueMap);
        this.defaultValue = defaultValue;
    }

    public static RubyHash nilHash(Ruby ruby) {
        return new RubyHash(ruby) {
            public boolean isNil() {
                return true;
            }
        };
    }

    public IRubyObject getDefaultValue() {
        return (defaultValue == null) ? getRuntime().getNil() : defaultValue;
    }

    public void setDefaultValue(IRubyObject defaultValue) {
        this.defaultValue = defaultValue;
    }

    public RubyMap getValueMap() {
        return valueMap;
    }

    public void setValueMap(RubyMap valueMap) {
        this.valueMap = valueMap;
    }

	/**
	 * gets an iterator on a copy of the keySet.
	 * modifying the iterator will NOT modify the map.
	 * if the map is modified while iterating on this iterator, the iterator
	 * will not be invalidated but the content will be the same as the old one.
	 * @return the iterator
	 **/
	private Iterator keyIterator() {
		return new ArrayList(valueMap.keySet()).iterator();
	}

	/**
	 * gets an iterator on the keySet.
	 * modifying the iterator WILL modify the map.
	 * the iterator will be invalidated if the map is modified.
	 * @return the iterator
	 **/
	private Iterator modifiableKeyIterator() {
		return valueMap.keySet().iterator();
	}

	private Iterator valueIterator() {
		return new ArrayList(valueMap.values()).iterator();
	}


	/**
	 * gets an iterator on the entries.
	 * modifying this iterator WILL modify the map.
	 * the iterator will be invalidated if the map is modified.
	 * @return the iterator
	 */
	private Iterator modifiableEntryIterator() {
		return valueMap.entrySet().iterator();
	}



	/**
	 * gets an iterator on a copy of the entries.
	 * modifying this iterator will NOT modify the map.
	 * if the map is modified while iterating on this iterator, the iterator
	 * will not be invalidated but the content will be the same as the old one.
	 * @return the iterator
	 */
	private Iterator entryIterator() {
		return new ArrayList(valueMap.entrySet()).iterator();		//in general we either want to modify the map or make sure we don't when we use this, so skip the copy
	}


    public static RubyClass createHashClass(Ruby ruby) {
        RubyClass hashClass = ruby.defineClass("Hash", ruby.getClasses().getObjectClass());
        hashClass.includeModule(ruby.getClasses().getEnumerableModule());

        hashClass.defineSingletonMethod("new", CallbackFactory.getOptSingletonMethod(RubyHash.class, "newInstance"));
        hashClass.defineSingletonMethod("[]", CallbackFactory.getOptSingletonMethod(RubyHash.class, "create"));
        hashClass.defineMethod("initialize", CallbackFactory.getOptMethod(RubyHash.class, "initialize"));
		hashClass.defineMethod("clone", CallbackFactory.getMethod(RubyHash.class, "rbClone"));

        //    rb_define_method(rb_cHash,"rehash", rb_hash_rehash, 0);

        hashClass.defineMethod("to_hash", CallbackFactory.getMethod(RubyHash.class, "to_hash"));
        hashClass.defineMethod("to_a", CallbackFactory.getMethod(RubyHash.class, "to_a"));
        hashClass.defineMethod("to_s", CallbackFactory.getMethod(RubyHash.class, "to_s"));
        hashClass.defineMethod("inspect", CallbackFactory.getMethod(RubyHash.class, "inspect"));

        hashClass.defineMethod("==", CallbackFactory.getMethod(RubyHash.class, "equal", IRubyObject.class));
        hashClass.defineMethod("[]", CallbackFactory.getMethod(RubyHash.class, "aref", IRubyObject.class));
        hashClass.defineMethod("fetch", CallbackFactory.getOptMethod(RubyHash.class, "fetch"));
        hashClass.defineMethod("[]=", CallbackFactory.getMethod(RubyHash.class, "aset", IRubyObject.class, IRubyObject.class));
        hashClass.defineMethod("store", CallbackFactory.getMethod(RubyHash.class, "aset", IRubyObject.class, IRubyObject.class));
		hashClass.defineMethod("default", CallbackFactory.getMethod(RubyHash.class, "getDefaultValue"));
		hashClass.defineMethod("default=", CallbackFactory.getMethod(RubyHash.class, "setDefaultValue", IRubyObject.class));
        //    rb_define_method(rb_cHash,"index", rb_hash_index, 1);
        hashClass.defineMethod("indexes", CallbackFactory.getOptMethod(RubyHash.class, "indexes"));
        hashClass.defineMethod("indices", CallbackFactory.getOptMethod(RubyHash.class, "indexes"));
        hashClass.defineMethod("size", CallbackFactory.getMethod(RubyHash.class, "size"));
        hashClass.defineMethod("length", CallbackFactory.getMethod(RubyHash.class, "size"));
        hashClass.defineMethod("empty?", CallbackFactory.getMethod(RubyHash.class, "empty_p"));
		hashClass.defineMethod("each", CallbackFactory.getMethod(RubyHash.class, "each"));
		hashClass.defineMethod("each_pair", CallbackFactory.getMethod(RubyHash.class, "each"));
		hashClass.defineMethod("each_value", CallbackFactory.getMethod(RubyHash.class, "each_value"));
		hashClass.defineMethod("each_key", CallbackFactory.getMethod(RubyHash.class, "each_key"));
		hashClass.defineMethod("sort", CallbackFactory.getMethod(RubyHash.class, "sort"));
		hashClass.defineMethod("keys", CallbackFactory.getMethod(RubyHash.class, "keys"));
		hashClass.defineMethod("values", CallbackFactory.getMethod(RubyHash.class, "values"));

		hashClass.defineMethod("shift", CallbackFactory.getMethod(RubyHash.class, "shift"));
		hashClass.defineMethod("delete", CallbackFactory.getMethod(RubyHash.class, "delete", IRubyObject.class));
		hashClass.defineMethod("delete_if", CallbackFactory.getMethod(RubyHash.class, "delete_if"));
		hashClass.defineMethod("reject", CallbackFactory.getMethod(RubyHash.class, "reject"));
		hashClass.defineMethod("reject!", CallbackFactory.getMethod(RubyHash.class, "reject_bang"));
		hashClass.defineMethod("clear", CallbackFactory.getMethod(RubyHash.class, "clear"));
		hashClass.defineMethod("invert", CallbackFactory.getMethod(RubyHash.class, "invert"));
        hashClass.defineMethod("update", CallbackFactory.getMethod(RubyHash.class, "update", IRubyObject.class));
        hashClass.defineMethod("replace", CallbackFactory.getMethod(RubyHash.class, "replace", IRubyObject.class));
        hashClass.defineMethod("include?", CallbackFactory.getMethod(RubyHash.class, "has_key", IRubyObject.class));
        hashClass.defineMethod("member?", CallbackFactory.getMethod(RubyHash.class, "has_key", IRubyObject.class));
        hashClass.defineMethod("has_key?", CallbackFactory.getMethod(RubyHash.class, "has_key", IRubyObject.class));
        hashClass.defineMethod("has_value?", CallbackFactory.getMethod(RubyHash.class, "has_value", IRubyObject.class));
        hashClass.defineMethod("key?", CallbackFactory.getMethod(RubyHash.class, "has_key", IRubyObject.class));
        hashClass.defineMethod("value?", CallbackFactory.getMethod(RubyHash.class, "has_value", IRubyObject.class));

        return hashClass;
    }

    /** rb_hash_modify
     *
     */
    public void modify() {
        if (isFrozen()) {
            throw new FrozenError(getRuntime(), "Hash");
        }
        if (isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw new SecurityError(getRuntime(), "Insecure: can't modify hash");
        }
    }

    public int length() {
        return valueMap.size();
    }

    // Hash methods

    public static RubyHash newHash(Ruby ruby) {
        return newInstance(ruby.getRubyClass("Hash"), new IRubyObject[0]);
    }

	public static RubyHash newHash(Ruby ruby, Map valueMap, IRubyObject defaultValue) {
		return new RubyHash(ruby, valueMap, defaultValue);
	}

    public static RubyHash newInstance(IRubyObject recv, IRubyObject[] args) {
        RubyHash hash = new RubyHash(recv.getRuntime());
        hash.setInternalClass((RubyClass) recv);

        hash.callInit(args);

        return hash;
    }

    public static RubyHash create(IRubyObject recv, IRubyObject[] args) {
        RubyHash hsh = new RubyHash(recv.getRuntime());
        if (args.length == 1) {
            hsh.setValueMap(new RubyHashMap(((RubyHash) args[0]).getValueMap()));
        } else if (args.length % 2 != 0) {
            throw new ArgumentError(recv.getRuntime(), "odd number of args for Hash");
        } else {
            for (int i = 0; i < args.length; i += 2) {
                hsh.aset(args[i], args[i + 1]);
            }
        }
        return hsh;
    }

    public IRubyObject initialize(IRubyObject[] args) {
        if (args.length > 0) {
            modify();

            setDefaultValue(args[0]);
        }
        return this;
    }

    public RubyString inspect() {
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
                sb.append(((IRubyObject) key).callMethod("inspect"));
                sb.append(arrow);
                sb.append(((IRubyObject) value).callMethod("inspect"));
                firstEntry = false;
                return RubyMapMethod.CONTINUE;
            }
        }, null);

        sb.append("}");
        return RubyString.newString(runtime, sb.toString());
    }

    public RubyFixnum size() {
        return RubyFixnum.newFixnum(getRuntime(), length());
    }

    public RubyBoolean empty_p() {
        return length() == 0 ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public RubyArray to_a() {
        RubyArray result = RubyArray.newArray(getRuntime(), length());
        valueMap.foreach(new RubyMapMethod() {
            public int execute(Object key, Object value, Object arg) {
                RubyArray ary = RubyArray.arrayValue((IRubyObject) arg);
                ary.append(RubyArray.newArray(getRuntime(), (IRubyObject) key, (IRubyObject) value));
                return RubyMapMethod.CONTINUE;
            }
        }, result);
        return result;
    }

    public RubyString to_s() {
        return to_a().to_s();
    }

	public IRubyObject rbClone() {
		RubyHash result = newHash(runtime, getValueMap(), getDefaultValue());
		result.setupClone(this);
		return result;
	}

    public RubyHash to_hash() {
        return this;
    }

    public IRubyObject aset(IRubyObject key, IRubyObject value) {
        modify();

        if (!(key instanceof RubyString) || valueMap.get(key) != null) {
            valueMap.put(key, value);
        } else {
            IRubyObject realKey = ((RubyString) key).dup();
            realKey.setFrozen(true);
            valueMap.put(realKey, value);
        }
        return this;
    }

    public IRubyObject aref(IRubyObject key) {
        IRubyObject value = (IRubyObject) valueMap.get(key);

        return value != null ? value : getDefaultValue();
    }

    public IRubyObject fetch(IRubyObject[] args) {
        if (args.length < 1) {
            throw new ArgumentError(runtime, args.length, 1);
        }
        IRubyObject key = args[0];
        IRubyObject result = (IRubyObject) valueMap.get(key);
        if (result == null) {
            if (args.length > 1) {
                return args[1];
            } else if (runtime.isBlockGiven()) {
                return runtime.yield(key);
            } else {
                throw new IndexError(runtime, "key not found");
            }
        }
        return result;
    }


    public RubyBoolean has_key(IRubyObject key) {
        return RubyBoolean.newBoolean(runtime, valueMap.containsKey(key));
    }

    public RubyBoolean has_value(IRubyObject value) {
        return RubyBoolean.newBoolean(runtime, valueMap.containsValue(value));
    }

    public RubyHash each() {
        Iterator iter = entryIterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
			runtime.yield(RubyArray.newArray(runtime, (IRubyObject)entry.getKey(), (IRubyObject)entry.getValue()));
        }
        return this;
    }

	public RubyHash each_value() {
		Iterator iter = valueIterator();
		while (iter.hasNext()) {
			IRubyObject value = (IRubyObject) iter.next();
			runtime.yield(value);
		}
		return this;
	}

	public RubyHash each_key() {
		Iterator iter = keyIterator();		//the block may modify the hash so we need the iterator on a copy
		while (iter.hasNext()) {
			IRubyObject key = (IRubyObject) iter.next();
			runtime.yield(key);
		}
		return this;
	}

	public RubyArray sort() {
		RubyArray result = to_a();
		result.sort_bang();
		return result;
	}

    public RubyArray indexes(IRubyObject[] indices) {
        ArrayList values = new ArrayList(indices.length);

        for (int i = 0; i < indices.length; i++) {
            values.add(aref(indices[i]));
        }

        return RubyArray.newArray(runtime, values);
    }

    public RubyArray keys() {
        return RubyArray.newArray(runtime, new ArrayList(valueMap.keySet()));
    }

    public RubyArray values() {
        return RubyArray.newArray(runtime, new ArrayList(valueMap.values()));
    }

    public RubyBoolean equal(IRubyObject other) {
        if (this == other) {
            return runtime.getTrue();
        } else if (!(other instanceof RubyHash)) {
            return runtime.getFalse();
        } else if (length() != ((RubyHash)other).length()) {
            return runtime.getFalse();
        }

        // +++
        Iterator iter = modifiableEntryIterator();		//Benoit: this is ok, nobody is modifying the map
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();

            Object value = ((RubyHash)other).valueMap.get(entry.getKey());
            if (value == null || !entry.getValue().equals(value)) {
                return runtime.getFalse();
            }
        }
        return runtime.getTrue();
        // ---
    }

    public RubyArray shift() {
		modify();
        Iterator iter = modifiableEntryIterator();		//we want to modify the map so we need this iterator
        Map.Entry entry = (Map.Entry)iter.next();
        iter.remove();
		return RubyArray.newArray(runtime, (IRubyObject)entry.getKey(), (IRubyObject)entry.getValue());
    }

	public IRubyObject delete(IRubyObject key) {
		modify();
		IRubyObject result = (IRubyObject) valueMap.remove(key);
		if (result != null) {
			return result;
		} else if (runtime.isBlockGiven()) {
			return runtime.yield(key);
		} else {
			return getDefaultValue();
		}
	}

	public RubyHash delete_if() {
//		modify();		//Benoit: not needed, it is done in the reject_bang method
		reject_bang();
		return this;
	}

	public RubyHash reject() {
		RubyHash result = (RubyHash) dup();
		result.reject_bang();
		return result;
	}

	public RubyHash reject_bang() {
		modify();
		boolean isModified = false;
		Iterator iter = keyIterator();
		while (iter.hasNext()) {
			IRubyObject key = (IRubyObject) iter.next();
			IRubyObject value = (IRubyObject) valueMap.get(key);
			IRubyObject shouldDelete = runtime.yield(RubyArray.newArray(runtime, key, value));
			if (shouldDelete.isTrue()) {
				valueMap.remove(key);
				isModified = true;
			}
		}
		if (isModified) {
			return this;
		} else {
			return nilHash(runtime);
		}
	}

	public RubyHash clear() {
		modify();
		valueMap.clear();
		return this;
	}

	public RubyHash invert() {
		RubyHash result = newHash(runtime);
		Iterator iter = modifiableEntryIterator();		//this is ok since nobody will modify the map
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			IRubyObject key = (IRubyObject) entry.getKey();
			IRubyObject value = (IRubyObject) entry.getValue();
			result.aset(value, key);
		}
		return result;
	}

    public RubyHash update(IRubyObject freshElements) {
        modify();
        RubyHash freshElementsHash =
            (RubyHash) freshElements.convertType(RubyHash.class, "Hash", "to_hash");
        valueMap.putAll(freshElementsHash.valueMap);
        return this;
    }

    public RubyHash replace(IRubyObject replacement) {
        modify();
        RubyHash replacementHash =
            (RubyHash) replacement.convertType(RubyHash.class, "Hash", "to_hash");
        valueMap.clear();
        valueMap.putAll(replacementHash.valueMap);
        return this;
    }

	public void marshalTo(MarshalStream output) throws java.io.IOException {
		output.write('{');
		output.dumpInt(getValueMap().size());
		Iterator iter = entryIterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			IRubyObject key = (IRubyObject) entry.getKey();
			IRubyObject value = (IRubyObject) entry.getValue();
			output.dumpObject(key);
			output.dumpObject(value);
		}
	}

    public static RubyHash unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        RubyHash result = newHash(input.getRuntime());
        input.registerLinkTarget(result);
        int size = input.unmarshalInt();
        for (int i = 0; i < size; i++) {
            IRubyObject key = input.unmarshalObject();
            IRubyObject value = input.unmarshalObject();
            result.aset(key, value);
        }
        return result;
    }
}
