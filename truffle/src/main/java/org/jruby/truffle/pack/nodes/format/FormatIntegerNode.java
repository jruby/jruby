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
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.ByteList;

import java.nio.charset.StandardCharsets;

@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class FormatIntegerNode extends PackNode {

    private final int spacePadding;
    private final int zeroPadding;
    private final char format;

    public FormatIntegerNode(RubyContext context, int spacePadding, int zeroPadding, char format) {
        super(context);
        this.spacePadding = spacePadding;
        this.zeroPadding = zeroPadding;
        this.format = format;
    }

    @Specialization
    public ByteList format(int value) {
        return doFormat(value);
    }

    @Specialization
    public ByteList format(long value) {
        return doFormat(value);
    }

    @CompilerDirectives.TruffleBoundary
    protected ByteList doFormat(Object value) {
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

        builder.append(format);

        return new ByteList(String.format(builder.toString(), value).getBytes(StandardCharsets.US_ASCII));
    }

}
