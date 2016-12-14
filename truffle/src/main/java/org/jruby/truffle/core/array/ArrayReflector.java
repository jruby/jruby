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

    public static ArrayMirror reflect(Object array) {
        if (array == null) {
            return EmptyArrayMirror.INSTANCE;
        } else if (array instanceof int[]) {
            return reflect((int[]) array);
        } else if (array instanceof long[]) {
            return reflect((long[]) array);
        } else if (array instanceof double[]) {
            return reflect((double[]) array);
        } else if (array.getClass() == Object[].class) {
            return reflect((Object[]) array);
        } else {
            throw new UnsupportedOperationException();
        }
    }

}
