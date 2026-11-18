package org.jruby.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.jruby.RubyBignum;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyInteger;
import org.jruby.RubyRange;
import org.jruby.RubyString;
import org.jruby.anno.CRuby;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Access.integerClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.indexError;

/**
 * Bit access to the bytes of a String: {@code String#bit_get}, {@code #bit_set?}, {@code #bit_set},
 * {@code #bit_clear}, {@code #bit_flip} and {@code #bit_count}.
 *
 * <p>Bit positions count from the first byte of the string. Within a byte, bit 0 is the least
 * significant bit by default ({@code lsb_first: true}). With {@code lsb_first: false} the bytes stay
 * in the same order, but bit 0 of each byte is its most significant bit.</p>
 *
 * <p>CRuby keeps bit positions as {@code uint64_t}, and only raises "out of representable range"
 * above {@code 2**64-1}. A JRuby string holds fewer than {@code 2**31} bytes, so any position of
 * {@code 2**63} or more is past the end of every string. Such positions are stored as
 * {@link #BEYOND_ANY_STRING}, which keeps all comparisons with the string size the same.</p>
 */
public final class StringBits {
    /** Stands for every bit position from {@code 2**63} to {@code 2**64-1}. */
    private static final long BEYOND_ANY_STRING = Long.MAX_VALUE;
    private static final BigInteger UINT64_MAX = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
    private static final VarHandle BYTES_AS_LONGS =
            MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.nativeOrder());

    private StringBits() {
    }

    /**
     * The change that {@code bit_set}, {@code bit_clear} and {@code bit_flip} make to each selected bit.
     */
    @CRuby(value = "str_bit_mutation", file = "string.c")
    public enum Mutation {
        SET, CLEAR, FLIP;

        @CRuby(value = "str_apply_bit_mask", file = "string.c")
        byte apply(byte value, int mask) {
            return switch (this) {
                case SET -> (byte) (value | mask);
                case CLEAR -> (byte) (value & ~mask);
                case FLIP -> (byte) (value ^ mask);
            };
        }
    }

    /**
     * A Range argument after its endpoints were converted to bit positions. It does not depend on the
     * string size, because converting the endpoints may run Ruby code that changes the string.
     *
     * @param begin the first bit position
     * @param endExclusive the position after the last bit; only used when {@code endOpen} is false
     * @param endOpen true for a Range with a nil end, which runs to the end of the string
     */
    @CRuby(value = "str_bit_range", file = "string.c")
    private record BitRange(long begin, long endExclusive, boolean endOpen) {
        /**
         * The number of bits from {@code begin} to the end of the range, using the string size for an
         * open end. A range that ends before it begins is empty. The result is not limited to the bits
         * that exist: reads limit it themselves, and writes raise when it is too long.
         */
        @CRuby(value = "str_bit_range_resolve", file = "string.c", note = "length part")
        long length(long totalBits) {
            long end = endOpen ? totalBits : endExclusive;
            return end < begin ? 0 : end - begin;
        }
    }

    /**
     * Reads the {@code lsb_first:} keyword. It defaults to true, and must be true or false.
     *
     * @param opts the keyword arguments, or null when none were given
     */
    @CRuby(value = "str_lsb_first_from_opts", file = "string.c")
    public static boolean lsbFirst(ThreadContext context, IRubyObject opts) {
        if (opts == null) return true;

        IRubyObject value = ArgsUtil.extractKeywordArg(context, (RubyHash) opts, "lsb_first");
        if (value == null || value == context.tru) return true;
        if (value == context.fals) return false;

        throw argumentError(context, "lsb_first must be true or false");
    }

    /**
     * Converts a bit position with {@code to_int}. Negative positions raise IndexError, and positions
     * above {@code 2**64-1} raise ArgumentError.
     */
    @CRuby(value = "str_bit_offset_from_index", file = "string.c")
    public static long bitOffset(ThreadContext context, IRubyObject index) {
        RubyInteger integer = toInteger(context, index);
        if (integer.isNegativeNumber(context)) {
            throw indexError(context, "bit index out of range");
        }

        return toUnsignedSaturated(context, integer, "bit index out of representable range");
    }

    /**
     * Converts a bit length with {@code to_int}. Unlike a position, a negative length raises
     * ArgumentError.
     */
    @CRuby(value = "str_bit_length_from_index", file = "string.c")
    public static long bitLength(ThreadContext context, IRubyObject index) {
        RubyInteger integer = toInteger(context, index);
        if (integer.isNegativeNumber(context)) {
            throw argumentError(context, "negative bit length");
        }

        return toUnsignedSaturated(context, integer, "bit length out of representable range");
    }

    @CRuby(value = "rb_to_int", file = "object.c")
    private static RubyInteger toInteger(ThreadContext context, IRubyObject value) {
        return (RubyInteger) TypeConverter.convertToType(context, value, integerClass(context),
                TypeConverter.sites(context).to_int_checked);
    }

    private static long toUnsignedSaturated(ThreadContext context, RubyInteger integer, String tooLarge) {
        if (integer instanceof RubyFixnum fixnum) return fixnum.getValue();

        BigInteger value = ((RubyBignum) integer).getValue();
        if (value.compareTo(UINT64_MAX) > 0) throw argumentError(context, tooLarge);

        return value.bitLength() < Long.SIZE ? value.longValue() : BEYOND_ANY_STRING;
    }

    /**
     * Converts the endpoints of a Range to bit positions. A nil begin means 0. Negative endpoints
     * raise IndexError: unlike byte indexes, they do not count back from the end.
     */
    @CRuby(value = "str_bit_range_to_offsets", file = "string.c")
    private static BitRange bitRange(ThreadContext context, RubyRange range) {
        IRubyObject beginValue = range.begin(context);
        IRubyObject endValue = range.end(context);

        long begin = beginValue.isNil() ? 0 : bitOffset(context, beginValue);
        if (endValue.isNil()) return new BitRange(begin, 0, true);

        long end = bitOffset(context, endValue);
        boolean exclusive = range.exclude_end_p(context).isTrue();
        // An inclusive end of BEYOND_ANY_STRING is past every string, so it needs no + 1.
        long endExclusive = exclusive || end == BEYOND_ANY_STRING ? end : end + 1;

        return new BitRange(begin, endExclusive, false);
    }

    private static long totalBits(ByteList bytes) {
        return (long) bytes.realSize() * Byte.SIZE;
    }

    @CRuby(value = "str_bit_offset_out_of_range", file = "string.c")
    private static boolean outOfRange(ByteList bytes, long offset) {
        return offset >= totalBits(bytes);
    }

    /**
     * Maps a bit position to the position counted from the least significant bit of its byte.
     */
    @CRuby(value = "str_logical_to_physical_bit64", file = "string.c")
    private static long physicalBit(long logical, boolean lsbFirst) {
        return lsbFirst ? logical : (logical & ~7L) | (7 - (logical & 7));
    }

    /**
     * Returns 0 or 1 for the bit at {@code index}, or -1 when the position is past the end.
     */
    @CRuby(value = "str_bit_get", file = "string.c")
    public static int bitGet(ThreadContext context, RubyString str, IRubyObject index, IRubyObject opts) {
        boolean lsbFirst = lsbFirst(context, opts);
        long offset = bitOffset(context, index);

        ByteList bytes = str.getByteList();
        if (outOfRange(bytes, offset)) return -1;

        long physical = physicalBit(offset, lsbFirst);
        return (bytes.get((int) (physical / Byte.SIZE)) >> (physical % Byte.SIZE)) & 1;
    }

    /**
     * Implements {@code bit_set}, {@code bit_clear} and {@code bit_flip}. The target is a single bit
     * position, a position followed by a length, or a Range of positions.
     *
     * @param length the length argument, or null when it was not given
     * @param opts the keyword arguments, or null when none were given
     */
    @CRuby(value = "str_mutate_bit", file = "string.c", note = "argument handling and region start check")
    public static RubyString mutate(ThreadContext context, RubyString str, IRubyObject target,
                                    IRubyObject length, IRubyObject opts, Mutation mutation) {
        boolean lsbFirst = lsbFirst(context, opts);

        if (target instanceof RubyRange range) {
            if (length != null) throw argumentError(context, "bit length not allowed with a Range");

            BitRange bitRange = bitRange(context, range);
            // Even a zero-length write needs a mutable receiver. The frozen check may warn about a
            // chilled string, and a warning handler may change the string, so read its size after it.
            str.modifyCheck();
            long totalBits = totalBits(str.getByteList());
            long begin = bitRange.begin();
            if (begin > totalBits) throw indexError(context, "bit range out of range");

            return mutateRegion(context, str, begin, bitRange.length(totalBits), lsbFirst, mutation);
        }

        if (length == null) return mutateSingleBit(context, str, target, lsbFirst, mutation);

        long begin = bitOffset(context, target);
        long bitLength = bitLength(context, length);
        str.modifyCheck();
        if (begin > totalBits(str.getByteList())) {
            throw indexError(context, "bit range out of range");
        }

        return mutateRegion(context, str, begin, bitLength, lsbFirst, mutation);
    }

    @CRuby(value = "str_mutate_single_bit", file = "string.c")
    private static RubyString mutateSingleBit(ThreadContext context, RubyString str, IRubyObject index,
                                              boolean lsbFirst, Mutation mutation) {
        long offset = bitOffset(context, index);
        str.modifyCheck();
        if (outOfRange(str.getByteList(), offset)) {
            throw indexError(context, "bit index out of range");
        }

        str.modifyAndClearCodeRange();
        ByteList bytes = str.getByteList();
        long physical = physicalBit(offset, lsbFirst);
        applyMask(bytes, (int) (physical / Byte.SIZE), 1 << (physical % Byte.SIZE), mutation);

        return str;
    }

    /**
     * Changes {@code length} bits from {@code begin}, which must already be at most the string size in
     * bits. The whole region must lie inside the string; writes do not clip it.
     */
    @CRuby(value = "str_mutate_bit", file = "string.c", note = "region length check and write")
    private static RubyString mutateRegion(ThreadContext context, RubyString str, long begin, long length,
                                           boolean lsbFirst, Mutation mutation) {
        if (length > totalBits(str.getByteList()) - begin) {
            throw indexError(context, "bit range out of range");
        }
        if (length == 0) return str;

        str.modifyAndClearCodeRange();
        mutateBits(str.getByteList(), begin, length, lsbFirst, mutation);

        return str;
    }

    /**
     * Applies {@code mutation} to {@code length} (at least 1) bits from {@code begin}. The partial
     * bytes at both ends get a mask, and whole bytes between them are changed at once.
     */
    @CRuby(value = "str_mutate_bit_region", file = "string.c")
    private static void mutateBits(ByteList bytes, long begin, long length, boolean lsbFirst, Mutation mutation) {
        long lastBit = begin + length - 1;
        int firstByte = (int) (begin / Byte.SIZE);
        int lastByte = (int) (lastBit / Byte.SIZE);
        int firstOffset = (int) (begin % Byte.SIZE);
        int lastOffset = (int) (lastBit % Byte.SIZE);

        if (firstByte == lastByte) {
            applyMask(bytes, firstByte, regionByteMask(firstOffset, lastOffset, lsbFirst), mutation);
            return;
        }

        applyMask(bytes, firstByte, regionByteMask(firstOffset, 7, lsbFirst), mutation);
        mutateWholeBytes(bytes.unsafeBytes(), bytes.begin() + firstByte + 1, bytes.begin() + lastByte, mutation);
        applyMask(bytes, lastByte, regionByteMask(0, lastOffset, lsbFirst), mutation);
    }

    private static void mutateWholeBytes(byte[] bytes, int from, int to, Mutation mutation) {
        switch (mutation) {
            case SET -> Arrays.fill(bytes, from, to, (byte) 0xFF);
            case CLEAR -> Arrays.fill(bytes, from, to, (byte) 0);
            case FLIP -> {
                for (int i = from; i < to; i++) bytes[i] = (byte) ~bytes[i];
            }
        }
    }

    private static void applyMask(ByteList bytes, int index, int mask, Mutation mutation) {
        byte[] unsafe = bytes.unsafeBytes();
        int i = bytes.begin() + index;
        unsafe[i] = mutation.apply(unsafe[i], mask);
    }

    /**
     * The mask for bits {@code low} to {@code high} (0 to 7, both included) of one byte, numbered as
     * {@code lsbFirst} says. A run of bits stays a run under both numberings; MSB-first mirrors it.
     */
    @CRuby(value = "str_bit_region_byte_mask", file = "string.c")
    private static int regionByteMask(int low, int high, boolean lsbFirst) {
        return lsbFirst ?
                (0xFF >>> (7 - high)) & (0xFF << low) :
                (0xFF >>> low) & (0xFF << (7 - high));
    }

    /**
     * Counts the set bits of the whole string, or of a region given as a position and a length or as a
     * Range. A region that runs past the end is clipped to the bits that exist.
     *
     * @param first the first positional argument, or null for a whole-string count
     * @param length the length argument, or null when it was not given
     * @param opts the keyword arguments, or null when none were given
     */
    @CRuby(value = "rb_str_bit_count", file = "string.c", note = "argument handling")
    public static long bitCount(ThreadContext context, RubyString str, IRubyObject first, IRubyObject length,
                                IRubyObject opts) {
        // lsb_first does not change a whole-string count, but it is still checked.
        boolean lsbFirst = lsbFirst(context, opts);
        if (first == null) return countBits(str.getByteList());

        if (first instanceof RubyRange range) {
            if (length != null) throw argumentError(context, "bit length not allowed with a Range");

            BitRange bitRange = bitRange(context, range);
            long totalBits = totalBits(str.getByteList());
            if (bitRange.begin() > totalBits) return 0;

            return countRegion(str.getByteList(), bitRange.begin(), bitRange.length(totalBits), lsbFirst);
        }

        if (length == null) throw argumentError(context, "no bit length given");

        long begin = bitOffset(context, first);
        long bitLength = bitLength(context, length);
        if (outOfRange(str.getByteList(), begin)) return 0;

        return countRegion(str.getByteList(), begin, bitLength, lsbFirst);
    }

    /**
     * Counts the set bits in {@code length} bits from {@code begin}, clipped to the end of the string.
     * {@code begin} must be at most the string size in bits.
     */
    @CRuby(value = "rb_str_bit_count", file = "string.c", note = "clipping a region")
    private static long countRegion(ByteList bytes, long begin, long length, boolean lsbFirst) {
        long available = totalBits(bytes) - begin;
        if (length > available) length = available;
        if (length == 0) return 0;

        return countBits(bytes, begin, length, lsbFirst);
    }

    @CRuby(value = "str_count_bits_region", file = "string.c")
    private static long countBits(ByteList bytes, long begin, long length, boolean lsbFirst) {
        long lastBit = begin + length - 1;
        int firstByte = (int) (begin / Byte.SIZE);
        int lastByte = (int) (lastBit / Byte.SIZE);
        int firstOffset = (int) (begin % Byte.SIZE);
        int lastOffset = (int) (lastBit % Byte.SIZE);

        if (firstByte == lastByte) {
            return Integer.bitCount(bytes.get(firstByte) & regionByteMask(firstOffset, lastOffset, lsbFirst));
        }

        long count = Integer.bitCount(bytes.get(firstByte) & regionByteMask(firstOffset, 7, lsbFirst));
        count += countBits(bytes.unsafeBytes(), bytes.begin() + firstByte + 1, bytes.begin() + lastByte);
        count += Integer.bitCount(bytes.get(lastByte) & regionByteMask(0, lastOffset, lsbFirst));
        return count;
    }

    private static long countBits(ByteList bytes) {
        return countBits(bytes.unsafeBytes(), bytes.begin(), bytes.begin() + bytes.realSize());
    }

    /**
     * Counts the set bits in {@code bytes[from]} to {@code bytes[to - 1]}, eight bytes at a time.
     */
    @CRuby(value = "str_count_bits", file = "string.c")
    private static long countBits(byte[] bytes, int from, int to) {
        long count = 0;
        int i = from;
        for (int wordsEnd = to - 7; i < wordsEnd; i += Long.BYTES) {
            count += Long.bitCount((long) BYTES_AS_LONGS.get(bytes, i));
        }
        for (; i < to; i++) {
            count += Integer.bitCount(Byte.toUnsignedInt(bytes[i]));
        }
        return count;
    }
}
