/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node.Child;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.BignumNodes;
import org.jruby.truffle.nodes.core.CoreMethod;
import org.jruby.truffle.nodes.dispatch.*;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyString;

import java.math.BigInteger;

/**
 * Rubinius primitives associated with the Ruby {@code Fixnum} class.
 */
public abstract class FixnumPrimitiveNodes {

    @RubiniusPrimitive(name = "fixnum_coerce")
    public static abstract class FixnumCoercePrimitiveNode extends RubiniusPrimitiveNode {

        @Child private DoesRespondDispatchHeadNode toFRespond;
        @Child private CallDispatchHeadNode toF;

        public FixnumCoercePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toFRespond = new DoesRespondDispatchHeadNode(context, false, false, MissingBehavior.RETURN_MISSING, null);
            toF = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization
        public RubyArray coerce(int a, int b) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), new int[]{b, a}, 2);
        }

        @Specialization
        public RubyArray coerce(long a, int b) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), new long[]{b, a}, 2);
        }

        @Specialization(guards = "!isInteger(b)")
        public RubyArray coerce(int a, Object b) {
            return null; // Primitive failure
        }

    }

    @RubiniusPrimitive(name = "fixnum_pow")
    public abstract static class FixnumPowPrimitiveNode extends BignumNodes.BignumCoreMethodNode {

        private final ConditionProfile negativeProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile complexProfile = ConditionProfile.createBinaryProfile();

        public FixnumPowPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

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

        @Specialization
        public Object pow(int a, RubyBignum b) {
            return pow((long) a, b);
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
        
        @CompilerDirectives.TruffleBoundary
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

        @Specialization
        public Object pow(long a, RubyBignum b) {
            notDesignedForCompilation();

            if (a == 0) {
                return 0;
            }

            if (a == 1) {
                return 1;
            }

            if (a == -1) {
                if (b.bigIntegerValue().testBit(0)) {
                    return -1;
                } else {
                    return 1;
                }
            }

            if (b.bigIntegerValue().compareTo(BigInteger.ZERO) < 0) {
                return null; // Primitive failure
            }

            getContext().getRuntime().getWarnings().warn("in a**b, b may be too big");
            // b >= 2**63 && (a > 1 || a < -1) => larger than largest double
            // MRI behavior/bug: always positive Infinity even if a negative and b odd (likely due to libc pow(a, +inf)).
            return Double.POSITIVE_INFINITY;
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public Object pow(int a, RubyBasicObject b) {
            return null; // Primitive failure
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public Object pow(long a, RubyBasicObject b) {
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
