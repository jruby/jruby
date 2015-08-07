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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.source.SourceSection;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.om.dsl.api.Layout;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;

@CoreClass(name = "Time")
public abstract class TimeNodes {

    @Layout
    public interface TimeLayout extends BasicObjectNodes.BasicObjectLayout {

        DynamicObjectFactory createTimeShape(RubyBasicObject logicalClass, RubyBasicObject metaClass);

        DynamicObject createTime(DynamicObjectFactory factory, DateTime dateTime, Object offset);

        boolean isTime(DynamicObject object);

        DateTime getDateTime(DynamicObject object);

        void setDateTime(DynamicObject object, DateTime value);

        Object getOffset(DynamicObject object);

        void setOffset(DynamicObject object, Object value);

    }

    public static final TimeLayout TIME_LAYOUT = TimeLayoutImpl.INSTANCE;

    private static final DateTime ZERO = new DateTime(0);

    public static DateTime getDateTime(RubyBasicObject time) {
        return TIME_LAYOUT.getDateTime(BasicObjectNodes.getDynamicObject(time));
    }

    public static void setDateTime(RubyBasicObject time, DateTime dateTime) {
        TIME_LAYOUT.setDateTime(BasicObjectNodes.getDynamicObject(time), dateTime);
    }

    public static Object getOffset(RubyBasicObject time) {
        return TIME_LAYOUT.getOffset(BasicObjectNodes.getDynamicObject(time));
    }

    public static void setOffset(RubyBasicObject time, Object offset) {
        TIME_LAYOUT.setOffset(BasicObjectNodes.getDynamicObject(time), offset);
    }

    public static RubyBasicObject createRubyTime(RubyBasicObject timeClass, DateTime dateTime, Object offset) {
        return BasicObjectNodes.createRubyBasicObject(timeClass, TIME_LAYOUT.createTime(ModuleNodes.getModel(timeClass).factory, dateTime, offset));
    }

    // We need it to copy the internal data for a call to Kernel#clone.
    @CoreMethod(names = "initialize_copy", required = 1)
    public abstract static class InitializeCopyNode extends CoreMethodArrayArgumentsNode {

        public InitializeCopyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyTime(from)")
        public Object initializeCopy(RubyBasicObject self, RubyBasicObject from) {
            setDateTime(self, getDateTime(from));
            setOffset(self, getOffset(from));
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
        public boolean internalGMT(RubyBasicObject time) {
            return getOffset(time) == nil() &&
                    (getDateTime(time).getZone().equals(DateTimeZone.UTC) ||
                     getDateTime(time).getZone().getOffset(getDateTime(time).getMillis()) == 0);
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

        @TruffleBoundary
        @Specialization
        public boolean internalSetGMT(RubyBasicObject time, boolean isGMT) {
            if (isGMT) {
                setDateTime(time, getDateTime(time).withZone(DateTimeZone.UTC));
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
        public Object internalOffset(RubyBasicObject time) {
            return getOffset(time);
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
        public Object internalSetOffset(RubyBasicObject time, Object offset) {
            setOffset(time, offset);
            return offset;
        }
    }

    public static class TimeAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyBasicObject rubyClass, Node currentNode) {
            return createRubyTime(rubyClass, ZERO, context.getCoreLibrary().getNilObject());
        }

    }
}
