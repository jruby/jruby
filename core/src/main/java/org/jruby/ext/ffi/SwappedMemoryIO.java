/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ext.ffi;

import java.nio.ByteOrder;
import org.jruby.Ruby;

public final class SwappedMemoryIO extends MemoryIO {
    protected static final int LONG_SIZE = Platform.getPlatform().longSize();
    protected static final int ADDRESS_SIZE = Platform.getPlatform().addressSize();

    private final Ruby runtime;
    private final MemoryIO io;

    SwappedMemoryIO(Ruby runtime, MemoryIO io) {
        super(io.isDirect(), io.address());
        this.runtime = runtime;
        this.io = io;
    }

    public final ByteOrder order() {
        return ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN)
                ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
    }

    public Object array() {
        return io.array();
    }

    public int arrayOffset() {
        return io.arrayOffset();
    }

    public int arrayLength() {
        return io.arrayLength();
    }

    public SwappedMemoryIO slice(long offset) {
        return offset == 0 ? this : new SwappedMemoryIO(runtime, io.slice(offset));
    }

    public SwappedMemoryIO slice(long offset, long size) {
        return new SwappedMemoryIO(runtime, io.slice(offset, size));
    }
    
    public SwappedMemoryIO dup() {
        return new SwappedMemoryIO(runtime, io.dup());
    }

    public final java.nio.ByteBuffer asByteBuffer() {
        return io.asByteBuffer().order(order());
    }

    Ruby getRuntime() {
        return this.runtime;
    }

    @Override
    public final boolean equals(Object obj) {
        return obj == this || (obj instanceof SwappedMemoryIO) && ((SwappedMemoryIO) obj).io.equals(io);
    }

    @Override
    public final int hashCode() {
        return io.hashCode();
    }

    public final byte getByte(long offset) {
        return io.getByte(offset);
    }

    public final short getShort(long offset) {
        return Short.reverseBytes(io.getShort(offset));
    }

    public final int getInt(long offset) {
        return Integer.reverseBytes(io.getInt(offset));
    }

    public final long getLong(long offset) {
        return Long.reverseBytes(io.getLong(offset));
    }

    public final long getNativeLong(long offset) {
        return LONG_SIZE == 32 ? getInt(offset) : getLong(offset);
    }

    public final float getFloat(long offset) {
        return Float.intBitsToFloat(Integer.reverseBytes(Float.floatToRawIntBits(io.getFloat(offset))));
    }

    public final double getDouble(long offset) {
        return Double.longBitsToDouble(Long.reverseBytes(Double.doubleToRawLongBits(io.getDouble(offset))));
    }

    public final long getAddress(long offset) {
        throw runtime.newRuntimeError("cannot get native address values in non-native byte order memory");
    }

    public final MemoryIO getMemoryIO(long offset) {
        throw runtime.newRuntimeError("cannot get native address values in non-native byte order memory");
    }

    public final void putByte(long offset, byte value) {
        io.putByte(offset, value);
    }

    public final void putShort(long offset, short value) {
        io.putShort(offset, Short.reverseBytes(value));
    }

    public final void putInt(long offset, int value) {
        io.putInt(offset, Integer.reverseBytes(value));
    }

    public final void putLong(long offset, long value) {
        io.putLong(offset, Long.reverseBytes(value));
    }

    public final void putNativeLong(long offset, long value) {
        if (LONG_SIZE == 32) {
            putInt(offset, (int) value);
        } else {
            putLong(offset, value);
        }
    }
    public final void putAddress(long offset, long value) {
        throw runtime.newRuntimeError("cannot write native address values to non-native byte order memory");
    }
    
    public final void putFloat(long offset, float value) {
        io.putFloat(offset, Float.intBitsToFloat(Integer.reverseBytes(Float.floatToRawIntBits(value))));
    }

    public final void putDouble(long offset, double value) {
        io.putDouble(offset, Double.longBitsToDouble(Long.reverseBytes(Double.doubleToRawLongBits(value))));
    }

    public final void putMemoryIO(long offset, MemoryIO value) {
        throw runtime.newRuntimeError("cannot write native address values to non-native byte order memory");
    }

    public final void get(long offset, byte[] dst, int off, int len) {
        io.get(offset, dst, off, len);
    }

    public final void put(long offset, byte[] src, int off, int len) {
        io.put(offset, src, off, len);
    }

    public final void get(long offset, short[] dst, int off, int len) {
        io.get(offset, dst, off, len);
        for (int i = 0; i < len; ++i) {
            dst[off + i] = Short.reverseBytes(dst[off + i]);
        }
    }

    public final void put(long offset, short[] src, int off, int len) {
        short[] values = new short[len];
        for (int i = 0; i < len; ++i) {
            values[i] = Short.reverseBytes(src[off + i]);
        }
        io.put(offset, values, 0, len);
    }

    public final void get(long offset, int[] dst, int off, int len) {
        io.get(offset, dst, off, len);
        for (int i = 0; i < len; ++i) {
            dst[off + i] = Integer.reverseBytes(dst[off + i]);
        }
    }

    public final void put(long offset, int[] src, int off, int len) {
        int[] values = new int[len];
        for (int i = 0; i < len; ++i) {
            values[i] = Integer.reverseBytes(src[off + i]);
        }
        io.put(offset, values, 0, len);
    }

    public final void get(long offset, long[] dst, int off, int len) {
        io.get(offset, dst, off, len);
        for (int i = 0; i < len; ++i) {
            dst[off + i] = Long.reverseBytes(dst[off + i]);
        }
    }

    public final void put(long offset, long[] src, int off, int len) {
        long[] values = new long[len];
        for (int i = 0; i < len; ++i) {
            values[i] = Long.reverseBytes(src[off + i]);
        }
        io.put(offset, values, 0, len);
    }

    public final void get(long offset, float[] dst, int off, int len) {
        io.get(offset, dst, off, len);
        for (int i = 0; i < len; ++i) {
            dst[off + i] = Float.intBitsToFloat(Integer.reverseBytes(Float.floatToRawIntBits(dst[off + i])));
        }
    }

    public final void put(long offset, float[] src, int off, int len) {
        int[] values = new int[len];
        for (int i = 0; i < len; ++i) {
            values[i] = Integer.reverseBytes(Float.floatToRawIntBits(src[off + i]));
        }
        io.put(offset, values, 0, len);
    }

    public final void get(long offset, double[] dst, int off, int len) {
        io.get(offset, dst, off, len);
        for (int i = 0; i < len; ++i) {
            dst[off + i] = Double.longBitsToDouble(Long.reverseBytes(Double.doubleToRawLongBits(dst[off + i])));
        }
    }

    public final void put(long offset, double[] src, int off, int len) {
        long[] values = new long[len];
        for (int i = 0; i < len; ++i) {
            values[i] = Long.reverseBytes(Double.doubleToRawLongBits(src[off + i]));
        }
        io.put(offset, values, 0, len);
    }

    public final int indexOf(long offset, byte value) {
        return io.indexOf(offset, value);
    }

    public final int indexOf(long offset, byte value, int maxlen) {
        return io.indexOf(offset, value, maxlen);
    }

    public final void setMemory(long offset, long size, byte value) {
        io.setMemory(offset, size, value);
    }

    public final byte[] getZeroTerminatedByteArray(long offset) {
        return io.getZeroTerminatedByteArray(offset);
    }

    public final byte[] getZeroTerminatedByteArray(long offset, int maxlen) {
        return io.getZeroTerminatedByteArray(offset, maxlen);
    }

    public void putZeroTerminatedByteArray(long offset, byte[] bytes, int off, int len) {
        io.putZeroTerminatedByteArray(offset, bytes, off, len);
    }

}
