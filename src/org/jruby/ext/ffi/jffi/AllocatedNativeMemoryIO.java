package org.jruby.ext.ffi.jffi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jruby.Ruby;
import org.jruby.ext.ffi.AllocatedDirectMemoryIO;
import org.jruby.util.ReferenceReaper;

final class AllocatedNativeMemoryIO extends BoundedNativeMemoryIO implements AllocatedDirectMemoryIO {
    /** Keeps strong references to the MemoryHolder until cleanup */
    private static final Map<MemoryHolder, Boolean> referenceSet = new ConcurrentHashMap<MemoryHolder, Boolean>();

    private final MemoryHolder holder;

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
            return new AllocatedNativeMemoryIO(runtime, address, size, align);
        } catch (Throwable t) {
            IO.freeMemory(address);
            throw new RuntimeException(t);
        }
    }
    
    private AllocatedNativeMemoryIO(Ruby runtime, long address, int size, int align) {
        super(runtime, ((address - 1) & ~(align - 1)) + align, size);
        referenceSet.put(holder = new MemoryHolder(this, address), Boolean.TRUE);
    }

    public void free() {
        if (holder.released) {
            throw getRuntime().newRuntimeError("memory already freed");
        }
        
        holder.free();
        referenceSet.remove(holder); // No auto cleanup needed
    }

    public void setAutoRelease(boolean release) {
        holder.autorelease = release;
    }

    private static final class MemoryHolder extends ReferenceReaper.Phantom<AllocatedNativeMemoryIO> implements Runnable {        

        private final long storage;
        private volatile boolean released = false;
        private volatile boolean autorelease = true;

        MemoryHolder(AllocatedNativeMemoryIO mem, long storage) {
            super(mem);
            this.storage = storage;
        }

        public final void run() {
            referenceSet.remove(this);
            if (autorelease) {
                free();
            }
        }

        final void free() {
            if (!released) {
                released = true;
                IO.freeMemory(storage);
            }
        }
    }
}
