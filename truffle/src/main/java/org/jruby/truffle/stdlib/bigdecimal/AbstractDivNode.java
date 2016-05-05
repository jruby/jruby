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

public abstract class AbstractDivNode extends BigDecimalOpNode {

    private final ConditionProfile normalZero = ConditionProfile.createBinaryProfile();

    private Object divBigDecimalWithProfile(DynamicObject a, DynamicObject b, MathContext mathContext) {
        final BigDecimal aBigDecimal = Layouts.BIG_DECIMAL.getValue(a);
        final BigDecimal bBigDecimal = Layouts.BIG_DECIMAL.getValue(b);
        if (normalZero.profile(bBigDecimal.signum() == 0)) {
            switch (aBigDecimal.signum()) {
                case 1:
                    return BigDecimalType.POSITIVE_INFINITY;
                case 0:
                    return BigDecimalType.NAN;
                case -1:
                    return BigDecimalType.NEGATIVE_INFINITY;
                default:
                    throw new UnsupportedOperationException("unreachable code branch for value: " + aBigDecimal.signum());
            }
        } else {
            return divBigDecimal(aBigDecimal, bBigDecimal, mathContext);
        }
    }

    @CompilerDirectives.TruffleBoundary
    private BigDecimal divBigDecimal(BigDecimal a, BigDecimal b, MathContext mathContext) {
        return a.divide(b, mathContext);
    }

    protected Object div(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
        return createBigDecimal(frame, divBigDecimalWithProfile(a, b, new MathContext(precision, getRoundMode(frame))));
    }

    protected Object divNormalSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
        switch (Layouts.BIG_DECIMAL.getType(b)) {
            case NAN:
                return createBigDecimal(frame, BigDecimalType.NAN);
            case NEGATIVE_ZERO:
                switch (Layouts.BIG_DECIMAL.getValue(a).signum()) {
                    case 1:
                        return createBigDecimal(frame, BigDecimalType.NEGATIVE_INFINITY);
                    case 0:
                        return createBigDecimal(frame, BigDecimalType.NAN);
                    case -1:
                        return createBigDecimal(frame, BigDecimalType.POSITIVE_INFINITY);
                }
            case POSITIVE_INFINITY:
                switch (Layouts.BIG_DECIMAL.getValue(a).signum()) {
                    case 1:
                    case 0:
                        return createBigDecimal(frame, BigDecimal.ZERO);
                    case -1:
                        return createBigDecimal(frame, BigDecimalType.NEGATIVE_ZERO);
                }
            case NEGATIVE_INFINITY:
                switch (Layouts.BIG_DECIMAL.getValue(b).signum()) {
                    case 1:
                        return createBigDecimal(frame, BigDecimalType.NEGATIVE_ZERO);
                    case 0:
                    case -1:
                        return createBigDecimal(frame, BigDecimal.ZERO);
                }
            default:
                throw new UnsupportedOperationException("unreachable code branch for value: " + Layouts.BIG_DECIMAL.getType(b));
        }
    }

    protected Object divSpecialNormal(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
        switch (Layouts.BIG_DECIMAL.getType(a)) {
            case NAN:
                return createBigDecimal(frame, BigDecimalType.NAN);
            case NEGATIVE_ZERO:
                switch (Layouts.BIG_DECIMAL.getValue(b).signum()) {
                    case 1:
                        return createBigDecimal(frame, BigDecimalType.NEGATIVE_ZERO);
                    case 0:
                        return createBigDecimal(frame, BigDecimalType.NAN);
                    case -1:
                        return createBigDecimal(frame, BigDecimal.ZERO);
                }
            case POSITIVE_INFINITY:
                switch (Layouts.BIG_DECIMAL.getValue(b).signum()) {
                    case 1:
                    case 0:
                        return createBigDecimal(frame, BigDecimalType.POSITIVE_INFINITY);
                    case -1:
                        return createBigDecimal(frame, BigDecimalType.NEGATIVE_INFINITY);
                }
            case NEGATIVE_INFINITY:
                switch (Layouts.BIG_DECIMAL.getValue(b).signum()) {
                    case 1:
                    case 0:
                        return createBigDecimal(frame, BigDecimalType.NEGATIVE_INFINITY);
                    case -1:
                        return createBigDecimal(frame, BigDecimalType.POSITIVE_INFINITY);
                }
            default:
                throw new UnsupportedOperationException("unreachable code branch for value: " + Layouts.BIG_DECIMAL.getType(a));
        }
    }

    protected Object divSpecialSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
        final BigDecimalType aType = Layouts.BIG_DECIMAL.getType(a);
        final BigDecimalType bType = Layouts.BIG_DECIMAL.getType(b);

        if (aType == BigDecimalType.NAN || bType == BigDecimalType.NAN ||
                (aType == BigDecimalType.NEGATIVE_ZERO && bType == BigDecimalType.NEGATIVE_ZERO)) {
            return createBigDecimal(frame, BigDecimalType.NAN);
        }

        if (aType == BigDecimalType.NEGATIVE_ZERO) {
            if (bType == BigDecimalType.POSITIVE_INFINITY) {
                return createBigDecimal(frame, BigDecimalType.NEGATIVE_ZERO);
            } else {
                return createBigDecimal(frame, BigDecimalType.POSITIVE_INFINITY);
            }
        }

        if (bType == BigDecimalType.NEGATIVE_ZERO) {
            if (aType == BigDecimalType.POSITIVE_INFINITY) {
                return createBigDecimal(frame, BigDecimalType.NEGATIVE_INFINITY);
            } else {
                return createBigDecimal(frame, BigDecimalType.POSITIVE_INFINITY);
            }
        }

        // a and b are only +-Infinity
        return createBigDecimal(frame, BigDecimalType.NAN);
    }
}
