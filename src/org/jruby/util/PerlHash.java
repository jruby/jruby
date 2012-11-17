package org.jruby.util;

/**
 * Perl's Hash implementation.
 *
 * @author nahi@ruby-lang.org
 */
public class PerlHash {
    public static long hash(long key, byte[] src, int offset, int length) {
	for (int idx = 0; idx < length; ++idx) {
	    key += (src[offset + idx] & 0xFF);
	    key += (key << 10);
	    key ^= (key >>> 6);
	}
	key += (key << 3);
	key ^= (key >>> 11);
	key += (key << 15);
	return key;
    }
}
