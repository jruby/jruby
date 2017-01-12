/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.cast;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;

/**
 * Casts a value into an int.
 */
@ImportStatic(Integer.class)
@NodeChild(value = "value", type = RubyNode.class)
public abstract class IntegerCastNode extends RubyNode {

    public abstract int executeCastInt(Object value);

    @Override
    public abstract int executeInteger(VirtualFrame frame);

    @Specialization
    public int doIntegerFixnum(int value) {
        return value;
    }

    @Specialization(guards = {"value >= MIN_VALUE", "value <= MAX_VALUE"})
    public int doLongFixnum(long value) {
        return (int) value;
    }

    @Fallback
    public int doBasicObject(Object object) {
        throw new RaiseException(coreExceptions().typeErrorIsNotA(object.toString(), "Fixnum (fitting in int)", this));
    }

}