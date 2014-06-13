package org.jruby.truffle.runtime;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import org.jruby.truffle.nodes.RubyNode;

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

    public static boolean contains(Object[] array, Object value) {
        for (int n = 0; n < array.length; n++) {
            if (array[n].equals(value)) {
                return true;
            }
        }

        return false;
    }

    public static Object[] box(int[] unboxed) {
        CompilerAsserts.neverPartOfCompilation();

        final Object[] boxed = new Object[unboxed.length];

        for (int n = 0; n < unboxed.length; n++) {
            boxed[n] = unboxed[n];
        }

        return boxed;
    }

    public static Object[] box(long[] unboxed) {
        CompilerAsserts.neverPartOfCompilation();

        final Object[] boxed = new Object[unboxed.length];

        for (int n = 0; n < unboxed.length; n++) {
            boxed[n] = unboxed[n];
        }

        return boxed;
    }

    public static Object[] box(double[] unboxed) {
        CompilerAsserts.neverPartOfCompilation();

        final Object[] boxed = new Object[unboxed.length];

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

    public static int[] unboxInteger(Object[] unboxed) {
        CompilerAsserts.neverPartOfCompilation();

        final int[] boxed = new int[unboxed.length];

        for (int n = 0; n < unboxed.length; n++) {
            boxed[n] = (int) unboxed[n];
        }

        return boxed;
    }

    public static long[] unboxLong(Object[] unboxed) {
        CompilerAsserts.neverPartOfCompilation();

        final long[] boxed = new long[unboxed.length];

        for (int n = 0; n < unboxed.length; n++) {
            boxed[n] = (long) unboxed[n];
        }

        return boxed;
    }

    public static double[] unboxDouble(Object[] unboxed) {
        CompilerAsserts.neverPartOfCompilation();

        final double[] boxed = new double[unboxed.length];

        for (int n = 0; n < unboxed.length; n++) {
            boxed[n] = (double) unboxed[n];
        }

        return boxed;
    }

    public static void copy(Object source, Object[] destination, int destinationStart, int length) {
        RubyNode.notDesignedForCompilation();

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
            System.arraycopy(source, 0, destination, destinationStart, length);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static long[] longCopyOf(int[] ints) {
        final long[] longs = new long[ints.length];

        for (int n = 0; n < ints.length; n++) {
            longs[n] = ints[n];
        }

        return longs;
    }

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

}
