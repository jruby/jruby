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

    public final boolean excludeEnd;
    public final long begin;
    public final long end;

    public RubyLongFixnumRange(RubyClass rangeClass, long begin, long end, boolean excludeEnd) {
        super(rangeClass);
        assert !CoreLibrary.fitsIntoInteger(begin) || !CoreLibrary.fitsIntoInteger(end);
        this.begin = begin;
        this.end = end;
        this.excludeEnd = excludeEnd;
    }

}
