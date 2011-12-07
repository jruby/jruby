package org.jruby.util;

public class MurmurHash {
    // Based on Murmurhash 2.0 Java port at http://dmy999.com/article/50/murmurhash-2-java-port
    // 2011-12-05: Modified by Hiroshi Nakamura <nahi@ruby-lang.org>
    // - signature change to use offset
    //   hash(byte[] data, int seed) to hash(byte[] src, int offset, int length, int seed)
    // - extract 'm' and 'r' as murmurhash2.0 constants

    // Ported by Derek Young from the C version (specifically the endian-neutral
    // version) from:
    //   http://murmurhash.googlepages.com/
    //
    // released to the public domain - dmy999@gmail.com

    // 'm' and 'r' are mixing constants generated offline.
    // They're not really 'magic', they just happen to work well.
    private static final int MURMUR2_MAGIC = 0x5bd1e995;
    // CRuby 1.9 uses 16 but original C++ implementation uses 24 with above Magic.
    private static final int MURMUR2_R = 24;

    @SuppressWarnings("fallthrough")
    public static int hash32(byte[] src, int offset, int length, int seed) {
        // Initialize the hash to a 'random' value
        int h = seed ^ length;

        int i = offset;
        int len = length;
        while (len >= 4) {
            int k = src[i + 0] & 0xFF;
            k |= (src[i + 1] & 0xFF) << 8;
            k |= (src[i + 2] & 0xFF) << 16;
            k |= (src[i + 3] & 0xFF) << 24;

            k *= MURMUR2_MAGIC;
            k ^= k >>> MURMUR2_R;
            k *= MURMUR2_MAGIC;

            h *= MURMUR2_MAGIC;
            h ^= k;

            i += 4;
            len -= 4;
        }

        switch (len) {
        case 3:
            h ^= (src[i + 2] & 0xFF) << 16;
        case 2:
            h ^= (src[i + 1] & 0xFF) << 8;
        case 1:
            h ^= (src[i + 0] & 0xFF);
            h *= MURMUR2_MAGIC;
        }

        h ^= h >>> 13;
        h *= MURMUR2_MAGIC;
        h ^= h >>> 15;

        return h;
    }
}
