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
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;

/**
 * Casts a value into a boolean and defaults to the given value if not provided.
 */
@NodeChild(value = "value", type = RubyNode.class)
public abstract class BooleanCastWithDefaultNode extends RubyNode {

    private final boolean defaultValue;

    public BooleanCastWithDefaultNode(RubyContext context, SourceSection sourceSection, boolean defaultValue) {
        super(context, sourceSection);
        this.defaultValue = defaultValue;
    }

    public abstract boolean executeBoolean(VirtualFrame frame, Object value);

    @Specialization
    public boolean doDefault(NotProvided value) {
        return defaultValue;
    }

    @Specialization
    public boolean doBoolean(boolean value) {
        return value;
    }

    @Specialization(guards = "isNil(nil)")
    public boolean doNil(Object nil) {
        return false;
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
    public boolean doBasicObject(RubyBasicObject object) {
        return true;
    }

    @Override
    public abstract boolean executeBoolean(VirtualFrame frame);

}
