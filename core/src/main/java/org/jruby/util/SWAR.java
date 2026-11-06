package org.jruby.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

/**
 * SIMD within a register: eight bytes in a little-endian long, so byte i of a word is bytes[offset + i].
 * Byte masks have the high bit of each selected byte set.
 */
public final class SWAR {
    private SWAR() {}

    // used directly, since wrapping it in a load method measured a few percent slower in String#tr
    public static final VarHandle LE_LONGS =
            MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    private static final long LOW_BITS = 0x0101010101010101L;
    private static final long LOW_SEVEN_BITS = 0x7F7F7F7F7F7F7F7FL;

    /** Load up to eight bytes, zero padded at the top. */
    public static long loadPartial(byte[] bytes, int offset, int length) {
        long word = 0;
        for (int i = 0; i < length; i++) word |= Byte.toUnsignedLong(bytes[offset + i]) << (i << 3);
        return word;
    }

    public static long broadcast(byte b) {
        return Byte.toUnsignedLong(b) * LOW_BITS;
    }

    /**
     * Mask of the bytes in word equal to those in pattern. This is the exact form of "determine if a word
     * has a zero byte" from Bit Twiddling Hacks, so there are no false positives from borrows.
     */
    public static long equal(long word, long pattern) {
        long x = word ^ pattern;
        return ~(((x & LOW_SEVEN_BITS) + LOW_SEVEN_BITS) | x | LOW_SEVEN_BITS);
    }
}
