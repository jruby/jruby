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

import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.runtime.core.RubyBasicObject;

public class ArrayGuards {

    // Storage strategies

    public static boolean isNullArray(RubyBasicObject array) {
        assert RubyGuards.isRubyArray(array);
        return ArrayNodes.getStore(array) == null;
    }

    public static boolean isIntArray(RubyBasicObject array) {
        assert RubyGuards.isRubyArray(array);
        return ArrayNodes.getStore(array) instanceof int[];
    }

    public static boolean isLongArray(RubyBasicObject array) {
        assert RubyGuards.isRubyArray(array);
        return ArrayNodes.getStore(array) instanceof long[];
    }

    public static boolean isDoubleArray(RubyBasicObject array) {
        assert RubyGuards.isRubyArray(array);
        return ArrayNodes.getStore(array) instanceof double[];
    }

    public static boolean isObjectArray(RubyBasicObject array) {
        assert RubyGuards.isRubyArray(array);
        return ArrayNodes.getStore(array) instanceof Object[];
    }

    // Higher level properties

    public static boolean isEmptyArray(RubyBasicObject array) {
        assert RubyGuards.isRubyArray(array);
        return ArrayNodes.getSize(array) == 0;
    }

}
