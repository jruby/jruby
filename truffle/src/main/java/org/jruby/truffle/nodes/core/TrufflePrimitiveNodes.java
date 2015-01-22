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

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.RubyGC;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBinding;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.hash.HashOperations;
import org.jruby.truffle.runtime.hash.KeyValue;
import org.jruby.util.Memo;

import java.util.ArrayList;
import java.util.Collection;

@CoreClass(name = "Truffle::Primitive")
public abstract class TrufflePrimitiveNodes {

    @CoreMethod(names = "binding_of_caller", onSingleton = true)
    public abstract static class BindingOfCallerNode extends CoreMethodNode {

        public BindingOfCallerNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BindingOfCallerNode(BindingOfCallerNode prev) {
            super(prev);
        }

        @Specialization
        public RubyBinding bindingOfCaller() {
            /*
             * When you use this method you're asking for the binding of the caller at the call site. When we get into
             * this method, that is then the binding of the caller of the caller.
             */

            notDesignedForCompilation();

            final Memo<Integer> frameCount = new Memo<>(0);

            final MaterializedFrame frame = Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<MaterializedFrame>() {

                @Override
                public MaterializedFrame visitFrame(FrameInstance frameInstance) {
                    if (frameCount.get() == 1) {
                        return frameInstance.getFrame(FrameInstance.FrameAccess.READ_WRITE, false).materialize();
                    } else {
                        frameCount.set(frameCount.get() + 1);
                        return null;
                    }
                }

            });

            return new RubyBinding(
                    getContext().getCoreLibrary().getBindingClass(),
                    RubyArguments.getSelf(frame.getArguments()),
                    frame);
        }

    }

    @CoreMethod(names = "coverage_result", onSingleton = true)
    public abstract static class CoverageResultNode extends CoreMethodNode {

        public CoverageResultNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CoverageResultNode(CoverageResultNode prev) {
            super(prev);
        }

        @Specialization
        public RubyHash coverageResult() {
            // TODO(CS, 18-Jan-15) Implement

            final Collection<KeyValue> keyValues = new ArrayList<>();

            for (String file : new String[]{"example.rb"}) {
                final Object[] array = new Object[]{getContext().getCoreLibrary().getNilObject(), 1, 2, 3};
                keyValues.add(new KeyValue(getContext().makeString(file), new RubyArray(getContext().getCoreLibrary().getArrayClass(), array, array.length)));
            }

            return HashOperations.verySlowFromEntries(getContext(), keyValues);
        }

    }

    @CoreMethod(names = "coverage_start", onSingleton = true)
    public abstract static class CoverageStartNode extends CoreMethodNode {

        public CoverageStartNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CoverageStartNode(CoverageStartNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass coverageStart() {
            // TODO(CS, 18-Jan-15) Implement

            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "gc_count", onSingleton = true)
    public abstract static class GCCountNode extends CoreMethodNode {

        public GCCountNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GCCountNode(GCCountNode prev) {
            super(prev);
        }

        @Specialization
        public int gcCount() {
            return RubyGC.getCollectionCount();
        }

    }

    @CoreMethod(names = "gc_time", onSingleton = true)
    public abstract static class GCTimeNode extends CoreMethodNode {

        public GCTimeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GCTimeNode(GCTimeNode prev) {
            super(prev);
        }

        @Specialization
        public long gcTime() {
            return RubyGC.getCollectionTime();
        }

    }

}
