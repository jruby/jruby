package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Closure;
import org.jruby.Ruby;
import org.jruby.ext.ffi.AllocatedDirectMemoryIO;
import org.jruby.ext.ffi.InvalidMemoryIO;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An implementation of MemoryIO that throws exceptions on any attempt to read/write
 * the callback memory area (which is code).
 *
 * This also keeps the callback alive via the handle member, as long as this
 * CallbackMemoryIO instance is contained in a valid Callback pointer.
 */
final class CallbackMemoryIO extends InvalidMemoryIO implements AllocatedDirectMemoryIO {
    private final Closure.Handle handle;
    private volatile boolean released;
    private volatile boolean unmanaged;
    private Object proc;

    public CallbackMemoryIO(Ruby runtime, Closure.Handle handle, Object proc) {
        super(runtime, true, handle.getAddress(), "cannot access closure trampoline memory");
        this.handle = handle;
        this.proc = proc;
    }

    public CallbackMemoryIO(Ruby runtime, Closure.Handle handle) {
        this(runtime, handle, null);
    }

    public synchronized void free() {
        if (!released) {
            this.proc = null;
            handle.dispose();
            released = true;
            unmanaged = true;
        }
    }

    public synchronized void setAutoRelease(boolean autorelease) {
        if (isAutoRelease() != autorelease) {
            handle.setAutoRelease(autorelease);
            unmanaged = !autorelease;
        }
    }

    public boolean isAutoRelease() {
        return !unmanaged;
    }
}
