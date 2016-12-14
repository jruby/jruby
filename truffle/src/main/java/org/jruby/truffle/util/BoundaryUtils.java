/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.util;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import java.util.Iterator;

public class BoundaryUtils {

    public static final class BoundaryIterable<E> implements Iterable<E> {

        public static <E> BoundaryIterable<E> wrap(Iterable<E> iterable) {
            return new BoundaryIterable<>(iterable);
        }

        private final Iterable<E> iterable;

        public BoundaryIterable(Iterable<E> iterable) {
            this.iterable = iterable;
        }

        @Override
        public BoundaryIterator<E> iterator() {
            return new BoundaryIterator<>(getIterator());
        }

        @TruffleBoundary
        private Iterator<E> getIterator() {
            return iterable.iterator();
        }

    }

    public static final class BoundaryIterator<E> implements Iterator<E> {

        private final Iterator<E> iterator;

        public BoundaryIterator(Iterator<E> iterator) {
            this.iterator = iterator;
        }

        @TruffleBoundary
        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @TruffleBoundary
        @Override
        public E next() {
            return iterator.next();
        }

        @TruffleBoundary
        @Override
        public void remove() {
            iterator.remove();
        }

    }

}
