/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Activation;
import org.jruby.truffle.runtime.core.StringOperations;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.layouts.ThreadBacktraceLocationLayoutImpl;
import org.jruby.util.StringSupport;

@CoreClass(name = "Thread::Backtrace::Location")
public class ThreadBacktraceLocationNodes {

    @CoreMethod(names = { "absolute_path", "path" })
    // TODO (eregon, 8 July 2015): these two methods are slightly different (path can be relative if it is the main script)
    public abstract static class AbsolutePathNode extends UnaryCoreMethodNode {

        public AbsolutePathNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject absolutePath(DynamicObject threadBacktraceLocation) {
            final Activation activation = ThreadBacktraceLocationLayoutImpl.INSTANCE.getActivation(threadBacktraceLocation);

            final SourceSection sourceSection = activation.getCallNode().getEncapsulatingSourceSection();

            if (sourceSection.getSource() == null) {
                return createString(StringOperations.encodeByteList(sourceSection.getShortDescription(), UTF8Encoding.INSTANCE));
            }

            // TODO CS 30-Apr-15: not absolute - not sure how to solve that

            return createString(StringOperations.encodeByteList(sourceSection.getSource().getPath(), UTF8Encoding.INSTANCE));
        }

    }

    @CoreMethod(names = "lineno")
    public abstract static class LinenoNode extends UnaryCoreMethodNode {

        public LinenoNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public int lineno(DynamicObject threadBacktraceLocation) {
            final Activation activation = ThreadBacktraceLocationLayoutImpl.INSTANCE.getActivation(threadBacktraceLocation);

            final SourceSection sourceSection = activation.getCallNode().getEncapsulatingSourceSection();

            return sourceSection.getStartLine();
        }

    }

    @CoreMethod(names = {"to_s", "inspect"})
    public abstract static class ToSNode extends UnaryCoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject toS(DynamicObject threadBacktraceLocation) {
            final Activation activation = ThreadBacktraceLocationLayoutImpl.INSTANCE.getActivation(threadBacktraceLocation);

            final SourceSection sourceSection = activation.getCallNode().getEncapsulatingSourceSection();

            if (sourceSection.getSource() == null) {
                return createString(StringOperations.encodeByteList(sourceSection.getShortDescription(), UTF8Encoding.INSTANCE));
            }

            return createString(StringOperations.encodeByteList(String.format("%s:%d:in `%s'",
                    sourceSection.getSource().getName(),
                    sourceSection.getStartLine(),
                    sourceSection.getIdentifier()), UTF8Encoding.INSTANCE));
        }

    }

}
