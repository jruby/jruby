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

import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.FrozenError;
import org.jruby.exceptions.IndexError;
import org.jruby.exceptions.SecurityError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
        this.defaultProc = ruby.getNil();
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

    public static RubyClass createHashClass(Ruby ruby) {
        RubyClass hashClass = ruby.defineClass("Hash", ruby.getClasses().getObjectClass());
        hashClass.includeModule(ruby.getClasses().getEnumerableModule());

        CallbackFactory callbackFactory = ruby.callbackFactory();

        hashClass.defineSingletonMethod("new", callbackFactory.getOptSingletonMethod(RubyHash.class, "newInstance"));
        hashClass.defineSingletonMethod("[]", callbackFactory.getOptSingletonMethod(RubyHash.class, "create"));
        hashClass.defineMethod("initialize", callbackFactory.getOptMethod(RubyHash.class, "initialize"));
		hashClass.defineMethod("clone", callbackFactory.getMethod(RubyHash.class, "rbClone"));
        hashClass.defineMethod("default_proc", callbackFactory.getMethod(RubyHash.class, "default_proc")); 
        hashClass.defineMethod("rehash", callbackFactory.getMethod(RubyHash.class, "rehash"));
        hashClass.defineMethod("to_hash", callbackFactory.getMethod(RubyHash.class, "to_hash"));
        hashClass.defineMethod("to_a", callbackFactory.getMethod(RubyHash.class, "to_a"));
        hashClass.defineMethod("to_s", callbackFactory.getMethod(RubyHash.class, "to_s"));
        hashClass.defineMethod("inspect", callbackFactory.getMethod(RubyHash.class, "inspect"));

        hashClass.defineMethod("==", callbackFactory.getMethod(RubyHash.class, "equal", IRubyObject.class));
        hashClass.defineMethod("[]", callbackFactory.getMethod(RubyHash.class, "aref", IRubyObject.class));
        hashClass.defineMethod("fetch", callbackFactory.getOptMethod(RubyHash.class, "fetch"));
        hashClass.defineMethod("[]=", callbackFactory.getMethod(RubyHash.class, "aset", IRubyObject.class, IRubyObject.class));
        hashClass.defineMethod("store", callbackFactory.getMethod(RubyHash.class, "aset", IRubyObject.class, IRubyObject.class));
		hashClass.defineMethod("default", callbackFactory.getMethod(RubyHash.class, "getDefaultValue"));
		hashClass.defineMethod("default=", callbackFactory.getMethod(RubyHash.class, "setDefaultValue", IRubyObject.class));
        hashClass.defineMethod("index", callbackFactory.getMethod(RubyHash.class, "index", IRubyObject.class));
        hashClass.defineMethod("indexes", callbackFactory.getOptMethod(RubyHash.class, "indices"));
        hashClass.defineMethod("indices", callbackFactory.getOptMethod(RubyHash.class, "indices"));
        hashClass.defineMethod("size", callbackFactory.getMethod(RubyHash.class, "size"));
        hashClass.defineMethod("length", callbackFactory.getMethod(RubyHash.class, "size"));
        hashClass.defineMethod("empty?", callbackFactory.getMethod(RubyHash.class, "empty_p"));
		hashClass.defineMethod("each", callbackFactory.getMethod(RubyHash.class, "each"));
		hashClass.defineMethod("each_pair", callbackFactory.getMethod(RubyHash.class, "each"));
		hashClass.defineMethod("each_value", callbackFactory.getMethod(RubyHash.class, "each_value"));
		hashClass.defineMethod("each_key", callbackFactory.getMethod(RubyHash.class, "each_key"));
		hashClass.defineMethod("sort", callbackFactory.getMethod(RubyHash.class, "sort"));
		hashClass.defineMethod("keys", callbackFactory.getMethod(RubyHash.class, "keys"));
		hashClass.defineMethod("values", callbackFactory.getMethod(RubyHash.class, "values"));

		hashClass.defineMethod("shift", callbackFactory.getMethod(RubyHash.class, "shift"));
		hashClass.defineMethod("delete", callbackFactory.getMethod(RubyHash.class, "delete", IRubyObject.class));
		hashClass.defineMethod("delete_if", callbackFactory.getMethod(RubyHash.class, "delete_if"));
		hashClass.defineMethod("reject", callbackFactory.getMethod(RubyHash.class, "reject"));
		hashClass.defineMethod("reject!", callbackFactory.getMethod(RubyHash.class, "reject_bang"));
		hashClass.defineMethod("clear", callbackFactory.getMethod(RubyHash.class, "clear"));
		hashClass.defineMethod("invert", callbackFactory.getMethod(RubyHash.class, "invert"));
        hashClass.defineMethod("update", callbackFactory.getMethod(RubyHash.class, "update", IRubyObject.class));
        hashClass.defineMethod("replace", callbackFactory.getMethod(RubyHash.class, "replace", IRubyObject.class));
        hashClass.defineMethod("include?", callbackFactory.getMethod(RubyHash.class, "has_key", IRubyObject.class));
        hashClass.defineMethod("member?", callbackFactory.getMethod(RubyHash.class, "has_key", IRubyObject.class));
        hashClass.defineMethod("has_key?", callbackFactory.getMethod(RubyHash.class, "has_key", IRubyObject.class));
        hashClass.defineMethod("has_value?", callbackFactory.getMethod(RubyHash.class, "has_value", IRubyObject.class));
        hashClass.defineMethod("key?", callbackFactory.getMethod(RubyHash.class, "has_key", IRubyObject.class));
        hashClass.defineMethod("value?", callbackFactory.getMethod(RubyHash.class, "has_value", IRubyObject.class));
        hashClass.defineMethod("values_at", callbackFactory.getOptMethod(RubyHash.class, "values_at"));

        hashClass.defineMethod("merge", callbackFactory.getMethod(RubyHash.class, "merge", IRubyObject.class));
        hashClass.defineAlias("merge!", "update");
        
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

    private int length() {
        return valueMap.size();
    }

    // Hash methods

    public static RubyHash newHash(Ruby ruby) {
    	return new RubyHash(ruby);
    }

	public static RubyHash newHash(Ruby ruby, Map valueMap, IRubyObject defaultValue) {
		return new RubyHash(ruby, valueMap, defaultValue);
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
            throw new ArgumentError(recv.getRuntime(), "odd number of args for Hash");
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
        
        for(Iterator iter = valueMap.entrySet().iterator(); iter.hasNext();) {
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

        if (value != null) {
        	return value;
        }
        
        if (defaultProc.isNil() == false) {
        	IRubyObject args[] = {this, key};
        	return ((RubyProc) defaultProc).call(args, this);
        } 

        return getDefaultValue(); 
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
            } 

            throw new IndexError(runtime, "key not found");
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
        for (Iterator iter = entryIterator(); iter.hasNext();) {
            checkRehashing();
            Map.Entry entry = (Map.Entry) iter.next();
			runtime.yield(RubyArray.newArray(runtime, (IRubyObject)entry.getKey(), (IRubyObject)entry.getValue()), null, null, true);
        }
        return this;
    }

    private void checkRehashing() {
        if (isRehashing) {
            throw new IndexError(getRuntime(), "rehash occured during iteration");
        }
    }

    public RubyHash each_value() {
		for (Iterator iter = valueIterator(); iter.hasNext();) {
            checkRehashing();
			IRubyObject value = (IRubyObject) iter.next();
			runtime.yield(value);
		}
		return this;
	}

	public RubyHash each_key() {
		for (Iterator iter = keyIterator(); iter.hasNext();) {
			checkRehashing();
            IRubyObject key = (IRubyObject) iter.next();
			runtime.yield(key);
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
        return runtime.getNil();
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

        for (Iterator iter = modifiableEntryIterator(); iter.hasNext();) {
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
			IRubyObject shouldDelete = runtime.yield(RubyArray.newArray(runtime, key, value), null, null, true);
			if (shouldDelete.isTrue()) {
				valueMap.remove(key);
				isModified = true;
			}
		}

		return isModified ? this : nilHash(runtime); 
	}

	public RubyHash clear() {
		modify();
		valueMap.clear();
		return this;
	}

	public RubyHash invert() {
		RubyHash result = newHash(runtime);
		
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
        RubyArray result = RubyArray.newArray(runtime);
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
