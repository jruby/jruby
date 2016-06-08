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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.Layouts;

import java.math.BigDecimal;
import java.math.MathContext;

public abstract class AbstractMultNode extends BigDecimalOpNode {

    private final ConditionProfile zeroNormal = ConditionProfile.createBinaryProfile();

    private Object multBigDecimalConsideringSignum(DynamicObject a, DynamicObject b, MathContext mathContext) {
        final BigDecimal bBigDecimal = Layouts.BIG_DECIMAL.getValue(b);

        if (zeroNormal.profile(isNormalZero(a) && bBigDecimal.signum() == -1)) {
            return BigDecimalType.NEGATIVE_ZERO;
        }

        return multBigDecimal(Layouts.BIG_DECIMAL.getValue(a), bBigDecimal, mathContext);
    }

    @TruffleBoundary
    private Object multBigDecimal(BigDecimal a, BigDecimal b, MathContext mathContext) {
        return a.multiply(b, mathContext);
    }

    protected Object mult(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
        return createBigDecimal(frame, multBigDecimalConsideringSignum(a, b, new MathContext(precision, getRoundMode(frame))));
    }

    protected Object multNormalSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
        return multSpecialNormal(frame, b, a, precision);
    }

    protected Object multSpecialNormal(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
        Object value = null;

        switch (Layouts.BIG_DECIMAL.getType(a)) {
            case NAN:
                value = BigDecimalType.NAN;
                break;
            case NEGATIVE_ZERO:
                switch (Layouts.BIG_DECIMAL.getValue(b).signum()) {
                    case 1:
                    case 0:
                        value = BigDecimalType.NEGATIVE_ZERO;
                        break;
                    case -1:
                        value = BigDecimal.ZERO;
                        break;
                }
                break;
            case POSITIVE_INFINITY:
                switch (Layouts.BIG_DECIMAL.getValue(b).signum()) {
                    case 1:
                        value = BigDecimalType.POSITIVE_INFINITY;
                        break;
                    case 0:
                        value = BigDecimalType.NAN;
                        break;
                    case -1:
                        value = BigDecimalType.NEGATIVE_INFINITY;
                        break;
                }
                break;
            case NEGATIVE_INFINITY:
                switch (Layouts.BIG_DECIMAL.getValue(b).signum()) {
                    case 1:
                        value = BigDecimalType.NEGATIVE_INFINITY;
                        break;
                    case 0:
                        value = BigDecimalType.NAN;
                        break;
                    case -1:
                        value = BigDecimalType.POSITIVE_INFINITY;
                        break;
                }
                break;
            default:
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new UnsupportedOperationException("unreachable code branch");
        }

        return createBigDecimal(frame, value);
    }

    protected Object multSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
        final BigDecimalType aType = Layouts.BIG_DECIMAL.getType(a);
        final BigDecimalType bType = Layouts.BIG_DECIMAL.getType(b);

        if (aType == BigDecimalType.NAN || bType == BigDecimalType.NAN) {
            return createBigDecimal(frame, BigDecimalType.NAN);
        } else if (aType == BigDecimalType.NEGATIVE_ZERO && bType == BigDecimalType.NEGATIVE_ZERO) {
            return createBigDecimal(frame, BigDecimal.ZERO);
        } else if (aType == BigDecimalType.NEGATIVE_ZERO || bType == BigDecimalType.NEGATIVE_ZERO) {
            return createBigDecimal(frame, BigDecimalType.NAN);
        }

        // a and b are only +-Infinity

        if (aType == BigDecimalType.POSITIVE_INFINITY) {
            if (bType == BigDecimalType.POSITIVE_INFINITY) {
                return a;
            } else {
                return createBigDecimal(frame, BigDecimalType.NEGATIVE_INFINITY);
            }
        } else if (aType == BigDecimalType.NEGATIVE_INFINITY) {
            if (bType == BigDecimalType.POSITIVE_INFINITY) {
                return a;
            } else {
                return createBigDecimal(frame, (BigDecimalType.POSITIVE_INFINITY));
            }
        }

        throw new UnsupportedOperationException("unreachable code branch");
    }
}
