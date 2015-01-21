/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import org.jruby.truffle.runtime.core.RubyArray;

public class ArrayGuards {

    public static boolean isNull(RubyArray array) {
        return array.getStore() == null;
    }

    public static boolean isIntegerFixnum(RubyArray array) {
        return array.getStore() instanceof int[];
    }

    public static boolean isLongFixnum(RubyArray array) {
        return array.getStore() instanceof long[];
    }

    public static boolean isFloat(RubyArray array) {
        return array.getStore() instanceof double[];
    }

    public static boolean isObject(RubyArray array) {
        return array.getStore() instanceof Object[];
    }

    public static boolean isOtherNull(RubyArray array, RubyArray other) {
        return other.getStore() == null;
    }

    public static boolean isOtherIntegerFixnum(RubyArray array, RubyArray other) {
        return other.getStore() instanceof int[];
    }

    public static boolean isOtherLongFixnum(RubyArray array, RubyArray other) {
        return other.getStore() instanceof long[];
    }

    public static boolean isOtherFloat(RubyArray array, RubyArray other) {
        return other.getStore() instanceof double[];
    }

    public static boolean isOtherObject(RubyArray array, RubyArray other) {
        return other.getStore() instanceof Object[];
    }

    public static boolean areBothNull(RubyArray a, RubyArray b) {
        return a.getStore() == null && b.getStore() == null;
    }

    public static boolean areBothIntegerFixnum(RubyArray a, RubyArray b) {
        return a.getStore() instanceof int[] && b.getStore() instanceof int[];
    }

    public static boolean areBothLongFixnum(RubyArray a, RubyArray b) {
        return a.getStore() instanceof long[] && b.getStore() instanceof long[];
    }

    public static boolean areBothFloat(RubyArray a, RubyArray b) {
        return a.getStore() instanceof double[] && b.getStore() instanceof double[];
    }

    public static boolean areBothObject(RubyArray a, RubyArray b) {
        return a.getStore() instanceof Object[] && b.getStore() instanceof Object[];
    }

}
