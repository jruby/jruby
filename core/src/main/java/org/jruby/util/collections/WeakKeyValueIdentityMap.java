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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Map-like that holds both its keys and its values weakly and compares keys by identity
 * (backed by a concurrent hash map). An entry goes away once its key or its value has been
 * collected; iteration only ever sees entries whose key and value are both still alive.
 * Null keys and null values are not supported.
 * @param <Key> key
 * @param <Value> value
 */
public class WeakKeyValueIdentityMap<Key, Value> extends AbstractMap<Key, Value> {

    private final ConcurrentHashMap<KeyRef<Key>, ValueRef<Key, Value>> map = new ConcurrentHashMap<>();
    private final ReferenceQueue<Object> deadRefs = new ReferenceQueue<>();

    @Override
    public Value get(Object key) {
        expunge();
        ValueRef<Key, Value> ref = map.get(new KeyRef<>(key, null));
        return ref == null ? null : ref.get();
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public Value put(Key key, Value value) {
        expunge();
        KeyRef<Key> keyRef = new KeyRef<>(key, deadRefs);
        ValueRef<Key, Value> prev = map.put(keyRef, new ValueRef<>(value, keyRef, deadRefs));
        return prev == null ? null : prev.get();
    }

    @Override
    public Value remove(Object key) {
        expunge();
        ValueRef<Key, Value> prev = map.remove(new KeyRef<>(key, null));
        return prev == null ? null : prev.get();
    }

    @Override
    public int size() {
        expunge();
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public void clear() {
        map.clear();
        expunge();
    }

    @Override
    public Set<Entry<Key, Value>> entrySet() {
        expunge();
        return new EntrySet();
    }

    // drop the entries whose key or value the collector has cleared; a value's reference only
    // removes the entry it was stored with, so a replaced value never evicts its successor
    @SuppressWarnings("unchecked")
    private void expunge() {
        Object ref;
        while ((ref = deadRefs.poll()) != null) {
            if (ref instanceof ValueRef) {
                ValueRef<Key, Value> valueRef = (ValueRef<Key, Value>) ref;
                map.remove(valueRef.key, valueRef);
            } else if (ref instanceof KeyRef) {
                map.remove(ref);
            }
        }
    }

    // weak reference to a key, hashed and compared by the identity of the key it was created for
    private static final class KeyRef<Key> extends WeakReference<Key> {
        private final int hash;

        KeyRef(Key key, ReferenceQueue<Object> queue) {
            super(key, queue);
            this.hash = System.identityHashCode(key);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) return true;
            if (!(other instanceof KeyRef)) return false;
            Key key = get();
            return key != null && key == ((KeyRef<?>) other).get();
        }
    }

    private static final class ValueRef<Key, Value> extends WeakReference<Value> {
        private final KeyRef<Key> key;

        ValueRef(Value value, KeyRef<Key> key, ReferenceQueue<Object> queue) {
            super(value, queue);
            this.key = key;
        }
    }

    private class EntrySet extends AbstractSet<Entry<Key, Value>> {
        @Override
        public Iterator<Entry<Key, Value>> iterator() {
            return new EntryIterator();
        }

        // the iterator skips collected entries, so the size must not be reported as exact
        @Override
        public Spliterator<Entry<Key, Value>> spliterator() {
            return Spliterators.spliteratorUnknownSize(iterator(), 0);
        }

        @Override
        public int size() {
            return WeakKeyValueIdentityMap.this.size();
        }

        @Override
        public void clear() {
            WeakKeyValueIdentityMap.this.clear();
        }
    }

    // walks the backing map yielding live entries and pruning the collected ones it meets
    private class EntryIterator implements Iterator<Entry<Key, Value>> {
        private final Iterator<Entry<KeyRef<Key>, ValueRef<Key, Value>>> iter = map.entrySet().iterator();
        private Entry<Key, Value> next;

        @Override
        public boolean hasNext() {
            while (next == null && iter.hasNext()) {
                Entry<KeyRef<Key>, ValueRef<Key, Value>> entry = iter.next();
                Key key = entry.getKey().get();
                Value value = entry.getValue().get();
                if (key != null && value != null) {
                    next = new SimpleImmutableEntry<>(key, value);
                } else {
                    map.remove(entry.getKey(), entry.getValue());
                }
            }
            return next != null;
        }

        @Override
        public Entry<Key, Value> next() {
            if (!hasNext()) throw new NoSuchElementException();
            Entry<Key, Value> entry = next;
            next = null;
            return entry;
        }
    }
}
