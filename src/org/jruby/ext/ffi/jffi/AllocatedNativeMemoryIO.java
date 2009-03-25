package org.jruby.ext.ffi.jffi;

import org.jruby.Ruby;
import org.jruby.ext.ffi.AllocatedDirectMemoryIO;

final class AllocatedNativeMemoryIO extends BoundedNativeMemoryIO implements AllocatedDirectMemoryIO {
    private volatile boolean released = false;
    private volatile boolean autorelease = true;

    static final AllocatedNativeMemoryIO allocate(Ruby runtime, int size, boolean clear) {
        long memory = IO.allocateMemory(size, clear);
        return memory != 0 ? new AllocatedNativeMemoryIO(runtime, memory, size) : null;
    }
    private AllocatedNativeMemoryIO(Ruby runtime, long address, int size) {
        super(runtime, address, size);
    }

    public void free() {
        if (!released) {
            IO.freeMemory(address);
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
                IO.freeMemory(address);
                released = true;
            }
        } finally {
            super.finalize();
        }
    }
}
