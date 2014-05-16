package org.jruby.truffle.runtime;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;

import java.util.List;

public abstract class ArrayUtils {

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

    public static Integer[] box(int[] unboxed) {
        final Integer[] boxed = new Integer[unboxed.length];

        for (int n = 0; n < unboxed.length; n++) {
            boxed[n] = unboxed[n];
        }

        return boxed;
    }

    public static Long[] box(long[] unboxed) {
        final Long[] boxed = new Long[unboxed.length];

        for (int n = 0; n < unboxed.length; n++) {
            boxed[n] = unboxed[n];
        }

        return boxed;
    }

    public static Double[] box(double[] unboxed) {
        final Double[] boxed = new Double[unboxed.length];

        for (int n = 0; n < unboxed.length; n++) {
            boxed[n] = unboxed[n];
        }

        return boxed;
    }

    public static Object[] box(Object array) {
        CompilerAsserts.neverPartOfCompilation();

        if (array == null) {
           return new Object[]{};
        } if (array instanceof int[]) {
            return box((int[]) array);
        } else if (array instanceof long[]) {
            return box((long[]) array);
        } else if (array instanceof double[]) {
            return box((double[]) array);
        } else if (array instanceof Object[]) {
            return (Object[]) array;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static int[] unbox(Integer[] unboxed) {
        final int[] boxed = new int[unboxed.length];

        for (int n = 0; n < unboxed.length; n++) {
            boxed[n] = unboxed[n];
        }

        return boxed;
    }

    public static long[] unbox(Long[] unboxed) {
        final long[] boxed = new long[unboxed.length];

        for (int n = 0; n < unboxed.length; n++) {
            boxed[n] = unboxed[n];
        }

        return boxed;
    }

    public static double[] unbox(Double[] unboxed) {
        final double[] boxed = new double[unboxed.length];

        for (int n = 0; n < unboxed.length; n++) {
            boxed[n] = unboxed[n];
        }

        return boxed;
    }

    @CompilerDirectives.SlowPath
    public static void copy(Object source, Object[] destination, int destinationStart) {
        if (source instanceof int[]) {
            final int[] unboxedSource = (int[]) source;

            for (int n = 0; n < unboxedSource.length; n++) {
                destination[destinationStart + n] = unboxedSource[n];
            }
        } else if (source instanceof long[]) {
            final long[] unboxedSource = (long[]) source;

            for (int n = 0; n < unboxedSource.length; n++) {
                destination[destinationStart + n] = unboxedSource[n];
            }
        } else if (source instanceof double[]) {
            final double[] unboxedSource = (double[]) source;

            for (int n = 0; n < unboxedSource.length; n++) {
                destination[destinationStart + n] = unboxedSource[n];
            }
        } else if (source instanceof Object[]) {
            System.arraycopy(source, 0, destination, destinationStart, ((Object[]) source).length);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static int capacity(int current) {
        if (current < 16) {
            return 16;
        } else {
            return 2 * current;
        }
    }

}
