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

    /**
     * A range that has {@code Fixnum} begin and end.
     */
    public static class IntegerFixnumRange extends RubyRange {

        private final int begin;
        private final int end;

        public IntegerFixnumRange(RubyClass rangeClass, int begin, int end, boolean excludeEnd) {
            super(rangeClass, excludeEnd);
            this.begin = begin;
            this.end = end;
        }

        @Override
        public String toString() {
            if (excludeEnd) {
                return begin + "..." + end;
            } else {
                return begin + ".." + end;
            }
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

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + begin;
            result = prime * result + end;
            result = prime * result + (excludeEnd ? 1231 : 1237);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof IntegerFixnumRange)) {
                return false;
            }
            IntegerFixnumRange other = (IntegerFixnumRange) obj;
            if (begin != other.begin) {
                return false;
            }
            if (end != other.end) {
                return false;
            }
            if (excludeEnd != other.excludeEnd) {
                return false;
            }
            return true;
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
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((begin == null) ? 0 : begin.hashCode());
            result = prime * result + ((end == null) ? 0 : end.hashCode());
            result = prime * result + (excludeEnd ? 1231 : 1237);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof ObjectRange)) {
                return false;
            }
            ObjectRange other = (ObjectRange) obj;
            if (begin == null) {
                if (other.begin != null) {
                    return false;
                }
            } else if (!begin.equals(other.begin)) {
                return false;
            }
            if (end == null) {
                if (other.end != null) {
                    return false;
                }
            } else if (!end.equals(other.end)) {
                return false;
            }
            if (excludeEnd != other.excludeEnd) {
                return false;
            }
            return true;
        }

        @Override
        public boolean doesExcludeEnd() {
            return excludeEnd;
        }

    }
}
