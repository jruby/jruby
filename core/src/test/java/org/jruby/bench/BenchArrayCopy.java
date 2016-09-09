package org.jruby.bench;

import org.jruby.util.ArraySupport;

/**
 * Created by kares on 28/08/16.
 */
public class BenchArrayCopy {

    private static final String[] arr0 = new String[0];
    private static final Object[] arr1 = new Object[] { "obj" };
    private static final Object[] arr2 = new Object[] { 1, 2 };
    private static final Object[] arr3 = new Object[] { 1, 2, null };
    private static final Object[] arr4 = new Object[] { 1, 2, 3, 4 };
    private static final Object[] arr5 = new Object[] { 1, 2, null, 4, null };

    private static final String[] dst0 = new String[] {};
    private static final Object[] dst1 = new Object[1];
    private static final Object[] dst2 = new Object[3];
    private static final Object[] dst3 = new Object[] { 1, 2, null };
    private static final Object[] dst4 = new Object[] { 1, 2, null, 4, null };

    public static void main(String[] args) {
        final int times = 100_000_000; final int loop = 10;

        for (int i = 0; i < loop; i++) {
            System.gc(); System.gc(); System.out.print("");
            benchSystemCopy0(times);

            System.gc(); System.gc(); System.out.print("");
            benchUtilsCopy0(times);
        }

        for (int i = 0; i < loop; i++) {
            System.gc(); System.gc(); System.out.print("");
            benchSystemCopy1(times);

            System.gc(); System.gc(); System.out.print("");
            benchUtilsCopy1(times);
        }

        for (int i = 0; i < loop; i++) {
            System.gc(); System.gc(); System.out.print("");
            benchSystemCopy2(times);

            System.gc(); System.gc(); System.out.print("");
            benchUtilsCopy2(times);
        }

        for (int i = 0; i < loop; i++) {
            System.gc(); System.gc(); System.out.print("");
            benchSystemCopy3(times);

            System.gc(); System.gc(); System.out.print("");
            benchUtilsCopy3(times);
        }

        for (int i = 0; i < loop; i++) {
            System.gc(); System.gc(); System.out.print("");
            benchSystemCopy4(times);

            System.gc(); System.gc(); System.out.print("");
            benchUtilsCopy4(times);
        }

    }

    public static void benchSystemCopy0(final int times) {
        final long time = System.currentTimeMillis();
        for ( int i = 0; i < times; i++ ) {
            System.arraycopy(arr0, 0, dst0, 0, arr0.length);
        }
        System.out.print("\n System.arraycopy arr0 : " + (System.currentTimeMillis() - time));
    }

    public static void benchSystemCopy1(final int times) {
        final long time = System.currentTimeMillis();
        for ( int i = 0; i < times; i++ ) {
            System.arraycopy(arr1, 0, dst1, 0, arr1.length);
        }
        System.out.print("\n System.arraycopy arr1 : " + (System.currentTimeMillis() - time));
    }

    public static void benchSystemCopy2(final int times) {
        final long time = System.currentTimeMillis();
        for ( int i = 0; i < times; i++ ) {
            System.arraycopy(arr2, 0, dst2, 0, arr2.length);
        }
        System.out.print("\n System.arraycopy arr2 : " + (System.currentTimeMillis() - time));
    }

    public static void benchSystemCopy3(final int times) {
        final long time = System.currentTimeMillis();
        for ( int i = 0; i < times; i++ ) {
            System.arraycopy(arr3, 0, dst3, 0, arr3.length);
        }
        System.out.print("\n System.arraycopy arr3 : " + (System.currentTimeMillis() - time));
    }

    public static void benchSystemCopy4(final int times) {
        final long time = System.currentTimeMillis();
        for ( int i = 0; i < times; i++ ) {
            System.arraycopy(arr4, 0, dst4, 0, arr4.length);
        }
        System.out.print("\n System.arraycopy arr4 : " + (System.currentTimeMillis() - time));
    }

    public static void benchSystemCopy5(final int times) {
        final long time = System.currentTimeMillis();
        for ( int i = 0; i < times; i++ ) {
            System.arraycopy(arr5, 0, new Object[6], 1, arr5.length);
        }
        System.out.print("\n System.arraycopy arr5 : " + (System.currentTimeMillis() - time));
    }

    //

    public static void benchUtilsCopy0(final int times) {
        final long time = System.currentTimeMillis();
        for ( int i = 0; i < times; i++ ) {
            ArraySupport.copy(arr0, dst0, 0, arr0.length);
        }
        System.out.print("\n ArrayUtils.copy arr0 : " + (System.currentTimeMillis() - time));
    }

    public static void benchUtilsCopy1(final int times) {
        final long time = System.currentTimeMillis();
        for ( int i = 0; i < times; i++ ) {
            ArraySupport.copy(arr1, dst1, 0, arr1.length);
        }
        System.out.print("\n ArrayUtils.copy arr1 : " + (System.currentTimeMillis() - time));
    }

    public static void benchUtilsCopy2(final int times) {
        final long time = System.currentTimeMillis();
        for ( int i = 0; i < times; i++ ) {
            ArraySupport.copy(arr2, dst2, 0, arr2.length);
        }
        System.out.print("\n ArrayUtils.copy arr2 : " + (System.currentTimeMillis() - time));
    }

    public static void benchUtilsCopy3(final int times) {
        final long time = System.currentTimeMillis();
        for ( int i = 0; i < times; i++ ) {
            ArraySupport.copy(arr3, dst3, 0, arr3.length);
        }
        System.out.print("\n ArrayUtils.copy arr3 : " + (System.currentTimeMillis() - time));
    }

    public static void benchUtilsCopy4(final int times) {
        final long time = System.currentTimeMillis();
        for ( int i = 0; i < times; i++ ) {
            ArraySupport.copy(arr4, dst4, 0, arr4.length);
        }
        System.out.print("\n ArrayUtils.copy arr4 : " + (System.currentTimeMillis() - time));
    }

}
