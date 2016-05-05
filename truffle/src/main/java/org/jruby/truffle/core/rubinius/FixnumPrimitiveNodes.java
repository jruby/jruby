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
import org.jruby.truffle.core.numeric.BignumNodes;

import java.math.BigInteger;

/**
 * Rubinius primitives associated with the Ruby {@code Fixnum} class.
 */
public abstract class FixnumPrimitiveNodes {

    @RubiniusPrimitive(name = "fixnum_coerce")
    public static abstract class FixnumCoercePrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Specialization
        public DynamicObject coerce(int a, int b) {
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), new int[]{b, a}, 2);
        }

        @Specialization
        public DynamicObject coerce(long a, int b) {
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), new long[]{b, a}, 2);
        }

        @Specialization(guards = "!isInteger(b)")
        public DynamicObject coerce(int a, Object b) {
            return null; // Primitive failure
        }

    }

    @RubiniusPrimitive(name = "fixnum_pow")
    public abstract static class FixnumPowPrimitiveNode extends BignumNodes.BignumCoreMethodNode {

        private final ConditionProfile negativeProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile complexProfile = ConditionProfile.createBinaryProfile();

        @Specialization(guards = "canShiftIntoInt(a, b)")
        public int powTwo(int a, int b) {
            return 1 << b;
        }

        @Specialization(guards = "canShiftIntoInt(a, b)")
        public int powTwo(int a, long b) {
            return 1 << b;
        }

        @Specialization
        public Object pow(int a, int b) {
            return pow(a, (long) b);
        }

        @Specialization
        public Object pow(int a, long b) {
            return pow((long) a, b);
        }

        @Specialization
        public Object pow(int a, double b) {
            return pow((long) a, b);
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object powBignum(int a, DynamicObject b) {
            return powBignum((long) a, b);
        }

        @Specialization(guards = "canShiftIntoLong(a, b)")
        public long powTwo(long a, int b) {
            return 1 << b;
        }

        @Specialization(guards = "canShiftIntoLong(a, b)")
        public long powTwo(long a, long b) {
            return 1 << b;
        }

        @Specialization
        public Object pow(long a, int b) {
            return pow(a, (long) b);
        }

        @Specialization
        public Object pow(long a, long b) {
            if (negativeProfile.profile(b < 0)) {
                return null; // Primitive failure
            } else {
                // TODO CS 15-Feb-15 - what to do about this cast?
                return fixnumOrBignum(bigPow(a, (int) b));
            }
        }
        
        @TruffleBoundary
        public BigInteger bigPow(long a, int b) {
            return BigInteger.valueOf(a).pow(b);
            
        }

        @Specialization
        public Object pow(long a, double b) {
            if (complexProfile.profile(a < 0)) {
                return null; // Primitive failure
            } else {
                return Math.pow(a, b);
            }
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyBignum(b)")
        public Object powBignum(long a, DynamicObject b) {
            if (a == 0) {
                return 0;
            }

            if (a == 1) {
                return 1;
            }

            if (a == -1) {
                if (Layouts.BIGNUM.getValue(b).testBit(0)) {
                    return -1;
                } else {
                    return 1;
                }
            }

            if (Layouts.BIGNUM.getValue(b).compareTo(BigInteger.ZERO) < 0) {
                return null; // Primitive failure
            }

            getContext().getJRubyRuntime().getWarnings().warn("in a**b, b may be too big");
            // b >= 2**63 && (a > 1 || a < -1) => larger than largest double
            // MRI behavior/bug: always positive Infinity even if a negative and b odd (likely due to libc pow(a, +inf)).
            return Double.POSITIVE_INFINITY;
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public Object pow(int a, DynamicObject b) {
            return null; // Primitive failure
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public Object pow(long a, DynamicObject b) {
            return null; // Primitive failure
        }

        protected static boolean canShiftIntoInt(int a, int b) {
            return canShiftIntoInt(a, (long) b);
        }

        protected static boolean canShiftIntoInt(int a, long b) {
            // Highest bit we can set is the 30th due to sign
            return a == 2 && b >= 0 && b <= 32 - 2;
        }

        protected static boolean canShiftIntoLong(long a, int b) {
            return canShiftIntoLong(a, (long) b);
        }

        protected static boolean canShiftIntoLong(long a, long b) {
            // Highest bit we can set is the 30th due to sign
            return a == 2 && b >= 0 && b <= 64 - 2;
        }

    }

}
