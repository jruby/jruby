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
    private static final long HIGH_BITS = 0x8080808080808080L;
    private static final long LOW_SEVEN_BITS = 0x7F7F7F7F7F7F7F7FL;

    /** Load up to eight bytes, zero padded at the top. */
    public static long loadPartial(byte[] bytes, int offset, int length) {
        long word = 0;
        for (int i = 0; i < length; i++) word |= Byte.toUnsignedLong(bytes[offset + i]) << (i << 3);
        return word;
    }

    /** Mask of the padding above the first length (less than eight) bytes of a {@link #loadPartial} word. */
    public static long padding(int length) {
        return HIGH_BITS & (-1L << (length << 3));
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

    /**
     * Move the bytes of word not selected by drop down to the bottom, in order. Walks whichever of the
     * dropped or kept bytes are fewer, so at most four steps. (Long.compress does this in one call, but
     * without BMI2 or SVE2, as on Apple M1, it measured slower than either loop.)
     */
    public static long squash(long word, long drop) {
        if (drop == 0) return word;
        if (Long.bitCount(drop) > 4) {
            long kept = 0;
            int to = 0;
            for (long keep = ~drop & HIGH_BITS; keep != 0; keep &= keep - 1, to += 8) {
                kept |= ((word >>> (Long.numberOfTrailingZeros(keep) & ~7)) & 0xFF) << to;
            }
            return kept;
        }
        while (drop != 0) {
            int high = 63 - Long.numberOfLeadingZeros(drop);
            // remove that byte: keep the bytes below it and shift the ones above it down
            long below = (1L << (high & ~7)) - 1;
            word = (word & below) | ((word >>> 8) & ~below);
            drop &= ~(1L << high);
        }
        return word;
    }

    /** Write the bytes of word not selected by drop at to and return the new end; always stores eight bytes. */
    public static int compact(byte[] bytes, int to, long word, long drop) {
        LE_LONGS.set(bytes, to, squash(word, drop));
        return to + 8 - Long.bitCount(drop);
    }

    /** Like {@link #compact} but stores only the kept bytes, for a final partial word; drop must include its padding. */
    public static int compactPartial(byte[] bytes, int to, long word, long drop) {
        word = squash(word, drop);
        for (int kept = 8 - Long.bitCount(drop); kept > 0; kept--) {
            bytes[to++] = (byte) word;
            word >>>= 8;
        }
        return to;
    }
}
