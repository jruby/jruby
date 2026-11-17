package org.jruby.ext.ffi;

import junit.framework.TestCase;

import java.nio.ByteOrder;
import java.util.Arrays;

public class ArrayMemoryIOTest extends TestCase {
    public void testPackedValuesRoundTrip() {
        ArrayMemoryIO memory = new ArrayMemoryIO(null, 16);
        short[] shorts = {Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE};
        long[] longs = {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE};

        for (short value : shorts) {
            memory.putShort(0, value);
            assertEquals(value, memory.getShort(0));
        }

        for (long value : longs) {
            memory.putLong(0, value);
            assertEquals(value, memory.getLong(0));
        }
    }

    public void testPackedValuesUseNativeByteOrder() {
        ArrayMemoryIO memory = new ArrayMemoryIO(null, 16);

        memory.putShort(0, (short) 0x1234);
        memory.putLong(2, 0x0102030405060708L);

        byte[] expected = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN ?
                new byte[] {0x12, 0x34, 1, 2, 3, 4, 5, 6, 7, 8} :
                new byte[] {0x34, 0x12, 8, 7, 6, 5, 4, 3, 2, 1};
        byte[] actual = new byte[expected.length];
        memory.get(0, actual, 0, actual.length);

        assertTrue(Arrays.equals(expected, actual));
    }
}
