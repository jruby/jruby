/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.collections;

import com.oracle.truffle.api.CompilerDirectives;

import java.util.Iterator;

public final class BoundaryIterable<E> implements Iterable<E> {

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

    @CompilerDirectives.TruffleBoundary
    private Iterator<E> getIterator() {
        return iterable.iterator();
    }

}
