package org.jruby.ext.ffi.jffi;

import org.jruby.ext.ffi.MemoryIO;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 
 */
final class NilPointerParameterStrategy extends PointerParameterStrategy {
    NilPointerParameterStrategy() {
        super(true, false);
    }

    public final MemoryIO getMemoryIO(Object parameter) {
        return NullMemoryIO.INSTANCE;
    }

    static final class NullMemoryIO extends MemoryIO {
        static final MemoryIO INSTANCE = new NullMemoryIO();

        private NullMemoryIO() {
            super(true, 0L);
        }

        private static final RuntimeException ex() {
            return new UnsupportedOperationException();
        }

        public Object array() {
            throw ex();
        }

        public int arrayOffset() {
            throw ex();
        }

        public int arrayLength() {
            throw ex();
        }

        public ByteOrder order() {
            throw ex();
        }

        public MemoryIO slice(long offset) {
            throw ex();
        }

        public MemoryIO slice(long offset, long size) {
            throw ex();
        }

        public MemoryIO dup() {
            throw ex();
        }

        public ByteBuffer asByteBuffer() {
            throw ex();
        }

        public byte getByte(long offset) {
            throw ex();
        }

        public short getShort(long offset) {
            throw ex();
        }

        public int getInt(long offset) {
            throw ex();
        }

        public long getLong(long offset) {
            throw ex();
        }

        public long getNativeLong(long offset) {
            throw ex();
        }

        public float getFloat(long offset) {
            throw ex();
        }

        public double getDouble(long offset) {
            throw ex();
        }

        public long getAddress(long offset) {
            throw ex();
        }

        public MemoryIO getMemoryIO(long offset) {
            throw ex();
        }

        public void putByte(long offset, byte value) {
            throw ex();
        }

        public void putShort(long offset, short value) {
            throw ex();
        }

        public void putInt(long offset, int value) {
            throw ex();
        }

        public void putLong(long offset, long value) {
            throw ex();
        }

        public void putNativeLong(long offset, long value) {
            throw ex();
        }

        public void putFloat(long offset, float value) {
            throw ex();
        }

        public void putDouble(long offset, double value) {
            throw ex();
        }

        public void putMemoryIO(long offset, MemoryIO value) {
            throw ex();
        }

        public void putAddress(long offset, long value) {
            throw ex();
        }

        public void get(long offset, byte[] dst, int off, int len) {
            throw ex();
        }

        public void put(long offset, byte[] src, int off, int len) {
            throw ex();
        }

        public void get(long offset, short[] dst, int off, int len) {
            throw ex();
        }

        public void put(long offset, short[] src, int off, int len) {
            throw ex();
        }

        public void get(long offset, int[] dst, int off, int len) {
            throw ex();
        }

        public void put(long offset, int[] src, int off, int len) {
            throw ex();
        }

        public void get(long offset, long[] dst, int off, int len) {
            throw ex();
        }

        public void put(long offset, long[] src, int off, int len) {
            throw ex();
        }

        public void get(long offset, float[] dst, int off, int len) {
            throw ex();
        }

        public void put(long offset, float[] src, int off, int len) {
            throw ex();
        }

        public void get(long offset, double[] dst, int off, int len) {
            throw ex();
        }

        public void put(long offset, double[] src, int off, int len) {
            throw ex();
        }

        public int indexOf(long offset, byte value) {
            throw ex();
        }

        public int indexOf(long offset, byte value, int maxlen) {
            throw ex();
        }

        public void setMemory(long offset, long size, byte value) {
            throw ex();
        }

        public byte[] getZeroTerminatedByteArray(long offset) {
            throw ex();
        }

        public byte[] getZeroTerminatedByteArray(long offset, int maxlen) {
            throw ex();
        }

        public void putZeroTerminatedByteArray(long offset, byte[] bytes, int off, int len) {
            throw ex();
        }
    }
}
