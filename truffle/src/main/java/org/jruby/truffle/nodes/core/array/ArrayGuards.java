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

import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.nodes.RubyGuards;

public class ArrayGuards {

    // Storage strategies

    public static boolean isNullArray(DynamicObject array) {
        assert RubyGuards.isRubyArray(array);
        return ArrayNodes.ARRAY_LAYOUT.getStore(array) == null;
    }

    public static boolean isIntArray(DynamicObject array) {
        assert RubyGuards.isRubyArray(array);
        return ArrayNodes.ARRAY_LAYOUT.getStore(array) instanceof int[];
    }

    public static boolean isLongArray(DynamicObject array) {
        assert RubyGuards.isRubyArray(array);
        return ArrayNodes.ARRAY_LAYOUT.getStore(array) instanceof long[];
    }

    public static boolean isDoubleArray(DynamicObject array) {
        assert RubyGuards.isRubyArray(array);
        return ArrayNodes.ARRAY_LAYOUT.getStore(array) instanceof double[];
    }

    public static boolean isObjectArray(DynamicObject array) {
        assert RubyGuards.isRubyArray(array);
        return ArrayNodes.ARRAY_LAYOUT.getStore(array) instanceof Object[];
    }

    // Higher level properties

    public static boolean isEmptyArray(DynamicObject array) {
        assert RubyGuards.isRubyArray(array);
        return ArrayNodes.ARRAY_LAYOUT.getSize(array) == 0;
    }

}
