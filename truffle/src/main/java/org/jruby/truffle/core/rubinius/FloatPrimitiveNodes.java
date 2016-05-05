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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;
import org.jruby.truffle.core.numeric.FixnumOrBignumNode;
import org.jruby.truffle.language.control.RaiseException;

import java.util.Locale;

/**
 * Rubinius primitives associated with the Ruby {@code Float} class.
 */
public abstract class FloatPrimitiveNodes {

    @Primitive(name = "float_dtoa")
    public static abstract class FloatDToAPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject dToA(double value) {
            // Large enough to print all digits of Float::MIN.
            String string = String.format(Locale.ENGLISH, "%.1022f", value);

            if (string.toLowerCase(Locale.ENGLISH).contains("e")) {
                throw new UnsupportedOperationException();
            }

            string = string.replace("-", "");
            while (string.charAt(string.length() - 1) == '0') {
                string = string.substring(0, string.length() - 1);
            }

            int decimal;

            if (string.startsWith("0.")) {
                string = string.replace("0.", "");
                decimal = 0;

                while (string.charAt(0) == '0') {
                    string = string.substring(1, string.length());
                    --decimal;
                }
            } else {
                decimal = string.indexOf('.');

                if (decimal == -1) {
                    throw new UnsupportedOperationException();
                }

                string = string.replace(".", "");
            }

            final int sign = value < 0 ? 1 : 0;

            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), new Object[]{create7BitString(string, UTF8Encoding.INSTANCE), decimal, sign, string.length()}, 4);
        }

    }

    @Primitive(name = "float_signbit_p")
    public static abstract class FloatSignBitNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public boolean floatSignBit(double value) {
            // Edge-cases: 0, NaN and infinity can all be negative
            return (Double.doubleToLongBits(value) >>> 63) == 1;
        }

    }

    @Primitive(name = "float_round")
    public static abstract class FloatRoundPrimitiveNode extends PrimitiveArrayArgumentsNode {

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
                throw new RaiseException(coreExceptions().floatDomainError("Infinity", this));
            }

            if (Double.isNaN(n)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().floatDomainError("NaN", this));
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
