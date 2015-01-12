
package org.jruby.util;

import java.util.AbstractSet;
import java.util.Set;
import java.util.Iterator;

public class WeakIdentityLinkedHashSet extends AbstractSet implements Set {

    private transient WeakIdentityLinkedHashMap map;

    // Dummy value to associate with an Object in the backing Map
    private static final Object PRESENT = new Object();

    public WeakIdentityLinkedHashSet() {
        map = new WeakIdentityLinkedHashMap();
    }

    public WeakIdentityLinkedHashSet(int size) {
        map = new WeakIdentityLinkedHashMap(size);
    }

    final class EntryIterator implements Iterator {
        private Iterator iter;

        EntryIterator() {
            iter = map.entryIterator();
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        public Object next() {
            return ((WeakIdentityLinkedHashMap.Entry) iter.next()).getKey();
        }

        public void remove() {
            iter.remove();
        }
    }


    public Iterator iterator() {
        return new EntryIterator();
    }

    public void clear() {
        map.clear();
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public boolean add(Object key) {
        return map.put(key, PRESENT) == null;
    }

    public boolean remove(Object key) {
        return map.remove(key) == PRESENT;
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.size() == 0;
    }
}
