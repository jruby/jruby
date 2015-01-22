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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;

@CoreClass(name = "Comparable")
public abstract class ComparableNodes {

    public abstract static class ComparableCoreMethodNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode compareNode;

        public ComparableCoreMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            compareNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        public ComparableCoreMethodNode(ComparableCoreMethodNode prev) {
            super(prev);
            compareNode = prev.compareNode;
        }

        public int compare(VirtualFrame frame, RubyBasicObject receiverObject, Object comparedTo) {
            return (int) compareNode.call(frame, receiverObject, "<=>", null, comparedTo);
        }

    }

    @CoreMethod(names = "<", required = 1)
    public abstract static class LessNode extends ComparableCoreMethodNode {

        public LessNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LessNode(LessNode prev) {
            super(prev);
        }

        @Specialization
        public boolean less(VirtualFrame frame, RubyBasicObject self, Object comparedTo) {
            notDesignedForCompilation();

            return compare(frame, self, comparedTo) < 0;
        }

    }

    @CoreMethod(names = "<=", required = 1)
    public abstract static class LessEqualNode extends ComparableCoreMethodNode {

        public LessEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LessEqualNode(LessEqualNode prev) {
            super(prev);
        }

        @Specialization
        public boolean lessEqual(VirtualFrame frame, RubyBasicObject self, Object comparedTo) {
            notDesignedForCompilation();

            return compare(frame, self, comparedTo) <= 0;
        }

    }

    @CoreMethod(names = "==", required = 1)
    public abstract static class EqualNode extends ComparableCoreMethodNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EqualNode(EqualNode prev) {
            super(prev);
        }

        @Specialization
        public boolean equal(VirtualFrame frame, RubyBasicObject self, Object comparedTo) {
            notDesignedForCompilation();

            if (self == comparedTo) {
                return true;
            }

            try {
                return compare(frame, self, comparedTo) == 0;
            } catch (Exception e) {
                // Comparable#== catches and ignores all exceptions in <=>, returning false
                return false;
            }
        }
    }

    @CoreMethod(names = ">=", required = 1)
    public abstract static class GreaterEqualNode extends ComparableCoreMethodNode {

        public GreaterEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GreaterEqualNode(GreaterEqualNode prev) {
            super(prev);
        }

        @Specialization
        public boolean greaterEqual(VirtualFrame frame, RubyBasicObject self, Object comparedTo) {
            notDesignedForCompilation();

            return compare(frame, self, comparedTo) >= 0;
        }

    }

    @CoreMethod(names = ">", required = 1)
    public abstract static class GreaterNode extends ComparableCoreMethodNode {

        public GreaterNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GreaterNode(GreaterNode prev) {
            super(prev);
        }

        @Specialization
        public boolean greater(VirtualFrame frame, RubyBasicObject self, Object comparedTo) {
            notDesignedForCompilation();

            return compare(frame, self, comparedTo) > 0;
        }

    }

    @CoreMethod(names = "between?", required = 2)
    public abstract static class BetweenNode extends ComparableCoreMethodNode {

        public BetweenNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BetweenNode(BetweenNode prev) {
            super(prev);
        }

        @Specialization
        public boolean between(VirtualFrame frame, RubyBasicObject self, Object min, Object max) {
            notDesignedForCompilation();

            return !(compare(frame, self, min) < 0 || compare(frame, self, max) > 0);
        }

    }

}
