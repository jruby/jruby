/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.core;

import com.oracle.truffle.api.CompilerDirectives;
import org.jcodings.Encoding;
import org.jruby.truffle.runtime.rope.CodeRange;
import org.jruby.truffle.runtime.rope.Rope;
import org.jruby.truffle.runtime.rope.RopeOperations;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RopeTable {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final WeakHashMap<Key, WeakReference<Rope>> ropesTable = new WeakHashMap<>();

    @CompilerDirectives.TruffleBoundary
    public Rope getRope(byte[] bytes, Encoding encoding, CodeRange codeRange) {
        final Key key = new Key(bytes, encoding);

        lock.readLock().lock();

        try {
            final WeakReference<Rope> ropeReference = ropesTable.get(key);

            if (ropeReference != null) {
                final Rope rope = ropeReference.get();

                if (rope != null) {
                    return rope;
                }
            }
        } finally {
            lock.readLock().unlock();
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

            final Rope rope = RopeOperations.create(bytes, encoding, codeRange);

            ropesTable.put(key, new WeakReference<>(rope));

            return rope;
        } finally {
            lock.writeLock().unlock();
        }
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

                return encoding == other.encoding && Arrays.equals(bytes, other.bytes);
            }

            return false;
        }

        @Override
        public String toString() {
            return RopeOperations.create(bytes, encoding, CodeRange.CR_UNKNOWN).toString();
        }

    }

}
