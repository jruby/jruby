

package org.jruby.ext.ffi;

import org.jruby.Ruby;

/**
 * An implementation of MemoryIO that throws an exception on any access.
 */
public class NullMemoryIO extends InvalidMemoryIO {
    public NullMemoryIO(Ruby runtime) {
        super(runtime, "NULL pointer access");
    }
    public long getAddress() {
        return 0L;
    }

    public boolean isNull() {
        return true;
    }
}
