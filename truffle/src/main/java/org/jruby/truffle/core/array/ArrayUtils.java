/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.array;

import com.oracle.truffle.api.CompilerAsserts;
import org.jruby.truffle.RubyContext;

import java.lang.reflect.Array;
import java.util.Arrays;

public abstract class ArrayUtils {

    public static final Object[] EMPTY_ARRAY = new Object[0];

    /**
     * Extracts part of an array into a newly allocated byte[] array. Does not perform safety checks on parameters.
     * @param source the source array whose values should be extracted
     * @param start the start index, must be >= 0 and <= source.length
     * @param end the end index (exclusive), must be >= 0 and <= source.length and >= start
     * @return a newly allocated array with the extracted elements and length (end - start)
     */
    public static byte[] extractRange(byte[] source, int start, int end) {
        assert assertExtractRangeArgs(source, start, end);
        int length = end - start;
        byte[] result = new byte[length];
        System.arraycopy(source, start, result, 0, length);
        return result;
    }

    /**
     * Extracts part of an array into a newly allocated int[] array. Does not perform safety checks on parameters.
     * @param source the source array whose values should be extracted
     * @param start the start index, must be >= 0 and <= source.length
     * @param end the end index (exclusive), must be >= 0 and <= source.length and >= start
     * @return a newly allocated array with the extracted elements and length (end - start)
     */
    public static int[] extractRange(int[] source, int start, int end) {
        assert assertExtractRangeArgs(source, start, end);
        int length = end - start;
        int[] result = new int[length];
        System.arraycopy(source, start, result, 0, length);
        return result;
    }

    /**
     * Extracts part of an array into a newly allocated long[] array. Does not perform safety checks on parameters.
     * @param source the source array whose values should be extracted
     * @param start the start index, must be >= 0 and <= source.length
     * @param end the end index (exclusive), must be >= 0 and <= source.length and >= start
     * @return a newly allocated array with the extracted elements and length (end - start)
     */
    public static long[] extractRange(long[] source, int start, int end) {
        assert assertExtractRangeArgs(source, start, end);
        int length = end - start;
        long[] result = new long[length];
        System.arraycopy(source, start, result, 0, length);
        return result;
    }

    /**
     * Extracts part of an array into a newly allocated double[] array. Does not perform safety checks on parameters.
     * @param source the source array whose values should be extracted
     * @param start the start index, must be >= 0 and <= source.length
     * @param end the end index (exclusive), must be >= 0 and <= source.length and >= start
     * @return a newly allocated array with the extracted elements and length (end - start)
     */
    public static double[] extractRange(double[] source, int start, int end) {
        assert assertExtractRangeArgs(source, start, end);
        int length = end - start;
        double[] result = new double[length];
        System.arraycopy(source, start, result, 0, length);
        return result;
    }

    /**
     * Extracts part of an array into a newly allocated Object[] array. Does not perform safety checks on parameters.
     * @param source the source array whose values should be extracted
     * @param start the start index, must be >= 0 and <= source.length
     * @param end the end index (exclusive), must be >= 0 and <= source.length and >= start
     * @return a newly allocated array with the extracted elements and length (end - start)
     */
    public static Object[] extractRange(Object[] source, int start, int end) {
        assert assertExtractRangeArgs(source, start, end);
        int length = end - start;
        Object[] result = new Object[length];
        System.arraycopy(source, start, result, 0, length);
        return result;
    }

    private static boolean assertExtractRangeArgs(Object source, int start, int end) {
        assert source != null;
        assert start >= 0;
        assert start <= Array.getLength(source);
        assert end >= start;
        assert end <= Array.getLength(source);
        return true;
    }

    public static boolean contains(int[] array, int value) {
        for (int n = 0; n < array.length; n++) {
            if (array[n] == value) {
                return true;
            }
        }

        return false;
    }

    public static boolean contains(long[] array, long value) {
        for (int n = 0; n < array.length; n++) {
            if (array[n] == value) {
                return true;
            }
        }

        return false;
    }

    public static boolean contains(double[] array, double value) {
        for (int n = 0; n < array.length; n++) {
            if (array[n] == value) {
                return true;
            }
        }

        return false;
    }

    public static Object[] box(int[] unboxed) {
        return boxExtra(unboxed, 0);
    }

    public static Object[] box(long[] unboxed) {
        return boxExtra(unboxed, 0);
    }

    public static Object[] box(double[] unboxed) {
        return boxExtra(unboxed, 0);
    }

    public static Object[] box(Object array) {
        return boxExtra(array, 0);
    }

    public static Object[] box(int[] unboxed, int newLength) {
        final Object[] boxed = new Object[newLength];

        final int boxCount = Math.min(unboxed.length, newLength);
        for (int n = 0; n < boxCount; n++) {
            boxed[n] = unboxed[n];
        }

        return boxed;
    }

    public static Object[] box(long[] unboxed, int newLength) {
        final Object[] boxed = new Object[newLength];

        final int boxCount = Math.min(unboxed.length, newLength);
        for (int n = 0; n < boxCount; n++) {
            boxed[n] = unboxed[n];
        }

        return boxed;
    }

    public static Object[] box(Object array, int newLength) {
        return boxExtra(array, newLength - Array.getLength(array));
    }

    public static Object[] box(double[] unboxed, int newLength) {
        final Object[] boxed = new Object[newLength];

        final int boxCount = Math.min(unboxed.length, newLength);
        for (int n = 0; n < boxCount; n++) {
            boxed[n] = unboxed[n];
        }

        return boxed;
    }

    public static Object[] boxExtra(int[] unboxed, int extra) {
        assert extra >= 0: "extra is not negative";
        final Object[] boxed = new Object[unboxed.length + extra];

        for (int n = 0; n < unboxed.length; n++) {
            boxed[n] = unboxed[n];
        }

        return boxed;
    }

    public static Object[] boxExtra(long[] unboxed, int extra) {
        assert extra >= 0: "extra is not negative";
        final Object[] boxed = new Object[unboxed.length + extra];

        for (int n = 0; n < unboxed.length; n++) {
            boxed[n] = unboxed[n];
        }

        return boxed;
    }

    public static Object[] boxExtra(double[] unboxed, int extra) {
        assert extra >= 0: "extra is not negative";
        final Object[] boxed = new Object[unboxed.length + extra];

        for (int n = 0; n < unboxed.length; n++) {
            boxed[n] = unboxed[n];
        }

        return boxed;
    }

    public static Object[] boxExtra(Object array, int extra) {
        assert extra >= 0: "extra is not negative";
        CompilerAsserts.neverPartOfCompilation();

        if (array == null) {
           return new Object[extra];
        } else if (array instanceof int[]) {
            return boxExtra((int[]) array, extra);
        } else if (array instanceof long[]) {
            return boxExtra((long[]) array, extra);
        } else if (array instanceof double[]) {
            return boxExtra((double[]) array, extra);
        } else if (array.getClass() == Object[].class) {
            final Object[] objectArray = (Object[]) array;
            return ArrayUtils.grow(objectArray, objectArray.length + extra);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static Object[] boxUntil(int[] unboxed, int length) {
        final Object[] boxed = new Object[length];

        for (int n = 0; n < length; n++) {
            boxed[n] = unboxed[n];
        }

        return boxed;
    }

    public static Object[] boxUntil(long[] unboxed, int length) {
        final Object[] boxed = new Object[length];

        for (int n = 0; n < length; n++) {
            boxed[n] = unboxed[n];
        }

        return boxed;
    }

    public static Object[] boxUntil(double[] unboxed, int length) {
        final Object[] boxed = new Object[length];

        for (int n = 0; n < length; n++) {
            boxed[n] = unboxed[n];
        }

        return boxed;
    }

    public static Object[] boxUntil(Object array, int length) {
        CompilerAsserts.neverPartOfCompilation();

        if (array == null) {
            return EMPTY_ARRAY;
        } else if (array instanceof int[]) {
            return boxUntil((int[]) array, length);
        } else if (array instanceof long[]) {
            return boxUntil((long[]) array, length);
        } else if (array instanceof double[]) {
            return boxUntil((double[]) array, length);
        } else if (array.getClass() == Object[].class) {
            final Object[] objectArray = (Object[]) array;
            return Arrays.copyOf(objectArray, length);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static int[] unboxInteger(Object[] unboxed, int length) {
        CompilerAsserts.neverPartOfCompilation();

        final int[] boxed = new int[length];

        for (int n = 0; n < length; n++) {
            boxed[n] = (int) unboxed[n];
        }

        return boxed;
    }

    public static long[] unboxLong(Object[] unboxed, int length) {
        CompilerAsserts.neverPartOfCompilation();

        final long[] boxed = new long[length];

        for (int n = 0; n < length; n++) {
            final Object value = unboxed[n];

            if (value instanceof Integer) {
                boxed[n] = (int) unboxed[n];
            } else if (value instanceof Long) {
                boxed[n] = (long) unboxed[n];
            }
        }

        return boxed;
    }

    public static double[] unboxDouble(Object[] unboxed, int length) {
        CompilerAsserts.neverPartOfCompilation();

        final double[] boxed = new double[length];

        for (int n = 0; n < length; n++) {
            boxed[n] = (double) unboxed[n];
        }

        return boxed;
    }

    public static void copy(Object source, Object[] destination, int destinationStart, int length) {
        if (length == 0) {
            return;
        }

        if (source instanceof int[]) {
            final int[] unboxedSource = (int[]) source;

            for (int n = 0; n < length; n++) {
                destination[destinationStart + n] = unboxedSource[n];
            }
        } else if (source instanceof long[]) {
            final long[] unboxedSource = (long[]) source;

            for (int n = 0; n < length; n++) {
                destination[destinationStart + n] = unboxedSource[n];
            }
        } else if (source instanceof double[]) {
            final double[] unboxedSource = (double[]) source;

            for (int n = 0; n < length; n++) {
                destination[destinationStart + n] = unboxedSource[n];
            }
        } else if (source.getClass() == Object[].class) {
            arraycopy((Object[]) source, 0, destination, destinationStart, length);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static long[] longCopyOf(int[] ints) {
        return longCopyOf(ints, ints.length);
    }

    public static long[] longCopyOf(int[] ints, int newLength) {
        assert newLength >= ints.length;

        final long[] longs = new long[newLength];

        for (int n = 0; n < ints.length; n++) {
            longs[n] = ints[n];
        }

        return longs;
    }

    public static int capacity(RubyContext context, int current, int needed) {
        if (needed == 0) {
            return 0;
        }

        assert current < needed;

        if (needed < context.getOptions().ARRAY_UNINITIALIZED_SIZE) {
            return context.getOptions().ARRAY_UNINITIALIZED_SIZE;
        } else {
            final int newCapacity = current << 1;
            if (newCapacity >= needed) {
                return newCapacity;
            } else {
                return needed;
            }
        }
    }

    public static int capacityForOneMore(RubyContext context, int current) {
        if (current < context.getOptions().ARRAY_UNINITIALIZED_SIZE) {
            return context.getOptions().ARRAY_UNINITIALIZED_SIZE;
        } else {
            return current << 1;
        }
    }

    public static void arraycopy(Object[] src, int srcPos, Object[] dest, int destPos, int length) {
        System.arraycopy(src, srcPos, dest, destPos, length);
    }

    public static Object[] copyOf(Object[] array, int newLength) {
        final Object[] copy = new Object[newLength];
        System.arraycopy(array, 0, copy, 0, Math.min(array.length, newLength));
        return copy;
    }

    public static Object[] grow(Object[] array, int newLength) {
        assert newLength >= array.length;
        final Object[] copy = new Object[newLength];
        System.arraycopy(array, 0, copy, 0, array.length);
        return copy;
    }

    public static Object[] copy(Object[] array) {
        final Object[] copy = new Object[array.length];
        System.arraycopy(array, 0, copy, 0, array.length);
        return copy;
    }

    public static Object[] unshift(Object[] array, Object element) {
        final Object[] newArray = new Object[1 + array.length];
        newArray[0] = element;
        arraycopy(array, 0, newArray, 1, array.length);
        return newArray;
    }

    public static int memcmp(final byte[] first, final int firstStart, final byte[] second, final int secondStart, int size) {
        assert firstStart + size <= first.length;
        assert secondStart + size <= second.length;

        int cmp;

        for (int i = 0; i < size; i++) {
            if ((cmp = (first[firstStart + i] & 0xff) - (second[secondStart + i] & 0xff)) != 0) {
                return cmp;
            }
        }

        return 0;
    }
}
