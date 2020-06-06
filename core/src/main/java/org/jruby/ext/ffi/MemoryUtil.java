package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyFloat;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;

public final class MemoryUtil {
    private MemoryUtil() {}

    public static final IRubyObject getArrayOfSigned8(Ruby runtime, MemoryIO io, long offset, int count) {

        byte[] array = new byte[count];
        io.get(offset, array, 0, array.length);
        
        RubyArray arr = RubyArray.newArray(runtime, array.length);
        for (int i = 0; i < array.length; ++i) {
            arr.add(Util.newSigned8(runtime, array[i]));
        }

        return arr;
    }

    public static final void putArrayOfSigned8(Ruby runtime, MemoryIO io, long offset, RubyArray ary) {

        byte[] array = new byte[ary.size()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = Util.int8Value(ary.entry(i));
        }

        io.put(offset, array, 0, array.length);
    }

    public static final IRubyObject getArrayOfUnsigned8(Ruby runtime, MemoryIO io, long offset, int count) {

        byte[] array = new byte[count];
        io.get(offset, array, 0, array.length);

        RubyArray arr = RubyArray.newArray(runtime, array.length);
        for (int i = 0; i < array.length; ++i) {
            arr.add(Util.newUnsigned8(runtime, array[i]));
        }

        return arr;
    }

    public static final void putArrayOfUnsigned8(Ruby runtime, MemoryIO io, long offset, RubyArray ary) {

        byte[] array = new byte[ary.size()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = (byte) Util.uint8Value(ary.entry(i));
        }

        io.put(offset, array, 0, array.length);
    }

    public static final IRubyObject getArrayOfSigned16(Ruby runtime, MemoryIO io, long offset, int count) {

        short[] array = new short[count];
        io.get(offset, array, 0, array.length);

        RubyArray arr = RubyArray.newArray(runtime, array.length);
        for (int i = 0; i < array.length; ++i) {
            arr.add(Util.newSigned16(runtime, array[i]));
        }

        return arr;
    }
    
    public static final void putArrayOfSigned16(Ruby runtime, MemoryIO io, long offset, RubyArray ary) {

        short[] array = new short[ary.size()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = Util.int16Value(ary.entry(i));
        }

        io.put(offset, array, 0, array.length);
    }

    public static final IRubyObject getArrayOfUnsigned16(Ruby runtime, MemoryIO io, long offset, int count) {

        short[] array = new short[count];
        io.get(offset, array, 0, array.length);

        RubyArray arr = RubyArray.newArray(runtime, array.length);
        for (int i = 0; i < array.length; ++i) {
            arr.add(Util.newUnsigned16(runtime, array[i]));
        }

        return arr;
    }

    public static final void putArrayOfUnsigned16(Ruby runtime, MemoryIO io, long offset, RubyArray ary) {

        short[] array = new short[ary.size()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = (short) Util.uint16Value(ary.entry(i));
        }

        io.put(offset, array, 0, array.length);
    }

    public static final IRubyObject getArrayOfSigned32(Ruby runtime, MemoryIO io, long offset, int count) {

        int[] array = new int[count];
        io.get(offset, array, 0, array.length);

        RubyArray arr = RubyArray.newArray(runtime, array.length);
        for (int i = 0; i < array.length; ++i) {
            arr.add(Util.newSigned32(runtime, array[i]));
        }

        return arr;
    }

    public static final void putArrayOfSigned32(Ruby runtime, MemoryIO io, long offset, RubyArray ary) {

        int[] array = new int[ary.size()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = Util.int32Value(ary.entry(i));
        }

        io.put(offset, array, 0, array.length);
    }
    
    public static final IRubyObject getArrayOfUnsigned32(Ruby runtime, MemoryIO io, long offset, int count) {

        int[] array = new int[count];
        io.get(offset, array, 0, array.length);

        RubyArray arr = RubyArray.newArray(runtime, array.length);
        for (int i = 0; i < array.length; ++i) {
            arr.add(Util.newUnsigned32(runtime, array[i]));
        }

        return arr;
    }

    public static final void putArrayOfUnsigned32(Ruby runtime, MemoryIO io, long offset, RubyArray ary) {

        int[] array = new int[ary.size()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = (int) Util.uint32Value(ary.entry(i));
        }

        io.put(offset, array, 0, array.length);
    }

    public static final IRubyObject getArrayOfSigned64(Ruby runtime, MemoryIO io, long offset, int count) {

        long[] array = new long[count];
        io.get(offset, array, 0, array.length);

        RubyArray arr = RubyArray.newArray(runtime, array.length);
        for (int i = 0; i < array.length; ++i) {
            arr.add(Util.newSigned64(runtime, array[i]));
        }

        return arr;
    }

    public static final void putArrayOfSigned64(Ruby runtime, MemoryIO io, long offset, RubyArray ary) {

        long[] array = new long[ary.size()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = Util.int64Value(ary.entry(i));
        }

        io.put(offset, array, 0, array.length);
    }

    public static final IRubyObject getArrayOfUnsigned64(Ruby runtime, MemoryIO io, long offset, int count) {

        long[] array = new long[count];
        io.get(offset, array, 0, array.length);

        RubyArray arr = RubyArray.newArray(runtime, array.length);
        for (int i = 0; i < array.length; ++i) {
            arr.add(Util.newUnsigned64(runtime, array[i]));
        }

        return arr;
    }

    public static final void putArrayOfUnsigned64(Ruby runtime, MemoryIO io, long offset, RubyArray ary) {

        long[] array = new long[ary.size()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = Util.uint64Value(ary.entry(i));
        }

        io.put(offset, array, 0, array.length);
    }

    public static final IRubyObject getArrayOfFloat32(Ruby runtime, MemoryIO io, long offset, int count) {

        float[] array = new float[count];
        io.get(offset, array, 0, array.length);

        RubyArray arr = RubyArray.newArray(runtime, array.length);
        for (int i = 0; i < array.length; ++i) {
            arr.add(RubyFloat.newFloat(runtime, array[i]));
        }

        return arr;
    }

    public static final void putArrayOfFloat32(Ruby runtime, MemoryIO io, long offset, RubyArray ary) {

        float[] array = new float[ary.size()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = Util.floatValue(ary.entry(i));
        }

        io.put(offset, array, 0, array.length);
    }

    public static final IRubyObject getArrayOfFloat64(Ruby runtime, MemoryIO io, long offset, int count) {

        double[] array = new double[count];
        io.get(offset, array, 0, array.length);

        RubyArray arr = RubyArray.newArray(runtime, array.length);
        for (int i = 0; i < array.length; ++i) {
            arr.add(RubyFloat.newFloat(runtime, array[i]));
        }

        return arr;
    }

    public static final void putArrayOfFloat64(Ruby runtime, MemoryIO io, long offset, RubyArray ary) {

        double[] array = new double[ary.size()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = Util.doubleValue(ary.entry(i));
        }

        io.put(offset, array, 0, array.length);
    }

    /**
     * Creates a ruby string from a byte array and sets the taint flag on it
     *
     * @param runtime The ruby runtime
     * @param bytes The array to make into a ruby string.
     * @return A ruby string.
     */
    public static final RubyString newTaintedString(Ruby runtime, byte[] bytes) {
        RubyString s = RubyString.newStringNoCopy(runtime, bytes);
        s.setTaint(true);
        return s;
    }

    /**
     * Reads a byte (binary) string from a memory object.
     *
     * @param runtime The ruby runtime
     * @param io The memory object to read the string from
     * @param offset The offset within the memory object to start reading
     * @param length The number of bytes to read
     * @return A ruby string
     */
    public static final RubyString getTaintedByteString(Ruby runtime, MemoryIO io, long offset, int length) {
        byte[] bytes = new byte[length];
        io.get(offset, bytes, 0, bytes.length);
        return newTaintedString(runtime, bytes);
    }

    /**
     * Gets a NUL terminated string from a memory object
     *
     * @param runtime The ruby runtime
     * @param io The memory object to read the string from
     * @param offset The offset within the memory object to start reading
     * @return A ruby string
     */
    public static final IRubyObject getTaintedString(Ruby runtime, MemoryIO io, long offset) {
        return newTaintedString(runtime, io.getZeroTerminatedByteArray(offset));
    }

    /**
     * Reads a NUL terminated string from a memory object
     *
     * @param runtime The ruby runtime
     * @param io The memory object to read the string from
     * @param offset The offset within the memory object to start reading
     * @param maxlen The maximum number of bytes to read
     * @return A ruby string
     */
    public static final IRubyObject getTaintedString(Ruby runtime, MemoryIO io, long offset, int length) {
        return newTaintedString(runtime, io.getZeroTerminatedByteArray(offset, length));
    }
}
