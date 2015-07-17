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

public class RubyIntegerFixnumRange extends RubyBasicObject {

    private final boolean excludeEnd;
    private final int begin;
    private final int end;

    public RubyIntegerFixnumRange(RubyClass rangeClass, int begin, int end, boolean excludeEnd) {
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
