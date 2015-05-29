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

    // Storage strategies

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

    // Higher level properties

    public static boolean isEmptyArray(RubyArray array) {
        return ArrayNodes.getSize(array) == 0;
    }

}
