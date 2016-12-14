/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.tools;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import org.jruby.truffle.language.RubyNode;

import java.util.Random;

@NodeChild
public abstract class ChaosNode extends RubyNode {

    private static final Random random = new Random(0);

    @TruffleBoundary
    @Specialization
    public Object chaos(int value) {
        if (random.nextBoolean()) {
            return (long) value;
        } else {
            return value;
        }
    }

    @Fallback
    public Object chaos(Object value) {
        return value;
    }

}
