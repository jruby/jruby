

package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyClass;

/**
 * An implementation of MemoryIO that throws an exception on any access.
 */
public class NullMemoryIO extends InvalidMemoryIO implements DirectMemoryIO {
    public NullMemoryIO(Ruby runtime) {
        super(runtime, "NULL pointer access");
    }

    @Override
    protected RubyClass getErrorClass(Ruby runtime) {
        return runtime.fastGetModule("FFI").fastGetClass("NullPointerError");
    }

    public long getAddress() {
        return 0L;
    }

    public boolean isNull() {
        return true;
    }
    public final boolean isDirect() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DirectMemoryIO && ((DirectMemoryIO) obj).getAddress() == 0;
    }

    @Override
    public int hashCode() {
        return 0;
    }

}
