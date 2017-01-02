/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.numeric;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.core.CoreLibrary;
import org.jruby.truffle.language.RubyBaseNode;
import org.jruby.truffle.language.SourceIndexLength;

import java.math.BigDecimal;
import java.math.BigInteger;

public class FixnumOrBignumNode extends RubyBaseNode {

    private static final BigInteger LONG_MIN_BIGINT = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger LONG_MAX_BIGINT = BigInteger.valueOf(Long.MAX_VALUE);

    public static FixnumOrBignumNode create(SourceIndexLength sourceSection) {
        return new FixnumOrBignumNode(sourceSection);
    }

    public FixnumOrBignumNode() {
    }

    public FixnumOrBignumNode(SourceIndexLength sourceSection) {
        if (sourceSection != null) {
            unsafeSetSourceSection(sourceSection);
        }
    }

    private final ConditionProfile lowerProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile intProfile = ConditionProfile.createBinaryProfile();

    private final ConditionProfile integerFromDoubleProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile longFromDoubleProfile = ConditionProfile.createBinaryProfile();

    @TruffleBoundary
    public Object fixnumOrBignum(BigDecimal value) {
        return fixnumOrBignum(value.toBigInteger());
    }

    public Object fixnumOrBignum(BigInteger value) {
        if (lowerProfile.profile(value.compareTo(LONG_MIN_BIGINT) >= 0 && value.compareTo(LONG_MAX_BIGINT) <= 0)) {
            final long longValue = value.longValue();

            if (intProfile.profile(CoreLibrary.fitsIntoInteger(longValue))) {
                return (int) longValue;
            } else {
                return longValue;
            }
        } else {
            return createBignum(value);
        }
    }

    public Object fixnumOrBignum(double value) {
        if (integerFromDoubleProfile.profile(value > Integer.MIN_VALUE && value < Integer.MAX_VALUE)) {
            return (int) value;
        } else if (longFromDoubleProfile.profile(value > Long.MIN_VALUE && value < Long.MAX_VALUE)) {
            return (long) value;
        } else {
            return fixnumOrBignum(doubleToBigInteger(value));
        }
    }

    @TruffleBoundary
    private static BigInteger doubleToBigInteger(double value) {
        return new BigDecimal(value).toBigInteger();
    }

}
