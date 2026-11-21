package org.jruby.util;

import junit.framework.TestCase;

import static org.jruby.util.BitPacker.pack;
import static org.jruby.util.BitPacker.unpackHigh;
import static org.jruby.util.BitPacker.unpackHighByte;
import static org.jruby.util.BitPacker.unpackHighChar;
import static org.jruby.util.BitPacker.unpackLow;
import static org.jruby.util.BitPacker.unpackLowByte;
import static org.jruby.util.BitPacker.unpackLowChar;

public class BitPackerTest extends TestCase {
    public void testPackAndUnpackSignedIntegers() {
        int[] values = {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE};

        for (int high : values) {
            for (int low : values) {
                long packed = pack(high, low);

                assertEquals(high, unpackHigh(packed));
                assertEquals(low, unpackLow(packed));
            }
        }
    }

    public void testPackAndUnpackCharacters() {
        char[] values = {Character.MIN_VALUE, 1, 0x7fff, 0x8000, Character.MAX_VALUE};

        for (char high : values) {
            for (char low : values) {
                int packed = pack(high, low);

                assertEquals(high, unpackHighChar(packed));
                assertEquals(low, unpackLowChar(packed));
            }
        }
    }

    public void testPackAndUnpackSignedBytes() {
        byte[] values = {Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE};

        for (byte high : values) {
            for (byte low : values) {
                short packed = pack(high, low);

                assertEquals(high, unpackHighByte(packed));
                assertEquals(low, unpackLowByte(packed));
            }
        }
    }
}
