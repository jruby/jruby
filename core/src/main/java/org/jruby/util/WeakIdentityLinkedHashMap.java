
package org.jruby.util;

import java.lang.ref.ReferenceQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class WeakIdentityLinkedHashMap extends WeakIdentityHashMap {
    public WeakIdentityLinkedHashMap() {
        super();
    }

    public WeakIdentityLinkedHashMap(int size) {
        super(size);
    }

    class Entry extends WeakIdentityHashMap.Entry {
        Entry before, after;

        Entry(int hash, Object masked_key, Object value, WeakIdentityHashMap.Entry next, ReferenceQueue queue, Entry tail) {
            super(hash, masked_key, value, next, queue);
            before = tail;
            if (tail != null) {
                tail.after = this;
            }
        }
    }

    // The head (eldest) of the doubly linked list.
    transient Entry head;

    // The tail (youngest) of the doubly linked list.
    transient Entry tail;

    @Override
    public void clear() {
        head = tail = null;
        super.clear();
    }

    @Override
    protected WeakIdentityHashMap.Entry newEntry(int hash, Object masked_key, Object value, WeakIdentityHashMap.Entry next, ReferenceQueue queue) {
        Entry newTail = new Entry(hash, masked_key, value, next, queue, tail);
        if (head == null) {
            head = newTail;
        }
        tail = newTail;
        return newTail;
    }

    @Override
    protected void entryRemoved(WeakIdentityHashMap.Entry entry) {
        Entry ent = (Entry) entry;
        if (ent.before == null) {
            head = ent.after;
        }
        else {
            ent.before.after = ent.after;
        }
        super.entryRemoved(entry);
    }

    @Override
    protected Iterator entryIterator() {
        return new EntryIterator();
    }

    final class EntryIterator implements Iterator {
        private Entry entry;

        EntryIterator() {
            expunge();
            entry = head;
        }

        public boolean hasNext() {
            return (entry != null);
        }

        public Object next() {
            Object result = entry;

            if (result == null) {
                throw new NoSuchElementException();
            } else {
                entry = entry.after;
                return result;
            }
        }

        public void remove() {
            Entry removed = entry;
            expunge();
            entry = entry.after;
            WeakIdentityLinkedHashMap.this.removeEntry(removed);
        }
    }
}
