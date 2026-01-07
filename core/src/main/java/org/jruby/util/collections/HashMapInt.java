package org.jruby.util.collections;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.ObjIntConsumer;

public class HashMapInt<V> {

    private Entry<V>[] table;
    private int count;

    transient volatile Set<V> keySet = null;
	transient volatile Collection<Integer> values = null;

    private int threshold;

    private final float loadFactor;
    private final boolean identity;

    public static class Entry<V> {
        final int hash;
        final V key;
        int value;
        Entry<V> next;

        protected Entry(int hash, V key, int value, Entry<V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public V getKey() {
            return key;
        }

        public int getValue() {
            return value;
        }
    }

    public HashMapInt() {
        this(20, 0.75f, false);
    }

    public HashMapInt(boolean identity) {
        this(20, 0.75f, identity);
    }

    public HashMapInt(int initialCapacity, boolean identity) {
        this(initialCapacity, 0.75f, identity);
    }

    @SuppressWarnings("unchecked")
    public HashMapInt(int initialCapacity, float loadFactor, boolean identity) {
        super();
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        }
        if (loadFactor <= 0) {
            throw new IllegalArgumentException("Illegal Load: " + loadFactor);
        }
        if (initialCapacity == 0) initialCapacity = 1;

        this.loadFactor = loadFactor;
        this.identity = identity;
        this.threshold = (int) (initialCapacity * loadFactor);
        this.table = new Entry[initialCapacity];
    }

    public int size() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public boolean contains(int value) {
        Entry<V> tab[] = table;
        for (int i = tab.length; i-- > 0;) {
            for (Entry<V> e = tab[i]; e != null; e = e.next) {
                if (e.value == value) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean containsValue(int value) {
        return contains(value);
    }

    public boolean containsKey(V key) {
        Entry<V>[] tab = table;
        int hash = getHash(key);
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (Entry<V> e = tab[index]; e != null; e = e.next) {
            if (e.hash == hash && keyMatches(key, e)) {
                return true;
            }
        }
        return false;
    }

    public int get(V key) {
        Entry<V>[] tab = table;
        int hash = getHash(key);
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (Entry<V> e = tab[index]; e != null; e = e.next) {
            if (e.hash == hash && keyMatches(key, e)) {
                return e.value;
            }
        }
        return -1;
    }

    private <V> int getHash(V key) {
        return identity ? System.identityHashCode(key) : key.hashCode();
    }

    private <V> boolean keyMatches(V key, Entry<V> e) {
        return identity ? e.key == key : e.key.equals(key);
    }

    protected void rehash() {
        int oldCapacity = table.length;
        Entry<V>[] oldMap = table;

        int newCapacity = oldCapacity * 2 + 1;
        @SuppressWarnings("unchecked")
        Entry<V>[] newMap = new Entry[newCapacity];

        threshold = (int) (newCapacity * loadFactor);
        table = newMap;

        for (int i = oldCapacity; i-- > 0;) {
            for (Entry<V> old = oldMap[i]; old != null;) {
                Entry<V> e = old;
                old = old.next;

                int index = (e.hash & 0x7FFFFFFF) % newCapacity;
                e.next = newMap[index];
                newMap[index] = e;
            }
        }
    }

	Entry<V> getEntry(V key)	{
        Entry<V>[] tab = table;
        int hash = getHash(key);
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for(Entry<V> e = tab[index]; e != null; e = e.next) {
            if (e.hash == hash && keyMatches(key, e)) {
                return e;
            }
        }
        return null;
	}

    public int put(V key, int value) {
        // Makes sure the key is not already in the hashtable.
        Entry<V>[] tab = table;
        int hash = getHash(key);
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (Entry<V> e = tab[index]; e != null; e = e.next) {
            if (e.hash == hash && keyMatches(key, e)) {
                int old = e.value;
                e.value = value;
                return old;
            }
        }

        if (count >= threshold) {
            // Rehash the table if the threshold is exceeded
            rehash();

            tab = table;
            index = (hash & 0x7FFFFFFF) % tab.length;
        }

        // Creates the new entry.
        Entry<V> e = new Entry<V>(hash, key, value, tab[index]);
        tab[index] = e;
        count++;
        return -1;
    }

    public int remove(V key) {
        Entry<V>[] tab = table;
        int hash = getHash(key);
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (Entry<V> e = tab[index], prev = null; e != null; prev = e, e = e.next) {
            if (e.hash == hash && keyMatches(key, e)) {
                if (prev != null) {
                    prev.next = e.next;
                } else {
                    tab[index] = e.next;
                }
                count--;
                return e.value;
            }
        }
        return -1;
    }

    public synchronized void clear() {
        Entry<V>[] tab = table;
        for (int index = tab.length; --index >= 0;) {
            tab[index] = null;
        }
        count = 0;
    }

    public <T> boolean ifPresent(T state, V key, ObjIntConsumer<T> action) {
        Entry<V> entry = getEntry(key);
        if (entry != null) {
            action.accept(state, entry.value);
            return true;
        }
        return false;
    }

    private abstract class HashIterator<T> implements Iterator<T> {
		Entry<V> next; // next entry to return
		int index; // current slot

		HashIterator() {
			Entry<V>[] t = table;
			int i = t.length;
			Entry<V> n = null;
			if(count != 0) { // advance to first entry
				while (i > 0 && (n = t[--i]) == null) {
				}
			}
			next = n;
			index = i;
		}

		public boolean hasNext() {
			return next != null;
		}

		Entry<V> nextEntry() {
			Entry<V> e = next;
			if(e == null) {
				throw new NoSuchElementException();
			}
			Entry<V> n = e.next;
			Entry<V>[] t = table;
			int i = index;
			while(n == null && i > 0) {
				n = t[--i];
			}
			index = i;
			next = n;
			return e;
		}

		public void remove() {
            throw new UnsupportedOperationException();
		}

	}

	private class ValueIterator extends HashIterator<Integer> {
		public Integer next() {
			return nextEntry().value;
		}
	}

	private class KeyIterator extends HashIterator<V> {
		public V next() {
			return nextEntry().key;
		}
	}

	private class EntryIterator extends HashIterator<Entry<V>> {
		public Entry<V> next() {
			return nextEntry();
		}
	}

    Iterator<V> newKeyIterator() {
		return new KeyIterator();
	}

	Iterator<Integer> newValueIterator() {
		return new ValueIterator();
	}

	Iterator<Entry<V>> newEntryIterator() {
		return new EntryIterator();
	}

	private transient Set<Entry<V>> entrySet = null;

	public Set<V> keySet() {
		Set<V> ks = keySet;
		return (ks != null ? ks : (keySet = new KeySet()));
	}

	private class KeySet extends AbstractSet<V> {

		public Iterator<V> iterator() {
			return newKeyIterator();
		}

		public int size() {
			return HashMapInt.this.count;
		}

        @Override
		public boolean contains(Object o) {
			return containsKey((V) o);
		}

        @Override
		public boolean remove(Object o) {
            throw new UnsupportedOperationException();
		}

        @Override
		public void clear() {
			HashMapInt.this.clear();
		}
	}

	public Collection<Integer> values() {
		Collection<Integer> vs = values;
		return (vs != null ? vs : (values = new Values()));
	}

	private class Values extends AbstractCollection<Integer> {

		public Iterator<Integer> iterator() {
			return newValueIterator();
		}

		public int size() {
			return HashMapInt.this.count;
		}

        @Override
		public boolean contains(Object o) {
			return containsValue((Integer) o);
		}

        @Override
		public void clear()	{
			HashMapInt.this.clear();
		}
	}

	public Set<Entry<V>> entrySet() {
		Set<Entry<V>> es = entrySet;
		return (es != null ? es : (entrySet = new EntrySet()));
	}

	private class EntrySet extends AbstractSet<Entry<V>> {

		public Iterator<Entry<V>> iterator() {
			return newEntryIterator();
		}

        @Override
		public boolean contains(Object o) {
			if (!(o instanceof Entry)) {
				return false;
			}
			@SuppressWarnings("unchecked")
            Entry<V> e = (Entry<V>) o;
			Entry<V> candidate = getEntry(e.key);
			return candidate != null && candidate.equals(e);
		}

        @Override
		public boolean remove(Object o)	{
            throw new UnsupportedOperationException();
		}

		public int size() {
			return HashMapInt.this.count;
		}

        @Override
		public void clear() {
			HashMapInt.this.clear();
		}
	}

	@Override
    public String toString() {
	    Iterator<Entry<V>> i = entrySet().iterator();
	    if (! i.hasNext()) return "{}";

	    StringBuilder sb = new StringBuilder();
	    sb.append('{');
	    for (;;) {
	        Entry<V> e = i.next();
            V key = e.getKey();
            sb.append(key == this ? "(this IntHashMap)" : key);
	        sb.append('=');
	        sb.append(e.getValue());
	        if (! i.hasNext()) return sb.append('}').toString();
	        sb.append(", ");
	    }
    }

    @Override
    public Object clone() {
        HashMapInt<V> newMap = new HashMapInt<>(table.length, loadFactor, identity);
        for (int i = 0; i < table.length; i++) {
            Entry<V> entry = table[i];
            while (entry != null) {
                newMap.put(entry.getKey(), entry.getValue());
                entry = entry.next;
            }
        }
        return newMap;
    }

    @SuppressWarnings("unchecked")
    public static <U> HashMapInt<U> nullMap() { return NullMap.INSTANCE; }

    private static final class NullMap<U> extends HashMapInt<U> {

        static final NullMap INSTANCE = new NullMap();

        private NullMap() { super(0, true); }

        @Override
        public boolean contains(int value) {
            return false;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public int get(Object key) {
            return -1;
        }

        @Override
        public int put(Object key, int value) {
            return -1;
        }

        @Override
        public int remove(Object key) {
            return -1;
        }

        @Override
        protected void rehash() {
            // NO-OP
        }

    }

}
