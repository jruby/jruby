package org.jruby.util.collections;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

@Deprecated
public class IntHashMap {
    private transient Entry table[];

    private transient int count;


    transient volatile Set keySet = null;
	transient volatile Collection values = null;

    private int threshold;
 
    private final float loadFactor;
 
    private static class Entry {
        final int hash;
        final int key;
        Object value;
        Entry next;
 
        protected Entry(int hash, int key, Object value, Entry next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
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
        table = new Entry[initialCapacity];
        threshold = (int) (initialCapacity * loadFactor);
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
 
        Entry tab[] = table;
        for (int i = tab.length; i-- > 0;) {
            for (Entry e = tab[i]; e != null; e = e.next) {
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
        Entry tab[] = table;
        int hash = key;
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (Entry e = tab[index]; e != null; e = e.next) {
            if (e.hash == hash) {
                return true;
            }
        }
        return false;
    }
 
    public Object get(int key) {
        Entry tab[] = table;
        int hash = key;
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (Entry e = tab[index]; e != null; e = e.next) {
            if (e.hash == hash) {
                return e.value;
            }
        }
        return null;
    }
 
    protected void rehash() {
        int oldCapacity = table.length;
        Entry oldMap[] = table;
 
        int newCapacity = oldCapacity * 2 + 1;
        Entry newMap[] = new Entry[newCapacity];
 
        threshold = (int) (newCapacity * loadFactor);
        table = newMap;
 
        for (int i = oldCapacity; i-- > 0;) {
            for (Entry old = oldMap[i]; old != null;) {
                Entry e = old;
                old = old.next;
 
                int index = (e.hash & 0x7FFFFFFF) % newCapacity;
                e.next = newMap[index];
                newMap[index] = e;
            }
        }
    }

	Entry getEntry(int key)	{
        Entry tab[] = table;
        int hash = key;
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for(Entry e = tab[index]; e != null; e = e.next) {
            if (e.hash == hash) {
                return e;
            }
        }
        return null;
	} 

    public Object put(int key, Object value) {
        // Makes sure the key is not already in the hashtable.
        Entry tab[] = table;
        int hash = key;
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (Entry e = tab[index]; e != null; e = e.next) {
            if (e.hash == hash) {
                Object old = e.value;
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
        Entry e = new Entry(hash, key, value, tab[index]);
        tab[index] = e;
        count++;
        return null;
    }
 
    public Object remove(int key) {
        Entry tab[] = table;
        int hash = key;
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (Entry e = tab[index], prev = null; e != null; prev = e, e = e.next) {
            if (e.hash == hash) {
                if (prev != null) {
                    prev.next = e.next;
                } else {
                    tab[index] = e.next;
                }
                count--;
                Object oldValue = e.value;
                e.value = null;
                return oldValue;
            }
        }
        return null;
    }
 
    public synchronized void clear() {
        Entry tab[] = table;
        for (int index = tab.length; --index >= 0;) {
            tab[index] = null;
        }
        count = 0;
    }

    private abstract class HashIterator implements Iterator {
		Entry next; // next entry to return
		int index; // current slot
		Entry current; // current entry

		HashIterator() {
			Entry[] t = table;
			int i = t.length;
			Entry n = null;
			if(count != 0) { // advance to first entry
				while (i > 0 && (n = t[--i]) == null) {
					;
				}
			}
			next = n;
			index = i;
		}

		public boolean hasNext() {
			return next != null;
		}

		Entry nextEntry() {
			Entry e = next;
			if(e == null) {
				throw new NoSuchElementException();
			}
			Entry n = e.next;
			Entry[] t = table;
			int i = index;
			while(n == null && i > 0) {
				n = t[--i];
			}
			index = i;
			next = n;
			return current = e;
		}

		public void remove() {
            throw new UnsupportedOperationException();
		}

	}

	private class ValueIterator extends HashIterator {
		public Object next() {
			return nextEntry().value;
		}
	}

	private class KeyIterator extends HashIterator {
		public Object next() {
			return Integer.valueOf(nextEntry().key);
		}
	}

	private class EntryIterator extends HashIterator {
		public Object next() {
			return nextEntry();
		}
	}

	Iterator newKeyIterator() {
		return new KeyIterator();
	}

	Iterator newValueIterator() {
		return new ValueIterator();
	}

	Iterator newEntryIterator() {
		return new EntryIterator();
	}

	private transient Set entrySet = null;

	public Set keySet() {
		Set ks = keySet;
		return (ks != null ? ks : (keySet = new KeySet()));
	}

	private class KeySet extends AbstractSet {
		public Iterator iterator() {
			return newKeyIterator();
		}

		public int size() {
			return count;
		}

		public boolean contains(Object o) {
			if(o instanceof Number) {
				return containsKey(((Number)o).intValue());
			}
			return false;
		}

		public boolean remove(Object o) {
            throw new UnsupportedOperationException();
		}

		public void clear() {
			IntHashMap.this.clear();
		}
	}

	public Collection values() {
		Collection vs = values;
		return (vs != null ? vs : (values = new Values()));
	}

	private class Values extends AbstractCollection {
		public Iterator iterator() {
			return newValueIterator();
		}

		public int size() {
			return count;
		}

		public boolean contains(Object o) {
			return containsValue(o);
		}

		public void clear()	{
			IntHashMap.this.clear();
		}
	}

	public Set entrySet() {
		Set es = entrySet;
		return (es != null ? es : (entrySet = new EntrySet()));
	}

	private class EntrySet extends AbstractSet {
		public Iterator iterator() {
			return newEntryIterator();
		}

		public boolean contains(Object o) {
			if (!(o instanceof Entry))
			{
				return false;
			}
			Entry e = (Entry)o;
			Entry candidate = getEntry(e.key);
			return candidate != null && candidate.equals(e);
		}

		public boolean remove(Object o)	{
            throw new UnsupportedOperationException();
		}

		public int size() {
			return count;
		}

		public void clear() {
			IntHashMap.this.clear();
		}
	}
}
