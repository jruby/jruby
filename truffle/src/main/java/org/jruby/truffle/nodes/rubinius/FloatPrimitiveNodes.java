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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyContext;
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

        public FloatDToAPrimitiveNode(FloatDToAPrimitiveNode prev) {
            super(prev);
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

        public FloatNegativePrimitiveNode(FloatNegativePrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public boolean floatNegative(double value) {
            // Edge-cases: 0, NaN and infinity can all be negative
            return (Double.doubleToLongBits(value) >>> 63) == 1;
        }

    }

}
