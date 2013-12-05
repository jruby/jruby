package org.jruby.util.collections;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public class IntHashMap<V> {
    
    private transient Entry<V>[] table;
    
    private transient int count;
    
    transient volatile Set<Integer> keySet = null;
	transient volatile Collection<V> values = null;

    private int threshold;
 
    private final float loadFactor;
 
    public static class Entry<V> {
        final int hash;
        final int key;
        V value;
        Entry<V> next;
 
        protected Entry(int hash, int key, V value, Entry<V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public int getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }
 
    public IntHashMap() {
        this(20, 0.75f);
    }
 
    public IntHashMap(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }
 
    public IntHashMap(int initialCapacity, float loadFactor) {
        super();
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        }
        if (loadFactor <= 0) {
            throw new IllegalArgumentException("Illegal Load: " + loadFactor);
        }
        if (initialCapacity == 0) {
            initialCapacity = 1;
        }
 
        this.loadFactor = loadFactor;
        this.threshold = (int) (initialCapacity * loadFactor);
        @SuppressWarnings("unchecked")
        Entry<V>[] table = new Entry[initialCapacity];
        this.table = table;
    }

    public int size() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0;
    }
 
    public boolean contains(Object value) {
        if (value == null) {
            throw new NullPointerException();
        }
 
        Entry<V> tab[] = table;
        for (int i = tab.length; i-- > 0;) {
            for (Entry<V> e = tab[i]; e != null; e = e.next) {
                if (e.value.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }
 
    public boolean containsValue(Object value) {
        return contains(value);
    }
 
    public boolean containsKey(int key) {
        Entry<V>[] tab = table;
        int hash = key;
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (Entry<V> e = tab[index]; e != null; e = e.next) {
            if (e.hash == hash) {
                return true;
            }
        }
        return false;
    }
 
    public V get(int key) {
        Entry<V>[] tab = table;
        int hash = key;
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (Entry<V> e = tab[index]; e != null; e = e.next) {
            if (e.hash == hash) {
                return e.value;
            }
        }
        return null;
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

	Entry<V> getEntry(int key)	{
        Entry<V>[] tab = table;
        int hash = key;
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for(Entry<V> e = tab[index]; e != null; e = e.next) {
            if (e.hash == hash) {
                return e;
            }
        }
        return null;
	} 

    public V put(int key, V value) {
        // Makes sure the key is not already in the hashtable.
        Entry<V>[] tab = table;
        int hash = key;
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (Entry<V> e = tab[index]; e != null; e = e.next) {
            if (e.hash == hash) {
                V old = e.value;
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
        return null;
    }
 
    public V remove(int key) {
        Entry<V>[] tab = table;
        int hash = key;
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (Entry<V> e = tab[index], prev = null; e != null; prev = e, e = e.next) {
            if (e.hash == hash) {
                if (prev != null) {
                    prev.next = e.next;
                } else {
                    tab[index] = e.next;
                }
                count--;
                V oldValue = e.value;
                e.value = null;
                return oldValue;
            }
        }
        return null;
    }
 
    public synchronized void clear() {
        Entry<V>[] tab = table;
        for (int index = tab.length; --index >= 0;) {
            tab[index] = null;
        }
        count = 0;
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

	private class ValueIterator extends HashIterator<V> {
		public V next() {
			return nextEntry().value;
		}
	}

	private class KeyIterator extends HashIterator<Integer> {
		public Integer next() {
			return Integer.valueOf(nextEntry().key);
		}
	}

	private class EntryIterator extends HashIterator<Entry<V>> {
		public Entry<V> next() {
			return nextEntry();
		}
	}

    Iterator<Integer> newKeyIterator() {
		return new KeyIterator();
	}

	Iterator<V> newValueIterator() {
		return new ValueIterator();
	}

	Iterator<Entry<V>> newEntryIterator() {
		return new EntryIterator();
	}

	private transient Set<Entry<V>> entrySet = null;

	public Set<Integer> keySet() {
		Set<Integer> ks = keySet;
		return (ks != null ? ks : (keySet = new KeySet()));
	}

	private class KeySet extends AbstractSet<Integer> {
	    
		public Iterator<Integer> iterator() {
			return newKeyIterator();
		}

		public int size() {
			return IntHashMap.this.count;
		}

        @Override
		public boolean contains(Object o) {
			if(o instanceof Number) {
				return containsKey(((Number)o).intValue());
			}
			return false;
		}

        @Override
		public boolean remove(Object o) {
            throw new UnsupportedOperationException();
		}

        @Override
		public void clear() {
			IntHashMap.this.clear();
		}
	}

	public Collection<V> values() {
		Collection<V> vs = values;
		return (vs != null ? vs : (values = new Values()));
	}

	private class Values extends AbstractCollection<V> {
	    
		public Iterator<V> iterator() {
			return newValueIterator();
		}

		public int size() {
			return IntHashMap.this.count;
		}

        @Override
		public boolean contains(Object o) {
			return containsValue(o);
		}

        @Override
		public void clear()	{
			IntHashMap.this.clear();
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
			return IntHashMap.this.count;
		}

        @Override
		public void clear() {
			IntHashMap.this.clear();
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
	        V value = e.getValue();
	        sb.append(e.getKey());
	        sb.append('=');
	        sb.append(value == this ? "(this IntHashMap)" : value);
	        if (! i.hasNext()) return sb.append('}').toString();
	        sb.append(", ");
	    }   
    }
	
}
