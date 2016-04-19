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
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;

import java.nio.charset.StandardCharsets;

@NodeChildren({
        @NodeChild(value = "value", type = FormatNode.class),
})
public abstract class FormatFloatHumanReadableNode extends FormatNode {

    public FormatFloatHumanReadableNode(RubyContext context) {
        super(context);
    }

    @TruffleBoundary
    @Specialization(guards = "isInteger(value)")
    public byte[] formatInteger(double value) {
        return String.valueOf((long) value).getBytes(StandardCharsets.US_ASCII);
    }

    @TruffleBoundary
    @Specialization(guards = "!isInteger(value)")
    public byte[] format(double value) {
        return String.valueOf(value).getBytes(StandardCharsets.US_ASCII);
    }

    protected boolean isInteger(double value) {
        /**
         * General approach taken from StackOverflow: http://stackoverflow.com/questions/703396/how-to-nicely-format-floating-numbers-to-string-without-unnecessary-decimal-0
         * Answers provided by JasonD (http://stackoverflow.com/users/1288598/jasond) and Darthenius (http://stackoverflow.com/users/974531/darthenius)
         * Licensed by cc-wiki license: http://creativecommons.org/licenses/by-sa/3.0/
         */

        // TODO (nirvdrum 09-Mar-15) Make this adhere to the MRI invariant: "single-precision, network (big-endian) byte order"

        return value - Math.rint(value) == 0;
    }

}
