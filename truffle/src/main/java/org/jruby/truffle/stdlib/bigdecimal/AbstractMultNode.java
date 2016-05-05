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
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.core.Layouts;

import java.math.BigDecimal;
import java.math.MathContext;

public abstract class AbstractMultNode extends BigDecimalOpNode {

    private final ConditionProfile zeroNormal = ConditionProfile.createBinaryProfile();

    private Object multBigDecimalWithProfile(DynamicObject a, DynamicObject b, MathContext mathContext) {
        final BigDecimal bBigDecimal = Layouts.BIG_DECIMAL.getValue(b);

        if (zeroNormal.profile(isNormalZero(a) && bBigDecimal.signum() == -1)) {
            return BigDecimalType.NEGATIVE_ZERO;
        }

        return multBigDecimal(Layouts.BIG_DECIMAL.getValue(a), bBigDecimal, mathContext);
    }

    @CompilerDirectives.TruffleBoundary
    private Object multBigDecimal(BigDecimal a, BigDecimal b, MathContext mathContext) {
        return a.multiply(b, mathContext);
    }

    protected Object mult(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
        return createBigDecimal(frame, multBigDecimalWithProfile(a, b, new MathContext(precision, getRoundMode(frame))));
    }

    protected Object multNormalSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
        return multSpecialNormal(frame, b, a, precision);
    }

    protected Object multSpecialNormal(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
        switch (Layouts.BIG_DECIMAL.getType(a)) {
            case NAN:
                return createBigDecimal(frame, BigDecimalType.NAN);
            case NEGATIVE_ZERO:
                switch (Layouts.BIG_DECIMAL.getValue(b).signum()) {
                    case 1:
                    case 0:
                        return createBigDecimal(frame, BigDecimalType.NEGATIVE_ZERO);
                    case -1:
                        return createBigDecimal(frame, BigDecimal.ZERO);
                }
            case POSITIVE_INFINITY:
                switch (Layouts.BIG_DECIMAL.getValue(b).signum()) {
                    case 1:
                        return createBigDecimal(frame, BigDecimalType.POSITIVE_INFINITY);
                    case 0:
                        return createBigDecimal(frame, BigDecimalType.NAN);
                    case -1:
                        return createBigDecimal(frame, BigDecimalType.NEGATIVE_INFINITY);
                }
            case NEGATIVE_INFINITY:
                switch (Layouts.BIG_DECIMAL.getValue(b).signum()) {
                    case 1:
                        return createBigDecimal(frame, BigDecimalType.NEGATIVE_INFINITY);
                    case 0:
                        return createBigDecimal(frame, BigDecimalType.NAN);
                    case -1:
                        return createBigDecimal(frame, BigDecimalType.POSITIVE_INFINITY);
                }
            default:
                throw new UnsupportedOperationException("unreachable code branch");
        }
    }

    protected Object multSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
        final BigDecimalType aType = Layouts.BIG_DECIMAL.getType(a);
        final BigDecimalType bType = Layouts.BIG_DECIMAL.getType(b);

        if (aType == BigDecimalType.NAN || bType == BigDecimalType.NAN) {
            return createBigDecimal(frame, BigDecimalType.NAN);
        }
        if (aType == BigDecimalType.NEGATIVE_ZERO && bType == BigDecimalType.NEGATIVE_ZERO) {
            return createBigDecimal(frame, BigDecimal.ZERO);
        }
        if (aType == BigDecimalType.NEGATIVE_ZERO || bType == BigDecimalType.NEGATIVE_ZERO) {
            return createBigDecimal(frame, BigDecimalType.NAN);
        }

        // a and b are only +-Infinity

        if (aType == BigDecimalType.POSITIVE_INFINITY) {
            return bType == BigDecimalType.POSITIVE_INFINITY ? a : createBigDecimal(frame, BigDecimalType.NEGATIVE_INFINITY);
        }
        if (aType == BigDecimalType.NEGATIVE_INFINITY) {
            return bType == BigDecimalType.POSITIVE_INFINITY ? a : createBigDecimal(frame, (BigDecimalType.POSITIVE_INFINITY));
        }

        throw new UnsupportedOperationException("unreachable code branch");
    }
}
