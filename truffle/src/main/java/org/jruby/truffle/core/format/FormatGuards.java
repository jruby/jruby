/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format;

import org.jruby.truffle.language.RubyGuards;

import java.math.BigInteger;

public class FormatGuards {

    public static boolean isNull(Object object) {
        return object == null;
    }

    public static boolean isBoolean(Object object) {
        return object instanceof Boolean;
    }

    public static boolean isInteger(Object object) {
        return object instanceof Integer;
    }

    public static boolean isLong(Object object) {
        return object instanceof Long;
    }

    public static boolean isBigInteger(Object object) {
        return object instanceof BigInteger;
    }

    public static boolean isRubyBignum(Object object) {
        return RubyGuards.isRubyBignum(object);
    }

    public static boolean isRubyString(Object object) {
        return RubyGuards.isRubyString(object);
    }

    public static boolean isRubyArray(Object object) {
        return RubyGuards.isRubyArray(object);
    }

    public static boolean isMissingValue(Object object) {
        return object == MissingValue.INSTANCE;
    }

}
