/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.extra.ffi;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.dsl.Layout;
import jnr.ffi.Pointer;
import org.jruby.truffle.core.basicobject.BasicObjectLayout;

@Layout
public interface PointerLayout extends BasicObjectLayout {

    DynamicObjectFactory createPointerShape(DynamicObject logicalClass,
                                            DynamicObject metaClass);

    DynamicObject createPointer(DynamicObjectFactory factory,
                                Pointer pointer);

    boolean isPointer(DynamicObject object);

    Pointer getPointer(DynamicObject object);
    void setPointer(DynamicObject object, Pointer value);

}
