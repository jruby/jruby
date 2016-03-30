/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.format;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.printf.PrintfTreeBuilder;

import java.nio.charset.StandardCharsets;

@NodeChildren({
        @NodeChild(value = "value", type = FormatNode.class),
})
public abstract class FormatFloatHumanReadableNode extends FormatNode {

    private final ConditionProfile integerProfile = ConditionProfile.createBinaryProfile();

    public FormatFloatHumanReadableNode(RubyContext context) {
        super(context);
    }

    @Specialization
    public byte[] format(double value) {
        /**
         * General approach taken from StackOverflow: http://stackoverflow.com/questions/703396/how-to-nicely-format-floating-numbers-to-string-without-unnecessary-decimal-0
         * Answers provided by JasonD (http://stackoverflow.com/users/1288598/jasond) and Darthenius (http://stackoverflow.com/users/974531/darthenius)
         * Licensed by cc-wiki license: http://creativecommons.org/licenses/by-sa/3.0/
         */

        // TODO (nirvdrum 09-Mar-15) Make this adhere to the MRI invariant: "single-precision, network (big-endian) byte order"

        // TODO CS 4-May-15 G/g? Space padding? Zero padding? Precision?

        // If the value is a long value stuffed in a double, cast it so we don't print a trailing ".0".
        if (integerProfile.profile(value - Math.rint(value) == 0)) {
            return toString((long) value);
        } else {
            return toString(value);
        }
    }

    @TruffleBoundary
    private byte[] toString(long value) {
        return String.valueOf(value).getBytes(StandardCharsets.US_ASCII);
    }

    @TruffleBoundary
    private byte[] toString(double value) {
        return String.valueOf(value).getBytes(StandardCharsets.US_ASCII);
    }

}
