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

@Deprecated
public class RubyObjectRange extends RubyBasicObject {

    public boolean excludeEnd;
    public Object begin;
    public Object end;

    public RubyObjectRange(RubyBasicObject rangeClass, Object begin, Object end, boolean excludeEnd) {
        super(rangeClass);
        this.begin = begin;
        this.end = end;
        this.excludeEnd = excludeEnd;
    }

}
