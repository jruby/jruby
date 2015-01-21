/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

public abstract class RubyRange extends RubyBasicObject {

    protected final boolean excludeEnd;

    public RubyRange(RubyClass rangeClass, boolean excludeEnd) {
        super(rangeClass);
        this.excludeEnd = excludeEnd;
    }

    public boolean doesExcludeEnd() {
        return excludeEnd;
    }

    public static class IntegerFixnumRange extends RubyRange {

        private final int begin;
        private final int end;

        public IntegerFixnumRange(RubyClass rangeClass, int begin, int end, boolean excludeEnd) {
            super(rangeClass, excludeEnd);
            this.begin = begin;
            this.end = end;
        }

        public final int getBegin() {
            return begin;
        }

        public final int getEnd() {
            return end;
        }

        public final int getExclusiveEnd() {
            if (excludeEnd) {
                return end;
            } else {
                return end + 1;
            }
        }

    }

    public static class LongFixnumRange extends RubyRange {

        private final long begin;
        private final long end;

        public LongFixnumRange(RubyClass rangeClass, long begin, long end, boolean excludeEnd) {
            super(rangeClass, excludeEnd);
            this.begin = begin;
            this.end = end;
        }

        public final long getBegin() {
            return begin;
        }

        public final long getEnd() {
            return end;
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

        private final Object begin;
        private final Object end;

        public ObjectRange(RubyClass rangeClass, Object begin, Object end, boolean excludeEnd) {
            super(rangeClass, excludeEnd);
            this.begin = begin;
            this.end = end;
        }

        public Object getBegin() {
            return begin;
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
}
