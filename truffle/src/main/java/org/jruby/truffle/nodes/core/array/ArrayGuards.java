/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core.array;

import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyRange;

public class ArrayGuards {

    public static boolean isEmpty(RubyArray array) {
        return ArrayNodes.getSize(array) == 0;
    }

    public static boolean isOtherEmpty(RubyArray array, RubyArray other) {
        return ArrayNodes.getSize(other) == 0;
    }

    public static boolean isNull(RubyArray array) {
        return ArrayNodes.getStore(array) == null;
    }

    public static boolean isNullOrEmpty(RubyArray array) {
        return ArrayNodes.getStore(array) == null || ArrayNodes.getSize(array) == 0;
    }

    public static boolean isIntegerFixnum(RubyArray array) {
        return ArrayNodes.getStore(array) instanceof int[];
    }

    public static boolean isLongFixnum(RubyArray array) {
        return ArrayNodes.getStore(array) instanceof long[];
    }

    public static boolean isFloat(RubyArray array) {
        return ArrayNodes.getStore(array) instanceof double[];
    }

    public static boolean isObject(RubyArray array) {
        return ArrayNodes.getStore(array) instanceof Object[];
    }

    public static boolean isOtherNull(RubyArray array, RubyArray other) {
        return ArrayNodes.getStore(other) == null;
    }

    public static boolean isOtherIntegerFixnum(RubyArray array, RubyArray other) {
        return ArrayNodes.getStore(other) instanceof int[];
    }

    public static boolean isOtherLongFixnum(RubyArray array, RubyArray other) {
        return ArrayNodes.getStore(other) instanceof long[];
    }

    public static boolean isOtherFloat(RubyArray array, RubyArray other) {
        return ArrayNodes.getStore(other) instanceof double[];
    }

    public static boolean isOtherObject(RubyArray array, RubyArray other) {
        return ArrayNodes.getStore(other) instanceof Object[];
    }

    public static boolean areBothNull(RubyArray a, RubyArray b) {
        return ArrayNodes.getStore(a) == null && ArrayNodes.getStore(b) == null;
    }

    public static boolean areBothIntegerFixnum(RubyArray a, RubyArray b) {
        return ArrayNodes.getStore(a) instanceof int[] && ArrayNodes.getStore(b) instanceof int[];
    }

    public static boolean areBothIntegerFixnum(RubyArray array, RubyRange.IntegerFixnumRange range, RubyArray other) {
        return ArrayNodes.getStore(array) instanceof int[] && ArrayNodes.getStore(other) instanceof int[];
    }

    public static boolean areBothLongFixnum(RubyArray a, RubyArray b) {
        return ArrayNodes.getStore(a) instanceof long[] && ArrayNodes.getStore(b) instanceof long[];
    }

    public static boolean areBothFloat(RubyArray a, RubyArray b) {
        return ArrayNodes.getStore(a) instanceof double[] && ArrayNodes.getStore(b) instanceof double[];
    }

    public static boolean areBothObject(RubyArray a, RubyArray b) {
        return ArrayNodes.getStore(a) instanceof Object[] && ArrayNodes.getStore(b) instanceof Object[];
    }

    // New names being used for the new primitive nodes - old guards will be removed over time

    public static boolean isNullArray(RubyArray array) {
        return ArrayNodes.getStore(array) == null;
    }

    public static boolean isIntArray(RubyArray array) {
        return ArrayNodes.getStore(array) instanceof int[];
    }

    public static boolean isLongArray(RubyArray array) {
        return ArrayNodes.getStore(array) instanceof long[];
    }

    public static boolean isDoubleArray(RubyArray array) {
        return ArrayNodes.getStore(array) instanceof double[];
    }

    public static boolean isObjectArray(RubyArray array) {
        return ArrayNodes.getStore(array) instanceof Object[];
    }

    public static boolean isOtherSingleIntegerFixnumArray(RubyArray array, Object[] others) {
        return others.length == 1 && others[0] instanceof RubyArray && ArrayNodes.getStore(((RubyArray) others[0])) instanceof int[];
    }

    public static boolean isOtherSingleObjectArray(RubyArray array, Object[] others) {
        return others.length == 1 && others[0] instanceof RubyArray && ArrayNodes.getStore(((RubyArray) others[0])) instanceof Object[];
    }

    public static boolean isArgsLengthTwo(RubyArray array, Object[] others) {
        return others.length == 2;
    }

    public static boolean isIntIndexAndOtherSingleObjectArg(RubyArray array, Object[] others) {
        return others.length == 2 && others[0] instanceof Integer && others[1] instanceof Object;
    }

    public static boolean isNegative(RubyArray array, int size) {
        return size < 0;
    }

    public static boolean isNegative(RubyArray array, long size) {
        return size < 0;
    }


}
