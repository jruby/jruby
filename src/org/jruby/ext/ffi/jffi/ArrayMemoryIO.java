
package org.jruby.ext.ffi.jffi;

import org.jruby.ext.ffi.MemoryIO;
import org.jruby.ext.ffi.NullMemoryIO;

public final class ArrayMemoryIO extends org.jruby.ext.ffi.ArrayMemoryIO {
    
    public ArrayMemoryIO(int size) {
        super(size);
    }
    public ArrayMemoryIO(byte[] buffer, int offset, int length) {
        super(buffer, offset, length);
    }
    public ArrayMemoryIO slice(long offset) {
        return offset == 0 ? this : new ArrayMemoryIO(array(), arrayOffset() + (int) offset, arrayLength() - (int) offset);
    }
    public final MemoryIO getMemoryIO(long offset) {
        return new NativeMemoryIO(getAddress(offset));
    }

    public final void putMemoryIO(long offset, MemoryIO value) {
        if (value instanceof PointerMemoryIO) {
            putAddress(offset, ((PointerMemoryIO) value).getAddress());
        } else if (value instanceof NullMemoryIO) {
            putAddress(offset, 0L);
        } else {
            throw new RuntimeException("Attempted to get address of non-direct MemoryIO");
        }
    }
}
