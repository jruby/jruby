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
    private final AtomicBoolean released = new AtomicBoolean();
    private Object proc;

    public CallbackMemoryIO(Ruby runtime, Closure.Handle handle, Object proc) {
        super(runtime);
        this.handle = handle;
        this.proc = proc;
    }

    public CallbackMemoryIO(Ruby runtime, Closure.Handle handle) {
        this(runtime, handle, null);
    }

    public final long getAddress() {
        return handle.getAddress();
    }

    public final boolean isNull() {
        return false;
    }

    public final boolean isDirect() {
        return true;
    }

    public void free() {
        if (!released.getAndSet(true)) {
            this.proc = null;
            handle.dispose();
        }
    }

    public void setAutoRelease(boolean autorelease) {
        handle.setAutoRelease(autorelease);
    }
}
