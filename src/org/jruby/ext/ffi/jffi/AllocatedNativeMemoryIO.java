package org.jruby.ext.ffi.jffi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jruby.Ruby;
import org.jruby.ext.ffi.AllocatedDirectMemoryIO;
import org.jruby.util.WeakReferenceReaper;

final class AllocatedNativeMemoryIO extends BoundedNativeMemoryIO implements AllocatedDirectMemoryIO {
    /** Keeps strong references to the memory bucket until cleanup */
    private static final Map<AllocationGroup, Boolean> referenceSet = new ConcurrentHashMap<AllocationGroup, Boolean>();
    private static final ThreadLocal<AllocationGroup> currentBucket = new ThreadLocal<AllocationGroup>();

    private final MemoryAllocation allocation;
    private final Object referent;

    /**
     * Allocates native memory
     *
     * @param runtime The Ruby runtime
     * @param size The number of bytes to allocate
     * @param clear Whether the memory should be cleared (zeroed)
     * @return A new {@link AllocatedDirectMemoryIO}
     */
    static final AllocatedNativeMemoryIO allocate(Ruby runtime, int size, boolean clear) {
        return allocateAligned(runtime, size, 1, clear);
    }

    /**
     * Allocates native memory, aligned to a minimum boundary.
     * 
     * @param runtime The Ruby runtime
     * @param size The number of bytes to allocate
     * @param align The minimum alignment of the memory
     * @param clear Whether the memory should be cleared (zeroed)
     * @return A new {@link AllocatedDirectMemoryIO}
     */
    static final AllocatedNativeMemoryIO allocateAligned(Ruby runtime, int size, int align, boolean clear) {
        final long address = IO.allocateMemory(size + align - 1, clear);
        if (address == 0) {
            throw runtime.newRuntimeError("failed to allocate " + size + " bytes of native memory");
        }

        try {
            /*
            * Instead of using a WeakReference per memory allocation to free the native memory, memory allocations
            * are grouped together into a bucket with a reference to a common object which has a WeakReference.
            *
            * When all the MemoryIO instances are no longer referenced, the common object can be garbage collected
            * and its WeakReference en-queued, and then the group of memory allocations will be freed in one hit.
            *
            * This reduces the overhead of automatically freed native memory allocations by about 70%
            */
            AllocationGroup allocationGroup = currentBucket.get();
            Object referent = allocationGroup != null ? allocationGroup.get() : null;

            if (referent == null || !allocationGroup.canAccept(size)) {
                referent = new Object();
                referenceSet.put(allocationGroup = new AllocationGroup(referent), Boolean.TRUE);
                currentBucket.set(allocationGroup);
            }

            AllocatedNativeMemoryIO io = new AllocatedNativeMemoryIO(runtime, referent, address, size, align);
            allocationGroup.add(io.allocation, size);

            return io;

        } catch (Throwable t) {
            IO.freeMemory(address);
            throw new RuntimeException(t);
        }
    }
    
    private AllocatedNativeMemoryIO(Ruby runtime, Object referent, long address, int size, int align) {
        super(runtime, ((address - 1) & ~(align - 1)) + align, size);
        this.referent = referent;
        this.allocation = new MemoryAllocation(address);
    }

    public void free() {
        if (allocation.released) {
            throw getRuntime().newRuntimeError("memory already freed");
        }
        
        allocation.free();
    }

    public void setAutoRelease(boolean autorelease) {
        if (autorelease && !allocation.released) {
            allocation.unmanaged = !autorelease;
        }
    }


    /**
     * Holder for a group of memory allocations.
     */
    private static final class AllocationGroup extends WeakReferenceReaper<Object> implements Runnable {
        public static final int MAX_BYTES_PER_BUCKET = 4096;
        private volatile MemoryAllocation head = null;
        private long bytesUsed = 0;
        
        AllocationGroup(Object referent) {
            super(referent);
        }

        void add(MemoryAllocation m, int size) {
            bytesUsed += size;
            m.next = head;
            head = m;
        }

        boolean canAccept(int size) {
            return bytesUsed + size < MAX_BYTES_PER_BUCKET;
        }

        public final void run() {
            referenceSet.remove(this);
            MemoryAllocation m = head;
            while (m != null) {
                if (!m.unmanaged) {
                    m.dispose();
                }
                m = m.next;
            }
        }
    }

    /**
     * Represents a single native memory allocation
     */
    private static final class MemoryAllocation {
        private final long address;
        volatile boolean released;
        volatile boolean unmanaged;
        volatile MemoryAllocation next;

        MemoryAllocation(long address) {
            this.address = address;
        }

        final void dispose() {
            IO.freeMemory(address);
        }

        final void free() {
            if (!released) {
                released = true;
                unmanaged = true;
                dispose();
            }
        }
    }
}
