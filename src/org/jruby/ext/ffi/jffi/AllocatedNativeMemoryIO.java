package org.jruby.ext.ffi.jffi;

import org.jruby.ext.ffi.AllocatedDirectMemoryIO;

final class AllocatedNativeMemoryIO extends NativeMemoryIO implements AllocatedDirectMemoryIO {
    private final int size;
    private volatile boolean released = false;
    private volatile boolean autorelease = true;

    static final AllocatedNativeMemoryIO allocate(int size, boolean clear) {
        long memory = IO.allocateMemory(size, clear);
        return memory != 0 ? new AllocatedNativeMemoryIO(memory, size) : null;
    }
    private AllocatedNativeMemoryIO(long address, int size) {
        super(address);
        this.size = size;
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
