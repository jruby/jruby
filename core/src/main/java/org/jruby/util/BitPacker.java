package org.jruby.util;

/**
 * Utility methods for packing pairs of fixed-width values.
 */
public final class BitPacker {
    private BitPacker() {
    }

    /**
     * Packs {@code high} into the high 32 bits and {@code low} into the low 32 bits.
     */
    public static long pack(int high, int low) {
        return packInts(high, low);
    }

    /**
     * Packs {@code high} into the high 32 bits and {@code low} into the low 32 bits.
     */
    public static long packInts(int high, int low) {
        return (long) high << 32 | low & 0xffffffffL;
    }

    /**
     * Returns the signed integer stored in the high 32 bits of {@code packed}.
     */
    public static int unpackHigh(long packed) {
        return (int) (packed >> 32);
    }

    /**
     * Returns the signed integer stored in the low 32 bits of {@code packed}.
     */
    public static int unpackLow(long packed) {
        return (int) packed;
    }

    /**
     * Packs {@code high} into the high 16 bits and {@code low} into the low 16 bits.
     */
    public static int pack(char high, char low) {
        return high << 16 | low;
    }

    /**
     * Returns the character stored in the high 16 bits of {@code packed}.
     */
    public static char unpackHighChar(int packed) {
        return (char) (packed >>> 16);
    }

    /**
     * Returns the character stored in the low 16 bits of {@code packed}.
     */
    public static char unpackLowChar(int packed) {
        return (char) packed;
    }

    /**
     * Packs {@code high} into the high 8 bits and {@code low} into the low 8 bits.
     */
    public static short pack(byte high, byte low) {
        return (short) (high << 8 | low & 0xff);
    }

    /**
     * Returns the signed byte stored in the high 8 bits of {@code packed}.
     */
    public static byte unpackHighByte(short packed) {
        return (byte) (packed >> 8);
    }

    /**
     * Returns the signed byte stored in the low 8 bits of {@code packed}.
     */
    public static byte unpackLowByte(short packed) {
        return (byte) packed;
    }
}
