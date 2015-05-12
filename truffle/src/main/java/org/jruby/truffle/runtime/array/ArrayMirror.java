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

import java.util.Arrays;

public abstract class ArrayMirror {

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

    public abstract int getLength();
    public abstract Object get(int index);
    public abstract void set(int index, Object value);
    public abstract ArrayMirror copyArrayAndMirror(int newLength);
    public abstract void copyTo(ArrayMirror destination, int sourceStart, int destinationStart, int count);
    public abstract void copyTo(Object[] destination, int sourceStart, int destinationStart, int count);
    public abstract Object getArray();

    public Object copyArrayAndMirror() {
        return copyArrayAndMirror(getLength());
    }

    public Object[] getBoxedCopy() {
        return getBoxedCopy(getLength());
    }

    public Object[] getBoxedCopy(int newLength) {
        final Object[] boxed = new Object[newLength];
        copyTo(boxed, 0, 0, Math.min(getLength(), newLength));
        return boxed;
    }

    private static class IntegerArrayMirror extends ArrayMirror {

        private final int[] array;

        public IntegerArrayMirror(int[] array) {
            this.array = array;
        }

        @Override
        public int getLength() {
            return array.length;
        }

        @Override
        public Object get(int index) {
            return array[index];
        }

        @Override
        public void set(int index, Object value) {
            array[index] = (int) value;
        }

        @Override
        public ArrayMirror copyArrayAndMirror(int newLength) {
            return new IntegerArrayMirror(Arrays.copyOf(array, newLength));
        }

        @Override
        public void copyTo(ArrayMirror destination, int sourceStart, int destinationStart, int count) {
            System.arraycopy(array, sourceStart, destination.getArray(), destinationStart, count);
        }

        @Override
        public Object[] getBoxedCopy(int newLength) {
            return ArrayUtils.box(array, newLength);
        }

        @Override
        public void copyTo(Object[] destination, int sourceStart, int destinationStart, int count) {
            for (int n = 0; n < count; n++) {
                destination[destinationStart + n] = array[sourceStart + n];
            }
        }

        @Override
        public Object getArray() {
            return array;
        }

    }

    private static class LongArrayMirror extends ArrayMirror {

        private final long[] array;

        public LongArrayMirror(long[] array) {
            this.array = array;
        }

        @Override
        public int getLength() {
            return array.length;
        }

        @Override
        public Object get(int index) {
            return array[index];
        }

        @Override
        public void set(int index, Object value) {
            array[index] = (long) value;
        }

        @Override
        public ArrayMirror copyArrayAndMirror(int newLength) {
            return new LongArrayMirror(Arrays.copyOf(array, newLength));
        }

        @Override
        public void copyTo(ArrayMirror destination, int sourceStart, int destinationStart, int count) {
            System.arraycopy(array, sourceStart, destination.getArray(), destinationStart, count);
        }

        @Override
        public void copyTo(Object[] destination, int sourceStart, int destinationStart, int count) {
            for (int n = 0; n < count; n++) {
                destination[destinationStart + n] = array[sourceStart + n];
            }
        }

        @Override
        public Object getArray() {
            return array;
        }

    }

    private static class DoubleArrayMirror extends ArrayMirror {

        private final double[] array;

        public DoubleArrayMirror(double[] array) {
            this.array = array;
        }

        @Override
        public int getLength() {
            return array.length;
        }

        @Override
        public Object get(int index) {
            return array[index];
        }

        @Override
        public void set(int index, Object value) {
            array[index] = (double) value;
        }

        @Override
        public ArrayMirror copyArrayAndMirror(int newLength) {
            return new DoubleArrayMirror(Arrays.copyOf(array, newLength));
        }

        @Override
        public void copyTo(ArrayMirror destination, int sourceStart, int destinationStart, int count) {
            System.arraycopy(array, sourceStart, destination.getArray(), destinationStart, count);
        }

        @Override
        public void copyTo(Object[] destination, int sourceStart, int destinationStart, int count) {
            for (int n = 0; n < count; n++) {
                destination[destinationStart + n] = array[sourceStart + n];
            }
        }

        @Override
        public Object getArray() {
            return array;
        }

    }

    private static class ObjectArrayMirror extends ArrayMirror {

        private final Object[] array;

        public ObjectArrayMirror(Object[] array) {
            this.array = array;
        }

        @Override
        public int getLength() {
            return array.length;
        }

        @Override
        public Object get(int index) {
            return array[index];
        }

        @Override
        public void set(int index, Object value) {
            array[index] = value;
        }

        @Override
        public ArrayMirror copyArrayAndMirror(int newLength) {
            return new ObjectArrayMirror(ArrayUtils.copyOf(array, newLength));
        }

        @Override
        public void copyTo(ArrayMirror destination, int sourceStart, int destinationStart, int count) {
            System.arraycopy(array, sourceStart, destination.getArray(), destinationStart, count);
        }

        @Override
        public void copyTo(Object[] destination, int sourceStart, int destinationStart, int count) {
            System.arraycopy(array, sourceStart, destination, destinationStart, count);
        }

        @Override
        public Object getArray() {
            return array;
        }

    }

}
