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

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.*;
import jnr.ffi.Pointer;
import org.jruby.truffle.nodes.core.BasicObjectNodes;
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.om.dsl.api.Layout;
import org.jruby.truffle.runtime.RubyContext;
import com.oracle.truffle.api.object.DynamicObject;

public abstract class PointerNodes {

    public static final Pointer NULL_POINTER = jnr.ffi.Runtime.getSystemRuntime().getMemoryManager().newOpaquePointer(0);

    @Layout
    public interface PointerLayout extends BasicObjectNodes.BasicObjectLayout {

        DynamicObjectFactory createPointerShape(DynamicObject logicalClass, DynamicObject metaClass);

        DynamicObject createPointer(DynamicObjectFactory factory, Pointer pointer);

        boolean isPointer(DynamicObject object);

        Pointer getPointer(DynamicObject object);
        void setPointer(DynamicObject object, Pointer pointer);

    }

    public static final PointerLayout POINTER_LAYOUT = PointerLayoutImpl.INSTANCE;

    public static DynamicObject createPointer(DynamicObject rubyClass, Pointer pointer) {
        if (pointer == null) {
            pointer = NULL_POINTER;
        }

        return POINTER_LAYOUT.createPointer(ModuleNodes.getModel(rubyClass).factory, pointer);
    }

    public static void setPointer(DynamicObject pointer, Pointer newPointer) {
        POINTER_LAYOUT.setPointer(pointer, newPointer);
    }

    public static Pointer getPointer(DynamicObject pointer) {
        return POINTER_LAYOUT.getPointer(pointer);
    }

}
