/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.stdlib.bigdecimal;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.core.Layouts;

import java.math.BigDecimal;
import java.math.MathContext;

public abstract class AbstractSubNode extends BigDecimalOpNode {

    @CompilerDirectives.TruffleBoundary
    private BigDecimal subBigDecimal(DynamicObject a, DynamicObject b, MathContext mathContext) {
        return Layouts.BIG_DECIMAL.getValue(a).subtract(Layouts.BIG_DECIMAL.getValue(b), mathContext);
    }

    protected Object subNormal(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
        return createBigDecimal(frame, subBigDecimal(a, b, new MathContext(precision, getRoundMode(frame))));
    }

    protected Object subSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
        final BigDecimalType aType = Layouts.BIG_DECIMAL.getType(a);
        final BigDecimalType bType = Layouts.BIG_DECIMAL.getType(b);

        if (aType == BigDecimalType.NAN || bType == BigDecimalType.NAN ||
                (aType == BigDecimalType.POSITIVE_INFINITY && bType == BigDecimalType.POSITIVE_INFINITY) ||
                (aType == BigDecimalType.NEGATIVE_INFINITY && bType == BigDecimalType.NEGATIVE_INFINITY)) {
            return createBigDecimal(frame, BigDecimalType.NAN);
        }

        if (aType == BigDecimalType.POSITIVE_INFINITY || bType == BigDecimalType.NEGATIVE_INFINITY) {
            return createBigDecimal(frame, BigDecimalType.POSITIVE_INFINITY);
        }

        if (aType == BigDecimalType.NEGATIVE_INFINITY || bType == BigDecimalType.POSITIVE_INFINITY) {
            return createBigDecimal(frame, BigDecimalType.NEGATIVE_INFINITY);
        }

        // one is NEGATIVE_ZERO and second is NORMAL
        if (isNormal(a)) {
            return a;
        } else {
            return createBigDecimal(frame, Layouts.BIG_DECIMAL.getValue(b).negate());
        }
    }
}
