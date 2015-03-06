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

import org.joni.Matcher;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyMatchData;
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
            notDesignedForCompilation("1ea8707d2af444a7b348e049345361ee");

            regexp.initialize(this, pattern.getBytes(), options);
            return regexp;
        }

    }

    @RubiniusPrimitive(name = "regexp_search_region")
    public static abstract class RegexpSearchRegionPrimitiveNode extends RubiniusPrimitiveNode {

        public RegexpSearchRegionPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RegexpSearchRegionPrimitiveNode(RegexpSearchRegionPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public Object searchRegion(RubyRegexp regexp, RubyString string, int start, int end, boolean forward) {
            notDesignedForCompilation("112e05e7cc9048a2bfb3a93d1c6b9e7d");

            final Matcher matcher = regexp.getRegex().matcher(string.getBytes().bytes());

            return regexp.matchCommon(string, false, false, matcher, start, end);
        }

    }

    @RubiniusPrimitive(name = "regexp_set_last_match")
    public static abstract class RegexpSetLastMatchPrimitiveNode extends RubiniusPrimitiveNode {

        public RegexpSetLastMatchPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RegexpSetLastMatchPrimitiveNode(RegexpSetLastMatchPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public Object setLastMatch(RubyClass regexpClass, Object matchData) {
            notDesignedForCompilation("50ccd29854f84c51ab8b57d2c17deb1e");

            getContext().getThreadManager().getCurrentThread().getThreadLocals().getOperations().setInstanceVariable(
                    getContext().getThreadManager().getCurrentThread().getThreadLocals(), "$~", matchData);

            return matchData;
        }

    }
}
