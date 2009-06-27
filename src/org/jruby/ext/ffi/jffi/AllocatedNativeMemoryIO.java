package org.jruby.ext.ffi.jffi;

import org.jruby.Ruby;
import org.jruby.ext.ffi.AllocatedDirectMemoryIO;

final class AllocatedNativeMemoryIO extends BoundedNativeMemoryIO implements AllocatedDirectMemoryIO {
    private volatile boolean released = false;
    private volatile boolean autorelease = true;

    /** The real memory address */
    private final long storage;

    /**
     * Allocates native memory
     *
     * @param runtime The Ruby runtime
     * @param size The number of bytes to allocate
     * @param clear Whether the memory should be cleared (zeroed)
     * @return A new {@link AllocatedDirectMemoryIO}
     */
    static final AllocatedNativeMemoryIO allocate(Ruby runtime, int size, boolean clear) {
        long memory = IO.allocateMemory(size, clear);
        return memory != 0 ? new AllocatedNativeMemoryIO(runtime, memory, size, 1) : null;
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
        long memory = IO.allocateMemory(size + align - 1, clear);
        return memory != 0 ? new AllocatedNativeMemoryIO(runtime, memory, size, align) : null;
    }
    
    private AllocatedNativeMemoryIO(Ruby runtime, long address, int size, int align) {
        super(runtime, ((address - 1) & ~(align - 1)) + align, size);
        this.storage = address;
    }

    public void free() {
        if (!released) {
            IO.freeMemory(storage);
            released = true;
        }
    }

    public void setAutoRelease(boolean release) {
        this.autorelease = release;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (!released && autorelease) {
                IO.freeMemory(storage);
                released = true;
            }
        } finally {
            super.finalize();
        }
    }
}
