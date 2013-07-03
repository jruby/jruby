/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Kresten Krab Thorup <krab@gnu.org>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.util;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A <code>GenericMap</code> is simply an abstract <code>java.util.Map</code> 
 * implementation for which subclasses really only need to implement 
 * the method entryIterator. 
 * 
 * @author Kresten Krab Thorup (krab@trifork.com)
 */

public abstract class GenericMap implements Map {
    protected int size;

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    protected int keyHash(Object key) {
        return key == null ? 0 : key.hashCode();
    }

    protected boolean keyEquals(Object containedKey, Object givenKey) {
        return containedKey == null ? givenKey == null : containedKey.equals(givenKey);
    }

    protected int valueHash(Object value) {
        return value == null ? 0 : value.hashCode();
    }

    protected boolean valueEquals(Object value1, Object value2) {
        return value1 == null ? value2 == null : value1.equals(value2);
    }

    abstract class Entry implements Map.Entry {
        @Override
        public int hashCode() {
            return keyHash(getKey()) ^ valueHash(getValue());
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Map.Entry) {
                Map.Entry ent = (Map.Entry) other;
                return keyEquals(getKey(), ent.getKey()) && valueEquals(getValue(), ent.getValue());
            } 

            return false;
        }
    }

    public void putAll(Map other) {
        if (other == this) return;

        for (Iterator it = other.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            
            put(entry.getKey(), entry.getValue());
        }
    }

    protected abstract Iterator entryIterator();

    protected Iterator keyIterator() {
        return new KeyIterator();
    }

    protected Iterator valueIterator() {
        return new ValueIterator();
    }

    abstract class KeyOrValueIterator implements Iterator {
        Iterator iter = entryIterator();

        public boolean hasNext() {
            return iter.hasNext();
        }

        protected Map.Entry nextEntry() {
            return (Map.Entry) iter.next();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    class KeyIterator extends KeyOrValueIterator {
        public Object next() {
            return nextEntry().getKey();
        }
    }

    class ValueIterator extends KeyOrValueIterator {
        public Object next() {
            return nextEntry().getValue();
        }
    }

    /**
     * I don't quite understand why we need to replace this method from
     * AbstractCollection, but it has been observed that toArray returns the
     * *reverse* order of elements. --Kresten
     */
    private static Object[] toArray(Object[] arr, int size, Iterator it) {
        Object[] out;

        if (arr != null && arr.length >= size) {
            out = arr;
        } else if (arr == null) {
            out = new Object[size];
        } else {
            out = (Object[]) java.lang.reflect.Array.newInstance(arr.getClass().getComponentType(), size);
        }

        for (int i = 0; i < size; i++) {
            out[i] = it.next();
        }

        if (out.length > size) out[size] = null;

        return out;
    }

    public Collection values() {
        return new AbstractCollection() {
            public Iterator iterator() {
                return valueIterator();
            }

            public int size() {
                return GenericMap.this.size();
            }

            @Override
            public Object[] toArray(Object[] arr) {
                return GenericMap.toArray(arr, size(), iterator());
            }
        };
    }

    public Set keySet() {
        return new AbstractSet() {
            public Iterator iterator() {
                return keyIterator();
            }

            public int size() {
                return GenericMap.this.size();
            }

            @Override
            public Object[] toArray(Object[] arr) {
                return GenericMap.toArray(arr, size(), iterator());
            }
        };
    }

    @Override
    public int hashCode() {
        int code = 0;
        
        for (Iterator it = entryIterator(); it.hasNext(); ) {
            code += it.next().hashCode();
        }

        return code;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Map)) return false;

        Map map = (Map) other;

        if (map.size() != size()) return false;
            
        for (Iterator it = entryIterator(); it.hasNext();) {
            Entry ent = (Entry) it.next();
            Object key = ent.getKey();
            Object val = ent.getValue();

            if (map.containsKey(key)) {
                Object otherVal = map.get(key);
                if (!valueEquals(val, otherVal)) return false;
            }
        }
            
        return true;
    }

    public Set entrySet() {
        return new AbstractSet() {
            public Iterator iterator() {
                return entryIterator();
            }

            public int size() {
                return size;
            }

            @Override
            public Object[] toArray(Object[] arr) {
                return GenericMap.toArray(arr, size(), iterator());
            }
        };
    }

    /** return the element with the given key */
    public boolean containsValue(Object value) {
        for (Iterator it = valueIterator(); it.hasNext(); ) {
            if (valueEquals(value, it.next())) return true;
        }
        
        return false;
    }

    public boolean containsKey(Object key) {
        return get(key) != null;
    }
}
