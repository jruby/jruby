/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.jruby.ext.bigdecimal;

import java.math.BigInteger;

// NOTE: from Android's sources
// https://android.googlesource.com/platform/libcore/+/refs/heads/master/luni/src/main/java/java/math/Multiplication.java

/**
 * Static library that provides all multiplication of {@link BigInteger} methods.
 */
class Multiplication {
    /** Just to denote that this class can't be instantiated. */
    private Multiplication() {}

    /**
     * An array with the first powers of ten in {@code BigInteger} version.
     * ({@code 10^0,10^1,...,10^31})
     */
    static final BigInteger[] bigTenPows = new BigInteger[32];
    /**
     * An array with the first powers of five in {@code BigInteger} version.
     * ({@code 5^0,5^1,...,5^31})
     */
    static final BigInteger bigFivePows[] = new BigInteger[32];
    static {
        int i;
        long fivePow = 1L;
        for (i = 0; i <= 18; i++) {
            bigFivePows[i] = BigInteger.valueOf(fivePow);
            bigTenPows[i] = BigInteger.valueOf(fivePow << i);
            fivePow *= 5;
        }
        for (; i < bigTenPows.length; i++) {
            bigFivePows[i] = bigFivePows[i - 1].multiply(bigFivePows[1]);
            bigTenPows[i] = bigTenPows[i - 1].multiply(BigInteger.TEN);
        }
    }

    /**
     * It calculates a power of ten, which exponent could be out of 32-bit range.
     * Note that internally this method will be used in the worst case with
     * an exponent equals to: {@code Integer.MAX_VALUE - Integer.MIN_VALUE}.
     * @param exp the exponent of power of ten, it must be positive.
     * @return a {@code BigInteger} with value {@code 10<sup>exp</sup>}.
     */
    static BigInteger powerOf10(long exp) {
        // PRE: exp >= 0
        int intExp = (int)exp;
        // "SMALL POWERS"
        if (exp < bigTenPows.length) {
            // The largest power that fit in 'long' type
            return bigTenPows[intExp];
        } else if (exp <= 50) {
            // To calculate:    10^exp
            return BigInteger.TEN.pow(intExp);
        }
        BigInteger res;
        try {
            // "LARGE POWERS"
            if (exp <= Integer.MAX_VALUE) {
                // To calculate:    5^exp * 2^exp
                res = bigFivePows[1].pow(intExp).shiftLeft(intExp);
            } else {
                /*
                 * "HUGE POWERS"
                 *
                 * This branch probably won't be executed since the power of ten is too
                 * big.
                 */
                // To calculate:    5^exp
                BigInteger powerOfFive = bigFivePows[1].pow(Integer.MAX_VALUE);
                res = powerOfFive;
                long longExp = exp - Integer.MAX_VALUE;
                intExp = (int) (exp % Integer.MAX_VALUE);
                while (longExp > Integer.MAX_VALUE) {
                    res = res.multiply(powerOfFive);
                    longExp -= Integer.MAX_VALUE;
                }
                res = res.multiply(bigFivePows[1].pow(intExp));
                // To calculate:    5^exp << exp
                res = res.shiftLeft(Integer.MAX_VALUE);
                longExp = exp - Integer.MAX_VALUE;
                while (longExp > Integer.MAX_VALUE) {
                    res = res.shiftLeft(Integer.MAX_VALUE);
                    longExp -= Integer.MAX_VALUE;
                }
                res = res.shiftLeft(intExp);
            }
        } catch (OutOfMemoryError error) {
            throw new ArithmeticException(error.getMessage());
        }
        return res;
    }

}