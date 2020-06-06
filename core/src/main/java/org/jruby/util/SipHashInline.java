package org.jruby.util;

import java.nio.ByteOrder;

import org.jruby.util.unsafe.UnsafeHolder;

import sun.misc.Unsafe;

/**
 * SipHash implementation with hand inlining the SIPROUND.
 *
 * To know details about SipHash, see;
 * "a fast short-input PRF" https://www.131002.net/siphash/
 *
 * @author nahi@ruby-lang.org
 */
public class SipHashInline {

    public static long hash24(long k0, long k1, byte[] data) {
        return hash24(k0, k1, data, 0, data.length);
    }
    
    public static long hash24(long k0, long k1, byte[] src, int offset, int length) {
        long v0 = 0x736f6d6570736575L ^ k0;
        long v1 = 0x646f72616e646f6dL ^ k1;
        long v2 = 0x6c7967656e657261L ^ k0;
        long v3 = 0x7465646279746573L ^ k1;
        long m;
        int last = offset + length / 8 * 8;
        int i = offset;

        if (offset < 0) {
            throw new ArrayIndexOutOfBoundsException(offset);
        } else if (offset + length > src.length) {
            throw new ArrayIndexOutOfBoundsException(src.length);
        }

        // processing 8 bytes blocks in data
        while (i < last) {
            m = LongReader.INSTANCE.getLong(src, i);
            i += 8;
            // MSGROUND {
                v3 ^= m;

                /* SIPROUND with hand reordering
                 *
                 * SIPROUND in siphash24.c:
                 *   A: v0 += v1;
                 *   B: v1=ROTL(v1,13);
                 *   C: v1 ^= v0;
                 *   D: v0=ROTL(v0,32);
                 *   E: v2 += v3;
                 *   F: v3=ROTL(v3,16);
                 *   G: v3 ^= v2;
                 *   H: v0 += v3;
                 *   I: v3=ROTL(v3,21);
                 *   J: v3 ^= v0;
                 *   K: v2 += v1;
                 *   L: v1=ROTL(v1,17);
                 *   M: v1 ^= v2;
                 *   N: v2=ROTL(v2,32);
                 *
                 * Each dependency:
                 *   B -> A
                 *   C -> A, B
                 *   D -> C
                 *   F -> E
                 *   G -> E, F
                 *   H -> D, G
                 *   I -> H
                 *   J -> H, I
                 *   K -> C, G
                 *   L -> K
                 *   M -> K, L
                 *   N -> M
                 *
                 * Dependency graph:
                 *   D -> C -> B -> A
                 *        G -> F -> E
                 *   J -> I -> H -> D, G
                 *   N -> M -> L -> K -> C, G
                 *
                 * Resulting parallel friendly execution order:
                 *   -> ABCDHIJ
                 *   -> EFGKLMN
                 */

                // SIPROUND {
                    v0 += v1;                    v2 += v3;
                    v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
                    v1 ^= v0;                    v3 ^= v2;
                    v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
                    v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
                    v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
                    v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
                // }
                // SIPROUND {
                    v0 += v1;                    v2 += v3;
                    v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
                    v1 ^= v0;                    v3 ^= v2;
                    v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
                    v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
                    v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
                    v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
                // }
                v0 ^= m;
            // }
        }

        // packing the last block to long, as LE 0-7 bytes + the length in the top byte
        m = 0;
        for (i = offset + length - 1; i >= last; --i) {
            m <<= 8; m |= (long) src[i];
        }
        m |= (long) length << 56;
        // MSGROUND {
            v3 ^= m;
            for (int j = 0; j < 2; j++) {
            // SIPROUND {
                v0 += v1;                    v2 += v3;
                v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
                v1 ^= v0;                    v3 ^= v2;
                v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
                v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
                v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
                v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
            // }
            }
            v0 ^= m;
        // }

        // finishing...
        v2 ^= 0xff;
        for (int j = 0; j < 4; j++) {
        // SIPROUND {
            v0 += v1;                    v2 += v3;
            v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
            v1 ^= v0;                    v3 ^= v2;
            v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
            v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
            v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
            v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
        // }
        }
        return v0 ^ v1 ^ v2 ^ v3;
    }

    private static abstract class LongReader {
        public abstract long getLong(byte[] src, int offset);

        public static final LongReader INSTANCE = createBestLongReader();

        private static LongReader createBestLongReader() {
            try {
                if (UnsafeHolder.U != null) {
                    if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                        return new UnsafeLongReader(UnsafeHolder.U);
                    }
                }
            } catch (Exception e) {
            }
            return new FallbackLongReader();
        }

        private static final class FallbackLongReader extends LongReader {
            @Override
            public long getLong(byte[] src, int offset) {
                return (long) src[offset++]       |
                       (long) src[offset++] <<  8 |
                       (long) src[offset++] << 16 |
                       (long) src[offset++] << 24 |
                       (long) src[offset++] << 32 |
                       (long) src[offset++] << 40 |
                       (long) src[offset++] << 48 |
                       (long) src[offset++] << 56 ;
            }
        }

        private static final class UnsafeLongReader extends LongReader {
            final Unsafe unsafe;
            final int byteArrayBaseOffset;

            public UnsafeLongReader(Unsafe unsafe) {
                this.unsafe = unsafe;
                this.byteArrayBaseOffset = unsafe.arrayBaseOffset(byte[].class);
            }

            @Override
            public final long getLong(byte[] src, int offset) {
                return unsafe.getLong(src, byteArrayBaseOffset + (long)offset);
            }
        }
    }
}
