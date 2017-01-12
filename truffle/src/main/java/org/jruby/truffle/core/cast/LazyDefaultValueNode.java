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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyNode;

import java.util.function.Supplier;

@NodeChild(value = "value", type = RubyNode.class)
public abstract class LazyDefaultValueNode extends RubyNode {

    private final Supplier<Object> defaultValueProducer;

    @CompilationFinal private boolean hasDefault;
    @CompilationFinal private Object defaultValue;

    public LazyDefaultValueNode(Supplier<Object> defaultValueProducer) {
        this.defaultValueProducer = defaultValueProducer;
    }

    @Specialization
    public Object doDefault(NotProvided value) {
        if (!hasDefault) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            defaultValue = defaultValueProducer.get();
            hasDefault = true;
        }

        return defaultValue;
    }

    @Specialization(guards = "wasProvided(value)")
    public Object doProvided(Object value) {
        return value;
    }

}
