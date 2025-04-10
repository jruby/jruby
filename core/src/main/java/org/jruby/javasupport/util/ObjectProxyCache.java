package org.jruby.javasupport.util;

import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

import java.util.concurrent.locks.ReentrantLock;


/**
 * <p>Maps Java objects to their proxies.  Combines elements of WeakHashMap and
 * ConcurrentHashMap to permit unsynchronized reads.  May be configured to
 * use either Weak (the default) or Soft references.</p>
 *
 * <p>Note that both Java objects and their proxies are held by weak/soft
 * references; because proxies (currently) keep strong references to their
 * Java objects, if we kept strong references to them the Java objects would
 * never be gc'ed.  This presents a problem in the case where a user passes
 * a Rubified Java object out to Java but keeps no reference in Ruby to the
 * proxy; if the object is returned to Ruby after its proxy has been gc'ed,
 * a new (and possibly very wrong, in the case of JRuby-defined subclasses)
 * proxy will be created.  Use of soft references may help reduce the
 * likelihood of this occurring; users may be advised to keep Ruby-side
 * references to prevent it occurring altogether.</p>
 *
 * @param <T> the T
 * @param <A> the A
 * @author <a href="mailto:bill.dortch@gmail.com">Bill Dortch</a>
 */
public abstract class ObjectProxyCache<T,A> {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectProxyCache.class);

    public static enum ReferenceType { WEAK, SOFT }

    private static final int DEFAULT_SEGMENTS = 16; // must be power of 2
    private static final int DEFAULT_SEGMENT_SIZE = 8; // must be power of 2
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final int MAX_CAPACITY = 1 << 30;
    private static final int MAX_SEGMENTS = 1 << 16;
    private static final int VULTURE_RUN_FREQ_SECONDS = 5;

    private static int _nextId = 0;

    private static synchronized int nextId() {
        return ++_nextId;
    }


    private final ReferenceType referenceType;
    private final Segment<T,A>[] segments;
    private final int segmentShift;
    private final int segmentMask;
    private final int id;

    public ObjectProxyCache() {
        this(DEFAULT_SEGMENTS, DEFAULT_SEGMENT_SIZE, ReferenceType.WEAK);
    }

    public ObjectProxyCache(ReferenceType refType) {
        this(DEFAULT_SEGMENTS, DEFAULT_SEGMENT_SIZE, refType);
    }


    public ObjectProxyCache(int numSegments, int initialSegCapacity, ReferenceType refType) {
        if (numSegments <= 0 || initialSegCapacity <= 0 || refType == null) {
            throw new IllegalArgumentException();
        }
        this.id = nextId();
        this.referenceType = refType;
        if (numSegments > MAX_SEGMENTS) numSegments = MAX_SEGMENTS;

        // Find power-of-two sizes best matching arguments
        int sshift = 0;
        int ssize = 1;
        while (ssize < numSegments) {
            ++sshift;
            ssize <<= 1;
        }
        // note segmentShift differs from ConcurrentHashMap's calculation due to
        // issues with System.identityHashCode (upper n bits always 0, at least
        // under Java 1.6 / WinXP)
        this.segmentShift = 24 - sshift;
        this.segmentMask = ssize - 1;
        this.segments = Segment.newArray(ssize);

        if (initialSegCapacity > MAX_CAPACITY) {
            initialSegCapacity = MAX_CAPACITY;
        }
        int cap = 1;
        while (cap < initialSegCapacity) cap <<= 1;

        for (int i = ssize; --i >= 0; ) {
            segments[i] = new Segment<T,A>(cap, this);
        }
        // vulture thread will periodically expunge dead
        // entries.  entries are also expunged during 'put'
        // operations; this is designed to cover the case where
        // many objects are created initially, followed by limited
        // put activity.
        //
        // FIXME: DISABLED (below) pending resolution of finalization issue
        //
        Thread vulture;
        try {
            vulture = new Thread("ObjectProxyCache "+id+" vulture") {
                    public void run() {
                        for ( ;; ) {
                            try {
                                sleep(VULTURE_RUN_FREQ_SECONDS * 1000);
                            } catch (InterruptedException e) {}
                            boolean dump = size() > 200;
                            if (dump) {
                                LOG.debug("***Vulture {} waking, stats:", id);
                                LOG.debug(stats());
                            }
                            for (int i = segments.length; --i >= 0; ) {
                                Segment<T,A> seg = segments[i];
                                seg.lock();
                                try {
                                    seg.expunge();
                                } finally {
                                    seg.unlock();
                                }
                                Thread.yield();
                            }
                            if (dump) {
                                LOG.debug("***Vulture {} sleeping, stats:", id);
                                LOG.debug(stats());
                            }
                        }
                    }
                };
            vulture.setDaemon(true);
        } catch (SecurityException e) {
            vulture = null;
        }


        // FIXME: vulture daemon thread prevents finalization,
        // find alternative approach.
        // vulture.start();

//      System.err.println("***ObjectProxyCache " + id + " started at "+ new java.util.Date());
    }

//    protected void finalize() throws Throwable {
//        System.err.println("***ObjectProxyCache " + id + " finalized at "+ new java.util.Date());
//    }

    public abstract T allocateProxy(Object javaObject, A allocator);

    public T get(Object javaObject) {
        if (javaObject == null) return null;
        int hash = hash(javaObject);
        return segmentFor(hash).get(javaObject, hash);
    }

    public T getOrCreate(Object javaObject, A allocator) {
        if (javaObject == null || allocator == null) return null;
        int hash = hash(javaObject);
        return segmentFor(hash).getOrCreate(javaObject, hash, allocator);
    }

    public void put(Object javaObject, T proxy) {
        if (javaObject == null || proxy == null) return;
        int hash = hash(javaObject);
        segmentFor(hash).put(javaObject, hash, proxy);
    }

    private static int hash(Object javaObject) {
        int h = System.identityHashCode(javaObject);
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }

    private Segment<T,A> segmentFor(int hash) {
        return segments[(hash >>> segmentShift) & segmentMask];
    }

    /**
     * Returns the approximate size (elements in use) of the cache. The
     * sizes of the segments are summed. No effort is made to synchronize
     * across segments, so the value returned may differ from the actual
     * size at any point in time.
     *
     * @return
     */
    public int size() {
       int size = 0;
       for (Segment<T,A> seg : segments) {
           size += seg.tableSize;
       }
       return size;
    }

    public String stats() {
        StringBuilder b = new StringBuilder();
        int n = 0;
        int size = 0;
        int alloc = 0;
        b.append("Segments: ").append(segments.length).append("\n");
        for (Segment<T,A> seg : segments) {
            int ssize = 0;
            int salloc = 0;
            seg.lock();
            try {
                ssize = seg.count();
                salloc = seg.entryTable.length;
            } finally {
                seg.unlock();
            }
            size += ssize;
            alloc += salloc;
            b.append("seg[").append(n++).append("]:  size: ").append(ssize)
                .append("  alloc: ").append(salloc).append("\n");
        }
        b.append("Total: size: ").append(size)
            .append("  alloc: ").append(alloc).append("\n");
        return b.toString();
    }

    // EntryRefs include hash with key to facilitate lookup by Segment#expunge
    // after ref is removed from ReferenceQueue
    private static interface EntryRef<T> {
        T get();
        int hash();
    }

    private static final class WeakEntryRef<T> extends WeakReference<T> implements EntryRef<T> {
        final int hash;
        WeakEntryRef(int hash, T rawObject, ReferenceQueue<Object> queue) {
            super(rawObject, queue);
            this.hash = hash;
        }
        public int hash() {
            return hash;
        }
    }

    private static final class SoftEntryRef<T> extends SoftReference<T> implements EntryRef<T> {
        final int hash;
        SoftEntryRef(int hash, T rawObject, ReferenceQueue<Object> queue) {
            super(rawObject, queue);
            this.hash = hash;
        }
        public int hash() {
            return hash;
        }
    }

    // Unlike WeakHashMap, our Entry does not subclass WeakReference, but rather
    // makes it a final field.  The theory is that doing so should force a happens-before
    // relationship WRT the WeakReference constructor, guaranteeing that the key will be
    // visibile to other threads (unless it's been GC'ed).  See JLS 17.5 (final fields) and
    // 17.4.5 (Happens-before order) to confirm or refute my reasoning here.
    static class Entry<T> {
        final EntryRef<Object> objectRef;
        final int hash;
        final EntryRef<T> proxyRef;
        final Entry<T> next;

        Entry(Object object, int hash, T proxy, ReferenceType type, Entry<T> next, ReferenceQueue<Object> queue) {
            this.hash = hash;
            this.next = next;
            // references to the Java object and its proxy will either both be
            // weak or both be soft, since the proxy contains a strong reference
            // to the object, so it wouldn't make sense for the reference types
            // to differ.
            if (type == ReferenceType.WEAK) {
                this.objectRef = new WeakEntryRef<Object>(hash, object, queue);
                this.proxyRef = new WeakEntryRef<T>(hash, proxy, queue);
            } else {
                this.objectRef = new SoftEntryRef<Object>(hash, object, queue);
                this.proxyRef = new SoftEntryRef<T>(hash, proxy, queue);
            }
        }

        // ctor used by remove/rehash
        Entry(EntryRef<Object> objectRef, int hash, EntryRef<T> proxyRef, Entry<T> next) {
            this.objectRef = objectRef;
            this.hash = hash;
            this.proxyRef = proxyRef;
            this.next = next;
        }

        @SuppressWarnings("unchecked")
        static final <T> Entry<T>[] newArray(int size) {
            return new Entry[size];
        }
     }

    // lame generics issues: making Segment class static and manually
    // inserting cache reference to work around various problems generically
    // referencing methods/vars across classes.
    static class Segment<T,A> extends ReentrantLock {

        final ObjectProxyCache<T,A> cache;
        final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<Object>();
        volatile Entry<T>[] entryTable;
        int tableSize;
        int threshold;

        Segment(int capacity, ObjectProxyCache<T,A> cache) {
            threshold = (int)(capacity * DEFAULT_LOAD_FACTOR);
            entryTable = Entry.newArray(capacity);
            this.cache = cache;
        }

        // must be called under lock
        private void expunge() {
            Entry<T>[] table = entryTable;
            ReferenceQueue<Object> queue = referenceQueue;
            EntryRef ref;
            // note that we'll potentially see the refs for both the java object and
            // proxy -- whichever we see first will cause the entry to be removed;
            // the other will not match an entry and will be ignored.
            while ((ref = (EntryRef)queue.poll()) != null) {
                int hash;
                for (Entry<T> e = table[(hash = ref.hash()) & (table.length - 1)]; e != null; e = e.next) {
                    if (hash == e.hash && (ref == e.objectRef || ref == e.proxyRef)) {
                        remove(table, hash, e);
                        break;
                    }
                }
            }
        }

        // must be called under lock
        private void remove(Entry<T>[] table, int hash, Entry<T> e) {
            int index = hash & (table.length - 1);
            Entry<T> first = table[index];
            for (Entry<T> n = first; n != null; n = n.next) {
                if (n == e) {
                    Entry<T> newFirst = n.next;
                    for (Entry<T> p = first; p != n; p = p.next) {
                        newFirst = new Entry<T>(p.objectRef, p.hash, p.proxyRef, newFirst);
                    }
                    table[index] = newFirst;
                    tableSize--;
                    entryTable = table; // write-volatile
                    return;
                }
            }
        }

        // temp method to verify tableSize value
        // must be called under lock
        private int count() {
            int count = 0;
            for (Entry<T> e : entryTable) {
                while (e != null) {
                    count++;
                    e = e.next;
                }
            }
            return count;
        }

        // must be called under lock
        private Entry<T>[] rehash() {
            assert tableSize == count() : "tableSize "+tableSize+" != count() "+count();
            Entry<T>[] oldTable = entryTable; // read-volatile
            int oldCapacity;
            if ((oldCapacity = oldTable.length) >= MAX_CAPACITY) {
                return oldTable;
            }
            int newCapacity = oldCapacity << 1;
            int sizeMask = newCapacity - 1;
            threshold = (int)(newCapacity * DEFAULT_LOAD_FACTOR);
            Entry<T>[] newTable = Entry.newArray(newCapacity);
            Entry<T> e;
            for (int i = oldCapacity; --i >= 0; ) {
                if ((e = oldTable[i]) != null) {
                    int idx = e.hash & sizeMask;
                    Entry<T> next;
                    if ((next = e.next) == null) {
                        // Single node in list
                        newTable[idx] = e;
                    } else {
                        // Reuse trailing consecutive sequence at same slot
                        int lastIdx = idx;
                        Entry<T> lastRun = e;
                        for (Entry<T> last = next; last != null; last = last.next) {
                            int k;
                            if ((k = last.hash & sizeMask) != lastIdx) {
                                lastIdx = k;
                                lastRun = last;
                            }
                        }
                        newTable[lastIdx] = lastRun;
                        // Clone all remaining nodes
                        for (Entry<T> p = e; p != lastRun; p = p.next) {
                            int k = p.hash & sizeMask;
                            Entry<T> m = new Entry<T>(p.objectRef, p.hash, p.proxyRef, newTable[k]);
                            newTable[k] = m;
                        }
                    }
                }
            }
            entryTable = newTable; // write-volatile
            return newTable;
        }

        void put(Object object, int hash, T proxy) {
            lock();
            try {
                expunge();
                Entry<T>[] table;
                int potentialNewSize;
                if ((potentialNewSize = tableSize + 1) > threshold) {
                    table = rehash(); // indirect read-/write- volatile
                } else {
                    table = entryTable; // read-volatile
                }
                int index;
                Entry<T> e;
                for (e = table[index = hash & (table.length - 1)]; e != null; e = e.next) {
                    if (hash == e.hash && object == e.objectRef.get()) {
                        if (proxy == e.proxyRef.get()) return;
                        // entry exists, proxy doesn't match. replace.
                        // this could happen if old proxy was gc'ed
                        // TODO: raise exception if stored proxy is non-null? (not gc'ed)
                        remove(table, hash, e);
                        potentialNewSize--;
                        break;
                    }
                }
                e = new Entry<T>(object, hash, proxy, cache.referenceType, table[index], referenceQueue);
                table[index] = e;
                tableSize = potentialNewSize;
                entryTable = table; // write-volatile
            } finally {
                unlock();
            }
        }

        T getOrCreate(Object object, int hash, A allocator) {
            Entry<T>[] table;
            T proxy;
            for (Entry<T> e = (table = entryTable)[hash & table.length - 1]; e != null; e = e.next) {
                if (hash == e.hash && object == e.objectRef.get()) {
                    if ((proxy = e.proxyRef.get()) != null) return proxy;
                    break;
                }
            }
            lock();
            try {
                expunge();
                int potentialNewSize;
                if ((potentialNewSize = tableSize + 1) > threshold) {
                    table = rehash(); // indirect read-/write- volatile
                } else {
                    table = entryTable; // read-volatile
                }
                int index;
                Entry<T> e;
                for (e = table[index = hash & (table.length - 1)]; e != null; e = e.next) {
                    if (hash == e.hash && object == e.objectRef.get()) {
                        if ((proxy = e.proxyRef.get()) != null) return proxy;
                        // entry exists, proxy has been gc'ed. replace entry.
                        remove(table, hash, e);
                        potentialNewSize--;
                        break;
                    }
                }
                proxy = cache.allocateProxy(object, allocator);
                e = new Entry<T>(object, hash, proxy, cache.referenceType, table[index], referenceQueue);
                table[index] = e;
                tableSize = potentialNewSize;
                entryTable = table; // write-volatile
                return proxy;
            } finally {
                unlock();
            }
        }

        T get(Object object, int hash) {
            Entry<T>[] table;
            for (Entry<T> e = (table = entryTable)[hash & table.length - 1]; e != null; e = e.next) {
                if (hash == e.hash && object == e.objectRef.get()) {
                    return e.proxyRef.get();
                }
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        static final <T,A> Segment<T,A>[] newArray(int size) {
            return new Segment[size];
        }
    }
}
