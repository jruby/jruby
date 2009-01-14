package org.jruby.ext.ffi.jna;

import com.sun.jna.Memory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jruby.ext.ffi.AllocatedDirectMemoryIO;

final class AllocatedNativeMemoryIO extends NativeMemoryIO implements AllocatedDirectMemoryIO {

    /**
     * Used to hold a permanent reference to a memory pointer so it does not get
     * garbage collected
     */
    private static final Map<AllocatedNativeMemoryIO, Object> pointerSet
            = new ConcurrentHashMap<AllocatedNativeMemoryIO, Object>();

    /**
     * Allocates a new block of native memory and wraps it in a {@link MemoryIO}
     * accessor.
     *
     * @param size The size in bytes of memory to allocate.
     *
     * @return A new <tt>MemoryIO</tt> instance that can access the memory.
     */
    static final AllocatedNativeMemoryIO allocate(int size, boolean clear) {
        Memory memory = new Memory(size);
        if (clear) {
            memory.setMemory(0, size, (byte) 0);
        }
        return new AllocatedNativeMemoryIO(memory, size);
    }

    public AllocatedNativeMemoryIO(Memory memory, int size) {
        super(memory);
    }

    public final void free() {
        // Just let the GC collect and free the pointer
        pointerSet.remove(this);
    }

    public final void setAutoRelease(boolean autorelease) {
        if (autorelease) {
            pointerSet.remove(this);
        } else {
            pointerSet.put(this, Boolean.TRUE);
        }
    }
}
