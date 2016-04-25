/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.rubinius;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.core.Layouts;

import java.math.BigInteger;

/**
 * Rubinius primitives associated with the Ruby {@code Bignum} class.
 */
public abstract class BignumPrimitiveNodes {

    @RubiniusPrimitive(name = "bignum_compare")
    public abstract static class BignumCompareNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Specialization
        public int compare(DynamicObject a, long b) {
            return Layouts.BIGNUM.getValue(a).compareTo(BigInteger.valueOf(b));
        }

        @Specialization(guards = "!isInfinity(b)")
        public int compare(DynamicObject a, double b) {
            return Double.compare(Layouts.BIGNUM.getValue(a).doubleValue(), b);
        }

        @Specialization(guards = "isInfinity(b)")
        public int compareInfinity(DynamicObject a, double b) {
            if (b < 0) {
                return +1;
            } else {
                return -1;
            }
        }

        @Specialization(guards = "isRubyBignum(b)")
        public int compare(DynamicObject a, DynamicObject b) {
            return Layouts.BIGNUM.getValue(a).compareTo(Layouts.BIGNUM.getValue(b));
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public Object compareFallback(DynamicObject a, DynamicObject b) {
            return null; // Primitive failure
        }

    }

    @RubiniusPrimitive(name = "bignum_pow")
    public static abstract class BignumPowPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        private final ConditionProfile negativeProfile = ConditionProfile.createBinaryProfile();

        @Specialization
        public DynamicObject pow(DynamicObject a, int b) {
            return pow(a, (long) b);
        }

        @Specialization
        public DynamicObject pow(DynamicObject a, long b) {
            if (negativeProfile.profile(b < 0)) {
                return null; // Primitive failure
            } else {
                // TODO CS 15-Feb-15 what about this cast?
                return createBignum(Layouts.BIGNUM.getValue(a).pow((int) b));
            }
        }

        @TruffleBoundary
        @Specialization
        public double pow(DynamicObject a, double b) {
            return Math.pow(Layouts.BIGNUM.getValue(a).doubleValue(), b);
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Void pow(DynamicObject a, DynamicObject b) {
            throw new UnsupportedOperationException();
        }

    }

}
