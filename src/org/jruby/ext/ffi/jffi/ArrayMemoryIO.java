
package org.jruby.ext.ffi.jffi;

import org.jruby.ext.ffi.MemoryIO;
import org.jruby.ext.ffi.NullMemoryIO;

public final class ArrayMemoryIO extends org.jruby.ext.ffi.ArrayMemoryIO {
    
    public ArrayMemoryIO(int size) {
        super(size);
    }

    public final MemoryIO getMemoryIO(long offset) {
        return new DirectMemoryIO(getAddress(offset));
    }

    public final void putMemoryIO(long offset, MemoryIO value) {
        if (value instanceof DirectMemoryIO) {
            putAddress(offset, ((DirectMemoryIO) value).getAddress());
        } else if (value instanceof NullMemoryIO) {
            putAddress(offset, 0L);
        } else {
            throw new RuntimeException("Attempted to get address of non-direct MemoryIO");
        }
    }
}
