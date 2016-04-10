/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.core.rope;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.util.StringSupport;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RopeTable {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final WeakHashMap<String, Key> stringsTable = new WeakHashMap<>();
    private final WeakHashMap<Key, WeakReference<Rope>> ropesTable = new WeakHashMap<>();
    private final Set<Key> keys = new HashSet<>();

    private int byteArrayReusedCount;
    private int ropesReusedCount;
    private int ropeBytesSaved;

    @TruffleBoundary
    public Rope getRope(String string) {
        lock.readLock().lock();

        try {
            final Key key = stringsTable.get(string);

            if (key != null) {
                final WeakReference<Rope> ropeReference = ropesTable.get(key);

                if (ropeReference != null) {
                    return ropeReference.get();
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();

        try {
            Key key = stringsTable.get(string);

            if (key == null) {
                key = new Key(string.getBytes(StandardCharsets.UTF_8), UTF8Encoding.INSTANCE);
                stringsTable.put(string, key);
            }

            WeakReference<Rope> ropeReference = ropesTable.get(key);

            if (ropeReference == null) {
                final long packedLengthAndCodeRange = RopeOperations.calculateCodeRangeAndLength(key.encoding, key.bytes, 0, key.bytes.length);
                final CodeRange codeRange = CodeRange.fromInt(StringSupport.unpackArg(packedLengthAndCodeRange));
                ropeReference = new WeakReference<Rope>(RopeOperations.create(key.bytes, key.encoding, codeRange));
                ropesTable.put(key, ropeReference);
            }

            return ropeReference.get();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @TruffleBoundary
    public Rope getRope(byte[] bytes, Encoding encoding, CodeRange codeRange) {
        final Key key = new Key(bytes, encoding);

        lock.readLock().lock();

        try {
            final WeakReference<Rope> ropeReference = ropesTable.get(key);

            if (ropeReference != null) {
                final Rope rope = ropeReference.get();

                if (rope != null) {
                    ++ropesReusedCount;
                    ropeBytesSaved += rope.byteLength();

                    return rope;
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        // The only time we should have a null encoding is if we want to find a rope with the same logical byte[] as
        // the one supplied to this method. If we've made it this far, no such rope exists, so return null immediately
        // to back out of the recursive call.
        if (encoding == null) {
            return null;
        }

        lock.writeLock().lock();

        try {
            final WeakReference<Rope> ropeReference = ropesTable.get(key);

            if (ropeReference != null) {
                final Rope rope = ropeReference.get();

                if (rope != null) {
                    return rope;
                }
            }

            // At this point, we were unable to find a rope with the same bytes and encoding (i.e., a direct match).
            // However, there may still be a rope with the same byte[] and sharing a direct byte[] can still allow some
            // reference equality optimizations. So, do another search but with a marker encoding. The only guarantee
            // we can make about the resulting rope is that it would have the same logical byte[], but that's good enough
            // for our purposes.
            final Rope ropeWithSameBytesButDifferentEncoding = getRope(bytes, null ,codeRange);

            final Rope rope;
            if (ropeWithSameBytesButDifferentEncoding != null) {
                rope = RopeOperations.create(ropeWithSameBytesButDifferentEncoding.getBytes(), encoding, codeRange);

                ++byteArrayReusedCount;
                ropeBytesSaved += rope.byteLength();
            } else {
                rope = RopeOperations.create(bytes, encoding, codeRange);
            }

            ropesTable.put(key, new WeakReference<>(rope));

            // TODO (nirvdrum 30-Mar-16): Revisit this. The purpose is to keep all keys live so the weak rope table never expunges results. We don't want that -- we want something that naturally ties to lifetime. Unfortunately, the old approach expunged live values because the key is synthetic.
            keys.add(key);

            return rope;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean contains(Rope rope) {
        lock.readLock().lock();

        try {
            for (Map.Entry<Key, WeakReference<Rope>> entry : ropesTable.entrySet()) {
                if (entry.getValue().get() == rope) {
                    return true;
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        return false;
    }

    public int getByteArrayReusedCount() {
        return byteArrayReusedCount;
    }

    public int getRopesReusedCount() {
        return ropesReusedCount;
    }

    public int getRopeBytesSaved() {
        return ropeBytesSaved;
    }

    public int totalRopes() {
        return ropesTable.size();
    }

    public static class Key {

        private final byte[] bytes;
        private final Encoding encoding;
        private int hashCode;

        public Key(byte[] bytes, Encoding encoding) {
            this.bytes = bytes;
            this.encoding = encoding;
            this.hashCode = Arrays.hashCode(bytes);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Key) {
                final Key other = (Key) o;

                return ((encoding == other.encoding) || (encoding == null)) && Arrays.equals(bytes, other.bytes);
            }

            return false;
        }

        @Override
        public String toString() {
            return RopeOperations.create(bytes, encoding, CodeRange.CR_UNKNOWN).toString();
        }

    }

}
