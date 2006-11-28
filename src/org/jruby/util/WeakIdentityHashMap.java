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
 * Copyright (C) 2006 Kresten Krab Thorup <krab@gnu.org>
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

package org.jruby.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Class <code>WeakIdentityHashMap</code> is a hash map that hashes
 * objects based on System.identityHashMap, and holds weakly onto the
 * key.  This fails if values make reference to the keys!
 * 
 * @author <a href="mailto:krab@trifork.com">Kresten Krab Thorup </a>
 * @version 2.0
 * @since 1.0
 */
public class WeakIdentityHashMap extends GenericMap implements Map {

    private static final float DEFAULT_RATIO = 0.75F;

    private static final Object NULL_KEY = new Object();

    private ReferenceQueue queue = new ReferenceQueue();

    private static Object unmaskKey(Object key) {
        if (key == NULL_KEY) {
            return null;
        } else {
            return key;
        }
    }

    private Object maskKey(Object key) {
        if (key == null) {
            return NULL_KEY;
        } else {
            return key;
        }
    }

    class Entry extends WeakReference implements Map.Entry {
        Entry next;

        int key_hash;

        Object value;

        public int hashCode() {
            return key_hash ^ valueHash(getValue());
        }

        public boolean equals(Object other) {
            if (other instanceof Map.Entry) {
                Map.Entry ent = (Map.Entry) other;
                return getKey() == ent.getKey()
                        && valueEquals(getValue(), ent.getValue());
            } else {
                return false;
            }
        }

        Entry(int key_hash, Object masked_key, Object value, Entry next, ReferenceQueue q) {
            super(masked_key, q);
            this.key_hash = key_hash;
            this.value = value;
            this.next = next;
        }

        Object getMaskedKey() {
            return super.get();
        }
        
        public Object getKey() {
            return unmaskKey(getMaskedKey());
        }

        public Object getValue() {
            return value;
        }

        public Object setValue(Object value) {
            Object result = this.value;
            this.value = value;
            return result;
        }

        boolean sameKey(int hash, Object masked_key) {
            return getMaskedKey() == masked_key;
        }
    }

    /** the hash index */
    private Entry[] table;

    /** the current range for table. */
    private int range;

    private float ratio;

    /** translate hash code bucket to index */
    private int index(int hash) {
        return (hash & 0x7ffffff) % range;
    }

    /** the default and only constructor */
    public WeakIdentityHashMap() {
        clear();
    }

    public WeakIdentityHashMap(int size) {
        clear(Math.max(3, Math.round(size/DEFAULT_RATIO)));
    }

    public void clear() {
        clear(3);
    }
    
    void clear(int size) {
        range = size;
        this.size = 0;
        ratio = DEFAULT_RATIO;
        table = new Entry[range];
    }

    private void expunge() {
        Entry e;
        while ((e = (Entry) queue.poll()) != null) {
            removeEntry(e);
        }
    }

    /** return the element with the given key */
    public Object get(Object key) {
        Object masked_key = maskKey(key);
        int hash = keyHash(masked_key);
        return get(hash, masked_key);
    }

    private Object get(int hash, Object masked_key) {
        int idx = index(hash);

        for (Entry ent = table[idx]; ent != null; ent = ent.next) {
            if (ent.sameKey(hash, masked_key))
                return ent.value;
        }

        return null;
    }

    /** return the element with the given key */
    public boolean containsKey(Object key) {
        Object masked_key = maskKey(key);
        int hash = keyHash(masked_key);
        return containsKey(hash, masked_key);
    }

    private boolean containsKey(int hash, Object masked_key) {
        int idx = index(hash);

        for (Entry ent = table[idx]; ent != null; ent = ent.next) {
            if (ent.sameKey(hash, masked_key))
                return true;
        }

        return false;
    }

    public Object put(Object key, Object value) {
        Object masked_key = maskKey(key);
        int hash = keyHash(masked_key);
        return put(hash, masked_key, value);
    }

    private Object put(int hash, Object masked_key, Object value) {
        int idx = index(hash);

        for (Entry ent = table[idx]; ent != null; ent = ent.next) {
            if (ent.sameKey(hash, masked_key)) {
                return ent.setValue(value);
            }
        }

        expunge();

        if (1.0F * size / range > ratio) {
            grow();
            idx = index(hash);
        }

        table[idx] = new Entry(hash, masked_key, value, table[idx], queue);

        size += 1;

        return null;
    }

    public Object remove(Object key) {
        key = maskKey(key);
        int hash = keyHash(key);
        return remove(hash, key);
    }

    public Object remove(int hash, Object key) {
        key = maskKey(key);
        int idx = index(hash);

        Entry entry = table[idx];
        if (entry != null) {

            if (entry.sameKey(hash, key)) {
                table[idx] = entry.next;
                size -= 1;
                return entry.getValue();

            } else {
                Entry ahead = entry.next;

                while (ahead != null) {
                    if (ahead.sameKey(hash, key)) {
                        entry.next = ahead.next;
                        size -= 1;
                        return ahead.getValue();
                    }

                    entry = ahead;
                    ahead = ahead.next;
                }
            }
        }

        // it was not found at all!
        return null;
    }

    private void removeEntry(Entry ent) {
        int idx = index(ent.key_hash);

        Entry entry = table[idx];
        if (entry != null) {

            if (entry == ent) {
                table[idx] = entry.next;
                size -= 1;
                return;

            } else {
                Entry ahead = entry.next;

                while (ahead != null) {
                    if (ahead == ent) {
                        entry.next = ahead.next;
                        size -= 1;
                        return;
                    }

                    entry = ahead;
                    ahead = ahead.next;
                }
            }
        }
        
        valueRemoved(ent.value);
    }

    // can be overridden to be informed when objects are removed
    protected void valueRemoved(Object value) {
	}

	private void grow() {
        int old_range = range;
        Entry[] old_table = table;

        range = old_range * 2 + 1;
        table = new Entry[range];

        for (int i = 0; i < old_range; i++) {
            Entry entry = old_table[i];

            while (entry != null) {
                Entry ahead = entry.next;
                int idx = index(entry.key_hash);
                entry.next = table[idx];
                table[idx] = entry;
                entry = ahead;
            }
        }
    }

    final class EntryIterator implements Iterator {
        int idx;

        Entry entry;

        EntryIterator() {
            idx = 0;
            entry = table[0];
            locateNext();
        }

        private void locateNext() {
            // we reached the end of a list
            while (entry == null) {
                // goto next bucket
                idx += 1;
                if (idx == range) {
                    // we reached the end
                    return;
                }

                // entry is the first element of this bucket
                entry = table[idx];
            }
        }

        public boolean hasNext() {
            return (entry != null);
        }

        public Object next() {
            Object result = entry;

            if (result == null) {
                throw new NoSuchElementException();
            } else {
                entry = entry.next;
                locateNext();
                return result;
            }
        }

        public void remove() {
            Entry remove = entry;

            entry = entry.next;
            locateNext();

            WeakIdentityHashMap.this.removeEntry(remove);
        }
    }

    protected Iterator entryIterator() {
        return new EntryIterator();
    }

    protected final int keyHash(Object key) {
        return System.identityHashCode(key);
    }

    protected final boolean keyEquals(Object key1, Object key2) {
        return key1 == key2;
    }

    public int size() {
        expunge();
        return super.size();
    }
    
    public boolean isEmpty() {
        return size() == 0;
    }
}