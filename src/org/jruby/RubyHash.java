/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2006 Ola Bini <Ola.Bini@ki.se>
 * Copyright (C) 2006 Tim Azzopardi <tim@tigerfive.com>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.Callback;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;

/** Implementation of the Hash class.
 *
 * @author  jpetersen
 */
public class RubyHash extends RubyObject implements Map {
    private Map valueMap;
    // Place we capture any explicitly set proc so we can return it for default_proc
    private IRubyObject capturedDefaultProc;
    
    // Holds either default value or default proc.  Executing whatever is here will return the
    // correct default value.
    private Callback defaultValueCallback;
    
    private boolean isRehashing = false;

    public RubyHash(IRuby runtime) {
        this(runtime, runtime.getNil());
    }

    public RubyHash(IRuby runtime, IRubyObject defaultValue) {
        super(runtime, runtime.getClass("Hash"));
        this.valueMap = new HashMap();
        this.capturedDefaultProc = runtime.getNil();
        setDefaultValue(defaultValue);
    }

    public RubyHash(IRuby runtime, Map valueMap, IRubyObject defaultValue) {
        super(runtime, runtime.getClass("Hash"));
        this.valueMap = new HashMap(valueMap);
        this.capturedDefaultProc = runtime.getNil();
        setDefaultValue(defaultValue);
    }
    
    public IRubyObject getDefaultValue(IRubyObject[] args) {
        if(defaultValueCallback == null || (args.length == 0 && !capturedDefaultProc.isNil())) {
            return getRuntime().getNil();
        }
        return defaultValueCallback.execute(this,args);
    }

    public IRubyObject setDefaultValue(final IRubyObject defaultValue) {
        capturedDefaultProc = getRuntime().getNil();
        defaultValueCallback = new Callback() {
            public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
                return defaultValue;
            }

            public Arity getArity() {
                return Arity.optional();
            }
        };
        
        return defaultValue;
    }

    public void setDefaultProc(final RubyProc newProc) {
        final IRubyObject self = this;
        capturedDefaultProc = newProc;
        defaultValueCallback = new Callback() {
            public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
                IRubyObject[] nargs = args.length == 0 ? new IRubyObject[] { self } :
                     new IRubyObject[] { self, args[0] };

                return newProc.call(nargs);
            }

            public Arity getArity() {
                return Arity.optional();
            }
        };
    }
    
    public IRubyObject default_proc() {
        return capturedDefaultProc;
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

    /** rb_hash_modify
     *
     */
    public void modify() {
    	testFrozen("Hash");
        if (isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw getRuntime().newSecurityError("Insecure: can't modify hash");
        }
    }

    private int length() {
        return valueMap.size();
    }

    // Hash methods

    public static RubyHash newHash(IRuby runtime) {
    	return new RubyHash(runtime);
    }

	public static RubyHash newHash(IRuby runtime, Map valueMap, IRubyObject defaultValue) {
		assert defaultValue != null;
		
		return new RubyHash(runtime, valueMap, defaultValue);
	}

    public IRubyObject initialize(IRubyObject[] args) {
        if (args.length > 0) {
            modify();

            setDefaultValue(args[0]);
        }
        return this;
    }
    
    public IRubyObject inspect() {
        final String sep = ", ";
        final String arrow = "=>";
        final StringBuffer sb = new StringBuffer("{");
        boolean firstEntry = true;
        
        ThreadContext context = getRuntime().getCurrentContext();
        
        for (Iterator iter = valueMap.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iter.next();
            IRubyObject key = (IRubyObject) entry.getKey();
            IRubyObject value = (IRubyObject) entry.getValue();
            if (!firstEntry) {
                sb.append(sep);
            }
            
            sb.append(key.callMethod(context, "inspect")).append(arrow);
            sb.append(value.callMethod(context, "inspect"));
            firstEntry = false;
        }
        sb.append("}");
        return getRuntime().newString(sb.toString());
    }

    public RubyFixnum rb_size() {
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

    public IRubyObject to_s() {
        return to_a().to_s();
    }

	public IRubyObject rbClone() {
		RubyHash result = newHash(getRuntime(), getValueMap(), getDefaultValue(NULL_ARRAY));
		result.setTaint(isTaint());
		result.initCopy(this);
		result.setFrozen(isFrozen());
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

        return value != null ? value : callMethod(getRuntime().getCurrentContext(), "default", new IRubyObject[] {key});
    }

    public IRubyObject fetch(IRubyObject[] args) {
        if (args.length < 1) {
            throw getRuntime().newArgumentError(args.length, 1);
        }
        IRubyObject key = args[0];
        IRubyObject result = (IRubyObject) valueMap.get(key);
        if (result == null) {
            ThreadContext tc = getRuntime().getCurrentContext();
            if (args.length > 1) {
                return args[1];
            } else if (tc.isBlockGiven()) {
                return tc.yield(key);
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
		return eachInternal(false);
	}

	public RubyHash each_pair() {
		return eachInternal(true);
	}

    protected RubyHash eachInternal(boolean aValue) {
        ThreadContext context = getRuntime().getCurrentContext();
        for (Iterator iter = entryIterator(); iter.hasNext();) {
            checkRehashing();
            Map.Entry entry = (Map.Entry) iter.next();
            context.yieldCurrentBlock(getRuntime().newArray((IRubyObject)entry.getKey(), (IRubyObject)entry.getValue()), null, null, aValue);
        }
        return this;
    }

	

    private void checkRehashing() {
        if (isRehashing) {
            throw getRuntime().newIndexError("rehash occured during iteration");
        }
    }

    public RubyHash each_value() {
        ThreadContext context = getRuntime().getCurrentContext();
		for (Iterator iter = valueIterator(); iter.hasNext();) {
            checkRehashing();
			IRubyObject value = (IRubyObject) iter.next();
			context.yield(value);
		}
		return this;
	}

	public RubyHash each_key() {
        ThreadContext context = getRuntime().getCurrentContext();
		for (Iterator iter = keyIterator(); iter.hasNext();) {
			checkRehashing();
            IRubyObject key = (IRubyObject) iter.next();
			context.yield(key);
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

    public RubyArray rb_values() {
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
        ThreadContext tc = getRuntime().getCurrentContext();
		if (result != null) {
			return result;
		} else if (tc.isBlockGiven()) {
			return tc.yield(key);
		} 

		return getDefaultValue(new IRubyObject[] {key});
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

	public IRubyObject reject_bang() {
		modify();
		boolean isModified = false;
        ThreadContext context = getRuntime().getCurrentContext();
		for (Iterator iter = keyIterator(); iter.hasNext();) {
			IRubyObject key = (IRubyObject) iter.next();
			IRubyObject value = (IRubyObject) valueMap.get(key);
			IRubyObject shouldDelete = context.yieldCurrentBlock(getRuntime().newArray(key, value), null, null, true);
			if (shouldDelete.isTrue()) {
				valueMap.remove(key);
				isModified = true;
			}
		}

		return isModified ? this : getRuntime().getNil(); 
	}

	public RubyHash rb_clear() {
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

    // FIXME:  Total hack to get flash in Rails marshalling/unmarshalling in session ok...We need
    // to totally change marshalling to work with overridden core classes.
	public void marshalTo(MarshalStream output) throws IOException {
		output.writeIVar(this, output);
		output.writeUserClass(this, getRuntime().getClass("Hash"), output);
		output.write('{');
		output.dumpInt(getValueMap().size());
		
		for (Iterator iter = entryIterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			
			output.dumpObject((IRubyObject) entry.getKey());
			output.dumpObject((IRubyObject) entry.getValue());
		}
		
    	if (!getMetaClass().equals(getRuntime().getClass("Hash"))) {
    		output.writeInstanceVars(this, output);
    	}
	}

    public static RubyHash unmarshalFrom(UnmarshalStream input) throws IOException {
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

    public Class getJavaClass() {
        return Map.class;
    }
	
    // Satisfy java.util.Set interface (for Java integration)

	public boolean isEmpty() {
		return valueMap.isEmpty();
	}

	public boolean containsKey(Object key) {
		return keySet().contains(key);
	}

	public boolean containsValue(Object value) {
		IRubyObject element = JavaUtil.convertJavaToRuby(getRuntime(), value);
		
		for (Iterator iter = valueMap.values().iterator(); iter.hasNext(); ) {
			if (iter.next().equals(element)) {
				return true;
			}
		}
		return false;
	}

	public Object get(Object key) {
		return JavaUtil.convertRubyToJava((IRubyObject) valueMap.get(JavaUtil.convertJavaToRuby(getRuntime(), key)));
	}

	public Object put(Object key, Object value) {
		return valueMap.put(JavaUtil.convertJavaToRuby(getRuntime(), key),
				JavaUtil.convertJavaToRuby(getRuntime(), value));
	}

	public Object remove(Object key) {
		return valueMap.remove(JavaUtil.convertJavaToRuby(getRuntime(), key));
	}

	public void putAll(Map map) {
		for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
			Object key = iter.next();
			
			put(key, map.get(key));
		}
	}


	public Set entrySet() {
		return new ConversionMapEntrySet(getRuntime(), valueMap.entrySet());
	}

	public int size() {
		return valueMap.size();
	}

	public void clear() {
		valueMap.clear();
	}

	public Collection values() {
		return new AbstractCollection() {
			public Iterator iterator() {
				return new IteratorAdapter(entrySet().iterator()) {
					public Object next() {
						return ((Map.Entry) super.next()).getValue();
					}
				};
			}

			public int size() {
				return RubyHash.this.size();
			}

			public boolean contains(Object v) {
				return RubyHash.this.containsValue(v);
			}
		};
	}
	
	public Set keySet() {
		return new AbstractSet() {
			public Iterator iterator() {
				return new IteratorAdapter(entrySet().iterator()) {
					public Object next() {
						return ((Map.Entry) super.next()).getKey();
					}
				};
			}

			public int size() {
				return RubyHash.this.size();
			}
		};
	}	

	/**
	 * Convenience adaptor for delegating to an Iterator.
	 *
	 */
	private static class IteratorAdapter implements Iterator {
		private Iterator iterator;
		
		public IteratorAdapter(Iterator iterator) {
			this.iterator = iterator;
		}
		public boolean hasNext() {
			return iterator.hasNext();
		}
		public Object next() {
			return iterator.next();
		}
		public void remove() {
			iterator.remove();
		}		
	}
	
	
    /**
     * Wraps a Set of Map.Entry (See #entrySet) such that JRuby types are mapped to Java types and vice verce.
     *
     */
    private static class ConversionMapEntrySet extends AbstractSet {
		protected Set mapEntrySet;
		protected IRuby runtime;

		public ConversionMapEntrySet(IRuby runtime, Set mapEntrySet) {
			this.mapEntrySet = mapEntrySet;
			this.runtime = runtime;
		}
        public Iterator iterator() {
            return new ConversionMapEntryIterator(runtime, mapEntrySet.iterator());
        }
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            return mapEntrySet.contains(getRubifiedMapEntry((Map.Entry) o));
        }
        
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            return mapEntrySet.remove(getRubifiedMapEntry((Map.Entry) o));
        }
		public int size() {
			return mapEntrySet.size();
		}
        public void clear() {
        	mapEntrySet.clear();
        }
		private Entry getRubifiedMapEntry(final Map.Entry mapEntry) {
			return new Map.Entry(){
				public Object getKey() {
					return JavaUtil.convertJavaToRuby(runtime, mapEntry.getKey());
				}
				public Object getValue() {
					return JavaUtil.convertJavaToRuby(runtime, mapEntry.getValue());
				}
				public Object setValue(Object arg0) {
					// This should never get called in this context, but if it did...
					throw new UnsupportedOperationException("unexpected call in this context");
				}
            };
		}
    }    
    
    /**
     * Wraps a RubyHash#entrySet#iterator such that the Map.Entry returned by next() will have its key and value 
     * mapped from JRuby types to Java types where applicable.
     */
    private static class ConversionMapEntryIterator implements Iterator {
        private Iterator iterator;
		private IRuby runtime;

        public ConversionMapEntryIterator(IRuby runtime, Iterator iterator) {
            this.iterator = iterator;
            this.runtime = runtime;            
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public Object next() {
            return new ConversionMapEntry(runtime, ((Map.Entry) iterator.next())); 
        }

        public void remove() {
            iterator.remove();
        }
    }
    
   
    /**
     * Wraps a Map.Entry from RubyHash#entrySet#iterator#next such that the the key and value 
     * are mapped from/to JRuby/Java types where applicable.
     */
    private static class ConversionMapEntry implements Map.Entry {
        private Entry entry;
		private IRuby runtime;

        public ConversionMapEntry(IRuby runtime, Map.Entry entry) {
            this.entry = entry;
            this.runtime = runtime;
        }
        
        public Object getKey() {
            IRubyObject rubyObject = (IRubyObject) entry.getKey();
            return JavaUtil.convertRubyToJava(rubyObject, Object.class); 
        }
        
        public Object getValue() {
            IRubyObject rubyObject = (IRubyObject) entry.getValue();
            return JavaUtil.convertRubyToJava(rubyObject, Object.class); 
        }
        
        public Object setValue(Object value) {
            return entry.setValue(JavaUtil.convertJavaToRuby(runtime, value));            
        }
    }
    
}
