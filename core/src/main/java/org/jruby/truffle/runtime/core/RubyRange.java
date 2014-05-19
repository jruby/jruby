/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

public abstract class RubyRange extends RubyObject {

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

        public final int getInclusiveEnd() {
            if (excludeEnd) {
                return end - 1;
            } else {
                return end;
            }
        }

        public final int getExclusiveEnd() {
            if (excludeEnd) {
                return end;
            } else {
                return end + 1;
            }
        }

        @Override
        public boolean doesExcludeEnd() {
            return excludeEnd;
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
        public boolean doesExcludeEnd() {
            return excludeEnd;
        }

    }
}
