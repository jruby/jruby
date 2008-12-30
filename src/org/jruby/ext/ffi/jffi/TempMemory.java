package org.jruby.ext.ffi.jffi;

final class TempMemory implements Runnable {

    private static final com.kenai.jffi.MemoryIO IO = com.kenai.jffi.MemoryIO.getInstance();
    final long address;
    private volatile boolean released = false;

    TempMemory(int size, boolean clear) {
        super();
        address = IO.allocateMemory(size, clear);
    }

    public void run() {
        IO.freeMemory(address);
        released = true;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (!released) {
                IO.freeMemory(address);
                released = true;
            }
        } finally {
            super.finalize();
        }
    }
}
