package org.jruby.ext.ffi.jffi;

import jnr.ffi.util.ref.FinalizableWeakReference;
import org.jruby.Ruby;
import org.jruby.ext.ffi.AllocatedDirectMemoryIO;
import org.jruby.ext.ffi.MemoryIO;

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
    static MemoryIO allocateAligned(Ruby runtime, int size, int align, boolean clear) {
        // Caching seems to work best for small allocations (<= 256 bytes).  For everything else, use the default allocator
        if (size > 256 || align > 8) {
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
        --n;
        n |= (n >> 1);
        n |= (n >> 2);
        n |= (n >> 4);
        n |= (n >> 8);
        n |= (n >> 16);

        return n + 1;
    }


    static final class AllocatedMemoryIO extends BoundedNativeMemoryIO implements AllocatedDirectMemoryIO {

        private final MemoryAllocation allocation;
        private Object sentinel;

        private AllocatedMemoryIO(Ruby runtime, Object sentinel, MemoryAllocation allocation, int size) {
            super(runtime, allocation.address, size);
            this.sentinel = sentinel;
            this.allocation = allocation;
        }

        public void free() {
            if (allocation.isReleased()) {
                throw getRuntime().newRuntimeError("memory already freed");
            }

            allocation.free();
            sentinel = null;
        }

        public void setAutoRelease(boolean autorelease) {
            allocation.setAutoRelease(autorelease);
        }

        public boolean isAutoRelease() {
            return !allocation.isUnmanaged();
        }
    }


    private static final class AllocationGroup extends FinalizableWeakReference<Object> {
        final Magazine magazine;

        AllocationGroup(Magazine magazine, Object sentinel) {
            super(sentinel, NativeFinalizer.getInstance().getFinalizerQueue());
            this.magazine = magazine;
        }

        MemoryAllocation allocate(boolean clear) {
            return magazine.allocate(clear);
        }

        public void finalizeReferent() {
            referenceSet.remove(this);
            magazine.recycle();
        }
    }


    private static final class MemoryAllocation {
        static final int UNMANAGED = 0x1;
        static final int RELEASED = 0x2;
        final Magazine magazine;
        final long address;
        volatile int flags;

        MemoryAllocation(Magazine magazine, long address) {
            this.magazine = magazine;
            this.address = address;
        }

        final void dispose() {
            IO.freeMemory(address);
        }

        final boolean isReleased() {
            return (flags & RELEASED) != 0;
        }

        final boolean isUnmanaged() {
            return (flags & UNMANAGED) != 0;
        }

        public void setAutoRelease(boolean autorelease) {
            if ((flags & RELEASED) == 0) {
                flags |= !autorelease ? UNMANAGED : 0;
            }

            if (!autorelease) {
                magazine.setFragmented();
            }
        }

        final void free() {
            if ((flags & RELEASED) == 0) {
                flags = RELEASED | UNMANAGED;
                magazine.setFragmented();
                dispose();
            }
        }
    }

    private static final class Magazine {
        static final int MAX_BYTES_PER_MAGAZINE = 16384;
        final Bucket bucket;
        private final MemoryAllocation[] allocations;
        private int nextIndex;
        private volatile boolean fragmented;

        Magazine(Bucket bucket) {
            this.bucket = bucket;
            this.allocations = new MemoryAllocation[MAX_BYTES_PER_MAGAZINE / bucket.size];
            this.nextIndex = 0;
        }

        MemoryAllocation allocate(boolean clear) {
            if (nextIndex < allocations.length && allocations[nextIndex] != null) {
                MemoryAllocation allocation = allocations[nextIndex++];
                if (clear) {
                    clearMemory(allocation.address, bucket.size);
                }

                return allocation;
            }

            if (nextIndex >= allocations.length) {
                return null;
            }

            // None on the freelist for this magazine, allocate more
            long address;
            while ((address = IO.allocateMemory(bucket.size, clear)) == 0L) {
                System.gc();
            }

            MemoryAllocation allocation = new MemoryAllocation(this, address);
            allocations[nextIndex++] = allocation;

            return allocation;
        }

        void setFragmented() {
            fragmented = true;
        }

        synchronized void dispose() {
            for (int i = 0; i < allocations.length; i++) {
                MemoryAllocation m = allocations[i];
                if (m != null && !m.isUnmanaged()) {
                    m.dispose();
                }
            }
        }

        synchronized void recycle() {
            if (fragmented) {
                int size = bucket.size;
                for (int i = 0; i < allocations.length; i++) {
                    MemoryAllocation m = allocations[i];
                    if (m != null) {
                        if (m.isUnmanaged()) {
                            allocations[i] = null;
                        } else {
                            clearMemory(allocations[i].address, size);
                        }
                    }
                }
                fragmented = false;
            }

            nextIndex = 0;
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

        final class CacheElement extends FinalizableWeakReference<Object> {
            private final Magazine magazine;
            private final AtomicBoolean disposed = new AtomicBoolean(false);
            CacheElement(Magazine magazine) {
                super(new Object(), NativeFinalizer.getInstance().getFinalizerQueue());
                this.magazine = magazine;
            }

            public void finalizeReferent() {
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

    static void clearMemory(long address, int size) {
        switch (size) {
            case 1:
                IO.putByte(address, (byte) 0);
                break;

            case 2:
                IO.putShort(address, (short) 0);
                break;

            case 4:
                IO.putInt(address, 0);
                break;

            case 8:
                IO.putLong(address, 0L);
                break;

            default:
                IO.setMemory(address, size, (byte) 0);
        }
    }
}
