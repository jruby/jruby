/*
 * Copyright (c) 2016, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.tracepoint;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.UnaryCoreMethodNode;
import org.jruby.truffle.builtins.YieldingCoreMethodNode;
import org.jruby.truffle.core.kernel.TraceManager;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.objects.AllocateObjectNode;

@CoreClass("TracePoint")
public abstract class TracePointNodes {

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends UnaryCoreMethodNode {

        @Child private AllocateObjectNode allocateNode = AllocateObjectNode.create();

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return allocateNode.allocate(rubyClass, null, null, null, 0, null, null, null, false);
        }

    }

    @CoreMethod(names = "initialize", rest = true, needsBlock = true)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isTracePoint(tracePoint)")
        public DynamicObject initialize(DynamicObject tracePoint, Object[] args, DynamicObject block) {
            Layouts.TRACE_POINT.setTags(tracePoint, new Class<?>[]{TraceManager.LineTag.class});
            Layouts.TRACE_POINT.setProc(tracePoint, block);
            return tracePoint;
        }

    }

    @CoreMethod(names = "enable", needsBlock = true)
    public abstract static class EnableNode extends YieldingCoreMethodNode {

        @Specialization(guards = "isTracePoint(tracePoint)")
        public boolean enable(VirtualFrame frame, DynamicObject tracePoint, NotProvided block) {
            return enable(frame, tracePoint, (DynamicObject) null);
        }

        @Specialization(guards = "isTracePoint(tracePoint)")
        public boolean enable(VirtualFrame frame, DynamicObject tracePoint, DynamicObject block) {
            EventBinding<?> eventBinding = (EventBinding<?>) Layouts.TRACE_POINT.getEventBinding(tracePoint);
            final boolean alreadyEnabled = eventBinding != null;

            if (!alreadyEnabled) {
                eventBinding = createEventBinding(getContext(), tracePoint);
                Layouts.TRACE_POINT.setEventBinding(tracePoint, eventBinding);
            }

            if (block != null) {
                try {
                    yield(frame, block);
                } finally {
                    if (!alreadyEnabled) {
                        dispose(eventBinding);
                        Layouts.TRACE_POINT.setEventBinding(tracePoint, null);
                    }
                }
            }

            return alreadyEnabled;
        }

        @TruffleBoundary
        public static EventBinding<?> createEventBinding(final RubyContext context, final DynamicObject tracePoint) {
            return context.getInstrumenter().attachFactory(SourceSectionFilter.newBuilder()
                    .mimeTypeIs(RubyLanguage.MIME_TYPE)
                    .tagIs((Class<?>[]) Layouts.TRACE_POINT.getTags(tracePoint))
                    .build(), eventContext -> new TracePointEventNode(context, tracePoint));
        }

        @TruffleBoundary
        public static void dispose(EventBinding<?> eventBinding) {
            eventBinding.dispose();
        }

    }

    @CoreMethod(names = "disable", needsBlock = true)
    public abstract static class DisableNode extends YieldingCoreMethodNode {

        @Specialization(guards = "isTracePoint(tracePoint)")
        public boolean disable(VirtualFrame frame, DynamicObject tracePoint, NotProvided block) {
            return disable(frame, tracePoint, (DynamicObject) null);
        }

        @Specialization(guards = "isTracePoint(tracePoint)")
        public boolean disable(VirtualFrame frame, DynamicObject tracePoint, DynamicObject block) {
            EventBinding<?> eventBinding = (EventBinding<?>) Layouts.TRACE_POINT.getEventBinding(tracePoint);
            final boolean alreadyEnabled = eventBinding != null;

            if (alreadyEnabled) {
                EnableNode.dispose(eventBinding);
                Layouts.TRACE_POINT.setEventBinding(tracePoint, null);
            }

            if (block != null) {
                try {
                    yield(frame, block);
                } finally {
                    if (alreadyEnabled) {
                        eventBinding = EnableNode.createEventBinding(getContext(), tracePoint);
                        Layouts.TRACE_POINT.setEventBinding(tracePoint, eventBinding);
                    }
                }
            }

            return alreadyEnabled;
        }

    }

    @CoreMethod(names = "enabled?")
    public abstract static class EnabledNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isTracePoint(tracePoint)")
        public boolean enabled(DynamicObject tracePoint) {
            return Layouts.TRACE_POINT.getEventBinding(tracePoint) != null;
        }

    }

    @CoreMethod(names = "event")
    public abstract static class EventNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isTracePoint(tracePoint)")
        public DynamicObject event(DynamicObject tracePoint) {
            return Layouts.TRACE_POINT.getEvent(tracePoint);
        }

    }

    @CoreMethod(names = "path")
    public abstract static class PathNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isTracePoint(tracePoint)")
        public DynamicObject path(DynamicObject tracePoint) {
            return Layouts.TRACE_POINT.getPath(tracePoint);
        }

    }

    @CoreMethod(names = "lineno")
    public abstract static class LineNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isTracePoint(tracePoint)")
        public int line(DynamicObject tracePoint) {
            return Layouts.TRACE_POINT.getLine(tracePoint);
        }

    }

    @CoreMethod(names = "binding")
    public abstract static class BindingNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isTracePoint(tracePoint)")
        public DynamicObject binding(DynamicObject tracePoint) {
            return Layouts.TRACE_POINT.getBinding(tracePoint);
        }

    }

}
