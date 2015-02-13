/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.CompilerDirectives;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;

import java.math.BigInteger;

public class RubyBignum extends RubyBasicObject {

    private BigInteger value;

    public RubyBignum(RubyClass rubyClass, BigInteger value) {
        super(rubyClass);
        // TODO(CS): we fail this but we shouldn't
        //assert value.bitLength() < 64;
        this.value = value;
    }

    @Override
    public boolean hasNoSingleton() {
        return true;
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum negate() {
        return create(value.negate());
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum abs() {
        return create(value.abs());
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum create(BigInteger value) {
        return new RubyBignum(getContext().getCoreLibrary().getBignumClass(), value);
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum add(RubyBignum other) {
        return create(value.add(other.value));
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum add(long other) {
        return create(value.add(BigInteger.valueOf(other)));
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum subtract(RubyBignum other) {
        return create(value.subtract(other.value));
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum subtract(long other) {
        return create(value.subtract(BigInteger.valueOf(other)));
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum multiply(RubyBignum other) {
        return create(value.multiply(other.value));
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum multiply(long other) {
        return create(value.multiply(BigInteger.valueOf(other)));
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum pow(int other) {
        return create(value.pow(other));
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum pow(long other) {
        if (other < Integer.MAX_VALUE) {
            return pow((int) other);
        } else {
            BigInteger result = BigInteger.ONE;

            for (long n = 0; n < other; n++) {
                result = result.multiply(value);
            }

            return create(result);
        }
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum pow(RubyBignum other) {
        BigInteger result = BigInteger.ONE;

        for (BigInteger n = BigInteger.ZERO; other.value.compareTo(n) < 0; n = n.add(BigInteger.ONE)) {
            result = result.multiply(value);
        }

        return create(result);
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum divide(RubyBignum other) {
        return create(value.divide(other.value));
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum divide(long other) {
        return create(value.divide(BigInteger.valueOf(other)));
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum mod(long other) {
        return create(value.mod(BigInteger.valueOf(other)));
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum mod(RubyBignum other) {
        return create(value.mod(other.bigIntegerValue()));
    }

    @CompilerDirectives.TruffleBoundary
    public int compare(int other) {
        return value.compareTo(BigInteger.valueOf(other));
    }

    @CompilerDirectives.TruffleBoundary
    public int compare(long other) {
        return value.compareTo(BigInteger.valueOf(other));
    }

    @CompilerDirectives.TruffleBoundary
    public int compare(RubyBignum other) {
        return value.compareTo(other.value);
    }

    @CompilerDirectives.TruffleBoundary
    public int compare(double other) {
        return compare((long) other);
    }

    @CompilerDirectives.TruffleBoundary
    public boolean isEqualTo(int b) {
        return value.equals(BigInteger.valueOf(b));
    }

    @CompilerDirectives.TruffleBoundary
    public boolean isEqualTo(long b) {
        return value.equals(BigInteger.valueOf(b));
    }

    @CompilerDirectives.TruffleBoundary
    public boolean isEqualTo(RubyBignum b) {
        return value.equals(b.value);
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum and(RubyBignum other) {
        return create(value.and(other.value));
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum and(long other) {
        return create(value.and(BigInteger.valueOf(other)));
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum or(RubyBignum other) {
        return create(value.or(other.value));
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum or(long other) {
        return create(value.or(BigInteger.valueOf(other)));
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum xor(RubyBignum other) {
        return create(value.xor(other.value));
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum xor(long other) {
        return create(value.xor(BigInteger.valueOf(other)));
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum shiftLeft(int n) {
        return create(value.shiftLeft(n));
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBignum shiftRight(int n) {
        return create(value.shiftRight(n));
    }

    @CompilerDirectives.TruffleBoundary
    public long longValue() {
        return value.longValue();
    }

    @CompilerDirectives.TruffleBoundary
    public boolean isZero() {
        return value.equals(BigInteger.ZERO);
    }

    @CompilerDirectives.TruffleBoundary
    @Override
    public String toString() {
        return value.toString();
    }

    @CompilerDirectives.TruffleBoundary
    public String toHexString() {
        return value.toString(16);
    }

    @CompilerDirectives.TruffleBoundary
    public double doubleValue() {
        return value.doubleValue();
    }

    public BigInteger bigIntegerValue() {
        return value;
    }

    public static class BignumAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, RubyNode currentNode) {
            return new RubyBignum(rubyClass, BigInteger.ZERO);
        }

    }

}
