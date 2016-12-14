/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core;

import java.util.Random;

public class Hashing {

    private static final boolean SIPHASH_ENABLED = false;
    private static final boolean CONSISTENT_HASHING_ENABLED = false;

    private static final int MURMUR2_MAGIC = 0x5bd1e995;

    public static final long SEED_K0;
    public static final long SEED_K1;

    static {
        if (CONSISTENT_HASHING_ENABLED) {
            SEED_K0 = -561135208506705104l;
            SEED_K1 = 7114160726623585955l;
        } else {
            final Random random = new Random();
            SEED_K0 = random.nextLong();
            SEED_K1 = random.nextLong();
        }
    }

    public static long hash(long seed, long value) {
        return end(update(start(seed), value));
    }

    public static long start(long value) {
        long hash = value;

        if (SIPHASH_ENABLED) {
            hash += SEED_K0;
        } else {
            hash += SEED_K1;
        }
        return hash;
    }

    public static long update(long hash, long value) {
        long v = 0;
        hash += value;
        v = murmur1(v + hash);
        v = murmur1(v + (hash >>> 4*8));
        return v;
    }

    public static long end(long hash) {
        hash = murmur_step(hash, 10);
        hash = murmur_step(hash, 17);
        return hash;
    }

    private static long murmur1(long h) {
        return murmur_step(h, 16);
    }

    public static long murmur_step(long h, long k) {
        return murmur((h), (k), 16);
    }

    public static long murmur(long h, long k, int r) {
        long m = MURMUR2_MAGIC;
        h += k;
        h *= m;
        h ^= h >> r;
        return h;
    }

}
