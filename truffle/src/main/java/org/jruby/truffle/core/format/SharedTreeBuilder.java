/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format;

import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.control.NNode;
import org.jruby.truffle.core.format.control.StarNode;
import org.jruby.truffle.core.format.pack.PackParser;

public class SharedTreeBuilder {

    private final RubyContext context;

    public SharedTreeBuilder(RubyContext context) {
        this.context = context;
    }

    public FormatNode applyCount(PackParser.CountContext count, FormatNode node) {
        if (count == null) {
            return node;
        } else if (count.INT() != null) {
            return new NNode(context, Integer.parseInt(count.INT().getText()), node);
        } else {
            return new StarNode(context, node);
        }
    }

    public StarLength parseCountContext(PackParser.CountContext ctx) {
        final boolean star;
        final int length;

        if (ctx == null) {
            star = false;
            length = 1;
        } else if (ctx.INT() == null) {
            star = true;
            length = 0;
        } else {
            star = false;
            length = Integer.parseInt(ctx.INT().getText());
        }

        return new StarLength(star, length);
    }

    public static class StarLength {

        private final boolean star;
        private final int length;

        public StarLength(boolean star, int length) {
            this.star = star;
            this.length = length;
        }

        public boolean isStar() {
            return star;
        }

        public int getLength() {
            return length;
        }
    }

}
