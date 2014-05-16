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

import java.math.BigInteger;

public class FixnumOrBignumNode extends Node {

    private final BranchProfile lowerProfile = new BranchProfile();
    private final BranchProfile integerProfile = new BranchProfile();
    private final BranchProfile longProfile = new BranchProfile();

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

}
