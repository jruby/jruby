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

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.exceptions.SecurityError;
import org.jruby.exceptions.FrozenError;
import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.IndexError;
import org.jruby.internal.runtime.builtin.definitions.HashDefinition;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

/** Implementation of the Hash class.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyHash extends RubyObject {
    private Map valueMap;
    private IRubyObject defaultValue;

    private boolean isRehashing = false;

    public RubyHash(Ruby ruby) {
        this(ruby, ruby.getNil());
    }

    public RubyHash(Ruby ruby, IRubyObject defaultValue) {
        this(ruby, new HashMap(), defaultValue);
    }

    public RubyHash(Ruby ruby, Map valueMap, IRubyObject defaultValue) {
        super(ruby, ruby.getClass("Hash"));
        this.valueMap = new HashMap(valueMap);
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

    public IRubyObject setDefaultValue(IRubyObject defaultValue) {
        this.defaultValue = defaultValue;
        return defaultValue;
    }

    public Map getValueMap() {
        return valueMap;
    }

    public void setValueMap(Map valueMap) {
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

    public static RubyClass createHashClass(Ruby runtime) {
        return new HashDefinition(runtime).getType();
    }

    public IRubyObject callIndexed(int index, IRubyObject[] args) {
        switch (index) {
            case HashDefinition.INITIALIZE :
                return initialize(args);
            case HashDefinition.RBCLONE :
                return rbClone();
            case HashDefinition.REHASH :
                return rehash();
            case HashDefinition.TO_HASH :
                return to_hash();
            case HashDefinition.TO_A :
                return to_a();
            case HashDefinition.TO_S :
                return to_s();
            case HashDefinition.INSPECT :
                return inspect();
            case HashDefinition.EQUAL :
                return equal(args[0]);
            case HashDefinition.AREF :
                return aref(args[0]);
            case HashDefinition.FETCH :
                return fetch(args);
            case HashDefinition.ASET :
                return aset(args[0], args[1]);
            case HashDefinition.GETDEFAULTVALUE :
                return getDefaultValue();
            case HashDefinition.SETDEFAULTVALUE :
                return setDefaultValue(args[0]);
            case HashDefinition.INDEX :
                return index(args[0]);
            case HashDefinition.INDICES :
                return indices(args);
            case HashDefinition.SIZE :
                return size();
            case HashDefinition.EMPTY_P :
                return empty_p();
            case HashDefinition.EACH :
                return each();
            case HashDefinition.EACH_VALUE :
                return each_value();
            case HashDefinition.EACH_KEY :
                return each_key();
            case HashDefinition.SORT :
                return sort();
            case HashDefinition.KEYS :
                return keys();
            case HashDefinition.VALUES :
                return values();
            case HashDefinition.SHIFT :
                return shift();
            case HashDefinition.DELETE :
                return delete(args[0]);
            case HashDefinition.DELETE_IF :
                return delete_if();
            case HashDefinition.REJECT :
                return reject();
            case HashDefinition.REJECT_BANG :
                return reject_bang();
            case HashDefinition.CLEAR :
                return clear();
            case HashDefinition.INVERT :
                return invert();
            case HashDefinition.UPDATE :
                return update(args[0]);
            case HashDefinition.REPLACE :
                return replace(args[0]);
            case HashDefinition.HAS_KEY :
                return has_key(args[0]);
            case HashDefinition.HAS_VALUE :
                return has_value(args[0]);
            default :
                return super.callIndexed(index, args);
        }
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

    private int length() {
        return valueMap.size();
    }

    // Hash methods

    public static RubyHash newHash(Ruby ruby) {
        return newInstance(ruby.getClass("Hash"), IRubyObject.NULL_ARRAY);
    }

	public static RubyHash newHash(Ruby ruby, Map valueMap, IRubyObject defaultValue) {
		return new RubyHash(ruby, valueMap, defaultValue);
	}

    public static RubyHash newInstance(IRubyObject recv, IRubyObject[] args) {
        RubyHash hash = new RubyHash(recv.getRuntime());
        hash.setMetaClass((RubyClass) recv);

        hash.callInit(args);

        return hash;
    }

    public static RubyHash create(IRubyObject recv, IRubyObject[] args) {
        RubyHash hsh = new RubyHash(recv.getRuntime());
        if (args.length == 1) {
            hsh.setValueMap(new HashMap(((RubyHash) args[0]).getValueMap()));
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

        Iterator iter = valueMap.entrySet().iterator();
        boolean firstEntry = true;
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            IRubyObject key = (IRubyObject) entry.getKey();
            IRubyObject value = (IRubyObject) entry.getValue();
            if (!firstEntry) {
                sb.append(sep);
            }
            sb.append(key.callMethod("inspect"));
            sb.append(arrow);
            sb.append(value.callMethod("inspect"));
            firstEntry = false;
        }
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
        Iterator iter = valueMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            IRubyObject key = (IRubyObject) entry.getKey();
            IRubyObject value = (IRubyObject) entry.getValue();
            result.append(RubyArray.newArray(getRuntime(), key, value));
        }
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

    public RubyHash rehash() {
        modify();
        try {
            isRehashing = true;
            valueMap = new HashMap(valueMap);
        } finally {
            isRehashing = false;
        }
        return this;
    }

    public RubyHash to_hash() {
        return this;
    }

    public IRubyObject aset(IRubyObject key, IRubyObject value) {
        modify();

        if (!(key instanceof RubyString) || valueMap.get(key) != null) {
            valueMap.put(key, value);
        } else {
            IRubyObject realKey = key.dup();
            realKey.setFrozen(true);
            valueMap.put(realKey, value);
        }
        return value;
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
            checkRehashing();
            Map.Entry entry = (Map.Entry) iter.next();
			runtime.yield(RubyArray.newArray(runtime, (IRubyObject)entry.getKey(), (IRubyObject)entry.getValue()));
        }
        return this;
    }

    private void checkRehashing() {
        if (isRehashing) {
            throw new IndexError(getRuntime(), "rehash occured during iteration");
        }
    }

    public RubyHash each_value() {
		Iterator iter = valueIterator();
		while (iter.hasNext()) {
            checkRehashing();
			IRubyObject value = (IRubyObject) iter.next();
			runtime.yield(value);
		}
		return this;
	}

	public RubyHash each_key() {
		Iterator iter = keyIterator();
		while (iter.hasNext()) {
			checkRehashing();
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

    public IRubyObject index(IRubyObject value) {
        Iterator iter = valueMap.keySet().iterator();
        while (iter.hasNext()) {
            Object key = iter.next();
            if (value.equals(valueMap.get(key))) {
                return (IRubyObject) key;
            }
        }
        return getDefaultValue();
    }

    public RubyArray indices(IRubyObject[] indices) {
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

        Iterator iter = modifiableEntryIterator();
        while (iter.hasNext()) {
            checkRehashing();
            Map.Entry entry = (Map.Entry) iter.next();

            Object value = ((RubyHash)other).valueMap.get(entry.getKey());
            if (value == null || !entry.getValue().equals(value)) {
                return runtime.getFalse();
            }
        }
        return runtime.getTrue();
    }

    public RubyArray shift() {
		modify();
        Iterator iter = modifiableEntryIterator();
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
		Iterator iter = modifiableEntryIterator();
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
