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

public interface ArrayView {

    Object get(int index);

    class IntegerArrayView implements ArrayView {

        private final int[] array;

        public IntegerArrayView(int[] array) {
            this.array = array;
        }

        @Override
        public Object get(int index) {
            return array[index];
        }
    }

    class LongArrayView implements ArrayView {

        private final long[] array;

        public LongArrayView(long[] array) {
            this.array = array;
        }

        @Override
        public Object get(int index) {
            return array[index];
        }
    }

    class DoubleArrayView implements ArrayView {

        private final double[] array;

        public DoubleArrayView(double[] array) {
            this.array = array;
        }

        @Override
        public Object get(int index) {
            return array[index];
        }
    }

    class ObjectArrayView implements ArrayView {

        private final Object[] array;

        public ObjectArrayView(Object[] array) {
            this.array = array;
        }

        @Override
        public Object get(int index) {
            return array[index];
        }
    }

}
