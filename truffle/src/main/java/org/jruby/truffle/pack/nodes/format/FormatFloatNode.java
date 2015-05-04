/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack.nodes.format;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.pack.parser.FormatDirective;
import org.jruby.util.ByteList;

import java.nio.charset.StandardCharsets;

@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class FormatFloatNode extends PackNode {

    private final int spacePadding;
    private final int zeroPadding;
    private final int precision;
    private final char format;

    public FormatFloatNode(int spacePadding, int zeroPadding, int precision, char format) {
        this.spacePadding = spacePadding;
        this.zeroPadding = zeroPadding;
        this.precision = precision;
        this.format = format;
    }

    @Specialization
    public ByteList format(double value) {
        // TODO CS 3-May-15 write this without building a string and formatting

        if (format == 'G' || format == 'g') {
            /**
             * General approach taken from StackOverflow: http://stackoverflow.com/questions/703396/how-to-nicely-format-floating-numbers-to-string-without-unnecessary-decimal-0
             * Answers provided by JasonD (http://stackoverflow.com/users/1288598/jasond) and Darthenius (http://stackoverflow.com/users/974531/darthenius)
             * Licensed by cc-wiki license: http://creativecommons.org/licenses/by-sa/3.0/
             */

            // TODO (nirvdrum 09-Mar-15) Make this adhere to the MRI invariant: "single-precision, network (big-endian) byte order"

            // TODO CS 4-May-15 G/g? Space padding? Zero padding? Precision?

            // If the value is a long value stuffed in a double, cast it so we don't print a trailing ".0".
            if ((value - Math.rint(value)) == 0) {
                return ByteList.create(String.valueOf((long) value));
            } else {
                return ByteList.create(String.valueOf(value));
            }
        } else {
            final StringBuilder builder = new StringBuilder();

            builder.append("%");

            if (spacePadding != FormatDirective.DEFAULT) {
                builder.append(" ");
                builder.append(spacePadding);

                if (zeroPadding != FormatDirective.DEFAULT) {
                    builder.append(".");
                    builder.append(zeroPadding);
                }
            } else if (zeroPadding != FormatDirective.DEFAULT) {
                builder.append("0");
                builder.append(zeroPadding);
            }

            if (precision != FormatDirective.DEFAULT) {
                builder.append(".");
                builder.append(precision);
            }

            builder.append(format);

            return new ByteList(String.format(builder.toString(), value).getBytes(StandardCharsets.US_ASCII));
        }
    }

}
