/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyRegexp;
import org.jruby.truffle.runtime.core.RubyString;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

/**
 * Rubinius primitives associated with the Ruby {@code Regexp} class.
 * <p>
 * Also see {@link RubyRegexp}.
 */
public abstract class RegexpPrimitiveNodes {

    @RubiniusPrimitive(name = "regexp_initialize")
    public static abstract class RegexpInitializePrimitiveNode extends RubiniusPrimitiveNode {

        public RegexpInitializePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RegexpInitializePrimitiveNode(RegexpInitializePrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public RubyRegexp initialize(RubyRegexp regexp, RubyString pattern, int options) {
            notDesignedForCompilation();

            regexp.initialize(this, pattern.getBytes(), options);
            return regexp;
        }

    }

}
