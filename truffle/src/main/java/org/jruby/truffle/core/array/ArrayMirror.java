/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.array;

public interface ArrayMirror {

    int getLength();

    Object get(int index);

    void set(int index, Object value);

    ArrayMirror copyArrayAndMirror(int newLength);

    void copyTo(ArrayMirror destination, int sourceStart, int destinationStart, int count);

    void copyTo(Object[] destination, int sourceStart, int destinationStart, int count);

    ArrayMirror extractRange(int start, int end);

    Object getArray();

    ArrayMirror copyArrayAndMirror();

    Object[] getBoxedCopy();

    Object[] getBoxedCopy(int newLength);

    Iterable<Object> iterableUntil(int length);

}
