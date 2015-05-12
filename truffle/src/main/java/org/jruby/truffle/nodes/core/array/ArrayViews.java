/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core.array;

public abstract class ArrayViews {

    public static ArrayView.IntegerArrayView view(int[] array) {
        return new ArrayView.IntegerArrayView(array);
    }

    public static ArrayView.LongArrayView view(long[] array) {
        return new ArrayView.LongArrayView(array);
    }

    public static ArrayView.DoubleArrayView view(double[] array) {
        return new ArrayView.DoubleArrayView(array);
    }

    public static ArrayView.ObjectArrayView view(Object[] array) {
        return new ArrayView.ObjectArrayView(array);
    }

}
