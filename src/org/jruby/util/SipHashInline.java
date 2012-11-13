package org.jruby.util;

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

        // processing 8 bytes blocks in data
        while (i < last) {
            // pack a block to long, as LE 8 bytes
            m = (long) src[i++]       |
                (long) src[i++] <<  8 |
                (long) src[i++] << 16 |
                (long) src[i++] << 24 |
                (long) src[i++] << 32 |
                (long) src[i++] << 40 |
                (long) src[i++] << 48 |
                (long) src[i++] << 56 ;
            // MSGROUND {
                v3 ^= m;

                /* SIPROUND wih hand reordering
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

        // finishing...
        v2 ^= 0xff;
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
        return v0 ^ v1 ^ v2 ^ v3;
    }
}
