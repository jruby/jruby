/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.array;

public abstract class ArrayReflector {

    public static IntegerArrayMirror reflect(int[] array) {
        return new IntegerArrayMirror(array);
    }

    public static LongArrayMirror reflect(long[] array) {
        return new LongArrayMirror(array);
    }

    public static DoubleArrayMirror reflect(double[] array) {
        return new DoubleArrayMirror(array);
    }

    public static ObjectArrayMirror reflect(Object[] array) {
        return new ObjectArrayMirror(array);
    }

}
