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
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.nodes.core.FixnumOrBignumNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyArray;

import java.util.Locale;

/**
 * Rubinius primitives associated with the Ruby {@code Float} class.
 */
public abstract class FloatPrimitiveNodes {

    @RubiniusPrimitive(name = "float_dtoa")
    public static abstract class FloatDToAPrimitiveNode extends RubiniusPrimitiveNode {

        public FloatDToAPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyArray dToA(double value) {
            notDesignedForCompilation();

            String string = String.format(Locale.ENGLISH, "%.9f", value);

            if (string.toLowerCase(Locale.ENGLISH).contains("e")) {
                throw new UnsupportedOperationException();
            }

            string = string.replace("-", "");

            final int decimal;

            if (string.startsWith("0.")) {
                string = string.replace("0.", "");
                decimal = 0;
            } else {
                decimal = string.indexOf('.');

                if (decimal == -1) {
                    throw new UnsupportedOperationException();
                }

                string = string.replace(".", "");
            }

            final int sign = value < 0 ? 1 : 0;

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(),
                    new Object[]{getContext().makeString(string), decimal, sign, string.length()}, 4);
        }

    }

    @RubiniusPrimitive(name = "float_negative")
    public static abstract class FloatNegativePrimitiveNode extends RubiniusPrimitiveNode {

        public FloatNegativePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean floatNegative(double value) {
            // Edge-cases: 0, NaN and infinity can all be negative
            return (Double.doubleToLongBits(value) >>> 63) == 1;
        }

    }

    @RubiniusPrimitive(name = "float_round")
    public static abstract class FloatRoundPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private FixnumOrBignumNode fixnumOrBignum;

        private final BranchProfile greaterZero = BranchProfile.create();
        private final BranchProfile lessZero = BranchProfile.create();

        public FloatRoundPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fixnumOrBignum = new FixnumOrBignumNode(context, sourceSection);
        }

        @Specialization
        public Object round(double n) {
            // Algorithm copied from JRuby - not shared as we want to branch profile it

            if (Double.isInfinite(n)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().floatDomainError("Infinity", this));
            }

            if (Double.isNaN(n)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().floatDomainError("NaN", this));
            }

            double f = n;

            if (f > 0.0) {
                greaterZero.enter();

                f = Math.floor(f);

                if (n - f >= 0.5) {
                    f += 1.0;
                }
            } else if (f < 0.0) {
                lessZero.enter();

                f = Math.ceil(f);

                if (f - n >= 0.5) {
                    f -= 1.0;
                }
            }

            return fixnumOrBignum.fixnumOrBignum(f);
        }

    }

}
