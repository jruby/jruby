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
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.api.source.NullSourceSection;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Activation;
import com.oracle.truffle.api.object.DynamicObject;

@CoreClass(name = "Thread::Backtrace::Location")
public class ThreadBacktraceLocationNodes {

    @org.jruby.truffle.om.dsl.api.Layout
    public interface ThreadBacktraceLocationLayout extends BasicObjectNodes.BasicObjectLayout {

        DynamicObjectFactory createThreadBacktraceLocationShape(DynamicObject logicalClass, DynamicObject metaClass);

        DynamicObject createThreadBacktraceLocation(DynamicObjectFactory factory, Activation activation);

        Activation getActivation(DynamicObject object);

    }

    public static final ThreadBacktraceLocationLayout THREAD_BACKTRACE_LOCATION_LAYOUT = ThreadBacktraceLocationLayoutImpl.INSTANCE;

    public static DynamicObject createRubyThreadBacktraceLocation(DynamicObject rubyClass, Activation activation) {
        return THREAD_BACKTRACE_LOCATION_LAYOUT.createThreadBacktraceLocation(ModuleNodes.getModel(rubyClass).getFactory(), activation);
    }

    protected static Activation getActivation(DynamicObject threadBacktraceLocation) {
        return THREAD_BACKTRACE_LOCATION_LAYOUT.getActivation(threadBacktraceLocation);
    }

    @CoreMethod(names = { "absolute_path", "path" })
    // TODO (eregon, 8 July 2015): these two methods are slightly different (path can be relative if it is the main script)
    public abstract static class AbsolutePathNode extends UnaryCoreMethodNode {

        public AbsolutePathNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject absolutePath(DynamicObject threadBacktraceLocation) {
            final Activation activation = getActivation(threadBacktraceLocation);

            final SourceSection sourceSection = activation.getCallNode().getEncapsulatingSourceSection();

            if (sourceSection instanceof NullSourceSection) {
                return createString(sourceSection.getShortDescription());
            }

            // TODO CS 30-Apr-15: not absolute - not sure how to solve that

            return createString(sourceSection.getSource().getPath());
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
            final Activation activation = getActivation(threadBacktraceLocation);

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
            final Activation activation = getActivation(threadBacktraceLocation);

            final SourceSection sourceSection = activation.getCallNode().getEncapsulatingSourceSection();

            if (sourceSection instanceof NullSourceSection) {
                return createString(sourceSection.getShortDescription());
            }

            return createString(String.format("%s:%d:in `%s'",
                        sourceSection.getSource().getShortName(),
                        sourceSection.getStartLine(),
                        sourceSection.getIdentifier()));
        }

    }

}
