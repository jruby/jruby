
package org.jruby.ext.ffi.jffi;

import java.nio.ByteOrder;
import org.jruby.Ruby;
import org.jruby.ext.ffi.MemoryIO;
import org.jruby.ext.ffi.Platform;

class NativeMemoryIO extends MemoryIO {
    protected static final com.kenai.jffi.MemoryIO IO = com.kenai.jffi.MemoryIO.getInstance();
    final NativeMemoryIO parent; // keep a reference to avoid the memory being freed
    private final Ruby runtime;

    static final MemoryIO wrap(Ruby runtime, long address) {
        return address != 0
                ? new NativeMemoryIO(runtime, address)
                : runtime.getFFI().getNullMemoryIO();
    }

    NativeMemoryIO(Ruby runtime, long address) {
        super(true, address);
        this.runtime = runtime;
        this.parent = null;
    }

    private NativeMemoryIO(NativeMemoryIO parent, long offset) {
        super(true, parent.address + offset);
        this.parent = parent;
        this.runtime = parent.runtime;
    }

    public Object array() {
        throw new UnsupportedOperationException("no array");
    }

    public int arrayOffset() {
        throw new UnsupportedOperationException("no array");
    }

    public int arrayLength() {
        throw new UnsupportedOperationException("no array");
    }

    public NativeMemoryIO slice(long offset) {
        return offset == 0 ? this :new NativeMemoryIO(this, offset);
    }

    public MemoryIO slice(long offset, long size) {
        return new BoundedNativeMemoryIO(runtime, this, offset, size);
    }
    
    public MemoryIO dup() {
        throw runtime.newNotImplementedError("cannot duplicate unbounded memory area");
    }

    public final java.nio.ByteBuffer asByteBuffer() {
        return IO.newDirectByteBuffer(address, Integer.MAX_VALUE);
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
        return IO.getByte(address + offset);
    }

    public final short getShort(long offset) {
        return IO.getShort(address + offset);
    }

    public final int getInt(long offset) {
        return IO.getInt(address + offset);
    }

    public final long getLong(long offset) {
        return IO.getLong(address + offset);
    }

    public final long getNativeLong(long offset) {
        return Platform.getPlatform().longSize() == 32
                ? IO.getInt(address + offset)
                : IO.getLong(address + offset);
    }

    public final float getFloat(long offset) {
        return IO.getFloat(address + offset);
    }

    public final double getDouble(long offset) {
        return IO.getDouble(address + offset);
    }

    public final long getAddress(long offset) {
        return IO.getAddress(address + offset);
    }

    public final MemoryIO getMemoryIO(long offset) {
        return wrap(runtime, IO.getAddress(address + offset));
    }

    public final void putByte(long offset, byte value) {
        IO.putByte(address + offset, value);
    }

    public final void putShort(long offset, short value) {
        IO.putShort(address + offset, value);
    }

    public final void putInt(long offset, int value) {
        IO.putInt(address + offset, value);
    }

    public final void putLong(long offset, long value) {
        IO.putLong(address + offset, value);
    }

    public final void putNativeLong(long offset, long value) {
        if (Platform.getPlatform().longSize() == 32) {
            IO.putInt(address + offset, (int) value);
        } else {
            IO.putLong(address + offset, value);
        }
    }
    public final void putAddress(long offset, long value) {
        IO.putAddress(address + offset, value);
    }
    public final void putFloat(long offset, float value) {
        IO.putFloat(address + offset, value);
    }

    public final void putDouble(long offset, double value) {
        IO.putDouble(address + offset, value);
    }

    public final void putMemoryIO(long offset, MemoryIO value) {
        IO.putAddress(address + offset, value.address());
    }

    public final void get(long offset, byte[] dst, int off, int len) {
        IO.getByteArray(address + offset, dst, off, len);
    }

    public final void put(long offset, byte[] src, int off, int len) {
        IO.putByteArray(address + offset, src, off, len);
    }

    public final void get(long offset, short[] dst, int off, int len) {
        IO.getShortArray(address + offset, dst, off, len);
    }

    public final void put(long offset, short[] src, int off, int len) {
        IO.putShortArray(address + offset, src, off, len);
    }

    public final void get(long offset, int[] dst, int off, int len) {
        IO.getIntArray(address + offset, dst, off, len);
    }

    public final void put(long offset, int[] src, int off, int len) {
        IO.putIntArray(address + offset, src, off, len);
    }

    public final void get(long offset, long[] dst, int off, int len) {
        IO.getLongArray(address + offset, dst, off, len);
    }

    public final void put(long offset, long[] src, int off, int len) {
        IO.putLongArray(address + offset, src, off, len);
    }

    public final void get(long offset, float[] dst, int off, int len) {
        IO.getFloatArray(address + offset, dst, off, len);
    }

    public final void put(long offset, float[] src, int off, int len) {
        IO.putFloatArray(address + offset, src, off, len);
    }

    public final void get(long offset, double[] dst, int off, int len) {
        IO.getDoubleArray(address + offset, dst, off, len);
    }

    public final void put(long offset, double[] src, int off, int len) {
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
        IO.setMemory(address + offset, size, value);
    }

    public final byte[] getZeroTerminatedByteArray(long offset) {
        return IO.getZeroTerminatedByteArray(address + offset);
    }

    public final byte[] getZeroTerminatedByteArray(long offset, int maxlen) {
        return IO.getZeroTerminatedByteArray(address + offset, maxlen);
    }

    public void putZeroTerminatedByteArray(long offset, byte[] bytes, int off, int len) {
        IO.putZeroTerminatedByteArray(address + offset, bytes, off, len);
    }

}
