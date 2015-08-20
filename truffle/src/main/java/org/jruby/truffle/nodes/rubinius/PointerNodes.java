/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.object.DynamicObject;
import jnr.ffi.Pointer;
import org.jruby.truffle.runtime.layouts.Layouts;

public abstract class PointerNodes {

    public static final Pointer NULL_POINTER = jnr.ffi.Runtime.getSystemRuntime().getMemoryManager().newOpaquePointer(0);

    public static DynamicObject createPointer(DynamicObject rubyClass, Pointer pointer) {
        if (pointer == null) {
            pointer = NULL_POINTER;
        }

        return Layouts.POINTER.createPointer(Layouts.CLASS.getInstanceFactory(rubyClass), pointer);
    }

}
