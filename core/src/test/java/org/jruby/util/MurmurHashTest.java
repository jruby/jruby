package org.jruby.util;

import junit.framework.TestCase;

// Based on Murmurhash 2.0 Java port at
// http://dmy999.com/article/50/murmurhash-2-java-port
// 2011-12-05: Modified by Hiroshi Nakamura <nahi@ruby-lang.org>
// - add testWithOffset() for signature change

// released to the public domain - dmy999@gmail.com
public class MurmurHashTest extends TestCase {
    // expected values are generated from the output of a C driver that
    // ran against the same input

    public void testChangingSeed() {
        // use a fixed key
        byte[] key = new byte[] { 0x4E, (byte) 0xE3, (byte) 0x91, 0x00, 0x10, (byte) 0x8F,
                (byte) 0xFF };

        int[] expected = { 0xeef8be32, 0x8109dec6, 0x9aaf4192, 0xc1bcaf1c, 0x821d2ce4, 0xd45ed1df,
                0x6c0357a7, 0x21d4e845, 0xfa97db50, 0x2f1985c8, 0x5d69782a, 0x0d6e4b85, 0xe7d9cf6b,
                0x337e6b49, 0xe1606944, 0xccc18ae8 };

        for (int i = 0; i < expected.length; i++) {
            int expectedHash = expected[i];
            int hash = MurmurHash.hash32(key, 0, key.length, i);
            assertEquals("i = " + i, expectedHash, hash);
        }
    }

    public void testChangingKey() {
        byte[] key = new byte[133];

        int[] expected = { 0xd743ae0b, 0xf1b461c6, 0xa45a6ceb, 0xdb15e003, 0x877721a4, 0xc30465f1,
                0xfb658ba4, 0x1adf93b2, 0xe40a7931, 0x3da52db0, 0xbf523511, 0x1efaf273, 0xe628c1dd,
                0x9a0344df, 0x901c99fc, 0x5ae1aa44 };
        for (int i = 0; i < 16; i++) {
            // keep seed constant, generate a known key pattern
            setKey(key, i);
            int expectedHash = expected[i];
            int hash = MurmurHash.hash32(key, 0, key.length, 0x1234ABCD);
            assertEquals("i = " + i, expectedHash, hash);
        }
    }

    public void testChangingKeyLength() {
        int[] expected = { 0xa0c72f8e, 0x29c2f97e, 0x00ca8bba, 0x88387876, 0xe203ce49, 0x58d75952,
                0xab84febe, 0x98153c65, 0xcbb38375, 0x6ea1a28b, 0x9afa8f55, 0xfb890eb6, 0x9516cc49,
                0x6408a8eb, 0xbb12d3e6, 0x00fb7519 };
        // vary the key and the length
        for (int i = 0; i < 16; i++) {
            byte[] key = new byte[i];
            setKey(key, i);
            int expectedHash = expected[i];
            int hash = MurmurHash.hash32(key, 0, key.length, 0x7870AAFF);
            assertEquals("i = " + i, expectedHash, hash);
        }
    }

    public void testWithOffset() {
        // use a fixed key
        byte[] key = new byte[] { 0x4E, (byte) 0xE3, (byte) 0x91, 0x00, 0x10, (byte) 0x8F,
                (byte) 0xFF };

        int[] expected = { 0xeef8be32, 0x8109dec6, 0x9aaf4192, 0xc1bcaf1c, 0x821d2ce4, 0xd45ed1df,
                0x6c0357a7, 0x21d4e845, 0xfa97db50, 0x2f1985c8, 0x5d69782a, 0x0d6e4b85, 0xe7d9cf6b,
                0x337e6b49, 0xe1606944, 0xccc18ae8 };

        for (int i = 0; i < expected.length; i++) {
            int expectedHash = expected[i];
            byte[] keyWithOffset = new byte[key.length + 31];
            for (int offset = 0; offset < 32; ++offset) {
                System.arraycopy(key, 0, keyWithOffset, offset, key.length);
                int hash = MurmurHash.hash32(keyWithOffset, offset, key.length, i);
                assertEquals("i = " + i, expectedHash, hash);
            }
        }
    }

    /** Fill a key with a known pattern (incrementing numbers) */
    private void setKey(byte[] key, int start) {
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) ((start + i) & 0xFF);
        }
    }
}
