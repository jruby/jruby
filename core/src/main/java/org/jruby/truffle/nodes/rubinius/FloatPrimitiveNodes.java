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

/**
 * Rubinius primitives associated with the Ruby {@code Float} class.
 */
public abstract class FloatPrimitiveNodes {

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
