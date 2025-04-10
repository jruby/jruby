/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2006 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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

package org.jruby.util.collections;

import java.io.Serializable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Map-like that holds its values weakly (backed by a concurrent hash map).
 * @param <Key> key
 * @param <Value> value
 */
public class WeakValuedMap<Key, Value> implements Map<Key, Value>, Serializable {

    private final Map<Key, KeyedReference<Key, Value>> map = newMap();
    private final ReferenceQueue<Value> deadRefs = new ReferenceQueue<>();

    public Value put(Key key, Value value) {
        cleanReferences();
        KeyedReference<Key, Value> prev = map.put(key, new KeyedReference<Key, Value>(value, key, deadRefs));
        return prev == null ? null : prev.get();
    }

    public Value get(Object key) {
        cleanReferences();
        KeyedReference<Key, Value> reference = map.get(key);
        if (reference == null) return null;
        return reference.get();
    }

    public void clear() {
        cleanReferences();
        map.clear();
    }

    public int size() {
        cleanReferences();
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(final Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        cleanReferences();
        for (KeyedReference ref : map.values()) {
            if (value == null) {
                if (ref.get() == null) return true;
            } else {
                if (value.equals(ref.get())) return true;
            }
        }
        return false;
    }

    @Override
    public Value remove(final Object key) {
        cleanReferences();
        KeyedReference<Key, Value> prev = map.remove(key);
        return prev == null ? null : prev.get();
    }

    @Override
    public void putAll(final Map<? extends Key, ? extends Value> m) {
        for (Map.Entry<? extends Key, ? extends Value> e : m.entrySet()) put(e.getKey(), e.getValue());
    }

    @Override
    public Set<Key> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<Value> values() {
        return new AbstractCollection<Value>() {
            public Iterator<Value> iterator() {
                return new Iterator<Value>() {
                    final Iterator<Entry<Key, Value>> i = entrySet().iterator();

                    public boolean hasNext() {
                        return i.hasNext();
                    }

                    public Value next() {
                        return i.next().getValue();
                    }

                    public void remove() {
                        i.remove();
                    }
                };
            }

            public int size() {
                return WeakValuedMap.this.size();
            }

            public boolean contains(Object v) {
                return WeakValuedMap.this.containsValue(v);
            }

            public void clear() {
                WeakValuedMap.this.clear();
            }
        };
    }

    @Override
    public Set<Entry<Key, Value>> entrySet() {
        return new EntrySet();
    }

    @Override
    public String toString() {
        return map.toString();
    }

    /**
     * Construct the backing store map for this WeakValuedMap. It should be capable of safe concurrent read and write.
     *
     * @return the backing store map
     */
    protected Map<Key, KeyedReference<Key, Value>> newMap() {
        return new ConcurrentHashMap<>();
    }

    protected static class KeyedReference<Key, Value> extends WeakReference<Value> {

        protected final Key key;

        public KeyedReference(Value object, Key key, ReferenceQueue<? super Value> queue) {
            super(object, queue);
            this.key = key;
        }

    }

    @SuppressWarnings("unchecked")
    private void cleanReferences() {
        KeyedReference<Key, Value> ref;
        while ( ( ref = (KeyedReference) deadRefs.poll() ) != null ) {
            map.remove( ref.key );
        }
    }

    private class EntrySet extends AbstractCollection<Entry<Key, Value>> implements Set<Entry<Key, Value>> {

        transient Set<Entry<Key, KeyedReference<Key, Value>>> entries;

        private Set<Entry<Key, KeyedReference<Key, Value>>> getEntries() {
            if (entries == null) {
                entries = WeakValuedMap.this.map.entrySet();
            }
            return entries;
        }

        public int size() {
            return WeakValuedMap.this.size();
        }

        public void clear() {
            WeakValuedMap.this.clear();
        }

        public boolean contains(Object o) {
            if (o instanceof Entry) {
                Entry<Key, Value> entry = (Entry) o;
                cleanReferences();
                KeyedReference<Key, Value> reference = map.get(entry.getKey());
                if (reference == null) return false;
                Object value = reference.get();
                return value == null ? entry.getValue() == null : value.equals(entry.getValue());
            }
            return false;
        }

        public Iterator<Entry<Key, Value>> iterator() {
            WeakValuedMap.this.cleanReferences();

            return new Iterator<Entry<Key, Value>>() {
                final Iterator<Entry<Key, KeyedReference<Key, Value>>> i = WeakValuedMap.this.map.entrySet().iterator();

                public boolean hasNext() {
                    return i.hasNext();
                }

                public Entry<Key, Value> next() {
                    Entry<Key, KeyedReference<Key, Value>> e = i.next();
                    return new SimpleImmutableEntry<>(e.getKey(), e.getValue().get());
                }

                public void remove() {
                    i.remove();
                }

            };
        }

        public boolean add(Entry<Key, Value> e) {
            throw new UnsupportedOperationException();
        }

        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

    }

}
