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

import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

public class RubyObjectRange extends RubyBasicObject {

    private boolean excludeEnd;
    private Object begin;
    private Object end;

    public RubyObjectRange(RubyClass rangeClass, Object begin, Object end, boolean excludeEnd) {
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

    public void setBegin(Object begin) {
        this.begin = begin;
    }

    public void setEnd(Object end) {
        this.end = end;
    }

    public void setExcludeEnd(boolean excludeEnd) {
        this.excludeEnd = excludeEnd;
    }

    public Object getBegin() {
        return begin;
    }

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
