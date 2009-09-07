package org.jruby.ext.ffi.jffi;

import java.util.concurrent.atomic.AtomicBoolean;
import org.jruby.Ruby;
import org.jruby.ext.ffi.AllocatedDirectMemoryIO;

final class AllocatedNativeMemoryIO extends BoundedNativeMemoryIO implements AllocatedDirectMemoryIO {
    private final AtomicBoolean released = new AtomicBoolean(false);
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
        try {
            return address != 0 ? new AllocatedNativeMemoryIO(runtime, address, size, align) : null;
        } catch (Throwable t) {
            IO.freeMemory(address);
            throw new RuntimeException(t);
        }
    }
    
    private AllocatedNativeMemoryIO(Ruby runtime, long address, int size, int align) {
        super(runtime, ((address - 1) & ~(align - 1)) + align, size);
        this.storage = address;
    }

    public void free() {
        if (released.getAndSet(true)) {
            throw getRuntime().newRuntimeError("memory already freed");
        }
        IO.freeMemory(storage);
    }

    public void setAutoRelease(boolean release) {
        this.autorelease = release;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (autorelease && !released.getAndSet(true)) {
                IO.freeMemory(storage);
            }
        } finally {
            super.finalize();
        }
    }
}
