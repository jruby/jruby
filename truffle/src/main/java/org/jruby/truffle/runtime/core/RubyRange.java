/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

public abstract class RubyRange extends RubyBasicObject {

    public RubyRange(RubyClass rangeClass) {
        super(rangeClass);
    }

    public abstract boolean doesExcludeEnd();

    public static class IntegerFixnumRange extends RubyRange {

        private final boolean excludeEnd;
        private final int begin;
        private final int end;

        public IntegerFixnumRange(RubyClass rangeClass, int begin, int end, boolean excludeEnd) {
            super(rangeClass);
            this.begin = begin;
            this.end = end;
            this.excludeEnd = excludeEnd;
        }

        public final int getBegin() {
            return begin;
        }

        public final int getEnd() {
            return end;
        }

        @Override
        public boolean doesExcludeEnd() {
            return excludeEnd;
        }

        public final int getExclusiveEnd() {
            if (excludeEnd) {
                return end;
            } else {
                return end + 1;
            }
        }

        public int getLength() {
            return getExclusiveEnd() - begin;
        }
    }

    public static class LongFixnumRange extends RubyRange {

        private final boolean excludeEnd;
        private final long begin;
        private final long end;

        public LongFixnumRange(RubyClass rangeClass, long begin, long end, boolean excludeEnd) {
            super(rangeClass);
            this.begin = begin;
            this.end = end;
            this.excludeEnd = excludeEnd;
        }

        public final long getBegin() {
            return begin;
        }

        public final long getEnd() {
            return end;
        }

        @Override
        public boolean doesExcludeEnd() {
            return excludeEnd;
        }

        public final long getExclusiveEnd() {
            if (excludeEnd) {
                return end;
            } else {
                return end + 1;
            }
        }

    }

    public static class ObjectRange extends RubyRange {

        private boolean excludeEnd;
        private Object begin;
        private Object end;

        public ObjectRange(RubyClass rangeClass, Object begin, Object end, boolean excludeEnd) {
            super(rangeClass);
            this.begin = begin;
            this.end = end;
            this.excludeEnd = excludeEnd;
        }

        public void initialize(Object begin, Object end, boolean excludeEnd) {
            this.begin = begin;
            this.end = end;
            this.excludeEnd = excludeEnd;
        }

        public Object getBegin() {
            return begin;
        }

        @Override
        public boolean doesExcludeEnd() {
            return excludeEnd;
        }

        public Object getEnd() {
            return end;
        }

        @Override
        public void visitObjectGraphChildren(ObjectSpaceManager.ObjectGraphVisitor visitor) {
            if (begin instanceof RubyBasicObject) {
                ((RubyBasicObject) begin).visitObjectGraph(visitor);
            }

            if (end instanceof RubyBasicObject) {
                ((RubyBasicObject) end).visitObjectGraph(visitor);
            }
        }

    }

    public static class RangeAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyRange.ObjectRange(rubyClass, context.getCoreLibrary().getNilObject(), context.getCoreLibrary().getNilObject(), false);
        }

    }
}
