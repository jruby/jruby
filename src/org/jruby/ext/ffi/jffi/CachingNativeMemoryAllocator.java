package org.jruby.ext.ffi.jffi;

import org.jruby.Ruby;
import org.jruby.ext.ffi.AllocatedDirectMemoryIO;
import org.jruby.util.WeakReferenceReaper;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

final class CachingNativeMemoryAllocator {
    protected static final com.kenai.jffi.MemoryIO IO = com.kenai.jffi.MemoryIO.getInstance();
    /** Keeps strong references to the memory bucket until cleanup */
    private static final Bucket[] buckets = new Bucket[32];
    private static final Map<AllocationGroup, Boolean> referenceSet = new ConcurrentHashMap<AllocationGroup, Boolean>();
    private static final ThreadLocal<Reference<Allocator>> currentAllocator = new ThreadLocal<Reference<Allocator>>();


    static int bucketIndex(int size) {
        return Integer.numberOfTrailingZeros(size);
    }

    static {
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = new Bucket(1 << i);
        }
    }

    /**
     * Allocates native memory, aligned to a minimum boundary.
     *
     * @param runtime The Ruby runtime
     * @param size The number of bytes to allocate
     * @param align The minimum alignment of the memory
     * @param clear Whether the memory should be cleared (zeroed)
     * @return A new {@link org.jruby.ext.ffi.AllocatedDirectMemoryIO}
     */
    static AllocatedDirectMemoryIO allocateAligned(Ruby runtime, int size, int align, boolean clear) {
        // Caching seems to work best for small allocations (<= 32 bytes).  For everything else, use the default allocator
        if (size > 32 || align > 8) {
            return AllocatedNativeMemoryIO.allocateAligned(runtime, size, align, clear);
        }

        Reference<Allocator> allocatorReference = currentAllocator.get();
        Allocator allocator = allocatorReference != null ? allocatorReference.get() : null;
        if (allocator == null) {
            allocator = new Allocator();
            currentAllocator.set(new SoftReference<Allocator>(allocator));
        }

        return allocator.allocate(runtime, size, clear);
    }

    private static long align(long offset, long align) {
        return (offset + align - 1L) & ~(align - 1L);
    }

    static int roundUpToPowerOf2(int n) {
        n = n - 1;
        n |= (n >> 1);
        n |= (n >> 2);
        n |= (n >> 4);
        n |= (n >> 8);
        n |= (n >> 16);
        n |= (n >> (32 / 2));
        return n + 1;
    }


    static final class AllocatedMemoryIO extends BoundedNativeMemoryIO implements AllocatedDirectMemoryIO {

        private final MemoryAllocation allocation;
        private final Object sentinel;

        private AllocatedMemoryIO(Ruby runtime, Object sentinel, MemoryAllocation allocation, int size) {
            super(runtime, allocation.address, size);
            this.sentinel = sentinel;
            this.allocation = allocation;
        }

        public void free() {
            if (allocation.released) {
                throw getRuntime().newRuntimeError("memory already freed");
            }

            allocation.free();
        }

        public void setAutoRelease(boolean autorelease) {
            allocation.setAutoRelease(autorelease);
        }

    }


    private static final class AllocationGroup extends WeakReferenceReaper<Object> implements Runnable {
        final Magazine magazine;

        AllocationGroup(Magazine magazine, Object sentinel) {
            super(sentinel);
            this.magazine = magazine;
        }

        MemoryAllocation allocate(boolean clear) {
            return magazine.allocate(clear);
        }

        public void run() {
            referenceSet.remove(this);
            magazine.recycle();
        }
    }


    private static final class MemoryAllocation {
        final Magazine magazine;
        final long address;
        volatile boolean released;
        volatile boolean unmanaged;
        volatile MemoryAllocation next;

        MemoryAllocation(Magazine magazine, long address) {
            this.magazine = magazine;
            this.address = address;
        }

        final void dispose() {
            IO.freeMemory(address);
        }


        public void setAutoRelease(boolean autorelease) {
            if (autorelease && !released) {
                unmanaged = !autorelease;
            }
            magazine.setFragmented();
        }

        final void free() {
            if (!released) {
                released = true;
                unmanaged = true;
                magazine.setFragmented();
                dispose();
            }
        }
    }

    private static final class Magazine {
        private static final int MAX_BYTES_PER_MAGAZINE = 4096;
        private final Bucket bucket;
        private int totalAllocated = 0;
        private volatile MemoryAllocation allocations;
        private MemoryAllocation freeList;
        private volatile boolean fragmented;

        Magazine(Bucket bucket) {
            this.bucket = bucket;
        }

        MemoryAllocation allocate(boolean clear) {
            if (freeList != null) {
                MemoryAllocation allocation = freeList;
                freeList = freeList.next;
                if (clear) {
                    IO.setMemory(allocation.address, bucket.size, (byte) 0);
                }
                return allocation;
            }

            if (totalAllocated >= MAX_BYTES_PER_MAGAZINE) {
                return null;
            }

            // None on the freelist for this magazine, allocate more
            long address;
            while ((address = IO.allocateMemory(bucket.size, clear)) == 0L) {
                System.gc();
            }
            MemoryAllocation allocation = new MemoryAllocation(this, address);
            allocation.next = this.allocations;
            this.allocations = allocation;
            totalAllocated += bucket.size;
            return allocation;
        }

        void setFragmented() {
            fragmented = true;
        }

        synchronized void dispose() {
            MemoryAllocation m = allocations;
            while (m != null) {
                if (!m.unmanaged) {
                    m.dispose();
                }
                m = m.next;
            }
            allocations = freeList = null;
        }

        synchronized void recycle() {
            if (fragmented) {
                MemoryAllocation m = allocations;
                MemoryAllocation list = null;

                // Re-assemble the free list, skipping any non-autorelease allocations
                while (m != null) {
                    MemoryAllocation next = m.next;
                    if (!m.unmanaged) {
                        m.next = list;
                        list = m;
                    }
                    m = next;
                }
                allocations = list;
            }

            freeList = allocations;
            bucket.recycle(this);
        }
    }

    private static final class Bucket {
        final int size;
        Set<CacheElement> cache = new HashSet<CacheElement>();

        Bucket(int size) {
            this.size = roundUpToPowerOf2(size);
        }

        synchronized Magazine getMagazine() {

            Iterator<CacheElement> it = cache.iterator();
            while (it.hasNext()) {
                CacheElement e = it.next();
                it.remove();
                e.clear();
                if (!e.disposed.getAndSet(true)) {
                    return e.magazine;
                }
            }

            return new Magazine(this);
        }

        synchronized void recycle(Magazine magazine) {
            cache.add(new CacheElement(magazine));
        }

        private synchronized void removeCacheElement(CacheElement e) {
            cache.remove(e);
        }

        final class CacheElement extends WeakReferenceReaper<Object> {
            private final Magazine magazine;
            private final AtomicBoolean disposed = new AtomicBoolean(false);
            CacheElement(Magazine magazine) {
                super(new Object());
                this.magazine = magazine;
            }

            public void run() {
                if (!disposed.getAndSet(true)) {
                    removeCacheElement(this);
                    magazine.dispose();
                }
            }
        }
    }

    private static final class Allocator {
        AllocationGroup[] allocationGroups = new AllocationGroup[32];

        AllocatedMemoryIO allocate(Ruby runtime, int size, boolean clear) {
            MemoryAllocation allocation;
            Object sentinel;

            int idx = bucketIndex(roundUpToPowerOf2(Math.max(8, size)));
            AllocationGroup group = allocationGroups[idx];

            if (group == null || (sentinel = group.get()) == null || (allocation = group.allocate(clear)) == null) {
                // no existing group, or it is all used up.
                allocationGroups[idx] = group = new AllocationGroup(buckets[idx].getMagazine(), sentinel = new Object());
                referenceSet.put(group, Boolean.TRUE);
                allocation = group.allocate(clear);
            }

            return new AllocatedMemoryIO(runtime, sentinel, allocation, size);
        }
    }
}
