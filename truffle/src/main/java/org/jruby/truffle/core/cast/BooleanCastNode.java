/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.cast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.language.RubyNode;

/**
 * Casts a value into a boolean.
 */
@NodeChild(value = "value", type = RubyNode.class)
@Instrumentable(factory = BooleanCastNodeWrapper.class)
public abstract class BooleanCastNode extends RubyNode {

    public BooleanCastNode() {
    }

    public BooleanCastNode(BooleanCastNode node) {
    }

    public abstract boolean executeToBoolean(Object value);

    @Specialization(guards = "isNil(nil)")
    public boolean doNil(Object nil) {
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

    @Specialization(guards = "!isNil(object)")
    public boolean doBasicObject(DynamicObject object) {
        return true;
    }

    @Override
    public abstract boolean executeBoolean(VirtualFrame frame);

}
