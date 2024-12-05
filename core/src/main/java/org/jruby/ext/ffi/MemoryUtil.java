package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyFloat;
import org.jruby.RubyString;
import org.jruby.api.Create;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public final class MemoryUtil {
    private MemoryUtil() {}

    public static IRubyObject getArrayOfSigned8(ThreadContext context, MemoryIO io, long offset, int count) {
        byte[] array = new byte[count];
        if (array.length > 0) {
            io.get(offset, array, 0, array.length);
        }

        var objArray = new IRubyObject[count];
        for (int i = 0; i < array.length; ++i) {
            objArray[i] = Util.newSigned8(context.runtime, array[i]);
        }

        return Create.newArrayNoCopy(context, objArray);
    }

    public static void putArrayOfSigned8(MemoryIO io, long offset, RubyArray ary) {
        byte[] array = new byte[ary.size()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = Util.int8Value(ary.entry(i));
        }

        if (array.length > 0) {
            io.put(offset, array, 0, array.length);
        }
    }

    public static IRubyObject getArrayOfUnsigned8(ThreadContext context, MemoryIO io, long offset, int count) {
        byte[] array = new byte[count];
        if (array.length > 0) {
            io.get(offset, array, 0, array.length);
        }

        var objArray = new IRubyObject[count];
        for (int i = 0; i < array.length; ++i) {
            objArray[i] = Util.newUnsigned8(context.runtime, array[i]);
        }

        return Create.newArrayNoCopy(context, objArray);
    }

    public static void putArrayOfUnsigned8(MemoryIO io, long offset, RubyArray ary) {
        byte[] array = new byte[ary.size()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = (byte) Util.uint8Value(ary.entry(i));
        }

        if (array.length > 0) {
            io.put(offset, array, 0, array.length);
        }
    }

    public static IRubyObject getArrayOfSigned16(ThreadContext context, MemoryIO io, long offset, int count) {
        short[] array = new short[count];
        if (array.length > 0) {
            io.get(offset, array, 0, array.length);
        }

        var objArray = new IRubyObject[count];
        for (int i = 0; i < array.length; ++i) {
            objArray[i] = Util.newSigned16(context.runtime, array[i]);
        }

        return Create.newArrayNoCopy(context, objArray);
    }
    
    public static void putArrayOfSigned16(MemoryIO io, long offset, RubyArray ary) {
        short[] array = new short[ary.size()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = Util.int16Value(ary.entry(i));
        }

        if (array.length > 0) {
            io.put(offset, array, 0, array.length);
        }
    }

    public static IRubyObject getArrayOfUnsigned16(ThreadContext context, MemoryIO io, long offset, int count) {
        short[] array = new short[count];
        if (array.length > 0) {
            io.get(offset, array, 0, array.length);
        }

        var objArray = new IRubyObject[count];
        for (int i = 0; i < array.length; ++i) {
            objArray[i] = Util.newUnsigned16(context.runtime, array[i]);
        }

        return Create.newArrayNoCopy(context, objArray);
    }

    public static void putArrayOfUnsigned16(MemoryIO io, long offset, RubyArray ary) {
        short[] array = new short[ary.size()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = (short) Util.uint16Value(ary.entry(i));
        }

        if (array.length > 0) {
            io.put(offset, array, 0, array.length);
        }
    }

    public static IRubyObject getArrayOfSigned32(ThreadContext context, MemoryIO io, long offset, int count) {

        int[] array = new int[count];
        if (array.length > 0) {
            io.get(offset, array, 0, array.length);
        }

        var objArray = new IRubyObject[count];
        for (int i = 0; i < array.length; ++i) {
            objArray[i] = Util.newSigned32(context.runtime, array[i]);
        }

        return Create.newArrayNoCopy(context, objArray);
    }

    public static void putArrayOfSigned32(MemoryIO io, long offset, RubyArray ary) {
        int[] array = new int[ary.size()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = Util.int32Value(ary.entry(i));
        }

        if (array.length > 0) {
            io.put(offset, array, 0, array.length);
        }
    }

    public static IRubyObject getArrayOfUnsigned32(ThreadContext context, MemoryIO io, long offset, int count) {
        int[] array = new int[count];
        if (array.length > 0) {
            io.get(offset, array, 0, array.length);
        }

        var objArray = new IRubyObject[count];
        for (int i = 0; i < array.length; ++i) {
            objArray[i] = Util.newUnsigned32(context.runtime, array[i]);
        }

        return Create.newArrayNoCopy(context, objArray);
    }

    public static void putArrayOfUnsigned32(MemoryIO io, long offset, RubyArray ary) {
        int[] array = new int[ary.size()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = (int) Util.uint32Value(ary.entry(i));
        }

        if (array.length > 0) {
            io.put(offset, array, 0, array.length);
        }
    }

    public static IRubyObject getArrayOfSigned64(ThreadContext context, MemoryIO io, long offset, int count) {
        long[] array = new long[count];
        if (array.length > 0) {
            io.get(offset, array, 0, array.length);
        }

        var objArray = new IRubyObject[count];
        for (int i = 0; i < array.length; ++i) {
            objArray[i] = Util.newSigned64(context.runtime, array[i]);
        }

        return Create.newArrayNoCopy(context, objArray);
    }

    public static void putArrayOfSigned64(MemoryIO io, long offset, RubyArray ary) {

        long[] array = new long[ary.size()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = Util.int64Value(ary.entry(i));
        }

        if (array.length > 0) {
            io.put(offset, array, 0, array.length);
        }
    }

    public static IRubyObject getArrayOfUnsigned64(ThreadContext context, MemoryIO io, long offset, int count) {
        long[] array = new long[count];
        if (array.length > 0) {
            io.get(offset, array, 0, array.length);
        }

        var objArray = new IRubyObject[count];
        for (int i = 0; i < array.length; ++i) {
            objArray[i] = Util.newUnsigned64(context.runtime, array[i]);
        }

        return Create.newArrayNoCopy(context, objArray);
    }

    public static void putArrayOfUnsigned64(MemoryIO io, long offset, RubyArray ary) {
        long[] array = new long[ary.size()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = Util.uint64Value(ary.entry(i));
        }

        if (array.length > 0) {
            io.put(offset, array, 0, array.length);
        }
    }

    public static IRubyObject getArrayOfFloat32(ThreadContext context, MemoryIO io, long offset, int count) {
        float[] array = new float[count];
        if (array.length > 0) {
            io.get(offset, array, 0, array.length);
        }

        var objArray = new IRubyObject[count];
        for (int i = 0; i < array.length; ++i) {
            objArray[i] = RubyFloat.newFloat(context.runtime, array[i]);
        }

        return Create.newArrayNoCopy(context, objArray);
    }

    public static void putArrayOfFloat32(MemoryIO io, long offset, RubyArray ary) {
        float[] array = new float[ary.size()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = Util.floatValue(ary.entry(i));
        }

        if (array.length > 0) {
            io.put(offset, array, 0, array.length);
        }
    }

    public static IRubyObject getArrayOfFloat64(ThreadContext context, MemoryIO io, long offset, int count) {
        double[] array = new double[count];
        if (array.length > 0) {
            io.get(offset, array, 0, array.length);
        }

        var objArray = new IRubyObject[count];
        for (int i = 0; i < array.length; ++i) {
            objArray[i] = RubyFloat.newFloat(context.runtime, array[i]);
        }

        return Create.newArrayNoCopy(context, objArray);
    }

    public static void putArrayOfFloat64(MemoryIO io, long offset, RubyArray ary) {
        double[] array = new double[ary.size()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = Util.doubleValue(ary.entry(i));
        }

        if (array.length > 0) {
            io.put(offset, array, 0, array.length);
        }
    }

    /**
     * Creates a ruby string from a byte array
     *
     * @param runtime The ruby runtime
     * @param bytes The array to make into a ruby string.
     * @return A ruby string.
     */
    public static RubyString newTaintedString(Ruby runtime, byte[] bytes) {
        RubyString s = RubyString.newStringNoCopy(runtime, bytes);
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
    public static RubyString getTaintedByteString(Ruby runtime, MemoryIO io, long offset, int length) {
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
    public static IRubyObject getTaintedString(Ruby runtime, MemoryIO io, long offset) {
        return newTaintedString(runtime, io.getZeroTerminatedByteArray(offset));
    }

    /**
     * Reads a NUL terminated string from a memory object
     *
     * @param runtime The ruby runtime
     * @param io The memory object to read the string from
     * @param offset The offset within the memory object to start reading
     * @param length The maximum number of bytes to read
     * @return A ruby string
     */
    public static IRubyObject getTaintedString(Ruby runtime, MemoryIO io, long offset, int length) {
        return newTaintedString(runtime, io.getZeroTerminatedByteArray(offset, length));
    }
}
