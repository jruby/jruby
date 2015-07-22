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
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Activation;
import org.jruby.truffle.runtime.core.RubyBasicObject;

import java.util.EnumSet;

@CoreClass(name = "Thread::Backtrace::Location")
public class ThreadBacktraceLocationNodes {

    private static final HiddenKey ACTIVATION_IDENTIFIER = new HiddenKey("activation");
    private static final Property ACTIVATION_PROPERTY;
    private static final DynamicObjectFactory THREAD_BACKTRACE_LOCATION_FACTORY;

    static {
        Shape.Allocator allocator = RubyBasicObject.LAYOUT.createAllocator();
        ACTIVATION_PROPERTY = Property.create(ACTIVATION_IDENTIFIER, allocator.locationForType(Activation.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)), 0);
        final Shape shape = RubyBasicObject.EMPTY_SHAPE.addProperty(ACTIVATION_PROPERTY);
        THREAD_BACKTRACE_LOCATION_FACTORY = shape.createFactory();
    }

    public static RubyBasicObject createRubyThreadBacktraceLocation(RubyBasicObject rubyClass, Activation activation) {
        return new RubyBasicObject(rubyClass, THREAD_BACKTRACE_LOCATION_FACTORY.newInstance(activation));
    }

    protected static Activation getActivation(RubyBasicObject threadBacktraceLocation) {
        assert threadBacktraceLocation.getDynamicObject().getShape().hasProperty(ACTIVATION_IDENTIFIER);
        return (Activation) ACTIVATION_PROPERTY.get(threadBacktraceLocation.getDynamicObject(), true);
    }

    @CoreMethod(names = { "absolute_path", "path" })
    // TODO (eregon, 8 July 2015): these two methods are slightly different (path can be relative if it is the main script)
    public abstract static class AbsolutePathNode extends UnaryCoreMethodNode {

        public AbsolutePathNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyBasicObject absolutePath(RubyBasicObject threadBacktraceLocation) {
            final Activation activation = getActivation(threadBacktraceLocation);

            final SourceSection sourceSection = activation.getCallNode().getEncapsulatingSourceSection();

            if (sourceSection.getSource() == null) {
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
        public int lineno(RubyBasicObject threadBacktraceLocation) {
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
        public RubyBasicObject toS(RubyBasicObject threadBacktraceLocation) {
            final Activation activation = getActivation(threadBacktraceLocation);

            final SourceSection sourceSection = activation.getCallNode().getEncapsulatingSourceSection();

            if (sourceSection.getSource() == null) {
                return createString(sourceSection.getShortDescription());
            }

            return createString(String.format("%s:%d:in `%s'",
                        sourceSection.getSource().getShortName(),
                        sourceSection.getStartLine(),
                        sourceSection.getIdentifier()));
        }

    }

}
