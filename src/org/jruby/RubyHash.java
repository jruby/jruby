/*
 * RubyHash.java - No description
 * Created on 04. Juli 2001, 22:53
 *
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Copyright (C) 2004 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Thomas E Enebo <enebo@acm.org>
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jruby.exceptions.SecurityError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;

/** Implementation of the Hash class.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyHash extends RubyObject {
    private Map valueMap;
    
    // Value returned by various functions if no such hash element exists
    private IRubyObject defaultValue;
    
    // Proc which gets run by various function if no such hash element exists.
    // Sometimes this proc will be run even if a default_value exists.
    private IRubyObject defaultProc;

    private boolean isRehashing = false;

    public RubyHash(Ruby runtime) {
        this(runtime, runtime.getNil());
    }

    public RubyHash(Ruby runtime, IRubyObject defaultValue) {
        this(runtime, new HashMap(), defaultValue);
    }

    public RubyHash(Ruby runtime, Map valueMap, IRubyObject defaultValue) {
        super(runtime, runtime.getClass("Hash"));
        this.valueMap = new HashMap(valueMap);
        this.defaultValue = defaultValue;
        this.defaultProc = runtime.getNil();
    }

    public static RubyHash nilHash(Ruby runtime) {
        return new RubyHash(runtime) {
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
        RubyClass hashClass = runtime.defineClass("Hash", runtime.getClasses().getObjectClass());
        hashClass.includeModule(runtime.getClasses().getEnumerableModule());

        CallbackFactory callbackFactory = runtime.callbackFactory(RubyHash.class);

        hashClass.defineSingletonMethod("new", callbackFactory.getOptSingletonMethod("newInstance"));
        hashClass.defineSingletonMethod("[]", callbackFactory.getOptSingletonMethod("create"));
        hashClass.defineMethod("initialize", callbackFactory.getOptMethod("initialize"));
		hashClass.defineMethod("clone", callbackFactory.getMethod("rbClone"));
        hashClass.defineMethod("default_proc", callbackFactory.getMethod("default_proc")); 
        hashClass.defineMethod("rehash", callbackFactory.getMethod("rehash"));
        hashClass.defineMethod("to_hash", callbackFactory.getMethod("to_hash"));
        hashClass.defineMethod("to_a", callbackFactory.getMethod("to_a"));
        hashClass.defineMethod("to_s", callbackFactory.getMethod("to_s"));
        hashClass.defineMethod("inspect", callbackFactory.getMethod("inspect"));

        hashClass.defineMethod("==", callbackFactory.getMethod("equal", IRubyObject.class));
        hashClass.defineMethod("[]", callbackFactory.getMethod("aref", IRubyObject.class));
        hashClass.defineMethod("fetch", callbackFactory.getOptMethod("fetch"));
        hashClass.defineMethod("[]=", callbackFactory.getMethod("aset", IRubyObject.class, IRubyObject.class));
        hashClass.defineMethod("store", callbackFactory.getMethod("aset", IRubyObject.class, IRubyObject.class));
		hashClass.defineMethod("default", callbackFactory.getMethod("getDefaultValue"));
		hashClass.defineMethod("default=", callbackFactory.getMethod("setDefaultValue", IRubyObject.class));
        hashClass.defineMethod("index", callbackFactory.getMethod("index", IRubyObject.class));
        hashClass.defineMethod("indexes", callbackFactory.getOptMethod("indices"));
        hashClass.defineMethod("indices", callbackFactory.getOptMethod("indices"));
        hashClass.defineMethod("size", callbackFactory.getMethod("size"));
        hashClass.defineMethod("length", callbackFactory.getMethod("size"));
        hashClass.defineMethod("empty?", callbackFactory.getMethod("empty_p"));
		hashClass.defineMethod("each", callbackFactory.getMethod("each"));
		hashClass.defineMethod("each_pair", callbackFactory.getMethod("each"));
		hashClass.defineMethod("each_value", callbackFactory.getMethod("each_value"));
		hashClass.defineMethod("each_key", callbackFactory.getMethod("each_key"));
		hashClass.defineMethod("sort", callbackFactory.getMethod("sort"));
		hashClass.defineMethod("keys", callbackFactory.getMethod("keys"));
		hashClass.defineMethod("values", callbackFactory.getMethod("values"));

		hashClass.defineMethod("shift", callbackFactory.getMethod("shift"));
		hashClass.defineMethod("delete", callbackFactory.getMethod("delete", IRubyObject.class));
		hashClass.defineMethod("delete_if", callbackFactory.getMethod("delete_if"));
		hashClass.defineMethod("reject", callbackFactory.getMethod("reject"));
		hashClass.defineMethod("reject!", callbackFactory.getMethod("reject_bang"));
		hashClass.defineMethod("clear", callbackFactory.getMethod("clear"));
		hashClass.defineMethod("invert", callbackFactory.getMethod("invert"));
        hashClass.defineMethod("update", callbackFactory.getMethod("update", IRubyObject.class));
        hashClass.defineMethod("replace", callbackFactory.getMethod("replace", IRubyObject.class));
        hashClass.defineMethod("include?", callbackFactory.getMethod("has_key", IRubyObject.class));
        hashClass.defineMethod("member?", callbackFactory.getMethod("has_key", IRubyObject.class));
        hashClass.defineMethod("has_key?", callbackFactory.getMethod("has_key", IRubyObject.class));
        hashClass.defineMethod("has_value?", callbackFactory.getMethod("has_value", IRubyObject.class));
        hashClass.defineMethod("key?", callbackFactory.getMethod("has_key", IRubyObject.class));
        hashClass.defineMethod("value?", callbackFactory.getMethod("has_value", IRubyObject.class));
        hashClass.defineMethod("values_at", callbackFactory.getOptMethod("values_at"));

        hashClass.defineMethod("merge", callbackFactory.getMethod("merge", IRubyObject.class));
        hashClass.defineAlias("merge!", "update");
        
        return hashClass;
    }

    /** rb_hash_modify
     *
     */
    public void modify() {
    	testFrozen("Hash");
        if (isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw new SecurityError(getRuntime(), "Insecure: can't modify hash");
        }
    }

    private int length() {
        return valueMap.size();
    }

    // Hash methods

    public static RubyHash newHash(Ruby runtime) {
    	return new RubyHash(runtime);
    }

	public static RubyHash newHash(Ruby runtime, Map valueMap, IRubyObject defaultValue) {
		return new RubyHash(runtime, valueMap, defaultValue);
	}

    public static RubyHash newInstance(IRubyObject recv, IRubyObject[] args) {
        RubyHash hash = new RubyHash(recv.getRuntime());

        // A block to represent 'default' value for unknown values
        if (recv.getRuntime().isBlockGiven()) {
        	hash.defaultProc = RubyProc.newProc(recv.getRuntime());
        }
        
        hash.setMetaClass((RubyClass) recv);
        hash.callInit(args);
        return hash;
    }

    public static RubyHash create(IRubyObject recv, IRubyObject[] args) {
        RubyHash hash = new RubyHash(recv.getRuntime());
        if (args.length == 1) {
            hash.setValueMap(new HashMap(((RubyHash) args[0]).getValueMap()));
        } else if (args.length % 2 != 0) {
            throw recv.getRuntime().newArgumentError("odd number of args for Hash");
        } else {
            for (int i = 0; i < args.length; i += 2) {
                hash.aset(args[i], args[i + 1]);
            }
        }
        return hash;
    }

    public IRubyObject initialize(IRubyObject[] args) {
        if (args.length > 0) {
            modify();

            setDefaultValue(args[0]);
        }
        return this;
    }
    
    public IRubyObject default_proc() {
    	return defaultProc;
    }

    public RubyString inspect() {
        final String sep = ", ";
        final String arrow = "=>";
        final StringBuffer sb = new StringBuffer("{");
        boolean firstEntry = true;
        
        for (Iterator iter = valueMap.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iter.next();
            IRubyObject key = (IRubyObject) entry.getKey();
            IRubyObject value = (IRubyObject) entry.getValue();
            if (!firstEntry) {
                sb.append(sep);
            }
            sb.append(key.callMethod("inspect")).append(arrow);
            sb.append(value.callMethod("inspect"));
            firstEntry = false;
        }
        sb.append("}");
        return getRuntime().newString(sb.toString());
    }

    public RubyFixnum size() {
        return getRuntime().newFixnum(length());
    }

    public RubyBoolean empty_p() {
        return length() == 0 ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public RubyArray to_a() {
        RubyArray result = getRuntime().newArray(length());
        
        for(Iterator iter = valueMap.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            IRubyObject key = (IRubyObject) entry.getKey();
            IRubyObject value = (IRubyObject) entry.getValue();
            result.append(getRuntime().newArray(key, value));
        }
        return result;
    }

    public RubyString to_s() {
        return to_a().to_s();
    }

	public IRubyObject rbClone() {
		RubyHash result = newHash(getRuntime(), getValueMap(), getDefaultValue());
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

        if (value != null) {
        	return value;
        }
        
        if (!defaultProc.isNil()) {
        	IRubyObject[] args = {this, key};
        	return ((RubyProc) defaultProc).call(args, this);
        } 

        return getDefaultValue(); 
    }

    public IRubyObject fetch(IRubyObject[] args) {
        if (args.length < 1) {
            throw getRuntime().newArgumentError(args.length, 1);
        }
        IRubyObject key = args[0];
        IRubyObject result = (IRubyObject) valueMap.get(key);
        if (result == null) {
            if (args.length > 1) {
                return args[1];
            } else if (getRuntime().isBlockGiven()) {
                return getRuntime().yield(key);
            } 

            throw getRuntime().newIndexError("key not found");
        }
        return result;
    }


    public RubyBoolean has_key(IRubyObject key) {
        return getRuntime().newBoolean(valueMap.containsKey(key));
    }

    public RubyBoolean has_value(IRubyObject value) {
        return getRuntime().newBoolean(valueMap.containsValue(value));
    }

    public RubyHash each() {
        for (Iterator iter = entryIterator(); iter.hasNext();) {
            checkRehashing();
            Map.Entry entry = (Map.Entry) iter.next();
			getRuntime().yield(getRuntime().newArray((IRubyObject)entry.getKey(), (IRubyObject)entry.getValue()), null, null, true);
        }
        return this;
    }

    private void checkRehashing() {
        if (isRehashing) {
            throw getRuntime().newIndexError("rehash occured during iteration");
        }
    }

    public RubyHash each_value() {
		for (Iterator iter = valueIterator(); iter.hasNext();) {
            checkRehashing();
			IRubyObject value = (IRubyObject) iter.next();
			getRuntime().yield(value);
		}
		return this;
	}

	public RubyHash each_key() {
		for (Iterator iter = keyIterator(); iter.hasNext();) {
			checkRehashing();
            IRubyObject key = (IRubyObject) iter.next();
			getRuntime().yield(key);
		}
		return this;
	}

	public RubyArray sort() {
		return (RubyArray) to_a().sort_bang();
	}

    public IRubyObject index(IRubyObject value) {
        for (Iterator iter = valueMap.keySet().iterator(); iter.hasNext(); ) {
            Object key = iter.next();
            if (value.equals(valueMap.get(key))) {
                return (IRubyObject) key;
            }
        }
        return getRuntime().getNil();
    }

    public RubyArray indices(IRubyObject[] indices) {
        ArrayList values = new ArrayList(indices.length);

        for (int i = 0; i < indices.length; i++) {
            values.add(aref(indices[i]));
        }

        return getRuntime().newArray(values);
    }

    public RubyArray keys() {
        return getRuntime().newArray(new ArrayList(valueMap.keySet()));
    }

    public RubyArray values() {
        return getRuntime().newArray(new ArrayList(valueMap.values()));
    }

    public IRubyObject equal(IRubyObject other) {
        if (this == other) {
            return getRuntime().getTrue();
        } else if (!(other instanceof RubyHash)) {
            return getRuntime().getFalse();
        } else if (length() != ((RubyHash)other).length()) {
            return getRuntime().getFalse();
        }

        for (Iterator iter = modifiableEntryIterator(); iter.hasNext();) {
            checkRehashing();
            Map.Entry entry = (Map.Entry) iter.next();

            Object value = ((RubyHash)other).valueMap.get(entry.getKey());
            if (value == null || !entry.getValue().equals(value)) {
                return getRuntime().getFalse();
            }
        }
        return getRuntime().getTrue();
    }

    public RubyArray shift() {
		modify();
        Iterator iter = modifiableEntryIterator();
        Map.Entry entry = (Map.Entry)iter.next();
        iter.remove();
		return getRuntime().newArray((IRubyObject)entry.getKey(), (IRubyObject)entry.getValue());
    }

	public IRubyObject delete(IRubyObject key) {
		modify();
		IRubyObject result = (IRubyObject) valueMap.remove(key);
		if (result != null) {
			return result;
		} else if (getRuntime().isBlockGiven()) {
			return getRuntime().yield(key);
		} 

		return getDefaultValue();
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
		for (Iterator iter = keyIterator(); iter.hasNext();) {
			IRubyObject key = (IRubyObject) iter.next();
			IRubyObject value = (IRubyObject) valueMap.get(key);
			IRubyObject shouldDelete = getRuntime().yield(getRuntime().newArray(key, value), null, null, true);
			if (shouldDelete.isTrue()) {
				valueMap.remove(key);
				isModified = true;
			}
		}

		return isModified ? this : nilHash(getRuntime()); 
	}

	public RubyHash clear() {
		modify();
		valueMap.clear();
		return this;
	}

	public RubyHash invert() {
		RubyHash result = newHash(getRuntime());
		
		for (Iterator iter = modifiableEntryIterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			result.aset((IRubyObject) entry.getValue(), 
					(IRubyObject) entry.getKey());
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
    
    public RubyHash merge(IRubyObject freshElements) {
        return ((RubyHash) dup()).update(freshElements);
    }

    public RubyHash replace(IRubyObject replacement) {
        modify();
        RubyHash replacementHash =
            (RubyHash) replacement.convertType(RubyHash.class, "Hash", "to_hash");
        valueMap.clear();
        valueMap.putAll(replacementHash.valueMap);
        return this;
    }

    public RubyArray values_at(IRubyObject[] argv) {
        RubyArray result = getRuntime().newArray();
        for (int i = 0; i < argv.length; i++) {
            IRubyObject key = argv[i];
            result.append(aref(key));
        }
        return result;
    }

	public void marshalTo(MarshalStream output) throws java.io.IOException {
		output.write('{');
		output.dumpInt(getValueMap().size());
		
		for (Iterator iter = entryIterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			
			output.dumpObject((IRubyObject) entry.getKey());
			output.dumpObject((IRubyObject) entry.getValue());
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
