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

public class RubyLongFixnumRange extends RubyBasicObject {

    private final boolean excludeEnd;
    private final long begin;
    private final long end;

    public RubyLongFixnumRange(RubyClass rangeClass, long begin, long end, boolean excludeEnd) {
        super(rangeClass);
        assert !CoreLibrary.fitsIntoInteger(begin) || !CoreLibrary.fitsIntoInteger(end);
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
