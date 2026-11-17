

package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.api.Access;

/**
 * An implementation of MemoryIO that throws an exception on any access.
 */
public class NullMemoryIO extends InvalidMemoryIO {
    public NullMemoryIO(Ruby runtime) {
        super(runtime, true, 0L, "NULL pointer access");
    }

    @Override
    protected RubyClass getErrorClass(Ruby runtime) {
        return Access.getClass(runtime.getCurrentContext(), "FFI", "NullPointerError");
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MemoryIO && ((MemoryIO) obj).address() == 0;
    }

    @Override
    public int hashCode() {
        return 0;
    }

}
