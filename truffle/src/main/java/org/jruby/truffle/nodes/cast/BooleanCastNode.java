/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.cast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyNilClass;

/**
 * Casts a value into a boolean.
 */
@NodeChild(value = "value", type = RubyNode.class)
public abstract class BooleanCastNode extends RubyNode {

    public BooleanCastNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public BooleanCastNode(BooleanCastNode copy) {
        super(copy.getContext(), copy.getSourceSection());
    }

    public abstract boolean executeBoolean(VirtualFrame frame, Object value);

    @Specialization
    public boolean doNil(RubyNilClass nil) {
        return false;
    }

    @Specialization
    public boolean doBoolean(boolean value) {
        return value;
    }

    @Specialization
    public boolean doIntegerFixnum(int value) {
        return true;
    }

    @Specialization
    public boolean doLongFixnum(long value) {
        return true;
    }

    @Specialization
    public boolean doFloat(double value) {
        return true;
    }

    @Specialization(guards = "!isRubyNilClass(object)")
    public boolean doBasicObject(RubyBasicObject object) {
        return true;
    }

    @Override
    public abstract boolean executeBoolean(VirtualFrame frame);

}
