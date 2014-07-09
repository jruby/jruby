/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.runtime.core.RubyFixnum;
import org.jruby.truffle.runtime.util.SlowPathBigInteger;

import java.math.BigDecimal;
import java.math.BigInteger;

public class FixnumOrBignumNode extends Node {

    private final BranchProfile lowerProfile = new BranchProfile();
    private final BranchProfile integerProfile = new BranchProfile();
    private final BranchProfile longProfile = new BranchProfile();

    private final BranchProfile bignumProfile = new BranchProfile();
    private final BranchProfile checkLongProfile = new BranchProfile();

    public Object fixnumOrBignum(BigInteger value) {
        if (value.compareTo(RubyFixnum.MIN_VALUE_BIG) >= 0 && value.compareTo(RubyFixnum.MAX_VALUE_BIG) <= 0) {
            lowerProfile.enter();

            final long longValue = value.longValue();

            if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                integerProfile.enter();
                return (int) longValue;
            } else {
                longProfile.enter();
                return value;
            }
        } else {
            return value;
        }
    }

    public Object fixnumOrBignum(double value) {
        if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            // TODO(CS): reusing profiles might not be a good idea
            integerProfile.enter();

            return (int) value;
        }

        checkLongProfile.enter();

        if (value >= Long.MIN_VALUE && value <= Long.MAX_VALUE) {
            // TODO(CS): reusing profiles might not be a good idea
            longProfile.enter();

            return (long) value;
        }

        bignumProfile.enter();

        return SlowPathBigInteger.create(value);
    }

}
