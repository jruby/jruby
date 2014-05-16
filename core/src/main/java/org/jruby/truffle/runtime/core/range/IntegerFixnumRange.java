/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core.range;

import org.jruby.truffle.runtime.core.*;

/**
 * A range that has {@code Fixnum} begin and end.
 */
public class IntegerFixnumRange extends RubyRange {

    private final int begin;
    private final int end;
    private final boolean excludeEnd;

    public IntegerFixnumRange(RubyClass rangeClass, int begin, int end, boolean excludeEnd) {
        super(rangeClass);
        this.begin = begin;
        this.end = end;
        this.excludeEnd = excludeEnd;
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
