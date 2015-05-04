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

import com.oracle.truffle.api.CompilerDirectives;
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

    public FormatFloatNode(int spacePadding, int zeroPadding, int precision) {
        this.spacePadding = spacePadding;
        this.zeroPadding = zeroPadding;
        this.precision = precision;
    }

    @Specialization
    public ByteList format(double value) {
        // TODO CS 3-May-15 write this without building a string and formatting

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

        builder.append("f");

        return new ByteList(String.format(builder.toString(), value).getBytes(StandardCharsets.US_ASCII));
    }

}
