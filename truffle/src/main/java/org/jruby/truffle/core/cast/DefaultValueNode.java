/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyNode;

/**
 * Return the given default value if the argument is not provided.
 */
@NodeChild(value = "value", type = RubyNode.class)
public abstract class DefaultValueNode extends RubyNode {

    private final Object defaultValue;

    public DefaultValueNode(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Specialization
    public Object doDefault(NotProvided value) {
        return defaultValue;
    }

    @Specialization(guards = "wasProvided(value)")
    public Object doProvided(Object value) {
        return value;
    }

}
