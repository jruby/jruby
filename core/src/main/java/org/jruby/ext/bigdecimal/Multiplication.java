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

    static final BigInteger FIVE = BigInteger.valueOf(5);

    /**
     * An array with the first powers of ten in {@code BigInteger} version.
     * ({@code 10^0,10^1,...,10^31})
     */
    static final BigInteger[] bigTenPows = new BigInteger[32];

    static {
        bigTenPows[0]  = BigInteger.ONE;
        bigTenPows[1]  = BigInteger.TEN;
        bigTenPows[2]  = BigInteger.valueOf(100);
        bigTenPows[3]  = BigInteger.valueOf(1000);
        bigTenPows[4]  = BigInteger.valueOf(10000);
        bigTenPows[5]  = BigInteger.valueOf(100000);
        bigTenPows[6]  = BigInteger.valueOf(1000000);
        bigTenPows[7]  = BigInteger.valueOf(10000000);
        bigTenPows[8]  = BigInteger.valueOf(100000000);
        bigTenPows[9]  = BigInteger.valueOf(1000000000);
        bigTenPows[10] = BigInteger.valueOf(10000000000L);
        bigTenPows[11] = BigInteger.valueOf(100000000000L);
        bigTenPows[12] = BigInteger.valueOf(1000000000000L);
        bigTenPows[13] = BigInteger.valueOf(10000000000000L);
        bigTenPows[14] = BigInteger.valueOf(100000000000000L);
        bigTenPows[15] = BigInteger.valueOf(1000000000000000L);
        bigTenPows[16] = BigInteger.valueOf(10000000000000000L);
        bigTenPows[17] = BigInteger.valueOf(100000000000000000L);
        bigTenPows[18] = BigInteger.valueOf(1000000000000000000L);
        for (int i=19; i < bigTenPows.length; i++) {
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
                res = FIVE.pow(intExp).shiftLeft(intExp);
            } else {
                /*
                 * "HUGE POWERS"
                 *
                 * This branch probably won't be executed since the power of ten is too
                 * big.
                 */
                // To calculate:    5^exp
                BigInteger powerOfFive = FIVE.pow(Integer.MAX_VALUE);
                res = powerOfFive;
                long longExp = exp - Integer.MAX_VALUE;
                intExp = (int) (exp % Integer.MAX_VALUE);
                while (longExp > Integer.MAX_VALUE) {
                    res = res.multiply(powerOfFive);
                    longExp -= Integer.MAX_VALUE;
                }
                res = res.multiply(FIVE.pow(intExp));
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