/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.core.ffi;


import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.rubinius.PointerPrimitiveNodes;

public class PointerGuards {

    public static boolean isNullPointer(DynamicObject pointer) {
        return Layouts.POINTER.getPointer(pointer) == PointerPrimitiveNodes.NULL_POINTER;
    }

}

