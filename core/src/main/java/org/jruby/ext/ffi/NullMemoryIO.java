

package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyClass;

/**
 * An implementation of MemoryIO that throws an exception on any access.
 */
public class NullMemoryIO extends InvalidMemoryIO {
    public NullMemoryIO(Ruby runtime) {
        super(runtime, true, 0L, "NULL pointer access");
    }

    @Override
    protected RubyClass getErrorClass(Ruby runtime) {
        var context = runtime.getCurrentContext();
        return runtime.getModule("FFI").getClass(context, "NullPointerError");
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
