
package org.jruby.ext.ffi.jffi;

import java.nio.ByteOrder;
import org.jruby.Ruby;
import org.jruby.ext.ffi.*;

class BoundedNativeMemoryIO extends MemoryIO {
    protected static final com.kenai.jffi.MemoryIO IO = com.kenai.jffi.MemoryIO.getInstance();
    protected static final int LONG_SIZE = Platform.getPlatform().longSize();
    protected static final int ADDRESS_SIZE = Platform.getPlatform().addressSize();

    private final Ruby runtime;
    final long size;
    final MemoryIO parent; // keep a reference to avoid the memory being freed

    BoundedNativeMemoryIO(Ruby runtime, long address, int size) {
        super(true, address);
        this.runtime = runtime;
        this.size = size;
        this.parent = null;
    }

    BoundedNativeMemoryIO(BoundedNativeMemoryIO parent, long offset) {
        super(true, parent.address() + offset);
        this.runtime = parent.runtime;
        this.size = parent.size - offset;
        this.parent = parent;
    }

    BoundedNativeMemoryIO(Ruby runtime, MemoryIO parent, long offset, long size) {
        super(true, parent.address() + offset);
        this.runtime = runtime;
        this.size = size;
        this.parent = parent;
        if (!parent.isDirect()) throw new IllegalArgumentException("non-direct memory");
    }

    private final void checkBounds(long off, long len) {
        Util.checkBounds(runtime, size, off, len);
    }

    public Object array() {
        throw runtime.newRuntimeError("no array");
    }

    public int arrayOffset() {
        throw runtime.newRuntimeError("no array");
    }

    public int arrayLength() {
        throw runtime.newRuntimeError("no array");
    }

    public BoundedNativeMemoryIO slice(long offset) {
        checkBounds(offset, 0);
        return offset == 0 ? this :new BoundedNativeMemoryIO(this, offset);
    }

    public BoundedNativeMemoryIO slice(long offset, long size) {
        checkBounds(offset, size);
        return offset == 0 && size == this.size ? this :new BoundedNativeMemoryIO(runtime, this, offset, size);
    }
    
    public MemoryIO dup() {
        AllocatedNativeMemoryIO tmp = AllocatedNativeMemoryIO.allocate(runtime, (int) size, false);
        
        IO.memcpy(tmp.address, address, size);
        
        return tmp;
    }

    public final java.nio.ByteBuffer asByteBuffer() {
        return IO.newDirectByteBuffer(address, (int) size);
    }

    Ruby getRuntime() {
        return this.runtime;
    }

    @Override
    public final boolean equals(Object obj) {
        return (obj instanceof MemoryIO) && ((MemoryIO) obj).address() == address;
    }

    @Override
    public final int hashCode() {
        int hash = 5;
        hash = 53 * hash + (int) (this.address ^ (this.address >>> 32));
        return hash;
    }

    public final ByteOrder order() {
        return ByteOrder.nativeOrder();
    }

    public final byte getByte(long offset) {
        checkBounds(offset, 1);
        return IO.getByte(address + offset);
    }

    public final short getShort(long offset) {
        checkBounds(offset, 2);
        return IO.getShort(address + offset);
    }

    public final int getInt(long offset) {
        checkBounds(offset, 4);
        return IO.getInt(address + offset);
    }

    public final long getLong(long offset) {
        checkBounds(offset, 8);
        return IO.getLong(address + offset);
    }

    public final long getNativeLong(long offset) {
        return LONG_SIZE == 32 ? getInt(offset) : getLong(offset);
    }

    public final float getFloat(long offset) {
        checkBounds(offset, 4);
        return IO.getFloat(address + offset);
    }

    public final double getDouble(long offset) {
        checkBounds(offset, 8);
        return IO.getDouble(address + offset);
    }

    public final long getAddress(long offset) {
        checkBounds(offset, ADDRESS_SIZE >> 3);
        return IO.getAddress(address + offset);
    }

    public final MemoryIO getMemoryIO(long offset) {
        checkBounds(offset, ADDRESS_SIZE >> 3);
        return NativeMemoryIO.wrap(runtime, IO.getAddress(address + offset));
    }

    public final void putByte(long offset, byte value) {
        checkBounds(offset, 1);
        IO.putByte(address + offset, value);
    }

    public final void putShort(long offset, short value) {
        checkBounds(offset, 2);
        IO.putShort(address + offset, value);
    }

    public final void putInt(long offset, int value) {
        checkBounds(offset, 4);
        IO.putInt(address + offset, value);
    }

    public final void putLong(long offset, long value) {
        checkBounds(offset, 8);
        IO.putLong(address + offset, value);
    }

    public final void putNativeLong(long offset, long value) {
        if (LONG_SIZE == 32) {
            putInt(offset, (int) value);
        } else {
            putLong(offset, value);
        }
    }
    public final void putAddress(long offset, long value) {
        checkBounds(offset, ADDRESS_SIZE >> 3);
        IO.putAddress(address + offset, value);
    }
    public final void putFloat(long offset, float value) {
        checkBounds(offset, 4);
        IO.putFloat(address + offset, value);
    }

    public final void putDouble(long offset, double value) {
        checkBounds(offset, 8);
        IO.putDouble(address + offset, value);
    }

    public final void putMemoryIO(long offset, MemoryIO value) {
        checkBounds(offset, ADDRESS_SIZE >> 3);
        IO.putAddress(address + offset, value.address());
    }

    public final void get(long offset, byte[] dst, int off, int len) {
        checkBounds(offset, len);
        IO.getByteArray(address + offset, dst, off, len);
    }

    public final void put(long offset, byte[] src, int off, int len) {
        checkBounds(offset, len);
        IO.putByteArray(address + offset, src, off, len);
    }

    public final void get(long offset, short[] dst, int off, int len) {
        checkBounds(offset, len << 1);
        IO.getShortArray(address + offset, dst, off, len);
    }

    public final void put(long offset, short[] src, int off, int len) {
        checkBounds(offset, len << 1);
        IO.putShortArray(address + offset, src, off, len);
    }

    public final void get(long offset, int[] dst, int off, int len) {
        checkBounds(offset, len << 2);
        IO.getIntArray(address + offset, dst, off, len);
    }

    public final void put(long offset, int[] src, int off, int len) {
        checkBounds(offset, len << 2);
        IO.putIntArray(address + offset, src, off, len);
    }

    public final void get(long offset, long[] dst, int off, int len) {
        checkBounds(offset, len << 3);
        IO.getLongArray(address + offset, dst, off, len);
    }

    public final void put(long offset, long[] src, int off, int len) {
        checkBounds(offset, len << 3);
        IO.putLongArray(address + offset, src, off, len);
    }

    public final void get(long offset, float[] dst, int off, int len) {
        checkBounds(offset, len << 2);
        IO.getFloatArray(address + offset, dst, off, len);
    }

    public final void put(long offset, float[] src, int off, int len) {
        checkBounds(offset, len << 2);
        IO.putFloatArray(address + offset, src, off, len);
    }

    public final void get(long offset, double[] dst, int off, int len) {
        checkBounds(offset, len << 3);
        IO.getDoubleArray(address + offset, dst, off, len);
    }

    public final void put(long offset, double[] src, int off, int len) {
        checkBounds(offset, len << 3);
        IO.putDoubleArray(address + offset, src, off, len);
    }

    public final int indexOf(long offset, byte value) {
        return value == 0
                ? (int) IO.getStringLength(address + offset)
                : (int) IO.indexOf(address + offset, value);
    }

    public final int indexOf(long offset, byte value, int maxlen) {
        return (int) IO.indexOf(address, value, maxlen);
    }

    public final void setMemory(long offset, long size, byte value) {
        checkBounds(offset, size);
        IO.setMemory(address + offset, size, value);
    }

    public final byte[] getZeroTerminatedByteArray(long offset) {
        checkBounds(offset, 1);
        return IO.getZeroTerminatedByteArray(address + offset);
    }

    public final byte[] getZeroTerminatedByteArray(long offset, int maxlen) {
        checkBounds(offset, 1);
        return IO.getZeroTerminatedByteArray(address + offset,
                Math.min(maxlen, (int) (size - offset)));
    }

    public void putZeroTerminatedByteArray(long offset, byte[] bytes, int off, int len) {
        // Ensure room for terminating zero byte
        checkBounds(offset, len + 1);
        IO.putZeroTerminatedByteArray(address + offset, bytes, off, len);
    }

}
