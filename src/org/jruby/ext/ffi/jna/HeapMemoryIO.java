
package org.jruby.ext.ffi.jna;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import org.jruby.ext.ffi.ArrayMemoryIO;
import org.jruby.ext.ffi.MemoryIO;
import org.jruby.ext.ffi.Platform;

public final class HeapMemoryIO extends ArrayMemoryIO {

    public HeapMemoryIO(int size) {
        super(size);
    }
    protected HeapMemoryIO(byte[] buffer, int offset, int length) {
        super(buffer, offset, length);
    }

    public HeapMemoryIO slice(long offset) {
        return offset == 0 ? this : new HeapMemoryIO(array(), arrayOffset() + (int) offset, arrayLength() - (int) offset);
    }

    public final JNAMemoryIO getMemoryIO(long offset) {
        return JNAMemoryIO.wrap(getPointer(offset));
    }

    public final void putMemoryIO(long offset, MemoryIO value) {
        putPointer(offset, ((JNAMemoryIO) value).getAddress());
    }
    public final Pointer getPointer(long offset) {
        if (Platform.getPlatform().longSize() == 32) {
            IntByReference ref = new IntByReference(getInt(offset));
            return ref.getPointer().getPointer(0);
        } else {
            LongByReference ref = new LongByReference(getLong(offset));
            return ref.getPointer().getPointer(0);
        }
    }


    public final void putPointer(long offset, Pointer value) {
        PointerByReference ref = new PointerByReference(value);
        if (Platform.getPlatform().longSize() == 32) {
            putInt(offset, ref.getPointer().getInt(0));
        } else {
            putLong(offset, ref.getPointer().getLong(0));
        }
    }
}
