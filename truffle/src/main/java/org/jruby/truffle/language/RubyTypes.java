/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language;

import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.TypeSystem;
import com.oracle.truffle.api.dsl.internal.DSLOptions;

import static com.oracle.truffle.api.dsl.internal.DSLOptions.DSLGenerator.DEFAULT;

@TypeSystem
@DSLOptions(defaultGenerator=DEFAULT)
public abstract class RubyTypes {

    @ImplicitCast
    public static int promoteToInt(byte value) {
        return value;
    }

    @ImplicitCast
    public static int promoteToInt(short value) {
        return value;
    }

    @ImplicitCast
    public static long promoteToLong(byte value) {
        return value;
    }

    @ImplicitCast
    public static long promoteToLong(short value) {
        return value;
    }

    @ImplicitCast
    public static long promoteToLong(int value) {
        return value;
    }

    @ImplicitCast
    public static double promoteToDouble(float value) {
        return value;
    }

}
