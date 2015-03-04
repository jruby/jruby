/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.joda.time.DateTimeZone;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyTime;

@CoreClass(name = "Time")
public abstract class TimeNodes {

    @CoreMethod(names = "_gmt?")
    public abstract static class InternalGMTNode extends CoreMethodNode {

        public InternalGMTNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InternalGMTNode(InternalGMTNode prev) {
            super(prev);
        }

        @Specialization
        public boolean internalGMT(RubyTime time) {
            // TODO CS 15-Feb-15 we've ended up with both null and nil here - should simplify
            return (time.getOffset() == null || time.getOffset() == getContext().getCoreLibrary().getNilObject()) && (time.getDateTime().getZone().equals(DateTimeZone.UTC) || time.getDateTime().getZone().getOffset(time.getDateTime().getMillis()) == 0);
        }
    }

    @CoreMethod(names = "_set_gmt", required = 1)
    public abstract static class InternalSetGMTNode extends CoreMethodNode {

        public InternalSetGMTNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InternalSetGMTNode(InternalSetGMTNode prev) {
            super(prev);
        }

        @Specialization
        public boolean internalSetGMT(RubyTime time, Object setGMT) {
            throw new UnsupportedOperationException("_set_gmt" + setGMT.getClass());
        }
    }

    @CoreMethod(names = "_offset")
    public abstract static class InternalOffsetNode extends CoreMethodNode {

        public InternalOffsetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InternalOffsetNode(InternalOffsetNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass internalOffset(RubyTime time) {
            return getContext().getCoreLibrary().getNilObject();
        }
    }

    @CoreMethod(names = "_set_offset", required = 1)
    public abstract static class InternalSetOffsetNode extends CoreMethodNode {

        public InternalSetOffsetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InternalSetOffsetNode(InternalSetOffsetNode prev) {
            super(prev);
        }

        @Specialization
        public boolean internalSetGMT(RubyTime time, Object setOffset) {
            throw new UnsupportedOperationException("_set_offset " + setOffset.getClass());
        }
    }

}
