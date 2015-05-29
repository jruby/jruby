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

    public static boolean isSingleIntegerFixnumArray(Object[] others) {
        return others.length == 1 && others[0] instanceof RubyArray && ArrayNodes.getStore(((RubyArray) others[0])) instanceof int[];
    }

    public static boolean isSingleObjectArray(Object[] others) {
        return others.length == 1 && others[0] instanceof RubyArray && ArrayNodes.getStore(((RubyArray) others[0])) instanceof Object[];
    }
    public static boolean isArgsLengthTwo(Object[] others) {
        return others.length == 2;
    }

    public static boolean isIntIndexAndOtherSingleObjectArg(Object[] others) {
        return others.length == 2 && others[0] instanceof Integer && others[1] instanceof Object;
    }

}
