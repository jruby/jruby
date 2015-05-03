/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.util;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import org.jruby.truffle.nodes.RubyNode;

import java.lang.reflect.Array;
import java.util.Arrays;

public abstract class ArrayUtils {

    /**
     * Extracts part of an array into a newly allocated Object[] array. Does not perform safety checks on parameters.
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
     * Extracts part of an array into a newly allocated Object[] array. Does not perform safety checks on parameters.
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
     * Extracts part of an array into a newly allocated Object[] array. Does not perform safety checks on parameters.
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
        return box(unboxed, 0);
    }

    public static Object[] box(long[] unboxed) {
        return box(unboxed, 0);
    }

    public static Object[] box(double[] unboxed) {
        return box(unboxed, 0);
    }

    public static Object[] box(Object array) {
        return box(array, 0);
    }

    public static Object[] box(int[] unboxed, int extra) {
        final Object[] boxed = new Object[unboxed.length + extra];

        for (int n = 0; n < unboxed.length; n++) {
            boxed[n] = unboxed[n];
        }

        return boxed;
    }

    public static Object[] box(long[] unboxed, int extra) {
        final Object[] boxed = new Object[unboxed.length + extra];

        for (int n = 0; n < unboxed.length; n++) {
            boxed[n] = unboxed[n];
        }

        return boxed;
    }

    public static Object[] box(double[] unboxed, int extra) {
        final Object[] boxed = new Object[unboxed.length + extra];

        for (int n = 0; n < unboxed.length; n++) {
            boxed[n] = unboxed[n];
        }

        return boxed;
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

    public static Object[] box(Object array, int extra) {
        CompilerAsserts.neverPartOfCompilation();

        if (array == null) {
           return new Object[extra];
        } if (array instanceof int[]) {
            return box((int[]) array, extra);
        } else if (array instanceof long[]) {
            return box((long[]) array, extra);
        } else if (array instanceof double[]) {
            return box((double[]) array, extra);
        } else if (array instanceof Object[]) {
            final Object[] objectArray = (Object[]) array;
            return Arrays.copyOf(objectArray, objectArray.length + extra);
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
                boxed[n] = (long) (int) unboxed[n];
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
        } else if (source instanceof Object[]) {
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

    @CompilerDirectives.TruffleBoundary
    public static int capacity(int current, int needed) {
        if (needed < 16) {
            return 16;
        } else {
            int capacity = current;

            while (capacity < needed) {
                capacity *= 2;
            }

            return capacity;
        }
    }

    public static void arraycopy(Object[] src, int srcPos, Object[] dest, int destPos, int length) {
        System.arraycopy(src, srcPos, dest, destPos, length);
    }

}
