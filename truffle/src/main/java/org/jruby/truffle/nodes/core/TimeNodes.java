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

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

import org.joda.time.DateTimeZone;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyTime;

@CoreClass(name = "Time")
public abstract class TimeNodes {

    // We need it to copy the internal data for a call to Kernel#clone.
    @CoreMethod(names = "initialize_copy", required = 1)
    public abstract static class InitializeCopyNode extends CoreMethodArrayArgumentsNode {

        public InitializeCopyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object initializeCopy(RubyTime self, RubyTime from) {
            self.setDateTime(from.getDateTime());
            self.setOffset(from.getOffset());
            return self;
        }

    }

    // Not a core method, used to simulate Rubinius @is_gmt.
    @NodeChild(type = RubyNode.class, value = "self")
    public abstract static class InternalGMTNode extends CoreMethodNode {

        public InternalGMTNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean internalGMT(RubyTime time) {
            return time.getOffset() == nil() &&
                    (time.getDateTime().getZone().equals(DateTimeZone.UTC) ||
                     time.getDateTime().getZone().getOffset(time.getDateTime().getMillis()) == 0);
        }
    }

    // Not a core method, used to simulate Rubinius @is_gmt.
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "self"),
            @NodeChild(type = RubyNode.class, value = "isGMT")
    })
    public abstract static class InternalSetGMTNode extends CoreMethodNode {

        public InternalSetGMTNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean internalSetGMT(RubyTime time, boolean isGMT) {
            if (isGMT) {
                time.setDateTime(time.getDateTime().withZone(DateTimeZone.UTC));
            } else {
                // Do nothing I guess - we can't change it to another zone, as what zone would that be?
            }

            return isGMT;
        }
    }

    // Not a core method, used to simulate Rubinius @offset.
    @NodeChild(type = RubyNode.class, value = "self")
    public abstract static class InternalOffsetNode extends CoreMethodNode {

        public InternalOffsetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object internalOffset(RubyTime time) {
            return time.getOffset();
        }
    }

    // Not a core method, used to simulate Rubinius @offset.
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "self"),
            @NodeChild(type = RubyNode.class, value = "offset")
    })
    public abstract static class InternalSetOffsetNode extends CoreMethodNode {

        public InternalSetOffsetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object internalSetOffset(RubyTime time, Object offset) {
            time.setOffset(offset);
            return offset;
        }
    }

}
