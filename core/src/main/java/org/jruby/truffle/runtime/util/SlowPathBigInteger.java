/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.util;

import com.oracle.truffle.api.CompilerDirectives;

import java.math.BigInteger;

public abstract class SlowPathBigInteger {

    @CompilerDirectives.SlowPath
    public static BigInteger negate(BigInteger a) {
        return a.negate();
    }

    @CompilerDirectives.SlowPath
    public static BigInteger add(BigInteger a, BigInteger b) {
        return a.add(b);
    }

    @CompilerDirectives.SlowPath
    public static BigInteger subtract(BigInteger a, BigInteger b) {
        return a.subtract(b);
    }

    @CompilerDirectives.SlowPath
    public static BigInteger multiply(BigInteger a, BigInteger b) {
        return a.multiply(b);
    }

    @CompilerDirectives.SlowPath
    public static BigInteger divide(BigInteger a, BigInteger b) {
        return a.divide(b);
    }

    @CompilerDirectives.SlowPath
    public static BigInteger[] divideAndRemainder(BigInteger a, BigInteger b) {
        return a.divideAndRemainder(b);
    }

    @CompilerDirectives.SlowPath
    public static BigInteger mod(BigInteger a, BigInteger b) {
        return a.mod(b);
    }

    @CompilerDirectives.SlowPath
    public static BigInteger pow(BigInteger a, int b) {
        return a.pow(b);
    }

    @CompilerDirectives.SlowPath
    public static int compareTo(BigInteger a, BigInteger b) {
        return a.compareTo(b);
    }

    @CompilerDirectives.SlowPath
    public static BigInteger and(BigInteger a, BigInteger b) {
        return a.and(b);
    }

    @CompilerDirectives.SlowPath
    public static BigInteger or(BigInteger a, BigInteger b) {
        return a.or(b);
    }

    @CompilerDirectives.SlowPath
    public static BigInteger xor(BigInteger a, BigInteger b) {
        return a.xor(b);
    }

    @CompilerDirectives.SlowPath
    public static BigInteger shiftLeft(BigInteger a, int b) {
        return a.shiftLeft(b);
    }

    @CompilerDirectives.SlowPath
    public static BigInteger shiftRight(BigInteger a, int b) {
        return a.shiftRight(b);
    }

    @CompilerDirectives.SlowPath
    public static double doubleValue(BigInteger a) {
        return a.doubleValue();
    }

}
