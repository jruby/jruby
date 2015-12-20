/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes;

import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.TypeSystem;

@TypeSystem({})
public abstract class RubyTypes {

    @ImplicitCast
    public static int promote(byte value) {
        return value;
    }

    @ImplicitCast
    public static int promote(short value) {
        return value;
    }

    @ImplicitCast
    public static long promote(int value) {
        return value;
    }

    @ImplicitCast
    public static double promote(float value) {
        return value;
    }

}
